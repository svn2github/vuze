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
import java.net.InetSocketAddress;

import org.gudy.azureus2.core3.util.SHA1Hasher;

import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
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
	{		
		return( new SHA1Hasher().calculateHash( address.toString().getBytes()));
	}
	
	protected static byte[]
	deserialiseID(
		DataInputStream	is )
	
		throws IOException
	{
		int	key_len	= is.readInt();
		
		if ( key_len > 1024 ){
			
			throw( new IOException( "Invalid key length" ));
		}
		
		byte[] key	= new byte[key_len];
		
		is.read(key);
		
		return( key );
	}
	
	protected static void
	serialiseID(
		DataOutputStream	os,
		byte[]				key )
	
		throws IOException
	{
		os.writeInt( key.length );
		
		os.write( key );
	}
	
	protected static DHTTransportValue
	deserialiseTransportValue(
		DataInputStream	is )
	
		throws IOException
	{
		final int	distance	= is.readInt();
		final long 	created		= is.readLong();
		
		int	value_len	= is.readInt();
		
		if ( value_len > 1024 ){
			
			throw( new IOException( "Invalid value length" ));
		}
		
		final byte[]	value_bytes	= new byte[value_len];
		
		is.read(value_bytes);
		
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
			};
			
		return( value );
	}
	
	protected static void
	serialiseTransportValue(
		DataOutputStream	os,
		DHTTransportValue	value )
	
		throws IOException
	{
		os.writeInt( value.getCacheDistance());
		
		os.writeLong( value.getCreationTime());
		
		os.writeInt( value.getValue().length);
		
		os.write( value.getValue());
	}
	
	protected static void
	serialiseContacts(
		DataOutputStream		os,
		DHTTransportContact[]	contacts )
	
		throws IOException
	{
		os.writeInt( contacts.length );
		
		for (int i=0;i<contacts.length;i++){
			
			serialiseContact( os, contacts[i] );
		}
	}

	protected static DHTTransportContact[]
	deserialiseContacts(
		DHTTransportUDPImpl		transport,	// TODO: multiple transport support
		DataInputStream			is )
	
		throws IOException
	{
		int	len = is.readInt();
		
		if ( len > 1024 ){
			
			throw( new IOException( "too many contacts" ));
		}
		
		DHTTransportContact[]	res = new DHTTransportContact[ len ];
		
		for (int i=0;i<res.length;i++){
			
			res[i] = deserialiseContact( transport, is );
		}
									  
		return( res );
	}
											
	protected static void
	serialiseContact(
		DataOutputStream		os,
		DHTTransportContact		contact )
	
		throws IOException
	{
		if ( contact instanceof DHTTransportUDPContactImpl ){
			
			DHTTransportUDPContactImpl c = (DHTTransportUDPContactImpl)contact;
			
			InetSocketAddress address = c.getAddress();
			
			os.writeByte( CT_UDP );
			
			os.writeInt( address.getHostName().length());
			
			os.write( address.getHostName().getBytes());
			
			os.writeShort( address.getPort());
			
		}else{
			
			throw( new IOException( "Unsupported contact type:" + contact.getClass().getName()));
		}
	}
	
	protected static DHTTransportContact
	deserialiseContact(
		DHTTransportUDPImpl		transport,	// TODO: multiple transport support
		DataInputStream			is )
	
		throws IOException
	{
		byte	ct = is.readByte();
		
		if ( ct != CT_UDP ){
			
			throw( new IOException( "Unsupported contact type:" + ct ));
		}
		
		int	name_len = is.readInt();
		
		if ( name_len > 1024 ){
			
			throw( new IOException( "host name too long:" + name_len ));
		}
		
		byte[]	host_name = new byte[ name_len ];
		
		is.read( host_name );
		
		int	port = is.readShort()&0xffff;
		
		return( new DHTTransportUDPContactImpl( transport, new InetSocketAddress(new String(host_name), port )));
	}
}
