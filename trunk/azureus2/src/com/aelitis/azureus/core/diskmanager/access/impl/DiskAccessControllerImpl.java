/*
 * Created on 02-Dec-2005
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

import java.util.*;

import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.diskmanager.access.DiskAccessController;
import com.aelitis.azureus.core.diskmanager.access.DiskAccessRequest;
import com.aelitis.azureus.core.diskmanager.access.DiskAccessRequestListener;
import com.aelitis.azureus.core.diskmanager.cache.CacheFile;

public class 
DiskAccessControllerImpl
	implements DiskAccessController
{
	private AESemaphore	max_read_requests_sem = new AESemaphore("DiskAccessControllerImpl:maxReadReq", 100);
	
	private requestDispatcher[]	read_dispatchers;
	private requestDispatcher[]	write_dispatchers;
	
	
	private Map			read_requests	= new HashMap();
	private Map			write_requests	= new HashMap();
	
	public
	DiskAccessControllerImpl(
		int		_max_read_threads,
		int 	_max_write_threads )
	{		
		read_dispatchers	= new requestDispatcher[_max_read_threads];
		
		for (int i=0;i<_max_read_threads;i++){
			read_dispatchers[i]	= new requestDispatcher();
		}
		
		write_dispatchers	= new requestDispatcher[_max_write_threads];
		
		for (int i=0;i<_max_write_threads;i++){
			write_dispatchers[i] = new requestDispatcher();
		}
	}
	
	public DiskAccessRequest
	queueReadRequest(
		CacheFile					file,
		long						offset,
		DirectByteBuffer			buffer,
		DiskAccessRequestListener	listener )
	{
		DiskAccessRequestImpl	request = 
			new DiskAccessRequestImpl( file, offset, buffer, listener, DiskAccessRequestImpl.OP_READ );

		queueRequest( read_requests, read_dispatchers, request );
		
		return( request );
	}
	
	public DiskAccessRequest
	queueWriteRequest(
		CacheFile					file,
		long						offset,
		DirectByteBuffer			buffer,
		boolean						free_buffer,
		DiskAccessRequestListener	listener )
	{
		DiskAccessRequestImpl	request = 
			new DiskAccessRequestImpl( 
					file, 
					offset, 
					buffer, 
					listener, 
					free_buffer?DiskAccessRequestImpl.OP_WRITE_AND_FREE:DiskAccessRequestImpl.OP_WRITE );
	
		queueRequest( write_requests, write_dispatchers, request );
		
		return( request );	
	}
	
	protected void
	queueRequest(
		Map						request_map,
		requestDispatcher[]		request_dispatchers,
		DiskAccessRequestImpl	request )
	{
		TOTorrent	torrent = request.getFile().getTorrentFile().getTorrent();
		
		requestDispatcher	dispatcher;
		
		synchronized( request_map ){
			
			dispatcher = (requestDispatcher)request_map.get(torrent);
			
			int	min_index 	= 0;
			int	min_size	= Integer.MAX_VALUE;
			
			if ( dispatcher == null ){
				
				for (int i=0;i<request_dispatchers.length;i++){
					
					int	size = request_dispatchers[i].size();
					
					if ( size == 0 ){
						
						min_index = i;
						
						break;
					}
					
					if ( size < min_size ){
						
						min_size 	= size;
						min_index	= i;
					}
				}
				
				dispatcher = request_dispatchers[min_index];
				
				request_map.put( torrent, dispatcher );
			}
		}
		
		dispatcher.queue( request );
	}
	
	protected class
	requestDispatcher
	{
		private AEThread	thread;
		private LinkedList	requests 	= new LinkedList();
		private AESemaphore	request_sem	= new AESemaphore("DiskAccessController:requestDispatcher" );
		
		protected void
		queue(
			DiskAccessRequestImpl	request )
		{
			max_read_requests_sem.reserve();
			
			synchronized( this ){
				
				requests.add( request );
				
				request_sem.release();
				
				if ( thread == null ){
					
					thread = 
						new AEThread("DiskAccessController:requestDispatcher", true )
						{
							public void
							runSupport()
							{
								while( true ){
									
									if ( request_sem.reserve( 10000 )){
										
										DiskAccessRequestImpl	request;
										
										synchronized( this ){

											request = (DiskAccessRequestImpl)requests.remove(0);
										}
										
										try{
											
											request.runSupport();
											
										}catch( Throwable e ){
											
											Debug.printStackTrace(e);
											
										}finally{
											
											max_read_requests_sem.release();
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
		
		protected int
		size()
		{
			return( requests.size());
		}
	}
}
