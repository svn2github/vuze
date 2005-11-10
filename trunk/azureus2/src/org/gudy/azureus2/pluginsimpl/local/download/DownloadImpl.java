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

import java.io.File;
import java.util.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.category.*;
import org.gudy.azureus2.core3.global.*;
import org.gudy.azureus2.core3.download.*;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.tracker.client.*;

import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;

import org.gudy.azureus2.plugins.peers.*;
import org.gudy.azureus2.pluginsimpl.local.disk.DiskManagerFileInfoImpl;
import org.gudy.azureus2.pluginsimpl.local.peers.*;
import org.gudy.azureus2.pluginsimpl.local.torrent.TorrentImpl;
import org.gudy.azureus2.pluginsimpl.local.torrent.TorrentManagerImpl;
import org.gudy.azureus2.plugins.disk.DiskManager;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadListener;
import org.gudy.azureus2.plugins.download.DownloadPeerListener;
import org.gudy.azureus2.plugins.download.DownloadPropertyListener;
import org.gudy.azureus2.plugins.download.DownloadPropertyEvent;
import org.gudy.azureus2.plugins.download.DownloadTrackerListener;
import org.gudy.azureus2.plugins.download.DownloadAnnounceResult;
import org.gudy.azureus2.plugins.download.DownloadScrapeResult;
import org.gudy.azureus2.plugins.download.DownloadStats;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.download.DownloadRemovalVetoException;
import org.gudy.azureus2.plugins.download.DownloadWillBeRemovedListener;
import org.gudy.azureus2.plugins.download.session.SessionAuthenticator;

import org.gudy.azureus2.core3.internat.MessageText;

