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
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.platform.PlatformManager;
import org.gudy.azureus2.platform.PlatformManagerCapabilities;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.platform.PlatformManagerPingCallback;
import org.gudy.azureus2.plugins.platform.PlatformManagerException;

import com.aelitis.azureus.core.networkmanager.admin.NetworkAdmin;
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

public class 
NetworkAdminImpl
	extends NetworkAdmin
{
	private static final LogIDs LOGID = LogIDs.NWMAN;
	
	private Set			old_network_interfaces;
	private InetAddress	old_bind_ip;
	
	private CopyOnWriteList	listeners = new CopyOnWriteList();
		
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
			// TODO: tidy up
		
		NetworkAdminProtocol[]	res = 
			{
				new NetworkAdminProtocolImpl( NetworkAdminProtocol.PT_HTTP, 0 ),
				new NetworkAdminProtocolImpl( NetworkAdminProtocol.PT_TCP, 0 ),
				new NetworkAdminProtocolImpl( NetworkAdminProtocol.PT_UDP, 0 ),
			};
		      
		return( res );
	}
 	
 	public NetworkAdminProtocol[]
 	getInboundProtocols()
 	{
 			// 	 TODO: tidy up
 		
		NetworkAdminProtocol[]	res = 
			{
				new NetworkAdminProtocolImpl( NetworkAdminProtocol.PT_HTTP, HTTPNetworkManager.getSingleton().getHTTPListeningPortNumber()),
				new NetworkAdminProtocolImpl( NetworkAdminProtocol.PT_TCP, TCPNetworkManager.getSingleton().getTCPListeningPortNumber()),
				new NetworkAdminProtocolImpl( NetworkAdminProtocol.PT_UDP, UDPNetworkManager.getSingleton().getUDPListeningPortNumber()),
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
		return( new NetworkAdminNATDevice[0] );
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
		
		NetworkAdminNetworkInterface[] interfaces = getInterfaces();
		
		for (int i=0;i<interfaces.length;i++){
			
			networkInterface	interf = (networkInterface)interfaces[i];
			
			iw.indent();
			
			try{
				
				interf.generateDiagnostics( iw );
			}finally{
				
				iw.exdent();
			}
		}
		
		iw.println( "Outbound protocols: default routing" );
		
		NetworkAdminProtocol[]	protocols = getOutboundProtocols();
		
		for (int i=0;i<protocols.length;i++){
			
			NetworkAdminProtocol	protocol = protocols[i];
			
			iw.println( "    " + protocol.getName() + " - " + testProtocol( protocol ));
		}
		
		iw.println( "Inbound protocols: default routing" );
		
		protocols = getInboundProtocols();
		
		for (int i=0;i<protocols.length;i++){
			
			NetworkAdminProtocol	protocol = protocols[i];
			
			iw.println( "    " + protocol.getName() + " - " + testProtocol( protocol ));
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
			IndentWriter iw )
		{
			iw.println( getDisplayName() + "/" + getName());
			
			NetworkAdminNetworkInterfaceAddress[] addresses = getAddresses();
			
			for (int i=0;i<addresses.length;i++){
				
				networkAddress	addr = (networkAddress)addresses[i];
				
				iw.indent();
				
				try{
					
					addr.generateDiagnostics( iw );
					
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
				IndentWriter iw )
			{
				iw.println( "" + getAddress());
				
				try{
					iw.println( "  Trace route" );
					
					iw.indent();
					
					if ( isLoopback()){
						
						iw.println( "Loopback - ignoring" );
						
					}else{
						
						try{
							NetworkAdminNode[] nodes = getRoute( InetAddress.getByName("www.google.com"), 30000, null );
							
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
							
							iw.println( "    " + protocol.getName() + " - " + testProtocol( protocol ));
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
		boolean	TEST_HTTP_PROXY		= true;
		
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
			    
			    byte[]	test = new byte[16];
			    
			    System.out.println( "base 64: " + new String( Base64.encode( ByteFormatter.decodeString( "4e544c4d53535000020000000000000000000000020200000123456789abcdef" ) )));
			    
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
			
			getSingleton().generateDiagnostics( iw );
			
		}catch( Throwable e){
			
			e.printStackTrace();
		}
	}
}
