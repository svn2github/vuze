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
import org.gudy.azureus2.core3.util.SHA1Hasher;


import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportException;
import com.aelitis.azureus.core.dht.transport.DHTTransportFullStats;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

/**
 * @author parg
 *
 */

public class 
DHTUDPUtils 
{
	protected static final int	CT_UDP		= 1;
	
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
			
			byte[]	res = new SHA1Hasher().calculateHash(
						(	ia.getHostAddress() + ":" + address.getPort()).getBytes());
			
			//System.out.println( "NodeID: " + address + " -> " + DHTLog.getString( res ));
			
			return( res );
		}
	}
	
	protected static byte[]
	deserialiseByteArray(
		DataInputStream	is,
		int				max_length )
	
		throws IOException
	{
		int	len;
		
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
		int	len = data.length;
		
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
		
		os.write( data );
	}
	
	protected static DHTTransportValue[]
	deserialiseTransportValues(
		DHTTransportUDPImpl		transport,
		DataInputStream			is,
		long					skew )
	
		throws IOException
	{
		short	len = is.readShort();
		
		if ( len > 1024 ){
			
			throw( new IOException( "too many values" ));
		}
		
		List	l = new ArrayList( len );
		
		for (int i=0;i<len;i++){
			
			try{
				
				l.add( deserialiseTransportValue( transport, is, skew ));
				
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
		DataOutputStream		os,
		DHTTransportValue[]		values,
		long					skew )
	
		throws IOException, DHTTransportException
	{
		os.writeShort( values.length );
	
		for (int i=0;i<values.length;i++){
			
			
			serialiseTransportValue( os, values[i], skew );
		}
	}
	
	protected static DHTTransportValue
	deserialiseTransportValue(
		DHTTransportUDPImpl	transport,
		DataInputStream		is, 
		long				skew )
	
		throws IOException, DHTTransportException
	{
		final int	distance	= is.readInt();
		
		final long 	created		= is.readLong() + skew;
		
		// System.out.println( "    Adjusted creation time by " + skew );
		
		final byte[]	value_bytes = deserialiseByteArray( is, 256 );
		
		final DHTTransportContact	originator		= deserialiseContact( transport, is );
		
		final int flags	= is.readByte()&0xff;
		
		DHTTransportValue value = 
			new DHTTransportValue()
			{
				public int
				getCacheDistance()
				{
					return( distance );
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
	
	protected static void
	serialiseTransportValue(
		DataOutputStream	os,
		DHTTransportValue	value,
		long				skew )
	
		throws IOException, DHTTransportException
	{
		os.writeInt( value.getCacheDistance());
		
		os.writeLong( value.getCreationTime() + skew );
		
		serialiseByteArray( os, value.getValue(), 256 );
		
		serialiseContact( os, value.getOriginator());
		
		os.writeByte( value.getFlags());
	}
	
	protected static void
	serialiseContacts(
		DataOutputStream		os,
		DHTTransportContact[]	contacts )
	
		throws IOException
	{
		os.writeShort( contacts.length );
		
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
		short	len = is.readShort();
		
		if ( len > 1024 ){
			
			throw( new IOException( "too many contacts" ));
		}
		
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
	serialiseContact(
		DataOutputStream		os,
		DHTTransportContact		contact )
	
		throws IOException, DHTTransportException
	{
		if ( contact instanceof DHTTransportUDPContactImpl ){
			
			os.writeByte( CT_UDP );
			
			os.writeByte( contact.getProtocolVersion());
			
			DHTTransportUDPContactImpl c = (DHTTransportUDPContactImpl)contact;
								
			serialiseAddress( os, c.getExternalAddress() );
			
		}else{
			
			throw( new IOException( "Unsupported contact type:" + contact.getClass().getName()));
		}
	}
	
	protected static DHTTransportContact
	deserialiseContact(
		DHTTransportUDPImpl		transport,	// TODO: multiple transport support
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
		
		serialiseByteArray( os, ia.getAddress(), 16);
		
		os.writeShort( address.getPort());
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
	
	protected static void
	serialiseStats(
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
	}
	
	protected static DHTTransportFullStats
	deserialiseStats(
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
							", router:" +
							getRouterNodes() + "," +
							getRouterLeaves() + "," +
							getRouterContacts() + 
							",database:" +
							getDBValuesStored());
				}
			};
	
		
		return( res );
	}
}
