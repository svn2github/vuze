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
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.components.BufferedTableRow;

/**
 * @author Olivier
 *
 */
public class WealthItem extends TorrentItem  {
  
  /**
   * @param row
   * @param position
   */
  public WealthItem(TorrentRow torrentRow, int position) {
    super(torrentRow, position);
  }
  
  public void refresh() {
    boolean valid = torrentRow.isValid();    
    BufferedTableRow row = torrentRow.getRow();
    
    if (row == null || row.isDisposed())
      return;
    
    //Compute bounds ...
    Rectangle bounds = getBounds();
    //In case item isn't displayed bounds is null
    if(bounds == null)
      return;
    
    //Get the table GC
    GC gc = new GC(row.getTable());
    gc.setClipping(bounds);
    Image image = ImageRepository.getImage("st_stopped");   
    DownloadManager manager = torrentRow.getManager();
    int wealth = manager.getWealthyStatus();
    if(wealth == DownloadManager.WEALTH_KO) {
      image = ImageRepository.getImage("st_ko");   
    } else if (wealth == DownloadManager.WEALTH_OK) {
      image = ImageRepository.getImage("st_ok");   
    } else if (wealth == DownloadManager.WEALTH_NO_TRACKER) {
      image = ImageRepository.getImage("st_no_tracker");   
    }else if (wealth == DownloadManager.WEALTH_NO_REMOTE) {
      image = ImageRepository.getImage("st_no_remote");   
    }
    if(image != null)
      gc.drawImage(image, bounds.x + 1, bounds.y + 1);      
    gc.dispose();    
  }
  
  public void dispose() {    
  }
}
