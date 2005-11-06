/*
 * Created on Nov 3, 2005
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

import java.nio.channels.SocketChannel;
import java.util.*;

import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.clientmessageservice.impl.*;
import com.aelitis.azureus.core.networkmanager.VirtualChannelSelector;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.azureus.AZGenericMapPayload;


/**
 * 
 */
public class NonBlockingReadWriteService {
	
  private final VirtualChannelSelector read_selector = new VirtualChannelSelector( VirtualChannelSelector.OP_READ, false );
  private final VirtualChannelSelector write_selector = new VirtualChannelSelector( VirtualChannelSelector.OP_WRITE, true );
  
  private final ArrayList connections = new ArrayList();
  private final AEMonitor connections_mon = new AEMonitor( "connections" );
  
  private final ServiceListener listener;
  private final String service_name;
  private volatile boolean running = true;
  
  private long last_timeout_check_time = 0;
  private static final int TIMEOUT_CHECK_INTERVAL_MS = 10*1000;  //check for timeouts every 10sec
  private final int activity_timeout_period_ms;
  
  
  
	public NonBlockingReadWriteService( String _service_name, int timeout, ServiceListener _listener ) {
		this.service_name = _service_name;
		this.listener = _listener;

		if( timeout < TIMEOUT_CHECK_INTERVAL_MS /1000 )  timeout = TIMEOUT_CHECK_INTERVAL_MS /1000;
		this.activity_timeout_period_ms = timeout *1000;		
		
    AEThread select_thread = new AEThread( "[" +service_name+ "] Service Select" ) {
      public void runSupport() {
        while( running ) {
          try{
            read_selector.select( 50 );
            write_selector.select( 50 );
          }
          catch( Throwable t ) {
            Debug.out( "[" +service_name+ "] SelectorLoop() EXCEPTION: ", t );
          }
          
          doConnectionTimeoutChecks();
        }
      }
    };
    select_thread.setDaemon( true );
    select_thread.start();
	}
	
	
	
	public void destroy() {
		connections.clear();
		running = false;
	}
	
	
	
	public void addClientConnection( ClientConnection connection ) {
		//add to active list
    try {  connections_mon.enter();
      connections.add( connection );
    }
    finally {  connections_mon.exit();  }
    
    registerForSelection( connection );
	}
	
	
	
	public void removeClientConnection( ClientConnection connection ) {
    read_selector.cancel( connection.getSocketChannel() );
    write_selector.cancel( connection.getSocketChannel() );
    
    //remove from active list
    try {  connections_mon.enter();
      connections.remove( connection );
    }
    finally {  connections_mon.exit();  }
	}
	
	
	
	
	
	private void registerForSelection( final ClientConnection client ) {		
		//READS
		VirtualChannelSelector.VirtualSelectorListener read_listener = new VirtualChannelSelector.VirtualSelectorListener() {
			//SUCCESS
      public boolean selectSuccess( VirtualChannelSelector selector, SocketChannel sc, Object attachment ) {     	
      	try{
      		Message[] messages = client.readMessages();
      	
      		if( messages != null ) {    		
      			for( int i=0; i < messages.length; i++ ) {
      				AZGenericMapPayload msg = (AZGenericMapPayload)messages[i];
      				ClientMessage client_msg = new ClientMessage( msg.getID(), client, msg.getMapPayload(), null );  //note no handler. we let the listener attach it		
      				listener.messageReceived( client_msg );	
      			}
      		}	
      	}
      	catch( Throwable t ) {
      		System.out.println( "[" +new Date()+ "] Connection read error [" +sc.socket().getInetAddress()+ "]: " +t.getMessage() );
      		listener.connectionError( client );
      	}

        return true;
      }

      //FAILURE
      public void selectFailure( VirtualChannelSelector selector, SocketChannel sc, Object attachment, Throwable msg ) {
        msg.printStackTrace();
        listener.connectionError( client );
      }
    };
    
    
    //WRITES
    final VirtualChannelSelector.VirtualSelectorListener write_listener = new VirtualChannelSelector.VirtualSelectorListener() {
      public boolean selectSuccess( VirtualChannelSelector selector, SocketChannel sc, Object attachment ) {   	
      	try{
      		boolean more_writes_needed = client.writeMessages();
      	
      		if( more_writes_needed ) {
      			write_selector.resumeSelects( client.getSocketChannel() );  //we need to resume since write selects are auto-paused after select op
      		}
      	}
      	catch( Throwable t ) {
          System.out.println( "[" +new Date()+ "] Connection write error [" +sc.socket().getInetAddress()+ "]: " +t.getMessage() );
          listener.connectionError( client );
      	}

      	return true;
      }

      public void selectFailure( VirtualChannelSelector selector, SocketChannel sc, Object attachment, Throwable msg ) {
        msg.printStackTrace();
        listener.connectionError( client );
      }
    };

    write_selector.register( client.getSocketChannel(), write_listener, null );  //start writing back to the connection
    write_selector.pauseSelects( client.getSocketChannel() );   //wait until we've got something to send before selecting
    
    read_selector.register( client.getSocketChannel(), read_listener, null );  //start reading from the connection
	}
	
	
  private void doConnectionTimeoutChecks() {
    //check timeouts
    long time = System.currentTimeMillis();
    if( time < last_timeout_check_time || time - last_timeout_check_time > TIMEOUT_CHECK_INTERVAL_MS ) {
      ArrayList timed_out = new ArrayList();
      
      try {  connections_mon.enter();
        long current_time = System.currentTimeMillis();
    
        for( int i=0; i < connections.size(); i++ ) {
          ClientConnection vconn = (ClientConnection)connections.get( i );
        
          if( current_time < vconn.getLastActivityTime() ) {  //time went backwards!
            vconn.resetLastActivityTime();
          }
          else if( current_time - vconn.getLastActivityTime() > activity_timeout_period_ms ) {
            timed_out.add( vconn );   //do actual removal outside the check loop
          }
        }
      }
      finally {  connections_mon.exit();  }
      
      for( int i=0; i < timed_out.size(); i++ ) {  
        ClientConnection vconn = (ClientConnection)timed_out.get( i );
        System.out.println( "[" +new Date()+ "] Connection timed out [" +vconn.getSocketChannel().socket().getInetAddress()+ "]" );
        listener.connectionError( vconn );
      }
      
      last_timeout_check_time = System.currentTimeMillis();
    }
  }
	
	

  
  
  public void sendMessage( ClientMessage message ) {
		ClientConnection vconn = message.getClient();
		
		boolean still_connected;
		
		try {  connections_mon.enter();
			still_connected = connections.contains( vconn );
		}
		finally {  connections_mon.exit();  }
		
		if( !still_connected ) {
			System.out.println( "[" +new Date()+ "] Connection message send error [connection no longer connected]" );
			message.getHandler().sendAttemptCompleted( message, false );
			//listener.connectionError( vconn ); //no need to call this, as there is no connection to remove
      return;
		}
		
		Message reply = new AZGenericMapPayload( message.getMessageID(), message.getPayload() );

		vconn.sendMessage( message, reply );
		
		write_selector.resumeSelects( vconn.getSocketChannel() );  //start write selecting now that there's something to send
  }
  
  
	

	public interface ServiceListener {

		public void messageReceived( ClientMessage message );
		
		public void connectionError( ClientConnection connection );
		
	}
	
}
