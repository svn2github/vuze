/*
 * Created on 28-Jan-2005
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

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.Timer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.plugins.logging.LoggerChannel;


import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.DHTStorageAdapter;
import com.aelitis.azureus.core.dht.db.*;
import com.aelitis.azureus.core.dht.impl.DHTLog;
import com.aelitis.azureus.core.dht.router.DHTRouter;
import com.aelitis.azureus.core.dht.control.DHTControl;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

/**
 * @author parg
 *
 */

public class 
DHTDBImpl
	implements DHTDB
{
	private static final int	MAX_ENTRIES_PER_MAPPING	= 1;
	
	private int			original_republish_interval;
	
		// the grace period gives the originator time to republish their data as this could involve
		// some work on their behalf to find closest nodes etc. There's no real urgency here anyway
	
	private int			ORIGINAL_REPUBLISH_INTERVAL_GRACE	= 30*60*1000;
	
	private int			cache_republish_interval;
	
	private long		MIN_CACHE_EXPIRY_CHECK_INTERVAL		= 60000;
	private long		last_cache_expiry_check;

	private long		max_values_stored;
	
	private Map			stored_values = new HashMap();
	
	private DHTControl				control;
	private DHTStorageAdapter		adapter;
	private DHTRouter				router;
	private DHTTransportContact		local_contact;
	private LoggerChannel			logger;
	
	private AEMonitor	this_mon	= new AEMonitor( "DHTDB" );

	public
	DHTDBImpl(
		DHTStorageAdapter	_adapter,
		int					_original_republish_interval,
		int					_cache_republish_interval,
		int					_max_values_stored,
		LoggerChannel		_logger )
	{
		adapter							= _adapter;
		original_republish_interval		= _original_republish_interval;
		cache_republish_interval		= _cache_republish_interval;
		max_values_stored				= _max_values_stored;
		logger							= _logger;
		
		Timer	timer = new Timer("DHT refresher");
		
		timer.addPeriodicEvent(
			original_republish_interval,
			new TimerEventPerformer()
			{
				public void
				perform(
					TimerEvent	event )
				{
					logger.log( "Republish of original mappings starts" );
					
					long	start 	= SystemTime.getCurrentTime();
					
					int	stats = republishOriginalMappings();
					
					long	end 	= SystemTime.getCurrentTime();

					logger.log( "Republish of original mappings completed in " + (end-start) + ": " +
								"values = " + stats );

				}
			});
					
				// random skew here so that cache refresh isn't very synchronised, as the optimisations
				// regarding non-republising benefit from this 
			
		timer.addPeriodicEvent(
				cache_republish_interval + 10000 - (int)(Math.random()*20000),
				new TimerEventPerformer()
				{
					public void
					perform(
						TimerEvent	event )
					{
						logger.log( "Republish of cached mappings starts" );
						
						long	start 	= SystemTime.getCurrentTime();
						
						int[]	stats = republishCachedMappings();		
						
						long	end 	= SystemTime.getCurrentTime();

						logger.log( "Republish of cached mappings completed in " + (end-start) + ": " +
									"values = " + stats[0] + ", keys = " + stats[1] + ", ops = " + stats[2]);

					}
				});
	}
	
	
	public void
	setControl(
		DHTControl		_control )
	{
		control			= _control;
		
		router			= control.getRouter();
		local_contact	= control.getTransport().getLocalContact(); 
	
			// our ID has changed - amend the originator of all our values
		
		try{
			this_mon.enter();
			
			Iterator	it = stored_values.values().iterator();
			
			while( it.hasNext()){
				
				DHTDBMapping	mapping = (DHTDBMapping)it.next();
				
				mapping.updateLocalContact( local_contact );
			}
		}finally{
			
			this_mon.exit();
		}
	}
	
	public DHTDBValue
	store(
		HashWrapper		key,
		byte[]			value,
		byte			flags )
	{
			// local store
		
		try{
			this_mon.enter();
			
			DHTDBValueImpl res =	
				new DHTDBValueImpl( 
						SystemTime.getCurrentTime(), 
						value, 
						local_contact, 
						local_contact,
						0,
						flags );
	
	
				// don't police max check for locally stored data
				// only that received
			
			DHTDBMapping	mapping = (DHTDBMapping)stored_values.get( key );
			
			if ( mapping == null ){
				
				mapping = new DHTDBMapping( adapter, key );
				
				stored_values.put( key, mapping );
			}
			
			mapping.add( res );
			
			return( res );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public byte
	store(
		DHTTransportContact 	sender, 
		HashWrapper				key,
		DHTTransportValue[]		values )
	{
			// remote store for cache values
		
			// Make sure that we only accept values for storing that are reasonable.
			// Assumption is that the caller has made a reasonable effort to ascertain
			// the correct place to store a value. Part of this will in general have 
			// needed them to query us for example. Therefore, limit values to those
			// that are at least as close to us
		
		List closest_contacts = control.getClosestKContactsList( key.getHash(), true );
		
		boolean	store_it	= false;
		
		for (int i=0;i<closest_contacts.size();i++){
			
			if ( router.isID(((DHTTransportContact)closest_contacts.get(i)).getID())){
				
				store_it	= true;
				
				break;
			}		
		}
		
		if ( !store_it ){
			
			DHTLog.log( "Not storing " + DHTLog.getString2(key.getHash()) + " as key too far away" );

			return( DHT.DT_NONE );
		}
		
			// next, for cache forwards (rather then values coming directly from 
			// originators) we ensure that the contact sending the values to us is
			// close enough.
		
		boolean	cache_forward = false;
		
		for (int i=0;i<values.length;i++){
			
			if (!Arrays.equals( sender.getID(), values[i].getOriginator().getID())){
				
				cache_forward	= true;
				
				break;
			}
		}
		
		
		if ( cache_forward ){
			
				// get the closest contacts to me
				
			byte[]	my_id	= local_contact.getID();
			
			closest_contacts = control.getClosestKContactsList( my_id, true );
			
			DHTTransportContact	furthest = (DHTTransportContact)closest_contacts.get( closest_contacts.size()-1);
			
			byte[]	furthest_ok_distance 	= control.computeDistance( furthest.getID(), my_id );
			byte[]	sender_distance			= control.computeDistance( sender.getID(), my_id );
			
			if ( control.compareDistances( furthest_ok_distance, sender_distance) < 0 ){

				store_it	= false;
			}
		}
		
		if ( !store_it ){
			
			DHTLog.log( "Not storing " + DHTLog.getString2(key.getHash()) + " as cache forward and sender too far away" );
			
			return( DHT.DT_NONE );
		}
		
		try{
			this_mon.enter();
						
			if ( getSize() >= max_values_stored ){
				
					// just drop it
				
				logger.log( "Max entries exceeded" );
				
				return( DHT.DT_NONE );
				
			}else{
				
				checkCacheExpiration( false );
				
				DHTDBMapping	mapping = (DHTDBMapping)stored_values.get( key );
				
				if ( mapping == null ){
					
					mapping = new DHTDBMapping( adapter, key );
					
					stored_values.put( key, mapping );
				}
				
					// we carry on an update as its ok to replace existing entries
					// even if diversified
				
				for (int i=0;i<values.length;i++){
					
					DHTDBValueImpl mapping_value	= new DHTDBValueImpl( sender, values[i], 1 );
					
					mapping.add( mapping_value );
				}
				
				return( mapping.getDiversificationType());
			}
		}finally{
			
			this_mon.exit();
		}
	}
	
	public DHTDBLookupResult
	get(
		DHTTransportContact		reader,
		HashWrapper				key,
		int						max_values,	// 0 -> all
		boolean					external_request )	
	{
		try{
			this_mon.enter();
			
			checkCacheExpiration( false );
					
			final DHTDBMapping mapping = (DHTDBMapping)stored_values.get(key);
			
			if ( mapping == null ){
				
				return( null );
			}
			
			if ( external_request ){
				
				mapping.addHit();
			}
			
			final DHTDBValue[]	values = mapping.get( reader, max_values, true );
						
			return(
				new DHTDBLookupResult()
				{
					public DHTDBValue[]
					getValues()
					{
						return( values );
					}
					
					public byte
					getDiversificationType()
					{
						return( mapping.getDiversificationType());
					}
				});
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public DHTDBValue
	get(
		HashWrapper				key )
	{
			// local remove
		
		try{
			this_mon.enter();
		
			DHTDBMapping mapping = (DHTDBMapping)stored_values.get( key );
			
			if ( mapping != null ){
				
				return( mapping.get( local_contact ));
			}
			
			return( null );
			
		}finally{
			
			this_mon.exit();
		}
	}
	public DHTDBValue
	remove(
		DHTTransportContact 	originator,
		HashWrapper				key )
	{
			// local remove
		
		try{
			this_mon.enter();
		
			DHTDBMapping mapping = (DHTDBMapping)stored_values.get( key );
			
			if ( mapping != null ){
				
				return( mapping.remove( originator ));
			}
			
			return( null );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public boolean
	isEmpty()
	{
		return( getSize() == 0 );
	}
	
	public long
	getSize()
	{
		try{
			this_mon.enter();
			
			int	res = 0;
			
			Iterator	it = stored_values.values().iterator();
			
			while( it.hasNext()){
				
				DHTDBMapping	mapping = (DHTDBMapping)it.next();
				
				res += mapping.getSize();
			}
			
			return( res );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public Iterator
	getKeys()
	{
		try{
			this_mon.enter();
			
			return( new ArrayList( stored_values.keySet()).iterator());
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected int
	republishOriginalMappings()
	{
		int	values_published	= 0;

		Map	republish = new HashMap();
		
		try{
			this_mon.enter();
			
			Iterator	it = stored_values.entrySet().iterator();
			
			while( it.hasNext()){
				
				Map.Entry	entry = (Map.Entry)it.next();
				
				HashWrapper		key		= (HashWrapper)entry.getKey();
				
				DHTDBMapping	mapping	= (DHTDBMapping)entry.getValue();
				
				Iterator	it2 = mapping.getValues();
				
				List	values = new ArrayList();
				
				while( it2.hasNext()){
					
					DHTDBValueImpl	value = (DHTDBValueImpl)it2.next();
				
					if ( value != null && value.getCacheDistance() == 0 ){
						
						// we're republising the data, reset the creation time
						
						value.setCreationTime();

						values.add( value );
					}
				}
				
				if ( values.size() > 0 ){
					
					republish.put( key, values );
					
				}
			}
		}finally{
			
			this_mon.exit();
		}
		
		Iterator	it = republish.entrySet().iterator();
		
		while( it.hasNext()){
			
			Map.Entry	entry = (Map.Entry)it.next();
			
			HashWrapper			key		= (HashWrapper)entry.getKey();
			
			List		values	= (List)entry.getValue();
			
				// no point in worry about multi-value puts here as it is extremely unlikely that
				// > 1 value will locally stored, or > 1 value will go to the same contact
			
			for (int i=0;i<values.size();i++){
				
				values_published++;
				
				control.put( key.getHash(), (DHTDBValueImpl)values.get(i), 0 );
			}
		}
		
		return( values_published );
	}
	
	protected int[]
	republishCachedMappings()
	{		
		int	values_published	= 0;
		int keys_published		= 0;
		int	republish_ops		= 0;
		
			// first refresh any leaves that have not performed at least one lookup in the
			// last period
		
		router.refreshIdleLeaves( cache_republish_interval );
		
		Map	republish = new HashMap();
		
		long	now = System.currentTimeMillis();
		
		try{
			this_mon.enter();
			
			checkCacheExpiration( true );

			Iterator	it = stored_values.entrySet().iterator();
			
			while( it.hasNext()){
				
				Map.Entry	entry = (Map.Entry)it.next();
				
				HashWrapper			key		= (HashWrapper)entry.getKey();
				
				DHTDBMapping		mapping	= (DHTDBMapping)entry.getValue();
				
				Iterator	it2 = mapping.getValues();
				
				List	values = new ArrayList();
				
				while( it2.hasNext()){
					
					DHTDBValueImpl	value = (DHTDBValueImpl)it2.next();
				
					if ( value.getCacheDistance() == 1 ){
						
							// if this value was stored < period ago then we assume that it was
							// also stored to the other k-1 locations at the same time and therefore
							// we don't need to re-store it
						
						if ( now < value.getStoreTime()){
							
								// deal with clock changes
							
							value.setStoreTime( now );
							
						}else if ( now - value.getStoreTime() <= cache_republish_interval ){
							
							// System.out.println( "skipping store" );
							
						}else{
								
							values.add( value );
						}
					}
				}

				if ( values.size() > 0 ){
					
					republish.put( key, values );
					
				}
			}
		}finally{
			
			this_mon.exit();
		}
		
		if ( republish.size() > 0 ){
			
			// System.out.println( "cache replublish" );
			
				// not sure I really understand this re-publish optimisation, however the approach
				// is to refresh all leaves in the smallest subtree, thus populating the tree with
				// sufficient information to directly know which nodes to republish the values
				// to.
			
				// However, I'm going to rely on the "refresh idle leaves" logic above
				// (that's required to keep the DHT alive in general) to ensure that all
				// k-buckets are reasonably up-to-date
					
			Iterator	it = republish.entrySet().iterator();
			
			List	stop_caching = new ArrayList();
			
				// build a map of contact -> list of keys to republish
			
			Map	contact_map	= new HashMap();
			
			while( it.hasNext()){
				
				Map.Entry	entry = (Map.Entry)it.next();
				
				HashWrapper			key		= (HashWrapper)entry.getKey();
				
				byte[]	lookup_id	= key.getHash();
				
					// just use the closest contacts - if some have failed then they'll
					// get flushed out by this operation. Grabbing just the live ones
					// is a bad idea as failures may rack up against the live ones due
					// to network problems and kill them, leaving the dead ones!
				
				List	contacts = control.getClosestKContactsList( lookup_id, false );
							
					// if we are no longer one of the K closest contacts then we shouldn't
					// cache the value
				
				boolean	keep_caching	= false;
				
				for (int j=0;j<contacts.size();j++){
				
					if ( router.isID(((DHTTransportContact)contacts.get(j)).getID())){
						
						keep_caching	= true;
						
						break;
					}
				}
				
				if ( !keep_caching ){
					
					DHTLog.log( "Dropping cache entry for " + DHTLog.getString( lookup_id ) + " as now too far away" );
					
					stop_caching.add( key );
					
						// we carry on and do one last publish
					
				}
				
				for (int j=0;j<contacts.size();j++){
					
					DHTTransportContact	contact = (DHTTransportContact)contacts.get(j);
					
					Object[]	data = (Object[])contact_map.get( new HashWrapper(contact.getID()));
					
					if ( data == null ){
						
						data	= new Object[]{ contact, new ArrayList()};
						
						contact_map.put( new HashWrapper(contact.getID()), data );
					}
					
					((List)data[1]).add( key );
				}
			}
		
			it = contact_map.values().iterator();
			
			while( it.hasNext()){
				
				Object[]	data = (Object[])it.next();
				
				DHTTransportContact	contact = (DHTTransportContact)data[0];
				List				keys	= (List)data[1];
					
				byte[][]				store_keys 		= new byte[keys.size()][];
				DHTTransportValue[][]	store_values 	= new DHTTransportValue[store_keys.length][];
				
				keys_published += store_keys.length;
				
				for (int i=0;i<store_keys.length;i++){
					
					HashWrapper	wrapper = (HashWrapper)keys.get(i);
					
					store_keys[i] = wrapper.getHash();
					
					List		values	= (List)republish.get( wrapper );
					
					store_values[i] = new DHTTransportValue[values.size()];
		
					values_published += store_values[i].length;
					
					for (int j=0;j<values.size();j++){
					
						DHTDBValueImpl	value	= (DHTDBValueImpl)values.get(j);
							
							// we reduce the cache distance by 1 here as it is incremented by the
							// recipients
						
						store_values[i][j] = value.getValueForRelay(local_contact);
					}
				}
					
				List	contacts = new ArrayList();
				
				contacts.add( contact );
				
				republish_ops++;
				
				control.put( 
						store_keys, 
						store_values,
						contacts );
			}
		
			try{
				this_mon.enter();
				
				for (int i=0;i<stop_caching.size();i++){
					
					DHTDBMapping	mapping = (DHTDBMapping)stored_values.remove( stop_caching.get(i));
					
					if ( mapping != null ){
						
						mapping.destroy();
					}
				}
			}finally{
				
				this_mon.exit();
			}
		}
		
		return( new int[]{ values_published, keys_published, republish_ops });
	}
		
	protected void
	checkCacheExpiration(
		boolean		force )
	{
		long	 now = SystemTime.getCurrentTime();
		
		if ( !force ){
			
			long elapsed = now - last_cache_expiry_check;
			
			if ( elapsed > 0 && elapsed < MIN_CACHE_EXPIRY_CHECK_INTERVAL ){
				
				return;
			}
		}
			
		try{
			this_mon.enter();
			
			last_cache_expiry_check	= now;
			
			Iterator	it = stored_values.values().iterator();
			
			while( it.hasNext()){
				
				DHTDBMapping	mapping = (DHTDBMapping)it.next();
	
				if ( mapping.getSize() == 0 ){
					
					it.remove();
					
				}else{
					
					Iterator	it2 = mapping.getValues();
					
					while( it2.hasNext()){
						
						DHTDBValueImpl	value = (DHTDBValueImpl)it2.next();
						
						int	distance = value.getCacheDistance();
						
							// distance = 0 are explicitly published and need to be explicitly removed
						
						if ( distance == 1 ){
							
								// distance 1 = initial store location. We use the initial creation date
								// when deciding whether or not to remove this, plus a bit, as the 
								// original publisher is supposed to republish these
							
							if ( now - value.getCreationTime() > original_republish_interval + ORIGINAL_REPUBLISH_INTERVAL_GRACE ){
								
								DHTLog.log( "removing cache entry at level " + distance + " (" + value.getString() + ")" );
								
								it2.remove();
							}
							
						}else if ( distance > 1 ){
							
								// distance 2 get 1/2 time, 3 get 1/4 etc.
								// these are indirectly cached at the nearest location traversed
								// when performing a lookup. the store time is used when deciding
								// whether or not to remove these in an ever reducing amount the
								// further away from the correct cache position that the value is
							
							long	permitted = cache_republish_interval >> (distance-1);
							
							if ( now - value.getStoreTime() >= permitted ){
								
								DHTLog.log( "removing cache entry at level " + distance + " (" + value.getString() + ")" );
								
								it2.remove();
							}
						}
					}
				}
			}
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	print()
	{
		Map	count = new TreeMap();
		
		try{
			this_mon.enter();
			
			logger.log( "Stored keys = " + stored_values.size() + ", values = " + getSize()); 

			Iterator	it = stored_values.entrySet().iterator();
			
			while( it.hasNext()){
						
				Map.Entry		entry = (Map.Entry)it.next();
				
				HashWrapper		value_key	= (HashWrapper)entry.getKey();
				
				DHTDBMapping	mapping = (DHTDBMapping)entry.getValue();
				
				DHTDBValue[]	values = mapping.get(null,0,false);
					
				for (int i=0;i<values.length;i++){
					
					DHTDBValue	value = values[i];
					
					Integer key = new Integer( value.getCacheDistance());
					
					Object[]	data = (Object[])count.get( key );
									
					if ( data == null ){
						
						data = new Object[2];
						
						data[0] = new Integer(1);
						
						data[1] = "";
									
						count.put( key, data );
	
					}else{
						
						data[0] = new Integer(((Integer)data[0]).intValue() + 1 );
					}
				
					String	s = (String)data[1];
					
					s += (s.length()==0?"":", ") + "key=" + DHTLog.getString2(value_key.getHash()) + ",val=" + value.getString();
					
					data[1]	= s;
				}
			}
			
			it = count.keySet().iterator();
			
			while( it.hasNext()){
				
				Integer	k = (Integer)it.next();
				
				Object[]	data = (Object[])count.get(k);
				
				logger.log( "    " + k + " -> " + data[0] + " entries" ); // ": " + data[1]);
			}
			
			it = stored_values.entrySet().iterator();
			
			String	str 		= "    ";
			int		str_entries	= 0;
			
			while( it.hasNext()){
						
				Map.Entry		entry = (Map.Entry)it.next();
				
				HashWrapper		value_key	= (HashWrapper)entry.getKey();
				
				DHTDBMapping	mapping = (DHTDBMapping)entry.getValue();
				
				if ( str_entries == 16 ){
					
					logger.log( str );
					
					str = "    ";
					
					str_entries	= 0;
				}
				
				str_entries++;
				
				str += (str_entries==1?"":", ") + DHTLog.getString2(value_key.getHash()) + " -> " + mapping.getSize() + "/" + mapping.getHits()+"["+mapping.getLocalSize()+","+mapping.getDirectSize()+","+mapping.getIndirectSize() + "]";
			}
			
			if ( str_entries > 0 ){
				
				logger.log( str );
			}
		}finally{
			
			this_mon.exit();
		}
	}
}
