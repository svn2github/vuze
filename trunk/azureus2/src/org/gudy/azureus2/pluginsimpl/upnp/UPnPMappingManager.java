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

import org.gudy.azureus2.core3.config.*;

public class 
UPnPMappingManager 
{
	protected static UPnPMappingManager	singleton = new UPnPMappingManager();
	
	public static UPnPMappingManager
	getSingleton()
	{
		return( singleton );
	}
	
	protected List	mappings	= new ArrayList();
	protected List	listeners	= new ArrayList();
	
	protected
	UPnPMappingManager()
	{
			// incoming data port
				
		addConfigPort( "upnp.mapping.dataport", true, "TCP.Listen.Port", true );
		
			// tracker TCP
		
		addConfigPort( "upnp.mapping.tcptrackerport", true, "Tracker Port Enable", "Tracker Port" );
		
		addConfigPort( "upnp.mapping.tcpssltrackerport", true, "Tracker Port SSL Enable", "Tracker Port SSL" );
		
			// tracker UDP

		addConfigPort( "upnp.mapping.udptrackerport", false, "Tracker Port UDP Enable", "Tracker Port" );
	}
	
	protected UPnPMapping
	addConfigPort(
		String			name_resource,
		boolean			tcp,
		final String	int_param_name,
		boolean			enabled )
	{
		int	value = COConfigurationManager.getIntParameter(int_param_name);
		
		final UPnPMapping	mapping = addMapping( name_resource, tcp, value, enabled );
		
		COConfigurationManager.addParameterListener(
				int_param_name,
				new ParameterListener()
				{
					public void
					parameterChanged(
						String	name )
					{
						mapping.setPort( COConfigurationManager.getIntParameter(int_param_name));
					}
				});
		
		return( mapping );
	}
	
	protected UPnPMapping
	addConfigPort(
		String			name_resource,
		boolean			tcp,
		final String	enabler_param_name,
		final String	int_param_name )
	{
		boolean	enabled = COConfigurationManager.getBooleanParameter(enabler_param_name);
		
		final UPnPMapping	mapping = addConfigPort( name_resource, tcp, int_param_name, enabled );
		
		COConfigurationManager.addParameterListener(
				enabler_param_name,
				new ParameterListener()
				{
					public void
					parameterChanged(
						String	name )
					{
						mapping.setEnabled( COConfigurationManager.getBooleanParameter(enabler_param_name));
					}
				});
		
		return( mapping );
	}
	
	public UPnPMapping
	addMapping(
		String		desc_resource,
		boolean		tcp,
		int			port,
		boolean		enabled )
	{
		// System.out.println( "UPnPMappingManager: added '" + desc_resource + "'" + (tcp?"TCP":"UDP") + "/" + port + ", enabled = " + enabled );
		
		UPnPMapping	mapping = new UPnPMapping(desc_resource, tcp, port, enabled );
		
		mappings.add( mapping );
		
		added( mapping );
		
		return( mapping );
	}
	
	public UPnPMapping[]
	getMappings()
	{
		UPnPMapping[]		res = new UPnPMapping[mappings.size()];
		
		mappings.toArray( res );
		
		return( res );
	}
	
	protected void
	added(
		UPnPMapping		mapping )
	{
		for (int i=0;i<listeners.size();i++){
			
			((UPnPMappingManagerListener)listeners.get(i)).mappingAdded( mapping );
		}
	}
	
	public void
	addListener(
			UPnPMappingManagerListener	l )
	{
		listeners.add(l);
	}
	
	public void
	removeListener(
			UPnPMappingManagerListener	l )
	{
		listeners.remove(l);
	}
}
