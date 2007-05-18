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


public class 
SpeedManagerImpl 
	implements SpeedManager, SpeedManagerAlgorithmProviderAdapter
{
	private static final int CONTACT_NUMBER		= 3;
	private static final int CONTACT_PING_SECS	= 5;
	
		// config items start
	
	private static int					PING_CHOKE_TIME;
	private static int					MIN_UP;
	private static int					MAX_UP;
	private static boolean				DEBUG;
	private static boolean				ADJUST_DOWNLOAD_ENABLE;
	private static float				ADJUST_DOWNLOAD_RATIO;
	private static int					MAX_INCREMENT;
	private static int					MAX_DECREMENT;
	private static int					LATENCY_FACTOR;
	private static int					FORCED_MIN_SPEED;


	private static final String	CONFIG_AVAIL			= "AutoSpeed Available";	// informative only
	private static final String	CONFIG_MIN_UP			= "AutoSpeed Min Upload KBs";
	private static final String	CONFIG_MAX_UP			= "AutoSpeed Max Upload KBs";
	private static final String	CONFIG_MAX_INC			= "AutoSpeed Max Increment KBs";
	private static final String	CONFIG_MAX_DEC			= "AutoSpeed Max Decrement KBs";
	private static final String	CONFIG_CHOKE_PING		= "AutoSpeed Choking Ping Millis";
	private static final String	CONFIG_DOWNADJ_ENABLE	= "AutoSpeed Download Adj Enable";
	private static final String	CONFIG_DOWNADJ_RATIO	= "AutoSpeed Download Adj Ratio";
	private static final String	CONFIG_LATENCY_FACTOR	= "AutoSpeed Latency Factor";
	private static final String	CONFIG_FORCED_MIN		= "AutoSpeed Forced Min KBs";
	private static final String	CONFIG_DEBUG			= "Auto Upload Speed Debug Enabled";
	
	private static final String[]	CONFIG_PARAMS = {
		CONFIG_MIN_UP, CONFIG_MAX_UP, 
		CONFIG_MAX_INC, CONFIG_MAX_DEC,
		CONFIG_CHOKE_PING, 
		CONFIG_DOWNADJ_ENABLE,
		CONFIG_DOWNADJ_RATIO,
		CONFIG_LATENCY_FACTOR,
		CONFIG_FORCED_MIN,
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

						FORCED_MIN_SPEED	= COConfigurationManager.getIntParameter( CONFIG_FORCED_MIN ) * 1024;

						if ( FORCED_MIN_SPEED < 1024 ){
							FORCED_MIN_SPEED = 1024;
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
	
	
	private AzureusCore			core;
	private DHTSpeedTester		speed_tester;
	private SpeedManagerAdapter	adapter;
	
	private SpeedManagerAlgorithmProvider	provider;
	
	
	private boolean				enabled;
		
	
	private Map							contacts	= new HashMap();
	private volatile int				total_contacts;
	private pingContact[]				contacts_array	= new pingContact[0];
	
	private Object	original_limits;
	
	
	public
	SpeedManagerImpl(
		AzureusCore			_core,
		SpeedManagerAdapter	_adapter )
	{
		core			= _core;
		adapter			= _adapter;

        boolean useBeta=false;
        try{
            useBeta = COConfigurationManager.getBooleanParameter(SpeedManagerAlgorithmProviderV2.SETTING_V2_BETA_ENABLED);
            if(useBeta){
                System.setProperty("azureus.autospeed.alg.provider.version","2");
            }
        }catch(Throwable t){
            Debug.out("SpeedManagerImpl had: "+t.getMessage());
        }

        String	alg = System.getProperty( "azureus.autospeed.alg.provider.version" );
		
		if ( alg == null || alg.equals( "1" )){
			
			provider = new SpeedManagerAlgorithmProviderV1( this );
			
		}else{
			
			provider = new SpeedManagerAlgorithmProviderV2( this );
		}
		
		COConfigurationManager.setParameter( CONFIG_AVAIL, false );
		
		reset();
	}
	
	protected void
	reset()
	{
		total_contacts		= 0;
		
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
						
						synchronized( contacts ){

							for (int i=0;i<st_contacts.length;i++){
								
								pingContact source = sources[i] = (pingContact)contacts.get( st_contacts[i] );
								
								if ( source != null ){
									
									source.setPingTime( round_trip_times[i] );
									
								}else{
									
									miss = true;
								}
							}
						}
						
						if ( miss ){
							
							Debug.out( "Auto-speed: source missing" );
							
						}else{
						
							provider.calculate( sources );
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
	

	
	public boolean
	isAvailable()
	{
		return( speed_tester != null );
	}
		
	public void
	setEnabled(
		boolean		_enabled )
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
									
				adapter.setLimits( original_limits, true, ADJUST_DOWNLOAD_ENABLE );
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
			
			if ( ADJUST_DOWNLOAD_ENABLE && !( Float.isInfinite( ADJUST_DOWNLOAD_RATIO ) || Float.isNaN( ADJUST_DOWNLOAD_RATIO ))){
				
				int	dl_limit = (int)(bytes_per_second * ADJUST_DOWNLOAD_RATIO);
				
				adapter.setCurrentDownloadLimit( dl_limit );
			}
		}
	}
	
	public int
	getCurrentDownloadLimit()
	{
		return( adapter.getCurrentDownloadLimit());
	}


        // config access
	
	public int
	getMaxUp()
	{
		return( MAX_UP );
	}
	
	public int
	getMinUp()
	{
		return( MIN_UP );
	}
	
	public int
	getForcedMinSpeed()
	{
		return( FORCED_MIN_SPEED );
	}
	
	public int
	getMaxIncrement()
	{
		return( MAX_INCREMENT );
	}
	
	public int
	getMaxDecrement()
	{
		return( MAX_DECREMENT );
	}
	
	public int
	getPingChokeTime()
	{
		return( PING_CHOKE_TIME );
	}
	
	public int
	getLatencyFactor()
	{
		return( LATENCY_FACTOR );
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
