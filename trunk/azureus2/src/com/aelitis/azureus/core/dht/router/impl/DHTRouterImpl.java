/*
 * Created on 11-Jan-2005
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

package com.aelitis.azureus.core.dht.router.impl;

import java.util.*;

import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.dht.router.*;

/**
 * @author parg
 *
 */

public class 
DHTRouterImpl
	implements DHTRouter
{
	private static final int	SMALLEST_SUBTREE_MAX_EXCESS	= 512;
	
	private int		K = 2;
	private int		B = 5;
	private int		SMALLEST_SUBTREE_MAX;
	
	
	private byte[]	router_node_id;
	
	private DHTRouterNodeImpl		root;
	private DHTRouterNodeImpl		smallest_subtree;
	
	public
	DHTRouterImpl(
		int		_K,
		int		_B )
	{
		K		= _K;
		B		= _B;
		
		SMALLEST_SUBTREE_MAX	= 1;
		
		for (int i=0;i<B;i++){
			
			SMALLEST_SUBTREE_MAX	*= 2;
		}
		
		SMALLEST_SUBTREE_MAX	+= SMALLEST_SUBTREE_MAX_EXCESS;
	}
	
	public void
	setNodeID(
		byte[]	_router_node_id,
		Object	_attachment )
	{
		router_node_id	= _router_node_id;
		
		List	buckets = new ArrayList();
		
		buckets.add( new DHTRouterContactImpl( router_node_id, _attachment ));
		
		root	= new DHTRouterNodeImpl( this, 0, true, buckets );
		
		smallest_subtree	= root;
	}
	
	public synchronized DHTRouterContact
	addContact(
		byte[]	node_id,
		Object	attachment )
	{
		return( addContact( node_id, attachment, false ));
	}
	
	public synchronized DHTRouterContact
	addContact(
		byte[]	node_id,
		Object	attachment,
		boolean	known_to_be_alive )
	{
			// System.out.println( ByteFormatter.nicePrint( node_id ));
		
		DHTRouterNodeImpl	current_node = root;
			
		boolean	part_of_smallest_subtree	= false;
		
		for (int i=0;i<node_id.length;i++){
			
			byte	b = node_id[i];
			
			int	j = 7;
			
			while( j >= 0 ){
					
				if ( current_node == smallest_subtree ){
					
					part_of_smallest_subtree	= true;
				}
				
				boolean	bit = ((b>>j)&0x01)==1?true:false;
				
				//System.out.print( bit?"1":"0" );
				
				DHTRouterNodeImpl	next_node;
				
				if ( bit ){
					
					next_node = current_node.getLeft();
					
				}else{
					
					next_node = current_node.getRight();
				}
				
				if ( next_node == null ){
		
					List	buckets = current_node.getBuckets();

						// see if we already know about it
					
					for (int k=0;k<buckets.size();k++){
						
						DHTRouterContactImpl	contact = (DHTRouterContactImpl)buckets.get(k);
						
						if ( Arrays.equals(node_id, contact.getID())){

							if ( known_to_be_alive ){
								
								current_node.alive( contact );
							}

							return( contact );
						}
					}

					if ( buckets.size() == K ){
						
							// split if either
							// 1) this list contains router_node_id or
							// 2) depth % B is not 0
							// 3) this is part of the smallest subtree
						
						boolean	contains_router_node_id = current_node.containsRouterNodeID();
						int		depth					= current_node.getDepth();
						
						boolean	too_deep_to_split = depth % B == 0;	// note this will be true for 0 but other
																	// conditions will allow the split
						
						if ( 	contains_router_node_id ||
								(!too_deep_to_split)	||
								part_of_smallest_subtree ){
							
								// the smallest-subtree bit is to ensure that we remember all of
								// our closest neighbours as ultimately they are the ones responsible
								// for returning our identity to queries (due to binary choppery in
								// general the query will home in on our neighbours before
								// hitting us. It is therefore important that we keep ourselves live
								// in their tree by refreshing. If we blindly chopped at K entries
								// (down to B levels) then a highly unbalanced tree would result in
								// us dropping some of them and therefore not refreshing them and
								// therefore dropping out of their trees. 
								// Note that it is rare for such an unbalanced tree. 
								// However, a possible DOS here would be for a rogue node to 
								// deliberately try and create such a tree with a large number
								// of entries.
								
							if ( 	part_of_smallest_subtree &&
									too_deep_to_split &&
									( !contains_router_node_id ) &&
									getContactCount( smallest_subtree ) > SMALLEST_SUBTREE_MAX ){
								
								Debug.out( "DHTRouter: smallest subtree max size violation" );
								
								return( null );	
							}
							
								// split!!!!
							
							List	left_buckets 	= new ArrayList();
							List	right_buckets 	= new ArrayList();
							
							for (int k=0;k<buckets.size();k++){
								
								DHTRouterContactImpl	contact = (DHTRouterContactImpl)buckets.get(k);
								
								byte[]	bucket_id = contact.getID();
								
								if (((bucket_id[depth/8]>>(7-(depth%8)))&0x01 ) == 0 ){
									
									right_buckets.add( contact );
									
								}else{
									
									left_buckets.add( contact );
								}
							}
				
							boolean	right_contains_rid = false;
							boolean left_contains_rid = false;
							
							if ( contains_router_node_id ){
								
								right_contains_rid = 
										((router_node_id[depth/8]>>(7-(depth%8)))&0x01 ) == 0;
							
								left_contains_rid	= !right_contains_rid;
							}
							
							DHTRouterNodeImpl	new_left 	= new DHTRouterNodeImpl( this, depth+1, left_contains_rid, left_buckets );
							DHTRouterNodeImpl	new_right 	= new DHTRouterNodeImpl( this, depth+1, right_contains_rid, right_buckets );
							
							current_node.split( new_left, new_right );
							
							if ( contains_router_node_id ){
							
									// we've created a new smallest subtree
									// TODO: tidy up old smallest subtree - remember to factor in B...
								
								smallest_subtree = current_node;
							}
							
								// not complete, retry addition 
							
						}else{
								
							current_node.addReplacement( new DHTRouterContactImpl( node_id, attachment ));
							
							return( null );
						}
					}else{
		
						DHTRouterContactImpl new_contact = new DHTRouterContactImpl( node_id, attachment );
							
						buckets.add( new_contact );	// complete - added to bucket
						
						return( new_contact );
					}						
				}else{
						
					current_node = next_node;
				
					j--;				
				}
			}
		}
		
		Debug.out( "DHTRouter inconsistency" );
		
		return( null );
	}
	
	public synchronized List
	findClosestContacts(
		byte[]	node_id )
	{
			// find the K closest nodes - consider all buckets, not just the closest

		DHTRouterContactImpl[]	res = new DHTRouterContactImpl[K];
				
		int res_pos = findClosestContacts( node_id, 0, root, res, 0 );
			
		List	res_l = new ArrayList( res_pos );
		
		for (int i=0;i<res_pos;i++){
			
			res_l.add( res[i] );
		}
		
		return( res_l );
	}
	
	public synchronized DHTRouterContact
	contactAlive(
		byte[]	node_id,
		Object	attachment )
	{
		return( addContact( node_id, attachment, true ));
	}

	public synchronized DHTRouterContact
	contactDead(
		byte[]	node_id,
		Object	attachment )
	{
		Object[]	res = findContactSupport( node_id );
		
		if ( res == null ){
			
			return( null );
		}
		
		DHTRouterNodeImpl		node	= (DHTRouterNodeImpl)res[0];
		DHTRouterContactImpl	contact = (DHTRouterContactImpl)res[1];
		
		node.dead( contact );
		
		return( contact );
	}
	
	
	
	
	
	
	protected int
	findClosestContacts(
		byte[]					node_id,
		int						depth,
		DHTRouterNodeImpl		current_node,
		DHTRouterContactImpl[]	res,
		int						res_pos )
	{
		List	buckets = current_node.getBuckets();
		
		if ( buckets != null ){
			
			for (int i=0;i<buckets.size();i++){
				
				if ( res_pos == res.length ){
					
					break;
				}
				
				res[res_pos++] = (DHTRouterContactImpl)buckets.get(i);
			}
			
			return( res_pos );
		}
		
		boolean bit = ((node_id[depth/8]>>(7-(depth%8)))&0x01 ) == 1;
				
		DHTRouterNodeImpl	best_node;
		DHTRouterNodeImpl	worse_node;
				
		if ( bit ){
					
			best_node = current_node.getLeft();
			
			worse_node = current_node.getRight();
		}else{
					
			best_node = current_node.getRight();
			
			worse_node = current_node.getLeft();
		}

		res_pos = findClosestContacts( node_id, depth+1, best_node, res, res_pos );
		
		if ( res_pos < res.length ){
			
			res_pos = findClosestContacts( node_id, depth+1, worse_node, res, res_pos );
		}
		
		return( res_pos );
	}
	
	public synchronized DHTRouterContact
	findContact(
		byte[]		node_id )
	{
		Object[]	res = findContactSupport( node_id );
		
		if ( res == null ){
			
			return( null );
		}
		
		return((DHTRouterContact)res[1]);
	}
	
	protected synchronized Object[]
	findContactSupport(
		byte[]		node_id )
	{
		DHTRouterNodeImpl	current_node	= root;
		
		for (int i=0;i<node_id.length;i++){
			
			if ( current_node.getBuckets() != null ){
			
				break;
			}

			byte	b = node_id[i];
			
			int	j = 7;
			
			while( j >= 0 ){
					
				boolean	bit = ((b>>j)&0x01)==1?true:false;
				
				if ( current_node.getBuckets() != null ){
					
					break;
				}
								
				if ( bit ){
					
					current_node = current_node.getLeft();
					
				}else{
					
					current_node = current_node.getRight();
				}
				
				j--;
			}
		}
		
		List	buckets = current_node.getBuckets();
		
		for (int k=0;k<buckets.size();k++){
			
			DHTRouterContactImpl	contact = (DHTRouterContactImpl)buckets.get(k);
			
			if ( Arrays.equals(node_id, contact.getID())){

				return( new Object[]{ current_node, contact });
			}
		}
		
		return( null );
	}
	
	public long
	getNodeCount()
	{
		DHTRouterNodeImpl	current_node	= root;
		
		return( getNodeCount( current_node ));
	}
	
	protected long
	getNodeCount(
		DHTRouterNodeImpl	node )
	{
		if ( node.getBuckets() != null ){
			
			return( 1 );
			
		}else{
			
			return( 1 + getNodeCount( node.getLeft())) + getNodeCount( node.getRight());
		}
	}
	
	public long
	getContactCount()
	{
		DHTRouterNodeImpl	current_node	= root;
		
		return( getContactCount( current_node ));
	}
	
	protected long
	getContactCount(
		DHTRouterNodeImpl	node )
	{
		if ( node.getBuckets() != null ){
			
			return( node.getBuckets().size());
			
		}else{
			
			return( getContactCount( node.getLeft())) + getContactCount( node.getRight());
		}
	}
	
	protected void
	requestPing(
		DHTRouterContactImpl	contact )
	{
		System.out.println( "DHTRouter: requestPing:" + ByteFormatter.nicePrint( contact.getID(), true ));
	}
	
	public void
	print()
	{
		System.out.println( "DHT: node count = " + getNodeCount()+ ", contacts =" + getContactCount());
		
		root.print( "", "" );
	}
}
