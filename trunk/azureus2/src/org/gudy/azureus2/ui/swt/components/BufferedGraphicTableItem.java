 /*
 * File    : BufferedGraphicTableItem.java
 * Created : 24 nov. 2003
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
 
package org.gudy.azureus2.ui.swt.components;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Table;
import org.gudy.azureus2.ui.swt.components.BufferedTableRow;
import org.gudy.azureus2.ui.swt.components.BufferedTableItem;
import org.gudy.azureus2.ui.swt.views.utils.VerticalAligner;

/**
 * @author TuxPaper
 *
 */
public abstract class BufferedGraphicTableItem extends BufferedTableItem {
  // same names as GridLayout

  /** marginHeight specifies the number of pixels of vertical margin that will 
   * be placed along the top and bottom edges of the layout.
   * The default is 1.
   */
  public int marginHeight = 1;

  /** marginWidth specifies the number of pixels of horizontal margin that will
   * be placed along the left and right edges of the layout.
   * The default is 1.
   */
  public int marginWidth = 1;

  /** Whether the graphic fills the whole cell.  If true, update() will be
   * called when the size of the cell has changed.
   */
  public boolean fillCell = true;
  
  /** Whether BufferedGraphicTableItem is to dispose of the graphic or not.
   */
  public boolean disposeGraphic = true;

  //The Buffered image
  private Image image;
  
  
  public BufferedGraphicTableItem(BufferedTableRow row,int position) {
    super(row, position);
  }

  /** Retrieve the graphic related to this table item.
   * @return the Image that is draw in the cell, or null if there is none.
   */
  public Image getGraphic() {
    return image;
  }
  
  /* Sets image to be drawn.
   * @param img Image to be stored & drawn
   * @return true - image was changed.  false = image was the same
   */
  public boolean setGraphic(Image img) {
    if (image == img)
      return false;
    if (disposeGraphic && image != null && !image.isDisposed()) {
      image.dispose();
    }
    image = img;
    doPaint();
    return true;
  }

  public boolean needsPainting() {
  	return true;
  }
  
  public void doPaint() {
    doPaint((GC)null);
  }

  /** Paint the bar without updating it's data.  Unless the size changed.
   */
  public void doPaint(GC gc) {
    //Compute bounds ...
    Rectangle bounds = getBoundsForCanvas();
    //In case item isn't displayed bounds is null
    if (bounds == null) {
      return;
    }

    if (image == null) {
      refresh();
      return;
    }
    if (fillCell) {
      Rectangle imageBounds = image.getBounds();
      if (imageBounds.width != bounds.width ||
          imageBounds.height != bounds.height) {
        refresh();
        return;
      }
    }
    
    if (image == null || image.isDisposed()) {
      return;
    }

    boolean ourGC = (gc == null);
    if (ourGC) {
      Table table = getTable();
      Rectangle tableBounds = table.getClientArea();
      // all OSes, scrollbars are excluded (I hope!)
      // some OSes (all?), table header is included in client area
      if (tableBounds.y < table.getHeaderHeight()) {
        tableBounds.y = table.getHeaderHeight();
      }

      if (bounds == null || bounds.y < table.getHeaderHeight()) {
        return;
      }

      bounds = bounds.intersection(tableBounds);
      if (bounds.width <= 0 && bounds.height <= 0) {
        return;
      }

      gc = new GC(table);
      if (gc == null) {
        return;
      }
      bounds.y += VerticalAligner.getTableAdjustVerticalBy(table);
      gc.setClipping(bounds);
    }
    gc.drawImage(image, bounds.x, bounds.y);
    //tableRow.debugOut("doPaint()"+gc+": "+ gc.getClipping(), false);
    if (ourGC) {
      gc.dispose();
    }
  }

  
  public void dispose() {
    if(disposeGraphic && image != null && ! image.isDisposed()) {
      image.dispose();
    }
  }
  
  /** Calculate the bounds of the receiver should be drawing in
    * @return what size/position the canvas should be
    */
  public Rectangle getBoundsForCanvas() {
    Rectangle bounds = getBounds();
    if(bounds == null)
      return null;
    bounds.y += marginHeight;
    bounds.height -= (marginHeight * 2);
    bounds.x += marginWidth;
    bounds.width -= (marginWidth * 2);
    return bounds;
  }
}
