/*
 * Created on 30 juin 2003
 *
 */
package org.gudy.azureus2.core;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

import org.gudy.azureus2.core2.PeerSocket;
import org.gudy.azureus2.ui.swt.IComponentListener;

import org.gudy.azureus2.core3.internat.*;
import org.gudy.azureus2.core3.tracker.client.*;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.util.*;

/**
 * @author Olivier
 * 
 */
public class DownloadManager extends Component {

  private int state;
  public static final int STATE_WAITING = 0;
  public static final int STATE_INITIALIZING = 5;
  public static final int STATE_INITIALIZED = 10;
  public static final int STATE_ALLOCATING = 20;
  public static final int STATE_CHECKING = 30;
  public static final int STATE_READY = 40;
  public static final int STATE_DOWNLOADING = 50;
  public static final int STATE_SEEDING = 60;
  public static final int STATE_STOPPING = 65;
  public static final int STATE_STOPPED = 70;
  public static final int STATE_ERROR = 100;
  // indicates, that there is already a DownloadManager with the same size and hash in the list
  public static final int STATE_DUPLICATE = 200;

  private int priority;
  public static final int LOW_PRIORITY = 1;
  public static final int HIGH_PRIORITY = 2;

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
  private TRTrackerClient 	trackerConnection;
  public DiskManager diskManager;
  public PeerManager peerManager;
  
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
  public DownloadManager(GlobalManager gm, String torrentFileName, String savePath, boolean stopped) {
    this(gm, torrentFileName, savePath);
    if (this.state == STATE_ERROR)
      return;
    if (stopped)
      this.state = STATE_STOPPED;
  }

  public DownloadManager(GlobalManager gm, String torrentFileName, String savePath) {
    this.globalManager = gm;
    this.maxUploads = ConfigurationManager.getInstance().getIntParameter("Max Uploads", 4); //$NON-NLS-1$
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
    	trackerConnection = TRTrackerClientFactory.create( torrent, server.getPort());
    
		diskManager = new DiskManager( torrent, savePath);
    
		this.state = STATE_INITIALIZED;
									
	}catch( TRTrackerClientException e ){
		
		e.printStackTrace();
		
		this.state = STATE_ERROR;
	}
  }

  public void startDownload() {
    this.state = STATE_DOWNLOADING;
    peerManager = new PeerManager(this, getHash(), server, trackerConnection, diskManager);
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
  	
			torrent	= TOTorrentFactory.deserialiseFromFile(new File(torrentFileName));
		
			// torrent.print();
			
			name = LocaleUtil.getCharsetString( torrent.getName().getBytes( Constants.DEFAULT_ENCODING));
			
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
 			
  			int	reason = e.getReason();
  			
 	     	if ( reason == TOTorrentException.RT_FILE_NOT_FOUND ){
 	     	        		 		
        		errorDetail = MessageText.getString("DownloadManager.error.filenotfound"); //$NON-NLS-1$
        		
 	     	}else if ( reason == TOTorrentException.RT_ZERO_LENGTH ){
     
	        	errorDetail = MessageText.getString("DownloadManager.error.fileempty"); //$NON-NLS-1$
        
 	     	}else if ( reason == TOTorrentException.RT_TOO_BIG ){
 	     		
		        errorDetail = MessageText.getString("DownloadManager.error.filetoobig"); //$NON-NLS-1$
		        
			}else if ( reason == TOTorrentException.RT_DECODE_FAILS ){
 
 		        errorDetail = MessageText.getString("DownloadManager.error.filewithouttorrentinfo"); //$NON-NLS-1$
 		        
 	     	}else if ( reason == TOTorrentException.RT_UNSUPPORTED_ENCODING ){
 	     		
				errorDetail = MessageText.getString("DownloadManager.error.unsupportedencoding"); //$NON-NLS-1$
				
			}else if ( reason == TOTorrentException.RT_READ_FAILS ){

				errorDetail = MessageText.getString("DownloadManager.error.ioerror"); //$NON-NLS-1$
				
			}else if ( reason == TOTorrentException.RT_HASH_FAILS ){

 				errorDetail = MessageText.getString("DownloadManager.error.sha1"); //$NON-NLS-1$
 	     	}else{
 	     
      			errorDetail = e.getMessage();
 	     	}
 			
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
        if (peerManager != null)
          peerManager.stopAll();        
        if (diskManager != null) {
          if (diskManager.getState() == DiskManager.READY)
             diskManager.dumpResumeDataToDisk(true);
          diskManager.stopIt();
        }
        trackerConnection = null;
        peerManager = null;
        state = DownloadManager.STATE_STOPPED;
                
        //globalManager.startWaitingDownloads();

        // remove all free buffers to regain memory 
        // Gudy :SILLY IDEA as those are DIRECT buffers, not managed by GC ... 
        //ByteBufferPool.getInstance().clearFreeBuffers();
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

  public String getTrackerUrl() {
    if (trackerConnection != null)
      return trackerConnection.getTrackerUrl();
    return null;
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
      PeerStats ps = peerManager.getStats();
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
  public byte[] 
  getHash() 
  {
  	try{
  	
    	return( torrent == null?new byte[20]:torrent.getHash());
    	
  	}catch( TOTorrentException e ){
  	
  		e.printStackTrace();
  		
  		return( new byte[20] );
  	}
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
      return PeerStats.format(diskManager.getPieceLength());
    return ""; //$NON-NLS-1$
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IComponent#addListener(org.gudy.azureus2.ui.swt.IComponentListener)
   */
  public void addListener(IComponentListener listener) {
    // TODO Auto-generated method stub
    super.addListener(listener);
    if (peerManager != null) {
      List connections = peerManager.get_connections();
      synchronized (connections) {
        for (int i = 0; i < connections.size(); i++) {
          PeerSocket ps = (PeerSocket) connections.get(i);
          if (ps.getState() == PeerSocket.TRANSFERING)
            objectAdded(ps);
        }
      }
      Piece[] pieces = peerManager.getPieces();
      for (int i = 0; i < pieces.length; i++) {
        if (pieces[i] != null) {
          objectAdded(pieces[i]);
        }
      }
    }
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

  public HashData getHashData() {
    if (trackerConnection != null  && globalManager != null)
      return globalManager.getTrackerChecker().getHashData(trackerConnection.getTrackerUrl(), getHash());
    else
      if(trackerUrl != null && globalManager != null)
        return globalManager.getTrackerChecker().getHashData(trackerUrl, getHash());
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
  public boolean equals(Object obj) {
    if(null != obj && obj instanceof DownloadManager) {
      DownloadManager other = (DownloadManager) obj;
      return getSize() == other.getSize() && Arrays.equals(getHash(), other.getHash());
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
  public TRTrackerClient getTrackerConnection() {
    return trackerConnection;
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
      String result = nbFails + " ( ~ " + PeerStats.format(size) + " )";
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

}
