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
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
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
	private final static LogIDs LOGID = LogIDs.UI3;

	private static final boolean DEBUGADDREMOVE = false;

	// Shorter name for ConfigManager, easier to read code
	private static final ConfigurationManager configMan = ConfigurationManager.getInstance();

	private static final String CFG_SORTDIRECTION = "config.style.table.defaultSortOrder";

	private static final boolean DEBUG_SORTER = false;

	protected static final boolean DELAY_SCROLL = false;

	private Canvas listCanvas;

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

	private ScrollBar vBar;

	private Image imageView = null;

	private int iLastVBarPos;

	protected boolean bRestartRefreshVisible = false;

	private boolean bInRefreshVisible;

	public ListView(final String sTableID, SWTSkinProperties skinProperties,
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

		listCanvas = new Canvas(parent, SWT.NO_BACKGROUND | SWT.NO_REDRAW_RESIZE
				| SWT.V_SCROLL);
		listCanvas.setLayout(new FormLayout());

		formData = new FormData();
		formData.left = new FormAttachment(0);
		formData.top = new FormAttachment(0);
		formData.right = new FormAttachment(100);
		formData.bottom = new FormAttachment(100);
		listCanvas.setLayoutData(formData);

		vBar = listCanvas.getVerticalBar();
		vBar.addListener(SWT.Selection, new Listener() {
			private TimerEvent event;

			public void handleEvent(Event e) {
				if (DELAY_SCROLL) {
					if (event == null || event.hasRun() || event.isCancelled()) {
						event = SimpleTimer.addEvent("Scrolling",
								SystemTime.getOffsetTime(0), new TimerEventPerformer() {
									public void perform(TimerEvent event) {
										event.cancel();
										event = null;

										Utils.execSWTThread(new AERunnable() {
											public void runSupport() {
												scrollTo(vBar.getSelection());
											}
										});
									}
								});
					} else {
						System.out.println("save");
					}
				} else {
					scrollTo(vBar.getSelection());
				}
			}
		});
		vBar.setVisible(false);

		listCanvas.addListener(SWT.Resize, new Listener() {
			int w = listCanvas.getSize().x;

			public void handleEvent(Event e) {
				refreshScrollbar();
				int nw = listCanvas.getSize().x;
				if (w != nw) {
					w = nw;
					listCanvas.redraw();
				}
			}
		});

		listCanvas.addListener(SWT.Paint, new Listener() {
			Rectangle lastBounds = new Rectangle(0, 0, 0, 0);

			public void handleEvent(Event e) {
				boolean bNeedsRefresh = false;

				Rectangle clientArea = listCanvas.getClientArea();
				if (imageView == null) {
					imageView = new Image(e.gc.getDevice(), clientArea);
					bNeedsRefresh = true;
					e.setBounds(clientArea);
				} else {
					if (!lastBounds.equals(clientArea)) {
						bNeedsRefresh = true;
						Image newImageView = new Image(e.gc.getDevice(), clientArea);
						if (lastBounds.width == clientArea.width) {
							GC gc = new GC(newImageView);
							try {
								gc.drawImage(imageView, 0, 0);
							} finally {
								gc.dispose();
							}
						} else {
							e.setBounds(clientArea);
						}
						imageView.dispose();
						imageView = newImageView;
					}
				}

				if (bNeedsRefresh) {
					lastBounds = clientArea;

					GC gc = new GC(imageView);
					try {
						gc.setForeground(e.gc.getForeground());
						gc.setBackground(e.gc.getBackground());

						TableRowCore[] visibleRows = getVisibleRows();
						if (visibleRows.length > 0) {
							//gc.setClipping(e.gc.getClipping());
							int ofs = getOffset(iLastVBarPos);

							int start = e.y / ListRow.ROW_HEIGHT;
							int end = (e.y + e.height) / ListRow.ROW_HEIGHT + 1;
							if (end > visibleRows.length) {
								end = visibleRows.length;
							}

							//System.out.println(sTableID + "] " + visibleRows.length
							//		+ " visible;" + "; " + start + " - " + (end - 1));
							long lStart = System.currentTimeMillis();
							for (int i = start; i < end; i++) {
								TableRowCore row = visibleRows[i];

								//gc.setBackground(e.gc.getDevice().getSystemColor(i));
								//gc.fillRectangle(0,0,800,38);

								//row.invalidate();
								row.doPaint(gc, true);
							}

							int endY = visibleRows.length * ListRow.ROW_HEIGHT + ofs;
							if (endY < clientArea.height) {
								//System.out.println("fill " + (clientArea.height - endY) + "@" + endY);
								gc.fillRectangle(0, endY, clientArea.width, clientArea.height
										- endY);
							}

							long diff = System.currentTimeMillis() - lStart;
							if (diff > 15) {
								System.out.println(diff + "ms to paint" + start + " - "
										+ (end - 1));
							}
						} else {
							//System.out.println("fillall");
							gc.fillRectangle(clientArea);
						}
					} finally {
						gc.dispose();
					}
				}

				e.gc.drawImage(imageView, e.x, e.y, e.width, e.height, e.x, e.y,
						e.width, e.height);
			}
		});

		selectionListener l = new selectionListener();
		listCanvas.addListener(SWT.MouseDown, l);
		listCanvas.addListener(SWT.MouseUp, l);

		listCanvas.addListener(SWT.MouseDoubleClick, this);
		listCanvas.addListener(SWT.FocusIn, this);
		listCanvas.addListener(SWT.FocusOut, this);
		listCanvas.addListener(SWT.Traverse, this);
		listCanvas.addListener(SWT.DefaultSelection, this);
		listCanvas.addListener(SWT.KeyDown, this); // so we are a tab focus

		listCanvas.setMenu(createMenu());

		UIUpdaterFactory.getInstance().addUpdater(this);
		TableStructureEventDispatcher.getInstance(sTableID).addListener(this);
	}

	private int getOffset(int i) {
		int ofs = (i % ListRow.ROW_HEIGHT);
		if (ofs == 0)
			return 0;
		return ofs;
	}

	/**
	 * 
	 */
	protected void refreshScrollbar() {
		Rectangle client = listCanvas.getClientArea();
		int h = rows.size() * ListRow.ROW_HEIGHT - client.height;

		if (h <= client.height || client.height == 0) {
			if (vBar.isVisible()) {
				vBar.setVisible(false);
				listCanvas.redraw();
				headerArea.redraw();
			}
		} else {
			if (!vBar.isVisible()) {
				vBar.setVisible(true);
				listCanvas.redraw();
				headerArea.redraw();
			}
			vBar.setIncrement(ListRow.ROW_HEIGHT);
			vBar.setMaximum(h);
			vBar.setThumb(1);
			//vBar.setThumb(client.height - ListRow.ROW_HEIGHT);
			vBar.setPageIncrement(client.height / 2);
			//vBar.setThumb(Math.min(h, client.height));
			if (iLastVBarPos != vBar.getSelection()) {
				scrollTo(vBar.getSelection());
			} else {
				listCanvas.redraw();
				refreshVisible(true, true);
			}
		}
	}

	private void scrollTo(int pos) {
		if (!vBar.getVisible()) {
			return;
		}

		if (pos == iLastVBarPos && pos == vBar.getSelection()) {
			return;
		}

		if (pos < 0) {
			System.err.println("scrollto " + pos + " via "
					+ Debug.getCompressedStackTrace());
			pos = 0;
		} else {
			//System.out.println("scrollto " + pos + "of" + vBar.getMaximum() + " via "
			//		+ Debug.getCompressedStackTrace());
		}
		if (pos != vBar.getSelection()) {
			vBar.setSelection(pos);
		}
		iLastVBarPos = vBar.getSelection();

		listCanvas.redraw();
		refreshVisible(true, true);
	}

	/**
	 * @param headerArea
	 */
	protected void setupHeader(final Composite headerArea) {
		this.headerArea = headerArea;
		FormData formData;

		final Cursor cursor = new Cursor(headerArea.getDisplay(), SWT.CURSOR_HAND);
		headerArea.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				Utils.disposeSWTObjects(new Object[] { cursor
				});
			}
		});

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

		headerArea.addMouseMoveListener(new MouseMoveListener() {
			Cursor cursor = null;

			public void mouseMove(MouseEvent e) {
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
				Cursor newCursor = (inColumn != -1) ? cursor : null;
				if (cursor != newCursor) {
					headerArea.setCursor(newCursor);
					cursor = newCursor;
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

					//Point size = e.gc.textExtent(text);

					Rectangle bounds = new Rectangle(pos, clientArea.y, drawWidth,
							clientArea.height);
					headerArea.setData("Column" + i + "Bounds", bounds);

					if (text.length() > 0) {
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
		} finally {
			selectedRows_mon.exit();
		}
	}

	public void refreshVisible(final boolean doGraphics,
			final boolean bForceRedraw) {
		if (bInRefreshVisible) {
			bRestartRefreshVisible = true;
			return;
		}
		bInRefreshVisible = true;

		final Display display = listCanvas.getDisplay();
		display.asyncExec(new AERunnable() {
			public void runSupport() {
				final TableRowCore[] visibleRows = getVisibleRows();
				try {
					for (int i = 0; i < visibleRows.length; i++) {
						if (bRestartRefreshVisible) {
							//System.out.println("STOPPED refresh at " + i);
							return;
						}

						final ListRow row = (ListRow) visibleRows[i];
						if (bForceRedraw) {
							row.invalidate();
						}

						display.asyncExec(new AERunnable() {
							public void runSupport() {
								if (bRestartRefreshVisible) {
									//System.out.println("stopped refresh at " + row.getIndex());
									return;
								}

								if (row.isVisible()) {
									//if (row.isVisible() && !row.isValid()) {
									if (rowRefresh(row, doGraphics, bForceRedraw)) {
										//System.out.println("   "  + row.getIndex());
									}
								} else {
									//System.out.println("skipping.. not visible. valid? " + row.isValid());
								}
							}
						});

						//row.refresh(doGraphics);
					}
				} finally {
					bInRefreshVisible = false;

					if (bRestartRefreshVisible) {
						bRestartRefreshVisible = false;
						refreshVisible(doGraphics, bForceRedraw);
					}
				}
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
			addDataSources(dataSourcesAdd, true);
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
		}, bImmediate);
	}

	public void addDataSources(final Object[] dataSources, boolean bImmediate) {
		long lTimeStart = System.currentTimeMillis();

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
				int iFirstChange = -1;

				try {
					row_mon.enter();
					
					for (int i = 0; i < dataSources.length; i++) {
						Object datasource = dataSources[i];

						if (datasource == null
								|| mapDataSourceToRow.containsKey(datasource)) {
							continue;
						}

						ListRow row = new ListRow(ListView.this, listCanvas, datasource);

						if (sortColumn != null) {
							TableCellCore cell = row.getTableCellCore(sortColumn.getName());
							if (cell != null) {
								try {
									cell.invalidate();
									cell.refresh(true);
								} catch (Exception e) {
									Logger.log(new LogEvent(LOGID,
											"Minor error adding a row to table " + sTableID, e));
								}
							}
						}

						int index;
						// If we are >= to the last item, then just add it to the end
						// instead of relying on binarySearch, which may return an item
						// in the middle that also is equal.
						if (rows.size() > 0
								&& sortColumn.compare(row, rows.get(rows.size() - 1)) >= 0) {
							index = rows.size();
						} else {
							index = Collections.binarySearch(rows, row, sortColumn);
							if (index < 0)
								index = -1 * index - 1; // best guess

							if (index > rows.size())
								index = rows.size();
						}

						if (iFirstChange < 0 || iFirstChange > index) {
							iFirstChange = index;
						}

						rows.add(index, row);
						log("addDS pos " + index);
						//System.out.println("addDS pos " + index);

						mapDataSourceToRow.put(datasource, row);

						triggerListenerRowAdded(row);
					}
				} finally {
					row_mon.exit();

					if (iFirstChange >= 0) {
						for (int i = iFirstChange; i < rows.size(); i++) {
							ListRow row = (ListRow) rows.get(i);
							row.fixupPosition();
						}
					}

					refreshScrollbar();
					refreshVisible(true, true);
				}
				//System.out.println(Debug.getCompressedStackTrace());
			}
		});
		long diff = System.currentTimeMillis() - lTimeStart;
		if (diff > 20) {
			System.out.println("addDS(" + dataSources.length + "): " + diff + "ms");
		}
	}

	/**
	 * @param string
	 */
	protected void log(String string) {
		System.out.println(sTableID + "] " + string);
	}

	public void removeDataSource(final Object datasource, boolean bImmediate) {
		Utils.execSWTThread(new Runnable() {
			public void run() {
				try {
					row_mon.enter();
					ListRow row = (ListRow) mapDataSourceToRow.get(datasource);
					if (row != null) {
						ListRow newFocusRow = null;

						int index = row.getIndex();
						if (row.isFocused()) {
							int newIndex = index + 1;
							if (index >= 0) {
								if (newIndex >= mapDataSourceToRow.size()) {
									newIndex -= 2;
								}

								if (newIndex >= 0) {
									newFocusRow = getRow(newIndex);
								}
							}
						}

						row = (ListRow) mapDataSourceToRow.remove(datasource);
						if (row == null) {
							return;
						}

						// Delete row before removing in case delete(..) calls back a method
						// which needs rows.
						row.setSelected(false);
						row.setFocused(false);
						row.delete(false);
						rows.remove(row);

						log("remDS pos " + index + ";" + rows.size());

						triggerListenerRowRemoved(row);

						if (newFocusRow != null) {
							//System.out.println("SR " + newFocusRow.getIndex());
							rowSetFocused(newFocusRow);
							newFocusRow.setSelected(true);
						}

						for (int i = index; i < rows.size(); i++) {
							ListRow fixRow = (ListRow) rows.get(i);
							fixRow.fixupPosition();
						}
					} else {
						//System.out.println("not found " + datasource);
					}
				} finally {
					row_mon.exit();
					refreshScrollbar();
					// TODO: Redraw only if visible or above visible (bg change)

					if (imageView != null && !imageView.isDisposed()) {
						TableRowCore[] visibleRows = getVisibleRows();
						Rectangle clientArea = listCanvas.getClientArea();
						if (visibleRows.length > 0) {
							int ofs = getOffset(iLastVBarPos);
							int endY = visibleRows.length * ListRow.ROW_HEIGHT + ofs;

							if (endY < clientArea.height) {
								GC gc = new GC(imageView);
								try {
									gc.setBackground(listCanvas.getBackground());

									gc.fillRectangle(0, endY, clientArea.width, clientArea.height
											- endY);
								} finally {
									gc.dispose();
								}
							}
						} else {
							GC gc = new GC(imageView);
							try {
								gc.setBackground(listCanvas.getBackground());
								gc.fillRectangle(clientArea);
							} finally {
								gc.dispose();
							}
						}

						listCanvas.redraw();
					}

					refreshVisible(true, true);
				}
			}
		});
	}

	public void removeAllDataSources(boolean bImmediate) {
		Utils.execSWTThread(new Runnable() {
			public void run() {

				row_mon.enter();
				log("removeAll");

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

			int i = cell.getTableRowCore().getIndex();
			int iTopIndex = iLastVBarPos / ListRow.ROW_HEIGHT;
			int ofs = getOffset(iLastVBarPos);
			int y = (i - iTopIndex) * ListRow.ROW_HEIGHT - ofs;

			Rectangle r = cell.getBounds();
			event.x = e.x - r.x;
			event.y = e.y - r.y;
			return event;
		}

		public void handleEvent(Event e) {
			ListRow row = getRow(e.x, e.y);
			if (row == null) {
				return;
			}
			int mouseEventType = -1;
			switch (e.type) {
				case SWT.MouseDown: {
					mouseEventType = TableCellMouseEvent.EVENT_MOUSEDOWN;
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
					if (listCanvas.isDisposed()) {
						return;
					}
					listCanvas.setFocus();
					mouseEventType = TableCellMouseEvent.EVENT_MOUSEUP;
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

	/**
	 * @param x
	 * @param y
	 * @return
	 */
	public ListRow getRow(int x, int y) {
		int pos = (y + iLastVBarPos) / ListRow.ROW_HEIGHT;
		if (pos < rows.size() && pos >= 0) {
			ListRow row = (ListRow) rows.get(pos);
			//System.out.println("getRow; y=" + y + ";sb=" + iLastVBarPos + ";pos="
			//		+ pos + ";" + row);
			return row;
		}

		return null;
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

		ListRow[] selectedRows = getSelectedRows();
		for (int i = 0; i < selectedRows.length; i++) {
			ListRow row = selectedRows[i];
			if (row != null) {
				l.add(row.getDataSource(bCoreDataSource));
			}
		}

		return l;
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
		if (row != null) {
			rowShow(row);
		}

		rowFocused = row;

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

		Rectangle clientArea = listCanvas.getClientArea();
		int iTopIndex = iLastVBarPos / ListRow.ROW_HEIGHT;
		int iBottomIndex = (iLastVBarPos + clientArea.height - 1)
				/ ListRow.ROW_HEIGHT;

		int size = iBottomIndex - iTopIndex + 1;
		if (size <= 0) {
			return;
		}

		int i = row.getIndex();
		if (i > iTopIndex && i < iBottomIndex) {
			return;
		}

		int myPos = (i - iTopIndex) * ListRow.ROW_HEIGHT;

		//System.out.println("rowShow:" + i + ";top=" + iTopIndex + ";b="
		//		+ iBottomIndex + ";" + iLastVBarPos);

		if (i == iTopIndex) {
			int ofs = getOffset(iLastVBarPos);
			if (ofs == 0) {
				return;
			}

			scrollTo(myPos);
			return;
		}

		if (i == iBottomIndex) {
			int ofs = getOffset(iLastVBarPos + clientArea.height);
			if (ofs == 0) {
				return;
			}

			scrollTo((iTopIndex + 1) * ListRow.ROW_HEIGHT);

			return;
		}

		// adjust bar so that new focused row is in the same spot as the old
		// one
		int ofs = 0;
		ListRow rowFocused = getRowFocused();
		if (rowFocused != null) {
			int iFocusedIdx = rowFocused.getIndex();
			if (iFocusedIdx >= iTopIndex && iFocusedIdx <= iBottomIndex) {
				ofs = (rowFocused.getIndex() * ListRow.ROW_HEIGHT) - iLastVBarPos;
				//System.out.println("moveF ofs=" + ofs + "; mp = " + myPos);
				if (ofs < 0) {
					ofs = 0;
				}
			}
		}
		scrollTo(i * ListRow.ROW_HEIGHT - ofs);
	}

	public boolean isFocused() {
		return listCanvas.isFocusControl();
	}

	// @see com.aelitis.azureus.ui.swt.utils.UIUpdatable#getUpdateUIName()
	public String getUpdateUIName() {
		return "ListView";
	}

	// @see com.aelitis.azureus.ui.swt.utils.UIUpdatable#updateUI()
	public void updateUI() {
		if (listCanvas.isDisposed()) {
			return;
		}
		processDataSourceQueue();

		iGraphicRefresh++;
		boolean bDoGraphics = (iGraphicRefresh % graphicsUpdate) == 0;
		refreshVisible(bDoGraphics, false);

		sortTable();
	}

	// XXX This gets called a lot.  Could store location and size on 
	//     resize/scroll of sc
	public Rectangle getBounds() {
		Rectangle clientArea = listCanvas.getClientArea();
		int pos = listCanvas.getVerticalBar().getSelection();
		return new Rectangle(clientArea.x, -pos, clientArea.width,
				clientArea.height);
	}

	public Rectangle getClientArea() {
		return listCanvas.getClientArea();
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
						moveFocus(getClientArea().height / -ListRow.ROW_HEIGHT, false); // TODO: Use real height
						break;

					case SWT.PAGE_DOWN:
						moveFocus(getClientArea().height / ListRow.ROW_HEIGHT, false); // TODO: Use real height
						break;

					case SWT.HOME: {
						ListRow row = (ListRow) rows.get(0);
						if (row != null) {
							setSelectedRows(new ListRow[] { row
							});
						}
						break;
					}

					case SWT.END: {
						int i = rows.size();
						if (i > 0) {
							ListRow row = (ListRow) rows.get(i - 1);

							if (row != null) {
								setSelectedRows(new ListRow[] { row
								});
							}
						}
						break;
					}
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
		return listCanvas;
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
							cell.refresh(true, false, false);
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

					refreshVisible(true, true);
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

	public boolean isRowVisible(ListRow row) {
		if (listCanvas == null || listCanvas.isDisposed()) {
			return false;
		}

		Rectangle clientArea = listCanvas.getClientArea();
		int iTopIndex = iLastVBarPos / ListRow.ROW_HEIGHT;
		int iBottomIndex = (iLastVBarPos + clientArea.height - 1)
				/ ListRow.ROW_HEIGHT;

		int size = iBottomIndex - iTopIndex + 1;
		if (size <= 0)
			return false;

		int i = row.getIndex();
		return (i >= iTopIndex && i <= iBottomIndex);
	}

	public TableRowCore[] getVisibleRows() {
		if (listCanvas == null || listCanvas.isDisposed()) {
			return new TableRowCore[0];
		}

		int y = listCanvas.getVerticalBar().getSelection();
		Rectangle clientArea = listCanvas.getClientArea();
		int iTopIndex = y / ListRow.ROW_HEIGHT;
		int iBottomIndex = (y + clientArea.height - 1) / ListRow.ROW_HEIGHT;

		int size = iBottomIndex - iTopIndex + 1;
		if (size <= 0)
			return new TableRowCore[0];

		TableRowCore[] visiblerows = new TableRowCore[size];
		int pos = 0;

		for (int i = iTopIndex; i <= iBottomIndex; i++) {
			if (i >= 0 && i < rows.size()) {
				TableRowCore row = (TableRowCore) rows.get(i);
				if (row != null) {
					visiblerows[pos++] = row;
				}
			}
		}

		if (pos <= visiblerows.length) {
			// Some were null, shrink array
			TableRowCore[] temp = new TableRowCore[pos];
			System.arraycopy(visiblerows, 0, temp, 0, pos);
			return temp;
		}

		return visiblerows;
	}

	/**
	 * @param row
	 * @param bDoGraphics 
	 */
	public boolean rowRefresh(ListRow row, boolean bDoGraphics,
			boolean bForceRedraw) {
		if (listCanvas == null || listCanvas.isDisposed()) {
			return false;
		}

		Rectangle clientArea = listCanvas.getClientArea();
		int iTopIndex = getTopIndex();

		int i = row.getIndex();
		boolean changed = false;
		if (i >= iTopIndex) {
			int ofs = getOffset(iLastVBarPos);
			int y = (i - iTopIndex) * ListRow.ROW_HEIGHT - ofs;

			Rectangle rect = new Rectangle(clientArea.x, y, clientArea.width,
					ListRow.ROW_HEIGHT);

			if (imageView != null) {
				GC gc = new GC(imageView);
				try {
					gc.setClipping(rect);
					if (!row.isVisible()) {
						System.out.println("asked for row refresh but not visible "
								+ row.getIndex() + ";" + Debug.getCompressedStackTrace());
						return false;
					}

					//System.out.println(row.getTableCellCore("date_added").isShown() + ";" + row.getTableCellCore("date_added").isUpToDate() + ";" + row.getTableCellCore("date_added").isValid());
					changed |= row.refresh(bDoGraphics, true);

					row.doPaint(gc, true);
				} finally {
					gc.dispose();
				}
			}
			if (changed || bForceRedraw) {
				listCanvas.redraw(rect.x, rect.y, rect.width, rect.height, false);
				listCanvas.update();
				//												System.out.println("redrawing row " + i + "/" + row.getIndex() + "; (" + clientArea.x + ","
				//														+ y + ","
				//														+ clientArea.width + "," + ListRow.ROW_HEIGHT + ") via "
				//														+ Debug.getCompressedStackTrace());
				//listCanvas.redraw();
			}
		}
		return changed;
	}

	private int getTopIndex() {
		if (listCanvas == null || listCanvas.isDisposed()) {
			return -1;
		}

		int y = listCanvas.getVerticalBar().getSelection();
		return y / ListRow.ROW_HEIGHT;
	}

	protected int rowGetVisibleYOffset(TableRowCore row) {
		int i = row.getIndex();
		int iTopIndex = iLastVBarPos / ListRow.ROW_HEIGHT;
		int ofs = getOffset(iLastVBarPos);
		return (i - iTopIndex) * ListRow.ROW_HEIGHT - ofs;
	}

	/** Creates the Context Menu.
	 *
	 * @return a new Menu object
	 */
	public Menu createMenu() {
		final Menu menu = new Menu(listCanvas.getShell(), SWT.POP_UP);
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
				//       addThisColumnSubMenu(getColumnNo(iMouseX));
			}
		});

		return menu;
	}

	public void fillMenu(Menu menu) {
	}
}
