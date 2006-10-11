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
import org.gudy.azureus2.plugins.peers.Piece;
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
	private Thread			request_thread;
	private Semaphore		request_sem;
	private Monitor			requests_mon;
	
	private int[]		priority_offsets;
	
	private boolean		transient_seed;
	
	private List	listeners	= new ArrayList();
	
	protected
	ExternalSeedReaderImpl(
		ExternalSeedPlugin 		_plugin,
		Torrent					_torrent,
		Map						_params )
	{
		plugin	= _plugin;
		torrent	= _torrent;
		
		transient_seed		= getBooleanParam( _params, "transient", false );

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
	
	public boolean
	isTransient()
	{
		return( transient_seed );
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
					
					List			selected_requests 	= new ArrayList();
					PeerReadRequest	cancelled_request	= null;
					
					try{
						requests_mon.enter();

							// get an advisory set to process together
						
						int	count = selectRequests( requests );
						
						if ( count <= 0 || count > requests.size()){
							
							Debug.out( "invalid count" );
							
							count	= 1;
						}
						
						for (int i=0;i<count;i++){
							
							PeerReadRequest	request = (PeerReadRequest)requests.remove(0);
							
							if ( request.isCancelled()){
								
									// if this is the first request then process it, otherwise leave
									// for the next-round
															
								if ( i == 0 ){
									
									cancelled_request = request;
									
								}else{
									
									requests.add( 0, request );
								}
								
								break;
								
							}else{
								
								selected_requests.add( request );
																
								if ( i > 0 ){
								
										// we've only got the sem for the first request, catch up for subsequent
									
									request_sem.reserve();
								}
							}
						}
						
					}finally{
						
						requests_mon.exit();
					}
					
					if ( cancelled_request != null ){
						
						informCancelled( cancelled_request );

					}else{
						
						processRequests( selected_requests );
					}
				}
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
		}
	}

	public int
	getMaximumNumberOfRequests()
	{
		if ( getRequestCount() == 0 ){
			
			return((int)(( getPieceGroupSize() * torrent.getPieceSize() ) / PeerReadRequest.NORMAL_REQUEST_SIZE ));
			
		}else{
			
			return( 0 );
		}
	}

	public void
	calculatePriorityOffsets(
		PeerManager		peer_manager,
		int[]			base_priorities )
	{
		try{
			Piece[]	pieces = peer_manager.getPieces();
			
			int	piece_group_size = getPieceGroupSize();
			
			int[]	contiguous_best_pieces = new int[piece_group_size];
			int[]	contiguous_highest_pri = new int[piece_group_size];
					
			Arrays.fill( contiguous_highest_pri, -1 );
			
			int	contiguous			= 0;
			int	contiguous_best_pri	= -1;
			
			int	max_contiguous	= 0;
			
			int	max_free_reqs		= 0;
			int max_free_reqs_piece	= -1;
			
			for (int i=0;i<pieces.length;i++){
				
				Piece	piece = pieces[i];
				
				if ( piece.isFullyAllocatable()){
			
					contiguous++;
					
					int	base_pri = base_priorities[i];
					
					if ( base_pri > contiguous_best_pri ){
						
						contiguous_best_pri	= base_pri;
					}
					
					for (int j=0;j<contiguous && j<contiguous_highest_pri.length;j++){
						
						if ( contiguous_best_pri > contiguous_highest_pri[j] ){
							
							contiguous_highest_pri[j]	= contiguous_best_pri;
							contiguous_best_pieces[j]	= i - j;
						}
						
						if ( j+1 > max_contiguous ){
								
							max_contiguous	= j+1;
						}
					}
		
				}else{
					
					contiguous			= 0;
					contiguous_best_pri	= -1;
					
					if ( max_contiguous == 0 ){
						
						int	free_reqs = piece.getAllocatableRequestCount();
						
						if ( free_reqs > max_free_reqs ){
							
							max_free_reqs 		= free_reqs;
							max_free_reqs_piece	= i;
						}
					}
				}
			}
					
			if ( max_contiguous == 0 ){
			
				if ( max_free_reqs_piece >= 0 ){
					
					priority_offsets	 = new int[ (int)getTorrent().getPieceCount()];

					priority_offsets[max_free_reqs_piece] = 10000;
					
				}else{
					
					priority_offsets	= null;
				}
			}else{
				
				priority_offsets	 = new int[ (int)getTorrent().getPieceCount()];
				
				int	start_piece = contiguous_best_pieces[max_contiguous-1];
				
				for (int i=start_piece;i<start_piece+max_contiguous;i++){
								
					priority_offsets[i] = 10000 - (i-start_piece);
				}
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
			
			priority_offsets	= null;
		}
	}
	
	protected abstract int
	getPieceGroupSize();
	
	protected abstract boolean
	getRequestCanSpanPieces();
	
	public int[]
	getPriorityOffsets()
	{
		return( priority_offsets );
	}
	
	protected int
	selectRequests(
		List	requests )
	{
		long	next_start = -1;
		
		int	last_piece_number = -1;
		
		for (int i=0;i<requests.size();i++){
			
			PeerReadRequest	request = (PeerReadRequest)requests.get(i);
			
			int	this_piece_number	= request.getPieceNumber();
			
			if ( last_piece_number != -1 && last_piece_number != this_piece_number ){
				
				if ( !getRequestCanSpanPieces()){
					
					return( i );
				}
			}
			
			long	this_start = this_piece_number * torrent.getPieceSize() + request.getOffset();
			
			if ( next_start != -1 && this_start != next_start ){
				
				return(i);
			}
			
			next_start	= this_start + request.getLength();
			
			last_piece_number	= this_piece_number;
		}
		
		return( requests.size());
	}

	protected abstract void
	readData(
		ExternalSeedReaderRequest	request )
	
		throws ExternalSeedException;
	
	protected void
	processRequests(
		List		requests )
	{	
		boolean	ok = false;
				
		ExternalSeedReaderRequest	request = new ExternalSeedReaderRequest( this, requests );
		
		try{
			
			readData( request );
													
			ok	= true;

		}catch( ExternalSeedException 	e ){
			
			if ( e.isPermanentFailure()){
				
				permanent_fail	= true;
			}
			
			status = "Failed: " + Debug.getNestedExceptionMessage(e);
			
			request.failed();
			
		}catch( Throwable e ){
			
			status = "Failed: " + Debug.getNestedExceptionMessage(e);
				
			request.failed();
			
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
	addRequests(
		List	new_requests )
	{
		try{
			requests_mon.enter();
			
			if ( !active ){
				
				Debug.out( "request added when not active!!!!" );
			}
				
			for (int i=0;i<new_requests.size();i++){
			
				requests.add( new_requests.get(i));

				request_sem.release();
			}
						
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
				}
			}			
		}finally{
			
			requests_mon.exit();
		}	
	}
	
	public int
	getRequestCount()
	{
		try{
			requests_mon.enter();

			return( requests.size());
			
		}finally{
			
			requests_mon.exit();
		}	
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
		byte[]				buffer )
	{
		PooledByteBuffer pool_buffer = plugin.getPluginInterface().getUtilities().allocatePooledByteBuffer( buffer );
		
		for (int i=0;i<listeners.size();i++){
			
			try{
				((ExternalSeedReaderListener)listeners.get(i)).requestComplete( request, pool_buffer );
				
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
	
	protected boolean
	getBooleanParam(
		Map			map,
		String		name,
		boolean		def )
	{
		return( getIntParam( map, name, def?1:0) != 0 );
	}
}
