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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.download.*;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.tracker.client.*;
import org.gudy.azureus2.core3.util.*;

/**
 * @author Olivier
 * 
 */
public class GlobalManager extends Component {

  private List managers;
  private Checker checker;
  private GlobalManagerStats stats;
  private TRTrackerScraper trackerScraper;
  private boolean isStopped = false;

  public class Checker extends Thread {
    boolean finished = false;
    int loopFactor;
    private static final int waitTime = 1000;
    // 5 minutes save resume data interval (default)
    private int saveResumeLoopCount = 300000 / waitTime;

    public Checker() {
      super("Global Status Checker");
      loopFactor = 0;
      setPriority(Thread.MIN_PRIORITY);
      //determineSaveResumeDataInterval();
    }

    private void determineSaveResumeDataInterval() {
      int saveResumeInterval = COConfigurationManager.getIntParameter("Save Resume Interval", 5);
      if (saveResumeInterval > 1 && saveResumeInterval < 21)
        saveResumeLoopCount = saveResumeInterval * 60000 / waitTime;
    }

    public void run() {
      while (!finished) {

        loopFactor++;
        determineSaveResumeDataInterval();
        // Changed to 20 mins :D
        // Should be user configurable.
        if (loopFactor >= 1200) {
          loopFactor = 0;
          trackerScraper.update();
        }

        synchronized (managers) {
          int nbStarted = 0;
          int nbDownloading = 0;
          if (loopFactor % saveResumeLoopCount == 0) {
            saveDownloads();
          }

          for (int i = 0; i < managers.size(); i++) {
            DownloadManager manager = (DownloadManager) managers.get(i);
            if (manager.getState() == DownloadManager.STATE_DOWNLOADING) {
              nbStarted++;
              nbDownloading++;
              if (loopFactor % saveResumeLoopCount == 0) {
                manager.getDiskManager().dumpResumeDataToDisk(false);
              }
            }
            else if (manager.getState() == DownloadManager.STATE_SEEDING) {
              nbStarted++;

              //First condition to be met to be able to stop a torrent is that the number of seeds
              //Is greater than the minimal set, if any.
              int nbMinSeeds = COConfigurationManager.getIntParameter("Start Num Peers", 0);
              TRTrackerScraperResponse hd = manager.getTrackerScrapeResponse();

              boolean mayStop = false;
              if (hd != null && hd.isValid()) {
                if (hd.getSeeds() > nbMinSeeds) {
                  mayStop = true;
                }
              }
              else {
                mayStop = true;
              }

              //Checks if any condition to stop seeding is met
              int minShareRatio = 1000 * COConfigurationManager.getIntParameter("Stop Ratio", 0);
              int shareRatio = manager.getShareRatio();
              //0 means unlimited
              if (minShareRatio != 0 && shareRatio > minShareRatio && mayStop) {
                manager.stopIt();
              }

              int minSeedsPerPeersRatio = COConfigurationManager.getIntParameter("Stop Peers Ratio", 0);
              //0 means never stop
              if (mayStop && minSeedsPerPeersRatio != 0) {
                if (hd != null && hd.isValid()) {
                  int nbPeers = hd.getPeers();
                  int nbSeeds = hd.getSeeds();
                  //If there are no seeds, avoid / by 0
                  if (nbSeeds != 0) {
                    int ratio = nbPeers / nbSeeds;
                    //Added a test over the shareRatio greater than 500
                    //Avoids disconnecting too early, even with many peers
                    if (ratio < minSeedsPerPeersRatio && (shareRatio > 500 || shareRatio == -1))
                      manager.stopIt();
                  }
                }
              }
            }
            else if (manager.getState() == DownloadManager.STATE_STOPPED && manager.getCompleted() == 1000) {
              //Checks if any condition to start seeding is met
              int nbMinSeeds = COConfigurationManager.getIntParameter("Start Num Peers", 0);
              int minSeedsPerPeersRatio = COConfigurationManager.getIntParameter("Start Peers Ratio", 0);
              //0 means never start
              if (minSeedsPerPeersRatio != 0) {
                TRTrackerScraperResponse hd = manager.getTrackerScrapeResponse();
                if (hd != null && hd.isValid()) {
                  int nbPeers = hd.getPeers();
                  int nbSeeds = hd.getSeeds();
                  //If there are no seeds, avoid / by 0
                  if (nbPeers != 0) {
                    if (nbSeeds != 0) {
                      int ratio = nbPeers / nbSeeds;
                      if (ratio >= minSeedsPerPeersRatio)
                        manager.setState(DownloadManager.STATE_WAITING);
                    }
                    else {
                      //No seeds, at least 1 peer, let's start download.
                      manager.setState(DownloadManager.STATE_WAITING);
                    }
                  }
                }
              }
              if (nbMinSeeds > 0) {
                TRTrackerScraperResponse hd = manager.getTrackerScrapeResponse();
                if (hd != null && hd.isValid()) {
                  int nbSeeds = hd.getSeeds();
                  if (nbSeeds < nbMinSeeds) {
                    manager.setState(DownloadManager.STATE_WAITING);
                  }
                }
              }
            }
          }
          boolean alreadyOneAllocatingOrChecking = false;
          for (int i = 0; i < managers.size(); i++) {
            DownloadManager manager = (DownloadManager) managers.get(i);
            if (((manager.getState() == DownloadManager.STATE_ALLOCATING)
              || (manager.getState() == DownloadManager.STATE_CHECKING)
              || (manager.getState() == DownloadManager.STATE_INITIALIZED))) {
              alreadyOneAllocatingOrChecking = true;
            }
          }

          for (int i = 0; i < managers.size(); i++) {
            DownloadManager manager = (DownloadManager) managers.get(i);
            if ((manager.getState() == DownloadManager.STATE_WAITING) && !alreadyOneAllocatingOrChecking) {
              manager.initialize();
              alreadyOneAllocatingOrChecking = true;
            }
            int nbMax = COConfigurationManager.getIntParameter("max active torrents", 4);
            int nbMaxDownloads = COConfigurationManager.getIntParameter("max downloads", 4);
            if (manager.getState() == DownloadManager.STATE_READY
              && ((nbMax == 0) || (nbStarted < nbMax))
              && (manager.getCompleted() == 1000 || ((nbMaxDownloads == 0) || (nbDownloading < nbMaxDownloads)))) {
              manager.startDownload();
              nbStarted++;
              if (manager.getCompleted() != 1000)
                nbDownloading++;
            }

            if (manager.getState() == DownloadManager.STATE_ERROR) {
              DiskManager dm = manager.getDiskManager();
              if (dm != null && dm.getState() == DiskManager.FAULTY)
                manager.setErrorDetail(dm.getErrorMessage());
            }

            if ((manager.getState() == DownloadManager.STATE_SEEDING)
              && (manager.getPriority() == DownloadManager.HIGH_PRIORITY)
              && COConfigurationManager.getBooleanParameter("Switch Priority", true)) {
              manager.setPriority(DownloadManager.LOW_PRIORITY);
            }

            if ((manager.getState() == DownloadManager.STATE_ERROR)
              && (manager.getErrorDetails() != null && manager.getErrorDetails().equals("File Not Found"))) {
              removeDownloadManager(manager);
            }
          }
        }
        try {
          Thread.sleep(waitTime);
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
    stats = new GlobalManagerStats(0);
    managers = new ArrayList();
    trackerScraper = TRTrackerScraperFactory.create();
    loadDownloads();
    checker = new Checker();
    checker.start();
  }

  public boolean addDownloadManager(String fileName, String savePath) {
    try {
      File f = new File(fileName);
      File torrentDir = new File(COConfigurationManager.getDirectoryParameter("General_sDefaultTorrent_Directory"));
      torrentDir.mkdirs();
      File fDest = new File(torrentDir, f.getName().replaceAll("%20","."));
      if (fDest.equals(f))
        throw new Exception("Same files");
      String prefix = "_";
      while (fDest.exists()) {
        fDest = new File(torrentDir, "_" + fDest.getName());
      }
      fDest.createNewFile();
      copyFile(f, fDest);
      DownloadManager manager = DownloadManagerFactory.create(this, fDest.getAbsolutePath(), savePath);
      boolean correct = addDownloadManager(manager);
      if (!correct) {
        fDest.delete();
      }
      return correct;
    }
    catch (IOException e) {
      e.printStackTrace();
      DownloadManager manager = DownloadManagerFactory.create(this, fileName, savePath);
      return addDownloadManager(manager);
    }
    catch (Exception e) {
      DownloadManager manager = DownloadManagerFactory.create(this, fileName, savePath);
      return addDownloadManager(manager);
    }
  }

  private void copyFile(File origin, File destination) throws IOException {
    OutputStream os = new FileOutputStream(destination);
    InputStream is = new FileInputStream(origin);
    byte[] buffer = new byte[32768];
    int nbRead = 0;
    while ((nbRead = is.read(buffer)) > 0) {
      os.write(buffer, 0, nbRead);
    }
    is.close();
    os.close();
  }

  //Public method !!! and don't touch it !
  public boolean addDownloadManager(DownloadManager manager) {
    if (!isStopped) {
      synchronized (managers) {
        if (managers.contains(manager)) {
          manager.setState(DownloadManager.STATE_DUPLICATE);
          return false;
        }
        managers.add(manager);
      }

      objectAdded(manager);
      saveDownloads();
      return true;
    }
    else {
      LGLogger.log(
        0,
        LGLogger.ERROR,
        LGLogger.ERROR,
        "Tried to add a DownloadManager after shutdown of GlobalManager.");
      return false;
    }
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

    if (manager.getTrackerClient() != null) {

      trackerScraper.remove(manager.getTrackerClient());

    }
    else if (manager.getTorrent() != null) {

      trackerScraper.remove(manager.getTorrent());
    }
  }

  public void stopAll() {
    if (!isStopped) {
      checker.stopIt();
      saveDownloads();
      while (managers.size() != 0) {
        DownloadManager manager = (DownloadManager) managers.remove(0);
        manager.stopIt();
      }
      isStopped = true;
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

  public void discarded(int length) {
    stats.discarded(length);
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
      File configFile = FileUtil.getApplicationFile("downloads.config");
      fin = new FileInputStream(configFile);
      bin = new BufferedInputStream(fin);
      Map map = BDecoder.decode(bin);
      boolean debug = Boolean.getBoolean("debug");

      Iterator iter = null;
      //v2.0.3.0+ vs older mode
      List downloads = (List) map.get("downloads");
      if (downloads == null) {
        //No downloads entry, then use the old way
        iter = map.values().iterator();
      }
      else {
        //New way, downloads stored in a list
        iter = downloads.iterator();
      }
      while (iter.hasNext()) {
        Map mDownload = (Map) iter.next();
        try {
          String fileName = new String((byte[]) mDownload.get("torrent"), Constants.DEFAULT_ENCODING);
          String savePath = new String((byte[]) mDownload.get("path"), Constants.DEFAULT_ENCODING);
          int nbUploads = ((Long) mDownload.get("uploads")).intValue();
          int stopped = debug ? 1 : ((Long) mDownload.get("stopped")).intValue();
          Long lPriority = (Long) mDownload.get("priority");
          Long lDownloaded = (Long) mDownload.get("downloaded");
          Long lUploaded = (Long) mDownload.get("uploaded");
          Long lCompleted = (Long) mDownload.get("completed");
          DownloadManager dm = DownloadManagerFactory.create(this, fileName, savePath, stopped == 1);
          dm.setMaxUploads(nbUploads);
          if (lPriority != null) {
            dm.setPriority(lPriority.intValue());
          }
          if (lDownloaded != null && lUploaded != null) {
            dm.setDownloadedUploaded(lDownloaded.longValue(), lUploaded.longValue());
          }
          if (lCompleted != null) {
            dm.setCompleted(lCompleted.intValue());
          }
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
    }
    finally {
      try {
        if (bin != null)
          bin.close();
      }
      catch (Exception e) {}
      try {
        if (fin != null)
          fin.close();
      }
      catch (Exception e) {}
    }
  }

  private void saveDownloads() {
    //    if(Boolean.getBoolean("debug")) return;

    Map map = new HashMap();
    List list = new ArrayList(managers.size());
    for (int i = 0; i < managers.size(); i++) {
      DownloadManager dm = (DownloadManager) managers.get(i);
      Map dmMap = new HashMap();
      dmMap.put("torrent", dm.getTorrentFileName());
      dmMap.put("path", dm.getSavePathForSave());
      dmMap.put("uploads", new Long(dm.getMaxUploads()));
      int stopped = 0;
      if (dm.getState() == DownloadManager.STATE_STOPPED)
        stopped = 1;
      dmMap.put("stopped", new Long(stopped));
      int priority = dm.getPriority();
      dmMap.put("priority", new Long(priority));
      dmMap.put("position", new Long(i));
      dmMap.put("downloaded", new Long(dm.getDownloadedRaw()));
      dmMap.put("uploaded", new Long(dm.getUploadedRaw()));
      dmMap.put("completed", new Long(dm.getCompleted()));
      list.add(dmMap);
    }
    map.put("downloads", list);
    //encode the data
    byte[] torrentData = BEncoder.encode(map);
    //open a file stream
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(FileUtil.getApplicationFile("downloads.config"));
      //write the data out
      fos.write(torrentData);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    finally {
      try {
        if (fos != null)
          fos.close();
      }
      catch (Exception e) {}
    }
  }

  /**
   * @return
   */
  public TRTrackerScraper getTrackerScraper() {
    return trackerScraper;
  }

  public int getIndexOf(DownloadManager manager) {
    if (managers != null && manager != null)
      return managers.indexOf(manager);
    return -1;
  }

  public boolean isMoveableUp(DownloadManager manager) {
    return getIndexOf(manager) > 0;
  }

  public boolean isMoveableDown(DownloadManager manager) {
    if (managers != null)
      return getIndexOf(manager) < managers.size() - 1;
    return false;
  }

  public void moveUp(DownloadManager manager) {
    if (managers != null) {
      synchronized (managers) {
        int index = managers.indexOf(manager);
        if (index > 0) {
          managers.remove(index);
          managers.add(index - 1, manager);
        }
      }
    }
  }

  public void moveDown(DownloadManager manager) {
    if (managers != null) {
      synchronized (managers) {
        int index = managers.indexOf(manager);
        if (index < managers.size() - 1) {
          managers.remove(index);
          managers.add(index + 1, manager);
        }
      }
    }
  }

}
