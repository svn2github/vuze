/*
 * Created on 2 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt.views;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.ConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.config.ParameterListener;
import org.gudy.azureus2.ui.swt.views.tableitems.peers.PeerRow;
import org.gudy.azureus2.ui.swt.views.utils.SortableTable;
import org.gudy.azureus2.ui.swt.views.utils.TableSorter;

/**
 * @author Olivier
 * 
 */
public class PeersView extends AbstractIView implements DownloadManagerListener, SortableTable, ParameterListener {

  DownloadManager manager;
  Table table;
  Map objectToSortableItem;
  Map tableItemToObject;
  
  TableSorter sorter;
  int loopFactor;
  static int graphicsUpdate = COConfigurationManager.getIntParameter("Graphics Update");

  public PeersView(DownloadManager manager) {
    this.manager = manager;
    objectToSortableItem = new HashMap();
    tableItemToObject = new HashMap();
  }
  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#initialize(org.eclipse.swt.widgets.Composite)
   */
  public void initialize(Composite composite) {
    table = new Table(composite, SWT.MULTI | SWT.FULL_SELECTION);
    table.setLinesVisible(false);
    table.setHeaderVisible(true);
    String[] titles = { "ip", //$NON-NLS-1$
      "port", //$NON-NLS-1$
      "T", "I1", "C1", "pieces", //$NON-NLS-1$
      "%", //$NON-NLS-1$
      "downloadspeed", //$NON-NLS-1$
      "download", //$NON-NLS-1$
      "I2", "C2", "uploadspeed", //$NON-NLS-1$
      "upload", //$NON-NLS-1$
      "statup", "S", "downloadspeedoverall", //$NON-NLS-1$
      "optunchoke", "client","discarded" };
    int[] align =
      {
        SWT.LEFT,
        SWT.LEFT,
        SWT.LEFT,
        SWT.CENTER,
        SWT.CENTER,
        SWT.CENTER,
        SWT.RIGHT,
        SWT.RIGHT,
        SWT.RIGHT,
        SWT.CENTER,
        SWT.CENTER,
        SWT.RIGHT,
        SWT.RIGHT,
        SWT.RIGHT,
        SWT.CENTER,
        SWT.RIGHT,
        SWT.LEFT,
        SWT.LEFT,
        SWT.CENTER };
    for (int i = 0; i < titles.length; i++) {
      TableColumn column = new TableColumn(table, align[i]);
      Messages.setLanguageText(column, "PeersView." + titles[i]);
    }
    table.getColumn(0).setWidth(100);
    table.getColumn(1).setWidth(0);
    table.getColumn(2).setWidth(20);
    table.getColumn(3).setWidth(20);
    table.getColumn(4).setWidth(20);
    table.getColumn(5).setWidth(100);
    table.getColumn(6).setWidth(55);
    table.getColumn(7).setWidth(65);
    table.getColumn(8).setWidth(70);
    table.getColumn(9).setWidth(20);
    table.getColumn(10).setWidth(20);
    table.getColumn(11).setWidth(65);
    table.getColumn(12).setWidth(70);
    table.getColumn(13).setWidth(70);
    table.getColumn(14).setWidth(20);
    table.getColumn(15).setWidth(60);
    table.getColumn(16).setWidth(30);
    table.getColumn(17).setWidth(105);
    table.getColumn(18).setWidth(60);

    sorter = new TableSorter(this,"done",true);
    sorter.addStringColumnListener(table.getColumn(0),"ip");
    sorter.addStringColumnListener(table.getColumn(17),"client");
    
    sorter.addIntColumnListener(table.getColumn(1),"port");
    sorter.addIntColumnListener(table.getColumn(2),"t");
    sorter.addIntColumnListener(table.getColumn(3),"i");
    sorter.addIntColumnListener(table.getColumn(4),"c");
    sorter.addIntColumnListener(table.getColumn(5),"done");
    sorter.addIntColumnListener(table.getColumn(6),"done");
    sorter.addIntColumnListener(table.getColumn(7),"ds");
    sorter.addIntColumnListener(table.getColumn(8),"down");
    sorter.addIntColumnListener(table.getColumn(9),"i2");
    sorter.addIntColumnListener(table.getColumn(10),"c2");
    sorter.addIntColumnListener(table.getColumn(11),"us");
    sorter.addIntColumnListener(table.getColumn(12),"up");
    sorter.addIntColumnListener(table.getColumn(13),"su");
    sorter.addIntColumnListener(table.getColumn(14),"s");
    sorter.addIntColumnListener(table.getColumn(15),"od");
    sorter.addIntColumnListener(table.getColumn(16),"opt");
    sorter.addIntColumnListener(table.getColumn(18),"discarded");
        
    final Menu menu = new Menu(composite.getShell(), SWT.POP_UP);
    final MenuItem item = new MenuItem(menu, SWT.CHECK);
    Messages.setLanguageText(item, "PeersView.menu.snubbed"); //$NON-NLS-1$
    
    /*final MenuItem itemClose = new MenuItem(menu, SWT.CHECK);
    Messages.setLanguageText(itemClose, "PeersView.menu.close"); //$NON-NLS-1$
    */
    menu.addListener(SWT.Show, new Listener() {
      public void handleEvent(Event e) {
        TableItem[] tis = table.getSelection();
        if (tis.length == 0) {
          item.setEnabled(false);
          //itemClose.setEnabled(false);
          return;
        }
        item.setEnabled(true);
        TableItem ti = tis[0];
        PeerRow pti = (PeerRow) PeerRow.tableItems.get(ti);
        if (pti != null)
          item.setSelection(pti.isSnubbed());
      }
    });

    item.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        TableItem[] tis = table.getSelection();
        if (tis.length == 0) {
          return;
        }
        for(int i = 0 ; i < tis.length ; i++) {
          TableItem ti = tis[i];
          PeerRow pti = (PeerRow) PeerRow.tableItems.get(ti);
          if (pti != null)
            pti.setSnubbed(item.getSelection());
        }
      }
    });
    
    /*itemClose.addListener(SWT.Selection, new Listener() {
    public void handleEvent(Event e) {
      TableItem[] tis = table.getSelection();
      if (tis.length == 0) {
        return;
      }
      for(int i = 0 ; i < tis.length ; i ++) {
        TableItem ti = tis[i];
        PeerTableItem pti = (PeerTableItem) PeerTableItem.tableItems.get(ti);
        if (pti != null)
          pti.getPeerSocket().closeAll(false);
      }
    }
  });*/
    table.setMenu(menu);
    ConfigurationManager.getInstance().addParameterListener("Graphics Update", this);

    //    manager.addListener(this);

  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getComposite()
   */
  public Composite getComposite() {
    return table;
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#refresh()
   */
  public void refresh() {
    if (getComposite() == null || getComposite().isDisposed())
      return;
    
    sorter.reOrder(false);

    loopFactor++;
    //Refresh all items in table...
    synchronized (objectToSortableItem) {

      Iterator iter = objectToSortableItem.values().iterator();
      while (iter.hasNext()) {
        PeerRow pti = (PeerRow) iter.next();
        pti.updateAll();
        pti.updateStats();
        //Every N GUI updates we unvalidate the images
        if (loopFactor % graphicsUpdate == 0)
          pti.invalidate();
        pti.updateImage();
      }
    }
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#delete()
   */
  public void delete() {
    manager.removeListener(this);
    Iterator iter = objectToSortableItem.values().iterator();
    while (iter.hasNext()) {
      PeerRow item = (PeerRow) iter.next();
      item.remove();
    }
    if(table != null && ! table.isDisposed())
      table.dispose();
    ConfigurationManager.getInstance().removeParameterListener("Graphics Update", this);
    ConfigurationManager.getInstance().removeParameterListener("ReOrder Delay", sorter);
   }

  public String getData() {
    return "PeersView.title.short"; //$NON-NLS-1$
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getFullTitle()
   */
  public String getFullTitle() {
    return MessageText.getString("PeersView.title.full"); //$NON-NLS-1$
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IComponentListener#objectAdded(java.lang.Object)
   */
  public void peerAdded(PEPeer created) {
    synchronized (objectToSortableItem) {
      if (objectToSortableItem.containsKey(created))
        return;
      try {
        PeerRow item = new PeerRow(this,table, (PEPeer) created);
        objectToSortableItem.put(created, item);
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IComponentListener#objectRemoved(java.lang.Object)
   */
  public void peerRemoved(PEPeer removed) {
    //System.out.println("removed : " + removed.getClass() + ":" + removed);
    PeerRow item;
    synchronized (objectToSortableItem) {
      item = (PeerRow) objectToSortableItem.remove(removed);
    }
    if (item == null)
      return;
    
    tableItemToObject.remove(item.getTableItem());
    item.remove();
    //System.out.println("PC removed"); 
  }

  public void
  pieceAdded(
	  PEPiece 	piece )
  {
  }
		
  public void
  pieceRemoved(
	  PEPiece		piece )
 {
 }
  
  public void setItem(TableItem item,PEPeer peer) {
    tableItemToObject.put(item,peer);
  }

  /*
   * SortableTable implementation
   */

  public Map getObjectToSortableItemMap() {
    return objectToSortableItem;
  }

  public Table getTable() {
    return table;
  }

  public Map getTableItemToObjectMap() {
    return tableItemToObject;
  }

  /**
   * @param parameterName the name of the parameter that has changed
   * @see org.gudy.azureus2.ui.swt.config.ParameterListener#parameterChanged(java.lang.String)
   */
  public void parameterChanged(String parameterName) {
    graphicsUpdate = COConfigurationManager.getIntParameter("Graphics Update");
  }
  
}
