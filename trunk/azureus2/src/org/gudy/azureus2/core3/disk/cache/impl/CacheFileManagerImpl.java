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
CacheFileManagerImpl 
	implements CacheFileManager
{
	protected static boolean	cache_enabled	= false;
	
	protected static CacheFileManagerImpl			singleton = new CacheFileManagerImpl();
	
	protected FMFileManager		file_manager;
	
	public static CacheFileManager
	getSingleton()
	{
		return( singleton );
	}
	
	protected
	CacheFileManagerImpl()
	{
		file_manager	= FMFileManagerFactory.getSingleton();
	}
	
	public CacheFile
	createFile(
		final CacheFileOwner	owner,
		File					file )
	
		throws CacheFileManagerException
	{
		try{
			FMFile	fm_file	= 
				file_manager.createFile(
					new FMFileOwner()
					{
						public String
						getName()
						{
							return( owner.getName());
						}
					}, file );
				
			return( new CacheFileImpl( this, fm_file ));
			
		}catch( FMFileManagerException e ){
			
			rethrow( e );
			
			return( null );
		}
	}
	
	
	
	protected void
	readCache(
		CacheFileImpl		file,
		DirectByteBuffer	buffer,
		long				position )
	
		throws CacheFileManagerException
	{
		if ( cache_enabled ){
			
			long	read_length	= buffer.limit() - buffer.position();
		
			System.out.println( "readCache: " + file.getName() + ", pos = " + position + ", length = " + read_length);
			
			// TODO: read from cache!!!!
			
		}else{
			
			try{			
				file.getFMFile().read( buffer, position );
					
			}catch( FMFileManagerException e ){
					
				rethrow(e);
			}
		}
	}
	
	protected void
	writeCache(
		CacheFileImpl		file,
		DirectByteBuffer	buffer,
		long				position,
		boolean				buffer_handed_over )
	
		throws CacheFileManagerException
	{
		boolean	buffer_cached	= false;
		
		try{
			long	write_length = buffer.limit() - buffer.position();
			
			if ( cache_enabled ){
				
				synchronized( this ){
					
					System.out.println( "writeCache: " + file.getName() + ", pos = " + position + ", length = " + write_length + ", bho = " + buffer_handed_over );
				
						// if we are overwriting stuff already in the cache then force-write overlapped
						// data (easiest solution as this should only occur on hash-fails
					
					flushCache( file, position, write_length );
				
					if ( buffer_handed_over ){
						
							// cache this write
		
						// TODO: cache the write!!!!
						
						buffer_cached	= true;
						
					}else{
						
						long	actual_size = file.getFMFile().write( buffer, position );
						
						if ( actual_size != write_length ){
							
							throw( new CacheFileManagerException( "Short write: required = " + write_length + ", actual = " + actual_size ));
						}					
					}
				}
			}else{
				
				long	actual_size = file.getFMFile().write( buffer, position );
				
				if ( actual_size != write_length ){
					
					throw( new CacheFileManagerException( "Short write: required = " + write_length + ", actual = " + actual_size ));
				}
			}
			
		}catch( FMFileManagerException e ){
			
			rethrow(e);
			
		}finally{
			
			if ( buffer_handed_over && !buffer_cached ){
				
				buffer.returnToPool();
			}
		}
	}
	
	protected void
	flushCache(
		CacheFileImpl		file,
		long				start,
		long				length )
	{
		// TODO: flush the cache!!!
	}
	
	protected void
	flushCache(
		CacheFileImpl		file )
	
		throws CacheFileManagerException
	{
		if ( cache_enabled ){
			
			synchronized( this ){
				
				System.out.println( "flushCache: " + file.getName());
				
				flushCache( file, 0, file.getLength());
			}
		}
	}
	
	protected void
	rethrow(
		FMFileManagerException e )
	
		throws CacheFileManagerException
	{
		Throwable 	cause = e.getCause();
		
		if ( cause != null ){
			
			throw( new CacheFileManagerException( e.getMessage(), cause ));
		}
		
		throw( new CacheFileManagerException( e.getMessage(), e ));
	}
}
