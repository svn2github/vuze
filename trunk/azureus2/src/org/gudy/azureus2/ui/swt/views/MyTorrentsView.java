/*
 * Created on 30 juin 2003
 *
 */
package org.gudy.azureus2.ui.swt.views;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
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
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.ConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.tracker.host.TRHostException;
import org.gudy.azureus2.core3.tracker.host.TRHostFactory;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.MainWindow;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.MinimizedWindow;
import org.gudy.azureus2.ui.swt.TrackerChangerWindow;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.exporttorrent.wizard.ExportTorrentWizard;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.TorrentRow;
import org.gudy.azureus2.ui.swt.views.tableitems.utils.ConfigBasedItemEnumerator;
import org.gudy.azureus2.ui.swt.views.tableitems.utils.EnumeratorEditor;
import org.gudy.azureus2.ui.swt.views.tableitems.utils.ITableStructureModificationListener;
import org.gudy.azureus2.ui.swt.views.tableitems.utils.ItemDescriptor;
import org.gudy.azureus2.ui.swt.views.tableitems.utils.ItemEnumerator;
import org.gudy.azureus2.ui.swt.views.utils.SortableTable;
import org.gudy.azureus2.ui.swt.views.utils.TableSorter;

/**
 * @author Olivier
 * 
 */
public class MyTorrentsView extends AbstractIView implements GlobalManagerListener, SortableTable, ITableStructureModificationListener {

  private GlobalManager globalManager;

  private Composite composite;
  private Composite panel;
  private Table table;
  private HashMap objectToSortableItem;
  private HashMap tableItemToObject;
  private Menu menu;

  private HashMap downloadBars;

  private ItemEnumerator itemEnumerator;
  private TableSorter sorter;

  private String[] tableItems = {
       "#;I;25;0"
      ,"name;S;250;1"
      ,"size;I;70;2"
      ,"done;I;55;3"
      ,"status;I;80;4"
      ,"seeds;I;45;5"
      ,"peers;I;45;6"
      ,"downspeed;I;70;7"
      ,"upspeed;I;70;8"    
      ,"eta;I;70;9"
      ,"tracker;I;70;10"
      ,"priority;I;70;11"
      ,"shareRatio;I;70;-1"
      ,"down;I;70;-1"
      ,"up;I;70;-1"
  };
  
	// table item index, where the drag has started
  private int drag_drop_line_start;
  
  /**
   * @return Returns the itemEnumerator.
   */
  public ItemEnumerator getItemEnumerator() {
    return itemEnumerator;
  }

