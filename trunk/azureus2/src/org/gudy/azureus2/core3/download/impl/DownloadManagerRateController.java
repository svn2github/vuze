/*
 * Created on May 12, 2010
 * Created by Paul Gardner
 * 
 * Copyright 2010 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package org.gudy.azureus2.core3.download.impl;

import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPeerManagerStats;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.core3.util.TimerEventPeriodic;

import com.aelitis.azureus.core.networkmanager.LimitedRateGroup;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.util.average.Average;
import com.aelitis.azureus.core.util.average.AverageFactory;

public class 
DownloadManagerRateController 
{
	private static Map<PEPeerManager,PMState>		pm_map = new HashMap<PEPeerManager, PMState>();
	
	private static TimerEventPeriodic	timer;
	
	private static AsyncDispatcher	dispatcher = new AsyncDispatcher();
	
	private static boolean enable_limit_handling;
	
	static{
		COConfigurationManager.addAndFireParameterListener(
			"Bias Upload Handle No Limit",
			new ParameterListener()
			{
				public void 
				parameterChanged(
					String parameterName) 
				{
					enable_limit_handling = COConfigurationManager.getBooleanParameter( "Bias Upload Handle No Limit" );
				}
			});
	}
	
	private static volatile int rate_limit	= 0;
	
	private static LimitedRateGroup 
		limiter = 
			new LimitedRateGroup()
			{
				public String 
				getName()
				{
					return( "DMRC" );
				}
		
				public int 
				getRateLimitBytesPerSecond()
				{
					return( rate_limit );
				}
			};
	
	private static final int TIMER_MILLIS			= 1000;
	private static final int AVERAGE_PERIOD_MILLIS	= 25*1000;
			
	private static final Average	complete_up_average 	= AverageFactory.MovingImmediateAverage(AVERAGE_PERIOD_MILLIS/TIMER_MILLIS);
	private static final Average	incomplete_up_average 	= AverageFactory.MovingImmediateAverage(AVERAGE_PERIOD_MILLIS/TIMER_MILLIS);
	
	private static int	tick_count				 = 0;
	private static int	last_tick_processed 	= -1;
			
	public static void
	addPeerManager(
		final PEPeerManager		pm )
	{
		dispatcher.dispatch(
			new AERunnable()
			{
				public void
				runSupport()
				{
					boolean	is_complete = !pm.hasDownloadablePiece();
					
					PEPeerManagerStats pm_stats = pm.getStats();

					long	up_bytes = pm_stats.getTotalDataBytesSentNoLan() + pm_stats.getTotalProtocolBytesSentNoLan();

					if ( is_complete ){
						
						pm.addRateLimiter( limiter, true );
					}
					
					pm_map.put( pm, new PMState( is_complete, up_bytes ));
										
					if ( timer == null ){
						
						timer = 
							SimpleTimer.addPeriodicEvent( 
								"DMRC", 
								TIMER_MILLIS,
								new TimerEventPerformer()
								{
									public void 
									perform(
										TimerEvent event ) 
									{
										dispatcher.dispatch(
											new AERunnable()
											{
												public void
												runSupport()
												{
													update();
												}
											});
									}
								});
					}
				}
			});
	}
	
	public static void
	removePeerManager(
		final PEPeerManager		pm )
	{
		dispatcher.dispatch(
			new AERunnable()
			{
				public void
				runSupport()
				{
					pm_map.remove( pm );
					
					if ( pm_map.size() == 0 ){
						
						timer.cancel();
						
						timer = null;
						
						rate_limit = 0;
					}
				}
			});
	}
	
	private static void
	update()
	{
		tick_count++;
		
		if ((!enable_limit_handling) ||  pm_map.size() == 0 ||  NetworkManager.isSeedingOnlyUploadRate()){
			
			rate_limit = 0;
			
			return;
		}

		long up_lim = NetworkManager.getMaxUploadRateBPSNormal();
		
		if ( up_lim != 0 ){
			
			rate_limit = 0;
			
			return;
		}
		
		rate_limit = 0;;
		
		long	i_up_diff = 0;
		long	c_up_diff = 0;
		
		for ( Map.Entry<PEPeerManager, PMState> entry: pm_map.entrySet()){
			
			PEPeerManager	pm 		= entry.getKey();
			PMState			state 	= entry.getValue();
			
			boolean	is_complete = !pm.hasDownloadablePiece();
			
			PEPeerManagerStats pm_stats = pm.getStats();
	
			long	up_bytes = pm_stats.getTotalDataBytesSentNoLan() + pm_stats.getTotalProtocolBytesSentNoLan();
						
			long	diff = state.setBytesUp( up_bytes );
			
			if ( is_complete ){
				
				c_up_diff += diff;
				
			}else{
				
				i_up_diff += diff;
			}
				
			if ( state.isComplete() != is_complete ){
			
				if ( is_complete ){
					
					pm.addRateLimiter( limiter, true );
					
				}else{
					
					pm.removeRateLimiter( limiter, true );
				}
				
				state.setComplete( is_complete );
			}
		}
		
		if ( last_tick_processed != tick_count - 1 ){
			
			complete_up_average.reset();
			
			incomplete_up_average.reset();
			
		}else{
			
			complete_up_average.update( c_up_diff  );
			
			incomplete_up_average.update( i_up_diff  );
		}
		
		last_tick_processed = tick_count;
		
		//System.out.println( 
		//	"comp=" + DisplayFormatters.formatByteCountToKiBEtcPerSec((long)complete_up_average.getAverage()) +
		//	", incomp=" + DisplayFormatters.formatByteCountToKiBEtcPerSec((long)incomplete_up_average.getAverage()));		
	}
	
	private static class
	PMState
	{
		private boolean		complete;
		private long		bytes_up;
		
		private
		PMState(
			boolean	comp,
			long	b )
		{
			complete 	= comp;
			bytes_up	= b;
		}
		
		private boolean
		isComplete()
		{
			return( complete );
		}
		
		private void
		setComplete(
			boolean	c )
		{
			complete = c;
		}
		
		private long
		setBytesUp(
			long	b )
		{
			long diff = b - bytes_up;
			
			bytes_up = b;
			
			return( diff );
		}
	}
}
