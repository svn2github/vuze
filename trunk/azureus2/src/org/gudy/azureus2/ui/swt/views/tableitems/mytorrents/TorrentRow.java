/*
 * Created on 30 juin 2003
 *
 */
package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.category.Category;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.plugins.ui.tables.mytorrents.PluginMyTorrentsItemFactory;
import org.gudy.azureus2.pluginsimpl.ui.tables.mytorrents.MyTorrentsTableExtensions;
import org.gudy.azureus2.ui.swt.components.BufferedTableItem;
import org.gudy.azureus2.ui.swt.components.BufferedTableRow;
import org.gudy.azureus2.ui.swt.views.MyTorrentsView;
import org.gudy.azureus2.ui.swt.views.tableitems.utils.ItemEnumerator;
import org.gudy.azureus2.ui.swt.views.utils.SortableItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.PluginItem;

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
  private Map pluginItems;
  
  private boolean valid;

  /**
   * @return Returns the valid.
   */
  public boolean isValid() {
    return valid;
  }

  /**
   * @param valid The valid to set.
   */
  public void setValid(boolean valid) {
    this.valid = valid;
  }

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
  	if (row == null)
  		// this sucks.  It could mean we are in the process of initialize()
  		return null;

    return this.row.getItem();
  }

  private void initialize(final MyTorrentsView view) {
    if (table == null || table.isDisposed())
      return;
    display = table.getDisplay();
    this.pluginItems = new HashMap();
    display.asyncExec(new Runnable() {
      public void run() {
        if (table == null || table.isDisposed())
          return;
        ItemEnumerator itemEnumerator = view.getItemEnumerator();
        
        row = new BufferedTableRow(table, SWT.NULL);
        items.add(new RankItem(TorrentRow.this,itemEnumerator.getPositionByName("#")));
        items.add(new NameItem(TorrentRow.this,itemEnumerator.getPositionByName("name")));
        items.add(new SizeItem(TorrentRow.this,itemEnumerator.getPositionByName("size")));    
        items.add(new DoneItem(TorrentRow.this,itemEnumerator.getPositionByName("done")));
        items.add(new StatusItem(TorrentRow.this,itemEnumerator.getPositionByName("status")));
        items.add(new SeedsItem(TorrentRow.this,itemEnumerator.getPositionByName("seeds")));
        items.add(new PeersItem(TorrentRow.this,itemEnumerator.getPositionByName("peers")));
        items.add(new DownSpeedItem(TorrentRow.this,itemEnumerator.getPositionByName("downspeed")));
        items.add(new UpSpeedItem(TorrentRow.this,itemEnumerator.getPositionByName("upspeed")));
        items.add(new ETAItem(TorrentRow.this,itemEnumerator.getPositionByName("eta")));
        items.add(new TrackerStatusItem(TorrentRow.this,itemEnumerator.getPositionByName("tracker")));
        items.add(new PriorityItem(TorrentRow.this,itemEnumerator.getPositionByName("priority")));
        items.add(new ShareRatioItem(TorrentRow.this,itemEnumerator.getPositionByName("shareRatio")));
        items.add(new DownItem(TorrentRow.this,itemEnumerator.getPositionByName("down")));
        items.add(new UpItem(TorrentRow.this,itemEnumerator.getPositionByName("up")));
        items.add(new PiecesItem(TorrentRow.this,itemEnumerator.getPositionByName("pieces")));
        items.add(new CompletionItem(TorrentRow.this,itemEnumerator.getPositionByName("completion")));
        items.add(new HealthItem(TorrentRow.this,itemEnumerator.getPositionByName("health")));
        items.add(new MaxUploadsItem(TorrentRow.this,itemEnumerator.getPositionByName("maxuploads")));
        items.add(new TotalSpeedItem(TorrentRow.this,itemEnumerator.getPositionByName("totalspeed")));
        items.add(new SavePathItem(TorrentRow.this,itemEnumerator.getPositionByName("savepath")));
        items.add(new CategoryItem(TorrentRow.this,itemEnumerator.getPositionByName("category")));
        items.add(new AvailabilityItem(TorrentRow.this,itemEnumerator.getPositionByName("availability")));
        items.add(new RemainingItem(TorrentRow.this,itemEnumerator.getPositionByName("remaining")));
        items.add(new SecondsSeedingItem(TorrentRow.this,itemEnumerator.getPositionByName("secondsseeding")));
        items.add(new SecondsDownloadingItem(TorrentRow.this,itemEnumerator.getPositionByName("secondsdownloading")));

        Map extensions = MyTorrentsTableExtensions.getInstance().getExtensions();
        Iterator iter = extensions.keySet().iterator();
        while(iter.hasNext()) {
          String name = (String) iter.next();
          PluginMyTorrentsItemFactory ppif = (PluginMyTorrentsItemFactory) extensions.get(name);
          PluginItem pi = new PluginItem(TorrentRow.this,itemEnumerator.getPositionByName(name),ppif);
          items.add(pi);
          pluginItems.put(name,pi);          
        }
        
        view.setItem(row.getItem(),manager);
      }
    });
  }

  public void delete() {
    if(display == null || display.isDisposed())
      return;
    display.asyncExec(new Runnable() {
      public void run() {
        Iterator iter = items.iterator();
        while(iter.hasNext()) {
          BufferedTableItem item = (BufferedTableItem) iter.next();
          item.dispose();
        }
        if (table == null || table.isDisposed())
          return;
        if (row == null || row.isDisposed())
          return;
        table.remove(table.indexOf(row.getItem()));
        row.dispose();
      }
    });
  }

  public void refresh(boolean bDoGraphics) {
    if (table == null || table.isDisposed())
      return;
    if (row == null || row.isDisposed())
      return;
    

    Iterator iter = items.iterator();
    while(iter.hasNext()) {
      BufferedTableItem item = (BufferedTableItem) iter.next();
      if (item.isShown() && (!item.needsPainting() || bDoGraphics || !valid)) {
        item.refresh();
      }
    }
    this.setValid(true);
  }
  
  public void locationChanged(int iStartColumn) {
  	if (table == null || table.isDisposed())
  		return;
  	if (row == null || row.isDisposed())
  		return;

  	Iterator iter = items.iterator();
  	while(iter.hasNext()) {
  		BufferedTableItem item = (BufferedTableItem) iter.next();
  		if (item.getPosition() > iStartColumn)
  		  item.locationChanged();
  	}
  }

  public void doPaint(GC gc) {
    if (table == null || table.isDisposed())
      return;
    if (row == null || row.isDisposed())
      return;

    Iterator iter = items.iterator();
    while(iter.hasNext()) {
      BufferedTableItem item = (BufferedTableItem) iter.next();
  		if (item.needsPainting()) {
  			item.doPaint(gc);
  		}
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
  
    if (field.equals("savepath")) //$NON-NLS-1$
      return manager.getSavePath();
  
    if (field.equals("category")) {
      Category cat = manager.getCategory();
      return (cat == null) ? "" : cat.getName();
    }
  
    PluginItem item = (PluginItem)pluginItems.get(field);
    if(item != null)
      return item.pluginItem.getStringValue();

    return ""; //$NON-NLS-1$
  }

  public long getIntField(String field) {
  
    if (field.equals("size")) //$NON-NLS-1$
      return manager.getSize();
  
    if (field.equals("done")) //$NON-NLS-1$
      return manager.getStats().getCompleted();
    
    if (field.equals("pieces")) //$NON-NLS-1$
      return manager.getStats().getCompleted();
    
    if (field.equals("completion")) //$NON-NLS-1$
      return manager.getStats().getDownloadCompleted(true);
    
    if (field.equals("downspeed")) //$NON-NLS-1$
      return manager.getStats().getDownloadAverage();
  
    if (field.equals("upspeed")) //$NON-NLS-1$
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
      return manager.getPosition();
    
    if (field.equals("eta")) //$NON-NLS-1$
      return manager.getStats().getETA();
    
    if (field.equals("shareRatio")){
    		// convert infinity into something that will sort as an int :)
    	
    	int sr = manager.getStats().getShareRatio();
    	if ( sr == -1 ){
    		return( 0x7fffffff );
    	}else{
    		return( sr );
    	}
    }
    
    if (field.equals("down")) //$NON-NLS-1$
      return manager.getStats().getDownloaded();
    
    if (field.equals("up")) //$NON-NLS-1$
      return manager.getStats().getUploaded();
    
    if (field.equals("remaining")) {
      DiskManager dm = manager.getDiskManager();
      if (dm == null) {
        return manager.getSize() - 
               ((long)manager.getStats().getCompleted() * manager.getSize() / 1000L);
      } else {
        return dm.getRemaining();
      }
    }

    if (field.equals("maxuploads")) //$NON-NLS-1$
      return manager.getStats().getMaxUploads();
    
    if (field.equals("totalspeed")) //$NON-NLS-1$
      return manager.getStats().getTotalAverage();
      
    if (field.equals("health"))
      return manager.getHealthStatus();

    if (field.equals("availability")) {
      PEPeerManager pm = manager.getPeerManager();
      if (pm == null)
        return 0;
      return (int)pm.getMinAvailability() * 1000;
    }
    
    if (field.equals("secondsseeding")) {
      return manager.getStats().getSecondsDownloading() + manager.getStats().getSecondsOnlySeeding();
    }

    if (field.equals("secondsdownloading")) {
      return manager.getStats().getSecondsDownloading();
    }

    PluginItem item = (PluginItem)pluginItems.get(field);
    if(item != null)
      return item.pluginItem.getIntValue();
    
    return 0;
  }  
  
  public void invalidate() {
    valid = false;
  }

  /**
   * @return true - dataSource changed.  false - already set
   */
  public boolean setDataSource(Object dataSource) {
    if (this.manager != (DownloadManager) dataSource) {
      this.manager = (DownloadManager)dataSource;
      invalidate();
      return true;
    }
    return false;
  }

}
