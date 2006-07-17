/*
 * Created on 14-Jun-2004
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

package com.aelitis.azureus.plugins.upnp;

/**
 * @author parg
 *
 */

import java.util.*;
import java.net.URL;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.logging.*;
import org.gudy.azureus2.plugins.ui.*;
import org.gudy.azureus2.plugins.ui.model.*;
import org.gudy.azureus2.plugins.ui.config.*;
import org.gudy.azureus2.plugins.utils.UTTimer;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderFactory;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocument;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentException;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.net.natpmp.NATPMPDeviceAdapter;
import com.aelitis.net.natpmp.NatPMPDeviceFactory;
import com.aelitis.net.natpmp.upnp.NatPMPUPnP;
import com.aelitis.net.natpmp.upnp.NatPMPUPnPFactory;
import com.aelitis.net.upnp.*;
import com.aelitis.net.upnp.services.*;

public class 
UPnPPlugin
	implements Plugin, UPnPListener, UPnPMappingListener, UPnPWANConnectionListener
{
	private static final String UPNP_PLUGIN_CONFIGSECTION_ID 	= "UPnP";
	private static final String NATPMP_PLUGIN_CONFIGSECTION_ID 	= "NATPMP";

	private static final String		STATS_DISCOVER 	= "discover";
	private static final String		STATS_FOUND		= "found";
	private static final String		STATS_READ_OK 	= "read_ok";
	private static final String		STATS_READ_BAD 	= "read_bad";
	private static final String		STATS_MAP_OK 	= "map_ok";
	private static final String		STATS_MAP_BAD 	= "map_bad";
	
	private static final String[]	STATS_KEYS = { STATS_DISCOVER, STATS_FOUND, STATS_READ_OK, STATS_READ_BAD, STATS_MAP_OK, STATS_MAP_BAD };
	
	private PluginInterface		plugin_interface;
	private LoggerChannel 		log;
	
	private UPnPMappingManager	mapping_manager	= UPnPMappingManager.getSingleton( this );
	
	private UPnP	upnp;
	
	private NatPMPUPnP	nat_pmp_upnp;
	
	private BooleanParameter	natpmp_enable_param;
	private StringParameter		nat_pmp_router;
	
	private BooleanParameter 	upnp_enable_param;
	
	private BooleanParameter	alert_success_param;
	private BooleanParameter	grab_ports_param;
	private BooleanParameter	alert_other_port_param;
	private BooleanParameter	alert_device_probs_param;
	private BooleanParameter	release_mappings_param;
	private StringParameter	selected_interfaces_param;
	
	private BooleanParameter	ignore_bad_devices;
	private LabelParameter	ignored_devices_list;
	
	private List	mappings	= new ArrayList();
	private List	services	= new ArrayList();
	
	private Map	root_info_map	= new HashMap();
	
	protected AEMonitor	this_mon 	= new AEMonitor( "UPnPPlugin" );
	   
	public void
	initialize(
		PluginInterface	_plugin_interface )
	{
		plugin_interface	= _plugin_interface;
		
		plugin_interface.getPluginProperties().setProperty( "plugin.version", 	"1.0" );
		plugin_interface.getPluginProperties().setProperty( "plugin.name", 		"Universal Plug and Play (UPnP)" );
		
		log = plugin_interface.getLogger().getTimeStampedChannel("UPnP");

		UIManager	ui_manager = plugin_interface.getUIManager();
		
		final BasicPluginViewModel model = 
			ui_manager.createBasicPluginViewModel( 
					"UPnP");
		model.setConfigSectionID(UPNP_PLUGIN_CONFIGSECTION_ID);
		
		BasicPluginConfigModel	upnp_config = ui_manager.createBasicPluginConfigModel(ConfigSection.SECTION_PLUGINS, UPNP_PLUGIN_CONFIGSECTION_ID );
		
			// NATPMP
		
		BasicPluginConfigModel	natpmp_config = ui_manager.createBasicPluginConfigModel( UPNP_PLUGIN_CONFIGSECTION_ID, NATPMP_PLUGIN_CONFIGSECTION_ID );

		natpmp_config.addLabelParameter2( "natpmp.info" );
		
		ActionParameter	natpmp_wiki = natpmp_config.addActionParameter2( "Utils.link.visit", "MainWindow.about.internet.wiki" );
		
		natpmp_wiki.setStyle( ActionParameter.STYLE_LINK );
		
		natpmp_wiki.addListener(
			new ParameterListener()
			{
				public void
				parameterChanged(
					Parameter	param )
				{
					try{
						plugin_interface.getUIManager().openURL( new URL( "http://azureus.aelitis.com/wiki/index.php/NATPMP" ));
						
					}catch( Throwable e ){
						
						e.printStackTrace();
					}
				}
			});
		
		natpmp_enable_param = 
			natpmp_config.addBooleanParameter2( "natpmp.enable", "natpmp.enable", false );
		
		nat_pmp_router = 	natpmp_config.addStringParameter2( "natpmp.routeraddress", "natpmp.routeraddress", "" );
		
		natpmp_enable_param.addListener(
			new ParameterListener()
			{
				public void
				parameterChanged(
					Parameter	param )
				{
					setNATPMPEnableState();
				}
			});
		
		natpmp_enable_param.addEnabledOnSelection( nat_pmp_router );
		
			// UPNP
		
		upnp_config.addLabelParameter2( "upnp.info" );
		
		ActionParameter	upnp_wiki = upnp_config.addActionParameter2( "Utils.link.visit", "MainWindow.about.internet.wiki" );
		
		upnp_wiki.setStyle( ActionParameter.STYLE_LINK );
		
		upnp_wiki.addListener(
			new ParameterListener()
			{
				public void
				parameterChanged(
					Parameter	param )
				{
					try{
						plugin_interface.getUIManager().openURL( new URL( "http://azureus.aelitis.com/wiki/index.php/UPnP" ));
						
					}catch( Throwable e ){
						
						e.printStackTrace();
					}
				}
			});
		
		upnp_enable_param = 
			upnp_config.addBooleanParameter2( "upnp.enable", "upnp.enable", true );
		
		
		grab_ports_param = upnp_config.addBooleanParameter2( "upnp.grabports", "upnp.grabports", false );
		
		release_mappings_param	 = upnp_config.addBooleanParameter2( "upnp.releasemappings", "upnp.releasemappings", true );

		ActionParameter refresh_param = upnp_config.addActionParameter2( "upnp.refresh.label", "upnp.refresh.button" );
		
		refresh_param.addListener(
			new ParameterListener()
			{
				public void
				parameterChanged(
					Parameter	param )
				{
					upnp.reset();
				}
			});

		
		upnp_config.addLabelParameter2( "blank.resource" );
		
		alert_success_param = upnp_config.addBooleanParameter2( "upnp.alertsuccess", "upnp.alertsuccess", false );
		
		alert_other_port_param = upnp_config.addBooleanParameter2( "upnp.alertothermappings", "upnp.alertothermappings", true );
		
		alert_device_probs_param = upnp_config.addBooleanParameter2( "upnp.alertdeviceproblems", "upnp.alertdeviceproblems", true );
		
		selected_interfaces_param = upnp_config.addStringParameter2( "upnp.selectedinterfaces", "upnp.selectedinterfaces", "" );

		ignore_bad_devices = upnp_config.addBooleanParameter2( "upnp.ignorebaddevices", "upnp.ignorebaddevices", true );
		
		ignored_devices_list = upnp_config.addLabelParameter2( "upnp.ignorebaddevices.info" );

		ActionParameter reset_param = upnp_config.addActionParameter2( "upnp.ignorebaddevices.reset", "upnp.ignorebaddevices.reset.action" );
		
		reset_param.addListener(
			new ParameterListener()
			{
				public void
				parameterChanged(
					Parameter	param )
				{
					PluginConfig pc = plugin_interface.getPluginconfig();
					
					for (int i=0;i<STATS_KEYS.length;i++){
						
						String	key = "upnp.device.stats." + STATS_KEYS[i];
						
						pc.setPluginMapParameter( key, new HashMap());
					}
					
					pc.setPluginMapParameter( "upnp.device.ignorelist", new HashMap());
					
					updateIgnoreList();
				}
			});
		
		upnp_enable_param.addEnabledOnSelection( alert_success_param );
		upnp_enable_param.addEnabledOnSelection( grab_ports_param );
		upnp_enable_param.addEnabledOnSelection( refresh_param );
		upnp_enable_param.addEnabledOnSelection( alert_other_port_param );
		upnp_enable_param.addEnabledOnSelection( alert_device_probs_param );
		upnp_enable_param.addEnabledOnSelection( release_mappings_param );
		upnp_enable_param.addEnabledOnSelection( selected_interfaces_param );
		upnp_enable_param.addEnabledOnSelection( ignore_bad_devices );
		upnp_enable_param.addEnabledOnSelection( ignored_devices_list );
		upnp_enable_param.addEnabledOnSelection( reset_param );

		final boolean	enabled = upnp_enable_param.getValue();
		
		natpmp_enable_param.setEnabled( enabled );
		
		model.getStatus().setText( enabled?"Running":"Disabled" );
		
		upnp_enable_param.addListener(
				new ParameterListener()
				{
					public void
					parameterChanged(
						Parameter	p )
					{
						boolean	e = upnp_enable_param.getValue();
						
						natpmp_enable_param.setEnabled( e );
						
						model.getStatus().setText( e?"Running":"Disabled" );
						
						if ( e ){
							
							startUp();
							
						}else{
							
							closeDown( true );
						}
						
						setNATPMPEnableState();
					}
				});
		
		model.getActivity().setVisible( false );
		model.getProgress().setVisible( false );
		
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
		
		plugin_interface.addListener(
				new PluginListener()
				{
					public void
					initializationComplete()
					{	
						if ( enabled ){
							
							updateIgnoreList();
							
							startUp();			
						}
					}
					
					public void
					closedownInitiated()
					{
						if ( services.size() == 0 ){
							
							plugin_interface.getPluginconfig().setPluginParameter( "plugin.info", "" );
						}
					}
					
					public void
					closedownComplete()
					{
						closeDown( true );
					}
				});
	}
	
	protected void
	updateIgnoreList()
	{
		try{
			String	param = "";
			
			PluginConfig pc = plugin_interface.getPluginconfig();

			Map	ignored = pc.getPluginMapParameter( "upnp.device.ignorelist", new HashMap());
			
			Iterator	it = ignored.entrySet().iterator();
			
			while( it.hasNext()){
				
				Map.Entry	entry = (Map.Entry)it.next();
							
				Map	value = (Map)entry.getValue();
				
				param += "\n    " + entry.getKey() + ": " + new String((byte[])value.get( "Location" ));
			}
			
			if ( ignored.size() > 0 ){
				
				log.log( "Devices currently being ignored: " + param );
			}
			
			String	text = 
				plugin_interface.getUtilities().getLocaleUtilities().getLocalisedMessageText(
					"upnp.ignorebaddevices.info",
					new String[]{ param });
			
			ignored_devices_list.setLabelText( text );
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
	
	protected void
	ignoreDevice(
		String		USN,
		URL			location )
	{
		try{
			PluginConfig pc = plugin_interface.getPluginconfig();

			Map	ignored = pc.getPluginMapParameter( "upnp.device.ignorelist", new HashMap());
	
			Map	entry = (Map)ignored.get( USN );
			
			if ( entry == null ){
				
				entry	= new HashMap();
				
				entry.put( "Location", location.toString().getBytes());
				
				ignored.put( USN, entry );
				
				pc.setPluginMapParameter( "upnp.device.ignorelist", ignored );
				
				updateIgnoreList();
				
				String	text = 
					plugin_interface.getUtilities().getLocaleUtilities().getLocalisedMessageText(
						"upnp.ignorebaddevices.alert",
						new String[]{ location.toString() });

				log.logAlertRepeatable( LoggerChannel.LT_WARNING, text );

			}
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
	
	protected void
	startUp()
	{
		if ( upnp != null ){
			
				// already started up, must have been re-enabled
			
			upnp.reset();
			
			return;
		}
		
		final LoggerChannel	core_log		= plugin_interface.getLogger().getChannel("UPnP Core");

		try{
			upnp = UPnPFactory.getSingleton(
					new UPnPAdapter()
					{
						Set	exception_traces = new HashSet();
						
						public SimpleXMLParserDocument
						parseXML(
							String	data )
						
							throws SimpleXMLParserDocumentException
						{
							return( plugin_interface.getUtilities().getSimpleXMLParserDocumentFactory().create( data ));
						}
						
						public ResourceDownloaderFactory
						getResourceDownloaderFactory()
						{
							return( plugin_interface.getUtilities().getResourceDownloaderFactory());
						}
						
						public UTTimer
						createTimer(
							String	name )
						{
							return( plugin_interface.getUtilities().createTimer( name, true ));
						}
						
						public void
						createThread(
							String		name,
							Runnable	runnable )
						{
							plugin_interface.getUtilities().createThread( name, runnable );
						}
						
						public Comparator
						getAlphanumericComparator()
						{
							return( plugin_interface.getUtilities().getFormatters().getAlphanumericComparator( true ));
						}

						public void
						trace(
							String	str )
						{
							core_log.log( str );
						}
						
						public void
						log(
							Throwable	e )
						{

							String	nested = Debug.getNestedExceptionMessage(e);
							
							if ( !exception_traces.contains( nested )){
								
								exception_traces.add( nested );
								
								if ( exception_traces.size() > 128 ){
									
									exception_traces.clear();
								}
								
								core_log.log( e );
																
							}else{
								
								core_log.log( nested );
							}
						}
						
						public void
						log(
							String	str )
						{
							log.log( str );
						}
						
						public String
						getTraceDir()
						{
							return( plugin_interface.getUtilities().getAzureusUserDir());
						}
					},
					getSelectedInterfaces());
				
			upnp.addRootDeviceListener( this );
			
			upnp.addLogListener(
				new UPnPLogListener()
				{
					public void
					log(
						String	str )
					{
						log.log( str );
					}
					
					public void
					logAlert(
						String	str,
						boolean	error,
						int		type )
					{
						boolean	logged = false;
						
						if ( alert_device_probs_param.getValue()){
							
							if ( type == UPnPLogListener.TYPE_ALWAYS ){
								
								log.logAlertRepeatable(						
										error?LoggerChannel.LT_ERROR:LoggerChannel.LT_WARNING,
										str );
								
								logged	= true;
								
							}else{
								
								boolean	do_it	= false;
								
								if ( type == UPnPLogListener.TYPE_ONCE_EVER ){
									
									byte[] fp = 
										plugin_interface.getUtilities().getSecurityManager().calculateSHA1(
											str.getBytes());
									
									String	key = "upnp.alert.fp." + plugin_interface.getUtilities().getFormatters().encodeBytesToString( fp );
									
									PluginConfig pc = plugin_interface.getPluginconfig();
									
									if ( !pc.getPluginBooleanParameter( key, false )){
										
										pc.setPluginParameter( key, true );
										
										do_it	= true;
									}
								}else{
									
									do_it	= true;
								}
							
								if ( do_it ){						
									
									log.logAlert(						
										error?LoggerChannel.LT_ERROR:LoggerChannel.LT_WARNING,
										str );	
									
									logged	= true;
								}
							}		
						}
						
						if ( !logged ){
							
							log.log( str );
						}
					}
				});
			
			mapping_manager.addListener(
				new UPnPMappingManagerListener()
				{
					public void
					mappingAdded(
						UPnPMapping		mapping )
					{
						addMapping( mapping );
					}
				});
			
			UPnPMapping[]	upnp_mappings = mapping_manager.getMappings();
			
			for (int i=0;i<upnp_mappings.length;i++){
				
				addMapping( upnp_mappings[i] );
			}
			
			setNATPMPEnableState();

		}catch( Throwable e ){
			
			log.log( e );
		}
	}
	
	protected void
	closeDown(
		boolean	end_of_day )
	{
		for (int i=0;i<mappings.size();i++){
			
			UPnPMapping	mapping = (UPnPMapping)mappings.get(i);
			
			if ( !mapping.isEnabled()){
				
				continue;
			}
			
			for (int j=0;j<services.size();j++){
				
				UPnPPluginService	service = (UPnPPluginService)services.get(j);
				
				service.removeMapping( log, mapping, end_of_day );
			}
		}		
	}

	public boolean
	deviceDiscovered(
		String		USN,
		URL			location )
	{
		if ( !ignore_bad_devices.getValue()){
			
			return( true );
		}
		
		incrementDeviceStats( USN, STATS_DISCOVER );

		boolean	ok = checkDeviceStats( USN, location );
		
		String	stats = "";
		
		for (int i=0;i<STATS_KEYS.length;i++){

			stats += (i==0?"":",")+STATS_KEYS[i] + "=" + getDeviceStats( USN, STATS_KEYS[i] );
		}

		if ( !ok ){
			
			log.log( "Device '" + location + "' is being ignored: " + stats );
			
		}else{
			
			
			log.log( "Device '" + location +"' is ok: " + stats );
		}
		
		return( ok );
	}
	
	public void
	rootDeviceFound(
		UPnPRootDevice	device )
	{
		incrementDeviceStats( device.getUSN(), "found" );

		checkDeviceStats( device );
		
		try{
			processDevice( device.getDevice() );
			
			try{
				this_mon.enter();
			
				root_info_map.put( device.getLocation(), device.getInfo());
			
				Iterator	it = root_info_map.values().iterator();
				
				String	all_info = "";
					
				List	reported_info = new ArrayList();
				
				while( it.hasNext()){
					
					String	info = (String)it.next();
					
					if ( info != null && !reported_info.contains( info )){
						
						reported_info.add( info );
						
						all_info += (all_info.length()==0?"":",") + info;
					}
				}
				
				if ( all_info.length() > 0 ){
					
					plugin_interface.getPluginconfig().setPluginParameter( "plugin.info", all_info );
				}
				
			}finally{
				
				this_mon.exit();
			}
			
		}catch( Throwable e ){
			
			log.log( "Root device processing fails", e );
		}
	}
	
	protected boolean
	checkDeviceStats(
		UPnPRootDevice	root )
	{
		return( checkDeviceStats( root.getUSN(), root.getLocation()));
	}
	
	protected boolean
	checkDeviceStats(
		String	USN,
		URL		location )
	{
		long	discovers 	= getDeviceStats( USN, STATS_DISCOVER );
		long	founds		= getDeviceStats( USN, STATS_FOUND );
		
		if ( discovers > 3 && founds == 0 ){
			
				// discovered but never found - something went wrong with the device
				// construction process
			
			ignoreDevice( USN, location );
			
			return( false );
			
		}else if ( founds > 0 ){
			
				// found ok before, reset details in case now its screwed
			
			setDeviceStats( USN, STATS_DISCOVER, 0 );
			setDeviceStats( USN, STATS_FOUND, 0 );
		}
		
		long	map_ok	 	= getDeviceStats( USN, STATS_MAP_OK );
		long	map_bad		= getDeviceStats( USN, STATS_MAP_BAD );

		if ( map_bad > 5 && map_ok == 0 ){
			
			ignoreDevice( USN, location );
			
			return( false );
			
		}else if ( map_ok > 0 ){
			
			setDeviceStats( USN, STATS_MAP_OK, 0 );
			setDeviceStats( USN, STATS_MAP_BAD, 0 );
		}
		
		return( true );
	}
	
	protected long
	incrementDeviceStats(
		String		USN,
		String		stat_key )
	{
		String	key = "upnp.device.stats." + stat_key;
		
		PluginConfig pc = plugin_interface.getPluginconfig();

		Map	counts = pc.getPluginMapParameter( key, new HashMap());
		
		Long	count = (Long)counts.get( USN );
		
		if ( count == null ){
			
			count = new Long(1);
			
		}else{
			
			count = new Long( count.longValue() + 1 );
		}
		
		counts.put( USN, count );
		
		pc.getPluginMapParameter( key, counts );
				
		return( count.longValue());
	}
	
	protected long
	getDeviceStats(
		String		USN,
		String		stat_key )
	{
		String	key = "upnp.device.stats." + stat_key;
		
		PluginConfig pc = plugin_interface.getPluginconfig();

		Map	counts = pc.getPluginMapParameter( key, new HashMap());
		
		Long	count = (Long)counts.get( USN );
		
		if ( count == null ){
			
			return( 0 );
		}
		
		return( count.longValue());
	}
		
	protected void
	setDeviceStats(
		String		USN,
		String		stat_key,
		long		value )
	{
		String	key = "upnp.device.stats." + stat_key;
		
		PluginConfig pc = plugin_interface.getPluginconfig();

		Map	counts = pc.getPluginMapParameter( key, new HashMap());
		
		counts.put( USN, new Long( value ));
		
		pc.getPluginMapParameter( key, counts );
	}
	
	public void
	mappingResult(
		UPnPWANConnection	connection,
		boolean				ok )
	{
		UPnPRootDevice	root = connection.getGenericService().getDevice().getRootDevice();
		
		incrementDeviceStats( root.getUSN(), ok?STATS_MAP_OK:STATS_MAP_BAD );
		
		checkDeviceStats( root );
	}
	
	public void
	mappingsReadResult(
		UPnPWANConnection	connection,
		boolean				ok )
	{
		UPnPRootDevice	root = connection.getGenericService().getDevice().getRootDevice();

		incrementDeviceStats( root.getUSN(), ok?STATS_READ_OK:STATS_READ_BAD );
	}
	
	protected String[]
	getSelectedInterfaces()
	{
		String	si = selected_interfaces_param.getValue().trim();
		
		StringTokenizer	tok = new StringTokenizer( si, ";" );
		
		List	res = new ArrayList();
		
		while( tok.hasMoreTokens()){
			
			String	s = tok.nextToken().trim();
			
			if ( s.length() > 0 ){
				
				res.add( s );
			}
		}
		
		return( (String[])res.toArray( new String[res.size()]));
	}
	
	protected void
	processDevice(
		UPnPDevice		device )
	
		throws UPnPException
	{			
		processServices( device, device.getServices());
			
		UPnPDevice[]	kids = device.getSubDevices();
		
		for (int i=0;i<kids.length;i++){
			
			processDevice( kids[i] );
		}
	}
	
	protected void
	processServices(
		UPnPDevice		device,
		UPnPService[] 	device_services )
	
		throws UPnPException
	{
		for (int i=0;i<device_services.length;i++){
			
			UPnPService	s = device_services[i];
			
			String	service_type = s.getServiceType();
			
			if ( 	service_type.equalsIgnoreCase( "urn:schemas-upnp-org:service:WANIPConnection:1") || 
					service_type.equalsIgnoreCase( "urn:schemas-upnp-org:service:WANPPPConnection:1")){
				
				final UPnPWANConnection	wan_service = (UPnPWANConnection)s.getSpecificService();
								
				device.getRootDevice().addListener(
					new UPnPRootDeviceListener()
					{
						public void
						lost(
							UPnPRootDevice	root,
							boolean			replaced )
						{
							removeService( wan_service, replaced );
						}
					});
				
				addService( wan_service );
				
			}else if ( 	service_type.equalsIgnoreCase( "urn:schemas-upnp-org:service:WANCommonInterfaceConfig:1")){ 
				
				/* useless stats
				try{
					UPnPWANCommonInterfaceConfig	config = (UPnPWANCommonInterfaceConfig)s.getSpecificService();
				
					long[]	speeds = config.getCommonLinkProperties();
					
					if ( speeds[0] > 0 && speeds[1] > 0 ){
						
						log.log( "Device speed: down=" + 
									plugin_interface.getUtilities().getFormatters().formatByteCountToKiBEtcPerSec(speeds[0]/8) + ", up=" + 
									plugin_interface.getUtilities().getFormatters().formatByteCountToKiBEtcPerSec(speeds[1]/8));
					}
				}catch( Throwable e ){
					
					log.log(e);
				}
				*/
			}
		}
	}
	
	protected void
	addService(
		UPnPWANConnection	wan_service )
	
		throws UPnPException
	{
		wan_service.addListener( this );
			
		mapping_manager.serviceFound( wan_service );

		try{
			this_mon.enter();
		
			log.log( "    Found " + ( wan_service.getGenericService().getServiceType().indexOf("PPP") == -1? "WANIPConnection":"WANPPPConnection" ));
			
			UPnPWANConnectionPortMapping[] ports;
			
			String	usn = wan_service.getGenericService().getDevice().getRootDevice().getUSN();
			
			if ( getDeviceStats( usn, STATS_READ_OK ) == 0 && getDeviceStats( usn, STATS_READ_BAD ) > 2 ){
				
				ports = new UPnPWANConnectionPortMapping[0];
				
				wan_service.periodicallyRecheckMappings( false );
				
				log.log( "    Not reading port mappings from device due to previous failures" );
				
			}else{
				
				ports = wan_service.getPortMappings();
			}
			
			for (int j=0;j<ports.length;j++){
				
				log.log( "      mapping [" + j  + "] " + ports[j].getExternalPort() + "/" + 
								(ports[j].isTCP()?"TCP":"UDP" ) + " [" + ports[j].getDescription() + "] -> " + ports[j].getInternalHost());
			}
			
			services.add(new UPnPPluginService( wan_service, ports, alert_success_param, grab_ports_param, alert_other_port_param, release_mappings_param ));
			
			checkState();
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected void
	removeService(
		UPnPWANConnection	wan_service,
		boolean				replaced )
	{
		try{
			this_mon.enter();
			
			String	name = wan_service.getGenericService().getServiceType().indexOf("PPP") == -1? "WANIPConnection":"WANPPPConnection";
			
			String	text = 
				MessageText.getString( 
						"upnp.alert.lostdevice", 
						new String[]{ name, wan_service.getGenericService().getDevice().getRootDevice().getLocation().getHost()});
			
			log.log( text );
			
			if ( (!replaced) && alert_device_probs_param.getValue()){
				
				log.logAlertRepeatable( LoggerChannel.LT_WARNING, text );
			}
					
			for (int i=0;i<services.size();i++){
				
				UPnPPluginService	ps = (UPnPPluginService)services.get(i);
				
				if ( ps.getService() == wan_service ){
					
					services.remove(i);
					
					break;
				}
			}
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected void
	addMapping(
		UPnPMapping		mapping )
	{
		try{
			this_mon.enter();
		
			mappings.add( mapping );
			
			log.log( "Mapping request: " + mapping.getString() + ", enabled = " + mapping.isEnabled());
			
			mapping.addListener( this );
			
			checkState();
			
		}finally{
			
			this_mon.exit();
		}
	}	
	
	public void
	mappingChanged(
		UPnPMapping	mapping )
	{
		checkState();
	
	}
	
	public void
	mappingDestroyed(
		UPnPMapping	mapping )
	{
		try{
			this_mon.enter();
		
			mappings.remove( mapping );
			
			for (int j=0;j<services.size();j++){
				
				UPnPPluginService	service = (UPnPPluginService)services.get(j);
				
				service.removeMapping( log, mapping, false );
			}
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected void
	checkState()
	{		
		try{
			this_mon.enter();
		
			for (int i=0;i<mappings.size();i++){
				
				UPnPMapping	mapping = (UPnPMapping)mappings.get(i);
	
				for (int j=0;j<services.size();j++){
					
					UPnPPluginService	service = (UPnPPluginService)services.get(j);
					
					service.checkMapping( log, mapping );
				}
			}
		}finally{
			
			this_mon.exit();
		}
	}
	
	public String[]
	getExternalIPAddresses()
	{
		List	res = new ArrayList();
		
		try{
			this_mon.enter();
			
			for (int j=0;j<services.size();j++){
				
				UPnPPluginService	service = (UPnPPluginService)services.get(j);
				
				try{
					String	address = service.getService().getExternalIPAddress();
				
					if ( address != null ){
						
						res.add( address );
					}
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
			}
		}finally{
			
			this_mon.exit();
		}
		
		return((String[])res.toArray( new String[res.size()]));
	}
	
		// for external use, e.g. webui
	
	public UPnPMapping
	addMapping(
		String		desc_resource,
		boolean		tcp,
		int			port,
		boolean		enabled )
	{
		return( mapping_manager.addMapping( desc_resource, tcp, port, enabled ));
	}
	
	public UPnPMapping
	getMapping(
		boolean	tcp,
		int		port )
	{
		return( mapping_manager.getMapping( tcp, port ));
	}
	
	protected void
	setNATPMPEnableState()
	{
		boolean	enabled = natpmp_enable_param.getValue() && upnp_enable_param.getValue();
		
		try{
			if ( enabled ){
				
				if ( nat_pmp_upnp == null ){
			
					nat_pmp_upnp = 
						NatPMPUPnPFactory.create( 
							upnp, 
							NatPMPDeviceFactory.getSingleton(
								new NATPMPDeviceAdapter()
								{
									public String 
									getRouterAddress() 
									{
										return( nat_pmp_router.getValue());
									}
									
									public void
									log(
										String	str )
									{
										log.log( "NAT-PMP: " + str );
									}
								}));
			
					nat_pmp_upnp.addListener( this );
				}
				
				nat_pmp_upnp.setEnabled( true );
			}else{
				
				if ( nat_pmp_upnp != null ){
					
					nat_pmp_upnp.setEnabled( false );
				}
			}
		}catch( Throwable e ){
			
			log.log( "Failed to initialise NAT-PMP subsystem", e );
		}
	}
	protected void
	logAlert(
		int			type,
		String		resource,
		String[]	params )
	{
		String	text = 
			plugin_interface.getUtilities().getLocaleUtilities().getLocalisedMessageText(
					resource, params );

		log.logAlertRepeatable( type, text );
	}
}
