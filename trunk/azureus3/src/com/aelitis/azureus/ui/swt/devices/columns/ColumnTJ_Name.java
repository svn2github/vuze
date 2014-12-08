/**
 * Created on Feb 26, 2009
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */

package com.aelitis.azureus.ui.swt.devices.columns;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.ui.swt.debug.ObfusticateCellText;

import com.aelitis.azureus.core.devices.TranscodeFile;
import com.aelitis.azureus.util.DataSourceUtils;

import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author TuxPaper
 * @created Feb 26, 2009
 *
 */
public class ColumnTJ_Name
	implements TableCellRefreshListener, ObfusticateCellText,
	TableCellDisposeListener, TableColumnExtraInfoListener
{
	public static final String COLUMN_ID = "transcode_name";

	/**
	 * 
	 * @param sTableID
	 */
	public ColumnTJ_Name(TableColumn column) {
		column.initialize(TableColumn.ALIGN_LEAD, TableColumn.POSITION_LAST, 215);
		column.addListeners(this);
		column.setObfustication(true);
		column.setRefreshInterval(TableColumn.INTERVAL_GRAPHIC);
		column.setType(TableColumn.TYPE_TEXT_ONLY);
	}

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			TableColumn.CAT_ESSENTIAL,
		});
		info.setProficiency(TableColumnInfo.PROFICIENCY_BEGINNER);
	}

	public void refresh(TableCell cell) {
		TranscodeFile tf = (TranscodeFile) cell.getDataSource();
		if (tf == null) {
			return;
		}

		String text = tf.getName();
		
		if ( text == null || text.length() == 0 ){
			
			return;
		}

		cell.setText(text);
	}

	public String getObfusticatedText(TableCell cell) {
		String name = null;
		DownloadManager dm = DataSourceUtils.getDM(cell.getDataSource());
		if (dm != null) {
			name = dm.toString();
			int i = name.indexOf('#');
			if (i > 0) {
				name = name.substring(i + 1);
			}
		}

		if (name == null)
			name = "";
		return name;
	}

	public void dispose(TableCell cell) {

	}
}
