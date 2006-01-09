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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.disk.impl.*;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.peer.impl.*;
import org.gudy.azureus2.core3.peer.impl.control.*;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.peermanager.piecepicker.PiecePicker;
import com.aelitis.azureus.core.peermanager.piecepicker.util.BitFlags;
import com.aelitis.azureus.core.util.PieceBlock;

/**
 * @author MjrTom
 * 
 */

public class PiecePickerImpl
	implements PiecePicker, ParameterListener
{
	// The following are added to the base User setting based priorities (all inspected pieces)
	private static final long	TIME_MIN_PRIORITY	=4990;			// ms
	private static final long	PRIORITY_W_FILE		=1010;			//user sets file as "High"
	private static final long	PRIORITY_W_1STLAST	=1009;			//user select prioritize 1st/last
//	private static final long PRIORITY_W_RARE =249; //rarity vs user settings
	// bias for globally rarest piece
//	private static final long PRIORITY_W_RAREST =126;

	// The following are only used when resuming already running pieces
	// (or maybe all pieces, depending on current code dev goals)
	private static final long	PRIORITY_W_PIECE_DONE	=1001;	// finish pieces already almost done
	private static final long	PRIORITY_W_SAME_PIECE	=334;	// keep working on same piece
	private static final long	PRIORITY_W_AGE			=1001;	// priority boost due to being too old
	private static final long	PRIORITY_DW_AGE			=20 *4 *1000;	// ms a block is expected to complete in, with 4x leway factor
	private static final long	PRIORITY_DW_STALE		=120 *1000;	// ms since last write

	// min time before starting a new piece when dont need to
//	private static final long TIME_START_NEW =1995;

	private static boolean		firstPiecePriority	=COConfigurationManager.getBooleanParameter("Prioritize First Piece", false);

	private DiskManagerHelper	disk_mgr;
	private MyDiskManagerListener	myDiskManListener;
	private int				_nbPieces;

	protected long			nowish =SystemTime.getCurrentTime();	// very recent time

	private boolean			has_piece_to_download =true;

	private AEMonitor		endGameModeChunks_mon	= new AEMonitor( "PiecePicker:EG");
	//The list of chunks needing to be downloaded (the mechanism change when entering end-game mode)
	private List 			endGameModeChunks;

	private int				_globalMinOthers;		//MAX(floor(Global Minimum Availablity),1)
	private int				_globalMin;				//floor(Global Minimum Availablity)
	private int				_rarestRunning;

	public PiecePickerImpl(DiskManagerHelper dm)
	{
		nowish =SystemTime.getCurrentTime();
		disk_mgr =dm;

		_nbPieces =disk_mgr.getNumberOfPieces();

		myDiskManListener =new MyDiskManagerListener();
		COConfigurationManager.addAndFireParameterListener("Prioritize First Piece", this);
	}

	public void start()
	{
		disk_mgr.addListener(myDiskManListener);

		has_piece_to_download =true;

//		for (int i =0; i <_nbPieces; i++)
//			calcStartPriority(i);
	}

	public void stop()
	{
		disk_mgr.removeListener(myDiskManListener);
		COConfigurationManager.removeParameterListener("Prioritize First Piece", this);
	}

	/**
	 * An instance of this listener is registered at disk_mgr. It updates
	 * the value returned by hasDownloadablePiece to reflect changes in
	 * file/piece priority values.
	 * 
	 * @author Balazs Poka
	 */
	private class MyDiskManagerListener
		implements DiskManagerListener
	{
		public void stateChanged(int oldState, int newState)
		{
		}

		public void filePriorityChanged(DiskManagerFileInfo file)
		{
			nowish =SystemTime.getCurrentTime();
			int startPieceNumber =file.getFirstPieceNumber();
			int endPieceNumber =file.getLastPieceNumber();
			for (int i =startPieceNumber; i <=endPieceNumber; i++)
			{
				calcStartPriority(i);
			}
		}

		public void pieceDoneChanged(DiskManagerPiece dmPiece)
		{
			calcStartPriority(dmPiece.getPieceNumber());
		}

		public void fileAccessModeChanged(DiskManagerFileInfo file, int old_mode, int new_mode)
		{
		}
	}

	public void setGlobalMinOthers(int i)
	{
		_globalMinOthers =i;
	}
	
	public void setGlobalMin(int i)
	{
		_globalMin =i;
	}
	
	public void setRarestRunning(int i)
	{
		_rarestRunning =i;
	}
	
	public boolean hasDownloadablePiece()
	{
		return has_piece_to_download;
	}
	
	public DiskManager getManager()
	{
		return disk_mgr;
	}

	public void calcStartPriority(int pieceNumber)
	{
		long startPriority =0;
		long filesPriority =Long.MIN_VALUE;
		long filePriority =Long.MIN_VALUE;
		DiskManagerPiece dmPiece =disk_mgr.getPiece(pieceNumber);
		DiskManagerFileInfo[] filesInfo =dmPiece.getFiles();
		for (int i =0; i <filesInfo.length; i++ )
		{
			DiskManagerFileInfo fileInfo =filesInfo[i];
			long fileLength =fileInfo.getLength();
			if (!fileInfo.isSkipped() &&fileLength >0 &&fileInfo.getDownloaded() <fileLength)
			{
				filePriority =0;
				// user option "prioritize first and last piece"
				// TODO: maybe should prioritize according to how far from edges of file (ie middle = no startPriority boost)
				if (firstPiecePriority)
				{
					if (pieceNumber ==fileInfo.getFirstPieceNumber() ||pieceNumber ==fileInfo.getLastPieceNumber())
					{
						filePriority +=PRIORITY_W_1STLAST;
					}
				}
				// if the file is high-priority
				// startPriority +=(1000 *fileInfo.getPriority()) /255;
				if (fileInfo.isPriority())
				{
					filePriority +=PRIORITY_W_FILE;
					// user option "prioritize more completed files"
					// if (completionPriority)
					// {
					// startPriority +=((1000 *fileInfo.getLength()) /disk_mgr.getTotalLength()) *fileInfo.getDownloaded() /fileInfo.getLength();
					// }
				}
				if (filesPriority <filePriority)
				{
					filesPriority =filePriority;
				}
			}
		}
		if (filesPriority >=0)
		{
			startPriority +=filesPriority;
			disk_mgr.getPiece(pieceNumber).setNeeded();
			has_piece_to_download =true;
		} else
		{
			startPriority =Long.MIN_VALUE;
			disk_mgr.getPiece(pieceNumber).clearNeeded();
		}
		disk_mgr.getPiece(pieceNumber).setStartPriority(startPriority);
		return;
	}

	public void parameterChanged(String parameterName)
	{
	    firstPiecePriority =COConfigurationManager.getBooleanParameter("Prioritize First Piece", false);
    }
	
	public PieceBlock getPieceToStart(PEPeerTransport pc, BitFlags startCandidates, int candidateMode)
	{
		if (startCandidates.getNbSet()<= 0)
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
		for (int i =startI; i >=startCandidates.getStartIndex()&& i<= startCandidates.getEndIndex(); i+=direction)
		{
			// is piece flagged and confirmed not in progress
			if (startCandidates.get(i)&& (_pieces[i]== null))
			{
				// This should be a piece we want to start
				return new PieceBlock(i, -1);
			}
		}
		return null;
	}

	// set FORCE_PIECE if trying to diagnose piece problems and only want to d/l a specific piece from a torrent
	private static final int	FORCE_PIECE	=-1;

	/**
	 * This method is the core download manager. It will decide for a given
	 * peer, which block it should download from it. Here is the overall
	 * algorithm : 0. If there a FORCED_PIECE or reserved piece, it will
	 * start/resume it if possible 1. Scan all the pieces in progress, and find
	 * a piece thats equally rarest (and then equally highest priority) to
	 * continue if possible 2. While scanning pieces in progress, develop list
	 * of equally highest priority pieces (with equally highest rarity) to start
	 * a new piece from 3. If it can't find any piece, this means all pieces are
	 * already downloaded/full requested, or the peer connection somehow isn't
	 * actually currently accepting new requests 4. Returns true when a
	 * succesfull request was issued 5. Returns false is a req wasn't issued for
	 * any reason
	 * 
	 * @param pc PEPeerTransport to work with
	 * @param int candidateMode >0 is sequential DL (future may have options for meaning of sequential)
	 * @return true if a request was assigned, false otherwise
	 */
	public PieceBlock getRequestCandidate(PEPeerTransport pt, int candidateMode)
	{
		nowish =SystemTime.getCurrentTime();
		if (pt ==null)
			return null;
		PEPeerControl 	pc =pt.getControl();

		DiskManagerPiece[] dm_pieces =disk_mgr.getPieces();
		int i;
		int nbPieces= _nbPieces;
		// piece number and its block number that we'll try to DL
		int pieceNumber= -1;
		int blockNumber= -1;
		// how many KB/s has the peer has been sending
		int peerSpeed= (int) pc.getStats().getDataReceiveRate()/ 1024;
		if (peerSpeed <0)
			peerSpeed= 0;
		boolean[] piecesAvailable= pt.getAvailable();
		
		if (FORCE_PIECE >-1&& FORCE_PIECE <nbPieces)
			pieceNumber= FORCE_PIECE;
		else
			pieceNumber= pt.getReservedPieceNumber();

		// If there's a piece Reserved to this peer or a FORCE_PIECE, start/resume it and only it (if possible)
		if (pieceNumber >=0)
		{
			DiskManagerPiece dmPiece =dm_pieces[pieceNumber];
			if (piecesAvailable[pieceNumber] &&dmPiece.isInteresting() &&!dmPiece.isRequested())
				return new PieceBlock(pieceNumber, blockNumber);
			return null; // this is an odd case that maybe should be handled better
		}
		
		BitFlags startCandidates= new BitFlags(nbPieces);
		
		int startCandidatesMinAvail= Integer.MAX_VALUE;
		long startCandidatesMaxPriority= Long.MIN_VALUE;
		int resumeMinAvail= Integer.MAX_VALUE;
		long resumeMaxPriority= Long.MIN_VALUE;
		// can the peer continue/start a piece with avail = int(global avail)
		boolean rarestCanResume= false;
		boolean rarestCanStart= false;
		// pieceNumber & blockNumber will be set from the scan of pieces already in progress
		// to the new piece number and blocknumber we want to try to download
		pieceNumber =-1;
		blockNumber =-1;
		int[] availability =pc.getAvailability();
		
		int _seeds =pc.getNbSeeds();
		int _peers =pc.getNbPeers();
		int _piecesNbr =pc.getNbPieces();
		
		// Dont seek rarest (on this block request) under a few circumstances, so that other factors work better
		// never seek rarest if bootstrapping torrent
		boolean RarestOverride =disk_mgr.getPiecesDone() <4;
		if (!RarestOverride && _rarestRunning >0 &&pc.getMinAvailability() >1)
		{
			// if already getting some rarest, dont get more if swarm is healthy or too many pieces running
			RarestOverride= _globalMinOthers> (1+ _globalMin)
				|| (_globalMinOthers>= (2* _seeds)&& (2* _globalMinOthers)>= _peers)
				|| _piecesNbr>= (_seeds+ _peers);
				// Interest in Rarest pieces could be influenced by several factors;
				// less demand closer to 0% and 100% of the torrent/farther from 50% of the torrent
				// less demand closer to 0% and 100% of peers interested in us/farther from 50% of peers interested in us
				// less demand the more pieces are in progress (compared to swarm size)
				// less demand the farther ahead from absolute global minimum we're at already
				// less demand the healthier a swarm is (rarity compared to # seeds and # peers)
		}
		
		// Try to continue a piece already loaded; look for rarest and, among equally rarest; highest priority
		for (i =0; i <nbPieces; i++)
		{
			// is the piece not completed already?
			// does this peer make this piece available?
			// is the piece not fully requested already?
			if (dm_pieces[i] !=null &&dm_pieces[i].isRequestable() &&piecesAvailable[i] &&!dm_pieces[i].isRequested())
			{
				DiskManagerPiece dmPiece =dm_pieces[i];
				int avail =availability[i];

				long priority =dmPiece.getStartPriority();
				if (priority >=0)
				{
					// is the piece loaded (therefore in progress DLing)
					PEPiece piece =pc.getPieces()[i];
					if (piece !=null)
					{
						// time since last write isn't here since the main loop
						// checker should totally take care of that

						// How many requests can still be made on this piece?
						int freeReqs =piece.getNbUnrequested();
						if (freeReqs <=0)
						{
							dm_pieces[i].setRequested(true);
							continue;
						}
						
						// Don't touch pieces reserved for others
						if (piece.getReservedBy() !=null &&pt !=piece.getReservedBy())
							continue;

						int pieceSpeed =piece.getSpeed();

						// peers allowed to continue same piece as before
						if (i !=pt.getLastPiece() &&pieceSpeed >0)
						{
							// if the peer is snubbed, only request on slow pieces
							if (peerSpeed <pieceSpeed ||pt.isSnubbed())
							{
								// slower peer allowed if more than 3 block and enough freeReqs left to not slow it down much
								if (freeReqs< 3|| freeReqs< (peerSpeed* pieceSpeed))
									continue;
							}
						}
						
						if (avail <=resumeMinAvail)
						{
							priority +=pieceSpeed;
							// now that we know avail & piece together, boost priority for rarity
							priority +=_seeds +_peers -avail;
							// Boost priority even a little more if it's a
							// globally rarest piece
							if (avail <=_globalMinOthers &&!RarestOverride)
							{
								priority +=_peers;
							}
							priority +=(i ==pt.getLastPiece()) ?PRIORITY_W_SAME_PIECE :0;
							// Adjust priority for purpose of continuing pieces
							// how long since last written to
							long staleness =nowish -dmPiece.getLastWriteTime();
							if (staleness >0)
								priority +=staleness /PRIORITY_DW_STALE;
							// how long since piece was started
							long pieceAge =nowish -piece.getCreationTime();
							if (pieceAge >0)
								priority +=PRIORITY_W_AGE *pieceAge /(PRIORITY_DW_AGE *dmPiece.getNbBlocks());
							// how much is already written to disk
							priority +=(PRIORITY_W_PIECE_DONE *dmPiece.getNbWritten()) /dmPiece.getNbBlocks();
							
							pc.getPieces()[i].setResumePriority(priority);
							
							int testAvail =avail -(int) (priority /1001);
							// fake availability a little if priority has gotten too high
							if (avail <resumeMinAvail ||priority >resumeMaxPriority
								|| (testAvail <resumeMinAvail &&priority >resumeMaxPriority))
							{
								// this piece seems like best choice for resuming

								// Make sure it's possible to get a block to request from this piece
								// Returns -1 if no more blocks need to be requested
								// Or a valid blockNumnber >= 0 otherwise
								int tempBlock =pc.getPieces()[i].getBlock();

								// So, if there is a block to request in that piece
								if (tempBlock >=0)
								{
									// Now we change the different variables to reflect interest in this block
									resumeMinAvail =avail;
									resumeMaxPriority =priority;
									// resume based on true numbers
									if (avail <=_globalMinOthers)
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
					} else if (!rarestCanResume ||RarestOverride || avail <=_globalMinOthers
						|| (priority >startCandidatesMaxPriority &&avail <=startCandidatesMinAvail))
					{
						// Piece isn't already loaded, so not in progress; tally most interesting pieces
						// find a new piece to startup. Go for priority first, then rarity, unless going for a rarest piece
						// But only if we can't resume a rarest piece
						// Develop bitfield of Priority pieces
						if (priority ==startCandidatesMaxPriority)
						{	 // this piece same as best before
							if (avail ==startCandidatesMinAvail)
							{	// this piece same as best before
								startCandidates.set(i);
							} else if (avail< startCandidatesMinAvail)
							{	// this piece better than before
								startCandidates.setOnly(i);
								startCandidatesMinAvail =avail;
								if (avail <=_globalMinOthers)
									rarestCanStart =true;
							}
						} else if (priority >startCandidatesMaxPriority
							&& (RarestOverride ||!rarestCanStart ||avail <=_globalMinOthers))
						{	// this piece better than before
							startCandidates.setOnly(i);
							startCandidatesMaxPriority =priority;
							startCandidatesMinAvail =avail;
							if (avail<= _globalMinOthers)
								rarestCanStart= true;
						} else if ( !RarestOverride&& !rarestCanResume
							&& avail<= _globalMinOthers&& !rarestCanStart)
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
		if (pieceNumber >=0 &&blockNumber >=0 &&(rarestCanResume ||!rarestCanStart ||RarestOverride))
		{
			return new PieceBlock(pieceNumber, blockNumber);
		}

		// Gets to here when no block was successfully continued
		return getPieceToStart(pt, startCandidates, candidateMode);	//pick piece from candidates bitfield
	}

	/**
	 * @param pc the PeerConnection we're working on
	 * @return true if a request was assigned, false otherwise
	 */
	public boolean findPieceToDownload(PEPeerTransport pc, int candidateMode)
	{
		PieceBlock candidateInfo =getRequestCandidate(pc, candidateMode);
		if (candidateInfo ==null)
			return false;

		int pieceNumber =candidateInfo.getPieceNumber();
		int blockNumber =candidateInfo.getBlockNumber();

		if (pieceNumber <0)
			return false;

		int peerSpeed =(int) pc.getStats().getDataReceiveRate() /1024;

		PEPiece[] _pieces =pc.getControl().getPieces();
		if (_pieces[pieceNumber] ==null)
		{
			PEPieceImpl piece =new PEPieceImpl(pc.getManager(), pc.getControl().getDiskManager().getPieces()[pieceNumber],
				(peerSpeed /2) -1, false);

			// Assign the created piece to the pieces array.
			pc.getControl().addPiece(piece, pieceNumber);
		}

		if (blockNumber <0)
			blockNumber =_pieces[pieceNumber].getBlock();

		if (blockNumber <0)
			return false;

		// really try to send the request to the peer
		if (pc.request(pieceNumber, blockNumber *DiskManager.BLOCK_SIZE, _pieces[pieceNumber].getBlockSize(blockNumber)))
		{
			_pieces[pieceNumber].markBlock(blockNumber);
			int pieceSpeed =_pieces[pieceNumber].getSpeed();
			// Up the speed on this piece?
			if (peerSpeed >pieceSpeed)
			{
				_pieces[pieceNumber].setSpeed(pieceSpeed +1);
			}
			// have requested a block
			return true;
		}
		return false;
	}

	public boolean findPieceInEndGameMode(PEPeerTransport pc)
	{
		//Ok, we try one, if it doesn't work, we'll try another next time
		try{
			endGameModeChunks_mon.enter();
			
			int nbChunks = endGameModeChunks.size();   
			if(nbChunks > 0) {
				int random =RandomUtils.generateRandomIntUpto(nbChunks);
				EndGameModeChunk chunk =(EndGameModeChunk)endGameModeChunks.get(random);
				int pieceNumber = chunk.getPieceNumber();
				if(pc.getAvailable()[pieceNumber]) {		      
					PEPiece pePiece =pc.getControl().getPieces()[pieceNumber];
					if(pePiece != null) {
						boolean result = pc.request(pieceNumber,chunk.getOffset(),chunk.getLength());
						pePiece.markBlock(chunk.getBlockNumber());
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
	
	public void finishEndGameMode()
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
		DiskManagerPiece[]	dm_pieces =disk_mgr.getPieces();
		PEPiece[] _pieces =pc.getPieces();
		if (_pieces ==null)
			return;

		try
		{
			endGameModeChunks_mon.enter();

			for (int i =0; i <dm_pieces.length; i++ )
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
}
