/*
 * Created on 04-Dec-2005
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
 * AELITIS, SAS au capital de 40,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.diskmanager.access.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Debug;

public class 
DiskAccessControllerInstance 
{
	private String	name;
	
	private int	max_mb_queued;
	
	private AESemaphore		max_requests_sem;
	
	private groupSemaphore	max_mb_sem;
	
	private long			request_bytes_queued;
	private long			requests_queued;
	
	private requestDispatcher[]	dispatchers;
	
	private long		last_check		= 0;	
	
	private Map			request_map	= new HashMap();	
	
	private static final int REQUEST_NUM_LOG_CHUNK 		= 100;
	private static final int REQUEST_BYTE_LOG_CHUNK 	= 1024*1024;
	
	private int			next_request_num_log	= REQUEST_NUM_LOG_CHUNK;
	private long		next_request_byte_log	= REQUEST_BYTE_LOG_CHUNK;
	
	private static ThreadLocal		tls	= 
		new ThreadLocal()
		{
			public Object
			initialValue()
			{
				return( null );
			}
		};
		
	public
	DiskAccessControllerInstance(
		String	_name,
		int		_max_threads,
		int		_max_requests,
		int		_max_mb )
	{		
		name			= _name;
		max_mb_queued	= _max_mb;
		
		max_requests_sem 	= new AESemaphore("DiskAccessControllerImpl:maxReq", _max_requests );
		
		max_mb_sem 			= new groupSemaphore( max_mb_queued );

		dispatchers	= new requestDispatcher[_max_threads];
		
		for (int i=0;i<_max_threads;i++){
			dispatchers[i]	= new requestDispatcher();
		}
	}
	
	protected void
	queueRequest(
		DiskAccessRequestImpl	request )
	{
		TOTorrent	torrent = request.getFile().getTorrentFile().getTorrent();
		
		requestDispatcher	dispatcher;
		
		synchronized( request_map ){
			
			int	min_index 	= 0;
			int	min_size	= Integer.MAX_VALUE;
			
			long	now = System.currentTimeMillis();
			
			boolean	check = false;
			
			if ( now - last_check > 60000 || now < last_check ){
				
				check		= true;
				last_check	= now;
			}
			
			if ( check ){
				
				Iterator	it = request_map.values().iterator();
				
				while( it.hasNext()){
					
					requestDispatcher	d = (requestDispatcher)it.next();
					
					long	last_active = d.getLastRequestTime();
					
					if ( now - last_active > 60000 ){
												
						it.remove();
						
					}else if ( now < last_active ){
						
						d.setLastRequestTime( now );
					}
				}
			}
			
			dispatcher = (requestDispatcher)request_map.get(torrent);			

			if ( dispatcher == null ){
				
				for (int i=0;i<dispatchers.length;i++){
					
					int	size = dispatchers[i].size();
					
					if ( size == 0 ){
						
						min_index = i;
						
						break;
					}
					
					if ( size < min_size ){
						
						min_size 	= size;
						min_index	= i;
					}
				}
				
				dispatcher = dispatchers[min_index];
				
				request_map.put( torrent, dispatcher );
			}
			
			dispatcher.setLastRequestTime( now );
		}
		
		dispatcher.queue( request );
	}
	
	protected void
	getSpaceAllowance(
		DiskAccessRequestImpl	request )
	{
		int	mb_diff;
				
		synchronized( request_map ){
			
			int	old_mb = (int)(request_bytes_queued/(1024*1024));
			
			request_bytes_queued += request.getSize();
							
			int	new_mb = (int)(request_bytes_queued/(1024*1024));
		
			mb_diff = new_mb - old_mb;
		
			if ( mb_diff > max_mb_queued ){
				
					// if this request is bigger than the max allowed queueable then easiest
					// approach is to bump up the limit
									
				max_mb_sem.releaseGroup( mb_diff - max_mb_queued );
				
				max_mb_queued	= mb_diff;
			}
			
			requests_queued++;
			
			if ( requests_queued >= next_request_num_log ){
				
				System.out.println( "DAC:" + name + ": requests = " + requests_queued );
				
				next_request_num_log += REQUEST_NUM_LOG_CHUNK;
			}
			
			if ( request_bytes_queued >= next_request_byte_log ){
				
				System.out.println( "DAC:" + name + ": bytes = " + request_bytes_queued );
				
				next_request_byte_log += REQUEST_BYTE_LOG_CHUNK;
			}
		}
		
		if ( mb_diff > 0 ){
			
			max_mb_sem.reserveGroup( mb_diff );
		}
	}
	
	protected void
	releaseSpaceAllowance(
		DiskAccessRequestImpl	request )
	{
		int	mb_diff;
		
		synchronized( request_map ){
			
			int	old_mb = (int)(request_bytes_queued/(1024*1024));
			
			request_bytes_queued -= request.getSize();
							
			int	new_mb = (int)(request_bytes_queued/(1024*1024));
		
			mb_diff = old_mb - new_mb;
			
			requests_queued--;
		}
		
		if ( mb_diff > 0 ){
			
			max_mb_sem.releaseGroup( mb_diff );
		}
	}
	
	protected class
	requestDispatcher
	{
		private AEThread	thread;
		private LinkedList	requests 	= new LinkedList();
		private AESemaphore	request_sem	= new AESemaphore("DiskAccessControllerInstance:requestDispatcher" );
		
		private long	last_request_time;
		
		protected void
		queue(
			DiskAccessRequestImpl			request )
		{
			if ( tls.get() != null ){
				
					// let recursive calls straight through
				
				try{
					request.runSupport();
					
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
				
			}else{
				
				max_requests_sem.reserve();
								
				getSpaceAllowance( request );
				
				synchronized( requests ){
					
					requests.add( request );
					
					// System.out.println( "request queue: req = " + requests.size() + ", bytes = " + request_bytes_queued );
					
					request_sem.release();
					
					if ( thread == null ){
						
						thread = 
							new AEThread("DiskAccessController:requestDispatcher", true )
							{
								public void
								runSupport()
								{
									tls.set( this );
									
									while( true ){
										
										if ( request_sem.reserve( 30000 )){
											
											DiskAccessRequestImpl	request;
											
											synchronized( requests ){
	
												request = (DiskAccessRequestImpl)requests.remove(0);
											}
											
											try{
												
												request.runSupport();
												
											}catch( Throwable e ){
												
												Debug.printStackTrace(e);
												
											}finally{
												
												releaseSpaceAllowance( request );
												
												max_requests_sem.release();											
											}
											
										}else{
											
											synchronized( requests ){
	
												if ( requests.size() == 0 ){
													
													thread	= null;
																																					
													break;
												}
											}
										}
									}
								}
							};
							
						thread.start();
					}
				}
			}
		}
	
		protected long
		getLastRequestTime()
		{
			return( last_request_time );
		}
		
		protected void
		setLastRequestTime(
			long	l )
		{
			last_request_time	= l;
		}
		
		protected int
		size()
		{
			return( requests.size());
		}
	}
	
	protected static class
	groupSemaphore
	{
		private int value;
		
		private List	waiters = new LinkedList();
		
		protected
		groupSemaphore(
			int	_value )
		{
			value	= _value;
		}
		
		protected void
		reserveGroup(
			int	num )
		{
			mutableInteger	wait;
			
			synchronized( this ){
				
					// for fairness we only return immediately if we can and there are no waiters
				
				if ( num <= value && waiters.size() == 0 ){
					
					value -= num;
					
					return;
					
				}else{
					
					wait = new mutableInteger( num - value );
										
					value	= 0;
					
					waiters.add( wait );
				}
			}
			
			wait.reserve();
		}
			
		protected void
		releaseGroup(
			int	num )
		{
			synchronized( this ){

				if ( waiters.size() == 0 ){
					
						// no waiters we just increment the value
					
					value += num;
					
				}else{
					
						// otherwise we share num out amongst the waiters in order
					
					while( waiters.size() > 0 ){
						
						mutableInteger wait	= (mutableInteger)waiters.get(0);
						
						int	wait_num = wait.getValue();
						
						if ( wait_num <= num ){
							
								// we've got enough now to release this waiter
							
							wait.release();
							
							waiters.remove(0);
							
							num -= wait_num;
							
						}else{
							
							wait.setValue( wait_num - num );
							
							num	= 0;
							
							break;
						}
					}
					
						// if we have any left over then save it
					
					value = num;
				}
			}
		}
		
		protected static class
		mutableInteger
		{
			private int		i;
			private boolean	released;
			
			protected
			mutableInteger(
				int	_i )
			{
				i	= _i;
			}
			
			protected int
			getValue()
			{
				return( i );
			}
			
			protected void
			setValue(
				int	_i )
			{
				i	= _i;
			}
			
			protected void
			release()
			{
				synchronized( this ){
					
					released	= true;
					
					notify();
				}
			}
			
			protected void
			reserve()
			{
				synchronized( this ){
					
					if ( released ){
						
						return;
					}
					
					try{
						wait();
						
					}catch( InterruptedException e ){
						
						throw( new RuntimeException("Semaphore: operation interrupted" ));
					}
				}
			}
		}
	}
	
	public static void
	main(
		String[]	args )
	{
		final groupSemaphore	sem = new groupSemaphore( 9 );
		
		for (int i=0;i<10;i++){
			
			final long seed = System.currentTimeMillis() + i;
			
			new Thread()
			{
				public void
				run()
				{
					Random 	rand = new Random( seed );
					
					int	count = 0;
					
					while( true ){
						
						int	group = rand.nextInt( 10 );
						
						System.out.println( Thread.currentThread().getName() + " reserving " + group );
						
						sem.reserveGroup( group );
						
						try{
							Thread.sleep(5 + rand.nextInt(5));
							
						}catch( Throwable e ){
						}
												
						sem.releaseGroup( group );
					
						count++;
						
						if ( count %100 == 0 ){
							
							System.out.println( Thread.currentThread().getName() + ": " + count + " ops" );
						}
					}
				}
			}.start();
		}
	}
}
