/*
 * Created on 03-Aug-2004
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

package org.gudy.azureus2.core3.disk.cache.impl;

/**
 * @author parg
 *
 */

import java.io.File;

import org.gudy.azureus2.core3.disk.cache.*;
import org.gudy.azureus2.core3.disk.file.*;
import org.gudy.azureus2.core3.util.DirectByteBuffer;

public class 
CacheFileImpl 
	implements CacheFile
{
	protected CacheFileManagerImpl		manager;
	protected FMFile					file;
	
	protected
	CacheFileImpl(
		CacheFileManagerImpl	_manager,
		FMFile					_file )
	{
		manager		= _manager;
		file		= _file;
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
	getSize()
	
		throws CacheFileManagerException
	{
		try{
			
			return( file.getSize());
			
		}catch( FMFileManagerException e ){
			
			manager.rethrow(e);
			
			return( 0 );
		}
	}
	
	public long
	getLength()
		
		throws CacheFileManagerException
	{
		try{
			
			return( file.getLength());
			
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
			try{
				
				file.read( buffer, position );
				
			}catch( FMFileManagerException e ){
				
				manager.rethrow(e);
			}
		}
		
	
	
	public int
	write(
		DirectByteBuffer	buffer,
		long				position )
	
		throws CacheFileManagerException
	{
		try{
			
			return( file.write( buffer, position ));
			
		}catch( FMFileManagerException e ){
			
			manager.rethrow(e);
			
			return( 0 );
		}
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
}
