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
import org.gudy.azureus2.core3.ipfilter.*;
import org.gudy.azureus2.core3.tracker.server.*;

public class 
TRTrackerServerImpl 
	implements TRTrackerServer
{
	protected static final int THREAD_POOL_SIZE				= 10;
	
	protected static final int RETRY_MINIMUM_SECS			= 60;
	protected static final int RETRY_MINIMUM_MILLIS			= RETRY_MINIMUM_SECS*1000;
	protected static final int CLIENT_TIMEOUT_MULTIPLIER	= 3;
	
	protected static final int TIMEOUT_CHECK 				= RETRY_MINIMUM_MILLIS*CLIENT_TIMEOUT_MULTIPLIER;
	
	protected IpFilter	ip_filter	= IpFilter.getInstance();
	
	protected int	port;
	
	protected Map	torrent_map = new HashMap(); 
		
	protected int	current_retry_interval;
	
	protected Vector	listeners = new Vector();
	
	ThreadPool	thread_pool = new ThreadPool( THREAD_POOL_SIZE );
	
	public
	TRTrackerServerImpl(
		int		_port )
		
		throws TRTrackerServerException
	{
		port					= _port;
		
		current_retry_interval	= COConfigurationManager.getIntParameter("Tracker Poll Interval Min", DEFAULT_MIN_RETRY_DELAY );
		
		if ( current_retry_interval < RETRY_MINIMUM_SECS ){
			
			current_retry_interval = RETRY_MINIMUM_SECS;
		}

		String bind_ip = COConfigurationManager.getStringParameter("Bind IP", "");

		try{
			ServerSocket ss;
			
			if ( bind_ip.length() < 7 ){
				
				ss = new ServerSocket( port, 128 );
				
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
										
				String	ip = socket.getInetAddress().getHostAddress();
							
				if ( !ip_filter.isInRange( ip )){
					
					thread_pool.run( new TRTrackerServerProcessor( this, socket ));
											
				}else{
					
					socket.close();
				}
			}catch( Throwable e ){
				
				e.printStackTrace();		
			}
		}
	}
	
	protected void
	timerLoop()
	{
		long	time_to_go = TIMEOUT_CHECK;
		
		while(true){
	
			try{
				Thread.sleep( RETRY_MINIMUM_MILLIS );
				
				time_to_go -= RETRY_MINIMUM_MILLIS;
	
					// recalc tracker interval every minute
					
				int	min 	= COConfigurationManager.getIntParameter("Tracker Poll Interval Min", DEFAULT_MIN_RETRY_DELAY );
				int	max 	= COConfigurationManager.getIntParameter("Tracker Poll Interval Max", DEFAULT_MAX_RETRY_DELAY );
				int	inc_by 	= COConfigurationManager.getIntParameter("Tracker Poll Inc By", DEFAULT_INC_BY );
				int	inc_per = COConfigurationManager.getIntParameter("Tracker Poll Inc Per", DEFAULT_INC_PER );
			
				int	retry = min;
						
				int	clients = 0;
						
				synchronized(this){
						
					Iterator	it = torrent_map.values().iterator();
						
					while(it.hasNext()){
							
						Map	temp = new HashMap();
							
							// this triggers timeouts...
								
						TRTrackerServerTorrent	t = (TRTrackerServerTorrent)it.next();
							
						clients += t.getPeers().length;
					}
				}
						
				if ( inc_by > 0 && inc_per > 0 ){
							
					retry += inc_by * (clients/inc_per);
				}
						
				if ( max > 0 && retry > max ){
							
					retry = max;
				}
						
				if ( retry < RETRY_MINIMUM_SECS ){
							
					retry = RETRY_MINIMUM_SECS;
				}
						
				current_retry_interval = retry;
											
					// timeout dead clients
						
				if ( time_to_go <= 0 ){
				
					time_to_go = TIMEOUT_CHECK;
					
					synchronized(this){
						
						Iterator	it = torrent_map.values().iterator();
						
						while(it.hasNext()){
							
							Map	temp = new HashMap();
							
								// this triggers timeouts...
								
							TRTrackerServerTorrent	t = (TRTrackerServerTorrent)it.next();
							
							t.exportPeersToMap( temp );
						}
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
		return( current_retry_interval );
	}
		
	public synchronized void
	permit(
		byte[]		_hash,
		boolean		_explicit )
		
		throws TRTrackerServerException
	{
		// System.out.println( "TRTrackerServerImpl::permit( " + _explicit + ")");
		
		HashWrapper	hash = new HashWrapper( _hash );
		
		TRTrackerServerTorrent	entry = (TRTrackerServerTorrent)torrent_map.get( hash );
		
		if ( entry == null ){
			
			for (int i=0;i<listeners.size();i++){
			
				if ( !((TRTrackerServerListener)listeners.elementAt(i)).permitted( _hash, _explicit )){
		
					throw( new TRTrackerServerException( "operation denied"));			
				}
			}
			
			entry = new TRTrackerServerTorrent( this, hash );
			
			torrent_map.put( hash, entry );
		}
	}
		
	public synchronized void
	deny(
		byte[]		_hash,
		boolean		_explicit )
		
		throws TRTrackerServerException
	{
		// System.out.println( "TRTrackerServerImpl::deny( " + _explicit + ")");
		
		HashWrapper	hash = new HashWrapper( _hash );
		
		torrent_map.remove( hash );

		for (int i=0;i<listeners.size();i++){
			
			if ( !((TRTrackerServerListener)listeners.elementAt(i)).denied( _hash, _explicit )){				
		
				throw( new TRTrackerServerException( "operation denied"));			
			}
		}
	}
	
	protected TRTrackerServerTorrent
	getTorrent(
		byte[]		hash )
	{
		return((TRTrackerServerTorrent)torrent_map.get(new HashWrapper(hash)));
	}
	
	protected synchronized TRTrackerServerTorrent[]
	getTorrents()
	{
		TRTrackerServerTorrent[]	res = new TRTrackerServerTorrent[torrent_map.size()];
		
		torrent_map.values().toArray( res );
		
		return( res );	
	}
	
	public TRTrackerServerTorrentStats
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
