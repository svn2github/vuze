/*
 * File    : ScaledGraphic.java
 * Created : 15 déc. 2003}
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
package org.gudy.azureus2.ui.swt.components.graphics;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.gudy.azureus2.ui.swt.MainWindow;

/**
 * @author Olivier
 *
 */
public class ScaledGraphic implements Graphic {

  protected Scale scale;
  protected ValueFormater formater;
  
  protected Canvas drawCanvas;  
  protected Image bufferImage;
  
  private int i = 1;
  
  public ScaledGraphic(Scale scale,ValueFormater formater) {
    this.scale = scale;
    this.formater = formater;
  }
  
  public void initialize(Canvas canvas) {
    this.drawCanvas = canvas;
  }
  
  public void refresh() {    
    i++;
    scale.setMax(i);
    if(drawCanvas == null || drawCanvas.isDisposed())
      return;
    drawScale();
    
    Rectangle bounds = drawCanvas.getClientArea();
    if(bounds.height < 30 || bounds.width  < 100)
      return;
    
    GC gc = new GC(drawCanvas);
    gc.drawImage(bufferImage,bounds.x,bounds.y);
    gc.dispose();
  }
  
  protected void drawScale() {
    if(drawCanvas == null || drawCanvas.isDisposed())
      return;
    Rectangle bounds = drawCanvas.getClientArea();
    if(bounds.height < 30 || bounds.width  < 30)
      return;
    //Ok if bufferedImage is not null, dispose it
    if(bufferImage != null && ! bufferImage.isDisposed())
      bufferImage.dispose();
    
    bufferImage = new Image(drawCanvas.getDisplay(),bounds);
    GC gcImage = new GC(bufferImage);
    gcImage.setForeground(MainWindow.white);
    gcImage.fillRectangle(1,1,bounds.width-2,bounds.height-2);
    gcImage.setForeground(MainWindow.black);
    gcImage.drawRectangle(0,0,bounds.width-1,bounds.height-1);   
    
    scale.setNbPixels(bounds.height - 16);
    int[] levels = scale.getScaleValues();
    for(int i = 0 ; i < levels.length ; i++) {
      int height = bounds.height - scale.getScaledValue(levels[i]) - 2;
      gcImage.drawLine(1,height,bounds.width - 70 ,height);
      gcImage.drawText(formater.format(levels[i]),bounds.width - 65,height - 12);
    }
    gcImage.dispose();   
  }
  
}
