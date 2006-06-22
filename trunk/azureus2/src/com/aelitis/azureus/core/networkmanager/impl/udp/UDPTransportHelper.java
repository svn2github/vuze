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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import com.aelitis.azureus.core.networkmanager.impl.TransportHelper;

public class 
UDPTransportHelper 
	implements TransportHelper
{
	private UDPConnectionManager	manager;
	private InetSocketAddress		address;
	
	private UDPConnection			connection;
	
	protected
	UDPTransportHelper(
		UDPConnectionManager	_manager,
		InetSocketAddress		_address )
	{
			// outgoing
	
		manager		= _manager;
		address 	= _address;
		
		connection 	= manager.registerOutgoing( this );
	}
	
	protected
	UDPTransportHelper(
		UDPConnectionManager	_manager,
		InetSocketAddress		_address, 
		UDPConnection			_connection )
	{
			// incoming
			
		manager		= _manager;
		address 	= _address;
		connection = _connection;
	}
	
	public InetSocketAddress
	getAddress()
	{
		return( address );
	}
	
	public int 
	write( 
		ByteBuffer buffer ) 
	
		throws IOException
	{
		return( connection.write( buffer ));
	}

    public long 
    write( 
    	ByteBuffer[] 	buffers, 
    	int 			array_offset, 
    	int 			length ) 
    
    	throws IOException
    {
    	throw( new IOException( "not imp" ));
    }

    public int 
    read( 
    	ByteBuffer buffer ) 
    
    	throws IOException
    {
    	throw( new IOException( "not imp" ));
    }

    public long 
    read( 
    	ByteBuffer[] 	buffers, 
    	int 			array_offset, 
    	int 			length ) 
    
    	throws IOException
    {
    	throw( new IOException( "not imp" ));
    }

    public void
    pauseReadSelects()
    {
    	
    }
    
    public void
    pauseWriteSelects()
    {
    	
    }
 
    public void
    resumeReadSelects()
    {
    	
    }
    
    public void
    resumeWriteSelects()
    {
    	
    }
    
    public void
    registerForReadSelects(
    	selectListener	listener,
    	Object			attachment )
    {
    	
    }
    
    public void
    registerForWriteSelects(
    	selectListener	listener,
    	Object			attachment )
    {
    	
    }
    
    public void
    cancelReadSelects()
    {
    	
    }
    
    public void
    cancelWriteSelects()
    {
    	
    }
    
    public void
    close()
    {
    	
    }
}
