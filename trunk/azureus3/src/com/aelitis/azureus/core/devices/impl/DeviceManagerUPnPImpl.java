/*
 * Created on Jan 27, 2009
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


package com.aelitis.azureus.core.devices.impl;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginEvent;
import org.gudy.azureus2.plugins.PluginEventListener;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.ipc.IPCInterface;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageRequest;
import org.gudy.azureus2.plugins.utils.UTTimer;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderFactory;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocument;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentException;
import org.gudy.azureus2.pluginsimpl.local.ipc.IPCInterfaceImpl;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.content.AzureusContentDownload;
import com.aelitis.azureus.core.content.AzureusContentFile;
import com.aelitis.azureus.core.content.AzureusContentFilter;
import com.aelitis.azureus.core.devices.TranscodeProfile;
import com.aelitis.azureus.core.util.UUIDGenerator;
import com.aelitis.net.upnp.UPnP;
import com.aelitis.net.upnp.UPnPAdapter;
import com.aelitis.net.upnp.UPnPDevice;
import com.aelitis.net.upnp.UPnPFactory;
import com.aelitis.net.upnp.UPnPListener;
import com.aelitis.net.upnp.UPnPRootDevice;
import com.aelitis.net.upnp.UPnPRootDeviceListener;
import com.aelitis.net.upnp.UPnPService;
import com.aelitis.net.upnp.services.UPnPWANConnection;

public class 
DeviceManagerUPnPImpl 
{
	private final static Object KEY_LISTENER_ADDED = new Object();
	
	private DeviceManagerImpl		manager;
	private PluginInterface			plugin_interface;
	private UPnP 					upnp;
	
	private volatile IPCInterface			upnpav_ipc;
	
	protected
	DeviceManagerUPnPImpl(
		DeviceManagerImpl		_manager )
	{
		manager	= _manager;
		
	}
	
	protected void
	initialise()
	{
		AzureusCore core = AzureusCoreFactory.getSingleton();
		
		plugin_interface = core.getPluginManager().getDefaultPluginInterface();
		
		plugin_interface.addListener(
				new PluginListener()
				{
					public void
					initializationComplete()
					{
						start();
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
	
	protected DeviceManagerImpl
	getManager()
	{
		return( manager );
	}
	
	protected void
	start()
	{
		UPnPAdapter adapter = 
			new UPnPAdapter()
			{
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
					return( plugin_interface.getUtilities().createTimer( name ));
				}
				
				public void
				createThread(
					String				name,
					final Runnable		runnable )
				{
					plugin_interface.getUtilities().createThread( name, runnable );
				}
				
				public Comparator
				getAlphanumericComparator()
				{
					return( plugin_interface.getUtilities().getFormatters().getAlphanumericComparator( true ));
				}

				public void
				log(
					Throwable	e )
				{
					Debug.printStackTrace(e);
				}
				
				public void
				trace(
					String	str )
				{
					// System.out.println( str );
				}
				
				public void
				log(
					String	str )
				{
					// System.out.println( str );
				}
				
				public String
				getTraceDir()
				{
					return( plugin_interface.getPluginDirectoryName());
				}
			};
		
		try{
			upnp = UPnPFactory.getSingleton( adapter, null );
			
			
			upnp.addRootDeviceListener(
				new UPnPListener()
				{
					public boolean
					deviceDiscovered(
						String		USN,
						URL			location )
					{
						return( true );
					}
					
					public void
					rootDeviceFound(
						UPnPRootDevice		device )
					{
						handleDevice( device, true );
					}
				});
		
		}catch( Throwable e ){
			
			manager.log( "UPnP device manager failed", e );
		}
		
		try{			
			plugin_interface.addEventListener(
					new PluginEventListener()
					{
						public void 
						handleEvent(
							PluginEvent ev )
						{
							int	type = ev.getType();
							
							if ( 	type == PluginEvent.PEV_PLUGIN_OPERATIONAL ||
									type == PluginEvent.PEV_PLUGIN_NOT_OPERATIONAL ){
								
								PluginInterface pi = (PluginInterface)ev.getValue();
			
								if ( pi.getPluginID().equals( "azupnpav" )){
				
									if ( type == PluginEvent.PEV_PLUGIN_OPERATIONAL ){
									
										upnpav_ipc = pi.getIPC();

										addListener( pi );
										
									}else{
										
										upnpav_ipc = null;
									}
								}
							}
						}
					});
			
			PluginInterface pi = plugin_interface.getPluginManager().getPluginInterfaceByID( "azupnpav" );

			if ( pi == null ){
				
				manager.log( "No UPnPAV plugin found" );
				
			}else{
				
				upnpav_ipc = pi.getIPC();
				
				addListener( pi );
			}			
		}catch( Throwable e ){
			
			manager.log( "Failed to hook into UPnPAV", e );
		}
	}
	
	protected void
	addListener(
		PluginInterface	pi )
	{
		try{			
			IPCInterface my_ipc = 
				new IPCInterfaceImpl(
					new Object()
					{
						public Map<String,Object>
						browseReceived(
							TrackerWebPageRequest		request,
							Map<String,Object>			browser_args )
						{
							Map headers = request.getHeaders();
							
							String user_agent 	= (String)headers.get( "user-agent" );
							String client_info 	= (String)headers.get( "x-av-client-info" );
							
							InetSocketAddress client_address = request.getClientAddress2();
						
							boolean	handled = false;
														
							if ( user_agent != null ){
								
								String lc_agent = user_agent.toLowerCase();
								
								if ( lc_agent.contains( "playstation 3")){
									
									handlePS3( client_address );
									
									handled = true;
									
								}else if ( lc_agent.contains( "xbox")){
								
									handleXBox( client_address );
									
									handled = true;
									
								}else if ( lc_agent.contains( "nintendo wii")){
								
									handleWii( client_address );
																		
									handled = true;
								}
							}
							
							if ( client_info != null ){
							
								String	lc_info = client_info.toLowerCase();
								
								if ( lc_info.contains( "playstation 3")){
									
									handlePS3( client_address );
									
									handled = true;
								}
							}
							
							if ( !handled ){
								
								String	 source = (String)browser_args.get( "source" );
								
								if ( source != null && source.equalsIgnoreCase( "http" )){
									
									handleBrowser( client_address );
																		
									handled = true;
								}
							}
							
							/*
							System.out.println( 
								"Received browse: " + request.getClientAddress() +
								", agent=" + user_agent +
								", info=" + client_info );
							*/
							
							DeviceImpl[] devices = manager.getDevices();
							
							final List<DeviceMediaRendererImpl>	browse_devices = new ArrayList<DeviceMediaRendererImpl>();
							
							for ( DeviceImpl device: devices ){
							
								if ( device instanceof DeviceMediaRendererImpl ){
								
									DeviceMediaRendererImpl renderer = (DeviceMediaRendererImpl)device;
									
									InetAddress device_address = renderer.getAddress();
									
									try{
										if ( device_address != null ){
														
												// just test on IP, should be OK
											
											if ( device_address.equals( client_address.getAddress())){
					
												if ( renderer.canFilterFilesView()){
												
													browse_devices.add( renderer );
													
													renderer.browseReceived();
												}
											}
										}
									}catch( Throwable e ){
										
										Debug.out( e );
									}
								}
							}
							
							Map<String,Object> result = new HashMap<String, Object>();
							
							if ( browse_devices.size() > 0 ){
								
								result.put(
									"filter",
									new AzureusContentFilter()
									{
										public boolean
										isVisible(
											AzureusContentDownload	download,
											Map<String,Object>		browse_args )
										{
											boolean	visible = false;
											
											for ( DeviceUPnPImpl device: browse_devices ){
												
												if ( device.isVisible( download )){
													
													visible	= true;
												}
											}
											
											return( visible );
										}
										
										public boolean
										isVisible(
											AzureusContentFile		file,
											Map<String,Object>		browse_args )
										{
											boolean	visible = false;
											
											for ( DeviceUPnPImpl device: browse_devices ){
												
												if ( device.isVisible( file )){
													
													visible	= true;
												}
											}
											
											return( visible );
										}
									});
							}
								
							return( result );
						}
					});
			
			if ( upnpav_ipc.canInvoke( "addBrowseListener", new Object[]{ my_ipc })){
				
				upnpav_ipc.invoke( "addBrowseListener", new Object[]{ my_ipc });
				
				DeviceImpl[] devices = manager.getDevices();
				
				for ( DeviceImpl device: devices ){
				
					if ( device instanceof DeviceUPnPImpl ){
					
						DeviceUPnPImpl u_d = (DeviceUPnPImpl)device;

						u_d.resetUPNPAV();
					}
				}
			}else{
				
				manager.log( "UPnPAV plugin needs upgrading" );
			}
		}catch( Throwable e ){
			
			manager.log( "Failed to hook into UPnPAV", e );
		}
	}
	
	protected IPCInterface
	getUPnPAVIPC()
	{
		return( upnpav_ipc );
	}
	
	public void
	search()
	{	
		if ( upnp != null ){
	
			// if the user has removed items we need to re-inject them

			UPnPRootDevice[] devices = upnp.getRootDevices();
			
			for ( UPnPRootDevice device: devices ){
				
				handleDevice( device, false );
			}
			
			String[] STs = {
				"upnp:rootdevice",
				"urn:schemas-upnp-org:device:MediaRenderer:1"
			};
			
			upnp.search( STs );
		}
	}
	
	protected void
	handleXBox(
		InetSocketAddress	address )
	{
		// normally we can detect the xbox renderer and things work automagically. However, on
		// occasion we receive the browse before detection and if the device's IP has changed
		// we need to associate its new address here otherwise association of browse to device
		// fails
		
		DeviceImpl[] devices = manager.getDevices();
		
		for ( DeviceImpl device: devices ){
			
			if ( device instanceof DeviceMediaRendererImpl && !device.isAlive()){
				
				if ( device.getName().toUpperCase().contains( "XBOX" )){
				
					((DeviceMediaRendererImpl)device).setAddress( address.getAddress());
					
					device.alive();
				}
			}
		}
	}
	
	protected void
	handlePS3(
		InetSocketAddress	address )
	{
		handleGeneric( address, "ps3", "PS3" );
	}

	protected void
	handleWii(
		InetSocketAddress	address )
	{
		handleGeneric( address, "wii", "Wii" );
	}
	
	protected void
	handleBrowser(
		InetSocketAddress	address )
	{
		handleGeneric( address, "browser", "Browser" );
	}
	
	protected void
	handleGeneric(
		InetSocketAddress	address,
		String				unique_name,
		String				display_name )
	{
		String uid;
		
		synchronized( this ){
			
			uid = COConfigurationManager.getStringParameter( "devices.upnp.uid." + unique_name, "" );
			
			if ( uid.length() == 0 ){
				
				uid = UUIDGenerator.generateUUIDString();
				
				COConfigurationManager.setParameter( "devices.upnp.uid." + unique_name, uid );
			}
		}
		
		DeviceMediaRendererImpl device = new DeviceMediaRendererImpl( manager, uid, display_name, false );
	
		device = (DeviceMediaRendererImpl)manager.addDevice( device );
		
		device.setAddress( address.getAddress());
		
		device.alive();
	}
	
	protected void
	handleDevice(
		UPnPRootDevice		root_device,
		boolean				update_if_found )
	{
		if ( !manager.getAutoSearch()){
			
			if ( !manager.isExplicitSearch()){
				
				return;
			}
		}
		
		handleDevice( root_device.getDevice(), update_if_found );
	}
	
	protected void
	handleDevice(
		UPnPDevice	device,
		boolean		update_if_found )
	{
		UPnPService[] 	services = device.getServices();
		
		List<DeviceUPnPImpl>	new_devices = new ArrayList<DeviceUPnPImpl>();
		
		List<UPnPWANConnection>	igd_services = new ArrayList<UPnPWANConnection>();
		
		for ( UPnPService service: services ){
				
			String	service_type = service.getServiceType();
				
			if ( 	service_type.equalsIgnoreCase( "urn:schemas-upnp-org:service:WANIPConnection:1") || 
					service_type.equalsIgnoreCase( "urn:schemas-upnp-org:service:WANPPPConnection:1")){
				
				UPnPWANConnection	wan_service = (UPnPWANConnection)service.getSpecificService();
				
				igd_services.add( wan_service );				

			}else if ( service_type.equals( "urn:schemas-upnp-org:service:ContentDirectory:1" )){
				
				new_devices.add( new DeviceContentDirectoryImpl( manager, device, service ));
			}
		}
		
		if ( igd_services.size() > 0 ){
			
			new_devices.add( new DeviceInternetGatewayImpl( manager, device, igd_services ));
		}
		
		if ( device.getDeviceType().equals( "urn:schemas-upnp-org:device:MediaRenderer:1" )){
				
			new_devices.add( new DeviceMediaRendererImpl( manager, device ));
		}
		
		for ( final DeviceUPnPImpl new_device: new_devices ){
			
			if ( !update_if_found &&  manager.getDevice( new_device.getID()) != null ){
					
				continue;
			}
			
				// grab the actual device as the 'addDevice' call will update an existing one
				// with same id
			
			final DeviceImpl actual_device = manager.addDevice( new_device );

			if ( actual_device.getTransientProperty( KEY_LISTENER_ADDED ) == null ){
				
				actual_device.setTransientProperty( KEY_LISTENER_ADDED, "" );
				
				device.getRootDevice().addListener(
					new UPnPRootDeviceListener()
					{
						public void
						lost(
							UPnPRootDevice	root,
							boolean			replaced )
						{
							if ( !replaced ){
								
								actual_device.dead();
							}
						}
					});
			}
		}
		
		for (UPnPDevice d: device.getSubDevices()){
			
			handleDevice( d, update_if_found );
		}
	}
}