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
				
				DHTDBValueImpl	value = (DHTDBValueImpl)it.next();
				
				if ( value.getCacheDistance() == 0 ){
					
					value.setOriginator( local_contact );
				}
			}
		}
	}
	
	public DHTDBValue
	store(
		HashWrapper		key,
		byte[]			value )
	{
		synchronized( stored_values ){
			
			DHTDBValue res =	
				new DHTDBValueImpl( 
						SystemTime.getCurrentTime(), 
						value, 
						local_contact, 
						local_contact,
						0,
						0 );
	
	
				// don't police max check for locally stored data
				// only that received
			
			stored_values.put( key, res );
			
			return( res );
		}	
	}
	
	public DHTDBValue
	store(
		DHTTransportContact 	originating_contact, 
		HashWrapper				key,
		DHTTransportValue		value )
	{
			// All values have
			//	1) a key
			//	2) a value
			//	3) an originator (the contact who originally published it)
			//	4) an originating contact (the contact who sent it, could be diff for caches)
		
			// for a given key
			//		a) we only hold one entry per originating contact (IP+port) (latest)
			//		b) we only allow up to 8 entries per originating IP address (excluding port)
			// 		c) only the originator can delete an entry
			//		d) if multiple keys have the same value the value is only returned once
		
			// a value can be "volatile" - this means that the cacher can ping the originator
			// periodically and delete the value if it is dead
		
		
			// the aim here is to
			//	1) 	reduce ability for single contacts to spam the key while supporting up to 8 
			//		contacts on a given IP (assuming NAT is being used)
			//	2)	stop one contact deleting or overwriting another contact's entry
			//	3)	support garbage collection for contacts that don't delete entries on exit
		
		synchronized( stored_values ){
			
			if ( stored_values.size() >= max_values_stored ){
				
					// just drop it
				
				DHTLog.log( "Max entries exceeded" );
				
				return( null );
				
			}else{
				
				checkCacheExpiration( false );
				
					// don't replace a closer cache value with a further away one. in particular
					// we have to avoid the case where the original publisher of a key happens to
					// be close to it and be asked by another node to cache it!
								
				DHTDBValueImpl	existing = (DHTDBValueImpl)stored_values.get( key );
				
				if ( existing == null || existing.getCacheDistance() > value.getCacheDistance() + 1 ){
					
					stored_values.put(
							key, 
							new DHTDBValueImpl( originating_contact, value, 1 ));
				}else{
					
						// mark it as current 
					
					existing.reset();
				}
				
				return( existing );
			}
		}
	}
	
	public DHTDBValue
	get(
		HashWrapper		key )
	{
		synchronized( stored_values ){
			
			checkCacheExpiration( false );
						
			DHTDBValue value = (DHTDBValue)stored_values.get(key);
			
				// TODO: think more on this - secondary caching is open to exploitation for DOS as a single
				// contact could spam all contacts surrounding the target with bogus information 
				// current approach is to only allow usage of a secondary cache entry ONCE before
				// we delete it :P
			
			if ( value != null && value.getTransportValue().getCacheDistance() > 1 ){
				
				stored_values.remove( key );
			}
			
			return( value );
		}
	}
	
	public DHTDBValue
	remove(
		HashWrapper		key )
	{
		synchronized( stored_values ){
		
			DHTDBValue val = (DHTDBValue)stored_values.remove( key );
			
			return( val );
		}
	}
	
	public boolean
	isEmpty()
	{
		return( stored_values.size() == 0 );
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
				
				HashWrapper			key		= (HashWrapper)entry.getKey();
				
				DHTTransportValue	value	= (DHTTransportValue)entry.getValue();
				
				if ( value.getCacheDistance() == 0 ){
					
					republish.put( key, value );
				}
			}
		}
		
		Iterator	it = republish.entrySet().iterator();
		
		while( it.hasNext()){
			
			Map.Entry	entry = (Map.Entry)it.next();
			
			HashWrapper			key		= (HashWrapper)entry.getKey();
			
			DHTDBValueImpl	value	= (DHTDBValueImpl)entry.getValue();
			
				// we're republising the data, reset the creation time
			
			value.setCreationTime();
			
			control.put( key.getHash(), value, 0 );
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
				
				DHTDBValueImpl	value	= (DHTDBValueImpl)entry.getValue();
				
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
							
						republish.put( key, value );
					}
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
				
				DHTDBValueImpl	value	= (DHTDBValueImpl)entry.getValue();
				
				byte[]	lookup_id	= key.getHash();
				
				List	contacts = control.getClosestKContactsList( lookup_id );
							
					// if we are no longer one of the K closest contacts then we shouldn't
					// cache the value
				
				boolean	keep_caching	= false;
				
				for (int i=0;i<contacts.size();i++){
				
					if ( router.isID(((DHTTransportContact)contacts.get(i)).getID())){
						
						keep_caching	= true;
						
						break;
					}
				}
				
				if ( !keep_caching ){
					
					logger.log( "Dropping cache entry for " + DHTLog.getString( lookup_id ) + " as now too far away" );
					
					stop_caching.add( key );
				}
					// we reduce the cache distance by 1 here as it is incremented by the
					// recipients
				
				control.put( 
						lookup_id, 
						(DHTDBValueImpl)value.getValueForRelay(local_contact), 
						contacts );
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
			
			DHTDBValueImpl	value = (DHTDBValueImpl)it.next();
			
			int	distance = value.getCacheDistance();
			
				// distance = 0 are explicitly published and need to be explicitly removed
			
			if ( distance == 1 ){
				
					// distance 1 = initial store location. We use the initial creation date
					// when deciding whether or not to remove this, plus a bit, as the 
					// original publisher is supposed to republish these
				
				if ( now - value.getCreationTime() > original_republish_interval + ORIGINAL_REPUBLISH_INTERVAL_GRACE ){
					
					DHTLog.log( "removing cache entry at level " + distance + " (" + value.getString() + ")" );
					
					it.remove();
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
					
					it.remove();
				}
			}
		}
	}
	
	public void
	print()
	{
		Map	count = new TreeMap();
		
		synchronized( stored_values ){
			
			logger.log( "Stored values = " + stored_values.size()); 

			Iterator	it = stored_values.entrySet().iterator();
			
			while( it.hasNext()){
						
				Map.Entry			entry = (Map.Entry)it.next();
				
				HashWrapper			value_key	= (HashWrapper)entry.getKey();
				
				DHTTransportValue	value		= (DHTTransportValue)entry.getValue();
				
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
				
				s += (s.length()==0?"":", ") + "key=" + DHTLog.getString(value_key.getHash()) + ",val=" + value.getString();
				
				data[1]	= s;
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
