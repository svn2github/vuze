/**
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */
package com.aelitis.azureus.ui.swt.views.list;

import java.util.*;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.config.impl.ConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.views.TableView.GroupTableRowRunner;
import org.gudy.azureus2.ui.swt.views.table.*;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnManager;
import org.gudy.azureus2.ui.swt.views.table.utils.TableStructureEventDispatcher;

import com.aelitis.azureus.ui.swt.skin.SWTSkinProperties;
import com.aelitis.azureus.ui.swt.utils.*;
import com.aelitis.azureus.ui.swt.utils.ImageLoader;

import org.gudy.azureus2.plugins.ui.tables.TableCellMouseEvent;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;

/**
 * @author TuxPaper
 * @created Jun 12, 2006
 *
 */
public abstract class ListView implements UIUpdatable, Listener,
		ITableStructureModificationListener
{
	private static final boolean DEBUGADDREMOVE = false;

	// Shorter name for ConfigManager, easier to read code
	private static final ConfigurationManager configMan = ConfigurationManager.getInstance();

	private static final String CFG_SORTDIRECTION = "config.style.table.defaultSortOrder";

	private static final boolean DEBUG_SORTER = false;

	private Composite listComposite;

	private ScrolledComposite sc;

	private final SWTSkinProperties skinProperties;

	private TableColumnCore[] visibleColumns;

	/** ArrayList of ListRow */
	private ArrayList selectedRows = new ArrayList();

	private AEMonitor selectedRows_mon = new AEMonitor("ListView:SR");

	private ArrayList rows = new ArrayList();

	private Map mapDataSourceToRow = new HashMap();

	/** Monitor for both rows and mapDataSourceToRow since these two are linked */
	private AEMonitor row_mon = new AEMonitor("ListView:OTSI");

	private ListRow rowFocused = null;

	private final String sTableID;

	/** Queue added datasources and add them on refresh */
	private List dataSourcesToAdd = new ArrayList(4);

	/** Queue removed datasources and add them on refresh */
	private List dataSourcesToRemove = new ArrayList(4);

	private long lCancelSelectionTriggeredOn = -1;

	private List listenersSelection = new ArrayList();

	private List listenersCountChange = new ArrayList();

	private boolean bMouseClickIsDefaultSelection = false;

	private int iGraphicRefresh;

	protected int graphicsUpdate;

	/** Sorting functions */
	private TableColumnCore sortColumn;

	/** TimeStamp of when last sorted all the rows was */
	private long lLastSortedOn;

	private Composite headerArea;

	private TableColumnCore[] allColumns;

	public ListView(String sTableID, SWTSkinProperties skinProperties,
			Composite parent) {
		this.skinProperties = skinProperties;
		this.sTableID = sTableID;

		FormData formData;

		COConfigurationManager.addAndFireParameterListener("Graphics Update",
				new ParameterListener() {
					public void parameterChanged(String parameterName) {
						graphicsUpdate = COConfigurationManager.getIntParameter("Graphics Update");
					}
				});

		parent.setBackgroundMode(SWT.INHERIT_FORCE);
		sc = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);

		formData = new FormData();
		formData.left = new FormAttachment(0);
		formData.top = new FormAttachment(0);
		formData.right = new FormAttachment(100);
		formData.bottom = new FormAttachment(100);
		sc.setLayoutData(formData);

		//listComposite = new Composite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		listComposite = new Composite(sc, SWT.NONE);
		listComposite.setLayout(new FormLayout());

		formData = new FormData();
		formData.left = new FormAttachment(0);
		formData.top = new FormAttachment(0);
		formData.right = new FormAttachment(100);
		formData.bottom = new FormAttachment(100);
		listComposite.setLayoutData(formData);

		sc.setContent(listComposite);
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);

		sc.getVerticalBar().setIncrement(30);

		listComposite.addListener(SWT.MouseDoubleClick, this);
		listComposite.addListener(SWT.FocusIn, this);
		listComposite.addListener(SWT.FocusOut, this);
		listComposite.addListener(SWT.Traverse, this);
		listComposite.addListener(SWT.DefaultSelection, this);
		listComposite.addListener(SWT.KeyDown, this); // so we are a tab focus

		UIUpdaterFactory.getInstance().addUpdater(this);
		TableStructureEventDispatcher.getInstance(sTableID).addListener(this);
	}

	/**
	 * @param headerArea
	 */
	protected void setupHeader(final Composite headerArea) {
		this.headerArea = headerArea;
		FormData formData;

		Label lblCenterer = new Label(headerArea, SWT.WRAP);
		formData = new FormData(0, 0);
		formData.top = new FormAttachment(0, 0);
		formData.bottom = new FormAttachment(100, 0);
		lblCenterer.setLayoutData(formData);

		ImageLoader imgLoader = ImageLoaderFactory.getInstance();
		final Image imgSortAsc = imgLoader.getImage("image.sort.asc");
		final Image imgSortDesc = imgLoader.getImage("image.sort.desc");

		int sortWidth = Math.max(imgSortAsc.getBounds().width,
				imgSortDesc.getBounds().width) + 2;

		// set min column width to width of header
		GC gc = new GC(headerArea);
		try {
			TableColumnCore[] columns = getVisibleColumns();
			for (int i = 0; i < columns.length; i++) {
				TableColumnCore column = columns[i];
				String title = MessageText.getString(column.getTitleLanguageKey(), "");
				int oldWidth = column.getWidth();
				int minWidth = gc.textExtent(title).x + sortWidth;
				if (minWidth > oldWidth) {
					column.setWidth(minWidth);
				}
			}
		} finally {
			if (gc != null) {
				gc.dispose();
			}
		}

		headerArea.addMouseListener(new MouseListener() {

			public void mouseDoubleClick(MouseEvent e) {
			}

			public void mouseDown(MouseEvent e) {
			}

			public void mouseUp(MouseEvent e) {
				TableColumnCore[] columns = getVisibleColumns();
				int inColumn = -1;
				for (int i = 0; i < columns.length; i++) {
					Rectangle bounds = (Rectangle) headerArea.getData("Column" + i
							+ "Bounds");
					if (bounds != null && bounds.contains(e.x, e.y)) {
						inColumn = i;
						break;
					}
				}
				if (inColumn != -1) {
					setSortColumn(columns[inColumn]);
					System.out.println("sorting on " + columns[inColumn].getName());
				}
			}

		});

		headerArea.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				TableColumnCore[] columns = getVisibleColumns();

				e.gc.setForeground(skinProperties.getColor("color.list.header.fg"));

				Rectangle clientArea = headerArea.getClientArea();
				int pos = clientArea.x + ListRow.MARGIN_WIDTH;
				for (int i = 0; i < columns.length; i++) {
					int width = columns[i].getWidth();
					String key = columns[i].getTitleLanguageKey();
					String text = MessageText.getString(key, "");
					int align = columns[i].getSWTAlign();

					int drawWidth = width;

					if (columns[i].equals(sortColumn)) {
						Image img = sortColumn.isSortAscending() ? imgSortAsc : imgSortDesc;
						Rectangle bounds = img.getBounds();

						if (align == SWT.RIGHT) {
							e.gc.drawImage(img, pos + width - bounds.width, clientArea.height
									- bounds.height);
							drawWidth -= bounds.width + 2;
						} else {
							e.gc.drawImage(img, pos, clientArea.height - bounds.height);

							if (align == SWT.CENTER) {
								int adj = bounds.width / 2 + 1;
								pos += adj;
								width -= adj;
							} else {
								pos += bounds.width + 2;
								width -= bounds.width + 2;
							}
							drawWidth = width;
						}
					}

					if (text.length() > 0) {
						//Point size = e.gc.textExtent(text);

						Rectangle bounds = new Rectangle(pos, clientArea.y, drawWidth,
								clientArea.height);
						headerArea.setData("Column" + i + "Bounds", bounds);
						GCStringPrinter.printString(e.gc, text, bounds, false, false, align);
					}

					//e.gc.drawLine(pos, bounds.y, pos, bounds.y + bounds.height);
					pos += width + (ListRow.MARGIN_WIDTH * 2);
				}
			}
		});
	}

	/**
	 * @param i
	 */
	protected void moveFocus(int relative, boolean moveall) {
		int index;

		if (moveall) {
			System.err.println("moveall not supported "
					+ Debug.getCompressedStackTrace());
		}

		selectedRows_mon.enter();
		try {
			if (selectedRows.size() == 0) {
				return;
			}

			ListRow focusedRow = getRowFocused();
			ListRow firstRow = (ListRow) selectedRows.get(0);

			index = indexOf(firstRow) + relative;
			if (index < 0) {
				index = 0;
			}

			if (index >= rows.size()) {
				if (index == 0) {
					return;
				}
				index = rows.size() - 1;
			}

			ListRow newRow = (ListRow) rows.get(index);
			setSelectedRows(new ListRow[] { newRow
			});

			if (firstRow.equals(focusedRow)) {
				newRow.setFocused(true);
			}
		} finally {
			selectedRows_mon.exit();
		}
	}

	public void refreshAll(final boolean doGraphics) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				//				String s = "";
				for (int i = 0; i < rows.size(); i++) {
					ListRow row = (ListRow) rows.get(i);
					//					if (row.isVisible()) {
					//						s += i + ";";
					//					}
					row.refresh(doGraphics);
				}
				//System.out.println(sTableID + "; vis=" + s);
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
			row_mon.enter();
			if (dataSourcesToAdd.size() > 0) {
				dataSourcesAdd = dataSourcesToAdd.toArray();
				dataSourcesToAdd.clear();

				// remove the ones we are going to add then delete
				if (dataSourcesToRemove != null && dataSourcesToRemove.size() > 0) {
					for (int i = 0; i < dataSourcesAdd.length; i++) {
						if (dataSourcesToRemove.contains(dataSourcesAdd[i])) {
							dataSourcesToRemove.remove(dataSourcesAdd[i]);
							dataSourcesAdd[i] = null;
							if (DEBUGADDREMOVE) {
								System.out.println(sTableID
										+ ": Saved time by not adding a row that was removed");
							}
						}
					}
				}
			}

			if (dataSourcesToRemove != null && dataSourcesToRemove.size() > 0) {
				dataSourcesRemove = dataSourcesToRemove.toArray();
				if (DEBUGADDREMOVE && dataSourcesRemove.length > 1) {
					System.out.println(sTableID + ": Streamlining removing "
							+ dataSourcesRemove.length + " rows");
				}
				dataSourcesToRemove.clear();
			}
		} finally {
			row_mon.exit();
		}

		if (dataSourcesAdd != null) {
			addDataSources(dataSourcesAdd, true, -1);
			if (DEBUGADDREMOVE && dataSourcesAdd.length > 1) {
				System.out.println(sTableID + ": Streamlined adding "
						+ dataSourcesAdd.length + " rows");
			}
		}

		if (dataSourcesRemove != null) {
			// for now, remove one at a time
			// TODO: all at once
			for (int i = 0; i < dataSourcesRemove.length; i++) {
				Object ds = dataSourcesRemove[i];
				removeDataSource(ds, true);
			}
		}
	}

	public void addDataSource(final Object datasource, boolean bImmediate) {
		addDataSources(new Object[] { datasource
		}, bImmediate, -1);
	}

	public void addDataSource(final Object datasource, boolean bImmediate, int pos) {
		addDataSources(new Object[] { datasource
		}, bImmediate, pos);
	}

	public void addDataSources(final Object[] dataSources, boolean bImmediate) {
		addDataSources(dataSources, bImmediate, -1);
	}

	public void addDataSources(final Object[] dataSources, boolean bImmediate,
			final int pos) {
		if (dataSources == null) {
			return;
		}

		// In order to save time, we cache entries to be added and process them
		// in a refresh cycle.  This is a huge benefit to tables that have
		// many rows being added and removed in rapid succession
		if (!bImmediate) {
			int count;
			if (DEBUGADDREMOVE) {
				count = 0;
			}

			try {
				row_mon.enter();

				if (dataSourcesToAdd == null) {
					dataSourcesToAdd = new ArrayList(4);
				}
				for (int i = 0; i < dataSources.length; i++) {
					if (!mapDataSourceToRow.containsKey(dataSources[i])) {
						dataSourcesToAdd.add(dataSources[i]);
						if (DEBUGADDREMOVE) {
							count++;
						}
					}
				}

				if (DEBUGADDREMOVE && count > 0) {
					System.out.println(sTableID + ": Queueing " + count
							+ " dataSources to add");
				}
				return;

			} finally {
				row_mon.exit();
			}
		}

		Utils.execSWTThread(new Runnable() {
			public void run() {
				try {
					row_mon.enter();
					for (int i = 0; i < dataSources.length; i++) {
						Object datasource = dataSources[i];

						if (datasource == null
								|| mapDataSourceToRow.containsKey(datasource)) {
							continue;
						}

						int newPos = (pos >= 0) ? pos : rows.size();
						ListRow row = new ListRow(ListView.this, listComposite, newPos,
								datasource);
						rows.add(newPos, row);
						//System.out.println("addDS pos " + newPos);

						row.fixupPosition();

						mapDataSourceToRow.put(datasource, row);

						new selectionListener(row);

						triggerListenerRowAdded(row);
					}
				} finally {
					row_mon.exit();
					listComposite.layout();
					// SWT Bug on OSX with computeSize being higher than needed..
					Point point = listComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT);
					point.y = ListRow.ROW_HEIGHT * rows.size();
					sc.setMinSize(point);
					//sc.setMinSize(listComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
				}
				//System.out.println(Debug.getCompressedStackTrace());
			}
		});
	}

	public void removeDataSource(final Object datasource, boolean bImmediate) {
		Utils.execSWTThread(new Runnable() {
			public void run() {
				try {
					row_mon.enter();
					ListRow row = (ListRow) mapDataSourceToRow.remove(datasource);
					if (row != null) {
						// Delete row before removing in case delete(..) calls back a method
						// which needs rows.
						row.setSelected(false);
						row.setFocused(false);
						row.delete(false);
						rows.remove(row);

						triggerListenerRowRemoved(row);
					} else {
						//System.out.println("not found " + datasource);
					}
				} finally {
					row_mon.exit();
					// SWT Bug on OSX with computeSize being higher than needed..
					listComposite.layout();
					Point point = listComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT);
					point.y = ListRow.ROW_HEIGHT * rows.size();
					sc.setMinSize(point);
				}
			}
		});
	}

	public void removeAllDataSources(boolean bImmediate) {
		Utils.execSWTThread(new Runnable() {
			public void run() {

				row_mon.enter();

				try {
					for (Iterator iterator = mapDataSourceToRow.keySet().iterator(); iterator.hasNext();) {
						Object datasource = iterator.next();
						ListRow row = (ListRow) mapDataSourceToRow.get(datasource);

						if (row != null) {
							rows.remove(row);
							row.setSelected(false);
							row.setFocused(false);
							row.delete(false);
						}
					}

					mapDataSourceToRow.clear();
					rows.clear();
				} finally {
					row_mon.exit();
				}
			}
		}, !bImmediate);
	}

	private class selectionListener implements Listener
	{
		private final ListRow row;

		// XXX Copied from TableView!
		private TableCellMouseEvent createMouseEvent(TableCellCore cell, Event e,
				int type) {
			TableCellMouseEvent event = new TableCellMouseEvent();
			event.cell = cell;
			event.eventType = type;
			event.button = e.button;
			// TODO: Change to not use SWT masks
			event.keyboardState = e.stateMask;
			event.skipCoreFunctionality = false;
			Rectangle r = cell.getBounds();
			event.x = e.x - r.x;
			event.y = e.y - r.y;
			return event;
		}

		public selectionListener(ListRow row) {
			this.row = row;
			addListenerAndChildren(row.getComposite(), SWT.MouseDown, this);
			addListenerAndChildren(row.getComposite(), SWT.MouseUp, this);
			addListenerAndChildren(row.getComposite(), SWT.MouseDoubleClick, this);
			//addListenerAndChildren(row.getComposite(), SWT.Traverse, this);
			addListenerAndChildren(row.getComposite(), SWT.DefaultSelection, this);
			//addListenerAndChildren(row.getComposite(), SWT.FocusIn, this);
			//addListenerAndChildren(row.getComposite(), SWT.KeyDown, this);
		}

		public void handleEvent(Event e) {
			int mouseEventType = -1;
			switch (e.type) {
				case SWT.MouseDown: {
					mouseEventType = TableCellMouseEvent.EVENT_MOUSEDOWN;
					break;
				}

				case SWT.MouseDoubleClick: {
					mouseEventType = TableCellMouseEvent.EVENT_MOUSEDOUBLECLICK;
					break;
				}

				case SWT.MouseUp: {
					if ((e.stateMask & SWT.MOD1) > 0) { // control
						boolean select = !row.isSelected();
						row.setSelected(select);
						if (select) {
							row.setFocused(true);
						}
					} else if ((e.stateMask & SWT.MOD2) > 0) { // shift
						ListRow rowFocused = getRowFocused();
						if (rowFocused == null) {
							boolean select = !row.isSelected();
							row.setSelected(select);
							if (select) {
								row.setFocused(true);
							}
						} else {
							int idxStart = rowFocused.getIndex();
							int idxEnd = row.getIndex();

							if (idxEnd != idxStart) {
								int dir = idxStart < idxEnd ? 1 : -1;
								for (int i = idxStart; i != idxEnd; i += dir) {
									ListRow rowToSelect = getRow(i);
									if (rowToSelect != null) {
										rowToSelect.setSelected(true);
									}
								}

								ListRow rowToSelect = getRow(idxEnd);
								if (rowToSelect != null) {
									rowToSelect.setSelected(true);
									rowToSelect.setFocused(true);
								}
							}

						}
					} else {
						setSelectedRows(new ListRow[] { row
						});
					}
					if (listComposite.isDisposed()) {
						return;
					}
					listComposite.setFocus();
					mouseEventType = TableCellMouseEvent.EVENT_MOUSEUP;
					break;
				}

				case SWT.Traverse: {
					e.doit = true;
					System.out.println("Traverse: " + e.detail);
					break;
				}

				case SWT.DefaultSelection: {
					System.out.println("defaultse");
					break;
				}

				case SWT.FocusIn: {
					System.out.println("FI");
					row.setSelected(true);
					break;
				}
			}

			if (mouseEventType != -1) {
				TableCellCore cell = row.getTableCellCore(e.x, e.y);
				if (cell != null) {
					TableColumn tc = cell.getTableColumn();
					TableCellMouseEvent event = createMouseEvent(cell, e, mouseEventType);
					((TableColumnCore) tc).invokeCellMouseListeners(event);
					cell.invokeMouseListeners(event);
					if (event.skipCoreFunctionality) {
						lCancelSelectionTriggeredOn = System.currentTimeMillis();
					}
				}
			}

			if (e.type == SWT.DefaultSelection || e.type == SWT.MouseDoubleClick) {
				_runDefaultAction();
			}

			if (bMouseClickIsDefaultSelection && e.type == SWT.MouseUp) {
				_runDefaultAction();
			}
		}
	}

	private void addListenerAndChildren(Composite composite, int eventType,
			Listener listener) {
		composite.addListener(eventType, listener);

		Control[] children = composite.getChildren();
		for (int i = 0; i < children.length; i++) {
			Control child = children[i];
			child.addListener(eventType, listener);
			if (child instanceof Composite) {
				addListenerAndChildren((Composite) child, eventType, listener);
			}
		}
	}

	public int indexOf(ListRow row) {
		return rows.indexOf(row);
	}

	public int size(boolean bIncludeQueue) {
		int size = rows.size();

		if (bIncludeQueue) {
			if (dataSourcesToAdd != null) {
				size += dataSourcesToAdd.size();
			}
			if (dataSourcesToRemove != null) {
				size += dataSourcesToRemove.size();
			}
		}
		return size;
	}

	public ListRow getRow(int i) {
		if (i < 0 || i >= rows.size()) {
			return null;
		}
		return (ListRow) rows.get(i);
	}

	/**
	 * Get the row associated with a datasource
	 * @param dataSource a reference to a core Datasource object 
	 * 										(not a plugin datasource object)
	 * @return The row, or null
	 */
	public ListRow getRow(Object dataSource) {
		return (ListRow) mapDataSourceToRow.get(dataSource);
	}

	public boolean dataSourceExists(Object dataSource) {
		return mapDataSourceToRow.containsKey(dataSource)
				|| dataSourcesToAdd.contains(dataSource);
	}

	public SWTSkinProperties getSkinProperties() {
		return skinProperties;
	}

	public TableColumnCore[] getVisibleColumns() {
		if (visibleColumns == null) {
			return new TableColumnCore[0];
		}

		return visibleColumns;
	}

	public void updateColumnList(TableColumnCore[] columns,
			String defaultSortColumnID) {
		// XXX Adding Columns only has to be done once per TableID.  
		// Doing it more than once won't harm anything, but it's a waste.
		TableColumnManager tcManager = TableColumnManager.getInstance();
		if (tcManager.getTableColumnCount(sTableID) != columns.length) {
			for (int i = 0; i < columns.length; i++) {
				columns[i].setTableID(sTableID);
				tcManager.addColumn(columns[i]);
			}
		}

		// fixup order
		tcManager.ensureIntegrety(sTableID);

		allColumns = columns;
		//visibleColumns = tcManager.getAllTableColumnCoreAsArray(sTableID);

		ArrayList visibleColumnsList = new ArrayList();
		for (int i = 0; i < columns.length; i++) {
			if (columns[i].getPosition() >= 0) {
				visibleColumnsList.add(columns[i]);
			}
		}
		visibleColumns = (TableColumnCore[]) visibleColumnsList.toArray(new TableColumnCore[0]);
		// TODO: Refresh all rows

		// Initialize the sorter after the columns have been added
		String sSortColumn = defaultSortColumnID;
		boolean bSortAscending = false;
		// For now, set to default column, until we have a way to select sorting
		// on a non-visible column
		//configMan.getStringParameter(sTableID + ".sortColumn", defaultSortColumnID);
		//int iSortDirection = configMan.getIntParameter(CFG_SORTDIRECTION);
		//boolean bSortAscending = configMan.getBooleanParameter(sTableID
				//+ ".sortAsc", iSortDirection == 1 ? false : true);

		TableColumnCore tc = tcManager.getTableColumnCore(sTableID, sSortColumn);
		if (tc == null) {
			tc = visibleColumns[0];
		}
		sortColumn = tc;
		sortColumn.setSortAscending(bSortAscending);
		changeColumnIndicator();
	}

	/**
	 * 
	 */
	private void changeColumnIndicator() {
		if (headerArea != null && !headerArea.isDisposed()) {
			headerArea.redraw();
		}
	}

	public ListRow[] getSelectedRows() {
		selectedRows_mon.enter();
		try {
			ListRow[] rows = new ListRow[selectedRows.size()];
			rows = (ListRow[]) selectedRows.toArray(rows);
			return rows;
		} finally {
			selectedRows_mon.exit();
		}
	}

	public void setSelectedRows(ListRow[] rows) {
		selectedRows_mon.enter();
		try {
			ArrayList rowsToSelect = new ArrayList();
			for (int i = 0; i < rows.length; i++) {
				rowsToSelect.add(rows[i]);
			}
			ListRow[] selectedRows = getSelectedRows();

			// unselect already selected rows that aren't going to be selected anymore
			for (int i = 0; i < selectedRows.length; i++) {
				ListRow selectedRow = selectedRows[i];
				boolean bStillSelected = false;
				for (int j = 0; j < rows.length; j++) {
					ListRow row = rows[j];
					if (row.equals(selectedRow)) {
						bStillSelected = true;
						break;
					}
				}
				if (!bStillSelected) {
					selectedRow.setSelected(false);
				} else {
					rowsToSelect.remove(selectedRow);
				}
			}

			for (Iterator iter = rowsToSelect.iterator(); iter.hasNext();) {
				ListRow row = (ListRow) iter.next();
				row.setSelected(true);
			}

			if (rows.length > 0) {
				rows[0].setFocused(true);
			}
		} finally {
			selectedRows_mon.exit();
		}
	}

	/**
	 * XXX DO NOT CALL FROM LISTVIEW!
	 * 
	 * Adds row to selection list
	 */
	protected void rowSetSelected(ListRow row, boolean bSelected) {
		if (row == null || (selectedRows.indexOf(row) >= 0) == bSelected) {
			return;
		}
		selectedRows_mon.enter();
		try {
			if (bSelected) {
				selectedRows.add(row);
			} else {
				selectedRows.remove(row);
			}
		} finally {
			selectedRows_mon.exit();
		}

		for (Iterator iter = listenersSelection.iterator(); iter.hasNext();) {
			ListSelectionAdapter l = (ListSelectionAdapter) iter.next();
			try {
				if (bSelected) {
					l.selected(row);
				} else {
					l.deselected(row);
				}
			} catch (Exception e) {
				Debug.out(e);
			}
		}
	}

	public ListRow getRowFocused() {
		return rowFocused;
	}

	protected void rowSetFocused(ListRow row) {
		rowFocused = row;

		if (row != null) {
			rowShow(row);
		}

		for (Iterator iter = listenersSelection.iterator(); iter.hasNext();) {
			ListSelectionAdapter l = (ListSelectionAdapter) iter.next();
			try {
				l.focusChanged(row);
			} catch (Exception e) {
				Debug.out(e);
			}
		}
	}

	public boolean rowIsSelected(ListRow row) {
		selectedRows_mon.enter();
		return selectedRows.indexOf(row) >= 0;
	}

	public void rowShow(ListRow row) {
		// move into view
		if (row == null) {
			return;
		}

		Control child = row.getComposite();
		if (child == null || child.isDisposed()) {
			return;
		}

		Rectangle bounds = child.getBounds();
		Rectangle area = sc.getClientArea();
		Point origin = sc.getOrigin();
		if (origin.x > bounds.x) {
			origin.x = Math.max(0, bounds.x);
		}
		if (origin.y > bounds.y) {
			origin.y = Math.max(0, bounds.y);
		}
		if (origin.x + area.width < bounds.x + bounds.width) {
			origin.x = Math.max(0, bounds.x + bounds.width - area.width);
		}
		if (origin.y + area.height < bounds.y + bounds.height) {
			origin.y = Math.max(0, bounds.y + bounds.height - area.height);
		}
		sc.setOrigin(origin);
	}

	public boolean isFocused() {
		return listComposite.isFocusControl();
	}

	// @see com.aelitis.azureus.ui.swt.utils.UIUpdatable#getUpdateUIName()
	public String getUpdateUIName() {
		return "ListView";
	}

	// @see com.aelitis.azureus.ui.swt.utils.UIUpdatable#updateUI()
	public void updateUI() {
		if (listComposite.isDisposed()) {
			return;
		}
		processDataSourceQueue();

		iGraphicRefresh++;
		boolean bDoGraphics = (iGraphicRefresh % graphicsUpdate) == 0;
		refreshAll(bDoGraphics);

		sortTable();
	}

	// XXX This gets called a lot.  Could store location and size on 
	//     resize/scroll of sc
	public Rectangle getBounds() {
		Point location = sc.getContent().getLocation();
		Point size = sc.getSize();
		return new Rectangle(-location.x, -location.y, size.x, size.y);
	}

	public Rectangle getClientArea() {
		return sc.getClientArea();
	}

	// @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
	public void handleEvent(Event event) {
		if (event.type == SWT.FocusIn || event.type == SWT.FocusOut) {
			selectedRows_mon.enter();
			try {
				for (Iterator iter = selectedRows.iterator(); iter.hasNext();) {
					ListRow row = (ListRow) iter.next();
					if (row != null) {
						row.repaint();
					}
				}
			} finally {
				selectedRows_mon.exit();
			}
		} else if (event.type == SWT.Traverse) {
			event.doit = true;

			switch (event.detail) {
				case SWT.TRAVERSE_ARROW_NEXT:
					if ((event.stateMask & SWT.MOD2) > 0) { // shift
						ListRow focusedRow = getRowFocused();
						if (focusedRow != null) {
							int index = focusedRow.getIndex();
							index++;
							ListRow nextRow = getRow(index);
							if (nextRow != null) {
								if (nextRow.isSelected()) {
									focusedRow.setSelected(false);
								}

								nextRow.setSelected(true);
								nextRow.setFocused(true);
							}
						}
					} else if ((event.stateMask & SWT.MOD1) > 0) { // control
						ListRow focusedRow = getRowFocused();
						if (focusedRow != null) {
							int index = focusedRow.getIndex();
							index++;
							ListRow nextRow = getRow(index);
							if (nextRow != null) {
								nextRow.setFocused(true);
							}
						}
					} else {
						moveFocus(1, false);
					}
					break;

				case SWT.TRAVERSE_ARROW_PREVIOUS:
					if ((event.stateMask & SWT.MOD2) > 0) {
						ListRow activeRow = getRowFocused();
						if (activeRow != null) {
							int index = activeRow.getIndex();
							index--;
							ListRow previousRow = getRow(index);
							if (previousRow != null) {
								if (previousRow.isSelected()) {
									activeRow.setSelected(false);
								}
								previousRow.setSelected(true);
								previousRow.setFocused(true);
							}
						}
					} else if ((event.stateMask & SWT.MOD1) > 0) { // control
						ListRow focusedRow = getRowFocused();
						if (focusedRow != null) {
							int index = focusedRow.getIndex();
							index--;
							ListRow nextRow = getRow(index);
							if (nextRow != null) {
								nextRow.setFocused(true);
							}
						}
					} else {
						moveFocus(-1, false);
					}
					break;

				case SWT.TRAVERSE_RETURN:
					_runDefaultAction();
					break;

				default:
					System.out.println("TR" + event.detail);

			}
		} else if (event.type == SWT.KeyDown) {
			int key = event.character;
			if (key <= 26 && key > 0)
				key += 'a' - 1;

			if (event.stateMask == SWT.MOD1) { // Control/Command
				switch (event.keyCode) {
					case 'a': // select all
						setSelectedRows(getRowsUnsorted());
						break;

					case ' ':
						event.doit = false;
						ListRow focusedRow = getRowFocused();
						if (focusedRow != null) {
							focusedRow.setSelected(!focusedRow.isSelected());
						}
						break;
				}

			} else {
				switch (event.keyCode) {
					case SWT.PAGE_UP:
						moveFocus(sc.getSize().y / -30, false); // TODO: Use real height
						break;

					case SWT.PAGE_DOWN:
						moveFocus(sc.getSize().y / 30, false); // TODO: Use real height
						break;

					case SWT.HOME:
						if (event.stateMask == SWT.CONTROL) {
							setSelectedRows(new ListRow[] { (ListRow) rows.get(0)
							});
						}
						break;

					case SWT.END:
						if (event.stateMask == SWT.CONTROL) {
							int i = rows.size();
							if (i > 0) {
								setSelectedRows(new ListRow[] { (ListRow) rows.get(i - 1)
								});
							}
						}
						break;
				}
			}
		} else if (event.type == SWT.DefaultSelection
				|| event.type == SWT.MouseDoubleClick) {
			_runDefaultAction();
		}

	}

	private void _runDefaultAction() {
		// plugin may have cancelled the default action

		if (lCancelSelectionTriggeredOn > 0
				&& System.currentTimeMillis() - lCancelSelectionTriggeredOn < 200) {
			lCancelSelectionTriggeredOn = -1;
		} else {
			ListRow[] selectedRows = getSelectedRows();
			for (Iterator iter = listenersSelection.iterator(); iter.hasNext();) {
				ListSelectionAdapter l = (ListSelectionAdapter) iter.next();
				l.defaultSelected(selectedRows);
			}
		}
	}

	public void setMouseClickIsDefaultSelection(boolean b) {
		bMouseClickIsDefaultSelection = b;
	}

	public void addSelectionListener(ListSelectionAdapter listener,
			boolean bFireSelection) {
		listenersSelection.add(listener);
		if (bFireSelection) {
			ListRow[] rows = getSelectedRows();
			for (int i = 0; i < rows.length; i++) {
				listener.selected(rows[i]);
			}

			listener.focusChanged(getRowFocused());
		}
	}

	public void addCountChangeListener(ListCountChangeAdapter listener) {
		listenersCountChange.add(listener);
	}

	protected void triggerListenerRowAdded(ListRow row) {
		for (Iterator iter = listenersCountChange.iterator(); iter.hasNext();) {
			ListCountChangeAdapter l = (ListCountChangeAdapter) iter.next();
			l.rowAdded(row);
		}
	}

	protected void triggerListenerRowRemoved(ListRow row) {
		for (Iterator iter = listenersCountChange.iterator(); iter.hasNext();) {
			ListCountChangeAdapter l = (ListCountChangeAdapter) iter.next();
			l.rowRemoved(row);
		}
	}

	/**
	 * Retrieve the control that the rows are added to.
	 * 
	 * @return
	 */
	public Control getControl() {
		return listComposite;
	}

	public ScrolledComposite getScrolledComposite() {
		return sc;
	}

	public String getTableID() {
		return sTableID;
	}

	/** Get all the rows for this table
	 *
	 * @return a list of TableRowCore objects
	 */
	public ListRow[] getRowsUnsorted() {
		try {
			row_mon.enter();

			return (ListRow[]) rows.toArray(new ListRow[0]);

		} finally {
			row_mon.exit();
		}
	}

	/** For every row source, run the code provided by the specified 
	 * parameter.
	 *
	 * @param runner Code to run for each row/datasource
	 */
	public void runForAllRows(GroupTableRowRunner runner) {
		// put to array instead of synchronised iterator, so that runner can remove
		TableRowCore[] rows = getRowsUnsorted();

		for (int i = 0; i < rows.length; i++) {
			runner.run(rows[i]);
		}
	}

	public void columnInvalidate(TableColumnCore tableColumn) {
		// TODO Auto-generated method stub

	}

	public void columnOrderChanged(int[] iPositions) {
		// TODO Auto-generated method stub

	}

	public void columnSizeChanged(TableColumnCore tableColumn) {
		if (tableColumn.getPosition() < 0) {
			return;
		}

		final String id = tableColumn.getName();
		final int width = tableColumn.getWidth();

		final int position = tableColumn.getPosition();
		final int numColumns = visibleColumns.length;

		runForAllRows(new GroupTableRowRunner() {
			public void run(TableRowCore row) {
				TableCellCore cell = row.getTableCellCore(id);
				if (cell != null) {
					Rectangle bounds = cell.getBounds();
					int diff = width - bounds.width;
					if (diff != 0) {
						bounds.width = width;
						((ListCell) cell.getBufferedTableItem()).setBounds(bounds);
						cell.refresh(true);

						for (int i = position + 1; i < numColumns; i++) {
							TableColumnCore nextColumn = visibleColumns[i];
							TableCellCore nextCell = row.getTableCellCore(nextColumn.getName());
							if (nextCell != null) {
								Rectangle nextBounds = nextCell.getBounds();
								nextBounds.x += diff;
								((ListCell) nextCell.getBufferedTableItem()).setBounds(nextBounds);
								nextCell.refresh(true);
							}
						}
					}
				}
			}
		});
	}

	public void tableStructureChanged() {
	}

	public TableColumnCore getSortColumn() {
		return sortColumn;
	}

	public void setSortColumn(TableColumnCore sorter) {
		boolean bSameColumn = sortColumn.equals(sorter);
		if (!bSameColumn) {
			sortColumn = sorter;
			int iSortDirection = configMan.getIntParameter(CFG_SORTDIRECTION);
			if (iSortDirection == 0)
				sortColumn.setSortAscending(true);
			else if (iSortDirection == 1)
				sortColumn.setSortAscending(false);
			else
				sortColumn.setSortAscending(!sortColumn.isSortAscending());

			configMan.setParameter(sTableID + ".sortAsc",
					sortColumn.isSortAscending());
			configMan.setParameter(sTableID + ".sortColumn", sortColumn.getName());
		} else {
			sortColumn.setSortAscending(!sortColumn.isSortAscending());
			configMan.setParameter(sTableID + ".sortAsc",
					sortColumn.isSortAscending());
		}

		sortColumn.setLastSortValueChange(SystemTime.getCurrentTime());

		changeColumnIndicator();
		sortTable();
	}

	public void sortTable() {
		long lTimeStart;
		if (DEBUG_SORTER) {
			//System.out.println(">>> Sort.. ");
			lTimeStart = System.currentTimeMillis();
		}

		try {
			row_mon.enter();

			if (sortColumn != null
					&& sortColumn.getLastSortValueChange() > lLastSortedOn) {
				lLastSortedOn = SystemTime.getCurrentTime();

				// 1) Copy rows to array and sort
				// 2) check if any have changed position
				// 3) make row sort permanent (move sorted array back into rows field)
				// 4) tell each row affected to fix itself up

				if (sortColumn != null) {
					String sColumnID = sortColumn.getName();
					for (Iterator iter = rows.iterator(); iter.hasNext();) {
						TableRowCore row = (TableRowCore) iter.next();
						TableCellCore cell = row.getTableCellCore(sColumnID);
						if (cell != null) {
							cell.refresh(true, true, true);
						}
					}
				}

				// Since rows alternate in color, all rows including and after the
				// first changed row need to be notified to change.

				Object[] rowsArray = rows.toArray();
				Arrays.sort(rowsArray, sortColumn);

				//Collections.sort(rows, sortColumn);
				if (DEBUG_SORTER) {
					long lTimeDiff = (System.currentTimeMillis() - lTimeStart);
					if (lTimeDiff > 100)
						System.out.println("--- Build & Sort took " + lTimeDiff + "ms");
					lTimeStart = System.currentTimeMillis();
				}

				int iNumChanged = 0;
				int iFirstChange = -1;
				for (int i = 0; i < rowsArray.length; i++) {
					ListRow row = (ListRow) rowsArray[i];
					if (row != rows.get(i)) {
						iNumChanged++;
						if (iFirstChange < 0) {
							iFirstChange = i;
						}
					}
				}

				List list = Arrays.asList(rowsArray);
				if (list instanceof ArrayList) {
					rows = (ArrayList) list;
				} else {
					rows = new ArrayList(list);
				}

				if (iFirstChange >= 0) {
					for (int i = iFirstChange; i < rows.size(); i++) {
						ListRow row = (ListRow) rows.get(i);
						row.fixupPosition();
					}

					if (DEBUG_SORTER) {
						long lTimeDiff = (System.currentTimeMillis() - lTimeStart);
						System.out.println("Sort made " + iNumChanged + " rows move in "
								+ lTimeDiff + "ms");
					}

					listComposite.layout();
				}
			}

		} finally {
			row_mon.exit();
		}

		// Selection should be okay still.  May need to be moved into view
		// if we want that behaviour
	}

	public TableColumnCore[] getAllColumns() {
		return allColumns;
	}
}
