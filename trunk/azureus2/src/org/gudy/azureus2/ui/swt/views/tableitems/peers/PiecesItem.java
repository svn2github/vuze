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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.ui.swt.MainWindow;
import org.gudy.azureus2.ui.swt.components.BufferedTableRow;
import org.gudy.azureus2.ui.swt.views.utils.VerticalAligner;

/**
 * @author Olivier
 *
 */
public class PiecesItem extends PeerGraphicItem  {
  // only supports 1 border width
  private final static int borderSize = 1;
  
  int[] imageBuffer = {};

  /**
   * @param row
   * @param position
   */
  public PiecesItem(PeerRow peerRow, int position) {
    super(peerRow, position);
  }
  
  public void refresh() {
    //Bounds of canvas without padding
    Rectangle bounds = getBoundsForCanvas();
    
    //In case item isn't displayed bounds is null
    if(bounds == null)
      return;

    int x1 = bounds.width - borderSize - 1;
    int y1 = bounds.height - borderSize - 1;
    if (x1 < 10 || y1 < 3)
      return;
    boolean bImageBufferValid = true;
    boolean bImageChanged = false;
    if (imageBuffer.length != x1) {
      imageBuffer = new int[x1];
      bImageBufferValid = false;
    }
    
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
      image = new Image(peerRow.getTableItem().getDisplay(), bounds.width, bounds.height);
      imageBounds = image.getBounds();
      bImageBufferValid = false;

      // draw border
      gcImage = new GC(image);
      gcImage.setForeground(MainWindow.grey);
      gcImage.drawRectangle(0, 0, bounds.width - 1, bounds.height - 1);
    } else {
      gcImage = new GC(image);
    }

    boolean available[] = peerRow.getPeerSocket().getAvailable();
    if (available != null) {
      int nbPieces = available.length;
      for (int i = 0; i < x1; i++) {
        int a0 = (i * nbPieces) / (x1);
        int a1 = ((i + 1) * nbPieces) / (x1);
        if (a1 == a0)
          a1++;
        if (a1 > nbPieces)
          a1 = nbPieces;
        int nbAvailable = 0;
        for (int j = a0; j < a1; j++)
          if (available[j])
            nbAvailable++;
        int index = (nbAvailable * MainWindow.BLUES_DARKEST) / (a1 - a0);

        if (!bImageBufferValid || imageBuffer[i] != index) {
          imageBuffer[i] = index;
          bImageChanged = true;
          gcImage.setForeground(MainWindow.blues[index]);
          gcImage.drawLine(i+borderSize, borderSize, i+borderSize, y1);
        }
      }
    }
    gcImage.dispose();

    if (bImageChanged) {
  	  //peerRow.debugOut("refresh()", false);
      setGraphic(image);
    }
  }
}
