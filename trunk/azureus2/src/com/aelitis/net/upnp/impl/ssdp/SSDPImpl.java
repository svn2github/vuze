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

import org.gudy.azureus2.core3.util.*;

import com.aelitis.net.upnp.*;
import com.aelitis.net.upnp.impl.*;

/**
 * @author parg
 *
 */

public class 
SSDPImpl 
	implements SSDP
{
	protected final static String	SSDP_GROUP_ADDRESS 	= "239.255.255.250";
	protected final static int		SSDP_GROUP_PORT		= 1900;
	protected final static int		SSDP_CONTROL_PORT	= 8008;
	
	protected final static int		PACKET_SIZE		= 8192;
		
	protected static final String	HTTP_VERSION	= "1.1";
	protected static final String	NL				= "\r\n";
	
	
	protected UPnPImpl		upnp;
	
	protected long			last_explicit_search	= 0;
	
	protected List			mc_bind_addresses		= new ArrayList();
		
	protected boolean		first_response			= true;
	
	protected List			listeners	= new ArrayList();
	
	protected AEMonitor		this_mon	= new AEMonitor( "SSDP" );

	public
	SSDPImpl(
		UPnPImpl		_upnp )
	
		throws UPnPException
	{	
		upnp	= _upnp;
	}
	
	public void
	start()
	
		throws UPnPException
	{
		try{
			Enumeration network_interfaces = NetworkInterface.getNetworkInterfaces();
			
			while (network_interfaces.hasMoreElements()){
				
				final NetworkInterface network_interface = (NetworkInterface)network_interfaces.nextElement();


				Enumeration ni_addresses = network_interface.getInetAddresses();
				
				while (ni_addresses.hasMoreElements()){
					
					final InetAddress ni_address = (InetAddress)ni_addresses.nextElement();
					
					// turn on loopback to see if it helps for local host UPnP devices
					// nah, turn it off again, it didn;t
					
					if ( ni_address.isLoopbackAddress()){
						
						upnp.log( "UPnP::SSDP: ignoring loopback address " + ni_address );
						
						continue;
					}
					
					if ( ni_address instanceof Inet6Address ){

						upnp.log( "UPnP::SSDP: ignoring IPv6 address " + ni_address );
						
						continue;
					}
					
					try{
							// set up group
						
						InetSocketAddress	bind_address = new InetSocketAddress( SSDP_GROUP_PORT );
						
						mc_bind_addresses.add( bind_address );
									
						final MulticastSocket mc_sock = new MulticastSocket(null);
						
						mc_sock.setReuseAddress(true);
						
							// windows 98 doesn't support setTimeToLive
						
						try{
							mc_sock.setTimeToLive(4);
							
						}catch( Throwable e ){
							
							Debug.printStackTrace( e );
						}
						
						mc_sock.bind( bind_address );
						
						final InetSocketAddress group_address = new InetSocketAddress(InetAddress.getByName(SSDP_GROUP_ADDRESS), SSDP_GROUP_PORT);
							
						upnp.log( "UPnP::SSDP: group = " + group_address +"/" + network_interface.getName() + ":" + ni_address.toString());
						
						mc_sock.joinGroup( group_address, network_interface );
					
						mc_sock.setLoopbackMode(true);
											
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
									handleSocket( ni_address, mc_sock);
								}
							};
							
						group_thread.setDaemon( true );
						
						group_thread.start();
						
							// now do the incoming control listener
						
						final DatagramSocket control_socket = new DatagramSocket( null );
						
						control_socket.setReuseAddress( true );
						
						control_socket.bind( new InetSocketAddress(ni_address, SSDP_CONTROL_PORT ));
		
						upnp.getPluginInterface().getUtilities().createThread(
							"SSDP:listener",
							new AERunnable()
							{
								public void
								runSupport()
								{
									handleSocket( ni_address, control_socket );
								}
							});
													
					}catch( Throwable e ){
					
						Debug.printStackTrace( e );
					}
				}
			}
		
			upnp.getPluginInterface().getUtilities().createThread(
					"SSDP:queryLoop",
					new AERunnable()
					{
						public void
						runSupport()
						{
							queryLoop();
						}	
					});
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
			
			throw( new UPnPException( "Failed to initialise SSDP", e ));
		}
	}
	
	public void
	searchNow()
	{
		long	now = System.currentTimeMillis();
		
		if ( now - last_explicit_search < 10000 ){
			
			return;
		}
		
		last_explicit_search	= now;
		
		search();
	}
	
	protected void
	queryLoop()
	{
		while(true){
			
			try{
				search();
				
				Thread.sleep( 60000 );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
			
		}
	}
	
	protected void
	search()
	{
		String	str =
			"M-SEARCH * HTTP/" + HTTP_VERSION + NL +  
			"ST: upnp:rootdevice" + NL +
			"MX: 3" + NL +
			"MAN: \"ssdp:discover\"" + NL + 
			"HOST: " + SSDP_GROUP_ADDRESS + ":" + SSDP_GROUP_PORT + NL + NL;

		byte[]	data = str.getBytes();
		
		for (int i=0;i<mc_bind_addresses.size();i++){
			
			InetSocketAddress	mc_bind_address = (InetSocketAddress)mc_bind_addresses.get(i);

			try{
				InetSocketAddress group_address = new InetSocketAddress(InetAddress.getByName(SSDP_GROUP_ADDRESS), SSDP_GROUP_PORT);
				
				MulticastSocket mc_sock = new MulticastSocket(null);

				mc_sock.setReuseAddress(true);
				
				mc_sock.setTimeToLive(4);

				mc_sock.bind( new InetSocketAddress( mc_bind_address.getAddress(), SSDP_CONTROL_PORT ));

				DatagramPacket packet = new DatagramPacket(data, data.length, group_address);
				
				mc_sock.send(packet);
				
				mc_sock.close();
			
			
			}catch( Throwable e ){
			
				Debug.printStackTrace( e );
			}
		}
	}
	
	protected void
	handleSocket(
		InetAddress			local_address,
		DatagramSocket		socket )
	{
		while(true){
			
			try{
				byte[] buf = new byte[PACKET_SIZE];
				
				DatagramPacket packet = new DatagramPacket(buf, buf.length );
								
				socket.receive( packet );
											
				receivePacket( local_address, packet );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}
	}
	
	protected void
	receivePacket(
		InetAddress			local_address,
	    DatagramPacket		packet )
	{
		try{
			this_mon.enter();
		
			String	str = new String( packet.getData(), 0, packet.getLength());
			
			if ( first_response ){
				
				first_response	= false;
				
				upnp.log( "UPnP:SSDP: first response:\n" + str );
			}
			
			if ( str.startsWith("M-SEARCH")){
				
					// hmm, loopack or another client announcing, ignore it
				
				return;
				
			}
				
					// notify event
	
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
				
				upnp.log( "SSDP::receive packet - 0 line reply" );
				
				return;
			}
			
			String	header = (String)lines.get(0);
			
				// Gudy's  Root: http://192.168.0.1:5678/igd.xml, uuid:upnp-InternetGatewayDevice-1_0-12345678900001::upnp:rootdevice, upnp:rootdevice
				// Parg's  Root: http://192.168.0.1:49152/gateway.xml, uuid:824ff22b-8c7d-41c5-a131-44f534e12555::upnp:rootdevice, upnp:rootdevice
	
			URL		location	= null;
			String	nt			= null;
			String	nts			= null;
			
			for (int i=1;i<lines.size();i++){
				
				String	line = (String)lines.get(i);
				
				int	c_pos = line.indexOf(":");
				
				if ( c_pos == -1 ){
					continue;
				}
				
				String	key	= line.substring( 0, c_pos ).trim();
				String 	val = line.substring( c_pos+1 ).trim();
				
				if ( key.equalsIgnoreCase("LOCATION" )){
					
					try{
						location	= new URL( val );
						
					}catch( MalformedURLException e ){
						
						upnp.log( e );
					}			
				}else if ( key.equalsIgnoreCase( "NT" )){
					
					nt	= val;
					
				}else if ( key.equalsIgnoreCase( "NTS" )){
					
					nts	= val;
				}
			}
				
			if ( header.startsWith( "NOTIFY" )){
				
				if ( location != null && nt != null && nts != null ){
					
					if ( nt.indexOf( "upnp:rootdevice" ) != -1 ){
						
						if ( nts.indexOf("alive") != -1 ){
							
								// alive can be reported on any interface
							
							gotAlive( location );
							
						}else if ( nts.indexOf( "byebye") != -1 ){
								
							lostRoot( local_address, location );
						}
					}		
				}
			}else if ( header.startsWith( "HTTP") && header.indexOf( "200") != -1 ){
	
				if ( location != null ){
					
					gotRoot( local_address, location );
				}
			}else{
				
				upnp.log( "UPnP::SSDP::receive packet - bad header:" + header );
			}
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected void
	gotRoot(
		InetAddress	local_address,
		URL		location )
	{
		for (int i=0;i<listeners.size();i++){
			
			((SSDPListener)listeners.get(i)).rootDiscovered( local_address, location );
		}
	}

	protected void
	gotAlive(
		URL		location )
	{
		for (int i=0;i<listeners.size();i++){
			
			((SSDPListener)listeners.get(i)).rootAlive( location );
		}
	}
	protected void
	lostRoot(
		InetAddress	local_address,
		URL		location )
	{
		for (int i=0;i<listeners.size();i++){
			
			((SSDPListener)listeners.get(i)).rootLost( local_address, location );
		}
	}
	
	public void
	addListener(
		SSDPListener	l )
	{
		listeners.add( l );
	}
	
	public void
	removeListener(
		SSDPListener	l )
	{
		listeners.remove(l);
	}
}
