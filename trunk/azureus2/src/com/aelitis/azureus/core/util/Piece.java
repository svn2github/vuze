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

package com.aelitis.azureus.core.util;

/**
 * @author MjrTom
 * Base Piece methods
 *
 */

public interface Piece
{
	public static final int	PIECE_STATUS_NEEDED		=1;		//want to have the piece
	public static final int	PIECE_STATUS_AVAIL		=1 <<1;	//piece is available from others
	public static final int	PIECE_STATUS_REQUESTED	=1 <<2;	//piece fully requested
	public static final int	PIECE_STATUS_DOWNLOADED	=1 <<3;	//piece fully downloaded
	public static final int	PIECE_STATUS_WRITTEN	=1 <<4;	//piece fully written to storage
	public static final int	PIECE_STATUS_CHECKING	=1 <<5;	//piece is being hash checked
	public static final int	PIECE_STATUS_DONE		=1 <<7;	//everything completed - piece 100%

	public static final int	PIECE_STATUS_REQUESTABLE =
		(PIECE_STATUS_DOWNLOADED |PIECE_STATUS_WRITTEN |PIECE_STATUS_CHECKING |PIECE_STATUS_DONE);

	public void			clearChecking();

	/**
	 * @return int the number of bytes in the piece
	 */
	public int			getLength();
	public int			getNbBlocks();
	public int			getPieceNumber();

	public boolean		isWritten();					//TODO: double check usage of this
	public int			getNbWritten();
	public boolean[]	getWritten();

	public void			reDownloadBlock(int blockNumber);
	public void			reset();
	
	public boolean		isRequestable();
	public void			setRequestable();

}
