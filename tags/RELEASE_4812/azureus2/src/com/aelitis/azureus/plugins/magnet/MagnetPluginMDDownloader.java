/*
 * Created on Mar 9, 2012
 * Created by Paul Gardner
 * 
 * Copyright 2012 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
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


package com.aelitis.azureus.plugins.magnet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.net.URL;
import java.util.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentAnnounceURLGroup;
import org.gudy.azureus2.core3.torrent.TOTorrentAnnounceURLSet;
import org.gudy.azureus2.core3.torrent.TOTorrentCreator;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AETemporaryFileHandler;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.RandomUtils;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.disk.DiskManagerChannel;
import org.gudy.azureus2.plugins.disk.DiskManagerEvent;
import org.gudy.azureus2.plugins.disk.DiskManagerListener;
import org.gudy.azureus2.plugins.disk.DiskManagerRequest;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.download.DownloadPeerListener;
import org.gudy.azureus2.plugins.peers.Peer;
import org.gudy.azureus2.plugins.peers.PeerEvent;
import org.gudy.azureus2.plugins.peers.PeerListener2;
import org.gudy.azureus2.plugins.peers.PeerManager;
import org.gudy.azureus2.plugins.peers.PeerManagerEvent;
import org.gudy.azureus2.plugins.peers.PeerManagerListener2;
import org.gudy.azureus2.plugins.utils.PooledByteBuffer;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;

public class 
MagnetPluginMDDownloader 
{
	private static Set<String>	active_set = new HashSet<String>();
	
	private PluginInterface		plugin_interface;
	private byte[]				hash;
	private String				args;
	
	private volatile boolean		started;
	private volatile boolean		cancelled;
	private volatile boolean		completed;
	
	private List<DiskManagerRequest>	requests = new ArrayList<DiskManagerRequest>();
	
	private AESemaphore running_sem 	= new AESemaphore( "MPMDD:run" );
	private AESemaphore complete_sem 	= new AESemaphore( "MPMDD:comp" );

	protected 
	MagnetPluginMDDownloader(
		PluginInterface		_plugin_interface,
		byte[]				_hash,
		String				_args )
	{
		plugin_interface	= _plugin_interface;
		hash				= _hash;
		args				= _args;
	}
	
	protected void
	start(
		final DownloadListener		listener )
	{
		synchronized( this ){
			
			if ( started ){
	
				listener.failed( new Exception( "Already started" ));
				
				return;
			}
			
			if ( cancelled || completed ){

				listener.failed( new Exception( "Already cancelled/completed" ));

				return;
			}
			
			started = true;
			
			new AEThread2( "MagnetPluginMDDownloader" )
			{
				public void
				run()
				{
					startSupport( listener );
				}
			}.start();
		}
	}
	
	protected void
	cancel()
	{
		cancelSupport( false );
	}
	
	private void
	cancelSupport(
		boolean	internal )
	{
		boolean	wait_for_complete 	= !internal;

		try{
			List<DiskManagerRequest>	to_cancel;
			
			synchronized( this ){
				
				if ( !started ){
					
					Debug.out( "Not started!" );
					
					wait_for_complete 	= false;
				}
				
				if ( cancelled || completed ){
					
					return;
				}
				
				cancelled	= true;
				
				to_cancel = new ArrayList<DiskManagerRequest>( requests );
				
				requests.clear();
			}
			
			for ( DiskManagerRequest request: to_cancel ){
				
				request.cancel();
			}
		}finally{
					
			running_sem.releaseForever();
			
			if ( wait_for_complete ){
				
				complete_sem.reserve();
			}
		}
	}
	
	private void
	startSupport(
		final DownloadListener		listener )
	{		
		String	hash_str = ByteFormatter.encodeString( hash );

		File tmp_dir 		= null;	
		File data_file 		= null;
		File torrent_file 	= null;

		DownloadManager download_manager = plugin_interface.getDownloadManager();

		Download download	= null;
		
		final Throwable[] error = { null };
		
		final ByteArrayOutputStream	result = new ByteArrayOutputStream(32*1024);
		
		TOTorrentAnnounceURLSet[]	url_sets = null;
		
		try{
			synchronized( active_set ){
				
				if ( active_set.contains( hash_str )){
					
					throw( new Exception( "Download already active for hash " + hash_str ));
				}
				
				active_set.add( hash_str );
			}
			
			Download existing_download = download_manager.getDownload( hash );
			
			if ( existing_download != null ){
				
				throw( new Exception( "download already exists" ));
			}
				
			tmp_dir = AETemporaryFileHandler.createTempDir();
			
			int	 rand = RandomUtils.generateRandomIntUpto( 10000 );
			
			data_file 		= new File( tmp_dir, hash_str + "_" + rand + ".torrent" );
			torrent_file 	= new File( tmp_dir, hash_str + "_" + rand + ".metatorrent" );

			RandomAccessFile raf = new RandomAccessFile( data_file, "rw" );
			
			byte[] buffer = new byte[256*1024];
			
			Arrays.fill( buffer, (byte)0xff );
			
			for (long i=0;i<32*1024*1024;i+=buffer.length){
				
				raf.write( buffer );
			}
			
			raf.close();
						
			URL announce_url = TorrentUtils.getDecentralisedURL( hash );
			
			TOTorrentCreator creator = 
				TOTorrentFactory.createFromFileOrDirWithFixedPieceLength( 
						data_file, 
						announce_url,
						16*1024 );
	
			TOTorrent meta_torrent = creator.create();
			
			String[] bits = args.split( "&" );
			
			List<String>	trackers = new ArrayList<String>();

			String	name = "magnet:" + Base32.encode( hash );
			
			for ( String bit: bits ){
				
				String[] x = bit.split( "=" );
				
				if ( x.length == 2 ){
					
					String 	lhs = x[0].toLowerCase();
					
					if ( lhs.equals( "tr" )){
						
						trackers.add( UrlUtils.decode( x[1] ));
						
					}else if ( lhs.equals( "dn" )){
						
						name = UrlUtils.decode( x[1] );
					}
				}
			}
			
			if ( trackers.size() > 0 ){
				
				trackers.add( 0, announce_url.toExternalForm());
				
				TOTorrentAnnounceURLGroup ag = meta_torrent.getAnnounceURLGroup();
				
				List<TOTorrentAnnounceURLSet> sets = new ArrayList<TOTorrentAnnounceURLSet>();
				
				for ( String tracker: trackers ){
				
					try{
						sets.add( ag.createAnnounceURLSet(new URL[]{ new URL( tracker )}));
						
					}catch( Throwable e ){
						
						Debug.out( e );
					}
				}
				
				if ( sets.size() > 0 ){
					
					url_sets = sets.toArray( new TOTorrentAnnounceURLSet[ sets.size()]);
					
					ag.setAnnounceURLSets( url_sets );
				}
			}
			
			if ( !data_file.delete()){
				
				throw( new Exception( "Failed to delete " + data_file ));
			}
			
			meta_torrent.setHashOverride( hash );
			
			TorrentUtils.setFlag( meta_torrent, TorrentUtils.TORRENT_FLAG_METADATA_TORRENT, true );
			
			TorrentUtils.setFlag( meta_torrent, TorrentUtils.TORRENT_FLAG_LOW_NOISE, true );
			
			meta_torrent.serialiseToBEncodedFile( torrent_file );
						
			download_manager.clearNonPersistentDownloadState( hash );
			
			download = download_manager.addNonPersistentDownloadStopped( PluginCoreUtils.wrap( meta_torrent ), torrent_file, data_file);
			
			String	display_name = MessageText.getString( "MagnetPlugin.use.md.download.name", new String[]{ name });
			
			PluginCoreUtils.unwrap( download ).getDownloadState().setDisplayName( display_name + ".torrent" );
			
			download.addPeerListener(
				new DownloadPeerListener()
				{
					public void
					peerManagerAdded(
						final Download			download,
						final PeerManager		peer_manager )
					{
						if ( cancelled || completed ){
							
							download.removePeerListener( this );
							
							return;
						}
						
						final PEPeerManager pm = PluginCoreUtils.unwrap( peer_manager );
						
						peer_manager.addListener(
							new PeerManagerListener2()
							{
								private PeerManagerListener2	pm_listener = this;
								
								private int	md_size;
								
								public void
								eventOccurred(
									PeerManagerEvent	event )
								{
									if ( cancelled || completed ){
										
										peer_manager.removeListener( this );
										
										return;
									}
									
									if ( event.getType() != PeerManagerEvent.ET_PEER_ADDED ){
										
										return;
									}
									
									final Peer peer = event.getPeer();
									
									peer.addListener(
										new PeerListener2()
										{
											public void
											eventOccurred(
												PeerEvent	event )
											{
												if ( cancelled || completed || md_size > 0 ){
													
													peer.removeListener( this );
													
													return;
												}
												
												if ( event.getType() != PeerEvent.ET_STATE_CHANGED ){
													
													return;
												}
												
												if ( (Integer)event.getData() != Peer.TRANSFERING ){
													
													return;
												}
												
												synchronized( this ){
													
													if ( md_size > 0 ){
														
														return;
													}
												
													md_size = pm.getTorrentInfoDictSize();
												
													if ( md_size > 0 ){
																								
														peer_manager.removeListener( pm_listener );
														
													}else{
														
														return;
													}
												}
										
												listener.reportProgress( 0, md_size );
												
												new AEThread2( "" )
												{
													public void
													run()
													{
														DiskManagerChannel channel = null;
														
														try{
															channel = download.getDiskManagerFileInfo()[0].createChannel();
														
															final DiskManagerRequest request = channel.createRequest();
															
															request.setType( DiskManagerRequest.REQUEST_READ );
															request.setOffset( 0 );
															request.setLength( md_size );
																
															request.setMaximumReadChunkSize( 16*1024 );
																				
															request.addListener(
																new DiskManagerListener()
																{
																	public void
																	eventOccurred(
																		DiskManagerEvent	event )
																	{
																		int	type = event.getType();
																		
																		if ( type == DiskManagerEvent.EVENT_TYPE_FAILED ){
																			
																			error[0]	= event.getFailure();
																			
																			running_sem.releaseForever();
																			
																		}else if ( type == DiskManagerEvent.EVENT_TYPE_SUCCESS ){
																			
																			PooledByteBuffer	buffer = null;
																			
																			try{
																				buffer	= event.getBuffer();
																																									
																				byte[]	bytes = buffer.toByteArray();
																					
																				int	dl_size;
																				
																				synchronized( MagnetPluginMDDownloader.this ){
																					
																					result.write( bytes );
																					
																					dl_size = result.size();
																					
																					if ( dl_size == md_size ){
																						
																						completed	= true;
																						
																						running_sem.releaseForever();
																					}
																				}
																				
																				if ( !completed ){
																					
																					listener.reportProgress( dl_size, md_size );
																				}
																				
																			}catch( Throwable e ){
																				
																				error[0] = e;
										
																				request.cancel();
																					
																				running_sem.releaseForever();
																				
																			}finally{
																				
																				if ( buffer != null ){
																					
																					buffer.returnToPool();
																				}
																			}
																		}else if ( type == DiskManagerEvent.EVENT_TYPE_BLOCKED ){
																			
																			//System.out.println( "Waiting..." );
																		}
																	}
																});
															
															synchronized( MagnetPluginMDDownloader.this ){
															
																if ( cancelled ){
																	
																	return;
																}
																
																requests.add( request );
															}
															
															request.run();	
															
															synchronized( MagnetPluginMDDownloader.this ){
																
																requests.remove( request );
															}
														}catch( Throwable e ){
														
															error[0] = e;
															
															running_sem.releaseForever();
															
														}finally{
															
															if ( channel != null ){
																
																channel.destroy();
															}
														}
													}
												}.start();
											}
										});
								}
							});
					}
					
					public void
					peerManagerRemoved(
						Download		download,
						PeerManager		peer_manager )
					{
					}
				});
		
			final Download f_download = download;
			
			DownloadManagerListener dl_listener = 
				new DownloadManagerListener()
				{
					public void
					downloadAdded(
						Download	download )
					{
					}
					
					public void
					downloadRemoved(
						Download	dl )
					{
						if ( dl == f_download ){
							
							if ( !( cancelled || completed )){
								
								error[0] = new Exception( "Download manually removed" );
								
								running_sem.releaseForever();
							}
						}
					}
				};
				
			download_manager.addListener( dl_listener, false );

			try{
			
				download.moveTo(1);		
				
				download.setForceStart( true );
				
				download.setFlag( Download.FLAG_DISABLE_AUTO_FILE_MOVE, true );
	
				running_sem.reserve();
				
			}finally{
				
				download_manager.removeListener( dl_listener );
			}
				
			if ( completed ){
			
				byte[]	bytes = result.toByteArray();
				
				Map	info = BDecoder.decode( bytes );
				
				Map	map = new HashMap();
				
				map.put( "info", info );
				
				TOTorrent torrent = TOTorrentFactory.deserialiseFromMap( map );
				
				byte[] final_hash = torrent.getHash();
				
				if ( !Arrays.equals( hash, final_hash )){
					
					throw( new Exception( "Metadata torrent hash mismatch: expected=" + ByteFormatter.encodeString( hash ) + ", actual=" + ByteFormatter.encodeString( final_hash )));
				}
				
				if ( url_sets != null ){
					
					torrent.setAnnounceURL( url_sets[0].getAnnounceURLs()[0] );
					
					torrent.getAnnounceURLGroup().setAnnounceURLSets( url_sets );
					
				}else{
					
					torrent.setAnnounceURL( TorrentUtils.getDecentralisedURL( hash ));
				}
				
				listener.complete( torrent );
				
			}else{
									
				if ( cancelled ){
					
					throw( new Exception( "Download cancelled" ));
				
				}else{
					
					cancelSupport( true );
					
					if ( error[0] != null ){
						
						throw( error[0] );

					}else{
					
						throw( new Exception( "Download terminated prematurely" ));
					}
				}
			}
		}catch( Throwable e ){
			
			boolean	was_cancelled = cancelled;
	
			cancelSupport( true );
	
			if ( !was_cancelled ){
			
				listener.failed( e );
			
				Debug.out( e );
			}
		}finally{
			
			try{
				if ( download != null ){
					
					try{
						download.stop();
						
					}catch( Throwable e ){
					}
					
					try{
						download.remove();
						
					}catch( Throwable e ){
						
						Debug.out( e );
					}
				}
				
				List<DiskManagerRequest>	to_cancel;
				
				synchronized( this ){
										
					to_cancel = new ArrayList<DiskManagerRequest>( requests );
					
					requests.clear();
				}
				
				for ( DiskManagerRequest request: to_cancel ){
					
					request.cancel();
				}
				
				if ( torrent_file != null ){
					
					torrent_file.delete();
				}
				
				if ( data_file != null ){
					
					data_file.delete();
				}
				
				if ( tmp_dir != null ){
					
					tmp_dir.delete();
				}
			}catch( Throwable e ){
				
				Debug.out( e );
				
			}finally{
			
				synchronized( active_set ){
	
					active_set.remove( hash_str );
				}
				
				complete_sem.releaseForever();
			}
		}
	}
	
	protected interface
	DownloadListener
	{
		public void
		reportProgress(
			int		downloaded,
			int		total_size );
		
		public void
		complete(
			TOTorrent	torrent );
		
		public void
		failed(
			Throwable e );
	}
}
