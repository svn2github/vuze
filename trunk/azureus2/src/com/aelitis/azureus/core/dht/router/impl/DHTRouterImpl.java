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

import com.aelitis.azureus.core.dht.router.*;

/**
 * @author parg
 *
 */

public class 
DHTRouterImpl
	implements DHTRouter
{
	private int		K = 2;
	private int		B = 5;
	
	
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
	}
	
	public void
	setNodeID(
		byte[]	_my_node_id )
	{
		router_node_id	= _my_node_id;
		
		List	buckets = new ArrayList();
		
		buckets.add( new DHTRouterContactImpl( router_node_id ));
		
		root	= new DHTRouterNodeImpl( 0, true, buckets );
		
		smallest_subtree	= root;
	}
	
	public synchronized void
	addContact(
		byte[]	node_id )
	{
		// TODO: check already exists
		
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
					
					if ( buckets.size() == K ){
						
							// split if either
							// 1) this list contains router_node_id or
							// 2) depth % B is not 0
							// 3) this is part of the smallest subtree
						
						boolean	contains_router_node_id = current_node.containsRouterNodeID();
						int		depth					= current_node.getDepth();
						
						if ( 	contains_router_node_id ||
								depth % B != 0 ||
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
								// TODO: worthwhile defending the max size of this subtree?
							
							
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
							
							DHTRouterNodeImpl	new_left 	= new DHTRouterNodeImpl( depth+1, left_contains_rid, left_buckets );
							DHTRouterNodeImpl	new_right 	= new DHTRouterNodeImpl( depth+1, right_contains_rid, right_buckets );
							
							current_node.split( new_left, new_right );
							
							if ( contains_router_node_id ){
							
									// we've created a new smallest subtree
									// TODO: tidy up old smallest subtree - remember to factor in B...
								
								smallest_subtree = current_node;
							}
							
							// not complete, retry addition 
							
						}else{
							
							return;
						}
					}else{
						
						buckets.add( new DHTRouterContactImpl( node_id ));	// complete - added to bucket
						
						return;
					}						
				}else{
						
					current_node = next_node;
				
					j--;				
				}
			}
		}
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
	
	public DHTRouterContact
	findContact()
	{
		return( null );
	}
	
	public Object
	findValue()
	{
		return( null );
	}
	
	public void
	print()
	{
		root.print( "", "" );
	}
}
