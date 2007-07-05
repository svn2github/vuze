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
	private static final int SHORT_PERIOD_SECS	= 30;
	
	private static final int LONG_PERIOD_TICKS 	= LONG_PERIOD_SECS / CONTACT_PING_SECS;
	private static final int SHORT_PERIOD_TICKS = SHORT_PERIOD_SECS / CONTACT_PING_SECS;
	
	private static final int SHORT_ESTIMATE_SECS	= 15;
	private static final int MEDIUM_ESTIMATE_SECS	= 150;
	private static final int LONG_ESTIMATE_SECS		= 1500;
	
	private static final int SHORT_ESTIMATE_SAMPLES		= SHORT_ESTIMATE_SECS/CONTACT_PING_SECS;
	private static final int MEDIUM_ESTIMATE_SAMPLES	= MEDIUM_ESTIMATE_SECS/CONTACT_PING_SECS;
	private static final int LONG_ESTIMATE_SAMPLES		= LONG_ESTIMATE_SECS/CONTACT_PING_SECS;

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

	//private pingMapper		long_ping_abs_mapper 	= new pingMapper( "l_abs", LONG_PERIOD_TICKS, false );
	//private pingMapper		short_ping_abs_mapper 	= new pingMapper( "s_abs", SHORT_PERIOD_TICKS, false );
	
	private pingMapper		long_ping_var_mapper 	= new pingMapper( "l_var", LONG_PERIOD_TICKS, true);
	//private pingMapper		short_ping_var_mapper 	= new pingMapper( "s_var", SHORT_PERIOD_TICKS, true );

	private pingMapper[] ping_mappers = 
		new pingMapper[]{ 
			//long_ping_abs_mapper, 
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
		// private final int NEAR_PERCENT	= 33;
		private final int MAX_PINGS;
		
		private String	name;
		private boolean	variance;
				
		private int	ping_count;
		
		private pingValue[]	pings;
		
		private pingValue	prev_ping;
		
		private List	regions;
			
		private int last_x;
		private int	last_y;
		
		private int[]	recent_metrics = new int[3];
		private int		recent_metrics_next;
		
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
			
			init();
		}
		
		public String
		getName()
		{
			return( name );
		}
		
		protected void
		init()
		{
			regions = new ArrayList();
			
			prev_ping	= null;
			/*
			region r = new region( 0, 0, 65535, 65535 );
			
			addRegion( r );
			*/
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
				
				//System.out.println( "Clean up!" );
				
				int	to_discard = MAX_PINGS/10;
				
				if ( to_discard < 3 ){
					
					to_discard = 3;
				}
				
				ping_count = MAX_PINGS - to_discard;

				System.arraycopy(pings, to_discard, pings, 0, MAX_PINGS - to_discard );
				
				init();
				
				for (int i=0;i<ping_count;i++){
					
					pingValue	p = pings[i];
										
					p.setIndex( i );
					
					addPing( p );
				}
			}
			
			int	index = ping_count++;
			
			pingValue	ping = new pingValue( x, y, metric, index );

			pings[index] = ping;
			
			addPing( ping );
		}
		
		protected void
		addPing(
			pingValue	ping )
		{			
			if ( prev_ping != null ){
				
				regions.add( new region(prev_ping,ping));
			}
			
			prev_ping = ping;
		}
		
		/*
		protected void
		addPing(
			pingValue	ping )
		{			
			region hit = null;
					
			int	x = ping.getX();
			int	y = ping.getY();
			
			for (int i=0;i<regions.size();i++){
				
				region r = (region)regions.get(i);
				
				if ( r.contains( x, y )){
										
					hit = r;
					
					break;
				}
			}
			
			if ( hit == null ){
				
				System.out.println( "bork bork" );
				
				return;
			}
			
			addPing( hit, ping );
		}
		
		protected void
		addPing(
			region		r,
			pingValue	ping )
		{
			pingValue[]	existing_pings = r.getPings();
			
			if ( existing_pings == null ){
				
				r.addPing( ping );
				
				//System.out.println( "    adding ping as region empty: " + r.getString());

				return;
			}
			
			int	hit_x1 = r.getX1();
			int	hit_x2 = r.getX2();
			int	hit_y1 = r.getY1();
			int	hit_y2 = r.getY2();
			
			int	width 	= ( hit_x2 - hit_x1 )+1;
			int height 	= ( hit_y2 - hit_y1 )+1;
			
			if ( width == 1 && height == 1 ){
				
				r.addPing( ping );
				
				//System.out.println( "    merging ping due region being minimal: " + r.getString());

				return;
			}		
			
				// if we're within X% of existing values for this region then just add to the
				// region rather than splitting
			
			int	min_existing = Integer.MAX_VALUE;
			int	max_existing = 0;
			
			for (int i=0;i<existing_pings.length;i++){
				
				pingValue	p = existing_pings[i];
				
				int	p_metric = p.getMetric();
				
				if ( p_metric < min_existing ){
					
					min_existing = p_metric;
				}
				
				if ( p_metric > max_existing ){
					
					max_existing = p_metric;
				}
			}
			
				// X% bigger than smallest and X% smaller than biggest
			
			int	metric = ping.getMetric();
			
			boolean	merge = 
					( metric >= min_existing && metric <= min_existing + min_existing*NEAR_PERCENT/100 ) ||
					( metric <= max_existing && metric >= max_existing - max_existing*NEAR_PERCENT/100 );
				
			if ( merge ){
				
				r.addPing( ping );
				
				//System.out.println( "    merging ping due to closeness: " + r.getString());
				
				return;
			}
			

			region r1;
			region r2;
			
			if ( width > height || ( width == height && random.nextInt( 2 ) == 1 )){
										
				int	split_x = hit_x1 + ((width-1) / 2 );
			
				r1 = new region( hit_x1, hit_y1, split_x, hit_y2 );
				r2 = new region( split_x+1, hit_y1, hit_x2, hit_y2 );
					
			}else{
										
				int	split_y = hit_y1 + ((height-1) / 2 );
				
				r1 = new region( hit_x1, hit_y1, hit_x2, split_y );
				r2 = new region( hit_x1, split_y+1, hit_x2, hit_y2 );
			}
				
			splitPings( existing_pings, r1, r2 );
			
			removeRegion( r );
			
			addRegion( r1 );
			
			addRegion( r2 );
			
			int	x = ping.getX();
			int y = ping.getY();
			
			if ( r1.contains( x, y )){
				
				addPing( r1, ping );
				
				//System.out.println( "    splitting regions -> " + r1.getString() );

			}else{
				
				addPing( r2, ping );
				
				//System.out.println( "    splitting regions -> " + r2.getString() );
			}
		}
		
		
		protected void
		splitPings(
			pingValue[]	pings,
			region		r1,
			region		r2 )
		{
			for (int i=0;i<pings.length;i++){
			
				pingValue	ping = pings[i];
				
				if ( r1.contains( ping.getX(), ping.getY())){
					
					r1.addPing( ping );
					
				}else{
					
					r2.addPing( ping );
				}
			}
		}
				protected void
		addRegion(
			region	r )
		{
			regions.add( r );
		}
		
		protected void
		removeRegion(
			region	r )
		{
			regions.remove( r );
		}
				
		protected synchronized List
		getRegions()
		{				
			return( new ArrayList( regions ));
		}
		
		protected synchronized void
		checkConsistency()
		{
			Iterator it = regions.iterator();
							
			int	max_x = 0;
			int	max_y = 0;
			
			while( it.hasNext()){
				
				region r = (region)it.next();
				
				if ( r.getPings() == null ){
					
					continue;
				}
				
				max_x = Math.max( max_x, r.getX2());
				max_y = Math.max( max_y, r.getY2());
			}
				
			boolean[][]	grid = new boolean[max_x+1][max_y+1];
			
			it = regions.iterator();
			
			while( it.hasNext()){
				
				region r = (region)it.next();
				
				if ( r.getPings() != null ){
					
					for (int i=r.getX1();i<=r.getX2();i++){
					
						for (int j=r.getY1();j<=r.getY2();j++){
							
							grid[i][j] = true;
						}
					}
				}
			}
			
			for (int i=0;i<=max_x;i++){
				
				String str = "";
				
				for (int j=0;j<max_y;j++){
					
					str += grid[i][j]?".":" ";
				}
				
				System.out.println( str );
			}
			
			grid = new boolean[max_x+1][max_y+1];

			it = regions.iterator();

			boolean	bad = false;
			
			while( it.hasNext()){
				
				region r = (region)it.next();
				
				if ( r.getX1() <= max_x && r.getY1() <= max_y ){
					
					for (int i=r.getX1();i<=r.getX2() && i <= max_x;i++){
					
						for (int j=r.getY1();j<=r.getY2() && j <= max_y;j++){
							
							if ( grid[i][j]){
								
								System.out.println( "Inconsistent at " + i + "," + j );
								
								bad	= true;
								
							}else{
								
								grid[i][j] = true;
							}
						}
					}
				}
			}
			
			for (int i=0;i<=max_x;i++){
				
				for (int j=0;j<max_y;j++){
					
					if ( !grid[i][j]){
						
						System.out.println( "Inconsistent at " + i + "," + j );
						
						bad	= true;
					}
				}
			}
			
			System.out.println( "All regions" );
			
			it = regions.iterator();

			while( it.hasNext()){
				
				region r = (region)it.next();
				
				if ( r.getX1() <= max_x && r.getY1() <= max_y ){

					System.out.println( "    " + r.getX1() + "," + r.getY1() + "," + r.getX2() + "," + r.getY2());
				}
			}
			
			if ( !bad ){
				
				System.out.println( "Consistent!" );
			}
		}
		
		*/
		
		public synchronized int[][]
		getHistory()
		{
			List	result = new ArrayList();

			Iterator it = regions.iterator();
						
			while( it.hasNext()){
				
				region r = (region)it.next();
				
				pingValue[] pings = r.getPings();
					
				if ( pings == null ){
					
					continue;
				}
				
				for (int i=0;i<pings.length;i++){
				
					pingValue	ping = pings[i];
				
					result.add( new int[]{ SPEED_DIVISOR*ping.getX(), SPEED_DIVISOR*ping.getY(), ping.getMetric()} );
				}
			}
			
			return((int[][])result.toArray( new int[result.size()][]));
		}
		
		public synchronized SpeedManagerPingZone[]
		getZones()
		{
			return((SpeedManagerPingZone[])regions.toArray( new SpeedManagerPingZone[regions.size()] ));
		}
		
		public synchronized int
		getEstimatedUploadLimit()
		{
			if ( !variance ){
				
				return( -1 );
			}
			
			int	max_end = 0;
			
			int	num_samples = regions.size();
			
			if ( num_samples == 0 ){
				
				return( -1 );
			}
			
			for (int i=0;i<regions.size();i++){
				
				region r = (region)regions.get(i);
				
				int	end		= r.getUploadEndBytesPerSec()/SPEED_DIVISOR;
				
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
			
			int[]	results = new int[samples.length];
			
			for (int sample=0;sample<samples.length;sample++){
				
				int[]	totals 	= new int[max_end+1];
				short[]	hits	= new short[max_end+1];				
				short[] when	= new short[max_end+1];
				
				int	sample_count = samples[sample];
				
				for (int i=1;i<=sample_count;i++){

					region r = (region)regions.get( num_samples-i );
				
					int	start 	= r.getUploadStartBytesPerSec()/SPEED_DIVISOR;
					int	end		= r.getUploadEndBytesPerSec()/SPEED_DIVISOR;
					int	metric	= r.getMetric();
				
					for (int j=start;j<=end;j++){
						
						totals[j] += metric;
						hits[j]++;
						
						if ( j > when[j] ){
							
							when[j] = (short)j;
						}
					}
				}
				
				int	worst = 0;
				
				for (int i=0;i<totals.length;i++){
					
					int	hit = hits[i];
					
					if ( hit > 0 ){
						
						int	average = totals[i]/hit;
						
						totals[i] = average;
						
						if ( average >= VARIANCE_BAD_VALUE ){
							
							worst = VARIANCE_BAD_VALUE;
							
						}else if ( average >= VARIANCE_GOOD_VALUE && worst < VARIANCE_GOOD_VALUE ){
							
							worst = VARIANCE_GOOD_VALUE;
						}
					}
				}
				
				int	estimate		= -1;
				int	estimate_when	= -1;
				
				if ( worst > 0 ){
					
					int	zone_start		= -1;
					int	zone_max_hit	= 0;
					int	zone_max_time	= -1;
					
					for (int i=0;i<totals.length;i++){
						
						int	average = totals[i];
						int	hit 	= hits[i];
						
						if ( average >= worst && hit >= zone_max_hit ){
							
							if ( zone_start == -1 || hit > zone_max_hit ){
								
								zone_start 		= i;
								zone_max_hit	= hit;
							}
							
							int	w = when[i];
							
							if ( w > zone_max_time ){
								
								zone_max_time = w;
							}
						}else{
							
							if ( zone_start != -1 ){
								
								if ( zone_max_time > estimate_when ){
								
									estimate 		= zone_start + (i-zone_start)/2;
									estimate_when	= zone_max_time;
								}
								
								zone_start		= -1;
								zone_max_hit	= 0;
								zone_max_time	= 0;

							}
						}
					}
					
					if ( zone_start != -1 ){
						
						if ( zone_max_time > estimate_when ){
						
							estimate 		= zone_start + (totals.length-zone_start)/2;
							estimate_when	= zone_max_time;
						}
					}
				}
				
				results[sample] = estimate;
			}
			
			String	str = "";
			
			int	result = -1;
			
			for (int i=0;i<results.length;i++){
				
				int	r = results[i];
				
				str += (i==0?"":",") + r;
				
				if ( r != -1 && result == -1 ){
					
					result = r;
				}
			}
			
			System.out.println( "Estimate up->" + str );
			
			return( result );
		}
		
		public int
		getEstimatedDownloadLimit()
		{
			return( 0 );
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
			private short	index;
			
			protected
			pingValue(
				int		_x,
				int		_y,
				int		_m,
				int		_i )
			{
				x		= (short)_x;
				y		= (short)_y;
				metric	= (short)_m;
				index	= (short)_i;
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
			
			protected int
			getIndex()
			{
				return(((int)(index))&0xffff );
			}
			
			protected void
			setIndex(
				int		i )
			{
				index	= (short)i;
			}
			
			protected String
			getString()
			{
				return("x=" + getX()+",y=" + getY() +",m=" + getMetric());
			}
		}
		/*
		class
		region
			implements SpeedManagerPingZone
		{
			private short		x1;
			private short		y1;
			private short		x2;
			private short		y2;
			
			private pingValue[]		pings;
			
			protected
			region(
				int		_x1,
				int		_y1,
				int		_x2,
				int		_y2 )
			{
				x1		= (short)_x1;
				y1		= (short)_y1;
				x2		= (short)_x2;
				y2		= (short)_y2;
			}
			
			public int
			getX1()
			{
				return(((int)x1)&0xffff );
			}
			
			public int
			getY1()
			{
				return(((int)y1)&0xffff );
			}
			
			public int
			getX2()
			{
				return(((int)x2)&0xffff );
			}
			
			public int
			getY2()
			{
				return(((int)y2)&0xffff );
			}
			
			public pingValue[]
			getPings()
			{
				return( pings );
			}
			
			public void
			addPing(
				pingValue	ping )
			{
				if ( pings == null ){
					
					pings = new pingValue[]{ ping };
					
				}else{
					
					pingValue[]	new_pings = new pingValue[pings.length+1];
					
					new_pings[0] = ping;
					
					System.arraycopy( pings, 0, new_pings, 1, pings.length );
					
					pings = new_pings;
				}
			}
			
			public boolean
			contains(
				int		x,
				int		y )
			{
				return( getX1() <= x && getX2() >= x && getY1() <= y && getY2() >= y );
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
				pingValue[] p = pings;
				
				if ( p == null || p.length == 0 ){
					
					return( 0 );
				}
				
				long	total = 0;
				
				for (int i=0;i<p.length;i++){
					
					total += p[i].getMetric();
				}
				
				return((int)( total/p.length ));
			}
						
			public String
			getString()
			{
				String	ping_str = "";
				
				for (int i=0;i<pings.length;i++){
					
					ping_str += (i==0?"":",") + pings[i].getString();
				}
				
				return( "x="+getX1() + ",y="+getY1()+",w=" + (getX2()-getX1()+1) +",h=" + (getY2()-getY1()+1) +",p=[" + ping_str + "]" );
			}
		}
		*/
		class
		region
			implements SpeedManagerPingZone
		{
			private pingValue	ping1;
			private pingValue	ping2;
			
			protected
			region(
				pingValue		p1,
				pingValue		p2 )
			{
				ping1 	= p1;
				ping2	= p2;
			}
			
			public int
			getX1()
			{
				return(ping1.getX());
			}
			
			public int
			getY1()
			{
				return(ping1.getY());
			}
			
			public int
			getX2()
			{
				return(ping2.getX());
			}
			
			public int
			getY2()
			{
				return(ping2.getY());
			}
			
			public pingValue[]
			getPings()
			{
				return( new pingValue[]{ ping1, ping2 });
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
				return(( ping1.getMetric() + ping2.getMetric())/2);
			}
						
			public String
			getString()
			{
				String	ping_str = "";
				
				for (int i=0;i<pings.length;i++){
					
					ping_str += (i==0?"":",") + pings[i].getString();
				}
				
				return( "x="+getX1() + ",y="+getY1()+",w=" + (getX2()-getX1()+1) +",h=" + (getY2()-getY1()+1) +",p=[" + ping_str + "]" );
			}
		}
	}
}
