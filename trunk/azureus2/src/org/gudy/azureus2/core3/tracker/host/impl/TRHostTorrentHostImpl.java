/*
 * File    : TRHostTorrentImpl.java
 * Created : 26-Oct-2003
 * By      : stuff
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

package org.gudy.azureus2.core3.tracker.host.impl;

/**
 * @author parg
 *
 */

import java.util.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.tracker.host.*;
import org.gudy.azureus2.core3.tracker.server.*;
import org.gudy.azureus2.core3.torrent.*;

public class 
TRHostTorrentHostImpl
	implements TRHostTorrent 
{
	protected TRHostImpl					host;
	protected TRTrackerServer				server;
	protected TRTrackerServerTorrentStats	torrent_stats;
	protected TOTorrent						torrent;
	protected int							port;
	
	protected List				listeners			= new ArrayList();
	protected List				removal_listeners	= new ArrayList();
	
	protected int				status	= TS_STOPPED;
	protected boolean			persistent;
	
	protected long				sos_uploaded;
	protected long				sos_downloaded;
	protected long				sos_bytes_in;
	protected long				sos_bytes_out;
	
	protected long				last_uploaded;
	protected long				last_downloaded;
	protected long				last_bytes_in;
	protected long				last_bytes_out;
	
		//average over 10 periods, update every period.

	protected Average			average_uploaded		= Average.getInstance(TRHostImpl.STATS_PERIOD_SECS*1000,TRHostImpl.STATS_PERIOD_SECS*10);
	protected Average			average_downloaded		= Average.getInstance(TRHostImpl.STATS_PERIOD_SECS*1000,TRHostImpl.STATS_PERIOD_SECS*10);
	protected Average			average_bytes_in		= Average.getInstance(TRHostImpl.STATS_PERIOD_SECS*1000,TRHostImpl.STATS_PERIOD_SECS*10);
	protected Average			average_bytes_out		= Average.getInstance(TRHostImpl.STATS_PERIOD_SECS*1000,TRHostImpl.STATS_PERIOD_SECS*10);
	
	private HashMap data;

	protected
	TRHostTorrentHostImpl(
		TRHostImpl		_host,
		TRTrackerServer	_server,
		TOTorrent		_torrent,
		int				_port )
	{
		host		= _host;
		server		= _server;
		torrent		= _torrent;
		port		= _port;
	}
	
	public int
	getPort()
	{
		return( port );
	}
	
	public synchronized void
	start()
	{
		try{
			// System.out.println( "TRHostTorrentHostImpl::start");
			
			status = TS_STARTED;
					
			torrent_stats = server.permit( torrent.getHash(), true).getStats();
		
			host.hostTorrentStateChange( this );
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	public synchronized void
	stop()
	{
		try{
			// System.out.println( "TRHostTorrentHostImpl::stop");
			
			status = TS_STOPPED;
				
			server.deny( torrent.getHash(), true);
		
			if ( torrent_stats != null ){
				
				sos_uploaded	= sos_uploaded 		+ torrent_stats.getUploaded();
				sos_downloaded	= sos_downloaded 	+ torrent_stats.getDownloaded();
				sos_bytes_in	= sos_bytes_in 		+ torrent_stats.getBytesIn();
				sos_bytes_out	= sos_bytes_out 	+ torrent_stats.getBytesOut();
				
				torrent_stats	= null;
			}
			
			last_uploaded		= 0;
			last_downloaded		= 0;
			last_bytes_in		= 0;
			last_bytes_out		= 0;

			host.hostTorrentStateChange( this );
			
		}catch( Throwable e ){
			
				e.printStackTrace();
		}
	}
	
	public synchronized void
	remove()
	
		throws TRHostTorrentRemovalVetoException
	{
		canBeRemoved();
		
		stop();
		
		host.remove( this );
	}
	
	public boolean
	canBeRemoved()
	
		throws TRHostTorrentRemovalVetoException
	{
		for (int i=0;i<removal_listeners.size();i++){
			
			((TRHostTorrentWillBeRemovedListener)removal_listeners.get(i)).torrentWillBeRemoved( this );
		}
		
		return( true );
	}
	
	public int
	getStatus()
	{
		return( status );
	}
	
	public boolean
	isPersistent()
	{
		return( persistent );
	}
	
	public void
	setPersistent(
		boolean		_persistent )
	{
		persistent	= _persistent;
	}
	
	public TOTorrent
	getTorrent()
	{
		return( torrent );
	}

	protected void
	setTorrent(
		TOTorrent		_torrent )
	{
		torrent = _torrent;
	}
	
	public TRHostPeer[]
	getPeers()
	{
		try{
		
			TRTrackerServerPeer[]	peers = server.getPeers( torrent.getHash());
		
			if ( peers != null ){
			
				TRHostPeer[]	res = new TRHostPeer[peers.length];
				
				for (int i=0;i<peers.length;i++){
					
					res[i] = new TRHostPeerHostImpl(peers[i]);
				}
				
				return( res );
			}
		}catch( TOTorrentException e ){
			
			e.printStackTrace();
		}
		
		return( new TRHostPeer[0] );
	}	
	
	protected TRTrackerServerTorrentStats
	getStats()
	{
		return( torrent_stats );
	}
	
	protected void
	setStartOfDayValues(
		long		completed,
		long		announces,
		long		scrapes,
		long		uploaded,
		long		downloaded,
		long		bytes_in,
		long		bytes_out )
	{
		TRTrackerServerTorrentStats	stats = getStats();
		
		if ( stats != null ){
			
			stats.setCompletedCount(completed);
			stats.setAnnounceCount(announces);
			stats.setScrapeCount(scrapes);
		}
		
		sos_uploaded		= uploaded;
		sos_downloaded		= downloaded;
		sos_bytes_in		= bytes_in;
		sos_bytes_out		= bytes_out;
	}
	
	public long
	getAnnounceCount()
	{
		TRTrackerServerTorrentStats	stats = getStats();
	
		if ( stats != null ){
		
			return( stats.getAnnounceCount());
		}
		
		return( 0 );
	}
	
	public long
	getScrapeCount()
	{
		TRTrackerServerTorrentStats	stats = getStats();
		
		if ( stats != null ){
			
			return( stats.getScrapeCount());
		}
		
		return( 0 );
	}
	
	public long
	getCompletedCount()
	{
		TRTrackerServerTorrentStats	stats = getStats();
		
		if ( stats != null ){
			
			return( stats.getCompletedCount());
		}
		
		return( 0 );
	}

	protected void
	updateStats()
	{
		TRTrackerServerTorrentStats	stats = getStats();
	
		if ( stats != null ){
		
			long	current_uploaded 	= stats.getUploaded();
								
			long ul_diff = current_uploaded - last_uploaded;
				
			if ( ul_diff < 0 ){
	
				ul_diff = 0;
			}
			
			average_uploaded.addValue((int)ul_diff);
			
			last_uploaded = current_uploaded;
			
				// downloaded 
			
			long	current_downloaded 	= stats.getDownloaded();
			
			long dl_diff = current_downloaded - last_downloaded;
				
			if ( dl_diff < 0 ){
				
				dl_diff = 0;
			}
			
			average_downloaded.addValue((int)dl_diff);
			
			last_downloaded = current_downloaded;
			
			// bytes in 
			
			long	current_bytes_in 	= stats.getBytesIn();
			
			long bi_diff = current_bytes_in - last_bytes_in;
			
			if ( bi_diff < 0 ){
								
				bi_diff = 0;
			}
			
			average_bytes_in.addValue((int)bi_diff);
			
			last_bytes_in = current_bytes_in;

			// bytes out 
			
			long	current_bytes_out 	= stats.getBytesOut();
			
			long bo_diff = current_bytes_out - last_bytes_out;
			
			if ( bo_diff < 0 ){
								
				bo_diff = 0;
			}
			
			average_bytes_out.addValue((int)bo_diff);
			
			last_bytes_out = current_bytes_out;
						
			// System.out.println( "tot_up = " + total_uploaded + ", tot_down = " + total_downloaded);
		}
	}
	
	protected TRTrackerServer
	getServer()
	{
		return( server );
	}
	
	public long
	getTotalUploaded()
	{
		TRTrackerServerTorrentStats	stats = getStats();
		
		if ( stats != null ){
			
			return( sos_uploaded + stats.getUploaded());
		}
		
		return( sos_uploaded );
	}
	
	public long
	getTotalDownloaded()
	{
		TRTrackerServerTorrentStats	stats = getStats();
		
		if ( stats != null ){
			
			return( sos_downloaded + stats.getDownloaded());
		}
		
		return( sos_downloaded );	
	}	
	
	public long
	getTotalLeft()
	{
		TRTrackerServerTorrentStats	stats = getStats();
		
		if ( stats != null ){
			
			return( stats.getAmountLeft());
		}
		
		return( 0 );	
	}
	
	public long
	getTotalBytesIn()
	{
		TRTrackerServerTorrentStats	stats = getStats();
		
		if ( stats != null ){
			
			return( sos_bytes_in + stats.getBytesIn());
		}
		
		return( sos_bytes_in );	
	}	
	
	public long
	getTotalBytesOut()
	{
		TRTrackerServerTorrentStats	stats = getStats();
		
		if ( stats != null ){
			
			return( sos_bytes_out + stats.getBytesOut());
		}
		
		return( sos_bytes_out );	
	}
	
		// averages
	
	public long
	getAverageBytesIn()
	{
		return( average_bytes_in.getAverage());
	}
	
	public long
	getAverageBytesOut()
	{
		return( average_bytes_out.getAverage() );
	}
	
	public long
	getAverageUploaded()
	{
		return( average_uploaded.getAverage() );
	}
	
	public long
	getAverageDownloaded()
	{
		return( average_downloaded.getAverage() );
	}
	
	protected synchronized void
	postProcess(
		TRHostTorrentRequest	req )
	{
		for (int i=0;i<listeners.size();i++){
			
			((TRHostTorrentListener)listeners.get(i)).postProcess(req);
		}
	}
	
	public synchronized void
	addListener(
		TRHostTorrentListener	l )
	{
		listeners.add(l);
		
		host.torrentListenerRegistered();
	}
	
	public synchronized void
	removeListener(
		TRHostTorrentListener	l )
	{
		listeners.remove(l);
	}
	
	public synchronized void
	addRemovalListener(
		TRHostTorrentWillBeRemovedListener	l )
	{
		removal_listeners.add(l);
	}
	
	public synchronized void
	removeRemovalListener(
		TRHostTorrentWillBeRemovedListener	l )
	{
		removal_listeners.remove(l);
	}

  /** To retreive arbitrary objects against this object. */
  public Object getData (String key) {
  	if (data == null) return null;
    return data.get(key);
  }

  /** To store arbitrary objects against this object. */
  public synchronized void setData (String key, Object value) {
  	if (data == null) {
  	  data = new HashMap();
  	}
    if (value == null) {
      if (data.containsKey(key))
        data.remove(key);
    } else {
      data.put(key, value);
    }
  }
}
