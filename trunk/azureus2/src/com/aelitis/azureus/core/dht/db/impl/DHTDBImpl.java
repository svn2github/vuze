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

import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.Timer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.plugins.logging.LoggerChannel;


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
	private DHTRouter				router;
	private DHTTransportContact		local_contact;
	private LoggerChannel			logger;
	
	public
	DHTDBImpl(
		int				_original_republish_interval,
		int				_cache_republish_interval,
		int				_max_values_stored,
		LoggerChannel	_logger )
	{
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
					logger.log( "Republishing original mappings" );
						
					republishOriginalMappings();
				}
			});
					
				// random skew here so that cache refresh isn't very synchronized, as the optimisations
				// regarding non-republising benefit from this 
			
		timer.addPeriodicEvent(
				cache_republish_interval + 10000 - (int)(Math.random()*20000),
				new TimerEventPerformer()
				{
					public void
					perform(
						TimerEvent	event )
					{
						logger.log( "Republishing cached mappings" );
						
						republishCachedMappings();					
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
		
		synchronized( stored_values ){
			
			Iterator	it = stored_values.values().iterator();
			
			while( it.hasNext()){
				
				DHTDBMapping	mapping = (DHTDBMapping)it.next();
				
				mapping.updateLocalContact( local_contact );
			}
		}
	}
	
	public DHTDBValue
	store(
		HashWrapper		key,
		byte[]			value )
	{
		synchronized( stored_values ){
			
			DHTDBValueImpl res =	
				new DHTDBValueImpl( 
						SystemTime.getCurrentTime(), 
						value, 
						local_contact, 
						local_contact,
						0,
						0 );
	
	
				// don't police max check for locally stored data
				// only that received
			
			DHTDBMapping	mapping = (DHTDBMapping)stored_values.get( key );
			
			if ( mapping == null ){
				
				mapping = new DHTDBMapping( key );
				
				stored_values.put( key, mapping );
			}
			
			mapping.add( res );
			
			return( res );
		}	
	}
	
	public DHTDBValue
	store(
		DHTTransportContact 	sender, 
		HashWrapper				key,
		DHTTransportValue		value )
	{
		synchronized( stored_values ){
			
				// TODO:size
			
			if ( getSize() >= max_values_stored ){
				
					// just drop it
				
				logger.log( "Max entries exceeded" );
				
				return( null );
				
			}else{
				
				checkCacheExpiration( false );
				
				DHTDBMapping	existing_mapping = (DHTDBMapping)stored_values.get( key );
				
				if ( existing_mapping == null ){
					
					existing_mapping = new DHTDBMapping( key );
					
					stored_values.put( key, existing_mapping );
				}
				
				DHTDBValueImpl mapping_value	= new DHTDBValueImpl( sender, value, 1 );
					
				existing_mapping.add( mapping_value );
				
				return( mapping_value );
			}
		}
	}
	
	public DHTDBValue[]
	get(
		HashWrapper		key,
		int				max_values )	// 0 -> all
	{
		synchronized( stored_values ){
			
			checkCacheExpiration( false );
					
			DHTDBMapping mapping = (DHTDBMapping)stored_values.get(key);
			
			if ( mapping == null ){
				
				return( new DHTDBValueImpl[0]);
			}
			
			return( mapping.get( max_values, true ));

		}
	}
	
	public DHTDBValue
	remove(
		DHTTransportContact 	sender,
		HashWrapper				key )
	{
		synchronized( stored_values ){
		
			DHTDBMapping mapping = (DHTDBMapping)stored_values.get( key );
			
			if ( mapping != null ){
				
				return( mapping.remove( sender ));
			}
			
			return( null );
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
		synchronized( stored_values ){
			
			int	res = 0;
			
			Iterator	it = stored_values.values().iterator();
			
			while( it.hasNext()){
				
				DHTDBMapping	mapping = (DHTDBMapping)it.next();
				
				res += mapping.getSize();
			}
			
			return( res );
		}
	}
	
	public Iterator
	getKeys()
	{
		synchronized( stored_values ){
			
			return( new ArrayList( stored_values.keySet()).iterator());
		}
	}
	
	protected void
	republishOriginalMappings()
	{
		Map	republish = new HashMap();
		
		synchronized( stored_values ){
			
			Iterator	it = stored_values.entrySet().iterator();
			
			while( it.hasNext()){
				
				Map.Entry	entry = (Map.Entry)it.next();
				
				HashWrapper		key		= (HashWrapper)entry.getKey();
				
				DHTDBMapping	mapping	= (DHTDBMapping)entry.getValue();
				
				Iterator	it2 = mapping.getKeys();
				
				List	values = new ArrayList();
				
				while( it2.hasNext()){
					
					DHTDBValueImpl	value = (DHTDBValueImpl)mapping.get((HashWrapper)it2.next());
				
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
		}
		
		Iterator	it = republish.entrySet().iterator();
		
		while( it.hasNext()){
			
			Map.Entry	entry = (Map.Entry)it.next();
			
			HashWrapper			key		= (HashWrapper)entry.getKey();
			
			List		values	= (List)entry.getValue();
			
				// TODO: multiple values
			
			for (int i=0;i<values.size();i++){
				
				control.put( key.getHash(), (DHTDBValueImpl)values.get(i), 0 );
			}
		}
	}
	
	protected void
	republishCachedMappings()
	{		
			// first refresh any leaves that have not performed at least one lookup in the
			// last period
		
		router.refreshIdleLeaves( cache_republish_interval );
		
		Map	republish = new HashMap();
		
		long	now = System.currentTimeMillis();
		
		synchronized( stored_values ){
			
			checkCacheExpiration( true );

			Iterator	it = stored_values.entrySet().iterator();
			
			while( it.hasNext()){
				
				Map.Entry	entry = (Map.Entry)it.next();
				
				HashWrapper			key		= (HashWrapper)entry.getKey();
				
				DHTDBMapping		mapping	= (DHTDBMapping)entry.getValue();
				
				Iterator	it2 = mapping.getKeys();
				
				List	values = new ArrayList();
				
				while( it2.hasNext()){
					
					DHTDBValueImpl	value = (DHTDBValueImpl)mapping.get((HashWrapper)it2.next());
				
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
			
			while( it.hasNext()){
				
				Map.Entry	entry = (Map.Entry)it.next();
				
				HashWrapper			key		= (HashWrapper)entry.getKey();
				
				List				values	= (List)entry.getValue();
				
				for (int i=0;i<values.size();i++){
					
					DHTDBValueImpl	value	= (DHTDBValueImpl)values.get(i);
					
					byte[]	lookup_id	= key.getHash();
					
					List	contacts = control.getClosestKContactsList( lookup_id );
								
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
					}
					
						// we reduce the cache distance by 1 here as it is incremented by the
						// recipients
					
						// TODO: multiple values
					
					control.put( 
							lookup_id, 
							(DHTDBValueImpl)value.getValueForRelay(local_contact), 
							contacts );
				}
			}
			
			synchronized( stored_values ){
				
				for (int i=0;i<stop_caching.size();i++){
					
					stored_values.remove( stop_caching.get(i));
				}
			}
		}
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
				
		last_cache_expiry_check	= now;
		
		Iterator	it = stored_values.values().iterator();
		
		while( it.hasNext()){
			
			DHTDBMapping	mapping = (DHTDBMapping)it.next();

			Iterator	it2 = mapping.getKeys();
			
			while( it2.hasNext()){
				
				DHTDBValueImpl	value = mapping.get((HashWrapper)it2.next());
				
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
	
	public void
	print()
	{
		Map	count = new TreeMap();
		
		synchronized( stored_values ){
			
			logger.log( "Stored values = " + getSize()); 

			Iterator	it = stored_values.entrySet().iterator();
			
			while( it.hasNext()){
						
				Map.Entry		entry = (Map.Entry)it.next();
				
				HashWrapper		value_key	= (HashWrapper)entry.getKey();
				
				DHTDBMapping	mapping = (DHTDBMapping)entry.getValue();
				
				DHTDBValue[]	values = mapping.get(0,false);
					
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
		}
				
		Iterator	it = count.keySet().iterator();
		
		while( it.hasNext()){
			
			Integer	k = (Integer)it.next();
			
			Object[]	data = (Object[])count.get(k);
			
			logger.log( "    " + k + " -> " + data[0] + ": " + data[1]);
		}
	}
}
