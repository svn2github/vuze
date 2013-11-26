/**
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
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
 */
 
package com.aelitis.azureus.ui.swt.shells.opentorrent;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

import org.gudy.azureus2.core3.torrent.impl.TorrentOpenFileOptions;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;

import com.aelitis.azureus.ui.common.table.TableColumnCore;

public class TableColumnOTOF_Size
implements TableCellRefreshListener, TableColumnExtraInfoListener, TableCellSWTPaintListener
{
	public static final String COLUMN_ID = "size";
  
  /** Default Constructor */
  public TableColumnOTOF_Size(TableColumn column) {
  	column.initialize(TableColumn.ALIGN_TRAIL, TableColumn.POSITION_LAST, 80);
  	column.addListeners(this);
		if (column instanceof TableColumnCore) {
			((TableColumnCore) column).addCellOtherListener("SWTPaint", this);
		}
  }

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			TableColumn.CAT_CONTENT,
		});
		info.setProficiency(TableColumnInfo.PROFICIENCY_BEGINNER);
	}

  public void refresh(TableCell cell) {
  	Object ds = cell.getDataSource();
  	if (!(ds instanceof TorrentOpenFileOptions)) {
  		return;
  	}
  	TorrentOpenFileOptions tfi = (TorrentOpenFileOptions) ds;
  	cell.setSortValue(tfi.lSize);
  	cell.setText(DisplayFormatters.formatByteCountToKiBEtc(tfi.lSize));
  }

	public void cellPaint(GC gc, TableCellSWT cell) {
  	Object ds = cell.getDataSource();
  	if (!(ds instanceof TorrentOpenFileOptions)) {
  		return;
  	}
  	TorrentOpenFileOptions tfi = (TorrentOpenFileOptions) ds;
  	
  	float pct = tfi.lSize / (float) tfi.parent.getTorrent().getSize();

  	Rectangle bounds = cell.getBounds();
		
		bounds.width = (int) (bounds.width * pct);
		if (bounds.width > 2) {
			bounds.x++;
			bounds.y++;
			bounds.height -= 2;
			bounds.width -= 2;
  		gc.setBackground(gc.getForeground());
  		int alpha = gc.getAlpha();
  		gc.setAlpha(10);
  		gc.fillRectangle(bounds);
  		gc.setAlpha(alpha);
		}
	}
  
}
