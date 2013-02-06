/*
 * Created on Feb 1, 2013
 * Created by Paul Gardner
 * 
 * Copyright 2013 Azureus Software, Inc.  All rights reserved.
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


package org.gudy.azureus2.core3.stats.transfer;

import java.util.Date;

public interface 
LongTermStats 
{
	public static final int ST_PROTOCOL_UPLOAD		= 0;
	public static final int ST_DATA_UPLOAD			= 1;
	public static final int ST_PROTOCOL_DOWNLOAD	= 2;
	public static final int ST_DATA_DOWNLOAD		= 3;
	public static final int ST_DHT_UPLOAD			= 4;
	public static final int ST_DHT_DOWNLOAD			= 5;
	
	public static final int PT_CURRENT_DAY			= 1;
	public static final int PT_CURRENT_WEEK			= 2;	// sun is start of week
	public static final int PT_CURRENT_MONTH		= 3;
	
	public boolean
	isEnabled();
	
	public long[]
	getCurrentRateBytesPerSecond();
	
	public long[]
	getTotalUsageInPeriod(
		Date		start_date,
		Date		end_date );
	
	public long[]
	getTotalUsageInPeriod(
		int	period_type );
	
	public void
	addListener(
		long						min_delta_bytes,
		LongTermStatsListener		listener );
	
	public void
	removeListener(
		LongTermStatsListener		listener );
}
