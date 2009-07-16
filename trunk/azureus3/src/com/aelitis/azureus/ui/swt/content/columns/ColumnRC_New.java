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

package com.aelitis.azureus.ui.swt.content.columns;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;

import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;

import com.aelitis.azureus.core.content.RelatedContent;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;

import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author TuxPaper
 * @created Sep 25, 2008
 *
 */
public class ColumnRC_New
	implements TableCellSWTPaintListener, TableCellAddedListener,
	TableCellRefreshListener, TableCellMouseListener
{
	public static final String COLUMN_ID = "rc_new";

	private static int WIDTH = 38; // enough to fit title

	private static Image imgNew;

	private static Image imgOld;


	public ColumnRC_New(TableColumn column ) {
	
		column.initialize(TableColumn.ALIGN_CENTER, TableColumn.POSITION_LAST, WIDTH );
		column.addListeners(this);
		column.setRefreshInterval(TableColumn.INTERVAL_GRAPHIC);
		column.setType(TableColumn.TYPE_GRAPHIC);

		if ( column instanceof TableColumnCore ){
			
			((TableColumnCore)column).addCellOtherListener("SWTPaint", this );
		}
		
		imgNew = ImageLoader.getInstance().getImage("image.activity.unread");
		imgOld = ImageLoader.getInstance().getImage("image.activity.read");
	}

	public void cellPaint(GC gc, TableCellSWT cell) {
		RelatedContent entry = (RelatedContent) cell.getDataSource();

		Rectangle cellBounds = cell.getBounds();
		Image img = entry== null || entry.isUnread() ? imgNew : imgOld;

		if (img != null && !img.isDisposed()) {
			Rectangle imgBounds = img.getBounds();
			gc.drawImage(img, cellBounds.x
					+ ((cellBounds.width - imgBounds.width) / 2), cellBounds.y
					+ ((cellBounds.height - imgBounds.height) / 2));
		}
	}

	public void cellAdded(TableCell cell) {
		cell.setMarginWidth(0);
		cell.setMarginHeight(0);
		
		if ( cell instanceof TableCellSWT ){
		
			((TableCellSWT)cell).setCursorID( SWT.CURSOR_HAND );
		}
	}

	public void refresh(TableCell cell) {
		RelatedContent entry = (RelatedContent) cell.getDataSource();

		if ( entry != null ){
			
			boolean unread = entry.isUnread();
			
			int sortVal = unread ? 1 : 0;
	
			if (!cell.setSortValue(sortVal) && cell.isValid()) {
				return;
			}
	
			cell.invalidate();
		}
	}

	public void cellMouseTrigger(final TableCellMouseEvent event) {
		if (event.eventType == TableRowMouseEvent.EVENT_MOUSEDOWN
				&& event.button == 1) {
			RelatedContent entry = (RelatedContent) event.cell.getDataSource();
			
			if ( entry != null ){
			
				entry.setUnread(!entry.isUnread());
			
				event.cell.invalidate();
			}
		}
	}
}
