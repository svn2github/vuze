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
import java.util.*;

import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.SHA1Hasher;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.Timer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.plugins.logging.LoggerChannel;

import com.aelitis.azureus.core.dht.impl.*;
import com.aelitis.azureus.core.dht.control.*;
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
	private DHTTransport	transport;
	private DHTRouter		router;
	
	private LoggerChannel	logger;
	
	private	int			node_id_byte_count;
	private int			search_concurrency;
	private int			cache_at_closest_n;
	private int			K;
	private int			B;
	private int			max_rep_per_node;
	
	private int			original_republish_interval;
	private int			ORIGINAL_REPUBLISH_INTERVAL_GRACE	= 120000;
	
	private int			cache_republish_interval;
	
	private long		max_values_stored;
	
	private Map			stored_values = new HashMap();
	
	private long		MIN_CACHE_EXPIRY_CHECK_INTERVAL		= 60000;
	private long		last_cache_expiry_check;
	
	public
	DHTControlImpl(
		DHTTransport	_transport,
		int				_K,
		int				_B,
		int				_max_rep_per_node,
		int				_search_concurrency,
		int				_original_republish_interval,
		int				_cache_republish_interval,
		int				_cache_at_closest_n,
		int				_max_values_stored,
		LoggerChannel	_logger )
	{
		transport	= _transport;
		logger		= _logger;
		
		K								= _K;
		B								= _B;
		max_rep_per_node				= _max_rep_per_node;
		search_concurrency				= _search_concurrency;
		original_republish_interval		= _original_republish_interval;
		cache_republish_interval		= _cache_republish_interval;
		cache_at_closest_n				= _cache_at_closest_n;
		max_values_stored				= _max_values_stored;
		
		createRouter( transport.getLocalContact());

		node_id_byte_count	= router.getID().length;
		
		transport.setRequestHandler( this );
	
		transport.addListener(
			new DHTTransportListener()
			{
				public void
				localContactChanged(
					DHTTransportContact	local_contact )
				{
					logger.log( "Transport ID changed, recreating router" );
					
					List	contacts = router.findBestContacts( 0 );
					
					DHTRouter	old_router = router;
					
					createRouter( local_contact );
				
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
			});
		
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
					
					try{		
						DHTLog.indent( router );

						republishOriginalMappings();
						
					}finally{
						
						DHTLog.exdent();
					}
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
						
						try{		
							DHTLog.indent( router );

							republishCachedMappings();
							
						}finally{
							
							DHTLog.exdent();
						}						
					}
				});
	}
	
	protected void
	createRouter(
		DHTTransportContact		local_contact)
	{		
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
					byte[]	id )
				{
					lookup( id, false, 0 );
				}
				
				public void
				requestAdd(
					DHTRouterContact	contact )
				{
					nodeAddedToRouter( contact );
				}
			});	
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
	
	public void
	contactImported(
		DHTTransportContact	contact )
	{
		try{
			DHTLog.indent( router );

			byte[]	id = contact.getID();
		
			router.contactKnown( id, new DHTControlContactImpl(contact));
				
		}finally{
			
			DHTLog.exdent();
		}
	}
	
	public void
	exportState(
		DataOutputStream	daos,
		int					max )
	
		throws IOException
	{
		List	contacts = router.findBestContacts( max );
				
		daos.writeInt( contacts.size());
				
		for (int i=0;i<contacts.size();i++){
			
			DHTRouterContact	contact = (DHTRouterContact)contacts.get(i);
			
			System.out.println( "export:" + contact.getString());
			
			DHTTransportContact	t_contact = ((DHTControlContactImpl)contact.getAttachment()).getContact();
			
			t_contact.exportContact( daos );
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
			
			transport.importContact( dais );
		}
	}
	
	public void
	seed()
	{
		lookup( router.getID(), false, 0 );
		
		router.seed();
	}
	
	public void
	put(
		final byte[]		_unencoded_key,
		final byte[]		_value )
	{
		try{
			DHTLog.indent( router );
			
			byte[]	encoded_key = encodeKey( _unencoded_key );
			
			DHTLog.log( "put for " + DHTLog.getString( encoded_key ));
			
			DHTTransportValue	value = 
				new DHTControlValueImpl( SystemTime.getCurrentTime(), _value, 0 );

			synchronized( stored_values ){
					
					// don't police max check for locally stored data
					// only that received
				
				stored_values.put( new HashWrapper( encoded_key ), value );
			}
			
			putSupport( encoded_key, value, 0 );		

		}finally{
			
			DHTLog.exdent();
		}
	}

	protected void
	putSupport(
		final byte[]		encoded_key,
		DHTTransportValue	value,
		long				timeout )
	{
		List	closest = lookup( encoded_key, false, timeout  );
		
		putSupport( encoded_key, value, closest );			
	}
	
	protected void
	putSupport(
		final byte[]		encoded_key,
		DHTTransportValue	value,
		List				closest )
	{
		for (int i=0;i<closest.size();i++){
		
			DHTTransportContact	contact = (DHTTransportContact)closest.get(i);
			
			if ( !router.isID( contact.getID())){
									
				contact.sendStore( 
					new DHTTransportReplyHandlerAdapter()
					{
						public void
						storeReply(
							DHTTransportContact _contact )
						{
							DHTLog.indent( router );
							
							DHTLog.log( "store ok" );
							
							router.contactAlive( _contact.getID(), new DHTControlContactImpl(_contact));
							
							DHTLog.exdent();
						}	
						
						public void
						failed(
							DHTTransportContact 	_contact )
						{
							DHTLog.indent( router );
							
							DHTLog.log( "store failed" );
							
							router.contactDead( _contact.getID(), new DHTControlContactImpl(_contact));
							
							DHTLog.exdent();
						}
					},
					encoded_key, 
					value );
			}
		}
	}
	
	public byte[]
	get(
		byte[]		unencoded_key,
		long		timeout )
	{
		try{		
			DHTLog.indent( router );
			
			final byte[]	encoded_key = encodeKey( unencoded_key );

			DHTLog.log( "get for " + DHTLog.getString( encoded_key ));

				// maybe we've got the value already
			
			synchronized( stored_values ){
				
				DHTTransportValue	local_result = (DHTTransportValue)stored_values.get( new HashWrapper( encoded_key ));
			
				if ( local_result != null ){
					
					DHTLog.log( "    surprisingly we've got it locally!" );
	
					return( local_result.getValue());
				}
			}
			
			List	result_and_closest = lookup( encoded_key, true, timeout );
	
			DHTTransportValue	value = (DHTTransportValue)result_and_closest.get(0);
			
			if ( value != null ){
				
					// cache the value at the 'n' closest seen locations
				
				for (int i=1;i<Math.min(1+cache_at_closest_n,result_and_closest.size());i++){
					
					((DHTTransportContact)result_and_closest.get(i)).sendStore( 
							new DHTTransportReplyHandlerAdapter()
							{
								public void
								storeReply(
									DHTTransportContact contact )
								{
									DHTLog.indent( router );
									
									DHTLog.log( "cache store ok" );
									
									router.contactAlive( contact.getID(), new DHTControlContactImpl(contact));
									
									DHTLog.exdent();
								}	
								
								public void
								failed(
									DHTTransportContact 	contact )
								{
									DHTLog.indent( router );
									
									DHTLog.log( "cache store failed" );
									
									router.contactDead( contact.getID(), new DHTControlContactImpl(contact));
									
									DHTLog.exdent();
								}
							},
							encoded_key, 
							value );
				}
			}
			
			DHTLog.log( "get reply: " + DHTLog.getString( value ));
			
			return( value==null?null:value.getValue());
			
		}finally{
			
			DHTLog.exdent();
		}
	}
	
	public byte[]
	remove(
		byte[]		unencoded_key )
	{
			// TODO: push the deletion out rather than letting values timeout
		
		try{		
			DHTLog.indent( router );
			
			final byte[]	encoded_key = encodeKey( unencoded_key );

			DHTLog.log( "remove for " + DHTLog.getString( encoded_key ));

				// maybe we've got the value already
				
			synchronized( stored_values ){
			
				DHTTransportValue val = (DHTTransportValue)stored_values.remove( new HashWrapper( encoded_key ));
				
				if ( val == null ){
					
					return( null );
				}
				
				return( val.getValue());
			}
		}finally{
			
			DHTLog.exdent();
		}
	}
	
		/**
		 * The lookup method returns up to K closest nodes to the target
		 * @param lookup_id
		 * @return
		 */
	
	protected List
	lookup(
		final byte[]	lookup_id,
		boolean			value_search,
		long			timeout )
	{
		try{		
			DHTLog.indent( router );
			
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

			final Set	contacts_to_query	= getClosestContactsSet( lookup_id ); 

				// record the set of contacts we've queried to avoid re-queries
			
			final Map			contacts_queried = new HashMap();
			
				// record the set of contacts that we've had a reply from
				// furthest away at front
			
			final Set			ok_contacts = new sortedContactSet( lookup_id, false ).getSet(); 
			

				// this handles the search concurrency
			
			final AESemaphore	search_sem = new AESemaphore( "DHTControl:search", search_concurrency );
				
			final int[]	idle_searches	= { 0 };
			final int[]	active_searches	= { 0 };
			
			final DHTTransportValue[]	value_search_result = {null};
			
			long	start = SystemTime.getCurrentTime();

			while( true ){
				
				if ( timeout > 0 ){
					
					long	now = SystemTime.getCurrentTime();
					
					long remaining = timeout - ( now - start );
						
					if ( remaining <= 0 ){
						
						break;
						
					}
						// get permission to kick off another search
					
					if ( !search_sem.reserve( remaining )){
						
						break;
					}
				}else{
					
					search_sem.reserve();
				}

				if ( value_search_result[0] != null ){
					
					break;
				}
				
				synchronized( contacts_to_query ){
			
						// if nothing pending then we need to wait for the results of a previous
						// search to arrive. Of course, if there are no searches active then
						// we've run out of things to do
					
					if ( contacts_to_query.size() == 0 ){
						
						if ( active_searches[0] == 0 ){
							
							DHTLog.log( "lookup: terminates as no contacts left to query" );
							
							break;
						}
						
						idle_searches[0]++;
						
						continue;
					}
				
						// select the next contact to search
					
					DHTTransportContact	closest	= (DHTTransportContact)contacts_to_query.iterator().next();			
				
						// if the next closest is further away than the furthest successful hit so 
						// far and we have K hits, we're done
					
					if ( ok_contacts.size() == router.getK()){
						
						DHTTransportContact	furthest_ok = (DHTTransportContact)ok_contacts.iterator().next();
						
						byte[]	furthest_ok_distance 	= computeDistance( furthest_ok.getID(), lookup_id );
						byte[]	closest_distance		= computeDistance( closest.getID(), lookup_id );
						
						if ( compareDistances( furthest_ok_distance, closest_distance) <= 0 ){
							
							DHTLog.log( "lookup: terminates as we've searched the closest K contacts" );
	
							break;
						}
					}
					
					contacts_to_query.remove( closest );

					contacts_queried.put( new HashWrapper( closest.getID()), "" );
								
						// never search ourselves!
					
					if ( router.isID( closest.getID())){
						
						search_sem.release();
						
						continue;
					}

					active_searches[0]++;
					
					DHTTransportReplyHandlerAdapter	handler = 
						new DHTTransportReplyHandlerAdapter()
						{
							public void
							findNodeReply(
								DHTTransportContact 	target_contact,
								DHTTransportContact[]	reply_contacts )
							{
								try{
									DHTLog.indent( router );
									
									DHTLog.log( "findNodeReply: " + DHTLog.getString( reply_contacts ));
							
									router.contactAlive( target_contact.getID(), new DHTControlContactImpl(target_contact));
									
									synchronized( contacts_to_query ){
												
										ok_contacts.add( target_contact );
										
										if ( ok_contacts.size() > router.getK()){
											
												// delete the furthest away
											
											Iterator it = ok_contacts.iterator();
											
											it.next();
											
											it.remove();
										}
										
										for (int i=0;i<reply_contacts.length;i++){
											
											DHTTransportContact	contact = reply_contacts[i];
											
												// ignore responses that are ourselves
											
											if ( compareDistances( router.getID(), contact.getID()) == 0 ){
												
												continue;
											}
											
												// dunno if its alive or not, however record its existance
											
											router.contactKnown( contact.getID(), new DHTControlContactImpl(contact));
											
											if (	contacts_queried.get( new HashWrapper( contact.getID())) == null &&
													!contacts_to_query.contains( contact )){
												
												DHTLog.log( "    new contact for query: " + DHTLog.getString( contact ));
												
												contacts_to_query.add( contact );
												
												if ( idle_searches[0] > 0 ){
													
													idle_searches[0]--;
													
													search_sem.release();
												}
											}else{
												
												// DHTLog.log( "    already queried: " + DHTLog.getString( contact ));
											}
										}
									}
								}finally{
									
									active_searches[0]--;								
		
									search_sem.release();
									
									DHTLog.exdent();
								}
							}
							
							public void
							findValueReply(
								DHTTransportContact 	contact,
								DHTTransportValue		value )
							{
								try{
									DHTLog.indent( router );
									
									DHTLog.log( "findValueReply: " + DHTLog.getString( value ));
									
									router.contactAlive( contact.getID(), new DHTControlContactImpl(contact));
									
									value_search_result[0]	= value;
		
								}finally{
														
									active_searches[0]--;
									
									search_sem.release();
									
									DHTLog.exdent();
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
								DHTTransportContact 	target_contact )
							{
								try{
									DHTLog.indent( router );
									
									DHTLog.log( "Reply: findNode/findValue -> failed" );
									
									router.contactDead( target_contact.getID(), new DHTControlContactImpl(target_contact));
		
								}finally{
																	
									active_searches[0]--;
									
									search_sem.release();
									
									DHTLog.exdent();
								}
							}
						};
						
					router.recordLookup( lookup_id );
					
					if ( value_search ){
						
						closest.sendFindValue( handler, lookup_id );
						
					}else{
						
						closest.sendFindNode( handler, lookup_id );
					}
				}
			}
			
				// maybe unterminated searches still going on so protect ourselves
				// against concurrent modification of result set
			
			synchronized( contacts_to_query ){

				DHTLog.log( "lookup complete for " + DHTLog.getString( lookup_id ));
				
				DHTLog.log( "    queried = " + DHTLog.getString( contacts_queried ));
				DHTLog.log( "    to query = " + DHTLog.getString( contacts_to_query ));
				DHTLog.log( "    ok = " + DHTLog.getString( ok_contacts ));
				
				ArrayList	res;
				
				if ( value_search ){
					
					res = new ArrayList( ok_contacts.size() + 1 );
					
					res.add( value_search_result[0]);
					
					res.addAll( ok_contacts );
				}else{
					
					res = new ArrayList( ok_contacts );
				}
				
				return( res );
			}
			
		}finally{
			
			DHTLog.exdent();
		}
	}
	
	
		// Request methods
	
	public void
	pingRequest(
		DHTTransportContact originating_contact )
	{
		try{		
			DHTLog.indent( router );

			DHTLog.log( "pingRequest from " + DHTLog.getString( originating_contact.getID()));
			
			router.contactAlive( originating_contact.getID(), new DHTControlContactImpl(originating_contact));
			
		}finally{
			
			DHTLog.exdent();
		}
	}
		
	public void
	storeRequest(
		DHTTransportContact 	originating_contact, 
		byte[]					key,
		final DHTTransportValue	value )
	{
		try{		
			DHTLog.indent( router );

			DHTLog.log( "storeRequest from " + DHTLog.getString( originating_contact.getID()));

			router.contactAlive( originating_contact.getID(), new DHTControlContactImpl(originating_contact));
	
			synchronized( stored_values ){
				
				if ( stored_values.size() >= max_values_stored ){
					
						// just drop it
					
					DHTLog.log( "Max entries exceeded" );
					
				}else{
					
					checkCacheExpiration( false );
					
						// don't replace a closer cache value with a further away one. in particular
						// we have to avoid the case where the original publisher of a key happens to
						// be close to it and be asked by another node to cache it!
					
					HashWrapper	wrapper = new HashWrapper( key );
					
					DHTControlValueImpl	existing = (DHTControlValueImpl)stored_values.get( wrapper );
					
					if ( existing == null || existing.getCacheDistance() > value.getCacheDistance() + 1 ){
						
						stored_values.put(
								wrapper, 
								new DHTControlValueImpl( value, 1 ));
					}else{
						
							// mark it as current 
						
						existing.reset();
					}
				}
			}
			
		}finally{
			
			DHTLog.exdent();
		}
	}
	
	public DHTTransportContact[]
	findNodeRequest(
		DHTTransportContact originating_contact, 
		byte[]				id )
	{
		try{		
			DHTLog.indent( router );

			DHTLog.log( "findNodeRequest from " + DHTLog.getString( originating_contact.getID()));

			router.contactAlive( originating_contact.getID(), new DHTControlContactImpl(originating_contact));
			
			List	l = getClosestKContactsList( id );
			
			DHTTransportContact[]	res = new DHTTransportContact[l.size()];
			
			l.toArray( res );
			
			return( res );
			
		}finally{
			
			DHTLog.exdent();
		}
	}
	
	public Object
	findValueRequest(
		DHTTransportContact originating_contact, 
		byte[]				key )
	{
		try{		
			DHTLog.indent( router );

			DHTLog.log( "findValueRequest from " + DHTLog.getString( originating_contact.getID()));

			DHTTransportValue	value;
			
			synchronized( stored_values ){
				
				checkCacheExpiration( false );
				
				value = (DHTTransportValue)stored_values.get( new HashWrapper( key ));
			}
			
			if ( value != null ){
				
				router.contactAlive( originating_contact.getID(), new DHTControlContactImpl(originating_contact));
	
				return( value );
				
			}else{
				
				return( findNodeRequest( originating_contact, key ));
			}
	
		}finally{
			
			DHTLog.exdent();
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
			
			DHTControlValueImpl	value	= (DHTControlValueImpl)entry.getValue();
			
				// we're republising the data, reset the creation time
			
			value.setCreationTime();
			
			putSupport( key.getHash(), value, 0 );
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
				
				DHTControlValueImpl	value	= (DHTControlValueImpl)entry.getValue();
				
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
			
			while( it.hasNext()){
				
				Map.Entry	entry = (Map.Entry)it.next();
				
				HashWrapper			key		= (HashWrapper)entry.getKey();
				
				DHTControlValueImpl	value	= (DHTControlValueImpl)entry.getValue();
				
				byte[]	lookup_id	= key.getHash();
				
				List	contacts = getClosestKContactsList( lookup_id );
							
					// we reduce the cache distance by 1 here as it is incremented by the
					// recipients
				
				putSupport( 
						lookup_id, 
						new DHTControlValueImpl(value,-1), 
						contacts );
			}
		}
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
						DHTLog.indent( router );
						
						DHTLog.log( "ping ok" );
						
						router.contactAlive( _contact.getID(), new DHTControlContactImpl(_contact));
						
						DHTLog.exdent();
					}	
					
					public void
					failed(
						DHTTransportContact 	_contact )
					{
						DHTLog.indent( router );
						
						DHTLog.log( "ping failed" );
						
						router.contactDead( _contact.getID(), new DHTControlContactImpl(_contact));
	
						DHTLog.exdent();
					}
				});
	}
	
	protected void
	nodeAddedToRouter(
		DHTRouterContact	new_contact )
	{		
		// when a new node is added we must check to see if we need to transfer
		// any of our values to it.
		
		Map	values_to_store	= new HashMap();
		
		synchronized( stored_values ){
			
			if ( stored_values.size() == 0 ){
				
					// nothing to do, ping it if it isn't known to be alive
				
				if ( !new_contact.hasBeenAlive()){
					
					requestPing( new_contact );
				}
				
				return;
			}
			
				// see if we're one of the K closest to the new node
			
			List	closest_contacts = getClosestKContactsList( new_contact.getID());
			
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
			
			Iterator	it = stored_values.entrySet().iterator();
			
			while( it.hasNext()){
				
				Map.Entry	entry = (Map.Entry)it.next();
				
				HashWrapper	key		= (HashWrapper)entry.getKey();
				
				byte[]	encoded_key		= key.getHash();
				
				DHTControlValueImpl	value	= (DHTControlValueImpl)entry.getValue();
				
					// we neither consider the node's originating values, nor any cached
					// further away than the initial location, for transfer
				
				if ( value.getCacheDistance() != 1 ){
					
					continue;
				}
				
				List		sorted_contacts	= getClosestKContactsList( encoded_key ); 
				
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
		
					values_to_store.put( key, value );
				}
			}
		}
		
		if ( values_to_store.size() > 0 ){
			
			Iterator	it = values_to_store.entrySet().iterator();
			
			DHTTransportContact	t_contact = ((DHTControlContactImpl)new_contact.getAttachment()).getContact();
	
			boolean	first_value	= true;
			
			while( it.hasNext()){
				
				Map.Entry	entry = (Map.Entry)it.next();
				
				HashWrapper	key		= (HashWrapper)entry.getKey();
				
				DHTControlValueImpl	value	= (DHTControlValueImpl)entry.getValue();
					
				final boolean	ping_replacement = 
					first_value && !new_contact.hasBeenAlive();
				
				first_value	= false;
				
				t_contact.sendStore( 
						new DHTTransportReplyHandlerAdapter()
						{
							public void
							storeReply(
								DHTTransportContact _contact )
							{
								DHTLog.indent( router );
								
								DHTLog.log( "add store ok" );
								
								router.contactAlive( _contact.getID(), new DHTControlContactImpl(_contact));
								
								DHTLog.exdent();
							}	
							
							public void
							failed(
								DHTTransportContact 	_contact )
							{
									// we ignore failures when propagating values as there might be
									// a lot to propagate and one failure would remove the newly added
									// node. given that the node has just appeared, and can therefore be
									// assumed to be alive, this is acceptable behaviour. 
									// If we don't do this then we can get a node appearing and disappearing
									// and taking up loads of resources
								
									// however, for contacts that aren't known to be alive
									// we use one of the stores in place of a ping to ascertain liveness
								
								if ( ping_replacement ){
									
									DHTLog.indent( router );
									
									DHTLog.log( "add store failed" );
									
									router.contactDead( _contact.getID(), new DHTControlContactImpl(_contact));
									
									DHTLog.exdent();
								}
							}
						},
						key.getHash(), 
						new DHTControlValueImpl( value, -1 ));
						
			}
		}else{
			
			if ( !new_contact.hasBeenAlive()){
				
				requestPing( new_contact );
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
			
			DHTControlValueImpl	value = (DHTControlValueImpl)it.next();
			
			int	distance = value.getCacheDistance();
			
				// distance = 0 are explicitly published and need to be explicitly removed
			
			if ( distance == 1 ){
				
				if ( now - value.getCreationTime() > original_republish_interval + ORIGINAL_REPUBLISH_INTERVAL_GRACE ){
					
					DHTLog.log( "removing cache entry at level " + distance );
					
					it.remove();
				}
				
			}else if ( distance > 1 ){
				
					// distance 2 get 1/2 time, 3 get 1/4 etc.
				
				long	permitted = cache_republish_interval >> (distance-1);
				
				if ( now - value.getStoreTime() >= permitted ){
					
					DHTLog.log( "removing cache entry at level " + distance );
					
					it.remove();
				}
			}
		}
	}
	
	public void
	print()
	{
		router.print();
		
		Map	count = new TreeMap();
		
		synchronized( stored_values ){
			
			DHTLog.log( "Stored values = " + stored_values.size()); 

			Iterator	it = stored_values.values().iterator();
			
			while( it.hasNext()){
				
				DHTTransportValue	value = (DHTTransportValue)it.next();
				
				Integer key = new Integer( value.getCacheDistance());
				
				Integer	i = (Integer)count.get( key );
				
				if ( i == null ){
					
					i = new Integer(1);
					
				}else{
					
					i = new Integer(i.intValue() + 1 );
				}
				
				count.put( key, i );
			}
		}
				
		Iterator	it = count.keySet().iterator();
		
		while( it.hasNext()){
			
			Integer	k = (Integer)it.next();
			
			DHTLog.log( "    " + k + " -> " + count.get(k));
		}
	}
	
	protected Set
	getClosestContactsSet(
		byte[]	id )
	{
		List	l = router.findClosestContacts( id );
		
		Set	sorted_set	= new sortedContactSet( id, true ).getSet(); 

		for (int i=0;i<l.size();i++){
			
			sorted_set.add(((DHTControlContactImpl)((DHTRouterContact)l.get(i)).getAttachment()).getContact());
		}
		
		return( sorted_set );
	}
	
	protected List
	getClosestKContactsList(
		byte[]	id )
	{
		Set	sorted_set	= getClosestContactsSet(id);
			
		int	K = router.getK();
		
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
	
	protected static byte[]
	computeDistance(
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
	
	protected static int
	compareDistances(
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
	
	protected String
	bytesToString(
		byte[]	b )
	{
		return( ByteFormatter.nicePrint( b ));
	}
	
	protected String
	getIndent()
	{
		return( "" );
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
}
