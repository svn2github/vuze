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

import java.util.ArrayList;
import java.util.List;

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
	public static final int	K = 2;
	public static final int B = 5;
	
	
	private byte[]	router_node_id;
	
	private DHTRouterNodeImpl		root;
	private DHTRouterNodeImpl		smallest_subtree;
	
	public void
	setNodeID(
		byte[]	_my_node_id )
	{
		router_node_id	= _my_node_id;
		
		List	buckets = new ArrayList();
		
		buckets.add( router_node_id );
		
		root	= new DHTRouterNodeImpl( 0, true, buckets );
		
		smallest_subtree	= root;
	}
	
	public synchronized void
	addNode(
		byte[]	node_id )
	{
		// TODO: check already exists
		
		System.out.println( ByteFormatter.nicePrint( node_id ));
		
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
							
								// split!!!!
							
							List	left_buckets 	= new ArrayList();
							List	right_buckets 	= new ArrayList();
							
							for (int k=0;k<buckets.size();k++){
								
								byte[]	bucket_entry = (byte[])buckets.get(k);
								
								if (((bucket_entry[depth/8]>>(7-(depth%8)))&0x01 ) == 0 ){
									
									right_buckets.add( bucket_entry );
									
								}else{
									
									left_buckets.add( bucket_entry );
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
						
						buckets.add( node_id );	// complete - added to bucket
						
						return;
					}						
				}else{
						
					current_node = next_node;
				
					j--;				
				}
			}
		}
	}
	
	public DHTRouterNode
	findNode()
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
