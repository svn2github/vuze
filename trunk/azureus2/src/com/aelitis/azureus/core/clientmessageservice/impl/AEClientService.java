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
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Map;

import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.clientmessageservice.*;
import com.aelitis.azureus.core.networkmanager.*;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;
import com.aelitis.azureus.core.peermanager.messaging.azureus.*;


/**
 * 
 */
public class AEClientService implements ClientMessageService {
	
	private final String address;
	private final int port;
	private final String msg_type_id;
	private ClientConnection connection;
	
	private final AESemaphore block = new AESemaphore( "AEClientService" );
	
  private final VirtualChannelSelector read_selector = new VirtualChannelSelector( VirtualChannelSelector.OP_READ, true );
  private final VirtualChannelSelector write_selector = new VirtualChannelSelector( VirtualChannelSelector.OP_WRITE, true );
	
  private boolean running = true;
  
  private final ArrayList received_messages = new ArrayList();
  
  
	
	public AEClientService( String server_address, int server_port, String _msg_type_id ) {
		this.address = server_address;
		this.port = server_port;
		this.msg_type_id = _msg_type_id;
		
		try {
			AZMessageFactory.registerGenericMapPayloadMessageType( msg_type_id );  //register for incoming type decoding
		}
		catch( MessageException me ) {  /*ignore, since message type probably already registered*/ }
	}
	
	
	

	//NOTE: blocking op
	private void connect() throws IOException {
    final Throwable[] errors = new Throwable[1];
    
    final TCPTransport transport = TransportFactory.createTCPTransport();  //use transport for proxy capabilities
    
    transport.establishOutboundConnection( new InetSocketAddress( address, port ), new TCPTransport.ConnectListener() {  //NOTE: async operation!
    	public void connectAttemptStarted() {  /*nothing*/ }
      
    	public void connectSuccess() {
    		connection = new ClientConnection( transport );
    		block.release();       
    	}
     
    	public void connectFailure( Throwable failure_msg ) {
    		errors[0] = failure_msg;
    		block.release();  
    	}
    });
    
    block.reserve();  //block while waiting for connect
    
    //connect op finished   
    
    if( errors[0] != null ) {  //connect failure
      String error = errors[0].getMessage();
      close();
      throw new IOException( "connect op failed: " + error == null ? "[]" : error );
    }
    
    //start up a thread to run select ops
    AEThread select_thread = new AEThread( "[" +msg_type_id+ "] Client Service Select" ) {
      public void runSupport() {
        while( running ) {
          try{
            read_selector.select( 100 );
            write_selector.select( 100 );
          }
          catch( Throwable t ) {
            Debug.out( "[" +msg_type_id+ "] SelectorLoop() EXCEPTION: ", t );
          }
        }
      }
    };
    select_thread.setDaemon( true );
    select_thread.start();
	}
	
	

	
	public void sendMessage( Map message ) throws IOException {
		if( connection == null ) {  //not yet connected
			connect();
		}
		
		final Throwable[] errors = new Throwable[1];
		
		ClientMessage client_msg = new ClientMessage( msg_type_id, connection, message, new ClientMessageHandler() {
			public String getMessageTypeID(){  return msg_type_id;  }

			public void processMessage( ClientMessage message ) {
				Debug.out( "ERROR: should never be called" );
			}

			public void sendAttemptCompleted( ClientMessage message, boolean success ) {
				if( !success )  errors[0] = new IOException( "message send attempt failed" );
				block.release();
			}
		});
		
		connection.sendMessage( client_msg, new AZGenericMapPayload( msg_type_id, message ) );  //queue message for sending

		//start write selecting now that there's something to send
    write_selector.register( connection.getSocketChannel(), new VirtualChannelSelector.VirtualSelectorListener() {
      public boolean selectSuccess( VirtualChannelSelector selector, SocketChannel sc, Object attachment ) {   	
      	try{
      		boolean more_writes_needed = connection.writeMessages();
      	
      		if( more_writes_needed ) {
      			write_selector.resumeSelects( connection.getSocketChannel() );  //we need to resume since write selects are auto-paused after select op
      		}
      	}
      	catch( Throwable t ) {
      		errors[0] = t;
      		block.release();
      	}

      	return true;
      }

      public void selectFailure( VirtualChannelSelector selector, SocketChannel sc, Object attachment, Throwable msg ) {
      	errors[0] = msg;
    		block.release();
      }
    }, null );  //start writing to the connection
		

		block.reserve();  //block until send completes
		
		//send op finished
		
		write_selector.cancel( connection.getSocketChannel() );		
    
    if( errors[0] != null ) {  //connect failure
      String error = errors[0].getMessage();
      close();
      throw new IOException( "send op failed: " + error == null ? "[]" : error );
    }
	}
	
	
	

	public Map receiveMessage() throws IOException {
		if( connection == null ) {  //not yet connected
			connect();
		}	
		
		if( !received_messages.isEmpty() ) {  //there were still read messages left from the previous read call
			Map recv_msg = (Map)received_messages.remove( 0 );
			return recv_msg;
		}		
		
		final Throwable[] errors = new Throwable[1];
		
		//start read selecting now that there's something to read
    read_selector.register( connection.getSocketChannel(), new VirtualChannelSelector.VirtualSelectorListener() {
      public boolean selectSuccess( VirtualChannelSelector selector, SocketChannel sc, Object attachment ) {   	
      	try{
      		Message[] messages = connection.readMessages();
        	
      		if( messages != null ) {  //message read successfull
      			for( int i=0; i < messages.length; i++ ) {
      				String msg_id = messages[i].getID();

      				if( !msg_id.equals( msg_type_id ) ) {
      					Debug.out( "read message [" +msg_id+ "] does not match msg_type_id [" +msg_type_id+ "]" );
      				}
      				
      				Map payload = ((AZGenericMapPayload)messages[i]).getMapPayload();   				
      				received_messages.add( payload );
      			}
      			
      			block.release();
      		}
      		else {
      			read_selector.resumeSelects( connection.getSocketChannel() );  //we need to resume since these read selects are auto-paused after select op      		
      		}
      	}
      	catch( Throwable t ) {
      		errors[0] = t;
      		block.release();
      	}

      	return true;
      }

      public void selectFailure( VirtualChannelSelector selector, SocketChannel sc, Object attachment, Throwable msg ) {
      	errors[0] = msg;
    		block.release();
      }
    }, null );  //start reading from the connecton
		

		block.reserve();  //block until receive completes
		
		//receive op finished
		
		read_selector.cancel( connection.getSocketChannel() );		
    
    if( errors[0] != null ) {  //connect failure
      String error = errors[0].getMessage();
      close();
      throw new IOException( "receive op failed: " + error == null ? "[]" : error );
    }
		
		Map recv_msg = (Map)received_messages.remove( 0 );
		return recv_msg;
	}
	
	
	
	//no handler notification
	public void close() {
		running = false;
		
		if( connection != null ) {
			connection.close();
		}
	}
	
	
}
