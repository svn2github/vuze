/*
 * File    : TrackerTorrentImpl.java
 * Created : 08-Dec-2003
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

package org.gudy.azureus2.pluginsimpl.tracker;

/**
 * @author parg
 *
 */

import java.util.*;

import org.gudy.azureus2.plugins.tracker.*;
import org.gudy.azureus2.plugins.torrent.*;
import org.gudy.azureus2.pluginsimpl.torrent.*;
import org.gudy.azureus2.core3.tracker.host.*;

public class 
TrackerTorrentImpl
	implements TrackerTorrent, TRHostTorrentListener
{
	protected TRHostTorrent		host_torrent;

	protected List	listeners = new ArrayList();
	
	protected
	TrackerTorrentImpl(
		TRHostTorrent	_host_torrent )
	{
		host_torrent	= _host_torrent;
	}
	
	protected TRHostTorrent
	getHostTorrent()
	{
		return( host_torrent );
	}
	
	public Torrent
	getTorrent()
	{
		return( new TorrentImpl( host_torrent.getTorrent()));
	}
	
	public TrackerPeer[]
	getPeers()
	{
		TRHostPeer[]	peers = host_torrent.getPeers();
		
		TrackerPeer[]	res = new TrackerPeer[peers.length];
		
		for (int i=0;i<peers.length;i++){
			
			res[i] = new TrackerPeerImpl( peers[i]);
		}
		
		return( res );
	}
	
	public int
	getStatus()
	{
		int	status = host_torrent.getStatus();
		
		switch(status){
			case TRHostTorrent.TS_STARTED:
				return( TS_STARTED );
			case TRHostTorrent.TS_STOPPED:
				return( TS_STOPPED );
			case TRHostTorrent.TS_PUBLISHED:
				return( TS_PUBLISHED );
			default:
				throw( new RuntimeException( "TrackerTorrent: status invalid"));
		}
	}
	
	public long
	getTotalUploaded()
	{
		return( host_torrent.getTotalUploaded());
	}
	
	public long
	getTotalDownloaded()
	{
		return( host_torrent.getTotalDownloaded());
	}
	
	public long
	getAverageUploaded()
	{
		return( host_torrent.getAverageUploaded());
	}
	
	public long
	getAverageDownloaded()
	{
		return( host_torrent.getAverageDownloaded());
	}
	
	public long
	getTotalLeft()
	{
		return( host_torrent.getTotalLeft());
	}	
	
	public long
	getCompletedCount()
	{
		return( host_torrent.getCompletedCount());
	}
	
	public Object
	getAdditionalProperty(
		String		name )
	{
		return( host_torrent.getTorrent().getAdditionalProperty(name));
	}
	public synchronized void
	postProcess(
		TRHostTorrentRequest	request )
	{
		for (int i=0;i<listeners.size();i++){
			
			((TrackerTorrentListener)listeners.get(i)).postProcess(new TrackerTorrentRequestImpl(request));
		}
	}
	
	public synchronized void
	addListener(
		TrackerTorrentListener	listener )
	{
		listeners.add( listener );
		
		if ( listeners.size() == 1 ){
			
			host_torrent.addListener( this );
		}
	}
	
	public synchronized void
	removeListener(
		TrackerTorrentListener	listener )
	{
		listeners.remove( listener );
		
		if ( listeners.size() == 0 ){
			
			host_torrent.removeListener(this);
		}
	}
}