/*
 * File    : ThreadPool.java
 * Created : 21-Nov-2003
 * By      : parg
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.core3.util;

/**
 * @author parg
 *
 */

import java.util.*;

public class 
ThreadPool 
{
	protected Stack		thread_pool = new Stack();
	protected Semaphore	thread_sem;
	
	public
	ThreadPool(
		int		max_size )
	{
		thread_sem = new Semaphore( max_size );
	}

	public void
	run(
		Runnable	runnable )
	{
		thread_sem.reserve();
						
		worker w;
						
		synchronized( this ){
							
			if ( thread_pool.isEmpty()){
						
				w = new worker();	
	
			}else{
								
				w = (worker)thread_pool.pop();
			}
			
			w.run( runnable );
		}
	}
	
	protected class
	worker
	{
		protected Semaphore my_sem = new Semaphore();
		
		protected Runnable	runnable;
		
		protected
		worker()
		{
			Thread t = new Thread()
				{
					public void 
					run()
					{
						while(true){
							
							try{
								my_sem.reserve();
									
								try{
											
									runnable.run();
										
								}finally{
																					
									runnable	= null;
								}
							}catch( Throwable e ){
									
								e.printStackTrace();
											
							}finally{
													
								synchronized( ThreadPool.this ){
															
									thread_pool.push( worker.this );
								}
															
								thread_sem.release();
							}
						}
					}
				};
				
			t.setDaemon(true);
			
			t.start();
		}
		
		protected void
		run(
			Runnable	_runnable )
		{
			runnable	= _runnable;
			
			my_sem.release();
		}
	}
}
