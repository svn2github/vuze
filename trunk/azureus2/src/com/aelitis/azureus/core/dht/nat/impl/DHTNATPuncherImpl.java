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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.HashSet;
import java.util.Set;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.utils.*;


import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.DHTLogger;
import com.aelitis.azureus.core.dht.DHTOperationListener;
import com.aelitis.azureus.core.dht.nat.*;
import com.aelitis.azureus.core.dht.transport.DHTTransport;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportListener;
import com.aelitis.azureus.core.dht.transport.DHTTransportReplyHandlerAdapter;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

public class 
DHTNATPuncherImpl
	implements DHTNATPuncher, UTTimerEventPerformer
{
	private static boolean		TESTING	= false;
	
	private boolean				started;
	
	private	DHT					dht;
	private int					network;
	private DHTLogger			logger;
	
	private PluginInterface		plugin_interface;
	
	private static final long	REPUBLISH_TIME_MIN = 5*60*1000;
	
	private long	last_action;
	
	private Monitor	pub_mon;
	private boolean	publish_in_progress;
	
	private volatile DHTTransportContact		rendezvous_local_contact;
	private volatile DHTTransportContact		rendezvous_target;
	
	private Set		failed_rendezvous	= new HashSet();
	
	private boolean	rendezvous_thread_running;
	
	public
	DHTNATPuncherImpl(
		DHT			_dht )
	{
		dht	= _dht;
	
		logger	= dht.getLogger();
		
		plugin_interface	= dht.getLogger().getPluginInterface();
		
		pub_mon	= plugin_interface.getUtilities().getMonitor();
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
								publishSupport( force );
								
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
	publishSupport(
		boolean		force )
	{
		DHTTransport	transport = dht.getTransport();
	
		if ( TESTING || !transport.isReachable() ){
			
			DHTTransportContact	local_contact = transport.getLocalContact();
			
			if ( rendezvous_local_contact != null && !force ){
				
				if ( local_contact.getAddress().equals( rendezvous_local_contact.getAddress())){
					
						// already running
					
					return;
				}
			}
			
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
					
					if ( failed_rendezvous.contains( contact.getAddress())){
						
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
					
						logger.log( "Rendezvous found: " + rendezvous_local_contact.getString() + " -> " + rendezvous_target.getString());

						runRendezvous();
					
						break;
					}
				}finally{
					
					pub_mon.exit();
				}
			}
			
			if ( new_rendezvous_target[0] == null ){
				
				logger.log( "No rendezvous found: candidates=" + reachables.length +",tried="+ reachables_tried+",skipped=" +reachables_skipped );
							
				try{
					pub_mon.enter();

					rendezvous_local_contact	= null;
					rendezvous_target			= null;
					
				}finally{
					
					pub_mon.exit();
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
							
							logger.log( "Removing publish for " + current_local.getString() + " -> " + current_target.getString());
							
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
					
							logger.log( "Adding publish for " + latest_local.getString() + " -> " + latest_target.getString());

							rendevzous_fail_count[0]	= 0;
							
							dht.put(
									getPublishKey( latest_local ),
									"DHTNatPuncher: publish",
									getPublishValue( latest_target ),
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
						
						logger.log( "Updating publish for " + latest_local.getString() + " -> " + latest_target.getString());

						rendevzous_fail_count[0]	= 0;
						
						dht.put(
								getPublishKey( latest_local ),
								"DHTNatPuncher: update publish",
								getPublishValue( latest_target ),
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
							
					current_target.sendPing(
						new DHTTransportReplyHandlerAdapter()
						{
							public void
							pingReply(
								DHTTransportContact ok_contact )
							{
								// System.out.println( "Rendezvous:" + ok_contact.getString() + " OK" );
								
								// failed( ok_contact, null );
								
								rendevzous_fail_count[0]	= 0;
							}
							
							public void
							failed(
								DHTTransportContact 	failed_contact,
								Throwable				e )
							{
								if ( rendevzous_fail_count[0]++ == 4 ){
									
									logger.log( "Rendezvous:" + failed_contact.getString() + " Failed" );
									
									try{
										pub_mon.enter();
									
										failed_rendezvous.add( failed_contact.getAddress());
										
									}finally{
										
										pub_mon.exit();
									}
									
									publish( true );
								}							
	
							}
						});
				}
				
			}catch( Throwable e ){
				
				logger.log(e);
				
			}finally{
				
				try{
					Thread.sleep( 30*1000 );
					
				}catch( Throwable e ){
					
					logger.log(e);
					
					break;
				}
			}
		}
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
   	getPublishValue(
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
			
			logger.log( e );
		
			return( new byte[0]);
    	}
   	}
}
