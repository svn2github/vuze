/*
 * File    : PEPeerManagerImpl
 * Created : 15-Oct-2003
 * By      : Olivier
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
 
 package org.gudy.azureus2.core3.peer.impl;


import java.nio.ByteBuffer;
import java.util.*;


import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.tracker.client.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.disk.DiskManagerDataQueueItem;
import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerRequest;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.peer.impl.transport.base.*;


public class 
PEPeerManagerImpl
	extends 	Thread
	implements 	PEPeerManager
{
  private static final int MAX_REQUESTS = 16;
  private static final boolean DEBUG = false;

  private int[] _availability;
  private boolean _bContinue;                
  private List _connections;
  private DiskManager _diskManager;
  private boolean[] _downloaded;
  private boolean[] _downloading;
  private boolean _finished;
  protected boolean[] _piecesRarest;
  private byte[] _hash;
  private int _loopFactor;
  private byte[] _myPeerId;
  private int _nbPieces;
  private PEPiece[] _pieces;
  private PEPeerServerImpl _server;
  private PEPeerStatsImpl _stats;
  private long _timeLastUpdate;
  private int _timeToWait;
  private TRTrackerClient _tracker;
  private String _trackerStatus;
  //  private int _maxUploads;
  private int _trackerState;
  private static final int TRACKER_START = 1;
  private static final int TRACKER_UPDATE = 2;
  private int _seeds, _peers;
  private long _timeStarted;
  private long _timeFinished;
  private Average _averageReceptionSpeed;
  private PEPeerTransport currentOptimisticUnchoke;

  private DownloadManager _manager;
  private List requestsToFree;
  private PeerUpdater peerUpdater;
  
  boolean slowConnect;
  private SlowConnector slowConnector;
  private List slowQueue;
  
  private int uploadCount = 0;
  
  private List RequestExpired;
  
  private int nbHashFails;

  public PEPeerManagerImpl(
    DownloadManager manager,
    PEPeerServer server,
	TRTrackerClient tracker,
    DiskManager diskManager) {
    super("Peer Manager"); //$NON-NLS-1$
    this._manager = manager;
    //This torrent Hash
    
    try{
    
    	_hash = tracker.getTorrent().getHash();
    	
    }catch( TOTorrentException e ){
    	
    		// this should never happen - TODO: tidy when refactoring peer manager
    	e.printStackTrace();
    	
    	_hash = new byte[20]; // TODO: just for the mo
    }
    
    this.nbHashFails = 0;

    //The connection to the tracker
    _tracker = tracker;
    _tracker.setManager(this);
    _myPeerId = _tracker.getPeerId();

    //The peer connections
    _connections = new ArrayList();

    //The Server that handle incoming connections
    _server = (PEPeerServerImpl)server;
    _server.setManager(this);

    this._diskManager = diskManager;
    _diskManager.setPeerManager(this);

    //BtManager is threaded, this variable represents the
    // current loop iteration. It's used by some components only called
    // at some specific times.
    _loopFactor = 0;

    //The current tracker state
    //this could be start or update
    _trackerState = TRACKER_START;
    _trackerStatus = "..."; //$NON-NLS-1$
    _timeToWait = 120;

    _averageReceptionSpeed = Average.getInstance(1000, 30);

    setDiskManager(diskManager);

    requestsToFree = new ArrayList();

    peerUpdater = new PeerUpdater();
    peerUpdater.start();
    
    /* create new outgoing connections slowly */
    slowConnect = COConfigurationManager.getBooleanParameter("Slow Connect", false);
    if (slowConnect) {
       slowQueue = Collections.synchronizedList(new LinkedList());
       slowConnector = new SlowConnector();
       slowConnector.setDaemon(true);
       slowConnector.start();
    }
  }

  private class PeerUpdater extends Thread {
    private boolean bContinue = true;

    private long started[];

    public PeerUpdater() {
      super("Peer Updater"); //$NON-NLS-1$
      started = new long[10];
    }

    public void run() {
      while (bContinue) {
        for (int i = 9; i > 0; i--)
          started[i] = started[i - 1];
        started[0] = System.currentTimeMillis();
        synchronized (_connections) {
          Iterator iter = _connections.iterator();
          while (iter.hasNext()) {
            PEPeerTransport ps = (PEPeerTransport) iter.next();
            if (ps.getState() == PEPeer.DISCONNECTED) {
              iter.remove();
            }
            else {
              ps.process();
            }
          }
        }
        try {

          long wait = 20;
          wait -= (System.currentTimeMillis() - started[0]);
          if (started[4] != 0)
            for (int i = 0; i < 9; i++) {
              wait += 20 + (started[i + 1] - started[i]);
            }

          if (wait > 30)
            wait = 30;

          if (wait > 10)
            Thread.sleep(wait);
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    public void stopIt() {
      bContinue = false;
    }
  }
  
  /* thread to slow connect new outgoing peer connections */
  private class SlowConnector extends Thread {
      private boolean bContinue = true;

      public SlowConnector() {
         super("Slow Connector");
      }

      public void run() {
         PEPeerTransport testPS;
         
         while (bContinue) {
            testPS = null;
            
            try {
               /* wait until notified of new connection to slow connect */
               synchronized (slowQueue) { slowQueue.wait(1000); }
               
               /* dequeue waiting connections and process */
               while ((slowQueue.size() > 0) && bContinue) {
                  /* get next connection */
                  testPS = (PEPeerTransport)slowQueue.remove(0);
                  /* add the connection */
                  if (testPS != null) {
                     synchronized (_connections) {
                        //System.out.println("new slow connect: " + (System.currentTimeMillis() /1000));
                        /* add connection */
                        _connections.add(PEPeerTransportFactory.createTransport((PEPeerManagerImpl)testPS.getManager(), testPS.getId(), testPS.getIp(), testPS.getPort(), false));
                     }
                  }
                  /* wait */
                  Thread.sleep(1000);
               }
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      }

      public void stopIt() {
        bContinue = false;
      }
    }
  
  

  //main method
  public void run() {
    _bContinue = true;
    _manager.setState(DownloadManager.STATE_DOWNLOADING);
    _timeStarted = System.currentTimeMillis() / 1000;
    while (_bContinue) //loop until stopAll() kills us
      {
      try {
        long timeStart = System.currentTimeMillis();
        checkTracker(timeStart / 1000); //check the tracker status, update peers
        checkCompletedPieces();
        //check to see if we've completed anything else
        computeAvailability(); //compute the availablity                   
        updateStats();
        freeRequests();
        if (!_finished) //if we're not finished
          {
          //::moved check finished to the front -Tyler
          checkFinished(); //see if we've finished
          _diskManager.computePriorityIndicator();
          checkRequests(); //check the requests               
          checkDLPossible(); //look for downloadable pieces          
        }
        checkSeeds();
        unChoke();
        //prefetchReadOperation();
        //sendReceive(); //Send - Receive data on sockets
        _loopFactor++; //increment the loopFactor
        long timeWait = 100 - (System.currentTimeMillis() - timeStart);
        if (timeWait < 10)
          timeWait = 10;
        Thread.sleep(timeWait); //sleep

      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public void stopAll() {

    //1. Stop the peer updater
    peerUpdater.stopIt();
    
    /* stop the slow connector if running */
    if (slowConnect) slowConnector.stopIt();

    //2. Stop itself
    _bContinue = false;

    //3. Stop the server
    _server.stopServer();

    for (int i = 0; i < _pieces.length; i++) {
      if (_pieces[i] != null)
        pieceRemoved(_pieces[i]);
    }    

    //Asynchronous cleaner

    Thread t = new Thread("Cleaner - Tracker Ender") {
      public void run() {
          //1. Send disconnect to Tracker
  _tracker.stop();
        try {
          while (requestsToFree.size() != 0) {
            freeRequests();
            Thread.sleep(100);
          }
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    };
    t.start();

    //  4. Close all clients
    if (_connections != null)
      synchronized (_connections) {
        while (_connections.size() != 0) {
          PEPeerTransport pc = (PEPeerTransport) _connections.remove(0);
          pc.closeAll(false);
        }
      }
  }

  /**
   * A private method that does analysis of the result sent by the tracker.
   * It will mainly open new connections with peers provided
   * and set the timeToWait variable according to the tracker response.
   * @param the tracker response
   */
  
	private void 
	analyseTrackerResponse(
  		TRTrackerResponse	tracker_response )
  	{
  		// tracker_response.print();
  		
  		int	status = tracker_response.getStatus();
  		
		_timeToWait	= (int)tracker_response.getTimeToWait();
		
    	if ( status == TRTrackerResponse.ST_OFFLINE ){
      
      		_trackerStatus = MessageText.getString("PeerManager.status.offline"); //set the status to offline       //$NON-NLS-1$
      		      
      		String	reason = tracker_response.getFailureReason();
      		
      		if ( reason != null ){
      			
      			_trackerStatus += " (" + reason + ")";		
      		}
	    }else if ( status == TRTrackerResponse.ST_REPORTED_ERROR ){

			_trackerStatus = tracker_response.getFailureReason();
			
	    }else{
	    	
        	addPeersFromTracker( tracker_response.getPeers());
        	
        	_trackerState = TRACKER_UPDATE;
        	
        	_trackerStatus = MessageText.getString("PeerManager.status.ok"); //set the status      //$NON-NLS-1$
    	}
  	}

 	private void 
 	addPeersFromTracker(
 		TRTrackerResponsePeer[]		peers )
 	{
      
		for (int i = 0; i < peers.length; i++){
      	
			TRTrackerResponsePeer	peer = peers[i];
		
			byte[]	this_peer_id = peer.getPeerId();
		
				// ignore ourselves
				
        	if (!Arrays.equals(this_peer_id, _myPeerId)){
           
        	   insertPeerSocket( this_peer_id, peer.getIPAddress(), peer.getPort());
        	}                
  		}
 	}

  /**
   * A private method that checks if pieces being downloaded are finished
   * If all blocks from a piece are written to disk, then this process will check the piece
   * and if it passes sha-1 check, it will be marked as downloaded.
   * otherwise, it will unmark it as being downloaded, so blocks can be retreived again.
   */
  private void checkCompletedPieces() {
    //for every piece
    for (int i = 0; i < _nbPieces; i++) {
      PEPiece currentPiece = _pieces[i]; //get the piece

      //if piece is loaded, and completed
      if (currentPiece != null && currentPiece.isComplete() && !currentPiece.isBeingChecked()) {
        //check the piece from the disk
        _diskManager.aSyncCheckPiece(i);
        currentPiece.setBeingChecked();
      }
    }
  }

  /**
   * This method scans all peers and if downloading from them is possible,
   * it will try to find blocks to download.
   * If the peer is marked as snubbed then only one block will be downloaded
   * otherwise it will queue up 5 requests.
   * 
   */
  private void checkDLPossible() {
    //::updated this method to work with List -Tyler
    //for all peers
    Vector bestUploaders = new Vector();
    synchronized (_connections) {
      int[] upRates = new int[_connections.size()];
      Arrays.fill(upRates, -1);

      for (int i = 0; i < _connections.size(); i++) {
        PEPeerTransport pc = null;
        try {
          pc = (PEPeerTransport) _connections.get(i);
        }
        catch (Exception e) {
          break;
        }
        if (pc.transferAvailable()) {
          int upRate = pc.getStats().getReception();
          testAndSortBest(upRate, upRates, pc, bestUploaders, 0);
        }
      }
    }

    for (int i = 0; i < bestUploaders.size(); i++) {
      //get a connection 
      PEPeerTransport pc = (PEPeerTransport) bestUploaders.get(i);
      //can we transfer something?
      if (pc.transferAvailable()) {
        boolean found = true;
        //If queue is too low, we will enqueue another request
        int maxRequests = MAX_REQUESTS;
        if (pc.isSnubbed())
          maxRequests = 1;
        while ((pc.isReadyToRequest() && pc.getState() == PEPeer.TRANSFERING) && found && (pc.getNbRequests() < maxRequests)) {
          found = findPieceToDownload(pc, pc.isSnubbed());
          //is there anything else to download?
        }
      }
    }
  }

  /**
   * This method checks if the downloading process is finished.
   *
   */
  private void checkFinished() {
    boolean temp = true;
    //for every piece
    for (int i = 0; i < _nbPieces; i++) {
      //:: we should be able to do this better than keeping a bunch of arrays
      //:: possibly adding a status field to the piece object? -Tyler 
      temp = temp && _downloaded[i];

      //::pre-emptive break should save some cycles -Tyler
      if (!temp) {
        break;
      }
    }

    //set finished
    _finished = temp;
    if (_finished) {
      _timeFinished = System.currentTimeMillis() / 1000;
      _manager.setState(DownloadManager.STATE_SEEDING);
      //_diskManager.changeToReadOnly();
      _diskManager.dumpResumeDataToDisk(true);
      synchronized (_connections) {
        for (int i = 0; i < _connections.size(); i++) {
          PEPeerTransport pc = (PEPeerTransport) _connections.get(i);
          if (pc != null) {
            pc.setSnubbed(false);
          }
        }
      }
      boolean checkPieces = COConfigurationManager.getBooleanParameter("Check Pieces on Completion",false);
      if(checkPieces && (_timeFinished - _timeStarted) > 10) {
        for(int i = 0 ; i < _downloaded.length ;i++) {
          //check the piece from the disk
          _diskManager.aSyncCheckPiece(i);
        }
      }
    }
  }

  /**
   * This method will locate any expired request, will cancel it, and mark the peer as snubbed
   *
   */
  private void checkRequests() {
    //for every connection
    synchronized (_connections) {
      for (int i = 0; i < _connections.size(); i++) {
        PEPeerTransport pc = (PEPeerTransport) _connections.get(i);
        if (pc.getState() == PEPeer.TRANSFERING) {
          List expired = pc.getExpiredRequests();
          //::May want to make this an ArrayList unless you
          //::need the synchronization a vector offers -Tyler
          if (expired.size() > 0) {
            pc.setSnubbed(true);

            //Only cancel first request if more than 2 mins have passed
            DiskManagerRequest request = (DiskManagerRequest) expired.get(0);
            long timeCreated = request.getTimeCreated();
            if (System.currentTimeMillis() - timeCreated > 1000 * 120) {
              int pieceNumber = request.getPieceNumber();
              //get the piece number
              int pieceOffset = request.getOffset();
              //get the piece offset
              PEPiece piece = _pieces[pieceNumber]; //get the piece
              if (piece != null)
                piece.unmarkBlock(pieceOffset / BLOCK_SIZE);
              //unmark the block
              _downloading[pieceNumber] = false;
              //set piece to not being downloaded
            }

            //for every expired request                              
            for (int j = 1; j < expired.size(); j++) {
              request = (DiskManagerRequest) expired.get(j);
              //get the request object
              pc.sendCancel(request); //cancel the request object
              int pieceNumber = request.getPieceNumber();
              //get the piece number
              int pieceOffset = request.getOffset();
              //get the piece offset
              PEPiece piece = _pieces[pieceNumber]; //get the piece
              if (piece != null)
                piece.unmarkBlock(pieceOffset / BLOCK_SIZE);
              //unmark the block
              _downloading[pieceNumber] = false;
              //set piece to not being downloaded
            }
          }
        }
      }
    }
  }

  /**
   * This method will check the tracker. It creates a new thread so requesting the url won't freeze the program.
   *
   */
  private void checkTracker(long time) {
    //has the timeout expired?
    if ((time - _timeLastUpdate) > _timeToWait) //if so...
      {
      _trackerStatus = MessageText.getString("PeerManager.status.checking") + "..."; //$NON-NLS-1$ //$NON-NLS-2$      
      checkTracker();
    }
  }

	public void 
  	checkTracker()
  	{
		long time = System.currentTimeMillis() / 1000;
		
    	if (time - _timeLastUpdate < 60){
    		
      		return;
    	}
    	
    	_timeLastUpdate = time; //update last checked time
    	
      	Thread t = new Thread("Tracker Checker") {//$NON-NLS-1$
			public void 
		  	run() 
		 	{
		  		try{
		    		TRTrackerResponse result;
		    	
		    	    if ( _trackerState == TRACKER_UPDATE){
		        
		        	    result = _tracker.update();
		        	}else{
		        
		            	result = _tracker.start();
		        	}
		        
		        	analyseTrackerResponse(result);
		       
		  		}catch (Throwable e){
		        
		    		e.printStackTrace();
		  		} 
		 	}
		};
    t.start(); //start the thread
  }

  /**
   * This methd will compute the overall availability (inluding yourself)
   *
   */
  private void computeAvailability() {
    //reset the availability
    int[] availability = new int[_availability.length];
    Arrays.fill(availability, 0); //:: should be faster -Tyler

    //for all clients
    synchronized (_connections) {
      for (int i = _connections.size() - 1; i >= 0; i--) //::Possible optimization to break early when you reach 100%
        {
        //get the peer connection
        PEPeerTransport pc = (PEPeerTransport) _connections.get(i);

        //get an array of available pieces    
        boolean[] piecesAvailable = pc.getAvailable();
        if (piecesAvailable != null) //if the list is not null
          {
          for (int j = _nbPieces - 1; j >= 0; j--) //loop for every piece
            {
            if (piecesAvailable[j]) //set the piece to available
              availability[j]++;
          }
        }
      }
    }

    //Then adds our own availability.
    for (int i = _downloaded.length - 1; i >= 0; i--) {
      if (_downloaded[i])
        availability[i]++;
    }

    //copy availability into _availability
    synchronized (_availability) {
      for (int i = 0; i < availability.length; i++)
        _availability[i] = availability[i];
    }
  }

  /**
   * This method is the core download manager.
   * It will decide for a given peer, which block it should download from it.
   * Here is the overall algorithm :
   * 1. It will determine a list of rarest pieces.
   * 2. If one is already being requested but not completely, it will continue it
   * 3. If not, it will start a new piece dl based on a randomly choosen piece from least available ones.  
   * 3. If it can't find a piece then, this means that all pieces are already downloaded/fully requested, and it returns false.
   * 
   * @param pc the PeerConnection we're working on
   * @param snubbed if the peer is snubbed, so requested block won't be mark as requested.
   * @return true if a request was assigned, false otherwise
   */
  private boolean findPieceToDownload(PEPeerTransport pc, boolean snubbed) {
    //get the rarest pieces list
    getRarestPieces(pc);
    if (_piecesRarest == null)
      return false;

    int nbPiecesRarest = 0;
    for (int i = 0; i < _piecesRarest.length; i++) {
      if (_piecesRarest[i])
        nbPiecesRarest++;
    }

    //If there is no piece to download, return.
    if (nbPiecesRarest == 0)
      return false;

    //Piece number and block number that we should dl
    int pieceNumber = -1;
    int blockNumber = -1;

    //Last completed level (this is for undo purposes)   
    int lastCompleted = -1;

    //For every piece
    for (int i = 0; i < _nbPieces; i++) {
      //If we're not downloading the piece and if it's available from that peer   
      if ((_pieces[i] != null) && !_downloading[i] && _piecesRarest[i]) {
        //We get and mark the next block number to dl
        //We will either get -1 if no more blocks need to be requested
        //Or a number >= 0 otherwise
        int tempBlock = _pieces[i].getAndMarkBlock();

        //SO, if there is a block to request in that piece
        if (tempBlock != -1) {
          //Is it a better piece to dl from?
          //A better piece is a piece which is more completed
          //ie more blocks have already been WRITTEN on disk (not requested)
          if (_pieces[i].getCompleted() > lastCompleted) {
            //If we had marked a block previously, we must unmark it
            if (pieceNumber != -1) {
              //So pieceNumber contains the last piece
              //We unmark the last block marked        
              _pieces[pieceNumber].unmarkBlock(blockNumber);
            }
            //Now we change the different variables
            //The new pieceNumber being used is pieceNumber
            pieceNumber = i;
            //The new last block number is block number
            blockNumber = tempBlock;
            //The new completed level
            lastCompleted = _pieces[i].getCompleted();
          }
          else {
            //This piece is not intersting, but we have marked it as
            //being downloaded, we have to unmark it.
            _pieces[i].unmarkBlock(tempBlock);
          }
        }
        else {
          //So ..... we have a piece not marked as being downloaded ...
          //but without any free block to request ...
          //let's correct this situation :p
          _downloading[i] = true;
          _piecesRarest[i] = false;
          nbPiecesRarest--;
        }
      }
    }

    //Ok, so we may have found a valid (piece;block) to request    
    if (pieceNumber != -1 && blockNumber != -1) {
      //If the peer is snubbed, we unmark the block as being requested
      //if (snubbed)
      //  _pieces[pieceNumber].unmarkBlock(blockNumber);

      //We really send the request to the peer
      pc.request(pieceNumber, blockNumber * BLOCK_SIZE, _pieces[pieceNumber].getBlockSize(blockNumber));

      //and return true as we have found a block to request
      return true;
    }

    if (nbPiecesRarest == 0)
      return false;

    //If we get to this point we haven't found a block from a piece being downloaded
    //So we'll find a new one          

    //Otherwhise, vPieces is not null, we'll 'randomly' choose an element from it.

    pieceNumber = _diskManager.getPiecenumberToDownload(_piecesRarest);

    if (pieceNumber == -1)
      return false;
    //Now we should have a piece with least presence on network    
    PEPiece piece = null;

    //We need to know if it's last piece or not when creating the BtPiece Object
    if (pieceNumber < _nbPieces - 1)
      piece = new PEPieceImpl(this, _diskManager.getPieceLength(), pieceNumber);
    else
      piece = new PEPieceImpl(this, _diskManager.getLastPieceLength(), pieceNumber);

    pieceAdded(piece);
    //Assign the created piece to the pieces array.
    _pieces[pieceNumber] = piece;

    //We send request ...
    blockNumber = piece.getAndMarkBlock();
    //if (snubbed)
    //  _pieces[pieceNumber].unmarkBlock(blockNumber);

    pc.request(pieceNumber, blockNumber * BLOCK_SIZE, piece.getBlockSize(blockNumber));
    return true;
  }

  private void getRarestPieces(PEPeerTransport pc) {
    boolean[] piecesAvailable = pc.getAvailable();
    Arrays.fill(_piecesRarest, false);

    //This will represent the minimum piece availability level.
    int pieceMinAvailability = -1;

    //1. Scan all pieces to determine min availability
    for (int i = 0; i < _nbPieces; i++) {
      if (!_downloaded[i] && !_downloading[i] && piecesAvailable[i]) {
        if (pieceMinAvailability == -1 || _availability[i] < pieceMinAvailability) {
          pieceMinAvailability = _availability[i];
        }
      }
    }

    //We add a 90 % range
    pieceMinAvailability = (190 * pieceMinAvailability) / 100;
    //For all pieces
    for (int i = 0; i < _nbPieces; i++) {
      //If we're not downloading it, if it's not downloaded, and if it's available from that peer
      if (!_downloaded[i] && !_downloading[i] && piecesAvailable[i]) {
        //null : special case, to ensure we find at least one piece
        //or if the availability level is lower than the old availablility level
        if (_availability[i] <= pieceMinAvailability) {
          _piecesRarest[i] = true;
        }
      }
    }
  }

  /**
    * private method to add a new outgoing peerConnection
    * created by Tyler
    * @param pc
    */
  private synchronized void insertPeerSocket(byte[] peerId, String ip, int port) {
    //Get the max number of connections allowed
    int maxConnections = COConfigurationManager.getIntParameter("Max Clients", 0); //$NON-NLS-1$
    /* create a peer socket for testing purposes */
    PEPeerTransport testPS = PEPeerTransportFactory.createTransport(this, peerId, ip, port, true);
    
    if (!IpFilterImpl.getInstance().isInRange(ip)) {
       synchronized (_connections) {
          if (!_connections.contains(testPS)) {
             if (maxConnections == 0 || _connections.size() < maxConnections) {
                /* do we need to slow down new connection creation? */
                if (slowConnect) {
                   /* add connection to be slow-connected */
                   slowQueue.add(testPS);
                   synchronized (slowQueue) { slowQueue.notify(); }
                }
                else {
                   /* add connection */
                   _connections.add(PEPeerTransportFactory.createTransport(this, peerId, ip, port, false));
                }
             }
          }
       }
    }
  }
  
  
 /**
   * private method to add a new incoming peerConnection
   * created by Tyler
   * @param pc
   */
 private synchronized void insertPeerSocket(PEPeerTransport ps) {
    //Get the max number of connections allowed
    int maxConnections = COConfigurationManager.getIntParameter("Max Clients", 0); //$NON-NLS-1$
    boolean addFailed = false;
    
    if (!IpFilterImpl.getInstance().isInRange(ps.getIp())) {
       synchronized (_connections) {
          if (!_connections.contains(ps)) {
             if (maxConnections == 0 || _connections.size() < maxConnections) {
                /* add connection */
                _connections.add(ps);
             }
             else { addFailed = true; }
          }
          else { addFailed = true; }
       }
    }
    else { addFailed = true; }
    
    if (addFailed) {
       ps.closeAll(false);
    }
 }

  
  private void unChoke() {
    //1. We retreive the current non-choking peers
    Vector nonChoking = getNonChokingPeers();

    //2. Determine how many uploads we should consider    
    int nbUnchoke = _manager.getMaxUploads();

    //System.out.println(nbUnchoke);

    //3. Then, in any case if we have too many unchoked pple we need to choke some
    while (nbUnchoke < nonChoking.size()) {
      PEPeerTransport pc = (PEPeerTransport) nonChoking.remove(0);
      pc.sendChoke();
    }

    //4. If we lack unchoke pple, find new ones ;)
    if (nbUnchoke > nonChoking.size()) {
      //4.1 Determine the N (nbUnchoke best peers)
      //Maybe we'll need some other test when we are a seed ...
      prepareBestUnChokedPeers(nbUnchoke - nonChoking.size());
    }

    //3. Only Choke-Unchoke Every 10 secs
    if ((_loopFactor % 100) != 0)
      return;

    //3. if non unchoke possibilities we should cancel all chokes
    //   and return;              
    /*if (nbUnchoke < 1) {
      for (int i = 0; i < nonChoking.size(); i++) {
        PeerSocket pc = (PeerSocket) nonChoking.get(i);
        pc.sendChoke();
      }
      return;
    }*/

    //4. Determine the N (nbUnchoke best peers)
    //   Maybe we'll need some other test when we are a seed ...
    Vector bestUploaders = getBestUnChokedPeers(nbUnchoke);

    //  optimistic unchoke
    if ((_loopFactor % 300) == 0 || (currentOptimisticUnchoke == null)) {
      performOptimisticUnChoke(bestUploaders);
    }
    if (currentOptimisticUnchoke != null)
      bestUploaders.add(currentOptimisticUnchoke);

    for (int i = 0; i < bestUploaders.size(); i++) {
      PEPeerTransport pc = (PEPeerTransport) bestUploaders.get(i);
      if (nonChoking.contains(pc)) {
        nonChoking.remove(pc);
      }
      else {
        pc.sendUnChoke();
      }
    }

    for (int i = 0; i < nonChoking.size(); i++) {
      PEPeerTransport pc = (PEPeerTransport) nonChoking.get(i);
      pc.sendChoke();
    }
  }

  // refactored out of unChoke() - Moti
  private void prepareBestUnChokedPeers(int numUpRates) {
    if (numUpRates <= 0)
      return;

    int[] upRates = new int[numUpRates];
    Arrays.fill(upRates, 0);

    Vector bestUploaders = new Vector();
    for (int i = 0; i < _connections.size(); i++) {
      PEPeerTransport pc = null;
      try {
        pc = (PEPeerTransport) _connections.get(i);
      }
      catch (Exception e) {
        break;
      }
      if (pc.isInteresting() && pc.isChoking()) {
        int upRate = pc.getStats().getReception();
        testAndSortBest(upRate, upRates, pc, bestUploaders, 0);
      }
    }

    for (int i = 0; i < bestUploaders.size(); i++) {
      PEPeerTransport pc = (PEPeerTransport) bestUploaders.get(i);
      pc.sendUnChoke();
    }
  }

  // refactored out of unChoke() - Moti
  private Vector getBestUnChokedPeers(int nbUnchoke) {
    int[] upRates = new int[nbUnchoke - 1];
    Arrays.fill(upRates, 0);

    Vector bestUploaders = new Vector();
    synchronized (_connections) {
      for (int i = 0; i < _connections.size(); i++) {
        PEPeerTransport pc = null;
        try {
          pc = (PEPeerTransport) _connections.get(i);
        }
        catch (Exception e) {
          continue;
        }
        if (pc != currentOptimisticUnchoke && pc.isInteresting()) {
          int upRate = 0;
          if (_finished) {
            upRate = pc.getStats().getStatisticSentRaw();
            if (pc.isSnubbed())
              upRate = -1;
          }
          else
            upRate = pc.getStats().getReception();
          if (upRate > 256)
            testAndSortBest(upRate, upRates, pc, bestUploaders, 0);
        }
      }
    }

    if (!_finished && bestUploaders.size() < upRates.length) {
      synchronized (_connections) {
        for (int i = 0; i < _connections.size(); i++) {
          PEPeerTransport pc = null;
          try {
            pc = (PEPeerTransport) _connections.get(i);
          }
          catch (Exception e) {
            break;
          }
          if (pc != currentOptimisticUnchoke
            && pc.isInteresting()
            && pc.isInterested()
            && bestUploaders.size() < upRates.length
            && !pc.isSnubbed()
            && (pc.getStats().getTotalSentRaw() / (pc.getStats().getTotalReceivedRaw() + 16000)) < 10) {
            bestUploaders.add(pc);
          }
        }
      }
    }

    if (bestUploaders.size() < upRates.length) {
      int start = bestUploaders.size();
      synchronized (_connections) {
        for (int i = 0; i < _connections.size(); i++) {
          PEPeerTransport pc = null;
          try {
            pc = (PEPeerTransport) _connections.get(i);
          }
          catch (Exception e) {
            continue;
          }
          if (pc != currentOptimisticUnchoke && pc.isInteresting()) {
            int upRate = 0;
            //If peer we'll use the overall uploaded value
            if (!_finished)
              upRate = (int) pc.getStats().getTotalReceivedRaw();
            else {
              upRate = pc.getPercentDone();
              if (pc.isSnubbed())
                upRate = -1;
            }
            testAndSortBest(upRate, upRates, pc, bestUploaders, start);
          }
        }
      }
    }
    return bestUploaders;
  }

  // refactored out of unChoke() - Moti
  private void performOptimisticUnChoke(Vector bestUploaders) {
    int index = 0;
    synchronized (_connections) {
      for (int i = 0; i < _connections.size(); i++) {
        PEPeerTransport pc = (PEPeerTransport) _connections.get(i);
        if (pc == currentOptimisticUnchoke)
          index = i + 1;
      }
      if (index >= _connections.size())
        index = 0;

      currentOptimisticUnchoke = null;

      for (int i = index; i < _connections.size() + index; i++) {
        PEPeerTransport pc = (PEPeerTransport) _connections.get(i % _connections.size());
        if (!pc.isSeed() && !bestUploaders.contains(pc) && pc.isInteresting() && !pc.isSnubbed()) {
          currentOptimisticUnchoke = pc;
          break;
        }
      }
    }
  }

  // refactored out of unChoke() - Moti
  private Vector getNonChokingPeers() {
    Vector nonChoking = new Vector();
    synchronized (_connections) {
      for (int i = 0; i < _connections.size(); i++) {
        PEPeerTransport pc = null;
        try {
          pc = (PEPeerTransport) _connections.get(i);
        }
        catch (Exception e) {
          continue;
        }
        if (!pc.isChoking()) {
          nonChoking.add(pc);
        }
      }
    }
    return nonChoking;
  }

  private void testAndSortBest(int upRate, int[] upRates, PEPeerTransport pc, Vector best, int start) {
    int i;
    for (i = start; i < upRates.length; i++) {
      if (upRate >= upRates[i])
        break;
    }
    if (i < upRates.length) {
      best.add(i, pc);
      for (int j = upRates.length - 2; j >= i; j--) {
        upRates[j + 1] = upRates[j];
      }
      upRates[i] = upRate;
    }
    if (best.size() > upRates.length)
      best.remove(upRates.length);
  }

  /*
    private static void testAndSortWeakest(int upRate, int[] upRates, PeerSocket pc, Vector worst) {
      int i;
      for (i = 0; i < upRates.length; i++) {
        if (upRate <= upRates[i])
          break;
      }
      if (i < upRates.length) {
        worst.add(i, pc);
        for (int j = i; j < upRates.length - 1; j++) {
          upRates[j + 1] = upRates[j];
        }
        upRates[i] = upRate;
      }
      if (worst.size() > upRates.length)
        worst.remove(upRates.length);
    }
  */

  //send the have requests out
  private void sendHave(int pieceNumber) {
    //for all clients
    synchronized (_connections) {
      for (int i = 0; i < _connections.size(); i++) {
        //get a peer connection
        PEPeerTransport pc = (PEPeerTransport) _connections.get(i);
        //send the have message
        pc.sendHave(pieceNumber);
      }
    }
  }

  //Methods that checks if we are connected to another seed, and if so, disconnect from him.
  private void checkSeeds() {
    //If we are not ourself a seed, return
    if (!_finished || !COConfigurationManager.getBooleanParameter("Disconnect Seed", false)) //$NON-NLS-1$
      return;
    synchronized (_connections) {
      for (int i = 0; i < _connections.size(); i++) {
        PEPeerTransport pc = (PEPeerTransport) _connections.get(i);
        if (pc != null && pc.getState() == PEPeer.TRANSFERING && pc.isSeed()) {
          pc.closeAll(false);
        }
      }
    }
  }

  private void updateStats() {
    _seeds = _peers = 0;
    //calculate seeds vs peers
    synchronized (_connections) {
      for (int i = 0; i < _connections.size(); i++) {
        PEPeerTransport pc = (PEPeerTransport) _connections.get(i);
        if (pc.getState() == PEPeer.TRANSFERING)
          if (pc.isSeed())
            _seeds++;
          else
            _peers++;
      }
    }
  }
  /**
   * The way to unmark a request as being downloaded.
   * Called by Peer connections objects when connection is closed or choked
   * @param request
   */
  public void requestCanceled(DiskManagerRequest request) {
    int pieceNumber = request.getPieceNumber(); //get the piece number
    int pieceOffset = request.getOffset(); //get the piece offset    
    PEPiece piece = _pieces[pieceNumber]; //get the piece
    if (piece != null)
      piece.unmarkBlock(pieceOffset / BLOCK_SIZE);
    //set as not being retrieved
    _downloading[pieceNumber] = false; //mark as not downloading
  }

  /**
   * This method is used by BtServer to add an incoming connection
   * to the list of peer connections.
   * @param sckClient the incoming connection socket
   */
  public void addPeerTransport(PEPeerTransport trans) {
    
    this.insertPeerSocket( trans );
  }


  /**
   * The way to remove a peer from our peer list.
   * @param pc
   */
  public void removePeer(PEPeerTransport pc) {
    synchronized (_connections) {
      _connections.remove(pc);
    }
  }

  //get the hash value
  public byte[] getHash() {
    return _hash;
  }

  //get the peer id value
  public byte[] getPeerId() {
    return _myPeerId;
  }

  //get the piece length
  public int getPieceLength() {
    return _diskManager.getPieceLength();
  }

  //get the number of pieces
  public int getPiecesNumber() {
    return _nbPieces;
  }

  //get the status array
  public boolean[] getPiecesStatus() {
    return _downloaded;
  }

  //get the remaining percentage
  public long getRemaining() {
    return _diskManager.getRemaining();
  }

  //:: possibly rename to setRecieved()? -Tyler
  //set recieved bytes
  public void received(int length) {
    if (length > 0) {
      _stats.received(length);
      _averageReceptionSpeed.addValue(length);
    }
    _manager.received(length);
  }

  public void discarded(int length) {
    if (length > 0) {
      _stats.discarded(length);
    }
    _manager.discarded(length);
  }
  //::possibly update to setSent() -Tyler
  //set the send value
  public void sent(int length) {
    if (length > 0)
      _stats.sent(length);
    _manager.sent(length);
  }

  //setup the diskManager
  public void setDiskManager(DiskManager diskManager) {
    //the diskManager that handles read/write operations
    _diskManager = diskManager;
    _downloaded = _diskManager.getPiecesStatus();
    _nbPieces = _diskManager.getPiecesNumber();

    //the bitfield indicating if pieces are currently downloading or not
    _downloading = new boolean[_nbPieces];
    Arrays.fill(_downloading, false);

    _piecesRarest = new boolean[_nbPieces];

    //the pieces
    _pieces = diskManager.getPieces();
    if (_pieces == null)
      _pieces = new PEPiece[_nbPieces];
    for (int i = 0; i < _pieces.length; i++) {
      if (_pieces[i] != null) {
        _pieces[i].setManager(this);
        pieceAdded(_pieces[i]);
      }
    }

    //the availability level of each piece in the network
    _availability = new int[_nbPieces];

    //the stats
    _stats = new PEPeerStatsImpl(diskManager.getPieceLength());

    _server.startServer();

    this.start();
  }

  public long downloaded() {
    return _stats.getTotalReceivedRaw();
  }

  public long uploaded() {
    return _stats.getTotalSentRaw();
  }

  public void haveNewPiece() {
    _stats.haveNewPiece();
  }

  public void blockWritten(int pieceNumber, int offset) {
    PEPiece piece = _pieces[pieceNumber];
    if (piece != null) {
      piece.setWritten(offset / BLOCK_SIZE);
    }
  }

  public void writeBlock(int pieceNumber, int offset, ByteBuffer data) {
    PEPiece piece = _pieces[pieceNumber];
    int blockNumber = offset / BLOCK_SIZE;
    if (piece != null && !piece.isWritten(blockNumber)) {
      piece.setBloc(blockNumber);
      _diskManager.writeBlock(pieceNumber, offset, data);
    }
  }

  public boolean checkBlock(int pieceNumber, int offset, int length) {
    return _diskManager.checkBlock(pieceNumber, offset, length);
  }

  public boolean checkBlock(int pieceNumber, int offset, ByteBuffer data) {
    return _diskManager.checkBlock(pieceNumber, offset, data);
  }

  public int getAvailability(int pieceNumber) {
    synchronized (_availability) {
      return _availability[pieceNumber];
    }
  }

  public int[] getAvailability() {
    synchronized (_availability) {
      return _availability;
    }
  }

  public void havePiece(int pieceNumber, PEPeer pcOrigin) {
    int length = getPieceLength(pieceNumber);
    int availability = _availability[pieceNumber];
    if (availability < 4) {
      if (_downloaded[pieceNumber])
        availability--;
      if (availability <= 0)
        return;
      //for all peers
      synchronized (_connections) {
        for (int i = _connections.size() - 1; i >= 0; i--) {
          PEPeerTransport pc = (PEPeerTransport) _connections.get(i);
          if (pc != null && pc != pcOrigin && pc.getState() == PEPeer.TRANSFERING) {
            boolean[] peerAvailable = pc.getAvailable();
            if (peerAvailable[pieceNumber])
              ((PEPeerStatsImpl)pc.getStats()).staticticSent(length / availability);
          }
        }
      }
    }
  }

  public int getPieceLength(int pieceNumber) {
    if (pieceNumber == _nbPieces - 1)
      return _diskManager.getLastPieceLength();
    return _diskManager.getPieceLength();
  }

  public boolean validateHandshaking(PEPeer pc, byte[] peerId) {
    PEPeerTransport pcTest = PEPeerTransportFactory.createTransport(this, peerId, pc.getIp(), pc.getPort(), true);
    synchronized (_connections) {
      return !_connections.contains(pcTest);
    }
  }

  public int getNbPeers() {
    return _peers;
  }

  public int getNbSeeds() {
    return _seeds;
  }

  public PEPeerStats getStats() {
    return _stats;
  }

  public String getTrackerStatus() {
    return _trackerStatus;
  }

  public String getETA() {
    String remaining;
    int writtenNotChecked = 0;
    for (int i = 0; i < _pieces.length; i++) {
      if (_pieces[i] != null) {
        writtenNotChecked += _pieces[i].getCompleted() * BLOCK_SIZE;
      }
    }
    long dataRemaining = _diskManager.getRemaining() - writtenNotChecked;
    if (dataRemaining == 0) {
      
      long timeElapsed = _timeFinished - _timeStarted;
      if(timeElapsed > 0) {
        remaining = MessageText.getString("PeerManager.status.finishedin"); //$NON-NLS-1$
        remaining += " " + TimeFormater.format(timeElapsed);
      } else {
        remaining = MessageText.getString("PeerManager.status.finished"); //$NON-NLS-1$
      }
    }
    else {
      int averageSpeed = _averageReceptionSpeed.getAverage();
      if (averageSpeed < 256) {
        remaining = "oo"; //$NON-NLS-1$
      }
      else {
        long timeRemaining = dataRemaining / averageSpeed;
        remaining = TimeFormater.format(timeRemaining);
      }
    }
    return remaining;
  }

  public void peerAdded(PEPeer pc) {
    _manager.addPeer(pc);
  }

  public void peerRemoved(PEPeer pc) {
    _manager.removePeer(pc);
  }

  public void pieceAdded(PEPiece p) {
    _manager.addPiece(p);
  }

  public void pieceRemoved(PEPiece p) {
    _manager.removePiece(p);
  }

  public String getElpased() {
    return TimeFormater.format(System.currentTimeMillis() / 1000 - _timeStarted);
  }

  public String getDownloaded() {
    return _stats.getTotalReceived();
  }

  public String getUploaded() {
    return _stats.getTotalSent();
  }

  public String getDownloadSpeed() {
    return _stats.getReceptionSpeed();
  }

  public String getUploadSpeed() {
    return _stats.getSendingSpeed();
  }

  public String getTotalSpeed() {
    return _stats.getOverAllDownloadSpeed();
  }

  public int getTrackerTime() {
    return _timeToWait - (int) (System.currentTimeMillis() / 1000 - _timeLastUpdate);
  }

  public void pieceChecked(int pieceNumber, boolean result) {
    this.pieceRemoved(_pieces[pieceNumber]);
    //  the piece has been written correctly
    if (result) {

      PEPiece piece = _pieces[pieceNumber];
      if(piece != null)
        piece.free();
      _pieces[pieceNumber] = null;

      //mark this piece as downloaded
      _downloaded[pieceNumber] = true;

      //send all clients an have message
      sendHave(pieceNumber);

    }
    else {

      _pieces[pieceNumber].free();
      _pieces[pieceNumber] = null;

      //Mark this piece as non downloading
      _downloading[pieceNumber] = false;
      
      //Mark this piece as not downloaded (shouldn't change anything)
      _downloaded[pieceNumber] = false;
      
      //We haven't finished (to recover from a wrong finish state)
      _finished = false;
            
      nbHashFails++;
    }
  }

  public void enqueueReadRequest(DiskManagerDataQueueItem item) {
    _diskManager.enqueueReadRequest(item);
  }

  /**
   * @return
   */
  public List get_connections() {
    return _connections;
  }

  public PEPiece[] getPieces() {
    return _pieces;
  }

  public int getDownloadPriority() {
    return _manager.getPriority();
  }

  public void freeRequest(DiskManagerDataQueueItem item) {
    synchronized (requestsToFree) {
      requestsToFree.add(item);
    }
  }

  private void freeRequests() {
    if (requestsToFree == null)
      return;
    synchronized (requestsToFree) {
      for (int i = 0; i < requestsToFree.size(); i++) {
        DiskManagerDataQueueItem item = (DiskManagerDataQueueItem) requestsToFree.get(i);
        if (item.isLoaded()) {
          requestsToFree.remove(item);
          i--;
          ByteBufferPool.getInstance().freeBuffer(item.getBuffer());
        }
        if (!item.isLoading()) {
          requestsToFree.remove(item);
          i--;
        }
      }
    }
  }

  public boolean isOptimisticUnchoke(PEPeer pc) {
    return pc == currentOptimisticUnchoke;
  }


  /**
   * @return
   */
  public int getNbHashFails() {
    return nbHashFails;
  }
  
  public PEPeerStats
  createPeerStats()
  {
  	return( new PEPeerStatsImpl( getPieceLength()));
  }

  public DiskManagerRequest
  createDiskManagerRequest(
  	int pieceNumber,
  	int offset,
  	int length )
  {
  	return( _diskManager.createRequest( pieceNumber, offset, length ));
  }
  
  public DiskManagerDataQueueItem
  createDiskManagerDataQueueItem(
  	DiskManagerRequest	req )
  {
  	return( _diskManager.createDataQueueItem( req ));
  }
  
}