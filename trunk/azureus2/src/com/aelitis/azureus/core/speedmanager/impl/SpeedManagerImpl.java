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
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
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
import com.aelitis.azureus.core.speedmanager.SpeedManagerLimitEstimate;
import com.aelitis.azureus.core.speedmanager.SpeedManagerPingMapper;
import com.aelitis.azureus.core.speedmanager.SpeedManagerPingSource;
import com.aelitis.azureus.core.speedmanager.SpeedManagerPingZone;


public class 
SpeedManagerImpl 
	implements SpeedManager, SpeedManagerAlgorithmProviderAdapter
{
	private static final int CONTACT_NUMBER		= 3;
	private static final int CONTACT_PING_SECS	= 5;
	
	private static final int LONG_PERIOD_SECS	= 30*60;
	
	private static final int LONG_PERIOD_TICKS 	= LONG_PERIOD_SECS / CONTACT_PING_SECS;
	
	private static final int SHORT_ESTIMATE_SECS	= 15;
	private static final int MEDIUM_ESTIMATE_SECS	= 150;
	
	private static final int SHORT_ESTIMATE_SAMPLES		= SHORT_ESTIMATE_SECS/CONTACT_PING_SECS;
	private static final int MEDIUM_ESTIMATE_SAMPLES	= MEDIUM_ESTIMATE_SECS/CONTACT_PING_SECS;

	private static final int VARIANCE_GOOD_VALUE		= 50;
	private static final int VARIANCE_BAD_VALUE			= 150;
	
	private static final int SPEED_DIVISOR = 256;
	
		// config items start
	
	private static boolean				DEBUG;

    public  static final String CONFIG_VERSION_STR      = "Auto_Upload_Speed_Version_String"; //Shadow of CONFIG_VERSION for config.
    public  static final String	CONFIG_VERSION			= "Auto Upload Speed Version";
	private static final String	CONFIG_AVAIL			= "AutoSpeed Available";	// informative only

	private static final String	CONFIG_DEBUG			= "Auto Upload Speed Debug Enabled";
	
	
	private static final String[]	CONFIG_PARAMS = {
		CONFIG_DEBUG };
		
	static{
		COConfigurationManager.addAndFireParameterListeners(
				CONFIG_PARAMS,
				new ParameterListener()
				{
					public void 
					parameterChanged(
						String parameterName )
					{						
						DEBUG = COConfigurationManager.getBooleanParameter( CONFIG_DEBUG );
					}
				});
		
	}
		// config end
	
	
	private AzureusCore			core;
	private DHTSpeedTester		speed_tester;
	private SpeedManagerAdapter	adapter;
	
	private SpeedManagerAlgorithmProvider	provider;
	
	
	private	int					provider_version	= -1;
	private boolean				enabled;

    private Map							contacts	= new HashMap();
	private volatile int				total_contacts;
	private pingContact[]				contacts_array	= new pingContact[0];
	
	private Object	original_limits;
	
	private AsyncDispatcher	dispatcher = new AsyncDispatcher();

	private pingMapper		long_ping_abs_mapper 	= new pingMapper( "Abs", LONG_PERIOD_TICKS, false );
	//private pingMapper		short_ping_abs_mapper 	= new pingMapper( "s_abs", SHORT_PERIOD_TICKS, false );
	
	private pingMapper		long_ping_var_mapper 	= new pingMapper( "Var", LONG_PERIOD_TICKS, true);
	//private pingMapper		short_ping_var_mapper 	= new pingMapper( "s_var", SHORT_PERIOD_TICKS, true );

	private pingMapper[] ping_mappers = 
		new pingMapper[]{ 
			long_ping_abs_mapper, 
			// short_ping_abs_mapper,
			long_ping_var_mapper, 
			// short_ping_var_mapper 
		};
	
	public
	SpeedManagerImpl(
		AzureusCore			_core,
		SpeedManagerAdapter	_adapter )
	{
		core			= _core;
		adapter			= _adapter;

		COConfigurationManager.addAndFireParameterListener( 
			CONFIG_VERSION,
			new ParameterListener()
			{
				public void
				parameterChanged(
					final String name )
				{
					dispatcher.dispatch(
						new AERunnable()
						{
							public void
							runSupport()
							{
								boolean	do_reset = provider_version == -1;
								
								int version = COConfigurationManager.getIntParameter( name );
								
								if ( version != provider_version ){
									
									provider_version = version;
									
									if ( isEnabled()){
										
										setEnabledSupport( false );
										
										setEnabledSupport( true );
									}
								}
								
								if ( do_reset ){
									
									reset();
								}
							}
						});
				}
			});
		
		COConfigurationManager.setParameter( CONFIG_AVAIL, false );
	}
	
	protected void
	reset()
	{
		total_contacts		= 0;
		
		if ( provider_version == 1 ){
			
			if ( !( provider instanceof SpeedManagerAlgorithmProviderV1 )){
				
				provider = new SpeedManagerAlgorithmProviderV1( this );
			}
		}else if ( provider_version == 2 ){
			
			if ( !( provider instanceof SpeedManagerAlgorithmProviderV2 )){
				
				provider = new SpeedManagerAlgorithmProviderV2( this );
			}
		}else if ( provider_version == 3 ){
			
			if ( !( provider instanceof SMUnlimited )){
				
				provider = new SMUnlimited();
			}
		}else{
			
			Debug.out( "Unknown provider version " + provider_version );
			
			return;
		}
		
		provider.reset();
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
						DHTSpeedTesterContact contact )
					{
						if ( core.getInstanceManager().isLANAddress(contact.getContact().getAddress().getAddress())){
							
							contact.destroy();
							
						}else{
							log( "activePing: " + contact.getContact().getString());
							
							contact.setPingPeriod( CONTACT_PING_SECS );
							
							synchronized( contacts ){
								
								pingContact	source = new pingContact( contact );
								
								contacts.put( contact, source );
								
								contacts_array = new pingContact[ contacts.size() ];
								
								contacts.values().toArray( contacts_array );
							
								total_contacts++;
							
								provider.pingSourceFound( source, total_contacts > CONTACT_NUMBER );
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
											
											pingContact source = (pingContact)contacts.remove( contact );
											
											if ( source != null ){
												
												contacts_array = new pingContact[ contacts.size() ];
												
												contacts.values().toArray( contacts_array );
												
												provider.pingSourceFailed( source );
											}
										}
									}
								});
						}
					}
					
					public void
					resultGroup(
						DHTSpeedTesterContact[]	st_contacts,
						int[]					round_trip_times )
					{
						if ( !enabled ){
							
							for (int i=0;i<st_contacts.length;i++){
								
								st_contacts[i].destroy();
							}
							
							return;
						}
						
						pingContact[]	sources = new pingContact[st_contacts.length];
					
						boolean	miss = false;
						
						int	worst_value	= -1;
						
						int	num_values	= 0;
						int	total		= 0;
						
						synchronized( contacts ){

							for (int i=0;i<st_contacts.length;i++){
								
								pingContact source = sources[i] = (pingContact)contacts.get( st_contacts[i] );
								
								if ( source != null ){
									
									int	rtt = round_trip_times[i];
									
									if ( rtt > 0 ){
										
										if ( rtt > worst_value ){
											
											worst_value = rtt;
										}
										
										num_values++;
										
										total += rtt;
									}
									
									source.setPingTime( rtt );
									
								}else{
									
									miss = true;
								}
							}
						}
						
						if ( miss ){
							
							Debug.out( "Auto-speed: source missing" );
							
						}else{
						
							provider.calculate( sources );
							
								// remove worst value if we have > 1
							
							if ( num_values > 1 ){
								
								total -= worst_value;
								num_values--;
							}

							if ( num_values> 0 ){
								
								addPingHistory( total/num_values );
							}
						}
					}
				});
		
		SimpleTimer.addPeriodicEvent(
			"SpeedManager:stats",
			SpeedManagerAlgorithmProvider.UPDATE_PERIOD_MILLIS,
			new TimerEventPerformer()
			{
				public void
				perform(
					TimerEvent	event )
				{
					provider.updateStats();
				}
			});
	}
	


	protected void
	addPingHistory(
		int		rtt )
	{
		int	average_period = 3000;
		
		int	x	= (adapter.getCurrentDataUploadSpeed(average_period) + adapter.getCurrentProtocolUploadSpeed(average_period));
		int	y 	= (adapter.getCurrentDataDownloadSpeed(average_period) + adapter.getCurrentProtocolDownloadSpeed(average_period));
		
		for (int i=0;i<ping_mappers.length;i++){
			
			ping_mappers[i].addPing( x, y, rtt );
		}
	}
	
	public boolean
	isAvailable()
	{
		return( speed_tester != null );
	}
		
	public void
	setEnabled(
		final boolean		_enabled )
	{
			// unfortunately we need this to run synchronously as the caller may be disabling it
			// and then setting speed limits in which case we can't go async and restore the
			// original values below and overwrite the new limit...
		
		final AESemaphore	sem = new AESemaphore( "SpeedManagerImpl.setEnabled" );
		
			// single thread enable/disable (and derivative reset) ops
		
		dispatcher.dispatch(
			new AERunnable()
			{
				public void
				runSupport()
				{
					try{
						setEnabledSupport( _enabled );
						
					}finally{
						
						sem.release();
					}
				}
			});
		
		if ( !sem.reserve( 10000 )){
			
			Debug.out( "operation didn't complete in time" );
		}
	}
	
	protected void
	setEnabledSupport(
		boolean	_enabled )
	{
		if ( enabled != _enabled ){
			
			if ( _enabled ){
				
				original_limits	= adapter.getLimits();
			}
			
			reset();
			
			enabled	= _enabled;
			
			if ( speed_tester != null ){
				
				speed_tester.setContactNumber( enabled?CONTACT_NUMBER:0);
			}
			
			if ( !enabled ){
									
				adapter.setLimits( original_limits, true, provider.getAdjustsDownloadLimits());
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
	
	public SpeedManagerPingMapper[]
	getMappers()
	{
		return( ping_mappers );
	}
	
	public int
	getIdlePingMillis()
	{
		return( provider.getIdlePingMillis());
	}
	
	public int
	getCurrentPingMillis()
	{
		return( provider.getCurrentPingMillis());
	}
	
	public int
	getMaxPingMillis()
	{
		return( provider.getMaxPingMillis());
	}
	
		/**
		 * Returns the current view of when choking occurs
		 * @return speed in bytes/sec
		 */
	
	public int
	getCurrentChokeSpeed()
	{
		return( provider.getCurrentChokeSpeed());
	}
	
	public int
	getMaxUploadSpeed()
	{
		return( provider.getMaxUploadSpeed());
	}

	public int
	getCurrentUploadLimit()
	{
		return( adapter.getCurrentUploadLimit());
	}
	
	public void
	setCurrentUploadLimit(
		int		bytes_per_second )
	{
		if ( enabled ){
			
			adapter.setCurrentUploadLimit( bytes_per_second );
		}
	}
	
	public int
	getCurrentDownloadLimit()
	{
		return( adapter.getCurrentDownloadLimit());
	}

	public void 
	setCurrentDownloadLimit(
		int bytes_per_second) 
	{
		adapter.setCurrentDownloadLimit( bytes_per_second );
	}
		
	public int
	getCurrentProtocolUploadSpeed()
	{
		return( adapter.getCurrentProtocolUploadSpeed(-1));
	}
	
	public int
	getCurrentDataUploadSpeed()
	{
		return( adapter.getCurrentDataUploadSpeed(-1));
	}
	
    public int
    getCurrentDataDownloadSpeed()
    {
        return( adapter.getCurrentDataDownloadSpeed(-1) );
    }

    public int
    getCurrentProtocolDownloadSpeed()
    {
        return( adapter.getCurrentProtocolDownloadSpeed(-1) );
    }

	public void
	setLoggingEnabled(
		boolean	enabled )
	{
		COConfigurationManager.setParameter( CONFIG_DEBUG, enabled );
	}
	
	public void
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
		
		private int	ping_time;
		
		protected
		pingContact(
			DHTSpeedTesterContact	_contact )
		{
			contact	= _contact;
		}
		
		protected void
		setPingTime(
			int		time )
		{
			ping_time = time;
		}
		
		public InetSocketAddress
		getAddress()
		{
			return( contact.getContact().getAddress());	
		}
		
		public int
		getPingTime()
		{
			return( ping_time );
		}
		
		public void
		destroy()
		{
			contact.destroy();
		}
	}
	
	protected class
	SMUnlimited
		implements SpeedManagerAlgorithmProvider
	{
		public void
		reset()
		{
			adapter.setCurrentDownloadLimit( 0 );
			adapter.setCurrentUploadLimit( 0 );
		}
		
		public void
		updateStats()
		{
		}
		
		public void
		pingSourceFound(
			SpeedManagerPingSource		source,
			boolean						is_replacement )
		{
		}
		
		public void
		pingSourceFailed(
			SpeedManagerPingSource		source )
		{
		}
				
		public void
		calculate(
			SpeedManagerPingSource[]	sources )
		{
			SpeedManagerLimitEstimate est = long_ping_var_mapper.getEstimatedUploadLimit();
			
			if ( est != null ){
				
				if ( est.getMetric() > 100 ){
					
					adapter.setCurrentUploadLimit( est.getBytesPerSec() - 1024 );
					
				}else if ( est.getMetric() >= 50 ){
						
					adapter.setCurrentUploadLimit( est.getBytesPerSec() + 1024 );
						
				}else if ( est.getMetric() < 50 ){
					
					adapter.setCurrentUploadLimit( est.getBytesPerSec() + 5*1024);

				}
			}
			
		}
				
		public int
		getIdlePingMillis()
		{
			return( 0 );
		}
		
		public int
		getCurrentPingMillis()
		{
			return( 0 );
		}
		
		public int
		getMaxPingMillis()
		{
			return( 0 );
		}
				
		public int
		getCurrentChokeSpeed()
		{
			return( 0 );
		}
		
		public int
		getMaxUploadSpeed()
		{
			return( 0 );
		}
			
		public boolean
		getAdjustsDownloadLimits()
		{
			return( true );
		}
	}
	
	protected static class
	pingMapper
		implements SpeedManagerPingMapper
	{
		private final int MAX_PINGS;
		
		private String	name;
		private boolean	variance;
				
		private int	ping_count;
		
		private pingValue[]	pings;
		
		private pingValue	prev_ping;
		
		private LinkedList	regions;
			
		private int last_x;
		private int	last_y;
		
		private int[]	recent_metrics = new int[3];
		private int		recent_metrics_next;
		
		private limitEstimate[]	up_estimate		= new limitEstimate[0];
		private limitEstimate[]	down_estimate	= new limitEstimate[0];
		
		protected
		pingMapper(
			String		_name,
			int			_entries ,
			boolean		_variance )
		{
			name		= _name;
			MAX_PINGS 	= _entries;
			variance	= _variance;
			
			pings	= new pingValue[MAX_PINGS];
			
			regions	= new LinkedList();
		}
		
		public String
		getName()
		{
			return( name );
		}
		
		protected synchronized void
		addPing(
			int		x,
			int		y,
			int		metric )
		{
			x = x/SPEED_DIVISOR;
			y = y/SPEED_DIVISOR;
			
			if ( x > 65535 )x = 65535;
			if ( y > 65535 )y = 65535;
			if ( metric > 65535 )metric = 65535;
			if ( metric == 0 )metric = 1;
			
				// ping time won't refer to current x+y due to latencies, apply to average between
				// current and previous
			
			int	average_x = (x + last_x )/2;
			int	average_y = (y + last_y )/2;
			
			last_x	= x;
			last_y	= y;
			
			x	= average_x;
			y	= average_y;
			
			if ( variance ){
				
				recent_metrics[recent_metrics_next++%recent_metrics.length] = metric;
				
				metric = 0;

				if ( recent_metrics_next > 1 ){
					
					int	entries = Math.min( recent_metrics_next, recent_metrics.length );
					
					int total = 0;
					
					for (int i=0;i<entries;i++){
						
						total += recent_metrics[i];
					}
					
					int	average = total/entries;
					
					int	total_deviation = 0;
					
					for (int i=0;i<entries;i++){

						int	deviation = recent_metrics[i] - average;
						
						total_deviation += deviation * deviation;
					}
					
					metric = (int)Math.sqrt( total_deviation );
				}
			}
			
			if ( ping_count == MAX_PINGS ){

					// discard oldest pings and reset 
								
				int	to_discard = MAX_PINGS/10;
				
				if ( to_discard < 3 ){
					
					to_discard = 3;
				}
				
				ping_count = MAX_PINGS - to_discard;

				System.arraycopy(pings, to_discard, pings, 0, MAX_PINGS - to_discard );
				
				for (int i=0;i<to_discard;i++ ){
					
					regions.removeFirst();
				}
			}
			
			int	index = ping_count++;
			
			pingValue	ping = new pingValue( x, y, metric );

			pings[index] = ping;
			
			addPing( ping );
		}
		
		protected void
		addPing(
			pingValue	ping )
		{			
			region	new_region = null;
			
			if ( prev_ping != null ){
				
				new_region = new region(prev_ping,ping);
				
				regions.add( new_region );
			}
			
			prev_ping = ping;
			
			if ( variance ){
			
				System.out.println( "Ping: " + ping.getString() + (new_region==null?"":(", region=" + new_region.getString())));
			}
			
			updateLimitEstimates();
		}
		
		public synchronized int[][]
		getHistory()
		{
			int[][]	result = new int[ping_count][];

			for (int i=0;i<ping_count;i++){
				
				pingValue	ping = pings[i];
				
				result[i] = new int[]{ SPEED_DIVISOR*ping.getX(), SPEED_DIVISOR*ping.getY(), ping.getMetric()};
			}
			
			return( result );
		}
		
		public synchronized SpeedManagerPingZone[]
		getZones()
		{
			return((SpeedManagerPingZone[])regions.toArray( new SpeedManagerPingZone[regions.size()] ));
		}
		
		public synchronized SpeedManagerLimitEstimate
		getEstimatedUploadLimit()
		{
			return( getEstimatedLimit( up_estimate ));
		}
		
		public synchronized SpeedManagerLimitEstimate
		getEstimatedDownloadLimit()
		{
			return( getEstimatedLimit( down_estimate ));
		}

		protected synchronized SpeedManagerLimitEstimate
		getEstimatedLimit(
			limitEstimate[]	estimates )
		{
			if ( estimates.length == 0 ){
				
				return( null );
			}
			
			for (int i=0;i<estimates.length;i++){
				
				limitEstimate e = estimates[i];
				
				if ( e.getMetric() >= VARIANCE_BAD_VALUE ){
					
					return( e );
				}
			}
			
			return( estimates[estimates.length-1] );
		}

		protected void
		updateLimitEstimates()
		{
			up_estimate 	= getEstimatedLimit( true );
			
			down_estimate 	= getEstimatedLimit( false );
		}
		
		protected synchronized limitEstimate[]
		getEstimatedLimit(
			boolean		up )
		{
			if ( !variance ){
				
				return( new limitEstimate[0] );
			}
			
			int	num_samples = regions.size();
			
			if ( num_samples == 0 ){
				
				return( new limitEstimate[0] );
			}
			
			Iterator	it = regions.iterator();
			
			int	max_end = 0;
			
			while( it.hasNext()){
				
				region r = (region)it.next();
				
				int	end		= (up?r.getUploadEndBytesPerSec():r.getDownloadEndBytesPerSec())/SPEED_DIVISOR;
				
				if ( end > max_end ){
					
					max_end = end;
				}
			}
			
			int[]	samples;
			
			if ( num_samples >= MEDIUM_ESTIMATE_SAMPLES ){
				
				samples = new int[]{ SHORT_ESTIMATE_SAMPLES, MEDIUM_ESTIMATE_SAMPLES, num_samples };
				
			}else if ( num_samples >= SHORT_ESTIMATE_SAMPLES ){
				
				samples = new int[]{ SHORT_ESTIMATE_SAMPLES, num_samples };

			}else{
				
				samples = new int[]{ num_samples };
			}
			
			limitEstimate[]	results = new limitEstimate[samples.length];
			
			for (int sample=0;sample<samples.length;sample++){
				
				int	sample_end = max_end + 1;
				
				int[]	totals 			= new int[sample_end];
				short[]	hits			= new short[sample_end];				
				short[] when			= new short[sample_end];
				short[]	worst_var_type	= new short[sample_end];
				
					// take the last 'n' samples (at end of list)
				
				int	sample_count = samples[sample];
				
				int	pos = num_samples - sample_count;
				
				ListIterator sample_it = regions.listIterator( pos );
					
					// flatten out all observations into a single munged metric

				for (int i=pos;i<num_samples;i++){

					region r = (region)sample_it.next();
				
					int	start 	= (up?r.getUploadStartBytesPerSec():r.getDownloadStartBytesPerSec())/SPEED_DIVISOR;
					int	end		= (up?r.getUploadEndBytesPerSec():r.getDownloadEndBytesPerSec())/SPEED_DIVISOR;
					int	metric	= r.getMetric();
				
					int	weighted_start;
					
					short	this_var_type;
					
					if ( metric < VARIANCE_GOOD_VALUE ){
					
							// a good variance applies to all speeds up to this one. This means
							// that previously occuring bad variance will get flattened out by
							// subsequent good variance
						
						weighted_start 	= 0;						
						this_var_type 	= 0;
						
					}else if ( metric < VARIANCE_BAD_VALUE ){
						
							// medium values, treat at face value
						
						weighted_start 	= start;
						this_var_type	= VARIANCE_GOOD_VALUE;

					}else{
						
							// bad ones, treat at face value
						
						weighted_start 	= start;
						this_var_type	= VARIANCE_BAD_VALUE;
					}
					
					for (int j=weighted_start;j<=end;j++){
					
							// a bad variance resets totals as we have encountered this after (in time)
							// the existing data and this is more relevant and replaces any feel good
							// factor we might have accumulated via prior observations
						
						if ( this_var_type == VARIANCE_BAD_VALUE && worst_var_type[j] < this_var_type ){
							
							totals[j]	= 0;
							hits[j]		= 0;
							when[j]		= 0;
							
							worst_var_type[j] = this_var_type;
						}
						
						totals[j] += metric;
						hits[j]++;
							
							// keep track of most recent observation pertaining to this value
						
						if ( i > when[j] ){
							
							when[j] = (short)i;
						}
					}
				}

					// now average out values based on history computed above
								
				for (int i=0;i<sample_end;i++){
					
					int	hit = hits[i];
					
					if ( hit > 0 ){
						
						int	average = totals[i]/hit;
						
						totals[i] = average;
						
						if ( average < VARIANCE_GOOD_VALUE ){
		
							worst_var_type[i] = 0;
						
						}else if ( average < VARIANCE_BAD_VALUE ){
						
							worst_var_type[i] = VARIANCE_GOOD_VALUE;

						}else{
							
							worst_var_type[i] = VARIANCE_BAD_VALUE;
						}
					}
				}
				
					// now we look for the most recent worst area of contiguous badness
				
				int	estimate		= -1;
				int	estimate_when	= 0;
				int	estimate_hits	= -1;
									
				int	zone_start		= -1;
				int	zone_max_hit	= 0;
				int	zone_max_time	= 0;
				
				int	worst_var		= 0;
				
				int	last_average 		= -1;
				int	last_average_change	= 0;
				
				List segments = new ArrayList(totals.length);
				
				for (int i=0;i<sample_end;i++){
					
					int var		= worst_var_type[i];
					int	hit 	= hits[i];
					
					int average = totals[i];
					
					if ( i == 0 ){
						
						last_average = average;
						
					}else if ( last_average != average ){
						
						segments.add( new int[]{ last_average, last_average_change*SPEED_DIVISOR, (i-1)*SPEED_DIVISOR });
						
						last_average 		= average;
						last_average_change	= i;
					}
					
					if ( var >= worst_var ){
						
						if ( var > worst_var || zone_start == -1 ){
					
							// start a new zone and discard any previous results as things have got worse
							
							worst_var		= var;
							
							zone_start 		= i;
							zone_max_hit	= hit;
							zone_max_time	= 0;
							
							estimate_when	= 0;	// forget any previous zone stats
							
						}else{
							
								// continuation of zone
						
							zone_max_hit = Math.max( zone_max_hit, hit );
						}
						
							// keep track of most recent contribution to this zone
						
						int	w = when[i];
						
						if ( w > zone_max_time ){
							
							zone_max_time = w;
						}
					}else{
						
							// zone ended - capture details if this is more recent
						
						if ( zone_start != -1 ){
							
							if ( zone_max_time > estimate_when ){
							
									// if zone has contiguous time region at start then take middle of this
									// when bad variance as we want to err on the side of caution
								
								if ( worst_var == VARIANCE_BAD_VALUE ){
									
									int	start_when = when[zone_start];
									
									int	k;
									
									for (k=zone_start+1;k<i;k++){
									
										if ( when[k] != start_when )break;
									}
									
									estimate 		= zone_start + (k-zone_start)/2;
									
								}else{
									
									estimate		= i-1;
								}
								
								estimate_when	= zone_max_time;
								estimate_hits	= zone_max_hit;
							}
							
							zone_start		= -1;
							zone_max_hit	= 0;
							zone_max_time	= 0;
						}
					}
				}
				
				if ( zone_start != -1 ){
					
						// capture any trailing zone
					
					if ( zone_max_time > estimate_when ){
					
						if ( worst_var == VARIANCE_BAD_VALUE ){

							int	start_when = when[zone_start];
							
							int	k;
							
							for (k=zone_start+1;k<sample_end;k++){
							
								if ( when[k] != start_when )break;
							}
							
							estimate 		= zone_start + (k-zone_start)/2;
							
						}else{
							
							estimate		= sample_end-1;
						}
						
						estimate_when	= zone_max_time;
						estimate_hits	= zone_max_hit;
					}
				}
				
				if ( last_average_change != sample_end - 1 ){
				
					segments.add( new int[]{ last_average, last_average_change*SPEED_DIVISOR, (sample_end-1)*SPEED_DIVISOR });
				}
				
				results[sample] = 
					new limitEstimate(
							estimate==-1?-1:(estimate*SPEED_DIVISOR),
							worst_var, 
							estimate_hits, 
							estimate_when,
							(int[][])segments.toArray(new int[segments.size()][]));
			}
			
			String	str = "";
						
			for (int i=0;i<results.length;i++){
				
				limitEstimate	r = results[i];
				
				str += (i==0?"":",") + r.getString();
			}
			
			if ( variance ){
			
				System.out.println( "Estimate (samples=" + num_samples + ")" + (up?"up":"down") + "->" + str );
			}
			
			return( results );
		}
		
		public synchronized double
		getCurrentMetricRating()
		{
			int	latest_metric = pings[ping_count-1].getMetric();
			
			if ( variance ){
				
				if ( latest_metric < VARIANCE_GOOD_VALUE ){
					
					return( +1 );
					
				}else if ( latest_metric > VARIANCE_BAD_VALUE ){
					
					return( -1 );
					
				}else{
					
					return( 1 - ((double)latest_metric - VARIANCE_GOOD_VALUE )/50 );
				}
			}else{
				
				pingValue[] p = (pingValue[])pings.clone();
				
				if ( ping_count < 3 ){
					
					return(0);
				}
				
				Arrays.sort(
					p,
					0,
					ping_count,
					new Comparator()
					{
						public int 
						compare(
							Object o1, 
							Object o2 ) 
						{
							return(((pingValue)o1).getMetric()-((pingValue)o2).getMetric());
						}
					});
				
				int	p1 = ping_count/3;
				int	p2 = ping_count*2/3;
				
				long	total1 = 0;
				
				for (int i=0;i<p1;i++){
				
					total1 += p[i].getMetric();
				}
				
				long	a1 = total1/p1;
								
				long	total3 = 0;
				
				for (int i=p1;i<ping_count;i++){
				
					total3 += p[i].getMetric();
				}
				
				long	a3 = total3/(ping_count-p2);
				
				if ( latest_metric <= a1 ){
					
					return( +1 );
					
				}else if ( latest_metric >= a3 ){
					
					return( -1 );
					
				}else{
					
					long	diff = a3 - a1;
					
					double	pos = latest_metric - a1;
					
					return( 1 - ( pos / (diff/2)));
				}
			}
		}
		

		

		class
		pingValue
		{
			private short	x;
			private short	y;
			private short	metric;
			
			protected
			pingValue(
				int		_x,
				int		_y,
				int		_m )
			{
				x		= (short)_x;
				y		= (short)_y;
				metric	= (short)_m;
			}
			
			protected int
			getX()
			{
				return(((int)(x))&0xffff );
			}
			
			protected int
			getY()
			{
				return(((int)(y))&0xffff );
			}
			
			protected int
			getMetric()
			{
				return(((int)(metric))&0xffff );
			}
			
			protected String
			getString()
			{
				return("x=" + getX()+",y=" + getY() +",m=" + getMetric());
			}
		}

		class
		region
			implements SpeedManagerPingZone
		{
			private short	x1;
			private short	y1;
			private short	x2;
			private short	y2;
			private short	metric;
			
			protected
			region(
				pingValue		p1,
				pingValue		p2 )
			{
				x1 = (short)p1.getX();
				y1 = (short)p1.getY();
				x2 = (short)p2.getX();
				y2 = (short)p2.getY();
				
				if ( x2 < x1 ){
					short t = x1;
					x1 = x2;
					x2 = t;
				}
				if ( y2 < y1 ){
					short t = y1;
					y1 = y2;
					y2 = t;
				}
				metric = (short)((p1.getMetric()+p2.getMetric())/2);
			}
			
			public int
			getX1()
			{
				return( x1 & 0x0000ffff );
			}
			
			public int
			getY1()
			{
				return( y1 & 0x0000ffff );
			}
			
			public int
			getX2()
			{
				return( x2 & 0x0000ffff );
			}
			
			public int
			getY2()
			{
				return( y2 & 0x0000ffff );
			}
						
			public int
			getUploadStartBytesPerSec()
			{
				return( getX1()*SPEED_DIVISOR );
			}
			
			public int
			getUploadEndBytesPerSec()
			{
				return( getX2()*SPEED_DIVISOR + (SPEED_DIVISOR-1));
			}
			
			public int
			getDownloadStartBytesPerSec()
			{
				return( getY1()*SPEED_DIVISOR );
			}
			
			public int
			getDownloadEndBytesPerSec()
			{
				return( getY2()*SPEED_DIVISOR + (SPEED_DIVISOR-1));
			}
			
			public int
			getMetric()
			{
				return( metric & 0x0000ffff );

			}
						
			public String
			getString()
			{				
				return( "x="+getX1() + ",y="+getY1()+",w=" + (getX2()-getX1()+1) +",h=" + (getY2()-getY1()+1));
			}
		}
		
		class
		limitEstimate
			implements SpeedManagerLimitEstimate
		{
			private int		speed;
			private int		metric;
			private int		when;
			private int		hits;
			
			private int[][]	segs;
			
			protected
			limitEstimate(
				int			_speed,
				int			_metric,
				int			_hits,
				int			_when,
				int[][]		_segs )
			{
				speed		= _speed;
				metric		= _metric;
				hits		= _hits;
				when		= _when;
				segs		= _segs;
			}
			
			public int
			getBytesPerSec()
			{
				return( speed );
			}
			
			public int
			getMetric()
			{
				return( metric );
			}
			
			public int[][]
			getSegments()
			{
				return( segs );
			}
			
			public String
			getString()
			{
				return( "speed=" + DisplayFormatters.formatByteCountToKiBEtc( speed )+
						",metric=" + metric + ",segs=" + segs.length + ",hits=" + hits + ",when=" + when );
			}
		}
	}
	
	public static void
	main(
		String[]	args )
	{
		pingMapper pm = new pingMapper( "test", 100, true );
		
		Random rand = new Random();
		
		int[][] phases = { 
				{ 50, 0, 100000, 50 },
				{ 50, 100000, 200000, 200 },
				{ 50, 50000, 50000, 200 },
				{ 50, 0, 100000, 50 },

		};
		
		for (int i=0;i<phases.length;i++){
			
			int[]	phase = phases[i];
			
			System.out.println( "**** phase " + i );
			
			for (int j=0;j<phase[0];j++){
			
				int	x_base 	= phase[1];
				int	x_var	= phase[2];
				int r = phase[3];
				
				pm.addPing( x_base + rand.nextInt( x_var ), x_base + rand.nextInt( x_var ), rand.nextInt( r ));
			
				SpeedManagerLimitEstimate up 	= pm.getEstimatedUploadLimit();
				SpeedManagerLimitEstimate down 	= pm.getEstimatedDownloadLimit();
				
				if ( up != null && down != null ){
					
					System.out.println( up.getString() + "," + down.getString());
				}
			}
		}
	}
}
