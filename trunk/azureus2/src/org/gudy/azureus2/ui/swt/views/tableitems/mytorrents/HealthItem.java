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

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Table;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.components.BufferedTableRow;

/**
 * @author Olivier
 *
 */
public class HealthItem extends TorrentGraphicItem  {
  
  /**
   * @param row
   * @param position
   */
  public HealthItem(TorrentRow torrentRow, int position) {
    super(torrentRow, position);
    fillCell = false;
    disposeGraphic = false;
  }
  
  public void refresh() {
    boolean valid = torrentRow.isValid();
    if(valid) return;
    
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
    
    if (setGraphic(ImageRepository.getImage(image_name))) {
      doPaint();
    }
  }
}
