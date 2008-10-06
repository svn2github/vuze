/**
 * Created on Oct 5, 2008
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

package org.gudy.azureus2.ui.swt.views.tableitems;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.TimeFormatter;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;

/**
 * @author TuxPaper
 * @created Oct 5, 2008
 *
 */
public class ColumnDateSizer
	extends CoreTableColumn
	implements TableCellRefreshListener
{
	int curFormat = 0;

	int maxWidthUsed = 0;

	Date maxWidthDate = new Date();

	private boolean showTime = true;

	private boolean multiline = true;

	private static Font fontBold;

	/**
	 * @param name
	 * @param tableID
	 */
	public ColumnDateSizer(String columnID, int width, String tableID) {
		super(columnID, width, tableID);
		setAlignment(ALIGN_TRAIL);

		Boolean bShowTime = (Boolean) getUserData("showTime");
		if (bShowTime != null) {
			showTime = bShowTime.booleanValue();
		} else {
			showTime = COConfigurationManager.getBooleanParameter("v3.Start Advanced");
		}

		TableContextMenuItem menuShowTime = addContextMenuItem("TableColumn.menu.date_added.time");
		menuShowTime.addListener(new MenuItemListener() {
			public void selected(MenuItem menu, Object target) {
				showTime = !showTime;
				setUserData("showTime", new Boolean(showTime));
				recalcWidth();
			}
		});
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener#refresh(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public final void refresh(TableCell cell) {
		refresh(cell, 0);
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener#refresh(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void refresh(TableCell cell, long timestamp) {
		if (!cell.setSortValue(timestamp) && cell.isValid()) {
			return;
		}

		if (timestamp <= 0) {
			return;
		}

		Date date = new Date(timestamp);

		if (curFormat >= 0) {
			if (multiline && cell.getHeight() < 20) {
				multiline = false;
			}
			String suffix = showTime && !multiline ? " hh:mm a" : "";

			int newWidth = calcWidth(date, TimeFormatter.DATEFORMATS_DESC[curFormat]
					+ suffix);
			if (newWidth > maxWidthUsed) {
				maxWidthUsed = newWidth;
				maxWidthDate = date;
				recalcWidth();
			}

			String s = TimeFormatter.DATEFORMATS_DESC[curFormat] + suffix;
			SimpleDateFormat temp = new SimpleDateFormat(s
					+ (showTime && multiline ? "\nh:mm a" : ""));
			cell.setText(temp.format(date));
		}
	}

	// @see com.aelitis.azureus.ui.common.table.impl.TableColumnImpl#setWidth(int)
	public void setWidth(int width) {
		int oldWidth = this.getWidth();
		super.setWidth(width);

		if (oldWidth == width) {
			return;
		}
		recalcWidth();
	}

	public void recalcWidth() {
		String suffix = showTime && !multiline ? " hh:mm a" : "";

		int width = getWidth();

		if (maxWidthDate == null) {
			maxWidthDate = new Date();
		}

		int idxFormat = TimeFormatter.DATEFORMATS_DESC.length - 1;

		GC gc = new GC(Display.getDefault());
		if (fontBold == null) {
			FontData[] fontData = gc.getFont().getFontData();
			for (int i = 0; i < fontData.length; i++) {
				FontData fd = fontData[i];
				fd.setStyle(SWT.BOLD);
			}
			fontBold = new Font(gc.getDevice(), fontData);
		}
		gc.setFont(fontBold);

		try {
			Point minSize = new Point(99999, 0);
			for (int i = 0; i < TimeFormatter.DATEFORMATS_DESC.length; i++) {
				SimpleDateFormat temp = new SimpleDateFormat(
						TimeFormatter.DATEFORMATS_DESC[i] + suffix);
				Point newSize = gc.stringExtent(temp.format(maxWidthDate));
				maxWidthUsed = newSize.x;
				if (newSize.x < width - 6) {
					idxFormat = i;
					break;
				}
				if (newSize.x < minSize.x) {
					minSize = newSize;
					idxFormat = i;
				}
			}
		} catch (Throwable t) {
			return;
		} finally {
			gc.dispose();
		}

		if (curFormat != idxFormat) {
			curFormat = idxFormat;
			invalidateCells();
		}
	}

	public int calcWidth(Date date, String format) {
		GC gc = new GC(Display.getDefault());
		if (fontBold == null) {
			FontData[] fontData = gc.getFont().getFontData();
			for (int i = 0; i < fontData.length; i++) {
				FontData fd = fontData[i];
				fd.setStyle(SWT.BOLD);
			}
			fontBold = new Font(gc.getDevice(), fontData);
		}
		gc.setFont(fontBold);
		SimpleDateFormat temp = new SimpleDateFormat(
				TimeFormatter.DATEFORMATS_DESC[curFormat]);
		Point newSize = gc.stringExtent(temp.format(date));
		gc.dispose();
		return newSize.x;
	}

	public boolean getShowTime() {
		return showTime;
	}

	public void setShowTime(boolean showTime) {
		this.showTime = showTime;
	}

	/**
	 * @return the multiline
	 */
	public boolean isMultiline() {
		return multiline;
	}

	/**
	 * @param multiline the multiline to set
	 */
	public void setMultiline(boolean multiline) {
		this.multiline = multiline;
	}
}
