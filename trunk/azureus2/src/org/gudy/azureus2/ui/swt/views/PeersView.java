/*
 * Created on 2 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt.views;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerPeerListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.views.tableitems.peers.PeerRow;
import org.gudy.azureus2.ui.swt.views.tableitems.peers.PeersViewEventDispacher;
import org.gudy.azureus2.ui.swt.views.tableitems.peers.PeersViewItemEnumerator;
import org.gudy.azureus2.ui.swt.views.tableitems.peers.PeersViewListener;
import org.gudy.azureus2.ui.swt.views.tableitems.utils.EnumeratorEditor;
import org.gudy.azureus2.ui.swt.views.tableitems.utils.ItemDescriptor;
import org.gudy.azureus2.ui.swt.views.tableitems.utils.ItemEnumerator;
import org.gudy.azureus2.ui.swt.views.utils.SortableTable;
import org.gudy.azureus2.ui.swt.views.utils.TableSorter;

/**
 * @author Olivier
 */
public class PeersView extends AbstractIView implements DownloadManagerPeerListener, SortableTable, ParameterListener, PeersViewListener {

  private DownloadManager manager;
  private Composite panel;
  private Table table;
  private Menu menu;
  private Map objectToSortableItem;
  private Map tableItemToObject;
  
  private ItemEnumerator itemEnumerator;
  private TableSorter sorter;
  
  private int loopFactor;
  private int graphicsUpdate = COConfigurationManager.getIntParameter("Graphics Update");

  private final String[] tableItems = {
     "ip;L;S;100;0"
    ,"port;L;I;40;-1"
    ,"T;L;I;20;1"
    ,"I1;C;I;20;2"
    ,"C1;C;I;20;3"
    ,"pieces;C;I;100;4"
    ,"%;R;I;55;5"
    ,"downloadspeed;R;I;65;6"
    ,"download;R;I;70;7"
    ,"I2;C;I;20;8"
    ,"C2;C;I;20;9"
    ,"optunchoke;C;I;20;10"
    ,"uploadspeed;R;I;65;11"
    ,"upload;R;I;70;12"
    ,"statup;R;I;65;-1"
    ,"S;C;I;20;13"
    ,"downloadspeedoverall;R;I;65;14"    
    ,"client;L;S;105;15"
    ,"discarded;R;I;60;16" };

  /**
   * @return Returns the itemEnumerator.
   */
  public ItemEnumerator getItemEnumerator() {
    return itemEnumerator;
  }

  public PeersView(DownloadManager manager) {
    this.manager = manager;
    objectToSortableItem = new HashMap();
    tableItemToObject = new HashMap();
  }
  
  public void initialize(Composite composite) {
    panel = new Composite(composite,SWT.NULL);
    panel.setLayout(new FillLayout());
    
    createMenu();
    createTable();        
    
    COConfigurationManager.addParameterListener("Graphics Update", this);
    PeersViewEventDispacher.getInstance().addListener(this);
  }
  
  public void tableStructureChanged() {
    //1. Unregister for item creation
    manager.removePeerListener(this);
    
    //2. Clear everything
    Iterator iter = objectToSortableItem.values().iterator();
    while(iter.hasNext()) {
      PeerRow row = (PeerRow) iter.next();
      TableItem tableItem = row.getTableItem();
      tableItemToObject.remove(tableItem);
      row.delete();
      iter.remove();
    }
    
    //3. Dispose the old table
    table.dispose();
    menu.dispose();
    
    //4. Re-create the table
    createMenu();
    createTable();
    
    //5. Re-add as a listener
    manager.addPeerListener(this);
    panel.layout();
  }

  private void createTable() {
    table = new Table(panel, SWT.MULTI | SWT.FULL_SELECTION);
    table.setLinesVisible(false);
    
    sorter = new TableSorter(this, "PeersView", "pieces",true);
    ControlListener resizeListener = new ControlAdapter() {
      public void controlResized(ControlEvent e) {
        TableColumn column = (TableColumn) e.widget;
        Utils.saveTableColumn(column);
        synchronized(objectToSortableItem) {
          Iterator iter = objectToSortableItem.values().iterator();
          while(iter.hasNext()) {
            PeerRow row = (PeerRow) iter.next();
            row.invalidate();
          }
        }        
        int columnNumber = table.indexOf(column);
        PeersViewEventDispacher.getInstance().columnSizeChanged(columnNumber,column.getWidth());
      }
    };
    
    itemEnumerator = PeersViewItemEnumerator.getItemEnumerator();    
    ItemDescriptor[] items = itemEnumerator.getItems();
    
    //Create all columns
    for (int i = 0; i < items.length; i++) {
      int position = items[i].getPosition();
      if (position != -1) {
        new TableColumn(table, SWT.NULL);
      }
    }
    //Assign length and titles
    //We can only do it after ALL columns are created, as position (order)
    //may not be in the natural order (if the user re-order the columns).    
    for (int i = 0; i < items.length; i++) {
      int position = items[i].getPosition();
      if(position != -1) {
        TableColumn column = table.getColumn(position);
        Messages.setLanguageText(column, "PeersView." + items[i].getName());
        column.setAlignment(items[i].getAlign());
        column.setWidth(items[i].getWidth());
        if (items[i].getType() == ItemDescriptor.TYPE_INT) {
          sorter.addIntColumnListener(column, items[i].getName());
        }
        if (items[i].getType() == ItemDescriptor.TYPE_STRING) {
          sorter.addStringColumnListener(column, items[i].getName());
        }
        column.setData("configName", "Table.Peers." + items[i].getName());
        column.addControlListener(resizeListener);
      }
    }   

    table.addPaintListener(new PaintListener() {
    	public void paintControl(PaintEvent event) {
        if(event.width == 0 || event.height == 0) return;
    		doPaint(new Rectangle(event.x,event.y,event.width,event.height));
    	}
    });
    
    table.setHeaderVisible(true);
    table.setMenu(menu);
  }

