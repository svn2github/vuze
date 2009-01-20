/*
 * Created on Jun 16, 2006 2:41:08 PM
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package com.aelitis.azureus.ui.swt.columns.torrent;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
import com.aelitis.azureus.ui.swt.views.skin.TorrentListViewsUtils;
import com.aelitis.azureus.util.DataSourceUtils;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author TuxPaper
 * @created Jun 16, 2006
 *
 * TODO: Implement
 */
public class ColumnInfo
	extends CoreTableColumn
	implements TableCellAddedListener,
	TableCellMouseListener, TableCellSWTPaintListener
{
	public static final Class DATASOURCE_TYPE = Download.class;

	public static final String COLUMN_ID = "Info";

	private static Rectangle imgBounds;

	private static int width = 38;

	private static Image img;
	
	private TableRow previousSelection;

	static {
		img = ImageLoader.getInstance().getImage("icon.info");
		imgBounds = img.getBounds();
//		width = boundsRateMe.width;
	}

	/**
	 * 
	 */
	public ColumnInfo(String sTableID) {
		super(DATASOURCE_TYPE, COLUMN_ID, ALIGN_CENTER, width, sTableID);
		initializeAsGraphic(width);
	}

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			CAT_CONTENT,
		});
	}

	public void cellAdded(TableCell cell) {
		cell.setMarginWidth(0);
		cell.setMarginHeight(0);
		if (cell instanceof TableCellSWT) {
			((TableCellSWT)cell).setCursorID(SWT.CURSOR_HAND);
		}
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener#cellPaint(org.eclipse.swt.graphics.GC, org.gudy.azureus2.ui.swt.views.table.TableCellSWT)
	public void cellPaint(GC gc, TableCellSWT cell) {
		TOTorrent torrent = DataSourceUtils.getTorrent(cell.getDataSource());
		
		if (PlatformTorrentUtils.isContent(torrent, true)) {
  		Rectangle cellBounds = cell.getBounds();
  		if (imgBounds.height > cellBounds.height) {
				int dstW = (imgBounds.width * (cellBounds.height - 4))
						/ imgBounds.height;
				try {
					gc.setAdvanced(true);
				} catch (Exception e) {
					//ignore
				}
				gc.drawImage(img, 0, 0, imgBounds.width, imgBounds.height, cellBounds.x
						+ ((cellBounds.width - dstW) / 2), cellBounds.y + 2, dstW,
						cellBounds.height - 4);
			} else {
    		gc.drawImage(img, cellBounds.x
    				+ ((cellBounds.width - imgBounds.width) / 2), cellBounds.y + ((cellBounds.height - imgBounds.height) / 2));
  		}
		}
	}

	public void cellMouseTrigger(final TableCellMouseEvent event) {
		Object ds = event.cell.getDataSource();
		TOTorrent torrent = null;
		if (ds instanceof TOTorrent) {
			torrent = (TOTorrent) ds;
		} else if (ds instanceof DownloadManager) {
			torrent = ((DownloadManager) ds).getTorrent();
		}
		
		if( ! PlatformTorrentUtils.isContent(torrent, true)) {
			return;
		}
		
		// no rating if row isn't selected yet
		TableRow row = event.cell.getTableRow();
		if (row != null && !row.isSelected()) {
			return;
		}
		
		if(row != previousSelection) {
			previousSelection = row;
			return;
		}


		// only first button
		if (event.button == 1 && event.eventType == TableCellMouseEvent.EVENT_MOUSEUP) {
			try {
				TorrentListViewsUtils.viewDetailsFromDS(torrent, "info-column");
			}catch (Exception e) {
				// TODO: handle exception
			}
		}
	}
}
