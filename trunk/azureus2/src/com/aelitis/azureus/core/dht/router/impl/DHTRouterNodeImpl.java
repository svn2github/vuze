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
	
	protected DHTRouterContact
	addReplacement(
		DHTRouterContactImpl	replacement )
	{
		if ( MAX_REPLACEMENTS == 0 ){
			
			return( null );
		}
		
		if( replacements == null ){
			
			replacements = new ArrayList();
			
		}else{
				
			if ( replacements.size() == MAX_REPLACEMENTS ){
				
					// if this replacement is known to be alive, replace any existing
					// replacements that haven't been known to be alive
				
				if ( replacement.hasBeenAlive() ){
					
					for (int i=0;i<replacements.size();i++){
						
						DHTRouterContactImpl	r = (DHTRouterContactImpl)replacements.get(i);
				
						if ( !r.hasBeenAlive()){
							
							replacements.remove(i);
							
							break;
						}
					}
					
						// no unknown existing replacements but this is "newer" than the existing
						// ones so replace the oldest one
					
					if ( replacements.size() == MAX_REPLACEMENTS ){
						
						replacements.remove(0);
					}
				}else{
						
						// replace old unknown ones with newer unknown ones but don't remove
						// ones with a ping in process as this would be a waste
					
					for (int i=0;i<replacements.size();i++){
						
						DHTRouterContactImpl	r = (DHTRouterContactImpl)replacements.get(i);
				
						if ( !( r.hasBeenAlive() || r.isPingOutstanding())){
							
							replacements.remove(i);
							
							break;
						}
					}
				}
			}
		}
		
		if ( replacements.size() == MAX_REPLACEMENTS ){
			
				// no room, drop the contact
			
			return( null );
		}
		
		replacements.add( replacement );
		
			// we need to find a bucket contact to ping - if it fails then it might be replaced with
			// this one
		
		for (int i=0;i<buckets.size();i++){
			
			DHTRouterContactImpl	contact = (DHTRouterContactImpl)buckets.get(i);
			
				// never consider ourselves for eviction
			
			if ( router.isID( contact.getID())){
				
				continue;
			}
			
			if ( !contact.isPingOutstanding()){
				
				contact.setPingOutstanding( true );
				
				router.requestPing( contact );
				
				break;
			}
		}
		
		return( replacement );
	}
	
	protected DHTRouterContactImpl
	findNode(
		byte[]		node_id,
		boolean		known_to_be_alive )
	{
			for (int k=0;k<buckets.size();k++){
			
			DHTRouterContactImpl	contact = (DHTRouterContactImpl)buckets.get(k);
			
			if ( Arrays.equals(node_id, contact.getID())){

				if ( known_to_be_alive ){
					
					alive( contact );
				}

				return( contact );
			}
		}
		
			// check replacements as well
			
		if ( replacements != null ){
			
			for (int k=0;k<replacements.size();k++){
				
				DHTRouterContactImpl	contact = (DHTRouterContactImpl)replacements.get(k);
				
				if ( Arrays.equals(node_id, contact.getID())){
	
					if ( known_to_be_alive ){
						
						alive( contact );
					}
	
					return( contact );
				}
			}
		}
		
		return( null );
	}
	
	protected void
	alive(
		DHTRouterContactImpl	contact )
	{
		DHTLog.log( DHTLog.getString( contact.getID()) + ": alive" );
				
		if ( buckets.remove( contact )){
			
			contact.setPingOutstanding( false );
			
			contact.setAlive();
			
			buckets.add( contact );
			
		}else if ( replacements.remove( contact )){
			
			contact.setPingOutstanding( false );
			
			contact.setAlive();
			
			replacements.add( contact );		
		}
	}
	
	protected void
	dead(
		DHTRouterContactImpl	contact )
	{
		DHTLog.log( DHTLog.getString( contact.getID()) + ": dead" );
		
		if ( contact.setFailed()){
			
				// check the contact is still present
			
			if ( buckets.remove( contact )){
				
				if ( replacements.size() > 0 ){
					
						// take most recent alive one and add to buckets
					
					boolean	replaced	= false;
					
					for (int i=replacements.size()-1;i>=0;i--){
						
						DHTRouterContactImpl	rep = (DHTRouterContactImpl)replacements.get(i);
						
						if ( rep.hasBeenAlive()){
							
							replacements.remove( rep );
							
							buckets.add( rep );
							
							replaced	= true;
							
							break;
						}
					}
					
						// non alive - just take most recently added
					
					if ( !replaced ){
						
						Object	rep = replacements.remove( replacements.size() - 1 );
					
						buckets.add( rep );
					}
				}
			}else{
				
				replacements.remove( contact );
			}
		}
	}
	
	public void
	print(
		String	indent,
		String	prefix )
	{
		if ( left == null ){
			
			DHTLog.log( 
					indent + prefix + 
					": buckets = " + buckets.size() + contactsToString( buckets) + 
					", replacements = " + (replacements==null?"null":( replacements.size() + contactsToString( replacements ))) + 
					(contains_router_node_id?" *":""));
			
		}else{
			
			DHTLog.log( indent + prefix + ":" + (contains_router_node_id?" *":""));
			
			left.print( indent + "  ", prefix + "1"  );
						
			right.print( indent + "  ", prefix + "0" );
		}
	}
	
	protected String
	contactsToString(
		List	contacts )
	{
		String	res = "{";
		
		for (int i=0;i<contacts.size();i++){
			
			res += (i==0?"":", ") + ((DHTRouterContactImpl)contacts.get(i)).getString();
		}
		
		return( res + "}" );
	}
}
