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
    public static final int OP_ACCEPT = SelectionKey.OP_ACCEPT;
    public static final int OP_CONNECT = SelectionKey.OP_CONNECT;
    public static final int OP_READ = SelectionKey.OP_READ;
    public static final int OP_WRITE = SelectionKey.OP_WRITE;
  
  
    private static final int SELECTOR_FAIL_COUNT_MAX = 100;
    
    private Selector selector;
    private final SelectorGuard selector_guard;
    private final ArrayList register_list = new ArrayList();
    private final int interest_op;


    /**
     * Create a new virtual selectable-channel selector,
     * selecting over the given interest-op(s).
     * @param interest_op operation set of OP_ACCEPT, OP_CONNECT, OP_READ, OP_WRITE
     */
    protected VirtualChannelSelector( int interest_op ) {
      this.interest_op = interest_op;
      selector_guard = new SelectorGuard( SELECTOR_FAIL_COUNT_MAX );
    	try {
    		selector = Selector.open();
    	}
      catch (Exception e) {  Debug.out( e.getMessage() );  }
    }
    
  
    /**
     * Register the given selectable channel, using the given listener for notification
     * of completed select operation.
     * @param channel socket to listen for
     * @param listener op-complete listener
     * @param attachment object to be passed back with listener notification
     */
    protected void register( SocketChannel channel, VirtualSelectorListener listener, Object attachment ) {
      synchronized( register_list ) {
        register_list.add( new RegistrationData( channel, listener, attachment ) );
      }
    }
    
    
    /**
     * Run a virtual select() operation, with the given selection timeout value;
     *   (1) new registrations are processed;
     *   (2) the select operation is performed;
     *   (3) listener notification of completed selects
     * @param timeout in ms
     * @return number of sockets selected
     */
    protected int select( long timeout ) {
      int count = 0;
                  
      //do the actual select
      selector_guard.markPreSelectTime();
      try {
        count = selector.select( timeout );
      }
      catch (Exception e) {  Debug.out( e.getMessage() );  }
      
      if( !selector_guard.isSelectorOK( count, timeout / 2 ) ) {
        selector = selector_guard.repairSelector( selector );
      }
      
      //notification of ready keys via listener callback
      if( count > 0 ) {
        for( Iterator i = selector.selectedKeys().iterator(); i.hasNext(); ) {
          SelectionKey key = (SelectionKey)i.next();
          i.remove();
          if( key.isValid() ) {
            RegistrationData data = (RegistrationData)key.attachment();
            data.listener.channelSuccessfullySelected( data.attachment );
            key.cancel();
          }
          else Debug.out( "key is invalid!" );
        }
      }
      
      //process new registrations  
      synchronized( register_list ) {
        if( !register_list.isEmpty() ) {
          for( int i=0; i < register_list.size(); i++ ) {
            RegistrationData data = (RegistrationData)register_list.get( i );
            try {
              if( data.channel.isOpen() ) {
                data.channel.register( selector, interest_op, data );
              }
            }
            catch (Throwable t) {  t.printStackTrace();  }
          }
          register_list.clear();
        }
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
      public void channelSuccessfullySelected( Object attachment );
    }
    
}
