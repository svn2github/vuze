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
	protected int							announce_count;
	protected int							scrape_count;
	protected int							completed_count;
	
	protected long							bytes_in;
	protected long							bytes_out;
	
	protected
	TRTrackerServerTorrentStatsImpl(
		TRTrackerServerTorrentImpl 	_torrent )
	{
		torrent	= _torrent;
	}
		
	protected void
	addAnnounce()
	{
		announce_count++;
	}
	
	public int
	getAnnounceCount()
	{
		return( announce_count );
	}
	
	public void
	setAnnounceCount(
		int		count )
	{
		announce_count	= count;
	}
	
	protected void
	addScrape()
	{
		scrape_count++;
	}
	
	public int
	getScrapeCount()
	{
		return( scrape_count );
	}
	
	public void
	setScrapeCount(
		int		count )
	{
		scrape_count	= count;
	}
	protected void
	addCompleted()
	{
		completed_count++;
	}

	public int
	getCompletedCount()
	{
		return( completed_count );
	}

	public void
	setCompletedCount(
		int		count )
	{
		completed_count	= count;
	}
	
	public long
	getUploaded()
	{
		TRTrackerServerPeer[]	peers = torrent.getPeers();
		
		long	res = 0;
		
		for(int i=0;i<peers.length;i++){
			
			res += peers[i].getUploaded();
		}
		
		return( res );
	}
	
	public long
	getDownloaded()
	{
		TRTrackerServerPeer[]	peers = torrent.getPeers();
		
		long	res = 0;
		
		for(int i=0;i<peers.length;i++){
			
			res += peers[i].getDownloaded();
		}
		
		return( res );
	}
	
	public long
	getAmountLeft()
	{
		TRTrackerServerPeer[]	peers = torrent.getPeers();
		
		long	res = 0;
		
		for(int i=0;i<peers.length;i++){
			
			res += peers[i].getAmountLeft();
		}
		
		return( res );
	}
	
	public int
	getNumberOfPeers()
	{
		TRTrackerServerPeer[]	peers = torrent.getPeers();
		
		int	res = 0;
		
		for(int i=0;i<peers.length;i++){
			
			res += peers[i].getNumberOfPeers();
		}
		
		return( res );
	}
	
	public int
	getNumberOfSeeds()
	{
		TRTrackerServerPeer[]	peers = torrent.getPeers();
		
		int	res = 0;
		
		for(int i=0;i<peers.length;i++){
			
			if (peers[i].getAmountLeft() == 0 ){

				res++;
			}
		}
		
		return( res );
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
}
