/*
 * Created on 30 juin 2003
 *
 */
package org.gudy.azureus2.ui.swt.views;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.*;
import org.gudy.azureus2.core3.tracker.host.*;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.MainWindow;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.MinimizedWindow;
import org.gudy.azureus2.ui.swt.TrackerChangerWindow;
import org.gudy.azureus2.ui.swt.views.tableitems.ManagerItem;
import org.gudy.azureus2.ui.swt.exporttorrent.wizard.*;

/**
 * @author Olivier
 * 
 */
public class MyTorrentsView extends AbstractIView implements GlobalManagerListener {

  /* see Download Manager ... too lazy to put all state names ;)
    private static final int tabStates[][] = { { 0, 5, 10, 20, 30, 40, 50, 60, 70, 100 }, {
        0, 20, 30, 40 }, {
        50 }, {
        60 }, {
        65, 70 }
    };
  */
  private GlobalManager globalManager;

  private Composite composite;
  private Composite panel;
  private Table table;
  private HashMap managerItems;
  private HashMap managers;
  private Menu menu;

  private HashMap downloadBars;

  public MyTorrentsView(GlobalManager globalManager) {
    this.ascending = true;
    this.lastField = "#"; //$NON-NLS-1$
    this.type = INT;
    this.globalManager = globalManager;
    managerItems = new HashMap();
    managers = new HashMap();
    downloadBars = MainWindow.getWindow().getDownloadBars();
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#initialize(org.eclipse.swt.widgets.Composite)
   */
  public void initialize(Composite composite0) {
    if(panel != null) {      
      return;
    }
    composite = new Composite(composite0, SWT.NULL);
    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    layout.horizontalSpacing = 0;
    layout.verticalSpacing = 0;
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    composite.setLayout(layout);
    GridData gridData = new GridData(GridData.FILL_BOTH);
          
    panel = new Composite(composite, SWT.NULL);
    panel.setLayoutData(gridData);
    
    layout = new GridLayout(1, false);
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.verticalSpacing = 0;
    layout.horizontalSpacing = 0;
    panel.setLayout(layout);
    
      
    table = new Table(panel, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER);
    gridData = new GridData(GridData.FILL_BOTH); 
    table.setLayoutData(gridData);
    String[] columnsHeader = { "#", "name", "size", "done", "status", "seeds", "peers", "downspeed", "upspeed", "eta", "tracker", "priority" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$
    int[] columnsSize = { 25, 250, 70, 55, 80, 45, 45, 70, 70, 70, 70, 70 };
    for (int i = 0; i < columnsHeader.length; i++) {
      columnsSize[i] = COConfigurationManager.getIntParameter("MyTorrentsView." + columnsHeader[i], columnsSize[i]);
    }

    ControlListener resizeListener = new ControlAdapter() {
      public void controlResized(ControlEvent e) {
        saveTableColumns((TableColumn) e.widget);
      }
    };
    for (int i = 0; i < columnsHeader.length; i++) {
      TableColumn column = new TableColumn(table, SWT.NULL);
      Messages.setLanguageText(column, "MyTorrentsView." + columnsHeader[i]);
      column.setWidth(columnsSize[i]);
      column.addControlListener(resizeListener);
    }
    table.getColumn(0).addListener(SWT.Selection, new IntColumnListener("#")); //$NON-NLS-1$
    table.getColumn(1).addListener(SWT.Selection, new StringColumnListener("name")); //$NON-NLS-1$
    table.getColumn(2).addListener(SWT.Selection, new IntColumnListener("size")); //$NON-NLS-1$
    table.getColumn(3).addListener(SWT.Selection, new IntColumnListener("done")); //$NON-NLS-1$
    table.getColumn(4).addListener(SWT.Selection, new IntColumnListener("status")); //$NON-NLS-1$
    table.getColumn(5).addListener(SWT.Selection, new IntColumnListener("seeds")); //$NON-NLS-1$
    table.getColumn(6).addListener(SWT.Selection, new IntColumnListener("peers")); //$NON-NLS-1$
    table.getColumn(7).addListener(SWT.Selection, new StringColumnListener("ds")); //$NON-NLS-1$
    table.getColumn(8).addListener(SWT.Selection, new StringColumnListener("us")); //$NON-NLS-1$
    table.getColumn(9).addListener(SWT.Selection, new StringColumnListener("eta")); //$NON-NLS-1$
    table.getColumn(10).addListener(SWT.Selection, new StringColumnListener("tracker")); //$NON-NLS-1$
    table.getColumn(11).addListener(SWT.Selection, new IntColumnListener("priority")); //$NON-NLS-1$

    table.setHeaderVisible(true);
    table.addKeyListener(createKeyListener());

    menu = new Menu(composite.getShell(), SWT.POP_UP);

    final MenuItem itemDetails = new MenuItem(menu, SWT.PUSH);
    Messages.setLanguageText(itemDetails, "MyTorrentsView.menu.showdetails"); //$NON-NLS-1$
    menu.setDefaultItem(itemDetails);

    final MenuItem itemBar = new MenuItem(menu, SWT.CHECK);
    Messages.setLanguageText(itemBar, "MyTorrentsView.menu.showdownloadbar"); //$NON-NLS-1$

    new MenuItem(menu, SWT.SEPARATOR);

  	final MenuItem itemOpen = new MenuItem(menu, SWT.PUSH);
  	Messages.setLanguageText(itemOpen, "MyTorrentsView.menu.open"); //$NON-NLS-1$
  
  	final MenuItem itemExport = new MenuItem(menu, SWT.PUSH);
  	Messages.setLanguageText(itemExport, "MyTorrentsView.menu.export"); //$NON-NLS-1$
  	
  	final MenuItem itemHost = new MenuItem(menu, SWT.PUSH);
  	Messages.setLanguageText(itemHost, "MyTorrentsView.menu.host"); //$NON-NLS-1$

    new MenuItem(menu, SWT.SEPARATOR);

    final MenuItem itemMove = new MenuItem(menu, SWT.CASCADE);
    Messages.setLanguageText(itemMove, "MyTorrentsView.menu.move"); //$NON-NLS-1$
    final Menu menuMove = new Menu(composite.getShell(), SWT.DROP_DOWN);
    itemMove.setMenu(menuMove);
    final MenuItem itemMoveUp = new MenuItem(menuMove, SWT.PUSH);
    Messages.setLanguageText(itemMoveUp, "MyTorrentsView.menu.moveUp"); //$NON-NLS-1$
    final MenuItem itemMoveDown = new MenuItem(menuMove, SWT.PUSH);
    Messages.setLanguageText(itemMoveDown, "MyTorrentsView.menu.moveDown"); //$NON-NLS-1$

    final MenuItem itemPriority = new MenuItem(menu, SWT.CASCADE);
    Messages.setLanguageText(itemPriority, "MyTorrentsView.menu.setpriority"); //$NON-NLS-1$
    final Menu menuPriority = new Menu(composite.getShell(), SWT.DROP_DOWN);
    itemPriority.setMenu(menuPriority);
    final MenuItem itemHigh = new MenuItem(menuPriority, SWT.PUSH);
    Messages.setLanguageText(itemHigh, "MyTorrentsView.menu.setpriority.high"); //$NON-NLS-1$
    final MenuItem itemLow = new MenuItem(menuPriority, SWT.PUSH);
    Messages.setLanguageText(itemLow, "MyTorrentsView.menu.setpriority.low"); //$NON-NLS-1$
    final MenuItem itemLockPriority = new MenuItem(menu, SWT.CHECK);
    Messages.setLanguageText(itemLockPriority, "MyTorrentsView.menu.lockpriority");
    
    new MenuItem(menu, SWT.SEPARATOR);

    final MenuItem itemLockStartStop = new MenuItem(menu, SWT.CHECK);
    Messages.setLanguageText(itemLockStartStop, "MyTorrentsView.menu.lockstartstop");
    
    final MenuItem itemStart = new MenuItem(menu, SWT.PUSH);
    Messages.setLanguageText(itemStart, "MyTorrentsView.menu.start"); //$NON-NLS-1$

    final MenuItem itemStop = new MenuItem(menu, SWT.PUSH);
    Messages.setLanguageText(itemStop, "MyTorrentsView.menu.stop"); //$NON-NLS-1$

    final MenuItem itemRemove = new MenuItem(menu, SWT.PUSH);
    Messages.setLanguageText(itemRemove, "MyTorrentsView.menu.remove"); //$NON-NLS-1$

    final MenuItem itemRemoveAnd = new MenuItem(menu, SWT.CASCADE);
    Messages.setLanguageText(itemRemoveAnd, "MyTorrentsView.menu.removeand"); //$NON-NLS-1$
    final Menu menuRemove = new Menu(composite.getShell(), SWT.DROP_DOWN);
    itemRemoveAnd.setMenu(menuRemove);
    final MenuItem itemDeleteTorrent = new MenuItem(menuRemove, SWT.PUSH);
    Messages.setLanguageText(itemDeleteTorrent, "MyTorrentsView.menu.removeand.deletetorrent"); //$NON-NLS-1$
    final MenuItem itemDeleteData = new MenuItem(menuRemove, SWT.PUSH);
    Messages.setLanguageText(itemDeleteData, "MyTorrentsView.menu.removeand.deletedata");
    final MenuItem itemDeleteBoth = new MenuItem(menuRemove, SWT.PUSH);
    Messages.setLanguageText(itemDeleteBoth, "MyTorrentsView.menu.removeand.deleteboth");

    new MenuItem(menu, SWT.SEPARATOR);

    final MenuItem itemChangeTracker = new MenuItem(menu, SWT.PUSH);
    Messages.setLanguageText(itemChangeTracker, "MyTorrentsView.menu.changeTracker"); //$NON-NLS-1$

    menu.addListener(SWT.Show, new Listener() {
      public void handleEvent(Event e) {
        TableItem[] tis = table.getSelection();

        itemDetails.setEnabled(false);
        itemBar.setEnabled(false);

        itemOpen.setEnabled(false);
        itemExport.setEnabled(false);
        itemHost.setEnabled(false);

        itemMove.setEnabled(false);
        itemPriority.setEnabled(false);
        itemLockPriority.setEnabled(false);

        itemLockStartStop.setEnabled(false);
        itemStart.setEnabled(false);
        itemStop.setEnabled(false);
        itemRemove.setEnabled(false);
        itemRemoveAnd.setEnabled(false);

        itemChangeTracker.setEnabled(false);

        if (tis.length > 0) {
          itemDetails.setEnabled(true);
          itemBar.setEnabled(true);

          itemOpen.setEnabled(true);
          itemExport.setEnabled(true);
          itemHost.setEnabled(true);

          itemMove.setEnabled(true);
          itemPriority.setEnabled(true);
          itemLockPriority.setEnabled(true);

          itemLockStartStop.setEnabled(true);
          itemStop.setEnabled(true);

          itemRemove.setEnabled(false);
          itemBar.setSelection(false);

          boolean moveUp, moveDown, start, stop, remove, changeUrl, barsOpened, lockPriority, lockStartStop;
          moveUp = moveDown = start = stop = remove = changeUrl = barsOpened = lockPriority = lockStartStop = true;
          for (int i = 0; i < tis.length; i++) {
            TableItem ti = tis[i];
            DownloadManager dm = (DownloadManager) managers.get(ti);
            if (dm != null) {
              if (dm.getTrackerClient() == null)
                changeUrl = false;
              if (!downloadBars.containsKey(dm))
                barsOpened = false;

              int state = dm.getState();
              if (state == DownloadManager.STATE_STOPPED) {
                stop = false;
              }
              if (state != DownloadManager.STATE_STOPPED
                && state != DownloadManager.STATE_ERROR
                && state != DownloadManager.STATE_DUPLICATE) {
                remove = false;
              }
              if (state != DownloadManager.STATE_STOPPED) {
                start = false;
              }

              if (!dm.isMoveableDown())
                moveDown = false;
              if (!dm.isMoveableUp())
                moveUp = false;
              
              if(!dm.isPriorityLocked())
                lockPriority = false;
              if(!dm.isStartStopLocked())
                lockStartStop = false;
            }
          }
          itemBar.setSelection(barsOpened);

          itemMoveDown.setEnabled(moveDown);
          itemMoveUp.setEnabled(moveUp);

          itemLockPriority.setSelection(lockPriority);
          
          itemLockStartStop.setSelection(lockStartStop);
          itemStart.setEnabled(start);
          itemStop.setEnabled(stop);
          itemRemove.setEnabled(remove);
          itemRemoveAnd.setEnabled(remove);

          itemChangeTracker.setEnabled(changeUrl);

        }
      }
    });

    itemStart.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        TableItem[] tis = table.getSelection();
        final boolean initStoppedDownloads = true;
        for (int i = 0; i < tis.length; i++) {
          TableItem ti = tis[i];
          DownloadManager dm = (DownloadManager) managers.get(ti);
          if (dm != null) {
            dm.setState(DownloadManager.STATE_WAITING);
          }
        }
      }
    });

