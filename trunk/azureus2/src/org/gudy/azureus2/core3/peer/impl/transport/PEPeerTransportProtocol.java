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
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Arrays;

import org.gudy.azureus2.core3.util.*;

import org.gudy.azureus2.core3.disk.DiskManagerDataQueueItem;
import org.gudy.azureus2.core3.disk.DiskManagerRequest;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.peer.impl.*;
import org.gudy.azureus2.core3.peer.util.*;
import org.gudy.azureus2.core3.config.*;


/**
 * @author Olivier
 *
 */
public abstract class 
PEPeerTransportProtocol
	implements PEPeerTransport
{
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
	private int port;
	private int hashcode;
	
	
	protected PEPeerStatsImpl stats;

	protected boolean choked;
	protected boolean interested;
	private List requested;

	protected boolean choking;
	protected boolean interesting;

	protected boolean snubbed;

	protected boolean[] available;
	protected boolean seed;

    private HashMap data;

		//The reference to the current ByteBuffer used for reading on the socket.
	
	private DirectByteBuffer readBuffer;
	
		//The Buffer for reading the length of the messages
		
	private DirectByteBuffer lengthBuffer;
	
		//A flag to indicate if we're reading the length or the message
		
	private boolean readingLength;
	
		//The reference tp the current ByteBuffer used to write on the socket
		
	private DirectByteBuffer writeBuffer;
	
		//A flag to indicate if we're sending protocol messages or data
		
	private boolean writeData;
	private int allowed;
	private int used;
  private int limit = 0;
  private int loopFactor;

		//The keepAlive counter
		
	private int keepAlive;

		//The Queue for protocol messages
		
	private List protocolQueue = new ArrayList();

		//The Queue for data messages
		
	private List dataQueue = new ArrayList();
	private boolean incoming;
	private volatile boolean closing;
	private PEPeerTransportProtocolState currentState;

		//The maxUpload ...
		
	int maxUpload = 1024 * 1024;
  
  private static final int CACHE_SIZE = 1460;
  private DirectByteBuffer cache_buffer;
  private boolean force_flush = true;

		//The client
		
	String client = "";

		//Reader / Writer Loop
		
	private int processLoop;
  
		//Number of connections made since creation
		
	private int nbConnections;
  
		//Flag to indicate if the connection is in a stable enough state to send a request.
		//Used to reduce discarded pieces due to request / choke / unchoke / re-request , and both in fact taken into account.
		
	private boolean readyToRequest;
  
		//Flag to determine when a choke message has really been sent
		
	private boolean waitingChokeToBeSent;
	
	//Number of bad chunks received from this peer
	private int nbBadChunks;
	
	//When superSeeding, number of unique piece announced
	private int uniquePiece;
	
	//Spread time (0 secs , fake default)
	private int spreadTimeHint = 0 * 1000;

	public final static int componentID = 1;
	public final static int evtProtocol = 0;
  
  private int readSleepTime;
  private int writeSleepTime;
  private long lastReadTime;
  private long lastWriteTime;
  
  private boolean identityAdded = false;  //needed so we don't remove id's in closeAll() on duplicate connection attempts
  
	
		// Protocol Info
		
	public final static int evtLifeCycle = 1;
	
		// PeerConnection Life Cycle
		
	public final static int evtErrors = 2;
	
		// PeerConnection Life Cycle
		
	private final static String PROTOCOL = "BitTorrent protocol";

 
  /*
	 * This object constructors will let the PeerConnection partially created,
	 * but hopefully will let us gain some memory for peers not going to be
	 * accepted.
	 */

  /**
   * The Default Contructor for outgoing connections.
   * @param manager the manager that will handle this PeerConnection
   * @param table the graphical table in which this PeerConnection should display its info
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
			
			LGLogger.log(componentID, evtLifeCycle, LGLogger.RECEIVED, "Creating incoming connection from " + ip + " : " + port);
			
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
  protected void allocateAll() {
  	allocateAllSupport();
    
    cache_buffer = DirectByteBufferPool.getBuffer( CACHE_SIZE );

  	this.closing = false;
  	//TODO
  	this.lengthBuffer = DirectByteBufferPool.getBuffer( 4 );

  	this.allowed = 0;
  	this.used = 0;
  	this.loopFactor = 0;
  }

  protected void handShake(
  	byte[]		data_already_read ) 
  {
	try {
	  byte[] protocol = PROTOCOL.getBytes();
	  byte[] reserved = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 };
	  byte[] hash = manager.getHash();
	  byte[] myPeerId = manager.getPeerId();
//  TODO
    DirectByteBuffer bufferHandshakeS = DirectByteBufferPool.getBuffer( 68 );
	  bufferHandshakeS.buff.put((byte) PROTOCOL.length()).put(protocol).put(reserved).put(hash).put(myPeerId);

	  bufferHandshakeS.buff.position(0);
	  bufferHandshakeS.buff.limit(68);
	  sendProtocol(bufferHandshakeS);
	  readBuffer = DirectByteBufferPool.getBuffer(68);
	  if (readBuffer == null) {
		 closeAll(ip + " : PeerSocket::handShake:: readBuffer null", true, false);
		 return;
	  }
	  readBuffer.buff.limit(68);
	  readBuffer.buff.position(0);
	  if ( data_already_read != null ){
	  	readBuffer.buff.put( data_already_read );
	  }
	}
	catch (Exception e) {
	  closeAll(ip + " : Exception in handshake : " + e, true, false);
	}
  }

  protected void handleHandShakeResponse() {
	readBuffer.buff.position(0);
	//Now test for data...

   byte b;
	if ((b = readBuffer.buff.get()) != (byte) PROTOCOL.length()) {
	   closeAll(ip + " has sent handshake, but handshake starts with wrong byte : " + b,true, true);
	   return;
	}

	byte[] protocol = PROTOCOL.getBytes();
	if (readBuffer.buff.remaining() < protocol.length) {
	   closeAll(ip + " has sent handshake, but handshake is of wrong size : " + readBuffer.buff.remaining(),true, true);
	   return;
	}
	else {
	   readBuffer.buff.get(protocol);
	   if (!(new String(protocol)).equals(PROTOCOL)) {
		  closeAll(ip + " has sent handshake, but protocol is wrong : " + new String(protocol),true, false);
		  return;
	   }
	}

	byte[] reserved = new byte[8];
	if (readBuffer.buff.remaining() < reserved.length) {
	   closeAll(ip + " has sent handshake, but handshake is of wrong size(2) : " + readBuffer.buff.remaining(),true, true);
	   return;
	}
	else readBuffer.buff.get(reserved);
	//Ignores reserved bytes


	byte[] hash = manager.getHash();
	byte[] otherHash = new byte[20];
	if (readBuffer.buff.remaining() < otherHash.length) {
	   closeAll(ip + " has sent handshake, but handshake is of wrong size(3) : " + readBuffer.buff.remaining(),true, true);
	   return;
	}
	else {
	   readBuffer.buff.get(otherHash);
	   for (int i = 0; i < 20; i++) {
		  if (otherHash[i] != hash[i]) {
			 closeAll(ip + " has sent handshake, but infohash is wrong",true, false);
			 return;
		  }
	   }
	}

    
	byte[] otherPeerId = new byte[20];
	if (readBuffer.buff.remaining() < otherPeerId.length) {
	   closeAll(ip + " has sent handshake, but handshake is of wrong size(4) : " + readBuffer.buff.remaining(),true, true);
	   return;
	}
	else readBuffer.buff.get(otherPeerId);

  this.id = otherPeerId;
  
  
  
  //make sure we are not connected to ourselves
  if ( Arrays.equals( manager.getPeerId(), otherPeerId )) {
    closeAll("OOPS, peerID matches myself", false, false);
    return;
  }
  

  
  //make sure we are not already connected to this peer
  boolean sameIdentity = PeerIdentityManager.containsIdentity( otherHash, otherPeerId );
  boolean sameIP = false;
  if (! COConfigurationManager.getBooleanParameter("Allow Same IP Peers", false)) {
    if ( PeerIdentityManager.containsIPAddress( otherHash, ip )) {
      sameIP = true;
    }
  }
  if ( sameIdentity ) {
    closeAll(ip + " exchanged handshake, but peer matches pre-existing identity", false, false);
    return;
  }
  if ( sameIP ) {
    closeAll(ip + " exchanged handshake, but peer matches pre-existing IP address", false, false);
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

	sendBitField();
	readMessage(readBuffer);
	manager.peerAdded(this);
	currentState = new StateTransfering();
  }

  protected void readMessage(DirectByteBuffer buffer) {
  	lengthBuffer.buff.position(0);
    if ( buffer != null ) {
    	buffer.buff.position(0);
    }
  	readingLength = true;
    readBuffer = buffer;
  }

  protected void sendProtocol(DirectByteBuffer buffer) {
    synchronized( protocolQueue ) {
    	protocolQueue.add(buffer);
    }
  }

  protected void sendData(DiskManagerRequest request) {
    synchronized( dataQueue ) {
    	dataQueue.add(manager.createDiskManagerDataQueueItem(request));
    }
  }

  
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
  	
  	//Close the socket
  	closeConnection();
  	
  	//release the read Buffer
  	if (readBuffer != null) {
  		readBuffer.returnToPool();
  		readBuffer = null;
  	}
    
    if ( cache_buffer != null ) {
      cache_buffer.returnToPool();
      cache_buffer = null;
    }
    
    if ( lengthBuffer != null ) {
      lengthBuffer.returnToPool();
      lengthBuffer = null;
    }
  	
  	//release the write Buffer
  	if (writeBuffer != null) {      
  		if (writeData) {
  			PEPeerTransportSpeedLimiter.getLimiter().removeUploader(this);
  		}
      writeBuffer.returnToPool();
      writeBuffer = null;
  	}
  	
  	synchronized( dataQueue ) {
  		//release all buffers in dataQueue
      for (int i=0; i < dataQueue.size(); i++) {
  			DiskManagerDataQueueItem item = (DiskManagerDataQueueItem) dataQueue.remove(i);
  			if (item.isLoaded()) {
  				item.getBuffer().returnToPool();
  				item.setBuffer(null);
  			}
  			else if (item.isLoading()) {
  				manager.freeRequest(item);
  			}
  		}
  	}
    
    synchronized( protocolQueue ) {
      for (int i=0; i < protocolQueue.size(); i++) {
        DirectByteBuffer dbb = (DirectByteBuffer)protocolQueue.remove( i );
        dbb.returnToPool();
      }
    }
  	
  	//remove identity
  	if ( this.id != null && identityAdded ) {
  		PeerIdentityManager.removeIdentity( manager.getHash(), this.id );
  	}
  	
  	//Send removed event ...
  	manager.peerRemoved(this);
  	
  	//Send a logger event
  	LGLogger.log(componentID, evtLifeCycle, LGLogger.INFORMATION, "Connection Ended with " + ip + " : " + port + " ( " + client + " )");
  	
  	if ( (attemptReconnect)
		  && (currentState != null)
		  && (currentState.getState() == TRANSFERING || currentState.getState() == HANDSHAKING)
			&& (incoming == false)
			&& (nbConnections < 3)) {
      
  		LGLogger.log(componentID, evtLifeCycle, LGLogger.INFORMATION, "Attempting to reconnect with " + ip + " : " + port + " ( " + client + " )");
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
      LGLogger.log(componentID, evtLifeCycle, LGLogger.SENT, "Creating outgoing connection to " + ip + " : " + port);

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
  			closeAll("Error in StateConnecting: (" + ip + " : " + port + " ) : " + e, false, false);
  			return PEPeerControl.NO_SLEEP;
  		}
  	}

  	public int getState() {
  		return CONNECTING;
  	}
  }
		
  private class StateHandshaking implements PEPeerTransportProtocolState {
		public synchronized int process() {
			if (readBuffer.buff.hasRemaining()) {
				try {
					int read = readData(readBuffer);
          if (read == 0) {
            return PEPeerControl.DATA_EXPECTED_SLEEP;
          }
          else if (read < 0) {
						throw new IOException("End of Stream Reached");
          }
				} catch (IOException e) {
					closeAll(ip + " : StateHandshaking:: " + e, true, false);
					return 0;
				}
			}
      
			if (readBuffer.buff.remaining() == 0) {
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
      if (lengthBuffer.buff.hasRemaining() ) {          
        try {
          int read = readData(lengthBuffer);
          
          if (read == 0) {
            //If there's nothing pending on the socket, then
            //we can quite safely send a request while we wait
            if(lengthBuffer.buff.remaining() == 4) {
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
          closeAll(ip + " : StateTransfering::" + e.getMessage()+ " (reading length)",true, true);
          return PEPeerControl.NO_SLEEP;
        }
			}

			if (!lengthBuffer.buff.hasRemaining()) {
				int length = lengthBuffer.buff.getInt(0);
		  
				if(length < 0) {
					closeAll(ip + " : length negative : " + length,true, true);
					return PEPeerControl.NO_SLEEP;
				}
      
				if(length >= DirectByteBufferPool.MAX_SIZE) {
					closeAll(ip + " : length greater than max size : " + length,true, true);
					return PEPeerControl.NO_SLEEP;
				}
        
				if (length > 0) {
					//return old readBuffer to pool if it's too small
					if(readBuffer.buff.capacity() < length) {
						readBuffer.returnToPool();
						readBuffer = DirectByteBufferPool.getBuffer(length);
            if (readBuffer == null) { closeAll(ip + " readBuffer null",true, false); }
					}
      			
					readBuffer.buff.position(0);
					readBuffer.buff.limit(length);
          
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
        if (readBuffer.buff.limit() > 13) {
	  				stats.received(read);
        }
	  		}
	  	}
	  	catch (IOException e) {
	  		closeAll(ip + " : StateTransfering::End of Stream Reached (reading data)",true, true);
	  		return PEPeerControl.NO_SLEEP;
	  	}
    
	  	if (!readBuffer.buff.hasRemaining()) {
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
  		closeAll(ip + " : Exception in process : " + e,true, false);
      return PEPeerControl.NO_SLEEP;
  	}
  }
  
  public int processWrite() {
    try {
      loopFactor++;
      processLoop = 0;
      return write();
    }
    catch (Exception e) {
      e.printStackTrace();
      closeAll(ip + " : Exception in process : " + e,true, false);
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
	buffer.buff.position(0);
	int pieceNumber, pieceOffset, pieceLength;
	byte cmd = buffer.buff.get();
	switch (cmd) {
	  case BT_CHOKED :
			if (buffer.buff.limit() != 1) {
			  closeAll(ip + " choking received, but message of wrong size : " + buffer.buff.limit(),true, true);
			  break;
			}
			if ( logging_is_on ) LGLogger.log(componentID, evtProtocol, LGLogger.RECEIVED, ip + " is choking you");
			choked = true;
			cancelRequests();
			readMessage(buffer);
			break;
	  case BT_UNCHOKED :
			if (buffer.buff.limit() != 1) {
			  closeAll(ip + " unchoking received, but message of wrong size : " + buffer.buff.limit(),true, true);
			  break;
			}
			if ( logging_is_on ) LGLogger.log(componentID, evtProtocol, LGLogger.RECEIVED, ip + " is unchoking you");
			choked = false;
			readMessage(buffer);
			break;
	  case BT_INTERESTED :
			if (buffer.buff.limit() != 1) {
			  closeAll(ip + " interested received, but message of wrong size : " + buffer.buff.limit(),true, true);
			  break;
			}
			if ( logging_is_on ) LGLogger.log(componentID, evtProtocol, LGLogger.RECEIVED, ip + " is interested");
			interesting = true;
			readMessage(buffer);
			break;
	  case BT_UNINTERESTED :
			if (buffer.buff.limit() != 1) {
			  closeAll(ip + " uninterested received, but message of wrong size : " + buffer.buff.limit(),true, true);
			  break;
			}
			if ( logging_is_on ) LGLogger.log(componentID, evtProtocol, LGLogger.RECEIVED, ip + " is not interested");
			interesting = false;
			readMessage(buffer);
			break;
	  case BT_HAVE :
			if (buffer.buff.limit() != 5) {
			  closeAll(ip + " have received, but message of wrong size : " + buffer.buff.limit(),true, true);
			  break;
			}
			pieceNumber = buffer.buff.getInt();
			if ( logging_is_on ) LGLogger.log(componentID, evtProtocol, LGLogger.RECEIVED, ip + " has " + pieceNumber);
			have(pieceNumber);
			readMessage(buffer);
			break;
	  case BT_BITFIELD :
	  	if ( logging_is_on ) LGLogger.log(componentID, evtProtocol, LGLogger.RECEIVED, ip + " has sent BitField");
			setBitField(buffer);
			checkInterested();
			checkSeed();
			readMessage(buffer);
			break;
	  case BT_REQUEST :
			if (buffer.buff.limit() != 13) {
			  closeAll(ip + " request received, but message of wrong size : " + buffer.buff.limit(),true, true);
			  break;
			}
			pieceNumber = buffer.buff.getInt();
			pieceOffset = buffer.buff.getInt();
			pieceLength = buffer.buff.getInt();
			
			if ( logging_is_on ){
				LGLogger.log(
					componentID,
					evtProtocol,
					LGLogger.RECEIVED,
					ip + " has requested #" + pieceNumber + ":" + pieceOffset + "->" + (pieceOffset + pieceLength));
			}
			
			if (manager.checkBlock(pieceNumber, pieceOffset, pieceLength)) {
			  if(!choking) {
			    sendData(manager.createDiskManagerRequest(pieceNumber, pieceOffset, pieceLength));
			  } else {
			    LGLogger.log(componentID, evtProtocol, LGLogger.RECEIVED,ip
          + " ("
          + client
          + ")"
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
			  closeAll(ip
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
			if (buffer.buff.limit() < 9) {
			   closeAll(ip + " piece block received, but message of wrong size : " + buffer.buff.limit(),true, true);
			   break;
			}
			pieceNumber = buffer.buff.getInt();
			pieceOffset = buffer.buff.getInt();
			pieceLength = buffer.buff.limit() - buffer.buff.position();
			if ( logging_is_on ){
				LGLogger.log(
					componentID,
					evtProtocol,
					LGLogger.RECEIVED,
					ip + " has sent #" + pieceNumber + ":" + pieceOffset + "->" + (pieceOffset + pieceLength));
			}
			DiskManagerRequest request = manager.createDiskManagerRequest(pieceNumber, pieceOffset, pieceLength);
			if (alreadyRequested(request) && manager.checkBlock(pieceNumber, pieceOffset, buffer)) {
			  removeRequest( request );
			  manager.received(pieceLength);
			  setSnubbed(false);
			  reSetRequestsTime();
			  manager.writeBlock(pieceNumber, pieceOffset, buffer,this);
        buffer = DirectByteBufferPool.getBuffer( buffer.buff.limit() );
        if (buffer == null) { closeAll(ip + " BT_PIECE buffer null",true, false); }
			  readMessage(buffer);      
			}
			else {
        String msg = ip + " [" + client + "]" + " has sent #" + pieceNumber + ":"
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
			if (buffer.buff.limit() != 13) {
			  closeAll(ip + " cancel received, but message of wrong size : " + buffer.buff.limit(),true, true);
			  break;
			}
			pieceNumber = buffer.buff.getInt();
			pieceOffset = buffer.buff.getInt();
			pieceLength = buffer.buff.getInt();
			if ( logging_is_on ){
				LGLogger.log(
					componentID,
					evtProtocol,
					LGLogger.RECEIVED,
					ip + " has canceled #" + pieceNumber + ":" + pieceOffset + "->" + (pieceOffset + pieceLength));
			}
			removeRequestFromQueue(manager.createDiskManagerRequest(pieceNumber, pieceOffset, pieceLength));
			readMessage(buffer);
			break;
	  default:
       Debug.out(ip + " [" + client + "] has sent an unknown protocol message id: " + cmd);
	    closeAll(ip + " has sent a wrong message " + cmd,true, true);
	}
  }

  private void have(int pieceNumber) {
	if ((pieceNumber >= available.length) || (pieceNumber < 0)) {
	   closeAll(ip + " gave invalid pieceNumber:" + pieceNumber,true, true);
      return;
	}
	else {    
	  available[pieceNumber] = true;
	  stats.haveNewPiece();
	  manager.haveNewPiece();
	  manager.havePiece(pieceNumber, this);
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

  /*
   *  (non-Javadoc)
   * @see org.gudy.azureus2.core3.peer.impl.PEPeerTransport#request(int, int, int)
   */
  public boolean request(int pieceNumber, int pieceOffset, int pieceLength) {
	if (getState() != TRANSFERING) {
    manager.requestCanceled(manager.createDiskManagerRequest(pieceNumber, pieceOffset, pieceLength));
	  return false;
  }	
	DiskManagerRequest request = manager.createDiskManagerRequest(pieceNumber, pieceOffset, pieceLength);
	if (!alreadyRequested(request)) {
		LGLogger.log(
		  componentID,
		  evtProtocol,
		  LGLogger.SENT,
		  ip + " is asked for #" + pieceNumber + ":" + pieceOffset + "->" + (pieceOffset + pieceLength));
		addRequest( request );
//  TODO
    DirectByteBuffer buffer = DirectByteBufferPool.getBuffer( 17 );
		buffer.buff.putInt(13);
		buffer.buff.put(BT_REQUEST);
		buffer.buff.putInt(pieceNumber);
		buffer.buff.putInt(pieceOffset);
		buffer.buff.putInt(pieceLength);
		buffer.buff.position(0);
		buffer.buff.limit(17);
		sendProtocol(buffer);
    return true;
	}
  return false;
  }

  public void sendCancel(DiskManagerRequest request) {
	if (getState() != TRANSFERING)
	  return;	
	if (alreadyRequested(request)) {
	  LGLogger.log(
	      componentID,
	      evtProtocol,
				LGLogger.SENT,
				ip
				+ " is canceled for #"
				+ request.getPieceNumber()
				+ "::"
				+ request.getOffset()
				+ "->"
				+ (request.getOffset() + request.getLength()));
	  removeRequest(request);
//  TODO
    DirectByteBuffer buffer = DirectByteBufferPool.getBuffer( 17 );
	  buffer.buff.putInt(13);
	  buffer.buff.put(BT_CANCEL);
	  buffer.buff.putInt(request.getPieceNumber());
	  buffer.buff.putInt(request.getOffset());
	  buffer.buff.putInt(request.getLength());
	  buffer.buff.position(0);
	  buffer.buff.limit(17);
	  sendProtocol(buffer);
	}
  }

  public void sendHave(int pieceNumber) {
	if (getState() != TRANSFERING)
	  return;
	LGLogger.log(componentID, evtProtocol, LGLogger.SENT, ip + " is notified you have " + pieceNumber);
  
//TODO
	DirectByteBuffer buffer = DirectByteBufferPool.getBuffer( 9 );
	buffer.buff.putInt(5);
	buffer.buff.put(BT_HAVE);
	buffer.buff.putInt(pieceNumber);
	buffer.buff.position(0);
	buffer.buff.limit(9);
	sendProtocol(buffer);
	checkInterested();
  }

  public void sendChoke() {
	if (getState() != TRANSFERING)
	  return;
	LGLogger.log(componentID, evtProtocol, LGLogger.SENT, ip + " is choked");
	choking = true;
	sendSimpleCommand(BT_CHOKED);
  }

  public void sendUnChoke() {
	if (getState() != TRANSFERING)
	  return;
	LGLogger.log(componentID, evtProtocol, LGLogger.SENT, ip + " is unchoked");
	choking = false;
	sendSimpleCommand(BT_UNCHOKED);
  }

  private void sendSimpleCommand(byte command) {
//  TODO
  DirectByteBuffer buffer = DirectByteBufferPool.getBuffer( 5 );
	buffer.buff.putInt(1);
	buffer.buff.put(command);
	buffer.buff.position(0);
	buffer.buff.limit(5);
	sendProtocol(buffer);
  }

  private void setBitField(DirectByteBuffer buffer) {
	byte[] dataf = new byte[(manager.getPiecesNumber() + 7) / 8];
   
	if (buffer.buff.remaining() < dataf.length) {
     LGLogger.log(componentID, evtProtocol, LGLogger.RECEIVED, ip + " has sent invalid BitField: too short");
	  return;
   }
   
	buffer.buff.get(dataf);
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
	  if (!myStatus[i] && available[i]) {
		newInterested = true;
		break;
	  }
	}

	if (newInterested && !interested) {
	  LGLogger.log(componentID, evtProtocol, LGLogger.SENT, ip + " is interesting");
	  sendSimpleCommand(BT_INTERESTED);
	}
	else if (!newInterested && interested) {
	  LGLogger.log(componentID, evtProtocol, LGLogger.SENT, ip + " is not interesting");
	  sendSimpleCommand(BT_UNINTERESTED);
	}
	interested = newInterested;
  }

  /**
   * Checks interested given a new piece received
   * @param pieceNumber the piece number that has been received
   */
  private void checkInterested(int pieceNumber) {
	boolean[] myStatus = manager.getPiecesStatus();
	boolean newInterested = !myStatus[pieceNumber];
	if (newInterested && !interested) {
	  LGLogger.log(componentID, evtProtocol, LGLogger.SENT, ip + " is interesting");
	  sendSimpleCommand(BT_INTERESTED);
	}
	else if (!newInterested && interested) {
	  LGLogger.log(componentID, evtProtocol, LGLogger.SENT, ip + " is not interesting");
	  sendSimpleCommand(BT_UNINTERESTED);
	}
	interested = newInterested;
  }

  /**
   * Private method to send the bitfield.
   * The bitfield will only be sent if there is at least one piece available.
   *
   */
  private void sendBitField() {
  //In case we're in super seed mode, we don't send our bitfield
  if(manager.isSuperSeedMode())
    return;
  
  int size = 5 + (manager.getPiecesNumber() + 7) / 8;
  ByteBuffer buffer = ByteBuffer.allocate( size );
	buffer.putInt(buffer.capacity() - 4);
	buffer.put(BT_BITFIELD);
	boolean atLeastOne = false;
	boolean[] myStatus = manager.getPiecesStatus();
	int bToSend = 0;
	int i = 0;
	for (; i < myStatus.length; i++) {
	  if ((i % 8) == 0)
		bToSend = 0;
	  bToSend = bToSend << 1;
	  if (myStatus[i]) {
		bToSend += 1;
		atLeastOne = true;
	  }
	  if ((i % 8) == 7)
		buffer.put((byte) bToSend);
	}
	if ((i % 8) != 0) {
	  bToSend = bToSend << (8 - (i % 8));
	  buffer.put((byte) bToSend);
	}

	buffer.position(0);
	if (atLeastOne) {
	  buffer.limit(buffer.capacity());
	  sendProtocol(new DirectByteBuffer(buffer));
	  LGLogger.log(componentID, evtProtocol, LGLogger.SENT, ip + " is sent your bitfield");
	}
  }

  private void removeRequestFromQueue(DiskManagerRequest request) {
    synchronized( dataQueue ) {
    	for (int i = 0; i < dataQueue.size(); i++) {
    		DiskManagerDataQueueItem item = (DiskManagerDataQueueItem) dataQueue.get(i);
    		if (item.getRequest().equals(request)) {
    			if (item.isLoaded()) {
    				item.getBuffer().returnToPool();
    				item.setBuffer(null);
    			}
    			if (item.isLoading()) {
    				manager.freeRequest(item);
    			}
    			dataQueue.remove(item);
    			i--;
    		}
    	}
    }
  }

 
  protected synchronized int write() {
  	if ( currentState.getState() != HANDSHAKING && currentState.getState() != TRANSFERING )
  		return PEPeerControl.WAITING_SLEEP;       
  	if(++processLoop > 10)
  		return PEPeerControl.NO_SLEEP;
      
  	//If we are already sending something, we simply continue
  	if (writeBuffer != null && cache_buffer != null ) {
  		try {
  			int realLimit = writeBuffer.buff.limit();
  			int limit = realLimit;
  			int uploadAllowed = 0;
  			if (writeData && PEPeerTransportSpeedLimiter.getLimiter().isLimited(this)) {
  				if ((loopFactor % 10) == 0) {
  					allowed = PEPeerTransportSpeedLimiter.getLimiter().getLimitPer100ms(this);
  					used = 0;
  				}
  				uploadAllowed = allowed - used;
  				limit = writeBuffer.buff.position() + uploadAllowed;
  				if ((limit > realLimit) || (limit < 0))
  					limit = realLimit;
  			}
  			
        int written = 0;
        writeBuffer.buff.limit( limit );
          
        int to_write = writeBuffer.buff.remaining();
        if ( to_write > cache_buffer.buff.remaining() ) {
          to_write = cache_buffer.buff.remaining();
          writeBuffer.buff.limit( writeBuffer.buff.position() + to_write );
        }
        cache_buffer.buff.put( writeBuffer.buff );
        written = to_write;
          
        if ( cache_buffer.buff.position() == CACHE_SIZE || force_flush ) {
          cache_buffer.buff.flip();
          int wrote = writeData( cache_buffer );
          //System.out.println(wrote+ " " +force_flush);
          cache_buffer.buff.clear();
          cache_buffer.buff.limit( CACHE_SIZE );
          force_flush = false;
        }
        
        //written = writeData( writeBuffer );
        
  			
  			if (written < 0)
  				throw new IOException("End of Stream Reached");
  			writeBuffer.buff.limit(realLimit);
  			
  			if (writeData && written > 0 ) {
  				stats.sent(written);
  				manager.sent(written);
  				if (PEPeerTransportSpeedLimiter.getLimiter().isLimited(this)) {
  					used += written;
  					if ((loopFactor % 10) == 9) {
  						if (used >= (95 * allowed) / 100)
  							maxUpload = max(110 * allowed / 100, 50);
  						if (used < (90 * allowed) / 100)
  							maxUpload = max((100 * used) / 100, 10);
  					}
  				}
  			}
  		}
  		catch (IOException e) {
  			closeAll("Error while writing to " + ip +" : " + e,true, true);
  			return PEPeerControl.NO_SLEEP;
  		}
  		
  		//If we have finished sending this buffer
  		if (!writeBuffer.buff.hasRemaining()) {
  			//If we were sending data, we must free the writeBuffer
  			if (writeData) {
  				PEPeerTransportSpeedLimiter.getLimiter().removeUploader(this);
  			}
  			//We set it to null
        writeBuffer.returnToPool();
  			writeBuffer = null;
  		}
  	}
  	
  	if (writeBuffer == null) {
      
  		synchronized( protocolQueue ) {
      	//So the ByteBuffer is null ... let's find out if there's any data in the protocol queue
      	if (protocolQueue.size() != 0) {
      		//Correct in 1st approximation (a choke message queued (if any) will to be send soon after this)
      		waitingChokeToBeSent = false;
      		//Assign the current buffer ...
      		keepAlive = 0;
      		writeBuffer = (DirectByteBuffer) protocolQueue.remove(0);
  			
      		if (writeBuffer == null){
      			closeAll(ip + " : Empty write Buffer on protocol message !!!",true, false);
      			return PEPeerControl.NO_SLEEP;
      		}
  			
      		//check to make sure we're sending a proper message length
      		if (!verifyLength(writeBuffer)) {
      			closeAll("OOPS, we're sending a bad protocol message length !!!", true, true);
      			return PEPeerControl.NO_SLEEP;
      		}
  			
      		writeBuffer.buff.position(0);
  			  writeData = false;
  			  //and loop
  			  write();
  			  return PEPeerControl.NO_SLEEP;
      	}
  		}
  		
      synchronized( dataQueue ) {
      	//Check if there's any data to send from the dataQueue
      	if (dataQueue.size() != 0) {
      		DiskManagerDataQueueItem item = (DiskManagerDataQueueItem) dataQueue.get(0);
      		if (!choking) {
      			if (!item.isLoading() && !choking) {
      				manager.enqueueReadRequest(item);
      			}
      			if (item.isLoaded()) {
      				dataQueue.remove(0);
      				if (dataQueue.size() != 0) {
      					DiskManagerDataQueueItem itemNext = (DiskManagerDataQueueItem) dataQueue.get(0);
      					if (!itemNext.isLoading()) {
      						manager.enqueueReadRequest(itemNext);
      					}
      				}
      				DiskManagerRequest request = item.getRequest();
      				LGLogger.log(
      						componentID,
									evtProtocol,
									LGLogger.SENT,
									ip
									+ " is being sent #"
									+ request.getPieceNumber()
									+ ":"
									+ request.getOffset()
									+ "->"
									+ (request.getOffset() + request.getLength()));
      				// Assign the current buffer ...
      				keepAlive = 0;
      				writeBuffer = item.getBuffer();
      				
      				//check to make sure we're sending a proper message length
      				if (!verifyLength(writeBuffer)) {
      					closeAll("OOPS, we're sending a bad data message length !!!", true, true);
      					return PEPeerControl.NO_SLEEP;
      				}
      				
      				writeBuffer.buff.position(0);
      				writeData = true;
      				PEPeerTransportSpeedLimiter.getLimiter().addUploader(this);
      				// and loop
      				write();
      				return PEPeerControl.NO_SLEEP;
      			}
      		}
      		else {
      			//We are choking the peer so ...
      			if (!item.isLoading()) {
      				dataQueue.remove(item);
      			}
      			if (item.isLoaded()) {
      				dataQueue.remove(item);
      				item.getBuffer().returnToPool();
      				item.setBuffer(null);
      			}
      			return PEPeerControl.NO_SLEEP;
      		}
      	}
      }
      
      synchronized( protocolQueue ) {
        synchronized( dataQueue ) {
        	if ((protocolQueue.size() == 0) && (dataQueue.size() == 0)) {
            force_flush = true;
        		keepAlive++;
        		if (keepAlive == 50 * 60 * 3) {
        			keepAlive = 0;
        			sendKeepAlive();
        		}
        	}
        }
      }
    
  	}
    
    return PEPeerControl.NO_SLEEP;
  }
  
  
  /**
   * Verifies that the buffer length is correct.
   * Returns true if the buffer length is OK.
   */
  private boolean verifyLength(DirectByteBuffer buffer) {
    //check for a handshake message
    if (buffer.buff.get(0) == (byte)PROTOCOL.length()) {
      //make sure the length is correct for a handshake
      if (buffer.buff.limit() != 68) {
        //make sure it isn't a normal message
        int length =  buffer.buff.getInt(0);
        if (length != buffer.buff.limit() - 4) {
          Debug.out("PROTOCOL: givenLength=" + length + " realLength=" + buffer.buff.limit());
          return false;
        }
        return true; 
      }
      return true;
    }
    
    int length =  buffer.buff.getInt(0);
    if (length != buffer.buff.limit() - 4) {
      Debug.out("givenLength=" + length + " realLength=" + buffer.buff.limit());
      return false;
    }
    return true;
  }

  private int max(int a, int b) {
	return a > b ? a : b;
  }

  private void sendKeepAlive() {
//  TODO
  	DirectByteBuffer buffer = DirectByteBufferPool.getBuffer( 4 );
  	buffer.buff.putInt(0);
  	buffer.buff.limit(4);
  	buffer.buff.position(0);
  	sendProtocol( buffer );
  }

  public int getMaxUpload() {
	return maxUpload;
  }

  public DiskManagerDataQueueItem getNextRequest() {
    synchronized( dataQueue ) {
    	if (dataQueue.size() != 0)
    		return (DiskManagerDataQueueItem) dataQueue.get(0);
    	return null;
    }
  }

  /**
   * @return
   */
  public boolean isIncoming() {
	return incoming;
  }

  public int getDownloadPriority() {
	return manager.getDownloadPriority();
  }

  /**
   * @return
   */
  public String getClient() {
	return client;
  }

  public boolean isOptimisticUnchoke() {
	return manager.isOptimisticUnchoke(this);
  }

  /**
   * @return
   */
  public boolean isReadyToRequest() {
	return readyToRequest;
  }

  /**
   * @return
   */
  public boolean isWaitingChokeToBeSent() {
	return waitingChokeToBeSent;
  }

  /**
   * @param waitingChokeToBeSent
   */
  public void setWaitingChokeToBeSent(boolean waitingChokeToBeSent) {
	this.waitingChokeToBeSent = waitingChokeToBeSent;
  }


  protected abstract void
  startConnection();

  protected abstract void
  closeConnection();
 	
  protected abstract boolean
  completeConnection()
	
	  throws IOException;

	
  protected abstract int
  readData(
	  DirectByteBuffer	buffer )
		
	  throws IOException;

  protected abstract int
  writeData(
		DirectByteBuffer	buffer )
		
		throws IOException;
  
  public void hasSentABadChunk() {
    nbBadChunks++;
  }
  
  public int getNbBadChunks() {
    return nbBadChunks;
  }
  
  public void setUploadHint(int spreadTime) {
    spreadTimeHint = spreadTime;
  }
  
  public int getUploadHint() {
    return spreadTimeHint;
  }
  
  public void setUniqueAnnounce(int uniquePiece) {
    this.uniquePiece = uniquePiece;
  }
  
  public int getUniqueAnnounce() {
    return this.uniquePiece;
  }

  public int getAllowed() {
    return allowed;
  }

  public int getLimit() {
    return limit;
  }
  public void setLimit(int newLimit) {
    limit = newLimit;
  }
  public void addLimitIfNotZero(int addToLimit) {
    if(limit != 0)
      limit += addToLimit;
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
 
	/**
	 * @return
	 */
	public byte[] getId() {
	  return id;
	}

	/**
	 * @return
	 */
	public String getIp() {
	  return ip;
	}

	/**
	 * @return
	 */
	public int getPort() {
	  return port;
	}
  
  
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
  
  public int hashCode() {
    return hashcode;
  }
  

	public PEPeerControl 
	getControl() {
	  return manager;
	}

	public PEPeerManager
	getManager()
	{
		return( manager );
	}
	
	
	protected void allocateAllSupport() {
		  seed = false;

		  choked = true;
		  interested = false;
		  requested = new ArrayList();

		  choking = true;
		  interesting = false;

		  available = new boolean[manager.getPiecesNumber()];
		  Arrays.fill(available, false);

		  stats = (PEPeerStatsImpl)manager.createPeerStats();
		}


		/**
		 * @return
		 */
		public boolean[] getAvailable() {
		  return available;
		}

		/**
		 * @return
		 */
		public boolean isChoked() {
		  return choked;
		}

		/**
		 * @return
		 */
		public boolean isChoking() {
		  return choking;
		}

		/**
		 * @return
		 */
		public boolean isInterested() {
		  return interested;
		}

		/**
		 * @return
		 */
		public boolean isInteresting() {
		  return interesting;
		}


		/**
		 * @return
		 */
		public boolean isSeed() {
		  return seed;
		}

		/**
		 * @return
		 */
		public boolean isSnubbed() {
		  return snubbed;
		}

		/**
		 * @return
		 */
		public PEPeerStats getStats() {
		  return stats;
		}

		/**
		 * @param b
		 */
		public void setChoked(boolean b) {
		  choked = b;
		}

		/**
		 * @param b
		 */
		public void setChoking(boolean b) {
		  choking = b;
		}

		/**
		 * @param b
		 */
		public void setInterested(boolean b) {
		  interested = b;
		}

		/**
		 * @param b
		 */
		public void setInteresting(boolean b) {
		  interesting = b;
		}

		/**
		 * @param b
		 */
		public void setSeed(boolean b) {
		  seed = b;
		}

		/**
		 * @param b
		 */
		public void setSnubbed(boolean b) {
		  snubbed = b;
		}
		
		protected void cancelRequests() {
			if (requested == null)
				return;
			synchronized (requested) {
				for (int i = requested.size() - 1; i >= 0; i--) {
					DiskManagerRequest request = (DiskManagerRequest) requested.remove(i);
					manager.requestCanceled(request);
				}
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
		
}
