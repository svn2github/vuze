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

import org.gudy.azureus2.plugins.torrent.Torrent;

import com.aelitis.azureus.plugins.extseed.ExternalSeedException;
import com.aelitis.azureus.plugins.extseed.ExternalSeedPlugin;
import com.aelitis.azureus.plugins.extseed.ExternalSeedReader;
import com.aelitis.azureus.plugins.extseed.impl.ExternalSeedReaderImpl;
import com.aelitis.azureus.plugins.extseed.impl.ExternalSeedReaderRequest;
import com.aelitis.azureus.plugins.extseed.util.ExternalSeedHTTPDownloader;

public class 
ExternalSeedReaderGetRight
	extends ExternalSeedReaderImpl
{
	private static final int	TARGET_REQUEST_SIZE_DEFAULT	= 256*1024;
	
	private URL			url;
	private String		ip;
	private int			port;
	
	private ExternalSeedHTTPDownloader	http_downloader;
	
	private int			piece_size;

	private int			piece_group_size;
		
	protected
	ExternalSeedReaderGetRight(
		ExternalSeedPlugin 		_plugin,
		Torrent					_torrent,	
		URL						_url,
		Map						_params )
	{
		super( _plugin, _torrent, _params );
				
		int target_request_size	= getIntParam( _params, "req_size", TARGET_REQUEST_SIZE_DEFAULT );
		
		url		= _url;
		
		ip		= url.getHost();
		port	= url.getPort();
		
		if ( port == -1 ){
			
			port = url.getDefaultPort();
		}
		
		http_downloader  = new ExternalSeedHTTPDownloader( url, getUserAgent());
	
		piece_size = (int)getTorrent().getPieceSize();
		
		piece_group_size = target_request_size / piece_size;
		
		if ( piece_group_size == 0 ){
			
			piece_group_size	= 1;
		}
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
	
	protected int
	getPieceGroupSize()
	{
		return( piece_group_size );
	}
	
	protected boolean
	getRequestCanSpanPieces()
	{
		return( true );
	}
	
	protected void
	readData(
		ExternalSeedReaderRequest	request )
	
		throws ExternalSeedException
	{
		setReconnectDelay( RECONNECT_DEFAULT, false );
		
        try{
			http_downloader.downloadRange( 
							request.getStartPieceNumber() * piece_size + request.getStartPieceOffset(), 
							request.getLength(),
							request,
							isTransient());

        }catch( ExternalSeedException ese ){
        	
        	if ( http_downloader.getLastResponse() == 503 && http_downloader.getLast503RetrySecs() >= 0 ){
		
				int	retry_secs = http_downloader.getLast503RetrySecs();
				
				setReconnectDelay( retry_secs * 1000, true );
				
				throw( new ExternalSeedException( "Server temporarily unavailable, retrying in " + retry_secs + " seconds" ));
        		
        	}else{
        		
        		throw(ese);                	
        	}
        }
	}
}
