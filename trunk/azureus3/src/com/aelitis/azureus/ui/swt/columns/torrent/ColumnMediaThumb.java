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
import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTGraphic;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTGraphicImpl;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.common.table.TableCellCore;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.swt.columns.utils.ColumnImageClickArea;
import com.aelitis.azureus.ui.swt.utils.ColorCache;
import com.aelitis.azureus.ui.swt.utils.ImageLoader;
import com.aelitis.azureus.ui.swt.utils.ImageLoaderFactory;
import com.aelitis.azureus.ui.swt.views.skin.TorrentListViewsUtils;

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
	TableCellMouseMoveListener, TableRowMouseListener, TableCellToolTipListener
{
	public static String COLUMN_ID = "MediaThumb";

	public static final boolean ROW_HOVER = System.getProperty("rowhover", "0").equals(
			"1");

	private static final boolean SET_ALPHA = true;

	private static final int WIDTH = 68;

	private Map mapCellTorrent = new HashMap();

	private final int maxThumbHeight;

	private Image imgPlay;

	private Rectangle imgPlayBounds;

	private List listClickAreas = new ArrayList();

	/**
	 * 
	 */
	public ColumnMediaThumb(String sTableID, int maxThumbHeight) {
		super(COLUMN_ID, sTableID);
		this.maxThumbHeight = maxThumbHeight;
		initializeAsGraphic(POSITION_LAST, WIDTH);
		setWidthLimits(WIDTH, WIDTH);
		setAlignment(ALIGN_CENTER);

		if (imgPlay == null) {
			loadImages();
		}

		ColumnImageClickArea clickArea;

		clickArea = new ColumnImageClickArea(COLUMN_ID, "play", "image.button.play");
		clickArea.setTooltip("Play Content");
		listClickAreas.add(clickArea);

		clickArea = new ColumnImageClickArea(COLUMN_ID, "details",
				"image.button.details");
		clickArea.setTooltip("View Details");
		listClickAreas.add(clickArea);

		clickArea = new ColumnImageClickArea(COLUMN_ID, "download",
				"image.button.download");
		clickArea.setTooltip("Download Content");
		listClickAreas.add(clickArea);

		clickArea = new ColumnImageClickArea(COLUMN_ID, "run", "image.button.run");
		clickArea.setTooltip("Launch Downloaded File");
		listClickAreas.add(clickArea);
	}

	private void loadImages() {
		ImageLoader imageLoader = ImageLoaderFactory.getInstance();
		imgPlay = imageLoader.getImage("image.thumb.play");
		if (imgPlay != null) {
			imgPlayBounds = imgPlay.getBounds();
		}
	}

	public void cellAdded(TableCell cell) {
		cell.setMarginWidth(0);
		cell.setMarginHeight(0);

		for (Iterator iter = listClickAreas.iterator(); iter.hasNext();) {
			ColumnImageClickArea clickArea = (ColumnImageClickArea) iter.next();
			clickArea.addCell(cell);
		}

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

	public void refresh(final TableCell cell, final boolean bForce) {
		Object ds = cell.getDataSource();
		DownloadManager dm = getDM(ds);

		//System.out.println("refresh " + bForce + " via " + Debug.getCompressedStackTrace(10));

		TOTorrent newTorrent = getTorrent(ds);
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

		if (!Utils.isThisThreadSWT()) {
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					refresh(cell, bForce);
				}
			});
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
		boolean canPlay = TorrentListViewsUtils.canPlay(dm);
		boolean showPlayButton = false; // TorrentListViewsUtils.canPlay(dm);
		if (torrent == null && (ds instanceof VuzeActivitiesEntry)) {
			b = ((VuzeActivitiesEntry) ds).getImageBytes();
			canPlay |= ((VuzeActivitiesEntry) ds).getAssetHash() != null;
		} else {
			b = PlatformTorrentUtils.getContentThumbnail(torrent);
		}

		boolean canRun = !canPlay;
		if (canRun && dm != null && !dm.getAssumedComplete()) {
			canRun = false;
		}

		Image firstImage = null;
		int dx = 0;
		int dy = 0;
		if (b == null) {
			// Don't ever dispose of PathIcon, it's cached and may be used elsewhere
			String path = null;
			if (dm == null) {
				TOTorrentFile[] files = torrent.getFiles();
				if (files.length > 0) {
					path = files[0].getRelativePath();
				}
			} else {
				path = dm.getDownloadState().getPrimaryFile();
			}
			if (path != null) {
				firstImage = ImageRepository.getPathIcon(path, true, torrent != null
						&& !torrent.isSimpleTorrent());
			}
		}

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

		boolean disposeImage = false;
		if (firstImage == null) {
			ByteArrayInputStream bis = new ByteArrayInputStream(b);
			firstImage = new Image(Display.getDefault(), bis);
			disposeImage = true;
		}
		Image newImg = null;
		try {
			int cellWidth = cell.getWidth();

			int w = firstImage.getBounds().width;
			int h = firstImage.getBounds().height;

			int h2;
			int w2;

			if (h > MAXH) {
				h2 = MAXH;
				w2 = h2 * w / h;
			} else {
				h2 = MAXH;
				w2 = h2 * w / h;
			}
			
			//if (cellWidth - 15 > w2) {
				//dx = (cellWidth - 15 - w2) / 2;
			//}
			dx += 16;

			newImg = new Image(firstImage.getDevice(), cellWidth, h2);

			GC gc = new GC(newImg);
			int[] bg = cell.getBackground();
			if (bg != null) {
				gc.setBackground(ColorCache.getColor(firstImage.getDevice(), bg));
			}
			gc.fillRectangle(0, 0, cellWidth, h2);
			gc.setAdvanced(true);
			try {
				gc.setInterpolation(SWT.HIGH);
			} catch (Exception e) {
				// may not be avail
			}
			if (showPlayButton && SET_ALPHA) {
				try {
					gc.setAlpha(40);
				} catch (Exception e) {
					// Ignore ERROR_NO_GRAPHICS_LIBRARY error or any others
				}
			}
			gc.drawImage(firstImage, 0, 0, w, h, dx, dy, w2, h2);

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

					gc.drawImage(imgPlay, 0, 0, imgW, imgH, (int) x, (int) y, (int) (w3),
							(int) (h3));
				}
			}

			int areaY = 0;
			int numClickAreas = listClickAreas.size() - 1;
			if (dm != null && !(ds instanceof VuzeActivitiesEntry)) {
				numClickAreas--;
			}
			String hash = getHash(ds, true);
			float areaYinc = (h2) / numClickAreas;
			for (Iterator iter = listClickAreas.iterator(); iter.hasNext();) {
				ColumnImageClickArea clickArea = (ColumnImageClickArea) iter.next();

				String id = clickArea.getId();
				boolean hideDownload = dm != null && id.equals("download");
				if (hideDownload) {
					if (torrent == null) {
						areaY += areaYinc;
					}
					clickArea.setArea(null);
					continue;
				}
				if (hash == null && id.equals("details")) {
					clickArea.setArea(null);
					continue;
				}

				if (!canRun && id.equals("run")) {
					clickArea.setArea(null);
					continue;
				}

				if (!canPlay && id.equals("play")) {
					clickArea.setArea(null);
					continue;
				}

				Rectangle imageArea = clickArea.getImageArea();
				if (id.equals("download")) {
					clickArea.setPosition(2, areaY + 3);
					float scale = (float) areaYinc / (float) (imageArea.height + 4);
					clickArea.setScale(scale);
				} else {
					clickArea.setPosition(0, areaY);
					float scale = (float) areaYinc / (float) imageArea.height;
					clickArea.setScale(scale);
				}
				areaY += areaYinc;

				//System.out.println("AS:" +  scale + ";" + imageArea.height + ";" + areaYinc);

				clickArea.drawImage(gc);
			}

			gc.dispose();

			if (disposeImage) {
				firstImage.dispose();
			}

			if (newImg == null) {
				cell.setGraphic(null);
			} else {
				Graphic graphic = new disposableUISWTGraphic(newImg);
				cell.setGraphic(graphic);
			}
		} catch (Exception e) {
			// ignore, probably invalid image
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
			dm = ((VuzeActivitiesEntry) ds).getDownloadManger();
		}
		return dm;
	}

	private String getHash(Object ds, boolean onlyOurs) {
		TOTorrent torrent = getTorrent(ds);
		if (torrent != null) {
			try {
				if (onlyOurs && !PlatformTorrentUtils.isContent(torrent, false)) {
					return null;
				}
				return torrent.getHashWrapper().toBase32String();
			} catch (Exception e) {
			}
		} else if (ds instanceof VuzeActivitiesEntry) {
			return ((VuzeActivitiesEntry) ds).getAssetHash();
		}
		return null;
	}

	private TOTorrent getTorrent(Object ds) {
		TOTorrent torrent = null;
		if (ds instanceof DownloadManager) {
			torrent = ((DownloadManager) ds).getTorrent();
		} else if (ds instanceof VuzeActivitiesEntry) {
			torrent = ((VuzeActivitiesEntry) ds).getTorrent();
			if (torrent == null) {
				DownloadManager dm = ((VuzeActivitiesEntry) ds).getDownloadManger();
				if (dm != null) {
					torrent = dm.getTorrent();
				}
			}
		}
		return torrent;
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
		if (event.data instanceof ColumnImageClickArea) {
			ColumnImageClickArea clickArea = (ColumnImageClickArea) event.data;
			String id = clickArea.getId();
			System.err.println("CLICK ON " + id);

			if (id.equals("play")) {
				TorrentListViewsUtils.playOrStreamDataSource(
						event.cell.getDataSource(), null);
			} else if (id.equals("download")) {
				TorrentListViewsUtils.downloadDataSource(event.cell.getDataSource(),
						false, "dldashboardactivity");
			} else if (id.equals("details")) {
				String hash = getHash(event.cell.getDataSource(), true);
				if (hash != null) {
					TorrentListViewsUtils.viewDetails(hash, "thumb");
				}
			}

			return;
		}

		ColumnImageClickArea clickArea = (ColumnImageClickArea) listClickAreas.get(0);
		Rectangle area = clickArea.getArea();
		if (event.x >= area.x) {
			return;
		}

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

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellToolTipListener#cellHover(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void cellHover(TableCell cell) {
		Object ds = cell.getDataSource();
		DownloadManager dm = getDM(ds);
		if (dm != null) {
			cell.setToolTip(PlatformTorrentUtils.getContentTitle2(dm));
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellToolTipListener#cellHoverComplete(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void cellHoverComplete(TableCell cell) {
		cell.setToolTip(null);
	}
}
