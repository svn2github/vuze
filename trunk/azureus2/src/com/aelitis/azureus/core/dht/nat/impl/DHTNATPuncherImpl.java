/*
 * Created on 11-Aug-2005
 * Created by Paul Gardner
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.core.dht.nat.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.*;

import org.gudy.azureus2.core3.util.SHA1Simple;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.utils.*;


import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.DHTLogger;
import com.aelitis.azureus.core.dht.DHTOperationAdapter;
import com.aelitis.azureus.core.dht.DHTOperationListener;
import com.aelitis.azureus.core.dht.nat.*;
import com.aelitis.azureus.core.dht.transport.DHTTransport;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportException;
import com.aelitis.azureus.core.dht.transport.DHTTransportListener;
import com.aelitis.azureus.core.dht.transport.DHTTransportProgressListener;
import com.aelitis.azureus.core.dht.transport.DHTTransportReplyHandlerAdapter;
import com.aelitis.azureus.core.dht.transport.DHTTransportTransferHandler;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

public class 
DHTNATPuncherImpl
	implements DHTNATPuncher
{
	private static boolean		TESTING	= false;
	private static boolean		TRACE	= false;
	static{
		if ( TESTING ){
			System.out.println( "**** DHTNATPuncher test on ****" );
		}
		if ( TRACE ){
			System.out.println( "**** DHTNATPuncher trace on ****" );
		}
	}
	
	private static final int	RT_BIND_REQUEST			= 0;
	private static final int	RT_BIND_REPLY			= 1;	
	private static final int	RT_PUNCH_REQUEST		= 2;
	private static final int	RT_PUNCH_REPLY			= 3;	
	private static final int	RT_CONNECT_REQUEST		= 4;
	private static final int	RT_CONNECT_REPLY		= 5;	
	
	private static final int	RESP_OK			= 0;
	private static final int	RESP_NOT_OK		= 1;
	private static final int	RESP_FAILED		= 2;
	
	private static byte[]		transfer_handler_key = new SHA1Simple().calculateHash("Aelitis:NATPuncher:TransferHandlerKey".getBytes());
	
	private boolean				started;
	
	private	DHT					dht;
	private DHTLogger			logger;
	
	private PluginInterface		plugin_interface;
	private Formatters			formatters;
	private UTTimer				timer;
	
	private static final long	REPUBLISH_TIME_MIN 			= 5*60*1000;
	private static final long	TRANSFER_TIMEOUT			= 30*1000;
	private static final long	RENDEZVOUS_LOOKUP_TIMEOUT	= 30*1000;
	
	private static final int	RENDEZVOUS_SERVER_MAX			= 8;
	private static final long	RENDEZVOUS_SERVER_TIMEOUT 		= 5*60*1000;
	private static final int	RENDEZVOUS_CLIENT_PING_PERIOD	= 50*1000;		// some routers only hold tunnel for 60s
	private static final int	RENDEZVOUS_PING_FAIL_LIMIT		= 4;			// if you make this < 2 change code below!
	
	private Monitor	server_mon;
	private Map 	rendezvous_bindings = new HashMap();
	
	private long	last_publish;
	
	private Monitor	pub_mon;
	private boolean	publish_in_progress;
	
	private volatile DHTTransportContact		rendezvous_local_contact;
	private volatile DHTTransportContact		rendezvous_target;
	private volatile DHTTransportContact		last_ok_rendezvous;
	
	private static final int FAILED_RENDEZVOUS_HISTORY_MAX	= 16;
	
	private Map		failed_rendezvous	= 
		new LinkedHashMap(FAILED_RENDEZVOUS_HISTORY_MAX,0.75f,true)
		{
			protected boolean 
			removeEldestEntry(
		   		Map.Entry eldest) 
			{
				return size() > FAILED_RENDEZVOUS_HISTORY_MAX;
			}
		};
	
	private boolean	rendezvous_thread_running;
	
	private Map		explicit_rendezvous_map		= new HashMap();
	
	public
	DHTNATPuncherImpl(
		DHT			_dht )
	{
		dht	= _dht;
	
		logger	= dht.getLogger();
		
		plugin_interface	= dht.getLogger().getPluginInterface();
		
		formatters	= plugin_interface.getUtilities().getFormatters();
		pub_mon		= plugin_interface.getUtilities().getMonitor();
		server_mon	= plugin_interface.getUtilities().getMonitor();
		
		timer = plugin_interface.getUtilities().createTimer(
						"DHTNATPuncher:refresher", true );
	}
	
	public void
	start()
	{
		if ( started ){
			
			return;
		}
		
		started	= true;
		
		DHTTransport	transport = dht.getTransport();
		
		transport.addListener(
			new DHTTransportListener()
			{
				public void
				localContactChanged(
					DHTTransportContact	local_contact )
				{
					publish( false );
				}
				
				public void
				currentAddress(
					String		address )
				{
				}
				
				public void
				reachabilityChanged(
					boolean	reacheable )
				{
					publish( false );
				}
			});
		
	
		transport.registerTransferHandler(
			transfer_handler_key,
			new DHTTransportTransferHandler()
			{
				public byte[]
	        	handleRead(
	        		DHTTransportContact	originator,
	        		byte[]				key )
				{
					return( null );
				}
				        	
	        	public byte[]
	        	handleWrite(
	        		DHTTransportContact	originator,
	        		byte[]				key,
	        		byte[]				value )
	        	{
	        		return( receiveRequest( originator, value ));
	        	}
			});
		
		timer.addPeriodicEvent(	
				REPUBLISH_TIME_MIN,
				new UTTimerEventPerformer()
				{
					public void
					perform(
						UTTimerEvent		event )
					{
						publish( false );
					}
				});
				
		timer.addPeriodicEvent(	
				RENDEZVOUS_SERVER_TIMEOUT/2,
				new UTTimerEventPerformer()
				{
					public void
					perform(
						UTTimerEvent		event )
					{
						long	now = plugin_interface.getUtilities().getCurrentSystemTime();
						
						try{
							server_mon.enter();
							
							Iterator	it = rendezvous_bindings.values().iterator();
							
							while( it.hasNext()){
								
								Object[]	entry = (Object[])it.next();
								
								long	time = ((Long)entry[1]).longValue();
								
								boolean	removed = false;
								
								if ( time > now ){
									
										// clock change, easiest approach is to remove it
									
									it.remove();
								
									removed	= true;
									
								}else if ( now - time > RENDEZVOUS_SERVER_TIMEOUT ){
									
										// timeout
									
									it.remove();
									
									removed = true;
								}
								
								if ( removed ){
									
									log( "Rendezvous " + ((DHTTransportContact)entry[0]).getString() + " removed due to inactivity" );
								}
							}
						}finally{
							
							server_mon.exit();
						}
					}
				});
		
		publish( false );
	}
	
	public boolean
	active()
	{
		return( rendezvous_local_contact != null );
	}
	
	public boolean
	operational()
	{
		DHTTransportContact	ok = last_ok_rendezvous;
		
		if ( ok != null && ok == rendezvous_target ){
			
			return( true );
		}
		
		return( false );	
	}
	
	protected void
	publish(
		final boolean		force )
	{
		long now = plugin_interface.getUtilities().getCurrentSystemTime();
		
		if ( now < last_publish && !force ){
			
			last_publish	= now;
			
		}else{
			
			if ( force || now - last_publish >= REPUBLISH_TIME_MIN ){
				
				last_publish	= now;
				
				plugin_interface.getUtilities().createThread(
					"DHTNATPuncher:publisher",
					new Runnable()
					{
						public void
						run()
						{
							try{
								pub_mon.enter();
								
								if ( publish_in_progress ){
									
									return;
								}
								
								publish_in_progress	= true;
								
							}finally{
								
								pub_mon.exit();
							}
							
							try{
								publishSupport();
								
							}finally{
								
								try{
									pub_mon.enter();
									
									publish_in_progress	= false;
									
								}finally{
									
									pub_mon.exit();
								}
							}
						}
					});
			}
		}
	}
	
	protected void
	publishSupport()
	{
		DHTTransport	transport = dht.getTransport();
	
		if ( TESTING || !transport.isReachable() ){
			
			DHTTransportContact	local_contact = transport.getLocalContact();
			
				// see if the rendezvous has failed and therefore we are required to find a new one
			
			boolean force = 
				rendezvous_target != null && 
				failed_rendezvous.containsKey( rendezvous_target.getAddress());
			
			if ( rendezvous_local_contact != null && !force ){
				
				if ( local_contact.getAddress().equals( rendezvous_local_contact.getAddress())){
					
						// already running for the current local contact
					
					return;
				}
			}
			
			DHTTransportContact	explicit = (DHTTransportContact)explicit_rendezvous_map.get( local_contact.getAddress());
			
			if ( explicit != null ){
				
				try{
					pub_mon.enter();
				
					rendezvous_local_contact	= local_contact;
					rendezvous_target			= explicit;
				
					runRendezvous();
					
				}finally{
					
					pub_mon.exit();
				}
			}else{
				
				final DHTTransportContact[] new_rendezvous_target			= { null };
				
				DHTTransportContact[]	reachables = dht.getTransport().getReachableContacts();
					
				int reachables_tried	= 0;
				int reachables_skipped	= 0;
				
				final Semaphore sem = plugin_interface.getUtilities().getSemaphore();
				
				for (int i=0;i<reachables.length;i++){
					
					DHTTransportContact	contact = reachables[i];
					
					try{
						pub_mon.enter();
	
							// see if we've found a good one yet
						
						if ( new_rendezvous_target[0] != null ){
						
							break;
						}
						
							// skip any known bad ones
						
						if ( failed_rendezvous.containsKey( contact.getAddress())){
							
							reachables_skipped++;
							
							sem.release();
							
							continue;
						}
					}finally{
						
						pub_mon.exit();
					}
					
					if ( i > 0 ){
						
						try{
							Thread.sleep( 1000 );
							
						}catch( Throwable e ){
							
						}
					}
					
					reachables_tried++;
					
					contact.sendPing(
						new DHTTransportReplyHandlerAdapter()
						{
							public void
							pingReply(
								DHTTransportContact ok_contact )
							{
								// System.out.println( "Punch:" + ok_contact.getString() + " OK" );
								
								try{
									pub_mon.enter();
								
									if ( new_rendezvous_target[0] == null ){
									
										new_rendezvous_target[0] = ok_contact;
									}
								}finally{
									
									pub_mon.exit();
									
									sem.release();
								}
							}
							
							public void
							failed(
								DHTTransportContact 	failed_contact,
								Throwable				e )
							{
								try{
									// System.out.println( "Punch:" + failed_contact.getString() + " Failed" );
									
								}finally{
									
									sem.release();
								}
							}
						});
				}
				
				for (int i=0;i<reachables.length;i++){
	
					sem.reserve();
					
					try{
						pub_mon.enter();
	
						if ( new_rendezvous_target[0] != null ){
						
							rendezvous_target			= new_rendezvous_target[0];					
							rendezvous_local_contact	= local_contact;
						
							log( "Rendezvous found: " + rendezvous_local_contact.getString() + " -> " + rendezvous_target.getString());
	
							runRendezvous();
						
							break;
						}
					}finally{
						
						pub_mon.exit();
					}
				}
			
				if ( new_rendezvous_target[0] == null ){
				
					log( "No rendezvous found: candidates=" + reachables.length +",tried="+ reachables_tried+",skipped=" +reachables_skipped );
							
					try{
						pub_mon.enter();
	
						rendezvous_local_contact	= null;
						rendezvous_target			= null;
						
					}finally{
						
						pub_mon.exit();
					}
				}
			}
		}else{
			
			try{
				pub_mon.enter();

				rendezvous_local_contact	= null;
				rendezvous_target			= null;
				
			}finally{
				
				pub_mon.exit();
			}
		}
	}
	
	protected void
	runRendezvous()
	{
		try{
			pub_mon.enter();

			if ( !rendezvous_thread_running ){
				
				rendezvous_thread_running	= true;
				
				plugin_interface.getUtilities().createThread(
					"DHTNatPuncher:rendevzous",
					new Runnable()
					{
						public void
						run()
						{
							runRendezvousSupport();
						}
					});
			}
		}finally{
			
			pub_mon.exit();
		}
	}
		
	protected void
	runRendezvousSupport()
	{
		DHTTransportContact		current_local		= null;
		DHTTransportContact		current_target		= null;
		
		int	rendevzous_fail_count = 0;
		
		while( true ){
			
			try{
				DHTTransportContact		latest_local;
				DHTTransportContact		latest_target;
				
				try{
					pub_mon.enter();
					
					latest_local	= rendezvous_local_contact;
					latest_target	= rendezvous_target;
				}finally{
					
					pub_mon.exit();
				}
				
				if ( current_local != null || latest_local != null ){
				
						// one's not null, worthwhile further investigation
					
					if ( current_local != latest_local ){
				
							// local has changed, remove existing publish
						
						if ( current_local != null ){
							
							log( "Removing publish for " + current_local.getString() + " -> " + current_target.getString());
							
							dht.remove( 
									getPublishKey( current_local ),
									"DHTNatPuncher: removal of publish",
									new DHTOperationListener()
									{
										public void
										searching(
											DHTTransportContact	contact,
											int					level,
											int					active_searches )
										{}
										
										public void
										found(
											DHTTransportContact	contact )
										{}
										
										public void
										read(
											DHTTransportContact	contact,
											DHTTransportValue	value )
										{}
										
										public void
										wrote(
											DHTTransportContact	contact,
											DHTTransportValue	value )
										{}
										
										public void
										complete(
											boolean				timeout )
										{}
									});
						}
						
						if ( latest_local != null ){
					
							log( "Adding publish for " + latest_local.getString() + " -> " + latest_target.getString());
							
							rendevzous_fail_count	= RENDEZVOUS_PING_FAIL_LIMIT - 2; // only 2 attempts to start with

							dht.put(
									getPublishKey( latest_local ),
									"DHTNatPuncher: publish",
									encodePublishValue( latest_target ),
									DHT.FLAG_SINGLE_VALUE,
									new DHTOperationListener()
									{
										public void
										searching(
											DHTTransportContact	contact,
											int					level,
											int					active_searches )
										{}
										
										public void
										found(
											DHTTransportContact	contact )
										{}
										
										public void
										read(
											DHTTransportContact	contact,
											DHTTransportValue	value )
										{}
										
										public void
										wrote(
											DHTTransportContact	contact,
											DHTTransportValue	value )
										{}
										
										public void
										complete(
											boolean				timeout )
										{}
									});
						}
					}else if ( current_target != latest_target ){
						
							// here current_local == latest_local and neither is null! 
						
							// target changed, update publish
						
						log( "Updating publish for " + latest_local.getString() + " -> " + latest_target.getString());

						rendevzous_fail_count	= RENDEZVOUS_PING_FAIL_LIMIT - 2; // only 2 attempts to start with
						
						dht.put(
								getPublishKey( latest_local ),
								"DHTNatPuncher: update publish",
								encodePublishValue( latest_target ),
								DHT.FLAG_SINGLE_VALUE,
								new DHTOperationListener()
								{
									public void
									searching(
										DHTTransportContact	contact,
										int					level,
										int					active_searches )
									{}
									
									public void
									found(
										DHTTransportContact	contact )
									{}
									
									public void
									read(
										DHTTransportContact	contact,
										DHTTransportValue	value )
									{}
									
									public void
									wrote(
										DHTTransportContact	contact,
										DHTTransportValue	value )
									{}
									
									public void
									complete(
										boolean				timeout )
									{}
								});
					}
				}
				
				current_local	= latest_local;
				current_target	= latest_target;
	
				if ( current_target != null ){
							
					int	bind_result = sendBind( current_target );
					
					if ( bind_result == RESP_OK ){
					
						if ( TRACE ){
							
							System.out.println( "Rendezvous:" + current_target.getString() + " OK" );
						}
												
						rendevzous_fail_count	= 0;
						
						if ( last_ok_rendezvous != current_target ){
							
							last_ok_rendezvous = current_target;
							
							log( "Rendezvous " + latest_target.getString() + " operational" );
						}
					}else{
						
						if ( bind_result == RESP_NOT_OK ){
							
								// denied access
							
							rendevzous_fail_count = RENDEZVOUS_PING_FAIL_LIMIT;
							
						}else{
							
							rendevzous_fail_count++;
						}
						
						if ( rendevzous_fail_count == RENDEZVOUS_PING_FAIL_LIMIT ){
							
							log( "Rendezvous failed: " + current_target.getString());

							if ( TRACE ){
								
								log( "Rendezvous:" + current_target.getString() + " Failed" );
							}
							
							try{
								pub_mon.enter();
							
								failed_rendezvous.put( current_target.getAddress(),"");
								
							}finally{
								
								pub_mon.exit();
							}
							
							publish( true );
						}			
					}					
				}
				
			}catch( Throwable e ){
				
				log(e);
				
			}finally{
				
				try{
					Thread.sleep( RENDEZVOUS_CLIENT_PING_PERIOD );
					
				}catch( Throwable e ){
					
					log(e);
					
					break;
				}
			}
		}
	}
	
	protected byte[]
	sendRequest(
		DHTTransportContact		target,
		byte[]					data )
	{
		try{
			return(
				dht.getTransport().writeReadTransfer(
					new DHTTransportProgressListener()
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
					target,
					transfer_handler_key,
					data,
					TRANSFER_TIMEOUT ));
				
		}catch( DHTTransportException e ){
			
			// log(e); timeout most likely
			
			return( null );
		}		
	}
	
	protected byte[]
	receiveRequest(
		DHTTransportContact		originator,
		byte[]					data )
	{
		try{
			Map	res = receiveRequest( originator, formatters.bDecode( data ));
			
			if ( res == null ){
				
				return( null );
			}
			
			return( formatters.bEncode( res ));
			
		}catch( Throwable e ){
			
			log(e);
			
			return( null );
		}
	}
	
	protected Map
	sendRequest(
		DHTTransportContact		target,
		Map						data )
	{
		try{
			byte[]	res = sendRequest( target, formatters.bEncode( data ));
			
			if ( res == null ){
				
				return( null );
			}
			
			return( formatters.bDecode( res ));
			
		}catch( Throwable e ){
			
			log(e);
			
			return( null );
		}
	}
	
	protected Map
	receiveRequest(
		DHTTransportContact		originator,
		Map						data )
	{
		int	type = ((Long)data.get("type")).intValue();
		
		Map	response = new HashMap();
		
		switch( type ){
		
			case RT_BIND_REQUEST:
			{
				response.put( "type", new Long( RT_BIND_REPLY ));
				
				receiveBind( originator, data, response );
				
				break;
			}
			case RT_PUNCH_REQUEST:
			{
				response.put( "type", new Long( RT_PUNCH_REPLY ));
				
				receivePunch( originator, data, response );
				
				break;
			}
			case RT_CONNECT_REQUEST:
			{
				response.put( "type", new Long( RT_CONNECT_REPLY ));
				
				receiveConnect( originator, data, response );
				
				break;
			}
			default:
			{
				response = null;
				
				break;
			}
		}
		
		return( response );
	}
	
	protected int
	sendBind(
		DHTTransportContact	target )
	{
		try{
			Map	request = new HashMap();
			
			request.put("type", new Long( RT_BIND_REQUEST ));
			
			Map response = sendRequest( target, request );
			
			if ( response == null ){
				
				return( RESP_FAILED );
			}
			
			if (((Long)response.get( "type" )).intValue() == RT_BIND_REPLY ){
				
				int	result = ((Long)response.get("ok")).intValue();

				if ( TRACE ){
					
					System.out.println( "received bind reply: " + (result==0?"failed":"ok" ));
				}
					
				if ( result == 1 ){
					
					return( RESP_OK );
				}
			}
			
			return( RESP_NOT_OK );
			
		}catch( Throwable e ){
			
			log(e);
			
			return( RESP_FAILED );
		}
	}
	
	protected void
	receiveBind(
		DHTTransportContact		originator,
		Map						request,
		Map						response )
	{
		if ( TRACE ){
			System.out.println( "received bind request" );
		}
		
		boolean	ok 	= true;
		boolean	log	= true;
		
		try{
			server_mon.enter();
		
			Object[]	entry = (Object[])rendezvous_bindings.get( originator.getAddress().toString());
			
			if ( entry == null ){
			
				if ( rendezvous_bindings.size() == RENDEZVOUS_SERVER_MAX ){
					
					ok	= false;
				}
			}else{
				
					// already present, no need to log again
				
				log	= false;
			}
			
			if ( ok ){
				
				long	now = plugin_interface.getUtilities().getCurrentSystemTime();
				
				rendezvous_bindings.put( originator.getAddress().toString(), new Object[]{ originator, new Long( now )});
			}
		}finally{
			
			server_mon.exit();
		}
		
		if ( log ){
			
			log( "Rendezvous request from " + originator.getString() + " " + (ok?"accepted":"denied" ));
		}
		
		response.put( "ok", new Long(ok?1:0));
	}
		
	protected int
	sendPunch(
		DHTTransportContact rendezvous,
		DHTTransportContact	target )
	{
		try{
			Map	request = new HashMap();
			
			request.put("type", new Long( RT_PUNCH_REQUEST ));
			
			request.put("target", target.getAddress().toString().getBytes());
			
			Map response = sendRequest( rendezvous, request );
			
			if ( response == null ){
				
				return( RESP_FAILED );
			}
			
			if (((Long)response.get( "type" )).intValue() == RT_PUNCH_REPLY ){
				
				int	result = ((Long)response.get("ok")).intValue();

				if ( TRACE ){
					System.out.println( "received punch reply: " + (result==0?"failed":"ok" ));
				}
				
				if ( result == 1 ){
					
					return( RESP_OK );
				}
			}
			
			return( RESP_NOT_OK );
			
		}catch( Throwable e ){
			
			log(e);
			
			return( RESP_FAILED );
		}
	}
	
	protected void
	receivePunch(
		DHTTransportContact		originator,
		Map						request,
		Map						response )
	{
		if ( TRACE ){
			System.out.println( "received puch request" );
		}
		
		boolean	ok = false;
		
		try{
			server_mon.enter();
		
			String	target_str = new String((byte[])request.get( "target" ));
			
			Object[] entry = (Object[])rendezvous_bindings.get( target_str );
		
			if ( entry != null ){
				
				DHTTransportContact	target = (DHTTransportContact)entry[0];
				
				if ( sendConnect( target, originator ) == RESP_OK ){
				
					ok	= true;
				}
			}
			
			log( "Rendezvous punch request from " + originator.getString() + " to " + target_str + " " + (ok?"initiated":"failed"));

		}finally{
			
			server_mon.exit();
		}
		
		response.put( "ok", new Long(ok?1:0));
	}
	
	protected int
	sendConnect(
		DHTTransportContact target,
		DHTTransportContact	originator )
	{
		try{
			Map	request = new HashMap();
			
			request.put("type", new Long( RT_CONNECT_REQUEST ));
			
			request.put("origin", encodeContact( originator ));
			
			Map response = sendRequest( target, request );
			
			if ( response == null ){
				
				return( RESP_FAILED );
			}
			
			if (((Long)response.get( "type" )).intValue() == RT_CONNECT_REPLY ){
				
				int	result = ((Long)response.get("ok")).intValue();

				if ( TRACE ){
					System.out.println( "received connect reply: " + (result==0?"failed":"ok" ));
				}
					
				if ( result == 1 ){
					
					return( RESP_OK );
				}
			}
			
			return( RESP_NOT_OK );
			
		}catch( Throwable e ){
			
			log(e);
			
			return( RESP_FAILED );
		}
	}
	
	protected void
	receiveConnect(
		DHTTransportContact		rendezvous,
		Map						request,
		Map						response )
	{
		if ( TRACE ){
			System.out.println( "received connect request" );
		}

		boolean	ok = false;
			
			// ensure that we've received this from our current rendezvous node
		
		DHTTransportContact	rt = rendezvous_target;
		
		if ( rt != null && rt.getAddress().equals( rendezvous.getAddress())){
					
			final DHTTransportContact	target = decodeContact( (byte[])request.get( "origin" ));
			
			if ( target != null ){
				
					// ping the origin a few times to try and establish a tunnel
				
				final int[]	pings = {1};
				
				timer.addPeriodicEvent(
							3000,
							new UTTimerEventPerformer()
							{
								public void
								perform(
									UTTimerEvent		event )
								{
									try{
										pub_mon.enter();
									
										if ( pings[0] > 3 ){
										
											event.cancel();
										
											return;
										}else{
								
											pings[0]++;
										}
									}finally{
										
										pub_mon.exit();
									}
										
									target.sendPing(
											new DHTTransportReplyHandlerAdapter()
											{
												public void
												pingReply(
													DHTTransportContact ok_contact )
												{
													if ( TRACE ){
														System.out.println( "tunnel ping ok" );
													}
												}
												
												public void
												failed(
													DHTTransportContact 	failed_contact,
													Throwable				e )
												{
												}
											});
								}
							});
					
				target.sendPing(
					new DHTTransportReplyHandlerAdapter()
					{
						public void
						pingReply(
							DHTTransportContact ok_contact )
						{
							try{
								pub_mon.enter();

								pings[0]	= 100;	// stop periodic above
							
								if ( TRACE ){
									System.out.println( "tunnel ping ok" );
								}
							}finally{
								
								pub_mon.exit();
							}
						}
						
						public void
						failed(
							DHTTransportContact 	failed_contact,
							Throwable				e )
						{
						}
					});
				
				ok	= true;
			}
		}
		
		response.put( "ok", new Long(ok?1:0));
	}
	
	public boolean
	punch(
		DHTTransportContact	target )
	{
		try{
			DHTTransportContact rendezvous = getRendezvous( target );
			
			if ( rendezvous == null ){
				
				return( false );
			}
			
			if ( sendPunch( rendezvous, target ) == RESP_OK ){
				
				log( "    punch to " + target.getString() + " succeeded" );
				
				return( true );
			}
			
		}catch( Throwable e ){
			
			log( e );
		}
		
		log( "    punch to " + target.getString() + " failed" );

		return( false );
	}
	
	public void
	setRendezvous(
		DHTTransportContact		target,
		DHTTransportContact		rendezvous )
	{
		explicit_rendezvous_map.put( target.getAddress(), rendezvous );
		
		if ( target.getAddress().equals( dht.getTransport().getLocalContact().getAddress())){
			
			publish( true );
		}
	}
	
	protected DHTTransportContact
	getRendezvous(
		DHTTransportContact	target )
	{
		DHTTransportContact	explicit = (DHTTransportContact)explicit_rendezvous_map.get( target.getAddress());
		
		if ( explicit != null ){
			
			return( explicit );
		}
		
		byte[]	key = getPublishKey( target );
		
		final DHTTransportValue[]	result_value = {null};
		
		final Semaphore sem = plugin_interface.getUtilities().getSemaphore();
		
		dht.get( 	key, 
					"DHTNatPuncher: lookup for '" + target.getString() + "'",
					(byte)0,
					1,
					RENDEZVOUS_LOOKUP_TIMEOUT,
					false,
					new DHTOperationAdapter()
					{
						public void
						read(
							DHTTransportContact	contact,
							DHTTransportValue	value )
						{
							result_value[0] = value;
							
							sem.release();
						}
						
						public void
						complete(
							boolean				timeout )
						{
							sem.release();
						}
					});
		
		sem.reserve();
		
		DHTTransportContact result = null;
		
		if ( result_value[0] != null ){
			
			byte[]	bytes = result_value[0].getValue();
			
			try{
				ByteArrayInputStream	bais = new ByteArrayInputStream( bytes );
				
				DataInputStream	dis = new DataInputStream( bais );
				
				byte	version = dis.readByte();
				
				if ( version != 0 ){
					
					throw( new Exception( "Unsupported rendezvous version '" + version + "'" ));
				}
				
				result = dht.getTransport().importContact( dis );
				
			}catch( Throwable e ){
				
				log(e);
			}
		}
		
		log( "Lookup of rendezvous for " + target.getString() + " -> " + ( result==null?"None":result.getString()));

		return( result );
	}
	
	protected byte[]
	getPublishKey(
		DHTTransportContact	contact )
	{
		byte[]	id = contact.getID();
		byte[]	suffix = ":DHTNATPuncher".getBytes();
		
		byte[]	res = new byte[id.length + suffix.length];
		
		System.arraycopy( id, 0, res, 0, id.length );
		System.arraycopy( suffix, 0, res, id.length, suffix.length );
		
		return( res );
	}
	
	protected byte[]
   	encodePublishValue(
   		DHTTransportContact	contact )
   	{ 		
		try{
	   		ByteArrayOutputStream	baos = new ByteArrayOutputStream();
	   		
	   		DataOutputStream	dos = new DataOutputStream(baos);
	   		
	   		dos.writeByte( 0 );	// version 
	   		
	   		contact.exportContact( dos );
	   		
	   		dos.close();
	   		
	  		return( baos.toByteArray());

		}catch( Throwable e ){
			
			log( e );
		
			return( new byte[0]);
    	}
   	}
	
	protected byte[]
  	encodeContact(
  		DHTTransportContact	contact )
  	{ 		
		try{
	   		ByteArrayOutputStream	baos = new ByteArrayOutputStream();
	   		
	   		DataOutputStream	dos = new DataOutputStream(baos);
	   		   		
	   		contact.exportContact( dos );
	   		
	   		dos.close();
	   		
	  		return( baos.toByteArray());
	
		}catch( Throwable e ){
			
			log( e );
		
			return( null );
	   	}
  	}
	
	protected DHTTransportContact
	decodeContact(
		byte[]		bytes )
	{
		try{
			ByteArrayInputStream	bais = new ByteArrayInputStream( bytes );
			
			DataInputStream	dis = new DataInputStream( bais );
						
			return( dht.getTransport().importContact( dis ));
			
		}catch( Throwable e ){
			
			log(e);
			
			return( null );
		}
	}
	
	protected void
	log(
		String	str )
	{
		logger.log( "NATPuncher: " + str );
	}
	
	protected void
	log(
		Throwable 	e )
	{
		logger.log( "NATPuncher: error occurred" );
		
		logger.log(e);
	}
}
