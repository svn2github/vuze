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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellDisposeListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellVisibilityListener;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTGraphic;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTGraphicImpl;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.core.messenger.config.PlatformRatingMessenger;
import com.aelitis.azureus.core.messenger.config.RatingUpdateListener2;
import com.aelitis.azureus.core.torrent.GlobalRatingUtils;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.core.torrent.RatingInfoList;
import com.aelitis.azureus.ui.swt.utils.ColorCache;

/**
 * A column that displays rating info like <code>ColumnRate</code> in a simple manner;
 * this column does not provide any actions such as mouse hover, mouse enter, etc... 
 * 
 * @author khai
 *
 */
public class ColumnRatingGlobal
	extends CoreTableColumn
	implements TableCellAddedListener
{
	public static final String COLUMN_ID = "Rating_global";

	public static final int COLUMN_WIDTH = 60;

	private static Font font = null;

	private static Font smallFont = null;

	/**
	 * 
	 */
	public ColumnRatingGlobal(String sTableID) {
		super(COLUMN_ID, sTableID);
		initializeAsGraphic(POSITION_LAST, COLUMN_WIDTH);
		setWidthLimits(COLUMN_WIDTH, COLUMN_WIDTH);
	}

	public void cellAdded(TableCell cell) {
		new Cell(cell);
	}

	private class Cell
		implements TableCellRefreshListener, TableCellDisposeListener,
		RatingUpdateListener2, TableCellVisibilityListener
	{
		String rating = "--";

		private DownloadManager dm;

		private TableCell cell;

		public Cell(final TableCell cell) {
			this.cell = cell;
			PlatformRatingMessenger.addListener(this);
			cell.addListeners(this);
			cell.setMarginWidth(2);

			dm = getDM(cell.getDataSource());
			if (dm != null) {
				boolean isContent = PlatformTorrentUtils.isContent(dm.getTorrent(),
						true);
				if (!isContent) {
					rating = "";
					return;
				}
			}

		}

		public void dispose(TableCell cell) {
			PlatformRatingMessenger.removeListener(this);
			disposeOldImage(cell);
		}

		// @see org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener#refresh(org.gudy.azureus2.plugins.ui.tables.TableCell)
		public void refresh(TableCell cell) {
			refresh(cell, true);
		}

		public void refresh(final TableCell cell, final boolean force) {

			if (cell.isDisposed()) {
				return;
			}
			DownloadManager newDM = getDM(cell.getDataSource());
			if (dm == null || newDM != dm) {
				if (newDM == null) {
					return;
				}
				dm = newDM;
			}

			if (!Utils.isThisThreadSWT()) {
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						refresh(cell, force);
					}
				});
				return;
			}
			DownloadManager dm = getDM(cell.getDataSource());
			if (dm == null) {
				return;
			}

			TOTorrent torrent = dm.getTorrent();
			String rating = GlobalRatingUtils.getRatingString(torrent);
			long count = GlobalRatingUtils.getCount(torrent);
			int userRating = -3;
			if (PlatformTorrentUtils.isContent(dm.getTorrent(), true)) {
				userRating = PlatformTorrentUtils.getUserRating(dm.getTorrent());
			}

			boolean b;
			try {
				float val = Float.parseFloat(rating) * 1000000 + count;
				val += (userRating + 3) * 10000000;
				b = !cell.setSortValue(val);
			} catch (Exception e) {
				b = !cell.setSortValue(count > 0 ? new Float(count) : null);
			}

			if (!force) {
				if (b && cell.isValid()) {
					return;
				}
				if (!cell.isShown()) {
					return;
				}
			}

			int width = cell.getWidth();
			int height = 34;//cell.getHeight(); KN: HARDCODE!!!
			if (width <= 0 || height <= 0) {
				return;
			}

			/*
			 * Creates a blank image to paint the rating on top of
			 */
			Graphic bgGraphic = cell.getBackgroundGraphic();
			Image img;
			if (bgGraphic instanceof UISWTGraphic) {
				img = ((UISWTGraphic)bgGraphic).getImage();
			} else {
				img = new Image(Display.getDefault(), width, height);
			}
			GC gcImage = new GC(img);
			Rectangle r = img.getBounds();

			int bigTextStyle = SWT.TOP | SWT.RIGHT;
			int smallTextStyle = SWT.RIGHT;

			if (font == null) {
				// no sync required, SWT is on single thread
				FontData[] fontData = gcImage.getFont().getFontData();
				fontData[0].setStyle(SWT.BOLD);
				// we can do a few more pixels because we have no text hanging below baseline
				Utils.getFontHeightFromPX(gcImage.getDevice(), fontData, gcImage, 22);
				font = new Font(Display.getDefault(), fontData);
			}

			gcImage.setFont(font);

			try {
				//				gcImage.setTextAntialias(SWT.ON);
			} catch (Exception e) {
				// Ignore ERROR_NO_GRAPHICS_LIBRARY error or any others
			}

			Color color1 = ColorCache.getColor(Display.getDefault(),
					GlobalRatingUtils.getColor(torrent));
			if (color1 == null) {
				color1 = ColorCache.getColor(gcImage.getDevice(), cell.getForeground());
			}

			r = img.getBounds();
			
			if (color1 != null) {
				gcImage.setForeground(color1);
			}

			GCStringPrinter.printString(gcImage, rating, r, true, false, bigTextStyle);

			if (count > 0) {
				if (smallFont == null) {
					gcImage.setFont(null);
					// no sync required, SWT is on single thread
					FontData[] fontData = gcImage.getFont().getFontData();
					fontData[0].setHeight(Utils.pixelsToPoint(9,
							Display.getDefault().getDPI().y));
					smallFont = new Font(Display.getDefault(), fontData);
				}

				gcImage.setFont(smallFont);
				try {
					//					gcImage.setTextAntialias(SWT.ON);
				} catch (Exception e) {
					// Ignore ERROR_NO_GRAPHICS_LIBRARY error or any others
				}

				Rectangle rectDrawRatings = new Rectangle(0, 0, width, height);
				rectDrawRatings.y = r.y + 21;
				String sRatingInfo = count + " ratings";
				Point ratingInfoExtent = gcImage.textExtent(sRatingInfo);
				if (ratingInfoExtent.x > rectDrawRatings.width) {
					sRatingInfo = DisplayFormatters.formatDecimal(count / 1000.0, 1)
							+ "k ratings";
					ratingInfoExtent = gcImage.textExtent(sRatingInfo);
					if (ratingInfoExtent.x > rectDrawRatings.width) {
						sRatingInfo = (count / 1000) + "k ratings";
					}
				}
				GCStringPrinter.printString(gcImage, sRatingInfo, rectDrawRatings,
						true, false, SWT.TOP | smallTextStyle);
			}

			Graphic graphic = new UISWTGraphicImpl(img);

			disposeOldImage(cell);

			cell.setGraphic(graphic);

			gcImage.dispose();

		}

		/**
		 * 
		 */
		private void disposeOldImage(TableCell cell) {
			Graphic oldGraphic = cell.getGraphic();
			if (oldGraphic instanceof UISWTGraphic) {
				Image image = ((UISWTGraphic) oldGraphic).getImage();
				if (image != null && !image.isDisposed()) {
					image.dispose();
				}
			}
		}

		// @see com.aelitis.azureus.core.messenger.config.PlatformRatingMessenger.RatingUpdateListener#ratingUpdated(com.aelitis.azureus.core.torrent.RatingInfoList)
		public void ratingUpdated(RatingInfoList rating) {
			if (dm == null) {
				return;
			}
			try {
				String hash = dm.getTorrent().getHashWrapper().toBase32String();
				if (rating.hasHash(hash)) {
					refresh(cell, true);
				}
			} catch (Exception e) {
				// ignore
			}
		}

		// @see org.gudy.azureus2.plugins.ui.tables.TableCellVisibilityListener#cellVisibilityChanged(org.gudy.azureus2.plugins.ui.tables.TableCell, int)
		public void cellVisibilityChanged(TableCell cell, int visibility) {
			if (visibility == TableCellVisibilityListener.VISIBILITY_SHOWN) {
				PlatformRatingMessenger.addListener(this);
			} else if (visibility == TableCellVisibilityListener.VISIBILITY_HIDDEN) {
				PlatformRatingMessenger.removeListener(this);
			}
		}

	}

	private DownloadManager getDM(Object ds) {
		DownloadManager dm = null;
		if (ds instanceof DownloadManager) {
			dm = (DownloadManager) ds;
		} else if (ds instanceof VuzeActivitiesEntry) {
			dm = ((VuzeActivitiesEntry) ds).getDownloadManger();
		}
		return dm;
	}

}
