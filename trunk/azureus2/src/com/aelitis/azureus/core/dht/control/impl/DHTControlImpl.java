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

package com.aelitis.azureus.core.dht.control.impl;

import java.util.*;

import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.SHA1Hasher;

import com.aelitis.azureus.core.dht.control.*;
import com.aelitis.azureus.core.dht.router.*;
import com.aelitis.azureus.core.dht.transport.*;

/**
 * @author parg
 *
 */

public class 
DHTControlImpl 
	implements DHTControl, DHTTransportRequestHandler
{
	private DHTRouter	router;
	private	int			node_id_byte_count	= 0;
	private int			search_concurrency	= 3;	// TODO: fix
	
	public
	DHTControlImpl(
		DHTRouter		_router )
	{
		router	= _router;
	}
	
	public void
	addTransport(
		DHTTransport	transport )
	{
		DHTTransportContact	local_contact = transport.getLocalContact();
		
		int	bc = local_contact.getID().length;
		
		if ( node_id_byte_count == 0 ){
			
			node_id_byte_count = bc;
			
		}else if ( node_id_byte_count != bc ){
			
			throw( new RuntimeException( "DHTRouter: transports have different node id size!" ));
		}
			
		transport.setRequestHandler( this );
		
		router.setNodeID( local_contact.getID(), local_contact );
	}
	
	public void
	contactImported(
		DHTTransportContact	contact )
	{
		router.addContact( contact.getID(), contact);
	}
	
	public void
	put(
		byte[]		unencoded_key,
		byte[]		value )
	{
		byte[]	encoded_key = encodeKey( unencoded_key );
								
			// keep querying successively closer nodes until we have got responses from the K
			// closest nodes that we've seen. We might get a bunch of closer nodes that then
			// fail to respond, which means we have reconsider further away nodes
		
			// we keep a list of nodes that we have queried to avoid re-querying them
		
			// we keep a list of nodes discovered that we have yet to query
		
			// we have a parallel search limit of A. For each A we effectively loop grabbing
			// the currently closest unqueried node, querying it and adding the results to the
			// yet-to-query-set (unless already queried)
		
			// we terminate when we have received responses from the K closest nodes we know
			// about (excluding failed ones)
		
			// Note that we never widen the root of our search beyond the initial K closest
			// that we know about - this could be relaxed
		
		List	router_contacts	= router.findClosestContacts( encoded_key );
		
		final List	contacts_to_query	= new ArrayList();
		
		for (int i=0;i<router_contacts.size();i++){
			
			contacts_to_query.add(
				((DHTRouterContact)router_contacts.get(i)).getAttachment());
		}
		
		final Map			contacts_queried = new HashMap();
		
		final AESemaphore	search_sem = new AESemaphore( "DHTControl:search", search_concurrency );
			
		final int[]	idle_searches	= { 0 };
		final int[]	active_searches	= { 0 };
		
		while( true ){
			
				// get permission to kick off another search
			
			search_sem.reserve();
					
			synchronized( contacts_to_query ){
		
					// if nothing pending then we need to wait for the results of a previous
					// search to arrive. Of course, if there are no searches active then
					// we've run out of things to do
				
				if ( contacts_to_query.size() == 0 ){
					
					if ( active_searches[0] == 0 ){
						
						break;
					}
					
					idle_searches[0]++;
					
					continue;
				}
			
					// select the next contact to search
				
				DHTTransportContact	closest	= null;
				
				byte[]	closest_distance	= null;
				
				for (int i=0;i<contacts_to_query.size();i++){
					
					DHTTransportContact	contact = (DHTTransportContact)contacts_to_query.get(i);
					
					byte[]	new_distance = computeDistance( contact.getID(), encoded_key );
	
					if ( closest == null ){
						
						closest 			= contact;
						
						closest_distance	= new_distance;
						
					}else{
											
						if ( compareDistances( new_distance, closest_distance ) < 0 ){
						
							closest 			= contact;
							
							closest_distance	= new_distance;
						}
					}
				}			
			
				contacts_to_query.remove( closest );
				
				contacts_queried.put( new HashWrapper( closest.getID()), "" );
				
				active_searches[0]++;
				
				closest.sendFindNode(
					new DHTTransportReplyHandlerAdapter()
					{
						public void
						findNodeReply(
							DHTTransportContact 	target_contact,
							DHTTransportContact[]	reply_contacts )
						{
							try{
								System.out.println( "Reply: findNode -> " + reply_contacts.length );
						
								router.contactAlive( target_contact.getID(), target_contact );
								
								synchronized( contacts_to_query ){
	
										//TODO: adjust idle searches + release sem
																	
									for (int i=0;i<reply_contacts.length;i++){
										
										DHTTransportContact	contact = reply_contacts[i];
										
										router.addContact( contact.getID(), contact );
										
										if ( contacts_queried.get( new HashWrapper( contact.getID())) == null ){
											
											contacts_to_query.add( contact );
											
											if ( idle_searches[0] > 0 ){
												
												idle_searches[0]--;
												
												search_sem.release();
											}
										}
									}
								}
							}finally{
								
								active_searches[0]--;								

								search_sem.release();
							}
						}
						
						public void
						failed(
							DHTTransportContact 	target_contact )
						{
							try{
								System.out.println( "Reply: findNode -> failed" );
								
								router.contactDead( target_contact.getID(), target_contact );

							}finally{
								
								
								active_searches[0]--;
								
								search_sem.release();
							}
						}
					},
					encoded_key );
			}
		}
	}
	
	
		// Request methods
	
	public void
	pingRequest(
		DHTTransportContact originating_contact )
	{
		router.contactAlive( originating_contact.getID(), originating_contact );
	}
		
	public void
	storeRequest(
		DHTTransportContact originating_contact, 
		byte[]				key,
		byte[]				value )
	{
		router.contactAlive( originating_contact.getID(), originating_contact );

		throw( new RuntimeException(""));
	}
	
	public DHTTransportContact[]
	findNodeRequest(
		DHTTransportContact originating_contact, 
		byte[]				id )
	{
		router.contactAlive( originating_contact.getID(), originating_contact );

		List	l = router.findClosestContacts( id );
		
		DHTTransportContact[]	res = new DHTTransportContact[l.size()];
		
		for (int i=0;i<res.length;i++){
			
			res[i] = (DHTTransportContact)((DHTRouterContact)l.get(i)).getAttachment();
		}
		
		return( res );
	}
	
	public Object
	findValueRequest(
		DHTTransportContact originating_contact, 
		byte[]				key )
	{
		router.contactAlive( originating_contact.getID(), originating_contact );

		throw( new RuntimeException(""));
	}
	
	protected byte[]
	encodeKey(
		byte[]		key )
	{
		byte[]	temp = new SHA1Hasher().calculateHash( key );
		
		byte[]	result =  new byte[node_id_byte_count];
		
		System.arraycopy( temp, 0, result, 0, node_id_byte_count );
		
		return( result );
	}
	
	protected byte[]
	computeDistance(
		byte[]		n1,
		byte[]		n2 )
	{
		byte[]	res = new byte[n1.length];
		
		for (int i=0;i<res.length;i++){
			
			res[i] = (byte)( n1[i] ^ n2[i] );
		}
		
		return( res );
	}
	
		/**
		 * -ve -> n1 < n2
		 * @param n1
		 * @param n2
		 * @return
		 */
	protected int
	compareDistances(
		byte[]		n1,
		byte[]		n2 )
	{
		for (int i=0;i<n1.length;i++){
			
			int diff = n1[i] - n2[i];
			
			if ( diff != 0 ){
				
				return( diff );
			}
		}
		
		return( 0 );
	}
}
