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

import org.gudy.azureus2.core3.tracker.host.*;
import org.gudy.azureus2.core3.tracker.server.*;
import org.gudy.azureus2.core3.torrent.*;

public class 
TRHostTorrentImpl
	implements TRHostTorrent 
{
	protected TRHostImpl		host;
	protected TRTrackerServer	server;
	protected TOTorrent			torrent;
	
	protected int				status	= TS_STOPPED;
	
	protected
	TRHostTorrentImpl(
		TRHostImpl		_host,
		TRTrackerServer	_server,
		TOTorrent		_torrent )
	{
		host		= _host;
		server		= _server;
		torrent		= _torrent;
	}
	
	public synchronized void
	start()
	{
		try{
		
			server.permit( torrent.getHash());
		
			status = TS_STARTED;
			
		}catch( TOTorrentException e ){
			
			e.printStackTrace();
		}
	}
	
	public synchronized void
	stop()
	{
		try{
			
			server.deny( torrent.getHash());
		
			status = TS_STOPPED;
			
		}catch( TOTorrentException e ){
			
				e.printStackTrace();
		}
	}
	
	public synchronized void
	remove()
	{
		stop();
		
		host.remove( this );
	}
	
	public int
	getStatus()
	{
		return( status );
	}
	
	public TOTorrent
	getTorrent()
	{
		return( torrent );
	}

	public TRHostPeer[]
	getPeers()
	{
		try{
		
			TRTrackerServerPeer[]	peers = server.getPeers( torrent.getHash());
		
			if ( peers != null ){
			
				TRHostPeer[]	res = new TRHostPeer[peers.length];
				
				for (int i=0;i<peers.length;i++){
					
					res[i] = new TRHostPeerImpl(peers[i]);
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
		
			TRTrackerServerStats	stats = server.getStats( torrent.getHash());
		
			if ( stats != null ){
			
				return( stats.getAnnounceCount());
			}
		}catch( TOTorrentException e ){
			
			e.printStackTrace();
		}
		
		return( 0 );
	}
}
