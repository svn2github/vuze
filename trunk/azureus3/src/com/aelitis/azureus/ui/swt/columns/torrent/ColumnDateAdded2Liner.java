/**
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */
package com.aelitis.azureus.ui.swt.columns.torrent;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.ui.swt.columns.utils.TableColumnCreatorV3;

import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author TuxPaper
 * @created Jun 16, 2006
 *
 */
public class ColumnDateAdded2Liner
	extends CoreTableColumn
	implements TableCellRefreshListener
{
	public static String COLUMN_ID = "date_added";
	
	final static String[] FORMATS = new String[] {
		"EEEE, MMMM d, yyyy",
		"EEE, MMMM d, yyyy",
		"MMMM d, ''yy",
		"EEE, MMM d, yyyy",
		"EEE, MMM d, ''yy",
		"MMM d, ''yy",
		"yyyy/mm/dd",
		"yyyy/mm",
	};

	int curFormat = 0;

	int maxWidthUsed = 0;

	Date maxWidthDate = new Date();

	public ColumnDateAdded2Liner(String sTableID) {
		this(sTableID, false);
	}

	public ColumnDateAdded2Liner(String sTableID, boolean bVisible) {
		super(COLUMN_ID, ALIGN_TRAIL,
				bVisible ? POSITION_LAST : POSITION_INVISIBLE,
				TableColumnCreatorV3.DATE_COLUMN_WIDTH, sTableID);
		setMaxWidthAuto(true);
	}

	public void refresh(TableCell cell) {
		DownloadManager dm = (DownloadManager) cell.getDataSource();
		long value = (dm == null) ? 0 : dm.getDownloadState().getLongParameter(
				DownloadManagerState.PARAM_DOWNLOAD_ADDED_TIME);

		if (!cell.setSortValue(value) && cell.isValid()) {
			return;
		}
		if (!cell.isShown()) {
			return;
		}

		Date date = new Date(value);

		if (curFormat >= 0) {
			int newWidth = calcWidth(date, FORMATS[curFormat]);
			if (newWidth > maxWidthUsed) {
				maxWidthUsed = newWidth;
				maxWidthDate = date;
				recalcWidth();
			}
			
			SimpleDateFormat temp = new SimpleDateFormat(FORMATS[curFormat]
					+ "\nh:mm a");
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
		int width = getWidth();

		if (maxWidthDate == null) {
			maxWidthDate = new Date();
		}

		GC gc = new GC(Display.getDefault());
		Point minSize = new Point(99999, 0);
		int idxFormat = FORMATS.length - 1;
		for (int i = 0; i < FORMATS.length; i++) {
			SimpleDateFormat temp = new SimpleDateFormat(FORMATS[i]);
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
		gc.dispose();

		if (curFormat != idxFormat) {
			curFormat = idxFormat;
			invalidateCells();
		}
	}

	public int calcWidth(Date date, String format) {
		GC gc = new GC(Display.getDefault());
		SimpleDateFormat temp = new SimpleDateFormat(FORMATS[curFormat]);
		Point newSize = gc.stringExtent(temp.format(date));
		gc.dispose();
		return newSize.x;
	}
}
