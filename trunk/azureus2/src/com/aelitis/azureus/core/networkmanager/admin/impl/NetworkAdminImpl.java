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
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.PasswordAuthentication;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.logging.LogAlert;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.AEDiagnostics;
import org.gudy.azureus2.core3.util.AEDiagnosticsEvidenceGenerator;
import org.gudy.azureus2.core3.util.AEMonitor;
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
import com.aelitis.azureus.core.instancemanager.AZInstance;
import com.aelitis.azureus.core.instancemanager.AZInstanceManager;
import com.aelitis.azureus.core.instancemanager.AZInstanceManagerListener;
import com.aelitis.azureus.core.instancemanager.AZInstanceTracked;
import com.aelitis.azureus.core.networkmanager.admin.*;
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
	implements AEDiagnosticsEvidenceGenerator
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
		
	private static final int ASN_MIN_CHECK = 30*60*1000;
	
	private long last_asn_lookup_time;
	
	private List asn_ips_checked = new ArrayList(0);
	
		
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
		
		AEDiagnostics.addEvidenceGenerator( this );
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
	
			if ( bind_ip.length() == 0 && old_bind_ip == null ){
				
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
			
			Enumeration addresses = ni.getInetAddresses();

			if (addresses.hasMoreElements()) {
				str += (str.length()==0?"":",") + ni.getName() + "=";
			}
			
			int	add_num = 0;
			
			while( addresses.hasMoreElements()){
				
				add_num++;
				
				InetAddress	ia = (InetAddress)addresses.nextElement();
				
				str += (add_num==1?"":";") + ia.getHostAddress();
			}
		}
		
		return( str );
	}
	
	public boolean
	hasIPV4Potential()
	{
		Set	interfaces = old_network_interfaces;
		
		if ( interfaces == null ){
			
			return( false );
		}
		
		Iterator	it = interfaces.iterator();
				
		while( it.hasNext()){
			
			NetworkInterface ni = (NetworkInterface)it.next();
			
			Enumeration addresses = ni.getInetAddresses();

			while( addresses.hasMoreElements()){
								
				InetAddress	ia = (InetAddress)addresses.nextElement();
				
				if ( ia.isLoopbackAddress()){
					
					continue;
				}
				
				if ( ia instanceof Inet4Address ){
					
					return( true );
				}
			}
		}
		
		return( false );	
	}
	
	public boolean
	hasIPV6Potential()
	{
		Set	interfaces = old_network_interfaces;
		
		if ( interfaces == null ){
			
			return( false );
		}
		
		Iterator	it = interfaces.iterator();
				
		while( it.hasNext()){
			
			NetworkInterface ni = (NetworkInterface)it.next();
			
			Enumeration addresses = ni.getInetAddresses();

			while( addresses.hasMoreElements()){
								
				InetAddress	ia = (InetAddress)addresses.nextElement();
				
				if ( ia.isLoopbackAddress()){
					
					continue;
				}
				
				if ( ia instanceof Inet6Address ){
					
					Inet6Address v6 = (Inet6Address)ia;
					
					if ( !v6.isLinkLocalAddress()){
	
						return( true );
					}
				}
			}
		}
		
		return( false );			
	}
	
	protected void
	firePropertyChange(
		String	property )
	{
		Iterator it = listeners.iterator();
		
		while( it.hasNext()){
			
			try{
				((NetworkAdminPropertyChangeListener)it.next()).propertyChanged( property );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
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

		List	protocols = new ArrayList();
		
		TCPNetworkManager	tcp_manager = TCPNetworkManager.getSingleton();
		
		if ( tcp_manager.isTCPListenerEnabled()){
			
			protocols.add( 
					new NetworkAdminProtocolImpl( 
							azureus_core, 
							NetworkAdminProtocol.PT_TCP, 
							tcp_manager.getTCPListeningPortNumber()));
		}

		UDPNetworkManager	udp_manager = UDPNetworkManager.getSingleton();
		
		int	done_udp = -1;
		
		if ( udp_manager.isUDPListenerEnabled()){
			
			protocols.add( 
					new NetworkAdminProtocolImpl( 
							azureus_core, 
							NetworkAdminProtocol.PT_UDP, 
							done_udp = udp_manager.getUDPListeningPortNumber()));
		}
		
		if ( udp_manager.isUDPNonDataListenerEnabled()){

			int	port = udp_manager.getUDPNonDataListeningPortNumber();
			
			if ( port != done_udp ){
				
				protocols.add( 
						new NetworkAdminProtocolImpl( 
								azureus_core, 
								NetworkAdminProtocol.PT_UDP, 
								done_udp = udp_manager.getUDPNonDataListeningPortNumber()));
	
			}
		}
		
		HTTPNetworkManager	http_manager = HTTPNetworkManager.getSingleton();
		
		if ( http_manager.isHTTPListenerEnabled()){
			
			protocols.add( 
					new NetworkAdminProtocolImpl( 
							azureus_core, 
							NetworkAdminProtocol.PT_HTTP, 
							http_manager.getHTTPListeningPortNumber()));
		}
	      
		return((NetworkAdminProtocol[])protocols.toArray( new NetworkAdminProtocol[protocols.size()]));
 	}
 	
	public InetAddress
	testProtocol(
		NetworkAdminProtocol	protocol )
	
		throws NetworkAdminException
	{
		return( protocol.test( null ));
	}
	   
	public NetworkAdminSocksProxy[]
	getSocksProxies()
	{
		String host = System.getProperty( "socksProxyHost", "" ).trim();
		String port = System.getProperty( "socksProxyPort", "" ).trim();
        
		String user 		= System.getProperty("java.net.socks.username", "" ).trim();
		String password 	= System.getProperty("java.net.socks.password", "").trim();

		List	res = new ArrayList();
		
		NetworkAdminSocksProxyImpl	p1 = new NetworkAdminSocksProxyImpl( host, port, user, password );
		
		if ( p1.isConfigured()){
		
			res.add( p1 );
		}
		
		if ( 	COConfigurationManager.getBooleanParameter( "Proxy.Data.Enable" ) &&
				!COConfigurationManager.getBooleanParameter( "Proxy.Data.Same" )){
			
			host 	= COConfigurationManager.getStringParameter( "Proxy.Data.Host" );
			port	= COConfigurationManager.getStringParameter( "Proxy.Data.Port" );
			user	= COConfigurationManager.getStringParameter( "Proxy.Data.Username" );
			
			if ( user.trim().equalsIgnoreCase("<none>")){
				user = "";
			}
			password = COConfigurationManager.getStringParameter( "Proxy.Data.Password" );
			
			NetworkAdminSocksProxyImpl	p2 = new NetworkAdminSocksProxyImpl( host, port, user, password );
			
			if ( p2.isConfigured()){
			
				res.add( p2 );
			}			
		}

		return((NetworkAdminSocksProxy[])res.toArray(new NetworkAdminSocksProxy[res.size()]));
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
	
	public NetworkAdminASN 
	getCurrentASN() 
	{
		List	asns = COConfigurationManager.getListParameter( "ASN Details", new ArrayList());
		
		if ( asns.size() == 0 ){
	
				// migration from when we only persisted a single AS
			
			String as 	= "";
			String asn 	= "";
			String bgp 	= "";

			try{
				as 		= COConfigurationManager.getStringParameter( "ASN AS" );
				asn 	= COConfigurationManager.getStringParameter( "ASN ASN" );
				bgp 	= COConfigurationManager.getStringParameter( "ASN BGP" );
					
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
			
			COConfigurationManager.removeParameter( "ASN AS" );
			COConfigurationManager.removeParameter( "ASN ASN" );
			COConfigurationManager.removeParameter( "ASN BGP" );
			COConfigurationManager.removeParameter( "ASN Autocheck Performed Time" );
			
			asns.add(ASNToMap(new NetworkAdminASNImpl(as, asn, bgp )));
			
			COConfigurationManager.setParameter( "ASN Details", asns );
		}
		
		if ( asns.size() > 0 ){
			
			Map	m = (Map)asns.get(0);
			
			return( ASNFromMap( m ));
		}
		
		return( new NetworkAdminASNImpl( "", "", "" ));
	}
	
	protected Map
	ASNToMap(
		NetworkAdminASNImpl	x )
	{
		Map	m = new HashMap();
		
		byte[]	as	= new byte[0];
		byte[]	asn	= new byte[0];
		byte[]	bgp	= new byte[0];
		
		try{	
			as	= x.getAS().getBytes("UTF-8");
			asn	= x.getASName().getBytes("UTF-8");
			bgp	= x.getBGPPrefix().getBytes("UTF-8");
	
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
		
		m.put( "as", as );
		m.put( "name", asn );
		m.put( "bgp", bgp );
		
		return( m );
	}
	
	protected NetworkAdminASNImpl
	ASNFromMap(
		Map	m )
	{
		String	as		= "";
		String	asn		= "";
		String	bgp		= "";
		
		try{
			as	= new String((byte[])m.get("as"),"UTF-8");
			asn	= new String((byte[])m.get("name"),"UTF-8");
			bgp	= new String((byte[])m.get("bgp"),"UTF-8");
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
		
		return( new NetworkAdminASNImpl( as, asn, bgp ));
	}
	
	public NetworkAdminASN
	lookupASN(
		InetAddress		address )
	
		throws NetworkAdminException
	{
		System.out.println( "lookupASN: " + address );
		
		NetworkAdminASN current = getCurrentASN();
		
		if ( current.matchesCIDR( address )){
			
			return( current );
		}
		
		List	asns = COConfigurationManager.getListParameter( "ASN Details", new ArrayList());

		for (int i=0;i<asns.size();i++){
			
			Map	m = (Map)asns.get(i);
			
			NetworkAdminASN x = ASNFromMap( m );
			
			if ( x.matchesCIDR( address )){
				
				asns.remove(i);
				
				asns.add( 0, m );
				
				firePropertyChange( PR_AS );
				
				return( x );
			}
		}
		
		if ( asn_ips_checked.contains( address )){
			
			return( current );
		}
				
		long now = SystemTime.getCurrentTime();

		if ( now < last_asn_lookup_time || now - last_asn_lookup_time > ASN_MIN_CHECK ){

			last_asn_lookup_time	= now;

			NetworkAdminASNLookupImpl lookup = new NetworkAdminASNLookupImpl( address );

			NetworkAdminASNImpl x = lookup.lookup();
			
			asn_ips_checked.add( address );
			
			asns.add( 0, ASNToMap( x ));
			
			firePropertyChange( PR_AS );

			return( x );
		}
		
		return( current );
	}
		
	public void
	runInitialChecks()
	{
		AZInstanceManager i_man = AzureusCoreFactory.getSingleton().getInstanceManager();
		
		final AZInstance	my_instance = i_man.getMyInstance();
		
		i_man.addListener(
			new AZInstanceManagerListener()
			{
				private InetAddress external_address;
				
				public void
				instanceFound(
					AZInstance		instance )
				{
				}
				
				public void
				instanceChanged(
					AZInstance		instance )
				{
					if ( instance == my_instance ){
						
						InetAddress address = instance.getExternalAddress();
						
						if ( external_address == null || !external_address.equals( address )){
							
							external_address = address;
							
							try{
								lookupASN( address );
								
							}catch( Throwable e ){
								
								Debug.printStackTrace(e);
							}
						}
					}
				}
				
				public void
				instanceLost(
					AZInstance		instance )
				{
				}
				
				public void
				instanceTracked(
					AZInstanceTracked	instance )
				{
				}
			});
		
		if ( COConfigurationManager.getBooleanParameter( "Proxy.Check.On.Start" )){
			
			NetworkAdminSocksProxy[]	socks = getSocksProxies();
		
			for (int i=0;i<socks.length;i++){
				
				NetworkAdminSocksProxy	sock = socks[i];
				
				try{
					sock.getVersionsSupported();
			
				}catch( Throwable e ){
				
					Debug.printStackTrace( e );
					
					Logger.log(
						new LogAlert(
							true,
							LogAlert.AT_WARNING,
							"Socks proxy " + sock.getName() + " check failed: " + Debug.getNestedExceptionMessage( e )));
				}
			}
			
			NetworkAdminHTTPProxy http_proxy = getHTTPProxy();
			
			if ( http_proxy != null ){
			
				try{

					http_proxy.getDetails();
					
				}catch( Throwable e ){
					
					Debug.printStackTrace( e );
					
					Logger.log(
						new LogAlert(
							true,
							LogAlert.AT_WARNING,
							"HTTP proxy " + http_proxy.getName() + " check failed: " + Debug.getNestedExceptionMessage( e )));
				}
			}
		}

        NetworkAdminSpeedTestScheduler nast = NetworkAdminSpeedTestSchedulerImpl.getInstance();
        
        nast.initialise();
    }
	
	public void
	addPropertyChangeListener(
		NetworkAdminPropertyChangeListener	listener )
	{
		listeners.add( listener );
	}
	
	public void
	addAndFirePropertyChangeListener(
		NetworkAdminPropertyChangeListener	listener )
	{
		listeners.add( listener );
		
		for (int i=0;i<NetworkAdmin.PR_NAMES.length;i++){
			
			try{
				listener.propertyChanged( PR_NAMES[i] );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	
	public void
	removePropertyChangeListener(
		NetworkAdminPropertyChangeListener	listener )
	{
		listeners.remove( listener );
	}
	
	public void
	generate(
		IndentWriter		writer )
	{
		writer.println( "Network Admin" );
		
		try{
			writer.indent();
			
			NetworkAdminHTTPProxy	proxy = getHTTPProxy();
			
			if ( proxy == null ){
				
				writer.println( "HTTP proxy: none" );
				
			}else{
				
				writer.println( "HTTP proxy: " + proxy.getName());
				
				try{
					
					NetworkAdminHTTPProxy.Details details = proxy.getDetails();
					
					writer.println( "    name: " + details.getServerName());
					writer.println( "    resp: " + details.getResponse());
					writer.println( "    auth: " + details.getAuthenticationType());
					
				}catch( NetworkAdminException e ){
					
					writer.println( "    failed: " + e.getLocalizedMessage());
				}
			}
			
			NetworkAdminSocksProxy[]	socks = getSocksProxies();
			
			if ( socks.length == 0 ){
				
				writer.println( "Socks proxy: none" );
				
			}else{
				
				for (int i=0;i<socks.length;i++){
					
					NetworkAdminSocksProxy	sock = socks[i];
					
					writer.println( "Socks proxy: " + sock.getName());
					
					try{
						String[] versions = sock.getVersionsSupported();
						
						String	str = "";
						
						for (int j=0;j<versions.length;j++){
							
							str += (j==0?"":",") + versions[j];
						}
						
						writer.println( "   version: " + str );
						
					}catch( NetworkAdminException e ){
						
						writer.println( "    failed: " + e.getLocalizedMessage());
					}
				}
			}
			
			NetworkAdminNATDevice[]	nat_devices = getNATDevices();
			
			writer.println( "NAT Devices: " + nat_devices.length );
			
			for (int i=0;i<nat_devices.length;i++){
				
				NetworkAdminNATDevice	device = nat_devices[i];
				
				writer.println( "    " + device.getName() + ",address=" + device.getAddress().getHostAddress() + ":" + device.getPort() + ",ext=" + device.getExternalAddress());
			}
			
			writer.println( "Interfaces" );
			
			writer.println( "   " + getNetworkInterfacesAsString());
			
		}finally{
	
			writer.exdent();
		}
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
		
		NetworkAdminSocksProxy[]	socks = getSocksProxies();
		
		if ( socks.length == 0 ){
			
			iw.println( "Socks proxy: none" );
			
		}else{
			
			for (int i=0;i<socks.length;i++){
				
				NetworkAdminSocksProxy	sock = socks[i];
				
				iw.println( "Socks proxy: " + sock.getName());
				
				try{
					String[] versions = sock.getVersionsSupported();
					
					String	str = "";
					
					for (int j=0;j<versions.length;j++){
						
						str += (j==0?"":",") + versions[j];
					}
					
					iw.println( "   version: " + str );
					
				}catch( NetworkAdminException e ){
					
					iw.println( "    failed: " + e.getLocalizedMessage());
				}
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
		
		iw.println( "Inbound protocols: default routing" );
		
		NetworkAdminProtocol[]	protocols = getInboundProtocols();
		
		for (int i=0;i<protocols.length;i++){
			
			NetworkAdminProtocol	protocol = protocols[i];
			
			try{
				InetAddress	ext_addr = testProtocol( protocol );
	
				if ( ext_addr != null ){
					
					public_addresses.add( ext_addr );
				}
	
				iw.println( "    " + protocol.getName() + " - " + ext_addr );
				
			}catch( NetworkAdminException e ){
				
				iw.println( "    " + protocol.getName() + " - " + Debug.getNestedExceptionMessage(e));
			}
		}
		
		iw.println( "Outbound protocols: default routing" );
		
		protocols = getOutboundProtocols();
		
		for (int i=0;i<protocols.length;i++){
			
			NetworkAdminProtocol	protocol = protocols[i];
			
			try{

				InetAddress	ext_addr = testProtocol( protocol );
				
				if ( ext_addr != null ){
				
					public_addresses.add( ext_addr );
				}
				
				iw.println( "    " + protocol.getName() + " - " + ext_addr );
				
			}catch( NetworkAdminException e ){
				
				iw.println( "    " + protocol.getName() + " - " + Debug.getNestedExceptionMessage(e));
			}
		}
		
		Iterator	it = public_addresses.iterator();
		
		iw.println( "Public Addresses" );
		
		while( it.hasNext()){
			
			InetAddress	pub_address = (InetAddress)it.next();
			
			try{
				NetworkAdminASN	res = lookupASN( pub_address );
				
				iw.println( "    " + pub_address.getHostAddress() + " -> " + res.getAS() + "/" + res.getASName());
				
			}catch( Throwable e ){
				
				iw.println( "    " + pub_address.getHostAddress() + " -> " + Debug.getNestedExceptionMessage(e));
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
			
				throws NetworkAdminException
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
							
							try{
								InetAddress	res = testProtocol( protocol );
								
								if ( res != null ){
									
									public_addresses.add( res );
								}
								
								iw.println( "    " + protocol.getName() + " - " + res );
								
							}catch( NetworkAdminException e ){
								
								iw.println( "    " + protocol.getName() + " - " + Debug.getNestedExceptionMessage(e));
							}
						}
						
						iw.println( "Inbound protocols: bound" );
						
						protocols = getInboundProtocols();
						
						for (int i=0;i<protocols.length;i++){
							
							NetworkAdminProtocol	protocol = protocols[i];
							
							try{
								InetAddress	res = testProtocol( protocol );
								
								if ( res != null ){
									
									public_addresses.add( res );
								}
								
								iw.println( "    " + protocol.getName() + " - " + res );
								
							}catch( NetworkAdminException e ){
								
								iw.println( "    " + protocol.getName() + " - " + Debug.getNestedExceptionMessage(e));
							}
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
	
	protected void
	generateDiagnostics(
		IndentWriter			iw,
		NetworkAdminProtocol[]	protocols )
	{
		for (int i=0;i<protocols.length;i++){
			
			NetworkAdminProtocol	protocol = protocols[i];
			
			iw.println( "Testing " + protocol.getName());
			
			try{
				InetAddress	ext_addr = testProtocol( protocol );
	
				iw.println( "    -> OK, public address=" + ext_addr );
				
			}catch( NetworkAdminException e ){
				
				iw.println( "    -> Failed: " + Debug.getNestedExceptionMessage(e));
			}
		}
	}
	
	public void
	logNATStatus(
		IndentWriter		iw )
	{
		generateDiagnostics( iw, getInboundProtocols());
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
			
			getSingleton().logNATStatus( iw );
			
		}catch( Throwable e){
			
			e.printStackTrace();
		}
	}
}
