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

package com.aelitis.azureus.core.util;

/**
 * @author MjrTom
 * Base Piece methods
 *
 */

public interface Piece
{
	public static final int	PIECE_STATUS_NEEDED		=0x00000001;	//want to have the piece
//	public static final int	PIECE_STATUS_AVAIL		=0x00000002;	//piece is available from others
	public static final int	PIECE_STATUS_REQUESTED	=0x00000004;	//piece fully requested
	public static final int	PIECE_STATUS_DOWNLOADED	=0x00000010;	//piece fully downloaded
	public static final int	PIECE_STATUS_WRITTEN	=0x00000020;	//piece fully written to storage
	public static final int	PIECE_STATUS_CHECKING	=0x00000040;	//piece is being hash checked
	public static final int	PIECE_STATUS_DONE		=0x00000080;	//everything completed - piece 100%

	public static final int	PIECE_STATUS_NEEDED_DONE=0x00000081;

	public static final int	PIECE_STATUS_REQUESTABLE=0x000000F5;    // Needed IS once again included in this

    public static final int PIECE_STATUS_EGM_ACTIVE =0x00000005;    //requested and needed
    public static final int PIECE_STATUS_EGM_IGNORED=0x000000F1;    //EGM ignores these pieces

    public int          getPieceNumber();
    public int          getNbBlocks();
    /**
     * @return int the number of bytes in the piece
     */
    public int          getLength();
    public int          getBlockSize(int blockNumber);

	public boolean		isChecking();

	public boolean[]	getWritten();

	public void			reDownloadBlock(int blockNumber);
	public void			reset();
	
	public void			setRequestable();

}
