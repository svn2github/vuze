/*
 * Created on Sep 10, 2014
 * Created by Paul Gardner
 * 
 * Copyright 2014 Azureus Software, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or 
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.core.content;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.AddressUtils;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.ByteArrayHashMap;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.plugins.ddb.DistributedDatabase;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseContact;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseException;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseKey;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseProgressListener;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseTransferHandler;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseTransferType;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseValue;
import org.gudy.azureus2.plugins.utils.search.SearchException;
import org.gudy.azureus2.plugins.utils.search.SearchInstance;
import org.gudy.azureus2.plugins.utils.search.SearchObserver;
import org.gudy.azureus2.plugins.utils.search.SearchProvider;
import org.gudy.azureus2.plugins.utils.search.SearchResult;
import org.gudy.azureus2.pluginsimpl.local.ddb.DDBaseImpl;

import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.content.RelatedContentManager.ContentCache;
import com.aelitis.azureus.core.content.RelatedContentManager.DownloadInfo;
import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.transport.udp.DHTTransportUDP;
import com.aelitis.azureus.core.util.RegExUtil;
import com.aelitis.azureus.core.util.bloom.BloomFilter;
import com.aelitis.azureus.core.util.bloom.BloomFilterFactory;
import com.aelitis.azureus.plugins.dht.DHTPluginContact;
import com.aelitis.azureus.plugins.dht.DHTPluginInterface;
import com.aelitis.azureus.plugins.dht.DHTPluginInterface.DHTInterface;
import com.aelitis.azureus.plugins.dht.DHTPluginValue;
import com.aelitis.azureus.util.ImportExportUtils;

public class 
RelatedContentSearcher 
	implements DistributedDatabaseTransferHandler
{
	private static final boolean	SEARCH_CVS_ONLY		= Constants.isCurrentVersionLT( "4.7.0.4" );
	private static final boolean	TRACE_SEARCH		= false;

	private static final int	MAX_REMOTE_SEARCH_RESULTS		= 30;
	private static final int	MAX_REMOTE_SEARCH_CONTACTS		= 50;
	private static final int	MAX_REMOTE_SEARCH_MILLIS		= 25*1000;
	private static final int	REDUCED_REMOTE_SEARCH_MILLIS	= 10*1000;

	private static final int	HARVEST_MAX_BLOOMS				= 50;
	private static final int	HARVEST_MAX_FAILS_HISTORY		= 128;
	private static final int	HARVEST_BLOOM_UPDATE_MILLIS		= 15*60*1000;
	private static final int	HARVEST_BLOOM_DISCARD_MILLIS	= 60*60*1000;
	private static final int 	HARVEST_BLOOM_OP_RESET_MILLIS	= 5*60*1000;
	private static final int 	HARVEST_BLOOM_OP_RESET_TICKS	= HARVEST_BLOOM_OP_RESET_MILLIS/RelatedContentManager.TIMER_PERIOD;
	private static final int 	HARVEST_BLOOM_SE_RESET_MILLIS	= 1*60*1000;
	private static final int 	HARVEST_BLOOM_SE_RESET_TICKS	= HARVEST_BLOOM_SE_RESET_MILLIS/RelatedContentManager.TIMER_PERIOD;

	private static final int KEY_BLOOM_LOAD_FACTOR			= 8;
	private static final int KEY_BLOOM_MIN_BITS				= 1000;	
	private static final int KEY_BLOOM_MAX_BITS				= 50000;	// 6k ish
	private static final int KEY_BLOOM_MAX_ENTRIES			= KEY_BLOOM_MAX_BITS/KEY_BLOOM_LOAD_FACTOR;

	private volatile BloomFilter	key_bloom_with_local;
	private volatile BloomFilter	key_bloom_without_local;
	private volatile long			last_key_bloom_update = -1;
	
	private Set<String>	ignore_words = new HashSet<String>();
	
	{
		String ignore = "a, in, of, at, the, and, or, if, to, an, for, with";
		
		String[]	lame_entries = ignore.toLowerCase( Locale.US ).split(",");
		
		for ( String entry: lame_entries ){
		
			entry = entry.trim();
			
			if ( entry.length() > 0 ){
				
				ignore_words.add( entry );
			}
		}
	}
	
	
	
	private ByteArrayHashMap<ForeignBloom>		harvested_blooms 	= new ByteArrayHashMap<ForeignBloom>();
	private ByteArrayHashMap<String>			harvested_fails 	= new ByteArrayHashMap<String>();
	
	private volatile BloomFilter harvest_op_requester_bloom = BloomFilterFactory.createAddOnly( 2048 );
	private volatile BloomFilter harvest_se_requester_bloom = BloomFilterFactory.createAddRemove4Bit( 512 );

	private final AsyncDispatcher	harvest_dispatcher			= new AsyncDispatcher();

	private final RelatedContentManager					manager;
	private final DistributedDatabaseTransferType		transfer_type;
	private final DHTPluginInterface					dht_plugin;
	
	private DistributedDatabase				ddb;

	protected
	RelatedContentSearcher(
		RelatedContentManager				_manager,
		DistributedDatabaseTransferType		_transfer_type,
		DHTPluginInterface					_dht_plugin,
		boolean								_defer_ddb_check )
	{
		manager			= _manager;
		transfer_type	= _transfer_type;
		dht_plugin 		= _dht_plugin;
		
		if ( !_defer_ddb_check ){
			
			checkDDB();
		}
	}
	
	protected DHTPluginInterface
	getDHTPlugin()
	{
		return( dht_plugin );
	}
	
	protected void
	timerTick(
		boolean		enabled,
		int			tick_count )
	{			
		checkDDB();
		
		if ( enabled ){
							
			harvestBlooms();
			
			if ( tick_count % HARVEST_BLOOM_SE_RESET_TICKS == 0 ){
				
				harvest_se_requester_bloom = harvest_se_requester_bloom.getReplica();
			}
			
			if ( tick_count % HARVEST_BLOOM_OP_RESET_TICKS == 0 ){
				
				harvest_op_requester_bloom = harvest_op_requester_bloom.getReplica();
			}									
		}
		
		checkKeyBloom();
		
		testKeyBloom();
	}
	
	private void
	checkDDB()
	{
		if ( ddb == null ){
			
			try{
				List<DistributedDatabase> ddbs = DDBaseImpl.getDDBs( new String[]{ dht_plugin.getNetwork() });
			
				if ( ddbs.size() > 0 ){
				
					ddb = ddbs.get( 0 );
					
					ddb.addTransferHandler( transfer_type, this );
				}
			}catch( Throwable e ){
				
				// Debug.out( e );
			}
		}
	}
	
	protected SearchInstance
	searchRCM(
		Map<String,Object>		search_parameters,
		SearchObserver			_observer )
	
		throws SearchException
	{				
		final MySearchObserver observer = new MySearchObserver( _observer );
		
		final String	term = fixupTerm( (String)search_parameters.get( SearchProvider.SP_SEARCH_TERM ));
		
		final SearchInstance si = 
			new SearchInstance()
			{
				public void
				cancel()
			{
					Debug.out( "Cancelled" );
				}
			};
			
		if ( term == null ){
		
			observer.complete();
			
		}else{
		
			new AEThread2( "RCM:search", true )
			{
				public void
				run()
				{
					final Set<String>	hashes_sync_me = new HashSet<String>();
					
					try{				
						List<RelatedContent>	matches = matchContent( term );
							
						for ( final RelatedContent c: matches ){
							
							final byte[] hash = c.getHash();
							
							if ( hash == null ){
								
								continue;
							}
							
							hashes_sync_me.add( Base32.encode( hash ));
							
							SearchResult result = 
								new SearchResult()
								{
									public Object
									getProperty(
										int		property_name )
									{
										if ( property_name == SearchResult.PR_NAME ){
											
											return( c.getTitle());
											
										}else if ( property_name == SearchResult.PR_SIZE ){
											
											return( c.getSize());
											
										}else if ( property_name == SearchResult.PR_HASH ){
											
											return( hash );
											
										}else if ( property_name == SearchResult.PR_RANK ){
											
												// this rank isn't that accurate, scale down
											
											return( new Long( c.getRank() / 4 ));
											
										}else if ( property_name == SearchResult.PR_SEED_COUNT ){
											
											return( new Long( c.getSeeds()));
											
										}else if ( property_name == SearchResult.PR_LEECHER_COUNT ){
											
											return( new Long( c.getLeechers()));
											
										}else if ( property_name == SearchResult.PR_SUPER_SEED_COUNT ){
											
											if ( c.getContentNetwork() != ContentNetwork.CONTENT_NETWORK_UNKNOWN ){
												
												return( new Long( 1 ));
												
											}else{
												
												return( new Long( 0 ));
											}
										}else if ( property_name == SearchResult.PR_PUB_DATE ){
												
											long	date = c.getPublishDate();
											
											if ( date <= 0 ){
												
												return( null );
											}
											
											return( new Date( date ));
											
										}else if ( 	property_name == SearchResult.PR_DOWNLOAD_LINK ||
													property_name == SearchResult.PR_DOWNLOAD_BUTTON_LINK ){
											
											byte[] hash = c.getHash();
											
											if ( hash != null ){
												
												return( UrlUtils.getMagnetURI( hash, c.getTitle(), c.getNetworks()));
											}
										}else if ( property_name == RelatedContentManager.RCM_SEARCH_PROPERTY_CONTENT_NETWORK ){
											
											return( c.getContentNetwork());
											
										}else if ( property_name == RelatedContentManager.RCM_SEARCH_PROPERTY_TRACKER_KEYS ){
											
											return( c.getTrackerKeys());
											
										}else if ( property_name == RelatedContentManager.RCM_SEARCH_PROPERTY_WEB_SEED_KEYS ){
											
											return( c.getWebSeedKeys());
											
										}else if ( property_name == RelatedContentManager.RCM_SEARCH_PROPERTY_TAGS ){

											return( c.getTags());
											
										}else if ( property_name == RelatedContentManager.RCM_SEARCH_PROPERTY_NETWORKS ){

											return( c.getNetworks());
										}
										
										return( null );
									}
								};
								
							observer.resultReceived( si, result );
						}
					}finally{
						
						try{	
							final List<DistributedDatabaseContact> 	initial_hinted_contacts = searchForeignBlooms( term );
							final Set<DistributedDatabaseContact>	extra_hinted_contacts	= new HashSet<DistributedDatabaseContact>();
							
							Collections.shuffle( initial_hinted_contacts );
							
							// test injection of local 
							// hinted_contacts.add( 0, ddb.getLocalContact());
							
							final LinkedList<DistributedDatabaseContact>	contacts_to_search = new LinkedList<DistributedDatabaseContact>();

							final Map<InetSocketAddress,DistributedDatabaseContact> contact_map = new HashMap<InetSocketAddress, DistributedDatabaseContact>();
														
							for ( DistributedDatabaseContact c: initial_hinted_contacts ){
								
									// stick in map so non-hinted get removed below, but interleave later
								
								contact_map.put( c.getAddress(), c );
							}
							
								// ddb might be null during init
							
							if ( ddb != null ){
								
								DHTInterface[]	dhts = dht_plugin.getDHTInterfaces();
									
								for ( DHTInterface dht: dhts ){
													
									if ( dht.isIPV6()){
										
										continue;
									}
									
									int	network = dht.getNetwork();
									
									if ( SEARCH_CVS_ONLY && network != DHT.NW_CVS ){
										
										logSearch( "Search: ignoring main DHT" );
	
										continue;
									}
									
									DHTPluginContact[] contacts = dht.getReachableContacts();
									
									Collections.shuffle( Arrays.asList( contacts ));
																	
									for ( DHTPluginContact dc: contacts ){
												
										InetSocketAddress address = dc.getAddress();
										
										if ( !contact_map.containsKey( address )){
											
											try{
												DistributedDatabaseContact c = importContact( dc, network );
												
												contact_map.put( address, c );
												
												contacts_to_search.add( c );
												
											}catch( Throwable e ){
												
											}
										}
									}
								}
								
								if ( contact_map.size() < MAX_REMOTE_SEARCH_CONTACTS ){
									
										// back fill with less reliable contacts if required
									
									for ( DHTInterface dht: dhts ){
																				
										if ( dht.isIPV6()){
											
											continue;
										}
										
										int	network = dht.getNetwork();
										
										if ( SEARCH_CVS_ONLY && network != DHT.NW_CVS ){
											
											logSearch( "Search: ignoring main DHT" );
	
											continue;
										}
										
										DHTPluginContact[] contacts = dht.getRecentContacts();
		
										for ( DHTPluginContact dc: contacts ){
											
											InetSocketAddress address = dc.getAddress();
											
											if ( !contact_map.containsKey( address )){
												
												try{
													DistributedDatabaseContact c = importContact( dc, network );
													
													contact_map.put( address, c );
													
													contacts_to_search.add( c );
																					
													if ( contact_map.size() >= MAX_REMOTE_SEARCH_CONTACTS ){
														
														break;
													}
												}catch( Throwable e ){
													
												}
											}
										}
										
										if ( contact_map.size() >= MAX_REMOTE_SEARCH_CONTACTS ){
											
											break;
										}
									}
								}
							}
							
								// interleave hinted ones so we get some variety
							
							int	desired_pos = 0;
							
							for ( DistributedDatabaseContact dc: initial_hinted_contacts ){
								
								if ( desired_pos < contacts_to_search.size()){
									
									contacts_to_search.add( desired_pos, dc );
								
									desired_pos += 2;
									
								}else{
									
									contacts_to_search.addLast( dc );
								}
							}

							
							long	start		= SystemTime.getMonotonousTime();
							long	max			= MAX_REMOTE_SEARCH_MILLIS;
							
							final AESemaphore	sem = new AESemaphore( "RCM:rems" );
							
							int	sent = 0;
							
							final int[]			done = {0};
							
							logSearch( "Search starts: contacts=" + contacts_to_search.size() + ", hinted=" + initial_hinted_contacts.size());
							
							while( true ){
										
									// hard limit of total results found and overall elapsed
								
								if ( 	observer.getResultCount() >= 200 || 
										SystemTime.getMonotonousTime() - start >= max ){
									
									logSearch( "Hard limit exceeded" );
									
									return;
								}

								if ( sent >= MAX_REMOTE_SEARCH_CONTACTS ){
									
									logSearch( "Max contacts searched" );
									
									break;
								}

								final DistributedDatabaseContact contact_to_search;
								
								synchronized( contacts_to_search ){
									
									if ( contacts_to_search.isEmpty()){
										
										logSearch( "Contacts exhausted" );
										
										break;
										
									}else{
										
										contact_to_search = contacts_to_search.removeFirst();
									}
								}
																
								new AEThread2( "RCM:rems", true )
								{
									public void
									run()
									{
										try{
											logSearch( "Searching " + contact_to_search.getAddress());
											
											List<DistributedDatabaseContact> extra_contacts = sendRemoteSearch( si, hashes_sync_me, contact_to_search, term, observer );
													
											if ( extra_contacts == null ){
												
												logSearch( "    " + contact_to_search.getAddress() + " failed" );
												
												foreignBloomFailed( contact_to_search );
												
											}else{
												
												String	type;
												
												if ( initial_hinted_contacts.contains( contact_to_search )){
													type = "i";
												}else if ( extra_hinted_contacts.contains( contact_to_search )){
													type = "e";
												}else{
													type = "n";
												}
												logSearch( "    " + contact_to_search.getAddress() + " OK " + type + " - additional=" + extra_contacts.size());
												
													// insert results from more predictable nodes after the less predictable ones
												
												synchronized( contacts_to_search ){
													
													int	insert_point = 0;
													
													if ( type.equals( "i" )){
														
														for (int i=0;i<contacts_to_search.size();i++){
															
															if ( extra_hinted_contacts.contains(contacts_to_search.get(i))){
																
																insert_point = i+1;
															}
														}
													}
													
													for ( DistributedDatabaseContact c: extra_contacts ){
														
														InetSocketAddress address = c.getAddress();
						
														if ( !contact_map.containsKey( address )){
															
															logSearch( "        additional target: " + address );
															
															extra_hinted_contacts.add( c );
															
															contact_map.put( address, c );
															
															contacts_to_search.add( insert_point, c );
														}
													}
												}
											}
										}finally{
											
											synchronized( done ){
											
												done[0]++;
											}
											
											sem.release();
										}
									}
								}.start();
								
								sent++;
								
								synchronized( done ){
									
									if ( done[0] >= MAX_REMOTE_SEARCH_CONTACTS / 2 ){
										
										logSearch( "Switching to reduced time limit (1)" );
										
											// give another 5 secs for results to come in
										
										start		= SystemTime.getMonotonousTime();
										max			= REDUCED_REMOTE_SEARCH_MILLIS;
										
										break;
									}
								}
								
								if ( sent > 10 ){
									
										// rate limit a bit after the first 10
									
									try{
										Thread.sleep( 250 );
										
									}catch( Throwable e ){
									}
								}
							}
							
							logSearch( "Request dispatch complete: sent=" + sent + ", done=" + done[0] );
							
							for ( int i=0;i<sent;i++ ){
								
								if ( done[0] > sent*9/10 ){
									
									logSearch( "9/10ths replied (" + done[0] + "/" + sent + "), done" );
									
									break;
								}
								
								long	remaining = ( start + max ) - SystemTime.getMonotonousTime();
								
								if ( 	remaining > REDUCED_REMOTE_SEARCH_MILLIS &&
										done[0] >= MAX_REMOTE_SEARCH_CONTACTS / 2 ){
									
									logSearch( "Switching to reduced time limit (2)" );
									
										// give another 5 secs for results to come in
									
									start		= SystemTime.getMonotonousTime();
									max			= REDUCED_REMOTE_SEARCH_MILLIS;
								}
								
								if ( remaining > 0 ){
									
									sem.reserve( 250 );
									
								}else{
									
									logSearch( "Time exhausted" );
									
									break;
								}
							}
						}finally{
								
							logSearch( "Search complete" );
							
							observer.complete();
						}
					}
				}
			}.start();
		}
		
		return( si );
	}
	
	private String
	fixupTerm(
		String	term )
	{
		if ( term == null ){
			
			return( null );
		}
		
		// if someone has a | in the expression then they probably mean "or" so to work with the following we remove any spaces
		// either side of it
	
		if ( term.contains( "|" )){
			
			while( term.contains( " |" )){
				
				term = term.replaceAll( " \\|", "|" );
			}
			
		while( term.contains( "| " )){
				
				term = term.replaceAll( "\\| ", "|" );
			}
		}
		
		return( term );
	}
	
	private String
	escapeTag(
		String	tag )
	{
		if ( tag.contains( " " )){
			
			tag = tag.replaceAll( " ", "+" );
		}
		
		return( tag );
	}
	
	private String
	unescapeTag(
		String	tag )
	{
		if ( tag.contains( "+" )){
			
			tag = tag.replaceAll( "\\+", " " );
		}
		
		return( tag );
	}
	
	protected List<RelatedContent>
	matchContent(
		String		term )
	{
			// term is made up of space separated bits - all bits must match
			// each bit can be prefixed by + or -, a leading - means 'bit doesn't match'. + doesn't mean anything
			// each bit (with prefix removed) can be "(" regexp ")"
			// if bit isn't regexp but has "|" in it it is turned into a regexp so a|b means 'a or b'
		
		String[]	 bits = Constants.PAT_SPLIT_SPACE.split(term.toLowerCase());

		int[]		bit_types 		= new int[bits.length];
		Pattern[]	bit_patterns 	= new Pattern[bits.length];
		
		for (int i=0;i<bits.length;i++){
			
			String bit = bits[i] = bits[i].trim();
			
			if ( bit.length() > 0 ){
				
				char	c = bit.charAt(0);
				
				if ( c == '+' ){
					
					bit_types[i] = 1;
					
					bit = bits[i] = bit.substring(1);
					
				}else if ( c == '-' ){
					
					bit_types[i] = 2;
					
					bit = bits[i] = bit.substring(1);
				}
				
				if ( bit.startsWith( "(" ) && bit.endsWith((")"))){
					
					bit = bit.substring( 1, bit.length()-1 );
					
					try{
						if ( !RegExUtil.mightBeEvil( bit )){
						
							bit_patterns[i] = Pattern.compile( bit, Pattern.CASE_INSENSITIVE );
						}
					}catch( Throwable e ){
					}
				}else if ( bit.contains( "|" )){
					
					if ( !bit.contains( "tag:" )){
						
						try{
							if ( !RegExUtil.mightBeEvil( bit )){
							
								bit_patterns[i] = Pattern.compile( bit, Pattern.CASE_INSENSITIVE );
							}
						}catch( Throwable e ){
						}
					}
				}
			}
		}
		
		Map<String,RelatedContent>	result = new HashMap<String,RelatedContent>();
		
		Iterator<DownloadInfo>	it1 = getDHTInfos().iterator();
		
		Iterator<DownloadInfo>	it2;
		
		synchronized( manager.rcm_lock ){
		
			it2 = new ArrayList<DownloadInfo>( RelatedContentManager.transient_info_cache.values()).iterator();
		}

		Iterator<DownloadInfo>	it3 = manager.getRelatedContentAsList().iterator();

		for ( Iterator _it: new Iterator[]{ it1, it2, it3 }){
			
			Iterator<DownloadInfo> it = (Iterator<DownloadInfo>)_it;	
			
			while( it.hasNext()){
				
				DownloadInfo c = it.next();
				
				String title 	= c.getTitle();
				String lc_title = c.getTitle().toLowerCase();
				
				boolean	match 			= true;
				boolean	at_least_one 	= false;
				
				if ( title.equalsIgnoreCase( term ) && term.trim().length() > 0 ){
					
						// pick up a direct match regardless of anything else
					
					at_least_one = true;
					
				}else{
					
					for (int i=0;i<bits.length;i++){
						
						String bit = bits[i];
						
						if ( bit.length() > 0 ){
							
							boolean	hit;
							
							if ( bit_patterns[i] == null ){
							
								String[]	sub_bits = bit.split("\\|");
								
								hit = false;
								
								for ( String sub_bit: sub_bits ){

									if ( sub_bit.startsWith( "tag:" )){
										
										String[] tags = c.getTags();
										
										hit = false;
										
										if ( tags != null && tags.length > 0 ){
											
											String	target_tag = sub_bit.substring( 4 ).toLowerCase( Locale.US );
											
											target_tag = unescapeTag( target_tag );
											
											target_tag = manager.truncateTag( target_tag );
											
											for ( String t: tags ){
												
												if ( t.startsWith( target_tag )){
													
													hit = true;
													
													break;
												}
											}
										}
									}else{
									
										hit = lc_title.contains( sub_bit );
									}
									
									if ( hit ){
										
										break;
									}
								}
							}else{
							
								hit = bit_patterns[i].matcher( lc_title ).find();
							}
							
							int	type = bit_types[i];
							
							if ( hit ){
														
								if ( type == 2 ){
									
									match = false;
									
									break;
									
								}else{
									
									at_least_one = true;
		
								}
							}else{
								
								if ( type == 2 ){
								
									at_least_one = true;
									
								}else{
									
									match = false;
								
									break;
								}
							}
						}
					}
				}
				
				if ( match && at_least_one ){
					
					byte[]	hash = c.getHash();
					
					String key;
					
					if ( hash != null ){
						
						key = Base32.encode( hash );
						
					}else{
			
						key = manager.getPrivateInfoKey( c );
					}
					
					result.put( key, c );
				}
			}
		}
		
		return( new ArrayList<RelatedContent>( result.values()));
	}
	
	protected List<DistributedDatabaseContact>
	sendRemoteSearch(
		SearchInstance					si,
		Set<String>						hashes_sync_me,
		DistributedDatabaseContact		contact,
		String							term,
		SearchObserver					observer )
	{
		try{
			Map<String,Object>	request = new HashMap<String,Object>();
			
			request.put( "t", term );
		
			DistributedDatabaseKey key = ddb.createKey( BEncoder.encode( request ));
			
			DistributedDatabaseValue value = 
				contact.read( 
					new DistributedDatabaseProgressListener()
					{
						public void
						reportSize(
							long	size )
						{	
						}
						
						public void
						reportActivity(
							String	str )
						{	
						}
						
						public void
						reportCompleteness(
							int		percent )
						{
						}
					},
					transfer_type,
					key,
					contact.getAddress().isUnresolved()?20000:10000 );
						
			if ( value == null ){
				
				return( null );
			}
			
			Map<String,Object> reply = (Map<String,Object>)BDecoder.decode((byte[])value.getValue( byte[].class ));
			
			List<Map<String,Object>>	list = (List<Map<String,Object>>)reply.get( "l" );
			
			if ( list != null ){
				
				for ( final Map<String,Object> map: list ){
					
					final String title = ImportExportUtils.importString( map, "n" );
					
					final byte[] hash = (byte[])map.get( "h" );
					
					if ( hash == null ){
						
						continue;
					}
					
					String	hash_str = Base32.encode( hash );
						
					synchronized( hashes_sync_me ){
						
						if ( hashes_sync_me.contains( hash_str )){
							
							continue;
						}
						
						hashes_sync_me.add( hash_str );
					}
	
					SearchResult result = 
						new SearchResult()
						{
							public Object
							getProperty(
								int		property_name )
							{
								try{
									if ( property_name == SearchResult.PR_NAME ){
										
										return( title );
										
									}else if ( property_name == SearchResult.PR_SIZE ){
										
										return( ImportExportUtils.importLong( map, "s" ));
										
									}else if ( property_name == SearchResult.PR_HASH ){
										
										return( hash );
										
									}else if ( property_name == SearchResult.PR_RANK ){
										
										return( ImportExportUtils.importLong( map, "r" ) / 4 );
										
									}else if ( property_name == SearchResult.PR_SUPER_SEED_COUNT ){
										
										long cnet = ImportExportUtils.importLong( map, "c", ContentNetwork.CONTENT_NETWORK_UNKNOWN );
										
										if ( cnet == ContentNetwork.CONTENT_NETWORK_UNKNOWN ){
											
											return( 0L );
											
										}else{
											
											return( 1L );
										}
									}else if ( property_name == SearchResult.PR_SEED_COUNT ){
										
										return( ImportExportUtils.importLong( map, "z" ));
										
									}else if ( property_name == SearchResult.PR_LEECHER_COUNT ){
										
										return( ImportExportUtils.importLong( map, "l" ));
										
									}else if ( property_name == SearchResult.PR_PUB_DATE ){
										
										long date = ImportExportUtils.importLong( map, "p", 0 )*60*60*1000L;
										
										if ( date <= 0 ){
											
											return( null );
										}
										
										return( new Date( date ));
										
									}else if ( 	property_name == SearchResult.PR_DOWNLOAD_LINK ||
												property_name == SearchResult.PR_DOWNLOAD_BUTTON_LINK ){
										
										byte[] hash = (byte[])map.get( "h" );
										
										if ( hash != null ){
											
											return( UrlUtils.getMagnetURI( hash, title, RelatedContentManager.convertNetworks((byte)ImportExportUtils.importLong( map, "o", RelatedContentManager.NET_PUBLIC ))));
										}
									}else if (  property_name == RelatedContentManager.RCM_SEARCH_PROPERTY_CONTENT_NETWORK ){
										
										long cnet = ImportExportUtils.importLong( map, "c", ContentNetwork.CONTENT_NETWORK_UNKNOWN );
	
										return( cnet );
										
									}else if ( property_name == RelatedContentManager.RCM_SEARCH_PROPERTY_TRACKER_KEYS ){
										
										return( map.get( "k" ));
										
									}else if ( property_name == RelatedContentManager.RCM_SEARCH_PROPERTY_WEB_SEED_KEYS ){
										
										return( map.get( "w" ));
										
									}else if ( property_name == RelatedContentManager.RCM_SEARCH_PROPERTY_TAGS ){
										
										return( manager.decodeTags((byte[])map.get( "g" )));
										
									}else if ( property_name == RelatedContentManager.RCM_SEARCH_PROPERTY_NETWORKS ){
	
										return( RelatedContentManager.convertNetworks((byte)ImportExportUtils.importLong( map, "o", RelatedContentManager.NET_PUBLIC )));
									}
	
								}catch( Throwable e ){
								}
								
								return( null );
							}
						};
						
					observer.resultReceived( si, result );
				}
			}
			
			list = (List<Map<String,Object>>)reply.get( "c" );

			List<DistributedDatabaseContact>	contacts = new ArrayList<DistributedDatabaseContact>();
			
			if ( list != null ){
			
				for ( Map<String,Object> m: list ){
					
					try{
						Map<String,Object> map = (Map<String,Object>)m.get( "m" );
						
						if ( map != null ){
							
							DistributedDatabaseContact ddb_contact = ddb.importContact( map );
		
							contacts.add( ddb_contact );
							
						}else{
							
							String	host 	= ImportExportUtils.importString( m, "a" );
							
							int		port	= ImportExportUtils.importInt( m, "p" );
							
							DistributedDatabaseContact ddb_contact = 
								ddb.importContact( new InetSocketAddress( InetAddress.getByName(host), port ), DHTTransportUDP.PROTOCOL_VERSION_MIN, contact.getDHT());
	
							contacts.add( ddb_contact );
						}
						
					}catch( Throwable e ){
					}
					
				}
			}
			
			return( contacts );
			
		}catch( Throwable e ){
			
			return( null );
		}
	}
	
	protected BloomFilter
	sendRemoteFetch(
		DistributedDatabaseContact		contact )
	{
		try{
			Map<String,Object>	request = new HashMap<String,Object>();
			
			request.put( "x", "f" );
		
			DistributedDatabaseKey key = ddb.createKey( BEncoder.encode( request ));
			
			DistributedDatabaseValue value = 
				contact.read( 
					new DistributedDatabaseProgressListener()
					{
						public void
						reportSize(
							long	size )
						{	
						}
						
						public void
						reportActivity(
							String	str )
						{	
						}
						
						public void
						reportCompleteness(
							int		percent )
						{
						}
					},
					transfer_type,
					key,
					contact.getAddress().isUnresolved()?15000:5000 );
			
				// System.out.println( "search result=" + value );
			
			if ( value != null ){
			
				Map<String,Object> reply = (Map<String,Object>)BDecoder.decode((byte[])value.getValue( byte[].class ));
			
				Map<String,Object>	m = (Map<String,Object>)reply.get( "f" );
				
				if ( m != null ){
					
					return( BloomFilterFactory.deserialiseFromMap( m ));
				}
			}
		}catch( Throwable e ){
		}
		
		return( null );
	}
	
	protected BloomFilter
	sendRemoteUpdate(
		ForeignBloom	f_bloom )
	{
		try{
			Map<String,Object>	request = new HashMap<String,Object>();
			
			request.put( "x", "u" );
			request.put( "s", new Long( f_bloom.getFilter().getEntryCount()));
		
			DistributedDatabaseKey key = ddb.createKey( BEncoder.encode( request ));
			
			DistributedDatabaseContact contact = f_bloom.getContact();
			
			DistributedDatabaseValue value = 
				contact.read( 
					new DistributedDatabaseProgressListener()
					{
						public void
						reportSize(
							long	size )
						{	
						}
						
						public void
						reportActivity(
							String	str )
						{	
						}
						
						public void
						reportCompleteness(
							int		percent )
						{
						}
					},
					transfer_type,
					key,
					contact.getAddress().isUnresolved()?15000:5000 );
			
				// System.out.println( "search result=" + value );
			
			if ( value != null ){
			
				Map<String,Object> reply = (Map<String,Object>)BDecoder.decode((byte[])value.getValue( byte[].class ));
			
				Map<String,Object>	m = (Map<String,Object>)reply.get( "f" );
				
				if ( m != null ){
					
					logSearch( "Bloom for " + f_bloom.getContact().getAddress() + " updated" );

					return( BloomFilterFactory.deserialiseFromMap( m ));
					
				}else{
					
					if ( reply.containsKey( "s" )){
						
						logSearch( "Bloom for " + f_bloom.getContact().getAddress() + " same size" );
						
					}else{
						
						logSearch( "Bloom for " + f_bloom.getContact().getAddress() + " update not supported yet" );
					}
					
					return( f_bloom.getFilter());
				}
			}
		}catch( Throwable e ){
		}
		
		logSearch( "Bloom for " + f_bloom.getContact().getAddress() + " update failed" );

		return( null );
	}
	
	protected Map<String,Object>
	receiveRemoteRequest(
		DistributedDatabaseContact		originator,
		Map<String,Object>				request )
	{
		Map<String,Object>	response = new HashMap<String,Object>();
		
		try{	
			boolean	originator_is_neighbour = false;
			
			DHTInterface[] dhts = dht_plugin.getDHTInterfaces();
			
			byte[] originator_id = originator.getID();
			
			byte[] originator_bytes = AddressUtils.getAddressBytes( originator.getAddress());

			for ( DHTInterface d: dhts ){
				
				List<DHTPluginContact> contacts = d.getClosestContacts( d.getID(), true );
				
				for ( DHTPluginContact c: contacts ){
					
					if ( Arrays.equals( c.getID(), originator_id)){
						
						originator_is_neighbour = true;
						
						break;
					}
				}
				
				if ( originator_is_neighbour ){
					
					break;
				}
			}
			
			String	req_type = ImportExportUtils.importString( request, "x" );
			
			if ( req_type != null ){
							
				boolean dup = harvest_op_requester_bloom.contains( originator_bytes );
					
				logSearch( "Received remote request: " + BDecoder.decodeStrings( request ) + " from " + originator.getAddress() + "/" + originator.getDHT() + ", dup=" + dup + ", bs=" + harvest_op_requester_bloom.getEntryCount());
				
				if ( !dup ){
					
					harvest_op_requester_bloom.add( originator_bytes );
					
					if ( req_type.equals( "f" )){
						
						BloomFilter filter = getKeyBloom( !originator_is_neighbour );
						
						if ( filter != null ){
							
							response.put( "f", filter.serialiseToMap());
						}
					}else if ( req_type.equals( "u" )){
						
						BloomFilter filter = getKeyBloom( !originator_is_neighbour );
						
						if ( filter != null ){
	
							int	existing_size = ImportExportUtils.importInt( request, "s", 0 );
						
							if ( existing_size != filter.getEntryCount()){
								
								response.put( "f", filter.serialiseToMap());
								
							}else{
								
								response.put( "s", new Long( existing_size ));
							}
						}
					}
				}
			}else{
					// fallback to default handling

				int hits = harvest_se_requester_bloom.count( originator_bytes );
				
				String	term = ImportExportUtils.importString( request, "t" );

				term = fixupTerm( term );
				
				logSearch( "Received remote search: '" + term + "' from " + originator.getAddress() + ", hits=" + hits + ", bs=" + harvest_se_requester_bloom.getEntryCount());

				if ( hits < 10 ){
					
					harvest_se_requester_bloom.add( originator_bytes );
					
					if ( term != null ){
												
						List<RelatedContent>	matches = matchContent( term );
		
						if ( matches.size() > MAX_REMOTE_SEARCH_RESULTS ){
							
							Collections.sort(
								matches,
								new Comparator<RelatedContent>()
								{
									public int 
									compare(
										RelatedContent o1,
										RelatedContent o2) 
									{
										return( o2.getRank() - o1.getRank());
									}
								});
						}
						
						List<Map<String,Object>> l_list = new ArrayList<Map<String,Object>>();
						
						for (int i=0;i<Math.min( matches.size(),MAX_REMOTE_SEARCH_RESULTS);i++){
							
							RelatedContent	c = matches.get(i);
							
							Map<String,Object>	map = new HashMap<String, Object>();
							
							l_list.add( map );
							
							ImportExportUtils.exportString( map, "n", c.getTitle());
							ImportExportUtils.exportLong( map, "s", c.getSize());
							ImportExportUtils.exportLong( map, "r", c.getRank());
							ImportExportUtils.exportLong( map, "d", c.getLastSeenSecs());
							ImportExportUtils.exportLong( map, "p", c.getPublishDate()/(60*60*1000));
							ImportExportUtils.exportLong( map, "l", c.getLeechers());
							ImportExportUtils.exportLong( map, "z", c.getSeeds());
							ImportExportUtils.exportLong( map, "c", c.getContentNetwork());
							
							byte[] hash = c.getHash();
							
							if ( hash != null ){
								
								map.put( "h", hash );
							}
							
							byte[] tracker_keys = c.getTrackerKeys();
							
							if ( tracker_keys != null ){
								map.put( "k", tracker_keys );
							}
							
							byte[] ws_keys	= c.getWebSeedKeys();
							
							if ( ws_keys != null ){
								map.put( "w", ws_keys );
							}
							
							String[] tags = c.getTags();
							
							if ( tags != null ){
								map.put( "g", manager.encodeTags( tags ));
							}
							
							byte nets = c.getNetworksInternal();
							
							if ( 	nets != RelatedContentManager.NET_NONE &&  
									nets != RelatedContentManager.NET_PUBLIC ){
								
								map.put( "o", new Long( nets&0x00ff));
							}
							
								// don't bother with tracker as no use to caller really
						}
						
						response.put( "l", l_list );
					
						List<DistributedDatabaseContact> bloom_hits = searchForeignBlooms( term );
						
						if ( bloom_hits.size() > 0 ){
							
							List<Map>	c_list = new ArrayList<Map>();
							
							for ( DistributedDatabaseContact c: bloom_hits ){
								
								Map	m = new HashMap();
								
								c_list.add( m );
								
								InetSocketAddress address = c.getAddress();
								
								if ( address.isUnresolved()){
									
									m.put( "m", c.exportToMap());
									
								}else{
									m.put( "a", address.getAddress().getHostAddress());
									
									m.put( "p", new Long( address.getPort()));
								}
							}
							
							response.put( "c", c_list );
						}
					}
				}
			}
		}catch( Throwable e ){
		}
		
		return( response );
	}
	
	public DistributedDatabaseValue
	read(
		DistributedDatabaseContact			contact,
		DistributedDatabaseTransferType		type,
		DistributedDatabaseKey				ddb_key )
	
		throws DistributedDatabaseException
	{
		Object	o_key = ddb_key.getKey();
		
		try{
			byte[]	key = (byte[])o_key;
			
				// TODO bloom
			
			Map<String,Object>	request = BDecoder.decode( key );
			
			Map<String,Object>	result = receiveRemoteRequest( contact, request );
			
			return( ddb.createValue( BEncoder.encode( result )));
			
		}catch( Throwable e ){
			
			Debug.out( e );
			
			return( null );
		}
	}
	
	public DistributedDatabaseValue
	write(
		DistributedDatabaseContact			contact,
		DistributedDatabaseTransferType		type,
		DistributedDatabaseKey				key,
		DistributedDatabaseValue			value )
	
		throws DistributedDatabaseException
	{
		return( null );
	}
	
	private void
	checkKeyBloom()
	{
		if ( last_key_bloom_update == -1 || SystemTime.getMonotonousTime() - last_key_bloom_update > 10*60*1000 ){
			
			synchronized( manager.rcm_lock ){
				
				updateKeyBloom( manager.loadRelatedContent());
			}
		}
	}
	
	private BloomFilter
	getKeyBloom(
		boolean		include_dht_local )
	{
		if ( key_bloom_with_local == null ){
				
			synchronized( manager.rcm_lock ){
			
				updateKeyBloom( manager.loadRelatedContent());
			}
		}
			
		if ( include_dht_local ){
			
			return( key_bloom_with_local );
			
		}else{
			
			return( key_bloom_without_local );
		}
	}
	
	private List<String>
	getDHTWords(
		DownloadInfo	info )
	{
		String title = info.getTitle();
		
		title = title.toLowerCase( Locale.US );
		
		char[]	chars = title.toCharArray();
		
		for ( int i=0;i<chars.length;i++){
			
			if ( !Character.isLetterOrDigit( chars[i])){
				
				chars[i] = ' ';
			}
		}
		
		String[] words = new String( chars ).split( " " );
		
		List<String>	result = new ArrayList<String>( words.length );
		
		for ( String word: words ){
			
			if ( word.length() > 0 && !ignore_words.contains( word )){
				
				result.add( word );
			}
		}
		
		String[] tags = info.getTags();
		
		if ( tags != null ){
			
			for ( String tag: tags ){
				
				tag = escapeTag( tag );
							
					// support prefix matching
				
				for ( int i=1; i<=tag.length(); i++){
				
					result.add( "tag:" + tag.substring( 0, i ));
				}
			}
		}
		
		return( result );
	}
	
	protected void
	updateKeyBloom(
		RelatedContentManager.ContentCache		cc )
	{
		synchronized( manager.rcm_lock ){
												
			Set<String>	dht_only_words 		= new HashSet<String>();
			Set<String>	non_dht_words 		= new HashSet<String>();
			
			List<DownloadInfo>		dht_infos		= getDHTInfos();
			
			Iterator<DownloadInfo>	it_dht 			= dht_infos.iterator();
						
			Iterator<DownloadInfo>	it_transient 	= RelatedContentManager.transient_info_cache.values().iterator();
			
			Iterator<DownloadInfo>	it_rc 			= cc.related_content.values().iterator();			

			for ( Iterator _it: new Iterator[]{ it_transient, it_rc, it_dht }){
				
				Iterator<DownloadInfo> it = (Iterator<DownloadInfo>)_it;
				
				while( it.hasNext()){
				
					DownloadInfo di = it.next();
							
					List<String>	words = getDHTWords( di );
					
					for ( String word: words ){
															
							// note that it_dht is processed last
						
						if ( it == it_dht ){
							
							if ( !non_dht_words.contains( word )){
							
								dht_only_words.add( word );
							}
						}else{
															
							non_dht_words.add( word );
						}
					}
				}
			}
			
			int	all_desired_bits = (dht_only_words.size() + non_dht_words.size()) * KEY_BLOOM_LOAD_FACTOR;
			
			all_desired_bits = Math.max( all_desired_bits, KEY_BLOOM_MIN_BITS );
			all_desired_bits = Math.min( all_desired_bits, KEY_BLOOM_MAX_BITS );
			
			BloomFilter all_bloom = BloomFilterFactory.createAddOnly( all_desired_bits );

			int	non_dht_desired_bits = non_dht_words.size() * KEY_BLOOM_LOAD_FACTOR;
			
			non_dht_desired_bits = Math.max( non_dht_desired_bits, KEY_BLOOM_MIN_BITS );
			non_dht_desired_bits = Math.min( non_dht_desired_bits, KEY_BLOOM_MAX_BITS );
			
			BloomFilter non_dht_bloom = BloomFilterFactory.createAddOnly( non_dht_desired_bits );

			List<String>	non_dht_words_rand = new ArrayList<String>( non_dht_words );
			
			Collections.shuffle( non_dht_words_rand );
			
			for ( String word: non_dht_words_rand ){
				
				try{
					byte[]	bytes = word.getBytes( "UTF8" );
					
					all_bloom.add( bytes );
					
					if ( all_bloom.getEntryCount() >= KEY_BLOOM_MAX_ENTRIES ){
						
						break;
					}
					
					if ( non_dht_bloom.getEntryCount() < KEY_BLOOM_MAX_ENTRIES ){
					
						non_dht_bloom.add( bytes );
					}
				}catch( Throwable e ){
				}
			}
				
			List<String>	dht_only_words_rand = new ArrayList<String>( dht_only_words );
			
			Collections.shuffle( dht_only_words_rand );

			for ( String word: dht_only_words_rand ){
				
				try{
					byte[]	bytes = word.getBytes( "UTF8" );
					
					all_bloom.add( bytes );
					
					if ( all_bloom.getEntryCount() >= KEY_BLOOM_MAX_ENTRIES ){
						
						break;
					}
				}catch( Throwable e ){
				}
			}
			
			logSearch( 
				"blooms=" + 
				all_bloom.getSize() + "/" + all_bloom.getEntryCount() +", " +
				non_dht_bloom.getSize() + "/" + non_dht_bloom.getEntryCount()  +
				": rcm=" + cc.related_content.size() + ", trans=" + RelatedContentManager.transient_info_cache.size() + ", dht=" + dht_infos.size());
			
			key_bloom_with_local 	= all_bloom;
			key_bloom_without_local = non_dht_bloom;
			
			last_key_bloom_update = SystemTime.getMonotonousTime();
		}
	}
	
	private List<DownloadInfo>
	getDHTInfos()
	{
		List<DHTPluginValue> vals = dht_plugin.getValues();
				
		Set<String>	unique_keys = new HashSet<String>();
		
		List<DownloadInfo>	dht_infos = new ArrayList<DownloadInfo>();
		
		for ( DHTPluginValue val: vals ){
			
			if ( !val.isLocal()){
				
				byte[]	bytes = val.getValue();
				
				String test = new String( bytes );
				
				if ( test.startsWith( "d1:d" ) && test.endsWith( "ee" ) && test.contains( "1:h20:")){
							
					try{
						Map map = BDecoder.decode( bytes );
					
						DownloadInfo info =	manager.decodeInfo( map, null, 1, false, unique_keys );
						
						if ( info != null ){
							
							dht_infos.add( info );
						}
					}catch( Throwable e ){
						
					}
				}
			}
		}
		
		return( dht_infos );
	}
	
	private void
	testKeyBloom()
	{
		if ( true ){
			return;
		}
		
		System.out.println( "test key bloom" );
		
		try{
			Map<String,int[]>	all_words 		= new HashMap<String,int[]>();
	
			synchronized( manager.rcm_lock ){
				
				ContentCache cache = manager.loadRelatedContent();
				
				List<DownloadInfo>		dht_infos		= getDHTInfos();
				
				Iterator<DownloadInfo>	it_dht 			= dht_infos.iterator();
							
				Iterator<DownloadInfo>	it_transient 	= RelatedContentManager.transient_info_cache.values().iterator();
				
				Iterator<DownloadInfo>	it_rc 			= cache.related_content.values().iterator();			
	
				updateKeyBloom( cache );
				
				int	 i=0;
				
				for ( Iterator _it: new Iterator[]{ it_transient, it_rc, it_dht }){
					
					Iterator<DownloadInfo> it = (Iterator<DownloadInfo>)_it;
					
					while( it.hasNext()){
					
						DownloadInfo di = it.next();
								
						List<String>	words = getDHTWords( di );
						
						for ( String word: words ){
									
							int[] x = all_words.get( word );
							
							if ( x == null ){
								
								x = new int[3];
								
								all_words.put( word, x );
							}
							
							x[i] = 1;
						}
					}
					
					i++;
				}
			}
			
			BloomFilter bloom = getKeyBloom( true );
	
			int	total 	= 0;
			int	clashes	= 0;
			int misses	= 0;
			
			int match_fails = 0;
			
			Random random = new Random();
			
			for ( Map.Entry<String,int[]> entry: all_words.entrySet()){
				
				String 	word 	= entry.getKey();
				int[]	source 	= entry.getValue();
				
				boolean	r1 = bloom.contains( word.getBytes("UTF-8") );
				boolean r2 = bloom.contains( (word + random.nextLong()).getBytes("UTF-8"));
				
				System.out.println( word + " -> " + r1 + "/" +r2 );
				
				total++;
				if ( r1 && r2 ){
					clashes++;
				}
				if ( !r1 ){
					
					misses++;
				}
				
				List<RelatedContent> hits = matchContent( word );
				
				if ( hits.size() == 0 ){
					
					hits = matchContent( word );
					match_fails++;
				}
			}
			
			System.out.println( "total=" + total + ", clash=" + clashes + ", miss=" + misses + ", fails=" + match_fails + ", bloom=" + bloom.getString() );
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	private void
	harvestBlooms()
	{
		harvest_dispatcher.dispatch(
			new AERunnable()
			{
				public void
				runSupport()
				{
					if ( harvest_dispatcher.getQueueSize() > 0 ){
						
						return;
					}
					
					ForeignBloom oldest = null;
					
					synchronized( harvested_blooms ){
						
						for ( ForeignBloom bloom: harvested_blooms.values()){
							
							if ( 	oldest == null ||
									bloom.getLastUpdateTime() < oldest.getLastUpdateTime()){
								
								oldest	= bloom;
							}
						}
					}
					
					long now = SystemTime.getMonotonousTime();
					
					if ( oldest != null ){
						
						if ( now - oldest.getLastUpdateTime() > HARVEST_BLOOM_UPDATE_MILLIS ){
							
							DistributedDatabaseContact ddb_contact = oldest.getContact();

							if ( 	now - oldest.getCreateTime() > HARVEST_BLOOM_DISCARD_MILLIS &&
									harvested_blooms.size() >= HARVEST_MAX_BLOOMS / 2 ){
							
									// don't want to stick with a stable one for too long otherwise the stabler
									// nodes will end up in lots of other nodes' harvest set and receive
									// undue attention (unless we don't have that many nodes...)
								
								logSearch( "Harvest: discarding " + ddb_contact.getAddress());
								
								synchronized( harvested_blooms ){
									
									harvested_blooms.remove( ddb_contact.getID());
								}
							}else{

								BloomFilter updated_filter = sendRemoteUpdate( oldest );
								
								if ( updated_filter == null ){
																						
									synchronized( harvested_blooms ){
									
										harvested_blooms.remove( ddb_contact.getID());
									
										harvested_fails.put( ddb_contact.getID(), "" );
									}
								}else{
																	
									oldest.updateFilter( updated_filter );
								}
							}
						}
					}
					
					if ( harvested_blooms.size() < HARVEST_MAX_BLOOMS ){
					
						try{
							int	fail_count	= 0;
							
							DHTInterface[] dhts = dht_plugin.getDHTInterfaces();
							
outer:
							for ( DHTInterface dht: dhts ){
																
								if ( dht.isIPV6()){
									
									continue;
								}
								
								int	network = dht.getNetwork();
								
								if ( SEARCH_CVS_ONLY && network != DHT.NW_CVS ){
									
									logSearch( "Harvest: ignoring main DHT" );
									
									continue;
								}
													
								DHTPluginContact[] contacts = dht.getReachableContacts();
																
								byte[] dht_id = dht.getID();
								
								for ( DHTPluginContact contact: contacts ){
				
									byte[]	contact_id = contact.getID();
									
									if ( Arrays.equals( dht_id, contact_id )){
										
										// logSearch( "not skipping local!!!!" );
										
										continue;
									}
									
									DistributedDatabaseContact ddb_contact = importContact( contact, network );
									
									synchronized( harvested_blooms ){
																				
										if ( harvested_fails.containsKey( contact_id )){
											
											continue;
										}

										if ( harvested_blooms.containsKey( contact_id )){
											
											continue;
										}
									}
									
									BloomFilter filter = sendRemoteFetch( ddb_contact );
									
									logSearch( "harvest: " + contact.getString() + " -> " +(filter==null?"null":filter.getString()));

									if ( filter != null ){
										
										synchronized( harvested_blooms ){
										
											harvested_blooms.put( contact_id, new ForeignBloom( ddb_contact, filter ));
										}
										
										break outer;
										
									}else{
										
										synchronized( harvested_blooms ){
										
											harvested_fails.put( contact_id, "" );
										}
										
										fail_count++;
										
										if ( fail_count > 5 ){
											
											break outer;
										}
									}
								}
							}
						}catch( Throwable e ){
							
							e.printStackTrace();
						}
					}
					
					synchronized( harvested_blooms ){
					
						if ( harvested_fails.size() > HARVEST_MAX_FAILS_HISTORY ){
						
							harvested_fails.clear();
						}
					}
				}
			});
	}
	
	private DistributedDatabaseContact
	importContact(
		DHTPluginContact		contact,
		int						network )
		
		throws DistributedDatabaseException
	{
		InetSocketAddress address = contact.getAddress();
		
		if ( address.isUnresolved()){
			
			return( ddb.importContact( contact.exportToMap()));
			
		}else{
		
			return( ddb.importContact( address, DHTTransportUDP.PROTOCOL_VERSION_MIN, network==DHT.NW_CVS?DistributedDatabase.DHT_CVS:DistributedDatabase.DHT_MAIN ));
		}
	}
	
	private void
	foreignBloomFailed(
		DistributedDatabaseContact		contact )
	{
		byte[]	contact_id = contact.getID();
		
		synchronized( harvested_blooms ){
			
			if ( harvested_blooms.remove( contact_id ) != null ){
			
				harvested_fails.put( contact_id, "" );
			}
		}
	}
	
	private List<DistributedDatabaseContact>
	searchForeignBlooms(
		String		term )
	{
		List<DistributedDatabaseContact>	result = new ArrayList<DistributedDatabaseContact>();
		
		try{
			String[]	 bits = Constants.PAT_SPLIT_SPACE.split(term.toLowerCase());
	
				// note that we don't need to unescape tags in this process as tags are escaped when
				// inserted into the blooms and include the 'tag:' prefix
			
			int[]			bit_types 		= new int[bits.length];
			byte[][]		bit_bytes	 	= new byte[bit_types.length][];
			byte[][][]		extras			= new byte[bit_types.length][][];
			
			for (int i=0;i<bits.length;i++){
				
				String bit = bits[i].trim();
				
				if ( bit.length() > 0 ){
					
					char	c = bit.charAt(0);
					
					if ( c == '+' ){
						
						bit_types[i] = 1;
						
						bit = bit.substring(1);
						
					}else if ( c == '-' ){
						
						bit_types[i] = 2;
						
						bit = bit.substring(1);
					}
					
					if ( bit.startsWith( "(" ) && bit.endsWith((")"))){
						
						bit_types[i] = 3;	// ignore
						
					}else if ( bit.contains( "|" )){
											
						String[]	parts = bit.split( "\\|" );
						
						List<String>	p = new ArrayList<String>();
						
						for ( String part: parts ){
							
							part = part.trim();
							
							if ( part.length() > 0 ){
								
								p.add( part );
							}
						}
						
						if ( p.size() == 0 ){
							
							bit_types[i] = 3;
							
						}else{
							
							bit_types[i] = 4;
	
							extras[i] = new byte[p.size()][];
							
							for ( int j=0;j<p.size();j++){
								
								extras[i][j] = p.get(j).getBytes( "UTF8" );
							}
						}
					}
					
					bit_bytes[i] = bit.getBytes( "UTF8" );
				}
			}
			
			synchronized( harvested_blooms ){
				
				for ( ForeignBloom fb: harvested_blooms.values()){
					
					BloomFilter filter = fb.getFilter();
					
					boolean	failed 	= false;
					int		matches	= 0;
					
					for (int i=0;i<bit_bytes.length;i++){
						
						byte[]	bit = bit_bytes[i];
						
						if ( bit == null || bit.length == 0 ){
							
							continue;
						}
						
						int	type = bit_types[i];
						
						if ( type == 3 ){
							
							continue;
						}
						
						if ( type == 0 || type == 1 ){
							
							if ( filter.contains( bit )){
								
								matches++;
								
							}else{
								
								failed	= true;
								
								break;
							}
						}else if ( type == 2 ){
						
							if ( !filter.contains( bit )){
								
								matches++;
								
							}else{
								
								failed	= true;
								
								break;
							}
						}else if ( type == 4 ){
							
							byte[][]	parts = extras[i];
							
							int	old_matches = matches;
							
							for ( byte[] p: parts ){
								
								if ( filter.contains( p )){
								
									matches++;
									
									break;
								}
							}
							
							if ( matches == old_matches ){
								
								failed = true;
								
								break;
							}
						}
					}
					
					if ( matches > 0 && !failed ){
						
						result.add( fb.getContact());
					}
				}
			}
		}catch( UnsupportedEncodingException e ){
			
			Debug.out( e );
		}
		
		return( result );
	}
	
	private static void
	logSearch(
		String		str )
	{
		if ( TRACE_SEARCH ){
			System.out.println( str );
		}
	}
	
	private static class
	MySearchObserver
		implements SearchObserver
	{
		private SearchObserver		observer;
		private AtomicInteger		num_results = new AtomicInteger();
		
		private
		MySearchObserver(
			SearchObserver		_observer )
		{
			observer = _observer;
		}
		
		public void
		resultReceived(
			SearchInstance		search,
			SearchResult		result )
		{
			observer.resultReceived( search, result );
			
			logSearch( "results=" + num_results.incrementAndGet());
		}
		
		private int
		getResultCount()
		{
			return( num_results.get());
		}
		
		public void
		complete()
		{
			observer.complete();
		}
		
		public void
		cancelled()
		{
			observer.cancelled();
		}
		
		public Object
		getProperty(
			int		property )
		{
			return( observer.getProperty(property));
		}
	}
	
	private static class
	ForeignBloom
	{
		private DistributedDatabaseContact	contact;
		private BloomFilter					filter;
		
		private long						created;
		private long						last_update;
		
		private
		ForeignBloom(
			DistributedDatabaseContact		_contact,
			BloomFilter						_filter )
		{
			contact	= _contact;
			filter	= _filter;
			
			created	 = SystemTime.getMonotonousTime();
			
			last_update	= created;
		}
		
		public DistributedDatabaseContact
		getContact()
		{
			return( contact );
		}
		
		public BloomFilter
		getFilter()
		{
			return( filter );
		}
		
		public long
		getCreateTime()
		{
			return( created );
		}
		
		public long
		getLastUpdateTime()
		{
			return( last_update );
		}
		
		public void
		updateFilter(
			BloomFilter		f )
		{
			filter		= f;
			last_update	= SystemTime.getMonotonousTime();
		}
	}
}
