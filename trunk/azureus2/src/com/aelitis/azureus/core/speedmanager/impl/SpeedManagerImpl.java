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
	
	private static final int	PING_CHOKE_TIME				= 1000;
	private static final int	PING_AVERAGE_HISTORY_COUNT	= 5;

	private static final int	IDLE_UPLOAD_SPEED		= 5*1024;
	private static final int	INITIAL_IDLE_AVERAGE	= 100;

	private static final int	INCREASING	= 1;
	private static final int	DECREASING	= 2;
	
	
	private AzureusCore			core;
	private DHTSpeedTester		speed_tester;
	private SpeedManagerAdapter	adapter;
	
	
	private int					min_up;
	private int					max_up;
	private boolean				enabled;
		
	private Average upload_average			= AverageFactory.MovingImmediateAverage( 5 );
	private Average upload_short_average	= AverageFactory.MovingImmediateAverage( 2 );
	
	private Average	ping_average_history		= AverageFactory.MovingImmediateAverage(PING_AVERAGE_HISTORY_COUNT);
	private Average	ping_short_average_history	= AverageFactory.MovingImmediateAverage(2);
	
	private Average choke_speed_average			= AverageFactory.MovingImmediateAverage( 3 );

	private int					mode;
	private volatile int		mode_ticks;
	private int					saved_limit;
	
	private int		direction;
	private int		ticks;
	private int		idle_ticks;
	private int		idle_average;
	private boolean	idle_average_set;
	
	private int		max_upload_average;
	
	private Map		contacts	= new HashMap();
	private volatile int	new_contacts;
	
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
		direction			= INCREASING;
		new_contacts		= 0;
		
		choke_speed_average.reset();
		upload_average.reset();
		upload_short_average.reset();
		ping_average_history.reset();
		ping_short_average_history.reset();
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
							
							synchronized( contacts ){
								
								contacts.put( contact, new pingContact( contact ));
							}
							
							new_contacts++;
							
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
										
										synchronized( contacts ){
											
											contacts.remove( contact );
										}
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
					
					upload_short_average.update( current_speed );
					
					mode_ticks++;
					
					ticks++;
				}
			});
	}
	
	protected void
	calculate(
		DHTSpeedTesterContact[]	rtt_contacts,
		int[]					round_trip_times )
	{	
		if ( !enabled ){
			
			for (int i=0;i<rtt_contacts.length;i++){
				
				rtt_contacts[i].destroy();
			}
			
			return;
		}
		
		int	min_rtt	= UNLIMITED;
		
		for (int i=0;i<rtt_contacts.length;i++){
			
			int	rtt =  round_trip_times[i];

			if ( rtt < min_rtt ){
				
				min_rtt	= rtt;
			}
		}
		
		String	str = "";
		
		int	ping_total		= 0;
		int	ping_count		= 0;
		
		for (int i=0;i<rtt_contacts.length;i++){
			
			pingContact	pc;
			
			synchronized( contacts ){
			
				pc = (pingContact)contacts.get( rtt_contacts[i] );
			}
			
			int	rtt =  round_trip_times[i];
			
			str += (i==0?"":",") + rtt;

				// discount anything 5*min reported
			
			if ( pc != null ){
			
				boolean	good_ping =  rtt < 5*min_rtt;
				
				pc.pingReceived( good_ping );
			}

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
		
			// if we're uploading slowly or the current ping rate is better than our current idle average
			// then we count this towards establishing the baseline
		
		if ( up_average <= IDLE_UPLOAD_SPEED || ( running_average < idle_average && !idle_average_set )){
			
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
		
		if ( idle_average_set && running_average < idle_average ){
			
				// bump down if we happen to come across lower idle values
			
			idle_average = running_average;
		}
		
		int	current_speed 	= adapter.getCurrentUploadSpeed();
		int	current_limit	= adapter.getCurrentUploadLimit();

		int	new_limit	= current_limit;

		System.out.println( 
				"pings= " + str + ", average=" + ping_average +", running_average=" + running_average +
				",idle_average=" + idle_average + ", speed=" + current_speed + ",limit=" + current_limit +
				",choke = " + (int)choke_speed_average.getAverage());



		if ( mode == MODE_FORCED_MAX ){
			
			if ( mode_ticks > FORCED_MAX_TICKS ){
				
				mode		= MODE_RUNNING;
				
				current_limit = new_limit	= saved_limit;
			}
			
		}else if ( mode == MODE_FORCED_MIN ){
			
			if ( idle_average_set || mode_ticks > FORCED_MIN_TICKS ){
				
				System.out.println( "Mode -> running" );

				if ( !idle_average_set ){
					
					idle_average		= running_average;
				
					idle_average_set	= true;
				}
				
				mode		= MODE_RUNNING;
				mode_ticks	= 0;
				
				current_limit = new_limit	= saved_limit;
				
			}else if ( mode_ticks == 5 ){
				
					// we've had 5 secs of min up speed, clear out the ping average now
					// to get accurate times
				
				ping_average_history.reset();
			}
		}
		
		if ( mode == MODE_RUNNING ){
			
			if (	( ticks > FORCED_MIN_TICK_LIMIT && !idle_average_set ) ||
					( new_contacts >= 2 && idle_average_set )){
				
					// we've been running a while but no min set, or we've got some new untested 
					// contacts - force it
				
				System.out.println( "Mode -> forced min" );
				
				mode		= MODE_FORCED_MIN;
				mode_ticks	= 0;
				saved_limit	= current_limit;
				
				idle_average_set	= false;
				new_contacts		= 0;
				
				new_limit	= FORCED_MIN_SPEED;
				
			}else{
			
				int SOMETHING = 50;
				
				int	short_up = (int)upload_short_average.getAverage();

				int	choke_speed = (int)choke_speed_average.getAverage();
			
				
				if ( running_average < 2* idle_average ){
					
					direction = INCREASING;
					
					int	diff = running_average - idle_average;
					
					if ( diff < 100 ){
						
						diff = 100;
					}
					
					int	increment = 1024 * ( diff / SOMETHING );
										
						// if we're close to the last choke-speed then decrease increments

					int	max_inc	= 5*1024;
					
					if ( new_limit + 2*1024 > choke_speed ){
						
						max_inc = 1024;
						
					}else if ( new_limit + 5*1024 > choke_speed ){
						
						max_inc += 3*1024;
					}
							
					new_limit += Math.min( increment, max_inc );					
					
				}else if ( ping_average > 4*idle_average || ping_average > PING_CHOKE_TIME ){
					
					if ( direction == INCREASING ){
						
						if ( idle_average_set ){
							
							choke_speed_average.update( short_up );
						}
					}
					
					direction = DECREASING;
					
					int decrement = 1024 * (( ping_average - (3*idle_average )) / SOMETHING );
					
					new_limit -= Math.min( decrement, 5*1024 );
				}			
				
				if ( new_limit < 1024 ){
					
					new_limit	= 1024;
				}
			}
		
				// final tidy up
			
			if ( min_up > 0 && new_limit < min_up && mode != MODE_FORCED_MIN  ){
				
				new_limit = min_up;
				
			}else if ( max_up > 0 &&  new_limit > max_up && mode != MODE_FORCED_MAX ){
				
				new_limit = max_up;
			}
			
				// if we're not achieving the current limit and the advice is to increase it, don't
				// bother
			
			if ( new_limit > current_limit && current_speed < ( current_limit - 10*1024 )){
			
				new_limit = current_limit;
			}
		}
		
		if ( enabled ){
			
				// round limit up to nearest K
			
			new_limit = (( new_limit + 1023 )/1024) * 1024;
			
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
	
	protected class
	pingContact
	{
		private DHTSpeedTesterContact	contact;
		
		private int	bad_pings;
		
		protected
		pingContact(
			DHTSpeedTesterContact	_contact )
		{
			contact	= _contact;
		}
		
		void
		pingReceived(
			boolean	good )
		{
			if ( good ){
				
				bad_pings = 0;
				
			}else{
				
				bad_pings++;
			}
			
				// three strikes and you're out!
			
			if ( bad_pings == 3 ){
				
				contact.destroy();
			}
		}
	}
}
