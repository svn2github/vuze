/**
 * Created on Feb 26, 2009
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

package com.aelitis.azureus.ui.swt.devices.columns;


import com.aelitis.azureus.core.devices.DeviceOfflineDownload;

import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.debug.ObfusticateCellText;

/**
 * @author TuxPaper
 * @created Feb 26, 2009
 *
 */
public class ColumnOD_Name
	implements TableCellRefreshListener, TableColumnExtraInfoListener, ObfusticateCellText
{
	public static final String COLUMN_ID = "od_name";

	public ColumnOD_Name(TableColumn column) {
		column.initialize(TableColumn.ALIGN_LEAD, TableColumn.POSITION_LAST, 300);
		column.addListeners(this);
		column.setRefreshInterval(TableColumn.INTERVAL_GRAPHIC);
		column.setType(TableColumn.TYPE_TEXT_ONLY);
		column.setObfustication(true);
	}

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			TableColumn.CAT_ESSENTIAL,
		});
		info.setProficiency(TableColumnInfo.PROFICIENCY_BEGINNER);
	}

	public void refresh(TableCell cell) {
		DeviceOfflineDownload od = (DeviceOfflineDownload) cell.getDataSource();
		if (od == null) {
			return;
		}

		String text = od.getDownload().getName();
		
		if ( text == null || text.length() == 0 ){
			
			return;
		}

		cell.setText(text);
	}

	public String getObfusticatedText(TableCell cell) {
		DeviceOfflineDownload od = (DeviceOfflineDownload) cell.getDataSource();
		if (od == null) {
			return null;
		}
		String name = od.getDownload().toString();
		int i = name.indexOf('#');
		if (i > 0) {
			name = name.substring(i + 1);
		}
		return name;
	}
}
