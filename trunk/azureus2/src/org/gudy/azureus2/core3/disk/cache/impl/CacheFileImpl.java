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
	
	protected String
	getName()
	{
		return( file.getFile().toString());
	}
	
	protected FMFile
	getFMFile()
	{
		return( file );
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
			manager.flushCache( this );
			
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
			if ( getAccessMode() != mode ){
				
				manager.flushCache( this );
			}
			
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
				// no cache flush required here
			
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
			
				// not sure of the difference between "size" and "length" here. Old code
				// used to use "size" so I'm going to carry on for the moment in case
				// there is some weirdness here
			
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
			
				// flush in case length change will invalidate cache data (unlikely but possible) 
			
			manager.flushCache( this );
			
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
			manager.readCache( this, buffer, position );
			
			file.read( buffer, position );
				
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
		manager.writeCache( this, buffer, position, false );
	}
	
	public void
	writeAndHandoverBuffer(
		DirectByteBuffer	buffer,
		long				position )
	
		throws CacheFileManagerException
	{
		manager.writeCache( this, buffer, position, true );
	}
	
	public void
	flushCache()
	
		throws CacheFileManagerException
	{
		manager.flushCache( this );
	}
	
	public void
	close()
	
		throws CacheFileManagerException
	{
		try{
			manager.flushCache( this );
			
			file.close();
			
		}catch( FMFileManagerException e ){
			
			manager.rethrow(e);
		}
	}
}
