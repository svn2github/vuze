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
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.util.*;



/**
 * Manages new connection establishment and ended connection termination.
 */
public class ConnectDisconnectManager {
  private static int MIN_SIMULTANIOUS_CONNECT_ATTEMPTS = 3;  
  private static int MAX_SIMULTANIOUS_CONNECT_ATTEMPTS = 5;  //NOTE: WinXP SP2 limits to 10 max at any given time
  static {
    MAX_SIMULTANIOUS_CONNECT_ATTEMPTS = COConfigurationManager.getIntParameter( "network.max.simultaneous.connect.attempts" );
    MIN_SIMULTANIOUS_CONNECT_ATTEMPTS = MAX_SIMULTANIOUS_CONNECT_ATTEMPTS - 2;
    if( MIN_SIMULTANIOUS_CONNECT_ATTEMPTS < 1 ) {
      MIN_SIMULTANIOUS_CONNECT_ATTEMPTS = MAX_SIMULTANIOUS_CONNECT_ATTEMPTS == 0 ? 0 : 1;  //max 0 = outbound disabled
    }
    COConfigurationManager.addParameterListener( "network.max.simultaneous.connect.attempts", new ParameterListener() {
      public void parameterChanged( String parameterName ) {
        MAX_SIMULTANIOUS_CONNECT_ATTEMPTS = COConfigurationManager.getIntParameter( "network.max.simultaneous.connect.attempts" );
        MIN_SIMULTANIOUS_CONNECT_ATTEMPTS = MAX_SIMULTANIOUS_CONNECT_ATTEMPTS - 2;
        if( MIN_SIMULTANIOUS_CONNECT_ATTEMPTS < 1 ) {
          MIN_SIMULTANIOUS_CONNECT_ATTEMPTS = MAX_SIMULTANIOUS_CONNECT_ATTEMPTS == 0 ? 0 : 1;  //max 0 = outbound disabled
        }
      }
    });
  }
  
  private static final int CONNECT_ATTEMPT_TIMEOUT = 30*1000;  //30sec
  private static final int CONNECT_ATTEMPT_STALL_TIME = 3*1000;  //3sec
  private static final boolean SHOW_CONNECT_STATS = false;
  
  private final VirtualChannelSelector connect_selector = new VirtualChannelSelector( VirtualChannelSelector.OP_CONNECT );
  
  private final LinkedList new_requests = new LinkedList();
  private final ArrayList canceled_requests = new ArrayList();
  private final AEMonitor	new_canceled_mon= new AEMonitor( "ConnectDisconnectManager:NCM");
  
  private final HashMap pending_attempts = new HashMap();
  
  private final LinkedList pending_closes = new LinkedList();
  private final AEMonitor	pending_closes_mon = new AEMonitor( "ConnectDisconnectManager:PC");
     
  private final Random random = new Random();
  
  
  
