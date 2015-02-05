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
import org.gudy.azureus2.core3.disk.DiskManagerPiece;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerPeerListener;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.global.GlobalManagerAdapter;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.DelayedEvent;
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;
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
					
				DownloadManagerPeerListener dmpl =
					new DownloadManagerPeerListener() {
						
						private DiskManager	current_disk_manager;
						
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
									
									SameSizeFiles.this.dataWritten( file_wrapper, offset, length );
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
						peerManagerAdded(
							PEPeerManager manager ) 
						{
							synchronized( this ){
								
								if ( current_disk_manager != null ){
									
									file.removeListener( file_listener );
								}
								
								current_disk_manager = manager.getDiskManager();
								
								if ( current_disk_manager == null ){
									
									return;
								}
							}
							
							file.addListener( file_listener );
						}

						public void 
						peerManagerRemoved(
							PEPeerManager manager) 
						{
							synchronized( this ){
								
								if ( current_disk_manager != null ){
									
									file.removeListener( file_listener );
									
									current_disk_manager = null;
								}
							}
						}	
						
						public void 
						peerAdded(
							PEPeer peer ) 
						{
						}
						
						public void 
						peerRemoved(
							PEPeer peer ) 
						{
						}
						
						public void 
						peerManagerWillBeAdded(
							PEPeerManager manager ) 
						{
						}
					};
					

						
				dm.setUserData( this, dmpl );
				
				dm.addPeerListener( dmpl );
			}
			
			System.out.println( "created " + getString());
		}
			
		private void 
		dataWritten(
			SameSizeFileWrapper		file,
			long 					offset, 
			long 					length ) 
		{
			System.out.println( "written: " + offset + "/" + length );
			
			DiskManager		disk_manager	= file.getDiskManager();
			PEPeerManager	peer_manager 	= file.getPeerManager();
			
			if ( disk_manager == null || peer_manager == null ){
				
				return;
			}
			
			DiskManagerPiece[]	pieces = disk_manager.getPieces();
						
			long piece_length 	= disk_manager.getPieceLength();

			long file_byte_offset = file.getFileByteOffset();
			
			long	written_start 				= file_byte_offset + offset;
			long	written_end_inclusive		= written_start + length - 1;
			
			int	first_piece_num = (int)( written_start/piece_length );
			int	last_piece_num 	= (int)( written_end_inclusive/piece_length );
			
			DiskManagerPiece	first_piece 	= pieces[first_piece_num];
			DiskManagerPiece	last_piece 		= pieces[last_piece_num];
			
			int	first_block = (int)( written_start % piece_length )/DiskManager.BLOCK_SIZE;
			int	last_block 	= (int)( written_end_inclusive % piece_length )/DiskManager.BLOCK_SIZE;
			
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
			
			long	avail_start 			= ( first_piece_num * piece_length ) + ( first_block * DiskManager.BLOCK_SIZE );
			long	avail_end_inclusive 	= ( last_piece_num  * piece_length ) + ( last_block * DiskManager.BLOCK_SIZE ) + pieces[last_piece_num].getBlockSize( last_block ) - 1;
				
			System.out.println( first_piece_num + "/" + first_block + " - " + last_piece_num + "/" + last_block  + ": " + avail_start + "-" + avail_end_inclusive );
			
			for ( SameSizeFileWrapper other_file: file_wrappers ){
				
				if ( other_file == file ){
					
					continue;
				}
				
				DiskManager 	other_disk_manager = other_file.getDiskManager();
				PEPeerManager 	other_peer_manager = other_file.getPeerManager();
				
				if ( other_disk_manager == null || other_peer_manager == null ){
					
					continue;
				}
				
				DiskManagerPiece[]	other_pieces = other_disk_manager.getPieces();
				
				long other_piece_length 	= other_disk_manager.getPieceLength();

				long	skew = file_byte_offset - other_file.getFileByteOffset();
				
				if ( skew % DiskManager.BLOCK_SIZE == 0 ){
					
						// special case of direct block->block mapping
					
					for ( long block_start = avail_start; block_start < avail_end_inclusive; block_start += DiskManager.BLOCK_SIZE ){

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
								
								DirectByteBuffer buffer = disk_manager.readBlock( origin_piece_num, origin_block_num*DiskManager.BLOCK_SIZE, origin_piece.getBlockSize( origin_block_num ));

								try{
									other_file.getDownloadManager().getPeerManager().writeBlock( target_piece_num, target_block_num*DiskManager.BLOCK_SIZE, buffer, null, true );

									System.out.println( "Write from " + origin_piece_num + "/" + origin_block_num + " to " + target_piece_num + "/" + target_block_num );

									buffer = null;
									
								}finally{
									
									if ( buffer != null ){
										
										buffer.returnToPool();
									}
								}
							}
						}
					}
				}else{
						// need two blocks from source to consider writing to target
					
					DirectByteBuffer	prev_block 		= null;
					int					prev_block_pn	= 0;
					int					prev_block_bn	= 0;
					
					try{
						for ( long block_start = avail_start; block_start < avail_end_inclusive; block_start += DiskManager.BLOCK_SIZE ){
							
							long	origin_start 			= block_start;
							long	origin_end_inclusive	= block_start + 2*DiskManager.BLOCK_SIZE - 1;
							
							if ( origin_end_inclusive > avail_end_inclusive ){
								
								origin_end_inclusive = avail_end_inclusive;
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
										origin_offset + target_block_size <= avail_end_inclusive + 1){
								
									int	origin1_piece_number 	= (int)( origin_start/piece_length );
									int	origin1_block_num		= (int)(( origin_start % piece_length ) / DiskManager.BLOCK_SIZE );
	
									DiskManagerPiece	origin1_piece = pieces[origin1_piece_number];
									
									DirectByteBuffer read_block1	= null;
									DirectByteBuffer read_block2	= null;
									
									try{
										if ( 	prev_block != null &&
												prev_block_pn == origin1_piece_number &&
												prev_block_bn == origin1_block_num ){
											
											read_block1 = prev_block;
											prev_block	= null;
													
										}else{
										
											read_block1 = disk_manager.readBlock( origin1_piece_number , origin1_block_num*DiskManager.BLOCK_SIZE, origin1_piece.getBlockSize( origin1_block_num ));
										}
										
										int	origin2_piece_number 	= origin1_piece_number;
										int	origin2_block_num		= origin1_block_num + 1;
										
										if ( origin2_block_num >= origin1_piece.getNbBlocks()){
											
											origin2_piece_number++;
											
											origin2_block_num = 0;
										}
		
										DiskManagerPiece	origin2_piece = pieces[origin2_piece_number];
		
										read_block2 = disk_manager.readBlock( origin2_piece_number , origin2_block_num*DiskManager.BLOCK_SIZE, origin2_piece.getBlockSize( origin2_block_num ));
										
										DirectByteBuffer write_block = DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_EXTERNAL, target_block_size );
										
										final byte SS = DirectByteBuffer.SS_EXTERNAL;
												
										int	delta = (int)( origin_offset - origin_start );
										
										read_block1.position( SS , delta );
										
										write_block.limit( SS, read_block1.remaining( SS ));
										
										write_block.put( SS, read_block1 );
										
										write_block.limit( SS, target_block_size );
										
										read_block2.limit( SS, write_block.remaining( SS ));
										
										write_block.put( SS, read_block2 );
										
										write_block.flip( SS );
										
										read_block1.returnToPool();
										
										read_block1 = null;
										
										read_block2.position( SS, 0 );
										read_block2.limit( SS, read_block2.capacity( SS ));
										
										prev_block 		= read_block2;
										prev_block_pn	= origin2_piece_number;
										prev_block_bn	= origin2_block_num;
												
										read_block2	= null;
										
										System.out.println( "Write from " + origin_offset + "/" + delta + "/" + target_block_size + " to " + target_piece_num + "/" + target_block_num );
			
										other_file.getDownloadManager().getPeerManager().writeBlock( target_piece_num, target_block_num*DiskManager.BLOCK_SIZE, write_block, null, true );
																				
									}finally{
										
										if ( read_block1 != null ){
											
											read_block1.returnToPool();
										}
										
										if ( read_block2 != null ){
											
											read_block2.returnToPool();
										}
									}
								}
							}
						}
					}finally{
						
						if ( prev_block != null ){
							
							prev_block.returnToPool();
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
				
				DownloadManagerPeerListener dmpl = (DownloadManagerPeerListener)dm.getUserData( this );
				
				if ( dmpl != null ){
				
					dm.removePeerListener( dmpl );
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
		
		private PEPeerManager
		getPeerManager()
		{
			return( download_manager.getPeerManager());
		}
		
		private long
		getFileByteOffset()
		{
			return( file_byte_offset );
		}
	}
}
