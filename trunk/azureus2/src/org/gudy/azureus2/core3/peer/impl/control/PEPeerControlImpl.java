/*
 * Created on Oct 15, 2003
 * Created by Olivier Chalouhi
 * Modified Apr 13, 2004 by Alon Rohter
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 * 
 */
 
package org.gudy.azureus2.core3.peer.impl.control;


import java.nio.ByteBuffer;
import java.util.*;


import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.tracker.client.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.ipfilter.*;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.peer.impl.*;
import org.gudy.azureus2.core3.peer.util.*;

import com.aelitis.azureus.core.peermanager.LimitedRateGroup;


public class 
PEPeerControlImpl
	implements 	PEPeerControl, ParameterListener, DiskManagerWriteRequestListener, DiskManagerCheckRequestListener
{
  
  //Min number of requests sent to a peer
  private static final int MIN_REQUESTS = 2;
  //Default number of requests sent to a peer
  //(for each X B/s a new request will be used)
  private static final int SLOPE_REQUESTS = 2 * 1024;
  //Max number of request sent to a peer
  private static final int MAX_REQUESTS = 64;
  
  private static final int BAD_CHUNKS_LIMIT = 3;
  private static final int WARNINGS_LIMIT = 3;
  
  private static boolean oldPolling = COConfigurationManager.getBooleanParameter("Old.Socket.Polling.Style", false);

  private int peer_manager_state = PS_INITIALISED;
  
  private int[] 	availability_cow;
  
  private boolean _bContinue;    
  
  private volatile ArrayList peer_transports_cow = new ArrayList();	// Copy on write!
  
  private AEMonitor	peer_transports_mon	= new AEMonitor( "PEPeerControl:PT");
  
  private DiskManager 			_diskManager;
  private DiskManagerPiece[]	dm_pieces;
  
  private boolean[] _downloading;
  private boolean 	_finished;
  private boolean	restart_initiated;
  
  private boolean[] 			_piecesRarest;
  private PeerIdentityDataID 	_hash;
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
  private long _timeStartedSeeding = -1;
  private long _timeFinished;
  private Average _averageReceptionSpeed;
  private PEPeerTransport currentOptimisticUnchoke;
  
  
  private static final long	END_GAME_MODE_SIZE_TRIGGER	= 2*1024*1024;
  private static final long	END_GAME_MODE_TIMEOUT		= 5*60*1000;
  
  	//A flag to indicate when we're in endgame mode
  private boolean endGameMode;
  private boolean endGameModeAbandoned;
  private long	  timeEndGameModeEntered;
  
    //The list of chunks needing to be downloaded (the mechanism change when entering end-game mode)
  private List 		endGameModeChunks;
  private AEMonitor	endGameModeChunks_mon	= new AEMonitor( "PEPeerControl:EG");

  
  private DownloadManager _downloadManager;
  private PeerUpdater peerUpdater;
  
  private int nbHashFails;
  
  /**
   * The loop time is a potential bottleneck for one-to-one xfers:
   * 500ms = 400kbs, 100ms = 600kbs, 50ms = 625kbs in testing.
   * The bottleneck method(s) should be moved away from a timed loop someday.
   */
  private static final int PEER_UPDATER_WAIT_TIME = 50;
  private static final int MAINLOOP_WAIT_TIME   = 100;
  
  private static final int CHOKE_UNCHOKE_FACTOR = 10000 / MAINLOOP_WAIT_TIME; //every 10s
  private static final int OPT_UNCHOKE_FACTOR   = 30000 / MAINLOOP_WAIT_TIME; //every 30s

  
  
  
  private List	peer_manager_listeners 		= new ArrayList();
  private List	peer_transport_listeners 	= new ArrayList();
  
  private List 		piece_check_result_list 	= new ArrayList();
  private AEMonitor	piece_check_result_list_mon	= new AEMonitor( "PEPeerControl:PCRL");

  private boolean superSeedMode;
  private int superSeedModeCurrentPiece;
  private int superSeedModeNumberOfAnnounces;
  private SuperSeedPiece[] superSeedPieces;
  
  private final HashMap 	reconnect_counts 		= new HashMap();
  private final AEMonitor	reconnect_counts_mon	= new AEMonitor( "PEPeerControl:RC");

  private AEMonitor	this_mon	= new AEMonitor( "PEPeerControl");
  
  private final LimitedRateGroup upload_limited_rate_group = new LimitedRateGroup() {
    public int getRateLimitBytesPerSecond() {
      return _downloadManager.getStats().getUploadRateLimitBytesPerSecond();
    }
  };
  

  public 
  PEPeerControlImpl
  (
    DownloadManager 	manager,
    PEPeerServerHelper 	server,
	TRTrackerClient 	tracker,
    DiskManager 		diskManager) 
  {
  	  _server = server;
  	  this._downloadManager = manager;
  	  _tracker = tracker;
  	  this._diskManager = diskManager;
  	  COConfigurationManager.addParameterListener("Old.Socket.Polling.Style", this);
      COConfigurationManager.addParameterListener("Ip Filter Enabled", this);
 }
  
	public DownloadManager
	getDownloadManager()
	{
		return( _downloadManager );
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
    
    	_hash = PeerIdentityManager.createDataID( _tracker.getTorrent().getHash());
    	
    }catch( TOTorrentException e ){
    	
    		// this should never happen
    	Debug.printStackTrace( e );
    	
    	_hash = PeerIdentityManager.createDataID( new byte[20] ); 
    }
    
    this.nbHashFails = 0;

    //The connection to the tracker
    _tracker.setAnnounceDataProvider(
    		new TrackerClientAnnounceDataProvider()
    		{
    			public String
				getName()
    			{
    				return( getDownloadManager().getDisplayName());
    			}
    			
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
    		});
    
    _myPeerId = _tracker.getPeerId();

    //The peer connections
    peer_transports_cow = new ArrayList();

    //The Server that handle incoming connections
     _server.setServerAdapter(this);

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
    
    peerUpdater = new PeerUpdater();
    peerUpdater.start();
    
    
    
    
    new AEThread( "Peer Manager"){
      public void
      runSupport()
      {
        mainLoop();
      }
    }.start();
  }

  private class 
  PeerUpdater 
  extends AEThread 
  {
    private boolean bContinue = true;

    public PeerUpdater() {
      super("Peer Updater"); //$NON-NLS-1$
      setPriority(Thread.NORM_PRIORITY - 1);
    }

    public void runSupport() {
      while (bContinue) {
        
        long start_time = SystemTime.getCurrentTime();
        
        	// here we unfortunately must process the transports under the monitor
        	// as if the transport has been closed it'll occassionally fail due
        	// to invocations been made on a cleared down transport
        
        try{
        	peer_transports_mon.enter();
                       	
	          for (int i=0; i < peer_transports_cow.size(); i++) {
	            PEPeerTransport ps = (PEPeerTransport) peer_transports_cow.get(i);
	
	            if (SystemTime.isErrorLast5sec() || oldPolling || (SystemTime.getCurrentTime() > (ps.getLastReadTime() + ps.getReadSleepTime()))) {
	              ps.setReadSleepTime( ps.processRead() );
	              if ( !oldPolling ) ps.setLastReadTime( SystemTime.getCurrentTime() );
	              
	              ps.doKeepAliveCheck();
	            }
	          }
        }catch( Throwable e ){
        	
        	Debug.printStackTrace( e );
	      
        }finally{
	          	
	        peer_transports_mon.exit();
            
        }
         
        long loop_time = SystemTime.getCurrentTime() - start_time;
        
        //TODO : BOTTLENECK for download speed HERE (100 : max 500kB/s from BitTornado, 50 : 1MB/s, 25 : 2MB/s, 10 : 3MB/s
        
        if( loop_time < PEER_UPDATER_WAIT_TIME ) {
          try {  Thread.sleep( PEER_UPDATER_WAIT_TIME - loop_time );  } catch(Exception e) {}
        }

      }
    }

    public void stopIt() {
      bContinue = false;
    }
  }
  


  	//main method
  
  public void 
  mainLoop() 
  {
    _bContinue = true;
    
    _downloadManager.setState(DownloadManager.STATE_DOWNLOADING);
    
    _timeStarted = SystemTime.getCurrentTime();
  
    	// initial check on finished state - future checks are driven by piece check results 
    
    checkFinished( true );
    
    while (_bContinue){ //loop until stopAll() kills us
      
      try {
        long timeStart = SystemTime.getCurrentTime();
        
        checkTracker(); //check the tracker status, update peers
        
        processPieceChecks();
        
        checkCompletedPieces();  //check to see if we've completed anything else
        
        computeAvailability(); //compute the availablity
        
        updateStats();
        
        checkFastPieces();
        
        if (!_finished) { //if we're not finished
        	
           _diskManager.computePriorityIndicator();
           
           checkRequests(); //check the requests
           
           checkDLPossible(); //look for downloadable pieces
        }
        
        checkSeeds(false);
        
        updatePeersInSuperSeedMode();
        
        unChoke();

        _loopFactor++; //increment the loopFactor
        
        long timeWait = MAINLOOP_WAIT_TIME - (SystemTime.getCurrentTime() - timeStart);
        
        if (!SystemTime.isErrorLast5sec() && timeWait > 10) {
        	
        	Thread.sleep(timeWait); //sleep
        }
      }catch (Throwable e) {
      	
      	Debug.printStackTrace( e );
      }
    }
  }

  public void stopAll() {
    
  	//Asynchronous cleaner
    Thread t = new AEThread("Cleaner - Tracker Ender") {
      public void runSupport() {
          //1. Send disconnect to Tracker
        _tracker.stop();
      }
    };
    t.setDaemon(true);
    t.start();

    
    //  Stop the server
    _server.stopServer();
    _server.clearServerAdapter();
    
    // Close all clients
    try{
    	peer_transports_mon.enter();
      
        	//  Stop itself
    	
        _bContinue = false;

        while (peer_transports_cow.size() != 0) {
        	
          removeFromPeerTransports((PEPeerTransport)peer_transports_cow.get(0), "Closing all Connections");
        }
    }finally{
      	
      	peer_transports_mon.exit();
    }
  
    
    // Stop the peer updater
    peerUpdater.stopIt();

    //clear pieces
    for (int i = 0; i < _pieces.length; i++) {
      if (_pieces[i] != null)
        pieceRemoved(_pieces[i]);
    }


    // 5. Remove listeners
    COConfigurationManager.removeParameterListener("Old.Socket.Polling.Style", this);
    COConfigurationManager.removeParameterListener("Ip Filter Enabled", this);
  }

  /**
   * A private method that does analysis of the result sent by the tracker.
   * It will mainly open new connections with peers provided
   * and set the timeToWait variable according to the tracker response.
   * @param tracker_response
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
								
			for (int i=0;i<transports.size();i++){
				
				PEPeer	transport = (PEPeer)transports.get(i);
				
				addPeer( transport );
			}
		}
	}
	
	public List
	getPeers()
	{
		return( new ArrayList( peer_transports_cow ));
	}
	
	public void
	addPeer(
		PEPeer		_transport )
	{
		if ( !( _transport instanceof PEPeerTransport )){
			
			throw( new RuntimeException("invalid class"));
		}
		
		PEPeerTransport	transport = (PEPeerTransport)_transport;
		
		try{
			peer_transports_mon.enter();
				
			if ( !peer_transports_cow.contains(transport)){
				
				addToPeerTransports( transport.getRealTransport());
				
			}else{
			  
				transport.closeAll("Already Connected",false,false);
			}
		}finally{
			
			peer_transports_mon.exit();
		}
	}

	public void
	removePeer(
		PEPeer	_transport )
	{
		if ( !( _transport instanceof PEPeerTransport )){
			
			throw( new RuntimeException("invalid class"));
		}
		
		PEPeerTransport	transport = (PEPeerTransport)_transport;
		
		removeFromPeerTransports( transport, "Peer Removed" );			
	}
	
 	private void 
 	addPeersFromTracker(
 		TRTrackerResponsePeer[]		peers )
 	{
      
		for (int i = 0; i < peers.length; i++){
      	
			TRTrackerResponsePeer	peer = peers[i];

			makeNewOutgoingConnection( peer.getIPAddress(), peer.getPort() );

		}
 	}
  
  
  /**
   * Request a new outgoing peer connection.
   * @param address ip of remote peer
   * @param port remote peer listen port
   */
  private void 
  makeNewOutgoingConnection( 
  	String address, int port ) 
  {    
  		//make sure this connection isn't filtered
  	
    if( IpFilterManagerFactory.getSingleton().getIPFilter().isInRange( address, _downloadManager.getDisplayName() ) ) {
      return;
    }
    
    //make sure we need a new connection
    int needed = PeerUtils.numNewConnectionsAllowed( _hash );
    if( needed == 0 )  return;

    //start the connection
    PEPeerTransport real = PEPeerTransportFactory.createTransport( this, address, port );
    
    addToPeerTransports( real );
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
      	
        currentPiece.setBeingChecked();
        
       _diskManager.enqueueCheckRequest(i,this, new Boolean(false));	
      }
    }
  }
  
  /**
   * Check wether a fast piece should stay in fast mode
   * or go back to slow mode.
   *
   */
  private void checkFastPieces() {
    long currentTime = SystemTime.getCurrentTime();
    //for every piece
    for (int i = 0; i < _nbPieces; i++) {
      PEPiece currentPiece = _pieces[i]; //get the piece

      
      //if piece is loaded, fast 
      if (currentPiece != null && !currentPiece.isSlowPiece() && ((currentTime - currentPiece.getLastWriteTime()) > (30 * 1000) ) ) {
        //System.out.println("fast > slow : " + currentTime + " - " + currentPiece.getLastWriteTime());
        currentPiece.setSlowPiece(true);
      }
    }
  }
  
  /**
   * Private method to process the results given by DiskManager's
   * piece checking thread via asyncPieceChecked(..)
   */
  	private void 
	processPieceChecks() 
  	{
  		if ( piece_check_result_list.size() > 0 ){
  			
	  		List	pieces;
	  		
	  			// process complete piece results
	  		
	  		try{
	  			piece_check_result_list_mon.enter();
	    
	  			pieces = new ArrayList( piece_check_result_list );
	  			
	  			piece_check_result_list.clear();
	  			
	  		}finally{
	  			
	  			piece_check_result_list_mon.exit();
	  		}
	  		
	    	Iterator it = pieces.iterator();
	    	
	    	while (it.hasNext()) {
	    		
	    		Object[]	data = (Object[])it.next();
	    		   		
	    		processPieceCheckResult(
	    			((Integer)data[0]).intValue(), 
	    			((Boolean)data[1]).booleanValue(), 
					data[2]);
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
    
    List	peer_transports = peer_transports_cow;
    	
	  long[] upRates = new long[peer_transports.size()];
	  Arrays.fill(upRates, -1);
	
	  for (int i = 0; i < peer_transports.size(); i++) {
	    PEPeerTransport pc = null;
	    try {
	      pc = (PEPeerTransport) peer_transports.get(i);
	    }
	    catch (Exception e) {
	    	Debug.printStackTrace( e );
	      break;
	    }
	    if (pc.transferAvailable()) {
	    	long upRate = pc.getStats().getReception();
	      testAndSortBest(upRate, upRates, pc, bestUploaders, 0);
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
        int maxRequests = MIN_REQUESTS + (int) (pc.getStats().getDownloadAverage() / SLOPE_REQUESTS);
        if(maxRequests > MAX_REQUESTS || maxRequests < 0) maxRequests = MAX_REQUESTS;
        
        
        
        if (endGameMode)
          maxRequests = 2;
        if (pc.isSnubbed())
          maxRequests = 1;  
        
        //Only loop when 3/5 of the queue is empty,
        //in order to make more consecutive requests, and improve
        //cache efficiency
        if(pc.getNbRequests() <= (3 * maxRequests) / 5) {        
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
  }

  /**
   * This method checks if the downloading process is finished.
   *
   */
  
  private void 
  checkFinished(
  	boolean	start_of_day ) 
  {
    boolean temp = true;
   
    for (int i = 0; i < _nbPieces; i++) {
 
    	temp = temp && dm_pieces[i].getDone();

    	if (!temp){
    		
    		break;
      }
    }

    //set finished
    _finished = temp;
        
    if (_finished) {
          	
      if(endGameMode) {
	      try{
	      	endGameModeChunks_mon.enter();
	      
	        endGameMode = false;
	        endGameModeChunks.clear();
	      }finally{
	      	
	      	endGameModeChunks_mon.exit();
	      }
      }
      
      boolean resumeEnabled = COConfigurationManager.getBooleanParameter("Use Resume", true);
      
      _downloadManager.setState(DownloadManager.STATE_FINISHING);
      
      _timeFinished = SystemTime.getCurrentTime();
           
      List	peer_transports = peer_transports_cow;
      
      //remove previous snubbing
      
        for (int i = 0; i < peer_transports.size(); i++){
        	
          PEPeerTransport pc = (PEPeerTransport) peer_transports.get(i);
         
          pc.setSnubbed(false);
        }
      
      //Disconnect seeds
      checkSeeds(true);
      
      boolean checkPieces = COConfigurationManager.getBooleanParameter("Check Pieces on Completion", true);
                  
      	//re-check all pieces to make sure they are not corrupt, but only if we weren't already complete
      
      if (checkPieces && !start_of_day) {
      	
      	_diskManager.enqueueCompleteRecheckRequest( this, new Boolean( true ));
      }
      
      boolean moveWhenDone = COConfigurationManager.getBooleanParameter("Move Completed When Done", false);
      
      if (moveWhenDone) {
      	
        String newName = _diskManager.moveCompletedFiles();
        
        if (newName.length() > 0){
        	
        	_downloadManager.setTorrentFileName(newName);
        }
      }
      
      //update resume data
      if (resumeEnabled){
      	
      	try{
      		_diskManager.dumpResumeDataToDisk(true, false);
      		
      	}catch( Exception e ){
      		
      			// won't go wrong here due to cache write fails as these must have completed
      			// prior to the files being moved. Possible problems with torrent save but
      			// if this fails we can live with it (just means that on restart we'll do
      			// a recheck )
      		
      		Debug.out( "dumpResumeDataToDisk fails" );
      	}
      }
      
      
      _timeStartedSeeding = SystemTime.getCurrentTime();
      _downloadManager.setState(DownloadManager.STATE_SEEDING);
      
      if ( !start_of_day ){
      
      	_downloadManager.downloadEnded();
      }
            		
      _tracker.complete( start_of_day );
    }
  }

  /**
   * This method will locate any expired request, will cancel it, and mark the peer as snubbed
   *
   */
  private void checkRequests() {
    //for every connection
  	List	peer_transports = peer_transports_cow;
  	    
      for (int i = 0; i < peer_transports.size(); i++) {
        PEPeerTransport pc = (PEPeerTransport) peer_transports.get(i);
        if (pc.getState() == PEPeer.TRANSFERING) {
          List expired = pc.getExpiredRequests();

          if (expired != null && expired.size() > 0) {
            pc.setSnubbed(true);

            //Only cancel first request if more than 2 mins have passed
            DiskManagerReadRequest request = (DiskManagerReadRequest) expired.get(0);
            long timeCreated = request.getTimeCreated();
            if (SystemTime.getCurrentTime() - timeCreated > 1000 * 120) {
              int pieceNumber = request.getPieceNumber();
              //get the piece number
              int pieceOffset = request.getOffset();
              //get the piece offset
              PEPiece piece = _pieces[pieceNumber]; //get the piece
              if (piece != null)
                piece.unmarkBlock(pieceOffset / DiskManager.BLOCK_SIZE);
              //unmark the block
              _downloading[pieceNumber] = false;
              //set piece to not being downloaded
              pc.sendCancel(request); //cancel the request object
            }

            //for every expired request                              
            for (int j = 1; j < expired.size(); j++) {
              request = (DiskManagerReadRequest) expired.get(j);
              //get the request object
              pc.sendCancel(request); //cancel the request object
              int pieceNumber = request.getPieceNumber();
              //get the piece number
              int pieceOffset = request.getOffset();
              //get the piece offset
              PEPiece piece = _pieces[pieceNumber]; //get the piece
              if (piece != null)
                piece.unmarkBlock(pieceOffset / DiskManager.BLOCK_SIZE);
              //unmark the block
              _downloading[pieceNumber] = false;
              //set piece to not being downloaded
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
    final int LIMIT = 100;
  	
    //if we're not downloading, use normal re-check rate
    if (_downloadManager.getState() == DownloadManager.STATE_DOWNLOADING) {
      int maxAllowed = PeerUtils.numNewConnectionsAllowed( _hash );
      if ( maxAllowed < 0 || maxAllowed > LIMIT ) {
      	maxAllowed = LIMIT;
      }

      TRTrackerScraperResponse tsr = _downloadManager.getTrackerScrapeResponse();

      //get current scrape values
      int swarmPeers = -1;
      int swarmSeeds = -1;
      if (tsr != null && tsr.isValid()) {
        swarmPeers = tsr.getPeers();
        swarmSeeds = tsr.getSeeds();
      }
      
      //lower limit to swarm size if necessary
      int swarmSize = swarmPeers + swarmSeeds;
      if (swarmSize > 0 && maxAllowed > swarmSize) {
        maxAllowed = swarmSize;
      }
      
      int currConnectionCount = PeerIdentityManager.getIdentityCount( _hash );
      if ( currConnectionCount == 0 ) {
        percentage = 0;  //no current connections, recheck in 1 min
      }
      else if ( maxAllowed > 0 ) {
        float currConnectionPercent = ((float)currConnectionCount) / (currConnectionCount + maxAllowed);
        percentage = (int)(currConnectionPercent * 100);
      }
      
      _tracker.setRefreshDelayOverrides( percentage );
    }
  }
  	
 

  /**
   * This methd will compute the overall availability (inluding yourself)
   *
   */
  private void computeAvailability() {
    //reset the availability
    int[] new_availability = new int[availability_cow.length];
    Arrays.fill(new_availability, 0); //:: should be faster -Tyler

    //for all clients
    
    List	peer_transports = peer_transports_cow;
        
      for (int i = peer_transports.size() - 1; i >= 0; i--) //::Possible optimization to break early when you reach 100%
        {
        //get the peer connection
        PEPeerTransport pc = (PEPeerTransport) peer_transports.get(i);

        //get an array of available pieces    
        boolean[] piecesAvailable = pc.getAvailable();
        if (piecesAvailable != null) //if the list is not null
          {
          for (int j = _nbPieces - 1; j >= 0; j--) //loop for every piece
            {
            if (piecesAvailable[j]) //set the piece to available
            	new_availability[j]++;
          }
        }
      }

    //Then adds our own availability.
    for (int i = dm_pieces.length - 1; i >= 0; i--) {
      if (dm_pieces[i].getDone())
      	new_availability[i]++;
    }

    //copy availability into _availability
  
    availability_cow	= new_availability;
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
   * @return true if a request was assigned, false otherwise
   */
  private boolean findPieceToDownload(PEPeerTransport pc) {
    
    //Slower than 2KB/s is a slow peer
    boolean slowPeer = pc.getStats().getDownloadAverage() < 2 * 1024;
    
    //get the rarest pieces list
    getRarestPieces(pc,90,false);
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

    long currentTime = SystemTime.getCurrentTime();
    
    //For every piece
    for (int i = 0; i < _nbPieces; i++) {
      //If we're not downloading the piece and if it's available from that peer 
      if (_piecesRarest[i] && (_pieces[i] != null) && !_downloading[i]) {
        //We get and mark the next block number to dl
        //We will either get -1 if no more blocks need to be requested
        //Or a number >= 0 otherwise
        int tempBlock = _pieces[i].getAndMarkBlock();

        //SO, if there is a block to request in that piece
        
        if (tempBlock != -1) {
          //Is it a better piece to dl from?
          //A better piece is a piece which is more completed
          //ie more blocks have already been WRITTEN on disk (not requested)
          // and the piece corresponds to our class of peer
          // or nothing has happened to the piece in the last 120 secs
          if (_pieces[i].getCompleted() > lastCompleted && (slowPeer == _pieces[i].isSlowPiece() || (_pieces[i].isSlowPiece() && (currentTime - _pieces[i].getLastWriteTime() > 120 * 1000))) ) {
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

      _pieces[pieceNumber].setSlowPiece(slowPeer);
      
      //We really send the request to the peer
      pc.request(pieceNumber, blockNumber * DiskManager.BLOCK_SIZE, _pieces[pieceNumber].getBlockSize(blockNumber));

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
    getRarestPieces(pc,20,true);
    
    pieceNumber = _diskManager.getPieceNumberToDownload(_piecesRarest);

    if (pieceNumber == -1)
      return false;
    //Now we should have a piece with least presence on network    
    PEPieceImpl piece = null;
    
    
    piece = new PEPieceImpl(this, dm_pieces[pieceNumber], slowPeer, false );
 
    pieceAdded(piece);
    //Assign the created piece to the pieces array.
    _pieces[pieceNumber] = piece;

    //We send request ...
    blockNumber = piece.getAndMarkBlock();
    //if (snubbed)
    //  _pieces[pieceNumber].unmarkBlock(blockNumber);

    pc.request(pieceNumber, blockNumber * DiskManager.BLOCK_SIZE, piece.getBlockSize(blockNumber));
    return true;
  }

  	// set FORCE_PIECE if trying to diagnose piece problems and only want to d/l a specific
  	// piece from a torrent
  
  private static final int FORCE_PIECE	= -1;
  
  private void 
  getRarestPieces(
  	PEPeerTransport 	pc,
	int 				rangePercent,
	boolean 			onlyNonAllocatedPieces) 
  {
    boolean[] piecesAvailable = pc.getAvailable();
    
    Arrays.fill(_piecesRarest, false);

    if ( FORCE_PIECE != -1 && FORCE_PIECE < dm_pieces.length ){
    	
    	if ( !dm_pieces[FORCE_PIECE].getDone() && !_downloading[FORCE_PIECE] && piecesAvailable[FORCE_PIECE]){
    		
    		if ( !onlyNonAllocatedPieces || _pieces[FORCE_PIECE] == null ){
    			
    			_piecesRarest[FORCE_PIECE]	= true;
    		}
    	}
    	
    	return;
    }
 
    //This will represent the minimum piece availability level.
    int pieceMinAvailability = -1;

    int[]	availability = availability_cow;
    
    //1. Scan all pieces to determine min availability
    for (int i = 0; i < _nbPieces; i++) {
      if (!dm_pieces[i].getDone() && !_downloading[i] && piecesAvailable[i]) {
        if (pieceMinAvailability == -1 || availability[i] < pieceMinAvailability) {
          pieceMinAvailability = availability[i];
        }
      }
    }

    
    //If availability is greater than 10, then grab any piece avail (999 should be enough)
    if(pieceMinAvailability > 10 && pieceMinAvailability < 9999) pieceMinAvailability = 9999;
    
    //  We add the range
    pieceMinAvailability = ((100+rangePercent) * pieceMinAvailability) / 100;
    
    //For all pieces
    for (int i = 0; i < _nbPieces; i++) {
      //If we're not downloading it, if it's not downloaded, and if it's available from that peer
      if (!dm_pieces[i].getDone() && !_downloading[i] && piecesAvailable[i]) {
        //null : special case, to ensure we find at least one piece
        //or if the availability level is lower than the old availablility level
        if (availability[i] <= pieceMinAvailability) {
          if(!onlyNonAllocatedPieces || _pieces[i] == null)
            _piecesRarest[i] = true;
        }
      }
    }
  }

  
  
 /**
   * private method to add a new incoming peerConnection
   */
 private void insertPeerSocket(PEPeerTransport ps) {
 	try{
 		this_mon.enter();
 	
	    //Get the max number of connections allowed
	    boolean addFailed = false;
	    String reason = "";
	    if (!IpFilterManagerFactory.getSingleton().getIPFilter().isInRange(ps.getIp(), _downloadManager.getDisplayName())) {
	       try{
	       	peer_transports_mon.enter();
	       
	          if (!peer_transports_cow.contains( ps )) {
	          
	          	addToPeerTransports(ps);
	          	
	          }else{
	          	
	            addFailed = true;
	            
	            reason=ps.getIp() + " : Already Connected";
	          }
	       }finally{
	       	
	       	peer_transports_mon.exit();
	       }
	    }
	    else {
	      addFailed = true;
	      reason=ps.getIp() + " : Blocked IP";
	    }
	    
	    if (addFailed) {
	       ps.closeAll(reason,false, false);
	    }
 	}finally{
 		this_mon.exit();
 	}
 }

  
  private void 
  unChoke() 
  {  
  		// Determine how many uploads we should consider    

    int nbUnchoke = _downloadManager.getStats().getMaxUploads();

    int	choking_count					= 0;
    int	non_choking_count				= 0;
    int	interesting_and_choking_count	= 0;
    
    	// for efficiency count the choking peers first
    
    List	peer_transports = peer_transports_cow;
    
    for (int i = 0; i < peer_transports.size(); i++){
    
        try {
        	PEPeerTransport	pc = (PEPeerTransport)peer_transports.get(i);
        	
            if ( pc.isChoking()){
            	
            	choking_count++;
            	
            	if ( pc.isInteresting()){
            		
            		interesting_and_choking_count++;
            	}
            }else{
            	non_choking_count++;
            }
        }catch (Exception e) {
          	
            continue;
        }
    }

    	// lazily initialise this list
    
    List	nonChoking	= null;
    
    	// if we have a mismatch between the required unchoking count and actual
    
    if ( nbUnchoke != non_choking_count ){
    
    	if ( non_choking_count > nbUnchoke ){
	    	
    			// Then, in any case if we have too many unchoked pple we need to choke some

	    	nonChoking = getNonChokingPeers(); 
		    
		    while( nonChoking.size() > nbUnchoke ){
		    	
		    	PEPeerTransport pc = (PEPeerTransport) nonChoking.remove(0);
		      
		    	pc.sendChoke();
		    }
    	}else if ( non_choking_count < nbUnchoke ){
		    	
    			//   If we lack unchoke pple, find new ones
    		
		    	//Determine the N (nbUnchoke best peers)
		    	//Maybe we'll need some other test when we are a seed ...
		    	
		   		// only interesting, choked peers are considered 
		    	
		   	if ( interesting_and_choking_count > 0 ){
		    	
		   		prepareBestUnChokedPeers( nbUnchoke - non_choking_count );
		   	}
		    
		   		// return here as we've found peers to unchoke or all are unchoked
		   	
		   	return;
		}
    }
    
    	//  Only Choke-Unchoke Every 10 secs
    
    if ((_loopFactor % CHOKE_UNCHOKE_FACTOR) != 0){
    	
      return;
    }

    if ( nonChoking == null ){
    	
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
 
  private List
  getNonChokingPeers()
  {
    List nonChoking = new ArrayList();
    
    List	peer_transports = peer_transports_cow;    
   
    for (int i = 0; i < peer_transports.size(); i++) {
      	
        PEPeerTransport pc = (PEPeerTransport) peer_transports.get(i);
                 
        if (!pc.isChoking()){
        	
          nonChoking.add(pc);
        }
      }
 
    
    return( nonChoking );
  }

  // refactored out of unChoke() - Moti
  private void prepareBestUnChokedPeers(int numUpRates) {
    if (numUpRates <= 0)
      return;

    long[] upRates = new long[numUpRates];
    Arrays.fill(upRates, 0);

    List bestUploaders = new ArrayList();
    
    List	peer_transports = peer_transports_cow;
    
    for (int i = 0; i < peer_transports.size(); i++) {
      PEPeerTransport pc = null;
      try {
        pc = (PEPeerTransport) peer_transports.get(i);
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
    
    List	peer_transports = peer_transports_cow;
    
      for (int i = 0; i < peer_transports.size(); i++) {
        PEPeerTransport pc = (PEPeerTransport) peer_transports.get(i);
  
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

    if (!_finished && bestUploaders.size() < upRates.length) {
      
        for (int i = 0; i < peer_transports.size(); i++) {
          PEPeerTransport pc = null;
          try {
            pc = (PEPeerTransport) peer_transports.get(i);
          }
          catch (Exception e) {
            break;
          }
          if (pc != currentOptimisticUnchoke
            && pc.isInteresting()
            && pc.isInterested()
            && bestUploaders.size() < upRates.length
            && !pc.isSnubbed()
            && (_downloadManager.getStats().getUploaded() / (_downloadManager.getStats().getDownloaded() + 16000)) < 10) {
            bestUploaders.add(pc);
          }
        }
  
    }

    if (bestUploaders.size() < upRates.length) {
      int start = bestUploaders.size();

      
        for (int i = 0; i < peer_transports.size(); i++) {
          PEPeerTransport pc = null;
          try {
            pc = (PEPeerTransport) peer_transports.get(i);
          }
          catch (Exception e) {
            continue;
          }
          if (pc != currentOptimisticUnchoke && pc.isInteresting()) {
            long upRate = 0;
            	//If peer we'll use the overall uploaded value
            if (!_finished){
            	
              upRate = pc.getStats().getTotalReceived();
              
            }else{
              
            		//TODO: seeding to more-complete peers is not the best way to do things
            	
              upRate = pc.getPercentDone();
              
              if (pc.isSnubbed()){
               
              	upRate = -1;
              }
            }
            testAndSortBest(upRate, upRates, pc, bestUploaders, start);
          }
        }
    }
    return bestUploaders;
  }

  // refactored out of unChoke() - Moti
  private void performOptimisticUnChoke() {
    int index = 0;
    List	peer_transports = peer_transports_cow;
     
      for (int i = 0; i <peer_transports.size(); i++) {
        PEPeerTransport pc = (PEPeerTransport)peer_transports.get(i);
        if (pc == currentOptimisticUnchoke)
          index = i + 1;
      }
      if (index >= peer_transports.size())
        index = 0;

      currentOptimisticUnchoke = null;

      for (int i = index; i < peer_transports.size() + index; i++) {
        PEPeerTransport pc = (PEPeerTransport) peer_transports.get(i % peer_transports.size());
        if (!pc.isSeed() && pc.isInteresting() && !pc.isSnubbed()) {
          currentOptimisticUnchoke = pc;
          break;
        }
      }
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
    //fo
  	List	peer_transports = peer_transports_cow;
    
      for (int i = 0; i < peer_transports.size(); i++) {
        //get a peer connection
        PEPeerTransport pc = (PEPeerTransport) peer_transports.get(i);
        //send the have message
        pc.sendHave(pieceNumber);
      }
  
  }

  //Method that checks if we are connected to another seed, and if so, disconnect from him.
  private void checkSeeds(boolean forceDisconnect) {
    //If we are not ourself a seed, return
    if (!forceDisconnect && ((!_finished) || !COConfigurationManager.getBooleanParameter("Disconnect Seed", true))) //$NON-NLS-1$
      return;

    List	peer_transports = peer_transports_cow;
          
      for (int i = 0; i < peer_transports.size(); i++) {
        PEPeerTransport pc = (PEPeerTransport) peer_transports.get(i);
        if (pc != null && pc.getState() == PEPeer.TRANSFERING && pc.isSeed()) {
          pc.closeAll(pc.getIp() + " : Disconnecting seeds when seed",false, false);
        }
      }
 
  }  

  private void updateStats() {   
    //calculate seeds vs peers
  	List	peer_transports = peer_transports_cow;
  	
      _seeds = _peers = _remotes = 0;
      for (int i = 0; i < peer_transports.size(); i++) {
        PEPeerTransport pc = (PEPeerTransport) peer_transports.get(i);
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
  /**
   * The way to unmark a request as being downloaded.
   * Called by Peer connections objects when connection is closed or choked
   * @param request
   */
  public void requestCanceled(DiskManagerReadRequest request) {
    int pieceNumber = request.getPieceNumber(); //get the piece number
    int pieceOffset = request.getOffset(); //get the piece offset    
    PEPiece piece = _pieces[pieceNumber]; //get the piece
    if (piece != null)
      piece.unmarkBlock(pieceOffset / DiskManager.BLOCK_SIZE);
    //set as not being retrieved
    _downloading[pieceNumber] = false; //mark as not downloading
  }

  /**
   * This method is used by BtServer to add an incoming connection
   * to the list of peer connections.
   * @param param the incoming connection socket
   */
  
  public void addPeerTransport(Object param) {
    
    this.insertPeerSocket( _server.createPeerTransport(param));
  }

  public PEPeerControl
  getControl()
  {
  	return( this );
  }

  //get the hash value
  public byte[] getHash() {
    return _hash.getDataID();
  }

  public PeerIdentityDataID
  getPeerIdentityDataID()
  {
  	return( _hash );
  }

  //get the peer id value
  public byte[] getPeerId() {
    return _myPeerId;
  }

  //get the number of pieces
  public int getPiecesNumber() {
    return _nbPieces;
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
    _downloadManager.getStats().received(length);
  }

  public void discarded(int length) {
    if (length > 0) {
      _stats.discarded(length);
    }
    _downloadManager.getStats().discarded(length);
  }
  //::possibly update to setSent() -Tyler
  //set the send value
  public void sent(int length) {
    if (length > 0)
      _stats.sent(length);
    _downloadManager.getStats().sent(length);
  }

  
  public void protocol_sent( int length ) {
    
  }
  
  //setup the diskManager
  
  protected void 
  setDiskManager(
  	DiskManager diskManager ) 
  {
    //the diskManager that handles read/write operations
    _diskManager = diskManager;
    dm_pieces	 = _diskManager.getPieces();
    _nbPieces = _diskManager.getNumberOfPieces();

    //the bitfield indicating if pieces are currently downloading or not
    _downloading = new boolean[_nbPieces];
    Arrays.fill(_downloading, false);

    _piecesRarest = new boolean[_nbPieces];

    	//the pieces - only present here when downloading
    
    _pieces = new PEPieceImpl[dm_pieces.length];
    	
    for (int i = 0; i < dm_pieces.length; i++) {
	    
    	DiskManagerPiece	dm_piece = dm_pieces[i];
    	
    	if ( !dm_piece.getDone() && dm_piece.getCompleteCount() > 0 ){
    		    		
	    	_pieces[i] = new PEPieceImpl( this, dm_piece, true, true );
	        	
	        pieceAdded(_pieces[i]);
	    }
    }

    //the availability level of each piece in the network
    availability_cow = new int[_nbPieces];

    //the stats
    _stats = new PEPeerManagerStatsImpl();

    _server.startServer();	
  }

  public void blockWritten(int pieceNumber, int offset, Object user_data) {
    PEPiece piece = _pieces[pieceNumber];
    if (piece != null) {
      piece.setWritten((PEPeer)user_data,offset / DiskManager.BLOCK_SIZE);
    }    
  }

  public void writeBlock(int pieceNumber, int offset, DirectByteBuffer data,PEPeer sender) {
    PEPiece piece = _pieces[pieceNumber];
    int blockNumber = offset / DiskManager.BLOCK_SIZE;
    if (piece != null && !piece.isWritten(blockNumber)) {
      piece.setBlockWritten(blockNumber);
      _diskManager.enqueueWriteRequest(pieceNumber, offset, data, sender, this );
      if(endGameMode) {
        //In case we are in endGame mode, remove the piece from the chunk list
        removeFromEndGameModeChunks(pieceNumber,offset);
        //For all connections cancel the request
        
        List	peer_transports = peer_transports_cow;
        
        for (int i=0;i<peer_transports.size();i++){

            PEPeerTransport connection = (PEPeerTransport)peer_transports.get(i);
            
            DiskManagerReadRequest dmr = _diskManager.createReadRequest( pieceNumber, offset, piece.getBlockSize(blockNumber));
            
            connection.sendCancel( dmr );
          }
      }
    }else{
    	
    	data.returnToPool();
    }
  }
  
  
  /**
   * This method is only called when a block is received after the initial request expired,
   * but the data has not yet been fulfilled by any other peer, so we use the block data anyway
   * instead of throwing it away, and cancel any outstanding requests for that block that might have
   * been sent after initial expiry.
   */
  public void writeBlockAndCancelOutstanding(int pieceNumber, int offset, DirectByteBuffer data,PEPeer sender) {
    PEPiece piece = _pieces[pieceNumber];
    int blockNumber = offset / DiskManager.BLOCK_SIZE;
    if (piece != null && !piece.isWritten(blockNumber)) {
      piece.setBlockWritten(blockNumber);
      _diskManager.enqueueWriteRequest(pieceNumber, offset, data, sender, this);

      //cancel any matching outstanding requests
      
      List	peer_transports = peer_transports_cow;
      
      for (int i=0;i<peer_transports.size();i++){
       
          PEPeerTransport connection = (PEPeerTransport)peer_transports.get(i);
          
          DiskManagerReadRequest dmr = _diskManager.createReadRequest( pieceNumber, offset, piece.getBlockSize(blockNumber));
          
          connection.sendCancel( dmr );   
        }
    }else{
    	
    	data.returnToPool();
    }
  }
  
  
  public boolean isBlockAlreadyWritten( int piece_number, int offset ) {
    PEPiece piece = _pieces[ piece_number ];
    int block_number = offset / DiskManager.BLOCK_SIZE;
    if( piece != null && piece.isWritten( block_number ) ) {
      return true;
    }
    return false;
  }
  
  
  

  public boolean checkBlock(int pieceNumber, int offset, int length) {
    return _diskManager.checkBlockConsistency(pieceNumber, offset, length);
  }

  public boolean checkBlock(int pieceNumber, int offset, DirectByteBuffer data) {
    return _diskManager.checkBlockConsistency(pieceNumber, offset, data);
  }

  public int getAvailability(int pieceNumber) {
    if (availability_cow == null){
      return 0;
    }
    
    return availability_cow[pieceNumber]; 
  }

  public float getMinAvailability() {
    if (availability_cow == null)
      return 0;

    int[]	availability = availability_cow;
    
    int total = 0;
    int nbPieces;
    int allMin;
    
      nbPieces = availability.length;
      if (nbPieces == 0)
        return 0;

      allMin = availability[0];
      for (int i = 0; i < nbPieces; i++) {
        if (availability[i] < allMin)
          allMin = availability[i];
      }
      for (int i = 0; i < nbPieces; i++) {
        if (availability[i] > allMin)
          total++;
      }
  
    return allMin + (total / (float)nbPieces);
  }

  public int[] 
  getAvailability() 
  {
  
    return( availability_cow );
 
  }

  public void havePiece(int pieceNumber, int pieceLength, PEPeer pcOrigin) {
    _stats.haveNewPiece(pieceLength);

    if(superSeedMode) {
      superSeedPieces[pieceNumber].peerHasPiece(pcOrigin);
      if(pieceNumber == pcOrigin.getUniqueAnnounce()) {
        pcOrigin.setUniqueAnnounce(-1);
        superSeedModeNumberOfAnnounces--;
      }      
    }
    int availability = availability_cow[pieceNumber];
    if (availability < 4) {
      if (dm_pieces[pieceNumber].getDone())
        availability--;
      if (availability <= 0)
        return;
      //for all peers
      
      List	peer_transports = peer_transports_cow;

      
        for (int i = peer_transports.size() - 1; i >= 0; i--) {
          PEPeerTransport pc = (PEPeerTransport) peer_transports.get(i);
          if (pc != pcOrigin && pc.getState() == PEPeer.TRANSFERING) {
            boolean[] peerAvailable = pc.getAvailable();
            if (peerAvailable[pieceNumber])
              ((PEPeerStatsImpl)pc.getStats()).statisticSent(pieceLength / availability);
          }
        }
    }
  }

  public int getPieceLength(int pieceNumber) {
    if (pieceNumber == _nbPieces - 1)
      return _diskManager.getLastPieceLength();
    return _diskManager.getPieceLength();
  }

  /*
  public boolean validateHandshaking(PEPeer pc, byte[] peerId) {
    PEPeerTransport pcTest = PEPeerTransportFactory.createTransport(this, peerId, pc.getIp(), pc.getPort(), true);
    try{
      _peer_transports_mon.enter();
      return !_peer_transports.contains(pcTest);
    }finally{
      _peer_transports_mon.exit();
    }
  }
  */

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
        writtenNotChecked += _pieces[i].getCompleted() * DiskManager.BLOCK_SIZE;
      }
    }
    
    long dataRemaining = _diskManager.getRemaining() - writtenNotChecked;
    if (dataRemaining == 0) {
      long timeElapsed = (_timeFinished - _timeStarted)/1000;
      //if time was spent downloading....return the time as negative
      if(timeElapsed > 1) return timeElapsed * -1;
      return 0;
    }
    
    long averageSpeed = _averageReceptionSpeed.getAverage();
    long lETA = (averageSpeed == 0) ? Constants.INFINITY_AS_INT : dataRemaining / averageSpeed;
    // stop the flickering of ETA from "Finished" to "x seconds" when we are 
    // just about complete, but the data rate is jumpy.
    if (lETA == 0)
      lETA = 1;
    return lETA;
  }
  
  	// the following three methods must be used when adding to/removing from peer transports
  	// they are also synchronised on peer_transports
  
  private void
  addToPeerTransports(
  	PEPeerTransport		peer )
  {
  	// System.out.println( "PEPeerControl::addToPeerTransports:" + peer );
    try{
      peer_transports_mon.enter();
    
      if ( !_bContinue ){
      	
      	throw( new RuntimeException( "PeerTransport added when manager not running" ));
      }
      
      if ( peer_transports_cow.contains( peer )){
      	
      	Debug.out( "Transport added twice" );
      	
      }else{
      	
	      	// copy-on-write semantics
	      
	      ArrayList	new_peer_transports = new ArrayList( peer_transports_cow.size() + 1 );
	      
	      new_peer_transports.addAll( peer_transports_cow );
	      
	      new_peer_transports.add( peer );
	      
	      peer_transports_cow	= new_peer_transports;
      }
      
    }finally{
    	
      peer_transports_mon.exit();
    }
      
  	for (int i=0;i<peer_transport_listeners.size();i++){
  		((PEPeerControlListener)peer_transport_listeners.get(i)).peerAdded( peer );
  	}
  }
  
  	/**
  	 * Monitor *must* be held when calling this
  	 * @param peer
  	 * @param reason
  	 */
  
  private void
  removeFromPeerTransports(
  	PEPeerTransport		peer,
	String				reason )
  {
  	boolean	connection_found = false;

  		// copy-on-write semantics
	try{
		peer_transports_mon.enter();
		  	
		connection_found	= true;
		
	  	if ( peer_transports_cow.contains( peer )){
	 
		  	ArrayList	new_peer_transports = new ArrayList( peer_transports_cow );
		  	
		  	new_peer_transports.remove(peer);
		  	 
		  	peer_transports_cow = new_peer_transports;
	  	
		  		//  System.out.println( "closing:" + peer.getClient() + "/" + peer.getIp() );
	 	 
		 	peer.closeAll( reason ,false, false);
	  	}
	}finally{
		
		peer_transports_mon.exit();
	}
	
	if ( connection_found ){
		
	  	 for (int i=0;i<peer_transport_listeners.size();i++){
	  	 	
	  	 	((PEPeerControlListener)peer_transport_listeners.get(i)).peerRemoved( peer );
	  	 }
  	}
  }
  
  
  public void 
  peerConnectionClosed( 
  	PEPeerTransport peer, 
	boolean reconnect ) 
  {
  	boolean	connection_found = false;
  
    try{
    		// may have already been removed
    	
    	peer_transports_mon.enter();
  	
     	if ( peer_transports_cow.contains( peer )){
     		 
     		connection_found	= true;
     		
	     	ArrayList	new_peer_transports = new ArrayList( peer_transports_cow );
	      	
	      	new_peer_transports.remove(peer);
	      	 
	      	peer_transports_cow = new_peer_transports;
     	}
    }finally{
    	peer_transports_mon.exit();
    }
    
    if ( connection_found ){
    	
	    for( int i=0; i < peer_transport_listeners.size(); i++ ){
	      ((PEPeerControlListener)peer_transport_listeners.get(i)).peerRemoved( peer );
	    }
	    
	    String key = peer.getIp() + ":" + peer.getPort();
	    if( reconnect ) {
	      boolean reconnect_allowed = false;
	      try{
	      	reconnect_counts_mon.enter();  //only allow 3 reconnect attempts
	      
	        Integer reconnect_count = (Integer)reconnect_counts.get( key );
	        int count = 0;
	        if( reconnect_count != null )  count = reconnect_count.intValue();
	        if( count < 3 ) {
	          reconnect_counts.put( key, new Integer( count + 1 ) );
	          reconnect_allowed = true;
	        }
	        else { //don't reconnect this time, but allow at some later time if needed
	          LGLogger.log(LGLogger.INFORMATION, "Reconnect aborted: already reconnected 3 times this session." );
	          reconnect_counts.remove( key );
	        }
	      }finally{
	      	
	      	reconnect_counts_mon.exit();
	      }
	      
	      if( reconnect_allowed )  makeNewOutgoingConnection( peer.getIp(), peer.getPort() );
	    }
	    else { //cleanup any reconnect count
	      try{
	      	reconnect_counts_mon.enter();
	      
	        reconnect_counts.remove( key );
	      }finally{
	      	
	      	reconnect_counts_mon.exit();
	      }
	    }
    }
  }
  
  
  

  	// these should be replaced by above methods + listeners
  
  public void peerAdded(PEPeer pc) {
    _downloadManager.addPeer(pc);
  }

  public void peerRemoved(PEPeer pc) {
    int piece = pc.getUniqueAnnounce();
    if(piece != -1) {
      superSeedModeNumberOfAnnounces--;
      superSeedPieces[piece].peerLeft();
    }
    _downloadManager.removePeer(pc);
  }

  public void pieceAdded(PEPiece p) {
    _downloadManager.addPiece(p);
  }

  public void pieceRemoved(PEPiece p) {
    _downloadManager.removePiece(p);
  }

  public String getElapsedTime() {
    return TimeFormatter.format((SystemTime.getCurrentTime() - _timeStarted) / 1000);
  }
  
  // Returns time started in ms
  public long getTimeStarted() {
    return _timeStarted;
  }

  public long getTimeStartedSeeding() {
    return _timeStartedSeeding;
  }
  
	private byte[] 
	computeMd5Hash(
		DirectByteBuffer buffer) 
	{ 			
		Md5Hasher md5 	= new Md5Hasher();

	    md5.reset();
	    
	    int position = buffer.position(DirectByteBuffer.SS_DW);
	    
	    md5.update(buffer.getBuffer(DirectByteBuffer.SS_DW));
	    
	    buffer.position(DirectByteBuffer.SS_DW, position);
	    
	    ByteBuffer md5Result	= ByteBuffer.allocate(16);
	    
	    md5Result.position(0);
	    
	    md5.finalDigest( md5Result );
	    
	    byte[] result = new byte[16];
	    
	    md5Result.position(0);
	    
	    for(int i = 0 ; i < result.length ; i++) {
	    	
	      result[i] = md5Result.get();
	    }   
	    
	    return result;    
	  }
	  
	  private void 
	  MD5CheckPiece(
	  	PEPiece piece,
		boolean correct) 
	  {
	    PEPeer[] writers = piece.getWriters();
	    int offset = 0;
	    for(int i = 0 ; i < writers.length ; i++) {
	      int length = piece.getBlockSize(i);
	      PEPeer peer = writers[i];
	      if(peer != null) {
	        DirectByteBuffer buffer = _diskManager.readBlock(piece.getPieceNumber(),offset,length);
	        byte[] hash = computeMd5Hash(buffer);
	        buffer.returnToPool();
	        buffer = null;
	        piece.addWrite(i,peer,hash,correct);        
	      }
	      offset += length;
	    }        
	  }
					  

  	public void
	pieceChecked( 
		int 		pieceNumber, 
		boolean 	result,
		Object		user_data )
  	{
        try{
	       	piece_check_result_list_mon.enter();
	        
	       	piece_check_result_list.add(new Object[]{new Integer(pieceNumber), new Boolean(result), user_data });
	    }finally{
	        	
	    	piece_check_result_list_mon.exit();
	    }
  	}
  	
  private void 
  processPieceCheckResult(
  	int 		pieceNumber, 
	boolean 	result,
	Object		user_data ) 
  {
  	boolean	recheck_on_completion = ((Boolean)user_data).booleanValue();
  	
  	try{  		
  		
  			// piece can be null when running a recheck on completion
  		
	    PEPieceImpl piece = _pieces[pieceNumber];
	
	    // System.out.println( "processPieceCheckResult(" + _finished + "/" + recheck_on_completion + "):" + pieceNumber + "/" + piece + " - " + result );
	    
	    if( result && piece != null ){
	    	
	    	pieceRemoved(piece);
	    }
	    
	    if( recheck_on_completion ) {  //this is a recheck, so don't send HAVE msgs
	    	
	      if ( result){
	        
	        // ok
	      	
	      }else{  	//piece failed
	      			//restart the download afresh
	      	
	        Debug.out("Piece #" + pieceNumber + " failed final re-check. Re-downloading...");
	        
	        if ( !restart_initiated ){
	        	
	        	restart_initiated	= true;
	        	
	        	_downloadManager.restartDownload( false );
	        }
	      }
	      
	      return;
	    }
	
	    
	    	//  the piece has been written correctly
	    
	    if ( result ){
	      
			
	      if(piece != null) {

	      	if( needsMD5CheckOnCompletion(pieceNumber)){
		    	
	      		MD5CheckPiece(piece,true);
	      	}
 
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
	      }
	           
	      _pieces[pieceNumber] = null;
	
	      	//send all clients a have message
	      
	      sendHave(pieceNumber);
	      
	    }else{
	    	
	    		//    the piece is corrupt

	       if (piece != null) {
	       	
		    MD5CheckPiece(piece,false);

	        PEPeer[] writers = piece.getWriters();
	        if((writers.length > 0) && writers[0] != null) {
	          PEPeer writer = writers[0];
	          boolean uniqueWriter = true;
	          for(int i = 1 ; i < writers.length ; i++) {
	            uniqueWriter = uniqueWriter && writer.equals(writers[i]);            
	          }
	          if(uniqueWriter) {
	          	
	          		// we don't know how many chunks are invalid, assume all were invalid
	          	
	          	for (int i=0;i<writers.length;i++){
	          		
	          		writer.hasSentABadChunk();
	          	}
	          	
	            badPeerDetected(writer);
	          }
	        }
	        
	        piece.reset();
	      }
	       
	       		//Mark this piece as non downloading
	       
	      _downloading[pieceNumber] = false;
	            
	      	//if we are in end-game mode, we need to re-add all the piece chunks
	      	//to the list of chunks needing to be downloaded
	      
	      if ( endGameMode && piece != null ){
	      	
	        try{
	        	endGameModeChunks_mon.enter();
	        
	        	int nbChunks = piece.getNbBlocs();
	        	
	        	for( int i = 0 ; i < nbChunks ; i++) {
	        		
	        		endGameModeChunks.add(new EndGameModeChunk(_pieces[pieceNumber],i));
	        	}
	        }finally{
	        	
	        	endGameModeChunks_mon.exit();
	        }
	      }
	      	         
	      nbHashFails++;
	    }
  	}finally{
  		
  		if ( !_finished ){
  			
           checkFinished( false );	 
  		}
  	}
  }

  private void badPeerDetected(PEPeer peer) {
    String ip = peer.getIp();
    //Debug.out("Bad Peer Detected: " + ip + " [" + peer.getClient() + "]");             
    int nbBadChunks = peer.getNbBadChunks();
    if(nbBadChunks > BAD_CHUNKS_LIMIT) {
    	IpFilterManager	filter_manager = IpFilterManagerFactory.getSingleton();
    	
      //Ban fist to avoid a fast reco of the bad peer
      int nbWarnings = filter_manager.getBadIps().addWarningForIp(ip);
      
      	// no need to reset the bad chunk count as the peer is going to be disconnected and
      	// if it comes back it'll start afresh
      
      if(nbWarnings > WARNINGS_LIMIT) {
      	IpFilterManagerFactory.getSingleton().getIPFilter().ban(ip, _downloadManager.getDisplayName());                    
      }
      //Close connection in 2nd
      ((PEPeerTransport)peer).closeAll(ip + " : has sent too many bad chunks (" + nbBadChunks + " , " + BAD_CHUNKS_LIMIT + " max)",false,false);
      //Trace the ban in third
      if(nbWarnings > WARNINGS_LIMIT) {
        LGLogger.log(LGLogger.ERROR,ip + " : has been banned and won't be able to connect until you restart azureus");
      }
    }
  }


  public PEPiece[] getPieces() {
    return _pieces;
  }


  public boolean isOptimisticUnchoke(PEPeer pc) {
    return pc == currentOptimisticUnchoke;
  }


  
  public int getNbHashFails() {
    return nbHashFails;
  }
  
  public void setNbHashFails(int fails) {
    this.nbHashFails = fails;
  }
  
  public PEPeerStats
  createPeerStats()
  {
  	return( new PEPeerStatsImpl() );
  }

  
  public DiskManagerReadRequest
  createDiskManagerRequest(
  	int pieceNumber,
  	int offset,
  	int length )
  {
  	return( _diskManager.createReadRequest( pieceNumber, offset, length ));
  }
  
  
  
  protected void
  changeState(
  	int		new_state )
  {
  	try{
  		this_mon.enter();
  	
  		peer_manager_state = new_state;
  	
  		for (int i=0;i<peer_manager_listeners.size();i++){
  		
  			((PEPeerManagerListener)peer_manager_listeners.get(i)).stateChanged( peer_manager_state );
  		}
  	}finally{
  		
  		this_mon.exit();
  	}
  }
	
  public void
  addListener(
	  PEPeerManagerListener	l )
  {
  	try{
  		this_mon.enter();
  	
  		peer_manager_listeners.add( l );
  	}finally{
  		
  		this_mon.exit();
  	}
  }
		
  public void
  removeListener(
	  PEPeerManagerListener	l )
  {
  	try{
  		this_mon.enter();
  	
  		peer_manager_listeners.remove(l);
  	}finally{
  		
  		this_mon.exit();
  	}
  }

  /**
   * @param parameterName the name of the parameter that has changed
   * @see org.gudy.azureus2.core3.config.ParameterListener#parameterChanged(java.lang.String)
   */
  public void parameterChanged(String parameterName) {
    oldPolling = COConfigurationManager.getBooleanParameter("Old.Socket.Polling.Style");
    
    //if ipfiltering becomes enabled, remove any existing filtered connections
    if (parameterName.equals("Ip Filter Enabled") && IpFilterManagerFactory.getSingleton().getIPFilter().isEnabled()) {

      	
      	ArrayList	peer_transports = peer_transports_cow;
      	
        for (int i=0; i < peer_transports.size(); i++) {
          PEPeerTransport conn = (PEPeerTransport)peer_transports.get( i );
          if ( IpFilterManagerFactory.getSingleton().getIPFilter().isInRange( conn.getIp(), _downloadManager.getDisplayName() )) {
            conn.closeAll( "IPFilter banned IP address", false, false );
          }
        }
    }
  }
  
  
  private void checkEndGameMode() {
    //We can't come back from end-game mode
    if ( endGameMode || endGameModeAbandoned ){
    	
    	if ( !endGameModeAbandoned ){
    		 	
    		if ( SystemTime.getCurrentTime() - timeEndGameModeEntered > END_GAME_MODE_TIMEOUT ){

    			endGameMode	= false;
    			
    			endGameModeAbandoned	= true;
  
    		    LGLogger.log(LGLogger.INFORMATION,"Abandoning end-game mode: " + _downloadManager.getDisplayName());

    		    try{
    		    	endGameModeChunks_mon.enter();

    		    	endGameModeChunks.clear();
    		    	
    		    }finally{
    		    	
    		    	endGameModeChunks_mon.exit();
    		    }
    		}
    	}
    	
    	return;
    }
    
    int	active_pieces = 0;
    
    for(int i = 0 ; i < _pieces.length ; i++) {
      //If the piece is downloaded, let's simply continue
      if(dm_pieces[i].getDone())
        continue;
      //If the piece is being downloaded (fully requested), let's simply continue
      if(_downloading[i]){
      	active_pieces++;
      
        continue;
      }
      
      //Else, the piece is not downloaded / not fully requested, this isn't end game mode
      return;     
    }    
    
    	// only flick into end-game mode if < trigger size left
    
    if ( active_pieces * _diskManager.getPieceLength() <= END_GAME_MODE_SIZE_TRIGGER ){
    	
    	timeEndGameModeEntered	= SystemTime.getCurrentTime();
		
	    computeEndGameModeChunks();
	    endGameMode = true;
	    LGLogger.log(LGLogger.INFORMATION,"Entering end-game mode: " + _downloadManager.getDisplayName());
	    //System.out.println("End-Game Mode activated");
    }
  }
  
  private void computeEndGameModeChunks() {    
    endGameModeChunks = new ArrayList();
    try{
    	endGameModeChunks_mon.enter();
    
	    for(int i = 0 ; i < _pieces.length ; i++) {
	      //Pieces already downloaded are of no interest
	      if(dm_pieces[i].getDone())
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
    }finally{
    	endGameModeChunks_mon.exit();
    }
  }
  
  private void removeFromEndGameModeChunks(int pieceNumber,int offset) {    
    try{
    	endGameModeChunks_mon.enter();
    
      Iterator iter = endGameModeChunks.iterator();
      while(iter.hasNext()) {
        EndGameModeChunk chunk = (EndGameModeChunk) iter.next();
        if(chunk.compare(pieceNumber,offset))
          iter.remove();
      }	   
	}finally{
		endGameModeChunks_mon.exit();
	}
  }
  
  private boolean findPieceInEndGameMode(PEPeerTransport peer) {
    //Ok, we try one, if it doesn't work, we'll try another next time
    try{
    	endGameModeChunks_mon.enter();
    
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
		      }
		      
          endGameModeChunks.remove(chunk);
		      //System.out.println("End Game Mode :: Piece is null : chunk remove !!!NOT REQUESTED!!!" + chunk.getPieceNumber() + ":" + chunk.getOffset() + ":" + chunk.getLength());
		      return false;
		    }
	    }
    }finally{
    	endGameModeChunks_mon.exit();
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

  public boolean isInEndGameMode() {
    return endGameMode;
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
    int nbUnchoke = _downloadManager.getStats().getMaxUploads();
    if(superSeedModeNumberOfAnnounces >= 2 * nbUnchoke)
      return;
    
    
    //Find an available Peer
    PEPeer selectedPeer = null;
    List sortedPeers = null;
 
    
    ArrayList	peer_transports = peer_transports_cow;
    
      sortedPeers = new ArrayList(peer_transports.size());
      Iterator iter = peer_transports.iterator();
      while(iter.hasNext()) {
        sortedPeers.add(new SuperSeedPeer((PEPeer)iter.next()));
      }      
 
    Collections.sort(sortedPeers);
    iter = sortedPeers.iterator();
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
      selectedPeer.setUploadHint(Constants.INFINITY_AS_INT);
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
  
  private void 
  quitSuperSeedMode() 
  {
    superSeedMode = false;
     
    		// closing a transport can result in it being removed from teh list. Therefore
    		// copy the list first else we get a "concurrent modification exception"	
    	
    ArrayList	peer_transports = peer_transports_cow;
    	
    Iterator iter = peer_transports.iterator();
     
    while(iter.hasNext()) {
      	
        PEPeerTransport peer = (PEPeerTransport) iter.next();
        
        peer.closeAll(peer.getIp() + " : Quiting SuperSeed Mode",false,true);
    }
  }
  
  public void
  addListener(
  	PEPeerControlListener	l )
  {
  	try{
  		peer_transports_mon.enter();
  	
  		if ( !peer_transport_listeners.contains( l )){
  			
  			peer_transport_listeners.add(l);
  		
  			for (int i=0;i<peer_transports_cow.size();i++){
  			
  				l.peerAdded( (PEPeerTransport)peer_transports_cow.get(i));
  			}
  		}
  	}finally{
  		peer_transports_mon.exit();
  	}
  }
  
  public void
  removeListener(
  	PEPeerControlListener	l )
  {
  	try{
  		peer_transports_mon.enter();
  		
  		peer_transport_listeners.remove(l);
  	}finally{
  		peer_transports_mon.exit();
  	}
  }
  
  
  public DiskManager getDiskManager() {  return _diskManager;   }
  
  
  public LimitedRateGroup getUploadLimitedRateGroup() {  return upload_limited_rate_group;  }
  
 }