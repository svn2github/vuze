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

import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.logging.*;

import com.aelitis.azureus.core.diskmanager.cache.*;
import com.aelitis.azureus.core.diskmanager.file.*;

public class 
CacheFileManagerImpl 
	implements CacheFileManager
{
	public static final boolean	DEBUG	= false;
	
	public static final int	CACHE_CLEANER_TICKS		= 60;	// every 60 seconds
	
	public static final int		STATS_UPDATE_FREQUENCY		= 1*1000;	// 1 sec
	public static final long	DIRTY_CACHE_WRITE_MAX_AGE	= 120*1000;	// 2 mins
		
	static{
		if ( DEBUG ){
			
			System.out.println( "**** Cache consistency debugging on ****" );
		}
	}
	
	protected boolean	cache_enabled;
	protected boolean	cache_read_enabled;
	protected boolean	cache_write_enabled;
	
	protected long		cache_size;
	protected long		cache_files_not_smaller_than;
	
	protected long		cache_minimum_free_size;
	protected long		cache_space_free;
		
	protected FMFileManager		file_manager;
	
		// copy on update semantics
	
	protected WeakHashMap		cache_files			= new WeakHashMap();
	protected WeakHashMap		updated_cache_files	= null;
	
		// access order
	
	protected LinkedHashMap		cache_entries = new LinkedHashMap(1024, 0.75f, true );
	
	protected CacheFileManagerStatsImpl	stats;
	
		// copy on update semantics
	
	protected Map	torrent_to_cache_file_map	= new HashMap();
	
	protected long				cache_bytes_written;
	protected long				cache_bytes_read;
	protected long				file_bytes_written;
	protected long				file_bytes_read;
	
	protected long				cache_read_count;
	protected long				cache_write_count;
	protected long				file_read_count;
	protected long				file_write_count;
	
	protected AEMonitor			this_mon	= new AEMonitor( "CacheFileManager" );
	
	public
	CacheFileManagerImpl()
	{
		file_manager	= FMFileManagerFactory.getSingleton();
		
		boolean	enabled	= COConfigurationManager.getBooleanParameter( "diskmanager.perf.cache.enable" );

		boolean	enable_read 	= COConfigurationManager.getBooleanParameter( "diskmanager.perf.cache.enable.read" );
		
		boolean	enable_write	= COConfigurationManager.getBooleanParameter( "diskmanager.perf.cache.enable.write" );
		
			// units are MB
		
		int		size			= 1024*1024*COConfigurationManager.getIntParameter( "diskmanager.perf.cache.size" );
		
			// units are KB
		
		int		not_smaller_than	= 1024*COConfigurationManager.getIntParameter( "notsmallerthan" );
		
		if ( size <= 0 ){
		
			Debug.out( "Invalid cache size parameter (" + size + "), caching disabled" );
			
			enabled	= false;
		}
		
		initialise( enabled, enable_read, enable_write, size, not_smaller_than );
	}

	protected void
	initialise(
		boolean	enabled,
		boolean	enable_read,
		boolean	enable_write,
		long	size,
		long	not_smaller_than )
	{
		cache_enabled			= enabled && ( enable_read || enable_write );
		
		cache_read_enabled		= enabled && enable_read;
		
		cache_write_enabled		= enabled && enable_write;
		
		cache_size				= size;
		
		cache_files_not_smaller_than	= not_smaller_than;
		
		cache_minimum_free_size	= cache_size/4;
		
		cache_space_free		= cache_size;
		
		stats = new CacheFileManagerStatsImpl( this );
		
		AEThread	t = 
			new AEThread( "CacheStatsAndCleaner")
			{
				public void
				runSupport()
				{
					cacheStatsAndCleaner();
				}
			};
				
		t.setDaemon(true);
			
		t.start();
			
		LGLogger.log( "DiskCache: enabled = " + cache_enabled + ", read = " + cache_read_enabled + ", write = " + cache_write_enabled + ", size = " + cache_size + " MB" );
	}
	
	protected boolean
	isWriteCacheEnabled()
	{
		return( cache_write_enabled );
	}
	
	protected boolean
	isReadCacheEnabled()
	{
		return( cache_read_enabled );
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
							return( owner.getCacheFileOwnerName());
						}
						public TOTorrentFile
						getTorrentFile()
						{
							return( owner.getCacheFileTorrentFile());
						}
					}, file );
				
			TOTorrentFile	tf = owner.getCacheFileTorrentFile();
			
			CacheFile	cf;
			
			if ( tf != null && tf.getLength() < cache_files_not_smaller_than  ){
				
				cf = new CacheFileWithoutCache( this, fm_file, tf );
				
			}else{
				
				cf = new CacheFileWithCache( this, fm_file, tf );
			
				try{
					this_mon.enter();
	
					if ( updated_cache_files == null ){
						
						updated_cache_files = new WeakHashMap( cache_files );
					}
						// copy on write so readers don't need to synchronize or copy
					
					updated_cache_files.put( cf, null );
										
					if ( tf != null ){
		
									
						Map	new_map = new HashMap( torrent_to_cache_file_map );
								
						new_map.put( tf, cf );
				
						torrent_to_cache_file_map	= new_map;
					}	
				}finally{
					
					this_mon.exit();
				}
			}
			
			return( cf );
			
		}catch( FMFileManagerException e ){
			
			rethrow( e );
			
			return( null );
		}
	}
	
	public CacheFileManagerStats
	getStats()
	{
		return( stats );
	}
	
	protected boolean
	isCacheEnabled()
	{
		return( cache_enabled );
	}
	
		/**
		 * allocates space but does NOT add it to the cache list due to synchronization issues. Basically
		 * the caller mustn't hold their monitor when calling allocate, as a flush may result in one or more
		 * other files being flushed which results in their monitor being taken, and we've got an A->B and 
		 * B->A classic deadlock situation. However, we must keep the file's cache and our cache in step.
		 * It is not acceptable to have an entry inserted into our records but not in the file's as this
		 * then screws up the flush algorithm (which assumes that if it finds an entry in our list, a flush
		 * of that file is guaranteed to release space). Therefore we add the cache entry in addCacheSpace
		 * so that the caller can safely do this while synchronised firstly on its monitor and then we can
		 * sync on our. Hence we only ever get A->B monitor grabs which won't deadlock
		 * @param file
		 * @param buffer
		 * @param file_position
		 * @param length
		 * @return
		 * @throws CacheFileManagerException
		 */
	protected CacheEntry
	allocateCacheSpace(
		int					entry_type,
		CacheFileWithCache	file,
		DirectByteBuffer	buffer,
		long				file_position,
		int					length )
	
		throws CacheFileManagerException
	{
		boolean	ok 	= false;
		boolean	log	= false;		
		
		while( !ok ){
			
				// musn't invoke synchronised CacheFile methods while holding manager lock as this
				// can cause deadlocks (as CacheFile calls manager methods with locks)
			
			CacheEntry	oldest_entry	= null;
			
			try{
				this_mon.enter();
			
				if ( length < cache_space_free || cache_space_free == cache_size ){
				
					ok	= true;
					
				}else{
					
					oldest_entry = (CacheEntry)cache_entries.keySet().iterator().next();
				}
			}finally{
				
				this_mon.exit();
			}
			
			if ( !ok ){
				
				log	= true;
				
				long	old_free	= cache_space_free;
			
				CacheFileWithCache	oldest_file = oldest_entry.getFile();
				
				oldest_file.flushCache( oldest_entry.getFilePosition(), true, cache_minimum_free_size );
				
				long	flushed = cache_space_free - old_free;
				
				LGLogger.log( "DiskCache: cache full, flushed " + ( flushed ) + " from " + oldest_file.getName());
				
				if ( flushed == 0 ){
				
					try{
						this_mon.enter();
						
						if (	cache_entries.size() > 0 &&
								(CacheEntry)cache_entries.keySet().iterator().next() == oldest_entry ){
							
								// hmm, something wrong with cache as the flush should have got rid
								// of at least the oldest entry
							
							throw( new CacheFileManagerException( "Cache inconsistent: 0 flushed"));
						}
					}finally{
						
						this_mon.exit();
					}
				}
			}
		}
					
		CacheEntry	entry = new CacheEntry( entry_type, file, buffer, file_position, length );
			
		if ( log ){
				
			LGLogger.log( 
					"DiskCache: cr=" + cache_bytes_read + ",cw=" + cache_bytes_written+
					",fr=" + file_bytes_read + ",fw=" + file_bytes_written ); 
		}
			
		return( entry );
	}
	
	protected void
	cacheStatsAndCleaner()
	{
		long	cleaner_ticks	= CACHE_CLEANER_TICKS;
		
		while( true ){
			
			try{
			
				Thread.sleep( STATS_UPDATE_FREQUENCY );
				
			}catch( InterruptedException e ){
				
				Debug.printStackTrace( e );
				
				break;
			}
			
			stats.update();
			
			
			// System.out.println( "cache file count = " + cache_files.size());
								
			Iterator	cf_it = cache_files.keySet().iterator();
			
			while(cf_it.hasNext()){
				
				((CacheFileWithCache)cf_it.next()).updateStats();
			}
			
			if ( --cleaner_ticks == 0 ){
				
				cleaner_ticks	= CACHE_CLEANER_TICKS;
				
				Set		dirty_files	= new HashSet();
	
				long	now 	= SystemTime.getCurrentTime();
							
				long	oldest	= now - DIRTY_CACHE_WRITE_MAX_AGE;
				
				try{
					this_mon.enter();
			
					if ( updated_cache_files != null ){
						
						cache_files	= updated_cache_files;
							
						updated_cache_files	= null;
					}

					if ( cache_entries.size() > 0 ){
						
						Iterator it = cache_entries.keySet().iterator();
						
						while( it.hasNext()){
							
							CacheEntry	entry = (CacheEntry)it.next();
								
							// System.out.println( "oldest entry = " + ( now - entry.getLastUsed()));
							
							if ( entry.isDirty()){
								
								dirty_files.add( entry.getFile());
							}
						}
					}
					
					// System.out.println( "cache file = " + cache_files.size() + ", torrent map = " + torrent_to_cache_file_map.size());
					
				}finally{
					
					this_mon.exit();
				}
				
				Iterator	it = dirty_files.iterator();
				
				while( it.hasNext()){
					
					try{
						CacheFileWithCache	file = (CacheFileWithCache)it.next();
						
						TOTorrentFile	tf = file.getTorrentFile();
						
						long	min_flush_size	= -1;
						
						if ( tf != null ){
							
							min_flush_size	= tf.getTorrent().getPieceLength();
							
						}
						
						file.flushOldDirtyData( oldest, min_flush_size );
						
					}catch( Throwable e ){
						
							// if this fails then the error should reoccur on a "proper"
							// flush later and be reported
						
						Debug.printStackTrace( e );
					}
				}
			}
		}
	}
	
		// must be called when the cachefileimpl is synchronised to ensure that the file's
		// cache view and our cache view are consistent
	
	protected void
	addCacheSpace(
		CacheEntry		new_entry )
	
		throws CacheFileManagerException
	{
		try{
			this_mon.enter();
			
			cache_space_free	-= new_entry.getLength();
			
				// 	System.out.println( "Total cache space = " + cache_space_free );
		
			cache_entries.put( new_entry, new_entry );
			
			if ( DEBUG ){
				
				CacheFileWithCache	file	= new_entry.getFile();
								
				long	total_cache_size	= 0;
				
				int		my_count = 0;

				Iterator it = cache_entries.keySet().iterator();
				
				while( it.hasNext()){
					
					CacheEntry	entry = (CacheEntry)it.next();
					
					total_cache_size	+= entry.getLength();
					
					if ( entry.getFile() == file ){
						
						my_count++;
					}
				}
			
				if ( my_count != file.cache.size()){
					
					Debug.out( "Cache inconsistency: my count = " + my_count + ", file = " + file.cache.size());
					
					throw( new CacheFileManagerException( "Cache inconsistency: counts differ"));
					
				}else{
					
					//System.out.println( "Cache: file_count = " + my_count );
				}
				
				if ( total_cache_size != cache_size - cache_space_free ){
					
					Debug.out( "Cache inconsistency: used_size = " + total_cache_size + ", free = " + cache_space_free + ", size = " + cache_size );
					
					throw( new CacheFileManagerException( "Cache inconsistency: sizes differ"));
					
				}else{
					
					//System.out.println( "Cache: usage = " + total_cache_size );
				}
			}
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected void
	cacheEntryUsed(
		CacheEntry		entry )
	
		throws CacheFileManagerException
	{
		try{
			this_mon.enter();
		
				// note that the "get" operation update the MRU in cache_entries
			
			if ( cache_entries.get( entry ) == null ){
				
				Debug.out( "Cache inconsistency: entry missing on usage" );
				
				throw( new CacheFileManagerException( "Cache inconsistency: entry missing on usage"));
				
			}else{
				
				entry.used();
			}
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected void
	releaseCacheSpace(
		CacheEntry		entry )
	
		throws CacheFileManagerException
	{
		entry.getBuffer().returnToPool();
		
		try{
			this_mon.enter();
			
			cache_space_free	+= entry.getLength();
			
			if ( cache_entries.remove( entry ) == null ){
				
				Debug.out( "Cache inconsistency: entry missing on removal" );

				throw( new CacheFileManagerException( "Cache inconsistency: entry missing on removal"));
			}

			/*
			if ( 	entry.getType() == CacheEntry.CT_READ_AHEAD ){
				
				if ( entry.getUsageCount() < 2 ){
				
					System.out.println( "ra: not used" );
				
				}else{
				
					System.out.println( "ra: used" );
				}
			}
			*/
			
			// System.out.println( "Total cache space = " + cache_space_free );
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected long
	getCacheSize()
	{
		return( cache_size );
	}
	
	protected long
	getCacheUsed()
	{
		long free = cache_space_free;
		
		if ( free < 0 ){
			
			free	= 0;
		}
		
		return( cache_size - free );
	}
	
	protected void
	cacheBytesWritten(
		long		num )
	{
		try{
			this_mon.enter();
			
			cache_bytes_written	+= num;
			
			cache_write_count++;
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected void
	cacheBytesRead(
		int		num )
	{
		try{
			this_mon.enter();
			
			cache_bytes_read	+= num;
			
			cache_read_count++;
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected void
	fileBytesWritten(
		long	num )
	{
		try{
			this_mon.enter();
			
			file_bytes_written	+= num;
			
			file_write_count++;
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected void
	fileBytesRead(
		int		num )
	{
		try{
			this_mon.enter();
			
			file_bytes_read	+= num;
			
			file_read_count++;
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected long
	getBytesWrittenToCache()
	{
		return( cache_bytes_written );
	}
	
	protected long
	getBytesWrittenToFile()
	{
		return( file_bytes_written );
	}
	
	protected long
	getBytesReadFromCache()
	{
		return( cache_bytes_read );
	}
	
	protected long
	getBytesReadFromFile()
	{
		return( file_bytes_read );
	}
	
	public long
	getCacheReadCount()
	{
		return( cache_read_count );
	}
	
	public long
	getCacheWriteCount()
	{
		return( cache_write_count );
	}
	
	public long
	getFileReadCount()
	{
		return( file_read_count );
	}
	
	public long
	getFileWriteCount()
	{
		return( file_write_count );
	}
	
	protected void
	closeFile(
		CacheFileWithCache	file )
	{
		TOTorrentFile	tf = file.getTorrentFile();
		
		if ( tf != null && torrent_to_cache_file_map.get( tf ) != null ){

			try{
				this_mon.enter();
						
				Map	new_map = new HashMap( torrent_to_cache_file_map );
				
				new_map.remove( tf );
	
				torrent_to_cache_file_map	= new_map;
				
			}finally{
				
				this_mon.exit();
			}
		}
	}
	
	protected long
	getBytesInCache(
		TOTorrent		torrent,
		int				piece_number,
		int				offset,
		long			length )
	{
			// copied on update, grab local ref to access
		
		Map	map = torrent_to_cache_file_map;
		
		TOTorrentFile[]	files = torrent.getFiles();
		
		long	piece_size = torrent.getPieceLength();
		
		long	target_start 	= piece_number*piece_size + offset;
		long	target_end		= target_start + length;
		
		long	pos = 0;
		
		long	result	= 0;
		
		for (int i=0;i<files.length;i++){
			
			TOTorrentFile	tf = files[i];
			
			long	len = tf.getLength();
			
			long	this_start 	= pos;
			
			pos	+= len;
			
			long	this_end	= pos;
				
			if ( this_end <= target_start ){
				
				continue;
			}
			
			if ( target_end <= this_start ){
				
				break;
			}
			
			long	bit_start	= target_start>this_start?target_start:this_start;
			long	bit_end		= target_end<this_end?target_end:this_end;
			
			CacheFileWithCache	cache_file = (CacheFileWithCache)map.get( tf );
			
			if ( cache_file != null ){
				
				result	+= cache_file.getBytesInCache( bit_start - this_start, bit_end - bit_start );
			}
		}
		
		return( result );
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
