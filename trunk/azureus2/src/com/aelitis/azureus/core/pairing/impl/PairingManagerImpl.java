/*
 * Created on Oct 5, 2009
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


package com.aelitis.azureus.core.pairing.impl;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.AEVerifier;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DelayedEvent;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.core3.util.TimerEventPeriodic;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerEvent;
import org.gudy.azureus2.plugins.ui.config.ActionParameter;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.HyperlinkParameter;
import org.gudy.azureus2.plugins.ui.config.InfoParameter;
import org.gudy.azureus2.plugins.ui.config.LabelParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.config.StringParameter;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.utils.DelayedTask;
import org.gudy.azureus2.plugins.utils.StaticUtilities;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdmin;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminHTTPProxy;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminNetworkInterface;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminNetworkInterfaceAddress;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSocksProxy;
import com.aelitis.azureus.core.pairing.*;
import com.aelitis.azureus.core.security.CryptoECCUtils;
import com.aelitis.azureus.core.security.CryptoManager;
import com.aelitis.azureus.core.security.CryptoManagerFactory;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.plugins.upnp.UPnPPlugin;
import com.aelitis.azureus.plugins.upnp.UPnPPluginService;
import com.aelitis.net.upnp.UPnPRootDevice;

public class 
PairingManagerImpl
	implements PairingManager
{
	private static final boolean DEBUG	= false;
	
	private static final String	SERVICE_URL;
	
	static{
		String url = System.getProperty( "az.pairing.url", "" );
		
		if ( url.length() == 0 ){
			
			SERVICE_URL = Constants.PAIRING_URL;
			
		}else{
			
			SERVICE_URL = url;
		}
	}
	
	private static final PairingManagerImpl	singleton = new PairingManagerImpl();
	
	public static PairingManager
	getSingleton()
	{
		return( singleton );
	}
	
	private static final int	GLOBAL_UPDATE_PERIOD	= 60*1000;
	private static final int	CD_REFRESH_PERIOD		= 23*60*60*1000;
	private static final int	CD_REFRESH_TICKS		= CD_REFRESH_PERIOD / GLOBAL_UPDATE_PERIOD;
	
	private AzureusCore	azureus_core;
	
	private BooleanParameter 	param_enable;

	
	private InfoParameter		param_ac_info;
	private InfoParameter		param_status_info;
	private HyperlinkParameter	param_view;
	
	private BooleanParameter 	param_e_enable;
	private StringParameter		param_ipv4;
	private StringParameter		param_ipv6;
	private StringParameter		param_host;
	
	private Map<String,PairedServiceImpl>		services = new HashMap<String, PairedServiceImpl>();
	
	private AESemaphore	init_sem = new AESemaphore( "PM:init" );
	
	private TimerEventPeriodic	global_update_event;
	
	private InetAddress		current_v4;
	private InetAddress		current_v6;
	
	private String			local_v4	= "";
	private String			local_v6	= "";
	
	private boolean	update_outstanding;
	private boolean	updates_enabled;

	private static final int MIN_UPDATE_PERIOD_DEFAULT	= 60*1000;
	private static final int MAX_UPDATE_PERIOD_DEFAULT	= 60*60*1000;
		
	private int min_update_period	= MIN_UPDATE_PERIOD_DEFAULT;
	private int max_update_period	= MAX_UPDATE_PERIOD_DEFAULT;
	
	
	private AsyncDispatcher	dispatcher = new AsyncDispatcher();
	
	private boolean			must_update_once;
	private TimerEvent		deferred_update_event;
	private long			last_update_time		= -1;
	private int				consec_update_fails;
	
	private String			last_server_error;
	private String			last_message;
	
	private CopyOnWriteList<PairingManagerListener>	listeners = new CopyOnWriteList<PairingManagerListener>();
	
	protected
	PairingManagerImpl()
	{
		must_update_once = COConfigurationManager.getBooleanParameter( "pairing.updateoutstanding" );

		PluginInterface default_pi = PluginInitializer.getDefaultInterface();
		
		final UIManager	ui_manager = default_pi.getUIManager();
		
		BasicPluginConfigModel configModel = ui_manager.createBasicPluginConfigModel(
				ConfigSection.SECTION_CONNECTION, "Pairing");

		param_enable = configModel.addBooleanParameter2( "pairing.enable", "pairing.enable", false );
		
		String	access_code = readAccessCode();
		
		param_ac_info = configModel.addInfoParameter2( "pairing.accesscode", access_code);
		
		param_status_info = configModel.addInfoParameter2( "pairing.status.info", "" );
		
		param_view = configModel.addHyperlinkParameter2( "pairing.view.registered", SERVICE_URL + "/web/view?ac=" + access_code);

		if ( access_code.length() == 0 ){
			
			param_view.setEnabled( false );
		}
		
		final ActionParameter ap = configModel.addActionParameter2( "pairing.ac.getnew", "pairing.ac.getnew.create" );
		
		ap.addListener(
			new ParameterListener()
			{
				public void 
				parameterChanged(
					Parameter 	param ) 
				{
					try{
						ap.setEnabled( false );
						
						allocateAccessCode( false );
						
						SimpleTimer.addEvent(
							"PM:enabler",
							SystemTime.getOffsetTime(30*1000),
							new TimerEventPerformer()
							{
								public void 
								perform(
									TimerEvent event ) 
								{
									ap.setEnabled( true );
								}
							});
						
					}catch( Throwable e ){
						
						ap.setEnabled( true );
						
						String details = MessageText.getString(
								"pairing.alloc.fail",
								new String[]{ Debug.getNestedExceptionMessage( e )});
						
						ui_manager.showMessageBox(
								"pairing.op.fail",
								"!" + details + "!",
								UIManagerEvent.MT_OK );
					}
				}
			});
		
		LabelParameter	param_e_info = configModel.addLabelParameter2( "pairing.explicit.info" );
		
		param_e_enable = configModel.addBooleanParameter2( "pairing.explicit.enable", "pairing.explicit.enable", false );
		
		param_ipv4	= configModel.addStringParameter2( "pairing.ipv4", "pairing.ipv4", "" );
		param_ipv6	= configModel.addStringParameter2( "pairing.ipv6", "pairing.ipv6", "" );
		param_host	= configModel.addStringParameter2( "pairing.host", "pairing.host", "" );
		
		param_ipv4.setGenerateIntermediateEvents( false );
		param_ipv6.setGenerateIntermediateEvents( false );
		param_host.setGenerateIntermediateEvents( false );
		
		ParameterListener change_listener = 
			new ParameterListener()
			{
				public void 
				parameterChanged(
					Parameter param )
				{
					updateNeeded();
					
					if ( param == param_enable ){
						
						fireChanged();
					}
				}
			};
			
		param_enable.addListener( change_listener );
		param_e_enable.addListener(	change_listener );
		param_ipv4.addListener(	change_listener );
		param_ipv6.addListener(	change_listener );
		param_host.addListener(	change_listener );
		
		param_e_enable.addEnabledOnSelection( param_ipv4 );
		param_e_enable.addEnabledOnSelection( param_ipv6 );
		param_e_enable.addEnabledOnSelection( param_host );
		
		configModel.createGroup(
			"pairing.group.explicit",
			new Parameter[]{
				param_e_info,
				param_e_enable,
				param_ipv4,	
				param_ipv6,
				param_host,
			});
		
		AzureusCoreFactory.addCoreRunningListener(
			new AzureusCoreRunningListener()
			{
				public void 
				azureusCoreRunning(
					AzureusCore core )
				{
					initialise( core );
				}
			});
	}
	
	
	protected void
	initialise(
		AzureusCore		_core )
	{
		synchronized( this ){
			
			azureus_core	= _core;
		}
		
		try{
			PluginInterface default_pi = PluginInitializer.getDefaultInterface();

			DelayedTask dt = default_pi.getUtilities().createDelayedTask(
				new Runnable()
				{
					public void 
					run() 
					{
						new DelayedEvent( 
							"PM:delayinit",
							30*1000,
							new AERunnable()
							{
								public void
								runSupport()
								{
									enableUpdates();
								}
							});
					}
				});
			
			dt.queue();
			
		}finally{
			
			init_sem.releaseForever();
		}
	}
	
	protected void
	waitForInitialisation()
	
		throws PairingException
	{
		if ( !init_sem.reserve( 30*1000 )){
		
			throw( new PairingException( "Timeout waiting for initialisation" ));
		}
	}
	
	public boolean
	isEnabled()
	{
		return( param_enable.getValue());
	}
	
	public void
	setEnabled(
		boolean	enabled )
	{
		param_enable.setValue( enabled );
	}
	
	protected void
	setStatus(
		String		str )
	{
		param_status_info.setValue( str );
	}
	
	public String
	getStatus()
	{
		return( param_status_info.getValue());
	}
	
	public String
	getLastServerError()
	{
		return( last_server_error );
	}

	protected String
	readAccessCode()
	{
		return( COConfigurationManager.getStringParameter( "pairing.accesscode", "" ));
	}
	
	protected void
	writeAccessCode(
		String		ac )
	{
		COConfigurationManager.setParameter( "pairing.accesscode", ac );
		 
		param_ac_info.setValue( ac );
		 
		param_view.setHyperlink( SERVICE_URL + "/web/view?ac=" + ac );
				
		param_view.setEnabled( ac.length() > 0 );
	}
	
	protected String
	allocateAccessCode(
		boolean		updating )
	
		throws PairingException
	{
		Map<String,Object>	request = new HashMap<String, Object>();
		
		String existing = readAccessCode();
		
		request.put( "ac", existing );
		
		Map<String,Object> response = sendRequest( "allocate", request );
		
		try{
			String code = getString( response, "ac" );
			
			writeAccessCode( code );
				
			if ( !updating ){
			
				updateNeeded();
			}
			
			return( code );
			
		}catch( Throwable e ){
			
			throw( new PairingException( "allocation failed", e ));
		}
	}

	public String
	getAccessCode()
	
		throws PairingException
	{
		waitForInitialisation();
		
		String ac = readAccessCode();
		
		if ( ac == null || ac.length() == 0 ){
			
			ac = allocateAccessCode( false );
		}
		
		return( ac );
	}
	
	public void
	getAccessCode(
		final PairingManagerListener 	listener )
	
		throws PairingException
	{
		new AEThread2( "PM:gac", true )
		{
			public void
			run()
			{
				try{
					getAccessCode();
					
				}catch( Throwable e ){
					
				}finally{
					
					listener.somethingChanged( PairingManagerImpl.this );
				}
			}
		}.start();
	}
	
	public String
	getReplacementAccessCode()
	
		throws PairingException
	{
		waitForInitialisation();
		
		String new_code = allocateAccessCode( false );
		
		return( new_code );
	}
	
	public PairedService
	addService(
		String		sid )
	{
		synchronized( this ){
						
			PairedServiceImpl	result = services.get( sid );
			
			if ( result == null ){
				
				if ( DEBUG ){
					System.out.println( "PS: added " + sid );
				}
				
				result = new PairedServiceImpl( sid );
				
				services.put( sid, result );
			}
			
			return( result );
		}
	}
	
	public PairedService
	getService(
		String		sid )
	{
		synchronized( this ){
			
			PairedService	result = services.get( sid );
			
			return( result );
		}
	}
	
	protected void
	remove(
		PairedServiceImpl	service )
	{
		synchronized( this ){

			String sid = service.getSID();
			
			if ( services.remove( sid ) != null ){
				
				if ( DEBUG ){
					System.out.println( "PS: removed " + sid );
				}
			}
		}
		
		updateNeeded();
	}
	
	protected void
	sync(
		PairedServiceImpl	service )
	{
		updateNeeded();
	}
	
	protected InetAddress
	updateAddress(
		InetAddress		current,
		InetAddress		latest,
		boolean			v6 )
	{
		if ( v6 ){
			
			if ( latest instanceof Inet4Address ){
				
				return( current );
			}
		}else{
			
			if ( latest instanceof Inet6Address ){
				
				return( current );
			}
		}
		
		if ( current == latest ){
			
			return( current );
		}
		
		if ( current == null || latest == null ){
			
			return( latest );
		}
		
		if ( !current.equals( latest )){
			
			return( latest );
		}
		
		return( current );
	}
	
	protected void
	updateGlobals(
		boolean	is_updating )	
	{
		synchronized( this ){
						
			NetworkAdmin network_admin = NetworkAdmin.getSingleton();
					
			InetAddress latest_v4 = azureus_core.getInstanceManager().getMyInstance().getExternalAddress();
			
			InetAddress temp_v4 = updateAddress( current_v4, latest_v4, false );
			
			InetAddress latest_v6 = network_admin.getDefaultPublicAddressV6();
	
			InetAddress temp_v6 = updateAddress( current_v6, latest_v6, true );
			
			TreeSet<String>	latest_v4_locals = new TreeSet<String>();
			TreeSet<String>	latest_v6_locals = new TreeSet<String>();
			
			NetworkAdminNetworkInterface[] interfaces = network_admin.getInterfaces();
			
			for ( NetworkAdminNetworkInterface intf: interfaces ){
				
				NetworkAdminNetworkInterfaceAddress[] addresses = intf.getAddresses();
				
				for ( NetworkAdminNetworkInterfaceAddress address: addresses ){
					
					InetAddress ia = address.getAddress();
					
					if ( ia.isLoopbackAddress()){
						
						continue;
					}
					
					if ( ia.isLinkLocalAddress() || ia.isSiteLocalAddress()){
						
						if ( ia instanceof Inet4Address ){
							
							latest_v4_locals.add( ia.getHostAddress());
							
						}else{
							
							latest_v6_locals.add( ia.getHostAddress());
						}
					}
				}
			}
			
			String v4_locals_str = getString( latest_v4_locals );
			String v6_locals_str = getString( latest_v6_locals );
			
			
			if (	temp_v4 != current_v4 ||
					temp_v6 != current_v6 ||
					!v4_locals_str.equals( local_v4 ) ||
					!v6_locals_str.equals( local_v6 )){
				
				current_v4	= temp_v4;
				current_v6	= temp_v6;
				local_v4	= v4_locals_str;
				local_v6	= v6_locals_str;
				
				if ( !is_updating ){
				
					updateNeeded();
				}
			}

		}
	}
	
	protected String
	getString(
		Set<String>	set )
	{
		String	str = "";
		
		for ( String s: set ){
			
			str += (str.length()==0?"":",") + s;
		}
		
		return( str );
	}
	
	protected void
	enableUpdates()
	{		
		synchronized( this ){
			
			updates_enabled = true;

			if ( update_outstanding ){
				
				update_outstanding = false;
				
				updateNeeded();
			}
		}
	}
	
	protected void
	updateNeeded()
	{
		if ( DEBUG ){
			System.out.println( "PS: updateNeeded" );
		}
		
		synchronized( this ){
			
			if ( updates_enabled ){
				
				dispatcher.dispatch(
					new AERunnable()
					{
						public void
						runSupport()
						{
							doUpdate();
						}
					});
						
				
			}else{
				
				setStatus( MessageText.getString( "pairing.status.initialising" ));
				
				update_outstanding	= true;
			}
		}
	}
	
	protected void
	doUpdate()
	{
		long	now = SystemTime.getMonotonousTime();

		synchronized( this ){
			
			if ( deferred_update_event != null ){
				
				return;
			}
			
			long	time_since_last_update = now - last_update_time;
			
			if ( last_update_time > 0 &&  time_since_last_update < min_update_period ){
				
				deferUpdate(  min_update_period - time_since_last_update  );
				
				return;
			}
		}
		
		try{
			Map<String,Object>	payload = new HashMap<String, Object>();
						
			boolean	is_enabled = param_enable.getValue();
			
			synchronized( this ){
				
				List<Map<String,String>>	list =  new ArrayList<Map<String,String>>();
				
				payload.put( "s", list );
				
				if ( services.size() > 0 && is_enabled ){
					
					if ( global_update_event == null ){
						
						global_update_event = 
							SimpleTimer.addPeriodicEvent(
							"PM:updater",
							GLOBAL_UPDATE_PERIOD,
							new TimerEventPerformer()
							{
								private int	tick_count;
								
								public void 
								perform(
									TimerEvent event ) 
								{
									tick_count++;
									
									updateGlobals( false );
									
									if ( tick_count % CD_REFRESH_TICKS == 0 ){
										
										updateNeeded();
									}
								}
							});
						
						updateGlobals( true );
					}
					
					for ( PairedServiceImpl service: services.values()){
						
						list.add( service.toMap());
					}
				}else{
					
						// when we get to zero services we want to push through the
						// last update to remove cd
					
					if ( global_update_event == null ){
						
						if ( consec_update_fails == 0 && !must_update_once ){
					
							setStatus( MessageText.getString( "pairing.status.disabled" ));
							
							return;
						}
					}else{
					
						global_update_event.cancel();
					
						global_update_event = null;
					}
				}
				
				last_update_time = now;
			}
			
				// we need a valid access code here!
			
			String ac = readAccessCode();
			
			if ( ac.length() == 0 ){
				
				ac = allocateAccessCode( true );			
			}
			
			payload.put( "ac", ac );
			
			synchronized( this ){

				if ( current_v4 != null ){
				
					payload.put( "c_v4", current_v4.getHostAddress());
				}
				
				if ( current_v6 != null ){
					
					payload.put( "c_v6", current_v6.getHostAddress());
				}
			
				if ( local_v4.length() > 0 ){
					
					payload.put( "l_v4", local_v4 );
				}
				
				if ( local_v6.length() > 0 ){
					
					payload.put( "l_v6", local_v6 );
				}
				
				if ( param_e_enable.getValue()){
				
					String host = param_host.getValue().trim();
					
					if ( host.length() > 0 ){
						
						payload.put( "e_h", host );
					}
					
					String v4 = param_ipv4.getValue().trim();
					
					if ( v4.length() > 0 ){
						
						payload.put( "e_v4", v4 );
					}
					
					String v6 = param_ipv6.getValue().trim();
					
					if ( v6.length() > 0 ){
						
						payload.put( "e_v4", v6 );
					}
				}
				
					// grab some UPnP info for diagnostics
				
				try{
				    PluginInterface pi_upnp = azureus_core.getPluginManager().getPluginInterfaceByClass( UPnPPlugin.class );

				    if ( pi_upnp != null ){
				    	
				        UPnPPlugin upnp = (UPnPPlugin)pi_upnp.getPlugin();

				        if ( upnp.isEnabled()){
				        	
				        	List<Map<String,String>>	upnp_list = new ArrayList<Map<String,String>>();
				        	
				        	payload.put( "upnp", upnp_list );
				        	
				        	UPnPPluginService[] services = upnp.getServices();
				        	
				        	Set<UPnPRootDevice> devices = new HashSet<UPnPRootDevice>();
				        	
				        	for ( UPnPPluginService service: services ){
				        		
				        		UPnPRootDevice root_device = service.getService().getGenericService().getDevice().getRootDevice();
				        		
				        		if ( !devices.contains( root_device )){
				        			
				        			devices.add( root_device );
				        	
					        		Map<String,String>	map = new HashMap<String, String>();
					        	
					        		upnp_list.add( map );
					        		
					        		map.put( "i", root_device.getInfo());
				        		}
				        	}
				        }
				    }
				}catch( Throwable e ){					
				}
				
				try{
					NetworkAdmin admin = NetworkAdmin.getSingleton();
					
					NetworkAdminHTTPProxy http_proxy = admin.getHTTPProxy();
					
					if ( http_proxy != null ){
						
						payload.put( "hp", http_proxy.getName());
					}
					
					NetworkAdminSocksProxy[] socks_proxies = admin.getSocksProxies();
					
					if ( socks_proxies.length > 0 ){
						
						payload.put( "sp", socks_proxies[0].getName());
					}
				}catch( Throwable e ){	
				}
				
				payload.put( "_enabled", is_enabled?1L:0L );
			}
			
			if ( DEBUG ){
				System.out.println( "PS: doUpdate: " + payload );
			}
			
			sendRequest( "update", payload );
			
			synchronized( this ){

				consec_update_fails	= 0;
				
				must_update_once = false;
				
				if ( deferred_update_event == null ){
										
					COConfigurationManager.setParameter( "pairing.updateoutstanding", false );
				}

				if ( global_update_event == null ){
					
					setStatus( MessageText.getString( "pairing.status.disabled" ));
					
				}else{
					
					setStatus( 
						MessageText.getString( 
							"pairing.status.registered", 
							new String[]{ new SimpleDateFormat().format(new Date( SystemTime.getCurrentTime() ))}));
				}
			}
		}catch( Throwable e ){
			
			synchronized( this ){
				
				consec_update_fails++;
	
				long back_off = min_update_period;
				
				for (int i=0;i<consec_update_fails;i++){
					
					back_off *= 2;
					
					if ( back_off > max_update_period ){
					
						back_off = max_update_period;
						
						break;
					}
				}
				
				deferUpdate( back_off );
			}
		}
	}
	
	protected void
	deferUpdate(
		long	millis )
	{
		millis += 5000;
		
		long target = SystemTime.getOffsetTime( millis );
		
		setStatus( 
			MessageText.getString( 
				"pairing.status.pending", 
				new String[]{ new SimpleDateFormat().format(new Date( target ))}));

		COConfigurationManager.setParameter( "pairing.updateoutstanding", true );
		
		deferred_update_event = 
			SimpleTimer.addEvent(
				"PM:defer",
				target,
				new TimerEventPerformer()
				{
					public void 
					perform(
						TimerEvent event )
					{
						synchronized( PairingManagerImpl.this ){
							
							deferred_update_event = null;
						}
						
						COConfigurationManager.setParameter( "pairing.updateoutstanding", false );
						
						updateNeeded();
					}
				});
	}
	
	
	private Map<String, Object> 
	sendRequest(
		String 				command,
		Map<String, Object> payload )
		
		throws PairingException
	{
		try{
			Map<String, Object> request = new HashMap<String, Object>();

			CryptoManager cman = CryptoManagerFactory.getSingleton();

			String azid = Base32.encode( cman.getSecureID());

			payload.put( "_azid", azid );

			try{
				String pk = Base32.encode( cman.getECCHandler().getPublicKey( "pairing" ));

				payload.put( "_pk", pk );
				
			}catch( Throwable e ){	
			}
			
			request.put( "req", payload );
			
			String request_str = Base32.encode( BEncoder.encode( request ));
			
			String	sig = null;
			
			try{
				sig = Base32.encode( cman.getECCHandler().sign( request_str.getBytes( "UTF-8" ), "pairing" ));
				
			}catch( Throwable e ){
			}
			
			String other_params = 
				"&ver=" + UrlUtils.encode( Constants.AZUREUS_VERSION ) + 
				"&app=" + UrlUtils.encode( SystemProperties.getApplicationName()) +
				"&locale=" + UrlUtils.encode( MessageText.getCurrentLocale().toString());

			if ( sig != null ){
				
				other_params += "&sig=" + sig;
			}
			
			URL target = new URL( SERVICE_URL + "/client/" + command + "?request=" + request_str + other_params );
			
			HttpURLConnection connection = (HttpURLConnection)target.openConnection();
			
			InputStream is = connection.getInputStream();
			
			Map<String,Object> response = (Map<String,Object>)BDecoder.decode( new BufferedInputStream( is ));
			
			synchronized( this ){
				
				Long	min_retry = (Long)response.get( "min_secs" );
				
				if ( min_retry != null ){
					
					min_update_period	= min_retry.intValue()*1000;
				}
				
				Long	max_retry = (Long)response.get( "max_secs" );
				
				if ( max_retry != null ){
					
					max_update_period	= max_retry.intValue()*1000;
				}
			}
			
			final String message = getString( response, "message" );
			
			if ( message != null ){
				
				if ( last_message == null || !last_message.equals( message )){
					
					last_message = message;
				
					try{
						byte[] message_sig = (byte[])response.get( "message_sig" );
						
						AEVerifier.verifyData( message, message_sig );
						
						new AEThread2( "PairMsg", true )
						{
							public void
							run()
							{
								UIManager ui_manager = StaticUtilities.getUIManager( 120*1000 );
								
								if ( ui_manager != null ){
								
									ui_manager.showMessageBox(
											"pairing.server.warning.title",
											"!" + message + "!",
											UIManagerEvent.MT_OK );
								}
							}
						}.start();
						
					}catch( Throwable e ){
					}
				}
			}
			
			String error = getString( response, "error" );
			
			if ( error != null ){
				
				throw( new PairingException( error ));
			}
			
			last_server_error = null;
			
			return((Map<String,Object>)response.get( "rep" ));
			
		}catch( Throwable e ){
			
			last_server_error = Debug.getNestedExceptionMessage( e );
			
			if ( e instanceof PairingException ){
				
				throw((PairingException)e);
			}
			
			throw( new PairingException( "invocation failed", e ));
		}
	}
	
	protected void
	fireChanged()
	{
		for ( PairingManagerListener l: listeners ){
			
			try{
				l.somethingChanged( this );
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
	}
		
	public void
	addListener(
		PairingManagerListener		l )
	{
		listeners.add( l );
	}
	
	public void
	removeListener(
		PairingManagerListener		l )
	{
		listeners.remove( l );
	}
	
	protected String
	getString(
		Map<String,Object>	map,
		String				name )
	
		throws IOException
	{
		byte[]	bytes = (byte[])map.get(name);
		
		if ( bytes == null ){
			
			return( null );
		}
		
		return( new String( bytes, "UTF-8" ));
	}
	
	protected class
	PairedServiceImpl
		implements PairedService, PairingConnectionData
	{
		private String				sid;
		private Map<String,String>	attributes	= new HashMap<String, String>();
		
		protected
		PairedServiceImpl(
			String		_sid )
		{
			sid		= _sid;
		}
		
		public String
		getSID()
		{
			return( sid );
		}
		
		public PairingConnectionData
		getConnectionData()
		{
			return( this );
		}
		
		public void
		remove()
		{
			PairingManagerImpl.this.remove( this );
		}
		
		public void
		setAttribute(
			String		name,
			String		value )
		{
			synchronized( this ){
				
				if ( DEBUG ){
					System.out.println( "PS: " + sid + ": " + name + " -> " + value );
				}
				
				attributes.put( name, value );
			}
		}
		
		public String
		getAttribute(
			String		name )
		{
			return( attributes.get( name ));
		}
		
		public void
		sync()
		{
			PairingManagerImpl.this.sync( this );
		}
		
		protected Map<String,String>
		toMap()
		{
			Map<String,String> result = new HashMap<String, String>();
			
			result.put( "sid", sid );
			
			synchronized( this ){
			
				result.putAll( attributes );
			}
			
			return( result );
		}
	}
}
