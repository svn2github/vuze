/*
 * Created on 29-Jul-2004
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

package org.gudy.azureus2.core3.tracker.server.impl;

/**
 * @author parg
 *
 */

import java.util.*;
import java.net.*;

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.util.*;

public class 
TRTrackerServerNATChecker 
{
	protected static TRTrackerServerNATChecker		singleton	= new TRTrackerServerNATChecker();
	
	protected static final int THREAD_POOL_SIZE		= 32;
	protected static final int CHECK_TIME_LIMIT		= 10000;
	protected static final int CHECK_QUEUE_LIMIT	= 2048; 
	
	protected static TRTrackerServerNATChecker
	getSingleton()
	{
		return( singleton );
	}
	
	protected boolean		enabled;
	protected ThreadPool	thread_pool;
	
	protected List			check_queue		= new ArrayList();
	protected Semaphore		check_queue_sem	= new Semaphore();
	
	protected
	TRTrackerServerNATChecker()
	{
		String	enable_param = "Tracker NAT Check Enable";
		
		COConfigurationManager.addParameterListener(
			enable_param,
			new ParameterListener()
			{
				public void 
				parameterChanged(
					String parameter_name)
				{
					checkConfig( parameter_name );
				}
			});
		
		checkConfig( enable_param );
	}
	
	protected synchronized void
	checkConfig(
		String	enable_param )
	{
		enabled = COConfigurationManager.getBooleanParameter( enable_param );
		
		if ( thread_pool == null ){
			
			thread_pool	= new ThreadPool("Tracker NAT Checker", THREAD_POOL_SIZE );
			
			thread_pool.setExecutionLimit( CHECK_TIME_LIMIT );
			
			Thread	dispatcher_thread = 
				new AEThread( "Tracker NAT Checker Dispatcher" )
				{
					public void
					run()
					{
						while(true){
							
							check_queue_sem.reserve();
							
							ThreadPoolTask	task;
							
							synchronized( check_queue ){
								
								task = (ThreadPoolTask)check_queue.remove(0);
							}
							
							try{
								thread_pool.run( task );
								
							}catch( Throwable e ){
								
								e.printStackTrace();
							}
						}
					}
				};
				
			dispatcher_thread.setDaemon( true );
			
			dispatcher_thread.start();
		}
	}

	protected boolean
	addNATCheckRequest(
		final String								host,
		final int									port,
		final TRTrackerServerNatCheckerListener		listener )
	{		
		if ((!enabled) || thread_pool == null ){
			
			return( false );
		}
		
		synchronized( check_queue ){
			
			if ( check_queue.size() > CHECK_QUEUE_LIMIT ){
				
				Debug.out( "NAT Check queue size too large, check skipped" );
				
				listener.NATCheckComplete( true );
				
			}else{
				
				check_queue.add(
					new ThreadPoolTask()
					{
						protected	Socket	socket;
						
						public void
						run()
						{
							boolean	ok = false;
							
							long	start = SystemTime.getCurrentTime();
							
							try{
								InetSocketAddress address = new InetSocketAddress( host, port );
								
								socket = new Socket();
								
								socket.connect( address, CHECK_TIME_LIMIT );
							
								ok	= true;
								
								socket.close();
								
								socket	= null;
								
							}catch( Throwable e ){
								
							}finally{
								
								long	now = SystemTime.getCurrentTime();

								// System.out.println( "NAT Check: " + host + ":" + port + " -> " + ok +", time = " + (now-start) + ", queue = " + check_queue.size());
								
								listener.NATCheckComplete( ok );
								
								if ( socket != null ){
									
									try{
										socket.close();
										
									}catch( Throwable e ){
									}
								}
							}
						}
						
						public void
						interruptTask()
						{
							if ( socket != null ){
								
								try{
									socket.close();
									
								}catch( Throwable e ){
								}
							}					
						}
					});
				
				check_queue_sem.release();
			}
		}
		
		return( true );
	}
}
