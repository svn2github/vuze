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
	implements TRHost 
{
	public static final int RETRY_DELAY 	= 120;	// seconds
	public static final int DEFAULT_PORT	= 80;	// port to use if none in announce URL
	
	protected static TRHostImpl		singleton;
	
	protected Hashtable	server_map 	= new Hashtable();
	
	protected List	torrents	= new ArrayList();
	
	protected List	listeners	= new ArrayList();
	
	public static synchronized TRHost
	create()
	{
		if ( singleton == null ){
			
			singleton = new TRHostImpl();
		}
		
		return( singleton );
	}
	
	public void
	addTorrent(
		TRTrackerClient	tracker_client )
	{
		TOTorrent	torrent = tracker_client.getTorrent();	
		
		TRHostTorrentImpl	ht = addTorrentSupport( torrent );
		
		if ( ht == null ){
			
			return;
		}
		
		String bind_ip = COConfigurationManager.getStringParameter("Bind IP", "");

		String	url = "http://";
		
		if ( bind_ip.length() < 7 ){
				
			url += "127.0.0.1";
				
		}else{
				
			url += bind_ip;
		}

			// set the ip override so that we announce ourselves to other peers via the 
			// real external address, not the local one used to connect to the tracker 
			
		tracker_client.setIPOverride( ht.getTorrent().getAnnounceURL().getHost());
		
		tracker_client.setTrackerUrl(url + ":" + ht.getPort() + "/announce");	
		
		ht.start();
	}

	public synchronized void
	addTorrent(
		TOTorrent		torrent )
	{
		addTorrentSupport( torrent );
	}
	
	protected synchronized TRHostTorrentImpl
	addTorrentSupport(
			TOTorrent		torrent )
	{
		for (int i=0;i<torrents.size();i++){
			
			TRHostTorrent	ht = (TRHostTorrent)torrents.get(i);
			
			if ( ht.getTorrent() == torrent ){
		
					// already there
							
				return( null );
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
		
		torrents.add( host_torrent );
		
		for (int i=0;i<listeners.size();i++){
			
			((TRHostListener)listeners.get(i)).torrentAdded( host_torrent );
		}
		
		return( host_torrent );
	}
	
	protected synchronized void
	remove(
		TRHostTorrent	host_torrent )
	{
		torrents.remove( host_torrent );
		
		for (int i=0;i<listeners.size();i++){
			
			((TRHostListener)listeners.get(i)).torrentRemoved( host_torrent );
		}		
	}
	
	public synchronized TRHostTorrent[]
	getTorrents()
	{
		TRHostTorrent[]	res = new TRHostTorrent[torrents.size()];
		
		torrents.toArray( res );
		
		return( res );
	}
	
	public synchronized void
	addListener(
		TRHostListener	l )
	{
		listeners.add( l );
		
		for (int i=0;i<torrents.size();i++){
			
			l.torrentAdded((TRHostTorrent)torrents.get(i));
		}
	}
		
	public synchronized void
	removeListener(
		TRHostListener	l )
	{
		listeners.remove( l );
	}
}
