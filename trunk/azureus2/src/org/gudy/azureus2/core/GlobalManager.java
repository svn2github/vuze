/*
 * Created on 30 juin 2003
 *
 */
package org.gudy.azureus2.core;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.ui.swt.IComponentListener;

/**
 * @author Olivier
 * 
 */
public class GlobalManager extends Component {

  private List managers;
  private Checker checker;
  private PeerStats stats;
  private TrackerChecker trackerChecker;

  public class Checker extends Thread {
    boolean finished = false;
    int loopFactor;

    public Checker() {
      super("Global Status Checker");
      loopFactor = 0;
    }

    public void run() {
      while (!finished) {

        loopFactor++;
        if (loopFactor >= 6000) {
          loopFactor = 0;
          trackerChecker.update();
        }

        synchronized (managers) {
          int nbStarted = 0;
          int nbDownloading = 0;
          for (int i = 0; i < managers.size(); i++) {
            DownloadManager manager = (DownloadManager) managers.get(i);
            if (manager.getState() == DownloadManager.STATE_DOWNLOADING) {
              nbStarted++;
              nbDownloading++;
            }
            if (manager.getState() == DownloadManager.STATE_SEEDING) {
              nbStarted++;
            }
          }
          boolean alreadyOneAllocatingOrChecking = false;
          for (int i = 0; i < managers.size(); i++) {
            DownloadManager manager = (DownloadManager) managers.get(i);
            if ((manager.getState() == DownloadManager.STATE_WAITING) && !alreadyOneAllocatingOrChecking) {
              manager.initialize();
              alreadyOneAllocatingOrChecking = true;
            }
            int nbMax = ConfigurationManager.getInstance().getIntParameter("max active torrents", 4);
            int nbMaxDownloads = ConfigurationManager.getInstance().getIntParameter("max downloads", 4);
            if (manager.getState() == DownloadManager.STATE_READY && ((nbMax == 0) || (nbStarted < nbMax)) && ((nbMaxDownloads == 0) || (nbDownloading < nbMaxDownloads))) {
              manager.startDownload();
              nbStarted++;
              if(manager.getCompleted() != 1000)
                nbDownloading++;
            }

            if (((manager.getState() == DownloadManager.STATE_ALLOCATING)
              || (manager.getState() == DownloadManager.STATE_CHECKING)
              || (manager.getState() == DownloadManager.STATE_INITIALIZED))) {
              alreadyOneAllocatingOrChecking = true;
            }
            
            if(manager.getState() == DownloadManager.STATE_ERROR) {
              DiskManager dm = manager.diskManager;
              if(dm != null && dm.getState() == DiskManager.FAULTY)
                manager.setErrorDetail(dm.getErrorMessage());
            }

            if ((manager.getState() == DownloadManager.STATE_SEEDING)
              && (manager.getPriority() == DownloadManager.HIGH_PRIORITY)
              && ConfigurationManager.getInstance().getBooleanParameter("Switch Priority", true)) {
              manager.setPriority(DownloadManager.LOW_PRIORITY);
            }

            if ((manager.getState() == DownloadManager.STATE_ERROR)
              && (manager.getErrorDetails() != null && manager.getErrorDetails().equals("File Not Found"))) {
              removeDownloadManager(manager);
            }
          }
        }
        try {
          Thread.sleep(100);
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    public void stopIt() {
      finished = true;
    }
  }

  public GlobalManager() {
    stats = new PeerStats(0);
    managers = new ArrayList();
    trackerChecker = new TrackerChecker();
    loadDownloads();
    checker = new Checker();
    checker.start();
  }

  public void addDownloadManager(DownloadManager manager) {
    synchronized (managers) {
      managers.add(manager);
    }

    this.objectAdded(manager);
    saveDownloads();
  }

  public List getDownloadManagers() {
    return managers;
  }

  public void removeDownloadManager(DownloadManager manager) {
    synchronized (managers) {
      managers.remove(manager);
    }
    this.objectRemoved(manager);
    saveDownloads();
  }

  public void startWaitingDownloads() {
    synchronized (managers) {
      for (int i = 0; i < managers.size(); i++) {
        DownloadManager manager = (DownloadManager) managers.get(i);
        if (manager.getState() == DownloadManager.STATE_WAITING) {
          manager.startDownloadInitialized(false);
        }
      }
    }
  }

  public void stopAll() {
    checker.stopIt();
    saveDownloads();
    while (managers.size() != 0) {
      DownloadManager manager = (DownloadManager) managers.remove(0);
      manager.stopIt();
    }
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IComponent#addListener(org.gudy.azureus2.ui.swt.IComponentListener)
   */
  public void addListener(IComponentListener listener) {
    // TODO Auto-generated method stub
    super.addListener(listener);
    synchronized (managers) {
      for (int i = 0; i < managers.size(); i++) {
        listener.objectAdded(managers.get(i));
      }
    }
  }

  public void received(int length) {
    stats.received(length);
  }

  public void sent(int length) {
    stats.sent(length);
  }

  public String getDownloadSpeed() {
    return stats.getReceptionSpeed();
  }

  public String getUploadSpeed() {
    return stats.getSendingSpeed();
  }

  private void loadDownloads() {
    FileInputStream fin = null;
    BufferedInputStream bin = null;
    try {
      //open the file
      File configFile = getApplicationFile("downloads.config");
      fin = new FileInputStream(configFile);
      bin = new BufferedInputStream(fin);
      Map map = BDecoder.decode(bin);
      boolean debug = Boolean.getBoolean("debug");
      Iterator iter = map.values().iterator();
      while (iter.hasNext()) {
        Map mDownload = (Map) iter.next();
        try {
          String fileName = new String((byte[]) mDownload.get("torrent"), "UTF-16");
          String savePath = new String((byte[]) mDownload.get("path"), "UTF-16");
          int nbUploads = ((Long) mDownload.get("uploads")).intValue();
          int stopped = debug ? 1 : ((Long) mDownload.get("stopped")).intValue();
          DownloadManager dm = new DownloadManager(this, fileName, savePath, stopped == 1);
          dm.setMaxUploads(nbUploads);
          this.addDownloadManager(dm);
        }
        catch (UnsupportedEncodingException e1) {
          //Do nothing and process next.
        }
      }
    }
    catch (FileNotFoundException e) {
      //Do nothing
    }
    catch (Exception e) {
      // TODO Auto-generated catch block     
    } finally {
      try {
        if (bin != null)
          bin.close();
      } catch (Exception e) {
      }
      try {
        if (fin != null)
          fin.close();
      } catch (Exception e) {
      }
    }
  }

  private void saveDownloads() {
//    if(Boolean.getBoolean("debug")) return;

    Map map = new HashMap();
    for (int i = 0; i < managers.size(); i++) {
      DownloadManager dm = (DownloadManager) managers.get(i);
      Map dmMap = new HashMap();
      dmMap.put("torrent", dm.getTorrentFileName().getBytes());
      dmMap.put("path", dm.getSavePathForSave());
      dmMap.put("uploads", new Long(dm.getMaxUploads()));
      int stopped = 0;
      if (dm.getState() == DownloadManager.STATE_STOPPED)
        stopped = 1;
      dmMap.put("stopped", new Long(stopped));
      map.put("torrent" + i, dmMap);
    }
    //encode the data
    byte[] torrentData = BEncoder.encode(map);
    //open a file stream
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(getApplicationFile("downloads.config"));
      //write the data out
      fos.write(torrentData);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        if (fos != null)
          fos.close();
      } catch (Exception e) {
      }
    }
  }

  //TODO:: Move this to a FileManager class?
  public static String getApplicationPath() {
    return System.getProperty("user.dir");
  }

  public static File getApplicationFile(String filename) {
    return new File(getApplicationPath(), filename);
  }

  /**
   * @return
   */
  public TrackerChecker getTrackerChecker() {
    return trackerChecker;
  }

}
