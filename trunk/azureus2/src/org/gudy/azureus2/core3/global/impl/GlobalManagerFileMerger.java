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

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfoListener;
import org.gudy.azureus2.core3.disk.DiskManagerPiece;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerPeerListener;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.global.GlobalManagerAdapter;
import org.gudy.azureus2.core3.logging.LogAlert;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPeerManagerListenerAdapter;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.DelayedEvent;
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.core3.util.TimerEventPeriodic;
import org.gudy.azureus2.plugins.PluginAdapter;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;


public class 
GlobalManagerFileMerger 
{
	private static final boolean	TRACE = false;
	
	
	private static final int MIN_PIECES				= 5;
	private static final int HASH_FAILS_BEFORE_QUIT	= 3;
	
	private static final int TIMER_PERIOD		= 5*1000;
	private static final int SYNC_TIMER_PERIOD	= 60*1000;
	private static final int SYNC_TIMER_TICKS	= SYNC_TIMER_PERIOD/TIMER_PERIOD;
	
	private static final Object merged_data_lock = new Object();
	
	private GlobalManagerImpl		gm;
	
	private boolean	initialised;
	private boolean	enabled;
	
	private Map<HashWrapper,DownloadManager>		dm_map = new HashMap<HashWrapper, DownloadManager>();
	
	private List<SameSizeFiles>				sames = new ArrayList<SameSizeFiles>();
	
	private AsyncDispatcher		read_write_dispatcher = new AsyncDispatcher( "GMFM" );
	
	private TimerEventPeriodic	timer_event;
	
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
		COConfigurationManager.addAndFireParameterListener(
			"Merge Same Size Files",
			new ParameterListener(){
				
				public void 
				parameterChanged(
					String name ) 
				{
					enabled = COConfigurationManager.getBooleanParameter( name );
					
					if ( initialised ){
						
						syncFileSets();
					}
				}
			});
		
		gm.addListener(
			new GlobalManagerAdapter()
			{				
				public void 
				downloadManagerAdded(
					DownloadManager dm ) 
				{
					syncFileSets();
				}
				
				public void 
				downloadManagerRemoved(
					DownloadManager dm ) 
				{		
					syncFileSets();
				}
			},
			false );
		
		syncFileSets();
		
		initialised = true;
	}
	
	private void
	syncFileSets()
	{
		List<DownloadManager> dms = gm.getDownloadManagers();
				
		synchronized( dm_map ){

			boolean	changed = false;

			Set<HashWrapper>	existing_dm_hashes = new HashSet<HashWrapper>( dm_map.keySet());
			
			if ( enabled ){
				
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
										
					DiskManagerFileInfo[] files = dm.getDiskManagerFileInfoSet().getFiles();
					
					for ( DiskManagerFileInfo file: files ){
						
							// filter out small files
							
						if ( file.getNbPieces() < MIN_PIECES ){
							
							continue;
						}
						
						long len = file.getLength();
						
						
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
			
				if ( sames.size() > 0 ){
					
					if ( timer_event == null ){
						
						timer_event = 
							SimpleTimer.addPeriodicEvent(
								"GMFM:sync",
								TIMER_PERIOD,
								new TimerEventPerformer()
								{
									private int	tick_count = 0;
									
									public void 
									perform(
										TimerEvent event) 
									{
										tick_count++;
										
										synchronized( dm_map ){
											
											for ( SameSizeFiles s: sames ){
												
												s.sync( tick_count );
											}
										}
									}
								});
					}
				}else{
					
					if ( timer_event != null ){
						
						timer_event.cancel();
						
						timer_event = null;
					}
				}
			}
		}
	}
	
	private class
	SameSizeFiles
	{
		final private Set<DiskManagerFileInfo>		files;
		final private Set<SameSizeFileWrapper>		file_wrappers;
		
		private boolean	completion_logged;
		
		private volatile boolean	dl_has_restarted;
		
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
					
				DownloadManagerPeerListenerEx dmpl =
					new DownloadManagerPeerListenerEx(){
						
						AsyncDispatcher dispatcher = new AsyncDispatcher( "GMFM:serial" );
						
						private Object		lock = this;
								
						private DiskManager	current_disk_manager;
						
						private boolean	pm_removed;
						
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
									
									file_wrapper.dataWritten( offset, length );
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
						sync()
						{
							if ( destroyed ){
								
								return;
							}
						
							dispatcher.dispatch(
								new AERunnable()
								{
									public void
									runSupport()
									{
										if ( destroyed ){
											
											return;
										}
										
										synchronized( lock ){
											
											if ( current_disk_manager != null ){
												
												file.removeListener( file_listener );
												
											}else{
												
												return;
											}
										}
							
										file.addListener( file_listener );
									}
								});
						}
						
						public void 
						peerManagerAdded(
							final PEPeerManager manager ) 
						{
							if ( destroyed ){
								
								return;
							}
							
							dispatcher.dispatch(
								new AERunnable()
								{
									public void
									runSupport()
									{
										if ( destroyed ){
											
											return;
										}
										
										if ( pm_removed ){
											
											dl_has_restarted = true;
										}
										
										manager.addListener(
											new PEPeerManagerListenerAdapter(){
												
												public void 
												pieceCorrupted( 
													PEPeerManager 	manager, 
													int 			piece_number )
												{
													if ( destroyed ){
														
														manager.removeListener( this );
														
														return;
													}
													
													file_wrapper.pieceCorrupt( piece_number );
												}
											});
										
										synchronized( lock ){
											
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
								});
						}

						public void 
						peerManagerRemoved(
							PEPeerManager manager) 
						{
							dispatcher.dispatch(
								new AERunnable()
								{
									public void
									runSupport()
									{
										synchronized( lock ){
											
											pm_removed = true;
											
											if ( current_disk_manager != null ){
												
												file.removeListener( file_listener );
												
												current_disk_manager = null;
											}
										}
									}
								});
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
			
			dl_has_restarted = true;
			
			if ( TRACE )System.out.println( "created " + getString());
		}
			
		private void
		sync(
			int		tick_count )
		{
			if ( read_write_dispatcher.getQueueSize() > 0 ){
				
					// stuff is going on, ignore sync until things are idle
				
				return;
			}
			
			if ( dl_has_restarted ){
				
				dl_has_restarted = false;
				
			}else{
				
				if ( tick_count % SYNC_TIMER_TICKS != 0 ){
					
					return;
				}
			}
			
			List<DiskManagerFileInfo>	active = new ArrayList<DiskManagerFileInfo>();
			
			int		num_incomplete	= 0;
			
			for ( DiskManagerFileInfo file: files ){
				
				if ( file.isSkipped()){
					
					continue;
				}
				
				int dl_state = file.getDownloadManager().getState();
				
				if ( dl_state == DownloadManager.STATE_DOWNLOADING || dl_state ==  DownloadManager.STATE_SEEDING ){
					
					active.add( file );
					
					if ( file.getLength() != file.getDownloaded()){
				
						num_incomplete++;
					}
				}
			}
			
			if ( num_incomplete > 0 &&  active.size() > 1 ){
				
				for ( DiskManagerFileInfo file: active ){
					
					DownloadManager	dm = file.getDownloadManager();
					
					DownloadManagerPeerListenerEx dmpl = (DownloadManagerPeerListenerEx)dm.getUserData( this );
					
					if ( dmpl != null ){
						
						dmpl.sync();
					}
				}
			}
			
			if ( !completion_logged ){
				
				boolean	all_done 		= true;
				long	total_merged	= 0;
				
				for ( SameSizeFileWrapper ssf: file_wrappers ){
					
					if ( ssf.isSkipped()){
						
						continue;
					}
					
					total_merged += ssf.getMergedByteCount();

					if ( !ssf.isComplete()){
					
						all_done = false;
					}
				}
				
				if ( all_done ){
					
					completion_logged	= true;
					
					if ( total_merged > 0 ){
						
						String msg = "Successfully merged files:\n";
						
						for ( SameSizeFileWrapper file: file_wrappers ){
							
							long	merged = file.getMergedByteCount();
							
							if ( merged > 0 ){
								
								DownloadManager dm = file.getDownloadManager();
								
								msg += dm.getDisplayName();
								
								if ( !dm.getTorrent().isSimpleTorrent()){
																	
									msg += " - " + file.getFile().getTorrentFile().getRelativePath();
								}
								
								msg +=  ": " + DisplayFormatters.formatByteCountToKiBEtc( merged ) + "\n";
							}
						}
							
						msg += "\nTotal: " + DisplayFormatters.formatByteCountToKiBEtc( total_merged );
						
						Logger.log(					
								new LogAlert(
									true,
									LogAlert.AT_INFORMATION,
									msg ));	
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
		abandon(
			SameSizeFileWrapper		failed )
		{
			destroy();
			
			String msg = "Abandoned attempt to merge files:\n";
			
			for ( SameSizeFileWrapper file: file_wrappers ){
				
				msg += file.getDownloadManager().getDisplayName() + " - " + file.getFile().getTorrentFile().getRelativePath() + "\n";
			}
			msg += "\nToo many hash fails in " + failed.getDownloadManager().getDisplayName();
			
			Logger.log(					
					new LogAlert(
						true,
						LogAlert.AT_INFORMATION,
						msg ));	
		}
		
		private void
		destroy()
		{
			destroyed = true;
			
			for ( DiskManagerFileInfo file: files ){
				
				DownloadManager	dm = file.getDownloadManager();
				
				DownloadManagerPeerListenerEx dmpl = (DownloadManagerPeerListenerEx)dm.getUserData( this );
				
				if ( dmpl != null ){
				
					dm.removePeerListener( dmpl );
				}
			}
			
			if ( TRACE )System.out.println( "destroyed " + getString());
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
	
		
		private class
		SameSizeFileWrapper
		{
			private final DiskManagerFileInfo		file;
			
			private final DownloadManager			download_manager;
			
			private final long						file_byte_offset;
			
			private final String					id;
			
			private long							merged_byte_counnt;
			
			private final boolean[]					modified_pieces;
			
			private int	pieces_completed;
			private int	pieces_corrupted;
			
			private 
			SameSizeFileWrapper(
				DiskManagerFileInfo		_file )
			{
				file	= _file;
				
				modified_pieces	= new boolean[ file.getNbPieces()];
				
				download_manager = file.getDownloadManager();
				
				int	file_index = file.getIndex();
					
				long fbo = 0;
				
				if ( file_index > 0){
										
					DiskManagerFileInfo[] f = download_manager.getDiskManagerFileInfoSet().getFiles();
					
					for ( int i=0;i<file_index;i++ ){
						
						fbo += f[i].getLength();
					}
				}
				
				String _id;
				
				try{
					_id = Base32.encode( download_manager.getTorrent().getHash()) + "/" + file.getIndex();
					
				}catch( Throwable e ){
					
					_id = download_manager.getDisplayName() + "/" + file.getIndex();
				}
				
				id	= _id;
				
				file_byte_offset = fbo;
			}
			
			private DiskManagerFileInfo
			getFile()
			{
				return( file );
			}
			
			private boolean
			isSkipped()
			{
				return( file.isSkipped());
			}
			
			private boolean
			isComplete()
			{
				return( file.getLength() == file.getDownloaded());
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
			
			private String
			getID()
			{
				return( id );
			}
			
			private void 
			dataWritten(
				long 						offset, 
				long 						length ) 
			{
				if ( TRACE )System.out.println( "written: " + offset + "/" + length );
				
				final DiskManager		disk_manager	= getDiskManager();
				final PEPeerManager	peer_manager 		= getPeerManager();
				
				if ( disk_manager == null || peer_manager == null ){
					
					return;
				}
				
				final DiskManagerPiece[]	pieces = disk_manager.getPieces();
							
				final long piece_length 	= disk_manager.getPieceLength();
				
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
				
				final long	avail_start 			= ( first_piece_num * piece_length ) + ( first_block * DiskManager.BLOCK_SIZE );
				final long	avail_end_inclusive 	= ( last_piece_num  * piece_length ) + ( last_block * DiskManager.BLOCK_SIZE ) + pieces[last_piece_num].getBlockSize( last_block ) - 1;
					
				if ( TRACE )System.out.println( first_piece_num + "/" + first_block + " - " + last_piece_num + "/" + last_block  + ": " + avail_start + "-" + avail_end_inclusive );
				
				for ( final SameSizeFileWrapper other_file: file_wrappers ){
					
					if ( other_file == this || other_file.isSkipped() || other_file.isComplete()){
						
						continue;
					}
					
					final DiskManager 		other_disk_manager = other_file.getDiskManager();
					final PEPeerManager 	other_peer_manager = other_file.getPeerManager();
					
					if ( other_disk_manager == null || other_peer_manager == null ){
						
						continue;
					}
					
					read_write_dispatcher.dispatch(
						new AERunnable()
						{
							public void
							runSupport()
							{		
								if ( other_file.isComplete()){
									
									return;
								}
								
								DiskManagerPiece[]	other_pieces = other_disk_manager.getPieces();
								
								long other_piece_length 	= other_disk_manager.getPieceLength();
				
								long	skew = file_byte_offset - other_file.getFileByteOffset();
								
								if ( skew % DiskManager.BLOCK_SIZE == 0 ){
									
										// special case of direct block->block mapping
									
									for ( long block_start = avail_start; block_start <= avail_end_inclusive; block_start += DiskManager.BLOCK_SIZE ){
				
										if ( destroyed ){
											
											break;
										}
										
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
				
												if ( buffer == null ){
													
													continue;
												}
												

												written = target_piece.getWritten();
												
												if ( target_piece.isDone() || (written != null && written[target_block_num])){
													
													continue;
												}
											
												try{
												
													boolean completed_piece = target_piece.getNbWritten() == target_piece.getNbBlocks() - 1;
													
													if ( TRACE )System.out.println( "Write from " + origin_piece_num + "/" + origin_block_num + " to " + target_piece_num + "/" + target_block_num );

													if ( other_file.writeBlock( target_piece_num, target_block_num, buffer )){
									
														buffer = null;
														
														if ( completed_piece ){
															
															pieces_completed++;
															
															if ( pieces_completed < 5 ){
																
																try{
																	Thread.sleep(500);
																	
																}catch( Throwable e ){
																	
																}
															}
														}													
													}else{
														
														break;
													}
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
										// unless this is the last block and short enough 
									
									DirectByteBuffer	prev_block 		= null;
									int					prev_block_pn	= 0;
									int					prev_block_bn	= 0;
									
									try{										
										for ( long block_start=avail_start; block_start <= avail_end_inclusive; block_start += DiskManager.BLOCK_SIZE ){
											
											if ( destroyed ){
												
												break;
											}
											
											long	origin_start 			= block_start;
											
											long target_offset = origin_start - skew;
											
											target_offset =  (( target_offset + DiskManager.BLOCK_SIZE - 1 ) / DiskManager.BLOCK_SIZE ) * DiskManager.BLOCK_SIZE;
											
											long origin_offset = target_offset + skew;
														
											int	target_piece_num 	= (int)( target_offset/other_piece_length );
											int	target_block_num	= (int)(( target_offset % other_piece_length ) / DiskManager.BLOCK_SIZE );
					
											DiskManagerPiece	target_piece = other_pieces[target_piece_num];
					
											boolean[]	target_written = target_piece.getWritten();
															
											if ( target_piece.isDone() || (target_written != null && ( target_block_num >= target_written.length || target_written[target_block_num]))){
												
												// already written or no such block
												
											}else{
											
												int target_block_size = target_piece.getBlockSize( target_block_num );
												
												if ( 	origin_offset >= file_byte_offset &&
														origin_offset + target_block_size <= avail_end_inclusive + 1){
												
													int	origin1_piece_number 	= (int)( origin_start/piece_length );
													int	origin1_block_num		= (int)(( origin_start % piece_length ) / DiskManager.BLOCK_SIZE );
					
													DiskManagerPiece	origin1_piece = pieces[origin1_piece_number];
													
													if ( !origin1_piece.isWritten( origin1_block_num )){
														
														continue;	// might have failed
													}
													
													DirectByteBuffer read_block1	= null;
													DirectByteBuffer read_block2	= null;
													DirectByteBuffer write_block	= null;
													
													try{
														if ( 	prev_block != null &&
																prev_block_pn == origin1_piece_number &&
																prev_block_bn == origin1_block_num ){
															
															read_block1 = prev_block;
															prev_block	= null;
																	
														}else{
														
															read_block1 = disk_manager.readBlock( origin1_piece_number , origin1_block_num*DiskManager.BLOCK_SIZE, origin1_piece.getBlockSize( origin1_block_num ));
															
															if ( read_block1 == null ){
																
																continue;
															}
														}
														
														write_block = DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_EXTERNAL, target_block_size );
														
														final byte SS = DirectByteBuffer.SS_EXTERNAL;
																
														int	delta = (int)( origin_offset - origin_start );
														
														read_block1.position( SS, delta );
														
															// readblock1 could have more bytes in it than the writeblock if writeblock is the last
															// block of the file
														
														int rb1_rem =  read_block1.remaining( SS );
														
														if ( rb1_rem > target_block_size ){
															
															read_block1.limit( SS, delta + target_block_size );
														}
														
														write_block.limit( SS, read_block1.remaining( SS ));
														
														write_block.put( SS, read_block1 );
														
														write_block.limit( SS, target_block_size );
														
														read_block1.returnToPool();
														
														read_block1 = null;
														
														if ( write_block.hasRemaining( SS )){
																												
															int	origin2_piece_number 	= origin1_piece_number;
															int	origin2_block_num		= origin1_block_num + 1;
															
															if ( origin2_block_num >= origin1_piece.getNbBlocks()){
																
																origin2_piece_number++;
																
																origin2_block_num = 0;
															}
							
															if ( origin2_piece_number < pieces.length ){
																
																DiskManagerPiece	origin2_piece = pieces[origin2_piece_number];
								
																if ( !origin2_piece.isWritten( origin2_block_num )){
																	
																	continue;
																}
																
																read_block2 = disk_manager.readBlock( origin2_piece_number , origin2_block_num*DiskManager.BLOCK_SIZE, origin2_piece.getBlockSize( origin2_block_num ));
																
																if ( read_block2 == null ){
																	
																	continue;
																}
																
																read_block2.limit( SS, write_block.remaining( SS ));
																
																write_block.put( SS, read_block2 );
																
																read_block2.position( SS, 0 );
																read_block2.limit( SS, read_block2.capacity( SS ));
																
																prev_block 		= read_block2;
																prev_block_pn	= origin2_piece_number;
																prev_block_bn	= origin2_block_num;
																		
																read_block2	= null;
															}
														}
														
														if ( write_block.hasRemaining( SS )){
															
															continue;
															
														}else{
															
															write_block.flip( SS );
																
															target_written = target_piece.getWritten();
																														
															if ( target_piece.isDone() || (target_written != null && target_written[target_block_num])){

																	// seems to have been done in the meantime
																
															}else{
																boolean completed_piece = target_piece.getNbWritten() == target_piece.getNbBlocks() - 1;
																
																if ( TRACE )System.out.println( "Write from " + origin_offset + "/" + delta + "/" + target_block_size + " to " + target_piece_num + "/" + target_block_num );
		
																if ( other_file.writeBlock( target_piece_num, target_block_num, write_block )){
																	
																	write_block = null;
																			
																	if ( completed_piece ){
																		
																		pieces_completed++;
																		
																		if ( pieces_completed < 5 ){
																			
																			try{
																				Thread.sleep(500);
																				
																			}catch( Throwable e ){
																				
																			}
																		}
																	}
																}else{
																	
																	break;
																}
															}
														}
													}finally{
														
														if ( read_block1 != null ){
															
															read_block1.returnToPool();
														}
														
														if ( read_block2 != null ){
															
															read_block2.returnToPool();
														}
														
														if ( write_block != null ){
															
															write_block.returnToPool();
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
						});
				}
			}
			
			private boolean
			writeBlock(
				int					piece_number,
				int					block_number,
				DirectByteBuffer	buffer )
			{
				PEPeerManager pm = getPeerManager();
			
				if ( pm == null ){
					
					return( false );
				}
				
				modified_pieces[ piece_number - file.getFirstPieceNumber() ] = true;
				
				int	length = buffer.remaining( DirectByteBuffer.SS_EXTERNAL );
				
				synchronized( merged_data_lock ){
					
					DownloadManagerState dms = download_manager.getDownloadState();
					
					long merged = dms.getLongAttribute( DownloadManagerState.AT_MERGED_DATA );
					
					merged += length;
					
					dms.setLongAttribute( DownloadManagerState.AT_MERGED_DATA, merged );
				}
				
				merged_byte_counnt += length;
				
				pm.writeBlock( piece_number, block_number*DiskManager.BLOCK_SIZE, buffer, "block-xfer from " + getID(), true );
				
				return( true );
			}

			private void
			pieceCorrupt(
				int		piece_number )
			{
				int	first_piece = file.getFirstPieceNumber();
				
				if ( piece_number >= first_piece && piece_number <= file.getLastPieceNumber()){
					
					if ( modified_pieces[ piece_number - first_piece ] ){
						
						pieces_corrupted++;
						
						if ( pieces_corrupted >= HASH_FAILS_BEFORE_QUIT ){
							
							abandon( this );
						}
					}
				}
			}
			
			private long
			getMergedByteCount()
			{
				return( merged_byte_counnt );
			}
		}
	}
	
	private interface
	DownloadManagerPeerListenerEx
		extends DownloadManagerPeerListener
	{
		public void
		sync();
	}
}
