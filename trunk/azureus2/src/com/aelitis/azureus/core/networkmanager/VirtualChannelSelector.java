/*
 * Created on Jun 5, 2005
 * Created by Alon Rohter
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.networkmanager;

import java.nio.channels.*;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.Debug;


import com.aelitis.azureus.core.networkmanager.impl.VirtualChannelSelectorImpl;


public class VirtualChannelSelector {
  public static final int OP_CONNECT  = SelectionKey.OP_CONNECT;
  public static final int OP_READ   = SelectionKey.OP_READ;
  public static final int OP_WRITE  = SelectionKey.OP_WRITE;

  private boolean SAFE_SELECTOR_MODE_ENABLED = COConfigurationManager.getBooleanParameter( "network.tcp.enable_safe_selector_mode" );
  
  private static final int MAX_SAFEMODE_SELECTORS = 100;
  

  private VirtualChannelSelectorImpl selector_impl;
  
  private volatile boolean	destroyed;
  
  //ONLY USED IN FAULTY MODE
  private HashMap selectors;
  private AEMonitor selectors_mon;
  private final int op;
  private final boolean pause;
  
  

  
  /**
   * Create a new virtual selectable-channel selector, selecting over the given interest-op.
   * @param interest_op operation set of OP_CONNECT, OP_READ, or OP_WRITE
   * @param pause_after_select whether or not to auto-disable interest op after select  
   */
  public VirtualChannelSelector( int interest_op, boolean pause_after_select ) { 
    this.op = interest_op;
    this.pause = pause_after_select;
    
    if( SAFE_SELECTOR_MODE_ENABLED ) {
      initSafeMode();
    }
    else {
      selector_impl = new VirtualChannelSelectorImpl( this, op, pause );
      selectors = null;
      selectors_mon = null;
    }
  }

  
  
  private void initSafeMode() {
  	if( LGLogger.isEnabled() )  LGLogger.log( "*** SAFE SOCKET SELECTOR MODE ENABLED ***" );
    selector_impl = null;
    selectors = new HashMap();
    selectors_mon = new AEMonitor( "VirtualChannelSelector:FM" );
    selectors.put( new VirtualChannelSelectorImpl( this, op, pause ), new ArrayList() );
  }
  
  
  
  
  /**
   * Register the given selectable channel, using the given listener for notification
   * of completed select operations.
   * NOTE: For OP_CONNECT and OP_WRITE -type selectors, once a selection request op
   * completes, the channel's op registration is automatically disabled (paused); any
   * future wanted selection notification requires re-enabling via resume.  For OP_READ selectors,
   * it stays enabled until actively paused, no matter how many times it is selected.
   * @param channel socket to listen for
   * @param listener op-complete listener
   * @param attachment object to be passed back with listener notification
   */
  public void register( SocketChannel channel, VirtualSelectorListener listener, Object attachment ) {
    if( SAFE_SELECTOR_MODE_ENABLED ) {
      try{  selectors_mon.enter();
        for( Iterator it = selectors.entrySet().iterator(); it.hasNext(); ) {
          Map.Entry entry = (Map.Entry)it.next();          
          VirtualChannelSelectorImpl sel = (VirtualChannelSelectorImpl)entry.getKey();
          ArrayList channels = (ArrayList)entry.getValue();
          
          if( channels.size() < 60 ) {  //there's room in the current selector
            sel.register( channel, listener, attachment );
            channels.add( channel );
            return;
          }
        }
        
        //we couldnt find room in any of the existing selectors, so start up a new one if allowed
        
        //max limit to the number of Selectors we are allowed to create
        if( selectors.size() >= MAX_SAFEMODE_SELECTORS ) {
      	  String msg = "Error: MAX_SAFEMODE_SELECTORS reached [" +selectors.size()+ "], no more socket channels can be registered. Too many peer connections.";
      	  Debug.out( msg );
      	  listener.selectFailure( this, channel, attachment, new Throwable( msg ) );  //reject registration    	  
      	  return;
        }
        
        if ( destroyed ){
          String	msg = "socket registered after controller destroyed";
       	  Debug.out( msg );
      	  listener.selectFailure( this, channel, attachment, new Throwable( msg ) );  //reject registration    	  
      	  return;
        }

        VirtualChannelSelectorImpl sel = new VirtualChannelSelectorImpl( this, op, pause );
        ArrayList chans = new ArrayList();
        selectors.put( sel, chans );
        sel.register( channel, listener, attachment );
        chans.add( channel );
      }
      finally{ selectors_mon.exit();  }
    }
    else {
      selector_impl.register( channel, listener, attachment );
    }
  }
  
  
  
  /**
   * Pause selection operations for the given channel
   * @param channel to pause
   */
  public void pauseSelects( SocketChannel channel ) {
    if( SAFE_SELECTOR_MODE_ENABLED ) {
      try{  selectors_mon.enter();
        for( Iterator it = selectors.entrySet().iterator(); it.hasNext(); ) {
          Map.Entry entry = (Map.Entry)it.next();          
          VirtualChannelSelectorImpl sel = (VirtualChannelSelectorImpl)entry.getKey();
          ArrayList channels = (ArrayList)entry.getValue();
          
          if( channels.contains( channel ) ) {
            sel.pauseSelects( channel );
            return;
          }
        }
        
        System.out.println( "pauseSelects():: channel not found!" );
      }
      finally{ selectors_mon.exit();  }
    }
    else {
      selector_impl.pauseSelects( channel );
    }
  }
  

  
  /**
   * Resume selection operations for the given channel
   * @param channel to resume
   */
  public void resumeSelects( SocketChannel channel ) {
    if( SAFE_SELECTOR_MODE_ENABLED ) {
      try{  selectors_mon.enter();
        for( Iterator it = selectors.entrySet().iterator(); it.hasNext(); ) {
          Map.Entry entry = (Map.Entry)it.next();          
          VirtualChannelSelectorImpl sel = (VirtualChannelSelectorImpl)entry.getKey();
          ArrayList channels = (ArrayList)entry.getValue();
          
          if( channels.contains( channel ) ) {
            sel.resumeSelects( channel );
            return;
          }
        }
        
        System.out.println( "resumeSelects():: channel not found!" );
      }
      finally{ selectors_mon.exit();  }
    }
    else {
      selector_impl.resumeSelects( channel );
    }
  }
  
  

  /**
   * Cancel the selection operations for the given channel.
   * @param channel channel originally registered
   */
  public void cancel( SocketChannel channel ) {
    if( SAFE_SELECTOR_MODE_ENABLED ) {
      try{  selectors_mon.enter();
        for( Iterator it = selectors.entrySet().iterator(); it.hasNext(); ) {
          Map.Entry entry = (Map.Entry)it.next();          
          VirtualChannelSelectorImpl sel = (VirtualChannelSelectorImpl)entry.getKey();
          ArrayList channels = (ArrayList)entry.getValue();
          
          if( channels.remove( channel ) ) {
            sel.cancel( channel );
            return;
          }
        }
      }
      finally{ selectors_mon.exit();  }
    }
    else {
      if( selector_impl != null )  selector_impl.cancel( channel );
    }
  }

  
  
  /**
   * Run a virtual select() operation, with the given selection timeout value;
   * (1) cancellations are processed (2) the select operation is performed; (3)
   * listener notification of completed selects (4) new registrations are processed
   * @param timeout in ms; if zero, block indefinitely
   * @return number of sockets selected
   */
  public int select(long timeout) {
    if( SAFE_SELECTOR_MODE_ENABLED ) {
      HashSet sels = null;
      
      try{  selectors_mon.enter();
        sels = new HashSet( selectors.keySet() );
      }
      finally{ selectors_mon.exit();  }

      int count = 0;
      
      for( Iterator it = sels.iterator(); it.hasNext(); ) {
        VirtualChannelSelectorImpl sel = (VirtualChannelSelectorImpl)it.next();
        
        count += sel.select( timeout );
      }
      
      return count;
    }
   
    return selector_impl.select( timeout );
  }

  public void destroy()
  {
	  destroyed	= true;
	  
	  if( SAFE_SELECTOR_MODE_ENABLED ) {
				      
		 try{  
			 selectors_mon.enter();
		      
		     for( Iterator it = selectors.keySet().iterator(); it.hasNext(); ) {
		    	 
		        VirtualChannelSelectorImpl sel = (VirtualChannelSelectorImpl)it.next();
		        
		       sel.destroy();
		     }
		 }finally{
			 selectors_mon.exit();
		 }
	  }else{
		  selector_impl.destroy();
	  }
  }
  
  public boolean
  isDestroyed()
  {
	  return( destroyed );
  }
  
  public boolean isSafeSelectionModeEnabled() {  return SAFE_SELECTOR_MODE_ENABLED;  }
  
  public void enableSafeSelectionMode() {
    if( !SAFE_SELECTOR_MODE_ENABLED ) {
      SAFE_SELECTOR_MODE_ENABLED = true;
      COConfigurationManager.setParameter( "network.tcp.enable_safe_selector_mode", true );
      initSafeMode();
    }
  }
  
  

  /**
   * Listener for notification upon socket channel selection.
   */
  public interface VirtualSelectorListener {
    /**
     * Called when a channel is successfully selected for readyness.
     * @param attachment originally given with the channel's registration
     * @return indicator of whether or not any 'progress' was made due to this select
     *         e.g. read-select -> read >0 bytes, write-select -> wrote > 0 bytes
     */
    public boolean selectSuccess(VirtualChannelSelector selector, SocketChannel sc, Object attachment);

    /**
     * Called when a channel selection fails.
     * @param msg  failure message
     */
    public void selectFailure(VirtualChannelSelector selector, SocketChannel sc, Object attachment, Throwable msg);
  }


}
