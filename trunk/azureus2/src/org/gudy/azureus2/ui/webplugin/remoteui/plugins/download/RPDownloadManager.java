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
	
	public void
	_setLocal()
	
		throws RPException
	{
		_fixupLocal();
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
			
		}else if ( method.equals( "addDownload[URL]" )){
			
			try{
				delegate.addDownload((URL)request.getParams());
				
			}catch( DownloadException e ){
				
				throw( new RPException("DownloadManager::addDownload failed", e ));
			}
		}
		
		throw( new RPException( "Unknown method: " + method ));
	}
	
	
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
		RPDownload[]	res = (RPDownload[])dispatcher.dispatch( new RPRequest( this, "addDownload[URL]", url )).getResponse();
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
		RPDownload[]	res = (RPDownload[])dispatcher.dispatch( new RPRequest( this, "getDownloads", null )).getResponse();
		
		for (int i=0;i<res.length;i++){
			
			res[i]._setRemote( dispatcher );
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