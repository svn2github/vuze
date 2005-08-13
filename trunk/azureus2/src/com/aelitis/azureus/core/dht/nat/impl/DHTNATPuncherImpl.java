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

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.utils.*;


import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.DHTLogger;
import com.aelitis.azureus.core.dht.nat.*;
import com.aelitis.azureus.core.dht.transport.DHTTransport;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportListener;
import com.aelitis.azureus.core.dht.transport.DHTTransportReplyHandlerAdapter;

public class 
DHTNATPuncherImpl
	implements DHTNATPuncher, UTTimerEventPerformer
{
	private static boolean		TESTING	= false;
	
	private boolean				started;
	
	private	DHT					dht;
	private DHTLogger			logger;
	
	private PluginInterface		plugin_interface;
	
	private static final long	REPUBLISH_TIME_MIN = 5*60*1000;
	
	private long	last_action;
	
	private Monitor	pub_mon;
	private boolean	publish_in_progress;
	
	private volatile DHTTransportContact		rendezvous_local_contact;
	private volatile DHTTransportContact		rendezvous_target;
	
	
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
		
		if ( now < last_action ){
			
			last_action	= now;
			
		}else{
			
			if ( now - last_action >= REPUBLISH_TIME_MIN ){
				
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
			
			rendezvous_local_contact	= null;
			rendezvous_target			= null;
			
			DHTTransportContact[]	reachables = dht.getTransport().getReachableContacts();
						
			final Semaphore sem = plugin_interface.getUtilities().getSemaphore();
			
			for (int i=0;i<reachables.length;i++){
				
				if ( rendezvous_target != null ){
					
					break;
				}
				
				if ( i > 0 ){
					
					try{
						Thread.sleep( 1000 );
						
					}catch( Throwable e ){
						
					}
				}
				
				DHTTransportContact	contact = reachables[i];
				
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
							
								if ( rendezvous_target == null ){
								
									rendezvous_target = ok_contact;
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
				
				if ( rendezvous_target != null ){
				
					rendezvous_local_contact	= local_contact;
					
					runRendezvous( rendezvous_local_contact, rendezvous_target );
					
					break;
				}
			}
			
			if ( rendezvous_target == null ){
				
				logger.log( "No rendezvous found" );		
			}
		}else{
			
			rendezvous_local_contact	= null;
			rendezvous_target			= null;
		}
	}
	
	protected void
	runRendezvous(
		DHTTransportContact		local_contact,
		DHTTransportContact		rendezvous )
	{
		logger.log( "Activating rendezvous " + rendezvous.getString());
		
		
	}
}
