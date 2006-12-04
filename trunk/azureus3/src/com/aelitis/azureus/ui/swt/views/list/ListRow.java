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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.core3.tracker.host.TRHostTorrent;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.pluginsimpl.local.disk.DiskManagerFileInfoImpl;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadManagerImpl;
import org.gudy.azureus2.pluginsimpl.local.peers.PeerManagerImpl;
import org.gudy.azureus2.pluginsimpl.local.tracker.TrackerTorrentImpl;
import org.gudy.azureus2.ui.swt.views.table.TableCellCore;
import org.gudy.azureus2.ui.swt.views.table.TableColumnCore;
import org.gudy.azureus2.ui.swt.views.table.TableRowCore;
import org.gudy.azureus2.ui.swt.views.table.impl.TableCellImpl;

import com.aelitis.azureus.ui.swt.skin.SWTSkinProperties;

import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.ui.UIRuntimeException;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;

/**
 * @author TuxPaper
 * @created Jun 12, 2006
 *
 */
public class ListRow implements TableRowCore
{
	public static int ROW_HEIGHT = 38;

	public static int MARGIN_HEIGHT = 2;

	public static int MARGIN_WIDTH = 3;

	Composite rowComposite;

	private SWTSkinProperties skinProperties;

	private Object coreDataSource;

	private Object pluginDataSource;

	private Map mapTableCells;

	private boolean bDisposed;

	private boolean bSelected;

	private ListView view;

