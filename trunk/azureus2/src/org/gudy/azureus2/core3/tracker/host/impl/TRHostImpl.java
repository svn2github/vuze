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

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.tracker.host.*;
import org.gudy.azureus2.core3.tracker.server.*;
import org.gudy.azureus2.core3.tracker.client.*;
import org.gudy.azureus2.core3.torrent.*;

public class 
TRHostImpl
	implements TRHost, TRTrackerClientFactoryListener, TRTrackerServerListener
{
	public static final int DEFAULT_PORT	= 80;	// port to use if none in announce URL

	protected static final String	NL			= "\r\n";
	
	protected static TRHostImpl		singleton;
	
	protected TRHostConfigImpl		config;
		
	protected Hashtable				server_map 	= new Hashtable();
	
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
		config = new TRHostConfigImpl(this);	
		
		TRTrackerClientFactory.addListener( this );
	}
	
	public void
	initialise(
		TRHostTorrentFinder	finder )
	{
		config.loadConfig( finder );
	}

	public synchronized void
	addTorrent(
		TOTorrent		torrent )
		
		throws TRHostException
	{
		addTorrent( torrent, TRHostTorrent.TS_STARTED );
	}
	
	public synchronized void
	addTorrent(
		TOTorrent		torrent,
		int				state )
		
		throws TRHostException
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
			
				server = TRTrackerServerFactory.create( port );
			
				server_map.put( new Integer( port ), server );
				
				server.addListener( this );
				
			}catch( TRTrackerServerException e ){
				
				throw( new TRHostException( e.getMessage()));
			}
		}
		
		TRHostTorrentImpl host_torrent = new TRHostTorrentImpl( this, server, torrent, port );
		
		host_torrents.add( host_torrent );
		host_torrent_map.put( torrent, host_torrent );
		
		startHosting( host_torrent );
		
		if ( state == TRHostTorrent.TS_STARTED ){
					
			host_torrent.start();
		}

		for (int i=0;i<listeners.size();i++){
			
			((TRHostListener)listeners.get(i)).torrentAdded( host_torrent );
		}
		
		config.saveConfig();
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
		
		config.saveConfig();		
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
	
	public boolean
	handleExternalRequest(
		String			url,
		OutputStream	os )
		
		throws IOException
	{
		if ( !COConfigurationManager.getBooleanParameter( "Tracker Publish Enable", true )){
			
			return( false );
		}
		
		byte[]		reply_bytes		= null;
		String		content_type	= "text/html";
		String		reply_status	= "200";
		
		try{
			if ( url.equals( "/" )){	
				
				String	reply_string = 
				"<html>" +
				"<head>" +
				"<title> Azureus : Java BitTorrent Client Tracker</title>" + 
				"<META HTTP-EQUIV=\"Pragma\" CONTENT=\"no-cache\">" +
				"<META HTTP-EQUIV=\"Expires\" CONTENT=\"-1\">" +
				"<meta name=\"keywords\" content=\"BitTorrent, bt, java, client, azureus\">" +
				"<link rel=\"stylesheet\" href=\"http://azureus.sourceforge.net/style.css\" type=\"text/css\">" +
				"</head>" +
				"<body>" +
				//"<table align=\"center\" class=\"body\" cellpadding=\"0\" cellspacing=\"0\">" + 
				//"<tr><td>" +
				"<table align=\"center\" class=\"main\" cellpadding=\"0\" cellspacing=\"0\">" +
				"<tr><td>" +
				"<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" bordercolor=\"#111111\" width=\"100%\">" +
				"  <tr>" +
				"	<td><a href=\"http://azureus.sourceforge.net/\"><img src=\"http://azureus.sourceforge.net/img/Azureus_banner.gif\" border=\"0\" alt=\"Azureus\" hspace=\"0\" width=\"100\" height=\"40\" /></a></td>" +
				"	<td><p align=\"center\"><font size=\"5\">Azureus: BitTorrent Client Tracker</font></td>" +
				"  </tr>" +
				"</table>" +
				"<table align=\"center\" class=\"main1\" bgcolor=\"#526ED6\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">" +
				"<tr><td valign=\"top\" height=\"20\"></td></tr>" +
				"<tr>" +
				"  <td valign=\"top\">"+
				"   <table align=\"center\" border=\"1\" cellpadding=\"2\" cellspacing=\"1\" bordercolor=\"#111111\" width=\"96%\" bgcolor=\"#D7E0FF\">" +
				"   <thead>" +
				"     <tr>" +
				"	    <td width=\"50%\" bgcolor=\"#FFDEAD\">Torrent</td>" +
				"	    <td width=\"10%\" bgcolor=\"#FFDEAD\">Status</td>" +
				"	    <td width=\"10%\" bgcolor=\"#FFDEAD\">Size</td>" +
				"	    <td width=\"10%\" bgcolor=\"#FFDEAD\">Seeds</td>" +
				"	    <td width=\"10%\" bgcolor=\"#FFDEAD\">Peers</td>" +
				"	  </tr>" +
				"    </thread>";
				
				StringBuffer	table_bit = new StringBuffer(1024);
				
				synchronized( this ){
				
						for (int i=0;i<host_torrents.size();i++){
					
							TRHostTorrentImpl	host_torrent = (TRHostTorrentImpl)host_torrents.get(i);
						
							TOTorrent	torrent = host_torrent.getTorrent();
							
							String	hash_str = URLEncoder.encode( new String( torrent.getHash(), Constants.BYTE_ENCODING ), Constants.BYTE_ENCODING );
							
							String	torrent_name = new String(torrent.getName());
							
							TRHostPeer[]	peers = host_torrent.getPeers();
							
							int	seed_count 		= 0;
							int non_seed_count	= 0;
							
							for (int j=0;j<peers.length;j++){
								
								if ( peers[j].isSeed()){
									
									seed_count++;
									
								}else{
									
									non_seed_count++;
								}
							}
							
							int	status = TRHostTorrent.TS_STARTED;
							
							String	status_str;
							
							if ( status == TRHostTorrent.TS_STARTED ){

								status_str = "Running";
								
							}else if ( status == TRHostTorrent.TS_STOPPED ){
								
								status_str = "Stopped";
								
							}else{
								
								status_str = "Failed";
							}
							
							table_bit.append( "<tr>" );
							
							table_bit.append( "<td width=\"50%\">"+
											  "<a href=\"/torrents/" + torrent_name.replace('?','_') + ".torrent?" + hash_str + "\">" + torrent_name + "</a></td>" );
											  
							table_bit.append( "<td width=\"10%\">" + status_str + "</td>" );
											  
							table_bit.append( "<td width=\"10%\">" + 
											  DisplayFormatters.formatByteCountToKBEtc( torrent.getSize()) + "</td>" );
											  
							table_bit.append( "<td width=\"10%\">" + 
											  seed_count + "</td>" );
											  
							table_bit.append( "<td width=\"10%\">" + 
											  non_seed_count + "</td>" );
											  
							table_bit.append( "</tr>" );
						}	
					}
				
				reply_string += table_bit;
				
				reply_string +=
				"    </table>" +
				"    <tr><td>&nbsp;</tr></td>" +
				"  </td>" +
				"</tr>" +
				"</table>" +
				"</td></tr>" +
				"</table>" +
				//"</td></tr>" +
				//"</table>" +
				"</body>" +
				"</html>";
						
				reply_bytes = reply_string.getBytes();
								
			}else if ( url.startsWith( "/torrents/")){
				
				String	str = url.substring(10);
				
				int	pos = str.indexOf ( "?" );
				
				String	hash_str = str.substring(pos+1);
				
				byte[]	hash = URLDecoder.decode( hash_str, Constants.BYTE_ENCODING ).getBytes( Constants.BYTE_ENCODING );
				
				synchronized( this ){
				
					for (int i=0;i<host_torrents.size();i++){
					
						TRHostTorrentImpl	host_torrent = (TRHostTorrentImpl)host_torrents.get(i);
						
						TOTorrent	torrent = host_torrent.getTorrent();
						
						if ( Arrays.equals( hash, torrent.getHash())){
						
							reply_bytes = BEncoder.encode(torrent.serialiseToMap());
							
							break;
						}
					}
				}
				
				if ( reply_bytes != null ){
					
					content_type 	= "application/x-bittorrent";
					
				}else{
					System.out.println( "Torrent not found at '" + url + "'" );
					
					reply_bytes = new byte[0];
				
					reply_status	= "404";
				}
			}else{
					
				System.out.println( "Invalid url '" + url + "'" );
							
				reply_bytes = new byte[0];
				
				reply_status	= "404";
				
			}
		}catch( Throwable e ){
		
			content_type = "text/html";
			
			reply_bytes = e.toString().getBytes();
			
			reply_status = "200";	
		}
		
		String reply_header = 
						"HTTP/1.1 " + reply_status + (reply_status.equals( "200" )?" OK":" BAD") + NL + 
						"Content-Type: " + content_type + NL +
						"Content-Length: " + reply_bytes.length + NL +
						NL;
	
		os.write( reply_header.getBytes());
				
		os.flush();
		
		os.write( reply_bytes );
		
		os.flush();
		
		return( true );
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
