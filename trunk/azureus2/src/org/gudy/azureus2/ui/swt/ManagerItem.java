/*
 * Created on 30 juin 2003
 *
 */
package org.gudy.azureus2.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.core.*;

/**
 * @author Olivier
 * 
 */
public class ManagerItem {

  private Display display;
  private Table table;
  private TableItem item;
  private DownloadManager manager;

  private String name = "";
  private String size = "";
  private String done = "";
  private String status = "";
  private String nbSeeds = "";
  private String nbPeers = "";
  private String downSpeed = "";
  private String upSpeed = "";
  private String eta = "";
  private String trackerStatus = "";
  private String priority = "";

  public ManagerItem(Table table, DownloadManager manager) {
    this.table = table;
    this.manager = manager;
    initialize();
  }

  public TableItem getTableItem() {
    return this.item;
  }

  private void initialize() {
    if (table == null || table.isDisposed())
      return;
    display = table.getDisplay();
    display.syncExec(new Runnable() {
      public void run() {
        if (table == null || table.isDisposed())
          return;
        item = new TableItem(table, SWT.NULL);
      }
    });
  }

  public void delete() {
    display.syncExec(new Runnable() {
      public void run() {
        if (table == null || table.isDisposed())
          return;
        if (item == null || item.isDisposed())
          return;
        table.remove(table.indexOf(item));
        item.dispose();
      }
    });
  }

  public void refresh() {
    if (table == null || table.isDisposed())
      return;
    if (item == null || item.isDisposed())
      return;
    String tmp;
    tmp = manager.getName();
    if (!(tmp.equals(this.name))) {
      name = tmp;
      item.setText(0, tmp);
    }

    tmp = "";
    tmp = PeerStats.format(manager.getSize());
    if (!(tmp.equals(this.size))) {
      size = tmp;
      item.setText(1, tmp);
    }

    tmp = "";
    int done = manager.getCompleted();
    tmp = (done / 10) + "." + (done % 10) + " %";
    if (!(tmp.equals(this.done))) {
      this.done = tmp;
      item.setText(2, tmp);
    }

    tmp = "";
    int state = manager.getState();
    switch (state) {
      case DownloadManager.STATE_WAITING :
        tmp = "Waiting";
        break;
      case DownloadManager.STATE_ALLOCATING :
        tmp = "Allocating";
        break;
      case DownloadManager.STATE_CHECKING :
        tmp = "Checking";
        break;
      case DownloadManager.STATE_READY :
        tmp = "Ready";
        break;
      case DownloadManager.STATE_DOWNLOADING :
        tmp = "Downloading";
        break;
      case DownloadManager.STATE_SEEDING :
        tmp = "Seeding";
        break;

      case DownloadManager.STATE_STOPPED :
        tmp = "Stopped";
        break;
      case DownloadManager.STATE_ERROR :
        tmp = "Error : " + manager.getErrorDetails();
        break;
    }
    if (!(tmp.equals(this.status))) {
      status = tmp;
      item.setText(3, tmp);
    }

    tmp = "" + manager.getNbSeeds();
    if (!(tmp.equals(this.nbSeeds))) {
      nbSeeds = tmp;
      item.setText(4, tmp);
    }

    tmp = "" + manager.getNbPeers();
    if (!(tmp.equals(this.nbPeers))) {
      nbPeers = tmp;
      item.setText(5, tmp);
    }

    tmp = "" + manager.getDownloadSpeed();
    if (!(tmp.equals(this.downSpeed))) {
      downSpeed = tmp;
      item.setText(6, tmp);
    }

    tmp = "" + manager.getUploadSpeed();
    if (!(tmp.equals(this.upSpeed))) {
      upSpeed = tmp;
      item.setText(7, tmp);
    }

    tmp = "" + manager.getETA();
    if (!(tmp.equals(this.eta))) {
      eta = tmp;
      item.setText(8, tmp);
    }

    tmp = "" + manager.getTrackerStatus();
    if (!(tmp.equals(this.trackerStatus))) {
      trackerStatus = tmp;
      item.setText(9, tmp);
    }

    int prio = manager.getPriority();
    if (prio == DownloadManager.HIGH_PRIORITY) {
      tmp = "high";
    }
    else {
      tmp = "low";
    }
    if (!(tmp.equals(this.priority))) {
      priority = tmp;
      item.setText(10, tmp);
    }
  }
}
