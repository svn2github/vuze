/*
 * File    : Timer.java
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

public class Timer
	implements Runnable
{	
	ThreadPool	thread_pool;
		
	Set	events = new TreeSet();
	
	Object	lock = new Object(){};
	
	public
	Timer(
		String	name,
		int		thread_pool_size )
	{
		thread_pool = new ThreadPool(name,thread_pool_size);
		
		Thread t = new Thread(this, "Timer:" + name );
		
		t.setDaemon( true );
			
		t.start();
	}
	
	public void
	run()
	{
		while(true){
			
			try{
				List	events_to_run = new ArrayList();
				
				synchronized(this){
					
					if ( events.isEmpty()){
						
						// System.out.println( "waiting forever" );
						
						this.wait();
						
					}else{
						long	now = System.currentTimeMillis();
						
						TimerEvent	next_event = (TimerEvent)events.iterator().next();
						
						long	delay = next_event.getWhen() - now;
						
						if ( delay > 0 ){
						
							// System.out.println( "waiting for " + delay );
							
							this.wait(delay);
						}
					}
				
					long	now = System.currentTimeMillis();
					
					Iterator	it = events.iterator();
					
					while( it.hasNext()){
						
						TimerEvent	event = (TimerEvent)it.next();
						
						if ( event.getWhen() <= now ){
							
							events_to_run.add( event );
							
							it.remove();
						}
					}
				}
				
				for (int i=0;i<events_to_run.size();i++){
					
					// System.out.println( "firing event");
					
					thread_pool.run(((TimerEvent)events_to_run.get(i)).getRunnable());
				}
				
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
		}
	}
	
	public synchronized TimerEvent
	addEvent(
		long				when,
		TimerEventPerformer	performer )
	{
		return( addEvent( System.currentTimeMillis(), when, performer ));
	}
	
	public synchronized TimerEvent
	addEvent(
		long				creation_time,
		long				when,
		TimerEventPerformer	performer )
	{
		TimerEvent	event = new TimerEvent( this, creation_time, when, performer );
		
		events.add( event );
		
		// System.out.println( "event added (" + when + ") - queue = " + events.size());
				
		notify();
		
		return( event );
	}
	
	public synchronized TimerEventPeriodic
	addPeriodicEvent(
		long				frequency,
		TimerEventPerformer	performer )
	{
		TimerEventPeriodic periodic_performer = new TimerEventPeriodic( this, frequency, performer );
				
		return( periodic_performer );
	}
	
	protected synchronized void
	cancelEvent(
		TimerEvent	event )
	{
		if ( events.contains( event )){
			
			events.remove( event );
		
			// System.out.println( "event cancelled (" + event.getWhen() + ") - queue = " + events.size());
	
			notify();
		}
	}
}
