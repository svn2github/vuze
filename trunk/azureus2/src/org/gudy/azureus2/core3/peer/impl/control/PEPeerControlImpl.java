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
 
package org.gudy.azureus2.core3.peer.impl.control;


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
import org.gudy.azureus2.core3.ipfilter.BadIps;
import org.gudy.azureus2.core3.ipfilter.IpFilter;
import org.gudy.azureus2.core3.ipfilter.impl.*;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.peer.impl.*;


public class 
PEPeerControlImpl
	implements 	PEPeerControl, ParameterListener
{
  private static final int MAX_REQUESTS = 16;
  private static final int BAD_CHUNKS_LIMIT = 3;
  private static final int WARNINGS_LIMIT = 3;
  private static int maxConnections = COConfigurationManager.getIntParameter("Max Clients");
  
  private int peer_manager_state = PS_INITIALISED;
  
  private int[] _availability;
  private boolean _bContinue;                
  private ArrayList _peer_transports;
  private DiskManager _diskManager;
  private boolean[] _downloaded;
  private boolean[] _downloading;
  private boolean _finished;
  protected boolean[] _piecesRarest;
  private byte[] _hash;
  private int _loopFactor;
  private byte[] _myPeerId;
  private int _nbPieces;
  private PEPieceImpl[] 				_pieces;
  private PEPeerServerHelper 			_server;
  private PEPeerManagerStatsImpl 		_stats;
  private TRTrackerClient _tracker;
   //  private int _maxUploads;
  private int _seeds, _peers,_remotes;
  private long _timeStarted;
  private long _timeFinished;
  private Average _averageReceptionSpeed;
  private PEPeerTransport currentOptimisticUnchoke;
  
  
  //A flag to indicate when we're in endgame mode
  private boolean endGameMode;
  //The list of chunks needing to be downloaded (the mechanism change when entering end-game mode)
  private List endGameModeChunks;
  
  private DownloadManager _manager;
  private List requestsToFree;
  private PeerUpdater peerUpdater;
  
  boolean slowConnect;
  private SlowConnector slowConnector;
  private List slowQueue;
  
  private int nbHashFails;
  private static final int MAINLOOP_WAIT_TIME   = 500;
  private static final int CHOKE_UNCHOKE_FACTOR = 10000 / MAINLOOP_WAIT_TIME; //every 10s
  private static final int OPT_UNCHOKE_FACTOR   = 30000 / MAINLOOP_WAIT_TIME; //every 30s

  private List	peer_manager_listeners 		= new ArrayList();
  private List	peer_transport_listeners 	= new ArrayList();
  
  private List failedPieceChecks = new ArrayList();
  private List successPieceChecks = new ArrayList();
  
  private boolean superSeedMode;
  private int superSeedModeCurrentPiece;
  private int superSeedModeNumberOfAnnounces;
  private SuperSeedPiece[] superSeedPieces;
  
  public PEPeerControlImpl(
    DownloadManager 	manager,
    PEPeerServerHelper 	server,
	TRTrackerClient 	tracker,
    DiskManager 		diskManager) {
    	
 	_server = server;
    this._manager = manager;
	_tracker = tracker;
	this._diskManager = diskManager;
    COConfigurationManager.addParameterListener("Max Clients", this);
 }
  
	public DownloadManager
	getDownloadManager()
	{
		return( _manager );
	}
 
	public int
	getState()
	{
		return( peer_manager_state );
	}
	
  public void
  start()
  {  
    
    endGameMode = false;
    //This torrent Hash
    
    try{
    
    	_hash = _tracker.getTorrent().getHash();
    	
    }catch( TOTorrentException e ){
    	
    		// this should never happen - TODO: tidy when refactoring peer manager
    	e.printStackTrace();
    	
    	_hash = new byte[20]; // TODO: just for the mo
    }
    
    this.nbHashFails = 0;

    //The connection to the tracker
    _tracker.setAnnounceDataProvider(
    		new TrackerClientAnnounceDataProvider()
    		{
    			public long
    			getTotalSent()
    			{
    				return(getStats().getTotalSent());
    			}
    			public long
    			getTotalReceived()
    			{
    				return(getStats().getTotalReceived());
    			}
    			
    			public long
    			getRemaining()
    			{
    				return( PEPeerControlImpl.this.getRemaining());
    			}
                
          public int
          getConnectionCount()
          {
            return( getNbSeeds() + getNbPeers() );
          }
    		});
    
    _myPeerId = _tracker.getPeerId();

    //The peer connections
    _peer_transports = new ArrayList();

    //The Server that handle incoming connections
     _server.setServerAdapter(this);

    _diskManager.setPeerManager(this);

    //BtManager is threaded, this variable represents the
    // current loop iteration. It's used by some components only called
    // at some specific times.
    _loopFactor = 0;

    //The current tracker state
    //this could be start or update

    _averageReceptionSpeed = Average.getInstance(1000, 30);

    
    setDiskManager(_diskManager);
    
    superSeedMode = (COConfigurationManager.getBooleanParameter("Use Super Seeding") && this.getRemaining() == 0);
    superSeedModeCurrentPiece = 0;
    superSeedPieces = new SuperSeedPiece[_nbPieces];
    for(int i = 0 ; i < _nbPieces ; i++) {
      superSeedPieces[i] = new SuperSeedPiece(this,i);
    }
    
    
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
    
    new Thread( "Peer Manager"){
      public void
      run()
      {
        mainLoop();
      }
    }.start();
  }

  private class PeerUpdater extends Thread {
    private boolean bContinue = true;

    private long started[];

    public PeerUpdater() {
      super("Peer Updater"); //$NON-NLS-1$
      setPriority(Thread.NORM_PRIORITY - 1);
      started = new long[10];
    }

    public void run() {
      while (bContinue) {
        
      	for (int i = 9; i > 0; i--) {
          started[i] = started[i - 1];
        }
        started[0] = System.currentTimeMillis();
        
        synchronized (_peer_transports) {
          for (int i=0; i < _peer_transports.size(); i++) {
            PEPeerTransport ps = (PEPeerTransport) _peer_transports.get(i);
            
            if (ps.getState() == PEPeer.DISCONNECTED) {
              _peer_transports.remove(i);
              
              for (int m=0; m < peer_transport_listeners.size(); m++) {
                ((PEPeerControlListener)peer_transport_listeners.get(m)).peerRemoved( ps );
              }
            }
            else {
              if ( System.currentTimeMillis() > (ps.getLastReadTime() + ps.getReadSleepTime()) ) {
                ps.setReadSleepTime( ps.processRead() );
                ps.setLastReadTime( System.currentTimeMillis() );
              }
              
              ps.processWrite();
            }
          }
        }
                
        try {
        	long wait = 20;
          wait -= (System.currentTimeMillis() - started[0]);
          if (started[4] != 0) {
            for (int i = 0; i < 9; i++) {
              wait += 20 + (started[i + 1] - started[i]);
            }
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
                     synchronized (_peer_transports) {
                        //System.out.println("new slow connect: " + (System.currentTimeMillis() /1000));
                        /* add connection */
                     	addToPeerTransports(PEPeerTransportFactory.createTransport(testPS.getManager(), testPS.getId(), testPS.getIp(), testPS.getPort(), false));               
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
  public void mainLoop() {
    _bContinue = true;
    _manager.setState(DownloadManager.STATE_DOWNLOADING);
    _timeStarted = System.currentTimeMillis();
    while (_bContinue) //loop until stopAll() kills us
      {
      try {
        long timeStart = System.currentTimeMillis();
        
        checkTracker(); //check the tracker status, update peers
        processPieceChecks();
        checkCompletedPieces();  //check to see if we've completed anything else
        computeAvailability(); //compute the availablity                   
        updateStats();
        freeRequests();
        
        if (!_finished) { //if we're not finished
          checkFinished(); //see if we've finished
          if(!_finished) {
            _diskManager.computePriorityIndicator();
            checkRequests(); //check the requests               
            checkDLPossible(); //look for downloadable pieces
          }
        }
        
        checkSeeds(false);
        updatePeersInSuperSeedMode();
        unChoke();

        _loopFactor++; //increment the loopFactor
        
        long timeWait = MAINLOOP_WAIT_TIME - (System.currentTimeMillis() - timeStart);
        if (timeWait > 10) {
        	Thread.sleep(timeWait); //sleep
        }

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

    _server.clearServerAdapter();

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
    t.setDaemon(true);
    t.start();

    //  4. Close all clients
    if (_peer_transports != null) {
      synchronized (_peer_transports) {
        while (_peer_transports.size() != 0) {
          PEPeerTransport pc = removeFromPeerTransports(0);
          pc.closeAll("Closing all Connections",false, false);
        }
      }
    }
    
    // 5. Remove listeners
    COConfigurationManager.removeParameterListener("Max Clients", this);
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
  		 				
    	TRTrackerResponsePeer[]	peers = tracker_response.getPeers();
      	    	
    	if ( peers != null ){
    		
        	addPeersFromTracker( tracker_response.getPeers());  
    	}
    	
        Map extensions = tracker_response.getExtensions();
        	
        if (extensions != null ){
        		
        	System.out.println( "PEPeerControl: tracker response contained extensions");
        		
        	addExtendedPeersFromTracker( extensions );
    	}
  	}

	public void
	processTrackerResponse(
		TRTrackerResponse	response )
	{
		analyseTrackerResponse( response );
	}

	private void
	addExtendedPeersFromTracker(
		Map		extensions )
	{
		Map	protocols = (Map)extensions.get("protocols");
		
		Iterator protocol_it = protocols.keySet().iterator();
		
		while( protocol_it.hasNext()){
			
			String	protocol_name = (String)protocol_it.next();
			
			Map	protocol = (Map)protocols.get(protocol_name);
			
			List	transports = PEPeerTransportFactory.createExtendedTransports( this, protocol_name, protocol );
			
			synchronized( _peer_transports ){
					
				for (int i=0;i<transports.size();i++){
					
					PEPeerTransport	transport = (PEPeerTransport)transports.get(i);
					
					if ( !_peer_transports.contains(transport)){
						
						addToPeerTransports( transport.getRealTransport());
						
					}else{
					  
						transport.closeAll("Already Connected",false,false);
					}
				}
			}
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
				
			// System.out.println( "addPeer: myPeerId=" + ByteFormatter.nicePrint(_myPeerId ) + ", received = " + ByteFormatter.nicePrint(this_peer_id));
			
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
   * Private method to process the results given by DiskManager's
   * piece checking thread via asyncPieceChecked(..)
   */
  private void processPieceChecks() {
    
    // process complete piece results
    synchronized( successPieceChecks ) {
    	Iterator it = successPieceChecks.iterator();
      while (it.hasNext()) {
        Integer pieceNum = (Integer)it.next();
        it.remove();
        pieceChecked(pieceNum.intValue(), true);
      }
    }
    
    // process incomplete piece results
    synchronized( failedPieceChecks ) {
      Iterator it = failedPieceChecks.iterator();
      while (it.hasNext()) {
        Integer pieceNum = (Integer)it.next();
        it.remove();
        pieceChecked(pieceNum.intValue(), false);
      }
    }
    
  }
  
  

  /**
   * This method scans all peers and if downloading from them is possible,
   * it will try to find blocks to download.
   * If the peer is marked as snubbed then only one block will be downloaded
   * otherwise it will queue up 16 requests.
   * 
   */
  private void checkDLPossible() {
    //::updated this method to work with List -Tyler
    //for all peers
    List bestUploaders = new ArrayList();
    synchronized (_peer_transports) {
      long[] upRates = new long[_peer_transports.size()];
      Arrays.fill(upRates, -1);

      for (int i = 0; i < _peer_transports.size(); i++) {
        PEPeerTransport pc = null;
        try {
          pc = (PEPeerTransport) _peer_transports.get(i);
        }
        catch (Exception e) {
          e.printStackTrace();
          break;
        }
        if (pc.transferAvailable()) {
        	long upRate = pc.getStats().getReception();
          testAndSortBest(upRate, upRates, pc, bestUploaders, 0);
        }
      }
    }
    
    checkEndGameMode();

    for (int i = 0; i < bestUploaders.size(); i++) {
      //get a connection 
      PEPeerTransport pc = (PEPeerTransport) bestUploaders.get(i);
      //can we transfer something?
      if (pc.transferAvailable()) {
        boolean found = true;
        //If queue is too low, we will enqueue another request
        int maxRequests = MAX_REQUESTS;
        if (endGameMode)
          maxRequests = 2;
        if (pc.isSnubbed())
          maxRequests = 1;        
        while ((pc.isReadyToRequest() && pc.getState() == PEPeer.TRANSFERING) && found && (pc.getNbRequests() < maxRequests)) {
          if(endGameMode)
            found = findPieceInEndGameMode(pc);
          else
            found = findPieceToDownload(pc);
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
      
      if(endGameMode) {
	      synchronized(endGameModeChunks) {
	        endGameMode = false;
	        endGameModeChunks.clear();
	      }
      }
      
      boolean resumeEnabled = COConfigurationManager.getBooleanParameter("Use Resume", true);
      
      _manager.setState(DownloadManager.STATE_FINISHING);
      _timeFinished = System.currentTimeMillis();
            
      //remove previous snubbing
      synchronized (_peer_transports) {
        for (int i = 0; i < _peer_transports.size(); i++) {
          PEPeerTransport pc = (PEPeerTransport) _peer_transports.get(i);
          if (pc != null) {
            pc.setSnubbed(false);
          }
        }
      }
      
      //Disconnect seeds
      checkSeeds(true);
      
      boolean checkPieces = COConfigurationManager.getBooleanParameter("Check Pieces on Completion", true);
      
      long	run_time = (_timeFinished - _timeStarted)/1000;	// secs
      
      boolean	looks_like_restart = run_time < 10;
      
      //re-check all pieces to make sure they are not corrupt
      if (checkPieces && !looks_like_restart) {
        for(int i=0; i < _downloaded.length; i++) {
          _diskManager.aSyncCheckPiece(i);
        }
      }
      
      boolean moveWhenDone = COConfigurationManager.getBooleanParameter("Move Completed When Done", false);
      if (moveWhenDone) {
        String newName = _diskManager.moveCompletedFiles();
        if (newName.length() > 0) _manager.setTorrentFileName(newName);
      }
      
      //update resume data
      if (resumeEnabled) _diskManager.dumpResumeDataToDisk(true, false);
      
      
      _manager.setState(DownloadManager.STATE_SEEDING);
      
      if ( !looks_like_restart ){
      
      	_manager.downloadEnded();
      }
            		
      _tracker.complete( looks_like_restart );
    }
  }

  /**
   * This method will locate any expired request, will cancel it, and mark the peer as snubbed
   *
   */
  private void checkRequests() {
    //for every connection
    synchronized (_peer_transports) {
      for (int i = 0; i < _peer_transports.size(); i++) {
        PEPeerTransport pc = (PEPeerTransport) _peer_transports.get(i);
        if (pc.getState() == PEPeer.TRANSFERING) {
          List expired = pc.getExpiredRequests();

          if (expired != null && expired.size() > 0) {
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
              pc.sendCancel(request); //cancel the request object
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
  private void 
  checkTracker() 
  {
  	int		percentage 			= 100;
  	boolean	use_minimum_delay	= false;
  	
    //if we're not downloading, use normal re-check rate
    if (_manager.getState() != DownloadManager.STATE_DOWNLOADING) {
    }else{
    	// calculate new rate
    	
      int swarmPeers = 0;
      int swarmSeeds = 0;
      int tempMaxConnections = maxConnections;
      int currentConnectionCount = _peer_transports.size();
      TRTrackerScraperResponse tsr = _manager.getTrackerScrapeResponse();
    
      //get current scrape values
      if (tsr != null && tsr.isValid()) {
        swarmPeers = tsr.getPeers();
        swarmSeeds = tsr.getSeeds();
      }
      
      //limit maximum number of peers to calculate with
      if (tempMaxConnections == 0 || tempMaxConnections > 100) tempMaxConnections = 100;
      
      //lower limit to swarm size if necessary
      if (tempMaxConnections > (swarmPeers + swarmSeeds)) {
        tempMaxConnections = swarmPeers + swarmSeeds;
      }
      
      //use only 3/4 the value
      tempMaxConnections = (int)(tempMaxConnections * .75);

      //if already over the limit, don't shorten the time
      if (currentConnectionCount >= tempMaxConnections){
      //if no connections, recheck in 1 minute
      }else if (currentConnectionCount == 0){
      
      	use_minimum_delay	= true;
      //otherwise...
      }else {
        //calculate the new wait time
        float currentConnectionPercent = ((float)currentConnectionCount) / tempMaxConnections;
        
		percentage = (int)(currentConnectionPercent*100);
 
      }
    }
   
   	_tracker.setRefreshDelayOverrides( use_minimum_delay, percentage );
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
    synchronized (_peer_transports) {
      for (int i = _peer_transports.size() - 1; i >= 0; i--) //::Possible optimization to break early when you reach 100%
        {
        //get the peer connection
        PEPeerTransport pc = (PEPeerTransport) _peer_transports.get(i);

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
      System.arraycopy(availability, 0, _availability, 0, availability.length);
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
  private boolean findPieceToDownload(PEPeerTransport pc) {
    //get the rarest pieces list
    getRarestPieces(pc,90);
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

    //If we're not going to continue a piece, then stick with more rarity :
    //Allowing 20% here.
    getRarestPieces(pc,20);
    
    pieceNumber = _diskManager.getPiecenumberToDownload(_piecesRarest);

    if (pieceNumber == -1)
      return false;
    //Now we should have a piece with least presence on network    
    PEPieceImpl piece = null;

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

  private void getRarestPieces(PEPeerTransport pc,int rangePercent) {
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
    pieceMinAvailability = ((100+rangePercent) * pieceMinAvailability) / 100;

    //If availability is greater than 10, then grab any piece avail (999 should be enough)
    if(pieceMinAvailability > 10 && pieceMinAvailability < 999) pieceMinAvailability = 999;
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
    /* create a peer socket for testing purposes */
    PEPeerTransport testPS = PEPeerTransportFactory.createTransport(this, peerId, ip, port, true);
    
    if (!IpFilterImpl.getInstance().isInRange(ip)) {
       synchronized (_peer_transports) {
          if (!_peer_transports.contains(testPS)) {
             if (maxConnections == 0 || _peer_transports.size() < maxConnections) {
                /* do we need to slow down new connection creation? */
                if (slowConnect) {
                   /* add connection to be slow-connected */
                   slowQueue.add(testPS);
                   synchronized (slowQueue) { slowQueue.notify(); }
                }
                else {
                   /* add connection */
                	addToPeerTransports(PEPeerTransportFactory.createTransport(this, peerId, ip, port, false));
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
    boolean addFailed = false;
    String reason = "";
    if (!IpFilterImpl.getInstance().isInRange(ps.getIp())) {
       synchronized (_peer_transports) {
          if (!_peer_transports.contains(ps)) {
             if (maxConnections == 0 || _peer_transports.size() < maxConnections) {
                /* add connection */
             	addToPeerTransports(ps);
             }
             else {
               addFailed = true;
               reason=ps.getIp() + " : Too many connections";
             }
          }
          else {
            addFailed = true;
            reason=ps.getIp() + " : Already Connected";
          }
       }
    }
    else {
      addFailed = true;
      reason=ps.getIp() + " : Blocked IP";
    }
    
    if (addFailed) {
       ps.closeAll(reason,false, false);
    }
 }

  
  private void unChoke() {
    // Only Choke-Unchoke Every 10 secs
    if ((_loopFactor % CHOKE_UNCHOKE_FACTOR) != 0) {
      return;
    }
    
    // We retreive the current non-choking peers
    List nonChoking = getNonChokingPeers();

    // Determine how many uploads we should consider    
    int nbUnchoke = _manager.getStats().getMaxUploads();

    // Then, in any case if we have too many unchoked pple we need to choke some
    while (nbUnchoke < nonChoking.size()) {
      PEPeerTransport pc = (PEPeerTransport) nonChoking.remove(0);
      pc.sendChoke();
    }

    // If we lack unchoke pple, find new ones ;)
    if (nbUnchoke > nonChoking.size()) {
      //Determine the N (nbUnchoke best peers)
      //Maybe we'll need some other test when we are a seed ...
      prepareBestUnChokedPeers(nbUnchoke - nonChoking.size());
      nonChoking = getNonChokingPeers();
    }

    // Determine the N (nbUnchoke best peers)
    // Maybe we'll need some other test when we are a seed ...
    List bestUploaders = getBestUnChokedPeers(nbUnchoke);

    // optimistic unchoke every 30 seconds
    if ((_loopFactor % OPT_UNCHOKE_FACTOR) == 0 || (currentOptimisticUnchoke == null)) {
      performOptimisticUnChoke();
    }
    
    if(currentOptimisticUnchoke != null) {
      if(!bestUploaders.contains(currentOptimisticUnchoke)) {
        if(bestUploaders.size() > 0) {
          bestUploaders.remove(bestUploaders.size()-1);
        }
        bestUploaders.add(currentOptimisticUnchoke);
      }
    }

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

    long[] upRates = new long[numUpRates];
    Arrays.fill(upRates, 0);

    List bestUploaders = new ArrayList();
    for (int i = 0; i < _peer_transports.size(); i++) {
      PEPeerTransport pc = null;
      try {
        pc = (PEPeerTransport) _peer_transports.get(i);
      }
      catch (Exception e) {
        break;
      }
      if (pc.isInteresting() && pc.isChoking()) {
      	long upRate = pc.getStats().getReception();
        testAndSortBest(upRate, upRates, pc, bestUploaders, 0);
      }
    }

    for (int i = 0; i < bestUploaders.size(); i++) {
      PEPeerTransport pc = (PEPeerTransport) bestUploaders.get(i);
      pc.sendUnChoke();
    }
  }

  // refactored out of unChoke() - Moti
  private List getBestUnChokedPeers(int nbUnchoke) {
    long[] upRates = new long[nbUnchoke];
    Arrays.fill(upRates, 0);

    List bestUploaders = new ArrayList();
    synchronized (_peer_transports) {
      for (int i = 0; i < _peer_transports.size(); i++) {
        PEPeerTransport pc = null;
        try {
          pc = (PEPeerTransport) _peer_transports.get(i);
        }
        catch (Exception e) {
          continue;
        }
        if (pc.isInteresting()) {
        	long upRate = 0;
          if (_finished) {
            upRate = pc.getStats().getUploadAverage();
            //int totalUploaded = (int) (pc.getStats().getTotalSent() / (1024l*1024l)) + 1;
            //upRate = upRate / totalUploaded;
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
      synchronized (_peer_transports) {
        for (int i = 0; i < _peer_transports.size(); i++) {
          PEPeerTransport pc = null;
          try {
            pc = (PEPeerTransport) _peer_transports.get(i);
          }
          catch (Exception e) {
            break;
          }
          if (pc != currentOptimisticUnchoke
            && pc.isInteresting()
            && pc.isInterested()
            && bestUploaders.size() < upRates.length
            && !pc.isSnubbed()
            && (_manager.getStats().getUploaded() / (_manager.getStats().getDownloaded() + 16000)) < 10) {
            bestUploaders.add(pc);
          }
        }
      }
    }

    if (bestUploaders.size() < upRates.length) {
      int start = bestUploaders.size();
      synchronized (_peer_transports) {
        for (int i = 0; i < _peer_transports.size(); i++) {
          PEPeerTransport pc = null;
          try {
            pc = (PEPeerTransport) _peer_transports.get(i);
          }
          catch (Exception e) {
            continue;
          }
          if (pc != currentOptimisticUnchoke && pc.isInteresting()) {
            long upRate = 0;
            //If peer we'll use the overall uploaded value
            if (!_finished)
              upRate = pc.getStats().getTotalReceived();
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
  private void performOptimisticUnChoke() {
    int index = 0;
    synchronized (_peer_transports) {
      for (int i = 0; i < _peer_transports.size(); i++) {
        PEPeerTransport pc = (PEPeerTransport) _peer_transports.get(i);
        if (pc == currentOptimisticUnchoke)
          index = i + 1;
      }
      if (index >= _peer_transports.size())
        index = 0;

      currentOptimisticUnchoke = null;

      for (int i = index; i < _peer_transports.size() + index; i++) {
        PEPeerTransport pc = (PEPeerTransport) _peer_transports.get(i % _peer_transports.size());
        if (!pc.isSeed() && pc.isInteresting() && !pc.isSnubbed()) {
          currentOptimisticUnchoke = pc;
          break;
        }
      }
    }
  }

  // refactored out of unChoke() - Moti
  private List getNonChokingPeers() {
    List nonChoking = new ArrayList();
    synchronized (_peer_transports) {
      for (int i = 0; i < _peer_transports.size(); i++) {
        PEPeerTransport pc = null;
        try {
          pc = (PEPeerTransport) _peer_transports.get(i);
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

  private void testAndSortBest(long upRate, long[] upRates, PEPeerTransport pc, List best, int start) {
    if(best == null || pc == null)
      return;
    if(best.contains(pc))
      return;
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


  //send the have requests out
  private void sendHave(int pieceNumber) {
    //for all clients
    synchronized (_peer_transports) {
      for (int i = 0; i < _peer_transports.size(); i++) {
        //get a peer connection
        PEPeerTransport pc = (PEPeerTransport) _peer_transports.get(i);
        //send the have message
        pc.sendHave(pieceNumber);
      }
    }
  }

  //Method that checks if we are connected to another seed, and if so, disconnect from him.
  private void checkSeeds(boolean forceDisconnect) {
    //If we are not ourself a seed, return
    if (!forceDisconnect && ((!_finished) || !COConfigurationManager.getBooleanParameter("Disconnect Seed", true))) //$NON-NLS-1$
      return;
    synchronized (_peer_transports) {
      for (int i = 0; i < _peer_transports.size(); i++) {
        PEPeerTransport pc = (PEPeerTransport) _peer_transports.get(i);
        if (pc != null && pc.getState() == PEPeer.TRANSFERING && pc.isSeed()) {
          pc.closeAll(pc.getIp() + " : Disconnecting seeds when seed",false, false);
        }
      }
    }
  }  

  private void updateStats() {   
    //calculate seeds vs peers
    synchronized (_peer_transports) {
      _seeds = _peers = _remotes = 0;
      for (int i = 0; i < _peer_transports.size(); i++) {
        PEPeerTransport pc = (PEPeerTransport) _peer_transports.get(i);
        if (pc.getState() == PEPeer.TRANSFERING) {
          if (pc.isSeed())
            _seeds++;
          else
            _peers++;
          
          if(((PEPeer)pc).isIncoming()) {
            _remotes++;
          }
        }
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
  
  public void addPeerTransport(Object param) {
    
    this.insertPeerSocket( _server.createPeerTransport(param));
  }

  public PEPeerControl
  getControl()
  {
  	return( this );
  }

  /**
   * The way to remove a peer from our peer list.
   * @param pc
   */
  public void removePeer(PEPeerTransport pc) {
    synchronized (_peer_transports) {
    	removeFromPeerTransports(pc);
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
    _manager.getStats().received(length);
  }

  public void discarded(int length) {
    if (length > 0) {
      _stats.discarded(length);
    }
    _manager.getStats().discarded(length);
  }
  //::possibly update to setSent() -Tyler
  //set the send value
  public void sent(int length) {
    if (length > 0)
      _stats.sent(length);
    _manager.getStats().sent(length);
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
    PEPiece[] dm_pieces = diskManager.getPieces();
    
    if (dm_pieces == null){
    
      _pieces = new PEPieceImpl[_nbPieces];
      
    }else{
    	_pieces = new PEPieceImpl[ dm_pieces.length ];
    	
	    for (int i = 0; i < dm_pieces.length; i++) {
	    
	    	_pieces[i] = (PEPieceImpl)dm_pieces[i];
	    	
	      	if (_pieces[i] != null){
	      		
	        	_pieces[i].setManager(this);
	        	
	        	pieceAdded(_pieces[i]);
	      	}
	    }
    }

    //the availability level of each piece in the network
    _availability = new int[_nbPieces];

    //the stats
    _stats = new PEPeerManagerStatsImpl(diskManager.getPieceLength());

    _server.startServer();	
  }

  public void haveNewPiece() {
    _stats.haveNewPiece();
  }

  public void blockWritten(int pieceNumber, int offset,PEPeer sender) {
    PEPiece piece = _pieces[pieceNumber];
    if (piece != null) {
      piece.setWritten(sender,offset / BLOCK_SIZE);
    }    
  }

  public void writeBlock(int pieceNumber, int offset, ByteBuffer data,PEPeer sender) {
    PEPiece piece = _pieces[pieceNumber];
    int blockNumber = offset / BLOCK_SIZE;
    if (piece != null && !piece.isWritten(blockNumber)) {
      piece.setBlockWritten(blockNumber);
      _diskManager.writeBlock(pieceNumber, offset, data,sender);
      if(endGameMode) {
        //In case we are in endGame mode, remove the piece from the chunk list
        removeFromEndGameModeChunks(pieceNumber,offset);
        //For all connections cancel the request
        synchronized(_peer_transports) {
          Iterator iter = _peer_transports.iterator();
          while(iter.hasNext()) {
            PEPeerTransport connection = (PEPeerTransport) iter.next();
            connection.sendCancel(
                _diskManager.createRequest(
                    pieceNumber,
                    offset,
										_pieces[pieceNumber].getBlockSize(offset / BLOCK_SIZE)));
            
          }
        }
      }
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

  public float getMinAvailability() {
    int total = 0;
    int nbPieces;
    int allMin;
    synchronized (_availability) {
      nbPieces = _availability.length;
      if (nbPieces == 0)
        return 0;

      allMin = _availability[0];
      for (int i = 0; i < nbPieces; i++) {
        if (_availability[i] < allMin)
          allMin = _availability[i];
      }
      for (int i = 0; i < nbPieces; i++) {
        if (_availability[i] > allMin)
          total++;
      }
    }
    return allMin + (total / (float)nbPieces);
  }

  public int[] getAvailability() {
    synchronized (_availability) {
      return _availability;
    }
  }

  public void havePiece(int pieceNumber, PEPeer pcOrigin) {
    if(superSeedMode) {
      superSeedPieces[pieceNumber].peerHasPiece(pcOrigin);
      if(pieceNumber == pcOrigin.getUniqueAnnounce()) {
        pcOrigin.setUniqueAnnounce(-1);
        superSeedModeNumberOfAnnounces--;
      }      
    }
    int length = getPieceLength(pieceNumber);
    int availability = _availability[pieceNumber];
    if (availability < 4) {
      if (_downloaded[pieceNumber])
        availability--;
      if (availability <= 0)
        return;
      //for all peers
      synchronized (_peer_transports) {
        for (int i = _peer_transports.size() - 1; i >= 0; i--) {
          PEPeerTransport pc = (PEPeerTransport) _peer_transports.get(i);
          if (pc != null && pc != pcOrigin && pc.getState() == PEPeer.TRANSFERING) {
            boolean[] peerAvailable = pc.getAvailable();
            if (peerAvailable[pieceNumber])
              ((PEPeerStatsImpl)pc.getStats()).statisticSent(length / availability);
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
    synchronized (_peer_transports) {
      return !_peer_transports.contains(pcTest);
    }
  }

  public int 
  getNbPeers() 
  {
      return _peers;
  }

  public int getNbSeeds() 
  {
      return _seeds;
  }
  
  public int getNbRemoteConnections() 
  {
      return _remotes;
  }

  public PEPeerManagerStats getStats() {
    return _stats;
  }


  
  /**
   * Returns the ETA time in seconds.
   * If the returned time is 0, the download is complete.
   * If the returned time is negative, the download
   * is complete and it took -xxx time to complete.
   */
  public long getETA() {
    int writtenNotChecked = 0;
    for (int i = 0; i < _pieces.length; i++) {
      if (_pieces[i] != null) {
        writtenNotChecked += _pieces[i].getCompleted() * BLOCK_SIZE;
      }
    }
    
    long dataRemaining = _diskManager.getRemaining() - writtenNotChecked;
    if (dataRemaining == 0) {
      long timeElapsed = (_timeFinished - _timeStarted)/1000;
      //if time was spent downloading....return the time as negative
      if(timeElapsed > 1) return timeElapsed * -1;
      else return 0;
    }
    
    long averageSpeed = _averageReceptionSpeed.getAverage();
    long lETA = dataRemaining / (averageSpeed + 1);
    // stop the flickering of ETA from "Finished" to "x seconds" when we are 
    // just about complete, but the data rate is jumpy.
    if (lETA == 0)
      lETA = 1;
    return lETA;
  }
  
  	// the following three methods must be used when adding to/removing from peer transports
  	// they are also synchronized on peer_transports
  
  private void
  addToPeerTransports(
  	PEPeerTransport		peer )
  {
  	_peer_transports.add(peer);
  	
  	for (int i=0;i<peer_transport_listeners.size();i++){
  		((PEPeerControlListener)peer_transport_listeners.get(i)).peerAdded( peer );
  	}
  }
  
  private PEPeerTransport
  removeFromPeerTransports(
  		int		index )
  {
  	PEPeerTransport res = (PEPeerTransport)_peer_transports.remove(index);

	for (int i=0;i<peer_transport_listeners.size();i++){
		((PEPeerControlListener)peer_transport_listeners.get(i)).peerRemoved( res );
	}	
	
	return( res );
  } 
  
  private void
  removeFromPeerTransports(
  	PEPeerTransport		peer )
  {
  	 _peer_transports.remove(peer);
  	 
  	 for (int i=0;i<peer_transport_listeners.size();i++){
  	 	((PEPeerControlListener)peer_transport_listeners.get(i)).peerRemoved( peer );
  	 }
  }

  	// these should be replaced by above methods + listeners
  
  public void peerAdded(PEPeer pc) {
    _manager.addPeer(pc);
  }

  public void peerRemoved(PEPeer pc) {
    int piece = pc.getUniqueAnnounce();
    if(piece != -1) {
      superSeedModeNumberOfAnnounces--;
      superSeedPieces[piece].peerLeft();
    }
    _manager.removePeer(pc);
  }

  public void pieceAdded(PEPiece p) {
    _manager.addPiece(p);
  }

  public void pieceRemoved(PEPiece p) {
    _manager.removePiece(p);
  }

  public String getElapsedTime() {
    return TimeFormater.format((System.currentTimeMillis() - _timeStarted) / 1000);
  }
  
  // Returns time started in ms
  public long getTimeStarted() {
    return _timeStarted;
  }
  
  
  /**
   * DiskManager places the result of a piece check here, so that they can be
   * analyzed in the PeerControl thread asyc.
   */
  public void asyncPieceChecked ( int pieceNumber, boolean result ){
     if (result) {
        synchronized( successPieceChecks ) {
            successPieceChecks.add(new Integer(pieceNumber));
        }
     }
     else {
        synchronized( failedPieceChecks ) {
            failedPieceChecks.add(new Integer(pieceNumber));
        }
     }
  }

  
  private void pieceChecked(int pieceNumber, boolean result) {
    this.pieceRemoved(_pieces[pieceNumber]);
    //  the piece has been written correctly
    if (result) {

      PEPieceImpl piece = _pieces[pieceNumber];
      
      if(piece != null) {
        
        List list = piece.getPieceWrites();
        if(list.size() > 0) {                  
          //For each Block
          for(int i = 0 ; i < piece.getNbBlocs() ; i++) {
            //System.out.println("Processing block " + i);
            //Find out the correct hash
            List listPerBlock = piece.getPieceWrites(i);
            byte[] correctHash = null;
            //PEPeer correctSender = null;
            Iterator iterPerBlock = listPerBlock.iterator();
            while(iterPerBlock.hasNext()) {
              PEPieceWriteImpl write = (PEPieceWriteImpl) iterPerBlock.next();
              if(write.isCorrect()) {
                correctHash = write.getHash();
                //correctSender = write.getSender();
              }
            }
            //System.out.println("Correct Hash " + correctHash);
            //If it's found                       
            if(correctHash != null) {
              List peersToDisconnect = new ArrayList();
              iterPerBlock = listPerBlock.iterator();
              while(iterPerBlock.hasNext()) {
              	PEPieceWriteImpl write = (PEPieceWriteImpl) iterPerBlock.next();
                if(! Arrays.equals(write.getHash(),correctHash)) {
                  //Bad peer found here
                  PEPeer peer = write.getSender();
                  peer.hasSentABadChunk();
                  if(!peersToDisconnect.contains(peer))
                    peersToDisconnect.add(peer);                  
              }
                
              Iterator iterPeers = peersToDisconnect.iterator();
              while(iterPeers.hasNext()) {
                PEPeer peer = (PEPeer) iterPeers.next();
                badPeerDetected(peer);
               }
              }              
            }            
          }
        }
        
        
        piece.free();
      }
      _pieces[pieceNumber] = null;

      //mark this piece as downloaded
      _downloaded[pieceNumber] = true;

      //send all clients an have message
      sendHave(pieceNumber);
      
    }
    //the piece is corrupt
    else { 
      PEPiece piece = _pieces[pieceNumber];
      if (piece != null) {            
        piece.reset();
        PEPeer[] writers = piece.getWriters();
        if((writers.length > 0) && writers[0] != null) {
          PEPeer writer = writers[0];
          boolean uniqueWriter = true;
          for(int i = 1 ; i < writers.length ; i++) {
            uniqueWriter = uniqueWriter && writer.equals(writers[i]);            
          }
          if(uniqueWriter) {
            badPeerDetected(writer);
          }
        }        
      }
      //Mark this piece as non downloading
      _downloading[pieceNumber] = false;
      
      //Mark this piece as not downloaded (shouldn't change anything)
      _downloaded[pieceNumber] = false;
      
      //if the download has been marked as finish, restart the download
      if (_finished) {
        Debug.out("Piece #" + pieceNumber + " failed final re-check. Re-downloading...");
        _manager.restartDownload(false);
      }
      
      //if we are in end-game mode, we need to re-add all the piece chunks
      //to the list of chunks needing to be downloaded
      if(endGameMode) {
        synchronized(endGameModeChunks) {
          int nbChunks = _pieces[pieceNumber].getNbBlocs();
          for(int i = 0 ; i < nbChunks ; i++) {
            endGameModeChunks.add(new EndGameModeChunk(_pieces[pieceNumber],i));
          }
        }
      }
      
      //We haven't finished (to recover from a wrong finish state)
      _finished = false;
         
      nbHashFails++;
    }
  }

  private void badPeerDetected(PEPeer peer) {
    String ip = peer.getIp();
    //Debug.out("Bad Peer Detected: " + ip + " [" + peer.getClient() + "]");             
    int nbBadChunks = peer.getNbBadChunks();
    if(nbBadChunks > BAD_CHUNKS_LIMIT) {
      //Ban fist to avoid a fast reco of the bad peer
      int nbWarnings = BadIps.getInstance().addWarningForIp(ip);
      if(nbWarnings > WARNINGS_LIMIT) {
        IpFilter.getInstance().ban(ip);                    
      }
      //Close connection in 2nd
      ((PEPeerTransport)peer).closeAll(ip + " : has sent too many bad chunks (" + nbBadChunks + " , " + BAD_CHUNKS_LIMIT + " max)",false,false);
      //Trace the ban in third
      if(nbWarnings > WARNINGS_LIMIT) {
        LGLogger.log(LGLogger.ERROR,ip + " : has been banned and won't be able to connect until you restart azureus");
      }
    }
  }

  public void enqueueReadRequest(DiskManagerDataQueueItem item) {
    _diskManager.enqueueReadRequest(item);
  }

  /**
   * @return
   */
  public List get_connections() {
    return _peer_transports;
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
          DirectByteBufferPool.freeBuffer(item.getBuffer());
          item.setBuffer(null);
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
  
  public void setNbHashFails(int fails) {
    this.nbHashFails = fails;
  }
  
  public PEPeerStatsImpl
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
  
  protected synchronized void
  changeState(
  	int		new_state )
  {
  	peer_manager_state = new_state;
  	
  	for (int i=0;i<peer_manager_listeners.size();i++){
  		
  		((PEPeerManagerListener)peer_manager_listeners.get(i)).stateChanged( peer_manager_state );
  	}
  }
	
  public synchronized void
  addListener(
	  PEPeerManagerListener	l )
  {
  	peer_manager_listeners.add( l );
  }
		
  public synchronized void
  removeListener(
	  PEPeerManagerListener	l )
  {
  	peer_manager_listeners.remove(l);
  }

  /**
   * @param parameterName the name of the parameter that has changed
   * @see org.gudy.azureus2.core3.config.ParameterListener#parameterChanged(java.lang.String)
   */
  public void parameterChanged(String parameterName) {
    maxConnections = COConfigurationManager.getIntParameter("Max Clients");
  }
  
  
  private void checkEndGameMode() {
    //We can't come back from end-game mode
    if(endGameMode)
      return;
    for(int i = 0 ; i < _pieces.length ; i++) {
      //If the piece is downloaded, let's simply continue
      if(_downloaded[i])
        continue;
      //If the piece is being downloaded (fully requested), let's simply continue
      if(_downloading[i])
        continue;
      //Else, the piece is not downloaded / not fully requested, this isn't end game mode
      return;     
    }    
    computeEndGameModeChunks();
    endGameMode = true;
    LGLogger.log(LGLogger.INFORMATION,"Entering end-game mode: " + _manager.getName());
    //System.out.println("End-Game Mode activated");
  }
  
  private void computeEndGameModeChunks() {    
    endGameModeChunks = new ArrayList();
    synchronized(endGameModeChunks) {
	    for(int i = 0 ; i < _pieces.length ; i++) {
	      //Pieces already downloaded are of no interest
	      if(_downloaded[i])
	        continue;
	      PEPiece piece = _pieces[i];
	      if(piece == null)
	        continue;
	      boolean written[] = piece.getWritten();
	      for(int j = 0 ; j < written.length ; j++) {
	        if(!written[j]) {
	          endGameModeChunks.add(new EndGameModeChunk(piece,j));
	        }
	      }
	    }
    }
  }
  
  private void removeFromEndGameModeChunks(int pieceNumber,int offset) {    
    synchronized(endGameModeChunks) {
      Iterator iter = endGameModeChunks.iterator();
      while(iter.hasNext()) {
        EndGameModeChunk chunk = (EndGameModeChunk) iter.next();
        if(chunk.compare(pieceNumber,offset))
          iter.remove();
      }	   
	  }
  }
  
  private boolean findPieceInEndGameMode(PEPeerTransport peer) {
    //Ok, we try one, if it doesn't work, we'll try another next time
    synchronized(endGameModeChunks) {
	    int nbChunks = endGameModeChunks.size();   
	    if(nbChunks > 0) {
		    int random = (int) (Math.random() * nbChunks);
		    EndGameModeChunk chunk = (EndGameModeChunk) endGameModeChunks.get(random);
		    int pieceNumber = chunk.getPieceNumber();
		    if(peer.getAvailable()[pieceNumber]) {		      
		      PEPiece piece = _pieces[pieceNumber];
		      if(piece != null) {
           boolean result = peer.request(pieceNumber,chunk.getOffset(),chunk.getLength());
		       piece.markBlock(chunk.getBlockNumber());
           return result;
		      } else {
		        endGameModeChunks.remove(chunk);
		        //System.out.println("End Game Mode :: Piece is null : chunk remove !!!NOT REQUESTED!!!" + chunk.getPieceNumber() + ":" + chunk.getOffset() + ":" + chunk.getLength());
		        return false;
		      }
		    }
	    }
    }
    return false;
  }
  
  public boolean needsMD5CheckOnCompletion(int pieceNumber) {
    PEPieceImpl piece = _pieces[pieceNumber];    
    if(piece == null)
      return false;
    return piece.getPieceWrites().size() > 0;
  }

  public boolean isSuperSeedMode() {
    return superSeedMode;
  }

  public void setSuperSeedMode(boolean superSeedMode) {
    this.superSeedMode = superSeedMode;
  }    
  
  private void updatePeersInSuperSeedMode() {
    if(!superSeedMode) {
      return;
    }  
    
    //Refresh the update time in case this is needed
    for(int i = 0 ; i < superSeedPieces.length ; i++) {
      superSeedPieces[i].updateTime();
    }
    
    //Use the same number of announces than unchoke
    int nbUnchoke = _manager.getStats().getMaxUploads();
    if(superSeedModeNumberOfAnnounces >= 2 * nbUnchoke)
      return;
    
    
    //Find an available Peer
    PEPeer selectedPeer = null;
    List sortedPeers = null;
    synchronized(_peer_transports) {
      sortedPeers = new ArrayList(_peer_transports.size());
      Iterator iter = _peer_transports.iterator();
      while(iter.hasNext()) {
        sortedPeers.add(new SuperSeedPeer((PEPeer)iter.next()));
      }      
    }
    Collections.sort(sortedPeers);
    Iterator iter = sortedPeers.iterator();
    while(iter.hasNext()) {
      PEPeer peer = ((SuperSeedPeer)iter.next()).peer;
      if((peer.getUniqueAnnounce() == -1) && (peer.getState() == PEPeer.TRANSFERING)) {
        selectedPeer = peer;
        break;
      }
    }      

    if(selectedPeer == null) {
			return;
		}

    if(selectedPeer.getUploadHint() == 0) {
      //Set to infinite
      selectedPeer.setUploadHint(864001 * 1000);
    }
    
		//Find a piece
		boolean found = false;
		SuperSeedPiece piece = null;
		while(!found) {
			piece = superSeedPieces[superSeedModeCurrentPiece];
			if(piece.getLevel() > 0) {
			  piece = null;
			  superSeedModeCurrentPiece++;
			  if(superSeedModeCurrentPiece >= _nbPieces) {
			    superSeedModeCurrentPiece = 0;
			    quitSuperSeedMode();
			    return;
			  }
			} else {
			  found = true;
			}			  
		}
		
		if(piece == null) {
		  return;
		}
		
		//If this peer already has this piece, return (shouldn't happen)
		if(selectedPeer.getAvailable()[piece.getPieceNumber()]) {
		  return;
		}
		
		selectedPeer.setUniqueAnnounce(piece.getPieceNumber());
		superSeedModeNumberOfAnnounces++;
		piece.pieceRevealedToPeer();
		((PEPeerTransport)selectedPeer).sendHave(piece.getPieceNumber());		
  }

  public void updateSuperSeedPiece(PEPeer peer,int pieceNumber) {
    if(superSeedMode) {
      superSeedPieces[pieceNumber].peerHasPiece(null);
      if(peer.getUniqueAnnounce() == pieceNumber) {
        peer.setUniqueAnnounce(-1);
        superSeedModeNumberOfAnnounces--;        
      }
    }
  }
  
  private void quitSuperSeedMode() {
    superSeedMode = false;
    synchronized(_peer_transports) {
      Iterator iter = _peer_transports.iterator();
      while(iter.hasNext()) {
        PEPeerTransport peer = (PEPeerTransport) iter.next();
        peer.closeAll(peer.getIp() + " : Quiting SuperSeed Mode",false,true);
      }
    }
  }
  
  public void
  addListener(
  	PEPeerControlListener	l )
  {
  	synchronized( _peer_transports ){
  	
  		if ( !peer_transport_listeners.contains( l )){
  			
  			peer_transport_listeners.add(l);
  		
  			for (int i=0;i<_peer_transports.size();i++){
  			
  				l.peerAdded( (PEPeerTransport)_peer_transports.get(i));
  			}
  		}
  	}
  }
  
  public void
  removeListener(
  	PEPeerControlListener	l )
  {
  	synchronized( _peer_transports ){
  		
  		peer_transport_listeners.remove(l);
  	}
  }
 }