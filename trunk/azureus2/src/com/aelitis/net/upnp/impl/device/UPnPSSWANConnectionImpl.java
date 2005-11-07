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

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Debug;


import com.aelitis.net.upnp.*;
import com.aelitis.net.upnp.services.UPnPWANConnectionPortMapping;

/**
 * @author parg
 *
 */

public class 
UPnPSSWANConnectionImpl 
{
	private static AEMonitor	class_mon 	= new AEMonitor( "UPnPSSWANConnection" );
	private static List			services	= new ArrayList();
	
	static{
		AEThread	t = 
			new AEThread( "UPnPSSWANConnection:mappingChecker" )
			{
				public void
				runSupport()
				{
					while( true ){
						
						try{
							Thread.sleep( 5*60*1000 );
							
							List	to_check = new ArrayList();
							
							try{
								class_mon.enter();
								
								Iterator	it = services.iterator();
								
								while( it.hasNext()){
									
									UPnPSSWANConnectionImpl	s = (UPnPSSWANConnectionImpl)it.next();
								
									if ( s.getGenericService().getDevice().getRootDevice().isDestroyed()){
										
										it.remove();
										
									}else{
										
										to_check.add( s );
									}
								}
																
							}finally{
								
								class_mon.exit();
							}
							
							for (int i=0;i<to_check.size();i++){
		
								try{
									((UPnPSSWANConnectionImpl)to_check.get(i)).checkMappings();
									
								}catch( Throwable e ){
									
									Debug.printStackTrace(e);
								}
							}
						}catch( Throwable e ){
							
							Debug.printStackTrace(e);
						}
					}
				}
			};
		
		t.setDaemon( true );
		
		t.start();
	}
	
	private UPnPServiceImpl		service;
	private List				mappings	= new ArrayList();
	
		// start off true to avoid logging first of repetitive failures
	
	private boolean				last_mapping_check_failed	= true;
	
	protected
	UPnPSSWANConnectionImpl(
		UPnPServiceImpl		_service )
	{
		service	= _service;
		
		try{
			class_mon.enter();

			services.add( this );
			
		}finally{
			
			class_mon.exit();
		}
	}
	
	public UPnPService
	getGenericService()
	{
		return( service );
	}
	
	public String[]
   	getStatusInfo()
	          	
   		throws UPnPException
   	{
		UPnPAction act = service.getAction( "GetStatusInfo" );
		
		if ( act == null ){
			
			service.getDevice().getRootDevice().getUPnP().log( "Action 'GetStatusInfo' not supported, binding not established" );
			
			throw( new UPnPException( "GetStatusInfo not supported" ));
			
		}else{
					
			UPnPActionInvocation inv = act.getInvocation();
						
			UPnPActionArgument[]	args = inv.invoke();
			
			String	connection_status	= null;
			String	connection_error	= null;
			String	uptime				= null;
			
			for (int i=0;i<args.length;i++){
				
				UPnPActionArgument	arg = args[i];
			
				String	name = arg.getName();
				
				if ( name.equalsIgnoreCase("NewConnectionStatus")){
					
					connection_status = arg.getValue();
					
				}else if ( name.equalsIgnoreCase("NewLastConnectionError")){
					
					connection_error = arg.getValue();
					
				}else if ( name.equalsIgnoreCase("NewUptime")){
					
					uptime = arg.getValue();
				}
			}
			
			return( new String[]{ connection_status, connection_error, uptime });
		}		
   	}
	
	protected void
	checkMappings()
	
		throws UPnPException
	{		
		List	mappings_copy;
		
		try{
			class_mon.enter();

			mappings_copy = new ArrayList( mappings );
		
		}finally{
			
			class_mon.exit();
		}
		
		UPnPWANConnectionPortMapping[]	current = getPortMappings();

		Iterator	it = mappings_copy.iterator();
				
		while( it.hasNext()){
		
			portMapping	mapping = (portMapping)it.next();
			
			for (int j=0;j<current.length;j++){
				
				UPnPWANConnectionPortMapping	c = current[j];
				
				if ( 	c.getExternalPort() == mapping.getExternalPort() &&
						c.isTCP() 			== mapping.isTCP()){
							
					it.remove();
					
					break;
				}
			}
		}
		
		boolean	log	= false;

		if ( mappings_copy.size() > 0 ){
			
			if ( !last_mapping_check_failed ){
				
				last_mapping_check_failed	= true;
				
				log	= true;
			}
		}else{
			
			last_mapping_check_failed	= false;
		}

		it = mappings_copy.iterator();
		
		while( it.hasNext()){
			
			portMapping	mapping = (portMapping)it.next();
		
			try{
					// some routers appear to continually fail to report the mappings - avoid 
					// reporting this
				
				if ( log ){
					
					service.getDevice().getRootDevice().getUPnP().log( "Re-establishing mapping " + mapping.getString());
				}

				addPortMapping(  mapping.isTCP(), mapping.getExternalPort(), mapping.getDescription());
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	
	public void
	addPortMapping(
		boolean		tcp,			// false -> UDP
		int			port,
		String		description )
	
		throws UPnPException
	{
		UPnPAction act = service.getAction( "AddPortMapping" );
		
		if ( act == null ){
			
			service.getDevice().getRootDevice().getUPnP().log( "Action 'AddPortMapping' not supported, binding not established" );
			
		}else{
					
			UPnPActionInvocation inv = act.getInvocation();
			
			inv.addArgument( "NewRemoteHost", 				"" );		// "" = wildcard for hosts, 0 = wildcard for ports
			inv.addArgument( "NewExternalPort", 			"" + port );
			inv.addArgument( "NewProtocol", 				tcp?"TCP":"UDP" );
			inv.addArgument( "NewInternalPort", 			"" + port );
			inv.addArgument( "NewInternalClient",			service.getDevice().getRootDevice().getLocalAddress().getHostAddress());
			inv.addArgument( "NewEnabled", 					"1" );
			inv.addArgument( "NewPortMappingDescription", 	description );
			inv.addArgument( "NewLeaseDuration",			"0" );		// 0 -> infinite (?)
			
			boolean	ok = false;
			
			try{
				inv.invoke();
				
				ok	= true;
				
			}finally{
									
				((UPnPRootDeviceImpl)service.getDevice().getRootDevice()).portMappingResult(ok);
			}
			
			try{
				class_mon.enter();
			
				Iterator	it = mappings.iterator();
				
				while( it.hasNext()){
					
					portMapping	m = (portMapping)it.next();
					
					if ( m.getExternalPort() == port && m.isTCP() == tcp ){
						
						it.remove();
					}
				}
				
				mappings.add( new portMapping( port, tcp, "", description ));
				
			}finally{
				
				class_mon.exit();
			}
		}
	}
	
	public void
	deletePortMapping(
		boolean		tcp,			
		int			port )
	
		throws UPnPException
	{
		UPnPAction act = service.getAction( "DeletePortMapping" );
		
		if ( act == null ){
			
			service.getDevice().getRootDevice().getUPnP().log( "Action 'DeletePortMapping' not supported, binding not removed" );
			
		}else{	

			UPnPActionInvocation inv = act.getInvocation();
			
			inv.addArgument( "NewRemoteHost", 				"" );		// "" = wildcard for hosts, 0 = wildcard for ports
			inv.addArgument( "NewProtocol", 				tcp?"TCP":"UDP" );
			inv.addArgument( "NewExternalPort", 			"" + port );
			
			inv.invoke();
			
			try{
				class_mon.enter();

				Iterator	it = mappings.iterator();
				
				while( it.hasNext()){
					
					portMapping	mapping = (portMapping)it.next();
					
					if ( 	mapping.getExternalPort() == port && 
							mapping.isTCP() == tcp ){
						
						it.remove();
						
						break;
					}
				}
			}finally{
				
				class_mon.exit();
			}
		}
	}
	
	public UPnPWANConnectionPortMapping[]
	getPortMappings()
										
		throws UPnPException
	{
		//UPnPStateVariable noe = service.getStateVariable("PortMappingNumberOfEntries");
		//System.out.println( "NOE = " + noe.getValue());
		
		int	entries = 0; //Integer.parseInt( noe.getValue());
		
			// some routers (e.g. Gudy's) return 0 here whatever!
			// In this case take mindless approach
			// hmm, even for my router the state variable isn't accurate...
		
		UPnPAction act	= service.getAction( "GetGenericPortMappingEntry" );

		if ( act == null ){
			
			service.getDevice().getRootDevice().getUPnP().log( "Action 'GetGenericPortMappingEntry' not supported, can't enumerate bindings" );
		
			return( new UPnPWANConnectionPortMapping[0] );
			
		}else{
			List	res = new ArrayList();
			
				// I've also seen some routers loop here rather than failing when the index gets too large (they
				// seem to keep returning the last entry) - check for a duplicate entry and exit if found
			
			portMapping	prev_mapping	= null;
			
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
			
					if ( prev_mapping != null ){
						
						if ( 	prev_mapping.getExternalPort() == port &&
								prev_mapping.isTCP() == tcp ){
					
								// repeat, get out
							
							break;
						}
					}
					
					prev_mapping = new portMapping( port, tcp, internal_host, description );
					
					res.add( prev_mapping );
					
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
		
		protected String
		getString()
		{
			return( getDescription() + " [" + getExternalPort() + ":" + (isTCP()?"TCP":"UDP") + "]");
		}
	}
}
