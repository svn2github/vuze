/*
 * Created on 03-Feb-2005
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

package com.aelitis.azureus.core.dht.db.impl;

import java.util.*;

import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.transport.DHTTransportContact;

/**
 * @author parg
 *
 */

public class 
DHTDBMapping 
{
	private HashWrapper		key;
	
		// maps are access order, most recently used at tail, so we cycle values
		
	private Map				direct_originator_map			= new LinkedHashMap(16, 0.75f, true );
	private Map				indirect_originator_value_map	= new LinkedHashMap(16, 0.75f, true );
	
	protected
	DHTDBMapping(
		HashWrapper	_key )
	{
		key	= _key;
	}
	
	protected HashWrapper
	getKey()
	{
		return( key );
	}

	protected void
	updateLocalContact(
		DHTTransportContact		contact )
	{
			// pull out all the local values, reset the originator and then
			// re-add them
		
		List	changed = new ArrayList();
		
		Iterator	it = direct_originator_map.values().iterator();
		
		while( it.hasNext()){
		
			DHTDBValueImpl	value = (DHTDBValueImpl)it.next();
			
			if ( value.getCacheDistance() == 0 ){
			
				value.setOriginator( contact );
				
				changed.add( value );
				
				it.remove();
			}
		}
		
		for (int i=0;i<changed.size();i++){
			
			add((DHTDBValueImpl)changed.get(i));
		}
	}
	
	// All values have
	//	1) a key
	//	2) a value
	//	3) an originator (the contact who originally published it)
	//	4) a sender  (the contact who sent it, could be diff for caches)

	// rethink time :P
	// a) for a value where sender + originator are the same we store a single value
	// b) where sender + originator differ we store an entry per originator/value pair as the 
	//    send can legitimately forward multiple values but their originator should differ
	
	// c) the code that adds values is responsible for not accepting values that are either
	//    to "far away" from our ID, or that are cache-forwards from a contact "too far"
	//    away.

	
	// for a given key
	//		c) we only allow up to 8 entries per sending IP address (excluding port)
	//		d) if multiple entries have the same value the value is only returned once
	// 		e) only the originator can delete an entry

	// a) prevents a single sender from filling up the mapping with garbage
	// b) prevents the same key->value mapping being held multiple times when sent by different caches
	// c) prevents multiple senders from same IP filling up, but supports multiple machines behind NAT
	// d) optimises responses.
	
	// Note that we can't trust the originator value in cache forwards, we therefore
	// need to prevent someone from overwriting a valid originator->value1 mapping
	// with an invalid originator->value2 mapping - that is we can't use uniqueness of
	// originator

	// a value can be "volatile" - this means that the cacher can ping the originator
	// periodically and delete the value if it is dead


	// the aim here is to
	//	1) 	reduce ability for single contacts to spam the key while supporting up to 8 
	//		contacts on a given IP (assuming NAT is being used)
	//	2)	stop one contact deleting or overwriting another contact's entry
	//	3)	support garbage collection for contacts that don't delete entries on exit

	// TODO: we should enforce a max-values-per-sender restriction to stop a sender from spamming
	// lots of keys - however, for a small DHT we need to be careful
	
	protected void
	add(
		DHTDBValueImpl		new_value )
	{
		// don't replace a closer cache value with a further away one. in particular
		// we have to avoid the case where the original publisher of a key happens to
		// be close to it and be asked by another node to cache it!

		DHTTransportContact	originator 		= new_value.getOriginator();
		DHTTransportContact	sender 			= new_value.getSender();

		HashWrapper	originator_id = new HashWrapper( originator.getID());
		
		HashWrapper	originator_value_id = getOriginatorValueID( new_value );

		boolean	direct = Arrays.equals( originator.getID(), sender.getID());
		
		if ( direct ){
			
				// direct contact from the originator is straight forward
			
			direct_originator_map.put( originator_id, new_value );
			
				// remove any indirect value we might already have for this
			
			indirect_originator_value_map.remove( originator_value_id );
			
		}else{
			
				// not direct. if we have a value already for this originator then
				// we drop the value as the originator originated one takes precedence
			
			if ( direct_originator_map.get( originator_id ) != null ){
				
				return;
			}
						
				// rule (b) - one entry per originator/value pair
								
			DHTDBValueImpl existing_value = (DHTDBValueImpl)indirect_originator_value_map.get( originator_value_id );
			
			if ( existing_value != null ){
				
				if ( existing_value.getCacheDistance() <= new_value.getCacheDistance() + 1 ){
		
						// update value with latest from this sender (could be 0 length implying
						// deletion)
					
					if ( new_value.getCreationTime() > existing_value.getCreationTime()){
					
						indirect_originator_value_map.put( originator_value_id, new_value );
						
					}else{
					
							// mark it as current 
				
						existing_value.reset();
					}
				
					//System.out.println( "    updating existing (sender same)" );
					
				}else{
					
						// overwrite further away entry for this sender
					
					indirect_originator_value_map.put( originator_value_id, new_value );
					
					//System.out.println( "    replacing existing" );
				}				
			}else{
			
				indirect_originator_value_map.put( originator_value_id, new_value );
			}	
		}
	}

	protected HashWrapper
	getOriginatorValueID(
		DHTDBValueImpl	value )
	{
		DHTTransportContact	originator	= value.getOriginator();
		
		byte[]	originator_id	= originator.getID();
		byte[]	value_bytes 	= value.getValue();

		byte[]	x = new byte[originator_id.length + value_bytes.length];
		
		System.arraycopy( originator_id, 0, x, 0, originator_id.length );
		System.arraycopy( value_bytes, 0, x, originator_id.length, value_bytes.length );
		
		HashWrapper	originator_value_id = new HashWrapper( x );
		
		return( originator_value_id );
	}
	
	
	protected DHTDBValueImpl[]
	get(
		int			max,
		boolean		remove_secondary_caches )
	{
		List	res 		= new ArrayList();
		
		Set		duplicate_check = new HashSet();
		
		Map[]	maps = new Map[]{ direct_originator_map, indirect_originator_value_map };
		
		for (int i=0;i<maps.length;i++){
			
			List	keys_used 	= new ArrayList();

			Map			map	= maps[i];
			
			Iterator	it = map.entrySet().iterator();
		
			while( it.hasNext() && ( max==0 || res.size()< max )){
			
				Map.Entry	entry = (Map.Entry)it.next();
				
				HashWrapper		entry_key		= (HashWrapper)entry.getKey();
				
				DHTDBValueImpl	entry_value = (DHTDBValueImpl)entry.getValue();
						
				HashWrapper	x = new HashWrapper( entry_value.getValue());
				
				if ( duplicate_check.contains( x )){
					
					continue;
				}
				
				duplicate_check.add( x );
				
				// TODO: think more on this - secondary caching is open to exploitation for DOS as a single
				// contact could spam all contacts surrounding the target with bogus information 
				// current approach is to only allow usage of a secondary cache entry ONCE before
				// we delete it :P
			
				if ( remove_secondary_caches && entry_value.getCacheDistance() > 1 ){
					
					it.remove();
				}
				
					// zero length values imply deleted values so don't return them
				
				if ( entry_value.getValue().length > 0 ){
					
					res.add( entry_value );
				
					keys_used.add( entry_key );
				}
			}
			
				// now update the access order so values get cycled
			
			for (int j=0;j<keys_used.size();j++){
				
				map.get( keys_used.get(j));
			}
		}
		
		DHTDBValueImpl[]	v = new DHTDBValueImpl[res.size()];
		
		res.toArray( v );
		
		return( v );
	}
	
	protected DHTDBValueImpl
	remove(
		DHTTransportContact 	originator )
	{
			// local remove
		
		HashWrapper originator_id = new HashWrapper( originator.getID());
		
		DHTDBValueImpl	res = (DHTDBValueImpl)direct_originator_map.remove( originator_id );
		
		return( res );
	}
		
	
	protected int
	getSize()
	{
		return( direct_originator_map.size() + indirect_originator_value_map.size());
	}
	
	protected Iterator
	getValues()
	{
		return( new valueIterator());
	}
	
	protected class
	valueIterator
		implements Iterator
	{
		Map[]	maps 		= new Map[]{ direct_originator_map, indirect_originator_value_map };
		int		map_index 	= 0;
		
		Iterator	it;
		
		public boolean
		hasNext()
		{
			if ( it != null && it.hasNext()){
				
				return( true );
			}
			
			if ( map_index < maps.length ){
				
				it = maps[map_index++].values().iterator();
				
				return( it.hasNext());
			}
			
			return( false );
		}
		
		public Object
		next()
		{
			if ( hasNext()){
			
				return( it.next());
			}
			
			throw( new NoSuchElementException());
		}
		
		public void
		remove()
		{
			if ( it == null ){
							
				throw( new IllegalStateException());
			}	
			
			it.remove();
		}
	}
}
