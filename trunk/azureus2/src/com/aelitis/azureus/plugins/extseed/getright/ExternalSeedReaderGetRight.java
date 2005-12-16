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
import java.util.*;

import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.peers.PeerManager;
import org.gudy.azureus2.plugins.peers.PeerReadRequest;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.utils.Monitor;
import org.gudy.azureus2.plugins.utils.Semaphore;

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
	
	private String			status;
	
	private boolean			active;
	
	private long			peer_manager_change_time;
	
	private volatile PeerManager		current_manager;
	
	private List			requests		= new LinkedList();
	private volatile int	request_count;
	private Thread			request_thread;
	private Semaphore		request_sem;
	private Monitor			requests_mon;
	
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
		request_sem		= plugin.getPluginInterface().getUtilities().getSemaphore();
		
		ip		= url.getHost();
		port	= url.getPort();
		
		if ( port == -1 ){
			
			port = url.getDefaultPort();
		}
		
		setActive( false );
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
		long now = plugin.getPluginInterface().getUtilities().getCurrentSystemTime();
		
		if ( peer_manager == current_manager ){
			
			if ( peer_manager_change_time > now ){
				
				peer_manager_change_time	= now;
			}
			
			if ( peer_manager != null ){
				
				try{
					float availability = peer_manager.getDownload().getStats().getAvailability();
		
					if ( active ){
						
						if ( availability >= 2.0 && now - peer_manager_change_time > 30000 ){
							
							plugin.log( getName() + ": deactivating as availability is good" );
							
							setActive( false );			
						}
					}else{
						
						if ( availability < 1.0 && now - peer_manager_change_time > 30000 ){
						
							plugin.log( getName() + ": activating as availability is poor" );

							setActive( true );				
						}
					}
				}catch( DownloadException e ){
					
					e.printStackTrace();
				}
			}
		}else{
			
				// if the peer manager's changed then we always go inactive for a period to wait for 
				// download status to stabilise a bit
			
			peer_manager_change_time	= now;
			
			current_manager	= peer_manager;
			
			setActive( false );
		}
		
		return( active );
	}
	
	protected void
	setActive(
		boolean		_active )
	{
		try{
			requests_mon.enter();
			
			active	= _active;
			
			status = active?"Active":"Idle";
			
		}finally{
			
			requests_mon.exit();
		}
	}
	
	public boolean
	isActive()
	{
		return( active );
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
	
	protected void
	processRequests()
	{
		try{
			requests_mon.enter();

			if ( request_thread != null ){
				
				return;
			}

			request_thread = Thread.currentThread();
			
		}finally{
			
			requests_mon.exit();
		}
		
		while( true ){
			
			try{
				if ( !request_sem.reserve(30000)){
					
					try{
						requests_mon.enter();
						
						if ( requests.size() == 0 ){
							
							request_thread	= null;
							
							break;
						}
					}finally{
						
						requests_mon.exit();
					}
				}else{
					
					PeerReadRequest	request;
					
					try{
						requests_mon.enter();

						request	= (PeerReadRequest)requests.remove(0);
						
					}finally{
						
						requests_mon.exit();
					}
					
					if ( !request.isCancelled()){
						
						processRequest( request );
					}
				}
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
		}
	}
	
	protected void
	processRequest(
		PeerReadRequest	request )
	{
		System.out.println( "process request" );
	}
	
	public void
	addRequest(
		PeerReadRequest	request )
	{
		try{
			requests_mon.enter();
			
			if ( !active ){
				
				System.out.println( "request added when not active!!!!" );
			}
					
			System.out.println( getName() + ": addRequest: " + request.getPieceNumber() + "/" + request.getOffset());
			
			requests.add( request );
			
			request_count	= requests.size();
			
			request_sem.release();
			
			if ( request_thread == null ){
				
				plugin.getPluginInterface().getUtilities().createThread(
						"RequestProcessor",
						new Runnable()
						{
							public void
							run()
							{
								processRequests();
							}
						});
			}

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
			
			System.out.println( getName() + ": cancelRequest: " + request.getPieceNumber() + "/" + request.getOffset());

			if ( requests.contains( request ) && !request.isCancelled()){
				
				request.cancel();
			
				request_count--;
			}
			
		}finally{
			
			requests_mon.exit();
		}
	}
	
	public void
	cancelAllRequests()
	{
		try{
			requests_mon.enter();
			
			System.out.println( getName() + ": cancelAllRequests" );

			for (int i=0;i<requests.size();i++){
				
				PeerReadRequest	request = (PeerReadRequest)requests.get(i);
			
				if ( !request.isCancelled()){
					
					request.cancel();
				
					request_count--;
				}
			}			
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
	
	public List
	getRequests()
	{
		List	res = null;
		
		try{
			requests_mon.enter();
			
			res = new ArrayList( requests );
			
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
