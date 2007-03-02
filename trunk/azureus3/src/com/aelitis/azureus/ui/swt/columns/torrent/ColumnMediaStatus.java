/**
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.ui.swt.columns.torrent;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;

/**
 * @author TuxPaper
 * @created Jan 17, 2007
 *
 */
public class ColumnMediaStatus
	extends CoreTableColumn
	implements TableCellRefreshListener
{

	public static String COLUMN_ID = "MediaStatus";

	/**
	 * 
	 */
	public ColumnMediaStatus(String sTableID) {
		super(COLUMN_ID, sTableID);
		setWidth(120);
	}

	public void refresh(TableCell cell) {
		String name = null;
		DownloadManager dm = (DownloadManager) cell.getDataSource();

		// xxx do me

		if (name == null) {
			name = "";
		}

		if (!cell.setSortValue(name) && cell.isValid()) {
			return;
		}

		if (!cell.isShown()) {
			return;
		}
	}
}
