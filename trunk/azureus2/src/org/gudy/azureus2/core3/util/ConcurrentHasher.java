/*
 * Created on 09-Sep-2004
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

package org.gudy.azureus2.core3.util;

/**
 * @author parg
 *
 */

import java.util.*;
import java.nio.ByteBuffer;

public class 
ConcurrentHasher 
{
	
	protected static ConcurrentHasher		singleton	= new ConcurrentHasher();
	
	
	protected int			processor_num;
	protected List			requests		= new LinkedList();
	protected List			hashers			= new ArrayList();
	
	protected AESemaphore		request_sem		= new AESemaphore("ConcHashReqQ");
	protected AESemaphore		scheduler_sem	= new AESemaphore("ConcHashSched");
		
	protected
	ConcurrentHasher()
	{
			// TODO: number of processors can apparently change....
			// so periodically grab num + reserve/release as necessary
		
		processor_num = Runtime.getRuntime().availableProcessors();
		
		for (int i=0;i<processor_num;i++){
			
			scheduler_sem.release();
		}
	
		final ThreadPool pool	= new ThreadPool( "ConcurrentHasher", 64 );
		
		AEThread	scheduler = 
			new AEThread("CuncurrentHasher:scheduler")
			{
				public void
				run()
				{
					while(true){
						
							// first get a request to run
						
						request_sem.reserve();
						
							// now get permission to run a request
						
						scheduler_sem.reserve();
						
							// now extract the request
						
						final ConcurrentHasherRequest	req;
						final SHA1Hasher				hasher;
						
						synchronized( requests ){
							
							req	= (ConcurrentHasherRequest)requests.remove(0);
							
							if ( hashers.size() == 0 ){
								
								hasher = new SHA1Hasher();
								
							}else{
								
								hasher	= (SHA1Hasher)hashers.remove( hashers.size()-1 );
							}
						}
						
						pool.run( 
								new Runnable()
								{
									public void
									run()
									{
										try{											
											req.run( hasher );
											
										}finally{
											
											hashers.add( hasher );
											
											scheduler_sem.release();
										}
									}
								});
													
					}
				}
			};
	
		scheduler.setDaemon( true );
		
		scheduler.start();
	}
	
	public static ConcurrentHasher
	getSingleton()
	{
		return( singleton );
	}
	
		/**
		 * add a synchronous request - on return it will have run (or been cancelled)
	     */
	
	public ConcurrentHasherRequest
	addRequest(
		ByteBuffer		buffer,
		long			priority )
	{
		return( addRequest( buffer, priority, null ));
	}
	
		/**
		 * Add an asynchronous request if listener supplied, sync otherwise 
		 * @param buffer
		 * @param priority
		 * @param listener
		 * @return
		 */
	
	public ConcurrentHasherRequest
	addRequest(
		ByteBuffer							buffer,
		long								priority,
		ConcurrentHasherRequestListener		listener )
	{
		final ConcurrentHasherRequest	req = new ConcurrentHasherRequest( this, buffer, priority, listener );
			
		synchronized( requests ){
			
			boolean	done = false;
			
			for (int i=0;i<requests.size();i++){
				
				ConcurrentHasherRequest r = (ConcurrentHasherRequest)requests.get(i);
				
				if ( r.getPriority() > priority ){
					
					requests.add( i, req );
					
					done	= true;
					
					break;
				}
			}
			
			if ( !done ){
				
				requests.add( req );
			}
		}
		
		request_sem.release();
	
		return( req );
	}
	
	public static void
	main(
		String[]	args )
	{
		final ConcurrentHasher	hasher = ConcurrentHasher.getSingleton();
		
		int		threads			= 2;
		
		final long	buffer_size		= 32*1024;
		final long	loop			= 1024;
		
		for (int i=0;i<threads;i++){
			
			final int	f_i	= i;
			
			new Thread()
			{
				public void
				run()
				{
					ByteBuffer	buffer = ByteBuffer.allocate((int)buffer_size);
					
					long	start = System.currentTimeMillis();
					
					for (int j=0;j<loop;j++){
						
						buffer.position(0);
						
						hasher.addRequest( buffer, 0 ).getResult();
					}
					
					long	elapsed = System.currentTimeMillis() - start;
					
					System.out.println( 
							"elapsed = " + elapsed + ", " + 
							((loop*buffer_size*1000)/elapsed) + " B/sec" );
				}
			}.start();
		}
	}
}
