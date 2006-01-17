/*
 * Created by Olivier Chalouhi
 * Modified Apr 13, 2004 by Alon Rohter
 * Heavily modified Sep 2005 by Joseph Bridgewater
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 * 
 * manages all peer transports for a torrent
 */

package org.gudy.azureus2.core3.peer.impl.control;


import java.nio.ByteBuffer;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.ipfilter.*;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.peer.impl.*;
import org.gudy.azureus2.core3.peer.impl.transport.PEPeerTransportProtocol;
import org.gudy.azureus2.core3.peer.util.*;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.tracker.client.*;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.networkmanager.LimitedRateGroup;
import com.aelitis.azureus.core.networkmanager.impl.ConnectDisconnectManager;
import com.aelitis.azureus.core.peermanager.PeerManager;
import com.aelitis.azureus.core.peermanager.control.*;
import com.aelitis.azureus.core.peermanager.peerdb.*;
import com.aelitis.azureus.core.peermanager.piecepicker.PiecePicker;
import com.aelitis.azureus.core.peermanager.unchoker.*;

/**
 * 
 * @author MjrTom
 *			2005/Oct/08: Numerous changes for new piece-picking. Also
 *						a few optimizations and multi-thread cleanups
 *			2006/Jan/02: refactoring piece picking related code
 */


