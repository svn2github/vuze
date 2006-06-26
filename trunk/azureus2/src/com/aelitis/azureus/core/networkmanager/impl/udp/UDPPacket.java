/*
 * Created on 26 Jun 2006
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.networkmanager.impl.udp;


public class 
UDPPacket 
{
	private final UDPConnection		connection;
	private final int				sequence;
	private final byte[]			buffer;
	
	protected
	UDPPacket(
		UDPConnection	_connection,
		int				_sequence,
		byte[]			_buffer )
	{
		connection	= _connection;
		sequence	= _sequence;
		buffer		= _buffer;
	}
		
	protected UDPConnection
	getConnection()
	{
		return( connection );
	}
	
	protected int
	getSequence()
	{
		return( sequence );
	}
	
	protected byte[]
	getBuffer()
	{
		return( buffer );
	}
}
