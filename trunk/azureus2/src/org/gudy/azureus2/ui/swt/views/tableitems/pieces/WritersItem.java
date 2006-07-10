/*
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
 
package org.gudy.azureus2.ui.swt.views.tableitems.pieces;

import java.util.*;

import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

/**
 *
 * @author TuxPaper
 * @since 2.0.8.5
 */
public class WritersItem
       extends CoreTableColumn 
       implements TableCellRefreshListener
{
  /** Default Constructor */
  public WritersItem() {
    super("writers", ALIGN_LEAD, POSITION_INVISIBLE, 80, TableManager.TABLE_TORRENT_PIECES);
    setObfustication(true);
    setRefreshInterval(4);
  }

  public void refresh(TableCell cell) {
    PEPiece piece = (PEPiece)cell.getDataSource();
    String[] writers = piece.getWriters();
    StringBuffer sb = new StringBuffer();
    int writer_count = 0;

    Map map = new HashMap();
    String last_writer = null;
    int end_range = 0;
    for(int i = 0 ; i < writers.length ; i++) {
      if (last_writer == writers[i]) { // if the writer is the same as before
        if (writers[i] != null)        // and the block has been written
          end_range = i;               // then keep tracking the range
      } else {                         // otherwise the writer is different
        if (end_range != 0) {          // if we were tracking a range, end the range
          map.put(last_writer, (String)map.get(last_writer) + "-" + end_range);
          end_range = 0;               // and stop tracking it
        }

        if (writers[i] != null) {
          String value = (String)map.get(writers[i]);
          if (value == null) {
            value = Integer.toString(i);
            writers[writer_count++] = writers[i];
          } else
            value += "," + i;
          map.put(writers[i], value);
        }
      }
      last_writer = writers[i];
    }
    
    if (end_range != 0)
      map.put(last_writer, (String)map.get(last_writer) + "-" + end_range);

    for (int i = 0 ; i < writer_count ; i++) {
			String writer = writers[i];
			if (sb.length() != 0)
				sb.append(";");
			sb.append(writer);
			sb.append("[");
			sb.append((String)map.get(writer));
			sb.append("]");
		}
    
    String value = sb.toString();
    if( !cell.setSortValue( value ) && cell.isValid() ) {
      return;
    }
    
    cell.setText(value);
  }
}