  protected ConnectDisconnectManager() {
    Thread loop = new AEThread( "ConnectDisconnectManager" ) {
      public void runSupport() {
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
      try{
        new_canceled_mon.enter();
      
        if( new_requests.isEmpty() )  break;
        ConnectionRequest cr = (ConnectionRequest)new_requests.removeFirst();
        addNewRequest( cr ); 
      }
      finally{
        new_canceled_mon.exit();
      }
    }
  }
  
  

  private void addNewRequest( final ConnectionRequest request ) {  
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
        LGLogger.log( "Binding outgoing connection [" +request.address+ "] to local IP address: " + bindIP );
        request.channel.socket().bind( new InetSocketAddress( InetAddress.getByName( bindIP ), 0 ) );
      }
      
      request.channel.configureBlocking( false );
      request.channel.connect( request.address );
      
      connect_selector.register( request.channel, new VirtualChannelSelector.VirtualSelectorListener() {
        public void selectSuccess( Object attachment ) {         
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
                                    ", num_connecting="+num_connecting);
              }
                  
              request.listener.connectSuccess( request.channel );
            }
            else { //should never happen
              Debug.out( "finishConnect() failed" );
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
            
          pending_attempts.remove( request );
        }
        
        public void selectFailure( Throwable msg ) {
          Debug.out( "selectFailure" );
          
          try{
          	pending_closes_mon.enter();
          
            pending_closes.addLast( request.channel );
          }finally{
          	
          	pending_closes_mon.exit();
          }

          request.listener.connectFailure( msg );

          pending_attempts.remove( request );
          
        }
      }, null );

      request.connect_start_time = SystemTime.getCurrentTime();
      pending_attempts.put( request, null );
    }
    catch( Throwable t ) {
      
      String full = request.address.toString();
      String hostname = request.address.getHostName();
      int port = request.address.getPort();
      boolean unresolved = request.address.isUnresolved();
      InetAddress	inet_address = request.address.getAddress();
      String full_sub = inet_address==null?request.address.toString():inet_address.toString();
      String host_address = inet_address==null?request.address.toString():inet_address.getHostAddress();
      
      String msg = "ConnectDisconnectManager::address exception: full="+full+ ", hostname="+hostname+ ", port="+port+ ", unresolved="+unresolved+ ", full_sub="+full_sub+ ", host_address="+host_address;
      if( request.channel != null ) {
        String channel = request.channel.toString();
        String socket = request.channel.socket().toString();
        String local_address = request.channel.socket().getLocalAddress().toString();
        int local_port = request.channel.socket().getLocalPort();
           SocketAddress ra = request.channel.socket().getRemoteSocketAddress();
        String remote_address;
           if( ra != null )  remote_address = ra.toString();
           else remote_address = "<null>";
        int remote_port = request.channel.socket().getPort();

        msg += "\n channel="+channel+ ", socket="+socket+ ", local_address="+local_address+ ", local_port="+local_port+ ", remote_address="+remote_address+ ", remote_port="+remote_port;
      }
      
      LGLogger.log( msg, t );
      
      
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
    //do cancellations
    try{
      new_canceled_mon.enter();
      
      for( Iterator can_it = canceled_requests.iterator(); can_it.hasNext(); ) {
        ConnectListener key = (ConnectListener)can_it.next();
        
        //boolean found = false;      
        for( Iterator pen_it = pending_attempts.keySet().iterator(); pen_it.hasNext(); ) {
          ConnectionRequest request = (ConnectionRequest)pen_it.next();
          if( request.listener == key ) {
            connect_selector.cancel( request.channel );
            
            try{
              pending_closes_mon.enter();
            
              pending_closes.addLast( request.channel );
            }
            finally{
              pending_closes_mon.exit();
            }
            
            //found = true;
            pen_it.remove();
            break;
          }
        }
        
        //if( !found )  Debug.out( "~~~ canceled request not found ~~~" );
      }
      
      canceled_requests.clear();
    }
    finally{
      new_canceled_mon.exit();
    }
    
    
    //run select
    connect_selector.select( 100 );
    

    //do connect attempt timeout checks
    int num_stalled_requests = 0;
    for( Iterator i = pending_attempts.keySet().iterator(); i.hasNext(); ) {
      ConnectionRequest request = (ConnectionRequest)i.next();
      long waiting_time = SystemTime.getCurrentTime() - request.connect_start_time;
      if( waiting_time > CONNECT_ATTEMPT_TIMEOUT ) {
        i.remove();

        connect_selector.cancel( request.channel );
        
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
      else if( waiting_time < 0 ) {  //time went backwards
        request.connect_start_time = SystemTime.getCurrentTime();
      }
    }
    
    //check if our connect queue is stalled, and expand if so
    if( num_stalled_requests == pending_attempts.size() && pending_attempts.size() < MAX_SIMULTANIOUS_CONNECT_ATTEMPTS ) {
      try{
        new_canceled_mon.enter();
      
        if( !new_requests.isEmpty() ) {
          ConnectionRequest cr = (ConnectionRequest)new_requests.removeFirst();
          addNewRequest( cr );
        }
      }
      finally{
        new_canceled_mon.exit();
      }
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
          catch( Throwable t ) {  Debug.printStackTrace(t);  }
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
    if( MAX_SIMULTANIOUS_CONNECT_ATTEMPTS == 0 ) { //outbound connects are disabled, so fail immediately
      listener.connectFailure( new Throwable( "Outbound connects disabled in config: MAX_SIMULTANIOUS_CONNECT_ATTEMPTS == 0" ) );
      return;
    }
    
    ConnectionRequest cr = new ConnectionRequest( address, listener );
    try{
      new_canceled_mon.enter();
    
      //insert at a random position because new connections are usually added in 50-peer
      //chunks, i.e. from a tracker announce reply, and we want to evenly distribute the
      //connect attempts if there are multiple torrents running
      int insert_pos = 0;
      if( new_requests.size() > 0 ) {
        insert_pos = random.nextInt( new_requests.size() );
      }
      new_requests.add( insert_pos, cr );
    }finally{
    	
      new_canceled_mon.exit();
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
      new_canceled_mon.enter();
    
      //check if we can cancel it right away
      for( Iterator i = new_requests.iterator(); i.hasNext(); ) {
        ConnectionRequest request = (ConnectionRequest)i.next();
        if( request.listener == listener_key ) {
          i.remove();
          return;
        }
      }
      
      canceled_requests.add( listener_key ); //else add for later removal during select
    }
    finally{
      new_canceled_mon.exit();
    }
  }
  
  

  private static class ConnectionRequest {
    private final InetSocketAddress address;
    private final ConnectListener listener;
    private final long request_start_time;
    private long connect_start_time;
    private SocketChannel channel;
    
    private ConnectionRequest( InetSocketAddress _address, ConnectListener _listener ) {
      address = _address;
      listener = _listener;
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
