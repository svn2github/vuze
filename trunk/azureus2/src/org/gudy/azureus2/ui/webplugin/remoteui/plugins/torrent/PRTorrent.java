/*
 * File    : PRTorrent.java
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

package org.gudy.azureus2.ui.webplugin.remoteui.plugins.torrent;

/**
 * @author parg
 *
 */

import java.io.File;

import org.gudy.azureus2.plugins.torrent.*;

import org.gudy.azureus2.ui.webplugin.remoteui.plugins.*;


public class 
PRTorrent
	extends		RPObject
	implements 	Torrent
{
	protected transient Torrent		delegate;

	protected String		name;
	
	public static PRTorrent
	create(
		Torrent		_delegate )
	{
		PRTorrent	res =(PRTorrent)_lookupLocal( _delegate );
		
		if ( res == null ){
			
			res = new PRTorrent( _delegate );
		}
		
		return( res );
	}
	
	protected
	PRTorrent(
		Torrent		_delegate )
	{
		super( _delegate );
		
		delegate	= _delegate;
		
		name		= _delegate.getName();
	}
	
	public void
	_setLocal()
	
		throws RPException
	{
		delegate = (Torrent)_fixupLocal();
	}
	
	
	public RPReply
	_process(
		RPRequest	request	)
	{
		String	method = request.getMethod();
		
		/*
		 if ( method.equals( "getPluginProperties")){
		 
		 return( new RPReply( delegate.getPluginProperties()));
		 }
		 */
		
		throw( new RPException( "Unknown method: " + method ));
	}
	
	public String
	getName()
	{
		return( name );
	}
	
	public byte[]
	getHash()
	{
		notSupported();
		
		return(null);
	}	
	public long
	getSize()
	{
		notSupported();
		
		return(0);
	}
	
	public String
	getComment()
	{
		notSupported();
		
		return(null);
	}	
	
	public long
	getCreationDate()
	{
		notSupported();
		
		return(0);
	}
	
	public String
	getCreatedBy()
	{
		notSupported();
		
		return(null);
	}	
	public long
	getPieceSize()
	{
		notSupported();
		
		return(0);
	}	
	public long
	getPieceCount()
	{
		notSupported();
		
		return(0);
	}	
	public TorrentFile[]
	getFiles()
	{
		notSupported();
		
		return(null);
	}	
	public void
	writeToFile(
		File		file )
	
		throws TorrentException
	{
		notSupported();
	}
}