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
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
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
import com.aelitis.azureus.util.DataSourceUtils;
import com.aelitis.azureus.util.PlayUtils;

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

	private static final int WIDTH = 60;

	private static final String BTN_PLAY = "play";

	private static final String BTN_DL = "download";

	private static final String BTN_DETAILS = "details";

	private static final String BTN_RUN = "run";

	private static final int BORDER_SIZE = 0;

	private Map mapCellTorrent = new HashMap();

	private final int maxThumbHeight;

	private Image imgPlay;

	private Rectangle imgPlayBounds;

	private List listClickAreas = new ArrayList();

	private ColumnImageClickArea clickAreaRun;

	private ColumnImageClickArea clickAreaPlay;

	private ColumnImageClickArea clickAreaDetails;

	private ColumnImageClickArea clickAreaDL;

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

		final String IMG_PREFIX = "image.button.";

		clickAreaPlay = new ColumnImageClickArea(COLUMN_ID, BTN_PLAY, IMG_PREFIX
				+ BTN_PLAY);
		clickAreaPlay.setTooltip(MessageText.getString("v3.MainWindow.button.play"));
		listClickAreas.add(clickAreaPlay);

		clickAreaDetails = new ColumnImageClickArea(COLUMN_ID, BTN_DETAILS,
				IMG_PREFIX + BTN_DETAILS);
		clickAreaDetails.setTooltip(MessageText.getString("v3.MainWindow.button.viewdetails"));
		listClickAreas.add(clickAreaDetails);

		clickAreaDL = new ColumnImageClickArea(COLUMN_ID, BTN_DL, IMG_PREFIX
				+ BTN_DL);
		clickAreaDL.setTooltip(MessageText.getString("v3.MainWindow.button.download"));
		listClickAreas.add(clickAreaDL);

		clickAreaRun = new ColumnImageClickArea(COLUMN_ID, BTN_RUN, IMG_PREFIX
				+ BTN_RUN);
		clickAreaRun.setTooltip(MessageText.getString("v3.MainWindow.button.run"));
		listClickAreas.add(clickAreaRun);
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
		DownloadManager dm = DataSourceUtils.getDM(ds);

		//System.out.println("refresh " + bForce + " via " + Debug.getCompressedStackTrace(10));

		TOTorrent newTorrent = DataSourceUtils.getTorrent(ds);
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

		boolean cellHasMouse = (cell instanceof TableCellCore)
				? ((TableCellCore) cell).isMouseOver() : false;

		torrent = newTorrent;
		mapCellTorrent.put(cell, torrent);

		// only dispose of old graphic if it's a thumbnail
		disposeOldImage(cell);

		byte[] b = null;
		boolean canPlay = PlayUtils.canPlayDS(ds);
		boolean showPlayButton = false; // TorrentListViewsUtils.canPlay(dm);
		if (ds instanceof VuzeActivitiesEntry) {
			b = ((VuzeActivitiesEntry) ds).getImageBytes();
		}
		if (b == null) {
			b = PlatformTorrentUtils.getContentThumbnail(torrent);
		}

		boolean canRun = !canPlay && dm != null;
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
			showPlayButton &= cellHasMouse;
		}

		boolean disposeImage = false;
		if (firstImage == null) {
			if (b != null) {
				ByteArrayInputStream bis = new ByteArrayInputStream(b);
				firstImage = new Image(Display.getDefault(), bis);
			} else {
				firstImage = new Image(Display.getDefault(), 1, 1);
			}
			disposeImage = true;
		}
		Image newImg = null;
		try {
			int cellWidth = cell.getWidth();
			int cellHeight = cell.getHeight();

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

			if (cellWidth > w2) {
				dx = (cellWidth - w2) / 2;
			}
			//dx += 18;

			newImg = new Image(firstImage.getDevice(), cellWidth, h2);

			GC gc = new GC(newImg);
			gc.setAdvanced(true);
			try {
				gc.setInterpolation(SWT.HIGH);
				gc.setAntialias(SWT.ON);
			} catch (Exception e) {
				// may not be avail
			}

			//gc.setBackground(ColorCache.getColor(firstImage.getDevice(), 40, 40, 40));
			//gc.fillRoundRectangle(0, 0, cellWidth, h2, 7, 7);

			gc.setBackground(ColorCache.getColor(firstImage.getDevice(), 30, 30, 30));
			//gc.fillRectangle(2, 2, (int) (h2 * 1.77f) - 4, h2 - 4);
			gc.fillRectangle(newImg.getBounds());

			if (cellHasMouse && SET_ALPHA) {
				int[] bg = cell.getBackground();
				if (bg != null) {
					gc.setBackground(ColorCache.getColor(firstImage.getDevice(), bg));
					gc.fillRectangle(newImg.getBounds());
				}
				try {
					gc.setAlpha(40);
				} catch (Exception e) {
					// Ignore ERROR_NO_GRAPHICS_LIBRARY error or any others
				}
			}
			gc.drawImage(firstImage, 0, 0, w, h, dx + BORDER_SIZE, dy + BORDER_SIZE,
					w2 - (BORDER_SIZE * 2), h2 - (BORDER_SIZE * 2));

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

			if (clickAreaDL != null) {
				clickAreaDL.setPosition(cellWidth - 30, cellHeight / 2 - 14);
				clickAreaDL.setVisible(dm == null);
			}
			if (clickAreaDetails != null) {
				clickAreaDetails.setPosition(2, cellHeight / 2 - 14);
				clickAreaDetails.setVisible(getHash(ds, true) != null);
			}
			if (clickAreaRun != null) {
				clickAreaRun.setPosition(cellWidth - 30, cellHeight / 2 - 14);
				clickAreaRun.setVisible(canRun);
			}
			if (clickAreaPlay != null) {
				clickAreaPlay.setPosition(cellWidth / 2 - 16, cellHeight / 2 - 14);
				clickAreaPlay.setVisible(canPlay);
			}

			if (showPlayButton && imgPlay != null) {
				int imgW = imgPlayBounds.width;
				int imgH = imgPlayBounds.height;

				float h3 = (int) (h2 * 0.8);
				float w3 = h3 * imgW / imgH;
				float x = (w2 - w3) / 2;
				float y = (h2 - h3) / 2;

				gc.drawImage(imgPlay, 0, 0, imgW, imgH, (int) x, (int) y, (int) (w3),
						(int) (h3));
			}

			for (Iterator iter = listClickAreas.iterator(); iter.hasNext();) {
				ColumnImageClickArea clickArea = (ColumnImageClickArea) iter.next();

				if (cellHasMouse) {
					clickArea.drawImage(gc);
				}
			}

			if (cellHasMouse && dm != null && !dm.getAssumedComplete()) {
				gc.setForeground(Colors.white);
				String sPercent = dm.getStats().getDownloadCompleted(false) / 10 + "%";
				Point extent = gc.textExtent(sPercent);
				gc.drawText(sPercent, cellWidth - extent.x - 2, cellHeight / 2 - 6,
						true);
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

	private String getHash(Object ds, boolean onlyOurs) {
		if (onlyOurs && !DataSourceUtils.isPlatformContent(ds)) {
			return null;
		}
		return DataSourceUtils.getHash(ds);
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
			log(cell, "whoo, save");
			disposeOldImage(cell);
		} else if (visibility == TableCellVisibilityListener.VISIBILITY_SHOWN) {
			log(cell, "whoo, draw");
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
			if (!((TableCellCore) event.cell).isMouseOver()) {
				return;
			}

			ColumnImageClickArea clickArea = (ColumnImageClickArea) event.data;
			String id = clickArea.getId();

			if (id.equals(BTN_PLAY)) {
				String referal = null;
				Object ds = event.cell.getDataSource();
				if (ds instanceof VuzeActivitiesEntry) {
					if (((VuzeActivitiesEntry) ds).isDRM()) {
						String hash = getHash(event.cell.getDataSource(), true);
						if (hash != null) {
							TorrentListViewsUtils.viewDetails(hash, "thumb");
						}
						return;
					}
					referal = "playdashboardactivity-"
							+ ((VuzeActivitiesEntry) ds).getTypeID();
				}
				TorrentListViewsUtils.playOrStreamDataSource(ds, null, referal);
			} else if (id.equals(BTN_DL)) {
				String referal = null;
				Object ds = event.cell.getDataSource();
				if (ds instanceof VuzeActivitiesEntry) {
					if (((VuzeActivitiesEntry) ds).isDRM()) {
						String hash = getHash(event.cell.getDataSource(), true);
						if (hash != null) {
							TorrentListViewsUtils.viewDetails(hash, "thumb");
						}
						return;
					}

					referal = "dashboardactivity-"
							+ ((VuzeActivitiesEntry) ds).getTypeID();
				}
				TorrentListViewsUtils.downloadDataSource(ds, false, referal);
			} else if (id.equals(BTN_DETAILS)) {
				String hash = getHash(event.cell.getDataSource(), true);
				if (hash != null) {
					TorrentListViewsUtils.viewDetails(hash, "thumb");
				}
			} else if (id.equals(BTN_RUN)) {
				// run via play or stream so we get the security warning
				Object ds = event.cell.getDataSource();
				TorrentListViewsUtils.playOrStreamDataSource(ds, null, "unknown");
			}

			return;
		}

		boolean changed = false;
		if (event.eventType == TableRowMouseEvent.EVENT_MOUSEENTER) {
			changed = true;
		} else if (event.eventType == TableRowMouseEvent.EVENT_MOUSEEXIT) {
			changed = true;
		}
		if (changed && event.cell != null) {
			invalidateAndRefresh(event.cell);
		}
	}

	/**
	 * @param cell
	 *
	 * @since 3.0.5.3
	 */
	private void invalidateAndRefresh(TableCell cell) {
		cell.invalidate();
		if (cell instanceof TableCellCore) {
			TableCellCore cellCore = (TableCellCore) cell;
			cellCore.refreshAsync();
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
			cell.invalidate();
			invalidateAndRefresh(cell);
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellToolTipListener#cellHover(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void cellHover(TableCell cell) {
		if (cell.getToolTip() != null) {
			return;
		}

		Object ds = cell.getDataSource();
		DownloadManager dm = DataSourceUtils.getDM(ds);
		if (dm != null) {
			cell.setToolTip(PlatformTorrentUtils.getContentTitle2(dm));
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellToolTipListener#cellHoverComplete(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void cellHoverComplete(TableCell cell) {
		cell.setToolTip(null);
	}
}
