/*
 * Created on 30 juin 2003
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
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.core.DownloadManager;
import org.gudy.azureus2.core.GlobalManager;

/**
 * @author Olivier
 * 
 */
public class MyTorrentsView implements IView, IComponentListener {

  //see Download Manager ... too lazy to put all state names ;)
  private static final int tabStates[][] = { { 0, 5, 10, 20, 30, 40, 50, 60, 70, 100 }, {
      0, 20, 30, 40 }, {
      50 }, {
      60 }, {
      65, 70 }
  };

  private GlobalManager globalManager;
  private String title = "My Torrents";

  private Composite panel;
  private Table table;
  private CTabFolder toolBar;
  private HashMap managerItems;
  private HashMap managers;

  private HashMap downloadBars;

  public MyTorrentsView(GlobalManager globalManager) {
    this.ascending = true;
    this.lastField = "";
    this.globalManager = globalManager;
    managerItems = new HashMap();
    managers = new HashMap();
    downloadBars = MainWindow.getWindow().getDownloadBars();
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#initialize(org.eclipse.swt.widgets.Composite)
   */
  public void initialize(Composite composite) {
    panel = new Composite(composite, SWT.NULL);
    GridLayout layout = new GridLayout(1, false);
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.verticalSpacing = 0;
    panel.setLayout(layout);

    GridData gridData = new GridData(GridData.FILL_HORIZONTAL);    

    gridData = new GridData(GridData.FILL_BOTH);
    table = new Table(panel, SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER);
    table.setLayoutData(gridData);
    String[] columnsHeader =
      { "Name", "Size", "Done", "Status", "Seeds", "Peers", "Down Speed", "Up Speed", "ETA", "Tracker", "Priority" };
    int[] columnsSize = { 250, 70, 55, 80, 45, 45, 70, 70, 70, 70, 70 };
    for (int i = 0; i < columnsHeader.length; i++) {
      TableColumn column = new TableColumn(table, SWT.NULL);
      column.setText(columnsHeader[i]);
      column.setWidth(columnsSize[i]);
    }
    table.getColumn(0).addListener(SWT.Selection,new StringColumnListener("name"));
    table.getColumn(1).addListener(SWT.Selection,new IntColumnListener("size"));
    table.getColumn(2).addListener(SWT.Selection,new IntColumnListener("done"));
    table.getColumn(3).addListener(SWT.Selection,new IntColumnListener("status"));
    table.getColumn(4).addListener(SWT.Selection,new IntColumnListener("seeds"));
    table.getColumn(5).addListener(SWT.Selection,new IntColumnListener("peers"));
    table.getColumn(6).addListener(SWT.Selection,new StringColumnListener("ds"));
    table.getColumn(7).addListener(SWT.Selection,new StringColumnListener("us"));
    table.getColumn(8).addListener(SWT.Selection,new StringColumnListener("eta"));
    table.getColumn(9).addListener(SWT.Selection,new StringColumnListener("tracker"));
    table.getColumn(10).addListener(SWT.Selection,new IntColumnListener("priority"));
    
    
    table.setHeaderVisible(true);
    final Menu menu = new Menu(composite.getShell(), SWT.POP_UP);

    final MenuItem itemDetails = new MenuItem(menu, SWT.PUSH);
    itemDetails.setText("Show details");
    menu.setDefaultItem(itemDetails);

    final MenuItem itemBar = new MenuItem(menu, SWT.RADIO);
    itemBar.setText("Show download Bar");

    new MenuItem(menu, SWT.SEPARATOR);

    final MenuItem itemOpen = new MenuItem(menu, SWT.PUSH);
    itemOpen.setText("Open");

    new MenuItem(menu, SWT.SEPARATOR);

    final MenuItem itemPriority = new MenuItem(menu, SWT.CASCADE);
    itemPriority.setText("Set Priority");
    final Menu menuPriority = new Menu(composite.getShell(), SWT.DROP_DOWN);
    itemPriority.setMenu(menuPriority);
    final MenuItem itemHigh = new MenuItem(menuPriority, SWT.CASCADE);
    itemHigh.setText("High");
    final MenuItem itemLow = new MenuItem(menuPriority, SWT.CASCADE);
    itemLow.setText("Low");

    final MenuItem itemStart = new MenuItem(menu, SWT.PUSH);
    itemStart.setText("Start");

    final MenuItem itemStop = new MenuItem(menu, SWT.PUSH);
    itemStop.setText("Stop");

    new MenuItem(menu, SWT.SEPARATOR);

    final MenuItem itemRemove = new MenuItem(menu, SWT.PUSH);
    itemRemove.setText("Remove");

    menu.addListener(SWT.Show, new Listener() {
      public void handleEvent(Event e) {
        TableItem[] tis = table.getSelection();
        if (tis.length == 0) {
          itemStart.setEnabled(false);
          itemStop.setEnabled(false);
          itemRemove.setEnabled(false);
          return;
        }
        itemStart.setEnabled(false);
        itemStop.setEnabled(true);
        itemRemove.setEnabled(false);
        itemBar.setSelection(false);
        TableItem ti = tis[0];
        DownloadManager dm = (DownloadManager) managers.get(ti);
        if (dm != null) {
          if (downloadBars.containsKey(dm))
            itemBar.setSelection(true);
          int state = dm.getState();
          if (state == DownloadManager.STATE_STOPPED) {
            itemStop.setEnabled(false);
            itemRemove.setEnabled(true);
          }
          if (state == DownloadManager.STATE_WAITING
            || state == DownloadManager.STATE_STOPPED
            || state == DownloadManager.STATE_READY) {
            itemStart.setEnabled(true);
          }
        }

      }
    });

    itemStart.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        TableItem[] tis = table.getSelection();
        if (tis.length == 0) {
          return;
        }
        TableItem ti = tis[0];
        DownloadManager dm = (DownloadManager) managers.get(ti);
        if (dm != null) {
          if (dm.getState() == DownloadManager.STATE_WAITING || dm.getState() == DownloadManager.STATE_STOPPED) {
            dm.initialize();
          }
          if (dm.getState() == DownloadManager.STATE_READY) {
            dm.startDownload();
          }
        }
      }
    });

    itemStop.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        TableItem[] tis = table.getSelection();
        if (tis.length == 0) {
          return;
        }
        TableItem ti = tis[0];
        DownloadManager dm = (DownloadManager) managers.get(ti);
        if (dm != null) {
          dm.stopIt();
        }
      }
    });

    itemRemove.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        TableItem[] tis = table.getSelection();
        if (tis.length == 0) {
          return;
        }        
        TableItem ti = tis[0];
        DownloadManager dm = (DownloadManager) managers.get(ti);
        if (dm != null && dm.getState() == DownloadManager.STATE_STOPPED) {
          globalManager.removeDownloadManager(dm);
        }
      }
    });

    itemDetails.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        TableItem[] tis = table.getSelection();
        if (tis.length == 0) {
          return;
        }
        TableItem ti = tis[0];
        DownloadManager dm = (DownloadManager) managers.get(ti);
        MainWindow.getWindow().openManagerView(dm);
      }
    });

    itemOpen.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        TableItem[] tis = table.getSelection();
        if (tis.length == 0) {
          return;
        }
        TableItem ti = tis[0];
        DownloadManager dm = (DownloadManager) managers.get(ti);
        Program.launch(dm.getFileName());
      }
    });

    itemBar.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        TableItem[] tis = table.getSelection();
        if (tis.length == 0) {
          return;
        }
        TableItem ti = tis[0];
        DownloadManager dm = (DownloadManager) managers.get(ti);
        synchronized (downloadBars) {
          if (downloadBars.containsKey(dm)) {
            MinimizedWindow mw = (MinimizedWindow) downloadBars.remove(dm);
            mw.close();
          }
          else {
            MinimizedWindow mw = new MinimizedWindow(dm, panel.getShell());
            downloadBars.put(dm, mw);
          }
        }
      }
    });

    itemHigh.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        TableItem[] tis = table.getSelection();
        if (tis.length == 0) {
          return;
        }
        TableItem ti = tis[0];
        DownloadManager dm = (DownloadManager) managers.get(ti);
        dm.setPriority(DownloadManager.HIGH_PRIORITY);
      }
    });

    itemLow.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        TableItem[] tis = table.getSelection();
        if (tis.length == 0) {
          return;
        }
        TableItem ti = tis[0];
        DownloadManager dm = (DownloadManager) managers.get(ti);
        dm.setPriority(DownloadManager.LOW_PRIORITY);
      }
    });

    table.setMenu(menu);

    //toolBar.setSelection(itemAll);

    globalManager.addListener(this);
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getComposite()
   */
  public Composite getComposite() {
    return panel;
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#refresh()
   */
  public void refresh() {
    Iterator iter = managerItems.keySet().iterator();
    while (iter.hasNext()) {
      if (this.panel.isDisposed())
        return;
      DownloadManager manager = (DownloadManager) iter.next();
      ManagerItem item = (ManagerItem) managerItems.get(manager);
      if (item != null) {
        item.refresh();
      }
    }
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#delete()
   */
  public void delete() {
    globalManager.removeListener(this);
    MainWindow.getWindow().setMytorrents(null);
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getShortTitle()
   */
  public String getShortTitle() {
    return title;
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getFullTitle()
   */
  public String getFullTitle() {
    return title;
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IComponentListener#objectAdded(java.lang.Object)
   */
  public void objectAdded(Object created) {
    if (!(created instanceof DownloadManager))
      return;
    DownloadManager manager = (DownloadManager) created;
    synchronized (managerItems) {
      ManagerItem item = (ManagerItem) managerItems.get(manager);
      if (item == null)
        item = new ManagerItem(table, (DownloadManager) manager);
      managerItems.put(manager, item);
      managers.put(item.getTableItem(),manager);
    }
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IComponentListener#objectRemoved(java.lang.Object)
   */
  public void objectRemoved(Object removed) {
    MinimizedWindow mw = (MinimizedWindow) downloadBars.remove(removed);
    if (mw != null) {
      mw.close();
    }

    ManagerItem managerItem = (ManagerItem) managerItems.remove(removed);
    if (managerItem != null) {
      managerItem.delete();
    }
  }

  private String getStringField(DownloadManager manager, String field) {
    if (field.equals("name"))
      return manager.getName();

    if (field.equals("ds"))
      return manager.getDownloadSpeed();

    if (field.equals("us"))
      return manager.getUploadSpeed();

    if (field.equals("eta"))
      return manager.getETA();

    if (field.equals("tracker"))
      return manager.getTrackerStatus();

    if (field.equals("priority"))
      return manager.getName();

    return "";
  }

  private long getIntField(DownloadManager manager, String field) {

    if (field.equals("size"))
      return manager.getSize();

    if (field.equals("done"))
      return manager.getCompleted();

    if (field.equals("status"))
      return manager.getState();

    if (field.equals("seeds"))
      return manager.getNbSeeds();

    if (field.equals("peers"))
      return manager.getNbPeers();

    if (field.equals("priority"))
      return manager.getPriority();

    return 0;
  }

  //Ordering
  private boolean ascending;
  private String lastField;

  private void orderInt(String field) {
    if (lastField.equals(field))
      ascending = !ascending;
    else {
      lastField = field;
      ascending = true;
    }
    synchronized (managerItems) {
      List ordered = new ArrayList(managerItems.size());
      ManagerItem items[] = new ManagerItem[managerItems.size()];
      Iterator iter = managerItems.keySet().iterator();
      while (iter.hasNext()) {
        DownloadManager manager = (DownloadManager) iter.next();
        ManagerItem item = (ManagerItem) managerItems.get(manager);
        items[item.getIndex()] = item;
        long value = getIntField(manager, field);
        int i;
        for (i = 0; i < ordered.size(); i++) {
          DownloadManager manageri = (DownloadManager) ordered.get(i);
          long valuei = getIntField(manageri, field);
          if (ascending) {
            if (valuei >= value)
              break;
          }
          else {
            if (valuei <= value)
              break;
          }          
        }    
        ordered.add(i,manager);
      }
      
          
      
      for(int i = 0 ; i < ordered.size() ; i++) {
        DownloadManager manager = (DownloadManager) ordered.get(i);
        //ManagerItem item = (ManagerItem) managerItems.get(manager);
        //DownloadManager oldManager = items[i].getManager();
        
        items[i].setManager(manager);
        //item.setManager(oldManager);
                        
        managerItems.put(manager,items[i]);
        managers.put(items[i].getTableItem(),manager);
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
    synchronized (managerItems) {
      Collator collator = Collator.getInstance(Locale.getDefault());
      List ordered = new ArrayList(managerItems.size());
      ManagerItem items[] = new ManagerItem[managerItems.size()];
      Iterator iter = managerItems.keySet().iterator();
      while (iter.hasNext()) {
        DownloadManager manager = (DownloadManager) iter.next();
        ManagerItem item = (ManagerItem) managerItems.get(manager);
        items[item.getIndex()] = item;
        String value = getStringField(manager, field);
        int i;
        for (i = 0; i < ordered.size(); i++) {
          DownloadManager manageri = (DownloadManager) ordered.get(i);
          String valuei = getStringField(manageri, field);
          if (ascending) {
            if (collator.compare(valuei, value) <= 0)
              break;
          }
          else {
            if (collator.compare(valuei, value) >= 0)
              break;
          }          
        }    
        ordered.add(i,manager);
      }
      
          
      
      for(int i = 0 ; i < ordered.size() ; i++) {
        DownloadManager manager = (DownloadManager) ordered.get(i);
        //ManagerItem item = (ManagerItem) managerItems.get(manager);
        //DownloadManager oldManager = items[i].getManager();
        
        items[i].setManager(manager);
        //item.setManager(oldManager);
                        
        managerItems.put(manager,items[i]);
        managers.put(items[i].getTableItem(),manager);
      }
    }
  }

}
