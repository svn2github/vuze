/*
 * File    : PRDownload.java
 * Created : 28-Jan-2004
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

package org.gudy.azureus2.ui.webplugin.remoteui.plugins.download;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.plugins.download.*;
import org.gudy.azureus2.plugins.torrent.*;

import org.gudy.azureus2.ui.webplugin.remoteui.plugins.*;
import org.gudy.azureus2.ui.webplugin.remoteui.plugins.torrent.*;


public class 
RPDownload
	extends		RPObject
	implements 	Download
{
	protected transient Download		delegate;

	protected RPTorrent					torrent;
	protected RPDownloadStats			stats;
	protected RPDownloadAnnounceResult	announce_result;
	protected RPDownloadScrapeResult	scrape_result;
	
	public static RPDownload
	create(
		Download		_delegate )
	{
		RPDownload	res =(RPDownload)_lookupLocal( _delegate );
		
		if ( res == null ){
			
			res = new RPDownload( _delegate );
		}
			
		return( res );
	}
	
	protected
	RPDownload(
		Download		_delegate )
	{
		super( _delegate );
				
		torrent = (RPTorrent)_lookupLocal( delegate.getTorrent());
		
		if ( torrent == null ){
			
			torrent = RPTorrent.create( delegate.getTorrent());
		}
		
		stats = (RPDownloadStats)_lookupLocal( delegate.getStats());
		
		if ( stats == null ){
			
			stats = RPDownloadStats.create( delegate.getStats());
		}
		
		announce_result = (RPDownloadAnnounceResult)_lookupLocal( delegate.getLastAnnounceResult());
		
		if ( announce_result == null ){
			
			announce_result = RPDownloadAnnounceResult.create( delegate.getLastAnnounceResult());
		}
		
		scrape_result = (RPDownloadScrapeResult)_lookupLocal( delegate.getLastScrapeResult());
		
		if ( scrape_result == null ){
			
			scrape_result = RPDownloadScrapeResult.create( delegate.getLastScrapeResult());
		}
	}
	
	protected void
	_setDelegate(
		Object		_delegate )
	{
		delegate = (Download)_delegate;
	}
	
	public void
	_setLocal()
	
		throws RPException
	{
		_fixupLocal();
		
		torrent._setLocal();
		
		stats._setLocal();
		
		announce_result._setLocal();
		
		scrape_result._setLocal();
	}
	
	public void
	_setRemote(
		RPRequestDispatcher		_dispatcher )
	{
		super._setRemote( _dispatcher );
		
		torrent._setRemote( _dispatcher );
		
		stats._setRemote( _dispatcher );
		
		announce_result._setRemote( _dispatcher );
		
		scrape_result._setRemote( _dispatcher );
	}
	
	public RPReply
	_process(
		RPRequest	request	)
	{
		String	method = request.getMethod();
		
		if ( method.equals( "initialize")){
			
			try{
				delegate.initialize();
				
			}catch( DownloadException e ){
				
				return( new RPReply(e));
			}
			
			return( null );
			
		}else if ( method.equals( "start")){
			
			try{
				delegate.start();
				
			}catch( DownloadException e ){
				
				return( new RPReply(e));
			}
			
			return( null );
			
		}else if ( method.equals( "restart")){
			
			try{
				delegate.restart();
				
			}catch( DownloadException e ){
				
				return( new RPReply(e));
			}
			
			return( null );
			
		}else if ( method.equals( "stop")){
			
			try{
				delegate.stop();
				
			}catch( DownloadException e ){
				
				return( new RPReply(e));
			}
			
			return( null );
			
		}else if ( method.equals( "remove")){
			
			try{
				delegate.remove();
				
			}catch( Throwable e ){
				
				return( new RPReply(e));
			}
			
			return( null );
		}
			
		throw( new RPException( "Unknown method: " + method ));
	}
	
		// ***************************************************
	
	public int
	getState()
	{
		notSupported();
		
		return(0);
	}
	

	public String
	getErrorStateDetails()
	{
		notSupported();
		
		return( null );
	}
	
	public int
	getIndex()
	{
		notSupported();
		
		return( 0 );
	}
	
	public Torrent
	getTorrent()
	{
		return( torrent );
	}
	
	public void
	initialize()
	
		throws DownloadException	
	{
		try{
			dispatcher.dispatch( new RPRequest( this, "initialize", null )).getResponse();
			
		}catch( RPException e ){
			
			if ( e.getCause() instanceof DownloadException ){
				
				throw((DownloadException)e.getCause());
			}
			
			throw( e );
		}
	}
	
	public void
	start()
	
		throws DownloadException
	{
		try{
			dispatcher.dispatch( new RPRequest( this, "start", null )).getResponse();
			
		}catch( RPException e ){
			
			if ( e.getCause() instanceof DownloadException ){
				
				throw((DownloadException)e.getCause());
			}
			
			throw( e );
		}
	}
	
	public void
	stop()
	
		throws DownloadException
	{
		try{
			dispatcher.dispatch( new RPRequest( this, "stop", null )).getResponse();
			
		}catch( RPException e ){
			
			if ( e.getCause() instanceof DownloadException ){
				
				throw((DownloadException)e.getCause());
			}
			
			throw( e );
		}
	}
	
	public void
	restart()
	
		throws DownloadException
	{
		try{
			dispatcher.dispatch( new RPRequest( this, "restart", null )).getResponse();
			
		}catch( RPException e ){
			
			if ( e.getCause() instanceof DownloadException ){
				
				throw((DownloadException)e.getCause());
			}
			
			throw( e );
		}
	}
	
	public boolean
	isStartStopLocked()
	{
		notSupported();
		
		return( false );
	}
	
	public int
	getPriority()
	{
		notSupported();
		
		return( 0 );
	}
	
	public void
	setPriority(
		int		priority )
	{
		notSupported();
	}
	
	public boolean
	isPriorityLocked()
	{
		notSupported();
		
		return( false );
	}
	
	public void
	remove()
	
		throws DownloadException, DownloadRemovalVetoException
	{
		try{
			dispatcher.dispatch( new RPRequest( this, "remove", null )).getResponse();
			
		}catch( RPException e ){
			
			Throwable cause = e.getCause();
			
			if ( cause instanceof DownloadException ){
				
				throw((DownloadException)cause);
			}
			
			if ( cause instanceof DownloadRemovalVetoException ){
				
				throw((DownloadRemovalVetoException)cause);
			}
			
			throw( e );
		}
	}
	
	public boolean
	canBeRemoved()
	
		throws DownloadRemovalVetoException
	{
		notSupported();
		
		return( false );
	}
	
	public DownloadAnnounceResult
	getLastAnnounceResult()
	{
		return( announce_result );
	}
	
	public DownloadScrapeResult
	getLastScrapeResult()
	{
		return( scrape_result );
	}
	
	public DownloadStats
	getStats()
	{
		return( stats );
	}
	
	public void
	addListener(
		DownloadListener	l )
	{
		notSupported();
	}
	
	public void
	removeListener(
		DownloadListener	l )
	{
		notSupported();
	}
	
	public void
	addTrackerListener(
		DownloadTrackerListener	l )
	{
		notSupported();
	}
	
	public void
	removeTrackerListener(
		DownloadTrackerListener	l )
	{
		notSupported();
	}
	
	public void
	addDownloadWillBeRemovedListener(
		DownloadWillBeRemovedListener	l )
	{
		notSupported();
	}
	
	public void
	removeDownloadWillBeRemovedListener(
		DownloadWillBeRemovedListener	l )
	{
		notSupported();
	}
}