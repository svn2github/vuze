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

package com.aelitis.azureus.plugins.extseed.impl.getright;

import java.util.*;
import java.net.URL;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.peers.Peer;
import org.gudy.azureus2.plugins.peers.PeerManager;
import org.gudy.azureus2.plugins.peers.PeerReadRequest;
import org.gudy.azureus2.plugins.torrent.Torrent;

import com.aelitis.azureus.plugins.extseed.ExternalSeedException;
import com.aelitis.azureus.plugins.extseed.ExternalSeedPlugin;
import com.aelitis.azureus.plugins.extseed.ExternalSeedReader;
import com.aelitis.azureus.plugins.extseed.impl.ExternalSeedReaderImpl;
import com.aelitis.azureus.plugins.extseed.util.ExternalSeedHTTPDownloader;

public class 
ExternalSeedReaderGetRight
	extends ExternalSeedReaderImpl
{
	private URL			url;
	private String		ip;
	private int			port;
	
	private ExternalSeedHTTPDownloader	http_downloader;
	
	private int			min_availability;
	private int			min_speed;
	private long		valid_until;
	
	protected
	ExternalSeedReaderGetRight(
		ExternalSeedPlugin 		_plugin,
		Torrent					_torrent,	
		URL						_url,
		Map						_params )
	{
		super( _plugin, _torrent );
				
		min_availability 	= getIntParam( _params, "min_avail", 1 );	// default is avail based
		min_speed			= getIntParam( _params, "min_speed", 0 );
		valid_until			= getIntParam( _params, "valid_ms", 0 );
		
		if ( valid_until > 0 ){
			
			valid_until += getSystemTime();
		}
		
		url		= _url;
		
		ip		= url.getHost();
		port	= url.getPort();
		
		if ( port == -1 ){
			
			port = url.getDefaultPort();
		}
		
		http_downloader  = new ExternalSeedHTTPDownloader( url, getUserAgent());
		
	}
	
	public boolean
	sameAs(
		ExternalSeedReader	other )
	{
		if ( other instanceof ExternalSeedReaderGetRight ){
			
			return( url.toString().equals(((ExternalSeedReaderGetRight)other).url.toString()));
		}
		
		return( false );
	}
	
	protected int
	getIntParam(
		Map			map,
		String		name,
		int			def )
	{
		Object	obj = map.get(name);
		
		if ( obj instanceof Long ){
			
			return(((Long)obj).intValue());
		}
		
		return( def );
	}
	
	public String
	getName()
	{
		return( "GR: " + url );
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
			
			int	delay	= 30000;
			
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
			if ( valid_until > 0 && getSystemTime() > valid_until ){
				
				return( false );
			}
			
			if ( peer_manager.getDownload().getState() == Download.ST_SEEDING ){
				
				return( false );
			}
						
			if ( min_availability > 0 ){
				
				float availability = peer_manager.getDownload().getStats().getAvailability();
			
				if ( availability < min_availability){
				
					log( getName() + ": activating as availability is poor" );
					
					return( true );
				}
			}
				
			if ( min_speed > 0 ){
				
				if ( peer_manager.getStats().getDownloadAverage() < min_speed ){
					
					log( getName() + ": activating as speed is slow" );
					
					return( true );
				}
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
			if ( valid_until > 0 && getSystemTime() > valid_until ){
				
				return( true );
			}
			
			if ( peer_manager.getDownload().getState() == Download.ST_SEEDING ){
				
				return( true );
			}
		
			if ( min_availability > 0 ){

				float availability = peer_manager.getDownload().getStats().getAvailability();
			
				if ( availability >= min_availability + 1 ){
				
					log( getName() + ": deactivating as availability is good" );
				
					return( true );
				}
			}
			
			if ( min_speed > 0 ){
				
				long	my_speed 		= peer.getStats().getDownloadAverage();
				
				long	overall_speed 	= peer_manager.getStats().getDownloadAverage();
				
				if ( overall_speed - my_speed > 2 * min_speed ){
					
					log( getName() + ": deactivating as speed is good" );

					return( true );
				}
				
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
		return( http_downloader.downloadRange( 
						request.getPieceNumber() * getTorrent().getPieceSize() + request.getOffset(), 
						request.getLength()));
	}
}
