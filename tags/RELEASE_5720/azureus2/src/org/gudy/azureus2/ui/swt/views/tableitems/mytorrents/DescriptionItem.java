/*
 * File    : CommentItem.java
 * Created : 14 Nov 2006
 * By      : Allan Crooks
 *
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
 
package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;

import org.eclipse.swt.SWT;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.ui.swt.TorrentUtil;
import org.gudy.azureus2.ui.swt.views.table.CoreTableColumnSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;

import com.aelitis.azureus.core.util.PlatformTorrentUtils;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author amc1
 */
public class DescriptionItem
       extends CoreTableColumnSWT 
       implements TableCellRefreshListener, TableCellMouseListener, TableCellAddedListener
{
	public static final Class DATASOURCE_TYPE = Download.class;

	public static final String COLUMN_ID = "description";

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] { CAT_CONTENT });
	}

  /** Default Constructor */
  public DescriptionItem(String sTableID) {
		super(DATASOURCE_TYPE, COLUMN_ID, ALIGN_LEAD, 150, sTableID);
		setRefreshInterval(INTERVAL_LIVE);
	}
  
  public void cellAdded(TableCell cell) {
	  if ( cell instanceof TableCellSWT ){
		  ((TableCellSWT)cell).setCursorID(SWT.CURSOR_HAND);
	  }
  }
  
  public void cellMouseTrigger(TableCellMouseEvent event) {
		DownloadManager dm = (DownloadManager) event.cell.getDataSource();
		if (dm == null) {return;}
		
		if (event.eventType != TableCellMouseEvent.EVENT_MOUSEUP) {return;}

		// Only activate on LMB.
		if (event.button != 1) {return;}
		event.skipCoreFunctionality = true;
		
		TorrentUtil.promptUserForDescription(new DownloadManager[]{dm});
		refresh(event.cell);
  }
  
  public void refresh(TableCell cell) {
	  if (cell.isDisposed()) {return;}
	  

	  DownloadManager dm = (DownloadManager)cell.getDataSource();
	  String desc = "";
	  if (dm != null) {
		  try{
			  desc = PlatformTorrentUtils.getContentDescription( dm.getTorrent());
			  
			  if ( desc == null ){
				  desc = "";
			  }
		  }catch( Throwable e ){
		  }
	  }
	  cell.setText( desc );
  }
}