  private void createMenu() {
    menu = new Menu(panel.getShell(), SWT.POP_UP);
    final MenuItem item = new MenuItem(menu, SWT.CHECK);
    Messages.setLanguageText(item, "PeersView.menu.snubbed"); //$NON-NLS-1$
    
    new MenuItem(menu, SWT.SEPARATOR);

    final MenuItem itemChangeTable = new MenuItem(menu, SWT.PUSH);
    Messages.setLanguageText(itemChangeTable, "MyTorrentsView.menu.editTableColumns"); //$NON-NLS-1$
    itemChangeTable.setImage(ImageRepository.getImage("columns"));
    
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
        PEPeer peer = (PEPeer) tableItemToObject.get(ti);
        if (peer != null)
          item.setSelection(peer.isSnubbed());
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
          PEPeer peer = (PEPeer) tableItemToObject.get(ti);
          if (peer != null)
            peer.setSnubbed(item.getSelection());
        }
      }
    });
    
    itemChangeTable.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
          new EnumeratorEditor(table.getDisplay(),PeersViewItemEnumerator.getItemEnumerator(),PeersViewEventDispacher.getInstance(),"PeersView");       
      }
    });
  }


  public Composite getComposite() {
    return panel;
  }


  public void refresh() {
    if (getComposite() == null || getComposite().isDisposed())
      return;
    
    sorter.reOrder(false);

    loopFactor++;
    //Refresh all items in table...
    synchronized (objectToSortableItem) {
      
      Iterator iter = objectToSortableItem.values().iterator();
      while (iter.hasNext()) {
        PeerRow pr = (PeerRow) iter.next();
        
        // Every N GUI updates we unvalidate the images
        if (loopFactor % graphicsUpdate == 0)
          pr.invalidate();
        
        pr.refresh();
        
      }
    }
  }
  
  private void doPaint(Rectangle clipping) {
  	if (getComposite() == null || getComposite().isDisposed())
  		return;    
    
  	synchronized(objectToSortableItem) {
  		Iterator iter = objectToSortableItem.values().iterator();
  		while (iter.hasNext()) {
  			PeerRow pr = (PeerRow) iter.next();  		  			
  			pr.doPaint(clipping);  			
  		}
  	}
  }

  public void delete() {
    manager.removePeerListener(this);
    PeersViewEventDispacher.getInstance().removeListener(this);
    Iterator iter = objectToSortableItem.values().iterator();
    while (iter.hasNext()) {
      PeerRow item = (PeerRow) iter.next();
      item.delete();
    }
    if(table != null && ! table.isDisposed())
      table.dispose();
    COConfigurationManager.removeParameterListener("Graphics Update", this);
    COConfigurationManager.removeParameterListener("ReOrder Delay", sorter);
   }

  public String getData() {
    return "PeersView.title.short"; //$NON-NLS-1$
  }


  public String getFullTitle() {
    return MessageText.getString("PeersView.title.full"); //$NON-NLS-1$
  }

  public void peerAdded(PEPeer created) {
    synchronized (objectToSortableItem) {
      if (objectToSortableItem.containsKey(created))
        return;
      try {
        PeerRow item = new PeerRow(this,table, created);
        objectToSortableItem.put(created, item);
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public void peerRemoved(PEPeer removed) {
    PeerRow item;
    synchronized (objectToSortableItem) {
      item = (PeerRow) objectToSortableItem.remove(removed);
    }
    if (item == null)
      return;
    TableItem ti = item.getTableItem();
    if(ti != null)
      tableItemToObject.remove(ti);
    item.delete(); 
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
   * @see org.gudy.azureus2.core3.config.ParameterListener#parameterChanged(java.lang.String)
   */
  public void parameterChanged(String parameterName) {
    graphicsUpdate = COConfigurationManager.getIntParameter("Graphics Update");
  }
  
  public void columnSizeChanged(int columnNumber, int newWidth) {
    if(table == null || table.isDisposed())
      return;
    TableColumn column = table.getColumn(columnNumber);
    if(column == null || column.isDisposed())
      return;
    if(column.getWidth() == newWidth)
      return;
    column.setWidth(newWidth);
  }
  
  
  
}
