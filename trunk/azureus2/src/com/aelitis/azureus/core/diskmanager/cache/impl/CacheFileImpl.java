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
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.logging.*;

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
		
	protected final static boolean	TRACE					= true;
	protected final static boolean	TRACE_CACHE_CONTENTS	= false;
	
	protected final static boolean	READAHEAD_ENABLE		= true;
	protected final static int		READAHEAD_MAX			= 65536;
	
	protected CacheFileManagerImpl		manager;
	protected FMFile					file;
	
	protected TreeSet					cache	= new TreeSet(comparator);
			
	protected int read_ahead_size				= 0;
	
	protected
	CacheFileImpl(
		CacheFileManagerImpl	_manager,
		FMFile					_file,
		TOTorrentFile			_torrent_file )
	{
		manager		= _manager;
		file		= _file;
		
		if ( _torrent_file != null ){
			
			TOTorrent	torrent = _torrent_file.getTorrent();
						
			read_ahead_size	= (int)torrent.getPieceLength();
			
			if ( read_ahead_size > READAHEAD_MAX ){
				
				read_ahead_size	= READAHEAD_MAX;
			}
		}else{
			
			read_ahead_size	= READAHEAD_MAX;
		}
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
				LGLogger.log( 
						"readCache: " + getName() + ", " + file_position + " - " + (file_position + read_length - 1 ) + 
						":" + file_buffer_position + "/" + file_buffer_limit );
			}
			
			if ( read_length == 0 ){
				
				return;	// nothing to do
			}
			
			synchronized( this ){
				
				long	writing_file_position	= file_position;
				int		writing_left			= read_length;
		
					// if we can totally satisfy the read from the cache, then use it
					// otherwise flush the cache (not so smart here to only read missing)
				
				Iterator	it = cache.iterator();
				
				boolean	ok 				= true;
				int		used_entries	= 0;
				
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
														
							entry_buffer.limit( entry_buffer_position + skip + available );
							
							entry_buffer.position( entry_buffer_position + skip );
							
							if ( TRACE ){
								LGLogger.log( 
										"cacheRead: using " + entry.getString() + 
										"[" + entry_buffer.position()+"/"+entry_buffer.limit()+ "]" +
										"to write to [" + file_buffer.position() + "/" + file_buffer.limit() + "]" );
							}
							
							used_entries++;
							
							file_buffer.put( entry_buffer );
								
							manager.cacheEntryUsed( entry );
							
						}finally{
							
							entry_buffer.limit( entry_buffer_limit );
							
							entry_buffer.position( entry_buffer_position );						
						}
						
						writing_file_position	+= available;
						writing_left			-= available;
					}
				}
				
				if ( ok && writing_left == 0 ){
					
					manager.cacheBytesRead( read_length );
					
					if ( TRACE ){
						
						LGLogger.log( "cacheRead: cache use ok [entries = " + used_entries + "]" );
					}
									
				}else{
					
					if ( TRACE ){
						
						LGLogger.log( "cacheRead: cache use fails, reverting to plain read" );
					}
							
					file_buffer.position( file_buffer_position );
					
					try{
						if ( 	READAHEAD_ENABLE &&
								read_length <  read_ahead_size &&
								file_position + read_ahead_size <= file.getLength()){
							
							if ( TRACE ){
								
								LGLogger.log( "\tread ahead hit" );
							}
							
							flushCache( file_position, read_ahead_size, true, -1 );
							
							DirectByteBuffer	cache_buffer = DirectByteBufferPool.getBuffer( read_ahead_size );
							
							cache_buffer.position(0);
							
							cache_buffer.limit( read_ahead_size );
							
							boolean	buffer_cached	= false;
							
							try{
								getFMFile().read( cache_buffer, file_position );
		
								manager.fileBytesRead( read_ahead_size );
								
								cache_buffer.position(0);
							
								CacheEntry	entry = manager.allocateCacheSpace( this, cache_buffer, file_position, read_ahead_size );
							
								entry.setClean();
							
								cache.add( entry );
								
								buffer_cached	= true;
								
								manager.cacheBytesWritten( read_ahead_size );
								
							}finally{
								
								if ( !buffer_cached ){
									
										// if the read operation failed, and hence the buffer
										// wasn't added to the cache, then release it here
									
									cache_buffer.returnToPool();
								}
							}
							
							if ( TRACE_CACHE_CONTENTS ){
								
								printCache();
							}

								// this recursive readCache is guaranteed to hit
							
							readCache( file_buffer, file_position );
							
						}else{
							
							if ( TRACE ){
								
								LGLogger.log( "\tread ahead miss" );
							}
							
							flushCache( file_position, read_length, true, -1 );
							
							getFMFile().read( file_buffer, file_position );
							
							manager.fileBytesRead( read_length );
						}
						
					}catch( FMFileManagerException e ){
							
						manager.rethrow(e);
					}				
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
						
						LGLogger.log( 
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
						
						manager.cacheBytesWritten( write_length );
					}else{
						
						getFMFile().write( file_buffer, file_position );
												
						manager.fileBytesWritten( write_length );
					}
				}
			}else{
				
				getFMFile().write( file_buffer, file_position );
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
		long				length,					// -1 -> do all from position onwards
		boolean				release_entries,
		long				minimum_to_release )	// -1 = all
	
		throws CacheFileManagerException
	{
		synchronized( this ){
							
			Iterator	it = cache.iterator();
			
			Throwable	last_failure = null;
			
			long	entry_total_released = 0;
			
			List	multi_block_entries		= new ArrayList();
			long	multi_block_start		= -1;
			long	multi_block_next		= -1;
			
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
					// we're going to deal with this entry one way or another. In particular if
					// we are releasing entries then this is guaranteed to be released, either directly
					// or via a flush if dirty
				
				boolean	dirty = entry.isDirty();

				try{
						
					if ( dirty ){
																	
						if ( multi_block_start == -1 ){
							
								// start of day
							
							multi_block_start	= entry_file_position;
							
							multi_block_next	= entry_file_position + entry_length;
							
							multi_block_entries.add( entry );
							
						}else if ( multi_block_next == entry_file_position ){
							
								// continuation, add in
							
							multi_block_next = entry_file_position + entry_length;
					
							multi_block_entries.add( entry );
							
						}else{
							
								// we've got a gap - flush current and start another series
							
								// set up ready for next block in case the flush fails - we try
								// and flush as much as possible in the face of failure
							
							List	f_multi_block_entries	= multi_block_entries;
							long	f_multi_block_start		= multi_block_start;
							long	f_multi_block_next		= multi_block_next;
							
							multi_block_start	= entry_file_position;
							
							multi_block_next	= entry_file_position + entry_length;
							
							multi_block_entries	= new ArrayList();
							
							multi_block_entries.add( entry );
							
							multiBlockFlush(
									f_multi_block_entries,
									f_multi_block_start,
									f_multi_block_next,
									release_entries );
						}
					}
				}catch( Throwable e ){
					
					Debug.out( "cacheFlush fails: " + e.getMessage());
					
					last_failure	= e;
					
				}finally{
					
					if ( release_entries ){
					
						it.remove();
						
							// if it is dirty it will be released when the flush is done
						
						if ( !dirty ){
	
							manager.releaseCacheSpace( entry );
						}
						
						entry_total_released += entry.getLength();

						if ( minimum_to_release != -1 && entry_total_released > minimum_to_release ){
							
								// if this entry needs flushing this is done outside the loop
							
							break;
						}
					}
				}
			}
			
			if ( multi_block_start != -1 ){
				
				multiBlockFlush(
						multi_block_entries,
						multi_block_start,
						multi_block_next,
						release_entries );
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
	multiBlockFlush(
		List		multi_block_entries,
		long		multi_block_start,
		long		multi_block_next,
		boolean		release_entries )
	
		throws CacheFileManagerException
	{
		boolean	write_ok	= false;
		
		try{
			if ( TRACE ){
				
				LGLogger.log( "multiBlockFlush: writing " + multi_block_entries.size() + " entries, [" + multi_block_start + "," + multi_block_next + "," + release_entries + "]" );			
			}
			
			DirectByteBuffer[]	buffers = new DirectByteBuffer[ multi_block_entries.size()];
			
			long	expected_per_entry_write = 0;
			
			for (int i=0;i<buffers.length;i++){
				
				CacheEntry	entry = (CacheEntry)multi_block_entries.get(i);
				
					// sanitity check - we should always be flushing entire entries
			
				DirectByteBuffer	buffer = entry.getBuffer();
				
				if ( buffer.limit() - buffer.position() != entry.getLength()){
					
					throw( new CacheFileManagerException( "flush: inconsistent entry length, position wrong" ));
				}
				
				expected_per_entry_write	+= entry.getLength();
				
				buffers[i] = buffer;
			}
			
			long	expected_overall_write	= multi_block_next - multi_block_start;

			if ( expected_per_entry_write != expected_overall_write ){
		
				throw( new CacheFileManagerException( "flush: inconsistent write length, entrys = " + expected_per_entry_write + " overall = " + expected_overall_write ));
				
			}
			
			getFMFile().write( buffers, multi_block_start );
									
			manager.fileBytesWritten( expected_overall_write );
			
			write_ok	= true;
			
		}catch( FMFileManagerException e ){
			
			throw( new CacheFileManagerException( "flush fails", e ));
			
		}finally{			
			
			for (int i=0;i<multi_block_entries.size();i++){
				
				CacheEntry	entry = (CacheEntry)multi_block_entries.get(i);
				
				if ( release_entries ){

					manager.releaseCacheSpace( entry );
					
				}else{
					
					entry.resetBufferPosition();
			
					if ( write_ok ){
						
						entry.setClean();
					}
				}
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
					
					LGLogger.log( "flushCache: " + getName() + ", rel = " + release_entries + ", min = " + minumum_to_release );
				}
				
				flushCache( 0, -1, release_entries, minumum_to_release);
			}
		}
	}
	
	
	protected void
	printCache()
	{
		synchronized( CacheFileImpl.class ){
			
			LGLogger.log( "cache for " + getName());
			
			Iterator	it = cache.iterator();
			
			while(it.hasNext()){
				
				CacheEntry entry = (CacheEntry)it.next();
				
				LGLogger.log( "  " + entry.getString());
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
	clearCache()
	
		throws CacheFileManagerException
	{
		flushCache(true, -1);
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
