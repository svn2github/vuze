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
import java.util.Vector;

import org.gudy.azureus2.core3.util.*;

import org.gudy.azureus2.core3.disk.DiskManagerDataQueueItem;
import org.gudy.azureus2.core3.disk.DiskManagerRequest;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.peer.impl.*;

/**
 * @author Olivier
 *
 */
public abstract class 
PEPeerTransportProtocol
	extends PEPeerConnectionImpl
	implements PEPeer, PEPeerTransport
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


		//The reference to the current ByteBuffer used for reading on the socket.
	
	private ByteBuffer readBuffer;
	
		//The Buffer for reading the length of the messages
		
	private ByteBuffer lengthBuffer;
	
		//A flag to indicate if we're reading the length or the message
		
	private boolean readingLength;
	
		//The reference tp the current ByteBuffer used to write on the socket
		
	private ByteBuffer writeBuffer;
	
		//A flag to indicate if we're sending protocol messages or data
		
	private boolean writeData;
	private int allowed;
	private int used;
	private int loopFactor;

		//The keepAlive counter
		
	private int keepAlive;

		//The Queue for protocol messages
		
	private List protocolQueue;

		//The Queue for data messages
		
	private List dataQueue;
	private boolean incoming;
	private boolean closing;
	private PEPeerTransportProtocolState currentState;

		//The maxUpload ...
		
	int maxUpload = 1024 * 1024;

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

	public final static int componentID = 1;
	public final static int evtProtocol = 0;
	
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
		super(manager, peerId, ip, port);
		
		if ( fake ){
		
			return;
		}
	
		incoming = incoming_connection;
		
		if ( incoming ){
			
			allocateAll();
			
			LGLogger.log(componentID, evtLifeCycle, LGLogger.INFORMATION, "Creating incoming connection from " + ip + " : " + port);
			
			handShake( data_already_read );
			
			currentState = new StateHandshaking();
					
		}else{
			
			nbConnections = 0;
			
			createConnection();
	  	}
	}

  
  protected void 
  createConnection()
  {
  this.nbConnections++;
  allocateAll();
  LGLogger.log(componentID, evtLifeCycle, LGLogger.INFORMATION, "Creating outgoing connection to " + ip + " : " + port);

  try {

	startConnection();
	
	this.currentState = new StateConnecting();
  }
  catch (Exception e) {
	closeAll("Exception while connecting : " + e,false);
  }
}


  /**
   * Private method that will finish fields allocation, once the handshaking is ok.
   * Hopefully, that will save some RAM.
   */
  protected void allocateAll() {

	super.allocateAll();

	this.closing = false;
	this.protocolQueue = new ArrayList();
	this.dataQueue = new ArrayList();
	this.lengthBuffer = ByteBuffer.allocate(4);

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

	  ByteBuffer bufferHandshakeS = ByteBuffer.allocate(68);
	  bufferHandshakeS.put((byte) PROTOCOL.length()).put(protocol).put(reserved).put(hash).put(myPeerId);

	  bufferHandshakeS.position(0);
	  bufferHandshakeS.limit(68);
	  sendProtocol(bufferHandshakeS);
	  readBuffer = ByteBufferPool.getInstance().getFreeBuffer(68);
	  if (readBuffer == null) {
		 closeAll(ip + " : PeerSocket::handShake:: readBuffer null", true);
		 return;
	  }
	  readBuffer.limit(68);
	  readBuffer.position(0);
	  if ( data_already_read != null ){
	  	readBuffer.put( data_already_read );
	  }
	}
	catch (Exception e) {
	  closeAll(ip + " : Exception in handshake : " + e, true);
	}
  }

  protected void handleHandShakeResponse() {
	readBuffer.position(0);
	//Now test for data...

   byte b;
	if ((b = readBuffer.get()) != (byte) PROTOCOL.length()) {
	   closeAll(ip + " has sent handshake, but handshake starts with wrong byte : " + b,true);
	   return;
	}

	byte[] protocol = PROTOCOL.getBytes();
	if (readBuffer.remaining() < protocol.length) {
	   closeAll(ip + " has sent handshake, but handshake is of wrong size : " + readBuffer.remaining(),true);
	   return;
	}
	else {
	   readBuffer.get(protocol);
	   if (!(new String(protocol)).equals(PROTOCOL)) {
		  closeAll(ip + " has sent handshake, but protocol is wrong : " + new String(protocol),true);
		  return;
	   }
	}

	byte[] reserved = new byte[8];
	if (readBuffer.remaining() < reserved.length) {
	   closeAll(ip + " has sent handshake, but handshake is of wrong size(2) : " + readBuffer.remaining(),true);
	   return;
	}
	else readBuffer.get(reserved);
	//Ignores reserved bytes


	byte[] hash = manager.getHash();
	byte[] otherHash = new byte[20];
	if (readBuffer.remaining() < otherHash.length) {
	   closeAll(ip + " has sent handshake, but handshake is of wrong size(3) : " + readBuffer.remaining(),true);
	   return;
	}
	else {
	   readBuffer.get(otherHash);
	   for (int i = 0; i < 20; i++) {
		  if (otherHash[i] != hash[i]) {
			 closeAll(ip + " has sent handshake, but hash is wrong",true);
			 return;
		  }
	   }
	}

    
	byte[] otherPeerId = new byte[20];
	if (readBuffer.remaining() < otherPeerId.length) {
	   closeAll(ip + " has sent handshake, but handshake is of wrong size(4) : " + readBuffer.remaining(),true);
	   return;
	}
	else readBuffer.get(otherPeerId);


	if (incoming) {
	  //HandShaking is ok so far
	  //We test if the handshaking is valid (no other connections with that peer)
	  if (manager.validateHandshaking(this, otherPeerId)) {
		  //Store the peerId
		  this.id = otherPeerId;
	  }
	  else {
		 closeAll(ip + " has sent handshake, but peer ID already connected",true);
		 return;
	  }
	}

	else if (!incoming) {
	  boolean same = true;
	  for (int j = 0; j < this.id.length; j++) {
		same = same && (this.id[j] == otherPeerId[j]);
	  }
	  if (!same) {
		 closeAll(ip + " has sent handshake, but peerId is wrong",true);
		 return;
	  }
	}

	//decode a client identification string from the given peerID
	client = Identification.decode(otherPeerId);

	sendBitField();
	readMessage(readBuffer);
	manager.peerAdded(this);
	currentState = new StateTransfering();
  }

  protected void readMessage(ByteBuffer buffer) {
	lengthBuffer.position(0);
	if (buffer != null) buffer.position(0);
	readBuffer = buffer;
	readingLength = true;
  }

  protected void sendProtocol(ByteBuffer buffer) {
	protocolQueue.add(buffer);
  }

  protected void sendData(DiskManagerRequest request) {
	dataQueue.add(manager.createDiskManagerDataQueueItem(request));
  }

  public synchronized void closeAll(String reason,boolean closedOnError) {
	  LGLogger.log(
         componentID,
         evtProtocol,
         closedOnError?LGLogger.ERROR:LGLogger.INFORMATION,
         reason);
    
   if (closing) {
     return;
   }
	closing = true;         
    
	//1. Cancel any pending requests (on the manager side)
	cancelRequests();

	//2. Close the socket

	closeConnection();

	//3. release the read Buffer
	if (readBuffer != null) {
	  ByteBufferPool.getInstance().freeBuffer(readBuffer);
	  readBuffer = null;
   }

	//4. release the write Buffer
	if (writeBuffer != null) {      
	  if (writeData) {
		PEPeerTransportSpeedLimiter.getLimiter().removeUploader(this);
		ByteBufferPool.getInstance().freeBuffer(writeBuffer);
		writeBuffer = null;
	  }
	}

	//5. release all buffers in dataQueue
	for (int i = dataQueue.size() - 1; i >= 0; i--) {
	  DiskManagerDataQueueItem item = (DiskManagerDataQueueItem) dataQueue.remove(i);
	  if (item.isLoaded()) {
		ByteBufferPool.getInstance().freeBuffer(item.getBuffer());
		item.setBuffer(null);
	  }
	  else if (item.isLoading()) {
		manager.freeRequest(item);
	  }
	}

	//6. Send removed event ...
	manager.peerRemoved(this);

	//7. Send a logger event
	LGLogger.log(componentID, evtLifeCycle, LGLogger.INFORMATION, "Connection Ended with " + ip + " : " + port + " ( " + client + " )");
	/*try{
	  throw new Exception("Peer Closed");
	} catch(Exception e) {
	  StackTraceElement elts[] = e.getStackTrace();
	  for(int i = 0 ; i < elts.length ; i++)
		LGLogger.log(componentID,evtLifeCycle,Logger.INFORMATION,elts[i].toString());
	}*/        
    
	//In case it was an outgoing connection, established, we can try to reconnect.   
	if((closedOnError) && (this.currentState != null) && (this.currentState.getState() == TRANSFERING) && (incoming == false) && (nbConnections < 10)) {
	  LGLogger.log(componentID, evtLifeCycle, LGLogger.INFORMATION, "Attempting to reconnect with " + ip + " : " + port + " ( " + client + " )");
	  createConnection();
	} else {
	  this.currentState = new StateClosed();
	}
  }

	
  private class StateConnecting implements PEPeerTransportProtocolState {
	public void process() {
	  try {
		if ( completeConnection()) {
		  handShake(null);
		  currentState = new StateHandshaking();
		}
	  }
	  catch (IOException e) {
		closeAll("Error in PeerConnection::initConnection (" + ip + " : " + port + " ) : " + e, true);
		return;
	  }

	}

	public int getState() {
	  return CONNECTING;
	}
  }
		
  private class StateHandshaking implements PEPeerTransportProtocolState {
	public synchronized void process() {
	  if (readBuffer.hasRemaining()) {
		try {
		  int read = readData(readBuffer);
		  if (read < 0)
			throw new IOException("End of Stream Reached");
		}
		catch (IOException e) {      
		  closeAll(ip + " : StateHandshaking::End of Stream Reached",true);
		  return;
		}
	  }
	  if (readBuffer.remaining() == 0) {
		handleHandShakeResponse();
	  }
	}

	public int getState() {
	  return HANDSHAKING;
	}
  }

  private class StateTransfering implements PEPeerTransportProtocolState {
	public synchronized void process() {      
	  if(++processLoop > 10)
		  return;
          
	  if (readingLength) {
		if (lengthBuffer.hasRemaining() ) {          
		  try {
			int read = readData(lengthBuffer);
      
			if (read < 0)
			  throw new IOException("End of Stream Reached");
		  }
		  catch (IOException e) {
			closeAll(ip + " : StateTransfering::End of Stream Reached (reading length)",true);
			return;
		  }
		}
		//If there's nothing on the socket, then we can quite safely send a request.
		if(lengthBuffer.remaining() == 4) {
		  readyToRequest = true;
		} 
		if (!lengthBuffer.hasRemaining()) {
		  int length = lengthBuffer.getInt(0);
		  
		  if(length < 0) {
		    closeAll(ip + " : length negative : " + length,true);
		    Debug.out("message length negative: "+ip+" "+client+": "+ length);
		    return;
		  }
      
        if(length >= ByteBufferPool.MAX_SIZE) {
          closeAll(ip + " : length greater than max size : " + length,true);
          Debug.out("message length too large: "+ip+" "+client+": "+ length);
          return;
		  }
        
        if (length > 0) {
         //return old readBuffer to pool if it's too small
			if(readBuffer != null && readBuffer.capacity() < length) {
			  ByteBufferPool.getInstance().freeBuffer(readBuffer);
			  readBuffer = null;
			}
      
			if(readBuffer == null) {
			  ByteBuffer newbuff = ByteBufferPool.getInstance().getFreeBuffer(length);
			  if (newbuff == null) {				
			    closeAll(ip + " newbuff null",true);
			    return;
			  }
        
			  if (newbuff.capacity() < length) {
			    Debug.out("newbuff.capacity<length: "+newbuff.capacity()+"<"+length);
			  }
        
			  readBuffer = newbuff;
			  readBuffer.position(0);
			}
      
			int pos = readBuffer.position();
			if (pos !=0) Debug.out("readBuffer.position!=0:"+ pos);
      
			readBuffer.position(0);
			readBuffer.limit(length);
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
		  if (read < 0) {
		    throw new IOException("End of Stream Reached");
        }
		  //hack to statistically determine when we're receving data...
		  if (readBuffer.limit() > 4069) {
			stats.received(read);            
			readyToRequest = true;
		  }
		}
		catch (IOException e) {
		  //e.printStackTrace();
		  closeAll(ip + " : StateTransfering::End of Stream Reached (reading data)",true);
		  return;
		}
    
		if (!readBuffer.hasRemaining()) {
		  //After each message has been received, we're not ready to request anymore,
		  //Unless we finish the socket's queue, or we start receiving a piece.
		  readyToRequest = false;
		  analyseBuffer(readBuffer);         
		  if(getState() == TRANSFERING && readingLength) {
		    process();
		    return;
		  }
		}
	  }
	}

	public int getState() {
	  return TRANSFERING;
	}
  }

  private static class StateClosed implements PEPeerTransportProtocolState {
	public void process() {}

	public int getState() {
	  return DISCONNECTED;
	}
  }

  public void process() {
	try {
	  loopFactor++;
	  processLoop = 0;
	  if (currentState != null)
		currentState.process();
	  processLoop = 0;
	  if (getState() != DISCONNECTED)
		write();
	}
	catch (Exception e) {
	  e.printStackTrace();
	  closeAll(ip + " : Exception in process : " + e,true);
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

  private void analyseBuffer(ByteBuffer buffer) {
	buffer.position(0);
	int pieceNumber, pieceOffset, pieceLength;
	byte cmd = buffer.get();
	switch (cmd) {
	  case BT_CHOKED :
		if (buffer.limit() != 1) {
		  closeAll(ip + " choking received, but message of wrong size : " + buffer.limit(),true);
		  break;
		}
		LGLogger.log(componentID, evtProtocol, LGLogger.RECEIVED, ip + " is choking you");
		choked = true;
		cancelRequests();
		readMessage(readBuffer);
		break;
	  case BT_UNCHOKED :
		if (buffer.limit() != 1) {
		  closeAll(ip + " unchoking received, but message of wrong size : " + buffer.limit(),true);
		  break;
		}
		LGLogger.log(componentID, evtProtocol, LGLogger.RECEIVED, ip + " is unchoking you");
		choked = false;
		readMessage(readBuffer);
		break;
	  case BT_INTERESTED :
		if (buffer.limit() != 1) {
		  closeAll(ip + " interested received, but message of wrong size : " + buffer.limit(),true);
		  break;
		}
		LGLogger.log(componentID, evtProtocol, LGLogger.RECEIVED, ip + " is interested");
		interesting = true;
		readMessage(readBuffer);
		break;
	  case BT_UNINTERESTED :
		if (buffer.limit() != 1) {
		  closeAll(ip + " uninterested received, but message of wrong size : " + buffer.limit(),true);
		  break;
		}
		LGLogger.log(componentID, evtProtocol, LGLogger.RECEIVED, ip + " is not interested");
		interesting = false;
		readMessage(readBuffer);
		break;
	  case BT_HAVE :
		if (buffer.limit() != 5) {
		  closeAll(ip + " interested received, but message of wrong size : " + buffer.limit(),true);
		  break;
		}
		pieceNumber = buffer.getInt();
		LGLogger.log(componentID, evtProtocol, LGLogger.RECEIVED, ip + " has " + pieceNumber);
		have(pieceNumber);
		readMessage(readBuffer);
		break;
	  case BT_BITFIELD :
		LGLogger.log(componentID, evtProtocol, LGLogger.RECEIVED, ip + " has sent BitField");
		setBitField(buffer);
		checkInterested();
		checkSeed();
		readMessage(readBuffer);
		break;
	  case BT_REQUEST :
		if (buffer.limit() != 13) {
		  closeAll(ip + " request received, but message of wrong size : " + buffer.limit(),true);
		  break;
		}
		pieceNumber = buffer.getInt();
		pieceOffset = buffer.getInt();
		pieceLength = buffer.getInt();
		LGLogger.log(
		  componentID,
		  evtProtocol,
		  LGLogger.RECEIVED,
		  ip + " has requested #" + pieceNumber + ":" + pieceOffset + "->" + (pieceOffset + pieceLength));
		if (!choking && manager.checkBlock(pieceNumber, pieceOffset, pieceLength)) {
		  sendData(manager.createDiskManagerRequest(pieceNumber, pieceOffset, pieceLength));
		}
		else {
		  closeAll(ip
        + " has requested #"
        + pieceNumber
        + ":"
        + pieceOffset
        + "->"
        + (pieceOffset + pieceLength)
        + "choking=" + choking
        + " which is an invalid request.",
        true);
		  return;
		}
		readMessage(readBuffer);
		break;
	  case BT_PIECE :
		if (buffer.limit() < 9) {
		   closeAll(ip + " piece received, but message of wrong size : " + buffer.limit(),true);
		   break;
		}
		pieceNumber = buffer.getInt();
		pieceOffset = buffer.getInt();
		pieceLength = buffer.limit() - buffer.position();
		LGLogger.log(
		  componentID,
		  evtProtocol,
		  LGLogger.RECEIVED,
		  ip + " has sent #" + pieceNumber + ":" + pieceOffset + "->" + (pieceOffset + pieceLength));
		DiskManagerRequest request = manager.createDiskManagerRequest(pieceNumber, pieceOffset, pieceLength);
		if (requested.contains(request) && manager.checkBlock(pieceNumber, pieceOffset, buffer)) {
		  requested.remove(request);
		  manager.received(pieceLength);
		  setSnubbed(false);
		  reSetRequestsTime();
		  manager.writeBlock(pieceNumber, pieceOffset, buffer,this);                
		  readMessage(null);      
		}
		else {
		  LGLogger.log(
			componentID,
			evtErrors,
			LGLogger.ERROR,
			ip
			  + " has sent #"
			  + pieceNumber
			  + ":"
			  + pieceOffset
			  + "->"
			  + (pieceOffset + pieceLength)
			  + " but piece was discarded (either not requested or invalid)");
		  stats.discarded(pieceLength);
		  manager.discarded(pieceLength);
		  readMessage(readBuffer);
		}
		break;
	  case BT_CANCEL :
		if (buffer.limit() != 13) {
		  closeAll(ip + " cancel received, but message of wrong size : " + buffer.limit(),true);
		  break;
		}
		pieceNumber = buffer.getInt();
		pieceOffset = buffer.getInt();
		pieceLength = buffer.getInt();
		LGLogger.log(
		  componentID,
		  evtProtocol,
		  LGLogger.RECEIVED,
		  ip + " has canceled #" + pieceNumber + ":" + pieceOffset + "->" + (pieceOffset + pieceLength));
		removeRequestFromQueue(manager.createDiskManagerRequest(pieceNumber, pieceOffset, pieceLength));
		readMessage(readBuffer);
		break;
	 default:
	  closeAll(ip + " has sent a wrong message " + cmd,true);
	}
  }

  private void have(int pieceNumber) {
	if ((pieceNumber >= available.length) || (pieceNumber < 0)) {
	   closeAll(ip + " gave invalid pieceNumber:" + pieceNumber,true);
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

  private void reSetRequestsTime() {
	for (int i = 0; i < requested.size(); i++) {
	  DiskManagerRequest request = null;
	  try {
		request = (DiskManagerRequest) requested.get(i);
	  }
	  catch (Exception e) {}
	  if (request != null)
		request.reSetTime();
	}
  }

  public void request(int pieceNumber, int pieceOffset, int pieceLength) {
	if (getState() != TRANSFERING) {
    manager.requestCanceled(manager.createDiskManagerRequest(pieceNumber, pieceOffset, pieceLength));
	  return;
  }	
	DiskManagerRequest request = manager.createDiskManagerRequest(pieceNumber, pieceOffset, pieceLength);
	if (!requested.contains(request)) {
		LGLogger.log(
		  componentID,
		  evtProtocol,
		  LGLogger.SENT,
		  ip + " is asked for #" + pieceNumber + ":" + pieceOffset + "->" + (pieceOffset + pieceLength));
		requested.add(request);
		ByteBuffer buffer = ByteBuffer.allocate(17);
		buffer.putInt(13);
		buffer.put(BT_REQUEST);
		buffer.putInt(pieceNumber);
		buffer.putInt(pieceOffset);
		buffer.putInt(pieceLength);
		buffer.position(0);
		buffer.limit(17);
		sendProtocol(buffer);
	}
  }

  public void sendCancel(DiskManagerRequest request) {
	if (getState() != TRANSFERING)
	  return;	
	if (requested.contains(request)) {
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
	  requested.remove(request);
	  ByteBuffer buffer = ByteBuffer.allocate(17);
	  buffer.putInt(13);
	  buffer.put(BT_CANCEL);
	  buffer.putInt(request.getPieceNumber());
	  buffer.putInt(request.getOffset());
	  buffer.putInt(request.getLength());
	  buffer.position(0);
	  buffer.limit(17);
	  sendProtocol(buffer);
	}
  }

  public void sendHave(int pieceNumber) {
	if (getState() != TRANSFERING)
	  return;
	LGLogger.log(componentID, evtProtocol, LGLogger.SENT, ip + " is notified you have " + pieceNumber);
	ByteBuffer buffer = ByteBuffer.allocate(9);
	buffer.putInt(5);
	buffer.put(BT_HAVE);
	buffer.putInt(pieceNumber);
	buffer.position(0);
	buffer.limit(9);
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
	ByteBuffer buffer = ByteBuffer.allocate(5);
	buffer.putInt(1);
	buffer.put(command);
	buffer.position(0);
	buffer.limit(5);
	sendProtocol(buffer);
  }

  private void setBitField(ByteBuffer buffer) {
	byte[] data = new byte[(manager.getPiecesNumber() + 7) / 8];
   
	if (buffer.remaining() < data.length) {
     LGLogger.log(componentID, evtProtocol, LGLogger.RECEIVED, ip + " has sent invalid BitField: too short");
	  return;
   }
   
	buffer.get(data);
	for (int i = 0; i < available.length; i++) {
	  int index = i / 8;
	  int bit = 7 - (i % 8);
	  byte bData = data[index];
	  byte b = (byte) (bData >> bit);
	  if ((b & 0x01) == 1) {
		available[i] = true;
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
	ByteBuffer buffer = ByteBuffer.allocate(5 + (manager.getPiecesNumber() + 7) / 8);
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
	  sendProtocol(buffer);
	  LGLogger.log(componentID, evtProtocol, LGLogger.SENT, ip + " is sent your bitfield");
	}
  }

  private void removeRequestFromQueue(DiskManagerRequest request) {
	for (int i = 0; i < dataQueue.size(); i++) {
	  DiskManagerDataQueueItem item = (DiskManagerDataQueueItem) dataQueue.get(i);
	  if (item.getRequest().equals(request)) {
		if (item.isLoaded()) {
		  ByteBufferPool.getInstance().freeBuffer(item.getBuffer());
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

  public void cancelRequests() {
	if (requested == null)
	  return;
	synchronized (requested) {
	  for (int i = requested.size() - 1; i >= 0; i--) {
		DiskManagerRequest request = (DiskManagerRequest) requested.remove(i);
		manager.requestCanceled(request);
	  }
	}
  }

  public int getNbRequests() {
	return requested.size();
  }

  public List getExpiredRequests() {
	Vector result = new Vector();
	for (int i = 0; i < requested.size(); i++) {
	  try {
		DiskManagerRequest request = (DiskManagerRequest) requested.get(i);
		if (request.isExpired()) {
		  result.add(request);
		}
	  }
	  catch (ArrayIndexOutOfBoundsException e) {
		//Keep going, most probably, piece removed...
		//Hopefully we'll find it later :p
	  }
	}
	return result;
  }

  protected synchronized void write() {
	if(currentState.getState() == CONNECTING || currentState.getState() == DISCONNECTED  )
	  return;       
	if(++processLoop > 10)
		return;    
	//If we are already sending something, we simply continue
	if (writeBuffer != null) {
	  try {
		int realLimit = writeBuffer.limit();
		int limit = realLimit;
		int uploadAllowed = 0;
		if (writeData && PEPeerTransportSpeedLimiter.getLimiter().isLimited(this)) {
		  if ((loopFactor % 5) == 0) {
			allowed = PEPeerTransportSpeedLimiter.getLimiter().getLimitPer100ms(this);
			used = 0;
		  }
		  uploadAllowed = allowed - used;
		  limit = writeBuffer.position() + uploadAllowed;
		  if ((limit > realLimit) || (limit < 0))
			limit = realLimit;
		}

		writeBuffer.limit(limit);
		int written = writeData(writeBuffer);
		if (written < 0)
		  throw new IOException("End of Stream Reached");
		writeBuffer.limit(realLimit);

		if (writeData) {
		  stats.sent(written);
		  manager.sent(written);
		  if (PEPeerTransportSpeedLimiter.getLimiter().isLimited(this)) {
			used += written;
			if ((loopFactor % 5) == 4) {
			  if (used >= allowed) // (100 * allowed) / 100
				maxUpload = max(110 * allowed, 20);
			  if (used < (95 * allowed) / 100)
				maxUpload = max((100 * written) / 100, 20);
			}
		  }
		}
	  }
	  catch (IOException e) {
	    closeAll("Error while writing to " + ip +" : " + e,true);
	    return;
	  }
    
	  //If we have finished sending this buffer
	  if (!writeBuffer.hasRemaining()) {
		//If we were sending data, we must free the writeBuffer
		if (writeData) {
		  ByteBufferPool.getInstance().freeBuffer(writeBuffer);
		  writeBuffer = null;
		  PEPeerTransportSpeedLimiter.getLimiter().removeUploader(this);
		}
		//We set it to null
		writeBuffer = null;
    //And return in order not to process any other writes this turn
    //So you may wonder? why no more writes this turn?
    //I'm asking myself the exact same question, and I've got no answer
    //But this solves BOTH hash fails and deconnections
    //Due to invalid packet length sent ....
    //to do : Understand why this return is needed here
    //return;
	  } //So if we haven't written the whole buffer, we simply return...
	}

	if (writeBuffer == null) {
	  //So the ByteBuffer is null ... let's find out if there's any data in the protocol queue
	  if (protocolQueue.size() != 0) {
		//Correct in 1st approximation (a choke message queued (if any) will to be send soon after this)
		waitingChokeToBeSent = false;
		//Assign the current buffer ...
		keepAlive = 0;
		writeBuffer = (ByteBuffer) protocolQueue.remove(0);
		
		if (writeBuffer == null){
		  closeAll(ip + " : Empty write Buffer on protocol message !!!",true);
		  return;
		}
		
		//check to make sure we're sending a proper message length
		if (!verifyLength(writeBuffer)) {
		  closeAll("OOPS, we're sending a bad protocol message length !!!", true);
		  System.out.println("OOPS, we're sending a bad protocol message length !!!");
		  return;
		}
		
		writeBuffer.position(0);
		writeData = false;
		//and loop
		write();
		return;
	  }
    
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
			  closeAll("OOPS, we're sending a bad data message length !!!", true);
			  System.out.println("OOPS, we're sending a bad data message length !!!");
			  return;
			}
    
			writeBuffer.position(0);
			writeData = true;
			PEPeerTransportSpeedLimiter.getLimiter().addUploader(this);
			// and loop
			write();
			return;
		  }
		}
		else {
		  //We are choking the peer so ...
		  if (!item.isLoading()) {
			dataQueue.remove(item);
		  }
		  if (item.isLoaded()) {
			dataQueue.remove(item);
			ByteBufferPool.getInstance().freeBuffer(item.getBuffer());
			item.setBuffer(null);
		  }
		  return;
		}
	  }
	  if ((protocolQueue.size() == 0) && (dataQueue.size() == 0)) {
		keepAlive++;
		if (keepAlive == 50 * 60) {
		  keepAlive = 0;
		  sendKeepAlive();
		}
	  }
	}
  }
  
  /**
   * Verifies that the buffer length is correct.
   * Returns true if the buffer length is OK.
   */
  private boolean verifyLength(ByteBuffer buffer) {
    //check for a handshake message
    if (buffer.get(0) == (byte)PROTOCOL.length()) {
      //make sure the length is correct for a handshake
      if (buffer.limit() != 68) {
        //make sure it isn't a normal message
        int length =  buffer.getInt(0);
        if (length != buffer.limit() - 4) {
          Debug.out("PROTOCOL: givenLength=" + length + " realLength=" + buffer.limit());
          return false;
        }
        return true; 
      }
      return true;
    }
    
    int length =  buffer.getInt(0);
    if (length != buffer.limit() - 4) {
      Debug.out("givenLength=" + length + " realLength=" + buffer.limit());
      return false;
    }
    return true;
  }

  private int max(int a, int b) {
	return a > b ? a : b;
  }

  private void sendKeepAlive() {
	ByteBuffer buffer = ByteBuffer.allocate(4);
	buffer.putInt(0);
	buffer.limit(4);
	buffer.position(0);
	protocolQueue.add(buffer);
  }

  public int getMaxUpload() {
	return maxUpload;
  }

  public DiskManagerDataQueueItem getNextRequest() {
	if (dataQueue.size() != 0)
	  return (DiskManagerDataQueueItem) dataQueue.get(0);
	return null;
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
  startConnection()

	  throws IOException;
	

  protected abstract void
  closeConnection();
 	
  protected abstract boolean
  completeConnection()
	
	  throws IOException;

	
  protected abstract int
  readData(
	  ByteBuffer	buffer )
		
	  throws IOException;

  protected abstract int
  writeData(
		ByteBuffer	buffer )
		
		throws IOException;
}
