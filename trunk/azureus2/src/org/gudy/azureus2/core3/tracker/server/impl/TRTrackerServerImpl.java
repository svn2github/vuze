/*
 * File    : TRTrackerServerImpl.java
 * Created : 19-Jan-2004
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
 *
 */


import java.util.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.ipfilter.*;
import org.gudy.azureus2.core3.tracker.server.*;

public abstract class 
TRTrackerServerImpl 
	implements 	TRTrackerServer
{
	protected static final int RETRY_MINIMUM_SECS			= 60;
	protected static final int RETRY_MINIMUM_MILLIS			= RETRY_MINIMUM_SECS*1000;
	protected static final int CLIENT_TIMEOUT_MULTIPLIER	= 3;
	
	protected static final int TIMEOUT_CHECK 				= RETRY_MINIMUM_MILLIS*CLIENT_TIMEOUT_MULTIPLIER;
	
	protected static boolean	send_peer_ids	= true;
	
	static{
		final String	param = "Tracker Send Peer IDs";
		
		send_peer_ids = COConfigurationManager.getBooleanParameter( param, true );
		
		COConfigurationManager.addParameterListener(
				param,
				new ParameterListener()
				{
					public void
					parameterChanged(
							String	value )
					{
						send_peer_ids = COConfigurationManager.getBooleanParameter( param, true );
					}
				});
	}
	
	protected static boolean
	getSendPeerIds()
	{
		return( send_peer_ids );
	}
	
	protected IpFilter	ip_filter	= IpFilter.getInstance();
	
	protected Map		torrent_map = new HashMap(); 
	
	protected int		current_retry_interval;
	
	protected Vector	listeners 			= new Vector();
		
	
	protected
	TRTrackerServerImpl()
	{
		Thread timer_thread = 
			new Thread("TrackerServer:timer.loop")
			{
				public void
				run( )
				{
					timerLoop();
				}
			};
		
		timer_thread.setDaemon( true );
		
		timer_thread.start();
	}
	
	public int
	getRetryInterval()
	{		
		return( current_retry_interval );
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
						
						TRTrackerServerTorrentImpl	t = (TRTrackerServerTorrentImpl)it.next();
						
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
							
							TRTrackerServerTorrentImpl	t = (TRTrackerServerTorrentImpl)it.next();
							
							t.exportPeersToMap( temp );
						}
					}
				}
				
			}catch( InterruptedException e ){
				
				e.printStackTrace();
			}
			
		}
	}
	
	public synchronized void
	permit(
		byte[]		_hash,
		boolean		_explicit )
	
		throws TRTrackerServerException
	{
		// System.out.println( "TRTrackerServerImpl::permit( " + _explicit + ")");
		
		HashWrapper	hash = new HashWrapper( _hash );
		
		TRTrackerServerTorrentImpl	entry = (TRTrackerServerTorrentImpl)torrent_map.get( hash );
		
		if ( entry == null ){
			
			for (int i=0;i<listeners.size();i++){
				
				if ( !((TRTrackerServerListener)listeners.elementAt(i)).permitted( _hash, _explicit )){
					
					throw( new TRTrackerServerException( "operation denied"));			
				}
			}
			
			entry = new TRTrackerServerTorrentImpl( this, hash );
			
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
	
	public TRTrackerServerTorrentImpl
	getTorrent(
		byte[]		hash )
	{
		return((TRTrackerServerTorrentImpl)torrent_map.get(new HashWrapper(hash)));
	}
	
	public synchronized TRTrackerServerTorrentImpl[]
	getTorrents()
	{
		TRTrackerServerTorrentImpl[]	res = new TRTrackerServerTorrentImpl[torrent_map.size()];
		
		torrent_map.values().toArray( res );
		
		return( res );	
	}
	
	public TRTrackerServerTorrentStats
	getStats(
		byte[]		hash )
	{
		TRTrackerServerTorrentImpl	torrent = getTorrent( hash );
		
		if ( torrent == null ){
			
			return( null );
		}
		
		return( torrent.getStats());
	}	
	
	public TRTrackerServerPeer[]
	getPeers(
		byte[]		hash )
	{
		TRTrackerServerTorrentImpl	torrent = getTorrent( hash );
		
		if ( torrent == null ){
			
			return( null );
		}
		
		return( torrent.getPeers());
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