public class 
DownloadImpl
	implements 	Download, DownloadManagerListener, 
				DownloadManagerTrackerListener, DownloadManagerPeerListener,
				DownloadManagerStateListener
{
	protected DownloadManager		download_manager;
	protected DownloadStatsImpl		download_stats;
	
	protected int		latest_state		= ST_STOPPED;
	protected boolean 	latest_forcedStart;
	
	protected DownloadAnnounceResultImpl	last_announce_result 	= new DownloadAnnounceResultImpl(this,null);
	protected DownloadScrapeResultImpl		last_scrape_result		= new DownloadScrapeResultImpl( this, null );
	
	protected List		listeners 				= new ArrayList();
	protected AEMonitor	listeners_mon			= new AEMonitor( "Download:L");
	protected List		property_listeners		= new ArrayList();
	protected List		tracker_listeners		= new ArrayList();
	protected AEMonitor	tracker_listeners_mon	= new AEMonitor( "Download:TL");
	protected List		removal_listeners 		= new ArrayList();
	protected AEMonitor	removal_listeners_mon	= new AEMonitor( "Download:RL");
	protected List		peer_listeners			= new ArrayList();
	protected AEMonitor	peer_listeners_mon		= new AEMonitor( "Download:PL");
	
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
		return( convertState( download_manager.getState()) );
	}
	
	public int
	getSubState()
	{
		int	state = getState();
		
		if ( state == ST_STOPPING ){
			
			int	substate = download_manager.getSubState();
			
			if ( substate == DownloadManager.STATE_QUEUED ){
				
				return( ST_QUEUED );
				
			}else if ( substate == DownloadManager.STATE_STOPPED ){
				
				return( ST_STOPPED );
				
			}else if ( substate == DownloadManager.STATE_ERROR ){
				
				return( ST_ERROR );
			}
		}
		
		return( state );
	}
	
	protected int
	convertState(
		int		dm_state )
	{	
		// dm states: waiting -> initialising -> initialized -> 
		//		disk states: allocating -> checking -> ready ->
		// dm states: downloading -> finishing -> seeding -> stopping -> stopped
		
		// "initialize" call takes from waiting -> initialising -> waiting (no port) or initialized (ok)
		// if initialized then disk manager runs through to ready
		// "startdownload" takes ready -> dl etc.
		// "stopIt" takes to stopped which is equiv to ready
		
		int	our_state;
		
		switch( dm_state ){
			case DownloadManager.STATE_WAITING:
			{
				our_state	= ST_WAITING;
				
				break;
			}		
			case DownloadManager.STATE_INITIALIZING:
			case DownloadManager.STATE_INITIALIZED:
			case DownloadManager.STATE_ALLOCATING:
			case DownloadManager.STATE_CHECKING:
			{
				our_state	= ST_PREPARING;
					
				break;
			}
			case DownloadManager.STATE_READY:
			{
				our_state	= ST_READY;
					
				break;
			}
			case DownloadManager.STATE_DOWNLOADING:
			case DownloadManager.STATE_FINISHING:		// finishing download - transit to seeding
			{
				our_state	= ST_DOWNLOADING;
					
				break;
			}
			case DownloadManager.STATE_SEEDING:
			{
				our_state	= ST_SEEDING;
				
				break;
			}
			case DownloadManager.STATE_STOPPING:
			{
				our_state	= ST_STOPPING;
				
				break;
			}
			case DownloadManager.STATE_STOPPED:
			{
				our_state	= ST_STOPPED;
					
				break;
			}
			case DownloadManager.STATE_QUEUED:
			{
				our_state	= ST_QUEUED;
					
				break;
			}
			case DownloadManager.STATE_ERROR:
			{
				our_state	= ST_ERROR;
				
				break;
			}
			default:
			{
				our_state	= ST_ERROR;
			}
		}
		
		return( our_state );
	}
	
	public String
	getErrorStateDetails()
	{
		return( download_manager.getErrorDetails());
	}
	
	public boolean
	getFlag(
		long		flag )
	{
		return( download_manager.getDownloadState().getFlag( flag ));
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
		int	state = download_manager.getState();
		
		if ( state == DownloadManager.STATE_WAITING ){
			
			download_manager.initialize();
			
		}else{
			
			throw( new DownloadException( "Download::initialize: download not waiting (state=" + state + ")" ));
		}
	}
	
	public void
	start()
	
		throws DownloadException
	{
		int	state = download_manager.getState();
		
		if ( state == DownloadManager.STATE_READY ){
						
			download_manager.startDownload();
										
		}else{
			
			throw( new DownloadException( "Download::start: download not ready (state=" + state + ")" ));
		}
	}
	
	public void
	restart()
	
		throws DownloadException
	{
		int	state = download_manager.getState();
		
		if ( 	state == DownloadManager.STATE_STOPPED ||
				state == DownloadManager.STATE_QUEUED ){
			
			download_manager.setStateWaiting();
			
		}else{
			
			throw( new DownloadException( "Download::restart: download already running (state=" + state + ")" ));
		}
	}
	
	public void
	stop()
	
		throws DownloadException
	{
		if ( download_manager.getState() != DownloadManager.STATE_STOPPED){
			
			download_manager.stopIt( DownloadManager.STATE_STOPPED, false, false );
			
		}else{
			
			throw( new DownloadException( "Download::stop: download already stopped" ));
		}
	}
	
	public void
	stopAndQueue()
	
		throws DownloadException
	{
		if ( download_manager.getState() != DownloadManager.STATE_QUEUED){
			
			download_manager.stopIt( DownloadManager.STATE_QUEUED, false, false );
		}else{
			
			throw( new DownloadException( "Download::stopAndQueue: download already queued" ));
		}
	}
	
	public void
	recheckData()
	
		throws DownloadException
	{
		if ( !download_manager.canForceRecheck()){
			
			throw( new DownloadException( "Download::recheckData: download must be stopped, quued or in error state" ));
		}
		
		download_manager.forceRecheck();
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
	getPosition()
	{
		return download_manager.getPosition();
	}
	
	public long
	getCreationTime()
	{
		return( download_manager.getCreationTime());
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
		return download_manager.getDisplayName();
	}

  public String getTorrentFileName() {
    return download_manager.getTorrentFileName();
  }
  
  public String getCategoryName() {
    Category category = download_manager.getDownloadState().getCategory();
    if (category == null)
      category = CategoryManager.getCategory(Category.TYPE_UNCATEGORIZED);

    if (category == null)
      return null;
    return category.getName();
  }
    
  
  public String
  getAttribute(
  	TorrentAttribute		attribute )
  {
  	String	name = convertAttribute( attribute );
  	
  	if ( name != null ){
  		
  		return( download_manager.getDownloadState().getAttribute( name ));
  	}
  	
  	return( null );
  }
  
  public String[]
  getListAttribute(
  	TorrentAttribute		attribute )
  {
	  	String	name = convertAttribute( attribute );
	  	
	  	if ( name != null ){
	  		
	  		return( download_manager.getDownloadState().getListAttribute( name ));
	  	}
	  	
	  	return( null );
  }
  
  public void
  setMapAttribute(
	TorrentAttribute		attribute,
	Map						value )
  {
	  	String	name = convertAttribute( attribute );
	  	
	  	if ( name != null ){
			
			download_manager.getDownloadState().setMapAttribute( name, value );
	  	}
  }
  
  public Map
  getMapAttribute(
	TorrentAttribute		attribute )
  {
	  	String	name = convertAttribute( attribute );
	  	
	  	if ( name != null ){
	  		
	  		return( download_manager.getDownloadState().getMapAttribute( name ));
	  	}
	  	
	  	return( null );
  }
  
  public void
  setAttribute(
  	TorrentAttribute		attribute,
	String					value )
  {
 	String	name = convertAttribute( attribute );
  	
  	if ( name != null ){

  		download_manager.getDownloadState().setAttribute( name, value );
  	}
  }
  
  protected String
  convertAttribute(
  	TorrentAttribute		attribute )
  {
 	if ( attribute.getName() == TorrentAttribute.TA_CATEGORY ){
  		
  		return( DownloadManagerState.AT_CATEGORY );
  		
 	}else if ( attribute.getName() == TorrentAttribute.TA_NETWORKS ){
  		
		return( DownloadManagerState.AT_NETWORKS );
		
 	}else if ( attribute.getName() == TorrentAttribute.TA_TRACKER_CLIENT_EXTENSIONS ){
  		
		return( DownloadManagerState.AT_TRACKER_CLIENT_EXTENSIONS );
	  		
	}else if ( attribute.getName() == TorrentAttribute.TA_PEER_SOURCES ){
  		
		return( DownloadManagerState.AT_PEER_SOURCES );
  		
	}else if ( attribute.getName().startsWith( "Plugin." )){
  		
		return( attribute.getName());
  		
  	}else{
  		
  		Debug.out( "Can't convert attribute '" + attribute.getName() + "'" );
  		
  		return( null );
  	}
  }
  
  protected TorrentAttribute
  convertAttribute(
  	String			name )
  {
 	if ( name.equals( DownloadManagerState.AT_CATEGORY )){
  		
  		return( TorrentManagerImpl.getSingleton().getAttribute( TorrentAttribute.TA_CATEGORY ));
  		
	}else if ( name.equals( DownloadManagerState.AT_NETWORKS )){
	  		
	  	return( TorrentManagerImpl.getSingleton().getAttribute( TorrentAttribute.TA_NETWORKS ));
	  		
	}else if ( name.equals( DownloadManagerState.AT_PEER_SOURCES )){
  		
		return( TorrentManagerImpl.getSingleton().getAttribute( TorrentAttribute.TA_PEER_SOURCES ));
		
	}else if ( name.equals( DownloadManagerState.AT_TRACKER_CLIENT_EXTENSIONS )){
  		
		return( TorrentManagerImpl.getSingleton().getAttribute( TorrentAttribute.TA_TRACKER_CLIENT_EXTENSIONS ));
		
	}else if ( name.startsWith( "Plugin." )){
  		
		return( TorrentManagerImpl.getSingleton().getAttribute( name ));
	  		
  	}else{
  		
  		return( null );
  	}
  }
  
  public void setCategory(String sName) {
    Category category = CategoryManager.getCategory(sName);
    if (category == null)
      category = CategoryManager.createCategory(sName);
    download_manager.getDownloadState().setCategory(category);
  }

  public boolean isPersistent() {
    return download_manager.isPersistent();
  }

	public void
	remove()
	
		throws DownloadException, DownloadRemovalVetoException
	{
		remove( false, false );
	}
	
	public void
	remove(
		boolean	delete_torrent,
		boolean	delete_data )
	
		throws DownloadException, DownloadRemovalVetoException
	{
		int	dl_state = download_manager.getState();
		
		if ( 	dl_state == DownloadManager.STATE_STOPPED 	|| 
				dl_state == DownloadManager.STATE_ERROR 	||
				dl_state == DownloadManager.STATE_QUEUED ){
			
			GlobalManager globalManager = download_manager.getGlobalManager();
			
			try{
				if ( delete_torrent || delete_data ){
					
					download_manager.stopIt( dl_state, delete_torrent, delete_data );
				}
				
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
	
 	public boolean
	isComplete()
 	{
 		int	state = getState();
 		
 		return( state == ST_SEEDING || download_manager.isDownloadComplete());
 	}
 	
 	public boolean
 	isChecking()
 	{
 		org.gudy.azureus2.core3.disk.DiskManager	dm = download_manager.getDiskManager();
 		
 		if ( dm != null ){
 			
 			return( dm.isChecking());
 		}
 		
 		return( false );
 	}
 	
	protected void
	isRemovable()
		throws DownloadRemovalVetoException
	{
			// no sync required, see update code
		
		for (int i=0;i<removal_listeners.size();i++){
			
			try{
				((DownloadWillBeRemovedListener)removal_listeners.get(i)).downloadWillBeRemoved(this);
				
			}catch( DownloadRemovalVetoException e ){
				
				throw( e );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
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
		int				state )
	{
		int	prev_state 	= latest_state;
		
		latest_state	= convertState(state);
		
		// System.out.println("Plug:prev = " + prev_state + ", curr = " + latest_state );
		
		boolean curr_forcedStart = isForceStart();
	
		if ( prev_state != latest_state || latest_forcedStart != curr_forcedStart ){
			
			latest_forcedStart = curr_forcedStart;
			
			for (int i=0;i<listeners.size();i++){
				
				try{
					((DownloadListener)listeners.get(i)).stateChanged( this, prev_state, latest_state );
				
				}catch( Throwable e ){
					
					Debug.printStackTrace( e );
				}
			}
		}
	}
	
	public void
	downloadComplete(DownloadManager manager)
	{	
	}

	public void 
	completionChanged(
		DownloadManager 	manager, 
		boolean 			bCompleted) 
	{
	}
	
  public void 
  positionChanged(
  	DownloadManager download, 
    int oldPosition, 
	int newPosition) 
  {	
	for (int i = 0; i < listeners.size(); i++) {
		try {
			((DownloadListener)listeners.get(i)).positionChanged(this, oldPosition, newPosition);
		} catch (Throwable e) {
			Debug.printStackTrace( e );
		}
	}
  }

	public void
	addListener(
		DownloadListener	l )
	{
		try{
			listeners_mon.enter();
			
			List	new_listeners = new ArrayList( listeners );
			
			new_listeners.add(l);
			
			listeners	= new_listeners;
		}finally{
			
			listeners_mon.exit();
		}
	}
	
	public void
	removeListener(
		DownloadListener	l )
	{
		try{
			listeners_mon.enter();
			
			List	new_listeners	= new ArrayList(listeners);
			
			new_listeners.remove(l);
			
			listeners	= new_listeners;
		}finally{
			
			listeners_mon.exit();
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
		
		for (int i=0;i<tracker_listeners.size();i++){
			
			try{						
				((DownloadTrackerListener)tracker_listeners.get(i)).scrapeResult( last_scrape_result );

			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}
	}
	
	public void
	announceResult(
		TRTrackerAnnouncerResponse			response )
	{
		last_announce_result.setContent( response );
		
		List	tracker_listeners_ref = tracker_listeners;
		
		for (int i=0;i<tracker_listeners_ref.size();i++){
			
			try{						
				((DownloadTrackerListener)tracker_listeners_ref.get(i)).announceResult( last_announce_result );

			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}
	}
	
	public void
	setAnnounceResult(
		DownloadAnnounceResult	result )
	{
		download_manager.setAnnounceResult( result );
	}
	
	public void
	setScrapeResult(
		DownloadScrapeResult	result )
	{
		download_manager.setScrapeResult( result );
	}
	
	public void
	stateChanged(
		DownloadManagerState			state,
		DownloadManagerStateEvent		event )
	{
		final int type = event.getType();
		
		if ( 	type == DownloadManagerStateEvent.ET_ATTRIBUTE_WRITTEN ||
				type == DownloadManagerStateEvent.ET_ATTRIBUTE_WILL_BE_READ 	){
			
			String	name = (String)event.getData();
			
			List	property_listeners_ref = property_listeners;
			
			final TorrentAttribute	attr = convertAttribute( name );
			
			if ( attr != null ){
				
				for (int i=0;i<property_listeners_ref.size();i++){
					
					try{						
						((DownloadPropertyListener)property_listeners_ref.get(i)).propertyChanged(
								this,
								new DownloadPropertyEvent()
								{
									public int
									getType()
									{
										return( type==DownloadManagerStateEvent.ET_ATTRIBUTE_WRITTEN
													?DownloadPropertyEvent.PT_TORRENT_ATTRIBUTE_WRITTEN
													:DownloadPropertyEvent.PT_TORRENT_ATTRIBUTE_WILL_BE_READ	);
									}
									
									public Object
									getData()
									{
										return( attr );
									}
								});

					}catch( Throwable e ){
						
						Debug.printStackTrace( e );
					}
				}
			}			
		}
	}
	
	public void
	addPropertyListener(
		DownloadPropertyListener	l )
	{
		try{
			tracker_listeners_mon.enter();
	
			List	new_property_listeners = new ArrayList( property_listeners );
			
			new_property_listeners.add( l );
			
			property_listeners	= new_property_listeners;
			
			if ( property_listeners.size() == 1 ){
				
				download_manager.getDownloadState().addListener( this );
			}
		}finally{
			
			tracker_listeners_mon.exit();
		}	
	}
	
	public void
	removePropertyListener(
		DownloadPropertyListener	l )
	{
		try{
			tracker_listeners_mon.enter();
			
			List	new_property_listeners	= new ArrayList( property_listeners );
			
			new_property_listeners.remove( l );
			
			property_listeners	= new_property_listeners;
			
			if ( property_listeners.size() == 0 ){
				
				download_manager.getDownloadState().removeListener( this );
			}
		}finally{
			
			tracker_listeners_mon.exit();
		}		
	}
	
	public void
	torrentChanged()
	{
		TRTrackerAnnouncer	client = download_manager.getTrackerClient();
		
		if ( client != null ){
			
			client.resetTrackerUrl(true);
		}
	}
	
	public void
	addTrackerListener(
		DownloadTrackerListener	l )
	{
		try{
			tracker_listeners_mon.enter();
	
			List	new_tracker_listeners = new ArrayList( tracker_listeners );
			
			new_tracker_listeners.add( l );
			
			tracker_listeners	= new_tracker_listeners;
			
			if ( tracker_listeners.size() == 1 ){
				
				download_manager.addTrackerListener( this );
			}
		}finally{
			
			tracker_listeners_mon.exit();
		}
		
		l.announceResult( last_announce_result );
		
		l.scrapeResult( last_scrape_result );
	}
	
	public void
	removeTrackerListener(
		DownloadTrackerListener	l )
	{
		try{
			tracker_listeners_mon.enter();
			
			List	new_tracker_listeners	= new ArrayList( tracker_listeners );
			
			new_tracker_listeners.remove( l );
			
			tracker_listeners	= new_tracker_listeners;
			
			if ( tracker_listeners.size() == 0 ){
				
				download_manager.removeTrackerListener( this );
			}
		}finally{
			
			tracker_listeners_mon.exit();
		}
	}
	
	public void
	addDownloadWillBeRemovedListener(
		DownloadWillBeRemovedListener	l )
	{
		try{
			removal_listeners_mon.enter();
			
			List	new_removal_listeners	= new ArrayList( removal_listeners );
			
			new_removal_listeners.add(l);
			
			removal_listeners	= new_removal_listeners;
			
		}finally{
			
			removal_listeners_mon.exit();
		}
	}
	
	public void
	removeDownloadWillBeRemovedListener(
		DownloadWillBeRemovedListener	l ) 
	{
		try{
			removal_listeners_mon.enter();
			
			List	new_removal_listeners	= new ArrayList( removal_listeners );
			
			new_removal_listeners.remove(l);
			
			removal_listeners	= new_removal_listeners;
			
		}finally{
			
			removal_listeners_mon.exit();
		}
	}
	
	public void
	addPeerListener(
		DownloadPeerListener	l )
	{
		try{
			peer_listeners_mon.enter();
		
			List	new_peer_listeners	= new ArrayList( peer_listeners );
			
			new_peer_listeners.add( l );
			
			peer_listeners	= new_peer_listeners;
			
			if ( peer_listeners.size() == 1 ){
				
				download_manager.addPeerListener( this );
			}
		}finally{
			
			peer_listeners_mon.exit();
		}
	}
	
	
	public void
	removePeerListener(
		DownloadPeerListener	l )
	{
		try{
			peer_listeners_mon.enter();

			List	new_peer_listeners	= new ArrayList( peer_listeners );
			
			new_peer_listeners.remove( l );
			
			peer_listeners	= new_peer_listeners;
			
			if ( peer_listeners.size() == 0 ){
				
				download_manager.removePeerListener( this );
			}
		}finally{
			
			peer_listeners_mon.exit();
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
	
 	public PeerManager
	getPeerManager()
 	{
 		PEPeerManager	pm = download_manager.getPeerManager();
 		
 		if ( pm == null ){
 			
 			return( null );
 		}
 		
 		return( PeerManagerImpl.getPeerManager( pm));
 	}
 	
	public DiskManager
	getDiskManager()
	{
		PeerManager	pm = getPeerManager();
		
		if ( pm != null ){
			
			return( pm.getDiskManager());
		}
		
		return( null );
	}
	
	
	
	public DiskManagerFileInfo[]
	getDiskManagerFileInfo()
	{
		org.gudy.azureus2.core3.disk.DiskManagerFileInfo[] info = download_manager.getDiskManagerFileInfo();
		
		if ( info == null ){
			
			return( new DiskManagerFileInfo[0] );
		}
		
		DiskManagerFileInfo[]	res = new DiskManagerFileInfo[info.length];
		
		for (int i=0;i<res.length;i++){
			
			res[i] = new DiskManagerFileInfoImpl( info[i] );
		}
		
		return( res );
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
	
 	public void
	setMaximumDownloadKBPerSecond(
		int		kb )
 	{
 		download_manager.getStats().setDownloadRateLimitBytesPerSecond( kb < 0 ? 0 : kb*1024 );
 	}
  	
  	public int
	getMaximumDownloadKBPerSecond()
  	{
  		return( download_manager.getStats().getDownloadRateLimitBytesPerSecond() /1024 );
  	}
    
  	public int getUploadRateLimitBytesPerSecond() {
      return download_manager.getStats().getUploadRateLimitBytesPerSecond();
  	}

  	public void setUploadRateLimitBytesPerSecond( int max_rate_bps ) {
      download_manager.getStats().setUploadRateLimitBytesPerSecond( max_rate_bps );
  	}
    
  	
	public String
	getSavePath()
 	{
		return( download_manager.getSaveLocation().toString());
 	}
	
	public void
  	moveDataFiles(
  		File	new_parent_dir )
  	
  		throws DownloadException
  	{
 		try{
 			download_manager.moveDataFiles( new_parent_dir );
 			
 		}catch( DownloadManagerException e ){
 			
 			throw( new DownloadException("move operation failed", e ));
 		}
  	}
  	
  	public void
  	moveTorrentFile(
  		File	new_parent_dir )
  	
  		throws DownloadException
 	{
		try{
 			download_manager.moveTorrentFile( new_parent_dir );
 			
 		}catch( DownloadManagerException e ){
 			
 			throw( new DownloadException("move operation failed", e ));
 		}  	
 	}
  	
 	public void
	requestTrackerAnnounce()
 	{
 		download_manager.checkTracker();
 	}
 	
  public byte[] getDownloadPeerId() {
    TRTrackerAnnouncer announcer = download_manager.getTrackerClient();
    if(announcer == null) return null;
    return announcer.getPeerId();
  }
  
  
  public boolean isMessagingEnabled() {  return download_manager.isAZMessagingEnabled();  }

  public void setMessagingEnabled( boolean enabled ) {
    download_manager.setAZMessagingEnabled( enabled );
  }
  
  
  public void setSessionAuthenticator( SessionAuthenticator auth ) {
    //TODO
  }
  
  
 	// Deprecated methods

  public int getPriority() {
    return 0;
  }
  
  public boolean isPriorityLocked() {
    return false;
  }  

  public void setPriority(int priority) {
  }
}
