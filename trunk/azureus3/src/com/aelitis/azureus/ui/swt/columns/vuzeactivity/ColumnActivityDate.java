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

import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnCreator;
import org.gudy.azureus2.ui.swt.views.tableitems.ColumnDateSizer;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;

import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener;

/**
 * @author TuxPaper
 * @created Sep 25, 2008
 *
 */
public class ColumnActivityDate
	extends ColumnDateSizer
	implements TableCellAddedListener
{
	public static final String COLUMN_ID = "activityDate";

	/**
	 * @param name
	 * @param tableID
	 */
	public ColumnActivityDate(String tableID) {
		super(null, COLUMN_ID, TableColumnCreator.DATE_COLUMN_WIDTH, tableID);
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener#cellAdded(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void cellAdded(TableCell cell) {
		if (cell instanceof TableCellSWT) {
			((TableCellSWT) cell).setTextAlpha(120);
		}
	}

	// @see org.gudy.azureus2.ui.swt.views.tableitems.ColumnDateSizer#refresh(org.gudy.azureus2.plugins.ui.tables.TableCell, long)
	public void refresh(TableCell cell, long timestamp) {
		VuzeActivitiesEntry entry = (VuzeActivitiesEntry) cell.getDataSource();
		timestamp = entry.getTimestamp();

		super.refresh(cell, timestamp);
	}
}
