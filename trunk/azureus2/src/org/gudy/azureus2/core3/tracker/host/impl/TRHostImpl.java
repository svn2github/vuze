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
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManagerListener;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.tracker.host.*;
import org.gudy.azureus2.core3.tracker.server.*;
import org.gudy.azureus2.core3.tracker.client.*;
import org.gudy.azureus2.core3.torrent.*;

public class 
TRHostImpl
	implements 	TRHost, TRTrackerClientFactoryListener, 
				TRTrackerServerListener, TRTrackerServerFactoryListener,
				TRTrackerServerRequestListener, TRTrackerServerAuthenticationListener
{
	protected static final int URL_DEFAULT_PORT		= 80;	// port to use if none in announce URL
	protected static final int URL_DEFAULT_PORT_SSL	= 443;	// port to use if none in announce URL
	
	protected static final int STATS_PERIOD_SECS	= 60;
		
	protected static TRHostImpl	singleton;
	protected static AEMonitor 	class_mon 	= new AEMonitor( "TRHost:class" );

	protected TRHostConfigImpl		config;
		
	protected Hashtable				server_map 	= new Hashtable();
	
	protected List	host_torrents			= new ArrayList();
	protected Map	host_torrent_hash_map	= new HashMap();
	
	protected Map	host_torrent_map		= new HashMap();
	protected Map	tracker_client_map		= new HashMap();
	
	private static final int LDT_TORRENT_ADDED			= 1;
	private static final int LDT_TORRENT_REMOVED		= 2;
	private static final int LDT_TORRENT_CHANGED		= 3;
	
	private ListenerManager	listeners 	= ListenerManager.createAsyncManager(
		"TRHost:ListenDispatcher",
		new ListenerManagerDispatcher()
		{
			public void
			dispatch(
				Object		_listener,
				int			type,
				Object		value )
			{
				TRHostListener	target = (TRHostListener)_listener;
		
				if ( type == LDT_TORRENT_ADDED ){
					
					target.torrentAdded((TRHostTorrent)value);
					
				}else if ( type == LDT_TORRENT_REMOVED ){
						
					target.torrentRemoved((TRHostTorrent)value);
						
				}else if ( type == LDT_TORRENT_CHANGED ){
					
					target.torrentChanged((TRHostTorrent)value);
				}
			}
		});	
	
	protected List	auth_listeners		= new ArrayList();
	
	protected boolean	server_factory_listener_added;
	
	protected AEMonitor this_mon 	= new AEMonitor( "TRHost" );

	public static TRHost
	create()
	{
		try{
			class_mon.enter();
		
			if ( singleton == null ){
				
				singleton = new TRHostImpl();
			}
			
			return( singleton );
			
		}finally{
			
			class_mon.exit();
		}
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
			 
		try{
			this_mon.enter();
					
			config = new TRHostConfigImpl(this);	
			
			TRTrackerClientFactory.addListener( this );
			
			Thread t = new AEThread("TRHost::stats.loop")
						{
							public void
							runSupport()
							{
								while(true){
									
									try{											
										if ( COConfigurationManager.getBooleanParameter( "Tracker Port Enable", true )){
										
											try{
													
												int port = COConfigurationManager.getIntParameter("Tracker Port", TRHost.DEFAULT_PORT );
												
												startServer( TRTrackerServerFactory.PR_TCP, port, false );
													
											}catch( Throwable e ){
												
												Debug.printStackTrace( e );
											}
										}
										
										if ( COConfigurationManager.getBooleanParameter( "Tracker Port UDP Enable", false )){
											
											try{
														
												int port = COConfigurationManager.getIntParameter("Tracker Port", TRHost.DEFAULT_PORT );
													
												startServer( TRTrackerServerFactory.PR_UDP, port, false );
														
											}catch( Throwable e ){
													
												Debug.printStackTrace( e );
											}
										}
											
										if ( COConfigurationManager.getBooleanParameter( "Tracker Port SSL Enable", false )){
										
											try{
													
												int port = COConfigurationManager.getIntParameter("Tracker Port SSL", TRHost.DEFAULT_PORT_SSL );
												
												startServer( TRTrackerServerFactory.PR_TCP, port, true );
														
											}catch( Throwable e ){
												
												Debug.printStackTrace( e );
											}
										}						
										
										Thread.sleep( STATS_PERIOD_SECS*1000 );
										
										try{
											this_mon.enter();
											
											for (int i=0;i<host_torrents.size();i++){
				
												TRHostTorrent	ht = (TRHostTorrent)host_torrents.get(i);
												
												if ( ht instanceof TRHostTorrentHostImpl ){
																					
													((TRHostTorrentHostImpl)ht).updateStats();
													
												}else{
													
													((TRHostTorrentPublishImpl)ht).updateStats();
													
												}
											}
										}finally{
											
											this_mon.exit();
										}
										
										config.saveConfig();
										
									}catch( InterruptedException e ){
										
										Debug.printStackTrace( e );
										
										break;
									}
								}
							}
						};
			
			t.setDaemon(true);
			
				// try to ensure that the tracker stats are collected reasonably
				// regularly
			
			t.setPriority( Thread.MAX_PRIORITY -1);
			
			t.start();
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	initialise(
		TRHostTorrentFinder	finder )
	{
		config.loadConfig( finder );
	}

	public String
	getName()
	{
		return( TRTrackerServer.DEFAULT_NAME );
	}
	
	public TRHostTorrent
	hostTorrent(
		TOTorrent		torrent )
		
		throws TRHostException
	{
		return( hostTorrent( torrent, true ));
	}

	public TRHostTorrent
	hostTorrent(
		TOTorrent		torrent,
		boolean			persistent )
	
		throws TRHostException
	{
		return( addTorrent( torrent, TRHostTorrent.TS_STARTED, persistent ));
	}
	
	public TRHostTorrent
	publishTorrent(
		TOTorrent		torrent )
		
		throws TRHostException
	{
		return( addTorrent( torrent, TRHostTorrent.TS_PUBLISHED, true ));
	}
	
	protected TRHostTorrent
	addTorrent(
		TOTorrent		torrent,
		int				state,
		boolean			persistent )
		
		throws TRHostException
	{
		try{
			this_mon.enter();
		
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
							
							listeners.dispatch( LDT_TORRENT_CHANGED, ht );
						}
					}
				}catch( TOTorrentException e ){
					
					Debug.printStackTrace( e );	
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
				
				Debug.printStackTrace( e );
			}
					
			host_torrent_map.put( torrent, host_torrent );
			
			if ( state != TRHostTorrent.TS_PUBLISHED ){
			
				startHosting((TRHostTorrentHostImpl)host_torrent );
			
				if ( state == TRHostTorrent.TS_STARTED ){
						
					host_torrent.start();
				}
			}
	
			listeners.dispatch( LDT_TORRENT_ADDED, host_torrent );
			
			config.saveConfig();
			
			return( host_torrent );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected TRTrackerServer
	startServer(
		int		protocol,
		int		port,
		boolean	ssl )
		
		throws TRHostException
	{
		try{
			this_mon.enter();
		
			String	key = ""+protocol+ ":" + port;
			
			TRTrackerServer	server = (TRTrackerServer)server_map.get( key );
				
			if ( server == null ){
					
				try{
					
					if ( ssl ){
						
						server = TRTrackerServerFactory.createSSL( protocol, port, true );
					
					}else{
					
						server = TRTrackerServerFactory.create( protocol, port, true );
					}
						
					server_map.put( key, server );
						
					if ( auth_listeners.size() > 0 ){
						
						server.addAuthenticationListener( this );
					}
					
					server.addListener( this );
							
				}catch( TRTrackerServerException e ){
						
					LGLogger.log(0, 0, LGLogger.ERROR, "Tracker Host: failed to start server: " + e.toString());
		
					throw( new TRHostException( e.getMessage()));
				}
			}
			
			return( server );
			
		}finally{
			
			this_mon.exit();
		}
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
			
			Debug.printStackTrace( e );
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

	protected void
	remove(
		TRHostTorrent	host_torrent )
	{
		try{
			this_mon.enter();
		
			if ( !host_torrents.contains( host_torrent )){
				
				return;
			}
			
			host_torrents.remove( host_torrent );
			
			TOTorrent	torrent = host_torrent.getTorrent();
			
			try{
				host_torrent_hash_map.remove(new HashWrapper(torrent.getHash()));
				
			}catch( TOTorrentException e ){
				
				Debug.printStackTrace( e );
			}
			
			host_torrent_map.remove( torrent );
			
			if ( host_torrent instanceof TRHostTorrentHostImpl ){
				
				stopHosting((TRHostTorrentHostImpl)host_torrent );
			}
			
			listeners.dispatch( LDT_TORRENT_REMOVED, host_torrent );
			
			config.saveConfig();	
			
		}finally{
			
			this_mon.exit();
		}
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
		
		Thread thread = new AEThread("StopHosting")
			{
				public void
				runSupport()
				{
					try{
						Thread.sleep(2500);
						
					}catch( InterruptedException e ){
						
					}
					
					try{
						this_mon.enter();
						
							// got to look up the host torrent again as may have been
							// removed and re-added
						
						TRHostTorrent	ht = lookupHostTorrent( host_torrent.getTorrent());
						
							// check it's still in stopped state and hasn't been restarted
							
						if ( ht == null || 
								( 	ht == host_torrent &&
								 	ht.getStatus() == TRHostTorrent.TS_STOPPED )){
					
							tracker_client.clearIPOverride();
						}
					}finally{
						
						this_mon.exit();
					}
				}
			};
			
		thread.setDaemon(true);
		
		thread.start();
	}
	
	protected TRTrackerClient
	getTrackerClient(
		TRHostTorrent host_torrent )
	{
		try{
			this_mon.enter();
		
			return((TRTrackerClient)tracker_client_map.get( host_torrent.getTorrent()));
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected void
	hostTorrentStateChange(
		TRHostTorrent host_torrent )
	{
		try{
			this_mon.enter();
		
			TOTorrent	torrent = host_torrent.getTorrent();
			
			TRTrackerClient tc = (TRTrackerClient)tracker_client_map.get( torrent );
			
			if ( tc != null ){
				
				tc.refreshListeners();
			}
			
			config.saveConfig();
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public TRHostTorrent[]
	getTorrents()
	{
		try{
			this_mon.enter();
		
			TRHostTorrent[]	res = new TRHostTorrent[host_torrents.size()];
		
			host_torrents.toArray( res );
		
			return( res );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	clientCreated(
		TRTrackerClient		client )
	{
		try{
			this_mon.enter();
		
			tracker_client_map.put( client.getTorrent(), client );
		
			startHosting( client );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	clientDestroyed(
		TRTrackerClient		client )
	{
		try{
			this_mon.enter();
		
			tracker_client_map.remove( client.getTorrent());
		
			stopHosting( client );
			
		}finally{
			
			this_mon.exit();
		}
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
		
	public boolean
	permitted(
		byte[]		hash,
		boolean		explicit  )
	{
		try{
			this_mon.enter();
		
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
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected void
	addExternalTorrent(
		byte[]		hash,
		int			state )
	{
		try{
			this_mon.enter();
		
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
				
				Debug.printStackTrace( e );
			}
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public boolean
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
		List	listeners_copy = listeners.getListenersCopy();
		
		for (int i=0;i<listeners_copy.size();i++){

			TRHostListener	listener = (TRHostListener)listeners_copy.get(i);
			
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
	
	public void
	addListener(
		TRHostListener	l )
	{
		try{
			this_mon.enter();
		
			listeners.addListener( l );
			
			for (int i=0;i<host_torrents.size();i++){
				
				listeners.dispatch( l, LDT_TORRENT_ADDED, host_torrents.get(i));
			}
		}finally{
			
			this_mon.exit();
		}
	}
		
	public void
	removeListener(
		TRHostListener	l )
	{
		listeners.removeListener( l );
	}
	
	protected void
	torrentListenerRegistered()
	{
		try{
			this_mon.enter();
		
			if ( !server_factory_listener_added ){
				
				server_factory_listener_added	= true;
				
				TRTrackerServerFactory.addListener( this );
			}
		}finally{
			
			this_mon.exit();
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
	
		throws TRTrackerServerException
	{
		if ( 	request.getType() 	== TRTrackerServerRequest.RT_ANNOUNCE  ||
				request.getType() 	== TRTrackerServerRequest.RT_SCRAPE ){
			
			TRTrackerServerTorrent ts_torrent = request.getTorrent();
		
			HashWrapper	hash_wrapper = ts_torrent.getHash();
			
			TRHostTorrent h_torrent = lookupHostTorrentViaHash( hash_wrapper.getHash());
			
			if ( h_torrent != null ){
				
				TRHostTorrentRequest	req = new TRHostTorrentRequestImpl( h_torrent, new TRHostPeerHostImpl(request.getPeer()), request );
			
				try{
					if ( h_torrent instanceof TRHostTorrentHostImpl ){
					
						((TRHostTorrentHostImpl)h_torrent).postProcess( req );
					}else{
						
						((TRHostTorrentPublishImpl)h_torrent).postProcess( req );	
					}
				}catch( TRHostException e ){
					
					throw( new TRTrackerServerException( "Post process fails", e ));
				}
			}
		}
	}
	
	public void
	close()
	{
		config.saveConfig();
	}
	
	public boolean
	authenticate(
		URL			resource,
		String		user,
		String		password )
	{
		for (int i=0;i<auth_listeners.size();i++){
			
			try{
				boolean res = ((TRHostAuthenticationListener)auth_listeners.get(i)).authenticate( resource, user, password );
				
				if ( res ){
					
					return(true );
				}
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}
		
		return( false );
	}
	
	public byte[]
	authenticate(
		URL			resource,
		String		user )
	{
		for (int i=0;i<auth_listeners.size();i++){
			
			try{
				byte[] res = ((TRHostAuthenticationListener)auth_listeners.get(i)).authenticate( resource, user );
				
				if ( res != null ){
					
					return( res );
				}
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}
		
		return( null );
	}
	
	public void
	addAuthenticationListener(
		TRHostAuthenticationListener	l )
	{	
		try{
			this_mon.enter();
		
			auth_listeners.add(l);
			
			if ( auth_listeners.size() == 1 ){
				
				Iterator it = server_map.values().iterator();
				
				while( it.hasNext()){
					
					((TRTrackerServer)it.next()).addAuthenticationListener( this );
				}			
			}
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	removeAuthenticationListener(
		TRHostAuthenticationListener	l )
	{	
		try{
			this_mon.enter();
		
			auth_listeners.remove(l);
			
			if ( auth_listeners.size() == 0 ){
				
				Iterator it = server_map.values().iterator();
				
				while( it.hasNext()){
					
					((TRTrackerServer)it.next()).removeAuthenticationListener( this );
				}
			}
		}finally{
			
			this_mon.exit();
		}
	}
}
