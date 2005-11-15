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
import java.util.*;

import org.gudy.azureus2.core3.util.*;

import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.peer.impl.*;
import org.gudy.azureus2.core3.peer.util.*;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.plugins.PluginInterface;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.networkmanager.*;
import com.aelitis.azureus.core.peermanager.messaging.*;
import com.aelitis.azureus.core.peermanager.messaging.azureus.*;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.*;
import com.aelitis.azureus.core.peermanager.peerdb.*;
import com.aelitis.azureus.core.peermanager.utils.*;
import com.aelitis.azureus.plugins.dht.DHTPlugin;




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
  
  private PeerItem peer_item_identity;
  private int tcp_listen_port = 0;
  private int udp_listen_port = 0;
	
	private final PEPeerStats peer_stats;
  
  private final ArrayList requested = new ArrayList();
  private final AEMonitor	requested_mon = new AEMonitor( "PEPeerTransportProtocol:Req" );

  private HashMap data;
  
  private boolean choked_by_other_peer = true;
  private boolean choking_other_peer = true;
  private boolean interested_in_other_peer = false;
  private boolean other_peer_interested_in_me = false;
  private boolean snubbed = false;
  private boolean[] other_peer_has_pieces;
  private boolean seed = false;
 
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
    	
	//When superSeeding, number of unique piece announced
	private int uniquePiece = -1;
  
  //When downloading a piece in exclusivity mode the piece number being downloaded
  private int reservedPiece = -1;
	
	//Spread time (0 secs , fake default)
	private int spreadTimeHint = 0 * 1000;

	public final static int componentID = 1;
	public final static int evtProtocol = 0;
  public final static int evtLifeCycle = 1;
  public final static int evtErrors = 2;

  private long last_message_sent_time = 0;
  private long last_message_received_time = 0;
  private long last_data_message_received_time = -1;
  private long last_data_message_sent_time = -1;
  
  private long connection_established_time = 0;

  private boolean az_messaging_mode = false;
  private Message[] supported_messages = null;
  
  
  private final AEMonitor	closing_mon	= new AEMonitor( "PEPeerTransportProtocol:closing" );
  private final AEMonitor data_mon  = new AEMonitor( "PEPeerTransportProtocol:data" );
  

  private LinkedHashMap recent_outgoing_requests;
  private AEMonitor	recent_outgoing_requests_mon;
  
  private static final boolean SHOW_DISCARD_RATE_STATS;
  static {
  	String	prop = System.getProperty( "show.discard.rate.stats" );
  	SHOW_DISCARD_RATE_STATS = prop != null && prop.equals( "1" );
  }
  
  private static int requests_discarded = 0;
  private static int requests_discarded_endgame = 0;
  private static int requests_recovered = 0;
  private static int requests_completed = 0;

  private List peer_listeners_cow;
  private final AEMonitor	peer_listeners_mon = new AEMonitor( "PEPeerTransportProtocol:PL" );

  
  //certain Optimum Online networks block peer seeding via "complete" bitfield message filtering
  //lazy mode makes sure we never send a complete (seed) bitfield
  private static final boolean ENABLE_LAZY_BITFIELD;
  static {
    String  prop = System.getProperty( "azureus.lazy.bitfield" );
    ENABLE_LAZY_BITFIELD = prop != null && prop.equals( "1" );
  }
  
  
  private boolean is_optimistic_unchoke = false;

  private PeerExchangerItem peer_exchange_item = null;
  private boolean peer_exchange_supported = false;
    
  private PeerMessageLimiter message_limiter;
  
  
  
  
  
  //INCOMING
  public PEPeerTransportProtocol( PEPeerControl _manager, String _peer_source, NetworkConnection _connection ) {
    manager = _manager;
    peer_source	= _peer_source;
    ip    = _connection.getAddress().getAddress().getHostAddress();
    port  = _connection.getAddress().getPort();
    
    peer_item_identity = PeerItemFactory.createPeerItem( ip, port, PeerItem.convertSourceID( _peer_source ) );
    
    incoming = true;
    connection = _connection;
    
    peer_stats = manager.createPeerStats();

    changePeerState( PEPeer.CONNECTING );
    
    //"fake" a connect request to register our listener
    connection.connect( new NetworkConnection.ConnectionListener() {
      public void connectStarted() {
        connection_state = PEPeerTransport.CONNECTION_CONNECTING;
      }
      
      public void connectSuccess() {  //will be called immediately
        if( LGLogger.isEnabled() )  LGLogger.log(componentID, evtLifeCycle, LGLogger.RECEIVED, "Established incoming connection from " + PEPeerTransportProtocol.this );
        initializeConnection();
        
        /*
         * Waiting until we've received the initiating-end's full handshake, before sending back our own,
         * really should be the "proper" behavior.  However, classic BT trackers running NAT checking will
         * only send the first 48 bytes (up to infohash) of the peer handshake, skipping peerid, which means
         * we'll never get their complete handshake, and thus never reply, which causes the NAT check to fail.
         * So, we need to send our handshake earlier, after we've verified the infohash.
         * NOTE:
         * This code makes the assumption that the inbound infohash has already been validated,
         * as we don't check their handshake fully before sending our own.
         */
        sendBTHandshake();
      }
      
      public void connectFailure( Throwable failure_msg ) {  //should never happen
        Debug.out( "ERROR: incoming connect failure: ", failure_msg );
        closeConnectionInternally( "ERROR: incoming connect failure [" + PEPeerTransportProtocol.this + "] : " + failure_msg.getMessage() );
      }
      
      public void exceptionThrown( Throwable error ) {
        if( error.getMessage() == null ) {
          Debug.out( error );
        }
        
        closeConnectionInternally( "connection exception: " + error.getMessage() );
      }
    });
  }
  
  
  
  //OUTGOING
  public PEPeerTransportProtocol( PEPeerControl _manager, String _peer_source, String _ip, int _port ) {
    manager = _manager;
    peer_source	= _peer_source;
    ip    = _ip;
    port  = _port;
    tcp_listen_port = _port;
    
    peer_item_identity = PeerItemFactory.createPeerItem( ip, tcp_listen_port, PeerItem.convertSourceID( _peer_source ) );
    
    incoming = false;
    
    peer_stats = manager.createPeerStats();
    
    if( port < 0 || port > 65535 ) {
      closeConnectionInternally( "given remote port is invalid: " + port );
      return;
    }

    
    connection = NetworkManager.getSingleton().createConnection( new InetSocketAddress( ip, port ), new BTMessageEncoder(), new BTMessageDecoder() );
    
    changePeerState( PEPeer.CONNECTING );
    
    connection.connect( new NetworkConnection.ConnectionListener() {
      public void connectStarted() {
        connection_state = PEPeerTransport.CONNECTION_CONNECTING;
      }
 
      public void connectSuccess() {
        if( closing ) {
          Debug.out( "PEPeerTransportProtocol::connectSuccess() called when closing." );
          return;
        }
        
        if( LGLogger.isEnabled() )  LGLogger.log(componentID, evtLifeCycle, LGLogger.SENT, "Established outgoing connection with " + PEPeerTransportProtocol.this);
        initializeConnection();
        sendBTHandshake();
      }
        
      public void connectFailure( Throwable failure_msg ) {
        closeConnectionInternally( "failed to establish outgoing connection: " + failure_msg.getMessage() );
      }
        
      public void exceptionThrown( Throwable error ) {
        if( error.getMessage() == null ) {
          Debug.out( error );
        }
        
        closeConnectionInternally( "connection exception: " + error.getMessage() );
      }
    });
      
    if( LGLogger.isEnabled() )  LGLogger.log(componentID, evtLifeCycle, LGLogger.SENT, "Creating outgoing connection to " +ip+ ": " +port );
  }
  
  

  
  private void initializeConnection() {
    if( closing )  return;

    other_peer_has_pieces = new boolean[ manager.getPiecesNumber() ];
    Arrays.fill( other_peer_has_pieces, false );

    recent_outgoing_requests = new LinkedHashMap( 32, .75F, true ) {
      public boolean removeEldestEntry(Map.Entry eldest) {
        return size() > 32;
      }
    };
    recent_outgoing_requests_mon  = new AEMonitor( "PEPeerTransportProtocol:ROR" );
    
    message_limiter = new PeerMessageLimiter();

    //link in outgoing piece handler
    outgoing_piece_message_handler = new OutgoingBTPieceMessageHandler( manager.getDiskManager(), connection.getOutgoingMessageQueue() );

    //link in outgoing have message aggregator
    outgoing_have_message_aggregator = new OutgoingBTHaveMessageAggregator( connection.getOutgoingMessageQueue() );

    connection_established_time = SystemTime.getCurrentTime();
    
    connection_state = PEPeerTransport.CONNECTION_WAITING_FOR_HANDSHAKE;
    changePeerState( PEPeer.HANDSHAKING );
    
    registerForMessageHandling();
  }
  
  
  
  
  
	public String
	getPeerSource()
	{
		return( peer_source );
	}
  


  //close the peer connection internally
  private void closeConnectionInternally( String reason ) {
    performClose( reason, false );
  }
  
  
  //close the peer connection externally
  public void closeConnection( String reason ) {
    performClose( reason, true );
  }

  
  private void performClose( String reason, boolean externally_closed ) {
    try{
      closing_mon.enter();
    
      if( closing ){        
        return;
      }
      
      closing = true;
      
    }finally{
      closing_mon.exit();
    }
    
    changePeerState( PEPeer.CLOSING );
    
       
    if( identityAdded ) {  //remove identity
    	if( peer_id != null ) {
    		PeerIdentityManager.removeIdentity( manager.getPeerIdentityDataID(), peer_id );
    	}
    	else {
    		Debug.out( "PeerIdentity added but peer_id == null !!!" );
    	}    	
    }
    
    
    if( outgoing_piece_message_handler != null ) {
      outgoing_piece_message_handler.destroy();
    }
    
    if( outgoing_have_message_aggregator != null ) {
      outgoing_have_message_aggregator.destroy();
    }
    
    if( peer_exchange_item != null ) {
      peer_exchange_item.destroy();
    }
    
    if( connection != null ) {  //can be null if close is called within ::<init>::, like when the given port is invalid
      connection.close();
    }
    
    //cancel any pending requests (on the manager side)
    cancelRequests();
 
    if ( ip_resolver_request != null ){
      ip_resolver_request.cancel();
    }
    

    changePeerState( PEPeer.DISCONNECTED );
    
    if( LGLogger.isEnabled() )  LGLogger.log(componentID, evtLifeCycle, LGLogger.INFORMATION, "Peer connection [" +toString()+ "] closed: " +reason );

    if( !externally_closed ) {  //if closed internally, notify manager, otherwise we assume it already knows
      manager.peerConnectionClosed( this );
    }
  }
  
  

  
  
  
  private void sendBTHandshake() {
    connection.getOutgoingMessageQueue().addMessage(
        new BTHandshake( manager.getHash(),
                         manager.getPeerId(),
                         manager.getDownloadManager().isAZMessagingEnabled() ), false );
  }
  
  
  
  private void sendAZHandshake() {
    Message[] avail_msgs = MessageManager.getSingleton().getRegisteredMessages();
    String[] avail_ids = new String[ avail_msgs.length ];
    byte[] avail_vers = new byte[ avail_msgs.length ];
    
    for( int i=0; i < avail_msgs.length; i++ ) {
      avail_ids[i] = avail_msgs[i].getID();
      avail_vers[i] = avail_msgs[i].getVersion();
    }
    
    int local_udp_port = 0;
    try{  //TODO udp port value should be in the core someday
    	
        PluginInterface dht_pi = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByClass( DHTPlugin.class );
        
        	// may not be present
        	
        if ( dht_pi != null ){
        	
        	DHTPlugin dht = (DHTPlugin)dht_pi.getPlugin();
             
        	local_udp_port = dht.getPort();
        }
    }
    catch( Throwable t ) {
      Debug.out( "Exception while obtaining local udp listen port from DHTPlugin:", t );
    }
    
    AZHandshake az_handshake = new AZHandshake(
        AZPeerIdentityManager.getAZPeerIdentity(),
        Constants.AZUREUS_NAME,
        Constants.AZUREUS_VERSION,
        COConfigurationManager.getIntParameter( "TCP.Listen.Port" ),
        local_udp_port,
        avail_ids,
        avail_vers );        
    
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

    int length = other_peer_has_pieces.length;
    
    if( length < 1 )  return 0;
    
    return (sum * 1000) / length;
  }
  

  public boolean transferAvailable() {
    return (!choked_by_other_peer && interested_in_other_peer);
  }

    
  
  private void printRequestStats() {
    if( SHOW_DISCARD_RATE_STATS ) {
      float discard_perc = (requests_discarded * 100F) / ((requests_completed + requests_recovered + requests_discarded) * 1F);
      float discard_perc_end = (requests_discarded_endgame * 100F) / ((requests_completed + requests_recovered + requests_discarded_endgame) * 1F);
      float recover_perc = (requests_recovered * 100F) / ((requests_recovered + requests_discarded) * 1F);
      System.out.println( "c="+requests_completed+ " d="+requests_discarded+ " de="+requests_discarded_endgame+ " r="+requests_recovered+ " dp="+discard_perc+  "% dpe="+discard_perc_end+ "% rp="+recover_perc+ "%" );
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
    
    //System.out.println( "["+(System.currentTimeMillis()/1000)+"] " +connection + " choked");
    
    outgoing_piece_message_handler.removeAllPieceRequests();
    connection.getOutgoingMessageQueue().addMessage( new BTChoke(), false );
    choking_other_peer = true;
    is_optimistic_unchoke = false;
  }

  
  public void sendUnChoke() {
    if ( getPeerState() != TRANSFERING ) return;
    
    //System.out.println( "["+(System.currentTimeMillis()/1000)+"] " +connection + " unchoked");
    
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
    
		boolean is_interesting = false;
    
    if( manager.getDiskManager().hasDownloadablePiece() ) {  //there is a piece worth being interested in
      DiskManagerPiece[]  pieces = manager.getDiskManager().getPieces();

      for (int i = 0; i < pieces.length; i++) {
        if ( !pieces[i].getDone() && pieces[i].isNeeded() && other_peer_has_pieces[i] ) {
          is_interesting = true;
          break;
        }
      }
    }
    
    if ( is_interesting && !interested_in_other_peer ) {
      connection.getOutgoingMessageQueue().addMessage( new BTInterested(), false );
    }
    else if ( !is_interesting && interested_in_other_peer ) {
      connection.getOutgoingMessageQueue().addMessage( new BTUninterested(), false );
    }
    
    interested_in_other_peer = is_interesting;
  }

  
  
  /**
   * Checks interested given a new piece received
   * @param pieceNumber the piece number that has been received
   */
  private void checkInterested( int pieceNumber ) {
    if ( getPeerState() == CLOSING ) return;

    boolean is_interesting = false;
    
    if( manager.getDiskManager().hasDownloadablePiece() ) {  //there is a piece worth being interested in
      DiskManagerPiece piece = manager.getDiskManager().getPieces()[ pieceNumber ];
      
      is_interesting = !piece.getDone() && piece.isNeeded();  //we dont have that piece yet
    }
    
    if ( is_interesting && !interested_in_other_peer ) {
      connection.getOutgoingMessageQueue().addMessage( new BTInterested(), false );
    }
    else if ( !is_interesting && interested_in_other_peer ) {
      connection.getOutgoingMessageQueue().addMessage( new BTUninterested(), false );
    }
    
    interested_in_other_peer = is_interesting;
  }
  


    
  /**
   * Private method to send the bitfield.
   */
  private void sendBitField() {
    if ( getPeerState() == CLOSING ) return;
    
		//In case we're in super seed mode, we don't send our bitfield
		if ( manager.isSuperSeedMode() ) return;
    
    ArrayList lazies = null;
    
    //create bitfield
		DirectByteBuffer buffer = DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_MSG, (manager.getPiecesNumber() + 7) / 8 );
		
		DiskManagerPiece[]	pieces = manager.getDiskManager().getPieces();
		
		int bToSend = 0;
		int i = 0;
		for (; i < pieces.length; i++) {
			if ( (i % 8) == 0 ) bToSend = 0;
			bToSend = bToSend << 1;
			if ( pieces[i].getDone()) {
        if( ENABLE_LAZY_BITFIELD ) {
          if( i < 8 || i >= (pieces.length - (pieces.length % 8)) ) {  //first and last bytes
            if( lazies == null ) lazies = new ArrayList();
            lazies.add( new Integer( i ) );  //send as a Have message instead
          }
          else {
            bToSend += 1;
          }
        }
        else {
          bToSend += 1;
        }
			}
			if ( (i % 8) == 7 ) buffer.put( DirectByteBuffer.SS_BT, (byte)bToSend );
		}
		if ( (i % 8) != 0 ) {
			bToSend = bToSend << (8 - (i % 8));
			buffer.put( DirectByteBuffer.SS_BT, (byte)bToSend );
		}

    buffer.flip( DirectByteBuffer.SS_BT );
    
    connection.getOutgoingMessageQueue().addMessage( new BTBitfield( buffer ), false );
    
    if( lazies != null ) {
      for( int x=0; x < lazies.size(); x++ ) {
        Integer num = (Integer)lazies.get( x );
      
        connection.getOutgoingMessageQueue().addMessage( new BTHave( num.intValue() ), false );
      }
    }
                                   
	}

  
  public byte[] getId() {  return peer_id;  }
  public String getIp() {  return ip;  }
  public int getPort() {  return port;  }
  
  public int getTCPListenPort() {  return tcp_listen_port;  }
  public int getUDPListenPort() {  return udp_listen_port;  }
  
  
  public String getClient() {  return client;  }
  
  public boolean isIncoming() {  return incoming;  }  
  
  
  public boolean isOptimisticUnchoke() {  return is_optimistic_unchoke && !isChokedByMe();  }
  public void setOptimisticUnchoke( boolean is_optimistic ) {  is_optimistic_unchoke = is_optimistic;  }
  
  
  public PEPeerControl getControl() {  return manager;  }
  public PEPeerManager getManager() {  return manager;  }
  public PEPeerStats getStats() {  return peer_stats;  }
  
  public boolean[] getAvailable() {  return other_peer_has_pieces;  }
  
  public boolean isChokingMe() {  return choked_by_other_peer;  }
  public boolean isChokedByMe() {  return choking_other_peer;  }
  public boolean isInterestingToMe() {  return interested_in_other_peer;  }
  public boolean isInterestedInMe() {  return other_peer_interested_in_me;  }
  public boolean isSeed() {  return seed;  }
  public boolean isSnubbed() {  return snubbed;  }
  public void setSnubbed(boolean b) {  snubbed = b;  }
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
      data_mon.enter();
  	
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
      data_mon.exit();
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
	    Message[] type = { new BTRequest( -1, -1, -1 ) };
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
        msg.destroy();
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
    if( connection != null ) {
      return connection + " [" + client+ "]";
    }
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
        closeConnectionInternally( "timed out while waiting for messages" );
        return true;
      }
    }
    //ensure we dont get stuck in the handshaking phases
    else if( connection_state == PEPeerTransport.CONNECTION_WAITING_FOR_HANDSHAKE ) {
      long wait_time = SystemTime.getCurrentTime() - connection_established_time;
      
      if( wait_time < 0 ) {  //oops, system clock went backwards
        connection_established_time = SystemTime.getCurrentTime();
        return false;
      }
      
      if( wait_time > 3*60*1000 ) { //3min timeout
        closeConnectionInternally( "timed out while waiting for handshake" );
        return true;
      }
    }
    
    return false;
  }
  
  
  
  public void doPerformanceTuningCheck() {
    if( peer_stats != null && outgoing_piece_message_handler != null ) {

      //send speed -based tuning
      long send_rate = peer_stats.getDataSendRate() + peer_stats.getProtocolSendRate();
      
      if( send_rate >= 3125000 ) {  // 25 Mbit/s
        connection.getTCPTransport().setTransportMode( TCPTransport.TRANSPORT_MODE_TURBO );
        outgoing_piece_message_handler.setRequestReadAhead( 256 );
      }
      else if( send_rate >= 1250000 ) {  // 10 Mbit/s
        connection.getTCPTransport().setTransportMode( TCPTransport.TRANSPORT_MODE_TURBO );
        outgoing_piece_message_handler.setRequestReadAhead( 128 );
      }
      else if( send_rate >= 125000 ) {  // 1 Mbit/s
        if( connection.getTCPTransport().getTransportMode() < TCPTransport.TRANSPORT_MODE_FAST ) {
          connection.getTCPTransport().setTransportMode( TCPTransport.TRANSPORT_MODE_FAST );
        }
        outgoing_piece_message_handler.setRequestReadAhead( 32 );
      }
      else if( send_rate >= 62500 ) {  // 500 Kbit/s
        outgoing_piece_message_handler.setRequestReadAhead( 16 );
      }
      else if( send_rate >= 31250 ) {  // 250 Kbit/s
        outgoing_piece_message_handler.setRequestReadAhead( 8 );
      }
      else if( send_rate >= 12500 ) {  // 100 Kbit/s
        outgoing_piece_message_handler.setRequestReadAhead( 4 );
      }
      else {
        outgoing_piece_message_handler.setRequestReadAhead( 2 );
      }
      
      
      //receive speed -based tuning
      long receive_rate = peer_stats.getDataReceiveRate() + peer_stats.getProtocolReceiveRate();
      
      if( receive_rate >= 1250000 ) {  // 10 Mbit/s
        connection.getTCPTransport().setTransportMode( TCPTransport.TRANSPORT_MODE_TURBO );
      }
      else if( receive_rate >= 125000 ) {  // 1 Mbit/s
        if( connection.getTCPTransport().getTransportMode() < TCPTransport.TRANSPORT_MODE_FAST ) {
          connection.getTCPTransport().setTransportMode( TCPTransport.TRANSPORT_MODE_FAST );
        }
      }
      
    }
  }
  
  
  
  
  public int getConnectionState() {  return connection_state;  }
  
  
  
  
  public long getTimeSinceLastDataMessageReceived() {
    if( last_data_message_received_time == -1 ) {  //never received
      return -1;
    }
    
    long time_since = SystemTime.getCurrentTime() - last_data_message_received_time;
    
    if( time_since < 0 ) {  //time went backwards
      last_data_message_received_time = SystemTime.getCurrentTime();
      time_since = 0;
    }
    
    return time_since;    
  }
  
  
  
  public long getTimeSinceLastDataMessageSent() {
    if( last_data_message_sent_time == -1 ) {  //never sent
      return -1;
    }
    
    long time_since = SystemTime.getCurrentTime() - last_data_message_sent_time;
    
    if( time_since < 0 ) {  //time went backwards
      last_data_message_sent_time = SystemTime.getCurrentTime();
      time_since = 0;
    }
    
    return time_since;    
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
      closeConnectionInternally( "handshake has wrong infohash" );
      handshake.destroy();
      return;
    }
    
    peer_id = handshake.getPeerId();

    //decode a client identification string from the given peerID
    client = PeerClassifier.getClientDescription( peer_id );

    //make sure the client type is not banned
    if( !PeerClassifier.isClientTypeAllowed( client ) ) {
      closeConnectionInternally( client+ " client type not allowed to connect, banned" );
      handshake.destroy();
      return;
    }

    //make sure we are not connected to ourselves
    if( Arrays.equals( manager.getPeerId(), peer_id ) ) {
      manager.peerVerifiedAsSelf( this );  //make sure we dont do it again
      closeConnectionInternally( "given peer id matches myself" );
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
      closeConnectionInternally( "peer matches already-connected peer id" );
      handshake.destroy();
      return;
    }
    
    if( sameIP ) {
      closeConnectionInternally( "peer matches already-connected IP address, duplicate connections not allowed" );
      handshake.destroy();
      return;
    }

    //make sure we haven't reached our connection limit
    int maxAllowed = PeerUtils.numNewConnectionsAllowed( my_peer_data_id );
    if( maxAllowed == 0 ) {
    	String msg = "too many existing peer connections [p" +
    								PeerIdentityManager.getIdentityCount( my_peer_data_id )+
    								"/g" +PeerIdentityManager.getTotalIdentityCount()+
    								", pmx" +PeerUtils.MAX_CONNECTIONS_PER_TORRENT+ "/gmx" +
    								PeerUtils.MAX_CONNECTIONS_TOTAL+ "]";
    	//System.out.println( msg );
      closeConnectionInternally( msg );
      handshake.destroy();
      return;
    }

    identityAdded = true;
    PeerIdentityManager.addIdentity( my_peer_data_id, peer_id, ip );    

    if( LGLogger.isEnabled() )  LGLogger.log( componentID, evtLifeCycle, LGLogger.RECEIVED, toString() + " has sent their handshake" );

    handshake.destroy();
    
    
    /*
     * Waiting until we've received the initiating-end's full handshake, before sending back our own,
     * really should be the "proper" behavior.  However, classic BT trackers running NAT checking will
     * only send the first 48 bytes (up to infohash) of the peer handshake, skipping peerid, which means
     * we'll never get their complete handshake, and thus never reply, which causes the NAT check to fail.
     * So, we need to send our handshake earlier, after we've verified the infohash.
     * 
      if( incoming ) {  //wait until we've received their handshake before sending ours
        sendBTHandshake();
      }
    */
    
    
    //extended protocol processing
    if( (handshake.getReserved()[0] & 128) == 128 ) {  //if first (high) bit is set
      if( !manager.getDownloadManager().isAZMessagingEnabled() ) {
        if( LGLogger.isEnabled() )  LGLogger.log( "Ignoring peer [" +toString()+ "]'s extended AZ messaging support, as disabled for this download." );
      }
      else if( client.indexOf( "Plus!" ) != -1 ) {
        if( LGLogger.isEnabled() )  LGLogger.log( toString() + " handshake mistakingly indicates extended AZ messaging support....ignoring." );
      }
      else {
        if( client.indexOf( "Azureus" ) == -1 ) {
          if( LGLogger.isEnabled() )  LGLogger.log( toString() + " handshake claims extended AZ messaging support....enabling AZ mode." );
        }
        
        az_messaging_mode = true;
        connection.getIncomingMessageQueue().setDecoder( new AZMessageDecoder() );
        connection.getOutgoingMessageQueue().setEncoder( new AZMessageEncoder() );
      
        sendAZHandshake();
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
     
      connection.getIncomingMessageQueue().resumeQueueProcessing();  //HACK: because BT decoder is auto-paused after initial handshake, so it doesn't accidentally decode the next AZ message
             
      changePeerState( PEPeer.TRANSFERING );
      
      connection_state = PEPeerTransport.CONNECTION_FULLY_ESTABLISHED;
      
      sendBitField(); 
    }
    
  }
  
  
  
  private void decodeAZHandshake( AZHandshake handshake ) {
    client = handshake.getClient()+ " " +handshake.getClientVersion();

    if( incoming && handshake.getTCPListenPort() > 0 ) {  //use the ports given in handshake
      tcp_listen_port = handshake.getTCPListenPort();
      udp_listen_port = handshake.getUDPListenPort();
      
      //remake the id using the peer's remote listen port instead of their random local port
      peer_item_identity = PeerItemFactory.createPeerItem( ip, tcp_listen_port, PeerItem.convertSourceID( peer_source ) );
    }

    //find mutually available message types
    ArrayList messages = new ArrayList();

    for( int i=0; i < handshake.getMessageIDs().length; i++ ) {
      Message msg = MessageManager.getSingleton().lookupMessage( handshake.getMessageIDs()[i], handshake.getMessageVersions()[i] );
      
      if( msg != null ) {  //mutual support!
        messages.add( msg );
      }
    }
    
    supported_messages = (Message[])messages.toArray( new Message[0] );
     
    changePeerState( PEPeer.TRANSFERING );
    
    connection_state = PEPeerTransport.CONNECTION_FULLY_ESTABLISHED;

    sendBitField();
    
    handshake.destroy();
  }
  
  
  

  
  
  
  private void decodeBitfield( BTBitfield bitfield ) {
    DirectByteBuffer field = bitfield.getBitfield();
   
    byte[] dataf = new byte[ (manager.getPiecesNumber() + 7) / 8 ];
         
    if( field.remaining( DirectByteBuffer.SS_PEER ) < dataf.length ) {
      Debug.out( toString() + " has sent invalid Bitfield: too short [" +field.remaining( DirectByteBuffer.SS_PEER )+ "<" +dataf.length+ "]" );
      if( LGLogger.isEnabled() )  LGLogger.log( componentID, evtProtocol, LGLogger.ERROR, toString() + " has sent invalid Bitfield: too short [" +field.remaining( DirectByteBuffer.SS_PEER )+ "<" +dataf.length+ "]" );
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
      closeConnectionInternally( "invalid piece_number: " + piece_number );
      have.destroy();
      return;
    }

    other_peer_has_pieces[piece_number] = true;
    int pieceLength = manager.getPieceLength(piece_number);
    peer_stats.hasNewPiece(pieceLength);
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
      closeConnectionInternally( "request #" + number + ":" + offset + "->" + (offset + length) + " is an invalid request" );
      request.destroy();
      return;
    }
      
    if( !choking_other_peer ) {
      outgoing_piece_message_handler.addPieceRequest( number, offset, length );
    }
    else {
      if( LGLogger.isEnabled() )  LGLogger.log( componentID, evtProtocol, LGLogger.RECEIVED, toString() + " has requested #" + number + ":" + offset + "->" + (offset + length) + " but peer is currently choked. Request ignored." );
    }

    request.destroy();  
  }
  
  
  
  private void decodePiece( BTPiece piece ) {
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
      if( LGLogger.isEnabled() )  LGLogger.log( componentID, evtProtocol, LGLogger.RECEIVED, error_msg + "but piece block discarded as invalid." );
      peer_stats.bytesDiscarded( length );
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
        peer_stats.bytesDiscarded( length );
        manager.discarded( length );

        if( manager.isInEndGameMode() ) {  //we're probably in end-game mode then
          if( LGLogger.isEnabled() )  LGLogger.log( componentID, evtProtocol, LGLogger.RECEIVED, error_msg + "but piece block ignored as already written in end-game mode." );      
          requests_discarded_endgame++;
        }
        else {
          if( LGLogger.isEnabled() )  LGLogger.log( componentID, evtProtocol, LGLogger.RECEIVED, error_msg + "but piece block discarded as already written." );
          requests_discarded++;
        }
        
        printRequestStats();
      }
      else {  //successfully received block!
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
          if( LGLogger.isEnabled() )  LGLogger.log( componentID, evtProtocol, LGLogger.RECEIVED, error_msg + "expired piece block data recovered as useful." );
          setSnubbed( false );
          reSetRequestsTime();
          manager.writeBlockAndCancelOutstanding( number, offset, payload, this );
          requests_recovered++;
          printRequestStats();
          piece_error = false;  //dont destroy message, as we've passed the payload on to the disk manager for writing
        }
        else {
          if( LGLogger.isEnabled() )  LGLogger.log( componentID, evtProtocol, LGLogger.RECEIVED, error_msg + "but expired piece block discarded as never requested." );
          
          System.out.println( error_msg + "but expired piece block discarded as never requested." );
          
          peer_stats.bytesDiscarded( length );
          manager.discarded( length );
          requests_discarded++;
          printRequestStats();
        }
      }
      else {
        if( LGLogger.isEnabled() )  LGLogger.log( componentID, evtProtocol, LGLogger.RECEIVED, error_msg + "but expired piece block discarded as already written." );
        peer_stats.bytesDiscarded( length );
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
    
    //INCOMING MESSAGES
    connection.getIncomingMessageQueue().registerQueueListener( new IncomingMessageQueue.MessageQueueListener() {
      public boolean messageReceived( Message message ) {      
        if( LGLogger.isEnabled() )  LGLogger.log( LGLogger.CORE_NETWORK, "Received [" +message.getDescription()+ "] message from " +PEPeerTransportProtocol.this );
        
        last_message_received_time = SystemTime.getCurrentTime();
        if( message.getType() == Message.TYPE_DATA_PAYLOAD ) {
          last_data_message_received_time = SystemTime.getCurrentTime();
        }
        
        if( message.getID().equals( BTMessage.ID_BT_KEEP_ALIVE ) ) {
          message.destroy();
          
          //make sure they're not spamming us
          if( !message_limiter.countIncomingMessage( message.getID(), 6, 60*1000 ) ) {  //allow max 6 keep-alives per 60sec
            System.out.println( "Incoming keep-alive message flood detected, dropping spamming peer connection." +PEPeerTransportProtocol.this );
            closeConnectionInternally( "Incoming keep-alive message flood detected, dropping spamming peer connection." );
          }

          return true;
        }

        
        if( message.getID().equals( BTMessage.ID_BT_HANDSHAKE ) ) {
          decodeBTHandshake( (BTHandshake)message );
          return true;
        }
        
        if( message.getID().equals( AZMessage.ID_AZ_HANDSHAKE ) ) {
          decodeAZHandshake( (AZHandshake)message );
          return true;
        }

        if( message.getID().equals( BTMessage.ID_BT_BITFIELD ) ) {
          decodeBitfield( (BTBitfield)message );
          return true;
        }
         
        if( message.getID().equals( BTMessage.ID_BT_CHOKE ) ) {
          decodeChoke( (BTChoke)message );
          if( choking_other_peer ) {
            connection.enableEnhancedMessageProcessing( false );  //downgrade back to normal handler
          }
          return true;
        }
        
        if( message.getID().equals( BTMessage.ID_BT_UNCHOKE ) ) {
          decodeUnchoke( (BTUnchoke)message );
          connection.enableEnhancedMessageProcessing( true );  //make sure we use a fast handler for the resulting download
          return true;
        }
        
        if( message.getID().equals( BTMessage.ID_BT_INTERESTED ) ) {
          decodeInterested( (BTInterested)message );
          return true;
        }
        
        if( message.getID().equals( BTMessage.ID_BT_UNINTERESTED ) ) {
          decodeUninterested( (BTUninterested)message );
          return true;
        }
        
        if( message.getID().equals( BTMessage.ID_BT_HAVE ) ) {
          decodeHave( (BTHave)message );
          return true;
        }
        
        if( message.getID().equals( BTMessage.ID_BT_REQUEST ) ) {
          decodeRequest( (BTRequest)message );
          return true;
        }
        
        if( message.getID().equals( BTMessage.ID_BT_PIECE ) ) {
          decodePiece( (BTPiece)message );
          return true;
        }
        
        if( message.getID().equals( BTMessage.ID_BT_CANCEL ) ) {
          decodeCancel( (BTCancel)message );
          return true;
        }

        if( message.getID().equals( AZMessage.ID_AZ_PEER_EXCHANGE ) ) {
          decodeAZPeerExchange( (AZPeerExchange)message );
          return true;
        }
        
        return false;
      }
      
      public void protocolBytesReceived( int byte_count ) {
        //update stats
        peer_stats.protocolBytesReceived( byte_count );
        manager.protocolBytesReceived( byte_count );
      }
      
      public void dataBytesReceived( int byte_count ) {
        //update stats
        peer_stats.dataBytesReceived( byte_count );
        manager.dataBytesReceived( byte_count );
      }
    });
    
    
    //OUTGOING MESSAGES
    connection.getOutgoingMessageQueue().registerQueueListener( new OutgoingMessageQueue.MessageQueueListener() {
      public boolean messageAdded( Message message ) {  return true;  }
      
      public void messageQueued( Message message ) { /* ignore */ }
      
      public void messageRemoved( Message message ) { /*ignore*/ }
        
      public void messageSent( Message message ) {
        //update keep-alive info
        last_message_sent_time = SystemTime.getCurrentTime();
        
        if( message.getType() == Message.TYPE_DATA_PAYLOAD ) {
          last_data_message_sent_time = SystemTime.getCurrentTime();
        }

        if( message.getID().equals( BTMessage.ID_BT_UNCHOKE ) ) { // is about to send piece data
          connection.enableEnhancedMessageProcessing( true );  //so make sure we use a fast handler
        }
        else if( message.getID().equals( BTMessage.ID_BT_CHOKE ) ) { // is done sending piece data
          if( choked_by_other_peer ) {
            connection.enableEnhancedMessageProcessing( false );  //so downgrade back to normal handler
          }
        }
        
        if( LGLogger.isEnabled() )  LGLogger.log( LGLogger.CORE_NETWORK, "Sent [" +message.getDescription()+ "] message to " +PEPeerTransportProtocol.this );
      }
  
      public void protocolBytesSent( int byte_count ) {
        //update stats
        peer_stats.protocolBytesSent( byte_count );
        manager.protocolBytesSent( byte_count );
      }
        
      public void dataBytesSent( int byte_count ) {
        //update stats
        peer_stats.dataBytesSent( byte_count );
        manager.dataBytesSent( byte_count );
      }
    });

    
    //start message processing
    connection.startMessageProcessing( manager.getUploadLimitedRateGroup(), manager.getDownloadLimitedRateGroup() );
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
  
  
  
  public void 
  addListener( 
	 PEPeerListener listener ) 
  {
	  try{
		peer_listeners_mon.enter();
	  
	    if( peer_listeners_cow == null ){
	    	
	    	peer_listeners_cow = new ArrayList();
	    }
	    
	    List	new_listeners = new ArrayList( peer_listeners_cow );
	    
	    new_listeners.add( listener );
	    
	    peer_listeners_cow	= new_listeners;
	    
	  }finally{
		  
		  peer_listeners_mon.exit();
	  }
  }
  
  public void 
  removeListener( 
		  PEPeerListener listener ) 
  {
	  try{
		  peer_listeners_mon.enter();

		  if ( peer_listeners_cow != null ){
			  
			   List	new_listeners = new ArrayList( peer_listeners_cow );
			    
			   new_listeners.remove( listener );
			   
			   if ( new_listeners.isEmpty()){
				   
				   new_listeners	= null;
			   }
			   
			   peer_listeners_cow	= new_listeners;
		  }
	  }finally{
		  
		  peer_listeners_mon.exit();
	  }
  }
  
  
  private void changePeerState( int new_state ) {
    current_peer_state = new_state;
    
    if( current_peer_state == PEPeer.TRANSFERING ) {   //YUCK!
      doPostHandshakeProcessing();
    }

    List	peer_listeners_ref = peer_listeners_cow;
    
    if ( peer_listeners_ref != null ){
    	
      for( int i=0; i < peer_listeners_ref.size(); i++ ) {
    	  
        PEPeerListener l = (PEPeerListener)peer_listeners_ref.get( i );
      
        l.stateChanged( current_peer_state );
      }
    }
  }
  
  
  
  
  private void doPostHandshakeProcessing() {
    //peer exchange registration
    if( manager.getDownloadManager().getDownloadState().isPeerSourceEnabled( PEPeerSource.PS_OTHER_PEER ) ) {
      //try and register all connections for their peer exchange info
      peer_exchange_item = manager.createPeerExchangeConnection( this );
    
      if( peer_exchange_item != null ) {
        //check for peer exchange support
        if( peerSupportsMessageType( AZMessage.ID_AZ_PEER_EXCHANGE ) ) {
          peer_exchange_supported = true;
        }
        else {  //no need to maintain internal states as we wont be sending/receiving peer exchange messages
          peer_exchange_item.disableStateMaintenance();
        }
      }
    }
  }
  
  
  
  private boolean peerSupportsMessageType( String message_id ) {
    if( supported_messages != null ) {
      for( int i=0; i < supported_messages.length; i++ ) {
        if( supported_messages[i].getID().equals( message_id ) )  return true;        
      }
    }
    return false;
  }
  
  
  
  public void updatePeerExchange() {
    if ( getPeerState() != TRANSFERING ) return;
    if( !peer_exchange_supported )  return;

    if( peer_exchange_item != null && manager.getDownloadManager().getDownloadState().isPeerSourceEnabled( PEPeerSource.PS_OTHER_PEER ) ) {
      PeerItem[] adds = peer_exchange_item.getNewlyAddedPeerConnections();
      PeerItem[] drops = peer_exchange_item.getNewlyDroppedPeerConnections();  
      
      if( (adds != null && adds.length > 0) || (drops != null && drops.length > 0) ) {
        connection.getOutgoingMessageQueue().addMessage( new AZPeerExchange( manager.getHash(), adds, drops ), false );
      }
    }
  }
 
  
  
  private void decodeAZPeerExchange( AZPeerExchange exchange ) {
    PeerItem[] added = exchange.getAddedPeers();
    PeerItem[] dropped = exchange.getDroppedPeers();
    
    exchange.destroy();
    
    //make sure they're not spamming us
    if( !message_limiter.countIncomingMessage( exchange.getID(), 7, 120*1000 ) ) {  //allow max 7 PEX per 2min  //TODO reduce max after 2306 release?
   	System.out.println( "Incoming PEX message flood detected, dropping spamming peer connection." +PEPeerTransportProtocol.this );
      closeConnectionInternally( "Incoming PEX message flood detected, dropping spamming peer connection." );
      return;
    }
    
    if( added != null && added.length > PeerExchangerItem.MAX_PEERS_PER_VOLLEY || dropped != null && dropped.length > PeerExchangerItem.MAX_PEERS_PER_VOLLEY ) {
      //drop these too-large messages as they seem to be used for DOS by swarm poisoners
   	System.out.println( "Invalid PEX message received: too large [" +added.length+ "/" +dropped.length+ "]" +PEPeerTransportProtocol.this );
      closeConnectionInternally( "Invalid PEX message received: too large, dropping likely poisoner peer connection." );
      return;
    }

    if( peer_exchange_supported && peer_exchange_item != null && manager.getDownloadManager().getDownloadState().isPeerSourceEnabled( PEPeerSource.PS_OTHER_PEER ) ) {
      if( added != null ) {
        for( int i=0; i < added.length; i++ ) {
          peer_exchange_item.addConnectedPeer( added[i] );
        }
      }
      
      if( dropped != null ) {
        for( int i=0; i < dropped.length; i++ ) {
          peer_exchange_item.dropConnectedPeer( dropped[i] );
        }
      }
    }
    else {
      if( LGLogger.isEnabled() )  LGLogger.log( "Peer Exchange disabled for this download, dropping received exchange message." );
    }
  }
  
  
  
  public PeerItem getPeerItemIdentity() {  return peer_item_identity;  }
  
  public int getReservedPieceNumber() {
    return reservedPiece;
  }
  
  public void setReservedPieceNumber(int pieceNumber) {
    reservedPiece = pieceNumber;
  }

}
