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

  /**
   * @return Returns the row.
   */
  public BufferedTableRow getRow() {
    return row;
  }

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
        items.add(new RankItem(TorrentRow.this,0));
        items.add(new NameItem(TorrentRow.this,1));
        items.add(new SizeItem(TorrentRow.this,2));    
        items.add(new DoneItem(TorrentRow.this,3));
        items.add(new StatusItem(TorrentRow.this,4));
        items.add(new SeedsItem(TorrentRow.this,5));
        items.add(new PeersItem(TorrentRow.this,6));
        items.add(new DownSpeedItem(TorrentRow.this,7));
        items.add(new UpSpeedItem(TorrentRow.this,8));
        items.add(new ETAItem(TorrentRow.this,9));
        items.add(new TrackerStatusItem(TorrentRow.this,10));
        items.add(new PriorityItem(TorrentRow.this,11));
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
