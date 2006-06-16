/*
 * Created on Jan 18, 2005
 * Created by Alon Rohter
 * Copyright (C) 2004-2005 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.networkmanager.impl;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.networkmanager.*;


/**
 * Accepts new incoming socket connections and manages routing of them
 * to registered handlers.
 */
public class IncomingSocketChannelManager
{
  private static final LogIDs LOGID = LogIDs.NWMAN;

  public static final int READ_TIMEOUT		= 10*1000;
  public static final int CONNECT_TIMEOUT	= 60*1000;
  
  private final ArrayList connections = new ArrayList();
  private final AEMonitor connections_mon = new AEMonitor( "IncomingConnectionManager:conns" );
  
  private volatile Map match_buffers_cow = new HashMap();	// copy-on-write
  private final AEMonitor match_buffers_mon = new AEMonitor( "IncomingConnectionManager:match" );
  private int max_match_buffer_size = 0;
  private int max_min_match_buffer_size = 0;
   
  private int tcp_listen_port = COConfigurationManager.getIntParameter( "TCP.Listen.Port" );
  private int udp_listen_port = COConfigurationManager.getIntParameter( "UDP.Listen.Port" );
  private int so_rcvbuf_size = COConfigurationManager.getIntParameter( "network.tcp.socket.SO_RCVBUF" );
  private String bind_address = COConfigurationManager.getStringParameter( "Bind IP" );
  
  private long last_timeout_check_time = SystemTime.getCurrentTime();
  
  private VirtualServerChannelSelector server_selector = null;
  
  protected AEMonitor	this_mon	= new AEMonitor( "IncomingSocketChannelManager" );

    
  
  /**
   * Create manager and begin accepting and routing new connections.
   */
  public IncomingSocketChannelManager() {    
    //allow dynamic port number changes
    COConfigurationManager.addParameterListener( "TCP.Listen.Port", new ParameterListener() {
      public void parameterChanged(String parameterName) {
        int port = COConfigurationManager.getIntParameter( "TCP.Listen.Port" );
        if( port != tcp_listen_port ) {
        	tcp_listen_port = port;
          restart();
        }
      }
    });
    
    COConfigurationManager.addParameterListener( "UDP.Listen.Port", new ParameterListener() {
        public void parameterChanged(String parameterName) {
          int port = COConfigurationManager.getIntParameter( "UDP.Listen.Port" );
          if( port != udp_listen_port ) {
        	  udp_listen_port = port;
          }
        }
      });
    
    //allow dynamic receive buffer size changes
    COConfigurationManager.addParameterListener( "network.tcp.socket.SO_RCVBUF", new ParameterListener() {
      public void parameterChanged(String parameterName) {
        int size = COConfigurationManager.getIntParameter( "network.tcp.socket.SO_RCVBUF" );
        if( size != so_rcvbuf_size ) {
          so_rcvbuf_size = size;
          restart();
        }
      }
    });
    
    //allow dynamic bind address changes
    COConfigurationManager.addListener(
    	new COConfigurationListener()
    	{
    		public void configurationSaved() {
	        String address = COConfigurationManager.getStringParameter( "Bind IP" );
	        if( !address.equals( bind_address ) ) {
	          bind_address = address;
	          restart();
	        }
      }
    });
    
    //start processing
    start();
    
     
    	//run a daemon thread to poll listen port for connectivity
    	//it seems that sometimes under OSX that listen server sockets sometimes stop accepting incoming connections for some unknown reason
    	//this checker tests to make sure the listen socket is still accepting connections, and if not, recreates the socket
    	AEThread checker = new AEThread( "ServerSocketChecker" ){
    		public void runSupport() {
    			try{  Thread.sleep( 60*1000 );  }catch( Throwable t){ t.printStackTrace(); }
    		
    			int fail_count = 0;
    		
    			while( true ) {			
    				if( server_selector != null && server_selector.isRunning() ) { //ensure it's actually running
    					
    					long accept_idle = SystemTime.getCurrentTime() - server_selector.getTimeOfLastAccept();
 
    					if( accept_idle > 10*60*1000 ) {  //the socket server hasn't accepted any new connections in the last 10min
  						
    						//so manually test the listen port for connectivity
    						
    						InetAddress inet_address = server_selector.getBoundToAddress();
        				
      					try{   					
      						if( inet_address == null )  inet_address = InetAddress.getByName( "127.0.0.1" );  //failback
      					
      						Socket sock = new Socket( inet_address, tcp_listen_port, inet_address, 0 );

      						sock.close();
      						fail_count = 0;
      					}
      					catch( Throwable t ) {
      						
      						//ok, let's try again without the explicit local bind
      						try {
      							Socket sock = new Socket( InetAddress.getByName( "127.0.0.1" ), tcp_listen_port );      							
      							sock.close();
      							fail_count = 0;
      						}
      						catch( Throwable x ) {
      							fail_count++;
        						Debug.out( new Date()+ ": listen port on [" +inet_address+ ": " +tcp_listen_port+ "] seems CLOSED [" +fail_count+ "x]" );
        				
        						if( fail_count > 4 ) {
        							String error = t.getMessage() == null ? "<null>" : t.getMessage();
        							String msg = "Listen server socket on [" +inet_address+ ": " +tcp_listen_port+ "] does not appear to be accepting inbound connections.\n[" +error+ "]\nAuto-repairing listen service....\n";
        							Logger.log(new LogAlert(LogAlert.UNREPEATABLE, LogAlert.AT_WARNING, msg));
        							restart();
        							fail_count = 0;
        						}
      						}
      					}
    					}
    					else {  //it's recently accepted an inbound connection
    						fail_count = 0;
    					}
    				}
    			
    				try{  Thread.sleep( 60*1000 );  }catch( Throwable t){ t.printStackTrace(); }
    			}
    		}
    	};
    	checker.setDaemon( true );
    	checker.start(); 
    
  }
  
  
  
