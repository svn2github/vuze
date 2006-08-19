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

import java.net.InetSocketAddress;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
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
import com.aelitis.azureus.core.speedmanager.SpeedManagerPingSource;
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
	
		// config items start
	
	private static final String	CONFIG_AVAIL			= "AutoSpeed Available";	// informative only
	private static final String	CONFIG_MIN_UP			= "AutoSpeed Min Upload KBs";
	private static final String	CONFIG_MAX_UP			= "AutoSpeed Max Upload KBs";
	private static final String	CONFIG_MAX_INC			= "AutoSpeed Max Increment KBs";
	private static final String	CONFIG_MAX_DEC			= "AutoSpeed Max Decrement KBs";
	private static final String	CONFIG_CHOKE_PING		= "AutoSpeed Choking Ping Millis";
	private static final String	CONFIG_DOWNADJ_ENABLE	= "AutoSpeed Download Adj Enable";
	private static final String	CONFIG_DOWNADJ_RATIO	= "AutoSpeed Download Adj Ratio";
	private static final String	CONFIG_LATENCY_FACTOR	= "AutoSpeed Latency Factor";
	private static final String	CONFIG_DEBUG			= "Auto Upload Speed Debug Enabled";
	
	private static final String[]	CONFIG_PARAMS = {
		CONFIG_MIN_UP, CONFIG_MAX_UP, 
		CONFIG_MAX_INC, CONFIG_MAX_DEC,
		CONFIG_CHOKE_PING, 
		CONFIG_DOWNADJ_ENABLE,
		CONFIG_DOWNADJ_RATIO,
		CONFIG_LATENCY_FACTOR,
		CONFIG_DEBUG };
		
	private static int					PING_CHOKE_TIME;
	private static int					MIN_UP;
	private static int					MAX_UP;
	private static boolean				DEBUG;
	private static boolean				ADJUST_DOWNLOAD_ENABLE;
	private static float				ADJUST_DOWNLOAD_RATIO;
	private static int					MAX_INCREMENT;
	private static int					MAX_DECREMENT;
	private static int					LATENCY_FACTOR;
	
	static{
		COConfigurationManager.addAndFireParameterListeners(
				CONFIG_PARAMS,
				new ParameterListener()
				{
					public void 
					parameterChanged(
						String parameterName )
					{
						PING_CHOKE_TIME	= COConfigurationManager.getIntParameter( CONFIG_CHOKE_PING );
						MIN_UP			= COConfigurationManager.getIntParameter( CONFIG_MIN_UP ) * 1024;
						MAX_UP			= COConfigurationManager.getIntParameter( CONFIG_MAX_UP ) * 1024;
						MAX_INCREMENT	= COConfigurationManager.getIntParameter( CONFIG_MAX_INC ) * 1024;
						MAX_DECREMENT	= COConfigurationManager.getIntParameter( CONFIG_MAX_DEC ) * 1024;
						ADJUST_DOWNLOAD_ENABLE	= COConfigurationManager.getBooleanParameter( CONFIG_DOWNADJ_ENABLE );
						String	str 	= COConfigurationManager.getStringParameter( CONFIG_DOWNADJ_RATIO );
						LATENCY_FACTOR	= COConfigurationManager.getIntParameter( CONFIG_LATENCY_FACTOR );

						if ( LATENCY_FACTOR < 1 ){
							LATENCY_FACTOR = 1;
						}
						
						DEBUG			= COConfigurationManager.getBooleanParameter( CONFIG_DEBUG );
						
						try{
							ADJUST_DOWNLOAD_RATIO = Float.parseFloat(str);
						}catch( Throwable e ){
						}
					}
				});
		
	}
		// config end
	
	private static final int	FORCED_MAX_TICKS	= 30;
	
	private static final int	FORCED_MIN_TICKS		= 60;			// time we'll force low upload to get baseline
	private static final int	FORCED_MIN_AT_START_TICK_LIMIT	= 60;	// how long we'll wait on start up before forcing min
	private static final int	FORCED_MIN_SPEED		= 4*1024;		// speed forced during min period
	
	private static final int	PING_AVERAGE_HISTORY_COUNT	= 5;

	private static final int	IDLE_UPLOAD_SPEED		= 5*1024;		// speed at which upload is treated as "idle"
	private static final int	INITIAL_IDLE_AVERAGE	= 100;
	private static final int	MIN_IDLE_AVERAGE		= 50;		// any lower than this and small ping variations cause overreaction

	private static final int	INCREASING	= 1;
	private static final int	DECREASING	= 2;
	
	
	private AzureusCore			core;
	private DHTSpeedTester		speed_tester;
	private SpeedManagerAdapter	adapter;
	
	
	private boolean				enabled;
		
	private Average upload_average				= AverageFactory.MovingImmediateAverage( 5 );
	private Average upload_short_average		= AverageFactory.MovingImmediateAverage( 2 );
	private Average upload_short_prot_average	= AverageFactory.MovingImmediateAverage( 2 );
	
	private Average	ping_average_history		= AverageFactory.MovingImmediateAverage(PING_AVERAGE_HISTORY_COUNT);
	
	private Average choke_speed_average			= AverageFactory.MovingImmediateAverage( 3 );

	private int					mode;
	private volatile int		mode_ticks;
	private int					saved_limit;
	
	private int		direction;
	private int		ticks;
	private int		idle_ticks;
	private int		idle_average;
	private boolean	idle_average_set;
	
	private int		max_ping;
	
	private int		max_upload_average;
	
	private Map							contacts	= new HashMap();
	private volatile int				total_contacts;
	private volatile int				replacement_contacts;
	private SpeedManagerPingSource[]	contacts_array	= new SpeedManagerPingSource[0];
	
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
		total_contacts		= 0;
		replacement_contacts= 0;
		max_ping			= 0;
		
		choke_speed_average.reset();
		upload_average.reset();
		upload_short_average.reset();
		upload_short_prot_average.reset();
		ping_average_history.reset();
	}
	
	
	public
	SpeedManagerImpl(
		AzureusCore			_core,
		SpeedManagerAdapter	_adapter )
	{
		core			= _core;
		adapter			= _adapter;
		
		COConfigurationManager.setParameter( CONFIG_AVAIL, false );
		
		reset();
	}
	
	public void
	setSpeedTester(
		DHTSpeedTester	_tester )
	{
		if ( speed_tester != null ){
			
			Debug.out( "speed tester already set!" );
			
			return;
		}
		
		COConfigurationManager.setParameter( CONFIG_AVAIL, true );

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
							log( "activePing: " + contact.getContact().getString());
							
							contact.setPingPeriod( CONTACT_PING_SECS );
							
							synchronized( contacts ){
								
								contacts.put( contact, new pingContact( contact ));
								
								contacts_array = new SpeedManagerPingSource[ contacts.size() ];
								
								contacts.values().toArray( contacts_array );
							}
							
							total_contacts++;
							
							if ( total_contacts > CONTACT_NUMBER ){
								
								replacement_contacts++;
							}
							
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
										log( "deadPing: " + contact.getContact().getString());
										
										synchronized( contacts ){
											
											contacts.remove( contact );
											
											contacts_array = new SpeedManagerPingSource[ contacts.size() ];
											
											contacts.values().toArray( contacts_array );
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
			"SpeedManager:stats",
			TICK_PERIOD,
			new TimerEventPerformer()
			{
				public void
				perform(
					TimerEvent	event )
				{
					int	current_protocol_speed 	= adapter.getCurrentProtocolUploadSpeed();
					int	current_data_speed		= adapter.getCurrentDataUploadSpeed();

					int	current_speed = current_protocol_speed + current_data_speed;
					
					upload_average.update( current_speed );
					
					upload_short_average.update( current_speed );
					
					upload_short_prot_average.update( current_protocol_speed );
					
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

			if ( rtt > 0 && rtt < min_rtt ){
				
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

				// discount anything 5*min reported unless min is really small, in which case round
				// up as we're only trying to catch badly behaved ones
			
			if ( pc != null ){
			
				boolean	good_ping =  rtt < 5 * Math.max( min_rtt, 75 );
				
				pc.pingReceived( rtt, good_ping );
				
				if ( !good_ping ){
					
					rtt = -1;
				}
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
		
		if ( ping_average > max_ping ){
			
			max_ping	= ping_average;
		}
		
		int	up_average = (int)upload_average.getAverage();
		
			// if we're uploading slowly or the current ping rate is better than our current idle average
			// then we count this towards establishing the baseline
		
		if ( up_average <= IDLE_UPLOAD_SPEED || ( running_average < idle_average && !idle_average_set )){
			
			idle_ticks++;
			
			if ( idle_ticks >= PING_AVERAGE_HISTORY_COUNT ){
				
				idle_average	= Math.max( running_average, MIN_IDLE_AVERAGE );

				log( "New idle average: " + idle_average );
				
				idle_average_set	= true;
			}
		}else{
			
			if ( up_average > max_upload_average ){
				
				max_upload_average	= up_average;
				
				log( "New max upload:" +  max_upload_average );
			}
			
			idle_ticks	= 0;
			
		}
		
		if ( idle_average_set && running_average < idle_average ){
			
				// bump down if we happen to come across lower idle values
			
			idle_average	= Math.max( running_average, MIN_IDLE_AVERAGE );
		}
		
		int	current_speed 	= adapter.getCurrentDataUploadSpeed() + adapter.getCurrentProtocolUploadSpeed();
		int	current_limit	= adapter.getCurrentUploadLimit();

		int	new_limit	= current_limit;

		log( 
				"Pings: " + str + ", average=" + ping_average +", running_average=" + running_average +
				",idle_average=" + idle_average + ", speed=" + current_speed + ",limit=" + current_limit +
				",choke = " + (int)choke_speed_average.getAverage());



		if ( mode == MODE_FORCED_MAX ){
			
			if ( mode_ticks > FORCED_MAX_TICKS ){
				
				mode		= MODE_RUNNING;
				
				current_limit = new_limit	= saved_limit;
			}
			
		}else if ( mode == MODE_FORCED_MIN ){
			
			if ( idle_average_set || mode_ticks > FORCED_MIN_TICKS ){
				
				log( "Mode -> running" );

				if ( !idle_average_set ){
					
					idle_average	= Math.max( running_average, MIN_IDLE_AVERAGE );
				
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
			
			if (	( ticks > FORCED_MIN_AT_START_TICK_LIMIT && !idle_average_set ) ||
					( replacement_contacts >= 2 && idle_average_set )){
				
					// we've been running a while but no min set, or we've got some new untested 
					// contacts - force it
				
				log( "Mode -> forced min" );
				
				mode		= MODE_FORCED_MIN;
				mode_ticks	= 0;
				saved_limit	= current_limit;
				
				idle_average_set	= false;
				idle_ticks			= 0;
				replacement_contacts= 0;
				
				new_limit	= FORCED_MIN_SPEED;
				
			}else{
							
				int	short_up = (int)upload_short_average.getAverage();

				int	choke_speed = (int)choke_speed_average.getAverage();
			
				
				if ( running_average < 2* idle_average && ping_average < PING_CHOKE_TIME ){
					
					direction = INCREASING;
					
					int	diff = running_average - idle_average;
					
					if ( diff < 100 ){
						
						diff = 100;
					}
					
					int	increment = 1024 * ( diff / LATENCY_FACTOR );
										
						// if we're close to the last choke-speed then decrease increments

					int	max_inc	= MAX_INCREMENT;
					
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
					
					int decrement = 1024 * (( ping_average - (3*idle_average )) / LATENCY_FACTOR );
					
					new_limit -= Math.min( decrement, MAX_DECREMENT );
					
						// don't drop below the current protocol upload speed. This is to address
						// the situation whereby it is downloading that is choking the line - killing
						// protocol upspeed kills the downspeed
					
					if ( new_limit < upload_short_prot_average.getAverage() + 1024 ){
						
						new_limit = (int)upload_short_prot_average.getAverage() + 1024;
					}
				}			
				
				if ( new_limit < 1024 ){
					
					new_limit	= 1024;
				}
			}
		
				// final tidy up
			
			if ( MIN_UP > 0 && new_limit < MIN_UP && mode != MODE_FORCED_MIN  ){
				
				new_limit = MIN_UP;
				
			}else if ( MAX_UP > 0 &&  new_limit > MAX_UP && mode != MODE_FORCED_MAX ){
				
				new_limit = MAX_UP;
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
			
			if ( ADJUST_DOWNLOAD_ENABLE && !( Float.isInfinite( ADJUST_DOWNLOAD_RATIO ) || Float.isNaN( ADJUST_DOWNLOAD_RATIO ))){
				
				int	dl_limit = (int)(new_limit * ADJUST_DOWNLOAD_RATIO);
				
				adapter.setCurrentDownloadLimit( dl_limit );
			}
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
		COConfigurationManager.setParameter( CONFIG_MIN_UP, speed );
	}
	
	public int
	getMinumumUploadSpeed()
	{
		return( MIN_UP );
	}
	
	public void
	setMaximumUploadSpeed(
		int		speed )
	{
		COConfigurationManager.setParameter( CONFIG_MAX_UP, speed );
	}
	
	public int
	getMaximumUploadSpeed()
	{
		return( MAX_UP );
	}
	
	public int
	getChokePingTime()
	{
		return( PING_CHOKE_TIME );
	}
	
	public void
	setChokePingTime(
		int		milliseconds )
	{
		COConfigurationManager.setParameter( CONFIG_CHOKE_PING, milliseconds );
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
	
	public SpeedManagerPingSource[]
	getPingSources()
	{
		return( contacts_array );
	}
	
	public int
	getIdlePingMillis()
	{
		return( idle_average );
	}
	
	public int
	getCurrentPingMillis()
	{
		return( (int)ping_average_history.getAverage());
	}
	
	public int
	getMaxPingMillis()
	{
		return( max_ping );
	}
	
		/**
		 * Returns the current view of when choking occurs
		 * @return speed in bytes/sec
		 */
	
	public int
	getCurrentChokeSpeed()
	{
		return((int)choke_speed_average.getAverage());
	}
	
	public int
	getMaxUploadSpeed()
	{
		return( max_upload_average );
	}
	
	protected void
	log(
		String		str )
	{
		if ( DEBUG ){
			
			System.out.println( str );
		}
	}
	
	protected class
	pingContact
		implements SpeedManagerPingSource
	{
		private DHTSpeedTesterContact	contact;
		
		private int	bad_pings;
		private int	last_good_ping;
		
		protected
		pingContact(
			DHTSpeedTesterContact	_contact )
		{
			contact	= _contact;
		}
		
		void
		pingReceived(
			int		time,
			boolean	good_ping )
		{
			if ( good_ping ){
				
				bad_pings = 0;
				
				last_good_ping	= time;
				
			}else{
				
				bad_pings++;
			}
			
				// three strikes and you're out!
			
			if ( bad_pings == 3 ){
				
				contact.destroy();
			}
		}
		
		public InetSocketAddress
		getAddress()
		{
			return( contact.getContact().getAddress());	
		}
		
		public int
		getPingTime()
		{
			return( last_good_ping );
		}
	}
}
