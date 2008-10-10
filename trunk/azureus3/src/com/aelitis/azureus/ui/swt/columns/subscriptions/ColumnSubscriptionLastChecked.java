/**
 * Copyright (C) 2008 Vuze Inc., All Rights Reserved.
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

package com.aelitis.azureus.ui.swt.columns.subscriptions;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.core.subs.Subscription;

/**
 * @author Olivier Chalouhi
 * @created Oct 7, 2008
 *
 */
public class ColumnSubscriptionLastChecked
	extends CoreTableColumn
	implements TableCellRefreshListener
{
	
	SimpleDateFormat format = new SimpleDateFormat("MM/dd/yy hh:mm");
	
	public static String COLUMN_ID = "last-checked";

	/** Default Constructor */
	public ColumnSubscriptionLastChecked(String sTableID) {
		super(COLUMN_ID, POSITION_LAST, 100, sTableID);
		setMinWidth(100);
	}

	public void refresh(TableCell cell) {
		Date date = null;
		String dateText = "--";
		Subscription sub = (Subscription) cell.getDataSource();
		if (sub != null) {
			date =  new Date(sub.getHistory().getLastScanTime());
		}
		
		if (date != null) {
			dateText = format.format(date);
		}

		if (!cell.setSortValue(date) && cell.isValid()) {
			return;
		}

		if (!cell.isShown()) {
			return;
		}
		
		cell.setText(dateText);
		return;
		
	}
}
