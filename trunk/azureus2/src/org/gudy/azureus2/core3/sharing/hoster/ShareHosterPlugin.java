/*
 * File    : ShareHosterPlugin.java
 * Created : 05-Jan-2004
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

package org.gudy.azureus2.core3.sharing.hoster;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.logging.*;
import org.gudy.azureus2.plugins.torrent.*;
import org.gudy.azureus2.plugins.tracker.*;
import org.gudy.azureus2.plugins.sharing.*;
import org.gudy.azureus2.plugins.download.*;

public class 
ShareHosterPlugin
	implements Plugin, PluginListener, ShareManagerListener
{
	protected PluginInterface	plugin_interface;
	protected LoggerChannel		log;
	protected Tracker			tracker;
	protected ShareManager		share_manager;
	protected DownloadManager	download_manager;
	
	protected boolean			initialised	= false;
	
	public void 
	initialize(
		PluginInterface _plugin_interface )
	{
		plugin_interface = _plugin_interface;
		
		log	= plugin_interface.getLogger().getChannel("ShareHosterPlugin");
		
		log.log( LoggerChannel.LT_INFORMATION, "ShareHosterPlugin: initialisation starts");
		
		plugin_interface.addListener( this );
	}
	
	public void
	initializationComplete()
	{
		log.log( LoggerChannel.LT_INFORMATION, "ShareHosterPlugin: initialisation complete");
		
		try{
			tracker	=  plugin_interface.getTracker();
	
			download_manager = plugin_interface.getDownloadManager();
			
			share_manager = plugin_interface.getShareManager();
						
			share_manager.addListener( this );
			
			share_manager.initialise();
			
			initialised	= true;
			
			ShareResource[]	shares = share_manager.getShares();
			
			for ( int i=0;i<shares.length;i++){
				
				resourceAdded( shares[i] );
			}
			
		}catch( ShareException e ){
			
			e.printStackTrace();
			
			log.log( e );
		}
	}
	
	public void
	resourceAdded(
		ShareResource		resource )
	{
		log.log( LoggerChannel.LT_INFORMATION, "Resource added:" + resource.getName());
		
		if ( initialised ){
			
			try{
				
				int	type = resource.getType();
				
				if ( type == ShareResource.ST_FILE ){
					
					ShareResourceFile	file_resource = (ShareResourceFile)resource;
					
					ShareItem	item = file_resource.getItem();
			
					Torrent torrent = item.getTorrent();
					
					tracker.host(torrent, false );
					
					Download	download = download_manager.getDownload( torrent );
					
					if ( download == null ){
						
						download_manager.addNonPersistentDownload( torrent, item.getTorrentFile(), file_resource.getFile());
					}
				}else if ( type == ShareResource.ST_DIR ){
				
					ShareResourceDir	dir_resource = (ShareResourceDir)resource;
					
					ShareItem	item = dir_resource.getItem();
					
					Torrent torrent = item.getTorrent();
					
					tracker.host(torrent, false );
					
					Download	download = download_manager.getDownload( torrent );
					
					if ( download == null ){
						
						download_manager.addNonPersistentDownload( torrent, item.getTorrentFile(), dir_resource.getDir());
					}
				}
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
		}
	}
	
	public void
	resourceModified(
		ShareResource		resource )
	{
		log.log( LoggerChannel.LT_INFORMATION, "Resource modified:" + resource.getName());
		
		if ( initialised ){
			
		}
	}
	
	public void
	resourceDeleted(
		ShareResource		resource )
	{
		log.log( LoggerChannel.LT_INFORMATION, "Resource deleted:" + resource.getName());
		
		if ( initialised ){
			
		}
	}

	public void
	reportProgress(
		int		percent_complete )
	{
	}
	
	public void
	reportCurrentTask(
		String	task_description )
	{
		log.log( LoggerChannel.LT_INFORMATION, "Current Task:" + task_description );
	}
}