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

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTGraphic;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTGraphicImpl;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.core.messenger.PlatformMessage;
import com.aelitis.azureus.core.messenger.PlatformMessenger;
import com.aelitis.azureus.core.messenger.PlatformMessengerListener;
import com.aelitis.azureus.core.messenger.config.PlatformRatingMessenger;
import com.aelitis.azureus.core.messenger.config.PlatformRatingMessenger.GetRatingReplyListener;
import com.aelitis.azureus.core.torrent.GlobalRatingUtils;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.swt.utils.ImageLoaderFactory;

import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author TuxPaper
 * @created Jun 16, 2006
 *
 * TODO: Implement
 */
public class ColumnRateUpDown
	extends CoreTableColumn
	implements TableCellAddedListener
{
	public static String COLUMN_ID = "RateIt";

	private static UISWTGraphicImpl graphicRateMe;

	private static UISWTGraphicImpl graphicUp;

	private static UISWTGraphicImpl graphicDown;

	private static UISWTGraphicImpl graphicWait;

	private static Rectangle boundsRateMe;

	private static int width;

	static {
		Image img = ImageLoaderFactory.getInstance().getImage("icon.rateme");
		graphicRateMe = new UISWTGraphicImpl(img);
		boundsRateMe = img.getBounds();
		width = boundsRateMe.width;

		img = ImageLoaderFactory.getInstance().getImage("icon.rate.up");
		graphicUp = new UISWTGraphicImpl(img);
		width = Math.max(width, img.getBounds().width);

		img = ImageLoaderFactory.getInstance().getImage("icon.rate.down");
		graphicDown = new UISWTGraphicImpl(img);
		width = Math.max(width, img.getBounds().width);

		img = ImageLoaderFactory.getInstance().getImage("icon.rate.wait");
		graphicWait = new UISWTGraphicImpl(img);
		width = Math.max(width, img.getBounds().width);
	}

	/**
	 * 
	 */
	public ColumnRateUpDown(String sTableID) {
		super(COLUMN_ID, sTableID);
		initializeAsGraphic(POSITION_LAST, width);
		setAlignment(ALIGN_CENTER);
		setWidthLimits(width, width);
	}

	public void cellAdded(TableCell cell) {
		new Cell(cell);
	}

	private class Cell
		implements TableCellRefreshListener, TableCellMouseListener
	{
		public Cell(TableCell cell) {
			cell.addListeners(this);
			cell.setMarginWidth(0);
			cell.setMarginHeight(0);
		}

		public void refresh(TableCell cell) {
			DownloadManager dm = (DownloadManager) cell.getDataSource();
			if (dm == null) {
				return;
			}
			if (!PlatformTorrentUtils.isContent(dm.getTorrent(), true)) {
				return;
			}
			if (!dm.isDownloadComplete(false)) {
				return;
			}

			int rating = PlatformTorrentUtils.getUserRating(dm.getTorrent());

			if (!cell.setSortValue(rating) && cell.isValid()) {
				return;
			}
			if (!cell.isShown()) {
				return;
			}

			UISWTGraphic graphic;
			switch (rating) {
				case -2: // waiting
					graphic = graphicWait;
					break;

				case -1: // unrated
					graphic = graphicRateMe;
					break;

				case 0:
					graphic = graphicDown;
					break;

				case 1:
					graphic = graphicUp;
					break;

				default:
					graphic = null;
			}

			cell.setGraphic(graphic);
		}

		boolean bMouseDowned = false;

		public void cellMouseTrigger(final TableCellMouseEvent event) {
			// middle button == refresh rate from platform
			if (event.eventType == TableCellMouseEvent.EVENT_MOUSEUP
					&& event.button == 2) {
				DownloadManager dm = (DownloadManager) event.cell.getDataSource();
				if (dm == null) {
					return;
				}

				final TOTorrent torrent = dm.getTorrent();
				try {
					final String fHash = torrent.getHashWrapper().toBase32String();
					PlatformRatingMessenger.getUserRating(new String[] {
						PlatformRatingMessenger.RATE_TYPE_CONTENT
					}, new String[] {
						fHash
					}, 5000, new GetRatingReplyListener() {
						public void replyReceived(String replyType,
								PlatformRatingMessenger.GetRatingReply reply) {
							if (replyType.equals(PlatformMessenger.REPLY_RESULT)) {
								long rating = reply.getRatingValue(fHash,
										PlatformRatingMessenger.RATE_TYPE_CONTENT);
								if (rating >= -1) {
									PlatformTorrentUtils.setUserRating(torrent, (int) rating);
									refresh(event.cell);
								}
							}

						}

						public void messageSent() {
						}
					});
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
			if (!event.cell.getTableRow().isSelected()) {
				return;
			}

			DownloadManager dm = (DownloadManager) event.cell.getDataSource();
			if (dm == null) {
				return;
			}

			if (!PlatformTorrentUtils.isContent(dm.getTorrent(), true)) {
				return;
			}

			if (!dm.isDownloadComplete(false)) {
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
							final TOTorrent torrent = dm.getTorrent();
							final String hash = torrent.getHashWrapper().toBase32String();
							final int value = (x < (boundsRateMe.height - y + 1)) ? 1 : 0;
							PlatformTorrentUtils.setUserRating(torrent, -2);
							refresh(event.cell);
							PlatformRatingMessenger.setUserRating(hash, value, 0,
									new PlatformMessengerListener() {
										public void replyReceived(PlatformMessage message,
												String replyType, Map reply) {
											if (PlatformRatingMessenger.ratingSucceeded(reply)) {
												PlatformTorrentUtils.setUserRating(torrent, value);
												GlobalRatingUtils.updateFromPlatform(torrent, 2000);
											} else {
												PlatformTorrentUtils.setUserRating(torrent, -1);
											}
											refresh(event.cell);
										}

										public void messageSent(PlatformMessage message) {
										}
									});
						} catch (TOTorrentException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				} else {
					// remove setting
					try {
						final TOTorrent torrent = dm.getTorrent();
						final String hash = torrent.getHashWrapper().toBase32String();
						final int oldValue = PlatformTorrentUtils.getUserRating(torrent);
						if (oldValue == -2) {
							return;
						}
						PlatformTorrentUtils.setUserRating(torrent, -2);
						refresh(event.cell);
						PlatformRatingMessenger.setUserRating(hash, -1, 0,
								new PlatformMessengerListener() {
									public void replyReceived(PlatformMessage message,
											String replyType, Map reply) {
										if (PlatformRatingMessenger.ratingSucceeded(reply)) {
											PlatformTorrentUtils.setUserRating(torrent, -1);
											GlobalRatingUtils.updateFromPlatform(torrent, 2000);
										} else {
											PlatformTorrentUtils.setUserRating(torrent,
													oldValue == -2 ? -1 : oldValue);
										}
										refresh(event.cell);
									}

									public void messageSent(PlatformMessage message) {
									}
								});
					} catch (TOTorrentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			bMouseDowned = false;
		}
	}
}
