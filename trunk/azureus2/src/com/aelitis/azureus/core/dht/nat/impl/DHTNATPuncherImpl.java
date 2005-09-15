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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

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
	implements DHTNATPuncher, UTTimerEventPerformer
{
	private static boolean		TESTING	= false;
	
	static{
		if ( TESTING ){
			System.out.println( "**** DHTNATPuncher test on ****" );
		}
	}
	
	private static final int	RT_BIND_REQUEST		= 0;
	private static final int	RT_BIND_REPLY		= 1;
	
	
		// DON'T rename/move this class as it'll change the key - maybe it was a bad choice :P
	
	private static byte[]		transfer_handler_key = new SHA1Simple().calculateHash(DHTNATPuncherImpl.class.getName().getBytes());
	
	private boolean				started;
	
	private	DHT					dht;
	private DHTLogger			logger;
	
	private PluginInterface		plugin_interface;
	private Formatters			formatters;
	
	private static final long	REPUBLISH_TIME_MIN = 5*60*1000;
	
	private long	last_action;
	
	private Monitor	pub_mon;
	private boolean	publish_in_progress;
	
	private volatile DHTTransportContact		rendezvous_local_contact;
	private volatile DHTTransportContact		rendezvous_target;
	
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
		
		UTTimer	timer = plugin_interface.getUtilities().createTimer(
							"DHTNATPuncher:refresher", true );
		
		timer.addPeriodicEvent(	REPUBLISH_TIME_MIN, this );
		
		publish( false );
	}
	
	public void
	perform(
		UTTimerEvent		event )
	{
		publish( false );
	}
	
	protected void
	publish(
		final boolean		force )
	{
		long now = plugin_interface.getUtilities().getCurrentSystemTime();
		
		if ( now < last_action && !force ){
			
			last_action	= now;
			
		}else{
			
			if ( force || now - last_action >= REPUBLISH_TIME_MIN ){
				
				last_action	= now;
				
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
		
		final int[]	rendevzous_fail_count = {0};
		
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

							rendevzous_fail_count[0]	= 0;
							
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
						
							// target changed, update publish
						
						log( "Updating publish for " + latest_local.getString() + " -> " + latest_target.getString());

						rendevzous_fail_count[0]	= 0;
						
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
							
					if ( sendBind( current_target )){
					
						System.out.println( "Rendezvous:" + current_target.getString() + " OK" );
												
						rendevzous_fail_count[0]	= 0;
						
					}else{
						
						if ( rendevzous_fail_count[0]++ == 4 ){
							
							log( "Rendezvous:" + current_target.getString() + " Failed" );
							
							try{
								pub_mon.enter();
							
								failed_rendezvous.put( current_target.getAddress(),"");
								
							}finally{
								
								pub_mon.exit();
							}
							
							publish( true );
						}			
					}
					
					/*
					current_target.sendPing(
						new DHTTransportReplyHandlerAdapter()
						{
							public void
							pingReply(
								DHTTransportContact ok_contact )
							{
								System.out.println( "Rendezvous:" + ok_contact.getString() + " OK" );
								
								 //failed( ok_contact, null );
								
								rendevzous_fail_count[0]	= 0;
							}
							
							public void
							failed(
								DHTTransportContact 	failed_contact,
								Throwable				e )
							{
								if ( rendevzous_fail_count[0]++ == 4 ){
									
									log( "Rendezvous:" + failed_contact.getString() + " Failed" );
									
									try{
										pub_mon.enter();
									
										failed_rendezvous.put( failed_contact.getAddress(),"");
										
									}finally{
										
										pub_mon.exit();
									}
									
									publish( true );
								}							
	
							}
						});
						*/
				}
				
			}catch( Throwable e ){
				
				log(e);
				
			}finally{
				
				try{
					Thread.sleep( 30*1000 );
					
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
					30000 ));
				
		}catch( DHTTransportException e ){
			
			log(e);
			
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
			
			default:
			{
				response = null;
				
				break;
			}
		}
		
		return( response );
	}
	
	protected void
	receiveBind(
		DHTTransportContact		originator,
		Map						request,
		Map						response )
	{
		System.out.println( "received bind request" );
	}
	
	protected boolean
	sendBind(
		DHTTransportContact	target )
	{
		try{
			Map	request = new HashMap();
			
			request.put("type", new Long( RT_BIND_REQUEST ));
			
			Map response = sendRequest( target, request );
			
			if ( response == null ){
				
				return( false );
			}
			
			if (((Long)response.get( "type" )).intValue() == RT_BIND_REPLY ){
				
				System.out.println( "received bind reply" );
				
				return( true );
			}
			
			return( false );
			
		}catch( Throwable e ){
			
			log(e);
			
			return( false );
		}
	}
			
	public boolean
	punch(
		DHTTransportContact	target )
	{

		
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
					60000,
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
