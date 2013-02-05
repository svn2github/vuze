/*
 * File    : Factory.java
 * Created : 2 mars 2004
 * By      : Olivier
 *
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.gudy.azureus2.core3.stats.transfer;

import org.gudy.azureus2.core3.global.GlobalManagerStats;
import org.gudy.azureus2.core3.stats.transfer.impl.LongTermStatsImpl;
import org.gudy.azureus2.core3.stats.transfer.impl.OverallStatsImpl;

import com.aelitis.azureus.core.AzureusCore;

/**
 * @author Olivier
 * 
 */
public class 
StatsFactory 
{
	private static OverallStats 	overall_stats;
	private static LongTermStats	longterm_stats;
	
	public static OverallStats 
	getStats() 
	{	
		return( overall_stats );
	}
	
	public static LongTermStats 
	getLongTermStats() 
	{	
		return( longterm_stats );
	}
  
	public static void 
	initialize(
		AzureusCore 		core,
		GlobalManagerStats	stats )
	{
		overall_stats 	= new OverallStatsImpl( core, stats );
		longterm_stats	= new LongTermStatsImpl( core, stats );
	}
}
