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
			
	protected long read_ahead_offset			= -1;
	protected long read_ahead_size				= 0;
	
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
			
			TOTorrentFile[]	torrent_files = torrent.getFiles();
			
			long	size_so_far	= 0;
			long	piece_size	= torrent.getPieceLength();
			
			for (int i=0;i<torrent_files.length;i++){
				
				TOTorrentFile	tf = torrent_files[i];
				
				if ( tf == _torrent_file ){
					
					long	first_piece_offset = size_so_far % piece_size;
					
					read_ahead_offset	= first_piece_offset;
					read_ahead_size		= piece_size;
					
					//System.out.println( getName() + ": piece offset = " + first_piece_offset );
					
				}else{
					
					size_so_far += tf.getLength();
				}
			}
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
				System.out.println( 
						"readCache: " + getName() + ", " + file_position + " - " + (file_position + read_length - 1 ) + 
						":" + file_buffer_position + "/" + file_buffer_limit );
			}
			
			if ( read_length == 0 ){
				
				return;	// nothing to do
			}
			
			synchronized( this ){
				
				long	writing_file_position	= file_position;
				long	writing_buffer_position	= file_buffer_position;
				int		writing_left			= read_length;
		
				// if we can totally satisfy the read from the cache, then use it
				// otherwise flush the cache - no read cache at the moment
				
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
														
							entry_buffer.position( entry_buffer_position + skip );
							
							entry_buffer.limit( entry_buffer_position + skip + available );
						
							if ( TRACE ){
								System.out.println( 
										"cacheRead: using " + entry.getString() + 
										"[" + entry_buffer.position()+"/"+entry_buffer.limit()+ "]" +
										"to write to [" + file_buffer.position() + "/" + file_buffer.limit() + "]" );
							}
							
							used_entries++;
							
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
						System.out.println( "cacheRead: cache use ok [entries = " + used_entries + "]" );
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
							
							multiBlockFlush(
									multi_block_entries,
									multi_block_start,
									multi_block_next,
									release_entries );

							multi_block_start	= entry_file_position;
							
							multi_block_next	= entry_file_position + entry_length;
							
							multi_block_entries.add( entry );
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
		try{
			if ( TRACE ){
				
				System.out.println( "flushCache: writing " + multi_block_entries.size() + " entries" );
			}
			
			DirectByteBuffer[]	buffers = new DirectByteBuffer[ multi_block_entries.size()];
			
			for (int i=0;i<buffers.length;i++){
				
				buffers[i] = ((CacheEntry)multi_block_entries.get(i)).getBuffer();
			}
			
			long	actual_size = getFMFile().write( buffers, multi_block_start );
			
			long	expected	= multi_block_next - multi_block_start;
			
			if ( actual_size != expected ){
				
				throw( new CacheFileManagerException( "Short write: required = " + expected + ", actual = " + actual_size ));
			}
			
			manager.cacheBytesWritten( expected );
			
		}catch( FMFileManagerException e ){
			
			throw( new CacheFileManagerException( "flush fails", e ));
			
		}finally{			
			
			for (int i=0;i<multi_block_entries.size();i++){
				
				CacheEntry	entry = (CacheEntry)multi_block_entries.get(i);
				
				if ( release_entries ){

					manager.releaseCacheSpace( entry );
					
				}else{
					
					entry.resetBufferPosition();
					
					entry.setClean();
				}
			}
						
			multi_block_entries.clear();
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
