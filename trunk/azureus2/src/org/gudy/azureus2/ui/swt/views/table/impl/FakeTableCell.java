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

package org.gudy.azureus2.ui.swt.views.table.impl;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.core3.tracker.host.TRHostTorrent;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.BufferedTableItem;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.plugins.UISWTGraphic;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTGraphicImpl;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;
import org.gudy.azureus2.ui.swt.views.table.TableRowSWT;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnSWTUtils;

import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.swt.utils.ColorCache;

import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;

import org.gudy.azureus2.pluginsimpl.local.disk.DiskManagerFileInfoImpl;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadManagerImpl;
import org.gudy.azureus2.pluginsimpl.local.peers.PeerManagerImpl;
import org.gudy.azureus2.pluginsimpl.local.tracker.TrackerTorrentImpl;

/**
 * @author TuxPaper
 * @created Aug 29, 2007
 *
 */
public class FakeTableCell
	implements TableCellSWT, PaintListener, MouseListener, MouseMoveListener,
	MouseTrackListener
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

	private Object coreDataSource;

	private Composite composite;

	private final TableColumnCore tableColumn;

	private Graphic graphic;

	private String text;

	private Object pluginDataSource;

	private Object tooltip;

	private Rectangle cellArea;

	private boolean hadMore;

	private ArrayList cellSWTPaintListeners;
	
	private boolean valid;

	private TableRow fakeRow = null;

	/**
	 * @param columnRateUpDown
	 */
	public FakeTableCell(TableColumn column) {
		valid = false;
		this.tableColumn = (TableColumnCore) column;
		setOrientationViaColumn();
	}

	public FakeTableCell(TableColumnCore column) {
		valid = false;
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
	
	/**
	 * @param listenerObject
	 *
	 * @since 4.0.0.1
	 */
	private void addSWTPaintListener(TableCellSWTPaintListener listener) {
		try {
			this_mon.enter();

			if (cellSWTPaintListeners == null)
				cellSWTPaintListeners = new ArrayList(1);

			cellSWTPaintListeners.add(listener);

		} finally {
			this_mon.exit();
		}
	}

	public void invokeSWTPaintListeners(GC gc) {
		if (getBounds().isEmpty()) {
			return;
		}
  	if (tableColumn != null) {
			Object[] swtPaintListeners = tableColumn.getCellOtherListeners("SWTPaint");
			if (swtPaintListeners != null) { 
  			for (int i = 0; i < swtPaintListeners.length; i++) {
  				try {
  					TableCellSWTPaintListener l = (TableCellSWTPaintListener) swtPaintListeners[i];
  
  					l.cellPaint(gc, this);
  
  				} catch (Throwable e) {
  					Debug.printStackTrace(e);
  				}
  			}
			}
		}

		if (cellSWTPaintListeners == null) {
			return;
		}
		

		for (int i = 0; i < cellSWTPaintListeners.size(); i++) {
			try {
				TableCellSWTPaintListener l = (TableCellSWTPaintListener) (cellSWTPaintListeners.get(i));

				l.cellPaint(gc, this);

			} catch (Throwable e) {
				Debug.printStackTrace(e);
			}
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

		if (listenerObject instanceof TableCellSWTPaintListener) {
			addSWTPaintListener((TableCellSWTPaintListener) listenerObject);
		}
	}

	public void invokeMouseListeners(TableCellMouseEvent event) {
		if (event.cell != null && event.row == null) {
			event.row = event.cell.getTableRow();
		}

		try {
			tableColumn.invokeCellMouseListeners(event);
		} catch (Throwable e) {
			Debug.printStackTrace(e);
		}

		ArrayList listeners = event.eventType == TableCellMouseEvent.EVENT_MOUSEMOVE
				? cellMouseMoveListeners : cellMouseListeners;

		if (listeners == null) {
			return;
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
		boolean bCoreObject = ((TableColumnCore) tableColumn).getUseCoreDataSource();
		if (bCoreObject) {
			return coreDataSource;
		}

		if (pluginDataSource != null) {
			return pluginDataSource;
		}

		if (coreDataSource instanceof DownloadManager) {
			DownloadManager dm = (DownloadManager) coreDataSource;
			if (dm != null) {
				try {
					pluginDataSource = DownloadManagerImpl.getDownloadStatic(dm);
				} catch (DownloadException e) { /* Ignore */
				}
			}
		}
		if (coreDataSource instanceof PEPeer) {
			PEPeer peer = (PEPeer) coreDataSource;
			if (peer != null) {
				pluginDataSource = PeerManagerImpl.getPeerForPEPeer(peer);
			}
		}

		if (coreDataSource instanceof PEPiece) {
			// XXX There is no Piece object for plugins yet
			PEPiece piece = (PEPiece) coreDataSource;
			if (piece != null) {
				pluginDataSource = null;
			}
		}

		if (coreDataSource instanceof DiskManagerFileInfo) {
			DiskManagerFileInfo fileInfo = (DiskManagerFileInfo) coreDataSource;
			if (fileInfo != null) {
				try {
					pluginDataSource = new DiskManagerFileInfoImpl(
							DownloadManagerImpl.getDownloadStatic(fileInfo.getDownloadManager()),
							fileInfo);
				} catch (DownloadException e) { /* Ignore */
				}
			}
		}

		if (coreDataSource instanceof TRHostTorrent) {
			TRHostTorrent item = (TRHostTorrent) coreDataSource;
			if (item != null) {
				pluginDataSource = new TrackerTorrentImpl(item);
			}
		}

		return pluginDataSource;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#getForeground()
	public int[] getForeground() {
		if (composite == null || composite.isDisposed()) {
			return null;
		}
		Color fg = composite.getForeground();
		return new int[] {
			fg.getRed(),
			fg.getGreen(),
			fg.getBlue()
		};
	}

	public int[] getBackground() {
		// until we can make sure composite.getBackground is being used
		// (background image might superceed), return 0
		if (true) {
			return new int[] {
				0,
				0,
				0
			};
		}
		if (composite == null || composite.isDisposed()) {
			return null;
		}
		Color bg = composite.getBackground();
		return new int[] {
			bg.getRed(),
			bg.getGreen(),
			bg.getBlue()
		};
	}

	public Graphic getBackgroundGraphic() {
		// TODO handle cellArea

		if (composite == null || composite.isDisposed()) {
			return null;
		}

		try {
			Rectangle bounds = composite.getBounds();

			if (bounds.isEmpty()) {
				return null;
			}

			Image imgCap = new Image(composite.getDisplay(), bounds.width,
					bounds.height);

			// will walk up tree until it gets an image
			Control bgControl = Utils.findBackgroundImageControl(composite);
			Image imgBG = composite.getBackgroundImage();

			GC gc = new GC(imgCap);
			try {
				if (imgBG == null) { // || imgBG has alpha..
					gc.setBackground(composite.getBackground());
					gc.fillRectangle(0, 0, bounds.width, bounds.height);
				}

				if (imgBG != null) {
					Point controlPos = new Point(0, 0);
					if (bgControl instanceof Composite) {
						Rectangle compArea = ((Composite) bgControl).getClientArea();
						controlPos.x = compArea.x;
						controlPos.y = compArea.y;
					}
					Point absControlLoc = bgControl.toDisplay(controlPos.x, controlPos.y);

					Rectangle shellClientArea = composite.getShell().getClientArea();
					Point absShellLoc = composite.getParent().toDisplay(
							shellClientArea.x, shellClientArea.y);

					Point ofs = new Point(absControlLoc.x - absShellLoc.x,
							absControlLoc.y - absShellLoc.y);
					Rectangle imgBGBounds = imgBG.getBounds();
					ofs.x = (ofs.x % imgBGBounds.width);
					ofs.y = (ofs.y % imgBGBounds.height);

					gc.drawImage(imgBG, ofs.x, ofs.y);
				}
			} finally {
				gc.dispose();
			}

			return new UISWTGraphicImpl(imgCap);
		} catch (Exception e) {
			Debug.out(e);
		}
		return null;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#getGraphic()
	public Graphic getGraphic() {
		return graphic;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#getHeight()
	public int getHeight() {
		if (composite != null && !composite.isDisposed()) {
			return composite.getSize().y;
		}
		return 0;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#getMaxLines()
	public int getMaxLines() {
		return -1;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#getSortValue()
	public Comparable getSortValue() {
		return sortValue;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#getTableColumn()
	public TableColumn getTableColumn() {
		return tableColumn;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#getTableID()
	public String getTableID() {
		return tableColumn == null ? null : tableColumn.getTableID();
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#getTableRow()
	public TableRow getTableRow() {
		if (fakeRow == null) {
			fakeRow = new TableRow() {
				public void setForegroundToErrorColor() {
				}

				public void setForeground(int[] rgb) {
				}

				public void setForeground(int red, int green, int blue) {
				}

				public void removeMouseListener(TableRowMouseListener listener) {
				}

				public boolean isValid() {
					return FakeTableCell.this.isValid();
				}

				public boolean isSelected() {
					return false;
				}

				public String getTableID() {
					return FakeTableCell.this.getTableID();
				}

				public TableCell getTableCell(String columnName) {
					return null;
				}

				public Object getDataSource() {
					return FakeTableCell.this.getDataSource();
				}

				public void addMouseListener(TableRowMouseListener listener) {
				}
			};
		}
		return fakeRow;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#getText()
	public String getText() {
		return text;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#getToolTip()
	public Object getToolTip() {
		if (tooltip == null && hadMore) {
			return text;
		}
		return tooltip;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#getWidth()
	public int getWidth() {
		if (!isDisposed()) {
			return composite.getSize().x;
		}
		return 0;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#invalidate()
	public void invalidate() {
		valid = false;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#isDisposed()
	public boolean isDisposed() {
		return composite == null || composite.isDisposed();
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#isShown()
	public boolean isShown() {
		return true;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#isValid()
	public boolean isValid() {
		return valid;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#setFillCell(boolean)
	public void setFillCell(boolean fillCell) {
		// TODO Auto-generated method stub

	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#setForeground(int, int, int)
	public boolean setForeground(int red, int green, int blue) {
		if (isDisposed()) {
			return false;
		}
		if (red < 0 || green < 0 || blue < 0) {
			composite.setForeground(null);
		} else {
			composite.setForeground(ColorCache.getColor(composite.getDisplay(), red,
					green, blue));
		}
		return true;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#setForeground(int[])
	public boolean setForeground(int[] rgb) {
		if (rgb == null || rgb.length < 3) {
			return setForeground(-1, -1, -1);
		}
		return setForeground(rgb[0], rgb[1], rgb[2]);
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#setForegroundToErrorColor()
	public boolean setForegroundToErrorColor() {
		if (isDisposed()) {
			return false;
		}
		composite.setForeground(Colors.colorError);
		return true;
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

		if (composite != null && !composite.isDisposed()) {
			redraw();
		}

		graphic = img;
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
		if (text != null && text.equals(this.text)) {
			return false;
		}
		this.text = text;
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (!isDisposed()) {
					composite.redraw();
				}
			}
		});
		return true;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#setToolTip(java.lang.Object)
	public void setToolTip(Object tooltip) {
		this.tooltip = tooltip;
		if (!isDisposed()) {
			composite.setToolTipText(tooltip == null ? null : tooltip.toString());
		}
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
		if (isDisposed()) {
			return;
		}
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
				x = bounds.width - marginWidth - imageBounds.width;
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

		if (text != null && text.length() > 0) {
			GCStringPrinter sp = new GCStringPrinter(gc, text, bounds, true, false,
					orientation | SWT.WRAP);
			sp.printString();
			hadMore = sp.isCutoff();
		}

		invokeSWTPaintListeners(gc);
	}

	public boolean refresh() {
		//System.out.println("refresh");
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				try {
					tableColumn.invokeCellRefreshListeners(FakeTableCell.this, false);
				} catch (Throwable e) {
				}
				if (refreshListeners != null) {
					for (int i = 0; i < refreshListeners.size(); i++) {
						((TableCellRefreshListener) (refreshListeners.get(i))).refresh(FakeTableCell.this);
					}
				}
				valid = true;
			}
		});
		return true;
	}

	public void setDataSource(Object datasource) {
		coreDataSource = datasource;
		if (datasource != null && !isDisposed()) {
			invokeVisibilityListeners(TableCellVisibilityListener.VISIBILITY_SHOWN,
					true);
		}
	}

	public void setControl(final Composite composite) {
		setControl(composite, null);
	}

	public void setControl(final Composite composite, Rectangle cellArea) {
		if (composite == null) {
			dispose();
			this.composite = null;
			return;
		}

		this.composite = composite;
		this.cellArea = cellArea;

		composite.addPaintListener(this);
		composite.addMouseListener(this);
		composite.addMouseMoveListener(this);
		composite.addMouseTrackListener(this);

		setForeground(-1, -1, -1);
		setText(null);
		setToolTip(null);

		composite.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				dispose();
			}
		});
		if (coreDataSource != null && !isDisposed()) {
			invokeVisibilityListeners(TableCellVisibilityListener.VISIBILITY_SHOWN,
					true);
		}
	}

	public void paintControl(PaintEvent e) {
		doPaint(e.gc, cellArea == null ? composite.getClientArea() : cellArea);
	}

	public void mouseUp(MouseEvent e) {
		invokeMouseListeners(buildMouseEvent(e, TableCellMouseEvent.EVENT_MOUSEUP));
	}

	public void mouseDown(MouseEvent e) {
		try{
			if ( 	composite == null || composite.getMenu() != null || 
					( cellMouseListeners != null && cellMouseListeners.size() > 0 ) ||
					text == null || text.length() == 0 ){
		
				return;
			}
			
			if (!(e.button == 3 || (e.button == 1 && e.stateMask == SWT.CONTROL))){
			
				return;
			}
		
			Menu menu = new Menu(composite.getShell(),SWT.POP_UP);
			
			MenuItem   item = new MenuItem( menu,SWT.NONE );
			
			item.setText( MessageText.getString( "ConfigView.copy.to.clipboard.tooltip"));
	
			item.addSelectionListener(
				new SelectionAdapter()
				{
					public void 
					widgetSelected(
						SelectionEvent arg0) 
					{
						if ( !composite.isDisposed() && text != null && text.length() > 0 ){
		    			
							new Clipboard(composite.getDisplay()).setContents(new Object[] {text}, new Transfer[] {TextTransfer.getInstance()});
						}
					}
				});
			
			composite.setMenu( menu );
			
			menu.addMenuListener(
				new MenuAdapter()
				{
					public void 
					menuHidden(
						MenuEvent arg0 )
					{
						if ( !composite.isDisposed()){
						
							composite.setMenu( null );
						}
					}
				});
			
			menu.setVisible( true );

		}finally{
		
			invokeMouseListeners(buildMouseEvent(e, TableCellMouseEvent.EVENT_MOUSEDOWN));
		}
	}

	public void mouseDoubleClick(MouseEvent e) {
		invokeMouseListeners(buildMouseEvent(e,
				TableCellMouseEvent.EVENT_MOUSEDOUBLECLICK));
	}

	public void mouseMove(MouseEvent e) {
		invokeMouseListeners(buildMouseEvent(e, TableCellMouseEvent.EVENT_MOUSEMOVE));
	}

	public void mouseHover(MouseEvent e) {
		invokeToolTipListeners(TOOLTIPLISTENER_HOVER);
	}

	public void mouseExit(MouseEvent e) {
		invokeMouseListeners(buildMouseEvent(e, TableCellMouseEvent.EVENT_MOUSEEXIT));
	}

	public void mouseEnter(MouseEvent e) {
		invokeMouseListeners(buildMouseEvent(e,
				TableCellMouseEvent.EVENT_MOUSEENTER));
	}

	/**
	 * @param e
	 * @return
	 *
	 * @since 3.0.2.1
	 */
	protected TableCellMouseEvent buildMouseEvent(MouseEvent e, int eventType) {
		if (isDisposed()) {
			return null;
		}
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

		if (cellArea != null) {
			r = new Rectangle(r.x + cellArea.x, r.y + cellArea.y, cellArea.width,
					cellArea.height);
		}

		event.x = e.x - r.x;
		event.y = e.y - r.y;

		return event;
	}

	private void setOrientationViaColumn() {
		orientation = TableColumnSWTUtils.convertColumnAlignmentToSWT(tableColumn.getAlignment());
	}

	// @see com.aelitis.azureus.ui.common.table.TableCellCore#dispose()
	public void dispose() {
		if (composite != null && !composite.isDisposed()) {
			composite.removePaintListener(this);
			composite.removeMouseListener(this);
			composite.removeMouseMoveListener(this);
			composite.removeMouseTrackListener(this);
		}

		if (disposeListeners != null) {
			for (Iterator iter = disposeListeners.iterator(); iter.hasNext();) {
				TableCellDisposeListener listener = (TableCellDisposeListener) iter.next();
				try {
					listener.dispose(this);
				} catch (Throwable e) {
					Debug.out(e);
				}
			}
			disposeListeners = null;
		}
		tableColumn.invokeCellDisposeListeners(this);
		tableColumn.invalidateCells();
	}

	// @see com.aelitis.azureus.ui.common.table.TableCellCore#getCursorID()
	public int getCursorID() {
		// TODO Auto-generated method stub
		return 0;
	}

	// @see com.aelitis.azureus.ui.common.table.TableCellCore#getObfusticatedText()
	public String getObfusticatedText() {
		return text;
	}

	// @see com.aelitis.azureus.ui.common.table.TableCellCore#getTableRowCore()
	public TableRowCore getTableRowCore() {
		return null;
	}

	// @see com.aelitis.azureus.ui.common.table.TableCellCore#getVisuallyChangedSinceRefresh()
	public boolean getVisuallyChangedSinceRefresh() {
		return true;
	}

	// @see com.aelitis.azureus.ui.common.table.TableCellCore#invalidate(boolean)
	public void invalidate(boolean mustRefresh) {
		valid = false;
	}

	// @see com.aelitis.azureus.ui.common.table.TableCellCore#invokeToolTipListeners(int)
	public void invokeToolTipListeners(int type) {
		if (tableColumn == null)
			return;

		tableColumn.invokeCellToolTipListeners(this, type);

		if (tooltipListeners == null)
			return;

		try {
			if (type == TOOLTIPLISTENER_HOVER) {
				for (int i = 0; i < tooltipListeners.size(); i++)
					((TableCellToolTipListener) (tooltipListeners.get(i))).cellHover(this);
			} else {
				for (int i = 0; i < tooltipListeners.size(); i++)
					((TableCellToolTipListener) (tooltipListeners.get(i))).cellHoverComplete(this);
			}
		} catch (Throwable e) {
			Debug.out(e);
		}
	}

	// @see com.aelitis.azureus.ui.common.table.TableCellCore#invokeVisibilityListeners(int, boolean)
	public void invokeVisibilityListeners(int visibility,
			boolean invokeColumnListeners) {
		if (invokeColumnListeners) {
			tableColumn.invokeCellVisibilityListeners(this, visibility);
		}

		if (cellVisibilityListeners == null)
			return;

		for (int i = 0; i < cellVisibilityListeners.size(); i++) {
			try {
				TableCellVisibilityListener l = (TableCellVisibilityListener) (cellVisibilityListeners.get(i));

				l.cellVisibilityChanged(this, visibility);

			} catch (Throwable e) {
				Debug.printStackTrace(e);
			}
		}
	}

	// @see com.aelitis.azureus.ui.common.table.TableCellCore#isMouseOver()
	public boolean isMouseOver() {
		if (isDisposed()) {
			return false;
		}
		Rectangle r = composite.getBounds();
		if (cellArea != null) {
			r = new Rectangle(r.x + cellArea.x, r.y + cellArea.y, cellArea.width,
					cellArea.height);
		}
		Point ptStart = composite.toDisplay(r.x, r.y);
		r.x = ptStart.x;
		r.y = ptStart.y;
		Point ptCursor = composite.getDisplay().getCursorLocation();
		return r.contains(ptCursor);
	}

	// @see com.aelitis.azureus.ui.common.table.TableCellCore#isUpToDate()
	public boolean isUpToDate() {
		return false;
	}

	// @see com.aelitis.azureus.ui.common.table.TableCellCore#locationChanged()
	public void locationChanged() {
		// TODO Auto-generated method stub

	}

	// @see com.aelitis.azureus.ui.common.table.TableCellCore#needsPainting()
	public boolean needsPainting() {
		return true;
	}

	// @see com.aelitis.azureus.ui.common.table.TableCellCore#refresh(boolean)
	public boolean refresh(boolean doGraphics) {
		return refresh();
	}

	// @see com.aelitis.azureus.ui.common.table.TableCellCore#refresh(boolean, boolean, boolean)
	public boolean refresh(boolean doGraphics, boolean rowVisible,
			boolean cellVisible) {
		return refresh();
	}

	// @see com.aelitis.azureus.ui.common.table.TableCellCore#refresh(boolean, boolean)
	public boolean refresh(boolean doGraphics, boolean rowVisible) {
		return refresh();
	}

	// @see com.aelitis.azureus.ui.common.table.TableCellCore#setCursorID(int)
	public void setCursorID(int cursorID) {
		// TODO Auto-generated method stub

	}

	// @see com.aelitis.azureus.ui.common.table.TableCellCore#setUpToDate(boolean)
	public void setUpToDate(boolean upToDate) {
		// TODO Auto-generated method stub

	}

	// @see java.lang.Comparable#compareTo(java.lang.Object)
	public int compareTo(Object arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	public void setOrentation(int o) {
		orientation = o;
	}

	public Rectangle getCellArea() {
		return cellArea;
	}

	public void setCellArea(Rectangle cellArea) {
		//System.out.println("SCA " + cellArea + ";" + Debug.getCompressedStackTrace());
		this.cellArea = cellArea;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#getMouseOffset()
	public int[] getMouseOffset() {
		if (isDisposed()) {
			return null;
		}
		Rectangle r = composite.getBounds();
		if (cellArea != null) {
			r = new Rectangle(r.x + cellArea.x, r.y + cellArea.y, cellArea.width,
					cellArea.height);
		}
		Point ptStart = composite.toDisplay(r.x, r.y);
		r.x = ptStart.x;
		r.y = ptStart.y;
		Point ptCursor = composite.getDisplay().getCursorLocation();
		if (!r.contains(ptCursor)) {
			return null;
		}
		return new int[] { ptCursor.x - r.x, ptCursor.y - r.y };
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#getMarginHeight()
	public int getMarginHeight() {
		return marginHeight;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#getMarginWidth()
	public int getMarginWidth() {
		return marginWidth;
	}

	// @see com.aelitis.azureus.ui.common.table.TableCellCore#refreshAsync()
	public void refreshAsync() {
		refresh();
	}

	// @see com.aelitis.azureus.ui.common.table.TableCellCore#redraw()
	public void redraw() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (!isDisposed()) {
					composite.redraw();
				}
			}
		});
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableCellSWT#doPaint(org.eclipse.swt.graphics.GC)
	public void doPaint(GC gc) {
		doPaint(gc, cellArea == null ? composite.getClientArea() : cellArea);
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableCellSWT#getBackgroundImage()
	public Image getBackgroundImage() {
		// TODO Auto-generated method stub
		return null;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableCellSWT#getBackgroundSWT()
	public Color getBackgroundSWT() {
		// TODO Auto-generated method stub
		return composite.getBackground();
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableCellSWT#getBounds()
	public Rectangle getBounds() {
		return cellArea == null ? composite.getClientArea() : new Rectangle(
				cellArea.x, cellArea.y, cellArea.width, cellArea.height);
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableCellSWT#getBufferedTableItem()
	public BufferedTableItem getBufferedTableItem() {
		// TODO Auto-generated method stub
		return null;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableCellSWT#getForegroundSWT()
	public Color getForegroundSWT() {
		return composite.getForeground();
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableCellSWT#getGraphicSWT()
	public Image getGraphicSWT() {
		// TODO Auto-generated method stub
		return null;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableCellSWT#getIcon()
	public Image getIcon() {
		// TODO Auto-generated method stub
		return null;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableCellSWT#getSize()
	public Point getSize() {
		Rectangle bounds = getBounds();
		if (bounds == null) {
			return null;
		}
		return new Point(bounds.width, bounds.height);
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableCellSWT#getTableRowSWT()
	public TableRowSWT getTableRowSWT() {
		// TODO Auto-generated method stub
		return null;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableCellSWT#getTextAlpha()
	public int getTextAlpha() {
		// TODO Auto-generated method stub
		return 0;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableCellSWT#setForeground(org.eclipse.swt.graphics.Color)
	public boolean setForeground(Color color) {
		// TODO Auto-generated method stub
		return false;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableCellSWT#setGraphic(org.eclipse.swt.graphics.Image)
	public boolean setGraphic(Image img) {
		graphic = null;

		image = img;
		if (image != null) {
			imageBounds = image.getBounds();
		}

		if (composite != null && !composite.isDisposed()) {
			redraw();
		}
		
		return true;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableCellSWT#setIcon(org.eclipse.swt.graphics.Image)
	public boolean setIcon(Image img) {
		// TODO Auto-generated method stub
		return false;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableCellSWT#setTextAlpha(int)
	public void setTextAlpha(int textOpacity) {
		// TODO Auto-generated method stub
		
	}
}
