/*
 * Created on 17-Jan-2006
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

package com.aelitis.azureus.core.networkmanager.impl;

import java.io.IOException;
import java.nio.ByteBuffer;

public class 
TCPTransportHelperFilterStreamXOR
	extends TCPTransportHelperFilterStream
{
	private byte[]		mask;
	
	protected
	TCPTransportHelperFilterStreamXOR(
		TCPTransportHelper		_transport,
		byte[]					_mask )
	{
		super( _transport );
		
		mask		= _mask;
	}
	
	protected void
	write(
		ByteBuffer	source_buffer,
		ByteBuffer	target_buffer )
	
		throws IOException
	{		
		target_buffer.put( source_buffer );
	}
	
	protected void
	read(
		ByteBuffer	source_buffer,
		ByteBuffer	target_buffer )
	
		throws IOException
	{		
		target_buffer.put( source_buffer );
	}
	
	public String
	getName()
	{
		return( "XOR-" + mask.length*8 );
	}
}
