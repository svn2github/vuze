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
    public static final int OP_CONNECT 	= SelectionKey.OP_CONNECT;
    public static final int OP_READ 	= SelectionKey.OP_READ;
    public static final int OP_WRITE 	= SelectionKey.OP_WRITE;
  
  
    private static final int SELECTOR_FAIL_COUNT_MAX = 10000;  // a real selector spin will easily reach this
    
    private Selector selector;
    private final SelectorGuard selector_guard;
    
    private final LinkedList 	register_cancel_list 		= new LinkedList();
    private final AEMonitor 	register_cancel_list_mon	= new AEMonitor( "VirtualChannelSelector:RCL");

    
    private final int 		INTEREST_OP;
    private final boolean	deregister_after_select_success;
    

    /**
     * Create a new virtual selectable-channel selector,
     * selecting over the given interest-op(s).
     * OP_READ will be left registered after select success, all others are cancelled
     * @param interest_op operation set of OP_ACCEPT, OP_CONNECT, OP_READ, OP_WRITE
     */
    
    public VirtualChannelSelector( int interest_op ) {
    	this( interest_op, interest_op != SelectionKey.OP_READ );
    }
    
    /**
     * Create a new virtual selectable-channel selector,
     * selecting over the given interest-op(s).
     * @param _interest_op operation set of OP_ACCEPT, OP_CONNECT, OP_READ, OP_WRITE
     * @param _deregister_after_select_success	whether or not to deregister after select success  
     */
    
    public 
	VirtualChannelSelector( 
		int 		_interest_op,
		boolean		_deregister_after_select_success ) 
    {	 
      INTEREST_OP = _interest_op;
      
      deregister_after_select_success	= _deregister_after_select_success;
      
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
    public void 
	register( 
		SocketChannel 			channel, 
		VirtualSelectorListener listener, 
		Object attachment ) 
    {
    	addRegOrCancel( new RegistrationData( channel, listener, attachment ));
 
		selector.wakeup();
    }
    
	    /**
	     * Cancel the select request.
	     * Once canceled, the channel is unregistered and the listener will never be invoked.
	     * @param channel channel originally registered
	     */
    
    public void 
	cancel( 
		SocketChannel channel ) 
    {
    	addRegOrCancel( channel ); 
    }
   
    private void 
    addRegOrCancel( 
    	Object	obj_to_add ) 
    {
    	try{
    		register_cancel_list_mon.enter();
      	   		
    			// ensure that there's only one operation outstanding for a given channel
    			// at any one time (the latest operation requested )
    		
    		for (Iterator it = register_cancel_list.iterator();it.hasNext();){
    			
    			Object	obj = it.next();
    			
    			boolean	remove_it	= false;
    		
    			if ( obj_to_add instanceof SocketChannel ){
    				
    				if ( obj_to_add == obj ||
    						(	obj instanceof RegistrationData &&
    								((RegistrationData)obj).channel == obj_to_add )){
    					
    					// remove existing cancel or register
    				
    					remove_it = true;
   				
     				}
    				
    			}else{
    				
    				RegistrationData	rd = (RegistrationData)obj_to_add;
    				
    				if ( rd.channel == obj ||
       						(	obj instanceof RegistrationData &&
    								((RegistrationData)obj).channel == rd.channel )){
 						
    					remove_it = true;
    					
    				}
    			}
    			
    			if ( remove_it ){
    				
    				it.remove();
    				
    				break;
    			}
    		}
    		   			
  			register_cancel_list.add( obj_to_add );
    		
    	}finally{
    		
    		register_cancel_list_mon.exit();
    	}
    }
    
    
    
    /**
     * Run a virtual select() operation, with the given selection timeout value;
     *   (1) cancellations are processed
     *   (2) the select operation is performed;
     *   (3) listener notification of completed selects
     *   (4) new registrations are processed;
     * @param timeout in ms; if zero, block indefinitely
     * @return number of sockets selected
     */
    public int select( long timeout ) {
      //process cancellations
      try {
      	register_cancel_list_mon.enter();
        
      		// don't use an iterator here as it is possible that error notifications to listeners
      		// can result in the addition of a cancel request.
      		// Note that this can only happen for registrations, and this *should* only result in
      		// possibly a cancel being added (i.e. not a further registration), hence this can't
      		// loop. Also note the approach of removing the entry before processing. This is so
      		// that the logic used when adding a cancel (the removal of any matching entries) does
      		// not cause the entry we're processing to be removed
      	
        while( register_cancel_list.size() > 0 ){
        	
         Object	obj = register_cancel_list.remove(0);
         
         if ( obj instanceof SocketChannel ){
         	
         		// process cancellation
         	
         	SocketChannel	canceled_channel = (SocketChannel)obj;
  
         	try{
	         	SelectionKey key = canceled_channel.keyFor( selector );
	            
	            if( key != null ){
	            	
	            	key.cancel();  //cancel the key, since already registered
	            }
	            
         	}catch( Throwable e ){
         		
         		Debug.printStackTrace(e);
         	}
         }else{
         		//process new registrations  
 
         	RegistrationData data = (RegistrationData)obj;
            	
            try {
                if( data.channel.isOpen() ){
                	
                		// see if already registered
                	
                  SelectionKey key = data.channel.keyFor( selector );
                  
                  if ( key != null && key.isValid()){
                  	
                  	key.attach( data );
                  	
                  }else{
                  	
                  	data.channel.register( selector, INTEREST_OP, data );
                  }
                  
                }else{
            	
                	data.listener.selectFailure( this, data.channel, data.attachment, new Throwable( "channel is closed" ) );
            	    //Debug.out( "channel is closed" );
            	}
           	}catch (Throwable t){
                     	    
           	    Debug.printStackTrace(t);
 
           		try{
           			data.listener.selectFailure( this, data.channel, data.attachment, t );
           			
           		}catch( Throwable e ){
           			
           			Debug.printStackTrace(e);
           		}
             } 	
         }
        }
               
      }finally { 
      	register_cancel_list_mon.exit();
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
            if( deregister_after_select_success ) { 
                key.cancel();
            }
            
            
            if( INTEREST_OP == OP_READ && !key.isReadable() ) {
              Debug.out( "!key.isReadable()" );
            }
            
            boolean	progress_made = data.listener.selectSuccess( this, data.channel, data.attachment );
            
            if ( progress_made ){
            	
            	data.non_progress_count = 0;
            }else{
            	
            	data.non_progress_count++;
            	
            	if ( data.non_progress_count %100 == 0 && data.non_progress_count > 0 ){
            		
            		System.out.println( 
            				"VirtualChannelSelector: No progress for op " + INTEREST_OP + ": " + data.non_progress_count +
            				", socket: open = " + data.channel.isOpen() + ", connected = " + data.channel.isConnected());
            		
            		if ( data.non_progress_count == 1000 ){
            			
            			Debug.out( "No progress for " + data.non_progress_count + ", closing connection" );
            			
            			try{
            				data.channel.close();
            				
            			}catch( Throwable e ){
            				
            			}
            		}
            	}
            }
           }
          else {
            key.cancel();
            data.listener.selectFailure( this, data.channel, data.attachment, new Throwable( "key is invalid" ) );
            // can get this if socket has been closed between select and here
            //Debug.out( "key is invalid" );
          }
        }
      }
      
      return count;
    }
    
    
    private static class RegistrationData {
        private final SocketChannel channel;
        private final VirtualSelectorListener listener;
        private final Object attachment;
        
        private int non_progress_count;
        
      	private RegistrationData( SocketChannel _channel, VirtualSelectorListener _listener, Object _attachment ) {
      		channel 		= _channel;
      		listener		= _listener;
      		attachment 		= _attachment;
      	}
      }
      
      
      /**
       * Listener for notification upon socket channel selection.
       */
      public interface 
  	VirtualSelectorListener 
  	{
        /**
         * Called when a channel is successfully selected for readyness.
         * @param attachment originally given with the channel's registration
         * @return indicator of whether or not any 'progress' was made due to this select
         * 			e.g. read-select -> read >0 bytes, write-select -> wrote > 0 bytes
         */
      	
        public boolean 
		selectSuccess( 
			VirtualChannelSelector	selector, 
			SocketChannel 			sc, 
			Object 					attachment );
        
        /**
         * Called when a channel selection fails.
         * @param msg failure message
         */
        public void selectFailure( VirtualChannelSelector	selector, SocketChannel sc, Object attachment, Throwable msg );
      }
    
}
