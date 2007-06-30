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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.SystemTime;
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


public class 
SpeedManagerImpl 
	implements SpeedManager, SpeedManagerAlgorithmProviderAdapter
{
	private static final int CONTACT_NUMBER		= 3;
	private static final int CONTACT_PING_SECS	= 5;
	
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

	private pingMapper		ping_mapper = new pingMapper();
	
	
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
		int	rtt )
	{
		int	average_period = 3000;
		
		int	x 	= (adapter.getCurrentDataDownloadSpeed(average_period) + adapter.getCurrentProtocolDownloadSpeed(average_period))/1024;
		int	y	= (adapter.getCurrentDataUploadSpeed(average_period) + adapter.getCurrentProtocolUploadSpeed(average_period))/1024;
				
		// ping_mapper.addPing( x, y, rtt );
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
	
	public int[][]
	getPingHistory()
	{
		return( new int[0][] );
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
	{
		private Random	random = new Random();
		
		private SortedSet x_set = 
			new TreeSet(
				new Comparator()
				{
					public int 
					compare(
						Object o1, 
						Object o2) 
					{
						region	r1 = (region)o1;
						region  r2 = (region)o2;
						
						int	diff = r1.getX1() - r2.getX1();
						
						if ( diff != 0 ){
							
							return( diff );
						}
						
						return( r1.getY1() - r2.getY1() );
					}
				});

		private SortedSet y_set = 
			new TreeSet(
				new Comparator()
				{
					public int 
					compare(
						Object o1, 
						Object o2) 
					{
						region	r1 = (region)o1;
						region  r2 = (region)o2;
						
						int	diff = r1.getY1() - r2.getY1();
						
						if ( diff != 0 ){
							
							return( diff );
						}
						
						return( r1.getX1() - r2.getX1() );
					}
				});
	
		private int	x_splits;
		private int y_splits;
		
		protected
		pingMapper()
		{
			region r = new region( 0, 0, 65535, 65535 );
			
			addRegion( r );
		}
		
		protected synchronized void
		addPing(
			int		x,
			int		y,
			int		rtt )
		{
			if ( x > 65535 )x = 65535;
			if ( y > 65535 )y = 65535;
			if ( rtt > 65535 )rtt = 65535;
			if ( rtt == 0 )rtt = 1;
			
			Object[]	xs = x_set.toArray();
			Object[]	ys = y_set.toArray();
			
			region hit = null;
			
			int	x_index = -1;
			
			for (int i=0;i<xs.length;i++){
				
				region r = (region)xs[i];
				
				if ( r.contains( x, y )){
					
					x_index = i;
					
					hit = r;
					
					break;
				}
			}
			
			int	y_index = -1;
			
			for (int i=0;i<ys.length;i++){
				
				if ( ys[i] == hit ){
					
					y_index = i;
					
					break;
				}
			}
			
			if ( x_index == -1 || y_index == -1 ){
				
				System.out.println( "bork bork" );
				
				return;
			}
			
			long	ping = createPing( x, y, rtt, SystemTime.getCurrentTime());

			addPing( hit, x, y, rtt, ping );
		}
		
		protected void
		addPing(
			region	r,
			int		x,
			int		y,
			int		rtt,
			long	ping )
		{
			long[]	existing_pings = r.getPings();
			
			if ( existing_pings == null ){
				
				r.addPing( ping );
				
			}else{
				int	hit_x1 = r.getX1();
				int	hit_x2 = r.getX2();
				int	hit_y1 = r.getY1();
				int	hit_y2 = r.getY2();
				
				int	width 	= ( hit_x2 - hit_x1 )+1;
				int height 	= ( hit_y2 - hit_y1 )+1;
				
				if ( width == 1 && height == 1 ){
					
					r.addPing( ping );
					
				}else{
					
					
					region r1;
					region r2;
					
					if ( width > height || ( width == height && random.nextInt( 2 ) == 1 )){
						
						x_splits++;
						
						int	split_x = hit_x1 + ((width-1) / 2 );
					
						r1 = new region( hit_x1, hit_y1, split_x, hit_y2 );
						r2 = new region( split_x+1, hit_y1, hit_x2, hit_y2 );
							
					}else{
						
						y_splits++;
						
						int	split_y = hit_y1 + ((height-1) / 2 );
						
						r1 = new region( hit_x1, hit_y1, hit_x2, split_y );
						r2 = new region( hit_x1, split_y+1, hit_x2, hit_y2 );
					}
					
					System.out.println( "x_s=" + x_splits + ",y_s=" + y_splits );
					
					splitPings( existing_pings, r1, r2 );
					
					removeRegion( r );
					
					addRegion( r1 );
					
					addRegion( r2 );
					
					if ( r1.contains( x, y )){
						
						addPing( r1, x, y, rtt, ping );
						
					}else{
						
						addPing( r2, x, y, rtt, ping );
					}
				}
			}
		}
		
		protected long
		createPing(
			int		x,
			int		y,
			int		rtt,
			long	now )
		{
			return( ((long)x) | 
					(((long)y<< 16) &0x00000000ffff0000L) |
					(((long)rtt<<32)&0x0000ffff00000000L) |
					(((long)now<<48)&0xffff000000000000L));
		}
		
		protected void
		splitPings(
			long[]	pings,
			region	r1,
			region	r2 )
		{
			for (int i=0;i<pings.length;i++){
			
				long	ping = pings[i];
				
				if ( r1.contains( getPingX( ping ), getPingY( ping ))){
					
					r1.addPing( ping );
					
				}else{
					
					r2.addPing( ping );
				}
			}
		}
		protected int
		getPingX(
			long	ping )
		{
			return(((int)(ping))&0x0000ffff );
		}
		
		protected int
		getPingY(
			long	ping )
		{
			return(((int)(ping>>16))&0x0000ffff );
		}
		
		protected int
		getPingRTT(
			long	ping )
		{
			return(((int)(ping>>32))&0x0000ffff );
		}
		
		protected int
		getPingTime(
			long	ping )
		{
			return(((int)(ping>>48))&0x0000ffff );
		}
		
		protected void
		addRegion(
			region	r )
		{
			x_set.add( r );
			y_set.add( r );
		}
		
		protected void
		removeRegion(
			region	r )
		{
			x_set.remove( r );
			y_set.remove( r );
		}
		
		protected synchronized List
		getRegions()
		{				
			return( new ArrayList( x_set ));
		}
		
		protected synchronized void
		checkConsistency()
		{
			Iterator it = x_set.iterator();
							
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
			
			it = x_set.iterator();
			
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

			it = x_set.iterator();

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
			
			it = x_set.iterator();

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
		
		class
		region
		{
			private short		x1;
			private short		y1;
			private short		x2;
			private short		y2;
			
			private long[]		pings;
			
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
			
			public long[]
			getPings()
			{
				return( pings );
			}
			
			public void
			setPings(
				long[]	_pings )
			{
				pings = _pings;
			}
			
			public void
			addPing(
				long	ping )
			{
				if ( pings == null ){
					
					pings = new long[]{ ping };
					
				}else{
					
					long[]	new_pings = new long[pings.length+1];
					
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
		}
	}
	
	protected static void
	runTest(
		final Canvas	canvas )
	{
		final pingMapper pm = new pingMapper();
		
		new Thread()
		{
			int MAX = 100;
			
			private Color colour_ping = new Color(canvas.getDisplay(),0,0,0);
			private Color colour_no_ping = new Color(canvas.getDisplay(),255,0,0);
			
			public void
			run()
			{	
				Display d = canvas.getDisplay();
				
				Random	r = new Random();
				
				for (int i=0;i<5000;i++){
					
					pm.addPing( r.nextInt(MAX), r.nextInt(MAX), r.nextInt(MAX));
					
					if ( d.isDisposed()){
						
						break;
					}
					
					repaint( d );
				
					try{
						Thread.sleep(10);
						
					}catch( Throwable e ){
						
					}
				}
				
				pm.checkConsistency();

				while( true ){
					
					if ( d.isDisposed()){
						
						break;
					}
					
					repaint( d );
					
					try{
						Thread.sleep(10);
						
					}catch( Throwable e ){
						
					}
				}
			}
			
			protected void
			repaint(
				Display	d )
			{
				if ( d.isDisposed()){
					
					return;
				}
				
				d.asyncExec(
						new Runnable()
						{
							public void
							run()
							{
								repaintSupport();
							}
						});
			}
			
			protected void
			repaintSupport()
			{
				if ( canvas == null || canvas.isDisposed()){
					
					return;
				}
				
				Rectangle bounds = canvas.getClientArea();
				
				if ( bounds.height < 1 || bounds.height < 1 ){
					
					return;
				}
				
				GC canvas_gc = new GC(canvas);
				
				Image image = new Image( canvas.getDisplay(), bounds );

				GC gc = new GC( image );
				
				
				Iterator it = pm.getRegions().iterator();
				
				int	max_x = 0;
				int	max_y = 0;
				
				while( it.hasNext()){
					
					pingMapper.region r = (pingMapper.region)it.next();
					
					if ( r.getPings() != null ){
						
						max_x = Math.max( max_x, r.getX2());
						max_y = Math.max( max_y, r.getY2());
					}
				}
								
				it = pm.getRegions().iterator();
				
				while( it.hasNext()){
					
					pingMapper.region r = (pingMapper.region)it.next();
					
					if ( r.getPings() != null ){
						
						gc.setBackground( colour_ping );
						
						gc.fillRectangle(
								r.getX1()*bounds.width/max_x, 
								r.getY1()*bounds.height/max_y,
								(r.getX2()-r.getX1()+2)*bounds.width/max_x-1, 
								(r.getY2()-r.getY1()+2)*bounds.height/max_y-1 );
						
					}else if ( r.getX1() <= max_x && r.getY1() <= max_y ){
						
						gc.setBackground( colour_no_ping );
						
						gc.fillRectangle(
								r.getX1()*bounds.width/max_x, 
								r.getY1()*bounds.height/max_y,
								(r.getX2()-r.getX1()+2)*bounds.width/max_x-1, 
								(r.getY2()-r.getY1()+2)*bounds.height/max_y-2 );
					}
				}
				
				gc.dispose();
				
				canvas_gc.drawImage( image, bounds.x, bounds.y );
				
				image.dispose();
				
				canvas_gc.dispose();   	
			}
		}.start();
	}
	
	public static void
	main(
		String[]	args )
	{
		Display   display = new Display();
		Shell shell = new Shell(display);
		shell.setText("Test");
		GridLayout layout = new GridLayout();
		shell.setLayout(layout);
		GridData gridData = new GridData( GridData.FILL_BOTH );
		shell.setLayoutData( gridData );
		
	    Canvas canvas = new Canvas(shell,SWT.NO_BACKGROUND);
		gridData = new GridData( GridData.FILL_BOTH );
		canvas.setLayoutData( gridData );

		shell.setSize(600,600);
		shell.open ();
		
	    runTest( canvas );
	    
		while (!shell.isDisposed ()) {
			if (!display.readAndDispatch ()) display.sleep ();
		}
		display.dispose ();
	}
}
