/*
 * File    : TRHostTorrentPublishImpl.java
 * Created : 12-Nov-2003
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

package org.gudy.azureus2.core3.tracker.host.impl;

/**
 * @author parg
 *
 */

import java.util.*;

import org.gudy.azureus2.core3.tracker.host.*;
import org.gudy.azureus2.core3.tracker.client.*;
import org.gudy.azureus2.core3.torrent.*;

public class 
TRHostTorrentPublishImpl
	implements TRHostTorrent 
{
	protected TRHostImpl		host;
	protected TOTorrent			torrent;

	protected int				status	= TS_PUBLISHED;
	protected boolean			persistent;
	
	protected TRHostPeer[]		peers = new TRHostPeer[0];
	
	protected List				listeners 			= new ArrayList();
	protected List				removal_listeners	= new ArrayList();
	
  private HashMap data;

	protected
	TRHostTorrentPublishImpl(
		TRHostImpl		_host,
		TOTorrent		_torrent )
	{
		host		= _host;
		torrent		= _torrent;
	}

	public synchronized void
	start()
	{
	}

	public synchronized void
	stop()
	{
	}

	public synchronized void
	remove()
	
		throws TRHostTorrentRemovalVetoException
	{
		canBeRemoved();
		
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

	public int
	getPort()
	{
		return( -1 );
	}
	
	public synchronized TRHostPeer[]
	getPeers()
	{
		return( peers );
	}	

	public long
	getAnnounceCount()
	{
		return( 0 );
	}
	
	public long
	getScrapeCount()
	{
		return( 0 );
	}
	public long
	getCompletedCount()
	{
		return( 0 );
	}
	
	protected void
	updateStats()
	{		
		TRTrackerScraperResponse resp = null;
		
		TRTrackerClient tc = host.getTrackerClient( this );
		
		if ( tc != null ){
			
			resp = TRTrackerScraperFactory.getSingleton().scrape( tc );
		}
		
		if ( resp == null ){
			
			resp = TRTrackerScraperFactory.getSingleton().scrape( torrent );
		}
				
		synchronized( this ){
		
			if ( resp != null && resp.isValid()){
						
				int peer_count 	= resp.getPeers();
				int seed_count	= resp.getSeeds();
				
				peers = new TRHostPeer[ peer_count + seed_count ];
				
				for (int i=0;i<peers.length;i++){
					
					peers[i] = new TRHostPeerPublishImpl( i<seed_count );
				}
			}else{
				
				peers = new TRHostPeer[0];
			}
		}
	}

	public long
	getTotalUploaded()
	{
		return( 0 );
	}

	public long
	getTotalDownloaded()
	{
		return( 0 );
	}	

	public long
	getTotalLeft()
	{
		return( 0 );
	}

	public long
	getAverageUploaded()
	{
		return( 0 );
	}

	public long
	getAverageDownloaded()
	{
		return( 0 );
	}
	
	public long
	getTotalBytesIn()
	{
		return( 0 );
	}
	
	public long
	getTotalBytesOut()
	{
		return( 0 );
	}
	
	public long
	getAverageBytesIn()
	{
		return( 0 );
	}
	
	public long
	getAverageBytesOut()
	{
		return( 0 );
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
