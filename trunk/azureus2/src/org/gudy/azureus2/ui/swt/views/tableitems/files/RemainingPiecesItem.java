/*
 * Copyright (C) 2004 Aelitis SARL, All rights Reserved
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
 * AELITIS, SARL au capital de 30,000 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
 
package org.gudy.azureus2.ui.swt.views.tableitems.files;

import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.disk.DiskManagerPiece;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

/**
 *
 * @author TuxPaper
 * @since 2.0.8.5
 */
public class RemainingPiecesItem
       extends CoreTableColumn 
       implements TableCellRefreshListener
{
  /** Default Constructor */
  public RemainingPiecesItem() {
    super("remaining", ALIGN_TRAIL, POSITION_LAST, 60, TableManager.TABLE_TORRENT_FILES);
    setRefreshInterval(INTERVAL_LIVE);
  }

  public void refresh(TableCell cell) {
    DiskManagerFileInfo fileInfo = (DiskManagerFileInfo)cell.getDataSource();
    
    int remaining = 0;
    
    if( fileInfo != null ) {
      int start = fileInfo.getFirstPieceNumber();
      int end = start + fileInfo.getNbPieces();
      DiskManagerPiece[] pieces = fileInfo.getDiskManager().getPieces();
      for( int i = start; i < end; i++ ) {
        if( !pieces[ i ].getDone() )  remaining++;
      }
    }

    cell.setSortValue( remaining );
    cell.setText( "" + remaining );
  }
}
