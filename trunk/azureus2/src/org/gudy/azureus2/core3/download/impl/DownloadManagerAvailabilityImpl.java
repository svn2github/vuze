/*
 * Created on Jun 16, 2014
 * Created by Paul Gardner
 * 
 * Copyright 2014 Azureus Software, Inc.  All rights reserved.
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


package org.gudy.azureus2.core3.download.impl;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.gudy.azureus2.core3.download.DownloadManagerAvailability;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentAnnounceURLSet;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncer;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerDataProvider;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerFactory;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraper;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.core3.util.AENetworkClassifier;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.tracker.TrackerPeerSource;
import com.aelitis.azureus.plugins.extseed.ExternalSeedPlugin;
import com.aelitis.azureus.plugins.tracker.dht.DHTTrackerPlugin;

public class 
DownloadManagerAvailabilityImpl
	implements DownloadManagerAvailability
{
	private  List<TrackerPeerSource>	peer_sources = new ArrayList<TrackerPeerSource>();
	
	private TRTrackerAnnouncer	tracker_client;
	
	public
	DownloadManagerAvailabilityImpl(
		final TOTorrent		to_torrent )
	{
		if ( to_torrent == null ){
			
			return;
		}
		
		Torrent	torrent = PluginCoreUtils.wrap( to_torrent );
		
		TOTorrentAnnounceURLSet[] sets = to_torrent.getAnnounceURLGroup().getAnnounceURLSets();
			
		if ( sets.length == 0 ){
			
			sets = new TOTorrentAnnounceURLSet[]{ to_torrent.getAnnounceURLGroup().createAnnounceURLSet( new URL[]{ to_torrent.getAnnounceURL()})};
		}
			  
		try{
			tracker_client = 
					TRTrackerAnnouncerFactory.create( 
							to_torrent, 
						new TRTrackerAnnouncerFactory.DataProvider()
						{
							public String[] 
							getNetworks()
							{
								return( AENetworkClassifier.AT_NETWORKS );
							}
						});
		
			tracker_client.setAnnounceDataProvider(
		    		new TRTrackerAnnouncerDataProvider()
		    		{
		    			public String
		    			getName()
		    			{
		    				return( "Availability checker" );
		    			}
		    			
		    			public long
		    			getTotalSent()
		    			{
		    				return( 0 );
		    			}
		    			
		    			public long
		    			getTotalReceived()
		    			{
		    				return( 0 );
		    			}
		    			
		    			public long
		    			getRemaining()
		    			{
		    				return( to_torrent.getSize());
		    			}
		    			
		    			public long
		    			getFailedHashCheck()
		    			{
		    				return( 0 );
		    			}

		    			public String
		    			getExtensions()
		    			{
		    				return( null );
		    			}
		    			
		    			public int
		    			getMaxNewConnectionsAllowed()
		    			{
		    				return( 1 );
		    			}
		    			
		    			public int
		    			getPendingConnectionCount()
		    			{
		    				return( 0 );
		    			}
		    			
		    			public int
		    			getConnectedConnectionCount()
		    			{
		    				return( 0 );
		    			}
		    			
		    			public int
		    			getUploadSpeedKBSec(
		    				boolean	estimate )
		    			{
		    				return( 0 );
		    			}
		    			
		    			public int
		    			getCryptoLevel()
		    			{
		    				return( NetworkManager.CRYPTO_OVERRIDE_NONE );
		    			}
		    			
		    			public boolean
		    			isPeerSourceEnabled(
		    				String		peer_source )
		    			{
		    				return( true );
		    			}
		    			
		    			public void
		    			setPeerSources(
		    				String[]	allowed_sources )
		    			{	
		    			}
		    		});
			
			tracker_client.update( true );
			
		}catch( Throwable e ){
			
			Debug.out( e );
		}
		
				// source per set
			
		for ( final TOTorrentAnnounceURLSet set: sets ){
			
			final URL[] urls = set.getAnnounceURLs();
			
			if ( urls.length == 0 || TorrentUtils.isDecentralised( urls[0] )){
				
				continue;
			}
			
			peer_sources.add( 
				new TrackerPeerSource()
				{
					private TrackerPeerSource _delegate;
					
 					private TRTrackerAnnouncer		ta;
  					private long					ta_fixup;

  					private long					last_scrape_fixup_time;
  					private Object[]				last_scrape;
  					
					private TrackerPeerSource
					fixup()
					{
						long	now = SystemTime.getMonotonousTime();
						
						if ( now - ta_fixup > 1000 ){
							
							TRTrackerAnnouncer current_ta = tracker_client;
							
							if ( current_ta == ta ){
								
								if ( current_ta != null && _delegate == null ){
									
									_delegate = current_ta.getTrackerPeerSource( set );
								}
							}else{
								
								if ( current_ta == null ){
									
									_delegate = null;
									
								}else{
									
									_delegate = current_ta.getTrackerPeerSource( set );
								}
								
								ta = current_ta;
							}
							
							ta_fixup	= now;
						}
						
						return( _delegate );
					}
					
					protected Object[]
					getScrape()
					{
						long now = SystemTime.getMonotonousTime();
						
						if ( now - last_scrape_fixup_time > 30*1000 || last_scrape == null ){

							
							
							last_scrape = new Object[]{ -1, -1, -1, -1, -1, "" }; 
							
							last_scrape_fixup_time = now;
						}
						
						return( last_scrape );
					}
					
					public int
					getType()
					{
						return( TrackerPeerSource.TP_TRACKER );
					}
					
					public String
					getName()
					{
						TrackerPeerSource delegate = fixup();
						
						if ( delegate == null ){
						
							return( urls[0].toExternalForm());
						}
						
						return( delegate.getName());
					}
					
					public int 
					getStatus()
					{
						TrackerPeerSource delegate = fixup();
						
						if ( delegate == null ){
						
							return( ST_STOPPED );
						}
						
						return( delegate.getStatus());
					}
					
					public String 
					getStatusString()
					{
						TrackerPeerSource delegate = fixup();
						
						if ( delegate == null ){
						
							return( (String)getScrape()[5] );
						}
						
						return( delegate.getStatusString());
					}
					
					public int
					getSeedCount()
					{
						TrackerPeerSource delegate = fixup();
						
						if ( delegate == null ){
						
							return((Integer)getScrape()[0] );
						}
						
						int seeds = delegate.getSeedCount();
						
						if ( seeds < 0 ){
							
							seeds = (Integer)getScrape()[0];
						}
						
						return( seeds );
					}
					
					public int
					getLeecherCount()
					{
						TrackerPeerSource delegate = fixup();
						
						if ( delegate == null ){
						
							return( (Integer)getScrape()[1] );
						}
						
						int leechers = delegate.getLeecherCount();
						
						if ( leechers < 0 ){
							
							leechers = (Integer)getScrape()[1];
						}
						
						return( leechers );						
					}

					public int
					getCompletedCount()
					{
						TrackerPeerSource delegate = fixup();
						
						if ( delegate == null ){
						
							return( (Integer)getScrape()[4] );
						}
						
						int comp = delegate.getCompletedCount();
						
						if ( comp < 0 ){
							
							comp = (Integer)getScrape()[4];
						}
						
						return( comp );						
					}
					
					public int
					getPeers()
					{
						TrackerPeerSource delegate = fixup();
						
						if ( delegate == null ){
						
							return( -1 );
						}
						
						return( delegate.getPeers());							
					}

					public int
					getInterval()
					{
						TrackerPeerSource delegate = fixup();
						
						if ( delegate == null ){
						
							Object[] si = getScrape();
							
							int	last 	= (Integer)si[2];
							int next	= (Integer)si[3];
							
							if ( last > 0 && next < Integer.MAX_VALUE && last < next ){
								
								return( next - last );
							}
							
							return( -1 );
						}
						
						return( delegate.getInterval());							
					}
					
					public int
					getMinInterval()
					{
						TrackerPeerSource delegate = fixup();
						
						if ( delegate == null ){
						
							return( -1 );
						}
						
						return( delegate.getMinInterval());							
					}
					
					public boolean
					isUpdating()
					{
						TrackerPeerSource delegate = fixup();
						
						if ( delegate == null ){
						
							return( false );
						}
						
						return( delegate.isUpdating());							
					}
					
					public int 
					getLastUpdate() 
					{
						TrackerPeerSource delegate = fixup();
						
						if ( delegate == null ){
						
							return( (Integer)getScrape()[2] );
						}
						
						return( delegate.getLastUpdate());
					}
					
					public int
					getSecondsToUpdate()
					{
						TrackerPeerSource delegate = fixup();
						
						if ( delegate == null ){
						
							return( -1 );
						}
						
						return( delegate.getSecondsToUpdate());
					}
					
					public boolean
					canManuallyUpdate()
					{
						TrackerPeerSource delegate = fixup();
						
						if ( delegate == null ){
							
							return( false );
						}
						
						return( delegate.canManuallyUpdate());
					}
					
					public void
					manualUpdate()
					{
						TrackerPeerSource delegate = fixup();
						
						if ( delegate != null ){
							
							delegate.manualUpdate();
						}
					}
					
					public boolean
					canDelete()
					{
						return( false );
					}
					
					public void
					delete()
					{
					}
				});
		}
		
			// http seeds
			
		try{
			ExternalSeedPlugin esp = DownloadManagerController.getExternalSeedPlugin();
			
			if ( esp != null ){
				  					
				peer_sources.add( esp.getTrackerPeerSource( torrent ));
			}
		}catch( Throwable e ){
		}
		
			// dht
		/*
		try{
			
			PluginInterface dht_pi = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByClass(DHTTrackerPlugin.class);

		    if ( dht_pi != null ){
		    	
		    	peer_sources.add(((DHTTrackerPlugin)dht_pi.getPlugin()).getTrackerPeerSource( plugin_download ));
		    }
		}catch( Throwable e ){
		}
		*/
	}
	
	public List<TrackerPeerSource>
	getTrackerPeerSources()
	{
		return( peer_sources );
	}
	
	public void
	destroy()
	{
		if ( tracker_client != null ){
			
			tracker_client.destroy();
		}
	}
}
