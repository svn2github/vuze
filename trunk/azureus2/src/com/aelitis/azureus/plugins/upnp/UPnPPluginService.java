/*
 * Created on 16-Jun-2004
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

package com.aelitis.azureus.plugins.upnp;

/**
 * @author parg
 *
 */

import java.util.*;

import org.gudy.azureus2.core3.internat.*;
import org.gudy.azureus2.plugins.logging.*;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;

import com.aelitis.net.upnp.services.*;

public class 
UPnPPluginService 
{
	protected UPnPWANConnection		connection;
	protected BooleanParameter 		alert_success;
	protected BooleanParameter 		grab_ports;
	protected BooleanParameter 		alert_other_port_param;
	
	protected List	service_mappings = new ArrayList();
	
	protected
	UPnPPluginService(
		UPnPWANConnection				_connection,
		UPnPWANConnectionPortMapping[]	_ports,
		BooleanParameter				_alert_success,
		BooleanParameter				_grab_ports,
		BooleanParameter				_alert_other_port_param)
	{
		connection				= _connection;
		alert_success			= _alert_success;
		grab_ports				= _grab_ports;
		alert_other_port_param	= _alert_other_port_param;
		
		for (int i=0;i<_ports.length;i++){

			service_mappings.add( new serviceMapping( _ports[i]));
		}
	}
	
	protected UPnPWANConnection
	getService()
	{
		return( connection );
	}
	
	protected String
	getDescriptionForPort(
		int		port )
	{
		return( "Azureus UPnP " + port );
	}
	
	protected synchronized void
	checkMapping(
		LoggerChannel		log,
		UPnPMapping			mapping )
	{
		if ( mapping.isEnabled()){
			
				// check for change of port number and delete old value if so
			
			for (int i=0;i<service_mappings.size();i++){
				
				serviceMapping	sm = (serviceMapping)service_mappings.get(i);
				
				if ( sm.getMapping() == mapping ){
			
					if ( sm.getPort() != mapping.getPort()){
						
						removeMapping( log, sm );
					}
				}
			}
		
			serviceMapping	grab_in_progress	= null;
			
			String local_address = connection.getGenericService().getDevice().getRootDevice().getLocalAddress().getHostAddress();
			
			for (int i=0;i<service_mappings.size();i++){
				
				serviceMapping	sm = (serviceMapping)service_mappings.get(i);
				
				if ( 	sm.isTCP() 		== mapping.isTCP() &&
						sm.getPort() 	== mapping.getPort()){				
			
					if ( sm.getInternalHost().equals( local_address )){
						
							// make sure we tie this to the mapping in case it
							// was external to begin with
						
						sm.setMapping( mapping  );
						
						if ( !sm.getLogged()){
							
							sm.setLogged();
							
							log.log( "Mapping " + mapping.getString() + " already established" );
						}
						
						return;
						
					}else{
						
						if ( !grab_ports.getValue() ){
	
							if ( !sm.getLogged()){
								
								sm.setLogged();
							
								String	text = 
									MessageText.getString( 
										"upnp.alert.differenthost", 
										new String[]{ mapping.getString(), sm.getInternalHost()});
							
								log.log( text );
							
								if ( alert_other_port_param.getValue()){
								
									log.logAlertRepeatable( LoggerChannel.LT_WARNING, text );
								}
							}
							
							return;
							
						}else{
							
								// we're going to grab it
							
							sm.setMapping( mapping  );

							grab_in_progress	= sm;
						}
					}
				}
			}
			
				// not found - try and establish it + add entry even if we fail so
				// that we don't retry later
						
			try{
				connection.addPortMapping( 
					mapping.isTCP(), mapping.getPort(), getDescriptionForPort( mapping.getPort()));
							
				String	text;
				
				if ( grab_in_progress != null ){
					
					text = MessageText.getString( 
							"upnp.alert.mappinggrabbed", 
							new String[]{ mapping.getString(), grab_in_progress.getInternalHost()});
				}else{
					
					text = MessageText.getString( 
							"upnp.alert.mappingok", 
							new String[]{ mapping.getString()});
				}
				
				log.log( text );
				
				if ( alert_success.getValue()){
					
					log.logAlertRepeatable( LoggerChannel.LT_INFORMATION, text );
				}
				
			}catch( Throwable e ){
				
				String	text = 
					MessageText.getString( 
							"upnp.alert.mappingfailed", 
							new String[]{ mapping.getString()});
				
				log.log( text );
				
				if ( alert_other_port_param.getValue()){
				
					log.logAlertRepeatable( LoggerChannel.LT_ERROR, text );
				}
			}
			
			if ( grab_in_progress == null ){
				
				serviceMapping	new_mapping = new serviceMapping( mapping );
			
				service_mappings.add( new_mapping );
			}
			
		}else{
				// mapping is disabled
			
			removeMapping( log, mapping );
		}
	}
	
	protected synchronized void
	removeMapping(
		LoggerChannel		log,
		UPnPMapping			mapping )
	{
		String local_address = connection.getGenericService().getDevice().getRootDevice().getLocalAddress().getHostAddress();
		
		for (int i=0;i<service_mappings.size();i++){
			
			serviceMapping	sm = (serviceMapping)service_mappings.get(i);
			
			if ( 	sm.isTCP() == mapping.isTCP() &&
					sm.getPort() == mapping.getPort() &&
					sm.getMapping() != null ){
				
				removeMapping( log, sm );

				return;
			}
		}
	}
							
	protected void
	removeMapping(
		LoggerChannel		log,
		serviceMapping		mapping )
	{
		if ( mapping.isExternal()){
		
			log.log( "Mapping " + mapping.getString() + " not removed as not created by Azureus" );
			
		}else{
			
			try{
				connection.deletePortMapping( 
					mapping.isTCP(), mapping.getPort());
		
				log.log( "Mapping " + mapping.getString() + " removed" );
				
			}catch( Throwable e ){
				
				log.log( "Mapping " + mapping.getString() + " failed to delete", e );
			}
			
			service_mappings.remove(mapping);
		}
	}
	
	protected class
	serviceMapping
	{
		protected UPnPMapping	mapping;
		
		protected boolean		tcp;
		protected int			port;
		protected String		internal_host;
		
		protected boolean		external;		// true -> not defined by us
		protected boolean		logged;
		
		protected
		serviceMapping(
			UPnPWANConnectionPortMapping		mapping )
		{
			tcp				= mapping.isTCP();
			port			= mapping.getExternalPort();
			internal_host	= mapping.getInternalHost();
			
			String	desc = mapping.getDescription();
			
			if ( desc == null || !desc.equalsIgnoreCase( getDescriptionForPort( port ))){
				
				external		= true;
			}
		}
	
		protected
		serviceMapping(
			UPnPMapping		_mapping )
		{
			mapping			= _mapping;
			tcp				= mapping.isTCP();
			port			= mapping.getPort();
			internal_host	= connection.getGenericService().getDevice().getRootDevice().getLocalAddress().getHostAddress();

		}
		
		protected boolean
		isExternal()
		{
			return( external );
		}
		
		protected UPnPMapping
		getMapping()
		{
			return( mapping );
		}
		
		protected void
		setMapping(
			UPnPMapping	_mapping )
		{
			mapping	= _mapping;
		}
		
		protected boolean
		getLogged()
		{
			return( logged );
		}
		
		protected void
		setLogged()
		{
			logged	= true;
		}
		
		protected boolean
		isTCP()
		{
			return( tcp );
		}
		
		protected int
		getPort()
		{
			return( port );
		}
		
		protected String
		getInternalHost()
		{
			return( internal_host );
		}
		
		public String
		getString()
		{
			if ( mapping==null ){
				
				return( "<external> (" + (isTCP()?"TCP":"UDP")+"/"+getPort()+")" ); 
				
			}else{
				
				return( mapping.getString( getPort()));
			}
		}
	}
}