/*
 * Created on 02-Nov-2004
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.core.diskmanager.cache.impl;

/**
 * @author parg
 *
 */

import java.io.File;

import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.diskmanager.cache.*;
import com.aelitis.azureus.core.diskmanager.file.FMFile;
import com.aelitis.azureus.core.diskmanager.file.FMFileManagerException;

public class 
CacheFileWithoutCache
	implements CacheFile
{
	protected CacheFileManagerImpl		manager;
	protected FMFile					file;
	protected TOTorrentFile				torrent_file;

	protected
	CacheFileWithoutCache(
		CacheFileManagerImpl	_manager,
		FMFile					_file,
		TOTorrentFile			_torrent_file )
	{
		manager			= _manager;
		file			= _file;
		torrent_file	= _torrent_file;
		// System.out.println( "without cache = " + file.getFile().toString());
	}
	
	public File
	getFile()
	{
		return( file.getFile());
	}

	public void
	moveFile(
		File		new_file )
	
		throws CacheFileManagerException
	{
		try{
			file.moveFile( new_file );
			
		}catch( FMFileManagerException e ){
			
			manager.rethrow(e);
		}	
	}
	
	public void
	setAccessMode(
		int		mode )
	
		throws CacheFileManagerException
	{
		try{
			
			file.setAccessMode( mode==CF_READ?FMFile.FM_READ:FMFile.FM_WRITE );
			
		}catch( FMFileManagerException e ){
			
			manager.rethrow(e);
		}	
	}
	
	public int
	getAccessMode()
	{
		return( file.getAccessMode()==FMFile.FM_READ?CF_READ:CF_WRITE );
	}
	
	public void
	ensureOpen()

		throws CacheFileManagerException
	{
		try{
			
			file.ensureOpen();
			
		}catch( FMFileManagerException e ){
			
			manager.rethrow(e);
		}	
	}

	public long
	getLength()
	
		throws CacheFileManagerException
	{
		try{
						
			return( file.getSize());
			
		}catch( FMFileManagerException e ){
			
			manager.rethrow(e);
			
			return( 0 );
		}
	}
	
	public void
	setLength(
		long		length )
	
		throws CacheFileManagerException
	{
		try{
						
			file.setLength( length );
			
		}catch( FMFileManagerException e ){
			
			manager.rethrow(e);
		}
	}
	
	public void
	read(
		DirectByteBuffer	buffer,
		long				position )
	
		throws CacheFileManagerException
	{
		int	file_buffer_position	= buffer.position(DirectByteBuffer.SS_CACHE);
		int	file_buffer_limit		= buffer.limit(DirectByteBuffer.SS_CACHE);
		
		int	read_length	= file_buffer_limit - file_buffer_position;

		try{			
			file.read( buffer, position );
			
			manager.fileBytesRead( read_length );

		}catch( FMFileManagerException e ){
				
			manager.rethrow(e);
		}
	}
		
	public void
	write(
		DirectByteBuffer	buffer,
		long				position )
	
		throws CacheFileManagerException
	{
		int	file_buffer_position	= buffer.position(DirectByteBuffer.SS_CACHE);
		int file_buffer_limit		= buffer.limit(DirectByteBuffer.SS_CACHE);
		
		int	write_length = file_buffer_limit - file_buffer_position;
		
		try{			
			file.write( buffer, position );
			
			manager.fileBytesWritten( write_length );

		}catch( FMFileManagerException e ){
				
			manager.rethrow(e);
		}
	}
	
	public void
	writeAndHandoverBuffer(
		DirectByteBuffer	buffer,
		long				position )
	
		throws CacheFileManagerException
	{
		int	file_buffer_position	= buffer.position(DirectByteBuffer.SS_CACHE);
		int file_buffer_limit		= buffer.limit(DirectByteBuffer.SS_CACHE);
		
		int	write_length = file_buffer_limit - file_buffer_position;
		
		boolean	write_ok	= false;
		
		try{			
			file.write( buffer, position );
			
			manager.fileBytesWritten( write_length );

			write_ok	= true;
			
		}catch( FMFileManagerException e ){
				
			manager.rethrow(e);
			
		}finally{
			
			if ( write_ok ){
				
				buffer.returnToPool();
			}
		}
	}
	
	public void
	flushCache()
	
		throws CacheFileManagerException
	{
	}
	
	public void
	clearCache()
	
		throws CacheFileManagerException
	{
	}
	
	public void
	close()
	
		throws CacheFileManagerException
	{
		try{
			
			file.close();
			
		}catch( FMFileManagerException e ){
			
			manager.rethrow(e);			
		}
	}
	
	public void
	setFileOffset(
		int		_file_offset )
	{
	}
}
