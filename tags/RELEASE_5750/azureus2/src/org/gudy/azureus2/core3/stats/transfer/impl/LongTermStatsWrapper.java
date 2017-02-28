/*
 * Created on Sep 18, 2015
 * Created by Paul Gardner
 * 
 * Copyright 2015 Azureus Software, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or 
 * (at your option) any later version.
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


package org.gudy.azureus2.core3.stats.transfer.impl;

import java.util.*;

import org.gudy.azureus2.core3.global.GlobalManagerStats;
import org.gudy.azureus2.core3.stats.transfer.LongTermStats;
import org.gudy.azureus2.core3.stats.transfer.LongTermStatsListener;

import com.aelitis.azureus.core.AzureusCore;

public class 
LongTermStatsWrapper 
	implements LongTermStats
{
	private AzureusCore			core;
	private GlobalManagerStats	gm_stats;
	
	private String									id;
	private LongTermStats.GenericStatsSource		source;
	
	private LongTermStatsWrapperHelper	delegate;
	
	private final Map<LongTermStatsListener,Long>	listeners = new IdentityHashMap<LongTermStatsListener,Long>();
	
	public
	LongTermStatsWrapper(
		AzureusCore 		_core,
		GlobalManagerStats	_stats )
	{
		core		= _core;
		gm_stats	= _stats;
		
		delegate = new LongTermStatsImpl( core, gm_stats );
	}
	
	public
	LongTermStatsWrapper(
		String									_id,
		LongTermStats.GenericStatsSource		_source )
	{
		id		= _id;
		source	= _source;
		
		delegate = new LongTermStatsGenericImpl( id, source );
	}
	
	public synchronized boolean
	isEnabled()
	{
		return( delegate.isEnabled());
	}
	
	public synchronized long[]
	getCurrentRateBytesPerSecond()
	{
		return( delegate.getCurrentRateBytesPerSecond());
	}
	
	public synchronized long[]
	getTotalUsageInPeriod(
		Date		start_date,
		Date		end_date )
	{
		return( delegate.getTotalUsageInPeriod(start_date, end_date));
	}
	
	public synchronized long[]
	getTotalUsageInPeriod(
		int		period_type,
		double	multiplier )
	{
		return( delegate.getTotalUsageInPeriod(period_type,multiplier));
	}
	
	public synchronized long[]
	getTotalUsageInPeriod(
		int					period_type,
		double				multiplier,
		RecordAccepter		accepter )
	{
		return( delegate.getTotalUsageInPeriod(period_type, multiplier, accepter));
	}
	
	public synchronized void
	addListener(
		long						min_delta_bytes,
		LongTermStatsListener		listener )
	{
		listeners.put( listener, min_delta_bytes );
		
		delegate.addListener(min_delta_bytes, listener);
	}
	
	public synchronized void
	removeListener(
		LongTermStatsListener		listener )
	{
		listeners.remove( listener );
		
		delegate.removeListener(listener);
	}
	
	public synchronized void
	reset()
	{
		delegate.destroyAndDeleteData();
		
		if ( core != null ){
			
			delegate = new LongTermStatsImpl( core, gm_stats );

		}else{
			
			delegate = new LongTermStatsGenericImpl( id, source );
		}
		
		for ( Map.Entry<LongTermStatsListener,Long> entry: listeners.entrySet()){
			
			delegate.addListener(entry.getValue(), entry.getKey());
		}
	}
	
	public interface
	LongTermStatsWrapperHelper
		extends LongTermStats
	{
		public void
		destroyAndDeleteData();
	}
}
