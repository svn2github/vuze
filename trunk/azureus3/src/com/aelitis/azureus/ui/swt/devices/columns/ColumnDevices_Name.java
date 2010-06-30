/**
 * Created on Feb 24, 2009
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

import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.core.devices.TranscodeProvider;

import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;

/**
 * @author TuxPaper
 * @created Feb 24, 2009
 *
 */
public class ColumnDevices_Name
extends CoreTableColumn
implements TableCellRefreshListener
{
	public static final String COLUMN_ID = "name";

	/**
	 * @param name
	 * @param tableID
	 */
	public ColumnDevices_Name(String tableID) {
		super(COLUMN_ID, tableID);
		addDataSourceType(TranscodeProvider.class);
		initialize(ALIGN_LEAD | ALIGN_TOP, POSITION_INVISIBLE, 150, INTERVAL_INVALID_ONLY);
	}

	public void refresh(TableCell cell) {
		TranscodeProvider ds = (TranscodeProvider) cell.getDataSource();
		String name = ds.getName();
		cell.setText(name);
	}
}
