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

import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.HSLColor;
import org.gudy.azureus2.ui.swt.plugins.UISWTGraphic;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTGraphicImpl;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.core.messenger.PlatformMessage;
import com.aelitis.azureus.core.messenger.PlatformMessengerListener;
import com.aelitis.azureus.core.messenger.config.PlatformRatingMessenger;
import com.aelitis.azureus.core.messenger.config.RatingUpdateListener2;
import com.aelitis.azureus.core.torrent.GlobalRatingUtils;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.core.torrent.RatingInfoList;
import com.aelitis.azureus.ui.common.table.TableCellCore;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.swt.skin.SWTSkinFactory;
import com.aelitis.azureus.ui.swt.skin.SWTSkinProperties;
import com.aelitis.azureus.ui.swt.utils.ColorCache;
import com.aelitis.azureus.ui.swt.utils.ImageLoaderFactory;
import com.aelitis.azureus.util.VuzeActivitiesEntry;

import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author TuxPaper
 * @created Jun 16, 2006
 */
public class ColumnRate
	extends CoreTableColumn
	implements TableCellAddedListener
{
	public static final String COLUMN_ID = "Rating";

	public static final int COLUMN_WIDTH = 58;

	public static final boolean ROW_HOVER = System.getProperty("rowhover", "0").equals(
			"1");

	private static Font font = null;

	private static Font smallFont = null;

	private static Image imgRateMe;

	private static Image imgRateMeUp;

	private static Image imgRateMeDown;

	private static Image imgUpSmall;

	private static Image imgDownSmall;

	private static Image imgUp;

	private static Image imgDown;

	private static Image imgWait;

	private static Image imgRateMeButton;

	private static Image imgRateMeButtonEnabled;

	private static Image imgRateMeButtonDisabled;

	private static Rectangle boundsRateMe;

	private static int width;

	private boolean useButton;

	private boolean disabled;

	private boolean allowRate = true;

	static {
		imgRateMe = ImageLoaderFactory.getInstance().getImage("icon.rateme");
		boundsRateMe = imgRateMe.getBounds();
		width = boundsRateMe.width;

		imgRateMeButtonEnabled = ImageLoaderFactory.getInstance().getImage(
				"icon.rateme-button");
		imgRateMeButton = imgRateMeButtonEnabled;
		width = Math.max(width, imgRateMeButton.getBounds().width);

		imgRateMeButtonDisabled = ImageLoaderFactory.getInstance().getImage(
				"icon.rateme-button-disabled");
		width = Math.max(width, imgRateMeButtonDisabled.getBounds().width);

		imgRateMeUp = ImageLoaderFactory.getInstance().getImage("icon.rateme.up");
		imgRateMeDown = ImageLoaderFactory.getInstance().getImage(
				"icon.rateme.down");

		imgWait = ImageLoaderFactory.getInstance().getImage("icon.rate.wait");
		width = Math.max(width, imgWait.getBounds().width);

		imgDown = ImageLoaderFactory.getInstance().getImage("icon.rate.down");
		imgUp = ImageLoaderFactory.getInstance().getImage("icon.rate.up");

		imgDownSmall = ImageLoaderFactory.getInstance().getImage(
				"icon.rate.small.down");
		imgUpSmall = ImageLoaderFactory.getInstance().getImage("icon.rate.small.up");
	}

	public ColumnRate(String sTableID) {
		this(sTableID, false);
	}

	/**
	 * 
	 */
	public ColumnRate(String sTableID, boolean allowRate) {
		super(COLUMN_ID, sTableID);
		this.allowRate = allowRate;
		initializeAsGraphic(POSITION_LAST, COLUMN_WIDTH);
		setAlignment(ALIGN_TRAIL);
		setWidthLimits(COLUMN_WIDTH, COLUMN_WIDTH);
	}

	public void cellAdded(TableCell cell) {
		new Cell(cell);
	}

	private class Cell
		implements TableCellRefreshListener, TableCellDisposeListener,
		TableCellMouseMoveListener, TableCellToolTipListener,
		TableRowMouseListener, RatingUpdateListener2, TableCellVisibilityListener
	{
		String rating = "--";

		private boolean bMouseDowned;

		private int hoveringOn = -1;

		private DownloadManager dm;

		private TableCell cell;

		private Rectangle areaUserRating = null;

		public Cell(final TableCell cell) {
			this.cell = cell;
			PlatformRatingMessenger.addListener(this);
			cell.addListeners(this);
			cell.setMarginWidth(0);
			cell.setMarginHeight(0);

			dm = getDM(cell.getDataSource());
			if (dm != null) {
				boolean isContent = PlatformTorrentUtils.isContent(dm.getTorrent(),
						true);
				if (!isContent) {
					rating = "";
					return;
				}
			}
			TableRow tableRow = cell.getTableRow();
			if (tableRow != null) {
				tableRow.addMouseListener(this);
			}
		}

		public void dispose(TableCell cell) {
			PlatformRatingMessenger.removeListener(this);
			disposeOldImage(cell);
		}

		// @see org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener#refresh(org.gudy.azureus2.plugins.ui.tables.TableCell)
		public void refresh(TableCell cell) {
			refresh(cell, false);
		}

		public void refresh(final TableCell cell, final boolean force) {
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
			int height = cell.getHeight();
			if (width <= 0 || height <= 0) {
				return;
			}
			boolean needsFill = false;
			Image img = ((UISWTGraphic) cell.getBackgroundGraphic()).getImage();
			if (img == null) {
				img = new Image(Display.getDefault(), width, height);
				needsFill = true;
			}

			// draw border
			GC gcImage = new GC(img);

			if (needsFill) {
				int[] bg = cell.getBackground();
				if (bg != null) {
					gcImage.setBackground(ColorCache.getColor(gcImage.getDevice(), bg[0],
							bg[1], bg[2]));
					gcImage.fillRectangle(0, 0, width, height);
				}
			}

			boolean showAverage = !useButton;
			boolean showRateActionIcon = useButton;

			Image imgRate = null;
			if (allowRate) {
				if (ROW_HOVER) {
					TableRow row = cell.getTableRow();
					boolean rowHasMouse = (row instanceof TableRowCore)
							? ((TableRowCore) row).isMouseOver() : false;
					if (rowHasMouse && userRating == -1) {
						showAverage = false;
						showRateActionIcon = true;
					}
				}
				boolean cellHasMouse = (cell instanceof TableCellCore)
						? ((TableCellCore) cell).isMouseOver() : false;

				switch (userRating) {
					case -2: // waiting
						imgRate = imgWait;
						break;

					case -1: // unrated
						boolean mouseIn = useButton && cellHasMouse;
						if ((useButton && !mouseIn) || disabled) {
							imgRate = imgRateMeButton;
						} else {
							if (cellHasMouse) {
								showAverage = false;
								showRateActionIcon = true;
							}
							switch (hoveringOn) {
								case 0:
									imgRate = imgRateMeDown;
									break;
								case 1:
									imgRate = imgRateMeUp;
									break;
								default:
									imgRate = imgRateMe;
							}
						}
						break;

					case 0:
						imgRate = useButton ? imgDown : imgDownSmall;
						break;

					case 1:
						imgRate = useButton ? imgUp : imgUpSmall;
						break;
				}
			}

			areaUserRating = null;

			if (showAverage) {
				if (showRateActionIcon) {
					try {
						gcImage.setAlpha(40);
					} catch (Exception e) {
						// Ignore ERROR_NO_GRAPHICS_LIBRARY error or any others
					}
				}
				Rectangle r = img.getBounds();
				int bigTextStyle = SWT.RIGHT;
				int smallTextStyle = SWT.RIGHT;
				if (imgRate != null && (userRating >= 0 || userRating == -2)) {
					//smallTextStyle = SWT.RIGHT;
					Rectangle imgRateDrawArea = imgRate.getBounds();
					imgRateDrawArea.x = r.width - 53;
					imgRateDrawArea.y = (height - 14) / 2
							- (imgRate.getBounds().height / 2);
					gcImage.drawImage(imgRate, imgRateDrawArea.x, imgRateDrawArea.y);
					if (userRating >= 0) {
						areaUserRating = imgRateDrawArea;
					}
				}

				r.y += 2;
				r.height -= 14;

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
					gcImage.setTextAntialias(SWT.ON);
				} catch (Exception e) {
					// Ignore ERROR_NO_GRAPHICS_LIBRARY error or any others
				}

				SWTSkinProperties skinProperties = SWTSkinFactory.getInstance().getSkinProperties();

				int[] cbg = cell.getBackground();
				HSLColor hsl = new HSLColor();
				hsl.initHSLbyRGB(cbg[0], cbg[1], cbg[2]);
				hsl.setLuminence(hsl.getLuminence() - 10);
				Color color2 = ColorCache.getColor(Display.getDefault(), hsl.getRed(),
						hsl.getGreen(), hsl.getBlue());

				if (color2 != null) {
					gcImage.setForeground(color2);
				}

				GCStringPrinter.printString(gcImage, rating, r, true, false,
						bigTextStyle);

				Color color1 = ColorCache.getColor(Display.getDefault(),
						GlobalRatingUtils.getColor(torrent));
				if (color1 == null) {
					color1 = skinProperties.getColor("color.row.fg");
				}

				r = img.getBounds();
				r.width -= 2;
				r.height -= 14;
				gcImage.setForeground(color1);
				GCStringPrinter.printString(gcImage, rating, r, true, false,
						bigTextStyle);

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
						gcImage.setTextAntialias(SWT.DEFAULT);
					} catch (Exception e) {
						// Ignore ERROR_NO_GRAPHICS_LIBRARY error or any others
					}

					Rectangle rectDrawRatings = img.getBounds();
					//rectDrawRatings.height -= 4;
					rectDrawRatings.width -= 3;
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
							true, false, SWT.BOTTOM | smallTextStyle);
				}

				if (showRateActionIcon) {
					try {
						gcImage.setAlpha(255);
					} catch (Exception e) {
						// Ignore ERROR_NO_GRAPHICS_LIBRARY error or any others
					}
				}
			}

			if (showRateActionIcon) {
				if (imgRate != null) {
					Point drawPos = getRateIconPos(imgRate.getBounds(), width, height);
					gcImage.drawImage(imgRate, drawPos.x, drawPos.y);
					areaUserRating = imgRate.getBounds();
					areaUserRating.x = drawPos.x;
					areaUserRating.y = drawPos.y;
				}
			}

			gcImage.dispose();

			Graphic graphic = new UISWTGraphicImpl(img);

			disposeOldImage(cell);

			cell.setGraphic(graphic);
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

		public void cellMouseTrigger(final TableCellMouseEvent event) {
			if (dm == null) {
				return;
			}

			TableRow tableRow = event.cell.getTableRow();
			if (tableRow == null) {
				rowMouseTrigger(event, event.cell);
			}

			if (disabled) {
				return;
			}

			if (event.eventType == TableCellMouseEvent.EVENT_MOUSEUP
					&& event.button == 2) {
				DownloadManager dm = getDM(event.cell.getDataSource());
				if (dm == null) {
					return;
				}

				TOTorrent torrent = dm.getTorrent();
				PlatformRatingMessenger.updateGlobalRating(torrent, 0);
				Utils.beep();
			}

			// rating!
			if (!allowRate) {
				return;
			}

			if (event.eventType == TableCellMouseEvent.EVENT_MOUSEEXIT) {
				hoveringOn = -1;
			} else if (event.eventType == TableCellMouseEvent.EVENT_MOUSEMOVE) {
				int userRating = PlatformTorrentUtils.getUserRating(dm.getTorrent());

				if (userRating == -1) {
					int cellWidth = event.cell.getWidth();
					int cellHeight = event.cell.getHeight();
					Point drawPos = getRateIconPos(boundsRateMe, cellWidth, cellHeight);
					drawPos.x = event.x - drawPos.x;
					drawPos.y = event.y - drawPos.y;
					if (drawPos.x >= 0 && drawPos.y >= 0
							&& drawPos.x < boundsRateMe.width
							&& drawPos.y < boundsRateMe.height) {
						final int value = (drawPos.x < (boundsRateMe.height - drawPos.y + 1))
								? 1 : 0;

						if (hoveringOn != value) {
							hoveringOn = value;
							if ((cell instanceof TableCellCore)) {
								((TableCellCore) event.cell).setCursorID(SWT.CURSOR_HAND);
							}
							refresh(event.cell, true);
						}
					} else {
						if (hoveringOn != -1) {
							hoveringOn = -1;
							if (cell instanceof TableCellCore) {
								((TableCellCore) event.cell).setCursorID(SWT.CURSOR_ARROW);
							}
							refresh(event.cell, true);
						}
					}
				} else {
					if (cell instanceof TableCellCore) {
						if (areaUserRating != null
								&& areaUserRating.contains(event.x, event.y)) {
							((TableCellCore) event.cell).setCursorID(SWT.CURSOR_HAND);
							event.cell.setToolTip("Click to remove your rating");
						} else {
							((TableCellCore) event.cell).setCursorID(SWT.CURSOR_ARROW);
							event.cell.setToolTip(null);
						}
					}
				}
			}

			if (event.eventType == TableCellMouseEvent.EVENT_MOUSEENTER
					|| event.eventType == TableCellMouseEvent.EVENT_MOUSEEXIT) {
				refresh(event.cell, true);
				return;
			}

			if (event.eventType != TableCellMouseEvent.EVENT_MOUSEDOWN
					&& event.eventType != TableCellMouseEvent.EVENT_MOUSEUP) {
				return;
			}

			// only first button
			if (event.button != 1) {
				return;
			}

			DownloadManager dm = getDM(event.cell.getDataSource());
			if (dm == null) {
				return;
			}

			if (!PlatformTorrentUtils.isContent(dm.getTorrent(), true)) {
				return;
			}

			int userRating = PlatformTorrentUtils.getUserRating(dm.getTorrent());

			if (event.eventType == TableCellMouseEvent.EVENT_MOUSEDOWN) {
				bMouseDowned = true;
				return;
			}

			if (event.eventType == TableCellMouseEvent.EVENT_MOUSEUP && bMouseDowned) {
				if (userRating == -1) {
					// not set
					int cellWidth = event.cell.getWidth();
					int cellHeight = event.cell.getHeight();
					Point drawPos = getRateIconPos(boundsRateMe, cellWidth, cellHeight);
					drawPos.x = event.x - drawPos.x;
					drawPos.y = event.y - drawPos.y;

					if (drawPos.x >= 0 && drawPos.y >= 0
							&& drawPos.x < boundsRateMe.width
							&& drawPos.y < boundsRateMe.height) {
						try {
							final TOTorrent torrent = dm.getTorrent();
							final int value = (drawPos.x < (boundsRateMe.height - drawPos.y + 1))
									? 1 : 0;

							PlatformRatingMessenger.setUserRating(torrent, value,
									true, 0, new PlatformMessengerListener() {
										public void replyReceived(PlatformMessage message,
												String replyType, Map reply) {
											refresh(event.cell, true);
										}

										public void messageSent(PlatformMessage message) {
										}
									});
						} catch (Exception e) {
							Debug.out(e);
						}
					}
				} else if (areaUserRating != null
						&& areaUserRating.contains(event.x, event.y)) {
					// remove setting
					try {
						final TOTorrent torrent = dm.getTorrent();
						if (userRating >= 0) {
							refresh(event.cell, true);
							PlatformRatingMessenger.setUserRating(torrent, -1, true,
									0, new PlatformMessengerListener() {
										public void replyReceived(PlatformMessage message,
												String replyType, Map reply) {
											refresh(event.cell, true);
										}

										public void messageSent(PlatformMessage message) {
										}
									});
							event.skipCoreFunctionality = true;
						}
					} catch (Exception e) {
						Debug.out(e);
					}
				}
			}

			bMouseDowned = false;
		}

		// @see org.gudy.azureus2.plugins.ui.tables.TableCellToolTipListener#cellHover(org.gudy.azureus2.plugins.ui.tables.TableCell)
		public void cellHover(TableCell cell) {
			if (Constants.isCVSVersion()) {
				DownloadManager dm = getDM(cell.getDataSource());
				if (dm == null) {
					return;
				}

				TOTorrent torrent = dm.getTorrent();
				long refreshOn = GlobalRatingUtils.getRefreshOn(torrent);
				long diff = (refreshOn - SystemTime.getCurrentTime()) / 1000;
				Object toolTip = cell.getToolTip();
				if (!(toolTip instanceof String) || ((String) toolTip).startsWith("G.")) {
					cell.setToolTip("G.Rating Auto Refreshes in "
							+ TimeFormatter.format(diff));
				}
			}
		}

		// @see org.gudy.azureus2.plugins.ui.tables.TableCellToolTipListener#cellHoverComplete(org.gudy.azureus2.plugins.ui.tables.TableCell)
		public void cellHoverComplete(TableCell cell) {
			// TODO Auto-generated method stub

		}

		// @see org.gudy.azureus2.plugins.ui.tables.TableRowMouseListener#rowMouseTrigger(org.gudy.azureus2.plugins.ui.tables.TableRowMouseEvent)
		public void rowMouseTrigger(TableRowMouseEvent event) {
			TableCell cell = event.row.getTableCell(COLUMN_ID);
			rowMouseTrigger(event, cell);
		}

		public void rowMouseTrigger(TableRowMouseEvent event, TableCell cell) {
			boolean changed = false;
			if (event.eventType == TableRowMouseEvent.EVENT_MOUSEENTER) {
				changed = true;
			} else if (event.eventType == TableRowMouseEvent.EVENT_MOUSEEXIT) {
				changed = true;
			}
			if (changed) {
				if (cell != null) {
					refresh(cell, true);
				} else if (event.row != null) {
					((TableRowCore) event.row).invalidate();
					((TableRowCore) event.row).redraw();
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

	public boolean useButton() {
		return useButton;
	}

	public void setUseButton(boolean useButton) {
		this.useButton = useButton;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
		imgRateMeButton = disabled ? imgRateMeButtonDisabled
				: imgRateMeButtonEnabled;
		this.invalidateCells();
	}

	private DownloadManager getDM(Object ds) {
		DownloadManager dm = null;
		if (ds instanceof DownloadManager) {
			dm = (DownloadManager) ds;
		} else if (ds instanceof VuzeActivitiesEntry) {
			dm = ((VuzeActivitiesEntry) ds).dm;
		}
		return dm;
	}

	private Point getRateIconPos(Rectangle imgBounds, int cellWidth,
			int cellHeight) {
		int x;
		int y = imgBounds.height == cellHeight ? 0
				: (cellHeight - imgBounds.height) / 2;
		if (useButton) {
			x = imgBounds.width == cellWidth ? 0 : (cellWidth - imgBounds.width) / 2;
		} else {
			x = cellWidth - imgBounds.width;
		}
		return new Point(x, y);
	}
}
