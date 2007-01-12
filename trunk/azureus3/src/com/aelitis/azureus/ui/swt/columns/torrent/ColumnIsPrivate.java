/*
 * Created on Jun 29, 2006 10:15:15 PM
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

import org.eclipse.swt.graphics.Image;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTGraphicImpl;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.ui.swt.utils.ImageLoaderFactory;

import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;

/**
 * @author TuxPaper
 * @created Jun 29, 2006
 *
 */
public class ColumnIsPrivate extends CoreTableColumn implements
		TableCellAddedListener
{

	private static UISWTGraphicImpl graphicCheck;

	private static int width;

	static {
		Image img = ImageLoaderFactory.getInstance().getImage("image.check");
		width = img.getBounds().width;
		graphicCheck = new UISWTGraphicImpl(img);
	}

	/**
	 * 
	 */
	public ColumnIsPrivate(String sTableID) {
		super("IsPrivate", sTableID);
		initializeAsGraphic(POSITION_LAST, width);
		setAlignment(ALIGN_CENTER);
	}

	public void cellAdded(TableCell cell) {
		new Cell(cell);
	}

	private class Cell implements TableCellRefreshListener
	{

		public Cell(TableCell cell) {
			cell.addListeners(this);
			cell.setMarginWidth(0);
			cell.setMarginHeight(0);
		}

		public void refresh(TableCell cell) {
			DownloadManager dm = (DownloadManager) cell.getDataSource();
			boolean bPrivate = dm.getTorrent().getPrivate();
			int sortVal = bPrivate ? 0 : 1;

			if (!cell.setSortValue(sortVal) && cell.isValid()) {
				return;
			}
			if (!cell.isShown()) {
				return;
			}

			cell.setGraphic(bPrivate ? graphicCheck : null);
		}
	}

}
