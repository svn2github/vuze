/*
 * Created by Joseph Bridgewater
 * Created on Jan 2, 2006
 * Copyright (C) 2005, 2006 Aelitis, All Rights Reserved.
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
 *
 */

package com.aelitis.azureus.core.peermanager.piecepicker.impl;

import java.util.*;

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.disk.impl.DiskManagerFileInfoImpl;
import org.gudy.azureus2.core3.disk.impl.piecemapper.DMPieceList;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.peer.impl.*;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.peermanager.control.PeerControlScheduler;
import com.aelitis.azureus.core.peermanager.piecepicker.*;
import com.aelitis.azureus.core.peermanager.piecepicker.util.BitFlags;
import com.aelitis.azureus.core.peermanager.unchoker.UnchokerUtil;
import com.aelitis.azureus.core.util.CopyOnWriteList;

/**
 * @author MjrTom
 * 
 */

public class PiecePickerImpl
	implements PiecePicker
{
	private static final LogIDs LOGID = LogIDs.PIECES;

    /** min ms for recalculating availability - reducing this has serious ramifications */
    private static final long TIME_MIN_AVAILABILITY	=974;
    /** min ms for recalculating base priorities */
    private static final long TIME_MIN_PRIORITIES	=999;
    /** min ms for forced availability rebuild */
    private static final long TIME_AVAIL_REBUILD	=5*60*1000 -24; 

	// The following are added to the base User setting based priorities (for all inspected pieces)
    /** user select prioritize first/last */
	private static final int PRIORITY_W_FIRSTLAST	=1300;
    /** min # pieces in file for first/last prioritization */
    private static final long FIRST_PIECE_MIN_NB	=4;
    /** user sets file as "High" */
    private static final int PRIORITY_W_FILE		=1000;
    /** Additional boost for more completed High priority */
    private static final int PRIORITY_W_COMPLETION	=2000;
  

	// The following are only used when resuming already running pieces
    /** priority boost due to being too old */
    private static final int PRIORITY_W_AGE		=900;
    /** ms a block is expected to complete in */
    private static final int PRIORITY_DW_AGE		=60 *1000;
    /** ms since last write */
    private static final int PRIORITY_DW_STALE		=120 *1000;
    /** finish pieces already almost done */
    private static final int PRIORITY_W_PIECE_DONE	=900;
    /** keep working on same piece */
    private static final int PRIORITY_W_SAME_PIECE	=700;

    	/** currently webseeds + other explicit priorities are around 10000 or more - at this point we ignore rarity */
    
    private static final int PRIORITY_OVERRIDES_RAREST	= 9000;
    
    	/** priority at and above which pieces require real-time scheduling */
    
    private static final int PRIORITY_REALTIME		= 90000;
    
    /** Min number of requests sent to a peer */
    private static final int REQUESTS_MIN	=2;
    /** Max number of request sent to a peer */
    private static final int REQUESTS_MAX	=256;
	/** Default number of requests sent to a peer, (for each X B/s another request will be used) */
	private static final int SLOPE_REQUESTS	=4 *1024;
	
	private static final long END_GAME_MODE_SIZE_TRIGGER	=20 *1024 *1024;
	private static final long END_GAME_MODE_TIMEOUT			=60 *END_GAME_MODE_SIZE_TRIGGER /16384;
	
	protected static volatile boolean	firstPiecePriority	=COConfigurationManager.getBooleanParameter("Prioritize First Piece", false);
	protected static volatile boolean	completionPriority	=COConfigurationManager.getBooleanParameter("Prioritize Most Completed Files", false);
    /** event # of user settings controlling priority changes */
    protected static volatile long		paramPriorityChange =Long.MIN_VALUE;

    private static final int	NO_REQUEST_BACKOFF_MAX_MILLIS	= 5*1000;
    private static final int	NO_REQUEST_BACKOFF_MAX_LOOPS	= NO_REQUEST_BACKOFF_MAX_MILLIS / PeerControlScheduler.SCHEDULE_PERIOD_MILLIS;
    
    
	private final DiskManager			diskManager;
	private final PEPeerControl			peerControl;
	
	private final DiskManagerListenerImpl	diskManagerListener;
	
	protected final Map					peerListeners;
	private final PEPeerManagerListener	peerManagerListener;
	
	protected final int			nbPieces;
	protected final DiskManagerPiece[]	dmPieces;
	protected final PEPiece[]			pePieces;
	
	protected final AEMonitor availabilityMon = new AEMonitor("PiecePicker:avail");
	private final AEMonitor endGameModeChunks_mon =new AEMonitor("PiecePicker:EGM");

	protected volatile int	nbPiecesDone;
	
    /** asyncronously updated availability */
    protected volatile int[]	availabilityAsynch;
    /** indicates availability needs to be recomputed due to detected drift */
    protected volatile long		availabilityDrift;
    private long				timeAvailRebuild =TIME_AVAIL_REBUILD;
	
    /** periodically updated consistent view of availability for calculating */
    protected volatile int[]	availability;
	
	private long			time_last_avail;
	protected volatile long	availabilityChange;
	private volatile long	availabilityComputeChange;
	private long			time_last_rebuild;
	
	private float		globalAvail;
	private float		globalAvgAvail;
	private int			nbRarestActive;
	private int			globalMin;
	/**
	 * The rarest availability level of pieces that we affirmatively want to try to request from others soonest
	 * ie; our prime targets for requesting rarest pieces
	 */
	private volatile int		globalMinOthers;
	
    /** event # of user file priority settings changes */
    protected volatile long		filePriorityChange;
	
    /** last user parameter settings event # when priority bases were calculated */
    private volatile long		priorityParamChange;
    /** last user priority event # when priority bases were calculated */
    private volatile long		priorityFileChange;
    /** last availability event # when priority bases were calculated */
    private volatile long		priorityAvailChange;
	
    private boolean 			priorityRTAexists;
    
    /** time that base priorities were last computed */
    private long				timeLastPriorities;
	
    /** the priority for starting each piece/base priority for resuming */
    private int[]				startPriorities;
	
	protected volatile boolean	hasNeededUndonePiece;
	protected volatile long		neededUndonePieceChange;
	
	/** A flag to indicate when we're in endgame mode */
	private volatile boolean	endGameMode;
	private volatile boolean	endGameModeAbandoned;
	private volatile long		timeEndGameModeEntered;
	/** The list of chunks needing to be downloaded (the mechanism change when entering end-game mode) */
	private List 				endGameModeChunks;
	
	private long				lastRTARecalcTime;
	private CopyOnWriteList		rta_providers = new CopyOnWriteList();
	private long[]				piece_rtas;
	
	private int					allocate_request_loop_count;
	
	static
	{
		class ParameterListenerImpl
			implements ParameterListener
		{
			public final void parameterChanged(final String parameterName)
			{
				if (parameterName.equals("Prioritize Most Completed Files"))
				{
					completionPriority =COConfigurationManager.getBooleanParameter(parameterName, false);
					paramPriorityChange++;	// this is a user's priority change event
				} else if (parameterName.equals("Prioritize First Piece"))
				{
					firstPiecePriority =COConfigurationManager.getBooleanParameter(parameterName, false);
					paramPriorityChange++;	// this is a user's priority change event
			    }
		    }
		}

		final ParameterListenerImpl	parameterListener =new ParameterListenerImpl();

		COConfigurationManager.addParameterListener("Prioritize Most Completed Files", parameterListener);
		COConfigurationManager.addAndFireParameterListener("Prioritize First Piece", parameterListener);

	}
	
	
	public PiecePickerImpl(final PEPeerControl pc)
	{
		// class administration first

		peerControl	= pc;
		diskManager = peerControl.getDiskManager();
		dmPieces =diskManager.getPieces();

 		nbPieces =diskManager.getNbPieces();
		nbPiecesDone =0;
		
		pePieces = pc.getPieces();
		
		// now do stuff related to availability
		availability =new int[nbPieces];  //always needed
		
		hasNeededUndonePiece =false;
		neededUndonePieceChange =Long.MIN_VALUE;
		
		// ensure all periodic calculaters perform operations at least once
		time_last_avail =Long.MIN_VALUE;
		availabilityChange =Long.MIN_VALUE +1;
		availabilityComputeChange =Long.MIN_VALUE;
		availabilityDrift =nbPieces;
		
		// initialize each piece; on going changes will use event driven tracking
		for (int i =0; i <nbPieces; i++)
		{
			if (dmPieces[i].isDone()){
				availability[i]++;
				nbPiecesDone++;
			}else{
				hasNeededUndonePiece |=dmPieces[i].calcNeeded();
			}
		}
		if (hasNeededUndonePiece)
			neededUndonePieceChange++;
		
		updateAvailability();
		
		// with availability charged and primed, ready for peer messages
		peerListeners =new HashMap();
		peerManagerListener =new PEPeerManagerListenerImpl();
		peerControl.addListener(peerManagerListener);
		
		
		// now do stuff related to starting/continuing pieces
//		startPriorities =new long[nbPieces];    //allocate on demand
		filePriorityChange =Long.MIN_VALUE;
		
		priorityParamChange =Long.MIN_VALUE;
		priorityFileChange =Long.MIN_VALUE;
		priorityAvailChange =Long.MIN_VALUE;
		
		timeLastPriorities =Long.MIN_VALUE;
		
		endGameMode =false;
		endGameModeAbandoned =false;
		timeEndGameModeEntered =0;
		
//		computeBasePriorities();
		
		// with priorities charged and primed, ready for dm messages
		diskManagerListener =new DiskManagerListenerImpl();
		diskManager.addListener(diskManagerListener);
	}
	

    public final void addHavePiece(final PEPeer peer, final int pieceNumber)
	{
    		// peer is null if called from disk-manager callback
		try
		{	availabilityMon.enter();
			if ( availabilityAsynch == null ){
				availabilityAsynch = (int[])availability.clone();
			}
			++availabilityAsynch[pieceNumber];
			availabilityChange++;
		} finally {availabilityMon.exit();}
		
			// if this is an interesting piece then clear any record of "no requests" so the peer gets
			// scheduled next loop
		
		if ( peer != null && dmPieces[pieceNumber].isDownloadable()){
			peer.setConsecutiveNoRequestCount(0);
		}
	}
	
    /**
     * This methd will compute the pieces' overall availability (including ourself)
     * and the _globalMinOthers & _globalAvail
     */
    public final void updateAvailability()
    {
        final long now =SystemTime.getCurrentTime();
        if (now >=time_last_avail &&now <time_last_avail +TIME_MIN_AVAILABILITY)
            return;
        if (availabilityDrift >0 || now < time_last_rebuild ||  (now - time_last_rebuild) > timeAvailRebuild){
            try
            {	availabilityMon.enter();
                
                time_last_rebuild	= now;
                final int[]	new_availability = recomputeAvailability();
                
                if (Constants.isCVSVersion())
                {
                    final int[]   old_availability =availabilityAsynch ==null ?availability :availabilityAsynch;
                    int	    errors	= 0;
                    
                    for (int i=0;i<new_availability.length;i++){
                        if ( new_availability[i] != old_availability[i]){
                            errors++;
                        }
                    }
                    if (errors >0 &&errors !=nbPieces)
                    {
                        if (Logger.isEnabled())
                            Logger.log(new LogEvent(peerControl, LOGID, LogEvent.LT_ERROR,
                                "updateAvailability(): availability rebuild errors =" +errors
                                +" timeAvailRebuild =" +timeAvailRebuild
                            ));
                        timeAvailRebuild -=errors;
                    } else
                    	timeAvailRebuild++;
                }
                
                availabilityAsynch	= new_availability;
                
                availabilityDrift =0;
                availabilityChange++;
            } finally {availabilityMon.exit();}

        } else if (availabilityComputeChange >=availabilityChange){
            return;
        }

        try
        {	availabilityMon.enter();
            time_last_avail =now;
            availabilityComputeChange =availabilityChange;
    
            // take a snapshot of availabilityAsynch
            if ( availabilityAsynch != null ){
                availability 		= availabilityAsynch;
                availabilityAsynch	= null;
            }
        } finally {availabilityMon.exit();}

        int i;
        int allMin =Integer.MAX_VALUE;
        int rarestMin =Integer.MAX_VALUE;
        for (i =0; i <nbPieces; i++)
        {
            final int avail =availability[i];
            final DiskManagerPiece dmPiece =dmPieces[i];
            final PEPiece	pePiece = pePieces[i];
            
            if (avail >0 &&avail <rarestMin && dmPiece.isDownloadable() && (pePiece == null || pePiece.isRequestable())) 
                rarestMin =avail;	// most important targets for near future requests from others

            if (avail <allMin)
                allMin =avail;
        }
        // copy updated local variables into globals
        globalMin =allMin;
        globalMinOthers =rarestMin;

        int total =0;
        int rarestActive =0;
        long totalAvail =0;
        for (i =0; i <nbPieces; i++ )
        {
            final int avail =availability[i];
            final DiskManagerPiece dmPiece =dmPieces[i];
            final PEPiece	pePiece = pePieces[i];
            
            if (avail >0)
            {
                if (avail >allMin)
                    total++;
                if (avail <=rarestMin &&dmPiece.isDownloadable() && pePiece != null && !pePiece.isRequested())
                    rarestActive++;
                totalAvail +=avail;
            }
        }
        // copy updated local variables into globals
        globalAvail =(total /(float) nbPieces) +allMin;
        nbRarestActive =rarestActive;
        globalAvgAvail =totalAvail /(float)(nbPieces)
        /(1 +peerControl.getNbSeeds() +peerControl.getNbPeers());
    }
	
	private final int[] recomputeAvailability()
	{
	    if (availabilityDrift >0 &&availabilityDrift !=nbPieces &&Logger.isEnabled())
	        Logger.log(new LogEvent(diskManager.getTorrent(), LOGID, LogEvent.LT_INFORMATION,
	            "Recomputing availabiliy. Drift=" +availabilityDrift +":" +peerControl.getDisplayName()));
	    final List peers =peerControl.getPeers();
	    
	    final int[]	newAvailability = new int[nbPieces];
	    int j;
	    int i;
	    // first our pieces
	    for (j =0; j <nbPieces; j++)
	        newAvailability[j] =dmPieces[j].isDone() ?1 :0;
	    //for all peers
	    final int peersSize =peers.size();
	    for (i =0; i <peersSize; i++)
	    {	//get the peer connection
	        final PEPeer peer =(PEPeerTransport)peers.get(i);
	        if (peer !=null &&peer.getPeerState() ==PEPeer.TRANSFERING)
	        {
	            //cycle trhough the pieces they actually have
	            final BitFlags peerHavePieces =peer.getAvailable();
	            if (peerHavePieces !=null &&peerHavePieces.nbSet >0)
	            {
	                for (j =peerHavePieces.start; j <=peerHavePieces.end; j++)
	                {
	                    if (peerHavePieces.flags[j])
	                        ++newAvailability[j];
	                }
	            }
	        }
	    }
	    return newAvailability;
	}
	
	
	public final int[] getAvailability()
	{
		return availability;
	}

	public final int getAvailability(final int pieceNumber)
	{
		return availability[pieceNumber];
	}
	
	//this only gets called when the My Torrents view is displayed
	public final float getMinAvailability()
	{
		return globalAvail;
	}

	public final float getAvgAvail()
	{
		return globalAvgAvail;
	}


	/**
	 * Early-outs when finds a downloadable piece
	 * Either way sets hasNeededUndonePiece and neededUndonePieceChange if necessary 
	 */
	protected final void checkDownloadablePiece()
	{
		for (int i =0; i <nbPieces; i++)
		{
			if (dmPieces[i].isInteresting())
			{
				if (!hasNeededUndonePiece)
				{
					hasNeededUndonePiece =true;
					neededUndonePieceChange++;
				}
				return;
			}
		}
		if (hasNeededUndonePiece)
		{
			hasNeededUndonePiece =false;
			neededUndonePieceChange++;
		}
	}

	/**
	 * one reason requests don't stem from the individual peers is so the connections can be
	 * sorted by best uploaders, providing some ooprtunity to download the most important
	 * (ie; rarest and/or highest priority) pieces faster and more reliably
	 */
	public final void allocateRequests()
	{
		if (!hasNeededUndonePiece){
			return;
		}
		
		allocate_request_loop_count++;
		
		final List peers =peerControl.getPeers();
        final int peersSize =peers.size();

		final long[] upRates =new long[peersSize];
        final ArrayList bestUploaders =new ArrayList();

		for (int i =0; i <peersSize; i++){
		
			final PEPeerTransport peer =(PEPeerTransport) peers.get(i);
			
			if ( peer.isDownloadPossible()){
			
				int	no_req_count 	= peer.getConsecutiveNoRequestCount();
	
				if ( 	no_req_count == 0 || 
						allocate_request_loop_count % ( no_req_count + 1 ) == 0 ){
					
					final long upRate = peer.getStats().getSmoothDataReceiveRate();
					
					UnchokerUtil.updateLargestValueFirstSort(upRate, upRates, peer, bestUploaders, 0);
					
				}
			}
		}
		
		final int uploadersSize = bestUploaders.size();

		if ( uploadersSize == 0 ){
			
				// no usable peers, bail out early
			return;
		}
		
		boolean	done_priorities = false;

		if ( priorityRTAexists ){
			
			System.out.println( "Priority rta" );
			
			LinkedList	block_time_order_peers = new LinkedList();
			
			block_time_order_peers.addAll( bestUploaders );
			
			Collections.sort( 
				block_time_order_peers,
				new Comparator()
				{
					public int 
					compare(
						Object arg1, 
						Object arg2) 
					{
						PEPeerTransport pt1	= (PEPeerTransport)arg1;
						PEPeerTransport pt2	= (PEPeerTransport)arg2;
						
						return( getNextBlockETA( pt1 ) - getNextBlockETA( pt2 ));
					};
				});		

				// give priority pieces the first look-in
				// we need to sort by how quickly the peer can get a block, not just its base speed
			
			boolean	allocated_request = true;
			
			while( allocated_request ){
				
				allocated_request = false;
				
				Iterator	it = block_time_order_peers.iterator();
				
				while( it.hasNext()){
	
					final PEPeerTransport pt =(PEPeerTransport)it.next();
	
					if ( !pt.isDownloadPossible()){
						
						it.remove();
						
						continue;
					}
						
					int	peer_request_num = pt.getMaxNbRequests();
						
						// If request queue is too low, enqueue another request
	
					int maxRequests;
		                
					if ( peer_request_num != -1 ){
							
						maxRequests = peer_request_num;
							
					}else{
						if (pt.isSnubbed()){
			                
							it.remove();
							
							continue;
			            }
			                
			            maxRequests = REQUESTS_MIN +(int)( pt.getStats().getDataReceiveRate() /SLOPE_REQUESTS );
			                    
			            if ( maxRequests > REQUESTS_MAX || maxRequests < 0 ){
			                		
			            	maxRequests = REQUESTS_MAX;
			            }
					}
	
					if ( !done_priorities ){
								
						done_priorities	= true;
								
						computeBasePriorities();
					}
	
					if ( findRTAPieceToDownload( pt )){
						
						allocated_request = true;
						
					}else{
						
						it.remove();
					}
				}	
			}
		}
		
		checkEndGameMode();
		
		for (int i =0; i <uploadersSize; i++){

			final PEPeerTransport pt =(PEPeerTransport) bestUploaders.get(i);
			
				// can we transfer something?
			
			if ( pt.isDownloadPossible()){
			
				int	peer_request_num = pt.getMaxNbRequests();
				
					// If request queue is too low, enqueue another request

				int maxRequests;
                
				if ( peer_request_num != -1 ){
					
					maxRequests = peer_request_num;
					
				}else{
	                if (!pt.isSnubbed()){
	                	
	                    if (!endGameMode){
	                        maxRequests =REQUESTS_MIN +(int) (pt.getStats().getDataReceiveRate() /SLOPE_REQUESTS);
	                        if (maxRequests >REQUESTS_MAX ||maxRequests <0)
	                            maxRequests =REQUESTS_MAX;
	                    }else{
	                        maxRequests =2;
	                    }
	                }else{
	                    maxRequests =1;
	                }
				}

					// Only loop when 3/5 of the queue is empty, in order to make more consecutive requests,
					// and improve cache efficiency
				
				if ( pt.getNbRequests() <=(maxRequests *3) /5){
				
					if ( !done_priorities ){
						
						done_priorities	= true;
						
						computeBasePriorities();
					}
					
					int	total_allocated = 0;
					
					try{
						boolean	peer_managing_requests = pt.requestAllocationStarts( startPriorities );
					
						while ( pt.isDownloadPossible() && pt.getNbRequests() < maxRequests ){
						
								// is there anything else to download?
							
							int	allocated;
							
	                        if ( peer_managing_requests || !endGameMode ){
	                        	
	                        	allocated = findPieceToDownload(pt, maxRequests, (int)(upRates[i]/1024));
	                            
	                        }else{
	                        	
	                        	allocated = findPieceInEndGameMode(pt, maxRequests);
	                        }
	                        
 	                        if ( allocated == 0 ){	                		
	                        	
	                        	break;
	                        	
	                        }else{
	                        	
	                        	total_allocated += allocated;
	                        }
						}
					}finally{
						
						pt.requestAllocationComplete();
					}
					
					if ( total_allocated == 0 ){
						
							// there are various reasons that we might not allocate any requests to a peer
							// such as them not having any pieces we're interested in. Keep track of the 
							// number of consecutive "no requests" outcomes so we can reduce the scheduling
							// frequency of such peers
						
						int	no_req_count = pt.getConsecutiveNoRequestCount();
						
						if ( no_req_count < NO_REQUEST_BACKOFF_MAX_LOOPS ){
							
							pt.setConsecutiveNoRequestCount( no_req_count + 2 );
						}
						
                		// System.out.println( pt.getIp() + ": nb=" + pt.getNbRequests() + ",max=" + maxRequests + ",nrc=" + no_req_count +",loop=" + allocate_request_loop_count); 

					}else{
						
						pt.setConsecutiveNoRequestCount( 0 );
					}
				}
			}
		}
	}
	
	
	protected int
	getNextBlockETA(
		PEPeerTransport	pt )
	{
		long upRate = pt.getStats().getSmoothDataReceiveRate();
		
		if ( upRate < 1 ){
			
			upRate = 1;
		}
		
		int	next_block_bytes = ( pt.getNbRequests() + 1 ) * DiskManager.BLOCK_SIZE;
		
		return((int)(( next_block_bytes * 1000 )/ upRate));
	}
	
    /** This computes the base priority for all pieces that need requesting if there's
     * been any availability change or user priority setting changes since the last
     * call, which will be most of the time since availability changes so dynamicaly
     * It will change startPriorities[] (unless there was nothing to do)
     */
    private final void 
    computeBasePriorities()
    {
        final long now = SystemTime.getCurrentTime();
                  
        if ( now < lastRTARecalcTime || now - lastRTARecalcTime > 1000 ){
        	
        	lastRTARecalcTime = now;
        	
        	priorityRTAexists = computePieceRTAs();
        }
        
        if ( !priorityRTAexists ){
	        if (startPriorities !=null &&((now >timeLastPriorities &&now <time_last_avail +TIME_MIN_PRIORITIES)
	            ||(priorityParamChange >=paramPriorityChange &&priorityFileChange >=filePriorityChange
	                &&priorityAvailChange >=availabilityChange)))
	            return;     // *somehow* nothing changed, so nothing to do
        }
        
            // store the latest change indicators before we start making dependent calculations so that a
            // further change while computing stuff doesn't get lost
        
        timeLastPriorities =now;
        priorityParamChange =paramPriorityChange;
        priorityFileChange =filePriorityChange;
        priorityAvailChange =availabilityChange;
        
        boolean     foundPieceToDownload =false;
        final int[]	newPriorities   =new int[nbPieces];

        // locals are a tiny bit faster
        final boolean firstPiecePriorityL =firstPiecePriority;
        final boolean completionPriorityL =completionPriority;
        
        try
        {
            final boolean rarestOverride =isRarestOverride();
            // calculate all base (starting) priorities for all pieces needing requesting
        	final int nbConnects =peerControl.getNbPeers() +peerControl.getNbSeeds();
            for (int i =0; i <nbPieces; i++)
            {
                final DiskManagerPiece dmPiece =dmPieces[i];
                if (dmPiece.isDone())
                    continue;   // nothing to do for pieces not needing requesting
                
                int priority =Integer.MIN_VALUE;
                int startPriority =Integer.MIN_VALUE;
                
                final DMPieceList pieceList =diskManager.getPieceList(dmPiece.getPieceNumber());
                final int pieceListSize =pieceList.size();
                for (int j =0; j <pieceListSize; j++)
                {
                    final DiskManagerFileInfoImpl fileInfo =pieceList.get(j).getFile();
                    final long downloaded =fileInfo.getDownloaded();
                    final long length =fileInfo.getLength();
                    if (length >0 &&downloaded <length &&!fileInfo.isSkipped())
                    {
                        priority =0;
                        // user option "prioritize first and last piece"
                        // TODO: should prioritize ~10% from edges of file
                        if (firstPiecePriorityL &&fileInfo.getNbPieces() >FIRST_PIECE_MIN_NB)
                        {
                            if (i ==fileInfo.getFirstPieceNumber() ||i ==fileInfo.getLastPieceNumber())
                                priority +=PRIORITY_W_FIRSTLAST;
                        }
                        // if the file is high-priority
                        // startPriority +=(1000 *fileInfo.getPriority()) /255;
                        if (fileInfo.isPriority())
                        {
                            priority +=PRIORITY_W_FILE;
                            if (completionPriorityL)
                            {
                                final long percent =(1000 *downloaded) /length;
                                if (percent >=900)
                                    priority +=(PRIORITY_W_COMPLETION *downloaded) /diskManager.getTotalLength();
                            }
                        }
                        if (priority >startPriority)
                            startPriority =priority;
                    }
                }
                
                if (startPriority >=0)
                {
                    dmPiece.setNeeded();
                    foundPieceToDownload =true;
                    final int avail =availability[i];
                    	// nbconnects is async calculate so may be wrong - make sure we don't decrease pri by accident
                    if (avail >0 && nbConnects > avail )
                    {   // boost priority for rarity
                        startPriority +=nbConnects -avail;
//                        startPriority +=(PRIORITY_W_RARE +peerControl.getNbPeers()) /avail;
                        // Boost priority even a little more if it's a globally rarest piece
                        if (!rarestOverride &&avail <=globalMinOthers)
                            startPriority +=nbConnects /avail;
                    }
                    
                    if ( piece_rtas != null ){
                    	
                    	if ( piece_rtas[i] > 0 ){
                    		
                    		startPriority 	= PRIORITY_REALTIME;
                    	}
                    }
                } else
                {
                    dmPiece.clearNeeded();
                }
                
                newPriorities[i] =startPriority;
            }
        } catch (Throwable e)
        {
            Debug.printStackTrace(e);
        }
        
        if (foundPieceToDownload !=hasNeededUndonePiece)
        {
            hasNeededUndonePiece =foundPieceToDownload;
            neededUndonePieceChange++;
        }
        
        startPriorities = newPriorities;
    }
    

	private final boolean isRarestOverride()
    {
        final int nbSeeds =peerControl.getNbSeeds();
        final int nbPeers =peerControl.getNbPeers();
        final int nbMost =(nbPeers >nbSeeds ?nbPeers :nbSeeds);
        final int nbActive =peerControl.getNbActivePieces();
        
        // Dont seek rarest under a few circumstances, so that other factors work better
        // never seek rarest when bootstrapping torrent
        boolean rarestOverride =nbPiecesDone <4 ||endGameMode
        	||(globalMinOthers >1 &&(nbRarestActive >=nbMost ||nbActive >=nbMost));
        if (!rarestOverride &&nbRarestActive >1 &&globalMinOthers >1)
        {
            // if already getting some rarest, dont get more if swarm is healthy or too many pieces running
            rarestOverride =globalMinOthers >globalMin
            	||(globalMinOthers >=(2 *nbSeeds) &&(2 *globalMinOthers) >=nbPeers);
            // Interest in Rarest pieces (compared to user priority settings) could be influenced by several factors;
            // less demand closer to 0% and 100% of torrent completion/farther from 50% of torrent completion
            // less demand closer to 0% and 100% of peers interestd in us/farther from 50% of peers interested in us
            // less demand the more pieces are in progress (compared to swarm size)
            // less demand the farther ahead from absolute global minimum we're at already
            // less demand the healthier a swarm is (rarity compared to # seeds and # peers)
        }
        return rarestOverride;
    }
    
	/**
	 * @param pt the PEPeerTransport we're working on
	 * @return int # of blocks that were requested (0 if no requests were made)
	 */
	protected final int findPieceToDownload(PEPeerTransport pt, final int nbWanted, final int smoothPeerSpeedKBSec)
	{
		final int pieceNumber =getRequestCandidate(pt,smoothPeerSpeedKBSec);
		if (pieceNumber <0)
        {   // probaly should have found something since chose to try; probably not interested anymore
            // (or maybe Needed but not Done pieces are otherwise not requestable)
//            pt.checkInterested();
			return 0;
        }

		final int peerSpeed =(int) pt.getStats().getDataReceiveRate() /1000;

		PEPiece pePiece = pePieces[pieceNumber];
		
		if ( pePiece==null ){

			int[]	peer_priority_offsets = pt.getPriorityOffsets();
			
			int	this_offset = peer_priority_offsets==null?0:peer_priority_offsets[pieceNumber];
			
			   //create piece manually
			
			pePiece =new PEPieceImpl(pt.getManager(), dmPieces[pieceNumber], peerSpeed >>1);

			// Assign the created piece to the pieces array.
			peerControl.addPiece(pePiece, pieceNumber);
            if (startPriorities !=null){
                pePiece.setResumePriority(startPriorities[pieceNumber] + this_offset);
            }else{
            	 pePiece.setResumePriority( this_offset );
            }
			if (availability[pieceNumber] <=globalMinOthers)
				nbRarestActive++;
		}
	
		final int[] blocksFound =pePiece.getAndMarkBlocks(pt, nbWanted  );
		final int blockNumber =blocksFound[0];
		final int nbBlocks =blocksFound[1];

		if (nbBlocks <=0)
			return 0;

		int requested =0;
		// really try to send the request to the peer
		for (int i =0; i <nbBlocks; i++)
		{
			final int thisBlock =blockNumber +i;
			if (pt.request(pieceNumber, thisBlock *DiskManager.BLOCK_SIZE, pePiece.getBlockSize(thisBlock)) != null )
			{
				requested++;
				pt.setLastPiece(pieceNumber);

				pePiece.setLastRequestedPeerSpeed( peerSpeed );
				// have requested a block
			}
		}
		return requested;
	}


	
	protected final boolean 
	findRTAPieceToDownload(
		PEPeerTransport pt )
	{
		if ( pt == null || pt.getPeerState() != PEPeer.TRANSFERING ){
			
			return( false );
		}
		
		final BitFlags  peerHavePieces =pt.getAvailable();
	    
		if ( peerHavePieces ==null || peerHavePieces.nbSet <=0 ){
	    
			return( false );
		}
		
	    final int       peerSpeed =(int) pt.getStats().getDataReceiveRate() /1000;  // how many KB/s has the peer has been sending
         
        final int	startI 	= peerHavePieces.start;
        final int	endI 	= peerHavePieces.end;
        	 
        int	piece_min_rta_index	= -1;
        int piece_min_rta_block	= 0;
        long piece_min_rta_time	= Long.MAX_VALUE;
        
        long	now = SystemTime.getCurrentTime();
        
        for ( int i=startI; i <=endI; i++){
        
            	// is the piece available from this peer?
        	
        	long piece_rta = piece_rtas[i];
        	
            if ( peerHavePieces.flags[i] && startPriorities[i] == PRIORITY_REALTIME && piece_rta > 0 ){
  
                final DiskManagerPiece dmPiece =dmPieces[i];

                if ( !dmPiece.isDownloadable()){
               
                	continue;
                }
                
                final PEPiece pePiece = pePieces[i];
                    
                if ( pePiece != null && pePiece.isDownloaded()){
                    	
                  	continue;
                }

                Object realtime_data = null;
                
                if ( piece_rta >= piece_min_rta_time  ){
                	
                		// piece is less urgent than an already found one
                
                }else if ( pePiece == null || ( realtime_data = pePiece.getRealTimeData()) == null ){
                    	
                   		// no real-time block allocated yet
                	               		
                	piece_min_rta_time 	= piece_rta;
                	piece_min_rta_index = i;
                	
                }else{
                	
                	RealTimeData	rtd = (RealTimeData)realtime_data;
                	
                		// check the blocks to see if any are now lagging behind their ETA given current peer speed
                	
                  	PEPeerTransport[]			allocated_peers = rtd.getBlockPeers();
                  	DiskManagerReadRequest[]	peer_requests	= rtd.getRequests();
                  	                	
                	for (int j=0;j<allocated_peers.length;j++){
                		
                		PEPeerTransport	ap = allocated_peers[j];
                		
                		boolean	interesting_block	= false;
                		
                		if ( ap == null ){
                		
                			interesting_block	= true;
                			
                		}else{
                			
                			DiskManagerReadRequest	request = peer_requests[j];
                			
                			if ( ap.getPeerState() != PEPeer.TRANSFERING || request == null ){
                				
                				allocated_peers[j] = null;
                				
                				interesting_block	= true;
                				
                			}else{
                				
                				int	index = ap.getRequestIndex( request );
                				
                				if ( index == -1 ){
                					
                					allocated_peers[j] 		= null;
                					peer_requests[j]		= null;
                					
                				}else{
                					
	                				long upRate = ap.getStats().getSmoothDataReceiveRate();
	                				
	                				if ( upRate < 1 ){
	                					
	                					upRate = 1;
	                				}
	                				
	                				int	next_block_bytes = ( index + 1 ) * DiskManager.BLOCK_SIZE;
	                				
	                				long	new_eta = now + (( next_block_bytes * 1000 ) / upRate );
	                				
	                					// looks like its lagging...
	                				
	                				if ( new_eta > piece_rta ){
	                					
	                					interesting_block = true;
	                				}
                				}
                			}
                		}
                		
                		if ( interesting_block ){
                			                       		
                        	piece_min_rta_time 	= piece_rta;
                        	piece_min_rta_index = i;
                        	piece_min_rta_block = j;
                		}
                	}
                }
            }
        }
        
        if ( piece_min_rta_index != -1 ){
        	
    		PEPiece pePiece = pePieces[piece_min_rta_index];
    		
    		if ( pePiece == null ){
    						
    			   	// create piece manually
    			
    			pePiece = new PEPieceImpl( pt.getManager(), dmPieces[piece_min_rta_index], peerSpeed >>1 );

    				// Assign the created piece to the pieces array.
    			
    			peerControl.addPiece(pePiece, piece_min_rta_index);
     
    			pePiece.setResumePriority( PRIORITY_REALTIME );
                
    			if ( availability[piece_min_rta_index] <=globalMinOthers ){
    				
    				nbRarestActive++;
    			}
    		}
        	
    		RealTimeData	rtd = (RealTimeData)pePiece.getRealTimeData();
    		
    		if ( rtd == null ){
    			
    			rtd = new RealTimeData( pePiece );
        	
    			pePiece.setRealTimeData( rtd );
    		}
    		
     		pePiece.getAndMarkBlock( pt, piece_min_rta_block );
    		
    		DiskManagerReadRequest	request = pt.request(piece_min_rta_index, piece_min_rta_block *DiskManager.BLOCK_SIZE, pePiece.getBlockSize(piece_min_rta_block));
	
    		if ( request != null ){
    			
       	   		rtd.getBlockPeers()[piece_min_rta_block] 	= pt;
       	   		
       	   		rtd.getRequests()[piece_min_rta_block] 		= request;   	        		

				pt.setLastPiece(piece_min_rta_index);
				
				pePiece.setLastRequestedPeerSpeed( peerSpeed );
			}
    		
    		return( true );
    		
        }else{
        	
        	return( false );
        }
    }
	    
	
	
	
    /**
     * This method is the downloading core. It decides, for a given peer,
     * which block should be requested. Here is the overall algorithm :
     * 0. If there a FORCED_PIECE or reserved piece, that will be started/resumed if possible
     * 1. Scan all the active pieces and find the rarest piece (and highest priority among equally rarest)
     *  that can possibly be continued by this peer, if any
     * 2. While scanning the active pieces, develop a list of equally highest priority pieces
     *  (and equally rarest among those) as candidates for starting a new piece
     * 3. If it can't find any piece, this means all pieces are
     *  already downloaded/full requested
     * 4. Returns int[] pieceNumber, blockNumber if a request to be made is found,
     *  or null if none could be found
     * @param pc PEPeerTransport to work with
     * 
     * @return int with pieceNumberto be requested or -1 if no request could be found
     */
    private final int getRequestCandidate(final PEPeerTransport pt,final int smoothPeerSpeedKBSec)
    {
        if (pt ==null ||pt.getPeerState() !=PEPeer.TRANSFERING)
            return -1;
        final BitFlags  peerHavePieces =pt.getAvailable();
        if (peerHavePieces ==null ||peerHavePieces.nbSet <=0)
            return -1;
        
        	// piece number and its block number that we'll try to DL
        
        int reservedPieceNumber = pt.getReservedPieceNumber();

        	// If there's a piece seserved to this peer resume it and only it (if possible)
        
        if ( reservedPieceNumber >=0 ){
        	           
            PEPiece pePiece = pePieces[reservedPieceNumber];

            if ( pePiece != null ){
            	
            	String peerReserved = pePiece.getReservedBy();
            	
            	if ( peerReserved != null && peerReserved.equals( pt.getIp())){
            		
            		if ( peerHavePieces.flags[reservedPieceNumber] &&pePiece.isRequestable()){
            
            			return reservedPieceNumber;
            		}
            	}
            }
                       
            	// reserved piece is no longer valid, dump it
            
            pt.setReservedPieceNumber(-1);
            
            	// clear the reservation if the piece is still allocated to the peer
            
            if ( pePiece != null ){
            	
            	String peerReserved = pePiece.getReservedBy();
            
            	if ( peerReserved != null ){
            
            		if ( peerReserved.equals(pt.getIp())){
            			
            			pePiece.setReservedBy( null );
            		}
            	}
            }

            	// note, pieces reserved to peers that get disconnected are released in pepeercontrol
            
            reservedPieceNumber	= -1;
        }

        final int       peerSpeed =(int) pt.getStats().getDataReceiveRate() /1000;  // how many KB/s has the peer has been sending
        final int       lastPiece =pt.getLastPiece();
        final boolean   globalRarestOverride =isRarestOverride();
        final int		nbSnubbed =peerControl.getNbPeersSnubbed();

        long        resumeMinAvail =Long.MAX_VALUE;
        int         resumeMaxPriority =Integer.MIN_VALUE;
        boolean     resumeIsRarest =false; // can the peer continuea piece with lowest avail of all pieces we want

        BitFlags    startCandidates =null;
        int         startMaxPriority =Integer.MIN_VALUE;
        int         startMinAvail =Integer.MAX_VALUE;
        boolean     startIsRarest =false;
        boolean		startIsRealtime = false;
        
        int         priority;   // aggregate priority of piece under inspection (start priority or resume priority for pieces to be resumed)
        int         avail =0;   // the swarm-wide availability level of the piece under inspection
        long        pieceAge;   // how long since the PEPiece first started downloading (requesting, actually)
        
        final int	startI =peerHavePieces.start;
        final int	endI =peerHavePieces.end;
        int         i;

        final int[]		peerPriorities	= pt.getPriorityOffsets();
        
        final long  now =SystemTime.getCurrentTime();
        // Try to continue a piece already loaded, according to priority
        for (i =startI; i <=endI; i++)
        {
            // is the piece available from this peer?
            if (peerHavePieces.flags[i])
            {
                priority =startPriorities[i];
                
                if ( peerPriorities != null ){
                	
                	if ( priority >= 0 ){
                		
                		priority += peerPriorities[i];
                	}
                }
                
                final DiskManagerPiece dmPiece =dmPieces[i];

                if ( priority >=0 && dmPiece.isDownloadable()){
               
                    final PEPiece pePiece = pePieces[i];
                    
               			// if we are considering realtime pieces then don't bother with non-realtime ones
                    
                    if (( pePiece == null || pePiece.isRequestable()) && !startIsRealtime){
                    
                   			// if this priority exceeds the priority-override threshold then  we override rarity
                    	
                    	boolean	pieceRarestOverride = priority>=PRIORITY_OVERRIDES_RAREST?true:globalRarestOverride;
                    	    
                        	// piece is: Needed, not fully: Requested, Downloaded, Written, hash-Checking or Done
                     	
                        avail =availability[i];
                        
                        if (avail ==0 )
                        {   // maybe we didn't know we could get it before
                            availability[i] =1;    // but the peer says s/he has it
                            avail =1;
                        }
                        
                        // is the piece active
                        if (pePiece !=null)
                        {
                            // How many requests can still be made on this piece?
                            final int freeReqs =pePiece.getNbUnrequested();
                            if (freeReqs <=0)
                            {
                            	pePiece.setRequested();
                                continue;
                            }
                            
                            // Don't touch pieces reserved for others
                            final String peerReserved =pePiece.getReservedBy();
                            if (peerReserved !=null)
                            {
                                if (!peerReserved.equals(pt.getIp()))
                                    continue;   //reserved to somebody else
                                // the peer forgot this is reserved to him; re-associate it
                                pt.setReservedPieceNumber(i);
                                return i;
                            }
                            
                            final int pieceSpeed =pePiece.getSpeed();
                        	final long timeSinceLastActivity =pePiece.getTimeSinceLastActivity();
                            // Snubbed peers or peers slower than the piece can only request on the piece if;
                            // they're the sole source OR
                            // it's the same as the last piece they were on AND there's enough free blocks
                            // TODO: instead of 3, should count how many peers are snubbed and use that
                            if (avail >1 &&(freeReqs <3 ||pieceSpeed -1 >=freeReqs *peerSpeed))
                            {
                            	// unless the piece has been inactive too long,
                            	//  don't request from snubbed peers UNLESS;
                            	//   it's possible all sources for the piece are snubbed,
                            	//  don't request from slow peers UNLESS;
                            	//   it was the last piece requested from them already
                                if (timeSinceLastActivity < 10 *60*1000 
                                	&&(avail >nbSnubbed &&pt.isSnubbed()) ||(peerSpeed <pieceSpeed &&i !=lastPiece))
                                    continue;
                            }
                            if (avail <=resumeMinAvail)
                            {
                                priority +=pieceSpeed;
                                
                                priority +=(i ==lastPiece) ?PRIORITY_W_SAME_PIECE :0;
                                // Adjust priority for purpose of continuing pieces
                                // how long since last written to (if written to)
                                priority +=timeSinceLastActivity /PRIORITY_DW_STALE;
                                // how long since piece was started
                                pieceAge =now -pePiece.getCreationTime();
                                if (pieceAge >0)
                                    priority +=PRIORITY_W_AGE *pieceAge /(PRIORITY_DW_AGE *dmPiece.getNbBlocks());
                                // how much is already written to disk
                                priority +=(PRIORITY_W_PIECE_DONE *dmPiece.getNbWritten()) /dmPiece.getNbBlocks();
                                
                                pePiece.setResumePriority(priority);
                                
                                if (avail <resumeMinAvail &&(!pieceRarestOverride ||priority >=resumeMaxPriority)
                                    ||(priority >resumeMaxPriority &&(!resumeIsRarest ||pieceRarestOverride)))
                                {   // this piece seems like best choice for resuming
                                    // Verify it's still possible to get a block to request from this piece
                                    if (pePiece.hasUnrequestedBlock())
                                    {   // change the different variables to reflect interest in this block
                                        reservedPieceNumber =i;
                                        resumeMinAvail =avail;
                                        resumeMaxPriority =priority;
                                        resumeIsRarest =avail <=globalMinOthers; // only going to try to resume one
                                    }
                                }
                            }
                        } else if (avail <=globalMinOthers &&!pieceRarestOverride) 
                        {   // rarest pieces only from now on
                            if (!startIsRarest)
                            {   // 1st rarest piece
                                if (startCandidates ==null)
                                    startCandidates =new BitFlags(nbPieces);
                                startMaxPriority =priority;
                                startMinAvail =avail;
                                startIsRarest =avail <=globalMinOthers;
                                startCandidates.setOnly(i); // clear the non-rarest bits in favor of only rarest
                            } else if (priority >startMaxPriority)
                            {   // continuing rarest, higher priority level
                                if (startCandidates ==null)
                                    startCandidates =new BitFlags(nbPieces);
                                startMaxPriority =priority;
                                startCandidates.setOnly(i);
                            } else if (priority ==startMaxPriority)
                            {   // continuing rares, same priority level
                                startCandidates.setEnd(i);
                            }
                        } else if (!startIsRarest ||pieceRarestOverride)
                        {   // not doing rarest pieces
                            if (priority >startMaxPriority)
                            {   // new priority level
                                if (startCandidates ==null)
                                    startCandidates =new BitFlags(nbPieces);
                                startMaxPriority =priority;
                                startMinAvail =avail;
                                startIsRarest =avail <=globalMinOthers;
                                startCandidates.setOnly(i);
                            } else if (priority ==startMaxPriority)
                            {   // continuing same priority level
                                if (avail <startMinAvail)
                                {   // same priority, new availability level
                                    startMinAvail =avail;
                                    startIsRarest =avail <=globalMinOthers;
                                    startCandidates.setOnly(i);
                                } else if (avail ==startMinAvail)
                                {   // same priority level, same availability level
                                    startCandidates.setEnd(i);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // can & should or must resume a piece?
        if (reservedPieceNumber >=0 &&(resumeIsRarest ||!startIsRarest ||globalRarestOverride ||startCandidates ==null ||startCandidates.nbSet <=0))
            return reservedPieceNumber;

// this would allow more non-rarest pieces to be resumed so they get completed so they can be re-shared,
// which can make us intersting to more peers, and generally improve the speed of the swarm,
// however, it can sometimes be hard to get the rarest pieces, such as when a holder unchokes very infrequently
// 20060312[MjrTom] this can lead to TOO many active pieces, so do the extra check with arbitrary # of active pieces
        final boolean resumeIsBetter;
        if (reservedPieceNumber >=0 &&globalMinOthers >0 &&peerControl.getNbActivePieces() >32)	// check at arbitrary figure of 32 pieces 
        {
            resumeIsBetter =(resumeMaxPriority /resumeMinAvail) >(startMaxPriority /globalMinOthers);
            
            if (Constants.isCVSVersion() &&Logger.isEnabled())
                Logger.log(new LogEvent(new Object[] {pt, peerControl}, LOGID, 
                    "Start/resume choice; piece #:" +reservedPieceNumber +" resumeIsBetter:" +resumeIsBetter
                    +" globalMinOthers=" +globalMinOthers
                    +" startMaxPriority=" +startMaxPriority +" startMinAvail=" +startMinAvail
                    +" resumeMaxPriority=" +resumeMaxPriority +" resumeMinAvail=" +resumeMinAvail
                    +" : " +pt));
            
            if (resumeIsBetter)
                return reservedPieceNumber;
        }
        
        // start a new piece; select piece from start candidates bitfield
        return getPieceToStart(startCandidates);
    }
    
    
    /** 
     * @param startCandidates BitFlags of potential candidates to choose from
     * @return int the piece number that was chosen to be started. Note it's possible for
     * the chosen piece to have been started already (by another thread).
     * This method considers that potential to not be relevant.
     */
	protected final int getPieceToStart(final BitFlags startCandidates)
	{
		if (startCandidates ==null ||startCandidates.nbSet <=0)
			return -1;
        if (startCandidates.nbSet ==1)
            return startCandidates.start;
		
		final int direction =RandomUtils.generateRandomPlusMinus1();
        final int startI;
        if (direction ==1)
            startI =startCandidates.start;
        else
            startI =startCandidates.end;
		
		// randomly select a bit flag to be the one
        final int targetNb =RandomUtils.generateRandomIntUpto(startCandidates.nbSet);
        
        // figure out the piece number of that selected bitflag
        int foundNb =-1;
		for (int i =startI; i <=startCandidates.end &&i >=startCandidates.start; i +=direction)
		{
			// is piece flagged
			if (startCandidates.flags[i])
            {
                foundNb++;
                if (foundNb >=targetNb)
                    return i;
            }
		}
		return -1;
	}

	public final boolean hasDownloadablePiece()
	{
		return hasNeededUndonePiece;
	}

	public final long getNeededUndonePieceChange()
	{
		return neededUndonePieceChange;
	}

    
    private final void checkEndGameMode()
    {
        if (peerControl.getNbSeeds() +peerControl.getNbPeers() <3)
            return;
        final long now =SystemTime.getCurrentTime();
        // We can't come back from end-game mode
        if (endGameMode ||endGameModeAbandoned)
        {
            if (!endGameModeAbandoned)
            {
                if (now -timeEndGameModeEntered >END_GAME_MODE_TIMEOUT)
                {
                    endGameModeAbandoned =true;
                    clearEndGameChunks();
                    
                    if (Logger.isEnabled())
                        Logger.log(new LogEvent(diskManager.getTorrent(), LOGID, "Abandoning end-game mode: "
                            +peerControl.getDisplayName()));
                }
            }
            return;
        }

        int active_pieces =0;

        for (int i =0; i <nbPieces; i++)
        {
            final DiskManagerPiece dmPiece =dmPieces[i];
            // If the piece isn't even Needed, or doesn't need more downloading, simply continue
            if (!dmPiece.isDownloadable())
                continue;
            
            final PEPiece pePiece = pePieces[i];
            
            if ( pePiece != null && pePiece.isDownloaded()){
            	continue;
            }
            
            // If the piece is being downloaded (fully requested), count it and continue
            if ( pePiece != null && pePiece.isRequested() && dmPiece.isNeeded())
            {
                active_pieces++;
                continue;
            }

            // Else, some piece is Needed, not downloaded/fully requested; this isn't end game mode
            return;
        }

        // only flick into end-game mode if < trigger size left
        if (active_pieces *diskManager.getPieceLength() <=END_GAME_MODE_SIZE_TRIGGER)
        {
            timeEndGameModeEntered =now;
            endGameMode =true;
            computeEndGameModeChunks();
            if (Logger.isEnabled())
                Logger.log(new LogEvent(diskManager.getTorrent(), LOGID, "Entering end-game mode: "
                    +peerControl.getDisplayName()));
            // System.out.println("End-Game Mode activated");
        }
    }
    
    private final void computeEndGameModeChunks()
    {
        endGameModeChunks =new ArrayList();
        try
        {
            endGameModeChunks_mon.enter();

            for (int i =0; i <nbPieces; i++ )
            {
            	final DiskManagerPiece dmPiece =dmPieces[i];
                // Pieces not Needed or not needing more downloading are of no interest
                if (!dmPiece.isInteresting())
                    continue;
                
                final PEPiece pePiece =pePieces[i];
                if (pePiece ==null)
                    continue;

                final boolean written[] =dmPiece.getWritten();
                if (written ==null)
                {
                    if (!dmPiece.isDone())
                    {
                        for (int j =0; j <pePiece.getNbBlocks(); j++ )
                        {
                            endGameModeChunks.add(new EndGameModeChunk(pePiece, j));
                        }
                    }
                } else
                {
                    for (int j =0; j <written.length; j++ )
                    {
                        if (!written[j])
                            endGameModeChunks.add(new EndGameModeChunk(pePiece, j));
                    }
                }
            }
        } finally
        {
            endGameModeChunks_mon.exit();
        }
    }

    public final boolean isInEndGameMode()
	{
		return endGameMode;
	}
	
    /** adds every block from the piece to the list of chuncks to be selected for egm requesting
     * 
     */ 
	public final void addEndGameChunks(final PEPiece pePiece)
	{
		if (!endGameMode)
			return;
		try
		{
			endGameModeChunks_mon.enter();
			final int nbChunks =pePiece.getNbBlocks();
			for (int i =0; i <nbChunks; i++ )
			{
				endGameModeChunks.add(new EndGameModeChunk(pePiece, i));
			}
		} finally
		{
			endGameModeChunks_mon.exit();
		}
	}

    /** adds blocks from the piece that are neither downloaded nor written to the list
     * of  chuncks to be selected for egm requesting
     */ 
	public final void addEndGameBlocks(final PEPiece pePiece)
	{
		if (!endGameMode ||pePiece ==null)
			return;
		final DiskManagerPiece dmPiece =pePiece.getDMPiece();
		final int nbChunks =pePiece.getNbBlocks();
		try
		{
			endGameModeChunks_mon.enter();
			for (int i =0; i <nbChunks; i++ )
			{
				if (!pePiece.isDownloaded(i) &&!dmPiece.isWritten(i))
                    endGameModeChunks.add(new EndGameModeChunk(pePiece, i));
			}
		} finally
		{
			endGameModeChunks_mon.exit();
		}
	}

    protected final int findPieceInEndGameMode(final PEPeerTransport pt, final int wants)
    {
        if (pt ==null ||wants <=0 ||pt.getPeerState() !=PEPeer.TRANSFERING)
            return 0;
        // Ok, we try one, if it doesn't work, we'll try another next time
        try
        {
            endGameModeChunks_mon.enter();
            
            final int nbChunks =endGameModeChunks.size();
            if (nbChunks >0)
            {
                final int random =RandomUtils.generateRandomIntUpto(nbChunks);
                final EndGameModeChunk chunk =(EndGameModeChunk) endGameModeChunks.get(random);
                final int pieceNumber =chunk.getPieceNumber();
                if (dmPieces[pieceNumber].isWritten(chunk.getBlockNumber()))
                {
                    endGameModeChunks.remove(chunk);
                    return 0;
                }
                final PEPiece	pePiece = pePieces[pieceNumber];
                if (pt.isPieceAvailable(pieceNumber)
                    &&pePiece != null 
                    &&(!pt.isSnubbed() ||availability[pieceNumber] <=peerControl.getNbPeersSnubbed())
                    &&pt.request(pieceNumber, chunk.getOffset(), chunk.getLength()) != null )
                {
                    pePiece.setRequested(pt, chunk.getBlockNumber());
                    pt.setLastPiece(pieceNumber);
                    return 1;
                }
            }
        } finally
        {
            endGameModeChunks_mon.exit();
        }
        return 0;
    }
    
	public final void removeFromEndGameModeChunks(final int pieceNumber, final int offset)
	{
		if (!endGameMode)
			return;
		try
		{
			endGameModeChunks_mon.enter();

			final Iterator iter =endGameModeChunks.iterator();
			while (iter.hasNext())
			{
				EndGameModeChunk chunk =(EndGameModeChunk) iter.next();
				if (chunk.equals(pieceNumber, offset))
					iter.remove();
			}
		} finally
		{
			endGameModeChunks_mon.exit();
		}
	}
	
	public final void clearEndGameChunks()
	{
		if (!endGameMode)
			return;
		try
		{
			endGameModeChunks_mon.enter();
			endGameModeChunks.clear();
			endGameMode =false;
		} finally
		{
			endGameModeChunks_mon.exit();
		}
	}
	
	private boolean
	computePieceRTAs()
	{
		List	list = rta_providers.getList();
		
		if ( rta_providers.size() == 0 ){
			
			if ( piece_rtas != null ){
			
					// coming out of real-time mode - clear down 
				
				for (int i=0;i<pePieces.length;i++){
					
					PEPiece	piece = pePieces[i];
					
					piece.setRealTimeData(null);
				}
				
				piece_rtas = null;
			}
			
			return( false );
			
		}else{

			boolean	has_rta = false;
			
				// prolly more efficient to reallocate than reset to 0
			
			piece_rtas = new long[nbPieces];
			
			for (int i=0;i<list.size();i++){
				
				PieceRTAProvider	shaper = (PieceRTAProvider)list.get(i);
				
				final long[]	offsets = shaper.updateRTAs( this );
				
				for (int j=0;j<offsets.length;j++){
					
					long rta = offsets[j];
					
					if ( rta > 0 ){
						
						piece_rtas[j] = Math.min( piece_rtas[j], rta );
						
						has_rta	= true;
					}
				}
			}
		
			return( has_rta );
		}
	}
	
	public void
	addRTAProvider(
		PieceRTAProvider		provider )
	{
		rta_providers.add( provider );
	}
	
	public void
	removeRTAProvider(
		PieceRTAProvider		provider )
	{
		rta_providers.remove( provider );
	}
	
	/**
	 * An instance of this listener is registered with peerControl
	 * Through this, we learn of peers joining and leaving
	 * and attach/detach listeners to them
	 */
	private class PEPeerManagerListenerImpl
		implements PEPeerManagerListener
	{
		public final void peerAdded(final PEPeerManager manager, PEPeer peer )
		{
			PEPeerListenerImpl peerListener;
			peerListener =(PEPeerListenerImpl)peerListeners.get(peer);
			if (peerListener ==null)
			{
				peerListener =new PEPeerListenerImpl();
				peerListeners.put(peer, peerListener);
			}
			peer.addListener(peerListener);
		}
		
		public final void peerRemoved(final PEPeerManager manager, PEPeer peer)
		{
			// remove this listener from list of listeners and from the peer
			final PEPeerListenerImpl peerListener =(PEPeerListenerImpl)peerListeners.remove(peer);
			peer.removeListener(peerListener);
		}
	}
	
	/**
	 * An instance of this listener is registered with each peer
	 */
	private class PEPeerListenerImpl
		implements PEPeerListener
	{
		public final void stateChanged(PEPeer peer, final int newState)
		{
            /*
			switch (newState)
			{
				case PEPeer.CONNECTING:
					return;
				
				case PEPeer.HANDSHAKING:
					return;
				
				case PEPeer.TRANSFERING:
					return;
				
				case PEPeer.CLOSING:
					return;
				
				case PEPeer.DISCONNECTED:
					return;
			}
            */
		}
		
		public final void sentBadChunk(final PEPeer peer, final int piece_num, final int total_bad_chunks )
		{
			/* nothing to do here */
		}
		
		public final void addAvailability(final PEPeer peer, final BitFlags peerHavePieces)
		{
			if (peerHavePieces ==null ||peerHavePieces.nbSet <=0)
				return;
			try
			{	availabilityMon.enter();
				if ( availabilityAsynch == null ){
					availabilityAsynch = (int[])availability.clone();
				}
				for (int i =peerHavePieces.start; i <=peerHavePieces.end; i++)
				{
					if ( peerHavePieces.flags[i] ){
						++availabilityAsynch[i];
					}
				}
				availabilityChange++;
			} finally {availabilityMon.exit();}
		}

        /**
         * Takes away the given pieces from global availability
         * @param PEPeer peer this is about
         * @param peerHasPieces BitFlags of the pieces
         */
		public final void removeAvailability(final PEPeer peer, final BitFlags peerHavePieces)
		{
			if (peerHavePieces ==null ||peerHavePieces.nbSet <=0)
				return;
			try
			{	availabilityMon.enter();
				if (availabilityAsynch ==null)
                {
                    availabilityAsynch =(int[]) availability.clone();
                }
                for (int i =peerHavePieces.start; i <=peerHavePieces.end; i++)
                {
                    if (peerHavePieces.flags[i])
                    {
                        if (availabilityAsynch[i] >(dmPieces[i].isDone() ?1 :0))
                            --availabilityAsynch[i];
                        else
                            availabilityDrift++;
                    }
                }
                availabilityChange++;
			} finally {availabilityMon.exit();}
		}
	}
	
	/**
	 * An instance of this listener is registered with peerControl
	 * @author MjrTom
	 */
	private class DiskManagerListenerImpl
		implements DiskManagerListener
	{
		public final void stateChanged(int oldState, int newState)
		{
			//starting torrent
		}

		public final void filePriorityChanged(DiskManagerFileInfo file)
		{
			// record that user-based priorities changed
			filePriorityChange++;	// this is a user's priority change event
			
			// only need to re-calc Needed on file's pieces; priority is calculated seperatly
			boolean foundPieceToDownload =false;
			// if didn't have anything to do before, now only need to check if we need
			// to DL from this file, but if had something to do before,
			// must rescan all pieces to see if now nothing to do
			final int startI;
			final int endI;
			if (hasNeededUndonePiece)
			{
				startI =0;
				endI =nbPieces;
			} else
			{
				startI =file.getFirstPieceNumber();
				endI =file.getLastPieceNumber() +1;
			}
			for (int i =startI; i <endI; i++)
			{
				final DiskManagerPiece dmPiece =dmPieces[i];
				if (!dmPiece.isDone())
					foundPieceToDownload |=dmPiece.calcNeeded();
			}
			if (foundPieceToDownload ^hasNeededUndonePiece)
			{
				hasNeededUndonePiece =foundPieceToDownload;
				neededUndonePieceChange++;
			}
		}
		
		
		public final void pieceDoneChanged(DiskManagerPiece dmPiece)
		{
			final int pieceNumber =dmPiece.getPieceNumber();
			if (dmPiece.isDone())
			{
				addHavePiece(null,pieceNumber);
				nbPiecesDone++;
                if (nbPiecesDone >=nbPieces)
                    checkDownloadablePiece();
			}else
			{
                try
                {   availabilityMon.enter();
                    if ( availabilityAsynch == null ){
                        availabilityAsynch = (int[])availability.clone();
                    }
                    if (availabilityAsynch[pieceNumber] >0)
                        --availabilityAsynch[pieceNumber];
                    else
                        availabilityDrift++;
                    availabilityChange++;
                } finally {availabilityMon.exit();}
				nbPiecesDone--;
				if (dmPiece.calcNeeded() &&!hasNeededUndonePiece)
				{
					hasNeededUndonePiece =true;
					neededUndonePieceChange++;
				}
			}
		}

		public final void fileAccessModeChanged(DiskManagerFileInfo file, int old_mode, int new_mode)
		{
			//file done (write to read)
			//starting to upload from the file (read to write)
		}
	}
	
	public String
	getPieceString(
		int	piece_number )
	{
		return( "pri=" + (startPriorities==null?0:startPriorities[piece_number]));
	}
	
	public void
	generateEvidence(
		IndentWriter	writer )
	{
		writer.println( "Piece Picker" );
		
		try{
			writer.indent();
			
			writer.println( "globalAvail: " + globalAvail );
			writer.println( "globalAvgAvail: " + globalAvgAvail );
			writer.println( "nbRarestActive: " + nbRarestActive );
			writer.println( "globalMin: " + globalMin );
			writer.println( "globalMinOthers: " + globalMinOthers );
			writer.println( "hasNeededUndonePiece: " + hasNeededUndonePiece );
			writer.println( "endGameMode: " + endGameMode );
			writer.println( "endGameModeAbandoned: " + endGameModeAbandoned );
			writer.println( "endGameModeChunks: " + endGameModeChunks );			
			
		}finally{
			
			writer.exdent();
		}
	}
	
	protected class
	RealTimeData
	{
      	private PEPeerTransport[]			allocated_peers;
      	private DiskManagerReadRequest[]	peer_requests;
      	
      	protected
      	RealTimeData(
      		PEPiece		piece )
      	{
      		int	nb = piece.getNbBlocks();
      		
      		allocated_peers 	= new PEPeerTransport[nb];
      		peer_requests		= new DiskManagerReadRequest[nb];
      	}
      	
      	public final PEPeerTransport[]
      	getBlockPeers()
      	{
      		return( allocated_peers );
      	}
      	
      	public final DiskManagerReadRequest[]
      	getRequests()
      	{
      		return( peer_requests );
      	}
	}
}
