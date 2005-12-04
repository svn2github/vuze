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
import java.util.Map;

import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Debug;

public class 
DiskAccessControllerInstance 
{
	public static int	MAX_MB_QUEUED	= 8;
	
	private AESemaphore	max_requests_sem 	= new AESemaphore("DiskAccessControllerImpl:maxReq", 100 );
	private AESemaphore	max_mb_sem 			= new AESemaphore("DiskAccessControllerImpl:maxReadReq", MAX_MB_QUEUED );
	
	private long		request_bytes_queued	= 0;
	
	private requestDispatcher[]	dispatchers;
	
	private long		last_check		= 0;	
	
	private Map			request_map	= new HashMap();	
	
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
		int		_max_threads )
	{		
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
								
				int	mb_diff;
				
				synchronized( request_map ){
					
					int	old_mb = (int)(request_bytes_queued/(1024*1024));
					
					request_bytes_queued += request.getSize();
									
					int	new_mb = (int)(request_bytes_queued/(1024*1024));
				
					mb_diff = new_mb - old_mb;
				
					if ( mb_diff > MAX_MB_QUEUED ){
						
							// if this request is bigget than the max allowed queueable then easiest
							// approach is to bump up the limit
						
						for (int i=0;i<mb_diff-MAX_MB_QUEUED;i++){
							
							max_mb_sem.release();
						}
						
						MAX_MB_QUEUED	= mb_diff;
					}
				}
					
				for (int i=0;i<mb_diff;i++){
					
					max_mb_sem.reserve();
				}
				
				synchronized( this ){
					
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
											
											synchronized( this ){
	
												request = (DiskAccessRequestImpl)requests.remove(0);
											}
											
											try{
												
												request.runSupport();
												
											}catch( Throwable e ){
												
												Debug.printStackTrace(e);
												
											}finally{
												
												int	mb_diff;
												
												synchronized( request_map ){
													
													int	old_mb = (int)(request_bytes_queued/(1024*1024));
													
													request_bytes_queued -= request.getSize();
																	
													int	new_mb = (int)(request_bytes_queued/(1024*1024));
												
													mb_diff = old_mb - new_mb;
												}
												
												for (int i=0;i<mb_diff;i++){
													
													max_mb_sem.release();
												}
												
												max_requests_sem.release();											
											}
											
										}else{
											
											synchronized( this ){
	
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
}
