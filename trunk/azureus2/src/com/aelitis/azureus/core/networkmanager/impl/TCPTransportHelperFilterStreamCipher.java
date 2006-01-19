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

import javax.crypto.Cipher;
import javax.crypto.ShortBufferException;

import org.gudy.azureus2.core3.util.Debug;

public class 
TCPTransportHelperFilterStreamCipher 
	extends TCPTransportHelperFilterStream
{
	private Cipher					read_cipher;
	private Cipher					write_cipher;
		
	protected
	TCPTransportHelperFilterStreamCipher(
		TCPTransportHelper		_transport,
		Cipher					_read_cipher,
		Cipher					_write_cipher )
	{
		super( _transport );
		
		read_cipher		= _read_cipher;
		write_cipher	= _write_cipher;
	}
	
	protected void
	write(
		ByteBuffer	source_buffer,
		ByteBuffer	target_buffer )
	
		throws IOException
	{
		try{
			write_cipher.update( source_buffer, target_buffer );
			
		}catch( ShortBufferException e ){
			
			throw( new IOException( Debug.getNestedExceptionMessage( e )));
		}
	}
	
	protected void
	read(
		ByteBuffer	source_buffer,
		ByteBuffer	target_buffer )
	
		throws IOException
	{
		try{
			read_cipher.update( source_buffer, target_buffer );
			
		}catch( ShortBufferException e ){
			
			throw( new IOException( Debug.getNestedExceptionMessage( e )));
		}
	}		
}
