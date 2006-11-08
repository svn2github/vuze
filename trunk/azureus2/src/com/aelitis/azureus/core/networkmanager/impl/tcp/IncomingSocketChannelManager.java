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

package com.aelitis.azureus.core.networkmanager.impl.tcp;

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.networkmanager.*;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdmin;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminPropertyChangeListener;
import com.aelitis.azureus.core.networkmanager.impl.IncomingConnectionManager;
import com.aelitis.azureus.core.networkmanager.impl.ProtocolDecoder;
import com.aelitis.azureus.core.networkmanager.impl.TransportHelperFilter;
import com.aelitis.azureus.core.networkmanager.impl.TransportCryptoManager;


/**
 * Accepts new incoming socket connections and manages routing of them
 * to registered handlers.
 */
public class IncomingSocketChannelManager
{
  private static final LogIDs LOGID = LogIDs.NWMAN;

  private final String	port_config_key;
  private final String	port_enable_config_key;
  
  private int tcp_listen_port;
  
  private int so_rcvbuf_size = COConfigurationManager.getIntParameter( "network.tcp.socket.SO_RCVBUF" );
  private InetAddress bind_address = NetworkAdmin.getSingleton().getDefaultBindAddress();
    
  private VirtualServerChannelSelector server_selector = null;
  
  private IncomingConnectionManager	incoming_manager = IncomingConnectionManager.getSingleton();
  
  protected AEMonitor	this_mon	= new AEMonitor( "IncomingSocketChannelManager" );

