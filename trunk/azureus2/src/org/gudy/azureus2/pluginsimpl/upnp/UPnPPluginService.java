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

package org.gudy.azureus2.pluginsimpl.upnp;

/**
 * @author parg
 *
 */

import java.util.*;

import org.gudy.azureus2.core3.upnp.services.*;
import org.gudy.azureus2.core3.internat.*;
import org.gudy.azureus2.plugins.logging.*;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;

public class 
UPnPPluginService 
{
	protected UPnPWANConnection		connection;
	protected BooleanParameter 		alert_success;
	
	protected List	service_mappings = new ArrayList();
	
	protected
	UPnPPluginService(
		UPnPWANConnection				_connection,
		UPnPWANConnectionPortMapping[]	_ports,
		BooleanParameter				_alert_success )
	{
		connection		= _connection;
		alert_success	= _alert_success;
		
		for (int i=0;i<_ports.length;i++){

			service_mappings.add( new serviceMapping( _ports[i]));
		}
	}
	
	protected UPnPWANConnection
	getService()
	{
		return( connection );
	}
	
	protected void
	checkMapping(
		LoggerChannel		log,
		UPnPMapping			mapping,
		boolean				grab_ports )
	{
		String	grab_in_progress	= null;
		
		String local_address = connection.getGenericService().getDevice().getRootDevice().getLocalAddress().getHostAddress();
		
		for (int i=0;i<service_mappings.size();i++){
			
			serviceMapping	sm = (serviceMapping)service_mappings.get(i);
			
			if ( 	sm.isTCP() == mapping.isTCP() &&
					sm.getPort() == mapping.getPort()){				
		
				if ( sm.getInternalHost().equals( local_address )){
					
					if ( !sm.getLogged()){
						
						sm.setLogged();
						
						log.log( "Mapping " + mapping.getString() + " already established" );
					}
					
					return;
					
				}else{
					
					if ( !grab_ports ){

						if ( !sm.getLogged()){
							
							sm.setLogged();
						
							String	text = 
								MessageText.getString( 
									"upnp.alert.differenthost", 
									new String[]{ mapping.getString(), sm.getInternalHost()});
						
							log.log( text );
						
							log.logAlertRepeatable( LoggerChannel.LT_WARNING, text );
						}
						
						return;
						
					}else{
						
						grab_in_progress	= sm.getInternalHost();
					}
				}
			}
		}
		
			// not found - try and establish it + add entry even if we fail so
			// that we don't retry later
		
		try{
			connection.addPortMapping( 
				mapping.isTCP(), mapping.getPort(),"AZ" + mapping.getPort());
		
			String	text;
			
			if ( grab_in_progress != null ){
				
				text = MessageText.getString( 
						"upnp.alert.mappinggrabbed", 
						new String[]{ mapping.getString(), grab_in_progress });
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
			
			log.logAlertRepeatable( LoggerChannel.LT_ERROR, text );
		}
		
		service_mappings.add( new serviceMapping( mapping ));
	}
	
	protected class
	serviceMapping
	{
		protected boolean		external;
		protected boolean		tcp;
		protected int			port;
		protected String		internal_host;
		
		protected boolean		logged;
		
		protected
		serviceMapping(
			UPnPWANConnectionPortMapping		mapping )
		{
			external		= true;
			tcp				= mapping.isTCP();
			port			= mapping.getExternalPort();
			internal_host	= mapping.getInternalHost();
		}
	
		protected
		serviceMapping(
			UPnPMapping		mapping )
		{
			external		= true;
			tcp				= mapping.isTCP();
			port			= mapping.getPort();
			internal_host	= connection.getGenericService().getDevice().getRootDevice().getLocalAddress().getHostAddress();
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
	}
}