/*
 * Created on 30 juin 2003
 *
 */
package org.gudy.azureus2.ui.swt.views.tableitems;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.MainWindow;
import org.gudy.azureus2.ui.swt.views.MyTorrentsView;

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

  public ManagerItem(MyTorrentsView view, Table table, DownloadManager manager) {
    this.table = table;
    this.manager = manager;
    initialize(view);
  }

  public TableItem getTableItem() {
    return this.item;
  }

  private void initialize(final MyTorrentsView view) {
    if (table == null || table.isDisposed())
      return;
    display = table.getDisplay();
    display.asyncExec(new Runnable() {
      public void run() {
        if (table == null || table.isDisposed())
          return;
        item = new TableItem(table, SWT.NULL);
        view.setItem(item,manager);
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
    int done = manager.getStats().getCompleted();
    tmp = (done / 10) + "." + (done % 10) + " %"; //$NON-NLS-1$ //$NON-NLS-2$
    if (!(tmp.equals(this.done))) {
      this.done = tmp;
      item.setText(3, tmp);
    }

    tmp = DisplayFormatters.formatDownloadStatus( manager );

    if (!(tmp.equals(this.status))) {
    	
      int state = manager.getState();
    	
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
    if(hd!=null && hd.isValid())
      tmp += " (" + hd.getSeeds() + ")";
    //else
    //  tmp += " (?)";
    if (!(tmp.equals(this.nbSeeds))) {
      nbSeeds = tmp;
      item.setText(5, tmp);
    }

    tmp = "" + manager.getNbPeers(); //$NON-NLS-1$
    if(hd!=null && hd.isValid())
      tmp += " (" + hd.getPeers() + ")";
    //else
    //  tmp += " (?)";
    if (!(tmp.equals(this.nbPeers))) {
      nbPeers = tmp;
      item.setText(6, tmp);
    }

    tmp = "" + DisplayFormatters.formatByteCountToKBEtcPerSec(manager.getStats().getDownloadAverage());
    if (!(tmp.equals(this.downSpeed))) {
      downSpeed = tmp;
      item.setText(7, tmp);
    }

    tmp = "" + DisplayFormatters.formatByteCountToKBEtcPerSec(manager.getStats().getUploadAverage());
    if (!(tmp.equals(this.upSpeed))) {
      upSpeed = tmp;
      item.setText(8, tmp);
    }

    tmp = "" + DisplayFormatters.formatETA(manager.getStats().getETA());
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
    if(table == null || table.isDisposed() || item == null || item.isDisposed())
      return -1;
    return table.indexOf(item);
  }

  public DownloadManager getManager() {
    return this.manager;
  }  
  
}
