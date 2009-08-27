/*
 * Created on Jan 28, 2009
 * Created by Paul Gardner
 * 
 * Copyright 2009 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.core.devices.impl;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerPiece;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.devices.*;
import com.aelitis.azureus.core.security.CryptoManagerFactory;
import com.aelitis.net.upnp.UPnPDevice;
import com.aelitis.net.upnp.services.UPnPOfflineDownloader;

public class 
DeviceOfflineDownloaderImpl
	extends DeviceUPnPImpl
	implements DeviceOfflineDownloader
{
	public static final int	UPDATE_MILLIS	= 30*1000;
	public static final int UPDATE_TICKS	= UPDATE_MILLIS/DeviceManagerImpl.DEVICE_UPDATE_PERIOD;
	
	public static final String	client_id = ByteFormatter.encodeString( CryptoManagerFactory.getSingleton().getSecureID());
	
	private UPnPOfflineDownloader		service;
	
	protected
	DeviceOfflineDownloaderImpl(
		DeviceManagerImpl			_manager,
		UPnPDevice					_device,
		UPnPOfflineDownloader		_service )
	{
		super( _manager, _device, Device.DT_OFFLINE_DOWNLOADER );
		
		service	= _service;
	}
	
	protected
	DeviceOfflineDownloaderImpl(
		DeviceManagerImpl	_manager,
		Map					_map )
	
		throws IOException
	{
		super(_manager, _map );
	}
	
	protected boolean
	updateFrom(
		DeviceImpl		_other,
		boolean			_is_alive )
	{
		if ( !super.updateFrom( _other, _is_alive )){
			
			return( false );
		}
		
		if ( !( _other instanceof DeviceOfflineDownloaderImpl )){
			
			Debug.out( "Inconsistent" );
			
			return( false );
		}
		
		DeviceOfflineDownloaderImpl other = (DeviceOfflineDownloaderImpl)_other;
			
		if ( service == null && other.service != null ){
			
			service = other.service;
		}
		
		return( true );
	}
	
	protected void 
	updateStatus(
		int tick_count ) 
	{
		super.updateStatus( tick_count );
		
		if ( service == null ){
			
			return;
		}
		
		if ( tick_count % UPDATE_TICKS != 0 ){
			
			return;
		}
		
		updateDownloads();
	}
	
	protected void
	updateDownloads()
	{
		AzureusCore core = getManager().getAzureusCore();
		
		if ( core == null ){
			
			return;
		}

		if ( !isAlive()){
			
			return;
		}
		
		Map<String,byte[]>	old_cache 	= (Map<String,byte[]>)getPersistentMapProperty( PP_OD_STATE_CACHE, new HashMap<String,byte[]>());
		
		Map<String,byte[]>	new_cache 	= new HashMap<String, byte[]>();
		
		List<DownloadManager> downloads = core.getGlobalManager().getDownloadManagers();
		
		Map<DownloadManager,byte[]>	download_map = new HashMap<DownloadManager, byte[]>();
		
		for ( DownloadManager download: downloads ){
						
			int	state = download.getState();
			
			if ( 	state == DownloadManager.STATE_SEEDING ||
					state == DownloadManager.STATE_ERROR ){
				
				continue;
			}
			
			if ( state == DownloadManager.STATE_STOPPED ){
				
				if ( !download.isPaused()){
					
					continue;
				}
			}

			if ( state == DownloadManager.STATE_QUEUED ){
				
				if ( download.isDownloadComplete( false )){
					
					continue;
				}
			}
			
				// download is interesting 
			
			TOTorrent torrent = download.getTorrent();

			if ( torrent == null ){
				
				continue;
			}
			
			try{
				byte[] hash = torrent.getHash();
				
				String	hash_str = ByteFormatter.encodeString( hash );
				
				DiskManager disk = download.getDiskManager();
				
				if ( disk == null ){
					
					byte[] existing = old_cache.get( hash_str );
					
					if ( existing != null ){
						
						new_cache.put( hash_str, existing );
						
						download_map.put( download, existing );
					}
				}else{
				
					DiskManagerPiece[] pieces = disk.getPieces();
					
					byte[] needed = new byte[( pieces.length + 7 ) / 8];
					
					int	needed_pos		= 0;
					int	current_byte	= 0;
					int	pos 			= 0;
					
					int	hits = 0;
					
					for ( DiskManagerPiece piece: pieces ){
						
						current_byte = current_byte << 1;
						
						if ( piece.isNeeded() && !piece.isDone()){
							
							current_byte += 1;
							
							hits++;
						}
						
						if (( pos %8 ) == 7 ){
							
							needed[needed_pos++] = (byte)current_byte;
							
							current_byte = 0;
						}
						pos++;
					}
					
					if (( pos % 8 ) != 0 ){
						
						needed[needed_pos++] = (byte)(current_byte << (8 - (pos % 8)));
					}
					
					if ( hits > 0 ){
						
						new_cache.put( hash_str, needed );
						
						download_map.put( download, needed );
						
						System.out.println( hash_str + " (pieces=" + pieces.length + ") -> " + ByteFormatter.encodeString( needed ));
					}
				}
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
		
			// store this so we have consistent record for downloads that queue/pause etc and therefore lose accessible piece details
		
		setPersistentMapProperty( PP_OD_STATE_CACHE, new_cache );
		
			// sort by download priority
		
		List<Map.Entry<DownloadManager, byte[]>> entries = new ArrayList<Map.Entry<DownloadManager,byte[]>>( download_map.entrySet());
		
		Collections.sort(
			entries,
			new Comparator<Map.Entry<DownloadManager, byte[]>>()
			{
				public int 
				compare(
					Map.Entry<DownloadManager, byte[]> o1,
					Map.Entry<DownloadManager, byte[]> o2) 
				{
					return( o1.getKey().getPosition() - o2.getKey().getPosition());
				} 
			});
			
		String	download_hashes = "";
		
		Iterator<Map.Entry<DownloadManager, byte[]>> it = entries.iterator();
		
		while( it.hasNext()){
			
			Map.Entry<DownloadManager, byte[]> entry = it.next();
			
			DownloadManager	download = entry.getKey();
			
			try{
				String hash = ByteFormatter.encodeString( download.getTorrent().getHash());
				
				download_hashes += ( download_hashes.length()==0?"":"," ) + hash;
				
			}catch( Throwable e ){
				
				log( "Failed to get download hash", e );
				
				it.remove();
			}
		}
		
		try{
			String[] set_dl_results = service.setDownloads( client_id, download_hashes );
			
			String	set_dl_result	= set_dl_results[0];
			String	set_dl_status 	= set_dl_results[1];
			
			if ( !set_dl_status.equals( "OK" )){
				
				throw( new Exception( "Failing result returned: " + set_dl_status ));
			}
			
			String[]	bits = set_dl_result.split( "," );
			
			if ( bits.length != entries.size()){
				
				log( "setDownloads returned an invalid number of results (hashes=" + entries.size() + ",result=" + set_dl_result );
				
			}else{
				
				it = entries.iterator();
				
				int	pos = 0;
				
				while( it.hasNext()){
					
					Map.Entry<DownloadManager, byte[]> entry = it.next();
					
					DownloadManager	download = entry.getKey();
					
					try{
						TOTorrent torrent = download.getTorrent();

						String hash_str = ByteFormatter.encodeString( torrent.getHash());
						
						int	status = Integer.parseInt( bits[ pos++ ]);
	
						boolean	do_update = false;
					
						if ( status == 0 ){
						
							do_update = true;
						
						}else if ( status == 1 ){
						
								// need to add the torrent
						
							try{
						
								String add_result = 
									service.addDownload( 
										client_id, 
										hash_str,
										ByteFormatter.encodeStringFully( BEncoder.encode( torrent.serialiseToMap())));
								
								if ( add_result.equals( "OK" )){
									
									do_update = true;
									
								}else{
									
									throw( new Exception( "Failed to add download: " + add_result ));
								}
							}catch( Throwable e ){
								
									// TODO: prevent continual attempts to add same torrent?
								
								log( "Failed to add download", e );
							}
						}else{
						
							log( "setDownloads: error status returned for " + download.getDisplayName() + " - " + status );
						}
				
						if ( do_update ){
				
							try{
								String	required_bitfield = ByteFormatter.encodeStringFully( entry.getValue());
								
								String[] update_results = 
									service.updateDownload( 
										client_id, 
										hash_str,
										required_bitfield );
									
								String	have_bitfield	= update_results[0];
								String	update_status 	= update_results[1];
								
								if ( !update_status.equals( "OK" )){
									
									throw( new Exception( "Failing result returned: " + update_status ));
								}
								
								System.out.println( "have bitfield: " + have_bitfield );
								
							}catch( Throwable e ){
						
								log( "updateDownload failed for " + download.getDisplayName(), e );
							}
						}
					}catch( Throwable e ){
					
						log( "Processing failed for " + download.getDisplayName(), e );
					}
				}
			}
			
		}catch( Throwable e ){
			
			log( "setDownloads failed", e );
		}
	}
	
	protected void
	log(
		String	str )
	{
		super.log( "OfflineDownloader: " + str );
	}
	
	protected void
	log(
		String		str,
		Throwable	e )
	{
		super.log( "OfflineDownloader: " + str, e );
	}
}
