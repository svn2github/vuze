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
	
		// sender map is access order, most recently used at tail, so we cycle values
	
	private Map				sender_map				= new LinkedHashMap(16, 0.75f, true );
	
	private Map				originator_value_map	= new HashMap();
	
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
		Iterator	it = sender_map.values().iterator();
		
		while( it.hasNext()){
		
			DHTDBValueImpl	value = (DHTDBValueImpl)it.next();
			
			if ( value.getCacheDistance() == 0 ){
			
				value.setOriginator( contact );
			}
		}
	}
	
	// All values have
	//	1) a key
	//	2) a value
	//	3) an originator (the contact who originally published it)
	//	4) a sender  (the contact who sent it, could be diff for caches)

	// for a given key
	//		a) we only hold one entry per sender (IP+port) (latest)
	//		b) we only hold one entry for a given originator+value pair
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
		
		try{
			HashWrapper	originator_value_id = getOriginatorValueID( new_value );
			
				// rule (b) - one entry per originator/value 
			
			DHTDBValueImpl existing_value = (DHTDBValueImpl)originator_value_map.get( originator_value_id );
			
			if ( existing_value != null ){
				
				existing_value.reset();
				
				//System.out.println( "    updating existing (originator/value same)" );
				
				return;
			}
			
				// rule (a) - one entry per sender
			
			DHTTransportContact	sender 		= new_value.getSender();
			
			HashWrapper		sender_id = new HashWrapper( sender.getID());
			
			existing_value = (DHTDBValueImpl)sender_map.get( sender_id );
			
			if ( existing_value != null ){
				
				if ( existing_value.getCacheDistance() <= new_value.getCacheDistance() + 1 ){
		
						// update value with latest from this sender (could be 0 length implying
						// deletion)
					
					if ( new_value.getCreationTime() > existing_value.getCreationTime()){
					
						existing_value.setValue( new_value.getValue());
					}
					
						// mark it as current 
				
					existing_value.reset();
				
					//System.out.println( "    updating existing (sender same)" );
					
				}else{
					
						// overwrite further away entry for this sender
					
					addValue(  originator_value_id, sender_id, new_value );
					
					//System.out.println( "    replacing existing" );
				}
				
				return;
			}
			
			addValue(  originator_value_id, sender_id, new_value );
				
		}finally{
			
			/*
			String	str = DHTLog.getString2(key.getHash());
			
			System.out.println( "Mapping for '" + str + "' has " + sender_map.size() + " entries" );

			if ( !str.startsWith("7")){
				
				System.out.println( "    Details: orig = " + new_value.getOriginator().getString() + ", sender = " + new_value.getSender().getString());
				
			}
			*/
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
	
	protected DHTDBValueImpl
	get(
		HashWrapper	value_key  )
	{
		return((DHTDBValueImpl)sender_map.get( value_key ));
	}
	
	protected DHTDBValueImpl
	get(
		DHTTransportContact	contact )
	{
		return((DHTDBValueImpl)sender_map.get( new HashWrapper( contact.getID())));
	}
	
	protected DHTDBValueImpl[]
	get(
		int			max,
		boolean		remove_secondary_caches )
	{
		List	res 		= new ArrayList();
		List	keys_used 	= new ArrayList();
		
		Set		duplicate_check = new HashSet();
		
		Iterator	it = sender_map.entrySet().iterator();
		
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
				
				removeValue( it, null, entry_value );
			}
			
				// zero length values imply deleted values so don't return them
			
			if ( entry_value.getValue().length > 0 ){
				
				res.add( entry_value );
			
				keys_used.add( entry_key );
			}
		}
		
			// now update the access order so values get cycled
		
		for (int i=0;i<keys_used.size();i++){
			
			sender_map.get( keys_used.get(i));
		}
		
		DHTDBValueImpl[]	v = new DHTDBValueImpl[res.size()];
		
		res.toArray( v );
		
		return( v );
	}
	
	protected DHTDBValueImpl
	remove(
		DHTTransportContact 	sender )
	{
		HashWrapper	sender_id = new HashWrapper( sender.getID());
		
		DHTDBValueImpl	res = (DHTDBValueImpl)sender_map.get( sender_id );
		
		if ( res != null ){
			
			removeValue( null, sender_id, res );
		}
		
		return( res );
	}
	
	
	protected void
	addValue(
		HashWrapper		originator_value_id,
		HashWrapper		sender_id,
		DHTDBValueImpl	value )
	{
		DHTDBValueImpl	old_value = (DHTDBValueImpl)sender_map.put( sender_id, value );
		
			// if we've removed an existing value for this sender then we've got to also
			// remove the associated originator mapping
		
		if ( old_value != null ){
			
			originator_value_map.remove( getOriginatorValueID( old_value ));
		}
		
		originator_value_map.put( originator_value_id, value );
	}
	
	protected void
	removeValue(
		Iterator			sender_map_iterator,
		HashWrapper			sender_id,
		DHTDBValueImpl		value )
	{
		if ( sender_map_iterator == null ){
			
			if ( sender_map.remove( sender_id ) == null ){
				
				System.out.println( "sender value entry missing" );
			}
			
		}else{
			
			sender_map_iterator.remove();
		}
		
		if ( originator_value_map.remove( getOriginatorValueID( value )) == null ){
			
			System.out.println( "originator value entry missing" );
		}
	}
	
	
	protected int
	getSize()
	{
		return( originator_value_map.size());
	}
	
	protected Iterator
	getValues()
	{
		return( sender_map.values()).iterator();
	}
}
