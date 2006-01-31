/*
 * Created by Joseph Bridgewater
 * Created on Jan 2, 2006
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
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
 * AELITIS, SARL au capital de 30,000 euros
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

import com.aelitis.azureus.core.peermanager.piecepicker.*;
import com.aelitis.azureus.core.peermanager.piecepicker.priority.PiecePriorityShaper;
import com.aelitis.azureus.core.peermanager.piecepicker.util.BitFlags;
import com.aelitis.azureus.core.peermanager.unchoker.UnchokerUtil;

/**
 * @author MjrTom
 * 
 */

public class PiecePickerImpl
	implements PiecePicker
{
	private static final LogIDs LOGID = LogIDs.PEER;

	private static final long TIME_MIN_AVAILABILITY	=949;	// min ms for recalculating availability
	private static final long TIME_MIN_PRIORITIES	=974;	// min ms for recalculating base priorities

	// The following are added to the base User setting based priorities (all inspected pieces)
	private static final long PRIORITY_W_FIRSTLAST	=1300;			// user select prioritize first/last
	private static final long FIRST_PIECE_MIN_NB	=4;				// min # pieces in file for first/last prioritization
	private static final long PRIORITY_W_FILE		=1100;			// user sets file as "High"
	private static final long PRIORITY_W_COMPLETION	=800;			// Additional boost for more completed High priority

	private static final long PRIORITY_W_RAREST		=1300;			// Additional boost for globally rarest piece
	private static final long PRIORITY_W_RARE		=1250;			// boost for rarity

	// The following are only used when resuming already running pieces
	private static final long PRIORITY_W_AGE		=1000;			// priority boost due to being too old
	private static final long PRIORITY_DW_AGE		=20 *1000 *3;	// ms a block is expected to complete in, with leeway factor
	private static final long PRIORITY_DW_STALE		=120 *1000;		// ms since last write
	private static final long PRIORITY_W_PIECE_DONE	=600;			// finish pieces already almost done
	private static final long PRIORITY_W_SAME_PIECE	=300;			// keep working on same piece

	private static final int REQUESTS_MIN	=2;			//Min number of requests sent to a peer
	private static final int REQUESTS_MAX	=256;		//Max number of request sent to a peer
	//Default number of requests sent to a peer, (for each X B/s another request will be used)
	private static final int SLOPE_REQUESTS	=4 *1024;
	
	private static final long END_GAME_MODE_SIZE_TRIGGER	=0;	//20 *1024 *1024;
	private static final long END_GAME_MODE_TIMEOUT			=1;	//10 *60 *1000;
	
	protected static volatile boolean	firstPiecePriority	=COConfigurationManager.getBooleanParameter("Prioritize First Piece", false);
	protected static volatile boolean	completionPriority	=COConfigurationManager.getBooleanParameter("Prioritize Most Completed Files", false);

	protected DiskManager	diskManager;
	protected PEPeerControl	peerControl;
	protected PiecePriorityShaper	priorityShaper;

	private DiskManagerListenerImpl	diskManagerListener;

	protected int			nbPieces =-1;
	protected volatile int	nbPiecesDone;

	protected final DiskManagerPiece[]	dm_pieces;
	protected volatile PEPiece[]		pePieces;

	protected Map					peerListeners;
	protected PEPeerManagerListener	peerManagerListener;
	
	protected volatile int[]	availability_cow;	// the dynamically modified availability
	protected final AEMonitor	availabilityMon =new AEMonitor( "PiecePicker:avail");
	protected boolean[]			doneAvailable;			// the pieces already added to availability

	private long				time_last_avail;
	protected volatile long		availabilityChange;
	protected volatile long		availabilityComputeChange;
	
	private int[]				availability;	// consistent availability to use throughout checkDownloadPossible

	private float	globalAvail;
	private float	globalAvgAvail;
	private int		nbRarestActive;
	private int		globalMin;
	/**
	 * The rarest availability level of pieces that we affirmatively want to try to DL from others 
	 */
	private int		globalMinOthers;
	
	private ParameterListenerImpl	parameterListener;

	private boolean				rarestOverride;
	
	private volatile long[]		startPriorities;		// the priority for starting each piece/base priority for resuming
	protected volatile long		userPriorityChange;		// event # of user priority settings changes 
	
	private long				timeLastPriorities;
	private volatile long		priorityUserChange;			// last user priority event # when priority bases were calculated
	private volatile long		priorityAvailabilityChange;	// last availability event # when priority bases were calculated
	
	protected volatile boolean	hasNeededUndonePiece;
	protected volatile long		neededUndonePieceChange;
	
	private volatile BitFlags	preQualifiedPieces;

	//A flag to indicate when we're in endgame mode
	private volatile boolean	endGameMode;
	private volatile boolean	endGameModeAbandoned;
	private volatile long		timeEndGameModeEntered;
	//The list of chunks needing to be downloaded (the mechanism change when entering end-game mode)
	private List 				endGameModeChunks;
	private final AEMonitor		endGameModeChunks_mon =new AEMonitor( "PiecePicker:EG");

	public PiecePickerImpl(final DiskManager dm)
	{
		diskManager =dm;
		dm_pieces =diskManager.getPieces();
	}
	
	public void start()
	{
 		nbPieces =diskManager.getNbPieces();
		nbPiecesDone =0;
		
		availability_cow =new int[nbPieces];
		doneAvailable =new boolean[nbPieces];

		time_last_avail =Long.MIN_VALUE;
		availabilityChange =Long.MIN_VALUE + 1;	// ensure at least one compute gets done
		availabilityComputeChange =Long.MIN_VALUE;

		globalAvail =0;
		globalAvgAvail =0;
		nbRarestActive =0;
		globalMin =0;
		globalMinOthers =0;
		
		rarestOverride =true;

//		startPriorities =new long[nbPieces];
		userPriorityChange =0;
		
		timeLastPriorities =Long.MIN_VALUE;
		priorityUserChange =Long.MIN_VALUE;
		priorityAvailabilityChange =Long.MIN_VALUE;
		
		hasNeededUndonePiece =false;
		neededUndonePieceChange =0;

		endGameMode =false;
		endGameModeAbandoned =false;
		timeEndGameModeEntered =0;
		
		
		// initialize each piece; on going changes will use event driven tracking
		for (int i =0; i <nbPieces; i++)
		{
			final DiskManagerPiece dmPiece =dm_pieces[i];
			if (dmPiece.isDone())
			{
				availability_cow[i] =1;
				doneAvailable[i] =true;
				nbPiecesDone++;
			} else
			{
				hasNeededUndonePiece |=dmPiece.calcNeeded();
			}
		}
		availability =availability_cow;
		if (nbPiecesDone >0)
			availabilityChange++;
		if (hasNeededUndonePiece)
			neededUndonePieceChange++;
		
		diskManagerListener =new DiskManagerListenerImpl();
		diskManager.addListener(diskManagerListener);
		
		parameterListener =new ParameterListenerImpl();
		COConfigurationManager.addParameterListener("Prioritize Most Completed Files", parameterListener);
		COConfigurationManager.addAndFireParameterListener("Prioritize First Piece", parameterListener);

		computeAvailability();
	}

	public void stop()
	{
		hasNeededUndonePiece =false;
		neededUndonePieceChange++;
		
		peerListeners.clear();	// since we're stopping, doesn't seem like need to .removeListener ... ?
		peerControl.removeListener(peerManagerListener);
		
		diskManager.removeListener(diskManagerListener);
		
		COConfigurationManager.removeParameterListener("Prioritize First Piece", parameterListener);
		COConfigurationManager.removeParameterListener("Prioritize Most Completed Files", parameterListener);
		parameterListener =null;
		
		startPriorities =null;
		peerManagerListener =null;
		peerControl =null;
		
		diskManagerListener =null;
		nbPieces =-1;
	}

	public DiskManager getDiskManager()
	{
		return diskManager;
	}

	public void setPeerControl(final PEPeerControl pc)
	{
		peerControl =pc;
		
		peerListeners =new HashMap();
		
		peerManagerListener =new PEPeerManagerListenerImpl();
		peerControl.addListener(peerManagerListener);
		
		computeBasePriorities();
	}
	
	public PEPeerControl getPeerControl()
	{
		return peerControl;
	}
	
	
	public void addHavePiece(final int pieceNumber)
	{
		try
		{	availabilityMon.enter();
			availability_cow[pieceNumber]++;
			availabilityChange++;
		} finally {availabilityMon.exit();}
	}

	protected void addBitfield(final BitFlags peerHasPieces)
	{
		if (peerHasPieces ==null ||peerHasPieces.nbSet <=0)
			return;
		try
		{	availabilityMon.enter();
			for (int i =peerHasPieces.start; i <=peerHasPieces.end; i++)
			{
				if (peerHasPieces.flags[i])
					availability_cow[i]++;
			}
			availabilityChange++;
		} finally {availabilityMon.exit();}
	}

	protected void removeBitfield(final BitFlags peerHasPieces)
	{
		if (peerHasPieces ==null ||peerHasPieces.nbSet <=0)
			return;
		try
		{	availabilityMon.enter();
			for (int i =peerHasPieces.start; i <=peerHasPieces.end; i++)
			{
				if (peerHasPieces.flags[i] &&availability_cow[i] >0)
					availability_cow[i]--;
			}
			availabilityChange++;
		} finally {availabilityMon.exit();}
	}
	
	protected void computeAvailability()
	{
		final long now =SystemTime.getCurrentTime();
		if (availabilityComputeChange >=availabilityChange
			||(now >time_last_avail &&now <time_last_avail +TIME_MIN_AVAILABILITY))
			return;
		
		int i;
		final BitFlags newPreQualifiedPieces =new BitFlags(nbPieces);
		int allMin =Integer.MAX_VALUE;
		int rarestMin =Integer.MAX_VALUE;
		for (i =0; i <nbPieces; i++)
		{
			final int avail =availability[i];
			final DiskManagerPiece dmPiece =dm_pieces[i];
			if (avail >0 &&dmPiece.isNeeded() &&dmPiece.isRequestable())
			{
				newPreQualifiedPieces.set(i);
				if (avail <rarestMin) 
					rarestMin =avail;	// most important targets for near future requests from others
			}
			
			if (avail <allMin)
				allMin =avail;
		}
		// copy updated local variables into globals
		globalMin =allMin;
		globalMinOthers =rarestMin;
		preQualifiedPieces =newPreQualifiedPieces;
		
		int total =0;
		int rarestActive =0;
		long totalAvail =0;
		for (i =0; i <nbPieces; i++ )
		{
			final int avail =availability[i];
			final DiskManagerPiece dmPiece =dm_pieces[i];
			if (avail >0)
			{
				if (avail >allMin)
					total++;
				if (peerControl !=null &&avail <=rarestMin &&dmPiece.isNeeded()
					&&dmPiece.isRequestable() &&peerControl.getPiece(i) !=null)
					rarestActive++;
				totalAvail +=avail;
			}
		}
		// copy updated local variables into globals
		globalAvail =(total /(float) nbPieces) +allMin;
		if (peerControl !=null)
		{
			nbRarestActive =rarestActive;
			globalAvgAvail =totalAvail /(float) (nbPieces) /(1 +peerControl.getNbSeeds() +peerControl.getNbPeers());
		}
		time_last_avail =now;
		availabilityComputeChange =availabilityChange;
	}


	public int[] getAvailability()
	{
		return availability;
	}

	public int getAvailability(final int pieceNumber)
	{
		return availability[pieceNumber];
	}
	
	//this only gets called when the My Torrents view is displayed
	public float getMinAvailability()
	{
		computeAvailability();
		return globalAvail;
	}

	public float getAvgAvail()
	{
		computeAvailability();
		return globalAvgAvail;
	}


	/**
	 * Early-outs when finds a downloadable piece
	 * Either way sets hasNeededUndonePiece and neededUndonePieceChange if necessary 
	 */
	protected void checkDownloadablePiece()
	{
		for (int i =0; i <nbPieces; i++)
		{
			if (dm_pieces[i].isInteresting())
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
	public boolean checkDownloadPossible()
	{
		if (!hasNeededUndonePiece)
			return false;

		final List bestUploaders =new ArrayList();
		final List peer_transports =peerControl.getPeers();

		final long[] upRates =new long[peer_transports.size()];
		Arrays.fill(upRates, -1);

		for (int i =0; i <peer_transports.size(); i++)
		{
			final PEPeerTransport pt =(PEPeerTransport) peer_transports.get(i);
			if (pt.getPeerState() <PEPeer.CLOSING)
			{
				final long upRate =pt.getStats().getSmoothDataReceiveRate();
				UnchokerUtil.updateLargestValueFirstSort(upRate, upRates, pt, bestUploaders, 0);
			}
		}

		// lock in a consistent availability ...
		availability =(int[])availability_cow.clone();

		checkEndGameMode();
		computeAvailability();
		computeBasePriorities();

		for (int i =0; i <bestUploaders.size(); i++)
		{
			// get a connection
			final PEPeerTransport pt =(PEPeerTransport) bestUploaders.get(i);
			// can we transfer something?
			if (pt.isDownloadPossible())
			{
				// If queue is too low, we will enqueue another request
				int found =0;
				int maxRequests =REQUESTS_MIN +(int) (pt.getStats().getDataReceiveRate() /SLOPE_REQUESTS);
				if (maxRequests >REQUESTS_MAX ||maxRequests <0)
					maxRequests =REQUESTS_MAX;

				if (endGameMode)
					maxRequests =2;
				if (pt.isSnubbed())
					maxRequests =1;

				// Only loop when 3/5 of the queue is empty, in order to make more consecutive requests,
				// and improve cache efficiency
				if (pt.getNbRequests() <=(maxRequests *3) /5)
				{
					while (pt.isDownloadPossible() &&pt.getNbRequests() <maxRequests)
					{
						// is there anything else to download?
						if (!endGameMode)
							found =findPieceToDownload(pt, maxRequests);
						else
							found =findPieceInEndGameMode(pt, maxRequests);
						if (found <=0)
							break;
					}
				}
			}
		}
		return true;
	}
	
	/**
	 * @param pc
	 *            the PeerConnection we're working on
	 * @return true if a request was assigned, false otherwise
	 */
	protected int findPieceToDownload(final PEPeerTransport pt, final int wants)
	{
		final int pieceNumber =getRequestCandidate(pt);
		if (pieceNumber <0)
			return 0;

		int peerSpeed =(int) pt.getStats().getDataReceiveRate() /1024;

		PEPeerControl pc =pt.getControl();
		PEPiece pePiece =pc.getPiece(pieceNumber);
		if (pePiece ==null)
		{
			pePiece =new PEPieceImpl(pt.getManager(), dm_pieces[pieceNumber], peerSpeed >>1, false);

			// Assign the created piece to the pieces array.
			pc.addPiece(pePiece, pieceNumber);
			pePiece.setResumePriority(startPriorities[pieceNumber]);
			if (availability[pieceNumber] <=globalMinOthers)
				nbRarestActive++;
		}

		final int[] blocksFound =pePiece.getAndMarkBlocks(pt, wants);
		final int blockNumber =blocksFound[0];
		final int blocks =blocksFound[1];

		if (blocks <=0)
			return 0;

		int requested =0;
		// really try to send the request to the peer
		for (int i =0; i <blocks; i++)
		{
			final int thisBlock =blockNumber +i;
			if (pt.request(pieceNumber, thisBlock *DiskManager.BLOCK_SIZE, pePiece.getBlockSize(thisBlock)))
			{
				requested++;
				pt.setLastPiece(pieceNumber);
				// Up the speed on this piece?
				if (peerSpeed >pePiece.getSpeed())
					pePiece.incSpeed();
				// have requested a block
			}
		}
		return requested;
	}

	protected int findPieceInEndGameMode(final PEPeerTransport pt, final int wants)
	{
		if (pt ==null ||wants <=0 ||pt.getPeerState() !=PEPeer.TRANSFERING)
			return 0;
		// Ok, we try one, if it doesn't work, we'll try another next time
		try
		{
			endGameModeChunks_mon.enter();

			int nbChunks =endGameModeChunks.size();
			if (nbChunks >0)
			{
				int random =RandomUtils.generateRandomIntUpto(nbChunks);
				EndGameModeChunk chunk =(EndGameModeChunk) endGameModeChunks.get(random);
				int pieceNumber =chunk.getPieceNumber();
				if (pt.isPieceAvailable(pieceNumber))
				{
					PEPiece pePiece =pt.getControl().getPiece(pieceNumber);
					if (pePiece !=null)
					{
						if (pt.request(pieceNumber, chunk.getOffset(), chunk.getLength()))
						{
							pePiece.markBlock(pt, chunk.getBlockNumber());
							pt.setLastPiece(pieceNumber);
							return 1;
						}
						return 0;
					}

					endGameModeChunks.remove(chunk);
					// System.out.println("End Game Mode :: Piece is null : chunk remove !!!NOT REQUESTED!!!" +
					// chunk.getPieceNumber() + ":" + chunk.getOffset() + ":" + chunk.getLength());
					return 0;
				}
			}
		} finally
		{
			endGameModeChunks_mon.exit();
		}
		return 0;
	}
	
	public int getPieceToStart(final PEPeerTransport pt, final BitFlags startCandidates)
	{
		if (startCandidates ==null ||startCandidates.nbSet <=0)
		{	// cant do anything if no pieces to startup
			return -1;
		}

		int startI;
		int direction;
		
		if (startCandidates.nbSet ==1)
		{
			startI =startCandidates.start;
			direction =1;
		} else
		{
			// Mix it up!
			startI =RandomUtils.generateRandomIntBetween(startCandidates.start,
					startCandidates.end);
			direction =RandomUtils.generateRandomPlusMinus1();
		}

		pePieces =pt.getControl().getPieces();
		// For every Priority piece
		for (int i =startI; i >=startCandidates.start &&i <=startCandidates.end; i +=direction)
		{
			// is piece flagged and confirmed not in progress
			if (pePieces[i] ==null &&startCandidates.flags[i])
			{
				// This should be a piece we want to start
				return i;
			}
		}
		return -1;
	}

	public boolean hasDownloadablePiece()
	{
		return hasNeededUndonePiece;
	}

	public long getNeededUndonePieceChange()
	{
		return neededUndonePieceChange;
	}

	public boolean isInEndGameMode()
	{
		return endGameMode;
	}
	
	public void addEndGameChunks(final PEPieceImpl pePiece)
	{
		if (!endGameMode)
			return;
		try
		{
			endGameModeChunks_mon.enter();
			int nbChunks =pePiece.getNbBlocks();
			for (int i =0; i <nbChunks; i++ )
			{
				endGameModeChunks.add(new EndGameModeChunk(pePiece, i));
			}
		} finally
		{
			endGameModeChunks_mon.exit();
		}
	}

	public void removeFromEndGameModeChunks(final int pieceNumber, final int offset)
	{
		try
		{
			endGameModeChunks_mon.enter();

			Iterator iter =endGameModeChunks.iterator();
			while (iter.hasNext())
			{
				EndGameModeChunk chunk =(EndGameModeChunk) iter.next();
				if (chunk.compare(pieceNumber, offset))
					iter.remove();
			}
		} finally
		{
			endGameModeChunks_mon.exit();
		}
	}
	
	public void clearEndGameChunks()
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

	private void checkEndGameMode()
	{
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

		for (int i =0; i <nbPieces; i++ )
		{
			final DiskManagerPiece dmPiece =dm_pieces[i];
			// If the piece is being downloaded (fully requested), let's simply continue
			if (dmPiece.isRequested())
			{
				active_pieces++ ;
				continue;
			}
			// If the piece isn't even needed, or doesn't need more requesting, simply continue
			if (!dmPiece.isNeeded() ||!dmPiece.isRequestable())
				continue;

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
	
	private void computeEndGameModeChunks()
	{
		endGameModeChunks =new ArrayList();
		PEPiece[] _pieces =peerControl.getPieces();
		if (_pieces ==null)
			return;

		try
		{
			endGameModeChunks_mon.enter();

			for (int i =0; i <nbPieces; i++ )
			{
				DiskManagerPiece dmPiece =dm_pieces[i];
				// Pieces not Needed or not needing more downloading are of no interest
				if (!dmPiece.isNeeded() ||!dmPiece.isRequestable())
					continue;
				
				PEPiece pePiece =_pieces[i];
				if (pePiece ==null)
					continue;

				boolean written[] =dmPiece.getWritten();
				if (written ==null)
				{
					if (!dmPiece.isWritten())
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
	
	/** This computes the base priority for all pieces that need requesting if there's
	 * been any availability change or user priority setting changes since the last
	 * call, which will be most of the time since availability changes so dynamicaly
	 * It will change startPriorities[] (unless there was nothing to do)
	 */
	private void computeBasePriorities()
	{
		final long now =SystemTime.getCurrentTime();
		if (priorityUserChange >=userPriorityChange &&priorityAvailabilityChange >=availabilityChange
			||(now >timeLastPriorities &&now <time_last_avail +TIME_MIN_PRIORITIES))
			return;		// *somehow* nothing changed, so nothing to do
		
		final int 		nbSeeds		=peerControl.getNbSeeds();
		final int 		nbPeers		=peerControl.getNbPeers();
		final boolean	bootstrap	=nbPiecesDone <4;
		
		boolean 		changedPriority	=false;
		boolean			foundPieceToDownload =false;
		long[] 			newPriorities	=new long[nbPieces];
		try
		{
			// calculate all base (starting) priorities for all pieces needing requesting
			for (int i =0; i <nbPieces; i++)
			{
				final int avail =availability[i];
				DiskManagerPiece dmPiece =dm_pieces[i];
				if (dmPiece.isDone())
					continue;	// nothing to do for pieces not needing requesting
				
				// Dont seek rarest this time around under a few circumstances, so that other factors work better
				// never seek rarest when bootstrapping torrent
				boolean newRarestOverride =bootstrap;
				if (!newRarestOverride &&nbRarestActive >=nbSeeds &&globalMinOthers >1)
				{
					// if already getting some rarest, dont get more if swarm is healthy or too many pieces running
					newRarestOverride =globalMinOthers >globalMin
					||(globalMinOthers >=(2 *nbSeeds) &&(2 *globalMinOthers) >=nbPeers)
					||nbRarestActive >=(nbSeeds +nbPeers);
					// Interest in Rarest pieces (compared to user priority settings) could be influenced by several factors;
					// less demand closer to 0% and 100% of the torrent/farther from 50% of the torrent
					// less demand closer to 0% and 100% of peers interestd in us/farther from 50% of peers interested in us
					// less demand the more pieces are in progress (compared to swarm size)
					// less demand the farther ahead from absolute global minimum we're at already
					// less demand the healthier a swarm is (rarity compared to # seeds and # peers)
				}
				rarestOverride =newRarestOverride;
				
				long startPriority =Long.MIN_VALUE;
				long priority =Long.MIN_VALUE;
				
				final DMPieceList pieceList =diskManager.getPieceList(dmPiece.getPieceNumber());
				for (int j =0; j <pieceList.size(); j++)
				{
					final DiskManagerFileInfoImpl file =pieceList.get(j).getFile();
					final long fileLength =file.getLength();
					if (fileLength >0 &&file.getDownloaded() <fileLength &&!file.isSkipped())
					{
						priority =0;
						// user option "prioritize first and last piece"
						// TODO: should prioritize ~10% to ~%25 from edges of file
						if (firstPiecePriority &&file.getNbPieces() >FIRST_PIECE_MIN_NB)
						{
							if (i ==file.getFirstPieceNumber() ||i ==file.getLastPieceNumber())
								priority +=PRIORITY_W_FIRSTLAST;
						}
						// if the file is high-priority
						// startPriority +=(1000 *fileInfo.getPriority()) /255;
						if (file.isPriority())
						{
							priority +=PRIORITY_W_FILE;
							if (completionPriority)
								priority +=(PRIORITY_W_COMPLETION *file.getDownloaded()) /diskManager.getTotalLength();
						}
						if (priority >startPriority)
							startPriority =priority;
					}
				}
				
				if (startPriority >=0)
				{
					dmPiece.setNeeded();
					foundPieceToDownload =true;
					if (avail >0)
					{
						// boost priority for rarity
						startPriority +=(PRIORITY_W_RARE +nbPeers -nbSeeds) /avail;
						// Boost priority even a little more if it's a globally rarest piece
						if (!rarestOverride &&avail <=globalMinOthers)
							startPriority +=PRIORITY_W_RAREST /avail;
					}
					
				} else
				{
					dmPiece.clearNeeded();
				}
				
				newPriorities[i] =startPriority;
				changedPriority =true;
			}
		} catch (Throwable e)
		{
			Debug.printStackTrace(e);
		}
		priorityUserChange =userPriorityChange;
		priorityAvailabilityChange =availabilityChange;
		
		if (foundPieceToDownload)
		{
			if (!hasNeededUndonePiece)
			{
				hasNeededUndonePiece =true;
				neededUndonePieceChange++;
			}
		} else if (hasNeededUndonePiece)
		{
			hasNeededUndonePiece =false;
			neededUndonePieceChange++;
		}
		
		if (changedPriority)
			startPriorities =newPriorities;
	}
	
	// set FORCE_PIECE if trying to diagnose piece problems and only want to d/l a specific piece from a torrent
	private static final int	FORCE_PIECE	=-1;

	/**
	 * This method is the downloading core. It decides, for a given peer,
	 * which block should be requested. Here is the overall algorithm :
	 * 0. If there a FORCED_PIECE or reserved piece, that will be started/resumed if possible
	 * 1. Scan all the active pieces and find the rarest piece (and highest priority among equally rarest)
	 *	that can possibly be continued by this peer, if any
	 * 2. While scanning the active pieces, develop a list of equally highest priority pieces
	 *	(and equally rarest among those) as candidates for starting a new piece
	 * 3. If it can't find any piece, this means all pieces are
	 *	already downloaded/full requested
	 * 4. Returns int[] pieceNumber, blockNumber if a request to be made is found,
	 *	or null if none could be found
	 * 
	 * @param pc PEPeerTransport to work with
	 * @return int with pieceNumberto be requested or -1 if no request could be found
	 */
	private int getRequestCandidate(final PEPeerTransport pt)
	{
		if (peerControl ==null || pt ==null ||pt.getPeerState() !=PEPeer.TRANSFERING)
			return -1;
		final BitFlags	peerHavePieces =pt.getAvailable();

		// piece number and its block number that we'll try to DL
		int 			pieceNumber;				// will be set to the piece # we want to resume

		if (FORCE_PIECE >-1 &&FORCE_PIECE <nbPieces)
			pieceNumber =FORCE_PIECE;
		else
			pieceNumber =pt.getReservedPieceNumber();

		// If there's a piece Reserved to this peer or a FORCE_PIECE, start/resume it and only it (if possible)
		if (pieceNumber >=0)
		{
			if (peerHavePieces.flags[pieceNumber] &&dm_pieces[pieceNumber].isNeeded()
				&&dm_pieces[pieceNumber].isRequestable())
				return pieceNumber;
			return -1; // this is an odd case that maybe should be handled better, but checkers might fully handle it
		}

		final int			peerSpeed =(int) pt.getStats().getDataReceiveRate() /1000;	// how many KB/s has the peer has been sending
		final int			lastPiece =pt.getLastPiece();

		long resumeMaxPriority =Long.MIN_VALUE;
		boolean resumeIsRarest =false; // can the peer continuea piece with lowest avail of all pieces we want

		BitFlags			startCandidates =null;
		int					startCandidatesMinAvail =Integer.MAX_VALUE;
		long				startMaxPriority =Long.MIN_VALUE;
		boolean				rarestCanStart =false;

		DiskManagerPiece	dmPiece;	// the DiskManagerPiece backing the PEPiece under inspection
		PEPiece 			pePiece;	// the PEPiece under inspection	
		long				priority;	// aggregate priority of piece under inspection (start priority or resume priority for pieces to be resumed)
		int 				avail;		// the swarm-wide availability level of the piece under inspection
		int 				freeReqs;	// the number of unrequested (free) blocks in the piece under inspection
		int 				pieceSpeed;	// the current speed rating of the PEPiece under inspection
		long 				staleness;	// how long since there's been a write to the resume piece under inspection
		long 				pieceAge;	// how long since the PEPiece first started downloading (requesting, actually)
		
		final int	startI =preQualifiedPieces.start >peerHavePieces.start ?preQualifiedPieces.start :peerHavePieces.start;
		final int 	endI =preQualifiedPieces.end <peerHavePieces.end ?preQualifiedPieces.end :peerHavePieces.end;
		int 		i;

		pePieces =peerControl.getPieces();
		final long	now =SystemTime.getCurrentTime();
		// Try to continue a piece already loaded, according to priority
		for (i =startI; i <=endI; i++)
		{
			// is the piece: Needed, not fully: Requested, Downloaded, Written, hash-Checking or Done?
			// and is the piece available from this peer?
			if (preQualifiedPieces.flags[i] &&peerHavePieces.flags[i])
			{
				priority =startPriorities[i];
				if (priority >=0)
				{
					avail =availability[i];
					if (avail ==0)	// maybe we didn't know we could get it before
						avail++;	// but the peer says s/he has it

					// is the piece loaded (therefore active)
					pePiece =pePieces[i];
					if (pePiece !=null)
					{
						// How many requests can still be made on this piece?
						dmPiece =dm_pieces[i];
						freeReqs =pePiece.getNbUnrequested();
						if (freeReqs <=0)
							continue;

						// Don't touch pieces reserved for others
						String peerReserved =pePiece.getReservedBy();
						if (peerReserved !=null)
						{
							if (peerReserved !=pt.getIp())
								continue;	//reserved ot somebody else
							return i;	// the peer forgot this is reserved to him, but we located it
						}

						pieceSpeed =pePiece.getSpeed();
						// peers allowed to continue same piece as last requested from them
						if (pieceSpeed >0 &&i !=lastPiece)
						{
							// if the peer is snubbed, only request on slow pieces
							if (peerSpeed <pieceSpeed ||pt.isSnubbed())
							{
								// peer allowed when free blocks is >= 3 and enough to not slow piece down too much
								if (freeReqs <3 ||pieceSpeed -1 >=freeReqs *peerSpeed)
									continue;
							}
						}

						priority +=pieceSpeed;
						priority +=(i ==lastPiece) ?PRIORITY_W_SAME_PIECE :0;
						// Adjust priority for purpose of continuing pieces
						// how long since last written to
						staleness =now -dmPiece.getLastWriteTime();
						if (staleness >0)
							priority +=staleness /PRIORITY_DW_STALE;
						// how long since piece was started
						pieceAge =now -pePiece.getCreationTime();
						if (pieceAge >0)
							priority +=PRIORITY_W_AGE *pieceAge /(PRIORITY_DW_AGE *dmPiece.getNbBlocks());
						// how much is already written to disk
						priority +=(PRIORITY_W_PIECE_DONE *dmPiece.getNbWritten()) /dmPiece.getNbBlocks();

						pePiece.setResumePriority(priority);

						if ((avail <=globalMinOthers &&(!rarestOverride &&!resumeIsRarest))
							||(priority >resumeMaxPriority &&(!resumeIsRarest ||rarestOverride ||avail <=globalMinOthers)))
						{ // this piece seems like best choice for resuming
							// Verify it's still possible to get a block to request from this piece
							if (pePiece.hasUnrequestedBlock())
							{	// change the different variables to reflect interest in this block
								pieceNumber =i;
								resumeMaxPriority =priority;
								resumeIsRarest =avail <=globalMinOthers; // only going to try to resume one
							}
						}
					} else if ((!resumeIsRarest &&priority >=startMaxPriority)
						||(!rarestOverride &&!resumeIsRarest &&avail <=globalMinOthers)
						||(avail <=globalMinOthers &&priority >=startMaxPriority)
						||(rarestOverride &&priority >=startMaxPriority))
					{ // Piece isn't already loaded, so not in progress; now we check if it's a candidate for starting
						// see if this piece is rarest, thus needs can-start-rarest handling
						if (!rarestOverride &&avail <=globalMinOthers)
						{ // yes; a rarest piece so we do can-start-rarest handling
							if (startCandidates ==null)
								startCandidates =new BitFlags(nbPieces);
							// are we switching into this mode, OR have we somehow found even rarer pieces (ie; skewed
							// avail)
							if (!rarestCanStart ||avail <startCandidatesMinAvail)
							{ // yep; switched to can-start-rarest detection mode so lock onto real rarest
								rarestCanStart =true;
								startCandidates.setOnly(i); // clear the non-rarest bits in favor of only rarest
								startMaxPriority =priority;
								startCandidatesMinAvail =avail;
							} else
							{ // already handling really rarest-only pieces; just add this one
								startCandidates.setEnd(i); // clear the non-rarest bits in favor of only rarest
							}
						} else if (priority >startMaxPriority)
						{ // didn't need rarest handling, but it's higher priority than before
							if (startCandidates ==null)
								startCandidates =new BitFlags(nbPieces);
							startCandidates.setOnly(i);
							startMaxPriority =priority;
							startCandidatesMinAvail =avail;
						} else
						{ // this is a valid start candidate, no better than any before it
							startCandidates.setEnd(i);
						}
					}
				}
			}
		}

		// See if have found a valid (piece;block) to request from a piece in progress
		if (pieceNumber >=0 &&(rarestOverride ||resumeIsRarest ||!rarestCanStart))
			return pieceNumber;

		// Gets here when no resume piece choice was made
		return getPieceToStart(pt, startCandidates); // pick piece from candidates bitfield
	}


	private int calcAvailability(final int pieceNumber)
	{
		final List	peer_transports =peerControl.getPeers();
		int avail =doneAvailable[pieceNumber] ?1 :0;
		//for all peers
		for (int i =peer_transports.size() -1; i >=0; i--)
		{	//get the peer connectiotn
			final PEPeerTransport pt =(PEPeerTransport)peer_transports.get(i);
			if (pt.getPeerState() ==PEPeer.TRANSFERING &&pt.isPieceAvailable(pieceNumber))
				avail++;
		}
		try
		{	availabilityMon.enter();
			availability_cow[pieceNumber] =avail;
			availabilityChange++;
		} finally {availabilityMon.exit();}
		return avail;
	}
	
	private class ParameterListenerImpl
		implements ParameterListener
	{
		public void parameterChanged(String parameterName)
		{
			if (parameterName.equals("Prioritize Most Completed Files"))
			{
				completionPriority =COConfigurationManager.getBooleanParameter(parameterName, false);
				userPriorityChange++;	// this is a user's priority change event
			} else if (parameterName.equals("Prioritize First Piece"))
			{
				firstPiecePriority =COConfigurationManager.getBooleanParameter(parameterName, false);
				userPriorityChange++;	// this is a user's priority change event
		    }
	    }
	}

	/**
	 * An instance of this listener is registered with peerControl
	 * Through this, we learn of peers leaving and joining
	 */
	private class PEPeerManagerListenerImpl
		implements PEPeerManagerListener
	{
		public void peerAdded(final PEPeerManager manager, final PEPeer peer )
		{
			PEPeerListenerImpl peerListener;
			if (peerListeners.containsKey(peer))
			{
				peerListener =(PEPeerListenerImpl)peerListeners.get(peer);
			} else
			{
				peerListener =new PEPeerListenerImpl();
				peerListeners.put(peer, peerListener);
			}
			peer.addListener(peerListener);
		}
		
		public void peerRemoved(final PEPeerManager manager, final PEPeer peer )
		{
			// the listener should be gone already, but nullify it just in case it isn't
			if (peer !=null)
				peerListeners.remove(peer);
		}
	}
	
	/**
	 * An instance of this listener is registered with each peer
	 */
	private class PEPeerListenerImpl
		implements PEPeerListener
	{
		public void stateChanged(final PEPeer peer, final int new_state )
		{
			if (new_state ==PEPeer.CONNECTING)
			{
				int placeholder =new_state;
			} else if (new_state ==PEPeer.HANDSHAKING)
			{
				int placeholder =new_state;
			} else if (new_state ==PEPeer.TRANSFERING)
			{
				addBitfield(peer.getAvailable());
			} else if (new_state ==PEPeer.CLOSING)
			{
				// remove the bitfield from availability
				removeBitfield(peer.getAvailable());
				
				// remove this listener from the peer
				final PEPeerListenerImpl peerListener =(PEPeerListenerImpl)peerListeners.remove(peer);
				if (peer !=null &&peerListener !=null)
					peer.removeListener(peerListener);
			}
		}
		
		public void sentBadChunk(final PEPeer peer, final int piece_num, final int total_bad_chunks )
		{
			/* nothing to do here */
		}
		
		public void receivedBitfield(final PEPeer peer)
		{
			addBitfield(peer.getAvailable());
		}
	}
	
	/**
	 * An instance of this listener is registered peerControl
	 * @author MjrTom
	 */
	private class DiskManagerListenerImpl
		implements DiskManagerListener
	{
		public void stateChanged(int oldState, int newState)
		{
			//starting torrent
		}

		public void filePriorityChanged(DiskManagerFileInfo file)
		{
			// record that user-based priorities changed
			userPriorityChange++;	// this is a user's priority change event
			
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
				final DiskManagerPiece dmPiece =dm_pieces[i];
				if (dmPiece.isRequestable())
					foundPieceToDownload |=dmPiece.calcNeeded();
			}
			if (foundPieceToDownload &&!hasNeededUndonePiece)
			{
				hasNeededUndonePiece =true;
				neededUndonePieceChange++;
			}
		}

//			// the first piece can span multiple files
//			int i =file.getFirstPieceNumber();
//			if (!doneAvailable[i])
//				foundPieceToDownload |=calcStartPriority(dm_pieces[i]);
//			i++;
//			
//			int lastPieceNumber =file.getLastPieceNumber();
//			
//			// intermediary pieces, if any, are all in the one file
//			long fileLength =file.getLength();
//			for (; i <lastPieceNumber; i++)
//			{
//				// don't need pieces if file is 0 length, or downloaded, or DND/Delete
//				if (fileLength >0 &&file.getDownloaded() <fileLength &&!file.isSkipped())
//				{
//					if (!doneAvailable[i])
//						foundPieceToDownload |=calcStartPriorityFile(dm_pieces[i], file);
//				} else	// need to be sure every not needed piece is marked as such
//					dm_pieces[i].clearNeeded();
//			}
//			
//			// maybe the last piece is the same as the first (or maybe it's already done)
//			if (i ==lastPieceNumber &&!doneAvailable[i])
//				foundPieceToDownload |=calcStartPriority(dm_pieces[i]);
//			
//			// if we found a piece to download and didn't have one before, need to checkInterested on peers
//			if (foundPieceToDownload)
//			{
//				if (!hasNeededUndonePiece)
//				{
//					hasNeededUndonePiece =true;
//					neededUndonePieceChange++;
//				}
//			} else	//if didn't find a piece, need to look through whole torrent to know what we should do
//				checkDownloadablePiece();
//		}
		

		public void pieceDoneChanged(DiskManagerPiece dmPiece)
		{
			int pieceNumber =dmPiece.getPieceNumber();
			if (dmPiece.isDone())
			{
				if (!doneAvailable[pieceNumber])
				{
					doneAvailable[pieceNumber] =true;
					try
					{	availabilityMon.enter();
					availability_cow[pieceNumber]++;
					availabilityChange++;
					} finally {availabilityMon.exit();}
					nbPiecesDone++;
				}
			} else if (doneAvailable[pieceNumber])
			{
				doneAvailable[pieceNumber] =false;
				try
				{	availabilityMon.enter();
				availability_cow[pieceNumber]--;
				availabilityChange++;
				} finally {availabilityMon.exit();}
				nbPiecesDone--;
				if (dmPiece.calcNeeded() &&dmPiece.isRequestable() &&!hasNeededUndonePiece)
				{
					hasNeededUndonePiece =true;
					neededUndonePieceChange++;
				}
			}
		}

		public void fileAccessModeChanged(DiskManagerFileInfo file, int old_mode, int new_mode)
		{
			//file done (write to read)
			//starting to upload from the file (read to write)
		}
	}

//	/**
//	 * calculates a piece's start priority when the piece is in only one single file
//	 * @param DiskManagerPiece		dmPiece	the piece to check
//	 * @param DiskManagerFileInfo	file	the one single file the piece is in
//	 */
//	protected boolean calcStartPriorityFile(final DiskManagerPiece dmPiece, final DiskManagerFileInfo file)
//	{
//		if (dmPiece ==null ||file ==null ||diskManager ==null)
//			return false;
//		final int pieceNumber =dmPiece.getPieceNumber();
//		long priority =Long.MIN_VALUE;
//		final long fileLength =file.getLength();
//		if (fileLength >0 &&file.getDownloaded() <fileLength &&!file.isSkipped())
//		{
//			priority =0;
//			// user option "prioritize first and last piece"
//			// TODO: should prioritize ~10% to ~%25 from edges of file
//			if (firstPiecePriority &&file.getNbPieces() >FIRST_PIECE_MIN_NB)
//			{
//				if (pieceNumber ==file.getFirstPieceNumber() ||pieceNumber ==file.getLastPieceNumber())
//					priority +=PRIORITY_W_FIRSTLAST;
//			}
//			// if the file is high-priority
//			// startPriority +=(1000 *fileInfo.getPriority()) /255;
//			if (file.isPriority())
//			{
//				priority +=PRIORITY_W_FILE;
//				if (completionPriority)
//					priority +=(PRIORITY_W_COMPLETION *file.getDownloaded()) /diskManager.getTotalLength(); 
//			}
//		}
//		startPriorities[pieceNumber] =priority;
//		if (priority >=0)
//		{
//			dmPiece.setNeeded();
//			if (!doneAvailable[pieceNumber])
//			{
//				if (!hasNeededUndonePiece)
//				{
//					hasNeededUndonePiece =true;
//					neededUndonePieceChange++;
//				}
//				return true;
//			}
//		} else
//			dmPiece.clearNeeded();
//		return false;
//	}
//	
//	protected boolean calcStartPriority(final DiskManagerPiece dmPiece)
//	{
//		if (dmPiece ==null ||diskManager ==null)
//			return false;
//		try
//		{
//			final int pieceNumber =dmPiece.getPieceNumber();
//			long startPriority =Long.MIN_VALUE;
//			long priority =Long.MIN_VALUE;
//			final DMPieceList pieceList =diskManager.getPieceList(dmPiece.getPieceNumber());
//			for (int i =0; i <pieceList.size(); i++)
//			{
//				final DiskManagerFileInfoImpl file =pieceList.get(i).getFile();
//				final long fileLength =file.getLength();
//				if (fileLength >0 &&file.getDownloaded() <fileLength &&!file.isSkipped())
//				{
//					priority =0;
//					// user option "prioritize first and last piece"
//					// TODO: should prioritize ~10% to ~%25 from edges of file
//					if (firstPiecePriority &&file.getNbPieces() >FIRST_PIECE_MIN_NB)
//					{
//						if (pieceNumber ==file.getFirstPieceNumber() ||pieceNumber ==file.getLastPieceNumber())
//							priority +=PRIORITY_W_FIRSTLAST;
//					}
//					// if the file is high-priority
//					// startPriority +=(1000 *fileInfo.getPriority()) /255;
//					if (file.isPriority())
//					{
//						priority +=PRIORITY_W_FILE;
//						if (completionPriority)
//							priority +=(PRIORITY_W_COMPLETION *file.getDownloaded()) /diskManager.getTotalLength(); 
//					}
//					if (priority >startPriority)
//						startPriority =priority;
//				}
//			}
//			startPriorities[pieceNumber] =startPriority;
//			if (startPriority >=0)
//			{
//				dmPiece.setNeeded();
//				if (!doneAvailable[pieceNumber])
//				{
//					if (!hasNeededUndonePiece)
//					{
//						hasNeededUndonePiece =true;
//						neededUndonePieceChange++;
//					}
//					return true;
//				}
//			} else
//				dmPiece.clearNeeded();
//		} catch (Throwable e)
//		{
//			Debug.printStackTrace(e);
//		}
//		return false;
//	}
	
	
}
