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
import java.util.ArrayList;

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
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerListener;
import org.gudy.azureus2.core3.global.GlobalManagerDownloadRemovalVetoException;
import org.gudy.azureus2.core3.category.*;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.plugins.ui.tables.mytorrents.PluginMyTorrentsItemFactory;
import org.gudy.azureus2.pluginsimpl.ui.tables.mytorrents.MyTorrentsTableExtensions;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.MainWindow;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.MinimizedWindow;
import org.gudy.azureus2.ui.swt.TrackerChangerWindow;
import org.gudy.azureus2.ui.swt.CategoryAdderWindow;
import org.gudy.azureus2.ui.swt.URLTransfer;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.exporttorrent.wizard.ExportTorrentWizard;
import org.gudy.azureus2.ui.swt.help.HealthHelpWindow;
import org.gudy.azureus2.ui.swt.maketorrent.MultiTrackerEditor;
import org.gudy.azureus2.ui.swt.maketorrent.TrackerEditorListener;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.TorrentRow;
import org.gudy.azureus2.ui.swt.views.tableitems.utils.ConfigBasedItemEnumerator;
import org.gudy.azureus2.ui.swt.views.tableitems.utils.EnumeratorEditor;
import org.gudy.azureus2.ui.swt.views.tableitems.utils.ITableStructureModificationListener;
import org.gudy.azureus2.ui.swt.views.tableitems.utils.ItemDescriptor;
import org.gudy.azureus2.ui.swt.views.tableitems.utils.ItemEnumerator;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;
import org.gudy.azureus2.ui.swt.views.utils.SortableTable;
import org.gudy.azureus2.ui.swt.views.utils.TableSorter;

/** Displays a list of torrents in a table view.
 *
 * @author Olivier
 * @author TuxPaper
 */
