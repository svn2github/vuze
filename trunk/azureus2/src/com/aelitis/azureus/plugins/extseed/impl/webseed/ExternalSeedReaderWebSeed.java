/*
 * Created on 16-Dec-2005
 * Created by Paul Gardner
 * Copyright (C) 2005, 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.plugins.extseed.impl.webseed;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.peers.Peer;
import org.gudy.azureus2.plugins.peers.PeerManager;
import org.gudy.azureus2.plugins.peers.PeerReadRequest;
import org.gudy.azureus2.plugins.torrent.Torrent;

import com.aelitis.azureus.plugins.extseed.ExternalSeedException;
import com.aelitis.azureus.plugins.extseed.ExternalSeedPlugin;
import com.aelitis.azureus.plugins.extseed.impl.ExternalSeedReaderImpl;
import com.aelitis.azureus.plugins.extseed.util.ExternalSeedHTTPDownloader;

public class 
ExternalSeedReaderWebSeed
	extends ExternalSeedReaderImpl
{
	private static final int	RECONNECT_DEFAULT = 30*1000;
	
	private URL			url;
	private String		ip;
	private int			port;
	private String		url_prefix;
	
	private int		reconnect_delay	= RECONNECT_DEFAULT;
	
	protected
	ExternalSeedReaderWebSeed(
		ExternalSeedPlugin 		_plugin,
		Torrent					_torrent,	
		URL						_url )
	{
		super( _plugin, _torrent );
		
		url		= _url;
		
		ip		= url.getHost();
		port	= url.getPort();
		
		if ( port == -1 ){
			
			port = url.getDefaultPort();
		}
		
		try{
			String hash_str = URLEncoder.encode(new String(_torrent.getHash(), "ISO-8859-1"), "ISO-8859-1").replaceAll("\\+", "%20");

			url_prefix = url.toString()+"?info_hash=" + hash_str;
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
	
	public String
	getName()
	{
		return( "WS: " + url );
	}
	
	public String
	getIP()
	{
		return( ip );
	}
	
	public int
	getPort()
	{
		return( port );
	}
	
	protected boolean
	readyToActivate(
		PeerManager	peer_manager,
		Peer		peer )
	{
		int	fail_count = getFailureCount();
		
		if ( fail_count > 0 ){
			
			int	delay	= reconnect_delay;
			
			for (int i=1;i<fail_count;i++){
				
				delay += delay;
				
				if ( delay > 30*60*1000 ){
					
					break;
				}
			}
			
			long	now = getSystemTime();
			
			long	last_fail = getLastFailTime();
			
			if ( last_fail < now && now - last_fail < delay ){
				
				return( false );
			}
		}
		
		try{
			float availability = peer_manager.getDownload().getStats().getAvailability();
			
			if ( availability < 1.0 ){
				
				log( getName() + ": activating as availability is poor" );
				
				return( true );
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
		
		return( false );		
	}
	
	protected boolean
	readyToDeactivate(
		PeerManager	peer_manager,
		Peer		peer )
	{
		try{
			float availability = peer_manager.getDownload().getStats().getAvailability();
			
			if ( availability >= 2.0 ){
				
				log( getName() + ": deactivating as availability is good" );
				
				return( true );
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
		
		return( false );
	}
	

	
	protected byte[]
	readData(
		PeerReadRequest	request )
	
		throws ExternalSeedException
	{		
		long	piece = request.getPieceNumber();
		
		long	piece_start = request.getOffset();
		long	piece_end	= piece_start + request.getLength()-1;
			
		String	str = url_prefix + "&piece=" + piece + "&ranges=" + piece_start + "-" + piece_end;
		
		reconnect_delay	= RECONNECT_DEFAULT;
		
		try{
			ExternalSeedHTTPDownloader	http_downloader = new ExternalSeedHTTPDownloader( new URL( str ), getUserAgent());

			byte[]	data = http_downloader.downloadSocket(request.getLength());
			
			if ( http_downloader.getLastResponse() == 503 ){
				
				Integer	retry = new Integer( new String(data));
				
				reconnect_delay = retry.intValue() * 1000;
				
				throw( new ExternalSeedException( "Server temporarily unavailable, retrying in " + retry ));
				
			}else{
						
				return( data );
			}
		}catch( MalformedURLException e ){
			
			throw( new ExternalSeedException( "URL encode fails", e ));
		}
	}
}
