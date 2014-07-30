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

import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumnInfo;
import org.gudy.azureus2.ui.swt.views.table.CoreTableColumnSWT;

import com.aelitis.azureus.core.subs.Subscription;
import com.aelitis.azureus.core.subs.SubscriptionHistory;

/**
 * @author Olivier Chalouhi
 * @created Mar 15, 2009
 *
 */
public class ColumnSubscriptionAutoDownload
	extends CoreTableColumnSWT
	implements TableCellRefreshListener
{
	public static String COLUMN_ID = "auto-download";

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			CAT_ESSENTIAL,
		});
		info.setProficiency(TableColumnInfo.PROFICIENCY_BEGINNER);
	}

	/** Default Constructor */
	public ColumnSubscriptionAutoDownload(String sTableID) {
		super(COLUMN_ID, ALIGN_CENTER, POSITION_LAST, 100, sTableID);
		setMinWidth(100);
		setMaxWidth(100);
	}

	public void refresh(TableCell cell) {
		boolean autoDownload = false;
		Subscription sub = (Subscription) cell.getDataSource();
		if (sub != null) {
			SubscriptionHistory history = sub.getHistory();
			if(history != null) {
				autoDownload = history.isAutoDownload();
			}
		}

		if (!cell.setSortValue(autoDownload) && cell.isValid()) {
			return;
		}

		if (!cell.isShown()) {
			return;
		}
		
		cell.setText( DisplayFormatters.getYesNo( autoDownload ));
		return;
		
	}
}
