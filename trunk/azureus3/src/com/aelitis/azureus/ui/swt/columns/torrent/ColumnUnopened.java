/**
 * Created on Sep 19, 2008
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


import org.eclipse.swt.graphics.Image;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTGraphicImpl;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.swt.utils.ImageLoaderFactory;

import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author TuxPaper
 * @created Sep 19, 2008
 *
 */
public class ColumnUnopened
	extends CoreTableColumn
	implements TableCellAddedListener, TableCellRefreshListener,
	TableCellMouseListener
{
	public static final String COLUMN_ID = "unopened";

	private static UISWTGraphicImpl graphicCheck;

	private static int width;


	/**
	 * @param name
	 * @param tableID
	 */
	public ColumnUnopened(String tableID) {
		super(COLUMN_ID, tableID);
		
		if (graphicCheck == null) {
			Image img = ImageLoaderFactory.getInstance().getImage("image.check");
			width = img.getBounds().width;
			graphicCheck = new UISWTGraphicImpl(img);
		}
		
		initializeAsGraphic(POSITION_LAST, width);
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener#cellAdded(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void cellAdded(TableCell cell) {
		cell.setMarginWidth(0);
		cell.setMarginHeight(0);
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener#refresh(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void refresh(TableCell cell) {
		DownloadManager dm = (DownloadManager) cell.getDataSource();
		boolean hasBeenOpened = PlatformTorrentUtils.getHasBeenOpened(dm.getTorrent());
		int sortVal = hasBeenOpened ? 1 : 0;

		if (!cell.setSortValue(sortVal) && cell.isValid()) {
			return;
		}
		if (!cell.isShown()) {
			return;
		}

		cell.setGraphic(hasBeenOpened ? null : graphicCheck);
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellMouseListener#cellMouseTrigger(org.gudy.azureus2.plugins.ui.tables.TableCellMouseEvent)
	public void cellMouseTrigger(TableCellMouseEvent event) {
		if (event.eventType == TableRowMouseEvent.EVENT_MOUSEUP && event.button == 1) {
			DownloadManager dm = (DownloadManager) event.cell.getDataSource();
			boolean hasBeenOpened = !PlatformTorrentUtils.getHasBeenOpened(dm.getTorrent());
			PlatformTorrentUtils.setHasBeenOpened(dm.getTorrent(), hasBeenOpened);
			event.cell.invalidate();
			event.cell.setGraphic(hasBeenOpened ? null : graphicCheck);
		}
	}
}
