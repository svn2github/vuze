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

package org.gudy.azureus2.core3.upnp.impl.ssdp;

import java.net.*;
import java.util.*;

import org.gudy.azureus2.core3.upnp.*;
import org.gudy.azureus2.core3.upnp.impl.*;

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
	
	protected List			mc_bind_addresses	= new ArrayList();
		
	protected List			listeners	= new ArrayList();
	
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
					
					InetAddress ni_address = (InetAddress)ni_addresses.nextElement();
					
					if ( ni_address.isLoopbackAddress()){
						
						continue;
					}
											
					try{
							// set up group
						
						InetSocketAddress	bind_address = new InetSocketAddress( SSDP_GROUP_PORT );
						
						mc_bind_addresses.add( bind_address );
									
						final MulticastSocket mc_sock = new MulticastSocket(null);
						
						mc_sock.setReuseAddress(true);
						
						mc_sock.setTimeToLive(4);
						
						mc_sock.bind( bind_address );
						
						final InetSocketAddress group_address = new InetSocketAddress(InetAddress.getByName(SSDP_GROUP_ADDRESS), SSDP_GROUP_PORT);
							
						upnp.log( "UPnP::SSDP: group = " + group_address +"/" + network_interface.getName() + ":" + bind_address );
						
						mc_sock.joinGroup( group_address, network_interface );
					
						mc_sock.setLoopbackMode(true);
											
						Runtime.getRuntime().addShutdownHook(
								new Thread()
								{
									public void
									run()
									{
										try{
											mc_sock.leaveGroup( group_address, network_interface );
											
										}catch( Throwable e ){
											
											e.printStackTrace();
										}
									}
								});
						
						Thread	group_thread = 
							new Thread()
							{
								public void
								run()
								{
									handleSocket( mc_sock);
								}
							};
							
						group_thread.setDaemon( true );
						
						group_thread.start();
						
							// now do the incoming control listener
						
						final DatagramSocket control_socket = new DatagramSocket( null );
						
						control_socket.setReuseAddress( true );
						
						control_socket.bind( new InetSocketAddress(ni_address, SSDP_CONTROL_PORT ));
		
						Thread	control_thread = 
							new Thread()
							{
								public void
								run()
								{
									handleSocket( control_socket );
								}
							};
							
						control_thread.setDaemon( true );
						
						control_thread.start();
						
					}catch( Throwable e ){
					
						e.printStackTrace();
					}
				}
			}
		
			Thread	query_thread = 
				new Thread()
				{
					public void
					run()
					{
						 queryLoop();
					}
				};
				
			query_thread.setDaemon(true);
			
			query_thread.start();
			
		}catch( Throwable e ){
			
			e.printStackTrace();
			
			throw( new UPnPException( "Failed to initialise SSDP", e ));
		}
	}
	
	protected void
	queryLoop()
	{
		while(true){
			
			try{
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
					
						e.printStackTrace();
					}
					
				}
				
				Thread.sleep( 60000 );
				
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
			
		}
	}
	

	
	protected void
	handleSocket(
		DatagramSocket		socket )
	{
		while(true){
			
			try{
				byte[] buf = new byte[PACKET_SIZE];
				
				DatagramPacket packet = new DatagramPacket(buf, buf.length );
								
				socket.receive( packet );
											
				receivePacket( packet );
				
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
		}
	}
	
	protected synchronized void
	receivePacket(
	    DatagramPacket		packet )
	{
		String	str = new String( packet.getData(), 0, packet.getLength());
		
		if ( str.startsWith("M-SEARCH")){
			
				// hmm, loopack or another client announcing, ignore it
			
			return;
			
		}else if ( str.startsWith( "NOTIFY" )){
			
				// notify event, ignore
			
			return;
		}
		
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

		if ( header.startsWith( "HTTP") && header.indexOf( "200") != -1 ){
			
			String	location	= null;
			String	usn			= null;
			String	st			= null;
			
			for (int i=1;i<lines.size();i++){
				
				String	line = (String)lines.get(i);
				
				int	c_pos = line.indexOf(":");
				
				if ( c_pos == -1 ){
					continue;
				}
				
				String	key	= line.substring( 0, c_pos ).trim();
				String 	val = line.substring( c_pos+1 ).trim();
				
				if ( key.equalsIgnoreCase("LOCATION" )){
					
					location	= val;
					
				}else if ( key.equalsIgnoreCase( "USN" )){
					
					usn	= val;
					
				}else if ( key.equalsIgnoreCase( "ST" )){
					
					st	= val;
				}
			}
			
			if ( location != null && usn != null && st != null ){
				
				gotRoot( location, usn, st );
			}
		}else{
			
			upnp.log( "UPnP::SSDP::receive packet - bad header:" + header );
		}
	}
	
	protected void
	gotRoot(
		String		location,
		String		usn,
		String		st )
	{
		for (int i=0;i<listeners.size();i++){
			
			((SSDPListener)listeners.get(i)).rootDiscovered( location, usn, st );
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
