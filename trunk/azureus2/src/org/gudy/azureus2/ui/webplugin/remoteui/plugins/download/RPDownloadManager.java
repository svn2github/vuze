/*
 * File    : PRDownloadManager.java
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

import java.io.*;
import java.net.*;

import org.gudy.azureus2.plugins.download.*;
import org.gudy.azureus2.plugins.torrent.*;

import org.gudy.azureus2.ui.webplugin.remoteui.plugins.*;
import org.gudy.azureus2.ui.webplugin.remoteui.plugins.torrent.*;

public class 
RPDownloadManager
	extends		RPObject
	implements 	DownloadManager
{
	protected transient DownloadManager		delegate;
	
	public static RPDownloadManager
	create(
		DownloadManager		_delegate )
	{
		RPDownloadManager	res =(RPDownloadManager)_lookupLocal( _delegate );
		
		if ( res == null ){
			
			res = new RPDownloadManager( _delegate );
		}
		
		return( res );
	}
	
	protected
	RPDownloadManager(
		DownloadManager		_delegate )
	{
		super( _delegate );
	}
	
	protected void
	_setDelegate(
		Object		_delegate )
	{
		delegate = (DownloadManager)_delegate;
	}
	
	public Object
	_setLocal()
	
		throws RPException
	{
		return( _fixupLocal());
	}
	
	
	public RPReply
	_process(
		RPRequest	request	)
	{
		String	method = request.getMethod();
		
		if ( method.equals( "getDownloads")){
			
			Download[]	downloads = delegate.getDownloads();
			
			RPDownload[]	res = new RPDownload[downloads.length];
			
			for (int i=0;i<res.length;i++){
				
				res[i] = RPDownload.create( downloads[i]);
			}
			
			return( new RPReply( res ));
			
		}else if ( method.equals( "getDownloads[bSort]")){
			
			Download[]	downloads = delegate.getDownloads(((Boolean)request.getParams()).booleanValue());
			
			RPDownload[]	res = new RPDownload[downloads.length];
			
			for (int i=0;i<res.length;i++){
				
				res[i] = RPDownload.create( downloads[i]);
			}
			
			return( new RPReply( res ));
						
		}else if ( method.equals( "addDownload[Torrent]" )){
		
			try{
				RPTorrent	torrent = (RPTorrent)request.getParams();
				
				Download res = delegate.addDownload((Torrent)torrent._setLocal());
				
				return( new RPReply( RPDownload.create(res)));
				
			}catch( DownloadException e ){
				
				throw( new RPException("DownloadManager::addDownload failed", e ));
			}
		
		}else if ( method.equals( "addDownload[URL]" )){
				
			try{
				delegate.addDownload((URL)request.getParams());
				
			}catch( DownloadException e ){
				
				throw( new RPException("DownloadManager::addDownload failed", e ));
			}
			
			return( new RPReply( null ));
		}
		
		throw( new RPException( "Unknown method: " + method ));
	}
	
		// ***********************************************************************************8
	
	public void 
	addDownload(
		File 	torrent_file )

		throws DownloadException
	{
		notSupported();
	}
	
	public void 
	addDownload(
		URL		url )
	
		throws DownloadException
	{
		RPDownload[]	res = (RPDownload[])_dispatcher.dispatch( new RPRequest( this, "addDownload[URL]", url )).getResponse();
	}
	
	
	public Download
	addDownload(
		Torrent		torrent )
	
		throws DownloadException
	{
		try{
			RPDownload	res = (RPDownload)_dispatcher.dispatch( new RPRequest( this, "addDownload[Torrent]", torrent )).getResponse();
			
			res._setRemote( _dispatcher );
		
			return( res );
			
		}catch( RPException e ){
			
			if ( e.getCause() instanceof DownloadException ){
				
				throw((DownloadException)e.getCause());
			}
			
			throw( e );
		}	
	}

		
	public Download
	addDownload(
		Torrent		torrent,
		File		torrent_location,
		File		data_location )
	
		throws DownloadException
	{
		notSupported();
		
		return( null );
	}	
	
	public Download
	addNonPersistentDownload(
		Torrent		torrent,
		File		torrent_location,
		File		data_location )
	
		throws DownloadException
	{
		notSupported();
		
		return( null );
	}
	
	
	public Download
	getDownload(
		Torrent		torrent )
	{
		notSupported();
		
		return( null );
	}
	
	
	public Download[]
	getDownloads()
	{
		RPDownload[]	res = (RPDownload[])_dispatcher.dispatch( new RPRequest( this, "getDownloads", null )).getResponse();
		
		for (int i=0;i<res.length;i++){
			
			res[i]._setRemote( _dispatcher );
		}
		
		return( res );
	}
	
	public Download[]
	getDownloads(boolean bSort)
	{
		RPDownload[]	res = (RPDownload[])_dispatcher.dispatch( new RPRequest( this, "getDownloads[bSort]", new Boolean(bSort) )).getResponse();
		
		for (int i=0;i<res.length;i++){
			
			res[i]._setRemote( _dispatcher );
		}
		
		return( res );
	}
	
	
	public void
	addListener(
		DownloadManagerListener	l )
	{
		notSupported();
	}
	
	
	public void
	removeListener(
		DownloadManagerListener	l )
	{
		notSupported();
	}	
}