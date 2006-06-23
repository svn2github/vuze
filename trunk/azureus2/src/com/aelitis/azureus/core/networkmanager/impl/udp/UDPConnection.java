/*
 * Created on 22 Jun 2006
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

import java.util.*;

import java.io.IOException;
import java.nio.ByteBuffer;

public class 
UDPConnection 
{
	private UDPConnectionSet	set;
	private UDPTransportHelper	transport;
	
	private List	buffers = new LinkedList();
	
	protected
	UDPConnection(
		UDPConnectionSet	_set,
		UDPTransportHelper	_transport )
	{
		set			= _set;
		transport	= _transport;
	}
	
	protected
	UDPConnection(
		UDPConnectionSet	_set )
	{
		set			= _set;
	}
	
	public boolean
	isIncoming()
	{
		return( transport.isIncoming());
	}
	
	protected void
	setSecret(
		byte[]	session_secret )
	{
		set.setSecret( this, session_secret );
	}
	
	protected void
	setTransport(
		UDPTransportHelper	_transport )
	{
		transport	= _transport;
	}
	
	protected UDPTransportHelper
	getTransport()
	{
		return( transport );
	}
	
	protected void
	receive(
		ByteBuffer		data )
	{
		boolean	was_empty = false;
		
		synchronized( buffers ){
		
			was_empty = buffers.size() == 0;
			
			buffers.add( data );
		}
		
		if ( was_empty ){
			
			transport.canRead();
		}
	}
	
	protected boolean
	canRead()
	{
		synchronized( buffers ){

			return( buffers.size() > 0 );
		}
	}
	
	protected boolean
	canWrite()
	{
		return( set.canWrite( this ));
	}
	
	protected int 
	write( 
		ByteBuffer buffer ) 
	
		throws IOException
	{
		return( set.write( this, buffer ));
	}
	
	protected int
	read(
		ByteBuffer	buffer )
	
		throws IOException
	{
		int	total = 0;
		
		synchronized( buffers ){

			while( buffers.size() > 0 ){
				
				int	rem = buffer.remaining();
				
				if ( rem == 0 ){
					
					break;
				}

				ByteBuffer	b = (ByteBuffer)buffers.get(0);
								
				int	old_limit = b.limit();
				
				if ( b.remaining() > rem ){
					
					b.limit( b.position() + rem );
				}
				
				buffer.put( b );
				
				b.limit( old_limit );
				
				total += rem - buffer.remaining();
				
				if ( b.hasRemaining()){
					
					break;
					
				}else{
					
					buffers.remove(0);
				}
			}
		}
		
		return( total );
	}
	
	protected void
	close()
	{
		set.close( this );
	}
	
	protected void
	poll()
	{
		transport.poll();
	}
}
