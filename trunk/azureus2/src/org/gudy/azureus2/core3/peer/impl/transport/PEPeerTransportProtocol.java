/*
 * File    : PEPeerTransportProtocol.java
 * Created : 22-Oct-2003
 * By      : stuff
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.gudy.azureus2.core3.peer.impl.transport;


import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.*;

import org.gudy.azureus2.core3.util.*;

import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.peer.impl.*;
import org.gudy.azureus2.core3.peer.util.*;
import org.gudy.azureus2.core3.config.*;

import com.aelitis.azureus.core.networkmanager.*;
import com.aelitis.azureus.core.peermanager.UploadManager;
import com.aelitis.azureus.core.peermanager.messages.ProtocolMessage;
import com.aelitis.azureus.core.peermanager.messages.bittorrent.*;
import com.aelitis.azureus.core.peermanager.utils.*;



/**
 * @author Olivier
 *
 */
public abstract class 
PEPeerTransportProtocol
	implements PEPeerTransport, ConnectionOwner
{
	//TODO xx
	// these appear in the plugin interface as well so don't renumber without
	// fixing things up
	public final static byte BT_CHOKED 			= 0;
	public final static byte BT_UNCHOKED 		= 1;
	public final static byte BT_INTERESTED 		= 2;
	public final static byte BT_UNINTERESTED 	= 3;
	public final static byte BT_HAVE 			= 4;
	public final static byte BT_BITFIELD 		= 5;
	public final static byte BT_REQUEST 		= 6;
	public final static byte BT_PIECE 			= 7;
	public final static byte BT_CANCEL 			= 8;
	
  
	private PEPeerControl manager;
	private byte[] id;
	private String ip;
	private String ip_resolved;
	private int port;
	
	private PEPeerStatsImpl stats;
  private List 		requested 		= new ArrayList();
  private AEMonitor	requested_mon	= new AEMonitor( "PEPeerTransportProtocol:Req" );

  private HashMap data;
  
  private boolean choked = true;
  private boolean interested_in_other_peer = false;
  private boolean choking = true;
  private boolean other_peer_interested_in_me = false;
  private boolean snubbed = false;
  private boolean[] other_peer_has_pieces;
  private boolean seed = false;

	//The Buffer for reading the length of the messages
	private DirectByteBuffer lengthBuffer;

  
  private boolean connection_registered = false;
  

  private boolean incoming;
  private volatile boolean closing = false;
  private PEPeerTransportProtocolState currentState;
  
  private Connection connection;
  private OutgoingBTPieceMessageHandler outgoing_piece_message_handler;
  private OutgoingBTHaveMessageAggregator outgoing_have_message_aggregator;
  
  private boolean identityAdded = false;  //needed so we don't remove id's in closeAll() on duplicate connection attempts
  
  //The client name identification	
	private String client = "";

	//Reader inner loop counter
	private int processLoop;
  
  
	//Flag to indicate if the connection is in a stable enough state to send a request.
	//Used to reduce discarded pieces due to request / choke / unchoke / re-request , and both in fact taken into account.
	private boolean readyToRequest;
  
	//Number of bad chunks received from this peer
	private int nbBadChunks;
	
	//When superSeeding, number of unique piece announced
	private int uniquePiece;
	
	//Spread time (0 secs , fake default)
	private int spreadTimeHint = 0 * 1000;

  //TODO xx
	public final static int componentID = 1;
	public final static int evtProtocol = 0;
  public final static int evtLifeCycle = 1;
  public final static int evtErrors = 2;
  
  private int readSleepTime;
  private long lastReadTime;

  private List		listeners;

  private long last_message_sent_time = 0;
  
  
  protected AEMonitor	this_mon	= new AEMonitor( "PEPeerTransportProtocol" );

  private final Map recent_outgoing_requests = new LinkedHashMap( 100, .75F, true ) {
    public boolean removeEldestEntry(Map.Entry eldest) {
      return size() > 100;
    }
  };
  
  private AEMonitor	recent_outgoing_requests_mon	= new AEMonitor( "PEPeerTransportProtocol:ROR" );
  
  private static final boolean SHOW_DISCARD_RATE_STATS;
  
  static {
  	String	prop = System.getProperty( "show.discard.rate.stats" );
  	
  	SHOW_DISCARD_RATE_STATS = prop != null && prop.equals( "1" );
  }
  
  private static int requests_discarded = 0;
  private static int requests_recovered = 0;
  private static int requests_completed = 0;
  
  
  
  private static final boolean	socks_peer_proxy_enable;
  private static final String	socks_version;
  private static final String	socks_host;
  private static  int			socks_port;
  private static final String	socks_user;
  private static final String	socks_password;

  static{
  	socks_peer_proxy_enable	= 	
  		COConfigurationManager.getBooleanParameter("Proxy.Data.Enable", false);
  	
	socks_version =	COConfigurationManager.getStringParameter("Proxy.Data.SOCKS.version" );
		  	
    boolean	socks_same = COConfigurationManager.getBooleanParameter("Proxy.Data.Same", true);

  	socks_host 				= COConfigurationManager.getStringParameter(socks_same?"Proxy.Host":"Proxy.Data.Host");
  	
  	String socks_port_str 	= COConfigurationManager.getStringParameter(socks_same?"Proxy.Port":"Proxy.Data.Port");
  	
  	if ( socks_peer_proxy_enable ){
  		
	  	try{
	  		socks_port = Integer.parseInt( socks_port_str );
	  		
	  	}catch( Throwable e ){
	  		
	  		Debug.printStackTrace(e);
	  	}
  	}else{
  		
  		socks_port	= 0;
  	}
  	
  	socks_user 		= COConfigurationManager.getStringParameter(socks_same?"Proxy.Username":"Proxy.Data.Username");
  	socks_password 	= COConfigurationManager.getStringParameter(socks_same?"Proxy.Password":"Proxy.Data.Password");

  }

  

  /*
	 * This object constructors will let the PeerConnection partially created,
	 * but hopefully will let us gain some memory for peers not going to be
	 * accepted.
	 */

  /**
   * The Default Contructor for outgoing connections.
   * @param manager the manager that will handle this PeerConnection
   * @param ip the peer Ip Address
   * @param port the peer port
   */
	public 
	PEPeerTransportProtocol(
  		PEPeerControl 	_manager,
  		String 			_ip, 
  		int 			_port,
  		boolean			_incoming_connection,
      SocketChannel channel,  //hack for incoming connections. null otherwise
  		final byte[]			data_already_read,
  		boolean 		fake ) 
	{		
		manager	= _manager;
		ip 		= _ip;
		port 	= _port;
	 	    
	 	
		if ( fake ){
			return;
		}
	
		uniquePiece = -1;
		
		incoming = _incoming_connection;
    
		
		if( incoming ) {
      connection = NetworkManager.getSingleton().createNewInboundConnection( this, channel );
      
      //"fake" a connect request to register our listener
      connection.connect( new Connection.ConnectionListener() {
        public void connectSuccess() {  //will be called immediately
          LGLogger.log(componentID, evtLifeCycle, LGLogger.RECEIVED, "Established incoming connection from " + PEPeerTransportProtocol.this );
          currentState = new StateHandshaking( false, data_already_read );
        }
        
        public void connectFailure( Throwable failure_msg ) {  //should never happen
          Debug.out( "ERROR: incoming connect failure: ", failure_msg );
          closeAll( "ERROR: incoming connect failure [" + PEPeerTransportProtocol.this + "] : " + failure_msg.getMessage(), true, false );
        }
        
        public void exceptionThrown( Throwable error ) {
          closeAll( "Connection [" + PEPeerTransportProtocol.this + "] exception : " + error.getMessage(), true, true );
        }
      });
		}
    else { //outgoing
			currentState = new StateConnecting();
		}
	}



  /**
   * Private method that will finish fields allocation, once the handshaking is ok.
   * Hopefully, that will save some RAM.
   */
  private void allocateAll() {
    other_peer_has_pieces = new boolean[ manager.getPiecesNumber() ];
  	Arrays.fill( other_peer_has_pieces, false );
  	stats = (PEPeerStatsImpl)manager.createPeerStats();
  	this.lengthBuffer = DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_PT_LENGTH,4 );

    //link in outgoing piece handler
    outgoing_piece_message_handler = new OutgoingBTPieceMessageHandler( manager.getDiskManager(), connection.getOutgoingMessageQueue() );
    
    //link in outgoing have message aggregator
    outgoing_have_message_aggregator = new OutgoingBTHaveMessageAggregator( connection.getOutgoingMessageQueue() );
    
    //register bytes sent listener
    connection.getOutgoingMessageQueue().registerQueueListener( new OutgoingMessageQueue.MessageQueueListener() {
      public void messageAdded( ProtocolMessage message ) { /*ignore*/ }
      public void messageRemoved( ProtocolMessage message ) { /*ignore*/ }
      
      public void messageSent( ProtocolMessage message ) {
        //update keep-alive info
        last_message_sent_time = SystemTime.getCurrentTime();
      }

      public void protocolBytesSent( int byte_count ) {
        //update stats
        stats.protocol_sent( byte_count );
        manager.protocol_sent( byte_count );
      }
      
      public void dataBytesSent( int byte_count ) {
        //update stats
        stats.sent( byte_count );
        manager.sent( byte_count );
      }
    });
    
    //register the new connection with the upload manager so that peer messages get processed
    UploadManager.getSingleton().registerStandardPeerConnection( connection, manager.getUploadLimitedRateGroup() );
    connection_registered = true;
  }

   
  
  public void closeAll(String reason, boolean closedOnError, boolean attemptReconnect) {
  	
	  	if (closing) {
	  		return;
	  	}
	  	closing = true;

	    currentState = new StateClosing();
      
      LGLogger.log( componentID, evtProtocol, closedOnError?LGLogger.ERROR:LGLogger.INFORMATION, reason);
      
	  	//Cancel any pending requests (on the manager side)
	  	cancelRequests();
	  	  	
	  	//Send removed event ...
	    manager.peerRemoved(this);
	    
	    
	    if( outgoing_piece_message_handler != null ) {
	      outgoing_piece_message_handler.removeAllPieceRequests();
	      outgoing_piece_message_handler.destroy();
	      outgoing_piece_message_handler = null;
	    }
	    
	    if( outgoing_have_message_aggregator != null ) {
	      outgoing_have_message_aggregator.destroy();
	      outgoing_have_message_aggregator = null;
	    }
	    
	    closeConnectionX();  //cleanup of download limiter
	    
	    if( connection != null ) {
        if( connection_registered ) {
          UploadManager.getSingleton().cancelStandardPeerConnection( connection );
        }
	      connection.close();
	      connection = null;
	    }
	    
	    
	    try{
	    	recent_outgoing_requests_mon.enter();
	    
	    	recent_outgoing_requests.clear();
	      
	    }finally{
	    	
	    	recent_outgoing_requests_mon.exit();
	    }
	    
	    
	    
	    
	  	//remove identity
	  	if ( this.id != null && identityAdded ) {
	  		PeerIdentityManager.removeIdentity( manager.getHash(), this.id );
	  	}
	
	    if ( lengthBuffer != null ) {
	      lengthBuffer.returnToPool();
	      lengthBuffer = null;
	    }
	  	
	  	//Send a logger event
	  	LGLogger.log(componentID, evtLifeCycle, LGLogger.INFORMATION, "Connection Ended with " + toString());
	  	
	    if( attemptReconnect && !incoming ) {      
	  		LGLogger.log(componentID, evtLifeCycle, LGLogger.INFORMATION, "Attempting to reconnect with " + toString());
	  		manager.peerConnectionClosed( this, true );
	  	}
	  	else {
	      manager.peerConnectionClosed( this, false );
	  	}

  }

	
  private class StateConnecting implements PEPeerTransportProtocolState {
    
    private StateConnecting() 
    {
    	
        Connection.ConnectionListener	cl;
    
    	if ( socks_peer_proxy_enable ){
    		
    		connection = NetworkManager.getSingleton().createNewConnection( PEPeerTransportProtocol.this, socks_host, socks_port );
 		
     	    cl = 
                new Connection.ConnectionListener() {
                   public void connectSuccess() {
                     LGLogger.log(componentID, evtLifeCycle, LGLogger.SENT, "Established outgoing connection with " + PEPeerTransportProtocol.this);
                     setChannel( connection.getSocketChannel() );
                     currentState = new SOCKSStateHandshaking();
                   }
                   
                   public void connectFailure( Throwable failure_msg ) {
                     closeAll( "Failed to establish outgoing connection [" + PEPeerTransportProtocol.this + "] : " + failure_msg.getMessage(), true, false );
                   }
                   
                   public void exceptionThrown( Throwable error ) {
                     closeAll( "Connection [" + PEPeerTransportProtocol.this + "] exception : " + error.getMessage(), true, true );
                   }
                 };
    		
    	}else{
    		
    		connection = NetworkManager.getSingleton().createNewConnection( PEPeerTransportProtocol.this, ip, port );
      
    	    cl = 
                new Connection.ConnectionListener() {
                   public void connectSuccess() {
                     LGLogger.log(componentID, evtLifeCycle, LGLogger.SENT, "Established outgoing connection with " + PEPeerTransportProtocol.this);
                     setChannel( connection.getSocketChannel() );
                     currentState = new StateHandshaking( false, null );
                   }
                   
                   public void connectFailure( Throwable failure_msg ) {
                     closeAll( "Failed to establish outgoing connection [" + PEPeerTransportProtocol.this + "] : " + failure_msg.getMessage(), true, false );
                   }
                   
                   public void exceptionThrown( Throwable error ) {
                     closeAll( "Connection [" + PEPeerTransportProtocol.this + "] exception : " + error.getMessage(), true, true );
                   }
                 };
    	}

      connection.connect( cl );
      
      LGLogger.log(componentID, evtLifeCycle, LGLogger.SENT, "Creating outgoing connection to " + PEPeerTransportProtocol.this);
    }
    
  	public int process() {
        return PEPeerControl.WAITING_SLEEP;
  	}

  	public int getState() {
  		return CONNECTING;
  	}
  }
		
  private class 
  SOCKSStateHandshaking 
  	implements PEPeerTransportProtocolState 
  {
    DirectByteBuffer socks_handshake_read_buff;
    
    int	 	handshake_phase = 0;
    int 	socks_v5_reply_rem_length;
    
    
    protected AEMonitor	StateHandshaking_this_mon	= new AEMonitor( "PEPeerTransportProtocol:SOCKSStateHandshaking" );

    private SOCKSStateHandshaking() {
      allocateAll();
      startConnectionX();  //sets up the download speed limiter
    }
    
    public int process() 
    {
    	try{
    		StateHandshaking_this_mon.enter();
     	          		
    	    if ( socks_handshake_read_buff == null ){
           
    	    	int	next_handshake_phase	= 100;
    	    	int	expected_reply_size;
    	    	
    	    	ByteBuffer	socks_out	= ByteBuffer.allocate(256);
                
   	    	   	  
    	    	if ( socks_version.equals( "V4" )){
    	    		   	    		
                    socks_out.put((byte)4);					// socks 4(a)
	                socks_out.put((byte)1);					// command = CONNECT
	                socks_out.putShort((short)port);  
	                
	                try{
	                	
	                	byte[]	ip_bytes = InetAddress.getByName(ip).getAddress();
	                
		                socks_out.put(ip_bytes[0]);
		                socks_out.put(ip_bytes[1]);
		                socks_out.put(ip_bytes[2]);
		                socks_out.put(ip_bytes[3]);
	                
	                }catch( Throwable e ){
	                	
	                	Debug.printStackTrace(e);
	                	
	       				closeAll( PEPeerTransportProtocol.this + ": SOCKS StateHandshaking:: " + e, true, false );

	       				return(0);
	                }
	                
	                if ( socks_user.length() > 0 ){
	                	
	                	socks_out.put( socks_user.getBytes());
	                }	
	                
	                socks_out.put((byte)0);
	                
	                expected_reply_size		= 8;
	                
    	    	}else if ( socks_version.equals( "V4a" )){
    	    		   	    		
	                socks_out.put((byte)4);					// socks 4(a)
	                socks_out.put((byte)1);					// command = CONNECT
	                socks_out.putShort((short)port);        // port
	                socks_out.put((byte)0);
	                socks_out.put((byte)0);
	                socks_out.put((byte)0);
	                socks_out.put((byte)1);	// indicates socks 4a
	                
	                if ( socks_user.length() > 0 ){
	                	
	                	socks_out.put( socks_user.getBytes());
	                }
	                
	                socks_out.put((byte)0);
	                socks_out.put( ip.getBytes());
					socks_out.put((byte)0);
					
					expected_reply_size		= 8;
					
    	    	}else{
    	    		
    	    		if ( handshake_phase == 0 ){
    	    			
    	    				// say hello
    	    			
    	    			socks_out.put((byte)5);					// socks 5
    		            socks_out.put((byte)2);					// 2 methods
    		            socks_out.put((byte)0);					// no auth
    		            socks_out.put((byte)2);					// user/pw

    		            next_handshake_phase	= 1;
    		            
    		            expected_reply_size		= 2;
    		            
    	    		}else if ( handshake_phase == 1 ){
    	    			
    	    				// user/password auth
    	    			
      	    			socks_out.put((byte)1);							// user/pw version
       		            socks_out.put((byte)socks_user.length());		// user length
    		            socks_out.put(socks_user.getBytes());
       		            socks_out.put((byte)socks_password.length());	// password length
    		            socks_out.put(socks_password.getBytes());
 	
       		            next_handshake_phase	= 2;
       		         
    		            expected_reply_size		= 2;
    		            
    	    		}else if ( handshake_phase == 2 ){
    	    			
    	    				// request
    	    			
    	                socks_out.put((byte)5);				// version			
    	    			socks_out.put((byte)1);				// connect			
      	    			socks_out.put((byte)0);				// reserved			

    	                try{
    	                	
    	                	byte[]	ip_bytes = InetAddress.getByName(ip).getAddress();
    	                
         	    			socks_out.put((byte)1);				// IP4			

    		                socks_out.put(ip_bytes[0]);
    		                socks_out.put(ip_bytes[1]);
    		                socks_out.put(ip_bytes[2]);
    		                socks_out.put(ip_bytes[3]);
    	                
    	                }catch( Throwable e ){
    	                	
	      	    			socks_out.put((byte)3);				// address type = domain name			
	     	    			socks_out.put((byte)ip.length());	// address type = domain name			
	     	    			socks_out.put( ip.getBytes());
    	                }
    	                
	    	            socks_out.putShort((short)port);    // port

    	                	// reply has to be processed in two parts as it has
    	                	// a variable length component
    	                
      		            next_handshake_phase	= 3;

    	    			expected_reply_size		= 5;
    	    			
    	    		}else{
    	    			
     		            next_handshake_phase	= 100;

    	    			expected_reply_size		= socks_v5_reply_rem_length;
  	    			
    	    		}
    	    	}
    	    	
				socks_out.limit( socks_out.position());
				 
                socks_out.position(0);
                
                	// TODO: fix this!
                
                SocketChannel	chan = connection.getSocketChannel();
                
                try{
	                while( socks_out.position() != socks_out.limit() ){
	                	
	                	chan.write( socks_out );
	                }
	          	                
	                handshake_phase	= next_handshake_phase;
	    	        
                }catch (IOException e) {
    	        	
    				closeAll( PEPeerTransportProtocol.this + ": SOCKS StateHandshaking:: " + e, true, false );
    				
    				if ( socks_handshake_read_buff != null ){
    				
    					socks_handshake_read_buff.returnToPool();
    					
    					socks_handshake_read_buff	= null;
    				}
    				
    				return 0;
    			}  
                
            	if ( socks_handshake_read_buff != null ){
            		
            		socks_handshake_read_buff.returnToPool();
            	}
                
                socks_handshake_read_buff = DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_PT_READ, expected_reply_size );
                
                if ( socks_handshake_read_buff == null ) {
                	
                  closeAll( toString() + ": SOCKS handshake_read_buff is null", true, false );
                  
                  return(0);
                }
    	    }
    	  
	      if ( socks_handshake_read_buff.hasRemaining( DirectByteBuffer.SS_PEER ) ) {
	      	
	        try {
	        	
	          int read = readData( socks_handshake_read_buff );
	          
	          if( read == 0 ) {
	          	
	            return PEPeerControl.DATA_EXPECTED_SLEEP;
	            
	          }else if( read < 0 ){
	          	
				 throw new IOException( "SOCKS: End of Stream reached" );
	          }
	        }catch (IOException e) {
	        	
				closeAll( PEPeerTransportProtocol.this + ": SOCKS StateHandshaking:: " + e, true, false );
				
				socks_handshake_read_buff.returnToPool();
	        
				return 0;
			}
		}
	      
	      if( !socks_handshake_read_buff.hasRemaining( DirectByteBuffer.SS_PEER ) ) {
	      	
	      	try{
	      		/*
	      		socks_handshake_read_buff.position(DirectByteBuffer.SS_PEER ,0);
	        
  				byte[]	trace = new byte[socks_handshake_read_buff.limit(DirectByteBuffer.SS_PEER)];
  				
  				socks_handshake_read_buff.get( DirectByteBuffer.SS_PEER, trace );
  				
  				System.out.println( PEPeerTransportProtocol.this + ":state= " + handshake_phase + ", v5l = " + socks_v5_reply_rem_length + ", data = '" + new String(trace) + "' / " + ByteFormatter.nicePrint(trace) );
  				*/
	      		
  	      		socks_handshake_read_buff.position(DirectByteBuffer.SS_PEER ,0);
  	      	
	      		if ( socks_version.equals( "V4" ) || socks_version.equals( "V4a")){
	      			
			        byte	ver 	= socks_handshake_read_buff.get( DirectByteBuffer.SS_PEER  );
			        byte	resp 	= socks_handshake_read_buff.get( DirectByteBuffer.SS_PEER  );
	        
			        if ( ver != 0 || resp != 90 ){
			        	
						closeAll( PEPeerTransportProtocol.this + ": SOCKS StateHandshaking: connection declined (" + resp + ")", true, false );
			        
						return 0;
			        	
			        }
	      		}else{
	      			
	      				// version 5 replies
	      			
	      			if ( handshake_phase == 1 ){
	      				
	      					// reply from hello
	      				
	      					// version byte
	      				
				        socks_handshake_read_buff.get( DirectByteBuffer.SS_PEER  );
				        
				        byte	method 	= socks_handshake_read_buff.get( DirectByteBuffer.SS_PEER  );
		        
				        if ( method != 0 && method != 2 ){
					
				        	closeAll( PEPeerTransportProtocol.this + ": SOCKS StateHandshaking: no valid method (" + method + ")", true, false );
					        
							return 0;
				        }
				        
				        	// no auth -> go to request phase
				        
				        if ( method == 0 ){
				        	
				        	handshake_phase	= 2;
				        }
	      			}else if ( handshake_phase == 2 ){
	      				
	      					// reply from auth
	      				
	      					// version byte
	      				
				        socks_handshake_read_buff.get( DirectByteBuffer.SS_PEER  );
				        
				        byte	status 	= socks_handshake_read_buff.get( DirectByteBuffer.SS_PEER  );
		        
				        if ( status != 0 ){
					
				        	closeAll( PEPeerTransportProtocol.this + ": SOCKS StateHandshaking: authentication fails", true, false );
					        
							return 0;
				        }
				        
	      			}else if ( handshake_phase == 3 ){
	      				
	      					// reply from request, first part
	      				
	      					// version byte
	      				
				        socks_handshake_read_buff.get( DirectByteBuffer.SS_PEER  );
				        
				        byte	rep 	= socks_handshake_read_buff.get( DirectByteBuffer.SS_PEER  );		        
				        
				        if ( rep != 0 ){
				        	
				        	String	error_msgs[] = {
				        			"",
					        		"General SOCKS server failure",			        	
						            "connection not allowed by ruleset",	
						            "Network unreachable",	
						            "Host unreachable",	
						            "Connection refused",	
						            "TTL expired",	
						            "Command not supported",	
						            "Address type not supported",	
				        	};
				        	
				        	String	error_msg = rep<error_msgs.length?error_msgs[rep]:"Unknown error";
	
				        	closeAll( PEPeerTransportProtocol.this + ": SOCKS StateHandshaking: request failure ( " + error_msg + "/" + rep + " )", true, false );
					        
							return 0;
				        }
				        
				        	// reserved byte
				        
				        socks_handshake_read_buff.get( DirectByteBuffer.SS_PEER  );
				        
				        byte	atype 	= socks_handshake_read_buff.get( DirectByteBuffer.SS_PEER  );

				        byte	first_address_byte 	= socks_handshake_read_buff.get( DirectByteBuffer.SS_PEER  );

				        int	address_len;
				        
				        if ( atype == 1 ){
				        
				        	address_len = 3;	// already read one
				        	
				        }else if ( atype == 3 ){
				        		
				        		// domain name, first byte gives length of remainder
				        	
				        	address_len = first_address_byte;
				        	
				        }else{
				        	
				        	address_len	= 15;	// already read one
				        }
				        
				        socks_v5_reply_rem_length	= address_len + 2; // 2 for port
	      			}else{
	
	      				// last part of v5 request
	      			}
	      		}
	      	}finally{
	      		
		        socks_handshake_read_buff.returnToPool();
		        
		        socks_handshake_read_buff	= null;
	      	}
	      	
	      	if ( handshake_phase == 100 ){
	      		
	      		currentState = new StateHandshaking( true, null );
	      	}
	      }
	      
	      return PEPeerControl.NO_SLEEP;
	      
    	}finally{
    		StateHandshaking_this_mon.exit();
    	}
      }
    
	  public int getState() {
			return HANDSHAKING;
	  }
	}

  
  
  
  private class 
  StateHandshaking 
  	implements PEPeerTransportProtocolState 
  {
    DirectByteBuffer handshake_read_buff;
    boolean sent_our_handshake = false;
  
    protected AEMonitor	StateHandshaking_this_mon	= new AEMonitor( "PEPeerTransportProtocol:StateHandshaking" );

    private StateHandshaking( boolean already_initialised, byte[] data_already_read ) {
    	if ( !already_initialised ){
    		allocateAll();
    		startConnectionX();  //sets up the download speed limiter
    	}
      
      handshake_read_buff = DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_PT_READ, 68 );
      if( handshake_read_buff == null ) {
        closeAll( toString() + ": handshake_read_buff is null", true, false );
        return;
      }

      if( data_already_read != null ) {
        handshake_read_buff.put( DirectByteBuffer.SS_PEER, data_already_read );
      }
    }
    
    public int process() 
    {
    	try{
    		StateHandshaking_this_mon.enter();
     
	      if( !sent_our_handshake ) {
	        connection.getOutgoingMessageQueue().addMessage( new BTHandshake( manager.getHash(), manager.getPeerId() ), false );
	        sent_our_handshake = true;
	      }
	      
	      if( handshake_read_buff.hasRemaining( DirectByteBuffer.SS_PEER ) ) {
	        try {
	          int read = readData( handshake_read_buff );
	          if( read == 0 ) {
	            return PEPeerControl.DATA_EXPECTED_SLEEP;
	          }
	          else if( read < 0 ) {
							throw new IOException( "End of Stream reached" );
	          }
	        }
	        catch (IOException e) {
						closeAll( PEPeerTransportProtocol.this + ": StateHandshaking:: " + e, true, false );
						handshake_read_buff.returnToPool();
	          return 0;
					}
				}
	      
	      if( !handshake_read_buff.hasRemaining( DirectByteBuffer.SS_PEER ) ) { //we've read all their handshake in
	        handleHandShakeResponse( handshake_read_buff );
	      }
	      
	      return PEPeerControl.NO_SLEEP;
	      
    	}finally{
    		StateHandshaking_this_mon.exit();
    	}
      }
    
	  public int getState() {
			return HANDSHAKING;
	  }
	}

  
  protected void handleHandShakeResponse( DirectByteBuffer handshake_data ) {
    handshake_data.position(DirectByteBuffer.SS_PEER, 0);

    byte b;
    if ((b = handshake_data.get(DirectByteBuffer.SS_PEER)) != (byte) BTHandshake.PROTOCOL.length()) {
      closeAll(toString() + " has sent handshake, but handshake starts with wrong byte : " + b,true, true);
      handshake_data.returnToPool();
      return;
    }

    byte[] protocol = BTHandshake.PROTOCOL.getBytes();
    if (handshake_data.remaining(DirectByteBuffer.SS_PEER) < protocol.length) {
      closeAll(toString() + " has sent handshake, but handshake is of wrong size : " + handshake_data.remaining(DirectByteBuffer.SS_PEER),true, true);
      handshake_data.returnToPool();
      return;
    }
    else {
      handshake_data.get(DirectByteBuffer.SS_PEER,protocol);
      if (!(new String(protocol)).equals(BTHandshake.PROTOCOL)) {
        closeAll(toString() + " has sent handshake, but protocol is wrong : " + new String(protocol),true, false);
        handshake_data.returnToPool();
        return;
      }
    }

    byte[] reserved = new byte[8];
    if (handshake_data.remaining(DirectByteBuffer.SS_PEER) < reserved.length) {
      closeAll(toString() + " has sent handshake, but handshake is of wrong size(2) : " + handshake_data.remaining(DirectByteBuffer.SS_PEER),true, true);
      handshake_data.returnToPool();
      return;
    }
    else handshake_data.get(DirectByteBuffer.SS_PEER,reserved);
    //Ignores reserved bytes

    byte[] hash = manager.getHash();
    byte[] otherHash = new byte[20];
    if (handshake_data.remaining(DirectByteBuffer.SS_PEER) < otherHash.length) {
      closeAll(toString() + " has sent handshake, but handshake is of wrong size(3) : " + handshake_data.remaining(DirectByteBuffer.SS_PEER),true, true);
      handshake_data.returnToPool();
      return;
    }
    else {
      handshake_data.get(DirectByteBuffer.SS_PEER,otherHash);
      for (int i = 0; i < 20; i++) {
        if (otherHash[i] != hash[i]) {
          closeAll(toString() + " has sent handshake, but infohash is wrong",true, false);
          handshake_data.returnToPool();
          return;
        }
      }
    }

    byte[] otherPeerId = new byte[20];
    if (handshake_data.remaining(DirectByteBuffer.SS_PEER) < otherPeerId.length) {
      closeAll(toString() + " has sent handshake, but handshake is of wrong size(4) : " + handshake_data.remaining(DirectByteBuffer.SS_PEER),true, true);
      handshake_data.returnToPool();
      return;
    }
    handshake_data.get( DirectByteBuffer.SS_PEER, otherPeerId );

    this.id = otherPeerId;

    //decode a client identification string from the given peerID
    client = PeerClassifier.getClientDescription( otherPeerId );

    //make sure the client type is not banned
    if( !PeerClassifier.isClientTypeAllowed( client ) ) {
      closeAll( client + " client type not allowed to connect, banned", false, false );
      handshake_data.returnToPool();
      return;
    }

    //make sure we are not connected to ourselves
    if( Arrays.equals( manager.getPeerId(), otherPeerId ) ) {
      closeAll( "OOPS, peerID matches myself", false, false );
      handshake_data.returnToPool();
      return;
    }

    //make sure we are not already connected to this peer
    boolean sameIdentity = PeerIdentityManager.containsIdentity( otherHash, otherPeerId );
    boolean sameIP = false;
    if( !COConfigurationManager.getBooleanParameter( "Allow Same IP Peers" ) ) {
      if( PeerIdentityManager.containsIPAddress( otherHash, ip ) ) {
        sameIP = true;
      }
    }
    if( sameIdentity ) {
      closeAll( toString() + " exchanged handshake, but peer matches pre-existing identity", false, false );
      handshake_data.returnToPool();
      return;
    }
    if( sameIP ) {
      closeAll( toString() + " exchanged handshake, but peer matches pre-existing IP address", false, false );
      handshake_data.returnToPool();
      return;
    }

    //make sure we haven't reached our connection limit
    int maxAllowed = PeerUtils.numNewConnectionsAllowed( otherHash );
    if( maxAllowed == 0 ) {
      closeAll( "Too many existing peer connections", false, false );
      handshake_data.returnToPool();
      return;
    }

    PeerIdentityManager.addIdentity( otherHash, otherPeerId, ip );
    identityAdded = true;

    LGLogger.log( componentID, evtLifeCycle, LGLogger.RECEIVED, toString() + " has sent their handshake" );

    sendBitField();
    manager.peerAdded( this );
    handshake_data.returnToPool();
    currentState = new StateTransfering();
  }
  
  
  
  
