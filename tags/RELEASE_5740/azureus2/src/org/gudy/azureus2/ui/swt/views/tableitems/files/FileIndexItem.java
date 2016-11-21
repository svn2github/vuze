/*
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
 
package org.gudy.azureus2.ui.swt.views.tableitems.files;

import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.CoreTableColumnSWT;

import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;



public class FileIndexItem
       extends CoreTableColumnSWT 
       implements TableCellRefreshListener
{
  
  
  /** Default Constructor */
  public FileIndexItem() {
    super("torrentfileindex", ALIGN_TRAIL, POSITION_INVISIBLE, 40, TableManager.TABLE_TORRENT_FILES);
  }

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			CAT_CONTENT,
		});
		info.setProficiency(TableColumnInfo.PROFICIENCY_INTERMEDIATE);
	}

  public void refresh(TableCell cell) {
    DiskManagerFileInfo fileInfo = (DiskManagerFileInfo)cell.getDataSource();
    int index = fileInfo==null?-1:fileInfo.getIndex();
    
    if ( cell.setSortValue( index )){
    	cell.setText( index==-1?"":String.valueOf(index));
    }
  }
}
