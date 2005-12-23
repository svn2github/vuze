/*
 * Created on 20-Dec-2005
 * Created by Paul Gardner
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.core.instancemanager.impl;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.utils.UTTimer;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.instancemanager.AZInstance;
import com.aelitis.azureus.core.instancemanager.AZInstanceManager;
import com.aelitis.azureus.plugins.dht.DHTPlugin;
import com.aelitis.net.upnp.UPnPFactory;
import com.aelitis.net.upnp.UPnPSSDP;
import com.aelitis.net.upnp.UPnPSSDPAdapter;
import com.aelitis.net.upnp.UPnPSSDPListener;

public class 
AZInstanceManagerImpl 
	implements AZInstanceManager, UPnPSSDPListener
{
	private static final LogIDs LOGID = LogIDs.NET;
	
	private String				SSDP_GROUP_ADDRESS 	= "239.255.068.250";	// 239.255.000.000-239.255.255.255 
	private int					SSDP_GROUP_PORT		= 16680;				//
	private int					SSDP_CONTROL_PORT	= 16679;

	private static AZInstanceManagerImpl	singleton;
	
	private static AEMonitor	class_mon = new AEMonitor( "AZInstanceManager:class" );
	
	public static AZInstanceManager
	getSingleton(
		AzureusCore	core )
	{
		try{
			class_mon.enter();
			
			if ( singleton == null ){
				
				singleton = new AZInstanceManagerImpl( core );
			}
		}finally{
			
			class_mon.exit();
		}
		
		return( singleton );
	}
	
	private AzureusCore	core;
	private UPnPSSDP 	ssdp;
	private String		my_instance_id;
	private long		search_id_next;
	private List		requests = new ArrayList();
	
	private AEMonitor	this_mon = new AEMonitor( "AZInstanceManager" );

	protected
	AZInstanceManagerImpl(
		AzureusCore	_core )
	{
		core			= _core;
		
		my_instance_id	= COConfigurationManager.getStringParameter( "ID", "" );
		
		if ( my_instance_id.length() == 0 ){
			
			my_instance_id	= "" + SystemTime.getCurrentTime();
		}
		
		final PluginInterface	pi = core.getPluginManager().getDefaultPluginInterface();
		
		try{
			ssdp = 
				UPnPFactory.getSSDP( 
					new UPnPSSDPAdapter()
					{
						public UTTimer
						createTimer(
							String	name )
						{
							return( pi.getUtilities().createTimer( name ));
						}
		
						public void
						createThread(
							String		name,
							AERunnable	runnable )
						{
							pi.getUtilities().createThread( name, runnable );
						}
						
						public void
						trace(
							Throwable	e )
						{
							Debug.printStackTrace( e );
							
							Logger.log(new LogEvent(LOGID, "SSDP: failed ", e)); 

						}
						
						public void
						trace(
							String	str )
						{
							if ( Logger.isEnabled()){
								
								Logger.log(new LogEvent( LOGID, str )); 
							}
						}
					},
					SSDP_GROUP_ADDRESS,
					SSDP_GROUP_PORT,
					SSDP_CONTROL_PORT );
			
			ssdp.addListener( this );
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
	
	public void
	receivedResult(
		NetworkInterface	network_interface,
		InetAddress			local_address,
		InetAddress			originator,
		URL					location,
		String				ST,
		String				AL )
	{
		if ( ST.startsWith("azureus:") && AL != null ){
			
			StringTokenizer	tok = new StringTokenizer( ST, ":" );
			
			tok.nextToken();	// az
			
			tok.nextToken();	// command 

			String	az_id = tok.nextToken();	// az id
			
			if ( az_id.equals( my_instance_id )){
			
				long search_id = Long.parseLong( tok.nextToken());	// search id
				
				try{
					this_mon.enter();
					
					for (int i=0;i<requests.size();i++){
						
						request	req = (request)requests.get(i);
						
						if ( req.getID() == search_id ){
							
							req.addReply( originator, AL );
						}
					}
				}finally{
					
					this_mon.exit();
				}
			}
		}
	}
	
	public String
	receivedSearch(
		NetworkInterface	network_interface,
		InetAddress			local_address,
		InetAddress			originator,
		String				user_agent,
		String				ST )
	{
		System.out.println( "got search:" + user_agent + "/" + ST );
		
		if ( ST.startsWith("azureus:")){
			
			StringTokenizer	tok = new StringTokenizer( ST, ":" );
			
			tok.nextToken();	// az
			
			String	command = tok.nextToken();

			String az_id = tok.nextToken();	// az id
			
			if ( !az_id.equals(my_instance_id )){
				
				String	internal_address = COConfigurationManager.getStringParameter("Bind IP");
				
				if ( internal_address.length() < 7 ){
					
					internal_address = "*";
				}
				
				String	external_address = null;
				
				int	udp_port	= 0;
				
				try{
				    PluginInterface dht_pi = core.getPluginManager().getPluginInterfaceByClass( DHTPlugin.class );
				        
			        	// may not be present
			        	
			        if ( dht_pi != null ){
			        	
			        	DHTPlugin dht = (DHTPlugin)dht_pi.getPlugin();
			             
			        	udp_port = dht.getPort();
			        	
			        	external_address = dht.getLocalAddress().getAddress().getAddress().getHostAddress();
			        }
				}catch( Throwable e ){
				}
				
				if ( external_address == null ){
					
					InetAddress ia = core.getPluginManager().getDefaultPluginInterface().getUtilities().getPublicAddress();
					
					if ( ia != null ){
						
						external_address = ia.getHostAddress();
						
					}else{
						
						external_address = "127.0.0.1";
					}
				}
								
				String	reply = my_instance_id;				

				reply += ":" + mapAddress(internal_address);
				
				reply += ":" + mapAddress(external_address);
				
				reply += ":" + COConfigurationManager.getIntParameter("TCP.Listen.Port");
				
		        reply += ":" + udp_port;
				
				if ( command.equals("btih")){
					
					
				}else if ( command.equals( "instance" )){
					
					return( reply );
				}
			}
		}
		
		return( null );
	}
	
	public void
	receivedNotify(
		NetworkInterface	network_interface,
		InetAddress			local_address,
		InetAddress			originator,
		URL					location,
		String				NT,
		String				NTS )
	{
			// not interested
	}

	public AZInstance
	getMyInstance()
	{
		return( null );
	}
	
	public AZInstance[]
	getOtherInstances()
	{
		if ( ssdp == null ){
			
			return( new AZInstance[0]);
		}
		
		request req = sendRequest( "instance" );
		
		return( req.getReply());
	}
	
	public AZInstance[]
	getInstancesForTorrent(
		TOTorrent	torrent )
	{
		if ( ssdp == null ){
			
			return( new AZInstance[0]);
		}
		
		request req = sendRequest( "btih" );
		
		return( req.getReply());
	}
	
	protected request
	sendRequest(
		String	type )
	{
		return( new request( type ));
	}
	
	protected class
	request
	{
		private long	id;
		
		private List	replies = new ArrayList();
		
		protected
		request(
			String		_type )
		{
			try{
				this_mon.enter();

				id	= search_id_next++;
						
				requests.add( this );
	
			}finally{
				
				this_mon.exit();
			}
			
			String	st = "azureus:" + _type + ":" + my_instance_id + ":" + id;
			
			ssdp.search( "azureus:sdsd", st );
		}
		
		protected long
		getID()
		{
			return( id );
		}
		
		protected void
		addReply(
			InetAddress	internal_address,
			String		AL )
		{
			try{
				this_mon.enter();
			
				StringTokenizer	tok = new StringTokenizer( AL, ":" );
				
				String	instance_id = tok.nextToken();
				String	int_ip		= unmapAddress(tok.nextToken());
				String	ext_ip		= unmapAddress(tok.nextToken());
				int		tcp			= Integer.parseInt(tok.nextToken());
				int		udp			= Integer.parseInt(tok.nextToken());
				
				for (int i=0;i<replies.size();i++){
					
					AZInstance	rep = (AZInstance)replies.get(i);
					
					if ( rep.getID().equals( instance_id )){
						
						return;
					}
				}
				
				try{
					if ( !int_ip.equals("*")){
						
						internal_address = InetAddress.getByName( int_ip );
					}
		
					InetAddress	external_address = InetAddress.getByName( ext_ip );
					
					AZInstance	inst = new AZInstanceImpl(instance_id, internal_address, external_address, tcp, udp );
					
					replies.add( inst );
					
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
			}finally{
				
				this_mon.exit();
			}
		}
		
		protected AZInstance[]
		getReply()
		{
			try{
				Thread.sleep( 2500 );
				
			}catch( Throwable e ){
				
			}
			try{
				this_mon.enter();

				requests.remove( this );
				
				return(( AZInstance[])replies.toArray(new AZInstance[replies.size()]));			
				
			}finally{
				
				this_mon.exit();
			}
		}
	}
	
	protected String
	mapAddress(
		String	str )
	{
		return( str.replace(':','$'));
	}
	
	protected String
	unmapAddress(
		String	str )
	{
		return( str.replace('$',':'));
	}
}
