/*
 * File    : DownloadImpl.java
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

import org.gudy.azureus2.core3.category.*;
import org.gudy.azureus2.core3.global.*;
import org.gudy.azureus2.core3.download.*;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.tracker.client.*;

import org.gudy.azureus2.plugins.torrent.Torrent;

import org.gudy.azureus2.plugins.peers.*;
import org.gudy.azureus2.pluginsimpl.local.peers.*;
import org.gudy.azureus2.pluginsimpl.local.torrent.TorrentImpl;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadListener;
import org.gudy.azureus2.plugins.download.DownloadPeerListener;
import org.gudy.azureus2.plugins.download.DownloadTrackerListener;
import org.gudy.azureus2.plugins.download.DownloadAnnounceResult;
import org.gudy.azureus2.plugins.download.DownloadScrapeResult;
import org.gudy.azureus2.plugins.download.DownloadStats;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.download.DownloadRemovalVetoException;
import org.gudy.azureus2.plugins.download.DownloadWillBeRemovedListener;

import org.gudy.azureus2.core3.internat.MessageText;

public class 
DownloadImpl
	implements 	Download, DownloadManagerListener, 
				DownloadManagerTrackerListener, DownloadManagerPeerListener
{
	protected DownloadManager		download_manager;
	protected DownloadStatsImpl		download_stats;
	
	protected int		latest_state		= ST_STOPPED;
	protected boolean latest_forcedStart;
	
	protected DownloadAnnounceResultImpl	last_announce_result 	= new DownloadAnnounceResultImpl(this,null);
	protected DownloadScrapeResultImpl		last_scrape_result		= new DownloadScrapeResultImpl( this, null );
	
	protected List		listeners 			= new ArrayList();
	protected List		tracker_listeners	= new ArrayList();
	protected List		removal_listeners 	= new ArrayList();
	protected List		peer_listeners		= new ArrayList();
	
	protected
	DownloadImpl(
		DownloadManager		_dm )
	{
		download_manager	= _dm;
		download_stats		= new DownloadStatsImpl( download_manager );
		
		download_manager.addListener( this );
		
		latest_forcedStart = download_manager.isForceStart();
	}
	
	protected DownloadManager
	getDownload()
	{
		return( download_manager );
	}
	
	public int
	getState()
	{
		int	state = download_manager.getState();
			
		// dm states: waiting -> initialising -> initialized -> 
		//		disk states: allocating -> checking -> ready ->
		// dm states: downloading -> finishing -> seeding -> stopping -> stopped
		
		// "initialize" call takes from waiting -> initialising -> waiting (no port) or initialized (ok)
		// if initialized then disk manager runs through to ready
		// "startdownload" takes ready -> dl etc.
		// "stopIt" takes to stopped which is equiv to ready
		
		switch( state ){
			case DownloadManager.STATE_WAITING:
			{
				latest_state	= ST_WAITING;
				
				break;
			}		
			case DownloadManager.STATE_INITIALIZING:
			case DownloadManager.STATE_INITIALIZED:
			case DownloadManager.STATE_ALLOCATING:
			case DownloadManager.STATE_CHECKING:
			{
				latest_state	= ST_PREPARING;
					
				break;
			}
			case DownloadManager.STATE_READY:
			{
				latest_state	= ST_READY;
					
				break;
			}
			case DownloadManager.STATE_DOWNLOADING:
			case DownloadManager.STATE_FINISHING:		// finishing download - transit to seeding
			{
				latest_state	= ST_DOWNLOADING;
					
				break;
			}
			case DownloadManager.STATE_SEEDING:
			{
				latest_state	= ST_SEEDING;
				
				break;
			}
			case DownloadManager.STATE_STOPPING:
			{
				latest_state	= ST_STOPPING;
				
				break;
			}
			case DownloadManager.STATE_STOPPED:
			{
				latest_state	= ST_STOPPED;
					
				break;
			}
			case DownloadManager.STATE_QUEUED:
			{
				latest_state	= ST_QUEUED;
					
				break;
			}
			case DownloadManager.STATE_ERROR:
			{
				latest_state	= ST_ERROR;
				
				break;
			}
			default:
			{
				latest_state	= ST_ERROR;
			}
		}
		
		return( latest_state );
	}
	
	public String
	getErrorStateDetails()
	{
		return( download_manager.getErrorDetails());
	}
	
	public int
	getIndex()
	{
		return( download_manager.getIndex());
	}
	
	public Torrent
	getTorrent()
	{
		TOTorrent	torrent = download_manager.getTorrent();
		
		if ( torrent == null ){
			
			return( null );
			
		}else{
			
			return( new TorrentImpl( torrent ));
		}
	}

	public void
	initialize()
	
		throws DownloadException
	{
		if ( download_manager.getState() == DownloadManager.STATE_WAITING ){
			
			download_manager.initialize();
			
		}else{
			
			throw( new DownloadException( "Download::initialize: download not waiting" ));
		}
	}
	
	public void
	start()
	
		throws DownloadException
	{
		if ( download_manager.getState() == DownloadManager.STATE_READY ){
						
			download_manager.startDownload();
							
				//set previous hash fails and discarded values
				
			download_manager.getStats().setSavedDiscarded();
			download_manager.getStats().setSavedHashFails();
			
		}else{
			
			throw( new DownloadException( "Download::start: download not ready" ));
		}
	}
	
	public void
	restart()
	
		throws DownloadException
	{
		if ( download_manager.getState() == DownloadManager.STATE_STOPPED ||
		     download_manager.getState() == DownloadManager.STATE_QUEUED ){
			
			download_manager.setState( DownloadManager.STATE_WAITING );
			
		}else{
			
			throw( new DownloadException( "Download::restart: download already running" ));
		}
	}
	
	public void
	stop()
	
		throws DownloadException
	{
		if ( download_manager.getState() != DownloadManager.STATE_STOPPED){
			
			download_manager.stopIt();
			
		}else{
			
			throw( new DownloadException( "Download::stop: download already stopped" ));
		}
	}
	
	public void
	stopAndQueue()
	
		throws DownloadException
	{
		if ( download_manager.getState() != DownloadManager.STATE_QUEUED)
			download_manager.stopIt(DownloadManager.STATE_QUEUED);
		else
			throw( new DownloadException( "Download::stopAndQueue: download already queued" ));
	}
	
	public boolean
	isStartStopLocked()
	{
		return( download_manager.getState() == DownloadManager.STATE_STOPPED );
	}
	
	public boolean
	isForceStart()
	{
		return download_manager.isForceStart();
	}
	
	public void
	setForceStart(boolean forceStart)
	{
		download_manager.setForceStart(forceStart);
	}
	
	public int
	getPriority()
	{
		if ( download_manager.getPriority() == DownloadManager.HIGH_PRIORITY ){
			
			return( PR_HIGH_PRIORITY );
			
		}else{
			
			return( PR_LOW_PRIORITY );
		}
	}
	
	public void
	setPriority(
		int		priority )
	{
		download_manager.setPriority(
			priority==PR_HIGH_PRIORITY?DownloadManager.HIGH_PRIORITY:DownloadManager.LOW_PRIORITY );
	}
	
	public boolean
	isPriorityLocked()
	{
		return( false );
	}
	
	public int
	getPosition()
	{
		return download_manager.getPosition();
	}
	
	public void
	setPosition(int newPosition)
	{
		download_manager.setPosition(newPosition);
	}
	
	public void
	moveUp()
	{
		download_manager.moveUp();
	}
	
	public void
	moveDown()
	{
		download_manager.moveDown();
	}
	
	public String 
	getName()
	{
		return download_manager.getName();
	}

  public String getTorrentFileName() {
    return download_manager.getTorrentFileName();
  }
  
  public String getCategoryName() {
    Category category = download_manager.getCategory();
    if (category == null)
      category = CategoryManager.getCategory(Category.TYPE_UNCATEGORIZED);

    if (category == null)
      return null;
    return category.getName();
  }
    
  
  public void setCategory(String sName) {
    Category category = CategoryManager.getCategory(sName);
    if (category == null)
      category = CategoryManager.createCategory(sName);
    download_manager.setCategory(category);
  }

	public void
	remove()
	
		throws DownloadException, DownloadRemovalVetoException
	{
		int	dl_state = download_manager.getState();
		
		if ( 	dl_state == DownloadManager.STATE_STOPPED 	|| 
				dl_state == DownloadManager.STATE_ERROR 	||
				dl_state == DownloadManager.STATE_QUEUED ){
			
			GlobalManager globalManager = download_manager.getGlobalManager();
			
			try{
				globalManager.removeDownloadManager(download_manager);
				
			}catch( GlobalManagerDownloadRemovalVetoException e ){
				
				throw( new DownloadRemovalVetoException( e.getMessage()));
			}
			
		}else{
			
			throw( new DownloadRemovalVetoException( MessageText.getString("plugin.download.remove.veto.notstopped")));
		}
	}
	
	public boolean
	canBeRemoved()
	
		throws DownloadRemovalVetoException
	{
		int	dl_state = download_manager.getState();
		
		if ( 	dl_state == DownloadManager.STATE_STOPPED 	|| 
				dl_state == DownloadManager.STATE_ERROR 	||
				dl_state == DownloadManager.STATE_QUEUED ){
						
			GlobalManager globalManager = download_manager.getGlobalManager();
			
			try{
				globalManager.canDownloadManagerBeRemoved(download_manager);
				
			}catch( GlobalManagerDownloadRemovalVetoException e ){
				
				throw( new DownloadRemovalVetoException( e.getMessage()));
			}
			
		}else{
			
			throw( new DownloadRemovalVetoException( MessageText.getString("plugin.download.remove.veto.notstopped")));
		}
		
		return( true );
	}
	
	public DownloadStats
	getStats()
	{
		return( download_stats );
	}
	
	protected void
	isRemovable()
		throws DownloadRemovalVetoException
	{
		synchronized( removal_listeners ){
			
			for (int i=0;i<removal_listeners.size();i++){
				
				try{
					((DownloadWillBeRemovedListener)removal_listeners.get(i)).downloadWillBeRemoved(this);
					
				}catch( DownloadRemovalVetoException e ){
					
					throw( e );
					
				}catch( Throwable e ){
					
					e.printStackTrace();
				}
			}
		}
	}
	
	protected void
	destroy()
	{
		download_manager.removeListener( this );
	}
	

	// DownloadManagerListener methods
	
	public void
	stateChanged(
      DownloadManager manager,
		int		state )
	{
		int	prev_state 	= latest_state;
		int	curr_state	= getState();
		boolean curr_forcedStart = isForceStart();
	
		if ( prev_state != curr_state || latest_forcedStart != curr_forcedStart ){
		  latest_forcedStart = curr_forcedStart;
			
			synchronized( listeners ){
				
				for (int i=0;i<listeners.size();i++){
					
					try{
						((DownloadListener)listeners.get(i)).stateChanged( this, prev_state, curr_state );
					
					}catch( Throwable e ){
						
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	public void
	downloadComplete(DownloadManager manager)
	{	
	}

  public void completionChanged(DownloadManager manager, boolean bCompleted) {
  }
	
  public void positionChanged(DownloadManager download, 
                              int oldPosition, int newPosition) {
		synchronized(listeners){
			for (int i = 0; i < listeners.size(); i++) {
				try {
					((DownloadListener)listeners.get(i)).positionChanged(this, oldPosition, newPosition);
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}
  }

	public void
	addListener(
		DownloadListener	l )
	{
		synchronized( listeners ){
			
			listeners.add(l);
		}
	}
	
	public void
	removeListener(
		DownloadListener	l )
	{
		synchronized( listeners ){
			
			listeners.remove(l);
		}
	}
	
	public DownloadAnnounceResult
	getLastAnnounceResult()
	{
		return( last_announce_result );
	}
	
	public DownloadScrapeResult
	getLastScrapeResult()
	{		
		TRTrackerScraperResponse response = download_manager.getTrackerScrapeResponse();
	
		last_scrape_result.setContent( response );
		
		return( last_scrape_result );
	}
	
	
	public void
	scrapeResult(
		TRTrackerScraperResponse	response )
	{
		last_scrape_result.setContent( response );
		
		synchronized( tracker_listeners ){
			
			for (int i=0;i<tracker_listeners.size();i++){
				
				try{						
					((DownloadTrackerListener)tracker_listeners.get(i)).scrapeResult( last_scrape_result );

				}catch( Throwable e ){
					
					e.printStackTrace();
				}
			}
		}	
	}
	
	public void
	announceResult(
		TRTrackerResponse			response )
	{
		last_announce_result.setContent( response );
		
		synchronized( tracker_listeners ){
			
			for (int i=0;i<tracker_listeners.size();i++){
				
				try{						
					((DownloadTrackerListener)tracker_listeners.get(i)).announceResult( last_announce_result );

				}catch( Throwable e ){
					
					e.printStackTrace();
				}
			}
		}
	}
	
	public void
	torrentChanged()
	{
		TRTrackerClient	client = download_manager.getTrackerClient();
		
		if ( client != null ){
			
			client.resetTrackerUrl(true);
		}
	}
	
	public void
	addTrackerListener(
		DownloadTrackerListener	l )
	{
		synchronized( tracker_listeners ){
	
			tracker_listeners.add( l );
			
			if ( tracker_listeners.size() == 1 ){
				
				download_manager.addTrackerListener( this );
			}
		}
	}
	
	public void
	removeTrackerListener(
		DownloadTrackerListener	l )
	{
		synchronized( tracker_listeners ){
			
			tracker_listeners.remove( l );
			
			if ( tracker_listeners.size() == 0 ){
				
				download_manager.removeTrackerListener( this );
			}
		}
	}
	
	public void
	addDownloadWillBeRemovedListener(
		DownloadWillBeRemovedListener	l )
	{
		synchronized( removal_listeners ){
			
			removal_listeners.add(l);
		}
	}
	
	public void
	removeDownloadWillBeRemovedListener(
		DownloadWillBeRemovedListener	l ) 
	{
		synchronized( removal_listeners ){
			
			removal_listeners.remove(l);
		}
	}
	
	public synchronized void
	addPeerListener(
		DownloadPeerListener	l )
	{
		peer_listeners.add( l );
		
		if ( peer_listeners.size() == 1 ){
			
			download_manager.addPeerListener( this );
		}
	}
	
	
	public synchronized void
	removePeerListener(
		DownloadPeerListener	l )
	{
		peer_listeners.remove( l );
		
		if ( peer_listeners.size() == 0 ){
			
			download_manager.removePeerListener( this );
		}
	}
	
	public void
	peerManagerAdded(
		PEPeerManager	manager )
	{
		if ( peer_listeners.size() > 0 ){
			
			PeerManager pm = PeerManagerImpl.getPeerManager( manager);
		
			for (int i=0;i<peer_listeners.size();i++){
		
				((DownloadPeerListener)peer_listeners.get(i)).peerManagerAdded( this, pm );
			}
		}
	}
	
	public void
	peerManagerRemoved(
		PEPeerManager	manager )
	{
		if ( peer_listeners.size() > 0 ){
			
			PeerManager pm = PeerManagerImpl.getPeerManager( manager);
		
			for (int i=0;i<peer_listeners.size();i++){
		
				((DownloadPeerListener)peer_listeners.get(i)).peerManagerRemoved( this, pm );
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
}
