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

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AEThread;
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
	public static 		boolean	SYNCHRONOUS	= true;
	public static 		int		LATENCY		= 0;
	
	public static void
	setSynchronous(
		boolean	b )
	{
		SYNCHRONOUS	= b;
	}
	
	public static void
	setLatency(
		int	_latency )
	{
		LATENCY	= _latency;
	}
	
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
	
	protected void
	run(
		final AERunnable	r )
	{
		if ( SYNCHRONOUS ){
			
			r.runSupport();
		}else{
			
			new AEThread( "DHTTransportLoopback")
			{
				public void
				runSupport()
				{
					if ( LATENCY > 0 ){
						
						try{
							Thread.sleep( LATENCY );
							
						}catch( Throwable e ){
							
						}
					}
					r.runSupport();
				}
			}.start();
		}
	}
	
		// transport
	
		// PING 
	
	public void
	sendPing(
		final DHTTransportContact			contact,
		final DHTTransportReplyHandler		handler )
	{
		AERunnable	runnable = 
			new AERunnable()
			{
				public void
				runSupport()
				{
					sendPingSupport( contact, handler );
				}
			};
		
		run( runnable );
	}
	
	public void
	sendPingSupport(
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
		
		// STORE
	
	public void
	sendStore(
		final DHTTransportContact		contact,
		final DHTTransportReplyHandler	handler,
		final byte[]					key,
		final byte[]					value )
	{
		AERunnable	runnable = 
			new AERunnable()
			{
				public void
				runSupport()
				{
					sendStoreSupport( contact, handler, key, value );
				}
			};
		
		run( runnable );
	}
	
	public void
	sendStoreSupport(
		DHTTransportContact			contact,
		DHTTransportReplyHandler	handler,
		byte[]						key,
		byte[]						value )
	{
		DHTTransportLoopbackImpl	target = findTarget( contact.getID());
		
		if ( target == null ){
			
			handler.failed(contact);
			
		}else{
			
			target.getRequestHandler().storeRequest( 
					new DHTTransportLoopbackContactImpl( target, node_id ),
					key, value );
			
			handler.storeReply( contact );
		}
	}
	
		// FIND NODE
	
	public void
	sendFindNode(
		final DHTTransportContact		contact,
		final DHTTransportReplyHandler	handler,
		final byte[]					nid )
	{
		AERunnable	runnable = 
			new AERunnable()
			{
				public void
				runSupport()
				{
					sendFindNodeSupport( contact, handler, nid );
				}
			};
		
		run( runnable );
	}
	
	public void
	sendFindNodeSupport(
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
			
			DHTTransportContact[] trans_res = new DHTTransportContact[res.length];
																	  														  
			for (int i=0;i<res.length;i++){
				
				trans_res[i] = new DHTTransportLoopbackContactImpl( this, res[i].getID());
			}
			
			handler.findNodeReply( contact, trans_res );
		}
	}
	
		// FIND VALUE
	
	public void
	sendFindValue(
		final DHTTransportContact		contact,
		final DHTTransportReplyHandler	handler,
		final byte[]					key )
	{
		AERunnable	runnable = 
			new AERunnable()
			{
				public void
				runSupport()
				{
					sendFindValueSupport( contact, handler, key );
				}
			};
		
		run( runnable );
	}
	
	public void
	sendFindValueSupport(
		DHTTransportContact			contact,
		DHTTransportReplyHandler	handler,
		byte[]						key )
	{
		DHTTransportLoopbackImpl	target = findTarget( contact.getID());
		
		if ( target == null ){
			
			handler.failed(contact);
			
		}else{
			
			Object o_res =
				target.getRequestHandler().findValueRequest( 
					new DHTTransportLoopbackContactImpl( target, node_id ),
					key );
			
			if ( o_res instanceof DHTTransportContact[]){
				
				DHTTransportContact[]	res  = (DHTTransportContact[])o_res;
				
				DHTTransportContact[] trans_res = new DHTTransportContact[res.length];
				  
				for (int i=0;i<res.length;i++){
				
					trans_res[i] = new DHTTransportLoopbackContactImpl( this, res[i].getID());
				}

				handler.findValueReply( contact, trans_res );
				
			}else{
				
				handler.findValueReply( contact, (byte[])o_res );
			}
		}
	}
}
