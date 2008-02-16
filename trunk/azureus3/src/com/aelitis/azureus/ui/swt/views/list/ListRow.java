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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Composite;

import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.core3.tracker.host.TRHostTorrent;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableRowSWT;
import org.gudy.azureus2.ui.swt.views.table.impl.TableCellImpl;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.ui.common.table.TableCellCore;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.TableView;
import com.aelitis.azureus.ui.swt.skin.SWTSkinProperties;

import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.ui.UIRuntimeException;
import org.gudy.azureus2.plugins.ui.tables.*;

import org.gudy.azureus2.pluginsimpl.local.disk.DiskManagerFileInfoImpl;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadManagerImpl;
import org.gudy.azureus2.pluginsimpl.local.peers.PeerManagerImpl;
import org.gudy.azureus2.pluginsimpl.local.tracker.TrackerTorrentImpl;

/**
 * @author TuxPaper
 * @created Jun 12, 2006
 *
 */
public class ListRow
	implements TableRowSWT
{
	private static final boolean READJUST_CELL_HEIGHT = true;

	private SWTSkinProperties skinProperties;

	private Object coreDataSource;

	private Object pluginDataSource;

	private Map mapTableCells;

	private boolean bDisposed;

	private boolean bSelected;

	private ListView view;

	private final Composite parent;

	private Color fg;

	private Color bg;

	private Color customBG;

	private boolean bRowVisuallyChangedSinceRefresh;

	private ArrayList mouseListeners;

	private AEMonitor this_mon = new AEMonitor("ListRow");

	private Map dataList;

	private int height = 0;

	private int basicYPos = -1;

	/**
	 * @param position 
	 * 
	 */
	public ListRow(final ListView view, Composite parent, Object datasource) {
		this.parent = parent;
		coreDataSource = datasource;
		this.view = view;
		setHeight(view.rowHeightDefault);

		pluginDataSource = null;
		bDisposed = false;
		bSelected = false;
		mapTableCells = new LinkedHashMap();

		skinProperties = view.getSkinProperties();

		TableColumnCore[] columns = view.getAllColumns();
		// XXX Need invisible "sort by" column
		int iStartPos = ListView.COLUMN_MARGIN_WIDTH;
		// this is -1 :(
		//int height = rowComposite.getSize().y;
		for (int i = 0; i < columns.length; i++) {
			TableColumnCore column = columns[i];

			boolean bVisible = column.isVisible();
			int margin = view.getRowMarginHeight();
			Rectangle bounds = new Rectangle(iStartPos, margin, bVisible
					? column.getWidth() : 0, height - (margin * 2));

			ListCell listCell;
			int iColumnPos = column.getPosition();
			int iSWTAlign = CoreTableColumn.getSWTAlign(column.getAlignment());
			if (column.getType() == TableColumn.TYPE_GRAPHIC) {
				listCell = new ListCellGraphic(this, iSWTAlign, bounds);
			} else {
				listCell = new ListCell(this, iSWTAlign, bounds);
			}

			if (bVisible) {
				iStartPos += bounds.width + (ListView.COLUMN_MARGIN_WIDTH * 2);
			}

			TableCellSWT cell = new TableCellImpl(this, column, i, listCell);
			listCell.setTableCell(cell);
			cell.setUpToDate(false);

			mapTableCells.put(column.getName(), cell);
			//cell.refresh();
		}
	}

	private void checkCellForSetting() {
		if (bDisposed) {
			throw new UIRuntimeException("ListRow is disposed.");
		}
	}

	public void setBackgroundColor(Color bg) {
		this.customBG = bg;
	}

	private void setBackgroundColor(int iPosition) {
		checkCellForSetting();
		
		boolean changed = false;
		Color newBG;
		Color newFG;

		boolean bOdd = ((iPosition + 1) % 2) == 0;
		if (bSelected) {
			newBG = bOdd ? view.colorRowSelectedOddBG : view.colorRowSelectedEvenBG;
			newFG = bOdd ? view.colorRowSelectedOddFG : view.colorRowSelectedEvenFG;
		} else {
			newBG = bOdd ? view.colorRowOddBG : view.colorRowEvenBG;
			newFG = bOdd ? view.colorRowOddFG : view.colorRowEvenFG;
		}

		if (!colorsEqual(newBG, bg)) {
			changed = true;
			bg = newBG;
		}
		if (!colorsEqual(newFG, fg)) {
			changed = true;
			fg = newFG;
		}

		if (changed) {
			bRowVisuallyChangedSinceRefresh = true;

			invalidateGraphic();
			if (isVisible()) {
				redraw();
			}
		}
		// 1160681379555: r54c4r.v?N;Invalidate Cell;true from ListRow::setBackgroundColor::316,ListRow::setIndex::468,ListView::notifyIndexChanges::385,ListView$3::run::344,Utils::execSWTThread::590,Utils::execSWTThread::618,ListView::addDataSources::313,ListView::processDataSourceQueue::242,ListView::updateUI::660,UIUpdaterImpl::update::139,UIUpdaterImpl::access$0::126,UIUpdaterImpl$1::runSupport::72,AERunnable::run::38,RunnableLock::run::35,Synchronizer::runAsyncMessages::123,Display::runAsyncMessages::3325,Display::readAndDispatch::2971,SWTThread::<init>::130,SWTThread::createInstance::64,Initializer::<init>::169,Initializer::main::147
	}

	private boolean colorsEqual(Color color1, Color color2) {
		if (color1 == color2) {
			return true;
		}
		if (color1 == null || color2 == null) {
			return false;
		}
		return color1.equals(color2);
	}

	private Color getAlternatingColor() {
		boolean bOdd = ((getIndex() + 1) % 2) == 0;
		if (skinProperties != null) {
			String sColorID = (bOdd) ? "color.row.odd.bg" : "color.row.even.bg";
			Color color = skinProperties.getColor(sColorID);
			if (color != null) {
				return color;
			}
		}
		return parent.getBackground();
	}

	// @see com.aelitis.azureus.ui.common.table.TableRowCore#delete()
	public void delete() {
		bDisposed = true;

		Iterator iter = mapTableCells.values().iterator();
		while (iter.hasNext()) {
			TableCellSWT item = (TableCellSWT) iter.next();
			try {
				item.dispose();
			} catch (Exception e) {
				Debug.out("Disposing ListRow Column", e);
			}
		}
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableRowSWT#doPaint(org.eclipse.swt.graphics.GC, boolean, boolean)
	public void doPaint(GC gc, boolean bVisible) {
		doPaint(gc, bVisible, false);
	}

	public void doPaint(GC gc, boolean bVisible, boolean bListCellPaint) {
		// XXX Copied from TableRowImpl
		if (bDisposed || !bVisible) {
			return;
		}

		TableColumnCore[] visibleColumns = view.getVisibleColumns();

		long lTimeStart = System.currentTimeMillis();
		Rectangle oldClipping = gc.getClipping();
		try {
			gc.setBackground(getBackground());
			//gc.setBackground(Display.getDefault().getSystemColor((int)(Math.random() * 16)));

			//System.out.println(getIndex() + ";" + view.rowGetVisibleYOffset(this) + "--" + height);
			Rectangle clientArea = view.getClientArea();
			int yofs = view.rowGetVisibleYOffset(this);
			gc.fillRectangle(0, yofs, clientArea.width, height);
			if (isFocused()) {
				if (view.colorRowFocus != null) {
					gc.setForeground(view.colorRowFocus);
					gc.setLineStyle(view.rowFocusStyle);
					gc.drawRectangle(0, yofs, clientArea.width - 1, height - 1);
				}
			} else {
  			int yy = height + yofs - 1;
  			if (yy > 0) {
  				if (view.colorRowDivider != null) {
  					gc.setForeground(view.colorRowDivider);
  					
    				if (isFocused()) {
    					//gc.setBackground(ColorCache.getColor(gc.getDevice(), "#191919"));
    					//gc.setForeground(ColorCache.getColor(gc.getDevice(), "#606060"));
    				}
    				gc.drawLine(3, yy, clientArea.width - 4, yy);
  				}
  			}
			}

			gc.setForeground(getForeground());
			
			for (int i = 0; i < visibleColumns.length; i++) {
				long lTimeStart2 = System.currentTimeMillis();

				TableColumnCore column = visibleColumns[i];
				TableCellSWT cell = (TableCellSWT) mapTableCells.get(column.getName());
				if (cell == null) {
					continue;
				}

				try {
					if (cell.needsPainting()) {
						Rectangle bounds = cell.getBounds();
						if (bounds != null) {
							Rectangle clipping = bounds.intersection(oldClipping);
							gc.setClipping(clipping);

							if (bListCellPaint) {
								ListCell listcell = (ListCell) cell.getBufferedTableItem();
								listcell.doPaint(gc);
							} else {
								cell.doPaint(gc);
							}
						}
					}
				} catch (Exception e) {
					//Debug.out(e);
					System.err.println("column " + column.getName() + ";" + cell);
				}

				long diff = System.currentTimeMillis() - lTimeStart2;
				if (diff > 120) {
					view.log("doPaint " + column.getName() + " took " + diff + "ms. "
							+ this.toString());
				}
			}
		} finally {
			gc.setClipping(oldClipping);
		}
		long diff = System.currentTimeMillis() - lTimeStart;
		if (diff > 90) {
			view.log("doPaint took " + diff + "ms. " + this.toString());
		}
	}

	// XXX Copied from TableRowImpl!!
	public Object getDataSource(boolean bCoreObject) {
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

	public Color getForeground() {
		if (fg == null) {
			setBackgroundColor(getIndex());
		}
		return fg;
	}

	public int getIndex() {
		return view.indexOf(this);
	}

	public TableCellCore getTableCellCore(String field) {
		if (bDisposed) {
			return null;
		}

		return (TableCellCore) mapTableCells.get(field);
	}

	// @see com.aelitis.azureus.ui.common.table.TableRowCore#invalidate()
	public void invalidate() {
		if (bDisposed) {
			return;
		}

		long lTimeStart = System.currentTimeMillis();
		Iterator iter = mapTableCells.values().iterator();
		while (iter.hasNext()) {
			TableCellSWT cell = (TableCellSWT) iter.next();
			if (cell != null) {
				cell.invalidate(true);
			}
		}
		long diff = System.currentTimeMillis() - lTimeStart;
		if (diff >= 10) {
			System.out.println("invalidate: " + diff + "ms");
		}
	}

	public void invalidateGraphic() {
		if (bDisposed) {
			return;
		}

		long lTimeStart = System.currentTimeMillis();
		Iterator iter = mapTableCells.values().iterator();
		while (iter.hasNext()) {
			TableCellSWT cell = (TableCellSWT) iter.next();
			if (cell.getTableColumn().getType() == TableColumn.TYPE_GRAPHIC) {
				cell.invalidate(true);
			}
		}
		long diff = System.currentTimeMillis() - lTimeStart;
		if (diff >= 10) {
			System.out.println("invalidate: " + diff + "ms");
		}
	}

	public boolean isRowDisposed() {
		return bDisposed;
	}

	public boolean isSelected() {
		return bSelected;
	}

	public boolean isVisible() {
		return view.isRowVisible(this);
	}

	public void locationChanged(int iStartColumn) {
		// TODO Auto-generated method stub

	}

	// XXX Copied from TableRowImp!
	public List refresh(boolean bDoGraphics) {
		if (bDisposed) {
			return Collections.EMPTY_LIST;
		}

		boolean bVisible = isVisible();

		return refresh(bDoGraphics, bVisible);
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableRowCore#refresh(boolean, boolean)
	public List refresh(boolean bDoGraphics, boolean bVisible) {
		if (bDisposed) {
			return Collections.EMPTY_LIST;
		}
		return view.rowRefresh(this, bDoGraphics, bVisible);
	}

	protected List _refresh(boolean bDoGraphics, boolean bVisible) {
		if (bDisposed) {
			return Collections.EMPTY_LIST;
		}

		// If this were called from a plugin, we'd have to refresh the sorted column
		// even if we weren't visible

		if (!bVisible) {
			setUpToDate(false);
			return Collections.EMPTY_LIST;
		}

		ArrayList list = new ArrayList();

		Iterator iter = mapTableCells.values().iterator();
		while (iter.hasNext()) {
			TableCellSWT cell = (TableCellSWT) iter.next();
			Rectangle bounds = cell.getBounds();
			TableColumn column = cell.getTableColumn();
			if (bounds != null && column.isVisible()) {
				boolean thisChanged = cell.refresh(bDoGraphics, bVisible);
				if (thisChanged) {
					list.add(cell);
				}
			}
		}
		if (bRowVisuallyChangedSinceRefresh) {
			list.add(0, this);
		}
		bRowVisuallyChangedSinceRefresh = false;
		return list;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableRowSWT#setForeground(org.eclipse.swt.graphics.Color)
	public void setForeground(Color c) {
		Iterator iter = mapTableCells.values().iterator();
		while (iter.hasNext()) {
			TableCellSWT item = (TableCellSWT) iter.next();
			item.setForeground(c);
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableRow#setForeground(int, int, int)
	public void setForeground(int r, int g, int b) {
		Iterator iter = mapTableCells.values().iterator();
		while (iter.hasNext()) {
			TableCellSWT item = (TableCellSWT) iter.next();
			item.setForeground(r, g, b);
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableRow#setForeground(int[])
	public void setForeground(int[] rgb) {
		if (rgb == null || rgb.length < 3) {
			setForeground((Color) null);
			return;
		}
		setForeground(rgb[0], rgb[1], rgb[2]);
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableRow#setForegroundToErrorColor()
	public void setForegroundToErrorColor() {
		this.setForeground(Colors.colorError);
	}

	public boolean setDrawableHeight(int iHeight) {
		return setHeight(iHeight + view.getRowMarginHeight() * 2);
	}
	
	public boolean setHeight(int iHeight) {
		int newHeight = iHeight;
		if (height == newHeight) {
			return false;
		}
		bRowVisuallyChangedSinceRefresh = true;

		int oldHeight = height;
		height = newHeight;
		if (READJUST_CELL_HEIGHT && mapTableCells != null) {
			Iterator iter = mapTableCells.values().iterator();
			while (iter.hasNext()) {
				TableCellSWT cell = (TableCellSWT) iter.next();
				if (cell != null) {
					// hack.. a call to ListCell.isShown will trigger Visibility Listener
					ListCell listcell = (ListCell) cell.getBufferedTableItem();
					Rectangle bounds = listcell.getBounds();
					bounds.height = height;
					listcell.setBounds(bounds);
				}
			}
		}

		view.rowHeightChanged(this, oldHeight, height);
		return true;
	}

	public int getHeight() {
		return height;
	}
	
	public int getDrawableHeight() {
		return height - (view.getRowMarginHeight() * 2); 
	}

	public boolean setIconSize(Point pt) {
		// TODO Auto-generated method stub
		bRowVisuallyChangedSinceRefresh = true;
		return false;
	}

	public void setSelected(boolean bSelected) {
		if (this.bSelected == bSelected) {
			return;
		}

		this.bSelected = bSelected;
		setBackgroundColor(getIndex());
		view.rowSetSelected(this, bSelected);
	}

	/**
	 * @param b
	 */
	public void setFocused(boolean b) {
		ListRow oldRow = view.getRowFocused();
		if (b) {
			view.rowSetFocused(this);
		} else if (isFocused()) {
			view.rowSetFocused(null);
		}
		if (oldRow != null) {
			oldRow.redraw();
		}
		redraw();
	}

	public boolean isFocused() {
		return this.equals(view.getRowFocused());
	}

	/**
	 * Fixes up layout, and if something changed, it makes sure items near it 
	 * are fixed up
	 */
	public boolean setTableItem(int iRowPos) {
		if (!fixupPosition()) {
			return false;
		}

		ListRow row = (ListRow) view.getRow(iRowPos + 1);
		if (row != null) {
			row.fixupPosition();
		}

		bRowVisuallyChangedSinceRefresh = true;
		return true;
	}

	/**
	 * Fixes up row's layout.  Does not check if rows next to it are correct,
	 * even if some layout information changed
	 * 
	 * @return
	 */
	public boolean fixupPosition() {
		int iRowPos = getIndex();

		setBackgroundColor(iRowPos);

		if (parent == null || parent.isDisposed()) {
			return false;
		}

		return true;
	}

	public void setUpToDate(boolean upToDate) {
		if (bDisposed) {
			return;
		}

		long lTimeStart = System.currentTimeMillis();
		Iterator iter = mapTableCells.values().iterator();
		while (iter.hasNext()) {
			TableCellSWT cell = (TableCellSWT) iter.next();
			if (cell != null) {
				boolean bOldUpToDate = cell.isUpToDate();
				if (bOldUpToDate != upToDate) {
					cell.setUpToDate(upToDate);

					// hack.. a call to ListCell.isShown will trigger Visibility Listener
					ListCell listcell = (ListCell) cell.getBufferedTableItem();
					listcell.isShown();
				}
			}
		}
		long diff = System.currentTimeMillis() - lTimeStart;
		if (diff >= 50) {
			System.out.println("sutd: " + upToDate + " for " + getIndex() + "; "
					+ diff + "ms");
		}
	}

	public Object getDataSource() {
		return getDataSource(false);
	}

	public TableCell getTableCell(String sColumnName) {
		if (bDisposed) {
			return null;
		}

		return (TableCell) mapTableCells.get(sColumnName);
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableRowSWT#getTableCellSWT(java.lang.String)
	public TableCellSWT getTableCellSWT(String sColumnName) {
		if (bDisposed) {
			return null;
		}

		return (TableCellSWT) mapTableCells.get(sColumnName);
	}

	public String getTableID() {
		return view.getTableID();
	}

	public boolean isValid() {
		if (bDisposed) {
			return true;
		}

		boolean valid = true;
		Iterator iter = mapTableCells.values().iterator();
		while (iter.hasNext()) {
			TableCellSWT cell = (TableCellSWT) iter.next();
			if (cell != null) {
				valid &= cell.isValid();
			}
		}
		return valid;
	}

	public boolean getVisuallyChangedSinceLastRefresh() {
		if (bDisposed) {
			return true;
		}

		if (bRowVisuallyChangedSinceRefresh) {
			return true;
		}

		Iterator iter = mapTableCells.values().iterator();
		while (iter.hasNext()) {
			TableCellSWT cell = (TableCellSWT) iter.next();
			if (cell != null) {
				if (cell.getVisuallyChangedSinceRefresh()) {
					return true;
				}
			}
		}
		return false;
	}

	public Color getBackground() {
		if (customBG != null && !customBG.isDisposed()) {
			return customBG;
		}

		if (bg == null) {
			setBackgroundColor(getIndex());
		}
		return bg;
	}

	/**
	 * @param x
	 * @param y
	 * @return
	 */
	public TableCellSWT getTableCellSWT(int x, int y) {
		Iterator iter = mapTableCells.values().iterator();
		while (iter.hasNext()) {
			TableCellSWT cell = (TableCellSWT) iter.next();
			if (cell.isShown()) {
				Rectangle bounds = cell.getBounds();
				if (bounds != null && bounds.contains(x, bounds.y)) {
					return cell;
				}
			}
		}
		return null;
	}

	public TableView getView() {
		return view;
	}

	public void redraw() {
		view.rowRefreshAsync(this, true, true);
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableRowCore#setAlternatingBGColor(boolean)
	public void setAlternatingBGColor(boolean bEvenIfNotVisible) {
		setBackgroundColor(getIndex());
	}

	public void doPaint(GC gc) {
		if (bDisposed) {
			return;
		}
		doPaint(gc, isVisible());
	}

	public String toString() {
		return "ListRow {" + getIndex() + (bDisposed ? ", Disposed" : "") + ","
				+ view.getTableID() + "," + (isVisible() ? "visible" : "invisible")
				+ "}";
	}

	public int getVisibleYOffset() {
		return view.rowGetVisibleYOffset(this);
	}

	public void addMouseListener(TableRowMouseListener listener) {
		try {
			this_mon.enter();

			if (mouseListeners == null)
				mouseListeners = new ArrayList(1);

			mouseListeners.add(listener);

		} finally {
			this_mon.exit();
		}
	}

	public void removeMouseListener(TableRowMouseListener listener) {
		try {
			this_mon.enter();

			if (mouseListeners == null)
				return;

			mouseListeners.remove(listener);

		} finally {
			this_mon.exit();
		}
	}

	public void invokeMouseListeners(TableRowMouseEvent event) {
		ArrayList listeners = mouseListeners;
		if (listeners == null)
			return;

		for (int i = 0; i < listeners.size(); i++) {
			try {
				TableRowMouseListener l = (TableRowMouseListener) (listeners.get(i));

				l.rowMouseTrigger(event);

			} catch (Throwable e) {
				Debug.printStackTrace(e);
			}
		}
	}

	// @see com.aelitis.azureus.ui.common.table.TableRowCore#isMouseOver()
	public boolean isMouseOver() {
		return view.getTableRowWithCursor() == this;
	}

	public void setBasicYPos(int y) {
		basicYPos = y;
	}

	public int getBasicYPos() {
		return basicYPos;
	}

	public void setData(String id, Object data) {
		if (dataList == null) {
			dataList = new HashMap(1);
		}
		if (data == null) {
			dataList.remove("id");
		} else {
			dataList.put(id, data);
		}
	}

	public Object getData(String id) {
		return dataList == null ? null : dataList.get(id);
	}
}
