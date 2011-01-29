/**
 * Created on Sep 25, 2008
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */

package com.aelitis.azureus.ui.swt.columns.torrent;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.plugins.download.DownloadTypeIncomplete;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.ui.common.table.TableCellCore;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.common.table.impl.TableColumnManager;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
import com.aelitis.azureus.ui.swt.views.skin.TorrentListViewsUtils;
import com.aelitis.azureus.util.DLReferals;
import com.aelitis.azureus.util.PlayUtils;

/**
 * @author TuxPaper
 * @created Sep 25, 2008
 *
 */
public class ColumnStream
	extends CoreTableColumn
	implements TableCellSWTPaintListener, TableCellAddedListener,
	TableCellRefreshListener, TableCellMouseListener, TableCellToolTipListener
{
	public static final String COLUMN_ID = "TorrentStream";

	public static final Class[] DATASOURCE_TYPES = {
		DownloadTypeIncomplete.class,
		org.gudy.azureus2.plugins.disk.DiskManagerFileInfo.class
	};

	private static int WIDTH = 62; // enough to fit title

	private static Image imgEnabled;

	private static Image imgDisabled;

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			CAT_ESSENTIAL,
			CAT_CONTENT
		});
		info.setProficiency(TableColumnInfo.PROFICIENCY_BEGINNER);
	}

	/**
	 * @param name
	 * @param tableID
	 */
	public ColumnStream(String tableID) {
		super(COLUMN_ID, tableID);
		addDataSourceTypes(DATASOURCE_TYPES);

		initializeAsGraphic(WIDTH);
		setAlignment(ALIGN_CENTER);
		imgEnabled = ImageLoader.getInstance().getImage("image.stream.small.on");
		imgDisabled = ImageLoader.getInstance().getImage("image.stream.small.off");
	}

	// @see com.aelitis.azureus.ui.common.table.impl.TableColumnImpl#preAdd()
	public void preAdd() {
		if (!isFirstLoad() || getPosition() >= 0 || getColumnAdded()) {
			return;
		}
		TableColumnManager tcManager = TableColumnManager.getInstance();
		TableColumnInfo columnInfoTAN = tcManager.getColumnInfo(null, getTableID(),
				ColumnThumbAndName.COLUMN_ID);
		if (columnInfoTAN != null) {
			TableColumn column = columnInfoTAN.getColumn();
			if (column != null) {
				int position = column.getPosition();
				if (position >= 0) {
					setPosition(position + 1);
				}
			}
		}
	}

	private boolean noIconForYou(Object ds, TableCell cell) {
		if (!(ds instanceof DownloadManager)) {
			return false;
		}
		if (!(cell instanceof TableCellCore)) {
			return false;
		}
		DownloadManager dm = (DownloadManager) ds;
		TableRowCore rowCore = ((TableCellCore) cell).getTableRowCore();
		if (rowCore == null) {
			return false;
		}

		if (dm.getAssumedComplete()
				|| (dm.getNumFileInfos() > 1 && rowCore.isExpanded())) {
			return true;
		}
		return false;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener#cellPaint(org.eclipse.swt.graphics.GC, org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void cellPaint(GC gc, TableCellSWT cell) {

		Object ds = cell.getDataSource();
		if (noIconForYou(ds, cell)) {
			return;
		}
		boolean canStream = PlayUtils.canStreamDS(ds, -1);
		Image img = canStream ? imgEnabled : imgDisabled;

		if (!canStream && PlayUtils.canPlayDS(ds, -1)) {
			return;
		}

		Rectangle cellBounds = cell.getBounds();

		if (img != null && !img.isDisposed()) {
			Rectangle imgBounds = img.getBounds();
			gc.drawImage(img, cellBounds.x
					+ ((cellBounds.width - imgBounds.width) / 2), cellBounds.y
					+ ((cellBounds.height - imgBounds.height) / 2));
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener#cellAdded(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void cellAdded(TableCell cell) {
		cell.setMarginWidth(0);
		cell.setMarginHeight(0);
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener#refresh(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void refresh(TableCell cell) {
		int sortVal;
		Object ds = cell.getDataSource();
		if (noIconForYou(ds, cell)) {
			sortVal = 0;
		} else {
			boolean canStream = PlayUtils.canStreamDS(ds, -1);
			boolean canPlay = PlayUtils.canPlayDS(ds, -1);
			sortVal = (canStream ? 2 : 0) + (canPlay ? 0 : 1);
		}

		if (cell.setSortValue(sortVal)) {
			cell.invalidate();
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellMouseListener#cellMouseTrigger(org.gudy.azureus2.plugins.ui.tables.TableCellMouseEvent)
	public void cellMouseTrigger(final TableCellMouseEvent event) {
		if (event.eventType == TableRowMouseEvent.EVENT_MOUSEDOWN
				&& event.button == 1) {
			Object ds = event.cell.getDataSource();
			if (PlayUtils.canStreamDS(ds, -1)) {
				TorrentListViewsUtils.playOrStreamDataSource(ds, "column", true, false);
			}
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellToolTipListener#cellHover(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void cellHover(TableCell cell) {
		Object ds = cell.getDataSource();
		if (noIconForYou(ds, cell)) {
			cell.setToolTip(null);
			return;
		}
		if (PlayUtils.canStreamDS(ds, -1) || PlayUtils.canPlayDS(ds, -1)) {
			cell.setToolTip(null);
			return;
		}
		cell.setToolTip(MessageText.getString("TableColumn.TorrentStream.tooltip.disabled"));
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellToolTipListener#cellHoverComplete(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void cellHoverComplete(TableCell cell) {
		cell.setToolTip(null);
	}
}
