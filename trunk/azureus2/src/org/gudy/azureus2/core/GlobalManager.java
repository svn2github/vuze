/*
 * Created on 30 juin 2003
 *
 */
package org.gudy.azureus2.core;

import java.util.ArrayList;
import java.util.List;

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

}
