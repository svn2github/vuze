/*
 * Created on 30 juin 2003
 *
 */
package org.gudy.azureus2.core;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core2.PeerSocket;
import org.gudy.azureus2.ui.swt.IComponentListener;

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
  private String nameFromUser = null;

  private int nbPieces;
  private String savePath;

  private byte[] hash;
  private Map metaData;
  private Server server;
  private TrackerConnection trackerConnection;
  public DiskManager diskManager;
  public PeerManager peerManager;

  private int maxUploads = 4;

  /**
   * @param nameFromUser the new filename for this torrent 
   *
   * @author Rene Leonhardt
   */
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
    extractMetaInfo();
//    if (this.state == STATE_ERROR) return;
  }

  public void initialize() {
    if(metaData == null) {
      this.state = STATE_ERROR;
      return;
    }
    this.state = STATE_INITIALIZING;
    startServer();
    if (this.state == STATE_WAITING)
      return;

    trackerConnection = new TrackerConnection(metaData, hash, server.getPort());
    diskManager = new DiskManager(metaData, savePath);
    this.state = STATE_INITIALIZED;
  }

  public void startDownload() {
    this.state = STATE_DOWNLOADING;
    peerManager = new PeerManager(this, hash, server, trackerConnection, diskManager);
  }

  private void extractMetaInfo() {
    FileInputStream fis = null;
    try {
      File torrent = new File(torrentFileName);
      if(!torrent.isFile()) {
        name = MessageText.getString("DownloadManager.error.filenotfound"); //$NON-NLS-1$
        nbPieces = 0;
        hash = new byte[20];
        this.state = STATE_ERROR;
        errorDetail = MessageText.getString("DownloadManager.error.filenotfound"); //$NON-NLS-1$
        return;
      }
      if(torrent.length() == 0L) {
        this.state = STATE_ERROR;
        errorDetail = MessageText.getString("DownloadManager.error.fileempty"); //$NON-NLS-1$
        return;
      }
      if(torrent.length() > 1024L * 1024L) {
        this.state = STATE_ERROR;
        errorDetail = MessageText.getString("DownloadManager.error.filetoobig"); //$NON-NLS-1$
        return;
      }
      byte[] buf = new byte[1024];
      int nbRead;
      ByteArrayOutputStream metaInfo = new ByteArrayOutputStream();
      fis = new FileInputStream(torrentFileName);
      while ((nbRead = fis.read(buf)) > 0)
        metaInfo.write(buf, 0, nbRead);
      metaData = BDecoder.decode(metaInfo.toByteArray());
      if(metaData == null) {
        name = torrent.getName();
        state = STATE_ERROR;
        errorDetail = MessageText.getString("DownloadManager.error.filewithouttorrentinfo"); //$NON-NLS-1$
        return;
      }
      Map info = (Map) metaData.get("info"); //$NON-NLS-1$
      name = LocaleUtil.getCharsetString((byte[]) info.get("name")); //$NON-NLS-1$
      if(nameFromUser != null) {
        name = nameFromUser;
        info.put("name", name.getBytes(LocaleUtil.getCharsetString(name.getBytes()))); //$NON-NLS-1$
      }
      byte[] pieces = (byte[]) info.get("pieces"); //$NON-NLS-1$
      nbPieces = pieces.length / 20;
      metaData.put("torrent filename", torrentFileName.getBytes(Constants.DEFAULT_ENCODING)); //$NON-NLS-1$ //$NON-NLS-2$
      SHA1Hasher s = new SHA1Hasher();
      hash = s.calculateHash(BEncoder.encode((Map) metaData.get("info"))); //$NON-NLS-1$
    } catch (UnsupportedEncodingException e) {
      this.state = STATE_ERROR;
      errorDetail = MessageText.getString("DownloadManager.error.unsupportedencoding"); //$NON-NLS-1$
    } catch (IOException e) {
      this.state = STATE_ERROR;
      errorDetail = MessageText.getString("DownloadManager.error.ioerror"); //$NON-NLS-1$
    } catch (NoSuchAlgorithmException e) {
      this.state = STATE_ERROR;
      errorDetail = MessageText.getString("DownloadManager.error.sha1"); //$NON-NLS-1$
    } catch (Exception e) {
      this.state = STATE_ERROR;
      errorDetail = e.getMessage();
    } finally {
      try {
        if (fis != null)
          fis.close();
      } catch (Exception e) {
      }
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
      return 0;
    if (diskManager.getState() == DiskManager.ALLOCATING || diskManager.getState() == DiskManager.CHECKING || diskManager.getState() == DiskManager.INITIALIZING)
      return diskManager.getPercentDone();
    else {
      long remaining = diskManager.getRemaining();
      long total = diskManager.getTotalLength();
      return (int) ((1000 * (total - remaining)) / total);
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
        peerManager = null;
        if (diskManager != null)
          diskManager.stopIt();
        trackerConnection = null;
        state = DownloadManager.STATE_STOPPED;

        globalManager.startWaitingDownloads();

        // remove all free buffers to regain memory 
        ByteBufferPool.getInstance().clearFreeBuffers();
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
    if (peerManager != null)
      return peerManager.getDownloaded();
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
  public byte[] getHash() {
    return hash;
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
    if (length > 0) {
      globalManager.received(length);
    }
  }

  public void sent(int length) {
    if (length > 0)
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
    if (trackerConnection != null)
      return globalManager.getTrackerChecker().getHashData(trackerConnection.getTrackerUrl(), hash);
    else
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
      return getSize() == other.getSize() && Arrays.equals(hash, other.getHash());
    }
    return false;
  }
  
  public void checkTracker() {
    if(peerManager != null)
      peerManager.checkTracker();
  }

}
