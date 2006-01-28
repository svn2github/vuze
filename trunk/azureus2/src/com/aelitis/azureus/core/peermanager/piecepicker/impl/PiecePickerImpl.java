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
import org.gudy.azureus2.core3.peer.impl.transport.PEPeerTransportProtocol;
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

	private static final long TIME_MIN_AVAILABILITY	=976;	// min ms for recalculating availability

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
	
	private long			time_last_avail;

	protected int			nbPieces =-1;
	protected volatile int	nbPiecesDone;

	protected final DiskManagerPiece[]	dm_pieces;
	protected volatile PEPiece[]		pePieces;

	protected volatile int[]	availability_cow;
	protected boolean[]			doneAvailable;			// the piece already added to availability

	protected volatile long		availabilityChange;
	protected volatile long		availabilityComputeChange;

	private float	globalAvail;
	private float	globalAvgAvail;
	private int		nbRarestActive;
	private int		globalMin;
	/**
	 * The rarest availability level of pieces that we affirmatively want to try to DL from others 
	 */
	private int		globalMinOthers;

	private long[]				startPriorities;
	protected volatile boolean	hasNeededUndonePiece;
	protected volatile long		neededUndonePieceChange;
	
	private volatile BitFlags	preQualifiedPieces;

	//A flag to indicate when we're in endgame mode
	private volatile boolean	endGameMode =false;
	private volatile boolean	endGameModeAbandoned =false;
	private volatile long		timeEndGameModeEntered;
	//The list of chunks needing to be downloaded (the mechanism change when entering end-game mode)
	private List 				endGameModeChunks;
	private final AEMonitor		endGameModeChunks_mon	= new AEMonitor( "PiecePicker:EG");

	static
	{
		ParameterListener ParameterListenerImpl = new ParameterListener()
		{
			public void parameterChanged(String parameterName)
			{
			    completionPriority =COConfigurationManager.getBooleanParameter("Prioritize Most Completed Files", false);
				firstPiecePriority =COConfigurationManager.getBooleanParameter("Prioritize First Piece", false);
		    }

		};

//		COConfigurationManager.addParameterListener("Prioritize Most Completed Files", ParameterListenerImpl);
		COConfigurationManager.addAndFireParameterListener("Prioritize First Piece", ParameterListenerImpl);
	}
	   
	public PiecePickerImpl(final DiskManager dm)
	{
		diskManager =dm;
		dm_pieces =diskManager.getPieces();
	}
	
	public void start()
	{
 		nbPieces =diskManager.getNbPieces();
		nbPiecesDone =0;
		availabilityChange =Long.MIN_VALUE;
		availabilityComputeChange =Long.MIN_VALUE;
		time_last_avail =Long.MIN_VALUE;

		doneAvailable =new boolean[nbPieces];
		availability_cow =new int[nbPieces];

		startPriorities =new long[nbPieces];
		hasNeededUndonePiece =false;
		neededUndonePieceChange =0;

		endGameMode =false;

		// initialize each piece; on going changes will use event driven tracking
		for (int i =0; i <nbPieces; i++)
		{
			if (dm_pieces[i].isDone())
			{
				availability_cow[i] =1;
				doneAvailable[i] =true;
				nbPiecesDone++;
			} else
				hasNeededUndonePiece |=calcStartPriority(dm_pieces[i]);
		}
		if (nbPiecesDone >0)
			availabilityChange++;
		if (hasNeededUndonePiece)
			neededUndonePieceChange++;
		
		diskManagerListener =new DiskManagerListenerImpl();
		diskManager.addListener(diskManagerListener);
		
		computeAvailability();
	}

	public void stop()
	{
		hasNeededUndonePiece =false;
		diskManager.removeListener(diskManagerListener);
		diskManagerListener =null;
		startPriorities =null;
		peerControl =null;
		nbPieces =-1;
//		COConfigurationManager.removeParameterListener("Prioritize First Piece", this);
//		COConfigurationManager.removeParameterListener("Prioritize Most Completed Files", this);
	}

	public DiskManager getDiskManager()
	{
		return diskManager;
	}

	public void setPeerControl(final PEPeerControl pc)
	{
		peerControl =pc;
	}
	
	public PEPeerControl getPeerControl()
	{
		return peerControl;
	}
	
	
	public void addHavePiece(final int pieceNumber)
	{
		availability_cow[pieceNumber]++;
		availabilityChange++;
	}

	public void addBitfield(final BitFlags peerHasPieces)
	{
		if (peerHasPieces ==null ||peerHasPieces.nbSet <=0)
			return;
		for (int i =peerHasPieces.start; i <=peerHasPieces.end; i++)
		{
			if (peerHasPieces.flags[i])
				availability_cow[i]++;
		}
		availabilityChange++;
	}

	public void removeBitfield(final BitFlags peerHasPieces)
	{
		if (peerHasPieces ==null ||peerHasPieces.nbSet <=0)
			return;
		for (int i =peerHasPieces.start; i <=peerHasPieces.end; i++)
		{
			if (peerHasPieces.flags[i] &&availability_cow[i] >0)
				availability_cow[i]--;
		}
		availabilityChange++;
	}
	
	protected void computeAvailability()
	{
		final long now =SystemTime.getCurrentTime();
		if (availabilityComputeChange >=availabilityChange
			||(now >time_last_avail &&now <time_last_avail +TIME_MIN_AVAILABILITY))
			return;

		final int[] availability =availability_cow;
		
		int i;
		final BitFlags newPreQualifiedPieces =new BitFlags(nbPieces);
		int allMin =Integer.MAX_VALUE;
		int rarestMin =Integer.MAX_VALUE;
		for (i =0; i <nbPieces; i++)
		{
			final int avail =availability[i];
			if (avail >0 &&diskManager.isRequestable(i))
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
			if (avail >0)
			{
				if (avail >allMin)
					total++;
				if (avail <=rarestMin &&peerControl !=null &&dm_pieces[i].isRequestable() &&peerControl.getPiece(i) !=null)
					rarestActive++;
				totalAvail +=availability[i];
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
		return availability_cow;
	}

	public int getAvailability(final int pieceNumber)
	{
		return availability_cow[pieceNumber];
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

		checkEndGameMode();
		computeAvailability();
		for (int i =0; i <bestUploaders.size(); i++)
		{
			// get a connection
			final PEPeerTransportProtocol pt =(PEPeerTransportProtocol) bestUploaders.get(i);
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
						maxRequests -=found;
						if (found <=0 ||maxRequests <=0)
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
		PEPiece piece =pc.getPiece(pieceNumber);
		if (piece ==null)
		{
			piece =new PEPieceImpl(pt.getManager(), dm_pieces[pieceNumber], peerSpeed >>1, false);

			// Assign the created piece to the pieces array.
			pc.addPiece(piece, pieceNumber);
			
			if (availability_cow[pieceNumber] <=globalMinOthers)
				nbRarestActive++;
		}

		final int[] blocksFound =piece.getAndMarkBlocks(pt, wants);
		final int blockNumber =blocksFound[0];
		final int blocks =blocksFound[1];

		if (blocks <=0)
			return 0;

		int requested =0;
		// really try to send the request to the peer
		for (int i =0; i <blocks; i++)
		{
			final int thisBlock =blockNumber +i;
			if (pt.request(pieceNumber, thisBlock *DiskManager.BLOCK_SIZE, piece.getBlockSize(thisBlock)))
			{
				requested++;
				pt.setLastPiece(pieceNumber);
				// Up the speed on this piece?
				if (peerSpeed >piece.getSpeed())
					piece.incSpeed();
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
	
	/**
	 * calculates a piece's start priority when the piece is in only one single file
	 * @param DiskManagerPiece		dmPiece	the piece to check
	 * @param DiskManagerFileInfo	file	the one single file the piece is in
	 */
	protected boolean calcStartPriorityFile(final DiskManagerPiece dmPiece, final DiskManagerFileInfo file)
	{
		if (dmPiece ==null ||file ==null ||diskManager ==null)
			return false;
		final int pieceNumber =dmPiece.getPieceNumber();
		long priority =Long.MIN_VALUE;
		final long fileLength =file.getLength();
		if (fileLength >0 &&file.getDownloaded() <fileLength &&!file.isSkipped())
		{
			priority =0;
			// user option "prioritize first and last piece"
			// TODO: should prioritize ~10% to ~%25 from edges of file
			if (firstPiecePriority &&file.getNbPieces() >FIRST_PIECE_MIN_NB)
			{
				if (pieceNumber ==file.getFirstPieceNumber() ||pieceNumber ==file.getLastPieceNumber())
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
		}
		startPriorities[pieceNumber] =priority;
		if (priority >=0)
		{
			dmPiece.setNeeded();
			if (!doneAvailable[pieceNumber])
			{
				if (!hasNeededUndonePiece)
				{
					hasNeededUndonePiece =true;
					neededUndonePieceChange++;
				}
				return true;
			}
		} else
			dmPiece.clearNeeded();
		return false;
	}

	protected boolean calcStartPriority(final DiskManagerPiece dmPiece)
	{
		if (dmPiece ==null ||diskManager ==null)
			return false;
		try
		{
			final int pieceNumber =dmPiece.getPieceNumber();
			long startPriority =Long.MIN_VALUE;
			long priority =Long.MIN_VALUE;
			final DMPieceList pieceList =diskManager.getPieceList(dmPiece.getPieceNumber());
			for (int i =0; i <pieceList.size(); i++)
			{
				final DiskManagerFileInfoImpl file =pieceList.get(i).getFile();
				final long fileLength =file.getLength();
				if (fileLength >0 &&file.getDownloaded() <fileLength &&!file.isSkipped())
				{
					priority =0;
					// user option "prioritize first and last piece"
					// TODO: should prioritize ~10% to ~%25 from edges of file
					if (firstPiecePriority &&file.getNbPieces() >FIRST_PIECE_MIN_NB)
					{
						if (pieceNumber ==file.getFirstPieceNumber() ||pieceNumber ==file.getLastPieceNumber())
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
			startPriorities[pieceNumber] =startPriority;
			if (startPriority >=0)
			{
				dmPiece.setNeeded();
				if (!doneAvailable[pieceNumber])
				{
					if (!hasNeededUndonePiece)
					{
						hasNeededUndonePiece =true;
						neededUndonePieceChange++;
					}
					return true;
				}
			} else
				dmPiece.clearNeeded();
		} catch (Throwable e)
		{
			Debug.printStackTrace(e);
		}
		return false;
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
		return false;
//		return endGameMode;
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
				// Pieces already downloaded are of no interest
				if (!dmPiece.isRequestable())
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
		if (pt ==null ||pt.getPeerState() !=PEPeer.TRANSFERING)
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
			if (peerHavePieces.flags[pieceNumber] &&dm_pieces[pieceNumber].isRequestable())
				return pieceNumber;
			return -1; // this is an odd case that maybe should be handled better, but checkers might fully handle it
		}

		final int			peerSpeed =(int) pt.getStats().getDataReceiveRate() /1000;	// how many KB/s has the peer has been sending
		final PEPeerControl	pc =pt.getControl();
		final int			nbSeeds =pc.getNbSeeds();
		final int			nbPeers =pc.getNbPeers();
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
		
		final int[]			availability =availability_cow;

		// Dont seek rarest this time around under a few circumstances, so that other factors work better
		// never seek rarest when bootstrapping torrent
		boolean				rarestOverride =nbPiecesDone <4;
		if (!rarestOverride &&nbRarestActive >=nbSeeds &&globalMinOthers >1)
		{
			// if already getting some rarest, dont get more if swarm is healthy or too many pieces running
			rarestOverride =globalMinOthers >(1 +globalMin)
				||(globalMinOthers >=(2 *nbSeeds) &&(2 *globalMinOthers) >=nbPeers)
				||nbRarestActive >=(nbSeeds +nbPeers);
			// Interest in Rarest pieces (compared to user priority settings) could be influenced by several factors;
			// less demand closer to 0% and 100% of the torrent/farther from 50% of the torrent
			// less demand closer to 0% and 100% of peers interestd in us/farther from 50% of peers interested in us
			// less demand the more pieces are in progress (compared to swarm size)
			// less demand the farther ahead from absolute global minimum we're at already
			// less demand the healthier a swarm is (rarity compared to # seeds and # peers)
		}

		final int	startI =preQualifiedPieces.start >peerHavePieces.start ?preQualifiedPieces.start :peerHavePieces.start;
		final int 	endI =preQualifiedPieces.end <peerHavePieces.end ?preQualifiedPieces.end :peerHavePieces.end;
		int 		i;

		pePieces =pc.getPieces();
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
					// priority for rarity changes too often to be done in event-driven file-priority calcs

					// now that we know avail & piece together, boost priority for rarity
					priority +=(PRIORITY_W_RARE +nbPeers -nbSeeds) /avail;
					// Boost priority even a little more if it's a globally rarest piece
					if (!rarestOverride &&avail <=globalMinOthers)
						priority +=PRIORITY_W_RAREST /avail;

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

						if ((avail <=globalMinOthers &&(pieceNumber ==-1 ||(!rarestOverride &&!resumeIsRarest)))
							||(priority >resumeMaxPriority &&(!resumeIsRarest ||rarestOverride ||avail <=globalMinOthers)))
						{ // this piece seems like best choice for resuming
							// Make sure it's possible to get a block to request from this piece
							if (pePiece.hasUnrequestedBlock())
							{ // change the different variables to reflect interest in this block
								pieceNumber =i;
								resumeMaxPriority =priority;
								resumeIsRarest =avail <=globalMinOthers; // only going to try to resume one
							} else
							{ // this piece can't yield free blocks to req, but is not marked as fully requested
								// probably should double check last time written and see if any requests went bad
								dmPiece.setRequested(); // mark it as fully requested
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
		availability_cow[pieceNumber] =avail;
		availabilityChange++;
		return avail;
	}
	
	/**
	 * An instance of this listener is registered at diskManager.
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
			boolean foundPieceToDownload =false;
			
			// the first piece can span multiple files
			int i =file.getFirstPieceNumber();
			if (!doneAvailable[i])
				foundPieceToDownload |=calcStartPriority(dm_pieces[i]);
			i++;
			
			int lastPieceNumber =file.getLastPieceNumber();
			
			// intermediary pieces, if any, are all in the one file
			long fileLength =file.getLength();
			for (; i <lastPieceNumber; i++)
			{
				// don't need pieces if file is 0 length, or downloaded, or DND/Delete
				if (fileLength >0 &&file.getDownloaded() <fileLength &&!file.isSkipped())
				{
					if (!doneAvailable[i])
						foundPieceToDownload |=calcStartPriorityFile(dm_pieces[i], file);
				} else	// need to be sure every not needed piece is marked as such
					dm_pieces[i].clearNeeded();
			}
			
			// maybe the last piece is the same as the first (or maybe it's already done)
			if (i ==lastPieceNumber &&!doneAvailable[i])
				foundPieceToDownload |=calcStartPriority(dm_pieces[i]);
			
			// if we found a piece to download and didn't have one before, need to checkInterested on peers
			if (foundPieceToDownload)
			{
				if (!hasNeededUndonePiece)
				{
					hasNeededUndonePiece =true;
					neededUndonePieceChange++;
				}
			} else	//if didn't find a piece, need to look through whole torrent to know what we should do
				checkDownloadablePiece();
		}

		public void pieceDoneChanged(DiskManagerPiece dmPiece)
		{
			int pieceNumber =dmPiece.getPieceNumber();
			if (dmPiece.isDone())
			{
				if (!doneAvailable[pieceNumber])
				{
					doneAvailable[pieceNumber] =true;
					availability_cow[pieceNumber]++;
					availabilityChange++;
				}
			} else if (doneAvailable[pieceNumber])
			{
				doneAvailable[pieceNumber] =false;
				availability_cow[pieceNumber]--;
				availabilityChange++;
				if (calcStartPriority(dmPiece))
				{
					if (!hasNeededUndonePiece)
					{
						hasNeededUndonePiece =true;
						neededUndonePieceChange++;
					}
				}
			}

		}

		public void fileAccessModeChanged(DiskManagerFileInfo file, int old_mode, int new_mode)
		{
			//file done (write to read)
			//starting to upload from the file (read to write)
		}
	}

}
