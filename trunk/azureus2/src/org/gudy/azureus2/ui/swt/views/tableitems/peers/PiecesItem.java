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
 
package org.gudy.azureus2.ui.swt.views.tableitems.peers;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.gudy.azureus2.ui.swt.MainWindow;
import org.gudy.azureus2.ui.swt.components.BufferedTableRow;

/**
 * @author Olivier
 *
 */
public class PiecesItem extends PeerItem  {
  
  //The Buffered image;
  Image image;
  
  /**
   * @param row
   * @param position
   */
  public PiecesItem(PeerRow peerRow, int position) {
    super(peerRow, position);
  }
  
  public void refresh() {
    boolean valid = peerRow.isValid();
    peerRow.setValid(true);
    BufferedTableRow row = peerRow.getRow();
    
    if (row == null || row.isDisposed())
      return;
    
    //Compute bounds ...
    Rectangle bounds = getBounds();
    int width = bounds.width - 1;
    int x0 = bounds.x;
    int y0 = bounds.y + 1;
    int height = bounds.height - 3;
    if (width < 10 || height < 3)
      return;
    //Get the table GC
    GC gc = new GC(row.getTable());
    gc.setClipping(row.getTable().getClientArea());
    if (valid) {
      //If the image is still valid, simply copy it :)
      gc.setForeground(MainWindow.grey);
      gc.drawImage(image, x0, y0);
      gc.drawRectangle(x0, y0, width, height);
      gc.dispose();
    }
    else {
 // no need to reallocate the image each time.
 //     Image is not valid anymore ... so 1st free it :)
 //     Image oldImage = null;//image;      
 //     image = new Image(peerRow.getTableItem().getDisplay(), width, height);
    	if (image == null) {
    		image = new Image(peerRow.getTableItem().getDisplay(), width, height);
     	}
      GC gcImage = new GC(image);
      boolean available[] = peerRow.getPeerSocket().getAvailable();
      if (available != null) {
        int nbPieces = available.length;
        for (int i = 0; i < width; i++) {
          int a0 = (i * nbPieces) / width;
          int a1 = ((i + 1) * nbPieces) / width;
          if (a1 == a0)
            a1++;
          if (a1 > nbPieces)
            a1 = nbPieces;
          int nbAvailable = 0;
          for (int j = a0; j < a1; j++)
            if (available[j])
              nbAvailable++;
          int index = (nbAvailable * 4) / (a1 - a0);
          //System.out.print(index);
//          gcImage.setBackground(MainWindow.blues[index]);
          gcImage.setForeground(MainWindow.blues[index]);
          gcImage.drawLine(i,1,i,1+height);
          // no need to draw a one pixel wide rect
         // gcImage.fillRectangle(i,1,1,height);
        }
      }
      gcImage.dispose();
      gc.setForeground(MainWindow.grey);
      gc.drawImage(image, x0, y0);
      gc.drawRectangle(x0, y0, width, height);
      gc.dispose();
 //     if (oldImage != null && !oldImage.isDisposed())
 //       oldImage.dispose();
    }
  }
  
  public void dispose() {
    if(image != null && ! image.isDisposed()) {
      image.dispose();
    }
  }
}
