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
 
package com.aelitis.azureus.ui.swt.columns.vuzeactivity;

import java.text.DateFormat;
import java.util.Date;

import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;

import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;

/**
 * @author TuxPaper
 * @created Sep 25, 2008
 *
 */
public class ColumnActivityDate
	extends CoreTableColumn
	implements TableCellRefreshListener
{
	
	public static final String COLUMN_ID = "activityDate";

	DateFormat df = DateFormat.getDateInstance();

	/**
	 * @param name
	 * @param tableID
	 */
	public ColumnActivityDate(String tableID) {
		super(COLUMN_ID, 100, tableID);
		setAlignment(ALIGN_TRAIL);
	}
	
	// @see org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener#refresh(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void refresh(TableCell cell) {
		VuzeActivitiesEntry entry = (VuzeActivitiesEntry) cell.getDataSource();
		long timestamp = entry.getTimestamp();
		
		if (!cell.setSortValue(timestamp) && cell.isValid()) {
			return;
		}
		cell.setText(df.format(new Date(timestamp)));
	}

	

}
