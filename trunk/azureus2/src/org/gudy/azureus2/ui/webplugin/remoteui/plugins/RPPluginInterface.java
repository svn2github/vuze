/*
 * File    : RPPluginInterface.java
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

package org.gudy.azureus2.ui.webplugin.remoteui.plugins;

/**
 * @author parg
 *
 */

import java.util.*;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.sharing.ShareManager;
import org.gudy.azureus2.plugins.sharing.ShareException;
import org.gudy.azureus2.plugins.tracker.Tracker;
import org.gudy.azureus2.plugins.logging.Logger;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.peers.protocol.PeerProtocolManager;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.PluginConfigUIFactory;
import org.gudy.azureus2.plugins.ui.tables.peers.PluginPeerItemFactory;

public class 
RPPluginInterface
	extends		RPObject
	implements 	PluginInterface
{
	protected transient PluginInterface		delegate;
	
	public
	RPPluginInterface(
		PluginInterface		_delegate )
	{
		super( _delegate );
		
		delegate	= _delegate;
	}
	
	public void
	_setLocal()
	
		throws RPException
	{
		delegate = (PluginInterface)_fixupLocal();
	}
	
	public void 
	addView(PluginView view)
	{
		notSupported();
	}
	
	public void addConfigUIParameters(Parameter[] parameters, String displayName)
	{
		notSupported();
	}
	
	public void addColumnToPeersTable(String columnName,PluginPeerItemFactory factory)
	{
		notSupported();
	}
	
	
	public Tracker getTracker()
	{
		notSupported();
		
		return( null );
	}
	
	public Logger getLogger()
	{
		notSupported();
		
		return( null );
	}
	
	public DownloadManager
	getDownloadManager()
	{
		notSupported();
		
		return( null );
	}
	
	
	public PeerProtocolManager
	getPeerProtocolManager()
	{
		notSupported();
		
		return( null );
	}
	
	public ShareManager
	getShareManager()
	
		throws ShareException
	{
		notSupported();
		
		return( null );
	}
	
	/**
	 * @deprecated
	 */
	
	public void openTorrentFile(String fileName)
	{
		notSupported();
	}
	
	/**
	 * @deprecated
	 */
	
	public void openTorrentURL(String url)
	{
		notSupported();
	}
	
	public Properties getPluginProperties()
	{
		return((Properties)dispatcher.dispatch( new RPRequest( this, "getPluginProperties", null )).getResponse());
	}
	
	public String getPluginDirectoryName()
	{
		notSupported();
		
		return( null );
	}
	
	public PluginConfig getPluginconfig()
	{
		notSupported();
		
		return( null );
	}
	
	public PluginConfigUIFactory getPluginConfigUIFactory()
	{
		notSupported();
		
		return( null );
	}
	
	public void
	addListener(
			PluginListener	l )
	{
		notSupported();
	}
	
	public void
	removeListener(
			PluginListener	l )	
	{
		notSupported();
	}
	
	public RPReply
	_process(
		RPRequest	request	)
	{
		String	method = request.getMethod();
		
		if ( method.equals( "getPluginProperties")){
		
			return( new RPReply( delegate.getPluginProperties()));
		}
		
		throw( new RPException( "Unknown method: " + method ));
	}
	
}
