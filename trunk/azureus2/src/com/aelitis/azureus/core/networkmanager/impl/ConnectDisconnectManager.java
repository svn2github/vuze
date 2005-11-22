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

package com.aelitis.azureus.core.networkmanager.impl;

import java.net.*;
import java.nio.channels.*;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.networkmanager.VirtualChannelSelector;




/**
 * Manages new connection establishment and ended connection termination.
 */
public class ConnectDisconnectManager {
  private static int MIN_SIMULTANIOUS_CONNECT_ATTEMPTS = 3;  
  public static int MAX_SIMULTANIOUS_CONNECT_ATTEMPTS = 5;  //NOTE: WinXP SP2 limits to 10 max at any given time
  static {
    MAX_SIMULTANIOUS_CONNECT_ATTEMPTS = COConfigurationManager.getIntParameter( "network.max.simultaneous.connect.attempts" );
    
    if( MAX_SIMULTANIOUS_CONNECT_ATTEMPTS < 1 ) { //should never happen, but hey
   	 MAX_SIMULTANIOUS_CONNECT_ATTEMPTS = 1;
   	 COConfigurationManager.setParameter( "network.max.simultaneous.connect.attempts", 1 );
    }
    
    MIN_SIMULTANIOUS_CONNECT_ATTEMPTS = MAX_SIMULTANIOUS_CONNECT_ATTEMPTS - 2;
    
    if( MIN_SIMULTANIOUS_CONNECT_ATTEMPTS < 1 ) {
      MIN_SIMULTANIOUS_CONNECT_ATTEMPTS = 1;
    }

    COConfigurationManager.addParameterListener( "network.max.simultaneous.connect.attempts", new ParameterListener() {
      public void parameterChanged( String parameterName ) {
        MAX_SIMULTANIOUS_CONNECT_ATTEMPTS = COConfigurationManager.getIntParameter( "network.max.simultaneous.connect.attempts" );
        MIN_SIMULTANIOUS_CONNECT_ATTEMPTS = MAX_SIMULTANIOUS_CONNECT_ATTEMPTS - 2;
        if( MIN_SIMULTANIOUS_CONNECT_ATTEMPTS < 1 ) {
          MIN_SIMULTANIOUS_CONNECT_ATTEMPTS = 1;
        }
      }
    });
  }
  
  private static final int CONNECT_ATTEMPT_TIMEOUT = 30*1000;  //30sec
  private static final int CONNECT_ATTEMPT_STALL_TIME = 3*1000;  //3sec
  private static final boolean SHOW_CONNECT_STATS = false;
  
  private final VirtualChannelSelector connect_selector = new VirtualChannelSelector( VirtualChannelSelector.OP_CONNECT, true );
  
  private final LinkedList new_requests = new LinkedList();
  private final ArrayList canceled_requests = new ArrayList();
  private final AEMonitor	new_canceled_mon= new AEMonitor( "ConnectDisconnectManager:NCM");
  
  private final HashMap pending_attempts = new HashMap();
  
  private final LinkedList pending_closes = new LinkedList();
  private final AEMonitor	pending_closes_mon = new AEMonitor( "ConnectDisconnectManager:PC");
     
  private final Random random = new Random();
  
  
  
  public ConnectDisconnectManager() {
    AEThread loop = new AEThread( "ConnectDisconnectManager" ) {
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
      ConnectionRequest cr = null;
      
      try{
        new_canceled_mon.enter();
      
        if( new_requests.isEmpty() )  break;
        cr = (ConnectionRequest)new_requests.removeFirst();
      }
      finally{
        new_canceled_mon.exit();
      }
      
      if( cr != null ) {
        addNewRequest( cr ); 
      }
    }
  }
  
  

