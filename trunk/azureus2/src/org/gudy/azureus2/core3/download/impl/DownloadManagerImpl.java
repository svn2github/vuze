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


import org.gudy.azureus2.core.MessageText;
import org.gudy.azureus2.core.Server;
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
	private Vector	listeners 		= new Vector();
	private Vector	current_peers 	= new Vector();
	private Vector	current_pieces	= new Vector();
  
  private int state;

  private int priority;

  private String errorDetail;

  private GlobalManager globalManager;

  private String torrentFileName;
  private String name;
  // private String nameFromUser = null;

  private int nbPieces;
  private String savePath;
  
  //Used when trackerConnection is not yet created.
  private String trackerUrl;
  
  //The comment field in the metaData
  private String comment;

  private Server server;
  private TOTorrent			torrent;
  private TRTrackerClient 	tracker_client;
  public DiskManager diskManager;
  public PEPeerManager peerManager;
  
  //saved downloaded and uploaded
  private long downloaded;
  private long uploaded;
  
  //Completed (used for auto-starting purposes)
  private int completed;

  private int maxUploads = 4;

  /**
   * @param nameFromUser the new filename for this torrent 
   *
   * @author Rene Leonhardt
   */
  /*
  public boolean setNameFromUser(String nameFromUser) {
	if(!name.equals(nameFromUser) && nameFromUser != null && nameFromUser.trim().length() != 0) {
	  if(state == STATE_STOPPED) {
		this.nameFromUser = nameFromUser;
		name = nameFromUser;
		return true;
	  }
	}
	return false;
  }
	*/
  public DownloadManagerImpl(GlobalManager gm, String torrentFileName, String savePath, boolean stopped) {
	this(gm, torrentFileName, savePath);
	if (this.state == STATE_ERROR)
	  return;
	if (stopped)
	  this.state = STATE_STOPPED;
  }

  public DownloadManagerImpl(GlobalManager gm, String torrentFileName, String savePath) {
	this.globalManager = gm;
	this.maxUploads = COConfigurationManager.getIntParameter("Max Uploads", 4); //$NON-NLS-1$
	this.state = STATE_WAITING;
	this.priority = HIGH_PRIORITY;
	this.torrentFileName = torrentFileName;
	this.savePath = savePath;
    
	readTorrent();
  }

  public void initialize() {
	if(torrent == null) {
	  this.state = STATE_ERROR;
	  return;
	}
	this.state = STATE_INITIALIZING;
    
	startServer();
    
	if (this.state == STATE_WAITING){
    	
		return;
	}
    
	try{
		tracker_client = TRTrackerClientFactory.create( torrent, server.getPort());
    
		diskManager = DiskManagerFactory.create( torrent, savePath);
    
		this.state = STATE_INITIALIZED;
									
	}catch( TRTrackerClientException e ){
		
		e.printStackTrace();
		
		this.state = STATE_ERROR;
	}
  }

  public void startDownload() {
	this.state = STATE_DOWNLOADING;
	peerManager = PEPeerManagerFactory.create(this, server, tracker_client, diskManager);
	peerManager.getStats().setTotalReceivedRaw(downloaded);
	peerManager.getStats().setTotalSent(uploaded);
  }

	private void 
	readTorrent()
	{
		name		= torrentFileName;	// default if things go wrong decoding it
		trackerUrl	= "";
		comment		= "";
		nbPieces	= 0;
		
		try{
  	
			torrent	= TOTorrentFactory.deserialiseFromBEncodedFile(new File(torrentFileName));
		
			// torrent.print();
			
			name = LocaleUtil.getCharsetString( torrent.getName());
			
			/*
			if( nameFromUser != null ){
				
				name = nameFromUser;
			   
				torrent.setName( new String( name.getBytes(LocaleUtil.getCharsetString(name.getBytes())),Constants.DEFAULT_ENCODING )); //$NON-NLS-1$
			 }
			 */
			 
			 trackerUrl = torrent.getAnnounceURL().toString();
         
			 comment = torrent.getComment();
         
			 if ( comment == null ){
         	
				comment	= "";
			 }
			 
			 nbPieces = torrent.getPieces().length;
			 
			 torrent.setAdditionalStringProperty("torrent filename", torrentFileName ); //$NON-NLS-1$ //$NON-NLS-2$

		}catch( TOTorrentException e ){
		
			nbPieces = 0;
        		
			this.state = STATE_ERROR;
 			
			errorDetail = TorrentUtils.exceptionToText( e );
 			
		}catch( UnsupportedEncodingException e ){
		
			nbPieces = 0;
        		
			this.state = STATE_ERROR;
			
			errorDetail = MessageText.getString("DownloadManager.error.unsupportedencoding"); //$NON-NLS-1$
		}
	}


  private void startServer() {
	if(Server.portsFree()) {
	  server = new Server();
	  int port = server.getPort();
	  if (port == 0) {
		this.state = STATE_WAITING;
		//      errorDetail = MessageText.getString("DownloadManager.error.unabletostartserver"); //$NON-NLS-1$
	  }
	} else {
	  this.state = STATE_WAITING;
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

  public String getName() {
	if (diskManager == null)
	  return name;
	return diskManager.getFileName();
  }

  public int getCompleted() {
	if (diskManager == null)
	  return completed;
	if (diskManager.getState() == DiskManager.ALLOCATING || diskManager.getState() == DiskManager.CHECKING || diskManager.getState() == DiskManager.INITIALIZING)
	  return diskManager.getPercentDone();
	else {
	  long total = diskManager.getTotalLength();
	  return total == 0 ? 0 : (int) ((1000 * (total - diskManager.getRemaining())) / total);
	}
  }

  public String getErrorDetails() {
	return errorDetail;
  }

  public long getSize() {
	if (diskManager == null)
	  return 0;
	return diskManager.getTotalLength();
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
		state = DownloadManager.STATE_STOPPING;
		if (peerManager != null) {
		  uploaded = peerManager.getStats().getTotalSentRaw();
		  downloaded = peerManager.getStats().getTotalReceivedRaw();
		  peerManager.stopAll();  
		}      
		if (diskManager != null) {
		  if (diskManager.getState() == DiskManager.READY)
			 diskManager.dumpResumeDataToDisk(true);
		  diskManager.stopIt();
		}
		tracker_client = null;
		peerManager = null;
		state = DownloadManager.STATE_STOPPED;                
	  }
	};
	stopThread.start();
  }

  public void setState(int state) {
	this.state = state;
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

  public String getDownloadSpeed() {
	if (peerManager != null)
	  return peerManager.getStats().getReceptionSpeed();
	return ""; //$NON-NLS-1$
  }

  public String getUploadSpeed() {
	if (peerManager != null)
	  return peerManager.getStats().getSendingSpeed();
	return ""; //$NON-NLS-1$
  }

  public String getTrackerStatus() {
	if (peerManager != null)
	  return peerManager.getTrackerStatus();
	return ""; //$NON-NLS-1$
  }

  public TRTrackerClient 
  getTrackerClient() 
  {
	return( tracker_client );
  }
  
  public String getETA() {
	if (peerManager != null)
	  return peerManager.getETA();
	return ""; //$NON-NLS-1$
  }

  /**
   * @return
   */
  public int getMaxUploads() {
	return maxUploads;
  }

  /**
   * @param i
   */
  public void setMaxUploads(int i) {
	maxUploads = i;
  }

  /**
   * @return
   */
  public int getNbPieces() {
	return nbPieces;
  }

  public String getElapsed() {
	if (peerManager != null)
	  return peerManager.getElpased();
	return ""; //$NON-NLS-1$
  }

  public String getDownloaded() {
	if (peerManager != null) {
	  PEPeerStats ps = peerManager.getStats();
	  return ps.getReallyReceived();
	}
	return ""; //$NON-NLS-1$
  }

  public String getUploaded() {
	if (peerManager != null)
	  return peerManager.getUploaded();
	return ""; //$NON-NLS-1$
  }

  public String getTotalSpeed() {
	if (peerManager != null)
	  return peerManager.getTotalSpeed();
	return ""; //$NON-NLS-1$
  }

  public int getTrackerTime() {
	if (peerManager != null)
	  return peerManager.getTrackerTime();
	return 60;
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

  public String getSavePathForSave() {
	return savePath;
  }

  public String getPieceLength() {
	if (diskManager != null)
	  return DisplayFormatters.formatByteCountToKBEtc(diskManager.getPieceLength());
	return ""; //$NON-NLS-1$
  }

  public String getFileName() {
	if (diskManager != null)
	  return diskManager.getPath() + System.getProperty("file.separator") + diskManager.getFileName(); //$NON-NLS-1$
	return savePath;
  }

  public void received(int length) {
	if (length > 0 && globalManager != null) {
	  globalManager.received(length);
	}
  }
  
  public void discarded(int length) {
	if (length > 0 && globalManager != null) {
	  globalManager.discarded(length);
	}
  }

  public void sent(int length) {
	if (length > 0 && globalManager != null)
	  globalManager.sent(length);
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
  
  public void checkTracker() {
	if(peerManager != null)
	  peerManager.checkTracker();
  }

  /**
   * @return
   */
  public String getComment() {
	return comment;
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
  
  public String getHashFails() {
	if(peerManager != null) {
	  int nbFails = peerManager.getNbHashFails();
	  long size = nbFails * diskManager.getPieceLength();
	  String result = nbFails + " ( ~ " + DisplayFormatters.formatByteCountToKBEtc(size) + " )";
	  return result;
	}
	return "";
  }
  
  public long getDownloadedRaw() {
	if(peerManager != null)
	  return peerManager.getStats().getTotalReceivedRaw();
	return this.downloaded;
  }
  
  public long getUploadedRaw() {
	if(peerManager != null)
	  return peerManager.getStats().getTotalSentRaw();
	return uploaded;    
  }
  
  public void setDownloadedUploaded(long downloaded,long uploaded) {
	this.downloaded = downloaded;
	this.uploaded = uploaded;
  }
  
  public void setCompleted(int completed) {
	this.completed = completed;
  }
  
  public int getShareRatio() {
	long downloaded,uploaded;
	if(peerManager != null) {
	  downloaded = peerManager.getStats().getTotalReceivedRaw();
	  uploaded = peerManager.getStats().getTotalSentRaw();
	} else {
	  downloaded = this.downloaded;
	  uploaded = this.uploaded;
	}
        
	if(downloaded == 0) {
	  return -1;
	}
	else {
	  return (int) ((1000 * uploaded) / downloaded);
	}
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
  		
  		listeners.addElement( listener );
  		
		for (int i=0;i<current_peers.size();i++){
  			
			listener.peerAdded((PEPeerSocket)current_peers.elementAt(i));
		}
		
		for (int i=0;i<current_pieces.size();i++){
  			
			listener.pieceAdded((PEPiece)current_pieces.elementAt(i));
		}
  	}
  }
		
  public void
  removeListener(
	  DownloadManagerListener	listener )
  {
	synchronized( listeners ){
  		
		listeners.removeElement( listener );
	}
  }
 

  public void
  addPeer(
	  PEPeerSocket 		peer )
  {
	synchronized( listeners ){
  		
  		current_peers.addElement( peer );
  		
		for (int i=0;i<listeners.size();i++){
			
			((DownloadManagerListener)listeners.elementAt(i)).peerAdded( peer );
		}
	}
  }
		
  public void
  removePeer(
	  PEPeerSocket		peer )
  {
	synchronized( listeners ){
  		
  		current_peers.removeElement( peer );
  		
		for (int i=0;i<listeners.size();i++){
			
			((DownloadManagerListener)listeners.elementAt(i)).peerRemoved( peer );
		}
	}
 }
		
  public void
  addPiece(
	  PEPiece 	piece )
  {
	synchronized( listeners ){
  		
  		current_pieces.addElement( piece );
  		
		for (int i=0;i<listeners.size();i++){
			
			((DownloadManagerListener)listeners.elementAt(i)).pieceAdded( piece );
		}
	}
 }
		
  public void
  removePiece(
	  PEPiece		piece )
  {
	synchronized( listeners ){
  		
  		current_pieces.removeElement( piece );
  		
		for (int i=0;i<listeners.size();i++){
			
			((DownloadManagerListener)listeners.elementAt(i)).pieceRemoved( piece );
		}
	}
 }

}
