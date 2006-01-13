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

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.disk.impl.*;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.core3.peer.impl.*;
import org.gudy.azureus2.core3.peer.impl.control.EndGameModeChunk;
import org.gudy.azureus2.core3.peer.impl.control.PEPeerControlImpl;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.peermanager.piecepicker.PiecePicker;
import com.aelitis.azureus.core.peermanager.piecepicker.util.BitFlags;

/**
 * @author MjrTom
 * 
 */

public class PiecePickerImpl
	implements PiecePicker
{
	// The following are added to the base User setting based priorities (all inspected pieces)
	private static final long	PRIORITY_W_FILE			=1010;			//user sets file as "High"
	private static final long	PRIORITY_W_FIRSTLAST	=1009;			//user select prioritize 1st/last
	private static final long	FIRST_PIECE_MIN_SIZE	=8 *1024 *1024;	// min file size for first/last priority
//	private static final long PRIORITY_W_RARE =249; //rarity vs user settings
	// bias for globally rarest piece
//	private static final long PRIORITY_W_RAREST =126;

	// The following are only used when resuming already running pieces
	// (or maybe all pieces, depending on current code dev goals)
	private static final long	PRIORITY_W_PIECE_DONE	=1001;	// finish pieces already almost done
	private static final long	PRIORITY_W_SAME_PIECE	=334;	// keep working on same piece
	private static final long	PRIORITY_W_AGE			=1001;	// priority boost due to being too old
	private static final long	PRIORITY_DW_AGE			=20 *1000 *3;	// ms a block is expected to complete in, with leeway factor
	private static final long	PRIORITY_DW_STALE		=120 *1000;	// ms since last write

	// min time before starting a new piece when dont need to
//	private static final long TIME_START_NEW =1995;

	protected static boolean		firstPiecePriority	=COConfigurationManager.getBooleanParameter("Prioritize First Piece", false);
//	protected static boolean		completionPriority	=COConfigurationManager.getBooleanParameter("Prioritize Most Completed Files", false);

	protected DiskManagerHelper		disk_mgr;
	private MyDiskManagerListener	myDiskManListener;
	protected int					nbPieces;
	protected DiskManagerPiece[]	dm_pieces;

	protected boolean		hasNeededUndonePiece =true;
	protected boolean		hasNeededUndonePieceChange;

	private AEMonitor		endGameModeChunks_mon	= new AEMonitor( "PiecePicker:EG");
	//The list of chunks needing to be downloaded (the mechanism change when entering end-game mode)
	private List 			endGameModeChunks;

	private int				globalMinOthers;		//MAX(floor(Global Minimum Availablity),1)
	private int				globalMin;				//floor(Global Minimum Availablity)
	private int				rarestNbActive;
	
	private long[]			startPriorities;

	static{    	
		ParameterListener param_listener = new ParameterListener()
		{
			public void parameterChanged(String parameterName)
			{
				firstPiecePriority =COConfigurationManager.getBooleanParameter("Prioritize First Piece", false);
//			    completionPriority =COConfigurationManager.getBooleanParameter("Prioritize Most Completed Files", false);
		    }

		};

//		COConfigurationManager.addParameterListener("Prioritize Most Completed Files", this);
		COConfigurationManager.addAndFireParameterListener("Prioritize First Piece", param_listener);
	}
	   
	public PiecePickerImpl(DiskManagerHelper dm)
	{
		disk_mgr =dm;

	}

	public void start()
	{
		nbPieces =disk_mgr.getNbPieces();
		dm_pieces =disk_mgr.getPieces();
		startPriorities =new long[nbPieces];

		myDiskManListener =new MyDiskManagerListener();
		disk_mgr.addListener(myDiskManListener);

		// initialize the true Needed state for each piece
		// on going changes will use event driven tracking
		for (int i =0; i <nbPieces; i++)
			dm_pieces[i].calcNeeded();

		checkDownloadablePiece();
	}

	public void stop()
	{
		hasNeededUndonePiece =false;
		disk_mgr.removeListener(myDiskManListener);
		myDiskManListener =null;
		startPriorities =null;
//		COConfigurationManager.removeParameterListener("Prioritize First Piece", this);
//		COConfigurationManager.removeParameterListener("Prioritize Most Completed Files", this);
	}

	/**
	 * An instance of this listener is registered at disk_mgr.
	 * 
	 * @author Balazs Poka
	 * @author MjrTom
	 */
	private class MyDiskManagerListener
		implements DiskManagerListener
	{
		public void stateChanged(int oldState, int newState)
		{
			//starting torrent
			if (newState ==DiskManager.READY)
			{
			}
		}

		public void filePriorityChanged(DiskManagerFileInfo file)
		{
			boolean foundPieceToDownload =false;
			// the first piece can span multiple files
			int i =file.getFirstPieceNumber();
			if (!dm_pieces[i].isDone())
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
					if (!dm_pieces[i].isDone())
						foundPieceToDownload |=calcStartPriorityFile(dm_pieces[i], file);
				} else	// but need ot be sure every not needed piece is marked as such
					dm_pieces[i].clearNeeded();
			}
			// maybe the last piece is the same as the first (or maybe it's already done)
			if (i ==lastPieceNumber &&!dm_pieces[i].isDone())
				foundPieceToDownload |=calcStartPriority(dm_pieces[i]);
			// if we found a piece to download and didn't have one before, need to checkInterested on peers
			if (foundPieceToDownload)
			{
				if (!hasNeededUndonePiece)
				{
					hasNeededUndonePieceChange =true;
					hasNeededUndonePiece =true;
				}
			} else	//if didn't find a piece, need to look through whole torrent to know what we should do
				checkDownloadablePiece();
		}

		public void pieceDoneChanged(DiskManagerPiece dmPiece)
		{
			boolean foundPieceToDownload =calcStartPriority(dmPiece);
			if (foundPieceToDownload)
			{
				if (!hasNeededUndonePiece)
				{
					hasNeededUndonePieceChange =true;
					hasNeededUndonePiece =true;
				}
			}
		}

		public void fileAccessModeChanged(DiskManagerFileInfo file, int old_mode, int new_mode)
		{
			//file done (write to read)
			//starting to upload from the file (read to write)
		}
	}

	/**
	 * Early-outs if finds a downloadable piece
	 * Either way sets hasNeededUndonePiece and hasNeededUndonePiece if necessary 
	 */
	protected void checkDownloadablePiece()
	{
		for (int i =0; i <nbPieces; i++)
		{
			if (dm_pieces[i].calcNeeded() &&!dm_pieces[i].isDone())
			{
				if (!hasNeededUndonePiece)
				{
					hasNeededUndonePiece =true;
					hasNeededUndonePieceChange =true;
				}
				return;
			}
		}
		if (hasNeededUndonePiece)
		{
			hasNeededUndonePiece =false;
			hasNeededUndonePieceChange =true;
		}
	}

	public void setGlobalMinOthers(int i)
	{
		globalMinOthers =i;
	}
	
	public void setGlobalMin(int i)
	{
		globalMin =i;
	}
	
	public void setRarestRunning(int i)
	{
		rarestNbActive =i;
	}
	
	public DiskManager getManager()
	{
		return disk_mgr;
	}

	/**
	 * calculates a piece's start priority when the piece is in only one single file
	 * @param DiskManagerPiece		dmPiece	the piece to check
	 * @param DiskManagerFileInfo	file	the one single file the piece is in
	 */
	protected boolean calcStartPriorityFile(DiskManagerPiece dmPiece, DiskManagerFileInfo file)
	{
		long startPriority =Long.MIN_VALUE;
		long fileLength =file.getLength();
		if (fileLength >0 &&file.getDownloaded() <fileLength &&!file.isSkipped())
		{
			startPriority =0;
			// user option "prioritize first and last piece"
			// TODO: should prioritize ~10% to ~%25 from edges of file
			if (firstPiecePriority &&fileLength >FIRST_PIECE_MIN_SIZE)
			{
				int pieceNumber =dmPiece.getPieceNumber();
				if (pieceNumber ==file.getFirstPieceNumber() ||pieceNumber ==file.getLastPieceNumber())
				{
					startPriority +=PRIORITY_W_FIRSTLAST;
				}
			}
			// if the file is high-priority
			// startPriority +=(1000 *fileInfo.getPriority()) /255;
			if (file.isPriority())
			{
				startPriority +=PRIORITY_W_FILE;
				// TODO: should allow further prioritizing High priority files based on completion and % of torrent size
				// user option "prioritize more completed files"
				// if (completionPriority)
				// {
				// startPriority +=((1000 *fileInfo.getLength()) /disk_mgr.getTotalLength()) *fileInfo.getDownloaded() /fileInfo.getLength();
				// }
			}
		}
		startPriorities[dmPiece.getPieceNumber()] =startPriority;
		if (startPriority >=0)
		{
			dmPiece.setNeeded();
			if (!dmPiece.isDone())
			{
				if (!hasNeededUndonePiece)
				{
					hasNeededUndonePieceChange =true;
					hasNeededUndonePiece =true;
				}
				return true;
			}
		} else
			dmPiece.clearNeeded();
		return false;
	}

	protected boolean calcStartPriority(DiskManagerPiece dmPiece)
	{
		long startPriority =Long.MIN_VALUE;
		long filePriority =Long.MIN_VALUE;
		List filesInfo =dmPiece.getFiles();
		for (int i =0; i <filesInfo.size(); i++)
		{
			DiskManagerFileInfoImpl file =(DiskManagerFileInfoImpl)filesInfo.get(i);
			long fileLength =file.getLength();
			if (fileLength >0 &&file.getDownloaded() <fileLength &&!file.isSkipped())
			{
				filePriority =0;
				// user option "prioritize first and last piece"
				// TODO: should prioritize ~10% to ~%25 from edges of file
				if (firstPiecePriority &&fileLength >FIRST_PIECE_MIN_SIZE)
				{
					int pieceNumber =dmPiece.getPieceNumber();
					if (pieceNumber ==file.getFirstPieceNumber() ||pieceNumber ==file.getLastPieceNumber())
					{
						filePriority +=PRIORITY_W_FIRSTLAST;
					}
				}
				// if the file is high-priority
				// startPriority +=(1000 *fileInfo.getPriority()) /255;
				if (file.isPriority())
				{
					filePriority +=PRIORITY_W_FILE;
					// TODO: should allow further prioritizing High priority files based on completion and % of torrent size
					// user option "prioritize more completed files"
					// if (completionPriority)
					// {
					// startPriority +=((1000 *fileInfo.getLength()) /disk_mgr.getTotalLength()) *fileInfo.getDownloaded() /fileInfo.getLength();
					// }
				}
				if (filePriority >startPriority)
					startPriority =filePriority;
			}
		}
		startPriorities[dmPiece.getPieceNumber()] =startPriority;
		if (startPriority >=0)
		{
			dmPiece.setNeeded();
			if (!dmPiece.isDone())
			{
				if (!hasNeededUndonePiece)
				{
					hasNeededUndonePieceChange =true;
					hasNeededUndonePiece =true;
				}
				return true;
			}
		} else
			dmPiece.clearNeeded();
		return false;
	}

	public int[] getPieceToStart(PEPeerTransport pc, BitFlags startCandidates, int candidateMode)
	{
		if (startCandidates.getNbSet() <=0)
		{	// cant do anything if no pieces to startup
			return null;
		}

		int startI;
		int direction;
		
		if (startCandidates.getNbSet() ==1 ||candidateMode >0)
		{
			startI =startCandidates.getStartIndex();
			direction =1;
		} else
		{
			// Mix it up!
			startI =RandomUtils.generateRandomIntBetween(startCandidates.getStartIndex(),
					startCandidates.getEndIndex());
			direction =RandomUtils.generateRandomPlusMinus1();
		}

		PEPiece[] _pieces =pc.getControl().getPieces();
		// For every Priority piece
		for (int i =startI; i >=startCandidates.getStartIndex() &&i <=startCandidates.getEndIndex(); i +=direction)
		{
			// is piece flagged and confirmed not in progress
			if (startCandidates.get(i) &&(_pieces[i] ==null))
			{
				// This should be a piece we want to start
				return new int[]{i, -1};
			}
		}
		return null;
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
	 * @param int candidateMode >0 is sequential DL (various options will soon be implemented)
	 * @return int[] with pieceNumber, blockNumber to be requested (blockNumber might be <0,
	 *	in which case it needs to be determined)
	 *  or null if no request could be found
	 */
	public int[] getRequestCandidate(PEPeerTransport pt, int candidateMode)
	{
		if (pt ==null)
			return null;
		PEPeerControl pc =pt.getControl();

		int i;
		// piece number and its block number that we'll try to DL
		int pieceNumber =-1;
		int blockNumber =-1;
		// how many KB/s has the peer has been sending
		boolean[] piecesAvailable =pt.getAvailable();

		if (FORCE_PIECE >-1 &&FORCE_PIECE <nbPieces)
			pieceNumber =FORCE_PIECE;
		else
			pieceNumber =pt.getReservedPieceNumber();

		// If there's a piece Reserved to this peer or a FORCE_PIECE, start/resume it and only it (if possible)
		if (pieceNumber >=0)
		{
			if (piecesAvailable[pieceNumber] &&dm_pieces[pieceNumber].isInteresting() &&!dm_pieces[pieceNumber].isRequested())
				return new int[]{pieceNumber, blockNumber};
			return null; // this is an odd case that maybe should be handled better
		}

		BitFlags startCandidates =new BitFlags(nbPieces);

		int startCandidatesMinAvail =Integer.MAX_VALUE;
		long startCandidatesMaxPriority =Long.MIN_VALUE;
		boolean rarestCanStart =false;

		int resumeMinAvail =Integer.MAX_VALUE;
		long resumeMaxPriority =Long.MIN_VALUE;
		boolean rarestCanResume =false; // can the peer continue/start a piece with avail = int(global avail)
		// pieceNumber & blockNumber will be set from the scan of pieces already in progress
		// to the new piece number and blocknumber we want to try to download
		pieceNumber =-1;
		blockNumber =-1;
		int[] availability =pc.getAvailability();

		int _seeds =pc.getNbSeeds();
		int _peers =pc.getNbPeers();
		int _piecesNbr =pc.getNbActive();

		// Dont seek rarest (on this block request) under a few circumstances, so that other factors work better
		// never seek rarest if bootstrapping torrent
		boolean rarestOverride =disk_mgr.getNbPiecesDone() <4;
		if (!rarestOverride &&rarestNbActive >0 &&pc.getMinAvailability() >1)
		{
			// if already getting some rarest, dont get more if swarm is healthy or too many pieces running
			rarestOverride =globalMinOthers >(1 +globalMin)
				||(globalMinOthers >=(2 *_seeds) &&(2 *globalMinOthers) >=_peers)
				||_piecesNbr >=(_seeds +_peers);
			// Interest in Rarest pieces could be influenced by several factors;
			// less demand closer to 0% and 100% of the torrent/farther from 50% of the torrent
			// less demand closer to 0% and 100% of peers interestd in us/farther from 50% of peers interested in us
			// less demand the more pieces are in progress (compared to swarm size)
			// less demand the farther ahead from absolute global minimum we're at already
			// less demand the healthier a swarm is (rarity compared to # seeds and # peers)
		}

		int peerSpeed =(int) pc.getStats().getDataReceiveRate() /1024;
		if (peerSpeed <0)
			peerSpeed =0;
		long now =SystemTime.getCurrentTime();
		// Try to continue a piece already loaded; look for rarest and, among equally rarest; highest priority
		for (i =0; i <nbPieces; i++ )
		{
			// is the piece not completed already?
			// does this peer make this piece available?
			// is the piece not fully requested/downloaded/written/checking/done already?
			if (piecesAvailable[i] &&dm_pieces[i].isInteresting() &&!dm_pieces[i].isRequested() &&dm_pieces[i].isRequestable())
			{
				long priority =startPriorities[i];
				if (priority >=0)
				{
					int avail =availability[i];

					// is the piece loaded (therefore in progress DLing)
					PEPiece pePiece =pc.getPiece(i);
					if (pePiece !=null)
					{
						// time since last write isn't here since the main loop
						// checker should totally take care of that

						// How many requests can still be made on this piece?
						int freeReqs =pePiece.getNbUnrequested();
						if (freeReqs <=0)
						{
							dm_pieces[i].setRequested();
							continue;
						}

						// Don't touch pieces reserved for others
						if (pePiece.getReservedBy() !=null &&pt !=pePiece.getReservedBy())
							continue;

						int pieceSpeed =pePiece.getSpeed();
						// peers allowed to continue same piece as last requested from them
						if (i !=pt.getLastPiece() &&pieceSpeed >0)
						{
							// if the peer is snubbed, only request on slow pieces
							if (peerSpeed <pieceSpeed ||pt.isSnubbed())
							{
								// peer allowed free blocks is > 3 and enough to not slow piece down too much
								if (freeReqs <3 || freeReqs *peerSpeed >= pieceSpeed -1)
									continue;
							}
						}

						if (avail <=resumeMinAvail)
						{
							priority +=pieceSpeed;
							// now that we know avail & piece together, boost priority for rarity
							priority +=_seeds +_peers -avail;
							// Boost priority even a little more if it's a globally rarest piece
							if (avail <=globalMinOthers &&!rarestOverride)
								priority +=_peers;
							priority +=(i ==pt.getLastPiece()) ?PRIORITY_W_SAME_PIECE :0;
							// Adjust priority for purpose of continuing pieces
							// how long since last written to
							long staleness =now -dm_pieces[i].getLastWriteTime();
							if (staleness >0)
								priority +=staleness /PRIORITY_DW_STALE;
							// how long since piece was started
							long pieceAge =now -pePiece.getCreationTime();
							if (pieceAge >0)
								priority +=PRIORITY_W_AGE *pieceAge /(PRIORITY_DW_AGE *dm_pieces[i].getNbBlocks());
							// how much is already written to disk
							priority +=(PRIORITY_W_PIECE_DONE *dm_pieces[i].getNbWritten()) /dm_pieces[i].getNbBlocks();

							pePiece.setResumePriority(priority);

							int testAvail =avail -(int) (priority /1001);
							// fake availability a little if priority has gotten too high
							if (avail <resumeMinAvail ||priority >resumeMaxPriority
								||(testAvail <resumeMinAvail &&priority >resumeMaxPriority))
							{
								// this piece seems like best choice for resuming

								// Make sure it's possible to get a block to request from this piece
								// Returns -1 if no more blocks need to be requested
								// Or a valid blockNumnber >= 0 otherwise
								int tempBlock =pePiece.getBlock();

								// So, if there is a block to request in that piece
								if (tempBlock >=0)
								{
									// change the different variables to reflect interest in this block
									resumeMinAvail =avail;
									resumeMaxPriority =priority;
									// resume based on true numbers
									if (avail <=globalMinOthers)
										rarestCanResume =true;
									pieceNumber =i;
									blockNumber =tempBlock;
								} else
								{
									// this piece can't yield free blocks to req, but is not marked as fully requested
									// mark it as fully requested
									dm_pieces[i].setRequested();
								}
							}
						}
					} else if (!rarestCanResume ||rarestOverride ||avail <=globalMinOthers
						||(priority >startCandidatesMaxPriority &&avail <=startCandidatesMinAvail))
					{
						// Piece isn't already loaded, so not in progress; tally most interesting pieces
						// find a new piece to startup. Go for priority first, then rarity, unless going for a rarest piece
						// But only if we can't resume a rarest piece
						// Develop bitfield of Priority pieces
						if (priority ==startCandidatesMaxPriority)
						{ // this piece same as best before
							if (avail ==startCandidatesMinAvail)
							{ // this piece same as best before
								startCandidates.set(i);
							} else if (avail <startCandidatesMinAvail)
							{ // this piece better than before
								startCandidates.setOnly(i);
								startCandidatesMinAvail =avail;
								if (avail <=globalMinOthers)
									rarestCanStart =true;
							}
						} else if (priority >startCandidatesMaxPriority
							&&(rarestOverride ||!rarestCanStart ||avail <=globalMinOthers))
						{ // this piece better than before
							startCandidates.setOnly(i);
							startCandidatesMaxPriority =priority;
							startCandidatesMinAvail =avail;
							if (avail <=globalMinOthers)
								rarestCanStart =true;
						} else if (!rarestOverride &&!rarestCanResume &&avail <=globalMinOthers &&!rarestCanStart)
						{ // this piece is rarest and needed
							startCandidates.setOnly(i);
							startCandidatesMaxPriority =priority;
							startCandidatesMinAvail =avail;
							rarestCanStart =true;
						}
					}
				}
			}
		}

		// See if have found a valid (piece;block) to request from a piece in progress
		if (pieceNumber >=0 &&blockNumber >=0 &&(rarestCanResume ||!rarestCanStart ||rarestOverride))
			return new int[]{pieceNumber, blockNumber};

		// Gets to here when no block was successfully continued
		return getPieceToStart(pt, startCandidates, candidateMode); // pick piece from candidates bitfield
	}

	/**
	 * @param pc
	 *            the PeerConnection we're working on
	 * @return true if a request was assigned, false otherwise
	 */
	public boolean findPieceToDownload(PEPeerTransport pt, int candidateMode)
	{
		int[] candidateInfo =getRequestCandidate(pt, candidateMode);
		if (candidateInfo ==null)
			return false;

		int pieceNumber =candidateInfo[0];
		int blockNumber =candidateInfo[1];

		if (pieceNumber <0)
			return false;

		
		int peerSpeed =(int) pt.getStats().getDataReceiveRate() /1024;

		PEPeerControl pc =pt.getControl();
		PEPieceImpl piece =(PEPieceImpl)pc.getPiece(pieceNumber);
		if (piece ==null)
		{
			piece =new PEPieceImpl(pt.getManager(), dm_pieces[pieceNumber], (peerSpeed /2) -1, false);

			// Assign the created piece to the pieces array.
			pc.addPiece(piece, pieceNumber);
		}

		if (blockNumber <0)
			blockNumber =piece.getBlock();

		if (blockNumber <0)
			return false;

		// really try to send the request to the peer
		if (pt.request(pieceNumber, blockNumber *DiskManager.BLOCK_SIZE, piece.getBlockSize(blockNumber)))
		{
			piece.markBlock(blockNumber);
			// Up the speed on this piece?
			if (peerSpeed >piece.getSpeed())
				piece.incSpeed();
			// have requested a block
			return true;
		}
		return false;
	}

	public boolean findPieceInEndGameMode(PEPeerTransport pc)
	{
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
				if (pc.getAvailable()[pieceNumber])
				{
					PEPiece pePiece =pc.getControl().getPiece(pieceNumber);
					if (pePiece !=null)
					{
						boolean result =pc.request(pieceNumber, chunk.getOffset(), chunk.getLength());
						pePiece.markBlock(chunk.getBlockNumber());
						return result;
					}

					endGameModeChunks.remove(chunk);
					// System.out.println("End Game Mode :: Piece is null : chunk remove !!!NOT REQUESTED!!!" +
					// chunk.getPieceNumber() + ":" + chunk.getOffset() + ":" + chunk.getLength());
					return false;
				}
			}
		} finally
		{
			endGameModeChunks_mon.exit();
		}
		return false;
	}
	
	public void clearEndGameChunks()
	{
		try
		{
			endGameModeChunks_mon.enter();
			endGameModeChunks.clear();
		} finally
		{
			endGameModeChunks_mon.exit();
		}
	}

	public void addEndGameChunks(PEPieceImpl pePiece)
	{
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

	public void computeEndGameModeChunks(PEPeerControlImpl pc)
	{
		endGameModeChunks =new ArrayList();
		PEPiece[] _pieces =pc.getPieces();
		if (_pieces ==null)
			return;

		try
		{
			endGameModeChunks_mon.enter();

			for (int i =0; i <nbPieces; i++ )
			{
				// Pieces already downloaded are of no interest
				if (!dm_pieces[i].isRequestable())
					continue;
				PEPiece pePiece =_pieces[i];
				if (pePiece ==null)
					continue;

				boolean written[] =dm_pieces[i].getWritten();

				if (written ==null)
				{
					if (!dm_pieces[i].isWritten())
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

	public void removeFromEndGameModeChunks(int pieceNumber, int offset)
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
	
	public boolean hasDownloadablePiece()
	{
		return hasNeededUndonePiece;
	}

	/** Do not use this method.  It's Only for the PeerController to
	 *  determine if every peer should recheck if it's interested.
	 */
	public boolean hasDownloadableChanged()
	{
		if (!hasNeededUndonePieceChange)
			return false;
		hasNeededUndonePieceChange =false;
		return true;
	}
}
