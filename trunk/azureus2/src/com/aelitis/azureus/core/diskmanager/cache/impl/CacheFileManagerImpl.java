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
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.logging.*;

import com.aelitis.azureus.core.diskmanager.cache.*;
import com.aelitis.azureus.core.diskmanager.file.*;

public class 
CacheFileManagerImpl 
	implements CacheFileManager
{
	public static final boolean	DEBUG	= true;
	
	public static final int	CACHE_CLEANER_TICKS		= 15;	// every 15 seconds
	
	public static final int		STATS_UPDATE_FREQUENCY		= 1*1000;
	public static final long	DIRTY_CACHE_WRITE_MAX_AGE	= 30*1000;
		
	static{
		if ( DEBUG ){
			
			System.out.println( "**** Cache consistency debugging on ****" );
		}
	}
	
	protected boolean	cache_enabled;
	protected long		cache_size;
	protected long		cache_minimum_free_size;
	protected long		cache_space_free;
		
	protected FMFileManager		file_manager;

		// access order
	
	protected LinkedHashMap		cache_entries = new LinkedHashMap(1024, 0.75f, true );
	
	protected CacheFileManagerStatsImpl	stats;
	
	protected long				cache_bytes_written;
	protected long				cache_bytes_read;
	protected long				file_bytes_written;
	protected long				file_bytes_read;
	
	public
	CacheFileManagerImpl()
	{
		file_manager	= FMFileManagerFactory.getSingleton();
		
		boolean	enabled	= COConfigurationManager.getBooleanParameter( "diskmanager.perf.cache.enable" );
		
		/*
		System.out.println( "**** Disk Cache forced on for testing purposes ****" );
		
		if ( !enabled ){
			
			COConfigurationManager.setParameter( "diskmanager.perf.cache.enable", true );
		
			COConfigurationManager.save();
			
			enabled	= true;
		}
		*/
		
		int		size	= 1024*1024*COConfigurationManager.getIntParameter( "diskmanager.perf.cache.size" );
		
		if ( size <= 0 ){
		
			Debug.out( "Invalid cache size parameter (" + size + "), caching disabled" );
			
			enabled	= false;
		}
		
		initialise( enabled, size );
	}

	protected void
	initialise(
		boolean	enabled,
		long	size )
	{
		cache_enabled			= enabled;
		cache_size				= size;
		
		cache_minimum_free_size	= cache_size/4;
		
		cache_space_free		= cache_size;
		
		stats = new CacheFileManagerStatsImpl( this );
		
		AEThread	t = 
			new AEThread( "CacheStatsAndCleaner")
			{
				public void
				run()
				{
					cacheStatsAndCleaner();
				}
			};
				
		t.setDaemon(true);
			
		t.start();
			
		LGLogger.log( "DiskCache: enabled = " + cache_enabled + ", size = " + cache_size + " MB" );
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
					}, file );
				
			return( new CacheFileImpl( this, fm_file, owner.getCacheFileTorrentFile()));
			
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
		CacheFileImpl		file,
		DirectByteBuffer	buffer,
		long				file_position,
		int					length )
	
		throws CacheFileManagerException
	{
		boolean	ok 	= false;
		boolean	log	= false;		
		
		while( !ok ){
			
				// musn't invoke synchronized CacheFile methods while holding manager lock as this
				// can cause deadlocks (as CacheFile calls manager methods with locks)
			
			CacheEntry	oldest_entry	= null;
			
			synchronized( this ){
			
				if ( length < cache_space_free || cache_space_free == cache_size ){
				
					ok	= true;
					
				}else{
					
					oldest_entry = (CacheEntry)cache_entries.keySet().iterator().next();
				}
			}
			
			if ( !ok ){
				
				log	= true;
				
				long	old_free	= cache_space_free;
			
				CacheFileImpl	oldest_file = oldest_entry.getFile();
				
				oldest_file.flushCache( oldest_entry.getFilePosition(), true, cache_minimum_free_size );
				
				long	flushed = cache_space_free - old_free;
				
				LGLogger.log( "DiskCache: cache full, flushed " + ( flushed ) + " from " + oldest_file.getName());
				
				if ( flushed == 0 ){
				
					synchronized( this ){
									
						if (	cache_entries.size() > 0 &&
								(CacheEntry)cache_entries.keySet().iterator().next() == oldest_entry ){
							
								// hmm, something wrong with cache as the flush should have got rid
								// of at least the oldest entry
							
							throw( new CacheFileManagerException( "Cache inconsistent: 0 flushed"));
						}
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
				
				e.printStackTrace();
				
				break;
			}
			
			stats.update();
			
			if ( --cleaner_ticks == 0 ){
				
				cleaner_ticks	= CACHE_CLEANER_TICKS;
				
				Set		dirty_files	= new HashSet();
	
				long	now 	= SystemTime.getCurrentTime();
							
				long	oldest	= now - DIRTY_CACHE_WRITE_MAX_AGE;
				
				synchronized( this ){
					
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
				}
				
				Iterator	it = dirty_files.iterator();
				
				while( it.hasNext()){
					
					try{
						((CacheFileImpl)it.next()).flushOldDirtyData( oldest );
						
					}catch( Throwable e ){
						
							// if this fails then the error should reoccur on a "proper"
							// flush later and be reported
						
						e.printStackTrace();
					}
				}
			}
		}
	}
	
		// must be called when the cachefileimpl is synchronized to ensure that the file's
		// cache view and our cache view are consistent
	
	protected void
	addCacheSpace(
		CacheEntry		new_entry )
	
		throws CacheFileManagerException
	{
		synchronized( this ){
	
			cache_space_free	-= new_entry.getLength();
			
				// 	System.out.println( "Total cache space = " + cache_space_free );
		
			cache_entries.put( new_entry, new_entry );
			
			if ( DEBUG ){
				
				CacheFileImpl	file	= new_entry.getFile();
								
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
		}
	}
	
	protected void
	cacheEntryUsed(
		CacheEntry		entry )
	
		throws CacheFileManagerException
	{
		synchronized( this ){
		
				// note that the "get" operation update the MRU in cache_entries
			
			if ( cache_entries.get( entry ) == null ){
				
				Debug.out( "Cache inconsistency: entry missing on usage" );
				
				throw( new CacheFileManagerException( "Cache inconsistency: entry missing on usage"));
				
			}else{
				
				entry.used();
			}
		}
	}
	
	protected void
	releaseCacheSpace(
		CacheEntry		entry )
	
		throws CacheFileManagerException
	{
		entry.getBuffer().returnToPool();
		
		synchronized( this ){

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
	
	protected synchronized void
	cacheBytesWritten(
		long		num )
	{
		cache_bytes_written	+= num;
	}
	
	protected synchronized void
	cacheBytesRead(
		int		num )
	{
		cache_bytes_read	+= num;
	}
	
	protected synchronized void
	fileBytesWritten(
		long	num )
	{
		file_bytes_written	+= num;
	}
	
	protected synchronized void
	fileBytesRead(
		int		num )
	{
		file_bytes_read	+= num;
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
