/*
 * File    : DownloadManagerImpl.java
 * Created : 06-Jan-2004
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

package org.gudy.azureus2.pluginsimpl.local.download;

/**
 * @author parg
 *
 */

import java.util.*;
import java.io.File;
import java.net.URL;

import com.aelitis.azureus.core.*;
import org.gudy.azureus2.plugins.torrent.*;
import org.gudy.azureus2.plugins.ui.UIManagerEvent;
import org.gudy.azureus2.pluginsimpl.local.torrent.*;
import org.gudy.azureus2.pluginsimpl.local.ui.UIManagerImpl;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.download.DownloadManagerStats;
import org.gudy.azureus2.plugins.download.DownloadRemovalVetoException;

import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.global.*;
import org.gudy.azureus2.core3.download.*;
import org.gudy.azureus2.core3.util.*;


public class 
DownloadManagerImpl
	implements org.gudy.azureus2.plugins.download.DownloadManager
{
	protected static DownloadManagerImpl	singleton;
	protected static AEMonitor				class_mon	= new AEMonitor( "DownloadManager:class");
	
	public static DownloadManagerImpl
	getSingleton(
		AzureusCore	azureus_core )
	{
		try{
			class_mon.enter();
	
			if ( singleton == null ){
				
				singleton = new DownloadManagerImpl( azureus_core );
			}
			
			return( singleton );
			
		}finally{
			
			class_mon.exit();
		}
	}
	
	private AzureusCore				azureus_core;
	private GlobalManager			global_manager;
	private DownloadManagerStats	stats;
	
	private List			listeners		= new ArrayList();
	private AEMonitor		listeners_mon	= new AEMonitor( "DownloadManager:L");
	
	private List			downloads		= new ArrayList();
	private Map				download_map	= new HashMap();
		
	protected
	DownloadManagerImpl(
		AzureusCore	_azureus_core )
	{
		azureus_core	= _azureus_core;
		global_manager	= _azureus_core.getGlobalManager();
		
		stats = new DownloadManagerStatsImpl( global_manager );
		
		global_manager.addListener(
			new GlobalManagerListener()
			{
				public void
				downloadManagerAdded(
					DownloadManager	dm )
				{
					addDownloadManager( dm );
				}
				
				public void
				downloadManagerRemoved(
					DownloadManager	dm )
				{
					List			listeners_ref	= null;
					DownloadImpl	dl				= null;
					
					try{
						listeners_mon.enter();
						
						dl = (DownloadImpl)download_map.get( dm );
						
						if ( dl == null ){
							
							System.out.println( "DownloadManager:unknown manager removed");
							
						}else{
						
							downloads.remove( dl );
							
							download_map.remove( dm );
							
							dl.destroy();
						
							listeners_ref = listeners;
						}
						
					}finally{
						
						listeners_mon.exit();
					}
					
					if ( dl != null ){
							
						for (int i=0;i<listeners_ref.size();i++){
								
							((DownloadManagerListener)listeners_ref.get(i)).downloadRemoved( dl );
						}
					}
				}
				
				public void
				destroyInitiated()
				{
				}				
				
				public void
				destroyed()
				{	
				}
                
                
                public void seedingStatusChanged( boolean seeding_only_mode ){
                  //TODO
                }           
			});
		
		global_manager.addDownloadWillBeRemovedListener(
			new GlobalManagerDownloadWillBeRemovedListener()
			{
				public void
				downloadWillBeRemoved(
					DownloadManager	dm )
				
					throws GlobalManagerDownloadRemovalVetoException
				{					
					DownloadImpl	download = (DownloadImpl)download_map.get( dm );
				
					if ( download != null ){
					
						try{ 
							download.isRemovable();
							
						}catch( DownloadRemovalVetoException e ){
													
							throw( new GlobalManagerDownloadRemovalVetoException( e.getMessage()));
						}		
					}
				}
			});
	}
	
	public void 
	addDownload(
		final File fileName ) 
	{
		UIManagerImpl.fireEvent(
			new UIManagerEvent()
			{
				public int
				getType()
				{
					return( UIManagerEvent.ET_OPEN_TORRENT_VIA_FILE );
				}
				
				public Object
				getData()
				{
					return( fileName );
				}
			});
	}

	public void 
	addDownload(
		final URL	url) 
	{
		addDownload(url,null);
	}
	
	public void 
	addDownload(
		final URL	url,
		final URL 	referrer) 
	{
		UIManagerImpl.fireEvent(
				new UIManagerEvent()
				{
					public int
					getType()
					{
						return( UIManagerEvent.ET_OPEN_TORRENT_VIA_URL );
					}
					
					public Object
					getData()
					{
						return( new URL[]{ url, referrer });
					}
				});
	}
	
	protected void
	addDownloadManager(
		DownloadManager	dm )
	{
		List			listeners_ref 	= null;
		DownloadImpl	dl				= null;
		
		try{
			listeners_mon.enter();
			
			if ( download_map.get(dm) == null ){
	
				dl = new DownloadImpl(dm);
				
				downloads.add( dl );
				
				download_map.put( dm, dl );
				
				listeners_ref = listeners;
			}
		}finally{
			
			listeners_mon.exit();
		}
		
		if ( dl != null ){
			
			for (int i=0;i<listeners_ref.size();i++){
					
				try{
					((DownloadManagerListener)listeners_ref.get(i)).downloadAdded( dl );
					
				}catch( Throwable e ){
						
					Debug.printStackTrace( e );
				}
			}
		}
	}
	
	public Download
	addDownload(
		Torrent		torrent )
	
		throws DownloadException
	{	 
	    return( addDownload( torrent, null, null ));
	}
	
	public Download
	addDownload(
		Torrent		torrent,
		File		torrent_file,
		File		data_location )
	
		throws DownloadException
	{
		if ( torrent_file == null ){
			
		    String torrent_dir = null;
		    
		    if( COConfigurationManager.getBooleanParameter("Save Torrent Files",true)){
		    	
		      try{
		      	
		      	torrent_dir = COConfigurationManager.getDirectoryParameter("General_sDefaultTorrent_Directory");
		        
		      }catch(Exception egnore){}
		    }
		    
		    if ( torrent_dir == null || torrent_dir.length() == 0 ){
		    	
		    	throw( new DownloadException("DownloadManager::addDownload: default torrent save directory must be configured" ));
		    }
		
		    torrent_file = new File( torrent_dir + File.separator + torrent.getName() + ".torrent" );
		    
		    try{
		    	torrent.writeToFile( torrent_file );
		    	
		    }catch( TorrentException e ){
		    	
		    	throw( new DownloadException("DownloadManager::addDownload: failed to write torrent to '" + torrent_file.toString() + "'", e ));	    	
		    }
		}
		
		if ( data_location == null ){
			
		    boolean useDefDataDir = COConfigurationManager.getBooleanParameter("Use default data dir");
		    
		    String data_dir = null;
		    
		    if (useDefDataDir){
		    	
		    	data_dir = COConfigurationManager.getStringParameter("Default save path");
		    }
		    
		    if ( data_dir == null || data_dir.length() == 0 ){
		    	
		    	throw( new DownloadException("DownloadManager::addDownload: default data save directory must be configured" ));
		    }
		    
		    data_location = new File(data_dir); 
		    
		    data_location.mkdirs();
		}
		
		DownloadManager dm = global_manager.addDownloadManager(torrent_file.toString(),
		                                                       data_location.toString(), 
		                                                       getInitialState(), 
		                                                       true );
		
		if ( dm == null ){
			
			throw( new DownloadException( "DownloadManager::addDownload - failed"));
		}
		
		addDownloadManager( dm );
		
		return( getDownload( dm ));
	}

	public Download
	addNonPersistentDownload(
		Torrent		torrent,
		File		torrent_file,
		File		data_location )
	
		throws DownloadException
	{

		DownloadManager dm = global_manager.addDownloadManager(torrent_file.toString(),
		                                                       data_location.toString(), 
		                                                       getInitialState(), 
		                                                       false );
		
		if ( dm == null ){
			
			throw( new DownloadException( "DownloadManager::addDownload - failed"));
		}
		
		addDownloadManager( dm );
		
		return( getDownload( dm ));
	}
	
	protected int
	getInitialState()
	{
	  	boolean	default_start_stopped = COConfigurationManager.getBooleanParameter( "Default Start Torrents Stopped" );

        return( default_start_stopped?DownloadManager.STATE_STOPPED:DownloadManager.STATE_WAITING);
	}
	
	protected Download
	getDownload(
		DownloadManager	dm )
	
		throws DownloadException
	{
		Download dl = (Download)download_map.get(dm);
		
		if ( dl == null ){
			
			throw( new DownloadException("DownloadManager::getDownload: download not found"));
		}
		
		return( dl );
	}

	public static Download
	getDownloadStatic(
		DownloadManager	dm )
	
		throws DownloadException
	{
		if ( singleton != null ){
			
			return( singleton.getDownload( dm ));
		}
		
		throw( new DownloadException( "DownloadManager not initialised"));
	}
	
	protected Download
	getDownload(
		TOTorrent	torrent )
	
		throws DownloadException
	{
		for (int i=0;i<downloads.size();i++){
			
			Download	dl = (Download)downloads.get(i);
			
			TorrentImpl	t = (TorrentImpl)dl.getTorrent();
			
				// can be null if broken torrent
			
			if ( t == null ){
				
				continue;
			}
			
			if ( t.getTorrent().hasSameHashAs( torrent )){
				
				return( dl );
			}
		}
		
		throw( new DownloadException("DownloadManager::getDownload: download not found"));
	}
	
	public static Download
	getDownloadStatic(
		TOTorrent	torrent )
	
		throws DownloadException
	{
		if ( singleton != null ){
			
			return( singleton.getDownload( torrent ));
		}
		
		throw( new DownloadException( "DownloadManager not initialised"));
	}
	
	public Download
	getDownload(
		Torrent		_torrent )
	{
		TorrentImpl	torrent = (TorrentImpl)_torrent;
		
		try{
			return( getDownload( torrent.getTorrent()));
			
		}catch( DownloadException e ){
		}
		
		return( null );
	}
	
	public Download
	getDownload(
		byte[]	hash )
	{
		List	dls = global_manager.getDownloadManagers();

		for (int i=0;i<dls.size();i++){
			
			DownloadManager	man = (DownloadManager)dls.get(i);
			
				// torrent can be null if download manager torrent file read fails
			
			TOTorrent	torrent = man.getTorrent();
			
			if ( torrent != null ){
				
				try{
					if ( Arrays.equals( torrent.getHash(), hash )){
				
						return( getDownload( torrent ));
					}
				}catch( DownloadException e ){
					
						// not found
					
				}catch( TOTorrentException e ){
					
					Debug.printStackTrace( e );
				}
			}
		}
		
		return( null );
	}
	
	public Download[]
	getDownloads()
	{
		List	res_l = new ArrayList();
	
		// we have to use the global manager's ordering as it
		// hold this

		List dms = global_manager.getDownloadManagers();

		try{
			listeners_mon.enter();

			for (int i=0;i<dms.size();i++){
			
				Object	dl = download_map.get( dms.get(i));
				
				if ( dl != null ){
					
					res_l.add( dl );
				}
			}
		}finally{
			
			listeners_mon.exit();
		}
		
		Download[]	res = new Download[res_l.size()];
			
		res_l.toArray( res );
			
		return( res );
	}
	
	public Download[]
	getDownloads(boolean bSorted)
	{
		if (bSorted){
	  
			return getDownloads();
		}
	  
		try{
			listeners_mon.enter();
		
			Download[]	res = new Download[downloads.size()];
			
			downloads.toArray( res );
			
			return( res );
			
		}finally{
			
			listeners_mon.exit();
		}
	}

	public void
	pauseDownloads()
	{
		global_manager.pauseDownloads();
	}
		
	public void
	resumeDownloads()
	{
		global_manager.resumeDownloads();
	}
		
	public void
	startAllDownloads()
	{
		global_manager.startAllDownloads();
	}
		
	public void
	stopAllDownloads()
	{
		global_manager.stopAllDownloads();
	}
	
	public DownloadManagerStats
	getStats()
	{
		return( stats );
	}
	
	public boolean
	isSeedingOnly()
	{
		return( global_manager.isSeedingOnly());
	}
	
	public void
	addListener(
		DownloadManagerListener	l )
	{
		List	downloads_copy;
		
		try{
			listeners_mon.enter();
			
			List	new_listeners = new ArrayList( listeners );
			
			new_listeners.add( l );
		
			listeners	= new_listeners;
			
			downloads_copy = new ArrayList( downloads );
	
		}finally{
			
			listeners_mon.exit();
		}	
		
		for (int i=0;i<downloads_copy.size();i++){
			
			try{
					
				l.downloadAdded((Download)downloads_copy.get(i));
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}
	}
	
	public void
	removeListener(
		DownloadManagerListener	l )
	{
		try{
			listeners_mon.enter();
			
			List	new_listeners	= new ArrayList( listeners );
			
			new_listeners.remove(l);
			
			listeners	= new_listeners;
			
		}finally{
			listeners_mon.exit();
		}	
	}
}
