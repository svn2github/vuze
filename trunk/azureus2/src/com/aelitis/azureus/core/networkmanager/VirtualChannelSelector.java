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


import com.aelitis.azureus.core.networkmanager.impl.VirtualChannelSelectorImpl;


public class VirtualChannelSelector {
  
  /**
   * TODO
   */
  private static final boolean ENABLE_FAULTY_SELECTOR_MODE = COConfigurationManager.getBooleanParameter( "network.tcp.enable_faulty_selector_mode" );
  static{
    if( ENABLE_FAULTY_SELECTOR_MODE )  LGLogger.log( LGLogger.INFORMATION, "**** FAULTY SELECTOR COMPATIBILITY MODE ENABLED ****" );
  }
  
  
  
  public static final int OP_CONNECT  = SelectionKey.OP_CONNECT;
  public static final int OP_READ   = SelectionKey.OP_READ;
  public static final int OP_WRITE  = SelectionKey.OP_WRITE;


  private final VirtualChannelSelectorImpl selector_impl;
  
  //ONLY USED IN FAULTY MODE
  private final HashMap selectors;
  private final AEMonitor selectors_mon;
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
    
    VirtualChannelSelectorImpl sel = new VirtualChannelSelectorImpl( this, op, pause );
    
    if( ENABLE_FAULTY_SELECTOR_MODE ) {
      selector_impl = null;
      selectors = new HashMap();
      selectors_mon = new AEMonitor( "VirtualChannelSelector:FM" );
      selectors.put( sel, new ArrayList() );
    }
    else {
      selector_impl = sel;
      selectors = null;
      selectors_mon = null;
    }
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
    if( ENABLE_FAULTY_SELECTOR_MODE ) {
      try{  selectors_mon.enter();
        for( Iterator it = selectors.entrySet().iterator(); it.hasNext(); ) {
          Map.Entry entry = (Map.Entry)it.next();          
          VirtualChannelSelectorImpl sel = (VirtualChannelSelectorImpl)entry.getKey();
          ArrayList channels = (ArrayList)entry.getValue();
          
          if( channels.size() < 55 ) {  //there's room in the current selector
            sel.register( channel, listener, attachment );
            channels.add( channel );
            return;
          }
        }
        
        //we couldnt find room in any of the existing selectors, so start up a new one
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
    if( ENABLE_FAULTY_SELECTOR_MODE ) {
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
    if( ENABLE_FAULTY_SELECTOR_MODE ) {
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
    if( ENABLE_FAULTY_SELECTOR_MODE ) {
      try{  selectors_mon.enter();
        for( Iterator it = selectors.entrySet().iterator(); it.hasNext(); ) {
          Map.Entry entry = (Map.Entry)it.next();          
          VirtualChannelSelectorImpl sel = (VirtualChannelSelectorImpl)entry.getKey();
          ArrayList channels = (ArrayList)entry.getValue();
          
          if( channels.contains( channel ) ) {
            sel.cancel( channel );
            channels.remove( channel );
            return;
          }
        }
        
        System.out.println( "cancel():: channel not found!" );
      }
      finally{ selectors_mon.exit();  }
    }
    else {
      selector_impl.cancel( channel );
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
    if( ENABLE_FAULTY_SELECTOR_MODE ) {
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
