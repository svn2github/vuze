/*
 * Created on 12-Jan-2005
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

package com.aelitis.azureus.core.dht.control.impl;

import java.io.*;
import java.math.BigInteger;
import java.util.*;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.ListenerManager;
import org.gudy.azureus2.core3.util.ListenerManagerDispatcher;
import org.gudy.azureus2.core3.util.SHA1Hasher;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.ThreadPool;
import org.gudy.azureus2.core3.util.ThreadPoolTask;
import org.gudy.azureus2.plugins.logging.LoggerChannel;

import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.DHTOperationAdapter;
import com.aelitis.azureus.core.dht.DHTOperationListener;
import com.aelitis.azureus.core.dht.impl.*;
import com.aelitis.azureus.core.dht.control.*;
import com.aelitis.azureus.core.dht.db.*;
import com.aelitis.azureus.core.dht.router.*;
import com.aelitis.azureus.core.dht.transport.*;

/**
 * @author parg
 *
 */

public class 
DHTControlImpl 
	implements DHTControl, DHTTransportRequestHandler
{
	private static final int EXTERNAL_LOOKUP_CONCURRENCY	= 32;
	private static final int EXTERNAL_PUT_CONCURRENCY		= 16;
	
	private static final int RANDOM_QUERY_PERIOD			= 5*60*1000;
	
	private DHTControlAdapter		adapter;
	private DHTTransport			transport;
	private DHTTransportContact		local_contact;
	
	private DHTRouter		router;
	
	private DHTDB			database;
	
	private DHTControlStatsImpl	stats;
	
	private LoggerChannel	logger;
	
	private	int			node_id_byte_count;
	private int			search_concurrency;
	private int			lookup_concurrency;
	private int			cache_at_closest_n;
	private int			K;
	private int			B;
	private int			max_rep_per_node;
	
	private long		router_start_time;
	private int			router_count;
		
	private ThreadPool	internal_lookup_pool;
	private ThreadPool	external_lookup_pool;
	private ThreadPool	internal_put_pool;
	private ThreadPool	external_put_pool;
	
	private Map			imported_state	= new HashMap();
	
	private long		last_lookup;
	

	private ListenerManager	listeners 	= ListenerManager.createAsyncManager(
			"DHTControl:listenDispatcher",
			new ListenerManagerDispatcher()
			{
				public void
				dispatch(
					Object		_listener,
					int			type,
					Object		value )
				{
					DHTControlListener	target = (DHTControlListener)_listener;
			
					target.activityChanged((DHTControlActivity)value, type );
				}
			});

	private List		activities		= new ArrayList();
	private AEMonitor	activity_mon	= new AEMonitor( "DHTControl:activities" );
	
	protected AEMonitor	estimate_mon		= new AEMonitor( "DHTControl:estimate" );
	private long		last_dht_estimate_time;
	private long		dht_estimate;
	
	private static final int	ESTIMATE_HISTORY	= 32;
	
	private Map	estimate_values = 
		new LinkedHashMap(ESTIMATE_HISTORY,0.75f,true)
		{
			protected boolean 
			removeEldestEntry(
		   		Map.Entry eldest) 
			{
				return( size() > ESTIMATE_HISTORY );
			}
		};
		
	public
	DHTControlImpl(
		DHTControlAdapter	_adapter,
		DHTTransport		_transport,
		int					_K,
		int					_B,
		int					_max_rep_per_node,
		int					_search_concurrency,
		int					_lookup_concurrency,
		int					_original_republish_interval,
		int					_cache_republish_interval,
		int					_cache_at_closest_n,
		LoggerChannel		_logger )
	{
		adapter		= _adapter;
		transport	= _transport;
		logger		= _logger;
		
		K								= _K;
		B								= _B;
		max_rep_per_node				= _max_rep_per_node;
		search_concurrency				= _search_concurrency;
		lookup_concurrency				= _lookup_concurrency;
		cache_at_closest_n				= _cache_at_closest_n;
		
			// set this so we don't do initial calculation until reasonably populated
		
		last_dht_estimate_time	= SystemTime.getCurrentTime();
		
		database = DHTDBFactory.create( 
						adapter.getStorageAdapter(),
						_original_republish_interval,
						_cache_republish_interval,
						logger );
					
		internal_lookup_pool 	= new ThreadPool("DHTControl:internallookups", lookup_concurrency );
		internal_put_pool 		= new ThreadPool("DHTControl:internalputs", lookup_concurrency );
		
			// external pools queue when full ( as opposed to blocking )
		
		external_lookup_pool 	= new ThreadPool("DHTControl:externallookups", EXTERNAL_LOOKUP_CONCURRENCY, true );
		external_put_pool 		= new ThreadPool("DHTControl:puts", EXTERNAL_PUT_CONCURRENCY, true );

		createRouter( transport.getLocalContact());

		node_id_byte_count	= router.getID().length;

		stats = new DHTControlStatsImpl( this );
		
		transport.setRequestHandler( this );
	
		transport.addListener(
			new DHTTransportListener()
			{
				public void
				localContactChanged(
					DHTTransportContact	new_local_contact )
				{
					logger.log( "Transport ID changed, recreating router" );
					
					List	contacts = router.findBestContacts( 0 );
					
					DHTRouter	old_router = router;
					
					createRouter( new_local_contact );
				
					for (int i=0;i<contacts.size();i++){
						
						DHTRouterContact	contact = (DHTRouterContact)contacts.get(i);
					
						if ( !old_router.isID( contact.getID())){
							
							if ( contact.isAlive()){
								
								router.contactAlive( contact.getID(), contact.getAttachment());
								
							}else{
								
								router.contactKnown( contact.getID(), contact.getAttachment());
							}
						}
						
					}
					
					seed();
				}
				
				public void
				currentAddress(
					String		address )
				{
				}
			});
		

	}
	
	protected void
	createRouter(
		DHTTransportContact		_local_contact)
	{	
		router_start_time	= SystemTime.getCurrentTime();
		router_count++;
		
		local_contact	= _local_contact;
		
		router	= DHTRouterFactory.create( 
					K, B, max_rep_per_node,
					local_contact.getID(), 
					new DHTControlContactImpl( local_contact ),
					logger);
		
		router.setAdapter( 
			new DHTRouterAdapter()
			{
				public void
				requestPing(
					DHTRouterContact	contact )
				{
					DHTControlImpl.this.requestPing( contact );
				}
				
				public void
				requestLookup(
					byte[]		id,
					String		description )
				{
					lookup( internal_lookup_pool, 
							id, 
							description,
							(byte)0,
							false, 
							0, 
							search_concurrency, 
							1,
							router.getK()/4,	// decrease search accuracy for refreshes
							new lookupResultHandler(new DHTOperationAdapter())
							{
								public void
								diversify(
									byte	diversification_type )
								{
								}
								
								public void
								closest(
									List		res )
								{
								}						
							});
				}
				
				public void
				requestAdd(
					DHTRouterContact	contact )
				{
					nodeAddedToRouter( contact );
				}
			});	
		
		database.setControl( this );
	}
	
	public long
	getRouterUptime()
	{
		return( SystemTime.getCurrentTime() - router_start_time );
	}
	
	public int
	getRouterCount()
	{
		return( router_count );
	}
	
	public DHTControlStats
	getStats()
	{
		return( stats );
	}
	
	public DHTTransport
	getTransport()
	{
		return( transport );
	}
	
	public DHTRouter
	getRouter()
	{
		return( router );
	}
	
	public DHTDB
	getDataBase()
	{
		return( database );
	}
	
	public void
	contactImported(
		DHTTransportContact	contact )
	{
		byte[]	id = contact.getID();
		
		router.contactKnown( id, new DHTControlContactImpl(contact));
	}
	
	public void
	exportState(
		DataOutputStream	daos,
		int					max )
	
		throws IOException
	{
			/*
			 * We need to be a bit smart about exporting state to deal with the situation where a
			 * DHT is started (with good import state) and then stopped before the goodness of the
			 * state can be re-established. So we remember what we imported and take account of this
			 * on a re-export
			 */
		
			// get all the contacts
		
		List	contacts = router.findBestContacts( 0 );
		
			// give priority to any that were alive before and are alive now
		
		List	to_save 	= new ArrayList();
		List	reserves	= new ArrayList();
		
		//System.out.println( "Exporting" );
		
		for (int i=0;i<contacts.size();i++){
		
			DHTRouterContact	contact = (DHTRouterContact)contacts.get(i);
			
			Object[]	imported = (Object[])imported_state.get( new HashWrapper( contact.getID()));
			
			if ( imported != null ){

				if ( contact.isAlive()){
					
						// definitely want to keep this one
					
					to_save.add( contact );
					
				}else if ( !contact.isFailing()){
					
						// dunno if its still good or not, however its got to be better than any
						// new ones that we didn't import who aren't known to be alive
					
					reserves.add( contact );
				}
			}
		}
		
		//System.out.println( "    initial to_save = " + to_save.size() + ", reserves = " + reserves.size());
		
			// now pull out any live ones
		
		for (int i=0;i<contacts.size();i++){
			
			DHTRouterContact	contact = (DHTRouterContact)contacts.get(i);
		
			if ( contact.isAlive() && !to_save.contains( contact )){
				
				to_save.add( contact );
			}
		}
		
		//System.out.println( "    after adding live ones = " + to_save.size());
		
			// now add any reserve ones
		
		for (int i=0;i<reserves.size();i++){
			
			DHTRouterContact	contact = (DHTRouterContact)reserves.get(i);
		
			if ( !to_save.contains( contact )){
				
				to_save.add( contact );
			}
		}
		
		//System.out.println( "    after adding reserves = " + to_save.size());

			// now add in the rest!
		
		for (int i=0;i<contacts.size();i++){
			
			DHTRouterContact	contact = (DHTRouterContact)contacts.get(i);
		
			if (!to_save.contains( contact )){
				
				to_save.add( contact );
			}
		}	
		
			// and finally remove the invalid ones
		
		Iterator	it = to_save.iterator();
		
		while( it.hasNext()){
			
			DHTRouterContact	contact	= (DHTRouterContact)it.next();
			
			DHTTransportContact	t_contact = ((DHTControlContactImpl)contact.getAttachment()).getContact();
			
			if ( !t_contact.isValid()){
				
				it.remove();
			}
		}
	
		//System.out.println( "    finally = " + to_save.size());

		int	num_to_write = Math.min( max, to_save.size());
		
		daos.writeInt( num_to_write );
				
		for (int i=0;i<num_to_write;i++){
			
			DHTRouterContact	contact = (DHTRouterContact)to_save.get(i);
			
			//System.out.println( "export:" + contact.getString());
			
			daos.writeLong( contact.getTimeAlive());
			
			DHTTransportContact	t_contact = ((DHTControlContactImpl)contact.getAttachment()).getContact();
			
			try{
									
				t_contact.exportContact( daos );
				
			}catch( DHTTransportException e ){
				
					// shouldn't fail as for a contact to make it to the router 
					// it should be valid...
				
				Debug.printStackTrace( e );
				
				throw( new IOException( e.getMessage()));
			}
		}
		
		daos.flush();
	}
		
	public void
	importState(
		DataInputStream		dais )
		
		throws IOException
	{
		int	num = dais.readInt();
		
		for (int i=0;i<num;i++){
			
			try{
				
				long	time_alive = dais.readLong();
				
				DHTTransportContact	contact = transport.importContact( dais );
								
				imported_state.put( new HashWrapper( contact.getID()), new Object[]{ new Long( time_alive ), contact });
				
			}catch( DHTTransportException e ){
				
				Debug.printStackTrace( e );
			}
		}
	}
	
	public void
	seed()
	{
		final AESemaphore	sem = new AESemaphore( "DHTControl:seed" );
		
		lookup( internal_lookup_pool,
				router.getID(), 
				"Seeding DHT",
				(byte)0,
				false, 
				0,
				search_concurrency*4,
				1,
				router.getK(),
				new lookupResultHandler(new DHTOperationAdapter())
				{
					public void
					diversify(
						byte	diversification_type )
					{
					}
										
					public void
					closest(
						List		res )
					{
						try{
							router.seed();
							
						}finally{
							
							sem.release();
						}
					}
				});
		
		sem.reserve();
	}
	
	protected void
	poke()
	{
		long	now = SystemTime.getCurrentTime();
		
		if ( now - last_lookup > RANDOM_QUERY_PERIOD ){
			
			last_lookup	= now;
			
				// we don't want this to be blocking as it'll stuff the stats
			
			external_lookup_pool.run(
				new task(external_lookup_pool)
				{
					private byte[]	target = {};
					
					public void
					runSupport()
					{
						target = router.refreshRandom();
					}
					
					public byte[]
					getTarget()
					{
						return( target );
					}
					
					public String
					getDescription()
					{
						return( "Random Query" ); 
					}
				});
		}
	}
	
	public void
	put(
		byte[]					_unencoded_key,
		String					_description,
		byte[]					_value,
		byte					_flags,
		DHTOperationListener	_listener )
	{
		if ( _value.length == 0 ){
			
				// zero length denotes value removal
			
			throw( new RuntimeException( "zero length values not supported"));
		}
		
		byte[]	encoded_key = encodeKey( _unencoded_key );
		
		DHTLog.log( "put for " + DHTLog.getString( encoded_key ));
		
		DHTDBValue	value = database.store( new HashWrapper( encoded_key ), _value, _flags );
		
		put( 	external_put_pool,
				encoded_key, 
				_description,
				value, 
				0, 
				_listener instanceof DHTOperationListenerDemuxer?
						(DHTOperationListenerDemuxer)_listener:
						new DHTOperationListenerDemuxer(_listener));		
	}
	
	public void
	putEncodedKey(
		final byte[]			encoded_key,
		final String			description,
		final DHTTransportValue	value,
		final long				timeout )
	{
		put( internal_put_pool, encoded_key, description, value, timeout, new DHTOperationListenerDemuxer( new DHTOperationAdapter()));
	}
	
	
	protected void
	put(
		final ThreadPool					thread_pool,
		final byte[]						initial_encoded_key,
		final String						description,
		final DHTTransportValue				value,
		final long							timeout,
		final DHTOperationListenerDemuxer	listener )
	{
		put( thread_pool, initial_encoded_key, description, new DHTTransportValue[]{ value }, timeout, listener );
	}
	
	protected void
	put(
		final ThreadPool					thread_pool,
		final byte[]						initial_encoded_key,
		final String						description,
		final DHTTransportValue[]			values,
		final long							timeout,
		final DHTOperationListenerDemuxer	listener )
	{

			// get the initial starting point for the put - may have previously been diversified
		
		byte[][]	encoded_keys	= adapter.diversify( true, true, initial_encoded_key, DHT.DT_NONE );
		
			// may be > 1 if diversification is replicating (for load balancing) 
		
		for (int i=0;i<encoded_keys.length;i++){
			
			final byte[]	encoded_key	= encoded_keys[i];
						
			lookup( thread_pool,
					encoded_key,
					description,
					(byte)0,
					false, 
					timeout,
					search_concurrency,
					1,
					router.getK(),
					new lookupResultHandler(listener)
					{						
						public void
						diversify(
							byte		diversification_type )
						{
							Debug.out( "Shouldn't get a diversify on a lookup-node" );
						}
	
						public void
						closest(
							List				_closest )
						{
							put( 	thread_pool,
									new byte[][]{ encoded_key }, 
									"Cache store of " + description,
									new DHTTransportValue[][]{ values }, 
									_closest, 
									timeout, 
									listener, 
									true );		
						}
					});
		}
	}
	
	public void
	putDirectEncodedKeys(
		byte[][]				encoded_keys,
		String					description,
		DHTTransportValue[][]	value_sets,
		List					contacts )
	{
			// we don't consider diversification for direct puts (these are for republishing
			// of cached mappings and we maintain these as normal - its up to the original
			// publisher to diversify as required)
		
		put( 	internal_put_pool,
				encoded_keys, 
				description,
				value_sets, 
				contacts, 
				0, 
				new DHTOperationListenerDemuxer( new DHTOperationAdapter()),
				false );
	}
		
	protected void
	put(
		final ThreadPool						thread_pool,
		final byte[][]							encoded_keys,
		final String							description,
		final DHTTransportValue[][]				value_sets,
		final List								contacts,
		final long								timeout,
		final DHTOperationListenerDemuxer		listener,
		final boolean							consider_diversification )
	{		
			// only diversify on one hit as we're storing at closest 'n' so we only need to
			// do it once for each key
		
		final boolean[]	diversified = new boolean[encoded_keys.length];
		
		for (int i=0;i<contacts.size();i++){
		
			DHTTransportContact	contact = (DHTTransportContact)contacts.get(i);
			
			if ( router.isID( contact.getID())){
					
					// don't send to ourselves!
				
			}else{
				
				try{

					for (int j=0;j<value_sets.length;j++){
							
						for (int k=0;k<value_sets[j].length;k++){
							
							listener.wrote( contact, value_sets[j][k] );
						}
					}
							
						// each store is going to report its complete event
					
					listener.incrementCompletes();
					
					contact.sendStore( 
						new DHTTransportReplyHandlerAdapter()
						{
							public void
							storeReply(
								DHTTransportContact _contact,
								byte[]				_diversifications )
							{
								try{
									DHTLog.log( "Store OK " + DHTLog.getString( _contact ));
																
									router.contactAlive( _contact.getID(), new DHTControlContactImpl(_contact));
								
										// can be null for old protocol versions
									
									if ( consider_diversification && _diversifications != null ){
																		
										for (int j=0;j<_diversifications.length;j++){
											
											if ( _diversifications[j] != DHT.DT_NONE && !diversified[j] ){
												
												diversified[j]	= true;
												
												byte[][]	diversified_keys = 
													adapter.diversify( true, false, encoded_keys[j], _diversifications[j] );
											
												for (int k=0;k<diversified_keys.length;k++){
												
													put( 	thread_pool,
															diversified_keys[k], 
															"Diversification of " + description,
															value_sets[j], 
															timeout, listener );
												}
											}
										}
									}
								}finally{
									
									listener.complete( false );
								}	
							}
							
							public void
							failed(
								DHTTransportContact 	_contact,
								Throwable 				_error )
							{
								try{
									DHTLog.log( "Store failed " + DHTLog.getString( _contact ) + " -> failed: " + _error.getMessage());
																			
									router.contactDead( _contact.getID(), new DHTControlContactImpl(_contact));
									
								}finally{
									
									listener.complete( true );
								}
							}
						},
						encoded_keys, 
						value_sets );
					
				}catch( Throwable e ){
										
					Debug.printStackTrace(e);
					
				}
			}
		}
	}
	
	public DHTTransportValue
	getLocalValue(
		byte[]		unencoded_key )
	{
		final byte[]	encoded_key = encodeKey( unencoded_key );

		DHTLog.log( "getLocalValue for " + DHTLog.getString( encoded_key ));

		DHTDBValue	res = database.get( new HashWrapper( encoded_key ));
	
		if ( res == null ){
			
			return( null );
		}
		
		return( res );
	}
	
	public void
	get(
		byte[]						unencoded_key,
		String						description,
		byte						flags,
		int							max_values,
		long						timeout,
		final DHTOperationListener	get_listener )
	{
		final byte[]	encoded_key = encodeKey( unencoded_key );

		DHTLog.log( "get for " + DHTLog.getString( encoded_key ));
		
		getSupport( encoded_key, description, flags, max_values, timeout, new DHTOperationListenerDemuxer( get_listener ));
	}
	
	public void
	getSupport(
		final byte[]						initial_encoded_key,
		final String						description,
		final byte							flags,
		final int							max_values,
		final long							timeout,
		final DHTOperationListenerDemuxer	get_listener )
	{
			// get the initial starting point for the get - may have previously been diversified
		
		byte[][]	encoded_keys	= adapter.diversify( false, true, initial_encoded_key, DHT.DT_NONE );

		for (int i=0;i<encoded_keys.length;i++){
			
			final boolean[]	diversified = { false };

			final byte[]	encoded_key	= encoded_keys[i];
			
			lookup( external_lookup_pool,
					encoded_key, 
					description,
					flags,
					true, 
					timeout,
					search_concurrency,
					max_values,
					router.getK(),
					new lookupResultHandler( get_listener )
					{
						private List	found_values	= new ArrayList();
							
						public void
						diversify(
							byte	diversification_type )
						{
								// we only want to follow one diversification
							
							if ( !diversified[0]){
								
								diversified[0] = true;

								int	rem = max_values==0?0:( max_values - found_values.size());
								
								if ( max_values == 0 || rem > 0 ){
									
									byte[][]	diversified_keys = adapter.diversify( false, false, encoded_key, diversification_type );
									
										// should return a max of 1 (0 if diversification refused)
										// however, could change one day to search > 1 
									
									for (int j=0;j<diversified_keys.length;j++){
										
										getSupport( diversified_keys[j], "Diversification of " + description, flags, rem,  timeout, get_listener );
									}
								}								
							}
						}
						
						public void
						read(
							DHTTransportContact	contact,
							DHTTransportValue	value )
						{	
							found_values.add( value );
							
							super.read( contact, value );
						}
														
						public void
						closest(
							List	closest )
						{
							if ( found_values.size() > 0 ){
									
								DHTTransportValue[]	values = new DHTTransportValue[found_values.size()];
								
								found_values.toArray( values );
								
									// cache the values at the 'n' closest seen locations
								
								for (int k=0;k<Math.min(cache_at_closest_n,closest.size());k++){
									
									DHTTransportContact	contact = (DHTTransportContact)(DHTTransportContact)closest.get(k);
									
									for (int j=0;j<values.length;j++){
										
										wrote( contact, values[j] );
									}
									
									contact.sendStore( 
											new DHTTransportReplyHandlerAdapter()
											{
												public void
												storeReply(
													DHTTransportContact _contact,
													byte[]				_diversifications )
												{
														// don't consider diversification for cache stores as we're not that
														// bothered
													
													DHTLog.log( "Cache store OK " + DHTLog.getString( _contact ));
													
													router.contactAlive( _contact.getID(), new DHTControlContactImpl(_contact));
												}	
												
												public void
												failed(
													DHTTransportContact 	_contact,
													Throwable 				_error )
												{
													DHTLog.log( "Cache store failed " + DHTLog.getString( _contact ) + " -> failed: " + _error.getMessage());
													
													router.contactDead( _contact.getID(), new DHTControlContactImpl(_contact));
												}
											},
											new byte[][]{ encoded_key }, 
											new DHTTransportValue[][]{ values });
								}
							}
						}
					});
		}
	}
		
	public byte[]
	remove(
		byte[]					unencoded_key,
		String					description,
		DHTOperationListener	listener )
	{		
		final byte[]	encoded_key = encodeKey( unencoded_key );

		DHTLog.log( "remove for " + DHTLog.getString( encoded_key ));

		DHTDBValue	res = database.remove( local_contact, new HashWrapper( encoded_key ));
		
		if ( res == null ){
			
				// not found locally, nothing to do
			
			return( null );
			
		}else{
			
				// we remove a key by pushing it back out again with zero length value 
			
			res.setValue( new byte[0] );
			
			put( external_put_pool, encoded_key, description, res, 0, new DHTOperationListenerDemuxer( listener ));
			
			return( res.getValue());
		}
	}
	
		/**
		 * The lookup method returns up to K closest nodes to the target
		 * @param lookup_id
		 * @return
		 */
	
	protected void
	lookup(
		ThreadPool					thread_pool,
		final byte[]				lookup_id,
		final String				description,
		final byte					flags,
		final boolean				value_search,
		final long					timeout,
		final int					concurrency,
		final int					max_values,
		final int					search_accuracy,
		final lookupResultHandler	handler )
	{
		thread_pool.run(
			new task(thread_pool)
			{
				public void
				runSupport()
				{
					try{
						lookupSupportSync( lookup_id, flags, value_search, timeout, concurrency, max_values, search_accuracy, handler );
						
					}catch( Throwable e ){
						
						Debug.printStackTrace(e);
					}
				}
				
				public byte[]
				getTarget()
				{
					return( lookup_id ); 
				}
				
				public String
				getDescription()
				{
					return( description );
				}
			});
	}
	
	protected void
	lookupSupportSync(
		final byte[]				lookup_id,
		byte						flags,
		boolean						value_search,
		long						timeout,
		int							concurrency,
		int							max_values,
		final int					search_accuracy,
		final lookupResultHandler	result_handler )
	{
		boolean		timeout_occurred	= false;
	
		last_lookup	= SystemTime.getCurrentTime();
	
		result_handler.incrementCompletes();
		
		try{
			DHTLog.log( "lookup for " + DHTLog.getString( lookup_id ));
			
				// keep querying successively closer nodes until we have got responses from the K
				// closest nodes that we've seen. We might get a bunch of closer nodes that then
				// fail to respond, which means we have reconsider further away nodes
			
				// we keep a list of nodes that we have queried to avoid re-querying them
			
				// we keep a list of nodes discovered that we have yet to query
			
				// we have a parallel search limit of A. For each A we effectively loop grabbing
				// the currently closest unqueried node, querying it and adding the results to the
				// yet-to-query-set (unless already queried)
			
				// we terminate when we have received responses from the K closest nodes we know
				// about (excluding failed ones)
			
				// Note that we never widen the root of our search beyond the initial K closest
				// that we know about - this could be relaxed
			
						
				// contacts remaining to query
				// closest at front
	
			final Set		contacts_to_query	= getClosestContactsSet( lookup_id, false );
			
			final AEMonitor	contacts_to_query_mon	= new AEMonitor( "DHTControl:ctq" );

			final Map	level_map			= new HashMap();
			
			Iterator	it = contacts_to_query.iterator();
			
			while( it.hasNext()){
				
				DHTTransportContact	contact	= (DHTTransportContact)it.next();
				
				result_handler.found( contact );
				
				level_map.put( contact , new Integer(0));
			}
			
				// record the set of contacts we've queried to avoid re-queries
			
			final Map			contacts_queried = new HashMap();
			
				// record the set of contacts that we've had a reply from
				// furthest away at front
			
			final Set			ok_contacts = new sortedContactSet( lookup_id, false ).getSet(); 
			
	
				// this handles the search concurrency
			
			final AESemaphore	search_sem = new AESemaphore( "DHTControl:search", concurrency );
				
			final int[]	idle_searches	= { 0 };
			final int[]	active_searches	= { 0 };
				
			final int[]	values_found	= { 0 };
			final int[]	value_replies	= { 0 };
			final Set	values_found_set	= new HashSet();
			
			long	start = SystemTime.getCurrentTime();
	
			while( true ){
				
				if ( timeout > 0 ){
					
					long	now = SystemTime.getCurrentTime();
					
					long remaining = timeout - ( now - start );
						
					if ( remaining <= 0 ){
						
						DHTLog.log( "lookup: terminates - timeout" );
	
						timeout_occurred	= true;
						
						break;
						
					}
						// get permission to kick off another search
					
					if ( !search_sem.reserve( remaining )){
						
						DHTLog.log( "lookup: terminates - timeout" );
	
						timeout_occurred	= true;
						
						break;
					}
				}else{
					
					search_sem.reserve();
				}
					
				try{
					contacts_to_query_mon.enter();
			
					if ( 	values_found[0] >= max_values ||
							value_replies[0]>= 2 ){	// all hits should have the same values anyway...	
							
						break;
					}						

						// if nothing pending then we need to wait for the results of a previous
						// search to arrive. Of course, if there are no searches active then
						// we've run out of things to do
					
					if ( contacts_to_query.size() == 0 ){
						
						if ( active_searches[0] == 0 ){
							
							DHTLog.log( "lookup: terminates - no contacts left to query" );
							
							break;
						}
						
						idle_searches[0]++;
						
						continue;
					}
				
						// select the next contact to search
					
					DHTTransportContact	closest	= (DHTTransportContact)contacts_to_query.iterator().next();			
				
						// if the next closest is further away than the furthest successful hit so 
						// far and we have K hits, we're done
					
					if ( ok_contacts.size() == search_accuracy ){
						
						DHTTransportContact	furthest_ok = (DHTTransportContact)ok_contacts.iterator().next();
						
						byte[]	furthest_ok_distance 	= computeDistance( furthest_ok.getID(), lookup_id );
						byte[]	closest_distance		= computeDistance( closest.getID(), lookup_id );
						
						if ( compareDistances( furthest_ok_distance, closest_distance) <= 0 ){
							
							DHTLog.log( "lookup: terminates - we've searched the closest " + search_accuracy + " contacts" );
	
							break;
						}
					}
					
					contacts_to_query.remove( closest );
	
					contacts_queried.put( new HashWrapper( closest.getID()), closest );
								
						// never search ourselves!
					
					if ( router.isID( closest.getID())){
						
						search_sem.release();
						
						continue;
					}
	
					final int	search_level = ((Integer)level_map.get(closest)).intValue();

					active_searches[0]++;				
					
					result_handler.searching( closest, search_level, active_searches[0] );
					
					DHTTransportReplyHandlerAdapter	handler = 
						new DHTTransportReplyHandlerAdapter()
						{
							private boolean	value_reply_received	= false;
							
							public void
							findNodeReply(
								DHTTransportContact 	target_contact,
								DHTTransportContact[]	reply_contacts )
							{
								try{
									DHTLog.log( "findNodeReply: " + DHTLog.getString( reply_contacts ));
							
									router.contactAlive( target_contact.getID(), new DHTControlContactImpl(target_contact));
									
									for (int i=0;i<reply_contacts.length;i++){
										
										DHTTransportContact	contact = reply_contacts[i];
										
											// ignore responses that are ourselves
										
										if ( compareDistances( router.getID(), contact.getID()) == 0 ){
											
											continue;
										}
										
											// dunno if its alive or not, however record its existance
										
										router.contactKnown( contact.getID(), new DHTControlContactImpl(contact));
									}
									
									try{
										contacts_to_query_mon.enter();
												
										ok_contacts.add( target_contact );
										
										if ( ok_contacts.size() > search_accuracy ){
											
												// delete the furthest away
											
											Iterator ok_it = ok_contacts.iterator();
											
											ok_it.next();
											
											ok_it.remove();
										}
										
										for (int i=0;i<reply_contacts.length;i++){
											
											DHTTransportContact	contact = reply_contacts[i];
											
												// ignore responses that are ourselves
											
											if ( compareDistances( router.getID(), contact.getID()) == 0 ){
												
												continue;
											}
																						
											if (	contacts_queried.get( new HashWrapper( contact.getID())) == null &&
													(!contacts_to_query.contains( contact ))){
												
												DHTLog.log( "    new contact for query: " + DHTLog.getString( contact ));
												
												contacts_to_query.add( contact );
												
												result_handler.found( contact );
												
												level_map.put( contact, new Integer( search_level+1));
				
												if ( idle_searches[0] > 0 ){
													
													idle_searches[0]--;
													
													search_sem.release();
												}
											}else{
												
												// DHTLog.log( "    already queried: " + DHTLog.getString( contact ));
											}
										}
									}finally{
										
										contacts_to_query_mon.exit();
									}
								}finally{
									
									try{
										contacts_to_query_mon.enter();

										active_searches[0]--;
										
									}finally{
										
										contacts_to_query_mon.exit();
									}
		
									search_sem.release();
								}
							}
							
							public void
							findValueReply(
								DHTTransportContact 	contact,
								DHTTransportValue[]		values,
								byte					diversification_type,
								boolean					more_to_come )
							{
								DHTLog.log( "findValueReply: " + DHTLog.getString( values ) + ",mtc=" + more_to_come + ", dt=" + diversification_type );

								try{
									if ( diversification_type != DHT.DT_NONE ){
										
											// diversification instruction									
	
										result_handler.diversify( diversification_type );									
									}
									
									value_reply_received	= true;
									
									router.contactAlive( contact.getID(), new DHTControlContactImpl(contact));
									
									int	new_values = 0;
									
									for (int i=0;i<values.length;i++){
										
										DHTTransportValue	value = values[i];
										
										DHTTransportContact	originator = value.getOriginator();
										
											// can't just use originator id as this value can be DOSed (see DB code)
										
										byte[]	originator_id 	= originator.getID();
										byte[]	value_bytes		= value.getValue();
										
										byte[]	value_id = new byte[originator_id.length + value_bytes.length];
										
										System.arraycopy( originator_id, 0, value_id, 0, originator_id.length );
										
										System.arraycopy( value_bytes, 0, value_id, originator_id.length, value_bytes.length );
										
										HashWrapper	x = new HashWrapper( value_id );

										if ( !values_found_set.contains( x )){
											
											new_values++;
											
											values_found_set.add( x );
											
											result_handler.read( contact, values[i] );
										}
									}
											
									try{
										contacts_to_query_mon.enter();

										if ( !more_to_come ){
											
											value_replies[0]++;
										}
										
										values_found[0] += new_values;
										
									}finally{
										
										contacts_to_query_mon.exit();
									}
								}finally{
									
									if ( !more_to_come ){

										try{
											contacts_to_query_mon.enter();
												
											active_searches[0]--;
											
										}finally{
											
											contacts_to_query_mon.exit();
										}
									
										search_sem.release();
									}
								}						
							}
							
							public void
							findValueReply(
								DHTTransportContact 	contact,
								DHTTransportContact[]	contacts )
							{
								findNodeReply( contact, contacts );
							}
							
							public void
							failed(
								DHTTransportContact 	target_contact,
								Throwable 				error )
							{
								try{
										// if at least one reply has been received then we
										// don't treat subsequent failure as indication of
										// a contact failure (just packet loss)
									
									if ( !value_reply_received ){
										
										DHTLog.log( "findNode/findValue " + DHTLog.getString( target_contact ) + " -> failed: " + error.getMessage());
									
										router.contactDead( target_contact.getID(), new DHTControlContactImpl(target_contact));
									}
		
								}finally{
									
									try{
										contacts_to_query_mon.enter();

										active_searches[0]--;
										
									}finally{
										
										contacts_to_query_mon.exit();
									}
									
									search_sem.release();
								}
							}
						};
						
					router.recordLookup( lookup_id );
					
					if ( value_search ){
						
						int	rem = max_values - values_found[0];
						
						if ( rem <= 0 ){
							
							Debug.out( "eh?" );
							
							rem = 1;
						}
						
						closest.sendFindValue( handler, lookup_id, rem, flags );
						
					}else{
						
						closest.sendFindNode( handler, lookup_id );
					}
				}finally{
					
					contacts_to_query_mon.exit();
				}
			}
			
				// maybe unterminated searches still going on so protect ourselves
				// against concurrent modification of result set
			
			List	closest_res;
			
			try{
				contacts_to_query_mon.enter();
	
				DHTLog.log( "lookup complete for " + DHTLog.getString( lookup_id ));
				
				DHTLog.log( "    queried = " + DHTLog.getString( contacts_queried ));
				DHTLog.log( "    to query = " + DHTLog.getString( contacts_to_query ));
				DHTLog.log( "    ok = " + DHTLog.getString( ok_contacts ));
				
				closest_res	= new ArrayList( ok_contacts );
				
					// we need to reverse the list as currently closest is at
					// the end
			
				Collections.reverse( closest_res );
				
				if ( timeout <= 0 && !value_search ){
				
						// we can use the results of this to estimate the DHT size
					
					estimateDHTSize( lookup_id, contacts_queried, search_accuracy );
				}
				
			}finally{
				
				contacts_to_query_mon.exit();
			}
			
			result_handler.closest( closest_res );
			
		}finally{
			
			result_handler.complete( timeout_occurred );
		}
	}
	
	
		// Request methods
	
	public void
	pingRequest(
		DHTTransportContact originating_contact )
	{
		DHTLog.log( "pingRequest from " + DHTLog.getString( originating_contact.getID()));
			
		router.contactAlive( originating_contact.getID(), new DHTControlContactImpl(originating_contact));
	}
		
	public byte[]
	storeRequest(
		DHTTransportContact 	originating_contact, 
		byte[][]				keys,
		DHTTransportValue[][]	value_sets )
	{
		router.contactAlive( originating_contact.getID(), new DHTControlContactImpl(originating_contact));
		
		DHTLog.log( "storeRequest from " + DHTLog.getString( originating_contact.getID())+ ", keys = " + keys.length );

		byte[]	diverse_res = new byte[ keys.length];
		
		for (int i=0;i<keys.length;i++){
			
			HashWrapper			key		= new HashWrapper( keys[i] );
			
			DHTTransportValue[]	values 	= value_sets[i];
		
			DHTLog.log( "    key=" + DHTLog.getString(key) + ", value=" + DHTLog.getString(values));
			
			diverse_res[i] = database.store( originating_contact, key, values );
		}
		
		return( diverse_res );
	}
	
	public DHTTransportContact[]
	findNodeRequest(
		DHTTransportContact originating_contact, 
		byte[]				id )
	{
		DHTLog.log( "findNodeRequest from " + DHTLog.getString( originating_contact.getID()));

		router.contactAlive( originating_contact.getID(), new DHTControlContactImpl(originating_contact));
		
		List	l = getClosestKContactsList( id, false );
		
		DHTTransportContact[]	res = new DHTTransportContact[l.size()];
		
		l.toArray( res );
		
		return( res );
	}
	
	public DHTTransportFindValueReply
	findValueRequest(
		DHTTransportContact originating_contact, 
		byte[]				key,
		int					max_values,
		byte				flags )
	{
		DHTLog.log( "findValueRequest from " + DHTLog.getString( originating_contact.getID()));
		
		DHTDBLookupResult	result	= database.get( originating_contact, new HashWrapper( key ), max_values, true );
					
		if ( result != null ){
			
			router.contactAlive( originating_contact.getID(), new DHTControlContactImpl(originating_contact));

			return( new DHTTransportFindValueReplyImpl( result.getDiversificationType(), result.getValues()));
			
		}else{
			
			return( new DHTTransportFindValueReplyImpl( findNodeRequest( originating_contact, key )));
		}
	}
	
	public DHTTransportFullStats
	statsRequest(
		DHTTransportContact	contact )
	{
		return( stats );
	}
	
	protected void
	requestPing(
		DHTRouterContact	contact )
	{
		((DHTControlContactImpl)contact.getAttachment()).getContact().sendPing(
				new DHTTransportReplyHandlerAdapter()
				{
					public void
					pingReply(
						DHTTransportContact _contact )
					{
						DHTLog.log( "ping OK " + DHTLog.getString( _contact ));
											
						router.contactAlive( _contact.getID(), new DHTControlContactImpl(_contact));
					}	
					
					public void
					failed(
						DHTTransportContact 	_contact,
						Throwable				_error )
					{
						DHTLog.log( "ping " + DHTLog.getString( _contact ) + " -> failed: " + _error.getMessage());
									
						router.contactDead( _contact.getID(), new DHTControlContactImpl(_contact));
					}
				});
	}
	
	protected void
	nodeAddedToRouter(
		DHTRouterContact	new_contact )
	{		
		// when a new node is added we must check to see if we need to transfer
		// any of our values to it.
		
		Map	keys_to_store	= new HashMap();
		
		if( database.isEmpty()){
							
			// nothing to do, ping it if it isn't known to be alive
				
			if ( !new_contact.hasBeenAlive()){
					
				requestPing( new_contact );
			}
				
			return;
		}
			
			// see if we're one of the K closest to the new node
		
		List	closest_contacts = getClosestKContactsList( new_contact.getID(), false );
		
		boolean	close	= false;
		
		for (int i=0;i<closest_contacts.size();i++){
			
			if ( router.isID(((DHTTransportContact)closest_contacts.get(i)).getID())){
				
				close	= true;
				
				break;
			}
		}
		
		if ( !close ){
			
			if ( !new_contact.hasBeenAlive()){
				
				requestPing( new_contact );
			}

			return;
		}
			
			// ok, we're close enough to worry about transferring values				
		
		Iterator	it = database.getKeys();
		
		while( it.hasNext()){
							
			HashWrapper	key		= (HashWrapper)it.next();
			
			byte[]	encoded_key		= key.getHash();
			
			DHTDBLookupResult	result = database.get( null, key, 0, false );
			
			if ( result == null  ){
				
					// deleted in the meantime
				
				continue;
			}
			
				// even if a result has been diversified we continue to maintain the base value set
				// until the original publisher picks up the diversification (next publish period) and
				// publishes to the correct place
			
			DHTDBValue[]	values = result.getValues();
			
			List	values_to_store = new ArrayList();
			
			for (int i=0;i<values.length;i++){
				
				DHTDBValue	value = values[i];
		
				// we don't consider any cached further away than the initial location, for transfer
				// however, we *do* include ones we originate as, if we're the closest, we have to
				// take responsibility for xfer (as others won't)
			
				if ( value.getCacheDistance() > 1 ){
					
					continue;
				}
				
				List		sorted_contacts	= getClosestKContactsList( encoded_key, false ); 
				
					// if we're closest to the key, or the new node is closest and
					// we're second closest, then we take responsibility for storing
					// the value
				
				boolean	store_it	= false;
				
				if ( sorted_contacts.size() > 0 ){
					
					DHTTransportContact	first = (DHTTransportContact)sorted_contacts.get(0);
					
					if ( router.isID( first.getID())){
						
						store_it = true;
						
					}else if ( Arrays.equals( first.getID(), new_contact.getID()) && sorted_contacts.size() > 1 ){
						
						store_it = router.isID(((DHTTransportContact)sorted_contacts.get(1)).getID());
						
					}
				}
				
				if ( store_it ){
		
					values_to_store.add( value );
				}
			}
			
			if ( values_to_store.size() > 0 ){
				
				keys_to_store.put( key, values_to_store );
			}
		}
		
		if ( keys_to_store.size() > 0 ){
			
			it = keys_to_store.entrySet().iterator();
			
			DHTTransportContact	t_contact = ((DHTControlContactImpl)new_contact.getAttachment()).getContact();
	
			byte[][]				keys 		= new byte[keys_to_store.size()][];
			DHTTransportValue[][]	value_sets = new DHTTransportValue[keys.length][];
			
			int		index = 0;
			
			while( it.hasNext()){
		
				Map.Entry	entry = (Map.Entry)it.next();
				
				HashWrapper	key		= (HashWrapper)entry.getKey();
				
				List		values	= (List)entry.getValue();
		
				keys[index] 		= key.getHash();
				value_sets[index]	= new DHTTransportValue[values.size()];
				
				
				for (int i=0;i<values.size();i++){
					
					value_sets[index][i] = ((DHTDBValue)values.get(i)).getValueForRelay( local_contact );
				}
				
				index++;
			}
			
			t_contact.sendStore( 
					new DHTTransportReplyHandlerAdapter()
					{
						public void
						storeReply(
							DHTTransportContact _contact,
							byte[]				_diversifications )
						{
								// don't consider diversifications for node additions as they're not interested
								// in getting values from us, they need to get them from nodes 'near' to the 
								// diversification targets or the originator
							
							DHTLog.log( "add store ok" );
							
							router.contactAlive( _contact.getID(), new DHTControlContactImpl(_contact));
						}	
						
						public void
						failed(
							DHTTransportContact 	_contact,
							Throwable				_error )
						{
							DHTLog.log( "add store failed " + DHTLog.getString( _contact ) + " -> failed: " + _error.getMessage());
																	
							router.contactDead( _contact.getID(), new DHTControlContactImpl(_contact));
						}
					},
					keys, 
					value_sets );
						
			
		}else{
			
			if ( !new_contact.hasBeenAlive()){
				
				requestPing( new_contact );
			}
		}
	}
	
	protected Set
	getClosestContactsSet(
		byte[]		id,
		boolean		live_only )
	{
		List	l = router.findClosestContacts( id, live_only );
		
		Set	sorted_set	= new sortedContactSet( id, true ).getSet(); 

		for (int i=0;i<l.size();i++){
			
			sorted_set.add(((DHTControlContactImpl)((DHTRouterContact)l.get(i)).getAttachment()).getContact());
		}
		
		return( sorted_set );
	}
	
	public List
	getClosestKContactsList(
		byte[]		id,
		boolean		live_only )
	{
		Set	sorted_set	= getClosestContactsSet( id, live_only );
					
		List	res = new ArrayList(K);
		
		Iterator	it = sorted_set.iterator();
		
		while( it.hasNext() && res.size() < K ){
			
			res.add( it.next());
		}
		
		return( res );
	}
	
	protected byte[]
	encodeKey(
		byte[]		key )
	{
		byte[]	temp = new SHA1Hasher().calculateHash( key );
		
		byte[]	result =  new byte[node_id_byte_count];
		
		System.arraycopy( temp, 0, result, 0, node_id_byte_count );
		
		return( result );
	}
	
	public byte[]
	computeDistance(
		byte[]		n1,
		byte[]		n2 )
	{
		return( computeDistance2( n1, n2 ));
	}
	
	protected static byte[]
	computeDistance2(
		byte[]		n1,
		byte[]		n2 )
	{
		byte[]	res = new byte[n1.length];
		
		for (int i=0;i<res.length;i++){
			
			res[i] = (byte)( n1[i] ^ n2[i] );
		}
		
		return( res );
	}
	
		/**
		 * -ve -> n1 < n2
		 * @param n1
		 * @param n2
		 * @return
		 */
	
	public int
	compareDistances(
		byte[]		n1,
		byte[]		n2 )
	{
		return( compareDistances2( n1,n2 ));
	}
	
	protected static int
	compareDistances2(
		byte[]		n1,
		byte[]		n2 )
	{
		for (int i=0;i<n1.length;i++){
			
			int diff = (n1[i]&0xff) - (n2[i]&0xff);
			
			if ( diff != 0 ){
				
				return( diff );
			}
		}
		
		return( 0 );
	}
	
	public void
	addListener(
		DHTControlListener	l )
	{
		try{
			activity_mon.enter();
			
			listeners.addListener( l );
			
			for (int i=0;i<activities.size();i++){
				
				listeners.dispatch( DHTControlListener.CT_ADDED, activities.get(i));
			}
			
		}finally{
			
			activity_mon.exit();
		}
	}
	
	public void
	removeListener(
		DHTControlListener	l )
	{
		listeners.removeListener( l );	
	}
		
	public DHTControlActivity[]
	getActivities()
	{
		List	res;
		
		try{
			
			activity_mon.enter();
			
			res = new ArrayList( activities );
			
		}finally{
		
			activity_mon.exit();
		}
		
		DHTControlActivity[]	x = new DHTControlActivity[res.size()];
		
		res.toArray( x );
		
		return( x );
	}
	
	public long
	getEstimatedDHTSize()
	{
			// public method, trigger actual computation periodically
		
		long	now = SystemTime.getCurrentTime();
		
		long	diff = now - last_dht_estimate_time;
		
		if ( diff < 0 || diff > 60*1000 ){

			estimateDHTSize( router.getID(), null, router.getK());
		}
		
		return( dht_estimate );
	}
	
	protected void
	estimateDHTSize(
		byte[]	id,
		Map		contacts,
		int		contacts_to_use )
	{
			// if called with contacts then this is in internal estimation based on lookup values
		
		try{
			estimate_mon.enter();
		
			last_dht_estimate_time	= SystemTime.getCurrentTime();
		
			List	l;
			
			if ( contacts == null ){
				
				l = getClosestKContactsList( id, false );
				
			}else{
				
				Set	sorted_set	= new sortedContactSet( id, true ).getSet(); 
	
				sorted_set.addAll( contacts.values());
				
				l = new ArrayList( sorted_set );
				
				if ( l.size() > 0 ){
			
						// algorithm works relative to a starting point in the ID space so we grab
						// the first here rather than using the initial lookup target
					
					id = ((DHTTransportContact)l.get(0)).getID();
				}
				
				/*
				String	str = "";
				for (int i=0;i<l.size();i++){
					str += (i==0?"":",") + DHTLog.getString2( ((DHTTransportContact)l.get(i)).getID());
				}
				System.out.println( "XXX: " + str );
				*/
			}
			
				// can't estimate with less than 2
			
			if ( l.size() < 2 ){
				
				return;
			}
			
			/*
			<Gudy> if you call N0 yourself, N1 the nearest peer, N2 the 2nd nearest peer ... Np the pth nearest peer that you know (for example, N1 .. N20)
			<Gudy> and if you call D1 the Kad distance between you and N1, D2 between you and N2 ...
			<Gudy> then you have to compute :
			<Gudy> Dc = sum(i * Di) / sum( i * i)
			<Gudy> and then :
			<Gudy> NbPeers = 2^160 / Dc
			*/
			
			BigInteger	sum1 = new BigInteger("0");
			BigInteger	sum2 = new BigInteger("0");
			
				// first entry should be us
					
			for (int i=1;i<Math.min( l.size(), contacts_to_use );i++){
				
				DHTTransportContact	node = (DHTTransportContact)l.get(i);
				
				byte[]	dist = computeDistance( id, node.getID());
				
				BigInteger b_dist = IDToBigInteger( dist );
				
				BigInteger	b_i = new BigInteger(""+i);
				
				sum1 = sum1.add( b_i.multiply(b_dist));
				
				sum2 = sum2.add( b_i.multiply( b_i ));
			}
			
			byte[]	max = new byte[id.length+1];
			
			max[0] = 0x01;
			
			long this_estimate = IDToBigInteger(max).multiply( sum2 ).divide( sum1 ).longValue();
			
				// there's always us!!!!
			
			if ( this_estimate < 1 ){
				
				this_estimate	= 1;
			}
			
			estimate_values.put( new HashWrapper( id ), new Long( this_estimate ));
			
			long	new_estimate	= 0;
			
			Iterator	it = estimate_values.values().iterator();
			
			String	sizes = "";
				
			while( it.hasNext()){
				
				long	estimate = ((Long)it.next()).longValue();
				
				sizes += (sizes.length()==0?"":",") + estimate;
				
				new_estimate += estimate;
			}
			
			dht_estimate = new_estimate/estimate_values.size();
			
			// System.out.println( "getEstimatedDHTSize: " + sizes + "->" + dht_estimate + " (id=" + DHTLog.getString2(id) + ",cont=" + (contacts==null?"null":(""+contacts.size())) + ",use=" + contacts_to_use );
			
		}finally{
			
			estimate_mon.exit();
		}
	}
	
	protected BigInteger
	IDToBigInteger(
		byte[]		data )
	{
		String	str_key = "";
		
		for (int i=0;i<data.length;i++){
			
			String	hex = Integer.toHexString( data[i]&0xff );
			
			while( hex.length() < 2 ){
				
				hex = "0" + hex;
			}
				
			str_key += hex;
		}
				
		BigInteger	res		= new BigInteger( str_key, 16 );	
		
		return( res );
	}
	
	public void
	print()
	{
		logger.log( "DHT Details: external IP = " + transport.getLocalContact().getAddress());
		
		router.print();
		
		database.print();
	}
	
	
	
	protected class
	sortedContactSet
	{
		private TreeSet	tree_set;
		
		private byte[]	pivot;
		private boolean	ascending;
		
		protected
		sortedContactSet(
			byte[]		_pivot,
			boolean		_ascending )
		{
			pivot		= _pivot;
			ascending	= _ascending;
			
			tree_set = new TreeSet(
				new Comparator()
				{
					public int
					compare(
						Object	o1,
						Object	o2 )
					{
							// this comparator ensures that the closest to the key
							// is first in the iterator traversal
					
						DHTTransportContact	t1 = (DHTTransportContact)o1;
						DHTTransportContact t2 = (DHTTransportContact)o2;
						
						byte[] d1 = computeDistance( t1.getID(), pivot);
						byte[] d2 = computeDistance( t2.getID(), pivot);
						
						int	distance = compareDistances( d1, d2 );
						
						if ( ascending ){
							
							return( distance );
							
						}else{
							
							return( -distance );
						}
					}
				});
		}
		
		public Set
		getSet()
		{
			return( tree_set );
		}
	}
	
	class
	DHTOperationListenerDemuxer
		implements DHTOperationListener
	{
		private AEMonitor	this_mon = new AEMonitor( "DHTOperationListenerDemuxer" );
		
		private DHTOperationListener	delegate;
		
		private boolean		complete_fired;
		private boolean		complete_included_ok;
		
		private int			complete_count	= 0;
		
		protected
		DHTOperationListenerDemuxer(
			DHTOperationListener	_delegate )
		{
			delegate	= _delegate;
			
			if ( delegate == null ){
				
				Debug.out( "invalid: null delegate" );
			}
		}
		
		public void
		incrementCompletes()
		{
			try{
				this_mon.enter();
				
				complete_count++;
				
			}finally{
				
				this_mon.exit();
			}
		}
		
		public void
		searching(
			DHTTransportContact	contact,
			int					level,
			int					active_searches )
		{
			delegate.searching( contact, level, active_searches );
		}
		
		public void
		found(
			DHTTransportContact	contact )
		{
			delegate.found( contact );
		}
		
		public void
		read(
			DHTTransportContact	contact,
			DHTTransportValue	value )
		{
			delegate.read( contact, value );
		}
		
		public void
		wrote(
			DHTTransportContact	contact,
			DHTTransportValue	value )
		{
			delegate.wrote( contact, value );
		}
		
		public void
		complete(
			boolean				timeout )
		{
			boolean	fire	= false;
			
			try{
				this_mon.enter();
				
				if ( !timeout ){
					
					complete_included_ok	= true;
				}
				
				complete_count--;
				
				if (complete_count <= 0 && !complete_fired ){
					
					complete_fired	= true;
					fire			= true;
				}
			}finally{
				
				this_mon.exit();
			}
			
			if ( fire ){
				
				delegate.complete( !complete_included_ok );
			}
		}
	}
	
	abstract class
	lookupResultHandler
		extends DHTOperationListenerDemuxer
	{		
		protected
		lookupResultHandler(
			DHTOperationListener	delegate )
		{
			super( delegate );
		}
			
		public abstract void
		closest(
			List		res );
		
		public abstract void
		diversify(
			byte		diversification_type );
		
	}
	

	class
	DHTTransportFindValueReplyImpl
		implements DHTTransportFindValueReply
	{
		private byte					dt = DHT.DT_NONE;
		private DHTTransportValue[]		values;
		private DHTTransportContact[]	contacts;
		
		protected
		DHTTransportFindValueReplyImpl(
			byte				_dt,
			DHTTransportValue[]	_values )
		{
			dt		= _dt;
			values	= _values;
		}
		
		protected
		DHTTransportFindValueReplyImpl(
			DHTTransportContact[]	_contacts )
		{
			contacts	= _contacts;
		}
		
		public byte
		getDiversificationType()
		{
			return( dt );
		}
		
		public boolean
		hit()
		{
			return( values != null );
		}
		
		public DHTTransportValue[]
		getValues()
		{
			return( values );
		}
		
		public DHTTransportContact[]
		getContacts()
		{
			return( contacts );
		}
	}
	
	protected abstract class
	task
		extends ThreadPoolTask
	{
		private controlActivity	activity;
		
		protected 
		task(
			ThreadPool	thread_pool )
		{
			activity = new controlActivity( thread_pool, this );

			try{
				
				activity_mon.enter();
				
				activities.add( activity );
								
				listeners.dispatch( DHTControlListener.CT_ADDED, activity );

				// System.out.println( "activity added:" + activities.size());
				
			}finally{
			
				activity_mon.exit();
			}
		}
		
		public void
		taskStarted()
		{
			listeners.dispatch( DHTControlListener.CT_CHANGED, activity );
			
			//System.out.println( "activity changed:" + activities.size());
		}
		
		public void
		taskCompleted()
		{
			try{		
				activity_mon.enter();
				
				activities.remove( activity );

				listeners.dispatch( DHTControlListener.CT_REMOVED, activity );
			
				// System.out.println( "activity removed:" + activities.size());

			}finally{
				
				activity_mon.exit();
			}	
		}
		
		public void
		interruptTask()
		{
		}
		
		public abstract byte[]
		getTarget();
		
		public abstract String
		getDescription();
	}
	
	protected class
	controlActivity
		implements DHTControlActivity
	{
		protected ThreadPool	tp;
		protected task			task;
		protected int			type;
		
		protected
		controlActivity(
			ThreadPool	_tp,
			task		_task )
		{
			tp		= _tp;
			task	= _task;
			
			if ( _tp == internal_lookup_pool ){
				
				type	= DHTControlActivity.AT_INTERNAL_GET;
				
			}else if ( _tp == external_lookup_pool ){
				
				type	= DHTControlActivity.AT_EXTERNAL_GET;

			}else if ( _tp == internal_put_pool ){
				
				type	= DHTControlActivity.AT_INTERNAL_PUT;

			}else{

				type	= DHTControlActivity.AT_EXTERNAL_PUT;
			}
		}
		
		public byte[]
		getTarget()
		{
			return( task.getTarget());
		}
		
		public String
		getDescription()
		{
			return( task.getDescription());
		}
		
		public int
		getType()
		{
			return( type );
		}
		
		public boolean
		isQueued()
		{
			return( tp.isQueued( task ));
		}
		
		public String
		getString()
		{
			return( type + ":" + DHTLog.getString( getTarget()) + "/" + getDescription() + ", q = " + isQueued());
		}
	}
}
