/*
 * Created on 2004/Apr/18
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.pluginsimpl.local.ui.tables.TableContextMenuItemImpl;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.views.table.*;
import org.gudy.azureus2.ui.swt.views.table.impl.TableRowImpl;
import org.gudy.azureus2.ui.swt.views.table.utils.*;


/** An IView with a SortableTable in it.  Handles composite/menu/table creation
 * and management.
 *
 * @author Olivier (Original PeersView/MyTorrentsView/etc code)
 * @author TuxPaper
 *         2004/Apr/20: Remove need for tableItemToObject
 */
public class TableView 
  extends AbstractIView 
  implements SortableTable,
             ParameterListener,
             ITableStructureModificationListener
{
  /** TableID (from {@link TableManager}) of the table this class is
   * handling.  Config settings are stored with the prefix of 
   * "Table.<i>TableID</i>"
   */
  protected String sTableID;
  /** Prefix for retrieving text from the properties file (MessageText)
   * Typically <i>TableID</i> + "View"
   */
  protected String sPropertiesPrefix;
  /** Column name to sort on if user hasn't chosen one yet 
   */
  protected String sDefaultSortOn;
  /** 1st column gap problem (Eclipse Bug 43910).  Set to true when table is 
   * using TableItem.setImage 
   */
  protected boolean bSkipFirstColumn;
  /** Sets the table row's height.  0 for default height.  Do NOT use when
   * TableItem uses setImage
   */
  protected int iCellHeight = 0;

  /** Basic (pre-defined) Column Definitions */
  private TableColumnCore[] basicItems;
  /** All Column Definitions.  The array is not necessarily in column order */
  private TableColumnCore[] tableColumns;

  /** Composite for IView implementation */
  private Composite panel;
  /** Table for SortableTable implementation */
  private Table table;
  /** SWT style options for the creation of the Table */
  private int iTableStyle;
  /** Context Menu */
  private Menu menu;
  /** Context Menu specific to the column the mouse was on */
  private Menu menuThisColumn;

  /** Link DataSource to their row in the table.
   * key = DataSource
   * value = TableRowCore
   */
  private Map objectToSortableItem;
  /** Sorting functions */
  protected TableSorter sorter;
  private boolean bSortScheduled = false;
  /* position of mouse in table.  Used for context menu. */
  private int iMouseX = -1;


  /** For updating GUI.  
   * Some UI objects get updating every X cycles (user configurable) 
   */
  private int loopFactor;
  /** How often graphic cells get updated
   */
  private int graphicsUpdate = COConfigurationManager.getIntParameter("Graphics Update");
  /** Check Column Widths every 10 seconds on Mac if view is active.  
   * Other OSes can capture column width changes automatically */
  private int checkColumnWidthsEvery = Constants.isOSX ?
                                       10000 / COConfigurationManager.getIntParameter("GUI Refresh") :
                                       0;


  /**
   * Main Initializer
   * @param _sTableID Which table to handle (see {@link TableManager}).  Config settings are stored with the prefix of  "Table.<i>TableID</i>"
   * @param _sPropertiesPrefix Prefix for retrieving text from the properties file (MessageText).  Typically <i>TableID</i> + "View"
   * @param _basicItems Column Definitions
   * @param _sDefaultSortOn Column name to sort on if user hasn't chosen one yet
   * @param _iTableStyle SWT style constants used when creating the table
   */  
  public TableView(String _sTableID, 
                   String _sPropertiesPrefix,
                   TableColumnCore[] _basicItems,
                   String _sDefaultSortOn,
                   int _iTableStyle) {
    sTableID = _sTableID;
    basicItems = _basicItems;
    sPropertiesPrefix = _sPropertiesPrefix;
    sDefaultSortOn = _sDefaultSortOn;
    iTableStyle = _iTableStyle;

    objectToSortableItem = new HashMap();
  }

  /**
   * Main Initializer. Table Style will be SWT.SINGLE | SWT.FULL_SELECTION
   *
   * @param _sTableID Which table to handle (see {@link TableManager}).  Config settings are stored with the prefix of  "Table.<i>TableID</i>"
   * @param _sPropertiesPrefix Prefix for retrieving text from the properties file (MessageText).  Typically <i>TableID</i> + "View"
   * @param _basicItems Column Definitions
   * @param _sDefaultSortOn Column name to sort on if user hasn't chosen one yet
   */  
  public TableView(String _sTableID, 
                   String _sPropertiesPrefix,
                   TableColumnCore[] _basicItems,
                   String _sDefaultSortOn) {
    this(_sTableID, _sPropertiesPrefix, _basicItems, _sDefaultSortOn, 
         SWT.SINGLE | SWT.FULL_SELECTION);
  }

  private void initializeColumnDefs() {
    // XXX Adding Columns only has to be done once per TableID.  
    // Doing it more than once won't harm anything, but it's a waste.
    TableColumnManager tcManager = TableColumnManager.getInstance();
    for (int i = 0; i < basicItems.length; i++) {
      tcManager.addColumn(basicItems[i]);
    }

    // fixup order
    tcManager.ensureIntegrety(sTableID);
    
    tableColumns = tcManager.getAllTableColumnCoreAsArray(sTableID);
  }

  /**
   * This method is called when the view is instanciated, it should initialize 
   * all GUI components. Must NOT be blocking, or it'll freeze the whole GUI.
   * Caller is the GUI Thread.
   *
   * @param composite the parent composite. Each view should create a child 
   *        composite, and then use this child composite to add all elements to.
   */
  public void initialize(Composite composite) {
    panel = createMainPanel(composite);

    menu = createMenu();
    fillMenu(menu);
    table = createTable();
    initializeTable(table);

    COConfigurationManager.addParameterListener("Graphics Update", this);
    Colors.getInstance().addColorsChangedListener(this);

    // So all TableView objects of the same TableID have the same columns, and column widths, etc
    TableStructureEventDispatcher.getInstance(sTableID).addListener(this);
  }
  
  
  /** Creates a composite within the specified composite and sets its layout
   * to a default FillLayout().
   *
   * @param composite to create your Composite under
   * @return The newly created composite
   */
  public Composite createMainPanel(Composite composite) {
    panel = new Composite(composite,SWT.NULL);
    panel.setLayout(new FillLayout());

    return panel;
  }
    
  /** Creates the Table.
   *
   * @return The created Table.
   */
  public Table createTable() {
    table = new Table(panel, iTableStyle);
    table.setLayout(new FillLayout());

    return table;
  }

  /** Sets up the sorter, columns, and context menu.
   *
   * @param table Table to be initialized
   */
  public void initializeTable(final Table table) {
    initializeColumnDefs();

    table.setLinesVisible(false);
    table.setMenu(menu);
    table.setData("Name", sTableID);

    sorter = new TableSorter(this, sTableID, sDefaultSortOn, true);

    // OSX doesn't call this!! :(
    ControlListener resizeListener = new ControlAdapter() {
      public void controlResized(ControlEvent e) {
        TableColumn column = (TableColumn) e.widget;
        if (column == null || column.isDisposed())
          return;

        TableColumnCore tc = (TableColumnCore)column.getData("TableColumnCore");
        if (tc != null)
          tc.setWidth(column.getWidth());

        int columnNumber = table.indexOf(column);
        locationChanged(columnNumber);
      }
    };

    // Setup table
    // -----------
    // Add 1 to position because we make a non resizable 0-sized 1st column
    // to fix the 1st column gap problem (Eclipse Bug 43910)
    if (bSkipFirstColumn) {
      TableColumn tc = new TableColumn(table, SWT.NULL);
      tc.setWidth(0);
      tc.setResizable(false);
    }

    //Create all columns
    for (int i = 0; i < tableColumns.length; i++) {
      int position = tableColumns[i].getPosition();
      if (position != -1) {
        new TableColumn(table, SWT.NULL);
      }
    }
    //Assign length and titles
    //We can only do it after ALL columns are created, as position (order)
    //may not be in the natural order (if the user re-order the columns).
    for (int i = 0; i < tableColumns.length; i++) {
      int position = tableColumns[i].getPosition();
      if (position == -1)
        continue;

      String sName = tableColumns[i].getName();
      // +1 for Eclipse Bug 43910 (see above)
      TableColumn column = table.getColumn(position + (bSkipFirstColumn ? 1 : 0));
      Messages.setLanguageText(column, tableColumns[i].getTitleLanguageKey());
      column.setAlignment(tableColumns[i].getSWTAlign());
      column.setWidth(tableColumns[i].getWidth());
      column.setData("TableColumnCore", tableColumns[i]);
      column.setData("configName", "Table." + sTableID + "." + sName);
      column.setData("Name", sName);

      // OSX doesn't call these listeners
      column.addControlListener(resizeListener);
      column.addListener(SWT.Selection, new ColumnListener(tableColumns[i]));
    }

    table.addPaintListener(new PaintListener() {
      public void paintControl(PaintEvent event) {
        if(event.width == 0 || event.height == 0) return;
        doPaint(event.gc);
      }
    });
    
    // Deselect rows if user clicks on a black spot (a spot with no row)
    table.addMouseListener(new MouseAdapter() {
      public void mouseDown(MouseEvent e) {
        iMouseX = e.x;
        try {
          if (table.getItemCount() <= 0)
            return;
          // skip if outside client area (ie. scrollbars)
          Rectangle rTableArea = table.getClientArea();
          //System.out.println("Mouse="+iMouseX+"x"+e.y+";TableArea="+rTableArea);
          Point pMousePosition = new Point(e.x, e.y);
          if (rTableArea.contains(pMousePosition)) {
            TableItem ti = table.getItem(table.getItemCount() - 1);
            Rectangle cellBounds = ti.getBounds(table.getColumnCount() - 1);
            // OSX returns 0 size if the cell is not on screen (sometimes? all the time?)
            if (cellBounds.width <= 0 || cellBounds.height <= 0)
              return;
            //System.out.println("cellbounds="+cellBounds);
            if (e.x > cellBounds.x + cellBounds.width ||
                e.y > cellBounds.y + cellBounds.height) {
              table.deselectAll();
            }
/*        // This doesn't work because of OS inconsistencies when table is scrolled
          // Re-enable once SWT fixes the problem
            TableItem ti = table.getItem(pMousePosition);
            if (ti == null)
              table.deselectAll();
*/
          }
        } catch (Exception ex) {
          System.out.println("MouseDownError");
          ex.printStackTrace();
        }
      }
    });
    
    // XXX this may not be needed if all platforms process mouseDown
    //     before the menu
    table.addMouseMoveListener(new MouseMoveListener() {
      public void mouseMove(MouseEvent e) {
        iMouseX = e.x;
      }
    });

    table.setHeaderVisible(true);
  }

  /** Creates the Context Menu.
   *
   * @return a new Menu object
   */
  public Menu createMenu() {
    return new Menu(panel.getShell(), SWT.POP_UP);
  }

  /** Fill the Context Menu with items.  Only called at TableView initialization
   * and when Table Structure changes.
   *
   * By default, a "Edit Columns" menu and a Column specific menu is set up.
   *
   * @param menu Menu to fill
   */
  public void fillMenu(Menu menu) {
    menuThisColumn = new Menu(panel.getShell(), SWT.DROP_DOWN);
    final MenuItem itemThisColumn = new MenuItem(menu, SWT.CASCADE);
    itemThisColumn.setMenu(menuThisColumn);

    final MenuItem itemChangeTable = new MenuItem(menu, SWT.PUSH);
    Messages.setLanguageText(itemChangeTable, "MyTorrentsView.menu.editTableColumns");
    itemChangeTable.setImage(ImageRepository.getImage("columns"));
    
    menu.addListener(SWT.Show, new Listener() {
      public void handleEvent(Event e) {
        if (table.getItemCount() > 0) {
          TableItem ti = table.getItem(0);
          // Unfortunately, this listener doesn't fill location
          int iColumn = -1;
          for (int i = 0; i < table.getColumnCount(); i++) {
            // M8 Fixes SWT GTK Bug 51777:
            //  "TableItem.getBounds(int) returns the wrong values when table scrolled"
            Rectangle cellBounds = ti.getBounds(i);
            //System.out.println("Mouse.x="+iMouseX+";cellbounds="+cellBounds);
            if (iMouseX >= cellBounds.x && iMouseX < cellBounds.x + cellBounds.width && cellBounds.width > 0) {
              iColumn = i;
              break;
            }
          }
          addThisColumnSubMenu(iColumn);
        }
      }
    });

    itemChangeTable.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        new TableColumnEditorWindow(table.getDisplay(), tableColumns,
                                    TableStructureEventDispatcher.getInstance(sTableID));
      }
    });
    
    // Add Plugin Context menus..
    TableContextMenuItem[] items = TableContextMenuManager.getInstance().getAllAsArray(sTableID);
    if (items.length > 0) {
      new MenuItem(menu, SWT.SEPARATOR);

      for (int i = 0; i < items.length; i++) {
        final TableContextMenuItemImpl contextMenuItem = (TableContextMenuItemImpl)items[i];
        final MenuItem menuItem = new MenuItem(menu, SWT.PUSH);

        Messages.setLanguageText(menuItem, contextMenuItem.getResourceKey());
        menuItem.addListener(SWT.Selection, new SelectedTableRowsListener() {
          public void run(TableRowCore row) {
            contextMenuItem.invokeListeners(row);
          }
        });
      }
    }
  }

  /* SubMenu for column specific tasks. 
   *
   * @param iColumn Column # that tasks apply to.
   */
  private void addThisColumnSubMenu(int iColumn) {
    MenuItem item;

    // Dispose of the old items
    MenuItem[] items = menuThisColumn.getItems();
    for (int i = 0; i < items.length; i++) {
      items[i].dispose();
    }

    item = menuThisColumn.getParentItem();
    if (iColumn == -1) {
      item.setEnabled(false);
      item.setText(MessageText.getString("GenericText.column"));
      return;
    }

    item.setEnabled(true);

    menu.setData("ColumnNo", new Long(iColumn));

    TableColumn tcColumn = table.getColumn(iColumn);
    item.setText("'" + tcColumn.getText() + "' " + 
                 MessageText.getString("GenericText.column"));

    String sColumnName = (String)tcColumn.getData("Name");
    if (sColumnName != null) {
      addThisColumnSubMenu(sColumnName, menuThisColumn);
    }

    if (menuThisColumn.getItemCount() > 0) {
      new MenuItem(menuThisColumn, SWT.SEPARATOR);
    }

    item = new MenuItem(menuThisColumn, SWT.PUSH);
    Messages.setLanguageText(item, "MyTorrentsView.menu.thisColumn.sort");
    item.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        int iColumn = ((Long)menu.getData("ColumnNo")).intValue();
        table.getColumn(iColumn).notifyListeners(SWT.Selection, new Event());
      }
    });

    item = new MenuItem(menuThisColumn, SWT.PUSH);
    Messages.setLanguageText(item, "MyTorrentsView.menu.thisColumn.remove");
    item.setEnabled(false);

    item = new MenuItem(menuThisColumn, SWT.PUSH);
    Messages.setLanguageText(item, "MyTorrentsView.menu.thisColumn.toClipboard");
    item.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        String sToClipboard = "";
        int iColumn = ((Long)menu.getData("ColumnNo")).intValue();
        TableItem[] tis = table.getSelection();
        for (int i = 0; i < tis.length; i++) {
          if (i != 0) sToClipboard += "\n";
          sToClipboard += tis[i].getText(iColumn);
        }
        new Clipboard(panel.getDisplay()).setContents(new Object[] { sToClipboard }, 
                                                      new Transfer[] {TextTransfer.getInstance()});
      }
    });
  }
  
  /** Create a SubMenu for column specific tasks.  Everytime the user opens
   * the context menu, the "This Column" submenu is cleared, and this function
   * is called to refill it.
   *
   * @param sColumnName The name of the column the user clicked on
   * @param menuThisColumn the menu to fill with MenuItems
   */
  public void addThisColumnSubMenu(String sColumnName, Menu menuThisColumn) {
/*  // Template code
    if (sColumnName.equals("xxx")) {
      item = new MenuItem(menuThisColumn, SWT.PUSH);
      Messages.setLanguageText(item, "xxx.menu.xxx");
      item.setImage(ImageRepository.getImage("xxx"));
      item.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event e) {
          // Code here
        }
      });
*/
  }

  /** IView.getComposite()
   * @return the composite for this TableView
   */
  public Composite getComposite() {
    return panel;
  }

  /** IView.refresh(), called when the GUI needs an update */
  public void refresh() {
    if(getComposite() == null || getComposite().isDisposed())
      return;

    // Since OSX can't handle TableColumn ControlListener, check widths
    if (checkColumnWidthsEvery != 0 && 
        (loopFactor % checkColumnWidthsEvery) == 0) {
      TableColumn[] tableColumnsSWT = table.getColumns();
      for (int i = 0; i < tableColumnsSWT.length; i++) {
        TableColumnCore tc = (TableColumnCore)tableColumnsSWT[i].getData("TableColumnCore");
        if (tc != null && tc.getWidth() != tableColumnsSWT[i].getWidth()) {
          tc.setWidth(tableColumnsSWT[i].getWidth());

          int columnNumber = table.indexOf(tableColumnsSWT[i]);
          locationChanged(columnNumber);
        }
      }
    }

    if (bSortScheduled) {
      bSortScheduled = false;
      sorter.sortColumn();
    } else {
      sorter.reOrder(false);
    }

    //Refresh all items in table...
    runForAllRows(new GroupTableRowRunner() {
      public void run(TableRowCore row) {
        // Every N GUI updates we refresh graphics
        row.refresh((loopFactor % graphicsUpdate) == 0);
      }
    });

    Utils.alternateTableBackground(table);
    loopFactor++;
  }

  private void locationChanged(final int iStartColumn) {
    if (getComposite() == null || getComposite().isDisposed())
      return;    
    
    runForAllRows(new GroupTableRowRunner() {
      public void run(TableRowCore row) {
        row.locationChanged(iStartColumn);
      }
    });
  }

  private void doPaint(final GC gc) {
    if (getComposite() == null || getComposite().isDisposed())
      return;    
    
    runForAllRows(new GroupTableRowRunner() {
      public void run(TableRowCore row) {
        row.doPaint(gc);
      }
    });
  }

  /** IView.delete: This method is caled when the view is destroyed.
   * Each color instanciated, images and such things should be disposed.
   * The caller is the GUI thread.
   */
  public void delete() {
    TableStructureEventDispatcher.getInstance(sTableID).removeListener(this);
    for (int i = 0; i < tableColumns.length; i++)
      tableColumns[i].saveSettings();

    removeAllTableRows();
    if (table != null && !table.isDisposed())
      table.dispose();
    COConfigurationManager.removeParameterListener("ReOrder Delay", sorter);
    COConfigurationManager.removeParameterListener("Graphics Update", this);
    Colors.getInstance().removeColorsChangedListener(this);
  }

  /** IView.getData: Data 'could' store a key to a language file, in order to 
   * support multi-language titles
   *
   * @return a String which is the key of this view title.
   */
  public String getData() {
    return sPropertiesPrefix + ".title.short";
  }

  /** IView.getFullTitle:Called in order to set / update the title of this View
   * @return the full title for the view
   */
  public String getFullTitle() {
    return MessageText.getString(sPropertiesPrefix + ".title.full");
  }

  /** Adds a dataSource to the table as a new row.  If the data source is
   * already added, a new row will not be added.  This function runs 
   * asynchronously, so the rows creation is not guaranteed directly after
   * calling this function.
   *
   * @param dataSource data source to add to the table
   */
  public synchronized void addDataSource(final Object dataSource) {
    try {
      if (objectToSortableItem.containsKey(dataSource) || panel.isDisposed())
        return;
      synchronized (objectToSortableItem) {
        // Since adding to objectToSortableItem is async, there's a chance
        // the item will not be stored in objectToSortableItem before another
        // call here.  So, add it now and null it
        objectToSortableItem.put(dataSource, null);
      }
      Display display = panel.getDisplay();
      // syncExec is evil because we eventually end up in a sync lock.
      // So, use async, then wait for it to finish
      display.asyncExec(new Runnable() {
        public void run() {
          TableRowCore row = new TableRowImpl(TableView.this, dataSource, 
                                              bSkipFirstColumn);

          if (iCellHeight > 0)
            row.setHeight(iCellHeight);

          synchronized (objectToSortableItem) {
            objectToSortableItem.put(dataSource, row);
          }
          TableCellCore cell = row.getTableCellCore(sorter.getLastField());
          if (cell != null)
            cell.refresh();
          bSortScheduled = true;
        }
      });
    } catch (Exception e) {
      System.out.println("Error adding row to " + sTableID + " table");
      e.printStackTrace();
    }
  }

  /** Remove the specified dataSource from the table.
   *
   * @param dataSource data source to be removed
   */
  public void removeDataSource(Object dataSource) {
    TableRowCore item;
    synchronized (objectToSortableItem) {
      item = (TableRowCore)objectToSortableItem.remove(dataSource);
    }
    if (item == null)
      return;
    item.delete();
  }

  /** Remove all the data sources (table rows) from the table.
   */
  public void removeAllTableRows() {
    // clear all table items first, so that TableRowCore.delete() doesn't remove
    // them one by one (slow)
    if (table != null && !table.isDisposed())
      table.removeAll();

    runForAllRows(new GroupTableRowRunner() {
      public void run(TableRowCore row) {
        row.delete();
      }
    });
    objectToSortableItem.clear();
/* Old Way.  DELME after new way is verified working :)
    Iterator iter = objectToSortableItem.values().iterator();
    while(iter.hasNext()) {
      TableRowCore row = (TableRowCore) iter.next();
      if (row != null) row.delete();
      iter.remove();
    }
*/
  }
    
  public Table getTable() {
    return table;
  }
  
  public String getTableID() {
    return sTableID;
  }

  /* ParameterListener Implementation */

  public void parameterChanged(String parameterName) {
    if (parameterName.equals("Graphics Update")) {
      graphicsUpdate = COConfigurationManager.getIntParameter("Graphics Update");
      return;
    }
    if (parameterName.startsWith("Color")) {
      runForAllRows(new GroupTableRowRunner() {
        public void run(TableRowCore row) {
          row.setValid(false);
        }
      });
    }
  }
  
  /* ITableStructureModificationListener implementation */

  public void tableStructureChanged() {
    //2. Clear everything
    removeAllTableRows();
    
    //3. Dispose the old table
    if (table != null && !table.isDisposed()) {
      table.dispose();
    }
    menu.dispose();
    
    //4. Re-create the table
    menu = createMenu();
    fillMenu(menu);
    table = createTable();
    initializeTable(table);
    
    panel.layout();
  }
  
  /** The Columns width changed
   *
   * @param columnNumber # of column which size has changed for
   * @param newWidth New width of column
   */
  public void columnSizeChanged(TableColumnCore tableColumn) {
    int newWidth = tableColumn.getWidth();
    if (table == null || table.isDisposed())
      return;
    
    TableColumn column = null;
    TableColumn[] tableColumnsSWT = table.getColumns();
    for (int i = 0; i < tableColumnsSWT.length; i++) {
      if (tableColumnsSWT[i].getData("TableColumnCore") == tableColumn) {
        column = tableColumnsSWT[i];
        break;
      }
    }
    if (column == null || column.isDisposed() || (column.getWidth() == newWidth))
      return;

    column.setWidth(newWidth);
  }
  
  public void columnInvalidate(TableColumnCore tableColumn) {
    final String sColumnName = tableColumn.getName();
    runForAllRows(new GroupTableRowRunner() {
      public void run(TableRowCore row) {
        TableCellCore cell = row.getTableCellCore(sColumnName);
        if (cell != null)
          cell.setValid(false);
      }
    });
  }

  /* End ITableStructureModificationListener implementation */

  /** Get all the cells for one Column, in the order they are displayed */
  /* SortableTable implementation */
  public List getColumnCoreCells(String sColumnName) {
    ArrayList l = new ArrayList();
    if (table != null && !table.isDisposed()) {
      TableItem[] tis = table.getItems();
      for (int i = 0; i < tis.length; i++) {
        TableRowCore row = (TableRowCore)tis[i].getData("TableRow");
        if (row == null)
          continue;
        TableCellCore cell = row.getTableCellCore(sColumnName);
        if (cell != null)
          l.add(cell);
      }
    }
    return l;
  }

  /** Get all the rows for this table, in the order they are displayed
   *
   * @return a list of TableRowCore objects in the order the user sees them
   */
  public List getRowsOrdered() {
    ArrayList l = new ArrayList();
    if (table != null && !table.isDisposed()) {
      TableItem[] tis = table.getItems();
      for (int i = 0; i < tis.length; i++) {
        TableRowCore row = (TableRowCore)tis[i].getData("TableRow");
        if (row != null)
          l.add(row);
      }
    }
    return l;
  }
  
  /** Get all the rows for this table, in no particular order.  Faster than
   * {@link #getRowsOrdered}
   *
   * @return TableRowCore objects.  May contain null entries.
   */
  public TableRowCore[] getRowsUnordered() {
    return (TableRowCore[])objectToSortableItem.values().toArray(new TableRowCore[0]);
  }
  
  /** Return all the TableColumnCore objects that belong to this TableView
   *
   * @return All the TableColumnCore objects
   */
  public TableColumnCore[] getAllTableColumnCore() {
    return tableColumns;
  }
  
  /** Return the specified TableColumnCore object
   *
   * @return TableColumnCore requested
   */
  /* SortableTable implementation */
  public TableColumnCore getTableColumnCore(String sColumnName) {
    TableColumnManager tcManager = TableColumnManager.getInstance();
    return tcManager.getTableColumnCore(sTableID, sColumnName);
  }
    
  /* various selected rows functions */
  /***********************************/

  /** Returns an array of all selected Data Sources.  Null data sources are
   * ommitted.
   *
   * @return an array containing the selected data sources
   */
  public List getSelectedDataSourcesList() {
    ArrayList l = new ArrayList();
    if (table != null && !table.isDisposed()) {
      TableItem[] tis = table.getSelection();
      for (int i = 0; i < tis.length; i++) {
        TableRowCore row = (TableRowCore)tis[i].getData("TableRow");
        if (row != null && row.getDataSource(true) != null)
          l.add(row.getDataSource(true));
      }
    }
    return l;
  }

  /** Returns an array of all selected Data Sources.  Null data sources are
   * ommitted.
   *
   * @param a the array into which the selected data sources are to be stored, 
   *          if the size is the big enough; otherwise, a new array of the same 
   *          runtime type is allocated for this purpose.
   *
   * @return an array containing the selected data sources
   */
  public Object[] getSelectedDataSources(Object[] a) {
    return getSelectedDataSourcesList().toArray(a);
  }

  /** Returns an array of all selected Data Sources.  Null data sources are
   * ommitted.
   *
   * @return an array containing the selected data sources
   */
  public Object[] getSelectedDataSources() {
    return getSelectedDataSourcesList().toArray();
  }

  /** Returns an array of all selected TableRowCore.  Null data sources are
   * ommitted.
   *
   * @return an array containing the selected data sources
   */
  public TableRowCore[] getSelectedRows() {
    return (TableRowCore[])getSelectedRowsList().toArray(new TableRowCore[0]);
  }

  /** Returns an list of all selected TableRowCore objects.  Null data sources are
   * ommitted.
   *
   * @return an list containing the selected TableRowCore objects
   */
  public List getSelectedRowsList() {
    ArrayList l = new ArrayList();
    if (table != null && !table.isDisposed()) {
      TableItem[] tis = table.getSelection();
      for (int i = 0; i < tis.length; i++) {
        TableRowCore row = (TableRowCore)tis[i].getData("TableRow");
        if (row != null && row.getDataSource(true) != null)
          l.add(row);
      }
    }
    return l;
  }
  
  /** Returns the first selected data sources.
   *
   * @return the first selected data source, or null if no data source is 
   *         selected
   */
  public Object getFirstSelectedDataSource() {
    if (table == null || table.isDisposed() || table.getSelectionCount() == 0)
      return null;

    TableRowCore row = (TableRowCore)table.getSelection()[0].getData("TableRow");
    if (row == null)
      return null;
    return row.getDataSource(true);
  }
  
  /** For each row source that the user has selected, run the code 
   * provided by the specified parameter.
   *
   * @param runner Code to run for each selected row/datasource
   */
  public void runForSelectedRows(GroupTableRowRunner runner) {
    if (table == null || table.isDisposed())
      return;

    TableItem[] tis = table.getSelection();
    for (int i = 0; i < tis.length; i++) {
      TableRowCore row = (TableRowCore)tis[i].getData("TableRow");
      if (row != null)
        runner.run(row);
    }
  }

  public void runForAllRows(GroupTableRowRunner runner) {
    synchronized (objectToSortableItem) {
      Iterator iter = objectToSortableItem.values().iterator();
      while (iter.hasNext()) {
        TableRowCore row = (TableRowCore)iter.next();
        if (row != null)
          runner.run(row);
      }
    }
  }
  
  /** Send Selected rows to the clipboard in a SpreadSheet friendly format 
   * (tab/cr delimited)
   */
  public void clipboardSelected() {
    String sToClipboard = "";
    for (int j = 0; j < getTable().getColumnCount(); j++) {
      if (j != 0) sToClipboard += "\t";
      sToClipboard += getTable().getColumn(j).getText();
    }

    TableItem[] tis = getTable().getSelection();
    for (int i = 0; i < tis.length; i++) {
      sToClipboard += "\n";
      for (int j = 0; j < getTable().getColumnCount(); j++) {
        if (j != 0) sToClipboard += "\t";
        sToClipboard += tis[i].getText(j);
      }
    }
    new Clipboard(getComposite().getDisplay()).setContents(
                                  new Object[] { sToClipboard },
                                  new Transfer[] {TextTransfer.getInstance()});
  }
    
  
  /** Used with {@link #runForSelectedRows}
   */
  public abstract class GroupTableRowRunner {
    /** Code to run 
     * @param row TableRowCore to run code against
     */
    public abstract void run(TableRowCore row);
  }
  
  /** Listener primarily for Menu Selection.  Implement run(TableRowCore) and it
   * will get called for each row the user has selected.
   */
  public abstract class SelectedTableRowsListener 
         extends GroupTableRowRunner
         implements Listener 
 {
    /** Event information passed in via the Listener.  Accessible in 
     * run(TableRowCore).
     */
    protected Event event;
    /** Process the trapped event.  This function does not need to be overidden.
     * @param e event information
     */
    public void handleEvent(Event e) {
      event = e;
      runForSelectedRows(this);
    }
    
    public abstract void run(TableRowCore row);
    
  }
  
  /** Handle sorting of a column based on clicking the Table Header */
  private class ColumnListener implements Listener {
    private TableColumnCore tableColumn;

    /** Initialize ColumnListener
     * @param tc TableColumnCore that will be sorted when header is clicked
     */
    public ColumnListener(TableColumnCore tc) {
      tableColumn = tc;
    }

    /** Process a Table Header click
     * @param e event information
     */
    public void handleEvent(Event e) {
      sorter.sortColumnReverse(tableColumn);
    }
  }

}
