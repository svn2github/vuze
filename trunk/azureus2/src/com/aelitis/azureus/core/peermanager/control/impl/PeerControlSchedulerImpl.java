package com.aelitis.azureus.core.peermanager.control.impl;

import java.util.*;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Average;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import com.aelitis.azureus.core.peermanager.control.PeerControlInstance;
import com.aelitis.azureus.core.peermanager.control.PeerControlScheduler;

public class 
PeerControlSchedulerImpl
	implements PeerControlScheduler
{
	private static final PeerControlSchedulerImpl	singleton = new PeerControlSchedulerImpl();
	
	public static PeerControlScheduler
	getSingleton()
	{
		return( singleton );
	}
	
	private Map	instance_map = new HashMap();
	
	private List	pending_registrations = new ArrayList();
	
	private volatile boolean	registrations_changed;
	
	protected AEMonitor	this_mon = new AEMonitor( "PeerControlScheduler" );
	
	protected
	PeerControlSchedulerImpl()
	{
		new AEThread( "PeerControlScheduler", true )
		{
			public void
			runSupport()
			{
				schedule();
			}
			
		}.start();
	}

	protected void
	schedule()
	{
		final int SLICE_PERIOD = 20;
		
		final int SLICES = SCHEDULE_PERIOD_MILLIS / SLICE_PERIOD;
		
		final int TICKS_PER_SEC = 1000 / SLICE_PERIOD;
		
		List[]	slices = new List[ SLICES ];
		
		for (int i=0;i<slices.length;i++){
			
			slices[i] = new LinkedList();
		}
		
		int	current_slice	= 0;
		
		long	second_start_time	= SystemTime.getCurrentTime();
		long	ticks				= 0;
		long	sleep_period		= 0;
		Average	second_time_average = Average.getInstance( 1000, 5 );
		
		long period_start_time	= second_start_time;
		long period_lag	= 0;
		
		final int	PERIOD_SECS			= 15;
		final int	TICKS_PER_PERIOD	= PERIOD_SECS*TICKS_PER_SEC;
		
		while( true ){
			
			if ( registrations_changed ){
				
				try{
					this_mon.enter();
					
					for (int i=0;i<pending_registrations.size();i++){
						
						int	min_index = current_slice;
						int min_size  = Integer.MAX_VALUE;
						
						for (int j=0;j<slices.length;j++){
							
							if ( slices[j].size() < min_size ){
								
								min_index	= j;
								min_size	= slices[j].size();
							}
						}
						
						slices[min_index].add( pending_registrations.get(i));
					}
					
					for (int i=0;i<slices.length;i++){
						
						Iterator	it = slices[i].iterator();
						
						while( it.hasNext()){
							
							if (((instanceWrapper)it.next()).isUnregistered()){
								
								it.remove();
							}
						}
					}
					
					registrations_changed	= false;
					
				}finally{
					
					this_mon.exit();
				}	
			}
			
			List	current_list = slices[current_slice++];
			
			if ( current_slice == SLICES ){
				
				current_slice = 0;
			}
			
			for (int i=0;i<current_list.size();i++){
				
				instanceWrapper	inst = (instanceWrapper)current_list.get(i);
				
				if ( !inst.isUnregistered()){
					
					try{
						inst.getInstance().schedule();
						
						Thread.sleep( 4);
					}catch( Throwable e ){
						
						Debug.printStackTrace(e);
					}
				}
			}
			
			ticks++;

			if ( ticks % TICKS_PER_SEC == 0 ){
				
				long second_end_time = SystemTime.getCurrentTime();
				
				if ( ticks % TICKS_PER_PERIOD == 0 ){
					
					period_lag = PERIOD_SECS * 1000 - ( second_end_time - period_start_time );
					
					period_start_time = second_end_time;
				}
				
				long second_elapsed = second_end_time - second_start_time;
				
				if ( second_elapsed < 0 ){
					
					second_elapsed = 1000;
				}
				
				second_time_average.addValue( second_elapsed );
				
				long average = second_time_average.getAverage();	
				
				if ( average < 1000 ){
					
					average = 1000;
				}
				
				sleep_period = ( 2000 - average ) / TICKS_PER_SEC;
				
				if ( sleep_period < 1 ){
					
					sleep_period = 1;
				}
				
				second_start_time = second_end_time;
			}
			
			if (ticks % TICKS_PER_SEC == 0 ){
				
				System.out.println( "sleep = " + sleep_period + ", " + PERIOD_SECS + " sec lag = " + period_lag );
			}
										
			try{
				Thread.sleep( sleep_period );
				
			}catch( Throwable e ){
				
			}
		}
	}
	
	public void
	register(
		PeerControlInstance	instance )
	{
		instanceWrapper wrapper = new instanceWrapper( instance );
		
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
	
	protected static class
	instanceWrapper
	{
		private PeerControlInstance		instance;
		private boolean					unregistered;
		
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
		
		protected PeerControlInstance
		getInstance()
		{
			return( instance );
		}
	}
}
