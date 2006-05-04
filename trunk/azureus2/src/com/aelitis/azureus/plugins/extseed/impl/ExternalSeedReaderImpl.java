/*
 * Created on 15-Dec-2005
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

package com.aelitis.azureus.plugins.extseed.impl;

import java.util.*;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.clientid.ClientIDGenerator;
import org.gudy.azureus2.plugins.peers.Peer;
import org.gudy.azureus2.plugins.peers.PeerManager;
import org.gudy.azureus2.plugins.peers.PeerReadRequest;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.utils.Monitor;
import org.gudy.azureus2.plugins.utils.PooledByteBuffer;
import org.gudy.azureus2.plugins.utils.Semaphore;

import com.aelitis.azureus.plugins.extseed.ExternalSeedException;
import com.aelitis.azureus.plugins.extseed.ExternalSeedPlugin;
import com.aelitis.azureus.plugins.extseed.ExternalSeedReader;
import com.aelitis.azureus.plugins.extseed.ExternalSeedReaderListener;

public abstract class 
ExternalSeedReaderImpl 
	implements ExternalSeedReader
{
	private ExternalSeedPlugin	plugin;
	private Torrent				torrent;
	
	private String			status;
	
	private boolean			active;
	private boolean			permanent_fail;
	
	private long			last_failed_read;
	private int				consec_failures;
	
	private String			user_agent;
	
	private long			peer_manager_change_time;
	
	private volatile PeerManager		current_manager;
		
	private List			requests		= new LinkedList();
	private volatile int	request_count;
	private Thread			request_thread;
	private Semaphore		request_sem;
	private Monitor			requests_mon;
	
	private List	listeners	= new ArrayList();
	
	protected
	ExternalSeedReaderImpl(
		ExternalSeedPlugin 		_plugin,
		Torrent					_torrent )	
	{
		plugin	= _plugin;
		torrent	= _torrent;
		
		requests_mon	= plugin.getPluginInterface().getUtilities().getMonitor();
		request_sem		= plugin.getPluginInterface().getUtilities().getSemaphore();
		
		PluginInterface	pi = plugin.getPluginInterface();
		
		user_agent = pi.getAzureusName();
		
		try{
			Properties	props = new Properties();
		
			pi.getClientIDManager().getGenerator().generateHTTPProperties( props );
			
			String ua = props.getProperty( ClientIDGenerator.PR_USER_AGENT );
			
			if ( ua != null ){
				
				user_agent	= ua;
			}
		}catch( Throwable e ){
		}
			
		setActive( false );
	}
	
	public Torrent
	getTorrent()
	{
		return( torrent );
	}
	
	public String
	getStatus()
	{
		return( status );
	}
	
	protected void
	log(
		String	str )
	{
		plugin.log( str );
	}
	
	protected String
	getUserAgent()
	{
		return( user_agent );
	}
	protected long
	getSystemTime()
	{
		return( plugin.getPluginInterface().getUtilities().getCurrentSystemTime());
	}
	
	protected int
	getFailureCount()
	{
		return( consec_failures );
	}
	
	protected long
	getLastFailTime()
	{
		return( last_failed_read );
	}
	
	public boolean
	isPermanentlyUnavailable()
	{
		return( permanent_fail );
	}
	
	protected abstract boolean
	readyToActivate(
		PeerManager		peer_manager,
		Peer			peer );
	
	protected abstract boolean
	readyToDeactivate(
		PeerManager		peer_manager,
		Peer			peer );
	
	public boolean
	checkActivation(
		PeerManager		peer_manager,
		Peer			peer )
	{
		long now = getSystemTime();
		
		if ( peer_manager == current_manager ){
			
			if ( peer_manager_change_time > now ){
				
				peer_manager_change_time	= now;
			}
			
			if ( peer_manager != null ){
				
				if ( active ){
					
					if ( now - peer_manager_change_time > 30000 && readyToDeactivate( peer_manager, peer )){
													
						setActive( false );			
					}
				}else{
					
					if ( !isPermanentlyUnavailable()){
					
						if ( now - peer_manager_change_time > 30000 && readyToActivate( peer_manager, peer )){
							
							setActive( true );				
						}
					}
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
	
	public void
	deactivate(
		String	reason )
	{
		plugin.log( getName() + ": deactivating (" + reason  + ")" );
		
		checkActivation( null, null );
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
						
						if ( !request.isCancelled()){
						
							request_count--;
						}
						
					}finally{
						
						requests_mon.exit();
					}
					
					if ( request.isCancelled()){
						
						informCancelled( request );

					}else{
						
						processRequest( request );
					}
				}
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
		}
	}
	
	protected abstract byte[]
	readData(
		PeerReadRequest	request )
	
		throws ExternalSeedException;
	
	protected void
	processRequest(
		PeerReadRequest	request )
	{	
		boolean	ok = false;
		
		try{
			
			byte[] data = readData( request );
			
			ok	= true;
						
			PooledByteBuffer buffer = plugin.getPluginInterface().getUtilities().allocatePooledByteBuffer( data );
			
			informComplete( request, buffer );
			
		}catch( ExternalSeedException 	e ){
			
			if ( e.isPermanentFailure()){
				
				permanent_fail	= true;
			}
			
			status = "Failed: " + Debug.getNestedExceptionMessage(e);
			
			informFailed( request );
			
		}catch( Throwable e ){
			
			request.cancel();

			status = "Failed: " + Debug.getNestedExceptionMessage(e);
			
			informFailed( request );
			
		}finally{
			
			if ( ok ){
				
				last_failed_read	= 0;
				
				consec_failures		= 0;

			}else{
				last_failed_read	= getSystemTime();
				
				consec_failures++;
			}
		}
	}
	
	public void
	addRequest(
		PeerReadRequest	request )
	{
		try{
			requests_mon.enter();
			
			if ( !active ){
				
				Debug.out( "request added when not active!!!!" );
			}
								
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
	
	protected void
	informComplete(
		PeerReadRequest		request,
		PooledByteBuffer	buffer )
	{
		for (int i=0;i<listeners.size();i++){
			
			try{
				((ExternalSeedReaderListener)listeners.get(i)).requestComplete( request, buffer );
				
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
		}		
	}
	
	protected void
	informCancelled(
		PeerReadRequest		request )
	{
		for (int i=0;i<listeners.size();i++){
			
			try{
				((ExternalSeedReaderListener)listeners.get(i)).requestCancelled( request );
				
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
		}		
	}
	
	protected void
	informFailed(
		PeerReadRequest	request )
	{
		for (int i=0;i<listeners.size();i++){
			
			try{
				((ExternalSeedReaderListener)listeners.get(i)).requestFailed( request );
				
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
		}
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