private class StateTransfering implements PEPeerTransportProtocolState {
  boolean readingLength = true;
  DirectByteBuffer message_read_buff;
  
  protected AEMonitor	StateTransfering_this_mon	= new AEMonitor( "PEPeerTransportProtocol:StateTransfering" );

  public int process() 
  {
  	try{
  		StateTransfering_this_mon.enter();
  	
	    if( ++processLoop > 10 ) {
	      return PEPeerControl.NO_SLEEP;
	    }
	          
	    if (readingLength) {
	      if (lengthBuffer.hasRemaining(DirectByteBuffer.SS_PEER) ) {          
	        try {
	          int read = readData(lengthBuffer);
	          
	          if (read == 0) {
	            //If there's nothing pending on the socket, then
	            //we can quite safely send a request while we wait
	            if(lengthBuffer.remaining(DirectByteBuffer.SS_PEER) == 4) {
	              readyToRequest = true;
	              return PEPeerControl.WAITING_SLEEP;
	            }
	            else {
	              //wait a bit before trying again
	              return PEPeerControl.DATA_EXPECTED_SLEEP;
	            }
	          }
	          else if (read < 0) {
	            throw new IOException("End of Stream Reached");
	          }
	        }
	        catch (IOException e) {
	          closeAll(PEPeerTransportProtocol.this + " : StateTransfering::" + e.getMessage()+ " (reading length)",true, true);
	          return PEPeerControl.NO_SLEEP;
	        }
				}
	
				if (!lengthBuffer.hasRemaining(DirectByteBuffer.SS_PEER)) {
					int length = lengthBuffer.getInt(DirectByteBuffer.SS_PEER,0);
	
	        //this message-length-read round is done, time to read the payload
	        readingLength = false;
	        //reset the position for next round
	        lengthBuffer.position( DirectByteBuffer.SS_PEER, 0 );
	        
					if(length < 0) {
						closeAll(PEPeerTransportProtocol.this + " : length negative : " + length,true, true);
						return PEPeerControl.NO_SLEEP;
					}
	      
					//message size should never be greater than 16KB+9B, as we never request chunks > 16KB
					if( length > 16393 ) {
						closeAll(PEPeerTransportProtocol.this + " : incoming message size too large: " + length,true, true);
						return PEPeerControl.NO_SLEEP;
					}
	        
					if (length > 0) {
	          message_read_buff = DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_PT_READ, length );
	          if( message_read_buff == null) {
	            closeAll( PEPeerTransportProtocol.this + ": message_read_buff is null", true, false );
	            return PEPeerControl.NO_SLEEP;
	          }
	          
						//'piece' data messages are greater than length 13
	          if ( length > 13 ) {
	            readyToRequest = true;
	          }
	          //protocol message, don't request until we know what the message is
	          else {
	            readyToRequest = false;
	          }
					}
					else {
					  //length is 0 : Keep alive message, process next.
            LGLogger.log( componentID, evtProtocol, LGLogger.RECEIVED, PEPeerTransportProtocol.this + " sent keep-alive" );
						readingLength = true;
					}
				}
			}
	    
		  if (!readingLength) {
		  	try {
		  		int read = readData( message_read_buff );
	        
	        if (read == 0) {
	          //nothing on the socket, wait a bit before trying again
	          return PEPeerControl.DATA_EXPECTED_SLEEP;
	        }
		  		else if (read < 0) {
		  			throw new IOException("End of Stream Reached");
		  		}
		  		else {
		  		  if (message_read_buff.limit(DirectByteBuffer.SS_PEER) > 13) {
		  		    stats.received(read);
		  		  }
		  		}
		  	}
		  	catch (IOException e) {
		  		closeAll(PEPeerTransportProtocol.this + " : StateTransfering::End of Stream Reached (reading data)",true, true);
		  		message_read_buff.returnToPool();
	        return PEPeerControl.NO_SLEEP;
		  	}
	    
		  	if( !message_read_buff.hasRemaining(DirectByteBuffer.SS_PEER) ) {
		  		//After each message has been received, we're not ready to request anymore,
		  		//Unless we finish the socket's queue, or we start receiving a piece.
		  		readyToRequest = false;
	        
	        readingLength = analyzeIncomingMessage( message_read_buff );
	        
		  		if( getState() == TRANSFERING && readingLength ) {
		  			process();
		  			return PEPeerControl.NO_SLEEP;
		  		}
		  	}
		  }
	    
	      return PEPeerControl.NO_SLEEP;
  	  }finally{
  		 StateTransfering_this_mon.exit();
  	  }
	}

	public int getState() {
	  return TRANSFERING;
	}
  }

  private static class StateClosing implements PEPeerTransportProtocolState {
    public int process() { return PEPeerControl.NO_SLEEP; }

    public int getState() {
      return CLOSING;
    }
  }

  /*
  private static class StateClosed implements PEPeerTransportProtocolState {
  	public int process() { return PEPeerControl.NO_SLEEP; }

  	public int getState() {
  		return DISCONNECTED;
  	}
  }
  */

  
  public int processRead() {
  	try {
  		processLoop = 0;
  		if (currentState != null) {
  			return currentState.process();
  		}
      else return PEPeerControl.NO_SLEEP;
  	}
  	catch (Exception e) {
  		Debug.printStackTrace( e );
  		closeAll(toString() + " : Exception in process : " + e,true, false);
      return PEPeerControl.NO_SLEEP;
  	}
  }
  

  public int getState() {
	if (currentState != null)
	  return currentState.getState();
	return 0;
  }

  public int getPercentDone() {
	int sum = 0;
	for (int i = 0; i < other_peer_has_pieces.length; i++) {
	  if (other_peer_has_pieces[i])
		sum++;
	}

	sum = (sum * 1000) / other_peer_has_pieces.length;
	return sum;
  }

  public boolean transferAvailable() {
	return (!choked && interested_in_other_peer);
  }

  
  private boolean analyzeIncomingMessage( DirectByteBuffer message_buff ) {
    boolean logging_is_on = LGLogger.isLoggingOn();
    
    message_buff.position( DirectByteBuffer.SS_PEER, 0 );
    
    int pieceNumber, pieceOffset, pieceLength;
    byte cmd = message_buff.get( DirectByteBuffer.SS_PEER );
    switch( cmd ) {
      case BT_CHOKED :
        if( message_buff.limit( DirectByteBuffer.SS_PEER ) != 1 ) {
          closeAll( toString() + " choking received, but message of wrong size : " + message_buff.limit( DirectByteBuffer.SS_PEER ), true, true );
          message_buff.returnToPool();
          return false;
        }
        if( logging_is_on ) {
          LGLogger.log( componentID, evtProtocol, LGLogger.RECEIVED, toString() + " is choking you" );
        }
        choked = true;
        cancelRequests();
        message_buff.returnToPool();
        return true;
      case BT_UNCHOKED :
        if( message_buff.limit( DirectByteBuffer.SS_PEER ) != 1 ) {
          closeAll( toString() + " unchoking received, but message of wrong size : " + message_buff.limit( DirectByteBuffer.SS_PEER ), true, true );
          message_buff.returnToPool();
          return false;
        }
        if( logging_is_on ) {
          LGLogger.log( componentID, evtProtocol, LGLogger.RECEIVED, toString() + " is unchoking you" );
        }
        choked = false;
        message_buff.returnToPool();
        return true;
      case BT_INTERESTED :
        if( message_buff.limit( DirectByteBuffer.SS_PEER ) != 1 ) {
          closeAll( toString() + " interested received, but message of wrong size : " + message_buff.limit( DirectByteBuffer.SS_PEER ), true, true );
          message_buff.returnToPool();
          return false;
        }
        if( logging_is_on ) {
          LGLogger.log( componentID, evtProtocol, LGLogger.RECEIVED, toString() + " is interested" );
        }
        other_peer_interested_in_me = true;
        message_buff.returnToPool();
        return true;
      case BT_UNINTERESTED :
        if( message_buff.limit( DirectByteBuffer.SS_PEER ) != 1 ) {
          closeAll( toString() + " uninterested received, but message of wrong size : " + message_buff.limit( DirectByteBuffer.SS_PEER ), true, true );
          message_buff.returnToPool();
          return false;
        }
        if( logging_is_on ) {
          LGLogger.log( componentID, evtProtocol, LGLogger.RECEIVED, toString() + " is not interested" );
        }
        other_peer_interested_in_me = false;

        //force send any pending haves in case one of them would make the other peer interested again
        if( outgoing_have_message_aggregator != null ) {
          outgoing_have_message_aggregator.forceSendOfPending();
        }

        message_buff.returnToPool();
        return true;
      case BT_HAVE :
        if( message_buff.limit( DirectByteBuffer.SS_PEER ) != 5 ) {
          closeAll( toString() + " have received, but message of wrong size : " + message_buff.limit( DirectByteBuffer.SS_PEER ), true, true );
          message_buff.returnToPool();
          return false;
        }
        pieceNumber = message_buff.getInt( DirectByteBuffer.SS_PEER );
        if( logging_is_on ) {
          LGLogger.log( componentID, evtProtocol, LGLogger.RECEIVED, toString() + " has " + pieceNumber );
        }
        have( pieceNumber );
        message_buff.returnToPool();
        return true;
      case BT_BITFIELD :
        if( logging_is_on ) {
          LGLogger.log( componentID, evtProtocol, LGLogger.RECEIVED, toString() + " has sent BitField" );
        }
        setBitField( message_buff );
        checkInterested();
        checkSeed();
        message_buff.returnToPool();
        return true;
      case BT_REQUEST :
        if( message_buff.limit( DirectByteBuffer.SS_PEER ) != 13 ) {
          closeAll( toString() + " request received, but message of wrong size : " + message_buff.limit( DirectByteBuffer.SS_PEER ), true, true );
          message_buff.returnToPool();
          return false;
        }
        pieceNumber = message_buff.getInt( DirectByteBuffer.SS_PEER );
        pieceOffset = message_buff.getInt( DirectByteBuffer.SS_PEER );
        pieceLength = message_buff.getInt( DirectByteBuffer.SS_PEER );

        if( logging_is_on ) {
          LGLogger.log( componentID, evtProtocol, LGLogger.RECEIVED, toString() + " has requested #" + pieceNumber + ":" + pieceOffset + "->" + (pieceOffset + pieceLength - 1) );
        }

        if( manager.checkBlock( pieceNumber, pieceOffset, pieceLength ) ) {
          if( !choking ) {
            outgoing_piece_message_handler.addPieceRequest( pieceNumber, pieceOffset, pieceLength );
          }
          else {
            LGLogger.log( componentID, evtProtocol, LGLogger.RECEIVED, toString() + " has requested #" + pieceNumber + ":" + pieceOffset + "->" + (pieceOffset + pieceLength) + " but peer is currently choked. Request dropped" );
          }
        }
        else {
          closeAll( toString() + " has requested #" + pieceNumber + ":" + pieceOffset + "->" + (pieceOffset + pieceLength) + " which is an invalid request.", true, true );
          message_buff.returnToPool();
          return false;
        }
        message_buff.returnToPool();
        return true;
      case BT_PIECE :
        if( message_buff.limit( DirectByteBuffer.SS_PEER ) < 9 ) {
          closeAll( toString() + " piece block received, but message of wrong size : " + message_buff.limit( DirectByteBuffer.SS_PEER ), true, true );
          message_buff.returnToPool();
          return false;
        }
        pieceNumber = message_buff.getInt( DirectByteBuffer.SS_PEER );
        pieceOffset = message_buff.getInt( DirectByteBuffer.SS_PEER );
        pieceLength = message_buff.limit( DirectByteBuffer.SS_PEER ) - message_buff.position( DirectByteBuffer.SS_PEER );
        
        String msg = "";
        if( logging_is_on ) {
          msg += toString() + " has sent #" + pieceNumber + ": " + pieceOffset + "->" + (pieceOffset + pieceLength - 1);
        }
        
        if ( AEDiagnostics.CHECK_DUMMY_FILE_DATA ){
        	
        	int	pos = message_buff.position( DirectByteBuffer.SS_PEER );
        	
        	long	offset = ((long)pieceNumber)*getControl().getPieceLength(0) + pieceOffset;
        	
        	for (int i=0;i<pieceLength;i++){
        		
				byte	v = message_buff.get( DirectByteBuffer.SS_PEER );
				
				if ((byte)offset != v ){
					
					System.out.println( "piece: read is bad at " + offset +
										": expected = " + (byte)offset + ", actual = " + v );
					
					break;
				}
				
				offset++;       		
        	}
        	
        	message_buff.position( DirectByteBuffer.SS_PEER, pos );
        }
        
        DiskManagerReadRequest request = manager.createDiskManagerRequest( pieceNumber, pieceOffset, pieceLength );

        if( manager.checkBlock( pieceNumber, pieceOffset, message_buff ) ) {
          if( alreadyRequested( request ) ) {  //from active request
            removeRequest( request );
            setSnubbed( false );
            reSetRequestsTime();
            
            if( manager.isBlockAlreadyWritten( pieceNumber, pieceOffset ) ) {  //oops, looks like this block has already been downloaded
              //we're probably in end-game mode then
              if( manager.isInEndGameMode() )  msg += ", but piece block ignored as already written in end-game mode";
              else  msg += ", but piece block ignored as already written";
              message_buff.returnToPool();
            }
            else {
              manager.received( pieceLength );
              manager.writeBlock( pieceNumber, pieceOffset, message_buff, this );
              requests_completed++;
            }
          }
          else { //initial request may have already expired, but check if we can use the data anyway
            if( !manager.isBlockAlreadyWritten( pieceNumber, pieceOffset ) ) {
              boolean ever_requested;
              try{
              	recent_outgoing_requests_mon.enter();
              
                ever_requested = recent_outgoing_requests.containsKey( request );
              }finally{
              	
              	recent_outgoing_requests_mon.exit();
              }
              if( ever_requested ) { //security-measure: we dont want to be accepting any ol' random block
                msg += ", piece block data recovered as useful";
                manager.received( pieceLength );
                setSnubbed( false );
                reSetRequestsTime();
                manager.writeBlockAndCancelOutstanding( pieceNumber, pieceOffset, message_buff, this );
                requests_recovered++;
                printRequestStats();
              }
              else {
                msg += ", but piece block discarded as never requested";
                stats.discarded( pieceLength );
                manager.discarded( pieceLength );
                message_buff.returnToPool();
                requests_discarded++;
                printRequestStats();
              }
            }
            else {
              msg += ", but piece block discarded as already written";
              stats.discarded( pieceLength );
              manager.discarded( pieceLength );
              message_buff.returnToPool();
              requests_discarded++;
              printRequestStats();
            }
          }
        }
        else {
          msg += ", but piece block discarded as invalid";
          stats.discarded( pieceLength );
          manager.discarded( pieceLength );
          message_buff.returnToPool();
          requests_discarded++;
          printRequestStats();
        }

        if( logging_is_on ) {
          LGLogger.log( componentID, evtProtocol, LGLogger.RECEIVED, msg );
        }
        return true;
      case BT_CANCEL :
        if( message_buff.limit( DirectByteBuffer.SS_PEER ) != 13 ) {
          closeAll( toString() + " cancel received, but message of wrong size : " + message_buff.limit( DirectByteBuffer.SS_PEER ), true, true );
          message_buff.returnToPool();
          return false;
        }
        pieceNumber = message_buff.getInt( DirectByteBuffer.SS_PEER );
        pieceOffset = message_buff.getInt( DirectByteBuffer.SS_PEER );
        pieceLength = message_buff.getInt( DirectByteBuffer.SS_PEER );
        if( logging_is_on ) {
          LGLogger.log( componentID, evtProtocol, LGLogger.RECEIVED, toString() + " has canceled #" + pieceNumber + ":" + pieceOffset + "->" + (pieceOffset + pieceLength - 1) );
        }
        outgoing_piece_message_handler.removePieceRequest( pieceNumber, pieceOffset, pieceLength );
        message_buff.returnToPool();
        return true;
      default :
        Debug.out( toString() + " has sent an unknown protocol message id: " + cmd );
        closeAll( toString() + " has sent a wrong message " + cmd, true, true );
        message_buff.returnToPool();
        return false;
    }
  }
  
  
  private void printRequestStats() {
    if( SHOW_DISCARD_RATE_STATS ) {
      float discard_percentage = (requests_discarded * 100F) / ((requests_completed + requests_recovered + requests_discarded) * 1F);
      float recover_percentage = (requests_recovered * 100F) / ((requests_recovered + requests_discarded) * 1F);
      System.out.println( "c="+requests_completed+ " d="+requests_discarded+ " r="+requests_recovered+ " dp="+discard_percentage+ "% rp="+recover_percentage+ "%" );
    }
  }
  

  private void have(int pieceNumber) {
    if ((pieceNumber >= other_peer_has_pieces.length) || (pieceNumber < 0)) {
      closeAll(toString() + " gave invalid pieceNumber:" + pieceNumber,true, true);
    }
    else {    
      other_peer_has_pieces[pieceNumber] = true;
      int pieceLength = manager.getPieceLength(pieceNumber);
      stats.haveNewPiece(pieceLength);
      manager.havePiece(pieceNumber, pieceLength, this);
      if (!interested_in_other_peer) {
        checkInterested(pieceNumber);
      }
      checkSeed();
    }
  }

  /**
	 * Checks if it's a seed or not.
	 */
  private void checkSeed() {
	for (int i = 0; i < other_peer_has_pieces.length; i++) {
	  if (!other_peer_has_pieces[i])
		return;
	}
	seed = true;
  }


  public boolean request( int pieceNumber, int pieceOffset, int pieceLength) {
  	if (getState() != TRANSFERING) {
  		manager.requestCanceled( manager.createDiskManagerRequest( pieceNumber, pieceOffset, pieceLength ) );
  		return false;
  	}	
  	DiskManagerReadRequest request = manager.createDiskManagerRequest( pieceNumber, pieceOffset, pieceLength );
  	if( !alreadyRequested( request ) ) {
  		addRequest( request );
      try{
      	recent_outgoing_requests_mon.enter();
      
        recent_outgoing_requests.put( request, null );
      }finally{
      	recent_outgoing_requests_mon.exit();
      }
      connection.getOutgoingMessageQueue().addMessage( new BTRequest( pieceNumber, pieceOffset, pieceLength ), false );
  		return true;
  	}
  	return false;
  }
  

  public void sendCancel( DiskManagerReadRequest request ) {
  	if ( getState() != TRANSFERING ) return;
		if ( alreadyRequested( request ) ) {
			removeRequest( request );
      connection.getOutgoingMessageQueue().addMessage( new BTCancel( request.getPieceNumber(), request.getOffset(), request.getLength() ), false );
		}
  }

  
  public void sendHave( int pieceNumber ) {
		if ( getState() != TRANSFERING ) return;
    //only force if the other peer doesn't have this piece and is not yet interested
    boolean force = !other_peer_has_pieces[ pieceNumber ] && !other_peer_interested_in_me;
    outgoing_have_message_aggregator.queueHaveMessage( pieceNumber, force );
		checkInterested();
	}

  
  public void sendChoke() {
  	if ( getState() != TRANSFERING ) return;
    outgoing_piece_message_handler.removeAllPieceRequests();
    connection.getOutgoingMessageQueue().addMessage( new BTChoke(), false );
  	choking = true;
  }

  
  public void sendUnChoke() {
    if ( getState() != TRANSFERING ) return;
    connection.getOutgoingMessageQueue().addMessage( new BTUnchoke(), false );
    choking = false;
  }


  private void sendKeepAlive() {
    if ( getState() != TRANSFERING ) return;
    connection.getOutgoingMessageQueue().addMessage( new BTKeepAlive(), false );
  }
  
  
  private void setBitField(DirectByteBuffer buffer) {
	byte[] dataf = new byte[(manager.getPiecesNumber() + 7) / 8];
   
	if (buffer.remaining(DirectByteBuffer.SS_PEER) < dataf.length) {
     LGLogger.log(componentID, evtProtocol, LGLogger.ERROR, 
                  toString() + " has sent invalid BitField: too short");
	  return;
   }
   
	buffer.get(DirectByteBuffer.SS_PEER,dataf);
	for (int i = 0; i < other_peer_has_pieces.length; i++) {
	  int index = i / 8;
	  int bit = 7 - (i % 8);
	  byte bData = dataf[index];
	  byte b = (byte) (bData >> bit);
	  if ((b & 0x01) == 1) {
	    other_peer_has_pieces[i] = true;
	    manager.updateSuperSeedPiece(this,i);
	  }
	  else {
      other_peer_has_pieces[i] = false;
	  }
	}
  }

  
  /**
   * Global checkInterested method.
   * Scans the whole pieces to determine if it's interested or not
   */
  private void checkInterested() {
		boolean newInterested = false;
		DiskManagerPiece[]	pieces = manager.getDiskManager().getPieces();
		
		for (int i = 0; i < pieces.length; i++) {
			if ( !pieces[i].getDone() && other_peer_has_pieces[i] ) {
				newInterested = true;
				break;
			}
		}
		if ( newInterested && !interested_in_other_peer ) {
      connection.getOutgoingMessageQueue().addMessage( new BTInterested(), false );
		}
    else if ( !newInterested && interested_in_other_peer ) {
      connection.getOutgoingMessageQueue().addMessage( new BTUninterested(), false );
		}
		interested_in_other_peer = newInterested;
	}

  
  /**
   * Checks interested given a new piece received
   * @param pieceNumber the piece number that has been received
   */
  private void checkInterested( int pieceNumber ) {
  		DiskManagerPiece[]	pieces = manager.getDiskManager().getPieces();
		boolean newInterested = !pieces[ pieceNumber ].getDone();
		if ( newInterested && !interested_in_other_peer ) {
      connection.getOutgoingMessageQueue().addMessage( new BTInterested(), false );
		}
    else if ( !newInterested && interested_in_other_peer ) {
      connection.getOutgoingMessageQueue().addMessage( new BTUninterested(), false );
		}
		interested_in_other_peer = newInterested;
	}
  

  /**
   * Private method to send the bitfield.
   * The bitfield will only be sent if there is at least one piece available.
   */
  private void sendBitField() {
		//In case we're in super seed mode, we don't send our bitfield
		if ( manager.isSuperSeedMode() ) return;
    
    //create bitfield
		ByteBuffer buffer = ByteBuffer.allocate( (manager.getPiecesNumber() + 7) / 8 );
		boolean atLeastOne = false;
		
		DiskManagerPiece[]	pieces = manager.getDiskManager().getPieces();
		
		int bToSend = 0;
		int i = 0;
		for (; i < pieces.length; i++) {
			if ( (i % 8) == 0 ) bToSend = 0;
			bToSend = bToSend << 1;
			if ( pieces[i].getDone()) {
				bToSend += 1;
				atLeastOne = true;
			}
			if ( (i % 8) == 7 ) buffer.put( (byte)bToSend );
		}
		if ( (i % 8) != 0 ) {
			bToSend = bToSend << (8 - (i % 8));
			buffer.put( (byte)bToSend );
		}

		if ( atLeastOne ) {
      connection.getOutgoingMessageQueue().addMessage( new BTBitfield( buffer ), false );
		}
	}

  
  public byte[] getId() {  return id;  }
  public String getIp() {  return ip;  }
  public int getPort() {  return port;  }
  public String getClient() {  return client;  }
  
  public boolean isIncoming() {  return incoming;  }  
  public boolean isOptimisticUnchoke() {  return manager.isOptimisticUnchoke(this);  }
  public boolean isReadyToRequest() {  return readyToRequest;  }
  
  public PEPeerControl getControl() {  return manager;  }
  public PEPeerManager getManager() {  return manager;  }
  public PEPeerStats getStats() {  return stats;  }
  
  public boolean[] getAvailable() {  return other_peer_has_pieces;  }
  public boolean isChoked() {  return choked;  }
  public boolean isChoking() {  return choking;  }
  public boolean isInterested() {  return interested_in_other_peer;  }
  public boolean isInteresting() {  return other_peer_interested_in_me;  }
  public boolean isSeed() {  return seed;  }
  public boolean isSnubbed() {  return snubbed;  }

  public void setChoked(boolean b) {  choked = b;  }
  public void setChoking(boolean b) {  choking = b;  }
  public void setInterested(boolean b) {  interested_in_other_peer = b;  }
  public void setInteresting(boolean b) {  other_peer_interested_in_me = b;  }
  public void setSeed(boolean b) {  seed = b;  }
  public void setSnubbed(boolean b) {  snubbed = b;  }

  //TODO: remove abstract methods
  protected abstract void startConnectionX();
  protected abstract void closeConnectionX();
  protected abstract void setChannel( SocketChannel channel );
  protected abstract int readData( DirectByteBuffer	buffer ) throws IOException;
  
  public void hasSentABadChunk() {  nbBadChunks++;  }
  public int getNbBadChunks() {  return nbBadChunks;  }

  public void setUploadHint(int spreadTime) {  spreadTimeHint = spreadTime;  }
  public int getUploadHint() {  return spreadTimeHint;  }
  public void setUniqueAnnounce(int _uniquePiece) {  uniquePiece = _uniquePiece;  }
  public int getUniqueAnnounce() {  return uniquePiece;  }

  public int getReadSleepTime() { return readSleepTime; }
  public void setReadSleepTime(int time) { readSleepTime = time; }
  public long getLastReadTime() { return lastReadTime; }
  public void setLastReadTime(long time) { lastReadTime = time; }


  /** To retreive arbitrary objects against a peer. */
  public Object getData (String key) {
  	if (data == null) return null;
    return data.get(key);
  }

  /** To store arbitrary objects against a peer. */
  public void setData (String key, Object value) {
  	try{
  		this_mon.enter();
  	
	  	if (data == null) {
	  	  data = new HashMap();
	  	}
	    if (value == null) {
	      if (data.containsKey(key))
	        data.remove(key);
	    } else {
	      data.put(key, value);
	    }
  	}finally{
  		this_mon.exit();
  	}
  }




	public String
	getIPHostName()
	{
		if ( ip_resolved == null ){
			
			ip_resolved = ip;
		
			IPToHostNameResolver.addResolverRequest( 
				ip_resolved,
				new IPToHostNameResolverListener()
				{
					public void
					IPResolutionComplete(
						String		res,
						boolean		ok )
					{
						ip_resolved	= res;
					}
				});
		}
		
		return( ip_resolved );
	}
	

  /////////////////////////////////////////////////////////////////
  /*
  public boolean equals( Object obj ) {
    if (this == obj)  return true;
    if (obj != null && obj instanceof PEPeerTransportProtocol) {
    	PEPeerTransportProtocol other = (PEPeerTransportProtocol)obj;
      if ( this.ip.equals(other.ip) && this.port == other.port ) {
      	return true;
      }
    }
    return false;
  }
  public int hashCode() {  return hashcode;  }
  */
  /////////////////////////////////////////////////////////////////


		protected void cancelRequests() 
		{
			if ( requested != null ) {
		        try{
		        	requested_mon.enter();
		        
		          for (int i = requested.size() - 1; i >= 0; i--) {
		            DiskManagerReadRequest request = (DiskManagerReadRequest) requested.remove(i);
		            manager.requestCanceled(request);
		          }
		        }finally{
		        	
		        	requested_mon.exit();
		        }
	      }
	      if( !closing ) {
	        //cancel any unsent requests in the queue
	        int[] type = { BTProtocolMessage.BT_REQUEST };
	        connection.getOutgoingMessageQueue().removeMessagesOfType( type, false );
	      }
		}

		public int 
		getNbRequests() {
			return requested.size();
		}

		/**
		 * 
		 * @return	may be null for performance purposes
		 */
		
		public List 
		getExpiredRequests() {
			List result = null;
			try{
				requested_mon.enter();
			
		    	for (int i = 0; i < requested.size(); i++) {
		    		try {
		    			DiskManagerReadRequest request = (DiskManagerReadRequest) requested.get(i);
		    			if (request.isExpired()) {
		    				if ( result == null ){
		    					result = new ArrayList();
		    				}
		    				result.add(request);
		    			}
		    		}
		    		catch (ArrayIndexOutOfBoundsException e) {
		    			//Keep going, most probably, piece removed...
		    			//Hopefully we'll find it later :p
		    			Debug.printStackTrace( e );
		    		}
		    	}
		    }finally{
		    	
		    	requested_mon.exit();
		    }
			return result;
		}
		
		protected boolean
		alreadyRequested(
			DiskManagerReadRequest	request )
		{
	    try{
	    	requested_mon.enter();
	    
	      return requested.contains( request );
	    }finally{
	    	requested_mon.exit();
	    }
		}
		
		protected void
		addRequest(
			DiskManagerReadRequest	request )
		{
			try{
				requested_mon.enter();
			
				requested.add(request);
			}finally{
				
				requested_mon.exit();
			}
		}
		
		protected void
		removeRequest(
			DiskManagerReadRequest	request )
		{
	    	try{
	    		requested_mon.enter();
	    	
	    		requested.remove(request);
	    	}finally{
	    		
	    		requested_mon.exit();
	    	}
	    	BTRequest msg = new BTRequest( request.getPieceNumber(), request.getOffset(), request.getLength() );
	    	connection.getOutgoingMessageQueue().removeMessage( msg, false );
        msg.destroy();  //we need to destroy this manually, as the queue removal only destroys the original message
                        //in the queue, not the new one we just created
		}
		
		protected void 
		reSetRequestsTime() 
		{
			try{
			  requested_mon.enter();
	    
			  for (int i = 0; i < requested.size(); i++) {
			  	DiskManagerReadRequest request = null;
			  	try {
			  		request = (DiskManagerReadRequest) requested.get(i);
			  	}
			  	catch (Exception e) { Debug.printStackTrace( e );}
	        
			  	if (request != null)
			  		request.reSetTime();
			  }
			}finally{
				
				requested_mon.exit();
			}
		}
    

	public String toString() {
    return ip + ":" + port + " [" + client+ "]";
	}
	
	public void
	addListener(
		PEPeerListener	l )
	{
		try{
			this_mon.enter();
			
			if ( listeners == null ){
				
				listeners = new ArrayList(1);
			}
			
			listeners.add(l);
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	removeListener(
		PEPeerListener	l )
	{
		try{
			this_mon.enter();
			
			if ( listeners == null ){
				
				listeners = new ArrayList(1);
			}
			
			listeners.remove(l);
		}finally{
			
			this_mon.exit();
		}
	}
  
	public TransportOwner
	getTransportOwner()
	{
		return( new TransportOwner()
				{
					public TransportDebugger
					getDebugger()
					{
						if ( AEDiagnostics.CHECK_DUMMY_FILE_DATA ){
							
							return( new PEPeerTransportDebugger( PEPeerTransportProtocol.this ));
						}
						
						return( null );
					}
			
				});
	}
  
  public void doKeepAliveCheck() {
    if( last_message_sent_time == 0 )  last_message_sent_time = SystemTime.getCurrentTime(); //don't send if brand new connection
    if( SystemTime.getCurrentTime() - last_message_sent_time > 2*60*1000 ) {  //2min keep-alive timer
      sendKeepAlive();
      last_message_sent_time = SystemTime.getCurrentTime();  //not quite true, but we don't want to queue multiple keep-alives before the first is actually sent
    }
  }
  
}
