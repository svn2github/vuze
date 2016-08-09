/*
 * File    : SavePathItem.java
 * Created : 01 febv. 2004
 * By      : TuxPaper
 *
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
 
package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;

import org.gudy.azureus2.core3.download.DownloadManager;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumnInfo;
import org.gudy.azureus2.ui.swt.views.table.CoreTableColumnSWT;


public class FilesLinkedItem
	extends CoreTableColumnSWT 
	implements TableCellRefreshListener
{
	public static final Class<Download> DATASOURCE_TYPE = Download.class;

	public static final String COLUMN_ID = "fileslinked";

	public FilesLinkedItem(String sTableID) {
		super(DATASOURCE_TYPE, COLUMN_ID, ALIGN_CENTER, 50, sTableID);

		setMinWidthAuto(true);
	}

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
				CAT_CONTENT,
		});
		info.setProficiency(TableColumnInfo.PROFICIENCY_INTERMEDIATE);
	}

	public void refresh(TableCell cell) {
		DownloadManager dm = (DownloadManager)cell.getDataSource();

		int	link_count = 0;

		if ( dm != null ){
			link_count = dm.getDownloadState().getFileLinks().size();
		}

		if (!cell.setSortValue(link_count) && cell.isValid()) {
			
			return;
		}
		
		cell.setText( link_count==0?"":String.valueOf(link_count));
	}
}
