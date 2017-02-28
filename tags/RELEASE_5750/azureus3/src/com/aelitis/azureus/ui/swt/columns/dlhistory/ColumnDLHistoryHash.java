/**
 * Copyright (C) 2013 Azureus Software, Inc. All Rights Reserved.
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
 */

package com.aelitis.azureus.ui.swt.columns.dlhistory;

import org.gudy.azureus2.core3.history.DownloadHistory;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.plugins.ui.tables.*;

public class ColumnDLHistoryHash
	implements TableCellRefreshListener, TableColumnExtraInfoListener
{
	public static String COLUMN_ID = "hash";

	public void 
	fillTableColumnInfo(
		TableColumnInfo info) 
	{
		info.addCategories(new String[] {
			TableColumn.CAT_ESSENTIAL,
		});
		
		info.setProficiency(TableColumnInfo.PROFICIENCY_BEGINNER);
	}
	
	public 
	ColumnDLHistoryHash(
		TableColumn column) 
	{
		column.setWidth(200);
		column.addListeners(this);
	}

	public void 
	refresh(
		TableCell cell )
	{
		DownloadHistory dl = (DownloadHistory)cell.getDataSource();
		
		byte[]	hash = null;
		
		if ( dl != null ){
			
			hash = dl.getTorrentHash();
		}
		
		String str = hash==null?"":ByteFormatter.encodeString( hash );
		
		if ( !cell.setSortValue(str) && cell.isValid()){
			
			return;
		}

		if (!cell.isShown()){
			
			return;
		}
		
		cell.setText(str);
	}
}
