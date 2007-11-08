/**
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.ui.swt.views.skin;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTGraphic;

import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author TuxPaper
 * @created Aug 29, 2007
 *
 */
public class FakeTableCell
	implements TableCell
{
	private AEMonitor this_mon = new AEMonitor("FakeTableCell");

	private ArrayList refreshListeners;

	private ArrayList disposeListeners;

	private ArrayList tooltipListeners;

	private ArrayList cellMouseListeners;

	private ArrayList cellMouseMoveListeners;

	private ArrayList cellVisibilityListeners;

	private Image image;

	private Rectangle imageBounds;

	private int marginHeight;

	private int orientation;

	private int marginWidth;

	private Comparable sortValue;

	private Object ds;

	private Control composite;

	private final TableColumn tableColumn;

	/**
	 * @param columnRateUpDown
	 */
	public FakeTableCell(TableColumn column) {
		this.tableColumn = column;
		setOrientationViaColumn();
	}

	public void addRefreshListener(TableCellRefreshListener listener) {
		try {
			this_mon.enter();

			if (refreshListeners == null)
				refreshListeners = new ArrayList(1);

			refreshListeners.add(listener);

		} finally {
			this_mon.exit();
		}
	}

	public void removeRefreshListener(TableCellRefreshListener listener) {
		try {
			this_mon.enter();

			if (refreshListeners == null)
				return;

			refreshListeners.remove(listener);
		} finally {

			this_mon.exit();
		}
	}

	public void addDisposeListener(TableCellDisposeListener listener) {
		try {
			this_mon.enter();

			if (disposeListeners == null) {
				disposeListeners = new ArrayList(1);
			}
			disposeListeners.add(listener);
		} finally {

			this_mon.exit();
		}
	}

	public void removeDisposeListener(TableCellDisposeListener listener) {
		try {
			this_mon.enter();

			if (disposeListeners == null)
				return;

			disposeListeners.remove(listener);

		} finally {

			this_mon.exit();
		}
	}

	public void addToolTipListener(TableCellToolTipListener listener) {
		try {
			this_mon.enter();

			if (tooltipListeners == null) {
				tooltipListeners = new ArrayList(1);
			}
			tooltipListeners.add(listener);

		} finally {
			this_mon.exit();
		}
	}

	public void removeToolTipListener(TableCellToolTipListener listener) {
		try {
			this_mon.enter();

			if (tooltipListeners == null)
				return;

			tooltipListeners.remove(listener);
		} finally {

			this_mon.exit();
		}
	}

	public void addMouseListener(TableCellMouseListener listener) {
		try {
			this_mon.enter();

			if (cellMouseListeners == null)
				cellMouseListeners = new ArrayList(1);

			cellMouseListeners.add(listener);

		} finally {
			this_mon.exit();
		}
	}

	public void removeMouseListener(TableCellMouseListener listener) {
		try {
			this_mon.enter();

			if (cellMouseListeners == null)
				return;

			cellMouseListeners.remove(listener);

		} finally {
			this_mon.exit();
		}
	}

	public void addMouseMoveListener(TableCellMouseMoveListener listener) {
		try {
			this_mon.enter();

			if (cellMouseMoveListeners == null)
				cellMouseMoveListeners = new ArrayList(1);

			cellMouseMoveListeners.add(listener);

		} finally {
			this_mon.exit();
		}
	}

	public void removeMouseMoveListener(TableCellMouseMoveListener listener) {
		try {
			this_mon.enter();

			if (cellMouseMoveListeners == null)
				return;

			cellMouseMoveListeners.remove(listener);

		} finally {
			this_mon.exit();
		}
	}

	public void addVisibilityListener(TableCellVisibilityListener listener) {
		try {
			this_mon.enter();

			if (cellVisibilityListeners == null)
				cellVisibilityListeners = new ArrayList(1);

			cellVisibilityListeners.add(listener);

		} finally {
			this_mon.exit();
		}
	}

	public void removeVisibilityListener(TableCellVisibilityListener listener) {
		try {
			this_mon.enter();

			if (cellVisibilityListeners == null)
				return;

			cellVisibilityListeners.remove(listener);

		} finally {
			this_mon.exit();
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#addListeners(java.lang.Object)
	public void addListeners(Object listenerObject) {
		if (listenerObject instanceof TableCellDisposeListener)
			addDisposeListener((TableCellDisposeListener) listenerObject);

		if (listenerObject instanceof TableCellRefreshListener)
			addRefreshListener((TableCellRefreshListener) listenerObject);

		if (listenerObject instanceof TableCellToolTipListener)
			addToolTipListener((TableCellToolTipListener) listenerObject);

		if (listenerObject instanceof TableCellMouseMoveListener) {
			addMouseMoveListener((TableCellMouseMoveListener) listenerObject);
		}

		if (listenerObject instanceof TableCellMouseListener) {
			addMouseListener((TableCellMouseListener) listenerObject);
		}

		if (listenerObject instanceof TableCellVisibilityListener)
			addVisibilityListener((TableCellVisibilityListener) listenerObject);
	}

	public void invokeMouseListeners(TableCellMouseEvent event) {
		ArrayList listeners = event.eventType == TableCellMouseEvent.EVENT_MOUSEMOVE
				? cellMouseMoveListeners : cellMouseListeners;
		if (listeners == null)
			return;

		if (event.cell != null && event.row == null) {
			event.row = event.cell.getTableRow();
		}

		for (int i = 0; i < listeners.size(); i++) {
			try {
				TableCellMouseListener l = (TableCellMouseListener) (listeners.get(i));

				l.cellMouseTrigger(event);

			} catch (Throwable e) {
				Debug.printStackTrace(e);
			}
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#getDataSource()
	public Object getDataSource() {
		return ds;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#getForeground()
	public int[] getForeground() {
		// TODO Auto-generated method stub
		return null;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#getGraphic()
	public Graphic getGraphic() {
		// TODO Auto-generated method stub
		return null;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#getHeight()
	public int getHeight() {
		if (composite != null) {
			return composite.getSize().y;
		}
		return 0;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#getMaxLines()
	public int getMaxLines() {
		// TODO Auto-generated method stub
		return 0;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#getSortValue()
	public Comparable getSortValue() {
		return sortValue;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#getTableColumn()
	public TableColumn getTableColumn() {
		// TODO Auto-generated method stub
		return null;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#getTableID()
	public String getTableID() {
		// TODO Auto-generated method stub
		return null;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#getTableRow()
	public TableRow getTableRow() {
		// TODO Auto-generated method stub
		return null;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#getText()
	public String getText() {
		// TODO Auto-generated method stub
		return null;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#getToolTip()
	public Object getToolTip() {
		// TODO Auto-generated method stub
		return null;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#getWidth()
	public int getWidth() {
		if (composite != null) {
			return composite.getSize().x;
		}
		return 0;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#invalidate()
	public void invalidate() {
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#isDisposed()
	public boolean isDisposed() {
		// TODO Auto-generated method stub
		return false;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#isShown()
	public boolean isShown() {
		return true;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#isValid()
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#setFillCell(boolean)
	public void setFillCell(boolean fillCell) {
		// TODO Auto-generated method stub

	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#setForeground(int, int, int)
	public boolean setForeground(int red, int green, int blue) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean setForegroundToErrorColor() {
		// TODO Auto-generated method stub
		return false;
	}
	
	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#setGraphic(org.gudy.azureus2.plugins.ui.Graphic)
	public boolean setGraphic(Graphic img) {
		Image imgSWT = null;
		if (img instanceof UISWTGraphic) {
			imgSWT = ((UISWTGraphic) img).getImage();
		}

		if (imgSWT != null && imgSWT.isDisposed()) {
			return false;
		}

		if (image == imgSWT) {
			return false;
		}

		//System.out.println("setGraphic " + image);

		image = imgSWT;
		if (image != null) {
			imageBounds = image.getBounds();
		}

		if (composite != null) {
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					if (composite != null && !composite.isDisposed()) {
						composite.redraw();
					}
				}
			});
		}

		return true;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#setMarginHeight(int)
	public void setMarginHeight(int height) {
		// TODO Auto-generated method stub

	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#setMarginWidth(int)
	public void setMarginWidth(int width) {
		// TODO Auto-generated method stub

	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#setSortValue(java.lang.Comparable)
	public boolean setSortValue(Comparable valueToSort) {
		// TODO Auto-generated method stub
		return false;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#setSortValue(float)
	public boolean setSortValue(float valueToSort) {
		// TODO Auto-generated method stub
		return false;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#setText(java.lang.String)
	public boolean setText(String text) {
		// TODO Auto-generated method stub
		return false;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#setToolTip(java.lang.Object)
	public void setToolTip(Object tooltip) {
		// TODO Auto-generated method stub

	}

	private boolean _setSortValue(Comparable valueToSort) {
		if (sortValue == valueToSort)
			return false;

		if ((valueToSort instanceof String) && (sortValue instanceof String)
				&& sortValue.equals(valueToSort)) {
			return false;
		}

		if ((valueToSort instanceof Number) && (sortValue instanceof Number)
				&& sortValue.equals(valueToSort)) {
			return false;
		}

		sortValue = valueToSort;

		return true;
	}

	public boolean setSortValue(long valueToSort) {
		if ((sortValue instanceof Long)
				&& ((Long) sortValue).longValue() == valueToSort)
			return false;

		return _setSortValue(new Long(valueToSort));
	}

	public void doPaint(GC gc, Rectangle bounds) {
		// TODO: Cleanup and stop calling me so often!

		//gc.setBackground(getBackground());
		//if (DEBUG_COLORCELL) {
		//	gc.setBackground(Display.getDefault().getSystemColor(
		//			(int) (Math.random() * 16)));
		//}
		if (bounds == null) {
			return;
		}
		//gc.fillRectangle(bounds);

		if (image != null && !image.isDisposed()) {
			Point size = new Point(bounds.width, bounds.height);

			int x;

			int y = marginHeight;
			y += (size.y - imageBounds.height) / 2;

			if (orientation == SWT.CENTER) {
				x = marginWidth;
				x += (size.x - (marginWidth * 2) - imageBounds.width) / 2;
			} else if (orientation == SWT.RIGHT) {
				x = bounds.height - marginWidth - imageBounds.width;
			} else {
				x = marginWidth;
			}

			int width = Math.min(bounds.width - x - marginWidth, imageBounds.width);
			int height = Math.min(bounds.height - y - marginHeight,
					imageBounds.height);

			if (width >= 0 && height >= 0) {
				gc.drawImage(image, 0, 0, width, height, bounds.x + x, bounds.y + y,
						width, height);
			}
		}
	}

	public void refresh() {
		//System.out.println("refresh");
		if (refreshListeners != null) {
			for (int i = 0; i < refreshListeners.size(); i++) {
				((TableCellRefreshListener) (refreshListeners.get(i))).refresh(this);
			}
		}
	}

	public void setDataSource(Object datasource) {
		ds = datasource;
	}

	public void setControl(final Composite composite) {
		this.composite = composite;

		composite.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				doPaint(e.gc, composite.getClientArea());
			}
		});

		composite.addMouseListener(new MouseListener() {

			public void mouseUp(MouseEvent e) {
				invokeMouseListeners(buildMouseEvent(e,
						TableCellMouseEvent.EVENT_MOUSEUP));
			}

			public void mouseDown(MouseEvent e) {
				invokeMouseListeners(buildMouseEvent(e,
						TableCellMouseEvent.EVENT_MOUSEDOWN));
			}

			public void mouseDoubleClick(MouseEvent e) {
			}

		});

		composite.addMouseMoveListener(new MouseMoveListener() {
			public void mouseMove(MouseEvent e) {
				invokeMouseListeners(buildMouseEvent(e,
						TableCellMouseEvent.EVENT_MOUSEMOVE));
			}
		});
		
		composite.addMouseTrackListener(new MouseTrackListener() {
		
			public void mouseHover(MouseEvent e) {
				// TODO Auto-generated method stub
		
			}
		
			public void mouseExit(MouseEvent e) {
				invokeMouseListeners(buildMouseEvent(e,
						TableCellMouseEvent.EVENT_MOUSEEXIT));
			}
		
			public void mouseEnter(MouseEvent e) {
				invokeMouseListeners(buildMouseEvent(e,
						TableCellMouseEvent.EVENT_MOUSEENTER));
			}
		
		});
	}

	/**
	 * @param e
	 * @return
	 *
	 * @since 3.0.2.1
	 */
	protected TableCellMouseEvent buildMouseEvent(MouseEvent e, int eventType) {
		TableCellMouseEvent event = new TableCellMouseEvent();
		event.cell = this;
		event.button = e.button;
		event.keyboardState = e.stateMask;
		event.eventType = eventType;

		Rectangle r = composite.getBounds();
//		int align = tableColumn.getAlignment();
//		if (align == TableColumn.ALIGN_CENTER) {
//			r.x = marginWidth;
//			r.x += (r.width - (marginWidth * 2) - imageBounds.width) / 2;
//		}

		event.x = e.x - r.x;
		event.y = e.y - r.y;

		return event;
	}

	private void setOrientationViaColumn() {
		int align = tableColumn.getAlignment();
		if (align == TableColumn.ALIGN_CENTER)
			orientation = SWT.CENTER;
		else if (align == TableColumn.ALIGN_LEAD)
			orientation = SWT.LEFT;
		else if (align == TableColumn.ALIGN_TRAIL)
			orientation = SWT.RIGHT;
	}
}
