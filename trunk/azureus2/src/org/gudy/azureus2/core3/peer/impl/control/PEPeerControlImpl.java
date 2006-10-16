/*
 * Created by Olivier Chalouhi
 * Modified Apr 13, 2004 by Alon Rohter
 * Heavily modified Sep 2005 by Joseph Bridgewater
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

package org.gudy.azureus2.core3.peer.impl.control;


import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.*;

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.ipfilter.*;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.peer.impl.*;
import org.gudy.azureus2.core3.peer.util.*;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.tracker.client.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.download.DownloadAnnounceResultPeer;
import org.gudy.azureus2.plugins.peers.PeerDescriptor;

import com.aelitis.azureus.core.networkmanager.LimitedRateGroup;
import com.aelitis.azureus.core.networkmanager.impl.tcp.ConnectDisconnectManager;
import com.aelitis.azureus.core.networkmanager.impl.tcp.TCPNetworkManager;
import com.aelitis.azureus.core.networkmanager.impl.udp.UDPNetworkManager;
import com.aelitis.azureus.core.peermanager.control.*;
import com.aelitis.azureus.core.peermanager.nat.PeerNATInitiator;
import com.aelitis.azureus.core.peermanager.nat.PeerNATTraversalAdapter;
import com.aelitis.azureus.core.peermanager.nat.PeerNATTraverser;
import com.aelitis.azureus.core.peermanager.peerdb.*;
import com.aelitis.azureus.core.peermanager.piecepicker.*;
import com.aelitis.azureus.core.peermanager.unchoker.*;
import com.aelitis.azureus.core.peermanager.uploadslots.UploadHelper;
import com.aelitis.azureus.core.peermanager.uploadslots.UploadSlotManager;

/**
 * manages all peer transports for a torrent
 * 
 * @author MjrTom
 *			2005/Oct/08: Numerous changes for new piece-picking. Also
 *						a few optimizations and multi-thread cleanups
 *			2006/Jan/02: refactoring piece picking related code
 */


