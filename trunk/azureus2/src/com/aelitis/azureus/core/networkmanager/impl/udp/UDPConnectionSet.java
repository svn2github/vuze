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
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class 
UDPConnectionSet 
{
	private UDPConnectionManager	manager;
	private int						local_port;
	private InetSocketAddress		remote_address;
	
	private UDPConnection	lead_connection;
	
	private List	connections = new ArrayList();
	
	protected
	UDPConnectionSet(
		UDPConnectionManager	_manager,
		int						_local_port,
		InetSocketAddress		_remote_address )
	{
		manager			= _manager;
		local_port		= _local_port;
		remote_address	= _remote_address;
	}
	
	public void
	receive(
		int					local_port,
		ByteBuffer			data )
	{
		UDPConnection	connection = null;
		
		boolean	new_connection = false;
		
		synchronized( connections ){

			if ( connections.size() == 0 ){
				
				new_connection	= true;
				
				connection	= new UDPConnection( this );
	
				connections.add( connection );
				
				lead_connection	= connection;
			}
		}
		
		connection.receive( data );
		
		if ( new_connection ){
			
			manager.accept( local_port, remote_address, connection );
		}
	}
	
	protected void
	add(
		UDPConnection	connection )
	{
		synchronized( connections ){
			
			connections.add( connection );
			
			if ( connections.size() == 1 ){
				
				lead_connection = connection;
			}
		}
	}
	
	protected int 
	write( 
		UDPConnection	connection,
		ByteBuffer 		buffer ) 
	
		throws IOException
	{
		return( manager.send( local_port, remote_address, buffer ));
	}
}
