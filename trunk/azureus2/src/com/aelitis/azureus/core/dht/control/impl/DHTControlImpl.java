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
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.SHA1Hasher;

import com.aelitis.azureus.core.dht.impl.*;
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
	private int			CACHE_AT_CLOSEST_N	= 1;	// TODO: parameterise
	
	private Map			stored_values = new HashMap();
	
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
			
			// TODO: ID changes/incompatabilities
		
		transport.setRequestHandler( this );
		
		router.setNodeID( local_contact.getID(), local_contact );
	}
	
	public void
	contactImported(
		DHTTransportContact	contact )
	{
		try{
			DHTLog.indent( router );

			byte[]	id = contact.getID();
		
			router.addContact( id, contact);
		
			lookup( id, false );
		
			router.print();
			
		}finally{
			
			DHTLog.exdent();
		}
	}
	
	public void
	put(
		byte[]		unencoded_key,
		byte[]		value )
	{
		try{
			DHTLog.indent( router );
			
			final byte[]	encoded_key = encodeKey( unencoded_key );
	
			DHTLog.log( "put for " + DHTLog.getString( encoded_key ));
		
			List	closest = lookup( encoded_key, false );
			
			for (int i=0;i<closest.size();i++){
			
				((DHTTransportContact)closest.get(i)).sendStore( 
						new DHTTransportReplyHandlerAdapter()
						{
							public void
							storeReply(
								DHTTransportContact contact )
							{
								DHTLog.indent( router );
								
								DHTLog.log( "store ok" );
								
								DHTLog.exdent();
							}	
							
							public void
							failed(
								DHTTransportContact 	contact )
							{
								DHTLog.indent( router );
								
								DHTLog.log( "store failed" );
								
								DHTLog.exdent();
							}
						},
						encoded_key, 
						value );
			}
		}finally{
			
			DHTLog.exdent();
		}
	}
	
	public byte[]
	get(
		byte[]		unencoded_key )
	{
		try{		
			DHTLog.indent( router );
			
			final byte[]	encoded_key = encodeKey( unencoded_key );

			DHTLog.log( "get for " + DHTLog.getString( encoded_key ));

				// maybe we've got the value already
			
			byte[]	local_result = (byte[])stored_values.get( new HashWrapper( encoded_key ));
			
			if ( local_result != null ){
				
				DHTLog.log( "    surprisingly we've got it locally!" );

				return( local_result );
			}
			
			List	result_and_closest = lookup( encoded_key, true );
	
			byte[]	value = (byte[])result_and_closest.get(0);
			
			if ( value != null ){
				
					// cache the value at the 'n' closest seen locations
				
				for (int i=1;i<Math.min(1+CACHE_AT_CLOSEST_N,result_and_closest.size());i++){
					
					((DHTTransportContact)result_and_closest.get(i)).sendStore( 
							new DHTTransportReplyHandlerAdapter()
							{
								public void
								storeReply(
									DHTTransportContact contact )
								{
									DHTLog.indent( router );
									
									DHTLog.log( "cache store ok" );
									
									DHTLog.exdent();
								}	
								
								public void
								failed(
									DHTTransportContact 	contact )
								{
									DHTLog.indent( router );
									
									DHTLog.log( "cache store failed" );
									
									DHTLog.exdent();
								}
							},
							encoded_key, 
							value );
				}
			}
			
			return( value );
			
		}finally{
			
			DHTLog.exdent();
		}
	}
	
		/**
		 * The lookup method returns up to K closest nodes to the target
		 * @param lookup_id
		 * @return
		 */
	
	protected List
	lookup(
		final byte[]	lookup_id,
		boolean			value_search )
	{
		try{		
			DHTLog.indent( router );
			
			DHTLog.log( "lookup for " + DHTLog.getString( lookup_id ));
			
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
			
			
			List	router_contacts	= router.findClosestContacts( lookup_id );
			
			final Set	contacts_to_query	= 
				new TreeSet(
						new Comparator()
						{
							public int
							compare(
								Object	o1,
								Object	o2 )
							{
									// this comparator ensures that the closest to the key
									// is first in the iterator traversal
							
								DHTTransportContact	t1 = (DHTTransportContact)o1;
								DHTTransportContact t2 = (DHTTransportContact)o2;
								
								byte[] d1 = computeDistance( t1.getID(), lookup_id );
								byte[] d2 = computeDistance( t2.getID(), lookup_id );
								
								return( compareDistances( d1, d2 ));
							}
						});
			
			for (int i=0;i<router_contacts.size();i++){
				
				contacts_to_query.add(
					((DHTRouterContact)router_contacts.get(i)).getAttachment());
			}
			
				// record the set of contacts we've queried to avoid re-queries
			
			final Map			contacts_queried = new HashMap();
			
				// record the set of contacts that we've had a reply from
			
			final Set			ok_contacts = 
				new TreeSet(
					new Comparator()
					{
						public int
						compare(
							Object	o1,
							Object	o2 )
						{
								// this comparator ensures that the furthest away from the key
								// is first in the iterator traversal
						
							DHTTransportContact	t1 = (DHTTransportContact)o1;
							DHTTransportContact t2 = (DHTTransportContact)o2;
							
							byte[] d1 = computeDistance( t1.getID(), lookup_id );
							byte[] d2 = computeDistance( t2.getID(), lookup_id );
							
							return( -compareDistances( d1, d2 ));
						}
					});
			
				// this handles the search concurrency
			
			final AESemaphore	search_sem = new AESemaphore( "DHTControl:search", search_concurrency );
				
			final int[]	idle_searches	= { 0 };
			final int[]	active_searches	= { 0 };
			
			final byte[][]	value_search_result = {null};
			
			while( true ){
				
					// get permission to kick off another search
				
				search_sem.reserve();				

				if ( value_search_result[0] != null ){
					
					break;
				}
				
				synchronized( contacts_to_query ){
			
						// if nothing pending then we need to wait for the results of a previous
						// search to arrive. Of course, if there are no searches active then
						// we've run out of things to do
					
					if ( contacts_to_query.size() == 0 ){
						
						if ( active_searches[0] == 0 ){
							
							DHTLog.log( "lookup: terminates as not contacts left to query" );
							
							break;
						}
						
						idle_searches[0]++;
						
						continue;
					}
				
						// select the next contact to search
					
					DHTTransportContact	closest	= (DHTTransportContact)contacts_to_query.iterator().next();			
				
						// if the next closest is further away than the furthest successful hit so 
						// far and we have K hits, we're done
					
					if ( ok_contacts.size() == router.getK()){
						
						DHTTransportContact	furthest_ok = (DHTTransportContact)ok_contacts.iterator().next();
						
						byte[]	furthest_ok_distance 	= computeDistance( furthest_ok.getID(), lookup_id );
						byte[]	closest_distance		= computeDistance( closest.getID(), lookup_id );
						
						if ( compareDistances( furthest_ok_distance, closest_distance) <= 0 ){
							
							DHTLog.log( "lookup: terminates as we've searched the closest K contacts" );
	
							break;
						}
					}
					
					contacts_to_query.remove( closest );
					
					contacts_queried.put( new HashWrapper( closest.getID()), "" );
					
					active_searches[0]++;
					
					DHTTransportReplyHandlerAdapter	handler = 
						new DHTTransportReplyHandlerAdapter()
						{
							public void
							findNodeReply(
								DHTTransportContact 	target_contact,
								DHTTransportContact[]	reply_contacts )
							{
								try{
									DHTLog.indent( router );
									
									DHTLog.log( "findNodeReply: " + DHTLog.getString( reply_contacts ));
							
									router.contactAlive( target_contact.getID(), target_contact );
									
									synchronized( contacts_to_query ){
												
										ok_contacts.add( target_contact );
										
										if ( ok_contacts.size() > router.getK()){
											
												// delete the furthest away
											
											Iterator it = ok_contacts.iterator();
											
											it.next();
											
											it.remove();
										}
										
										for (int i=0;i<reply_contacts.length;i++){
											
											DHTTransportContact	contact = reply_contacts[i];
											
												// ignore responses that are ourselves
											
											if ( compareDistances( router.getLocalContact().getID(), contact.getID()) == 0 ){
												
												continue;
											}
											
												// dunno if its alive or not, however record its existance
											
											router.addContact( contact.getID(), contact );
											
											if ( contacts_queried.get( new HashWrapper( contact.getID())) == null ){
												
												DHTLog.log( "    new contact for query: " + DHTLog.getString( contact ));
												
												contacts_to_query.add( contact );
												
												if ( idle_searches[0] > 0 ){
													
													idle_searches[0]--;
													
													search_sem.release();
												}
											}else{
												
												DHTLog.log( "    already queried: " + DHTLog.getString( contact ));
												
											}
										}
									}
								}finally{
									
									active_searches[0]--;								
		
									search_sem.release();
									
									DHTLog.exdent();
								}
							}
							
							public void
							findValueReply(
								DHTTransportContact 	contact,
								byte[]					value )
							{
								try{
									DHTLog.indent( router );
									
									DHTLog.log( "findValueReply: " + DHTLog.getString( value ));
									
									value_search_result[0]	= value;
		
								}finally{
														
									active_searches[0]--;
									
									search_sem.release();
									
									DHTLog.exdent();
								}						
							}
							
							public void
							findValueReply(
								DHTTransportContact 	contact,
								DHTTransportContact[]	contacts )
							{
								findNodeReply( contact, contacts );
							}
							
							public void
							failed(
								DHTTransportContact 	target_contact )
							{
								try{
									DHTLog.indent( router );
									
									DHTLog.log( "Reply: findNode/findValue -> failed" );
									
									router.contactDead( target_contact.getID(), target_contact );
		
								}finally{
									
									
									active_searches[0]--;
									
									search_sem.release();
									
									DHTLog.exdent();
								}
							}
						};
						
					if ( value_search ){
						
						closest.sendFindValue( handler, lookup_id );
						
					}else{
						
						closest.sendFindNode( handler, lookup_id );
					}
				}
			}
			
				// maybe unterminated searches still going on so protect ourselves
				// against concurrent modification of result set
			
			synchronized( contacts_to_query ){

				DHTLog.log( "lookup complete for " + DHTLog.getString( lookup_id ));
				
				DHTLog.log( "    queried = " + DHTLog.getString( contacts_queried ));
				DHTLog.log( "    to query = " + DHTLog.getString( contacts_to_query ));
				DHTLog.log( "    ok = " + DHTLog.getString( ok_contacts ));
				
				ArrayList	res;
				
				if ( value_search ){
					
					res = new ArrayList( ok_contacts.size() + 1 );
					
					res.add( value_search_result[0]);
					
					res.addAll( ok_contacts );
				}else{
					
					res = new ArrayList( ok_contacts );
				}
				
				return( res );
			}
			
		}finally{
			
			DHTLog.exdent();
		}
	}
	
	
		// Request methods
	
	public void
	pingRequest(
		DHTTransportContact originating_contact )
	{
		try{		
			DHTLog.indent( router );

			DHTLog.log( "pingRequest from " + DHTLog.getString( originating_contact.getID()));
			
			router.contactAlive( originating_contact.getID(), originating_contact );
			
		}finally{
			
			DHTLog.exdent();
		}
	}
		
	public void
	storeRequest(
		DHTTransportContact originating_contact, 
		byte[]				key,
		byte[]				value )
	{
		try{		
			DHTLog.indent( router );

			DHTLog.log( "storeRequest from " + DHTLog.getString( originating_contact.getID()));

			router.contactAlive( originating_contact.getID(), originating_contact );
	
			stored_values.put( new HashWrapper( key ), value );
			
		}finally{
			
			DHTLog.exdent();
		}
	}
	
	public DHTTransportContact[]
	findNodeRequest(
		DHTTransportContact originating_contact, 
		byte[]				id )
	{
		try{		
			DHTLog.indent( router );

			DHTLog.log( "findNodeRequest from " + DHTLog.getString( originating_contact.getID()));

			router.contactAlive( originating_contact.getID(), originating_contact );
	
			List	l = router.findClosestContacts( id );
			
			DHTTransportContact[]	res = new DHTTransportContact[l.size()];
			
			for (int i=0;i<res.length;i++){
				
				res[i] = (DHTTransportContact)((DHTRouterContact)l.get(i)).getAttachment();
			}
			
			return( res );
			
		}finally{
			
			DHTLog.exdent();
		}
	}
	
	public Object
	findValueRequest(
		DHTTransportContact originating_contact, 
		byte[]				key )
	{
		try{		
			DHTLog.indent( router );

			DHTLog.log( "findValueRequest from " + DHTLog.getString( originating_contact.getID()));

			byte[]	value = (byte[])stored_values.get( new HashWrapper( key ));
			
			if ( value != null ){
				
				router.contactAlive( originating_contact.getID(), originating_contact );
	
				return( value );
				
			}else{
				
				return( findNodeRequest( originating_contact, key ));
			}
		}finally{
			
			DHTLog.exdent();
		}
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
	
	protected static byte[]
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
	
	protected static int
	compareDistances(
		byte[]		n1,
		byte[]		n2 )
	{
		for (int i=0;i<n1.length;i++){
			
			int diff = (n1[i]&0xff) - (n2[i]&0xff);
			
			if ( diff != 0 ){
				
				return( diff );
			}
		}
		
		return( 0 );
	}
	
	protected String
	bytesToString(
		byte[]	b )
	{
		return( ByteFormatter.nicePrint( b ));
	}
	
	protected String
	getIndent()
	{
		return( "" );
	}
}
