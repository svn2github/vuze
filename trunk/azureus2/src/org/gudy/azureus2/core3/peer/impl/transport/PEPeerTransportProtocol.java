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
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.*;

import org.gudy.azureus2.core3.util.*;

import org.gudy.azureus2.core3.disk.DiskManagerRequest;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.peer.impl.*;
import org.gudy.azureus2.core3.peer.util.*;
import org.gudy.azureus2.core3.config.*;

import com.aelitis.azureus.core.networkmanager.*;
import com.aelitis.azureus.core.peermanager.messages.*;
import com.aelitis.azureus.core.peermanager.messages.bittorrent.*;
import com.aelitis.azureus.core.peermanager.utils.PeerClassifier;



/**
 * @author Olivier
 *
 */
public abstract class 
PEPeerTransportProtocol
	implements PEPeerTransport
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
	private int hashcode;
	
	private PEPeerStatsImpl stats;
  private List requested;
  private HashMap data;
  
  private boolean choked;
  private boolean interested_in_other_peer;
  private boolean choking;
  private boolean other_peer_interested_in_me;
  private boolean snubbed;
  private boolean[] other_peer_has_pieces;
  private boolean seed;

	//The Buffer for reading the length of the messages
	private DirectByteBuffer lengthBuffer;


  private boolean incoming;
  private volatile boolean closing;
  private PEPeerTransportProtocolState currentState;
  
  private Connection connection;
  private OutgoingMessageQueue outgoing_message_queue;
  private OutgoingBTPieceMessageHandler outgoing_piece_message_handler;
  private OutgoingBTHaveMessageAggregator outgoing_have_message_aggregator;
  
  private boolean identityAdded = false;  //needed so we don't remove id's in closeAll() on duplicate connection attempts
  
  //The client name identification	
	private String client = "";

	//Reader inner loop counter
	private int processLoop;
  
	//Number of connections made since creation
	private int nbConnections;
  
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

  private long last_bytes_sent_time = 0;
  
  private final Connection.ConnectionListener connection_listener = new Connection.ConnectionListener() {
    public void notifyOfException( Throwable error ) {
      closeAll( "Connection [" + connection + "] error : " + error.getMessage(), true, true );
    }
  };
  
  private final OutgoingMessageQueue.ByteListener bytes_sent_listener = new OutgoingMessageQueue.ByteListener() {
    public void bytesSent( int byte_count ) {
      //update keep-alive info
      last_bytes_sent_time = SystemTime.getCurrentTime();
      //update stats
      stats.sent( byte_count );
      manager.sent( byte_count );
    }
  };
  
  private final Map recent_outgoing_requests = new LinkedHashMap( 100, .75F, true ) {
    public boolean removeEldestEntry(Map.Entry eldest) {
      return size() > 100;
    }
  };
  
  
  private static final boolean SHOW_DISCARD_RATE_STATS;
  
  static {
  	String	prop = System.getProperty( "show.discard.rate.stats" );
  	
  	SHOW_DISCARD_RATE_STATS = prop != null && prop.equals( "1" );
  }
  
  private static int requests_discarded = 0;
  private static int requests_recovered = 0;
  private static int requests_completed = 0;
  
  
  

  /*
	 * This object constructors will let the PeerConnection partially created,
	 * but hopefully will let us gain some memory for peers not going to be
	 * accepted.
	 */

  /**
   * The Default Contructor for outgoing connections.
   * @param manager the manager that will handle this PeerConnection
   * @param peerId the other's peerId which will be checked during Handshaking
   * @param ip the peer Ip Address
   * @param port the peer port
   */
	public 
	PEPeerTransportProtocol(
  		PEPeerControl 	manager, 
  		byte[] 			peerId, 
  		String 			ip, 
  		int 			port,
  		boolean			incoming_connection, 
  		byte[]			data_already_read,
  		boolean 		fake ) 
	{		
		this.manager	= manager;
		this.ip 		= ip;
		this.port 		= port;
	 	this.id 		= peerId;
	 	
	 	this.hashcode = (ip + String.valueOf(port)).hashCode();
		if ( fake ){
		
			return;
		}
	
		uniquePiece = -1;
		
		incoming = incoming_connection;
    
		
		if ( incoming ){
			
			allocateAll();

      LGLogger.log(componentID, evtLifeCycle, LGLogger.RECEIVED, "Creating incoming connection from " + toString());
      
			currentState = new StateHandshaking( data_already_read );
					
		}else{
			
			nbConnections = 0;
			
			currentState = new StateConnecting();
		}
	}



  /**
   * Private method that will finish fields allocation, once the handshaking is ok.
   * Hopefully, that will save some RAM.
   */
  private void allocateAll() {
  	seed = false;
  	choked = true;
  	interested_in_other_peer = false;
  	requested = new ArrayList();
  	choking = true;
  	other_peer_interested_in_me = false;
  	other_peer_has_pieces = new boolean[manager.getPiecesNumber()];
  	Arrays.fill(other_peer_has_pieces, false);
  	stats = (PEPeerStatsImpl)manager.createPeerStats();
  	this.closing = false;
  	this.lengthBuffer = DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_PT_LENGTH,4 );
  }

   
  
  public synchronized void closeAll(String reason, boolean closedOnError, boolean attemptReconnect) {
    //System.out.println(reason + ", " + closedOnError+ ", " + attemptReconnect);
    
  	LGLogger.log(
  			componentID,
				evtProtocol,
				closedOnError?LGLogger.ERROR:LGLogger.INFORMATION,
						reason);
  	
  	if (closing) {
  		return;
  	}
  	closing = true;
    
    currentState = new StateClosing();
  	
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
    
    
    if( connection != null ) {
      manager.getConnectionPool().removeConnection( connection );
      connection.destroy();
      connection = null;
    }
    
    outgoing_message_queue = null;
    
    synchronized( recent_outgoing_requests ) {
      recent_outgoing_requests.clear();
    }
    
    //Close the socket
    closeConnection();
    
    
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
  	
    if( attemptReconnect && incoming == false && nbConnections < 3 ) {      
  		LGLogger.log(componentID, evtLifeCycle, LGLogger.INFORMATION, "Attempting to reconnect with " + toString());
  		currentState = new StateConnecting();
  	}
  	else {
  		currentState = new StateClosed();
  	}
  	
  }

	
  private class StateConnecting implements PEPeerTransportProtocolState {
    
    private StateConnecting() {
      nbConnections++;
      allocateAll();
      LGLogger.log(componentID, evtLifeCycle, LGLogger.SENT, "Creating outgoing connection to " + PEPeerTransportProtocol.this);

      startConnection();
    }
    
  	public int process() {
  		try {
  			if ( completeConnection() ) {
  				currentState = new StateHandshaking( null );
  			}
        return PEPeerControl.WAITING_SLEEP;
  		}
  		catch (IOException e) {
  			closeAll("Error in StateConnecting: (" + PEPeerTransportProtocol.this + " ) : " + e, false, false);
  			return PEPeerControl.NO_SLEEP;
  		}
  	}

  	public int getState() {
  		return CONNECTING;
  	}
  }
		
  private class StateHandshaking implements PEPeerTransportProtocolState {
    DirectByteBuffer handshake_read_buff;
    boolean sent_our_handshake = false;
    
    private StateHandshaking( byte[] data_already_read ) {
      handshake_read_buff = DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_PT_READ, 68 );
      if( handshake_read_buff == null ) {
        closeAll( toString() + ": handshake_read_buff is null", true, false );
        return;
      }

      if( data_already_read != null ) {
        handshake_read_buff.put( DirectByteBuffer.SS_PEER, data_already_read );
      }
    }
    
    public synchronized int process() {
      if( !sent_our_handshake ) {
        doConnectionLinking();
        outgoing_message_queue.addMessage( new BTHandshake( manager.getHash(), manager.getPeerId() ) );
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
  
  public synchronized int process() {      
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
					//length == 0 : Keep alive message, process next.
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

  private static class StateClosed implements PEPeerTransportProtocolState {
  	public int process() { return PEPeerControl.NO_SLEEP; }

  	public int getState() {
  		return DISCONNECTED;
  	}
  }
  

  
  public int processRead() {
  	try {
  		processLoop = 0;
  		if (currentState != null) {
  			return currentState.process();
  		}
      else return PEPeerControl.NO_SLEEP;
  	}
  	catch (Exception e) {
  		e.printStackTrace();
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
        
        DiskManagerRequest request = manager.createDiskManagerRequest( pieceNumber, pieceOffset, pieceLength );

        if( manager.checkBlock( pieceNumber, pieceOffset, message_buff ) ) {
          if( alreadyRequested( request ) ) {
            removeRequest( request );
            manager.received( pieceLength );
            setSnubbed( false );
            reSetRequestsTime();
            manager.writeBlock( pieceNumber, pieceOffset, message_buff, this );
            requests_completed++;
          }
          else { //initial request may have already expired, but check if we can use the data anyway
            if( !manager.isBlockAlreadyWritten( pieceNumber, pieceOffset ) ) {
              boolean ever_requested;
              synchronized( recent_outgoing_requests ) {
                ever_requested = recent_outgoing_requests.containsKey( request );
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
  	DiskManagerRequest request = manager.createDiskManagerRequest( pieceNumber, pieceOffset, pieceLength );
  	if( !alreadyRequested( request ) ) {
  		addRequest( request );
      synchronized( recent_outgoing_requests ) {
        recent_outgoing_requests.put( request, null );
      }
      outgoing_message_queue.addMessage( new BTRequest( pieceNumber, pieceOffset, pieceLength ) );
  		return true;
  	}
  	return false;
  }
  

  public void sendCancel( DiskManagerRequest request ) {
  	if ( getState() != TRANSFERING ) return;
		if ( alreadyRequested( request ) ) {
			removeRequest( request );
      outgoing_message_queue.addMessage( new BTCancel( request.getPieceNumber(), request.getOffset(), request.getLength() ) );
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
    outgoing_message_queue.addMessage( new BTChoke() );
  	choking = true;
  }

  
  public void sendUnChoke() {
    if ( getState() != TRANSFERING ) return;
    outgoing_message_queue.addMessage( new BTUnchoke() );
    choking = false;
  }


  private void sendKeepAlive() {
    if ( getState() != TRANSFERING ) return;
    outgoing_message_queue.addMessage( new BTKeepAlive() );
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
		boolean[] myStatus = manager.getPiecesStatus();
		for (int i = 0; i < myStatus.length; i++) {
			if ( !myStatus[i] && other_peer_has_pieces[i] ) {
				newInterested = true;
				break;
			}
		}
		if ( newInterested && !interested_in_other_peer ) {
      outgoing_message_queue.addMessage( new BTInterested() );
		}
    else if ( !newInterested && interested_in_other_peer ) {
      outgoing_message_queue.addMessage( new BTUninterested() );
		}
		interested_in_other_peer = newInterested;
	}

  
  /**
   * Checks interested given a new piece received
   * @param pieceNumber the piece number that has been received
   */
  private void checkInterested( int pieceNumber ) {
		boolean[] myStatus = manager.getPiecesStatus();
		boolean newInterested = !myStatus[ pieceNumber ];
		if ( newInterested && !interested_in_other_peer ) {
      outgoing_message_queue.addMessage( new BTInterested() );
		}
    else if ( !newInterested && interested_in_other_peer ) {
      outgoing_message_queue.addMessage( new BTUninterested() );
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
		boolean[] myStatus = manager.getPiecesStatus();
		int bToSend = 0;
		int i = 0;
		for (; i < myStatus.length; i++) {
			if ( (i % 8) == 0 ) bToSend = 0;
			bToSend = bToSend << 1;
			if ( myStatus[i] ) {
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
      outgoing_message_queue.addMessage( new BTBitfield( buffer ) );
		}
	}

  
  public byte[] getId() {  return id;  }
  public String getIp() {  return ip;  }
  public int getPort() {  return port;  }
  public String getClient() {  return client;  }
  
  public boolean isIncoming() {  return incoming;  }
  public int getDownloadPriority() {  return manager.getDownloadPriority();  }
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

  //abstract methods
  protected abstract void startConnection();
  protected abstract void closeConnection();
  protected abstract boolean completeConnection() throws IOException;
  protected abstract SocketChannel getSocketChannel();
  //TODO: remove
  protected abstract int readData( DirectByteBuffer	buffer ) throws IOException;
  
  public void hasSentABadChunk() {  nbBadChunks++;  }
  public int getNbBadChunks() {  return nbBadChunks;  }

  public void setUploadHint(int spreadTime) {  spreadTimeHint = spreadTime;  }
  public int getUploadHint() {  return spreadTimeHint;  }
  public void setUniqueAnnounce(int uniquePiece) {  this.uniquePiece = uniquePiece;  }
  public int getUniqueAnnounce() {  return this.uniquePiece;  }

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
  public synchronized void setData (String key, Object value) {
  	if (data == null) {
  	  data = new HashMap();
  	}
    if (value == null) {
      if (data.containsKey(key))
        data.remove(key);
    } else {
      data.put(key, value);
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
  /////////////////////////////////////////////////////////////////


  //TODO: remove all this request stuff
		protected void cancelRequests() {
			if ( requested != null ) {
        synchronized (requested) {
          for (int i = requested.size() - 1; i >= 0; i--) {
            DiskManagerRequest request = (DiskManagerRequest) requested.remove(i);
            manager.requestCanceled(request);
          }
        }
      }
      if( !closing ) {
        //cancel any unsent requests in the queue
        int[] type = { BTProtocolMessage.BT_REQUEST };
        outgoing_message_queue.removeMessagesOfType( type );
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
	    synchronized (requested) {
	    	for (int i = 0; i < requested.size(); i++) {
	    		try {
	    			DiskManagerRequest request = (DiskManagerRequest) requested.get(i);
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
            e.printStackTrace();
	    		}
	    	}
	    }
			return result;
		}
		
		protected boolean
		alreadyRequested(
			DiskManagerRequest	request )
		{
	    synchronized (requested) {
	      return requested.contains( request );
	    }
		}
		
		protected void
		addRequest(
			DiskManagerRequest	request )
		{
	    synchronized (requested) {
	    	requested.add(request);
	    }
		}
		
		protected void
		removeRequest(
			DiskManagerRequest	request )
		{
	    synchronized (requested) {
	    	requested.remove(request);
	    }
      BTRequest msg = new BTRequest( request.getPieceNumber(), request.getOffset(), request.getLength() );
      outgoing_message_queue.removeMessage( msg );
		}
		
		protected void 
		reSetRequestsTime() {
	    synchronized (requested) {
			  for (int i = 0; i < requested.size(); i++) {
			  	DiskManagerRequest request = null;
			  	try {
			  		request = (DiskManagerRequest) requested.get(i);
			  	}
			  	catch (Exception e) { e.printStackTrace(); }
	        
			  	if (request != null)
			  		request.reSetTime();
			  }
	    }
	}
    

	public String toString() {
    return ip + ":" + port + " [" + client+ "]";
	}
	
	public void
	addListener(
		PEPeerListener	l )
	{
		synchronized( this ){
			
			if ( listeners == null ){
				
				listeners = new ArrayList(1);
			}
			
			listeners.add(l);
		}
	}
	
	public void
	removeListener(
		PEPeerListener	l )
	{
		synchronized( this ){
			
			if ( listeners == null ){
				
				listeners = new ArrayList(1);
			}
			
			listeners.remove(l);
		}
	}
  
  
  private void doConnectionLinking() {
    //get a connection object from the network manager - this will be an async callback some day   
    connection = NetworkManager.getSingleton().createNewConnection( getSocketChannel(), connection_listener );
    
    outgoing_message_queue = connection.getOutgoingMessageQueue();
    
    //attach the new connection to the torrent's pool so that peer messages get processed
    manager.getConnectionPool().addConnection( connection );
    
    //link in outgoing piece handler
    outgoing_piece_message_handler = new OutgoingBTPieceMessageHandler( manager.getDiskManager(), outgoing_message_queue );
    
    //link in outgoing have message aggregator
    outgoing_have_message_aggregator = new OutgoingBTHaveMessageAggregator( outgoing_message_queue );
    
    //register bytes sent listener
    outgoing_message_queue.registerByteListener( bytes_sent_listener );
  }
  
  
  
  public void doKeepAliveCheck() {
    if( last_bytes_sent_time == 0 )  last_bytes_sent_time = SystemTime.getCurrentTime(); //don't send if brand new connection
    if( SystemTime.getCurrentTime() - last_bytes_sent_time > 2*60*1000 ) {  //2min keep-alive timer
      sendKeepAlive();
      last_bytes_sent_time = SystemTime.getCurrentTime();  //not quite true, but we don't want to queue multiple keep-alives before the first is actually sent
    }
  }
  
}
