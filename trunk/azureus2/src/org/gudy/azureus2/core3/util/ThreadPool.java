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
	protected static final int	IDLE_LINGER_TIME	= 10000;
	
	protected static final boolean	LOG_WARNINGS	= false;
	protected static final int		WARN_TIME		= 10000;
	
	protected static List		busy_pools			= new ArrayList();
	protected static boolean	busy_pool_timer_set	= false;
	
	protected static void
	checkAllTimeouts()
	{
		List	pools;	
	
			// copy the busy pools to avoid potential deadlock due to synchronization
			// nestings
		
		synchronized( busy_pools ){
			
			pools	= new ArrayList( busy_pools );
		}
		
		for (int i=0;i<pools.size();i++){
			
			((ThreadPool)pools.get(i)).checkTimeouts();
		}
	}
	
	
	private String	name;
	private int		thread_name_index	= 1;
	
	private long	execution_limit;
	
	private Stack	thread_pool;
	private List	busy;
	private boolean	queue_when_full;
	private List	task_queue	= new ArrayList();
	
	private AESemaphore	thread_sem;
	
	public
	ThreadPool(
		String	_name,
		int		max_size )
	{
		this( _name, max_size, false );
	}
	
	public
	ThreadPool(
		String	_name,
		int		_max_size,
		boolean	_queue_when_full )
	{
		name			= _name;
		queue_when_full	= _queue_when_full;
		
		thread_sem = new AESemaphore( "ThreadPool::" + name, _max_size );
		
		thread_pool	= new Stack();
					
		busy		= new ArrayList( _max_size );
	}

	public void
	setExecutionLimit(
		long		millis )
	{
		execution_limit	= millis;
	}
	
	public threadPoolWorker
	run(
		AERunnable	runnable )
	{
		// System.out.println( "Thread pool:" + name + " - sem = " + thread_sem.getValue() + ", queue = " + task_queue.size());
		
			// not queueing, grab synchronous sem here
		
		if ( !queue_when_full ){
		
			thread_sem.reserve();
		}
						
		threadPoolWorker allocated_worker;
						
		synchronized( this ){
		
				// reserve if available is non-blocking
			
			if ( queue_when_full && !thread_sem.reserveIfAvailable()){
			
				allocated_worker	= null;
			
				task_queue.add( runnable );
				
			}else{
				
				if ( thread_pool.isEmpty()){
							
					allocated_worker = new threadPoolWorker();	
		
				}else{
									
					allocated_worker = (threadPoolWorker)thread_pool.pop();
				}
				
				if ( runnable instanceof ThreadPoolTask ){
					
					((ThreadPoolTask)runnable).worker = allocated_worker;
				}
				
				allocated_worker.run( runnable );
			}
		}
		
		return( queue_when_full?null:allocated_worker );
	}
	
	public AERunnable[]
	getQueuedTasks()
	{
		synchronized( this ){

			AERunnable[]	res = new AERunnable[task_queue.size()];
			
			task_queue.toArray(res);
			
			return( res );
		}
	}
	
	public boolean
	isQueued(
		AERunnable	task )
	{
		synchronized( this ){

			return( task_queue.contains( task ));
		}
	}
	
	public AERunnable[]
	getRunningTasks()
	{
		List	runnables	= new ArrayList();
		
		synchronized( this ){

			Iterator	it = busy.iterator();
			
			while( it.hasNext()){
				
				threadPoolWorker	worker = (threadPoolWorker)it.next();
				
				AERunnable	runnable = worker.getRunnable();
				
				if ( runnable != null ){
					
					runnables.add( runnable );
				}
			}
		}
		
		AERunnable[]	res = new AERunnable[runnables.size()];
			
		runnables.toArray(res);
			
		return( res );
	}
	
	protected void
	checkTimeouts()
	{
		synchronized( ThreadPool.this ){
		
			long	now = SystemTime.getCurrentTime();
			
			for (int i=0;i<busy.size();i++){
					
				threadPoolWorker	x = (threadPoolWorker)busy.get(i);
			
				long	elapsed = now - x.run_start_time ;
					
				if ( elapsed > ( WARN_TIME * (x.warn_count+1))){
		
					x.warn_count++;
					
					if ( LOG_WARNINGS ){
						
						Debug.out( x.getWorkerName() + ": running, elapsed = " + elapsed + ", state = " + x.state );
					}
					
					if ( execution_limit > 0 && elapsed > execution_limit ){
						
						if ( LOG_WARNINGS ){
							
							Debug.out( x.getWorkerName() + ": interrupting" );
						}
						
						AERunnable r = x.runnable;

						try{
							if ( r instanceof ThreadPoolTask ){
								
								((ThreadPoolTask)r).interruptTask();
								
							}else{
								
								x.worker_thread.interrupt();
							}
						}catch( Throwable e ){
							
							Debug.printStackTrace( e );
						}
					}
				}
			}
		}
	}
	
	public class
	threadPoolWorker
	{
		private String	worker_name;
		
		private Thread	worker_thread;
		
		private AESemaphore my_sem = new AESemaphore("TPWorker");
		
		private AERunnable	runnable;
		private long		run_start_time;
		private int			warn_count;
		
		private String	state	= "<none>";
		
		protected
		threadPoolWorker()
		{
			worker_name = name + "[" + (thread_name_index++) +  "]";
			
			worker_thread = new AEThread( worker_name )
				{
					public void 
					runSupport()
					{
						boolean	time_to_die = false;
			
outer:
						while(true){
							
							try{
								while( !my_sem.reserve(IDLE_LINGER_TIME)){
																		
									synchronized( ThreadPool.this ){
										
										if ( runnable == null ){
											
											time_to_die	= true;
											
											thread_pool.remove( threadPoolWorker.this );
																						
											break outer;
										}
									}
								}
								
								while( runnable != null ){
									
									try{
										
										synchronized( ThreadPool.this ){
												
											run_start_time	= SystemTime.getCurrentTime();
											warn_count		= 0;
											
											busy.add( threadPoolWorker.this );
											
											if ( busy.size() == 1 ){
												
												synchronized( busy_pools ){
													
													busy_pools.add( ThreadPool.this );
													
													if  ( !busy_pool_timer_set ){
														
															// we have to defer this action rather
															// than running as a static initialiser
															// due to the dependency between
															// ThreadPool, Timer and ThreadPool again
														
														busy_pool_timer_set	= true;
														
														SimpleTimer.addPeriodicEvent(
																WARN_TIME,
																new TimerEventPerformer()
																{
																	public void
																	perform(
																		TimerEvent	event )
																	{
																		checkAllTimeouts();
																	}
																});
													}
												}
											}
										}
										
										if ( runnable instanceof ThreadPoolTask ){
										
											ThreadPoolTask	tpt = (ThreadPoolTask)runnable;
											
											try{
												tpt.taskStarted();
												
												runnable.run();
												
											}finally{
												
												tpt.taskCompleted();
											}
										}else{
											
											runnable.run();
										}
										
									}catch( Throwable e ){
										
										Debug.printStackTrace( e );		
	
									}finally{
																					
										synchronized( ThreadPool.this ){
												
											long	elapsed = SystemTime.getCurrentTime() - run_start_time;
											
											if ( elapsed > WARN_TIME && LOG_WARNINGS ){
												
												Debug.out( getWorkerName() + ": terminated, elapsed = " + elapsed + ", state = " + state );
											}
											
											busy.remove( threadPoolWorker.this );
											
											if ( busy.size() == 0 ){
												
												synchronized( busy_pools ){
												
													busy_pools.remove( ThreadPool.this );
												}
											}
										
											if ( task_queue.size() > 0 ){
												
												runnable = (AERunnable)task_queue.remove(0);
												
											}else{
											
												runnable	= null;
											}
										}
									}
								}
							}catch( Throwable e ){
									
								Debug.printStackTrace( e );
											
							}finally{
										
								if ( !time_to_die ){
									
									synchronized( ThreadPool.this ){
															
										thread_pool.push( threadPoolWorker.this );
									}
								
									thread_sem.release();
								}
							}
						}
					}
				};
				
			worker_thread.setDaemon(true);
			
			worker_thread.start();
		}
		
		public void
		setState(
			String	_state )
		{
			//System.out.println( "state = " + _state );
			
			state	= _state;
		}
		
		public String
		getState()
		{
			return( state );
		}
		
		protected String
		getWorkerName()
		{
			return( worker_name );
		}
		
		protected void
		run(
			AERunnable	_runnable )
		{
			runnable	= _runnable;
			
			my_sem.release();
		}
		
		protected AERunnable
		getRunnable()
		{
			return( runnable );
		}
	}
	
	public String
	getName()
	{
		return( name );
	}
}
