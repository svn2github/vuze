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

import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.core3.util.TimerEventPeriodic;
import org.gudy.azureus2.plugins.network.Connection;

import com.aelitis.azureus.core.networkmanager.LimitedRateGroup;

public class 
DownloadManagerRateController 
{
	private static Map<PEPeerManager,Integer>		pm_map = new HashMap<PEPeerManager, Integer>();
	
	private static TimerEventPeriodic	timer;
	
	private static AsyncDispatcher	dispatcher = new AsyncDispatcher();
	
	private static int rate_limit	= 0;
	
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
					int	state = 0;
					
					if ( !pm.hasDownloadablePiece()){
						
						pm.addRateLimiter( limiter, true );
						
						state = 1;
					}
					
					pm_map.put( pm, state );
										
					if ( timer == null ){
						
						timer = 
							SimpleTimer.addPeriodicEvent( 
								"DMRC", 
								10*1000,
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
					}
				}
			});
	}
	
	private static void
	update()
	{
		for ( Map.Entry<PEPeerManager, Integer> entry: pm_map.entrySet()){
			
			PEPeerManager	pm = entry.getKey();
			
			List<PEPeer> peers = pm.getPeers();
			
			int total_data_queued = 0;
			
			for ( PEPeer peer: peers ){
				
				Connection connection = peer.getPluginConnection();
				
				if ( connection != null ){
					
					total_data_queued += connection.getOutgoingMessageQueue().getDataQueuedBytes();
				}
			}
			
			int	target_state = pm.hasDownloadablePiece()?0:1;
			
			System.out.println( pm.getDisplayName() + ": dl=" + ( target_state==0 ) + ", q_data=" + total_data_queued );
			
			if ( entry.getValue() != target_state ){
			
				if ( target_state == 0 ){
					
					pm.removeRateLimiter( limiter, true );
					
				}else{
					
					pm.addRateLimiter( limiter, true );
				}
				
				entry.setValue( target_state );
			}
		}
	}
}
