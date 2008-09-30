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

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;

import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.ui.swt.utils.ImageLoaderFactory;

import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;

/**
 * @author TuxPaper
 * @created Sep 25, 2008
 *
 */
public class ColumnActivityNew
	extends CoreTableColumn
	implements TableCellSWTPaintListener, TableCellRefreshListener
{
	public static final String COLUMN_ID = "activityNew";
	
	private static int WIDTH = 35; // enough to fit title

	private static Image imgNew;

	private Rectangle imgBounds;


	/**
	 * @param name
	 * @param tableID
	 */
	public ColumnActivityNew(String tableID) {
		super(COLUMN_ID, tableID);
		
		initializeAsGraphic(WIDTH);
		imgNew = ImageLoaderFactory.getInstance().getImage("image.activity.unread");
		imgBounds = imgNew.getBounds();
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener#cellPaint(org.eclipse.swt.graphics.GC, org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void cellPaint(GC gc, TableCellSWT cell) {
		VuzeActivitiesEntry entry = (VuzeActivitiesEntry) cell.getDataSource();
		
		if (!entry.isRead()) {
			Rectangle cellBounds = cell.getBounds();
			gc.drawImage(imgNew, cellBounds.x
					+ ((cellBounds.width - imgBounds.width) / 2), cellBounds.y + ((cellBounds.height - imgBounds.height) / 2));
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener#refresh(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void refresh(TableCell cell) {
		VuzeActivitiesEntry entry = (VuzeActivitiesEntry) cell.getDataSource();
		
		cell.setSortValue(entry.isRead() ? 0 : 1);
	}
}
