/*
 * File    : SeedsItem.java
 * Created : 24 nov. 2003
 * By      : Olivier
 *
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
 
package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;

import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;


/**
 *
 * @author Olivier
 * @author TuxPaper (2004/Apr/17: modified to TableCellAdapter)
 */
public class SeedsItem
       extends CoreTableColumn 
       implements TableCellRefreshListener
{
  /** Default Constructor */
  public SeedsItem(String sTableID) {
    super("seeds", ALIGN_CENTER, POSITION_LAST, 60, sTableID);
    setRefreshInterval(INTERVAL_LIVE);
  }

  public void refresh(TableCell cell) {
    long lConnectedSeeds = 0;
    long lTotalSeeds = -1;
    DownloadManager dm = (DownloadManager)cell.getDataSource();
    if (dm != null) {
      lConnectedSeeds = dm.getNbSeeds();
      TRTrackerScraperResponse hd = dm.getTrackerScrapeResponse();
      if (hd != null && hd.isValid())
        lTotalSeeds = hd.getSeeds();
    }
    
    long value = lConnectedSeeds * 10000000 + lTotalSeeds;
    if (!cell.setSortValue(value) && cell.isValid())
      return;

    String tmp = String.valueOf(lConnectedSeeds); //$NON-NLS-1$
    if (lTotalSeeds != -1)
      tmp += " (" + lTotalSeeds + ")";
    else
      lTotalSeeds = 0;
    cell.setText(tmp);
  }
}
