/**
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
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


import com.aelitis.azureus.core.subs.Subscription;

import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.CoreTableColumnSWT;


public class ColumnSubscriptionParent
	extends CoreTableColumnSWT
	implements TableCellRefreshListener
{
	public static String COLUMN_ID = "parent";
	
	
	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			CAT_ESSENTIAL,
		});
		info.setProficiency(TableColumnInfo.PROFICIENCY_INTERMEDIATE);
	}

	/** Default Constructor */
	public ColumnSubscriptionParent(String sTableID) {
		super(COLUMN_ID, POSITION_INVISIBLE, 100, sTableID);
		setRefreshInterval(INTERVAL_LIVE);
		setAlignment(ALIGN_LEAD);
	}

	public void refresh(TableCell cell) {
		String parent = "";
		Subscription sub = (Subscription) cell.getDataSource();
		if (sub != null) {
			parent = sub.getParent();
		}

		if (!cell.setSortValue(parent) && cell.isValid()) {
			return;
		}

		if (!cell.isShown()) {
			return;
		}
		
		cell.setText( parent );
	}
}
