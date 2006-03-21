/*
 * Created on 16-Mar-2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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
 *
 */

package com.aelitis.azureus.core.speedmanager.impl;

import java.util.*;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.dht.speed.DHTSpeedTester;
import com.aelitis.azureus.core.dht.speed.DHTSpeedTesterContact;
import com.aelitis.azureus.core.dht.speed.DHTSpeedTesterContactListener;
import com.aelitis.azureus.core.dht.speed.DHTSpeedTesterListener;
import com.aelitis.azureus.core.speedmanager.SpeedManager;
import com.aelitis.azureus.core.speedmanager.SpeedManagerAdapter;
import com.aelitis.azureus.core.util.average.*;


public class 
SpeedManagerImpl 
	implements SpeedManager
{
	private static final int UNLIMITED	= Integer.MAX_VALUE;
	
	private static final int CONTACT_NUMBER		= 3;
	private static final int CONTACT_PING_SECS	= 5;
	
	private static final int	MODE_RUNNING	= 0;
	private static final int	MODE_FORCED_MIN	= 1;
	private static final int	MODE_FORCED_MAX	= 2;
	
	private static final int	TICK_PERIOD			= 1000;	// 1 sec
	
	private static final int	FORCED_MAX_TICKS	= 30;
	
	private static final int	FORCED_MIN_TICKS		= 30;
	private static final int	FORCED_MIN_TICK_LIMIT	= 60;
	private static final int	FORCED_MIN_SPEED		= 4*1024;
	
	private static final int	PING_AVERAGE_HISTORY_COUNT	= 6;

	private static final int	IDLE_UPLOAD_SPEED		= 5*1024;
	private static final int	INITIAL_IDLE_AVERAGE	= 100;

	
	
	private AzureusCore			core;
	private DHTSpeedTester		speed_tester;
	private SpeedManagerAdapter	adapter;
	
	
	private int					min_up;
	private int					max_up;
	private boolean				enabled;
		
	private Average upload_average = AverageFactory.MovingImmediateAverage( 5 );
	private Average	ping_average_history	= AverageFactory.MovingImmediateAverage(PING_AVERAGE_HISTORY_COUNT);
	
	private int					mode;
	private volatile int		mode_ticks;
	private int					saved_limit;
	
	private int		ticks;
	private int		idle_ticks;
	private int		idle_average;
	private boolean	idle_average_set;
	
	private int		max_upload_average;
	
	protected void
	reset()
	{
		ticks				= 0;
		mode				= MODE_RUNNING;
		mode_ticks			= 0;
		idle_ticks			= 0;
		idle_average		= INITIAL_IDLE_AVERAGE;
		idle_average_set	= false;
		max_upload_average	= 0;
		upload_average.reset();
		ping_average_history.reset();
	}
	
	
	public
	SpeedManagerImpl(
		AzureusCore			_core,
		SpeedManagerAdapter	_adapter )
	{
		core			= _core;
		adapter			= _adapter;
	}
	
	public void
	setSpeedTester(
		DHTSpeedTester	_tester )
	{
		if ( speed_tester != null ){
			
			Debug.out( "speed tester already set!" );
			
			return;
		}
		
		speed_tester	= _tester; 
				
		speed_tester.addListener(
				new DHTSpeedTesterListener()
				{
					public void 
					contactAdded(
						DHTSpeedTesterContact contact)
					{
						if ( core.getInstanceManager().isLANAddress(contact.getContact().getAddress().getAddress())){
							
							contact.destroy();
							
						}else{
							System.out.println( "activePing: " + contact.getContact().getString());
							
							contact.setPingPeriod( CONTACT_PING_SECS );
							
							contact.addListener(
								new DHTSpeedTesterContactListener()
								{
									public void
									ping(
										DHTSpeedTesterContact	contact,
										int						round_trip_time )
									{
									}
									
									public void
									pingFailed(
										DHTSpeedTesterContact	contact )
									{
									}
									
									public void
									contactDied(
										DHTSpeedTesterContact	contact )
									{
										System.out.println( "deadPing: " + contact.getContact().getString());
									}
								});
						}
					}
					
					public void
					resultGroup(
						DHTSpeedTesterContact[]	contacts,
						int[]					round_trip_times )
					{
						calculate( contacts, round_trip_times );
					}
				});
		
		SimpleTimer.addPeriodicEvent(
			TICK_PERIOD,
			new TimerEventPerformer()
			{
				public void
				perform(
					TimerEvent	event )
				{
					int	current_speed = adapter.getCurrentUploadSpeed();

					upload_average.update( current_speed );
					
					mode_ticks++;
					
					ticks++;
				}
			});
	}
	
	protected void
	calculate(
		DHTSpeedTesterContact[]	contacts,
		int[]					round_trip_times )
	{	
		if ( !enabled ){
			
			for (int i=0;i<contacts.length;i++){
				
				contacts[i].destroy();
			}
			
			return;
		}
		
		String	str = "";
		
		int	ping_total		= 0;
		int	ping_count		= 0;
		
		for (int i=0;i<contacts.length;i++){
			
			int	rtt =  round_trip_times[i];
			
			str += (i==0?"":",") + rtt;

			if ( rtt != -1 ){
			
				ping_total += round_trip_times[i];
				
				ping_count++;
			}
		}
		
		if ( ping_count == 0 ){
		
				// all failed
			
			return;
		}
		
		int	ping_average = ping_total/ping_count;
				
		int	running_average = (int)ping_average_history.update( ping_average );
		
		int	up_average = (int)upload_average.getAverage();
		
		if ( up_average <= IDLE_UPLOAD_SPEED ){
			
			idle_ticks++;
			
			if ( idle_ticks >= PING_AVERAGE_HISTORY_COUNT ){
				
				System.out.println( "New idle average: " + running_average );
				
				idle_average	= running_average;
				
				idle_average_set	= true;
			}
		}else{
			
			if ( up_average > max_upload_average ){
				
				max_upload_average	= up_average;
				
				System.out.println( "New max upload:" +  max_upload_average );
			}
			
			idle_ticks	= 0;
			
		}
		
		if ( ticks > PING_AVERAGE_HISTORY_COUNT && running_average < idle_average ){
			
			idle_average = running_average;
		}
		
		int	current_speed 	= adapter.getCurrentUploadSpeed();
		int	current_limit	= adapter.getCurrentUploadLimit();

		int	new_limit	= current_limit;

		System.out.println( 
				"pings= " + str + ", average=" + ping_average +", running_average=" + running_average +
				",idle_average=" + idle_average + ", speed=" + current_speed + ",limit=" + current_limit );



		if ( mode == MODE_FORCED_MAX ){
			
			if ( mode_ticks > FORCED_MAX_TICKS ){
				
				mode		= MODE_RUNNING;
				
				current_limit = new_limit	= saved_limit;
			}
			
		}else if ( mode == MODE_FORCED_MIN ){
			
			if ( idle_average_set || mode_ticks > FORCED_MIN_TICKS ){
				
				System.out.println( "forced min ping calc complete" );

				if ( !idle_average_set ){
					
					idle_average	= running_average;
				
					idle_average_set	= true;
				}
				
				mode	= MODE_RUNNING;
				
				current_limit = new_limit	= saved_limit;
			}
		}
		
		if ( mode == MODE_RUNNING ){
			
			if ( ticks > FORCED_MIN_TICK_LIMIT && !idle_average_set ){
				
					// we've been running a while but no min set, force it
				
				System.out.println( "forcing min ping calc" );
				
				mode		= MODE_FORCED_MIN;
				mode_ticks	= 0;
				saved_limit	= current_limit;
				
				new_limit	= FORCED_MIN_SPEED;
				
			}else{
			
				if ( current_limit == UNLIMITED ){
					
					if ( running_average > 500 ){
					
						new_limit	= ( 85*idle_average )/100;
					}
				}else{
					
					int SOMETHING = 50;
					
					if ( running_average < 2* idle_average ){
						
						int	diff = running_average - idle_average;
						
						if ( diff < 100 ){
							
							diff = 100;
						}
						
						int	increment = 1024 * ( diff / SOMETHING );
						
						new_limit += Math.min( increment, 5*1024 );
						
					}else if ( ping_average > 4*idle_average ){
						
						int decrement = 1024 * (( ping_average - (3*idle_average )) / SOMETHING );
						
						new_limit -= Math.min( decrement, 5*1024 );
					}			
					
					if ( new_limit < 1024 ){
						
						new_limit	= 1024;
					}
				}
			}
		
				// final tidy up
			
			if ( min_up > 0 && new_limit < min_up ){
				
				new_limit = min_up;
				
			}else if ( max_up > 0 &&  new_limit > max_up ){
				
				new_limit = max_up;
			}
			
				// if we're not achieving the current limit and the advice is to increase it, don't
				// bother
			
			if ( new_limit > current_limit && current_speed < ( current_limit - 10*1024 )){
			
				new_limit = current_limit;
			}
		}
		
		if ( enabled ){
			
			adapter.setCurrentUploadLimit( new_limit );
		}
	}
	
	public boolean
	isAvailable()
	{
		return( speed_tester != null );
	}
	
	public void
	setMinumumUploadSpeed(
		int		speed )
	{
		min_up	= speed;
	}
	
	public int
	getMinumumUploadSpeed()
	{
		return( min_up );
	}
	
	public void
	setMaximumUploadSpeed(
		int		speed )
	{
		max_up	= speed;
	}
	
	public int
	getMaximumUploadSpeed()
	{
		return( max_up );
	}
	
	public void
	setEnabled(
		boolean		_enabled )
	{
		if ( enabled != _enabled ){
			
			reset();
			
			enabled	= _enabled;
			
			if ( speed_tester != null ){
				
				speed_tester.setContactNumber( enabled?CONTACT_NUMBER:0);
			}
		}
	}
	
	public boolean
	isEnabled()
	{
		return( enabled );
	}
	
	public DHTSpeedTester
	getSpeedTester()
	{
		return( speed_tester );
	}
}
