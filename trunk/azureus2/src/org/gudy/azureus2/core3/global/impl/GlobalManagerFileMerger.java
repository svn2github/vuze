/*
 * Created on Feb 2, 2015
 * Created by Paul Gardner
 * 
 * Copyright 2015 Azureus Software, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or 
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package org.gudy.azureus2.core3.global.impl;

import java.util.*;

import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfoListener;
import org.gudy.azureus2.core3.disk.DiskManagerListener;
import org.gudy.azureus2.core3.disk.DiskManagerPiece;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerDiskListener;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.global.GlobalManagerAdapter;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.DelayedEvent;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.plugins.PluginAdapter;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;

public class 
GlobalManagerFileMerger 
{
	private GlobalManagerImpl		gm;
	
	private Map<HashWrapper,DownloadManager>		dm_map = new HashMap<HashWrapper, DownloadManager>();
	
	private List<SameSizeFiles>				sames = new ArrayList<SameSizeFiles>();
	
	protected
	GlobalManagerFileMerger(
		GlobalManagerImpl			_gm )
	{
		gm		= _gm;
		
		PluginInitializer.getDefaultInterface().addListener(
			new PluginAdapter()
			{
				public void
				initializationComplete()
				{
					new DelayedEvent( 
						"GMFM:delay",
						30*1000,
						new AERunnable() {
							
							@Override
							public void 
							runSupport() 
							{
								initialise();
							}
						});
				}
			});
	}
	
	private void
	initialise()
	{
		gm.addListener(
			new GlobalManagerAdapter()
			{				
				public void 
				downloadManagerAdded(
					DownloadManager dm ) 
				{
					sync();
				}
				
				public void 
				downloadManagerRemoved(
					DownloadManager dm ) 
				{		
					sync();
				}
			},
			false );
		
		sync();
	}
	
	private void
	sync()
	{
		List<DownloadManager> dms = gm.getDownloadManagers();
				
		synchronized( dm_map ){

			boolean	changed = false;

			Set<HashWrapper>	existing_dm_hashes = new HashSet<HashWrapper>( dm_map.keySet());
			
			for ( DownloadManager dm: dms ){
				
				if ( !dm.isPersistent()){
					
					continue;
				}
				
				DownloadManagerState state = dm.getDownloadState();
				
				if ( 	state.getFlag( DownloadManagerState.FLAG_LOW_NOISE ) ||
						state.getFlag( DownloadManagerState.FLAG_METADATA_DOWNLOAD )){
					
					continue;
				}
				
				if ( !dm.isDownloadComplete( false )){
				
					TOTorrent torrent = dm.getTorrent();
					
					if ( torrent != null ){
										
						try{
							HashWrapper hw = torrent.getHashWrapper();
							
							if ( dm_map.containsKey( hw )){
								
								existing_dm_hashes.remove( hw );
								
							}else{
								
								dm_map.put( hw, dm );
								
								changed = true;
							}
						}catch( Throwable e ){
						}
					}
				}
			}
			
			if ( existing_dm_hashes.size() > 0 ){
				
				changed = true;
				
				for ( HashWrapper hw: existing_dm_hashes ){
					
					dm_map.remove( hw );
				}
			}
		
			if ( changed ){
							
				List<Set<DiskManagerFileInfo>>	interesting = new ArrayList<Set<DiskManagerFileInfo>>();
				
				Map<Long,Set<DiskManagerFileInfo>>		size_map = new HashMap<Long, Set<DiskManagerFileInfo>>();
				
				for ( DownloadManager dm: dm_map.values()){
					
					TOTorrent torrent = dm.getTorrent();
					
					if ( torrent == null ){
						
						continue;
					}
					
					long piece_size = torrent.getPieceLength();
					
					DiskManagerFileInfo[] files = dm.getDiskManagerFileInfoSet().getFiles();
					
					for ( DiskManagerFileInfo file: files ){
						
						long len = file.getLength();
						
							// filter out small files
						
						if ( len < piece_size ){
							
							continue;
						}
						
						Set<DiskManagerFileInfo> set = size_map.get( len );
						
						if ( set == null ){
							
							set = new HashSet<DiskManagerFileInfo>();
							
							size_map.put( len, set );
						}
						
						boolean same_dm = false;
						
						for ( DiskManagerFileInfo existing: set ){
							
							if ( existing.getDownloadManager() == dm ){
								
								same_dm = true;
								
								break;
							}
						}
						
						if ( !same_dm ){
							
							set.add( file );
							
							if ( set.size() == 2 ){
								
								interesting.add( set );
							}
						}
					}
				}
				
				List<SameSizeFiles>	sames_copy = new ArrayList<SameSizeFiles>( sames );

				for ( Set<DiskManagerFileInfo> set: interesting ){
					
					boolean	found = false;
										
					Iterator<SameSizeFiles>	sames_it = sames_copy.iterator();
					
					while( sames_it.hasNext()){
						
						SameSizeFiles same = sames_it.next();
						
						if ( same.sameAs( set )){
							
							found = true;
							
							sames_it.remove();
							
							break;
						}
					}
					
					if ( !found ){
						
						sames.add( new SameSizeFiles( set ));
					}
				}
				
				for ( SameSizeFiles dead: sames_copy ){
					
					dead.destroy();
					
					sames.remove( dead );
				}
			}
		}
	}
	
	private class
	SameSizeFiles
	{
		private Set<DiskManagerFileInfo>		files;
		private Set<SameSizeFileWrapper>		file_wrappers;
		
		private volatile boolean	destroyed;
		
		private
		SameSizeFiles(
			Set<DiskManagerFileInfo>		_files )
		{
			files 	= _files;
			
			file_wrappers = new HashSet<SameSizeFileWrapper>();
			
			for ( final DiskManagerFileInfo file: files ){
				
				final SameSizeFileWrapper file_wrapper = new SameSizeFileWrapper( file );
				
				DownloadManager dm = file_wrapper.getDownloadManager();
				
				file_wrappers.add( file_wrapper );
					
				DownloadManagerDiskListener	dmdl = 
					new DownloadManagerDiskListener() {
							
						private DiskManager		current_disk_manager;
						
						final DiskManagerFileInfoListener file_listener =
							new DiskManagerFileInfoListener() {
							
								public void 
								dataWritten(
									long offset, 
									long length ) 
								{
									if ( destroyed ){
										
										file.removeListener( this );
										
										return;
									}
									
									SameSizeFiles.this.dataWritten(  current_disk_manager, file_wrapper, offset, length );
								}
								
								public void 
								dataChecked(
									long offset, 
									long length ) 
								{
									if ( destroyed ){
										
										file.removeListener( this );
										
										return;
									}
								}
							};

							
						public void 
						diskManagerAdded(
							final DiskManager dm ) 
						{
							current_disk_manager	= dm;

								// derp, adding the listener triggers callbacks for all existing blocks
							
							file.addListener( file_listener );
							
							/*
							dm.addListener(
								new DiskManagerListener() {
									
									public void 
									stateChanged(
										int oldState, int new_state )
									{
										if ( new_state == DiskManager.READY ){
											
											SameSizeFiles.this.diskManagerAdded( file, f_fbo, dm );
										}
									}
									
									public void 
									pieceDoneChanged(
										DiskManagerPiece piece )
									{
									}
									
									public void 
									filePriorityChanged(
										DiskManagerFileInfo file )
									{
									}
									
									public void 
									fileAccessModeChanged(
										DiskManagerFileInfo 	file, 
										int 					old_mode,
										int 					new_mode ) 
									{
									}
								});
							*/
						}
						
						public void 
						diskManagerRemoved(
							DiskManager dm ) 
						{
							file.removeListener( file_listener );
							
							current_disk_manager = null;
						}
					};
						
				dm.setUserData( this, dmdl );
				
				dm.addDiskListener( dmdl );
			}
			
			System.out.println( "created " + getString());
		}
			
		private void 
		dataWritten(
			DiskManager				disk_manager,
			SameSizeFileWrapper		file,
			long 					offset, 
			long 					length ) 
		{
			//System.out.println( "written: " + offset + "/" + length );
			
			DiskManagerPiece[]	pieces = disk_manager.getPieces();
						
			long piece_length 	= disk_manager.getPieceLength();

			long file_byte_offset = file.getFileByteOffset();
			
			long	written_start 	= file_byte_offset + offset;
			long	written_end		= written_start + length - 1;
			
			int	first_piece_num = (int)( written_start/piece_length );
			int	last_piece_num 	= (int)( written_end/piece_length );
			
			DiskManagerPiece	first_piece 	= pieces[first_piece_num];
			DiskManagerPiece	last_piece 		= pieces[last_piece_num];
			
			int	first_block = (int)( written_start - ( first_piece_num * piece_length ))/DiskManager.BLOCK_SIZE;
			int	last_block 	= (int)( written_end - ( last_piece_num * piece_length ))/DiskManager.BLOCK_SIZE;
			
			if ( first_block > 0 ){
				boolean[] written = first_piece.getWritten();
				if ( first_piece.isDone() || ( written != null && written[first_block-1])){
					first_block--;
				}
			}else{
				if ( first_piece_num > 0 ){
					DiskManagerPiece	prev_piece 	= pieces[first_piece_num-1];
					boolean[] written = prev_piece.getWritten();
					int	nb = prev_piece.getNbBlocks();
					
					if ( prev_piece.isDone() || ( written != null && written[nb-1])){
						first_piece_num--;
						first_block	= nb-1;
					}
				}
			}
			
			if ( last_block < last_piece.getNbBlocks()-1 ){
				boolean[] written = last_piece.getWritten();
				if ( last_piece.isDone() || ( written != null && written[last_block+1])){
					last_block++;
				}
			}else{
				if ( last_piece_num < pieces.length-1 ){
					DiskManagerPiece	next_piece 	= pieces[last_piece_num+1];
					boolean[] written = next_piece.getWritten();

					if ( next_piece.isDone() || ( written != null && written[0])){
						last_piece_num++;
						last_block = 0;
					}
				}
			}
			
				// we've widened the effective write by one block each way where possible to handle block
				// misalignment across downloads
			
			long	avail_start = first_piece_num * piece_length + first_block * DiskManager.BLOCK_SIZE;
			long	avail_end 	= last_piece_num * piece_length +  last_block * DiskManager.BLOCK_SIZE + pieces[last_piece_num].getBlockSize( last_block );
				
			System.out.println( first_piece_num + "/" + first_block + " - " + last_piece_num + "/" + last_block  + ": " + avail_start + "-" + avail_end );
			
			for ( SameSizeFileWrapper other_file: file_wrappers ){
				
				if ( other_file == file ){
					
					continue;
				}
				
				DiskManager other_disk_manager = other_file.getDiskManager();
				
				if ( other_disk_manager == null ){
					
					continue;
				}
				
				DiskManagerPiece[]	other_pieces = other_disk_manager.getPieces();
				
				long other_piece_length 	= other_disk_manager.getPieceLength();

				long	skew = file_byte_offset - other_file.getFileByteOffset();
				
				if ( skew % DiskManager.BLOCK_SIZE == 0 ){
					
						// special case of direct block->block mapping
					
					for ( long block_start = avail_start; block_start < avail_end; block_start += DiskManager.BLOCK_SIZE ){

						int	origin_piece_num 	= (int)( block_start/piece_length );
						int	origin_block_num	= (int)(( block_start % piece_length ) / DiskManager.BLOCK_SIZE );
						
						long target_offset = block_start - skew;
						
						int	target_piece_num 	= (int)( target_offset/other_piece_length );
						int	target_block_num	= (int)(( target_offset % other_piece_length ) / DiskManager.BLOCK_SIZE );

						DiskManagerPiece	origin_piece = pieces[origin_piece_num];
						DiskManagerPiece	target_piece = other_pieces[target_piece_num];
						
						boolean[]	written = target_piece.getWritten();
						
						if ( target_piece.isDone() || (written != null && written[target_block_num])){
							
							// already written
							
						}else{
							
							if ( origin_piece.getBlockSize( origin_block_num ) == target_piece.getBlockSize( target_block_num )){
								
								System.out.println( "I would read and write here" );
							}
						}
					}
				}else{
						// need two blocks from source to consider writing to target
					
					for ( long block_start = avail_start; block_start < avail_end; block_start += DiskManager.BLOCK_SIZE ){
						
						long	origin_start 	= block_start;
												
						long	origin_end		= block_start + 2*DiskManager.BLOCK_SIZE - 1;
						
						if ( origin_end > avail_end ){
							
							origin_end = avail_end;
						}
						
						long target_offset = origin_start - skew;
						
						target_offset =  (( target_offset + DiskManager.BLOCK_SIZE - 1 ) / DiskManager.BLOCK_SIZE ) * DiskManager.BLOCK_SIZE;
						
						long origin_offset = target_offset + skew;
									
						int	target_piece_num 	= (int)( target_offset/other_piece_length );
						int	target_block_num	= (int)(( target_offset % other_piece_length ) / DiskManager.BLOCK_SIZE );

						DiskManagerPiece	target_piece = other_pieces[target_piece_num];

						boolean[]	written = target_piece.getWritten();

						if ( target_piece.isDone() || (written != null && written[target_block_num])){
							
							// already written
							
						}else{
						
							int target_block_size = target_piece.getBlockSize( target_block_num );
							
							if ( 	origin_offset >= file_byte_offset &&
									origin_offset + target_block_size < avail_end ){
							
								System.out.println( "Write from " + origin_offset + "/" + target_block_size + " to " + target_piece_num + "/" + target_block_num );
							}
						}
					}
				}
			}
		}
		
			
		private boolean
		sameAs(
			Set<DiskManagerFileInfo>		_others )
		{
			return( files.equals( _others ));
		}

		private void
		destroy()
		{
			destroyed = true;
			
			for ( DiskManagerFileInfo file: files ){
				
				DownloadManager	dm = file.getDownloadManager();
				
				DownloadManagerDiskListener dmdl = (DownloadManagerDiskListener)dm.getUserData( this );
				
				if ( dmdl != null ){
				
					dm.removeDiskListener( dmdl );
				}
			}
			
			System.out.println( "destroyed " + getString());
		}
		
		private String
		getString()
		{
			String str = "";
			
			long	size = -1;
			
			for ( DiskManagerFileInfo file: files ){
				
				size = file.getLength();
				
				str += (str.length()==0?"":", ") + file.getTorrentFile().getRelativePath();
			}
			
			str += " - length " + size;
			
			return( str );
		}
	}
	
	private class
	SameSizeFileWrapper
	{
		private final DiskManagerFileInfo		file;
		
		private final DownloadManager			download_manager;
		
		private final long						file_byte_offset;
		
		private 
		SameSizeFileWrapper(
			DiskManagerFileInfo		_file )
		{
			file	= _file;
			
			download_manager = file.getDownloadManager();
			
			int	file_index = file.getIndex();
				
			long fbo = 0;
			
			if ( file_index > 0){
									
				DiskManagerFileInfo[] f = download_manager.getDiskManagerFileInfoSet().getFiles();
				
				for ( int i=0;i<file_index;i++ ){
					
					fbo += f[i].getLength();
				}
			}
			
			file_byte_offset = fbo;
		}
		
		private DownloadManager
		getDownloadManager()
		{
			return( download_manager );
		}
		
		private DiskManager
		getDiskManager()
		{
			return( file.getDiskManager());
		}
		
		private long
		getFileByteOffset()
		{
			return( file_byte_offset );
		}
	}
}
