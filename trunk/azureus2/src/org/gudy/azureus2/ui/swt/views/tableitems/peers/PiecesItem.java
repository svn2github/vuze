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
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Table;
import org.gudy.azureus2.ui.swt.MainWindow;
import org.gudy.azureus2.ui.swt.components.BufferedTableRow;
import org.gudy.azureus2.ui.swt.views.utils.VerticalAligner;

/**
 * @author Olivier
 *
 */
public class PiecesItem extends PeerItem  {
  
  //The Buffered image;
  Image image;
  //And its size
  Point imageSize;
  Canvas cBlockView;
  
  String txt;
  
  /**
   * @param row
   * @param position
   */
  public PiecesItem(PeerRow peerRow, int position) {
    super(peerRow, position);
    txt = "";
    cBlockView = new Canvas(getTable(), SWT.NULL);
    cBlockView.setBackground(MainWindow.white);
    cBlockView.addPaintListener(new PaintListener() {
    	public void paintControl(PaintEvent event) {
        if (event.count > 0 || event.width == 0 || event.height == 0)
          return;
    		refresh(true);
    	}
    });
  }
  
  public void refresh() {
    refresh(false);
  }
  public void refresh(boolean bForce) {
    boolean valid = peerRow.isValid();
    
    //If the image is still valid (not expired or not resized)
    if(valid && !bForce)
      return;
    
    //Compute bounds ...
    Rectangle bounds = getBounds();
    
    //In case item isn't displayed bounds is null
    if(bounds == null)
      return;
    cBlockView.setBounds(bounds);
    
    int width = bounds.width - 2;
    int height = bounds.height - 2;
    if (width < 10 || height < 3)
      return;
    
    if (!valid || image == null || bForce) {
      Image oldImage = null;
      if (image == null || ! imageSize.equals(new Point(width,height))) {
        oldImage = image;
        image = new Image(cBlockView.getDisplay(), width, height);
        imageSize = new Point(width,height);
      }
      GC gcImage = new GC(image);
      boolean available[] = peerRow.getPeerSocket().getAvailable();
      if (available != null) {
        int nbPieces = available.length;
        for (int i = 0; i < width - 2; i++) {
          int a0 = (i * nbPieces) / (width-2);
          int a1 = ((i + 1) * nbPieces) / (width-2);
          if (a1 == a0)
            a1++;
          if (a1 > nbPieces)
            a1 = nbPieces;
          int nbAvailable = 0;
          for (int j = a0; j < a1; j++)
            if (available[j])
              nbAvailable++;
          int index = (nbAvailable * 4) / (a1 - a0);
          gcImage.setForeground(MainWindow.blues[index]);
          gcImage.drawLine(i,1,i,1+height);
        }
        gcImage.setForeground(MainWindow.grey);
        gcImage.drawRectangle(0, 0, width-2, height-1);
      }
      gcImage.dispose();
      if (oldImage != null && !oldImage.isDisposed())
        oldImage.dispose();
      
      doPaint(bounds,true);
    }
  }
  
  public boolean needsPainting() {
  	return true;
  }
  
  public void doPaint(Rectangle clipping) {
    doPaint(clipping,false);
  }
  
  public void doPaint(Rectangle clipping,boolean ignoreValid) {
    boolean valid = ignoreValid || peerRow.isValid();    
    BufferedTableRow row = peerRow.getRow();
    
    if (row == null || row.isDisposed())
      return;
    
    //Compute bounds ...
    Rectangle bounds = getBounds();
    //In case item isn't displayed bounds is null
    if(bounds == null)
      return;
    int width = bounds.width - 1;    
    int height = bounds.height - 3;
        
    if (width < 10 || height < 3)
      return;
    
    if (valid && image != null) {
      GC gc = new GC(cBlockView);
      gc.drawImage(image, 0, 0);
//      gc.setClipping(clipping);   
      gc.drawImage(image, 0, 0);
      gc.dispose();
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
}
