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
import org.gudy.azureus2.ui.swt.plugins.UISWTGraphic;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTGraphicImpl;
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
import com.aelitis.azureus.core.messenger.config.PlatformRatingMessenger;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
import com.aelitis.azureus.ui.swt.skin.SWTSkinFactory;
import com.aelitis.azureus.ui.swt.skin.SWTSkinProperties;
import com.aelitis.azureus.ui.swt.views.skin.TorrentListViewsUtils;
import com.aelitis.azureus.util.*;

import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author TuxPaper
 * @created Sep 25, 2008
 *
 */
public class ColumnActivityActions
	extends CoreTableColumn
	implements TableCellSWTPaintListener, TableCellRefreshListener,
	TableCellMouseMoveListener, TableCellAddedListener
{
	public static final String COLUMN_ID = "activityActions";

	private Color colorLinkNormal;

	private Color colorLinkHover;

	private boolean useButton = false;

	private boolean mouseIn = false;

	private boolean disabled = false;

	private static UISWTGraphicImpl graphicRate;
	
	private static UISWTGraphicImpl graphicRateDown;
	
	private static UISWTGraphicImpl graphicRateUp;
	
	private static UISWTGraphicImpl graphicsWait[];
	
	private static Rectangle boundsRate;
	
	private static Font font = null;

	/**
	 * @param name
	 * @param tableID
	 */
	public ColumnActivityActions(String tableID) {
		super(COLUMN_ID, tableID);
		initializeAsGraphic(150);

		SWTSkinProperties skinProperties = SWTSkinFactory.getInstance().getSkinProperties();
		colorLinkNormal = skinProperties.getColor("color.links.normal");
		colorLinkHover = skinProperties.getColor("color.links.hover");

		Image img;
		
		img = ImageLoader.getInstance().getImage("icon.rate.library");
		graphicRate = new UISWTGraphicImpl(img);
		
		img = ImageLoader.getInstance().getImage("icon.rate.library.down");
		graphicRateDown = new UISWTGraphicImpl(img);
		
		img = ImageLoader.getInstance().getImage("icon.rate.library.up");
		graphicRateUp = new UISWTGraphicImpl(img);
		
		boundsRate = img.getBounds();
		
		Image[] imgs = ImageLoader.getInstance().getImages("image.sidebar.vitality.dots");
		graphicsWait = new UISWTGraphicImpl[imgs.length];
		for(int i = 0 ; i < imgs.length  ;i++) {
			graphicsWait[i] =  new UISWTGraphicImpl(imgs[i]);
		}
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
						// handle fake row when showing in column editor
					
					info.urlUnderline = cell.getTableRow() == null || cell.getTableRow().isSelected();
					if (info.urlUnderline) {
						info.urlColor = null;
					} else {
						info.urlColor = colorLinkNormal;
					}
				}
				int[] mouseOfs = cell.getMouseOffset();
				if (mouseOfs != null) {
					Rectangle realBounds = cell.getBounds();
					URLInfo hitUrl = sp.getHitUrl(mouseOfs[0] + realBounds.x, mouseOfs[1]
							+ realBounds.y);
					if (hitUrl != null) {
						hitUrl.urlColor = colorLinkHover;
					}
				}
			}

			sp.printString();
		}
	}
	
	// @see org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener#cellAdded(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void cellAdded(TableCell cell) {
		cell.setMarginHeight(0);
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener#refresh(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void refresh(TableCell cell) {
		VuzeActivitiesEntry entry = (VuzeActivitiesEntry) cell.getDataSource();
		
		if(entry == null) return;
		
		if (VuzeActivitiesConstants.TYPEID_RATING_REMINDER.equals(entry.getTypeID()) ) {
			TOTorrent torrent = DataSourceUtils.getTorrent(entry);
			if (torrent == null) {
				return;
			}
			int rating = PlatformTorrentUtils.getUserRating(torrent);

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
			String text = "<A HREF=\"" + urlAccept + "\" TARGET=\"browse\">Accept</A>";
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
					sb.append(" | ");
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
			if (font != null) {
				gc.setFont(font);
			}
			Rectangle drawBounds = getDrawBounds((TableCellSWT) event.cell);
			sp = new GCStringPrinter(gc, text, drawBounds, true, true, SWT.WRAP
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
								if (DataSourceUtils.isPlatformContent(ds)) {
									TorrentListViewsUtils.viewDetailsFromDS(
											event.cell.getDataSource(), "activity-dl");
								}
								return;
							}

							referal = ConstantsV3.DL_REFERAL_DASHACTIVITY + "-"
									+ ((VuzeActivitiesEntry) ds).getTypeID();
						}
						TorrentListViewsUtils.downloadDataSource(ds, false, referal);
						
					} else if (hitUrl.url.equals("play")) {
						String referal = null;
						Object ds = event.cell.getDataSource();
						if (ds instanceof VuzeActivitiesEntry) {
							if (((VuzeActivitiesEntry) ds).isDRM()
									&& ((VuzeActivitiesEntry) ds).getDownloadManger() == null) {
								if (DataSourceUtils.isPlatformContent(ds)) {
									TorrentListViewsUtils.viewDetailsFromDS(
											event.cell.getDataSource(), "thumb");
								}
								return;
							}
							referal = ConstantsV3.DL_REFERAL_PLAYDASHACTIVITY + "-"
									+ ((VuzeActivitiesEntry) ds).getTypeID();
						}
						TorrentListViewsUtils.playOrStreamDataSource(ds, null, referal);
						
					} else if (hitUrl.url.equals("launch")) {
						// run via play or stream so we get the security warning
						Object ds = event.cell.getDataSource();
						TorrentListViewsUtils.playOrStreamDataSource(ds, null,
								ConstantsV3.DL_REFERAL_LAUNCH);
						
					} else if (!UrlFilter.getInstance().urlCanRPC(hitUrl.url)) {
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
				Object ds = event.cell.getDataSource();

				newCursor = SWT.CURSOR_HAND;
				if (UrlFilter.getInstance().urlCanRPC(hitUrl.url)) {
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
		if ((o == null) || (o instanceof String)) {
			String oldTooltip = (String) o;
			if (!StringCompareUtils.equals(oldTooltip, tooltip)) {
				invalidateAndRefresh = true;
				event.cell.setToolTip(tooltip);
			}
		}

		if (invalidateAndRefresh) {
			event.cell.invalidate();
			((TableCellSWT)event.cell).redraw();
		}
	}

	boolean bMouseDowned = false;

	public void cellMouseTriggerForRate(final TableCellMouseEvent event) {
		if (disabled) {
			return;
		}
		TOTorrent torrent0 = DataSourceUtils.getTorrent(event.cell.getDataSource());

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

		if (event.eventType == TableCellMouseEvent.EVENT_MOUSEUP ) {

			//By default, let's cancel the setting
			boolean cancel = true;

			// Are we in the graphics area? (and not canceling)
			int cellWidth = event.cell.getWidth();
			int cellHeight = event.cell.getHeight();
			int x = event.x - ((cellWidth - boundsRate.width) / 2);
			int y = event.y - ((cellHeight - boundsRate.height) / 2);

			Graphic currentGraphic = event.cell.getGraphic();
			
			if (x >= 0 && y >= 0 && x < boundsRate.width
					&& y < boundsRate.height ) {
				
				int middle = boundsRate.width / 2 + 2;
				boolean hit = x < middle - 2 || x > middle + 2;
				//The event is within the graphic, are we on a non-transparent pixel ?
				//boolean hit = graphicRate.getImage().getImageData().getAlpha(x,y) > 0;
				if(hit) {
					try {
						cancel = false;
						final int value = (x < (boundsRate.width / 2)) ? 0 : 1;
						int previousValue = PlatformTorrentUtils.getUserRating(torrent);
						//Changing the value
						if(value != previousValue) {
							
							PlatformRatingMessenger.setUserRating(torrent, value, true, 0,
									new PlatformMessengerListener() {
										public void replyReceived(PlatformMessage message,
												String replyType, Map reply) {
											refresh(event.cell);
										}
		
										public void messageSent(PlatformMessage message) {
										}
									});
							refresh(event.cell);
						}
					} catch (Exception e) {
						Debug.out(e);
					}
				}
			} else {
				cancel = false;
			}
			
			 if(cancel) {
				// remove setting
				try {
					final int oldValue = PlatformTorrentUtils.getUserRating(torrent);
					if (oldValue == -2 || oldValue == -1) {
						return;
					}
					PlatformRatingMessenger.setUserRating(torrent, -1, true, 0,
							new PlatformMessengerListener() {
								public void replyReceived(PlatformMessage message,
										String replyType, Map reply) {
									refresh(event.cell);
								}

								public void messageSent(PlatformMessage message) {
								}
							});
					refresh(event.cell);
				} catch (Exception e) {
					Debug.out(e);
				}
			}
		}
	}

	public void cellPaintForRate(GC gc, TableCellSWT cell) {

		Object ds = cell.getDataSource();
		
		TOTorrent torrent = DataSourceUtils.getTorrent(ds);

		if (torrent == null) {
			return;
		}
		if (!PlatformTorrentUtils.isContent(torrent, true)) {
			return;
		}

		int rating = PlatformTorrentUtils.getUserRating(torrent);

		/*if (!cell.setSortValue(rating) && cell.isValid()) {
			if(rating != -2) {
				return;
			}
		}*/
		
		
		
		if (!cell.isShown()) {
			return;
		}
		
		UISWTGraphic graphic;
		switch (rating) {
			case -2: // waiting
				graphic = graphicsWait[0];
				break;

			case -1: // unrated
				graphic = graphicRate;
				break;

			case 0:
				graphic = graphicRateDown;
				break;

			case 1:
				graphic = graphicRateUp;
				break;

			default:
				graphic = null;
		}

		if(graphic != null) {
			Rectangle drawBounds = getDrawBounds(cell);
			gc.drawImage(graphic.getImage(), drawBounds.x + (drawBounds.width - boundsRate.width)
					/ 2, drawBounds.y + (drawBounds.height - boundsRate.height) / 2);
		}
	}
	
	private Rectangle getDrawBounds(TableCellSWT cell) {
		Rectangle bounds = cell.getBounds();
		bounds.height -= 12;
		bounds.y += 6;
		bounds.x += 4;
		bounds.width -= 4;

		return bounds;
	}
}
