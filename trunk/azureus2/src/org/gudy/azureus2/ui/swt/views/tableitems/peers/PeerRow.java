package org.gudy.azureus2.ui.swt.views.tableitems.peers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.ui.tables.peers.PluginPeerItemFactory;
import org.gudy.azureus2.pluginsimpl.ui.tables.peers.PeersTableExtensions;
import org.gudy.azureus2.ui.swt.components.BufferedTableItem;
import org.gudy.azureus2.ui.swt.components.BufferedTableRow;
import org.gudy.azureus2.ui.swt.views.PeersView;
import org.gudy.azureus2.ui.swt.views.tableitems.utils.ItemEnumerator;
import org.gudy.azureus2.ui.swt.views.utils.SortableItem;

/**
 * This class (GUI) represents a row into the peers table.
 * 
 * @author Olivier
 *
 */
public class PeerRow implements SortableItem {

  private Display display;
  private Table table;
  private PEPeer peerSocket;
  private BufferedTableRow row;
  private List items;
  private Map pluginItems;
  
  private boolean valid;

  /**
   * @return Returns the valid.
   */
  public boolean isValid() {
    return valid;
  }

  /**
   * @return Returns the row.
   */
  public BufferedTableRow getRow() {
    return row;
  }

  public PeerRow(final PeersView view, final Table table,final PEPeer pc) {
    if (table == null || table.isDisposed()) {
      this.display = null;
      this.table = null;
      this.peerSocket = null;
      return;
    }
    this.display = table.getDisplay();
    this.table = table;
    this.peerSocket = pc;
    this.valid = false;
    this.items = new ArrayList();
    this.pluginItems = new HashMap();
    display.asyncExec(new Runnable() {
      public void run() {
        if (table == null || table.isDisposed())
          return;
        ItemEnumerator itemEnumerator = view.getItemEnumerator();
        
        row = new BufferedTableRow(table, SWT.NULL);
        items.add(new IpItem(PeerRow.this,itemEnumerator.getPositionByName("ip")));
        items.add(new PortItem(PeerRow.this,itemEnumerator.getPositionByName("port")));
        items.add(new TypeItem(PeerRow.this,itemEnumerator.getPositionByName("T")));
        items.add(new InterestedItem(PeerRow.this,itemEnumerator.getPositionByName("I1")));
        items.add(new ChokedItem(PeerRow.this,itemEnumerator.getPositionByName("C1")));
        items.add(new PiecesItem(PeerRow.this,itemEnumerator.getPositionByName("pieces")));
        items.add(new PercentItem(PeerRow.this,itemEnumerator.getPositionByName("%")));
        items.add(new DownSpeedItem(PeerRow.this,itemEnumerator.getPositionByName("downloadspeed")));
        items.add(new DownItem(PeerRow.this,itemEnumerator.getPositionByName("download")));
        items.add(new InterestingItem(PeerRow.this,itemEnumerator.getPositionByName("I2")));
        items.add(new ChokingItem(PeerRow.this,itemEnumerator.getPositionByName("C2")));
        items.add(new UpSpeedItem(PeerRow.this,itemEnumerator.getPositionByName("uploadspeed")));        
        items.add(new UpItem(PeerRow.this,itemEnumerator.getPositionByName("upload")));
        items.add(new StatUpItem(PeerRow.this,itemEnumerator.getPositionByName("statup")));
        items.add(new SnubbedItem(PeerRow.this,itemEnumerator.getPositionByName("S")));
        items.add(new TotalDownSpeedItem(PeerRow.this,itemEnumerator.getPositionByName("downloadspeedoverall")));
        items.add(new OptimisticUnchokeItem(PeerRow.this,itemEnumerator.getPositionByName("optunchoke")));
        items.add(new ClientItem(PeerRow.this,itemEnumerator.getPositionByName("client")));
        items.add(new DiscardedItem(PeerRow.this,itemEnumerator.getPositionByName("discarded")));
        items.add(new UniquePieceItem(PeerRow.this,itemEnumerator.getPositionByName("uniquepiece")));
        items.add(new TimeToSendPieceItem(PeerRow.this,itemEnumerator.getPositionByName("timetosend")));
        items.add(new AllowedUpItem(PeerRow.this,itemEnumerator.getPositionByName("allowedup")));
        
        Map extensions = PeersTableExtensions.getInstance().getExtensions();
        Iterator iter = extensions.keySet().iterator();
        while(iter.hasNext()) {
          String name = (String) iter.next();
          PluginPeerItemFactory ppif = (PluginPeerItemFactory) extensions.get(name);
          PluginItem pi = new PluginItem(PeerRow.this,itemEnumerator.getPositionByName(name),ppif);
          items.add(pi);
          pluginItems.put(name,pi);          
        }
        
        view.setItem(row.getItem(),pc);
      }
    });  
  }

  public void refresh(boolean bDoGraphics) {
    if (display == null || display.isDisposed())
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
    valid = true;
  }
  