  public MyTorrentsView(GlobalManager globalManager) {
    this.globalManager = globalManager;
    objectToSortableItem = new HashMap();
    tableItemToObject = new HashMap();
    downloadBars = MainWindow.getWindow().getDownloadBars();
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#initialize(org.eclipse.swt.widgets.Composite)
   */
  public void initialize(Composite composite0) {
    if(panel != null) {      
      return;
    }
    createMainPanel(composite0);   
    createMenu();    
    createTable();    
    createDragDrop();
    globalManager.addListener(this);
  }
  
  
  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.views.tableitems.utils.ITableStructureModificationListener#tableStructureChanged()
   */
  public void tableStructureChanged() {
    //1. Unregister for item creation
    globalManager.removeListener(this);
    
    //2. Clear everything
    Iterator iter = objectToSortableItem.values().iterator();
    while(iter.hasNext()) {
      TorrentRow row = (TorrentRow) iter.next();
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
    createDragDrop();
    
    //5. Re-add as a listener
    globalManager.addListener(this);
    panel.layout();
  }
  

  private void createMainPanel(Composite composite0) {
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
  }

  private void createTable() {
    GridData gridData;
    table = new Table(panel, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER);
    gridData = new GridData(GridData.FILL_BOTH); 
    table.setLayoutData(gridData);
    sorter = new TableSorter(this,"#",true);
    
    ControlListener resizeListener = new ControlAdapter() {
      public void controlResized(ControlEvent e) {
        Utils.saveTableColumn((TableColumn) e.widget);
      }
    };
        
    itemEnumerator = ConfigBasedItemEnumerator.getInstance("MyTorrents",tableItems);
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
        Messages.setLanguageText(column, "MyTorrentsView." + items[i].getName());
        column.setWidth(items[i].getWidth());
        if (items[i].getType() == ItemDescriptor.TYPE_INT) {
          sorter.addIntColumnListener(column, items[i].getName());
        }
        if (items[i].getType() == ItemDescriptor.TYPE_STRING) {
          sorter.addStringColumnListener(column, items[i].getName());
        }
        column.setData("configName", "Table.MyTorrents." + items[i].getName());
        column.addControlListener(resizeListener);
      }
    }   

    table.setHeaderVisible(true);
    table.addKeyListener(createKeyListener());
    
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
        DownloadManager dm = (DownloadManager) tableItemToObject.get(ti);
        MainWindow.getWindow().openManagerView(dm);
      }
    });
    
    table.setMenu(menu);
  }

  private void createMenu() {
    menu = new Menu(composite.getShell(), SWT.POP_UP);

    final MenuItem itemDetails = new MenuItem(menu, SWT.PUSH);
    Messages.setLanguageText(itemDetails, "MyTorrentsView.menu.showdetails"); //$NON-NLS-1$
    menu.setDefaultItem(itemDetails);
    //itemDetails.setImage(ImageRepository.getImage("stop"));

    final MenuItem itemBar = new MenuItem(menu, SWT.CHECK);
    Messages.setLanguageText(itemBar, "MyTorrentsView.menu.showdownloadbar"); //$NON-NLS-1$
    itemBar.setImage(ImageRepository.getImage("downloadBar"));
    
    new MenuItem(menu, SWT.SEPARATOR);

  	final MenuItem itemOpen = new MenuItem(menu, SWT.PUSH);
  	Messages.setLanguageText(itemOpen, "MyTorrentsView.menu.open"); //$NON-NLS-1$
    //itemOpen.setImage(ImageRepository.getImage("stop"));
    
  	final MenuItem itemExport = new MenuItem(menu, SWT.PUSH);
  	Messages.setLanguageText(itemExport, "MyTorrentsView.menu.export"); //$NON-NLS-1$
    //itemExport.setImage(ImageRepository.getImage("stop"));
    
		final MenuItem itemHost = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(itemHost, "MyTorrentsView.menu.host"); //$NON-NLS-1$
		//itemHost.setImage(ImageRepository.getImage("stop"));
	
		final MenuItem itemPublish = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(itemPublish, "MyTorrentsView.menu.publish"); //$NON-NLS-1$
		//itemPublish.setImage(ImageRepository.getImage("stop"));
    
    new MenuItem(menu, SWT.SEPARATOR);

    final MenuItem itemMove = new MenuItem(menu, SWT.CASCADE);
    Messages.setLanguageText(itemMove, "MyTorrentsView.menu.move"); //$NON-NLS-1$
    //itemMove.setImage(ImageRepository.getImage("stop"));
    
    final Menu menuMove = new Menu(composite.getShell(), SWT.DROP_DOWN);
    itemMove.setMenu(menuMove);    
    
    final MenuItem itemMoveUp = new MenuItem(menuMove, SWT.PUSH);
    Messages.setLanguageText(itemMoveUp, "MyTorrentsView.menu.moveUp"); //$NON-NLS-1$    
    
    final MenuItem itemMoveDown = new MenuItem(menuMove, SWT.PUSH);
    Messages.setLanguageText(itemMoveDown, "MyTorrentsView.menu.moveDown"); //$NON-NLS-1$

    final MenuItem itemPriority = new MenuItem(menu, SWT.CASCADE);
    Messages.setLanguageText(itemPriority, "MyTorrentsView.menu.setpriority"); //$NON-NLS-1$
    //itemPriority.setImage(ImageRepository.getImage("stop"));
    
    final Menu menuPriority = new Menu(composite.getShell(), SWT.DROP_DOWN);
    itemPriority.setMenu(menuPriority);
    final MenuItem itemHigh = new MenuItem(menuPriority, SWT.PUSH);
    Messages.setLanguageText(itemHigh, "MyTorrentsView.menu.setpriority.high"); //$NON-NLS-1$
    final MenuItem itemLow = new MenuItem(menuPriority, SWT.PUSH);
    Messages.setLanguageText(itemLow, "MyTorrentsView.menu.setpriority.low"); //$NON-NLS-1$
    final MenuItem itemLockPriority = new MenuItem(menu, SWT.CHECK);
    Messages.setLanguageText(itemLockPriority, "MyTorrentsView.menu.lockpriority");
    itemLockPriority.setImage(ImageRepository.getImage("lock"));
    
    new MenuItem(menu, SWT.SEPARATOR);

    final MenuItem itemLockStartStop = new MenuItem(menu, SWT.CHECK);
    Messages.setLanguageText(itemLockStartStop, "MyTorrentsView.menu.lockstartstop");
    itemLockStartStop.setImage(ImageRepository.getImage("lock"));
    
    final MenuItem itemStart = new MenuItem(menu, SWT.PUSH);
    Messages.setLanguageText(itemStart, "MyTorrentsView.menu.start"); //$NON-NLS-1$
    itemStart.setImage(ImageRepository.getImage("start"));
    
    final MenuItem itemStop = new MenuItem(menu, SWT.PUSH);
    Messages.setLanguageText(itemStop, "MyTorrentsView.menu.stop"); //$NON-NLS-1$
    itemStop.setImage(ImageRepository.getImage("stop"));
    
    final MenuItem itemRemove = new MenuItem(menu, SWT.PUSH);
    Messages.setLanguageText(itemRemove, "MyTorrentsView.menu.remove"); //$NON-NLS-1$
    itemRemove.setImage(ImageRepository.getImage("delete"));
    
    final MenuItem itemRemoveAnd = new MenuItem(menu, SWT.CASCADE);
    Messages.setLanguageText(itemRemoveAnd, "MyTorrentsView.menu.removeand"); //$NON-NLS-1$
    itemRemoveAnd.setImage(ImageRepository.getImage("delete"));
    
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
    
    new MenuItem(menu, SWT.SEPARATOR);

    final MenuItem itemChangeTable = new MenuItem(menu, SWT.PUSH);
    Messages.setLanguageText(itemChangeTable, "MyTorrentsView.menu.editTableColumns"); //$NON-NLS-1$
    
    
    menu.addListener(SWT.Show, new Listener() {
      public void handleEvent(Event e) {
        TableItem[] tis = table.getSelection();

        itemDetails.setEnabled(false);
        itemBar.setEnabled(false);

        itemOpen.setEnabled(false);
        itemExport.setEnabled(false);
				itemHost.setEnabled(false);
				itemPublish.setEnabled(false);

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
          itemPublish.setEnabled(true);

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
            DownloadManager dm = (DownloadManager) tableItemToObject.get(ti);
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
        resumeSelectedTorrents();
      }
    });

    itemStop.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        stopSelectedTorrents();
      }
    });

    itemRemove.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        removeSelectedTorrentsIfStoppedOrError();
      }
    });

    itemDeleteTorrent.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        TableItem[] tis = table.getSelection();
        for (int i = 0; i < tis.length; i++) {
          TableItem ti = tis[i];
          DownloadManager dm = (DownloadManager) tableItemToObject.get(ti);
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
          DownloadManager dm = (DownloadManager) tableItemToObject.get(ti);
          if (dm != null
            && (dm.getState() == DownloadManager.STATE_STOPPED || dm.getState() == DownloadManager.STATE_ERROR)) {
            String path = dm.getFullName();
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
                FileUtil.recursiveDelete(f);
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
          DownloadManager dm = (DownloadManager) tableItemToObject.get(ti);
          if (dm != null
            && (dm.getState() == DownloadManager.STATE_STOPPED || dm.getState() == DownloadManager.STATE_ERROR)) {
            String path = dm.getFullName();
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
                FileUtil.recursiveDelete(f);
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
          DownloadManager dm = (DownloadManager) tableItemToObject.get(ti);
          if (dm != null && dm.getTrackerClient() != null) {
            new TrackerChangerWindow(MainWindow.getWindow().getDisplay(), dm.getTrackerClient());
          }
        }
      }
    });
    
    itemChangeTable.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
          new EnumeratorEditor(table.getDisplay(),ConfigBasedItemEnumerator.getInstance("MyTorrents",tableItems),MyTorrentsView.this,"MyTorrentsView");       
      }
    });

    itemDetails.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        TableItem[] tis = table.getSelection();
        for (int i = 0; i < tis.length; i++) {
          TableItem ti = tis[i];
          DownloadManager dm = (DownloadManager) tableItemToObject.get(ti);
          MainWindow.getWindow().openManagerView(dm);
        }
      }
    });



    itemOpen.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        TableItem[] tis = table.getSelection();
        if (tis.length == 0) {
          return;
        }
        TableItem ti = tis[0];
        DownloadManager dm = (DownloadManager) tableItemToObject.get(ti);
        Program.launch(dm.getFullName());
      }
    });

	itemExport.addListener(SWT.Selection, new Listener() {
	   public void handleEvent(Event event) {
		 TableItem[] tis = table.getSelection();
		 if (tis.length == 0) {
		   return;
		 }
		 TableItem ti = tis[0];
		 DownloadManager dm = (DownloadManager) tableItemToObject.get(ti);
		 
		 new ExportTorrentWizard(itemExport.getDisplay(), dm);
	   }
	 });

	  itemHost.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        TableItem[] tis = table.getSelection();
        for (int i = 0; i < tis.length; i++) {
          TableItem ti = tis[i];
          DownloadManager dm = (DownloadManager) tableItemToObject.get(ti);
          TOTorrent torrent = dm.getTorrent();
          if (torrent != null) {
            try {
              TRHostFactory.create().hostTorrent(torrent);
            } catch (TRHostException e) {
              MessageBox mb = new MessageBox(panel.getShell(), SWT.ICON_ERROR | SWT.OK);
              mb.setText(MessageText.getString("MyTorrentsView.menu.host.error.title"));
              mb.setMessage(MessageText.getString("MyTorrentsView.menu.host.error.message") + "\n" + e.toString());
              mb.open();
            }
          }
        }
        MainWindow.getWindow().showMyTracker();
      }
    });
	 
	  itemPublish.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        TableItem[] tis = table.getSelection();
        for (int i = 0; i < tis.length; i++) {
          TableItem ti = tis[i];
          DownloadManager dm = (DownloadManager) tableItemToObject.get(ti);
          TOTorrent torrent = dm.getTorrent();
          if (torrent != null) {
            try {
              TRHostFactory.create().publishTorrent(torrent);
            } catch (TRHostException e) {
              MessageBox mb = new MessageBox(panel.getShell(), SWT.ICON_ERROR | SWT.OK);
              mb.setText(MessageText.getString("MyTorrentsView.menu.host.error.title"));
              mb.setMessage(MessageText.getString("MyTorrentsView.menu.host.error.message") + "\n" + e.toString());
              mb.open();
            }
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
          DownloadManager dm = (DownloadManager) tableItemToObject.get(ti);
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
          DownloadManager dm = (DownloadManager) tableItemToObject.get(ti);
          if (dm != null && dm.isMoveableDown()) {
            dm.moveDown();

          }
        }
        if (sorter.getLastField().equals("#"))
          sorter.reOrder(true);
      }
    });

    itemMoveUp.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        TableItem[] tis = table.getSelection();
        for (int i = 0; i < tis.length; i++) {
          TableItem ti = tis[i];
          DownloadManager dm = (DownloadManager) tableItemToObject.get(ti);
          if (dm != null && dm.isMoveableUp()) {
            dm.moveUp();
          }
        }
        if (sorter.getLastField().equals("#"))
          sorter.reOrder(true);
      }
    });

    itemHigh.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        TableItem[] tis = table.getSelection();
        for (int i = 0; i < tis.length; i++) {
          TableItem ti = tis[i];
          DownloadManager dm = (DownloadManager) tableItemToObject.get(ti);
          dm.setPriority(DownloadManager.HIGH_PRIORITY);
        }
      }
    });

    itemLow.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        TableItem[] tis = table.getSelection();
        for (int i = 0; i < tis.length; i++) {
          TableItem ti = tis[i];
          DownloadManager dm = (DownloadManager) tableItemToObject.get(ti);
          dm.setPriority(DownloadManager.LOW_PRIORITY);
        }
      }
    });
    
    itemLockPriority.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        TableItem[] tis = table.getSelection();
        for (int i = 0; i < tis.length; i++) {
          TableItem ti = tis[i];
          DownloadManager dm = (DownloadManager) tableItemToObject.get(ti);
          dm.setPriorityLocked(itemLockPriority.getSelection());
        }
      }
    });
    
    itemLockStartStop.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        TableItem[] tis = table.getSelection();
        for (int i = 0; i < tis.length; i++) {
          TableItem ti = tis[i];
          DownloadManager dm = (DownloadManager) tableItemToObject.get(ti);
          dm.setStartStopLocked(itemLockStartStop.getSelection());
        }
      }
    });
  }

  private void createDragDrop() {
    Transfer[] types = new Transfer[] { TextTransfer.getInstance()};

    DragSource dragSource = new DragSource(table, DND.DROP_MOVE);
    dragSource.setTransfer(types);
    dragSource.addDragListener(new DragSourceAdapter() {
      public void dragStart(DragSourceEvent event) {
        if (table.getSelectionCount() != 0 && table.getSelectionCount() != table.getItemCount()) {
          event.doit = true;
          drag_drop_line_start = table.getSelectionIndex();
         } else {
          event.doit = false;
        }
      }
    });

    DropTarget dropTarget = new DropTarget(table, DND.DROP_MOVE | DND.DROP_COPY);
    dropTarget.setTransfer(new Transfer[] { FileTransfer.getInstance(), TextTransfer.getInstance()});
    dropTarget.addDropListener(new DropTargetAdapter() {
      public void dragOver(DropTargetEvent event) {
        if(TextTransfer.getInstance().isSupportedType(event.currentDataType)) {
          event.feedback = DND.FEEDBACK_EXPAND | DND.FEEDBACK_SCROLL | DND.FEEDBACK_SELECT | DND.FEEDBACK_INSERT_BEFORE | DND.FEEDBACK_INSERT_AFTER;
          event.detail = event.item == null ? DND.DROP_NONE : DND.DROP_MOVE;
        }
      }
      public void drop(DropTargetEvent event) {
        // Torrent file from shell dropped
        if(event.data instanceof String[]) {
          MainWindow.getWindow().openDroppedTorrents(event);
          return;
        }
        event.detail = DND.DROP_NONE;
        if(event.item == null)
          return;
        int drag_drop_line_end = table.indexOf((TableItem)event.item);
        moveSelectedTorrents(drag_drop_line_start, drag_drop_line_end);
      }
    });
  }

  private void moveSelectedTorrents(int drag_drop_line_start, int drag_drop_line_end) {
    if (drag_drop_line_end == drag_drop_line_start)
      return;

    TableItem[] tis = table.getSelection();
    List list = Arrays.asList(tis);
    final boolean moveDown = drag_drop_line_end > drag_drop_line_start;
    DownloadManager dm = (DownloadManager) tableItemToObject.get(tis[moveDown ? tis.length - 1 : 0]);
    int lastIndex = dm.getIndex();
    if (moveDown) {
      Collections.reverse(list);
      lastIndex += drag_drop_line_end - drag_drop_line_start + 1;
    } else {
      lastIndex -= drag_drop_line_start - drag_drop_line_end + 1;
    }
    for (Iterator iter = list.iterator(); iter.hasNext();) {
      TableItem ti = (TableItem) iter.next();
      dm = (DownloadManager) tableItemToObject.get(ti);
      if (dm != null) {
        if (!moveDown) {
          for (int j = drag_drop_line_start - drag_drop_line_end; j > 0; j--) {
            if (dm.isMoveableUp() && dm.getIndex() > lastIndex + 1)
              dm.moveUp();
          }
        } else {
          for (int j = drag_drop_line_end - drag_drop_line_start; j > 0; j--) {
            if (dm.isMoveableDown() && dm.getIndex() < lastIndex - 1)
              dm.moveDown();
          }
        }
        lastIndex = dm.getIndex();
      }
    }
    sorter.orderField("#", true);
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

    sorter.reOrder(false);

    Iterator iter = objectToSortableItem.keySet().iterator();
    while (iter.hasNext()) {
      if (this.panel.isDisposed())
        return;
      DownloadManager manager = (DownloadManager) iter.next();
      TorrentRow item = (TorrentRow) objectToSortableItem.get(manager);
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
    ConfigurationManager.getInstance().removeParameterListener("ReOrder Delay", sorter);
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getFullTitle()
   */
  public String getFullTitle() {
    return MessageText.getString("MyTorrentsView.mytorrents");
  }

   public void downloadManagerAdded(DownloadManager manager) {
     synchronized (objectToSortableItem) {
      TorrentRow item = (TorrentRow) objectToSortableItem.get(manager);
      if (item == null)
        item = new TorrentRow(this,table, manager);
        objectToSortableItem.put(manager, item);      
    }
  }

  public void downloadManagerRemoved(DownloadManager removed) {
    MinimizedWindow mw = (MinimizedWindow) downloadBars.remove(removed);
    if (mw != null) {
      mw.close();
    }

    TorrentRow managerItem = (TorrentRow) objectToSortableItem.remove(removed);
    if (managerItem != null) {
      TableItem tableItem = managerItem.getTableItem();
      tableItemToObject.remove(tableItem);
      managerItem.delete();
    }
  }

  
/*
  private List getSelection() {
    TableItem[] selection = table.getSelection();
    List selected = new ArrayList(selection.length);
    for (int i = 0; i < selection.length; i++) {
      DownloadManager manager = (DownloadManager) tableItemToObject.get(selection[i]);
      if (manager != null)
        selected.add(manager);
    }
    return selected;
  }
*/

  private KeyListener createKeyListener() {
    return new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
/*
        String string = "stateMask=0x" + Integer.toHexString(e.stateMask);
        if ((e.stateMask & SWT.CTRL) != 0)
          string += " CTRL";
        if ((e.stateMask & SWT.ALT) != 0)
          string += " ALT";
        if ((e.stateMask & SWT.SHIFT) != 0)
          string += " SHIFT";
        if ((e.stateMask & SWT.COMMAND) != 0)
          string += " COMMAND";
        string += ", keyCode=0x" + Integer.toHexString(e.keyCode) + "=" + e.keyCode;
        string += ", character=0x" + Integer.toHexString(e.character);
        switch (e.character) {
          case 0 :
            string += " '\\0'";
            break;
          case SWT.BS :
            string += " '\\b'";
            break;
          case SWT.CR :
            string += " '\\r'";
            break;
          case SWT.DEL :
            string += " DEL";
            break;
          case SWT.ESC :
            string += " ESC";
            break;
          case SWT.LF :
            string += " '\\n'";
            break;
          case SWT.TAB :
            string += " '\\t'";
            break;
          default :
            string += " '" + e.character + "'";
            break;
        }
        System.out.println(string);
//*/
        if (e.stateMask == (SWT.CTRL|SWT.SHIFT)) {
          // CTRL+SHIFT+S stop all Torrents
          if(e.character == 0x13)
            globalManager.stopAllDownloads();
        } else if (e.stateMask == SWT.CTRL) {
          // CTRL+CURSOR DOWN move selected Torrents one down
          if(e.keyCode == 0x1000001)
            moveSelectedTorrents(1, 0);
          // CTRL+CURSOR UP move selected Torrents one up
          else if(e.keyCode == 0x1000002)
            moveSelectedTorrents(0, 1);
          // CTRL+HOME move selected Torrents to top
          else if(e.keyCode == 0x1000007)
            moveSelectedTorrents(table.getItemCount()-1, 0);
          // CTRL+END move selected Torrents to end
          else if(e.keyCode == 0x1000008)
            moveSelectedTorrents(0, table.getItemCount()-1);
          // CTRL+A select all Torrents
          else if(e.character == 0x1)
            table.selectAll();
          // CTRL+R resume/start selected Torrents
          else if(e.character == 0x12)
            resumeSelectedTorrents();
          // CTRL+S stop selected Torrents
          else if(e.character == 0x13)
            stopSelectedTorrents();
        } else if(e.stateMask == 0) {
          // DEL remove selected Torrents
          if(e.keyCode == 127) {
            removeSelectedTorrentsIfStoppedOrError();
          }
        }
      }
    };
  }
  
  public void setItem(TableItem item,DownloadManager manager) {
    tableItemToObject.put(item,manager);
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

  private void removeSelectedTorrentsIfStoppedOrError() {
    TableItem[] tis = table.getSelection();
    for (int i = 0; i < tis.length; i++) {
      TableItem ti = tis[i];
      DownloadManager dm = (DownloadManager) tableItemToObject.get(ti);
      if (dm != null
          && (dm.getState() == DownloadManager.STATE_STOPPED || dm.getState() == DownloadManager.STATE_ERROR)) {
        globalManager.removeDownloadManager(dm);
      }
    }
  }

  private void stopSelectedTorrents() {
    TableItem[] tis = table.getSelection();
    for (int i = 0; i < tis.length; i++) {
      TableItem ti = tis[i];
      DownloadManager dm = (DownloadManager) tableItemToObject.get(ti);
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

  private void resumeSelectedTorrents() {
    TableItem[] tis = table.getSelection();
    for (int i = 0; i < tis.length; i++) {
      TableItem ti = tis[i];
      DownloadManager dm = (DownloadManager) tableItemToObject.get(ti);
      if (dm != null) {
        dm.setState(DownloadManager.STATE_WAITING);
      }
    }
  }
  
}
