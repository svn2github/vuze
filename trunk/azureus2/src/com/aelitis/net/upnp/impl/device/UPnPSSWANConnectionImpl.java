/*
 * Created on 15-Jun-2004
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

package com.aelitis.net.upnp.impl.device;

import java.util.*;


import com.aelitis.net.upnp.*;
import com.aelitis.net.upnp.services.UPnPWANConnectionPortMapping;

/**
 * @author parg
 *
 */

public class 
UPnPSSWANConnectionImpl 
{
	protected UPnPServiceImpl		service;
	
	protected
	UPnPSSWANConnectionImpl(
		UPnPServiceImpl		_service )
	{
		service	= _service;
	}
	
	public UPnPService
	getGenericService()
	{
		return( service );
	}
	
	public void
	addPortMapping(
		boolean		tcp,			// false -> UDP
		int			port,
		String		description )
	
		throws UPnPException
	{
		UPnPAction act = service.getAction( "AddPortMapping" );
		
		UPnPActionInvocation inv = act.getInvocation();
		
		inv.addArgument( "NewRemoteHost", 				"" );		// "" = wildcard for hosts, 0 = wildcard for ports
		inv.addArgument( "NewExternalPort", 			"" + port );
		inv.addArgument( "NewProtocol", 				tcp?"TCP":"UDP" );
		inv.addArgument( "NewInternalPort", 			"" + port );
		inv.addArgument( "NewInternalClient",			service.getDevice().getRootDevice().getLocalAddress().getHostAddress());
		inv.addArgument( "NewEnabled", 					"1" );
		inv.addArgument( "NewPortMappingDescription", 	description );
		inv.addArgument( "NewLeaseDuration",			"0" );		// 0 -> infinite (?)
		
		inv.invoke();
		
	}
	
	public void
	deletePortMapping(
		boolean		tcp,			
		int			port )
	
		throws UPnPException
	{
		UPnPAction act = service.getAction( "DeletePortMapping" );
		
		UPnPActionInvocation inv = act.getInvocation();
		
		inv.addArgument( "NewRemoteHost", 				"" );		// "" = wildcard for hosts, 0 = wildcard for ports
		inv.addArgument( "NewProtocol", 				tcp?"TCP":"UDP" );
		inv.addArgument( "NewExternalPort", 			"" + port );
		
		inv.invoke();
	}
	
	public UPnPWANConnectionPortMapping[]
	getPortMappings()
										
		throws UPnPException
	{
		// UPnPStateVariable noe = service.getStateVariable("PortMappingNumberOfEntries");
		
		int	entries = 0; //Integer.parseInt( noe.getValue());
		
			// some routers (e.g. Gudy's) return 0 here whatever!
			// In this case take mindless approach
			// hmm, even for my router the state variable isn't accurate...
		
		UPnPAction act	= service.getAction( "GetGenericPortMappingEntry" );

		List	res = new ArrayList();
		
		for (int i=0;i<(entries==0?512:entries);i++){
					
			UPnPActionInvocation inv = act.getInvocation();

			inv.addArgument( "NewPortMappingIndex", "" + i );
			
			try{
				UPnPActionArgument[] outs = inv.invoke();
				
				int		port			= 0;
				boolean	tcp				= false;
				String	internal_host	= null;
				String	description		= "";
				
				for (int j=0;j<outs.length;j++){
					
					UPnPActionArgument	out = outs[j];
					
					String	out_name = out.getName();
					
					if ( out_name.equalsIgnoreCase("NewExternalPort")){
						
						port	= Integer.parseInt( out.getValue());
						
					}else if ( out_name.equalsIgnoreCase( "NewProtocol" )){
						
						tcp = out.getValue().equalsIgnoreCase("TCP");
			
					}else if ( out_name.equalsIgnoreCase( "NewInternalClient" )){
						
						internal_host = out.getValue();
						
					}else if ( out_name.equalsIgnoreCase( "NewPortMappingDescription" )){
						
						description = out.getValue();
					}
				}
				
				res.add( new portMapping( port, tcp, internal_host, description ));
				
			}catch( UPnPException e ){
				
				if ( entries == 0 ){
					
					break;
				}
				
				throw(e);
			}
		}
		
		UPnPWANConnectionPortMapping[]	res2= new UPnPWANConnectionPortMapping[res.size()];
		
		res.toArray( res2 );

		return( res2 );
	}
	
	protected class
	portMapping
		implements UPnPWANConnectionPortMapping
	{
		protected int			external_port;
		protected boolean		tcp;
		protected String		internal_host;
		protected String		description;
		
		protected
		portMapping(
			int			_external_port,
			boolean		_tcp,
			String		_internal_host,
			String		_description )
		{
			external_port	= _external_port;
			tcp				= _tcp;
			internal_host	= _internal_host;
			description		= _description;
		}
		
		public boolean
		isTCP()
		{
			return( tcp );
		}
		
		public int
		getExternalPort()
		{
			return( external_port );
		}
		
		public String
		getInternalHost()
		{
			return( internal_host );
		}
		
		public String
		getDescription()
		{
			return( description );
		}
	}
}
