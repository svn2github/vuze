/*
 * File    : DownloadManagerImpl.java
 * Created : 19-Oct-2003
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

package org.gudy.azureus2.core3.download.impl;
/*
 * Created on 30 juin 2003
 *
 */
 
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Vector;


import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.internat.*;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.tracker.client.*;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.download.*;

/**
 * @author Olivier
 * 
 */

public class 
DownloadManagerImpl 
	implements DownloadManager
{
	private Vector	listeners		= new Vector();
	
	private Vector	peer_listeners 	= new Vector();
	private Vector	current_peers 	= new Vector();
	private Vector	current_pieces	= new Vector();
  
	private DownloadManagerStatsImpl	stats;
	
  private boolean startStopLocked;
  private int state;
  private boolean download_ended;
  
  private int prevState = -1;

  private boolean priorityLocked;
  private int priority;

  private String errorDetail;

  private GlobalManager globalManager;

  private String torrentFileName;
  private String name;

  private int nbPieces;
  private String savePath;
  
  //Used when trackerConnection is not yet created.
  private String trackerUrl;
  
  private boolean forcedRecheck;
  
  private PEPeerServer server;
  private TOTorrent			torrent;
  private String torrent_comment;
  private String torrent_created_by;
  
  private TRTrackerClient 			tracker_client;
  private TRTrackerClientListener	tracker_client_listener;
  
  private DiskManager diskManager;
  
  private PEPeerManager 		peerManager;
  private PEPeerManagerListener	peer_manager_listener;
   
  public DownloadManagerImpl(GlobalManager gm, String torrentFileName, String savePath, boolean stopped) {
	this(gm, torrentFileName, savePath);
	if (this.state == STATE_ERROR)
	  return;
	if (stopped)
	  setState( STATE_STOPPED );
  }

  public DownloadManagerImpl(GlobalManager gm, String torrentFileName, String savePath) {
  	
  	stats = new DownloadManagerStatsImpl( this );
  	
	this.globalManager = gm;
	
	stats.setMaxUploads( COConfigurationManager.getIntParameter("Max Uploads", 4));
	 
	this.startStopLocked = false;
  
	this.forcedRecheck = false;
  
    setState( STATE_WAITING );
	
  this.priorityLocked = false;
	this.priority = HIGH_PRIORITY;
	
	this.torrentFileName = torrentFileName;
	
	this.savePath = savePath;
	
	readTorrent();
  }

  public void initialize() {
	if(torrent == null) {
	  setState( STATE_ERROR );
	  return;
	}
	setState( STATE_INITIALIZING );
    
	startServer();
    
	if (this.state == STATE_WAITING){
    	
		return;
	}
    
	try{
		if ( tracker_client != null ){
			
			tracker_client.destroy();
		}
		
		tracker_client = TRTrackerClientFactory.create( torrent, server.getPort());
    
    	tracker_client_listener = 
			new TRTrackerClientListener()
			{
				public void
				 receivedTrackerResponse(
					 TRTrackerResponse	response	)
				 {
				   peerManager.processTrackerResponse( response );
				 }
			
				 public void
				 urlChanged(
				   String		url,
				   boolean		explicit )
				 {  	
				   if ( explicit ){
			  		
					   checkTracker( true );
				   }
				 }
			  
				 public void
				 urlRefresh()
				 {
				   checkTracker( true );
				 }		
			};
			
		tracker_client.addListener( tracker_client_listener );

    initializeDiskManager();

		setState( STATE_INITIALIZED );
									
	}catch( TRTrackerClientException e ){
		
		e.printStackTrace();
		
		setState( STATE_ERROR );
	}
  }

  public void startDownload() {
	setState( STATE_DOWNLOADING );
	
	PEPeerManager temp = PEPeerManagerFactory.create(this, server, tracker_client, diskManager);

	peer_manager_listener = 	
		new PEPeerManagerListener()
		{
			public void
			stateChanged(
				int	new_state )
			{
			}
		};
		
	temp.addListener( peer_manager_listener );
		
	temp.start();
	
	peerManager = temp;		// delay this so peerManager var not available to other threads until it is started
	
	tracker_client.update( true );
  }

	private void 
	readTorrent()
	{
		name				= torrentFileName;	// default if things go wrong decoding it
		trackerUrl			= "";
		torrent_comment		= "";
		torrent_created_by	= "";
		nbPieces			= 0;
		
		try {

			 torrent = TorrentUtils.readFromFile( torrentFileName );
			   	
			 LocaleUtilDecoder	locale_decoder = LocaleUtil.getTorrentEncoding( torrent );
			 	
			 name = locale_decoder.decodeString( torrent.getName());
                  	 
         	 if (torrent.isSimpleTorrent()){
          	
            	File testFile = new File(savePath);
            
            	if (!testFile.isDirectory()){
            		 name = testFile.getName(); 
            	}
          	 }
          
			 trackerUrl = torrent.getAnnounceURL().toString();
         
			 torrent_comment = locale_decoder.decodeString(torrent.getComment());
         
			if ( torrent_comment == null ){
			   torrent_comment	= "";
			}
			
			torrent_created_by = locale_decoder.decodeString(torrent.getCreatedBy());
         
			if ( torrent_created_by == null ){
				torrent_created_by	= "";
			}
			 
			 nbPieces = torrent.getPieces().length;
			 
		}catch( TOTorrentException e ){
		
			nbPieces = 0;
        		
			setState( STATE_ERROR );
 			
			errorDetail = TorrentUtils.exceptionToText( e );
 			
		}catch( UnsupportedEncodingException e ){
		
			nbPieces = 0;
        		
			setState( STATE_ERROR );
			
			errorDetail = MessageText.getString("DownloadManager.error.unsupportedencoding"); //$NON-NLS-1$
		}
	}


  private void startServer() 
  {
  	server = PEPeerServerFactory.create();
  	
	if( server != null ) {
		
	  int port = server.getPort();
	  
	  if (port == 0){
	  	
		setState( STATE_WAITING );
		//      errorDetail = MessageText.getString("DownloadManager.error.unabletostartserver"); //$NON-NLS-1$
	  }
	}else {
		
	  setState( STATE_WAITING );
	}
  }

  /**
   * @return
   */
  public int getState() {
	if (state != STATE_INITIALIZED)
	  return state;
	if (diskManager == null)
	  return STATE_INITIALIZED;
	int diskManagerState = diskManager.getState();
	if (diskManagerState == DiskManager.INITIALIZING)
	  return STATE_INITIALIZED;
	if (diskManagerState == DiskManager.ALLOCATING)
	  return STATE_ALLOCATING;
	if (diskManagerState == DiskManager.CHECKING)
	  return STATE_CHECKING;
	if (diskManagerState == DiskManager.READY)
	  return STATE_READY;
	if (diskManagerState == DiskManager.FAULTY)
	  return STATE_ERROR;
	return STATE_ERROR;
  }
  
  
  /**
   * Returns the 'previous' state.
   */
  public int getPrevState() {
    return prevState;
  }
  
  /**
   * Sets the 'previous' state.
   */
  public void setPrevState(int state) {
    this.prevState = state;
  }
  

  public String getName() {
	if (diskManager == null)
	  return name;
	return diskManager.getFileName();
  }	

  public String getErrorDetails() {
	return errorDetail;
  }

  public long getSize() {
	if (diskManager != null)
	  return diskManager.getTotalLength();
  if(torrent != null)
    return torrent.getSize();
  return 0;
  }

  public boolean[] getPiecesStatus() {
	if (peerManager != null)
	  return peerManager.getPiecesStatus();
	if (diskManager != null)
	  return diskManager.getPiecesStatus();
	return new boolean[nbPieces];
  }

  public void stopIt() {
	Thread stopThread = new Thread() {
	  public void run() {
	  	
		setState( DownloadManager.STATE_STOPPING );

			// kill tracker client first so it doesn't report to peer manager
			// after its been deleted 
			
		if ( tracker_client != null ){
		
			tracker_client.removeListener( tracker_client_listener );
	
			tracker_client.destroy();
			
			tracker_client = null;
		}
		
		if (peerManager != null){
			
		  stats.setSavedDownloadedUploaded( 
				  stats.getSavedDownloaded() + peerManager.getStats().getTotalReceived(),
			 	  stats.getSavedUploaded() + peerManager.getStats().getTotalSent());
      
		  stats.saveDiscarded(stats.getDiscarded());
		  stats.saveHashFails(stats.getHashFails());
			 	  
		  peerManager.removeListener( peer_manager_listener );
		  
		  peerManager.stopAll(); 
		  
		  peerManager = null; 
		  server	  = null;	// clear down ref
		}      
		
		if (diskManager != null){
      stats.setCompleted(stats.getCompleted());
      
		  if (diskManager.getState() == DiskManager.READY){
		    diskManager.dumpResumeDataToDisk(true, false);
		  }
      
		  //update path+name info before termination
		  savePath = diskManager.getPath();
		  name = diskManager.getFileName();
		  
		  diskManager.stopIt();
		  	
		  diskManager = null;
		}
				
		setState( DownloadManager.STATE_STOPPED );                
	  }
	};
	stopThread.setDaemon(true);
	stopThread.start();
  }

  public void setState(int state) {
	this.state = state;
	
	informStateChanged();
  }

  public int getNbSeeds() {
	if (peerManager != null)
	  return peerManager.getNbSeeds();
	return 0;
  }

  public int getNbPeers() {
	if (peerManager != null)
	  return peerManager.getNbPeers();
	return 0;
  }

  

  public String getTrackerStatus() {
	if (tracker_client != null)
	  return tracker_client.getStatusString();
	return ""; //$NON-NLS-1$
  }

  public TRTrackerClient 
  getTrackerClient() 
  {
	return( tracker_client );
  }
 
 
  /**
   * @return
   */
  public int getNbPieces() {
	return nbPieces;
  }


  public int getTrackerTime() {
	if (tracker_client != null){
	
	  return tracker_client.getTimeUntilNextUpdate();
	}
	
	return TRTrackerClient.REFRESH_MINIMUM_SECS;
  }

  /**
   * @return
   */
  public TOTorrent
  getTorrent() 
  {
	return( torrent );
  }

  /**
   * @return
   */
  public String getSavePath() {
	if (diskManager != null)
	  return diskManager.getPath();
	return savePath;
  }

  public String getPieceLength() {
	if (diskManager != null)
	  return DisplayFormatters.formatByteCountToKBEtc(diskManager.getPieceLength());
	return ""; //$NON-NLS-1$
  }

  /**
   * Returns the full path including file/dir name
   */
  public String getFullName() {
	//if diskmanager is already running, use its values
   if (diskManager != null) {
    String path = savePath = diskManager.getPath();
    String fname = name = diskManager.getFileName();
    return FileUtil.smartFullName(path, fname); 
	}
   //otherwise use downloadmanager's values
   else return FileUtil.smartFullName(savePath, name);
  }


  /**
   * @return
   */
  public int getPriority() {
	return priority;
  }

  /**
   * @param i
   */
  public void setPriority(int i) {
	priority = i;
  }

  /**
   * @return
   */
  public String getTorrentFileName() {
	return torrentFileName;
  }

  /**
   * @param string
   */
  public void setTorrentFileName(String string) {
	torrentFileName = string;
  }

  public TRTrackerScraperResponse getTrackerScrapeResponse() {
	if (tracker_client != null  && globalManager != null)
	  return globalManager.getTrackerScraper().scrape(tracker_client);
	else
	  if(torrent != null && globalManager != null)
		return globalManager.getTrackerScraper().scrape(torrent);
	return null;
  }

  /**
   * @param string
   */
  public void setErrorDetail(String string) {
	errorDetail = string;
  }

  
  /**
   * Stops the current download, then restarts it again.
   */
  public void restartDownload(boolean use_fast_resume) {
    if (!use_fast_resume) {
      //invalidate resume info
      diskManager.dumpResumeDataToDisk(false, true);
      readTorrent();
    }
    
    stopIt();
    
    try {
      while (state != DownloadManager.STATE_STOPPED) Thread.sleep(50);
    } catch (Exception ignore) {/*ignore*/}
    
    initialize();
  }
    
  
  public void startDownloadInitialized(boolean initStoppedDownloads) {
	if (getState() == DownloadManager.STATE_WAITING || initStoppedDownloads && getState() == DownloadManager.STATE_STOPPED) {
	  initialize();
	}
	if (getState() == DownloadManager.STATE_READY) {
	  startDownload();
	}
  }

  /** @retun true, if the other DownloadManager has the same size and hash 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object obj)
  {
		// check for object equivalence first!
  		
	if ( this == obj ){
  		
		return( true );
	}
  	
	if(null != obj && obj instanceof DownloadManager) {
    	
	  DownloadManager other = (DownloadManager) obj;
          
	  TOTorrent t1 = getTorrent();
	  TOTorrent t2 = other.getTorrent();
      
	  if ( t1 == null || t2 == null ){
      	
		return( false );	// broken torrents - treat as different so shown
							// as broken
	  }
      
	  try{
      	
		return( Arrays.equals(t1.getHash(), t2.getHash()));
     
	  }catch( TOTorrentException e ){
      	
			// only get here is serious problem with hashing process
      		
		e.printStackTrace();
	  }
	}
    
	return false;
  }
  
  public void 
  checkTracker() 
  {
  	checkTracker(false);
  }
  
  protected void
  checkTracker(
  	boolean	force )
  {
	if( tracker_client != null)
	tracker_client.update( force );
  }

  /**
   * @return
   */
  public String 
  getTorrentComment() {
	return torrent_comment;
  }
  
  public String 
  getTorrentCreatedBy() {
	return torrent_created_by;
  }

  /**
   * @return
   */
  public int getIndex() {
	if(globalManager != null)
	  return globalManager.getIndexOf(this);
	return -1;
  }
  
  public boolean isMoveableUp() {
	if(globalManager != null)
	  return globalManager.isMoveableUp(this);
	return false;
  }
  
  public boolean isMoveableDown() {
	if(globalManager != null)
	  return globalManager.isMoveableDown(this);
	return false;
  }
  
  public void moveUp() {
	if(globalManager != null)
	  globalManager.moveUp(this);
  }
  
  public void moveDown() {
	if(globalManager != null)
	  globalManager.moveDown(this);
  }      
  

	public GlobalManager
	getGlobalManager()
	{
		return( globalManager );
	}
	
  public DiskManager
  getDiskManager()
  {
  	return( diskManager );
  }
  
  public PEPeerManager
  getPeerManager()
  {
  	return( peerManager );
  }

	public void
	addListener(
		DownloadManagerListener	listener )
	{
		synchronized( listeners ){
			
			listeners.addElement(listener);
			
			listener.stateChanged( state );
			
			if ( download_ended ){
				
				listener.downloadComplete();
			}
		}
	}
	
	public void
	removeListener(
		DownloadManagerListener	listener )
	{
		synchronized( listeners ){
			
			listeners.removeElement(listener);
			
		}
	}
	
	protected void
	informStateChanged()
	{
		synchronized( listeners ){
		
			for (int i=0;i<listeners.size();i++){
				
				((DownloadManagerListener)listeners.elementAt(i)).stateChanged( state );
			}
		}
	}
	
	protected void
	informDownloadEnded()
	{
		synchronized( listeners ){
		
			for (int i=0;i<listeners.size();i++){
				
				((DownloadManagerListener)listeners.elementAt(i)).downloadComplete();
			}
		}
	}

  public void
  addPeerListener(
	  DownloadManagerPeerListener	listener )
  {
  	synchronized( peer_listeners ){
  		
  		peer_listeners.addElement( listener );
  		
		for (int i=0;i<current_peers.size();i++){
  			
			listener.peerAdded((PEPeer)current_peers.elementAt(i));
		}
		
		for (int i=0;i<current_pieces.size();i++){
  			
			listener.pieceAdded((PEPiece)current_pieces.elementAt(i));
		}
  	}
  }
		
  public void
  removePeerListener(
	  DownloadManagerPeerListener	listener )
  {
	synchronized( peer_listeners ){
  		
		peer_listeners.removeElement( listener );
	}
  }
 

  public void
  addPeer(
	  PEPeer 		peer )
  {

  current_peers.addElement( peer );
  //Moved the synchronised block AFTER the addElement,
  //as it ended sometimes on a dead-lock situation. Gudy
  synchronized( peer_listeners ){
		for (int i=0;i<peer_listeners.size();i++){
			
			((DownloadManagerPeerListener)peer_listeners.elementAt(i)).peerAdded( peer );
		}
	}
  }
		
  public void
  removePeer(
	  PEPeer		peer )
  {
  	current_peers.removeElement( peer );
    
  	//Moved the synchronised block AFTER the removeElement,
    //as it ended sometimes on a dead-lock situation. Gudy
    synchronized( peer_listeners ){
		for (int i=0;i<peer_listeners.size();i++){
			
			((DownloadManagerPeerListener)peer_listeners.elementAt(i)).peerRemoved( peer );
		}
	}
 }
		
  public void
  addPiece(
	  PEPiece 	piece )
  {
	synchronized( peer_listeners ){
  		
  		current_pieces.addElement( piece );
  		
		for (int i=0;i<peer_listeners.size();i++){
			
			((DownloadManagerPeerListener)peer_listeners.elementAt(i)).pieceAdded( piece );
		}
	}
 }
		
  public void
  removePiece(
	  PEPiece		piece )
  {
	synchronized( peer_listeners ){
  		
  		current_pieces.removeElement( piece );
  		
		for (int i=0;i<peer_listeners.size();i++){
			
			((DownloadManagerPeerListener)peer_listeners.elementAt(i)).pieceRemoved( piece );
		}
	}
 }

	public DownloadManagerStats
	getStats()
	{
		return( stats );
	}
  /**
   * @return Returns the priorityLocked.
   */
  public boolean isPriorityLocked() {
    return priorityLocked;
  }

  /**
   * @param priorityLocked The priorityLocked to set.
   */
  public void setPriorityLocked(boolean priorityLocked) {
    this.priorityLocked = priorityLocked;
  }

  /**
   * @return Returns the startStopLocked.
   */
  public boolean isStartStopLocked() {
    return startStopLocked;
  }

  /**
   * @param startStopLocked The startStopLocked to set.
   */
  public void setStartStopLocked(boolean startStopLocked) {
    this.startStopLocked = startStopLocked;
  }

  /**
   * Is called when a download is finished.
   * Activates alerts for the user.
   *
   * @author Rene Leonhardt
   */
  public void 
  downloadEnded()
  {
  	download_ended = true;
  	
	informDownloadEnded();
  }

  public void initializeDiskManager() {
    if(diskManager == null) {
      diskManager = DiskManagerFactory.create( torrent, FileUtil.smartFullName(savePath, name));
    }
    
    if (forcedRecheck) {
      forcedRecheck = false;
      while (diskManager.getState() == DiskManager.INITIALIZING) {
        try { Thread.sleep(50); } catch (Exception ignore) {}
      }
      restartDownload(false);
    }
  }
  
  public void forceRecheck() {
    forcedRecheck = true;
    setState( STATE_WAITING );
  }
  
}
