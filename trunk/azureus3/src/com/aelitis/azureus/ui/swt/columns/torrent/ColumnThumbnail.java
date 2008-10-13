/*
 * File    : HealthItem.java
 * Created : 24 nov. 2003
 * By      : Olivier
 *
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

package com.aelitis.azureus.ui.swt.columns.torrent;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.util.DataSourceUtils;

import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableManager;

/**
 * A non-interactive (no click no hover) thumbnail column
 * @author khai
 *
 */

public class ColumnThumbnail
	extends CoreTableColumn
	implements TableCellRefreshListener, TableCellSWTPaintListener
{
	public static final String COLUMN_ID = "Thumbnail";

	private static final int WIDTH_SMALL = 35;

	private static final int WIDTH_BIG = 60;

	private static final int WIDTH_ACTIVITY = 80;

	/**
	 * Each cell is mapped to a torrent
	 */
	private Map mapCellTorrent = new HashMap();

	/** Default Constructor */
	public ColumnThumbnail(String sTableID) {
		super(COLUMN_ID, sTableID);
		if (TableManager.TABLE_ACTIVITY_BIG.equals(sTableID)) {
			initializeAsGraphic(WIDTH_ACTIVITY);
		} else {
			initializeAsGraphic(sTableID.endsWith(".big") ? WIDTH_BIG : WIDTH_SMALL);
		}
		setAlignment(ALIGN_CENTER);
	}

	public void dispose(TableCell cell) {
		mapCellTorrent.remove(cell);
	}

	public void refresh(final TableCell cell) {

		Object ds = cell.getDataSource();
		TOTorrent newTorrent = DataSourceUtils.getTorrent(ds);

		//System.out.println("REF");
		//TableCellImpl c1 = ((TableCellImpl) cell);
		//TableRowSWT tableRowSWT = c1.getTableRowSWT();
		//TableViewSWTImpl view = (TableViewSWTImpl) tableRowSWT.getView();
		//System.out.println(view.getComposite());
		//view.getTableComposite().redraw(0, 0, 5000, 5000, true);
		//view.getTableComposite().update();
		
		
		/*
		 * For sorting we only create 2 buckets... Vuze content and non-vuze content
		 */
		long sortIndex = PlatformTorrentUtils.isContent(newTorrent, true) ? 0 : 1;
		boolean bChanged = cell.setSortValue(sortIndex);

		/*
		 * Get the torrent for this cell
		 */
		TOTorrent torrent = (TOTorrent) mapCellTorrent.get(cell);

		/*
		 * If the cell is not shown or nothing has changed then skip since there's nothing to update
		 */
		if (false == cell.isShown()
				|| (newTorrent == torrent && !bChanged && cell.isValid())) {
			return;
		}

		torrent = newTorrent;
		mapCellTorrent.put(cell, torrent);
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener#cellPaint(org.eclipse.swt.graphics.GC, org.gudy.azureus2.ui.swt.views.table.TableCellSWT)
	public void cellPaint(GC gc, TableCellSWT cell) {
		Object ds = cell.getDataSource();
		
		TOTorrent torrent = (TOTorrent) mapCellTorrent.get(cell);
		if (torrent == null) {
			return;
		}

		/*
		 * Try to get the image bytes if available
		 */
		byte[] imageBytes = null;
		if (ds instanceof VuzeActivitiesEntry) {
			imageBytes = ((VuzeActivitiesEntry) ds).getImageBytes();
		}
		if (imageBytes == null) {
			imageBytes = PlatformTorrentUtils.getContentThumbnail(torrent);
		}

		Image imgThumbnail = null;
		Rectangle cellBounds = cell.getBounds();

		if (imageBytes != null) {
			/*
			 * Creates an image from what's given
			 */
			ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
			imgThumbnail = new Image(Display.getDefault(), bis);

		} else {
			DownloadManager dm = DataSourceUtils.getDM(ds);
			/*
			 * Try to get an image from the OS
			 */

			// Don't ever dispose of PathIcon, it's cached and may be used elsewhere
			String path = null;
			if (dm == null) {
				if (torrent != null) {
					TOTorrentFile[] files = torrent.getFiles();
					if (files.length > 0) {
						path = files[0].getRelativePath();
					}
				}
			} else {
				path = dm.getDownloadState().getPrimaryFile();
			}
			if (path != null) {
				Image icon = ImageRepository.getPathIcon(path, cellBounds.height >= 32
						&& cellBounds.width >= 32, torrent != null
						&& !torrent.isSimpleTorrent());
				if(icon != null) {
					imgThumbnail = new Image(Display.getDefault(), icon, SWT.IMAGE_COPY);
				}
			}

		}

		if (null != imgThumbnail) {
			if (cellBounds.height > 30) {
				cellBounds.y += 2;
				cellBounds.height -= 4;
			}

			Rectangle imgBounds = imgThumbnail.getBounds();

			int dstWidth;
			int dstHeight;
			if (imgBounds.width > cellBounds.width
					|| imgBounds.height > cellBounds.height) {
				dstWidth = cellBounds.width - 4;
				dstHeight = imgBounds.height * cellBounds.width / imgBounds.width;
				if (cellBounds.height < 30) {
					cellBounds.y += 1;
					cellBounds.height -= 2;
				}
			} else {
				dstWidth = imgBounds.width;
				dstHeight = imgBounds.height;
			}

			try {
				gc.setAdvanced(true);
				gc.setInterpolation(SWT.HIGH);
			} catch (Exception e) {
			}
			int x = cellBounds.x + ((cellBounds.width - dstWidth) / 2);
			int y = cellBounds.y + ((cellBounds.height - dstHeight) / 2);
			if (dstWidth > 0 && dstHeight > 0 && !imgBounds.isEmpty()) {
  			gc.drawImage(imgThumbnail, 0, 0, imgBounds.width, imgBounds.height,
  					x, y, dstWidth, dstHeight);
			}

			imgThumbnail.dispose();
		}
	}
}
