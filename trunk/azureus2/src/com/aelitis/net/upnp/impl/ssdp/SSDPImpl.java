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

import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.utils.UTTimer;
import org.gudy.azureus2.plugins.utils.UTTimerEvent;
import org.gudy.azureus2.plugins.utils.UTTimerEventPerformer;

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


	private UPnPImpl		upnp;
	
	private long			last_explicit_search	= 0;
		
	private boolean		first_response			= true;
	private boolean		ttl_problem_reported	= false;
	private boolean		sso_problem_reported	= false;
	
	private List			listeners	= new ArrayList();
	
	protected AEMonitor		this_mon	= new AEMonitor( "SSDP" );

	private Map	current_registrations = new HashMap();
	
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
			processNetworkInterfaces( true );
		
			UTTimer timer = 
				upnp.getPluginInterface().getUtilities().createTimer( "SSDP:refresher", true );
			
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
							
							upnp.log( "UPnP::SSDP: ignoring loopback address " + ni_address );
						}
						
						continue;
					}
					
					if ( ni_address instanceof Inet6Address ){
			
						if ( log_ignored ){
							
							upnp.log( "UPnP::SSDP: ignoring IPv6 address " + ni_address );
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
						
						upnp.log( "UPnP::SSDP: group = " + group_address +"/" + 
									network_interface.getName()+":"+ 
									network_interface.getDisplayName() + "-" + addresses_string +": started" );
						
						mc_sock.joinGroup( group_address, network_interface );
					
						mc_sock.setNetworkInterface( network_interface );
						
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
			
						upnp.getPluginInterface().getUtilities().createThread(
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
	searchNow()
	{
		long	now = SystemTime.getCurrentTime();
		
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
		
		while(true){
			
			if ( !validNetworkAddress( network_interface, local_address )){
				
				upnp.log( "UPnP::SSDP: group = " + group_address +"/" + 
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
				
			}catch( Throwable e ){
				
				failed_accepts++;
				
				LGLogger.log( "SSDP: receive failed on port " + port, e ); 

				if (( failed_accepts > 100 && successful_accepts == 0 ) || failed_accepts > 1000 ){
					
					LGLogger.logUnrepeatableAlertUsingResource( 
							LGLogger.AT_ERROR,
							"Network.alert.acceptfail",
							new String[]{ ""+port, "UDP" } );
			
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
		try{
			this_mon.enter();
		
			String	str = new String( packet.getData(), 0, packet.getLength());
			
			boolean	log = first_response;
			
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
							
							try{

								InetAddress	dev = InetAddress.getByName( location.getHost());
								
								byte[]	dev_bytes = dev.getAddress();
																
								boolean[]	dev_bits = bytesToBits( dev_bytes );
								
									// try and work out what bind address this location corresponds to
								
								NetworkInterface	best_ni 	= null;
								InetAddress			best_addr	= null;
								
								int	best_prefix	= 0;
								
								Enumeration network_interfaces = NetworkInterface.getNetworkInterfaces();
								
								while (network_interfaces.hasMoreElements()){
									
									NetworkInterface this_ni = (NetworkInterface)network_interfaces.nextElement();
															
									Enumeration ni_addresses = this_ni.getInetAddresses();
									
									while (ni_addresses.hasMoreElements()){
										
										InetAddress this_address = (InetAddress)ni_addresses.nextElement();
										
										byte[]	this_bytes = this_address.getAddress();
										
										if ( dev_bytes.length == this_bytes.length ){
											
											boolean[]	this_bits = bytesToBits( this_bytes );

											for (int i=0;i<this_bits.length;i++){
												
												if ( dev_bits[i] != this_bits[i] ){
													
													break;
												}
												
												if ( i > best_prefix ){
													
													best_prefix	= i;
													
													best_ni		= this_ni;
													best_addr	= this_address;
												}
											}
										}
									}
								}
								
								if ( best_ni != null ){
									
									if ( log ){
										
										upnp.log( location + " -> " + best_ni.getDisplayName() + "/" + best_addr + " (prefix=" + (best_prefix + 1 ) + ")");
									}
									
									gotRoot( best_ni, best_addr, location );
									
								}else{
									
									gotAlive( location );
								}
							}catch( Throwable e ){
								
								gotAlive( location );
							}
						}else if ( nts.indexOf( "byebye") != -1 ){
								
							lostRoot( local_address, location );
						}
					}		
				}
			}else if ( header.startsWith( "HTTP") && header.indexOf( "200") != -1 ){
	
				if ( location != null ){
					
					gotRoot( network_interface, local_address, location );
				}
			}else{
				
				upnp.log( "UPnP::SSDP::receive packet - bad header:" + header );
			}
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected boolean[]
	bytesToBits(
		byte[]	bytes )
	{
		boolean[]	res = new boolean[bytes.length*8];
		
		for (int i=0;i<bytes.length;i++){
			
			byte	b = bytes[i];
			
			for (int j=0;j<8;j++){
				
				res[i*8+j] = (b&(byte)(0x01<<(7-j))) != 0;
			}
		}
				
		return( res );
	}
	
	protected void
	gotRoot(
		NetworkInterface	network_interface,
		InetAddress			local_address,
		URL					location )
	{
		for (int i=0;i<listeners.size();i++){
			
			((SSDPListener)listeners.get(i)).rootDiscovered( network_interface, local_address, location );
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
