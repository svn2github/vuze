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
import java.util.*;

import org.gudy.azureus2.core3.util.*;

import org.gudy.azureus2.core3.disk.DiskManagerRequest;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.peer.impl.*;
import org.gudy.azureus2.core3.peer.util.*;
import org.gudy.azureus2.core3.config.*;

import com.aelitis.azureus.core.networkmanager.*;
import com.aelitis.azureus.core.peermanager.messages.OutgoingBTPieceMessageHandler;
import com.aelitis.azureus.core.peermanager.messages.OutgoingMessageQueue;
import com.aelitis.azureus.core.peermanager.messages.bittorrent.*;


/**
 * @author Olivier
 *
 */
public abstract class 
PEPeerTransportProtocol
	implements PEPeerTransport, PeerConnection
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
  private boolean interested;
  private boolean choking;
  private boolean interesting;
  private boolean snubbed;
  private boolean[] available;
  private boolean seed;

	//The reference to the current ByteBuffer used for reading on the socket.
	private DirectByteBuffer readBuffer;
	//The Buffer for reading the length of the messages
	private DirectByteBuffer lengthBuffer;
  //A flag to indicate if we're reading the length or the message
	private boolean readingLength;

  private boolean incoming;
  private volatile boolean closing;
  private PEPeerTransportProtocolState currentState;
  
  private OutgoingMessageQueue outgoing_message_queue = new OutgoingMessageQueue( this );
  private OutgoingBTPieceMessageHandler outgoing_piece_message_handler;
  private boolean identityAdded = false;  //needed so we don't remove id's in closeAll() on duplicate connection attempts
  
  //socket writing vars:
  private static final int WRITE_CACHE_SIZE = System.getProperty("socket.write.cache") == null ? 1460 : Integer.parseInt( System.getProperty("socket.write.cache"));
  private int allowed_to_write = 0;
  private boolean force_flush_mode = false;
  private long force_start_time = 0;
  private long previous_queue_size = 0;
  private int limit = 0;
  private int allowed = 0;
  private int used = 0;
  private int maxUpload = 100*1024*1024;
  private long last_write_time = 0;
  
  private long last_bw_check_time = 0;

  
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
  private int writeSleepTime;
  private long lastReadTime;
  private long lastWriteTime;
  
  private List		listeners;

 
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
    
    outgoing_piece_message_handler = new OutgoingBTPieceMessageHandler( manager.getDiskManager(), outgoing_message_queue );
		
		if ( incoming ){
			
			allocateAll();
			
			LGLogger.log(componentID, evtLifeCycle, LGLogger.RECEIVED, "Creating incoming connection from " + toString());
			
			handShake( data_already_read );
			
			currentState = new StateHandshaking();
					
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
  	interested = false;
  	requested = new ArrayList();
  	choking = true;
  	interesting = false;
  	available = new boolean[manager.getPiecesNumber()];
  	Arrays.fill(available, false);
  	stats = (PEPeerStatsImpl)manager.createPeerStats();
    PEPeerTransportSpeedLimiter.getLimiter().addUploader( this );
  	this.closing = false;
  	this.lengthBuffer = DirectByteBufferPool.getBuffer( 4 );
  }

  
  protected void handShake( byte[] data_already_read ) {
  	try {
  		byte[] hash = manager.getHash();
  		byte[] myPeerId = manager.getPeerId();

      outgoing_message_queue.addMessage( new BTHandshake( hash, myPeerId ) );
    
  		readBuffer = DirectByteBufferPool.getBuffer( 68 );
  		if ( readBuffer == null ) {
  			closeAll(toString() + " : PeerSocket::handShake:: readBuffer null", true, false);
  			return;
  		}

  		if ( data_already_read != null ) {
  			readBuffer.put( data_already_read );
  		}
  	}
  	catch (Exception e) {
  		closeAll(toString() + " : Exception in handshake : " + e, true, false);
  	}
  }
  

  protected void handleHandShakeResponse() {
	readBuffer.position(0);
	//Now test for data...

   byte b;
	if ((b = readBuffer.get()) != (byte) BTHandshake.PROTOCOL.length()) {
	   closeAll(toString() + " has sent handshake, but handshake starts with wrong byte : " + b,true, true);
	   return;
	}

	byte[] protocol = BTHandshake.PROTOCOL.getBytes();
	if (readBuffer.remaining() < protocol.length) {
	   closeAll(toString() + " has sent handshake, but handshake is of wrong size : " + readBuffer.remaining(),true, true);
	   return;
	}
	else {
	   readBuffer.get(protocol);
	   if (!(new String(protocol)).equals(BTHandshake.PROTOCOL)) {
		  closeAll(toString() + " has sent handshake, but protocol is wrong : " + new String(protocol),true, false);
		  return;
	   }
	}

	byte[] reserved = new byte[8];
	if (readBuffer.remaining() < reserved.length) {
	   closeAll(toString() + " has sent handshake, but handshake is of wrong size(2) : " + readBuffer.remaining(),true, true);
	   return;
	}
	else readBuffer.get(reserved);
	//Ignores reserved bytes


	byte[] hash = manager.getHash();
	byte[] otherHash = new byte[20];
	if (readBuffer.remaining() < otherHash.length) {
	   closeAll(toString() + " has sent handshake, but handshake is of wrong size(3) : " + readBuffer.remaining(),true, true);
	   return;
	}
	else {
	   readBuffer.get(otherHash);
	   for (int i = 0; i < 20; i++) {
		  if (otherHash[i] != hash[i]) {
			 closeAll(toString() + " has sent handshake, but infohash is wrong",true, false);
			 return;
		  }
	   }
	}

    
	byte[] otherPeerId = new byte[20];
	if (readBuffer.remaining() < otherPeerId.length) {
	   closeAll(toString() + " has sent handshake, but handshake is of wrong size(4) : " + readBuffer.remaining(),true, true);
	   return;
	}
	else readBuffer.get(otherPeerId);

  this.id = otherPeerId;
  
  
  
  //make sure we are not connected to ourselves
  if ( Arrays.equals( manager.getPeerId(), otherPeerId )) {
    closeAll("OOPS, peerID matches myself", false, false);
    return;
  }
  

  
  //make sure we are not already connected to this peer
  boolean sameIdentity = PeerIdentityManager.containsIdentity( otherHash, otherPeerId );
  boolean sameIP = false;
  if (! COConfigurationManager.getBooleanParameter("Allow Same IP Peers")) {
    if ( PeerIdentityManager.containsIPAddress( otherHash, ip )) {
      sameIP = true;
    }
  }
  if ( sameIdentity ) {
    closeAll(toString() + " exchanged handshake, but peer matches pre-existing identity", false, false);
    return;
  }
  if ( sameIP ) {
    closeAll(toString() + " exchanged handshake, but peer matches pre-existing IP address", false, false);
    return;
  }
  
  
  //make sure we haven't reached our connection limit
  int maxAllowed = PeerUtils.numNewConnectionsAllowed( otherHash );
  if ( maxAllowed == 0 ) {
    closeAll("Too many existing peer connections", false, false);
    return;
  }
  
  
  PeerIdentityManager.addIdentity( otherHash, otherPeerId, ip );
  identityAdded = true;
 
  
	//decode a client identification string from the given peerID
	client = Identification.decode(otherPeerId);

  LGLogger.log(componentID, evtLifeCycle, LGLogger.RECEIVED,
               toString() + " has sent a handshake");


	sendBitField();
	readMessage(readBuffer);
	manager.peerAdded(this);
	currentState = new StateTransfering();
  }

  protected void readMessage(DirectByteBuffer buffer) {
  	lengthBuffer.position(0);
    if ( buffer != null ) {
    	buffer.position(0);
    }
  	readingLength = true;
    readBuffer = buffer;
  }

  /*
  //from parg's changes....not method not used with my refactoring. -Nolar
  protected void queueProtocolMessage( BTMessage message ) {
    if ( closing ) return;
    
    try{
    	if ( listeners != null ){
	    	for (int i=0;i<listeners.size();i++){
	    		
	    		((PEPeerListener)listeners.get(i)).messageQueued( this, message.getType());
	    	}
    	}
    }catch( Throwable e ){
    	// ignore an errors, in particular we aren't synchronized on listeners so there
    	// is a remote chance that the loop will fail due to listener removal
    }
	}
	*/
  
  
  public synchronized void closeAll(String reason, boolean closedOnError, boolean attemptReconnect) {
  	LGLogger.log(
  			componentID,
				evtProtocol,
				closedOnError?LGLogger.ERROR:LGLogger.INFORMATION,
						reason);
  	
  	if (closing) {
  		return;
  	}
  	closing = true;         
  	
  	//Cancel any pending requests (on the manager side)
  	cancelRequests();
  	
    outgoing_piece_message_handler.removeAllPieceRequests();
    
    PEPeerTransportSpeedLimiter.getLimiter().removeUploader( this );
    
  	//Close the socket
  	closeConnection();
  	
  	if (readBuffer != null) {
  		readBuffer.returnToPool();
  		readBuffer = null;
  	}
    
    if ( lengthBuffer != null ) {
      lengthBuffer.returnToPool();
      lengthBuffer = null;
    }
    
    outgoing_message_queue.destroy();
    
  	//remove identity
  	if ( this.id != null && identityAdded ) {
  		PeerIdentityManager.removeIdentity( manager.getHash(), this.id );
  	}
  	
  	//Send removed event ...
  	manager.peerRemoved(this);
  	
  	//Send a logger event
  	LGLogger.log(componentID, evtLifeCycle, LGLogger.INFORMATION, "Connection Ended with " + toString());
  	
  	if ( (attemptReconnect)
		  && (currentState != null)
		  && (currentState.getState() == TRANSFERING || currentState.getState() == HANDSHAKING)
			&& (incoming == false)
			&& (nbConnections < 3)) {
      
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
  				handShake(null);
  				currentState = new StateHandshaking();
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
		public synchronized int process() {
			if (readBuffer.hasRemaining()) {
				try {
					int read = readData(readBuffer);
          if (read == 0) {
            return PEPeerControl.DATA_EXPECTED_SLEEP;
          }
          else if (read < 0) {
						throw new IOException("End of Stream Reached");
          }
				} catch (IOException e) {
					closeAll(PEPeerTransportProtocol.this + " : StateHandshaking:: " + e, true, false);
					return 0;
				}
			}
      
			if (readBuffer.remaining() == 0) {
				handleHandShakeResponse();
			}
      
      return PEPeerControl.NO_SLEEP;
		}
    
		public int getState() {
			return HANDSHAKING;
		}
	}

  
  
private class StateTransfering implements PEPeerTransportProtocolState {
  public synchronized int process() {      
    if(++processLoop > 10)
      return PEPeerControl.NO_SLEEP;
          
    if (readingLength) {
      if (lengthBuffer.hasRemaining() ) {          
        try {
          int read = readData(lengthBuffer);
          
          if (read == 0) {
            //If there's nothing pending on the socket, then
            //we can quite safely send a request while we wait
            if(lengthBuffer.remaining() == 4) {
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

			if (!lengthBuffer.hasRemaining()) {
				int length = lengthBuffer.getInt(0);
		  
				if(length < 0) {
					closeAll(PEPeerTransportProtocol.this + " : length negative : " + length,true, true);
					return PEPeerControl.NO_SLEEP;
				}
      
				if(length >= DirectByteBufferPool.MAX_SIZE) {
					closeAll(PEPeerTransportProtocol.this + " : length greater than max size : " + length,true, true);
					return PEPeerControl.NO_SLEEP;
				}
        
				if (length > 0) {
					//return old readBuffer to pool if it's too small
					if(readBuffer.capacity() < length) {
						readBuffer.returnToPool();
						readBuffer = DirectByteBufferPool.getBuffer(length);
            if (readBuffer == null) { closeAll(PEPeerTransportProtocol.this + " readBuffer null",true, false); }
					}
      			
					readBuffer.position(0);
					readBuffer.limit(length);
          
					//'piece' data messages are greater than length 13
          if ( length > 13 ) {
            readyToRequest = true;
          }
          //protocol message, don't request until we know what the message is
          else {
            readyToRequest = false;
          }
          
					readingLength = false;
				}
				else {
					//readingLength = 0 : Keep alive message, process next.
					readMessage(readBuffer);
				}
			}
		}
    
	  if (!readingLength) {
	  	try {
	  		int read = readData(readBuffer);
        
        if (read == 0) {
          //nothing on the socket, wait a bit before trying again
          return PEPeerControl.DATA_EXPECTED_SLEEP;
        }
	  		else if (read < 0) {
	  			throw new IOException("End of Stream Reached");
	  		}
	  		else  {
        if (readBuffer.limit() > 13) {
	  				stats.received(read);
        }
	  		}
	  	}
	  	catch (IOException e) {
	  		closeAll(PEPeerTransportProtocol.this + " : StateTransfering::End of Stream Reached (reading data)",true, true);
	  		return PEPeerControl.NO_SLEEP;
	  	}
    
	  	if (!readBuffer.hasRemaining()) {
	  		//After each message has been received, we're not ready to request anymore,
	  		//Unless we finish the socket's queue, or we start receiving a piece.
	  		readyToRequest = false;
	  		analyseBuffer(readBuffer);         
	  		if(getState() == TRANSFERING && readingLength) {
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
  
  public int processWrite() {
    try {
      return write();
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
	for (int i = 0; i < available.length; i++) {
	  if (available[i])
		sum++;
	}

	sum = (sum * 1000) / available.length;
	return sum;
  }

  public boolean transferAvailable() {
	return (!choked && interested);
  }

  private void analyseBuffer(DirectByteBuffer buffer) {
  	boolean	logging_is_on = LGLogger.isLoggingOn();
	buffer.position(0);
	int pieceNumber, pieceOffset, pieceLength;
	byte cmd = buffer.get();
	switch (cmd) {
	  case BT_CHOKED :
			if (buffer.limit() != 1) {
			  closeAll(toString() + " choking received, but message of wrong size : " + buffer.limit(),true, true);
			  break;
			}
			if ( logging_is_on ) LGLogger.log(componentID, evtProtocol, LGLogger.RECEIVED,
			                                  toString() + " is choking you");
			choked = true;
			cancelRequests();
			readMessage(buffer);
			break;
	  case BT_UNCHOKED :
			if (buffer.limit() != 1) {
			  closeAll(toString() + " unchoking received, but message of wrong size : " + buffer.limit(),true, true);
			  break;
			}
			if ( logging_is_on ) LGLogger.log(componentID, evtProtocol, LGLogger.RECEIVED,
			                                  toString() + " is unchoking you");
			choked = false;
			readMessage(buffer);
			break;
	  case BT_INTERESTED :
			if (buffer.limit() != 1) {
			  closeAll(toString() + " interested received, but message of wrong size : " + buffer.limit(),true, true);
			  break;
			}
			if ( logging_is_on ) LGLogger.log(componentID, evtProtocol, LGLogger.RECEIVED,
			                                  toString() + " is interested");
			interesting = true;
			readMessage(buffer);
			break;
	  case BT_UNINTERESTED :
			if (buffer.limit() != 1) {
			  closeAll(toString() + " uninterested received, but message of wrong size : " + buffer.limit(),true, true);
			  break;
			}
			if ( logging_is_on ) LGLogger.log(componentID, evtProtocol, LGLogger.RECEIVED,
			                                  toString() + " is not interested");
			interesting = false;
			readMessage(buffer);
			break;
	  case BT_HAVE :
			if (buffer.limit() != 5) {
			  closeAll(toString() + " have received, but message of wrong size : " + buffer.limit(),true, true);
			  break;
			}
			pieceNumber = buffer.getInt();
			if ( logging_is_on ) LGLogger.log(componentID, evtProtocol, LGLogger.RECEIVED,
			                                  toString() + " has " + pieceNumber);
			have(pieceNumber);
			readMessage(buffer);
			break;
	  case BT_BITFIELD :
	  	if ( logging_is_on ) LGLogger.log(componentID, evtProtocol, LGLogger.RECEIVED,
			                                  toString() + " has sent BitField");
			setBitField(buffer);
			checkInterested();
			checkSeed();
			readMessage(buffer);
			break;
	  case BT_REQUEST :
			if (buffer.limit() != 13) {
			  closeAll(toString() + " request received, but message of wrong size : " + buffer.limit(),true, true);
			  break;
			}
			pieceNumber = buffer.getInt();
			pieceOffset = buffer.getInt();
			pieceLength = buffer.getInt();
			
			if ( logging_is_on ) LGLogger.log(componentID, evtProtocol, LGLogger.RECEIVED,
			                                  toString() + " has requested #" + 
			                                  pieceNumber + ":" + pieceOffset + "->" + 
			                                  (pieceOffset + pieceLength));
			
			if (manager.checkBlock(pieceNumber, pieceOffset, pieceLength)) {
			  if( !choking ) {
          outgoing_piece_message_handler.addPieceRequest( pieceNumber, pieceOffset, pieceLength );
			  }
        else {
			    LGLogger.log(componentID, evtProtocol, LGLogger.RECEIVED, toString()
	        + " has requested #"
	        + pieceNumber
	        + ":"
	        + pieceOffset
	        + "->"
	        + (pieceOffset + pieceLength)        
	        + " but peer is currently choked. Request dropped");
			  }
			}
			else {
			  closeAll(toString()
	        + " has requested #"
	        + pieceNumber
	        + ":"
	        + pieceOffset
	        + "->"
	        + (pieceOffset + pieceLength)        
	        + " which is an invalid request.",
	        true, true);
			  return;
			}
			readMessage(buffer);
			break;
	  case BT_PIECE :
			if (buffer.limit() < 9) {
			   closeAll(toString() + " piece block received, but message of wrong size : " + buffer.limit(),true, true);
			   break;
			}
			pieceNumber = buffer.getInt();
			pieceOffset = buffer.getInt();
			pieceLength = buffer.limit() - buffer.position();
			if ( logging_is_on ){
				LGLogger.log(
					componentID,
					evtProtocol,
					LGLogger.RECEIVED,
					toString() + " has sent #" + pieceNumber + ":" + pieceOffset + "->" + (pieceOffset + pieceLength));
			}
			DiskManagerRequest request = manager.createDiskManagerRequest(pieceNumber, pieceOffset, pieceLength);
			if (alreadyRequested(request) && manager.checkBlock(pieceNumber, pieceOffset, buffer)) {
			  removeRequest( request );
			  manager.received(pieceLength);
			  setSnubbed(false);
			  reSetRequestsTime();
			  manager.writeBlock(pieceNumber, pieceOffset, buffer,this);
        buffer = DirectByteBufferPool.getBuffer( buffer.limit() );
        if (buffer == null)
          closeAll(toString() + " BT_PIECE buffer null", true, false);
			  readMessage(buffer);      
			}
			else {
        String msg = toString() + " has sent #" + pieceNumber + ":"
                     + pieceOffset + "->" + (pieceOffset + pieceLength);
        if (alreadyRequested(request))
          msg += " but piece block was discarded as invalid";
        else 
          msg += " but piece block was discarded as unrequested";
        
			  LGLogger.log( componentID, evtErrors, LGLogger.ERROR, msg);
			  stats.discarded(pieceLength);
			  manager.discarded(pieceLength);
			  readMessage(buffer);
			}
			break;
	  case BT_CANCEL :
			if (buffer.limit() != 13) {
			  closeAll(toString() + " cancel received, but message of wrong size : " + buffer.limit(),true, true);
			  break;
			}
			pieceNumber = buffer.getInt();
			pieceOffset = buffer.getInt();
			pieceLength = buffer.getInt();
			if ( logging_is_on ){
				LGLogger.log(
					componentID,
					evtProtocol,
					LGLogger.RECEIVED,
					toString() + " has canceled #" + pieceNumber + ":" + pieceOffset + "->" + (pieceOffset + pieceLength));
			}
      outgoing_piece_message_handler.removePieceRequest( pieceNumber, pieceOffset, pieceLength );
			readMessage(buffer);
			break;
	  default:
       Debug.out(toString() + " has sent an unknown protocol message id: " + cmd);
	    closeAll(toString() + " has sent a wrong message " + cmd,true, true);
	}
  }

  private void have(int pieceNumber) {
	if ((pieceNumber >= available.length) || (pieceNumber < 0)) {
	   closeAll(toString() + " gave invalid pieceNumber:" + pieceNumber,true, true);
      return;
	}
	else {    
	  available[pieceNumber] = true;
    int pieceLength = manager.getPieceLength(pieceNumber);
	  stats.haveNewPiece(pieceLength);
	  manager.havePiece(pieceNumber, pieceLength, this);
	  if (!interested)
		 checkInterested(pieceNumber);
	  checkSeed();
	}
  }

  /**
	 * Checks if it's a seed or not.
	 */
  private void checkSeed() {
	for (int i = 0; i < available.length; i++) {
	  if (!available[i])
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
  	if ( !alreadyRequested( request ) ) {
  		addRequest( request );
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
    outgoing_message_queue.addMessage( new BTHave( pieceNumber ) );
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
    outgoing_message_queue.addMessage( new BTKeepAlive() );
  }
  
  
  private void setBitField(DirectByteBuffer buffer) {
	byte[] dataf = new byte[(manager.getPiecesNumber() + 7) / 8];
   
	if (buffer.remaining() < dataf.length) {
     LGLogger.log(componentID, evtProtocol, LGLogger.ERROR, 
                  toString() + " has sent invalid BitField: too short");
	  return;
   }
   
	buffer.get(dataf);
	for (int i = 0; i < available.length; i++) {
	  int index = i / 8;
	  int bit = 7 - (i % 8);
	  byte bData = dataf[index];
	  byte b = (byte) (bData >> bit);
	  if ((b & 0x01) == 1) {
	    available[i] = true;
	    manager.updateSuperSeedPiece(this,i);
	  }
	  else {
		available[i] = false;
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
			if ( !myStatus[i] && available[i] ) {
				newInterested = true;
				break;
			}
		}
		if ( newInterested && !interested ) {
      outgoing_message_queue.addMessage( new BTInterested() );
		}
    else if ( !newInterested && interested ) {
      outgoing_message_queue.addMessage( new BTUninterested() );
		}
		interested = newInterested;
	}

  
  /**
   * Checks interested given a new piece received
   * @param pieceNumber the piece number that has been received
   */
  private void checkInterested( int pieceNumber ) {
		boolean[] myStatus = manager.getPiecesStatus();
		boolean newInterested = !myStatus[ pieceNumber ];
		if ( newInterested && !interested ) {
      outgoing_message_queue.addMessage( new BTInterested() );
		}
    else if ( !newInterested && interested ) {
      outgoing_message_queue.addMessage( new BTUninterested() );
		}
		interested = newInterested;
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


 
  protected synchronized int write() {
  	if( currentState.getState() != HANDSHAKING && currentState.getState() != TRANSFERING ) {
  		return PEPeerControl.WAITING_SLEEP;       
    }
    
    if( outgoing_message_queue.getTotalSize() > 0 ) {
      //update allowed bytes
      
      if( PEPeerTransportSpeedLimiter.getLimiter().isLimited( this ) ) {
        if( SystemTime.getCurrentTime() - last_bw_check_time > 50 ) {
          if( used >= (95 * allowed) / 100 )
            maxUpload = max( 110 * allowed / 100, 50 );
          if( used < (90 * allowed) / 100 )
            maxUpload = max( (100 * used) / 100, 10 );
          
          allowed = PEPeerTransportSpeedLimiter.getLimiter().getLimitPer100ms( this );
          last_bw_check_time = SystemTime.getCurrentTime();
          allowed_to_write += allowed;
          used = 0;
        }
      }
      else {
        allowed_to_write = WRITE_CACHE_SIZE;
      }
                  
      //we are allowed to write
      if( allowed_to_write >= WRITE_CACHE_SIZE ) { //it is assumed that allowed_tw will always eventually fill
        int data_ready = outgoing_message_queue.getTotalSize();
        
        //make sure the queue isnt still growing
        if( force_flush_mode && data_ready > previous_queue_size ) {
          force_flush_mode = false;
        }
        
        boolean flush = force_flush_mode && SystemTime.getCurrentTime() - force_start_time > 1000;
        
        if( data_ready >= WRITE_CACHE_SIZE || flush ) {  //do the write
          int written;
          try {
          	written = outgoing_message_queue.deliverToTransport( WRITE_CACHE_SIZE );
            //System.out.println(ip+ ": ready=" +data_ready+ " written=" +written+ " ff=" +flush );
          }
          catch( Throwable t ) {
            System.out.println( "DEBUG: " + t.getMessage() );
            closeAll("Error while writing to " +ip+ ": " +t.getMessage(), true, true);
            return PEPeerControl.NO_SLEEP;
          }
          
          if( written < 0 ) {
            closeAll("Error while writing to " +ip+ ": End Of Stream" , true, true);
            return PEPeerControl.NO_SLEEP;
          }
          
          if( written > 0 ) {  //update stats
            force_flush_mode = false;
            allowed_to_write -= written;
            last_write_time = SystemTime.getCurrentTime();
            
          	stats.sent( written );
            manager.sent( written );
            used += written;
          }
                    
        }
        else if( !force_flush_mode ) { //not enough data for a full write, start flush
          force_flush_mode = true;
          force_start_time = SystemTime.getCurrentTime();
          previous_queue_size = data_ready;
        }
      }
    }
    else { //nothing in message queue, update keep-alive
      if( last_write_time == 0 ) last_write_time = SystemTime.getCurrentTime();
      if( SystemTime.getCurrentTime() - last_write_time > 2*60*1000 ) {
        sendKeepAlive();
      }
    }
        
    return PEPeerControl.NO_SLEEP;
  }
  
  
  private int max(int a, int b) {  return a > b ? a : b;  }

  
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
  
  public boolean[] getAvailable() {  return available;  }
  public boolean isChoked() {  return choked;  }
  public boolean isChoking() {  return choking;  }
  public boolean isInterested() {  return interested;  }
  public boolean isInteresting() {  return interesting;  }
  public boolean isSeed() {  return seed;  }
  public boolean isSnubbed() {  return snubbed;  }

  public void setChoked(boolean b) {  choked = b;  }
  public void setChoking(boolean b) {  choking = b;  }
  public void setInterested(boolean b) {  interested = b;  }
  public void setInteresting(boolean b) {  interesting = b;  }
  public void setSeed(boolean b) {  seed = b;  }
  public void setSnubbed(boolean b) {  snubbed = b;  }

  //abstract methods
  protected abstract void startConnection();
  protected abstract void closeConnection();
  protected abstract boolean completeConnection() throws IOException;
  //TODO: remove
  protected abstract int readData( DirectByteBuffer	buffer ) throws IOException;
  protected abstract int writeData( DirectByteBuffer	buffer ) throws IOException;
  
  public void hasSentABadChunk() {  nbBadChunks++;  }
  public int getNbBadChunks() {  return nbBadChunks;  }

  public void setUploadHint(int spreadTime) {  spreadTimeHint = spreadTime;  }
  public int getUploadHint() {  return spreadTimeHint;  }
  public void setUniqueAnnounce(int uniquePiece) {  this.uniquePiece = uniquePiece;  }
  public int getUniqueAnnounce() {  return this.uniquePiece;  }

  //TODO: remove when refactoring speedlimiter
  public int getAllowed() {  return allowed;  }
  public int getMaxUpload() {  return maxUpload;  }
  public int getLimit() {  return limit;  }
  public void setLimit(int newLimit) {  limit = newLimit;  }
  public void addLimitIfNotZero(int addToLimit) {
    if( limit != 0 ) limit += addToLimit;
  }

  public int getReadSleepTime() { return readSleepTime; }
  public int getWriteSleepTime() { return writeSleepTime; }
  public void setReadSleepTime(int time) { readSleepTime = time; }
  public void setWriteSleepTime(int time) { writeSleepTime = time; }
  public long getLastReadTime() { return lastReadTime; }
  public long getLastWriteTime() { return lastWriteTime; }
  public void setLastReadTime(long time) { lastReadTime = time; }
  public void setLastWriteTime(long time) { lastWriteTime = time; }

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
      //cancel any unsent requests in the queue
      int[] type = { BTProtocolMessage.BT_REQUEST };
      outgoing_message_queue.removeMessagesOfType( type );
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
}
