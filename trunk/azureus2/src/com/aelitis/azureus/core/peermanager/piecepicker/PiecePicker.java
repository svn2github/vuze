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

package com.aelitis.azureus.core.peermanager.piecepicker;

import org.gudy.azureus2.core3.peer.PEPiece;


/**
 * @author MjrTom
 *
 */

public interface PiecePicker
{
//	public void				start();
//	public void				stop();
//	public DiskManager		getDiskManager();
//	public void 			setPeerControl(final PEPeerControl pc);
//	public PEPeerControl	getPeerControl();
	
	public void	updateAvailability();
	
	public boolean	checkDownloadPossible();
	/** @return int the piece number that should be started, according to selection criteria
	 * 
	 * @param pt PEPeer the piece would be started for
	 * @param startCandidates BitFlags of potential candidates to choose from
	 * @return int the piece number that was chosen to be started
	 */
	//public int		getPieceToStart(final PEPeerTransport pt, final BitFlags startCandidates);

//	public boolean	findPiece(final PEPeerTransport pt);

	public boolean	isInEndGameMode();
	public void		clearEndGameChunks();
	/** adds all blocks in the piece to endGameModeChunks
	 * @param pePiece
	 */
	public void		addEndGameChunks(final PEPiece pePiece);
	/** adds only the blocks that aren't downloaded or written to endGameModeChunks
	 * @param pePiece
	 */
	public void 	addEndGameBlocks(final PEPiece pePiece);
	public void		removeFromEndGameModeChunks(final int pieceNumber, final int offset);
	
	public boolean	hasDownloadablePiece();

	/** @return long value indicated serial number of current count of changes
	 * to hasNeededUndonePiece.
	 * A method interesting in tracking changes can compare this with a locally stored
	 * value to determine if the hasNeededUndonePiece status has changed since the last check. 
	 */
	public long		getNeededUndonePieceChange();
	
	public void		addHavePiece(final int pieceNumber);
//	public void		addBitfield(final BitFlags peerHasPieces);
	/**
	 * Takes away the given pieces from availability
	 * @param peerHasPieces
	 */
//	public void		removeBitfield(final BitFlags peerHasPieces);
	
	/**
	 * Currently unused.
	 * This methd will compute the pieces' overall availability (including ourself)
	 * and the _globalMinOthers & _globalAvail
	 */
//	protected int	calcAvailability(final int pieceNumber);
//	protected void	computeAvailability();
	public int[]	getAvailability();
	public int		getAvailability(final int pieceNumber);
	
	public float	getMinAvailability();
	public float	getAvgAvail();
}