  int listen_fail_count = 0;
  
  
  /**
   * Create manager and begin accepting and routing new connections.
   */
  public IncomingSocketChannelManager( String _port_config_key, String _port_enable_config_key ) {
	  
	port_config_key 		= _port_config_key;
	port_enable_config_key	= _port_enable_config_key;
	
	tcp_listen_port = COConfigurationManager.getIntParameter( port_config_key );

    //allow dynamic port number changes
    COConfigurationManager.addParameterListener( port_config_key, new ParameterListener() {
      public void parameterChanged(String parameterName) {
        int port = COConfigurationManager.getIntParameter( port_config_key );
        if( port != tcp_listen_port ) {
        	tcp_listen_port = port;
          restart();
        }
      }
    });
    
    COConfigurationManager.addParameterListener( port_enable_config_key, new ParameterListener() {
        public void parameterChanged(String parameterName) {
          restart();
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
    
    NetworkAdmin.getSingleton().addPropertyChangeListener(
    	new NetworkAdminPropertyChangeListener()
    	{
    		public void
    		propertyChanged(
    			String		property )
    		{
    			if ( property == NetworkAdmin.PR_DEFAULT_BIND_ADDRESS ){
    			
			        InetAddress address = NetworkAdmin.getSingleton().getDefaultBindAddress();
			        
			        if ( address == null && bind_address == null ){
			        	
			        	return;
			        }
			        
			        if ( address == null || bind_address == null || !address.equals( bind_address )) {
			        	
			          bind_address = address;
			          
			          restart();
			        }
    			}
    		}
      });
 
    
    //start processing
    start();
    
     
    	//run a daemon thread to poll listen port for connectivity
    	//it seems that sometimes under OSX that listen server sockets sometimes stop accepting incoming connections for some unknown reason
    	//this checker tests to make sure the listen socket is still accepting connections, and if not, recreates the socket
    
    SimpleTimer.addPeriodicEvent(
    	"IncomingSocketChannelManager:concheck",
    	60*1000,
        new TimerEventPerformer() {
          public void perform( TimerEvent ev ) {
       
          	if( server_selector != null && server_selector.isRunning() ) { //ensure it's actually running
    					
    					long accept_idle = SystemTime.getCurrentTime() - server_selector.getTimeOfLastAccept();
 
    					if( accept_idle > 10*60*1000 ) {  //the socket server hasn't accepted any new connections in the last 10min
  						
    						//so manually test the listen port for connectivity
    						
    						InetAddress inet_address = server_selector.getBoundToAddress();
        				
      					try{   					
      						if( inet_address == null )  inet_address = InetAddress.getByName( "127.0.0.1" );  //failback
      					
      						Socket sock = new Socket( inet_address, tcp_listen_port, inet_address, 0 );

      						sock.close();
      						listen_fail_count = 0;
      					}
      					catch( Throwable t ) {
      						
      						//ok, let's try again without the explicit local bind
      						try {
      							Socket sock = new Socket( InetAddress.getByName( "127.0.0.1" ), tcp_listen_port );      							
      							sock.close();
      							listen_fail_count = 0;
      						}
      						catch( Throwable x ) {
      							listen_fail_count++;
        						Debug.out( new Date()+ ": listen port on [" +inet_address+ ": " +tcp_listen_port+ "] seems CLOSED [" +listen_fail_count+ "x]" );
        				
        						if( listen_fail_count > 4 ) {
        							String error = t.getMessage() == null ? "<null>" : t.getMessage();
        							String msg = "Listen server socket on [" +inet_address+ ": " +tcp_listen_port+ "] does not appear to be accepting inbound connections.\n[" +error+ "]\nAuto-repairing listen service....\n";
        							Logger.log(new LogAlert(LogAlert.UNREPEATABLE, LogAlert.AT_WARNING, msg));
        							restart();
        							listen_fail_count = 0;
        						}
      						}
      					}
    					}
    					else {  //it's recently accepted an inbound connection
    						listen_fail_count = 0;
    					}
    				}

          }
        }
     );
    
  }
  
  
  
  /**
   * Get port that the TCP server socket is listening for incoming connections on.
   * @return port number
   */
  public int getTCPListeningPortNumber() {  return tcp_listen_port;  }  
  


  
  
  private void start() {
  	try{
  		this_mon.enter();
      
        if( tcp_listen_port < 0 || tcp_listen_port > 65535 || tcp_listen_port == 6880 ) {
          String msg = "Invalid incoming TCP listen port configured, " +tcp_listen_port+ ". Port reset to default. Please check your config!";
          Debug.out( msg );
          Logger.log(new LogAlert(LogAlert.UNREPEATABLE, LogAlert.AT_ERROR, msg));
          tcp_listen_port = RandomUtils.generateRandomNetworkListenPort();
          COConfigurationManager.setParameter( port_config_key, tcp_listen_port );
        }
 
        if ( COConfigurationManager.getBooleanParameter(port_enable_config_key)){
        	
		    if( server_selector == null ) {
		      InetSocketAddress address;

		      if( bind_address != null ) {
		        address = new InetSocketAddress( bind_address, tcp_listen_port );
		      }
		      else {
		        address = new InetSocketAddress( tcp_listen_port );
		      }

		      	      
		      server_selector = VirtualServerChannelSelectorFactory.createTest( address, so_rcvbuf_size, new VirtualBlockingServerChannelSelector.SelectListener() {
		        public void newConnectionAccepted( final ServerSocketChannel server, final SocketChannel channel ) {
		        	
		        	//check for encrypted transport
		  	      	final TCPTransportHelper	helper = new TCPTransportHelper( channel );
	
		        	TransportCryptoManager.getSingleton().manageCrypto( helper, null, true, null, new TransportCryptoManager.HandshakeListener() {
		        		public void handshakeSuccess( ProtocolDecoder decoder, ByteBuffer remaining_initial_data ) {
		        			process( server.socket().getLocalPort(), decoder.getFilter());
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
		            
		            	helper.close( "Handshake failure: " + Debug.getNestedExceptionMessage( failure_msg ));
		            }
		            
		        	public void
		        	gotSecret(
						byte[]				session_secret )
		        	{
		        	}
		        	
		    		public int
		    		getMaximumPlainHeaderLength()
		    		{
		    			return( incoming_manager.getMaxMinMatchBufferSize());
		    		}
		    		
		    		public int
		    		matchPlainHeader(
		    			ByteBuffer			buffer )
		    		{
		    			Object[]	match_data = incoming_manager.checkForMatch( helper, server.socket().getLocalPort(), buffer, true );
		    			
		    			if ( match_data == null ){
		    				
		    				return( TransportCryptoManager.HandshakeListener.MATCH_NONE );
		    				
		    			}else{
		    				
		    				IncomingConnectionManager.MatchListener match = (IncomingConnectionManager.MatchListener)match_data[0];
		    				
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
        }else{
        
        	Logger.log(new LogEvent(LOGID, "Not starting TCP listener on port " + tcp_listen_port + " as protocol disabled" ));
        }
  	}finally{
  		
  		this_mon.exit();
  	}
  }
  
  
  protected void 
  process( 
	int						local_port, 
	TransportHelperFilter 	filter )

  {
 
    SocketChannel	channel = ((TCPTransportHelper)filter.getHelper()).getSocketChannel();
       
    //set advanced socket options
    try {
      int so_sndbuf_size = COConfigurationManager.getIntParameter( "network.tcp.socket.SO_SNDBUF" );
      if( so_sndbuf_size > 0 )  channel.socket().setSendBufferSize( so_sndbuf_size );
      
      String ip_tos = COConfigurationManager.getStringParameter( "network.tcp.socket.IPTOS" );
      if( ip_tos.length() > 0 )  channel.socket().setTrafficClass( Integer.decode( ip_tos ).intValue() );
    }
    catch( Throwable t ) {
      t.printStackTrace();
    }
    
	InetSocketAddress tcp_address = new InetSocketAddress( channel.socket().getInetAddress(), channel.socket().getPort());

	ConnectionEndpoint	co_ep = new ConnectionEndpoint(tcp_address);

	ProtocolEndpointTCP	pe_tcp = new ProtocolEndpointTCP( co_ep, tcp_address );

	Transport transport = new TCPTransportImpl( pe_tcp, filter );
	
    incoming_manager.addConnection( local_port, filter, transport );
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
  
  

}
