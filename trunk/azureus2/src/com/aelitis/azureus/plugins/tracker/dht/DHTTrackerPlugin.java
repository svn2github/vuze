/*
 * Created on 31-Jan-2005
 * Created by Paul Gardner
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.plugins.tracker.dht;


import java.net.URL;
import java.net.UnknownHostException;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPeerSource;
import org.gudy.azureus2.core3.tracker.protocol.PRHelpers;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AENetworkClassifier;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadAnnounceResult;
import org.gudy.azureus2.plugins.download.DownloadAnnounceResultPeer;
import org.gudy.azureus2.plugins.download.DownloadAttributeListener;
import org.gudy.azureus2.plugins.download.DownloadListener;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.download.DownloadScrapeResult;
import org.gudy.azureus2.plugins.download.DownloadTrackerListener;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;
import org.gudy.azureus2.plugins.peers.Peer;
import org.gudy.azureus2.plugins.peers.PeerManager;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.plugins.utils.DelayedTask;
import org.gudy.azureus2.plugins.utils.UTTimerEvent;
import org.gudy.azureus2.plugins.utils.UTTimerEventPerformer;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;

import com.aelitis.azureus.core.dht.netcoords.DHTNetworkPosition;
import com.aelitis.azureus.core.dht.netcoords.DHTNetworkPositionManager;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdmin;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminASN;
import com.aelitis.azureus.plugins.dht.*;

/**
 * @author parg
 *
 */

