/*
 * Created on Sep 13, 2004
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

import java.net.*;
import java.nio.channels.*;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.*;



/**
 * Manages new connection establishment and ended connection termination.
 */
public class ConnectDisconnectManager {
  private static final int MIN_SIMULTANIOUS_CONNECT_ATTEMPTS = 3;  //NOTE: WinXP SP2 limits to 10 max at any given time
  private static final int MAX_SIMULTANIOUS_CONNECT_ATTEMPTS = 5;
  private static final int CONNECT_ATTEMPT_TIMEOUT = 30*1000;  //30sec
  private static final int CONNECT_ATTEMPT_STALL_TIME = 3*1000;  //3sec
  
  private final VirtualChannelSelector connect_selector = new VirtualChannelSelector( VirtualChannelSelector.OP_CONNECT );
  
  private final LinkedList 	new_requests 			= new LinkedList();
  private final AEMonitor	new_requests_mon		= new AEMonitor( "ConnectDisconnectManager:NR");
  private final HashMap 	pending_attempts 		= new HashMap();
  private final HashMap 	canceled_requests 		= new HashMap();
  private final AEMonitor	canceled_requests_mon	= new AEMonitor( "ConnectDisconnectManager:CR");
  private final LinkedList 	pending_closes 			= new LinkedList();
  private final AEMonitor	pending_closes_mon		= new AEMonitor( "ConnectDisconnectManager:PC");
     
  private final Random random = new Random();
  
  private static final boolean SHOW_CONNECT_STATS = false;
  
  
  
  protected ConnectDisconnectManager() {
    Thread loop = new AEThread( "ConnectDisconnectManager" ) {
      public void run() {
        mainLoop();
      }
    };
    loop.setDaemon( true );
    loop.start();    
  }
  

  private void mainLoop() {      
    while( true ) {
      addNewOutboundRequests();
      runSelect();
      doClosings();
    }
  }
  
  
  private void addNewOutboundRequests() {    
    while( pending_attempts.size() < MIN_SIMULTANIOUS_CONNECT_ATTEMPTS ) {
      ConnectionRequest cr;
      try{
      	new_requests_mon.enter();
      
        if( new_requests.isEmpty() )  break;
        
        cr = (ConnectionRequest)new_requests.removeFirst();
      }finally{
      	
      	new_requests_mon.exit();
      }
      addNewRequest( cr );
    }
  }
  
  

