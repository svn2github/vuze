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
PRDownload
	extends		RPObject
	implements 	Download
{
	protected transient Download		delegate;

	public static PRDownload
	create(
		Download		_delegate )
	{
		PRDownload	res =(PRDownload)_lookupLocal( _delegate );
		
		if ( res == null ){
			
			res = new PRDownload( _delegate );
		}
		
		return( res );
	}
	
	protected
	PRDownload(
		Download		_delegate )
	{
		super( _delegate );
		
		delegate	= _delegate;
	}
	
	public void
	_setLocal()
	
		throws RPException
	{
		delegate = (Download)_fixupLocal();
	}
	
	
	public RPReply
	_process(
		RPRequest	request	)
	{
		String	method = request.getMethod();
		
		if ( method.equals( "getTorrent")){
		 
			Torrent	res = delegate.getTorrent();
			
			return( new RPReply( PRTorrent.create( res )));
		}
		
		throw( new RPException( "Unknown method: " + method ));
	}
	
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
	
	public Torrent
	getTorrent()
	{
		PRTorrent	res = (PRTorrent)dispatcher.dispatch( new RPRequest( this, "getTorrent", null )).getResponse();
	
		res._setRemote( dispatcher );
		
		return( res );		
	}
	
	public void
	initialize()
	
		throws DownloadException	
	{
		notSupported();
	}
	
	public void
	start()
	
	throws DownloadException
	{
		notSupported();
	}
	
	public void
	stop()
	
		throws DownloadException
	{
		notSupported();
	}
	
	public void
	restart()
	
		throws DownloadException
	{
		notSupported();
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
		notSupported();
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
		notSupported();
		
		return( null );
	}
	
	public DownloadScrapeResult
	getLastScrapeResult()
	{
		notSupported();
		
		return( null );
	}
	
	public DownloadStats
	getStats()
	{
		notSupported();
		
		return( null );
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