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
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SHA1Simple;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.ddb.DistributedDatabase;
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
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.config.ActionParameter;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
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
import org.gudy.azureus2.plugins.utils.UTTimerEvent;
import org.gudy.azureus2.plugins.utils.UTTimerEventPerformer;
import org.gudy.azureus2.plugins.utils.security.SEPublicKey;
import org.gudy.azureus2.plugins.utils.security.SEPublicKeyLocator;
import org.gudy.azureus2.plugins.utils.security.SESecurityManager;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

import com.aelitis.azureus.core.security.CryptoHandler;
import com.aelitis.azureus.core.security.CryptoManagerFactory;
import com.aelitis.azureus.core.security.CryptoManagerKeyChangeListener;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.plugins.net.buddy.swt.BuddyPluginView;

public class 
BuddyPlugin 
	implements Plugin
{
	public static final int	SUBSYSTEM_INTERNAL	= 0;
	public static final int	SUBSYSTEM_AZ2		= 1;
	public static final int	SUBSYSTEM_AZ3		= 2;
	
	protected static final int RT_INTERNAL_REQUEST_PING		= 1;
	protected static final int RT_INTERNAL_REPLY_PING		= 2;

	
	public static final int RT_AZ2_REQUEST_MESSAGE		= 1;
	public static final int RT_AZ2_REPLY_MESSAGE		= 2;
	
	public static final int RT_AZ2_REQUEST_SEND_TORRENT	= 3;
	public static final int RT_AZ2_REPLY_SEND_TORRENT	= 4;

	protected static final boolean TRACE = false; 

	private static final String VIEW_ID = "azbuddy";

	private static final int	INIT_UNKNOWN		= 0;
	private static final int	INIT_OK				= 1;
	private static final int	INIT_BAD			= 2;
	
	private static final int	TIMER_PERIOD	= 10*1000;
	
	private static final int	BUDDY_STATUS_CHECK_PERIOD	= 60*1000;
	
	private static final int	STATUS_REPUBLISH_PERIOD		= 5*60*1000;
	private static final int	STATUS_REPUBLISH_TICKS		= STATUS_REPUBLISH_PERIOD/TIMER_PERIOD;

	private static final int	CHECK_YGM_PERIOD			= 5*60*1000;
	private static final int	CHECK_YGM_TICKS				= CHECK_YGM_PERIOD/TIMER_PERIOD;
	
	private static final int	SAVE_CONFIG_PERIOD			= 60*1000;
	private static final int	SAVE_CONFIG_TICKS			= SAVE_CONFIG_PERIOD/TIMER_PERIOD;

	private volatile int	 initialisation_state = INIT_UNKNOWN;
	
	private PluginInterface	plugin_interface;
	
	private LoggerChannel	logger;
	
	private ActionParameter add_buddy_button;

			
	private boolean			ready_to_publish;
	private publishDetails	current_publish		= new publishDetails();
	private publishDetails	latest_publish		= current_publish;
	
	
	private AsyncDispatcher	publish_dispatcher = new AsyncDispatcher();
	
	private	DistributedDatabase 	ddb;
	
	private CryptoHandler ecc_handler = CryptoManagerFactory.getSingleton().getECCHandler();

	private List	buddies 	= new ArrayList();
	private Map		buddies_map	= new HashMap();
	
	private boolean	is_enabled;

	private CopyOnWriteList		listeners 			= new CopyOnWriteList();
	private CopyOnWriteList		request_listeners	= new CopyOnWriteList(); 
		
	private SESecurityManager	sec_man;

	private GenericMessageRegistration	msg_registration;
		
	private boolean		config_dirty;
	
	private Random	random = new SecureRandom();
	
	private BuddyPluginAZ2		az2_handler;
	
	public void
	initialize(
		final PluginInterface		_plugin_interface )
	{
		plugin_interface	= _plugin_interface;
		
		az2_handler = new BuddyPluginAZ2( this );
		
		String name_res = "Views.plugins." + VIEW_ID + ".title";
		
		String name = 
			plugin_interface.getUtilities().getLocaleUtilities().getLocalisedMessageText( name_res );
		
		plugin_interface.getPluginProperties().setProperty( "plugin.version", 	"1.0" );
		plugin_interface.getPluginProperties().setProperty( "plugin.name", 		name );

		if ( !Constants.isCVSVersion()){
			
			return;
		}
		
		sec_man = plugin_interface.getUtilities().getSecurityManager();

		logger = plugin_interface.getLogger().getChannel( "Buddy" );
		
		logger.setDiagnostic();
				
		BasicPluginConfigModel config = plugin_interface.getUIManager().createBasicPluginConfigModel( name_res );
			
			// enabled

		final BooleanParameter enabled_param = config.addBooleanParameter2( "enabled", "enabled", false );
				
			// nickname

		final StringParameter nick_name_param = config.addStringParameter2( "nickname", "nickname", "" );

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
		
			// add buddy
		
		final StringParameter buddy_pk_param = config.addStringParameter2( "other buddy key", "other buddy key", "" );
		
		buddy_pk_param.addListener(
				new ParameterListener()
				{
					public void
					parameterChanged(
						Parameter	param )
					{
						String	value = buddy_pk_param.getValue().trim();
						
						byte[] bytes = Base32.decode( value );					
						
						add_buddy_button.setEnabled( ecc_handler.verifyPublicKey( bytes )); 
					}
				});
		
		add_buddy_button = config.addActionParameter2( "add the key", "do it!" );
		
		add_buddy_button.setEnabled( false );
		
		add_buddy_button.addListener(
			new ParameterListener()
			{
				public void
				parameterChanged(
					Parameter	param )
				{
					addBuddy( buddy_pk_param.getValue().trim());
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
					
					boolean enabled = download.getTorrent() != null;
					
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
		
		plugin_interface.getUIManager().addUIListener(
			new UIManagerListener()
			{
				public void
				UIAttached(
					UIInstance		instance )
				{
					if ( instance instanceof UISWTInstance ){
						
						UISWTInstance swt_ui = (UISWTInstance)instance;
						
						BuddyPluginView view = new BuddyPluginView( BuddyPlugin.this );

						swt_ui.addView(	UISWTInstance.VIEW_MAIN, VIEW_ID, view );
						
						//swt_ui.openMainView( VIEW_ID, view, null );
					}
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
					
					nick_name_param.setEnabled( enabled );
					buddy_pk_param.setEnabled( enabled );
					add_buddy_button.setEnabled( enabled );
					
						// only toggle overall state on a real change
					
					if ( param != null ){
					
						setEnabled( enabled );
					}
				}
			};
		
		enabled_listener.parameterChanged( null );
			
		enabled_param.addListener( enabled_listener );
		
		loadBuddies();
		
		registerMessageHandler();
		
		plugin_interface.addListener(
			new PluginListener()
			{
				public void
				initializationComplete()
				{
					new AEThread2( "BuddyPlugin:init", true )
					{
						public void
						run()
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
								
								CryptoManagerFactory.getSingleton().addKeyChangeListener(
									new CryptoManagerKeyChangeListener()
									{
										public void 
										keyChanged(
											CryptoHandler handler ) 
										{
											updateKey();
										}
									});
								
								ready_to_publish	= true;
								
								setEnabled( enabled_param.getValue());
								
								checkBuddiesAndRepublish();
								
								fireInitialised( true );
								
							}catch( Throwable e ){
							
								log( "Initialisation failed", e );
								
								fireInitialised( false );
							}
						}
					}.start();
				}
				
				public void
				closedownInitiated()
				{	
					saveBuddies();
				}
				
				public void
				closedownComplete()
				{				
				}
			});
	}
	
	protected void
	setEnabled(
		boolean		_enabled )
	{
		synchronized( this ){
			
			is_enabled	= _enabled;
			
			if ( latest_publish.isEnabled() != _enabled ){
				
				publishDetails new_publish = latest_publish.getCopy();
				
				new_publish.setEnabled( _enabled );
				
				updatePublish( new_publish );
			}
		}
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
							if ( TRACE ){
								System.out.println( "accept" );
							}
							
							try{	
								String reason = "Buddy: Incoming connection establishment";
																
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
													if ( TRACE ){
														System.out.println( "Incoming: acceptKey" );
													}
													
													synchronized( BuddyPlugin.this ){
															
														for (int i=0;i<buddies.size();i++){
														
															BuddyPluginBuddy	buddy = (BuddyPluginBuddy)buddies.get(i);

															if ( buddy.hasPublicKey( other_key )){
																
																buddy.incomingConnection(
																	(GenericMessageConnection)context );	
																
																return( true );
															}
														}
													}
													
													log( "incoming connection failed due to pk mismatch" );

													return( false );
												}
											},
											reason,
											SESecurityManager.BLOCK_ENCRYPTION_AES );
							
							}catch( Throwable e ){
								
								connection.close();
								
								log( "Incoming connection failed", e );
							}
							
							return( true );
						}
					});
					
		}catch( Throwable e ){
			
			log( "Failed to register message listener", e );
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
					getStatusKey( key_to_remove, "Buddy status de-registration for old key" ));
				
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
			
			try{
				byte[] data = BEncoder.encode( payload );
										
				DistributedDatabaseKey	key = getStatusKey( details.getPublicKey(), "My buddy status registration " + payload );
	
				byte[] signature = ecc_handler.sign( data, "Buddy online status" );
			
				byte[]	signed_payload = new byte[ 1 + signature.length + data.length ];
				
				signed_payload[0] = (byte)signature.length;
				
				System.arraycopy( signature, 0, signed_payload, 1, signature.length );
				System.arraycopy( data, 0, signed_payload, 1 + signature.length, data.length );		
				
				DistributedDatabaseValue	value = ddb.createValue( signed_payload );
				
				final AESemaphore	sem = new AESemaphore( "BuddyPlugin:reg" );
				
				if ( log_this ){
					
					logMessage( "Publishing status starts: " + details.getString());
				}
				
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
				
				log( "Failed to publish online status", e );
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
	loadBuddies()
	{
		synchronized( this ){
			
			List buddies_config = readConfig(); 
	
			for (int i=0;i<buddies_config.size();i++){
				
				Object o = buddies_config.get(i);
	
				if ( o instanceof Map ){
					
					Map	details = (Map)o;
					
					String	key = new String((byte[])details.get("pk"));
					
					List	recent_ygm = (List)details.get( "ygm" );
										
					String	nick = decodeString((byte[])details.get( "n" ));
					
					BuddyPluginBuddy buddy = new BuddyPluginBuddy( this, key, nick, recent_ygm );
					
					logMessage( "Loaded buddy " + buddy.getString());
					
					buddies.add( buddy );
					
					buddies_map.put( key, buddy );
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
	saveBuddies()
	{
		synchronized( this ){

			if ( config_dirty ){
				
				List buddies_config = new ArrayList();
		
				for (int i=0;i<buddies.size();i++){
					
					BuddyPluginBuddy buddy = (BuddyPluginBuddy)buddies.get(i);
		
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
					
					buddies_config.add( map );
				}
				
				writeConfig( buddies_config );
				
				config_dirty = false;
			}
		}
	}
	
	protected void
	addBuddy(
		String		key )
	{
		if ( key.length() == 0 ){
			
			return;
		}
				
		BuddyPluginBuddy	new_buddy;
		
		synchronized( this ){
						
			for (int i=0;i<buddies.size();i++){
				
				BuddyPluginBuddy buddy = (BuddyPluginBuddy)buddies.get(i);
				
				if ( buddy.getPublicKey().equals( key )){
					
					return;
				}
			}
			
			new_buddy = new BuddyPluginBuddy( this, key, null, null );
			
			buddies.add( new_buddy );
			
			buddies_map.put( key, new_buddy );
			
			config_dirty	= true;
			
			logMessage( "Added buddy " + new_buddy.getString());

			saveBuddies();
		}
		
		fireAdded( new_buddy );
	}
	
	protected void
	removeBuddy(
		BuddyPluginBuddy 	buddy )
	{
		synchronized( this ){

			if ( !buddies.remove( buddy )){
				
				return;
			}
		
			buddies_map.remove( buddy.getPublicKey());
			
			config_dirty = true;
			
			logMessage( "Removed buddy " + buddy.getString());

			saveBuddies();
		}
		
		fireRemoved( buddy );
	}
	
	protected List
	readConfig()
	{
		File	config_file = new File( plugin_interface.getUtilities().getAzureusUserDir(), "buddies.config" );
		
		Map map = plugin_interface.getUtilities().readResilientBEncodedFile(
				config_file.getParentFile(), config_file.getName(), true );
		
		if ( map != null ){
			
			List	buddies = (List)map.get( "buddies" );
			
			if ( buddies != null ){
				
				return( buddies );
			}
		}
		
		return( new ArrayList());
	}
	
	protected void
	writeConfig(
		List	buddies )
	{
		File	config_file = new File( plugin_interface.getUtilities().getAzureusUserDir(), "buddies.config" );
		
		Map	map = new HashMap();
		
		map.put( "buddies", buddies );
		
		plugin_interface.getUtilities().writeResilientBEncodedFile(
				config_file.getParentFile(), config_file.getName(), map, true );
	}
		
	public BuddyPluginAZ2
	getAZ2Handler()
	{
		return( az2_handler );
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
					
					if ( !is_enabled ){
						
						return;
					}
												
					updateBuddys();
					
					if ( tick_count % STATUS_REPUBLISH_TICKS == 0 ){
							
						if ( latest_publish.isEnabled()){
								
							updatePublish( latest_publish );
						}
					}
					
					if ( tick_count % CHECK_YGM_TICKS == 0 ){

						checkMessagePending();
					}
					
					if ( tick_count % SAVE_CONFIG_TICKS == 0 ){

						saveBuddies();
					}
				}
			});
	}
	
	protected void
	updateBuddys()
	{
		List	buddies_copy;
		
		synchronized( BuddyPlugin.this ){
		
			buddies_copy = new ArrayList( buddies );
		}
		
		long	now = SystemTime.getCurrentTime();
		
		for (int i=0;i<buddies_copy.size();i++){
			
			BuddyPluginBuddy	buddy = (BuddyPluginBuddy)buddies_copy.get(i);
			
			long	last_check = buddy.getLastStatusCheckTime();
			
			buddy.checkTimeouts();
			
			if ( last_check > now || now - last_check > BUDDY_STATUS_CHECK_PERIOD ){
				
				if ( !buddy.statusCheckActive()){
			
					updateBuddyStatus( buddy );
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
				getStatusKey( public_key, "Buddy status check for " + buddy.getName());
			
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
									
									buddy.statusCheckComplete( latest_time, ip, tcp_port, udp_port, nick );
									
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
			
			log( "Buddy status update failed: " + buddy.getString(), e );
		}
	}
	
	protected Map
	verifyAndExtract(
		byte[]		signed_stuff,
		byte[]		public_key )
	
		throws Exception
	{
		int	signature_length = ((int)signed_stuff[0])&0xff;
		
		byte[]	signature 	= new byte[ signature_length ];
		byte[]	data		= new byte[ signed_stuff.length - 1 - signature_length];
		
		System.arraycopy( signed_stuff, 1, signature, 0, signature_length );
		System.arraycopy( signed_stuff, 1 + signature_length, data, 0, data.length );
				
		if ( ecc_handler.verify( public_key, data, signature )){													

			return( BDecoder.decode( data ));
																																
		}else{
			
			log( "Verification failed" );
			
			return( null );
		}
	}
	
	protected byte[]
	signAndInsert(
		Map		plain_stuff,
		String	reason )
	
		throws Exception
	{
		byte[] data = BEncoder.encode( plain_stuff );
		
		byte[] signature = ecc_handler.sign( data, reason );
	
		byte[]	signed_payload = new byte[ 1 + signature.length + data.length ];
		
		signed_payload[0] = (byte)signature.length;
		
		System.arraycopy( signature, 0, signed_payload, 1, signature.length );
		System.arraycopy( data, 0, signed_payload, 1 + signature.length, data.length );		

		return( signed_payload );
	}
	
	protected cryptoResult
	encrypt(
		BuddyPluginBuddy	buddy,
		byte[]				payload )
	
		throws BuddyPluginException
	{
		
		try{
			byte[]	hash = new byte[16];
			
			random.nextBytes( hash );
			
			Map	content = new HashMap();
			
			content.put( "h", hash );
			content.put( "p", payload );
			
			final byte[] encrypted = ecc_handler.encrypt( buddy.getRawPublicKey(), BEncoder.encode( content ), "Encrypting message for " + buddy.getName());
			
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
			
			throw( new BuddyPluginException( "Encryption failed", e ));
		}
	}
	
	protected cryptoResult
	decrypt(
		BuddyPluginBuddy	buddy,
		byte[]				content )
	
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
			
			throw( new BuddyPluginException( "Decryption failed", e ));
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

			final String	reason = "Buddy YGM write for " + buddy.getName();
			
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
				log( "Failed to publish YGM", e );
				
				if ( e instanceof BuddyPluginException ){
					
					throw((BuddyPluginException)e);
				}
				
				throw( new BuddyPluginException( "Failed to publish YGM", e ));
				
			}finally{
				
				listener.complete();
			}
		}
	}
	
	public void
	checkMessagePending()
	{
		log( "Checking YGM" );

		try{	
			String	reason = "Buddy YGM check";
			
			byte[] public_key = ecc_handler.getPublicKey( reason );

			DistributedDatabaseKey	key = getYGMKey( public_key, reason );
			
			ddb.read(
				new DistributedDatabaseListener()
				{	
					private List	new_ygm_buddies = new ArrayList();
					
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
								
								String	pk_str = Base32.encode( pk );
								
								BuddyPluginBuddy buddy = getBuddyFromPublicKey( pk_str );
								
								if ( buddy == null ){
									
									log( "YGM entry from unknown buddy '" + pk_str + "' - ignoring" );
									
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
							
							if ( new_ygm_buddies.size() > 0 ){
								
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
			
			
		}catch( Throwable e ){
						
			log( "YGM check failed", e );
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
	
	public List
	getBuddies()
	{
		synchronized( this ){
			
			return( new ArrayList( buddies ));
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
		logger.log( str );
		logger.log( e );
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
