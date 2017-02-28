/**
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
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
 */

package com.aelitis.azureus.ui.swt.columns.search;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;

import com.aelitis.azureus.ui.common.table.TableColumnCore;

import com.aelitis.azureus.ui.swt.search.SBC_SearchResult;

import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author TuxPaper
 * @created Sep 25, 2008
 *
 */
public class ColumnSearchResultSite
	implements TableCellSWTPaintListener, TableCellAddedListener,
	TableCellRefreshListener
{
	public static final String COLUMN_ID = "site";

	private static int WIDTH = 38; // enough to fit title

	public ColumnSearchResultSite(TableColumn column ) {
	
		column.initialize(TableColumn.ALIGN_CENTER, TableColumn.POSITION_LAST, WIDTH );
		column.addListeners(this);
		column.setRefreshInterval(TableColumn.INTERVAL_INVALID_ONLY);
		column.setType(TableColumn.TYPE_GRAPHIC);

		if ( column instanceof TableColumnCore ){
			
			((TableColumnCore)column).addCellOtherListener("SWTPaint", this );
		}
	}

	public void cellPaint(GC gc, TableCellSWT cell) {
		SBC_SearchResult entry = (SBC_SearchResult) cell.getDataSource();

		Rectangle cellBounds = cell.getBounds();
				
		Image img = entry.getIcon();

		if (img != null && !img.isDisposed()) {
			Rectangle imgBounds = img.getBounds();
			if (cellBounds.width < imgBounds.width || cellBounds.height < imgBounds.height) {
				float dx = (float) cellBounds.width / imgBounds.width;
				float dy = (float) cellBounds.height / imgBounds.height;
				float d = Math.min(dx, dy);
				int newWidth = (int) (imgBounds.width * d);
				int newHeight = (int) (imgBounds.height * d);
				
				gc.drawImage(img, 0, 0, imgBounds.width, imgBounds.height,
						cellBounds.x + (cellBounds.width - newWidth) / 2,
						cellBounds.y + (cellBounds.height - newHeight) / 2, newWidth,
						newHeight);
			} else {
  			gc.drawImage(img, cellBounds.x
  					+ ((cellBounds.width - imgBounds.width) / 2), cellBounds.y
  					+ ((cellBounds.height - imgBounds.height) / 2));
			}
		}
	}

	public void cellAdded(TableCell cell) {
		cell.setMarginWidth(0);
		cell.setMarginHeight(0);
	}

	public void refresh(TableCell cell) {
		SBC_SearchResult entry = (SBC_SearchResult)cell.getDataSource();

		if ( entry != null ){
			
			long sortVal = entry.getEngine().getId();
			
			if (!cell.setSortValue(sortVal) && cell.isValid()) {
				return;
			}

			String name = entry.getEngine().getName();

			Image img = entry.getIcon();
			
			cell.setText(img == null || img.isDisposed() ? name : null);
			
			cell.setToolTip( name);
		}
	}
}
