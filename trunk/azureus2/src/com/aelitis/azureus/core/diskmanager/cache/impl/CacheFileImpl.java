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
import org.gudy.azureus2.core3.config.COConfigurationManager;
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
		
	protected static boolean		TRACE					= false;
	protected final static boolean	TRACE_CACHE_CONTENTS	= false;
	
	static{
		
		TRACE = COConfigurationManager.getBooleanParameter( "diskmanager.perf.cache.trace" );
		
		if ( TRACE ){
		
			System.out.println( "**** Disk Cache tracing enabled ****" );
		}
	}
	
	protected final static int		READAHEAD_LOW_LIMIT		= 64*1024;
	protected final static int		READAHEAD_HIGH_LIMIT	= 256*1024;
	
	protected final static int		READAHEAD_HISTORY	= 32;
	
	protected CacheFileManagerImpl		manager;
	protected FMFile					file;
	protected TOTorrentFile				torrent_file;
	
	protected long[]					read_history		= new long[ READAHEAD_HISTORY ];
	protected int						read_history_next	= 0;
	
	protected TreeSet					cache			= new TreeSet(comparator);
			
	protected int 	current_read_ahead_size				= 0;
	
	protected static final int READ_AHEAD_STATS_WAIT_TICKS	= 10*1000 / CacheFileManagerImpl.STATS_UPDATE_FREQUENCY;
	
	protected int		read_ahead_stats_wait	= READ_AHEAD_STATS_WAIT_TICKS;
	
	protected Average	read_ahead_made_average 	= Average.getInstance(CacheFileManagerImpl.STATS_UPDATE_FREQUENCY, 5);
	protected Average	read_ahead_used_average 	= Average.getInstance(CacheFileManagerImpl.STATS_UPDATE_FREQUENCY, 5);
	
	protected long		read_ahead_bytes_made;
	protected long		last_read_ahead_bytes_made;
	protected long		read_ahead_bytes_used;
	protected long		last_read_ahead_bytes_used;
	
	protected int piece_size						= 0;
	protected int piece_offset						= 0;
	
	protected int file_offset						= 0;
	
	protected AEMonitor				this_mon		= new AEMonitor( "CacheFile" );
	
	protected
	CacheFileImpl(
		CacheFileManagerImpl	_manager,
		FMFile					_file,
		TOTorrentFile			_torrent_file )
	{
		manager		= _manager;
		file		= _file;
				
		Arrays.fill( read_history, -1 );
		
		if ( _torrent_file != null ){
			
			torrent_file	= _torrent_file;
			
			TOTorrent	torrent = torrent_file.getTorrent();
					
			piece_size	= (int)torrent.getPieceLength();
						
			long	total_size	= 0;
			
			for (int i=0;i<torrent.getFiles().length;i++){
				
				TOTorrentFile	f = torrent.getFiles()[i];
				
				if ( f == _torrent_file ){
					
					break;
				}
				
				total_size	+= f.getLength();
			}
			
			piece_offset	= piece_size - (int)( total_size % piece_size );
			
			if ( piece_offset == piece_size ){
				
				piece_offset	= 0;
			}
			
			current_read_ahead_size	= Math.min( READAHEAD_LOW_LIMIT, piece_size );
		}
	}
	
	protected void
	updateStats()
	{
		long	made	= read_ahead_bytes_made;
		long	used	= read_ahead_bytes_used;

		long	made_diff	= made - last_read_ahead_bytes_made;
		long	used_diff	= used - last_read_ahead_bytes_used;
		
		read_ahead_made_average.addValue( made_diff );
		read_ahead_used_average.addValue( used_diff );
		
		last_read_ahead_bytes_made	= made;
		last_read_ahead_bytes_used	= used;
		
			// give changes made to read ahead size a chance to work through the stats
			// before recalculating
		
		if ( --read_ahead_stats_wait == 0 ){
		
			read_ahead_stats_wait	= READ_AHEAD_STATS_WAIT_TICKS;
			
				// see if we need to adjust the read-ahead size
							
			double	made_average	= read_ahead_made_average.getAverage();
			double	used_average	= read_ahead_used_average.getAverage();
		
				// if used average > 75% of made average then increase
				
			double 	ratio = used_average*100/made_average;
			
			if ( ratio > 0.75 ){
				
				current_read_ahead_size	+= 16*1024;
				
				current_read_ahead_size	= Math.min( current_read_ahead_size, piece_size );
				
				current_read_ahead_size = Math.min( current_read_ahead_size, (int)(manager.getCacheSize()/16 ));
				
			}else if ( ratio < 0.5 ){
				
				current_read_ahead_size	-= 16*1024;
				
				current_read_ahead_size = Math.max( current_read_ahead_size, READAHEAD_LOW_LIMIT );
			}
		}
		
		// System.out.println( "read-ahead: done = " + read_ahead_bytes_made + ", used = " + read_ahead_bytes_used + ", done_av = " + read_ahead_made_average.getAverage() + ", used_av = " +  read_ahead_used_average.getAverage()+ ", size = " + current_read_ahead_size );
	}
	
	protected void
	readCache(
		DirectByteBuffer	file_buffer,
		long				file_position,
		boolean				recursive )
	
		throws CacheFileManagerException
	{
		int	file_buffer_position	= file_buffer.position(DirectByteBuffer.SS_CACHE);
		int	file_buffer_limit		= file_buffer.limit(DirectByteBuffer.SS_CACHE);
		
		int	read_length	= file_buffer_limit - file_buffer_position;
	
		if ( manager.isCacheEnabled()){
		
			if ( TRACE ){
				LGLogger.log( 
						"readCache: " + getName() + ", " + file_position + " - " + (file_position + read_length - 1 ) + 
						":" + file_buffer_position + "/" + file_buffer_limit );
			}
			
			if ( read_length == 0 ){
				
				return;	// nothing to do
			}
							
			long	writing_file_position	= file_position;
			int		writing_left			= read_length;

			boolean	ok 				= true;
			int		used_entries	= 0;
			long	used_read_ahead	= 0;
			

		
					// if we can totally satisfy the read from the cache, then use it
					// otherwise flush the cache (not so smart here to only read missing)
			
			try{
					
				this_mon.enter();

					// record the position of the byte *following* the end of this read
				
				read_history[read_history_next++]	= file_position + read_length;

				if ( read_history_next == READAHEAD_HISTORY ){
					
					read_history_next	= 0;
				}
				
				Iterator	it = cache.iterator();
				
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
						
						int					entry_buffer_position 	= entry_buffer.position(DirectByteBuffer.SS_CACHE);
						int					entry_buffer_limit		= entry_buffer.limit(DirectByteBuffer.SS_CACHE);
						
						try{
														
							entry_buffer.limit( DirectByteBuffer.SS_CACHE, entry_buffer_position + skip + available );
							
							entry_buffer.position( DirectByteBuffer.SS_CACHE, entry_buffer_position + skip );
							
							if ( TRACE ){
								LGLogger.log( 
										"cacheRead: using " + entry.getString() + 
										"[" + entry_buffer.position(DirectByteBuffer.SS_CACHE)+"/"+entry_buffer.limit(DirectByteBuffer.SS_CACHE)+ "]" +
										"to write to [" + file_buffer.position(DirectByteBuffer.SS_CACHE) + "/" + file_buffer.limit(DirectByteBuffer.SS_CACHE) + "]" );
							}
							
							used_entries++;
							
							file_buffer.put( DirectByteBuffer.SS_CACHE, entry_buffer );
								
							manager.cacheEntryUsed( entry );
							
						}finally{
							
							entry_buffer.limit( DirectByteBuffer.SS_CACHE, entry_buffer_limit );
							
							entry_buffer.position( DirectByteBuffer.SS_CACHE, entry_buffer_position );						
						}
						
						writing_file_position	+= available;
						writing_left			-= available;
						
						if ( entry.getType() == CacheEntry.CT_READ_AHEAD ){
							
							used_read_ahead	+= available;
						}

					}
				}
			}finally{
				
				if ( ok ){
				
					read_ahead_bytes_used += used_read_ahead;
				}
			
				this_mon.exit();
			}
			
			if ( ok && writing_left == 0 ){
				
					// only record this as a cache read hit if we haven't just read the 
					// data from the file system
				
				if ( !recursive ){
					
					manager.cacheBytesRead( read_length );
				}
					
				if ( TRACE ){
						
					LGLogger.log( "cacheRead: cache use ok [entries = " + used_entries + "]" );
				}
									
			}else{
					
				if ( TRACE ){
						
					LGLogger.log( "cacheRead: cache use fails, reverting to plain read" );
				}
							
					// reset in case we've done some partial reads
					
				file_buffer.position( DirectByteBuffer.SS_CACHE, file_buffer_position );
				
				try{
					boolean	do_read_ahead	= 
								!recursive &&
								manager.isReadCacheEnabled() &&
								read_length <  current_read_ahead_size &&
								file_position + current_read_ahead_size <= file.getLength();

					if ( do_read_ahead ){

							// only read ahead if this is a continuation of a prior read within history
						
						do_read_ahead	= false;
						
						for (int i=0;i<READAHEAD_HISTORY;i++){
							
							if ( read_history[i] == file_position ){
								
								do_read_ahead	= true;
								
								break;
							}
						}
					}
					
					int	actual_read_ahead = current_read_ahead_size;
					
					if ( do_read_ahead ){
					
							// don't read ahead over the end of a piece
						
						int	request_piece_offset = (int)((file_position - ( piece_offset + file_offset )) % piece_size);
						
						if ( request_piece_offset < 0 ){
							
							request_piece_offset += piece_size;
						}
						
						//System.out.println( "request offset = " + request_piece_offset );
						
						int	data_left = piece_size - request_piece_offset;
						
						if ( data_left < actual_read_ahead ){
							
							actual_read_ahead	= data_left;
							
								// no point in using read-ahead logic if actual read ahead
								// smaller or same as request size!
							
							if ( actual_read_ahead <= read_length ){
								
								do_read_ahead	= false;
							}
							//System.out.println( "    trimmed to " + data_left );
						}
					}
					
					if ( do_read_ahead ){
							
						if ( TRACE ){
								
							LGLogger.log( "\tperforming read-ahead" );
						}
							
						DirectByteBuffer	cache_buffer = 
								DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_CACHE_READ, actual_read_ahead );
														
						boolean	buffer_cached	= false;
							
						try{
							
								// must allocate space OUTSIDE sync block (see manager for details)
							
							CacheEntry	entry = 
								manager.allocateCacheSpace( 
										CacheEntry.CT_READ_AHEAD,
										this, 
										cache_buffer, file_position, actual_read_ahead );
														
							entry.setClean();
			
							System.out.println("read-ahead =" + actual_read_ahead );
							try{
								
								this_mon.enter();

									// flush before read so that any bits in cache get re-read correctly on read
						
								flushCache( file_position, actual_read_ahead, true, -1, 0, -1 );
								
								getFMFile().read( cache_buffer, file_position );
			
								read_ahead_bytes_made	+= actual_read_ahead;
								
								manager.fileBytesRead( actual_read_ahead );
									
								cache_buffer.position( DirectByteBuffer.SS_CACHE, 0 );
								
								cache.add( entry );
								
								manager.addCacheSpace( entry );
								
							}finally{
								
								this_mon.exit();
							}
								
							buffer_cached	= true;
																
						}finally{
								
							if ( !buffer_cached ){
									
									// if the read operation failed, and hence the buffer
									// wasn't added to the cache, then release it here
									
								cache_buffer.returnToPool();
							}
						}
														
							// recursively read from the cache, should hit the data we just read although
							// there is the possibility that it could be flushed before then - hence the
							// recursion flag that will avoid this happening next time around
						
						readCache( file_buffer, file_position, true );
					
					}else{
							
						if ( TRACE ){
								
							LGLogger.log( "\tnot performing read-ahead" );
						}
							
						try{
							
							this_mon.enter();
							
							flushCache( file_position, read_length, true, -1, 0, -1 );
						
							getFMFile().read( file_buffer, file_position );
							
						}finally{
							
							this_mon.exit();
						}
						
						manager.fileBytesRead( read_length );
					}
						
				}catch( FMFileManagerException e ){
						
					manager.rethrow(e);
				}				
			}
		}else{
			
			try{			
				getFMFile().read( file_buffer, file_position );
				
				manager.fileBytesRead( read_length );
	
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
		boolean	failed			= false;
		
		try{
			int	file_buffer_position	= file_buffer.position(DirectByteBuffer.SS_CACHE);
			int file_buffer_limit		= file_buffer.limit(DirectByteBuffer.SS_CACHE);
			
			int	write_length = file_buffer_limit - file_buffer_position;
			
			if ( write_length == 0 ){
				
				return;	// nothing to do
			}
			
			if ( manager.isWriteCacheEnabled() ){
				
				if ( TRACE ){
					
					LGLogger.log( 
							"writeCache: " + getName() + ", " + file_position + " - " + (file_position + write_length - 1 ) + 
							":" + file_buffer_position + "/" + file_buffer_limit );
				}
				
					// if the data is smaller than a piece and not handed over then it is most
					// likely apart of a piece at the start or end of a file. If so, copy it
					// and insert the copy into cache
							
				if ( 	( !buffer_handed_over ) &&
						write_length < piece_size ){
				
					if ( TRACE ){
						
						LGLogger.log( "    making copy of non-handedover buffer" );
					}
					
					DirectByteBuffer	cache_buffer = DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_CACHE_WRITE, write_length );
										
					cache_buffer.put( DirectByteBuffer.SS_CACHE, file_buffer );
					
					cache_buffer.position( DirectByteBuffer.SS_CACHE, 0 );
					
						// make it look like this buffer has been handed over
					
					file_buffer				= cache_buffer;
					
					file_buffer_position	= 0;
					file_buffer_limit		= write_length;
					
					buffer_handed_over	= true;
				}
				
				if ( buffer_handed_over ){
					
						// cache this write, allocate outside sync block (see manager for details)
	
					CacheEntry	entry = 
						manager.allocateCacheSpace(
								CacheEntry.CT_DATA_WRITE,
								this, 
								file_buffer, 
								file_position, 
								write_length );
					
					try{

						this_mon.enter();
						
							// if we are overwriting stuff already in the cache then force-write overlapped
							// data (easiest solution as this should only occur on hash-fails)

							// do the flush and add sychronized to avoid possibility of another
							// thread getting in-between and adding same block thus causing mutiple entries
							// for same space
						
						flushCache( file_position, write_length, true, -1, 0, -1 );
						
						cache.add( entry );
					
						manager.addCacheSpace( entry );
						
					}finally{
						
						this_mon.exit();
					}
																
					manager.cacheBytesWritten( write_length );
					
					buffer_cached	= true;
					
				}else{

						// not handed over, invalidate any cache that exists for the area
						// as it is now out of date
					
					try{
						
						this_mon.enter();
						
						flushCache( file_position, write_length, true, -1, 0, -1 );

						getFMFile().write( file_buffer, file_position );
						
					}finally{
						
						this_mon.exit();
					}
					
					manager.fileBytesWritten( write_length );
				}
			}else{
				
				getFMFile().write( file_buffer, file_position );
				
				manager.fileBytesWritten( write_length );
			}
			
		}catch( CacheFileManagerException e ){
			
			failed	= true;
			
			throw( e );
			
		}catch( FMFileManagerException e ){
			
			failed	= true;
			
			manager.rethrow(e);
			
		}finally{
			
			if ( buffer_handed_over ){
				
				if ( !(failed || buffer_cached )){
					
					file_buffer.returnToPool();
				}
			}
		}
	}
	
	protected void
	flushCache(
		long				file_position,
		long				length,					// -1 -> do all from position onwards
		boolean				release_entries,
		long				minimum_to_release,		// -1 -> all
		long				oldest_dirty_time, 		// dirty entries newer than this won't be flushed
													// 0 -> now
		long				min_chunk_size )		// minimum contiguous size for flushing, -1 -> no limit
	
		throws CacheFileManagerException
	{
		try{
			this_mon.enter();
			
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
						
					if ( 	dirty && 
							(	oldest_dirty_time == 0 ||
								entry.getLastUsed() < oldest_dirty_time )){
																	
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
							
							boolean	skip_chunk	= false;
							
							if ( min_chunk_size != -1 ){
								
								if ( release_entries ){
								
									Debug.out( "CacheFile: can't use min chunk with release option" );
								}else{
									
									skip_chunk	= multi_block_next - multi_block_start < min_chunk_size;
								}
							}
							
							List	f_multi_block_entries	= multi_block_entries;
							long	f_multi_block_start		= multi_block_start;
							long	f_multi_block_next		= multi_block_next;
							
							multi_block_start	= entry_file_position;
							
							multi_block_next	= entry_file_position + entry_length;
							
							multi_block_entries	= new ArrayList();
							
							multi_block_entries.add( entry );
							
							if ( skip_chunk ){
								
								if ( TRACE ){
									
									LGLogger.log( "flushCache: skipping " + multi_block_entries.size() + " entries, [" + multi_block_start + "," + multi_block_next + "] as too small" );			
								}
							}else{
								
								multiBlockFlush(
										f_multi_block_entries,
										f_multi_block_start,
										f_multi_block_next,
										release_entries );
							}
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
				
				boolean	skip_chunk	= false;
				
				if ( min_chunk_size != -1 ){
					
					if ( release_entries ){
					
						Debug.out( "CacheFile: can't use min chunk with release option" );
					}else{
						
						skip_chunk	= multi_block_next - multi_block_start < min_chunk_size;
					}
				}

				if ( skip_chunk ){
					
					if ( TRACE ){
						
						LGLogger.log( "flushCache: skipping " + multi_block_entries.size() + " entries, [" + multi_block_start + "," + multi_block_next + "] as too small" );			
					}
					
				}else{
					
					multiBlockFlush(
							multi_block_entries,
							multi_block_start,
							multi_block_next,
							release_entries );
				}
			}
			
			if ( last_failure != null ){
				
				if ( last_failure instanceof CacheFileManagerException ){
					
					throw((CacheFileManagerException)last_failure );
				}
				
				throw( new CacheFileManagerException( "cache flush failed", last_failure ));
			}
		}finally{
			
			this_mon.exit();
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
				
				if ( buffer.limit(DirectByteBuffer.SS_CACHE) - buffer.position(DirectByteBuffer.SS_CACHE) != entry.getLength()){
					
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
		long				file_start_position,
		boolean				release_entries,
		long				minumum_to_release )
	
		throws CacheFileManagerException
	{
		if ( manager.isCacheEnabled()){
							
			if ( TRACE ){
					
				LGLogger.log( "flushCache: " + getName() + ", rel = " + release_entries + ", min = " + minumum_to_release );
			}
			
			flushCache( file_start_position, -1, release_entries, minumum_to_release, 0, -1 );
		}
	}
	
	protected void
	flushCache(
		boolean				release_entries,
		long				minumum_to_release )
	
		throws CacheFileManagerException
	{
		flushCache(0, release_entries, minumum_to_release );
	}
	
	protected void
	flushOldDirtyData(
		long	oldest_dirty_time,
		long	min_chunk_size )
	
		throws CacheFileManagerException
	{
		if ( manager.isCacheEnabled()){
			
			if ( TRACE ){
	
				LGLogger.log( "flushOldDirtyData: " + getName());
			}

			flushCache( 0, -1, false, -1, oldest_dirty_time, min_chunk_size );
		}
	}
	
	protected void
	flushOldDirtyData(
		long	oldest_dirty_time )
	
		throws CacheFileManagerException
	{
		flushOldDirtyData( oldest_dirty_time, -1 );
	}
	
	protected long
	getBytesInCache(
		long	offset,
		long	length )
	{
		try{
			this_mon.enter();
			
			long	result	= 0;
			
			Iterator	it = cache.iterator();
			
			long	start_pos	= offset;
			long	end_pos		= offset + length;
			
			while( it.hasNext()){
			
				CacheEntry	entry = (CacheEntry)it.next();
				
				long	this_start 		= entry.getFilePosition();
				int		entry_length	= entry.getLength();
				
				long	this_end	= this_start + entry_length;
				
				if ( this_end <= start_pos ){
					
					continue;
				}
				
				if ( end_pos <= this_start ){
					
					break;
				}
				
				long	bit_start	= start_pos<this_start?this_start:start_pos;
				long	bit_end		= end_pos>=this_end?this_end:end_pos;

				result	+= bit_end - bit_start;
			}
			
			return( result );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
		// support methods
	
	
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
	
	protected TOTorrentFile
	getTorrentFile()
	{
		return( torrent_file );
	}
	
		// public methods
	
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
		readCache( buffer, position, false );
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
	
	public void
	setFileOffset(
		int		_file_offset )
	{
		file_offset		= _file_offset;
	}
}
