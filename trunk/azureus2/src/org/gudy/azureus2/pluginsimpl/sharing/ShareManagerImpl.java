/*
 * File    : ShareManagerImpl.java
 * Created : 30-Dec-2003
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

package org.gudy.azureus2.pluginsimpl.sharing;

/**
 * @author parg
 *
 */

import java.io.*;
import java.net.*;
import java.util.*;

import org.gudy.azureus2.plugins.torrent.*;
import org.gudy.azureus2.pluginsimpl.torrent.*;
import org.gudy.azureus2.plugins.sharing.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.tracker.host.*;

public class 
ShareManagerImpl
	implements ShareManager, TOTorrentProgressListener
{
	public static final String		TORRENT_STORE 		= "shares";
	public static final String		TORRENT_SUBSTORE	= "cache";
	
	protected static ShareManagerImpl	singleton;
	
	public synchronized static ShareManagerImpl
	getSingleton()
	
		throws ShareException
	{
		if ( singleton == null ){
			
			singleton = new ShareManagerImpl();
		}
		
		return( singleton );
	}
	
	
	protected boolean			initialised;
	protected File				share_dir;
	
	protected URL				announce_url;
	protected ShareConfigImpl	config;
	
	protected Map				shares 		= new HashMap();
	protected List				listeners	= new ArrayList();
	
	protected
	ShareManagerImpl()
	
		throws ShareException
	{
	}
	
	public synchronized void
	initialise()
		throws ShareException
	{
		if ( !initialised ){
		
			share_dir = FileUtil.getApplicationFile( TORRENT_STORE );
			
			share_dir.mkdirs();
			
			String 	tracker_ip 		= COConfigurationManager.getStringParameter("Tracker IP", "");
	
			if ( tracker_ip.length() == 0 ){
				
				throw( new ShareException( "ShareManager: tracker must be configured"));
			}
			
			// port = COConfigurationManager.getIntParameter("Tracker Port SSL", TRHost.DEFAULT_PORT_SSL );
									
			int port = COConfigurationManager.getIntParameter("Tracker Port", TRHost.DEFAULT_PORT );
					
			try{
				announce_url = new URL( "http://" + tracker_ip + ":" + port + "/announce" );
				
			}catch( MalformedURLException e ){
				
				throw( new ShareException( "Announce URL invalid", e ));
			}
	
			reportCurrentTask( "Loading configuration");
			
			config = new ShareConfigImpl();
			
			config.loadConfig(this);
		
			reportCurrentTask( "Checking Consistency");	
			
			checkConsistency();
		}
	}
	
	protected void
	checkConsistency()
	{
		Iterator	it = new HashSet(shares.keySet()).iterator();
		
		while(it.hasNext()){
			
			ShareResourceImpl	resource = (ShareResourceImpl)it.next();
			
			resource.checkConsistency();
		}
	}
	
	protected void
	deserialiseResource(
		Map					map )
	{
		try{
			ShareResourceImpl	new_resource = null;
			
			int	type = ((Long)map.get("type")).intValue();
			
			if ( 	type == ShareResource.ST_FILE ||
					type == ShareResource.ST_DIR ){
				
				new_resource = ShareResourceFileOrDirImpl.deserialiseResource( this, map, type );
			}
			
			if ( new_resource != null ){
				
				ShareResource	old_resource = (ShareResource)shares.get(new_resource);
				
				if ( old_resource != null ){
					
					old_resource.delete();
				}
				
				shares.put( new_resource, new_resource );
				
				for (int i=0;i<listeners.size();i++){
					
					try{
					
						((ShareManagerListener)listeners.get(i)).resourceAdded( new_resource );
						
					}catch( Throwable e ){
					
						e.printStackTrace();
					}
				}
			}
		}catch( ShareException e ){
			
			e.printStackTrace();
		}
	}
	
	protected String
	getNewTorrentLocation()
	
		throws ShareException
	{
		Random rand = new Random(System.currentTimeMillis());
		
		for (int i=1;i<1024;i++){
			
			String	cache_dir_str = share_dir + File.separator + TORRENT_SUBSTORE + i;
			
			File	cache_dir = new File(cache_dir_str);
			
			if ( !cache_dir.exists()){
				
				cache_dir.mkdirs();
			}
			
			for (int j=0;j<1024;j++){
				
				long	file = Math.abs(rand.nextLong());
		
				File	file_name = new File(cache_dir_str + File.separator + file + ".torrent");
				
				if ( !file_name.exists()){
					
					return( TORRENT_SUBSTORE + i + File.separator + file + ".torrent" );
				}
			}
		}
		
		throw( new ShareException( "ShareManager: failed to allocate cache file"));
	}
	
	protected void
	writeTorrent(
		ShareItemImpl		item )
	
		throws ShareException
	{
		try{
			item.getTorrent().writeToFile( new File(share_dir+File.separator+item.getTorrentLocation()));
			
		}catch( TorrentException e ){
			
			throw( new ShareException( "ShareManager: torrent write fails", e ));
		}
	}
	
	protected void
	readTorrent(
		ShareItemImpl		item )
	
		throws ShareException
	{
		try{
			TOTorrent torrent = TOTorrentFactory.deserialiseFromBEncodedFile( new File(share_dir+File.separator+item.getTorrentLocation()));
			
			item.setTorrent(new TorrentImpl(torrent));
			
		}catch( TOTorrentException e ){
			
			throw( new ShareException( "ShareManager: torrent read fails", e ));
		}
	}
	
	protected void
	deleteTorrent(
			ShareItemImpl		item )
	{
		File file = new File(share_dir+File.separator+item.getTorrentLocation());
				
		file.delete();
	}
	
	protected boolean
	torrentExists(
		ShareItemImpl		item )
	{
		File file = new File(share_dir+File.separator+item.getTorrentLocation());
		
		return( file.exists());
	}
	
	protected URL
	getAnnounceURL()
	{
		return( announce_url );
	}
	
	public ShareResource[]
	getShares()
	{
		ShareResource[]	res = new ShareResource[shares.size()];
		
		shares.keySet().toArray( res );
		
		return( res );
	}
	
	public ShareResourceFile
	addFile(
		File	file )
	
		throws ShareException
	{
		return( (ShareResourceFile)addFileOrDir( file, ShareResource.ST_FILE, false ));
	}
	
	public synchronized ShareResourceDir
	addDir(
		File	dir )
	
		throws ShareException
	{
		return( (ShareResourceDir)addFileOrDir( dir, ShareResource.ST_DIR, false ));		
	}
	
	protected synchronized ShareResource
	addFileOrDir(
		File		file,
		int			type,
		boolean		modified )
	
		throws ShareException
	{
		ShareResourceImpl new_resource;
		
		if ( type == ShareResource.ST_FILE ){
	
			reportCurrentTask( "Adding file '" + file.toString() + "'");
			
			new_resource = new ShareResourceFileImpl( this, file );
			
		}else{
			
			reportCurrentTask( "Adding dir '" + file.toString() + "'");
			
			new_resource = new ShareResourceDirImpl( this, file );
		}
		
		ShareResource	old_resource = (ShareResource)shares.get(new_resource);
				
		if ( old_resource != null ){
			
			old_resource.delete();
		}
		
		shares.put( new_resource, new_resource );
		
		config.saveConfig();
		
		for (int i=0;i<listeners.size();i++){
			
			try{
				
				if ( modified ){
					
					((ShareManagerListener)listeners.get(i)).resourceModified( new_resource );
				
				}else{
					
					((ShareManagerListener)listeners.get(i)).resourceAdded( new_resource );				
				}
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
		}
		
		return( new_resource );
	}
	

	
	public synchronized ShareResourceDirContents
	addDirContents(
		File	dir,
		boolean	recursive )
	{
		return( null );
		
	}	
	
	protected synchronized void
	delete(
		ShareResourceImpl	resource )
	{
		shares.remove(resource);
		
		resource.deleteInternal();
		
		config.saveConfig();
		
		for (int i=0;i<listeners.size();i++){
			
			try{
				
				((ShareManagerListener)listeners.get(i)).resourceDeleted( resource );
				
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
		}
	}
	
	public void
	reportProgress(
		int		percent_complete )
	{
		for (int i=0;i<listeners.size();i++){
			
			try{
				
				((ShareManagerListener)listeners.get(i)).reportProgress( percent_complete );
				
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
		}		
	}
	
	public void
	reportCurrentTask(
		String	task_description )
	{
		for (int i=0;i<listeners.size();i++){
			
			try{
				
				((ShareManagerListener)listeners.get(i)).reportCurrentTask( task_description );
				
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
		}			
	}

	public void
	addListener(
		ShareManagerListener		l )
	{
		listeners.add(l);	
	}
	
	public void
	removeListener(
		ShareManagerListener		l )
	{
		listeners.remove(l);
	}
}
