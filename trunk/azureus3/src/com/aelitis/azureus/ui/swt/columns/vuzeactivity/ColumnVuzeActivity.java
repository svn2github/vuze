/**
 * Created on Feb 2, 2008 
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */

package com.aelitis.azureus.ui.swt.columns.vuzeactivity;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTGraphic;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTGraphicImpl;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter.URLInfo;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.impl.TableCellImpl;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.activities.VuzeActivitiesConstants;
import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.activities.VuzeActivitiesEntryBuddy;
import com.aelitis.azureus.buddy.VuzeBuddy;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.core.utils.UrlFilter;
import com.aelitis.azureus.ui.common.table.TableCellCore;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.buddy.VuzeBuddySWT;
import com.aelitis.azureus.ui.swt.columns.torrent.ColumnMediaThumb;
import com.aelitis.azureus.ui.swt.columns.torrent.ColumnRate;
import com.aelitis.azureus.ui.swt.skin.SWTSkinFactory;
import com.aelitis.azureus.ui.swt.skin.SWTSkinProperties;
import com.aelitis.azureus.ui.swt.utils.ColorCache;
import com.aelitis.azureus.ui.swt.utils.ImageLoader;
import com.aelitis.azureus.ui.swt.utils.ImageLoaderFactory;
import com.aelitis.azureus.ui.swt.views.list.*;
import com.aelitis.azureus.util.*;

import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author TuxPaper
 * @created Feb 2, 2008
 *
 */
