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

import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;

import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.ui.swt.utils.ImageLoader;
import com.aelitis.azureus.ui.swt.utils.ImageLoaderFactory;

import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;

/**
 * @author TuxPaper
 * @created Sep 25, 2008
 *
 */
public class ColumnActivityType
	extends CoreTableColumn
	implements TableCellSWTPaintListener, TableCellRefreshListener
{
	public static final String COLUMN_ID = "activityType";

	private static SimpleDateFormat timeFormat = new SimpleDateFormat(
			"h:mm:ss a, EEEE, MMMM d, yyyy");

	/**
	 * @param name
	 * @param alignment
	 * @param position
	 * @param width
	 * @param tableID
	 */
	public ColumnActivityType(String tableID) {
		super(COLUMN_ID, 22, tableID);

		initializeAsGraphic(22);
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener#cellPaint(org.eclipse.swt.graphics.GC, org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void cellPaint(GC gc, TableCellSWT cell) {
		VuzeActivitiesEntry entry = (VuzeActivitiesEntry) cell.getDataSource();

		Image imgIcon;
		if (entry.getIconID() != null) {
			ImageLoader imageLoader = ImageLoaderFactory.getInstance();
			imgIcon = imageLoader.getImage(entry.getIconID());
			if (ImageLoader.isRealImage(imgIcon)) {
				Rectangle cellBounds = cell.getBounds();
				Rectangle imgBounds = imgIcon.getBounds();
				gc.drawImage(imgIcon, cellBounds.x
						+ ((cellBounds.width - imgBounds.width) / 2), cellBounds.y
						+ ((cellBounds.height - imgBounds.height) / 2));
			}
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener#refresh(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void refresh(TableCell cell) {
		VuzeActivitiesEntry entry = (VuzeActivitiesEntry) cell.getDataSource();
		if (cell.setSortValue(entry.getTypeID()) || !cell.isValid()) {
			String ts = timeFormat.format(new Date(entry.getTimestamp()));
			cell.setToolTip("Activity occurred on " + ts);
		}
	}

}