public class 
PEPeerControlImpl
	extends LogRelation
	implements 	PEPeerControl, ParameterListener, DiskManagerWriteRequestListener, PeerControlInstance,
		DiskManagerCheckRequestListener, IPFilterListener
{
	private static final LogIDs LOGID = LogIDs.PEER;
  
	private static final long 	TIME_MIN_AVAILABILITY	=995;	// min ms for recalculating availability

	//Min number of requests sent to a peer
	private static final int	MIN_REQUESTS = 2;
	//Max number of request sent to a peer
	private static final int	MAX_REQUESTS = 256;
	//Default number of requests sent to a peer, (for each X B/s a new request will be used)
	private static final int	SLOPE_REQUESTS = 4 * 1024;
	// TBD: for peers above this speed, when a new piece is started, it gets reserved to them
	// to help optimize their disk-cache ... maybe
//	private static final int	RESERVE_SPEED =SLOPE_REQUESTS *4;
	
	private static final int	WARNINGS_LIMIT = 2;
	
	private static final int	CHECK_REASON_DOWNLOADED		= 1;
	private static final int	CHECK_REASON_COMPLETE		= 2;
	private static final int	CHECK_REASON_SCAN			= 3;

  	private static boolean disconnect_seeds_when_seeding = COConfigurationManager.getBooleanParameter("Disconnect Seed", true);

	private static IpFilter ip_filter = IpFilterManagerFactory.getSingleton().getIPFilter();

	private int				peer_manager_state = PS_INITIALISED;

	private int[] 	availability_cow;

	private volatile boolean	is_running = false;  

	private volatile ArrayList	peer_transports_cow = new ArrayList();	// Copy on write!
	private AEMonitor			peer_transports_mon	= new AEMonitor( "PEPeerControl:PT");
	
	protected PEPeerManagerAdapter adapter;
	private DiskManager			disk_mgr;
	private DiskManagerPiece[]	dm_pieces;
	
	private PiecePicker			piecePicker;
	
	private boolean 		seeding_mode;
	private boolean			restart_initiated;
	
	private int				_nbPieces =-1;			//how many pieces in the torrent
	volatile private PEPieceImpl[]	_pieces;		//pieces that are currently in progress
	private int				nbActivePieces;			//how many in _pieces[] based on Added and Removed
	private float			_globalAvail;
	private float			_globalAvgAvail;
	private long			_time_last_avail;

	private PeerIdentityDataID	_hash;
	private byte[]				_myPeerId;
	protected PEPeerManagerStats	_stats;
	//private final TRTrackerAnnouncer _tracker;
	//  private int _maxUploads;
	private int		_seeds, _peers,_remotes;
	private long last_remote_time;
	private long	_timeStarted;
	private long	_timeStartedSeeding = -1;
	private long	_timeFinished;
	private Average	_averageReceptionSpeed;
	
	private static final long	END_GAME_MODE_SIZE_TRIGGER	= 20*1024*1024;
	private static final long	END_GAME_MODE_TIMEOUT		= 10*60*1000;
	
	//A flag to indicate when we're in endgame mode
	private boolean	endGameMode;
	private boolean	endGameModeAbandoned;
	private long	timeEndGameModeEntered;
	
	private long mainloop_loop_count;
 
	private static final int MAINLOOP_ONE_SECOND_INTERVAL = 1000 / PeerControlScheduler.SCHEDULE_PERIOD_MILLIS;
	private static final int MAINLOOP_FIVE_SECOND_INTERVAL = MAINLOOP_ONE_SECOND_INTERVAL * 5;
	private static final int MAINLOOP_TEN_SECOND_INTERVAL = MAINLOOP_ONE_SECOND_INTERVAL * 10;
	private static final int MAINLOOP_THIRTY_SECOND_INTERVAL = MAINLOOP_ONE_SECOND_INTERVAL * 30;
	private static final int MAINLOOP_SIXTY_SECOND_INTERVAL = MAINLOOP_ONE_SECOND_INTERVAL * 60;
	private static final int MAINLOOP_TEN_MINUTE_INTERVAL = MAINLOOP_SIXTY_SECOND_INTERVAL * 10;
	
	
	private volatile ArrayList peer_manager_listeners_cow = new ArrayList();  //copy on write
	
	
	private List	piece_check_result_list 	= new ArrayList();
	private AEMonitor	piece_check_result_list_mon	= new AEMonitor( "PEPeerControl:PCRL");
	
	private boolean 			superSeedMode;
	private int 				superSeedModeCurrentPiece;
	private int 				superSeedModeNumberOfAnnounces;
	private SuperSeedPiece[]	superSeedPieces;
	
	private AEMonitor	this_mon	= new AEMonitor( "PEPeerControl");
	
	private long		ip_filter_last_update_time;
	
	private Map	user_data;

	private Unchoker		unchoker;
	
	private PeerDatabase	peer_database;
	
	private int				next_rescan_piece		= -1;
	private long			rescan_piece_time		= -1;
	
	private long			last_eta;
	private long			last_eta_calculation;
	
	private final LimitedRateGroup upload_limited_rate_group = new LimitedRateGroup() {
		public int getRateLimitBytesPerSecond() {
      		return adapter.getUploadRateLimitBytesPerSecond();
		}
	};
	
	private final LimitedRateGroup download_limited_rate_group = new LimitedRateGroup() {
		public int getRateLimitBytesPerSecond() {
      		return adapter.getDownloadRateLimitBytesPerSecond();
		}
	};
	
	public 
	PEPeerControlImpl(
		byte[]					_peer_id,
		PEPeerManagerAdapter 	_adapter,
		DiskManager 			diskManager) 
	{
		_myPeerId		= _peer_id;
		adapter 		= _adapter;
  
		disk_mgr = diskManager;
  	  
		COConfigurationManager.addParameterListener("Ip Filter Enabled", this);
		COConfigurationManager.addParameterListener( "Disconnect Seed", this );

		ip_filter.addListener( this );
      
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
			
    		_hash = PeerIdentityManager.createDataID( disk_mgr.getTorrent().getHash());
			
		}catch( TOTorrentException e ){
			
			// this should never happen
			Debug.printStackTrace( e );
			
			_hash = PeerIdentityManager.createDataID( new byte[20] ); 
		}
		
		//The peer connections
		peer_transports_cow = new ArrayList();
		
		//BtManager is threaded, this variable represents the
		// current loop iteration. It's used by some components only called
		// at some specific times.
		mainloop_loop_count = 0;
		
		//The current tracker state
		//this could be start or update
		
		_averageReceptionSpeed = Average.getInstance(1000, 30);
		
		setDiskManager(disk_mgr);
		
		superSeedMode = (COConfigurationManager.getBooleanParameter("Use Super Seeding") && this.getRemaining() == 0);
		
		superSeedModeCurrentPiece = 0;
		
		if ( superSeedMode ){
			initialiseSuperSeedMode();
		}
		
		
		peer_database = PeerDatabaseFactory.createPeerDatabase();
		
		//register as legacy controller
		PeerManager.getSingleton().registerLegacyManager( this );
		
		
    
		// initial check on finished state - future checks are driven by piece check results

		// Moved out of mainLoop() so that it runs immediately, possibly changing
		// the state to seeding.

		checkFinished( true );

		PeerControlSchedulerFactory.getSingleton().register( this );
    
		_timeStarted = SystemTime.getCurrentTime();

		is_running = true;
	}

  
 
  
  
  public void
  schedule()
  {
      try {
				updateTrackerAnnounceInterval();
				doConnectionChecks();
				processPieceChecks();
				checkCompletedPieces();		//check to see if we've completed anything else
				computeAvailability();		//compute the availablity
				updateStats();
				checkSpeedAndReserved();

				// see if need to recheck Interested on all peers
				if (piecePicker.hasDownloadableChanged())
				{
					List peer_transports =peer_transports_cow;

					for (int i =0; i <peer_transports.size(); i++ )
					{
						PEPeerTransport pc =(PEPeerTransport)peer_transports.get(i);
						pc.checkInterested();
					}
				}

				boolean forcenoseeds = disconnect_seeds_when_seeding;
				if (!seeding_mode) 
				{	// if we're not finished
				
					// if we have no downloadable pieces (due to "do not download")
					// then we disconnect seeds and avoid calling these methods
					// to save CPU.
//					disk_mgr.computePriorityIndicator();

					if (disk_mgr.hasDownloadablePiece()) 
					{
						checkRequests();	//check the requests
						checkDLPossible(0);	//look for downloadable pieces
						checkRescan();
						forcenoseeds = false;
					}
				}
				
				checkSeeds( forcenoseeds );
				updatePeersInSuperSeedMode();
				doUnchokes();
				
      }catch (Throwable e) {
   	
        Debug.printStackTrace( e );
      }

      mainloop_loop_count++;
  }

  
  
  public void 
  stopAll() 
  {

    is_running = false;
    
    PeerControlSchedulerFactory.getSingleton().unregister( this );
       
    peer_database = null;
    
    //remove legacy controller registration
    PeerManager.getSingleton().deregisterLegacyManager( this );
    
    closeAndRemoveAllPeers( "download stopped", false );

    //clear pieces
    for (int i = 0; i < _pieces.length; i++) {
      if (_pieces[i] != null)
        pieceRemoved(_pieces[i]);
    }

    // 5. Remove listeners
		COConfigurationManager.removeParameterListener("Ip Filter Enabled", this);
		COConfigurationManager.removeParameterListener( "Disconnect Seed", this );
		
		ip_filter.removeListener( this );
	}
	
	
	/**
	 * A private method that does analysis of the result sent by the tracker.
	 * It will mainly open new connections with peers provided
	 * and set the timeToWait variable according to the tracker response.
	 * @param tracker_response
	 */
	
	private void 
	analyseTrackerResponse(
		TRTrackerAnnouncerResponse	tracker_response )
	{
		// tracker_response.print();
		TRTrackerAnnouncerResponsePeer[]	peers = tracker_response.getPeers();
		
		if ( peers != null ){
			addPeersFromTracker( tracker_response.getPeers());  
		}
		
		Map extensions = tracker_response.getExtensions();
		
		if (extensions != null ){
			addExtendedPeersFromTracker( extensions );
		}
	}
	
	public void
	processTrackerResponse(
		TRTrackerAnnouncerResponse	response )
	{
		// only process new peers if we're still running
		if ( is_running ){
			analyseTrackerResponse( response );
		}
	}
	
	private void
	addExtendedPeersFromTracker(
		Map		extensions )
	{
		Map	protocols = (Map)extensions.get("protocols");
		
		if ( protocols != null ){
			
			System.out.println( "PEPeerControl: tracker response contained protocol extensions");
			
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
		
	    if (!ip_filter.isInRange(transport.getIp(), adapter.getDisplayName())) {

		ArrayList peer_transports = peer_transports_cow;
		
		if ( !peer_transports.contains(transport)){
			
			addToPeerTransports( transport );
			
		}else{
			Debug.out( "addPeer():: peer_transports.contains(transport): SHOULD NEVER HAPPEN !" );
			transport.closeConnection( "already connected" );
		}
	    }else{
	    	
	        transport.closeConnection( "IP address blocked by filters" );
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
		
		closeAndRemovePeer( transport, "remove peer" );
	}

  private void closeAndRemovePeer( PEPeerTransport peer, String reason ) {
    boolean removed = false;
    
    // copy-on-write semantics
    try{
      peer_transports_mon.enter();
          
        if ( peer_transports_cow.contains( peer )){

          ArrayList new_peer_transports = new ArrayList( peer_transports_cow );
          
          new_peer_transports.remove( peer );
           
          peer_transports_cow = new_peer_transports;
          
          removed = true;
        }
    }
    finally{ 
      peer_transports_mon.exit();
    }
    
    if( removed ) {
    	peer.closeConnection( reason );
      peerRemoved( peer );  //notify listeners      
    }
    else {
    	Debug.out( "closeAndRemovePeer(): peer not removed" );
    }
  }
  
  
  
	private void closeAndRemoveAllPeers( String reason, boolean reconnect ) {
		ArrayList peer_transports;
		
		try{
			peer_transports_mon.enter();
			
			peer_transports = peer_transports_cow;
			
			peer_transports_cow = new ArrayList( 0 );  
		}
		finally{
			peer_transports_mon.exit();
		}
		
    for( int i=0; i < peer_transports.size(); i++ ) {
	    PEPeerTransport peer = (PEPeerTransport)peer_transports.get( i );
	      
	    try{

	    	peer.closeConnection( reason );
	     
	    }catch( Throwable e ){
	    		
    		// if something goes wrong with the close process (there's a bug in there somewhere whereby
    		// we occasionally get NPEs then we want to make sure we carry on and close the rest
    		
	    	Debug.printStackTrace(e);
	    }
	    
	    try{
	    	peerRemoved( peer );  //notify listeners
	    	
	    }catch( Throwable e ){
    		   		
	    	Debug.printStackTrace(e);
	    }
    }
    
    if( reconnect ) {
      for( int i=0; i < peer_transports.size(); i++ ) {
        PEPeerTransport peer = (PEPeerTransport)peer_transports.get( i );
        
        if( peer.getTCPListenPort() > 0 ) {
          PEPeerTransport new_conn = PEPeerTransportFactory.createTransport( this, peer.getPeerSource(), peer.getIp(), peer.getTCPListenPort() );
          addToPeerTransports( new_conn );
        }
      }
    }
  }
	
	
	
	
	public void addPeer( String ip_address, int port ) {		
		PeerItem peer_item = PeerItemFactory.createPeerItem( ip_address, port, PeerItem.convertSourceID( PEPeerSource.PS_PLUGIN ) );
		
		if( !isAlreadyConnected( peer_item ) ) {
			boolean added = makeNewOutgoingConnection( PEPeerSource.PS_PLUGIN, ip_address, port );  //directly inject the the imported peer
			if( !added )  Debug.out( "injected peer was not added" );
	}
	}
	
	
	
	private void 
	addPeersFromTracker(
		TRTrackerAnnouncerResponsePeer[]		peers )
	{
		
		for (int i = 0; i < peers.length; i++){
			TRTrackerAnnouncerResponsePeer	peer = peers[i];
			
			ArrayList peer_transports = peer_transports_cow;
			
			boolean already_connected = false;
			
      		for( int x=0; x < peer_transports.size(); x++ ) {
				PEPeerTransport transport = (PEPeerTransport)peer_transports.get( x );
				
				// allow loopback connects for co-located proxy-based connections and testing
				
				if( peer.getAddress().equals( transport.getIp() )){
					
					boolean same_allowed = COConfigurationManager.getBooleanParameter( "Allow Same IP Peers" ) ||
					transport.getIp().equals( "127.0.0.1" );
					
					if( !same_allowed || peer.getPort() == transport.getPort() ) {
						already_connected = true;
						break;
					}
				}
			}
			
			if( already_connected )  continue;
			
			if( peer_database != null ) {
				PeerItem item = PeerItemFactory.createPeerItem( peer.getAddress(), peer.getPort(), PeerItem.convertSourceID( peer.getSource() ) );
				peer_database.addDiscoveredPeer( item );
			}
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
		String	peer_source,
		String 	address, 
		int port ) 
	{    
		//make sure this connection isn't filtered
    if( ip_filter.isInRange( address, adapter.getDisplayName() ) ) {
			return false;
		}
		
		//make sure we need a new connection
		int needed = getMaxNewConnectionsAllowed();
		if( needed == 0 )  return false;
		
		//make sure not already connected to the same IP address; allow loopback connects for co-located proxy-based connections and testing
		boolean same_allowed = COConfigurationManager.getBooleanParameter( "Allow Same IP Peers" ) || address.equals( "127.0.0.1" );
		if( !same_allowed && PeerIdentityManager.containsIPAddress( _hash, address ) ){  
			return false;
		}
		
		if( PeerUtils.ignorePeerPort( port ) ) {
    	if (Logger.isEnabled())
				Logger.log(new LogEvent(disk_mgr.getTorrent(), LOGID,
						"Skipping connect with " + address + ":" + port
								+ " as peer port is in ignore list."));
			return false;
		}
		
		//start the connection
		PEPeerTransport real = PEPeerTransportFactory.createTransport( this, peer_source, address, port );
		
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
		int	nbPieces	=_nbPieces;
		for (int i = 0; i < nbPieces; i++) {
			PEPiece piece = _pieces[i];		//get the piece
			//if piece is loaded, and completed
			if (piece !=null &&dm_pieces[i].calcWritten() &&!dm_pieces[i].isChecking())
			{
				//check the piece from the disk
				dm_pieces[i].setChecking();
				
				DiskManagerCheckRequest req = 
					disk_mgr.createCheckRequest(
						i, new Integer(CHECK_REASON_DOWNLOADED));
						
				req.setAdHoc( false );
				
				disk_mgr.enqueueCheckRequest( req, this );
			}
		}
	}

	private void checkEmptyPiece(int pieceNumber)
	{
		if (_pieces[pieceNumber] ==null ||dm_pieces[pieceNumber].isRequested())
			return;
		PEPieceImpl piece =_pieces[pieceNumber];
		if (dm_pieces[pieceNumber].getNbWritten() >0 ||piece.getNbRequests() >0 ||piece.getSpeed() >0 ||piece.getReservedBy() !=null)
			return;
		pieceRemoved(_pieces[pieceNumber]);
	}

	/**
	 * Check if a piece's Speed is too fast for it to be getting new data
	 * and if a reserved pieced failed to get data within 120 seconds
	 */
	private void checkSpeedAndReserved()
	{
		long			now			=SystemTime.getCurrentTime();
		int				nbPieces	=_nbPieces;
		PEPieceImpl[]	pieces		=_pieces;
		//for every piece
		for (int i =0; i <nbPieces; i++)
		{
			// these checks are only against pieces being downloaded yet needing requests still/again
			if (pieces[i] !=null)
			{
				long time_since_write	=now -dm_pieces[i].getLastWriteTime();
				if (time_since_write >4001 &&(mainloop_loop_count %MAINLOOP_FIVE_SECOND_INTERVAL) ==0)
				{
					// maybe piece's speed is too high for it to get new data
					if (pieces[i].getSpeed() >0)
					{
					    if (!dm_pieces[i].isRequested())
					    	_pieces[i].decSpeed();
					    else
					    	_pieces[i].setSpeed(0);
					} else if (time_since_write >(120 *1000))
					{
						dm_pieces[i].clearRequested();
						// has reserved piece gone stagnant?
						PEPeerTransport pt =(PEPeerTransport)pieces[i].getReservedBy();
						if (pt !=null)
						{
							// Peer is too slow; Ban them and unallocate the piece
							// but, banning is no good for peer types that get pieces reserved
							// to them for other reasons, such as special seed peers
							if (pt.isSeed())
								closeAndRemovePeer(pt, "Reserved piece data timeout; 120 seconds" );
							else
								badPeerDetected(pt);
							_pieces[i].setReservedBy(null);
						} else if (time_since_write >(240 *1000))
							checkEmptyPiece(i);
					}
				}
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
			
			List pieces;
			
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
				
	    		processPieceCheckResult((DiskManagerCheckRequest)data[0],((Integer)data[1]).intValue());
	    			
			}
		}
	}

	/**
	 * one reason requests stem from here (PeerControl) rather than within each peer object themselves
	 * is so the connections can be sorted by best uploaders, which provides some effort to download
	 * the most important (ie; rarest and/or highest priority) pieces faster/more reliably
	 * @param candidateMode specifies how pieces should be selected
	 *	soon-to-be implemented features include; sequantial, reverse order, ignore rarity, full pieces only 
	 */
	private void checkDLPossible(int candidateMode)
	{
		List bestUploaders =new ArrayList();

		List peer_transports =peer_transports_cow;

		long[] upRates =new long[peer_transports.size()];
		Arrays.fill(upRates, -1);

		for (int i =0; i <peer_transports.size(); i++ )
		{
			PEPeerTransport pc =(PEPeerTransport) peer_transports.get(i);

			if (pc.transferAvailable())
			{
				long upRate =pc.getStats().getSmoothDataReceiveRate();
				UnchokerUtil.updateLargestValueFirstSort(upRate, upRates, pc, bestUploaders, 0);
			}
		}

		checkEndGameMode();
		for (int i =0; i <bestUploaders.size(); i++ )
		{
			// get a connection
			PEPeerTransportProtocol pc =(PEPeerTransportProtocol) bestUploaders.get(i);
			// can we transfer something?
			if (pc.transferAvailable())
			{
				boolean found =true;
				// If queue is too low, we will enqueue another request
				int maxRequests =MIN_REQUESTS +(int) (pc.getStats().getDataReceiveRate() /SLOPE_REQUESTS);
				if (maxRequests >MAX_REQUESTS ||maxRequests <0)
					maxRequests =MAX_REQUESTS;

				if (endGameMode)
					maxRequests =2;
				if (pc.isSnubbed())
					maxRequests =1;

				// Only loop when 3/5 of the queue is empty,
				// in order to make more consecutive requests, and improve
				// cache efficiency
				if (pc.getNbRequests() <=(3 *maxRequests) /5)
				{
					while ((pc.getPeerState() ==PEPeer.TRANSFERING) &&found &&(pc.getNbRequests() <maxRequests))
					{
						// is there anything else to download?
						if (endGameMode)
							found =piecePicker.findPieceInEndGameMode(pc);
						else
							found =piecePicker.findPieceToDownload(pc, candidateMode);
					}
				}
			}
		}
	}
	
	protected void
	checkRescan()
	{
		if ( rescan_piece_time == 0 ){
			
				// pending a piece completion
			
			return;
		}
		
		if ( next_rescan_piece == -1 ){
			
			if ( mainloop_loop_count % MAINLOOP_FIVE_SECOND_INTERVAL == 0 ){
			
				if ( adapter.isPeriodicRescanEnabled()){
					
					next_rescan_piece	= 0;
				}
			}
		}else{
			
			if ( mainloop_loop_count % MAINLOOP_TEN_MINUTE_INTERVAL == 0 ){

				if ( !adapter.isPeriodicRescanEnabled()){
					
					next_rescan_piece	= -1;
				}
			}
		}
		
		if ( next_rescan_piece == -1 ){
			
			return;
		}
		
			// delay as required
		
		long	now = SystemTime.getCurrentTime();
		
		if ( rescan_piece_time > now ){
			
			rescan_piece_time	= now;
		}
		
			// 250K/sec limit
		
		long	piece_size = disk_mgr.getPieceLength();
		
		long	millis_per_piece = piece_size / 250;
		
		if ( now - rescan_piece_time < millis_per_piece ){
			
			return;
		}
		
		while( next_rescan_piece != -1 ){
			
			int	this_piece = next_rescan_piece;
			
			next_rescan_piece++;
			
			if ( next_rescan_piece == _pieces.length ){
				
				next_rescan_piece	= -1;
			}
			
			if ( _pieces[this_piece] == null && !dm_pieces[this_piece].isDone()){
				
				DiskManagerCheckRequest	req = 
					disk_mgr.createCheckRequest(
						this_piece, 
						new Integer( CHECK_REASON_SCAN ));	
				
			   	if ( Logger.isEnabled()){
			   		
					Logger.log(
							new LogEvent(
								disk_mgr.getTorrent(), LOGID,
								"Rescanning piece " + this_piece ));
							
			   	}
			   	
				rescan_piece_time	= 0;	// mark as check piece in process
				
				try{
					disk_mgr.enqueueCheckRequest( req, this );
					
				}catch( Throwable e ){
					
					rescan_piece_time	= now;
					
					Debug.printStackTrace(e);
				}
				
				break;
			}
		}
	}
	
	/**
	 * This method checks if the downloading process is finished.
	 * 
	 */
	private void checkFinished(boolean start_of_day)
	{
		boolean all_pieces_done =disk_mgr.getRemaining() ==0;

		if (all_pieces_done)
		{
			seeding_mode =true;
			if (endGameMode)
			{
				piecePicker.clearEndGameChunks();
				endGameMode =false;
			}

			if (!start_of_day)
				adapter.setStateFinishing();

			_timeFinished =SystemTime.getCurrentTime();
			List peer_transports =peer_transports_cow;

			// remove previous snubbing
			for (int i =0; i <peer_transports.size(); i++ )
			{
				PEPeerTransport pc =(PEPeerTransport) peer_transports.get(i);
				pc.setSnubbed(false);
			}

			// Disconnect seeds
			checkSeeds(true);
			boolean checkPieces =COConfigurationManager.getBooleanParameter("Check Pieces on Completion", true);

			// re-check all pieces to make sure they are not corrupt, but only if we weren't already complete
			if (checkPieces &&!start_of_day)
			{
				DiskManagerCheckRequest req =disk_mgr.createCheckRequest(-1, new Integer(CHECK_REASON_COMPLETE));
				disk_mgr.enqueueCompleteRecheckRequest(req, this);
			}

			disk_mgr.downloadEnded();
			_timeStartedSeeding =SystemTime.getCurrentTime();
			adapter.setStateSeeding(start_of_day);
		}
	}
	
	/**
	 * This method will locate expired requests on peers, will cancel them, and mark the peer as snubbed
	 */
	private void checkRequests()
	{
		//for every connection
		List	peer_transports = peer_transports_cow;
		
		for (int i =peer_transports.size() -1; i >=0 ; i--)
		{
			PEPeerTransport pc =(PEPeerTransport)peer_transports.get(i);
			if (pc.getPeerState() ==PEPeer.TRANSFERING)
			{
				List expired = pc.getExpiredRequests();
				
				if (expired !=null &&expired.size() >0)
				{
					pc.setSnubbed(true);
					
					//Only cancel first request if more than 2 mins have passed
					DiskManagerReadRequest request =(DiskManagerReadRequest) expired.get(0);
					
					long wait_time =SystemTime.getCurrentTime() -request.getTimeCreated();
					
					if (wait_time >120 *1000)
					{
						pc.sendCancel(request); //cancel the request object
						//get the piece number
						int pieceNumber = request.getPieceNumber();
						//get the block offset
						int pieceOffset = request.getOffset();
						//unmark the block
						if (_pieces[pieceNumber] !=null)
							_pieces[pieceNumber].unmarkBlock(pieceOffset /DiskManager.BLOCK_SIZE);
						//set piece to not fulle requested
						dm_pieces[pieceNumber].clearRequested();
						checkEmptyPiece(pieceNumber);
					}
					
					//for every expired request                              
            		for (int j =1; j <expired.size(); j++)
            		{
						//get the request object
						request =(DiskManagerReadRequest) expired.get(j);
						pc.sendCancel(request);				//cancel the request object
						//get the piece number
						int pieceNumber = request.getPieceNumber();
						//get the piece offset
						int pieceOffset = request.getOffset();
						//unmark the block
						if (_pieces[pieceNumber] !=null)
							_pieces[pieceNumber].unmarkBlock(pieceOffset /DiskManager.BLOCK_SIZE);
						//set piece to not being downloaded
						dm_pieces[pieceNumber].clearRequested();
						checkEmptyPiece(pieceNumber);
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
  	
  	final int WANT_LIMIT = 100;
      
	  int num_wanted = getMaxNewConnectionsAllowed();
	  
	  boolean has_remote = adapter.isNATHealthy();
	  if( has_remote ) {
	    //is not firewalled, so can accept incoming connections,
	    //which means no need to continually keep asking the tracker for peers
	    num_wanted = (int)(num_wanted / 1.5);
	  }
	  
	  if ( num_wanted < 0 || num_wanted > WANT_LIMIT ) {
	    num_wanted = WANT_LIMIT;
	  }
	  
	  int current_connection_count = PeerIdentityManager.getIdentityCount( _hash );
	  
	  TRTrackerScraperResponse tsr = adapter.getTrackerScrapeResponse();
	  
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
	    adapter.setTrackerRefreshDelayOverrides( 100 );  //use normal announce interval
	    return;
	  }
	  
	  if( current_connection_count == 0 )  current_connection_count = 1;  //fudge it :)
	  
	  int current_percent = (current_connection_count * 100) / (current_connection_count + num_wanted);
	  
	  adapter.setTrackerRefreshDelayOverrides( current_percent );  //set dynamic interval override
  }

	public int[] getAvailability() 
	{
		return availability_cow;
	}
	
	/**
	 * This methd will compute the pieces' overall availability (including ourself)
	 * and the _globalMinOthers & _globalAvail
	 */
	public void computeAvailability()
	{
		long time_since_last	=SystemTime.getCurrentTime() -_time_last_avail;

		if ((time_since_last >=0) &&(time_since_last <TIME_MIN_AVAILABILITY))
			return;
		int 	i;
		int		nbPieces		=_nbPieces;
		//reset the availability
		int[] availability =new int[nbPieces];

		//for all peers
		List	peer_transports =peer_transports_cow;
		for (i =peer_transports.size() -1; i >=0; i--)
		{
			//get the peer connection
			PEPeerTransport pc =(PEPeerTransport)peer_transports.get(i);
			//get an array of available pieces    
			boolean[] piecesAvailable =pc.getAvailable();
			if (piecesAvailable !=null)				//if the list is not null
			{
				for (int j =0; j <nbPieces; j++)	//loop for every piece
				{
					if (piecesAvailable[j])			//set the piece to available
						availability[j]++;
				}
			}
		}
		int allMin		=Integer.MAX_VALUE;
		int othersMin	=Integer.MAX_VALUE;
		//Add our own availability
		for (i =dm_pieces.length -1; i >=0; i--)
		{
			if (availability[i] >0)
				dm_pieces[i].setAvail();
			else
				dm_pieces[i].clearAvail();
			if (dm_pieces[i].isDone())
				availability[i]++;
			else if (availability[i] >0 &&availability[i] <othersMin &&dm_pieces[i].isNeeded() &&!dm_pieces[i].isRequested() &&dm_pieces[i].isRequestable())
				othersMin =availability[i];	// only what we want to target for near future requests from others

			if (availability[i] <allMin)
				allMin =availability[i];
		}
		//copy updated local variables into globals
		availability_cow =availability;
		piecePicker.setGlobalMin(allMin);
		piecePicker.setGlobalMinOthers(othersMin);

		int		total =0;
		int		rarest =0;
		long	TotalAvail =0;
		for (i =0; i <nbPieces; i++)
		{
			if (availability[i] >0)
			{
				if (availability[i] >allMin)
					total++;
				if (availability[i] <=othersMin &&_pieces[i] !=null &&!dm_pieces[i].isRequestable())
					rarest++;
				TotalAvail +=availability[i];
			}
		}
		//copy updated local variables into globals
		piecePicker.setRarestRunning(rarest);
		_globalAvail =(total /(float)nbPieces) +allMin;
		_globalAvgAvail =TotalAvail /(float)(nbPieces) /(1+ _seeds +_peers);
		_time_last_avail =SystemTime.getCurrentTime();
	}

	//this only gets called when the My Torrents view is displayed
	public float getMinAvailability()
	{
		return _globalAvail;
	}

	public float getAvgAvail()
	{
		return _globalAvgAvail;
	}

	public void addPeerTransport( PEPeerTransport transport ) {
    if (!ip_filter.isInRange(transport.getIp(), adapter.getDisplayName())) {
			ArrayList peer_transports = peer_transports_cow;
			
			if (!peer_transports.contains( transport )) {
				addToPeerTransports(transport);    
			}
			else{
				Debug.out( "addPeerTransport():: peer_transports.contains(transport): SHOULD NEVER HAPPEN !" );        
				transport.closeConnection( "already connected" );
			}
		}
		else {
			transport.closeConnection( "IP address blocked by filters" );
		}
	}
	
	
	/**
	 * Do all peer choke/unchoke processing.
	 */
	private void doUnchokes() {  
    int max_to_unchoke = adapter.getMaxUploads();  //how many simultaneous uploads we should consider
		ArrayList peer_transports = peer_transports_cow;
		
		//determine proper unchoker
		if( seeding_mode ) {
			if( unchoker == null || !(unchoker instanceof SeedingUnchoker) ) {
				unchoker = new SeedingUnchoker();
			}
		}
		else {
			if( unchoker == null || !(unchoker instanceof DownloadingUnchoker) ) {
				unchoker = new DownloadingUnchoker();
			}
		}
		
		
		//do main choke/unchoke update every 10 secs
		if( mainloop_loop_count % MAINLOOP_TEN_SECOND_INTERVAL == 0 ) {
			
			boolean refresh = mainloop_loop_count % MAINLOOP_THIRTY_SECOND_INTERVAL == 0;
			
			unchoker.calculateUnchokes( max_to_unchoke, peer_transports, refresh );
			
			ArrayList peers_to_choke = unchoker.getChokes();
			ArrayList peers_to_unchoke = unchoker.getUnchokes();
			
			//do chokes
			for( int i=0; i < peers_to_choke.size(); i++ ) {
				PEPeerTransport peer = (PEPeerTransport)peers_to_choke.get( i );
				
				if( !peer.isChokedByMe() ) {
					peer.sendChoke(); 
				}
			}
			
			//do unchokes
			for( int i=0; i < peers_to_unchoke.size(); i++ ) {
				PEPeerTransport peer = (PEPeerTransport)peers_to_unchoke.get( i );
				
				if( peer.isChokedByMe() ) {
					peer.sendUnChoke();
				}
			}
		}
		else if( mainloop_loop_count % MAINLOOP_ONE_SECOND_INTERVAL == 0 ) {  //do quick unchoke check every 1 sec
			
			ArrayList peers_to_unchoke = unchoker.getImmediateUnchokes( max_to_unchoke, peer_transports );
			
			//do unchokes
			for( int i=0; i < peers_to_unchoke.size(); i++ ) {
				PEPeerTransport peer = (PEPeerTransport)peers_to_unchoke.get( i );
				
				if( peer.isChokedByMe() ) {
					peer.sendUnChoke();
				}
			}
		}
		
	}
	
	
//	send the have requests out
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
    
    ArrayList to_close = null;
    
    List	peer_transports = peer_transports_cow;          
    for (int i = 0; i < peer_transports.size(); i++) {
      PEPeerTransport pc = (PEPeerTransport) peer_transports.get(i);
      
      if (pc != null && pc.getPeerState() == PEPeer.TRANSFERING && pc.isSeed()) {      	
      	if( to_close == null )  to_close = new ArrayList();
				to_close.add( pc );
      }
    }
		
		if( to_close != null ) {		
			for( int i=0; i < to_close.size(); i++ ) {  			
				closeAndRemovePeer( (PEPeerTransport)to_close.get(i), "disconnect other seed when seeding" );
			}
		}
  }

  
  
  
	private void updateStats() {   
		//calculate seeds vs peers
		List	peer_transports = peer_transports_cow;
		
		_seeds = _peers = _remotes = 0;
		for (int i = 0; i < peer_transports.size(); i++) {
			PEPeerTransport pc = (PEPeerTransport) peer_transports.get(i);
			if (pc.getPeerState() == PEPeer.TRANSFERING) {
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
	public void requestCanceled(DiskManagerReadRequest request)
	{
		int pieceNumber =request.getPieceNumber();	//get the piece number
		int pieceOffset =request.getOffset();		//get the piece offset    
		if (_pieces[pieceNumber] !=null)
			_pieces[pieceNumber].unmarkBlock(pieceOffset /DiskManager.BLOCK_SIZE);
		//set as not fully Requested
		dm_pieces[pieceNumber].clearRequested();
	}
	
	
	public PEPeerControl
	getControl()
	{
		return( this );
	}
	
//	get the hash value
	public byte[] getHash() {
		return _hash.getDataID();
	}
	
	public PeerIdentityDataID
	getPeerIdentityDataID()
	{
		return( _hash );
	}
	
//	get the peer id value
	public byte[] getPeerId() {
		return _myPeerId;
	}
	
//	get the remaining percentage
	public long getRemaining() {
		return disk_mgr.getRemaining();
	}
	
	
	public void discarded(int length) {
		if (length > 0){
			_stats.discarded(length);
		}
	}
	
	public void dataBytesReceived(int length) {
		if (length > 0) {
			_stats.dataBytesReceived(length);
			
			_averageReceptionSpeed.addValue(length);
		}
	}
	
	
	public void protocolBytesReceived( int length ) {
		if (length > 0) {
			_stats.protocolBytesReceived(length);
		}
	}
	
	public void dataBytesSent(int length) {
		if (length > 0) {
			_stats.dataBytesSent(length);
		}
	}
	
	
	public void protocolBytesSent( int length ) {
		if (length > 0) {
			_stats.protocolBytesSent(length);
		}
	}
	
	
  //setup the diskManager
  protected void 
  setDiskManager(
  	DiskManager diskManager ) 
  {
		//the diskManager that handles read/write operations
		disk_mgr	= diskManager;
		dm_pieces		= disk_mgr.getPieces();
		_nbPieces		= disk_mgr.getNbPieces();
		
		//the pieces in progress
		if (_pieces ==null) 
			_pieces = new PEPieceImpl[dm_pieces.length];
		
		for (int i = 0; i < dm_pieces.length; i++)
		{
			DiskManagerPiece	dm_piece = dm_pieces[i];
			if (!dm_piece.isDone() && dm_piece.getNbWritten() >0)
			{
				addPiece(new PEPieceImpl( this, dm_piece, 0, true ), i);
			}
		}
		
		//the availability level of each piece in the network
		if (null ==availability_cow)
			availability_cow = new int[_nbPieces];

		//the stats
		_stats =new PEPeerManagerStatsImpl(this);
		
		piecePicker =disk_mgr.getPiecePicker();
	}
	
	public void writeCompleted(DiskManagerWriteRequest request)
	{
		int pieceNumber =request.getPieceNumber();
		if (_pieces[pieceNumber] !=null)
		{
			_pieces[pieceNumber].setWritten( (PEPeer)request.getUserData(), request.getOffset() / DiskManager.BLOCK_SIZE );
		}
	}

  public void 
  writeFailed( 
	DiskManagerWriteRequest 	request, 
	Throwable		 			cause )
  {
  }
  
  public void writeBlock(int pieceNumber, int offset, DirectByteBuffer data,PEPeer sender) {
    PEPiece piece = _pieces[pieceNumber];
    int blockNumber = offset / DiskManager.BLOCK_SIZE;
    if (piece != null && !dm_pieces[pieceNumber].isWritten(blockNumber)) {
      _pieces[pieceNumber].setBlockWritten(blockNumber);
      DiskManagerWriteRequest request = disk_mgr.createWriteRequest(pieceNumber, offset, data, sender );
      disk_mgr.enqueueWriteRequest(request, this );
      if(endGameMode)
      {
        //In case we are in endGame mode, remove the piece from the chunk list
        piecePicker.removeFromEndGameModeChunks(pieceNumber, offset);

        //For all connections cancel the request
        List	peer_transports = peer_transports_cow;
        for (int i=0;i<peer_transports.size();i++)
        {
            PEPeerTransport connection = (PEPeerTransport)peer_transports.get(i);
            DiskManagerReadRequest dmr = disk_mgr.createReadRequest( pieceNumber, offset, piece.getBlockSize(blockNumber));
            connection.sendCancel( dmr );
          }
      }
		} else
    		data.returnToPool();
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
		if (piece !=null && !dm_pieces[pieceNumber].isWritten(blockNumber)) {
			_pieces[pieceNumber].setBlockWritten(blockNumber);
			
      DiskManagerWriteRequest request = disk_mgr.createWriteRequest(pieceNumber, offset, data, sender);
      
      disk_mgr.enqueueWriteRequest(request, this);

			//cancel any matching outstanding requests
			List	peer_transports = peer_transports_cow;
			for (int i=0;i<peer_transports.size();i++){
				PEPeerTransport connection = (PEPeerTransport)peer_transports.get(i);
				DiskManagerReadRequest dmr = disk_mgr.createReadRequest( pieceNumber, offset, piece.getBlockSize(blockNumber));
				connection.sendCancel( dmr );   
			}
		}else
			data.returnToPool();
	}
	
	
	public boolean isBlockAlreadyWritten(int piece_number, int offset)
	{
		return dm_pieces[piece_number].isWritten(offset /DiskManager.BLOCK_SIZE);
	}

	public boolean checkBlock(int pieceNumber, int offset, int length) {
		return disk_mgr.checkBlockConsistency(pieceNumber, offset, length);
	}

	public boolean checkBlock(int pieceNumber, int offset, DirectByteBuffer data) {
		return disk_mgr.checkBlockConsistency(pieceNumber, offset, data);
	}
	
	public int getAvailability(int pieceNumber)
	{
		if (null !=availability_cow)
		{
			return availability_cow[pieceNumber]; 
		}
		return 0;
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
		int availability =availability_cow[pieceNumber];
		if (availability < 4) {
			if (dm_pieces[pieceNumber].isDone())
				availability--;
			if (availability <= 0)
				return;
			//for all peers
			
			List	peer_transports = peer_transports_cow;
			
			for (int i = peer_transports.size() - 1; i >= 0; i--) {
				PEPeerTransport pc = (PEPeerTransport) peer_transports.get(i);
				if (pc != pcOrigin && pc.getPeerState() == PEPeer.TRANSFERING) {
					boolean[] peerAvailable = pc.getAvailable();
					if (peerAvailable[pieceNumber])
						((PEPeerStatsImpl)pc.getStats()).statisticalSentPiece(pieceLength / availability);
				}
			}
		}
	}
	
	public int getPieceLength(int pieceNumber) {
		if (pieceNumber ==_nbPieces -1)
			return disk_mgr.getLastPieceLength();
		return disk_mgr.getPieceLength();
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
	
	public int getNbActive()
	{
		return nbActivePieces;
	}
	
  public long getLastRemoteConnectionTime()
  {
	  return( last_remote_time );
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
	public long 
	getETA() 
	{	
		long	now = SystemTime.getCurrentTime();
		
		if ( now < last_eta_calculation || now - last_eta_calculation > 900 ){
			
			long dataRemaining = disk_mgr.getRemainingExcludingDND();
	
			if ( dataRemaining > 0 ){
				
				int writtenNotChecked = 0;
				
				for (int i = 0; i < _pieces.length; i++)
				{
					if (dm_pieces[i].isInteresting()){
						writtenNotChecked +=dm_pieces[i].getNbWritten() *DiskManager.BLOCK_SIZE;
					}
				}
			
				dataRemaining = dataRemaining - writtenNotChecked;
			
				if  (dataRemaining < 0 ){
					
					dataRemaining	= 0;
				}
			}
			
			long	result;
			
			if (dataRemaining == 0) {
				long timeElapsed = (_timeFinished - _timeStarted)/1000;
				//if time was spent downloading....return the time as negative
				if(timeElapsed > 1){
					result = timeElapsed * -1;
				}else{
					result = 0;
				}
			}else{
			
				long averageSpeed = _averageReceptionSpeed.getAverage();
				long lETA = (averageSpeed == 0) ? Constants.INFINITY_AS_INT : dataRemaining / averageSpeed;
				// stop the flickering of ETA from "Finished" to "x seconds" when we are 
				// just about complete, but the data rate is jumpy.
				if (lETA == 0)
					lETA = 1;
				result = lETA;
			}
		
			last_eta				= result;
			last_eta_calculation	= now;
		}
		
		return( last_eta );
	}
	
	
	
	private void
	addToPeerTransports(
		PEPeerTransport		peer )
	{
		boolean added = false;
		
		try{
			peer_transports_mon.enter();
			
			if( peer_transports_cow.contains( peer ) ){
				Debug.out( "Transport added twice" );
				return;  //we do not want to close it
			}
			
			if( is_running ) {
				//copy-on-write semantics
				ArrayList new_peer_transports = new ArrayList(peer_transports_cow.size() +1);
				
				new_peer_transports.addAll( peer_transports_cow );
				
				new_peer_transports.add( peer );
				
				peer_transports_cow = new_peer_transports;
				
				added = true;
			}
		}
		finally{
			peer_transports_mon.exit();
		}
		
		if( added ) {
      if ( peer.isIncoming()){
	      long	connect_time = SystemTime.getCurrentTime();
	        
	      if ( connect_time > last_remote_time ){
	        	
	       	last_remote_time = connect_time;
	      }
      }
      
			peerAdded( peer ); 
		}
		else {
			peer.closeConnection( "PeerTransport added when manager not running" );
		}
	}
	
	
//	the peer calls this method itself in closeConnection() to notify this manager
	public void peerConnectionClosed( PEPeerTransport peer ) {
		boolean	connection_found = false;
		
		try{
			peer_transports_mon.enter();
			
			if( peer_transports_cow.contains( peer )) {
				
				ArrayList new_peer_transports = new ArrayList( peer_transports_cow );
				
				new_peer_transports.remove(peer);
				
				peer_transports_cow = new_peer_transports;
				
				connection_found  = true;
			}
		}
		finally{
			peer_transports_mon.exit();
		}
		
		if ( connection_found ){
    	if( peer.getPeerState() != PEPeer.DISCONNECTED ) {
    		System.out.println( "peer.getPeerState() != PEPeer.DISCONNECTED: " +peer.getPeerState() );
    	}
    	
			peerRemoved( peer );  //notify listeners
		}
	}
	
	
	
	
	public void 
	peerAdded(
		PEPeer pc) 
	{
    adapter.addPeer(pc);  //async downloadmanager notification
		
		//sync peermanager notification
		ArrayList peer_manager_listeners = peer_manager_listeners_cow;
		
		for( int i=0; i < peer_manager_listeners.size(); i++ ) {
      		((PEPeerManagerListener)peer_manager_listeners.get(i)).peerAdded( this, pc );
		}
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
		
    adapter.removePeer(pc);  //async downloadmanager notification
		
		//sync peermanager notification
		ArrayList peer_manager_listeners = peer_manager_listeners_cow;
		
		for( int i=0; i < peer_manager_listeners.size(); i++ ) {
      		((PEPeerManagerListener)peer_manager_listeners.get(i)).peerRemoved( this, pc );
		}
	}
	

	public void addPiece(PEPiece piece, int pieceNumber)
	{
		_pieces[pieceNumber] =(PEPieceImpl)piece;
		adapter.addPiece(piece);
		nbActivePieces++;
	}
	
	public void pieceRemoved(PEPiece p) {
		adapter.removePiece(p);
		_pieces[p.getPieceNumber()] =null;
		nbActivePieces--;
	}
	
	public String getElapsedTime() {
		return TimeFormatter.format((SystemTime.getCurrentTime() - _timeStarted) / 1000);
	}
	
//	Returns time started in ms
	public long getTimeStarted() {
		return _timeStarted;
	}
	
	public long getTimeStartedSeeding() {
		return _timeStartedSeeding;
	}
	
	private byte[] computeMd5Hash(DirectByteBuffer buffer)
	{
		BrokenMd5Hasher md5 =new BrokenMd5Hasher();
		md5.reset();
		int position =buffer.position(DirectByteBuffer.SS_DW);
		md5.update(buffer.getBuffer(DirectByteBuffer.SS_DW));
		buffer.position(DirectByteBuffer.SS_DW, position);
		ByteBuffer md5Result =ByteBuffer.allocate(16);
		md5Result.position(0);
		md5.finalDigest(md5Result);

		byte[] result =new byte[16];
		md5Result.position(0);
		for (int i =0; i <result.length; i++ )
		{
			result[i] =md5Result.get();
		}

		return result;
	}

	private void MD5CheckPiece(PEPiece piece, boolean correct)
	{
		PEPeer[] writers =piece.getWriters();
		int offset =0;
		for (int i =0; i <writers.length; i++ )
		{
			int length =piece.getBlockSize(i);
			PEPeer peer =writers[i];
			if (peer !=null)
			{
				DirectByteBuffer buffer =disk_mgr.readBlock(piece.getPieceNumber(), offset, length);

				if (buffer !=null)
				{
					byte[] hash =computeMd5Hash(buffer);
					buffer.returnToPool();
					buffer =null;
					piece.addWrite(i, peer, hash, correct);
				}
			}
			offset +=length;
		}
	}

	public void checkCompleted(DiskManagerCheckRequest request, boolean passed)
	{
		try
		{
			piece_check_result_list_mon.enter();
			piece_check_result_list.add(new Object[]{request, new Integer(passed ?1 :0)});
		} finally
		{
			piece_check_result_list_mon.exit();
		}
	}

	public void checkCancelled(DiskManagerCheckRequest request)
	{
		try
		{
			piece_check_result_list_mon.enter();
			piece_check_result_list.add(new Object[]{request, new Integer(2)});

		} finally
		{
			piece_check_result_list_mon.exit();
		}
	}

	public void checkFailed(DiskManagerCheckRequest request, Throwable cause)
	{
		try
		{
			piece_check_result_list_mon.enter();
			piece_check_result_list.add(new Object[]{request, new Integer(0)});

		} finally
		{
			piece_check_result_list_mon.exit();
		}
	}

	public boolean needsMD5CheckOnCompletion(int pieceNumber)
	{
		PEPieceImpl piece = _pieces[pieceNumber];    
		if(piece == null)
			return false;
		return piece.getPieceWrites().size() > 0;
	}
	
	private void processPieceCheckResult(DiskManagerCheckRequest request, int outcome)
	{
		int check_type =((Integer) request.getUserData()).intValue();

		try
		{
			int pieceNumber =request.getPieceNumber();
			dm_pieces[pieceNumber].clearRequested();

			// piece can be null when running a recheck on completion
			PEPieceImpl pePiece =_pieces[pieceNumber];

			// System.out.println( "processPieceCheckResult(" + _finished + "/" + recheck_on_completion + "):" + pieceNumber +
			// "/" + piece + " - " + result );

			// passed = 1, failed = 0, cancelled = 2
			if (outcome ==1 &&pePiece !=null)
				pieceRemoved(pePiece);

			if (check_type ==CHECK_REASON_COMPLETE)
			{ // this is a recheck, so don't send HAVE msgs
				if (outcome ==0)
				{
					// piece failed; restart the download afresh
					Debug.out("Piece #" +pieceNumber +" failed final re-check. Re-downloading...");

					if (!restart_initiated)
					{
						restart_initiated =true;
						adapter.restartDownload(false);
					}
				}
				return;
			}

			// the piece has been written correctly
			if (outcome ==1)
			{
				if (pePiece !=null)
				{
					if (needsMD5CheckOnCompletion(pieceNumber))
						MD5CheckPiece(pePiece, true);

					List list =pePiece.getPieceWrites();
					if (list.size() >0)
					{
						// For each Block
						for (int i =0; i <pePiece.getNbBlocks(); i++ )
						{
							// System.out.println("Processing block " + i);
							// Find out the correct hash
							List listPerBlock =pePiece.getPieceWrites(i);
							byte[] correctHash =null;
							// PEPeer correctSender = null;
							Iterator iterPerBlock =listPerBlock.iterator();
							while (iterPerBlock.hasNext())
							{
								PEPieceWriteImpl write =(PEPieceWriteImpl) iterPerBlock.next();
								if (write.isCorrect())
								{
									correctHash =write.getHash();
									// correctSender = write.getSender();
								}
							}
							// System.out.println("Correct Hash " + correctHash);
							// If it's found
							if (correctHash !=null)
							{
								iterPerBlock =listPerBlock.iterator();
								while (iterPerBlock.hasNext())
								{
									PEPieceWriteImpl write =(PEPieceWriteImpl) iterPerBlock.next();
									if (!Arrays.equals(write.getHash(), correctHash))
									{
										// Bad peer found here
										PEPeer peer =write.getSender();
										badPeerDetected((PEPeerTransport) peer);
									}
								}
							}
						}
					}
				}
				_pieces[pieceNumber] =null;

				// send all clients a have message
				sendHave(pieceNumber);
			} else if (outcome ==0)
			{
				// the piece is corrupt
				if (pePiece !=null)
				{
					MD5CheckPiece(pePiece, false);

					PEPeer[] writers =pePiece.getWriters();
					List uniqueWriters =new ArrayList();
					int[] writesPerWriter =new int[writers.length];
					for (int i =0; i <writers.length; i++ )
					{
						PEPeer writer =writers[i];
						if (writer !=null)
						{
							int writerId =uniqueWriters.indexOf(writer);
							if (writerId ==-1)
							{
								uniqueWriters.add(writer);
								writerId =uniqueWriters.size() -1;
							}
							writesPerWriter[writerId]++ ;
						}
					}
					int nbWriters =uniqueWriters.size();
					if (nbWriters ==1)
					{
						// Very simple case, only 1 peer contributed for that piece,
						// so, let's mark it as a bad peer
						badPeerDetected((PEPeerTransport) uniqueWriters.get(0));

						// and let's reset the whole piece
						pePiece.reset();
					} else if (nbWriters >1)
					{
						int maxWrites =0;
						PEPeer bestWriter =null;
						for (int i =0; i <uniqueWriters.size(); i++ )
						{
							int writes =writesPerWriter[i];
							PEPeer writer =(PEPeer) uniqueWriters.get(i);
							if (writes >maxWrites &&writer.getReservedPieceNumber() ==-1)
							{
								bestWriter =writer;
								maxWrites =writes;
							}
						}
						if (bestWriter !=null)
						{
							pePiece.setReservedBy(bestWriter);
							bestWriter.setReservedPieceNumber(pePiece.getPieceNumber());
							pePiece.setRequestable();
							for (int i =0; i <pePiece.getNbBlocks(); i++ )
							{
								// If the block was contributed by someone else
								if (writers[i] ==null ||!writers[i].equals(bestWriter))
								{
									pePiece.reDownloadBlock(i);
								}
							}
						} else
						{
							// In all cases, reset the piece
							pePiece.reset();
						}
					} else
					{
						// In all cases, reset the piece
						pePiece.reset();
					}

					// if we are in end-game mode, we need to re-add all the piece chunks
					// to the list of chunks needing to be downloaded
					if (endGameMode)
					{
						piecePicker.addEndGameChunks(_pieces[pieceNumber]);
					}
					_stats.hashFailed(pePiece.getLength());
				}
			} else
			{
				// cancelled, download stopped
			}
		} finally
		{
			if (check_type ==CHECK_REASON_SCAN)
				rescan_piece_time =SystemTime.getCurrentTime();

			if (!seeding_mode)
				checkFinished(false);
		}
	}

	private void badPeerDetected(PEPeerTransport peer)
	{
		String ip =peer.getIp();
		// Debug.out("Bad Peer Detected: " + ip + " [" + peer.getClient() + "]");

		IpFilterManager filter_manager =IpFilterManagerFactory.getSingleton();

		// Ban fist to avoid a fast reco of the bad peer
		int nbWarnings =filter_manager.getBadIps().addWarningForIp(ip);

		// no need to reset the bad chunk count as the peer is going to be disconnected and
		// if it comes back it'll start afresh
		if (nbWarnings >WARNINGS_LIMIT)
		{
			if (COConfigurationManager.getBooleanParameter("Ip Filter Enable Banning"))
			{
				int ps =peer.getPeerState();

				// might have been through here very recently and already started closing
				// the peer (due to multiple bad blocks being found from same peer when checking piece)
				if (!(ps ==PEPeer.CLOSING ||ps ==PEPeer.DISCONNECTED))
				{
					// Close connection
					closeAndRemovePeer(peer, "has sent too many bad pieces, " +WARNINGS_LIMIT +" max.");
				}

				// if a block-ban occurred, check other connections
				if (ip_filter.ban(ip, adapter.getDisplayName()))
				{
					checkForBannedConnections();
				}

				// Trace the ban in third
				if (Logger.isEnabled())
					Logger.log(new LogEvent(peer, LOGID, LogEvent.LT_ERROR, ip +" : has been banned and won't be able "
						+"to connect until you restart azureus"));
			}
		}
	}

	public PEPiece getPiece(int pieceNumber)
	{
		return _pieces[pieceNumber];
	}
	
	public PEPiece[] getPieces()
	{
		return _pieces;
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
		return( disk_mgr.createReadRequest( pieceNumber, offset, length ));
	}
	
	
	
	protected void
	changeState(
		int		new_state )
	{
		peer_manager_state = new_state;
		
		ArrayList peer_manager_listeners = peer_manager_listeners_cow;
		
		for (int i=0;i<peer_manager_listeners.size();i++){
			
			((PEPeerManagerListener)peer_manager_listeners.get(i)).stateChanged( peer_manager_state );
		}
		
	}
	
	public void
	addListener(
		PEPeerManagerListener	l )
	{
		try{
			this_mon.enter();
			
			//copy on write
			ArrayList peer_manager_listeners = new ArrayList( peer_manager_listeners_cow.size() + 1 );      
			peer_manager_listeners.addAll( peer_manager_listeners_cow );
			peer_manager_listeners.add( l );
			peer_manager_listeners_cow = peer_manager_listeners;
			
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
			
			//copy on write
			ArrayList peer_manager_listeners = new ArrayList( peer_manager_listeners_cow );      
			peer_manager_listeners.remove( l );
			peer_manager_listeners_cow = peer_manager_listeners;
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	
	public void 
	parameterChanged(
		String parameterName)
	{   
		disconnect_seeds_when_seeding = COConfigurationManager.getBooleanParameter("Disconnect Seed", true);
		
		if ( parameterName.equals("Ip Filter Enabled")){
			
			checkForBannedConnections();
		}
	}
	
  protected void
  checkForBannedConnections()
  {	 	
  	if ( ip_filter.isEnabled()){  //if ipfiltering is enabled, remove any existing filtered connections    	
  		ArrayList to_close = null;
    	
  		ArrayList	peer_transports = peer_transports_cow;      	
  		for (int i=0; i < peer_transports.size(); i++) {
  			PEPeerTransport conn = (PEPeerTransport)peer_transports.get( i );
  			
  			if ( ip_filter.isInRange( conn.getIp(), adapter.getDisplayName() )) {        	
  				if( to_close == null )  to_close = new ArrayList();
  				to_close.add( conn );
  			}
  		}
  		
  		if( to_close != null ) {		
  			for( int i=0; i < to_close.size(); i++ ) {  			
  				closeAndRemovePeer( (PEPeerTransport)to_close.get(i), "IPFilter banned IP address" );
  			}
  		}
  	}
  }
	
	
	private void checkEndGameMode()
	{
		// We can't come back from end-game mode
		if (endGameMode ||endGameModeAbandoned)
		{
			if (!endGameModeAbandoned)
			{
				if (SystemTime.getCurrentTime() -timeEndGameModeEntered >END_GAME_MODE_TIMEOUT)
				{
					endGameMode =false;
					endGameModeAbandoned =true;

					if (Logger.isEnabled())
						Logger.log(new LogEvent(disk_mgr.getTorrent(), LOGID, "Abandoning end-game mode: "
							+adapter.getDisplayName()));
					piecePicker.clearEndGameChunks();
				}
			}

			return;
		}

		int active_pieces =0;

		for (int i =0; i <_pieces.length; i++ )
		{
			// If the piece is being downloaded (fully requested), let's simply continue
			if (dm_pieces[i].isRequested())
			{
				active_pieces++ ;
				continue;
			}
			// If the piece is downloaded, let's simply continue
			if (!dm_pieces[i].isRequestable())
				continue;

			// Else, the piece is not downloaded / not fully requested, this isn't end game mode
			return;
		}

		// only flick into end-game mode if < trigger size left
		if (active_pieces *disk_mgr.getPieceLength() <=END_GAME_MODE_SIZE_TRIGGER)
		{

			timeEndGameModeEntered =SystemTime.getCurrentTime();
			piecePicker.computeEndGameModeChunks(this);
			endGameMode =true;
			if (Logger.isEnabled())
				Logger.log(new LogEvent(disk_mgr.getTorrent(), LOGID, "Entering end-game mode: " +adapter.getDisplayName()));
			// System.out.println("End-Game Mode activated");
		}
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
    int nbUnchoke = adapter.getMaxUploads();
		if(superSeedModeNumberOfAnnounces >= 2 * nbUnchoke)
			return;
		
		
		//Find an available Peer
		PEPeer selectedPeer = null;
		List sortedPeers = null;

		ArrayList peer_transports	= peer_transports_cow;
		
		sortedPeers = new ArrayList(peer_transports.size());
		Iterator iter	=peer_transports.iterator();
		while(iter.hasNext()) {
			sortedPeers.add(new SuperSeedPeer((PEPeer)iter.next()));
		}      
		
		Collections.sort(sortedPeers);
		iter = sortedPeers.iterator();
		while(iter.hasNext()) {
			PEPeer peer = ((SuperSeedPeer)iter.next()).peer;
			if((peer.getUniqueAnnounce() == -1) && (peer.getPeerState() == PEPeer.TRANSFERING)) {
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
					
					//quit superseed mode
					superSeedMode = false;
					closeAndRemoveAllPeers( "quiting SuperSeed mode", true );
					
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

	public DiskManager getDiskManager() {  return disk_mgr;   }
	
  public PEPeerManagerAdapter	getAdapter(){ return( adapter ); }
	
  public String getDisplayName(){ return( adapter.getDisplayName()); }
  
  public boolean
  isAZMessagingEnabled()
  {
	  return( adapter.isAZMessagingEnabled());
  }
  
  public boolean
  isPeerExchangeEnabled()
  {
	  return( adapter.isPeerExchangeEnabled());
  }

	public LimitedRateGroup getUploadLimitedRateGroup() {  return upload_limited_rate_group;  }
	
	public LimitedRateGroup getDownloadLimitedRateGroup() {  return download_limited_rate_group;  }
	
	
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
			int allowed = getMaxNewConnectionsAllowed();
			
			if( allowed < 0 || allowed > 1000 )  allowed = 1000;  //ensure a very upper limit so it doesnt get out of control when using PEX
			
      if( adapter.isNATHealthy()) {  //if unfirewalled, leave slots avail for remote connections
				int free = PeerUtils.MAX_CONNECTIONS_PER_TORRENT / 20;  //leave 5%
				allowed = allowed - free;
			}
			
			if( allowed > 0 ) {
				//try and connect only as many as necessary
				int wanted = ConnectDisconnectManager.MAX_SIMULTANIOUS_CONNECT_ATTEMPTS - num_waiting_establishments;
				if( wanted > allowed ) {
					num_waiting_establishments += wanted - allowed;
				}
				
        //load stored peer-infos to be established
        while( num_waiting_establishments < ConnectDisconnectManager.MAX_SIMULTANIOUS_CONNECT_ATTEMPTS ) {        	
        	if( peer_database == null || !is_running )  break;        	
       
        	PeerItem item = peer_database.getNextOptimisticConnectPeer();
        	
        	if( item == null || !is_running )  break;

        	PeerItem self = peer_database.getSelfPeer();
        	if( self != null && self.equals( item ) ) {
        		continue;
        	}
        	
        	if( !isAlreadyConnected( item ) ) {
        		String source = PeerItem.convertSourceString( item.getSource() );

        		if( makeNewOutgoingConnection( source, item.getAddressString(), item.getPort() ) ) {
        			num_waiting_establishments++;
        		}
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
				
				//speed tuning check
				transport.doPerformanceTuningCheck();
			}
			
			//update storage capacity
			int allowed = getMaxNewConnectionsAllowed();
			if( allowed == -1 )  allowed = 100;
		}
		
		// every 10 seconds check for connected + banned peers
		if ( mainloop_loop_count % MAINLOOP_TEN_SECOND_INTERVAL == 0 )
		{
			long	last_update = ip_filter.getLastUpdateTime();
			if ( last_update != ip_filter_last_update_time )
			{
				ip_filter_last_update_time	= last_update;
				checkForBannedConnections();
			}
		}
		
		//every 30 seconds
		if ( mainloop_loop_count % MAINLOOP_THIRTY_SECOND_INTERVAL == 0 ) {
			//if we're at our connection limit, time out the least-useful
			//one so we can establish a possibly-better new connection
			if( getMaxNewConnectionsAllowed() == 0 ) {  //we've reached limit        
				ArrayList peer_transports = peer_transports_cow;
				PEPeerTransport max_transport = null;
				long max_time = 0;
				
				for( int i=0; i < peer_transports.size(); i++ ) {
					PEPeerTransport peer = (PEPeerTransport)peer_transports.get( i );
					
					if( peer.getConnectionState() == PEPeerTransport.CONNECTION_FULLY_ESTABLISHED ) {
						long last_time = 0;            
						
						if( seeding_mode ) {
							long time = peer.getTimeSinceLastDataMessageSent();
							
							if( time != -1 ) {  //ensure we've sent them at least one data message to qualify for drop
								last_time = time;
							}
						}
						else {
							long time = peer.getTimeSinceLastDataMessageReceived();
							
							if( time == -1 ) {  //never received
								last_time = peer.getTimeSinceConnectionEstablished();
							}
							else {
								last_time = time;
							}
						}
						
						if( !peer.isIncoming() ) {  //prefer to drop a local connection, to make room for more remotes
							last_time = last_time * 2;
						}
						
						if( last_time > max_time ) {
							max_time = last_time;
							max_transport = peer;
						}
					}
				}
				
				if( max_transport != null && max_time > 60*1000 ) {  //ensure a 1min minimum
					closeAndRemovePeer( max_transport, "timed out by optimistic-connect" );
				}
			}
		}
		
		
		//every 60 seconds
		if ( mainloop_loop_count % MAINLOOP_SIXTY_SECOND_INTERVAL == 0 ) {
			//do peer exchange volleys
			ArrayList peer_transports = peer_transports_cow;
			for( int i=0; i < peer_transports.size(); i++ ) {
				PEPeerTransport peer = (PEPeerTransport)peer_transports.get( i );
				peer.updatePeerExchange();
			}
		}
	}
	
	
	public PeerExchangerItem createPeerExchangeConnection( final PEPeer base_peer ) {
		if( peer_database != null && base_peer.getTCPListenPort() > 0 ) {  //only accept peers whose remote port is known
			PeerItem peer = PeerItemFactory.createPeerItem( base_peer.getIp(), base_peer.getTCPListenPort(), PeerItemFactory.PEER_SOURCE_PEER_EXCHANGE );
			
			return peer_database.registerPeerConnection( peer, new PeerExchangerItem.Helper(){
				public boolean isSeed(){  return base_peer.isSeed();  }
			});
		}
		
		return null;
	}
	
	
	
	private boolean isAlreadyConnected( PeerItem peer_id ) {
		ArrayList peer_transports = peer_transports_cow;
		for( int i=0; i < peer_transports.size(); i++ ) {
			PEPeerTransport peer = (PEPeerTransport)peer_transports.get( i );
			if( peer.getPeerItemIdentity().equals( peer_id ) )  return true;
		}
		return false;
	}
	
	
	public void peerVerifiedAsSelf( PEPeer self ) {
		if( peer_database != null && self.getTCPListenPort() > 0 ) {  //only accept self if remote port is known
			PeerItem peer = PeerItemFactory.createPeerItem( self.getIp(), self.getTCPListenPort(), PeerItem.convertSourceID( self.getPeerSource() ) );
			peer_database.setSelfPeer( peer );
		}
	}
	
	public void IPBanned(BannedIp ip)
	{
		for (int i =0; i <_pieces.length; i++ )
		{
			if (_pieces[i] !=null)
			{
				_pieces[i].reDownloadBlocks(ip.getIp());
				if (!_pieces[i].isWritten())
					dm_pieces[i].clearRequested();
			}
		}
	}
	
	
	public int getAverageCompletionInThousandNotation()
	{
		ArrayList peers =peer_transports_cow;

		if (peers !=null)
		{
			long total =disk_mgr.getTotalLength();

			int my_completion =total ==0 ?1000 :(int) ((1000 *(total -disk_mgr.getRemaining())) /total);

			int sum =my_completion ==1000 ?0 :my_completion; // add in our own percentage if not seeding
			int num =my_completion ==1000 ?0 :1;

			for (int i =0; i <peers.size(); i++ )
			{
				PEPeer peer =(PEPeer) peers.get(i);

				if (peer.getPeerState() ==PEPeer.TRANSFERING &&!peer.isSeed())
				{
					num++ ;
					sum +=peer.getPercentDoneInThousandNotation();
				}
			}

			return num >0 ?sum /num :0;
		}

		return -1;
	}

	public int
	getMaxConnections()
	{
		return( adapter.getMaxConnections());
	}
	
	public int
	getMaxNewConnectionsAllowed()
	{
		int	dl_max = getMaxConnections();

		int	allowed_peers = PeerUtils.numNewConnectionsAllowed(getPeerIdentityDataID(), dl_max );
				
		return( allowed_peers );
	}

	public String 
	getRelationText() 
	{
		return( adapter.getLogRelation().getRelationText());
	}

	public Object[] 
	getQueryableInterfaces() 
	{
		return( adapter.getLogRelation().getQueryableInterfaces());
	}
	
	
	
	public PEPeerTransport getTransportFromIdentity( byte[] peer_id ) {
		ArrayList	peer_transports = peer_transports_cow;      	
		for( int i=0; i < peer_transports.size(); i++ ) {
			PEPeerTransport conn = (PEPeerTransport)peer_transports.get( i );			
			if( Arrays.equals( peer_id, conn.getId() ) )   return conn;
}
		return null;
	}
}
