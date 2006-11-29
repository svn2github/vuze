/*
 * Created on 1 Nov 2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */


package com.aelitis.azureus.core.networkmanager.admin.impl;

import java.io.PrintWriter;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.PasswordAuthentication;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bouncycastle.util.encoders.Base64;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.logging.LogAlert;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.platform.PlatformManager;
import org.gudy.azureus2.platform.PlatformManagerCapabilities;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.platform.PlatformManagerPingCallback;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.platform.PlatformManagerException;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdmin;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminASNLookup;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminException;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminHTTPProxy;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminNATDevice;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminNetworkInterfaceAddress;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminNetworkInterface;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminNode;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminPropertyChangeListener;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminProtocol;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminRouteListener;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSocksProxy;
import com.aelitis.azureus.core.networkmanager.impl.http.HTTPNetworkManager;
import com.aelitis.azureus.core.networkmanager.impl.tcp.TCPNetworkManager;
import com.aelitis.azureus.core.networkmanager.impl.udp.UDPNetworkManager;
import com.aelitis.azureus.core.proxy.socks.AESocksProxy;
import com.aelitis.azureus.core.proxy.socks.AESocksProxyFactory;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.plugins.upnp.UPnPPlugin;
import com.aelitis.azureus.plugins.upnp.UPnPPluginService;