public class MyTorrentsView extends AbstractIView
  implements GlobalManagerListener, 
             SortableTable, 
             ITableStructureModificationListener,
             ParameterListener,
             DownloadManagerListener,
             CategoryManagerListener,
             CategoryListener {

  private GlobalManager globalManager;
  private boolean isSeedingView;

  private Composite composite;
  private Composite panel;
  private Table table;
  private HashMap objectToSortableItem;
  private HashMap tableItemToObject;
  private Menu menu;
  private Composite cCategories;
  private Menu menuCategory;

  private HashMap downloadBars;

  private ItemEnumerator itemEnumerator;
  private TableSorter sorter;

  private String[] tableItems;
  private String configTableName;
  private Category currentCategory;
  private boolean skipDMAdding = true;


	// table item index, where the drag has started
  private int drag_drop_line_start = -1;


  private int loopFactor;
  private int graphicsUpdate = COConfigurationManager.getIntParameter("Graphics Update");
  private boolean confirmDataDelete = COConfigurationManager.getBooleanParameter("Confirm Data Delete", true);

  /**
   * @return Returns the itemEnumerator.
   */
  public ItemEnumerator getItemEnumerator() {
    return itemEnumerator;
  }

  public MyTorrentsView(GlobalManager globalManager, boolean isSeedingView) {
    // Table rows seem to have a bug
    // if any column has a image set using setImage, the first column will
    // reserve a space for an image.
    // Default Health to first column and no one will notice ;)
    final String[] tableItemsDL = {
        "health;L;I;18;0"
        ,"#;R;I;30;1"
        ,"name;L;S;250;2"
        ,"size;R;I;70;3"
        ,"down;R;I;70;4"
        ,"done;R;I;55;5"
        ,"status;L;I;80;6"
        ,"seeds;C;I;60;7"
        ,"peers;C;I;60;8"
        ,"downspeed;R;I;70;9"
        ,"upspeed;R;I;60;10"
        ,"eta;L;I;70;11"
        ,"priority;L;I;50;12"

        ,"tracker;L;I;70;-1"
        ,"shareRatio;R;I;70;-1"
        ,"up;R;I;70;-1"
        ,"pieces;C;I;100;-1"
        ,"completion;C;I;100;-1"
        ,"maxuploads;R;I;30;-1"
        ,"totalspeed;R;I;70;-1"
        ,"savepath;L;S;150;-1"
        ,"category;L;S;70;-1"

    };

    final String[] tableItemsSeeder = {
        "health;L;I;18;0"
        ,"#;R;I;30;1"
        ,"name;L;S;250;2"
        ,"size;R;I;70;3"
        ,"status;L;I;80;4"
        ,"seeds;C;I;60;5"
        ,"peers;C;I;60;6"
        ,"upspeed;R;I;60;7"
        ,"priority;L;I;50;8"
        ,"shareRatio;R;I;70;9"
        ,"up;R;I;70;10"

        ,"tracker;L;I;70;-1"
        ,"done;R;I;55;-1"
        ,"maxuploads;R;I;30;-1"
        ,"totalspeed;R;I;70;-1"
        ,"savepath;L;S;150;-1"
        ,"category;L;S;70;-1"
    };

    this.globalManager = globalManager;
    this.isSeedingView = isSeedingView;


    if (isSeedingView) {
      tableItems = tableItemsSeeder;
      configTableName = "MySeeders";
    }
    else {
      tableItems = tableItemsDL;
      configTableName = "MyTorrents";
    }
    List itemsList = new ArrayList(tableItems.length);
    for(int i=0 ; i < tableItems.length ; i++) {
      itemsList.add(tableItems[i]);
    }
    Map extensions = MyTorrentsTableExtensions.getInstance().getExtensions();
    Iterator iter = extensions.keySet().iterator();
    while(iter.hasNext()) {
      String name = (String) iter.next();
      PluginMyTorrentsItemFactory ppif = (PluginMyTorrentsItemFactory) extensions.get(name);
      if ((isSeedingView && ((ppif.getTablesVisibleIn() & PluginMyTorrentsItemFactory.TABLE_COMPLETE) != 0)) ||
          (!isSeedingView && ((ppif.getTablesVisibleIn() & PluginMyTorrentsItemFactory.TABLE_INCOMPLETE) != 0)))
      {
        String sItem = ppif.getName() + ";" + ppif.getOrientation()  + ";" + 
                       ppif.getType() + ";" + ppif.getDefaultSize() + ";";
        int iPosition = ppif.getDefaultPosition();
        if (iPosition > itemsList.size() || iPosition < -1)
          iPosition = itemsList.size();
        itemsList.add(sItem + iPosition);
      }
    }
    tableItems = (String[])itemsList.toArray(new String[itemsList.size()]);

    objectToSortableItem = new HashMap();
    tableItemToObject = new HashMap();
    downloadBars = MainWindow.getWindow().getDownloadBars();
    currentCategory = CategoryManager.getCategory(Category.TYPE_ALL);
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

    composite.layout();

    COConfigurationManager.addParameterListener("Graphics Update", this);
    COConfigurationManager.addParameterListener("Confirm Data Delete", this);

    activateCategory(currentCategory);
    CategoryManager.addCategoryManagerListener(this);
    // globalManager.addListener sends downloadManagerAdded()'s when you addListener
    // we don't need them..
    skipDMAdding = true;
    globalManager.addListener(this);
    skipDMAdding = false;
  }


  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.views.tableitems.utils.ITableStructureModificationListener#tableStructureChanged()
   */
  public void tableStructureChanged() {
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

    activateCategory(currentCategory);
    panel.layout();
  }


  private void createMainPanel(Composite composite0) {
    GridData gridData;
    composite = new Composite(composite0, SWT.NULL);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    layout.horizontalSpacing = 0;
    layout.verticalSpacing = 0;
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    composite.setLayout(layout);
    composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    createTabs();

    panel = new Composite(composite, SWT.NULL);
    gridData = new GridData(GridData.FILL_BOTH);
    gridData.horizontalSpan = 2;
    panel.setLayoutData(gridData);

    layout = new GridLayout(1, false);
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.verticalSpacing = 0;
    layout.horizontalSpacing = 0;
    panel.setLayout(layout);
  }

  private void createTabs() {
    GridData gridData;
    Category[] categories = CategoryManager.getCategories();
    Arrays.sort(categories);

    if (categories.length > 0) {
      if (cCategories == null) {
        cCategories = new Composite(composite, SWT.NULL);
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
  
        Label l = new Label(composite, SWT.WRAP);
        gridData = new GridData();
        gridData.horizontalIndent = 3;
        l.setLayoutData(gridData);
        Messages.setLanguageText(l, configTableName + "View.header");
        cCategories.moveAbove(null);
        l.moveAbove(null);
      } else {
        Control[] controls = cCategories.getChildren();
        for (int i = 0; i < controls.length; i++)
          controls[i].dispose();
        cCategories.layout();
      }


      for (int i = 0; i < categories.length; i++) {
        final Button catButton = new Button(cCategories, SWT.TOGGLE);
        RowData rd = new RowData();
        rd.height = 17;
        catButton.setLayoutData(rd);
        
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
          Menu menu = new Menu(composite.getShell(), SWT.POP_UP);
  
          final MenuItem itemDelete = new MenuItem(menu, SWT.PUSH);
          Messages.setLanguageText(itemDelete, "MyTorrentsView.menu.category.delete");
          menu.setDefaultItem(itemDelete);
          
          itemDelete.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event) {
              Category catToDelete = (Category)catButton.getData("Category");
              if (catToDelete != null) {
                List managers = catToDelete.getDownloadManagers();
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
      composite.layout();
    }
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
        synchronized(objectToSortableItem) {
          Iterator iter = objectToSortableItem.values().iterator();
          while(iter.hasNext()) {
            TorrentRow row = (TorrentRow) iter.next();
            row.invalidate();
          }
        }
      }
    };

    // Setup table
    // -----------
    itemEnumerator = ConfigBasedItemEnumerator.getInstance(configTableName, tableItems);
    ItemDescriptor[] items = itemEnumerator.getItems();

    // Add 1 to position because we make a non resizable 0-sized 1st column
    // to fix the 1st column gap problem (Eclipse Bug 43910)
    TableColumn tc = new TableColumn(table, SWT.NULL);
    tc.setWidth(0);
    tc.setResizable(false);

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
        // +1 for Eclipse Bug 43910 (see above)
        TableColumn column = table.getColumn(position + 1);
        Messages.setLanguageText(column, "MyTorrentsView." + items[i].getName());
        column.setAlignment(items[i].getAlign());
        column.setWidth(items[i].getWidth());
        if (items[i].getType() == ItemDescriptor.TYPE_INT) {
          sorter.addIntColumnListener(column, items[i].getName());
        }
        if (items[i].getType() == ItemDescriptor.TYPE_STRING) {
          sorter.addStringColumnListener(column, items[i].getName());
        }
        column.setData("configName", "Table." + configTableName + "." + items[i].getName());
        column.addControlListener(resizeListener);
      }
    }

    table.setHeaderVisible(true);
    table.addKeyListener(createKeyListener());

    table.addMouseListener(new MouseAdapter() {
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

    table.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent event) {
        if(event.width == 0 || event.height == 0) return;
				doPaint(new Rectangle(event.x,event.y,event.width,event.height));
			}
    });

    table.setMenu(menu);
  }

  private void createMenu() {
    menu = new Menu(composite.getShell(), SWT.POP_UP);

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

    final Menu menuMove = new Menu(composite.getShell(), SWT.DROP_DOWN);
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

    final Menu menuPriority = new Menu(composite.getShell(), SWT.DROP_DOWN);
    itemPriority.setMenu(menuPriority);

    final MenuItem itemHigh = new MenuItem(menuPriority, SWT.PUSH);
    Messages.setLanguageText(itemHigh, "MyTorrentsView.menu.setpriority.high"); //$NON-NLS-1$
    final MenuItem itemLow = new MenuItem(menuPriority, SWT.PUSH);
    Messages.setLanguageText(itemLow, "MyTorrentsView.menu.setpriority.low"); //$NON-NLS-1$

    // Category

    menuCategory = new Menu(composite.getShell(), SWT.DROP_DOWN);
    final MenuItem itemCategory = new MenuItem(menu, SWT.CASCADE);
    Messages.setLanguageText(itemCategory, "MyTorrentsView.menu.setCategory"); //$NON-NLS-1$
    //itemCategory.setImage(ImageRepository.getImage("speed"));
    itemCategory.setMenu(menuCategory);
    
    addCategorySubMenu();

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
    itemChangeTracker.setImage(ImageRepository.getImage("add_tracker"));

    final MenuItem itemEditTracker = new MenuItem(menu, SWT.PUSH);
    Messages.setLanguageText(itemEditTracker, "MyTorrentsView.menu.editTracker"); //$NON-NLS-1$
    itemEditTracker.setImage(ImageRepository.getImage("edit_trackers"));

    final MenuItem itemRecheck = new MenuItem(menu, SWT.PUSH);
    Messages.setLanguageText(itemRecheck, "MyTorrentsView.menu.recheck");
    itemRecheck.setImage(ImageRepository.getImage("recheck"));

    new MenuItem(menu, SWT.SEPARATOR);

    final MenuItem itemChangeTable = new MenuItem(menu, SWT.PUSH);
    Messages.setLanguageText(itemChangeTable, "MyTorrentsView.menu.editTableColumns"); //$NON-NLS-1$
    itemChangeTable.setImage(ImageRepository.getImage("columns"));

    final MenuItem itemAboutHealth = new MenuItem(menu, SWT.PUSH);
    Messages.setLanguageText(itemAboutHealth, "MyTorrentsView.menu.health"); //$NON-NLS-1$
    itemAboutHealth.setImage(ImageRepository.getImage("st_explain"));



    menu.addListener(SWT.Show, new Listener() {
      public void handleEvent(Event e) {
        TableItem[] tis = table.getSelection();
        boolean hasSelection = (tis.length > 0);

        itemDetails.setEnabled(hasSelection);

        itemOpen.setEnabled(hasSelection);
        itemExport.setEnabled(hasSelection);
        itemHost.setEnabled(hasSelection);
        itemPublish.setEnabled(hasSelection);

        itemMove.setEnabled(hasSelection);
        itemPriority.setEnabled(hasSelection);

        if (hasSelection) {
          boolean moveUp, moveDown, start, stop, remove, changeUrl, barsOpened, 
                  forceStart, forceStartEnabled, recheck, top, bottom;
          moveUp = moveDown = start = stop = remove = changeUrl = barsOpened = 
                   forceStart = forceStartEnabled = recheck = true;
          for (int i = 0; i < tis.length; i++) {
            TableItem ti = tis[i];
            DownloadManager dm = (DownloadManager) tableItemToObject.get(ti);
            if (dm != null) {
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
            }
          }
          itemBar.setSelection(barsOpened);

          itemMoveDown.setEnabled(moveDown);
          itemMoveUp.setEnabled(moveUp);
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

        } else {
          itemBar.setEnabled(false);

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
          int state = dm.getState();
          if (dm != null && 
              (state == DownloadManager.STATE_STOPPED || 
               state == DownloadManager.STATE_ERROR ||
               state == DownloadManager.STATE_QUEUED)) {

          	try{
          		globalManager.removeDownloadManager(dm);
          		try {
          			File f = new File(dm.getTorrentFileName());
          			f.delete();
          		}
          		catch (Exception ex) {
          			ex.printStackTrace();
          		}
            }catch( GlobalManagerDownloadRemovalVetoException f ){

            	MainWindow.showErrorMessageBox( "globalmanager.download.remove.veto", f );
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
          int state = dm.getState();
          if (dm != null && 
              (state == DownloadManager.STATE_STOPPED || 
               state == DownloadManager.STATE_ERROR ||
               state == DownloadManager.STATE_QUEUED)) {
            String path = dm.getFullName();
            MessageBox mb = new MessageBox(panel.getShell(), SWT.ICON_WARNING | SWT.YES | SWT.NO);
            mb.setText(MessageText.getString("deletedata.title"));
            mb.setMessage(
              MessageText.getString("deletedata.message1")
                + dm.getName() + " :\n"
                + path
                + MessageText.getString("deletedata.message2"));

            int choice;
            if (confirmDataDelete) choice = mb.open();
            else choice = SWT.YES;

            if (choice == SWT.YES) {
              try {
                globalManager.removeDownloadManager(dm);
                File f = new File(path);
                FileUtil.recursiveDelete(f);

              }catch( GlobalManagerDownloadRemovalVetoException f ){

              	MainWindow.showErrorMessageBox( "globalmanager.download.remove.veto", f );

              }catch (Exception ex) {

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
          int state = dm.getState();
          if (dm != null && 
              (state == DownloadManager.STATE_STOPPED || 
               state == DownloadManager.STATE_ERROR ||
               state == DownloadManager.STATE_QUEUED)) {
            String path = dm.getFullName();
            MessageBox mb = new MessageBox(panel.getShell(), SWT.ICON_WARNING | SWT.YES | SWT.NO);
            mb.setText(MessageText.getString("deletedata.title"));
            mb.setMessage(
              MessageText.getString("deletedata.message1")
                + dm.getName() + " :\n"
                + path
                + MessageText.getString("deletedata.message2"));

            int choice;
            if (confirmDataDelete) choice = mb.open();
            else choice = SWT.YES;

            if (choice == SWT.YES) {
              try {
                globalManager.removeDownloadManager(dm);
                File f = new File(path);
                FileUtil.recursiveDelete(f);
                f = new File(dm.getTorrentFileName());
                f.delete();

              }catch( GlobalManagerDownloadRemovalVetoException f ){

              	MainWindow.showErrorMessageBox( "globalmanager.download.remove.veto", f );

              }catch (Exception ex) {

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

    itemEditTracker.addListener(SWT.Selection, new Listener() {
    	public void handleEvent(Event e) {
    		TableItem[] tis = table.getSelection();
    		for (int i = 0; i < tis.length; i++) {
    			TableItem ti = tis[i];
    			final DownloadManager dm = (DownloadManager) tableItemToObject.get(ti);
    			if (dm != null && dm.getTorrent() != null) {

    				final TOTorrent	torrent = dm.getTorrent();

    				List	group = TorrentUtils.announceGroupsToList( torrent );

    				new MultiTrackerEditor(null,group,
    						new TrackerEditorListener()
    						{
    							public void
    							trackersChanged(
    								String	str,
									String	str2,
									List	group )
    							{
    								TorrentUtils.listToAnnounceGroups( group, torrent );

    								try{
    									TorrentUtils.writeToFile( torrent );
    								}catch( Throwable e ){

    									e.printStackTrace();
    								}

    								if ( dm.getTrackerClient() != null ){

    									dm.getTrackerClient().resetTrackerUrl( true );
    								}
    							}
    						}, true);
    			}
    		}
    	}
    });

    itemChangeTable.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
          new EnumeratorEditor(table.getDisplay(),ConfigBasedItemEnumerator.getInstance(configTableName,tableItems),MyTorrentsView.this,"MyTorrentsView");
      }
    });

    itemAboutHealth.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
          HealthHelpWindow.show(table.getDisplay());
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
        runSelectedTorrents();
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
        hostSelectedTorrents();
      }
    });

	  itemPublish.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        publishSelectedTorrents();
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

    itemForceStart.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        TableItem[] tis = table.getSelection();
        for (int i = 0; i < tis.length; i++) {
          TableItem ti = tis[i];
          DownloadManager dm = (DownloadManager) tableItemToObject.get(ti);
          dm.setForceStart(itemForceStart.getSelection());
        }
      }
    });

    itemRecheck.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        TableItem[] tis = table.getSelection();
        for (int i = 0; i < tis.length; i++) {
          TableItem ti = tis[i];
          DownloadManager dm = (DownloadManager) tableItemToObject.get(ti);
          if (dm != null) {
            dm.forceRecheck();
          }
        }
      }
    });

  }

  private void addCategorySubMenu() {
    MenuItem[] items = menuCategory.getItems();
    int i;
    for (i = 0; i < items.length; i++)
      items[i].dispose();

    Category[] categories = CategoryManager.getCategories();
    Arrays.sort(categories);

    if (categories.length > 0) {
      for (i = 0; i < categories.length; i++) {
        final MenuItem itemCategory = new MenuItem(menuCategory, SWT.PUSH);
        if (categories[i].getType() == Category.TYPE_USER)
          itemCategory.setText(categories[i].getName());
        else
          Messages.setLanguageText(itemCategory, categories[i].getName());
        itemCategory.setData("Category", categories[i]);
  
        itemCategory.addListener(SWT.Selection, new Listener() {
          public void handleEvent(Event event) {
            MenuItem item = (MenuItem)event.widget;
            assignSelectedToCategory((Category)item.getData("Category"));
          }
        });
      }
  
      new MenuItem(menuCategory, SWT.SEPARATOR);
    }

    final MenuItem itemAddCategory = new MenuItem(menuCategory, SWT.PUSH);
    Messages.setLanguageText(itemAddCategory, "MyTorrentsView.menu.setCategory.add");

    itemAddCategory.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        addCategory();
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
          drag_drop_line_start = -1;
        }
      }
    });

    DropTarget dropTarget = new DropTarget(table, DND.DROP_DEFAULT | DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK | DND.DROP_TARGET_MOVE);
    dropTarget.setTransfer(new Transfer[] { URLTransfer.getInstance(), FileTransfer.getInstance(), TextTransfer.getInstance()});
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
          int drag_drop_line_end = table.indexOf((TableItem)event.item);
          moveSelectedTorrents(drag_drop_line_start, drag_drop_line_end);
          drag_drop_line_start = -1;
        } else {
          MainWindow.getWindow().openDroppedTorrents(event);
        }
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
    if (sorter.getLastField().equals("#"))
      sorter.reOrder(true);
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

    computePossibleActions();
    MainWindow.getWindow().refreshIconBar();

    sorter.reOrder(false);
    synchronized(objectToSortableItem) {
      Iterator iter = objectToSortableItem.keySet().iterator();
      while (iter.hasNext()) {
        if (this.panel.isDisposed())
          return;
        DownloadManager manager = (DownloadManager) iter.next();
        TorrentRow item = (TorrentRow) objectToSortableItem.get(manager);
        if (item != null) {
          //Every N GUI updates we unvalidate the images
          if ((loopFactor % graphicsUpdate) == 0) {
            item.invalidate();
          }
          item.refresh();
        }
      }
    }
    loopFactor++;
  }


  private void doPaint(Rectangle clipping) {
    if (getComposite() == null || getComposite().isDisposed())
      return;

    synchronized(objectToSortableItem) {
      Iterator iter = objectToSortableItem.keySet().iterator();
      while (iter.hasNext()) {
        if (this.panel.isDisposed())
          return;
        DownloadManager manager = (DownloadManager) iter.next();
        TorrentRow item = (TorrentRow) objectToSortableItem.get(manager);
        if (item != null) {
          item.doPaint(clipping);
        }
      }
    }
  }


  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#delete()
   */
  public void delete() {
    CategoryManager.removeCategoryManagerListener(this);
    globalManager.removeListener(this);
    COConfigurationManager.removeParameterListener("ReOrder Delay", sorter);
    COConfigurationManager.removeParameterListener("Graphics Update", this);
    COConfigurationManager.removeParameterListener("Confirm Data Delete", this);
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getFullTitle()
   */
  	public String getFullTitle() {
    	return MessageText.getString("MyTorrentsView.mytorrents");
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
          } else {
            // normal character: jump to next item with a name beginning with this character
            TableItem[] items = table.getSelection();
            int lastSelectedIndex = items.length == 0 ? -1 : table.indexOf(items[items.length-1]);
            int nextIndex = globalManager.getNextIndexForCharacter(e.character, lastSelectedIndex);
            if (nextIndex >= 0)
              table.setSelection(nextIndex);
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
      try{
      	ManagerUtils.remove(dm);
      }catch(GlobalManagerDownloadRemovalVetoException e){
      	MainWindow.showErrorMessageBox( "globalmanager.download.remove.veto", e );
      }
    }
  }

  private void stopSelectedTorrents() {
    TableItem[] tis = table.getSelection();
    for (int i = 0; i < tis.length; i++) {
      TableItem ti = tis[i];
      DownloadManager dm = (DownloadManager) tableItemToObject.get(ti);
      ManagerUtils.stop(dm,panel);
    }
  }

  private void queueSelectedTorrents() {
    TableItem[] tis = table.getSelection();
    for (int i = 0; i < tis.length; i++) {
      TableItem ti = tis[i];
      DownloadManager dm = (DownloadManager) tableItemToObject.get(ti);
      ManagerUtils.queue(dm,panel);
    }
  }

  private void resumeSelectedTorrents() {
    TableItem[] tis = table.getSelection();
    for (int i = 0; i < tis.length; i++) {
      TableItem ti = tis[i];
      DownloadManager dm = (DownloadManager) tableItemToObject.get(ti);
      ManagerUtils.start(dm);
    }
  }

  private void hostSelectedTorrents() {
    TableItem[] tis = table.getSelection();
    for (int i = 0; i < tis.length; i++) {
      TableItem ti = tis[i];
      DownloadManager dm = (DownloadManager) tableItemToObject.get(ti);
      ManagerUtils.host(dm,panel);
    }
    MainWindow.getWindow().showMyTracker();
  }

  private void publishSelectedTorrents() {
    TableItem[] tis = table.getSelection();
    for (int i = 0; i < tis.length; i++) {
      TableItem ti = tis[i];
      DownloadManager dm = (DownloadManager) tableItemToObject.get(ti);
      ManagerUtils.publish(dm,panel);
    }
    MainWindow.getWindow().showMyTracker();
  }

  private void runSelectedTorrents() {
    TableItem[] tis = table.getSelection();
    if (tis.length == 0) {
      return;
    }
    TableItem ti = tis[0];
    DownloadManager dm = (DownloadManager) tableItemToObject.get(ti);
    ManagerUtils.run(dm);
  }

  private void moveSelectedTorrentsDown() {
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

  private void moveSelectedTorrentsUp() {
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

  private void moveSelectedTorrentsTop() {
    moveSelectedTorrentsTopOrEnd(true);
  }

  private void moveSelectedTorrentsEnd() {
    moveSelectedTorrentsTopOrEnd(false);
  }

  private void moveSelectedTorrentsTopOrEnd(boolean moveToTop) {
    TableItem[] tis = table.getSelection();
    if(tis.length == 0)
      return;
    DownloadManager[] downloadManagers = new DownloadManager[tis.length];
    for (int i = 0; i < tis.length; i++) {
      downloadManagers[i] = (DownloadManager) tableItemToObject.get(tis[i]);
    }
    if(moveToTop)
      globalManager.moveTop(downloadManagers);
    else
      globalManager.moveEnd(downloadManagers);
    if (sorter.getLastField().equals("#"))
      sorter.reOrder(true);
  }

  /**
   * @param parameterName the name of the parameter that has changed
   * @see org.gudy.azureus2.core3.config.ParameterListener#parameterChanged(java.lang.String)
   */
  public void parameterChanged(String parameterName) {
    graphicsUpdate = COConfigurationManager.getIntParameter("Graphics Update");
    confirmDataDelete = COConfigurationManager.getBooleanParameter("Confirm Data Delete", true);
  }

  private boolean up,down,run,host,publish,start,stop,remove;

  private void computePossibleActions() {
    if(table == null || table.isDisposed())
      return;
    TableItem[] tis = table.getSelection();
    up = down = run = host = publish = start = stop = remove = false;
    if(tis.length > 0) {
      for (int i = 0; i < tis.length; i++) {
        TableItem ti = tis[i];
        DownloadManager dm = (DownloadManager) tableItemToObject.get(ti);
        if(dm == null)
          continue;

        //Safer here, in case the DownloadManager is null
        remove = up = down = true;
        host = publish = true;
        run = true;

        if(ManagerUtils.isStartable(dm))
          start =  true;
        if(ManagerUtils.isStopable(dm))
          stop = true;
        if(! ManagerUtils.isRemoveable(dm))
          remove = false;
        if(!dm.isMoveableUp())
          up = false;
        if(!dm.isMoveableDown())
          down = false;
      }
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
      return up;
    if(itemKey.equals("bottom"))
      return down;
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
      removeSelectedTorrentsIfStoppedOrError();
      return;
    }
    return;
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
  public void downloadManagerAdded(Category category, DownloadManager manager)
  {
    if ((manager.getStats().getDownloadCompleted(false) == 1000 && isSeedingView) ||
        (manager.getStats().getDownloadCompleted(false) != 1000 && !isSeedingView)) {
      synchronized (objectToSortableItem) {
        TorrentRow item = (TorrentRow) objectToSortableItem.get(manager);
          if (item == null) {
            item = new TorrentRow(this,table, manager);
            objectToSortableItem.put(manager, item);
          }
      }
    }
    manager.addListener( this );
  }

  public void downloadManagerRemoved(Category category, DownloadManager removed)
  {
    MinimizedWindow mw = (MinimizedWindow) downloadBars.remove(removed);
    if (mw != null)
      mw.close();
    TorrentRow managerItem;
    synchronized(objectToSortableItem) {
      managerItem = (TorrentRow) objectToSortableItem.remove(removed);
    }
    if (managerItem != null) {
        TableItem tableItem = managerItem.getTableItem();
        tableItemToObject.remove(tableItem);
        managerItem.delete();
    }
    removed.removeListener( this );
  }


  // DownloadManagerListener Functions
  public void stateChanged(DownloadManager manager, int state)
  {
  }

  public void completionChanged(DownloadManager manager, boolean bCompleted) {
    // manager has moved lists
    synchronized (objectToSortableItem) {
      TorrentRow item = (TorrentRow) objectToSortableItem.get(manager);
      if (item == null) {
        // check to see if it should be in this view
        if ((isSeedingView && bCompleted) ||
            (!isSeedingView && !bCompleted)) {
          // Add to view
          item = new TorrentRow(this,table, manager);
          objectToSortableItem.put(manager, item);
        }
      } else { // item exists in view
        // does it belong?
        if ((isSeedingView && !bCompleted) ||
            (!isSeedingView && bCompleted)) {
          TorrentRow managerItem = (TorrentRow) objectToSortableItem.remove(manager);
          if (managerItem != null) {
            TableItem tableItem = managerItem.getTableItem();
            tableItemToObject.remove(tableItem);
            managerItem.delete();
          }
        }
      }
    }
  }

  public void downloadComplete(DownloadManager manager) {
  }

  // Category Stuff
  private void assignSelectedToCategory(Category category) {
    TableItem[] tis = table.getSelection();
    for (int i = 0; i < tis.length; i++) {
      TableItem ti = tis[i];
      DownloadManager dm = (DownloadManager) tableItemToObject.get(ti);
      dm.setCategory(category);
    }
  }
  
  private void activateCategory(Category category) {
    if (currentCategory != null)
      currentCategory.removeCategoryListener(this);
    if (category != null)
      category.addCategoryListener(this);

    currentCategory = category;

    int catType = (currentCategory == null) ? Category.TYPE_ALL : currentCategory.getType();
    List managers;
    if (catType == Category.TYPE_USER)
      managers = currentCategory.getDownloadManagers();
    else
      managers = globalManager.getDownloadManagers();
      
    
    // remove all
    Iterator iter = objectToSortableItem.values().iterator();
    while(iter.hasNext()) {
      TorrentRow row = (TorrentRow) iter.next();
      TableItem tableItem = row.getTableItem();
      tableItemToObject.remove(tableItem);
      row.delete();
      iter.remove();
    }

    // add new
    if (catType == Category.TYPE_UNCATEGORIZED)
      for (int i = 0; i < managers.size(); i++) {
        DownloadManager manager = (DownloadManager)managers.get(i);
        if (manager.getCategory() == null)
          downloadManagerAdded(currentCategory, manager);
      }
    else
      for (int i = 0; i < managers.size(); i++)
        downloadManagerAdded(currentCategory, (DownloadManager)managers.get(i));
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
	public void	downloadManagerAdded( DownloadManager	dm ) {
	  if (skipDMAdding || 
	      (currentCategory != null && currentCategory.getType() == Category.TYPE_USER))
	    return;
	  Category cat = dm.getCategory();
	  if (cat == null)
      downloadManagerAdded(null, dm);
  }
		
	public void	downloadManagerRemoved(	DownloadManager	dm ) {
	  if (skipDMAdding || 
	      (currentCategory != null && currentCategory.getType() == Category.TYPE_USER))
	    return;
	  Category cat = dm.getCategory();
	  if (cat == null)
      downloadManagerRemoved(null, dm);
	}
		
	public void	destroyInitiated() {
	}
		
	public void destroyed() {
	}
}
