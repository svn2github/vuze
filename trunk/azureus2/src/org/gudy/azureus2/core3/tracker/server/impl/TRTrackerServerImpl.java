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
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.tracker.server.*;

public class 
TRTrackerServerImpl 
	implements TRTrackerServer
{
	protected int	port;
	protected int	retry_interval_seconds;
	
	protected Map	torrent_map = new HashMap(); 
	
	public
	TRTrackerServerImpl(
		int		_port,
		int		_retry_interval )
	{
		port					= _port;
		retry_interval_seconds	= _retry_interval;
		
		Thread accept_thread = 
				new Thread()
				{
					public void
					run()
					{
						acceptLoop();
					}
				};
		
		accept_thread.setDaemon( true );
		
		accept_thread.start();
		
		Thread timer_thread = 
			new Thread()
			{
				public void
				run()
				{
					timerLoop();
				}
			};
			
		timer_thread.setDaemon( true );
			
		timer_thread.start();
	}
	
	protected void
	acceptLoop()
	{
		//System.out.println( "TRTrackerServerImpl: starts on port " + port );
		
		String bind_ip = COConfigurationManager.getStringParameter("Bind IP", "");

		try{
			ServerSocket ss;
			
			if ( bind_ip.length() < 7 ){
				
				ss = new ServerSocket( port );
				
			}else{
				
				ss = new ServerSocket( port, 128, InetAddress.getByName(bind_ip));
			}
			
			while(true){
				
				Socket socket = ss.accept();
				
				new TRTrackerServerProcessor( this, socket );
			}
		}catch( Throwable e ){
			
			e.printStackTrace();
			
			//throw( new TRTrackerServerException( "TRTrackerServer: accept fails: " + e.toString()  ));
		}	
	}
	
	protected void
	timerLoop()
	{
		while(true){
	
			try{
				Thread.sleep( getTimeoutIntervalInMillis() );		
				
				synchronized(this){
					
					Iterator	it = torrent_map.values().iterator();
					
					while(it.hasNext()){
						
						Map	temp = new HashMap();
						
							// this triggers timeouts...
							
						((TRTrackerServerTorrent)it.next()).exportPeersToMap( temp );
					}
				}
				
			}catch( InterruptedException e ){
				
				e.printStackTrace();
			}
			
		}
	}
	
	public int
	getRetryInterval()
	{
		return( retry_interval_seconds );
	}
	
	protected long
	getTimeoutIntervalInMillis()
	{
		return( retry_interval_seconds * 1000 * 3 );
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
	
	public TRTrackerServerStats
	getStats(
		byte[]		hash )
	{
		TRTrackerServerTorrent	torrent = getTorrent( hash );
		
		if ( torrent == null ){
			
			return( null );
		}
		
		return( torrent.getStats());
	}	
	
	public TRTrackerServerPeer[]
	getPeers(
		byte[]		hash )
	{
		TRTrackerServerTorrent	torrent = getTorrent( hash );
		
		if ( torrent == null ){
			
			return( null );
		}
		
		return( torrent.getPeers());
	}
}
