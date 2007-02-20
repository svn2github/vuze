/*
 * Created on Jun 29, 2006 10:13:59 PM
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

import java.io.ByteArrayInputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTGraphic;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTGraphicImpl;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.common.table.TableRowCore;

import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author TuxPaper
 * @created Jun 29, 2006
 *
 */
public class ColumnMediaThumb extends CoreTableColumn implements
		TableCellAddedListener
{
	public static String COLUMN_ID = "MediaThumb";

	/**
	 * 
	 */
	public ColumnMediaThumb(String sTableID) {
		super(COLUMN_ID, sTableID);
		initializeAsGraphic(POSITION_LAST, 50);
		setWidthLimits(50, 50);
		setAlignment(ALIGN_CENTER);
	}

	public void cellAdded(TableCell cell) {
		new Cell(cell);
	}

	private class Cell implements TableCellRefreshListener,
			TableCellDisposeListener, TableCellVisibilityListener
	{
		TOTorrent torrent;

		Graphic graphic;

		public Cell(TableCell cell) {
			cell.addListeners(this);
			cell.setMarginWidth(0);
			cell.setMarginHeight(0);
		}

		public void dispose(TableCell cell) {
			disposeOldImage(cell);
		}

		public void refresh(TableCell cell) {
			refresh(cell, false);
		}

		public void refresh(TableCell cell, boolean bForce) {
			DownloadManager dm = (DownloadManager) cell.getDataSource();
			TOTorrent newTorrent = dm.getTorrent();
			long lastUpdated = PlatformTorrentUtils.getContentLastUpdated(newTorrent);
			// xxx hack.. cell starts with 0 sort value
			if (lastUpdated == 0) {
				lastUpdated = -1;
			}

			boolean bChanged = cell.setSortValue(lastUpdated) || bForce;

			if (newTorrent == torrent && !bChanged && cell.isValid()) {
				return;
			}

			if (!bForce && !cell.isShown()) {
				return;
			}

			torrent = newTorrent;

			if (torrent == null) {
				cell.setGraphic(null);
			} else {
				// only dispose of old graphic if it's a thumbnail
				disposeOldImage(cell);

				byte[] b = PlatformTorrentUtils.getContentThumbnail(torrent);

				if (b == null) {
					// Don't ever dispose of PathIcon, it's cached and may be used elsewhere
					String path = dm.getDownloadState().getPrimaryFile();
					if (path != null) {
						Image icon = ImageRepository.getPathIcon(path, true);
						Graphic graphic = new UISWTGraphicImpl(icon);
						cell.setGraphic(graphic);
					} else {
						cell.setGraphic(null);
					}
				} else {
					int MAXH = cell.getHeight() - 2;
					// hack!
					if (MAXH <= 0) {
						MAXH = 30;
					}

					ByteArrayInputStream bis = new ByteArrayInputStream(b);
					try {
  					Image img = new Image(Display.getDefault(), bis);
  
  					int w = img.getBounds().width;
  					int h = img.getBounds().height;
  
  					if (h > MAXH) {
  						int h2 = MAXH;
  						int w2 = h2 * w / h;
  						Image newImg = new Image(img.getDevice(), w2, h2);
  
  						GC gc = new GC(newImg);
  						gc.setAdvanced(true);
  						try {
  							gc.setInterpolation(SWT.HIGH);
  						} catch (Exception e) {
  							// may not be avail
  						}
  						gc.drawImage(img, 0, 0, w, h, 0, 0, w2, h2);
  						gc.dispose();
  
  						img.dispose();
  						img = newImg;
  					}
  
  					Graphic graphic = new disposableUISWTGraphic(img);
  					cell.setGraphic(graphic);
					} catch (Exception e) {
						// ignore, probably invalid image
					}
				}
			}
		}

		/**
		 * 
		 */
		private void disposeOldImage(TableCell cell) {
			Graphic oldGraphic = cell.getGraphic();
			if (oldGraphic instanceof disposableUISWTGraphic) {
				Image oldImage = ((UISWTGraphic) oldGraphic).getImage();
				Utils.disposeSWTObjects(new Object[] { oldImage
				});
			}
		}
		
		public void cellVisibilityChanged(TableCell cell, int visibility) {
			if (visibility == TableCellVisibilityListener.VISIBILITY_HIDDEN) {
				//log(cell, "whoo, save");
				disposeOldImage(cell);
			} else if (visibility == TableCellVisibilityListener.VISIBILITY_SHOWN) {
				//log(cell, "whoo, draw");
				refresh(cell, true);
			}
		}

		private void log(TableCell cell, String s) {
			System.out.println(((TableRowCore) cell.getTableRow()).getIndex() + ":"
					+ System.currentTimeMillis() + ": " + s);
		}
	}

	public class disposableUISWTGraphic extends UISWTGraphicImpl
	{
		public disposableUISWTGraphic(Image newImage) {
			super(newImage);
		}
	}
}
