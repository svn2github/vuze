/*
 * Created on 12-Jan-2005
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

package com.aelitis.azureus.core.dht.transport.loopback;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import com.aelitis.azureus.core.dht.transport.*;

/**
 * @author parg
 *
 */

public class 
DHTTransportLoopbackImpl
	implements DHTTransport
{
	private int			id_byte_length;
	
	private List		receivers	= new ArrayList();
	
	public
	DHTTransportLoopbackImpl(
		int		_id_byte_length )
	{
		id_byte_length	= _id_byte_length;
	}
	
	public void
	ping(
		DHTTransportContact	contact )
	{
		
	}
	
	public void
	importContact(
		InputStream		is )
	
		throws IOException
	{
		byte[]	id = new byte[id_byte_length];
		
		int	read = 0;
		
		while( read < id.length ){
			
			int	len = is.read( id, read, id.length - read );
		
			if ( len <= 0 ){
				
				throw( new IOException( "read fails" ));
			}
			
			read	+= len;
		}
		
		
	}
	
	public void
	addReceiver(
		DHTTransportReceiver	receiver )
	{
		receivers.add( receiver );
	}
	
	public void
	removeReceiver(
		DHTTransportReceiver	receiver )
	{
		receivers.remove( receiver );
	}
}
