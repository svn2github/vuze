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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


/**
 * @author parg
 *
 */

public class 
DHTUDPPacketData 
	extends DHTUDPPacketRequest
{
	private byte[]	transfer_key;
	private byte[]	key;
	private byte[]	data;
	private int		start_position;
	private int		length;
	private int		total_length;
	
	public
	DHTUDPPacketData(
		long							_connection_id,
		DHTTransportUDPContactImpl		_local_contact,
		DHTTransportUDPContactImpl		_remote_contact )
	{
		super( DHTUDPPacket.ACT_DATA, _connection_id, _local_contact, _remote_contact );
	}
	
	protected
	DHTUDPPacketData(
		DataInputStream		is,
		long				con_id,
		int					trans_id )
	
		throws IOException
	{
		super( is,  DHTUDPPacket.ACT_REQUEST_PING, con_id, trans_id );
		
		transfer_key	= DHTUDPUtils.deserialiseByteArray( is, 64 );
		key				= DHTUDPUtils.deserialiseByteArray( is, 64 );
		start_position	= is.readInt();
		length			= is.readInt();
		total_length	= is.readInt();
		data			= DHTUDPUtils.deserialiseByteArray( is, 65535 );
	}
	
	public void
	serialise(
		DataOutputStream	os )
	
		throws IOException
	{
		super.serialise(os);
		
		DHTUDPUtils.serialiseByteArray( os, transfer_key, 64 );
		DHTUDPUtils.serialiseByteArray( os, key, 64 );
		os.writeInt( start_position );
		os.writeInt( length );
		os.writeInt( total_length );
		DHTUDPUtils.serialiseByteArray( os, data, 65535 );
	}
	
	public void
	setDetails(
		byte[]		_transfer_key,
		byte[]		_key,
		byte[]		_data,
		int			_start_pos,
		int			_length,
		int			_total_length )
	{
		transfer_key		= _transfer_key;
		key					= _key;
		data				= _data;
		start_position		= _start_pos;
		length				= _length;
		total_length		= _total_length;
	}
	
	public byte[]
	getTransferKey()
	{
		return( transfer_key );
	}
	
	public byte[]
	getRequestKey()
	{
		return( key );
	}
	
	public int
	getStartPosition()
	{
		return( start_position );
	}
	
	public int
	getLength()
	{
		return( length );
	}
	
	public int
	getTotalLength()
	{
		return( total_length );
	}
	
	public String
	getString()
	{
		return( super.getString());
	}
}