	/**
	 * @param position 
	 * 
	 */
	public ListRow(final ListView view, Composite parent, int position,
			Object datasource) {
		coreDataSource = datasource;
		this.view = view;

		pluginDataSource = null;
		bDisposed = false;
		bSelected = false;
		mapTableCells = new HashMap();

		rowComposite = new Canvas(parent, SWT.NO_FOCUS | SWT.NO_BACKGROUND);
		//		rowComposite = new Composite(parent, SWT.NONE);

		rowComposite.setLayout(new FormLayout());

		rowComposite.setData("ListRow", this);

		skinProperties = view.getSkinProperties();

		FormData formData = new FormData();
		// TODO: Let initializer set height
		formData.height = ROW_HEIGHT;
		// TODO: Proper width
		formData.width = 10;

		ListRow rowAbove = view.getRow(position - 1);
		if (rowAbove != null) {
			formData.top = new FormAttachment(rowAbove.getComposite());
		}
		formData.left = new FormAttachment(0);
		formData.right = new FormAttachment(100);

		//		int p = position + 1;
		//		Composite rc = rowComposite;
		//		ListRow rowBelow = view.getRow(p);
		//		while (rowBelow != null) {
		//			Composite rcBelow = rowBelow.getComposite();
		//			FormData fd = (FormData)rcBelow.getLayoutData();
		//			fd.top = new FormAttachment(rc);
		//			rowBelow.getComposite().setLayoutData(fd);
		//
		//			p++;
		//			rc = rcBelow;
		//			rowBelow = view.getRow(p);
		//		}

		rowComposite.setLayoutData(formData);

		rowComposite.setBackgroundMode(SWT.INHERIT_DEFAULT);

		rowComposite.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent event) {
				if (event.width == 0 || event.height == 0) {
					return;
				}

				Rectangle bounds = rowComposite.getClientArea();
				// fill whole image with normal bg
				event.gc.setBackground(getAlternatingColor());
				event.gc.fillRectangle(event.x, event.y, event.width, event.height);

				// fill round area with normal or selected color
				event.gc.setBackground(getBackground());
				//event.gc.fillRoundRectangle(bounds.x + 2, bounds.y + 2,
				//		bounds.width - 5, bounds.height - 5, 12, 12);
				event.gc.fillRectangle(bounds.x, bounds.y, bounds.width, bounds.height);

				if (view.isFocused() && isFocused()) {
					// draw round box around focused area
					event.gc.setForeground(event.widget.getDisplay().getSystemColor(
							SWT.COLOR_LIST_BACKGROUND));
					event.gc.drawRectangle(bounds.x, bounds.y, bounds.width - 1,
							bounds.height - 1);

					event.gc.setForeground(event.widget.getDisplay().getSystemColor(
							SWT.COLOR_LIST_FOREGROUND));
					event.gc.setLineWidth(1);
					event.gc.setLineStyle(SWT.LINE_DOT);
					event.gc.drawRectangle(bounds.x, bounds.y, bounds.width - 1,
							bounds.height - 1);
					//event.gc.drawRoundRectangle(bounds.x + 1, bounds.y + 1,
					//		bounds.width - 3, bounds.height - 3, 12, 12);
				}

				doPaint(event.gc);
			}
		});

		rowComposite.addListener(SWT.Resize, new Listener() {
			// @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
			public void handleEvent(Event event) {
				resizeRow(((Control) event.widget).getBounds());
			}
		});

		rowComposite.addListener(SWT.MouseUp, new Listener() {
			public void handleEvent(Event event) {
				if (event.button == 2 && event.stateMask == SWT.CONTROL) {
					Object[] objects = mapTableCells.values().toArray();
					for (int i = 0; i < objects.length; i++) {
						TableCellImpl cell = (TableCellImpl) objects[i];
						Rectangle bounds = cell.getBounds();
						if (bounds != null && bounds.contains(event.x, event.y)) {
							cell.bDebug = !cell.bDebug;
							System.out.println("set debug " + cell.bDebug + ";"
									+ cell.getTableColumn().getName());
						}
					}
				}
			}
		});

		TableColumnCore[] columns = view.getAllColumns();
		//		Control cLastCell = null;
		int iStartPos = MARGIN_WIDTH;
		// this is -1 :(
		//int height = rowComposite.getSize().y;
		for (int i = 0; i < columns.length; i++) {
			TableColumnCore column = columns[i];

			boolean bVisible = column.getPosition() >= 0;
			Rectangle bounds = bVisible ? new Rectangle(iStartPos, MARGIN_HEIGHT,
					column.getWidth(), ROW_HEIGHT - (MARGIN_HEIGHT * 2)) : null;

			ListCell listCell;
			int iColumnPos = column.getPosition();
			if (column.getType() == TableColumn.TYPE_GRAPHIC) {
				listCell = new ListCellGraphic(this, iColumnPos, column.getSWTAlign(),
						bounds);
			} else {
				listCell = new ListCell(this, iColumnPos, column.getSWTAlign(), bounds);
			}

			if (bVisible) {
				iStartPos += bounds.width + (MARGIN_WIDTH * 2);
			}

			//			Control cCell = listCell.getControl();
			//
			//			formData = new FormData();
			//			formData.width = column.getWidth();
			//			if (cLastCell != null) {
			//				formData.left = new FormAttachment(cLastCell, 5);
			//			}
			//			formData.top = new FormAttachment(0);
			//			formData.bottom = new FormAttachment(100);
			//
			//			cCell.setLayoutData(formData);

			TableCellCore cell = new TableCellImpl(this, column, i, listCell);

			mapTableCells.put(column.getName(), cell);
			cell.refresh();

			//			cLastCell = cCell;
		}

		setBackgroundColor(position);
	}

	/**
	 * @param bounds 
	 * 
	 */
	protected void resizeRow(Rectangle bounds) {
		if (bDisposed) {
			return;
		}

		Iterator iter = mapTableCells.values().iterator();
		while (iter.hasNext()) {
			TableCellCore item = (TableCellCore) iter.next();
			ListCell cell = (ListCell) item.getBufferedTableItem();
			Rectangle cellBounds = cell.getBounds();
			if (cellBounds != null && cell.getPosition() >= 0) {
				cellBounds.height = bounds.height - (MARGIN_HEIGHT * 2);
				cell.setBounds(cellBounds);
			}
		}
	}

	private void checkCellForSetting() {
		if (bDisposed) {
			throw new UIRuntimeException("ListRow is disposed.");
		}
	}

	private void setBackgroundColor(int iPosition) {
		checkCellForSetting();
		boolean bOdd = ((iPosition + 1) % 2) == 0;
		if (bSelected) {
			String sColorID = (bOdd) ? "color.row.odd.selected.bg"
					: "color.row.even.selected.bg";
			Color color = skinProperties.getColor(sColorID);
			if (color != null) {
				rowComposite.setBackground(color);
			} else {
				rowComposite.setBackground(rowComposite.getDisplay().getSystemColor(
						SWT.COLOR_LIST_SELECTION));
			}

			sColorID = (bOdd) ? "color.row.odd.selected.fg"
					: "color.row.even.selected.fg";
			Color cText = skinProperties.getColor(sColorID);
			if (cText == null) {
				sColorID = (bOdd) ? "color.row.odd.fg" : "color.row.even.fg";
				cText = skinProperties.getColor(sColorID);

				if (cText == null) {
					cText = rowComposite.getDisplay().getSystemColor(
							SWT.COLOR_LIST_SELECTION_TEXT);
				}
			}

			rowComposite.setForeground(cText);

			Iterator iter = mapTableCells.values().iterator();
			while (iter.hasNext()) {
				TableCellCore item = (TableCellCore) iter.next();
				// TODO: Only set not previously set 
				//item.setForeground(cText);
				item.invalidate(true);
				item.refresh(true);
			}
		} else {
			boolean bChanged = false;
			if (skinProperties != null) {
				String sColorID = (bOdd) ? "color.row.odd.bg" : "color.row.even.bg";
				Color color = skinProperties.getColor(sColorID);
				if (color != null && !colorsEqual(color, rowComposite.getBackground())) {
					bChanged = true;
					rowComposite.setBackground(color);
				}
			} else {
				Color oldColor = rowComposite.getBackground();
				rowComposite.setBackground(null);
				if (!colorsEqual(oldColor, rowComposite.getBackground())) {
					bChanged = true;
				}
			}

			String sColorID = (bOdd) ? "color.row.odd.fg" : "color.row.even.fg";
			Color cText = skinProperties.getColor(sColorID);

			if (cText == null) {
				cText = rowComposite.getDisplay().getSystemColor(
						SWT.COLOR_LIST_SELECTION_TEXT);
			}
			if (!colorsEqual(cText, rowComposite.getForeground())) {
				bChanged = true;
				rowComposite.setForeground(cText);
			}

			if (bChanged) {
				Iterator iter = mapTableCells.values().iterator();
				while (iter.hasNext()) {
					TableCellCore item = (TableCellCore) iter.next();
					// TODO: Only set if COLOR_LIST_SELECTION_TEXT 
					//item.setForeground(cText);
					item.invalidate(true);
					item.refresh(true);
				}
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
		return rowComposite.getParent().getBackground();
	}

	public Composite getComposite() {
		return rowComposite;
	}

	public void delete(boolean bDeleteSWTObject) {
		// XXX bDeleteSWTObject non-usage is intentional

		int iIndex = getIndex();
		bDisposed = true;

		Iterator iter = mapTableCells.values().iterator();
		while (iter.hasNext()) {
			TableCellCore item = (TableCellCore) iter.next();
			try {
				item.dispose();
			} catch (Exception e) {
				Debug.out("Disposing ListRow Column", e);
			}
		}

		if (rowComposite != null && !rowComposite.isDisposed()) {
			// Link next row to what is above this row
			ListRow rowNext = view.getRow(iIndex + 1);
			if (rowNext != null) {
				rowNext.fixupPosition();
				Composite nextComposite = rowNext.getComposite();
				if (nextComposite != null && !nextComposite.isDisposed()) {
					FormData fdNext = (FormData) nextComposite.getLayoutData();
					if (fdNext != null) {
						ListRow rowPrevious = view.getRow(iIndex - 1);

						Composite previousComposite = rowPrevious == null ? null
								: rowPrevious.getComposite();

						if (previousComposite != null && !previousComposite.isDisposed()) {
							fdNext.top = new FormAttachment(previousComposite, 0);
						} else {
							fdNext.top = new FormAttachment(0, 0);
						}
						nextComposite.setLayoutData(fdNext);
					}
				}
			}

			rowComposite.dispose();
		}
	}

	public void doPaint(GC gc, boolean bVisible) {
		// XXX Copied from TableRowImpl
		if (bDisposed || !bVisible) {
			return;
		}

		Rectangle oldClipping = gc.getClipping();
		try {
			gc.setForeground(getForeground());
			gc.setBackground(getBackground());
			Iterator iter = mapTableCells.values().iterator();
			while (iter.hasNext()) {
				TableCellCore cell = (TableCellCore) iter.next();
				try {
					if (cell.needsPainting()) {
						Rectangle bounds = cell.getBounds();
						if (bounds != null) {
							Rectangle clipping = bounds.intersection(oldClipping);
							gc.setClipping(clipping);

							cell.doPaint(gc);
						}
					}
				} catch (Exception e) {
					Debug.out(e);
				}
			}
		} finally {
			gc.setClipping(oldClipping);
		}
	}

	// XXX Copied from TableRowImpl!!
	public Object getDataSource(boolean bCoreObject) {
		checkCellForSetting();

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
		return rowComposite.getForeground();
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

	public void invalidate() {
		// TODO Auto-generated method stub

	}

	public boolean isRowDisposed() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isSelected() {
		return bSelected;
	}

	public boolean isVisible() {
		//		if (position == 50) {
		//			System.out.println(view.getBounds());
		//			System.out.println(rowComposite.getBounds());
		//		}
		return rowComposite.isVisible()
				&& view.getBounds().intersects(rowComposite.getBounds());
	}

	public void locationChanged(int iStartColumn) {
		// TODO Auto-generated method stub

	}

	// XXX Copied from TableRowImp!
	public void refresh(boolean bDoGraphics) {
		checkCellForSetting();

		// If this were called from a plugin, we'd have to refresh the sorted column
		// even if we weren't visible

		boolean bVisible = isVisible();
		if (!bVisible) {
			setUpToDate(false);
			return;
		}

		Iterator iter = mapTableCells.values().iterator();
		while (iter.hasNext()) {
			TableCellCore item = (TableCellCore) iter.next();
			item.refresh(bDoGraphics, bVisible);
		}
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableRowCore#refresh(boolean, boolean)
	public void refresh(boolean bDoGraphics, boolean bVisible) {
		refresh(bDoGraphics);
	}

	public void setForeground(Color c) {
		Iterator iter = mapTableCells.values().iterator();
		while (iter.hasNext()) {
			TableCellCore item = (TableCellCore) iter.next();
			item.setForeground(c);
		}
	}

	public boolean setHeight(int iHeight) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean setIconSize(Point pt) {
		// TODO Auto-generated method stub
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
		ListRow rowFocused = view.getRowFocused();
		if (rowFocused != null && !rowFocused.equals(this)) {
			rowFocused.setFocused(false);
		}

		if (b) {
			view.rowSetFocused(this);
		} else {
			view.rowSetFocused(null);
		}
		repaint();
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

		ListRow row = view.getRow(iRowPos + 1);
		if (row != null) {
			row.fixupPosition();
		}

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

		ListRow rowPrevious = view.getRow(iRowPos - 1);

		Composite previousComposite = rowPrevious == null ? null
				: rowPrevious.getComposite();

		FormData fd = (FormData) rowComposite.getLayoutData();
		if (previousComposite != null && !previousComposite.isDisposed()) {
			if (fd.top != null && fd.top.control == previousComposite) {
				return false;
			}
			fd.top = new FormAttachment(previousComposite, 0);
		} else {
			if (fd.top != null && fd.top.control == null) {
				return false;
			}
			fd.top = new FormAttachment(0, 0);
		}
		rowComposite.setLayoutData(fd);

		return true;
	}

	public void setUpToDate(boolean upToDate) {
		// TODO Auto-generated method stub

	}

	public Object getDataSource() {
		return getDataSource(false);
	}

	public TableCell getTableCell(String sColumnName) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getTableID() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}

	public Color getBackground() {
		return rowComposite.getBackground();
	}

	/**
	 * @param x
	 * @param y
	 * @return
	 */
	public TableCellCore getTableCellCore(int x, int y) {
		Iterator iter = mapTableCells.values().iterator();
		while (iter.hasNext()) {
			TableCellCore cell = (TableCellCore) iter.next();
			if (cell.isShown()) {
  			Rectangle bounds = cell.getBounds();
  			if (bounds != null && bounds.contains(x, y)) {
  				return cell;
  			}
			}
		}
		return null;
	}

	public ListView getView() {
		return view;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableRowCore#repaint()
	public void repaint() {
		if (rowComposite != null && !rowComposite.isDisposed()) {
			rowComposite.redraw();
		} else {
			Debug.out("ListRow repaint called after rowComposite is disposed.  "
					+ Debug.getCompressedStackTrace());
		}
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableRowCore#setAlternatingBGColor(boolean)
	public void setAlternatingBGColor(boolean bEvenIfNotVisible) {
		setBackgroundColor(getIndex());
	}

	public void doPaint(GC gc) {
		doPaint(gc, isVisible());
	}
}