public class ColumnVuzeActivity
	extends CoreTableColumn
	implements TableCellRefreshListener, TableCellDisposeListener,
	TableCellAddedListener, TableCellMouseMoveListener,
	TableCellVisibilityListener, TableCellToolTipListener
{
	private static final int MARGIN_WIDTH = 8 - ListView.COLUMN_MARGIN_WIDTH;

	private static final int EVENT_INDENT = 8 + 16 + 5 - MARGIN_WIDTH;

	private static final int MARGIN_HEIGHT = 7 - 1; // we set row margin height to 1

	private static final int AVATAR_HEIGHT = 40;

	private static final int AVATAR_PADDING = 5;

	public static String COLUMN_ID = "name";

	private static Font headerFont = null;

	private static Font vuzeNewsFont = null;

	private static SimpleDateFormat timeFormat = new SimpleDateFormat(
			"h:mm:ss a, EEEE, MMMM d, yyyy");

	private Color colorLinkNormal;

	private Color colorLinkHover;

	private Color colorHeaderFG;

	private Color colorNewsFG;

	private Color colorNewsBG;

	private Color colorHeaderBG;

	private static Image imgDelete;
	
	private static Image imgUnRead;

	private int sortBy = VuzeActivitiesConstants.SORT_DATE;

	/** Default Constructor */
	public ColumnVuzeActivity(String sTableID) {
		super(COLUMN_ID, POSITION_LAST, 250, sTableID);
		setObfustication(true);
		setType(TableColumn.TYPE_GRAPHIC);
		setRefreshInterval(INTERVAL_LIVE);

		SWTSkinProperties skinProperties = SWTSkinFactory.getInstance().getSkinProperties();
		colorLinkNormal = skinProperties.getColor("color.links.normal");
		colorLinkHover = skinProperties.getColor("color.links.hover");
		colorHeaderFG = skinProperties.getColor("color.activity.row.header.fg");
		colorHeaderBG = skinProperties.getColor("color.activity.row.header.bg");
		colorNewsBG = skinProperties.getColor("color.vuze-entry.news.bg");
		colorNewsFG = skinProperties.getColor("color.vuze-entry.news.fg");

		imgUnRead = ImageLoaderFactory.getInstance().getImage("image.activity.unread");
		//imgDelete = ImageRepository.getImage("progress_remove"); 
	}

	// @see com.aelitis.azureus.ui.common.table.impl.TableColumnImpl#compare(java.lang.Object, java.lang.Object)
	public int compare(Object arg1, Object arg0) {
		VuzeActivitiesEntry c0;
		VuzeActivitiesEntry c1;
		if (arg0 instanceof VuzeActivitiesEntry) {
			c0 = (VuzeActivitiesEntry) arg0;
		} else {
  		TableCellCore cell0 = ((TableRowCore) arg0).getTableCellCore(COLUMN_ID);
  		c0 = (VuzeActivitiesEntry) ((cell0 == null) ? null
  				: cell0.getDataSource());
		}
		if (arg1 instanceof VuzeActivitiesEntry) {
			c1 = (VuzeActivitiesEntry) arg1;
		} else {
  		TableCellCore cell1 = ((TableRowCore) arg1).getTableCellCore(COLUMN_ID);
  		c1 = (VuzeActivitiesEntry) ((cell1 == null) ? null
  				: cell1.getDataSource());
		}

		boolean c0_is_null = c0 == null;
		boolean c1_is_null = c1 == null;
		if (c1_is_null) {
			return (c0_is_null) ? 0 : -1;
		} else if (c0_is_null) {
			return 1;
		}

		//System.out.println("EQ" + timestamp + ";" + text.substring(0, 8) + (int) (timestamp - ((VuzeNewsEntry) obj).timestamp));
		if (sortBy == VuzeActivitiesConstants.SORT_TYPE) {
			String c0TypeID = c0.getTypeID();
			String c1TypeID = c1.getTypeID();

			boolean isC0Header = VuzeActivitiesConstants.TYPEID_HEADER.equals(c0TypeID);
			boolean isC1Header = VuzeActivitiesConstants.TYPEID_HEADER.equals(c1TypeID);

			if (isC0Header) {
				c0TypeID = c0.getID();
			}
			if (isC1Header) {
				c1TypeID = c1.getID();
			}

			long c0IDpos = MapUtils.getMapLong(
					VuzeActivitiesConstants.SORT_TYPE_ORDER, c0TypeID, 100);
			long c1IDpos = MapUtils.getMapLong(
					VuzeActivitiesConstants.SORT_TYPE_ORDER, c1TypeID, 100);
			if (c0IDpos < c1IDpos) {
				return 1;
			}
			if (c0IDpos > c1IDpos) {
				return -1;
			}

			// same
			if (isC0Header) {
				return 1;
			}
			if (isC1Header) {
				return -1;
			}

			// FALLTHROUGH to date sort
		}

		long x = (c0.getTimestamp() - c1.getTimestamp());
		return x == 0 ? 0 : x > 0 ? 1 : -1;
	}

	public void cellAdded(TableCell cell) {
		cell.setMarginWidth(MARGIN_WIDTH);
		cell.setMarginHeight(0);
		cell.setFillCell(true);

		Object ds = cell.getDataSource();
		if (!(ds instanceof VuzeActivitiesEntry)) {
			return;
		}
		VuzeActivitiesEntry entry = (VuzeActivitiesEntry) ds;
		entry.tableColumn = (TableColumnCore) cell.getTableColumn();
		//((TableCellCore) cell).getTableRowCore().setHeight(
		//		(int) (Math.random() * 30) + 20);
	}

	public void refresh(TableCell cell) {
		TableCellImpl thumbCell = getThumbCell(cell);
		TableCellImpl ratingCell = getRatingCell(cell);
		boolean force = !cell.isValid();
		if (thumbCell != null
				&& (!thumbCell.isValid() || thumbCell.getVisuallyChangedSinceRefresh())) {
			force = true;
		}
		if (ratingCell != null
				&& (!ratingCell.isValid() || ratingCell.getVisuallyChangedSinceRefresh())) {
			force = true;
		}

		Object ds = cell.getDataSource();
		if (!(ds instanceof VuzeActivitiesEntry)) {
			return;
		}
		VuzeActivitiesEntry entry = (VuzeActivitiesEntry) ds;

		int width = cell.getWidth();
		int height = cell.getHeight();
		if (width <= 0 || height <= 0) {
			return;
		}

		boolean canShowThumb = canShowThumb(entry);

		Image image = null;

		Graphic graphic = cell.getGraphic();
		if (graphic instanceof UISWTGraphic) {
			image = ((UISWTGraphic) graphic).getImage();
		}

		Rectangle imgBounds = image == null ? null : image.getBounds();
		force |= imgBounds == null
				|| !imgBounds.equals(new Rectangle(0, 0, width, height));

		if (cell.isValid() && !force) {
			return;
		}
		((TableColumnCore) cell.getTableColumn()).setSortValueLive(false);

		if (force) {
			graphic = cell.getBackgroundGraphic();
			if (graphic instanceof UISWTGraphic) {
				image = ((UISWTGraphic) graphic).getImage();
				if (image == null) {
					return;
				}
			}
			imgBounds = image.getBounds();
		}

		Image imgIcon;
		if (entry.getIconID() == null) {
			imgIcon = null;
		} else {
			ImageLoader imageLoader = ImageLoaderFactory.getInstance();
			imgIcon = imageLoader.getImage(entry.getIconID());
			if (!ImageLoader.isRealImage(imgIcon)) {
				imgIcon = null;
			}
		}

		boolean isHeader = VuzeActivitiesConstants.TYPEID_HEADER.equals(entry.getTypeID());
		int x = isHeader ? 0 : EVENT_INDENT;
		int y = 0;

		boolean isVuzeNewsEntry = !isHeader
				&& VuzeActivitiesConstants.TYPEID_VUZENEWS.equalsIgnoreCase(entry.getTypeID());

		Image imgAvatar = null;
		if (entry instanceof VuzeActivitiesEntryBuddy) {
			VuzeActivitiesEntryBuddy entryBuddy = (VuzeActivitiesEntryBuddy) entry;
			VuzeBuddy buddy = entryBuddy.getBuddy();
			if (buddy instanceof VuzeBuddySWT) {
				VuzeBuddySWT buddySWT = (VuzeBuddySWT) buddy;
				imgAvatar = buddySWT.getAvatarImage();
			}
		}

		int avatarPos = x + 10;
		if (imgAvatar != null && !imgAvatar.isDisposed()) {
			x += AVATAR_HEIGHT + AVATAR_PADDING + 12;
		}

		int style = SWT.WRAP;
		Device device = Display.getDefault();
		GCStringPrinter stringPrinter;
		GC gcQuery = new GC(device);
		Rectangle drawRect;
		try {
			try {
				gcQuery.setAdvanced(true);
				gcQuery.setTextAntialias(SWT.ON);
			} catch (Exception e) {
				// Ignore ERROR_NO_GRAPHICS_LIBRARY error or any others
			}
			if (isHeader) {
				if (headerFont == null) {
					// no sync required, SWT is on single thread
					FontData[] fontData = gcQuery.getFont().getFontData();
					fontData[0].setStyle(SWT.BOLD);
					// we can do a few more pixels because we have no text hanging below baseline
					if (!ConstantsV3.isUnix) {
						fontData[0].setName("Arial");
					}
					Utils.getFontHeightFromPX(device, fontData, gcQuery, 17);
					headerFont = new Font(device, fontData);
				}
				gcQuery.setFont(headerFont);
			} else if (isVuzeNewsEntry) {
				if (vuzeNewsFont == null) {
					FontData[] fontData = gcQuery.getFont().getFontData();
					fontData[0].setStyle(SWT.BOLD);
					vuzeNewsFont = new Font(device, fontData);
				}
				gcQuery.setFont(vuzeNewsFont);
			}
			Rectangle potentialArea = new Rectangle(x, 2, width - x - 4, 10000);
			stringPrinter = new GCStringPrinter(gcQuery, entry.getText(),
					potentialArea, 0, SWT.WRAP | SWT.TOP);
			stringPrinter.calculateMetrics();
			Point textSize = stringPrinter.getCalculatedSize();
			//System.out.println(size + ";" + entry.text);

			//boolean focused = ((ListRow) cell.getTableRow()).isFocused();
			//if (focused) size.y += 40;
			if (isHeader) {
				height = 35 - 2;
				y = 8;
			} else {
				height = textSize.y;
				height += (MARGIN_HEIGHT * 2);
				y = MARGIN_HEIGHT + 1;
			}
			style |= SWT.TOP;

			int minHeight = imgAvatar == null || imgAvatar.isDisposed() ? 50
					: AVATAR_HEIGHT + MARGIN_HEIGHT * 2;
			if (!isHeader && height < minHeight) {
				height = minHeight;
			}

			if (canShowThumb) {
				//height += 60;
				if (height < 80 + MARGIN_HEIGHT * 2) {
					height = 80  + MARGIN_HEIGHT * 2;
				}
			}

			boolean isRatingReminder = VuzeActivitiesConstants.TYPEID_RATING_REMINDER.equals(entry.getTypeID());
			if (isRatingReminder && height < 70) {
				height = 70;
			}

			boolean heightChanged = ((TableCellCore) cell).getTableRowCore().setDrawableHeight(
					height);

			if (heightChanged) {
				disposeExisting(cell, null);
				graphic = cell.getBackgroundGraphic();
				if (graphic instanceof UISWTGraphic) {
					image = ((UISWTGraphic) graphic).getImage();
					if (image == null) {
						return;
					}
				}
				imgBounds = image.getBounds();
			}

			if (canShowThumb) {
				if (isRatingReminder) {
					Rectangle ratingRect = getDMRatingRect(width, height);
					int x1 = ratingRect.x + ratingRect.width + 5;
					drawRect = new Rectangle(x1, y, (width / 2) - x1 - 4, height - y + MARGIN_HEIGHT);
				} else {
					drawRect = new Rectangle(x, y, (width / 2) - x - 4, height - y + MARGIN_HEIGHT);
				}
			} else {
				drawRect = new Rectangle(x, y, width - x - 4, height - y + MARGIN_HEIGHT);
			}
			stringPrinter = new GCStringPrinter(gcQuery, entry.getText(), drawRect,
					0, SWT.WRAP | SWT.TOP);
			stringPrinter.calculateMetrics();

			if (stringPrinter.hasHitUrl()) {
				URLInfo[] hitUrlInfo = stringPrinter.getHitUrlInfo();
				for (int i = 0; i < hitUrlInfo.length; i++) {
					URLInfo info = hitUrlInfo[i];
					info.urlColor = colorLinkNormal;
				}
				int[] mouseOfs = cell.getMouseOffset();
				if (mouseOfs != null) {
					URLInfo hitUrl = stringPrinter.getHitUrl(mouseOfs[0] - MARGIN_WIDTH,
							mouseOfs[1]);
					if (hitUrl != null) {
						hitUrl.urlColor = colorLinkHover;
					}
				}
			}

			//System.out.println("height=" + height + ";b=" + imgBounds);
		} finally {
			gcQuery.dispose();
		}

		GC gc = new GC(image);
		try {
			try {
				gc.setAdvanced(true);
				gc.setTextAntialias(SWT.ON);
			} catch (Exception e) {
				// Ignore ERROR_NO_GRAPHICS_LIBRARY error or any others
			}
			gc.setBackground(ColorCache.getColor(device, cell.getBackground()));
			gc.setForeground(ColorCache.getColor(device, cell.getForeground()));

			if (isHeader) {
				gc.setFont(headerFont);
				if (colorHeaderFG != null) {
					gc.setForeground(colorHeaderFG);
				}
				if (colorHeaderBG != null) {
					ListRow row = (ListRow) cell.getTableRow();
					row.setBackgroundColor(colorHeaderBG);
					gc.setBackground(colorHeaderBG);
				}

				gc.fillRectangle(imgBounds);
				height = height - 5;
			} else {
				if (isVuzeNewsEntry) {
					if (colorNewsBG != null) {
						gc.setBackground(colorNewsBG);
						ListRow row = (ListRow) cell.getTableRow();
						row.setBackgroundColor(colorNewsBG);
					}
					if (colorNewsFG != null) {
						gc.setForeground(colorNewsFG);
					}
					gc.setFont(vuzeNewsFont);
				} else {
					TableRow row = cell.getTableRow();
					gc.setBackground(((ListRow) row).getBackground());
				}

				gc.fillRectangle(imgBounds);
				if (imgIcon != null) {
					Rectangle iconBounds = imgIcon.getBounds();
					gc.drawImage(imgIcon, iconBounds.x, iconBounds.y, iconBounds.width,
							iconBounds.height, 0, MARGIN_HEIGHT, 16, 16);
				}
				
				if (imgUnRead != null && !entry.isRead()) {
					Rectangle imageBounds = imgUnRead.getBounds();
					gc.drawImage(imgUnRead, (16 - imageBounds.width) / 2,
							MARGIN_HEIGHT + 22);
				}
			}

			try {
				gc.setInterpolation(SWT.HIGH);
			} catch (Throwable t) {
			}

			if (imgAvatar != null && !imgAvatar.isDisposed()) {
				Rectangle bounds = imgAvatar.getBounds();
				gc.drawImage(imgAvatar, 0, 0, bounds.width, bounds.height, avatarPos,
						MARGIN_HEIGHT, AVATAR_HEIGHT, AVATAR_HEIGHT);
			}

			stringPrinter.printString(gc, drawRect, style);
			entry.urlInfo = stringPrinter;

			if (canShowThumb) {
				Rectangle dmThumbRect = getDMImageRect(width, height);
				if (thumbCell == null) {
					ListCell listCell = new ListCellGraphic((ListRow) cell.getTableRow(),
							SWT.LEFT, dmThumbRect);

					thumbCell = new TableCellImpl((TableRowCore) cell.getTableRow(),
							new ColumnMediaThumb(cell.getTableID(), -1), 0, listCell);
					listCell.setTableCell(thumbCell);

					((TableCellImpl) cell).addChildCell(thumbCell);
					listCell.setParentCell((TableCellSWT) cell);

					setThumbCell(cell, thumbCell);
				}

				((ListCell) thumbCell.getBufferedTableItem()).setBackground(gc.getBackground());
				((ListCell) thumbCell.getBufferedTableItem()).setBounds(dmThumbRect);
				invalidateAndRefresh(thumbCell);
				
				DownloadManager dm = DataSourceUtils.getDM(ds);
				TOTorrent torrent = DataSourceUtils.getTorrent(ds);
				if (dm != null || torrent != null) {
					String title =  PlatformTorrentUtils.getContentTitle2(dm);
					if (title == null) {
						title = PlatformTorrentUtils.getContentTitle(torrent);
						if (title == null && (ds instanceof VuzeActivitiesEntry)) {
							title = ((VuzeActivitiesEntry)ds).getTorrentName();
						}
					}
					long time = PlatformTorrentUtils.getContentVideoRunningTime(torrent);
					int[] resolution = PlatformTorrentUtils.getContentVideoResolution(torrent);
					if (time > 0) {
						title += "\n" + DisplayFormatters.formatTime(time * 1000);
					}
					if (resolution != null) {
						title += "\n" + resolution[0] + "x" + resolution[1];
					}
					Rectangle titleArea = new Rectangle(dmThumbRect.x + dmThumbRect.width
							+ 3, dmThumbRect.y, width - dmThumbRect.x - dmThumbRect.width
							- MARGIN_WIDTH, dmThumbRect.height);
					GCStringPrinter.printString(gc, title, titleArea); 
				}
				
				

				if (VuzeActivitiesConstants.TYPEID_RATING_REMINDER.equals(entry.getTypeID())) {
					if (canShowThumb && DataSourceUtils.isPlatformContent(ds)) {
						Rectangle dmRatingRect = getDMRatingRect(width, height);
						if (ratingCell == null) {
							ListCell listCell = new ListCellGraphic(
									(ListRow) cell.getTableRow(), SWT.RIGHT, dmRatingRect);

							ratingCell = new TableCellImpl((TableRowCore) cell.getTableRow(),
									new ColumnRate(cell.getTableID(), true), 0, listCell);
							listCell.setTableCell(ratingCell);

							((TableCellImpl) cell).addChildCell(ratingCell);
							listCell.setParentCell((TableCellSWT) cell);

							setRatingCell(cell, ratingCell);
						}

						((ListCell) ratingCell.getBufferedTableItem()).setBackground(gc.getBackground());
						((ListCell) ratingCell.getBufferedTableItem()).setBounds(dmRatingRect);
						invalidateAndRefresh(ratingCell);
					}
				}
			}

			if (!isHeader && ((TableCellCore) cell).isMouseOver()
					&& imgDelete != null) {
				gc.setAlpha(50);
				gc.drawImage(imgDelete, width - imgDelete.getBounds().width - 5, 2);
			}
		} finally {
			gc.dispose();
		}

		disposeExisting(cell, image);

		cell.setGraphic(new UISWTGraphicImpl(image));
	}

	/**
	 * @param entry
	 * @return
	 *
	 * @since 3.0.5.3
	 */
	private boolean canShowThumb(VuzeActivitiesEntry entry) {
		if (!entry.getShowThumb()) {
			return false;
		}

		return entry.getDownloadManger() != null || entry.getTorrent() != null
				|| entry.getImageBytes() != null;
	}

	private void disposeExisting(TableCell cell, Image exceptIfThisImage) {
		Graphic oldGraphic = cell.getGraphic();
		//log(cell, oldGraphic);
		if (oldGraphic instanceof UISWTGraphic) {
			Image oldImage = ((UISWTGraphic) oldGraphic).getImage();
			if (oldImage != null && !oldImage.isDisposed()
					&& oldImage != exceptIfThisImage) {
				//log(cell, "dispose");
				cell.setGraphic(null);
				oldImage.dispose();
			}
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellDisposeListener#dispose(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void dispose(TableCell cell) {
		disposeExisting(cell, null);
		try {
			TableCellImpl thumbCell = getThumbCell(cell);
			if (thumbCell != null) {
				thumbCell.dispose();
				setThumbCell(cell, null);
			}
		} catch (Exception e) {
			Debug.out(e);
		}
		try {
			TableCellImpl ratingCell = getRatingCell(cell);
			if (ratingCell != null) {
				ratingCell.dispose();
				setRatingCell(cell, null);
			}
		} catch (Exception e) {
			Debug.out(e);
		}
	}

	private void setThumbCell(TableCell cell, TableCellImpl thumbCell) {
		TableRow tableRow = cell.getTableRow();
		if (tableRow instanceof TableRowCore) {
			TableRowCore tableRowCore = (TableRowCore) tableRow;
			tableRowCore.setData("ThumbCell", thumbCell);
		}
	}

	private TableCellImpl getThumbCell(TableCell cell) {
		TableRow tableRow = cell.getTableRow();
		if (tableRow instanceof TableRowCore) {
			TableRowCore tableRowCore = (TableRowCore) tableRow;
			return (TableCellImpl) tableRowCore.getData("ThumbCell");
		}
		return null;
	}

	private boolean getIsMouseOverCell(String id, TableCell cell) {
		TableRow tableRow = cell.getTableRow();
		if (tableRow instanceof TableRowCore) {
			TableRowCore tableRowCore = (TableRowCore) tableRow;
			Object data = tableRowCore.getData("IsMouseOver" + id + "Cell");
			if (data instanceof Boolean) {
				return ((Boolean) data).booleanValue();
			}
		}
		return false;
	}

	private boolean setIsMouseOverCell(String id, TableCell cell, boolean b) {
		TableRow tableRow = cell.getTableRow();
		if (tableRow instanceof TableRowCore) {
			TableRowCore tableRowCore = (TableRowCore) tableRow;
			tableRowCore.setData("IsMouseOver" + id + "Cell", new Boolean(b));
		}
		return false;
	}

	private void setRatingCell(TableCell cell, TableCellImpl ratingCell) {
		TableRow tableRow = cell.getTableRow();
		if (tableRow instanceof TableRowCore) {
			TableRowCore tableRowCore = (TableRowCore) tableRow;
			tableRowCore.setData("RatingCell", ratingCell);
		}
	}

	private TableCellImpl getRatingCell(TableCell cell) {
		TableRow tableRow = cell.getTableRow();
		if (tableRow instanceof TableRowCore) {
			TableRowCore tableRowCore = (TableRowCore) tableRow;
			return (TableCellImpl) tableRowCore.getData("RatingCell");
		}
		return null;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellToolTipListener#cellHover(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void cellHover(TableCell cell) {
		TableCellImpl thumbCell = getThumbCell(cell);
		if (thumbCell != null) {
			if (getIsMouseOverCell("Thumb", cell)) {
				Object toolTip = thumbCell.getToolTip();
				if (toolTip != null) {
					cell.setToolTip(toolTip);
				}
			}
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellToolTipListener#cellHoverComplete(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void cellHoverComplete(TableCell cell) {
		TableCellImpl thumbCell = getThumbCell(cell);
		if (thumbCell != null) {
			if (getIsMouseOverCell("Thumb", cell)) {
				Object toolTip = thumbCell.getToolTip();
				if (toolTip != null) {
					cell.setToolTip(null);
				}
			}
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellMouseListener#cellMouseTrigger(org.gudy.azureus2.plugins.ui.tables.TableCellMouseEvent)
	public void cellMouseTrigger(TableCellMouseEvent event) {
		String tooltip = null;

		boolean invalidateAndRefresh = false;

		TableCellImpl thumbCell = getThumbCell(event.cell);
		TableCellImpl ratingCell = getRatingCell(event.cell);
		if (thumbCell != null || ratingCell != null) {
			Rectangle dmThumbRect = getDMImageRect(event.cell.getWidth(), event.cell.getHeight());
			Rectangle dmRatingRect = getDMRatingRect(event.cell.getWidth(),
					event.cell.getHeight());

			TableCellMouseEvent subCellEvent = new TableCellMouseEvent();
			subCellEvent.button = event.button;
			subCellEvent.data = event.data;
			subCellEvent.eventType = event.eventType;
			subCellEvent.row = event.row;

			boolean isMouseOverThumbCell = false;
			if (thumbCell != null) {
				isMouseOverThumbCell = dmThumbRect.contains(event.x, event.y);
				boolean wasMouseOverThumbCell = getIsMouseOverCell("Thumb", event.cell);
				//System.out.println("was=" + wasMouseOverThumbCell + ";is=" + isMouseOverThumbCell);

				if (wasMouseOverThumbCell != isMouseOverThumbCell) {
					invalidateAndRefresh = true;
					setIsMouseOverCell("Thumb", event.cell, isMouseOverThumbCell);
					subCellEvent.eventType = isMouseOverThumbCell
							? TableCellMouseEvent.EVENT_MOUSEENTER
							: TableCellMouseEvent.EVENT_MOUSEEXIT;
					subCellEvent.x = event.x - dmThumbRect.x - MARGIN_WIDTH;
					subCellEvent.y = event.y - dmThumbRect.y;
					subCellEvent.cell = thumbCell;

					TableColumn tc = thumbCell.getTableColumn();
					if (tc instanceof TableColumnCore) {
						((TableColumnCore) tc).invokeCellMouseListeners(subCellEvent);
					}
					if (thumbCell instanceof TableCellCore) {
						((TableCellCore) thumbCell).invokeMouseListeners(subCellEvent);
					}
					event.skipCoreFunctionality |= subCellEvent.skipCoreFunctionality;

					subCellEvent.eventType = TableCellMouseEvent.EVENT_MOUSEMOVE;
				}
			}

			boolean isMouseOverRatingCell = false;
			if (ratingCell != null) {
				isMouseOverRatingCell = dmRatingRect.contains(event.x, event.y);
				boolean wasMouseOverRatingCell = getIsMouseOverCell("Rating",
						event.cell);
				if (wasMouseOverRatingCell != isMouseOverRatingCell) {
					invalidateAndRefresh = true;
					setIsMouseOverCell("Rating", event.cell, isMouseOverRatingCell);
					subCellEvent.eventType = isMouseOverRatingCell
							? TableCellMouseEvent.EVENT_MOUSEENTER
							: TableCellMouseEvent.EVENT_MOUSEEXIT;
					subCellEvent.cell = ratingCell;
					subCellEvent.x = event.x - dmRatingRect.x - MARGIN_WIDTH;
					subCellEvent.y = event.y - dmRatingRect.y;

					TableColumn tc = ratingCell.getTableColumn();
					if (tc instanceof TableColumnCore) {
						((TableColumnCore) tc).invokeCellMouseListeners(subCellEvent);
					}
					if (ratingCell instanceof TableCellCore) {
						((TableCellCore) ratingCell).invokeMouseListeners(subCellEvent);
					}
					event.skipCoreFunctionality |= subCellEvent.skipCoreFunctionality;
				}
			}

			if (thumbCell != null && isMouseOverThumbCell) {
				subCellEvent.cell = thumbCell;
				subCellEvent.x = event.x - dmThumbRect.x - MARGIN_WIDTH;
				subCellEvent.y = event.y - dmThumbRect.y;

				//System.out.println(subCellEvent.x + ";" + subCellEvent.y);
				if (thumbCell instanceof TableCellCore) {
					TableRowCore row = ((TableCellCore) thumbCell).getTableRowCore();
					if (row != null) {
						row.invokeMouseListeners(subCellEvent);
					}
					((TableCellCore) thumbCell).invokeMouseListeners(subCellEvent);
					TableColumn tc = thumbCell.getTableColumn();
					if (tc instanceof TableColumnCore) {
						((TableColumnCore) tc).invokeCellMouseListeners(subCellEvent);
					}
				}
				event.skipCoreFunctionality |= subCellEvent.skipCoreFunctionality;
			}
			if (ratingCell != null && isMouseOverRatingCell) {
				subCellEvent.cell = ratingCell;
				subCellEvent.x = event.x - dmRatingRect.x - MARGIN_WIDTH;
				subCellEvent.y = event.y - dmRatingRect.y;

				if (ratingCell instanceof TableCellCore) {
					((TableCellCore) ratingCell).getTableRowCore().invokeMouseListeners(
							subCellEvent);
					((TableCellCore) ratingCell).invokeMouseListeners(subCellEvent);
				}
				TableColumn tc = ratingCell.getTableColumn();
				if (tc instanceof TableColumnCore) {
					((TableColumnCore) tc).invokeCellMouseListeners(subCellEvent);
				}
				event.skipCoreFunctionality |= subCellEvent.skipCoreFunctionality;
			}
		}

		
		Object ds = event.cell.getDataSource();
		if (ds instanceof VuzeActivitiesEntry) {
			VuzeActivitiesEntry entry = (VuzeActivitiesEntry) ds;
			if (entry.urlInfo != null) {
				//((ListView)((ListRow)event.cell.getTableRow()).getView()).getTableCellCursorOffset()
				//System.out.println(entry.urlInfo);
				URLInfo hitUrl = ((GCStringPrinter) entry.urlInfo).getHitUrl(event.x
						- MARGIN_WIDTH, event.y);
				int newCursor;
				if (hitUrl != null) {
					if (event.eventType == TableCellMouseEvent.EVENT_MOUSEUP) {
						if (!UrlFilter.getInstance().urlCanRPC(hitUrl.url)) {
							Utils.launch(hitUrl.url);
						} else {
							UIFunctionsSWT uif = UIFunctionsManagerSWT.getUIFunctionsSWT();
							if (uif != null) {
								String target = hitUrl.target == null
										? SkinConstants.VIEWID_BROWSER_BROWSE : hitUrl.target;
								uif.viewURL(hitUrl.url, target, 0, 0, false, false);
								return;
							}
						}
					}

					newCursor = SWT.CURSOR_HAND;
					if (UrlFilter.getInstance().urlCanRPC(hitUrl.url)) {
						tooltip = hitUrl.title;
					} else {
						tooltip = hitUrl.url;
					}
					//tooltip = hitUrl.url;
				} else {
					newCursor = SWT.CURSOR_ARROW;
				}

				int oldCursor = ((TableCellSWT) event.cell).getCursorID();
				if (oldCursor != newCursor) {
					invalidateAndRefresh = true;
					((TableCellSWT) event.cell).setCursorID(newCursor);
				}
			}

			boolean inHitArea = new Rectangle(0, 0, EVENT_INDENT, EVENT_INDENT).contains(
					event.x, event.y);

			boolean isHeader = VuzeActivitiesConstants.TYPEID_HEADER.equals(entry.getTypeID());
			if (!isHeader && inHitArea) {
				String ts = timeFormat.format(new Date(entry.getTimestamp()));
				tooltip = "Activity occurred on " + ts + ";" + entry.getID();
			}
		}

		Object o = event.cell.getToolTip();
		if ((o == null) | (o instanceof String)) {
			String oldTooltip = (String) o;
			if (!StringCompareUtils.equals(oldTooltip, tooltip)) {
				invalidateAndRefresh = true;
				event.cell.setToolTip(tooltip);
			}
		}

		if (invalidateAndRefresh) {
			invalidateAndRefresh(event.cell);
		}
	}

	private void invalidateAndRefresh(TableCell cell) {
		cell.invalidate();
		if (cell instanceof TableCellCore) {
			TableCellCore cellCore = (TableCellCore) cell;
			cellCore.refreshAsync();
		}
	}

	private Rectangle getDMImageRect(int cellWidth, int cellHeight) {
		//return new Rectangle(0, cellHeight - 50 - MARGIN_HEIGHT, 16, 50);
		//return new Rectangle(EVENT_INDENT, cellHeight - 50 - MARGIN_HEIGHT, 88, 50);
		return new Rectangle(cellWidth / 2 + 10, MARGIN_HEIGHT, 105, 80);
	}

	private Rectangle getDMRatingRect(int cellWidth, int cellHeight) {
		//return new Rectangle(cellWidth - 80 - 10, cellHeight - 42 - MARGIN_HEIGHT,
		//		80, 38);
		return new Rectangle(EVENT_INDENT, 4, 50, 32);
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellVisibilityListener#cellVisibilityChanged(org.gudy.azureus2.plugins.ui.tables.TableCell, int)
	public void cellVisibilityChanged(TableCell cell, int visibility) {
		TableCellImpl thumbCell = getThumbCell(cell);
		if (thumbCell != null) {
			thumbCell.invokeVisibilityListeners(visibility, true);
		}
		TableCellImpl ratingCell = getRatingCell(cell);
		if (ratingCell != null) {
			ratingCell.invokeVisibilityListeners(visibility, true);
		}
	}

	public int getSortBy() {
		return sortBy;
	}

	public void setSortBy(int sortBy) {
		this.sortBy = sortBy;
	}
}