public class 
NetworkAdminImpl
	extends NetworkAdmin
{
	private static final LogIDs LOGID = LogIDs.NWMAN;
	
	private Set			old_network_interfaces;
	private InetAddress	old_bind_ip;
	
	private CopyOnWriteList	listeners = new CopyOnWriteList();
		
	private NetworkAdminRouteListener
		trace_route_listener = new NetworkAdminRouteListener()
		{
			private int	node_count = 0;
			
			public boolean
			foundNode(
				NetworkAdminNode	node,
				int					distance,
				int					rtt )
			{
				node_count++;
				
				return( true );
			}
			
			public boolean
			timeout(
				int					distance )
			{
				if ( distance == 3 && node_count == 0 ){
					
					return( false );
				}
				
				return( true );
			}
		};
		
	public
	NetworkAdminImpl()
	{
		COConfigurationManager.addParameterListener(
			"Bind IP",
			new ParameterListener()
			{
				public void 
				parameterChanged(
					String parameterName )
				{
					checkDefaultBindAddress( false );
				}
			});
		
		SimpleTimer.addPeriodicEvent(
			"NetworkAdmin:checker",
			15000,
			new TimerEventPerformer()
			{
				public void 
				perform(
					TimerEvent event )
				{
					checkNetworkInterfaces( false );
				}
			});
		
			// populate initial values
		
		checkNetworkInterfaces(true);
		
		checkDefaultBindAddress(true);
	}
	
	protected void
	checkNetworkInterfaces(
		boolean	first_time )
	{
		try{
			Enumeration 	nis = NetworkInterface.getNetworkInterfaces();
		
			boolean	changed	= false;

			if ( nis == null && old_network_interfaces == null ){
				
			}else if ( nis == null ){
				
				old_network_interfaces	= null;
					
				changed = true;
					
			}else if ( old_network_interfaces == null ){
				
				Set	new_network_interfaces = new HashSet();
				
				while( nis.hasMoreElements()){

					new_network_interfaces.add( nis.nextElement());
				}
				
				old_network_interfaces = new_network_interfaces;
				
				changed = true;
				
			}else{
				
				Set	new_network_interfaces = new HashSet();
				
				while( nis.hasMoreElements()){
					
					Object	 ni = nis.nextElement();
					
						// NetworkInterface's "equals" method is based on ni name + addresses
					
					if ( !old_network_interfaces.contains( ni )){
						
						changed	= true;
					}
					
					new_network_interfaces.add( ni );
				}
					
				if ( old_network_interfaces.size() != new_network_interfaces.size()){
					
					changed = true;
				}
				
				old_network_interfaces = new_network_interfaces;
			}
			
			if ( changed ){
					
				if ( !first_time ){
					
					Logger.log(
						new LogEvent(LOGID,
								"NetworkAdmin: network interfaces have changed" ));
				}
				
				firePropertyChange( NetworkAdmin.PR_NETWORK_INTERFACES );
				
				checkDefaultBindAddress( first_time );
			}
		}catch( Throwable e ){
		}
	}
	
	public InetAddress
	getDefaultBindAddress()
	{
		return( old_bind_ip );
	}
	
	protected void
	checkDefaultBindAddress(
		boolean	first_time )
	{
		boolean	changed = false;
		
		String bind_ip = COConfigurationManager.getStringParameter("Bind IP", "").trim();

		try{
	
			if ( bind_ip.length() == 0 & old_bind_ip == null ){
				
			}else if ( bind_ip.length() == 0 ){
				
				old_bind_ip = null;
				
				changed = true;
				
			}else{
			
				InetAddress new_bind_ip	= null;
				
				if ( bind_ip.indexOf('.') == -1 ){
				
						// no dots -> interface name (e.g. eth0 )
					
					Enumeration 	nis = NetworkInterface.getNetworkInterfaces();

					while( nis.hasMoreElements()){
						
						NetworkInterface	 ni = (NetworkInterface)nis.nextElement();

						if ( bind_ip.equalsIgnoreCase( ni.getName())){
							
							Enumeration addresses = ni.getInetAddresses();
							
							if ( addresses.hasMoreElements()){
								
								new_bind_ip = (InetAddress)addresses.nextElement();
							}
						}
					}
					
					if ( new_bind_ip == null ){
						
						Logger.log(
								new LogAlert(LogAlert.UNREPEATABLE,
									LogAlert.AT_ERROR, "Bind IP '" + bind_ip + "' is invalid - no matching network interfaces" ));

						return;
					}
				}else{
				
					new_bind_ip = InetAddress.getByName( bind_ip );
				}
				
				if ( old_bind_ip == null || !old_bind_ip.equals( new_bind_ip )){
					
					old_bind_ip = new_bind_ip;
					
					changed = true;
				}
			}
			
			if ( changed ){
				
				if ( !first_time ){
					
					Logger.log(
						new LogEvent(LOGID,
								"NetworkAdmin: default bind ip has changed to '" + (old_bind_ip==null?"none":old_bind_ip.getHostAddress())  + "'"));
				}
				
				firePropertyChange( NetworkAdmin.PR_DEFAULT_BIND_ADDRESS );
			}
			
		}catch( Throwable e ){
			
			Logger.log(
				new LogAlert(LogAlert.UNREPEATABLE,
					LogAlert.AT_ERROR, "Bind IP '" + bind_ip + "' is invalid" ));
			
		}
	}
	
	public String
	getNetworkInterfacesAsString()
	{
		Set	interfaces = old_network_interfaces;
		
		if ( interfaces == null ){
			
			return( "" );
		}
		
		Iterator	it = interfaces.iterator();
		
		String	str = "";
		
		while( it.hasNext()){
			
			NetworkInterface ni = (NetworkInterface)it.next();
			
			str += (str.length()==0?"":",") + ni.getName() + "=";
			
			Enumeration addresses = ni.getInetAddresses();
		
			int	add_num = 0;
			
			while( addresses.hasMoreElements()){
				
				add_num++;
				
				InetAddress	ia = (InetAddress)addresses.nextElement();
				
				str += (add_num==1?"":";") + ia.getHostAddress();
			}
		}
		
		return( str );
	}
	
	protected void
	firePropertyChange(
		String	property )
	{
		Iterator it = listeners.iterator();
		
		while( it.hasNext()){
			
			((NetworkAdminPropertyChangeListener)it.next()).propertyChanged( property );
		}
	}
	
	public NetworkAdminNetworkInterface[]
	getInterfaces()
	{
		Set	interfaces = old_network_interfaces;
		
		if ( interfaces == null ){
			
			return( new NetworkAdminNetworkInterface[0] );
		}
		
		NetworkAdminNetworkInterface[]	res = new NetworkAdminNetworkInterface[interfaces.size()];
		
		Iterator	it = interfaces.iterator();
				
		int	pos = 0;
		
		while( it.hasNext()){
			
			NetworkInterface ni = (NetworkInterface)it.next();

			res[pos++] = new networkInterface( ni );
		}
		
		return( res );
	}

	public NetworkAdminProtocol[]
 	getOutboundProtocols()
	{
		AzureusCore azureus_core = AzureusCoreFactory.getSingleton();
		

			// TODO: tidy up
		
		NetworkAdminProtocol[]	res = 
			{
				new NetworkAdminProtocolImpl( azureus_core, NetworkAdminProtocol.PT_HTTP ),
				new NetworkAdminProtocolImpl( azureus_core, NetworkAdminProtocol.PT_TCP ),
				new NetworkAdminProtocolImpl( azureus_core, NetworkAdminProtocol.PT_UDP ),
			};
		      
		return( res );
	}
 	
 	public NetworkAdminProtocol[]
 	getInboundProtocols()
 	{
		AzureusCore azureus_core = AzureusCoreFactory.getSingleton();

 			// 	 TODO: tidy up
 		
		NetworkAdminProtocol[]	res = 
			{
				new NetworkAdminProtocolImpl( azureus_core, NetworkAdminProtocol.PT_HTTP, HTTPNetworkManager.getSingleton().getHTTPListeningPortNumber()),
				new NetworkAdminProtocolImpl( azureus_core, NetworkAdminProtocol.PT_TCP, TCPNetworkManager.getSingleton().getTCPListeningPortNumber()),
				new NetworkAdminProtocolImpl( azureus_core, NetworkAdminProtocol.PT_UDP, UDPNetworkManager.getSingleton().getUDPListeningPortNumber()),
			};
	      
		return( res );
 	}
 	
	public InetAddress
	testProtocol(
		NetworkAdminProtocol	protocol )
	{
		return( protocol.test( null ));
	}
	   
	public NetworkAdminSocksProxy
	getSocksProxy()
	{
		NetworkAdminSocksProxyImpl	res = new NetworkAdminSocksProxyImpl();
		
		if ( !res.isConfigured()){
		
			res	= null;
		}
		
		return( res );
	}
	
	public NetworkAdminHTTPProxy
	getHTTPProxy()
	{
		NetworkAdminHTTPProxyImpl	res = new NetworkAdminHTTPProxyImpl();
		
		if ( !res.isConfigured()){
		
			res	= null;
		}
		
		return( res );
	}
	
	public NetworkAdminNATDevice[]
	getNATDevices()
	{
		List	devices = new ArrayList();
		
		try{
	
		    PluginInterface upnp_pi = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByClass( UPnPPlugin.class );
		    
		    if ( upnp_pi != null ){
	    	
		    	UPnPPlugin upnp = (UPnPPlugin)upnp_pi.getPlugin();
		    	
		    	UPnPPluginService[]	services = upnp.getServices();
		    	
		    	for (int i=0;i<services.length;i++){
		    		
		    		devices.add( new NetworkAdminNATDeviceImpl( services[i] ));
		    	}
		    }
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
		}
		
		return((NetworkAdminNATDevice[])devices.toArray(new NetworkAdminNATDevice[devices.size()]));
	}
	
	public NetworkAdminASNLookup
	lookupASN(
		InetAddress		address )
	
		throws NetworkAdminException
	{
		return( new NetworkAdminASNLookupImpl( address ));
	}
	
	public boolean
	matchesCIDR(
		String		cidr,
		InetAddress	address )
	
		throws NetworkAdminException
	{
		return( NetworkAdminASNLookupImpl.matchesCIDR( cidr, address ));
	}
	
	public void
	addPropertyChangeListener(
		NetworkAdminPropertyChangeListener	listener )
	{
		listeners.add( listener );
	}
	
	public void
	removePropertyChangeListener(
		NetworkAdminPropertyChangeListener	listener )
	{
		listeners.remove( listener );
	}
	
	
	public void 
	generateDiagnostics(
		IndentWriter iw )
	{
		Set	public_addresses = new HashSet();
		
		NetworkAdminHTTPProxy	proxy = getHTTPProxy();
		
		if ( proxy == null ){
			
			iw.println( "HTTP proxy: none" );
			
		}else{
			
			iw.println( "HTTP proxy: " + proxy.getName());
			
			try{
				
				NetworkAdminHTTPProxy.Details details = proxy.getDetails();
				
				iw.println( "    name: " + details.getServerName());
				iw.println( "    resp: " + details.getResponse());
				iw.println( "    auth: " + details.getAuthenticationType());
				
			}catch( NetworkAdminException e ){
				
				iw.println( "    failed: " + e.getLocalizedMessage());
			}
		}
		
		NetworkAdminSocksProxy	socks = getSocksProxy();
		
		if ( socks == null ){
			
			iw.println( "Socks proxy: none" );
			
		}else{
			
			iw.println( "Socks proxy: " + socks.getName());
			
			try{
				String[] versions = socks.getVersionsSupported();
				
				String	str = "";
				
				for (int i=0;i<versions.length;i++){
					
					str += (i==0?"":",") + versions[i];
				}
				
				iw.println( "   version: " + str );
				
			}catch( NetworkAdminException e ){
				
				iw.println( "    failed: " + e.getLocalizedMessage());
			}
		}
		
		NetworkAdminNATDevice[]	nat_devices = getNATDevices();
		
		iw.println( "NAT Devices: " + nat_devices.length );
		
		for (int i=0;i<nat_devices.length;i++){
			
			NetworkAdminNATDevice	device = nat_devices[i];
			
			iw.println( "    " + device.getName() + ",address=" + device.getAddress().getHostAddress() + ":" + device.getPort() + ",ext=" + device.getExternalAddress());
			
			public_addresses.add( device.getExternalAddress());
		}
		
		iw.println( "Interfaces" );
		
		/*
		NetworkAdminNetworkInterface[] interfaces = getInterfaces();
		
		if ( interfaces.length > 0 ){
			
			if ( interfaces.length > 1 || interfaces[0].getAddresses().length > 1 ){
				
				for (int i=0;i<interfaces.length;i++){
					
					networkInterface	interf = (networkInterface)interfaces[i];
					
					iw.indent();
					
					try{
						
						interf.generateDiagnostics( iw, public_addresses );
						
					}finally{
						
						iw.exdent();
					}
				}
			}else{
				
				if ( interfaces[0].getAddresses().length > 0 ){
					
					networkInterface.networkAddress address = (networkInterface.networkAddress)interfaces[0].getAddresses()[0];
					
					try{
						NetworkAdminNode[] nodes = address.getRoute( InetAddress.getByName("www.google.com"), 30000, trace_route_listener  );
						
						for (int i=0;i<nodes.length;i++){
							
							networkInterface.networkAddress.networkNode	node = (networkInterface.networkAddress.networkNode)nodes[i];
															
							iw.println( node.getString());
						}
					}catch( Throwable e ){
						
						iw.println( "Can't resolve host for route trace - " + e.getMessage());
					}
				}
			}
		}
		*/
		
		iw.println( "Outbound protocols: default routing" );
		
		NetworkAdminProtocol[]	protocols = getOutboundProtocols();
		
		for (int i=0;i<protocols.length;i++){
			
			NetworkAdminProtocol	protocol = protocols[i];
			
			InetAddress	ext_addr = testProtocol( protocol );
			
			if ( ext_addr != null ){
			
				public_addresses.add( ext_addr );
			}
			
			iw.println( "    " + protocol.getName() + " - " + ext_addr );
		}
		
		iw.println( "Inbound protocols: default routing" );
		
		protocols = getInboundProtocols();
		
		for (int i=0;i<protocols.length;i++){
			
			NetworkAdminProtocol	protocol = protocols[i];
			
			InetAddress	ext_addr = testProtocol( protocol );

			if ( ext_addr != null ){
				
				public_addresses.add( ext_addr );
			}

			iw.println( "    " + protocol.getName() + " - " + ext_addr );
		}
		
		Iterator	it = public_addresses.iterator();
		
		iw.println( "Public Addresses" );
		
		while( it.hasNext()){
			
			InetAddress	pub_address = (InetAddress)it.next();
			
			try{
				NetworkAdminASNLookup	res = lookupASN( pub_address );
				
				iw.println( "    " + pub_address.getHostAddress() + " -> " + res.getAS() + "/" + res.getASName());
				
			}catch( Throwable e ){
				
				iw.println( "    " + pub_address.getHostAddress() + " -> " + e.getMessage());
			}
		}
	}
	
	protected class
	networkInterface
		implements NetworkAdminNetworkInterface
	{
		private NetworkInterface		ni;
		
		protected
		networkInterface(
			NetworkInterface	_ni )
		{
			ni	= _ni;
		}
		
		public String
		getDisplayName()
		{
			return( ni.getDisplayName());
		}
		
		public String
		getName()
		{
			return( ni.getName());
		}
		
		public NetworkAdminNetworkInterfaceAddress[]
		getAddresses()
		{
				// BAH NetworkInterface has lots of goodies but is 1.6
			
			Enumeration	e = ni.getInetAddresses();
		
			List	addresses = new ArrayList();
			
			while( e.hasMoreElements()){
				
				addresses.add( new networkAddress((InetAddress)e.nextElement()));
			}
	
			return((NetworkAdminNetworkInterfaceAddress[])addresses.toArray( new NetworkAdminNetworkInterfaceAddress[addresses.size()]));
		}
	
		public void 
		generateDiagnostics(
			IndentWriter 	iw,
			Set				public_addresses )
		{
			iw.println( getDisplayName() + "/" + getName());
			
			NetworkAdminNetworkInterfaceAddress[] addresses = getAddresses();
			
			for (int i=0;i<addresses.length;i++){
				
				networkAddress	addr = (networkAddress)addresses[i];
				
				iw.indent();
				
				try{
					
					addr.generateDiagnostics( iw, public_addresses );
					
				}finally{
					
					iw.exdent();
				}
			}
		}
		

		protected class
		networkAddress
			implements NetworkAdminNetworkInterfaceAddress
		{
			private InetAddress		address;
			
			protected
			networkAddress(
				InetAddress	_address )
			{
				address = _address;
			}
			
			public InetAddress
			getAddress()
			{
				return( address );
			}
			
			public boolean
			isLoopback()
			{
				return( address.isLoopbackAddress());
			}
						
			public NetworkAdminNode[]
			getRoute(
				InetAddress						target,
				final int						max_millis,
				final NetworkAdminRouteListener	listener )
			
				throws NetworkAdminException
			{
				PlatformManager	pm = PlatformManagerFactory.getPlatformManager();
					
				if ( !pm.hasCapability( PlatformManagerCapabilities.TraceRouteAvailability )){
					
					throw( new NetworkAdminException( "No trace-route capability on platform" ));
				}
				
				final List	nodes = new ArrayList();
				
				try{
					pm.traceRoute( 
						address,
						target,
						new PlatformManagerPingCallback()
						{
							private long	start_time = SystemTime.getCurrentTime();
							
							public boolean
							reportNode(
								int				distance,
								InetAddress		address,
								int				millis )
							{
								boolean	timeout	= false;
								
								if ( max_millis >= 0 ){
												
									long	now = SystemTime.getCurrentTime();
									
									if ( now < start_time ){
										
										start_time = now;
									}
									
									if ( now - start_time >= max_millis ){
										
										timeout = true;
									}
								}
								
								NetworkAdminNode	node = null;
								
								if ( address != null ){
									
									node = new networkNode( address, distance, millis );
									
									nodes.add( node );
								}
								
								boolean	result;
								
								if ( listener == null ){
									
									result = true;
									
								}else{

									if ( node == null ){
										
										result = listener.timeout( distance );
										
									}else{
										
										result =  listener.foundNode( node, distance, millis );
									}
								}
								
								return( result && !timeout );
							}
						});
				}catch( PlatformManagerException e ){
					
					throw( new NetworkAdminException( "trace-route failed", e ));
				}
				
				return((NetworkAdminNode[])nodes.toArray( new NetworkAdminNode[nodes.size()]));
			}
			
			public InetAddress
			testProtocol(
				NetworkAdminProtocol	protocol )
			{
				return( protocol.test( this ));
			}
			
			public void 
			generateDiagnostics(
				IndentWriter 	iw,
				Set				public_addresses )
			{
				iw.println( "" + getAddress());
				
				try{
					iw.println( "  Trace route" );
					
					iw.indent();
					
					if ( isLoopback()){
						
						iw.println( "Loopback - ignoring" );
						
					}else{
						
						try{
							NetworkAdminNode[] nodes = getRoute( InetAddress.getByName("www.google.com"), 30000, trace_route_listener );
							
							for (int i=0;i<nodes.length;i++){
								
								networkNode	node = (networkNode)nodes[i];
																
								iw.println( node.getString());
							}
						}catch( Throwable e ){
							
							iw.println( "Can't resolve host for route trace - " + e.getMessage());
						}
												
						iw.println( "Outbound protocols: bound" );
						
						NetworkAdminProtocol[]	protocols = getOutboundProtocols();
						
						for (int i=0;i<protocols.length;i++){
							
							NetworkAdminProtocol	protocol = protocols[i];
							
							InetAddress	res = testProtocol( protocol );
							
							if ( res != null ){
								
								public_addresses.add( res );
							}
							
							iw.println( "    " + protocol.getName() + " - " + res );
						}
						
						iw.println( "Inbound protocols: bound" );
						
						protocols = getInboundProtocols();
						
						for (int i=0;i<protocols.length;i++){
							
							NetworkAdminProtocol	protocol = protocols[i];
							
							InetAddress	res = testProtocol( protocol );
							
							if ( res != null ){
								
								public_addresses.add( res );
							}
							
							iw.println( "    " + protocol.getName() + " - " + res );
						}
					}
				}finally{
					
					iw.exdent();
				}
			}
			
			protected class
			networkNode
				implements NetworkAdminNode
			{
				private InetAddress	address;
				private int			distance;
				private int			rtt;
				
				protected
				networkNode(
					InetAddress		_address,
					int				_distance,
					int				_millis )
				{
					address		= _address;
					distance	= _distance;
					rtt			= _millis;
				}
				
				public InetAddress
				getAddress()
				{
					return( address );
				}
				
				public boolean
				isLocalAddress()
				{
					return( address.isLinkLocalAddress() ||	address.isSiteLocalAddress()); 
				}

				public int
				getDistance()
				{
					return( distance );
				}
				
				public int
				getRTT()
				{
					return( rtt );
				}
				
				protected String
				getString()
				{
					if ( address == null ){
						
						return( "" + distance );
						
					}else{
					
						return( distance + "," + address + "[local=" + isLocalAddress() + "]," + rtt );
					}
				}
			}
		}
	}
	
	public static void
	main(
		String[]	args )
	{
		boolean	TEST_SOCKS_PROXY 	= false;
		boolean	TEST_HTTP_PROXY		= false;
		
		try{
			if ( TEST_SOCKS_PROXY ){
				
				AESocksProxy proxy = AESocksProxyFactory.create( 4567, 10000, 10000 );
				
				proxy.setAllowExternalConnections( true );
				
				System.setProperty( "socksProxyHost", "localhost" );
				System.setProperty( "socksProxyPort", "4567" );
			}
			
			if ( TEST_HTTP_PROXY ){
			   
				System.setProperty("http.proxyHost", "localhost" );
			    System.setProperty("http.proxyPort", "3128" );
			    System.setProperty("https.proxyHost", "localhost" );
			    System.setProperty("https.proxyPort", "3128" );
			    			    
				Authenticator.setDefault(
						new Authenticator()
						{
							protected AEMonitor	auth_mon = new AEMonitor( "SESecurityManager:auth");
							
							protected PasswordAuthentication
							getPasswordAuthentication()
							{
								return( new PasswordAuthentication( "fred", "bill".toCharArray()));
							}
						});

			}
			
			IndentWriter iw = new IndentWriter( new PrintWriter( System.out ));
			
			iw.setForce( true );
			
			COConfigurationManager.initialise();
			
			AzureusCoreFactory.create();
			
			getSingleton().generateDiagnostics( iw );
			
		}catch( Throwable e){
			
			e.printStackTrace();
		}
	}
}
