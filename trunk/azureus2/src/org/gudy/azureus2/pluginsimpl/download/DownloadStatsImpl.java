/*
 * File    : DownloadStatsImpl.java
 * Created : 08-Jan-2004
 * By      : parg
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

package org.gudy.azureus2.pluginsimpl.download;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.core3.download.*;
import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.util.*;

import org.gudy.azureus2.plugins.download.DownloadStats;

public class 
DownloadStatsImpl 
	implements DownloadStats
{
	protected DownloadManager		dm;
	protected DownloadManagerStats	dm_stats;
	
	protected
	DownloadStatsImpl(
		DownloadManager	_dm )
	{
		dm 			= _dm;
		dm_stats	= dm.getStats();
	}
	
	public String
	getStatus()
	{
		return( DisplayFormatters.formatDownloadStatusDefaultLocale( dm));
	}
	
	public String
	getDownloadDirectory()
	{
		return( dm.getSavePath());
	}
	
	public String
	getTargetFileOrDir()
	{		
		DiskManager	disk_manager = dm.getDiskManager();
		
		if ( disk_manager != null ){
							
			return( disk_manager.getFileName());
				
		}
		
		return( null );
	}
	
	public String
	getTrackerStatus()
	{
		return(dm.getTrackerStatus());
	}
	
	public int
	getCompleted()
	{
		return( dm_stats.getCompleted());
	}
	
	public long
	getDownloaded()
	{
		return( dm_stats.getDownloaded());
	}
	
	public long
	getUploaded()
	{
		return( dm_stats.getUploaded());
	}
	public long
	getDiscarded()
	{
		return( dm_stats.getDiscarded());
	}
	
	public long
	getDownloadAverage()
	{
		return( dm_stats.getDownloadAverage());
	}
	
	public long
	getUploadAverage()
	{
		return( dm_stats.getUploadAverage());
	}
	
	public long
	getTotalAverage()
	{
		return( dm_stats.getTotalAverage());
	}
	
	public String
	getElapsedTime()
	{
		return( dm_stats.getElapsedTime());
	}
	
	public String
	getETA()
	{
		return(DisplayFormatters.formatETA(dm_stats.getETA()));
	}
	
	public long
	getHashFails()
	{
		return( dm_stats.getHashFails());
	}
	
	public int
	getShareRatio()
	{
		return( dm_stats.getShareRatio());
	}
}
