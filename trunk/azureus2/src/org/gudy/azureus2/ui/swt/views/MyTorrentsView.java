/*
 * Created on 30 juin 2003
 *
 * Copyright (C) 2004 Aelitis SARL, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * AELITIS, SARL au capital de 30,000 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

package org.gudy.azureus2.ui.swt.views;

import java.util.*;
import org.eclipse.swt.SWT;

import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.*;

import com.aelitis.azureus.core.*;

import org.gudy.azureus2.core3.category.Category;
import org.gudy.azureus2.core3.category.CategoryListener;
import org.gudy.azureus2.core3.category.CategoryManager;
import org.gudy.azureus2.core3.category.CategoryManagerListener;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerDownloadRemovalVetoException;
import org.gudy.azureus2.core3.global.GlobalManagerListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.tracker.client.TRTrackerClient;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.ui.swt.views.table.TableColumnCore;
import org.gudy.azureus2.ui.swt.views.table.TableRowCore;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.exporttorrent.wizard.ExportTorrentWizard;
import org.gudy.azureus2.ui.swt.help.HealthHelpWindow;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;
import org.gudy.azureus2.ui.swt.maketorrent.MultiTrackerEditor;
import org.gudy.azureus2.ui.swt.maketorrent.TrackerEditorListener;

/** Displays a list of torrents in a table view.
 *
 * @author Olivier
 * @author TuxPaper
 *         2004/Apr/18: Use TableRowImpl instead of PeerRow
 *         2004/Apr/20: Remove need for tableItemToObject
 *         2004/Apr/21: extends TableView instead of IAbstractView
 */
