/*
 * File    : CommentItem.java
 * Created : 14 Nov 2006
 * By      : Allan Crooks
 *
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
 
package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.tracker.host.*;

import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;
import org.gudy.azureus2.ui.swt.views.table.TableCellCore;

import com.aelitis.azureus.core.AzureusCoreFactory;

/**
 *
 * @author Olivier
 * @author TuxPaper (2004/Apr/17: modified to TableCellAdapter)
 */
public class CommentIconItem
       extends CoreTableColumn 
       implements TableCellRefreshListener, TableCellMouseListener
{
  /** Default Constructor */
  public CommentIconItem(String sTableID) {
	super("commenticon", CommentIconItem.POSITION_LAST, 300, sTableID);
	setRefreshInterval(INTERVAL_LIVE);
    initializeAsGraphic(POSITION_LAST, 20);
  }
  
  public void cellMouseTrigger(TableCellMouseEvent event) {
		DownloadManager dm = (DownloadManager) event.cell.getDataSource();
		if (dm == null) {return;}
		
		if (event.eventType != TableCellMouseEvent.EVENT_MOUSEUP) {return;}

		// Only activate on LMB.
		if (event.button != 1) {return;}
		event.skipCoreFunctionality = true;
		
		CommentItem.openEditCommentWindow(dm);
		refresh(event.cell);
  }
  
  public void refresh(TableCell cell) {
	  if (cell.isDisposed()) {return;}
	  
	  DownloadManager dm = (DownloadManager)cell.getDataSource();
	  String comment = null;
	  if (dm != null) {
		  comment = dm.getDownloadState().getUserComment();
		  if (comment!=null && comment.length()==0) {comment = null;}
	  }
	  
	  if (comment == null) {
		  ((TableCellCore)cell).setGraphic(ImageRepository.getImage(null));
		  cell.setToolTip(null);
		  cell.setSortValue(0);
	  }
	  else {
		  ((TableCellCore)cell).setGraphic(ImageRepository.getImage("comment"));
		  cell.setToolTip(comment);
		  cell.setSortValue(1);
	  }
	  
  }

}
