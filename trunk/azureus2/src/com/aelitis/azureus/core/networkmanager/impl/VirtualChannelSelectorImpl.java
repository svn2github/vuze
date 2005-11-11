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
package com.aelitis.azureus.core.networkmanager.impl;


import java.nio.channels.*;
import java.util.*;

import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.networkmanager.VirtualChannelSelector;



/**
 * Provides a simplified and safe (selectable-channel) socket single-op selector.
 */
public class VirtualChannelSelectorImpl {
    private Selector selector;
    private final SelectorGuard selector_guard;
    
    private final LinkedList 	register_cancel_list 		= new LinkedList();
    private final AEMonitor 	register_cancel_list_mon	= new AEMonitor( "VirtualChannelSelector:RCL");

    private final HashMap paused_states = new HashMap();
    
    private final int 		INTEREST_OP;
    private final boolean	pause_after_select;

    private final VirtualChannelSelector parent;
    
    
    private int[] select_counts = new int[ 50 ];
    private int round = 0;
    
    private volatile boolean	destroyed;
    
    
    public VirtualChannelSelectorImpl( VirtualChannelSelector _parent, int _interest_op, boolean _pause_after_select ) {	
      this.parent = _parent;
      INTEREST_OP = _interest_op;
      
      pause_after_select	= _pause_after_select;
      
      String type;
      switch( INTEREST_OP ) {
        case VirtualChannelSelector.OP_CONNECT:
          type = "OP_CONNECT";  break;
        case VirtualChannelSelector.OP_READ:
          type = "OP_READ";  break;
        default:
          type = "OP_WRITE";  break;
      }
      
      
      selector_guard = new SelectorGuard( type, new SelectorGuard.GuardListener() {
        public boolean safeModeSelectEnabled() {
          return parent.isSafeSelectionModeEnabled();
        }
        
        public void spinDetected() {
          closeExistingSelector();
          try {  Thread.sleep( 1000 );  }catch( Throwable x ) {x.printStackTrace();}
          parent.enableSafeSelectionMode();
        }
        
        public void failureDetected() {
          try {  Thread.sleep( 10000 );  }catch( Throwable x ) {x.printStackTrace();}
          closeExistingSelector();
          try {  Thread.sleep( 1000 );  }catch( Throwable x ) {x.printStackTrace();}
          selector = openNewSelector();
        }
      });
      
      selector = openNewSelector();
    }
    
  
    
    private Selector openNewSelector() {
      Selector sel = null;
      
      try {
        sel = Selector.open();
      }
      catch (Throwable t) {
        Debug.out( "ERROR: caught exception on Selector.open()", t );
        
        try {  Thread.sleep( 3000 );  }catch( Throwable x ) {x.printStackTrace();}
        
        int fail_count = 1;
        
        while( fail_count < 10 ) {
          try {
            sel = Selector.open();
            break;
          }
          catch( Throwable f ) {
            Debug.out( f );
            fail_count++;
            try {  Thread.sleep( 3000 );  }catch( Throwable x ) {x.printStackTrace();}
          }
        }
        
        if( fail_count < 10 ) { //success ! 
          Debug.out( "NOTICE: socket Selector successfully opened after " +fail_count+ " failures." );
        }
        else {  //failure
          LGLogger.logRepeatableAlert( LGLogger.AT_ERROR ,"ERROR: socket Selector.open() failed 10 times in a row, aborting.\nAzureus / Java is likely being firewalled!" );
        }
      }
      
      return sel;
    }
    
    
    
    
    


