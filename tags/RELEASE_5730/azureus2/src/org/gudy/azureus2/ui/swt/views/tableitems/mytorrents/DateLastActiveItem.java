/*
 * Created on 05-May-2006
 * Created by Paul Gardner
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableColumnInfo;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnCreator;
import org.gudy.azureus2.ui.swt.views.tableitems.ColumnDateSizer;

public class DateLastActiveItem
	extends ColumnDateSizer
{
	public static final Class DATASOURCE_TYPE = Download.class;

	public static final String COLUMN_ID = "DateTorrentLastActive";

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] { CAT_TIME, CAT_CONTENT });
		info.setProficiency(TableColumnInfo.PROFICIENCY_INTERMEDIATE);
	}

	public DateLastActiveItem(String sTableID) {
		super(DATASOURCE_TYPE, COLUMN_ID, TableColumnCreator.DATE_COLUMN_WIDTH, sTableID);
		
		setMultiline(false);
	}

	public void refresh(TableCell cell, long timestamp) {
		DownloadManager dm = (DownloadManager) cell.getDataSource();
		if (dm == null) {
			timestamp = 0;
		} else {
			timestamp = dm.getDownloadState().getLongParameter(DownloadManagerState.PARAM_DOWNLOAD_LAST_ACTIVE_TIME);
			if (timestamp == 0) {
				timestamp = dm.getDownloadState().getLongParameter(DownloadManagerState.PARAM_DOWNLOAD_COMPLETED_TIME);
			}
			if (timestamp == 0) {
				timestamp = dm.getDownloadState().getLongParameter(DownloadManagerState.PARAM_DOWNLOAD_ADDED_TIME);
			}
		}
		super.refresh(cell, timestamp);
	}
}
