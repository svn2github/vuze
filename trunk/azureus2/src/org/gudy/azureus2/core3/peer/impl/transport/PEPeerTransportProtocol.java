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


import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.*;

import org.gudy.azureus2.core3.util.*;

import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.peer.impl.*;
import org.gudy.azureus2.core3.peer.util.*;
import org.gudy.azureus2.core3.config.*;

import com.aelitis.azureus.core.networkmanager.*;
import com.aelitis.azureus.core.peermanager.PeerManager;
import com.aelitis.azureus.core.peermanager.messaging.*;
import com.aelitis.azureus.core.peermanager.messaging.azureus.AZHandshake;
import com.aelitis.azureus.core.peermanager.messaging.azureus.AZMessage;
import com.aelitis.azureus.core.peermanager.messaging.azureus.AZMessageDecoder;
import com.aelitis.azureus.core.peermanager.messaging.azureus.AZMessageEncoder;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.*;
import com.aelitis.azureus.core.peermanager.utils.*;




public class 
PEPeerTransportProtocol
	implements PEPeerTransport
{
 
	private PEPeerControl 	manager;
	private String			peer_source;
	private byte[] peer_id;
	private String ip;
	private String ip_resolved;
	private IPToHostNameResolverRequest	ip_resolver_request;
	
	private int port;
	
	private PEPeerStatsImpl stats;
  private List 		requested 		= new ArrayList();
  private AEMonitor	requested_mon	= new AEMonitor( "PEPeerTransportProtocol:Req" );

  private HashMap data;
  
  private boolean choked_by_other_peer = true;
  private boolean choking_other_peer = true;
  private boolean interested_in_other_peer = false;
  private boolean other_peer_interested_in_me = false;
  private boolean snubbed = false;
  private boolean[] other_peer_has_pieces;
  private boolean seed = false;
  
  private boolean connection_registered = false;
  

  private boolean incoming;
  private volatile boolean closing = false;
  private int current_peer_state;
  
  private NetworkConnection connection;
  private OutgoingBTPieceMessageHandler outgoing_piece_message_handler;
  private OutgoingBTHaveMessageAggregator outgoing_have_message_aggregator;
  
  private boolean identityAdded = false;  //needed so we don't remove id's in closeAll() on duplicate connection attempts
  
  
  private int connection_state = PEPeerTransport.CONNECTION_PENDING;
  
  
  
  //The client name identification	
	private String client = "";
    
	//Number of bad chunks received from this peer
	private int nbBadChunks;
	
	//When superSeeding, number of unique piece announced
	private int uniquePiece;
	
	//Spread time (0 secs , fake default)
	private int spreadTimeHint = 0 * 1000;

	public final static int componentID = 1;
	public final static int evtProtocol = 0;
  public final static int evtLifeCycle = 1;
  public final static int evtErrors = 2;

  private long last_message_sent_time = 0;
  private long last_message_received_time = 0;
  private long last_data_message_received_time = 0;
  
  private long connection_established_time = 0;

  private boolean az_messaging_mode = false;
  private Message[] supported_messages = null;
  
  
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
  
  
  private ArrayList peer_listeners;
  
  
  

  
  
  //INCOMING
  public PEPeerTransportProtocol( PEPeerControl _manager, String _peer_source, NetworkConnection _connection ) {
    manager = _manager;
    peer_source	= _peer_source;
    ip    = _connection.getAddress().getAddress().getHostAddress();
    port  = _connection.getAddress().getPort();
    incoming = true;
    
    init();
    
    connection = _connection;
    
    //"fake" a connect request to register our listener
    connection.connect( new NetworkConnection.ConnectionListener() {
      public void connectStarted() {
        connection_state = PEPeerTransport.CONNECTION_CONNECTING;
      }
      
      public void connectSuccess() {  //will be called immediately
        LGLogger.log(componentID, evtLifeCycle, LGLogger.RECEIVED, "Established incoming connection from " + PEPeerTransportProtocol.this );
        registerForMessageHandling();
        changePeerState( PEPeer.HANDSHAKING );
        sendBTHandshake();
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
  
  
  
  //OUTGOING
  public PEPeerTransportProtocol( PEPeerControl _manager, String _peer_source, String _ip, int _port ) {
    manager = _manager;
    peer_source	= _peer_source;
    ip    = _ip;
    port  = _port;
    incoming = false;
    
    init();
    
    if( port < 0 || port > 65535 ) {
      Debug.out( "given remote port invalid: " + port );
      closeAll( "Given remote port is invalid: " + port, false, false );
    }
    
    connection = NetworkManager.getSingleton().createConnection( new InetSocketAddress( ip, port ), new BTMessageEncoder(), new BTMessageDecoder() );
    
    changePeerState( PEPeer.CONNECTING );
    
    connection.connect( new NetworkConnection.ConnectionListener() {
      public void connectStarted() {
        connection_state = PEPeerTransport.CONNECTION_CONNECTING;
      }
 
      public void connectSuccess() {
        LGLogger.log(componentID, evtLifeCycle, LGLogger.SENT, "Established outgoing connection with " + PEPeerTransportProtocol.this);
        registerForMessageHandling();
        changePeerState( PEPeer.HANDSHAKING );
        sendBTHandshake();
      }
        
      public void connectFailure( Throwable failure_msg ) {
        closeAll( "Failed to establish outgoing connection [" + PEPeerTransportProtocol.this + "] : " + failure_msg.getMessage(), true, false );
      }
        
      public void exceptionThrown( Throwable error ) {
        closeAll( "Connection [" + PEPeerTransportProtocol.this + "] exception : " + error.getMessage(), true, true );
      }
    });
      
    LGLogger.log(componentID, evtLifeCycle, LGLogger.SENT, "Creating outgoing connection to " + PEPeerTransportProtocol.this);
  }
  
  

  
  
  private void init() {
    uniquePiece = -1;
    
    stats = (PEPeerStatsImpl)manager.createPeerStats();
  }
  
  
	public String
	getPeerSource()
	{
		return( peer_source );
	}
  

  /**
   * Private method that will finish fields allocation, once the handshaking is ok.
   * Hopefully, that will save some RAM.
   */
  private void allocateAll() {
  	try{
  		this_mon.enter();
  	
  		if ( closing ){
  			
  			return;
  		}
  		
	    other_peer_has_pieces = new boolean[ manager.getPiecesNumber() ];
	  	Arrays.fill( other_peer_has_pieces, false );
	
	    //link in outgoing piece handler
	    outgoing_piece_message_handler = new OutgoingBTPieceMessageHandler( manager.getDiskManager(), connection.getOutgoingMessageQueue() );
	    
	    //link in outgoing have message aggregator
	    outgoing_have_message_aggregator = new OutgoingBTHaveMessageAggregator( connection.getOutgoingMessageQueue() );
	    
	    //register bytes sent listener
	    connection.getOutgoingMessageQueue().registerQueueListener( new OutgoingMessageQueue.MessageQueueListener() {
	      public boolean messageAdded( Message message ) {  return true;  }
        public void messageQueued( Message message ) { /*ignore*/ }
	      public void messageRemoved( Message message ) { /*ignore*/ }
	      
	      public void messageSent( Message message ) {
	        //update keep-alive info
	        last_message_sent_time = SystemTime.getCurrentTime();
          LGLogger.log( LGLogger.CORE_NETWORK, "Sent " +message.getDescription()+ " message to " + connection );
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
	    PeerManager.getSingleton().getUploadManager().registerStandardPeerConnection( connection, manager.getUploadLimitedRateGroup() );
	    
	    connection_registered = true;
	    
  	}finally{
	
  		this_mon.exit();
  	}
  }

   
  
  public void 
  closeAll(
  		String 		reason, 
		boolean 	closedOnError, 
		boolean 	attemptReconnect ) 
  {
  		try{
  			this_mon.enter();
  		
  			if (closing){
  				
  				return;
  			}
  			
  			closing = true;
  			
  		}finally{
  			
  			this_mon.exit();
  		}
  		
      
      changePeerState( PEPeer.CLOSING );
      
      connection.getIncomingMessageQueue().stopQueueProcessing();

      LGLogger.log( componentID, evtProtocol, closedOnError?LGLogger.ERROR:LGLogger.INFORMATION, reason);
      
	  	//Cancel any pending requests (on the manager side)
	  	cancelRequests();
	  	  		    
	    if( outgoing_piece_message_handler != null ) {
	      outgoing_piece_message_handler.removeAllPieceRequests();
	      outgoing_piece_message_handler.destroy();
	      //outgoing_piece_message_handler = null;
	    }
	    
	    if( outgoing_have_message_aggregator != null ) {
	      outgoing_have_message_aggregator.destroy();
	      //outgoing_have_message_aggregator = null;
	    }
	    	    
	    
	    if( connection_registered ) {
	      PeerManager.getSingleton().getUploadManager().cancelStandardPeerConnection( connection );
	    }
      
	    connection.close();

	    recent_outgoing_requests.clear();
   
	    if ( ip_resolver_request != null ){
	    	ip_resolver_request.cancel();
	    }
	    
	    
	  	//remove identity
	  	if ( this.peer_id != null && identityAdded ) {
	  		PeerIdentityManager.removeIdentity( manager.getPeerIdentityDataID(), this.peer_id );
	  	}
	
	  		//	Send a logger event
	  	
	  	LGLogger.log(componentID, evtLifeCycle, LGLogger.INFORMATION, "Connection Ended with " + toString());
	  	
	  	if( attemptReconnect && !incoming ) {      
	  		LGLogger.log(componentID, evtLifeCycle, LGLogger.INFORMATION, "Attempting to reconnect with " + toString());
	  		manager.peerConnectionClosed( this, true );
	  		
	  	}else{
	  		
	      manager.peerConnectionClosed( this, false );
	  	}
  }

	

  
  
  
  private void sendBTHandshake() {
    connection_established_time = SystemTime.getCurrentTime();
    connection_state = PEPeerTransport.CONNECTION_WAITING_FOR_HANDSHAKE;
    allocateAll();
    connection.getOutgoingMessageQueue().addMessage( new BTHandshake( manager.getHash(), manager.getPeerId() ), false );
  }
  
  
  
  private void sendAZHandshake() {
    Message[] avail_msgs = MessageManager.getSingleton().getRegisteredMessages();
    String[] avail_ids = new String[ avail_msgs.length ];
    byte[] avail_vers = new byte[ avail_msgs.length ];
    
    for( int i=0; i < avail_msgs.length; i++ ) {
      avail_ids[i] = avail_msgs[i].getID();
      avail_vers[i] = avail_msgs[i].getVersion();
    }
    
    AZHandshake az_handshake = new AZHandshake(
        AZPeerIdentityManager.getAZPeerIdentity(),
        Constants.AZUREUS_NAME,
        Constants.AZUREUS_VERSION,
        avail_ids,
        avail_vers );        

    //System.out.println( "Sending " +az_handshake.getDescription() );
    
    connection.getOutgoingMessageQueue().addMessage( az_handshake, false );
  }
  

  
  
  
  
  
  

  public int getPeerState() {  return current_peer_state;  }

  
  
  public int getPercentDoneInThousandNotation() {
    if( other_peer_has_pieces == null ) {  return 0;  }
    
    int sum = 0;
    for( int i=0; i < other_peer_has_pieces.length; i++ ) {
      if( other_peer_has_pieces[i] ) {
        sum++;
      }
    }

    sum = (sum * 1000) / other_peer_has_pieces.length;
    return sum;
  }
  

  public boolean transferAvailable() {
    return (!choked_by_other_peer && interested_in_other_peer);
  }

    
  
  private void printRequestStats() {
    if( SHOW_DISCARD_RATE_STATS ) {
      float discard_percentage = (requests_discarded * 100F) / ((requests_completed + requests_recovered + requests_discarded) * 1F);
      float recover_percentage = (requests_recovered * 100F) / ((requests_recovered + requests_discarded) * 1F);
      System.out.println( "c="+requests_completed+ " d="+requests_discarded+ " r="+requests_recovered+ " dp="+discard_percentage+ "% rp="+recover_percentage+ "%" );
    }
  }
  

  

  /**
	 * Checks if it's a seed or not.
	 */
  private void checkSeed() {
    for (int i = 0; i < other_peer_has_pieces.length; i++) {
      if (!other_peer_has_pieces[i]) {
        return;
      }
    }
    seed = true;
  }


  public boolean request( int pieceNumber, int pieceOffset, int pieceLength) {
  	if (getPeerState() != TRANSFERING) {
  		manager.requestCanceled( manager.createDiskManagerRequest( pieceNumber, pieceOffset, pieceLength ) );
  		return false;
  	}	
  	DiskManagerReadRequest request = manager.createDiskManagerRequest( pieceNumber, pieceOffset, pieceLength );
  	if( !hasBeenRequested( request ) ) {
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
  	if ( getPeerState() != TRANSFERING ) return;
		if ( hasBeenRequested( request ) ) {
			removeRequest( request );
      connection.getOutgoingMessageQueue().addMessage( new BTCancel( request.getPieceNumber(), request.getOffset(), request.getLength() ), false );
		}
  }

  
  public void sendHave( int pieceNumber ) {
		if ( getPeerState() != TRANSFERING ) return;
    //only force if the other peer doesn't have this piece and is not yet interested
    boolean force = !other_peer_has_pieces[ pieceNumber ] && !other_peer_interested_in_me;
    outgoing_have_message_aggregator.queueHaveMessage( pieceNumber, force );
		checkInterested();
	}

  
  public void sendChoke() {
  	if ( getPeerState() != TRANSFERING ) return;
    outgoing_piece_message_handler.removeAllPieceRequests();
    connection.getOutgoingMessageQueue().addMessage( new BTChoke(), false );
    choking_other_peer = true;
  }

  
  public void sendUnChoke() {
    if ( getPeerState() != TRANSFERING ) return;
    connection.getOutgoingMessageQueue().addMessage( new BTUnchoke(), false );
    choking_other_peer = false;
  }


  private void sendKeepAlive() {
    if ( getPeerState() != TRANSFERING ) return;
    
    if( outgoing_have_message_aggregator.hasPending() ) {
      outgoing_have_message_aggregator.forceSendOfPending();
    }
    else {
      connection.getOutgoingMessageQueue().addMessage( new BTKeepAlive(), false );
    }
  }
  
  
  

  
  /**
   * Global checkInterested method.
   * Scans the whole pieces to determine if it's interested or not
   */
  private void checkInterested() {
    if ( getPeerState() == CLOSING ) return;
    
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
    if ( getPeerState() == CLOSING ) return;
    
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
   */
  private void sendBitField() {
    if ( getPeerState() == CLOSING ) return;
    
		//In case we're in super seed mode, we don't send our bitfield
		if ( manager.isSuperSeedMode() ) return;
    
    //create bitfield
		ByteBuffer buffer = ByteBuffer.allocate( (manager.getPiecesNumber() + 7) / 8 );
		
		DiskManagerPiece[]	pieces = manager.getDiskManager().getPieces();
		
		int bToSend = 0;
		int i = 0;
		for (; i < pieces.length; i++) {
			if ( (i % 8) == 0 ) bToSend = 0;
			bToSend = bToSend << 1;
			if ( pieces[i].getDone()) {
				bToSend += 1;
			}
			if ( (i % 8) == 7 ) buffer.put( (byte)bToSend );
		}
		if ( (i % 8) != 0 ) {
			bToSend = bToSend << (8 - (i % 8));
			buffer.put( (byte)bToSend );
		}

    buffer.flip();
    
    connection.getOutgoingMessageQueue().addMessage( new BTBitfield( new DirectByteBuffer( buffer ) ), false );
	}

  
  public byte[] getId() {  return peer_id;  }
  public String getIp() {  return ip;  }
  public int getPort() {  return port;  }
  public String getClient() {  return client;  }
  
  public boolean isIncoming() {  return incoming;  }  
  public boolean isOptimisticUnchoke() {  return manager.isOptimisticUnchoke(this);  }
  
  public PEPeerControl getControl() {  return manager;  }
  public PEPeerManager getManager() {  return manager;  }
  public PEPeerStats getStats() {  return stats;  }
  
  public boolean[] getAvailable() {  return other_peer_has_pieces;  }
  
  public boolean isChokingMe() {  return choked_by_other_peer;  }
  public boolean isChokedByMe() {  return choking_other_peer;  }
  public boolean isInterestingToMe() {  return interested_in_other_peer;  }
  public boolean isInterestedInMe() {  return other_peer_interested_in_me;  }
  public boolean isSeed() {  return seed;  }
  public boolean isSnubbed() {  return snubbed;  }
  public void setSnubbed(boolean b) {  snubbed = b;  }


  public void hasSentABadChunk() {  nbBadChunks++;  }
  public int getNbBadChunks() {  return nbBadChunks;  }
  public void resetNbBadChunks(){ nbBadChunks = 0; }
  
  public void setUploadHint(int spreadTime) {  spreadTimeHint = spreadTime;  }
  public int getUploadHint() {  return spreadTimeHint;  }
  public void setUniqueAnnounce(int _uniquePiece) {  uniquePiece = _uniquePiece;  }
  public int getUniqueAnnounce() {  return uniquePiece;  }



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
		
			ip_resolver_request = IPToHostNameResolver.addResolverRequest( 
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
	


	private void cancelRequests() {
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
	    Message[] type = { new BTRequest() };
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
			
				// this is frequently called, hence we operate without a monitor and
				// take the hit of possible exceptions due to concurrent list
				// modification (only out-of-bounds can occur)
				
			try{
		    	for (int i = 0; i < requested.size(); i++){
		    		
	    			DiskManagerReadRequest request = (DiskManagerReadRequest) requested.get(i);
		    			
	    			if (request.isExpired()){
	    				
	    				if ( result == null ){
	    					
	    					result = new ArrayList();
	    				}
	    				
	    				result.add(request);
	    			}
		    	}
		    	
		    	return( result );
		    	
		    }catch(Throwable e ){
		    	
		    	return( null );
		    }
		}
		
    
		private boolean	hasBeenRequested( DiskManagerReadRequest request ) {
			try{  requested_mon.enter();
	    
				return requested.contains( request );
      }
      finally{  requested_mon.exit();  }
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
	
  
  
  
  public void doKeepAliveCheck() {
    long wait_time = SystemTime.getCurrentTime() - last_message_sent_time;
    
    if( last_message_sent_time == 0 || wait_time < 0 ) {
      last_message_sent_time = SystemTime.getCurrentTime(); //don't send if brand new connection
      return;
    }
    
    if( wait_time > 2*60*1000 ) {  //2min keep-alive timer
      sendKeepAlive();
      last_message_sent_time = SystemTime.getCurrentTime();  //not quite true, but we don't want to queue multiple keep-alives before the first is actually sent
    }
  }

  
  public boolean doTimeoutChecks() {
    //Timeouts when in states PEPeerTransport.CONNECTION_PENDING and
    //PEPeerTransport.CONNECTION_CONNECTING are handled by the ConnectDisconnectManager
    //so we don't need to deal with them here.
    
    //make sure we time out stalled connections
    if( connection_state == PEPeerTransport.CONNECTION_FULLY_ESTABLISHED ) {
      long dead_time = SystemTime.getCurrentTime() - last_message_received_time;
      
      if( dead_time < 0 ) {  //oops, system clock went backwards
        last_message_received_time = SystemTime.getCurrentTime();
        return false;
      }
      
      if( dead_time > 5*60*1000 ) { //5min timeout
        closeAll( toString() + ": Timed out while waiting for messages", true, true );
        return true;
      }
    }
    //ensure we dont get stuck in the handshaking phases
    else if( connection_state == PEPeerTransport.CONNECTION_WAITING_FOR_HANDSHAKE ||
             connection_state == PEPeerTransport.CONNECTION_WAITING_FOR_BITFIELD ) {
      
      long wait_time = SystemTime.getCurrentTime() - connection_established_time;
      
      if( wait_time < 0 ) {  //oops, system clock went backwards
        connection_established_time = SystemTime.getCurrentTime();
        return false;
      }
      
      if( wait_time > 3*60*1000 ) { //3min timeout
        String phase = connection_state == PEPeerTransport.CONNECTION_WAITING_FOR_HANDSHAKE ? "handshaking" : "bitfield";
        closeAll( toString() + ": Timed out while waiting in " +phase+ " phase", true, true );
        return true;
      }
    }
    
    return false;
  }
  
  
  public int getConnectionState() {  return connection_state;  }
  
  
  public long getTimeSinceLastDataMessageReceived() {
    if( last_data_message_received_time == 0 ) {  //fudge it while we're still handshaking
      return 0;
    }
    
    long time = SystemTime.getCurrentTime() - last_data_message_received_time;
    
    if( time < 0 ) {  //time went backwards
      last_data_message_received_time = SystemTime.getCurrentTime();
      time = 0;
    }
    
    return time;
  }
  
  
  public long getTimeSinceConnectionEstablished() {
    if( connection_established_time == 0 ) {  //fudge it while the transport is being connected
      return 0;
    }
    
    long time = SystemTime.getCurrentTime() - connection_established_time;
    
    if( time < 0 ) {  //time went backwards
      connection_established_time = SystemTime.getCurrentTime();
      time = 0;
    }
    return time;
  }
  
  
  
  private void decodeBTHandshake( BTHandshake handshake ) {
    PeerIdentityDataID  my_peer_data_id = manager.getPeerIdentityDataID();
      
    if( !Arrays.equals( manager.getHash(), handshake.getDataHash() ) ) {
      closeAll( toString() + " has sent handshake, but infohash is wrong", true, false );
      handshake.destroy();
      return;
    }
    
    peer_id = handshake.getPeerId();

    //decode a client identification string from the given peerID
    client = PeerClassifier.getClientDescription( peer_id );

    //make sure the client type is not banned
    if( !PeerClassifier.isClientTypeAllowed( client ) ) {
      closeAll( toString() + ": " +client+ " client type not allowed to connect, banned", false, false );
      handshake.destroy();
      return;
    }

    //make sure we are not connected to ourselves
    if( Arrays.equals( manager.getPeerId(), peer_id ) ) {
      closeAll( toString() + ": peerID matches myself", false, false );
      handshake.destroy();
      return;
    }

    //make sure we are not already connected to this peer
    boolean sameIdentity = PeerIdentityManager.containsIdentity( my_peer_data_id, peer_id );
    boolean sameIP = false;
      
      
    //allow loopback connects for co-located proxy-based connections and testing
    boolean same_allowed = COConfigurationManager.getBooleanParameter( "Allow Same IP Peers" ) || ip.equals( "127.0.0.1" );
    if( !same_allowed ){  
      if( PeerIdentityManager.containsIPAddress( my_peer_data_id, ip )) {
        sameIP = true;
      }
    }
      
    if( sameIdentity ) {
      closeAll( toString() + " exchanged handshake, but peer matches pre-existing identity", false, false );
      handshake.destroy();
      return;
    }
    
    if( sameIP ) {
      closeAll( toString() + " exchanged handshake, but peer matches pre-existing IP address", false, false );
      handshake.destroy();
      return;
    }

    //make sure we haven't reached our connection limit
    int maxAllowed = PeerUtils.numNewConnectionsAllowed( my_peer_data_id );
    if( maxAllowed == 0 ) {
      closeAll( toString() + ": Too many existing peer connections [" +PeerIdentityManager.getIdentityCount( my_peer_data_id )+ "]", false, false );
      handshake.destroy();
      return;
    }

    PeerIdentityManager.addIdentity( my_peer_data_id, peer_id, ip );
    identityAdded = true;

    LGLogger.log( componentID, evtLifeCycle, LGLogger.RECEIVED, toString() + " has sent their handshake" );

    handshake.destroy();
    
    
    //extended protocol processing
    if( (handshake.getReserved()[0] & 128) == 128 ) {  //if first (high) bit is set
      if( client.indexOf( "Azureus" ) != -1 ) {  //for now, filter out non-az clients, as ABC seems to set our reserved flag
        az_messaging_mode = true;
        connection.getIncomingMessageQueue().setDecoder( new AZMessageDecoder() );
        connection.getOutgoingMessageQueue().setEncoder( new AZMessageEncoder() );
        
        sendAZHandshake();
      }
      else {
        System.out.println( "Peer " +ip+ " [" +client+ "] handshake mistakingly indicates extended AZ messaging support." );
      }
    }

    /*
    for( int i=0; i < reserved.length; i++ ) {
      int val = reserved[i] & 0xFF;
      if( val != 0 ) {
        System.out.println( "Peer "+ip+" ["+client+"] sent reserved byte #"+i+" to " +val);
      }
    }
    */

    if( !az_messaging_mode ) {  //otherwise we'll do this after receiving az handshake
      //fudge to ensure optimistic-connect code processes connections that have never sent a data message
      last_data_message_received_time = SystemTime.getCurrentTime();
       
      changePeerState( PEPeer.TRANSFERING );
      
      connection_state = PEPeerTransport.CONNECTION_WAITING_FOR_BITFIELD;
      sendBitField(); 
    }
    
  }
  
  
  
  private void decodeAZHandshake( AZHandshake handshake ) {
    
    client = handshake.getClient()+ " " +handshake.getClientVersion();
    
    
    //find mutually available message types
    ArrayList messages = new ArrayList();
    
    String mutual = "";
    
    for( int i=0; i < handshake.getMessageIDs().length; i++ ) {
      Message msg = MessageManager.getSingleton().lookupMessage( handshake.getMessageIDs()[i], handshake.getMessageVersions()[i] );
      
      if( msg != null ) {  //mutual support!
        messages.add( msg );
        
        String id = msg.getID();
        
        if( !id.equals( BTMessage.ID_BT_BITFIELD ) &&   //filter out obvious mutual messages
            !id.equals( BTMessage.ID_BT_CANCEL ) &&
            !id.equals( BTMessage.ID_BT_CHOKE ) &&
            !id.equals( BTMessage.ID_BT_HANDSHAKE ) &&
            !id.equals( BTMessage.ID_BT_HAVE ) &&
            !id.equals( BTMessage.ID_BT_INTERESTED ) &&
            !id.equals( BTMessage.ID_BT_KEEP_ALIVE ) &&
            !id.equals( BTMessage.ID_BT_PIECE ) &&
            !id.equals( BTMessage.ID_BT_REQUEST ) &&
            !id.equals( BTMessage.ID_BT_UNCHOKE ) &&
            !id.equals( BTMessage.ID_BT_UNINTERESTED ) )
        {
          mutual += "[" +id+ "] ";
        }
      }
    }
    
    supported_messages = (Message[])messages.toArray( new Message[0] );

    System.out.println( "[" +(incoming ? "R:" : "L:")+" " +ip+":"+port+" "+client+ "] Mutually supported messages: " +mutual );

    //fudge to ensure optimistic-connect code processes connections that have never sent a data message
    last_data_message_received_time = SystemTime.getCurrentTime();
     
    changePeerState( PEPeer.TRANSFERING );
    
    connection_state = PEPeerTransport.CONNECTION_WAITING_FOR_BITFIELD;
    sendBitField();
  }
  
  
  
  
  private void decodeBitfield( BTBitfield bitfield ) {
    DirectByteBuffer field = bitfield.getBitfield();
   
    byte[] dataf = new byte[ (manager.getPiecesNumber() + 7) / 8 ];
         
    if( field.remaining( DirectByteBuffer.SS_PEER ) < dataf.length ) {
      Debug.out( toString() + " has sent invalid Bitfield: too short [" +field.remaining( DirectByteBuffer.SS_PEER )+ "<" +dataf.length+ "]" );
      LGLogger.log( componentID, evtProtocol, LGLogger.ERROR, toString() + " has sent invalid Bitfield: too short [" +field.remaining( DirectByteBuffer.SS_PEER )+ "<" +dataf.length+ "]" );
      bitfield.destroy();
      return;
    }
       
    field.get( DirectByteBuffer.SS_PEER, dataf );
    
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

    checkInterested();
    checkSeed();
    bitfield.destroy();
  }
  
  
  
  private void decodeChoke( BTChoke choke ) {    
    choked_by_other_peer = true;
    cancelRequests();
    choke.destroy();
  }
  
  
  private void decodeUnchoke( BTUnchoke unchoke ) {
    choked_by_other_peer = false;
    unchoke.destroy();
  }
  
  
  private void decodeInterested( BTInterested interested ) {
    other_peer_interested_in_me = true;
    interested.destroy();                                                   
  }
  
  
  private void decodeUninterested( BTUninterested uninterested ) {
    other_peer_interested_in_me = false;

    //force send any pending haves in case one of them would make the other peer interested again
    if( outgoing_have_message_aggregator != null ) {
      outgoing_have_message_aggregator.forceSendOfPending();
    }

    uninterested.destroy();
  }
  
  
  
  
  private void decodeHave( BTHave have ) {
    int piece_number = have.getPieceNumber();
    
    if ((piece_number >= other_peer_has_pieces.length) || (piece_number < 0)) {
      closeAll( toString() + " gave invalid piece_number: " + piece_number, true, true );
      have.destroy();
      return;
    }

    other_peer_has_pieces[piece_number] = true;
    int pieceLength = manager.getPieceLength(piece_number);
    stats.haveNewPiece(pieceLength);
    manager.havePiece(piece_number, pieceLength, this);
    
    if (!interested_in_other_peer) {
      checkInterested(piece_number);
    }
    
    checkSeed();
     
    have.destroy();
  }
  
  
  
  private void decodeRequest( BTRequest request ) {
    int number = request.getPieceNumber();
    int offset = request.getPieceOffset();
    int length = request.getLength();
    
    if( !manager.checkBlock( number, offset, length ) ) {
      closeAll( toString() + " has requested #" + number + ":" + offset + "->" + (offset + length) + " which is an invalid request.", true, true );
      request.destroy();
      return;
    }
      
    if( !choking_other_peer ) {
      outgoing_piece_message_handler.addPieceRequest( number, offset, length );
    }
    else {
      LGLogger.log( componentID, evtProtocol, LGLogger.RECEIVED, toString() + " has requested #" + number + ":" + offset + "->" + (offset + length) + " but peer is currently choked. Request ignored." );
    }

    request.destroy();  
  }
  
  
  
  private void decodePiece( BTPiece piece ) {
    last_data_message_received_time = SystemTime.getCurrentTime();
    
    int number = piece.getPieceNumber();
    int offset = piece.getPieceOffset();
    DirectByteBuffer payload = piece.getPieceData();
    int length = payload.remaining( DirectByteBuffer.SS_PEER );
    
    /*
    if ( AEDiagnostics.CHECK_DUMMY_FILE_DATA ){
      int pos = payload.position( DirectByteBuffer.SS_PEER );
      long  off = ((long)number) * getControl().getPieceLength(0) + offset;
      for (int i=0;i<length;i++){
        byte  v = payload.get( DirectByteBuffer.SS_PEER );
        if ((byte)off != v ){      
          System.out.println( "piece: read is bad at " + off + ": expected = " + (byte)off + ", actual = " + v );
          break;
        }
        off++;           
      }
      payload.position( DirectByteBuffer.SS_PEER, pos );
    }
    */
    
    String error_msg = toString() + " has sent #" + number + ":" + offset + "->" + (offset + length) + ", ";
    
    if( !manager.checkBlock( number, offset, payload ) ) {
      LGLogger.log( componentID, evtProtocol, LGLogger.RECEIVED, error_msg + "but piece block discarded as invalid." );
      stats.discarded( length );
      manager.discarded( length );
      requests_discarded++;
      printRequestStats();
      piece.destroy();
      return;
    }
    
    DiskManagerReadRequest request = manager.createDiskManagerRequest( number, offset, length );
    boolean piece_error = true;

    if( hasBeenRequested( request ) ) {  //from active request
      removeRequest( request );
      setSnubbed( false );
      reSetRequestsTime();
        
      if( manager.isBlockAlreadyWritten( number, offset ) ) {  //oops, looks like this block has already been downloaded
        //we're probably in end-game mode then
        if( manager.isInEndGameMode() ) {
          LGLogger.log( componentID, evtProtocol, LGLogger.RECEIVED, error_msg + "but piece block ignored as already written in end-game mode." );
          //we dont count end-game mode duplicates towards discarded, since this is a normal side-effect of the mode
        }
        else {
          LGLogger.log( componentID, evtProtocol, LGLogger.RECEIVED, error_msg + "but piece block discarded as already written." );
          stats.discarded( length );
          manager.discarded( length );
          requests_discarded++;
          printRequestStats();
        }
      }
      else {  //successfully received piece!
        manager.received( length );
        manager.writeBlock( number, offset, payload, this );
        requests_completed++;
        piece_error = false;  //dont destroy message, as we've passed the payload on to the disk manager for writing
      }
    }
    else {  //initial request may have already expired, but check if we can use the data anyway
      if( !manager.isBlockAlreadyWritten( number, offset ) ) {
        boolean ever_requested;
        
        try{  recent_outgoing_requests_mon.enter();
          ever_requested = recent_outgoing_requests.containsKey( request );
        }
        finally{  recent_outgoing_requests_mon.exit();  }
        
        if( ever_requested ) { //security-measure: we dont want to be accepting any ol' random block
          LGLogger.log( componentID, evtProtocol, LGLogger.RECEIVED, error_msg + "expired piece block data recovered as useful." );
          manager.received( length );
          setSnubbed( false );
          reSetRequestsTime();
          manager.writeBlockAndCancelOutstanding( number, offset, payload, this );
          requests_recovered++;
          printRequestStats();
          piece_error = false;  //dont destroy message, as we've passed the payload on to the disk manager for writing
        }
        else {
          LGLogger.log( componentID, evtProtocol, LGLogger.RECEIVED, error_msg + "but expired piece block discarded as never requested." );
          stats.discarded( length );
          manager.discarded( length );
          requests_discarded++;
          printRequestStats();
        }
      }
      else {
        LGLogger.log( componentID, evtProtocol, LGLogger.RECEIVED, error_msg + "but expired piece block discarded as already written." );
        stats.discarded( length );
        manager.discarded( length );
        requests_discarded++;
        printRequestStats();
      }
    }
    
    if( piece_error )  piece.destroy();
  }
  
  
  
  private void decodeCancel( BTCancel cancel ) {
    int number = cancel.getPieceNumber();
    int offset = cancel.getPieceOffset();
    int length = cancel.getLength();
    
    outgoing_piece_message_handler.removePieceRequest( number, offset, length );
    cancel.destroy();
  }
  
  
  
  private void registerForMessageHandling() {
    connection.getIncomingMessageQueue().registerQueueListener( new IncomingMessageQueue.MessageQueueListener() {
      public boolean messageReceived( Message message ) {      
        LGLogger.log( componentID, evtProtocol, LGLogger.RECEIVED, "Received message [" +message.getDescription()+ "] from " +PEPeerTransportProtocol.this );
        
        last_message_received_time = SystemTime.getCurrentTime();
        
        if( message.getID().equals( BTMessage.ID_BT_HANDSHAKE ) && message.getVersion() == BTMessage.BT_DEFAULT_VERSION ) {
          decodeBTHandshake( (BTHandshake)message );
          return true;
        }
        
        if( message.getID().equals( AZMessage.ID_AZ_HANDSHAKE ) && message.getVersion() == AZMessage.AZ_DEFAULT_VERSION ) {
          decodeAZHandshake( (AZHandshake)message );
          return true;
        }
        
        if( message.getID().equals( BTMessage.ID_BT_BITFIELD ) && message.getVersion() == BTMessage.BT_DEFAULT_VERSION ) {
          decodeBitfield( (BTBitfield)message );
          return true;
        }
        
        connection_state = PEPeerTransport.CONNECTION_FULLY_ESTABLISHED;
          
        if( message.getID().equals( BTMessage.ID_BT_CHOKE ) && message.getVersion() == BTMessage.BT_DEFAULT_VERSION ) {
          decodeChoke( (BTChoke)message );
          return true;
        }
        
        if( message.getID().equals( BTMessage.ID_BT_UNCHOKE ) && message.getVersion() == BTMessage.BT_DEFAULT_VERSION ) {
          decodeUnchoke( (BTUnchoke)message );
          return true;
        }
        
        if( message.getID().equals( BTMessage.ID_BT_INTERESTED ) && message.getVersion() == BTMessage.BT_DEFAULT_VERSION ) {
          decodeInterested( (BTInterested)message );
          return true;
        }
        
        if( message.getID().equals( BTMessage.ID_BT_UNINTERESTED ) && message.getVersion() == BTMessage.BT_DEFAULT_VERSION ) {
          decodeUninterested( (BTUninterested)message );
          return true;
        }
        
        if( message.getID().equals( BTMessage.ID_BT_HAVE ) && message.getVersion() == BTMessage.BT_DEFAULT_VERSION ) {
          decodeHave( (BTHave)message );
          return true;
        }
        
        if( message.getID().equals( BTMessage.ID_BT_REQUEST ) && message.getVersion() == BTMessage.BT_DEFAULT_VERSION ) {
          decodeRequest( (BTRequest)message );
          return true;
        }
        
        if( message.getID().equals( BTMessage.ID_BT_PIECE ) && message.getVersion() == BTMessage.BT_DEFAULT_VERSION ) {
          decodePiece( (BTPiece)message );
          return true;
        }
        
        if( message.getID().equals( BTMessage.ID_BT_CANCEL ) && message.getVersion() == BTMessage.BT_DEFAULT_VERSION ) {
          decodeCancel( (BTCancel)message );
          return true;
        }
        
        if( message.getID().equals( BTMessage.ID_BT_KEEP_ALIVE ) && message.getVersion() == BTMessage.BT_DEFAULT_VERSION ) {
          //do nothing
          message.destroy();
          return true;
        }       
        
        //String reason = "Received unknown message: " +message.getID()+ ":" +message.getVersion();
        //Debug.out( reason );
        
        return false;
      }
      
      public void protocolBytesReceived( int byte_count ) {
        //update stats
        stats.protocol_recevied( byte_count );
        manager.protocol_received( byte_count );
      }
      
      public void dataBytesReceived( int byte_count ) {
        //update stats
        stats.received( byte_count );
        //we dont call manager.received(byte_count) here, as piece message handler will do it if piece is ok
      }
    });
    
    connection.getIncomingMessageQueue().startQueueProcessing();
  }
  
  
  
  public NetworkConnection getConnection() {
    return connection;
  }
  
  
  public Message[] getSupportedMessages() {
    return supported_messages;
  }
  
  
  public boolean supportsMessaging() {
    return supported_messages != null;
  }
  
  
  
  public void addListener( PEPeerListener listener ) {
    if( peer_listeners == null )  peer_listeners = new ArrayList();
    peer_listeners.add( listener );
  }
  
  public void removeListener( PEPeerListener listener ) {
    peer_listeners.remove( listener );
    if( peer_listeners.isEmpty() )  peer_listeners = null;
  }
  
  
  private void changePeerState( int new_state ) {
    current_peer_state = new_state;
    
    if( peer_listeners != null ) {
      for( int i=0; i < peer_listeners.size(); i++ ) {
        PEPeerListener l = (PEPeerListener)peer_listeners.get( i );
      
        l.stateChanged( current_peer_state );
      }
    }
  }
  
}
