 /*
 * File    : BufferedGraphicTableItem1.java
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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Table;
import org.gudy.azureus2.ui.swt.components.BufferedTableRow;
import org.gudy.azureus2.ui.swt.components.BufferedTableItem;
import org.gudy.azureus2.ui.swt.views.utils.VerticalAligner;

/** Draws an image at a column in a row of a table using direct paints to the 
 *  table.
 * In comparison to BufferedGraphicTable2,
 * Pros:
 *  - Cleaner
 *  - More proper
 *
 * Cons:
 *  - Bug - overpainting of table causing our cell to redraw everytime any other cell redraws
 *          (New for Windows since SWT3.0M8, always been there for linux)
 *  - Bug - incorrect drawing location on linux (new to SWT3.0M8)
 *  - other bugs
 *
 * @see BufferedGraphicTable2
 * @author TuxPaper
 *
 */
public abstract class BufferedGraphicTableItem1 extends BufferedGraphicTableItem {
  //The Buffered image
  private Image image;
  
  
  public BufferedGraphicTableItem1(BufferedTableRow row,int position) {
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
    boolean bImageSet = (image != img);

    if (bImageSet) {
      image = img;
    }

    Table table = getTable();
    if (table != null && !table.isDisposed()) {
      Rectangle bounds = getBoundsForCanvas();
      if (bounds != null) table.redraw(bounds.x, bounds.y, 
                                       bounds.width, bounds.height, true);
    }

    return bImageSet;
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
    Table table = getTable();
    //Compute bounds ...
    Rectangle bounds = getBoundsForCanvas();
    //In case item isn't displayed bounds is null
    if (bounds == null || image == null || image.isDisposed()) {
      return;
    }
    //debugOut("doPaint()" + ((gc == null) ? "GC NULL" : String.valueOf(gc.getClipping())) + 
    //         "ta="+table.getClientArea()+";bounds="+bounds, false);

    Rectangle imageBounds = image.getBounds();
    if (fillCell) {
      if (imageBounds.width != bounds.width ||
          imageBounds.height != bounds.height) {
/*
        // Enable this for semi-fast visual update with some flicker
        boolean ourGC = (gc == null);
        if (ourGC)
          gc = new GC(table);
        if (gc != null) {
          gc.drawImage(image, 0, 0, imageBounds.width, imageBounds.height, 
                       bounds.x, bounds.y, bounds.width, bounds.height);
          if (ourGC)
            gc.dispose();
        }
        // _OR_ enable refresh() for slower visual update with lots of flicker
        //refresh();
        
        // OR, disable both and image will be updated on next graphic bar update
        
        // TODO: make config option to choose
*/
        //debugOut("doPaint() sizewrong.  Image="+imageBounds +";us="+bounds, false);
        invalidate();
        return;
      }
    } else if (imageBounds.width < bounds.width) {
      bounds.x += (bounds.width - imageBounds.width) / 2;
    }
    
    Rectangle tableBounds = table.getClientArea();
    if (bounds.y + bounds.height < table.getHeaderHeight() || bounds.y > tableBounds.height) {
      //debugOut("doPaint() "+String.valueOf(bounds.y + bounds.height)+"<"+tableBounds.y, false);
      return;
    }
    
    Rectangle clipping = new Rectangle(bounds.x, bounds.y, 
                                       bounds.width, 
                                       bounds.height);
    if (clipping.y < table.getHeaderHeight()) {
      clipping.height -= table.getHeaderHeight() - clipping.y;
      clipping.y = table.getHeaderHeight();
    }
    if (clipping.y + clipping.height > tableBounds.height)
      clipping.height = tableBounds.height - clipping.y + 1;

    //debugOut("doPaint() clipping="+clipping, false);
    if (clipping.width <= 0 && clipping.height <= 0) {
      return;
    }

    // See Eclipse Bug 42416
    // "[Platform Inconsistency] GC(Table) has wrong origin"
    // Notes/Questions:
    // - GTK's "new GC(table)" starts under header, instead of above
    //   -- so, adjust bounds up
    // - Appears to apply to new GC(table) AND GC passed by PaintEvent from a Table PaintListener
    // - Q) .height may be effected (smaller than it should be).  How does this effect clipping?
    // - Q) At what version does this bug start appearing?
    //   A) Reports suggest at least 2.1.1
    bounds.y += VerticalAligner.getTableAdjustVerticalBy(table);
    clipping.y += VerticalAligner.getTableAdjustVerticalBy(table);

    boolean ourGC = (gc == null);
    if (ourGC) {
      gc = new GC(table);
      if (gc == null) {
        return;
      }
    }
    gc.setClipping(clipping);
    // I believe GTK M8 has a bounds.x bug.. because this works fine in M7
    gc.drawImage(image, bounds.x, bounds.y);
    //debugOut("doPaint()"+gc+": ourGC="+ourGC+"clip:+"+ gc.getClipping()+";bounds:"+bounds+";ca="+table.getClientArea(), false);
    if (ourGC) {
      gc.dispose();
    }
  }

  
  public void dispose() {
    super.dispose();
    image = null;
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

  public Point getSize() {
    Rectangle bounds = getBounds();
    if(bounds == null)
      return new Point(0, 0);
    return new Point(bounds.width - (marginWidth * 2), 
                     bounds.height - (marginHeight * 2));
  }
  
  public void invalidate() {
  }
}
