/*
 * File    : TRTrackerServerStatsImpl.java
 * Created : 31-Oct-2003
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
 
package org.gudy.azureus2.core3.tracker.server.impl;

/**
 * @author parg
 */

import org.gudy.azureus2.core3.tracker.server.*;

public class 
TRTrackerServerTorrentStatsImpl
	implements TRTrackerServerTorrentStats 
{
	protected TRTrackerServerTorrentImpl	torrent;
	protected long							announce_count;
	protected long							scrape_count;
	protected long							completed_count;
	
	protected long							uploaded;
	protected long							downloaded;
	protected long							left;
	
	protected long							bytes_in;
	protected long							bytes_out;
	
	protected
	TRTrackerServerTorrentStatsImpl(
		TRTrackerServerTorrentImpl 	_torrent )
	{
		torrent	= _torrent;
	}
		
	protected void
	addAnnounce(
		long		ul_diff,
		long		dl_diff,
		long		le_diff )
	{
		announce_count++;
		
		uploaded	+= ul_diff<0?0:ul_diff;	// should always be +ve
		downloaded	+= dl_diff<0?0:dl_diff;
		left		+= le_diff;	// can be +ve at start (0->x) then neg after
		
		if ( left < 0 ){
			
			left	= 0;
		}
	}
	
	protected void
	removeLeft(
		long	_left )
	{
		left	-= _left;
		
		if ( left < 0 ){
			
			left	= 0;
		}
	}
	
	public long
	getAnnounceCount()
	{
		return( announce_count );
	}
	
	public void
	setAnnounceCount(
		long		count )
	{
		announce_count	= count;
	}
	
	protected void
	addScrape()
	{
		scrape_count++;
	}
	
	public long
	getScrapeCount()
	{
		return( scrape_count );
	}
	
	public void
	setScrapeCount(
		long		count )
	{
		scrape_count	= count;
	}
	protected void
	addCompleted()
	{
		completed_count++;
	}

	public long
	getCompletedCount()
	{
		return( completed_count );
	}

	public void
	setCompletedCount(
		long		count )
	{
		completed_count	= count;
	}
	
	public long
	getUploaded()
	{
		return( uploaded );
	}
	
	public long
	getDownloaded()
	{
		return( downloaded );
	}
	
	public long
	getAmountLeft()
	{
		return( left );
	}
		
	protected void
	addXferStats(
		int		in,
		int		out )
	{
		bytes_in	+= in;
		bytes_out	+= out;
	}
	
	public long
	getBytesIn()
	{
		return( bytes_in );
	}
	
	public long
	getBytesOut()
	{
		return( bytes_out );
	}
	
	public int
	getSeedCount()
	{
		return( torrent.getSeedCount());
	}
	
	public int
	getLeecherCount()
	{
		return( torrent.getLeecherCount());
	}
}