  private void addNewRequest( final ConnectionRequest request ) {
    request.listener.connectAttemptStarted();
    
    try {
      request.channel = SocketChannel.open();
      
      try {  //advanced socket options
        int rcv_size = COConfigurationManager.getIntParameter( "network.tcp.socket.SO_RCVBUF" );
        if( rcv_size > 0 ) {
        	if( LGLogger.isEnabled() )  LGLogger.log( "Setting socket receive buffer size for outgoing connection [" +request.address+ "] to: " + rcv_size );
          request.channel.socket().setReceiveBufferSize( rcv_size );
        }
      
        int snd_size = COConfigurationManager.getIntParameter( "network.tcp.socket.SO_SNDBUF" );
        if( snd_size > 0 ) {
        	if( LGLogger.isEnabled() )  LGLogger.log( "Setting socket send buffer size for outgoing connection [" +request.address+ "] to: " + snd_size );
          request.channel.socket().setSendBufferSize( snd_size );
        }

        String ip_tos = COConfigurationManager.getStringParameter( "network.tcp.socket.IPTOS" );
        if( ip_tos.length() > 0 ) {
        	if( LGLogger.isEnabled() )  LGLogger.log( "Setting socket TOS field for outgoing connection [" +request.address+ "] to: " + ip_tos );
          request.channel.socket().setTrafficClass( Integer.decode( ip_tos ).intValue() );
        }

        String bindIP = COConfigurationManager.getStringParameter("Bind IP", "");
        if ( bindIP.length() > 6 ) {
        	if( LGLogger.isEnabled() )  LGLogger.log( "Binding outgoing connection [" +request.address+ "] to local IP address: " + bindIP );
          request.channel.socket().bind( new InetSocketAddress( InetAddress.getByName( bindIP ), 0 ) );
        }
      }
      catch( Throwable t ) {
        String msg = "Error while processing advanced socket options.";
        Debug.out( msg, t );
        LGLogger.logUnrepeatableAlert( msg, t );
        //dont pass the exception outwards, so we will continue processing connection without advanced options set
      }
      
      request.channel.configureBlocking( false );
      request.connect_start_time = SystemTime.getCurrentTime();
      
      if( request.channel.connect( request.address ) ) {  //already connected
        finishConnect( request );
      }
      else {  //not yet connected, so register for connect selection
        pending_attempts.put( request, null );
        
        connect_selector.register( request.channel, new VirtualChannelSelector.VirtualSelectorListener() {
          public boolean selectSuccess( VirtualChannelSelector selector, SocketChannel sc, Object attachment ) {         
            pending_attempts.remove( request );
            finishConnect( request );
            return true;
          }
          
          public void selectFailure( VirtualChannelSelector selector, SocketChannel sc,Object attachment, Throwable msg ) {
            pending_attempts.remove( request );
            try{  pending_closes_mon.enter();
              pending_closes.addLast( request.channel );
            }
            finally{   pending_closes_mon.exit();  }
            request.listener.connectFailure( msg );
          }
        }, null );
      }
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
      else {
        msg += "\n channel=<null>";
      }
      
      Debug.out( msg, t );
      
      
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
  
  
  
  
  private void finishConnect( ConnectionRequest request ) {
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
        
        //ensure the request hasn't been canceled during the select op
        boolean canceled = false;
        try{  new_canceled_mon.enter();
          canceled = canceled_requests.contains( request.listener );
        }
        finally{ new_canceled_mon.exit(); }
        
        if( canceled ) {
          try{  pending_closes_mon.enter();
            pending_closes.addLast( request.channel );  //just close it
          }
          finally{ pending_closes_mon.exit();  }
        }
        else {
          request.listener.connectSuccess( request.channel );
        }
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
  }
  

  
  private void runSelect() {
    //do cancellations
    try{
      new_canceled_mon.enter();
      
      for( Iterator can_it = canceled_requests.iterator(); can_it.hasNext(); ) {
        ConnectListener key = (ConnectListener)can_it.next();
        
        ConnectionRequest to_remove = null;
        
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
            
            to_remove = request;
            break;
          }
        }
        
        if( to_remove != null ) {
          pending_attempts.remove( to_remove );
        }
      }
      
      canceled_requests.clear();
    }
    finally{
      new_canceled_mon.exit();
    }
    
    
    //run select
    try{
      connect_selector.select( 100 );
    }
    catch( Throwable t ) {
      Debug.out( "connnectSelectLoop() EXCEPTION: ", t );
    }

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
      ConnectionRequest cr = null;
      
      try{
        new_canceled_mon.enter();
      
        if( !new_requests.isEmpty() ) {
          cr = (ConnectionRequest)new_requests.removeFirst();
        }
      }
      finally{
        new_canceled_mon.exit();
      }
      
      if( cr != null ) {
        addNewRequest( cr );
      }
    }
  }
  
  
  private void doClosings() {
    try{
    	pending_closes_mon.enter();
    
      while( !pending_closes.isEmpty() ) {
        SocketChannel channel = (SocketChannel)pending_closes.removeFirst();
        if( channel != null ) {
        	
        	connect_selector.cancel( channel );
        	
          try{ 
            channel.close();
          }
          catch( Throwable t ) {
            /*Debug.printStackTrace(t);*/
          }
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
  public void requestNewConnection( InetSocketAddress address, ConnectListener listener ) {    
    ConnectionRequest cr = new ConnectionRequest( address, listener );
    try{
      new_canceled_mon.enter();
    
      //insert at a random position because new connections are usually added in 50-peer
      //chunks, i.e. from a tracker announce reply, and we want to evenly distribute the
      //connect attempts if there are multiple torrents running
      int insert_pos = random.nextInt( new_requests.size() + 1 );
      new_requests.add( insert_pos, cr );
    }finally{
    	
      new_canceled_mon.exit();
    }
  }
  
  
  /**
   * Close the given connection.
   * @param channel to close
   */
  public void closeConnection( SocketChannel channel ) {
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
  public void cancelRequest( ConnectListener listener_key ) {
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
  public interface ConnectListener {
     /**
      * The connection establishment process has started,
      * i.e. the connection is actively being attempted.
      */
     public void connectAttemptStarted();    
     
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
