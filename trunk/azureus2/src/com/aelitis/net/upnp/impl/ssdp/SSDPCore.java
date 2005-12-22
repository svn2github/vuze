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

package com.aelitis.net.upnp.impl.ssdp;

import java.net.*;
import java.util.*;

import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.utils.UTTimer;
import org.gudy.azureus2.plugins.utils.UTTimerEvent;
import org.gudy.azureus2.plugins.utils.UTTimerEventPerformer;

import com.aelitis.net.upnp.*;

/**
 * @author parg
 *
 */

public class 
SSDPCore 
	implements UPnPSSDP
{
	private static final LogIDs LOGID = LogIDs.NET;
	private final static String		SSDP_GROUP_ADDRESS 	= "239.255.255.250";
	private final static int		SSDP_GROUP_PORT		= 1900;
	private final static int		SSDP_CONTROL_PORT	= 8008;
	private final static int		TTL					= 4;
	
	private final static int		PACKET_SIZE		= 8192;
		
	private static final String	HTTP_VERSION	= "1.1";
	private static final String	NL				= "\r\n";
	
	
	private static InetSocketAddress group_address;
	
	static{
		try{
			group_address = new InetSocketAddress(InetAddress.getByName(SSDP_GROUP_ADDRESS), 0 );
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e );
		}
	}

	private static SSDPCore		singleton;
	private static AEMonitor	class_mon 	= new AEMonitor( "SSDPCore:class" );

	public static SSDPCore
	getSingleton(
		UPnPSSDPAdapter		adapter )
	
		throws UPnPException
	{
		try{
			class_mon.enter();
		
			if ( singleton == null ){
				
				singleton = new SSDPCore( adapter );
			}
			
			return( singleton );
			
		}finally{
			
			class_mon.exit();
		}
	}
	
	private UPnPSSDPAdapter	adapter;
	
	private boolean		first_response			= true;
	private boolean		ttl_problem_reported	= true;	// remove these diagnostic reports on win98
	private boolean		sso_problem_reported	= true; // remove these diagnostic reports on win98
	
	private List			listeners	= new ArrayList();
	
	protected AEMonitor		this_mon	= new AEMonitor( "SSDP" );

	private Map	current_registrations = new HashMap();
	
	public
	SSDPCore(
		UPnPSSDPAdapter		_adapter )
	
		throws UPnPException
	{	
		adapter	= _adapter;

		try{	
			processNetworkInterfaces( true );
		
			UTTimer timer = adapter.createTimer( "SSDP:refresher" );
			
			timer.addPeriodicEvent(
				60*1000,
				new UTTimerEventPerformer()
				{
					public void 
					perform(
						UTTimerEvent event )
					{
						try{
							processNetworkInterfaces( false );
							
						}catch( Throwable e ){
							
							Debug.printStackTrace(e);
						}
					}
				});
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
			
			throw( new UPnPException( "Failed to initialise SSDP", e ));
		}
	}
	
	protected void
	processNetworkInterfaces(
		boolean		log_ignored )
	
		throws SocketException
	{
		Map			new_registrations	= new HashMap();
		
		try{
			this_mon.enter();
			
			Enumeration network_interfaces = NetworkInterface.getNetworkInterfaces();
			
			while (network_interfaces.hasMoreElements()){
				
				final NetworkInterface network_interface = (NetworkInterface)network_interfaces.nextElement();
	
				Set old_address_set = (Set)current_registrations.get( network_interface );
					
				if ( old_address_set == null ){
				
					old_address_set	= new HashSet();
				}
				
				Set	new_address_set = new HashSet();
				
				new_registrations.put( network_interface, new_address_set );
				
				Enumeration ni_addresses = network_interface.getInetAddresses();
				
				while (ni_addresses.hasMoreElements()){
					
					final InetAddress ni_address = (InetAddress)ni_addresses.nextElement();
	
					new_address_set.add( ni_address );

					if ( old_address_set.contains( ni_address )){
								
							// already established
						
						continue;
					}
						// turn on loopback to see if it helps for local host UPnP devices
						// nah, turn it off again, it didn;t
					
					if ( ni_address.isLoopbackAddress()){
						
						if ( log_ignored ){
							
							adapter.trace( "UPnP::SSDP: ignoring loopback address " + ni_address );
						}
						
						continue;
					}
					
					if ( ni_address instanceof Inet6Address ){
			
						if ( log_ignored ){
							
							adapter.trace( "UPnP::SSDP: ignoring IPv6 address " + ni_address );
						}
						
						continue;
					}
										
					try{
							// set up group
						
						final MulticastSocket mc_sock = new MulticastSocket(SSDP_GROUP_PORT);
										
						mc_sock.setReuseAddress(true);
						
							// windows 98 doesn't support setTimeToLive
						
						try{
							mc_sock.setTimeToLive(TTL);
							
						}catch( Throwable e ){
							
							if ( !ttl_problem_reported ){
								
								ttl_problem_reported	= true;
								
								Debug.printStackTrace( e );
							}
						}
						
						String	addresses_string = "";
							
						Enumeration it = network_interface.getInetAddresses();
						
						while (it.hasMoreElements()){
							
							InetAddress addr = (InetAddress)it.nextElement();
							
							addresses_string += (addresses_string.length()==0?"":",") + addr;
						}
						
						adapter.trace( "UPnP::SSDP: group = " + group_address +"/" + 
									network_interface.getName()+":"+ 
									network_interface.getDisplayName() + "-" + addresses_string +": started" );
						
						mc_sock.joinGroup( group_address, network_interface );
					
						mc_sock.setNetworkInterface( network_interface );
						
							// note that false ENABLES loopback mode which is what we want 
						
						mc_sock.setLoopbackMode(false);
											
						Runtime.getRuntime().addShutdownHook(
								new AEThread("SSDP:VMShutdown")
								{
									public void
									runSupport()
									{
										try{
											mc_sock.leaveGroup( group_address, network_interface );
											
										}catch( Throwable e ){
											
											Debug.printStackTrace( e );
										}
									}
								});
						
						Thread	group_thread = 
							new AEThread("SSDP: MC listener")
							{
								public void
								runSupport()
								{
									handleSocket( network_interface, ni_address, mc_sock );
								}
							};
							
						group_thread.setDaemon( true );
						
						group_thread.start();
						
					}catch( Throwable e ){
						
						Debug.printStackTrace( e );
					}						
				
						// now do the incoming control listener
					
					try{
						final DatagramSocket control_socket = new DatagramSocket( null );
							
						control_socket.setReuseAddress( true );
							
						control_socket.bind( new InetSocketAddress(ni_address, SSDP_CONTROL_PORT ));
			
						adapter.createThread(
							"SSDP:listener",
							new AERunnable()
							{
								public void
								runSupport()
								{
									handleSocket( network_interface, ni_address, control_socket );
								}
							});
														
					}catch( Throwable e ){
					
						Debug.printStackTrace( e );
					}
				}
			}
		}finally{
			
			current_registrations	= new_registrations;
			
			this_mon.exit();
		}
	}
	
	protected boolean
	validNetworkAddress(
		final NetworkInterface	network_interface,
		final InetAddress		ni_address )
	{
		try{
			this_mon.enter();
		
			Set	set = (Set)current_registrations.get( network_interface );
			
			if ( set == null ){
				
				return( false );
			}
			
			return( set.contains( ni_address ));
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	search(
		String	ST )
	{
		String	str =
			"M-SEARCH * HTTP/" + HTTP_VERSION + NL +  
			"ST: " + ST + NL +
			"MX: 3" + NL +
			"MAN: \"ssdp:discover\"" + NL + 
			"HOST: " + SSDP_GROUP_ADDRESS + ":" + SSDP_GROUP_PORT + NL + NL;

		byte[]	data = str.getBytes();
		
		try{
			Enumeration	x = NetworkInterface.getNetworkInterfaces();
			
			while( x != null && x.hasMoreElements()){
				
				NetworkInterface	network_interface = (NetworkInterface)x.nextElement();
				
				if ( !network_interface.getInetAddresses().hasMoreElements()){
					
						// skip any interface that have no addresses as this will
						// cause an error when we try and set the mc_socks's NI
					
					continue;
				}
				
				try{
					
					MulticastSocket mc_sock = new MulticastSocket(null);
	
					mc_sock.setReuseAddress(true);
					
					try{
						mc_sock.setTimeToLive( TTL );
						
					}catch( Throwable e ){
						
						if ( !ttl_problem_reported ){
							
							ttl_problem_reported	= true;
							
							Debug.printStackTrace( e );
						}
					}
					
					mc_sock.bind( new InetSocketAddress( SSDP_CONTROL_PORT ));
	
					mc_sock.setNetworkInterface( network_interface );
					
					// System.out.println( "querying interface " + network_interface );
					
					DatagramPacket packet = new DatagramPacket(data, data.length, group_address.getAddress(), SSDP_GROUP_PORT );
					
					mc_sock.send(packet);
					
					mc_sock.close();
						
				}catch( Throwable e ){
				
					if ( !sso_problem_reported ){
						
						sso_problem_reported	= true;
					
						Debug.printStackTrace( e );
					}
				}
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
	
	protected void
	handleSocket(
		NetworkInterface	network_interface,
		InetAddress			local_address,
		DatagramSocket		socket )
	{
		long	successful_accepts 	= 0;
		long	failed_accepts		= 0;

		int	port = socket.getLocalPort();
		
		try{
				// introduce a timeout so that when a Network interface changes we don't sit here
				// blocking forever and thus never realise that we should shutdown
			
			socket.setSoTimeout( 30000 );
			
		}catch( Throwable e ){
			
		}
		
		while(true){
			
			if ( !validNetworkAddress( network_interface, local_address )){
				
				adapter.trace( "UPnP::SSDP: group = " + group_address +"/" + 
						network_interface.getName()+":"+ 
						network_interface.getDisplayName() + " - " + local_address + ": stopped" );
				
				return;
			}
			
			try{
				byte[] buf = new byte[PACKET_SIZE];
				
				DatagramPacket packet = new DatagramPacket(buf, buf.length );
								
				socket.receive( packet );
					
				successful_accepts++;
				
				failed_accepts	 = 0;
				
				receivePacket( network_interface, local_address, packet );
				
			}catch( SocketTimeoutException e ){
				
			}catch( Throwable e ){
				
				failed_accepts++;
				
				Logger.log(new LogEvent(LOGID, "SSDP: receive failed on port " + port,
						e)); 

				if (( failed_accepts > 100 && successful_accepts == 0 ) || failed_accepts > 1000 ){
					
						Logger.logTextResource(new LogAlert(LogAlert.UNREPEATABLE,
							LogAlert.AT_ERROR, "Network.alert.acceptfail"), new String[] {
							"" + port, "UDP" });
			
					break;
				}
			}
		}
	}
	
	protected void
	receivePacket(
		NetworkInterface	network_interface,
		InetAddress			local_address,
	    DatagramPacket		packet )
	{
		String	str = new String( packet.getData(), 0, packet.getLength());
				
		if ( first_response ){
			
			first_response	= false;
			
			adapter.trace( "UPnP:SSDP: first response:\n" + str );
		}
		
				// example notify event
			/*
			NOTIFY * HTTP/1.1
			HOST: 239.255.255.250:1900
			CACHE-CONTROL: max-age=3600
			LOCATION: http://192.168.0.1:49152/gateway.xml
			NT: urn:schemas-upnp-org:service:WANIPConnection:1
			NTS: ssdp:byebye
			SERVER: Linux/2.4.17_mvl21-malta-mips_fp_le, UPnP/1.0, Intel SDK for UPnP devices /1.2
			USN: uuid:ab5d9077-0710-4373-a4ea-5192c8781666::urn:schemas-upnp-org:service:WANIPConnection:1
			*/
			
		// System.out.println( str );
		
		List	lines = new ArrayList();
		
		int	pos = 0;
		
		while(true){
			
			int	p1 = str.indexOf( NL, pos );
			
			String	line;
			
			if ( p1 == -1 ){
			
				line = str.substring(pos);
			}else{
				
				line = str.substring(pos,p1);
				
				pos	= p1+1;
			}
			
			lines.add( line.trim());
			
			if ( p1 == -1 ){
				
				break;
			}
		}
		
		if ( lines.size() == 0 ){
			
			adapter.trace( "SSDP::receive packet - 0 line reply" );
			
			return;
		}
		
		String	header = (String)lines.get(0);
		
			// Gudy's  Root: http://192.168.0.1:5678/igd.xml, uuid:upnp-InternetGatewayDevice-1_0-12345678900001::upnp:rootdevice, upnp:rootdevice
			// Parg's  Root: http://192.168.0.1:49152/gateway.xml, uuid:824ff22b-8c7d-41c5-a131-44f534e12555::upnp:rootdevice, upnp:rootdevice

		URL		location	= null;
		String	nt			= null;
		String	nts			= null;
		String	st			= null;
		String	al			= null;
		
		for (int i=1;i<lines.size();i++){
			
			String	line = (String)lines.get(i);
			
			int	c_pos = line.indexOf(":");
			
			if ( c_pos == -1 ){
				continue;
			}
			
			String	key	= line.substring( 0, c_pos ).trim().toUpperCase();
			String 	val = line.substring( c_pos+1 ).trim();
			
			if ( key.equals("LOCATION" )){
				
				try{
					location	= new URL( val );
					
				}catch( MalformedURLException e ){
					
					adapter.trace( e );
				}			
			}else if ( key.equals( "NT" )){
				
				nt	= val;
				
			}else if ( key.equals( "NTS" )){
				
				nts	= val;
				
			}else if ( key.equals( "ST" )){
				
				st	= val;
				
			}else if ( key.equals( "AL" )){
				
				al	= val;
			}
		}
			
		if ( header.startsWith("M-SEARCH")){

			if ( st != null ){
				
				/*
				HTTP/1.1 200 OK
				CACHE-CONTROL: max-age=600
				DATE: Tue, 20 Dec 2005 13:07:31 GMT
				EXT:
				LOCATION: http://192.168.1.1:2869/gatedesc.xml
				SERVER: Linux/2.4.17_mvl21-malta-mips_fp_le UPnP/1.0 
				ST: upnp:rootdevice
				USN: uuid:UUID-InternetGatewayDevice-1234::upnp:rootdevice
				*/
				
				String	response = informSearch( network_interface, local_address, packet.getAddress(), st );
				
				if ( response != null ){
					
					String	data = 
						"HTTP/1.1 200 OK" + NL +
						"SERVER: Azureus (UPnP/1.0)" + NL +
						"CACHE-CONTROL: max-age=600" + NL +
						"LOCATION: http://" + local_address.getHostAddress() + ":" + SSDP_CONTROL_PORT + "/" + NL +
						"ST: " + st + NL + 
						"USN: uuid:UUID-Azureus-1234::" + st + NL + 
						"AL: " + response;
					
					DatagramSocket	reply_socket	= null;
					
					byte[]	data_bytes = data.getBytes();
					
					try{
						reply_socket = new DatagramSocket();
						
						DatagramPacket reply_packet = new DatagramPacket(data_bytes,data_bytes.length,packet.getSocketAddress());
						
						reply_socket.send( reply_packet );
						
					}catch( Throwable e ){
						
						adapter.trace(e);
						
					}finally{
						
						if ( reply_socket != null ){
							
							try{
								reply_socket.close();
								
							}catch( Throwable e ){
								
								adapter.trace(e);
							}
						}
					}
					
					
				}
			}else{
				
				adapter.trace( "SSDP::receive M-SEARCH - bad header:" + header );
			}
		}else if ( header.startsWith( "NOTIFY" )){
			
			if ( location != null && nt != null && nts != null ){
			
				informNotify( network_interface, local_address, packet.getAddress(), location, nt, nts );
			}else{
				
				adapter.trace( "SSDP::receive NITOFY - bad header:" + header );
			}
		}else if ( header.startsWith( "HTTP") && header.indexOf( "200") != -1 ){
			
			if ( location != null && st != null ){
		
				informResult( network_interface, local_address, packet.getAddress(), location, st, al  );
				
			}else{
				
				adapter.trace( "SSDP::receive HTTP - bad header:" + header );
			}			
		}else{
			
			adapter.trace( "SSDP::receive packet - bad header:" + header );
		}
	}
	

	protected void
	informResult(
		NetworkInterface	network_interface,
		InetAddress			local_address,
		InetAddress			originator,
		URL					location,
		String				st,
		String				al )
	{
		for (int i=0;i<listeners.size();i++){
			
			try{
				((UPnPSSDPListener)listeners.get(i)).receivedResult(network_interface,local_address,originator,location,st,al);
				
			}catch( Throwable e ){
				
				adapter.trace(e);
			}
		}
	}
	
	protected void
	informNotify(
		NetworkInterface	network_interface,
		InetAddress			local_address,
		InetAddress			originator,
		URL					location,
		String				nt,
		String				nts )
	{
		for (int i=0;i<listeners.size();i++){
			
			try{
				((UPnPSSDPListener)listeners.get(i)).receivedNotify(network_interface,local_address,originator,location,nt,nts);
				
			}catch( Throwable e ){
				
				adapter.trace(e);
			}
		}
	}
	
	protected String
	informSearch(
		NetworkInterface	network_interface,
		InetAddress			local_address,
		InetAddress			originator,
		String				st )
	{
		for (int i=0;i<listeners.size();i++){
			
			try{
				String	res = ((UPnPSSDPListener)listeners.get(i)).receivedSearch(network_interface,local_address,originator,st );
				
				if ( res != null ){
					
					return( res );
				}
			}catch( Throwable e ){
				
				adapter.trace(e);
			}
		}
		
		return( null );
	}
	
	public void
	addListener(
		UPnPSSDPListener	l )
	{
		listeners.add( l );
	}
	
	public void
	removeListener(
			UPnPSSDPListener	l )
	{
		listeners.remove(l);
	}
}
