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
	protected TRHostImpl		host;
	protected TRTrackerServer	server;
	protected TOTorrent			torrent;
	protected int				port;
	
	protected List				listeners			= new ArrayList();
	protected List				removal_listeners	= new ArrayList();
	
	protected int				status	= TS_STOPPED;
	protected boolean			persistent;
	
	protected long				total_uploaded;
	protected long				total_downloaded;
	protected long				total_left;
	protected long				total_bytes_in;
	protected long				total_bytes_out;
	
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
					
			server.permit( torrent.getHash(), true);
		
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
	
	public int
	getAnnounceCount()
	{
		try{
		
			TRTrackerServerTorrentStats	stats = server.getStats( torrent.getHash());
		
			if ( stats != null ){
			
				return( stats.getAnnounceCount());
			}
		}catch( TOTorrentException e ){
			
			e.printStackTrace();
		}
		
		return( 0 );
	}
	
	protected void
	setAnnounceCount(
		int		count )
	{
		try{
			
			TRTrackerServerTorrentStats	stats = server.getStats( torrent.getHash());
			
			if ( stats != null ){
				
				stats.setAnnounceCount(count);
			}
		}catch( TOTorrentException e ){
			
			e.printStackTrace();
		}
	}
	
	public int
	getScrapeCount()
	{
		try{
			
			TRTrackerServerTorrentStats	stats = server.getStats( torrent.getHash());
			
			if ( stats != null ){
				
				return( stats.getScrapeCount());
			}
		}catch( TOTorrentException e ){
			
			e.printStackTrace();
		}
		
		return( 0 );
	}
	
	protected void
	setScrapeCount(
		int		count )
	{
		try{
			
			TRTrackerServerTorrentStats	stats = server.getStats( torrent.getHash());
			
			if ( stats != null ){
				
				stats.setScrapeCount(count);
			}
		}catch( TOTorrentException e ){
			
			e.printStackTrace();
		}
	}
	
	public int
	getCompletedCount()
	{
		try{
		
			TRTrackerServerTorrentStats	stats = server.getStats( torrent.getHash());
		
			if ( stats != null ){
			
				return( stats.getCompletedCount());
			}
		}catch( TOTorrentException e ){
			
			e.printStackTrace();
		}
		
		return( 0 );	
	}

	protected void
	setCompletedCount(
		int		count )
	{
		try{
			
			TRTrackerServerTorrentStats	stats = server.getStats( torrent.getHash());
			
			if ( stats != null ){
				
				stats.setCompletedCount(count);
			}
		}catch( TOTorrentException e ){
			
			e.printStackTrace();
		}
	}
	protected void
	updateStats()
	{
		try{
		
			// System.out.println( "stats update ");
			
			TRTrackerServerTorrentStats	stats = server.getStats( torrent.getHash());
		
			if ( stats != null ){
			
				long	current_uploaded 	= stats.getUploaded();
				
					// bit crap this as the maintained values are only for *active*
					// peers - stats are lost when they disconnect
					
				long ul_diff = current_uploaded - last_uploaded;
					
				if ( ul_diff > 0 ){
			
					total_uploaded += ul_diff;
				
				}else{
				
					ul_diff = 0;
				}
				
				average_uploaded.addValue((int)ul_diff);
				
				last_uploaded = current_uploaded;
				
					// downloaded 
				
				long	current_downloaded 	= stats.getDownloaded();
				
				long dl_diff = current_downloaded - last_downloaded;
					
				if ( dl_diff > 0 ){
				
					total_downloaded += dl_diff;
					
				}else{
					
					dl_diff = 0;
				}
				
				average_downloaded.addValue((int)dl_diff);
				
				last_downloaded = current_downloaded;
				
				// bytes in 
				
				long	current_bytes_in 	= stats.getBytesIn();
				
				long bi_diff = current_bytes_in - last_bytes_in;
				
				if ( bi_diff > 0 ){
					
					total_bytes_in += bi_diff;
					
				}else{
					
					bi_diff = 0;
				}
				
				average_bytes_in.addValue((int)bi_diff);
				
				last_bytes_in = current_bytes_in;

				// bytes out 
				
				long	current_bytes_out 	= stats.getBytesOut();
				
				long bo_diff = current_bytes_out - last_bytes_out;
				
				if ( bo_diff > 0 ){
					
					total_bytes_out += bo_diff;
					
				}else{
					
					bo_diff = 0;
				}
				
				average_bytes_out.addValue((int)bo_diff);
				
				last_bytes_out = current_bytes_out;
				
				
					// left
				
				total_left = stats.getAmountLeft();
				
				// System.out.println( "tot_up = " + total_uploaded + ", tot_down = " + total_downloaded);
			}
		}catch( TOTorrentException e ){
			
			e.printStackTrace();
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
		return( total_uploaded );
	}
	
	protected void
	setTotalUploaded(
		long	l )
	{
		total_uploaded	= l;
	}
	
	public long
	getTotalDownloaded()
	{
		return( total_downloaded );
	}	
	
	protected void
	setTotalDownloaded(
		long	l )
	{
		total_downloaded	= l;
	}
	
	public long
	getTotalLeft()
	{
		return( total_left );
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
	
	public long
	getTotalBytesIn()
	{
		return( total_bytes_in );
	}
	
	protected void
	setTotalBytesIn(
		long	l )
	{
		total_bytes_in	= l;
	}
	
	
	public long
	getTotalBytesOut()
	{
		return( total_bytes_out );
	}
	
	protected void
	setTotalBytesOut(
		long	l )
	{
		total_bytes_out	= l;
	}
	
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
