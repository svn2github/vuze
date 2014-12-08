/**
 * Created on Feb 26, 2009
 *
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
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
 
package com.aelitis.azureus.ui.swt.devices.columns;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;

import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;

import com.aelitis.azureus.core.devices.DeviceOfflineDownload;
import com.aelitis.azureus.ui.common.table.impl.TableColumnImpl;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
import com.aelitis.azureus.ui.swt.utils.ColorCache;
import com.aelitis.azureus.ui.swt.utils.FontUtils;

import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author TuxPaper
 * @created Feb 26, 2009
 *
 */
public class ColumnOD_Completion
implements TableCellAddedListener, TableCellRefreshListener,
TableCellDisposeListener, TableCellSWTPaintListener, TableColumnExtraInfoListener
{
	private static final int borderWidth = 1;

	public static final String COLUMN_ID = "od_completion";

	private static Font fontText;

	private Map<TableCell,Integer> mapCellLastPercentDone = new HashMap<TableCell,Integer>();

	private int marginHeight = -1;
	
	Color textColor;
	

	public ColumnOD_Completion(final TableColumn column) {
		column.initialize(TableColumn.ALIGN_LEAD, TableColumn.POSITION_LAST, 145);
		column.addListeners(this);
		// cheat.  TODO: Either auto-add (in above method), or provide
		// access via TableColumn instead of type casting
		((TableColumnImpl)column).addCellOtherListener("SWTPaint", this);
		column.setType(TableColumn.TYPE_GRAPHIC);
		column.setRefreshInterval(TableColumn.INTERVAL_GRAPHIC);
	}

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			TableColumn.CAT_ESSENTIAL,
		});
		info.setProficiency(TableColumnInfo.PROFICIENCY_BEGINNER);
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener#cellAdded(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void cellAdded(TableCell cell) {
		if (marginHeight != -1) {
			cell.setMarginHeight(marginHeight);
		} else {
			cell.setMarginHeight(2);
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellDisposeListener#dispose(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void dispose(TableCell cell) {
		mapCellLastPercentDone.remove(cell);
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener#refresh(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void refresh(TableCell cell) {
		DeviceOfflineDownload od =  (DeviceOfflineDownload) cell.getDataSource();

		int percentDone = getPerThouDone(od);

		Integer intObj = mapCellLastPercentDone.get(cell);
		int lastPercentDone = intObj == null ? 0 : intObj.intValue();

		if (!cell.setSortValue(percentDone) && cell.isValid()
				&& lastPercentDone == percentDone) {
			return;
		}
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener#cellPaint(org.eclipse.swt.graphics.GC, org.gudy.azureus2.ui.swt.views.table.TableCellSWT)
	public void cellPaint(GC gcImage, TableCellSWT cell) {
		DeviceOfflineDownload od =  (DeviceOfflineDownload) cell.getDataSource();
		
		Rectangle bounds = cell.getBounds();

		int yOfs = (bounds.height - 13) / 2 ;
		int x1 = bounds.width - borderWidth - 2;
		int y1 = bounds.height - 3 - yOfs;

		if (x1 < 10 || y1 < 3) {
			return;
		}

		if ( !od.isTransfering()){
			
			gcImage.fillRectangle( bounds );
			
			return;
		}

		int percentDone = getPerThouDone(od);

		mapCellLastPercentDone.put(cell, new Integer(percentDone));
		
		ImageLoader imageLoader = ImageLoader.getInstance();
		Image imgEnd = imageLoader.getImage("tc_bar_end");
		Image img0 = imageLoader.getImage("tc_bar_0");
		Image img1 = imageLoader.getImage("tc_bar_1");

		//draw begining and end
		if (!imgEnd.isDisposed()) {
			gcImage.drawImage(imgEnd, bounds.x , bounds.y + yOfs);
			gcImage.drawImage(imgEnd, bounds.x + x1 + 1, bounds.y + yOfs);
		}
		
		
		
		int limit = (x1 * percentDone) / 1000;
		
		if (!img1.isDisposed() && limit > 0) {
			Rectangle imgBounds = img1.getBounds();
			gcImage.drawImage(img1, 0, 0, imgBounds.width, imgBounds.height,
					bounds.x + 1, bounds.y + yOfs, limit, imgBounds.height);
		}
		if (percentDone < 1000 && !img0.isDisposed()) {
			Rectangle imgBounds = img0.getBounds();
			gcImage.drawImage(img0, 0, 0, imgBounds.width, imgBounds.height, bounds.x
					+ limit + 1, bounds.y + yOfs, x1 - limit, imgBounds.height);
		}

		imageLoader.releaseImage("tc_bar_end");
		imageLoader.releaseImage("tc_bar_0");
		imageLoader.releaseImage("tc_bar_1");
		
		if(textColor == null) {
			textColor = ColorCache.getColor(gcImage.getDevice(), "#006600" );
		}

		gcImage.setForeground(textColor);

		if (fontText == null) {
			fontText = FontUtils.getFontWithHeight(gcImage.getFont(), gcImage, 10);
		}
		
		gcImage.setFont(fontText);
		
		String sText = DisplayFormatters.formatPercentFromThousands(percentDone);
		
		GCStringPrinter.printString(gcImage, sText, new Rectangle(bounds.x + 4,
				bounds.y + yOfs, bounds.width - 4,13), true,
				false, SWT.CENTER);
	}

	private int getPerThouDone(DeviceOfflineDownload od) {
		if (od == null) {
			return 0;
		}
		long total 	= od.getCurrentTransferSize();
		long rem	= od.getRemaining();
		
		if ( total == 0 || total < rem ){
			
			return( 0 );
		}
		
		if ( rem == 0 ){
			
			return( 1000 );
		}
		
		return((int)( 1000 * ( total - rem ) / total ));
	}
}
