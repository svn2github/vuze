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
import org.gudy.azureus2.ui.swt.MainWindow;
import org.gudy.azureus2.ui.swt.components.BufferedTableRow;

/**
 * @author Olivier
 *
 */
public class CompletionItem extends TorrentGraphicItem  {
  
  private static final int borderWidth = 1;
  int lastPercentDone = 0;
  int lastWidth = 0;
  
  
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
    Rectangle bounds = getBoundsForCanvas();

    //In case item isn't displayed bounds is null
    if(bounds == null)
      return;
    
    int x1 = bounds.width - borderWidth - 1;
    int y1 = bounds.height - borderWidth - 1;
    if (x1 < 10 || y1 < 3) {
      return;
    }

    int percentDone = torrentRow.getManager().getStats().getDownloadCompleted(true);
    boolean bImageBufferValid = (lastPercentDone == percentDone) && (lastWidth == bounds.width) && torrentRow.isValid();
    if (bImageBufferValid) {
      return;
    }

    lastPercentDone = percentDone;
    lastWidth = bounds.width;

    Image image = getGraphic();
    GC gcImage;
    boolean bImageSizeChanged;
    Rectangle imageBounds;
    if (image == null) {
      bImageSizeChanged = true;
    } else {
      imageBounds = image.getBounds();
      bImageSizeChanged = imageBounds.width != bounds.width ||
                          imageBounds.height != bounds.height;
    }
    if (bImageSizeChanged) {
      image = new Image(torrentRow.getTableItem().getDisplay(), bounds.width, bounds.height);
      imageBounds = image.getBounds();
      bImageBufferValid = false;

      // draw border
      gcImage = new GC(image);
      gcImage.setForeground(MainWindow.grey);
      gcImage.drawRectangle(0, 0, bounds.width - 1, bounds.height - 1);
    } else {
      gcImage = new GC(image);
    }

    gcImage.setForeground(MainWindow.grey);
    gcImage.drawRectangle(0, 0, bounds.width-1, bounds.height-1);

    int limit = (x1 * percentDone) / 1000;
    gcImage.setBackground(MainWindow.blues[MainWindow.BLUES_DARKEST]);
    gcImage.fillRectangle(1,1,limit,y1);
    if (limit < x1) {
      gcImage.setBackground(MainWindow.blues[MainWindow.BLUES_LIGHTEST]);
      gcImage.fillRectangle(limit+1,1,x1-limit,y1);
    }

    gcImage.dispose();
      
    if (!bImageBufferValid) {
      setGraphic(image);
    }
  }
}