    itemStop.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        TableItem[] tis = table.getSelection();
        for (int i = 0; i < tis.length; i++) {
          TableItem ti = tis[i];
          DownloadManager dm = (DownloadManager) managers.get(ti);
          if (dm != null) {
            if (dm.getState() == DownloadManager.STATE_SEEDING
              && dm.getStats().getShareRatio() >= 0
              && dm.getStats().getShareRatio() < 1000
              && COConfigurationManager.getBooleanParameter("Alert on close", true)) {
              MessageBox mb = new MessageBox(panel.getShell(), SWT.ICON_WARNING | SWT.YES | SWT.NO);
              mb.setText(MessageText.getString("seedmore.title"));
              mb.setMessage(
                MessageText.getString("seedmore.shareratio")
                  + (dm.getStats().getShareRatio() / 10)
                  + "%.\n"
                  + MessageText.getString("seedmore.uploadmore"));
              int action = mb.open();
              if (action == SWT.YES)
                dm.stopIt();
            }
            else {
              dm.stopIt();
            }

          }
        }
      }
    });

    itemRemove.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        TableItem[] tis = table.getSelection();
        for (int i = 0; i < tis.length; i++) {
          TableItem ti = tis[i];
          DownloadManager dm = (DownloadManager) managers.get(ti);
          if (dm != null
            && (dm.getState() == DownloadManager.STATE_STOPPED || dm.getState() == DownloadManager.STATE_ERROR)) {
            globalManager.removeDownloadManager(dm);
          }
        }
      }
    });

    itemDeleteTorrent.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        TableItem[] tis = table.getSelection();
        for (int i = 0; i < tis.length; i++) {
          TableItem ti = tis[i];
          DownloadManager dm = (DownloadManager) managers.get(ti);
          if (dm != null
            && (dm.getState() == DownloadManager.STATE_STOPPED || dm.getState() == DownloadManager.STATE_ERROR)) {
            globalManager.removeDownloadManager(dm);
            try {
              File f = new File(dm.getTorrentFileName());
              f.delete();
            }
            catch (Exception ex) {
              ex.printStackTrace();
            }
          }
        }
      }
    });

    itemDeleteData.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        TableItem[] tis = table.getSelection();
        for (int i = 0; i < tis.length; i++) {
          TableItem ti = tis[i];
          DownloadManager dm = (DownloadManager) managers.get(ti);
          if (dm != null
            && (dm.getState() == DownloadManager.STATE_STOPPED || dm.getState() == DownloadManager.STATE_ERROR)) {
            String path = getFilePath(dm);
            MessageBox mb = new MessageBox(panel.getShell(), SWT.ICON_WARNING | SWT.YES | SWT.NO);
            mb.setText(MessageText.getString("deletedata.title"));
            mb.setMessage(
              MessageText.getString("deletedata.message1")
                + dm.getName() + " :\n"
                + path 
                + MessageText.getString("deletedata.message2"));
            int choice = mb.open();
            if (choice == SWT.YES) {
              try {
                globalManager.removeDownloadManager(dm);                
                File f = new File(path);
                recursiveDelete(f);
              }
              catch (Exception ex) {
                ex.printStackTrace();
              }
            }
          }
        }
      }
    });

    itemDeleteBoth.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        TableItem[] tis = table.getSelection();
        for (int i = 0; i < tis.length; i++) {
          TableItem ti = tis[i];
          DownloadManager dm = (DownloadManager) managers.get(ti);
          if (dm != null
            && (dm.getState() == DownloadManager.STATE_STOPPED || dm.getState() == DownloadManager.STATE_ERROR)) {
            String path = getFilePath(dm);
            MessageBox mb = new MessageBox(panel.getShell(), SWT.ICON_WARNING | SWT.YES | SWT.NO);
            mb.setText(MessageText.getString("deletedata.title"));
            mb.setMessage(
              MessageText.getString("deletedata.message1")
                + dm.getName() + " :\n"
                + path 
                + MessageText.getString("deletedata.message2"));
            int choice = mb.open();
            if (choice == SWT.YES) {
              try {
                globalManager.removeDownloadManager(dm);                
                File f = new File(path);
                recursiveDelete(f);
                f = new File(dm.getTorrentFileName());
                f.delete();
              }
              catch (Exception ex) {
                ex.printStackTrace();
              }
            }
          }
        }
      }
    });

    itemChangeTracker.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        TableItem[] tis = table.getSelection();
        for (int i = 0; i < tis.length; i++) {
          TableItem ti = tis[i];
          DownloadManager dm = (DownloadManager) managers.get(ti);
          if (dm != null && dm.getTrackerClient() != null) {
            new TrackerChangerWindow(MainWindow.getWindow().getDisplay(), dm.getTrackerClient());
          }
        }
      }
    });

    itemDetails.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        TableItem[] tis = table.getSelection();
        for (int i = 0; i < tis.length; i++) {
          TableItem ti = tis[i];
          DownloadManager dm = (DownloadManager) managers.get(ti);
          MainWindow.getWindow().openManagerView(dm);
        }
      }
    });

    table.addMouseListener(new MouseAdapter() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.events.MouseAdapter#mouseDoubleClick(org.eclipse.swt.events.MouseEvent)
       */
      public void mouseDoubleClick(MouseEvent mEvent) {
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
        Program.launch(getFilePath(dm));
      }
    });

	itemExport.addListener(SWT.Selection, new Listener() {
	   public void handleEvent(Event event) {
		 TableItem[] tis = table.getSelection();
		 if (tis.length == 0) {
		   return;
		 }
		 TableItem ti = tis[0];
		 DownloadManager dm = (DownloadManager) managers.get(ti);
		 
		 new ExportTorrentWizard(itemExport.getDisplay(), dm);
	   }
	 });

	itemHost.addListener(SWT.Selection, new Listener() {
	   public void handleEvent(Event event) {

		 TableItem[] tis = table.getSelection();

		 for (int i = 0; i < tis.length; i++) {

		   TableItem ti = tis[i];
		 
			 DownloadManager dm = (DownloadManager) managers.get(ti);
		 
			 TOTorrent	torrent = dm.getTorrent();
		 
			 if ( torrent != null ){
		 
		 		TRHostFactory.create().addTorrent( torrent );
			 } 
		 }
		 
		 MainWindow.getWindow().showMyTracker();
	   }
	 });
	 
    itemBar.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        TableItem[] tis = table.getSelection();
        for (int i = 0; i < tis.length; i++) {
          TableItem ti = tis[i];
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
      }
    });

    itemMoveDown.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        TableItem[] tis = table.getSelection();
        for (int i = tis.length - 1; i >= 0; i--) {
          TableItem ti = tis[i];
          DownloadManager dm = (DownloadManager) managers.get(ti);
          if (dm != null && dm.isMoveableDown()) {
            dm.moveDown();

          }
        }
        if (lastField.equals("#"))
          reOrder(true);
      }
    });

    itemMoveUp.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        TableItem[] tis = table.getSelection();
        for (int i = 0; i < tis.length; i++) {
          TableItem ti = tis[i];
          DownloadManager dm = (DownloadManager) managers.get(ti);
          if (dm != null && dm.isMoveableUp()) {
            dm.moveUp();

          }
        }
        if (lastField.equals("#"))
          reOrder(true);
      }
    });

    itemHigh.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        TableItem[] tis = table.getSelection();
        for (int i = 0; i < tis.length; i++) {
          TableItem ti = tis[i];
          DownloadManager dm = (DownloadManager) managers.get(ti);
          dm.setPriority(DownloadManager.HIGH_PRIORITY);
        }
      }
    });

    itemLow.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        TableItem[] tis = table.getSelection();
        for (int i = 0; i < tis.length; i++) {
          TableItem ti = tis[i];
          DownloadManager dm = (DownloadManager) managers.get(ti);
          dm.setPriority(DownloadManager.LOW_PRIORITY);
        }
      }
    });
    
    itemLockPriority.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        TableItem[] tis = table.getSelection();
        for (int i = 0; i < tis.length; i++) {
          TableItem ti = tis[i];
          DownloadManager dm = (DownloadManager) managers.get(ti);
          dm.setPriorityLocked(itemLockPriority.getSelection());
        }
      }
    });
    
    itemLockStartStop.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        TableItem[] tis = table.getSelection();
        for (int i = 0; i < tis.length; i++) {
          TableItem ti = tis[i];
          DownloadManager dm = (DownloadManager) managers.get(ti);
          dm.setStartStopLocked(itemLockStartStop.getSelection());
        }
      }
    });

    table.setMenu(menu);

    //toolBar.setSelection(itemAll);
    /*DropTarget dt = new DropTarget(table,DND.DROP_LINK);
    Transfer[] transfers = {FileTransfer.getInstance()};
    dt.setTransfer(transfers);*/

    globalManager.addListener(this);
  }

  private String getFilePath(DownloadManager dm) {
    if (dm == null)
      return "";
    try {
      //The save path, for a directory torrent, it may be the directory itself
      String path = dm.getSavePath();
      //String name = LocaleUtil.getCharsetString(dm.getTorrent().getName());
      String name = dm.getName();
      String fullPath = path + System.getProperty("file.separator") + name;
      if (path.endsWith(name)) {
        File f = new File(fullPath);
        if(f.exists() && f.isDirectory())
          return fullPath;
        else
          return path;
      } else {
        return fullPath;
      }
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
    return "";
  }

  private void recursiveDelete(File f) {
    if (f.isDirectory()) {
      File[] files = f.listFiles();
      for (int i = 0; i < files.length; i++) {
        recursiveDelete(files[i]);
      }
      f.delete();
    }
    else {
      f.delete();
    }
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getComposite()
   */
  public Composite getComposite() {
    return composite;
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#refresh()
   */
  public void refresh() {
    if (getComposite() == null || getComposite().isDisposed())
      return;

    reOrder(false);

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

  private void saveTableColumns(TableColumn t) {
    COConfigurationManager.setParameter((String) t.getData(), t.getWidth());
    COConfigurationManager.save();
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
    return MessageText.getString("MyTorrentsView.mytorrents");
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getFullTitle()
   */
  public String getFullTitle() {
    return MessageText.getString("MyTorrentsView.mytorrents");
  }

   public void downloadManagerAdded(DownloadManager manager) {
     synchronized (managerItems) {
      ManagerItem item = (ManagerItem) managerItems.get(manager);
      if (item == null)
        item = new ManagerItem(table, manager);
      managerItems.put(manager, item);
      managers.put(item.getTableItem(), manager);
    }
  }

  public void downloadManagerRemoved(DownloadManager removed) {
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
    if (field.equals("name")) //$NON-NLS-1$
      return manager.getName();

    if (field.equals("ds")) //$NON-NLS-1$
      return DisplayFormatters.formatByteCountToKBEtcPerSec( manager.getStats().getDownloadAverage());

    if (field.equals("us")) //$NON-NLS-1$
      return DisplayFormatters.formatByteCountToKBEtcPerSec( manager.getStats().getUploadAverage());

    if (field.equals("eta")) //$NON-NLS-1$
      return manager.getStats().getETA();

    if (field.equals("tracker")) //$NON-NLS-1$
      return manager.getTrackerStatus();

    if (field.equals("priority")) //$NON-NLS-1$
      return manager.getName();

    return ""; //$NON-NLS-1$
  }

  private long getIntField(DownloadManager manager, String field) {

    if (field.equals("size")) //$NON-NLS-1$
      return manager.getSize();

    if (field.equals("done")) //$NON-NLS-1$
      return manager.getStats().getCompleted();

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

    return 0;
  }

  //Ordering
  private boolean ascending;
  private String lastField;
  private int type;
  private static final int INT = 1;
  private static final int STRING = 2;
  private int loopFactor;

  private void reOrder(boolean force) {
    if (!force && loopFactor++ < 20)
      return;
    loopFactor = 0;
    if (type == INT && lastField != null) {
      ascending = !ascending;
      orderInt(lastField);
    }
    if (type == STRING && lastField != null) {
      ascending = !ascending;
      orderString(lastField);
    }

  }

  private void orderInt(String field) {
    computeAscending(field);
    synchronized (managerItems) {
      List selected = getSelection();
      List ordered = new ArrayList(managerItems.size());
      ManagerItem items[] = new ManagerItem[managerItems.size()];
      Iterator iter = managerItems.keySet().iterator();
      while (iter.hasNext()) {
        DownloadManager manager = (DownloadManager) iter.next();
        ManagerItem item = (ManagerItem) managerItems.get(manager);
        int index = item.getIndex();
        items[index] = item;
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
        ordered.add(i, manager);
      }

      sort(items, ordered, selected);

    }
    refresh();
  }

  private List getSelection() {
    TableItem[] selection = table.getSelection();
    List selected = new ArrayList(selection.length);
    for (int i = 0; i < selection.length; i++) {
      DownloadManager manager = (DownloadManager) managers.get(selection[i]);
      if (manager != null)
        selected.add(manager);
    }
    return selected;
  }

  private void sort(ManagerItem[] items, List ordered, List selected) {
    for (int i = 0; i < ordered.size(); i++) {
      DownloadManager manager = (DownloadManager) ordered.get(i);

      items[i].setManager(manager);

      managerItems.put(manager, items[i]);
      managers.put(items[i].getTableItem(), manager);
      if (selected.contains(manager)) {
        table.select(i);
      }
      else {
        table.deselect(i);
      }
    }
  }

  private void computeAscending(String field) {
    if (lastField.equals(field))
      ascending = !ascending;
    else {
      lastField = field;
      ascending = true;
    }
  }

  private class IntColumnListener implements Listener {

    private String field;

    public IntColumnListener(String field) {
      this.field = field;
    }

    public void handleEvent(Event e) {
      type = INT;
      orderInt(field);
    }
  }

  private class StringColumnListener implements Listener {

    private String field;

    public StringColumnListener(String field) {
      this.field = field;
    }

    public void handleEvent(Event e) {
      type = STRING;
      orderString(field);
    }
  }

  private void orderString(String field) {
    computeAscending(field);
    synchronized (managerItems) {
      List selected = getSelection();
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
        ordered.add(i, manager);
      }

      sort(items, ordered, selected);
    }
    refresh();
  }

  private KeyListener createKeyListener() {
    return new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        if (0 == e.keyCode && 0x40000 == e.stateMask && 1 == e.character)
          table.selectAll(); // CTRL+a
      }
    };
  }
}
