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
    setRefreshInterval(4);
  }

  public void refresh(TableCell cell) {
    PEPiece piece = (PEPiece)cell.getDataSource();
    String[] writers = piece.getWriters();
    StringBuffer sb = new StringBuffer();

    Map map = new HashMap();
    for(int i = 0 ; i < writers.length ; i++) {
      if (writers[i] != null) {
      	String value = (String)map.get(writers[i]);
      	if (value == null)
      		value = Integer.toString(i);
      	else
      		value += "," + i;
      	map.put(writers[i], value);
      }
    }
    
    for (Iterator iter = map.keySet().iterator(); iter.hasNext();) {
			String writer =(String)iter.next();
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