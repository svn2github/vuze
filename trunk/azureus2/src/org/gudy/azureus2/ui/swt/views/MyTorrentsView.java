/*
 * Created on 30 juin 2003
 *
 */
package org.gudy.azureus2.ui.swt.views;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import org.gudy.azureus2.core3.config.impl.ConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.*;
import org.gudy.azureus2.core3.tracker.host.*;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.MainWindow;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.MinimizedWindow;
import org.gudy.azureus2.ui.swt.TrackerChangerWindow;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.TorrentRow;
import org.gudy.azureus2.ui.swt.views.utils.SortableTable;
import org.gudy.azureus2.ui.swt.views.utils.TableSorter;
import org.gudy.azureus2.ui.swt.exporttorrent.wizard.*;

/**
 * @author Olivier
 * 
 */
public class MyTorrentsView extends AbstractIView implements GlobalManagerListener, SortableTable {

  private GlobalManager globalManager;

  private Composite composite;
  private Composite panel;
  private Table table;
  private HashMap objectToSortableItem;
  private HashMap tableItemToObject;
  private Menu menu;

  private HashMap downloadBars;

  private TableSorter sorter;
  
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
    
    sorter = new TableSorter(this,"#",true);
    
    sorter.addStringColumnListener(table.getColumn(1),"name");
    sorter.addStringColumnListener(table.getColumn(10),"tracker");
    
    sorter.addIntColumnListener(table.getColumn(0),"#");
    sorter.addIntColumnListener(table.getColumn(2),"size");
    sorter.addIntColumnListener(table.getColumn(3),"done");
    sorter.addIntColumnListener(table.getColumn(4),"status");
    sorter.addIntColumnListener(table.getColumn(5),"seeds");
    sorter.addIntColumnListener(table.getColumn(6),"peers");
    sorter.addIntColumnListener(table.getColumn(7),"ds");
    sorter.addIntColumnListener(table.getColumn(8),"us");
    sorter.addIntColumnListener(table.getColumn(9),"eta");
    sorter.addIntColumnListener(table.getColumn(11),"priority");   

    table.setHeaderVisible(true);
    table.addKeyListener(createKeyListener());

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
    //itemChangeTracker.setImage(ImageRepository.getImage("stop"));
    
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
        TableItem[] tis = table.getSelection();
        final boolean initStoppedDownloads = true;
        for (int i = 0; i < tis.length; i++) {
          TableItem ti = tis[i];
          DownloadManager dm = (DownloadManager) tableItemToObject.get(ti);
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
    });

    itemRemove.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
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
		 	
			 TOTorrent	torrent = dm.getTorrent();
			 
			 if ( torrent != null ){
			 
							 	try{
			 	
					TRHostFactory.create().hostTorrent( torrent );
					
			 	}catch( TRHostException e ){
			 		
					MessageBox mb = new MessageBox(panel.getShell(),SWT.ICON_ERROR | SWT.OK );
		
					mb.setText(MessageText.getString("MyTorrentsView.menu.host.error.title"));
		
					mb.setMessage(	MessageText.getString("MyTorrentsView.menu.host.error.message")+"\n" +
									e.toString());
			
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
		 	
			 TOTorrent	torrent = dm.getTorrent();
			 
			 if ( torrent != null ){
			 
				try{
			 	
					TRHostFactory.create().publishTorrent( torrent );
					
				}catch( TRHostException e ){
			 		
					MessageBox mb = new MessageBox(panel.getShell(),SWT.ICON_ERROR | SWT.OK );
		
					mb.setText(MessageText.getString("MyTorrentsView.menu.host.error.title"));
		
					mb.setMessage(	MessageText.getString("MyTorrentsView.menu.host.error.message")+"\n" +
									e.toString());
			
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

    table.setMenu(menu);

    //toolBar.setSelection(itemAll);
    /*DropTarget dt = new DropTarget(table,DND.DROP_LINK);
    Transfer[] transfers = {FileTransfer.getInstance()};
    dt.setTransfer(transfers);*/

    globalManager.addListener(this);
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

  private KeyListener createKeyListener() {
    return new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        if (0 == e.keyCode && 0x40000 == e.stateMask && 1 == e.character)
          table.selectAll(); // CTRL+a
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

}
