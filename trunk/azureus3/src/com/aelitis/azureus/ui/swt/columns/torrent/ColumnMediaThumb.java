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
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTGraphic;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTGraphicImpl;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.common.table.TableCellCore;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.swt.utils.ColorCache;
import com.aelitis.azureus.ui.swt.utils.ImageLoaderFactory;
import com.aelitis.azureus.ui.swt.views.skin.TorrentListViewsUtils;
import com.aelitis.azureus.util.VuzeActivitiesEntry;

import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author TuxPaper
 * @created Jun 29, 2006
 *
 */
public class ColumnMediaThumb
	extends CoreTableColumn
	implements TableCellAddedListener, TableCellRefreshListener,
	TableCellDisposeListener, TableCellVisibilityListener,
	TableCellMouseMoveListener, TableRowMouseListener
{
	public static String COLUMN_ID = "MediaThumb";

	public static final boolean ROW_HOVER = System.getProperty("rowhover", "0").equals(
			"1");

	private static final boolean SET_ALPHA = false;

	private Map mapCellTorrent = new HashMap();

	private final int maxThumbHeight;

	private Image imgPlay;

	private Rectangle imgPlayBounds;

	/**
	 * 
	 */
	public ColumnMediaThumb(String sTableID, int maxThumbHeight) {
		super(COLUMN_ID, sTableID);
		this.maxThumbHeight = maxThumbHeight;
		initializeAsGraphic(POSITION_LAST, 53);
		setWidthLimits(53, 53);
		setAlignment(ALIGN_CENTER);

		imgPlay = ImageLoaderFactory.getInstance().getImage("image.thumb.play");
		if (imgPlay != null) {
			imgPlayBounds = imgPlay.getBounds();
		}
	}

	public void cellAdded(TableCell cell) {
		cell.setMarginWidth(0);
		cell.setMarginHeight(0);

		TableRow tableRow = cell.getTableRow();
		if (tableRow != null) {
			tableRow.addMouseListener(this);
		}
	}

	public void dispose(TableCell cell) {
		mapCellTorrent.remove(cell);
		disposeOldImage(cell);
	}

	public void refresh(TableCell cell) {
		refresh(cell, false);
	}

	public void refresh(TableCell cell, boolean bForce) {
		Object ds = cell.getDataSource();
		DownloadManager dm = getDM(ds);

		//System.out.println("refresh " + bForce + " via " + Debug.getCompressedStackTrace(10));

		TOTorrent newTorrent = dm == null ? null : dm.getTorrent();
		long lastUpdated = PlatformTorrentUtils.getContentLastUpdated(newTorrent);
		// xxx hack.. cell starts with 0 sort value
		if (lastUpdated == 0) {
			lastUpdated = -1;
		}

		boolean bChanged = cell.setSortValue(lastUpdated) || bForce;

		TOTorrent torrent = (TOTorrent) mapCellTorrent.get(cell);

		if (newTorrent == torrent && !bChanged && cell.isValid()) {
			return;
		}

		if (!bForce && !cell.isShown()) {
			return;
		}

		torrent = newTorrent;
		mapCellTorrent.put(cell, torrent);

		// only dispose of old graphic if it's a thumbnail
		disposeOldImage(cell);

		byte[] b = null;
		boolean showPlayButton = TorrentListViewsUtils.canPlay(dm);
		if (torrent == null && (ds instanceof VuzeActivitiesEntry)) {
			b = ((VuzeActivitiesEntry) ds).imageBytes;
			showPlayButton |= ((VuzeActivitiesEntry) ds).assetHash != null;
		} else {
			b = PlatformTorrentUtils.getContentThumbnail(torrent);
		}

		if (b == null) {
			// Don't ever dispose of PathIcon, it's cached and may be used elsewhere
			String path = dm == null ? null : dm.getDownloadState().getPrimaryFile();
			if (path != null) {
				Image icon = ImageRepository.getPathIcon(path, true,
						dm.getTorrent() != null && !dm.getTorrent().isSimpleTorrent());
				Graphic graphic = new UISWTGraphicImpl(icon);
				cell.setGraphic(graphic);
			} else {
				cell.setGraphic(null);
			}
		} else {

			int MAXH = maxThumbHeight < 0 ? cell.getHeight() : maxThumbHeight;

			TableRow row = cell.getTableRow();
			if (ROW_HOVER) {
				boolean rowHasMouse = (row instanceof TableRowCore)
						? ((TableRowCore) row).isMouseOver() : false;
				showPlayButton &= rowHasMouse;
			} else {
				boolean cellHasMouse = (cell instanceof TableCellCore)
						? ((TableCellCore) cell).isMouseOver() : false;
				showPlayButton &= cellHasMouse;
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
					int[] bg = cell.getBackground();
					if (bg != null) {
						gc.setBackground(ColorCache.getColor(img.getDevice(), bg));
					}
					gc.fillRectangle(0, 0, w2, h2);
					gc.setAdvanced(true);
					try {
						gc.setInterpolation(SWT.HIGH);
					} catch (Exception e) {
						// may not be avail
					}
					if (!showPlayButton && SET_ALPHA) {
						try {
							gc.setAlpha(180);
						} catch (Exception e) {
							// Ignore ERROR_NO_GRAPHICS_LIBRARY error or any others
						}
					}
					gc.drawImage(img, 0, 0, w, h, 0, 0, w2, h2);

					if (cell instanceof TableCellSWT) {
						TableCellSWT cellSWT = (TableCellSWT) cell;
						cellSWT.setCursorID(showPlayButton && cellSWT.isMouseOver()
								? SWT.CURSOR_HAND : SWT.CURSOR_ARROW);
					}

					if (SET_ALPHA) {
  					try {
  						gc.setAlpha(255);
  					} catch (Exception e) {
  						// Ignore ERROR_NO_GRAPHICS_LIBRARY error or any others
  					}
					}
					if (showPlayButton) {

						if (imgPlay != null) {
							int imgW = imgPlayBounds.width;
							int imgH = imgPlayBounds.height;

							float h3 = (int) (h2 * 0.8);
							float w3 = h3 * imgW / imgH;
							float x = (w2 - w3) / 2;
							float y = (h2 - h3) / 2;

							gc.drawImage(imgPlay, 0, 0, imgW, imgH, (int) x, (int) y,
									(int) (w3), (int) (h3));
						}
					}

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

	/**
	 * @param dataSource
	 * @return
	 *
	 * @since 3.0.4.3
	 */
	private DownloadManager getDM(Object ds) {
		DownloadManager dm = null;
		if (ds instanceof DownloadManager) {
			dm = (DownloadManager) ds;
		} else if (ds instanceof VuzeActivitiesEntry) {
			dm = ((VuzeActivitiesEntry) ds).dm;
		}
		return dm;
	}

	/**
	 * 
	 */
	private void disposeOldImage(TableCell cell) {
		Graphic oldGraphic = cell.getGraphic();
		if (oldGraphic instanceof disposableUISWTGraphic) {
			Image oldImage = ((UISWTGraphic) oldGraphic).getImage();
			Utils.disposeSWTObjects(new Object[] {
				oldImage
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

	public class disposableUISWTGraphic
		extends UISWTGraphicImpl
	{
		public disposableUISWTGraphic(Image newImage) {
			super(newImage);
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellMouseListener#cellMouseTrigger(org.gudy.azureus2.plugins.ui.tables.TableCellMouseEvent)
	public void cellMouseTrigger(TableCellMouseEvent event) {
		if (event.eventType == TableRowMouseEvent.EVENT_MOUSEDOWN) {
			TorrentListViewsUtils.playOrStreamDataSource(event.cell.getDataSource(),
					null);
		}
		boolean changed = false;
		if (event.eventType == TableRowMouseEvent.EVENT_MOUSEENTER) {
			changed = true;
		} else if (event.eventType == TableRowMouseEvent.EVENT_MOUSEEXIT) {
			changed = true;
		}
		if (changed && event.cell != null) {
			refresh(event.cell, true);
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableRowMouseListener#rowMouseTrigger(org.gudy.azureus2.plugins.ui.tables.TableRowMouseEvent)
	public void rowMouseTrigger(TableRowMouseEvent event) {
		//if (event instanceof TableCellMouseEvent) {
		//	rowMouseTrigger(event, ((TableCellMouseEvent)event).cell);
		//}
		rowMouseTrigger(event, event.row.getTableCell(COLUMN_ID));
	}

	public void rowMouseTrigger(TableRowMouseEvent event, TableCell cell) {
		boolean changed = false;
		if (event.eventType == TableRowMouseEvent.EVENT_MOUSEENTER) {
			changed = true;
		} else if (event.eventType == TableRowMouseEvent.EVENT_MOUSEEXIT) {
			changed = true;
		}
		if (changed && cell != null) {
			refresh(cell, true);
		}
	}
}
