/*
 * File    : TRTrackerServerImpl.java
 * Created : 5 Oct. 2003
 * By      : Parg 
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


import java.net.*;
import java.util.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.tracker.server.*;

public class 
TRTrackerServerImpl 
	implements TRTrackerServer, Runnable
{
	protected int	port;
	protected int	retry_interval;
	
	protected Map	torrent_map = new HashMap(); 
	
	public
	TRTrackerServerImpl(
		int		_port,
		int		_retry_interval )
	{
		port			= _port;
		retry_interval	= _retry_interval;
		
		Thread t = new Thread(this);
		
		t.setDaemon( true );
		
		t.start();
	}
	
	public void
	run()
	{
		System.out.println( "TRTrackerServerImpl: starts on port " + port );
		
		try{
			ServerSocket ss = new ServerSocket( port );
			
			while(true){
				
				Socket socket = ss.accept();
				
				new TRTrackerServerProcessor( this, socket );
			}
		}catch( Throwable e ){
			
			e.printStackTrace();
			
			//throw( new TRTrackerServerException( "TRTrackerServer: accept fails: " + e.toString()  ));
		}	
	}
	
	public int
	getRetryInterval()
	{
		return( retry_interval );
	}
	
	public synchronized void
	permit(
		byte[]		_hash )
	{
		HashWrapper	hash = new HashWrapper( _hash );
		
		TRTrackerServerTorrent	entry = (TRTrackerServerTorrent)torrent_map.get( hash );
		
		if ( entry == null ){
			
			entry = new TRTrackerServerTorrent( this, hash );
			
			torrent_map.put( hash, entry );
		}
	}
		
	public void
	deny(
		byte[]		_hash )
	{
		HashWrapper	hash = new HashWrapper( _hash );
		
		torrent_map.remove( hash );
	}
	
	protected TRTrackerServerTorrent
	getTorrent(
		byte[]		hash )
	{
		return((TRTrackerServerTorrent)torrent_map.get(new HashWrapper(hash)));
	}
}
