/*
 * File    : TorrentItem.java
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

/**
 * @author Olivier
 *
 */
public class HealthItem extends TorrentGraphicItem  {
  private String sLastImageName = "";
  /**
   * @param row
   * @param position
   */
  public HealthItem(TorrentRow torrentRow, int position) {
    super(torrentRow, position);
    marginWidth = 0;
    marginHeight = 0;
    fillCell = false;
    disposeGraphic = false;
  }
  
  public void refresh() {
    String	image_name = "st_stopped";
    
    DownloadManager manager = torrentRow.getManager();
    int wealth = manager.getHealthStatus();
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
    
    if (!sLastImageName.equals(image_name) || !torrentRow.isValid()) {
      setGraphic(ImageRepository.getImage(image_name));
      sLastImageName = image_name;
    }
  }
}
