/*
 * Created on 30 juin 2003
 *
 */
package org.gudy.azureus2.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.core.DownloadManager;
import org.gudy.azureus2.core.PeerStats;

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

  private Color blue;
  private Color red;

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
        blue = new Color(display, new RGB(64, 160, 255));
        red = new Color(display, new RGB(255, 68, 68));
        item = new TableItem(table, SWT.NULL);
      }
    });
  }

  public void delete() {
    display.syncExec(new Runnable() {
      public void run() {
        if (blue != null && !blue.isDisposed())
          blue.dispose();
        if (red != null && !red.isDisposed())
          red.dispose();
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
      int sep = tmp.lastIndexOf(".");
      if(sep == -1) sep = 0;
      tmp = tmp.substring(sep);
      Program program = Program.findProgram(tmp);
      Image icon = ImageRepository.getIconFromProgram(program);
      item.setText(0, name);
      item.setImage(icon);
      
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
      if (state == DownloadManager.STATE_SEEDING)
        item.setForeground(blue);
      else if (state == DownloadManager.STATE_ERROR)
        item.setForeground(red);
      else
        item.setForeground(display.getSystemColor(SWT.COLOR_BLACK));

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

  public void setManager(DownloadManager manager) {
    this.manager = manager;
  }

  public int getIndex() {
    return table.indexOf(item);
  }

  public DownloadManager getManager() {
    return this.manager;
  }
}
