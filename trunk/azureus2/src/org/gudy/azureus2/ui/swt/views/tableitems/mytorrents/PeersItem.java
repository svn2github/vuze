/*
 * File    : PeersItem.java
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

import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;


/** # of Peers
 *
 * @author Olivier
 * @author TuxPaper (2004/Apr/17: modified to TableCellAdapter)
 */
public class PeersItem
       extends CoreTableColumn 
       implements TableCellRefreshListener
{
  /** Default Constructor */
  public PeersItem(String sTableID) {
    super("peers", ALIGN_CENTER, POSITION_LAST, 60, sTableID);
    setRefreshInterval(INTERVAL_LIVE);
  }

  public void refresh(TableCell cell) {
    long lConnectedPeers = 0;
    long lTotalPeers = -1;
    DownloadManager dm = (DownloadManager)cell.getDataSource();
    if (dm != null) {
      lConnectedPeers = dm.getNbPeers();
      TRTrackerScraperResponse hd = dm.getTrackerScrapeResponse();
      if (hd != null && hd.isValid())
        lTotalPeers = hd.getPeers();
    }
    
    String tmp = String.valueOf(lConnectedPeers); //$NON-NLS-1$
    if (lTotalPeers != -1)
      tmp += " (" + lTotalPeers + ")";
    else
      lTotalPeers = 0;
    cell.setText(tmp);
    cell.setSortValue(lConnectedPeers * 10000000 + lTotalPeers);
  }
}