  /**
   * Get port that the TCP server socket is listening for incoming connections on.
   * @return port number
   */
  public int getTCPListeningPortNumber() {  return tcp_listen_port;  }
  public int getUDPListeningPortNumber() {  return udp_listen_port;  }
  
  

  /**
   * Register the given byte sequence matcher to handle matching against new incoming connection
   * initial data; i.e. the first bytes read from a connection must match in order for the given
   * listener to be invoked.
   * @param matcher byte filter sequence
   * @param listener to call upon match
   */
  public void registerMatchBytes( NetworkManager.ByteMatcher matcher, MatchListener listener ) {
    try {  match_buffers_mon.enter();
    
      if( matcher.size() > max_match_buffer_size ) {
        max_match_buffer_size = matcher.size();
      }

      if ( matcher.minSize() > max_min_match_buffer_size ){
    	  max_min_match_buffer_size = matcher.minSize();
      }
      
      Map	new_match_buffers = new HashMap( match_buffers_cow );
      
      new_match_buffers.put( matcher, listener );
      
      match_buffers_cow = new_match_buffers;
    
      byte[]	secret = matcher.getSharedSecret();
      
      if ( secret != null ){
    	  
	     TCPProtocolDecoder.addSecret( secret );
      }
    } finally {  match_buffers_mon.exit();  }
    
  }
  
  
  /**
   * Remove the given byte sequence match from the registration list.
   * @param to_remove byte sequence originally used to register
   */
  public void deregisterMatchBytes( NetworkManager.ByteMatcher to_remove ) {
    try {  match_buffers_mon.enter();
      Map	new_match_buffers = new HashMap( match_buffers_cow );
    
      new_match_buffers.remove( to_remove );
    
      if( to_remove.size() == max_match_buffer_size ) { //recalc longest buffer if necessary
        max_match_buffer_size = 0;
        for( Iterator i = new_match_buffers.keySet().iterator(); i.hasNext(); ) {
          NetworkManager.ByteMatcher bm = (NetworkManager.ByteMatcher)i.next();
          if( bm.size() > max_match_buffer_size ) {
            max_match_buffer_size = bm.size();
          }
        }
      }
    
      match_buffers_cow = new_match_buffers;
      
      byte[]	secret = to_remove.getSharedSecret();
      
      if ( secret != null ){
    	  
	      TCPProtocolDecoder.removeSecret( secret );
      }
    } finally {  match_buffers_mon.exit();  }  
  } 
  
    
  
  
  private void start() {
  	try{
  		this_mon.enter();
      
        if( tcp_listen_port < 0 || tcp_listen_port > 65535 || tcp_listen_port == 6880 ) {
          String msg = "Invalid incoming TCP listen port configured, " +tcp_listen_port+ ". Port reset to default. Please check your config!";
          Debug.out( msg );
          Logger.log(new LogAlert(LogAlert.UNREPEATABLE, LogAlert.AT_ERROR, msg));
          tcp_listen_port = RandomUtils.generateRandomNetworkListenPort();
          COConfigurationManager.setParameter( "TCP.Listen.Port", tcp_listen_port );
        }
        if( udp_listen_port < 0 || udp_listen_port > 65535 || udp_listen_port == 6880 ) {
            String msg = "Invalid incoming UDP listen port configured, " +udp_listen_port+ ". Port reset to default. Please check your config!";
            Debug.out( msg );
            Logger.log(new LogAlert(LogAlert.UNREPEATABLE, LogAlert.AT_ERROR, msg));
            udp_listen_port = tcp_listen_port;
            COConfigurationManager.setParameter( "UDP.Listen.Port", udp_listen_port );
        }
	    if( server_selector == null ) {
	      InetSocketAddress address;
	      try{
	        if( bind_address.length() > 6 ) {
	          address = new InetSocketAddress( InetAddress.getByName( bind_address ), tcp_listen_port );
	        }
	        else {
	          address = new InetSocketAddress( tcp_listen_port );
	        }
	      }
	      catch( UnknownHostException e ) {
	        Debug.out( e );
	        address = new InetSocketAddress( tcp_listen_port );
	      }
	      
	      server_selector = VirtualServerChannelSelectorFactory.createTest( address, so_rcvbuf_size, new VirtualBlockingServerChannelSelector.SelectListener() {
	        public void newConnectionAccepted( final ServerSocketChannel server, final SocketChannel channel ) {
	        	
	        	//check for encrypted transport
	        	TransportCryptoManager.getSingleton().manageCrypto( channel, null, true, new TransportCryptoManager.HandshakeListener() {
	        		public void handshakeSuccess( TCPTransportHelperFilter filter ) {
	        			process( server, filter );
	        		}

	            public void 
	            handshakeFailure( 
	            	Throwable failure_msg ) 
	            {
	            	
	            	if (Logger.isEnabled()) 	Logger.log(new LogEvent(LOGID, "incoming crypto handshake failure: " + Debug.getNestedExceptionMessage( failure_msg )));
	            	
	            	/*
	            		// we can have problems with sockets stuck in a TIME_WAIT state if we just
	            		// close an incoming channel - to clear things down properly the client needs
	            		// to initiate the close. So what we do is send some random bytes to the client
	            		// under the assumption this will cause them to close, and we delay our socket close
	            		// for 10 seconds to give them a chance to do so.	            	
	            		try{
	            			Random	random = new Random();
	            		
	            			byte[]	random_bytes = new byte[68+random.nextInt(128-68)];
	            		
	            			random.nextBytes( random_bytes );
	            		
	            			channel.write( ByteBuffer.wrap( random_bytes ));
	            		
	            		}catch( Throwable e ){
	            			// ignore anything here
	            		}
	            		NetworkManager.getSingleton().closeSocketChannel( channel, 10*1000 );
	            	*/
	            
	            	NetworkManager.getSingleton().closeSocketChannel( channel );
	            }
	            
	    		public int
	    		getMaximumPlainHeaderLength()
	    		{
	    			return( max_min_match_buffer_size );
	    		}
	    		
	    		public int
	    		matchPlainHeader(
	    			ByteBuffer			buffer )
	    		{
	    			MatchListener	match = checkForMatch( server.socket().getLocalPort(), buffer, true );
	    			
	    			if ( match == null ){
	    				
	    				return( TransportCryptoManager.HandshakeListener.MATCH_NONE );
	    				
	    			}else{
	    				
	    				if ( match.autoCryptoFallback()){
	    					
		    				return( TransportCryptoManager.HandshakeListener.MATCH_CRYPTO_AUTO_FALLBACK );
		    					
	    				}else{
	    					
		    				return( TransportCryptoManager.HandshakeListener.MATCH_CRYPTO_NO_AUTO_FALLBACK );
		    				
	    				}
	    			}
	    		}
	        	});
	        	
	        }
	      });
	      
	      server_selector.start();
	    }
  	}finally{
  		
  		this_mon.exit();
  	}
  }
  
  
  protected void process( ServerSocketChannel	server, TCPTransportHelperFilter filter ) {
    //do timeout check if necessary
    long now = SystemTime.getCurrentTime();
    if( now < last_timeout_check_time || now - last_timeout_check_time > 5*1000 ) {
      doTimeoutChecks();
      last_timeout_check_time = now;
    }
    
    if( match_buffers_cow.isEmpty() ) {  //no match registrations, just close
    	if (Logger.isEnabled())
    		Logger.log(new LogEvent(LOGID, "Incoming TCP connection from ["
    				+ filter.getSocketChannel().socket().getInetAddress().getHostAddress() + ":"
    				+ filter.getSocketChannel().socket().getPort()+ "] dropped because zero routing handlers registered"));
    	NetworkManager.getSingleton().closeSocketChannel( filter.getSocketChannel() );
      return;
    }
    
    //set advanced socket options
    try {
      int so_sndbuf_size = COConfigurationManager.getIntParameter( "network.tcp.socket.SO_SNDBUF" );
      if( so_sndbuf_size > 0 )  filter.getSocketChannel().socket().setSendBufferSize( so_sndbuf_size );
      
      String ip_tos = COConfigurationManager.getStringParameter( "network.tcp.socket.IPTOS" );
      if( ip_tos.length() > 0 )  filter.getSocketChannel().socket().setTrafficClass( Integer.decode( ip_tos ).intValue() );
    }
    catch( Throwable t ) {
      t.printStackTrace();
    }
    
    	// note that the filter may have some data internally queued in it after the crypto handshake decode
    	// (in particular the BT header). However, there should be some data right behind it that will trigger
    	// a read-select below, thus giving prompt access to the queued data
    
    final IncomingConnection ic = new IncomingConnection( filter, max_match_buffer_size );
    
    VirtualChannelSelector	selector = NetworkManager.getSingleton().getReadSelector();
    
    SelectorListener	sel_listener = new SelectorListener( server );
    
    try{  connections_mon.enter();

      connections.add( ic );
      
      selector.register(  
    		 ic.filter.getSocketChannel(),
    		 sel_listener,
    		 ic );
      
    } finally {  connections_mon.exit();  } 

    	// might be stuff queued up in the filter - force one process cycle (NAT check in particular )
    
    sel_listener.selectSuccess( selector, ic.filter.getSocketChannel(), ic );
  }
  
  

  
  private void restart() {
  	try{
  		this_mon.enter();
      	
  		if( server_selector != null ) {	  			  			
  			server_selector.stop();
  			server_selector = null;
  		}
  	}finally{
      		
  		this_mon.exit();
  	}
      	
  	try{ Thread.sleep( 1000 );  }catch( Throwable t ) { t.printStackTrace();  }
      	
  	start();
  }
  
  
  protected void removeConnection( IncomingConnection connection, boolean close_as_well ) {
    try{  connections_mon.enter();
    
      NetworkManager.getSingleton().getReadSelector().cancel( connection.filter.getSocketChannel() );  //cancel read op
      connections.remove( connection );   //remove from connection list
      
    } finally {  connections_mon.exit();  }
    
    if( close_as_well ) {
      NetworkManager.getSingleton().closeSocketChannel( connection.filter.getSocketChannel() );  //async close it
    }
  }
  

