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

import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.net.udp.PRUDPPacketRequest;

/**
 * @author parg
 *
 */

public class 
DHTUDPPacketRequest 
	extends PRUDPPacketRequest
{
	private short	version;
	private byte[]	originator_id;
	private int		originator_instance_id;
	
	public
	DHTUDPPacketRequest(
		int					_type,
		long				_connection_id,
		DHTTransportContact	_contact )
	{
		super( _type, _connection_id );
		
		version	= DHTUDPPacket.VERSION;
		
		originator_id			= _contact.getID();
		originator_instance_id	= _contact.getInstanceID();
	}
	
	protected
	DHTUDPPacketRequest(
		DataInputStream		is,
		int					type,
		long				con_id,
		int					trans_id )
	
		throws IOException
	{
		super( type, con_id, trans_id );
		
		version	= is.readShort();
		
		originator_id = DHTUDPUtils.deserialiseByteArray( is );
		
		originator_instance_id	= is.readInt();
	}
	
	public int
	getVersion()
	{
		return( version );
	}
	
	public void
	setOriginatorID(
		byte[]		id )
	{
		originator_id	= id;
	}
	
	public byte[]
	getOriginatorID()
	{
		return( originator_id );
	}
	
	public int
	getOriginatorInstanceID()
	{
		return( originator_instance_id );
	}
	
	public void
	serialise(
		DataOutputStream	os )
	
		throws IOException
	{
		super.serialise(os);
		
		os.writeShort( version );
		
		DHTUDPUtils.serialiseByteArray( os, originator_id );
		
		os.writeInt( originator_instance_id );
	}
}
