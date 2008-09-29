/**
 * Created on Sep 28, 2008
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

import java.text.SimpleDateFormat;

import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.util.DataSourceUtils;

import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;

/**
 * @author TuxPaper
 * @created Sep 28, 2008
 *
 */
public class ColumnVideoLength
	extends CoreTableColumn
	implements TableCellRefreshListener
{
	public static final String COLUMN_ID = "videoLength";

	private static final int WIDTH = 100;

	/**
	 * @param name
	 * @param alignment
	 * @param position
	 * @param width
	 * @param tableID
	 */
	public ColumnVideoLength(String tableID) {
		super(COLUMN_ID, WIDTH, tableID);
		setAlignment(ALIGN_TRAIL);
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener#refresh(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void refresh(TableCell cell) {
		TOTorrent torrent = DataSourceUtils.getTorrent(cell.getDataSource());
		
		long value = PlatformTorrentUtils.getContentVideoRunningTime(torrent);
		if (!cell.setSortValue(value) && cell.isValid()) {
			return;
		}
		cell.setText(DisplayFormatters.formatTime(value * 1000));
	}
}