  protected MatchListener 
  checkForMatch( 
	 int	incoming_port, ByteBuffer to_check, boolean min_match ) 
  { 
       //remember original values for later restore
      int orig_position = to_check.position();
      int orig_limit = to_check.limit();
      
      //rewind
      to_check.position( 0 );

      MatchListener listener = null;
           
      for( Iterator i = match_buffers_cow.entrySet().iterator(); i.hasNext(); ) {
        Map.Entry entry = (Map.Entry)i.next();
        NetworkManager.ByteMatcher bm = (NetworkManager.ByteMatcher)entry.getKey();
        
        if ( min_match ){
            if( orig_position < bm.minSize() ) {  //not enough bytes yet to compare
  	          continue;
  	        }
  	                
  	        if( bm.minMatches( to_check, incoming_port ) ) {  //match found!
  	          listener = (MatchListener)entry.getValue();
  	          break;
  	        }      	
        }else{
	        if( orig_position < bm.size() ) {  //not enough bytes yet to compare
	          continue;
	        }
	                
	        if( bm.matches( to_check, incoming_port ) ) {  //match found!
	          listener = (MatchListener)entry.getValue();
	          break;
	        }
        }
      }

      //restore original values in case the checks changed them
      to_check.position( orig_position );
      to_check.limit( orig_limit );
      
      return listener;
  }
  
