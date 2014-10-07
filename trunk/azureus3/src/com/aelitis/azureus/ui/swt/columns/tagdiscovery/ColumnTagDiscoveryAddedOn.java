/**
 * Copyright (C) 2014 Azureus Software, Inc. All Rights Reserved.
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

package com.aelitis.azureus.ui.swt.columns.tagdiscovery;

import java.util.Date;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.TimeFormatter;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemFillListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnCreator;
import org.gudy.azureus2.ui.swt.views.tableitems.ColumnDateSizer;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.tag.Tag;
import com.aelitis.azureus.core.tag.TagDiscovery;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.TableColumnCoreCreationListener;

public class ColumnTagDiscoveryAddedOn
	implements TableColumnExtraInfoListener, TableCellRefreshListener
{
	public static String COLUMN_ID = "tag.discovery.addedon";

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			TableColumn.CAT_TIME,
		});
		info.setProficiency(TableColumnInfo.PROFICIENCY_BEGINNER);
	}

	/** Default Constructor */
	public ColumnTagDiscoveryAddedOn(TableColumn column) {
		column.setWidth(TableColumnCreator.DATE_COLUMN_WIDTH);
		column.addListeners(this);
	}

	public void refresh(TableCell cell) {
		TableColumn tc = cell.getTableColumn();
		if (tc instanceof ColumnDateSizer) {
			TagDiscovery discovery = (TagDiscovery) cell.getDataSource();
			((ColumnDateSizer) tc).refresh(cell, discovery.getTimestamp());
		}
	}
}