  private void addNewRequest( final ConnectionRequest request ) {
    try{
    	canceled_requests_mon.enter();
    
      if( canceled_requests.containsKey( request.listener ) ) {  //it's been canceled
        canceled_requests.remove( request.listener );
        return;
      }
    }finally{
    	
    	canceled_requests_mon.exit();
    }
    
    try {
      request.channel = SocketChannel.open();
      
      String rcv_size = System.getProperty("socket.SO_RCVBUF");
      if ( rcv_size != null ) request.channel.socket().setReceiveBufferSize( Integer.parseInt( rcv_size ) );
      
      String snd_size = System.getProperty("socket.SO_SNDBUF");
      if ( snd_size != null ) request.channel.socket().setSendBufferSize( Integer.parseInt( snd_size ) );

      String ip_tos = System.getProperty("socket.IPTOS");
      if ( ip_tos != null ) request.channel.socket().setTrafficClass( Integer.decode( ip_tos ).intValue() );

      String bindIP = COConfigurationManager.getStringParameter("Bind IP", "");
      if ( bindIP.length() > 6 ) {
        request.channel.socket().bind( new InetSocketAddress( InetAddress.getByName( bindIP ), 0 ) );
      }
      
      request.channel.configureBlocking( false );
      request.channel.connect( request.address );
      
      connect_selector.register( request.channel, new VirtualChannelSelector.VirtualSelectorListener() {
        public void selectSuccess( Object attachment ) {
          try{
          	canceled_requests_mon.enter();
          
            if( !canceled_requests.containsKey( request.listener ) ) { //check if canceled
              try {
                if( request.channel.finishConnect() ) {
                  
                  if( SHOW_CONNECT_STATS ) {
                    long queue_wait_time = request.connect_start_time - request.request_start_time;
                    long connect_time = SystemTime.getCurrentTime() - request.connect_start_time;
                    int num_queued = new_requests.size();
                    int num_connecting = pending_attempts.size();
                    System.out.println("S: queue_wait_time="+queue_wait_time+
                                       ", connect_time="+connect_time+
                                       ", num_queued="+num_queued+
                                       ", num_canceled="+canceled_requests.size()+
                                       ", num_connecting="+num_connecting);
                  }
                  
                  request.listener.connectSuccess( request.channel );
                }
                else { //should never happen
                  System.out.println( "finishConnect() failed" );
                  request.listener.connectFailure( new Throwable( "finishConnect() failed" ) );
                  try{
                  	pending_closes_mon.enter();
                  
                    pending_closes.addLast( request.channel );
                  }finally{
                  	pending_closes_mon.exit();
                  }
                }
              }
              catch( Throwable t ) {
                
                if( SHOW_CONNECT_STATS ) {
                  long queue_wait_time = request.connect_start_time - request.request_start_time;
                  long connect_time = SystemTime.getCurrentTime() - request.connect_start_time;
                  int num_queued = new_requests.size();
                  int num_connecting = pending_attempts.size();
                  System.out.println("F: queue_wait_time="+queue_wait_time+
                                     ", connect_time="+connect_time+
                                     ", num_queued="+num_queued+
                                     ", num_canceled="+canceled_requests.size()+
                                     ", num_connecting="+num_connecting);
                }
                
                request.listener.connectFailure( t );
                try{
                	pending_closes_mon.enter();
                
                	pending_closes.addLast( request.channel );
                }finally{
                	
                	pending_closes_mon.exit();
                }
              }
            }
            else {  //if already canceled, no need to invoke listener
              canceled_requests.remove( request.listener );
              try{
              	pending_closes_mon.enter();
              
                pending_closes.addLast( request.channel );
              }finally{
              	
              	pending_closes_mon.exit();
              }
            }
          }finally{
          	
          	canceled_requests_mon.exit();
          }
          pending_attempts.remove( request );
        }
        
        public void selectFailure( Throwable msg ) {
          System.out.println( "selectFailure" );
          
          try{
          	pending_closes_mon.enter();
          
            pending_closes.addLast( request.channel );
          }finally{
          	
          	pending_closes_mon.exit();
          }
          
          try{
          	canceled_requests_mon.enter();
          
            if( !canceled_requests.containsKey( request.listener ) ) { //check if canceled
              request.listener.connectFailure( msg );
            }
            else {  //if already canceled, no need to invoke listener
              canceled_requests.remove( request.listener );
            }
          }finally{
          	
          	canceled_requests_mon.exit();
          }
          
          pending_attempts.remove( request );
        }
      }, null );

      request.connect_start_time = SystemTime.getCurrentTime();
      pending_attempts.put( request, null );
    }
    catch( Throwable t ) {
      t.printStackTrace();
      if( request.channel != null ) {
        try{
        	pending_closes_mon.enter();
        
        	pending_closes.addLast( request.channel );
        }finally{
        	
        	pending_closes_mon.exit();
        }
      }
      request.listener.connectFailure( t );
    }
  }
  
  
  
