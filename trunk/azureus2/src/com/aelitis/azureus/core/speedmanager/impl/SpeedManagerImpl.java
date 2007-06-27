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
	
	private LinkedList	ping_histories = new LinkedList();
	
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
		}else{
			
			if ( !( provider instanceof SpeedManagerAlgorithmProviderV2 )){
				
				provider = new SpeedManagerAlgorithmProviderV2( this );
			}
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
		int	down = adapter.getCurrentDataDownloadSpeed() + adapter.getCurrentProtocolDownloadSpeed();
		int	up	 = adapter.getCurrentDataUploadSpeed() + adapter.getCurrentProtocolUploadSpeed();
		
		synchronized( ping_histories ){
			
			ping_histories.addLast( new int[]{ up, down, rtt });
			
			if ( ping_histories.size() > 1000 ){
				
				ping_histories.removeFirst();
			}
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
	
	public int[][]
	getPingHistory()
	{
		synchronized( ping_histories ){
			
			return((int[][])ping_histories.toArray(new int[ping_histories.size()][]));
		}
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
	getCurrentProtocolUploadSpeed()
	{
		return( adapter.getCurrentProtocolUploadSpeed());
	}
	
	public int
	getCurrentDataUploadSpeed()
	{
		return( adapter.getCurrentDataUploadSpeed());
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
    getCurrentDataDownloadSpeed()
    {
        return( adapter.getCurrentDataDownloadSpeed() );
    }

    public int
    getCurrentProtocolDownloadSpeed()
    {
        return( adapter.getCurrentProtocolDownloadSpeed() );
    }

    public int
	getCurrentDownloadLimit()
	{
		return( adapter.getCurrentDownloadLimit());
	}

    /**
     * NOTE: added for V2 SpeedManagerAlgorithmProvider. Need to move upload and download
     * independently within a range.
     * @param bytes_per_second
     */
    public void setCurrentDownloadLimit(int bytes_per_second) {
        adapter.setCurrentDownloadLimit( bytes_per_second );
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
}
