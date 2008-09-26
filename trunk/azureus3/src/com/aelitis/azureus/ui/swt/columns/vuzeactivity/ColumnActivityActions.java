/**
 * Created on Sep 25, 2008
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

import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter.URLInfo;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.activities.VuzeActivitiesConstants;
import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.activities.VuzeActivitiesEntryBuddyRequest;
import com.aelitis.azureus.core.messenger.PlatformMessage;
import com.aelitis.azureus.core.messenger.PlatformMessengerListener;
import com.aelitis.azureus.core.messenger.config.PlatformConfigMessenger;
import com.aelitis.azureus.core.messenger.config.PlatformRatingMessenger;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.skin.SWTSkinFactory;
import com.aelitis.azureus.ui.swt.skin.SWTSkinProperties;
import com.aelitis.azureus.ui.swt.utils.ColorCache;
import com.aelitis.azureus.ui.swt.utils.ImageLoaderFactory;
import com.aelitis.azureus.ui.swt.views.skin.TorrentListViewsUtils;
import com.aelitis.azureus.util.DataSourceUtils;
import com.aelitis.azureus.util.PlayUtils;
import com.aelitis.azureus.util.StringCompareUtils;

import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author TuxPaper
 * @created Sep 25, 2008
 *
 */
public class ColumnActivityActions
	extends CoreTableColumn
	implements TableCellSWTPaintListener, TableCellRefreshListener,
	TableCellMouseMoveListener
{
	public static final String COLUMN_ID = "activityActions";

	private Color colorLinkNormal;

	private Color colorLinkHover;

	private static Rectangle boundsRateMe;

	private boolean useButton = false;

	private boolean mouseIn = false;

	private boolean disabled = false;

	private Image imgRateMe;

	private Image imgRateMeButton;

	private Image imgRateMeButtonEnabled;

	private Image imgRateMeDisabled;

	private Image imgUp;

	private Image imgDown;

	private Image imgWait;
	
	private static Font font = null;

	/**
	 * @param name
	 * @param tableID
	 */
	public ColumnActivityActions(String tableID) {
		super(COLUMN_ID, tableID);
		initializeAsGraphic(POSITION_LAST, 150);

		SWTSkinProperties skinProperties = SWTSkinFactory.getInstance().getSkinProperties();
		colorLinkNormal = skinProperties.getColor("color.links.normal");
		colorLinkHover = skinProperties.getColor("color.links.hover");

		imgRateMe = ImageLoaderFactory.getInstance().getImage("icon.rateme");
		boundsRateMe = imgRateMe.getBounds();

		imgRateMeButton = ImageLoaderFactory.getInstance().getImage("icon.rateme-button");
		imgRateMeButtonEnabled = imgRateMe;

		imgRateMeDisabled = ImageLoaderFactory.getInstance().getImage(
				"icon.rateme-button-disabled");

		imgUp = ImageLoaderFactory.getInstance().getImage("icon.rate.up");

		imgDown = ImageLoaderFactory.getInstance().getImage("icon.rate.down");

		imgWait = ImageLoaderFactory.getInstance().getImage("icon.rate.wait");
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener#cellPaint(org.eclipse.swt.graphics.GC, org.gudy.azureus2.ui.swt.views.table.TableCellSWT)
	public void cellPaint(GC gc, TableCellSWT cell) {
		VuzeActivitiesEntry entry = (VuzeActivitiesEntry) cell.getDataSource();
		if (entry.getTypeID().equals(VuzeActivitiesConstants.TYPEID_RATING_REMINDER)) {
			cellPaintForRate(gc, cell);
			return;
		}

		String text = cell.getText();

		if (text != null && text.length() > 0) {
			if (font == null) {
				FontData[] fontData = gc.getFont().getFontData();
				fontData[0].setStyle(SWT.BOLD);
				font = new Font(gc.getDevice(), fontData);
			}
			gc.setFont(font);

			Rectangle bounds = getDrawBounds(cell);

			GCStringPrinter sp = new GCStringPrinter(gc, text, bounds, true, true,
					SWT.WRAP | SWT.CENTER);

			sp.calculateMetrics();

			if (sp.hasHitUrl()) {
				URLInfo[] hitUrlInfo = sp.getHitUrlInfo();
				for (int i = 0; i < hitUrlInfo.length; i++) {
					URLInfo info = hitUrlInfo[i];
					if (cell.getTableRow().isSelected()) {
						info.urlColor = ColorCache.getColor(gc.getDevice(), 200, 150, 50);
					} else {
						info.urlColor = colorLinkNormal;
					}
				}
				int[] mouseOfs = cell.getMouseOffset();
				if (mouseOfs != null) {
					URLInfo hitUrl = sp.getHitUrl(mouseOfs[0] + bounds.x, mouseOfs[1]
							+ bounds.y);
					if (hitUrl != null) {
						hitUrl.urlColor = colorLinkHover;
					}
				}
			}

			sp.printString();
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener#refresh(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void refresh(TableCell cell) {
		VuzeActivitiesEntry entry = (VuzeActivitiesEntry) cell.getDataSource();
		
		if (entry.getTypeID().equals(VuzeActivitiesConstants.TYPEID_RATING_REMINDER)) {
			DownloadManager dm = DataSourceUtils.getDM(entry);
			if (dm == null) {
				return;
			}
			int rating = PlatformTorrentUtils.getUserRating(dm.getTorrent());

			if (cell.setSortValue(rating)) {
				((TableCellSWT) cell).redraw();
			}
			
			return;
		}

		if (!cell.setSortValue(entry.getTypeID()) && cell.isValid()) {
			return;
		}

		if (entry instanceof VuzeActivitiesEntryBuddyRequest) {
			VuzeActivitiesEntryBuddyRequest br = (VuzeActivitiesEntryBuddyRequest) entry;
			String urlAccept = br.getUrlAccept();
			String text = "<A HREF=\"" + urlAccept + "\">Accept</A>";
			cell.setText(text);
		} else {
			DownloadManager dm = entry.getDownloadManger();
			boolean canPlay = PlayUtils.canPlayDS(entry);
			boolean canDL = dm == null && entry.getDownloadManger() == null
					&& (entry.getTorrent() != null || entry.getAssetHash() != null);
			boolean canRun = !canPlay && dm != null;
			if (canRun && dm != null && !dm.getAssumedComplete()) {
				canRun = false;
			}

			StringBuffer sb = new StringBuffer();
			if (canDL) {
				if (sb.length() > 0) {
					sb.append(" | ");
				}
				sb.append("<A HREF=\"download\">Download</A>");
			}

			if (canPlay) {
				if (sb.length() > 0) {
					sb.append(", ");
				}
				sb.append("<A HREF=\"play\">Play</A>");
			}

			if (canRun) {
				if (sb.length() > 0) {
					sb.append(", ");
				}
				sb.append("<A HREF=\"launch\">Launch</A>");
			}

			cell.setText(sb.toString());
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellMouseListener#cellMouseTrigger(org.gudy.azureus2.plugins.ui.tables.TableCellMouseEvent)
	public void cellMouseTrigger(TableCellMouseEvent event) {
		VuzeActivitiesEntry entry = (VuzeActivitiesEntry) event.cell.getDataSource();
		if (entry.getTypeID().equals(VuzeActivitiesConstants.TYPEID_RATING_REMINDER)) {
			cellMouseTriggerForRate(event);
			return;
		}

		String tooltip = null;
		boolean invalidateAndRefresh = false;

		Rectangle bounds = ((TableCellSWT) event.cell).getBounds();
		String text = event.cell.getText();

		GCStringPrinter sp = null;
		GC gc = new GC(Display.getDefault());
		try {
			sp = new GCStringPrinter(gc, text, bounds, true, true, SWT.WRAP
					| SWT.CENTER);
			sp.calculateMetrics();
		} catch (Exception e) {
			Debug.out(e);
		} finally {
			gc.dispose();
		}

		if (sp != null) {
			URLInfo hitUrl = sp.getHitUrl(event.x + bounds.x, event.y + bounds.y);
			int newCursor;
			if (hitUrl != null) {
				if (event.eventType == TableCellMouseEvent.EVENT_MOUSEUP) {
					if (hitUrl.url.equals("download")) {
						String referal = null;
						Object ds = event.cell.getDataSource();
						if (ds instanceof VuzeActivitiesEntry) {
							if (((VuzeActivitiesEntry) ds).isDRM()) {
								String hash = getHash(event.cell.getDataSource(), true);
								if (hash != null) {
									TorrentListViewsUtils.viewDetails(hash, "activity-dl");
								}
								return;
							}

							referal = "dashboardactivity-"
									+ ((VuzeActivitiesEntry) ds).getTypeID();
						}
						TorrentListViewsUtils.downloadDataSource(ds, false, referal);
						
					} else if (hitUrl.url.equals("play")) {
						String referal = null;
						Object ds = event.cell.getDataSource();
						if (ds instanceof VuzeActivitiesEntry) {
							if (((VuzeActivitiesEntry) ds).isDRM()
									&& ((VuzeActivitiesEntry) ds).getDownloadManger() == null) {
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
						
					} else if (hitUrl.url.equals("launch")) {
						// run via play or stream so we get the security warning
						Object ds = event.cell.getDataSource();
						TorrentListViewsUtils.playOrStreamDataSource(ds, null, "unknown");
						
					} else if (!PlatformConfigMessenger.urlCanRPC(hitUrl.url)) {
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
				if (PlatformConfigMessenger.urlCanRPC(hitUrl.url)) {
					tooltip = hitUrl.title;
				} else {
					tooltip = hitUrl.url;
				}
			} else {
				newCursor = SWT.CURSOR_ARROW;
			}

			int oldCursor = ((TableCellSWT) event.cell).getCursorID();
			if (oldCursor != newCursor) {
				invalidateAndRefresh = true;
				((TableCellSWT) event.cell).setCursorID(newCursor);
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
			event.cell.invalidate();
		}
	}

	boolean bMouseDowned = false;

	public void cellMouseTriggerForRate(final TableCellMouseEvent event) {
		if (disabled) {
			return;
		}
		DownloadManager dm = DataSourceUtils.getDM(event.cell.getDataSource());
		if (dm == null) {
			return;
		}
		TOTorrent torrent0 = dm.getTorrent();

		if (torrent0 == null) {
			return;
		}

		if (useButton) {
			if (event.eventType == TableCellMouseEvent.EVENT_MOUSEENTER) {
				mouseIn = true;
				refresh(event.cell);
			} else if (event.eventType == TableCellMouseEvent.EVENT_MOUSEEXIT) {
				mouseIn = false;
				refresh(event.cell);
			}
		}

		final TOTorrent torrent = torrent0;

		// middle button == refresh rate from platform
		if (event.eventType == TableCellMouseEvent.EVENT_MOUSEUP
				&& event.button == 2) {

			try {
				final String fHash = torrent.getHashWrapper().toBase32String();
				PlatformRatingMessenger.getUserRating(new String[] {
					PlatformRatingMessenger.RATE_TYPE_CONTENT
				}, new String[] {
					fHash
				}, 5000);
			} catch (TOTorrentException e) {
				Debug.out(e);
			}
			Utils.beep();
		}

		// only first button
		if (event.button != 1) {
			return;
		}

		// no rating if row isn't selected yet
		TableRow row = event.cell.getTableRow();
		if (row != null && !row.isSelected()) {
			return;
		}

		if (!PlatformTorrentUtils.isContent(torrent, true)) {
			return;
		}

		if (event.eventType == TableCellMouseEvent.EVENT_MOUSEDOWN) {
			bMouseDowned = true;
			return;
		}

		if (event.eventType == TableCellMouseEvent.EVENT_MOUSEUP && bMouseDowned) {
			Comparable sortValue = event.cell.getSortValue();
			if (sortValue == null || sortValue.equals(new Long(-1))) {
				// not set
				int cellWidth = event.cell.getWidth();
				int cellHeight = event.cell.getHeight();
				int x = event.x - ((cellWidth - boundsRateMe.width) / 2);
				int y = event.y - ((cellHeight - boundsRateMe.height) / 2);

				if (x >= 0 && y >= 0 && x < boundsRateMe.width
						&& y < boundsRateMe.height) {
					try {
						final int value = (x < (boundsRateMe.height - y + 1)) ? 1 : 0;
						refresh(event.cell);
						PlatformRatingMessenger.setUserRating(torrent, value, true, 0,
								new PlatformMessengerListener() {
									public void replyReceived(PlatformMessage message,
											String replyType, Map reply) {
										refresh(event.cell);
									}

									public void messageSent(PlatformMessage message) {
									}
								});
					} catch (Exception e) {
						Debug.out(e);
					}
				}
			} else {
				// remove setting
				try {
					final int oldValue = PlatformTorrentUtils.getUserRating(torrent);
					if (oldValue == -2) {
						return;
					}
					refresh(event.cell);
					PlatformRatingMessenger.setUserRating(torrent, -1, true, 0,
							new PlatformMessengerListener() {
								public void replyReceived(PlatformMessage message,
										String replyType, Map reply) {
									refresh(event.cell);
								}

								public void messageSent(PlatformMessage message) {
								}
							});
				} catch (Exception e) {
					Debug.out(e);
				}
			}
		}
		bMouseDowned = false;
	}

	public void cellPaintForRate(GC gc, TableCellSWT cell) {
		Object ds = cell.getDataSource();
		DownloadManager dm = DataSourceUtils.getDM(ds);
		if (dm == null) {
			return;
		}
		TOTorrent torrent = dm.getTorrent();

		if (torrent == null) {
			return;
		}
		if (!PlatformTorrentUtils.isContent(torrent, true)) {
			return;
		}

		int rating = PlatformTorrentUtils.getUserRating(torrent);

		Image img = null;
		switch (rating) {
			case -2: // waiting
				img = imgWait;
				break;

			case -1: // unrated
				img = (useButton && !mouseIn) || disabled ? imgRateMeButton
						: imgRateMe;
				break;

			case 0:
				img = imgDown;
				break;

			case 1:
				img = imgUp;
				break;

			default:
				img = null;
		}

		Rectangle drawBounds = getDrawBounds(cell);
		gc.drawImage(img, drawBounds.x + (drawBounds.width - boundsRateMe.width)
				/ 2, drawBounds.y + (drawBounds.height - boundsRateMe.height) / 2);
	}
	
	private Rectangle getDrawBounds(TableCellSWT cell) {
		Rectangle bounds = cell.getBounds();
		bounds.height -= 12;
		bounds.y += 6;
		bounds.x += 4;
		bounds.width -= 4;

		return bounds;
	}

	private String getHash(Object ds, boolean onlyOurs) {
		if (onlyOurs && !DataSourceUtils.isPlatformContent(ds)) {
			return null;
		}
		return DataSourceUtils.getHash(ds);
	}

}
