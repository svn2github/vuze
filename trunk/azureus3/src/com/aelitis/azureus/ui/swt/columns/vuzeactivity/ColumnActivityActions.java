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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.activities.VuzeActivitiesEntryBuddyRequest;

import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;

/**
 * @author TuxPaper
 * @created Sep 25, 2008
 *
 */
public class ColumnActivityActions
	extends CoreTableColumn
	implements TableCellSWTPaintListener, TableCellRefreshListener
{

	public static final String COLUMN_ID = "activityActions";

	/**
	 * @param name
	 * @param tableID
	 */
	public ColumnActivityActions(String tableID) {
		super(COLUMN_ID, tableID);
		initializeAsGraphic(POSITION_LAST, 150);
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener#cellPaint(org.eclipse.swt.graphics.GC, org.gudy.azureus2.ui.swt.views.table.TableCellSWT)
	public void cellPaint(GC gc, TableCellSWT cell) {
		
		String text = cell.getText();
		
		if (text != null && text.length() > 0) {
			Rectangle bounds = cell.getBounds();
			bounds.height -= 8;
			bounds.y += 4;

			GCStringPrinter.printString(gc, text, bounds, true, false, SWT.WRAP);
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener#refresh(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void refresh(TableCell cell) {
		VuzeActivitiesEntry entry = (VuzeActivitiesEntry) cell.getDataSource();
		if (!cell.setSortValue(entry.getTypeID()) && cell.isValid()) {
			return;
		}
		
		if (entry instanceof VuzeActivitiesEntryBuddyRequest) {
			VuzeActivitiesEntryBuddyRequest br = (VuzeActivitiesEntryBuddyRequest) entry;
			String urlAccept = br.getUrlAccept();
			String text = "<A HREF=\"" + urlAccept + "\">Accept</A>";
			cell.setText(text);
		} else {
			cell.setText(null);
		}
	}
}
