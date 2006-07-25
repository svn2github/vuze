/*
 * Created on 2004/Apr/18
 *
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
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
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.views;

import java.util.*;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.config.impl.ConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.util.Timer;
import org.gudy.azureus2.pluginsimpl.local.ui.tables.TableContextMenuItemImpl;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.debug.ObfusticateImage;
import org.gudy.azureus2.ui.swt.debug.UIDebugGenerator;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.plugins.UISWTGraphic;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTInstanceImpl;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewImpl;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.views.table.ITableStructureModificationListener;
import org.gudy.azureus2.ui.swt.views.table.TableCellCore;
import org.gudy.azureus2.ui.swt.views.table.TableColumnCore;
import org.gudy.azureus2.ui.swt.views.table.TableRowCore;
import org.gudy.azureus2.ui.swt.views.table.impl.TableCellImpl;
import org.gudy.azureus2.ui.swt.views.table.impl.TableRowComparator;
import org.gudy.azureus2.ui.swt.views.table.impl.TableRowImpl;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnEditorWindow;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnManager;
import org.gudy.azureus2.ui.swt.views.table.utils.TableContextMenuManager;
import org.gudy.azureus2.ui.swt.views.table.utils.TableStructureEventDispatcher;
import org.gudy.azureus2.ui.swt.views.utils.VerticalAligner;

import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;

import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.tables.TableCellMouseEvent;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;


/** 
 * An IView with a sortable table.  Handles composite/menu/table creation
 * and management.
 * <P>
 * Usage of this class is rather haphazard.  Sometimes, you can set a
 * variable to change behavior.  Other times you call a function.  Some
 * functions are meant for extending or implementing, others are meant as
 * normal functions.  
 *
 * @author Olivier (Original PeersView/MyTorrentsView/etc code)
 * @author TuxPaper
 *         2004/Apr/20: Remove need for tableItemToObject
 *         2005/Oct/07: Virtual Table
 *         2005/Nov/16: Moved TableSorter into TableView
 *         
 * @todo Remove TableSorter.java, SortableTable.java from CVS
 * 
 * @note From TableSorter.java:<br>
 *   <li>2004/Apr/20: Remove need for tableItemToObject (store object in tableItem.setData)
 *   <li>2004/May/11: Use Comparable instead of SortableItem
 *   <li>2004/May/14: moved from org.gudy.azureus2.ui.swt.utils
 *   <li>2005/Oct/10: v2307 : Sort SWT.VIRTUAL Tables, Column Indicator
 *   
 * @future TableView should be split into two.  One for non SWT functions, and
 *          the other extending the first, with extra SWT stuff. 
 *
 * @future dataSourcesToRemove should be removed after a certain amount of time
 *          has passed.  Currently, dataSourcesToRemove is processed every
 *          refresh IF the table is visible, or it is processed when we collect
 *          20 items to remove. 
 */
