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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.gudy.azureus2.ui.swt.MainWindow;
import org.gudy.azureus2.ui.swt.components.BufferedTableRow;

/**
 * @author Olivier
 *
 */
public class CompletionItem extends TorrentItem  {
  
  //The Buffered image;
  Image image;
  //And its size
  Point imageSize;
  
  /**
   * @param row
   * @param position
   */
  public CompletionItem(TorrentRow torrentRow, int position) {
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
    int width = bounds.width - 1;
    int x0 = bounds.x;
    int y0 = bounds.y + 1;
    int height = bounds.height - 3;
    if (width < 10 || height < 3)
      return;
    //Get the table GC
    GC gc = new GC(row.getTable());
    gc.setClipping(row.getTable().getClientArea());
    if (valid && image != null) {
      //If the image is still valid, simply copy it :)
      gc.setForeground(MainWindow.grey);
      gc.drawImage(image, x0, y0);
      gc.drawRectangle(x0, y0, width, height);
      gc.dispose();
    }
    else {
    	Image oldImage = null;
      if (image == null || ! imageSize.equals(new Point(width,height))) {
        oldImage = image;
    		image = new Image(torrentRow.getTableItem().getDisplay(), width, height);
        imageSize = new Point(width,height);
     	}
      GC gcImage = new GC(image);
      int percentDone = torrentRow.getManager().getStats().getCompleted();        
      gcImage.setBackground(MainWindow.blues[4]);
      gcImage.fillRectangle(0,1,(percentDone*width)/1000,1+height);        
      gcImage.dispose();
      gc.setForeground(MainWindow.grey);
      gc.drawImage(image, x0, y0);
      gc.drawRectangle(x0, y0, width, height);
      gc.dispose();
      if (oldImage != null && !oldImage.isDisposed())
        oldImage.dispose();
    }
  }
  
  public void dispose() {
    if(image != null && ! image.isDisposed()) {
      image.dispose();
    }
  }
}