    public void 
	register( 
		SocketChannel 			channel, 
		VirtualChannelSelector.VirtualSelectorListener listener, 
		Object attachment ) 
    {
    	addRegOrCancel( new RegistrationData( channel, listener, attachment ));
    }
    


    
    public void pauseSelects( SocketChannel channel ) {
      
      if( channel == null ) {
        return;
      }
      
      SelectionKey key = channel.keyFor( selector );
      
      if( key != null && key.isValid() ) {
        key.interestOps( key.interestOps() & ~INTEREST_OP );
      }
      else {  //channel not (yet?) registered
        if( channel.isOpen() ) {  //only bother if channel has not already been closed
          try{  register_cancel_list_mon.enter();
          
            paused_states.put( channel, new Boolean( true ) );  //ensure the op is paused upon reg select-time reg

          }
          finally{  register_cancel_list_mon.exit();  }
        }
      }
    }
    


    
    public void resumeSelects( SocketChannel channel ) {
      if( channel == null ) {
        Debug.printStackTrace( new Exception( "resumeSelects():: channel == null" ) );
        return;
      }
      
      SelectionKey key = channel.keyFor( selector );
      
      if( key != null && key.isValid() ) {
        key.interestOps( key.interestOps() | INTEREST_OP );
      }
      else {  //channel not (yet?) registered
        try{  register_cancel_list_mon.enter();
          paused_states.remove( channel );  //check if the channel's op has been already paused before select-time reg
        }
        finally{  register_cancel_list_mon.exit();  }
      }
      
      //try{
      //  selector.wakeup();
      //}
      //catch( Throwable t ) {  Debug.out( "selector.wakeup():: caught exception: ", t );   }
    }



    
    public void 
	cancel( 
		SocketChannel channel ) 
    {
      pauseSelects( channel );
    	addRegOrCancel( channel ); 
    }
   
    
    
    
    
    
    private void 
    addRegOrCancel( 
    	Object	obj_to_add ) 
    {
    	if ( destroyed ){
    		
    		if ( obj_to_add instanceof SocketChannel ){
    		
    				// don't worry too much about cancels
    		}else{
    			
    			Debug.out( "addRegOrCancel called after selector destroyed" );
    		}
    	}
    	
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
    
    
    
    public int select( long timeout ) {
    	
      long select_start_time = SystemTime.getCurrentTime();
      
      if( selector == null ) {
        Debug.out( "VirtualChannelSelector.select() op called with null selector" );
        try {  Thread.sleep( 3000 );  }catch( Throwable x ) {x.printStackTrace();}
        return 0;
      } 
      
      if( !selector.isOpen()) {
          Debug.out( "VirtualChannelSelector.select() op called with closed selector" );
          try {  Thread.sleep( 3000 );  }catch( Throwable x ) {x.printStackTrace();}
          return 0;
      }  
      
      	// store these when they occur so they can be raised *outside* of the monitor to avoid
      	// potential deadlocks
      
      RegistrationData	select_fail_data	= null;
      Throwable 		select_fail_excep	= null;
      
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
            	
            if( data == null ) {
              Debug.out( "data == null" );
            }
            
            if( data.channel == null ) {
              Debug.out( "data.channel == null" );
            }
            
            try {
              if( data.channel.isOpen() ){
                	
                // see if already registered
                SelectionKey key = data.channel.keyFor( selector );
                  
                if ( key != null && key.isValid() ) {  //already registered
                  key.attach( data );
                  key.interestOps( key.interestOps() | INTEREST_OP );  //ensure op is enabled
                }
                else{
                  data.channel.register( selector, INTEREST_OP, data );
                }
                  
                //check if op has been paused before registration moment
                Object paused = paused_states.get( data.channel );
                  
                if( paused != null ) {
                  pauseSelects( data.channel );  //pause it
                }
              }
              else{
            	
              	select_fail_data	= data;
              	select_fail_excep	= new Throwable( "select registration: channel is closed" );
              	
              }
            }catch (Throwable t){
              
            	Debug.printStackTrace(t);
           	    
           		select_fail_data	= data;
           		select_fail_excep	= t;
            } 	
          }
        }
        
