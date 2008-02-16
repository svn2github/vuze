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
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.views.IView;
import org.gudy.azureus2.ui.swt.views.table.*;
import org.gudy.azureus2.ui.swt.views.table.impl.TableCellImpl;
import org.gudy.azureus2.ui.swt.views.table.impl.TableTooltips;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnEditorWindow;
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
	TableStructureModificationListener, KeyListener
{
	public static int COLUMN_MARGIN_WIDTH = 3;

	public static int COLUMN_PADDING_WIDTH = COLUMN_MARGIN_WIDTH * 2;

	private final static LogIDs LOGID = LogIDs.UI3;

	private static final boolean DEBUGPAINT = false;

	private static final boolean DEBUG_SORTER = false;

	private static final boolean DEBUG_COLUMNSIZE = false;

	private static final boolean DEMO_DRAGROW = false;

	private static final boolean DELAY_SCROLL = false;

	// Shorter name for ConfigManager, easier to read code
	private static final ConfigurationManager configMan = ConfigurationManager.getInstance();

	private static final String CFG_SORTDIRECTION = "config.style.table.defaultSortOrder";

	public int rowMarginHeight = 2;

	public int rowHeightDefault = 38;

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

	private TableColumnCore[] allColumns = new TableColumnCore[0];

	private ScrollBar vBar;

	private Image imgView = null;

	private GC gcImgView = null;

	private int iLastVBarPos;

	protected Object[] restartRefreshVisible = null;

	private boolean bInRefreshVisible;

	protected boolean viewVisible;

	private List listenersMenuFill = new ArrayList();

	private ArrayList listenersKey = new ArrayList();

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

	private Rectangle lastBounds = new Rectangle(0, 0, 0, 0);

	private Listener lShowHide;

	private RowInfo topRowInfo = null;

	private RowInfo bottomRowInfo = null;

	private int totalHeight;

	private Rectangle clientArea;

	private Menu menuHeader;

	private static final Comparator rowYPosComparator;

	protected Color colorRowOddBG;

	protected Color colorRowOddFG;

	protected Color colorRowEvenBG;

	protected Color colorRowEvenFG;

	protected Color colorRowSelectedOddBG;

	protected Color colorRowSelectedOddFG;

	protected Color colorRowSelectedEvenBG;

	protected Color colorRowSelectedEvenFG;

	protected Color colorRowDivider;

	protected Color colorRowFocus;

	private Display display;

	protected int rowFocusStyle;

	static {
		rowYPosComparator = new Comparator() {
			public int compare(Object arg0, Object arg1) {
				long index0 = (arg0 instanceof ListRow)
						? ((ListRow) arg0).getBasicYPos() : ((Long) arg0).longValue();
				long index1 = (arg1 instanceof ListRow)
						? ((ListRow) arg1).getBasicYPos() : ((Long) arg1).longValue();

				return (int) (index0 - index1);
			}
		};
	}

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
		display = parent.getDisplay();
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
		clientArea = listCanvas.getClientArea();

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
		lShowHide = new Listener() {
			public void handleEvent(final Event event) {
				boolean toBeVisible = event.type == SWT.Show;

				if (event.widget == listCanvas) {
					viewVisible = toBeVisible;
				} else if (!toBeVisible || listCanvas.isVisible()) {
					viewVisible = toBeVisible;
				} else {
					// for the moment, assume visible is true
					viewVisible = true;

					// container item.. check listCanvas.isVisible(), but only after
					// events have been processed, so that the visibility is propogated
					// to the listCanvas
					display.asyncExec(new AERunnable() {
						public void runSupport() {
							viewVisible = listCanvas.isVisible();
						}
					});
				}

				if (viewVisible) {
					// asyncExec so SWT finishes up it's show routine
					// Otherwise, the scrollbar visibility setting will fail
					display.asyncExec(new AERunnable() {
						public void runSupport() {
							refreshVisible(true, true, true);
							refreshScrollbar();
							handleResize(true);
						}
					});
				}
			}
		};

		viewVisible = false;
		// We pretend view is invisible and make it visible later to 
		// speed up startup.  Commented out code is the real visiblility getter
		//viewVisible = true;
		Composite walkUp = listCanvas;
		do {
			//viewVisible &= walkUp.isVisible();
			walkUp.addListener(SWT.Show, lShowHide);
			walkUp.addListener(SWT.Hide, lShowHide);
			walkUp = walkUp.getParent();
		} while (walkUp != null);

		listCanvas.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event event) {
				handleResize(false);
			}
		});

		listCanvas.addListener(SWT.Paint, new canvasPaintListener());

		selectionListener l = new selectionListener();
		listCanvas.addListener(SWT.MouseDown, l);
		listCanvas.addListener(SWT.MouseUp, l);
		listCanvas.addListener(SWT.MouseMove, l);
		listCanvas.addListener(SWT.MouseDoubleClick, l);

		listCanvas.addListener(SWT.FocusIn, this);
		listCanvas.addListener(SWT.FocusOut, this);
		listCanvas.addListener(SWT.Traverse, this);
		listCanvas.addListener(SWT.DefaultSelection, this);
		listCanvas.addKeyListener(this);

		listCanvas.setMenu(createMenu());

		Listener mouseListener = new Listener() {
			TableCellSWT lastCell = null;

			TableRowCore lastRow = null;

			int lastCursorID = -1;

			public void handleEvent(Event e) {
				try {
					boolean bExited = e.type == SWT.MouseExit;
					TableRowCore row = bExited ? null : getRow(e.x, e.y);
					TableCellSWT cell = bExited ? null : getTableCell(e.x, e.y);
					//System.out.println(sTableID + "] mouse event row=" + row + ";cell=" + cell + ";" + (row == null ? "" : "" + ((ListRow)row).getHeight()));
					int iCursorID = -2;

					boolean changedCell = lastCell != cell;
					boolean changedRow = row != lastRow;

					// Exit previous
					if (changedCell && lastCell != null && !lastCell.isDisposed()) {
						TableCellMouseEvent event = createMouseEvent(lastCell, e,
								TableCellMouseEvent.EVENT_MOUSEEXIT);
						if (event != null) {
							TableColumnCore tc = ((TableColumnCore) lastCell.getTableColumn());
							tc.invokeCellMouseListeners(event);
							lastCell.invokeMouseListeners(event);
						}
					}

					if (changedRow && lastRow != null && !lastRow.isRowDisposed()) {
						TableCellMouseEvent event = createMouseEvent(lastCell, e,
								TableCellMouseEvent.EVENT_MOUSEEXIT);
						if (event != null) {
							event.row = lastRow;
							lastRow.invokeMouseListeners(event);
						}
					}

					// Enter new
					if (cell == null) {
						lastCell = null;
					} else {
						if (changedCell) {
							TableCellMouseEvent event = createMouseEvent(cell, e,
									TableCellMouseEvent.EVENT_MOUSEENTER);
							if (event != null) {
								TableColumnCore tc = ((TableColumnCore) cell.getTableColumn());
								tc.invokeCellMouseListeners(event);
								cell.invokeMouseListeners(event);
							}
							iCursorID = cell.getCursorID();
							lastCell = cell;
						}
					}

					if (row == null) {
						lastRow = null;
					} else {
						if (changedRow) {
							TableCellMouseEvent event = createMouseEvent(cell, e,
									TableCellMouseEvent.EVENT_MOUSEENTER);
							if (event != null) {
								event.row = row;
								row.invokeMouseListeners(event);
							}
							lastRow = row;
						}
					}

					// cursor
					if (iCursorID != lastCursorID) {
						lastCursorID = iCursorID;

						if (iCursorID >= 0) {
							listParent.setCursor(display.getSystemCursor(iCursorID));
						} else if (iCursorID == -1) {
							listParent.setCursor(null);
						}
					}

					// mouse move for good gesture
					if (cell != null) {
						TableCellMouseEvent event = createMouseEvent(cell, e,
								TableCellMouseEvent.EVENT_MOUSEMOVE);
						if (event != null) {
							TableColumnCore tc = ((TableColumnCore) cell.getTableColumn());
							if (tc.hasCellMouseMoveListener()) {
								((TableColumnCore) cell.getTableColumn()).invokeCellMouseListeners(event);
							}
							cell.invokeMouseListeners(event);
						}
					}
					if (row != null) {
						TableCellMouseEvent event = createMouseEvent(cell, e,
								TableCellMouseEvent.EVENT_MOUSEMOVE);
						if (event != null) {
							event.row = row;
							row.invokeMouseListeners(event);
						}
					}
				} catch (Exception ex) {
					Debug.out(ex);
				}
			}
		};

		listCanvas.addListener(SWT.MouseMove, mouseListener);
		listCanvas.addListener(SWT.MouseExit, mouseListener);

		listCanvas.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				delete();
			}
		});

		new TableTooltips(this, listCanvas);

		if (headerArea != null) {
			setupHeader(headerArea);
		}

		TableStructureEventDispatcher.getInstance(sTableID).addListener(this);

		initializeDefaultRowInfo();

		triggerLifeCycleListener(TableLifeCycleListener.EVENT_INITIALIZED);

		if (!viewVisible && listCanvas.isVisible()) {
			display.asyncExec(new AERunnable() {
				public void runSupport() {
					Event e = new Event();
					e.type = SWT.Show;
					e.widget = listCanvas;
					lShowHide.handleEvent(e);
				}
			});
		}
	}

	/**
	 * 
	 *
	 * @since 3.0.4.3
	 */
	private void initializeDefaultRowInfo() {
		if (skinProperties != null) {
			rowMarginHeight = skinProperties.getIntValue("table." + sTableID
					+ ".row.margin.height", rowMarginHeight);
			rowHeightDefault = skinProperties.getIntValue("table." + sTableID
					+ ".row.height", rowHeightDefault);

			String sID;
			String sID2;
			sID = "color.row.odd.selected.bg";
			colorRowSelectedOddBG = getSkinColor("table." + sTableID + "." + sID, sID);

			sID = "color.row.even.selected.bg";
			colorRowSelectedEvenBG = getSkinColor("table." + sTableID + "." + sID,
					sID);

			sID = "color.row.odd.selected.fg";
			colorRowSelectedOddFG = getSkinColor("table." + sTableID + "." + sID, sID);

			sID = "color.row.even.selected.fg";
			colorRowSelectedEvenFG = getSkinColor("table." + sTableID + "." + sID,
					sID);

			sID = "color.row.odd.bg";
			colorRowOddBG = getSkinColor("table." + sTableID + "." + sID, sID);

			sID = "color.row.even.bg";
			colorRowEvenBG = getSkinColor("table." + sTableID + "." + sID, sID);

			sID = "color.row.odd.fg";
			colorRowOddFG = getSkinColor("table." + sTableID + "." + sID, sID);

			sID = "color.row.even.fg";
			colorRowEvenFG = getSkinColor("table." + sTableID + "." + sID, sID);

			sID = "color.row.divider";
			sID2 = "table." + sTableID + "." + sID;
			String val = skinProperties.getStringValue(sID2);
			if (val != null && val.length() == 0) {
				colorRowDivider = null;
			} else {
				colorRowDivider = getSkinColor(sID2, sID);
			}

			sID = "color.row.focus";
			sID2 = "table." + sTableID + "." + sID;
			val = skinProperties.getStringValue(sID2);
			if (val != null && val.length() == 0) {
				colorRowFocus = null;
			} else {
				colorRowFocus = getSkinColor(sID2, sID);
			}

			rowFocusStyle = skinProperties.getStringValue(
					"table." + sTableID + ".row.focus.style", "dot").toLowerCase().equals(
					"dot") ? SWT.LINE_DOT : SWT.LINE_SOLID;
		}

		colorRowOddBG = pickColorIfNull(colorRowOddBG, colorRowEvenBG,
				display.getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		colorRowOddFG = pickColorIfNull(colorRowOddFG, colorRowEvenFG,
				display.getSystemColor(SWT.COLOR_LIST_FOREGROUND));

		colorRowEvenBG = pickColorIfNull(colorRowEvenBG, colorRowOddBG,
				display.getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		colorRowEvenFG = pickColorIfNull(colorRowEvenFG, colorRowOddFG,
				display.getSystemColor(SWT.COLOR_LIST_FOREGROUND));

		colorRowSelectedEvenBG = pickColorIfNull(colorRowSelectedEvenBG,
				colorRowSelectedOddBG, display.getSystemColor(SWT.COLOR_LIST_SELECTION));
		colorRowSelectedEvenFG = pickColorIfNull(colorRowSelectedEvenFG,
				colorRowSelectedOddFG,
				display.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));

		colorRowSelectedOddBG = pickColorIfNull(colorRowSelectedOddBG,
				colorRowSelectedEvenBG,
				display.getSystemColor(SWT.COLOR_LIST_SELECTION));
		colorRowSelectedOddFG = pickColorIfNull(colorRowSelectedOddFG,
				colorRowSelectedEvenFG,
				display.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
	}

	/**
	 *
	 * @since 3.0.4.3
	 */
	private Color pickColorIfNull(Color colorMaybeNull, Color replacementColor,
			Color backupColor) {
		if (colorMaybeNull == null) {
			if (replacementColor != null) {
				return replacementColor;
			} else {
				return backupColor;
			}
		}
		return colorMaybeNull;
	}

	/**
	 * @param sid
	 * @param string
	 *
	 * @since 3.0.4.3
	 */
	private Color getSkinColor(String id1, String id2) {
		Color color = skinProperties.getColor(id1);
		if (color == null) {
			color = skinProperties.getColor(id2);
		}
		return color;
	}

	/**
	 * 
	 *
	 * @param bForce 
	 * @since 3.0.0.7
	 */
	protected void handleResize(boolean bForce) {
		boolean bNeedsRefresh = false;
		if (listCanvas == null || listCanvas.isDisposed()) {
			return;
		}

		clientArea = listCanvas.getClientArea();

		if (clientArea.width == 0 || clientArea.height == 0) {
			return;
		}

		if (lastBounds.height != clientArea.height || bottomRowInfo == null) {
			// don't need to recalc bottom row if only width changed.
			// If a row happens to change height, that is dealt with later
			bottomRowInfo = findBottomRow(iLastVBarPos, clientArea.height);
		}

		if (imgView == null || bForce) {
			if (DEBUGPAINT) {
				logPAINT("first resize (img null)");
			}
			if (imgView != null && !imgView.isDisposed())
				imgView.dispose();
			imgView = new Image(display, clientArea);
			lastBounds = new Rectangle(0, 0, 0, 0);
			bNeedsRefresh = true;
		} else if (!lastBounds.equals(clientArea)) {
			// resize image by creating a new one, drawing the old one on it,
			// and blanking out the new areas
			bNeedsRefresh = lastBounds.height != clientArea.height;
			Image newImageView = new Image(display, clientArea);
			GC gc = null;
			try {
				gc = new GC(newImageView);
				gc.setAdvanced(true);
				gc.drawImage(imgView, 0, 0);

				Region reg = new Region();
				reg.add(clientArea);
				reg.subtract(imgView.getBounds());
				gc.setClipping(reg);

				//gc.setBackground(Display.getDefault().getSystemColor((int)(Math.random() * 16)));
				gc.setBackground(listCanvas.getBackground());
				gc.fillRectangle(clientArea);

				gc.setClipping((Region) null);
				reg.dispose();
			} finally {
				if (gc != null) {
					gc.dispose();
				}
			}
			imgView.dispose();
			imgView = newImageView;
			//listCanvas.update();
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

				int y0 = iLastVBarPos + lastBounds.y + lastBounds.height;

				long lStart = System.currentTimeMillis();

				int y = y0;
				int numDrawn = 0;
				RowInfo rowInfo = getRowAbsolute(0, y);
				while (rowInfo != null && rowInfo.row.isVisible()) {
					if (DEBUGPAINT) {
						logPAINT("resize: doPaint @" + rowInfo.row.getBasicYPos());
					}
					rowInfo.row.doPaint(gcImgView, true);
					y += rowInfo.row.getHeight();
					rowInfo = getRowAbsolute(0, y);
					numDrawn++;
					if (DEBUGPAINT) {
						logPAINT("numDrawn " + numDrawn);
					}
				}

				// Blank out area below visible rows
				int gap = clientArea.y + clientArea.height - totalHeight;
				if (gap > 0) {
					if (DEBUGPAINT) {
						logPAINT("fill " + gap + "@" + (clientArea.height - gap));
					}
					gcImgView.setBackground(listCanvas.getBackground());
					gcImgView.fillRectangle(0, clientArea.y + clientArea.height - gap,
							clientArea.width, gap);
				}

				long diff = System.currentTimeMillis() - lStart;
				if (diff > 100) {
					log(diff + "ms to paint " + numDrawn + " on redraw");
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

	private int getBottomRowHeight(int vBarPos) {
		if (bottomRowInfo == null) {
			return 0;
		}

		int visibleHeight = (vBarPos + clientArea.height)
				- bottomRowInfo.row.getBasicYPos();
		int rowHeight = bottomRowInfo.row.getHeight();
		if (visibleHeight > rowHeight) {
			visibleHeight = rowHeight;
		}

		return visibleHeight;
	}

	/**
	 * 
	 */
	protected boolean refreshScrollbar() {
		if (!viewVisible || vBar == null || vBar.isDisposed()) {
			return false;
		}
		boolean changed = false;

		if (totalHeight < clientArea.height || clientArea.height == 0) {
			if (vBar.isVisible()) {
				vBar.setVisible(false);
				logPAINT("redraw via refreshScrollbar");
				listCanvas.redraw();
				if (headerArea != null) {
					headerArea.redraw();
				}
			}
			iLastVBarPos = 0;
		} else {
			if (!vBar.isVisible()) {
				vBar.setVisible(true);
				logPAINT("redraw via refreshScrollbar");
				listCanvas.redraw();
				if (headerArea != null) {
					headerArea.redraw();
				}
				changed = true;
			}
			vBar.setIncrement(rowHeightDefault);
			int thumb = clientArea.height;
			int maximum = vBar.getMaximum();
			if (maximum != totalHeight) {
				vBar.setMaximum(totalHeight);
			}
			vBar.setThumb(thumb);
			vBar.setPageIncrement(clientArea.height / 2);
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

			// adjust toprow
			topRowInfo = findTopRow(iThisVBarPos);
			bottomRowInfo = findBottomRow(iThisVBarPos, clientArea.height);

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

			logPAINT("redraw via scrollTo");
			listCanvas.redraw();
			listCanvas.update();
		}
		iLastVBarPos = iThisVBarPos;

		if (DEBUGPAINT) {
			logPAINT("done in " + (System.currentTimeMillis() - lTimeStart));
		}
	}

	public static class RowInfo
	{
		int index;

		ListRow row;

		public RowInfo(ListRow row, int index) {
			this.row = row;
			this.index = index;
		}
	}

	/**
	 * @param atYPos
	 *
	 * @since 3.0.4.3
	 */
	private RowInfo findTopRow(int atYPos) {
		RowInfo rowInfo = getRowAbsolute(0, atYPos);
		if (rowInfo == null) {
			return null;
		}
		//System.out.println(sTableID + "] toprow @" + atYPos + "=" + rowInfo.index);
		return rowInfo;
	}

	private RowInfo findBottomRow(int atYPos, int height) {
		RowInfo rowInfo = getRowAbsolute(0, atYPos + height - 1);
		if (rowInfo == null) {
			if (rows.size() == 0) {
				return null;
			}
			ListRow row = (ListRow) rows.get(rows.size() - 1);
			rowInfo = new RowInfo(row, rows.size() - 1);
		}
		//log("bottomrow @" + atYPos + "=" + rowInfo.index);

		return rowInfo;
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
					gc.drawImage(imgView, 0, -diff, bounds.width, h, 0, 0, bounds.width,
							h);
				} else {
					// Windows can't use drawImage on same image
					gc.copyArea(0, -diff, bounds.width, h, 0, 0);
				}
			}
		}

		if (bMoveOnly) {
			return;
		}

		topRowInfo = findTopRow(iThisVBarPos);
		bottomRowInfo = findBottomRow(iThisVBarPos, clientArea.height);
		//		System.out.println(sTableID + "] ftr2 " + topRowInfo.index + ";b="
		//				+ bottomRowInfo.index + ";" + iThisVBarPos);

		iLastVBarPos = iThisVBarPos;
		TableRowSWT[] visibleRows = getVisibleRows();
		if (diff < 0) {
			int ofs = getBottomRowHeight(iThisVBarPos);
			// image moved up.. gap at bottom
			int i = visibleRows.length - 1;

			if (i < 0) {
				if (DEBUGPAINT) {
					logPAINT("No rows visible! This shouldn't happen");
				}
				return;
			}

			ListRow row = (ListRow) visibleRows[i];
			while (diff <= 0) {
				if (DEBUGPAINT) {
					logPAINT("scrollTo repaint visRow#" + i + "(idx:" + row.getIndex()
							+ ") d=" + diff + ";ofs=" + ofs);
				}
				row.doPaint(gc, true);
				i--;
				if (i < 0) {
					break;
				}
				diff += ofs;
				row = (ListRow) visibleRows[i];
				ofs = row.getHeight();
			}
			if (i >= 0) {
				row = (ListRow) visibleRows[i];
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
			int ofs = topRowInfo.row.getHeight()
					- (iThisVBarPos - topRowInfo.row.getBasicYPos());
			ListRow row = (ListRow) visibleRows[i];
			while (diff >= 0) {
				if (DEBUGPAINT) {
					logPAINT("repaint " + i + "(" + row.getIndex() + ") d=" + diff
							+ ";o=" + ofs);
				}
				row.doPaint(gc, true);
				i++;
				if (i >= visibleRows.length) {
					break;
				}
				diff -= ofs;
				row = (ListRow) visibleRows[i];
				ofs = row.getHeight();
			}
		}
	}

	/**
	 * @param headerArea
	 */
	private void setupHeader(final Composite headerArea) {
		this.headerArea = headerArea;

		menuHeader = new Menu(headerArea.getShell(), SWT.POP_UP);
		headerArea.setMenu(menuHeader);

		menuHeader.addMenuListener(new MenuListener() {

			public void menuShown(MenuEvent e) {
				MenuItem[] items = menuHeader.getItems();
				Utils.disposeSWTObjects(items);

				Point pt = headerArea.toControl(display.getCursorLocation());
				final TableColumnCore inColumn = getColumnHeaderMouseIn(pt.x, pt.y);
				if (inColumn != null) {
					MenuItem itemSortOn = new MenuItem(menuHeader, SWT.PUSH);
					Messages.setLanguageText(itemSortOn, "menu.sortByColumn",
							new String[] {
								MessageText.getString(inColumn.getTitleLanguageKey())
							});
					itemSortOn.addListener(SWT.Selection, new Listener() {
						public void handleEvent(Event event) {
							setSortColumn(inColumn);
						}
					});
				}

				MenuItem itemEdit = new MenuItem(menuHeader, SWT.PUSH);
				itemEdit.setText(MessageText.getString("MyTorrentsView.menu.editTableColumns"));
				itemEdit.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event event) {
						String tableID = getTableID();
						TableRowCore focusedRow = getFocusedRow();
						new TableColumnEditorWindow(getComposite().getShell(), tableID,
								getAllColumns(), focusedRow,
								TableStructureEventDispatcher.getInstance(tableID));
					}
				});
			}

			public void menuHidden(MenuEvent e) {
			}
		});

		final Cursor cursor = new Cursor(display, SWT.CURSOR_HAND);
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
				if (e.button != 1) {
					return;
				}

				TableColumnCore inColumn = getColumnHeaderMouseIn(e.x, e.y);
				if (inColumn != null) {
					setSortColumn(inColumn);
					if (DEBUG_SORTER) {
						log("sorting on " + inColumn.getName());
					}
				}
			}

		});

		headerArea.addMouseMoveListener(new MouseMoveListener() {
			Cursor cursor = null;

			public void mouseMove(MouseEvent e) {
				TableColumnCore[] columns = getVisibleColumns();
				int inColumnNo = -1;
				Rectangle bounds = null;
				for (int i = 0; i < columns.length; i++) {
					bounds = (Rectangle) headerArea.getData("Column" + i + "Bounds");
					if (bounds != null && bounds.contains(e.x, e.y)) {
						inColumnNo = i;
						break;
					}
				}
				boolean inColumn = inColumnNo != -1;
				if (inColumn) {
					//					GC gc = new GC(headerArea);
					//					gc.drawRectangle(bounds);
					//					gc.dispose();
				}
				Cursor newCursor = inColumn ? cursor : null;
				if (cursor != newCursor) {
					headerArea.setCursor(newCursor);
					cursor = newCursor;
				}
			}
		});

		headerArea.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				TableColumnCore[] columns = lastVisibleColumns;
				if (columns == null) {
					return;
				}

				Color colorFG = null;
				Color colorDivider = null;
				if (skinProperties != null) {
					colorFG = skinProperties.getColor("color.list.header.fg");
					colorDivider = skinProperties.getColor("color.list.header.divider");
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
						if (align == SWT.CENTER) {
							align = SWT.RIGHT;
						}
						drawWidth += ListView.COLUMN_MARGIN_WIDTH;
						int giveSpace = Math.min(lastExtraSpace, size.x - drawWidth);
						bounds = new Rectangle(pos - giveSpace, clientArea.y + 2, drawWidth
								+ giveSpace, clientArea.height);
					} else {
						bounds = new Rectangle(pos, clientArea.y + 2, drawWidth,
								clientArea.height);
					}

					if (i > 0 && colorDivider != null) {
						e.gc.setForeground(colorDivider);
						e.gc.drawLine(pos - 2, clientArea.y + size.y + 7, pos - 2,
								clientArea.y + clientArea.height);
					}

					headerArea.setData("Column" + i + "Bounds", bounds);

					if (text.length() > 0) {
						if (colorFG != null) {
							e.gc.setForeground(colorFG);
						}
						GCStringPrinter.printString(e.gc, text, bounds, false, false, align
								| SWT.TOP);
					}

					int middlePos = bounds.x;
					if (align == SWT.LEFT) {
						lastExtraSpace = bounds.width - size.x;
						middlePos += size.x / 2;
					} else if (align == SWT.CENTER) {
						lastExtraSpace = (bounds.width - size.x) / 2 + 1;
						middlePos += bounds.width / 2;
					} else {
						lastExtraSpace = 0;
						middlePos += bounds.width - (size.x / 2);
					}

					if (columns[i].equals(sortColumn)) {
						Image img = sortColumn.isSortAscending() ? imgSortAsc : imgSortDesc;
						if (img != null) {
							Rectangle imgBounds = img.getBounds();
							e.gc.drawImage(img, middlePos - (imgBounds.width / 2),
									bounds.height + bounds.y - imgBounds.height - 3);
						}
					}

					//System.out.println(i + ";xtraspace=" + lastExtraSpace);

					//e.gc.drawLine(pos, bounds.y, pos, bounds.y + bounds.height);
					pos += width + (ListView.COLUMN_MARGIN_WIDTH * 2);
				}
			}
		});
	}

	/**
	 * 
	 *
	 * @return 
	 * @since 3.0.4.3
	 */
	protected TableColumnCore getColumnHeaderMouseIn(int x, int y) {
		TableColumnCore[] columns = getVisibleColumns();
		int inColumn = -1;
		for (int i = 0; i < columns.length; i++) {
			Rectangle bounds = (Rectangle) headerArea.getData("Column" + i + "Bounds");
			if (bounds != null && bounds.contains(x, y)) {
				inColumn = i;
				break;
			}
		}
		if (inColumn != -1) {
			return columns[inColumn];
		}
		return null;
	}

	public Menu getTableHeaderMenu() {
		return menuHeader;
	}

	/**
	 * 
	 *
	 * @since 3.0.0.7
	 */
	private void setColumnMinWidthToHeaders() {
		if (headerArea == null) {
			return;
		}
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
		if (isDisposed() || !viewVisible) {
			if (DEBUGPAINT) {
				logPAINT("cancel invisible refreshVisible "
						+ Debug.getCompressedStackTrace());
			}
			return;
		}
		if (bInRefreshVisible) {
			if (DEBUGPAINT) {
				logPAINT("Set flag to restart visible because of "
						+ Debug.getCompressedStackTrace());
			}
			//log("Set flag to restart visible because of "					+ Debug.getCompressedStackTrace());
			restartRefreshVisible = new Object[] {
				new Boolean(doGraphics),
				new Boolean(bForceRedraw),
				new Boolean(bAsync)
			};
			//System.out.println("-1-");
			return;
		}
		bInRefreshVisible = true;
		if (DEBUGPAINT) {
			logPAINT("Start refreshVisible " + Debug.getCompressedStackTrace());
		}
		//log("Start refreshVisible " + Debug.getCompressedStackTrace());

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
									if (DEBUGPAINT) {
										logPAINT(sTableID + "] -2-" + indexOf(row) + ";"
												+ row.getBasicYPos() + ";");
									}

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
						boolean newDoGraphics = ((Boolean) params[0]).booleanValue()
								|| doGraphics;
						boolean newForceRedraw = ((Boolean) params[1]).booleanValue()
								|| bForceRedraw;
						boolean newAsync = ((Boolean) params[2]).booleanValue();
						refreshVisible(newDoGraphics, newForceRedraw, newAsync);
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
			int count = 0;

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
						if (rows.size() == 0) {
							topRowInfo = new RowInfo(row, 0);
							bottomRowInfo = new RowInfo(row, 0);
						}

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
						if (sortColumn == null
								|| (rows.size() > 0 && sortColumn.compare(row,
										rows.get(rows.size() - 1)) >= 0)) {
							index = rows.size();
						} else {
							index = Collections.binarySearch(rows, row, sortColumn);
							if (index < 0) {
								index = -1 * index - 1; // best guess
							}

							if (index > rows.size()) {
								index = rows.size();
							}
						}

						if (iFirstChange < 0 || iFirstChange > index) {
							iFirstChange = index;
						}

						rows.add(index, row);
						row.setBasicYPos(0);
						logADDREMOVE("addDS pos " + index);

						mapDataSourceToRow.put(datasource, row);

						triggerListenerRowAdded(row);
					}
				} finally {
					row_mon.exit();

					if (iFirstChange >= 0) {
						fixUpPositions(iFirstChange, false);
					}
					bottomRowInfo = findBottomRow(iLastVBarPos, clientArea.height);
					topRowInfo = findTopRow(iLastVBarPos);

					if (!viewVisible && listCanvas.isVisible() && lShowHide != null) {
						Event e = new Event();
						e.type = SWT.Show;
						e.widget = listCanvas;
						lShowHide.handleEvent(e);
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
		System.err.println(System.currentTimeMillis() + ":" + sTableID + "] "
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
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
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
										newFocusRow = (ListRow) getRow(newIndex);
									}
								}
							}

							row = (ListRow) mapDataSourceToRow.remove(datasource);
							if (row == null) {
								return;
							}

							// Delete row before removing in case delete(..) calls back a method
							// which needs rows.
							totalHeight -= row.getHeight();
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

					fixUpPositions(firstIndex, false);
					bottomRowInfo = findBottomRow(iLastVBarPos, clientArea.height);

					if (newFocusRow != null) {
						//System.out.println("SR " + newFocusRow.getIndex());
						rowSetFocused(newFocusRow);
						newFocusRow.setSelected(true);
					}

				} finally {
					row_mon.exit();
					refreshScrollbar();
					// TODO: Redraw only if visible or above visible (bg change)

					if (imgView != null && !imgView.isDisposed()
							&& !listCanvas.isDisposed()) {
						TableRowCore[] visibleRows = getVisibleRows();
						if (visibleRows.length > 0) {
							ListRow lastRow = (ListRow) visibleRows[visibleRows.length - 1];
							int endY = lastRow.getBasicYPos() + lastRow.getHeight();

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
					totalHeight = 0;
				} finally {
					row_mon.exit();
				}

				handleResize(true);
				listCanvas.redraw();
			}
		}, !bImmediate);
	}

	// XXX Copied from TableViewSWTImpl!
	private TableCellMouseEvent createMouseEvent(TableCellSWT cell, Event e,
			int type) {
		TableCellMouseEvent event = new TableCellMouseEvent();
		event.cell = cell;
		if (cell != null) {
			event.row = cell.getTableRow();
		}
		event.eventType = type;
		event.button = e.button;
		// TODO: Change to not use SWT masks
		event.keyboardState = e.stateMask;
		event.skipCoreFunctionality = false;

		if (cell != null) {
			Rectangle r = cell.getBounds();
			event.x = e.x - r.x;
			event.y = e.y - r.y;
			if (event.x < 0 || event.y < 0 || event.x >= r.width
					|| event.y >= r.height) {
				//			return null; // borks mouseenter/exit
			}
		}
		return event;
	}

	private class selectionListener
		implements Listener
	{
		Image imgMove;

		Point mouseDownAt;

		public void handleEvent(Event e) {
			ListRow row = (ListRow) getRow(e.x, e.y);
			if (row == null) {
				return;
			}
			int mouseEventType = -1;
			switch (e.type) {
				case SWT.MouseMove:
					if (DEMO_DRAGROW && (e.stateMask & SWT.BUTTON1) > 0
							&& imgMove != null && !imgMove.isDisposed()) {
						listCanvas.redraw();
						listCanvas.update();
						GC gc = new GC(listCanvas);
						gc.drawImage(imgMove, e.x - mouseDownAt.x, e.y - mouseDownAt.y);
						gc.dispose();
					}
					break;

				case SWT.MouseDown: {
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
									ListRow rowToSelect = (ListRow) getRow(i);
									if (rowToSelect != null) {
										rowToSelect.setSelected(true);
									}
								}

								ListRow rowToSelect = (ListRow) getRow(idxEnd);
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

					if (DEMO_DRAGROW) {
						Utils.disposeSWTObjects(new Object[] {
							imgMove
						});
						Rectangle rowArea = clientArea.intersection(clientArea);
						rowArea.y = row.getVisibleYOffset();
						rowArea.height = row.getHeight();
						mouseDownAt = new Point(e.x, e.y - rowArea.y);

						imgMove = new Image(display, rowArea.width, rowArea.height);
						GC gc = new GC(listCanvas);
						gc.copyArea(imgMove, rowArea.x, rowArea.y);
						gc.dispose();
					}

					mouseEventType = TableCellMouseEvent.EVENT_MOUSEDOWN;
					break;
				}

				case SWT.MouseUp: {
					if (DEMO_DRAGROW) {
						listCanvas.redraw();
						listCanvas.update();
					}
					mouseEventType = TableCellMouseEvent.EVENT_MOUSEUP;
					break;
				}

				case SWT.MouseDoubleClick: {
					mouseEventType = TableCellMouseEvent.EVENT_MOUSEDOUBLECLICK;
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

				TableCellMouseEvent event = createMouseEvent(cell, e, mouseEventType);
				if (event != null) {
					event.row = row;
					row.invokeMouseListeners(event);
					if (event.skipCoreFunctionality) {
						lCancelSelectionTriggeredOn = System.currentTimeMillis();
					}
				}
			}

			if (bMouseClickIsDefaultSelection && e.type == SWT.MouseUp) {
				_runDefaultAction();
			}

			if (e.type == SWT.MouseDoubleClick) {
				_runDefaultAction();
			}
		}
	}

	/**
	 * Get Row relative to top
	 * @param x
	 * @param y
	 * @return
	 */
	// @see com.aelitis.azureus.ui.common.table.TableView#getRow(int, int)
	public TableRowCore getRow(int x, int y) {
		if (y < 0 || x >= clientArea.x + clientArea.width || x < 0
				|| y >= clientArea.y + clientArea.height) {
			return null;
		}
		RowInfo rowInfo = getRowAbsolute(x, iLastVBarPos + y);
		if (rowInfo == null) {
			return null;
		}
		if (y > rowInfo.row.getBasicYPos() + rowInfo.row.getHeight()) {
			return null;
		}
		return rowInfo.row;
	}

	//public TableRowCore getRowAbsolute(int x, int y) {
	public RowInfo getRowAbsolute(int x, int y) {
		row_mon.enter();
		try {
			int numRows = rows.size();
			if (numRows == 0) {
				return null;
			}

			int index = Collections.binarySearch(rows, new Long(y), rowYPosComparator);

			if (index < 0) {
				index = -1 * index - 2; // best guess
				if (index < 0) {
					index = 0;
				}
			}

			if (index >= numRows) {
				index = numRows;
			}

			ListRow row = (ListRow) rows.get(index);
			if (row == null) {
				return null;
			}
			if (y > row.getBasicYPos() + row.getHeight()) {
				return null;
			}
			return new RowInfo(row, index);
		} finally {
			row_mon.exit();
		}
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableViewSWT#getTableCell(int, int)
	public TableCellSWT getTableCell(int x, int y) {
		ListRow row = (ListRow) getRow(x, y);
		if (row == null) {
			return null;
		}
		return row.getTableCellSWT(x, y);
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

	// @see com.aelitis.azureus.ui.common.table.TableView#getRow(int)
	public TableRowCore getRow(int position) {
		if (position < 0 || position >= rows.size()) {
			return null;
		}
		return (TableRowCore) rows.get(position);
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

		if (adjustingColumns) {
			if (DEBUG_COLUMNSIZE) {
				logCOLUMNSIZE("getVisColumns: exit early via "
						+ Debug.getCompressedStackTrace());
			}
			return lastVisibleColumns;
		}

		adjustingColumns = true;

		try {
			final int iClientWidth = clientArea.width;

			if (iClientWidth <= 0) {
				return new TableColumnCore[0];
			}

			if (lastClientWidth == iClientWidth) {
				return lastVisibleColumns;
			}

			if (DEBUG_COLUMNSIZE) {
				logCOLUMNSIZE("getVisColumns: size=" + listCanvas.getSize() + ","
						+ listCanvas.getBounds() + ";" + clientArea + ";last="
						+ lastClientWidth + "; via " + Debug.getCompressedStackTrace());
			}

			lastClientWidth = iClientWidth;

			TableColumnManager tcManager = TableColumnManager.getInstance();
			List autoHideOrder = tcManager.getAutoHideOrder(sTableID);

			// calculate totals
			int totalWidthVis = 0;
			int totalMinWidth = 0;
			int totalMinWidthVis = 0;
			int totalPrefWidthVis = 0;
			ArrayList visibleColumnsList = new ArrayList(allColumns.length);
			for (int i = 0; i < allColumns.length; i++) {
				TableColumnCore column = allColumns[i];
				if (!column.isVisible() && !autoHideOrder.contains(column)) {
					continue;
				}
				visibleColumnsList.add(allColumns[i]);

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
						logCOLUMNSIZE("+++ attempt add column " + column.getName() + ";mw="
								+ iMinWidth);
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
					if (DEBUG_COLUMNSIZE) {
						logCOLUMNSIZE("remaining " + remaining + ";adj=" + adj);
					}
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
						} else if (diff > 0) {
							if (adj > remaining) {
								column.setWidth(iWidth - remaining);
							} else {
								column.setWidth(iWidth - adj);
							}
							remaining -= (column.getWidth() - iWidth);
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

			logCOLUMNSIZE("refresh " + lastVisibleColumns.length + " columns");
			refreshVisible(true, true, false);

		} finally {
			adjustingColumns = false;
		}

		return lastVisibleColumns;
	}

	public void setColumnList(TableColumnCore[] columns,
			String defaultSortColumnID, boolean defaultSortAscending,
			boolean titleIsMinWidth) {
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

		Arrays.sort(allColumns, TableColumnManager.getTableColumnOrderComparator());

		ArrayList visibleColumnsList = new ArrayList();
		for (int i = 0; i < allColumns.length; i++) {
			if (allColumns[i].getPosition() >= 0 && allColumns[i].isVisible()) {
				//System.out.println("visColumn for " + sTableID + ":" + allColumns[i].getName());
				visibleColumnsList.add(allColumns[i]);
			}
		}
		lastVisibleColumns = (TableColumnCore[]) visibleColumnsList.toArray(new TableColumnCore[0]);
		// TODO: Refresh all rows

		// Initialize the sorter after the columns have been added
		// TODO: Restore sort column and direction from config (list in TVSWTImpl)
		String sSortColumn = defaultSortColumnID;
		boolean bSortAscending = defaultSortAscending;

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
			triggerSelectionListeners(rowsToSelectArray);

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

		int y = row.getBasicYPos();
		int height = row.getHeight();
		//System.out.println(sTableID + "] show row " + row + ";" + y
		//		+ ";totalheight=" + totalHeight + ";vbar=" + iLastVBarPos);
		if (y < iLastVBarPos) {
			scrollTo(y);
		} else if (y + height > iLastVBarPos + clientArea.height) {

			if (y <= iLastVBarPos + clientArea.height) {
				row = topRowInfo.row;
				int y2 = row.getBasicYPos();
				int i = topRowInfo.index;
				while (y + height > y2 + clientArea.height) {
					y2 += row.getHeight();
					row = (ListRow) getRow(++i);
				}
				scrollTo(y2);
			} else {
				// adjust bar so that new focused row is in the same spot as the old
				// one
				int ofs = 0;
				ListRow rowFocused = getRowFocused();
				if (rowFocused != null) {
					ofs = rowFocused.getBasicYPos() - iLastVBarPos;
					if (ofs > 0 && ofs < clientArea.height) {
						y -= ofs;
					}
				}
				scrollTo(y);
			}

		}
		//System.out.println(sTableID + "] show done " + row + ";" + y
		//		+ ";totalheight=" + totalHeight + ";vbar=" + iLastVBarPos);

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
		return new Rectangle(clientArea.x, -iLastVBarPos, clientArea.width,
				clientArea.height);
	}

	public Rectangle getClientArea() {
		return clientArea;
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
							ListRow nextRow = (ListRow) getRow(index);
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
							ListRow nextRow = (ListRow) getRow(index);
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
							ListRow previousRow = (ListRow) getRow(index);
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
							ListRow nextRow = (ListRow) getRow(index);
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
		} else if (event.type == SWT.DefaultSelection) {
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
		if (tableColumn == null) {
			return;
		}

		final String sColumnName = tableColumn.getName();

		runForAllRows(new TableGroupRowRunner() {
			public void run(TableRowCore row) {
				TableCellSWT cell = ((TableRowSWT) row).getTableCellSWT(sColumnName);
				if (cell != null) {
					cell.invalidate(bMustRefresh);
				}
			}
		});
		if (tableColumn.equals(getSortColumn())) {
			sortTable(true);
		}
	}

	// @see com.aelitis.azureus.ui.common.table.TableStructureModificationListener#columnOrderChanged(int[])
	public void columnOrderChanged(int[] iPositions) {
		Arrays.sort(allColumns, TableColumnManager.getTableColumnOrderComparator());

		logCOLUMNSIZE("Clear lastClientWidth via columnOrdereChanged");
		lastClientWidth = 0;
		getVisibleColumns();
	}

	// @see com.aelitis.azureus.ui.common.table.TableStructureModificationListener#columnSizeChanged(com.aelitis.azureus.ui.common.table.TableColumnCore)
	public void columnSizeChanged(TableColumnCore tableColumn) {
		if (adjustingColumns) {
			return;
		}
		if (isDisposed()) {
			return;
		}
		if (!tableColumn.isVisible()) {
			return;
		}
		logCOLUMNSIZE("Clear lastClientWidth via columnSizeChanged "
				+ tableColumn.getName());
		lastClientWidth = 0;
		getVisibleColumns();
	}

	public void tableStructureChanged() {
		// force an eventual recalc of visible row widths
		triggerLifeCycleListener(TableLifeCycleListener.EVENT_DESTROYED);

		removeAllTableRows();

		Arrays.sort(allColumns, TableColumnManager.getTableColumnOrderComparator());

		logCOLUMNSIZE("Clear lastClientWidth via tableStructureChanged");
		lastClientWidth = 0;
		getVisibleColumns();

		triggerLifeCycleListener(TableLifeCycleListener.EVENT_INITIALIZED);
	}

	public TableColumnCore getSortColumn() {
		return sortColumn;
	}

	public void setSortColumn(TableColumnCore sorter) {
		boolean bSameColumn = sortColumn.equals(sorter);
		if (!bSameColumn) {
			sortColumn = sorter;
			int iSortDirection = configMan.getIntParameter(CFG_SORTDIRECTION);
			if (iSortDirection == 0) {
				sortColumn.setSortAscending(true);
			} else if (iSortDirection == 1) {
				sortColumn.setSortAscending(false);
			} else {
				sortColumn.setSortAscending(!sortColumn.isSortAscending());
			}

			TableColumnManager.getInstance().setDefaultSortColumnName(sTableID,
					sortColumn.getName());
		} else {
			sortColumn.setSortAscending(!sortColumn.isSortAscending());
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
							cell.refresh(true);
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
					if (lTimeDiff > 100) {
						System.out.println("--- Build & Sort took " + lTimeDiff + "ms");
					}
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
					fixUpPositions(iFirstChange, false);

					topRowInfo = findTopRow(iLastVBarPos);
					bottomRowInfo = findBottomRow(iLastVBarPos, clientArea.height);

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

	/**
	 * @param onlyHeight 
	 * @param firstChange
	 *
	 * @since 3.0.4.3
	 */
	private void fixUpPositions(int startAtIndex, boolean onlyHeight) {
		row_mon.enter();
		try {
			if (rows.size() < startAtIndex || startAtIndex < 0) {
				return;
			}

			int y;
			if (startAtIndex > 0) {
				ListRow prevRow = ((ListRow) rows.get(startAtIndex - 1));
				y = prevRow.getBasicYPos() + prevRow.getHeight();
			} else {
				y = 0;
			}
			for (int i = startAtIndex; i < rows.size(); i++) {
				ListRow row = (ListRow) rows.get(i);
				//log("change " + i + " fron " + row.getBasicYPos() + " to " + y);
				row.setBasicYPos(y);
				y += row.getHeight();
				if (!onlyHeight) {
					row.fixupPosition();
				}
			}
		} finally {
			row_mon.exit();
		}
	}

	public TableColumnCore[] getAllColumns() {
		return allColumns;
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#isRowVisible(com.aelitis.azureus.ui.common.table.TableRowCore)
	public boolean isRowVisible(final TableRowCore row) {
		if (!viewVisible) {
			return false;
		}

		// Calculating based on position and size is faster than indexOf(row)
		// on large lists
		if (row instanceof ListRow) {
			ListRow listRow = (ListRow) row;
			int yPos = listRow.getBasicYPos();
			if (yPos < 0 || yPos >= iLastVBarPos + clientArea.height) {
				return false;
			}
			int height = listRow.getHeight();
			//System.out.println(sTableID + "] " + i + ";y=" + yPos + ";h=" + height + ";l=" + iLastVBarPos + ";ch=" + clientArea.height );
			return yPos + height > iLastVBarPos;
		}

		if (topRowInfo == null || bottomRowInfo == null) {
			return false;
		}

		int i = indexOf(row);

		int iTopIndex = topRowInfo.index;
		if (i < iTopIndex) {
			return false;
		} else if (i == iTopIndex) {
			return true;
		}

		return i <= bottomRowInfo.index;
	}

	public TableRowSWT[] getVisibleRows() {
		if (listCanvas == null || listCanvas.isDisposed() || topRowInfo == null
				|| bottomRowInfo == null || !viewVisible) {
			return new TableRowSWT[0];
		}

		int iTopIndex = topRowInfo.index;
		int iBottomIndex = bottomRowInfo.index;

		int size = iBottomIndex - iTopIndex + 1;
		if (size <= 0) {
			return new TableRowSWT[0];
		}

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
		if (!isRowVisible(cell.getRow())) {
			return true;
		}
		final Boolean[] b = new Boolean[1];

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				b[0] = new Boolean(_cellRefresh(cell, bDoGraphics, bForceRedraw));
			}
		}, false);

		return b[0] == null ? false : b[0].booleanValue();
	}

	public boolean _cellRefresh(final ListCell cell, final boolean bDoGraphics,
			final boolean bForceRedraw) {
		// assume cell if being refreshed if there's already a GC
		if (gcImgView != null || imgView == null || !cell.isShown()) {
			return true;
		}

		try {
			gcImgView = new GC(imgView);

			cell.doPaint(gcImgView);

			Rectangle rect = cell.getBounds();
			if (rect != null) {
				logPAINT("redraw via cellRefresh " + rect + ";"
						+ Debug.getCompressedStackTrace());
				listCanvas.redraw(rect.x, rect.y, rect.width, rect.height, false);
			}
		} catch (Exception e) {
			if (cell instanceof TableCellCore) {
				Debug.out(((TableCellCore) cell).getTableColumn().getName(), e);
			} else {
				Debug.out(e);
			}
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
		List list = (List) Utils.execSWTThreadWithObject("rowRefresh",
				new AERunnableObject() {
					public Object runSupport() {
						return _rowRefresh(row, bDoGraphics, bForceRedraw);
					}
				});

		return list == null ? new ArrayList() : list;
	}

	public void rowRefreshAsync(final ListRow row, final boolean bDoGraphics,
			final boolean bForceRedraw) {
		if (DEBUGPAINT) {
			logPAINT("rowRefreshA " + row + " force? " + bForceRedraw + " via "
					+ Debug.getCompressedStackTrace());
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
			display.asyncExec(new AERunnable() {
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
		if (listCanvas == null || listCanvas.isDisposed() || !isRowVisible(row)) {
			return Collections.EMPTY_LIST;
		}

		// TuxDebug
		//if (bForceRedraw)
		//System.out.println(sTableID + "] row refresh @" + row.getBasicYPos());

		boolean changed = false;
		List changedItems = null;
		Rectangle rect = new Rectangle(clientArea.x, rowGetVisibleYOffset(row),
				clientArea.width, row.getHeight());

		if (imgView != null) {
			boolean isOurGC = gcImgView == null || gcImgView.isDisposed();
			/*
			 * 1) Refresh the row
			 * 2) Paint any columns (or full row) if they visually changed
			 */
			try {
				if (isOurGC) {
					gcImgView = new GC(imgView);
				}
				gcImgView.setClipping(rect);

				changedItems = row._refresh(bDoGraphics, true);
				boolean thisChanged = changedItems.size() > 0;
				changed |= thisChanged;

				//bForceRedraw = true;
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
			logPAINT("redraw via rowRefresh " + rect);
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

		return changedItems == null ? new ArrayList() : changedItems;
	}

	private class canvasPaintListener
		implements Listener
	{
		long makeSureWeDraw = -1;

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
				e.gc.fillRectangle(e.x, e.y, e.width, e.height);
				return;
			}

			if (vBar != null && !vBar.isDisposed() && vBar.isVisible()
					&& iLastVBarPos != vBar.getSelection()) {
				if (makeSureWeDraw < 0) {
					makeSureWeDraw = SystemTime.getCurrentTime();
				} else if (SystemTime.getCurrentTime() < makeSureWeDraw + 3000) {
					return;
				}
			}

			if (e.width > 0) {
				if (DEBUGPAINT) {
					logPAINT("paint " + e.getBounds() + " image area: "
							+ imgView.getBounds() + "; pending=" + e.count);
				}
				makeSureWeDraw = -1;

				e.gc.drawImage(imgView, e.x, e.y, e.width, e.height, e.x, e.y, e.width,
						e.height);
			}
		}
	}

	protected int rowGetVisibleYOffset(TableRowCore row) {
		ListRow listRow = (ListRow) row;
		int basicYPos = listRow.getBasicYPos();

		return basicYPos - iLastVBarPos;
	}

	/**
	 * @param listRow
	 *
	 * @since 3.0.4.3
	 */
	public void rowHeightChanged(ListRow rowChanged, int oldHeight, int newHeight) {
		totalHeight = totalHeight - oldHeight + newHeight;

		if (rowChanged.getBasicYPos() < 0) {
			// not added yet, no need to fixup or refresh anything
			return;
		}

		int startPos = indexOf(rowChanged);
		fixUpPositions(startPos, true);

		if (bottomRowInfo == null || startPos <= bottomRowInfo.index) {
			bottomRowInfo = findBottomRow(iLastVBarPos, clientArea.height);
		}
		refreshScrollbar();
		refreshVisible(true, true, true);
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

				if (Constants.isOSX) {
					return;
				}

				// Must dispose in an asyncExec, otherwise SWT.Selection doesn't
				// get fired (async workaround provided by Eclipse Bug #87678)
				display.asyncExec(new AERunnable() {
					public void runSupport() {
						if (bShown || menu.isDisposed()) {
							return;
						}
						MenuItem[] items = menu.getItems();
						for (int i = 0; i < items.length; i++) {
							items[i].dispose();
						}
					}
				});
			}

			public void menuShown(MenuEvent e) {
				MenuItem[] items = menu.getItems();
				for (int i = 0; i < items.length; i++) {
					items[i].dispose();
				}

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
		if (listenersKey.contains(listener)) {
			return;
		}

		listenersKey.add(listener);
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#removeKeyListener(org.eclipse.swt.events.KeyListener)
	public void removeKeyListener(KeyListener listener) {
		listenersKey.remove(listener);
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

	// @see com.aelitis.azureus.ui.common.table.TableStructureModificationListener#cellInvalidate(com.aelitis.azureus.ui.common.table.TableColumnCore, java.lang.Object)
	public void cellInvalidate(TableColumnCore tableColumn, Object data_source) {
		cellInvalidate(tableColumn, data_source, true);
	}

	public void cellInvalidate(TableColumnCore tableColumn,
			final Object data_source, final boolean bMustRefresh) {
		final String sColumnName = tableColumn.getName();

		runForAllRows(new TableGroupRowRunner() {
			public void run(TableRowCore row) {
				TableCellSWT cell = ((TableRowSWT) row).getTableCellSWT(sColumnName);
				if (cell != null && cell.getDataSource() != null
						&& cell.getDataSource().equals(data_source)) {
					cell.invalidate(bMustRefresh);
				}
			}
		});
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#delete()
	public void delete() {
		viewVisible = false;
		triggerLifeCycleListener(TableLifeCycleListener.EVENT_DESTROYED);

		UIUpdaterFactory.getInstance().removeUpdater(this);
		TableStructureEventDispatcher.getInstance(sTableID).removeListener(this);

		Utils.disposeSWTObjects(new Object[] {
			headerArea,
			listCanvas
		});
		TableColumnManager.getInstance().saveTableColumns(sTableID);
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
	public void refreshTable(final boolean forceSort) {
		if (listCanvas.isDisposed()) {
			return;
		}
		//log("updateUI via " + Debug.getCompressedStackTrace());
		processDataSourceQueue();

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (!sortTable(forceSort)) {
					iGraphicRefresh++;
					boolean bDoGraphics = (iGraphicRefresh % graphicsUpdate) == 0;
					refreshVisible(bDoGraphics, false, true);
				}
			}
		});
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
		rowHeightDefault = height + (rowMarginHeight * 2);
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#getRowDefaultHeight()
	public int getRowDefaultHeight() {
		return rowHeightDefault;
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#setRowDefaultIconSize(org.eclipse.swt.graphics.Point)
	public void setRowDefaultIconSize(Point size) {
		rowHeightDefault = size.y + (rowMarginHeight * 2);
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
		if (mapColumnMetrics.size() == 0) {
			// hack, because getVisibleColumns regenerates mapColumnMetrics
			getVisibleColumns();
		}
		TableColumnMetrics metrics = (TableColumnMetrics) mapColumnMetrics.get(column);
		return metrics;
	}

	// @see org.eclipse.swt.events.KeyListener#keyPressed(org.eclipse.swt.events.KeyEvent)
	public void keyPressed(KeyEvent event) {
		Object[] listeners = listenersKey.toArray();
		for (int i = 0; i < listeners.length; i++) {
			KeyListener l = (KeyListener) listeners[i];
			l.keyPressed(event);
			if (!event.doit) {
				return;
			}
		}

		int key = event.character;
		if (key <= 26 && key > 0) {
			key += 'a' - 1;
		}

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

				case SWT.F5: {
					System.out.println(sTableID + "] ^F5");
					fixUpPositions(0, true);
					sortTable(true);
					//refreshVisible(true, true, true);
					break;
				}
			}

		} else {
			switch (event.keyCode) {
				case SWT.PAGE_UP:
					moveFocus(getClientArea().height / -rowHeightDefault, false);
					break;

				case SWT.PAGE_DOWN:
					moveFocus(getClientArea().height / rowHeightDefault, false);
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
					System.out.println(sTableID + "] F5");
					TableRowCore[] rows = getSelectedRows();
					for (int i = 0; i < rows.length; i++) {
						rows[i].invalidate();
						rows[i].refresh(true);
					}
					break;
				}
			}
		}
	}

	// @see org.eclipse.swt.events.KeyListener#keyReleased(org.eclipse.swt.events.KeyEvent)
	public void keyReleased(KeyEvent event) {
		Object[] listeners = listenersKey.toArray();
		for (int i = 0; i < listeners.length; i++) {
			KeyListener l = (KeyListener) listeners[i];
			l.keyReleased(event);
			if (!event.doit) {
				return;
			}
		}
	}

	public TableCellCore getTableCellWithCursor() {
		Point pt = display.getCursorLocation();
		pt = listCanvas.toControl(pt);
		return getTableCell(pt.x, pt.y);
	}

	public Point getTableCellMouseOffset() {
		Point pt = display.getCursorLocation();
		pt = listCanvas.toControl(pt);
		TableCellSWT tableCell = getTableCell(pt.x, pt.y);
		if (tableCell == null) {
			return null;
		}
		Rectangle bounds = tableCell.getBounds();
		return new Point(pt.x - bounds.x, pt.y - bounds.y);
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableViewSWT#getTableRowWithCursor()
	public TableRowCore getTableRowWithCursor() {
		Point pt = display.getCursorLocation();
		pt = listCanvas.toControl(pt);
		return (TableRowSWT) getRow(pt.x, pt.y);
	}

	/**
	 * @param column
	 * @return
	 *
	 * @since 3.0.4.3
	 */
	public boolean isColumnShown(TableColumn column) {
		for (int i = 0; i < lastVisibleColumns.length; i++) {
			TableColumn tc = lastVisibleColumns[i];
			if (tc == column) {
				return true;
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.views.table.TableViewSWT#isColumnVisible(com.aelitis.azureus.ui.common.table.TableColumnCore)
	 */
	public boolean isColumnVisible(TableColumn column) {
		return true;
	}

	public int getRowMarginHeight() {
		return rowMarginHeight;
	}

	public void setRowMarginHeight(int h) {
		rowMarginHeight = h;
	}
}
