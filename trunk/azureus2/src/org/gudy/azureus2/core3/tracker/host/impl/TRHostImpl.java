/*
 * File    : TRHostImpl.java
 * Created : 24-Oct-2003
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
 */

import java.util.*;

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.tracker.host.*;
import org.gudy.azureus2.core3.tracker.server.*;
import org.gudy.azureus2.core3.tracker.client.*;
import org.gudy.azureus2.core3.torrent.*;

public class 
TRHostImpl
	implements TRHost, TRTrackerClientFactoryListener 
{
	public static final int RETRY_DELAY 	= 120;	// seconds
	public static final int DEFAULT_PORT	= 80;	// port to use if none in announce URL
	
	protected static TRHostImpl		singleton;
	
	protected Hashtable	server_map 	= new Hashtable();
	
	protected List	host_torrents		= new ArrayList();
	
	protected Map	host_torrent_map	= new HashMap();
	protected Map	tracker_client_map	= new HashMap();
	
	protected List	listeners			= new ArrayList();
	
	public static synchronized TRHost
	create()
	{
		if ( singleton == null ){
			
			singleton = new TRHostImpl();
		}
		
		return( singleton );
	}
	
	protected
	TRHostImpl()
	{			
		TRTrackerClientFactory.addListener( this );
	}
	

	public synchronized void
	addTorrent(
		TOTorrent		torrent )
	{
		for (int i=0;i<host_torrents.size();i++){
			
			TRHostTorrent	ht = (TRHostTorrent)host_torrents.get(i);
			
			if ( ht.getTorrent() == torrent ){
		
					// already there
							
				return;
			}
		}
		
		int	port = torrent.getAnnounceURL().getPort();
		
		if ( port == -1 ){
			
			port = DEFAULT_PORT;
		}
		
		TRTrackerServer	server = (TRTrackerServer)server_map.get( new Integer( port ));
		
		if ( server == null ){
			
			try{
			
				server = TRTrackerServerFactory.create( port, RETRY_DELAY );
			
				server_map.put( new Integer( port ), server );
				
			}catch( TRTrackerServerException e ){
				
				e.printStackTrace();
			}
		}
		
		TRHostTorrentImpl host_torrent = new TRHostTorrentImpl( this, server, torrent, port );
		
		host_torrents.add( host_torrent );
		host_torrent_map.put( torrent, host_torrent );
		
		startHosting( host_torrent );
		
			// start off running
					
		host_torrent.start();

		for (int i=0;i<listeners.size();i++){
			
			((TRHostListener)listeners.get(i)).torrentAdded( host_torrent );
		}
	}
	
	protected void
	startHosting(
		TRHostTorrentImpl	host_torrent )
	{
		TOTorrent	torrent = host_torrent.getTorrent();
		
		TRTrackerClient tc = (TRTrackerClient)tracker_client_map.get( torrent );
		
		if ( tc != null ){
			
			startHosting( host_torrent, tc );
		}
	}
	
	protected void
	startHosting(
		TRTrackerClient	tracker_client )
	{
		TRHostTorrentImpl	host_torrent = (TRHostTorrentImpl)host_torrent_map.get( tracker_client.getTorrent());
			
		if ( host_torrent != null ){
			
			startHosting( host_torrent, tracker_client );
		}
	}
	
	protected void
	startHosting(
		TRHostTorrentImpl	host_torrent,
		TRTrackerClient 	tracker_client )
	{
		TOTorrent	torrent = host_torrent.getTorrent();	
				
		String bind_ip = COConfigurationManager.getStringParameter("Bind IP", "");

		String	url = "http://";
		
		if ( bind_ip.length() < 7 ){
				
			url += "127.0.0.1";
				
		}else{
				
			url += bind_ip;
		}

			// set the ip override so that we announce ourselves to other peers via the 
			// real external address, not the local one used to connect to the tracker 
			
		tracker_client.setIPOverride( torrent.getAnnounceURL().getHost());
		
		tracker_client.setTrackerUrl(url + ":" + host_torrent.getPort() + "/announce");	
	}

	protected synchronized void
	remove(
		TRHostTorrent	host_torrent )
	{
		if ( !host_torrents.contains( host_torrent )){
			
			return;
		}
		
		host_torrents.remove( host_torrent );
		host_torrent_map.remove( host_torrent.getTorrent());
		
		if ( host_torrent != null ){
			
			stopHosting((TRHostTorrentImpl)host_torrent );
			
			for (int i=0;i<listeners.size();i++){
			
				((TRHostListener)listeners.get(i)).torrentRemoved( host_torrent );
			}
		}		
	}
	
	protected void
	stopHosting(
		TRHostTorrentImpl	host_torrent )
	{
		TOTorrent	torrent = host_torrent.getTorrent();
		
		TRTrackerClient tc = (TRTrackerClient)tracker_client_map.get( torrent );
		
		if ( tc != null ){
			
			stopHosting( host_torrent, tc );
		}
	}
	
	protected void
	stopHosting(
		TRTrackerClient	tracker_client )
	{
		TRHostTorrentImpl	host_torrent = (TRHostTorrentImpl)host_torrent_map.get( tracker_client.getTorrent());
			
		if ( host_torrent != null ){
			
			stopHosting( host_torrent, tracker_client );
		}
	}
	
	protected void
	stopHosting(
		TRHostTorrentImpl	host_torrent,
		TRTrackerClient 	tracker_client )
	{
		TOTorrent	torrent = host_torrent.getTorrent();	
				
			// unfortunately a lot of the "stop" operations that occur when a tracker client
			// connection is closed happen async. In particular the "stopped" message to the
			// tracker. Hence, if we switch the URL back here the "stopped" doesn't get
			// through.
			
			// For the moment leave the torrent in its hosted state - its most likely that
			// it'll only be restarted or removed anyway.
					
		// tracker_client.clearIPOverride();
		
		// tracker_client.resetTrackerUrl();	
	}
	
	protected synchronized void
	hostTorrentStateChange(
		TRHostTorrentImpl host_torrent )
	{
		TOTorrent	torrent = host_torrent.getTorrent();
		
		TRTrackerClient tc = (TRTrackerClient)tracker_client_map.get( torrent );
		
		if ( tc != null ){
			
			tc.refreshListeners();
		}			
	}
	
	public synchronized TRHostTorrent[]
	getTorrents()
	{
		TRHostTorrent[]	res = new TRHostTorrent[host_torrents.size()];
		
		host_torrents.toArray( res );
		
		return( res );
	}
	
	public synchronized void
	clientCreated(
		TRTrackerClient		client )
	{
		tracker_client_map.put( client.getTorrent(), client );
		
		startHosting( client );
	}
	
	public synchronized void
	clientDestroyed(
		TRTrackerClient		client )
	{
		tracker_client_map.remove( client.getTorrent());
		
		stopHosting( client );
	}
	
	public synchronized void
	addListener(
		TRHostListener	l )
	{
		listeners.add( l );
		
		for (int i=0;i<host_torrents.size();i++){
			
			l.torrentAdded((TRHostTorrent)host_torrents.get(i));
		}
	}
		
	public synchronized void
	removeListener(
		TRHostListener	l )
	{
		listeners.remove( l );
	}
}
