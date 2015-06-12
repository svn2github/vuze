/*
 * Created on Jun 16, 2014
 * Created by Paul Gardner
 * 
 * Copyright 2014 Azureus Software, Inc.  All rights reserved.
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
 */


package org.gudy.azureus2.core3.download.impl;

import java.net.URL;
import java.util.*;

import org.gudy.azureus2.core3.download.DownloadManagerAvailability;
import org.gudy.azureus2.core3.peer.PEPeerSource;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentAnnounceURLSet;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncer;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerDataProvider;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerFactory;
import org.gudy.azureus2.core3.util.AENetworkClassifier;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ipc.IPCException;
import org.gudy.azureus2.plugins.ipc.IPCInterface;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.tracker.TrackerPeerSource;
import com.aelitis.azureus.core.tracker.TrackerPeerSourceAdapter;
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
		TOTorrent				to_torrent,
		List<List<String>>		updated_trackers,
		final String[]			_enabled_peer_sources,
		final String[]			_enabled_networks )
	{
		if ( to_torrent == null ){
			
			return;
		}
	
		final Set<String>	enabled_peer_sources = new HashSet<String>( Arrays.asList( _enabled_peer_sources ));
		final Set<String>	enabled_networks	 = new HashSet<String>( Arrays.asList( _enabled_networks ));
				
		if ( enabled_peer_sources.contains( PEPeerSource.PS_BT_TRACKER)){
			
			TOTorrentAnnounceURLSet[] sets;
			
			if ( updated_trackers == null ){
				
				sets = to_torrent.getAnnounceURLGroup().getAnnounceURLSets();
				
			}else{
				
				sets = TorrentUtils.listToAnnounceSets( updated_trackers, to_torrent );
				
				try{
					to_torrent = TorrentUtils.cloneTorrent( to_torrent );
				
					TorrentUtils.setMemoryOnly( to_torrent, true );
					
					to_torrent.getAnnounceURLGroup().setAnnounceURLSets( sets );
					
				}catch( Throwable e ){
					
					Debug.out( e );
				}
			}
				
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
									return( _enabled_networks );
								}
							});
			
				final long torrent_size = to_torrent.getSize();
				
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
			    				return( torrent_size );
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
			    				return( 1 );	// num-want -> 1
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
							
								// we only asked for 1 peer so if we have a scrape value then we'll leave this blank
								// to avoid confusion
							
							if ( delegate.getSeedCount() > 0 || delegate.getLeecherCount() > 0 ){
								
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
		}
		
		Torrent	torrent = PluginCoreUtils.wrap( to_torrent );

			// http seeds
			
		try{
			ExternalSeedPlugin esp = DownloadManagerController.getExternalSeedPlugin();
			
			if ( esp != null ){
				  	
				TrackerPeerSource ext_ps = esp.getTrackerPeerSource( torrent );
				
				if ( ext_ps.getSeedCount() > 0 ){
				
					peer_sources.add( ext_ps );
				}
			}
		}catch( Throwable e ){
		}
		
		if ( 	enabled_peer_sources.contains( PEPeerSource.PS_DHT) &&
				enabled_networks.contains( AENetworkClassifier.AT_PUBLIC )){

				// dht
			
			if ( !torrent.isPrivate()){
				
				try{
					
					PluginInterface dht_pi = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByClass(DHTTrackerPlugin.class);
		
				    if ( dht_pi != null ){
				    	
				    	peer_sources.addAll( Arrays.asList(((DHTTrackerPlugin)dht_pi.getPlugin()).getTrackerPeerSources( torrent )));
				    }
				}catch( Throwable e ){
				}
			}
		}
		
		if ( 	enabled_peer_sources.contains( PEPeerSource.PS_DHT)){
				// enabled_networks.contains( AENetworkClassifier.AT_I2P )){

				// always do this availability check for the moment
			
			if ( !torrent.isPrivate()){
			
				try{
					PluginInterface i2p_pi = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByID( "azneti2phelper", true );
					
					if ( i2p_pi != null ){
						
						IPCInterface ipc = i2p_pi.getIPC();
						
						Map<String,Object>	options = new HashMap<String, Object>();
						
						options.put( "peer_networks", _enabled_networks );
						
						final int[] lookup_status = new int[]{ TrackerPeerSource.ST_INITIALISING, -1, -1, -1 };
						
						IPCInterface callback =
							new IPCInterface()
							{
								public Object 
								invoke(
									String methodName, 
									Object[] params) 
										
									throws IPCException
								{
									if ( methodName.equals( "statusUpdate" )){
										
										synchronized( lookup_status ){
											
											lookup_status[0] = (Integer)params[0];
											
											if ( params.length >= 4 ){
												
												lookup_status[1] = (Integer)params[1];
												lookup_status[2] = (Integer)params[2];
												lookup_status[3] = (Integer)params[3];
											}
										}
									}
									
									return( null );
								}
	
								public boolean 
								canInvoke( 
									String methodName, 
									Object[] params )
								{
									return( true );
								}
							};
							
						TrackerPeerSource	ps = new
							TrackerPeerSourceAdapter() 
							{
								public int
								getType()
								{
									return( TP_DHT );
								}
								
								public String
								getName()
								{
									return( "I2P DHT" );
								}
								
								public int
								getStatus()
								{
									synchronized( lookup_status ){
										
										return( lookup_status[0] );
									}
								}
								
								public int
								getSeedCount()
								{
									synchronized( lookup_status ){
									
										int	seeds 		= lookup_status[1];
										int peers 		= lookup_status[3];
										
										if ( seeds == 0 && peers > 0 ){
											
											return( -1 );
										}
										
										return( seeds );
									}
								}
								
								public int
								getLeecherCount()
								{
									synchronized( lookup_status ){
										
										int leechers 	= lookup_status[2];
										int peers 		= lookup_status[3];
										
										if ( leechers == 0 && peers > 0 ){
											
											return( -1 );
										}
										
										return( leechers );
									}
								}
								
								public int
								getPeers()
								{
									synchronized( lookup_status ){
										
										int peers = lookup_status[3];
										
										return( peers==0?-1:peers );
									}
								}
							};
							
							
						ipc.invoke(
							"lookupTorrent",
							new Object[]{
								"Availability lookup for '" + torrent.getName() + "'",
								torrent.getHash(),
								options,
								callback 
							});
						
						peer_sources.add( ps );
					}
				}catch( Throwable e ){
				}
			}
		}
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
