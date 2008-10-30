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
import com.aelitis.azureus.core.messenger.PlatformMessengerListener;
import com.aelitis.azureus.core.messenger.config.PlatformRatingMessenger;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.swt.utils.ImageLoaderFactory;

import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author TuxPaper
 * @created Jun 16, 2006
 *
 * TODO: Implement
 */
public class ColumnRateUpDown
	extends CoreTableColumn
	implements TableCellAddedListener, TableCellRefreshListener,
	TableCellMouseListener
{
	public static final String COLUMN_ID = "RateIt";
	
	private static UISWTGraphicImpl graphicRate;
	
	private static UISWTGraphicImpl graphicRateDown;
	
	private static UISWTGraphicImpl graphicRateUp;
	
	private static UISWTGraphicImpl graphicsWait[];
	
	private static Rectangle boundsRate;

	private static int width = 50;

	private boolean useButton = false;

	private boolean disabled = false;
	

	static {
		Image img;
		
		img = ImageLoaderFactory.getInstance().getImage("icon.rate.library");
		graphicRate = new UISWTGraphicImpl(img);
		
		img = ImageLoaderFactory.getInstance().getImage("icon.rate.library.down");
		graphicRateDown = new UISWTGraphicImpl(img);
		
		img = ImageLoaderFactory.getInstance().getImage("icon.rate.library.up");
		graphicRateUp = new UISWTGraphicImpl(img);
		
		boundsRate = img.getBounds();
		
		Image[] imgs = ImageLoaderFactory.getInstance().getImages("image.sidebar.vitality.dots");
		graphicsWait = new UISWTGraphicImpl[imgs.length];
		for(int i = 0 ; i < imgs.length  ;i++) {
			graphicsWait[i] =  new UISWTGraphicImpl(imgs[i]);
		}
		
				
	}

	/**
	 * 
	 */
	public ColumnRateUpDown(String sTableID) {
		super(COLUMN_ID, sTableID);
		initializeAsGraphic(width);
		setAlignment(ALIGN_CENTER);
		setWidthLimits(width, width);
	}

	public void cellAdded(TableCell cell) {
		cell.setMarginWidth(0);
		cell.setMarginHeight(0);
	}

	public void refresh(TableCell cell) {
		
		Object ds = cell.getDataSource();
		TOTorrent torrent = null;
		if (ds instanceof TOTorrent) {
			torrent = (TOTorrent) ds;
		} else if (ds instanceof DownloadManager) {
			torrent = ((DownloadManager) ds).getTorrent();
			if (!PlatformTorrentUtils.isContentProgressive(torrent)
					&& !((DownloadManager) ds).isDownloadComplete(false)) {
				return;
			}
		}

		if (torrent == null) {
			return;
		}
		if (!PlatformTorrentUtils.isContent(torrent, true)) {
			return;
		}

		int rating = PlatformTorrentUtils.getUserRating(torrent);

		if (!cell.setSortValue(rating) && cell.isValid()) {
			if(rating != -2) {
				return;
			}
		}
		
		
		
		if (!cell.isShown()) {
			return;
		}
		
		UISWTGraphic graphic;
		switch (rating) {
			case -2: // waiting
				int i = TableCellRefresher.getRefreshIndex(1, graphicsWait.length);
				graphic = graphicsWait[i];
				TableCellRefresher.addCell(this,cell);
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

		cell.setGraphic(graphic);
	}

	TableRow previousSelection = null;

	public void cellMouseTrigger(final TableCellMouseEvent event) {
		if (disabled) {
			return;
		}
		
		Object ds = event.cell.getDataSource();
		TOTorrent torrent0 = null;
		if (ds instanceof TOTorrent) {
			torrent0 = (TOTorrent) ds;
		} else if (ds instanceof DownloadManager) {
			torrent0 = ((DownloadManager) ds).getTorrent();
			if (!PlatformTorrentUtils.isContentProgressive(torrent0)
					&& !((DownloadManager) ds).isDownloadComplete(false)) {
				return;
			}
		}

		if (torrent0 == null) {
			return;
		}

		final TOTorrent torrent = torrent0;

		// only first button
		if (event.button != 1) {
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
					&& y < boundsRate.height &&  (graphicRate.equals(currentGraphic) || graphicRateUp.equals(currentGraphic) || graphicRateDown.equals(currentGraphic)) ) {
				//The event is within the graphic, are we on a non-transparent pixel ?
				int alpha = graphicRate.getImage().getImageData().getAlpha(x,y);
				if(alpha > 0) {
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

	public boolean useButton() {
		return useButton;
	}

	public void setUseButton(boolean useButton) {
		this.useButton = useButton;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}
}
