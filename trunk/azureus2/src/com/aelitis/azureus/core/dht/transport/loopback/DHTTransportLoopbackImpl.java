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

import java.util.*;
import java.io.IOException;
import java.io.InputStream;

import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.SHA1Hasher;

import com.aelitis.azureus.core.dht.transport.*;

/**
 * @author parg
 *
 */

public class 
DHTTransportLoopbackImpl
	implements DHTTransport
{
	private static long	node_id_seed_next	= 0;
	private static Map	node_map	= new HashMap();
	
	private byte[]				node_id;
	private DHTTransportContact	local_contact;
	
	private int			id_byte_length;
	
	private DHTTransportRequestHandler		request_handler;
	
	public
	DHTTransportLoopbackImpl(
		int							_id_byte_length )
	{	
		id_byte_length	= _id_byte_length;
		
		synchronized( DHTTransportLoopbackImpl.class ){
			
			byte[]	temp = new SHA1Hasher().calculateHash( ( "" + ( node_id_seed_next++ )).getBytes());
			
			node_id	= new byte[id_byte_length];
			
			System.arraycopy( temp, 0, node_id, 0, id_byte_length );
			
			node_map.put( new HashWrapper( node_id ), this );
			
			local_contact	= new DHTTransportLoopbackContactImpl( this, node_id );
		}
	}
	
	public DHTTransportContact
	getLocalContact()
	{
		return( local_contact );
	}
	
	protected DHTTransportLoopbackImpl
	findTarget(
		byte[]		id )
	{
		synchronized( DHTTransportLoopbackImpl.class ){
			
			return((DHTTransportLoopbackImpl)node_map.get( new HashWrapper( id )));
		}
	}
	
	public void
	setRequestHandler(
		DHTTransportRequestHandler	_request_handler )
	{
		request_handler = _request_handler;
	}
	
	protected DHTTransportRequestHandler
	getRequestHandler()
	{
		return( request_handler );
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
		
		DHTTransportContact contact = new DHTTransportLoopbackContactImpl( this, id );
		
		request_handler.contactImported( contact );
	}
	
	public void
	sendPing(
		DHTTransportContact			contact,
		DHTTransportReplyHandler	handler )
	{
		DHTTransportLoopbackImpl	target = findTarget( contact.getID());
		
		if ( target == null ){
			
			handler.failed(contact);
			
		}else{
			
			target.getRequestHandler().pingRequest( new DHTTransportLoopbackContactImpl( target, node_id ));
			
			handler.pingReply(contact);
		}
	}
		
	public void
	sendStore(
		DHTTransportContact			contact,
		DHTTransportReplyHandler	handler,
		byte[]						key,
		byte[]						value )
	{
	}
	
	public void
	sendFindNode(
		DHTTransportContact			contact,
		DHTTransportReplyHandler	handler,
		byte[]						nid )
	{
		DHTTransportLoopbackImpl	target = findTarget( contact.getID());
		
		if ( target == null ){
			
			handler.failed(contact);
			
		}else{
			
			DHTTransportContact[] res =
				target.getRequestHandler().findNodeRequest( 
					new DHTTransportLoopbackContactImpl( target, node_id ),
					nid );
			
			handler.findNodeReply( contact, res );
		}
	}
		
	public void
	sendFindValue(
		DHTTransportContact			contact,
		DHTTransportReplyHandler	handler,
		byte[]						key )
	{
	}
}
