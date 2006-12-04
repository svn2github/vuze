/*
 * Created on 1 Nov 2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */


package com.aelitis.azureus.core.download;

import java.util.List;

import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerPiece;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerPeerListener;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.peermanager.piecepicker.PiecePicker;
import com.aelitis.azureus.core.peermanager.piecepicker.PiecePriorityProvider;
import com.aelitis.azureus.core.peermanager.piecepicker.PieceRTAProvider;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;

public class 
EnhancedDownloadManager 
{
	public static final int	MINIMUM_INITIAL_BUFFER_SECS	= 30;
	
	private DownloadManagerEnhancer		enhancer;
	private DownloadManager				download_manager;
	
	private transient PiecePicker		current_piece_pickler;
	
	private long		last_eta_result	= Long.MAX_VALUE;
	private long		last_eta_time;
	
	private boolean	progressive_active	= false;
	
	private long	content_bps;
	private int		initial_buffer_pieces;
	
	private bufferETAProvider	buffer_provider	= new bufferETAProvider();
	private boostETAProvider	boost_provider	= new boostETAProvider();
	
	protected
	EnhancedDownloadManager(
		DownloadManagerEnhancer		_enhancer,
		DownloadManager				_download_manager )
	{
		enhancer			= _enhancer;
		download_manager	= _download_manager;
		
		TOTorrent	torrent = download_manager.getTorrent();
				
		if ( torrent != null ){
			
			content_bps = PlatformTorrentUtils.getContentSpeedBps( torrent );
			
			if ( content_bps == 0 ){
			
					// hack in some test values for torrents that don't have a bps in them yet
				
				long	size = torrent.getSize();
				
				if ( size < 200*1024*1024 ){
				
					content_bps = 30*1024;
					
				}else if ( size < 1000*1024*1024L ){
					
					content_bps = 200*1024;
					
				}else{

					content_bps = 400*1024;
				}
			}
			
			long	initial_bytes = MINIMUM_INITIAL_BUFFER_SECS * content_bps;
	
			initial_buffer_pieces = (int)( initial_bytes / torrent.getPieceLength());
			
			initial_buffer_pieces = Math.min( initial_buffer_pieces, torrent.getNumberOfPieces());
			
			// setProgressiveMode( true );
		}
		
		download_manager.addPeerListener(
			new DownloadManagerPeerListener()
			{
				public void
				peerManagerAdded(
					PEPeerManager	manager )
				{
					synchronized( this ){
					
						current_piece_pickler = manager.getPiecePicker();
						
						if ( progressive_active && current_piece_pickler != null ){
							
							buffer_provider.activate( current_piece_pickler );
							
							boost_provider.activate( current_piece_pickler );
						}
					}
				}
				
				public void
				peerManagerRemoved(
					PEPeerManager	manager )
				{
					synchronized( this ){

						if ( current_piece_pickler != null ){
					
							buffer_provider.deactivate(  current_piece_pickler );
							
							current_piece_pickler	= null;	
						}
					}
				}
				
				public void
				peerAdded(
					PEPeer 	peer )
				{
				}
					
				public void
				peerRemoved(
					PEPeer	peer )
				{
				}
					
				public void
				pieceAdded(
					PEPiece 	piece )
				{
				}
					
				public void
				pieceRemoved(
					PEPiece		piece )
				{
				}
			});
	}

	public boolean
	supportsProgressiveMode()
	{
		TOTorrent	torrent = download_manager.getTorrent();
		
		if ( torrent == null ){
			
			return( false );
		}
		
		return( content_bps > 0 && enhancer.isProgressiveAvailable() && PlatformTorrentUtils.isContentProgressive( torrent ));
	}
	
	public void
	setProgressiveMode(
		boolean		active )
	{
		TOTorrent	torrent = download_manager.getTorrent();
		
		if ( torrent == null ){

			return;
		}
		
		synchronized( this ){

			if ( progressive_active== active ){
				
				return;
			}
			
			if ( current_piece_pickler != null ){

				progressive_active	= active;
		
				if ( progressive_active ){
					
					buffer_provider.activate( current_piece_pickler );
					
					boost_provider.activate( current_piece_pickler );
					
				}else{
					
					buffer_provider.deactivate( current_piece_pickler );
					
					boost_provider.deactivate( current_piece_pickler );
				}
			}
		}
	}
	
	public boolean
	getProgressiveMode()
	{
		return( progressive_active );
	}
	
	public long
	getProgressivePlayETA()
	{
		long	now = SystemTime.getCurrentTime();
		
		if ( 	now > last_eta_time &&
				now - last_eta_time < 1000 ){
			
			return( last_eta_result );
		}
		
		long	dl_rate = download_manager.getStats().getDataReceiveRate();
		
		long	result	= Long.MAX_VALUE;
				
		DiskManager	disk_manager = download_manager.getDiskManager();
		
		if ( dl_rate > 0 && content_bps > 0 && disk_manager != null ){
				
			PiecePicker	picker = current_piece_pickler;
			
			if ( picker != null ){
			
				List	providers = picker.getRTAProviders();
				
				long	max_cp	= 0;
				long	max_bp	= 0;
				
				for (int i=0;i<providers.size();i++){
					
					PieceRTAProvider	provider = (PieceRTAProvider)providers.get(i);
					
					long	cp = provider.getCurrentPosition();
					
					if ( cp >= max_cp ){
						
						max_cp	= cp;
						max_bp	= provider.getBlockingPosition();
					}
				}
				
					// max-cp 	= current streaming position
					// max-bp	= blocking position (i.e. first missing data after max-cp)
					
				long	secs_pos			= max_cp/content_bps;
				
				long	secs_to_watch 		= ( disk_manager.getTotalLength() - max_cp )/ content_bps;
				
				long	secs_to_download	= disk_manager.getRemainingExcludingDND() / dl_rate;
				
				result = secs_to_download - secs_to_watch;
				
				//System.out.println( "Stream readyness: watch=" + secs_to_watch + " (pos=" + secs_pos + "), dl=" + secs_to_download + ",wait=" + result );
			}
		}
		
		last_eta_result	= result;
		last_eta_time	= now;
		
		return( result );
	}
	
	protected class
	bufferETAProvider
		implements PieceRTAProvider
	{
		private long[]		piece_rtas;
		
		protected void
		activate(	
			PiecePicker		picker )
		{
			synchronized( EnhancedDownloadManager.this ){

				piece_rtas = new long[ picker.getNumberOfPieces()];
				
				long	now = SystemTime.getCurrentTime();
				
				for (int i=0;i<initial_buffer_pieces;i++){
					
					piece_rtas[i] = now+i;
				}

				picker.addRTAProvider( this );
			}
		}
		
		protected void
		deactivate(
			PiecePicker		picker )
		{
			synchronized( EnhancedDownloadManager.this ){
									
				picker.removeRTAProvider( this );
				
				piece_rtas	= null;
			}
		}
		
		public long[]
    	updateRTAs(
    		PiecePicker		picker )
    	{
    		DiskManager	dm = download_manager.getDiskManager();

    		if ( dm != null ){

    			DiskManagerPiece[]	pieces = dm.getPieces();
    			
    			boolean	all_done = true;
    			
    			for (int i=0;i<initial_buffer_pieces;i++){
    				
    				if ( !pieces[i].isDone()){
    						
    					all_done = false;
    					
    					break;
    				}
    			}
    			
    			if ( all_done ){
    				
    				deactivate( picker );
    			}
    		}
    		
    		return( piece_rtas );
    	}
    	
    	public long
    	getCurrentPosition()
    	{
    		return( 0 );
    	}
    	
    	public long
    	getBlockingPosition()
    	{
    		DiskManager	dm = download_manager.getDiskManager();
    		
    		if ( dm == null ){
    			
    			return( 0 );
    		}
    		
    		DiskManagerPiece[]	pieces = dm.getPieces();
    		
    		for (int i=0;i<pieces.length;i++){
    			
    			DiskManagerPiece	piece = pieces[i];
    			
    			if ( !piece.isDone()){
    				
    				long	complete = i*dm.getPieceLength();
    				
    				boolean[] written = piece.getWritten();
    				
    				if ( written == null ){
    					
    					complete += piece.getLength();
    					
    				}else{
    					
    					for (int j=0;j<written.length;j++){
    						
    						if ( written[j] ){
    							
    							complete += piece.getBlockSize( j );
    						}
    						
    						break;
    					}
    				}
    				
    				return( complete );
    			}
    		}
    		
    		return( dm.getTotalLength());
    	}
	}
	
	protected class
	boostETAProvider
		implements PiecePriorityProvider
	{
		private long[]		piece_priorities;
		
		private long		last_recalc;
		
		protected void
		activate(	
			PiecePicker		picker )
		{
			synchronized( EnhancedDownloadManager.this ){
				
				picker.addPriorityProvider( this );
			}
		}
		
		protected void
		deactivate(
			PiecePicker		picker )
		{
			synchronized( EnhancedDownloadManager.this ){
									
				picker.removePriorityProvider( this );
				
				piece_priorities	= null;
			}
		}
		
		public long[]
    	updatePriorities(
    		PiecePicker		picker )
    	{
			long	now = SystemTime.getCurrentTime();
			
			if ( now < last_recalc || now - last_recalc > 5000 ){
				
				last_recalc	= now;
				
				long	stream_delay = getProgressivePlayETA();
				
				DiskManager	disk_manager = download_manager.getDiskManager();

				if ( stream_delay <= 0 || disk_manager == null ){
					
					piece_priorities = null;
					
				}else{
					
					long	dl_rate = download_manager.getStats().getDataReceiveRate();

					if ( dl_rate > 0 && content_bps > 0 ){
							
							// boost assumes streaming from start
						
						long	secs_to_watch 		= disk_manager.getTotalLength()/ content_bps;
						
						long	secs_to_download	= disk_manager.getRemainingExcludingDND() / dl_rate;
						
						long 	delay = secs_to_download - secs_to_watch;
						
						if ( delay <= 0 ){
							
							piece_priorities = null;
							
						}else{
							
							long	bytes_to_boost = delay * content_bps;
							
							long	pieces_to_boost = (bytes_to_boost + disk_manager.getPieceLength()-1)/ disk_manager.getPieceLength();
							
							int	num_pieces = picker.getNumberOfPieces();

							if ( pieces_to_boost >= num_pieces ){
								
									// no point in boosting entire thing
								
								// System.out.println("not boosting, too many pieces" );
								
								piece_priorities	= null;
								
							}else{
								
								// System.out.println( "boosting " + pieces_to_boost );
								
								piece_priorities = new long[num_pieces];

								for (int i=0;i<pieces_to_boost;i++){
									
									piece_priorities[i] = 20000;
								}
							}
						}
					}else{
						
						piece_priorities = null;
					}
				}
			}
    		
    		return( piece_priorities );
    	}
	}
}
