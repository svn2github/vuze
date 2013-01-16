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

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.CoreTableColumnSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;

import com.aelitis.azureus.core.subs.Subscription;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;

/**
 * @author Olivier Chalouhi
 * @created Oct 7, 2008
 *
 */
public class ColumnSubscriptionNew
	extends CoreTableColumnSWT
	implements TableCellRefreshListener,TableCellSWTPaintListener
{
	public static final String COLUMN_ID = "new";

	private static int WIDTH = 38; // enough to fit title

	private static Image imgNew;

	private Rectangle imgBounds;

	/**
	 * @param name
	 * @param tableID
	 */
	public ColumnSubscriptionNew(String tableID) {
		super(COLUMN_ID, tableID);

		initializeAsGraphic(WIDTH);
		setMinWidth(WIDTH);
		setMaxWidth(WIDTH);
		setVisible(true);
		imgNew = ImageLoader.getInstance().getImage("image.activity.unread");
		imgBounds = imgNew.getBounds();
	}

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			CAT_ESSENTIAL,
		});
		info.setProficiency(TableColumnInfo.PROFICIENCY_BEGINNER);
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener#cellPaint(org.eclipse.swt.graphics.GC, org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void cellPaint(GC gc, TableCellSWT cell) {
		Subscription sub = (Subscription) cell.getDataSource();

		if (sub.getHistory().getNumUnread() > 0) {
			Rectangle cellBounds = cell.getBounds();
			gc.drawImage(imgNew, cellBounds.x
					+ ((cellBounds.width - imgBounds.width) / 2), cellBounds.y
					+ ((cellBounds.height - imgBounds.height) / 2));
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener#cellAdded(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void cellAdded(TableCell cell) {
		cell.setMarginWidth(0);
		cell.setMarginHeight(0);
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener#refresh(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void refresh(TableCell cell) {
		Subscription sub = (Subscription) cell.getDataSource();

		boolean isRead = sub.getHistory().getNumUnread() > 0;
		int sortVal = isRead ? 1 : 0;

		if (!cell.setSortValue(sortVal) && cell.isValid()) {
			return;
		}

		cell.invalidate();
	}
}
