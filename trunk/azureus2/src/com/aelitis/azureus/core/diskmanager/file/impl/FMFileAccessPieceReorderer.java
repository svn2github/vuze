/*
 * Created on 28-Sep-2005
 * Created by Paul Gardner
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

package com.aelitis.azureus.core.diskmanager.file.impl;

import java.io.File;
import java.io.RandomAccessFile;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.diskmanager.file.FMFile;
import com.aelitis.azureus.core.diskmanager.file.FMFileManagerException;

public class 
FMFileAccessPieceReorderer
	implements FMFileAccess
{
/*
 * Idea is to grow the file as needed on a piece-write basis
 * 
 * Each file in general starts with a part of a piece and then is optionally
 * followed by zero or more complete pieces and ends with an option part of a piece.
 * 
 * The first part-piece of the file is always stored in position.
 * 
 * Whenever we receive a write request we calculate which piece number(s) it affects
 * If we have already allocated piece sized chunks for the pieces then we simply write
 * to the relevant part of the file
 * If we haven't then we allocate new piece size chunks at file end and record their position in 
 * the control file. If it now turns out that we have allocated the space required for a piece previously
 * completed then we copy that piece data into the new block and reuse the space it has been
 * copied from for the new chunk
 * 
 * When allocating space for the last part-piece we allocate an entire piece sized chunk and
 * trim later
 * 
 * Whenever a piece is marked as complete we look up its location. If the required piece
 * of the file has already been allocated (and its not alread in the right place) then
 * we swap the piece data at that location with the current piece's. If the file chunk hasn't
 * been allocated yet then we leave the piece where it is - it'll be moved later.
 * 
 * If the control file is lost then there is an opportunity to recover completed pieces by 
 * hashing all of the allocated chunks and checking the SHA1 results with the file's piece hashes.
 * However, this would require the addition of further interfaces etc to integrate somehow with
 * the existing force-recheck functionality...
 * 
 * Obviously the setLength/getLength calls just have to be consistent, they don't actually
 * modify the length of the physical file
 * 
 * Conversion between storage formats is another possibility to consider - conversion from this
 * to linear can fairly easily be done here as it just needs pieces to be written to their 
 * correct locations. Conversion to this format can't be done here as we don't know which
 * pieces and blocks contain valid data. I guess such details could be added to the 
 * setStorageType call as a further parameter
 */
	
	protected
	FMFileAccessPieceReorderer(
		TOTorrentFile	_torrent_file,
		File			_control_file_dir,
		String			_control_file_name,
		FMFileAccess	_delegate )
	
		throws FMFileManagerException
	{

	}
	
	public long
	getLength(
		RandomAccessFile		raf )
	
		throws FMFileManagerException
	{
		throw( new FMFileManagerException( "not implemented" ));
	}
	
	public void
	setLength(
		RandomAccessFile		raf,
		long					length )
	
		throws FMFileManagerException
	{
		throw( new FMFileManagerException( "not implemented" ));
	}
	

	public void
	read(
		RandomAccessFile		raf,
		DirectByteBuffer[]		buffers,
		long					position )
	
		throws FMFileManagerException
	{		
		throw( new FMFileManagerException( "not implemented" ));
	}
	
	protected void
	write(
		RandomAccessFile	raf,
		DirectByteBuffer	buffer,
		long				position )
	
		throws FMFileManagerException
	{
		throw( new FMFileManagerException( "not implemented" ));
	}
	
	
	public void
	write(
		RandomAccessFile		raf,
		DirectByteBuffer[]		buffers,
		long					position )
	
		throws FMFileManagerException
	{		
		throw( new FMFileManagerException( "not implemented" ));
	}
	
	public void
	flush()
	
		throws FMFileManagerException
	{
		throw( new FMFileManagerException( "not implemented" ));
	}
	
	public void
	setPieceComplete(
		RandomAccessFile	raf,
		int					piece_number,
		DirectByteBuffer	piece_data )
	
		throws FMFileManagerException
	{	
		throw( new FMFileManagerException( "not implemented" ));
	}

	public String
	getString()
	{
		return( "reorderer" );
	}
}