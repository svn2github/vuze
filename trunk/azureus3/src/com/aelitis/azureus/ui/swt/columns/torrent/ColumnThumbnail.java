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
import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellDisposeListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.plugins.UISWTGraphic;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTGraphicImpl;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.util.DataSourceUtils;

/**
 * A non-interactive (no click no hover) thumbnail column
 * @author khai
 *
 */

public class ColumnThumbnail
	extends CoreTableColumn
	implements TableCellAddedListener, TableCellRefreshListener,
	TableCellDisposeListener
{
	public static final String COLUMN_ID = "Thumbnail";

	private int columnWidth = 60;

	private int marginBorder = 1;

	/**
	 * Each cell is mapped to a torrent
	 */
	private Map mapCellTorrent = new HashMap();

	/** Default Constructor */
	public ColumnThumbnail(String sTableID) {
		this(sTableID, -1, -1);
	}

	/**
	 * 
	 * @param sTableID
	 * @param width The fixed width of the column
	 * @param marginBorder The border margin for the thumbnail image inside a cell
	 */
	public ColumnThumbnail(String sTableID, int width, int marginBorder) {
		super(COLUMN_ID, sTableID);
		if (width > 0) {
			columnWidth = width;
		}
		if (marginBorder > 0) {
			this.marginBorder = marginBorder;
		}
		initializeAsGraphic(POSITION_LAST, columnWidth);
		setAlignment(ALIGN_CENTER);
	}

	public void cellAdded(TableCell cell) {
		cell.setMarginWidth(marginBorder);
		cell.setMarginHeight(marginBorder);
		cell.addDisposeListener(new TableCellDisposeListener() {
			public void dispose(TableCell cell) {
				Graphic graphic = cell.getGraphic();
				if(graphic != null &&graphic instanceof UISWTGraphic) {
					Image image = ((UISWTGraphic)graphic).getImage();
					if(image!= null && !image.isDisposed()) {
						image.dispose();
					}
					cell.setGraphic(null);
				}
			}
		});
	}

	public void dispose(TableCell cell) {
		mapCellTorrent.remove(cell);
	}

	public void refresh(final TableCell cell) {

		Object ds = cell.getDataSource();
		DownloadManager dm = DataSourceUtils.getDM(ds);
		TOTorrent newTorrent = DataSourceUtils.getTorrent(ds);

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
		if(cell.getGraphic() != null) {
			if(cell.isValid()) {
				return;
			} else {
				Graphic graphic = cell.getGraphic();
				if(graphic instanceof UISWTGraphic) {
					Image image = ((UISWTGraphic)graphic).getImage();
					if(image!= null && !image.isDisposed()) {
						image.dispose();
					}
					cell.setGraphic(null);
				}
				
			}
		}

		Image thumbnailImage = null;

		if (imageBytes != null) {
			/*
			 * Creates an image from what's given
			 */
			ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
			thumbnailImage = new Image(Display.getDefault(), bis);

		} else {
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
				Image icon = ImageRepository.getPathIcon(path, true,
						torrent != null && !torrent.isSimpleTorrent());
				thumbnailImage = new Image(Display.getDefault(), icon,SWT.IMAGE_COPY);
			}

		}

		if (null != thumbnailImage) {

			int cellWidth = cell.getWidth();
			int cellHeight = cell.getHeight();
			Rectangle bounds = thumbnailImage.getBounds();

			/*
			 * If the original image is bigger than the cell (minus margin)
			 * then use GC to resize it to fit
			 * Only perform if the current cell width and height are not 0
			 */
			if (cellWidth > 0 && cellHeight > 0
					&& (bounds.width > cellWidth || bounds.height > cellHeight)) {
				Image resizedImage = new Image(Display.getDefault(), cellWidth,
						cellHeight);
				GC gc = new GC(resizedImage);
				try {
					gc.setAdvanced(true);
					gc.setInterpolation(SWT.HIGH);
				} catch (Exception e) {
				}
				gc.drawImage(thumbnailImage, 0, 0, bounds.width, bounds.height, 0, 0,
						cellWidth, cellHeight);
				
				gc.dispose();

				thumbnailImage.dispose();
				thumbnailImage = resizedImage;

			}

			if (cell instanceof TableCellSWT) {
				((TableCellSWT) cell).setGraphic(thumbnailImage);
			} else {
				cell.setGraphic(new UISWTGraphicImpl(thumbnailImage));
			}

		}
	}
}
