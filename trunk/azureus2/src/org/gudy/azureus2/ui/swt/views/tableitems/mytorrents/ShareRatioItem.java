/*
 * File    : ShareRatioItem.java
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
 
package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;

import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;


/**
 *
 * @author Olivier
 * @author TuxPaper (2004/Apr/17: modified to TableCellAdapter)
 */
public class ShareRatioItem
       extends CoreTableColumn 
       implements TableCellRefreshListener
{
  /** Default Constructor */
  public ShareRatioItem(String sTableID) {
    super("shareRatio", ALIGN_TRAIL, POSITION_LAST, 70, sTableID);
    setRefreshInterval(INTERVAL_LIVE);

    if (sTableID.equals(TableManager.TABLE_MYTORRENTS_COMPLETE))
      setPosition(POSITION_LAST);
    else
      setPosition(POSITION_INVISIBLE);
  }

  public void refresh(TableCell cell) {
    String shareRatio = "";

    DownloadManager dm = (DownloadManager)cell.getDataSource();
    // exit early if the cell is valid and the download isn't active
    // (an inactive download means the share ratio won't change)
    int iState = (dm == null) ? DownloadManager.STATE_QUEUED : dm.getState();
    if (cell.isValid() && 
        iState != DownloadManager.STATE_DOWNLOADING &&
        iState != DownloadManager.STATE_SEEDING)
      return;
                       
    int sr = (dm == null) ? 0 : dm.getStats().getShareRatio();
    if (sr == -1) {
      sr = Constants.INFINITY_AS_INT;
      shareRatio = Constants.INFINITY_STRING;
    } else {
      String partial = String.valueOf(sr % 1000);
      while (partial.length() < 3) {
        partial = "0" + partial;
      }
      shareRatio = (sr / 1000) + "." + partial;
    }
    cell.setSortValue(sr);
    cell.setText(shareRatio);
  }
}
