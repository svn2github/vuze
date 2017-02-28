/*
 * Created on Sep 28, 2004
 * Created by Alon Rohter
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
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
 */

package com.aelitis.azureus.core.networkmanager.impl;

import java.io.IOException;

import org.gudy.azureus2.core3.util.AEDiagnostics;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.networkmanager.EventWaiter;
import com.aelitis.azureus.core.networkmanager.NetworkConnectionBase;
import com.aelitis.azureus.core.networkmanager.RateHandler;
import com.aelitis.azureus.core.peermanager.messaging.Message;


/**
 * A fast write entity backed by a single peer connection.
 */
public class SinglePeerUploader implements RateControlledEntity {
  
  private final NetworkConnectionBase connection;
  private final RateHandler rate_handler;
  
  public SinglePeerUploader( NetworkConnectionBase connection, RateHandler rate_handler ) {
    this.connection = connection;
    this.rate_handler = rate_handler;
  }
    
	public RateHandler 
	getRateHandler() 
	{
		return( rate_handler );
	}
	
////////////////RateControlledWriteEntity implementation ////////////////////
  
  public boolean canProcess(EventWaiter waiter) {
    if( !connection.getTransportBase().isReadyForWrite(waiter) )  {
      return false;  //underlying transport not ready
    }
    if( connection.getOutgoingMessageQueue().getTotalSize() < 1 ) {
      return false;  //no data to send
    }
    int[] allowed = rate_handler.getCurrentNumBytesAllowed();
    
    if( allowed[0] < 1 ){
    	
    	boolean protocol_is_free = allowed[1] > 0;
    	
    	if ( protocol_is_free ){
    		
    		Message first = connection.getOutgoingMessageQueue().peekFirstMessage();
    		
    		if ( first != null && first.getType() == Message.TYPE_PROTOCOL_PAYLOAD ){
    			
    			return( true );
    			
    		}else{
    			
    			return( false );
    		}
    	}else{
    		
    		return( false );
    	}
    }
    return true;
  }
  
  public int doProcessing(EventWaiter waiter, int max_bytes ) {
    if( !connection.getTransportBase().isReadyForWrite(waiter) )  {
      //Debug.out("dW:not ready"); happens sometimes, just live with it as non-fatal
      return 0;
    }
    
    int[] allowed = rate_handler.getCurrentNumBytesAllowed();
    
    int num_bytes_allowed = allowed[0];
    
    boolean	protocol_is_free = allowed[1] > 0;
    
    if ( num_bytes_allowed < 1 ){
    	
    	if ( protocol_is_free ){
    		
    		num_bytes_allowed = 0;	// in case negative
    		
    	}else{
    	
    		return( 0 );
    	}
    }
    
	if ( max_bytes > 0 && max_bytes < num_bytes_allowed ){
		
		num_bytes_allowed = max_bytes;
	}
	
    int num_bytes_available = connection.getOutgoingMessageQueue().getTotalSize();
    
    if ( num_bytes_available < 1 ){
    	
    	if ( !connection.getOutgoingMessageQueue().isDestroyed()){
    		
    		//Debug.out("dW:not avail"); happens sometimes, just live with it as non-fatal
    	}
    	
    	return 0;
    }
    
    int num_bytes_to_write = num_bytes_allowed > num_bytes_available ? num_bytes_available : num_bytes_allowed;
    
    //int mss = NetworkManager.getTcpMssSize();
    //if( num_bytes_to_write > mss )  num_bytes_to_write = mss;
    
    int[] written;
    
    try {
    	
      written = connection.getOutgoingMessageQueue().deliverToTransport( num_bytes_to_write, protocol_is_free, false );
      
    }catch( Throwable e ) {
      
    	written = new int[2];
    	
    	if( AEDiagnostics.TRACE_CONNECTION_DROPS ) {
    		if( e.getMessage() == null ) {
    			Debug.out( "null write exception message: ", e );
    		}
    		else {
    			if(!e.getMessage().contains(
						"An existing connection was forcibly closed by the remote host") &&
						!e.getMessage().contains("Connection reset by peer") &&
						!e.getMessage().contains("Broken pipe") &&
						!e.getMessage().contains(
							"An established connection was aborted by the software in your host machine")) {

    				System.out.println( "SP: write exception [" +connection.getTransportBase().getDescription()+ "]: " +e.getMessage() );
    			}
    		}
    	}

    	if (! (e instanceof IOException )){

    		Debug.printStackTrace(e);
    	}

    	connection.notifyOfException( e );
    	return 0;
    }
    
    int data_bytes_written		= written[0];
    int protocol_bytes_written	= written[1];

    int total_written =data_bytes_written + protocol_bytes_written;
    
    if ( total_written < 1 ){
    	
    	return 0;
    }
    
    rate_handler.bytesProcessed( data_bytes_written, protocol_bytes_written );
    
    return( total_written );
  }
  
  public int getPriority() {
    return RateControlledEntity.PRIORITY_NORMAL;
  }

  public boolean 
  getPriorityBoost()
  {
	  return( connection.getOutgoingMessageQueue().getPriorityBoost());
  }
  
  public long
  getBytesReadyToWrite()
  {
	  return( connection.getOutgoingMessageQueue().getTotalSize());
  }
  
  public int
  getConnectionCount( EventWaiter waiter )
  {
	  return( 1 );
  }
  
  public int
  getReadyConnectionCount(
	EventWaiter	waiter )
  {
	  if ( connection.getTransportBase().isReadyForWrite(waiter)){
		  
		  return( 1 );
	  }
	  
	  return( 0 );
  }

  public String
  getString()
  {
	  return( "SPU: bytes_allowed=" + rate_handler.getCurrentNumBytesAllowed() + " " + connection.getString());
  }
  
}
