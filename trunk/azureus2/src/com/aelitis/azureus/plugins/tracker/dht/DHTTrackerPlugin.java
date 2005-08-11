/*
 * Created on 31-Jan-2005
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

package com.aelitis.azureus.plugins.tracker.dht;


import java.net.URL;
import java.net.UnknownHostException;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.peer.PEPeerSource;
import org.gudy.azureus2.core3.tracker.protocol.PRHelpers;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AENetworkClassifier;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread;
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
import org.gudy.azureus2.plugins.download.DownloadListener;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.download.DownloadPropertyEvent;
import org.gudy.azureus2.plugins.download.DownloadPropertyListener;
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
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.plugins.utils.UTTimerEvent;
import org.gudy.azureus2.plugins.utils.UTTimerEventPerformer;

import com.aelitis.azureus.plugins.dht.*;

/**
 * @author parg
 *
 */

public class 
DHTTrackerPlugin 
	implements Plugin, DownloadListener, DownloadPropertyListener, DownloadTrackerListener
{
	private static final String	PLUGIN_NAME			= "Distributed Tracker";
	
	private static final int	ANNOUNCE_TIMEOUT	= 2*60*1000;
	private static final int	SCRAPE_TIMEOUT		= 30*1000;
	
	private static final int	ANNOUNCE_MIN_DEFAULT		= 2*60*1000;
	private static final int	ANNOUNCE_MAX				= 60*60*1000;
	
	private static final boolean	TRACK_NORMAL_DEFAULT	= true;
	
	private static final int	NUM_WANT			= 35;	// Limit to ensure replies fit in 1 packet

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

	private Set					running_downloads 		= new HashSet();
	private Map					registered_downloads 	= new HashMap();
	
	private Map					query_map			 	= new HashMap();
	
	private Map					in_progress				= new HashMap();
	
	private BooleanParameter	track_normal_when_offline;
	
	private LoggerChannel		log;
	
	private Map					scrape_injection_map = new WeakHashMap();
	
	private AEMonitor	this_mon	= new AEMonitor( "DHTTrackerPlugin" );
	
	
	public void
	initialize(
		PluginInterface 	_plugin_interface )
	{
		plugin_interface	= _plugin_interface;
				
		plugin_interface.getPluginProperties().setProperty( "plugin.version", 	"1.0" );
		plugin_interface.getPluginProperties().setProperty( "plugin.name", 		PLUGIN_NAME );

		log = plugin_interface.getLogger().getTimeStampedChannel(PLUGIN_NAME);

		
		ta_networks 	= plugin_interface.getTorrentManager().getAttribute( TorrentAttribute.TA_NETWORKS );
		ta_peer_sources = plugin_interface.getTorrentManager().getAttribute( TorrentAttribute.TA_PEER_SOURCES );

		UIManager	ui_manager = plugin_interface.getUIManager();

		final BasicPluginViewModel model = 
			ui_manager.createBasicPluginViewModel( PLUGIN_NAME);
		
		BasicPluginConfigModel	config = 
			ui_manager.createBasicPluginConfigModel( "Plugins", "plugins.dhttracker" );
			
		track_normal_when_offline = config.addBooleanParameter2( "dhttracker.tracknormalwhenoffline", "dhttracker.tracknormalwhenoffline", TRACK_NORMAL_DEFAULT );

		track_normal_when_offline.addListener(
			new ParameterListener()
			{
				public void
				parameterChanged(
					Parameter	param )
				{
					configChanged();
				}
			});
		
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
						
						Thread	t = 
							new AEThread( "DHTTrackerPlugin:init" )
							{
								public void
								runSupport()
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
							
						t.setDaemon( true );
						
						t.start();

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
					public void
					downloadAdded(
						Download	download )
					{
						download.addPropertyListener( DHTTrackerPlugin.this );
						
						download.addTrackerListener( DHTTrackerPlugin.this );
						
						download.addListener( DHTTrackerPlugin.this );
						
						checkDownloadForRegistration( download, true );
					}
					
					public void
					downloadRemoved(
						Download	download )
					{
						download.removePropertyListener( DHTTrackerPlugin.this );
						
						download.removeTrackerListener( DHTTrackerPlugin.this );

						download.removeListener( DHTTrackerPlugin.this );
						
						try{
							this_mon.enter();
				
							running_downloads.remove( download );
							
						}finally{
							
							this_mon.exit();
						}
					}
				});
		
		plugin_interface.getUtilities().createTimer("DHT Tracker").addPeriodicEvent(
			15000,
			new UTTimerEventPerformer()
			{
				public void 
				perform(
					UTTimerEvent event) 
				{
					processRegistrations();
				}
			});
	}
	
	public void
	propertyChanged(
		Download				download,
		DownloadPropertyEvent	event )
	{
		if ( event.getType() == DownloadPropertyEvent.PT_TORRENT_ATTRIBUTE_WRITTEN ){
			
			if ( 	event.getData() == ta_networks ||
					event.getData() == ta_peer_sources ){
				
				checkDownloadForRegistration( download, false );
			}
		}
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
			
		boolean	register_it	= false;
		
		String	register_reason;
		
		if ( 	state == Download.ST_DOWNLOADING 	||
				state == Download.ST_SEEDING 		||
				state == Download.ST_QUEUED ){
			
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
						
						register_it	= true;
						
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
										
								if( torrent.isDecentralisedBackupRequested()){
									
									register_it	= true;
									
									register_reason = "torrent requests decentralised tracking";
									
								}else if ( track_normal_when_offline.getValue()){
									
										// only track if torrent's tracker is not available
																
									if ( 	state == Download.ST_DOWNLOADING ||
											state == Download.ST_SEEDING ){
										
										DownloadAnnounceResult result = download.getLastAnnounceResult();
										
										if (	result == null ||
												result.getResponseType() == DownloadAnnounceResult.RT_ERROR ||
												TorrentUtils.isDecentralised(result.getURL())){
											
											register_it	= true;
											
											register_reason = "tracker unavailable (announce)";
											
										}else{	
											
											register_reason = "tracker available (announce: " + result.getURL() + ")";								
										}
									}else{
										
										DownloadScrapeResult result = download.getLastScrapeResult();
										
										if (	result == null || 
												result.getResponseType() == DownloadScrapeResult.RT_ERROR ||
												TorrentUtils.isDecentralised(result.getURL())){
											
											register_it	= true;
											
											register_reason = "tracker unavailable (scrape)";
											
										}else{
											
											register_reason = "tracker available (scrape: " + result.getURL() + ")";								
										}
									}
								}else{
									register_it	= true;
									
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
		}else if ( 	state == Download.ST_STOPPED ||
					state == Download.ST_ERROR ){
			
			register_reason	= "not running";
			
		}else{
			
			register_reason	= "";
		}
		
		if ( register_reason.length() > 0 ){
			
			try{
				this_mon.enter();
	
				if ( register_it ){
				
					if ( !running_downloads.contains( download )){
						
						log.log( "Monitoring '" + download.getName() + "': " + register_reason );
						
						running_downloads.add( download );
					}
				}else{
					
					if ( running_downloads.contains( download )){
						
						log.log( "Not monitoring '" + download.getName() + "': " + register_reason );
	
						running_downloads.remove( download );
					}else{
						
						if ( first_time ){
							
							log.log( "Not monitoring '" + download.getName() + "': " + register_reason );
						}
					}
				}
			}finally{
				
				this_mon.exit();
			}
		}
	}
	
	protected void
	processRegistrations()
	{
		ArrayList	rds;
	
		try{
			this_mon.enter();

			rds = new ArrayList(running_downloads);
			
		}finally{
			
			this_mon.exit();
		}

		long	 now = SystemTime.getCurrentTime();
		
		Iterator	it = rds.iterator();
		
		while( it.hasNext()){
			
			final Download	dl = (Download)it.next();
			
			Byte	existing_flags = (Byte)registered_downloads.get( dl );

			byte	new_flags = dl.isComplete()?DHTPlugin.FLAG_SEEDING:DHTPlugin.FLAG_DOWNLOADING;
				
			if ( existing_flags == null || existing_flags.byteValue() != new_flags ){
				
				log.log( "Registering download '" + dl.getName() + "' as " + (new_flags == DHTPlugin.FLAG_SEEDING?"Seeding":"Downloading"));
				
				final 	long	start = SystemTime.getCurrentTime();
				
				registered_downloads.put( dl, new Byte( new_flags ));
				
				try{ 
					this_mon.enter();

					query_map.put( dl, new Long( now ));
					
				}finally{
					
					this_mon.exit();
				}
				
				int	port = plugin_interface.getPluginconfig().getIntParameter( "TCP.Listen.Port" );

		 		String port_override = COConfigurationManager.getStringParameter("TCP.Announce.Port","");
		 		
		  		if( !port_override.equals("")){
		 
		  			try{
		  				port	= Integer.parseInt( port_override );
		  				
		  			}catch( Throwable e ){
		  			}
		  		}
		  		
		  		if ( port == 0 ){
		  			
		  			log.log( "    port = 0, registration not performed" );
		  			
		  		}else{
		  			
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
			  	    
			  	    	// format is [ip_override:]port
			  	    
			  	    String	value_to_put = override_ip==null?"":(override_ip+":");
			  	    
			  	    value_to_put += port;
			  	    	
			  	    // don't let a put block an announce as we don't want to be waiting for 
			  	    // this at start of day to get a torrent running
			  	    
			  	    // increaseActive( dl );
			  	    
					dht.put( 
							dl.getTorrent().getHash(),
							"Tracker registration of '" + dl.getName() + "' -> " + value_to_put,
							value_to_put.getBytes(),
							new_flags,
							new DHTPluginOperationListener()
							{
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
									boolean	timeout_occurred )
								{
									log.log( "Registration of '" + dl.getName() + "' completed (elapsed=" + (SystemTime.getCurrentTime()-start) + ")");
									
									// decreaseActive( dl );
								}
							});
		  		}
			}
		}
		
		it = registered_downloads.keySet().iterator();
		
		while( it.hasNext()){
			
			final Download	dl = (Download)it.next();

			boolean	unregister;
			
			try{ 
				this_mon.enter();

				unregister = !running_downloads.contains( dl );
				
			}finally{
				
				this_mon.exit();
			}
			
			if ( unregister ){
				
				log.log( "Unregistering download '" + dl.getName() + "'" );
				
				final long	start = SystemTime.getCurrentTime();
				
				it.remove();
				
				try{
					this_mon.enter();

					query_map.remove( dl );
					
				}finally{
					
					this_mon.exit();
				}
				
				increaseActive( dl );
				
				dht.remove( 
						dl.getTorrent().getHash(),
						"Tracker deregistration of '" + dl.getName() + "'",
						new DHTPluginOperationListener()
						{
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
								boolean	timeout_occurred )
							{
								log.log( "Unregistration of '" + dl.getName() + "' completed (elapsed=" + (SystemTime.getCurrentTime()-start) + ")");
								
								decreaseActive( dl );
							}
						});
			}
		}
		
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
			
				try{
					this_mon.enter();
		
					query_map.remove( dl );
					
				}finally{
					
					this_mon.exit();
				}
				
				final long	start = SystemTime.getCurrentTime();
					
					// if we're already connected to > NUM_WANT peers then don't bother announcing
				
				PeerManager	pm = dl.getPeerManager();
				
					// don't query if this download already has an active DHT operation
				
				boolean	skip	= isActive( dl );
				
				if ( skip ){
					
					log.log( "Deferring announce for '" + dl.getName() + "' as activity outstanding" );
				}
				
				if ( pm != null && !skip ){
					
					int	con = pm.getStats().getConnectedLeechers() + pm.getStats().getConnectedSeeds();
				
					skip = con >= NUM_WANT;
				}
				
				if ( skip ){
					
					try{
						this_mon.enter();
					
						if ( running_downloads.contains( dl )){
							
								// use "min" here as we're just deferring it
							
							query_map.put( dl, new Long( start + ANNOUNCE_MIN_DEFAULT ));
						}
						
					}finally{
						
						this_mon.exit();
					}
				}else{
					final Torrent	torrent = dl.getTorrent();
					
					final URL	url_to_report = torrent.isDecentralised()?torrent.getAnnounceURL():DEFAULT_URL;
					
					increaseActive( dl );
					
					dht.get(dl.getTorrent().getHash(), 
							"Tracker announce for '" + dl.getName() + "'",
							dl.isComplete()?DHTPlugin.FLAG_SEEDING:DHTPlugin.FLAG_DOWNLOADING,
							NUM_WANT, 
							ANNOUNCE_TIMEOUT,
							false,
							new DHTPluginOperationListener()
							{
								List	addresses 	= new ArrayList();
								List	ports		= new ArrayList();
								
								int		seed_count;
								int		leecher_count;
								
								public void
								valueRead(
									DHTPluginContact	originator,
									DHTPluginValue		value )
								{
									String	str_val = new String(value.getValue());
									
										// for future hacks we trim anything after a ';'
									
									int sep = str_val.indexOf(';');
									
									if ( sep != -1 ){
										
										str_val = str_val.substring(0,sep);
									}
									
									try{
										sep = str_val.indexOf(':');
									
										String	ip_str		= null;
										String	port_str;
										
										if ( sep == -1 ){
											
											port_str = str_val;
											
										}else{
											
											ip_str 		= str_val.substring( 0, sep );
											
											port_str	= str_val.substring( sep+1 );	
										}
										
										int	port = Integer.parseInt( port_str );
																			
										addresses.add( 
												ip_str==null?originator.getAddress().getAddress().getHostAddress():ip_str);
										
										ports.add(new Integer(port));
										
										if (( value.getFlags() & DHTPlugin.FLAG_DOWNLOADING ) == 1 ){
											
											leecher_count++;
											
										}else{
											
											seed_count++;
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
									boolean	timeout_occurred )
								{
									log.log( "Get of '" + dl.getName() + "' completed (elapsed=" + (SystemTime.getCurrentTime()-start) + "), addresses = " + addresses.size() + ", seeds = " + seed_count + ", leechers = " + leecher_count );
								
									decreaseActive(dl);
									
									final DownloadAnnounceResultPeer[]	peers = new
										DownloadAnnounceResultPeer[addresses.size()];
									
										// scale min and max based on number of active torrents
										// we don't want more than a few announces a minute
									
									int	announce_per_min = 4;
									
									int	num_active = query_map.size();
									
									int	announce_min = Math.max( ANNOUNCE_MIN_DEFAULT, ( num_active / announce_per_min )*60*1000 );
									
									announce_min = Math.min( announce_min, ANNOUNCE_MAX );
									
									final long	retry = announce_min + peers.length*(ANNOUNCE_MAX-announce_min)/NUM_WANT;
																		
									try{
										this_mon.enter();
									
										if ( running_downloads.contains( dl )){
											
											query_map.put( dl, new Long( SystemTime.getCurrentTime() + retry ));
										}
										
									}finally{
										
										this_mon.exit();
									}
									
									for (int i=0;i<peers.length;i++){
										
										final int f_i = i;
										
										peers[i] = 
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
												
												public byte[]
												getPeerID()
												{
													return( null );
												}
											};
										
									}
																	
									if ( 	dl.getState() == Download.ST_DOWNLOADING ||
											dl.getState() == Download.ST_SEEDING ){
									
										dl.setAnnounceResult(
												new DownloadAnnounceResult()
												{
													public Download
													getDownload()
													{
														return( dl );
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
									
									DownloadScrapeResult result = dl.getLastScrapeResult();
																		
									if (	result == null || 
											result.getResponseType() == DownloadScrapeResult.RT_ERROR ){									
			
									}else{
									
											// if the currently reported values are the same as the 
											// ones we previously injected then overwrite them
											// note that we can't test the URL to see if we originated
											// the scrape values as this gets replaced when a normal
											// scrape fails :(
											
										int[]	prev = (int[])scrape_injection_map.get( dl );
											
										if ( 	prev != null && 
												prev[0] == result.getSeedCount() &&
												prev[1] == result.getNonSeedCount()){
																								
											inject_scrape	= true;
										}
									}
									
									if ( torrent.isDecentralised() || inject_scrape ){
										
										
											// make sure that the injected scrape values are consistent
											// with our currently connected peers
										
										PeerManager	pm = dl.getPeerManager();
										
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
										
										scrape_injection_map.put( dl, new int[]{ f_adj_seeds, f_adj_leechers });

										dl.setScrapeResult(
											new DownloadScrapeResult()
											{
												public Download
												getDownload()
												{
													return( dl );
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
				
				if ( running_downloads.contains( download )){
					
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
				false,
				new DHTPluginOperationListener()
				{
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
}
