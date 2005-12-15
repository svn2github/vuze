/*
 * Created on 15-Dec-2005
 * Created by Paul Gardner
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.plugins.extseed.getright;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.peers.PeerManager;
import org.gudy.azureus2.plugins.peers.PeerReadRequest;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.utils.Monitor;

import com.aelitis.azureus.plugins.extseed.ExternalSeedException;
import com.aelitis.azureus.plugins.extseed.ExternalSeedPlugin;
import com.aelitis.azureus.plugins.extseed.ExternalSeedReader;
import com.aelitis.azureus.plugins.extseed.ExternalSeedReaderListener;

public class 
ExternalSeedReaderGetRight 
	implements ExternalSeedReader
{
	private ExternalSeedPlugin	plugin;
	private Torrent				torrent;
	
	private URL			url;
	private String		ip;
	private int			port;
	
	private String			status			= "Unknown";
	
	private List			requests		= new ArrayList();
	private volatile int	request_count;
	
	private Monitor	requests_mon;
	
	private List	listeners	= new ArrayList();
	
	protected
	ExternalSeedReaderGetRight(
		ExternalSeedPlugin 		_plugin,
		Torrent					_torrent,	
		URL						_url )
	{
		plugin	= _plugin;
		torrent	= _torrent;
		url		= _url;
		
		requests_mon	= plugin.getPluginInterface().getUtilities().getMonitor();

		ip		= url.getHost();
		port	= url.getPort();
		
		if ( port == -1 ){
			
			port = url.getDefaultPort();
		}
	}
	
	public Torrent
	getTorrent()
	{
		return( torrent );
	}
	
	public String
	getName()
	{
		return( "GR: " + url );
	}
	
	public String
	getStatus()
	{
		return( status );
	}
	
	public boolean
	checkConnection(
		PeerManager		peer_manager )
	{
		return( peer_manager != null );
	}
	
	public boolean
	isActive()
	{
		return( true );
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
	
	public void
	addRequest(
		PeerReadRequest	request )
	{
		try{
			requests_mon.enter();
			
			System.out.println( "addRequest: " + request.getPieceNumber() + "/" + request.getOffset());
			
			requests.add( request );
			
			request_count	= requests.size();
			
		}finally{
			
			requests_mon.exit();
		}
	}
	
	public void
	cancelRequest(
		PeerReadRequest	request )
	{
		try{
			requests_mon.enter();
			
			System.out.println( "cancelRequest: " + request.getPieceNumber() + "/" + request.getOffset());

			requests.remove( request );
			
			request_count	= requests.size();
			
		}finally{
			
			requests_mon.exit();
		}
	}
	
	public void
	cancelAllRequests()
	{
		try{
			requests_mon.enter();
			
			for (int i=0;i<requests.size();i++){
				
				PeerReadRequest	request = (PeerReadRequest)requests.get(i);
			
				request.cancel();
			}
			
			requests.clear();
			
			request_count	= 0;
			
		}finally{
			
			requests_mon.exit();
		}	
	}
	
	public int
	getRequestCount()
	{
		return( request_count );
	}
	
	public List
	getExpiredRequests()
	{
		List	res = null;
		
		try{
			requests_mon.enter();
			
			for (int i=0;i<requests.size();i++){
				
				PeerReadRequest	request = (PeerReadRequest)requests.get(i);
				
				if ( request.isExpired()){
					
					if ( res == null ){
						
						res = new ArrayList();
					}
					
					res.add( request );
				}
			}			
		}finally{
			
			requests_mon.exit();
		}	
		
		return( res );
	}
	
	public void
	addListener(
		ExternalSeedReaderListener	l )
	{
		listeners.add( l );
	}
	
	public void
	removeListener(
		ExternalSeedReaderListener	l )
	{
		listeners.remove( l );
	}
}
