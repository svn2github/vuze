/*
 * File    : HealthItem.java
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

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

/**
 *
 * @author Olivier
 * @author TuxPaper (2004/Apr/17: modified to TableCellAdapter)
 */
public class HealthItem
       extends CoreTableColumn 
       implements TableCellAddedListener
{
  /** Default Constructor */
  public HealthItem(String sTableID) {
    super("health", sTableID);
    initializeAsGraphic(POSITION_LAST, 20);
  }

  public void cellAdded(TableCell cell) {
    new Cell(cell);
  }

  private class Cell
          implements TableCellRefreshListener
  {
    private String sLastImageName = "";

    public Cell(TableCell cell) {
      cell.addRefreshListener(this);
      cell.setMarginWidth(0);
      cell.setMarginHeight(0);
      cell.setFillCell(false);
    }
    
    public void refresh(TableCell cell) {
      String image_name = "st_stopped";
      
      DownloadManager dm = (DownloadManager)cell.getDataSource();
      int wealth = (dm == null) ? 0 : dm.getHealthStatus();
      if (!cell.setSortValue(wealth) && cell.isValid())
        return;

      if(wealth == DownloadManager.WEALTH_KO) {
      	image_name = "st_ko";   
      } else if (wealth == DownloadManager.WEALTH_OK) {
      	image_name = "st_ok";   
      } else if (wealth == DownloadManager.WEALTH_NO_TRACKER) {
      	image_name = "st_no_tracker";   
      }else if (wealth == DownloadManager.WEALTH_NO_REMOTE) {
      	image_name = "st_no_remote";   
      }
      image_name += "_selected";
      
      if (!sLastImageName.equals(image_name) || !cell.isValid()) {
        cell.setGraphic(ImageRepository.getImage(image_name));
        sLastImageName = image_name;
      }
    }
  }
}
