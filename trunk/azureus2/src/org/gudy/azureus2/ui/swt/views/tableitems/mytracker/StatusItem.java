/*
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
 
package org.gudy.azureus2.ui.swt.views.tableitems.mytracker;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.tracker.host.*;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

/**
 *
 * @author TuxPaper
 * @since 2.0.8.5
 */
public class StatusItem
       extends CoreTableColumn 
       implements TableCellRefreshListener
{
  /** Default Constructor */
  public StatusItem() {
    super("status", POSITION_LAST, 60, TableManager.TABLE_MYTRACKER);
    setRefreshInterval(INTERVAL_LIVE);
  }


  public void refresh(TableCell cell) {
    String sText;
    TRHostTorrent item = (TRHostTorrent)cell.getDataSource();
    if (item == null) {
      sText = "";
    } else {
  		int	iStatus = item.getStatus();
  		String status = null;
  		
  		if (iStatus == TRHostTorrent.TS_STARTED) {
  			status = "started";
  		} else if (iStatus == TRHostTorrent.TS_STOPPED) {
  			status = "stopped";
  		} else if (iStatus == TRHostTorrent.TS_FAILED) {
  			status = "failed";
  		} else if (iStatus == TRHostTorrent.TS_PUBLISHED) {
  			status = "published";
  		}

      sText = (status == null) ? "?" 
                               : MessageText.getString("MyTrackerView.status." +
                                                       status);
    }

    cell.setText(sText);
  }
}
