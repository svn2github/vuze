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

import com.aelitis.azureus.core.dht.impl.DHTLog;
import com.aelitis.azureus.core.dht.router.DHTRouterContact;

/**
 * @author parg
 *
 */

public class 
DHTRouterNodeImpl
{
	private static final int	MAX_REPLACEMENTS	= 5;	// TODO:
	
	private DHTRouterImpl	router;
	private int				depth;
	private boolean			contains_router_node_id;
	
	private List	buckets;
	private List	replacements;
	
	private DHTRouterNodeImpl	left;
	private DHTRouterNodeImpl	right;
	
	protected
	DHTRouterNodeImpl(
		DHTRouterImpl	_router,
		int				_depth,
		boolean			_contains_router_node_id,
		List			_buckets )
	{
		router					= _router;
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
		
		if ( replacements != null ){
			
			Debug.out( "DHTRouterNode: inconsistenct - splitting a node with replacements" );
		}
		
		left	= new_left;
		right	= new_right;
	}
		
	protected List
	getBuckets()
	{
		return( buckets );
	}
	
	protected List
	getReplacements()
	{
		return( replacements );
	}
	
	protected void
	addReplacement(
		DHTRouterContactImpl	replacement )
	{
		if ( replacements == null ){
			
			replacements = new ArrayList();
			
		}else{
			
				// check its not already there
			
			for (int i=0;i<replacements.size();i++){
				
				DHTRouterContactImpl	r = (DHTRouterContactImpl)replacements.get(i);
				
				if ( Arrays.equals( replacement.getID(), r.getID())){
					
					return;
				}
			}
			
			if ( replacements.size() == MAX_REPLACEMENTS ){
			
				replacements.remove(0);
			}
		}
			
		replacements.add( replacement );
		
			// we need to find a bucket contact to ping - if it fails then it might be replaced with
			// this one
		
		for (int i=0;i<buckets.size();i++){
			
			DHTRouterContactImpl	contact = (DHTRouterContactImpl)buckets.get(i);
			
			if ( !contact.getPingOutstanding()){
				
				contact.setPingOutstanding( true );
				
				router.requestPing( contact );
				
				break;
			}
		}
	}
	
	protected void
	alive(
		DHTRouterContactImpl	contact )
	{
		DHTLog.log( DHTLog.getString( contact.getID()) + ": alive" );
		
			// only action this if still present
		
		if ( buckets.remove( contact )){
			
			contact.setPingOutstanding( false );
			
			contact.alive();
			
			buckets.add( contact );
		}
	}
	
	protected void
	dead(
		DHTRouterContactImpl	contact )
	{
		DHTLog.log( DHTLog.getString( contact.getID()) + ": dead" );
		
		if ( contact.failed()){
			
				// check the contact is still present
			
			if ( buckets.remove( contact )){
				
				if ( replacements.size() > 0 ){
					
						// take most recent and add to buckets
					
					Object	rep = replacements.remove( replacements.size() - 1 );
					
					buckets.add( rep );
				}
			}
		}
	}
	
	public void
	print(
		String	indent,
		String	prefix )
	{
		if ( left == null ){
			
			DHTLog.log( indent + prefix + ": buckets = " + buckets.size() + bucketsToString() + (contains_router_node_id?" *":""));
			
		}else{
			
			DHTLog.log( indent + prefix + ":" + (contains_router_node_id?" *":""));
			
			left.print( indent + "  ", prefix + "1"  );
						
			right.print( indent + "  ", prefix + "0" );
		}
	}
	
	protected String
	bucketsToString()
	{
		String	res = "{";
		
		for (int i=0;i<buckets.size();i++){
			
			res += (i==0?"":", ") + ByteFormatter.nicePrint(((DHTRouterContact)buckets.get(i)).getID(), true);
		}
		
		return( res + "}" );
	}
}
