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
import java.io.*;
import java.net.*;

import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.tracker.host.*;
import org.gudy.azureus2.core3.tracker.server.*;
import org.gudy.azureus2.core3.tracker.client.*;
import org.gudy.azureus2.core3.torrent.*;

public class 
TRHostImpl
	implements 	TRHost, TRTrackerClientFactoryListener, 
				TRTrackerServerListener, TRTrackerServerFactoryListener,
				TRTrackerServerRequestListener
{
	protected static final int URL_DEFAULT_PORT		= 80;	// port to use if none in announce URL
	protected static final int URL_DEFAULT_PORT_SSL	= 443;	// port to use if none in announce URL
	
	protected static final int STATS_PERIOD_SECS	= 60;
		
	protected static TRHostImpl		singleton;
	
	protected TRHostConfigImpl		config;
		
	protected Hashtable				server_map 	= new Hashtable();
	
	protected List	host_torrents			= new ArrayList();
	protected Map	host_torrent_hash_map	= new HashMap();
	
	protected Map	host_torrent_map		= new HashMap();
	protected Map	tracker_client_map		= new HashMap();
	
	protected List	listeners			= new ArrayList();
	
	protected boolean	server_factory_listener_added;
	
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
			// we need to synchronize this so that the async (possible) establishment of
			// a server within the stats loop (to deal with public trackers with no locally
			// hosted torrents) doesn't get ahead of the reading of persisted torrents
			// If we allow the server to start early then it can potentially receive an
			// announce/scrape and result in the creation of an "external" torrent when
			// it should really be using an existing torrent 
			 
		synchronized(this){
					
			config = new TRHostConfigImpl(this);	
			
			TRTrackerClientFactory.addListener( this );
			
			Thread t = new Thread("TRHost::stats.loop")
						{
							public void
							run()
							{
								while(true){
									
									try{											
										if ( COConfigurationManager.getBooleanParameter( "Tracker Port Enable", true )){
										
											try{
													
												int port = COConfigurationManager.getIntParameter("Tracker Port", TRHost.DEFAULT_PORT );
												
												startServer( TRTrackerServerFactory.PR_TCP, port, false );
													
											}catch( Throwable e ){
												
												e.printStackTrace();
											}
										}
											
										if ( COConfigurationManager.getBooleanParameter( "Tracker Port SSL Enable", false )){
										
											try{
													
												int port = COConfigurationManager.getIntParameter("Tracker Port SSL", TRHost.DEFAULT_PORT_SSL );
												
												startServer( TRTrackerServerFactory.PR_TCP, port, true );
														
											}catch( Throwable e ){
												
												e.printStackTrace();
											}
										}						
										
										Thread.sleep( STATS_PERIOD_SECS*1000 );
										
										synchronized( TRHostImpl.this ){
											
											for (int i=0;i<host_torrents.size();i++){
				
												TRHostTorrent	ht = (TRHostTorrent)host_torrents.get(i);
												
												if ( ht instanceof TRHostTorrentHostImpl ){
																					
													((TRHostTorrentHostImpl)ht).updateStats();
													
												}else{
													
													((TRHostTorrentPublishImpl)ht).updateStats();
													
												}
											}
										}
										
										config.saveConfig();
										
									}catch( InterruptedException e ){
										
										e.printStackTrace();
										
										break;
									}
								}
							}
						};
			
			t.setDaemon(true);
			
			t.start();
		}
	}
	
	public void
	initialise(
		TRHostTorrentFinder	finder )
	{
		config.loadConfig( finder );
	}

	public synchronized TRHostTorrent
	hostTorrent(
		TOTorrent		torrent )
		
		throws TRHostException
	{
		return( hostTorrent( torrent, true ));
	}

	public synchronized TRHostTorrent
	hostTorrent(
		TOTorrent		torrent,
		boolean			persistent )
	
		throws TRHostException
	{
		return( addTorrent( torrent, TRHostTorrent.TS_STARTED, persistent ));
	}
	
	public synchronized TRHostTorrent
	publishTorrent(
		TOTorrent		torrent )
		
		throws TRHostException
	{
		return( addTorrent( torrent, TRHostTorrent.TS_PUBLISHED, true ));
	}
	
	protected synchronized TRHostTorrent
	addTorrent(
		TOTorrent		torrent,
		int				state,
		boolean			persistent )
		
		throws TRHostException
	{
		TRHostTorrent	ht = lookupHostTorrent( torrent );
		
		if ( ht != null ){
					
			// check that this isn't the explicit publish/host of a torrent already there
			// as an external torrent. If so then just replace the torrent
			
			try{
			
				ht = lookupHostTorrentViaHash( torrent.getHash());
			
				if ( ht instanceof TRHostTorrentHostImpl ){
					
					TRHostTorrentHostImpl hti = (TRHostTorrentHostImpl)ht;
					
					if ( hti.getTorrent() != torrent ){
						
						hti.setTorrent( torrent );	
					
						if ( persistent && !hti.isPersistent()){
							
							hti.setPersistent( true );
						}
						
						if ( state != TRHostTorrent.TS_PUBLISHED ){
				
							startHosting( hti );
				
							if ( state == TRHostTorrent.TS_STARTED ){
							
								hti.start();
							}
						}	
						
						for (int i=0;i<listeners.size();i++){
							
							((TRHostListener)listeners.get(i)).torrentChanged( ht );
						}
					}
				}
			}catch( TOTorrentException e ){
				
				e.printStackTrace();	
			}
			
			return( ht );
		}
		
		int		port;
		boolean	ssl;
		int		protocol	= TRTrackerServerFactory.PR_TCP;
		
		if ( state == TRHostTorrent.TS_PUBLISHED ){
		
			port = COConfigurationManager.getIntParameter("Tracker Port", TRHost.DEFAULT_PORT );
			
			ssl	= false;		
		}else{
		
			URL	announce_url = torrent.getAnnounceURL();
			
			String	protocol_str = announce_url.getProtocol();
			
			ssl = protocol_str.equalsIgnoreCase("https");
			
			if ( protocol_str.equalsIgnoreCase("udp")){
				
				protocol = TRTrackerServerFactory.PR_UDP;
			}
			
			boolean force_external = COConfigurationManager.getBooleanParameter("Tracker Port Force External", false );
			
			port = announce_url.getPort();
			
			if ( force_external ){
				
				String 	tracker_ip 		= COConfigurationManager.getStringParameter("Tracker IP", "");
	
				if ( 	tracker_ip.length() > 0 &&
						!announce_url.getHost().equalsIgnoreCase( tracker_ip )){
						
					if ( ssl ){
		
						port = COConfigurationManager.getIntParameter("Tracker Port SSL", TRHost.DEFAULT_PORT_SSL );
						
					}else{
						
						port = COConfigurationManager.getIntParameter("Tracker Port", TRHost.DEFAULT_PORT );
						
					}
				}
			}
			
			if ( port == -1 ){
				
				port = ssl?URL_DEFAULT_PORT_SSL:URL_DEFAULT_PORT;
			}
		}
		
		TRTrackerServer server = startServer( protocol, port, ssl );
		
		TRHostTorrent host_torrent;
	
		if ( state == TRHostTorrent.TS_PUBLISHED ){

			host_torrent = new TRHostTorrentPublishImpl( this, torrent );

		}else{
		
			host_torrent = new TRHostTorrentHostImpl( this, server, torrent, port );
		}
		
		host_torrent.setPersistent( persistent );
		
		host_torrents.add( host_torrent );
		
		try{
			host_torrent_hash_map.put( new HashWrapper( torrent.getHash()), host_torrent );
			
		}catch( TOTorrentException e ){
			
			e.printStackTrace();
		}
				
		host_torrent_map.put( torrent, host_torrent );
		
		if ( state != TRHostTorrent.TS_PUBLISHED ){
		
			startHosting((TRHostTorrentHostImpl)host_torrent );
		
			if ( state == TRHostTorrent.TS_STARTED ){
					
				host_torrent.start();
			}
		}

		for (int i=0;i<listeners.size();i++){
			
			((TRHostListener)listeners.get(i)).torrentAdded( host_torrent );
		}
		
		config.saveConfig();
		
		return( host_torrent );
	}
	
	protected synchronized TRTrackerServer
	startServer(
		int		protocol,
		int		port,
		boolean	ssl )
		
		throws TRHostException
	{
		String	key = ""+protocol+ ":" + port;
		
		TRTrackerServer	server = (TRTrackerServer)server_map.get( key );
			
		if ( server == null ){
				
			try{
				
				if ( ssl ){
					
					server = TRTrackerServerFactory.createSSL( protocol, port );
				
				}else{
				
					server = TRTrackerServerFactory.create( protocol, port );
				}
					
				server_map.put( key, server );
					
				server.addListener( this );
						
			}catch( TRTrackerServerException e ){
					
				LGLogger.log(0, 0, LGLogger.ERROR, "Tracker Host: failed to start server: " + e.toString());
	
				throw( new TRHostException( e.getMessage()));
			}
		}
		
		return( server );
	}
	
	protected TRHostTorrent
	lookupHostTorrent(
		TOTorrent	torrent )
	{
	  if (torrent == null)
	    return null;

		try{
			return((TRHostTorrent)host_torrent_hash_map.get( torrent.getHashWrapper()));
			
		}catch( TOTorrentException e ){
			
			e.printStackTrace();
		}
		
		return( null );
	}
	
	protected void
	startHosting(
		TRHostTorrentHostImpl	host_torrent )
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
		TRHostTorrent	host_torrent = (TRHostTorrent)host_torrent_map.get( tracker_client.getTorrent());
			
		if ( host_torrent instanceof TRHostTorrentHostImpl ){
			
			startHosting( (TRHostTorrentHostImpl)host_torrent, tracker_client );
		}
	}
	
	protected void
	startHosting(
		TRHostTorrentHostImpl	host_torrent,
		final TRTrackerClient 	tracker_client )
	{
		final TOTorrent	torrent = host_torrent.getTorrent();	

			// set the ip override so that we announce ourselves to other peers via the 
			// real external address, not the local one used to connect to the tracker 
			
		tracker_client.setIPOverride( torrent.getAnnounceURL().getHost());
		
			// hook into the client so that when the announce succeeds after the refresh below
			// we can force a rescrape to pick up the new status 
		
		TRTrackerClientListener	listener = 
			new TRTrackerClientListener()
			{
				public void
				receivedTrackerResponse(
					TRTrackerResponse	response	)
				{	
					try{
						TRTrackerScraperFactory.getSingleton().scrape( torrent, true );
					
					}finally{
						
						tracker_client.removeListener( this );
					}
				}

				public void
				urlChanged(
					String		url,
					boolean		explicit )
				{	
				}
					
				public void
				urlRefresh()
				{
				}
			};
		
		tracker_client.addListener(listener);

		tracker_client.refreshListeners();
	}

	protected synchronized void
	remove(
		TRHostTorrent	host_torrent )
	{
		if ( !host_torrents.contains( host_torrent )){
			
			return;
		}
		
		host_torrents.remove( host_torrent );
		
		TOTorrent	torrent = host_torrent.getTorrent();
		
		try{
			host_torrent_hash_map.remove(new HashWrapper(torrent.getHash()));
			
		}catch( TOTorrentException e ){
			
			e.printStackTrace();
		}
		
		host_torrent_map.remove( torrent );
		
		if ( host_torrent instanceof TRHostTorrentHostImpl ){
			
			stopHosting((TRHostTorrentHostImpl)host_torrent );
		}
		
		for (int i=0;i<listeners.size();i++){
			
			((TRHostListener)listeners.get(i)).torrentRemoved( host_torrent );
		}
		
		config.saveConfig();		
	}
	
	protected void
	stopHosting(
		TRHostTorrentHostImpl	host_torrent )
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
		TRHostTorrent	host_torrent = (TRHostTorrent)host_torrent_map.get( tracker_client.getTorrent());
			
		if ( host_torrent instanceof TRHostTorrentHostImpl ){
			
			stopHosting( (TRHostTorrentHostImpl)host_torrent, tracker_client );
		}
	}
	
	protected void
	stopHosting(
		final TRHostTorrentHostImpl	host_torrent,
		final TRTrackerClient 		tracker_client )
	{
		TOTorrent	torrent = host_torrent.getTorrent();	
				
			// unfortunately a lot of the "stop" operations that occur when a tracker client
			// connection is closed happen async. In particular the "stopped" message to the
			// tracker. Hence, if we switch the URL back here the "stopped" doesn't get
			// through.
			
		// for the moment stick a delay in to allow any async stuff to complete
		
		Thread thread = new Thread()
			{
				public void
				run()
				{
					try{
						Thread.sleep(2500);
						
					}catch( InterruptedException e ){
						
					}
					
					synchronized( TRHostImpl.this ){
						
							// got to look up the host torrent again as may have been
							// removed and re-added
						
						TRHostTorrent	ht = lookupHostTorrent( host_torrent.getTorrent());
						
							// check it's still in stopped state and hasn't been restarted
							
						if ( ht == null || 
								( 	ht == host_torrent &&
								 	ht.getStatus() == TRHostTorrent.TS_STOPPED )){
					
							tracker_client.clearIPOverride();
						}
					}
				}
			};
			
		thread.setDaemon(true);
		
		thread.start();
	}
	
	protected synchronized TRTrackerClient
	getTrackerClient(
		TRHostTorrent host_torrent )
	{
		return((TRTrackerClient)tracker_client_map.get( host_torrent.getTorrent()));
	}
	
	protected synchronized void
	hostTorrentStateChange(
		TRHostTorrent host_torrent )
	{
		TOTorrent	torrent = host_torrent.getTorrent();
		
		TRTrackerClient tc = (TRTrackerClient)tracker_client_map.get( torrent );
		
		if ( tc != null ){
			
			tc.refreshListeners();
		}
		
		config.saveConfig();			
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
	
	protected TRHostTorrent
	lookupHostTorrentViaHash(
		byte[]		hash )
	{
		return((TRHostTorrent)host_torrent_hash_map.get(new HashWrapper(hash)));
	}
	
		// reports from TRTrackerServer regarding state of hashes
		// if we get a "permitted" event for a torrent we know nothing about
		// the the server is allowing public hosting and this is a new hash
		// create an 'external' entry for it
		
	public synchronized boolean
	permitted(
		byte[]		hash,
		boolean		explicit  )
	{
		TRHostTorrent ht = lookupHostTorrentViaHash( hash );
		
		if ( ht != null ){
		
			if ( !explicit ){
				
				if ( ht.getStatus() != TRHostTorrent.TS_STARTED ){
					
					return( false );
				}
			}
			
			return( true );
		}
		
		addExternalTorrent( hash, TRHostTorrent.TS_STARTED );
		
		return( true );
	}
	
	protected synchronized void
	addExternalTorrent(
		byte[]		hash,
		int			state )
	{
		if ( lookupHostTorrentViaHash( hash ) != null ){
			
			return;
		}
		
		String 	tracker_ip 		= COConfigurationManager.getStringParameter("Tracker IP", "127.0.0.1");
						
			// external torrents don't care whether ssl or not so just assume non-ssl for simplicity 
			
		int port = COConfigurationManager.getIntParameter("Tracker Port", TRHost.DEFAULT_PORT );

		try{
			TOTorrent	external_torrent = new TRHostExternalTorrent(hash, new URL( "http://" + tracker_ip + ":" + port + "/announce"));
		
			addTorrent( external_torrent, state, true );	
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	public synchronized boolean
	denied(
		byte[]		hash,
		boolean		permitted )
	{
		return( true );
	}
	
	public boolean
	handleExternalRequest(
		String			client_address,
		String			url,
		String			header,
		InputStream		is,
		OutputStream	os )
		
		throws IOException
	{

		for (int i=0;i<listeners.size();i++){

			TRHostListener	listener;
			
			synchronized( listeners ){
				
				if ( i >= listeners.size()){
					
					break;
				}
				
				listener	= (TRHostListener)listeners.get(i);
			}
			
			if ( listener.handleExternalRequest( client_address, url, header, is, os )){
				
				return( true );
			}
		}
		
		return( false );
	}
	
	public TRHostTorrent
	getHostTorrent(
		TOTorrent		torrent )
	{
		return( lookupHostTorrent( torrent ));
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
	
	protected synchronized void
	torrentListenerRegistered()
	{
		if ( !server_factory_listener_added ){
			
			server_factory_listener_added	= true;
			
			TRTrackerServerFactory.addListener( this );
		}
	}
	
	public void
	serverCreated(
		TRTrackerServer	server )
	{
		server.addRequestListener(this);
	}
	
	public void
	postProcess(
		TRTrackerServerRequest	request )
	{
		if ( 	request.getType() 	== TRTrackerServerRequest.RT_ANNOUNCE  ||
				request.getType() 	== TRTrackerServerRequest.RT_SCRAPE ){
			
			TRTrackerServerTorrent ts_torrent = request.getTorrent();
		
			HashWrapper	hash_wrapper = ts_torrent.getHash();
			
			TRHostTorrent h_torrent = lookupHostTorrentViaHash( hash_wrapper.getHash());
			
			if ( h_torrent != null ){
				
				TRHostTorrentRequest	req = new TRHostTorrentRequestImpl( h_torrent, new TRHostPeerHostImpl(request.getPeer()), request );
			
				if ( h_torrent instanceof TRHostTorrentHostImpl ){
				
					((TRHostTorrentHostImpl)h_torrent).postProcess( req );
				}else{
					
					((TRHostTorrentPublishImpl)h_torrent).postProcess( req );	
				}
			}
		}
	}
	
	public void
	close()
	{
		config.saveConfig();
	}
}
