/*
 * Created on Jul 28, 2004
 * Created by Alon Rohter
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
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

import org.gudy.azureus2.core3.util.*;


/**
 * Provides a simplified and safe (selectable-channel) socket single-op selector.
 */
public class VirtualChannelSelector {
    public static final int OP_CONNECT = SelectionKey.OP_CONNECT;
    public static final int OP_READ = SelectionKey.OP_READ;
    public static final int OP_WRITE = SelectionKey.OP_WRITE;
  
  
    private static final int SELECTOR_FAIL_COUNT_MAX = 10000;  // a real selector spin will easily reach this
    
    private Selector selector;
    private final SelectorGuard selector_guard;
    private final ArrayList register_list 		= new ArrayList();
    private final AEMonitor register_list_mon	= new AEMonitor( "VirtualChannelSelector:RL");

    private final ArrayList cancel_list = new ArrayList();
    private final AEMonitor cancel_list_mon = new AEMonitor( "VirtualChannelSelector:CL" );
    
    private final int INTEREST_OP;


    /**
     * Create a new virtual selectable-channel selector,
     * selecting over the given interest-op(s).
     * @param interest_op operation set of OP_ACCEPT, OP_CONNECT, OP_READ, OP_WRITE
     */
    protected VirtualChannelSelector( int interest_op ) {
      this.INTEREST_OP = interest_op;
      selector_guard = new SelectorGuard( SELECTOR_FAIL_COUNT_MAX );
    	try {
    		selector = Selector.open();
    	}
      catch (Throwable t) {  Debug.out( t );  }
    }
    
  
    /**
     * Register the given selectable channel, using the given listener for notification
     * of completed select operation.
     * NOTE: For OP_CONNECT and OP_WRITE -type selectors, once a selection request op
     * completes, the channel's listener registration is automatically canceled; any
     * future selection notification requires re-registration.  For OP_READ selectors,
     * a registration is valid until actively canceled, no matter how many times it is
     * selected.
     * @param channel socket to listen for
     * @param listener op-complete listener
     * @param attachment object to be passed back with listener notification
     */
    protected void register( SocketChannel channel, VirtualSelectorListener listener, Object attachment ) {
      try{
      	register_list_mon.enter();
      
        register_list.add( new RegistrationData( channel, listener, attachment ) );
      }finally{
      	
      	register_list_mon.exit();
      }
      selector.wakeup();
    }
    
    
    /**
     * Cancel the select request.
     * Once canceled, the channel is unregistered and the listener will never be invoked.
     * @param channel channel originally registered
     */
    protected void cancel( SocketChannel channel ) {
      try {
        cancel_list_mon.enter();
        cancel_list.add( channel );
      }
      finally { 
        cancel_list_mon.exit();
      }
    }
    
    
    
    /**
     * Run a virtual select() operation, with the given selection timeout value;
     *   (1) cancellations are processed
     *   (2) the select operation is performed;
     *   (3) listener notification of completed selects
     *   (4) new registrations are processed;
     * @param timeout in ms
     * @return number of sockets selected
     */
    protected int select( long timeout ) {
      //process cancellations
      try {
        cancel_list_mon.enter();
        
        for( Iterator can_it = cancel_list.iterator(); can_it.hasNext(); ) {
          SocketChannel canceled_channel = (SocketChannel)can_it.next();
          boolean found_in_registration_list = false;
          
          try{
            register_list_mon.enter();
            
            //check if not yet registered, and cancel immediately
            for( Iterator reg_it = register_list.iterator(); reg_it.hasNext(); ) {
              RegistrationData data = (RegistrationData)reg_it.next();
              if( data.channel == canceled_channel ) {  //canceled before registration with selector
                reg_it.remove();
                found_in_registration_list = true;
                break;
              }
            }
          }
          finally{
            register_list_mon.exit();
          }
          
          if( !found_in_registration_list ) {
            SelectionKey key = canceled_channel.keyFor( selector );
            if( key != null )  key.cancel();  //cancel the key, since already registered
          }
        }
        cancel_list.clear();        
      }
      finally { 
        cancel_list_mon.exit();
      }
      
      
      //do the actual select
      int count = 0;
      selector_guard.markPreSelectTime();
      try {
        count = selector.select( timeout );
      }
      catch (Throwable t) {  Debug.printStackTrace(t);  }
      
      if( !selector_guard.isSelectorOK( count, 10 ) ) {
        selector = selector_guard.repairSelector( selector );
      }
      
      //notification of ready keys via listener callback
      if( count > 0 ) {
        for( Iterator i = selector.selectedKeys().iterator(); i.hasNext(); ) {
          SelectionKey key = (SelectionKey)i.next();
          i.remove();
          RegistrationData data = (RegistrationData)key.attachment();
          if( key.isValid() ) {
            data.listener.selectSuccess( data.attachment );
            if( INTEREST_OP != OP_READ ) { //read selections don't auto-remove
              key.cancel();
            }
          }
          else {
            data.listener.selectFailure( new Throwable( "key is invalid" ) );
            key.cancel();
            Debug.out( "key is invalid" );
          }
        }
      }
      
      //process new registrations  
      try{
      	register_list_mon.enter();
      
      	for( int i=0; i < register_list.size(); i++ ) {
      	  RegistrationData data = (RegistrationData)register_list.get( i );
      	  try {
      	    if( data.channel.isOpen() && data.channel.keyFor( selector ) == null ) {
      	      data.channel.register( selector, INTEREST_OP, data );
      	    }
      	    else {
      	      data.listener.selectFailure( new Throwable( "channel is closed" ) );
      	      Debug.out( "channel is closed" );
      	    }
      	  }
      	  catch (Throwable t) {
      	    data.listener.selectFailure( t );
      	    Debug.printStackTrace(t);
      	  }
      	}
      	register_list.clear();
        
      }finally{
      	register_list_mon.exit();
      }
      
      return count;
    }
    
    
    private static class RegistrationData {
      private final SocketChannel channel;
      private final VirtualSelectorListener listener;
      private final Object attachment;
      
    	private RegistrationData( SocketChannel channel, VirtualSelectorListener listener, Object attach ) {
    		this.channel = channel;
        this.listener = listener;
        this.attachment = attach;
    	}
    }
    
    
    /**
     * Listener for notification upon socket channel selection.
     */
    protected interface VirtualSelectorListener {
      /**
       * Called when a channel is successfully selected for readyness.
       * @param attachment originally given with the channel's registration
       */
      public void selectSuccess( Object attachment );
      
      /**
       * Called when a channel selection fails.
       * @param msg failure message
       */
      public void selectFailure( Throwable msg );
    }
    
}
