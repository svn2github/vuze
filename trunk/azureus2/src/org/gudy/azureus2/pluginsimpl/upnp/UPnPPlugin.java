/*
 * Created on 14-Jun-2004
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

package org.gudy.azureus2.pluginsimpl.upnp;

/**
 * @author parg
 *
 */

import java.util.*;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.logging.*;
import org.gudy.azureus2.plugins.ui.*;
import org.gudy.azureus2.plugins.ui.model.*;
import org.gudy.azureus2.plugins.ui.config.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.upnp.*;
import org.gudy.azureus2.core3.upnp.services.*;

public class 
UPnPPlugin
	implements Plugin, UPnPMappingListener
{
	protected PluginInterface		plugin_interface;
	protected LoggerChannel 		log;
	
	protected UPnPMappingManager	mapping_manager	= UPnPMappingManager.getSingleton();
	
	protected UPnP	upnp;
	
	protected BooleanParameter	alert_success_param;
	protected BooleanParameter	grab_ports_param;
		
	protected List	mappings	= new ArrayList();
	protected List	services	= new ArrayList();
	
	public void
	initialize(
		PluginInterface	_plugin_interface )
	{
		plugin_interface	= _plugin_interface;
		
		log = plugin_interface.getLogger().getChannel("UPnP");

		UIManager	ui_manager = plugin_interface.getUIManager();
		
		final BasicPluginViewModel model = 
			ui_manager.createBasicPluginViewModel( 
					"UPnP");
		
		BasicPluginConfigModel	config = ui_manager.createBasicPluginConfigModel( "Plugins", "UPnP" );
		
		config.addLabelParameter2( "upnp.info" );
		
		final BooleanParameter enable_param = 
			config.addBooleanParameter2( "upnp.enable", "upnp.enable", true );
		
		alert_success_param = config.addBooleanParameter2( "upnp.alertsuccess", "upnp.alertsuccess", true );
		
		grab_ports_param = config.addBooleanParameter2( "upnp.grabports", "upnp.grabports", false );
		
		ActionParameter refresh_param = config.addActionParameter2( "upnp.refresh.label", "upnp.refresh.button" );
		
		refresh_param.addListener(
			new ParameterListener()
			{
				public void
				parameterChanged(
					Parameter	param )
				{
					System.out.println( "refresh!!!!" );
				}
			});
		
		enable_param.addEnabledOnSelection( alert_success_param );
		enable_param.addEnabledOnSelection( grab_ports_param );
		enable_param.addEnabledOnSelection( refresh_param );
		
		boolean	enabled = enable_param.getValue();
		
		model.getStatus().setText( enabled?"Running":"Disabled" );
		
		enable_param.addListener(
				new ParameterListener()
				{
					public void
					parameterChanged(
						Parameter	p )
					{
						boolean	enabled = enable_param.getValue();
						
						model.getStatus().setText( enabled?"Running":"Disabled" );
						
						if ( enabled ){
							
							startUp();
						}
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
		
		if ( enabled ){
			
			startUp();
		}
	}
	
	protected void
	startUp()
	{
		if ( upnp != null ){
			
			return;
		}
		
		try{
			upnp = UPnPFactory.getSingleton();
				
			upnp.addRootDeviceListener(
				new UPnPListener()
				{
					public void
					rootDeviceFound(
						UPnPRootDevice		device )
					{
						try{
							processDevice( device.getDevice() );
							
						}catch( Throwable e ){
							
							log.log( "Root device processing fails", e );
						}
					}
				});
			
			upnp.addLogListener(
				new UPnPLogListener()
				{
					public void
					log(
						String	str )
					{
						log.log( str );
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
			
			UPnPMapping[]	mappings = mapping_manager.getMappings();
			
			for (int i=0;i<mappings.length;i++){
				
				addMapping( mappings[i] );
			}
			
		}catch( Throwable e ){
			
			log.log( e );
		}
	}
	
	protected void
	processDevice(
		UPnPDevice		device )
	
		throws UPnPException
	{
		if ( device.getDeviceType().equalsIgnoreCase("urn:schemas-upnp-org:device:WANConnectionDevice:1")){
			
			log.log( "Found WANConnectionDevice" );
			
			processServices( device, device.getServices());
			
		}else{
			
			UPnPDevice[]	kids = device.getSubDevices();
			
			for (int i=0;i<kids.length;i++){
				
				processDevice( kids[i] );
			}
		}
	}
	
	protected void
	processServices(
		UPnPDevice		device,
		UPnPService[] 	services )
	
		throws UPnPException
	{
		for (int i=0;i<services.length;i++){
			
			UPnPService	s = services[i];
			
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
			}
		}
	}
	
	protected synchronized void
	addService(
		UPnPWANConnection	wan_service )
	
		throws UPnPException
	{
		log.log( "    Found " + ( wan_service.getGenericService().getServiceType().indexOf("PPP") == -1? "WANIPConnection":"WANPPPConnection" ));
		
		UPnPWANConnectionPortMapping[] ports = wan_service.getPortMappings();
		
		for (int j=0;j<ports.length;j++){
			
			log.log( "      mapping:" + ports[j].getExternalPort() + "/" + 
							(ports[j].isTCP()?"TCP":"UDP" ) + " -> " + ports[j].getInternalHost());
		}
		
		services.add(new UPnPPluginService( wan_service, ports, alert_success_param ));
		
		checkState();
	}
	
	protected synchronized void
	removeService(
		UPnPWANConnection	wan_service,
		boolean				replaced )
	{
		String	name = wan_service.getGenericService().getServiceType().indexOf("PPP") == -1? "WANIPConnection":"WANPPPConnection";
		
		String	text = 
			MessageText.getString( 
					"upnp.alert.lostdevice", 
					new String[]{ name, wan_service.getGenericService().getDevice().getRootDevice().getLocation().getHost()});
		
		log.log( text );
		
		if ( !replaced ){
			
			log.logAlertRepeatable( LoggerChannel.LT_WARNING, text );
		}
				
		for (int i=0;i<services.size();i++){
			
			UPnPPluginService	ps = (UPnPPluginService)services.get(i);
			
			if ( ps.getService() == wan_service ){
				
				services.remove(i);
				
				break;
			}
		}
	}
	
	protected synchronized void
	addMapping(
		UPnPMapping		mapping )
	{
		mappings.add( mapping );
		
		log.log( "Mapping request: " + 
					plugin_interface.getUtilities().getLocaleUtilities().getLocalisedMessageText(mapping.getResourceName()) +
					" - " + (mapping.isTCP()?"TCP":"UDP") + "/" + mapping.getPort() + ", enabled = " + mapping.isEnabled());
		
		mapping.addListener( this );
		
		checkState();
	}	
	
	public synchronized void
	mappingChanged(
		UPnPMapping	mapping )
	{
		checkState();
	}
	
	protected synchronized void
	checkState()
	{		
		for (int i=0;i<mappings.size();i++){
			
			UPnPMapping	mapping = (UPnPMapping)mappings.get(i);
			
			if ( !mapping.isEnabled()){
				
				continue;
			}
			
			for (int j=0;j<services.size();j++){
				
				UPnPPluginService	service = (UPnPPluginService)services.get(j);
				
				service.checkMapping( log, mapping );
			}
		}
	}
}
