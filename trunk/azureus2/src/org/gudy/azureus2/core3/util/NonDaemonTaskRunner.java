/*
 * File    : NonDaemonTaskRunner.java
 * Created : 29-Dec-2003
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
NonDaemonTaskRunner 
{
	public static final int	LINGER_PERIOD	= 2500;
	
	protected static NonDaemonTaskRunner	singleton;
	
	protected Stack		tasks		= new Stack();
	protected Semaphore	task_sem	= new Semaphore();
	
	protected Thread	current_thread;
	
	protected synchronized static NonDaemonTaskRunner
	getSingleton()
	{
		if ( singleton == null ){
			
			singleton = new NonDaemonTaskRunner();
		}
		
		return( singleton );
	}
	
	public static Object
	run(
		NonDaemonTask	target )
	
		throws Throwable
	{
		return(getSingleton().runSupport( target ));
	}
	
	protected Object
	runSupport(
		NonDaemonTask	target )
	
		throws Throwable
	{
		taskWrapper	wrapper = new taskWrapper( target );
		
		synchronized( tasks ){
			
			tasks.push( wrapper );
			
			task_sem.release();
			
			if ( current_thread == null ){
				
				final Semaphore wait_sem = new Semaphore();
				
				current_thread = new AEThread("NonDaemonTaskRunner" )
					{
						public void
						run()
						{
							wait_sem.release();
							
							// System.out.println( "non daemon starts" );
							
							while(true){
								
								task_sem.reserve(LINGER_PERIOD);
								
								synchronized( tasks ){
									
									if ( tasks.isEmpty()){
	
										current_thread = null;
																				
										break;
										
									}else{
										
										taskWrapper t = (taskWrapper)tasks.pop();
																					
										t.run();
									}
								}
							}
							
							// System.out.println( "non daemon ends" );
						}
					};
						
				current_thread.setDaemon(false);
				
				current_thread.start();	
				
				wait_sem.reserve();
			}
		}
		
		return( wrapper.waitForResult());
	}
	
	protected class
	taskWrapper
	{
		protected NonDaemonTask	task;
		protected Semaphore		sem;
		
		protected Object	  	result;
		protected Throwable  	exception;
		
		protected
		taskWrapper(
			NonDaemonTask	_task )
		{
			task		= _task;
			sem			= new Semaphore();
		}
		
		protected void
		run()
		{
			try{
				result = task.run();
				
			}catch( Throwable e ){
								
				exception	= e;
				
			}finally{
		
				sem.release();
			}
		}
		
		protected Object
		waitForResult()
		
			throws Throwable
		{
			sem.reserve();
			
			if ( exception != null ){
							
				throw( exception );
			}
			
			return( result );
		}
	}
}
