/*
 * Copyright (C) 2005, 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package com.aelitis.azureus.core.peermanager.control.impl;

import java.util.*;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.peermanager.control.PeerControlInstance;
import com.aelitis.azureus.core.peermanager.control.PeerControlScheduler;
import com.aelitis.azureus.core.peermanager.control.SpeedTokenDispenser;
import com.aelitis.azureus.core.stats.AzureusCoreStats;
import com.aelitis.azureus.core.stats.AzureusCoreStatsProvider;

public class 
PeerControlSchedulerImpl
	implements PeerControlScheduler, AzureusCoreStatsProvider
{
	private static final PeerControlSchedulerImpl	singleton = new PeerControlSchedulerImpl();
	
	public static PeerControlScheduler
	getSingleton()
	{
		return( singleton );
	}
	
	private Random	random = new Random();
	
	private Map	instance_map = new HashMap();
	
	private List	pending_registrations = new ArrayList();
	
	private volatile boolean	registrations_changed;
	private volatile long		latest_time;
	
	protected AEMonitor	this_mon = new AEMonitor( "PeerControlScheduler" );
	
	private long	wait_count;
	private long	yield_count;
	private long	total_wait_time;
	
	private final SpeedTokenDispenser tokenDispenser = new SpeedTokenDispenser();
	
	
	
	protected
	PeerControlSchedulerImpl()
	{
		Set	types = new HashSet();
		
		types.add( AzureusCoreStats.ST_PEER_CONTROL_LOOP_COUNT );
		types.add( AzureusCoreStats.ST_PEER_CONTROL_YIELD_COUNT );
		types.add( AzureusCoreStats.ST_PEER_CONTROL_WAIT_COUNT );
		types.add( AzureusCoreStats.ST_PEER_CONTROL_WAIT_TIME );

		AzureusCoreStats.registerProvider( types, this );
		
		new AEThread( "PeerControlScheduler", true )
		{
			public void
			runSupport()
			{
				schedule();
			}
			
		}.start();
	}

	public void
	updateStats(
		Set		types,
		Map		values )
	{
			//read
		
		if ( types.contains( AzureusCoreStats.ST_PEER_CONTROL_LOOP_COUNT )){
			
			values.put( AzureusCoreStats.ST_PEER_CONTROL_LOOP_COUNT, new Long( wait_count + yield_count ));
		}
		if ( types.contains( AzureusCoreStats.ST_PEER_CONTROL_YIELD_COUNT )){
			
			values.put( AzureusCoreStats.ST_PEER_CONTROL_YIELD_COUNT, new Long( yield_count ));
		}
		if ( types.contains( AzureusCoreStats.ST_PEER_CONTROL_WAIT_COUNT )){
			
			values.put( AzureusCoreStats.ST_PEER_CONTROL_WAIT_COUNT, new Long( wait_count ));
		}
		if ( types.contains( AzureusCoreStats.ST_PEER_CONTROL_WAIT_TIME )){
			
			values.put( AzureusCoreStats.ST_PEER_CONTROL_WAIT_TIME, new Long( total_wait_time ));
		}
	}
	
	public static SpeedTokenDispenser getDispenser()
	{
		return singleton.tokenDispenser;
	}
	
	protected void
	schedule()
	{
		latest_time	= SystemTime.getCurrentTime();
		SystemTime.registerConsumer(
			new SystemTime.consumer()
			{
				public void
				consume( long	time )
				{
					synchronized( PeerControlSchedulerImpl.this ){
						latest_time	= time;
						PeerControlSchedulerImpl.this.notify();
					}
				}
			});
						
		
		ArrayList	instances = new ArrayList();

		long	latest_time_used	= 0;
		int scheduledNext = 0;
		long	currentScheduleStart = latest_time;
		long 	last_stats_time	= latest_time;
		
		while( true ){
			if ( registrations_changed ){
				try{
					this_mon.enter();
					Iterator	it = instances.iterator();
					while( it.hasNext()){
						if (((instanceWrapper)it.next()).isUnregistered()){
							it.remove();
						}
					}

					for (int i=0;i<pending_registrations.size();i++)
						instances.add( pending_registrations.get(i));
					
					pending_registrations.clear();
					
					// order instances by their priority (lowest number first)
					Collections.sort(instances);
					
					if(instances.size() > 0)
					{
						for(int i=0;i<instances.size();i++)
							((instanceWrapper)instances.get(i)).setScheduleOffset((SCHEDULE_PERIOD_MILLIS * i) / instances.size());
					}

					scheduledNext = 0;			
					currentScheduleStart = latest_time;
					
					registrations_changed	= false;
				}finally{
					this_mon.exit();
				}	
			}
						
			tokenDispenser.update(latest_time);
			
			for (int i = scheduledNext; i < instances.size(); i++)
			{
				instanceWrapper inst = (instanceWrapper) instances.get(i);
				if (currentScheduleStart + inst.getScheduleOffset() > latest_time_used)
					break; // too early for next task, continue waiting
				if (i == 0)
					tokenDispenser.refill();
				// System.out.println("scheduling "+i+" time:"+latest_time);
				inst.schedule();
				scheduledNext++;
				if (scheduledNext >= instances.size())
				{
					scheduledNext = 0;
					// try to run every task every SCHEDULE_PERIOD_MILLIS on average
					currentScheduleStart += SCHEDULE_PERIOD_MILLIS;
					// if tasks hog too much time then delay to prevent massive
					// catch-up-hammering
					if (latest_time_used - currentScheduleStart > SCHEDULE_PERIOD_MAX_CATCHUP )
						currentScheduleStart = latest_time_used + SCHEDULE_PERIOD_MILLIS;
				}
			}
			
			
				
			/*
			for (Iterator it=instances.iterator();it.hasNext();){
				instanceWrapper	inst = (instanceWrapper)it.next();
				long	target = inst.getNextTick();
				long	diff = target - latest_time_used;			
				
				if ( diff <= 0 || diff > SCHEDULE_PERIOD_MILLIS ){
					inst.schedule();
					long new_target = target + SCHEDULE_PERIOD_MILLIS;
					diff = new_target - latest_time_used;
					if ( diff <= 0 || diff > SCHEDULE_PERIOD_MILLIS )
						new_target = latest_time_used + SCHEDULE_PERIOD_MILLIS;
					inst.setNextTick( new_target );
				}
			}*/
						
			synchronized( this ){
				if ( latest_time == latest_time_used ){
					wait_count++;
					try{
						long wait_start = SystemTime.getHighPrecisionCounter();
						wait();
						long wait_time 	= SystemTime.getHighPrecisionCounter() - wait_start;
						total_wait_time += wait_time;
					}catch( Throwable e ){
						Debug.printStackTrace(e);
					}
					
				}else{
					yield_count++;
					Thread.yield();
				}
				
				latest_time_used	= latest_time;
			}
			
			long	stats_diff =  latest_time_used - last_stats_time;
			
			if ( stats_diff > 10000 ){
				// System.out.println( "stats: time = " + stats_diff + ", ticks = " + tick_count + ", inst = " + instances.size());
				last_stats_time	= latest_time_used;
			}
		}
	}
	
	public void
	register(
		PeerControlInstance	instance )
	{
		instanceWrapper wrapper = new instanceWrapper( instance );
		
		wrapper.setScheduleOffset( latest_time + random.nextInt( SCHEDULE_PERIOD_MILLIS ));
		
		try{
			this_mon.enter();
			
			Map	new_map = new HashMap( instance_map );
			
			new_map.put( instance, wrapper );
			
			instance_map = new_map;
			
			pending_registrations.add( wrapper );
			
			registrations_changed = true;
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	unregister(
		PeerControlInstance	instance )
	{
		try{
			this_mon.enter();
			
			Map	new_map = new HashMap( instance_map );
			
			instanceWrapper wrapper = (instanceWrapper)new_map.remove(instance);
			
			if ( wrapper == null ){
				
				Debug.out( "instance wrapper not found" );
				
				return;
			}
				
			wrapper.unregister();
			
			instance_map = new_map;
			
			registrations_changed = true;
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void updateScheduleOrdering() {
		registrations_changed = true;
	}
	
	protected static class
	instanceWrapper implements Comparable
	{
		private PeerControlInstance		instance;
		private boolean					unregistered;
		
		private long					offset;
		
		protected
		instanceWrapper(
			PeerControlInstance	_instance )
		{
			instance = _instance;
		}
		
		protected void
		unregister()
		{
			unregistered	= true;
		}
		
		protected boolean
		isUnregistered()
		{
			return( unregistered );
		}
		
		protected void
		setScheduleOffset(
			long	t )
		{
			offset	= t;
		}
		
		protected long
		getScheduleOffset()
		{
			return( offset );
		}
		
		protected PeerControlInstance
		getInstance()
		{
			return( instance );
		}
		
		protected void
		schedule()
		{
			try{
				instance.schedule();
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
		
		public int compareTo(Object o) {
			return instance.getSchedulePriority()-((instanceWrapper)o).instance.getSchedulePriority();
		}
	}
}
