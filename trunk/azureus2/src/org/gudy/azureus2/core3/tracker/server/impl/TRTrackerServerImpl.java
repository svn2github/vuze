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
import java.io.*;
import java.util.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.tracker.server.*;

public class 
TRTrackerServerImpl 
	implements TRTrackerServer
{
	protected int	port;
	
	protected Map	torrent_map = new HashMap(); 
		
	protected Vector	listeners = new Vector();
	
	public
	TRTrackerServerImpl(
		int		_port )
		
		throws TRTrackerServerException
	{
		port					= _port;
		
		String bind_ip = COConfigurationManager.getStringParameter("Bind IP", "");

		try{
			ServerSocket ss;
			
			if ( bind_ip.length() < 7 ){
				
				ss = new ServerSocket( port );
				
			}else{
				
				ss = new ServerSocket( port, 128, InetAddress.getByName(bind_ip));
			}
		
			final ServerSocket	f_ss = ss;
			
			Thread accept_thread = 
					new Thread()
					{
						public void
						run()
						{
							acceptLoop( f_ss );
						}
					};
		
			accept_thread.setDaemon( true );
		
			accept_thread.start();									
	
		}catch( Throwable e ){
						
			throw( new TRTrackerServerException( "TRTrackerServer: accept fails: " + e.toString()));
		}			
		
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
	acceptLoop(
		ServerSocket	ss )
	{
		while(true){
	
			try{				
				final Socket socket = ss.accept();
								
				new Thread()
					{
						public void
						run()
						{			
							new TRTrackerServerProcessor( TRTrackerServerImpl.this, socket );
						}
					}.start();

			}catch( Throwable e ){
				
				e.printStackTrace();		
			}
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
		return( COConfigurationManager.getIntParameter("Tracker Poll Interval", DEFAULT_RETRY_DELAY ));
	}
	
	protected long
	getTimeoutIntervalInMillis()
	{
		return( getRetryInterval() * 1000 * 3 );
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
	
	protected synchronized boolean
	handleExternalRequest(
		String			header,
		OutputStream	os )
		
		throws IOException
	{
		for (int i=0;i<listeners.size();i++){
			
			if (((TRTrackerServerListener)listeners.elementAt(i)).handleExternalRequest( header, os )){
				
				return( true );
			}
		}
		
		return( false );
	}
	
	public synchronized void
	addListener(
		TRTrackerServerListener	l )
	{
		listeners.addElement( l );
	}
		
	public synchronized void
	removeListener(
		TRTrackerServerListener	l )
	{
		listeners.removeElement(l);
	}
}
