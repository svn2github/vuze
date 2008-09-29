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

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.ui.swt.plugins.UISWTGraphic;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTGraphicImpl;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.swt.utils.ImageLoaderFactory;
import com.aelitis.azureus.ui.swt.views.skin.TorrentListViewsUtils;

import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author TuxPaper
 * @created Jun 16, 2006
 *
 * TODO: Implement
 */
public class ColumnInfo
	extends CoreTableColumn
	implements TableCellAddedListener, TableCellRefreshListener,
	TableCellMouseListener
{
	public static final String COLUMN_ID = "Info";

	private static UISWTGraphicImpl graphicInfo;

	private static Rectangle boundsInfo;

	private static int width = 30;

	static {
		Image img = ImageLoaderFactory.getInstance().getImage("icon.info");
		graphicInfo = new UISWTGraphicImpl(img);
		boundsInfo = img.getBounds();
//		width = boundsRateMe.width;
	}

	/**
	 * 
	 */
	public ColumnInfo(String sTableID) {
		super(COLUMN_ID, sTableID);
		initializeAsGraphic(width);
		setAlignment(ALIGN_CENTER);
	}

	public void cellAdded(TableCell cell) {
		cell.setMarginWidth(0);
		cell.setMarginHeight(0);
	}

	public void refresh(TableCell cell) {
		Object ds = cell.getDataSource();
		TOTorrent torrent = null;
		if (ds instanceof TOTorrent) {
			torrent = (TOTorrent) ds;
		} else if (ds instanceof DownloadManager) {
			torrent = ((DownloadManager) ds).getTorrent();
		}
		
		if (!cell.isShown() || cell.isValid()) {
			return;
		}

		if (torrent == null || !PlatformTorrentUtils.isContent(torrent, true)) {
			cell.setGraphic(null);
		} else {
			UISWTGraphic graphic = graphicInfo;
			cell.setGraphic(graphic);
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

		if (torrent == null) {
			return;
		}


		// only first button
		if (event.button == 1 && event.eventType == TableCellMouseEvent.EVENT_MOUSEUP) {
			try {
				TorrentListViewsUtils.viewDetails(torrent.getHashWrapper().toBase32String(), "info-column");
			}catch (Exception e) {
				// TODO: handle exception
			}
		}
	}



	public void setDisabled(boolean disabled) {
		this.invalidateCells();
	}
}
