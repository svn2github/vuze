/**
 * Created on Sep 19, 2008
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
 
package com.aelitis.azureus.ui.swt.columns.torrent;


import org.eclipse.swt.graphics.Image;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTGraphicImpl;
import org.gudy.azureus2.ui.swt.views.table.CoreTableColumnSWT;

import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author TuxPaper
 * @created Sep 19, 2008
 *
 */
public class ColumnUnopened
	extends CoreTableColumnSWT
	implements TableCellAddedListener, TableCellRefreshListener,
	TableCellMouseListener
{
	public static final Class<?> DATASOURCE_TYPE = Download.class;

	public static final String COLUMN_ID = "unopened";

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] { CAT_CONTENT, CAT_ESSENTIAL });
		info.setProficiency(TableColumnInfo.PROFICIENCY_BEGINNER);
	}

	private static UISWTGraphicImpl graphicCheck;
	private static UISWTGraphicImpl graphicUnCheck;
	private static UISWTGraphicImpl[] graphicsProgress;

	private static int WIDTH = 38; // enough to fit title


	/**
	 * @param name
	 * @param tableID
	 */
	public ColumnUnopened(String tableID) {
		super(COLUMN_ID, tableID);
		
		if (graphicCheck == null) {
			Image img = ImageLoader.getInstance().getImage("image.unopened");
			graphicCheck = new UISWTGraphicImpl(img);
		}
		if (graphicUnCheck == null) {
			Image img = ImageLoader.getInstance().getImage("image.opened");
			graphicUnCheck = new UISWTGraphicImpl(img);
		}

		if (graphicsProgress == null) {
			
			Image[] imgs = ImageLoader.getInstance().getImages("image.sidebar.vitality.dl");
			graphicsProgress = new UISWTGraphicImpl[imgs.length];
			for(int i = 0 ; i < imgs.length ; i++) {
				graphicsProgress[i] = new UISWTGraphicImpl(imgs[i]);
			}
			
		}
		
		TableContextMenuItem menuItem = addContextMenuItem("label.toggle.new.marker");

		menuItem.addMultiListener(new MenuItemListener() {
			public void selected(MenuItem menu, Object target) {
				Object[] dataSources = (Object[])target;
				
				for ( Object _ds: dataSources ){
										
					DownloadManager dm = (DownloadManager)_ds;
					
					boolean x = PlatformTorrentUtils.getHasBeenOpened( dm );
					
					PlatformTorrentUtils.setHasBeenOpened(dm, !x );
				}
			}
		});
		
		initializeAsGraphic(WIDTH);
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener#cellAdded(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void cellAdded(TableCell cell) {
		cell.setMarginWidth(0);
		cell.setMarginHeight(0);
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener#refresh(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void refresh(TableCell cell) {
		DownloadManager dm = (DownloadManager) cell.getDataSource();
		if (dm == null) {
			return;
		}
		int sortVal;
		boolean complete = dm.getAssumedComplete();
		boolean hasBeenOpened = false;
		if (complete) {
			hasBeenOpened = PlatformTorrentUtils.getHasBeenOpened(dm);
			sortVal = hasBeenOpened ? 1 : 0;
		} else {
			sortVal = isSortAscending()?2:-1;
		}

		if (!cell.setSortValue(sortVal) && cell.isValid()) {
			if(complete) {
				return;
			}
		}
		if (!cell.isShown()) {
			return;
		}
		
		if (complete) {
			cell.setGraphic(hasBeenOpened ? graphicUnCheck : graphicCheck);
		} else {
			if(dm.getState() == DownloadManager.STATE_DOWNLOADING) {
				int i = TableCellRefresher.getRefreshIndex(1, graphicsProgress.length);
				cell.setGraphic(graphicsProgress[i]);
				TableCellRefresher.addCell(this, cell);
			} else {
				cell.setGraphic(null);
			}
			
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellMouseListener#cellMouseTrigger(org.gudy.azureus2.plugins.ui.tables.TableCellMouseEvent)
	public void cellMouseTrigger(TableCellMouseEvent event) {
		if (event.eventType == TableRowMouseEvent.EVENT_MOUSEUP && event.button == 1) {
			DownloadManager dm = (DownloadManager) event.cell.getDataSource();
			boolean complete = dm.getAssumedComplete();
			if(!complete) return;
			boolean hasBeenOpened = !PlatformTorrentUtils.getHasBeenOpened(dm);
			PlatformTorrentUtils.setHasBeenOpened(dm, hasBeenOpened);
			event.cell.setGraphic(hasBeenOpened ? graphicUnCheck : graphicCheck);
			event.cell.invalidate();
		}
	}
}