public class 
DHTTrackerPlugin 
	implements Plugin, DownloadListener, DownloadAttributeListener, DownloadTrackerListener
{
	private static final String	PLUGIN_NAME				= "Distributed Tracker";
	private static final String PLUGIN_CONFIGSECTION_ID = "plugins.dhttracker";
	
	private static final int	ANNOUNCE_TIMEOUT	= 2*60*1000;
	private static final int	SCRAPE_TIMEOUT		= 30*1000;
	
	private static final int	ANNOUNCE_MIN_DEFAULT		= 2*60*1000;
	private static final int	ANNOUNCE_MAX				= 60*60*1000;
	private static final int	ANNOUNCE_MAX_DERIVED_ONLY	= 30*60*1000;
	
	private static final int	INTERESTING_CHECK_PERIOD		= 4*60*60*1000;
	private static final int	INTERESTING_INIT_RAND_OURS		=    5*60*1000;
	private static final int	INTERESTING_INIT_MIN_OURS		=    2*60*1000;
	private static final int	INTERESTING_INIT_RAND_OTHERS	=   30*60*1000;
	private static final int	INTERESTING_INIT_MIN_OTHERS		=    5*60*1000;

	private static final int	INTERESTING_AVAIL_MAX		= 8;	// won't pub if more
	private static final int	INTERESTING_PUB_MAX_DEFAULT	= 30;	// limit on pubs
	
	private static final int	REG_TYPE_NONE			= 1;
	private static final int	REG_TYPE_FULL			= 2;
	private static final int	REG_TYPE_DERIVED		= 3;
	
	private static final int	LIMITED_TRACK_SIZE		= 16;
	
	private static final boolean	TRACK_NORMAL_DEFAULT	= true;
	private static final boolean	TRACK_LIMITED_DEFAULT	= true;
	
	public static final int	NUM_WANT			= 30;	// Limit to ensure replies fit in 1 packet

	private static final long	start_time = SystemTime.getCurrentTime();
	
	private static final Object	DL_DERIVED_METRIC_KEY		= new Object();
	private static final int	DL_DERIVED_MIN_TRACK		= 5;
	private static final int	DL_DERIVED_MAX_TRACK		= 20;
	private static final int	DIRECT_INJECT_PEER_MAX		= 5;
	
	private static URL	DEFAULT_URL;
	
	static{
		try{
			DEFAULT_URL = new URL( "dht:" );
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
	
	private PluginInterface		plugin_interface;
	
	private DHTPlugin			dht;
	
	private TorrentAttribute 	ta_networks;
	private TorrentAttribute 	ta_peer_sources;

	private Map					interesting_downloads 	= new HashMap();
	private int					interesting_published	= 0;
	private int					interesting_pub_max		= INTERESTING_PUB_MAX_DEFAULT;
	private Map					running_downloads 		= new HashMap();
	private Map					registered_downloads 	= new HashMap();
	
	private Map					limited_online_tracking	= new HashMap();
	private Map					query_map			 	= new HashMap();
	
	private Map					in_progress				= new HashMap();
	
	private BooleanParameter	track_normal_when_offline;
	private BooleanParameter	track_limited_when_online;
	
	private LoggerChannel		log;
	
	private Map					scrape_injection_map = new WeakHashMap();
	
	private AEMonitor	this_mon	= new AEMonitor( "DHTTrackerPlugin" );
	
	public static void
	load(
		PluginInterface		plugin_interface )
	{
		plugin_interface.getPluginProperties().setProperty( "plugin.version", 	"1.0" );
		plugin_interface.getPluginProperties().setProperty( "plugin.name", 		PLUGIN_NAME );
	}
	
	public void
	initialize(
		PluginInterface 	_plugin_interface )
	{
		plugin_interface	= _plugin_interface;
				
		log = plugin_interface.getLogger().getTimeStampedChannel(PLUGIN_NAME);
		
		ta_networks 	= plugin_interface.getTorrentManager().getAttribute( TorrentAttribute.TA_NETWORKS );
		ta_peer_sources = plugin_interface.getTorrentManager().getAttribute( TorrentAttribute.TA_PEER_SOURCES );

		UIManager	ui_manager = plugin_interface.getUIManager();

		final BasicPluginViewModel model = 
			ui_manager.createBasicPluginViewModel( PLUGIN_NAME);
		
		model.setConfigSectionID(PLUGIN_CONFIGSECTION_ID);
		
		BasicPluginConfigModel	config = 
			ui_manager.createBasicPluginConfigModel( ConfigSection.SECTION_PLUGINS, 
					PLUGIN_CONFIGSECTION_ID);
			
		track_normal_when_offline = config.addBooleanParameter2( "dhttracker.tracknormalwhenoffline", "dhttracker.tracknormalwhenoffline", TRACK_NORMAL_DEFAULT );

		track_limited_when_online = config.addBooleanParameter2( "dhttracker.tracklimitedwhenonline", "dhttracker.tracklimitedwhenonline", TRACK_LIMITED_DEFAULT );

		track_limited_when_online.addListener(
			new ParameterListener()
			{
				public void
				parameterChanged(
					Parameter	param )
				{
					configChanged();
				}
			});
		
		track_normal_when_offline.addListener(
			new ParameterListener()
			{
				public void
				parameterChanged(
					Parameter	param )
				{
					track_limited_when_online.setEnabled( track_normal_when_offline.getValue());

					configChanged();											
				}
			});
		
		if ( !track_normal_when_offline.getValue()){
			
			track_limited_when_online.setEnabled( false );
		}
		
		interesting_pub_max = plugin_interface.getPluginconfig().getPluginIntParameter( "dhttracker.presencepubmax", INTERESTING_PUB_MAX_DEFAULT );
		
		
		if ( !TRACK_NORMAL_DEFAULT ){
			// should be TRUE by default
			System.out.println( "**** DHT Tracker default set for testing purposes ****" );
		}
		
		
		model.getActivity().setVisible( false );
		model.getProgress().setVisible( false );
		
		model.getLogArea().setMaximumSize( 80000 );
		
		log.addListener(
				new LoggerChannelListener()
				{
					public void
					messageLogged(
						int		type,
						String	message )
					{
						model.getLogArea().appendText( message+"\n");
					}
					
					public void
					messageLogged(
						String		str,
						Throwable	error )
					{
						model.getLogArea().appendText( error.toString()+"\n");
					}
				});

		model.getStatus().setText( "Initialising" );
		
		log.log( "Waiting for Distributed Database initialisation" );
		
		plugin_interface.addListener(
			new PluginListener()
			{
				public void
				initializationComplete()
				{
					final PluginInterface dht_pi = 
						plugin_interface.getPluginManager().getPluginInterfaceByClass(
									DHTPlugin.class );
					
					if ( dht_pi != null ){
						
						dht = (DHTPlugin)dht_pi.getPlugin();
						
						final DelayedTask dt = plugin_interface.getUtilities().createDelayedTask(new Runnable()
							{
								public void 
								run() 
								{
									AEThread2	t = 
										new AEThread2( "DHTTrackerPlugin:init", true )
										{
											public void
											run()
											{
												try{
												
													if ( dht.isEnabled()){
													
														log.log( "DDB Available" );
															
														model.getStatus().setText( "Running" );
														
														initialise();
															
													}else{
														
														log.log( "DDB Disabled" );
														
														model.getStatus().setText( "Disabled, Distributed database not available" );
															
														notRunning();
													}
												}catch( Throwable e ){
														
													log.log( "DDB Failed", e );
														
													model.getStatus().setText( "Failed" );
														
													notRunning();
												}
											}
										};
																	
										t.start();	
										
								}
							});
						
						dt.queue();

					}else{
						
						log.log( "DDB Plugin missing" );
						
						model.getStatus().setText( "Failed" );
						
						notRunning();
					}
				}
				
				public void
				closedownInitiated()
				{
					
				}
				
				public void
				closedownComplete()
				{
					
				}
			});
	}
	
	protected void
	notRunning()
	{
		plugin_interface.getDownloadManager().addListener(
				new DownloadManagerListener()
				{
					public void
					downloadAdded(
						final Download	download )
					{
						Torrent	torrent = download.getTorrent();
						
						if ( torrent != null && torrent.isDecentralised()){
							
							download.addListener(
								new DownloadListener()
								{
									public void
									stateChanged(
										final Download		download,
										int					old_state,
										int					new_state )
									{
										int	state = download.getState();
										
										if ( 	state == Download.ST_DOWNLOADING ||
												state == Download.ST_SEEDING ){
											
											download.setAnnounceResult(
												new DownloadAnnounceResult()
												{
													public Download
													getDownload()
													{
														return( download );
													}
																								
													public int
													getResponseType()
													{
														return( DownloadAnnounceResult.RT_ERROR );
													}
																							
													public int
													getReportedPeerCount()
													{
														return( 0 );
													}
													
												
													public int
													getSeedCount()
													{
														return( 0 );
													}
													
													public int
													getNonSeedCount()
													{
														return( 0 );
													}
													
													public String
													getError()
													{
														return( "Distributed Database Offline" );
													}
																								
													public URL
													getURL()
													{
														return( download.getTorrent().getAnnounceURL());
													}
													
													public DownloadAnnounceResultPeer[]
													getPeers()
													{
														return( new DownloadAnnounceResultPeer[0] );
													}
													
													public long
													getTimeToWait()
													{
														return( 0 );
													}
													
													public Map
													getExtensions()
													{
														return( null );
													}
												});
										}
									}
									
									public void
									positionChanged(
										Download		download, 
										int 			oldPosition,
										int 			newPosition )
									{
										
									}
								});
									
							
							download.setScrapeResult(
								new DownloadScrapeResult()
								{
									public Download
									getDownload()
									{
										return( download );
									}
									
									public int
									getResponseType()
									{
										return( RT_ERROR );
									}
									
									public int
									getSeedCount()
									{
										return( -1 );
									}
									
									public int
									getNonSeedCount()
									{
										return( -1 );
									}

									public long
									getScrapeStartTime()
									{
										return( SystemTime.getCurrentTime());
									}
										
									public void 
									setNextScrapeStartTime(
										long nextScrapeStartTime)
									{
									}

									public long
									getNextScrapeStartTime()
									{
										return( -1 );
									}
									
									public String
									getStatus()
									{
										return( "Distributed Database Offline" );
									}

									public URL
									getURL()
									{
										return( download.getTorrent().getAnnounceURL());
									}
								});
						}
					}
					
					public void
					downloadRemoved(
						Download	download )
					{
					}
				});
	}
	
	protected void
	initialise()
	{
		plugin_interface.getDownloadManager().addListener(
				new DownloadManagerListener()
				{
					Random	random = new Random();
					
					public void
					downloadAdded(
						Download	download )
					{
						String[]	networks = download.getListAttribute( ta_networks );
						
						Torrent	torrent = download.getTorrent();
						
						if ( torrent != null && networks != null ){
							
							boolean	public_net = false;
							
							for (int i=0;i<networks.length;i++){
								
								if ( networks[i].equalsIgnoreCase( "Public" )){
										
									public_net	= true;
									
									break;
								}
							}
							
							if ( public_net && !torrent.isPrivate()){
	
								boolean	our_download =  torrent.wasCreatedByUs();

								long	delay;
								
								if ( our_download ){
									
									if ( download.getCreationTime() > start_time ){
										
										delay = 0;
									
									}else{
									
										delay = plugin_interface.getUtilities().getCurrentSystemTime() + 
												INTERESTING_INIT_MIN_OURS + 
												random.nextInt( INTERESTING_INIT_RAND_OURS );

									}
								}else{
									
									delay = plugin_interface.getUtilities().getCurrentSystemTime() + 
												INTERESTING_INIT_MIN_OTHERS + 
												random.nextInt( INTERESTING_INIT_RAND_OTHERS );
								}
								
								try{
									this_mon.enter();
						
									interesting_downloads.put( download, new Long( delay ));
									
								}finally{
									
									this_mon.exit();
								}
							}
						}
						
						download.addAttributeListener(DHTTrackerPlugin.this, ta_networks, DownloadAttributeListener.WRITTEN);
						download.addAttributeListener(DHTTrackerPlugin.this, ta_peer_sources, DownloadAttributeListener.WRITTEN);
						
						download.addTrackerListener( DHTTrackerPlugin.this );
						
						download.addListener( DHTTrackerPlugin.this );
						
						checkDownloadForRegistration( download, true );
					}
					
					public void
					downloadRemoved(
						Download	download )
					{
						
						download.removeTrackerListener( DHTTrackerPlugin.this );

						download.removeListener( DHTTrackerPlugin.this );
						
						try{
							this_mon.enter();
				
							interesting_downloads.remove( download );
							
							running_downloads.remove( download );
							
							limited_online_tracking.remove( download );
							
						}finally{
							
							this_mon.exit();
						}
					}
				});
		
		plugin_interface.getUtilities().createTimer("DHT Tracker", true ).addPeriodicEvent(
			15000,
			new UTTimerEventPerformer()
			{
				private int	ticks;
				
				public void 
				perform(
					UTTimerEvent event) 
				{
					ticks++;
					
					processRegistrations( ticks%8==0 );
					
					if ( ticks == 2 || ticks%4==0 ){
						
						processNonRegistrations();
					}
				}
			});
	}
	
	public void attributeEventOccurred(Download download, TorrentAttribute attr, int event_type) {
		checkDownloadForRegistration(download, false);
	}
	
	public void
	scrapeResult(
		DownloadScrapeResult	result )
	{
		checkDownloadForRegistration( result.getDownload(), false );
	}
	
	public void
	announceResult(
		DownloadAnnounceResult	result )
	{
		checkDownloadForRegistration( result.getDownload(), false );
	}
	
	
	protected void
	checkDownloadForRegistration(
		Download		download,
		boolean			first_time )
	{
		int	state = download.getState();
			
		int	register_type	= REG_TYPE_NONE;
		
		String	register_reason;
		
		Random	random = new Random();
			/*
			 * Queued downloads are removed from the set to consider as we now have the "presence store"
			 * mechanism to ensure that there are a number of peers out there to provide torrent download
			 * if required. This has been done to avoid the large number of registrations that users with
			 * large numbers of queued torrents were getting.
			 */
		
		if ( 	state == Download.ST_DOWNLOADING 	||
				state == Download.ST_SEEDING 		||
				// state == Download.ST_QUEUED 		||	
				download.isPaused()){	// pause is a transitory state, don't dereg
						
			String[]	networks = download.getListAttribute( ta_networks );
			
			Torrent	torrent = download.getTorrent();
			
			if ( torrent != null && networks != null ){
				
				boolean	public_net = false;
				
				for (int i=0;i<networks.length;i++){
					
					if ( networks[i].equalsIgnoreCase( "Public" )){
							
						public_net	= true;
						
						break;
					}
				}
				
				if ( public_net && !torrent.isPrivate()){
					
					if ( torrent.isDecentralised()){
						
							// peer source not relevant for decentralised torrents
						
						register_type	= REG_TYPE_FULL;
						
						register_reason = "decentralised";
							
					}else{
						
						if ( torrent.isDecentralisedBackupEnabled()){
								
							String[]	sources = download.getListAttribute( ta_peer_sources );
	
							boolean	ok = false;
							
							for (int i=0;i<sources.length;i++){
								
								if ( sources[i].equalsIgnoreCase( "DHT")){
									
									ok	= true;
									
									break;
								}
							}
	
							if ( !ok ){
											
								register_reason = "decentralised peer source disabled";
								
							}else{
									// this will always be true since change to exclude queued...
								
								boolean	is_active = 
											state == Download.ST_DOWNLOADING ||
											state == Download.ST_SEEDING ||
											download.isPaused();

								if ( is_active ){
									
									register_type = REG_TYPE_DERIVED;
								}
								
								if( torrent.isDecentralisedBackupRequested()){
									
									register_type	= REG_TYPE_FULL;
									
									register_reason = "torrent requests decentralised tracking";
									
								}else if ( track_normal_when_offline.getValue()){
									
										// only track if torrent's tracker is not available
																
									if ( is_active ){
										
										DownloadAnnounceResult result = download.getLastAnnounceResult();
										
										if (	result == null ||
												result.getResponseType() == DownloadAnnounceResult.RT_ERROR ||
												TorrentUtils.isDecentralised(result.getURL())){
											
											register_type	= REG_TYPE_FULL;
											
											register_reason = "tracker unavailable (announce)";
											
										}else{	
											
											register_reason = "tracker available (announce: " + result.getURL() + ")";								
										}
									}else{
										
										DownloadScrapeResult result = download.getLastScrapeResult();
										
										if (	result == null || 
												result.getResponseType() == DownloadScrapeResult.RT_ERROR ||
												TorrentUtils.isDecentralised(result.getURL())){
											
											register_type	= REG_TYPE_FULL;
											
											register_reason = "tracker unavailable (scrape)";
											
										}else{
											
											register_reason = "tracker available (scrape: " + result.getURL() + ")";								
										}
									}
									
									if ( register_type != REG_TYPE_FULL && track_limited_when_online.getValue()){
										
										Boolean	existing = (Boolean)limited_online_tracking.get( download );
										
										boolean	track_it = false;
										
										if ( existing != null ){
											
											track_it = existing.booleanValue();
											
										}else{
											
											DownloadScrapeResult result = download.getLastScrapeResult();
											
											if (	result != null&& 
													result.getResponseType() == DownloadScrapeResult.RT_SUCCESS ){
												
												int	seeds 		= result.getSeedCount();
												int leechers	= result.getNonSeedCount();
												
												int	swarm_size = seeds + leechers;
																								
												if ( swarm_size <= LIMITED_TRACK_SIZE ){
													
													track_it = true;
													
												}else{
													
													track_it = random.nextInt( swarm_size ) < LIMITED_TRACK_SIZE;
												}
												
												if ( track_it ){
													
													limited_online_tracking.put( download, new Boolean( track_it ));
												}
											}
										}
										
										if( track_it ){
											
											register_type	= REG_TYPE_FULL;
											
											register_reason = "limited online tracking";
										}
									}
								}else{
									register_type	= REG_TYPE_FULL;
									
									register_reason = "peer source enabled";
								}
							}
						}else{
							
							register_reason = "decentralised backup disabled for the torrent";
						}
					}
				}else{
					
					register_reason = "not public";
				}
			}else{
				
				register_reason = "torrent is broken";
			}
			
			if ( register_type == REG_TYPE_DERIVED ){
								
				if ( register_reason.length() == 0 ){
					
					register_reason = "derived";
					
				}else{
					
					register_reason = "derived (overriding ' " + register_reason + "')";
				}
			}
		}else if ( 	state == Download.ST_STOPPED ||
					state == Download.ST_ERROR ){
			
			register_reason	= "not running";
			
		}else if ( 	state == Download.ST_QUEUED ){

				// leave in whatever state it current is (reg or not reg) to avoid thrashing
				// registrations when seeding rules are start/queueing downloads
			
			register_reason	= "";
			
		}else{
			
			register_reason	= "";
		}
		
		if ( register_reason.length() > 0 ){
			
			try{
				this_mon.enter();
	
				Integer	existing_type = (Integer)running_downloads.get( download );
			
				if ( register_type != REG_TYPE_NONE ){
				
					if ( existing_type == null ){
						
						log.log(download.getTorrent(), LoggerChannel.LT_INFORMATION,
								"Monitoring '" + download.getName() + "': " + register_reason);
						
						running_downloads.put( download, new Integer( register_type ));
						
					}else if ( 	existing_type.intValue() == REG_TYPE_DERIVED &&
								register_type == REG_TYPE_FULL ){
						
							// upgrade
						
						running_downloads.put( download, new Integer( register_type ));

					}
				}else{
					
					if ( existing_type  != null ){
						
						log.log(download.getTorrent(), LoggerChannel.LT_INFORMATION,
								"Not monitoring '" + download.getName() + "': "	+ register_reason);
	
						running_downloads.remove( download );
						
							// add back to interesting downloads for monitoring
						
						interesting_downloads.put( 
								download,
								new Long( 	plugin_interface.getUtilities().getCurrentSystemTime() + 
											INTERESTING_INIT_MIN_OTHERS ));

					}else{
						
						if ( first_time ){
							
							log.log(download.getTorrent(), LoggerChannel.LT_INFORMATION,
									"Not monitoring '" + download.getName() + "': "	+ register_reason);
						}
					}
				}
			}finally{
				
				this_mon.exit();
			}
		}
	}
	
	protected void
	processRegistrations(
		boolean		full_processing )
	{
		int	tcp_port = plugin_interface.getPluginconfig().getIntParameter( "TCP.Listen.Port" );

 		String port_override = COConfigurationManager.getStringParameter("TCP.Listen.Port.Override");
 		
  		if( !port_override.equals("")){
 
  			try{
  				tcp_port	= Integer.parseInt( port_override );
  				
  			}catch( Throwable e ){
  			}
  		}
  		
  		if ( tcp_port == 0 ){
  			
  			log.log( "TCP port=0, registration not performed" );

  			return;
  		}
  				
	    String override_ips = COConfigurationManager.getStringParameter( "Override Ip", "" );

	    String override_ip	= null;
	    
	  	if ( override_ips.length() > 0 ){
    		
   				// gotta select an appropriate override based on network type
	  			
	  		StringTokenizer	tok = new StringTokenizer( override_ips, ";" );
				
	  		while( tok.hasMoreTokens()){
			
	  			String	this_address = (String)tok.nextToken().trim();
			
	  			if ( this_address.length() > 0 ){
				
	  				String	cat = AENetworkClassifier.categoriseAddress( this_address );
				
	  				if ( cat == AENetworkClassifier.AT_PUBLIC ){
					
	  					override_ip	= this_address;
	  					
	  					break;
	  				}
	  			}
			}
		}	
    
  	    if ( override_ip != null ){
  	     	   	
    		try{
    			override_ip = PRHelpers.DNSToIPAddress( override_ip );
  	    		
    		}catch( UnknownHostException e){

    			log.log( "    Can't resolve IP override '" + override_ip + "'" );
    			
    			override_ip	= null;
    		}
    	}
  	    
  	    	// format is [ip_override:]tcp_port[;C][;udp_port]
  	    
  	    String	value_to_put = override_ip==null?"":(override_ip+":");
  	    
  	    value_to_put += tcp_port;
  	    	
  	    if ( NetworkManager.REQUIRE_CRYPTO_HANDSHAKE ){
  	    	
  	    	value_to_put += ";C";
  	    }
  	    
		int	udp_port = plugin_interface.getPluginconfig().getIntParameter( "UDP.Listen.Port" );

		int	dht_port = dht.getLocalAddress().getAddress().getPort();
		
		if ( udp_port != dht_port ){
			
			value_to_put += ";" + udp_port;
		}
		
		putDetails	put_details = new putDetails( value_to_put, override_ip, tcp_port, udp_port );
		
		ArrayList	rds;
		
		try{
			this_mon.enter();

			rds = new ArrayList(running_downloads.keySet());
			
		}finally{
			
			this_mon.exit();
		}

		long	 now = SystemTime.getCurrentTime();
		
		
		if ( full_processing ){
			
			Iterator	it = rds.iterator();
			
			List interesting = new ArrayList();
			
			while( it.hasNext()){
				
				Download	dl = (Download)it.next();
				
				int	reg_type = REG_TYPE_NONE;
				
				try{ 
					this_mon.enter();
				
					Integer x = (Integer)running_downloads.get( dl );
					
					if ( x != null ){
						
						reg_type = x.intValue();
					}
				}finally{
					
					this_mon.exit();
				}
					
		  		if ( reg_type == REG_TYPE_NONE ){

		  			continue;
		  		}
		  		
		  		long metric = getDerivedTrackMetric( dl );
		  		
		  		interesting.add( new Object[]{ dl, new Long( metric )} );
			}
			
			Collections.sort(
				interesting,
				new Comparator()
				{
					public int 
					compare(
						Object o1, 
						Object o2) 
					{
						Object[] entry1 = (Object[])o1;
						Object[] entry2 = (Object[])o2;
						
						long	res = ((Long)entry2[1]).longValue() - ((Long)entry1[1]).longValue();
						
						if( res < 0 ){
							
							return( -1 );
							
						}else if ( res > 0 ){
							
							return( 1 );
							
						}else{
							
							return( 0 );
						}
					}
				});
			
			it	= interesting.iterator();
			
			int	num = 0;
			
			while( it.hasNext()){
				
				Object[] entry = (Object[])it.next();
				
				Download	dl 		= (Download)entry[0];
				long		metric	= ((Long)entry[1]).longValue();
				
				num++;
				
				if ( metric > 0 ){
					
					if ( num <= DL_DERIVED_MIN_TRACK ){
						
							// leave as is
						
					}else if ( num <= DL_DERIVED_MAX_TRACK ){
						
							// scale metric between limits
						
						metric = ( metric * ( DL_DERIVED_MAX_TRACK - num )) / ( DL_DERIVED_MAX_TRACK - DL_DERIVED_MIN_TRACK );
						
					}else{
						
						metric = 0;
					}
				}
				
				if ( metric > 0 ){
					
					dl.setUserData( DL_DERIVED_METRIC_KEY, new Long( metric ));
					
				}else{
					
					dl.setUserData( DL_DERIVED_METRIC_KEY, null );
				}
			}
		}
		
		Iterator	it = rds.iterator();
		
			// first off do any puts
		
		while( it.hasNext()){
			
			final Download	dl = (Download)it.next();
			
			int	reg_type = REG_TYPE_NONE;
			
			try{ 
				this_mon.enter();
			
				Integer x = (Integer)running_downloads.get( dl );
				
				if ( x != null ){
					
					reg_type = x.intValue();
				}
			}finally{
				
				this_mon.exit();
			}
				
	  		if ( reg_type == REG_TYPE_NONE ){

	  			continue;
	  		}
	  		
			byte	flags = isComplete( dl )?DHTPlugin.FLAG_SEEDING:DHTPlugin.FLAG_DOWNLOADING;
			
			RegistrationDetails	registration = (RegistrationDetails)registered_downloads.get( dl );
			
			boolean	do_it = false;
			
			if ( registration == null ){
				
				log.log( "Registering download '" + dl.getName() + "' as " + (flags == DHTPlugin.FLAG_SEEDING?"Seeding":"Downloading"));

				registration = new RegistrationDetails( dl, reg_type, put_details, flags );
				
				registered_downloads.put( dl, registration );

				do_it = true;
				
			}else{

				boolean	targets_changed = false;
				
				if ( full_processing ){
					
					targets_changed = registration.updateTargets( dl, reg_type );
				}
				
				if (	targets_changed ||
						registration.getFlags() != flags ||
						!registration.getPutDetails().sameAs( put_details )){
				
					log.log((registration==null?"Registering":"Re-registering") + " download '" + dl.getName() + "' as " + (flags == DHTPlugin.FLAG_SEEDING?"Seeding":"Downloading"));
					
					registration.update( put_details, flags );
					
					do_it = true;
				}
			} 
			
			if ( do_it ){
				
				try{ 
					this_mon.enter();

					query_map.put( dl, new Long( now ));

				}finally{
					
					this_mon.exit();
				}
					  					  						  	    
				trackerPut( dl, registration );
			}
		}
		
			// second any removals
		
		it = registered_downloads.entrySet().iterator();
		
		while( it.hasNext()){
			
			Map.Entry	entry = (Map.Entry)it.next();
			
			final Download	dl = (Download)entry.getKey();

			boolean	unregister;
			
			try{ 
				this_mon.enter();

				unregister = !running_downloads.containsKey( dl );
				
			}finally{
				
				this_mon.exit();
			}
			
			if ( unregister ){
				
				log.log(dl.getTorrent(), LoggerChannel.LT_INFORMATION,
						"Unregistering download '" + dl.getName() + "'");
								
				it.remove();
				
				try{
					this_mon.enter();

					query_map.remove( dl );
					
				}finally{
					
					this_mon.exit();
				}
				
				trackerRemove( dl, (RegistrationDetails)entry.getValue());
			}
		}
		
			// lastly gets
		
		it = rds.iterator();
		
		while( it.hasNext()){
			
			final Download	dl = (Download)it.next();
			
			Long	next_time;
			
			try{
				this_mon.enter();
	
				next_time = (Long)query_map.get( dl );
				
			}finally{
				
				this_mon.exit();
			}
			
			if ( next_time != null && now >= next_time.longValue()){
			
				int	reg_type = REG_TYPE_NONE;
				
				try{
					this_mon.enter();
		
					query_map.remove( dl );
					
					Integer x = (Integer)running_downloads.get( dl );
					
					if ( x != null ){
						
						reg_type = x.intValue();
					}
				}finally{
					
					this_mon.exit();
				}
				
				final long	start = SystemTime.getCurrentTime();
					
					// if we're already connected to > NUM_WANT peers then don't bother with the main announce
				
				PeerManager	pm = dl.getPeerManager();
				
					// don't query if this download already has an active DHT operation
				
				boolean	skip	= isActive( dl ) || reg_type == REG_TYPE_NONE;
				
				if ( skip ){
					
					log.log(dl.getTorrent(), LoggerChannel.LT_INFORMATION,
							"Deferring announce for '" + dl.getName()
									+ "' as activity outstanding");
				}
				
				RegistrationDetails	registration = (RegistrationDetails)registered_downloads.get( dl );

				if ( registration == null ){
					
					Debug.out( "Inconsistent, registration should be non-null" );
					
					continue;
				}
				
				boolean	derived_only = false;
				
				if ( pm != null && !skip ){
					
					int	con = pm.getStats().getConnectedLeechers() + pm.getStats().getConnectedSeeds();
				
					derived_only = con >= NUM_WANT;
				}
				
				if ( !skip ){
					
					skip = trackerGet( dl, registration, derived_only ) == 0;
					
				}
				
					// if we didn't kick off a get then we have to reschedule here as normally
					// the get operation will do the rescheduling when it receives a result
				
				if ( skip ){
					
					try{
						this_mon.enter();
					
						if ( running_downloads.containsKey( dl )){
							
								// use "min" here as we're just deferring it
							
							query_map.put( dl, new Long( start + ANNOUNCE_MIN_DEFAULT ));
						}
						
					}finally{
						
						this_mon.exit();
					}
				}
			}
		}
	}
	
	protected long
	getDerivedTrackMetric(
		Download		download )
	{
			// metric between -100 and + 100. Note that all -ve mean 'don't do it'
			// they're just indicating different reasons 
	
		Torrent t = download.getTorrent();
		
		if ( t == null ){
			
			return( -100 );
		}
		
		if ( t.getSize() < 10*1024*1024 ){
			
			return( -99 );
		}
		
		DownloadAnnounceResult announce = download.getLastAnnounceResult();
		
		if ( 	announce == null ||
				announce.getResponseType() != DownloadAnnounceResult.RT_SUCCESS ){			
			
			return( -98 );
		}
		
		DownloadScrapeResult scrape = download.getLastScrapeResult();
		
		if ( 	scrape == null ||
				scrape.getResponseType() != DownloadScrapeResult.RT_SUCCESS ){
			
			return( -97 );
		}
		
		int leechers 	= scrape.getNonSeedCount();
		int seeds		= scrape.getSeedCount();
		
		int	total = leechers + seeds;
		
		if ( total >= 500 ){
			
			return( 100 );
			
		}else if ( total <= 100 ){
			
			return( 0 );
			
		}else{
		
			return( ( total - 100 ) / 4 );
		}
	}
	
	protected void
	trackerPut(
		final Download			download,
		RegistrationDetails		details )
	{
		final 	long	start = SystemTime.getCurrentTime();
		 	    
		trackerTarget[] targets = details.getTargets( true );
		
		byte flags = details.getFlags();
		
		for (int i=0;i<targets.length;i++){
			
			final trackerTarget target = targets[i];

		 	    // don't let a put block an announce as we don't want to be waiting for 
		  	    // this at start of day to get a torrent running
		  	    
		  	    // increaseActive( dl );
	 
			String	encoded = details.getPutDetails().getEncoded();
			
			byte[]	encoded_bytes = encoded.getBytes();
			
			DHTPluginValue existing = dht.getLocalValue( target.getHash());
			
			if ( 	existing != null &&
					existing.getFlags() == flags &&
					Arrays.equals( existing.getValue(), encoded_bytes )){
				
					// already present, no point in updating
				
				continue;
			}
			
			dht.put( 
					target.getHash(),
					"Tracker registration of '" + download.getName() + "'" + target.getDesc() + " -> " + encoded,
					encoded_bytes,
					flags,
					new DHTPluginOperationListener()
					{
						public void
						diversified()
						{
						}
						
						public void
						valueRead(
							DHTPluginContact	originator,
							DHTPluginValue		value )
						{							
						}
						
						public void
						valueWritten(
							DHTPluginContact	target,
							DHTPluginValue		value )
						{	
						}
	
						public void
						complete(
							byte[]	key,
							boolean	timeout_occurred )
						{
							if ( target.getType() == REG_TYPE_FULL ){
								
								log.log(download.getTorrent(), LoggerChannel.LT_INFORMATION,
										"Registration of '" + download.getName()
												+ "'" + target.getDesc() + " completed (elapsed="
												+ (SystemTime.getCurrentTime() - start) + ")");
							}
							
								// decreaseActive( dl );
						}
					});
		}
	}
	
	protected int
	trackerGet(
		final Download					download,
		final RegistrationDetails		details,
		final boolean					derived_only )
	{
		final 	long	start = SystemTime.getCurrentTime();

		final Torrent	torrent = download.getTorrent();
		
		final URL	url_to_report = torrent.isDecentralised()?torrent.getAnnounceURL():DEFAULT_URL;

		trackerTarget[] targets = details.getTargets( false );
		
		final long[]	max_retry = { 0 };
		
		int	num_done = 0;
		
		for (int i=0;i<targets.length;i++){
			
			final trackerTarget target = targets[i];
	
			if ( target.getType() == REG_TYPE_FULL && derived_only ){
				
				continue;
			}
			
			increaseActive( download );
			
			num_done++;
			
			dht.get(target.getHash(), 
					"Tracker announce for '" + download.getName() + "'" + target.getDesc(),
					isComplete( download )?DHTPlugin.FLAG_SEEDING:DHTPlugin.FLAG_DOWNLOADING,
					NUM_WANT, 
					ANNOUNCE_TIMEOUT,
					false, false,
					new DHTPluginOperationListener()
					{
						List	addresses 	= new ArrayList();
						List	ports		= new ArrayList();
						List	udp_ports	= new ArrayList();
						List	is_seeds	= new ArrayList();
						List	flags		= new ArrayList();
						
						int		seed_count;
						int		leecher_count;
						
						public void
						diversified()
						{
						}
						
						public void
						valueRead(
							DHTPluginContact	originator,
							DHTPluginValue		value )
						{
							try{										
								String[]	tokens = new String(value.getValue()).split(";");
							
								String	tcp_part = tokens[0].trim();
								
								int	sep = tcp_part.indexOf(':');
								
								String	ip_str		= null;
								String	tcp_port_str;
								
								if ( sep == -1 ){
									
									tcp_port_str = tcp_part;
									
								}else{
									
									ip_str 			= tcp_part.substring( 0, sep );							
									tcp_port_str	= tcp_part.substring( sep+1 );	
								}
								
								int	tcp_port = Integer.parseInt( tcp_port_str );
									
								if ( tcp_port > 0 && tcp_port < 65536 ){
	
									String	flag_str	= null;
									int		udp_port	= -1;
									
									try{
										for (int i=1;i<tokens.length;i++){
										
											String	token = tokens[i].trim();
											
											if ( token.length() > 0 ){
												
												if ( Character.isDigit( token.charAt( 0 ))){
													
													udp_port = Integer.parseInt( token );
													
													if ( udp_port <= 0 || udp_port >=65536 ){
														
														udp_port = -1;
													}
												}else{
													
													flag_str = token;
												}
											}
										}
									}catch( Throwable e ){
									}
								
									addresses.add( 
											ip_str==null?originator.getAddress().getAddress().getHostAddress():ip_str);
									
									ports.add( new Integer( tcp_port ));
									
									udp_ports.add( new Integer( udp_port==-1?originator.getAddress().getPort():udp_port));
									
									flags.add( flag_str );
									
									if (( value.getFlags() & DHTPlugin.FLAG_DOWNLOADING ) == 1 ){
										
										leecher_count++;
										
										is_seeds.add( new Boolean( false ));
	
									}else{
										
										is_seeds.add( new Boolean( true ));
										
										seed_count++;
									}
								}
								
							}catch( Throwable e ){
								
								// in case we get crap back (someone spamming the DHT) just
								// silently ignore
							}
						}
						
						public void
						valueWritten(
							DHTPluginContact	target,
							DHTPluginValue		value )
						{
						}
	
						public void
						complete(
							byte[]	key,
							boolean	timeout_occurred )
						{
							if ( 	target.getType() == REG_TYPE_FULL ||
									(	target.getType() == REG_TYPE_DERIVED && 
										seed_count + leecher_count > 1 )){
								
								log.log(download.getTorrent(), LoggerChannel.LT_INFORMATION,
										"Get of '" + download.getName() + "'" + target.getDesc() + " completed (elapsed="
												+ (SystemTime.getCurrentTime() - start)
												+ "), addresses=" + addresses.size() + ", seeds="
												+ seed_count + ", leechers=" + leecher_count);
							}
						
							decreaseActive(download);
							
							int	peers_found = addresses.size();
							
							List	peers_for_announce = new ArrayList();
							
								// scale min and max based on number of active torrents
								// we don't want more than a few announces a minute
							
							int	announce_per_min = 4;
							
							int	num_active = query_map.size();
							
							int	announce_min = Math.max( ANNOUNCE_MIN_DEFAULT, ( num_active / announce_per_min )*60*1000 );
							
							int	announce_max = derived_only?ANNOUNCE_MAX_DERIVED_ONLY:ANNOUNCE_MAX;
							
							announce_min = Math.min( announce_min, announce_max );
							
							final long	retry = announce_min + peers_found*(announce_max-announce_min)/NUM_WANT;
																
							try{
								this_mon.enter();
							
								if ( running_downloads.containsKey( download )){

									long	absolute_retry = SystemTime.getCurrentTime() + retry;
								
									if ( absolute_retry > max_retry[0] ){
											
											// only update next query time if none set yet 
											// or we appear to have set the existing one. If we
											// don't do this then we'll overwrite any rescheduled
											// announces
										
										Long	existing = (Long)query_map.get( download );
										
										if ( 	existing == null ||
												existing.longValue() == max_retry[0] ){
											
											max_retry[0] = absolute_retry;
									
											query_map.put( download, new Long( absolute_retry ));
										}
									}
								}						
							}finally{
								
								this_mon.exit();
							}
							
							int download_state = download.getState();
							
							boolean	we_are_seeding = download_state == Download.ST_SEEDING;
							
							putDetails put_details = details.getPutDetails();
							
							String	ext_address = put_details.getIPOverride();
							
							if ( ext_address == null ){
								
								ext_address = dht.getLocalAddress().getAddress().getAddress().getHostAddress();
							}
							
							for (int i=0;i<addresses.size();i++){
								
									// when we are seeding ignore seeds
								
								if ( we_are_seeding && ((Boolean)is_seeds.get(i)).booleanValue()){
									
									continue;
								}
								
									// remove ourselves
								
								String	ip = (String)addresses.get(i);
								
								if ( ip.equals( ext_address )){
									
									if ( ((Integer)ports.get(i)).intValue() == put_details.getTCPPort() &&
										 ((Integer)udp_ports.get(i)).intValue() == put_details.getUDPPort()){
									
										continue;
									}
								}
								
								final int f_i = i;
								
								peers_for_announce.add(
									new DownloadAnnounceResultPeer()
									{
										public String
										getSource()
										{
											return( PEPeerSource.PS_DHT );
										}
										
										public String
										getAddress()
										{
											return((String)addresses.get(f_i));
										}
										
										public int
										getPort()
										{
											return(((Integer)ports.get(f_i)).intValue());
										}
										
										public int
										getUDPPort()
										{
											return(((Integer)udp_ports.get(f_i)).intValue());
										}
										
										public byte[]
										getPeerID()
										{
											return( null );
										}
										
										public short
										getProtocol()
										{
											String	flag = (String)flags.get(f_i);
											
											short protocol;
											
											if ( flag != null && flag.indexOf("C") != -1 ){
												
												protocol = PROTOCOL_CRYPT;
												
											}else{
												
												protocol = PROTOCOL_NORMAL;
											}
											
											return( protocol );
										}
									});
								
							}
								
							if ( target.getType() == REG_TYPE_DERIVED && peers_for_announce.size() > 0 ){
								
								PeerManager pm = download.getPeerManager();
								
								if ( pm != null ){
										
										// try some limited direct injection
									
									List	temp = new ArrayList( peers_for_announce );
									
									Random rand = new Random();
									
									for (int i=0;i<DIRECT_INJECT_PEER_MAX && temp.size() > 0; i++ ){
										
										DownloadAnnounceResultPeer peer = (DownloadAnnounceResultPeer)temp.remove( rand.nextInt( temp.size()));
										
										log.log( "    Injecting " + peer.getAddress());
										
										pm.addPeer( 
												peer.getAddress(),
												peer.getPort(),
												peer.getUDPPort(),
												peer.getProtocol() == DownloadAnnounceResultPeer.PROTOCOL_CRYPT );
												
									}
								}
							}
							
							if ( 	download_state == Download.ST_DOWNLOADING ||
									download_state == Download.ST_SEEDING ){
							
								final DownloadAnnounceResultPeer[]	peers = new DownloadAnnounceResultPeer[peers_for_announce.size()];
								
								peers_for_announce.toArray( peers );
								
								download.setAnnounceResult(
										new DownloadAnnounceResult()
										{
											public Download
											getDownload()
											{
												return( download );
											}
																						
											public int
											getResponseType()
											{
												return( DownloadAnnounceResult.RT_SUCCESS );
											}
																					
											public int
											getReportedPeerCount()
											{
												return( peers.length);
											}
													
											public int
											getSeedCount()
											{
												return( seed_count );
											}
											
											public int
											getNonSeedCount()
											{
												return( leecher_count );	
											}
											
											public String
											getError()
											{
												return( null );
											}
																						
											public URL
											getURL()
											{
												return( url_to_report );
											}
											
											public DownloadAnnounceResultPeer[]
											getPeers()
											{
												return( peers );
											}
											
											public long
											getTimeToWait()
											{
												return( retry/1000 );
											}
											
											public Map
											getExtensions()
											{
												return( null );
											}
										});
							}
								
								// only inject the scrape result if the torrent is decentralised. If we do this for
								// "normal" torrents then it can have unwanted side-effects, such as stopping the torrent
								// due to ignore rules if there are no downloaders in the DHT - bthub backup, for example,
								// isn't scrapable...
							
								// hmm, ok, try being a bit more relaxed about this, inject the scrape if
								// we have any peers. 
																
							boolean	inject_scrape = leecher_count > 0;
							
							DownloadScrapeResult result = download.getLastScrapeResult();
																
							if (	result == null || 
									result.getResponseType() == DownloadScrapeResult.RT_ERROR ){									
	
							}else{
							
									// if the currently reported values are the same as the 
									// ones we previously injected then overwrite them
									// note that we can't test the URL to see if we originated
									// the scrape values as this gets replaced when a normal
									// scrape fails :(
									
								int[]	prev = (int[])scrape_injection_map.get( download );
									
								if ( 	prev != null && 
										prev[0] == result.getSeedCount() &&
										prev[1] == result.getNonSeedCount()){
																						
									inject_scrape	= true;
								}
							}
							
							if ( torrent.isDecentralised() || inject_scrape ){
								
								
									// make sure that the injected scrape values are consistent
									// with our currently connected peers
								
								PeerManager	pm = download.getPeerManager();
								
								int	local_seeds 	= 0;
								int	local_leechers 	= 0;
								
								if ( pm != null ){
									
									Peer[]	dl_peers = pm.getPeers();
									
									for (int i=0;i<dl_peers.length;i++){
										
										Peer	dl_peer = dl_peers[i];
										
										if ( dl_peer.getPercentDoneInThousandNotation() == 1000 ){
											
											local_seeds++;
											
										}else{
											local_leechers++;
										}
									}							
								}
								
								final int f_adj_seeds 		= Math.max( seed_count, local_seeds );
								final int f_adj_leechers	= Math.max( leecher_count, local_leechers );
								
								scrape_injection_map.put( download, new int[]{ f_adj_seeds, f_adj_leechers });
	
								download.setScrapeResult(
									new DownloadScrapeResult()
									{
										public Download
										getDownload()
										{
											return( download );
										}
										
										public int
										getResponseType()
										{
											return( RT_SUCCESS );
										}
										
										public int
										getSeedCount()
										{
											return( f_adj_seeds );
										}
										
										public int
										getNonSeedCount()
										{
											return( f_adj_leechers );
										}
	
										public long
										getScrapeStartTime()
										{
											return( start );
										}
											
										public void 
										setNextScrapeStartTime(
											long nextScrapeStartTime)
										{
											
										}
										public long
										getNextScrapeStartTime()
										{
											return( SystemTime.getCurrentTime() + retry );
										}
										
										public String
										getStatus()
										{
											return( "OK" );
										}
	
										public URL
										getURL()
										{
											return( url_to_report );
										}
									});
								}	
						}
					});
		}
		
		return( num_done );
	}
	
	protected boolean
	isComplete(
		Download	download )
	{
		boolean	is_complete = download.isComplete();
		
		if ( is_complete ){
			
			DownloadManager core_dm = PluginCoreUtils.unwrap( download );
			
			if ( core_dm != null ){
			
				PEPeerManager pm = core_dm.getPeerManager();
				
				if ( pm != null && pm.getHiddenBytes() > 0 ){
					
					is_complete = false;
				}
			}
		}
		
		return( is_complete );
	}
	
	protected void
	trackerRemove(
		final Download			download,
		RegistrationDetails		details )
	{
		final 	long	start = SystemTime.getCurrentTime();
		
		trackerTarget[] targets = details.getTargets( true );
		
		for (int i=0;i<targets.length;i++){
			
			final trackerTarget target = targets[i];
			
			if ( dht.hasLocalKey( target.getHash())){
				
				increaseActive( download );
				
				dht.remove( 
						target.getHash(),
						"Tracker deregistration of '" + download.getName() + "' " + target.getDesc(),
						new DHTPluginOperationListener()
						{
							public void
							diversified()
							{
							}
							
							public void
							valueRead(
								DHTPluginContact	originator,
								DHTPluginValue		value )
							{								
							}
							
							public void
							valueWritten(
								DHTPluginContact	target,
								DHTPluginValue		value )
							{
							}	
		
							public void
							complete(
								byte[]	key,
								boolean	timeout_occurred )
							{
								if ( target.getType() == REG_TYPE_FULL ){
	
									log.log(download.getTorrent(), LoggerChannel.LT_INFORMATION,
									"Unregistration of '" + download.getName() + "' " + target.getDesc() + " completed (elapsed="
											+ (SystemTime.getCurrentTime() - start) + ")");
								}
								
								decreaseActive( download );
							}
						});
			}
		}
	}

	protected void
	trackerRemove(
		final Download			download,
		final trackerTarget 	target )
	{
		final 	long	start = SystemTime.getCurrentTime();
		
		if ( dht.hasLocalKey( target.getHash())){
			
			increaseActive( download );
			
			dht.remove( 
					target.getHash(),
					"Tracker deregistration of '" + download.getName() + "' " + target.getDesc(),
					new DHTPluginOperationListener()
					{
						public void
						diversified()
						{
						}
						
						public void
						valueRead(
							DHTPluginContact	originator,
							DHTPluginValue		value )
						{								
						}
						
						public void
						valueWritten(
							DHTPluginContact	target,
							DHTPluginValue		value )
						{
						}	
	
						public void
						complete(
							byte[]	key,
							boolean	timeout_occurred )
						{
							if ( target.getType() == REG_TYPE_FULL ){

								log.log(download.getTorrent(), LoggerChannel.LT_INFORMATION,
								"Unregistration of '" + download.getName() + "' " + target.getDesc() + " completed (elapsed="
										+ (SystemTime.getCurrentTime() - start) + ")");
							}
							
							decreaseActive( download );
						}
					});
		}
	}
	
	protected void
	processNonRegistrations()
	{
		Download	ready_download = null;
	
		long	now = plugin_interface.getUtilities().getCurrentSystemTime();
		
			// unfortunately getting scrape results can acquire locks and there is a vague
			// possibility of deadlock here, so pre-fetch the scrape results
		
		List	to_scrape = new ArrayList();
		
		try{
			this_mon.enter();

			Iterator	it = interesting_downloads.keySet().iterator();
			
			while( it.hasNext() && ready_download == null ){
				
				Download	download = (Download)it.next();
				
				Torrent	torrent = download.getTorrent();
				
				if ( torrent == null ){
					
					continue;
				}
				
				Integer state = (Integer)running_downloads.get( download );

				if ( state == null || state.intValue() == REG_TYPE_DERIVED ){
					
						// looks like we'll need the scrape below
					
					to_scrape.add( download );
				}
			}
		}finally{
			
			this_mon.exit();
		}
		
		Map scrapes = new HashMap();
		
		for (int i=0;i<to_scrape.size();i++){
			
			Download	download = (Download)to_scrape.get(i);
						
			scrapes.put( download, download.getLastScrapeResult());		
		}
		
		try{
			this_mon.enter();

			Iterator	it = interesting_downloads.keySet().iterator();
			
			while( it.hasNext() && ready_download == null ){
				
				Download	download = (Download)it.next();
				
				Torrent	torrent = download.getTorrent();
				
				if ( torrent == null ){
					
					continue;
				}
				
				Integer state = (Integer)running_downloads.get( download );
				
				if ( state == null || state.intValue() == REG_TYPE_DERIVED ){
					
					boolean	force =  torrent.wasCreatedByUs();
					
					if ( !force ){
						
						if ( !dht.isReachable()){
							
							continue;
						}
						
						if ( interesting_pub_max > 0 && interesting_published > interesting_pub_max ){
							
							continue;
						}
						
						DownloadScrapeResult	scrape = (DownloadScrapeResult)scrapes.get( download );
						
						if ( scrape == null ){
							
								// catch it next time round
							
							continue;
						}
						
						if ( scrape.getSeedCount() + scrape.getNonSeedCount() > NUM_WANT ){
							
							continue;
						}
					}
					
					long	target = ((Long)interesting_downloads.get( download )).longValue();
					
					if ( target <= now ){
						
						ready_download	= download;
						
						interesting_downloads.put( download, new Long( now + INTERESTING_CHECK_PERIOD ));
						
					}else if ( target - now > INTERESTING_CHECK_PERIOD ){
						
						interesting_downloads.put( download, new Long( now + (target%INTERESTING_CHECK_PERIOD)));
					}
				}
			}
			
		}finally{
			
			this_mon.exit();
		}
		
		if ( ready_download != null ){
			
			final Download	f_ready_download = ready_download;
			
			if ( dht.isDiversified( ready_download.getTorrent().getHash())){
				
				// System.out.println( "presence query for " + f_ready_download.getName() + "-> diversified pre start" );

				try{
					this_mon.enter();

					interesting_downloads.remove( f_ready_download );
					
				}finally{
					
					this_mon.exit();
				}
			}else{
			
				//System.out.println( "presence query for " + ready_download.getName());
				
				final long start = now;
				
				dht.get(	ready_download.getTorrent().getHash(), 
							"Presence query for '" + ready_download.getName() + "'",
							(byte)0,
							INTERESTING_AVAIL_MAX, 
							ANNOUNCE_TIMEOUT,
							false, false,
							new DHTPluginOperationListener()
							{
								private boolean diversified;
								private int total = 0;
								
								public void
								diversified()
								{
									diversified	= true;
								}
								
								public void
								valueRead(
									DHTPluginContact	originator,
									DHTPluginValue		value )
								{
									total++;
								}
								
								public void
								valueWritten(
									DHTPluginContact	target,
									DHTPluginValue		value )
								{
								}
								
								public void
								complete(
									byte[]	key,
									boolean	timeout_occurred )
								{
									// System.out.println( "    presence query for " + f_ready_download.getName() + "->" + total + "/div = " + diversified );
	
									log.log( f_ready_download.getTorrent(), LoggerChannel.LT_INFORMATION,
											"Presence query for '" + f_ready_download.getName() + "': availability="+
											(total==INTERESTING_AVAIL_MAX?(INTERESTING_AVAIL_MAX+"+"):(total+"")) + ",div=" + diversified +
											" (elapsed=" + (SystemTime.getCurrentTime() - start) + ")");
											
									if ( diversified ){
										
										try{
											this_mon.enter();
	
											interesting_downloads.remove( f_ready_download );
											
										}finally{
											
											this_mon.exit();
										}
										
									}else if ( total < INTERESTING_AVAIL_MAX ){
										
											// once we're registered we don't need to process this download any
											// more unless it goes active and then inactive again
										
										try{
											this_mon.enter();
	
											interesting_downloads.remove( f_ready_download );
											
										}finally{
											
											this_mon.exit();
										}
										
										interesting_published++;
										
										dht.put( 
												f_ready_download.getTorrent().getHash(),
												"Presence store '" + f_ready_download.getName() + "'",
												"0".getBytes(),	// port 0, no connections
												(byte)0,
												new DHTPluginOperationListener()
												{
													public void
													diversified()
													{
													}
													
													public void
													valueRead(
														DHTPluginContact	originator,
														DHTPluginValue		value )
													{
													}
													
													public void
													valueWritten(
														DHTPluginContact	target,
														DHTPluginValue		value )
													{
													}
													
													public void
													complete(
														byte[]	key,
														boolean	timeout_occurred )
													{
													}
												});
	
									}
								}
							});
	
			}
		}
	}
	
	public void
	stateChanged(
		Download		download,
		int				old_state,
		int				new_state )
	{
		int	state = download.getState();
		
		try{
			this_mon.enter();

			if ( 	state == Download.ST_DOWNLOADING ||
					state == Download.ST_SEEDING ||
					state == Download.ST_QUEUED ){	// included queued here for the mo to avoid lots
													// of thrash for torrents that flip a lot
				
				if ( running_downloads.containsKey( download )){
					
						// force requery
					
					query_map.put( download, new Long( SystemTime.getCurrentTime()));
				}
			}
		}finally{
			
			this_mon.exit();
		}
		
		checkDownloadForRegistration( download, false );
	}
 
	public void
	announceAll()
	{
		log.log( "Announce-all requested" );
		
		Long now = new Long( SystemTime.getCurrentTime());
		
		try{
			this_mon.enter();

			Iterator it = query_map.entrySet().iterator();
			
			while( it.hasNext()){
				
				Map.Entry	entry = (Map.Entry)it.next();
				
				entry.setValue( now );
			}
		}finally{
			
			this_mon.exit();
		}		
	}
	
	public void
	positionChanged(
		Download		download, 
		int 			oldPosition,
		int 			newPosition )
	{
	}
	
	protected void
	configChanged()
	{
		Download[] downloads = plugin_interface.getDownloadManager().getDownloads();
	
		for (int i=0;i<downloads.length;i++){
			
			checkDownloadForRegistration(downloads[i], false );
		}
	}
	
	public DownloadScrapeResult
	scrape(
		byte[]		hash )
	{
		final int[]	seeds 		= {0};
		final int[] leechers 	= {0};
		
		final AESemaphore	sem = new AESemaphore( "DHTTrackerPlugin:scrape" );
		
		dht.get(hash, 
				"Scrape for '" + ByteFormatter.nicePrint( hash ) + "'",
				DHTPlugin.FLAG_DOWNLOADING,
				NUM_WANT, 
				SCRAPE_TIMEOUT,
				false, false,
				new DHTPluginOperationListener()
				{
					public void
					diversified()
					{
					}
					
					public void
					valueRead(
						DHTPluginContact	originator,
						DHTPluginValue		value )
					{						
						if (( value.getFlags() & DHTPlugin.FLAG_DOWNLOADING ) == 1 ){
							
							leechers[0]++;
							
						}else{
							
							seeds[0]++;
						}
					}
					
					public void
					valueWritten(
						DHTPluginContact	target,
						DHTPluginValue		value )
					{
					}

					public void
					complete(
						byte[]	key,
						boolean	timeout_occurred )
					{
						sem.release();
					}
				});

		sem.reserve();
		
		return(
				new DownloadScrapeResult()
				{
					public Download
					getDownload()
					{
						return( null );
					}
					
					public int
					getResponseType()
					{
						return( RT_SUCCESS );
					}
					
					public int
					getSeedCount()
					{
						return( seeds[0] );
					}
					
					public int
					getNonSeedCount()
					{
						return( leechers[0] );
					}

					public long
					getScrapeStartTime()
					{
						return( 0 );
					}
						
					public void 
					setNextScrapeStartTime(
						long nextScrapeStartTime)
					{
					}
					
					public long
					getNextScrapeStartTime()
					{
						return( 0 );
					}
					
					public String
					getStatus()
					{
						return( "OK" );
					}

					public URL
					getURL()
					{
						return( null );
					}
				});
	}
	
	protected void
	increaseActive(
		Download		dl )
	{
		try{
			this_mon.enter();
		
			Integer	active_i = (Integer)in_progress.get( dl );
			
			int	active = active_i==null?0:active_i.intValue();
			
			in_progress.put( dl, new Integer( active+1 ));
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected void
	decreaseActive(
		Download		dl )
	{
		try{
			this_mon.enter();
		
			Integer	active_i = (Integer)in_progress.get( dl );
			
			if ( active_i == null ){
				
				Debug.out( "active count inconsistent" );
				
			}else{
				
				int	active = active_i.intValue()-1;
			
				if ( active == 0 ){
					
					in_progress.remove( dl );
					
				}else{
					
					in_progress.put( dl, new Integer( active ));
				}
			}
		}finally{
			
			this_mon.exit();
		}
	}
		
	protected boolean
	isActive(
		Download		dl )
	{
		try{
			this_mon.enter();
			
			return( in_progress.get(dl) != null );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected class
	RegistrationDetails
	{
		private static final int DERIVED_ACTIVE_MIN_MILLIS	= 2*60*60*1000;
		
		private putDetails		put_details;
		private byte			flags;
		private trackerTarget[]	put_targets;
		private List			not_put_targets;
		
		private long			derived_active_start	= -1;
		private long			previous_metric;
		
		protected
		RegistrationDetails(
			Download			_download,
			int					_reg_type,
			putDetails			_put_details,
			byte				_flags )
		{
			put_details		= _put_details;
			flags			= _flags;
			
			getTrackerTargets( _download, _reg_type );
		}
		
		protected void
		update(
			putDetails		_put_details,
			byte			_flags )
		{
			put_details	= _put_details;
			flags		= _flags;
		}
		
		protected boolean
		updateTargets(
			Download			_download,
			int					_reg_type )
		{
			trackerTarget[]	old_put_targets = put_targets;
			 
			getTrackerTargets( _download, _reg_type );
			
				// first remove any redundant entries
			
			for (int i=0;i<old_put_targets.length;i++){
				
				boolean	found = false;
				
				byte[]	old_hash = old_put_targets[i].getHash();
				
				for (int j=0;j<put_targets.length;j++){
					
					if ( Arrays.equals( put_targets[j].getHash(), old_hash )){
						
						found	= true;
						
						break;
					}
				}
				
				if ( !found ){
					
					trackerRemove( _download, old_put_targets[i] );
				}
			}
			
				// now look to see if we have any new stuff 
			
			boolean	changed = false;
			
			for (int i=0;i<put_targets.length;i++){
								
				byte[]	new_hash = put_targets[i].getHash();
				
				boolean	found = false;
				
				for (int j=0;j<old_put_targets.length;j++){
					
					if ( Arrays.equals( old_put_targets[j].getHash(), new_hash )){
						
						found = true;
						
						break;
					}
				}
				
				if ( !found ){
					
					changed = true;
				}
			}
			
			return( changed );
		}
		
		protected putDetails
		getPutDetails()
		{
			return( put_details );
		}
		
		protected byte
		getFlags()
		{
			return( flags );
		}
		
		protected trackerTarget[]
		getTargets(
			boolean		for_put )
		{
			if ( for_put || not_put_targets == null ){
				
				return( put_targets );
				
			}else{
			
				List	result = new ArrayList( Arrays.asList( put_targets ));
				
				for (int i=0;i<not_put_targets.size()&& i < 2; i++ ){
					
					trackerTarget target = (trackerTarget)not_put_targets.remove(0);
					
					not_put_targets.add( target );
					
					// System.out.println( "Mixing in " + target.getDesc());
					
					result.add( target );
				}
				
				return( (trackerTarget[])result.toArray( new trackerTarget[result.size()]));
			}
		}
		
		protected boolean
    	getTrackerTargets(
    		Download		download,
    		int				type )
    	{
    		byte[]	torrent_hash = download.getTorrent().getHash();
    		
    		List	result = new ArrayList();
    		
    		if ( type == REG_TYPE_FULL ){
    			
    			result.add( new trackerTarget( torrent_hash, REG_TYPE_FULL, "" ));
    		}
    		
    	    NetworkAdminASN net_asn = NetworkAdmin.getSingleton().getCurrentASN();
    	      
    	    String	as 	= net_asn.getAS();
    	    String	asn = net_asn.getASName();

    		if ( as.length() > 0 && asn.length() > 0 ){
    			 
    			String	key = "azderived:asn:" + as;
    			
    			try{
    				byte[] asn_bytes = key.getBytes( "UTF-8" );
    			
    				byte[] key_bytes = new byte[torrent_hash.length + asn_bytes.length];
    				
    				System.arraycopy( torrent_hash, 0, key_bytes, 0, torrent_hash.length );
    				
    				System.arraycopy( asn_bytes, 0, key_bytes, torrent_hash.length, asn_bytes.length );
    				
    				result.add( new trackerTarget( key_bytes, REG_TYPE_DERIVED, asn + "/" + as ));
    				
    			}catch( Throwable e ){
    				
    				Debug.printStackTrace(e);
    			}
    		}
    		
    		long	now = SystemTime.getMonotonousTime();
    		
    		boolean	do_it;
    		
       		Long	metric = (Long)download.getUserData( DL_DERIVED_METRIC_KEY );
       	 
       		boolean	do_it_now = metric != null;
       		
    		if ( derived_active_start >= 0 && now - derived_active_start <= DERIVED_ACTIVE_MIN_MILLIS ){
    			
    			do_it = true;
    			
    			if ( metric == null ){
    				
    				metric = new Long( previous_metric );
    			}
    		}else{
    			
    			if ( do_it_now ){
    				
    				do_it = true;
    				
    			}else{
    				
    				derived_active_start = -1;
    				
    				do_it = false;
    			}
    		}
    		
    		boolean	newly_active = false;
    		
    		if ( do_it_now ){
    			
    			newly_active = derived_active_start == -1;
    			
    			derived_active_start = now;
    		}
     		  
    		List	skipped_targets = null;
    		
    		if ( do_it ){
    			
    			previous_metric = metric.longValue();
    			
	    		try{
	    			DHTNetworkPosition[] positions = DHTNetworkPositionManager.getLocalPositions();
	    		
	    			for (int i=0;i<positions.length;i++){
	    				
	    				DHTNetworkPosition pos = positions[i];
	    				
	    				if ( pos.getPositionType() == DHTNetworkPosition.POSITION_TYPE_VIVALDI_V2 ){
	    					
	    					if ( pos.isValid()){
	    						
	    						List	derived_results = getVivaldiTargets( torrent_hash, pos.getLocation());
	    						
	    		    			int	num_to_add = metric.intValue() * derived_results.size() / 100;
    				 			
	    		    			// System.out.println( download.getName() + ": metric=" + metric + ", adding=" + num_to_add );
	    		    			
	    						for (int j=0;j<derived_results.size();j++){
	    							
	    							Object[] entry = (Object[])derived_results.get(j);
	    							
	    							// int	distance = ((Integer)entry[0]).intValue();
	    							
	    							trackerTarget	target= (trackerTarget)entry[1];
	    								    						
	    							if ( j < num_to_add ){
	    							
	    								result.add( target );
	    								
	    							}else{
	    								
	    								if ( skipped_targets == null ){
	    									
	    									skipped_targets = new ArrayList();
	    								}
	    								
	    								skipped_targets.add( target );
	    							}
	    						}
	    					}
	    				}
	    			}
	    		}catch( Throwable e ){
	    			
	    			Debug.printStackTrace(e);
	    		}
    		}
    		
    		put_targets 	= (trackerTarget[])result.toArray( new trackerTarget[result.size()]);
    		not_put_targets = skipped_targets;
    		
    		return( newly_active );
    	}
	}
	
	public static List
	getVivaldiTargets(
		byte[]					torrent_hash,
		double[]				loc )
	{
		List	derived_results = new ArrayList();
				
		String	loc_str = "";
		
		for (int j=0;j<loc.length;j++){
			
			loc_str += (j==0?"":",") + loc[j];
		}
		
		TriangleSlicer slicer = new TriangleSlicer( 25 );
		
		double	t1_x = loc[0];
		double	t1_y = loc[1];
		double	t2_x = loc[2];
		double	t2_y = loc[3];
		
		int[] triangle1 = slicer.findVertices( t1_x, t1_y );
		
		int[] triangle2 = slicer.findVertices( t2_x, t2_y );
		
		/*
		
		System.out.println( "NetPos: " + loc_str );						

		String	tr1_str = "";
		
		for (int j=0;j<triangle1.length;j+=2 ){
			
			tr1_str += (j==0?"":",") + "(" + triangle1[j] + "," + triangle1[j+1] + ")";
		}
		
		String	tr2_str = "";
		
		for (int j=0;j<triangle2.length;j+=2 ){
			
			tr2_str += (j==0?"":",") + "(" + triangle2[j] + "," + triangle2[j+1] + ")";
		}
		
		System.out.println( "t1=" + tr1_str + ",t2=" + tr2_str );
		*/
		
		for (int j=0;j<triangle1.length;j+=2 ){

			int	t1_vx = triangle1[j];
			int t1_vy = triangle1[j+1];
			
			double	t1_distance = getDistance( t1_x, t1_y, t1_vx, t1_vy );
			
			for (int k=0;k<triangle2.length;k+=2 ){
		
				int	t2_vx = triangle2[k];
				int t2_vy = triangle2[k+1];

				double	t2_distance = getDistance( t2_x, t2_y, t2_vx, t2_vy );
				
					// these distances are in different dimensions - make up a combined distance
					
				double distance = getDistance( t1_distance, 0, 0, t2_distance );
				
				
				String	key = "azderived:vivaldi:";
				
				String v_str = 	t1_vx + "." + t1_vy + "." + t2_vx + "." + t2_vy;
				
				key += v_str;
												
				try{
					byte[] v_bytes = key.getBytes( "UTF-8" );
				
					byte[] key_bytes = new byte[torrent_hash.length + v_bytes.length];
					
					System.arraycopy( torrent_hash, 0, key_bytes, 0, torrent_hash.length );
					
					System.arraycopy( v_bytes, 0, key_bytes, torrent_hash.length, v_bytes.length );
					
					derived_results.add( 
						new Object[]{ 
							new Integer((int)distance), 
							new trackerTarget( key_bytes, REG_TYPE_DERIVED, "Vivaldi: " + v_str ) });
						
					
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
			}
		}
		
		Collections.sort(
			derived_results,
			new Comparator()
			{
				public int 
				compare(
					Object 	o1, 
					Object 	o2 ) 
				{
					Object[]	entry1 = (Object[])o1;
					Object[]	entry2 = (Object[])o2;
					
					int	d1 = ((Integer)entry1[0]).intValue();
					int	d2 = ((Integer)entry2[0]).intValue();
					
					return( d1 - d2 );
				}
			});
		
		return( derived_results );
	}
	
	protected static double
	getDistance(
		double	x1,
		double	y1,
		double	x2,
		double	y2 )
	{
		return(Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1)));
	}
	
	protected static class
	putDetails
	{
		private String	encoded;
		private String	ip_override;
		private int		tcp_port;
		private int		udp_port;
		
		private
		putDetails(
			String	_encoded,
			String	_ip,
			int		_tcp_port,
			int		_udp_port )
		{
			encoded			= _encoded;
			ip_override		= _ip;
			tcp_port		= _tcp_port;
			udp_port		= _udp_port;
		}
		
		protected String
		getEncoded()
		{
			return( encoded );
		}
		
		protected String
		getIPOverride()
		{
			return( ip_override );
		}
		
		protected int
		getTCPPort()
		{
			return( tcp_port );
		}
		
		protected int
		getUDPPort()
		{
			return( udp_port );
		}
		
		protected boolean
		sameAs(
			putDetails		other )
		{
			return( getEncoded().equals( other.getEncoded()));
		}
	}
	
	public static class
	trackerTarget
	{
		private String		desc;
		private	byte[]		hash;
		private int			type;
		
		protected
		trackerTarget(
			byte[]			_hash,
			int				_type,
			String			_desc )
		{
			hash		= _hash;
			type		= _type;
			desc		= _desc;
		}
		
		public int
		getType()
		{
			return( type );
		}
		
		public byte[]
		getHash()
		{
			return( hash );
		}
		
		public String
		getDesc()
		{
			if ( type != REG_TYPE_FULL ){
			
				return( " (" + desc + ")" );
			}
			
			return( "" );
		}
	}
	
	public static class 
	TriangleSlicer 
	{	
		int width;
		
		private double w;
		private double w2;
		private double h;
		
		private double tan60;
		
		public TriangleSlicer(int width) {
			this.width = width;
			
			this.w = (float) width;
			this.w2 = w / 2;
			this.h = Math.cos(Math.PI / 6) * w;
			
			this.tan60 = Math.tan(Math.PI / 3);
			
		}
		
		/**
		 * 
		 * @param x
		 * @param y
		 * @return an array of int values being x,y coordinate pairs
		 */
		public int[] findVertices(double x,double y) {

			int yN = (int) Math.floor((y / h));
			int xN = (int) Math.floor((x /w2));
			
			double v1x,v2x,v3x,v1y,v2y,v3y;
			
			//weither the triangle is like /\ (true) or \/ (false)
			boolean upTriangle;
			
			if((xN+yN) % 2 == 0) {
				// we have a / separator in the "cell"
				if( (y-h*yN) > (x-w2*xN) * tan60 ) {
					//we're in the upper part
					upTriangle = false;
					v1x = w2 * (xN - 1);
					v1y = h * (yN + 1) ;
				} else {
					//we're in the lower part
					upTriangle = true;
					v1x = w2 * xN;
					v1y = h * yN;
				}
			} else {
				// We have a \ separator in the "cell"
				if( (y- h*yN) > (w2 - (x-w2*xN)) * tan60 ) {
					//we're in the upper part
					upTriangle = false;
					v1x = w2 * xN;
					v1y = h * (yN+1);
				} else {
					//we're in the lower part
					upTriangle = true;
					v1x = w2 * (xN - 1);
					v1y = h * yN;
				}
			}

			if(upTriangle) {
				v2x = v1x + w;
				v2y = v1y;
				
				v3x = v1x + w2;
				v3y = v1y + h;
			} else {
				v2x = v1x + w;
				v2y = v1y;
				
				v3x = v1x + w2;
				v3y = v1y - h;
			}
			
			int[] result = new int[6];
			
			result[0] = (int) v1x;
			result[1] = (int) v1y;
			
			result[2] = (int) v2x;
			result[3] = (int) v2y;
			
			result[4] = (int) v3x;
			result[5] = (int) v3y;
			
			return result;

		}
	}
}