        paused_states.clear();  //reset after every registration round
               
      }finally { 
      	
      	register_cancel_list_mon.exit();
      }
      
      if ( select_fail_data != null ){
      	
      	try{
	      	select_fail_data.listener.selectFailure(
	      	        parent, 
					select_fail_data.channel, 
					select_fail_data.attachment, 
					select_fail_excep );
	      	
      	}catch( Throwable e ){
      		
      		Debug.printStackTrace( e );
      	}
      }
      
  
      //do the actual select
      int count = 0;
      selector_guard.markPreSelectTime();
      try {
        count = selector.select( timeout );
      }
      catch (Throwable t) {
        Debug.out( "Caught exception on selector.select() op: " +t.getMessage(), t );
        try {  Thread.sleep( timeout );  }catch(Throwable e) { e.printStackTrace(); }
      }
      
      	// do this after the select so that any pending cancels (prior to destroy) are processed
      	// by the selector before we kill it
      
 	  if ( destroyed ){
  		
 	    closeExistingSelector();
 	    	
 	    return( 0 );
 	  }
 	   	  
      /*
      if( INTEREST_OP == VirtualChannelSelector.OP_READ ) {  //TODO
      	select_counts[ round ] = count;
      	round++;
      	if( round == select_counts.length ) {
      		StringBuffer buf = new StringBuffer( select_counts.length * 3 );
      		
      		buf.append( "select_counts=" );
      		for( int i=0; i < select_counts.length; i++ ) {
      			buf.append( select_counts[i] );
      			buf.append( ' ' );
      		}
      		
      		//System.out.println( buf.toString() );
      		round = 0;
      	}
      }
      */
      
      selector_guard.verifySelectorIntegrity( count, SystemTime.TIME_GRANULARITY_MILLIS /2 );
      
      if( !selector.isOpen() )  return count;
      
      //notification of ready keys via listener callback
      for( Iterator i = selector.selectedKeys().iterator(); i.hasNext(); ) {
        SelectionKey key = (SelectionKey)i.next();
        i.remove();
        RegistrationData data = (RegistrationData)key.attachment();

        if( key.isValid() ) {
          if( (key.interestOps() & INTEREST_OP) == 0 ) {  //it must have been paused between select and notification
            continue;
          }            
            
          if( pause_after_select ) { 
            key.interestOps( key.interestOps() & ~INTEREST_OP );
          }
                        
          boolean	progress_made = data.listener.selectSuccess( parent, data.channel, data.attachment );
            
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
            				e.printStackTrace();
                }
              }
            }
          }
        }
        else {
          key.cancel();
          data.listener.selectFailure( parent, data.channel, data.attachment, new Throwable( "key is invalid" ) );
          // can get this if socket has been closed between select and here
        }
      }
      
      
      long time_diff = SystemTime.getCurrentTime() - select_start_time;
      
      if( time_diff < timeout && time_diff >= 0 ) {  //ensure that it always takes at least 'timeout' time to complete the select op
      	try {  Thread.sleep( timeout - time_diff );  }catch(Throwable e) { e.printStackTrace(); }      
      }
      
      return count;
    }
    
    	/**
    	 * Note that you have to ensure that a select operation is performed on the normal select
    	 * loop *after* destroying the selector to actually cause the destroy to occur
    	 */
    
    public void
    destroy()
    {
    	destroyed	= true;
    }
    
    private void closeExistingSelector() {
      for( Iterator i = selector.keys().iterator(); i.hasNext(); ) {
        SelectionKey key = (SelectionKey)i.next();
        RegistrationData data = (RegistrationData)key.attachment();
        data.listener.selectFailure( parent, data.channel, data.attachment, new Throwable( "selector destroyed" ) );
      }
      
      try{
        selector.close();
      }
      catch( Throwable t ) { t.printStackTrace(); }
    }
    
    
    
    
    private static class RegistrationData {
        private final SocketChannel channel;
        private final VirtualChannelSelector.VirtualSelectorListener listener;
        private final Object attachment;
        
        private int non_progress_count;
        
      	private RegistrationData( SocketChannel _channel, VirtualChannelSelector.VirtualSelectorListener _listener, Object _attachment ) {
      		channel 		= _channel;
      		listener		= _listener;
      		attachment 		= _attachment;
      	}
      }
          
}
