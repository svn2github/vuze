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

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.util.AEDiagnostics;
import org.gudy.azureus2.core3.util.AEDiagnosticsLogger;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DelayedEvent;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.core3.util.TimerEventPeriodic;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadCompletionListener;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerEvent;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemFillListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.tables.TableRow;
import org.gudy.azureus2.plugins.utils.StaticUtilities;
import org.gudy.azureus2.pluginsimpl.local.torrent.TorrentImpl;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreLifecycleAdapter;
import com.aelitis.azureus.core.lws.LightWeightSeed;
import com.aelitis.azureus.core.lws.LightWeightSeedManager;
import com.aelitis.azureus.core.messenger.config.PlatformSubscriptionsMessenger;
import com.aelitis.azureus.core.subs.Subscription;
import com.aelitis.azureus.core.subs.SubscriptionException;
import com.aelitis.azureus.core.subs.SubscriptionLookupListener;
import com.aelitis.azureus.core.subs.SubscriptionManager;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.core.vuzefile.VuzeFile;
import com.aelitis.azureus.core.vuzefile.VuzeFileComponent;
import com.aelitis.azureus.core.vuzefile.VuzeFileHandler;
import com.aelitis.azureus.core.vuzefile.VuzeFileProcessor;
import com.aelitis.azureus.plugins.dht.DHTPlugin;
import com.aelitis.azureus.plugins.dht.DHTPluginContact;
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
	
	private TimerEventPeriodic	timer;
	
	private volatile DHTPlugin	dht_plugin;
	
	private List		subscriptions	= new ArrayList();
	
	private boolean	config_dirty;
	
	private boolean	publish_associations_active;
	private boolean publish_subscription_active;
	
	private AEDiagnosticsLogger		logger;
	
	
	protected
	SubscriptionManagerImpl()
	{
		loadConfig();

		/*
		if ( subscriptions.size() == 1 ){
			
			try{
				create( "test subscription 2" );
				
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
	
			for (int i=0;i<subscriptions.size();i++){
			
				((Subscription)subscriptions.get(i)).addAssociation( ByteFormatter.decodeString( "E02E5E117A5A9080D552A11FA675DE868A05FE71" ));
			}
		}
		*/
		
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
		
		if ( Constants.isCVSVersion()){
			
			final PluginInterface default_pi = StaticUtilities.getDefaultPluginInterface();
			
				// check assoc
			
			{
				final TableContextMenuItem menu_item_itorrents = 
					default_pi.getUIManager().getTableManager().addContextMenuItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE, "azsubs.contextmenu.lookupassoc");
				final TableContextMenuItem menu_item_ctorrents 	= 
					default_pi.getUIManager().getTableManager().addContextMenuItem(TableManager.TABLE_MYTORRENTS_COMPLETE, "azsubs.contextmenu.lookupassoc");
				
				menu_item_itorrents.setStyle(TableContextMenuItem.STYLE_PUSH);
				menu_item_ctorrents.setStyle(TableContextMenuItem.STYLE_PUSH);
		
				MenuItemListener listener = 
					new MenuItemListener()
					{
						public void 
						selected(
							MenuItem 	menu, 
							Object 		target) 
						{
							TableRow[]	rows = (TableRow[])target;
							
							for (int i=0;i<rows.length;i++){
								
								Download download = (Download)rows[i].getDataSource();
								
								Torrent t = download.getTorrent();
								
								if ( t != null ){
									
									try{
										lookupAssociations( 
											t.getHash(),
											new SubscriptionLookupListener()
											{
												public void
												found(
													byte[]					hash,
													Subscription			subscription )
												{
													log( "    lookup: found " + ByteFormatter.encodeString( hash ) + " -> " + subscription.getName());
												}
												
												public void
												complete(
													byte[]					hash,
													Subscription[]			subscriptions )
												{
													log( "    lookup: complete " + ByteFormatter.encodeString( hash ) + " -> " +subscriptions.length );
		
												}
												
												public void
												failed(
													byte[]					hash,
													SubscriptionException	error )
												{
													log( "    lookup: failed", error );
												}
											});
										
									}catch( Throwable e ){
										
										log( "Lookup failed", e );
									}
								}	
							}
						}
					};
				
				menu_item_itorrents.addMultiListener( listener );
				menu_item_ctorrents.addMultiListener( listener );	
			}
			
				// make assoc
			
			{
				final TableContextMenuItem menu_item_itorrents = 
					default_pi.getUIManager().getTableManager().addContextMenuItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE, "azsubs.contextmenu.addassoc");
				final TableContextMenuItem menu_item_ctorrents 	= 
					default_pi.getUIManager().getTableManager().addContextMenuItem(TableManager.TABLE_MYTORRENTS_COMPLETE, "azsubs.contextmenu.addassoc");
				
				menu_item_itorrents.setStyle(TableContextMenuItem.STYLE_MENU);
				menu_item_ctorrents.setStyle(TableContextMenuItem.STYLE_MENU);
				
				MenuItemFillListener	menu_fill_listener = 
					new MenuItemFillListener()
					{
						public void
						menuWillBeShown(
							MenuItem	menu,
							Object		target )
						{	
							TableRow[]	rows;
							
							if ( target instanceof TableRow[] ){
								
								rows = (TableRow[])target;
								
							}else{
								
								rows = new TableRow[]{ (TableRow)target };
							}
							
							final List	hashes = new ArrayList();
							
							for (int i=0;i<rows.length;i++){
								
								Download	download = (Download)rows[i].getDataSource();
							
								Torrent torrent = download.getTorrent();
								
								if ( torrent != null ){
									
									hashes.add( torrent.getHash());
								}
							}
														
							menu.removeAllChildItems();
							
							boolean enabled = hashes.size() > 0;
							
							if ( enabled ){
							
								Subscription[] subs = getSubscriptions();
								
								boolean	incomplete = ((TableContextMenuItem)menu).getTableID() == TableManager.TABLE_MYTORRENTS_INCOMPLETE;
								
								TableContextMenuItem parent = incomplete?menu_item_itorrents:menu_item_ctorrents;
																
								for (int i=0;i<subs.length;i++){
									
									final Subscription	sub = subs[i];
									
									TableContextMenuItem item =
										default_pi.getUIManager().getTableManager().addContextMenuItem(
											parent,
											"!" + sub.getName() + "!");
									
									item.addListener(
										new MenuItemListener()
										{
											public void 
											selected(
												MenuItem 	menu,
												Object 		target ) 
											{
												for (int i=0;i<hashes.size();i++){
													
													sub.addAssociation( (byte[])hashes.get(i));
												}
											}
										});
								}
							}
							
							menu.setEnabled( enabled );
						}
					};
					
				menu_item_itorrents.addFillListener( menu_fill_listener );
				menu_item_ctorrents.addFillListener( menu_fill_listener );		
					
			}
		}
			
		PluginInterface  dht_plugin_pi  = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByClass( DHTPlugin.class );

		if ( dht_plugin_pi != null ){
			
			dht_plugin = (DHTPlugin)dht_plugin_pi.getPlugin();
			
			if ( subscriptions.size() > 0 ){
				
				publishAssociations();
			
				publishSubscriptions();
				
				checkTimer();
			}
		}
	}
	
	public Subscription 
	create(
		String			name )
	
		throws SubscriptionException 
	{
		SubscriptionImpl result = new SubscriptionImpl( this, name );
		
		log( "Created new subscription: " + result.getString());
		
		synchronized( this ){
			
			subscriptions.add( result );
			
			saveConfig();
		}
		
		subscriptionAdded();
		
		checkTimer();
		
		return( result );
	}
	
	protected void
	checkTimer()
	{
		synchronized( this ){

			if ( timer == null ){
				
				timer = SimpleTimer.addPeriodicEvent(
						"SubscriptionChecker",
						30*1000,
						new TimerEventPerformer()
						{
							public void 
							perform(
								TimerEvent event )
							{
								checkStuff();
							}
						});
			}
		}
	}
	
	protected void
	checkStuff()
	{
		List subs;
		
		synchronized( this ){
			
			subs = new ArrayList( subscriptions );
		}
		
		for (int i=0;i<subs.size();i++){
			
			((SubscriptionImpl)subs.get(i)).checkPublish();
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
				
				SubscriptionImpl new_subs = new SubscriptionImpl( this, body );
	
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
				
				synchronized( this ){
					
					subscriptions.add( new_subs );
					
					saveConfig();
				}
				
				subscriptionAdded();
				
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
	
	public void
	lookupAssociations(
		final byte[] 							hash,
		final SubscriptionLookupListener		listener )
	
		throws SubscriptionException 
	{
		log( "Looking up associations for '" + ByteFormatter.encodeString( hash ));
		
		final String	key = "subscription:assoc:" + ByteFormatter.encodeString( hash ); 
						
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
				private Map	hits = new HashMap();
				
				private List	subscriptions = new ArrayList();
				
				private boolean	complete;
				
				public void
				diversified()
				{
				}
				
				public void
				valueRead(
					DHTPluginContact	originator,
					DHTPluginValue		value )
				{
					byte[]	val = value.getValue();
					
					if ( val.length > 4 ){
						
						int	ver = ((val[0]<<16)&0xff0000) | ((val[1]<<8)&0xff00) | (val[2]&0xff);

						byte[]	sid = new byte[ val.length - 4 ];
						
						System.arraycopy( val, 4, sid, 0, sid.length );
						
						log( "    Found subscription " + ByteFormatter.encodeString( sid ) + " version " + ver );
						
						HashWrapper hw = new HashWrapper( sid );
						
						boolean	new_sid = false;
						
						synchronized( hits ){
							
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
							
							Subscription subs = getSubscriptionFromSID( sid );
							
							if ( subs != null ){
								
								synchronized( subscriptions ){

									subscriptions.add( subs );
								}
								
								listener.found( hash, subs );
								
							}else{
								
								lookupSubscription( hash, sid, listener );
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
					log( "    Association lookup complete - " + hits.size() + " found" );
					
					Subscription[] s;
					
					synchronized( subscriptions ){
						
						if ( complete ){
							
							return;
						}
						
						complete = true;
						
						s = (Subscription[])subscriptions.toArray( new Subscription[ subscriptions.size() ]);
					}
					
					listener.complete( hash, s );
				}
			});
	}
	
	protected void
	lookupSubscription(
		byte[]						association_hash,
		byte[]						sid,
		SubscriptionLookupListener	listener )
	{
		try{
			PlatformSubscriptionsMessenger.getSubscriptionBySID( sid );
			
		}catch( Throwable e ){
			
			log( "Subscription lookup via platform failed", e );
		}
	}
	
	protected void
	associationAdded()
	{
		if ( dht_plugin != null ){
			
			publishAssociations();
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
				
				SubscriptionImpl.association  assoc = sub.getAssociationForPublish();
				
				if ( assoc != null ){
					
					publishAssociation( sub, assoc );
					
					publish_initiated = true;
					
					break;
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

					if ( max_ver >= subs.getVersion()){
						
						if ( !subs.isMine()){
						
							updateSubscription( subs, max_ver );
						}
					}
					
					if ( hits < 6 ){			
			
						log( "    Publishing association '" + subs.getString() + "' -> '" + assoc.getString() + "', existing=" + hits );

						dht_plugin.put(
							key.getBytes(),
							"Subscription association write: " + ByteFormatter.encodeString( assoc.getHash()) + " -> " + ByteFormatter.encodeString( subs.getShortID() ) + ":" + subs.getVersion(),
							put_value,
							DHTPlugin.FLAG_ANON,
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
	subscriptionAdded()
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
				
				if ( sub.isPublic() && !sub.getPublished()){
									
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
							
							dht_plugin.put(
								key.getBytes(),
								"Subscription presence write: " + ByteFormatter.encodeString( subs.getShortID() ) + ":" + subs.getVersion(),
								put_value,
								DHTPlugin.FLAG_SINGLE_VALUE,
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
			
			//return;
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
				valueRead(
					DHTPluginContact	originator,
					DHTPluginValue		value )
				{
					byte[]	data = value.getValue();
							
					try{
						Map	details = decodeSubscriptionDetails( data );
						
						if ( 	verified_hash == null && 
								subs.getVerifiedPublicationVersion( details ) == new_version ){
							
							verified_hash 	= subs.getPublicationHash( details );
							verified_size	= subs.getPublicationSize( details );
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

		final MagnetPlugin	magnet_plugin = getMagnetPlugin();
		
		if ( magnet_plugin == null ){
			
			log( "    Can't update, no magnet plugin" );
			
			return;
		}
		
		new AEThread2( "SubsUpdate", true )
		{
			public void
			run()
			{
				try{
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
						},
						update_hash,
						new InetSocketAddress[0],
						300*1000 );
					
					log( "Subscription torrent downloaded" );
					
					TOTorrent torrent = TOTorrentFactory.deserialiseFromBEncodedByteArray( torrent_data );
				
						// update size is just that of signed content, torrent itself is .vuze file
						// so take this into account
					
					if ( torrent.getSize() > update_size + 10*1024 ){
					
						log( "Subscription update abandoned, torrent size is " + torrent.getSize() + ", underlying data size is " + update_size );
					}
					
					if ( torrent.getSize() > 4*1024*1024 ){
						
						log( "Subscription update abandoned, torrent size is too large (" + torrent.getSize() + ")" );
					}
					
					updateSubscription( subs, update_version, torrent );
					
				}catch( Throwable e ){
					
					log( "    update failed", e );
				}
			}
		}.start();
	}
	
	protected void
	updateSubscription(
		final SubscriptionImpl		subs,
		int							new_version,
		TOTorrent					torrent )
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
				
		try{
				// testing purposes, see if local exists
			
			LightWeightSeed lws = LightWeightSeedManager.getSingleton().get( new HashWrapper( torrent.getHash()));

			if ( lws != null ){
				
				log( "Light weight seed found" );
				
				updateSubscription( subs, lws.getDataLocation());
				
			}else{
				String	sid = ByteFormatter.encodeString( subs.getShortID());
				
				File	dir = getSubsDir();
				
				dir = new File( dir, "temp" );
				
				if ( !dir.exists()){
					
					if ( !dir.mkdirs()){
						
						throw( new IOException( "Failed to create dir '" + dir + "'" ));
					}
				}
				
				final File	torrent_file 	= new File( dir, sid + "_" + new_version + ".torrent" );
				final File	data_file 		= new File( dir, sid + "_" + new_version + ".vuze" );
	
				PluginInterface pi = AzureusCoreFactory.getSingleton().getPluginManager().getDefaultPluginInterface();
			
				DownloadManager dm = pi.getDownloadManager();
				
				Download download = dm.getDownload( torrent.getHash());
				
				if ( download == null ){
					
					PlatformTorrentUtils.setContentTitle(torrent, "Update for subscription '" + subs.getName() + "'" );
					
						// TODO PlatformTorrentUtils.setContentThumbnail(torrent, thumbnail);
						
					TorrentUtils.setFlag( torrent, TorrentUtils.TORRENT_FLAG_LOW_NOISE, true );
					
					Torrent t = new TorrentImpl( torrent );
					
					t.setDefaultEncoding();
					
					t.writeToFile( torrent_file );
					
					download = dm.addDownload( t, torrent_file, data_file );
				}
				
				download.addCompletionListener(
					new DownloadCompletionListener()
					{
						public void 
						onCompletion(
							Download d ) 
						{
							updateSubscription( subs, d, torrent_file, data_file );
						}
					});
				
				if ( download.isComplete()){
					
					updateSubscription( subs, download, torrent_file, data_file  );
					
				}else{
								
					download.setForceStart( true );
				}
			}
		}catch( Throwable e ){
			
			log( "Failed to add download", e );
		}
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
}
