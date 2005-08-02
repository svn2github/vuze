/*
 * Created on 21-Jan-2005
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

package com.aelitis.azureus.core.dht.transport.udp.impl;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SHA1Simple;


import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportException;
import com.aelitis.azureus.core.dht.transport.DHTTransportFullStats;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;
import com.aelitis.azureus.core.dht.transport.udp.DHTTransportUDP;

/**
 * @author parg
 *
 */

public class 
DHTUDPUtils 
{
	protected static final int	CT_UDP		= 1;
	
	private static ThreadLocal		tls	= 
		new ThreadLocal()
		{
			public Object
			initialValue()
			{
				return( new SHA1Simple());
			}
		};
		
	protected static byte[]
	getNodeID(
		InetSocketAddress	address )
	
		throws DHTTransportException
	{		
		InetAddress ia = address.getAddress();
		
		if ( ia == null ){
			
			// Debug.out( "Address '" + address + "' is unresolved" );
			
			throw( new DHTTransportException( "Address '" + address + "' is unresolved" ));
			
		}else{
			
			SHA1Simple	hasher = (SHA1Simple)tls.get();
			
			byte[]	res = hasher.calculateHash(
						(	ia.getHostAddress() + ":" + address.getPort()).getBytes());
			
			//System.out.println( "NodeID: " + address + " -> " + DHTLog.getString( res ));
			
			return( res );
		}
	}
	
	protected static void
	serialiseLength(
		DataOutputStream	os,
		int					len,
		int					max_length )
	
		throws IOException
	{
		if ( len > max_length ){
			
			throw( new IOException( "Invalid data length" ));
		}
		
		if ( max_length < 256 ){
			
			os.writeByte( len );
			
		}else if ( max_length < 65536 ){
			
			os.writeShort( len );
			
		}else{
			
			os.writeInt( len );
		}
	}
	
	protected static int
	deserialiseLength(
		DataInputStream	is,
		int				max_length )
	
		throws IOException
	{
		int		len;
		
		if ( max_length < 256 ){
			
			len = is.readByte()&0xff;
			
		}else if ( max_length < 65536 ){
			
			len = is.readShort()&0xffff;
			
		}else{
			
			len = is.readInt();
		}
		
		if ( len > max_length ){
			
			throw( new IOException( "Invalid data length" ));
		}
		
		return( len );
	}
	
	protected static byte[]
	deserialiseByteArray(
		DataInputStream	is,
		int				max_length )
	
		throws IOException
	{
		int	len = deserialiseLength( is, max_length );
		
		byte[] data	= new byte[len];
		
		is.read(data);
		
		return( data );
	}
	
	protected static void
	serialiseByteArray(
		DataOutputStream	os,
		byte[]				data,
		int					max_length )
	
		throws IOException
	{
		serialiseByteArray( os, data, 0, data.length, max_length );
	}
	
	protected static void
	serialiseByteArray(
		DataOutputStream	os,
		byte[]				data,
		int					start,
		int					length,
		int					max_length )
	
		throws IOException
	{
		serialiseLength( os, length, max_length );
		
		os.write( data, start, length );
	}
	
	protected static void
	serialiseByteArrayArray(
		DataOutputStream		os,
		byte[][]				data,
		int						max_length )
	
		throws IOException
	{
		serialiseLength(os,data.length,max_length);
		
		for (int i=0;i<data.length;i++){
			
			serialiseByteArray( os, data[i], max_length );
		}
	}
	
	protected static byte[][]
	deserialiseByteArrayArray(
		DataInputStream	is,
		int				max_length )
	
		throws IOException
	{
		int	len = deserialiseLength( is, max_length );
		
		byte[][] data	= new byte[len][];
		
		for (int i=0;i<data.length;i++){
			
			data[i] = deserialiseByteArray( is, max_length );
		}
		
		return( data );
	}
	
	public static final int INETSOCKETADDRESS_IPV4_SIZE	= 7;
	public static final int INETSOCKETADDRESS_IPV6_SIZE	= 19;
	
	protected static void
	serialiseAddress(
		DataOutputStream	os,
		InetSocketAddress	address )
	
		throws IOException, DHTTransportException
	{
		InetAddress	ia = address.getAddress();
		
		if ( ia == null ){
			
			Debug.out( "Address '" + address + "' is unresolved" );
			
			throw( new DHTTransportException( "Address '" + address + "' is unresolved" ));
		}
		
		serialiseByteArray( os, ia.getAddress(), 16);	// 16 (Pv6) + 1 length
		
		os.writeShort( address.getPort());	//19
	}
	
	protected static InetSocketAddress
	deserialiseAddress(
		DataInputStream		is )
	
		throws IOException
	{
		byte[]	bytes = deserialiseByteArray( is, 16 );
				
		int	port = is.readShort()&0xffff;
		
		return( new InetSocketAddress( InetAddress.getByAddress( bytes ), port ));
	}
	
	protected static DHTTransportValue[][]
	deserialiseTransportValuesArray(
		DHTUDPPacket			packet,
		DataInputStream			is,
		long					skew,
		int						max_length )
	
		throws IOException
	{
		int	len = deserialiseLength( is, max_length );
		
		DHTTransportValue[][] data	= new DHTTransportValue[len][];
		
		for (int i=0;i<data.length;i++){
			
			data[i] = deserialiseTransportValues( packet, is, skew );
		}
		
		return( data );	
	}
	
	protected static void
	serialiseTransportValuesArray(
		DHTUDPPacket			packet,
		DataOutputStream		os,
		DHTTransportValue[][]	values,
		long					skew,
		int						max_length )
	
		throws IOException, DHTTransportException
	{
		serialiseLength(os,values.length,max_length);
		
		for (int i=0;i<values.length;i++){
			
			serialiseTransportValues( packet, os, values[i], skew );
		}	
	}
	
	public static final int	DHTTRANSPORTCONTACT_SIZE	= 2 + INETSOCKETADDRESS_IPV4_SIZE;
	
	protected static void
	serialiseContact(
		DataOutputStream		os,
		DHTTransportContact		contact )
	
		throws IOException, DHTTransportException
	{
		if ( contact instanceof DHTTransportUDPContactImpl ){
			
			os.writeByte( CT_UDP );		// 1
			
			os.writeByte( contact.getProtocolVersion());	// 2
			
			DHTTransportUDPContactImpl c = (DHTTransportUDPContactImpl)contact;
								
			serialiseAddress( os, c.getExternalAddress() );	// 2 + address
			
		}else{
			
			throw( new IOException( "Unsupported contact type:" + contact.getClass().getName()));
		}
	}
	
	protected static DHTTransportUDPContactImpl
	deserialiseContact(
		DHTTransportUDPImpl		transport,
		DataInputStream			is )
	
		throws IOException, DHTTransportException
	{
		byte	ct = is.readByte();
		
		if ( ct != CT_UDP ){
			
			throw( new IOException( "Unsupported contact type:" + ct ));
		}
			
		byte	version = is.readByte();
		
			// we don't transport instance ids around via this route as they are just
			// cached versions and not useful
				
		InetSocketAddress	external_address = deserialiseAddress( is );
		
		return( new DHTTransportUDPContactImpl( transport, external_address, external_address, version, 0, 0 ));
	}
	

	protected static DHTTransportValue[]
	deserialiseTransportValues(
		DHTUDPPacket			packet,
		DataInputStream			is,
		long					skew )
	
		throws IOException
	{
		int	len = deserialiseLength( is, 65535 );
		
		List	l = new ArrayList( len );
		
		for (int i=0;i<len;i++){
			
			try{
				
				l.add( deserialiseTransportValue( packet, is, skew ));
				
			}catch( DHTTransportException e ){
				
				Debug.printStackTrace(e);
			}
		}
				
		DHTTransportValue[]	res = new DHTTransportValue[l.size()];
		
		l.toArray( res );
		
		return( res );
	}

	protected static void
	serialiseTransportValues(
		DHTUDPPacket			packet,
		DataOutputStream		os,
		DHTTransportValue[]		values,
		long					skew )
	
		throws IOException, DHTTransportException
	{
		serialiseLength( os, values.length, 65535 );
	
		for (int i=0;i<values.length;i++){
			
			serialiseTransportValue( packet, os, values[i], skew );
		}
	}
	
	protected static DHTTransportValue
	deserialiseTransportValue(
		DHTUDPPacket		packet,
		DataInputStream		is, 
		long				skew )
	
		throws IOException, DHTTransportException
	{
		final int	version;
		
		if ( packet.getProtocolVersion() >= DHTTransportUDP.PROTOCOL_VERSION_REMOVE_DIST_ADD_VER ){
			
			version = is.readInt();
			
			//System.out.println( "read: version = " + version );
			
		}else{
			
			version	= -1;
			
			int distance = is.readInt();
			
			//System.out.println( "read:" + distance );
		}
		
		final long 	created		= is.readLong() + skew;
		
		// System.out.println( "    Adjusted creation time by " + skew );
		
		final byte[]	value_bytes = deserialiseByteArray( is, DHT.MAX_VALUE_SIZE );
		
		final DHTTransportContact	originator		= deserialiseContact( packet.getTransport(), is );
		
		final int flags	= is.readByte()&0xff;
		
		DHTTransportValue value = 
			new DHTTransportValue()
			{
				public boolean
				isLocal()
				{
					return( false );
				}
				
				public long
				getCreationTime()
				{
					return( created );
				}
				
				public byte[]
				getValue()
				{
					return( value_bytes );
				}
				
				public int
				getVersion()
				{
					return( version );
				}
				
				public DHTTransportContact
				getOriginator()
				{
					return( originator );
				}
				
				public int
				getFlags()
				{
					return( flags );
				}
				
				public String
				getString()
				{
					return( new String(getValue()));
				}
			};
			
		return( value );
	}
	
	public static final int DHTTRANSPORTVALUE_SIZE_WITHOUT_VALUE	= 15 + DHTTRANSPORTCONTACT_SIZE;
		
	protected static void
	serialiseTransportValue(
		DHTUDPPacket		packet,
		DataOutputStream	os,
		DHTTransportValue	value,
		long				skew )
	
		throws IOException, DHTTransportException
	{		
		if ( packet.getProtocolVersion() >= DHTTransportUDP.PROTOCOL_VERSION_REMOVE_DIST_ADD_VER ){
			
			int	version = value.getVersion();
			
			//System.out.println( "write: version = " + version );

			os.writeInt( version );
		}else{
			
			//System.out.println( "write: 0" );

			os.writeInt( 0 );
		}
		
			// Don't forget to change the CONSTANT above if you change the size of this!
				
		os.writeLong( value.getCreationTime() + skew );	// 12
		
		serialiseByteArray( os, value.getValue(), DHT.MAX_VALUE_SIZE );	// 12+2+X
		
		serialiseContact( os, value.getOriginator());	// 12 + 2+X + contact
		
		os.writeByte( value.getFlags());	// 13 + 2+ X + contact
	}
	
	protected static void
	serialiseContacts(
		DataOutputStream		os,
		DHTTransportContact[]	contacts )
	
		throws IOException
	{
		serialiseLength( os, contacts.length, 65535 );
		
		for (int i=0;i<contacts.length;i++){
			
			try{
				serialiseContact( os, contacts[i] );
				
			}catch( DHTTransportException e ){
				
				Debug.printStackTrace(e);
				
					// not much we can do here to recover - shouldn't fail anyways
				
				throw( new IOException(e.getMessage()));
			}
		}
	}

	protected static DHTTransportContact[]
	deserialiseContacts(
		DHTTransportUDPImpl		transport,
		DataInputStream			is )
	
		throws IOException
	{
		int	len = deserialiseLength( is, 65535 );
		
		List	l = new ArrayList( len );
		
		for (int i=0;i<len;i++){
			
			try{
				
				l.add( deserialiseContact( transport, is ));
				
			}catch( DHTTransportException e ){
				
				Debug.printStackTrace(e);
			}
		}
				
		DHTTransportContact[]	res = new DHTTransportContact[l.size()];
		
		l.toArray( res );
		
		return( res );
	}
			
	protected static void
	serialiseVivaldi(
		DataOutputStream	os,
		DHTUDPPacketReply	reply )
	
		throws IOException
	{
		float[]	data = reply.getVivaldiData();
	
		if ( data.length != DHTUDPPacketReply.VIVALDI_DATA_LENGTH_V1 ){
		
			Debug.out( "Vivaldi serialisation length changed!!!!" );
			
			throw( new IOException( "argh!!" ));
		}
		
		for (int i=0;i<data.length;i++){
			
			os.writeFloat( data[i] );
		}
	}
	
	protected static void
	deserialiseVivaldi(
		DataInputStream		is,
		DHTUDPPacketReply	reply )
	
		throws IOException
	{
		float[]	data	= new float[DHTUDPPacketReply.VIVALDI_DATA_LENGTH];
		
		if ( data.length != DHTUDPPacketReply.VIVALDI_DATA_LENGTH_V1 ){
			
			Debug.out( "Vivaldi serialisation length changed!!!!" );
			
			throw( new IOException( "argh!!" ));
		}
		
		for (int i=0;i<data.length;i++){
			
			data[i] = is.readFloat();
		}
		
		reply.setVivaldiData( data );
	}
	
	protected static void
	serialiseStats(
		int						version,
		DataOutputStream		os,
		DHTTransportFullStats	stats )
	
		throws IOException
	{
		os.writeLong( stats.getDBValuesStored());
		
		os.writeLong( stats.getRouterNodes());
		os.writeLong( stats.getRouterLeaves());
		os.writeLong( stats.getRouterContacts());
		
		os.writeLong( stats.getTotalBytesReceived());
		os.writeLong( stats.getTotalBytesSent());
		os.writeLong( stats.getTotalPacketsReceived());
		os.writeLong( stats.getTotalPacketsSent());
		os.writeLong( stats.getTotalPingsReceived());
		os.writeLong( stats.getTotalFindNodesReceived());
		os.writeLong( stats.getTotalFindValuesReceived());
		os.writeLong( stats.getTotalStoresReceived());
		os.writeLong( stats.getAverageBytesReceived());
		os.writeLong( stats.getAverageBytesSent());
		os.writeLong( stats.getAveragePacketsReceived());
		os.writeLong( stats.getAveragePacketsSent());
		
		os.writeLong( stats.getIncomingRequests());
		
		String	azversion = stats.getVersion() + "["+version+"]";
		
		serialiseByteArray( os, azversion.getBytes(), 64);
		
		if ( version >= 5 ){
			
			os.writeLong( stats.getRouterUptime());
			os.writeInt( stats.getRouterCount());
		}
	}
	
	protected static DHTTransportFullStats
	deserialiseStats(
		int					version,
		DataInputStream		is )
	
		throws IOException
	{
		final long db_values_stored				= is.readLong();
		
		final long router_nodes					= is.readLong();
		final long router_leaves				= is.readLong();
		final long router_contacts 				= is.readLong();
		
		final long total_bytes_received			= is.readLong();
		final long total_bytes_sent				= is.readLong();
		final long total_packets_received		= is.readLong();
		final long total_packets_sent			= is.readLong();
		final long total_pings_received			= is.readLong();
		final long total_find_nodes_received	= is.readLong();
		final long total_find_values_received	= is.readLong();
		final long total_stores_received		= is.readLong();
		final long average_bytes_received		= is.readLong();
		final long average_bytes_sent			= is.readLong();
		final long average_packets_received		= is.readLong();
		final long average_packets_sent			= is.readLong();
		
		final long incoming_requests			= is.readLong();
		
		final String	az_version = new String( deserialiseByteArray( is, 64 ));
		
		final long	router_uptime;
		final int	router_count;
		
		if ( version >= 5 ){
			
			router_uptime	= is.readLong();
			router_count	= is.readInt();
		}else{
			
			router_uptime	= 0;
			router_count	= 0;
		}
		
		DHTTransportFullStats	res = 
			new DHTTransportFullStats()
			{
				public long
				getDBValuesStored()
				{
					return( db_values_stored );
				}
				
					// Router
				
				public long
				getRouterNodes()
				{
					return( router_nodes );
				}
				
				public long
				getRouterLeaves()
				{
					return( router_leaves );
				}
				
				public long
				getRouterContacts()
				{
					return( router_contacts );
				}
			
				public long
				getRouterUptime()
				{
					return( router_uptime );
				}
				
				public int
				getRouterCount()
				{
					return( router_count );
				}
				public long
				getTotalBytesReceived()
				{
					return( total_bytes_received );
				}
				
				public long
				getTotalBytesSent()
				{
					return( total_bytes_sent );
				}
				
				public long
				getTotalPacketsReceived()
				{
					return( total_packets_received );
				}
				
				public long
				getTotalPacketsSent()
				{
					return( total_packets_sent );
				}
				
				public long
				getTotalPingsReceived()
				{
					return( total_pings_received );
				}
				
				public long
				getTotalFindNodesReceived()
				{
					return( total_find_nodes_received );
				}
				
				public long
				getTotalFindValuesReceived()
				{
					return( total_find_values_received );
				}
				
				public long
				getTotalStoresReceived()
				{
					return( total_stores_received );
				}
				
					// averages
				
				public long
				getAverageBytesReceived()
				{
					return( average_bytes_received );
				}
				
				public long
				getAverageBytesSent()
				{
					return( average_bytes_sent );
				}
				
				public long
				getAveragePacketsReceived()
				{
					return( average_packets_received );
				}
				
				public long
				getAveragePacketsSent()
				{
					return( average_packets_sent );
				}
				
				public long
				getIncomingRequests()
				{
					return( incoming_requests );
				}
				
				public String
				getVersion()
				{
					return( az_version );
				}
				
				public String
				getString()
				{
					return(	"transport:" + 
							getTotalBytesReceived() + "," +
							getTotalBytesSent() + "," +
							getTotalPacketsReceived() + "," +
							getTotalPacketsSent() + "," +
							getTotalPingsReceived() + "," +
							getTotalFindNodesReceived() + "," +
							getTotalFindValuesReceived() + "," +
							getTotalStoresReceived() + "," +
							getAverageBytesReceived() + "," +
							getAverageBytesSent() + "," +
							getAveragePacketsReceived() + "," +
							getAveragePacketsSent() + "," +
							getIncomingRequests() + 
							",router:" +
							getRouterNodes() + "," +
							getRouterLeaves() + "," +
							getRouterContacts() + 
							",database:" +
							getDBValuesStored()+
							",version:" + getVersion()+","+
							getRouterUptime() + ","+
							getRouterCount());
					}
			};
	
		
		return( res );
	}
}
