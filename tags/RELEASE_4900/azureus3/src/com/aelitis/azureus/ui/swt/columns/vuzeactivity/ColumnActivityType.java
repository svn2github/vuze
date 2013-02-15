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

import org.gudy.azureus2.ui.swt.views.table.CoreTableColumnSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader.ImageDownloaderListener;

import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;

/**
 * @author TuxPaper
 * @created Sep 25, 2008
 *
 */
public class ColumnActivityType
	extends CoreTableColumnSWT
	implements TableCellSWTPaintListener, TableCellRefreshListener
{
	public static final String COLUMN_ID = "activityType";

	private static int WIDTH = 42; // enough to fit title in most cases

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
		super(COLUMN_ID, tableID);

		initializeAsGraphic(WIDTH);
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener#cellPaint(org.eclipse.swt.graphics.GC, org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void cellPaint(GC gc, final TableCellSWT cell) {
		VuzeActivitiesEntry entry = (VuzeActivitiesEntry) cell.getDataSource();

		Image imgIcon = null;
		String iconID = entry.getIconID();
		if (iconID != null) {
			ImageLoader imageLoader = ImageLoader.getInstance();
			if (iconID.startsWith("http")) {
				imgIcon = imageLoader.getUrlImage(iconID,
						new ImageDownloaderListener() {
							public void imageDownloaded(Image image,
									boolean returnedImmediately) {
								if (returnedImmediately) {
									return;
								}
								cell.invalidate();
							}
						});
				if (imgIcon == null) {
					return;
				}
			} else {
				imgIcon = imageLoader.getImage(iconID);
			}

			if (ImageLoader.isRealImage(imgIcon)) {
				Rectangle cellBounds = cell.getBounds();
				Rectangle imgBounds = imgIcon.getBounds();
				gc.drawImage(imgIcon, cellBounds.x
						+ ((cellBounds.width - imgBounds.width) / 2), cellBounds.y
						+ ((cellBounds.height - imgBounds.height) / 2));
			}
			imageLoader.releaseImage(iconID);
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
