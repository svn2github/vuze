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
import org.gudy.azureus2.core3.peer.impl.transport.base.DataReaderOwner;
import org.gudy.azureus2.core3.peer.impl.transport.base.DataReaderSpeedLimiter;
import org.gudy.azureus2.core3.peer.util.*;

import com.aelitis.azureus.core.networkmanager.ConnectDisconnectManager;
import com.aelitis.azureus.core.peermanager.LimitedRateGroup;
import com.aelitis.azureus.core.peermanager.utils.PeerConnectInfoStorage;


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
  private static boolean disconnect_seeds_when_seeding = COConfigurationManager.getBooleanParameter("Disconnect Seed", true);
  
    
  private static IpFilter ip_filter = IpFilterManagerFactory.getSingleton().getIPFilter();
  
  private int peer_manager_state = PS_INITIALISED;
  
  private int[] 	availability_cow;
  
  private boolean _bContinue;    
  
  private volatile ArrayList peer_transports_cow = new ArrayList();	// Copy on write!
  
  private AEMonitor	peer_transports_mon	= new AEMonitor( "PEPeerControl:PT");
  
  private DiskManager 			_diskManager;
  private DiskManagerPiece[]	dm_pieces;
  
  private boolean[] _downloading;
  private boolean 	seeding_mode;
  private boolean	restart_initiated;
  
  private boolean[] 			_piecesRarest;
  private PeerIdentityDataID 	_hash;
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
  
  
  private static final long	END_GAME_MODE_SIZE_TRIGGER	= 20*1024*1024;
  private static final long	END_GAME_MODE_TIMEOUT		= 10*60*1000;
  
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
  

  private static final int PEER_UPDATER_INTERVAL = 50;
  
  private long mainloop_loop_count;
  private static final int MAINLOOP_INTERVAL   = 100;
  private static final int MAINLOOP_ONE_SECOND_INTERVAL = 1000 / MAINLOOP_INTERVAL;
  private static final int MAINLOOP_FIVE_SECOND_INTERVAL = MAINLOOP_ONE_SECOND_INTERVAL * 5;
  private static final int MAINLOOP_TEN_SECOND_INTERVAL = MAINLOOP_ONE_SECOND_INTERVAL * 10;
  private static final int MAINLOOP_THIRTY_SECOND_INTERVAL = MAINLOOP_ONE_SECOND_INTERVAL * 30;
  
  

  private List	peer_manager_listeners 		= new ArrayList();
  //private List	peer_transport_listeners 	= new ArrayList();
  
  private List 		piece_check_result_list 	= new ArrayList();
  private AEMonitor	piece_check_result_list_mon	= new AEMonitor( "PEPeerControl:PCRL");

  private boolean superSeedMode;
  private int superSeedModeCurrentPiece;
  private int superSeedModeNumberOfAnnounces;
  private SuperSeedPiece[] superSeedPieces;
  
  private final PeerConnectInfoStorage peer_info_storage = new PeerConnectInfoStorage( 200 );  //size will be updated later on
  
  private final HashMap 	reconnect_counts 		= new HashMap();
  private final AEMonitor	reconnect_counts_mon	= new AEMonitor( "PEPeerControl:RC");

  private AEMonitor	this_mon	= new AEMonitor( "PEPeerControl");
  
  private long		ip_filter_last_update_time;
  
  private Map		user_data;
  
  private PEPeerTransportDataReader		download_speed_limiter;
  
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
      COConfigurationManager.addParameterListener( "Disconnect Seed", this );
      
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
    mainloop_loop_count = 0;

    //The current tracker state
    //this could be start or update

    _averageReceptionSpeed = Average.getInstance(1000, 30);

	download_speed_limiter = 
				DataReaderSpeedLimiter.getSingleton().getDataReader(
						new DataReaderOwner()
					{
							public int
							getMaximumBytesPerSecond()
							{
								return( _downloadManager.getStats().getMaxDownloadKBSpeed() * 1024 );
							}
					});
    
    setDiskManager(_diskManager);
    
    superSeedMode = (COConfigurationManager.getBooleanParameter("Use Super Seeding") && this.getRemaining() == 0);
    
    superSeedModeCurrentPiece = 0;
    
    if ( superSeedMode ){
    	initialiseSuperSeedMode();
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
        
        try{
        	List	peer_transports = peer_transports_cow;
                       	
	          for (int i=0; i < peer_transports.size(); i++) {
	            PEPeerTransport ps = (PEPeerTransport) peer_transports.get(i);
	
	            if (SystemTime.isErrorLast5sec() || oldPolling || (SystemTime.getCurrentTime() > (ps.getLastReadTime() + ps.getReadSleepTime()))) {
	              ps.setReadSleepTime( ps.processRead() );
	              if ( !oldPolling ) ps.setLastReadTime( SystemTime.getCurrentTime() );
	            }
	          }
        }catch( Throwable e ){
        	
        	Debug.printStackTrace( e );
        }
         
        long loop_time = SystemTime.getCurrentTime() - start_time;
        
        //TODO : BOTTLENECK for download speed HERE (100 : max 500kB/s from BitTornado, 50 : 1MB/s, 25 : 2MB/s, 10 : 3MB/s
        
        if( loop_time < PEER_UPDATER_INTERVAL && loop_time >= 0 ) {
          try {  Thread.sleep( PEER_UPDATER_INTERVAL - loop_time );  } catch(Exception e) {}
        }

      }
    }

    public void stopIt() {
      bContinue = false;
    }
  }
  
  
  
  private void mainLoop() {
    _bContinue = true;

    _downloadManager.setState( DownloadManager.STATE_DOWNLOADING );

    _timeStarted = SystemTime.getCurrentTime();

    // initial check on finished state - future checks are driven by piece check results

    checkFinished( true );

    while( _bContinue ) { //loop until stopAll() kills us

      try {
        long timeStart = SystemTime.getCurrentTime();

        updateTrackerAnnounceInterval();
        
        doConnectionChecks();

        processPieceChecks();

        checkCompletedPieces(); //check to see if we've completed anything else

        computeAvailability(); //compute the availablity

        updateStats();

        checkFastPieces();

        if( !seeding_mode ) { //if we're not finished

          _diskManager.computePriorityIndicator();

          checkRequests(); //check the requests

          checkDLPossible(); //look for downloadable pieces
        }

        checkSeeds( false );

        updatePeersInSuperSeedMode();

        doUnchokes();
        

        long loop_time = SystemTime.getCurrentTime() - timeStart;
        if( loop_time < MAINLOOP_INTERVAL && loop_time >= 0 ) {
          try {  Thread.sleep( MAINLOOP_INTERVAL - loop_time );  } catch(Exception e) {}
        }
        
      }
      catch (Throwable e) {

        Debug.printStackTrace( e );
      }

      mainloop_loop_count++;
    }
  }

  
  
  public void 
  stopAll() 
  {
  		// send stopped event
  	
    _tracker.stop();
    
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


	download_speed_limiter	= null;
	
    // 5. Remove listeners
    COConfigurationManager.removeParameterListener("Old.Socket.Polling.Style", this);
    COConfigurationManager.removeParameterListener("Ip Filter Enabled", this);
    COConfigurationManager.removeParameterListener( "Disconnect Seed", this );
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
			// only process new peers if we're still running
		
		if ( _bContinue ){
			
			analyseTrackerResponse( response );
		}
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
				
				addToPeerTransports( transport );
								
			}else{
			  
				transport.closeAll(transport.getIp()+ ":" +transport.getPort()+ ": Already Connected",false,false);
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
	
  
  
  public void addPeer( final String ip_address, final int port ) {
    TRTrackerResponsePeer peer = new TRTrackerResponsePeer() {
      public byte[] getPeerId() {  return new byte[20];  }     
      public String getIPAddress() {  return ip_address;  }         
      public int getPort() {  return port;  }
    };
    
    addPeersFromTracker( new TRTrackerResponsePeer[]{ peer } );
  }
  
  
  
 	private void 
 	addPeersFromTracker(
 		TRTrackerResponsePeer[]		peers )
 	{
      
		for (int i = 0; i < peers.length; i++){
			TRTrackerResponsePeer	peer = peers[i];
      
      ArrayList peer_transports = peer_transports_cow;
      
      boolean already_connected = false;
      
      for( int x=0; x < peer_transports.size(); x++ ) {
        PEPeerTransport transport = (PEPeerTransport)peer_transports.get( x );
        
        	// allow loopback connects for co-located proxy-based connections and testing
        
        if( peer.getIPAddress().equals( transport.getIp() )){
        	
          boolean same_allowed = COConfigurationManager.getBooleanParameter( "Allow Same IP Peers" ) ||
          							transport.getIp().equals( "127.0.0.1" );
          
          if( !same_allowed || peer.getPort() == transport.getPort() ) {
            already_connected = true;
            break;
          }
        }
      }
      
      if( already_connected )  continue;
      
      peer_info_storage.addPeerInfo( new PeerConnectInfoStorage.PeerInfo( peer.getIPAddress(), peer.getPort() ) );
		}
 	}
  
  
  /**
   * Request a new outgoing peer connection.
   * @param address ip of remote peer
   * @param port remote peer listen port
   * @return true if the connection was added to the transport list, false if rejected
   */
  private boolean 
  makeNewOutgoingConnection( 
  	String address, int port ) 
  {    
  		//make sure this connection isn't filtered
  	
    if( ip_filter.isInRange( address, _downloadManager.getDisplayName() ) ) {
      return false;
    }
    
    //make sure we need a new connection
    int needed = PeerUtils.numNewConnectionsAllowed( _hash );
    if( needed == 0 )  return false;

    //start the connection
    PEPeerTransport real = PEPeerTransportFactory.createTransport( this, address, port );
    
    addToPeerTransports( real );
    return true;
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
  
  

  private void checkDLPossible() {
    List bestUploaders = new ArrayList();
    
    List	peer_transports = peer_transports_cow;
    	
	  long[] upRates = new long[peer_transports.size()];
	  Arrays.fill(upRates, -1);
	
	  for (int i = 0; i < peer_transports.size(); i++) {
	    PEPeerTransport pc = (PEPeerTransport) peer_transports.get(i);

	    if (pc.transferAvailable()) {
	    	long upRate = pc.getStats().getReception();
        updateLargestValueFirstSort( upRate, upRates, pc, bestUploaders, 0 );
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
    seeding_mode = true;
   
    //check if we still have (incomplete) pieces to download
    for( int i = 0; i < _nbPieces; i++ ) {
      seeding_mode = seeding_mode && dm_pieces[i].getDone();
    	if( !seeding_mode ) {
    		break;
      }
    }

    if (seeding_mode) {
          	
      if(endGameMode) {
	      try{
	      	endGameModeChunks_mon.enter();
	      
	        endGameMode = false;
	        endGameModeChunks.clear();
	      }finally{
	      	
	      	endGameModeChunks_mon.exit();
	      }
      }
      
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
      
      _diskManager.downloadEnded();
      
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
            
            long wait_time = SystemTime.getCurrentTime() - request.getTimeCreated();
            
            if( wait_time < 0 ) {  //time went backwards
              request.reSetTime();
            }

            if( wait_time > 1000*120 ) {
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

 
  private void 
  updateTrackerAnnounceInterval() 
  {
  	if ( mainloop_loop_count % MAINLOOP_FIVE_SECOND_INTERVAL != 0 ){
  		return;
  	}
  	
    final int WANT_LIMIT = 200;
  	
    //if we're not downloading, use normal re-check rate
    if (_downloadManager.getState() == DownloadManager.STATE_DOWNLOADING ||
        _downloadManager.getState() == DownloadManager.STATE_SEEDING ) {
      
      int num_wanted = PeerUtils.numNewConnectionsAllowed( _hash );
      
      boolean has_remote = _downloadManager.getHealthStatus() == DownloadManager.WEALTH_OK;
      if( has_remote ) {
        //is not firewalled, so can accept incoming connections,
        //which means no need to continually keep asking the tracker for peers
        num_wanted = (int)(num_wanted / 1.5);
      }
      
      if ( num_wanted < 0 || num_wanted > WANT_LIMIT ) {
        num_wanted = WANT_LIMIT;
      }
      
      //include number of pending connection establishments in calculation
      int num_pending = peer_info_storage.getStoredCount();
      num_wanted -= num_pending;
      
      int current_connection_count = PeerIdentityManager.getIdentityCount( _hash );
      
      TRTrackerScraperResponse tsr = _downloadManager.getTrackerScrapeResponse();
      
      if( tsr != null && tsr.isValid() ) {  //we've got valid scrape info
        int num_seeds = tsr.getSeeds();   
        int num_peers = tsr.getPeers();

        int swarm_size;
        
        if( seeding_mode ) {
          //Only use peer count when seeding, as other seeds are unconnectable.
          //Since trackers return peers randomly (some of which will be seeds),
          //backoff by the seed2peer ratio since we're given only that many peers
          //on average each announce.
          float ratio = (float)num_peers / (num_seeds + num_peers);
          swarm_size = (int)(num_peers * ratio);
        }
        else {
          swarm_size = num_peers + num_seeds;
        }
        
        if( swarm_size < num_wanted ) {  //lower limit to swarm size if necessary
          num_wanted = swarm_size;
        }
      }
      
      if( num_wanted < 1 ) {  //we dont need any more connections
        _tracker.setRefreshDelayOverrides( 100 );  //use normal announce interval
        return;
      }
      
      if( current_connection_count == 0 )  current_connection_count = 1;  //fudge it :)
      
      int current_percent = (current_connection_count * 100) / (current_connection_count + num_wanted);
      
      _tracker.setRefreshDelayOverrides( current_percent );  //set dynamic interval override
      return;
    }
  }
  	
 

  /**
   * This methd will compute the overall availability (inluding yourself)
   *
   */
  private void computeAvailability() {
  	
 	if ( mainloop_loop_count % MAINLOOP_ONE_SECOND_INTERVAL != 0 ){
  		
  		return;
  	}
 	
 		//reset the availability
  
 	int[] new_availability = new int[availability_cow.length];
  
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
          if (_pieces[i].getCompleted() > lastCompleted && (slowPeer == _pieces[i].isSlowPiece() || (_pieces[i].isSlowPiece() && (SystemTime.getCurrentTime() - _pieces[i].getLastWriteTime() > 120 * 1000))) ) {
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
    
    int max_avail = (_peers * 2) / 3;
 
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
    
    if(!onlyNonAllocatedPieces && pieceMinAvailability < max_avail && ! pc.isSeed()) pieceMinAvailability = max_avail;
    
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
	    if (!ip_filter.isInRange(ps.getIp(), _downloadManager.getDisplayName())) {
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

  
 
 /**
  * Do all peer choke/unchoke processing.
  */
  private void doUnchokes() {  
  	//Determine how many simultaneous uploads we should consider    
    int max_to_unchoke = _downloadManager.getStats().getMaxUploads();
    List unchoked_peers = getUnchokedPeers();
    
    //if there are less currently-unchoked peers than max allowed
    if( unchoked_peers.size() < max_to_unchoke ) {
      List peer_transports = peer_transports_cow;
      
      for( int i=0; i < peer_transports.size(); i++ ) {
        PEPeerTransport pc = (PEPeerTransport)peer_transports.get( i );

        //unchoke (possibly temporarily) any currently-choked right away
        if( pc.isInterestedInMe() && pc.isChokedByMe() && !pc.isSnubbed() ) {
          pc.sendUnChoke();
          unchoked_peers.add( pc );
          if( unchoked_peers.size() == max_to_unchoke ) {
            break;
          }
        }
      }
    }

    //do main choke/unchoke every 10 secs
    if( mainloop_loop_count % MAINLOOP_TEN_SECOND_INTERVAL == 0 ) {
      
      //refresh optimistic unchoke every 30 seconds
      boolean refresh = mainloop_loop_count % MAINLOOP_THIRTY_SECOND_INTERVAL == 0 || currentOptimisticUnchoke == null;
      
      List best_peers = getBestPeersToUnchoke( max_to_unchoke, refresh );

      //send chokes to those no longer 'best'
      unchoked_peers.removeAll( best_peers );
      for( int i=0; i < unchoked_peers.size(); i++ ) {
        PEPeerTransport pc = (PEPeerTransport)unchoked_peers.get( i );
        pc.sendChoke();
      }
      
      //send unchokes to the 'best' that are not already unchoked
      for( int i=0; i < best_peers.size(); i++ ) {
        PEPeerTransport pc = (PEPeerTransport)best_peers.get( i );
        if( pc.isChokedByMe() )  pc.sendUnChoke();
      }
    }
  }
 
  
  
  /**
   * Get a list of all the peers currently unchoked.
   * @return unchoked peers
   */
  private List getUnchokedPeers() {
    List unchoked = new ArrayList();
    
    List	peer_transports = peer_transports_cow;    
   
    for (int i = 0; i < peer_transports.size(); i++) {
      	
      PEPeerTransport pc = (PEPeerTransport) peer_transports.get(i);
                 
      if( !pc.isChokedByMe() ) {
        	
        unchoked.add(pc);
      }
    }
 
    return( unchoked );
  }
  

  
  /**
   * Get a list of the best possible peers (including the optimistic unchoke)
   * that should be in state unchoked, some of which may already be unchoked.
   * @param max_wanted max number of peers wanted
   * @param refresh_opt_unchoke find a new optimistic unchoke
   * @return list of best peers
   */
  private List getBestPeersToUnchoke( int max_wanted, boolean refresh_opt_unchoke ) {
    max_wanted--;  //reserve the last spot for the opt unchoke
    
    long[] best_rates = new long[ max_wanted ];  //0-initialized
    List best_peers = new ArrayList();
    List peer_transports = peer_transports_cow;
    
    //(1) fill with fastest-transfering peers
    for( int i=0; i < peer_transports.size(); i++ ) {
      PEPeerTransport pc = (PEPeerTransport) peer_transports.get(i);
      
      if( pc == currentOptimisticUnchoke && !refresh_opt_unchoke )  continue;  //skip opt unchoke if not being refreshed
      
      boolean interesting = seeding_mode ? true : pc.isInterestingToMe();
      
      if( interesting && pc.isInterestedInMe() && !pc.isSnubbed() ) {
        long upRate = seeding_mode ? pc.getStats().getUploadAverage() : pc.getStats().getReception();
        if( upRate > 256 ) {  //need to be uploading at least 256kbs to qualify
          updateLargestValueFirstSort( upRate, best_rates, pc, best_peers, 0 );
        }
      }
    }

    //(2) if necessary, fill with peers we are interested in and have reciprocated in the past
    if( !seeding_mode && best_peers.size() < max_wanted ) {
      int sort_start_pos = best_peers.size();
      
      for( int i=0; i < peer_transports.size(); i++ ) {
        PEPeerTransport pc = (PEPeerTransport) peer_transports.get(i);

        if( pc == currentOptimisticUnchoke && !refresh_opt_unchoke )  continue;  //skip opt unchoke if not being refreshed
        
        if( pc.isInterestingToMe() && pc.isInterestedInMe() && !pc.isSnubbed() && !best_peers.contains( pc ) ) { 
          long uploaded_ratio = pc.getStats().getTotalSent() / (pc.getStats().getTotalReceived() + 16383); //assumes 16KB piece chunks
          if( uploaded_ratio < 10 ) {  //make sure we haven't already uploaded 10 times as much data as they've sent us
            updateLargestValueFirstSort( pc.getStats().getTotalReceived(), best_rates, pc, best_peers, sort_start_pos );
          }
        }
      }
    }
    
    //(3) if necessary, fill with peers who have uploaded the most to us
    if( best_peers.size() < max_wanted ) {
      int sort_start_pos = best_peers.size();
      
      for( int i=0; i < peer_transports.size(); i++ ) {
        PEPeerTransport pc = (PEPeerTransport) peer_transports.get(i);
        
        if( pc == currentOptimisticUnchoke && !refresh_opt_unchoke )  continue;  //skip opt unchoke if not being refreshed
        
        boolean allowed = seeding_mode ? !pc.isSnubbed() : true;  //when downloading, allow upload to snubbed peer as last resort
        
        if( pc.isInterestedInMe() && allowed && !best_peers.contains( pc ) ) {
          long total = pc.getStats().getTotalReceived();  //either 0 or >=16384
            
          if( total == 0 ) {  //has never sent us any data
            total = 1000 - pc.getPercentDoneInThousandNotation();  //so prioritize least-completed peers
          }
            
          updateLargestValueFirstSort( total, best_rates, pc, best_peers, sort_start_pos );
        }
      }
    }
    
    //append the optimistic unchoke
    if( refresh_opt_unchoke ) {
      int index = 0;
      
      if( currentOptimisticUnchoke != null ) {
        index = peer_transports.indexOf( currentOptimisticUnchoke ) + 1;
        index = index >= peer_transports.size() ? 0 : index;
      }
      
      currentOptimisticUnchoke = null;
      
      for( int i = index; i < peer_transports.size() + index; i++ ) {
        PEPeerTransport pc = (PEPeerTransport) peer_transports.get( i % peer_transports.size() );
        
        if( !pc.isSeed() && pc.isInterestedInMe() && !pc.isSnubbed() && !best_peers.contains( pc ) ) {
          currentOptimisticUnchoke = pc;
          break;
        }
      }
    }

    if( currentOptimisticUnchoke != null ) {
      best_peers.add( currentOptimisticUnchoke );
    }

    return best_peers;
  }

  
  

  /**
   * Update (if necessary) the given list with the given value while maintaining
   * a largest-value-first (as seen so far) sort order.
   * @param new_value to use
   * @param values existing values array
   * @param new_item to insert
   * @param items existing items
   * @param start_pos index at which to start compare
   */
  private void updateLargestValueFirstSort( long new_value, long[] values, PEPeerTransport new_item, List items, int start_pos ) {  
    for( int i=start_pos; i < values.length; i++ ) {
      if( new_value >= values[ i ] ) {
        for( int j = values.length - 2; j >= i; j-- ) {  //shift displaced values to the right
          values[j + 1] = values[ j ];
        }
        
        values[ i ] = new_value;
        items.add( i, new_item );
        
        if( items.size() > values.length ) {  //throw away last item if list too large 
          items.remove( values.length );
        }
        
        return;
      }
    }
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
  	
  	if ( (!forceDisconnect) && mainloop_loop_count % MAINLOOP_ONE_SECOND_INTERVAL != 0 ){
  		
  		return;
  	}
	
    //If we are not ourself a seed, return
    if (!forceDisconnect && (!seeding_mode || !disconnect_seeds_when_seeding )){
      return;
    }
    
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
	      
	      peerAdded( peer );
      }
      
    }finally{
    	
      peer_transports_mon.exit();
    }
      
  	//for (int i=0;i<peer_transport_listeners.size();i++){
  	//	((PEPeerControlListener)peer_transport_listeners.get(i)).peerAdded( peer );
  	//}
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
  	//boolean	connection_found = false;

  		// copy-on-write semantics
	try{
		peer_transports_mon.enter();
		  	
	  	if ( peer_transports_cow.contains( peer )){
	 
			//connection_found	= true;
			
	  	  ArrayList	new_peer_transports = new ArrayList( peer_transports_cow );
		  	
		  	new_peer_transports.remove(peer);
		  	 
		  	peer_transports_cow = new_peer_transports;
	  		 	
		  	peerRemoved( peer );

		  	//  System.out.println( "closing:" + peer.getClient() + "/" + peer.getIp() );
	 	 
		  	peer.closeAll( peer.getIp() + ": " + reason ,false, false);
	  	}
	}finally{
		
		peer_transports_mon.exit();
	}
	
	//if ( connection_found ){
	//	
	//  	 for (int i=0;i<peer_transport_listeners.size();i++){
	//  	 	
	//  	 	((PEPeerControlListener)peer_transport_listeners.get(i)).peerRemoved( peer );
	// 	 }
  //	}
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
	      	
	      	peerRemoved( peer );
     	}
    }finally{
    	peer_transports_mon.exit();
    }
    
    if ( connection_found ){
    	
	    //for( int i=0; i < peer_transport_listeners.size(); i++ ){
	    //  ((PEPeerControlListener)peer_transport_listeners.get(i)).peerRemoved( peer );
	    //}
	    
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
  
  public void 
  peerAdded(
  	PEPeer pc) 
  {
    _downloadManager.addPeer(pc);
  }

  public void 
  peerRemoved(
  	PEPeer pc) 
  {
    int piece = pc.getUniqueAnnounce();
    if(piece != -1 && superSeedMode ) {
      superSeedModeNumberOfAnnounces--;
      superSeedPieces[piece].peerLeft();
    }
    
    if( pc == currentOptimisticUnchoke )  currentOptimisticUnchoke = null;
    
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
	        
	        if ( buffer != null ){
	        	
		        byte[] hash = computeMd5Hash(buffer);
		        buffer.returnToPool();
		        buffer = null;
		        piece.addWrite(i,peer,hash,correct);  
	        }
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
  		
  		if ( !seeding_mode ){
  			
           checkFinished( false );	 
  		}
  	}
  }

  
  private void 
  badPeerDetected(
  		PEPeer peer) 
  {
    String ip = peer.getIp();
    
    	//Debug.out("Bad Peer Detected: " + ip + " [" + peer.getClient() + "]");
    
    int nbBadChunks = peer.getNbBadChunks();
    
    if (  nbBadChunks > BAD_CHUNKS_LIMIT) {
    	
      IpFilterManager	filter_manager = IpFilterManagerFactory.getSingleton();
    	
      	//Ban fist to avoid a fast reco of the bad peer
      
      int nbWarnings = filter_manager.getBadIps().addWarningForIp(ip);
      
      	// no need to reset the bad chunk count as the peer is going to be disconnected and
      	// if it comes back it'll start afresh
      
      if(	nbWarnings > WARNINGS_LIMIT ){
      	
      	if (COConfigurationManager.getBooleanParameter("Ip Filter Enable Banning")){
      	
	      	ip_filter.ban(ip, _downloadManager.getDisplayName());                    
	      
	      		//	Close connection in 2nd
	      	
	      	((PEPeerTransport)peer).closeAll(ip + " : has sent too many bad chunks (" + nbBadChunks + " , " + BAD_CHUNKS_LIMIT + " max)",false,false);
	      	
	      		//Trace the ban in third
	      		      		
	      	LGLogger.log(LGLogger.ERROR,ip + " : has been banned and won't be able to connect until you restart azureus");
	      	
      	}else{
      		
	      	LGLogger.log(LGLogger.ERROR,ip + " : has not been banned as this is disabled. Bad data count has been reset" );

      		peer.resetNbBadChunks();
      	}
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

 
  public void 
  parameterChanged(
  		String parameterName)
  {
    oldPolling = COConfigurationManager.getBooleanParameter("Old.Socket.Polling.Style");
    
    disconnect_seeds_when_seeding = COConfigurationManager.getBooleanParameter("Disconnect Seed", true);
    
    if ( parameterName.equals("Ip Filter Enabled")){
    	
    	checkForBannedConnections();
    }
  }
  
  protected void
  checkForBannedConnections()
  {
  	//if ipfiltering is enabled, remove any existing filtered connections
  	
    if ( ip_filter.isEnabled()){
    	
      	ArrayList	peer_transports = peer_transports_cow;
      	
        for (int i=0; i < peer_transports.size(); i++) {
          PEPeerTransport conn = (PEPeerTransport)peer_transports.get( i );
          if ( ip_filter.isInRange( conn.getIp(), _downloadManager.getDisplayName() )) {
            conn.closeAll( "IPFilter banned IP address: " + conn.getIp(), false, false );
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
  
  private void 
  computeEndGameModeChunks() 
  {    
    endGameModeChunks = new ArrayList();
    try{
    	endGameModeChunks_mon.enter();
    
	    for(int i = 0 ; i < _pieces.length ; i++) {
	      //Pieces already downloaded are of no interest
	      if(dm_pieces[i].getDone())
	        continue;
	      PEPiece piece = _pieces[i];
	      if(piece == null){
	      	
	        continue;
	      }
	      
	      boolean written[] = piece.getWritten();
	      
	      if ( written == null ){
	      	
	      	if ( !piece.isComplete()){
	      		
			   for(int j = 0 ; j < piece.getNbBlocs() ; j++) {
			   
			      endGameModeChunks.add(new EndGameModeChunk(piece,j));
			  }	      		
	      	}
	      }else{
	      	
		      for(int j = 0 ; j < written.length ; j++) {
		      	
		        if(!written[j]) {
		        	
		          endGameModeChunks.add(new EndGameModeChunk(piece,j));
		        }
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
  
  public void 
  setSuperSeedMode(
  		boolean _superSeedMode) 
  {
  	if (_superSeedMode && superSeedPieces == null ){
  		initialiseSuperSeedMode();
  	}
  	
    superSeedMode = _superSeedMode;
  }    
  
  private void
  initialiseSuperSeedMode()
  {
    superSeedPieces = new SuperSeedPiece[_nbPieces];
    for(int i = 0 ; i < _nbPieces ; i++) {
      superSeedPieces[i] = new SuperSeedPiece(this,i);
    }
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
  
  /*
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
  */
  
  public DiskManager getDiskManager() {  return _diskManager;   }
  
  public PEPeerTransportDataReader
  getDataReader()
  {
  	return( download_speed_limiter );
  }
  
  public LimitedRateGroup getUploadLimitedRateGroup() {  return upload_limited_rate_group;  }
  
  
  /** To retreive arbitrary objects against this object. */
  public Object 
  getData(
    String key) 
  {
    try{
      this_mon.enter();
    
      if (user_data == null) return null;
      
      return user_data.get(key);
      
    }finally{
      
      this_mon.exit();
    }
  }

  /** To store arbitrary objects against a control. */
  
  public void 
  setData(
    String key, 
  Object value) 
  {
    try{
      this_mon.enter();
    
      if (user_data == null) {
        user_data = new HashMap();
      }
      if (value == null) {
        if (user_data.containsKey(key))
          user_data.remove(key);
      } else {
        user_data.put(key, value);
      }
    }finally{
      this_mon.exit();
    }
  }
  
  
  private void doConnectionChecks() 
  {
  		// every 10 seconds check for connected + banned peers
  
    if ( mainloop_loop_count % MAINLOOP_TEN_SECOND_INTERVAL == 0 ) {
    	
    	long	last_update = ip_filter.getLastUpdateTime();
    	
    	if ( last_update != ip_filter_last_update_time ){
    		
    		ip_filter_last_update_time	= last_update;
    		
    		checkForBannedConnections();
    	}
    }
    
    //every 1 second
    if ( mainloop_loop_count % MAINLOOP_ONE_SECOND_INTERVAL == 0 ){
      ArrayList peer_transports = peer_transports_cow;
      
      int num_waiting_establishments = 0;
      
      for( int i=0; i < peer_transports.size(); i++ ) {
        PEPeerTransport transport = (PEPeerTransport)peer_transports.get( i );

        //update waiting count
        int state = transport.getConnectionState();
        if( state == PEPeerTransport.CONNECTION_PENDING || state == PEPeerTransport.CONNECTION_CONNECTING ) {
          num_waiting_establishments++;
        }
      }
      
      //pass from storage to connector
      int allowed = PeerUtils.numNewConnectionsAllowed( _hash );
      if( allowed != 0 ) {
        //try and connect only as many as necessary
        if( allowed != -1 ) {
          int wanted = ConnectDisconnectManager.MAX_SIMULTANIOUS_CONNECT_ATTEMPTS - num_waiting_establishments;
          if( wanted > allowed ) {
            num_waiting_establishments += wanted - allowed;
          }
        }
           
        //load stored peer-infos to be established
        while( num_waiting_establishments < ConnectDisconnectManager.MAX_SIMULTANIOUS_CONNECT_ATTEMPTS ) {
          PeerConnectInfoStorage.PeerInfo peer_info = peer_info_storage.getPeerInfo();
          if( peer_info == null )  break;
          if( makeNewOutgoingConnection( peer_info.getAddress(), peer_info.getPort() ) ) {
            num_waiting_establishments++;
          }
        }
      }
    }
    
    //every 5 seconds
    if ( mainloop_loop_count % MAINLOOP_FIVE_SECOND_INTERVAL == 0 ) {
      ArrayList peer_transports = peer_transports_cow;
      
      for( int i=0; i < peer_transports.size(); i++ ) {
        PEPeerTransport transport = (PEPeerTransport)peer_transports.get( i );
        
        //check for timeouts
        if( transport.doTimeoutChecks() )  continue;
        
        //keep-alive check
        transport.doKeepAliveCheck();
      }

      //update storage capacity
      int allowed = PeerUtils.numNewConnectionsAllowed( _hash );
      if( allowed == -1 )  allowed = 100;
      peer_info_storage.setMaxCapacity( allowed * 2 );
    }
    
    //every 30 seconds
    if ( mainloop_loop_count % MAINLOOP_THIRTY_SECOND_INTERVAL == 0 ) {
      //if we're at our connection limit, time out the least-useful
      //one so we can establish a possibly-better new connection
      if( PeerUtils.numNewConnectionsAllowed( _hash ) == 0 ) {  //we've reached limit
        ArrayList peer_transports = peer_transports_cow;
        
        PEPeerTransport max_transport = null;
        long max_time = 0;
        
        for( int i=0; i < peer_transports.size(); i++ ) {
          PEPeerTransport transport = (PEPeerTransport)peer_transports.get( i );
          
          long time = transport.getTimeSinceLastDataMessageReceived();
          if( time > max_time ) {
            max_time = time;
            max_transport = transport;
          }
        }
        
        if( max_transport != null && max_time > 60*1000 ) {  //ensure a 1min minimum
          max_transport.closeAll( max_transport.getIp()+ ": Timed out by optimistic-connect for lack of activity", false, false );
        }
      }
    }
    
  }
  
 
 }