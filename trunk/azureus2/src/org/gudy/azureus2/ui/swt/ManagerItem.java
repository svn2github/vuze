/*
 * Created on 30 juin 2003
 *
 */
package org.gudy.azureus2.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.core.DownloadManager;
import org.gudy.azureus2.core.MessageText;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.core3.util.DisplayFormatters;

/**
 * @author Olivier
 * 
 */
public class ManagerItem {

  private Display display;
  private Table table;
  private TableItem item;
  private DownloadManager manager;

  private String index = ""; //$NON-NLS-1$
  private String name = ""; //$NON-NLS-1$
  private String size = ""; //$NON-NLS-1$
  private String done = ""; //$NON-NLS-1$
  private String status = ""; //$NON-NLS-1$
  private String nbSeeds = ""; //$NON-NLS-1$
  private String nbPeers = ""; //$NON-NLS-1$
  private String downSpeed = ""; //$NON-NLS-1$
  private String upSpeed = ""; //$NON-NLS-1$
  private String eta = ""; //$NON-NLS-1$
  private String trackerStatus = ""; //$NON-NLS-1$
  private String priority = ""; //$NON-NLS-1$
  
  //Used when sorting
  public boolean selected;  

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
    
    tmp = "" + (manager.getIndex()+1);
    if(!(this.index.equals(tmp))) {
      index = tmp;
      item.setText(0,index);
    }
    
    tmp = manager.getName();
    if (tmp != null && !(this.name.equals(tmp))) {
      name = tmp;
      int sep = tmp.lastIndexOf('.'); //$NON-NLS-1$
      if(sep < 0) sep = 0;
      tmp = tmp.substring(sep);
      Program program = Program.findProgram(tmp);
      Image icon = ImageRepository.getIconFromProgram(program);
      item.setText(1, name);
      item.setImage(icon);
      
    }

    tmp = ""; //$NON-NLS-1$
    tmp = DisplayFormatters.formatByteCountToKBEtc(manager.getSize());
    if (tmp != null && !(tmp.equals(this.size))) {
      size = tmp;
      item.setText(2, tmp);
    }

    tmp = ""; //$NON-NLS-1$
    int done = manager.getCompleted();
    tmp = (done / 10) + "." + (done % 10) + " %"; //$NON-NLS-1$ //$NON-NLS-2$
    if (!(tmp.equals(this.done))) {
      this.done = tmp;
      item.setText(3, tmp);
    }

    tmp = ""; //$NON-NLS-1$
    int state = manager.getState();
    switch (state) {
      case DownloadManager.STATE_WAITING :
        tmp = MessageText.getString("ManagerItem.waiting"); //$NON-NLS-1$
        break;
      case DownloadManager.STATE_ALLOCATING :
        tmp = MessageText.getString("ManagerItem.allocating"); //$NON-NLS-1$
        break;
      case DownloadManager.STATE_CHECKING :
        tmp = MessageText.getString("ManagerItem.checking"); //$NON-NLS-1$
        break;
      case DownloadManager.STATE_READY :
        tmp = MessageText.getString("ManagerItem.ready"); //$NON-NLS-1$
        break;
      case DownloadManager.STATE_DOWNLOADING :
        tmp = MessageText.getString("ManagerItem.downloading"); //$NON-NLS-1$
        break;
      case DownloadManager.STATE_SEEDING :
        tmp = MessageText.getString("ManagerItem.seeding"); //$NON-NLS-1$
        break;

      case DownloadManager.STATE_STOPPED :
        tmp = MessageText.getString("ManagerItem.stopped"); //$NON-NLS-1$
        break;
      case DownloadManager.STATE_ERROR :
        tmp = MessageText.getString("ManagerItem.error") + " : " + manager.getErrorDetails(); //$NON-NLS-1$ //$NON-NLS-2$
        break;
    }
    if (!(tmp.equals(this.status))) {
      status = tmp;
      item.setText(4, tmp);
      if (state == DownloadManager.STATE_SEEDING)
        item.setForeground(MainWindow.blues[3]);
      else if (state == DownloadManager.STATE_ERROR)
        item.setForeground(MainWindow.red_ManagerItem);
      else
        item.setForeground(display.getSystemColor(SWT.COLOR_BLACK));

    }

    TRTrackerScraperResponse hd = manager.getTrackerScrapeResponse();
    
    tmp = "" + manager.getNbSeeds(); //$NON-NLS-1$
    if(hd!=null)
      tmp += " (" + hd.getSeeds() + ")";
    else
      tmp += " (?)";
    if (!(tmp.equals(this.nbSeeds))) {
      nbSeeds = tmp;
      item.setText(5, tmp);
    }

    tmp = "" + manager.getNbPeers(); //$NON-NLS-1$
    if(hd!=null)
          tmp += " (" + hd.getPeers() + ")";
        else
          tmp += " (?)";
    if (!(tmp.equals(this.nbPeers))) {
      nbPeers = tmp;
      item.setText(6, tmp);
    }

    tmp = "" + manager.getDownloadSpeed(); //$NON-NLS-1$
    if (!(tmp.equals(this.downSpeed))) {
      downSpeed = tmp;
      item.setText(7, tmp);
    }

    tmp = "" + manager.getUploadSpeed(); //$NON-NLS-1$
    if (!(tmp.equals(this.upSpeed))) {
      upSpeed = tmp;
      item.setText(8, tmp);
    }

    tmp = "" + manager.getETA(); //$NON-NLS-1$
    if (!(tmp.equals(this.eta))) {
      eta = tmp;
      item.setText(9, tmp);
    }

    tmp = "" + manager.getTrackerStatus(); //$NON-NLS-1$
    if (!(tmp.equals(this.trackerStatus))) {
      trackerStatus = tmp;
      item.setText(10, tmp);
    }

    if (manager.getPriority() == DownloadManager.HIGH_PRIORITY) {
      tmp = MessageText.getString("ManagerItem.high"); //$NON-NLS-1$
    }
    else {
      tmp = MessageText.getString("ManagerItem.low"); //$NON-NLS-1$
    }
    if (!(tmp.equals(this.priority))) {
      priority = tmp;
      item.setText(11, tmp);
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