public class 
PEPeerControlImpl
	extends LogRelation
	implements 	PEPeerControl, ParameterListener, DiskManagerWriteRequestListener, PeerControlInstance, PeerNATInitiator,
		DiskManagerCheckRequestListener, IPFilterListener
{
	private static final LogIDs LOGID = LogIDs.PEER;
  
	private static final int	WARNINGS_LIMIT = 2;
	
	private static final int	CHECK_REASON_DOWNLOADED		= 1;
	private static final int	CHECK_REASON_COMPLETE		= 2;
	private static final int	CHECK_REASON_SCAN			= 3;

	private static boolean disconnect_seeds_when_seeding = COConfigurationManager.getBooleanParameter("Disconnect Seed", true);
	
	private static IpFilter ip_filter = IpFilterManagerFactory.getSingleton().getIPFilter();
    
    private volatile boolean    is_running = false;

    private volatile ArrayList  peer_transports_cow = new ArrayList();  // Copy on write!
	private final AEMonitor     peer_transports_mon	= new AEMonitor( "PEPeerControl:PT");

    protected final PEPeerManagerAdapter	adapter;
    private final DiskManager           disk_mgr;
    private final DiskManagerPiece[]    dm_pieces;

    private final PiecePicker	piecePicker;
    private long            	lastNeededUndonePieceChange;
    
    /** literally seeding as in 100% torrent complete */
	private boolean 		seeding_mode;
	private boolean			restart_initiated;

    private final int       _nbPieces;     //how many pieces in the torrent
    private PEPieceImpl[]	pePieces;      //pieces that are currently in progress
    private int				nbPiecesActive;	// how many pieces are currently in progress
    
    private int				nbPeersSnubbed;

    private PeerIdentityDataID	_hash;
    private final byte[]        _myPeerId;
    private PEPeerManagerStats        _stats;
	//private final TRTrackerAnnouncer _tracker;
	//  private int _maxUploads;
	private int		_seeds, _peers,_remotes;
    private long    last_remote_time;
	private long	_timeStarted;
	private long	_timeStartedSeeding = -1;
	private long	_timeFinished;
	private Average	_averageReceptionSpeed;

	private long mainloop_loop_count;

    private static final int MAINLOOP_ONE_SECOND_INTERVAL = 1000 / PeerControlScheduler.SCHEDULE_PERIOD_MILLIS;
    private static final int MAINLOOP_FIVE_SECOND_INTERVAL = MAINLOOP_ONE_SECOND_INTERVAL * 5;
    private static final int MAINLOOP_TEN_SECOND_INTERVAL = MAINLOOP_ONE_SECOND_INTERVAL * 10;
    private static final int MAINLOOP_THIRTY_SECOND_INTERVAL = MAINLOOP_ONE_SECOND_INTERVAL * 30;
    private static final int MAINLOOP_SIXTY_SECOND_INTERVAL = MAINLOOP_ONE_SECOND_INTERVAL * 60;
    private static final int MAINLOOP_TEN_MINUTE_INTERVAL = MAINLOOP_SIXTY_SECOND_INTERVAL * 10;
    

    private volatile ArrayList  peer_manager_listeners_cow = new ArrayList();  //copy on write


    private final List		piece_check_result_list     = new ArrayList();
    private final AEMonitor	piece_check_result_list_mon  = new AEMonitor( "PEPeerControl:PCRL");
    
	private boolean 			superSeedMode;
	private int 				superSeedModeCurrentPiece;
	private int 				superSeedModeNumberOfAnnounces;
	private SuperSeedPiece[]	superSeedPieces;
	
    private final AEMonitor     this_mon = new AEMonitor( "PEPeerControl");

	private long		ip_filter_last_update_time;

    private Map                 user_data;

	
    
    
    
  private Unchoker unchoker;
  
	private final UploadHelper upload_helper = new UploadHelper() {		
		public int getPriority() {			
			return UploadHelper.PRIORITY_NORMAL;  //TODO also must call UploadSlotManager.getSingleton().updateHelper( upload_helper ); on priority change
		}
		
		public ArrayList getAllPeers() {
			ArrayList peer_transports = peer_transports_cow;
			return peer_transports;
		}		
	
		public boolean isSeeding() {
			return seeding_mode;
		}		
	};
	
	
	
	private PeerDatabase	peer_database = PeerDatabaseFactory.createPeerDatabase();
	
	private int				next_rescan_piece		= -1;
	private long			rescan_piece_time		= -1;
	
	private long			last_eta;
	private long			last_eta_calculation;
	
	private static final int UDP_FALLBACK_MAX			= 32;
	private static final int MAX_UDP_TRAVERSAL_COUNT	= 3;
	private static final int MAX_UDP_CONNECTIONS		= 10;
	
	private Map	udp_fallbacks = 
		new LinkedHashMap(UDP_FALLBACK_MAX,0.75f,true)
		{
			protected boolean 
			removeEldestEntry(
		   		Map.Entry eldest) 
			{
				return size() > UDP_FALLBACK_MAX;
			}
		};	
		
	private int udp_traversal_count;
		
		
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
		_nbPieces =disk_mgr.getNbPieces();
        dm_pieces =disk_mgr.getPieces();

        pePieces =new PEPieceImpl[_nbPieces];

		piecePicker = PiecePickerFactory.create( this );

		COConfigurationManager.addParameterListener("Ip Filter Enabled", this);
		COConfigurationManager.addParameterListener( "Disconnect Seed", this );

		ip_filter.addListener( this );

	}
 

	public void
	start()
	{
		// This torrent Hash
		try
		{

			_hash =PeerIdentityManager.createDataID(disk_mgr.getTorrent().getHash());

		} catch (TOTorrentException e)
		{

			// this should never happen
			Debug.printStackTrace(e);

			_hash =PeerIdentityManager.createDataID(new byte[20]);
		}

        // the recovered active pieces
        for (int i =0; i <_nbPieces; i++ )
        {
            final DiskManagerPiece dmPiece =dm_pieces[i];
            if (!dmPiece.isDone() &&dmPiece.getNbWritten() >0)
            {
                addPiece(new PEPieceImpl(this, dmPiece, 0), i, true );
            }
        }

		// The peer connections
		peer_transports_cow =new ArrayList();

		// BtManager is threaded, this variable represents the
		// current loop iteration. It's used by some components only called
		// at some specific times.
		mainloop_loop_count =0;

		// The current tracker state
		// this could be start or update

		_averageReceptionSpeed =Average.getInstance(1000, 30);

        // the stats
        _stats =new PEPeerManagerStatsImpl(this);

		superSeedMode =(COConfigurationManager.getBooleanParameter("Use Super Seeding") &&this.getRemaining() ==0);

		superSeedModeCurrentPiece =0;

		if (superSeedMode)
		{
			initialiseSuperSeedMode();
		}

		// initial check on finished state - future checks are driven by piece check results

		// Moved out of mainLoop() so that it runs immediately, possibly changing
		// the state to seeding.

		checkFinished(true);

		UploadSlotManager.getSingleton().registerHelper( upload_helper );
		
		lastNeededUndonePieceChange =Long.MIN_VALUE;
		_timeStarted =SystemTime.getCurrentTime();

		is_running = true;
		
			// activate after marked as running as we may synchronously add connections here due to pending activations
		
		adapter.getPeerManagerRegistration().activate( this );
		
		PeerNATTraverser.getSingleton().register( this );
		
		PeerControlSchedulerFactory.getSingleton().register(this);
	}

	public void stopAll()
	{
		is_running = false;

		UploadSlotManager.getSingleton().deregisterHelper( upload_helper );
		
		PeerControlSchedulerFactory.getSingleton().unregister(this);

		PeerNATTraverser.getSingleton().unregister( this );

			// remove legacy controller activation
		
		adapter.getPeerManagerRegistration().deactivate();
		
		closeAndRemoveAllPeers("download stopped", false);

		// clear pieces
		for (int i =0; i <_nbPieces; i++ )
		{
			if (pePieces[i] !=null)
				removePiece(pePieces[i], i);
		}

		// 5. Remove listeners
		COConfigurationManager.removeParameterListener("Ip Filter Enabled", this);
		COConfigurationManager.removeParameterListener("Disconnect Seed", this);

		ip_filter.removeListener(this);
	}

	public DiskManager getDiskManager() {  return disk_mgr;   }
	public PiecePicker getPiecePicker()
	{
		return piecePicker;
	}
	
  public PEPeerManagerAdapter	getAdapter(){ return( adapter ); }
	
  public String getDisplayName(){ return( adapter.getDisplayName()); }
  
  public void
  schedule()
  {
      try {
			updateTrackerAnnounceInterval();
			doConnectionChecks();
			processPieceChecks();
			
				// note that seeding_mode -> torrent totally downloaded, not just non-dnd files
				// complete, so there is no change of a new piece appearing done by a means such as
				// background periodic file rescans
			
			if ( !seeding_mode ){
				
				checkCompletedPieces();		//check to see if we've completed anything else
			}
			
			updateStats();
			
            checkInterested();      // see if need to recheck Interested on all peers
			
			piecePicker.updateAvailability();
						
			checkCompletionState();	// pick up changes in completion caused by dnd file changes
			
			if ( seeding_mode ){
				
				checkSeeds();

			}else{
					// if we're not finished
				
				checkRequests();
				
				piecePicker.allocateRequests();
               
				checkRescan();
				
				checkSpeedAndReserved();
				
				check99PercentBug();
			}
			
				
			updatePeersInSuperSeedMode();
			
			doUnchokes();
			
      }catch (Throwable e) {
   	
        Debug.printStackTrace( e );
      }

      mainloop_loop_count++;
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
		final TRTrackerAnnouncerResponsePeer[]	peers = tracker_response.getPeers();
		
		if ( peers != null ){
			addPeersFromTracker( tracker_response.getPeers());  
		}
		
		final Map extensions = tracker_response.getExtensions();
		
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
		final Map	protocols = (Map)extensions.get("protocols");
		
		if ( protocols != null ){
			
			System.out.println( "PEPeerControl: tracker response contained protocol extensions");
			
			final Iterator protocol_it = protocols.keySet().iterator();
			
			while( protocol_it.hasNext()){
				
				final String	protocol_name = (String)protocol_it.next();
				
				final Map	protocol = (Map)protocols.get(protocol_name);
				
				final List	transports = PEPeerTransportFactory.createExtendedTransports( this, protocol_name, protocol );
				
				for (int i=0;i<transports.size();i++){
					
					final PEPeer	transport = (PEPeer)transports.get(i);
					
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
	
	public List
	getPeers(
		String	address )
	{		
		List	result = new ArrayList();
	
		Iterator	it = peer_transports_cow.iterator();
			
		while( it.hasNext()){
			
			PEPeerTransport	peer = (PEPeerTransport)it.next();
			
			if ( peer.getIp().equals( address )){
				
				result.add( peer );
			}
		}
		
		return( result );
	}
	
	public PeerDescriptor[]
	getPendingPeers(
		String	address )
	{
		return((PeerDescriptor[])peer_database.getDiscoveredPeers());
	}
	
	public void
	addPeer(
		PEPeer		_transport )
	{
		if ( !( _transport instanceof PEPeerTransport )){
			
			throw( new RuntimeException("invalid class"));
		}
		
		final PEPeerTransport	transport = (PEPeerTransport)_transport;
		
	    if (!ip_filter.isInRange(transport.getIp(), adapter.getDisplayName())) {

			final ArrayList peer_transports = peer_transports_cow;
		
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
		removePeer( _transport, "remove peer" );
	}
	
	public void
	removePeer(
		PEPeer	_transport,
		String	reason )
	{
		if ( !( _transport instanceof PEPeerTransport )){
			
			throw( new RuntimeException("invalid class"));
		}
		
		PEPeerTransport	transport = (PEPeerTransport)_transport;
		
		closeAndRemovePeer( transport, reason, true );
	}

  private void closeAndRemovePeer( PEPeerTransport peer, String reason, boolean log_if_not_found ) {
    boolean removed = false;
    
	// copy-on-write semantics
    try{
      peer_transports_mon.enter();
          
        if ( peer_transports_cow.contains( peer )){

          final ArrayList new_peer_transports = new ArrayList( peer_transports_cow );
          
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
    	if ( log_if_not_found ){
    		// we know this happens due to timing issues... Debug.out( "closeAndRemovePeer(): peer not removed" );
    	}
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
	    final PEPeerTransport peer = (PEPeerTransport)peer_transports.get( i );
	      
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
    	  
        final PEPeerTransport peer = (PEPeerTransport)peer_transports.get( i );
        
        PEPeerTransport	reconnected_peer = peer.reconnect();
 
        if ( reconnected_peer != null ){
        	
			addToPeerTransports( reconnected_peer );
        }
      }
    }
  }
	
	
	
	
	public void 
	addPeer( 
		String 	ip_address, 
		int		tcp_port, 
		int		udp_port,
		boolean use_crypto ) 
	{
		final byte type = use_crypto ? PeerItemFactory.HANDSHAKE_TYPE_CRYPTO : PeerItemFactory.HANDSHAKE_TYPE_PLAIN;
		final PeerItem peer_item = PeerItemFactory.createPeerItem( ip_address, tcp_port, PeerItem.convertSourceID( PEPeerSource.PS_PLUGIN ), type, udp_port );
		
		if( !isAlreadyConnected( peer_item ) ) {
			
			String fail_reason;
			
			if ( TCPNetworkManager.TCP_OUTGOING_ENABLED && tcp_port > 0){

				fail_reason = makeNewOutgoingConnection( PEPeerSource.PS_PLUGIN, ip_address, tcp_port, udp_port, true, use_crypto );  //directly inject the the imported peer
				
			}else if ( UDPNetworkManager.UDP_OUTGOING_ENABLED && udp_port > 0 ){
				
				fail_reason = makeNewOutgoingConnection( PEPeerSource.PS_PLUGIN, ip_address, tcp_port, udp_port, false, use_crypto );  //directly inject the the imported peer

			}else{
			
				fail_reason = "No usable protocol";
			}
		
			if( fail_reason != null )  Debug.out( "injected peer was not added - " + fail_reason );
		}
	}
	
	
	
	private void 
	addPeersFromTracker(
		TRTrackerAnnouncerResponsePeer[]		peers )
	{
		
		for (int i = 0; i < peers.length; i++){
			final TRTrackerAnnouncerResponsePeer	peer = peers[i];
			
			final ArrayList peer_transports = peer_transports_cow;
			
			boolean already_connected = false;
			
      		for( int x=0; x < peer_transports.size(); x++ ) {
				final PEPeerTransport transport = (PEPeerTransport)peer_transports.get( x );
				
				// allow loopback connects for co-located proxy-based connections and testing
				
				if( peer.getAddress().equals( transport.getIp() )){
					
					final boolean same_allowed = COConfigurationManager.getBooleanParameter( "Allow Same IP Peers" ) ||
					transport.getIp().equals( "127.0.0.1" );
					
					if( !same_allowed || peer.getPort() == transport.getPort() ) {
						already_connected = true;
						break;
					}
				}
			}
			
			if( already_connected )  continue;
			
			if( peer_database != null ) {				
				final byte type = peer.getProtocol() == DownloadAnnounceResultPeer.PROTOCOL_CRYPT ? PeerItemFactory.HANDSHAKE_TYPE_CRYPTO : PeerItemFactory.HANDSHAKE_TYPE_PLAIN;
				final PeerItem item = PeerItemFactory.createPeerItem( peer.getAddress(), peer.getPort(), PeerItem.convertSourceID( peer.getSource() ), type, peer.getUDPPort());
				peer_database.addDiscoveredPeer( item );
			}
			
			int	http_port = peer.getHTTPPort();
			
			if ( http_port != 0 ){
				
				adapter.addHTTPSeed( peer.getAddress(), http_port );
			}
		}
	}
	
	
	/**
	 * Request a new outgoing peer connection.
	 * @param address ip of remote peer
	 * @param port remote peer listen port
	 * @return null if the connection was added to the transport list, reason if rejected
	 */
	private String 
	makeNewOutgoingConnection( 
		String		peer_source,
		String 		address, 
		int 		tcp_port,
		int			udp_port,
		boolean		use_tcp,
		boolean 	require_crypto ) 
	{    
		//make sure this connection isn't filtered
   
		if( ip_filter.isInRange( address, adapter.getDisplayName() ) ) {
			return "IPFilter block";
		}
		
		//make sure we need a new connection
		final int needed = getMaxNewConnectionsAllowed();
		
		if( needed == 0 ){
			
			if ( 	peer_source != PEPeerSource.PS_PLUGIN ||
					!doOptimisticDisconnect( AddressUtils.isLANLocalAddress( address ) != AddressUtils.LAN_LOCAL_NO)){
			
				return "Too many connections";
			}
		}
		
		//make sure not already connected to the same IP address; allow loopback connects for co-located proxy-based connections and testing
		final boolean same_allowed = COConfigurationManager.getBooleanParameter( "Allow Same IP Peers" ) || address.equals( "127.0.0.1" );
		if( !same_allowed && PeerIdentityManager.containsIPAddress( _hash, address ) ){  
			return "Already connected to IP";
		}
		
		if( PeerUtils.ignorePeerPort( tcp_port ) ) {
			if (Logger.isEnabled())
				Logger.log(new LogEvent(disk_mgr.getTorrent(), LOGID,
						"Skipping connect with " + address + ":" + tcp_port
								+ " as peer port is in ignore list."));
			return "TCP port in ignore list";
		}
		
		//start the connection
		PEPeerTransport real = PEPeerTransportFactory.createTransport( this, peer_source, address, tcp_port, udp_port, use_tcp, require_crypto );
		
		addToPeerTransports( real );
		return null;
	}
	
	
	/**
	 * A private method that checks if PEPieces being downloaded are finished
	 * If all blocks from a PEPiece are written to disk, this method will
     * queue the piece for hash check.
	 * Elsewhere, if it passes sha-1 check, it will be marked as downloaded,
	 * otherwise, it will unmark it as fully downloaded, so blocks can be retreived again.
	 */
	private void checkCompletedPieces() {
		if ((mainloop_loop_count %MAINLOOP_ONE_SECOND_INTERVAL) !=0)
			return;
		
		//for every piece
		for (int i = 0; i <_nbPieces; i++) {
			final DiskManagerPiece dmPiece =dm_pieces[i];
			//if piece is completly written, not already checking, and not Done
			if (dmPiece.isNeedsCheck())
			{
				//check the piece from the disk
				dmPiece.setChecking();
				
				DiskManagerCheckRequest req = 
					disk_mgr.createCheckRequest(
						i, new Integer(CHECK_REASON_DOWNLOADED));
						
				req.setAdHoc( false );
				
				disk_mgr.enqueueCheckRequest( req, this );
			}
		}
	}

	/** Checks given piece to see if it's active but empty, and if so deactivates it.
	 * @param pieceNumber to check
	 * @return true if the piece was removed and is no longer active (pePiece ==null)
	 */ 
	private boolean checkEmptyPiece(final int pieceNumber)
	{
        if (piecePicker.isInEndGameMode())
            return false;   // be sure to not remove pieces in EGM
		final PEPiece pePiece =pePieces[pieceNumber];
		final DiskManagerPiece dmPiece =dm_pieces[pieceNumber];
		if (pePiece == null || pePiece.isRequested())
			return false;
		if (dmPiece.getNbWritten() >0 ||pePiece.getNbRequests() >0 ||pePiece.getSpeed() >0 ||pePiece.getReservedBy() !=null)
			return false;
		removePiece(pePiece, pieceNumber);
		return true;
	}
	
	/**
	 * Check if a piece's Speed is too fast for it to be getting new data
	 * and if a reserved pieced failed to get data within 120 seconds
	 */
	private void checkSpeedAndReserved()
	{
		if ( (mainloop_loop_count %MAINLOOP_FIVE_SECOND_INTERVAL) != 0 ){
			return;
		}
		
		final int nbPieces =_nbPieces;
		final PEPieceImpl[] pieces =pePieces;
		//for every piece
		for (int i =0; i <nbPieces; i++)
		{
			final PEPieceImpl pePiece =pieces[i];
			// these checks are only against pieces being downloaded yet needing requests still/again
			if (pePiece !=null)
			{
                final long timeSinceActivity =pePiece.getTimeSinceLastActivity();
				if (timeSinceActivity >4 *1000)
				{
                    final int oldSpeed =pePiece.getSpeed();
					// maybe piece's speed is too high for it to get new data
					if (oldSpeed >0)
                    {
                        final DiskManagerPiece dmPiece =dm_pieces[i];
                        if (pePiece.isRequested() ||timeSinceActivity >29 *1000)
                            pePiece.setSpeed(0);
                        else
                        {
                            final long calcSpeed =((dmPiece.getNbWritten() *DiskManager.BLOCK_SIZE) /timeSinceActivity) -1;
                            if (calcSpeed <oldSpeed)
                                pePiece.setSpeed((int)(calcSpeed >0 ?calcSpeed :0));
                        }
                    } else if (timeSinceActivity >(120 *1000))
					{
						// has reserved piece gone stagnant?
						final String reservingPeer =pePiece.getReservedBy();
						if (reservingPeer !=null)
						{
							if (needsMD5CheckOnCompletion(i))
								badPeerDetected(reservingPeer);
							else
							{
								final PEPeerTransport pt =getTransportFromAddress(reservingPeer);
								if (pt !=null)
									closeAndRemovePeer(pt, "Reserved piece data timeout; 120 seconds", true);
							}
                            pePiece.setReservedBy(null);
						}
//						if (!piecePicker.isInEndGameMode())
//							pePiece.checkRequests();
                        checkEmptyPiece(i);
					}
				}
			}
		}
	}

	private void
	check99PercentBug()
	{
			// there's a bug whereby pieces are left downloaded but never written. might have been fixed by
			// changes to the "write result" logic, however as a stop gap I'm adding code to scan for such
			// stuck pieces and reset them
		
		if ( mainloop_loop_count % MAINLOOP_SIXTY_SECOND_INTERVAL == 0 ) {
			
			long	now = SystemTime.getCurrentTime();
			
			for ( int i=0;i<pePieces.length;i++){
				
				PEPiece	pe_piece = pePieces[ i ];

				if ( pe_piece != null ){
									
					DiskManagerPiece	dm_piece = dm_pieces[i];
				
					if ( !dm_piece.isDone()){
					
						if ( pe_piece.isDownloaded()){
						
							if ( now - pe_piece.getLastDownloadTime(now) > 60*1000 ){
							
									// people with *very* slow disk writes can trigger this (I've been talking to a user
									// with a SAN that has .5 second write latencies when checking a file at the same time
									// this means that when dowloading > 32K/sec things start backing up). Eventually the
									// write controller will start blocking the network thread to prevent unlimited
									// queueing but until that time we need to handle this situation slightly better)
								
									// if there are any outstanding writes for this piece then leave it alone
								
								if ( !disk_mgr.hasOutstandingWriteRequestForPiece( i )){
									
									Debug.out( "Fully downloaded piece stalled pending write, resetting p_piece " + i );
															
									pe_piece.reset();
								}
							}
						}
					}
				}
			}
		}
	}
	
    private void checkInterested()
    {
        if ( (mainloop_loop_count %MAINLOOP_ONE_SECOND_INTERVAL) != 0 ){
            return;
        }
        
        if (lastNeededUndonePieceChange >=piecePicker.getNeededUndonePieceChange())
            return;
        
        lastNeededUndonePieceChange =piecePicker.getNeededUndonePieceChange();

		final ArrayList peer_transports = peer_transports_cow;
        int cntPeersSnubbed =0;	// recount # snubbed peers while we're at it
        for (int i =0; i <peer_transports.size(); i++)
        {
            final PEPeerTransport peer =(PEPeerTransport)peer_transports.get(i);
            peer.checkInterested();
            if (peer.isSnubbed())
            	cntPeersSnubbed++;
        }
        setNbPeersSnubbed(cntPeersSnubbed);
    }


	/**
	 * Private method to process the results given by DiskManager's
	 * piece checking thread via asyncPieceChecked(..)
	 */
	private void 
	processPieceChecks() 
	{
		if ( piece_check_result_list.size() > 0 ){
			
			final List pieces;
			
			// process complete piece results
			
			try{
				piece_check_result_list_mon.enter();
				
				pieces = new ArrayList( piece_check_result_list );
				
				piece_check_result_list.clear();
				
			}finally{
				
				piece_check_result_list_mon.exit();
			}
			
			final Iterator it = pieces.iterator();
			
			while (it.hasNext()) {
				
				final Object[]	data = (Object[])it.next();
				
	    		processPieceCheckResult((DiskManagerCheckRequest)data[0],((Integer)data[1]).intValue());
	    			
			}
		}
	}

	private void
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
		
		final long	now = SystemTime.getCurrentTime();
		
		if ( rescan_piece_time > now ){
			
			rescan_piece_time	= now;
		}
		
			// 250K/sec limit
		
		final long	piece_size = disk_mgr.getPieceLength();
		
		final long	millis_per_piece = piece_size / 250;
		
		if ( now - rescan_piece_time < millis_per_piece ){
			
			return;
		}
		
		while( next_rescan_piece != -1 ){
			
			int	this_piece = next_rescan_piece;
			
			next_rescan_piece++;
			
			if ( next_rescan_piece == _nbPieces ){
				
				next_rescan_piece	= -1;
			}
			
			if ( pePieces[this_piece] == null && !dm_pieces[this_piece].isDone()){
				
				DiskManagerCheckRequest	req = 
					disk_mgr.createCheckRequest(
						this_piece, 
						new Integer( CHECK_REASON_SCAN ));	
				
				req.setLowPriority( true );
				
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
	private void 
	checkFinished(
		boolean start_of_day )
	{
		final boolean all_pieces_done =disk_mgr.getRemainingExcludingDND() ==0;

		if (all_pieces_done)
		{
			seeding_mode =true;
			piecePicker.clearEndGameChunks();

			if (!start_of_day)
				adapter.setStateFinishing();

			_timeFinished =SystemTime.getCurrentTime();
			final ArrayList peer_transports = peer_transports_cow;

			// remove previous snubbing
			for (int i =0; i <peer_transports.size(); i++ )
			{
				final PEPeerTransport pc =(PEPeerTransport) peer_transports.get(i);
				pc.setSnubbed(false);
			}
			setNbPeersSnubbed(0);
			
			final boolean checkPieces =COConfigurationManager.getBooleanParameter("Check Pieces on Completion", true);

			// re-check all pieces to make sure they are not corrupt, but only if we weren't already complete
			if (checkPieces &&!start_of_day)
			{
				final DiskManagerCheckRequest req =disk_mgr.createCheckRequest(-1, new Integer(CHECK_REASON_COMPLETE));
				disk_mgr.enqueueCompleteRecheckRequest(req, this);
			}

			disk_mgr.downloadEnded();
			_timeStartedSeeding =SystemTime.getCurrentTime();
			adapter.setStateSeeding(start_of_day);
		}
	}
	
	protected void
	checkCompletionState()
	{
		if ( mainloop_loop_count % MAINLOOP_ONE_SECOND_INTERVAL != 0 ){
	  		
	  		return;
	  	}
		
		boolean dm_done = disk_mgr.getRemainingExcludingDND() == 0;
		
		if ( seeding_mode ){
			
			if ( !dm_done ){
				
				seeding_mode = false;
				
				_timeStartedSeeding = -1;
				_timeFinished		= 0;
				
				Logger.log(
						new LogEvent(	disk_mgr.getTorrent(), LOGID,
										"Turning off seeding mode for PEPeerManager"));
			}
		}else{
			
			if ( dm_done ){
				
				checkFinished( false );
				
				if ( seeding_mode ){
					
					Logger.log(
							new LogEvent(	disk_mgr.getTorrent(), LOGID,
											"Turning on seeding mode for PEPeerManager"));
				}
			}
		}
	}
	
	/**
	 * This method will locate expired requests on peers, will cancel them,
	 * and mark the peer as snubbed if we haven't received usefull data from
	 * them within the last 60 seconds
	 */
	private void checkRequests()
	{
		final long now =SystemTime.getCurrentTime();

		//for every connection
		final ArrayList peer_transports = peer_transports_cow;
		for (int i =peer_transports.size() -1; i >=0 ; i--)
		{
			final PEPeerTransport pc =(PEPeerTransport)peer_transports.get(i);
			if (pc.getPeerState() ==PEPeer.TRANSFERING)
			{
				final List expired =pc.getExpiredRequests();
				if (expired !=null &&expired.size() >0)
				{   // now we know there's a request that's > 60 seconds old
                    final boolean isSeed =pc.isSeed();
                    // snub peers that haven't sent any good data for a minute
                    final long timeSinceGoodData =pc.getTimeSinceGoodDataReceived();
                    if (timeSinceGoodData <0 ||timeSinceGoodData >60 *1000)
                        pc.setSnubbed(true);
                    
                    final long timeSinceData =pc.getTimeSinceLastDataMessageReceived();
                    final long timeSinceOldestRequest =now -((DiskManagerReadRequest) expired.get(0)).getTimeCreated(now);
                    
                    for (int j =0; j <expired.size(); j++)
                    {
                        //for every expired request                              
                        //get the request object
                        final DiskManagerReadRequest request =(DiskManagerReadRequest) expired.get(j);
                        //Only cancel first request if more than 2 mins have passed
                        if (j >0 ||(timeSinceOldestRequest >120 *1000
                            &&(timeSinceData <0 ||timeSinceData >(isSeed ?120 :60) *1000)))
                        {
                            pc.sendCancel(request);             //cancel the request object
                            //get the piece number
                            final int pieceNumber = request.getPieceNumber();
                            PEPiece	pe_piece = pePieces[pieceNumber];
                            //unmark the request on the block
                            if ( pe_piece != null )
                            	pe_piece.clearRequested(request.getOffset() /DiskManager.BLOCK_SIZE);
                             // remove piece if empty so peers can choose something else, except in end game
                            if (!piecePicker.isInEndGameMode())
                                checkEmptyPiece(pieceNumber);
                        }
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
	  
	  final boolean has_remote = adapter.isNATHealthy();
	  if( has_remote ) {
	    //is not firewalled, so can accept incoming connections,
	    //which means no need to continually keep asking the tracker for peers
	    num_wanted = (int)(num_wanted / 1.5);
	  }
	  
	  if ( num_wanted < 0 || num_wanted > WANT_LIMIT ) {
	    num_wanted = WANT_LIMIT;
	  }
	  
	  int current_connection_count = PeerIdentityManager.getIdentityCount( _hash );
	  
	  final TRTrackerScraperResponse tsr = adapter.getTrackerScrapeResponse();
	  
	  if( tsr != null && tsr.isValid() ) {  //we've got valid scrape info
	    final int num_seeds = tsr.getSeeds();   
	    final int num_peers = tsr.getPeers();
	
	    final int swarm_size;
	    
	    if( seeding_mode ) {
	      //Only use peer count when seeding, as other seeds are unconnectable.
	      //Since trackers return peers randomly (some of which will be seeds),
	      //backoff by the seed2peer ratio since we're given only that many peers
	      //on average each announce.
	      final float ratio = (float)num_peers / (num_seeds + num_peers);
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
	  
	  final int current_percent = (current_connection_count * 100) / (current_connection_count + num_wanted);
	  
	  adapter.setTrackerRefreshDelayOverrides( current_percent );  //set dynamic interval override
  }

  	public boolean
  	hasDownloadablePiece()
  	{
  		return( piecePicker.hasDownloadablePiece());
  	}
  	
	public int[] getAvailability() 
	{
		return piecePicker.getAvailability();
	}
	
	//this only gets called when the My Torrents view is displayed
	public float getMinAvailability()
	{
		return piecePicker.getMinAvailability();
	}

	public float getAvgAvail()
	{
		return piecePicker.getAvgAvail();
	}

	public void addPeerTransport( PEPeerTransport transport ) {
    if (!ip_filter.isInRange(transport.getIp(), adapter.getDisplayName())) {
		final ArrayList peer_transports = peer_transports_cow;
			
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
			
			// logic below is either 1 second or 10 secondly, bail out early id neither
		
		if( !UploadSlotManager.AUTO_SLOT_ENABLE ) {		 //manual per-torrent unchoke slot mode
		
			if( mainloop_loop_count % MAINLOOP_ONE_SECOND_INTERVAL != 0 ) { 
				return;
			}

			final int max_to_unchoke = adapter.getMaxUploads();  //how many simultaneous uploads we should consider
			final ArrayList peer_transports = peer_transports_cow;
		
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
			
				final boolean refresh = mainloop_loop_count % MAINLOOP_THIRTY_SECOND_INTERVAL == 0;
			
				unchoker.calculateUnchokes( max_to_unchoke, peer_transports, refresh );
			
				UnchokerUtil.performChokes( unchoker.getChokes(), unchoker.getUnchokes() );
			}
			else if( mainloop_loop_count % MAINLOOP_ONE_SECOND_INTERVAL == 0 ) {  //do quick unchoke check every 1 sec
			
				final ArrayList peers_to_unchoke = unchoker.getImmediateUnchokes( max_to_unchoke, peer_transports );
			
				//ensure that lan-local peers always get unchoked   //TODO
				for( Iterator it=peer_transports.iterator();it.hasNext();) {
					final PEPeerTransport peer = (PEPeerTransport)it.next();				
					if( peer.isLANLocal() ) {
						peers_to_unchoke.add( peer );
					}
				}
						
				UnchokerUtil.performChokes( null, peers_to_unchoke );
			}
		
		}
		
	}
	
	
//	send the have requests out
	private void sendHave(int pieceNumber) {
		//fo
		final ArrayList peer_transports = peer_transports_cow;
		
		for (int i = 0; i < peer_transports.size(); i++) {
			//get a peer connection
			final PEPeerTransport pc = (PEPeerTransport) peer_transports.get(i);
			//send the have message
			pc.sendHave(pieceNumber);
		}
		
	}
	
  //Method that checks if we are connected to another seed, and if so, disconnect from him.
  private void checkSeeds() {
    //proceed on mainloop 1 second intervals if we're a seed and we want to force disconnects
    if ((mainloop_loop_count % MAINLOOP_ONE_SECOND_INTERVAL) != 0)
        return;
    
  	if (!disconnect_seeds_when_seeding ){
        return;
  	}
  	
    ArrayList to_close = null;
    
	final ArrayList peer_transports = peer_transports_cow;
    for (int i = 0; i < peer_transports.size(); i++) {
      final PEPeerTransport pc = (PEPeerTransport) peer_transports.get(i);
      
      if (pc != null && pc.getPeerState() == PEPeer.TRANSFERING && pc.isSeed()) {      	
      	if( to_close == null )  to_close = new ArrayList();
				to_close.add( pc );
      }
    }
		
		if( to_close != null ) {		
			for( int i=0; i < to_close.size(); i++ ) {  			
				closeAndRemovePeer( (PEPeerTransport)to_close.get(i), "disconnect other seed when seeding", false );
			}
		}
  }

  
  
  
	private void updateStats() {   
		
	   if ( (mainloop_loop_count %MAINLOOP_ONE_SECOND_INTERVAL) != 0 ){
		   return;
	   }
	   
		//calculate seeds vs peers
		final ArrayList peer_transports = peer_transports_cow;
		
		int	new_seeds = 0;
		int new_peers = 0;
		int new_remotes = 0;
		
		for (Iterator it=peer_transports.iterator();it.hasNext();){
			final PEPeerTransport pc = (PEPeerTransport) it.next();
			if (pc.getPeerState() == PEPeer.TRANSFERING) {
				if (pc.isSeed())
					new_seeds++;
				else
					new_peers++;
				
				if(((PEPeer)pc).isIncoming()) {
					new_remotes++;
				}
			}
		}
		
		_seeds = new_seeds;
		_peers = new_peers;
		_remotes = new_remotes;
	}

	/**
	 * The way to unmark a request as being downloaded, or also 
	 * called by Peer connections objects when connection is closed or choked
	 * @param request a DiskManagerReadRequest holding details of what was canceled
	 */
	public void requestCanceled(DiskManagerReadRequest request)
	{
        final int pieceNumber =request.getPieceNumber();  //get the piece number
        PEPiece pe_piece = pePieces[pieceNumber];
        if (pe_piece != null )
        	pe_piece.clearRequested(request.getOffset() /DiskManager.BLOCK_SIZE);
	}
	
	
	public PEPeerControl
	getControl()
	{
		return( this );
	}
	
	public HashWrapper
	getTorrentHash()
	{
		try{
			return( disk_mgr.getTorrent().getHashWrapper());
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
			
			return( null );
		}
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
	
	public void dataBytesSent(int length, boolean LAN) {
		if (length > 0) {
			_stats.dataBytesSent(length, LAN );
		}
	}
	
	
	public void protocolBytesSent( int length, boolean LAN ) {
		if (length > 0) {
			_stats.protocolBytesSent(length, LAN);
		}
	}
	
	/** DiskManagerWriteRequestListener message
     * @see org.gudy.azureus2.core3.disk.DiskManagerWriteRequestListener
	 */
	public void writeCompleted(DiskManagerWriteRequest request)
	{
		final int pieceNumber =request.getPieceNumber();
		
		DiskManagerPiece	dm_piece = dm_pieces[pieceNumber];
		
        if (!dm_piece.isDone()){
        
            final PEPiece pePiece =pePieces[pieceNumber];
            
            if ( pePiece != null ){
            	
                pePiece.setWritten((PEPeer)request.getUserData(), request.getOffset() /DiskManager.BLOCK_SIZE );
                
            }else{
            	
            		// this is a way of fixing a 99.9% bug where a dmpiece is left in a 
            		// fully downloaded state with the underlying pe_piece null. Possible explanation is
            		// that a slow peer sends an entire piece at around the time a pe_piece gets reset
            		// due to inactivity.
            	            
            		// we also get here when recovering data that has come in late after the piece has
            		// been abandoned
            	
            	dm_piece.setWritten( request.getOffset() /DiskManager.BLOCK_SIZE );
            }
        }
	}

  public void 
  writeFailed( 
	DiskManagerWriteRequest 	request, 
	Throwable		 			cause )
  {
	  	// if the write has failed then the download will have been stopped so there is no need to try
	  	// and reset the piece
  }
  
    /** This method will queue up a dism manager write request for the block if the block is not already written.
     * It will send out cancels for the block to all peer either if in end-game mode, or per cancel param 
     * @param pieceNumber to potentialy write to
     * @param offset within piece to queue write for
     * @param data to be writen
     * @param sender peer that sent this data
     * @param cancel if cancels definatly need to be sent to all peers for this request
     */
    public void writeBlock(int pieceNumber, int offset, DirectByteBuffer data, PEPeer sender, boolean cancel)
    {
        final int blockNumber =offset /DiskManager.BLOCK_SIZE;
        final DiskManagerPiece dmPiece =dm_pieces[pieceNumber];
        if (dmPiece.isWritten(blockNumber))
        {
            data.returnToPool();
            return;
        }
        
        PEPiece	pe_piece = pePieces[ pieceNumber ];
        
        if ( pe_piece != null ){
        	
        	pe_piece.setDownloaded( offset );
        }
        
        final DiskManagerWriteRequest request =disk_mgr.createWriteRequest(pieceNumber, offset, data, sender);
        disk_mgr.enqueueWriteRequest(request, this);
        // In case we are in endGame mode, remove the block from the chunk list
        if (piecePicker.isInEndGameMode())
            piecePicker.removeFromEndGameModeChunks(pieceNumber, offset);
        if (cancel ||piecePicker.isInEndGameMode())
        {   // cancel any matching outstanding download requests
            // For all connections cancel the request
    		final ArrayList peer_transports = peer_transports_cow;
            for (int i =0; i <peer_transports.size(); i++)
            {
                final PEPeerTransport connection =(PEPeerTransport) peer_transports.get(i);
                final DiskManagerReadRequest dmr =disk_mgr.createReadRequest(pieceNumber, offset, dmPiece.getBlockSize(blockNumber));
                connection.sendCancel(dmr);
            }
        }
    }
	
	
//	/**
//	 * This method is only called when a block is received after the initial request expired,
//	 * but the data has not yet been fulfilled by any other peer, so we use the block data anyway
//	 * instead of throwing it away, and cancel any outstanding requests for that block that might have
//	 * been sent after initial expiry.
//	 */
//	public void writeBlockAndCancelOutstanding(int pieceNumber, int offset, DirectByteBuffer data,PEPeer sender) {
//        final int blockNumber =offset /DiskManager.BLOCK_SIZE;
//        final DiskManagerPiece dmPiece =dm_pieces[pieceNumber];
//        if (dmPiece.isWritten(blockNumber))
//        {
//            data.returnToPool();
//            return;
//        }
//		DiskManagerWriteRequest request =disk_mgr.createWriteRequest(pieceNumber, offset, data, sender);
//		disk_mgr.enqueueWriteRequest(request, this);
//
//		// cancel any matching outstanding download requests
//		List peer_transports =peer_transports_cow;
//		for (int i =0; i <peer_transports.size(); i++)
//		{
//			PEPeerTransport connection =(PEPeerTransport) peer_transports.get(i);
//			DiskManagerReadRequest dmr =disk_mgr.createReadRequest(pieceNumber, offset, dmPiece.getBlockSize(blockNumber));
//			connection.sendCancel(dmr);
//		}
//	}
	
	
	public boolean isWritten(int piece_number, int offset)
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
		return piecePicker.getAvailability(pieceNumber); 
	}
	
	public void havePiece(int pieceNumber, int pieceLength, PEPeer pcOrigin) {
		piecePicker.addHavePiece(pcOrigin, pieceNumber);
		_stats.haveNewPiece(pieceLength);
		
		if(superSeedMode) {
			superSeedPieces[pieceNumber].peerHasPiece(pcOrigin);
			if(pieceNumber == pcOrigin.getUniqueAnnounce()) {
				pcOrigin.setUniqueAnnounce(-1);
				superSeedModeNumberOfAnnounces--;
			}      
		}
		int availability =piecePicker.getAvailability(pieceNumber) -1;
		if (availability < 4) {
			if (dm_pieces[pieceNumber].isDone())
				availability--;
			if (availability <= 0)
				return;
            //for all peers

			final ArrayList peer_transports = peer_transports_cow;

			for (int i = peer_transports.size() - 1; i >= 0; i--) {
				final PEPeerTransport pc = (PEPeerTransport) peer_transports.get(i);
				if (pc !=pcOrigin &&pc.getPeerState() ==PEPeer.TRANSFERING &&pc.isPieceAvailable(pieceNumber))
					((PEPeerStatsImpl)pc.getStats()).statisticalSentPiece(pieceLength / availability);
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
		final long	now = SystemTime.getCurrentTime();
		
		if ( now < last_eta_calculation || now - last_eta_calculation > 900 ){
			
			long dataRemaining = disk_mgr.getRemainingExcludingDND();
	
			if ( dataRemaining > 0 ){
				
				int writtenNotChecked = 0;
				
				for (int i = 0; i < _nbPieces; i++)
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
				final long timeElapsed = (_timeFinished - _timeStarted)/1000;
				//if time was spent downloading....return the time as negative
				if(timeElapsed > 1){
					result = timeElapsed * -1;
				}else{
					result = 0;
				}
			}else{
			
				final long averageSpeed = _averageReceptionSpeed.getAverage();
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
			
				// if it is already disconnected (synchronous failure during connect
				// for example) don't add it
			
			if ( peer.getPeerState() == PEPeer.DISCONNECTED ){
				
				return;
			}
			
			if( peer_transports_cow.contains( peer ) ){
				Debug.out( "Transport added twice" );
				return;  //we do not want to close it
			}
			
			if( is_running ) {
				//copy-on-write semantics
				final ArrayList new_peer_transports = new ArrayList(peer_transports_cow.size() +1);
				
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
	public void peerConnectionClosed( PEPeerTransport peer, boolean connect_failed ) {
		boolean	connection_found = false;
			
		try{
			peer_transports_mon.enter();
		
			int	udp_port = peer.getUDPListenPort();
			
			if ( is_running && connect_failed && peer.isTCP() && UDPNetworkManager.UDP_OUTGOING_ENABLED && udp_port > 0 ){
				
				PeerItem peer_item = peer.getPeerItemIdentity();
				
				PeerItem self_item = peer_database.getSelfPeer();
				
				if ( self_item == null || !self_item.equals( peer_item )){

						// candidate for a fallback UDP connection attempt
								
					String	ip = peer.getIp();
					
					String	key = ip + ":" + udp_port;
					
					udp_fallbacks.put( key, peer_item );
				}
			}
		
	
			if( peer_transports_cow.contains( peer )) {
				
				final ArrayList new_peer_transports = new ArrayList( peer_transports_cow );
				
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
		final ArrayList peer_manager_listeners = peer_manager_listeners_cow;
		
		for( int i=0; i < peer_manager_listeners.size(); i++ ) {
      		((PEPeerManagerListener)peer_manager_listeners.get(i)).peerAdded( this, pc );
		}

	}
	
	
	public void 
	peerRemoved(
		PEPeer pc) 
	{
		final int piece = pc.getUniqueAnnounce();
		if(piece != -1 && superSeedMode ) {
			superSeedModeNumberOfAnnounces--;
			superSeedPieces[piece].peerLeft();
		}
		
		int	reserved_piece = pc.getReservedPieceNumber();
		
		if ( reserved_piece >= 0 ){
			
			PEPiece	pe_piece = pePieces[reserved_piece];
			
			if ( pe_piece != null ){
				
				String	reserved_by = pe_piece.getReservedBy();
				
				if ( reserved_by != null && reserved_by.equals( pc.getIp())){
					
					pe_piece.setReservedBy( null );
				}
			}
		}
		
		adapter.removePeer(pc);  //async downloadmanager notification
    	
		//sync peermanager notification
		final ArrayList peer_manager_listeners = peer_manager_listeners_cow;
		
		for( int i=0; i < peer_manager_listeners.size(); i++ ) {
      		((PEPeerManagerListener)peer_manager_listeners.get(i)).peerRemoved( this, pc );
		}
	}
	
	/** Don't pass a null to this method. All activations of pieces must go through here.
     * @param piece PEPiece invoked; notifications of it's invocation need to be done
     * @param pieceNumber of the PEPiece 
	 */
	public void addPiece(final PEPiece piece, final int pieceNumber)
	{
		addPiece( piece, pieceNumber, false );
	}
	
	protected void addPiece(final PEPiece piece, final int pieceNumber, final boolean force_add )
	{
		pePieces[pieceNumber] =(PEPieceImpl)piece;
		nbPiecesActive++;
		if ( is_running || force_add ){
				// deal with possible piece addition by scheduler loop after closdown started
			adapter.addPiece(piece);
		}
	}
	
    /** Sends messages to listeners that the piece is no longer active.  All closing
     * out (deactivation) of pieces must go through here. The piece will be null upon return.
     * @param pePiece PEPiece to remove
     * @param pieceNumber int
     */
	public void removePiece(PEPiece pePiece, int pieceNumber) {
        adapter.removePiece(pePiece);
        pePieces[pieceNumber] =null;
		nbPiecesActive--;
	}
	
	public int getNbActivePieces()
	{
		return nbPiecesActive;
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
		final int position =buffer.position(DirectByteBuffer.SS_DW);
		md5.update(buffer.getBuffer(DirectByteBuffer.SS_DW));
		buffer.position(DirectByteBuffer.SS_DW, position);
		ByteBuffer md5Result =ByteBuffer.allocate(16);
		md5Result.position(0);
		md5.finalDigest(md5Result);

		final byte[] result =new byte[16];
		md5Result.position(0);
		for (int i =0; i <result.length; i++ )
		{
			result[i] =md5Result.get();
		}

		return result;
	}

	private void MD5CheckPiece(PEPiece piece, boolean correct)
	{
		final String[] writers =piece.getWriters();
		int offset =0;
		for (int i =0; i <writers.length; i++ )
		{
			final int length =piece.getBlockSize(i);
			final String peer =writers[i];
			if (peer !=null)
			{
				DirectByteBuffer buffer =disk_mgr.readBlock(piece.getPieceNumber(), offset, length);

				if (buffer !=null)
				{
					final byte[] hash =computeMd5Hash(buffer);
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
		final PEPieceImpl piece = pePieces[pieceNumber];    
		if (piece == null)
        {
			return false;
        }
		return piece.getPieceWrites().size() > 0;
	}
	
	private void processPieceCheckResult(DiskManagerCheckRequest request, int outcome)
	{
		final int check_type =((Integer) request.getUserData()).intValue();

		try
		{
			final int pieceNumber =request.getPieceNumber();

			// System.out.println( "processPieceCheckResult(" + _finished + "/" + recheck_on_completion + "):" + pieceNumber +
			// "/" + piece + " - " + result );

			// passed = 1, failed = 0, cancelled = 2

			if (check_type ==CHECK_REASON_COMPLETE)
			{ // this is a recheck, so don't send HAVE msgs
				if (outcome ==0)
				{
					// piece failed; restart the download afresh
					Debug.out("Piece #" +pieceNumber +" failed final re-check. Re-downloading...");

					if (!restart_initiated)
					{
						restart_initiated =true;
						adapter.restartDownload();
					}
				}
				return;
			}

            // piece can be null when running a recheck on completion
            final PEPieceImpl pePiece =pePieces[pieceNumber];

			// the piece has been written correctly
			if (outcome ==1)
			{
				if (pePiece !=null)
				{
					if (needsMD5CheckOnCompletion(pieceNumber))
						MD5CheckPiece(pePiece, true);

					final List list =pePiece.getPieceWrites();
					if (list.size() >0)
					{
						// For each Block
						for (int i =0; i <pePiece.getNbBlocks(); i++ )
						{
							// System.out.println("Processing block " + i);
							// Find out the correct hash
							final List listPerBlock =pePiece.getPieceWrites(i);
							byte[] correctHash =null;
							// PEPeer correctSender = null;
							Iterator iterPerBlock =listPerBlock.iterator();
							while (iterPerBlock.hasNext())
							{
								final PEPieceWriteImpl write =(PEPieceWriteImpl) iterPerBlock.next();
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
									final PEPieceWriteImpl write =(PEPieceWriteImpl) iterPerBlock.next();
									if (!Arrays.equals(write.getHash(), correctHash))
									{
										// Bad peer found here
										badPeerDetected(write.getSender());
									}
								}
							}
						}
					}
				}
            	removePiece(pePiece, pieceNumber);

                // send all clients a have message
				sendHave(pieceNumber);  //XXX: if Done isn't set yet, might refuse to send this piece
			} else if (outcome ==0)
			{
                // the piece is corrupt
				if (pePiece !=null)
				{
					MD5CheckPiece(pePiece, false);

					final String[] writers =pePiece.getWriters();
					final List uniqueWriters =new ArrayList();
					final int[] writesPerWriter =new int[writers.length];
					for (int i =0; i <writers.length; i++ )
					{
						final String writer =writers[i];
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
					final int nbWriters =uniqueWriters.size();
					if (nbWriters ==1)
					{
						// Very simple case, only 1 peer contributed for that piece,
						// so, let's mark it as a bad peer
						badPeerDetected((String)uniqueWriters.get(0));

						// and let's reset the whole piece
						pePiece.reset();
					} else if (nbWriters >1)
					{
						int maxWrites =0;
						String bestWriter =null;
						for (int i =0; i <uniqueWriters.size(); i++ )
						{
							final int writes =writesPerWriter[i];
							if (writes >maxWrites)
							{
								final String writer =(String) uniqueWriters.get(i);
								final PEPeerTransport pt =getTransportFromAddress(writer);
								if (pt !=null &&pt.getReservedPieceNumber() ==-1 &&!ip_filter.isInRange(writer, adapter.getDisplayName()))
								{
									bestWriter =writer;
									maxWrites =writes;
								}
							}
						}
						if (bestWriter !=null)
						{
							pePiece.setReservedBy(bestWriter);
							getTransportFromAddress(bestWriter).setReservedPieceNumber(pePiece.getPieceNumber());
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
					piecePicker.addEndGameChunks(pePiece);
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

	private void badPeerDetected(String ip)
	{
		final PEPeerTransport peer =getTransportFromAddress(ip);
		// Debug.out("Bad Peer Detected: " + peerIP + " [" + peer.getClient() + "]");

		final IpFilterManager filter_manager =IpFilterManagerFactory.getSingleton();

		// Ban fist to avoid a fast reco of the bad peer
		final int nbWarnings =filter_manager.getBadIps().addWarningForIp(ip);

		// no need to reset the bad chunk count as the peer is going to be disconnected and
		// if it comes back it'll start afresh
		if (nbWarnings >WARNINGS_LIMIT)
		{
			if (COConfigurationManager.getBooleanParameter("Ip Filter Enable Banning"))
			{
				// if a block-ban occurred, check other connections
				if (ip_filter.ban(ip, adapter.getDisplayName()))
				{
					checkForBannedConnections();
				}

				if (peer !=null)
				{
					final int ps =peer.getPeerState();
	
					// might have been through here very recently and already started closing
					// the peer (due to multiple bad blocks being found from same peer when checking piece)
					if (!(ps ==PEPeer.CLOSING ||ps ==PEPeer.DISCONNECTED))
					{
						// Close connection
						closeAndRemovePeer(peer, "has sent too many bad pieces, " +WARNINGS_LIMIT +" max.", true);
					}
	
					// Trace the ban
					if (Logger.isEnabled())
						Logger.log(new LogEvent(peer, LOGID, LogEvent.LT_ERROR, ip +" : has been banned and won't be able "
							+"to connect until you restart azureus"));
				}
			}
		}
	}
	
    public PEPiece[] getPieces()
    {
        return pePieces;
    }
    
	public PEPiece getPiece(int pieceNumber)
	{
		return pePieces[pieceNumber];
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
	
	

	
	public void
	addListener(
		PEPeerManagerListener	l )
	{
		try{
			this_mon.enter();
			
			//copy on write
			final ArrayList peer_manager_listeners = new ArrayList( peer_manager_listeners_cow.size() + 1 );      
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
			final ArrayList peer_manager_listeners = new ArrayList( peer_manager_listeners_cow );
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
	
  private void
  checkForBannedConnections()
  {	 	
  	if ( ip_filter.isEnabled()){  //if ipfiltering is enabled, remove any existing filtered connections    	
  		ArrayList to_close = null;
    	
		final ArrayList peer_transports = peer_transports_cow;
  		for (int i=0; i < peer_transports.size(); i++) {
  			final PEPeerTransport conn = (PEPeerTransport)peer_transports.get( i );
  			
  			if ( ip_filter.isInRange( conn.getIp(), adapter.getDisplayName() )) {        	
  				if( to_close == null )  to_close = new ArrayList();
  				to_close.add( conn );
  			}
  		}
  		
  		if( to_close != null ) {		
  			for( int i=0; i < to_close.size(); i++ ) {  			
  				closeAndRemovePeer( (PEPeerTransport)to_close.get(i), "IPFilter banned IP address", true );
  			}
  		}
  	}
  }
	
  	public boolean
  	isSeeding()
  	{
  		return( seeding_mode );
  	}
	
	public boolean isSuperSeedMode() {
		return superSeedMode;
	}
	
	public boolean isInEndGameMode() {
		return piecePicker.isInEndGameMode();
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

		final ArrayList peer_transports = peer_transports_cow;
		
		sortedPeers = new ArrayList(peer_transports.size());
		Iterator iter	=peer_transports.iterator();
		while(iter.hasNext()) {
			sortedPeers.add(new SuperSeedPeer((PEPeer)iter.next()));
		}      
		
		Collections.sort(sortedPeers);
		iter = sortedPeers.iterator();
		while(iter.hasNext()) {
			final PEPeer peer = ((SuperSeedPeer)iter.next()).peer;
			if((peer.getUniqueAnnounce() == -1) && (peer.getPeerState() == PEPeer.TRANSFERING)) {
				selectedPeer = peer;
				break;
			}
		}      
		
		if(selectedPeer == null ||selectedPeer.getPeerState() >=PEPeer.CLOSING)
			return;
		
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
		if(selectedPeer.isPieceAvailable(piece.getPieceNumber())) {
			return;
		}
		
		selectedPeer.setUniqueAnnounce(piece.getPieceNumber());
		superSeedModeNumberOfAnnounces++;
		piece.pieceRevealedToPeer();
		((PEPeerTransport)selectedPeer).sendHave(piece.getPieceNumber());		
	}
	
	public void updateSuperSeedPiece(PEPeer peer,int pieceNumber) {
		if (!superSeedMode)
			return;
		superSeedPieces[pieceNumber].peerHasPiece(null);
		if(peer.getUniqueAnnounce() == pieceNumber)
		{
			peer.setUniqueAnnounce(-1);
			superSeedModeNumberOfAnnounces--;        
		}
	}

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
			final ArrayList peer_transports = peer_transports_cow;
			
			int num_waiting_establishments = 0;
			
			int udp_connections = 0;
			
			for( int i=0; i < peer_transports.size(); i++ ) {
				final PEPeerTransport transport = (PEPeerTransport)peer_transports.get( i );
				
				//update waiting count
				final int state = transport.getConnectionState();
				if( state == PEPeerTransport.CONNECTION_PENDING || state == PEPeerTransport.CONNECTION_CONNECTING ) {
					num_waiting_establishments++;
				}
				
				if ( !transport.isTCP()){
					
					udp_connections++;
				}
			}
			
			//pass from storage to connector
			int allowed = getMaxNewConnectionsAllowed();
			
			if( allowed < 0 || allowed > 1000 )  allowed = 1000;  //ensure a very upper limit so it doesnt get out of control when using PEX
			
			if( adapter.isNATHealthy()) {  //if unfirewalled, leave slots avail for remote connections
				final int free = getMaxConnections() / 20;  //leave 5%
				allowed = allowed - free;
			}
			
			if( allowed > 0 ) {
				//try and connect only as many as necessary
				final int wanted = ConnectDisconnectManager.MAX_SIMULTANIOUS_CONNECT_ATTEMPTS - num_waiting_establishments;
				if( wanted > allowed ) {
					num_waiting_establishments += wanted - allowed;
				}
				
				int	remaining = allowed;
				
				//load stored peer-infos to be established
				while( num_waiting_establishments < ConnectDisconnectManager.MAX_SIMULTANIOUS_CONNECT_ATTEMPTS ) {        	
					if( !is_running )  break;        	

					final PeerItem item = peer_database.getNextOptimisticConnectPeer();

					if( item == null || !is_running )  break;

					final PeerItem self = peer_database.getSelfPeer();
					if( self != null && self.equals( item ) ) {
						continue;
					}

					if( !isAlreadyConnected( item ) ) {
						final String source = PeerItem.convertSourceString( item.getSource() );

						final boolean use_crypto = item.getHandshakeType() == PeerItemFactory.HANDSHAKE_TYPE_CRYPTO;

						if ( TCPNetworkManager.TCP_OUTGOING_ENABLED && item.getTCPPort() > 0 ){
							
							if ( makeNewOutgoingConnection( source, item.getAddressString(), item.getTCPPort(), item.getUDPPort(), true, use_crypto ) == null) {
								
								num_waiting_establishments++;
								
								remaining--;
							}
						}else if ( UDPNetworkManager.UDP_OUTGOING_ENABLED && item.getUDPPort() > 0 ){
								
							if ( makeNewOutgoingConnection( source, item.getAddressString(), item.getTCPPort(), item.getUDPPort(), false, use_crypto ) == null) {
									
								num_waiting_establishments++;
									
								remaining--;
							}
						}
					}          
				}
				
				if ( 	UDPNetworkManager.UDP_OUTGOING_ENABLED &&
						remaining > 0 &&
						udp_connections < MAX_UDP_CONNECTIONS &&
						num_waiting_establishments < ConnectDisconnectManager.MAX_SIMULTANIOUS_CONNECT_ATTEMPTS ){
					
					doUDPConnectionChecks( remaining );
				}
			}
		}
    
		//every 5 seconds
		if ( mainloop_loop_count % MAINLOOP_FIVE_SECOND_INTERVAL == 0 ) {
			final ArrayList peer_transports = peer_transports_cow;

			for( int i=0; i < peer_transports.size(); i++ ) {
				final PEPeerTransport transport = (PEPeerTransport)peer_transports.get( i );

				//check for timeouts
				if( transport.doTimeoutChecks() )  continue;

				//keep-alive check
				transport.doKeepAliveCheck();

				//speed tuning check
				transport.doPerformanceTuningCheck();
			}
		}
		
		// every 10 seconds check for connected + banned peers
		if ( mainloop_loop_count % MAINLOOP_TEN_SECOND_INTERVAL == 0 )
		{
			final long	last_update = ip_filter.getLastUpdateTime();
			if ( last_update != ip_filter_last_update_time )
			{
				ip_filter_last_update_time	= last_update;
				checkForBannedConnections();
			}
		}
		
		//every 30 seconds
		if (mainloop_loop_count % MAINLOOP_THIRTY_SECOND_INTERVAL == 0 ) {
			//if we're at our connection limit, time out the least-useful
			//one so we can establish a possibly-better new connection
			if( getMaxNewConnectionsAllowed() == 0 ) {  //we've reached limit        
                doOptimisticDisconnect( false );
			}
		}
		
		
		//every 60 seconds
		if ( mainloop_loop_count % MAINLOOP_SIXTY_SECOND_INTERVAL == 0 ) {
			//do peer exchange volleys
			final ArrayList peer_transports = peer_transports_cow;
			for( int i=0; i < peer_transports.size(); i++ ) {
				final PEPeerTransport peer = (PEPeerTransport)peer_transports.get( i );
				peer.updatePeerExchange();
			}
		}
	}
    
	private void
	doUDPConnectionChecks(
		int		number )
	{
		try{
			peer_transports_mon.enter();
			
			if ( udp_fallbacks.size() == 0 ){
				
				return;
			}
			
			int max = MAX_UDP_TRAVERSAL_COUNT;
			
				// bigger the swarm, less chance of doing it
			
			if ( seeding_mode ){
				
				if ( _peers > 8 ){
				
					max = 0;
					
				}else{
					
					max = 1;
				}
			}else if ( _seeds > 8 ){
				
				max = 0;
				
			}else if ( _seeds > 4 ){
				
				max = 1;
			}
			
			int	avail = max - udp_traversal_count;
			
			int	to_do = Math.min( number, avail );

			Iterator	it = udp_fallbacks.values().iterator();
		
			while( to_do > 0 && it.hasNext()){
			
				to_do--;
				
				final PeerItem	peer_item = (PeerItem)it.next();
			
				it.remove();
			
				PeerNATTraverser.getSingleton().create(
						this,
						new InetSocketAddress( peer_item.getAddressString(), peer_item.getUDPPort() ),
						new PeerNATTraversalAdapter()
						{
							private boolean	done;
							
							public void
							success(
								InetSocketAddress	target )
							{
								complete();
								
								makeNewOutgoingConnection( 
										PeerItem.convertSourceString(peer_item.getSource()),
										target.getAddress().getHostAddress(),
										peer_item.getTCPPort(),
										target.getPort(),
										false,
										true );
							}
							
							public void
							failed()
							{
								complete();
							}
						
							protected void
							complete()
							{
								try{
									peer_transports_mon.enter();

									if ( !done ){
									
										done = true;
										
										udp_traversal_count--;
									}
									
								}finally{
									
									peer_transports_mon.exit();
								}
							}
						});
				
				udp_traversal_count++;
			}
		}finally{
			
			peer_transports_mon.exit();
		}
	}
	
    public boolean doOptimisticDisconnect( boolean	pending_lan_local_peer )
    {
		final ArrayList peer_transports = peer_transports_cow;
	    PEPeerTransport max_transport 			= null;
	    PEPeerTransport max_non_lan_transport 	= null;
	 
	    long max_time 			= 0;
        long max_non_lan_time	= 0;
        
        int	lan_peer_count	= 0;
        
        for( int i=0; i < peer_transports.size(); i++ ) {
            final PEPeerTransport peer = (PEPeerTransport)peer_transports.get( i );
            
            if( peer.getConnectionState() == PEPeerTransport.CONNECTION_FULLY_ESTABLISHED ) {
                final long timeSinceSentData =peer.getTimeSinceLastDataMessageSent();
                
                long peerTestTime = 0;
                if( seeding_mode)
                {
                    if( timeSinceSentData != -1 )
                        peerTestTime = timeSinceSentData;  //ensure we've sent them at least one data message to qualify for drop
                } else
                {
                    final long timeSinceGoodData =peer.getTimeSinceGoodDataReceived();
                    final long timeSinceConnection =peer.getTimeSinceConnectionEstablished();

                    if( timeSinceGoodData == -1 ) 
                        peerTestTime +=timeSinceConnection;   //never received
                    else
                        peerTestTime +=timeSinceGoodData;
                    
                    // try to drop unInteresting in favor of Interesting connections
                    if (!peer.isInteresting())
                    {
                        if (!peer.isInterested())   // if mutually unInterested, really try to drop the connection
                            peerTestTime +=timeSinceConnection +timeSinceSentData;   // if we never sent, it will subtract 1, which is good
                        else
                            peerTestTime +=(timeSinceConnection -timeSinceSentData); // try to give interested peers a chance to get data
                        
                        peerTestTime *=2;
                    }

                    peerTestTime +=peer.getSnubbedTime();
                }
                
                if( !peer.isIncoming() )
                    peerTestTime = peerTestTime * 2;   //prefer to drop a local connection, to make room for more remotes
    
                if ( peer.isLANLocal()){
                	
                	lan_peer_count++;
                	
                }else{

	                if( peerTestTime > max_non_lan_time ) {
	                	max_non_lan_time = peerTestTime;
	                    max_non_lan_transport = peer;
	                }
                }
                
                if( peerTestTime > max_time ) {
                    max_time = peerTestTime;
                    max_transport = peer;
                }
            }
        }
        
        	// don't boot lan peers if we can help it (unless we have a few of them)
        
        if ( max_transport != null ){
        	
        	final int LAN_PEER_MAX	= 4;
        	
        	if ( max_transport.isLANLocal() && lan_peer_count < LAN_PEER_MAX && max_non_lan_transport != null ){
        		
        			// override lan local max with non-lan local max
        		
        		max_transport	= max_non_lan_transport;
        		max_time		= max_non_lan_time;
        	}
        
	        if( max_transport != null && max_time > 5 *60*1000 ) {  //ensure a 5 min minimum test time
	            closeAndRemovePeer( max_transport, "timed out by doOptimisticDisconnect()", true );
	            return true;
	        }
        
	        	// kick worst peers to accomodate lan peer
	        
	        if ( pending_lan_local_peer && lan_peer_count < LAN_PEER_MAX ){
	            closeAndRemovePeer( max_transport, "making space for LAN peer in doOptimisticDisconnect()", true );
	            return true;
	        }
        }
        
        return false;
    }
	
	
	public PeerExchangerItem createPeerExchangeConnection( final PEPeerTransport base_peer ) {
		if( base_peer.getTCPListenPort() > 0 ) {  //only accept peers whose remote port is known
			final PeerItem peer = 
				PeerItemFactory.createPeerItem( base_peer.getIp(),
												base_peer.getTCPListenPort(),
												PeerItemFactory.PEER_SOURCE_PEER_EXCHANGE,
												base_peer.getPeerItemIdentity().getHandshakeType(),
												base_peer.getUDPListenPort());
			
			return peer_database.registerPeerConnection( peer, new PeerExchangerItem.Helper(){
				public boolean 
				isSeed()
				{
					return base_peer.isSeed();  
				}
			});
		}
		
		return null;
	}
	
	
	
	private boolean isAlreadyConnected( PeerItem peer_id ) {
		final ArrayList peer_transports = peer_transports_cow;
		for( int i=0; i < peer_transports.size(); i++ ) {
			final PEPeerTransport peer = (PEPeerTransport)peer_transports.get( i );
			if( peer.getPeerItemIdentity().equals( peer_id ) )  return true;
		}
		return false;
	}
	
	
	public void peerVerifiedAsSelf( PEPeerTransport self ) {
		if( self.getTCPListenPort() > 0 ) {  //only accept self if remote port is known
			final PeerItem peer = PeerItemFactory.createPeerItem( self.getIp(), self.getTCPListenPort(),
				PeerItem.convertSourceID( self.getPeerSource() ), self.getPeerItemIdentity().getHandshakeType(), self.getUDPListenPort());
			peer_database.setSelfPeer( peer );
		}
	}
	
	public void IPBanned(BannedIp ip)
	{
		for (int i =0; i <_nbPieces; i++ )
		{
			if (pePieces[i] !=null)
                pePieces[i].reDownloadBlocks(ip.getIp());
		}
	}
	
	
	public int getAverageCompletionInThousandNotation()
	{
		final ArrayList peer_transports = peer_transports_cow;

		if (peer_transports !=null)
		{
			final long total =disk_mgr.getTotalLength();

			final int my_completion = total == 0
					? 1000
					: (int) ((1000 * (total - disk_mgr.getRemainingExcludingDND())) / total);

			int sum =my_completion ==1000 ?0 :my_completion; // add in our own percentage if not seeding
			int num =my_completion ==1000 ?0 :1;

			for (int i =0; i <peer_transports.size(); i++ )
			{
				final PEPeer peer =(PEPeer) peer_transports.get(i);

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
		final int	dl_max = getMaxConnections();

		final int	allowed_peers = PeerUtils.numNewConnectionsAllowed(getPeerIdentityDataID(), dl_max );
				
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
		final ArrayList peer_transports = peer_transports_cow;
		for( int i=0; i < peer_transports.size(); i++ ) {
			final PEPeerTransport conn = (PEPeerTransport)peer_transports.get( i );			
			if( Arrays.equals( peer_id, conn.getId() ) )   return conn;
}
		return null;
	}
	
	
	/* peer item is not reliable for general use
	public PEPeerTransport getTransportFromPeerItem(PeerItem peerItem)
	{
		ArrayList peer_transports =peer_transports_cow;
		for (int i =0; i <peer_transports.size(); i++)
		{
			PEPeerTransport pt =(PEPeerTransport) peer_transports.get(i);
			if (pt.getPeerItemIdentity().equals(peerItem))
				return pt;
		}
		return null;
	}
	*/

	public PEPeerTransport getTransportFromAddress(String peer)
	{
		final ArrayList peer_transports =peer_transports_cow;
		for (int i =0; i <peer_transports.size(); i++)
		{
			final PEPeerTransport pt =(PEPeerTransport) peer_transports.get(i);
			if (peer.equals(pt.getIp()))
				return pt;
		}
		return null;
	}
	
	// Snubbed peers accounting
	public void incNbPeersSnubbed()
	{
		nbPeersSnubbed++;
	}
	
	public void decNbPeersSnubbed()
	{
		nbPeersSnubbed--;
	}
	
	public void setNbPeersSnubbed(int n)
	{
		nbPeersSnubbed =n;
	}
	
	public int getNbPeersSnubbed()
	{
		return nbPeersSnubbed;
	}
	
	public void
	generateEvidence(
		IndentWriter		writer )
	{
		writer.println( "PeerManager: seeding=" + seeding_mode );
		
		if ( !seeding_mode ){
			
			writer.println( "  Active Pieces" );
			
			int	num_active = 0;
			
			try{
				writer.indent();
				
				String	str	= "";
				int		num	= 0;
				
				for (int i=0;i<pePieces.length;i++){
					
					PEPiece	piece = pePieces[i];
					
					if ( piece != null ){
						
						num_active++;
						
						str += (str.length()==0?"":",") + "#" + i + " " + dm_pieces[i].getString() + ": " + piece.getString();
						
						num++;
						
						if ( num == 20 ){
							
							writer.println( str );
							str = "";
							num	= 0;
						}
					}
				}
				
				if ( num > 0 ){
					writer.println(str);
				}
				
			}finally{
				
				writer.exdent();
			}
			
			if ( num_active == 0 ){
				
				writer.println( "  Inactive Pieces (excluding done/skiped)" );
				
				try{
					writer.indent();
					
					String	str	= "";
					int		num	= 0;
					
					for (int i=0;i<dm_pieces.length;i++){

						DiskManagerPiece	dm_piece = dm_pieces[i];
						
						if ( dm_piece.isInteresting()){
														
							str += (str.length()==0?"":",") + "#" + i + " " + dm_pieces[i].getString();
							
							num++;
							
							if ( num == 20 ){
								
								writer.println( str );
								str = "";
								num	= 0;
							}
						}
					}
					
					if ( num > 0 ){
						
						writer.println(str);
					}
					
				}finally{
					
					writer.exdent();
				}
			}
			
			piecePicker.generateEvidence( writer );
		}
		
		try{
			peer_transports_mon.enter();
			
			writer.indent();
		
			writer.println( "Peers: total = " + peer_transports_cow.size()); 

			try{
				writer.indent();
				
				Iterator it = peer_transports_cow.iterator();
				
				while( it.hasNext()){
				
					PEPeerTransport	peer = (PEPeerTransport)it.next();
					
					peer.generateEvidence( writer );
				}
			}finally{
				
				writer.exdent();
			}
		}finally{
			
			peer_transports_mon.exit();
			
			writer.exdent();
		}
	}
}