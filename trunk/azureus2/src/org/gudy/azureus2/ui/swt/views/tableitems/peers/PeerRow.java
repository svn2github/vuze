package org.gudy.azureus2.ui.swt.views.tableitems.peers;

import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.core3.peer.PEPeerStats;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.MainWindow;
import org.gudy.azureus2.ui.swt.views.PeersView;
import org.gudy.azureus2.ui.swt.views.utils.SortableItem;

/**
 * This class (GUI) represents a row into the peers table.
 * 
 * @author Olivier
 *
 */
public class PeerRow implements SortableItem {

  public static final HashMap tableItems = new HashMap();

  private Display display;
  private Table table;
  private PEPeer peerSocket;
  private TableItem item;
  private Listener listener;
  
  //This is used for caching purposes of the Image
  private boolean valid;
  private Image image;
  private String[] oldTexts;

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
    this.oldTexts = new String[19];
    for (int i = 0; i < oldTexts.length; i++)
      oldTexts[i] = "";	
    final PeerRow thisItem = this;
    display.asyncExec(new Runnable() {
      public void run() {
        if (table == null || table.isDisposed())
          return;
                
          item = new TableItem(table, SWT.NULL);                
          view.setItem(item,pc);
          table.getColumn(5).addListener(SWT.Resize, listener = new Listener() {
            public void handleEvent(Event e) {
              valid = false;
            }
          });

        item.addDisposeListener(new DisposeListener() {
          public void widgetDisposed(DisposeEvent e) {
            if (image != null && !image.isDisposed())
              image.dispose();
          }
        });        
        tableItems.put(item, thisItem);
      }
    });  
  }

  public synchronized void updateImage() {

    if (display == null || display.isDisposed())
      return;

    //A small hack to insure valid won't pass twice with false value in the loop.
    final boolean _valid = this.valid;
    this.valid = true;

    if (item == null || item.isDisposed())
      return;
    //Compute bounds ...
    Rectangle bounds = item.getBounds(5);
    int width = bounds.width - 1;
    int x0 = bounds.x;
    int y0 = bounds.y + 1;
    int height = bounds.height - 3;
    if (width < 10 || height < 3)
      return;
    //Get the table GC
    GC gc = new GC(table);
    gc.setClipping(table.getClientArea());
    if (_valid) {
      //If the image is still valid, simply copy it :)
      gc.setForeground(MainWindow.grey);
      gc.drawImage(image, x0, y0);
      gc.drawRectangle(x0, y0, width, height);
      gc.dispose();
    }
    else {
      //Image is not valid anymore ... so 1st free it :)
      if (image != null && !image.isDisposed())
        image.dispose();
      image = new Image(display, width, height);

      //System.out.println(table.getHeaderHeight());

      GC gcImage = new GC(image);
      boolean available[] = peerSocket.getAvailable();
      if (available != null) {
        int nbPieces = available.length;

        for (int i = 0; i < width; i++) {
          int a0 = (i * nbPieces) / width;
          int a1 = ((i + 1) * nbPieces) / width;
          if (a1 == a0)
            a1++;
          if (a1 > nbPieces)
            a1 = nbPieces;
          int nbAvailable = 0;
          for (int j = a0; j < a1; j++)
            if (available[j])
              nbAvailable++;
          int index = (nbAvailable * 4) / (a1 - a0);
          //System.out.print(index);
          gcImage.setBackground(MainWindow.blues[index]);
          gcImage.fillRectangle(i,1,1,height);
        }
      }
      gcImage.dispose();
      gc.setForeground(MainWindow.grey);
      gc.drawImage(image, x0, y0);
      gc.drawRectangle(x0, y0, width, height);
      gc.dispose();
    }
  }

  public void updateAll() {

    if (display == null || display.isDisposed())
      return;
    /*display.asyncExec( new Runnable() {
      public void run()
      {*/
    if (item == null || item.isDisposed())
      return;

    String tmp;

    tmp = "";
    if (peerSocket.isSnubbed())
      tmp = "*";
    if (!(oldTexts[14].equals(tmp))) {
      item.setText(14, tmp);
      oldTexts[14] = tmp;
      if (peerSocket.isSnubbed())
        item.setForeground(MainWindow.grey);
      else
        item.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
    }

    tmp = peerSocket.getIp();
    if ((!oldTexts[0].equals(tmp))) {
      item.setText(0, tmp);
      oldTexts[0] = tmp;
    }

    tmp = "" + peerSocket.getPort();
    if ((!oldTexts[1].equals(tmp))) {
      item.setText(1, tmp);
      oldTexts[1] = tmp;
    }

    tmp = "L";
    boolean isIcoming = peerSocket.isIncoming();
    if (isIcoming)
      tmp = "R";
    if (!oldTexts[2].equals(tmp)) {
      item.setText(2, tmp);
      oldTexts[2] = tmp;
    }

    tmp = "";
    if (peerSocket.isInterested())
      tmp = "*";
    if (!(oldTexts[3].equals(tmp))) {
      item.setText(3, tmp);
      oldTexts[3] = tmp;
    }

    tmp = "";
    if (peerSocket.isChoked())
      tmp = "*";
    if (!(oldTexts[4].equals(tmp))) {
      item.setText(4, tmp);
      oldTexts[4] = tmp;
    }

    tmp = "";
    if (peerSocket.isInteresting())
      tmp = "*";
    if (!(oldTexts[9].equals(tmp))) {
      item.setText(9, tmp);
      oldTexts[9] = tmp;
    }

    tmp = "";
    if (peerSocket.isChoking())
      tmp = "*";
    if (!(oldTexts[10].equals(tmp))) {
      item.setText(10, tmp);
      oldTexts[10] = tmp;
    }

    boolean available[] = peerSocket.getAvailable();
    int sum = 0;
//    int availability[] = peerSocket.getManager().getAvailability();
    for (int i = 0; i < available.length; i++) {
      if (available[i]) {
        sum++;
      }
    }
    sum = (sum * 1000) / (available.length);
    tmp = (sum / 10) + "." + (sum % 10) + " %";
    if (!(oldTexts[6].equals(tmp))) {
      item.setText(6, tmp);
      oldTexts[6] = tmp;
    }

    tmp = "" + peerSocket.getClient();
    if (!(oldTexts[17].equals(tmp))) {
      item.setText(17, tmp);
      oldTexts[17] = tmp;
    }        

  }

  public void updateStats() {

    if (display == null || display.isDisposed())
      return;

    if (item == null || item.isDisposed())
      return;
    String tmp;
    PEPeerStats stats = peerSocket.getStats();

    tmp = DisplayFormatters.formatByteCountToKBEtcPerSec(stats.getDownloadAverage());
    if (!(oldTexts[7].equals(tmp))) {
      item.setText(7, tmp);
      oldTexts[7] = tmp;
    }

    tmp = DisplayFormatters.formatByteCountToKBEtc(stats.getTotalReceived());
    if (!(oldTexts[8].equals(tmp))) {
      item.setText(8, tmp);
      oldTexts[8] = tmp;
    }

    tmp = DisplayFormatters.formatByteCountToKBEtcPerSec( stats.getUploadAverage());
    if (!(oldTexts[11].equals(tmp))) {
      item.setText(11, tmp);
      oldTexts[11] = tmp;
    }

    tmp = DisplayFormatters.formatByteCountToKBEtc(stats.getTotalSent());
    if (!(oldTexts[12].equals(tmp))) {
      item.setText(12, tmp);
      oldTexts[12] = tmp;

    }

    tmp = DisplayFormatters.formatByteCountToKBEtcPerSec(stats.getStatisticSentAverage());
    if (!(oldTexts[13].equals(tmp))) {
      item.setText(13, tmp);
      oldTexts[13] = tmp;
    }

    tmp = DisplayFormatters.formatByteCountToKBEtcPerSec(stats.getTotalAverage());
    if (!(oldTexts[15].equals(tmp))) {
      item.setText(15, tmp);
      oldTexts[15] = tmp;
    }

    tmp = "";
    if (peerSocket.isOptimisticUnchoke())
      tmp = "*";
    if (!(oldTexts[16].equals(tmp))) {
      item.setText(16, tmp);
      oldTexts[16] = tmp;
    }
    
    tmp = "" + DisplayFormatters.formatByteCountToKBEtc(stats.getTotalDiscarded());
            if (!(oldTexts[18].equals(tmp))) {
              item.setText(18, tmp);
              oldTexts[18] = tmp;
            }

  }

  public void remove() {
    if (display == null || display.isDisposed())
      return;
    try {
	    display.syncExec(new Runnable() {
	      public void run() {
	        if (table == null || table.isDisposed())
	          return;
	        if (item == null || item.isDisposed())
	          return;
	        table.getColumn(5).removeListener(SWT.Resize, listener);
	        table.remove(table.indexOf(item));
	        item.dispose();
          if(tableItems == null)
            return;
	        tableItems.remove(item);
	      }
	    });
    }
    catch (Exception e) {
    	e.printStackTrace();
    }
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
    if(table == null || table.isDisposed() || item == null || item.isDisposed())
      return -1;
    return table.indexOf(item);
  }
  
  /*
   * SortableItem
   */
  
  public String getStringField(String field) {
    if (field.equals("ip")) //$NON-NLS-1$
      return peerSocket.getIp();

    if (field.equals("client")) //$NON-NLS-1$
      return peerSocket.getClient();

    return ""; //$NON-NLS-1$
  }

  public long getIntField(String field) {

    if (field.equals("port")) //$NON-NLS-1$
      return peerSocket.getPort();

    if (field.equals("done")) //$NON-NLS-1$
      return peerSocket.getPercentDone();

    if (field.equals("ds")) //$NON-NLS-1$
      return peerSocket.getStats().getDownloadAverage();

    if (field.equals("us")) //$NON-NLS-1$
      return peerSocket.getStats().getUploadAverage();

    if (field.equals("down")) //$NON-NLS-1$
      return peerSocket.getStats().getTotalReceived();

    if (field.equals("up")) //$NON-NLS-1$
      return peerSocket.getStats().getTotalSent();
    
    if (field.equals("su")) //$NON-NLS-1$
      return peerSocket.getStats().getStatisticSentAverage();
    
    if (field.equals("od")) //$NON-NLS-1$
      return peerSocket.getStats().getTotalAverage();
    
    if (field.equals("discarded"))
      return peerSocket.getStats().getTotalDiscarded();

    if (getBooleanField(field))
      return 1;

    return 0;
  }
  
  private boolean getBooleanField(String field) {
    if (field.equals("t")) //$NON-NLS-1$
      return peerSocket.isIncoming();

    if (field.equals("i")) //$NON-NLS-1$
      return peerSocket.isInterested();

    if (field.equals("c")) //$NON-NLS-1$
      return peerSocket.isChoked();

    if (field.equals("i2")) //$NON-NLS-1$
      return peerSocket.isInteresting();

    if (field.equals("i2")) //$NON-NLS-1$
      return peerSocket.isChoking();
    return false;
  } 

  public void setDataSource(Object dataSource) {
    peerSocket = (PEPeer) dataSource;
  }
  
  public TableItem getTableItem() {
    return item;
  }

}