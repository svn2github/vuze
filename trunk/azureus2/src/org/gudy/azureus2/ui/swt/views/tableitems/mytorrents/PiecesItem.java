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

/**
 * @author Olivier
 *
 */
public class PiecesItem extends TorrentGraphicItem  {
  // only supports 0 or 1 border width
  private final static int borderHorizontalSize = 1;
  private final static int borderVerticalSize = 1;
  private final static int borderSplit = 1;
  private final static int completionHeight = 2;
  
  int[] imageBuffer = {};

  /**
   * @param row
   * @param position
   */
  public PiecesItem(TorrentRow torrentRow, int position) {
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
      
    int x0 = borderVerticalSize;
    int x1 = bounds.width - 1 - borderVerticalSize;
    int y0 = completionHeight + borderHorizontalSize + borderSplit;
    int y1 = bounds.height - 1 - borderHorizontalSize;
    int drawWidth = x1 - x0 + 1;
    if (drawWidth < 10 || y1 < 3)
      return;
    boolean bImageBufferValid = true;
    boolean bImageChanged = false;
    if (imageBuffer.length != x1 || !torrentRow.isValid()) {
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
      image = new Image(torrentRow.getTableItem().getDisplay(), bounds.width, bounds.height);
      imageBounds = image.getBounds();
      bImageBufferValid = false;

      // draw border
      gcImage = new GC(image);
      gcImage.setForeground(MainWindow.grey);
      if (borderHorizontalSize > 0) {
        if (borderVerticalSize > 0) {
          gcImage.drawRectangle(0, 0, bounds.width - 1, bounds.height - 1);
        } else {
          gcImage.drawLine(0, 0, bounds.width - 1, 0);
          gcImage.drawLine(0, bounds.height -1, bounds.width - 1, bounds.height - 1);
        }
      } else if (borderVerticalSize > 0) {
        gcImage.drawLine(0, 0, 0, bounds.height - 1);
        gcImage.drawLine(bounds.width - 1, 0, bounds.width - 1, bounds.height - 1);
      }
      
      if (borderSplit > 0) {
        gcImage.setForeground(MainWindow.white);
        gcImage.drawLine(x0, completionHeight + borderHorizontalSize,
                         x1, completionHeight + borderHorizontalSize);
      }
    } else {
      gcImage = new GC(image);
    }

    boolean available[] = torrentRow.getManager().getPiecesStatus();
    if (available != null && available.length > 0) {
      int nbComplete = 0;
      int nbPieces = available.length;
      int a0;
      int a1 = 0;
      for (int i = 0; i < drawWidth; i++) {
        if (i == 0) {
          // always start out with one piece
          a0 = 0;
          a1 = nbPieces / drawWidth;
          if (a1 == 0) 
            a1 = 1;
        } else {
          // the last iteration, a1 will be nbPieces
          a0 = a1;
          a1 = ((i + 1) * nbPieces) / (drawWidth);
        }
        
        int index = 0;
        
        if (a1 <= a0) {
          index = imageBuffer[i - 1];
        } else {
          int nbAvailable = 0;
          for (int j = a0; j < a1; j++)
            if (available[j])
              nbAvailable++;
          nbComplete += nbAvailable;
          index = (nbAvailable * MainWindow.BLUES_DARKEST) / (a1 - a0);
          //System.out.println("i="+i+";nbAvailable="+nbAvailable+";nbComplete="+nbComplete+";nbPieces="+nbPieces+";a0="+a0+";a1="+a1);
        }

        if (!bImageBufferValid || imageBuffer[i] != index) {
          imageBuffer[i] = index;
          bImageChanged = true;
          gcImage.setForeground(MainWindow.blues[index]);
          gcImage.drawLine(i + x0, y0, i + x0, y1);
        }
      }

      int limit = (drawWidth * nbComplete) / nbPieces;
      if (limit < drawWidth) {
        gcImage.setBackground(MainWindow.blues[MainWindow.BLUES_LIGHTEST]);
        gcImage.fillRectangle(limit+x0, borderHorizontalSize,
                              x1-limit, completionHeight);
      }
      gcImage.setBackground(MainWindow.colorProgressBar);
      gcImage.fillRectangle(x0, borderHorizontalSize,
                            limit, completionHeight);
    }
    gcImage.dispose();

    if (bImageChanged) {
  	  //tableRow.debugOut("tr.refresh() "+image+ ";V"+ tableRow.isValid(), false);
      setGraphic(image);
    }
  }
}
