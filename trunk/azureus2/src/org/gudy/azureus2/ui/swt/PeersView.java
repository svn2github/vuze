/*
 * Created on 2 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.core.DownloadManager;
import org.gudy.azureus2.core2.PeerSocket;

/**
 * @author Olivier
 * 
 */
public class PeersView implements IView, IComponentListener {

  DownloadManager manager;
  Table table;
  HashMap items;
  int loopFactor;

  public PeersView(DownloadManager manager) {
    this.manager = manager;
    items = new HashMap();
  }
  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#initialize(org.eclipse.swt.widgets.Composite)
   */
  public void initialize(Composite composite) {
    table = new Table(composite, SWT.SINGLE | SWT.FULL_SELECTION);
    table.setLinesVisible(false);
    table.setHeaderVisible(true);
    String[] titles =
      {
        "Ip",
        "Port",
        "T",
        "I",
        "C",
        "Pieces",
        "%",
        "Down Speed",
        "Down",
        "I",
        "C",
        "Up Speed",
        "Up",
        "Stat Up",
        "S",
        "Overall Down Speed",
        "Opt. Unchoke",
        "Client" };
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
        SWT.LEFT,
        SWT.LEFT,
        SWT.LEFT,
        SWT.LEFT };
    for (int i = 0; i < titles.length; i++) {
      TableColumn column = new TableColumn(table, align[i]);
      column.setText(titles[i]);
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
    table.getColumn(17).setWidth(60);
    
    table.getColumn(0).addListener(SWT.Selection, new StringColumnListener("ip"));
    table.getColumn(1).addListener(SWT.Selection, new IntColumnListener("port"));
    table.getColumn(2).addListener(SWT.Selection, new IntColumnListener("t"));
    table.getColumn(3).addListener(SWT.Selection, new IntColumnListener("i"));
    table.getColumn(4).addListener(SWT.Selection, new IntColumnListener("c"));
    table.getColumn(5).addListener(SWT.Selection, new IntColumnListener("done"));
    table.getColumn(6).addListener(SWT.Selection, new IntColumnListener("done"));
    table.getColumn(7).addListener(SWT.Selection, new IntColumnListener("ds"));
    table.getColumn(8).addListener(SWT.Selection, new IntColumnListener("down"));
    table.getColumn(9).addListener(SWT.Selection, new IntColumnListener("i2"));
    table.getColumn(10).addListener(SWT.Selection, new IntColumnListener("c2"));
    table.getColumn(11).addListener(SWT.Selection, new IntColumnListener("us"));
    table.getColumn(12).addListener(SWT.Selection, new IntColumnListener("up"));
    table.getColumn(13).addListener(SWT.Selection, new IntColumnListener("su"));
    table.getColumn(14).addListener(SWT.Selection, new IntColumnListener("s"));
    table.getColumn(15).addListener(SWT.Selection, new IntColumnListener("od"));
    table.getColumn(16).addListener(SWT.Selection, new IntColumnListener("opt"));
    table.getColumn(17).addListener(SWT.Selection, new StringColumnListener("client"));
    
    

    final Menu menu = new Menu(composite.getShell(), SWT.POP_UP);
    final MenuItem item = new MenuItem(menu, SWT.CHECK);
    item.setText("Snubbed");

    menu.addListener(SWT.Show, new Listener() {
      public void handleEvent(Event e) {
        TableItem[] tis = table.getSelection();
        if (tis.length == 0) {
          item.setEnabled(false);
          return;
        }
        item.setEnabled(true);
        TableItem ti = tis[0];
        PeerTableItem pti = (PeerTableItem) PeerTableItem.tableItems.get(ti);
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
        TableItem ti = tis[0];
        PeerTableItem pti = (PeerTableItem) PeerTableItem.tableItems.get(ti);
        if (pti != null)
          pti.setSnubbed(item.getSelection());
      }
    });
    table.setMenu(menu);

    manager.addListener(this);

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
    loopFactor++;
    //Refresh all items in table...
    synchronized (items) {

      Iterator iter = items.values().iterator();
      while (iter.hasNext()) {
        PeerTableItem pti = (PeerTableItem) iter.next();
        pti.updateAll();
        pti.updateStats();
        //Every second, we unvalidate the images.
        if (loopFactor % 8 == 0)
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
    Iterator iter = items.values().iterator();
    while (iter.hasNext()) {
      PeerTableItem item = (PeerTableItem) iter.next();
      item.remove();
    }
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getShortTitle()
   */
  public String getShortTitle() {
    return "Details";
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getFullTitle()
   */
  public String getFullTitle() {
    return "Details";
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IComponentListener#objectAdded(java.lang.Object)
   */
  public void objectAdded(Object created) {
    if (!(created instanceof PeerSocket))
      return;
    synchronized (items) {
      if (items.containsKey(created))
        return;
      //System.out.println("adding : " + created.getClass() + ":" + created);
      PeerTableItem item = new PeerTableItem(table, (PeerSocket) created);
      items.put(created, item);
    }
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IComponentListener#objectRemoved(java.lang.Object)
   */
  public void objectRemoved(Object removed) {
    //System.out.println("removed : " + removed.getClass() + ":" + removed);
    PeerTableItem item;
    synchronized (items) {
      item = (PeerTableItem) items.remove(removed);
    }
    if (item == null)
      return;

    item.remove();
    //System.out.println("PC removed"); 
  }

  //Sorting methods
  private boolean getBooleanFiedl(PeerSocket peerSocket, String field) {
    if (field.equals("t"))
      return peerSocket.isIncoming();

    if (field.equals("i"))
      return peerSocket.isInterested();

    if (field.equals("c"))
      return peerSocket.isChoked();

    if (field.equals("i2"))
      return peerSocket.isInteresting();

    if (field.equals("i2"))
      return peerSocket.isChoking();
    return false;
  }
  
  private String getStringField(PeerSocket peerSocket, String field) {
    if (field.equals("ip"))
      return peerSocket.getIp();

    if (field.equals("client"))
      return peerSocket.getClient();    

    return "";
  }

  private long getIntField(PeerSocket peerSocket, String field) {

    if (field.equals("port"))
      return peerSocket.getPort();

    if (field.equals("done"))
      return peerSocket.getPercentDone();
 
    if (field.equals("ds"))
      return peerSocket.getStats().getDownloadSpeedRaw();

    if (field.equals("us"))
      return peerSocket.getStats().getuploadSpeedRaw();

    if (field.equals("down"))
      return peerSocket.getStats().getTotalReceivedRaw();

    if (field.equals("up"))
      return peerSocket.getStats().getTotalSentRaw();   

    if(getBooleanFiedl(peerSocket,field))
      return 1;
      
    return 0;
  }

  private boolean ascending = false;
  private String lastField = "";

  private void orderInt(String field) {
    if (lastField.equals(field))
      ascending = !ascending;
    else {
      lastField = field;
      ascending = true;
    }
    synchronized (items) {
      List ordered = new ArrayList(items.size());
      PeerTableItem psItems[] = new PeerTableItem[items.size()];
      Iterator iter = items.keySet().iterator();
      while (iter.hasNext()) {
        PeerSocket peerSocket = (PeerSocket) iter.next();
        PeerTableItem item = (PeerTableItem) items.get(peerSocket);
        psItems[item.getIndex()] = item;
        long value = getIntField(peerSocket, field);
        int i;
        for (i = 0; i < ordered.size(); i++) {
          PeerSocket peerSocketi = (PeerSocket) ordered.get(i);
          long valuei = getIntField(peerSocketi, field);
          if (ascending) {
            if (valuei >= value)
              break;
          }
          else {
            if (valuei <= value)
              break;
          }
        }
        ordered.add(i, peerSocket);
      }

      for (int i = 0; i < ordered.size(); i++) {
        PeerSocket peerSocket = (PeerSocket) ordered.get(i);
        psItems[i].setPeerSocket(peerSocket);
        psItems[i].invalidate();
        items.put(peerSocket, psItems[i]);   
             
      }
    }
  }

  private class IntColumnListener implements Listener {

    private String field;

    public IntColumnListener(String field) {
      this.field = field;
    }

    public void handleEvent(Event e) {
      orderInt(field);
    }
  }

  private class StringColumnListener implements Listener {

    private String field;

    public StringColumnListener(String field) {
      this.field = field;
    }

    public void handleEvent(Event e) {
      orderString(field);
    }
  }

  private void orderString(String field) {
    if (lastField.equals(field))
      ascending = !ascending;
    else {
      lastField = field;
      ascending = true;
    }
    synchronized (items) {
      Collator collator = Collator.getInstance(Locale.getDefault());
      List ordered = new ArrayList(items.size());
      PeerTableItem psItems[] = new PeerTableItem[items.size()];
      Iterator iter = items.keySet().iterator();
      while (iter.hasNext()) {
        PeerSocket peerSocket = (PeerSocket) iter.next();
        PeerTableItem item = (PeerTableItem) items.get(peerSocket);
        psItems[item.getIndex()] = item;
        String value = getStringField(peerSocket, field);
        int i;
        for (i = 0; i < ordered.size(); i++) {
          PeerSocket peerSocketi = (PeerSocket) ordered.get(i);
          String valuei = getStringField(peerSocketi, field);
          if (ascending) {
            if (collator.compare(valuei, value) <= 0)
              break;
          }
          else {
            if (collator.compare(valuei, value) >= 0)
              break;
          }
        }
        ordered.add(i, peerSocket);
      }

      for (int i = 0; i < ordered.size(); i++) {
        PeerSocket peerSocket = (PeerSocket) ordered.get(i);
        psItems[i].setPeerSocket(peerSocket);
        psItems[i].invalidate();
        items.put(peerSocket, psItems[i]);
      }
    }
  }

}