public class TableView 
  extends AbstractIView 
  implements ParameterListener,
             ITableStructureModificationListener, ObfusticateImage
{
	private final static LogIDs LOGID = LogIDs.GUI;
	
	/** Helpfull output when trying to debug add/removal of rows */
	public final static boolean DEBUGADDREMOVE = false;
	
	/** Virtual Tables still a work in progress */
	// Non-Virtual tables scroll faster with they keyboard
	// Virtual tables don't flicker when updating a cell (Windows)
	private final static boolean DISABLEVIRTUAL = !Constants.isWindows
			|| SWT.getVersion() < 3138;

	private final static boolean COLUMN_CLICK_DELAY = Constants.isOSX
			&& SWT.getVersion() >= 3221 && SWT.getVersion() <= 3222;

	private static final boolean DEBUG_SORTER = false;

	// Shorter name for ConfigManager, easier to read code
	private static final ConfigurationManager configMan = ConfigurationManager
			.getInstance();

	private static final String CFG_SORTDIRECTION = "config.style.table.defaultSortOrder";

	private static final long IMMEDIATE_ADDREMOVE_DELAY = 150;

	private static final long IMMEDIATE_ADDREMOVE_MAXDELAY = 2000;

  /** TableID (from {@link org.gudy.azureus2.plugins.ui.tables.TableManager}) 
   * of the table this class is
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
  private boolean bSkipFirstColumn;
  
  private Point ptIconSize = null;

  /** Basic (pre-defined) Column Definitions */
  private TableColumnCore[] basicItems;
  /** All Column Definitions.  The array is not necessarily in column order */
  private TableColumnCore[] tableColumns;

  /** Composite for IView implementation */
  private Composite mainComposite;
  
  /** Composite that stores the table (sometimes the same as mainComposite) */
  private Composite tableComposite;
  
  /** Table for SortableTable implementation */
  private Table table;
  /** SWT style options for the creation of the Table */
  protected int iTableStyle;
  /** Whether the Table is Virtual */
  private boolean bTableVirtual;
  /** Context Menu */
  private Menu menu;
  /** Context Menu specific to the column the mouse was on */
  private Menu menuThisColumn;

  /** Link DataSource to their row in the table.
   * key = DataSource
   * value = TableRowCore
   */
  private Map 		dataSourceToRow;
  private AEMonitor dataSourceToRow_mon 	= new AEMonitor( "TableView:OTSI" );
  private List      sortedRows;
  private AEMonitor sortedRows_mon 	= new AEMonitor( "TableView:sR" );
  private AEMonitor sortColumn_mon 	= new AEMonitor( "TableView:sC" );
  
  /** Sorting functions */
  protected TableRowComparator rowSorter;
  /** TimeStamp of when last sorted all the rows was */
	private long lLastSortedOn;

  /* position of mouse in table.  Used for context menu. */
  private int iMouseX = -1;


  /** For updating GUI.  
   * Some UI objects get updating every X cycles (user configurable) 
   */
  protected int loopFactor;
  /** How often graphic cells get updated
   */
  protected int graphicsUpdate = configMan.getIntParameter("Graphics Update");
  
	protected int reOrderDelay = configMan.getIntParameter("ReOrder Delay");

  /** Check Column Widths every 10 seconds on Pre 3.0RC1 on OSX if view is active.  
   * Other OSes can capture column width changes automatically */
  private int checkColumnWidthsEvery = (Constants.isOSX && SWT.getVersion() < 3054) ?
                                       10000 / configMan.getIntParameter("GUI Refresh") :
                                       0;

  /**
   * Cache of selected table items to bypass insufficient drawing on Mac OS X
   */
  //private ArrayList oldSelectedItems;
  
  
  /** We need to remember the order of the columns at the time we added them
   * in case the user drags the columns around.
   */
  private TableColumnCore[] columnsOrdered;

	private ColumnMoveListener columnMoveListener = new ColumnMoveListener();
	
	/** Queue added datasources and add them on refresh */
	private List dataSourcesToAdd = new ArrayList(4);
	
	/** Queue removed datasources and add them on refresh */
	private List dataSourcesToRemove = new ArrayList(4);

	private Timer timerProcessDataSources = new Timer("Process Data Sources");
	private TimerEvent timerEventProcessDS;

	private boolean bReallyAddingDataSources = false;


	/** TabViews */
	public boolean bEnableTabViews = false;
	/** TabViews */
  private CTabFolder tabFolder;
	/** TabViews */
  private ArrayList tabViews = new ArrayList(1);

  private int lastTopIndex = 0;
  private int lastBottomIndex = -1;
  
  protected IView[] coreTabViews = null;
  
  private long lCancelSelectionTriggeredOn = -1;

  // XXX Remove after column selection is no longered triggered on column resize (OSX)
  private long lLastColumnResizeOn = -1;

  /**
   * Main Initializer
   * @param _sTableID Which table to handle (see 
   *                   {@link org.gudy.azureus2.plugins.ui.tables.TableManager}).
   *                   Config settings are stored with the prefix of  
   *                   "Table.<i>TableID</i>"
   * @param _sPropertiesPrefix Prefix for retrieving text from the properties
   *                            file (MessageText).  Typically 
   *                            <i>TableID</i> + "View"
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
    iTableStyle = _iTableStyle | SWT.V_SCROLL;
    if (DISABLEVIRTUAL)
    	iTableStyle &= ~(SWT.VIRTUAL);
    bTableVirtual = (iTableStyle & SWT.VIRTUAL) != 0;

    dataSourceToRow = new HashMap();
    sortedRows = new ArrayList();
  }

  /**
   * Main Initializer. Table Style will be SWT.SINGLE | SWT.FULL_SELECTION
   *
   * @param _sTableID Which table to handle (see 
   *                   {@link org.gudy.azureus2.plugins.ui.tables.TableManager}
   *                   ).  Config settings are stored with the prefix of 
   *                   "Table.<i>TableID</i>"
   * @param _sPropertiesPrefix Prefix for retrieving text from the properties
   *                            file (MessageText).  
   *                            Typically <i>TableID</i> + "View"
   * @param _basicItems Column Definitions
   * @param _sDefaultSortOn Column name to sort on if user hasn't chosen one
   *                         yet
   */  
  public TableView(String _sTableID, 
                   String _sPropertiesPrefix,
                   TableColumnCore[] _basicItems,
                   String _sDefaultSortOn) {
    this(_sTableID, _sPropertiesPrefix, _basicItems, _sDefaultSortOn, 
         SWT.SINGLE | SWT.FULL_SELECTION | SWT.VIRTUAL);
  }

  private void initializeColumnDefs() {
    // XXX Adding Columns only has to be done once per TableID.  
    // Doing it more than once won't harm anything, but it's a waste.
    TableColumnManager tcManager = TableColumnManager.getInstance();
    if (tcManager.getTableColumnCount(sTableID) != basicItems.length) {
	    for (int i = 0; i < basicItems.length; i++) {
	      tcManager.addColumn(basicItems[i]);
	    }
    }

    // fixup order
    tcManager.ensureIntegrety(sTableID);

    tableColumns = tcManager.getAllTableColumnCoreAsArray(sTableID);
  }

  // AbstractIView::initialize
  public void initialize(Composite composite) {
  	composite.setRedraw(false);
  	mainComposite = createSashForm(composite);
    menu = createMenu();
    table = createTable(tableComposite);
    initializeTable(table);

    configMan.addParameterListener("Graphics Update", this);
    configMan.addParameterListener("ReOrder Delay", this);
    Colors.getInstance().addColorsChangedListener(this);

    // So all TableView objects of the same TableID have the same columns,
    // and column widths, etc
    TableStructureEventDispatcher.getInstance(sTableID).addListener(this);
  	composite.setRedraw(true);
  }

  
	private Composite createSashForm(final Composite composite) {
		if (!bEnableTabViews) {
			tableComposite = createMainPanel(composite);
			return tableComposite;
		}

		int iNumViews = coreTabViews == null ? 0 : coreTabViews.length;

		UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
		Map pluginViews = null;
		if (uiFunctions != null) {
			UISWTInstanceImpl pluginUI = uiFunctions.getSWTPluginInstanceImpl();

			if (pluginUI != null) {
				pluginViews = pluginUI.getViewListeners(sTableID);
				if (pluginViews != null)
					iNumViews += pluginViews.size();
			}
		}

		if (iNumViews == 0) {
			tableComposite = createMainPanel(composite);
			return tableComposite;
		}

		FormData formData;

		final Composite form = new Composite(composite, SWT.NONE);
		FormLayout flayout = new FormLayout();
		flayout.marginHeight = 0;
		flayout.marginWidth = 0;
		form.setLayout(flayout);
		GridData gridData;
		gridData = new GridData(GridData.FILL_BOTH);
		form.setLayoutData(gridData);

		// Create them in reverse order, so we can have the table auto-grow, and
		// set the tabFolder's height manually

		final int TABHEIGHT = 20;
		tabFolder = new CTabFolder(form, SWT.TOP | SWT.BORDER);
		tabFolder.setMinimizeVisible(true);
		tabFolder.setTabHeight(TABHEIGHT);
		final int iFolderHeightAdj = tabFolder.computeSize(SWT.DEFAULT, 0).y;

		final Sash sash = new Sash(form, SWT.HORIZONTAL);

		tableComposite = createMainPanel(form);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		tableComposite.setLayout(layout);

		// FormData for Folder
		formData = new FormData();
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		formData.bottom = new FormAttachment(100, 0);
		int iSplitAt = configMan.getIntParameter(sPropertiesPrefix + ".SplitAt",
				3000);
		// Was stored at whole
		if (iSplitAt < 100)
			iSplitAt *= 100;

		double pct = iSplitAt / 10000.0;
		if (pct < 0.03)
			pct = 0.03;
		else if (pct > 0.97)
			pct = 0.97;

		// height will be set on first resize call
		sash.setData("PCT", new Double(pct));
		tabFolder.setLayoutData(formData);
		final FormData tabFolderData = formData;

		// FormData for Sash
		formData = new FormData();
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		formData.bottom = new FormAttachment(tabFolder);
		formData.height = 5;
		sash.setLayoutData(formData);

		// FormData for table Composite
		formData = new FormData();
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		formData.top = new FormAttachment(0, 0);
		formData.bottom = new FormAttachment(sash);
		tableComposite.setLayoutData(formData);

		// Listeners to size the folder
		sash.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				final boolean FASTDRAG = true;

				if (FASTDRAG && e.detail == SWT.DRAG)
					return;

				if (tabFolder.getMinimized()) {
					tabFolder.setMinimized(false);
					refreshSelectedSubView();
					configMan.setParameter(sPropertiesPrefix + ".subViews.minimized", false);
				}

				Rectangle area = form.getClientArea();
				tabFolderData.height = area.height - e.y - e.height - iFolderHeightAdj;
				form.layout();

				Double l = new Double((double) tabFolder.getBounds().height
						/ form.getBounds().height);
				sash.setData("PCT", l);
				if (e.detail != SWT.DRAG)
					configMan.setParameter(sPropertiesPrefix + ".SplitAt", (int) (l
							.doubleValue() * 10000));
			}
		});

		final CTabFolder2Adapter folderListener = new CTabFolder2Adapter() {
			public void minimize(CTabFolderEvent event) {
				tabFolder.setMinimized(true);
				tabFolderData.height = iFolderHeightAdj;
				form.layout();
				
				configMan.setParameter(sPropertiesPrefix + ".subViews.minimized", true);
			}

			public void restore(CTabFolderEvent event) {
				tabFolder.setMinimized(false);
				form.notifyListeners(SWT.Resize, null);
				
				refreshSelectedSubView();

				configMan.setParameter(sPropertiesPrefix + ".subViews.minimized", false);
			}
		};
		tabFolder.addCTabFolder2Listener(folderListener);
		
		tabFolder.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				if (tabFolder.getMinimized()) {
					folderListener.restore(null);
					// If the user clicked down on the restore button, and we restore
					// before the CTabFolder does, CTabFolder will minimize us again
					// There's no way that I know of to determine if the mouse is 
					// on that button!
					
					// one of these will tell tabFolder to cancel
					e.button = 0;
					tabFolder.notifyListeners(SWT.MouseExit, null);
				}
			}
		});

		form.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event e) {
				if (tabFolder.getMinimized())
					return;

				Double l = (Double) sash.getData("PCT");
				if (l != null) {
					tabFolderData.height = (int) (form.getBounds().height * l
							.doubleValue())
							- iFolderHeightAdj;
					form.layout();
				}
			}
		});

		if (coreTabViews != null)
			for (int i = 0; i < coreTabViews.length; i++)
				addTabView(coreTabViews[i]);

		// Call plugin listeners
		if (pluginViews != null) {
			String[] sNames = (String[]) pluginViews.keySet().toArray(new String[0]);
			for (int i = 0; i < sNames.length; i++) {
				UISWTViewEventListener l = (UISWTViewEventListener) pluginViews
						.get(sNames[i]);
				if (l != null) {
					try {
						UISWTViewImpl view = new UISWTViewImpl(sTableID, sNames[i], l);
						addTabView(view);
					} catch (Exception e) {
						// skip, plugin probably specifically asked to not be added
					}
				}
			}
		}
		
		if (configMan.getBooleanParameter(
				sPropertiesPrefix + ".subViews.minimized", false)) {
			tabFolder.setMinimized(true);
			tabFolderData.height = iFolderHeightAdj;
		} else {
			tabFolder.setMinimized(false);
		}

		tabFolder.setSelection(0);

		return form;
	}
  
	
  /** Creates a composite within the specified composite and sets its layout
   * to a default FillLayout().
   *
   * @param composite to create your Composite under
   * @return The newly created composite
   */
  public Composite createMainPanel(Composite composite) {
    Composite panel = new Composite(composite,SWT.NULL);
    GridLayout layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    panel.setLayout(layout);

    return panel;
  }
    
  /** Creates the Table.
   *
   * @return The created Table.
   */
  public Table createTable(Composite panel) {
    table = new Table(panel, iTableStyle);
    table.setLayoutData(new GridData(GridData.FILL_BOTH));

    return table;
  }

  /** Sets up the sorter, columns, and context menu.
   *
   * @param table Table to be initialized
   */
  public void initializeTable(final Table table) {
    initializeColumnDefs();

    iTableStyle = table.getStyle();
    bTableVirtual = (iTableStyle & SWT.VIRTUAL) != 0;

    table.setLinesVisible(Utils.TABLE_GRIDLINE_IS_ALTERNATING_COLOR);
    table.setMenu(menu);
    table.setData("Name", sTableID);
    table.setData("TableView", this);

    // Setup table
    // -----------

    // XXX On linux (an other OSes?), changing the column indicator doesn't 
    //     work until the table is shown.  Since SWT.Show doesn't trigger,
    //     use the first paint trigger.
    if (!Utils.SWT32_TABLEPAINT) {

			table.addPaintListener(new PaintListener() {
				boolean first = true;

				public void paintControl(PaintEvent event) {
					if (first) {
						changeColumnIndicator();
						// This fixes the scrollbar not being long enough on Win2k
						// There may be other methods to get it to refresh right, but
						// layout(true, true) didn't work.
						table.setRedraw(false);
						table.setRedraw(true);
						first = false;
					}
					if (event.width == 0 || event.height == 0)
						return;
					doPaint(event.gc);
					visibleRowsChanged();
				}
			});
		}

    if (Utils.SWT32_TABLEPAINT) {
  		// SWT 3.2 only.  Code Ok -- Only called in SWT 3.2 mode
			table.addListener(SWT.PaintItem, new Listener() {
				public void handleEvent(Event event) {
					paintItem(event);
				}
			});
			table.addListener(SWT.EraseItem, new Listener() {
				public void handleEvent(Event event) {
				}
			});
		}

    // Deselect rows if user clicks on a black spot (a spot with no row)
    table.addMouseListener(new MouseAdapter() {
    	private TableCellMouseEvent createMouseEvent(TableCellCore cell,
					MouseEvent e, int type) {
				TableCellMouseEvent event = new TableCellMouseEvent();
				event.cell = cell;
				event.eventType = type;
				event.button = e.button;
				// TODO: Change to not use SWT masks
				event.keyboardState = e.stateMask;
				event.skipCoreFunctionality = false;
				Rectangle r = cell.getBounds();
				event.x = e.x - r.x + VerticalAligner.getTableAdjustHorizontallyBy(table);
				event.y = e.y - r.y + VerticalAligner.getTableAdjustVerticalBy(table);
				return event;
    	}
    	
    	public void mouseDoubleClick(MouseEvent e) {
    		TableColumnCore tc = getTableColumnByOffset(e.x);
				TableCellCore cell = getTableCell(e.x, e.y);
				if (cell != null && tc != null) {
					TableCellMouseEvent event = createMouseEvent(cell, e,
							TableCellMouseEvent.EVENT_MOUSEDOUBLECLICK);
					tc.invokeCellMouseListeners(event);
					cell.invokeMouseListeners(event);
					if (event.skipCoreFunctionality)
						lCancelSelectionTriggeredOn = System.currentTimeMillis();
				}
    	}

    	public void mouseUp(MouseEvent e) {
    		TableColumnCore tc = getTableColumnByOffset(e.x);
				TableCellCore cell = getTableCell(e.x, e.y);
				if (cell != null && tc != null) {
					TableCellMouseEvent event = createMouseEvent(cell, e,
							TableCellMouseEvent.EVENT_MOUSEUP);
					tc.invokeCellMouseListeners(event);
					cell.invokeMouseListeners(event);
					if (event.skipCoreFunctionality)
						lCancelSelectionTriggeredOn = System.currentTimeMillis();
				}
    	}

      public void mouseDown(MouseEvent e) {
    		TableColumnCore tc = getTableColumnByOffset(e.x);
				TableCellCore cell = getTableCell(e.x, e.y);
				if (cell != null && tc != null) {
	      	if (e.button == 2 && e.stateMask == SWT.CONTROL) {
	      		((TableCellImpl)cell).bDebug = !((TableCellImpl)cell).bDebug;
	      		System.out.println("Set debug for " + cell + " to "
								+ ((TableCellImpl) cell).bDebug);
	      	}
					TableCellMouseEvent event = createMouseEvent(cell, e,
							TableCellMouseEvent.EVENT_MOUSEDOWN);
					tc.invokeCellMouseListeners(event);
					cell.invokeMouseListeners(event);
					if (event.skipCoreFunctionality)
						lCancelSelectionTriggeredOn = System.currentTimeMillis();
				}

        iMouseX = e.x;
        try {
          if (table.getItemCount() <= 0)
            return;

          // skip if outside client area (ie. scrollbars)
          Rectangle rTableArea = table.getClientArea();
          //System.out.println("Mouse="+iMouseX+"x"+e.y+";TableArea="+rTableArea);
          Point pMousePosition = new Point(e.x, e.y);
          if (rTableArea.contains(pMousePosition)) {
          	int[] columnOrder = table.getColumnOrder();
          	if (columnOrder.length == 0) {
          		return;
          	}
						TableItem ti = table.getItem(columnOrder[columnOrder.length - 1]);
            Rectangle cellBounds = ti.getBounds(columnOrder[columnOrder.length - 1]);
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
          // Bug 103934: Table.getItem(Point) uses incorrect calculation on Motif
          //             Fixed 20050718 SWT 3.2M1 (3201) & SWT 3.1.1 (3139)
          // TODO: Get Build IDs and use this code (if it works)
            TableItem ti = table.getItem(pMousePosition);
            if (ti == null)
              table.deselectAll();
*/
          }
        } catch (Exception ex) {
          System.out.println("MouseDownError");
          Debug.printStackTrace( ex );
        }
      }
    });
    
    table.addMouseMoveListener(new MouseMoveListener() {
    	TableCellCore lastCell = null;
    	int lastCursorID = -1;
    	
      public void mouseMove(MouseEvent e) {
        // XXX this may not be needed if all platforms process mouseDown
        //     before the menu
        iMouseX = e.x;
        
				TableCellCore cell = getTableCell(e.x, e.y);
				int iCursorID = -1;
				if (cell != lastCell) {
					iCursorID = cell.getCursorID();
					cell = lastCell;
				}

				if (iCursorID != lastCursorID) {
					lastCursorID = iCursorID;

					if (iCursorID >= 0) {
						table.setCursor(table.getDisplay().getSystemCursor(iCursorID));
					} else {
						table.setCursor(null);
					}
				}
      }
    });

    table.addSelectionListener(new SelectionListener() {
      public void widgetSelected(SelectionEvent event) {
      	if (tabViews == null && tabViews.size() == 0)
      		return;

      	// Set Data Object for all tabs.  Tabs of PluginView are sent the plugin
      	// Peer object, while Tabs of IView are sent the core PEPeer object.

      	// TODO: Send all datasources
      	Object[] dataSourcesCore = getSelectedDataSources(true);
      	Object[] dataSourcesPlugin = null;

      	for (int i = 0; i < tabViews.size(); i++) {
      		IView view = (IView) tabViews.get(i);
      		if (view != null) {
      			if (view instanceof UISWTViewImpl) {
      				if (dataSourcesPlugin == null)
      					dataSourcesPlugin = getSelectedDataSources(false);

      				((UISWTViewImpl) view)
									.dataSourceChanged(dataSourcesPlugin.length == 0 ? null
											: dataSourcesPlugin);
      			} else {
      				view.dataSourceChanged(dataSourcesCore.length == 0 ? null
									: dataSourcesCore);
      			}
      		}
      	}
      }

			public void widgetDefaultSelected(SelectionEvent e) {
				if (lCancelSelectionTriggeredOn > 0
						&& System.currentTimeMillis() - lCancelSelectionTriggeredOn < 200) {
					e.doit = false;
					lCancelSelectionTriggeredOn = -1;
				} else {
					runDefaultAction();
				}
			}
    });
    
    // we are sent a SWT.Settings event when the language changes and
    // when System fonts/colors change.  In both cases, invalidate
    if (SWT.getVersion() > 3200) {
	    table.addListener(SWT.Settings, new Listener() {
	      public void handleEvent(Event e) {
	      	tableInvalidate();
	      }
	    });
    }

    // XXX Disabled.  We handle unset rows ourselves via table paints which
    //     are more reliable.
    if (bTableVirtual || false)
	    table.addListener(SWT.SetData, new Listener() {
	      public void handleEvent(Event e) {
					final TableItem item = (TableItem) e.item;
		  		// This is catch is temporary for SWT 3212, because there are cases where
		  		// it says it isn't disposed, when it really almost is
		  		try {
		  			if (item.getData("SD") != null) {
		  				return;
		  			}
						item.setData("SD", "1");
		  		} catch (NullPointerException badSWT) {
		  			return;
		  		}
	
					int tableIndex = table.indexOf(item);
					if (tableIndex < 0) {
						System.out.println("XXX TI < 0!!");
						return;
					}
					
					TableRowCore row = (TableRowCore) item.getData("TableRow");
					if (row == null || row.getIndex() != tableIndex) {
						//System.out.println("SetData " + tableIndex + ": Sort..");
						fillRowGaps(false);
	
						row = (TableRowCore) item.getData("TableRow");
						if (row == null || row.getIndex() != tableIndex) {
							// row's been deleted.  tableitem probably about to be remove
							// (hopefully!)
							if (DEBUGADDREMOVE)
								Debug.outStackTrace();
							return;
						}
					} else {
						//System.out.println("SetData " + tableIndex + ": invalidate");
						row.invalidate();
					}
					
					// User made the row visible, they want satisfaction now!
					if (!row.setIconSize(ptIconSize)) {
						row.refresh(true, true);
					}

					if (!Utils.TABLE_GRIDLINE_IS_ALTERNATING_COLOR) {
						Utils.alternateRowBackground(item);
						// Bug, background color doesn't fully draw in SetData
						Rectangle r = item.getBounds(0);
						table.redraw(0, r.y, table.getClientArea().width, r.height, false);
					}
				}
	    });

    // bypasses disappearing graphic glitch on Mac OS X
/* Temporarily Disabled to see if we need it anymore
    if(Constants.isOSX) {
        table.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected( SelectionEvent event) {
                GroupTableRowRunner refresher = new GroupTableRowRunner() {
                  public void run(TableRowCore row) {
                      row.setValid(false);
                      row.refresh(true);
                  }
                };

                TableItem[] sel = table.getSelection();

                ArrayList toRefresh = new ArrayList(sel.length);

                if(oldSelectedItems != null) {
                    runForTableItems(oldSelectedItems, refresher);
                    for (int i = 0; i < sel.length; i++) {
                        if(!oldSelectedItems.contains(sel[i]))
                            toRefresh.add(sel[i]);
                    }
                }
                else {
                    for (int i = 0; i < sel.length; i++) {
                        toRefresh.add(sel[i]);
                    }
                }

                runForTableItems(toRefresh, refresher);

                oldSelectedItems = toRefresh;
            }
        });
    }
*/

    new TableTooltips(table);
  	
  	table.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent event) {
				if (event.keyCode == SWT.F5) { 
					if ((event.stateMask & SWT.SHIFT) > 0) {
      			runForSelectedRows(new GroupTableRowRunner() {
							public void run(TableRowCore row) {
								row.invalidate();
								row.refresh(true);
							}
						});
					} else {
						sortColumn(true);
					}
					event.doit = false;
				}
			}
  	});
  	
  	ScrollBar bar = table.getVerticalBar();
  	if (bar != null) {
  		bar.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					// Bug: Scroll is slow when table is not focus
					if (!table.isFocusControl()) {
						table.setFocus();
					}
				}
  		});
		}
  	
    table.setHeaderVisible(true);

    initializeTableColumns(table);
  }
  
  /**
	 * @param event
	 */
	protected void paintItem(Event event) {
		TableItem item = (TableItem) event.item;
		if (item == null || item.isDisposed()) {
			return;
		}

		TableRowCore row;
		try {
			row = (TableRowCore) item.getData("TableRow");
		} catch (NullPointerException e) {
			return;
		}

		// SWT 3.2 only.  Code Ok -- Only called in SWT 3.2 mode
		Rectangle cellBounds = item.getBounds(event.index);
		
		cellBounds.x += 3;
		cellBounds.width -= 6;
		
		try {
			// SWT 3.2 only.  Code Ok -- Only called in SWT 3.2 mode
			int iColumnNo = event.index;
			
			if (item.getImage(iColumnNo) != null) {
				cellBounds.x += 18;
				cellBounds.width -= 18;
			}
			
			if (cellBounds.width <= 0 || cellBounds.height <= 0) {
				return;
			}

			if (bSkipFirstColumn) {
				if (iColumnNo == 0) {
					return;
				}
				iColumnNo--;
			}
			
			if (iColumnNo >= columnsOrdered.length) {
				System.out.println(iColumnNo + " >= " + columnsOrdered.length);
				return;
			}
			
			TableCellCore cell = row.getTableCellCore(columnsOrdered[iColumnNo].getName());
			
			if (!cell.isUpToDate()) {
				//System.out.println("R " + table.indexOf(item));
				cell.refresh(true, true);
				return;
			}
			
			//System.out.println("PS " + table.indexOf(item) + ";" + cellBounds);
			GCStringPrinter.printString(event.gc, cell.getText(),
					cellBounds, true, true, columnsOrdered[iColumnNo].getSWTAlign());

			if (cell.needsPainting()) {
				cell.doPaint(event.gc);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void runDefaultAction() {
  	
  }

  protected void initializeTableColumns(final Table table) {
  	TableColumn[] oldColumns = table.getColumns();

    if (SWT.getVersion() >= 3100)
    	for (int i = 0; i < oldColumns.length; i++)
    		oldColumns[i].removeListener(SWT.Move, columnMoveListener);

  	for (int i = oldColumns.length - 1; i >= 0 ; i--)
  		oldColumns[i].dispose();
  	
    // Pre 3.0RC1 SWT on OSX doesn't call this!! :(
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

    // Add 1 to position because we make a non resizable 0-sized 1st column
    // to fix the 1st column gap problem (Eclipse Bug 43910)

    // SWT does not set 0 column width as expected in OS X; see bug 43910
    // this will be removed when a SWT-provided solution is available to satisfy all platforms with identation issue
    bSkipFirstColumn = bSkipFirstColumn && !Constants.isOSX;

    if (bSkipFirstColumn) {
      TableColumn tc = new TableColumn(table, SWT.NULL);
      tc.setWidth(0);
      tc.setResizable(false);
    }

    TableColumnCore[] tmpColumnsOrdered = new TableColumnCore[tableColumns.length];
    //Create all columns
    for (int i = 0; i < tableColumns.length; i++) {
      int position = tableColumns[i].getPosition();
      if (position != -1) {
        new TableColumn(table, SWT.NULL);
        tmpColumnsOrdered[position] = tableColumns[i];
      }
    }
    int numSWTColumns = table.getColumnCount();
    int iNewLength = numSWTColumns - (bSkipFirstColumn ? 1 : 0);
    columnsOrdered = new TableColumnCore[iNewLength];
    System.arraycopy(tmpColumnsOrdered, 0, columnsOrdered, 0, iNewLength);
    
    ColumnSelectionListener columnSelectionListener = new ColumnSelectionListener();
    
    //Assign length and titles
    //We can only do it after ALL columns are created, as position (order)
    //may not be in the natural order (if the user re-order the columns).
    for (int i = 0; i < tableColumns.length; i++) {
      int position = tableColumns[i].getPosition();
      if (position == -1)
        continue;

      String sName = tableColumns[i].getName();
      // +1 for Eclipse Bug 43910 (see above)
      // user has reported a problem here with index-out-of-bounds - not sure why
      // but putting in a preventative check so that hopefully the view still opens
      // so they can fix it
      
      int	adjusted_position = position + (bSkipFirstColumn ? 1 : 0);
      
      if (adjusted_position >= numSWTColumns) {
				Debug.out("Incorrect table column setup, skipping column '" + sName
						+ "', position=" + adjusted_position + ";numCols=" + numSWTColumns);
				continue;
			}
      
      TableColumn column = table.getColumn(adjusted_position);
      try {
      	column.setMoveable(true);
      } catch (NoSuchMethodError e) {
      	// Ignore < SWT 3.1
      }
      column.setAlignment(tableColumns[i].getSWTAlign());
      Messages.setLanguageText(column, tableColumns[i].getTitleLanguageKey());
      column.setWidth(tableColumns[i].getWidth());
      column.setData("TableColumnCore", tableColumns[i]);
      column.setData("configName", "Table." + sTableID + "." + sName);
      column.setData("Name", sName);
      
      column.addControlListener(resizeListener);
      // At the time of writing this SWT (3.0RC1) on OSX doesn't call the 
      // selection listener for tables
      column.addListener(SWT.Selection, columnSelectionListener);
    }

    // Initialize the sorter after the columns have been added
		String sSortColumn = configMan.getStringParameter(sTableID
				+ ".sortColumn", sDefaultSortOn);
		int iSortDirection = configMan.getIntParameter(CFG_SORTDIRECTION);
		boolean bSortAscending = configMan.getBooleanParameter(sTableID
				+ ".sortAsc", iSortDirection == 1 ? false : true);

    TableColumnManager tcManager = TableColumnManager.getInstance();
    TableColumnCore tc = tcManager.getTableColumnCore(sTableID, sSortColumn);
    if (tc == null) {
    	tc = tableColumns[0];
    }
		rowSorter = new TableRowComparator(tc, bSortAscending);
		changeColumnIndicator();
		
    // Add move listener at the very end, so we don't get a bazillion useless 
    // move triggers
    if (SWT.getVersion() >= 3100) {
			Listener columnResizeListener = (!COLUMN_CLICK_DELAY) ? null
					: new Listener() {
						public void handleEvent(Event event) {
							lLastColumnResizeOn = System.currentTimeMillis();
						}
					};
    	
	    for (int i = 0; i < tableColumns.length; i++) {
	      int position = tableColumns[i].getPosition();
	      if (position == -1)
	        continue;
	
	      int	adjusted_position = position + (bSkipFirstColumn ? 1 : 0);
	      if (adjusted_position >= numSWTColumns)
	      	continue;
	      
	      TableColumn column = table.getColumn(adjusted_position);
	      column.addListener(SWT.Move, columnMoveListener);
	    	if (COLUMN_CLICK_DELAY)
	    		column.addListener(SWT.Resize, columnResizeListener);
	    }
    }
  }

  /** Creates the Context Menu.
   *
   * @return a new Menu object
   */
  public Menu createMenu() {
    final Menu menu = new Menu(tableComposite.getShell(), SWT.POP_UP);
    menu.addMenuListener(new MenuListener() {
    	boolean bShown = false;
    	
			public void menuHidden(MenuEvent e) {
				bShown = false;

				if (Constants.isOSX)
					return;

				// Must dispose in an asyncExec, otherwise SWT.Selection doesn't
				// get fired (async workaround provided by Eclipse Bug #87678)
				e.widget.getDisplay().asyncExec(new AERunnable() {
					public void runSupport() {
						if (bShown || menu.isDisposed())
							return;
						MenuItem[] items = menu.getItems();
						for (int i = 0; i < items.length; i++) {
							items[i].dispose();
						}
					}
				});
			}

			public void menuShown(MenuEvent e) {
				MenuItem[] items = menu.getItems();
				for (int i = 0; i < items.length; i++)
					items[i].dispose();

        bShown = true;

      	fillMenu(menu);
        addThisColumnSubMenu(getColumnNo(iMouseX));
			}
    });

    return menu;
  }

  /** Fill the Context Menu with items.  Called when menu is about to be shown.
   *
   * By default, a "Edit Columns" menu and a Column specific menu is set up.
   *
   * @param menu Menu to fill
   */
  public void fillMenu(Menu menu) {
    final MenuItem itemChangeTable = new MenuItem(menu, SWT.PUSH);
    Messages.setLanguageText(itemChangeTable, "MyTorrentsView.menu.editTableColumns");
    Utils.setMenuItemImage(itemChangeTable, "columns");
    
    itemChangeTable.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
      	new TableColumnEditorWindow(table.getShell(), tableColumns,
						TableStructureEventDispatcher.getInstance(sTableID));
      }
    });
    
    menuThisColumn = new Menu(tableComposite.getShell(), SWT.DROP_DOWN);
    final MenuItem itemThisColumn = new MenuItem(menu, SWT.CASCADE);
    itemThisColumn.setMenu(menuThisColumn);

    // Add Plugin Context menus..
 		boolean	enable_items = table != null && table.getSelection().length > 0;
    
    TableContextMenuItem[] items = TableContextMenuManager.getInstance()
				.getAllAsArray(sTableID);
		if (items.length > 0) {
			new MenuItem(menu, SWT.SEPARATOR);

			boolean prev_was_separator = true;

			for (int i = 0; i < items.length; i++) {
				final TableContextMenuItemImpl contextMenuItem = (TableContextMenuItemImpl) items[i];

				final int style = contextMenuItem.getStyle();

				final int swt_style;

				boolean this_is_separator = false;

				if (style == TableContextMenuItem.STYLE_PUSH) {
					swt_style = SWT.PUSH;
				} else if (style == TableContextMenuItem.STYLE_CHECK) {
					swt_style = SWT.CHECK;
				} else if (style == TableContextMenuItem.STYLE_RADIO) {
					swt_style = SWT.RADIO;
				} else if (style == TableContextMenuItem.STYLE_SEPARATOR) {
					this_is_separator = true;
					swt_style = SWT.SEPARATOR;
				} else {
					swt_style = SWT.PUSH;
				}

				// skip contiguous separators

				if (prev_was_separator && this_is_separator) {
					continue;
				}

				// skip trailing separator

				if (this_is_separator && i == items.length - 1) {
					continue;
				}

				prev_was_separator = this_is_separator;

				final MenuItem menuItem = new MenuItem(menu, swt_style);

				if (swt_style == SWT.SEPARATOR) {
					continue;
				}

				Messages.setLanguageText(menuItem, contextMenuItem.getResourceKey());

				menuItem.addListener(SWT.Selection, new SelectedTableRowsListener() {
					public void run(TableRowCore row) {
						if (swt_style == SWT.CHECK || swt_style == SWT.RADIO) {

							contextMenuItem.setData(new Boolean(menuItem.getSelection()));
						}

						contextMenuItem.invokeListeners(row);
					}
				});

				if (enable_items) {
					contextMenuItem.invokeMenuWillBeShownListeners(getSelectedRows());

					if (style == TableContextMenuItem.STYLE_CHECK
							|| style == TableContextMenuItem.STYLE_RADIO) {

						menuItem.setSelection(((Boolean) contextMenuItem.getData())
								.booleanValue());
					}
				}

				Graphic g = contextMenuItem.getGraphic();
				if (g instanceof UISWTGraphic) {
					Utils.setMenuItemImage(menuItem, ((UISWTGraphic) g).getImage());
				}

				menuItem.setEnabled(enable_items && contextMenuItem.isEnabled());
			}
    }
  }
  
  /**
   * SubMenu for column specific tasks. 
   *
   * @param iColumn Column # that tasks apply to.
   */
  private void addThisColumnSubMenu(int iColumn) {
    MenuItem item;

    if (menuThisColumn == null || menuThisColumn.isDisposed())
    	return;

    // Dispose of the old items
    MenuItem[] oldItems = menuThisColumn.getItems();
    for (int i = 0; i < oldItems.length; i++) {
      oldItems[i].dispose();
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
        new Clipboard(mainComposite.getDisplay()).setContents(new Object[] { sToClipboard }, 
                                                      new Transfer[] {TextTransfer.getInstance()});
      }
    });

    // Add Plugin Context menus..
    TableColumnCore tc = (TableColumnCore)tcColumn.getData("TableColumnCore");
    TableContextMenuItem[] items = tc.getContextMenuItems();
    if (items.length > 0) {
      new MenuItem(menuThisColumn, SWT.SEPARATOR);

      for (int i = 0; i < items.length; i++) {
        final TableContextMenuItemImpl contextMenuItem = (TableContextMenuItemImpl)items[i];
        final MenuItem menuItem = new MenuItem(menuThisColumn, SWT.PUSH);

        Messages.setLanguageText(menuItem, contextMenuItem.getResourceKey());
        menuItem.addListener(SWT.Selection, new SelectedTableRowsListener() {
          public void run(TableRowCore row) {
            contextMenuItem.invokeListeners(row);
          }
        });
      }
    }
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
    return mainComposite;
  }
  
  public Composite getTableComposite() {
    return tableComposite;
  }

  /** IView.refresh(), called when the GUI needs an update */
  public final void refresh() {
  	refresh(false);
  }

  long count = 0;

  public void refresh(boolean bForceSort) {
  	// don't refresh while there's no table
  	if (table == null)
  		return;
  	
  	refreshTable(bForceSort);

  	if (bEnableTabViews && tabFolder != null && !tabFolder.isDisposed()
				&& !tabFolder.getMinimized())
  		refreshSelectedSubView();
  	// TODO: Refresh folder titles
  }
  
  public IView getActiveSubView() {
  	if (!bEnableTabViews || tabFolder == null || tabFolder.isDisposed()
				|| tabFolder.getMinimized())
  		return null;

		CTabItem item = tabFolder.getSelection();
		if (item != null) {
			return (IView)item.getData("IView");
		}

		return null;
  }
  
	public void refreshSelectedSubView() {
		IView view = getActiveSubView();
		if (view != null)
			view.refresh();
	}

  public void refreshTable(boolean bForceSort) {
  	// don't refresh while there's no table
  	if (table == null)
  		return;

  	try{
  		this_mon.enter();

	    if(getComposite() == null || getComposite().isDisposed())
	      return;
	
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
	
	    long lTimeStart = System.currentTimeMillis();
	    
	    count = 0;
	    
	    final boolean bDoGraphics = (loopFactor % graphicsUpdate) == 0;
	    final boolean bWillSort = bForceSort || (reOrderDelay != 0) && ((loopFactor % reOrderDelay) == 0); 
	    //System.out.println("Refresh.. WillSort? " + bWillSort);
	    
			if (bWillSort) {
				if (bForceSort) {
					rowSorter.getColumn().setLastSortValueChange(SystemTime.getCurrentTime());
				}
				sortColumn(true);
			}
	
	    lTimeStart = System.currentTimeMillis();
	    
	    //Refresh all visible items in table...
	    runForAllRows(new GroupTableRowVisibilityRunner() {
	      public void run(TableRowCore row, boolean bVisible) {
      		row.refresh(bDoGraphics, bVisible);
	      }
	    });

			if (DEBUGADDREMOVE) {
		    long lTimeDiff = (System.currentTimeMillis() - lTimeStart);
				if (lTimeDiff > 500)
					debug(lTimeDiff + "ms to refresh rows");
			}
	
	    loopFactor++;
  	}finally{
  		
  		this_mon.exit();
  	}
  }
  
  private void refreshVisibleRows() {
    if (getComposite() == null || getComposite().isDisposed())
      return;    
    
    runForVisibleRows(new GroupTableRowRunner() {
      public void run(TableRowCore row) {
        row.setAlternatingBGColor(true);
        row.refresh(false, true);
      }
    });
  }
  
  
  /**
   * Process the queue of datasources to be added and removed
   *
   */
  public void processDataSourceQueue() {
		Object[] dataSourcesAdd = null;
		Object[] dataSourcesRemove = null;

		try {
			dataSourceToRow_mon.enter();
			if (dataSourcesToAdd.size() > 0) {
				dataSourcesAdd = dataSourcesToAdd.toArray();
				dataSourcesToAdd.clear();

				// remove the ones we are going to add then delete
				if (dataSourcesToRemove.size() > 0) {
					for (int i = 0; i < dataSourcesAdd.length; i++)
						if (dataSourcesToRemove.contains(dataSourcesAdd[i])) {
							dataSourcesToRemove.remove(dataSourcesAdd[i]);
							dataSourcesAdd[i] = null;
							if (DEBUGADDREMOVE)
								debug("Saved time by not adding a row that was removed");
						}
				}
			}

			if (dataSourcesToRemove.size() > 0) {
				dataSourcesRemove = dataSourcesToRemove.toArray();
				if (DEBUGADDREMOVE && dataSourcesRemove.length > 1)
					debug("Streamlining removing " + dataSourcesRemove.length + " rows");
				dataSourcesToRemove.clear();
			}
		} finally {
			dataSourceToRow_mon.exit();
		}

		if (dataSourcesAdd != null && dataSourcesAdd.length > 0) {
			reallyAddDataSources(dataSourcesAdd);
			if (DEBUGADDREMOVE && dataSourcesAdd.length > 1)
				debug("Streamlined adding " + dataSourcesAdd.length + " rows");
		}

		if (dataSourcesRemove != null && dataSourcesRemove.length > 0) {
			reallyRemoveDataSources(dataSourcesRemove);
		}
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
    
    runForVisibleRows(new GroupTableRowRunner() {
      public void run(TableRowCore row) {
        row.doPaint(gc, true);
      }
    });
  }

  /** IView.delete: This method is called when the view is destroyed.
   * Each color instanciated, images and such things should be disposed.
   * The caller is the GUI thread.
   */
  public void delete() {
    if (tabViews != null && tabViews.size() > 0) {
			for (int i = 0; i < tabViews.size(); i++) {
				IView view = (IView)tabViews.get(i);
				if (view != null)
					view.delete();
			}
    }

    TableStructureEventDispatcher.getInstance(sTableID).removeListener(this);
    if (tableColumns != null)
	    for (int i = 0; i < tableColumns.length; i++)
	      tableColumns[i].saveSettings();

    if (table != null && !table.isDisposed())
      table.dispose();
    removeAllTableRows();
    configMan.removeParameterListener("ReOrder Delay", this);
    configMan.removeParameterListener("Graphics Update", this);
    Colors.getInstance().removeColorsChangedListener(this);

    //oldSelectedItems =  null;
    super.delete();
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

  /* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.views.AbstractIView#updateLanguage()
	 */
	public void updateLanguage() {
		super.updateLanguage();
		
    if (tabViews != null && tabViews.size() > 0) {
			for (int i = 0; i < tabViews.size(); i++) {
				IView view = (IView)tabViews.get(i);
				if (view != null)
					view.updateLanguage();
			}
    }
	}

	/** Adds a dataSource to the table as a new row.  If the data source is
   * already added, a new row will not be added.  This function runs 
   * asynchronously, so the rows creation is not guaranteed directly after
   * calling this function.
   *
   * You can't add datasources until the table is initialized
   * 
   * @param dataSource data source to add to the table
   * @param bImmediate Add immediately, or queue and add at next refresh
   */
  
  public void addDataSource(Object dataSource) {
  	addDataSources(new Object[] { dataSource});
	}

  /**
   * Add a list of dataSources to the table.  The array passed in may be 
   * modified, so make sure you don't need it afterwards.
   * 
   * You can't add datasources until the table is initialized
   * 
   * @param dataSources
   * @param bImmediate Add immediately, or queue and add at next refresh
   */
  public void addDataSources(final Object dataSources[]) {

		if (dataSources == null)
			return;

		if (IMMEDIATE_ADDREMOVE_DELAY == 0) {
			reallyAddDataSources(dataSources);
			return;
		}

		// In order to save time, we cache entries to be added and process them
		// in a refresh cycle.  This is a huge benefit to tables that have
		// many rows being added and removed in rapid succession

		try {
			dataSourceToRow_mon.enter();

			for (int i = 0; i < dataSources.length; i++) {
				if (dataSources[i] != null) {
					dataSourcesToAdd.add(dataSources[i]);
				}
			}

			if (DEBUGADDREMOVE)
				debug("Queued " + dataSources.length
						+ " dataSources to add.  Total Queued: " + dataSourcesToAdd.size());

		} finally {

			dataSourceToRow_mon.exit();
		}

		refreshenProcessDataSourcesTimer();
  }
		
	private void refreshenProcessDataSourcesTimer() {
		if (bReallyAddingDataSources) {
			return;
		}

		synchronized (timerProcessDataSources) {
			if (timerEventProcessDS != null && !timerEventProcessDS.hasRun()) {
				// Push timer forward, unless we've pushed it forward for over x seconds
				long now = SystemTime.getCurrentTime();
				if (now - timerEventProcessDS.getCreatedTime() < IMMEDIATE_ADDREMOVE_MAXDELAY) {
					long lNextTime = now + IMMEDIATE_ADDREMOVE_DELAY;
					timerProcessDataSources.adjustAllBy(lNextTime
							- timerEventProcessDS.getWhen());
				} else {
					timerEventProcessDS.cancel();
					timerEventProcessDS = null;
					if (DEBUGADDREMOVE) {
						debug("Over immediate delay limit, processing queue now");
					}
					
					processDataSourceQueue();
				}
			} else {
				timerEventProcessDS = timerProcessDataSources.addEvent(
						SystemTime.getCurrentTime() + IMMEDIATE_ADDREMOVE_DELAY,
						new TimerEventPerformer() {
							public void perform(TimerEvent event) {
								timerEventProcessDS = null;
								processDataSourceQueue();
							}
						});
			}
		}
	}
  

	private void reallyAddDataSources(final Object dataSources[]) {
  	
		if (mainComposite == null || table == null || mainComposite.isDisposed()
				|| table.isDisposed())
			return;
		
		bReallyAddingDataSources = true;
		if (DEBUGADDREMOVE)
			debug(">>" + " Add " + dataSources.length + " rows;");

		Object[] remainingDataSources = null;
		Object[] doneDataSources = dataSources;
		
		// Create row, and add to map immediately
		try {
			dataSourceToRow_mon.enter();
			
			long lStartTime = SystemTime.getCurrentTime();

			for (int i = 0; i < dataSources.length; i++) {
				if (dataSources[i] == null)
					continue;

				// Break off and add the rows to the UI if we've taken too long to
				// create them
				if (SystemTime.getCurrentTime() - lStartTime > 500) {
					int iNewSize = dataSources.length - i;
					if (DEBUGADDREMOVE) {
						debug("Breaking off adding datasources to map after "
							+ i + " took " + (SystemTime.getCurrentTime() - lStartTime)
							+ "ms; # remaining: " + iNewSize);
					}
					remainingDataSources = new Object[iNewSize];
					doneDataSources = new Object[i];
					System.arraycopy(dataSources, i, remainingDataSources, 0, iNewSize);
					System.arraycopy(dataSources, 0, doneDataSources, 0, i);
					break;
				}

				if (dataSourceToRow.containsKey(dataSources[i])) {
					dataSources[i] = null;
				} else {
					TableRowImpl row = new TableRowImpl(table, sTableID, columnsOrdered,
							dataSources[i], bSkipFirstColumn);
					dataSourceToRow.put(dataSources[i], row);
				}
			}
		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "Error while added row to Table "
					+ sTableID, e));
		} finally {
			dataSourceToRow_mon.exit();
		}
		
		if (DEBUGADDREMOVE)
			debug("--" + " Add " + doneDataSources.length + " rows;");

		if (remainingDataSources == null) {
			addDataSourcesToSWT(doneDataSources, true);
		} else {
			addDataSourcesToSWT(doneDataSources, false);
			reallyAddDataSources(remainingDataSources);
		}
	}
	
	private void addDataSourcesToSWT(final Object dataSources[], boolean async) {
		try {
			if (async) {
				table.getDisplay().asyncExec(new AERunnable() {
					public void runSupport() {
						_addDataSourcesToSWT(dataSources);
					}
				});
			} else {
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						_addDataSourcesToSWT(dataSources);
					}
				}, false);
			}
		} catch (Exception e) {
			bReallyAddingDataSources = false;
			e.printStackTrace();
		}
	}
	
	private void _addDataSourcesToSWT(final Object dataSources[]) {
		if (table == null || table.isDisposed()) {
			bReallyAddingDataSources = false;
			return;
		}

		boolean bBrokeEarly = false;
		try {
			dataSourceToRow_mon.enter();
			sortedRows_mon.enter();

			// purposefully not included in time check 
			table.setItemCount(sortedRows.size() + dataSources.length);

			long lStartTime = SystemTime.getCurrentTime();

			// add to sortedRows list in best position.  
			// We need to be in the SWT thread because the rowSorter may end up
			// calling SWT objects.
			for (int i = 0; i < dataSources.length; i++) {
				Object dataSource = dataSources[i];
				if (dataSource == null)
					continue;

				// If we've been processing on the SWT thread for too long,
				// break off and allow SWT a breather to update.
				if (SystemTime.getCurrentTime() - lStartTime > 2000) {
					int iNewSize = dataSources.length - i;
					if (DEBUGADDREMOVE) {
						debug("Breaking off adding datasources to SWT after " + i
								+ " took " + (SystemTime.getCurrentTime() - lStartTime)
								+ "ms; # remaining: " + iNewSize);
					}
					Object[] remainingDataSources = new Object[iNewSize];
					System.arraycopy(dataSources, i, remainingDataSources, 0, iNewSize);
					addDataSourcesToSWT(remainingDataSources, true);
					bBrokeEarly = true;
					break;
				}

				TableRowImpl row = (TableRowImpl) dataSourceToRow.get(dataSource);
				if (row == null || row.getIndex() >= 0)
					continue;
				TableCellCore cell = row.getTableCellCore(rowSorter.getColumnName());
				if (cell != null) {
					try {
						cell.invalidate();
						cell.refresh(true);
					} catch (Exception e) {
						Logger.log(new LogEvent(LOGID, "Minor error adding a row to table "
								+ sTableID, e));
					}
				}

				try {
					int index = 0;
					if (sortedRows.size() > 0) {
						// If we are >= to the last item, then just add it to the end
						// instead of relying on binarySearch, which may return an item
						// in the middle that also is equal.
						TableRowCore lastRow = (TableRowCore) sortedRows.get(sortedRows.size() - 1);
						if (rowSorter.compare(row, lastRow) >= 0) {
							index = sortedRows.size();
							sortedRows.add(row);
							if (DEBUGADDREMOVE)
								debug("Adding new row to bottom");
						} else {
							index = Collections.binarySearch(sortedRows, row, rowSorter);
							if (index < 0)
								index = -1 * index - 1; // best guess

							if (index > sortedRows.size())
								index = sortedRows.size();

							if (DEBUGADDREMOVE)
								debug("Adding new row at position " + index + " of "
										+ (sortedRows.size() - 1));
							sortedRows.add(index, row);
						}
					} else {
						if (DEBUGADDREMOVE)
							debug("Adding new row to bottom (1st Entry)");
						index = sortedRows.size();
						sortedRows.add(row);
					}

					row.setTableItem(index);
					row.setIconSize(ptIconSize);
				} catch (Exception e) {
					Logger.log(new LogEvent(LOGID, "Error adding a row to table "
							+ sTableID, e));
					try {
						if (!sortedRows.contains(row))
							sortedRows.add(row);
					} catch (Exception e2) {
						Debug.out(e2);
					}
				}
			} // for dataSources

			// Sanity Check: Make sure # of rows in table and in array match
			if (table.getItemCount() > sortedRows.size()) {
				// This could happen if one of the datasources was null, or
				// an error occured, or we exited early because things were talking
				// to long
				table.setItemCount(sortedRows.size());
			}

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "Error while adding row to Table "
					+ sTableID, e));
		} finally {
			sortedRows_mon.exit();
			dataSourceToRow_mon.exit();

			if (!bBrokeEarly) {
				bReallyAddingDataSources = false;
				refreshenProcessDataSourcesTimer();
			}
		}

		fillRowGaps(false);
		visibleRowsChanged();
		if (DEBUGADDREMOVE)
			debug("<< " + sortedRows.size());
	}

  public void removeDataSource(final Object dataSource) {
  	removeDataSources(new Object[] { dataSource });
  }
  
  /** Remove the specified dataSource from the table.
   *
   * @param dataSources data sources to be removed
   * @param bImmediate Remove immediately, or queue and remove at next refresh
   */
  public void removeDataSources(final Object[] dataSources) {
  	if (dataSources == null) {
  		return;
  	}

		if (IMMEDIATE_ADDREMOVE_DELAY == 0) {
			reallyAddDataSources(dataSources);
			return;
		}

		try{
			dataSourceToRow_mon.enter();	
	
  		for (int i = 0; i < dataSources.length; i++)
  			dataSourcesToRemove.add(dataSources[i]);

			if (DEBUGADDREMOVE)
				debug("Queued " + dataSources.length
						+ " dataSources to remove.  Total Queued: "
						+ dataSourcesToRemove.size());
		}finally{
			dataSourceToRow_mon.exit();
		}

		refreshenProcessDataSourcesTimer();
  }
  
  private void reallyRemoveDataSources(final Object[] dataSources) {
  	
  	if (DEBUGADDREMOVE)
  		debug(">> Remove rows");

		boolean ok = Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (table == null || table.isDisposed()) {
					return;
				}

				if (DEBUGADDREMOVE)
					debug(">>> Remove rows.  Start");
				
				ArrayList itemsToRemove = new ArrayList();
				int iTopIndex = table.getTopIndex();
				int iBottomIndex = Utils.getTableBottomIndex(table, iTopIndex);
				boolean bRefresh = false;
				
				for (int i = 0; i < dataSources.length; i++) {
					if (dataSources[i] == null)
						continue;

					// Must remove from map before deleted from gui
					TableRowCore item = (TableRowCore) dataSourceToRow.remove(dataSources[i]);
					if (item != null) {
						if (!bRefresh) {
							int index = item.getIndex();
							bRefresh = index >= iTopIndex || index <= iBottomIndex;
						}
						itemsToRemove.add(item);
						sortedRows.remove(item);
					}
				}

				// Remove the rows from SWT first.  On SWT 3.2, this currently has 
				// zero perf gain, and a small perf gain on Windows.  However, in the
				// future it may be optimized.
				int[] swtRowsToRemove = new int[itemsToRemove.size()];
				int count = 0;
				for (Iterator iter = itemsToRemove.iterator(); iter.hasNext();) {
					TableRowCore item = (TableRowCore) iter.next();
					int index = item.getIndex();
					if (index >= 0) {
						swtRowsToRemove[count++] = index;
					} else if (count != 0) {
						// duplicate indexes will be only removed once
						// this saves us from shrinking the array
						swtRowsToRemove[count] = swtRowsToRemove[count - 1];
						count++;
					}
				}
				if (count > 0) {
					table.remove(swtRowsToRemove);
				}
				
				for (Iterator iter = itemsToRemove.iterator(); iter.hasNext();) {
					TableRowCore item = (TableRowCore) iter.next();
					item.delete(false);
				}

				if (bRefresh) {
					refreshVisibleRows();
				}

				if (DEBUGADDREMOVE)
					debug("<< Remove "
							+ itemsToRemove.size() + " rows. now " + dataSourceToRow.size()
							+ "ds; tc=" + table.getItemCount());
			}
		});

		if (!ok) {
			// execRunnable will only fail if we are closing
			for (int i = 0; i < dataSources.length; i++) {
				if (dataSources[i] == null)
					continue;

				TableRowCore item = (TableRowCore) dataSourceToRow.get(dataSources[i]);
				dataSourceToRow.remove(dataSources[i]);
				if (item != null) {
					sortedRows.remove(item);
					item.delete(true);
				}
			}

			fillRowGaps(false);
			if (DEBUGADDREMOVE)
				debug("<< Remove 1 row, noswt");
		}
	}

  /** Remove all the data sources (table rows) from the table.
   */
  public void removeAllTableRows() {
  	long lTimeStart = System.currentTimeMillis();
  	
		try {
			dataSourceToRow_mon.enter();
			sortedRows_mon.enter();

			dataSourceToRow.clear();
			sortedRows.clear();
			
			dataSourcesToAdd.clear();
			dataSourcesToRemove.clear();

			if (DEBUGADDREMOVE)
				debug("removeAll");

		} finally {

			sortedRows_mon.exit();
			dataSourceToRow_mon.exit();
		}

  	Utils.execSWTThread(new AERunnable() {
  		public void runSupport() {
  			if (table != null && !table.isDisposed())
  				table.removeAll();

  			// Image Disposal handled by each cell
  			TableRowCore[] rows = getRows();
  			for (int i = 0; i < rows.length; i++)
  				rows[i].delete(false);
  		}
  	});

		if (DEBUGADDREMOVE) {
	    long lTimeDiff = (System.currentTimeMillis() - lTimeStart);
			if (lTimeDiff > 10)
				debug("RemovaAll took " + lTimeDiff + "ms");
		}
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
      graphicsUpdate = configMan.getIntParameter("Graphics Update");
      return;
    }
    if (parameterName.equals("ReOrder Delay")) {
    	reOrderDelay = configMan.getIntParameter("ReOrder Delay");
    	return;
    }
    if (parameterName.startsWith("Color")) {
      tableInvalidate();
    }
  }
  
  // ITableStructureModificationListener
  public void tableStructureChanged() {
    removeAllTableRows();
    initializeTableColumns(table);
    refreshTable(false);
  }
  
  // ITableStructureModificationListener
  public void columnOrderChanged(int[] positions) {
		try {
			table.setColumnOrder(positions);
		} catch (NoSuchMethodError e) {
			// Pre SWT 3.1
			// This shouldn't really happen, since this function only gets triggered
			// from SWT >= 3.1
			tableStructureChanged();
		}
	}
  
  /** 
   * The Columns width changed
   */
  // ITableStructureModificationListener
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
  
  // ITableStructureModificationListener
  public void columnInvalidate(TableColumnCore tableColumn) {
  	// We are being called from a plugin (probably), so we must refresh
  	columnInvalidate(tableColumn, true);
  }

  public void columnRefresh(TableColumnCore tableColumn) {
    final String sColumnName = tableColumn.getName();
    runForAllRows(new GroupTableRowVisibilityRunner() {
      public void run(TableRowCore row, boolean bVisible) {
        TableCellCore cell = row.getTableCellCore(sColumnName);
        if (cell != null)
          cell.refresh(true, bVisible);
      }
    });
  }

  /**
   * Invalidate and refresh whole table
   */
  public void tableInvalidate() {
    runForAllRows(new GroupTableRowVisibilityRunner() {
      public void run(TableRowCore row, boolean bVisible) {
        row.invalidate();
        row.refresh(true, bVisible);
      }
    });
  }

  /**
   * Invalidate all the cells in a column
   * 
   * @param sColumnName Name of column to invalidate
   */
  public void columnInvalidate(final String sColumnName) {
  	TableColumnCore tc = TableColumnManager.getInstance().getTableColumnCore(
				sTableID, sColumnName);
  	if (tc != null)
  		columnInvalidate(tc, tc.getType() == TableColumnCore.TYPE_TEXT_ONLY);
  }

  public void columnInvalidate(TableColumnCore tableColumn, final boolean bMustRefresh) {
  	final String sColumnName = tableColumn.getName();

    runForAllRows(new GroupTableRowRunner() {
      public void run(TableRowCore row) {
        TableCellCore cell = row.getTableCellCore(sColumnName);
        if (cell != null)
          cell.invalidate(bMustRefresh);
      }
    });
  }
  
  /**
	 * Retrieve a list of <pre>TableCell</pre>s, in the last sorted order.
	 * The order will not be of the supplied cell's sort unless the table
	 * has been sorted by that column previously.
	 * <p>
	 * ie.  You can sort on the 5th column, and retrieve the cells for the
	 *      3rd column, but they will be in order of the 5th columns sort.  
	 * 
	 * @param sColumnName Which column cell's to return.  This does not sort
	 *         the array on the column. 
	 * @return array of cells
	 */
	public TableCellCore[] getColumnCells(String sColumnName) {
		TableCellCore[] cells = new TableCellCore[sortedRows.size()];

		try {
			sortedRows_mon.enter();

			int i = 0;
			for (Iterator iter = sortedRows.iterator(); iter.hasNext();) {
				TableRowCore row = (TableRowCore) iter.next();
				cells[i++] = row.getTableCellCore(sColumnName);
			}

		} finally {
			sortedRows_mon.exit();
		}

		return cells;
	}
	
	/** Get all the rows for this table, in the order they are displayed
	 *
	 * @return a list of TableRowCore objects in the order the user sees them
	 */
	public TableRowCore[] getRows() {
		try {
			sortedRows_mon.enter();

			return (TableRowCore[]) sortedRows.toArray(new TableRowCore[0]);

		} finally {
			sortedRows_mon.exit();
		}
	}
	
	/**
	 * Get the row associated with a datasource
	 * @param dataSource a reference to a core Datasource object 
	 * 										(not a plugin datasource object)
	 * @return The row, or null
	 */
	public TableRowCore getRow(Object dataSource) {
		return (TableRowCore)dataSourceToRow.get(dataSource);
	}
	
	public int getRowCount() {
		// don't use sortedRows here, it's not always up to date 
		return dataSourceToRow.size();
	}
  
	public Object[] getDataSources() {
		return dataSourceToRow.keySet().toArray();
	}
	
  /* various selected rows functions */
  /***********************************/

  public List getSelectedDataSourcesList() {
  	return getSelectedDataSourcesList(true);
  }

  /** Returns an array of all selected Data Sources.  Null data sources are
   * ommitted.
   *
   * @return an array containing the selected data sources
   * 
   * @TODO TuxPaper: Virtual row not created when usint getSelection?
   *                  computePossibleActions isn't being calculated right
   *                  because of non-created rows when select user selects all
   */
  public List getSelectedDataSourcesList(boolean bCoreDataSource) {
    ArrayList l = new ArrayList();
    if (table != null && !table.isDisposed()) {
      TableItem[] tis = table.getSelection();
      for (int i = 0; i < tis.length; i++) {
        TableRowCore row = (TableRowCore)tis[i].getData("TableRow");
        if (row == null) {
        	fillRowGaps(false);

        	// Try again
          row = (TableRowCore)tis[i].getData("TableRow");
          if (row == null)
          	System.out.println("XXX Boo, row still null "
								+ table.indexOf(tis[i]) + ";sd=" + tis[i].getData("SD") + ";" 
								+ Debug.getCompressedStackTrace());
        }
        if (row != null && row.getDataSource(true) != null)
          l.add(row.getDataSource(bCoreDataSource));
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
  
  public Object[] getSelectedDataSources(boolean bCoreDataSource) {
    return getSelectedDataSourcesList(bCoreDataSource).toArray();
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
  
  public Object getFirstSelectedDataSource() {
  	return getFirstSelectedDataSource(true);
  }
  
  public TableRowCore[] getVisibleRows() {
		if (table == null || table.isDisposed())
			return new TableRowCore[0];

		int iTopIndex = table.getTopIndex();
		int iBottomIndex = Utils.getTableBottomIndex(table, iTopIndex);

		int size = iBottomIndex - iTopIndex + 1;
		if (size <= 0)
			return new TableRowCore[0];

		TableRowCore[] rows = new TableRowCore[size];
		int pos = 0;
		for (int i = iTopIndex; i <= iBottomIndex; i++) {
			TableRowCore row = (TableRowCore) table.getItem(i).getData("TableRow");
			if (row != null)
				rows[pos++] = row;
		}
		
		if (pos <= rows.length) {
			// Some were null, shrink array
			TableRowCore[] temp = new TableRowCore[pos];
			System.arraycopy(rows, 0, temp, 0, pos);
			return temp;
		}
		
		return rows;
	}

  /** Returns the first selected data sources.
   *
   * @return the first selected data source, or null if no data source is 
   *         selected
   */
  public Object getFirstSelectedDataSource(boolean bCoreObject) {
    if (table == null || table.isDisposed() || table.getSelectionCount() == 0)
      return null;

    TableRowCore row = (TableRowCore)table.getSelection()[0].getData("TableRow");
    if (row == null)
      return null;
    return row.getDataSource(bCoreObject);
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
  
  /** For each visible row source, run the code provided by the specified 
	 * parameter.
	 *
	 * @param runner Code to run for each selected row/datasource
	 */
	public void runForVisibleRows(GroupTableRowRunner runner) {
		TableRowCore[] rows = getVisibleRows();

		for (int i = 0; i < rows.length; i++)
			runner.run(rows[i]);
	}

  /** For every row source, run the code provided by the specified 
	 * parameter.
	 *
	 * @param runner Code to run for each row/datasource
	 */
	public void runForAllRows(GroupTableRowRunner runner) {
		// put to array instead of synchronised iterator, so that runner can remove
		TableRowCore[] rows = getRows();

		for (int i = 0; i < rows.length; i++)
			runner.run(rows[i]);
	}

  /** For every row source, run the code provided by the specified 
	 * parameter.
	 *
	 * @param runner Code to run for each row/datasource
	 */
	public void runForAllRows(GroupTableRowVisibilityRunner runner) {
		// put to array instead of synchronised iterator, so that runner can remove
		TableRowCore[] rows = getRows();
		int iTopIndex = table.getTopIndex();
		int iBottomIndex = Utils.getTableBottomIndex(table, iTopIndex);

		for (int i = 0; i < rows.length; i++) {
			int index = rows[i].getIndex();
			runner.run(rows[i], index >= iTopIndex && index <= iBottomIndex);
		}
	}
	
  /**
   * Runs a specified task for a list of table items that the table contains
   * @param items A list of TableItems that are part of the table view
   * @param runner A task
   */
  public void runForTableItems(List items, GroupTableRowRunner runner) {
      if(table == null || table.isDisposed())
          return;

      final Iterator iter = items.iterator();
      while (iter.hasNext()) {
          TableItem tableItem = (TableItem) iter.next();
          if(tableItem.isDisposed())
            continue;

          TableRowCore row = (TableRowCore) tableItem.getData("TableRow");
          if (row != null)
            runner.run(row);
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


	/** 
	 * Used with {@link TableView#runForSelectedRows}
	 */
	public abstract class GroupTableRowRunner {
		/** Code to run 
		 * @param row TableRowCore to run code against
		 */
		public void run(TableRowCore row) {
		}
	}

	public abstract class GroupTableRowVisibilityRunner {
		/** Code to run 
		 * @param row TableRowCore to run code against
		 */
		public void run(TableRowCore row, boolean bVisible) {
		}
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
  }
  
  /** Handle sorting of a column based on clicking the Table Header */
  private class ColumnSelectionListener implements Listener {
    /** Process a Table Header click
     * @param event event information
     */
    public void handleEvent(final Event event) {
    	if (COLUMN_CLICK_DELAY) {
				// temporary for OSX.. resizing column triggers selection, so cancel
				// if a resize was recent. 
				final Timer timer = new Timer("Column Selection Wait");
				timer.addEvent(System.currentTimeMillis() + 85,
						new TimerEventPerformer() {
							public void perform(TimerEvent timerEvent) {
								Utils.execSWTThread(new AERunnable() {
									public void runSupport() {
										if (lLastColumnResizeOn == -1
												|| System.currentTimeMillis() - lLastColumnResizeOn > 220)
											reallyHandleEvent(event);
									}
								});
								timer.destroy();
							}
						});
			} else {
				reallyHandleEvent(event);
			}
		}
    
    private void reallyHandleEvent(Event event) {
			TableColumn column = (TableColumn) event.widget;
			if (column == null)
				return;
			TableColumnCore tableColumnCore = (TableColumnCore) column.getData("TableColumnCore");
			if (tableColumnCore != null) {
				sortColumnReverse(tableColumnCore);
				refreshTable(true);
			}
    }
  }

  /**
   * Handle movement of a column based on user dragging the Column Header.
   * SWT >= 3.1
   */
  private class ColumnMoveListener implements Listener {
		public void handleEvent(Event event) {
			TableColumn column = (TableColumn) event.widget;
			if (column == null)
				return;
			
			TableColumnCore tableColumnCore = (TableColumnCore) column
					.getData("TableColumnCore");
			if (tableColumnCore == null)
				return;

			Table table = column.getParent();
			
			// Get the 'added position' of column
			// It would have been easier if event (.start, .end) contained the old
			// and new position..
			TableColumn[] tableColumns = table.getColumns();
			int iAddedPosition;
			for (iAddedPosition = 0; iAddedPosition < tableColumns.length; iAddedPosition++) {
				if (column == tableColumns[iAddedPosition])
					break;
			}
			if (iAddedPosition >= tableColumns.length)
				return;

			// Find out position in the order list
			int iColumnOrder[];
			try {
				iColumnOrder = table.getColumnOrder();
      } catch (NoSuchMethodError e) {
      	// Ignore < SWT 3.1
      	return;
      }
			for (int i = 0; i < iColumnOrder.length; i++) {
				if (iColumnOrder[i] == iAddedPosition) {
					int iNewPosition = i - (bSkipFirstColumn ? 1 : 0);
					if (tableColumnCore.getPosition() != iNewPosition) {
						//System.out.println("Moving " + tableColumnCore.getName() + " to Position " + i);
						tableColumnCore.setPositionNoShift(iNewPosition);
						tableColumnCore.saveSettings();
						TableStructureEventDispatcher.getInstance(sTableID).columnOrderChanged(iColumnOrder);
					}
					break;
				}
			}
		}
	}
  
  private class TableTooltips implements Listener {
		Shell toolTipShell = null;
		Shell mainShell = null;

		Label toolTipLabel = null;

		/**
		 * Initialize
		 */
		public TableTooltips(Table table) {
			mainShell = table.getShell();
			
	  	table.addListener(SWT.Dispose, this);
	  	table.addListener(SWT.KeyDown, this);
	  	table.addListener(SWT.MouseMove, this);
	  	table.addListener(SWT.MouseHover, this);
			mainShell.addListener(SWT.Deactivate, this);
			getComposite().addListener(SWT.Deactivate, this);
		}

		public void handleEvent(Event event) {
			switch (event.type) {
				case SWT.MouseHover: {
					if (toolTipShell != null && !toolTipShell.isDisposed())
						toolTipShell.dispose();

					TableCellCore cell = getTableCell(event.x, event.y);
					if (cell == null)
						return;
					cell.invokeToolTipListeners(TableCellCore.TOOLTIPLISTENER_HOVER);
					Object oToolTip = cell.getToolTip();

					// TODO: support composite, image, etc
					if (oToolTip == null || !(oToolTip instanceof String))
						return;
					String sToolTip = (String) oToolTip;

					Display d = table.getDisplay();
					if (d == null)
						return;

					// We don't get mouse down notifications on trim or borders..
					toolTipShell = new Shell(table.getShell(), SWT.ON_TOP);
					FillLayout f = new FillLayout();
					try {
						f.marginWidth = 3;
						f.marginHeight = 1;
					} catch (NoSuchFieldError e) {
						/* Ignore for Pre 3.0 SWT.. */
					}
					toolTipShell.setLayout(f);
					toolTipShell.setBackground(d.getSystemColor(SWT.COLOR_INFO_BACKGROUND));

					toolTipLabel = new Label(toolTipShell, SWT.WRAP);
					toolTipLabel.setForeground(d.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
					toolTipLabel.setBackground(d.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
					toolTipShell.setData("TableCellCore", cell);
					toolTipLabel.setText(sToolTip.replaceAll("&", "&&"));
					// compute size on label instead of shell because label
					// calculates wrap, while shell doesn't
					Point size = toolTipLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT);
					if (size.x > 600) {
						size = toolTipLabel.computeSize(600, SWT.DEFAULT, true);
					}
					size.x += toolTipShell.getBorderWidth() * 2 + 2;
					size.y += toolTipShell.getBorderWidth() * 2;
					try {
						size.x += toolTipShell.getBorderWidth() * 2 + (f.marginWidth * 2);
						size.y += toolTipShell.getBorderWidth() * 2 + (f.marginHeight * 2);
					} catch (NoSuchFieldError e) {
						/* Ignore for Pre 3.0 SWT.. */
					}
					Point pt = table.toDisplay(event.x, event.y);
					Rectangle displayRect;
					try {
						displayRect = table.getMonitor().getClientArea();
					} catch (NoSuchMethodError e) {
						displayRect = table.getDisplay().getClientArea();
					}
					if (pt.x + size.x > displayRect.x + displayRect.width) {
						pt.x = displayRect.x + displayRect.width - size.x;
					}
					
					if (pt.y + size.y > displayRect.y + displayRect.height) {
						pt.y -= size.y + 2;
					} else {
						pt.y += 21;
					}
					
					if (pt.y < displayRect.y)
						pt.y = displayRect.y;

					toolTipShell.setBounds(pt.x, pt.y, size.x, size.y);
					toolTipShell.setVisible(true);

					break;
				}
				
				case SWT.Dispose:
					if (mainShell != null && !mainShell.isDisposed())
						mainShell.removeListener(SWT.Deactivate, this);
					if (getComposite() != null && !getComposite().isDisposed())
						mainShell.removeListener(SWT.Deactivate, this);
					// fall through
				
				default:
					if (toolTipShell != null) {
						toolTipShell.dispose();
						toolTipShell = null;
						toolTipLabel = null;
					}
					break;
			} // switch
		} // handlEvent()
  }

  private int getColumnNo(int iMouseX) {
    int iColumn = -1;
    if (table.getItemCount() > 0) {
      //Using  table.getTopIndex() instead of 0, cause
      //the first row has no bounds when it's not visible under OS X.
      TableItem ti = table.getItem(table.getTopIndex());
      for (int i = bSkipFirstColumn ?  1 : 0; i < table.getColumnCount(); i++) {
        // M8 Fixes SWT GTK Bug 51777:
        //  "TableItem.getBounds(int) returns the wrong values when table scrolled"
        Rectangle cellBounds = ti.getBounds(i);
        //System.out.println("i="+i+";Mouse.x="+iMouseX+";cellbounds="+cellBounds);
        if (iMouseX >= cellBounds.x && iMouseX < cellBounds.x + cellBounds.width && cellBounds.width > 0) {
          iColumn = i;
          break;
        }
      }
    }
    return iColumn;
  }
  
  private TableCellCore getTableCell(int x, int y) {
		int iColumn = getColumnNo(x);
		if (iColumn < 0)
			return null;
		
		TableItem item = table.getItem(new Point(2, y));
		if (item == null)
			return null;
		TableRowCore row = (TableRowCore) item.getData("TableRow");

		if (row == null)
			return null;
		
		TableColumn tcColumn = table.getColumn(iColumn);
		String sCellName = (String) tcColumn.getData("Name");
		if (sCellName == null)
			return null;

		return row.getTableCellCore(sCellName);
  }
  
  private TableColumnCore getTableColumnByOffset(int x) {
		int iColumn = getColumnNo(x);
		if (iColumn < 0)
			return null;
		
		TableColumn column = table.getColumn(iColumn);
    return (TableColumnCore)column.getData("TableColumnCore");
  }

  public void
  generateDiagnostics(
	IndentWriter	writer )
  {
	  super.generateDiagnostics( writer );
	  
	  try{
		  dataSourceToRow_mon.enter();
		  
		  writer.println("DataSources scheduled to Add/Remove: "
					+ dataSourcesToAdd.size()
					+ "/"
					+ dataSourcesToRemove.size());
		  
		  writer.println( "TableView: " + dataSourceToRow.size() + " datasources");
		  Iterator	it = dataSourceToRow.keySet().iterator();
		  
		  while( it.hasNext()){
			  
			  Object key = it.next();
			  
			  writer.println( "  " + key + " -> " + dataSourceToRow.get(key));
		  }
		  
			writer.println("# of SubViews: " + tabViews.size());
		  writer.indent();
		  try {
			  for (Iterator iter = tabViews.iterator(); iter.hasNext();) {
					IView view = (IView) iter.next();
					view.generateDiagnostics(writer);
				}
		  } finally {
		  	writer.exdent();
		  }
	  }finally{
		  
		  dataSourceToRow_mon.exit();
	  }
  }
  
  public boolean getSkipFirstColumn() {
  	return bSkipFirstColumn;
  }
  
  public void setRowDefaultHeight(int iHeight) {
		if (ptIconSize == null)
			ptIconSize = new Point(1, iHeight);
		else
			ptIconSize.y = iHeight;
		bSkipFirstColumn = true;
	}

	public int getRowDefaultHeight() {
		if (ptIconSize == null)
			return 0;
		return ptIconSize.y;
	}

	public void setRowDefaultIconSize(Point size) {
		ptIconSize = size;
		bSkipFirstColumn = true;
	}

	// TabViews Functions
	public void addTabView(IView view) {
		if (view == null || tabFolder == null)
			return;

		CTabItem item = new CTabItem(tabFolder, SWT.NULL);
		item.setData("IView", view);
		Messages.setLanguageText(item, view.getData());
		view.initialize(tabFolder);
		item.setControl(view.getComposite());
		tabViews.add(view);
	}
	
	private void fillRowGaps(boolean bForceDataRefresh) {
		_sortColumn(bForceDataRefresh, true, true);
	}

	private void sortColumn(boolean bForceDataRefresh) {
		_sortColumn(bForceDataRefresh, false, true);
	}

	private void _sortColumn(boolean bForceDataRefresh,
			boolean bFillGapsOnly, boolean bFollowSelected) 
	{
		if (table == null || table.isDisposed()) {
			return;
		}

		try{
			sortColumn_mon.enter();
		
			long lTimeStart;
			if (DEBUG_SORTER) {
				//System.out.println(">>> Sort.. ");
				lTimeStart = System.currentTimeMillis();
			}
	
			int iNumMoves = 0;
	
			// This actually gets the focus, assuming the focus is selected
			int iFocusIndex = table.getSelectionIndex();
			TableRowCore focusedRow = (iFocusIndex == -1) ? null : (TableRowCore) table
					.getItem(iFocusIndex).getData("TableRow");
	
			int[] selectedRowIndices = table.getSelectionIndices();
			TableRowCore[] selectedRows = new TableRowCore[selectedRowIndices.length];
			for (int i = 0; i < selectedRowIndices.length; i++) {
				selectedRows[i] = (TableRowCore) table.getItem(selectedRowIndices[i])
						.getData("TableRow");
			}
	
			try {
				sortedRows_mon.enter();
	
				if (bForceDataRefresh) {
					for (Iterator iter = sortedRows.iterator(); iter.hasNext();) {
						TableRowCore row = (TableRowCore) iter.next();
						TableCellCore cell = row.getTableCellCore(rowSorter.getColumnName());
						if (cell != null) {
							cell.refresh();
						}
					}
				}
	
				if (!bFillGapsOnly) {
					TableColumnCore tc = rowSorter.getColumn();
					if (tc == null || tc.getLastSortValueChange() > lLastSortedOn) {
						lLastSortedOn = SystemTime.getCurrentTime();
						Collections.sort(sortedRows, rowSorter);
						if (DEBUG_SORTER) {
							long lTimeDiff = (System.currentTimeMillis() - lTimeStart);
							if (lTimeDiff > 150)
								System.out.println("--- Build & Sort took " + lTimeDiff + "ms");
						}
					} else {
						if (DEBUG_SORTER) {
							System.out.println("Skipping sort :)");
						}
					}
	
				}
	
				if (bTableVirtual) {
					for (int i = 0; i < sortedRows.size(); i++) {
						TableRowCore row = (TableRowCore) sortedRows.get(i);
						if (row.setTableItem(i)) {
							iNumMoves++;
						}
					}
				} else {
					for (int i = 0; i < sortedRows.size(); i++) {
						TableRowCore row = (TableRowCore) sortedRows.get(i);
						if (row.setTableItem(i)) {
							iNumMoves++;
						}
					}
				}
			} finally {
				sortedRows_mon.exit();
			}
	
			// move cursor to selected row
			/** SWT/Windows Bug:
			 * When we set selection, the first index is the focus row.
			 * This works visually, however, if you press shift-up or shift-down,
			 * it uses an older selection index.
			 * 
			 * ie. User selects row #10
			 *     Programmically change selection to Row #15 only
			 *     Shift-down
			 *     Rows 10 through 26 will be selected
			 *     
			 * This is Eclipse bug #77106, and is marked WONTFIX 
			 */
			if (focusedRow != null) {
				int pos = 1;
				int numSame = 0;
				int[] newSelectedRowIndices = new int[selectedRows.length];
				Arrays.sort(selectedRowIndices);
				for (int i = 0; i < selectedRows.length; i++) {
					int index = selectedRows[i].getIndex();
					newSelectedRowIndices[(selectedRows[i] == focusedRow) ? 0 : pos++] = index;
					if (Arrays.binarySearch(selectedRowIndices, index) >= 0)
						numSame++;
				}
				
				if (numSame < selectedRows.length) {
					// XXX setSelection calls showSelection().  We don't want the table
					//     to jump all over.  Quick fix is to reset topIndex, but
					//     there might be a better way
					int iTopIndex = 0;
					if (!bFollowSelected) {
						table.setRedraw(false);
						iTopIndex = table.getTopIndex();
					}
					table.setSelection(newSelectedRowIndices);
					if (!bFollowSelected) {
						table.setTopIndex(iTopIndex);
						table.setRedraw(true);
					}
				}
			}
	
			if (DEBUG_SORTER) {
				long lTimeDiff = (System.currentTimeMillis() - lTimeStart);
				if (lTimeDiff >= 500)
					System.out.println("<<< Sort & Assign took " + lTimeDiff + "ms with "
							+ iNumMoves + " rows (of " + sortedRows.size() + ") moved. "
							+ focusedRow + ";" + Debug.getCompressedStackTrace());
			}
		}finally{
			sortColumn_mon.exit();
		}
	}

	public void sortColumnReverse(TableColumnCore tableColumn) {
		boolean bSameColumn = (rowSorter.getColumnName().equals(tableColumn.getName()));
		if (!bSameColumn) {
			rowSorter.setColumn(tableColumn);
			int iSortDirection = configMan.getIntParameter(CFG_SORTDIRECTION);
			if (iSortDirection == 0) 
				rowSorter.setAscending(true);
			else if (iSortDirection == 1)
				rowSorter.setAscending(false);
			else
				rowSorter.setAscending(!rowSorter.isAscending());

			configMan.setParameter(sTableID + ".sortAsc", rowSorter.isAscending());
			configMan.setParameter(sTableID + ".sortColumn",
					rowSorter.getColumnName());
		} else {
			rowSorter.setAscending(!rowSorter.isAscending());
			configMan.setParameter(sTableID + ".sortAsc", rowSorter.isAscending());
		}

		changeColumnIndicator();
		sortColumn(!bSameColumn);
	}

	private void changeColumnIndicator() {
		if (table == null || table.isDisposed())
			return;

		try {
			// can't use TableColumnCore.getPosition, because user may have moved
			// columns around, messing up the SWT column indexes.  
			// We can either use search columnsOrdered, or search table.getColumns()
			TableColumn[] tcs = table.getColumns();
			for (int i = 0; i < tcs.length; i++) {
				String sName = (String)tcs[i].getData("Name");
				if (sName != null && sName.equals(rowSorter.getColumnName())) {
					table.setSortDirection(rowSorter.isAscending() ? SWT.UP : SWT.DOWN);
					table.setSortColumn(tcs[i]);
					return;
				}
			}

			table.setSortColumn(null);
		} catch (NoSuchMethodError e) {
			// sWT < 3.2 doesn't have column indicaters
		}
	}

	private void visibleRowsChanged() {
		if (Utils.SWT32_TABLEPAINT) {
			return;
		}
		
		if (!table.isVisible()) {
		  lastTopIndex = 0;
		  lastBottomIndex = -1;
		  return;
		}
		//System.out.println(SystemTime.getCurrentTime() + ": VRC " + Debug.getCompressedStackTrace());
		
		boolean bTableUpdate = false;
		int iTopIndex = table.getTopIndex();
		int iBottomIndex = Utils.getTableBottomIndex(table, iTopIndex);

		if (lastTopIndex != iTopIndex) {
			int tmpIndex = lastTopIndex;
			lastTopIndex = iTopIndex;
			
			if (iTopIndex < tmpIndex) {
				if (tmpIndex > iBottomIndex + 1 && iBottomIndex >= 0)
					tmpIndex = iBottomIndex + 1;

				//System.out.println("Refresh top rows " + iTopIndex + " to " + (tmpIndex - 1));
				try {
					sortedRows_mon.enter();
					for (int i = iTopIndex; i < tmpIndex && i < sortedRows.size(); i++) {
						TableRowCore row = (TableRowCore) sortedRows.get(i);
						row.refresh(true, true);
						row.setAlternatingBGColor(true);
						if (Constants.isOSX) {
							bTableUpdate = true;
						}
					}
				} finally {
					sortedRows_mon.exit();
				}

				// A refresh might have triggered a row height resize, so
				// bottom index needs updating
				iBottomIndex = Utils.getTableBottomIndex(table, iTopIndex);
			} else {
				//System.out.println("Made T.Invisible " + (tmpIndex) + " to " + (iTopIndex - 1));
			}
		}

		if (lastBottomIndex != iBottomIndex) {
			int tmpIndex = lastBottomIndex;
			lastBottomIndex = iBottomIndex;
			
			if (tmpIndex < iTopIndex - 1)
				tmpIndex = iTopIndex - 1;

			if (tmpIndex <= iBottomIndex) {
				//System.out.println("Refresh bottom rows " + (tmpIndex + 1) + " to " + iBottomIndex);
				try {
					sortedRows_mon.enter();
					for (int i = tmpIndex + 1; i <= iBottomIndex
							&& i < sortedRows.size(); i++) {
						TableRowCore row = (TableRowCore) sortedRows.get(i);
						row.refresh(true, true);
						row.setAlternatingBGColor(true);
						if (Constants.isOSX) {
							bTableUpdate = true;
						}
					}
				} finally {
					sortedRows_mon.exit();
				}
			} else {
				//System.out.println("Made B.Invisible " + (tmpIndex) + " to " + (iBottomIndex + 1));
			}
		}
		
		if (bTableUpdate) {
			table.update();
		}
	}

	public Image obfusticatedImage(final Image image, Point shellOffset) {
		if (table.getItemCount() == 0) {
			return image;
		}
		Rectangle tableArea = table.getClientArea();

		TableColumn[] tableColumnsSWT = table.getColumns();
		for (int i = 0; i < tableColumnsSWT.length; i++) {
			final TableColumnCore tc = (TableColumnCore) tableColumnsSWT[i].getData("TableColumnCore");

			if (tc != null && tc.isObfusticated()) {
				int iTopIndex = table.getTopIndex();
				int iBottomIndex = Utils.getTableBottomIndex(table, iTopIndex);

				int size = iBottomIndex - iTopIndex + 1;
				if (size <= 0)
					continue;

				for (int j = iTopIndex; j <= iBottomIndex; j++) {
					TableItem rowSWT = table.getItem(j);
					TableRowCore row = (TableRowCore) table.getItem(j).getData("TableRow");
					if (row != null) {
						TableCellCore cell = row.getTableCellCore(tc.getName());
						final Rectangle columnBounds = rowSWT.getBounds(i);
						if (columnBounds.y + columnBounds.height > tableArea.y
								+ tableArea.height) {
							columnBounds.height -= (columnBounds.y + columnBounds.height)
									- (tableArea.y + tableArea.height);
						}
						if (columnBounds.x + columnBounds.width > tableArea.x
								+ tableArea.width) {
							columnBounds.width -= (columnBounds.x + columnBounds.width)
									- (tableArea.x + tableArea.width);
						}

						final Point offset = table.toDisplay(columnBounds.x, columnBounds.y);

						columnBounds.x = offset.x - shellOffset.x;
						columnBounds.y = offset.y - shellOffset.y;

						String text = cell.getObfusticatedText();

						if (text != null) {
							UIDebugGenerator.obfusticateArea(table.getDisplay(), image,
									columnBounds, text);
						}
					}
				}

				//UIDebugGenerator.offusticateArea(image, columnBounds);
			}
		}

		IView view = getActiveSubView();
		if (view instanceof ObfusticateImage) {
			((ObfusticateImage)view).obfusticatedImage(image, shellOffset);
		}
		return image;
	}
	
	private void debug(String s) {
		System.out.println(SystemTime.getCurrentTime() + ": " + sTableID + ": " + s);
	}
}
