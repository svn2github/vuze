/*
 * File    : SnubbedItem.java
 * Created : 24 nov. 2003
 * By      : Olivier
 *
 * Azureus - a Java Bittorrent client
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
 */
 
package org.gudy.azureus2.ui.swt.views.tableitems.peers;

import org.gudy.azureus2.ui.swt.mainwindow.Colors;

import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.ui.swt.views.table.TableCellCore;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

/**
 *
 * @author Olivier
 * @author TuxPaper (2004/Apr/19: modified to TableCellAdapter)
 */
public class SnubbedItem
       extends CoreTableColumn 
       implements TableCellRefreshListener
{
  /** Default Constructor */
  public SnubbedItem() {
    super("S", ALIGN_CENTER, POSITION_INVISIBLE, 20, TableManager.TABLE_TORRENT_PEERS);
    setRefreshInterval(INTERVAL_LIVE);
  }

  public void refresh(TableCell cell) {
    PEPeer peer = (PEPeer)cell.getDataSource();
    long value = (peer == null) ? 0 : (peer.isSnubbed() ? 1 : 0);

    if (!cell.setSortValue(value) && cell.isValid())
      return;

    if (cell.setText((value == 1) ? "*" : "")) {
      ((TableCellCore)cell).getTableRowCore().setForeground((value == 1) ? Colors.grey 
                                                                     : null);
    }
  }
}
