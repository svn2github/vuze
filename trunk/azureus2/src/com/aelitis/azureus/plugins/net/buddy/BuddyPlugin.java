/*
 * Created on Mar 19, 2008
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


package com.aelitis.azureus.plugins.net.buddy;

import java.io.File;
import java.net.InetAddress;
import java.security.SecureRandom;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.util.SHA1Simple;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.ddb.DistributedDatabase;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseContact;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseEvent;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseKey;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseListener;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseValue;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.messaging.MessageException;
import org.gudy.azureus2.plugins.messaging.MessageManager;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageConnection;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageHandler;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageRegistration;
import org.gudy.azureus2.plugins.network.RateLimiter;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.config.IntParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.config.StringParameter;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemFillListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.tables.TableRow;
import org.gudy.azureus2.plugins.utils.*;
import org.gudy.azureus2.plugins.utils.security.SEPublicKey;
import org.gudy.azureus2.plugins.utils.security.SEPublicKeyLocator;
import org.gudy.azureus2.plugins.utils.security.SESecurityManager;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.security.CryptoHandler;
import com.aelitis.azureus.core.security.CryptoManagerFactory;
import com.aelitis.azureus.core.security.CryptoManagerKeyListener;
import com.aelitis.azureus.core.security.CryptoManagerPasswordException;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.core.util.bloom.BloomFilter;
import com.aelitis.azureus.core.util.bloom.BloomFilterFactory;
import com.aelitis.azureus.plugins.net.buddy.swt.BuddyPluginView;
import com.aelitis.azureus.plugins.net.buddy.tracker.BuddyPluginTracker;

public class 
BuddyPlugin 
	implements Plugin
{
	public static final int MAX_MESSAGE_SIZE	= 4*1024*1024;
	
	public static final int	SUBSYSTEM_INTERNAL	= 0;
	public static final int	SUBSYSTEM_AZ2		= 1;
	public static final int	SUBSYSTEM_AZ3		= 2;
	
	protected static final int RT_INTERNAL_REQUEST_PING		= 1;
	protected static final int RT_INTERNAL_REPLY_PING		= 2;
	protected static final int RT_INTERNAL_REQUEST_CLOSE	= 3;
	protected static final int RT_INTERNAL_REPLY_CLOSE		= 4;
	protected static final int RT_INTERNAL_FRAGMENT			= 5;
	
	protected static final boolean TRACE = false; 

	private static final String VIEW_ID = "azbuddy";

	private static final int	INIT_UNKNOWN		= 0;
	private static final int	INIT_OK				= 1;
	private static final int	INIT_BAD			= 2;
	
	private static final int	MAX_UNAUTH_BUDDIES	= 16;
	
	public static final int	TIMER_PERIOD	= 10*1000;
	
	private static final int	BUDDY_STATUS_CHECK_PERIOD_MIN	= 3*60*1000;
	private static final int	BUDDY_STATUS_CHECK_PERIOD_INC	= 1*60*1000;
	
	protected static final int	STATUS_REPUBLISH_PERIOD		= 10*60*1000;
	private static final int	STATUS_REPUBLISH_TICKS		= STATUS_REPUBLISH_PERIOD/TIMER_PERIOD;

	private static final int	CHECK_YGM_PERIOD			= 5*60*1000;
	private static final int	CHECK_YGM_TICKS				= CHECK_YGM_PERIOD/TIMER_PERIOD;
	private static final int	YGM_BLOOM_LIFE_PERIOD		= 60*60*1000;
	private static final int	YGM_BLOOM_LIFE_TICKS		= YGM_BLOOM_LIFE_PERIOD/TIMER_PERIOD;

	private static final int	SAVE_CONFIG_PERIOD			= 60*1000;
	private static final int	SAVE_CONFIG_TICKS			= SAVE_CONFIG_PERIOD/TIMER_PERIOD;

	public static final int		PERSISTENT_MSG_RETRY_PERIOD		= 5*60*1000;
	private static final int	PERSISTENT_MSG_CHECK_PERIOD		= 60*1000;
	private static final int	PERSISTENT_MSG_CHECK_TICKS		= PERSISTENT_MSG_CHECK_PERIOD/TIMER_PERIOD;

	private static final int	UNAUTH_BLOOM_RECREATE		= 120*1000;
	private static final int	UNAUTH_BLOOM_CHUNK			= 1000;
	private static BloomFilter	unauth_bloom;
	private static long			unauth_bloom_create_time;

	private static final int	BLOOM_CHECK_PERIOD			= UNAUTH_BLOOM_RECREATE/2;
	private static final int	BLOOM_CHECK_TICKS			= BLOOM_CHECK_PERIOD/TIMER_PERIOD;

	private static BloomFilter	ygm_unauth_bloom;

	
	private volatile int	 initialisation_state = INIT_UNKNOWN;
	
	private PluginInterface	plugin_interface;
	
	private LoggerChannel	logger;
	
	private BooleanParameter 	enabled_param; 
	private StringParameter 	nick_name_param;
	
	private boolean			ready_to_publish;
	private publishDetails	current_publish		= new publishDetails();
	private publishDetails	latest_publish		= current_publish;
	private long			last_publish_start;
	
	
	private AsyncDispatcher	publish_dispatcher = new AsyncDispatcher();
	
	private	DistributedDatabase 	ddb;
	
	private CryptoHandler ecc_handler = CryptoManagerFactory.getSingleton().getECCHandler();

	private List	buddies 	= new ArrayList();
	private Map		buddies_map	= new HashMap();
	
	private CopyOnWriteList		listeners 			= new CopyOnWriteList();
	private CopyOnWriteList		request_listeners	= new CopyOnWriteList(); 
		
	private SESecurityManager	sec_man;

	private GenericMessageRegistration	msg_registration;
		
	private int	inbound_limit;
	private int	outbound_limit;
	
	private RateLimiter	inbound_limiter = 
		new RateLimiter()
		{
			public int 
			getRateLimitBytesPerSecond() 
			{
				return( inbound_limit );
			}
		};
		
	private RateLimiter	outbound_limiter = 
		new RateLimiter()
		{
			public int 
			getRateLimitBytesPerSecond() 
			{
				return( outbound_limit );
			}
		};
			
	private boolean		config_dirty;
	
	private Random	random = new SecureRandom();
	
	private BuddyPluginAZ2		az2_handler;
	
	private List	publish_write_contacts = new ArrayList();
	
	private int		status_seq;
	
	{
		while( status_seq == 0 ){
			
			status_seq = random.nextInt();
		}
	}
		
	private Set			pd_preinit		= new HashSet();
	private List		pd_queue 		= new ArrayList();
	private AESemaphore	pd_queue_sem	= new AESemaphore( "BuddyPlugin:persistDispatch");
	private AEThread2	pd_thread;
	
	private boolean		bogus_ygm_written;
	
	private BuddyPluginTracker	buddy_tracker;
	
	public static void
	load(
		PluginInterface		plugin_interface )
	{
		String name = 
			plugin_interface.getUtilities().getLocaleUtilities().getLocalisedMessageText( "Views.plugins." + VIEW_ID + ".title" );
		
		plugin_interface.getPluginProperties().setProperty( "plugin.version", 	"1.0" );
		plugin_interface.getPluginProperties().setProperty( "plugin.name", 		name );
	}

	public void
	initialize(
		final PluginInterface		_plugin_interface )
	{
		plugin_interface	= _plugin_interface;
		
		az2_handler = new BuddyPluginAZ2( this );
				
		sec_man = plugin_interface.getUtilities().getSecurityManager();

		logger = plugin_interface.getLogger().getChannel( "Friends" );
		
		logger.setDiagnostic();
				
		BasicPluginConfigModel config = plugin_interface.getUIManager().createBasicPluginConfigModel( "Views.plugins." + VIEW_ID + ".title" );
			
			// enabled

		enabled_param = config.addBooleanParameter2( "azbuddy.enabled", "azbuddy.enabled", false );
				
			// nickname

		nick_name_param = config.addStringParameter2( "azbuddy.nickname", "azbuddy.nickname", "" );

		nick_name_param.setGenerateIntermediateEvents( false );
		
		nick_name_param.addListener(
				new ParameterListener()
				{
					public void
					parameterChanged(
						Parameter	param )
					{
						updateNickName( nick_name_param.getValue());
					}
				});
		
		final IntParameter	protocol_speed = config.addIntParameter2( "azbuddy.protocolspeed", "azbuddy.protocolspeed", 32 );
		
		protocol_speed.setMinimumRequiredUserMode( Parameter.MODE_ADVANCED );
		
		inbound_limit = protocol_speed.getValue()*1024;
		
		protocol_speed.addListener(
				new ParameterListener()
				{
					public void
					parameterChanged(
						Parameter	param )
					{
						inbound_limit = protocol_speed.getValue()*1024;
					}
				});
		
		final TableContextMenuItem menu_item_itorrents = 
			plugin_interface.getUIManager().getTableManager().addContextMenuItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE, "azbuddy.contextmenu");
		final TableContextMenuItem menu_item_ctorrents 	= 
			plugin_interface.getUIManager().getTableManager().addContextMenuItem(TableManager.TABLE_MYTORRENTS_COMPLETE, "azbuddy.contextmenu");
		
		menu_item_itorrents.setStyle(TableContextMenuItem.STYLE_MENU);
		menu_item_ctorrents.setStyle(TableContextMenuItem.STYLE_MENU);
		
		MenuItemFillListener	menu_fill_listener = 
			new MenuItemFillListener()
			{
				public void
				menuWillBeShown(
					MenuItem	menu,
					Object		_target )
				{
					if ( !( isEnabled() && isAvailable())){
						
						menu.setEnabled( false );
						
						return;
					}
					
					Object	obj = null;
					
					if ( _target instanceof TableRow ){
						
						obj = ((TableRow)_target).getDataSource();
	
					}else{
						
						TableRow[] rows = (TableRow[])_target;
					     
						if ( rows.length > 0 ){
						
							obj = rows[0].getDataSource();
						}
					}
					
					if ( obj == null ){
						
						menu.setEnabled( false );

						return;
					}
					
					Download				download;
					
					if ( obj instanceof Download ){
					
						download = (Download)obj;
						
					}else{
						
						DiskManagerFileInfo file = (DiskManagerFileInfo)obj;
						
						try{
							download	= file.getDownload();
							
						}catch( DownloadException e ){	
							
							Debug.printStackTrace(e);
							
							return;
						}
					}
					
					Torrent torrent = download.getTorrent();
					
					boolean enabled = torrent != null && !torrent.isPrivate();
					
					menu.removeAllChildItems();

					if ( enabled ){
					
						List buddies = getBuddies();
						
						boolean	incomplete = ((TableContextMenuItem)menu).getTableID() == TableManager.TABLE_MYTORRENTS_INCOMPLETE;
						
						TableContextMenuItem parent = incomplete?menu_item_itorrents:menu_item_ctorrents;
						
						final Download f_download = download;
						
						for (int i=0;i<buddies.size();i++){
							
							final BuddyPluginBuddy	buddy = (BuddyPluginBuddy)buddies.get(i);
							
							TableContextMenuItem item =
								plugin_interface.getUIManager().getTableManager().addContextMenuItem(
									parent,
									"!" + buddy.getName() + "!");
							
							item.addListener(
								new MenuItemListener()
								{
									public void 
									selected(
										MenuItem 	menu,
										Object 		target ) 
									{
										az2_handler.sendAZ2Torrent( f_download.getTorrent(), buddy );
									}
								});
						}
					}
					
					menu.setEnabled( enabled );
				}
			};
			
		menu_item_itorrents.addFillListener( menu_fill_listener );
		menu_item_ctorrents.addFillListener( menu_fill_listener );
		
		buddy_tracker = new BuddyPluginTracker( this, config );
		
		plugin_interface.getUIManager().addUIListener(
			new UIManagerListener()
			{
				public void
				UIAttached(
					final UIInstance		instance )
				{
					if ( instance instanceof UISWTInstance ){
						
						UISWTInstance swt_ui = (UISWTInstance)instance;
						
						BuddyPluginView view = new BuddyPluginView( BuddyPlugin.this, swt_ui );

						swt_ui.addView(	UISWTInstance.VIEW_MAIN, VIEW_ID, view );
						
						//swt_ui.openMainView( VIEW_ID, view, null );
					}
					
					setupDisablePrompt(instance);
				}

				public void
				UIDetached(
					UIInstance		instance )
				{
				}
			});
		
		ParameterListener enabled_listener = 
			new ParameterListener()
			{
				public void
				parameterChanged(
					Parameter	param )
				{
					boolean enabled = enabled_param.getValue();

					if (param != null && !enabled) {
						UIInstance[] uis = plugin_interface.getUIManager().getUIInstances();
						if (uis != null && uis.length > 0) {
							int i = promptUserOnDisable(uis[0]);
  						if (i != 0) {
    						enabled_param.setValue(true);
    						fireEnabledStateChanged();
  							return;
  						}
						}
					}
					
					nick_name_param.setEnabled( enabled );
					
						// only toggle overall state on a real change
					
					if ( param != null ){
					
						setEnabledInternal( enabled );
						fireEnabledStateChanged();
					}
				}
			};
		
		enabled_listener.parameterChanged( null );
			
		enabled_param.addListener( enabled_listener );
		
		loadConfig();
		
		registerMessageHandler();
		
		plugin_interface.addListener(
			new PluginListener()
			{
				public void
				initializationComplete()
				{
					final DelayedTask dt = plugin_interface.getUtilities().createDelayedTask(new Runnable()
						{
							public void 
							run() 
							{
								new AEThread2( "BuddyPlugin:init", true )
								{
									public void
									run()
									{
										startup();
									}
								}.start();
							}
						});
					
					dt.queue();
				}
				
				public void
				closedownInitiated()
				{	
					saveConfig( true );
					
					closedown();
				}
				
				public void
				closedownComplete()
				{				
				}
			});
	}
	
	/**
	 * 
	 *
	 * @since 3.0.5.3
	 */
	protected void 
	setupDisablePrompt(
			final UIInstance ui) 
	{
		if (plugin_interface == null) {
			return;
		}

		String enabledConfigID = "PluginInfo." + plugin_interface.getPluginID()
				+ ".enabled";
		COConfigurationManager.addParameterListener(enabledConfigID,
				new org.gudy.azureus2.core3.config.ParameterListener() {
					public void parameterChanged(
							String parameterName) 
					{
						boolean enabled = COConfigurationManager.getBooleanParameter(parameterName);
						if (enabled) {
							fireEnabledStateChanged();
							return;
						}

						if (promptUserOnDisable(ui) != 0) {
							plugin_interface.setDisabled(false);
							COConfigurationManager.setParameter(parameterName, true);
						} else {
							fireEnabledStateChanged();
						}
					}
				});
	}
	
	protected int
	promptUserOnDisable(UIInstance ui)
	{
		if ("az2".equals(COConfigurationManager.getStringParameter("ui", "az3"))) {
			return 0;
		}
		LocaleUtilities localeUtil = plugin_interface.getUtilities().getLocaleUtilities();
		return ui.promptUser(
				localeUtil.getLocalisedMessageText("azbuddy.ui.dialog.disable.title"),
				localeUtil.getLocalisedMessageText("azbuddy.ui.dialog.disable.text"),
				new String[] {
					localeUtil.getLocalisedMessageText("Button.yes"),
					localeUtil.getLocalisedMessageText("Button.no"),
				}, 1);
	}

	protected void
	startup()
	{
		try{
			ddb = plugin_interface.getDistributedDatabase();
		
			if ( !ddb.isAvailable()){
				
				throw( new Exception( "DDB Unavailable" ));
			}
				// pick up initial values before enabling

			ddb.addListener(
				new DistributedDatabaseListener()
				{
					public void 
					event(
						DistributedDatabaseEvent event )
					{
						if ( event.getType() == DistributedDatabaseEvent.ET_LOCAL_CONTACT_CHANGED ){
							
							updateIP();
						}
					}
				});
					
			updateIP();
			
			updateNickName( nick_name_param.getValue());
			
			COConfigurationManager.addAndFireParameterListeners(
					new String[]{
						"TCP.Listen.Port",
						"TCP.Listen.Port.Enable",
						"UDP.Listen.Port",
						"UDP.Listen.Port.Enable" },
					new org.gudy.azureus2.core3.config.ParameterListener()
					{
						public void 
						parameterChanged(
							String parameterName )
						{
							updateListenPorts();
						}
					});
			
			CryptoManagerFactory.getSingleton().addKeyListener(
				new CryptoManagerKeyListener()
				{
					public void 
					keyChanged(
						CryptoHandler handler ) 
					{
						updateKey();
					}
					
					public void
					keyLockStatusChanged(
						CryptoHandler		handler )
					{	
						boolean unlocked = handler.isUnlocked();
						
						if ( unlocked ){
							
							if ( latest_publish.isEnabled()){
								
								updatePublish( latest_publish );
							}
						}
					}
				});
			
			ready_to_publish	= true;
			
			setEnabledInternal( enabled_param.getValue());
			
			checkBuddiesAndRepublish();
			
			fireInitialised( true );
			
		}catch( Throwable e ){
		
			log( "Initialisation failed", e );
			
			fireInitialised( false );
		}
	}
	
	public boolean
	isEnabled()
	{
		return( enabled_param.getValue());
	}
	
	public void
	setEnabled(
		boolean		enabled )
	{
		enabled_param.setValue( enabled );
	}
	
	protected void
	setEnabledInternal(
		boolean		_enabled )
	{
		synchronized( this ){
						
			if ( latest_publish.isEnabled() != _enabled ){
				
				publishDetails new_publish = latest_publish.getCopy();
				
				new_publish.setEnabled( _enabled );
				
				updatePublish( new_publish );
			}
		}
	}
	
	public BuddyPluginTracker
	getTracker()
	{
		return( buddy_tracker );
	}
	
	public String
	getNickname()
	{
		return(  nick_name_param.getValue());
	}
	
	public void
	setNickname(
		String	str )
	{
		nick_name_param.setValue( str );
	}
	
	protected void
	registerMessageHandler()
	{
		try{
			addRequestListener(
				new BuddyPluginBuddyRequestListener()
				{
					public Map
					requestReceived(
						BuddyPluginBuddy	from_buddy,
						int					subsystem,
						Map					request )
					
						throws BuddyPluginException
					{
						if ( subsystem == SUBSYSTEM_INTERNAL ){
						
							if ( !from_buddy.isAuthorised()){
							
								throw( new BuddyPluginException( "Unauthorised" ));
							}
							
							return( processInternalRequest( from_buddy, request ));							
						}

						return( null );
					}
					
					public void
					pendingMessages(
						BuddyPluginBuddy[]	from_buddies )
					{
					}
				});
			
			msg_registration = 
				plugin_interface.getMessageManager().registerGenericMessageType(
					"AZBUDDY", "Buddy message handler", 
					MessageManager.STREAM_ENCRYPTION_RC4_REQUIRED,
					new GenericMessageHandler()
					{
						public boolean
						accept(
							GenericMessageConnection	connection )
						
							throws MessageException
						{
							if ( !isEnabled()){
								
								return( false );
							}
							
							final String originator = connection.getEndpoint().getNotionalAddress().getAddress().getHostAddress();
							
							if ( TRACE ){
								System.out.println( "accept " + originator );
							}
							
							try{
								String reason = "Friend: Incoming connection establishment (" + originator + ")";
									
								addRateLimiters( connection );

								connection = 
									sec_man.getSTSConnection(
											connection, 
											sec_man.getPublicKey( SEPublicKey.KEY_TYPE_ECC_192, reason ),
											new SEPublicKeyLocator()
											{
												public boolean
												accept(
													Object		context,
													SEPublicKey	other_key )
												{
													String	other_key_str = Base32.encode( other_key.encodeRawPublicKey());

													if ( TRACE ){
														System.out.println( "Incoming: acceptKey - " + other_key_str );
													}
													
													try{
														synchronized( BuddyPlugin.this ){
																
															int	unauth_count = 0;
															
															for (int i=0;i<buddies.size();i++){
															
																BuddyPluginBuddy	buddy = (BuddyPluginBuddy)buddies.get(i);
	
																if ( buddy.getPublicKey().equals( other_key_str )){
																	
																		// don't accept a second or subsequent connection for unauth buddies
																		// as they have a single chance to be processed
																	
																	if ( !buddy.isAuthorised()){
																		
																		log( "Incoming connection from " + originator + " failed as for unauthorised buddy" );
																		
																		return( false );
																	}
																	
																	buddy.incomingConnection((GenericMessageConnection)context );	
																	
																	return( true );
																}
																
																if ( !buddy.isAuthorised()){
																	
																	unauth_count++;
																}
															}
															
																// no existing authorised buddy
															
															if ( unauth_count < MAX_UNAUTH_BUDDIES ){
																		
																if ( tooManyUnauthConnections( originator )){
																	
																	log( "Too many recent unauthorised connections from " + originator );
																	
																	return( false );
																}
																
																BuddyPluginBuddy buddy = addBuddy( other_key_str, SUBSYSTEM_AZ2, false );
																
																buddy.incomingConnection((GenericMessageConnection)context );	
																	
																return( true );
	
															}
														}
														
														log( "Incoming connection from " + originator + " failed due to pk mismatch" );
	
														return( false );
														
													}catch( Throwable e ){
														
														log( "Incomming connection from " + originator + " failed", e );
														
														return( false );
													}
												}
											},
											reason,
											SESecurityManager.BLOCK_ENCRYPTION_AES );
							
							}catch( Throwable e ){
								
								connection.close();
								
								log( "Incoming connection from " + originator + " failed", e );
							}
							
							return( true );
						}
					});
					
		}catch( Throwable e ){
			
			log( "Failed to register message listener", e );
		}
	}
	
	protected void
	addRateLimiters(
		GenericMessageConnection	connection )
	{
		connection.addInboundRateLimiter( inbound_limiter );
		connection.addOutboundRateLimiter( outbound_limiter );
	}
	
	protected boolean
	tooManyUnauthConnections(
		String	originator )
	{
		synchronized( this ){
	
			if ( unauth_bloom == null ){
				
				unauth_bloom = BloomFilterFactory.createAddRemove4Bit( UNAUTH_BLOOM_CHUNK );
				
				unauth_bloom_create_time	= SystemTime.getCurrentTime();
			}
			
			int	hit_count = unauth_bloom.add( originator.getBytes());
			
			if ( hit_count >= 8 ){
			    		
				Debug.out( "Too many recent unauthorised connection attempts from " + originator );
     		
				return( true );
			}
			
			return( false );
		}
	}
	
	protected void
	checkUnauthBloom()
	{
		synchronized( this ){
		
			if ( unauth_bloom != null ){
				
				long	now = SystemTime.getCurrentTime();
				
				if ( now < unauth_bloom_create_time ){
					
					unauth_bloom_create_time = now;
					
				}else if ( now - unauth_bloom_create_time > UNAUTH_BLOOM_RECREATE ){
					
					unauth_bloom = null;
				}
			}
		}
	}
	
	protected void
	checkMaxMessageSize(
		int		size )
	
		throws BuddyPluginException
	{
		if ( size > MAX_MESSAGE_SIZE ){
			
			throw( new BuddyPluginException( "Message is too large to send, limit is " + DisplayFormatters.formatByteCountToKiBEtc( MAX_MESSAGE_SIZE )));
		}
	}
	
	protected void
	checkPersistentDispatch()
	{
		List	buddies_copy;
		
		synchronized( this ){
		
			buddies_copy = new ArrayList( buddies );
		}
				
		for (int i=0;i<buddies_copy.size();i++){
			
			BuddyPluginBuddy	buddy = (BuddyPluginBuddy)buddies_copy.get(i);

			buddy.checkPersistentDispatch();
		}
	}
	
	protected void
	persistentDispatchInit()
	{
		Iterator it = pd_preinit.iterator();
		
		while( it.hasNext()){
		
			persistentDispatchPending((BuddyPluginBuddy)it.next());
		}
		
		pd_preinit = null;
	}
	
	protected void
	persistentDispatchPending(
		BuddyPluginBuddy	buddy )
	{
		synchronized( pd_queue ){
			
			if ( initialisation_state == INIT_UNKNOWN ){
				
				pd_preinit.add( buddy );
				
				return;
			}
			
			if ( !pd_queue.contains( buddy )){
				
				pd_queue.add( buddy );
				
				pd_queue_sem.release();
				
				if ( pd_thread == null ){
					
					pd_thread = 
						new AEThread2( "BuddyPlugin:persistDispatch", true )
						{
							public void
							run()
							{
								while( true ){
									
									if ( !pd_queue_sem.reserve( 30*1000 )){
										
										synchronized( pd_queue ){
											
											if ( pd_queue.isEmpty()){
												
												pd_thread	= null;
												
												break;
											}
										}
									}else{
										
										BuddyPluginBuddy	buddy;
										
										synchronized( pd_queue ){
											
											buddy = (BuddyPluginBuddy)pd_queue.remove(0);
										}
										
										buddy.persistentDispatch();
									}
								}
							}
						};
						
					pd_thread.start();
				}
			}
		}
	}
	
	protected Map
	processInternalRequest(
		BuddyPluginBuddy	from_buddy,
		Map					request )		
		
		throws BuddyPluginException
	{
		int	type = ((Long)request.get("type")).intValue();
		
		if ( type == RT_INTERNAL_REQUEST_PING ){
		
			Map	reply = new HashMap();
		
			reply.put( "type", new Long( RT_INTERNAL_REPLY_PING ));
		
			return( reply );
			
		}else if ( type == RT_INTERNAL_REQUEST_CLOSE ){
		
			from_buddy.receivedCloseRequest( request );
			
			Map	reply = new HashMap();
		
			reply.put( "type", new Long( RT_INTERNAL_REPLY_CLOSE ));
		
			return( reply );
			
		}else{
			
			throw( new BuddyPluginException( "Unrecognised request type " + type ));
		}
	}

	protected void
	updateListenPorts()
	{
		synchronized( this ){

			int	tcp_port = COConfigurationManager.getIntParameter( "TCP.Listen.Port" );
			boolean	tcp_enabled = COConfigurationManager.getBooleanParameter( "TCP.Listen.Port.Enable" );
			int	udp_port = COConfigurationManager.getIntParameter("UDP.Listen.Port" );
			boolean	udp_enabled = COConfigurationManager.getBooleanParameter( "UDP.Listen.Port.Enable" );
				
			if ( !tcp_enabled ){
				
				tcp_port = 0;
			}
			
			if ( !udp_enabled ){
				
				udp_port = 0;
			}
			
			if ( 	latest_publish.getTCPPort() != tcp_port ||
					latest_publish.getUDPPort() != udp_port ){
				
				publishDetails new_publish = latest_publish.getCopy();
				
				new_publish.setTCPPort( tcp_port );
				new_publish.setUDPPort( udp_port );
				
				updatePublish( new_publish );
			}
		}
	}
	
	protected void
	updateIP()
	{
		if ( ddb == null || !ddb.isAvailable()){
			
			return;
		}
				
		synchronized( this ){

			InetAddress public_ip = ddb.getLocalContact().getAddress().getAddress();
				
			if ( 	latest_publish.getIP() == null ||
					!latest_publish.getIP().equals( public_ip )){
					
				publishDetails new_publish = latest_publish.getCopy();
				
				new_publish.setIP( public_ip );
				
				updatePublish( new_publish );
			}
		}
	}
	
	protected void
	updateNickName(
		String		new_nick )
	{
		new_nick = new_nick.trim();
		
		if ( new_nick.length() == 0 ){
			
			new_nick = null;
		}
		
		synchronized( this ){

			String	old_nick = latest_publish.getNickName();
			
			if ( !stringsEqual( new_nick, old_nick )){
			
				publishDetails new_publish = latest_publish.getCopy();
					
				new_publish.setNickName( new_nick );
					
				updatePublish( new_publish );
			}
		}
	}
	
	protected boolean
	stringsEqual(
		String	s1, 
		String	s2 )
	{
		if ( s1 == null && s2 == null ){
			
			return( true );
		}
		
		if ( s1 == null || s2 == null ){
			
			return( false );
		}
		
		return( s1.equals( s2 ));
	}
	
	protected void
	updateKey()
	{
		synchronized( this ){

			publishDetails new_publish = latest_publish.getCopy();
				
			new_publish.setPublicKey( null );
				
			updatePublish( new_publish );
		}
	}
	
	protected void
	updatePublish(
		final publishDetails	details )
	{
		latest_publish = details;
		
		if ( ddb == null || !ready_to_publish ){
			
			return;
		}
		
		publish_dispatcher.dispatch(
			new AERunnable()
			{
				public void
				runSupport()
				{
						// only execute the most recent publish
					
					if ( publish_dispatcher.getQueueSize() > 0 ){
						
						return;
					}
					
					updatePublishSupport( details );
				}
			});
	}
	
	protected void
	updatePublishSupport(
		publishDetails	details )
	{
		byte[]	key_to_remove = null;
		
		publishDetails	existing_details;
		
		boolean	log_this;
		
		synchronized( this ){

			log_this = !current_publish.getString().equals( details.getString());
			
			existing_details = current_publish;
			
			if ( !details.isEnabled()){
				
				if ( current_publish.isPublished()){
					
					key_to_remove	= current_publish.getPublicKey();
				}
			}else{
								
				if ( details.getPublicKey() == null ){
					
					try{
						details.setPublicKey( ecc_handler.getPublicKey( "Creating online status key" ));
						
					}catch( Throwable e ){
						
						log( "Failed to publish details", e );
						
						return;
					}			
				}
				
				if ( current_publish.isPublished()){
					
					byte[]	existing_key = current_publish.getPublicKey();
				
					if ( !Arrays.equals( existing_key, details.getPublicKey())){
						
						key_to_remove = existing_key;
					}
				}
			}
			
			current_publish = details;
		}
		
		if ( key_to_remove != null ){
			
			log( "Removing old status publish: " + existing_details.getString());
			
			try{
				ddb.delete(
					new DistributedDatabaseListener()
					{
						public void
						event(
							DistributedDatabaseEvent		event )
						{
						}
					},
					getStatusKey( key_to_remove, "Friend status de-registration for old key" ));
				
			}catch( Throwable e ){	
			
				log( "Failed to remove existing publish", e );
			}
		}
		
		if ( details.isEnabled()){
			
				// ensure we have a sensible ip
			
			InetAddress ip = details.getIP();
			
			if ( ip.isLoopbackAddress() || ip.isLinkLocalAddress() || ip.isSiteLocalAddress()){
				
				log( "Can't publish as ip address is invalid: " + details.getString());
				
				return;
			}
			
			details.setPublished( true );
			
			Map	payload = new HashMap();
			
			if ( details.getTCPPort() > 0 ){
			
				payload.put( "t", new Long(  details.getTCPPort() ));
			}
			
			if (  details.getUDPPort() > 0 ){
				
				payload.put( "u", new Long( details.getUDPPort() ));
			}
						
			payload.put( "i", ip.getAddress());
			
			String	nick = details.getNickName();
			
			if ( nick != null ){
				
				if ( nick.length() > 32 ){
					
					nick = nick.substring( 0, 32 );
				}
				
				payload.put( "n", nick );
			}
			
			int	next_seq = ++status_seq;
			
			if ( next_seq == 0 ){
				
				next_seq = ++status_seq;
			}
			
			details.setSequence( next_seq );
			
			payload.put( "s", new Long( next_seq ));
			
			boolean	failed_to_get_key = true;
			
			try{
				byte[] data = BEncoder.encode( payload );
										
				DistributedDatabaseKey	key = getStatusKey( details.getPublicKey(), "My buddy status registration " + payload );
	
				byte[] signature = ecc_handler.sign( data, "Friend online status" );
			
				failed_to_get_key = false;
				
				byte[]	signed_payload = new byte[ 1 + signature.length + data.length ];
				
				signed_payload[0] = (byte)signature.length;
				
				System.arraycopy( signature, 0, signed_payload, 1, signature.length );
				System.arraycopy( data, 0, signed_payload, 1 + signature.length, data.length );		
				
				DistributedDatabaseValue	value = ddb.createValue( signed_payload );
				
				final AESemaphore	sem = new AESemaphore( "BuddyPlugin:reg" );
				
				if ( log_this ){
					
					logMessage( "Publishing status starts: " + details.getString());
				}
				
				last_publish_start = SystemTime.getMonotonousTime();
				
				ddb.write(
					new DistributedDatabaseListener()
					{
						private List	write_contacts = new ArrayList();
						
						public void
						event(
							DistributedDatabaseEvent		event )
						{
							int	type = event.getType();
						
							if ( type == DistributedDatabaseEvent.ET_VALUE_WRITTEN ){
								
								write_contacts.add( event.getContact());
								
							}else if ( 	type == DistributedDatabaseEvent.ET_OPERATION_TIMEOUT ||
										type == DistributedDatabaseEvent.ET_OPERATION_COMPLETE ){

								synchronized( publish_write_contacts ){
									
									publish_write_contacts.clear();
									
									publish_write_contacts.addAll( write_contacts );
								}
								
								sem.release();
							}
						}
					},
					key,
					value );
				
				sem.reserve();
				
				if ( log_this ){
				
					logMessage( "My status publish complete" );
				}
			}catch( Throwable e ){
				
				logMessage( "Failed to publish online status", e );
				
				if ( failed_to_get_key ){
					
					if ( 	last_publish_start == 0 ||
							SystemTime.getMonotonousTime() - last_publish_start > STATUS_REPUBLISH_PERIOD ){
					
						log( "Rescheduling publish as failed to get key" );
					
						SimpleTimer.addEvent(
							"BuddyPlugin:republish",
							SystemTime.getCurrentTime() + 60*1000,
							new TimerEventPerformer()
							{
								public void 
								perform(
									TimerEvent event) 
								{
									if ( 	last_publish_start == 0 ||
											SystemTime.getMonotonousTime() - last_publish_start > STATUS_REPUBLISH_PERIOD ){
									
										if ( latest_publish.isEnabled()){
											
											updatePublish( latest_publish );
										}
									}
								}
							});
							
					}	
				}
			}
		}
	}
	
	protected int
	getCurrentStatusSeq()
	{
		return( current_publish.getSequence());
	}
	
	protected void
	closedown()
	{
		logMessage( "Closing down" );

		if ( ddb != null ){
			
			boolean	restarting = AzureusCoreFactory.getSingleton().isRestarting();
		
			List	buddies = getAllBuddies();
			
			logMessage( "   closing buddy connections" );
			
			for (int i=0;i<buddies.size();i++){
				
				((BuddyPluginBuddy)buddies.get(i)).sendCloseRequest( restarting );
			}
			
			if ( !restarting ){
				
				logMessage( "   updating online status" );
				
				List	contacts = new ArrayList();
				
				synchronized( publish_write_contacts ){
					
					contacts.addAll( publish_write_contacts );
				}
				
				byte[] key_to_remove;
				
				synchronized( this ){
	
					key_to_remove	= current_publish.getPublicKey();
				}
				
				if ( contacts.size() == 0 || key_to_remove == null ){
					
					return;
				}
				
				DistributedDatabaseContact[] contact_a = new DistributedDatabaseContact[contacts.size()];
				
				contacts.toArray( contact_a );
				
				try{
					ddb.delete(
						new DistributedDatabaseListener()
						{
							public void
							event(
								DistributedDatabaseEvent		event )
							{
								if ( event.getType() == DistributedDatabaseEvent.ET_VALUE_DELETED ){
	
									// System.out.println( "Deleted status from " + event.getContact().getName());
								}
							}
						},
						getStatusKey( key_to_remove, "Friend status de-registration for closedown" ),
						contact_a );
					
				}catch( Throwable e ){	
				
					log( "Failed to remove existing publish", e );
				}
			}
		}
	}
	
	protected DistributedDatabaseKey
	getStatusKey(
		byte[]	public_key,
		String	reason )
	
		throws Exception
	{
		byte[]	key_prefix = "azbuddy:status".getBytes();
		
		byte[]	key_bytes = new byte[ key_prefix.length + public_key.length ];
		
		System.arraycopy( key_prefix, 0, key_bytes, 0, key_prefix.length );
		System.arraycopy( public_key, 0, key_bytes, key_prefix.length, public_key.length );
		
		DistributedDatabaseKey key = ddb.createKey( key_bytes, reason );
		
		return( key );
	}

	protected DistributedDatabaseKey
	getYGMKey(
		byte[]	public_key,
		String	reason )
	
		throws Exception
	{
		byte[]	key_prefix = "azbuddy:ygm".getBytes();
		
		byte[]	key_bytes = new byte[ key_prefix.length + public_key.length ];
		
		System.arraycopy( key_prefix, 0, key_bytes, 0, key_prefix.length );
		System.arraycopy( public_key, 0, key_bytes, key_prefix.length, public_key.length );
		
		DistributedDatabaseKey key = ddb.createKey( key_bytes, reason );
		
		return( key );
	}
	
	protected void
	setConfigDirty()
	{
		synchronized( this ){
			
			config_dirty = true;
		}
	}
	
	protected void
	loadConfig()
	{
		long	now = SystemTime.getCurrentTime();
		
		synchronized( this ){
			
			Map map = readConfig(); 
					
			List	buddies_config = (List)map.get( "friends" );
				
			if ( buddies_config != null ){
							
				for (int i=0;i<buddies_config.size();i++){
					
					Object o = buddies_config.get(i);
		
					if ( o instanceof Map ){
						
						Map	details = (Map)o;
						
						String	key = new String((byte[])details.get( "pk" ));
						
						List	recent_ygm = (List)details.get( "ygm" );
											
						String	nick = decodeString((byte[])details.get( "n" ));
						
						Long	l_seq = (Long)details.get( "ls" );
						
						int	last_seq = l_seq==null?0:l_seq.intValue();
						
						Long	l_lo = (Long)details.get( "lo" );
						
						long	last_time_online = l_lo==null?0:l_lo.longValue();
					
						if ( last_time_online > now ){
							
							last_time_online = now;
						}
						
						Long l_subsystem = (Long)details.get( "ss" );
						
						int	subsystem = l_subsystem==null?SUBSYSTEM_AZ2:l_subsystem.intValue();
						
						BuddyPluginBuddy buddy = new BuddyPluginBuddy( this, subsystem, true, key, nick, last_seq, last_time_online, recent_ygm );
						
						logMessage( "Loaded buddy " + buddy.getString());
						
						buddies.add( buddy );
						
						buddies_map.put( key, buddy );
					}
				}
			}
		}
	}
	
	protected String
	decodeString(
		byte[]		bytes )
	{
		if (  bytes == null ){
			
			return( null );
		}
		
		try{
			return( new String( bytes, "UTF8" ));
			
		}catch( Throwable e ){
			
			return( null );
		}
	}
	
	protected void
	saveConfig()
	{
		saveConfig( false );
	}
	
	protected void
	saveConfig(
		boolean	force )
	{
		synchronized( this ){

			if ( config_dirty || force ){
				
				List buddies_config = new ArrayList();
		
				for (int i=0;i<buddies.size();i++){
					
					BuddyPluginBuddy buddy = (BuddyPluginBuddy)buddies.get(i);
		
					if ( !buddy.isAuthorised()){
						
						continue;
					}
					
					Map	map = new HashMap();
				
					map.put( "pk", buddy.getPublicKey());
				
					List	ygm = buddy.getYGMMarkers();
					
					if ( ygm != null ){
						
						map.put( "ygm", ygm );
					}
					
					String	nick = buddy.getNickName();
					
					if ( nick != null ){
						
						map.put( "n", nick );
					}
					
					map.put( "ls", new Long( buddy.getLastStatusSeq()));
					
					map.put( "lo", new Long( buddy.getLastTimeOnline()));
					
					map.put( "ss", new Long( buddy.getSubsystem()));
					
					buddies_config.add( map );
				}
				
				Map	map = new HashMap();
				
				map.put( "friends", buddies_config );
				
				writeConfig( map );
				
				config_dirty = false;
			}
		}
	}
	
	public BuddyPluginBuddy
	addBuddy(
		String		key,
		int			subsystem )
	
	{
		return( addBuddy( key, subsystem, true ));
	}
	
	protected BuddyPluginBuddy
	addBuddy(
		String		key,
		int			subsystem,
		boolean		authorised )
	{
		if ( key.length() == 0 || !verifyPublicKey( key )){
			
			return( null );
		}
				
		BuddyPluginBuddy	buddy_to_return = null;
		
			// buddy may be already present as unauthorised in which case we pick it up
			// and authorise it and send the added event (we don't fire added events for
			// unauthorised buddies)
		
		synchronized( this ){
						
			for (int i=0;i<buddies.size();i++){
				
				BuddyPluginBuddy buddy = (BuddyPluginBuddy)buddies.get(i);
				
				if ( buddy.getPublicKey().equals( key )){
					
					if ( buddy.getSubsystem() != subsystem ){
						
						buddy.setSubsystem( subsystem );
						
						saveConfig( true );
					}
					
					if ( authorised && !buddy.isAuthorised()){
						
						buddy.setAuthorised( true );
						
						buddy_to_return	= buddy;
						
					}else{
					
						return( buddy );
					}
				}
			}
			
			if ( buddy_to_return == null ){
				
				buddy_to_return = new BuddyPluginBuddy( this, subsystem, authorised, key, null, 0, 0, null );
				
				buddies.add( buddy_to_return );
				
				buddies_map.put( key, buddy_to_return );
			}
			
			if ( buddy_to_return.isAuthorised()){
				
				logMessage( "Added buddy " + buddy_to_return.getString());
	
				saveConfig( true );
			}
		}
		
		if ( buddy_to_return.isAuthorised()){
		
			fireAdded( buddy_to_return );
		}
		
		return( buddy_to_return );
	}
	
	protected void
	removeBuddy(
		BuddyPluginBuddy 	buddy,
		boolean				fire_removed )
	{
		synchronized( this ){

			if ( !buddies.remove( buddy )){
				
				return;
			}
		
			buddies_map.remove( buddy.getPublicKey());
						
			logMessage( "Removed friend " + buddy.getString());

			saveConfig( true );
		}
		
		buddy.destroy();
		
		if ( fire_removed ){
		
			fireRemoved( buddy );
		}
	}
	
	protected Map
	readConfig()
	{
		File	config_file = new File( plugin_interface.getUtilities().getAzureusUserDir(), "friends.config" );
		
		return( readConfigFile( config_file ));
	}
	
	protected void
	writeConfig(
		Map		map )
	{
		File	config_file = new File( plugin_interface.getUtilities().getAzureusUserDir(), "friends.config" );
		
		writeConfigFile( config_file, map );
	}
	
	protected Map
	readConfigFile(
		File		name )
	{
		Map map = plugin_interface.getUtilities().readResilientBEncodedFile(
						name.getParentFile(), name.getName(), true );
		
		if ( map == null ){
			
			map = new HashMap();
		}
		
		return( map );
	}
	
	protected boolean
	writeConfigFile(
		File		name,
		Map			data )
	{
		plugin_interface.getUtilities().writeResilientBEncodedFile(
				name.getParentFile(), name.getName(), data, true );
		
		return( name.exists());
	}
	
	protected File
	getBuddyConfigDir()
	{
		return( new File( plugin_interface.getUtilities().getAzureusUserDir(), "friends" ));
	}
	
	public BuddyPluginAZ2
	getAZ2Handler()
	{
		return( az2_handler );
	}
	
	public String
	getPublicKey()
	{
		try{
			return( Base32.encode(ecc_handler.getPublicKey( "Friend get key" )));
			
		}catch( Throwable e ){
			
			logMessage( "Failed to access public key", e );
			
			return( null );
		}
	}
	
	public boolean
	verifyPublicKey(
		String		key )
	{
		return( ecc_handler.verifyPublicKey( Base32.decode( key )));
	}
	
	protected void
	checkBuddiesAndRepublish()
	{
		updateBuddys();
		
		plugin_interface.getUtilities().createTimer( "Buddy checker" ).addPeriodicEvent(
			TIMER_PERIOD,
			new UTTimerEventPerformer()
			{
				int	tick_count;
				
				public void 
				perform(
					UTTimerEvent event ) 
				{
					tick_count++;
					
					if ( !isEnabled()){
						
						return;
					}
												
					updateBuddys();
					
					if ( tick_count % STATUS_REPUBLISH_TICKS == 0 ){
							
						if ( latest_publish.isEnabled()){
								
							updatePublish( latest_publish );
						}
					}
					
					if ( tick_count % CHECK_YGM_TICKS == 0 ){

						checkMessagePending( tick_count );
					}
					
					if ( tick_count % BLOOM_CHECK_TICKS == 0 ){
						
						checkUnauthBloom();
					}
					
					if ( tick_count % SAVE_CONFIG_TICKS == 0 ){

						saveConfig();
					}
					
					if ( tick_count % PERSISTENT_MSG_CHECK_TICKS == 0 ){
						
						checkPersistentDispatch();
					}
					
					if ( buddy_tracker != null ){
						
						buddy_tracker.tick( tick_count );
					}
				}
			});
	}
	
	protected void
	updateBuddys()
	{
		List	buddies_copy;
		
		synchronized( this ){
		
			buddies_copy = new ArrayList( buddies );
		}
		
		long	now = SystemTime.getCurrentTime();
		
		for (int i=0;i<buddies_copy.size();i++){
			
			BuddyPluginBuddy	buddy = (BuddyPluginBuddy)buddies_copy.get(i);
			
			long	last_check = buddy.getLastStatusCheckTime();
			
			buddy.checkTimeouts();
			
			int	period = BUDDY_STATUS_CHECK_PERIOD_MIN + BUDDY_STATUS_CHECK_PERIOD_INC*buddies_copy.size()/5;
			
			if ( last_check > now || now - last_check > period ){
				
				if ( !buddy.statusCheckActive()){
			
					if ( buddy.isAuthorised()){
					
						updateBuddyStatus( buddy );
					}
				}
			}
		}
		
			// trim any non-authorised buddies that have gone idle

		synchronized( this ){
			
			for (int i=0;i<buddies_copy.size();i++){
			
				BuddyPluginBuddy	buddy = (BuddyPluginBuddy)buddies_copy.get(i);
				
				if ( buddy.isIdle() && !buddy.isAuthorised()){
					
					removeBuddy( buddy, false );
				}
			}
		}
	}
	
	protected void
	updateBuddyStatus(
		final BuddyPluginBuddy	buddy )
	{	
		if ( !buddy.statusCheckStarts()){
			
			return;
		}
		
		log( "Updating buddy status: " + buddy.getString());

		try{							
			final byte[]	public_key = buddy.getRawPublicKey();

			DistributedDatabaseKey	key = 
				getStatusKey( public_key, "Friend status check for " + buddy.getName());
			
			ddb.read(
				new DistributedDatabaseListener()
				{
					private long	latest_time;
					private Map		status;
					
					public void
					event(
						DistributedDatabaseEvent		event )
					{
						int	type = event.getType();
						
						if ( type == DistributedDatabaseEvent.ET_VALUE_READ ){
							
							try{
								DistributedDatabaseValue value = event.getValue();
								
								long time = value.getCreationTime();
								
								if ( time > latest_time ){
								
									byte[] signed_stuff = (byte[])value.getValue( byte[].class );
								
									Map	new_status = verifyAndExtract( signed_stuff, public_key );
									
									if ( new_status != null ){
	
										status = new_status;
																																							
										latest_time = time;
									}
								}
							}catch( Throwable e ){
								
								log( "Read failed", e );
							}
						}else if ( 	type == DistributedDatabaseEvent.ET_OPERATION_TIMEOUT ||
									type == DistributedDatabaseEvent.ET_OPERATION_COMPLETE ){
							
							if ( status == null ){
																
								buddy.statusCheckFailed();
								
							}else{
								
								try{
									int	tcp_port = ((Long)status.get( "t" )).intValue();
									int udp_port = ((Long)status.get( "u" )).intValue();
									
									InetAddress ip = InetAddress.getByAddress((byte[])status.get("i"));
									
									String	nick = decodeString((byte[])status.get( "n" ));
									
									Long	l_seq = (Long)status.get( "s" );
									
									int		seq = l_seq==null?0:l_seq.intValue();
									
									buddy.statusCheckComplete( latest_time, ip, tcp_port, udp_port, nick, seq );
									
								}catch( Throwable e ){
									
									buddy.statusCheckFailed();
									
									log( "Status decode failed", e );
								}
							}
						}
					}
				},
				key,
				120*1000 );
			
		}catch( Throwable e ){
			
			buddy.statusCheckFailed();
			
			log( "Friend status update failed: " + buddy.getString(), e );
		}
	}
	
	protected Map
	verifyAndExtract(
		byte[]		signed_stuff,
		byte[]		public_key )
	
		throws BuddyPluginException
	{
		int	signature_length = ((int)signed_stuff[0])&0xff;
		
		byte[]	signature 	= new byte[ signature_length ];
		byte[]	data		= new byte[ signed_stuff.length - 1 - signature_length];
		
		System.arraycopy( signed_stuff, 1, signature, 0, signature_length );
		System.arraycopy( signed_stuff, 1 + signature_length, data, 0, data.length );
			
		try{
			if ( ecc_handler.verify( public_key, data, signature )){													
	
				return( BDecoder.decode( data ));
																																	
			}else{
				
				logMessage( "Signature verification failed" );
				
				return( null );
			}
		}catch( Throwable e ){
			
			rethrow( "Verification failed", e );
			
			return( null );
		}
	}
	
	protected byte[]
	signAndInsert(
		Map		plain_stuff,
		String	reason )
	
		throws BuddyPluginException
	{
		try{
			byte[] data = BEncoder.encode( plain_stuff );
			
			byte[] signature = ecc_handler.sign( data, reason );
		
			byte[]	signed_payload = new byte[ 1 + signature.length + data.length ];
			
			signed_payload[0] = (byte)signature.length;
			
			System.arraycopy( signature, 0, signed_payload, 1, signature.length );
			System.arraycopy( data, 0, signed_payload, 1 + signature.length, data.length );		
	
			return( signed_payload );
			
		}catch( Throwable e ){
			
			rethrow( "Signing failed", e );
			
			return( null );
		}
	}
	
	public boolean
	verify(
		String				pk,
		byte[]				payload,
		byte[]				signature )
	
		throws BuddyPluginException
	{
		return( verify( Base32.decode( pk ), payload, signature ));
	}
	
	protected boolean
	verify(
		BuddyPluginBuddy	buddy,
		byte[]				payload,
		byte[]				signature )
	
		throws BuddyPluginException
	{
		return( verify( buddy.getRawPublicKey(), payload, signature ));
	}
	
	protected boolean
	verify(
		byte[]				pk,
		byte[]				payload,
		byte[]				signature )
	
		throws BuddyPluginException
	{
		try{
		
			return( ecc_handler.verify( pk, payload, signature ));
			
		}catch( Throwable e ){
			
			rethrow( "Verification failed", e );
			
			return( false );
		}
	}
	
	public byte[]
   	sign(
   		byte[]		payload )
	        	
	   	throws BuddyPluginException
	{ 
		try{
		
			return( ecc_handler.sign( payload, "Friend message signing" ));

		}catch( Throwable e ){
			
			rethrow( "Signing failed", e );
			
			return( null );
		}
	}

	protected cryptoResult
	encrypt(
		BuddyPluginBuddy	buddy,
		byte[]				payload )
	
		throws BuddyPluginException
	{
		return encrypt(buddy.getPublicKey(), payload, buddy.getName());
	}

	public cryptoResult
	encrypt(
		String				pk,
		byte[]				payload,
		String				forWho )
	
		throws BuddyPluginException
	{
		
		try{
			byte[]	hash = new byte[20];
			
			random.nextBytes( hash );
			
			Map	content = new HashMap();
			
			content.put( "h", hash );
			content.put( "p", payload );
			
			final byte[] encrypted = ecc_handler.encrypt( Base32.decode(pk), BEncoder.encode( content ), "Encrypting message for " + forWho);
			
			final byte[] sha1_hash = new SHA1Simple().calculateHash( hash );
			
			return( 
				new cryptoResult()
				{
					public byte[]
		    		getChallenge()
					{
						return( sha1_hash );
					}
		    		
		    		public byte[]
		    		getPayload()
		    		{
		    			return( encrypted );
		    		}
				});
			
		}catch( Throwable e ){
			
			rethrow( "Encryption failed", e );
			
			return( null );
		}
	}
	
	protected cryptoResult
	decrypt(
		BuddyPluginBuddy	buddy,
		byte[]				content,
		String forName)
	
		throws BuddyPluginException
	{
		
		try{
			final byte[] decrypted = ecc_handler.decrypt( buddy.getRawPublicKey(), content, "Decrypting message for " + buddy.getName());
			
			final Map	map = BDecoder.decode( decrypted );
			
			return( 
				new cryptoResult()
				{
					public byte[]
		    		getChallenge()
					{
						return((byte[])map.get("h"));
					}
		    		
		    		public byte[]
		    		getPayload()
		    		{
		    			return((byte[])map.get("p"));
		    		}
				});
			
		}catch( Throwable e ){
			
			rethrow( "Decryption failed", e );
			
			return( null );
		}
	}

	public cryptoResult
	decrypt(
		String				public_key,
		byte[]				content )
	
		throws BuddyPluginException
	{
		
		try{
			final byte[] decrypted = ecc_handler.decrypt( Base32.decode(public_key), content, "Decrypting message for " + public_key);
			
			final Map	map = BDecoder.decode( decrypted );
			
			return( 
				new cryptoResult()
				{
					public byte[]
		    		getChallenge()
					{
						return((byte[])map.get("h"));
					}
		    		
		    		public byte[]
		    		getPayload()
		    		{
		    			return((byte[])map.get("p"));
		    		}
				});
			
		}catch( Throwable e ){
			
			rethrow( "Decryption failed", e );
			
			return( null );
		}
	}

	protected void
	setMessagePending(
		BuddyPluginBuddy			buddy,
		final operationListener		listener )
	
		throws BuddyPluginException
	{		
		try{
			checkAvailable();

			final String	reason = "Friend YGM write for " + buddy.getName();
			
			Map	payload = new HashMap();
			
			payload.put( "r", new Long( random.nextLong()));
			
			byte[] signed_payload = signAndInsert( payload, reason);
			
			Map	envelope = new HashMap();
			
			envelope.put( "pk", ecc_handler.getPublicKey( reason ));
			envelope.put( "ss", signed_payload );
			
			DistributedDatabaseValue	value = ddb.createValue( BEncoder.encode( envelope ));
										
			logMessage( reason + " starts: " + payload );
			
			DistributedDatabaseKey	key = getYGMKey( buddy.getRawPublicKey(), reason );

			ddb.write(
				new DistributedDatabaseListener()
				{
					public void
					event(
						DistributedDatabaseEvent		event )
					{
						int	type = event.getType();
					
						if ( 	type == DistributedDatabaseEvent.ET_OPERATION_TIMEOUT ||
								type == DistributedDatabaseEvent.ET_OPERATION_COMPLETE ){
							
							logMessage( reason + " complete"  );

							listener.complete();
						}
					}
				},
				key,
				value );

		}catch( Throwable e ){
			
			try{				
				rethrow( "Failed to publish YGM", e );
								
			}finally{
				
				listener.complete();
			}
		}
	}
	
	public void
	checkMessagePending(
		int	tick_count )
	{
		log( "Checking YGM" );

		if ( tick_count % YGM_BLOOM_LIFE_TICKS == 0 ){
			
			synchronized( this ){
				
				ygm_unauth_bloom = null;
			}
		}

		try{	
			String	reason = "Friend YGM check";
			
			byte[] public_key = ecc_handler.getPublicKey( reason );

			DistributedDatabaseKey	key = getYGMKey( public_key, reason );
			
			ddb.read(
				new DistributedDatabaseListener()
				{	
					private List		new_ygm_buddies = new ArrayList();
					private boolean	 	unauth_permitted = false;;
					
					public void
					event(
						DistributedDatabaseEvent		event )
					{
						int	type = event.getType();
						
						if ( type == DistributedDatabaseEvent.ET_VALUE_READ ){
							
							try{
								DistributedDatabaseValue value = event.getValue();
																
								byte[]	envelope = (byte[])value.getValue( byte[].class );
								
								Map	map = BDecoder.decode( envelope );
								
								byte[]	pk = (byte[])map.get( "pk" );
								
								if ( pk == null ){
									
									return;
								}
								
								String	pk_str = Base32.encode( pk );
								
								BuddyPluginBuddy buddy = getBuddyFromPublicKey( pk_str );
																
								if ( buddy == null || !buddy.isAuthorised() ){
									
									if ( buddy == null ){
									
										log( "YGM entry from unknown friend '" + pk_str + "' - ignoring" );
										
									}else{								
									
										log( "YGM entry from unauthorised friend '" + pk_str + "' - ignoring" );
									}

									byte[] address = event.getContact().getAddress().getAddress().getAddress();
									
									synchronized( BuddyPlugin.this ){
									
										if ( ygm_unauth_bloom == null ){
										
											ygm_unauth_bloom = BloomFilterFactory.createAddOnly(512);
										}
										
										if ( !ygm_unauth_bloom.contains( address )){
											
											ygm_unauth_bloom.add( address );
											
											unauth_permitted = true;
										}
									}
								}else{
									
									byte[]	signed_stuff = (byte[])map.get( "ss" );
									
									Map	payload = verifyAndExtract( signed_stuff, pk );
									
									if ( payload != null ){
										
										long	rand = ((Long)payload.get("r")).longValue();
										
										if ( buddy.addYGMMarker( rand )){
											
											new_ygm_buddies.add( buddy );
										}
									}
								}
							}catch( Throwable e ){
								
								log( "Read failed", e );
							}
						}else if ( 	type == DistributedDatabaseEvent.ET_OPERATION_TIMEOUT ||
									type == DistributedDatabaseEvent.ET_OPERATION_COMPLETE ){
							
							if ( new_ygm_buddies.size() > 0 || unauth_permitted ){
								
								BuddyPluginBuddy[] b = new BuddyPluginBuddy[new_ygm_buddies.size()];
								
								new_ygm_buddies.toArray( b );
								
								fireYGM( b );
							}
						}
					}
				},
				key,
				120*1000,
				DistributedDatabase.OP_EXHAUSTIVE_READ );
			
			boolean	write_bogus_ygm = false;
			
			synchronized( this ){
			
				if ( !bogus_ygm_written ){
					
					bogus_ygm_written = write_bogus_ygm = true;
				}
			}
			
			if ( write_bogus_ygm ){
				
				final String	reason2 = "Friend YGM write for myself";
				
				Map	envelope = new HashMap();
								
				DistributedDatabaseValue	value = ddb.createValue( BEncoder.encode( envelope ));
											
				logMessage( reason2 + " starts" );
				
				ddb.write(
					new DistributedDatabaseListener()
					{
						public void
						event(
							DistributedDatabaseEvent		event )
						{
							int	type = event.getType();
						
							if ( type == DistributedDatabaseEvent.ET_OPERATION_COMPLETE ){
								
								logMessage( reason2 + " complete"  );
							}
						}
					},
					key,
					value );
			}
			
		}catch( Throwable e ){
						
			logMessage( "YGM check failed", e );
		}
	}
	
	public BuddyPluginBuddy
	getBuddyFromPublicKey(
		String		key )
	{
		synchronized( this ){
			
			return((BuddyPluginBuddy)buddies_map.get( key ));
		}
	}
	
	public PluginInterface
	getPluginInterface()
	{
		return( plugin_interface );
	}
	
	protected SESecurityManager
	getSecurityManager()
	{
		return( sec_man );
	}
	
	protected GenericMessageRegistration
	getMessageRegistration()
	{
		return( msg_registration );
	}
	
		/**
		 * Returns authorised buddies only
		 */
	
	public List
	getBuddies()
	{
		synchronized( this ){
			
			List	result = new ArrayList();
			
			for (int i=0;i<buddies.size();i++){
				
				BuddyPluginBuddy	buddy = (BuddyPluginBuddy)buddies.get(i);
				
				if ( buddy.isAuthorised()){
					
					result.add( buddy );
				}
			}
			
			return( result );
		}
	}
	
	protected List
	getAllBuddies()
	{
		synchronized( this ){
			
			return( new ArrayList( buddies ));
		}
	}
	
	public boolean
	isAvailable()
	{
		try{
			checkAvailable();
			
			return( true );
			
		}catch( Throwable e ){
			
			return( false );
		}
	}
	
	protected void
	checkAvailable()
	
		throws BuddyPluginException
	{
		if ( initialisation_state == INIT_UNKNOWN ){
			
			throw( new BuddyPluginException( "Plugin not yet initialised" ));
			
		}else if ( initialisation_state == INIT_BAD ){
			
			throw( new BuddyPluginException( "Plugin unavailable" ));

		}
	}
	

	protected void
	fireInitialised(
		boolean		ok )
	{
		if ( ok ){
			
			initialisation_state = INIT_OK;
				
		}else{
			
			initialisation_state = INIT_BAD;
		}
		      
		persistentDispatchInit();
		
		if ( ok ){
			
			buddy_tracker.initialise();
		}
		
		List	 listeners_ref = listeners.getList();
		
		for (int i=0;i<listeners_ref.size();i++){

			try{
				((BuddyPluginListener)listeners_ref.get(i)).initialised( ok );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}	
	}
	
	public void
	addListener(
		BuddyPluginListener	listener )
	{
		if ( listeners.contains(listener) ){
			return;
		}

		listeners.add( listener );
		
		if ( initialisation_state != INIT_UNKNOWN ){
			
			listener.initialised( initialisation_state == INIT_OK );
		}
	}
	
	public void
	removeListener(
		BuddyPluginListener	listener )
	{
		listeners.remove( listener );
	}
	
	protected Map
	requestReceived(
		BuddyPluginBuddy		from_buddy,
		int						subsystem,
		Map						content )
	
		throws BuddyPluginException
	{
		List	 listeners_ref = request_listeners.getList();
		
		for (int i=0;i<listeners_ref.size();i++){
			
			try{
				Map reply = ((BuddyPluginBuddyRequestListener)listeners_ref.get(i)).requestReceived(from_buddy, subsystem, content);
				
				if ( reply != null ){
					
					return( reply );
				}
			}catch( BuddyPluginException e ){
				
				throw( e );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
				
				throw( new BuddyPluginException( "Request processing failed", e ));
			}
		}
		
		return( null );
	}
	
	protected void
   	fireAdded(
   		BuddyPluginBuddy		buddy )
   	{
   		List	 listeners_ref = listeners.getList();
   		
   		for (int i=0;i<listeners_ref.size();i++){
   			
   			try{
   				((BuddyPluginListener)listeners_ref.get(i)).buddyAdded( buddy );
 
   			}catch( Throwable e ){
   				
   				Debug.printStackTrace( e );
   			}
   		}
   	}
	
	protected void
   	fireRemoved(
   		BuddyPluginBuddy		buddy )
   	{
   		List	 listeners_ref = listeners.getList();
   		
   		for (int i=0;i<listeners_ref.size();i++){
   			
   			try{
   				((BuddyPluginListener)listeners_ref.get(i)).buddyRemoved( buddy );
 
   			}catch( Throwable e ){
   				
   				Debug.printStackTrace( e );
   			}
   		}
   	}
	
	protected void
   	fireDetailsChanged(
   		BuddyPluginBuddy		buddy )
   	{
   		List	 listeners_ref = listeners.getList();
   		
   		for (int i=0;i<listeners_ref.size();i++){
   			
   			try{
   				((BuddyPluginListener)listeners_ref.get(i)).buddyChanged( buddy );
 
   			}catch( Throwable e ){
   				
   				Debug.printStackTrace( e );
   			}
   		}
   	}
	
	protected void
   	fireYGM(
   		BuddyPluginBuddy[]		from_buddies )
   	{
   		List	 listeners_ref = request_listeners.getList();
   		
   		for (int i=0;i<listeners_ref.size();i++){
   			
   			try{
   				((BuddyPluginBuddyRequestListener)listeners_ref.get(i)).pendingMessages( from_buddies );
 
   			}catch( Throwable e ){
   				
   				Debug.printStackTrace( e );
   			}
   		}
   	}

	protected void
 	fireEnabledStateChanged()
 	{
		final boolean enabled = !plugin_interface.isDisabled() && isEnabled();

 		List	 listeners_ref = listeners.getList();
 		
 		for (int i=0;i<listeners_ref.size();i++){
 			
 			try{
 				((BuddyPluginListener)listeners_ref.get(i)).enabledStateChanged( enabled );

 			}catch( Throwable e ){
 				
 				Debug.printStackTrace( e );
 			}
 		}
 	}
	
	protected void
	rethrow(
		String		reason,
		Throwable	e )
	
		throws BuddyPluginException
	{
		logMessage( reason, e );

		if ( e instanceof CryptoManagerPasswordException ){
		
		
			throw( new BuddyPluginPasswordException(((CryptoManagerPasswordException)e).wasIncorrect(), reason, e ));
		
		}else{
		
			throw( new BuddyPluginException( reason, e ));
		}
	}
	
	public void
	addRequestListener(
		BuddyPluginBuddyRequestListener	listener )
	{
		request_listeners.add( listener );
	}
	
	public void
	removeRequestListener(
		BuddyPluginBuddyRequestListener	listener )
	{
		request_listeners.remove( listener );
	}
	
	public void
	logMessage(
		String		str,
		Throwable	e )
	{
		logMessage( str + ": " + Debug.getNestedExceptionMessage(e));
	}
	
	public void
	logMessage(
		String		str )
	{
		log( str );
		
		Iterator it = listeners.iterator();
		
		while( it.hasNext()){
			
			try{
				((BuddyPluginListener)it.next()).messageLogged( str );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}
	}
	
	public void
	log(
		String		str )
	{
		logger.log( str );
	}
	
	public void
	log(
		String		str,
		Throwable	e )
	{
		logger.log( str + ": " + Debug.getNestedExceptionMessageAndStack( e ));
	}

	private class
	publishDetails
		implements Cloneable
	{
		private byte[]			public_key;
		private InetAddress		ip;
		private int				tcp_port;
		private int				udp_port;
		private String			nick_name;
		
		private boolean			enabled;
		private boolean			published;
		
		private int				sequence;
		
		protected publishDetails
		getCopy()
		{
			try{
				publishDetails copy = (publishDetails)clone();
				
				copy.published = false;
				
				return( copy );
				
			}catch( Throwable e ){
				
				return( null);
			}
		}
		
		protected boolean
		isPublished()
		{
			return( published );
		}
		
		protected void
		setPublished(
			boolean		b )
		{
			published	= b;
		}
		
		protected boolean
		isEnabled()
		{
			return( enabled );
		}
		
		protected void
		setEnabled(
			boolean	_enabled )
		{
			enabled	= _enabled;
		}
		
		protected void
		setSequence(
			int		seq )
		{
			sequence = seq;
		}
		
		protected int
		getSequence()
		{
			return( sequence );
		}
		
		protected byte[]
		getPublicKey()
		{
			return( public_key );
		}
		
		protected void
		setPublicKey(
			byte[]		k )
		{
			public_key	= k;
		}
		
		protected InetAddress
		getIP()
		{
			return( ip );
		}
		
		protected void
		setIP(
			InetAddress	_ip )
		{
			ip	= _ip;
		}
		
		protected int
		getTCPPort()
		{
			return( tcp_port );
		}
		
		protected void
		setTCPPort(
			int		_port )
		{
			tcp_port = _port;
		}
		
		protected int
		getUDPPort()
		{
			return( udp_port );
		}
		
		protected void
		setUDPPort(
			int		_port )
		{
			udp_port = _port;
		}
		
		protected String
		getNickName()
		{
			return( nick_name );
		}
		
		protected void
		setNickName(
			String		 n )
		{
			nick_name	= n;
		}
		
		protected String
		getString()
		{
			return( "enabled=" + enabled + ",ip=" + ip + ",tcp=" + tcp_port + ",udp=" + udp_port + ",key=" + (public_key==null?"<none>":Base32.encode( public_key )));
		}
	}
	
	protected interface
	operationListener
	{
		public void
		complete();
	}
	
	public interface
	cryptoResult
	{
		public byte[]
		getChallenge();
		
		public byte[]
		getPayload();
	}
}
