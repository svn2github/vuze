/*
 * Created : 11 nov. 2004
 * By      : Alon Rohter
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



public class SeedToPeerRatioItem
       extends CoreTableColumn 
       implements TableCellRefreshListener
{

  public SeedToPeerRatioItem(String sTableID) {
    super("seed_to_peer_ratio", ALIGN_TRAIL, POSITION_INVISIBLE, 70, sTableID);
    setRefreshInterval(INTERVAL_LIVE);
  }

  protected void finalize() throws Throwable {
    super.finalize();
  }

  public void refresh(TableCell cell) {
    float ratio = -1;

    DownloadManager dm = (DownloadManager)cell.getDataSource();
    if( dm != null ) {
      TRTrackerScraperResponse response = dm.getTrackerScrapeResponse();
      if( response != null && response.isValid() ) {
        ratio = (float)response.getSeeds() / response.getPeers();
      }
    }

    if( !cell.setSortValue( ratio ) && cell.isValid() ) {
      return;
    }
    
    if( ratio == -1 ) {
      cell.setText( "" );
    }
    else {
      String value = Float.toString( ratio );
      
      int dot_index = value.indexOf( (char)46 );
      
      if( value.length() > dot_index + 3 ) {
        value = value.substring( 0, dot_index + 4 );
      }
      else {
        while( value.length() < dot_index + 4 ) {
          value = value + "0";
        }
      }
      
      cell.setText( value );
    }
  }

}
