/*
 * Created on 30 juin 2003
 *
 */
package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.MainWindow;
import org.gudy.azureus2.ui.swt.components.BufferedTableItem;
import org.gudy.azureus2.ui.swt.components.BufferedTableRow;
import org.gudy.azureus2.ui.swt.views.MyTorrentsView;
import org.gudy.azureus2.ui.swt.views.utils.SortableItem;

/**
 * @author Olivier
 * 
 */
public class TorrentRow implements SortableItem {

  private Display display;
  private Table table;
  private BufferedTableRow row;
  private List items;
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

  public TorrentRow(MyTorrentsView view, Table table, DownloadManager manager) {
    this.table = table;
    this.manager = manager;
    items = new ArrayList();
    initialize(view);
  }

  public TableItem getTableItem() {
    return this.row.getItem();
  }

  private void initialize(final MyTorrentsView view) {
    if (table == null || table.isDisposed())
      return;
    display = table.getDisplay();
    display.asyncExec(new Runnable() {
      public void run() {
        if (table == null || table.isDisposed())
          return;
        row = new BufferedTableRow(table, SWT.NULL);
        items.add(new RankItem(row,0,manager));
        view.setItem(row.getItem(),manager);
      }
    });
  }

  public void delete() {
    display.syncExec(new Runnable() {
      public void run() {
        if (table == null || table.isDisposed())
          return;
        if (row == null || row.isDisposed())
          return;
        table.remove(table.indexOf(row.getItem()));
        row.dispose();
      }
    });
  }

  public void refresh() {
    if (table == null || table.isDisposed())
      return;
    if (row == null || row.isDisposed())
      return;

    Iterator iter = items.iterator();
    while(iter.hasNext()) {
      BufferedTableItem item = (BufferedTableItem) iter.next();
      item.refresh();
    }
    
    String tmp;   
    
    tmp = manager.getName();
    if (tmp != null && !(this.name.equals(tmp))) {
      name = tmp;      
      row.setText(1, name);     
    }

    tmp = ""; //$NON-NLS-1$
    tmp = DisplayFormatters.formatByteCountToKBEtc(manager.getSize());
    if (tmp != null && !(tmp.equals(this.size))) {
      size = tmp;
      row.setText(2, tmp);
    }

    tmp = ""; //$NON-NLS-1$
    int done = manager.getStats().getCompleted();
    tmp = (done / 10) + "." + (done % 10) + " %"; //$NON-NLS-1$ //$NON-NLS-2$
    if (!(tmp.equals(this.done))) {
      this.done = tmp;
      row.setText(3, tmp);
    }

    tmp = DisplayFormatters.formatDownloadStatus( manager );

    if (!(tmp.equals(this.status))) {
    	
      int state = manager.getState();
    	
      status = tmp;
      row.setText(4, tmp);
      if (state == DownloadManager.STATE_SEEDING)
        
        row.setForeground(MainWindow.blues[3]);
      else if (state == DownloadManager.STATE_ERROR)
        row.setForeground(MainWindow.red_ManagerItem);
      else
        row.setForeground(display.getSystemColor(SWT.COLOR_BLACK));

    }

    TRTrackerScraperResponse hd = manager.getTrackerScrapeResponse();
    
    tmp = "" + manager.getNbSeeds(); //$NON-NLS-1$
    if(hd!=null && hd.isValid())
      tmp += " (" + hd.getSeeds() + ")";
    //else
    //  tmp += " (?)";
    if (!(tmp.equals(this.nbSeeds))) {
      nbSeeds = tmp;
      row.setText(5, tmp);
    }

    tmp = "" + manager.getNbPeers(); //$NON-NLS-1$
    if(hd!=null && hd.isValid())
      tmp += " (" + hd.getPeers() + ")";
    //else
    //  tmp += " (?)";
    if (!(tmp.equals(this.nbPeers))) {
      nbPeers = tmp;
      row.setText(6, tmp);
    }

    tmp = "" + DisplayFormatters.formatByteCountToKBEtcPerSec(manager.getStats().getDownloadAverage());
    if (!(tmp.equals(this.downSpeed))) {
      downSpeed = tmp;
      row.setText(7, tmp);
    }

    tmp = "" + DisplayFormatters.formatByteCountToKBEtcPerSec(manager.getStats().getUploadAverage());
    if (!(tmp.equals(this.upSpeed))) {
      upSpeed = tmp;
      row.setText(8, tmp);
    }

    tmp = "" + DisplayFormatters.formatETA(manager.getStats().getETA());
    if (!(tmp.equals(this.eta))) {
      eta = tmp;
      row.setText(9, tmp);
    }

    tmp = "" + manager.getTrackerStatus(); //$NON-NLS-1$
    if (!(tmp.equals(this.trackerStatus))) {
      trackerStatus = tmp;
      row.setText(10, tmp);
    }

    if (manager.getPriority() == DownloadManager.HIGH_PRIORITY) {
      tmp = MessageText.getString("ManagerItem.high"); //$NON-NLS-1$
    }
    else {
      tmp = MessageText.getString("ManagerItem.low"); //$NON-NLS-1$
    }
    if (!(tmp.equals(this.priority))) {
      priority = tmp;
      row.setText(11, tmp);
    }
  }

  public int getIndex() {
    if(table == null || table.isDisposed() || row == null || row.isDisposed())
      return -1;
    return table.indexOf(row.getItem());
  }

  public DownloadManager getManager() {
    return this.manager;
  }
  
  
  /*
   * SortablePeer implementation
   */

  public String getStringField(String field) {
    if (field.equals("name")) //$NON-NLS-1$
      return manager.getName();
  
    if (field.equals("tracker")) //$NON-NLS-1$
      return manager.getTrackerStatus();
  
    if (field.equals("priority")) //$NON-NLS-1$
      return manager.getName();
  
    return ""; //$NON-NLS-1$
  }

  public long getIntField(String field) {
  
    if (field.equals("size")) //$NON-NLS-1$
      return manager.getSize();
  
    if (field.equals("done")) //$NON-NLS-1$
      return manager.getStats().getCompleted();
    
    if (field.equals("ds")) //$NON-NLS-1$
      return manager.getStats().getDownloadAverage();
  
    if (field.equals("us")) //$NON-NLS-1$
      return manager.getStats().getUploadAverage();
  
    if (field.equals("status")) //$NON-NLS-1$
      return manager.getState();
  
    if (field.equals("seeds")) //$NON-NLS-1$
      return manager.getNbSeeds();
  
    if (field.equals("peers")) //$NON-NLS-1$
      return manager.getNbPeers();
  
    if (field.equals("priority")) //$NON-NLS-1$
      return manager.getPriority();
  
    if (field.equals("#")) //$NON-NLS-1$
      return manager.getIndex();
    
    if (field.equals("eta")) //$NON-NLS-1$
      return manager.getStats().getETA();
  
    return 0;
  }  
  
  public void invalidate() {
  }

  public void setDataSource(Object dataSource) {
    this.manager = (DownloadManager) dataSource;
  }

}
