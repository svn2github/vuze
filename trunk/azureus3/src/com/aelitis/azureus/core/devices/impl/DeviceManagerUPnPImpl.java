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

import java.net.InetSocketAddress;
import java.net.URL;
import java.util.*;

import org.gudy.azureus2.core3.util.Debug;
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
	private DeviceManagerImpl		manager;
	private PluginInterface			plugin_interface;
	private UPnP 					upnp;
	
	protected
	DeviceManagerUPnPImpl(
		DeviceManagerImpl		_manager )
	{
		manager	= _manager;
		
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
			PluginInterface pi = plugin_interface.getPluginManager().getPluginInterfaceByID( "azupnpav" );
			
			if ( pi == null ){
				
				manager.log( "No UPnPAV plugin found" );
				
			}else{
				
				IPCInterface upnpav_ipc = pi.getIPC();
				
				IPCInterface my_ipc = 
					new IPCInterfaceImpl(
						new Object()
						{
							public void
							browseReceived(
								TrackerWebPageRequest		request )
							{
								Map headers = request.getHeaders();
								
								String user_agent 	= (String)headers.get( "user-agent" );
								String client_info 	= (String)headers.get( "x-av-client-info" );
								
								if ( user_agent != null ){
									
									if ( user_agent.equalsIgnoreCase( "PLAYSTATION 3" )){
										
										handlePS3( request.getClientAddress2());
									}
								}
								
								if ( client_info != null ){
								
									if ( client_info.toUpperCase().contains( "PLAYSTATION 3")){
										
										handlePS3( request.getClientAddress2());
									}
								}
								
								System.out.println( 
									"Received browse: " + request.getClientAddress() +
									", agent=" + user_agent +
									", info=" + client_info );
							}
						});
				
				if ( upnpav_ipc.canInvoke( "addBrowseListener", new Object[]{ my_ipc })){
					
					upnpav_ipc.invoke( "addBrowseListener", new Object[]{ my_ipc });
					
				}else{
					
					manager.log( "UPnPAV plugin needs upgrading" );
				}
			}
		}catch( Throwable e ){
			
			manager.log( "Failed to hook into UPnPAV", e );
		}
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
	handlePS3(
		InetSocketAddress	address )
	{
		String psp_uid = "";
		
		DeviceImpl device = new DeviceMediaRendererImpl( manager, psp_uid, "PS3" );
	
		device = manager.addDevice( device );
		
		device.setTransientProperty( DeviceUPnPImpl.TP_IP_ADDRESS, address.getAddress().getHostAddress() + ":" + address.getPort());
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
			
			manager.addDevice( new_device );

			device.getRootDevice().addListener(
				new UPnPRootDeviceListener()
				{
					public void
					lost(
						UPnPRootDevice	root,
						boolean			replaced )
					{
						if ( !replaced ){
							
							new_device.dead();
						}
					}
				});
		}
		
		for (UPnPDevice d: device.getSubDevices()){
			
			handleDevice( d, update_if_found );
		}
	}
}