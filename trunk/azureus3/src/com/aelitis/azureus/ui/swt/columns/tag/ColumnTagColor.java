/**
 * Copyright (C) 2013 Azureus Software, Inc. All Rights Reserved.
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

package com.aelitis.azureus.ui.swt.columns.tag;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;

import com.aelitis.azureus.core.tag.Tag;
import com.aelitis.azureus.ui.common.table.TableColumnCore;

public class ColumnTagColor
	implements TableCellRefreshListener, TableCellSWTPaintListener, TableColumnExtraInfoListener
{
	public static String COLUMN_ID = "tag.color";

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			TableColumn.CAT_ESSENTIAL,
		});
		info.setProficiency(TableColumnInfo.PROFICIENCY_BEGINNER);
	}

	/** Default Constructor */
	public ColumnTagColor(TableColumn column) {
		column.setWidth(30);
		column.addListeners(this);

		if (column instanceof TableColumnCore) {
			((TableColumnCore) column).addCellOtherListener("SWTPaint", this);
		}
	}

	public void refresh(TableCell cell) {
		Tag tag = (Tag) cell.getDataSource();
		if (tag == null) {
			return;
		}
		int[] color;
		color = tag.getColor();
		
		if (color == null || color.length < 3) {
			return;
		}
		
		int sortVal = color[0] + color[1] << 8 + color[2] << 16;
		
		if (!cell.setSortValue(sortVal) && cell.isValid()) {
			return;
		}

		if (!cell.isShown()) {
			return;
		}
		
		cell.setForeground(color);
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener#cellPaint(org.eclipse.swt.graphics.GC, org.gudy.azureus2.ui.swt.views.table.TableCellSWT)
	public void cellPaint(GC gc, TableCellSWT cell) {
		Rectangle bounds = cell.getBounds();
		Color foregroundSWT = cell.getForegroundSWT();
		if (foregroundSWT != null) {
			gc.setBackground(foregroundSWT);
			bounds.x+=1;
			bounds.y+=1;
			bounds.width-=1;
			bounds.height-=1;
			gc.fillRectangle(bounds);
		}
	}
}
