/*
 * Created on Oct 31, 2005
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
package com.aelitis.azureus.core.clientmessageservice.impl;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Map;

import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.clientmessageservice.*;
import com.aelitis.azureus.core.networkmanager.*;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;
import com.aelitis.azureus.core.peermanager.messaging.azureus.*;


/**
 * 
 */
public class AEClientService implements ClientMessageService {
	
	private final String address;
	private final int port;
	private final String msg_type_id;
	private ClientConnection conn;
	
	private final AESemaphore read_block = new AESemaphore( "AEClientService:R" );
	private final AESemaphore write_block = new AESemaphore( "AEClientService:W" );
  
  private final ArrayList received_messages = new ArrayList();  
  
	private final NonBlockingReadWriteService rw_service;
	
	private Throwable error;
	
  
	public AEClientService( String server_address, int server_port, String _msg_type_id ) {

		this( server_address, server_port, 30, _msg_type_id );
	}
  
	public AEClientService( String server_address, int server_port, int timeout, String _msg_type_id ) {
		this.address = server_address;
		this.port = server_port;
		this.msg_type_id = _msg_type_id;
		
		try {
			AZMessageFactory.registerGenericMapPayloadMessageType( msg_type_id );  //register for incoming type decoding
		}
		catch( MessageException me ) {  /*ignore, since message type probably already registered*/ }
		
		rw_service = new NonBlockingReadWriteService( msg_type_id, timeout, 0, new NonBlockingReadWriteService.ServiceListener() {			
			public void messageReceived( ClientMessage message ) {
				received_messages.add( message.getPayload() );
				read_block.release();
			}
			
			public void connectionError( ClientConnection connection ) {
				error = new IOException( "connection error" );
    		read_block.release();
    		write_block.release();
			}
		});
	}
	
	
	

	//NOTE: blocking op
	private void connect() throws IOException {
    final TCPTransport transport = TransportFactory.createTCPTransport();  //use transport for proxy capabilities
    
    transport.establishOutboundConnection( new InetSocketAddress( address, port ), new TCPTransport.ConnectListener() {  //NOTE: async operation!
    	public void connectAttemptStarted() {  /*nothing*/ }
      
    	public void connectSuccess() {
    		conn = new ClientConnection( transport );
    		read_block.release();       
    	}
     
    	public void connectFailure( Throwable failure_msg ) {
    		error = failure_msg;
    		read_block.release();  
    	}
    });
    
    read_block.reserve();  //block while waiting for connect
    
    //connect op finished   
    
    if( error != null ) {  //connect failure
      close();
      throw new IOException( "connect op failed: " + error.getMessage() == null ? "[]" : error.getMessage() );
    }
        
    rw_service.addClientConnection( conn );  //register for read/write handling
	}
	
	

	
	public void sendMessage( Map message ) throws IOException {
		if( conn == null ) {  //not yet connected
			connect();
		}
		
		ClientMessage client_msg = new ClientMessage( msg_type_id, conn, message, new ClientMessageHandler() {
			public String getMessageTypeID(){  return msg_type_id;  }

			public void processMessage( ClientMessage message ) {
				Debug.out( "ERROR: should never be called" );
			}

			public void sendAttemptCompleted( ClientMessage message, boolean success ) {
				if( !success )  error = new IOException( "message send attempt failed" );
				write_block.release();
			}
		});
		
		rw_service.sendMessage( client_msg );  //queue message for sending	

		write_block.reserve();  //block until send completes
		
		//send op finished
    
    if( error != null ) {  //connect failure
      close();
      throw new IOException( "send op failed: " + error.getMessage() == null ? "[]" : error.getMessage() );
    }
	}
	
	
	

	public Map receiveMessage() throws IOException {
		if( conn == null ) {  //not yet connected
			connect();
		}	
		
		if( !received_messages.isEmpty() ) {  //there were still read messages left from the previous read call
			Map recv_msg = (Map)received_messages.remove( 0 );
			return recv_msg;
		}

		read_block.reserve();  //block until receive completes
		
		//receive op finished	
    
    if( error != null ) {  //connect failure
      close();
      throw new IOException( "receive op failed: " + error.getMessage() == null ? "[]" : error.getMessage() );
    }
		
		Map recv_msg = (Map)received_messages.remove( 0 );
		return recv_msg;
	}
	
	
	
	//no handler notification
	public void close() {
		if( conn != null ) {
			rw_service.removeClientConnection( conn );
			conn.close();
		}
		rw_service.destroy();
	}
	
	
}