  protected void doTimeoutChecks() {
    try{  connections_mon.enter();

      ArrayList to_close = null;
      long now = SystemTime.getCurrentTime();
      
      for( int i=0; i < connections.size(); i++ ) {
        IncomingConnection ic = (IncomingConnection)connections.get( i );
        
        if( ic.last_read_time > 0 ) {  //at least one read op has occured
          if( now < ic.last_read_time ) {  //time went backwards!
            ic.last_read_time = now;
          }
          else if( now - ic.last_read_time > READ_TIMEOUT ) {  //10s read timeout
          	if (Logger.isEnabled())
							Logger.log(new LogEvent(LOGID, "Incoming TCP connection ["
									+ ic.filter.getSocketChannel().socket().getInetAddress().getHostAddress() + ":"
									+ ic.filter.getSocketChannel().socket().getPort()
									+ "] forcibly timed out due to socket read inactivity ["
									+ ic.buffer.position() + " bytes read: "
									+ new String(ic.buffer.array()) + "]"));
            if( to_close == null )  to_close = new ArrayList();
            to_close.add( ic );
          }
        }
        else { //no bytes have been read yet
          if( now < ic.initial_connect_time ) {  //time went backwards!
            ic.initial_connect_time = now;
          }
          else if( now - ic.initial_connect_time > CONNECT_TIMEOUT ) {  //60s connect timeout
          	if (Logger.isEnabled())
							Logger.log(new LogEvent(LOGID, "Incoming TCP connection ["
									+ ic.filter.getSocketChannel() + "] forcibly timed out after "
									+ "60sec due to socket inactivity"));
            if( to_close == null )  to_close = new ArrayList();
            to_close.add( ic );
          }
        }
      }
      
      if( to_close != null ) {
        for( int i=0; i < to_close.size(); i++ ) {
          IncomingConnection ic = (IncomingConnection)to_close.get( i );
          removeConnection( ic, true );
        }
      }
      
    } finally {  connections_mon.exit();  }
  }
  
  
  
    
 
  
  protected static class IncomingConnection {
  	protected final TCPTransportHelperFilter filter;
  	protected final ByteBuffer buffer;
  	protected long initial_connect_time;
  	protected long last_read_time = -1;
    