  private void runSelect() {
    connect_selector.select( 100 );
    int num_stalled_requests = 0;
    
    //do connect attempt timeout checks
    for( Iterator i = pending_attempts.keySet().iterator(); i.hasNext(); ) {
      ConnectionRequest request = (ConnectionRequest)i.next();
      long waiting_time = SystemTime.getCurrentTime() - request.connect_start_time;
      if( waiting_time > CONNECT_ATTEMPT_TIMEOUT ) {
        i.remove();
        try{
        	canceled_requests_mon.enter();  //cancel it for when it finally gets selected
        
        	canceled_requests.put( request.listener, null );
        }finally{
        	canceled_requests_mon.exit();
        }
        
        try{
        	pending_closes_mon.enter();
        
        	pending_closes.addLast( request.channel );
        }finally{
        	
        	pending_closes_mon.exit();
        }
        
        request.listener.connectFailure( new Throwable( "Connection attempt aborted: timed out after " +CONNECT_ATTEMPT_TIMEOUT/1000+ "sec" ) );
      }
      else if( waiting_time >= CONNECT_ATTEMPT_STALL_TIME ) {
        num_stalled_requests++;
      }
    }
    
    //check if our connect queue is stalled, and expand if so
    if( num_stalled_requests == pending_attempts.size() && pending_attempts.size() < MAX_SIMULTANIOUS_CONNECT_ATTEMPTS ) {
      ConnectionRequest cr = null;
      try{
      	new_requests_mon.enter();
      
        if( !new_requests.isEmpty() ) {
          cr = (ConnectionRequest)new_requests.removeFirst();
        }
      }finally{
      	new_requests_mon.exit();
      }
      if( cr != null )  addNewRequest( cr );
    }
  }
  
  
  private void doClosings() {
    try{
    	pending_closes_mon.enter();
    
      while( !pending_closes.isEmpty() ) {
        SocketChannel channel = (SocketChannel)pending_closes.removeFirst();
        if( channel != null ) {
          try{ 
            channel.close();
          }
          catch( Throwable t ) {  t.printStackTrace();  }
        }
      }
    }finally{
    	
    	pending_closes_mon.exit();
    }
  }
  
  
  /**
   * Request that a new connection be made out to the given address.
   * @param address remote ip+port to connect to
   * @param listener to receive notification of connect attempt success/failure
   */
  protected void requestNewConnection( InetSocketAddress address, ConnectListener listener ) {
    ConnectionRequest cr = new ConnectionRequest( address, listener );
    try{
    	new_requests_mon.enter();
    
      //insert at a random position because new connections are usually added in 50-peer
      //chunks, i.e. from a tracker announce reply, and we want to evenly distribute the
      //connect attempts if there are multiple torrents running
      int insert_pos = 0;
      if( new_requests.size() > 0 ) {
        insert_pos = random.nextInt( new_requests.size() );
      }
      new_requests.add( insert_pos, cr );
    }finally{
    	
    	new_requests_mon.exit();
    }
  }
  
  
  /**
   * Close the given connection.
   * @param channel to close
   */
  protected void closeConnection( SocketChannel channel ) {
    try{
    	pending_closes_mon.enter();
    
    	pending_closes.addLast( channel );
    }finally{
    	
    	pending_closes_mon.exit();
    }
  }
  
  
  /**
   * Cancel a pending new connection request.
   * @param listener_key used in the initial connect request
   */
  protected void cancelRequest( ConnectListener listener_key ) {
    try{
    	new_requests_mon.enter();
    
      for( Iterator i = new_requests.iterator(); i.hasNext(); ) {
        ConnectionRequest request = (ConnectionRequest)i.next();
        if( request.listener == listener_key ) {
          i.remove();
          return;
        }
      }
    }finally{
    	new_requests_mon.exit();
    }
    
    try{
    	canceled_requests_mon.enter();
    
    	canceled_requests.put( listener_key, null );
    }finally{
    	
    	canceled_requests_mon.exit();
    }
  }
  
  
  
  
  
  private static class ConnectionRequest {
    private final InetSocketAddress address;
    private final ConnectListener listener;
    private final long request_start_time;
    private long connect_start_time;
    private SocketChannel channel;
    
    private ConnectionRequest( InetSocketAddress address, ConnectListener listener ) {
      this.address = address;
      this.listener = listener;
      request_start_time = SystemTime.getCurrentTime();
    }
  }
  
  
  
///////////////////////////////////////////////////////////  
  
  /**
   * Listener for notification of connection establishment.
   */
   protected interface ConnectListener {
     /**
      * The connection attempt succeeded.
      * @param channel connected socket channel
      */
     public void connectSuccess( SocketChannel channel ) ;
     
    
    /**
     * The connection attempt failed.
     * @param failure_msg failure reason
     */
    public void connectFailure( Throwable failure_msg );
  }
   
/////////////////////////////////////////////////////////////
   
}
