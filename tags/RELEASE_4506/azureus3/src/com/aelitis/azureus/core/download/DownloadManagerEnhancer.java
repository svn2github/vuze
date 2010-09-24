/*
 * Created on 1 Nov 2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */


package com.aelitis.azureus.core.download;

import java.util.*;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManagerListener;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.pluginsimpl.local.disk.DiskManagerChannelImpl;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.util.ExternalStimulusHandler;
import com.aelitis.azureus.util.ExternalStimulusListener;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.disk.DiskManagerChannel;

public class 
DownloadManagerEnhancer 
{
	public static final int	TICK_PERIOD				= 1000;
	
	private static DownloadManagerEnhancer		singleton;
	
	public static synchronized DownloadManagerEnhancer
	initialise(
		AzureusCore		core )
	{
		if ( singleton == null ){
			
			singleton	= new DownloadManagerEnhancer( core );
		}
		
		return( singleton );
	}
	
	public static synchronized DownloadManagerEnhancer
	getSingleton()
	{
		return( singleton );
	}
	
	private AzureusCore		core;
	
	private Map<DownloadManager,EnhancedDownloadManager>		download_map = new HashMap<DownloadManager,EnhancedDownloadManager>();
	
	private boolean			progressive_enabled;
	
	protected
	DownloadManagerEnhancer(
		AzureusCore	_core )
	{
		core	= _core;
		
		core.getGlobalManager().addListener(
			new GlobalManagerListener()
			{
				public void
				downloadManagerAdded(
					DownloadManager	dm )
				{
					// Don't auto-add to download_map. getEnhancedDownload will
					// take care of it later if we ever need the download
				}
					
				public void
				downloadManagerRemoved(
					DownloadManager	dm )
				{
					EnhancedDownloadManager	edm;
					
					synchronized( download_map ){
						
						edm = download_map.remove( dm );
					}
					
					if ( edm != null ){
						
						edm.destroy();
					}
				}
					
				public void
				destroyInitiated()
				{
					// resume any downloads we paused
					core.getGlobalManager().resumeDownloads();
				}
					
				public void
				destroyed()
				{
				}
			  
			    public void 
			    seedingStatusChanged( 
			    	boolean seeding_only_mode, boolean b )
			    {
			    }
			});
		
		ExternalStimulusHandler.addListener(
			new ExternalStimulusListener()
			{
				public boolean
				receive(
					String		name,
					Map			values )
				{
					return( false );
				}
				
				public int
				query(
					String		name,
					Map			values )
				{
					if ( name.equals( "az3.downloadmanager.stream.eta" )){
						
						Object	hash = values.get( "hash" );
						
						byte[]	b_hash = null;
						
						if ( hash instanceof String ){
							
							String	hash_str = (String)hash;
							
							if ( hash_str.length() == 32 ){
								
								b_hash = Base32.decode( hash_str );
								
							}else{
							
								b_hash = ByteFormatter.decodeString( hash_str );
							}
						}
						
						if ( b_hash != null ){
						
								// ensure we have an enhanced download object for it
							
							getEnhancedDownload( b_hash );
						}
						
						synchronized( download_map ){

							Iterator<EnhancedDownloadManager> it = download_map.values().iterator();
							
							while( it.hasNext()){
								
								EnhancedDownloadManager	edm = it.next();
								
								if ( b_hash != null ){
									
									byte[]	d_hash = edm.getHash();
									
									if ( d_hash != null && Arrays.equals( b_hash, d_hash )){
									
											// if its complete then obviously 0
										
										if ( edm.getDownloadManager().isDownloadComplete( false )){
											
											return( 0 );
										}

										if ( !edm.supportsProgressiveMode()){											
											
											return( Integer.MIN_VALUE );
										}
										
										if ( !edm.getProgressiveMode()){
										
											edm.setProgressiveMode( true );
										}
										
										long eta = edm.getProgressivePlayETA();
										
										if ( eta > Integer.MAX_VALUE ){
											
											return( Integer.MAX_VALUE );
										}
										
										return((int)eta);
									}
								}else{
									
									if ( edm.getProgressiveMode()){
										
										long eta = edm.getProgressivePlayETA();
										
										if ( eta > Integer.MAX_VALUE ){
											
											return( Integer.MAX_VALUE );
										}
										
										return((int)eta);
									}
								}
							}
						}
					}
					
					return( Integer.MIN_VALUE );
				}
			});
		
		SimpleTimer.addPeriodicEvent(
				"DownloadManagerEnhancer:speedChecker",
				TICK_PERIOD,
				new TimerEventPerformer()
				{
					private int tick_count;
					
					public void 
					perform(
						TimerEvent event ) 
					{
						tick_count++;
						
						List	downloads = core.getGlobalManager().getDownloadManagers();
						
						for ( int i=0;i<downloads.size();i++){
							
							DownloadManager download = (DownloadManager)downloads.get(i);
							
							int	state = download.getState();
							
							if ( 	state == DownloadManager.STATE_DOWNLOADING ||
									state == DownloadManager.STATE_SEEDING ){
								
								EnhancedDownloadManager edm = getEnhancedDownload( download );
								
								if ( edm == null ){
									
									return;
								}
								
								edm.updateStats( tick_count );
							
							}
						}
					}
				});
		
			// listener to pick up on streams kicked off externally
		
		DiskManagerChannelImpl.addListener(
			new DiskManagerChannelImpl.channelCreateListener()
			{
				public void
				channelCreated(
					final DiskManagerChannel	channel )
				{
					try{
						final EnhancedDownloadManager edm = 
							getEnhancedDownload(
									PluginCoreUtils.unwrap(channel.getFile().getDownload()));

						if ( edm == null ){
							
							return;
						}

						if ( edm.getDownloadManager().isDownloadComplete( true )){
						
							return;
						}
								
						if ( !edm.getProgressiveMode()){
							
							if ( edm.supportsProgressiveMode()){
								
								Debug.out( "Enabling progressive mode for '" + edm.getName() + "' due to external stream" );
								
								edm.setProgressiveMode( true );
							}
						}
					}catch( Throwable e ){
						
						Debug.printStackTrace(e);
					}
				}
			});
	}
	
	protected AzureusCore
	getCore()
	{
		return( core );
	}
	
	public EnhancedDownloadManager
	getEnhancedDownload(
		byte[]			hash )
	{
		DownloadManager dm = core.getGlobalManager().getDownloadManager(new HashWrapper( hash ));
		
		if ( dm == null ){
			
			return( null );
		}
		
		return( getEnhancedDownload( dm ));
	}
	
	public EnhancedDownloadManager
	getEnhancedDownload(
		DownloadManager	manager )
	{
		TOTorrent torrent = manager.getTorrent();
		
		if ( torrent == null ){
			
			return( null );
		}
		
		DownloadManager dm2 = manager.getGlobalManager().getDownloadManager( torrent );
		
		if ( dm2 != manager ){
			
			return null;
		}

		synchronized( download_map ){
			
			EnhancedDownloadManager	res = (EnhancedDownloadManager)download_map.get( manager );
			
			if ( res == null ){
				
				res = new EnhancedDownloadManager( DownloadManagerEnhancer.this, manager );
				
				download_map.put( manager, res );
			}
			
			return( res );
		}
	}
	
	public boolean
	isProgressiveAvailable()
	{
		if ( progressive_enabled ){
			
			return( true );
		}
	
		PluginInterface	ms_pi = core.getPluginManager().getPluginInterfaceByID( "azupnpav", true );
		
		if ( ms_pi != null ){
			
			progressive_enabled = true;
		}
		
		return( progressive_enabled );
	}
	
	/**
	 * @param hash
	 * @return 
	 *
	 * @since 3.0.1.7
	 */
	public DownloadManager findDownloadManager(String hash) {
		synchronized (download_map) {

			for (Iterator<DownloadManager> iter = download_map.keySet().iterator(); iter.hasNext();) {
				DownloadManager dm = iter.next();

				TOTorrent torrent = dm.getTorrent();
				if (PlatformTorrentUtils.isContent(torrent, true)) {
					String thisHash = PlatformTorrentUtils.getContentHash(torrent);
					if (hash.equals(thisHash)) {
						return dm;
					}
				}
			}
		}
		return null;
	}
}
