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

import com.aelitis.azureus.core.dht.router.DHTRouterNode;

/**
 * @author parg
 *
 */

public class 
DHTRouterNodeImpl
	implements DHTRouterNode
{
	private int		depth;
	private boolean	contains_router_node_id;
	
	private List	buckets;
	
	private DHTRouterNodeImpl	left;
	private DHTRouterNodeImpl	right;
	
	protected
	DHTRouterNodeImpl(
		int			_depth,
		boolean		_contains_router_node_id,
		List		_buckets )
	{
		depth					= _depth;
		contains_router_node_id	= _contains_router_node_id;
		buckets					= _buckets;
	}
	
	protected int
	getDepth()
	{
		return( depth );
	}
	
	protected boolean
	containsRouterNodeID()
	{
		return( contains_router_node_id );
	}
	
	protected DHTRouterNodeImpl
	getLeft()
	{
		return( left );
	}
	
	protected DHTRouterNodeImpl
	getRight()
	{
		return( right );
	}
	
	protected void
	split(
		DHTRouterNodeImpl	new_left,
		DHTRouterNodeImpl	new_right )
	{
		buckets	= null;
		
		left	= new_left;
		right	= new_right;
	}
		
	protected List
	getBuckets()
	{
		return( buckets );
	}
	
	public void
	ping()
	{
		
	}
	
	public void
	store()
	{
		
	}
	
	public void
	print(
		String	indent,
		String	prefix )
	{
		if ( left == null ){
			
			System.out.println( indent + prefix + ": buckets = " + buckets.size() + bucketsToString() + (contains_router_node_id?" *":""));
			
		}else{
			
			System.out.println( indent + prefix + ":" + (contains_router_node_id?" *":""));
			
			left.print( indent + "  ", prefix + "1"  );
						
			right.print( indent + "  ", prefix + "0" );
		}
	}
	
	protected String
	bucketsToString()
	{
		String	res = "{";
		
		for (int i=0;i<buckets.size();i++){
			
			res += ByteFormatter.nicePrint((byte[])buckets.get(i));
		}
		
		return( res + "}" );
	}
}