  /** Calls doPaint for each BufferentTableItem in row if that item 
      has needsPainting set to true.
    */
  public void doPaint(GC gc) {
  	if (table == null || table.isDisposed())
  		return;
  	if (row == null || row.isDisposed())
  		return;
  		
    Rectangle dirtyBounds = gc.getClipping();
    Rectangle tableBounds = table.getClientArea();
    // all OSes, scrollbars are excluded (I hope!)
    // some OSes (all?), table header is included in client area
    if (tableBounds.y < table.getHeaderHeight()) {
      tableBounds.y = table.getHeaderHeight();
    }

  	Iterator iter = items.iterator();
  	while(iter.hasNext()) {
  		BufferedTableItem item = (BufferedTableItem) iter.next();
  		if (item.needsPainting()) {
  		  Rectangle cellBounds = item.getBounds();
  		  if (cellBounds != null && cellBounds.y >= table.getHeaderHeight()) {
    		  Rectangle clippedBounds = dirtyBounds.intersection(cellBounds.intersection(tableBounds));
    		  if (clippedBounds.width > 0 && clippedBounds.height > 0) {
      		  gc.setClipping(clippedBounds);
/*
            debugOut("doPaint()"+gc+
                    "\nClippingOld: "+dirtyBounds+
                    "\nclippingNew: "+clippedBounds+
                    "\nclippingNew: "+gc.getClipping()+
                    "\ndirtyBounds: "+dirtyBounds+
                    "\ncellbounds: "+cellBounds,
                    false);
*/
      			item.doPaint(gc);
      			gc.setClipping(dirtyBounds);
      		}
    		}
  		}
  	}
  }

  public void delete() {
    if (display == null || display.isDisposed())
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

  public void invalidate() {
    this.valid = false;
  }

  public boolean isSnubbed() {
    return this.peerSocket.isSnubbed();
  }

  public void setSnubbed(boolean value) {
    this.peerSocket.setSnubbed(value);
  }

  /**
   * @return
   */
  public PEPeer getPeerSocket() {
    return peerSocket;
  }

  /**
   * @param socket
   */
  public void setPeerSocket(PEPeer socket) {
    peerSocket = socket;
  }

  public int getIndex() {
    if(table == null || table.isDisposed() || row == null || row.isDisposed())
      return -1;
    return table.indexOf(row.getItem());
  }
  
  /*
   * SortableItem
   */
  
  public String getStringField(String field) {
    if (field.equals("ip")) //$NON-NLS-1$
      return peerSocket.getIp();

    if (field.equals("client")) //$NON-NLS-1$
      return peerSocket.getClient();

    PluginItem item = (PluginItem)pluginItems.get(field);
    if(item != null)
      return item.pluginItem.getStringValue();
    
    return ""; //$NON-NLS-1$
  }

  public long getIntField(String field) {

    if (field.equals("port")) //$NON-NLS-1$
      return peerSocket.getPort();

    if (field.equals("pieces")) //$NON-NLS-1$
      return peerSocket.getPercentDone();
    
    if (field.equals("%")) //$NON-NLS-1$
      return peerSocket.getPercentDone();

    if (field.equals("downloadspeed")) //$NON-NLS-1$
      return peerSocket.getStats().getDownloadAverage();

    if (field.equals("uploadspeed")) //$NON-NLS-1$
      return peerSocket.getStats().getUploadAverage();

    if (field.equals("download")) //$NON-NLS-1$
      return peerSocket.getStats().getTotalReceived();

    if (field.equals("upload")) //$NON-NLS-1$
      return peerSocket.getStats().getTotalSent();
    
    if (field.equals("statup")) //$NON-NLS-1$
      return peerSocket.getStats().getStatisticSentAverage();
    
    if (field.equals("downloadspeedoverall")) //$NON-NLS-1$
      return peerSocket.getStats().getTotalAverage();
    
    if (field.equals("discarded"))
      return peerSocket.getStats().getTotalDiscarded();

    if(field.equals("uniquepiece"))
      return peerSocket.getUniqueAnnounce();
    
    if(field.equals("timetosend"))
      return peerSocket.getUploadHint();
        
    PluginItem item = (PluginItem)pluginItems.get(field);
    if(item != null)
      return item.pluginItem.getIntValue();
    
    if (getBooleanField(field))
      return 1;
    
    return 0;
  }
  
  private boolean getBooleanField(String field) {
    if (field.equals("T")) //$NON-NLS-1$
      return peerSocket.isIncoming();

    if (field.equals("I1")) //$NON-NLS-1$
      return peerSocket.isInterested();

    if (field.equals("C1")) //$NON-NLS-1$
      return peerSocket.isChoked();

    if (field.equals("optunchoke")) //$NON-NLS-1$
      return peerSocket.isOptimisticUnchoke();
    
    if (field.equals("I2")) //$NON-NLS-1$
      return peerSocket.isInteresting();

    if (field.equals("C2")) //$NON-NLS-1$
      return peerSocket.isChoking();
    
    if (field.equals("S")) //$NON-NLS-1$
      return peerSocket.isSnubbed();
    
    
    return false;
  } 

  public boolean setDataSource(Object dataSource) {
    if (peerSocket != (PEPeer) dataSource) {
      peerSocket = (PEPeer) dataSource;
      invalidate();
      return true;
    }
    return false;
  }
  
  public TableItem getTableItem() {
    if(row != null)
      return row.getItem();
    return null;
  }

  public void debugOut(String s, boolean bStackTrace) {
    if (table == null || table.isDisposed() || row == null)
      return;
    TableItem[] ti = table.getSelection();
    for (int i = 0; i < ti.length; i++) {
      if (ti[i] == row.getItem()) {
        System.out.println(s);
        if (bStackTrace) Debug.outStackTrace(3);
      }
    }
  }
}