  	protected IncomingConnection( TCPTransportHelperFilter filter, int buff_size ) {
      this.filter = filter;
      this.buffer = ByteBuffer.allocate( buff_size );
      this.initial_connect_time = SystemTime.getCurrentTime();
    }
  }
  
  
  protected class
  SelectorListener
	 implements VirtualChannelSelector.VirtualSelectorListener
 {
	  private ServerSocketChannel	server;
	  protected
	  SelectorListener(
		  ServerSocketChannel	_server )
	  {
		  server = _server;
	  }
	  
	  public boolean selectSuccess( VirtualChannelSelector selector, SocketChannel sc, Object attachment ) {
		  IncomingConnection	ic = (IncomingConnection)attachment;
		  
		  try {                 
			  long bytes_read = ic.filter.read( new ByteBuffer[]{ ic.buffer }, 0, 1 );
			  
			  if( bytes_read < 0 ) {
				  throw new IOException( "end of stream on socket read" );
			  }
			  
			  if( bytes_read == 0 ) {
				  return false;
			  }
			  
			  ic.last_read_time = SystemTime.getCurrentTime();
			  
			  MatchListener listener = checkForMatch( server.socket().getLocalPort(), ic.buffer, false );
			  
			  if( listener == null ) {  //no match found
				  if( ic.buffer.position() >= max_match_buffer_size ) { //we've already read in enough bytes to have compared against all potential match buffers
					  ic.buffer.flip();
					  if (Logger.isEnabled())
						  Logger.log(new LogEvent(LOGID,
								  LogEvent.LT_WARNING,
								  "Incoming TCP stream from ["
								  + sc.socket().getInetAddress()
								  .getHostAddress()
								  + ":"
								  + sc.socket().getPort()
								  + "] does not match "
								  + "any known byte pattern: "
								  + ByteFormatter.nicePrint(ic.buffer.array())));
					  removeConnection( ic, true );
				  }
			  }
			  else {  //match found!
				  ic.buffer.flip();
				  if (Logger.isEnabled())
					  Logger.log(new LogEvent(LOGID,
							  "Incoming TCP stream from ["
							  + sc.socket().getInetAddress()
							  .getHostAddress()
							  + ":"
							  + sc.socket().getPort()
							  + "] recognized as "
							  + "known byte pattern: "
							  + ByteFormatter.nicePrint(ic.buffer.array())));
				  removeConnection( ic, false );
				  
				  ConnectionEndpoint	co_ep = new ConnectionEndpoint();
				  
				  ProtocolEndpointTCP	pe_tcp = new ProtocolEndpointTCP( co_ep, new InetSocketAddress( sc.socket().getInetAddress(), sc.socket().getPort()));
				  				  
				  Transport transport = new TCPTransportImpl( pe_tcp, ic.filter, ic.buffer );
				  
				  listener.connectionMatched( transport );
			  }
			  return( true );
		  }
		  catch( Throwable t ) {
			  try {
				  if (Logger.isEnabled())
					  Logger.log(new LogEvent(LOGID,
							  LogEvent.LT_ERROR,
							  "Incoming TCP connection ["
							  + sc.socket().getInetAddress()
							  .getHostAddress() + ":"
							  + sc.socket().getPort()
							  + "] socket read exception: "
							  + t.getMessage()));
			  }
			  catch( Throwable x ) {
				  Debug.out( "Caught exception on incoming exception log:" );
				  x.printStackTrace();
				  System.out.println( "CAUSED BY:" );
				  t.printStackTrace();
			  }
			  
			  removeConnection( ic, true );
			  
			  return( false );
		  }
	  }
	  
	  //FAILURE
	  public void selectFailure( VirtualChannelSelector selector, SocketChannel sc, Object attachment, Throwable msg ) {
		  IncomingConnection	ic = (IncomingConnection)attachment;
		  if (Logger.isEnabled())
			  Logger.log(new LogEvent(LOGID, LogEvent.LT_ERROR,
					  "Incoming TCP connection [" + sc
					  + "] socket select op failure: "
					  + msg.getMessage()));
		  removeConnection( ic, true );
	  }
 }
  
  
  /**
   * Listener for byte matches.
   */
  public interface MatchListener {
    
	  /**
	   * Currently if message crypto is on and default fallback for incoming not
	   * enabled then we would bounce incoming messages from non-crypto transports
	   * For example, NAT check
	   * This method allows auto-fallback for such transports
	   * @return
	   */
	public boolean
	autoCryptoFallback();
	
    /**
     * The given socket has been accepted as matching the byte filter.
     * @param channel matching accepted connection
     * @param read_so_far bytes already read
     */
    public void connectionMatched( Transport	transport );
  }
  
  
}
