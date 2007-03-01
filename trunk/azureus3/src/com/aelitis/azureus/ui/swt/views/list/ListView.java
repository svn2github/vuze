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
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
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
import org.gudy.azureus2.ui.swt.views.IView;
import org.gudy.azureus2.ui.swt.views.table.*;
import org.gudy.azureus2.ui.swt.views.table.impl.TableCellImpl;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnManager;

import com.aelitis.azureus.ui.common.table.*;
import com.aelitis.azureus.ui.common.table.impl.TableViewImpl;
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
public class ListView
	extends TableViewImpl
	implements TableViewSWT, UIUpdatable, Listener,
	TableStructureModificationListener
{
	public static int COLUMN_MARGIN_WIDTH = 3;

	public static int COLUMN_PADDING_WIDTH = COLUMN_MARGIN_WIDTH * 2;

	public static int ROW_MARGIN_HEIGHT = 2;

	private final static LogIDs LOGID = LogIDs.UI3;

	private static final boolean DEBUGADDREMOVE = false;

	private static final boolean DEBUGPAINT = false;

	private static final boolean DEBUG_SORTER = false;

	private static final boolean DEBUG_COLUMNSIZE = false;

	private static final boolean DELAY_SCROLL = false;

	// Shorter name for ConfigManager, easier to read code
	private static final ConfigurationManager configMan = ConfigurationManager.getInstance();

	private static final String CFG_SORTDIRECTION = "config.style.table.defaultSortOrder";

	private Canvas listCanvas;

	private boolean isPaintingCanvas = false;

	private SWTSkinProperties skinProperties;

	private TableColumnCore[] lastVisibleColumns;

	private int lastClientWidth = 0;

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

	private Image imgView = null;

	private GC gcImgView = null;

	private int iLastVBarPos;

	protected Object[] restartRefreshVisible = null;

	private boolean bInRefreshVisible;

	protected boolean viewVisible;

	private List listenersMenuFill = new ArrayList();

	private final int style;

	private Composite listParent;

	private Image imgSortAsc;

	private Image imgSortDesc;

	private boolean bTitleIsMinWidth;

	private TableViewSWTPanelCreator mainPanelCreator;

	private Map mapColumnMetrics = new HashMap();

	private boolean bSkipSelectionTrigger = false;

	private ArrayList rowsToRefresh = new ArrayList();

	private AEMonitor rowsToRefresh_mon = new AEMonitor("rowsToRefresh");

	public ListView(final String sTableID, SWTSkinProperties skinProperties,
			Composite parent, Composite headerArea, int style) {
		this.skinProperties = skinProperties;
		this.sTableID = sTableID;
		this.style = style;
		this.headerArea = headerArea;
		if (headerArea != null) {
			ImageLoader imgLoader = ImageLoaderFactory.getInstance();
			if (imgLoader != null) {
				imgSortAsc = imgLoader.getImage("image.sort.asc");
				imgSortDesc = imgLoader.getImage("image.sort.desc");
			}
		}
		initialize(parent);
		UIUpdaterFactory.getInstance().addUpdater(this);
	}

	public ListView(String sTableID, int style) {
		this.sTableID = sTableID;
		this.style = style;
	}

	public void setHeaderArea(Composite headerArea, Image imgSortAsc,
			Image imgSortDesc) {
		this.headerArea = headerArea;
		this.imgSortAsc = imgSortAsc;
		this.imgSortDesc = imgSortDesc;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableViewSWT#initialize(org.eclipse.swt.widgets.Composite)
	public void initialize(Composite parent) {
		FormData formData;

		COConfigurationManager.addAndFireParameterListener("Graphics Update",
				new ParameterListener() {
					public void parameterChanged(String parameterName) {
						graphicsUpdate = COConfigurationManager.getIntParameter("Graphics Update");
					}
				});

		TableViewSWTPanelCreator mainPanelCreator = getMainPanelCreator();
		if (mainPanelCreator != null) {
			listParent = mainPanelCreator.createTableViewPanel(parent);
		} else {
			listParent = parent;
		}

		listParent.setBackgroundMode(SWT.INHERIT_FORCE);

		listCanvas = new Canvas(listParent, SWT.NO_BACKGROUND
				| SWT.NO_REDRAW_RESIZE | style);
		listCanvas.setLayout(new FormLayout());

		Object layout = listParent.getLayout();
		if (layout instanceof FormLayout) {
			formData = new FormData();
			formData.left = new FormAttachment(0);
			formData.top = new FormAttachment(0);
			formData.right = new FormAttachment(100);
			formData.bottom = new FormAttachment(100);
			listCanvas.setLayoutData(formData);
		} else if (layout instanceof GridLayout) {
			GridData gd = new GridData(GridData.FILL_BOTH);
			listCanvas.setLayoutData(gd);
		}

		vBar = listCanvas.getVerticalBar();
		if (vBar != null) {
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
							log("save");
						}
					} else {
						scrollTo(vBar.getSelection());
					}
				}
			});
		}

		// Track whether the view is visible or not and adjust scrollbar when
		// visibility becomes true (Bug on SWT/Windows where setting scrollbar's
		// visibility doesn't set it in Windows, but SWT still returns that it
		// does)
		Composite c = listCanvas;
		Listener listenerShow = new Listener() {
			public void handleEvent(Event event) {
				if (event.type == SWT.Show) {
					viewVisible = true;
					// asyncExec so SWT finishes up it's show routine
					// Otherwise, the scrollbar visibility setting will fail
					listCanvas.getDisplay().asyncExec(new AERunnable() {
						public void runSupport() {
							refreshVisible(true, true, true);
							refreshScrollbar();
						}
					});
				} else {
					viewVisible = false;
				}
			}
		};
		viewVisible = true;
		while (c != null) {
			viewVisible |= c.isVisible();
			c.addListener(SWT.Show, listenerShow);
			c.addListener(SWT.Hide, listenerShow);
			c = c.getParent();
		}

		listCanvas.addListener(SWT.Resize, new Listener() {
			Rectangle lastBounds = new Rectangle(0, 0, 0, 0);

			public void handleEvent(Event event) {
				boolean bNeedsRefresh = false;
				Rectangle clientArea = listCanvas.getClientArea();

				if (clientArea.width == 0 || clientArea.height == 0) {
					return;
				}

				if (imgView == null) {
					if (DEBUGPAINT) {
						logPAINT("first resize (img null)");
					}
					imgView = new Image(listCanvas.getDisplay(), clientArea);
					bNeedsRefresh = true;
				} else {
					if (!lastBounds.equals(clientArea)) {
						bNeedsRefresh = lastBounds.height != clientArea.height;
						Image newImageView = new Image(listCanvas.getDisplay(), clientArea);
						GC gc = null;
						try {
							gc = new GC(newImageView);
							gc.drawImage(imgView, 0, 0);

							Region reg = new Region();
							reg.add(clientArea);
							reg.subtract(imgView.getBounds());
							gc.setClipping(reg);

							//gc.setBackground(Display.getDefault().getSystemColor((int)(Math.random() * 16)));
							gc.setBackground(listCanvas.getBackground());
							gc.fillRectangle(clientArea);
						} finally {
							if (gc != null) {
								gc.dispose();
							}
						}
						imgView.dispose();
						imgView = newImageView;
						//listCanvas.update();
					}
				}

				if (bNeedsRefresh) {
					if (DEBUGPAINT) {
						logPAINT("paint needs refresh");
					}

					boolean isOurGC = gcImgView == null;
					try {
						if (isOurGC) {
							gcImgView = new GC(imgView);
						}
						gcImgView.setForeground(listCanvas.getForeground());
						gcImgView.setBackground(listCanvas.getBackground());

						TableRowSWT[] visibleRows = getVisibleRows();
						if (visibleRows.length > 0) {
							//gc.setClipping(e.gc.getClipping());
							int ofs = getOffset(iLastVBarPos);

							int y0 = lastBounds.y + lastBounds.height;
							int y1 = clientArea.y + clientArea.height;

							int start = y0 / ListRow.ROW_HEIGHT;
							int end = y1 / ListRow.ROW_HEIGHT + 1;
							if (end > visibleRows.length) {
								end = visibleRows.length;
							}

							if (DEBUGPAINT) {
								logPAINT(visibleRows.length + " visible;" + "; " + start
										+ " - " + (end - 1));
							}
							long lStart = System.currentTimeMillis();
							for (int i = start; i < end; i++) {
								TableRowSWT row = visibleRows[i];

								row.doPaint(gcImgView, true);
							}

							// Blank out area below visible rows
							int endY = visibleRows.length * ListRow.ROW_HEIGHT + ofs;
							if (endY < clientArea.height) {
								if (DEBUGPAINT) {
									logPAINT("fill " + (clientArea.height - endY) + "@" + endY);
								}
								gcImgView.setBackground(listCanvas.getBackground());
								gcImgView.fillRectangle(0, endY, clientArea.width,
										clientArea.height - endY);
							}

							long diff = System.currentTimeMillis() - lStart;
							if (diff > 50) {
								log(diff + "ms to paint" + start + " - " + (end - 1));
							}
						} else {
							if (DEBUGPAINT) {
								logPAINT("fillall");
							}
							gcImgView.fillRectangle(clientArea);
						}
					} catch (Exception ex) {
						if (!(ex instanceof IllegalArgumentException)) {
							// IllegalArgumentException happens when we are already drawing 
							// to the image.  This is "normal" as we may be in a paint event,
							// and something forces a repaint
							Debug.out(ex);
						}
					} finally {
						if (isOurGC && gcImgView != null) {
							gcImgView.dispose();
							gcImgView = null;
						}
					}
				}
				lastBounds = clientArea;

				// SWT does resize, then paint 

				// Refreshing the scrollbar will trigger a bigger paint
				// Otherwise, we may have to trigger one ourselves
				if (vBar == null || !refreshScrollbar()) {
					getVisibleColumns();
				}
			}
		});

		listCanvas.addListener(SWT.Paint, new canvasPaintListener());

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

		TableStructureEventDispatcher.getInstance(sTableID).addListener(this);

		triggerLifeCycleListener(TableLifeCycleListener.EVENT_INITIALIZED);

		if (headerArea != null) {
			setupHeader(headerArea);
		}
	}

	private int getOffset(int i) {
		int ofs = (i % ListRow.ROW_HEIGHT);
		if (ofs == 0)
			return 0;
		return ofs;
	}

	private int getBottomRowHeight() {
		int ofs = (iLastVBarPos + listCanvas.getClientArea().height)
				% ListRow.ROW_HEIGHT;

		return ofs;
	}

	/**
	 * 
	 */
	protected boolean refreshScrollbar() {
		if (!viewVisible || vBar == null || vBar.isDisposed()) {
			return false;
		}
		boolean changed = false;

		Rectangle client = listCanvas.getClientArea();
		int h = (rows.size() * ListRow.ROW_HEIGHT) - client.height;

		if (h <= 0 || client.height == 0) {
			if (vBar.isVisible()) {
				vBar.setVisible(false);
				listCanvas.redraw();
				if (headerArea != null) {
					headerArea.redraw();
				}
			}
			iLastVBarPos = 0;
		} else {
			if (!vBar.isVisible()) {
				vBar.setVisible(true);
				listCanvas.redraw();
				if (headerArea != null) {
					headerArea.redraw();
				}
				changed = true;
			}
			vBar.setIncrement(ListRow.ROW_HEIGHT);
			int thumb = client.height;
			vBar.setMaximum(h + thumb);
			vBar.setThumb(thumb);
			vBar.setPageIncrement(client.height / 2);
			if (iLastVBarPos != vBar.getSelection()) {
				scrollTo(vBar.getSelection());
				changed = true;
			}
		}
		if (DEBUGPAINT) {
			logPAINT("refreshScrollbar. changed? " + changed);
		}
		if (changed) {
			listCanvas.update();
		}
		return changed;
	}

	private void scrollTo(int pos) {
		if (vBar == null || !vBar.getVisible()) {
			return;
		}

		if (pos == iLastVBarPos && pos == vBar.getSelection()) {
			return;
		}

		long lTimeStart = System.currentTimeMillis();

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
		int iThisVBarPos = vBar.getSelection();
		int diff = iLastVBarPos - iThisVBarPos;
		if (DEBUGPAINT) {
			logPAINT("scroll diff = " + diff + ";" + imgView);
		}

		if (diff != 0 && imgView != null && !imgView.isDisposed()) {
			// Shift image up or down, then fill in the gap with a newly displayed row
			boolean isOurGC = gcImgView == null;

			try {
				if (isOurGC) {
					gcImgView = new GC(imgView);
				}
				scrollToWithGC(gcImgView, diff, iThisVBarPos, false, true);
			} catch (Exception ex) {
				Debug.out(ex);
			} finally {
				if (isOurGC && gcImgView != null) {
					gcImgView.dispose();
					gcImgView = null;
				}
			}

			listCanvas.redraw();
			listCanvas.update();
		}
		iLastVBarPos = iThisVBarPos;

		if (DEBUGPAINT) {
			logPAINT("done in " + (System.currentTimeMillis() - lTimeStart));
		}
	}

	private void scrollToWithGC(GC gc, int diff, int iThisVBarPos,
			boolean bMoveOnly, boolean bGCisImage) {
		Rectangle bounds = imgView.getBounds();

		if (diff > 0) {
			int h = bounds.height - diff;
			if (h > 0) {
  			if (Constants.isOSX) {
  				// copyArea should work on OSX, but why risk it when drawImage works
  				gc.drawImage(imgView, 0, 0, bounds.width, h, 0, diff, bounds.width, h);
  			} else {
  				// Windows can't use drawImage on same image
  				gc.copyArea(0, 0, bounds.width, h, 0, diff);
  			}
			}
		} else {
			int h = bounds.height + diff;
			if (h > 0) {
  			if (Constants.isOSX) {
  				// OSX can't copyArea upwards
  				gc.drawImage(imgView, 0, -diff, bounds.width, h, 0, 0, bounds.width, h);
  			} else {
  				// Windows can't use drawImage on same image
  				gc.copyArea(0, -diff, bounds.width, h, 0, 0);
  			}
			}
		}

		if (bMoveOnly) {
			return;
		}

		iLastVBarPos = iThisVBarPos;
		TableRowSWT[] visibleRows = getVisibleRows();
		if (diff < 0) {
			int ofs = getBottomRowHeight();
			// image moved up.. gap at bottom
			int i = visibleRows.length - 1;
			while (diff <= 0 && i >= 0) {
				TableRowSWT row = visibleRows[i];
				if (DEBUGPAINT) {
					logPAINT("scrollTo repaint visRow#" + i + "(idx:" + row.getIndex()
							+ ") d=" + diff + ";ofs=" + ofs);
				}
				row.doPaint(gc, true);
				i--;
				diff += ofs;
				ofs = ListRow.ROW_HEIGHT;
			}
			if (i >= 0) {
				ListRow row = (ListRow) visibleRows[i];
				if (!row.isValid()) {
					if (DEBUGPAINT) {
						logPAINT("scrollTo repaint dirty Row#" + i + "(idx:"
								+ row.getIndex() + ") d=" + diff + ";ofs=" + ofs);
					}
					row.doPaint(gc, true);
				}
			}
		} else {
			// image moved down.. gap at top to draw
			int i = 0;
			int ofs = ListRow.ROW_HEIGHT - getOffset(iLastVBarPos);
			while (diff >= 0 && i < visibleRows.length) {
				TableRowSWT row = visibleRows[i];
				if (DEBUGPAINT) {
					logPAINT("repaint " + i + "(" + row.getIndex() + ") d=" + diff
							+ ";o=" + ofs);
				}
				row.doPaint(gc, true);
				i++;
				diff -= ofs;
				ofs = ListRow.ROW_HEIGHT;
			}
		}
	}

	/**
	 * @param headerArea
	 */
	private void setupHeader(final Composite headerArea) {
		this.headerArea = headerArea;

		final Cursor cursor = new Cursor(headerArea.getDisplay(), SWT.CURSOR_HAND);
		headerArea.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				Utils.disposeSWTObjects(new Object[] {
					cursor
				});
			}
		});

		if (bTitleIsMinWidth) {
			setColumnMinWidthToHeaders();
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
					if (DEBUG_SORTER) {
						log("sorting on " + columns[inColumn].getName());
					}
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
				TableColumnCore[] columns = lastVisibleColumns;

				if (skinProperties != null) {
					e.gc.setForeground(skinProperties.getColor("color.list.header.fg"));
				}

				Rectangle clientArea = headerArea.getClientArea();
				int pos = clientArea.x + ListView.COLUMN_MARGIN_WIDTH;
				int lastExtraSpace = -1;
				for (int i = 0; i < columns.length; i++) {
					int width = columns[i].getWidth();
					String key = columns[i].getTitleLanguageKey();
					String text = MessageText.getString(key, "");
					int align = CoreTableColumn.getSWTAlign(columns[i].getAlignment());

					int drawWidth = width;

					Point size = e.gc.textExtent(text);
					Rectangle bounds;
					if (size.x > drawWidth && lastExtraSpace > 0) {
						int giveSpace = Math.min(lastExtraSpace, size.x - drawWidth);
						bounds = new Rectangle(pos - giveSpace, clientArea.y, drawWidth
								+ giveSpace, clientArea.height);
					} else {
						bounds = new Rectangle(pos, clientArea.y, drawWidth,
								clientArea.height);
					}

					headerArea.setData("Column" + i + "Bounds", bounds);

					if (text.length() > 0) {
						GCStringPrinter.printString(e.gc, text, bounds, false, false, align);
					}

					if (align == SWT.LEFT) {
						lastExtraSpace = bounds.width - size.x;
					} else if (align == SWT.CENTER) {
						lastExtraSpace = (bounds.width - size.x) / 2;
					} else {
						lastExtraSpace = 0;
					}

					if (columns[i].equals(sortColumn)) {
						Image img = sortColumn.isSortAscending() ? imgSortAsc : imgSortDesc;
						if (img != null) {
							Rectangle imgBounds = img.getBounds();
							e.gc.drawImage(img, bounds.x + (bounds.width / 2)
									- (imgBounds.width / 2), 0);
						}
					}

					//e.gc.drawLine(pos, bounds.y, pos, bounds.y + bounds.height);
					pos += width + (ListView.COLUMN_MARGIN_WIDTH * 2);
				}
			}
		});
	}

	/**
	 * 
	 *
	 * @since 3.0.0.7
	 */
	private void setColumnMinWidthToHeaders() {
		int sortWidth = Math.max(imgSortAsc == null ? -2
				: imgSortAsc.getBounds().width, imgSortDesc == null ? -2
				: imgSortDesc.getBounds().width) + 2;

		// set min column width to width of header
		GC gc = new GC(headerArea);
		try {
			TableColumnCore[] columns = getAllColumns();
			if (columns == null) {
				return;
			}
			for (int i = 0; i < columns.length; i++) {
				TableColumnCore column = columns[i];
				String title = MessageText.getString(column.getTitleLanguageKey(), "");
				int oldWidth = column.getMinWidth();
				int minWidth = gc.textExtent(title).x + sortWidth;
				if (minWidth > oldWidth) {
					column.setMinWidth(minWidth);
				}
			}
		} finally {
			if (gc != null) {
				gc.dispose();
			}
		}
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
			setSelectedRows(new ListRow[] {
				newRow
			});
		} finally {
			selectedRows_mon.exit();
		}
	}

	public void refreshVisible(final boolean doGraphics,
			final boolean bForceRedraw, final boolean bAsync) {
		if (isDisposed() || !listCanvas.isVisible()) {
			return;
		}
		if (bInRefreshVisible) {
			if (DEBUGPAINT) {
				logPAINT("Set flag to restart visible because of "
						+ Debug.getCompressedStackTrace());
			}
			restartRefreshVisible = new Object[] {
				new Boolean(doGraphics),
				new Boolean(bForceRedraw),
				new Boolean(bAsync)
			};
			return;
		}
		bInRefreshVisible = true;
		if (DEBUGPAINT) {
			logPAINT("Start refreshVisible " + Debug.getCompressedStackTrace());
		}

		final Display display = listCanvas.getDisplay();

		AERunnable runnable = new AERunnable() {
			public void runSupport() {
				final TableRowCore[] visibleRows = getVisibleRows();
				try {
					for (int i = 0; i < visibleRows.length; i++) {
						if (restartRefreshVisible != null) {
							if (DEBUGPAINT) {
								logPAINT("STOPPED refresh at " + i);
							}
							return;
						}

						final ListRow row = (ListRow) visibleRows[i];

						AERunnable rowRunnable = new AERunnable() {
							public void runSupport() {

								if (restartRefreshVisible != null) {
									if (DEBUGPAINT) {
										logPAINT("stopped refresh at " + row.getIndex());
									}
									return;
								}
								
								if (!row.isVisible()) {
									// uh oh, order changed, refresh!
									restartRefreshVisible = new Object[] {
										new Boolean(doGraphics),
										new Boolean(bForceRedraw),
										new Boolean(bAsync)
									};
									return;
								}

								if (bForceRedraw) {
									row.invalidate();
								}

								if (row.isVisible()) {
									//if (row.isVisible() && !row.isValid()) {
									rowRefreshAsync(row, doGraphics, bForceRedraw);
								} else {
									//System.out.println("skipping.. not visible. valid? " + row.isValid());
								}
							}
						};

						if (bAsync) {
							display.asyncExec(rowRunnable);
						} else {
							display.syncExec(rowRunnable);
						}
					}
				} finally {
					bInRefreshVisible = false;

					if (restartRefreshVisible != null) {
						Object[] params = restartRefreshVisible;
						restartRefreshVisible = null;
						if (DEBUGPAINT) {
							logPAINT("Restarting refresh");
						}
						refreshVisible(((Boolean) params[0]).booleanValue(),
								((Boolean) params[1]).booleanValue(),
								((Boolean) params[2]).booleanValue());
					}
				}
			}
		};

		if (bAsync) {
			display.asyncExec(runnable);
		} else {
			display.syncExec(runnable);
		}
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
								logADDREMOVE("Saved time by not adding a row that was removed");
							}
						}
					}
				}
			}

			if (dataSourcesToRemove != null && dataSourcesToRemove.size() > 0) {
				dataSourcesRemove = dataSourcesToRemove.toArray();
				if (DEBUGADDREMOVE && dataSourcesRemove.length > 1) {
					logADDREMOVE("Streamlining removing " + dataSourcesRemove.length
							+ " rows");
				}
				dataSourcesToRemove.clear();
			}
		} finally {
			row_mon.exit();
		}

		if (dataSourcesAdd != null) {
			addDataSources(dataSourcesAdd, true);
			if (DEBUGADDREMOVE && dataSourcesAdd.length > 1) {
				logADDREMOVE("Streamlined adding " + dataSourcesAdd.length + " rows");
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

	// @see com.aelitis.azureus.ui.common.table.TableView#addDataSource(java.lang.Object)
	public void addDataSource(Object dataSource) {
		addDataSources(new Object[] {
			dataSource
		}, false);
	}

	public void addDataSource(final Object datasource, boolean bImmediate) {
		addDataSources(new Object[] {
			datasource
		}, bImmediate);
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#addDataSources(java.lang.Object[])
	public void addDataSources(Object[] dataSources) {
		addDataSources(dataSources, false);
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
					logADDREMOVE(sTableID + ": Queueing " + count + " dataSources to add");
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
						logADDREMOVE("addDS pos " + index);

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
					refreshVisible(true, true, true);
				}
				//System.out.println(Debug.getCompressedStackTrace());
			}
		});
		long diff = System.currentTimeMillis() - lTimeStart;
		if (diff > 20) {
			logADDREMOVE("addDS(" + dataSources.length + "): " + diff + "ms");
		}
	}

	/**
	 * @param string
	 */
	protected void logADDREMOVE(String string) {
		if (DEBUGADDREMOVE) {
			System.out.println(System.currentTimeMillis() + ":" + sTableID + "] "
					+ string);
		}
	}

	protected void log(String string) {
		System.out.println(System.currentTimeMillis() + ":" + sTableID + "] "
				+ string);
	}

	protected void logPAINT(String string) {
		if (!DEBUGPAINT) {
			return;
		}

		System.out.println(System.currentTimeMillis() + ":" + sTableID + "] "
				+ string);
	}

	protected void logCOLUMNSIZE(String string) {
		if (!DEBUG_COLUMNSIZE) {
			return;
		}

		System.out.println(System.currentTimeMillis() + ":" + sTableID + "] "
				+ string);
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#removeDataSource(java.lang.Object)
	public void removeDataSource(Object dataSource) {
		removeDataSources(new Object[] {
			dataSource
		});
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#removeDataSource(java.lang.Object, boolean)
	public void removeDataSource(final Object datasource, boolean bImmediate) {
		removeDataSources(new Object[] {
			datasource
		});
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#removeDataSources(java.lang.Object[])
	public void removeDataSources(final Object[] dataSources) {
		Utils.execSWTThread(new Runnable() {
			public void run() {
				try {
					row_mon.enter();
					int firstIndex = dataSources.length;
					ListRow newFocusRow = null;

					for (int i = 0; i < dataSources.length; i++) {
						Object datasource = dataSources[i];
						ListRow row = (ListRow) mapDataSourceToRow.get(datasource);
						if (row != null) {

							int index = row.getIndex();
							if (index < firstIndex) {
								firstIndex = index;
							}

							if (newFocusRow == null && row.isFocused()) {
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
							row.delete();
							rows.remove(row);

							logADDREMOVE("remDS pos " + index + ";" + rows.size());

							triggerListenerRowRemoved(row);
						} else {
							//System.out.println("not found " + datasource);
						}
					}

					if (newFocusRow != null) {
						//System.out.println("SR " + newFocusRow.getIndex());
						rowSetFocused(newFocusRow);
						newFocusRow.setSelected(true);
					}

					for (int i = firstIndex; i < rows.size(); i++) {
						ListRow fixRow = (ListRow) rows.get(i);
						fixRow.fixupPosition();
					}
				} finally {
					row_mon.exit();
					refreshScrollbar();
					// TODO: Redraw only if visible or above visible (bg change)

					if (imgView != null && !imgView.isDisposed()
							&& !listCanvas.isDisposed()) {
						TableRowCore[] visibleRows = getVisibleRows();
						Rectangle clientArea = listCanvas.getClientArea();
						if (visibleRows.length > 0) {
							int ofs = getOffset(iLastVBarPos);
							int endY = visibleRows.length * ListRow.ROW_HEIGHT + ofs;

							if (endY < clientArea.height) {
								boolean isOurGC = gcImgView == null;
								try {
									if (isOurGC) {
										gcImgView = new GC(imgView);
									}
									gcImgView.setBackground(listCanvas.getBackground());

									gcImgView.fillRectangle(0, endY, clientArea.width,
											clientArea.height - endY);
									listCanvas.redraw(0, endY, clientArea.width,
											clientArea.height - endY, false);
								} catch (Exception ex) {
									if (!(ex instanceof IllegalArgumentException)) {
										// IllegalArgumentException happens when we are already drawing 
										// to the image.  This is "normal" as we may be in a paint event,
										// and something forces a repaint
										Debug.out(ex);
									}
								} finally {
									if (isOurGC && gcImgView != null) {
										gcImgView.dispose();
										gcImgView = null;
									}
								}
							}
						} else {
							boolean isOurGC = gcImgView == null;
							try {
								if (isOurGC) {
									gcImgView = new GC(imgView);
								}
								gcImgView.setBackground(listCanvas.getBackground());
								gcImgView.fillRectangle(clientArea);
								listCanvas.redraw();
							} catch (Exception ex) {
								if (!(ex instanceof IllegalArgumentException)) {
									// IllegalArgumentException happens when we are already drawing 
									// to the image.  This is "normal" as we may be in a paint event,
									// and something forces a repaint
									Debug.out(ex);
								}
							} finally {
								if (isOurGC && gcImgView != null) {
									gcImgView.dispose();
									gcImgView = null;
								}
							}
						}

						listCanvas.redraw();
					}

					refreshVisible(true, true, true);
				}
			}
		});
	}

	public void removeAllDataSources(boolean bImmediate) {
		Utils.execSWTThread(new Runnable() {
			public void run() {

				row_mon.enter();
				logADDREMOVE("removeAll");

				try {
					for (Iterator iterator = mapDataSourceToRow.keySet().iterator(); iterator.hasNext();) {
						Object datasource = iterator.next();
						ListRow row = (ListRow) mapDataSourceToRow.get(datasource);

						if (row != null) {
							rows.remove(row);
							row.setSelected(false);
							row.setFocused(false);
							row.delete();
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

	private class selectionListener
		implements Listener
	{
		// XXX Copied from TableView!
		private TableCellMouseEvent createMouseEvent(TableCellSWT cell, Event e,
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

		public void handleEvent(Event e) {
			ListRow row = (ListRow) getRow(e.x, e.y);
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
					boolean MOD1 = (e.stateMask & SWT.MOD1) > 0; // ctrl(Win)
					boolean MOD2 = (e.stateMask & SWT.MOD2) > 0; // shift(Win)
					boolean MOD4 = (e.stateMask & SWT.MOD4) > 0; // ctrl(OSX)

					if (MOD1 && MOD2) {
						TableCellSWT cell = ((ListRow) row).getTableCellSWT(e.x, e.y);
						if (cell instanceof TableCellImpl) {
							((TableCellImpl) cell).bDebug = !((TableCellImpl) cell).bDebug;
							System.out.println("DEBUG ROW " + cell.getTableColumn().getName()
									+ ":" + row.getIndex() + " "
									+ (((TableCellImpl) cell).bDebug ? "ON" : "OFF"));
						}
					} else if (MOD1) { // control
						boolean select = !row.isSelected();
						row.setSelected(select);
						if (select) {
							row.setFocused(true);
						}
					} else if (MOD2) { // shift
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
					} else if (!MOD4) {
						setSelectedRows(new ListRow[] {
							row
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
				TableCellSWT cell = row.getTableCellSWT(e.x, e.y);
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

	/**
	 * @param x
	 * @param y
	 * @return
	 */
	// @see com.aelitis.azureus.ui.common.table.TableView#getRow(int, int)
	public TableRowCore getRow(int x, int y) {
		int pos = (y + iLastVBarPos) / ListRow.ROW_HEIGHT;
		if (pos < rows.size() && pos >= 0) {
			ListRow row = (ListRow) rows.get(pos);
			//System.out.println("getRow; y=" + y + ";sb=" + iLastVBarPos + ";pos="
			//		+ pos + ";" + row);
			return row;
		}

		return null;
	}

	public int indexOf(TableRowCore row) {
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
	public TableRowCore getRow(Object dataSource) {
		return (ListRow) mapDataSourceToRow.get(dataSource);
	}

	public TableRowSWT getRowSWT(Object dataSource) {
		return (TableRowSWT) mapDataSourceToRow.get(dataSource);
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

		TableRowCore[] selectedRows = getSelectedRows();
		for (int i = 0; i < selectedRows.length; i++) {
			TableRowCore row = selectedRows[i];
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

	boolean adjustingColumns = false;

	int lastVisColumnWidth = 0;

	public TableColumnCore[] getVisibleColumns() {
		if (lastVisibleColumns == null) {
			return new TableColumnCore[0];
		}

		adjustingColumns = true;

		try {
			final int iClientWidth = listCanvas.getClientArea().width;

			if (iClientWidth <= 0) {
				return new TableColumnCore[0];
			}

			if (lastClientWidth == iClientWidth) {
				return lastVisibleColumns;
			}

			lastClientWidth = iClientWidth;

			if (DEBUG_COLUMNSIZE) {
				logCOLUMNSIZE(listCanvas.getSize() + "," + listCanvas.getBounds());
			}

			TableColumnManager tcManager = TableColumnManager.getInstance();
			List autoHideOrder = tcManager.getAutoHideOrder(sTableID);

			// calculate totals
			int totalWidthVis = 0;
			int totalMinWidth = 0;
			int totalMinWidthVis = 0;
			int totalPrefWidthVis = 0;
			for (int i = 0; i < allColumns.length; i++) {
				TableColumnCore column = allColumns[i];
				if (column.getPosition() < 0) {
					continue;
				}

				int minWidth = column.getMinWidth();
				if (DEBUG_COLUMNSIZE) {
					logCOLUMNSIZE("  " + column.getName() + ",w" + column.getWidth()
							+ ",mi" + minWidth + ",ma=" + column.getMaxWidth() + ",p="
							+ column.getPreferredWidth());
				}

				totalMinWidth += minWidth + COLUMN_PADDING_WIDTH;
				if (column.isVisible()) {
					totalMinWidthVis += minWidth + COLUMN_PADDING_WIDTH;
					totalWidthVis += column.getWidth() + COLUMN_PADDING_WIDTH;
					totalPrefWidthVis += column.getPreferredWidth()
							+ COLUMN_PADDING_WIDTH;
				}
			}

			if (DEBUG_COLUMNSIZE) {
				logCOLUMNSIZE("tot=" + totalWidthVis + ";minTot=" + totalMinWidth + "/"
						+ totalMinWidthVis + ";avail=" + iClientWidth);
			}

			ArrayList visibleColumnsList = new ArrayList(allColumns.length);
			for (int i = 0; i < allColumns.length; i++) {
				if (allColumns[i].getPosition() >= 0) {
					visibleColumnsList.add(allColumns[i]);
				}
			}

			if (totalMinWidthVis > iClientWidth) {
				// we gotta do column removals
				int pos = 0;
				while (totalMinWidthVis > iClientWidth && pos < autoHideOrder.size()) {
					TableColumn columnToHide = (TableColumn) autoHideOrder.get(pos);
					if (columnToHide.isVisible()) {
						totalMinWidth -= columnToHide.getMinWidth() + COLUMN_PADDING_WIDTH;
						totalWidthVis -= columnToHide.getWidth() + COLUMN_PADDING_WIDTH;
						totalMinWidthVis -= columnToHide.getMinWidth()
								+ COLUMN_PADDING_WIDTH;
						columnToHide.setVisible(false);
						visibleColumnsList.remove(columnToHide);
						if (DEBUG_COLUMNSIZE) {
							logCOLUMNSIZE("--- remove column " + columnToHide.getName()
									+ ". minTot=" + totalMinWidth + "/" + totalMinWidthVis);
						}
					}
					pos++;
				}
			} else if (totalMinWidth != totalMinWidthVis) {
				// add a column
				TableColumn columnToShow;
				for (int i = autoHideOrder.size() - 1; i >= 0; i--) {
					TableColumnCore column = (TableColumnCore) autoHideOrder.get(i);
					if (!column.isVisible()) {
						columnToShow = column;

						int iMinWidth = columnToShow.getMinWidth();
						if (totalMinWidthVis + iMinWidth + COLUMN_PADDING_WIDTH < iClientWidth) {
							columnToShow.setWidth(iMinWidth);
							columnToShow.setVisible(true);
							// reget width in case minwidth didn't apply 
							int width = columnToShow.getWidth();
							totalWidthVis += width + COLUMN_PADDING_WIDTH;
							totalMinWidthVis += width + COLUMN_PADDING_WIDTH;
							if (DEBUG_COLUMNSIZE) {
								logCOLUMNSIZE("+++ add column " + column.getName() + ";w="
										+ width + ";mw=" + iMinWidth + "; left: "
										+ (totalMinWidthVis + iMinWidth - iClientWidth));
							}
						} else {
							break;
						}
					}
				}
			}

			// clean up list.  remove anything with 0 width
			for (Iterator iter = visibleColumnsList.iterator(); iter.hasNext();) {
				TableColumnCore column = (TableColumnCore) iter.next();

				if (!column.isVisible()) {
					iter.remove();
				}
			}

			if (totalWidthVis > iClientWidth) {
				// we gotta do some shrinking
				int iNeededSpace = totalWidthVis - iClientWidth;
				if (DEBUG_COLUMNSIZE) {
					logCOLUMNSIZE("1] Shrink by " + iNeededSpace + " (tot="
							+ totalWidthVis + ";avail=" + iClientWidth + ")");
				}

				// Pass 1: Shrink to preferred width
				for (int i = 0; i < visibleColumnsList.size(); i++) {
					if (iNeededSpace <= 0) {
						break;
					}

					TableColumnCore column = (TableColumnCore) visibleColumnsList.get(i);

					int width = column.getWidth();
					int prefWidth = column.getPreferredWidth();
					if (prefWidth <= 0 || width < prefWidth) {
						continue;
					}
					int minWidth = column.getMinWidth();
					if (prefWidth < minWidth) {
						prefWidth = minWidth;
					}
					int diff = width - prefWidth;
					if (diff > iNeededSpace) {
						column.setWidth(width - iNeededSpace);
						iNeededSpace = 0;
					} else {
						column.setWidth(prefWidth);
						iNeededSpace -= diff;
					}
				}

				if (DEBUG_COLUMNSIZE) {
					logCOLUMNSIZE("2] Shrink by " + iNeededSpace + " (tot="
							+ totalWidthVis + ";avail=" + iClientWidth + ")");
				}
				// Pass 2: Shrink to min width
				for (int i = visibleColumnsList.size() - 1; i >= 0; i--) {
					if (iNeededSpace <= 0) {
						break;
					}

					TableColumnCore column = (TableColumnCore) visibleColumnsList.get(i);

					int width = column.getWidth();
					int minWidth = column.getMinWidth();
					int diff = width - minWidth;
					if (diff > iNeededSpace) {
						column.setWidth(width - iNeededSpace);
						iNeededSpace = 0;
					} else {
						column.setWidth(minWidth);
						iNeededSpace -= diff;
					}
				}
				if (DEBUG_COLUMNSIZE) {
					logCOLUMNSIZE("3] Remaining Needed Space" + iNeededSpace + " (tot="
							+ totalWidthVis + ";avail=" + iClientWidth + ")");
				}
			} else if (totalWidthVis < iClientWidth) {

				// Expand expandable columns
				int iExtraSpace = iClientWidth - totalWidthVis;

				ArrayList expandableColumns = new ArrayList();
				for (int i = 0; i < visibleColumnsList.size(); i++) {
					TableColumnCore column = (TableColumnCore) visibleColumnsList.get(i);

					int width = column.getWidth();
					int maxWidth = column.getMaxWidth();

					//					if (width == 0) {
					//						int minWidth = column.getMinWidth();
					//						if (minWidth == -1) {
					//							minWidth = 50;
					//						}
					//						width = minWidth;
					//						column.setWidth(width);
					//					}
					//
					if (width != maxWidth) {
						expandableColumns.add(column);
					}
				}

				// pass 1.. set to preferred width if smaller
				boolean bMoreSpace;
				do {
					int numExpandableColumns = expandableColumns.size();
					if (DEBUG_COLUMNSIZE) {
						logCOLUMNSIZE("1] Extra Space=" + iExtraSpace + ";# Expandable: "
								+ numExpandableColumns);
					}
					bMoreSpace = false;

					for (Iterator iter = expandableColumns.iterator(); iter.hasNext();) {
						TableColumnCore column = (TableColumnCore) iter.next();
						int width = column.getWidth();
						int prefWidth = column.getPreferredWidth();
						if (width >= prefWidth) {
							continue;
						}

						int expandBy = (int) ((double) iExtraSpace / numExpandableColumns);
						if (expandBy == 0) {
							expandBy = 1;
						}
						int newWidth = width + expandBy;
						if (newWidth > prefWidth) {
							expandBy -= newWidth - prefWidth;
							newWidth = prefWidth;
						} else {
							bMoreSpace = true;
						}
						column.setWidth(newWidth);
						numExpandableColumns--;
						iExtraSpace -= expandBy;
						if (iExtraSpace <= 0) {
							break;
						}
					}
				} while (bMoreSpace && iExtraSpace > 0);

				// pass 2: expand columns
				if (iExtraSpace > 0) {
					int numExpandableColumns = expandableColumns.size();
					if (DEBUG_COLUMNSIZE) {
						logCOLUMNSIZE("2] Extra Space=" + iExtraSpace + ";# Expandable: "
								+ numExpandableColumns);
					}

					for (Iterator iter = expandableColumns.iterator(); iter.hasNext();) {
						TableColumnCore column = (TableColumnCore) iter.next();
						int width = column.getWidth();
						int maxWidth = column.getMaxWidth();

						int expandBy = (int) ((double) iExtraSpace / numExpandableColumns);
						int newWidth = width + expandBy;
						if (maxWidth != -1 && newWidth > maxWidth) {
							newWidth = maxWidth;
							expandBy = maxWidth - width;
						}
						column.setWidth(newWidth);
						if (DEBUG_COLUMNSIZE) {
							logCOLUMNSIZE(column.getName() + "]" + numExpandableColumns
									+ ": expandBy:" + expandBy + ";newWidth=" + column.getWidth()
									+ ";wantedW=" + newWidth + ";mxw=" + column.getMaxWidth());
						}
						expandBy = column.getWidth() - width;
						numExpandableColumns--;
						iExtraSpace -= expandBy;
					}
					if (DEBUG_COLUMNSIZE) {
						logCOLUMNSIZE("3] Extra Space=" + iExtraSpace);
					}
				}

			} else {
				if (DEBUG_COLUMNSIZE) {
					logCOLUMNSIZE("perfect fit");
				}
			}

			// Do a pass to try to match preferred widths
			int iPrefWidthsOver = 0;
			int iPrefWidthsUnder = 0;
			int iPrefWidthsOverCount = 0;
			int iPrefWidthsUnderCount = 0;
			int iPrefWidthDiff = 0;
			for (int i = 0; i < visibleColumnsList.size(); i++) {
				TableColumnCore column = (TableColumnCore) visibleColumnsList.get(i);
				int iPrefWidth = column.getPreferredWidth();
				if (iPrefWidth <= 0) {
					continue;
				}
				int diff = column.getWidth() - iPrefWidth;
				if (diff > 0) {
					iPrefWidthsOverCount++;
					iPrefWidthsOver += diff;
				} else {
					iPrefWidthsUnderCount++;
					iPrefWidthsUnder -= diff;
				}
				iPrefWidthDiff += diff;
			}

			if (DEBUG_COLUMNSIZE) {
				logCOLUMNSIZE("PrefWO=" + iPrefWidthsOver + "(" + iPrefWidthsOverCount
						+ "),PrefWU=" + iPrefWidthsUnder + "(" + iPrefWidthsUnderCount
						+ "),d=" + iPrefWidthDiff);
			}
			if (iPrefWidthsOver > 0 && iPrefWidthsUnder > 0) {
				if (iPrefWidthDiff >= 0) {
					// we have iPrefWidthsUnder to shift to the under. All unders
					// will end up at pref
					int remaining = iPrefWidthsUnder;
					int adj = (int) (remaining / iPrefWidthsOverCount) + 1;
					for (int i = 0; i < visibleColumnsList.size(); i++) {
						TableColumnCore column = (TableColumnCore) visibleColumnsList.get(i);
						int iPrefWidth = column.getPreferredWidth();
						if (iPrefWidth <= 0) {
							continue;
						}
						int iWidth = column.getWidth();
						int diff = iWidth - iPrefWidth;
						if (diff < 0) {
							diff *= -1;
							// we can always set it to pref, because we have more over than
							// under
							column.setWidth(iPrefWidth);
							remaining -= diff;
						} else if (diff > 0) {
							if (diff > remaining) {
								column.setWidth(iWidth + diff - remaining);
							} else {
								column.setWidth(iPrefWidth - adj);
							}
						}
					}
				} else {
					// we have iPrefWidthOver to shift to under.  Some unders will
					// remain under. All overs will end up at pref
					int remaining = iPrefWidthsOver;
					double pctPerColumn = ((double) iPrefWidthsOver / iPrefWidthsUnder);

					if (DEBUG_COLUMNSIZE) {
						logCOLUMNSIZE("pct per column: " + pctPerColumn);
					}

					for (int i = 0; i < visibleColumnsList.size(); i++) {
						TableColumnCore column = (TableColumnCore) visibleColumnsList.get(i);

						int iPrefWidth = column.getPreferredWidth();
						if (iPrefWidth <= 0) {
							continue;
						}
						int iWidth = column.getWidth();
						int diff = iWidth - iPrefWidth;
						if (diff < 0 && remaining > 0) {
							if (iPrefWidthsUnderCount == 1) {
								diff = remaining;
							} else {
								diff = (int) ((diff * -1) * pctPerColumn);
								if (diff > remaining) {
									if (DEBUG_COLUMNSIZE) {
										logCOLUMNSIZE(column.getName() + " wants " + diff
												+ ", gets " + remaining);
									}
									diff = remaining;
								}
							}
							column.setWidth(iWidth + diff);

							remaining -= (column.getWidth() - iWidth);
							if (DEBUG_COLUMNSIZE) {
								logCOLUMNSIZE(column.getName() + "sw from " + iWidth + " to "
										+ (iWidth + diff) + "; End Size=" + column.getWidth()
										+ ";remaining=" + remaining);
							}
							iPrefWidthsUnderCount--;

						} else if (diff > 0) {
							column.setWidth(iPrefWidth);
						}
					}
				}
			}

			// fill in metrics map
			Map mapColumnMetricsNew = new HashMap();
			int iStartPos = COLUMN_MARGIN_WIDTH;
			for (int i = 0; i < visibleColumnsList.size(); i++) {
				TableColumnCore column = (TableColumnCore) visibleColumnsList.get(i);
				int width = column.getWidth();

				TableColumnMetrics metrics = new TableColumnMetrics(iStartPos, width);
				mapColumnMetricsNew.put(column, metrics);

				iStartPos += width + COLUMN_PADDING_WIDTH;
			}

			// make new values live
			mapColumnMetrics = mapColumnMetricsNew;

			lastVisibleColumns = new TableColumnCore[visibleColumnsList.size()];
			visibleColumnsList.toArray(lastVisibleColumns);

			// refresh
			changeColumnIndicator();

			refreshVisible(true, true, false);

		} finally {
			adjustingColumns = false;
		}

		return lastVisibleColumns;
	}

	public void setColumnList(TableColumnCore[] columns,
			String defaultSortColumnID, boolean titleIsMinWidth) {
		this.bTitleIsMinWidth = titleIsMinWidth;
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

		allColumns = tcManager.getAllTableColumnCoreAsArray(sTableID);

		Arrays.sort(allColumns, new Comparator() {
			public int compare(Object o1, Object o2) {
				TableColumn tc0 = (TableColumn) o1;
				TableColumn tc1 = (TableColumn) o2;
				return tc0.getPosition() - tc1.getPosition();
			}
		});

		ArrayList visibleColumnsList = new ArrayList();
		for (int i = 0; i < allColumns.length; i++) {
			if (allColumns[i].getPosition() >= 0) {
				visibleColumnsList.add(allColumns[i]);
			}
		}
		lastVisibleColumns = (TableColumnCore[]) visibleColumnsList.toArray(new TableColumnCore[0]);
		// TODO: Refresh all rows

		// Initialize the sorter after the columns have been added
		// TODO: Restore sort column and direction from config (list in TVSWTImpl)
		String sSortColumn = defaultSortColumnID;
		boolean bSortAscending = false;

		TableColumnCore tc = tcManager.getTableColumnCore(sTableID, sSortColumn);
		if (tc == null) {
			tc = lastVisibleColumns[0];
		}
		sortColumn = tc;
		sortColumn.setSortAscending(bSortAscending);

		if (bTitleIsMinWidth) {
			setColumnMinWidthToHeaders();
		}

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

	public TableRowCore[] getSelectedRows() {
		selectedRows_mon.enter();
		try {
			ListRow[] rows = new ListRow[selectedRows.size()];
			rows = (ListRow[]) selectedRows.toArray(rows);
			return rows;
		} finally {
			selectedRows_mon.exit();
		}
	}

	public int getSelectedRowsSize() {
		return selectedRows.size();
	}

	public void setSelectedRows(TableRowCore[] rows) {
		selectedRows_mon.enter();
		try {
			ArrayList rowsToSelect = new ArrayList();
			ArrayList rowsToDeselect = new ArrayList();
			for (int i = 0; i < rows.length; i++) {
				rowsToSelect.add(rows[i]);
			}
			TableRowCore[] selectedRows = getSelectedRows();

			// unselect already selected rows that aren't going to be selected anymore
			for (int i = 0; i < selectedRows.length; i++) {
				TableRowCore selectedRow = selectedRows[i];
				boolean bStillSelected = false;
				for (int j = 0; j < rows.length; j++) {
					TableRowCore row = rows[j];
					if (row.equals(selectedRow)) {
						bStillSelected = true;
						break;
					}
				}
				if (!bStillSelected) {
					rowsToDeselect.add(selectedRow);
				} else {
					rowsToSelect.remove(selectedRow);
				}
			}

			// trigger selection/deselection early, which will prevent each
			// row from firing one individually

			TableRowCore[] rowsToDeselectArray = new TableRowCore[rowsToDeselect.size()];
			rowsToDeselect.toArray(rowsToDeselectArray);
			triggerDeselectionListeners(rowsToDeselectArray);

			TableRowCore[] rowsToSelectArray = new TableRowCore[rowsToSelect.size()];
			rowsToSelect.toArray(rowsToSelectArray);
			triggerDeselectionListeners(rowsToSelectArray);

			bSkipSelectionTrigger = true;
			for (Iterator iter = rowsToDeselect.iterator(); iter.hasNext();) {
				ListRow row = (ListRow) iter.next();
				row.setSelected(false);
			}

			for (Iterator iter = rowsToSelect.iterator(); iter.hasNext();) {
				ListRow row = (ListRow) iter.next();
				row.setSelected(true);
			}

			if (rows.length > 0) {
				((ListRow) rows[0]).setFocused(true);
			}
		} finally {
			bSkipSelectionTrigger = false;
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

		if (!bSkipSelectionTrigger) {
			if (bSelected) {
				triggerDeselectionListeners(new TableRowCore[] {
					row
				});
			} else {
				triggerSelectionListeners(new TableRowCore[] {
					row
				});
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

		triggerFocusChangedListeners(row);
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
		refreshTable(false);
	}

	// XXX This gets called a lot.  Could store location and size on 
	//     resize/scroll of sc
	public Rectangle getBounds() {
		Rectangle clientArea = listCanvas.getClientArea();
		return new Rectangle(clientArea.x, -iLastVBarPos, clientArea.width,
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
						row.redraw();
					}
				}
			} finally {
				selectedRows_mon.exit();
			}
		} else if (event.type == SWT.Traverse) {
			event.doit = true;

			switch (event.detail) {
				case SWT.TRAVERSE_ARROW_NEXT:
					if (event.stateMask == SWT.MOD2) { // shift only
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
					} else if (event.stateMask == SWT.MOD1) { // control only
						ListRow focusedRow = getRowFocused();
						if (focusedRow != null) {
							int index = focusedRow.getIndex();
							index++;
							ListRow nextRow = getRow(index);
							if (nextRow != null) {
								nextRow.setFocused(true);
							}
						}
					} else if (event.stateMask == 0) {
						moveFocus(1, false);
					}
					break;

				case SWT.TRAVERSE_ARROW_PREVIOUS:
					if (event.stateMask == SWT.MOD2) { // shift only
						// select up
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
					} else if (event.stateMask == SWT.MOD1) { // control only
						// focus up
						ListRow focusedRow = getRowFocused();
						if (focusedRow != null) {
							int index = focusedRow.getIndex();
							index--;
							ListRow nextRow = getRow(index);
							if (nextRow != null) {
								nextRow.setFocused(true);
							}
						}
					} else if (event.stateMask == 0) {
						// focus up, selection replace
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
						selectAll();
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
							setSelectedRows(new ListRow[] {
								row
							});
						}
						break;
					}

					case SWT.END: {
						int i = rows.size();
						if (i > 0) {
							ListRow row = (ListRow) rows.get(i - 1);

							if (row != null) {
								setSelectedRows(new ListRow[] {
									row
								});
							}
						}
						break;
					}

					case SWT.F5: {
						System.out.println("F5");
						refreshVisible(true, true, true);
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
			triggerDefaultSelectedListeners(getSelectedRows());
		}
	}

	public void setMouseClickIsDefaultSelection(boolean b) {
		bMouseClickIsDefaultSelection = b;
	}

	public void addCountChangeListener(TableCountChangeListener listener) {
		listenersCountChange.add(listener);
	}

	protected void triggerListenerRowAdded(ListRow row) {
		for (Iterator iter = listenersCountChange.iterator(); iter.hasNext();) {
			TableCountChangeListener l = (TableCountChangeListener) iter.next();
			l.rowAdded(row);
		}
	}

	protected void triggerListenerRowRemoved(ListRow row) {
		for (Iterator iter = listenersCountChange.iterator(); iter.hasNext();) {
			TableCountChangeListener l = (TableCountChangeListener) iter.next();
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

	public void columnInvalidate(TableColumnCore tableColumn) {
		if (tableColumn.isVisible()) {
			columnInvalidate(tableColumn, true);
		} else {
			// TODO
		}
	}

	public void columnInvalidate(TableColumnCore tableColumn,
			final boolean bMustRefresh) {
		final String sColumnName = tableColumn.getName();

		runForAllRows(new TableGroupRowRunner() {
			public void run(TableRowCore row) {
				TableCellSWT cell = ((TableRowSWT) row).getTableCellSWT(sColumnName);
				if (cell != null)
					cell.invalidate(bMustRefresh);
			}
		});
	}

	// @see com.aelitis.azureus.ui.common.table.TableStructureModificationListener#columnOrderChanged(int[])
	public void columnOrderChanged(int[] iPositions) {
		// TODO Auto-generated method stub

	}

	// @see com.aelitis.azureus.ui.common.table.TableStructureModificationListener#columnSizeChanged(com.aelitis.azureus.ui.common.table.TableColumnCore)
	public void columnSizeChanged(TableColumnCore tableColumn) {
		if (adjustingColumns) {
			return;
		}
		if (isDisposed()) {
			return;
		}
		if (tableColumn.getPosition() < 0) {
			return;
		}
		lastClientWidth = 0;
		getVisibleColumns();
	}

	public void tableStructureChanged() {
		// force an eventual recalc of visible row widths
		lastClientWidth = 0;
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
		sortTable(false);
	}

	/**
	 * Sorts the table
	 * 
	 * @param bForce
	 * @return true: There were sort order changes (and the visible rows were
	 *               refreshed)<br>
	 *         false: No sort order changes
	 *
	 * @since 3.0.0.7
	 */
	private boolean sortTable(boolean bForce) {
		long lTimeStart;
		if (DEBUG_SORTER) {
			log(">>> Sort.. " + (sortColumn.getLastSortValueChange() - lLastSortedOn));
			lTimeStart = System.currentTimeMillis();
		}

		int iFirstChange = -1;
		try {
			row_mon.enter();

			if (sortColumn != null
					&& (bForce || sortColumn.getLastSortValueChange() > lLastSortedOn)) {
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

				if (DEBUG_SORTER) {
					log("numChanged " + iNumChanged);
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
					
					rowShow(getRowFocused());

					refreshVisible(true, true, true);
				}
			}

		} finally {
			row_mon.exit();
		}

		return iFirstChange >= 0;

		// Selection should be okay still.  May need to be moved into view
		// if we want that behaviour
	}

	public TableColumnCore[] getAllColumns() {
		return allColumns;
	}

	public boolean isRowVisible(final ListRow row) {
		final Boolean[] b = new Boolean[1];

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				b[0] = new Boolean(_isRowVisible(row));
			}
		}, false);

		return b[0].booleanValue();
	}

	public boolean _isRowVisible(ListRow row) {
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

	public TableRowSWT[] getVisibleRows() {
		if (listCanvas == null || listCanvas.isDisposed()) {
			return new TableRowSWT[0];
		}

		int y = iLastVBarPos;
		Rectangle clientArea = listCanvas.getClientArea();
		int iTopIndex = y / ListRow.ROW_HEIGHT;
		int iBottomIndex = (y + clientArea.height - 1) / ListRow.ROW_HEIGHT;

		int size = iBottomIndex - iTopIndex + 1;
		if (size <= 0)
			return new TableRowSWT[0];

		TableRowSWT[] visiblerows = new TableRowSWT[size];
		int pos = 0;

		for (int i = iTopIndex; i <= iBottomIndex; i++) {
			if (i >= 0 && i < rows.size()) {
				TableRowSWT row = (TableRowSWT) rows.get(i);
				if (row != null) {
					visiblerows[pos++] = row;
				}
			}
		}

		if (pos <= visiblerows.length) {
			// Some were null, shrink array
			TableRowSWT[] temp = new TableRowSWT[pos];
			System.arraycopy(visiblerows, 0, temp, 0, pos);
			return temp;
		}

		return visiblerows;
	}

	public boolean cellRefresh(final ListCell cell, final boolean bDoGraphics,
			final boolean bForceRedraw) {
		final Boolean[] b = new Boolean[1];

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				b[0] = new Boolean(_cellRefresh(cell, bDoGraphics, bForceRedraw));
			}
		}, false);

		return b[0].booleanValue();
	}

	public boolean _cellRefresh(final ListCell cell, final boolean bDoGraphics,
			final boolean bForceRedraw) {
		// assume cell if being refreshed if there's already a GC
		if (gcImgView != null || imgView == null) {
			return true;
		}

		try {
			gcImgView = new GC(imgView);

			cell.doPaint(gcImgView);

			Rectangle rect = cell.getBounds();
			listCanvas.redraw(rect.x, rect.y, rect.width, rect.height, false);
		} catch (Exception e) {
			Debug.out(e);
		} finally {
			if (gcImgView != null) {
				gcImgView.dispose();
				gcImgView = null;
			}
		}

		return true;
	}

	/**
	 * @param row
	 * @param bDoGraphics 
	 */
	public List rowRefresh(final ListRow row, final boolean bDoGraphics,
			final boolean bForceRedraw) {
		final List[] list = new List[1];

		if (DEBUGPAINT) {
			logPAINT("rowRefresh " + row + " force? " + bForceRedraw + " via "
					+ Debug.getCompressedStackTrace(5));
		}

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				list[0] = _rowRefresh(row, bDoGraphics, bForceRedraw);
			}
		}, false);

		return list[0];
	}

	public void rowRefreshAsync(final ListRow row, final boolean bDoGraphics,
			final boolean bForceRedraw) {
		if (DEBUGPAINT) {
			logPAINT("rowRefreshA " + row + " force? " + bForceRedraw + " via "
					+ Debug.getCompressedStackTrace(5));
		}

		try {
			rowsToRefresh_mon.enter();

			if (rowsToRefresh.contains(row)) {
				return;
			}
			rowsToRefresh.add(row);

			if (rowsToRefresh.size() > 1) {
				return;
			}
		} finally {
			rowsToRefresh_mon.exit();
		}

		if (!isDisposed()) {
			listCanvas.getDisplay().asyncExec(new AERunnable() {
				public void runSupport() {
					Object[] rows;
					try {
						rowsToRefresh_mon.enter();

						rows = rowsToRefresh.toArray();

						rowsToRefresh.clear();
					} finally {
						rowsToRefresh_mon.exit();
					}
					if (DEBUGPAINT) {
						logPAINT("rowRefreshA hit " + rows.length + " force? "
								+ bForceRedraw);
					}

					for (int i = 0; i < rows.length; i++) {
						ListRow row = (ListRow) rows[i];
						if (row.isVisible()) {
  						// XXX May be using the wrong boolean params!!
  						_rowRefresh(row, bDoGraphics, bForceRedraw);
						}
					}
				}
			});
		}
		;
	}

	private List _rowRefresh(ListRow row, boolean bDoGraphics,
			boolean bForceRedraw) {
		if (listCanvas == null || listCanvas.isDisposed()) {
			return new ArrayList();
		}

		Rectangle clientArea = listCanvas.getClientArea();
		int iTopIndex = getTopIndex();

		int i = row.getIndex();
		boolean changed = false;
		List changedItems = null;
		if (i >= iTopIndex) {
			int ofs = getOffset(iLastVBarPos);
			int y = (i - iTopIndex) * ListRow.ROW_HEIGHT - ofs;

			Rectangle rect = new Rectangle(clientArea.x, y, clientArea.width,
					ListRow.ROW_HEIGHT);

			if (imgView != null) {
				boolean isOurGC = gcImgView == null;
				/*
				 * 1) Refresh the row
				 * 2) Paint any columns (or full row) if they visually changed
				 */
				try {
					if (isOurGC) {
						gcImgView = new GC(imgView);
					}
					gcImgView.setClipping(rect);

					if (!row.isVisible()) {
						System.out.println("asked for row refresh but not visible "
								+ row.getIndex() + ";" + Debug.getCompressedStackTrace());
						return new ArrayList();
					}

					changedItems = row._refresh(bDoGraphics, true);
					boolean thisChanged = changedItems.size() > 0;
					changed |= thisChanged;

					if (bForceRedraw) {
						row.doPaint(gcImgView, true);
					} else if (thisChanged) {
						String sChanged = "" + row.getIndex() + " ";
						for (Iterator iter = changedItems.iterator(); iter.hasNext();) {
							Object item = iter.next();
							if (item instanceof TableRowSWT) {
								sChanged += ", r" + ((TableRowSWT) item).getIndex();
								((TableRowSWT) item).doPaint(gcImgView, true);
								break;
							}
							if (item instanceof TableCellSWT) {
								sChanged += "," + item;
								((TableCellSWT) item).doPaint(gcImgView);
							}
						}
						//log("rowRefresh: Items changed: " + sChanged);
					}
				} catch (Exception e) {
					if (!(e instanceof IllegalArgumentException)) {
						// IllegalArgumentException happens when we are already drawing 
						// to the image.  This is "normal" as we may be in a paint event,
						// and something forces a repaint
						Debug.out(e);
					} else {
						log("Already drawing on image: " + Debug.getCompressedStackTrace());
					}
				} finally {
					if (isOurGC && gcImgView != null) {
						gcImgView.dispose();
						gcImgView = null;
					}
				}
			}

			if (changed || bForceRedraw) {
				// paint the image onto the canvas
				listCanvas.redraw(rect.x, rect.y, rect.width, rect.height, false);

				// prevent recursion
				if (!isPaintingCanvas) {
					//listCanvas.update();
				}

				//System.out.println("redrawing row " + i + "/" + row.getIndex() 
				//	+ "; (" + clientArea.x + "," + y + ","
				//	+ clientArea.width + "," + ListRow.ROW_HEIGHT + ") via "
				//	+ Debug.getCompressedStackTrace());
			}
		}
		return changedItems == null ? new ArrayList() : changedItems;
	}

	private class canvasPaintListener
		implements Listener
	{
		Rectangle lastBounds = new Rectangle(0, 0, 0, 0);

		public void handleEvent(Event e) {
			try {
				isPaintingCanvas = true;

				doPaint(e);
			} finally {
				isPaintingCanvas = false;
			}
		}

		/**
		 * @param e
		 */
		private void doPaint(Event e) {
			if (imgView == null) {
				return;
			}

			if (vBar != null && !vBar.isDisposed()
					&& iLastVBarPos != vBar.getSelection()) {
				return;
			}

			if (e.width > 0) {
				if (DEBUGPAINT) {
					logPAINT("paint " + e.getBounds() + " image area: "
							+ imgView.getBounds() + "; pending=" + e.count);
				}
				e.gc.drawImage(imgView, e.x, e.y, e.width, e.height, e.x, e.y, e.width,
						e.height);
			}
		}
	}

	private int getTopIndex() {
		if (listCanvas == null || listCanvas.isDisposed()) {
			return -1;
		}

		return iLastVBarPos / ListRow.ROW_HEIGHT;
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
		Object[] listeners = listenersMenuFill.toArray();
		for (int i = 0; i < listeners.length; i++) {
			TableViewSWTMenuFillListener l = (TableViewSWTMenuFillListener) listeners[i];
			l.fillMenu(menu);
		}
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableViewSWT#addKeyListener(org.eclipse.swt.events.KeyListener)
	public void addKeyListener(KeyListener listener) {
		listCanvas.addKeyListener(listener);
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableViewSWT#addMenuFillListener(org.gudy.azureus2.ui.swt.views.table.TableViewSWTMenuFillListener)
	public void addMenuFillListener(TableViewSWTMenuFillListener l) {
		listenersMenuFill.add(l);
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableViewSWT#createDragSource(int)
	public DragSource createDragSource(int style) {
		return null;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableViewSWT#createDropTarget(int)
	public DropTarget createDropTarget(int style) {
		return null;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableViewSWT#getComposite()
	public Composite getComposite() {
		return listParent;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableViewSWT#getRow(org.eclipse.swt.dnd.DropTargetEvent)
	public TableRowCore getRow(DropTargetEvent event) {
		return null;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableViewSWT#getTableComposite()
	public Composite getTableComposite() {
		return listCanvas;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableViewSWT#obfusticatedImage(org.eclipse.swt.graphics.Image, org.eclipse.swt.graphics.Point)
	public Image obfusticatedImage(Image image, Point shellOffset) {
		return image;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableViewSWT#removeKeyListener(org.eclipse.swt.events.KeyListener)
	public void removeKeyListener(KeyListener listener) {
		listCanvas.addKeyListener(listener);
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#clipboardSelected()
	public void clipboardSelected() {
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#columnInvalidate(java.lang.String)
	public void columnInvalidate(String columnName) {
		TableColumnCore tc = TableColumnManager.getInstance().getTableColumnCore(
				sTableID, columnName);
		if (tc != null) {
			columnInvalidate(tc, tc.getType() == TableColumnCore.TYPE_TEXT_ONLY);
		}
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#delete()
	public void delete() {
		triggerLifeCycleListener(TableLifeCycleListener.EVENT_DESTROYED);

		UIUpdaterFactory.getInstance().removeUpdater(this);
		TableStructureEventDispatcher.getInstance(sTableID).removeListener(this);

		Utils.disposeSWTObjects(new Object[] {
			headerArea,
			listCanvas
		});
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#getColumnCells(java.lang.String)
	public TableCellCore[] getColumnCells(String sColumnName) {
		TableCellCore[] cells = new TableCellCore[rows.size()];

		try {
			row_mon.enter();

			int i = 0;
			for (Iterator iter = rows.iterator(); iter.hasNext();) {
				TableRowCore row = (TableRowCore) iter.next();
				cells[i++] = row.getTableCellCore(sColumnName);
			}

		} finally {
			row_mon.exit();
		}

		return cells;
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#getCoreTabViews()
	public IView[] getCoreTabViews() {
		return new IView[0];
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#getDataSources()
	public Object[] getDataSources() {
		return mapDataSourceToRow.keySet().toArray();
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#getFirstSelectedDataSource()
	public Object getFirstSelectedDataSource() {
		Object[] selectedDataSources = getSelectedDataSources();
		if (selectedDataSources.length > 0) {
			return selectedDataSources[0];
		}
		return null;
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#getFocusedRow()
	public TableRowCore getFocusedRow() {
		return getRowFocused();
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#getPropertiesPrefix()
	public String getPropertiesPrefix() {
		return sTableID;
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#getRows()
	public TableRowCore[] getRows() {
		return (TableRowCore[]) rows.toArray(new TableRowCore[0]);
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#isDisposed()
	public boolean isDisposed() {
		return listCanvas == null || listCanvas.isDisposed();
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#isTableFocus()
	public boolean isTableFocus() {
		return listCanvas.isFocusControl();
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#refreshTable(boolean)
	public void refreshTable(boolean forceSort) {
		if (listCanvas.isDisposed()) {
			return;
		}
		//log("updateUI via " + Debug.getCompressedStackTrace());
		processDataSourceQueue();

		if (!sortTable(forceSort)) {
			iGraphicRefresh++;
			boolean bDoGraphics = (iGraphicRefresh % graphicsUpdate) == 0;
			refreshVisible(bDoGraphics, false, true);
		}
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#removeAllTableRows()
	public void removeAllTableRows() {
		removeAllDataSources(true);
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#selectAll()
	public void selectAll() {
		setSelectedRows(getRowsUnsorted());
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#setCoreTabViews(org.gudy.azureus2.ui.swt.views.IView[])
	public void setCoreTabViews(IView[] coreTabViews) {
		// XXX TabViews not supported
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#setEnableTabViews(boolean)
	public void setEnableTabViews(boolean enableTabViews) {
		// XXX TabViews not supported
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#setFocus()
	public void setFocus() {
		listCanvas.setFocus();
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#setRowDefaultHeight(int)
	public void setRowDefaultHeight(int height) {
		ListRow.ROW_HEIGHT = height + (ListView.ROW_MARGIN_HEIGHT * 2);
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#setRowDefaultIconSize(org.eclipse.swt.graphics.Point)
	public void setRowDefaultIconSize(Point size) {
		ListRow.ROW_HEIGHT = size.y + (ListView.ROW_MARGIN_HEIGHT * 2);
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#updateLanguage()
	public void updateLanguage() {
	}

	// @see org.gudy.azureus2.core3.util.AEDiagnosticsEvidenceGenerator#generate(org.gudy.azureus2.core3.util.IndentWriter)
	public void generate(IndentWriter writer) {
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#runForAllRows(com.aelitis.azureus.ui.common.table.TableGroupRowVisibilityRunner)
	public void runForAllRows(TableGroupRowVisibilityRunner runner) {
		TableRowCore[] rows = getRows();

		for (int i = 0; i < rows.length; i++) {
			runner.run(rows[i], rows[i].isVisible());
		}
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#runForSelectedRows(com.aelitis.azureus.ui.common.table.TableGroupRowRunner)
	public void runForSelectedRows(TableGroupRowRunner runner) {
		TableRowCore[] rows = getSelectedRows();
		if (runner.run(rows)) {
			return;
		}

		for (int i = 0; i < rows.length; i++) {
			runner.run(rows[i]);
		}
	}

	/**
	 * @return
	 */
	protected TableViewSWTPanelCreator getMainPanelCreator() {
		return mainPanelCreator;
	}

	// @see org.gudy.azureus2.ui.swt.views.TableViewSWT#setMainPanelCreator(org.gudy.azureus2.ui.swt.views.TableViewMainPanelCreator)
	public void setMainPanelCreator(TableViewSWTPanelCreator mainPanelCreator) {
		this.mainPanelCreator = mainPanelCreator;
	}

	public TableColumnMetrics getColumnMetrics(TableColumn column) {
		TableColumnMetrics metrics = (TableColumnMetrics) mapColumnMetrics.get(column);
		return metrics;
	}
}
