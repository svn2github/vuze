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

package org.gudy.azureus2.pluginsimpl.download;

/**
 * @author parg
 *
 */

import java.util.*;

import org.gudy.azureus2.core3.global.*;
import org.gudy.azureus2.core3.download.*;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.tracker.client.*;

import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.pluginsimpl.torrent.TorrentImpl;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadListener;
import org.gudy.azureus2.plugins.download.DownloadTrackerListener;
import org.gudy.azureus2.plugins.download.DownloadStats;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.download.DownloadRemovalVetoException;
import org.gudy.azureus2.plugins.download.DownloadWillBeRemovedListener;

public class 
DownloadImpl
	implements Download, DownloadManagerListener, DownloadManagerTrackerListener
{
	protected DownloadManager		download_manager;
	
	protected int		latest_state		= ST_STOPPED;
	
	protected List		listeners 			= new ArrayList();
	protected List		tracker_listeners	= new ArrayList();
	protected List		removal_listeners 	= new ArrayList();
	
	protected
	DownloadImpl(
		DownloadManager		_dm )
	{
		download_manager	= _dm;
		
		download_manager.addListener( this );
	}
	
	public int
	getState()
	{
		int	state = download_manager.getState();
				
		switch( state ){
			case DownloadManager.STATE_DOWNLOADING:
			case DownloadManager.STATE_FINISHING:
			case DownloadManager.STATE_SEEDING:
			case DownloadManager.STATE_STOPPING:
			{
				latest_state	= ST_STARTED;
				
				break;
			}
			case DownloadManager.STATE_WAITING:
			case DownloadManager.STATE_INITIALIZING:
			case DownloadManager.STATE_INITIALIZED:
			case DownloadManager.STATE_ALLOCATING:
			case DownloadManager.STATE_CHECKING:
			case DownloadManager.STATE_READY:
			case DownloadManager.STATE_STOPPED:
			case DownloadManager.STATE_ERROR:
			{
				latest_state	= ST_STOPPED;
				
				break;
			}
			default:
			{
				latest_state	= ST_STOPPED;
			}
		}
		
		return( latest_state );
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
	start()
	
		throws DownloadException
	{
		if ( download_manager.getState() == DownloadManager.STATE_STOPPED){
			
			download_manager.setState(DownloadManager.STATE_WAITING);
			
		}else{
			
			throw( new DownloadException( "Download::start: download not stopped" ));
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
	remove()
	
		throws DownloadException, DownloadRemovalVetoException
	{
		if ( download_manager.getState() == DownloadManager.STATE_STOPPED){
			
			GlobalManager globalManager = download_manager.getGlobalManager();
			
			try{
				globalManager.removeDownloadManager(download_manager);
				
			}catch( GlobalManagerDownloadRemovalVetoException e ){
				
				throw( new DownloadRemovalVetoException( "Download::remove: operation vetoed" ));
			}
			
		}else{
			
			throw( new DownloadException( "Download::remove: download not stopped" ));
		}
	}
	
	public DownloadStats
	getStats()
	{
		return( new DownloadStatsImpl( download_manager ));
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
		int		state )
	{
		int	prev_state 	= latest_state;
		int	curr_state	= getState();
	
		if ( prev_state != curr_state ){
			
			synchronized( listeners ){
				
				for (int i=0;i<listeners.size();i++){
					
					try{
						((DownloadListener)listeners.get(i)).stateChanged( prev_state, curr_state );
					
					}catch( Throwable e ){
						
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	public void
	downloadComplete()
	{	
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
	
	public void
	scrapeResult(
		TRTrackerScraperResponse	response )
	{
		
	}
	
	public void
	announceResult(
		TRTrackerResponse			response )
	{
		int status = response.getStatus();
		
		String	fail_reason 	= null;
		
		int		total_peers		= 0;
		int		seeds			= 0;
		int		non_seeds		= 0;
		
		if ( status == TRTrackerResponse.ST_ONLINE ){
			
			PEPeerManager	pm = download_manager.getPeerManager();
				
				// use latest peer manager stats if available 
			
			if ( pm != null ){
				
				seeds 		= pm.getNbSeeds();
				non_seeds 	= pm.getNbPeers();
			}
			
			total_peers = response.getPeers().length;
			
		}else{
			
			fail_reason = response.getFailureReason();
		}
		
		synchronized( tracker_listeners ){
			
			for (int i=0;i<tracker_listeners.size();i++){
				
				try{
					if ( status == TRTrackerResponse.ST_ONLINE ){
						
						((DownloadTrackerListener)tracker_listeners.get(i)).announceResult( total_peers, seeds, non_seeds );
						
					}else{
						
						((DownloadTrackerListener)tracker_listeners.get(i)).announceFailed( fail_reason );
					}
				}catch( Throwable e ){
					
					e.printStackTrace();
				}
			}
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
}
