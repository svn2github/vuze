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

package com.aelitis.azureus.core.diskmanager.cache.impl;

/**
 * @author parg
 *
 */

import java.io.File;
import java.util.*;

import org.gudy.azureus2.core3.util.*;

import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.diskmanager.cache.*;
import com.aelitis.azureus.core.diskmanager.file.*;

public class 
CacheFileImpl 
	implements CacheFile
{
	protected static Comparator comparator = new
		Comparator()
		{
			public int 
		   	compare(
		   		Object _o1, 
				Object _o2)
			{
					// entries in the cache should never overlap
				
				CacheEntry	o1 = (CacheEntry)_o1;
				CacheEntry	o2 = (CacheEntry)_o2;
				
				long	offset1 = o1.getFilePosition();
				int		length1	= o1.getLength();
				
				long	offset2 = o2.getFilePosition();
				int		length2	= o2.getLength();
				
				if (	offset1 + length1 <= offset2 ||
						offset2 + length2 <= offset1 ){
					
				}else{
					
					Debug.out( "Overlapping cache entries - " + o1.getString() + "/" + o2.getString());
				}
		   	
				return( offset1 - offset2 < 0?-1:1 );
			}
		};
		
	protected final static boolean	TRACE					= false;
	protected final static boolean	TRACE_CACHE_CONTENTS	= false;
	
	
	protected CacheFileManagerImpl		manager;
	protected FMFile					file;
	
	protected TreeSet					cache	= new TreeSet(comparator);
			
	
	protected
	CacheFileImpl(
		CacheFileManagerImpl	_manager,
		FMFile					_file )
	{
		manager		= _manager;
		file		= _file;
	}
	
	
	protected void
	readCache(
		DirectByteBuffer	file_buffer,
		long				file_position )
	
		throws CacheFileManagerException
	{
		if ( manager.isCacheEnabled()){
		
			int	file_buffer_position	= file_buffer.position();
			int	file_buffer_limit		= file_buffer.limit();
			
			int	read_length	= file_buffer_limit - file_buffer_position;
		
			if ( TRACE ){
				System.out.println( 
						"readCache: " + getName() + ", " + file_position + " - " + (file_position + read_length - 1 ) + 
						":" + file_buffer_position + "/" + file_buffer_limit );
			}
			
			if ( read_length == 0 ){
				
				return;	// nothing to do
			}
			
			synchronized( file ){
				
				long	writing_file_position	= file_position;
				long	writing_buffer_position	= file_buffer_position;
				int		writing_left			= read_length;
		
				// if we can totally satisfy the read from the cache, then use it
				// otherwise flush the cache - no read cache at the moment
				
				Iterator	it = cache.iterator();
				
				boolean	ok = true;
				
				while( ok && writing_left > 0 && it.hasNext()){
				
					CacheEntry	entry = (CacheEntry)it.next();
					
					long	entry_file_position 	= entry.getFilePosition();
					int		entry_length			= entry.getLength();
				
					if ( entry_file_position > writing_file_position ){
						
							// data missing at the start of the read section
						
						ok = false;
						
						break;
						
					}else if ( entry_file_position + entry_length <= writing_file_position ){
						
							// not got there yet
					}else{
						
							// copy required amount into read buffer
						
						int		skip	= (int)(writing_file_position - entry_file_position);
						
						int		available = entry_length - skip;
						
						if ( available > writing_left ){
							
							available	= writing_left;
						}
						
						DirectByteBuffer	entry_buffer = entry.getBuffer();
						
						int					entry_buffer_position 	= entry_buffer.position();
						int					entry_buffer_limit		= entry_buffer.limit();
						
						try{
														
							entry_buffer.position( entry_buffer_position + skip );
							
							entry_buffer.limit( entry_buffer_position + skip + available );
						
							if ( TRACE ){
								System.out.println( 
										"cacheRead: using " + entry.getString() + 
										"[" + entry_buffer.position()+"/"+entry_buffer.limit()+ "]" +
										"to write to [" + file_buffer.position() + "/" + file_buffer.limit() + "]" );
							}
							
							file_buffer.put( entry_buffer );
								
							manager.cacheEntryUsed( entry );
							
						}finally{
							
							entry_buffer.position( entry_buffer_position );
							
							entry_buffer.limit( entry_buffer_limit );
						}
						
						writing_file_position	+= available;
						writing_left			-= available;
					}
				}
				
				if ( ok && writing_left == 0 ){
					
					manager.cacheBytesRead( read_length );
					
					if ( TRACE ){
						System.out.println( "cacheRead: cache use ok" );
					}
									
				}else{
					
					if ( TRACE ){
						System.out.println( "cacheRead: cache use fails, reverting to plain read" );
					}
							
					file_buffer.position( file_buffer_position );
					
					flushCache( file_position, read_length, true, -1 );
					
					try{			
						getFMFile().read( file_buffer, file_position );
							
					}catch( FMFileManagerException e ){
							
						manager.rethrow(e);
					}
					
					manager.fileBytesRead( read_length );
				}
			}
		}else{
			
			try{			
				getFMFile().read( file_buffer, file_position );
					
			}catch( FMFileManagerException e ){
					
				manager.rethrow(e);
			}
		}
	}
	
	protected void
	writeCache(
		DirectByteBuffer	file_buffer,
		long				file_position,
		boolean				buffer_handed_over )
	
		throws CacheFileManagerException
	{
		boolean	buffer_cached	= false;
		
		try{
			int	file_buffer_position	= file_buffer.position();
			int file_buffer_limit		= file_buffer.limit();
			
			int	write_length = file_buffer_limit - file_buffer_position;
			
			if ( write_length == 0 ){
				
				return;	// nothing to do
			}
			
			if ( manager.isCacheEnabled() ){
				
				synchronized( this ){
					
					if ( TRACE ){
						System.out.println( 
								"writeCache: " + getName() + ", " + file_position + " - " + (file_position + write_length - 1 ) + 
								":" + file_buffer_position + "/" + file_buffer_limit );
					}
					
						// if we are overwriting stuff already in the cache then force-write overlapped
						// data (easiest solution as this should only occur on hash-fails)
					
					flushCache( file_position, write_length, true, -1 );
				
					if ( buffer_handed_over ){
						
							// cache this write
		
						CacheEntry	entry = manager.allocateCacheSpace( this, file_buffer, file_position, write_length );
						
						cache.add( entry );
						
						if ( TRACE_CACHE_CONTENTS ){
							printCache();
						}
						
						buffer_cached	= true;
						
					}else{
						
						long	actual_size = getFMFile().write( file_buffer, file_position );
						
						if ( actual_size != write_length ){
							
							throw( new CacheFileManagerException( "Short write: required = " + write_length + ", actual = " + actual_size ));
						}	
						
						manager.fileBytesWritten( write_length );
					}
				}
			}else{
				
				long	actual_size = getFMFile().write( file_buffer, file_position );
				
				if ( actual_size != write_length ){
					
					throw( new CacheFileManagerException( "Short write: required = " + write_length + ", actual = " + actual_size ));
				}
			}
			
		}catch( FMFileManagerException e ){
			
			manager.rethrow(e);
			
		}finally{
			
			if ( buffer_handed_over && !buffer_cached ){
				
				file_buffer.returnToPool();
			}
		}
	}
	
	protected void
	flushCache(
		long				file_position,
		long				length,	// -1 -> do all from position onwards
		boolean				release_entries,
		long				minimum_to_release )	// -1 = all
	
		throws CacheFileManagerException
	{
		synchronized( file ){
							
			Iterator	it = cache.iterator();
			
			Throwable	last_failure = null;
			
			long	entry_total_released = 0;
			
			while( it.hasNext()){
			
				CacheEntry	entry = (CacheEntry)it.next();
				
				long	entry_file_position 	= entry.getFilePosition();
				int		entry_length			= entry.getLength();
			
				if ( entry_file_position + entry_length <= file_position ){
					
						// to the left
				
					continue;
					
				}else if ( length != -1 && file_position + length <= entry_file_position ){
					
						// to the right, give up
					
					break;
				}
				
					// overlap!!!!
				
				try{
					if ( entry.isDirty()){
						
						DirectByteBuffer	entry_buffer = entry.getBuffer();
						
						int	pos = entry_buffer.position();
					
						try{
							if ( TRACE ){
								System.out.println( "flushCache: writing " + entry.getString());
							}
							
							int	actual_size = getFMFile().write( entry_buffer, entry.getFilePosition());
					
							if ( actual_size != entry.getLength()){
								
								throw( new CacheFileManagerException( "Short write: required = " + entry.getLength() + ", actual = " + actual_size ));
							}
							
							manager.cacheBytesWritten( actual_size );
							
							entry.setClean();
							
						}finally{
							
							entry_buffer.position( pos );
						}
					}
				}catch( Throwable e ){
					
					Debug.out( "cacheFlush fails: " + e.getMessage());
					
					last_failure	= e;
					
				}finally{
					
					if ( release_entries ){
					
						it.remove();
						
						entry_total_released += entry.getLength();

						manager.releaseCacheSpace( entry );
						
						if ( minimum_to_release != -1 && entry_total_released > minimum_to_release ){
							
							break;
						}
					}
				}
			}
			
			if ( last_failure != null ){
				
				if ( last_failure instanceof CacheFileManagerException ){
					
					throw((CacheFileManagerException)last_failure );
				}
				
				throw( new CacheFileManagerException( "cache flush failed", last_failure ));
			}
		}
	}
	
	protected void
	flushCache(
		boolean				release_entries,
		long				minumum_to_release )
	
		throws CacheFileManagerException
	{
		if ( manager.isCacheEnabled()){
			
			synchronized( this ){
				
				if ( TRACE ){
					System.out.println( "flushCache: " + getName());
				}
				
				flushCache( 0, -1, release_entries, minumum_to_release);
			}
		}
	}
	
	protected void
	printCache()
	{
		synchronized( CacheFileImpl.class ){
			
			System.out.println( "cache for " + getName());
			
			Iterator	it = cache.iterator();
			
			while(it.hasNext()){
				
				CacheEntry entry = (CacheEntry)it.next();
				
				System.out.println( "  " + entry.getString());
			}
		}
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
			flushCache( true, -1 );
			
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
				
				flushCache( false, -1 );
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
			
			flushCache( true, -1 );
			
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
		readCache( buffer, position );
	}
		
	public void
	write(
		DirectByteBuffer	buffer,
		long				position )
	
		throws CacheFileManagerException
	{
		writeCache( buffer, position, false );
	}
	
	public void
	writeAndHandoverBuffer(
		DirectByteBuffer	buffer,
		long				position )
	
		throws CacheFileManagerException
	{
		writeCache( buffer, position, true );
	}
	
	public void
	flushCache()
	
		throws CacheFileManagerException
	{
		flushCache( false, -1 );
	}
	
	public void
	close()
	
		throws CacheFileManagerException
	{
		try{
			flushCache( true, -1 );
			
			file.close();
			
		}catch( FMFileManagerException e ){
			
			manager.rethrow(e);
		}
	}
}
