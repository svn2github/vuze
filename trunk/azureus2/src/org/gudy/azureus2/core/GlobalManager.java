/*
 * Created on 30 juin 2003
 *
 */
package org.gudy.azureus2.core;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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

  public class Checker extends Thread {
    boolean finished = false;

    public Checker() {
      super("Global Status Checker");
    }

    public void run() {
      while (!finished) {
        synchronized (managers) {
          int nbStarted = 0;
          for (int i = 0; i < managers.size(); i++) {
            DownloadManager manager = (DownloadManager) managers.get(i);
            if (manager.getState() == DownloadManager.STATE_DOWNLOADING
              || manager.getState() == DownloadManager.STATE_SEEDING)
              nbStarted++;
          }
          boolean alreadyOneAllocatingOrChecking = false;
          for (int i = 0; i < managers.size(); i++) {
            DownloadManager manager = (DownloadManager) managers.get(i);
            if ((manager.getState() == DownloadManager.STATE_WAITING) && !alreadyOneAllocatingOrChecking) {
              manager.initialize();
              alreadyOneAllocatingOrChecking = true;
            }
            if (manager.getState() == DownloadManager.STATE_READY
              && nbStarted < ConfigurationManager.getInstance().getIntParameter("max active torrents", 4)) {
              manager.startDownload();
            }

            if (((manager.getState() == DownloadManager.STATE_ALLOCATING)
              || (manager.getState() == DownloadManager.STATE_CHECKING)
              || (manager.getState() == DownloadManager.STATE_INITIALIZED))) {
              alreadyOneAllocatingOrChecking = true;
            }
            
            if((manager.getState() == DownloadManager.STATE_SEEDING) && (manager.getPriority() == DownloadManager.HIGH_PRIORITY) && ConfigurationManager.getInstance().getBooleanParameter("Switch Priority",true)) {
              manager.setPriority(DownloadManager.LOW_PRIORITY);
            }

          }
        }
        try {
          Thread.sleep(50);
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
    loadDownloads();
    checker = new Checker();
    checker.start();
  }

  public void addDownloadManager(DownloadManager manager) {
    synchronized (managers) {
      managers.add(manager);
    }

    this.objectAdded(manager);
  }

  public List getDownloadManagers() {
    return managers;
  }

  public void removeDownloadManager(DownloadManager manager) {
    synchronized (managers) {
      managers.remove(manager);
    }
    this.objectRemoved(manager);
  }

  public void stopAll() {
    checker.stopIt();
    saveDownloads();
    while (managers.size() > 0) {
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
        try {
          //open the file
          FileInputStream fin = new FileInputStream(this.getApplicationPath() + "downloads.config");      
          BufferedInputStream bin = new BufferedInputStream(fin);     
          Map map = BDecoder.decode(bin);
          Iterator iter = map.values().iterator();
          while(iter.hasNext()) {
            Map mDownload = (Map) iter.next();
            try {
              String fileName = new String((byte[])mDownload.get("torrent"), "ISO-8859-1");
              String savePath = new String((byte[])mDownload.get("path"), "ISO-8859-1");
              int nbUploads = ((Long) mDownload.get("uploads")).intValue();
              int stopped = ((Long) mDownload.get("stopped")).intValue();
              DownloadManager dm = new DownloadManager(this,fileName,savePath);
              dm.setMaxUploads(nbUploads);
              if(stopped == 1)
                dm.stopIt();
              this.addDownloadManager(dm);              
            }
            catch (UnsupportedEncodingException e1) {
              //Do nothing and process next.
            }    
          }
        } catch (FileNotFoundException e) {
          //Do nothing
        }
  }
  
  private void saveDownloads() {
    
      Map map = new HashMap();
      for(int i = 0 ; i < managers.size() ; i++) {
        DownloadManager dm = (DownloadManager) managers.get(i);
        Map dmMap = new HashMap();
        dmMap.put("torrent",dm.getTorrentFileName().getBytes());
        dmMap.put("path",dm.getSavePath());
        dmMap.put("uploads",new Long(dm.getMaxUploads()));
        int stopped = 0;
        if(dm.getState() == DownloadManager.STATE_STOPPED)
          stopped = 1;
        dmMap.put("stopped", new Long(stopped));
        map.put("torrent" +i ,dmMap);
      }
      //encode the data
      byte[] torrentData = BEncoder.encode(map);
      //open a file stream
      FileOutputStream fos = null;
      try {
        fos = new FileOutputStream(this.getApplicationPath() + "downloads.config");
      } catch (FileNotFoundException e) {     
        e.printStackTrace();
      }
      //write the data out
      try {
        fos.write(torrentData);
      } catch (IOException e) {
        e.printStackTrace();
      }    
  }
  
//TODO:: Move this to a FileManager class?
  private String getApplicationPath()
  {
    return System.getProperty("user.dir")+System.getProperty("file.separator");
  } 

}
