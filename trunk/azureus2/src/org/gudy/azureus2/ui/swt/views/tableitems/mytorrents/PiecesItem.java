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
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
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
import org.gudy.azureus2.core3.util.Debug;

/**
 * @author Olivier
 *
 */
public class PiecesItem extends TorrentItem  {
  
  private static final int verticalPadding = 1;
  // only supports 1 border width
  private static final int borderWidth = 1;

  //The Buffered image;
  Image image;
  //And its size
  Point imageSize;
  //Painting on a canvas is smoother
  Canvas cBlockView;
  int[] imageBuffer = {};
  
  private void debugOut(String s, boolean bStackTrace) {
    TableItem[] ti = getTable().getSelection();
    if (ti.length > 0 && ti[0] == torrentRow.getTableItem()) {
      System.out.println(s);
      if (bStackTrace) Debug.outStackTrace(3);
    }
  }

  /**
   * @param row
   * @param position
   */
  public PiecesItem(TorrentRow torrentRow, int position) {
    super(torrentRow, position);
    cBlockView = new Canvas(getTable(), SWT.NULL);
    cBlockView.setBackground(MainWindow.white);
    cBlockView.addPaintListener(new PaintListener() {
    	public void paintControl(PaintEvent event) {
        if (event.width == 0 || event.height == 0)
          return;
    	  //debugOut("pc"+event, true);
        drawOnCanvas(event.gc, new Rectangle(event.x,event.y,event.width,event.height), true);
    	}
    });
  }
  
  public void refresh() {
    if (cBlockView == null || cBlockView.isDisposed())
      return;
    boolean valid = torrentRow.isValid();
    BufferedTableRow row = torrentRow.getRow();    
    if (row == null || row.isDisposed())
      return;
    
    //Compute bounds ...
    Rectangle bounds = getBoundsForCanvas();

    //In case item isn't displayed bounds is null
    if(bounds == null)
      return;
      
    if (!cBlockView.equals(bounds)) {
      cBlockView.setBounds(bounds);
      valid = false;
    }

    //If the image is still valid (not expired or not resized)
    if (valid)
      return;

    int x1 = bounds.width - borderWidth - 1;
    int y1 = bounds.height - borderWidth - 1;
    if (x1 < 10 || y1 < 3)
      return;
    boolean bImageBufferValid = true;
    boolean bImageChanged = false;
    if (imageBuffer.length != x1) {
      imageBuffer = new int[x1];
      bImageBufferValid = false;
    }
    
    Image oldImage = null;
    GC gcImage;
    if (image == null || !imageSize.equals(new Point(bounds.width,bounds.height))) {
      oldImage = image;
      image = new Image(cBlockView.getDisplay(), bounds.width, bounds.height);
      imageSize = new Point(bounds.width, bounds.height);

      gcImage = new GC(image);
      gcImage.setForeground(MainWindow.grey);
      gcImage.drawRectangle(0, 0, bounds.width - 1, bounds.height - 1);
    } else {
      gcImage = new GC(image);
    }

    boolean available[] = torrentRow.getManager().getPiecesStatus();
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
        int index = (nbAvailable * 4) / (a1 - a0);

        if (!bImageBufferValid || imageBuffer[i] != index) {
          imageBuffer[i] = index;
          bImageChanged = true;
          gcImage.setForeground(MainWindow.blues[index]);
          gcImage.drawLine(i+borderWidth, borderWidth, i+borderWidth, y1);
        }
      }      
    }
    gcImage.dispose();

    if (oldImage != null && !oldImage.isDisposed())
      oldImage.dispose();
    
    if (bImageChanged) {
      cBlockView.redraw();
  	  //debugOut("refresh()", true);
    }
  }
  
	public boolean needsPainting() {
		return true;
	}
  
  public void doPaint(Rectangle clipping) {
    if (cBlockView == null || cBlockView.isDisposed())
      return;
    // clipping is relative to table
    
    // verify position, because redraw() won't work if it isn't on screen
    Rectangle bounds = getBoundsForCanvas();
    //In case item isn't displayed bounds is null
    if(bounds != null) {
      Rectangle cBounds = cBlockView.getBounds();
      if (bounds.width != cBounds.width || bounds.height != cBounds.height) {
        refresh();
      }
      if (bounds.x != cBounds.x || bounds.y != cBounds.y) {
        cBlockView.setLocation(bounds.x, bounds.y);
      }
    }
/*  // disabled.  Every draw case taken care of.  (I think!)
    Rectangle drawArea = clipping.intersection(bounds);
    // make relative to canvas
    drawArea.x -= bounds.x;
    drawArea.y -= bounds.y;
    // I don't think we need to adjust if x or y is negative.  redraw _should_
    // handle it..
    cBlockView.redraw(drawArea.x, drawArea.y, drawArea.width, drawArea.height, false);
*/
  }

  public void drawOnCanvas(GC gc,  Rectangle clipping, boolean bForce) {
    if (cBlockView == null || cBlockView.isDisposed())
      return;
    BufferedTableRow row = torrentRow.getRow();    
    
    if (row == null || row.isDisposed())
      return;
    
    //Compute bounds ...
    Rectangle bounds = getBoundsForCanvas();
    //In case item isn't displayed bounds is null
    if(bounds == null)
      return;

    Rectangle cBounds = cBlockView.getBounds();
    if (bounds.width != cBounds.width || bounds.height != cBounds.height) {
      refresh();  // refresh will call doPaint again
      return;
    }
    if (bounds.x != cBounds.x || bounds.y != cBounds.y) {
      cBlockView.setLocation(bounds.x, bounds.y);
    }

    if (bForce || !torrentRow.isValid() || image != null) {
      // no need to setClipping()
      // "the graphics context to use when painting that is configured to use the colors, font and damaged
      // region of the control."
      gc.drawImage(image, 0, 0);
  	  //debugOut("drawOnCanvas() Painted?" + (bForce || !torrentRow.isValid() || image != null), true);
    }    
  }
  
  public void dispose() {
    if(image != null && ! image.isDisposed()) {
      image.dispose();
    }
    if (cBlockView != null) {
      if (!cBlockView.isDisposed())
        cBlockView.dispose();
      cBlockView = null;
    }
  }
  

  // returns what size/position the canvas should be
  private Rectangle getBoundsForCanvas() {
    Rectangle bounds = getBounds();
    if(bounds == null)
      return null;
    bounds.y += verticalPadding + VerticalAligner.getTableAdjustVerticalBy(getTable());
    bounds.height -= (verticalPadding * 2);
    return bounds;
  }
}
