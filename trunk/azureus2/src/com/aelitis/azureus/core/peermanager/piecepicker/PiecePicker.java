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

import org.gudy.azureus2.core3.peer.impl.PEPeerTransport;
import org.gudy.azureus2.core3.peer.impl.PEPieceImpl;
import org.gudy.azureus2.core3.peer.impl.control.PEPeerControlImpl;

import com.aelitis.azureus.core.peermanager.piecepicker.util.BitFlags;
import com.aelitis.azureus.core.util.PieceBlock;

/**
 * @author MjrTom
 *
 */

public interface PiecePicker
{
	public void		start();
	public void		stop();
	
	public			PieceBlock getRequestCandidate(PEPeerTransport pc, int candidateMode);
	public			PieceBlock getPieceToStart(PEPeerTransport pc, BitFlags candidates, int candidateMode);
	public boolean	findPieceToDownload(PEPeerTransport pc, int candidateMode);
	
	public boolean	findPieceInEndGameMode(PEPeerTransport pc);
	public void		finishEndGameMode();
	public void		addEndGameChunks(PEPieceImpl pePiece);
	public void		removeFromEndGameModeChunks(int pieceNumber, int offset);
	public void		computeEndGameModeChunks(PEPeerControlImpl pc);
	
	public boolean	hasDownloadablePiece();

	public void		setGlobalMinOthers(int i);
	public void		setGlobalMin(int i);
	public void		setRarestRunning(int i);

	public long		getStartPriority(int pieceNumber);
}
