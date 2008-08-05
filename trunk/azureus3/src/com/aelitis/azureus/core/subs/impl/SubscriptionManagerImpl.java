/*
 * Created on Jul 11, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.core.subs.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.bouncycastle.util.encoders.Base64;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadCompletionListener;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.download.DownloadPeerListener;
import org.gudy.azureus2.plugins.peers.PeerManager;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerEvent;
import org.gudy.azureus2.plugins.utils.DelayedTask;
import org.gudy.azureus2.plugins.utils.StaticUtilities;
import org.gudy.azureus2.pluginsimpl.local.torrent.TorrentImpl;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreLifecycleAdapter;
import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.lws.LightWeightSeed;
import com.aelitis.azureus.core.lws.LightWeightSeedManager;
import com.aelitis.azureus.core.messenger.config.PlatformSubscriptionsMessenger;
import com.aelitis.azureus.core.subs.Subscription;
import com.aelitis.azureus.core.subs.SubscriptionAssociationLookup;
import com.aelitis.azureus.core.subs.SubscriptionException;
import com.aelitis.azureus.core.subs.SubscriptionLookupListener;
import com.aelitis.azureus.core.subs.SubscriptionManager;
import com.aelitis.azureus.core.subs.SubscriptionManagerListener;
import com.aelitis.azureus.core.subs.SubscriptionPopularityListener;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.core.vuzefile.VuzeFile;
import com.aelitis.azureus.core.vuzefile.VuzeFileComponent;
import com.aelitis.azureus.core.vuzefile.VuzeFileHandler;
import com.aelitis.azureus.core.vuzefile.VuzeFileProcessor;
import com.aelitis.azureus.plugins.dht.DHTPlugin;
import com.aelitis.azureus.plugins.dht.DHTPluginContact;
import com.aelitis.azureus.plugins.dht.DHTPluginKeyStats;
import com.aelitis.azureus.plugins.dht.DHTPluginOperationListener;
import com.aelitis.azureus.plugins.dht.DHTPluginValue;
import com.aelitis.azureus.plugins.magnet.MagnetPlugin;
import com.aelitis.azureus.plugins.magnet.MagnetPluginProgressListener;


public class 
SubscriptionManagerImpl 
	implements SubscriptionManager
{
	private static final String	CONFIG_FILE = "subscriptions.config";
	private static final String	LOGGER_NAME = "Subscriptions";

	private static SubscriptionManagerImpl		singleton;
	
	public static void
	preInitialise()
	{
		VuzeFileHandler.getSingleton().addProcessor(
			new VuzeFileProcessor()
			{
				public void
				process(
					VuzeFile[]		files,
					int				expected_types )
				{
					for (int i=0;i<files.length;i++){
						
						VuzeFile	vf = files[i];
						
						VuzeFileComponent[] comps = vf.getComponents();
						
						for (int j=0;j<comps.length;j++){
							
							VuzeFileComponent comp = comps[j];
							
							if ( comp.getType() == VuzeFileComponent.COMP_TYPE_SUBSCRIPTION ){
								
								try{
									((SubscriptionManagerImpl)getSingleton()).importSubscription(
											comp.getContent(),
											( expected_types & VuzeFileComponent.COMP_TYPE_SUBSCRIPTION ) == 0 );
									
									comp.setProcessed();
									
								}catch( Throwable e ){
									
									Debug.printStackTrace(e);
								}
							}
						}
					}
				}
			});		
	}
		
	public static synchronized SubscriptionManager
	getSingleton()
	{
		if ( singleton == null ){
			
			singleton = new SubscriptionManagerImpl();
		}
		
		return( singleton );
	}
	
	
	private boolean		started;
		
	private static final int	TIMER_PERIOD		= 30*1000;
	
	private static final int	ASSOC_CHECK_PERIOD	= 5*60*1000;
	private static final int	ASSOC_CHECK_TICKS	= ASSOC_CHECK_PERIOD/TIMER_PERIOD;
	
	private static final int	SERVER_PUB_CHECK_PERIOD	= 10*60*1000;
	private static final int	SERVER_PUB_CHECK_TICKS	= SERVER_PUB_CHECK_PERIOD/TIMER_PERIOD;

	
	private volatile DHTPlugin	dht_plugin;
	
	private List		subscriptions	= new ArrayList();
	
	private boolean	config_dirty;
	
	private boolean	publish_associations_active;
	private boolean publish_subscription_active;
	
	private TorrentAttribute		ta_subs_download;
	private TorrentAttribute		ta_subs_download_rd;
	private TorrentAttribute		ta_subscription_info;
	
	private boolean					periodic_lookup_in_progress;
	
	private CopyOnWriteList			listeners = new CopyOnWriteList();
	
	
	private AEDiagnosticsLogger		logger;
	
	
	protected
	SubscriptionManagerImpl()
	{
		loadConfig();

		AzureusCore	core = AzureusCoreFactory.getSingleton();
		
		core.addLifecycleListener(
			new AzureusCoreLifecycleAdapter()
			{
				public void
				started(
					AzureusCore		core )
				{
					core.removeLifecycleListener( this );
					
					startUp();
				}
			});
		
		if ( core.isStarted()){
		
			startUp();
		}
	}

	protected void
	startUp()
	{
		synchronized( this ){
			
			if ( started ){
				
				return;
			}
			
			started	= true;
		}
		
		PluginInterface  dht_plugin_pi  = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByClass( DHTPlugin.class );
		
		if ( dht_plugin_pi != null ){
			
			dht_plugin = (DHTPlugin)dht_plugin_pi.getPlugin();

			final PluginInterface default_pi = StaticUtilities.getDefaultPluginInterface();
	
			if ( Constants.isCVSVersion()){
				
				addListener(
						new SubscriptionManagerListener()
						{
							public void 
							subscriptionAdded(
								Subscription subscription ) 
							{
							}
				
							public void
							subscriptionChanged(
								Subscription		subscription )
							{
							}
							
							public void 
							subscriptionRemoved(
								Subscription subscription ) 
							{
							}
							
							public void 
							associationsChanged(
								byte[] hash )
							{
								/*
								System.out.println( "Subscriptions changed: " + ByteFormatter.encodeString( hash ));
								
								Subscription[] subs = getKnownSubscriptions( hash );
							
								for (int i=0;i<subs.length;i++){
									
									System.out.println( "    " + subs[i].getString());
								}
								*/
							}
						});	
			}
	
			ta_subs_download 		= default_pi.getTorrentManager().getPluginAttribute( "azsubs.subs_dl" );
			ta_subs_download_rd 	= default_pi.getTorrentManager().getPluginAttribute( "azsubs.subs_dl_rd" );
			ta_subscription_info 	= default_pi.getTorrentManager().getPluginAttribute( "azsubs.subs_info" );
	
	
			DelayedTask dt = 
				default_pi.getUtilities().createDelayedTask(
					new Runnable()
					{
						public void 
						run() 
						{
							Download[] downloads = default_pi.getDownloadManager().getDownloads();
									
							for (int i=0;i<downloads.length;i++){
								
								Download download = downloads[i];
								
								if ( download.getBooleanAttribute( ta_subs_download )){
									
									Map	rd = download.getMapAttribute( ta_subs_download_rd );
									
									boolean	delete_it;
									
									if ( rd == null ){
										
										delete_it = true;
										
									}else{
										
										delete_it = !recoverSubscriptionUpdate( download, rd );
									}
									
									if ( delete_it ){
										
										try{
											download.stop();
											
										}catch( Throwable e ){
										}
										
										try{
											download.remove( true, true );
											
											log( "Removed non-recoverable download '" + download.getName() + "'" );

										}catch( Throwable e ){
											
											log( "Failed to remove non-recoverable download '" + download.getName() + "'", e );
										}
									}
								}
							}
								
							default_pi.getDownloadManager().addListener(
								new DownloadManagerListener()
								{
									public void
									downloadAdded(
										Download	download )
									{
										lookupAssociations();
									}
									
									public void
									downloadRemoved(
										Download	download )
									{
									}
								});
							
							publishAssociations();
							
							publishSubscriptions();
							
							SimpleTimer.addPeriodicEvent(
									"SubscriptionChecker",
									TIMER_PERIOD,
									new TimerEventPerformer()
									{
										private int	ticks;
										
										public void 
										perform(
											TimerEvent event )
										{
											ticks++;
											
											checkStuff( ticks );
										}
									});
						}
					});
		
			dt.queue();
		}
		
		/*
		if ( subscriptions.size() == 0 ){
			
			try{
				create( "test subscription 2", true, "blah blah" );
				
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
	
			for (int i=0;i<subscriptions.size();i++){
			
				((Subscription)subscriptions.get(i)).addAssociation( ByteFormatter.decodeString( "E02E5E117A5A9080D552A11FA675DE868A05FE71" ));
			}
		}
		*/
	}

	public Subscription 
	create(
		String			name,
		boolean			public_subs,
		String			json )
	
		throws SubscriptionException 
	{
		if ( getSubscriptionFromName( name ) != null ){
			
			throw ( new SubscriptionException( "Subscription with name '" + name + "' already exists" ));
		}
		
		SubscriptionImpl subs = new SubscriptionImpl( this, name, public_subs, json );
		
		log( "Created new subscription: " + subs.getString());
		
		if ( subs.isPublic()){
			
			updatePublicSubscription( subs, json );
		}
		
		addSubscription( subs );
				
		return( subs );
	}
	
	
	protected void
	addSubscription(
		SubscriptionImpl		subs )
	{
		synchronized( this ){
			
			subscriptions.add( subs );
			
			saveConfig();
		}
		
		Iterator it = listeners.iterator();
		
		while( it.hasNext()){
			
			((SubscriptionManagerListener)it.next()).subscriptionAdded( subs );
		}
		
		if ( dht_plugin != null ){
				
			publishSubscriptions();
		}
	}
	
	protected void
	changeSubscription(
		SubscriptionImpl		subs )
	{
		Iterator it = listeners.iterator();
		
		while( it.hasNext()){
			
			((SubscriptionManagerListener)it.next()).subscriptionChanged( subs );
		}
	}
	
	protected void
	removeSubscription(
		SubscriptionImpl		subs )
	{
		synchronized( this ){
			
			if ( subscriptions.remove( subs )){
			
				saveConfig();
				
			}else{
			
				return;
			}
		}
		
		Iterator it = listeners.iterator();
		
		while( it.hasNext()){
			
			((SubscriptionManagerListener)it.next()).subscriptionRemoved( subs );
		}
	}
	
	protected void
	updatePublicSubscription(
		SubscriptionImpl		subs,
		String					json )
	{		
		try{
			if ( json == null ){
				
				json = subs.getJSON();
			}
						
			File vf = getVuzeFile( subs );

			byte[] bytes = FileUtil.readFileAsByteArray( vf );
			
			byte[]	encoded_subs = Base64.encode( bytes );

			PlatformSubscriptionsMessenger.updateSubscription(
					!subs.getServerPublished(),
					subs.getName(),
					subs.getPublicKey(),
					subs.getPrivateKey(),
					subs.getShortID(),
					subs.getVersion(),
					new String( encoded_subs ));
			
			subs.setServerPublished();
			
			log( "    Updated public subscription " + subs.getString());
			
		}catch( Throwable e ){
			
			log( "    Failed to update public subscription " + subs.getString(), e );
			
			subs.setServerPublicationOutstanding();
		}
	}
	
	protected void
	checkServerPublications(
		List		subs )
	{
		for (int i=0;i<subs.size();i++){
			
			SubscriptionImpl	sub = (SubscriptionImpl)subs.get(i);
			
			if ( sub.getServerPublicationOutstanding()){
				
				updatePublicSubscription( sub, null );
			}
		}
	}
	
	protected void
	checkStuff(
		int		ticks )
	{
		List subs;
		
		synchronized( this ){
			
			subs = new ArrayList( subscriptions );
		}
		
		for (int i=0;i<subs.size();i++){
			
			((SubscriptionImpl)subs.get(i)).checkPublish();
		}
		
		if ( ticks % ASSOC_CHECK_TICKS == 0 ){
			
			lookupAssociations();
		}
		
		if ( ticks % SERVER_PUB_CHECK_TICKS == 0 ){
			
			checkServerPublications( subs );
		}
	}
	
	public Subscription
	importSubscription(
		Map			map,
		boolean		warn_user )
	
		throws SubscriptionException
	{
		try{
			SubscriptionBodyImpl body = new SubscriptionBodyImpl( this, map );
					
			SubscriptionImpl existing = getSubscriptionFromPublicKey( body.getPublicKey());
			
			if ( existing != null ){
			
				if ( existing.getVersion() <= body.getVersion()){
					
					log( "Not upgrading subscription: " + existing.getString() + " as supplied is not more recent");
					
					if ( warn_user ){
						
						UIManager ui_manager = StaticUtilities.getUIManager( 120*1000 );
						
						String details = MessageText.getString(
								"subscript.add.dup.desc",
								new String[]{ existing.getName()});
						
						ui_manager.showMessageBox(
								"subscript.add.dup.title",
								"!" + details + "!",
								UIManagerEvent.MT_OK );
					}
						// we have a newer one, ignore
					
					return( existing );
					
				}else{
					
					if ( warn_user ){
						
						UIManager ui_manager = StaticUtilities.getUIManager( 120*1000 );
		
						String details = MessageText.getString(
								"subscript.add.upgrade.desc",
								new String[]{ existing.getName()});
						
						long res = ui_manager.showMessageBox(
								"subscript.add.upgrade.title",
								"!" + details + "!",
								UIManagerEvent.MT_YES | UIManagerEvent.MT_NO );
						
						if ( res != UIManagerEvent.MT_YES ){	
						
							throw( new SubscriptionException( "User declined upgrade" ));
						}
					}
					
					log( "Upgrading subscription: " + existing.getString());
	
					existing.upgrade( body );
					
					saveConfig();
					
					subscriptionUpdated();
					
					return( existing );
				}
			}else{
				
				SubscriptionImpl new_subs = new SubscriptionImpl( this, body, SubscriptionImpl.ADD_TYPE_IMPORT, true );
	
				if ( warn_user ){
					
					UIManager ui_manager = StaticUtilities.getUIManager( 120*1000 );
		
					String details = MessageText.getString(
							"subscript.add.desc",
							new String[]{ new_subs.getName()});
					
					long res = ui_manager.showMessageBox(
							"subscript.add.title",
							"!" + details + "!",
							UIManagerEvent.MT_YES | UIManagerEvent.MT_NO );
					
					if ( res != UIManagerEvent.MT_YES ){	
					
						throw( new SubscriptionException( "User declined addition" ));
					}
				}
				
				log( "Imported new subscription: " + new_subs.getString());
				
				addSubscription( new_subs );
				
				return( new_subs );
			}
		}catch( SubscriptionException e ){
				
			throw( e );
				
		}catch( Throwable e ){
			
			throw( new SubscriptionException( "Subscription import failed", e ));
		}
	}
	
	public Subscription[]
	getSubscriptions()
	{
		synchronized( this ){
			
			return((Subscription[])subscriptions.toArray( new Subscription[subscriptions.size()]));
		}
	}
	
	public SubscriptionImpl
	getSubscriptionFromName(
		String		name )
	{
		synchronized( this ){
			
			for (int i=0;i<subscriptions.size();i++){
				
				SubscriptionImpl s = (SubscriptionImpl)subscriptions.get(i);
				
				if ( s.getName().equalsIgnoreCase( name )){
					
					return( s );
				}
			}
		}
		
		return( null );
	}
	
	public SubscriptionImpl
	getSubscriptionFromPublicKey(
		byte[]		public_key )
	{
		synchronized( this ){
			
			for (int i=0;i<subscriptions.size();i++){
				
				SubscriptionImpl s = (SubscriptionImpl)subscriptions.get(i);
				
				if ( Arrays.equals( s.getPublicKey(), public_key )){
					
					return( s );
				}
			}
		}
		
		return( null );
	}
	
	public SubscriptionImpl
	getSubscriptionFromSID(
		byte[]		sid )
	{
		synchronized( this ){
			
			for (int i=0;i<subscriptions.size();i++){
				
				SubscriptionImpl s = (SubscriptionImpl)subscriptions.get(i);
				
				if ( Arrays.equals( s.getShortID(), sid )){
					
					return( s );
				}
			}
		}
		
		return( null );
	}
	
	protected File
	getSubsDir()
	
		throws IOException
	{
		File dir = new File(SystemProperties.getUserPath());

		dir = new File( dir, "subs" );
 		
 		if ( !dir.exists()){
 			
 			if ( !dir.mkdirs()){
 				
 				throw( new IOException( "Failed to create '" + dir + "'" ));
 			}
 		}	
 		
 		return( dir );
	}
	
	protected File
	getVuzeFile(
		SubscriptionImpl 		subs )
	
		throws IOException
	{
 		File dir = getSubsDir();
 		
 		return( new File( dir, ByteFormatter.encodeString( subs.getShortID()) + ".vuze" ));
	}
	
	public Subscription[]
	getKnownSubscriptions(
		byte[]						hash )
	{
		PluginInterface pi = StaticUtilities.getDefaultPluginInterface();

		try{
			Download download = pi.getDownloadManager().getDownload( hash );
			
			if ( download != null ){
				
				Map	m = download.getMapAttribute( ta_subscription_info );
				
				if ( m != null ){
					
					List s = (List)m.get("s");
					
					if ( s != null && s.size() > 0 ){
						
						List	result = new ArrayList( s.size());
						
						for (int i=0;i<s.size();i++){
							
							byte[]	sid = (byte[])s.get(i);
							
							Subscription subs = getSubscriptionFromSID(sid);
							
							if ( subs != null ){
								
								result.add( subs );
							}
						}
						
						return((Subscription[])result.toArray( new Subscription[result.size()]));
					}
				}
			}
		}catch( Throwable e ){
			
			log( "Failed to get known subscriptions", e );
		}
		
		return( new Subscription[0] );
	}
	
	protected void
	lookupAssociations()
	{
		synchronized( this ){
			
			if ( periodic_lookup_in_progress ){
				
				return;
			}
			
			periodic_lookup_in_progress  = true;
		}
		
		try{
			PluginInterface pi = StaticUtilities.getDefaultPluginInterface();
			
			Download[] downloads = pi.getDownloadManager().getDownloads();
					
			long	now = SystemTime.getCurrentTime();
			
			long		newest_time		= 0;
			Download	newest_download	= null;
			
			
			for( int i=0;i<downloads.length;i++){
				
				Download	download = downloads[i];
				
				if ( download.getTorrent() == null || !download.isPersistent()){
					
					continue;
				}
				
				Map	map = download.getMapAttribute( ta_subscription_info );
				
				if ( map == null ){
					
					map = new LightHashMap();
					
				}else{
					
					map = new LightHashMap( map );
				}	
				
				Long	l_last_check = (Long)map.get( "lc" );
				
				long	last_check = l_last_check==null?0:l_last_check.longValue();
				
				if ( last_check > now ){
					
					last_check = now;
					
					map.put( "lc", new Long( last_check ));
					
					download.setMapAttribute( ta_subscription_info, map );
				}
				
				List	subs = (List)map.get( "s" );
				
				int	sub_count = subs==null?0:subs.size();
				
				if ( sub_count > 8 ){
					
					continue;
				}
				
				long	create_time = download.getCreationTime();
				
				int	time_between_checks = (sub_count + 1) * 24*60*60*1000 + (int)(create_time%4*60*60*1000);
				
				if ( now - last_check >= time_between_checks ){
					
					if ( create_time > newest_time ){
						
						newest_time		= create_time;
						newest_download	= download;
					}
				}
			}
			
			if ( newest_download != null ){
			
				byte[] hash = newest_download.getTorrent().getHash();
				
				log( "Periodic lookup starts for " + newest_download.getName() + "/" + ByteFormatter.encodeString( hash ));

				lookupAssociationsSupport( 
					hash,
					new	SubscriptionLookupListener()
					{
						public void
						found(
							byte[]					hash,
							Subscription			subscription )
						{							
						}
						
						public void
						failed(
							byte[]					hash,
							SubscriptionException	error )
						{
							synchronized( SubscriptionManagerImpl.this ){
								
								periodic_lookup_in_progress = false;
							}
							
							log( "Periodic lookup failed for " + ByteFormatter.encodeString( hash ), error );

						}
						
						public void 
						complete(
							byte[] 			hash,
							Subscription[]	subs )
						{
							synchronized( SubscriptionManagerImpl.this ){
								
								periodic_lookup_in_progress = false;
							}
							
							log( "Periodic lookup complete for " + ByteFormatter.encodeString( hash ));
						}
					});
						
			}
		}catch( Throwable e ){
			
			synchronized( this ){
				
				periodic_lookup_in_progress = false;
			}
			
			log( "Periodic update check failed", e );
		}
	}
	
	public SubscriptionAssociationLookup
	lookupAssociations(
		final byte[] 							hash,
		final SubscriptionLookupListener		listener )
	
		throws SubscriptionException
	{
		if ( dht_plugin != null && !dht_plugin.isInitialising()){
			
			return( lookupAssociationsSupport( hash, listener ));
		}
		
		final boolean[]	cancelled = { false };
		
		final SubscriptionAssociationLookup[]	actual_res = { null };
		
		final SubscriptionAssociationLookup res = 
			new SubscriptionAssociationLookup()
			{
				public void 
				cancel() 
				{
					log( "    Association lookup cancelled" );
	
					synchronized( actual_res ){
						
						cancelled[0] = true;
						
						if ( actual_res[0] != null ){
							
							actual_res[0].cancel();
						}
					}
				}
			};
			
		new AEThread2( "SM:initwait", true )
		{
			public void
			run()
			{
				try{
					SubscriptionAssociationLookup x = lookupAssociationsSupport( hash, listener );
					
					synchronized( actual_res ){

						actual_res[0] = x;
						
						if ( cancelled[0] ){
							
							x.cancel();
						}
					}
					
				}catch( SubscriptionException e ){
					
					listener.failed( hash, e );
				}
				
			}
		}.start();
		
		return( res );
	}
	
	protected SubscriptionAssociationLookup
	lookupAssociationsSupport(
		final byte[] 							hash,
		final SubscriptionLookupListener		listener )
	
		throws SubscriptionException 
	{
		log( "Looking up associations for '" + ByteFormatter.encodeString( hash ));
		
		final String	key = "subscription:assoc:" + ByteFormatter.encodeString( hash ); 
			
		final boolean[]	cancelled = { false };
		
		dht_plugin.get(
			key.getBytes(),
			"Subscription association read: " + ByteFormatter.encodeString( hash ),
			DHTPlugin.FLAG_SINGLE_VALUE,
			30,
			60*1000,
			true,
			true,
			new DHTPluginOperationListener()
			{
				private Map			hits 					= new HashMap();
				private AESemaphore	hits_sem				= new AESemaphore( "Subs:lookup" );
				private List		found_subscriptions 	= new ArrayList();
				
				private boolean	complete;
				
				public void
				diversified()
				{
				}
				
				public void 
				starts(
					byte[] 				key ) 
				{
				}
				
				public void
				valueRead(
					DHTPluginContact	originator,
					DHTPluginValue		value )
				{
					if ( isCancelled2()){
						
						return;
					}
					
					byte[]	val = value.getValue();
					
					if ( val.length > 4 ){
						
						int	ver = ((val[0]<<16)&0xff0000) | ((val[1]<<8)&0xff00) | (val[2]&0xff);

						byte[]	sid = new byte[ val.length - 4 ];
						
						System.arraycopy( val, 4, sid, 0, sid.length );
						
						log( "    Found subscription " + ByteFormatter.encodeString( sid ) + " version " + ver );
						
						HashWrapper hw = new HashWrapper( sid );
						
						boolean	new_sid = false;
						
						synchronized( this ){
							
							if ( complete ){
								
								return;
							}
							
							Integer v = (Integer)hits.get(hw);
							
							if ( v != null ){
								
								if ( ver > v.intValue()){
									
									hits.put( hw, new Integer( ver ));								
								}
							}else{
								
								new_sid = true;
								
								hits.put( hw, new Integer( ver ));
							}
						}
						
						if ( new_sid ){
							
								// check if already subscribed
							
							SubscriptionImpl subs = getSubscriptionFromSID( sid );
							
							if ( subs != null ){
								
								found_subscriptions.add( subs );
								
								try{
									listener.found( hash, subs );
									
								}catch( Throwable e ){
									
									Debug.printStackTrace(e);
								}
								
								hits_sem.release();
								
							}else{
								
								lookupSubscription( 
									hash, 
									sid, 
									ver,
									new subsLookupListener()
									{
										private boolean sem_done = false;
										
										public void
										found(
											byte[]					hash,
											Subscription			subscription )
										{
										}
										
										public void
										complete(
											byte[]					hash,
											Subscription[]			subscriptions )
										{
											done( subscriptions );
										}
										
										public void
										failed(
											byte[]					hash,
											SubscriptionException	error )
										{
											done( new Subscription[0]);
										}
										
										protected void
										done(
											Subscription[]			subs )
										{
											if ( isCancelled()){
												
												return;
											}
											
											synchronized( this ){
												
												if ( sem_done ){
													
													return;
												}
												
												sem_done = true;
											}
											
											if ( subs.length > 0 ){
												
												found_subscriptions.add( subs[0] );
												
												try{
													listener.found( hash, subs[0] );
													
												}catch( Throwable e ){
													
													Debug.printStackTrace(e);
												}
											}
											
											hits_sem.release();
										}
										
										public boolean 
										isCancelled() 
										{
											return( isCancelled2());
										}
									});
							}
						}
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
					byte[]				original_key,
					boolean				timeout_occurred )
				{
					new AEThread2( "Subs:lookup wait", true )
					{
						public void
						run()
						{
							synchronized( this ){
								
								if ( complete ){
									
									return;
								}
								
								complete = true;
							}
							
							
							for (int i=0;i<hits.size();i++){
								
								if ( isCancelled2()){
									
									return;
								}

								hits_sem.reserve();
							}

							if ( isCancelled2()){
								
								return;
							}

							SubscriptionImpl[] s;
							
							synchronized( this ){
								
								s = (SubscriptionImpl[])found_subscriptions.toArray( new SubscriptionImpl[ found_subscriptions.size() ]);
							}
							
							log( "    Association lookup complete - " + s.length + " found" );

							try{						
								recordAssociations( hash, s, true );
								
							}finally{
								
								listener.complete( hash, s );
							}
						}
					}.start();
				}
				
				protected boolean
				isCancelled2()
				{
					synchronized( cancelled ){
						
						return( cancelled[0] );
					}
				}
			});
		
		return( 
			new SubscriptionAssociationLookup()
			{
				public void 
				cancel() 
				{
					log( "    Association lookup cancelled" );

					synchronized( cancelled ){
						
						cancelled[0] = true;
					}
				}
			});
	}
	
	interface 
	subsLookupListener
		extends SubscriptionLookupListener
	{
		public boolean
		isCancelled();
	}
	
	protected void
	getPopularity(
		final SubscriptionImpl					subs,
		final SubscriptionPopularityListener	listener )
	
		throws SubscriptionException
	{
		try{
			long pop = PlatformSubscriptionsMessenger.getPopularityBySID( subs.getShortID());

			listener.gotPopularity( pop );
			
		}catch( Throwable e ){
			
			log( "Subscription lookup via platform failed", e );

			if ( dht_plugin != null && !dht_plugin.isInitialising()){

				getPopularitySupport( subs, listener );
				
			}else{
				
				new AEThread2( "SM:popwait", true )
				{
					public void
					run()
					{
						getPopularitySupport( subs, listener );
					}
				}.start();
			}
		}
	}
	
	protected void
	getPopularitySupport(
		SubscriptionImpl					subs,
		SubscriptionPopularityListener		listener )
	{
		byte[]	hash = subs.getPublicationHash();
		
		final AESemaphore sem = new AESemaphore( "SM:pop" );
		
		final long[] result = { -1 };
		
		final int timeout = 15*1000;
		
		dht_plugin.get(
				hash,
				"Popularity lookup for subscription " + subs.getName(),
				DHT.FLAG_STATS,
				5,
				timeout,
				false,
				true,
				new DHTPluginOperationListener()
				{
					private boolean	diversified;
					
					private int	hits = 0;
					
					public void
					diversified()
					{
						diversified = true;
					}
					
					public void 
					starts(
						byte[] 				key ) 
					{
					}
					
					public void
					valueRead(
						DHTPluginContact	originator,
						DHTPluginValue		value )
					{
						DHTPluginKeyStats stats = dht_plugin.decodeStats( value );
						
						result[0] = Math.max( result[0], stats.getEntryCount());
						
						hits++;
						
						if ( hits >= 3 ){
							
							sem.release();
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
						byte[]				key,
						boolean				timeout_occurred )
					{
						if ( diversified ){
							
								// TODO: fix?
							
							result[0] *= 11;
							
							if ( result[0] == 0 ){
								
								result[0] = 10;
							}
						}
						
						sem.release();
					}
				});
		
		sem.reserve( timeout );
		
		if ( result[0] == -1 ){
			
			listener.failed( new SubscriptionException( "Timeout" ));
			
		}else{
		
			listener.gotPopularity( result[0] );
		}
	}
	
	protected void
	lookupSubscription(
		final byte[]						association_hash,
		final byte[]						sid,
		final int							version,
		final subsLookupListener			listener )
	{
		try{
			String content = PlatformSubscriptionsMessenger.getSubscriptionBySID( sid );
			
			VuzeFileHandler vfh = VuzeFileHandler.getSingleton();
			
			VuzeFile vf = vfh.loadVuzeFile( Base64.decode( content ));

			VuzeFileComponent[] comps = vf.getComponents();
			
			for (int j=0;j<comps.length;j++){
				
				VuzeFileComponent comp = comps[j];
				
				if ( comp.getType() == VuzeFileComponent.COMP_TYPE_SUBSCRIPTION ){
					
					Map map = comp.getContent();
					
					try{
						SubscriptionBodyImpl body = new SubscriptionBodyImpl( SubscriptionManagerImpl.this, map );
						
						SubscriptionImpl existing = getSubscriptionFromPublicKey( body.getPublicKey());
						
						if ( existing == null ){
							
							SubscriptionImpl new_subs = new SubscriptionImpl( SubscriptionManagerImpl.this, body, SubscriptionImpl.ADD_TYPE_LOOKUP, false );
								
							if ( Arrays.equals( new_subs.getShortID(), sid )){
			
								log( "Added temporary subscription: " + new_subs.getString());
								
								addSubscription( new_subs );
								
								listener.complete( association_hash, new Subscription[]{ new_subs });
								
								return;
							}
						}
					}catch( Throwable e ){
						
						log( "Subscription decode failed", e );
					}
				}
			}
			
			throw( new SubscriptionException( "Platform returned mis-matched, corrupt or missing subscription" ));
			
		}catch( Throwable e ){
			
			if ( listener.isCancelled()){
				
				return;
			}
			
			final String sid_str = ByteFormatter.encodeString( sid );
			
			log( "Subscription lookup via platform for " + sid_str + " failed", e );
			
			if ( getSubscriptionDownloadCount() > 8 ){
				
				log( "Too many existing subscription downloads" );
				
				listener.complete( association_hash, new Subscription[0]);

				return;
			}
				// fall back to DHT
			
			log( "Subscription lookup via DHT starts for " + sid_str, e );

			final String	key = "subscription:publish:" + ByteFormatter.encodeString( sid ) + ":" + version; 
			
			dht_plugin.get(
				key.getBytes(),
				"Subscription lookup read: " + ByteFormatter.encodeString( sid ) + ":" + version,
				DHTPlugin.FLAG_SINGLE_VALUE,
				12,
				60*1000,
				false,
				true,
				new DHTPluginOperationListener()
				{
					private boolean went_async;
					
					public void
					diversified()
					{
					}
					
					public void 
					starts(
						byte[] 				key ) 
					{
					}
					
					public void
					valueRead(
						DHTPluginContact	originator,
						DHTPluginValue		value )
					{
						byte[]	data = value.getValue();
								
						try{
							final Map	details = decodeSubscriptionDetails( data );
							
							if ( SubscriptionImpl.getPublicationVersion( details ) == version ){
								
								log( "    found " + sid_str + ", verification ok" );
								
								synchronized( this ){
									
									if ( went_async  ){
										
										return;
									}
									
									went_async = true;
								}
								
								new AEThread2( "Subs:lookup download", true )
								{
									public void
									run()
									{
										downloadSubscription( 
											association_hash,
											SubscriptionImpl.getPublicationHash( details ),
											sid,
											version,
											SubscriptionImpl.getPublicationSize( details ),
											listener );
									}
								}.start();
							}else{
								
								log( "    found " + sid_str + " but verification failed" );

							}
						}catch( Throwable e ){
							
							log( "    found " + sid_str + " but verification failed", e );

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
						byte[]				original_key,
						boolean				timeout_occurred )
					{
						if ( listener.isCancelled()){
							
							return;
						}
						
						log( "    " + sid_str + " complete" );
						
						synchronized( this ){
							
							if ( !went_async ){
						
								listener.complete( association_hash, new Subscription[0] );
							}
						}
					}
				});
		}
	}
	
	protected void 
	downloadSubscription(
		final byte[]						association_hash,
		byte[]								torrent_hash,
		final byte[]						sid,
		int									version,
		int									size,
		final subsLookupListener		 	listener )
	{
		try{
			Object[] res = downloadTorrent( torrent_hash, size );
			
			if ( listener.isCancelled()){
				
				return;
			}
			
			if ( res == null ){
				
				listener.complete( association_hash, new Subscription[0] );
			}

			downloadSubscription(
				(TOTorrent)res[0], 
				(InetSocketAddress)res[1],
				sid,
				version,
				"Subscription " + ByteFormatter.encodeString( sid ) + " for " + ByteFormatter.encodeString( association_hash ),
				new downloadListener()
				{
					public void
					complete(
						File		data_file )
					{
						boolean	reported = false;
						
						try{
							if ( listener.isCancelled()){
								
								return;
							}
							
							VuzeFileHandler vfh = VuzeFileHandler.getSingleton();
							
							VuzeFile vf = vfh.loadVuzeFile( data_file.getAbsolutePath());
	
							VuzeFileComponent[] comps = vf.getComponents();
							
							for (int j=0;j<comps.length;j++){
								
								VuzeFileComponent comp = comps[j];
								
								if ( comp.getType() == VuzeFileComponent.COMP_TYPE_SUBSCRIPTION ){
									
									Map map = comp.getContent();
									
									try{
										SubscriptionBodyImpl body = new SubscriptionBodyImpl( SubscriptionManagerImpl.this, map );
										
										SubscriptionImpl existing = getSubscriptionFromPublicKey( body.getPublicKey());
										
										if ( existing == null ){
											
											SubscriptionImpl new_subs = new SubscriptionImpl( SubscriptionManagerImpl.this, body, SubscriptionImpl.ADD_TYPE_LOOKUP, false );
												
											if ( Arrays.equals( new_subs.getShortID(), sid )){
											
												log( "Added temporary subscription: " + new_subs.getString());
												
												addSubscription( new_subs );
												
												listener.complete( association_hash, new Subscription[]{ new_subs });
												
												reported = true;
											}
										}else{
											
											listener.complete( association_hash, new Subscription[]{ existing });
											
											reported = true;
										}
									}catch( Throwable e ){
										
										log( "Subscription decode failed", e );
									}
								}
							}
						}finally{
														
							if ( !reported ){
								
								listener.complete( association_hash, new Subscription[0] );
							}
						}
					}
					
					public void
					complete(
						Download	download,	
						File		torrent_file )
					{
						File	data_file = new File( download.getSavePath());
						
						try{
							download.stop();
							
						}catch( Throwable e ){
						}
						
						try{
							download.remove( true, false );

							complete( data_file );
							
						}catch( Throwable e ){
							
							log( "Failed to remove download", e );
							
							listener.complete( association_hash, new Subscription[0] );
							
						}finally{
							
							torrent_file.delete();
							
							data_file.delete();
						}
					}
						
					public void
					failed(
						Throwable	error )
					{
						listener.complete( association_hash, new Subscription[0] );
					}
					
					public Map
					getRecoveryData()
					{
						return( null );
					}
					
					public boolean
					isCancelled()
					{
						return( listener.isCancelled());
					}
				});
				
		}catch( Throwable e ){
			
			listener.complete( association_hash, new Subscription[0] );
		}
	}
	
	protected int
	getSubscriptionDownloadCount()
	{
		PluginInterface pi = StaticUtilities.getDefaultPluginInterface();
		
		Download[] downloads = pi.getDownloadManager().getDownloads();
		
		int	res = 0;
		
		for( int i=0;i<downloads.length;i++){
			
			Download	download = downloads[i];
			
			if ( download.getBooleanAttribute( ta_subs_download )){
				
				res++;
			}
		}
		
		return( res );
	}
	
	protected void
	associationAdded(
		SubscriptionImpl			subscription,
		byte[]						association_hash )
	{
		recordAssociations( association_hash, new SubscriptionImpl[]{ subscription }, false );
		
		if ( dht_plugin != null ){
			
			publishAssociations();
		}
	}
	
	protected void
	recordAssociations(
		byte[]						association_hash,
		SubscriptionImpl[]			subscriptions,
		boolean						full_lookup )
	{
		PluginInterface pi = StaticUtilities.getDefaultPluginInterface();

		boolean	changed = false;
		
		try{
			Download download = pi.getDownloadManager().getDownload( association_hash );
			
			if ( download != null ){
				
				Map	map = download.getMapAttribute( ta_subscription_info );
				
				if ( map == null ){
					
					map = new LightHashMap();
					
				}else{
					
					map = new LightHashMap( map );
				}
				
				List	s = (List)map.get( "s" );
				
				for (int i=0;i<subscriptions.length;i++){
				
					byte[]	sid = subscriptions[i].getShortID();
					
					if ( s == null ){
						
						s = new ArrayList();
						
						s.add( sid );
						
						changed	= true;
						
						map.put( "s", s );
						
					}else{
						
						boolean found = false;
						
						for (int j=0;j<s.size();j++){
							
							byte[]	existing = (byte[])s.get(j);
							
							if ( Arrays.equals( sid, existing )){
								
								found = true;
								
								break;
							}
						}
						
						if ( !found ){
						
							s.add( sid );
								
							changed	= true;
						}
					}
				}
				
				if ( full_lookup ){
				
					map.put( "lc", new Long( SystemTime.getCurrentTime()));
					
					changed	= true;
				}
				
				if ( changed ){
				
					download.setMapAttribute( ta_subscription_info, map );
				}
			}
		}catch( Throwable e ){
			
			log( "Failed to record associations", e );
		}
		
		if ( changed ){
			
			Iterator it = listeners.iterator();
			
			while( it.hasNext()){
				
				try{
					((SubscriptionManagerListener)it.next()).associationsChanged( association_hash );
					
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
			}
		}
	}
	
	protected void
	publishAssociations()
	{
		List	 shuffled_subs;

		synchronized( this ){
			
			if ( publish_associations_active ){
				
				return;
			}			
			
			shuffled_subs = new ArrayList( subscriptions );

			publish_associations_active = true;
		}
		
		boolean	publish_initiated = false;
		
		try{
			Collections.shuffle( shuffled_subs );
						
			for (int i=0;i<shuffled_subs.size();i++){
				
				SubscriptionImpl sub = (SubscriptionImpl)shuffled_subs.get( i );
				
				if ( sub.isSubscribed()){
					
					SubscriptionImpl.association  assoc = sub.getAssociationForPublish();
					
					if ( assoc != null ){
						
						publishAssociation( sub, assoc );
						
						publish_initiated = true;
						
						break;
					}
				}
			}
		}finally{
			
			if ( !publish_initiated ){
				
				log( "Publishing Associations Complete" );
				
				synchronized( this ){

					publish_associations_active = false;
				}
			}
		}
	}
	
	protected void
	publishAssociation(
		final SubscriptionImpl					subs,
		final SubscriptionImpl.association		assoc )
	{
		log( "Checking association '" + subs.getString() + "' -> '" + assoc.getString() + "'" );
		
		byte[]	sub_id 		= subs.getShortID();
		int		sub_version	= subs.getVersion();
		
		byte[]	assoc_hash	= assoc.getHash();
		
		final String	key = "subscription:assoc:" + ByteFormatter.encodeString( assoc_hash ); 
				
		final byte[]	put_value = new byte[sub_id.length + 4];
		
		System.arraycopy( sub_id, 0, put_value, 4, sub_id.length );
		
		put_value[0]	= (byte)(sub_version>>16);
		put_value[1]	= (byte)(sub_version>>8);
		put_value[2]	= (byte)sub_version;
		put_value[3]	= (byte)subs.getFixedRandom();
		
		dht_plugin.get(
			key.getBytes(),
			"Subscription association read: " + ByteFormatter.encodeString( assoc_hash ),
			DHTPlugin.FLAG_SINGLE_VALUE,
			30,
			60*1000,
			false,
			false,
			new DHTPluginOperationListener()
			{
				private int	hits	= 0;
				private int	max_ver = 0;
				
				public void
				diversified()
				{
				}
				
				public void 
				starts(
					byte[] 				key ) 
				{
				}
				
				public void
				valueRead(
					DHTPluginContact	originator,
					DHTPluginValue		value )
				{
					byte[]	val = value.getValue();
					
					if ( val.length == put_value.length ){
						
						boolean	diff = false;
						
						for (int i=4;i<val.length;i++){
							
							if ( val[i] != put_value[i] ){
								
								diff = true;
								
								break;
							}
						}
						
						if ( !diff ){
							
							hits++;
							
							int	ver = ((val[0]<<16)&0xff0000) | ((val[1]<<8)&0xff00) | (val[2]&0xff);
							
							if ( ver > max_ver ){
								
								max_ver = ver;
							}
						}
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
					byte[]				original_key,
					boolean				timeout_occurred )
				{
					log( "Checked association '" + subs.getString() + "' -> '" + assoc.getString() + "' - max_ver=" + max_ver + ",hits=" + hits );

					if ( max_ver > subs.getVersion()){
						
						if ( !subs.isMine()){
						
							updateSubscription( subs, max_ver );
						}
					}
					
					if ( hits < 6 ){			
			
						log( "    Publishing association '" + subs.getString() + "' -> '" + assoc.getString() + "', existing=" + hits );

						byte flags = DHTPlugin.FLAG_ANON;
						
						if ( hits < 3 ){
							
							flags |= DHTPlugin.FLAG_PRECIOUS;
						}
						
						dht_plugin.put(
							key.getBytes(),
							"Subscription association write: " + ByteFormatter.encodeString( assoc.getHash()) + " -> " + ByteFormatter.encodeString( subs.getShortID() ) + ":" + subs.getVersion(),
							put_value,
							flags,
							new DHTPluginOperationListener()
							{
								public void
								diversified()
								{
								}
								
								public void 
								starts(
									byte[] 				key ) 
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
									byte[]				key,
									boolean				timeout_occurred )
								{
									log( "        completed '" + subs.getString() + "' -> '" + assoc.getString() + "'" );
				
									publishNext();
								}
							});
					}else{
						
						log( "    Not publishing association '" + subs.getString() + "' -> '" + assoc.getString() + "', existing =" + hits );

						publishNext();
					}
				}
				
				protected void
				publishNext()
				{
					synchronized( this ){
						
						publish_associations_active = false;
					}
					
					publishAssociations();
				}
			});
	}
	
	protected void
	subscriptionUpdated()
	{
		if ( dht_plugin != null ){
			
			publishSubscriptions();
		}
	}
	
	protected void
	publishSubscriptions()
	{
		List	 shuffled_subs;

		synchronized( this ){
			
			if ( publish_subscription_active ){
				
				return;
			}			
			
			shuffled_subs = new ArrayList( subscriptions );

			publish_subscription_active = true;
		}
	
		boolean	publish_initiated = false;
		
		try{
			Collections.shuffle( shuffled_subs );
						
			for (int i=0;i<shuffled_subs.size();i++){
				
				SubscriptionImpl sub = (SubscriptionImpl)shuffled_subs.get( i );
				
				if ( sub.isSubscribed() && sub.isPublic() && !sub.getPublished()){
									
					sub.setPublished( true );
					
					publishSubscription( sub );
					
					publish_initiated = true;
					
					break;
				}
			}
		}finally{
			
			if ( !publish_initiated ){
				
				log( "Publishing Subscriptions Complete" );
				
				synchronized( this ){
	
					publish_subscription_active = false;
				}
			}
		}
	}
	
	protected void
	publishSubscription(
		final SubscriptionImpl					subs )
	{
		log( "Checking subscription publication '" + subs.getString() + "'" );
		
		byte[]	sub_id 		= subs.getShortID();
		int		sub_version	= subs.getVersion();
				
		final String	key = "subscription:publish:" + ByteFormatter.encodeString( sub_id ) + ":" + sub_version; 
						
		dht_plugin.get(
			key.getBytes(),
			"Subscription presence read: " + ByteFormatter.encodeString( sub_id ) + ":" + sub_version,
			DHTPlugin.FLAG_SINGLE_VALUE,
			12,
			60*1000,
			false,
			false,
			new DHTPluginOperationListener()
			{
				private int	hits	= 0;
				
				public void
				diversified()
				{
				}
				
				public void 
				starts(
					byte[] 				key ) 
				{
				}
				
				public void
				valueRead(
					DHTPluginContact	originator,
					DHTPluginValue		value )
				{					
					byte[]	data = value.getValue();
						
					try{
						Map	details = decodeSubscriptionDetails( data );

						if ( subs.getVerifiedPublicationVersion( details ) == subs.getVersion()){
						
							hits++;
						}
					}catch( Throwable e ){
						
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
					byte[]				original_key,
					boolean				timeout_occurred )
				{
					log( "Checked subscription publication '" + subs.getString() + "' - hits=" + hits );

					if ( hits < 6 ){			
			
						log( "    Publishing subscription '" + subs.getString() + ", existing=" + hits );

						try{
							byte[]	put_value = encodeSubscriptionDetails( subs );						
							
							byte	flags = DHTPlugin.FLAG_SINGLE_VALUE;
							
							if ( hits < 3 ){
								
								flags |= DHTPlugin.FLAG_PRECIOUS;
							}
							
							dht_plugin.put(
								key.getBytes(),
								"Subscription presence write: " + ByteFormatter.encodeString( subs.getShortID() ) + ":" + subs.getVersion(),
								put_value,
								flags,
								new DHTPluginOperationListener()
								{
									public void
									diversified()
									{
									}
									
									public void 
									starts(
										byte[] 				key ) 
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
										byte[]				key,
										boolean				timeout_occurred )
									{
										log( "        completed '" + subs.getString() + "'" );
					
										publishNext();
									}
								});
							
						}catch( Throwable e ){
							
							Debug.printStackTrace( e );
							
							publishNext();
						}
						
					}else{
						
						log( "    Not publishing subscription '" + subs.getString() + "', existing =" + hits );

						publishNext();
					}
				}
				
				protected void
				publishNext()
				{
					synchronized( this ){
						
						publish_subscription_active = false;
					}
					
					publishSubscriptions();
				}
			});
	}
	
	protected void
	updateSubscription(
		final SubscriptionImpl		subs,
		final int					new_version )
	{
		log( "Subscription " + subs.getString() + " - higher version found: " + new_version );
		
		if ( !subs.canAutoUpgradeCheck()){
			
			log( "    Checked too recently, ignoring" );
			
			return;
		}
		
		if ( subs.getHighestUserPromptedVersion() >= new_version ){
			
			log( "    User has already been prompted for version " + new_version + " so ignoring" );
			
			return;
		}
		
		log( "Checking subscription '" + subs.getString() + "' upgrade to version " + new_version );
			
		byte[]	sub_id 		= subs.getShortID();
				
		final String	key = "subscription:publish:" + ByteFormatter.encodeString( sub_id ) + ":" + new_version; 
						
		dht_plugin.get(
			key.getBytes(),
			"Subscription update read: " + ByteFormatter.encodeString( sub_id ) + ":" + new_version,
			DHTPlugin.FLAG_SINGLE_VALUE,
			12,
			60*1000,
			false,
			false,
			new DHTPluginOperationListener()
			{
				private byte[]	verified_hash;
				private int		verified_size;
				
				public void
				diversified()
				{
				}
				
				public void 
				starts(
					byte[] 				key ) 
				{
				}
				
				public void
				valueRead(
					DHTPluginContact	originator,
					DHTPluginValue		value )
				{
					byte[]	data = value.getValue();
							
					try{
						Map	details = decodeSubscriptionDetails( data );
						
						if ( 	verified_hash == null && 
								subs.getVerifiedPublicationVersion( details ) == new_version ){
							
							verified_hash 	= SubscriptionImpl.getPublicationHash( details );
							verified_size	= SubscriptionImpl.getPublicationSize( details );
						}
						
					}catch( Throwable e ){
						
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
					byte[]				original_key,
					boolean				timeout_occurred )
				{
					if ( verified_hash != null ){			
			
						log( "    Subscription '" + subs.getString() + " upgrade verified as authentic" );

						updateSubscription( subs, new_version, verified_hash, verified_size );
						
					}else{
						
						log( "    Subscription '" + subs.getString() + " upgrade not verified" );
					}
				}
			});
	}
	
	protected byte[]
	encodeSubscriptionDetails(
		SubscriptionImpl		subs )
	
		throws IOException
	{
		Map		details = subs.getPublicationDetails();					
		
		byte[] encoded = BEncoder.encode( details );
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
				
		GZIPOutputStream os = new GZIPOutputStream( baos );

		os.write( encoded );
		
		os.close();
		
		byte[] compressed = baos.toByteArray();
		
		byte	header;
		byte[]	data;
		
		if ( compressed.length < encoded.length ){
			
			header 	= 1;
			data	= compressed; 
		}else{
			
			header	= 0;
			data	= encoded;
		}
				
		byte[] result = new byte[data.length+1];
		
		result[0] = header;
		
		System.arraycopy( data, 0, result, 1, data.length );
		
		return( result );
	}
	
	protected Map
	decodeSubscriptionDetails(
		byte[]			data )
	
		throws IOException
	{
		byte[]	to_decode;
		
		if ( data[0] == 0 ){
			
			to_decode = new byte[ data.length-1 ];
			
			System.arraycopy( data, 1, to_decode, 0, data.length - 1 );
			
		}else{
			
			GZIPInputStream is = new GZIPInputStream(new ByteArrayInputStream( data, 1, data.length - 1 ));
			
			to_decode = FileUtil.readInputStreamAsByteArray( is );
			
			is.close();
		}
		
		return( BDecoder.decode( to_decode ));
	}
	
	protected void
	updateSubscription(
		final SubscriptionImpl			subs,
		final int						update_version,
		final byte[]					update_hash,
		final int						update_size )
	{
		log( "Subscription " + subs.getString() + " - update hash=" + ByteFormatter.encodeString( update_hash ) + ", size=" + update_size );

		new AEThread2( "SubsUpdate", true )
		{
			public void
			run()
			{
				try{
					Object[] res = downloadTorrent( update_hash, update_size );
					
					if ( res != null ){
					
						updateSubscription( subs, update_version, (TOTorrent)res[0], (InetSocketAddress)res[1] );
					}
				}catch( Throwable e ){
					
					log( "    update failed", e );
				}
			}
		}.start();
	}
	
	protected Object[]
	downloadTorrent(
		byte[]		hash,
		int			update_size )
	{		
		final MagnetPlugin	magnet_plugin = getMagnetPlugin();
	
		if ( magnet_plugin == null ){
		
			log( "    Can't download, no magnet plugin" );
		
			return( null );
		}

		try{
			final InetSocketAddress[] sender = { null };
			
			byte[] torrent_data = magnet_plugin.download(
				new MagnetPluginProgressListener()
				{
					public void
					reportSize(
						long	size )
					{
					}
					
					public void
					reportActivity(
						String	str )
					{
						log( "    MagnetDownload: " + str );
					}
					
					public void
					reportCompleteness(
						int		percent )
					{
					}
					
					public void
					reportContributor(
						InetSocketAddress	address )
					{
						synchronized( sender ){
						
							sender[0] = address;
						}
					}
				},
				hash,
				new InetSocketAddress[0],
				300*1000 );
			
			if ( torrent_data == null ){
				
				log( "    download failed - timeout" );
				
				return( null );
			}
			
			log( "Subscription torrent downloaded" );
			
			TOTorrent torrent = TOTorrentFactory.deserialiseFromBEncodedByteArray( torrent_data );
		
				// update size is just that of signed content, torrent itself is .vuze file
				// so take this into account
			
			if ( torrent.getSize() > update_size + 10*1024 ){
			
				log( "Subscription download abandoned, torrent size is " + torrent.getSize() + ", underlying data size is " + update_size );
				
				return( null );
			}
			
			if ( torrent.getSize() > 4*1024*1024 ){
				
				log( "Subscription download abandoned, torrent size is too large (" + torrent.getSize() + ")" );
				
				return( null );
			}
			
			synchronized( sender ){
			
				return( new Object[]{ torrent, sender[0] });
			}
			
		}catch( Throwable e ){
			
			log( "    download failed", e );
			
			return( null );
		}
	}
	
	protected void
	downloadSubscription(
		final TOTorrent			torrent,
		final InetSocketAddress	peer,
		byte[]					subs_id,
		int						version,
		String					name,
		final downloadListener	listener )
	{
		try{
				// testing purposes, see if local exists
			
			LightWeightSeed lws = LightWeightSeedManager.getSingleton().get( new HashWrapper( torrent.getHash()));
	
			if ( lws != null ){
				
				log( "Light weight seed found" );
				
				listener.complete( lws.getDataLocation());
				
			}else{
				String	sid = ByteFormatter.encodeString( subs_id );
				
				File	dir = getSubsDir();
				
				dir = new File( dir, "temp" );
				
				if ( !dir.exists()){
					
					if ( !dir.mkdirs()){
						
						throw( new IOException( "Failed to create dir '" + dir + "'" ));
					}
				}
				
				final File	torrent_file 	= new File( dir, sid + "_" + version + ".torrent" );
				final File	data_file 		= new File( dir, sid + "_" + version + ".vuze" );
	
				PluginInterface pi = StaticUtilities.getDefaultPluginInterface();
			
				final DownloadManager dm = pi.getDownloadManager();
				
				Download download = dm.getDownload( torrent.getHash());
				
				if ( download == null ){
					
					log( "Adding download for subscription '" + new String(torrent.getName()) + "'" );
					
					PlatformTorrentUtils.setContentTitle(torrent, "Update for subscription '" + name + "'" );
					
						// PlatformTorrentUtils.setContentThumbnail(torrent, thumbnail);
						
					TorrentUtils.setFlag( torrent, TorrentUtils.TORRENT_FLAG_LOW_NOISE, true );
					
					Torrent t = new TorrentImpl( torrent );
					
					t.setDefaultEncoding();
					
					t.writeToFile( torrent_file );
					
					download = dm.addDownload( t, torrent_file, data_file );
					
					download.setBooleanAttribute( ta_subs_download, true );
					
					Map rd = listener.getRecoveryData();
					
					if ( rd != null ){
						
						download.setMapAttribute( ta_subs_download_rd, rd );
					}
				}else{
					
					log( "Existing download found for subscription '" + new String(torrent.getName()) + "'" );
				}
				
				final Download f_download = download;
				
				final TimerEventPeriodic[] event = { null };
				
				event[0] = 
					SimpleTimer.addPeriodicEvent(
						"SM:cancelTimer",
						10*1000,
						new TimerEventPerformer()
						{
							public void 
							perform(
								TimerEvent ev ) 
							{
								boolean	kill = false;
								
								try{						
									if ( 	listener.isCancelled() ||
											dm.getDownload( torrent.getHash()) == null ){
										
										kill = true;
									}
								}catch( Throwable e ){
									
									kill = true;
								}
								
								if ( kill && event[0] != null ){
									
									event[0].cancel();
									
									if ( listener.isCancelled()){
										
										try{
											f_download.stop();
											
										}catch( Throwable e ){
										}
										
										try{
											f_download.remove( true, true );
											
										}catch( Throwable e ){
										}
										
										torrent_file.delete();
									}
								}
							}
						});
				
				download.addCompletionListener(
					new DownloadCompletionListener()
					{
						public void 
						onCompletion(
							Download d ) 
						{
							listener.complete( d, torrent_file );
						}
					});
				
				if ( download.isComplete()){
					
					listener.complete( download, torrent_file  );
					
				}else{
								
					download.setForceStart( true );
					
					if ( peer != null ){
					
						download.addPeerListener(
							new DownloadPeerListener()
							{
								public void
								peerManagerAdded(
									Download		download,
									PeerManager		peer_manager )
								{									
									InetSocketAddress tcp = AddressUtils.adjustTCPAddress( peer, true );
									InetSocketAddress udp = AddressUtils.adjustUDPAddress( peer, true );
									
									log( "    Injecting peer into download: " + tcp );

									peer_manager.addPeer( tcp.getAddress().getHostAddress(), tcp.getPort(), udp.getPort(), true );
								}
								
								public void
								peerManagerRemoved(
									Download		download,
									PeerManager		peer_manager )
								{							
								}
							});
					}
				}
			}
		}catch( Throwable e ){
			
			log( "Failed to add download", e );
			
			listener.failed( e );
		}
	}
	
	protected interface
	downloadListener
	{
		public void
		complete(
			File		data_file );
		
		public void
		complete(
			Download	download,	
			File		torrent_file );
			
		public void
		failed(
			Throwable	error );
		
		public Map
		getRecoveryData();
		
		public boolean
		isCancelled();
	}
	
	protected void
	updateSubscription(
		final SubscriptionImpl		subs,
		final int					new_version,
		TOTorrent					torrent,
		InetSocketAddress			peer )
	{
		log( "Subscription " + subs.getString() + " - update torrent: " + new String( torrent.getName()));

		subs.setHighestUserPromptedVersion( new_version );
		
		UIManager ui_manager = StaticUtilities.getUIManager( 120*1000 );
		
		String details = MessageText.getString(
				"subscript.add.upgradeto.desc",
				new String[]{ String.valueOf(new_version), subs.getName()});
		
		long res = ui_manager.showMessageBox(
				"subscript.add.upgrade.title",
				"!" + details + "!",
				UIManagerEvent.MT_YES | UIManagerEvent.MT_NO );
		
		if ( res != UIManagerEvent.MT_YES ){	
		
			log( "    User declined upgrade" );
			
			return;
		}
			
		downloadSubscription(
			torrent,
			peer,
			subs.getShortID(),
			new_version,
			subs.getName(),
			new downloadListener()
			{
				public void
				complete(
					File		data_file )
				{
					updateSubscription( subs, data_file );
				}
				
				public void
				complete(
					Download	download,	
					File		torrent_file )
				{
					updateSubscription( subs, download, torrent_file, new File( download.getSavePath()));
				}
					
				public void
				failed(
					Throwable	error )
				{
					log( "Failed to download subscription", error );
				}
				
				public Map
				getRecoveryData()
				{
					Map	rd = new HashMap();
					
					rd.put( "sid", subs.getShortID());
					rd.put( "ver", new Long( new_version ));
					
					return( rd );
				}
				
				public boolean
				isCancelled()
				{
					return( false );
				}
			});
	}

	protected boolean
	recoverSubscriptionUpdate(
		Download				download,
		final Map				rd )
	{
		byte[]	sid 	= (byte[])rd.get( "sid" );
		int		version = ((Long)rd.get( "ver" )).intValue();
		
		final SubscriptionImpl subs = getSubscriptionFromSID( sid );
		
		if ( subs == null ){
		
			log( "Can't recover '" + download.getName() + "' - subscription " + ByteFormatter.encodeString( sid ) +  " not found" );
			
			return( false );
		}
		
		downloadSubscription(
				((TorrentImpl)download.getTorrent()).getTorrent(),
				null,
				subs.getShortID(),
				version,
				subs.getName(),
				new downloadListener()
				{
					public void
					complete(
						File		data_file )
					{
						updateSubscription( subs, data_file );
					}
					
					public void
					complete(
						Download	download,	
						File		torrent_file )
					{
						updateSubscription( subs, download, torrent_file, new File( download.getSavePath()));
					}
						
					public void
					failed(
						Throwable	error )
					{
						log( "Failed to download subscription", error );
					}
					
					public Map
					getRecoveryData()
					{
						return( rd );
					}
					
					public boolean
					isCancelled()
					{
						return( false );
					}
				});
		
		return( true );
	}
	
	protected void
	updateSubscription(
		SubscriptionImpl		subs,
		Download				download,
		File					torrent_file,
		File					data_file )
	{
		try{
			try{
				download.stop();
				
			}catch( Throwable e ){
			}
			
			download.remove( true, false );
		
			try{				
				updateSubscription( subs, data_file );
											
			}finally{
				
				if ( !data_file.delete()){
					
					log( "Failed to delete update file '" + data_file + "'" );
				}
				
				if ( !torrent_file.delete()){
					
					log( "Failed to delete update torrent '" + torrent_file + "'" );
				}
			}
		}catch( Throwable e ){
			
			log( "Failed to remove update download", e );
		}
	}
	
	protected void
	updateSubscription(
		SubscriptionImpl		subs,
		File					data_location )
	{
		log( "Updating subscription '" + subs.getString() + " using '" + data_location + "'" );
		
		VuzeFileHandler vfh = VuzeFileHandler.getSingleton();
			
		VuzeFile vf = vfh.loadVuzeFile( data_location.getAbsolutePath());
						
		vfh.handleFiles( new VuzeFile[]{ vf }, VuzeFileComponent.COMP_TYPE_SUBSCRIPTION );
	}
	
	protected MagnetPlugin
	getMagnetPlugin()
	{
		PluginInterface  pi  = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByClass( MagnetPlugin.class );
	
		if ( pi == null ){
			
			return( null );
		}
		
		return((MagnetPlugin)pi.getPlugin());
	}
	
	protected void
	loadConfig()
	{
		if ( !FileUtil.resilientConfigFileExists( CONFIG_FILE )){
			
			return;
		}
		
		log( "Loading configuration" );
		
		synchronized( this ){
			
			Map map = FileUtil.readResilientConfigFile( CONFIG_FILE );
			
			List	l_subs = (List)map.get( "subs" );
			
			if ( l_subs != null ){
				
				for (int i=0;i<l_subs.size();i++){
					
					Map	m = (Map)l_subs.get(i);
					
					try{
						SubscriptionImpl sub = new SubscriptionImpl( this, m );
						
						subscriptions.add( sub );
						
						log( "    loaded " + sub.getString());
						
					}catch( Throwable e ){
						
						log( "Failed to import subscription from " + m, e );
					}
				}
			}
		}
	}
	
	protected void
	configDirty(
		SubscriptionImpl		subs )
	{
		changeSubscription( subs );
		
		configDirty();
	}
	
	protected void
	configDirty()
	{
		synchronized( this ){
			
			if ( config_dirty ){
				
				return;
			}
			
			config_dirty = true;
		
			new DelayedEvent( 
				"Subscriptions:save", 5000,
				new AERunnable()
				{
					public void 
					runSupport() 
					{
						synchronized( this ){
							
							if ( !config_dirty ){

								return;
							}
							
							saveConfig();
						}	
					}
				});
		}
	}
	
	protected void
	saveConfig()
	{
		log( "Saving configuration" );
		
		synchronized( this ){
			
			config_dirty = false;
			
			if ( subscriptions.size() == 0 ){
				
				FileUtil.deleteResilientConfigFile( CONFIG_FILE );
				
			}else{
				
				Map map = new HashMap();
				
				List	l_subs = new ArrayList();
				
				map.put( "subs", l_subs );
				
				Iterator	it = subscriptions.iterator();
				
				while( it.hasNext()){
					
					SubscriptionImpl sub = (SubscriptionImpl)it.next();
						
					try{
						l_subs.add( sub.toMap());
						
					}catch( Throwable e ){
						
						log( "Failed to save subscription " + sub.getString(), e );
					}
				}
				
				FileUtil.writeResilientConfigFile( CONFIG_FILE, map );
			}
		}
	}
	
	protected synchronized AEDiagnosticsLogger
	getLogger()
	{
		if ( logger == null ){
			
			logger = AEDiagnostics.getLogger( LOGGER_NAME );
		}
		
		return( logger );
	}
	
	public void 
	log(
		String 		s,
		Throwable 	e )
	{
		AEDiagnosticsLogger diag_logger = getLogger();
		
		diag_logger.log( s );
		diag_logger.log( e );
	}
	
	public void 
	log(
		String 	s )
	{
		AEDiagnosticsLogger diag_logger = getLogger();
		
		diag_logger.log( s );
	}
	
	public void
	addListener(
		SubscriptionManagerListener	listener )
	{
		listeners.add( listener );
	}
	
	public void
	removeListener(
		SubscriptionManagerListener	listener )
	{
		listeners.remove( listener );
	}
}
