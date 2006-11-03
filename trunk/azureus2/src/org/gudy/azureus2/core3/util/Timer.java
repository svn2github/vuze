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
	extends 	AERunnable
	implements	SystemTime.consumer
{	
	private ThreadPool	thread_pool;
		
	private Set	events = new TreeSet();
		
	private long	unique_id_next	= 0;
	
	private volatile boolean	destroyed;
	private boolean				indestructable;
	
	private boolean		log;
	private int			max_events_logged;
	
	public
	Timer(
		String	name )
	{
		this( name, 1 );
	}

	public
	Timer(
		String	name,
		int		thread_pool_size )
	{
		this(name, thread_pool_size, Thread.NORM_PRIORITY);
	}

	public
	Timer(
		String	name,
		int		thread_pool_size,
		int		thread_priority )
	{
		thread_pool = new ThreadPool(name,thread_pool_size);
	
		SystemTime.registerClockChangeListener( this );

		Thread t = new Thread(this, "Timer:" + name );
		
		t.setDaemon( true );
		
		t.setPriority(thread_priority);
			
		t.start();
	}
	
	public void
	setIndestructable()
	{
		indestructable	= true;
	}
	
	public void
	setLogging(
		boolean	_log )
	{
		log	= _log;
	}
	
	public boolean getLogging() {
		return log;
	}
	
	public void
	setWarnWhenFull()
	{
		thread_pool.setWarnWhenFull();
	}
	
	public void
	runSupport()
	{
		while( true ){
			
			try{
				TimerEvent	event_to_run = null;
				
				synchronized(this){
					
					if ( destroyed ){
						
						break;
					}
					
					if ( events.isEmpty()){
						
						// System.out.println( "waiting forever" );
						
						this.wait();
						
					}else{
						long	now = SystemTime.getCurrentTime();
						
						TimerEvent	next_event = (TimerEvent)events.iterator().next();
						
						long	delay = next_event.getWhen() - now;
						
						if ( delay > 0 ){
							
							// System.out.println( "waiting for " + delay );
							
							this.wait(delay);
						}
					}
				
					if ( destroyed ){
						
						break;
					}
					
					long	now = SystemTime.getCurrentTime();
					
					Iterator	it = events.iterator();
					
					while( it.hasNext()){
						
						TimerEvent	event = (TimerEvent)it.next();
						
						if ( event.getWhen() <= now ){
							
							event_to_run = event;
							
							it.remove();
							
							break;
						}
					}
				}
				
				if ( event_to_run != null ){
					
					event_to_run.setHasRun();
					
					if (log) {
						System.out.println( "running: " + event_to_run.getString() );
					}
					
					thread_pool.run(event_to_run.getRunnable());
				}
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}
	}
	
	public void
	consume(
		long	offset )
	{
		// System.out.println( "Timer '" + thread_pool.getName() +"': clock change by " + offset );
		  
		if ( Math.abs( offset ) >= 60*1000 ){
			
				// fix up the timers
			
			synchronized( this ){
				
					// as we're adjusting all events by the same amount the ordering remains valid
				
				Iterator	it = events.iterator();
				
				while (it.hasNext()){
					
					TimerEvent	event = (TimerEvent)it.next();
					
					long	old_when = event.getWhen();
					long	new_when = old_when + offset;
					
					// System.out.println( "    adjusted: " + old_when + " -> " + new_when );
					
					event.setWhen( new_when );
				}

				notify();
			}
		}
	}
	
	public void
	adjustAllBy(
		long	offset )
	{
		// fix up the timers

		synchronized (this) {

			// as we're adjusting all events by the same amount the ordering remains valid

			Iterator it = events.iterator();

			while (it.hasNext()) {

				TimerEvent event = (TimerEvent) it.next();

				long old_when = event.getWhen();
				long new_when = old_when + offset;

				// System.out.println( "    adjusted: " + old_when + " -> " + new_when );

				event.setWhen(new_when);
			}

			notify();
		}
	}

	public synchronized TimerEvent
	addEvent(
		long				when,
		TimerEventPerformer	performer )
	{
		return( addEvent( SystemTime.getCurrentTime(), when, performer ));
	}
	
	public synchronized TimerEvent
	addEvent(
		String				name,
		long				when,
		TimerEventPerformer	performer )
	{
		return( addEvent( name, SystemTime.getCurrentTime(), when, performer ));
	}
	
	public synchronized TimerEvent
	addEvent(
		long				creation_time,
		long				when,
		TimerEventPerformer	performer )
	{
		return( addEvent( null, creation_time, when, performer ));
	}
	
	public synchronized TimerEvent
	addEvent(
		String				name,
		long				creation_time,
		long				when,
		TimerEventPerformer	performer )
	{
		TimerEvent	event = new TimerEvent( this, unique_id_next++, creation_time, when, performer );
		
		if ( name != null ){
			
			event.setName( name );
		}
		
		events.add( event );
		
		if ( log ){
			
			if ( !(performer instanceof TimerEventPerformer )){
				
				System.out.println( "Timer '" + thread_pool.getName() + "' - added " + event.getString());
			}
					
			if ( events.size() > max_events_logged ){
		
				max_events_logged = events.size();
				
				System.out.println( "Timer '" + thread_pool.getName() + "' - events = " + max_events_logged );
			}
		}
		
		// System.out.println( "event added (" + when + ") - queue = " + events.size());
				
		notify();
		
		return( event );
	}
	
	public synchronized TimerEventPeriodic
	addPeriodicEvent(
		long				frequency,
		TimerEventPerformer	performer )
	{
		return( addPeriodicEvent( null, frequency, performer ));
	}
	
	public synchronized TimerEventPeriodic
	addPeriodicEvent(
		String				name,
		long				frequency,
		TimerEventPerformer	performer )
	{
		TimerEventPeriodic periodic_performer = new TimerEventPeriodic( this, frequency, performer );
		
		if ( name != null ){
			
			periodic_performer.setName( name );
		}
		
		if ( log ){
						
			System.out.println( "Timer '" + thread_pool.getName() + "' - added " + periodic_performer.getString());
		}
		
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
	
	public synchronized void
	destroy()
	{
		if ( indestructable ){
			
			Debug.out( "Attempt to destroy indestructable timer '" + getName() + "'" );
			
		}else{
			
			destroyed	= true;
			
			notify();
			
			SystemTime.unregisterClockChangeListener( this );
		}
	}
	
	public String
	getName()
	{
		return( thread_pool.getName());
	}
	
	public synchronized void
	dump()
	{
		System.out.println( "Timer '" + thread_pool.getName() + "': dump" );

		Iterator	it = events.iterator();
		
		while(it.hasNext()){
			
			TimerEvent	ev = (TimerEvent)it.next();
			
			System.out.println( "\t" + ev.getString());
		}
	}
}
