/*
 * Created on 30 juin 2003
 *
 */
package org.gudy.azureus2.ui.swt;

import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
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
    this.globalManager = globalManager;
    managerItems = new HashMap();
    managers = new HashMap();
    downloadBars = MainWindow.getWindow().getDownloadBars();
  }

  private class TabListener implements Listener {

    public void handleEvent(Event e) {
      synchronized (managerItems) {
        Iterator iter = managerItems.keySet().iterator();
        while (iter.hasNext()) {
          DownloadManager manager = (DownloadManager) iter.next();
          checkItem(manager, getCurrentStates());
        }
      }
    }
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
    toolBar = new CTabFolder(panel, SWT.TOP);
    toolBar.setSelectionBackground(new Color[] { MainWindow.white }, new int[0]);
    toolBar.setLayoutData(gridData);
    CTabItem itemAll = new CTabItem(toolBar, SWT.NULL);
    itemAll.setText("All");
    itemAll.setControl(new Label(toolBar, SWT.NULL));

    CTabItem itemWaiting = new CTabItem(toolBar, SWT.NULL);
    itemWaiting.setText("Waiting");
    itemWaiting.setControl(new Label(toolBar, SWT.NULL));

    CTabItem itemDownloading = new CTabItem(toolBar, SWT.NULL);
    itemDownloading.setText("Downloading");
    itemDownloading.setControl(new Label(toolBar, SWT.NULL));

    CTabItem itemSeeding = new CTabItem(toolBar, SWT.NULL);
    itemSeeding.setText("Seeding");
    itemSeeding.setControl(new Label(toolBar, SWT.NULL));

    CTabItem itemStopped = new CTabItem(toolBar, SWT.NULL);
    itemStopped.setText("Stopped");
    itemStopped.setControl(new Label(toolBar, SWT.NULL));

    gridData = new GridData(GridData.FILL_BOTH);
    table = new Table(panel, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER);
    table.setLayoutData(gridData);
    String[] columnsHeader =
      { "Name", "Size", "Done", "Status", "Seeds", "Peers", "Down Speed", "Up Speed", "ETA", "Tracker", "Priority" };
    int[] columnsSize = { 250, 70, 55, 80, 45, 45, 70, 70, 70, 70, 70 };
    for (int i = 0; i < columnsHeader.length; i++) {
      TableColumn column = new TableColumn(table, SWT.NULL);
      column.setText(columnsHeader[i]);
      column.setWidth(columnsSize[i]);
    }
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
          if (state == DownloadManager.STATE_WAITING || state == DownloadManager.STATE_STOPPED || state == DownloadManager.STATE_READY) {
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
          if(dm.getState() == DownloadManager.STATE_READY) {
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

    toolBar.setSelection(itemAll);

    globalManager.addListener(this);
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getComposite()
   */
  public Composite getComposite() {
    // TODO Auto-generated method stub
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
      checkItem(manager, getCurrentStates());
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
    checkItem((DownloadManager) created, getCurrentStates());
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

  public boolean contains(int values[], int value) {
    for (int i = 0; i < values.length; i++) {
      if (values[i] == value)
        return true;
    }
    return false;
  }

  public int[] getCurrentStates() {
    if (toolBar.isDisposed())
      return new int[0];
    /*
    ToolItem items[] = toolBar.getItems();
    int selected = 0;
    for (; selected < items.length; selected++) {
      if (items[selected].getSelection())
        break;
    }*/
    return MyTorrentsView.tabStates[toolBar.getSelectionIndex()];
  }

  public void checkItem(DownloadManager manager, int[] states) {
    ManagerItem item = (ManagerItem) managerItems.get(manager);
    int state = ((DownloadManager) manager).getState();
    if (item == null && contains(states, state))
      item = new ManagerItem(table, (DownloadManager) manager);
    if (item != null && !contains(states, state)) {
      item.delete();
      managers.remove(item);
      item = null;
    }
    managerItems.put(manager, item);
    if (item != null && item.getTableItem() != null)
      managers.put(item.getTableItem(), manager);
  }

}