public class MyTorrentsView
       extends TableView
       implements GlobalManagerListener,
                  ParameterListener,
                  DownloadManagerListener,
                  CategoryManagerListener,
                  CategoryListener
{
	private AzureusCore		azureus_core;

  private GlobalManager globalManager;
  private boolean isSeedingView;

  private Composite cTablePanel;
  private Font fontButton = null;
  private Composite cCategories;
  private Menu menuCategory;
  private MenuItem menuItemChangeDir = null;

  private Map downloadBars;

  private Category currentCategory;
  private boolean skipDMAdding = true;

  // table item index, where the drag has started
  private int drag_drop_line_start = -1;

  private boolean confirmDataDelete = COConfigurationManager.getBooleanParameter("Confirm Data Delete", true);

  public 
  MyTorrentsView(
  		AzureusCore			_azureus_core, 
		boolean 			isSeedingView,
        TableColumnCore[] 	basicItems) 
  {
    super((isSeedingView) ? TableManager.TABLE_MYTORRENTS_COMPLETE
                          : TableManager.TABLE_MYTORRENTS_INCOMPLETE,
          "MyTorrentsView", basicItems, "#", 
          SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER);
    ptIconSize = new Point(16, 16);
    azureus_core		= _azureus_core;
    this.globalManager 	= azureus_core.getGlobalManager();
    this.isSeedingView 	= isSeedingView;

    downloadBars = MainWindow.getWindow().getDownloadBars();
    currentCategory = CategoryManager.getCategory(Category.TYPE_ALL);
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#initialize(org.eclipse.swt.widgets.Composite)
   */
  public void initialize(Composite composite0) {
    if(cTablePanel != null) {
      return;
    }

    super.initialize(composite0);

    createTabs();

    createDragDrop();

    COConfigurationManager.addParameterListener("Confirm Data Delete", this);

    activateCategory(currentCategory);
    CategoryManager.addCategoryManagerListener(this);
    // globalManager.addListener sends downloadManagerAdded()'s when you addListener
    // we don't need them..
    skipDMAdding = true;
    globalManager.addListener(this);
    skipDMAdding = false;
  }


  public void tableStructureChanged() {
    super.tableStructureChanged();

    createDragDrop();
    activateCategory(currentCategory);
  }

  public Composite createMainPanel(Composite composite) {
    GridData gridData;
    Composite panel = new Composite(composite, SWT.NULL);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    layout.horizontalSpacing = 0;
    layout.verticalSpacing = 0;
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    panel.setLayout(layout);
    panel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    cTablePanel = new Composite(panel, SWT.NULL);
    gridData = new GridData(GridData.FILL_BOTH);
    gridData.horizontalSpan = 2;
    cTablePanel.setLayoutData(gridData);

    layout = new GridLayout(1, false);
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.verticalSpacing = 0;
    layout.horizontalSpacing = 0;
    cTablePanel.setLayout(layout);

    return panel;
  }

  private void createTabs() {
    GridData gridData;
    Category[] categories = CategoryManager.getCategories();
    Arrays.sort(categories);

    if (categories.length > 0) {
      if (cCategories == null) {
        cCategories = new Composite(getComposite(), SWT.NULL);
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
        cCategories.setLayoutData(gridData);
        RowLayout rowLayout = new RowLayout();
        rowLayout.marginTop = 0;
        rowLayout.marginBottom = 0;
        rowLayout.marginLeft = 0;
        rowLayout.marginRight = 0;
        rowLayout.spacing = 0;
        rowLayout.wrap = true;
        cCategories.setLayout(rowLayout);

        Label l = new Label(getComposite(), SWT.WRAP);
        gridData = new GridData();
        gridData.horizontalIndent = 3;
        l.setLayoutData(gridData);
        Messages.setLanguageText(l, sTableID + "View.header");
        cCategories.moveAbove(null);
        l.moveAbove(null);
      } else {
        Control[] controls = cCategories.getChildren();
        for (int i = 0; i < controls.length; i++) {
          controls[i].dispose();
        }
      }



      int iFontPixelsHeight = 11;
      int iFontPointHeight = (iFontPixelsHeight * 72) / cCategories.getDisplay().getDPI().y;
      for (int i = 0; i < categories.length; i++) {
        final Button catButton = new Button(cCategories, SWT.TOGGLE);
        if (i == 0 && fontButton == null) {
          Font f = catButton.getFont();
          FontData fd = f.getFontData()[0];
          fd.setHeight(iFontPointHeight);
          fontButton = new Font(cCategories.getDisplay(), fd);
        }
        catButton.setFont(fontButton);
        catButton.pack(true);
        if (catButton.getSize().y > 0) {
          RowData rd = new RowData();
          rd.height = catButton.getSize().y - 3 + catButton.getBorderWidth() * 2;
//          Point pt = catButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
//          rd.height = pt.y;
          catButton.setLayoutData(rd);
        }

        String name = categories[i].getName();
        if (categories[i].getType() == Category.TYPE_USER)
          catButton.setText(name);
        else
          Messages.setLanguageText(catButton, name);

        catButton.setData("Category", categories[i]);
        if (categories[i] == currentCategory) {
          catButton.setSelection(true);
        }

        catButton.addSelectionListener(new SelectionAdapter() {
          public void widgetSelected(SelectionEvent e) {
            Button curButton = (Button)e.widget;
            boolean isEnabled = curButton.getSelection();
            Control[] controls = cCategories.getChildren();
            if (!isEnabled)
              curButton = (Button)controls[0];

            for (int i = 0; i < controls.length; i++) {
              Button b = (Button)controls[i];
              if (b != curButton && b.getSelection())
                b.setSelection(false);
              else if (b == curButton && !b.getSelection())
                b.setSelection(true);
            }
            activateCategory( (Category)curButton.getData("Category") );
          }
        });

        DropTarget tabDropTarget = new DropTarget(catButton, DND.DROP_DEFAULT | DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK);
        Transfer[] types = new Transfer[] { TextTransfer.getInstance()};
        tabDropTarget.setTransfer(types);
        tabDropTarget.addDropListener(new DropTargetAdapter() {
          public void dragOver(DropTargetEvent e) {
            if(drag_drop_line_start >= 0)
              e.detail = DND.DROP_MOVE;
            else
              e.detail = DND.DROP_NONE;
          }

          public void drop(DropTargetEvent e) {
            e.detail = DND.DROP_NONE;
            if(drag_drop_line_start >= 0) {
              drag_drop_line_start = -1;

              assignSelectedToCategory((Category)catButton.getData("Category"));
            }
          }
        });

        if (categories[i].getType() == Category.TYPE_USER) {
          Menu menu = new Menu(getComposite().getShell(), SWT.POP_UP);

          final MenuItem itemDelete = new MenuItem(menu, SWT.PUSH);
          Messages.setLanguageText(itemDelete, "MyTorrentsView.menu.category.delete");
          menu.setDefaultItem(itemDelete);

          itemDelete.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event) {
              Category catToDelete = (Category)catButton.getData("Category");
              if (catToDelete != null) {
                java.util.List managers = catToDelete.getDownloadManagers();
                // move to array,since setcategory removed it from the category,
                // which would mess up our loop
                DownloadManager dms[] = (DownloadManager [])managers.toArray(new DownloadManager[managers.size()]);
                for (int i = 0; i < dms.length; i++) {
                  dms[i].setCategory(null);
                }
                if (currentCategory == catToDelete)
                   activateCategory(CategoryManager.getCategory(Category.TYPE_ALL));
                CategoryManager.removeCategory(catToDelete);
              }
            }
          });
          catButton.setMenu(menu);
        }
      }
      cCategories.layout();
      getComposite().layout();
    }
  }

  public Table createTable() {
    bSkipFirstColumn = true;
    Table table = new Table(cTablePanel, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER);
    table.setLayoutData(new GridData(GridData.FILL_BOTH));

    table.addKeyListener(createKeyListener());

    table.addSelectionListener(new SelectionAdapter() {
      public void widgetDefaultSelected(SelectionEvent e) {
        DownloadManager dm = (DownloadManager)getFirstSelectedDataSource();
        if (dm != null)
          MainWindow.getWindow().openManagerView(dm);
      }
    });
    
    cTablePanel.layout();
    return table;
  }

  public void fillMenu(final Menu menu) {
    final MenuItem itemDetails = new MenuItem(menu, SWT.PUSH);
    Messages.setLanguageText(itemDetails, "MyTorrentsView.menu.showdetails"); //$NON-NLS-1$
    menu.setDefaultItem(itemDetails);
    itemDetails.setImage(ImageRepository.getImage("details"));

    final MenuItem itemBar = new MenuItem(menu, SWT.CHECK);
    Messages.setLanguageText(itemBar, "MyTorrentsView.menu.showdownloadbar"); //$NON-NLS-1$
    itemBar.setImage(ImageRepository.getImage("downloadBar"));

    new MenuItem(menu, SWT.SEPARATOR);

    final MenuItem itemOpen = new MenuItem(menu, SWT.PUSH);
    Messages.setLanguageText(itemOpen, "MyTorrentsView.menu.open"); //$NON-NLS-1$
    itemOpen.setImage(ImageRepository.getImage("run"));

    final MenuItem itemExport = new MenuItem(menu, SWT.PUSH);
    Messages.setLanguageText(itemExport, "MyTorrentsView.menu.export"); //$NON-NLS-1$
    itemExport.setImage(ImageRepository.getImage("export"));

    final MenuItem itemHost = new MenuItem(menu, SWT.PUSH);
    Messages.setLanguageText(itemHost, "MyTorrentsView.menu.host"); //$NON-NLS-1$
    itemHost.setImage(ImageRepository.getImage("host"));

    final MenuItem itemPublish = new MenuItem(menu, SWT.PUSH);
    Messages.setLanguageText(itemPublish, "MyTorrentsView.menu.publish"); //$NON-NLS-1$
    itemPublish.setImage(ImageRepository.getImage("publish"));

    new MenuItem(menu, SWT.SEPARATOR);

    final MenuItem itemMove = new MenuItem(menu, SWT.CASCADE);
    Messages.setLanguageText(itemMove, "MyTorrentsView.menu.move"); //$NON-NLS-1$
    itemMove.setImage(ImageRepository.getImage("move"));

    final Menu menuMove = new Menu(getComposite().getShell(), SWT.DROP_DOWN);
    itemMove.setMenu(menuMove);

    final MenuItem itemMoveTop = new MenuItem(menuMove, SWT.PUSH);
    Messages.setLanguageText(itemMoveTop, "MyTorrentsView.menu.moveTop"); //$NON-NLS-1$
    itemMoveTop.setImage(ImageRepository.getImage("top"));

    final MenuItem itemMoveUp = new MenuItem(menuMove, SWT.PUSH);
    Messages.setLanguageText(itemMoveUp, "MyTorrentsView.menu.moveUp"); //$NON-NLS-1$
    itemMoveUp.setImage(ImageRepository.getImage("up"));

    final MenuItem itemMoveDown = new MenuItem(menuMove, SWT.PUSH);
    Messages.setLanguageText(itemMoveDown, "MyTorrentsView.menu.moveDown"); //$NON-NLS-1$
    itemMoveDown.setImage(ImageRepository.getImage("down"));

    final MenuItem itemMoveEnd = new MenuItem(menuMove, SWT.PUSH);
    Messages.setLanguageText(itemMoveEnd, "MyTorrentsView.menu.moveEnd"); //$NON-NLS-1$
    itemMoveEnd.setImage(ImageRepository.getImage("bottom"));

    final MenuItem itemPriority = new MenuItem(menu, SWT.CASCADE);
    Messages.setLanguageText(itemPriority, "MyTorrentsView.menu.setpriority"); //$NON-NLS-1$
    itemPriority.setImage(ImageRepository.getImage("speed"));

    final Menu menuPriority = new Menu(getComposite().getShell(), SWT.DROP_DOWN);
    itemPriority.setMenu(menuPriority);

    final MenuItem itemHigh = new MenuItem(menuPriority, SWT.PUSH);
    Messages.setLanguageText(itemHigh, "MyTorrentsView.menu.setpriority.high"); //$NON-NLS-1$
    final MenuItem itemLow = new MenuItem(menuPriority, SWT.PUSH);
    Messages.setLanguageText(itemLow, "MyTorrentsView.menu.setpriority.low"); //$NON-NLS-1$

    // Category

    menuCategory = new Menu(getComposite().getShell(), SWT.DROP_DOWN);
    final MenuItem itemCategory = new MenuItem(menu, SWT.CASCADE);
    Messages.setLanguageText(itemCategory, "MyTorrentsView.menu.setCategory"); //$NON-NLS-1$
    //itemCategory.setImage(ImageRepository.getImage("speed"));
    itemCategory.setMenu(menuCategory);

    addCategorySubMenu();

    // Tracker
    final Menu menuTracker = new Menu(getComposite().getShell(), SWT.DROP_DOWN);
    final MenuItem itemTracker = new MenuItem(menu, SWT.CASCADE);
    Messages.setLanguageText(itemTracker, "MyTorrentsView.menu.tracker");
    itemTracker.setMenu(menuTracker);

    final MenuItem itemChangeTracker = new MenuItem(menuTracker, SWT.PUSH);
    Messages.setLanguageText(itemChangeTracker, "MyTorrentsView.menu.changeTracker"); //$NON-NLS-1$
    itemChangeTracker.setImage(ImageRepository.getImage("add_tracker"));

    final MenuItem itemEditTracker = new MenuItem(menuTracker, SWT.PUSH);
    Messages.setLanguageText(itemEditTracker, "MyTorrentsView.menu.editTracker"); //$NON-NLS-1$
    itemEditTracker.setImage(ImageRepository.getImage("edit_trackers"));

    final MenuItem itemManualUpdate = new MenuItem(menuTracker,SWT.PUSH);
    Messages.setLanguageText(itemManualUpdate, "GeneralView.label.trackerurlupdate"); //$NON-NLS-1$
    //itemManualUpdate.setImage(ImageRepository.getImage("edit_trackers"));

    new MenuItem(menu, SWT.SEPARATOR);

    final MenuItem itemQueue = new MenuItem(menu, SWT.PUSH);
    Messages.setLanguageText(itemQueue, "MyTorrentsView.menu.queue"); //$NON-NLS-1$
    itemQueue.setImage(ImageRepository.getImage("start"));

    final MenuItem itemForceStart = new MenuItem(menu, SWT.CHECK);
    Messages.setLanguageText(itemForceStart, "MyTorrentsView.menu.forceStart");
    itemForceStart.setImage(ImageRepository.getImage("forcestart"));

    final MenuItem itemStop = new MenuItem(menu, SWT.PUSH);
    Messages.setLanguageText(itemStop, "MyTorrentsView.menu.stop"); //$NON-NLS-1$
    itemStop.setImage(ImageRepository.getImage("stop"));

    final MenuItem itemRemove = new MenuItem(menu, SWT.PUSH);
    Messages.setLanguageText(itemRemove, "MyTorrentsView.menu.remove"); //$NON-NLS-1$
    itemRemove.setImage(ImageRepository.getImage("delete"));

    final MenuItem itemRemoveAnd = new MenuItem(menu, SWT.CASCADE);
    Messages.setLanguageText(itemRemoveAnd, "MyTorrentsView.menu.removeand"); //$NON-NLS-1$
    itemRemoveAnd.setImage(ImageRepository.getImage("delete"));

    final Menu menuRemove = new Menu(getComposite().getShell(), SWT.DROP_DOWN);
    itemRemoveAnd.setMenu(menuRemove);
    final MenuItem itemDeleteTorrent = new MenuItem(menuRemove, SWT.PUSH);
    Messages.setLanguageText(itemDeleteTorrent, "MyTorrentsView.menu.removeand.deletetorrent"); //$NON-NLS-1$
    final MenuItem itemDeleteData = new MenuItem(menuRemove, SWT.PUSH);
    Messages.setLanguageText(itemDeleteData, "MyTorrentsView.menu.removeand.deletedata");
    final MenuItem itemDeleteBoth = new MenuItem(menuRemove, SWT.PUSH);
    Messages.setLanguageText(itemDeleteBoth, "MyTorrentsView.menu.removeand.deleteboth");

    final MenuItem itemRecheck = new MenuItem(menu, SWT.PUSH);
    Messages.setLanguageText(itemRecheck, "MyTorrentsView.menu.recheck");
    itemRecheck.setImage(ImageRepository.getImage("recheck"));

    new MenuItem(menu, SWT.SEPARATOR);

    super.fillMenu(menu);

    menu.addListener(SWT.Show, new Listener() {
      public void handleEvent(Event e) {
        Object[] dms = getSelectedDataSources();
        boolean hasSelection = (dms.length > 0);

        itemDetails.setEnabled(hasSelection);

        itemOpen.setEnabled(hasSelection);
        itemExport.setEnabled(hasSelection);
        itemHost.setEnabled(hasSelection);
        itemPublish.setEnabled(hasSelection);

        itemMove.setEnabled(hasSelection);
        itemPriority.setEnabled(hasSelection);
        itemBar.setEnabled(hasSelection);

        itemManualUpdate.setEnabled(hasSelection);

        boolean bChangeDir = false;
        if (hasSelection) {
          bChangeDir = true;
          boolean moveUp, moveDown, start, stop, remove, changeUrl, barsOpened,
                  forceStart, forceStartEnabled, recheck, manualUpdate;
          moveUp = moveDown = start = stop = remove = changeUrl = barsOpened =
                   forceStart = forceStartEnabled = recheck = manualUpdate = true;
          for (int i = 0; i < dms.length; i++) {
            DownloadManager dm = (DownloadManager)dms[i];
            if (dm.getTrackerClient() == null)
              changeUrl = false;
            if (!downloadBars.containsKey(dm))
              barsOpened = false;

            int state = dm.getState();
            stop = stop && ManagerUtils.isStopable(dm);
            remove = remove && ManagerUtils.isRemoveable(dm);
            start = start && ManagerUtils.isStartable(dm);

            if (state != DownloadManager.STATE_STOPPED)
              start = false;

            if (!dm.canForceRecheck())
              recheck = false;

            if (!dm.isMoveableDown())
              moveDown = false;
            if (!dm.isMoveableUp())
              moveUp = false;

            if (state != DownloadManager.STATE_STOPPED && state != DownloadManager.STATE_QUEUED &&
                state != DownloadManager.STATE_SEEDING && state != DownloadManager.STATE_DOWNLOADING)
              forceStartEnabled = false;

            if (!dm.isForceStart())
              forceStart = false;

            TRTrackerClient trackerClient = dm.getTrackerClient();
            if(trackerClient != null) {
              boolean update_state = ((SystemTime.getCurrentTime()/1000 - trackerClient.getLastUpdateTime() >= TRTrackerClient.REFRESH_MINIMUM_SECS ));
              manualUpdate = manualUpdate & update_state;
            }

            bChangeDir &= (state == DownloadManager.STATE_ERROR && !dm.filesExist());
          }
          itemBar.setSelection(barsOpened);

          itemMoveTop.setEnabled(moveUp);
          itemMoveEnd.setEnabled(moveDown);

          itemForceStart.setSelection(forceStart);
          itemForceStart.setEnabled(forceStartEnabled);
          itemQueue.setEnabled(start);
          itemStop.setEnabled(stop);
          itemRemove.setEnabled(remove);
          itemRemoveAnd.setEnabled(remove);

          itemEditTracker.setEnabled(true);
          itemChangeTracker.setEnabled(changeUrl);
          itemRecheck.setEnabled(recheck);

          itemManualUpdate.setEnabled(manualUpdate);

        } else {
          itemBar.setSelection(false);

          itemForceStart.setEnabled(false);
          itemForceStart.setSelection(false);
          itemQueue.setEnabled(false);
          itemStop.setEnabled(false);
          itemRemove.setEnabled(false);
          itemRemoveAnd.setEnabled(false);

          itemEditTracker.setEnabled(false);
          itemChangeTracker.setEnabled(false);
          itemRecheck.setEnabled(false);
        }

        if (menuItemChangeDir != null && !menuItemChangeDir.isDisposed()) {
          menuItemChangeDir.dispose();
        }
        if (bChangeDir) {
          menuItemChangeDir = new MenuItem(menu, SWT.PUSH, 0);
          Messages.setLanguageText(menuItemChangeDir, "MyTorrentsView.menu.changeDirectory");
          menuItemChangeDir.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
              changeDirSelectedTorrents();
            }
          });
        }
      }
    });

    itemQueue.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        queueSelectedTorrents();
      }
    });

    itemStop.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        stopSelectedTorrents();
      }
    });

    itemRemove.addListener(SWT.Selection,
                           new SelectedTableRowsListener() {
      public void run(TableRowCore row) {
        removeTorrent((DownloadManager)row.getDataSource(true), false, false);
      }
    });

    itemDeleteTorrent.addListener(SWT.Selection,
                                  new SelectedTableRowsListener() {
      public void run(TableRowCore row) {
        removeTorrent((DownloadManager)row.getDataSource(true), true, false);
      }
    });

    itemDeleteData.addListener(SWT.Selection,
                               new SelectedTableRowsListener() {
      public void run(TableRowCore row) {
        removeTorrent((DownloadManager)row.getDataSource(true), false, true);
      }
    });

    itemDeleteBoth.addListener(SWT.Selection,
                               new SelectedTableRowsListener() {
      public void run(TableRowCore row) {
        removeTorrent((DownloadManager)row.getDataSource(true), true, true);
      }
    });

    itemChangeTracker.addListener(SWT.Selection,
                                  new SelectedTableRowsListener() {
      public void run(TableRowCore row) {
        TRTrackerClient tc = ((DownloadManager)row.getDataSource(true)).getTrackerClient();
        if (tc != null)
          new TrackerChangerWindow(MainWindow.getWindow().getDisplay(), tc);
      }
    });

    itemEditTracker.addListener(SWT.Selection,
                                new SelectedTableRowsListener() {
      public void run(TableRowCore row) {
        final DownloadManager dm = (DownloadManager)row.getDataSource(true);
        if (dm.getTorrent() != null) {
          final TOTorrent torrent = dm.getTorrent();

          java.util.List group = TorrentUtils.announceGroupsToList(torrent);

          new MultiTrackerEditor(null, group, new TrackerEditorListener() {
            public void trackersChanged(String str, String str2, 
                                        java.util.List group) {
              TorrentUtils.listToAnnounceGroups(group, torrent);

              try {
                TorrentUtils.writeToFile(torrent);
              } catch(Throwable e) {
                e.printStackTrace();
              }

              if (dm.getTrackerClient() != null)
                dm.getTrackerClient().resetTrackerUrl( true );
            }
          }, true);
        }
      } // run
    }); 


    itemManualUpdate.addListener(SWT.Selection, 
                                 new SelectedTableRowsListener() {
      public void run(TableRowCore row) {
        ((DownloadManager)row.getDataSource(true)).checkTracker();
      }
    });

    itemDetails.addListener(SWT.Selection,
                            new SelectedTableRowsListener() {
      public void run(TableRowCore row) {
        MainWindow.getWindow().openManagerView((DownloadManager)row.getDataSource(true));
      }
    });



    itemOpen.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        runSelectedTorrents();
      }
    });

    itemExport.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        DownloadManager dm = (DownloadManager)getFirstSelectedDataSource();
        if (dm != null)
          new ExportTorrentWizard(azureus_core, itemExport.getDisplay(), dm);
      }
    });

    itemHost.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        hostSelectedTorrents();
      }
    });

    itemPublish.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        publishSelectedTorrents();
      }
    });

    itemBar.addListener(SWT.Selection,
                        new SelectedTableRowsListener() {
      public void run(TableRowCore row) {
        DownloadManager dm = (DownloadManager)row.getDataSource(true);
        synchronized (downloadBars) {
          if (downloadBars.containsKey(dm)) {
            MinimizedWindow mw = (MinimizedWindow) downloadBars.remove(dm);
            mw.close();
          } else {
            MinimizedWindow mw = new MinimizedWindow(dm, cTablePanel.getShell());
            downloadBars.put(dm, mw);
          }
        } // sync
      } // run
    });

    itemMoveDown.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        moveSelectedTorrentsDown();
      }
    });

    itemMoveUp.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        moveSelectedTorrentsUp();
      }
    });

    itemMoveTop.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        moveSelectedTorrentsTop();
      }
    });

    itemMoveEnd.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        moveSelectedTorrentsEnd();
      }
    });

    itemHigh.addListener(SWT.Selection,
                         new SelectedTableRowsListener() {
      public void run(TableRowCore row) {
        ((DownloadManager)row.getDataSource(true)).setPriority(DownloadManager.HIGH_PRIORITY);
      }
    });

    itemLow.addListener(SWT.Selection,
                         new SelectedTableRowsListener() {
      public void run(TableRowCore row) {
        ((DownloadManager)row.getDataSource(true)).setPriority(DownloadManager.LOW_PRIORITY);
      }
    });

    itemForceStart.addListener(SWT.Selection,
                         new SelectedTableRowsListener() {
      public void run(TableRowCore row) {
        ((DownloadManager)row.getDataSource(true)).setForceStart(itemForceStart.getSelection());
      }
    });

    itemRecheck.addListener(SWT.Selection,
                         new SelectedTableRowsListener() {
      public void run(TableRowCore row) {
        ((DownloadManager)row.getDataSource(true)).forceRecheck();
      }
    });

  } // fillMenu

  private void addCategorySubMenu() {
    MenuItem[] items = menuCategory.getItems();
    int i;
    for (i = 0; i < items.length; i++) {
      items[i].dispose();
    }

    Category[] categories = CategoryManager.getCategories();
    Arrays.sort(categories);

    if (categories.length > 0) {
      Category catUncat = CategoryManager.getCategory(Category.TYPE_UNCATEGORIZED);
      if (catUncat != null) {
        final MenuItem itemCategory = new MenuItem(menuCategory, SWT.PUSH);
        Messages.setLanguageText(itemCategory, catUncat.getName());
        itemCategory.setData("Category", catUncat);
        itemCategory.addListener(SWT.Selection, new Listener() {
          public void handleEvent(Event event) {
            MenuItem item = (MenuItem)event.widget;
            assignSelectedToCategory((Category)item.getData("Category"));
          }
        });

        new MenuItem(menuCategory, SWT.SEPARATOR);
      }

      for (i = 0; i < categories.length; i++) {
        if (categories[i].getType() == Category.TYPE_USER) {
          final MenuItem itemCategory = new MenuItem(menuCategory, SWT.PUSH);
          itemCategory.setText(categories[i].getName());
          itemCategory.setData("Category", categories[i]);

          itemCategory.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event) {
              MenuItem item = (MenuItem)event.widget;
              assignSelectedToCategory((Category)item.getData("Category"));
            }
          });
        }
      }

      new MenuItem(menuCategory, SWT.SEPARATOR);
    }

    final MenuItem itemAddCategory = new MenuItem(menuCategory, SWT.PUSH);
    Messages.setLanguageText(itemAddCategory,
                             "MyTorrentsView.menu.setCategory.add");

    itemAddCategory.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        addCategory();
      }
    });

  }

  /* SubMenu for column specific tasks.
   */
  public void addThisColumnSubMenu(String sColumnName, Menu menuThisColumn) {
    final Table table = getTable();

    if (sColumnName.equals("health")) {
      MenuItem item = new MenuItem(menuThisColumn, SWT.PUSH);
      Messages.setLanguageText(item, "MyTorrentsView.menu.health");
      item.setImage(ImageRepository.getImage("st_explain"));
      item.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event e) {
          HealthHelpWindow.show(table.getDisplay());
        }
      });

    } else if (sColumnName.equals("maxuploads")) {
      int iStart = COConfigurationManager.getIntParameter("Max Uploads") - 2;
      if (iStart < 2) iStart = 2;
      for (int i = iStart; i < iStart + 6; i++) {
        MenuItem item = new MenuItem(menuThisColumn, SWT.PUSH);
        item.setText(String.valueOf(i));
        item.setData("MaxUploads", new Long(i));
        item.addListener(SWT.Selection,
                         new SelectedTableRowsListener() {
          public void run(TableRowCore row) {
            DownloadManager dm = (DownloadManager)row.getDataSource(true);
            MenuItem item = (MenuItem)event.widget;
            if (item != null) {
              int value = ((Long)item.getData("MaxUploads")).intValue();
              dm.getStats().setMaxUploads(value);
            }
          } // run
        }); // listener
      } // for
    }
  }

  private void createDragDrop() {
    Transfer[] types = new Transfer[] { TextTransfer.getInstance()};

    DragSource dragSource = new DragSource(getTable(), DND.DROP_MOVE);
    dragSource.setTransfer(types);
    dragSource.addDragListener(new DragSourceAdapter() {
      public void dragStart(DragSourceEvent event) {
        Table table = getTable();
        if (table.getSelectionCount() != 0 &&
           table.getSelectionCount() != table.getItemCount())
        {
          event.doit = true;
          drag_drop_line_start = table.getSelectionIndex();
         } else {
          event.doit = false;
          drag_drop_line_start = -1;
        }
      }
    });

    DropTarget dropTarget = new DropTarget(getTable(),
                                           DND.DROP_DEFAULT | DND.DROP_MOVE |
                                           DND.DROP_COPY | DND.DROP_LINK |
                                           DND.DROP_TARGET_MOVE);
    dropTarget.setTransfer(new Transfer[] { URLTransfer.getInstance(),
                                            FileTransfer.getInstance(),
                                            TextTransfer.getInstance()});
    dropTarget.addDropListener(new DropTargetAdapter() {
/*
      public void dragEnter(DropTargetEvent event) {
        System.out.print("dragEnter typ id: " + event.currentDataType.type + ",types count: " + event.dataTypes.length + " = ");
        for (int i = 0; i < event.dataTypes.length; i++) {
          System.out.print(event.dataTypes[i].type + ",");
        }
        System.out.println();
      }
      public void dropAccept(DropTargetEvent event) {
        System.out.print("dropAccept typ id: " + event.currentDataType.type + ",types count: " + event.dataTypes.length + " = ");
        for (int i = 0; i < event.dataTypes.length; i++) {
          System.out.print(event.dataTypes[i].type + ",");
        }
        System.out.println();
      }
      public void dragOperationChanged(DropTargetEvent event) {
        System.out.print("dragOperationChanged typ id: " + event.currentDataType.type + ",types count: " + event.dataTypes.length + " = ");
        for (int i = 0; i < event.dataTypes.length; i++) {
          System.out.print(event.dataTypes[i].type + ",");
        }
        System.out.println();
      }
      public void dragLeave(DropTargetEvent event) {
        System.out.print("dragLeave typ id: " + event.currentDataType.type + ",types count: " + event.dataTypes.length + " = ");
        for (int i = 0; i < event.dataTypes.length; i++) {
          System.out.print(event.dataTypes[i].type + ",");
        }
        System.out.println();
      }
//*/
      public void dragOver(DropTargetEvent event) {
/*
        System.out.print("dragOver typ id: " + event.currentDataType.type + ",types count: " + event.dataTypes.length + " = ");
        for (int i = 0; i < event.dataTypes.length; i++) {
          System.out.print(event.dataTypes[i].type + ",");
        }
        System.out.println();
//*/
        if(drag_drop_line_start < 0) {
          if(event.detail != DND.DROP_COPY)
            event.detail = DND.DROP_LINK;
        } else if(TextTransfer.getInstance().isSupportedType(event.currentDataType)) {
          event.feedback = DND.FEEDBACK_EXPAND | DND.FEEDBACK_SCROLL | DND.FEEDBACK_SELECT | DND.FEEDBACK_INSERT_BEFORE | DND.FEEDBACK_INSERT_AFTER;
          event.detail = event.item == null ? DND.DROP_NONE : DND.DROP_MOVE;
        }
      }
      public void drop(DropTargetEvent event) {
        // Torrent file from shell dropped
        if(drag_drop_line_start >= 0) { // event.data == null
          event.detail = DND.DROP_NONE;
          if(event.item == null)
            return;
          int drag_drop_line_end = getTable().indexOf((TableItem)event.item);
          moveSelectedTorrents(drag_drop_line_start, drag_drop_line_end);
          drag_drop_line_start = -1;
        } else {
          TorrentOpener.openDroppedTorrents(azureus_core, event);
        }
      }
    });
  }

  private void moveSelectedTorrents(int drag_drop_line_start, int drag_drop_line_end) {
    if (drag_drop_line_end == drag_drop_line_start)
      return;

    java.util.List list = getSelectedRowsList();
    if (list.size() == 0)
      return;

    TableItem ti = getTable().getItem(drag_drop_line_end);
    TableRowCore row = (TableRowCore)ti.getData("TableRow");
    DownloadManager dm = (DownloadManager)row.getDataSource(true);
    
    int iNewPos = dm.getPosition();
    for (Iterator iter = list.iterator(); iter.hasNext();) {
      row = (TableRowCore)iter.next();
      dm = (DownloadManager)row.getDataSource(true);
      int iOldPos = dm.getPosition();
      
      globalManager.moveTo(dm, iNewPos);
      if (sorter.isAscending()) {
        if (iOldPos > iNewPos)
          iNewPos++;
      } else {
        if (iOldPos < iNewPos)
          iNewPos--;
      }
    }

    if (sorter.getLastField().equals("#"))
      sorter.sortColumn(true);
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#refresh()
   */
  public void refresh() {
    if (getComposite() == null || getComposite().isDisposed())
      return;

    computePossibleActions();
    MainWindow.getWindow().refreshIconBar();

    super.refresh();
  }


  public void delete() {
    super.delete();

    if (fontButton != null && !fontButton.isDisposed()) {
      fontButton.dispose();
      fontButton = null;
    }
    CategoryManager.removeCategoryManagerListener(this);
    globalManager.removeListener(this);
    COConfigurationManager.removeParameterListener("Confirm Data Delete", this);
  }

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
            moveSelectedTorrentsUp();
          // CTRL+CURSOR UP move selected Torrents one up
          else if(e.keyCode == 0x1000002)
            moveSelectedTorrentsDown();
          // CTRL+HOME move selected Torrents to top
          else if(e.keyCode == 0x1000007)
            moveSelectedTorrentsTop();
          // CTRL+END move selected Torrents to end
          else if(e.keyCode == 0x1000008)
            moveSelectedTorrentsEnd();
          // CTRL+A select all Torrents
          else if(e.character == 0x1)
            getTable().selectAll();
          else if(e.character == 0x3) {
            clipboardSelected();
          // CTRL+R resume/start selected Torrents
          } else if(e.character == 0x12)
            resumeSelectedTorrents();
          // CTRL+S stop selected Torrents
          else if(e.character == 0x13)
            stopSelectedTorrents();
        } else if(e.stateMask == 0) {
          // DEL remove selected Torrents
          if(e.keyCode == 127) {
            removeSelectedTorrents();
          } else {
            // normal character: jump to next item with a name beginning with this character
            TableItem[] items = getTable().getSelection();
            int lastSelectedIndex = items.length == 0 ? -1 : getTable().indexOf(items[items.length-1]);
            int nextIndex = globalManager.getNextIndexForCharacter(e.character, lastSelectedIndex);
            if (nextIndex >= 0)
              getTable().setSelection(nextIndex);
          }
        }
      }
    };
  }

  private void changeDirSelectedTorrents() {
    Object[] dataSources = getSelectedDataSources();
    if (dataSources.length <= 0)
      return;

    String sDefPath = COConfigurationManager.getBooleanParameter("Use default data dir") ?
                      COConfigurationManager.getStringParameter("Default save path", "") :
                      "";
    DirectoryDialog dDialog = new DirectoryDialog(cTablePanel.getShell(),
                                                  SWT.SYSTEM_MODAL);
    dDialog.setFilterPath(sDefPath);
    dDialog.setMessage(MessageText.getString("MainWindow.dialog.choose.savepath"));
    String sSavePath = dDialog.open();
    if (sSavePath != null) {
      for (int i = 0; i < dataSources.length; i++) {
        DownloadManager dm = (DownloadManager)dataSources[i];
        if (dm.getState() == DownloadManager.STATE_ERROR &&
            dm.setSavePath(sSavePath) &&
            dm.filesExist()) {
          dm.setState(DownloadManager.STATE_STOPPED);
          ManagerUtils.queue(dm, cTablePanel);
        }
      }
    }
  }

  private void removeTorrent(DownloadManager dm, boolean bDeleteTorrent, 
                             boolean bDeleteData) {
    if (ManagerUtils.isRemoveable(dm)) {
      int choice;
      if (confirmDataDelete && bDeleteData) {
        String path = dm.getFullName();
        MessageBox mb = new MessageBox(cTablePanel.getShell(), 
                                       SWT.ICON_WARNING | SWT.YES | SWT.NO);
        mb.setText(MessageText.getString("deletedata.title"));
        mb.setMessage(MessageText.getString("deletedata.message1")
                      + dm.getName() + " :\n"
                      + path
                      + MessageText.getString("deletedata.message2"));

        choice = mb.open();
      } else {
        choice = SWT.YES;
      }

      if (choice == SWT.YES) {
        try {
          ManagerUtils.remove(dm);
          if (bDeleteData)
            dm.deleteDataFiles();
          if (bDeleteTorrent) {
          	TOTorrent torrent = dm.getTorrent();
          	if ( torrent != null ){
              TorrentUtils.delete( torrent );
          	}
          }
        } catch (GlobalManagerDownloadRemovalVetoException f) {
          Alerts.showErrorMessageBoxUsingResourceString("globalmanager.download.remove.veto", f);
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      } // if choice
    } // if state
  }

  private void removeSelectedTorrents() {
    runForSelectedRows(new GroupTableRowRunner() {
      public void run(TableRowCore row) {
        removeTorrent((DownloadManager)row.getDataSource(true), false, false);
      }
    });
  }

  private void stopSelectedTorrents() {
    runForSelectedRows(new GroupTableRowRunner() {
      public void run(TableRowCore row) {
        ManagerUtils.stop((DownloadManager)row.getDataSource(true), cTablePanel);
      }
    });
  }

  private void queueSelectedTorrents() {
    runForSelectedRows(new GroupTableRowRunner() {
      public void run(TableRowCore row) {
        ManagerUtils.queue((DownloadManager)row.getDataSource(true), cTablePanel);
      }
    });
  }

  private void resumeSelectedTorrents() {
    runForSelectedRows(new GroupTableRowRunner() {
      public void run(TableRowCore row) {
        ManagerUtils.start((DownloadManager)row.getDataSource(true));
      }
    });
  }

  private void hostSelectedTorrents() {
    runForSelectedRows(new GroupTableRowRunner() {
      public void run(TableRowCore row) {
        ManagerUtils.host(azureus_core, (DownloadManager)row.getDataSource(true), cTablePanel);
      }
    });
    MainWindow.getWindow().showMyTracker();
  }

  private void publishSelectedTorrents() {
    runForSelectedRows(new GroupTableRowRunner() {
      public void run(TableRowCore row) {
        ManagerUtils.publish(azureus_core, (DownloadManager)row.getDataSource(true), cTablePanel);
      }
    });
    MainWindow.getWindow().showMyTracker();
  }

  // Note: This only runs the first selected torrent!
  private void runSelectedTorrents() {
    DownloadManager dm = (DownloadManager)getFirstSelectedDataSource();
    if (dm != null)
      ManagerUtils.run(dm);
  }

  private void moveSelectedTorrentsDown() {
    // Don't use runForSelectDataSources to ensure the order we want
    Object[] dataSources = getSelectedDataSources();
    Arrays.sort(dataSources, new Comparator() {
      public int compare (Object a, Object b) {
        return ((DownloadManager)a).getPosition() - ((DownloadManager)b).getPosition();
      }
    });
    for (int i = dataSources.length - 1; i >= 0; i--) {
      DownloadManager dm = (DownloadManager)dataSources[i];
      if (dm.isMoveableDown()) {
        dm.moveDown();
      }
    }

    if (sorter.getLastField().equals("#"))
      sorter.sortColumn(true);
  }

  private void moveSelectedTorrentsUp() {
    // Don't use runForSelectDataSources to ensure the order we want
    Object[] dataSources = getSelectedDataSources();
    Arrays.sort(dataSources, new Comparator() {
      public int compare (Object a, Object b) {
        return ((DownloadManager)a).getPosition() - ((DownloadManager)b).getPosition();
      }
    });
    for (int i = 0; i < dataSources.length; i++) {
      DownloadManager dm = (DownloadManager)dataSources[i];
      if (dm.isMoveableUp()) {
        dm.moveUp();
      }
    }

    if (sorter.getLastField().equals("#"))
      sorter.sortColumn(true);
  }

  private void moveSelectedTorrentsTop() {
    moveSelectedTorrentsTopOrEnd(true);
  }

  private void moveSelectedTorrentsEnd() {
    moveSelectedTorrentsTopOrEnd(false);
  }

  private void moveSelectedTorrentsTopOrEnd(boolean moveToTop) {
    DownloadManager[] downloadManagers = (DownloadManager[])getSelectedDataSources(new DownloadManager[0]);
    if (downloadManagers.length == 0)
      return;
    if(moveToTop)
      globalManager.moveTop(downloadManagers);
    else
      globalManager.moveEnd(downloadManagers);
    if (sorter.getLastField().equals("#"))
      sorter.sortColumn(true);
  }

  /**
   * @param parameterName the name of the parameter that has changed
   * @see org.gudy.azureus2.core3.config.ParameterListener#parameterChanged(java.lang.String)
   */
  public void parameterChanged(String parameterName) {
    super.parameterChanged(parameterName);
    confirmDataDelete = COConfigurationManager.getBooleanParameter("Confirm Data Delete", true);
  }

  private boolean top,bottom,up,down,run,host,publish,start,stop,remove;

  private void computePossibleActions() {
    Object[] dataSources = getSelectedDataSources();
    // enable up and down so that we can do the "selection rotate trick"
    up = down = run = host = publish = remove = (dataSources.length > 0);
    top = bottom = start = stop = false;
    for (int i = 0; i < dataSources.length; i++) {
      DownloadManager dm = (DownloadManager)dataSources[i];

      if(!start && ManagerUtils.isStartable(dm))
        start =  true;
      if(!stop && ManagerUtils.isStopable(dm))
        stop = true;
      if(remove && !ManagerUtils.isRemoveable(dm))
        remove = false;
      if(!top && dm.isMoveableUp())
        top = true;
      if(!bottom && dm.isMoveableDown())
        bottom = true;
    }
  }

  public boolean isEnabled(String itemKey) {
    if(itemKey.equals("run"))
      return run;
    if(itemKey.equals("host"))
      return host;
    if(itemKey.equals("publish"))
      return publish;
    if(itemKey.equals("start"))
      return start;
    if(itemKey.equals("stop"))
      return stop;
    if(itemKey.equals("remove"))
      return remove;
    if(itemKey.equals("top"))
      return top;
    if(itemKey.equals("bottom"))
      return bottom;
    if(itemKey.equals("up"))
      return up;
    if(itemKey.equals("down"))
      return down;
    return false;
  }

  public void itemActivated(String itemKey) {
    if(itemKey.equals("top")) {
      moveSelectedTorrentsTop();
      return;
    }
    if(itemKey.equals("bottom")){
      moveSelectedTorrentsEnd();
      return;
    }
    if(itemKey.equals("up")) {
      moveSelectedTorrentsUp();
      return;
    }
    if(itemKey.equals("down")){
      moveSelectedTorrentsDown();
      return;
    }
    if(itemKey.equals("run")){
      runSelectedTorrents();
      return;
    }
    if(itemKey.equals("host")){
      hostSelectedTorrents();
      return;
    }
    if(itemKey.equals("publish")){
      publishSelectedTorrents();
      return;
    }
    if(itemKey.equals("start")){
      queueSelectedTorrents();
      return;
    }
    if(itemKey.equals("stop")){
      stopSelectedTorrents();
      return;
    }
    if(itemKey.equals("remove")){
      removeSelectedTorrents();
      return;
    }
  }



  public void  removeDownloadBar(DownloadManager dm) {
    synchronized(downloadBars) {
      downloadBars.remove(dm);
    }
  }

  private void addCategory() {
    CategoryAdderWindow adderWindow = new CategoryAdderWindow(MainWindow.getWindow().getDisplay());
    Category newCategory = adderWindow.getNewCategory();
    if (newCategory != null)
      assignSelectedToCategory(newCategory);
  }

  // categorymanagerlistener Functions
  public void downloadManagerAdded(Category category, final DownloadManager manager)
  {
    boolean bCompleted = manager.getStats().getDownloadCompleted(false) == 1000;
    if ((bCompleted && isSeedingView) || (!bCompleted && !isSeedingView)) {
      addDataSource(manager);
    }
  }

  public void downloadManagerRemoved(Category category, DownloadManager removed)
  {
    removeDataSource(removed);
  }


  // DownloadManagerListener Functions
  public void stateChanged(DownloadManager manager, int state) {
  }

  public void positionChanged(DownloadManager download, int oldPosition, int newPosition) {
  }
  
  public void completionChanged(final DownloadManager manager, boolean bCompleted) {
    // manager has moved lists
    if ((isSeedingView && bCompleted) || (!isSeedingView && !bCompleted)) {
      addDataSource(manager);
    } else if ((isSeedingView && !bCompleted) || (!isSeedingView && bCompleted)) {
      removeDataSource(manager);
    }
  }

  public void downloadComplete(DownloadManager manager) {
  }

  // Category Stuff
  private void assignSelectedToCategory(final Category category) {
    runForSelectedRows(new GroupTableRowRunner() {
      public void run(TableRowCore row) {
        ((DownloadManager)row.getDataSource(true)).setCategory(category);
      }
    });
  }

  private void activateCategory(Category category) {
    if (currentCategory != null)
      currentCategory.removeCategoryListener(this);
    if (category != null)
      category.addCategoryListener(this);

    currentCategory = category;

    int catType = (currentCategory == null) ? Category.TYPE_ALL : currentCategory.getType();
    java.util.List managers;
    if (catType == Category.TYPE_USER)
      managers = currentCategory.getDownloadManagers();
    else
      managers = globalManager.getDownloadManagers();

    removeAllTableRows();

    // add new
    if (catType == Category.TYPE_UNCATEGORIZED) {
      for (int i = 0; i < managers.size(); i++) {
        DownloadManager manager = (DownloadManager)managers.get(i);
        if (manager.getCategory() == null)
          downloadManagerAdded(currentCategory, manager);
      }
    } else {
      for (int i = 0; i < managers.size(); i++) {
        downloadManagerAdded(currentCategory, (DownloadManager)managers.get(i));
      }
    }
  }


  // CategoryManagerListener Functions
  public void categoryAdded(Category category) {
    createTabs();
    addCategorySubMenu();
  }

  public void categoryRemoved(Category category) {
    createTabs();
    addCategorySubMenu();
  }

  // globalmanagerlistener Functions
  public void downloadManagerAdded( DownloadManager dm ) {
    dm.addListener( this );

    if (skipDMAdding ||
        (currentCategory != null && currentCategory.getType() == Category.TYPE_USER))
      return;
    Category cat = dm.getCategory();
    if (cat == null)
      downloadManagerAdded(null, dm);
  }

  public void downloadManagerRemoved( DownloadManager dm ) {
    dm.removeListener( this );

    MinimizedWindow mw = (MinimizedWindow) downloadBars.remove(dm);
    if (mw != null) mw.close();

    if (skipDMAdding ||
        (currentCategory != null && currentCategory.getType() == Category.TYPE_USER))
      return;
    downloadManagerRemoved(null, dm);
  }

  public void destroyInitiated() {  }
  public void destroyed() { }

  // End of globalmanagerlistener Functions
}
