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

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.gudy.azureus2.ui.swt.MainWindow;

/**
 * @author Olivier
 *
 */
public class BackGroundGraphic implements Graphic{
    
  protected Canvas drawCanvas;    
  
  protected Image bufferBackground;
  
  protected Color lightGrey;
  protected Color lightGrey2;
  
  
  public BackGroundGraphic() {
  }
  
  public void initialize(Canvas canvas) {
    this.drawCanvas = canvas;
    lightGrey = new Color(canvas.getDisplay(),250,250,250);
    lightGrey2 = new Color(canvas.getDisplay(),233,233,233);
  }
  
  public void refresh() {    
  }
  
  protected void drawBackGround(boolean sizeChanged) {    
    if(drawCanvas == null || drawCanvas.isDisposed())
      return;
    
    if(sizeChanged || bufferBackground == null) {             
      Rectangle bounds = drawCanvas.getClientArea();
      if(bounds.height < 30 || bounds.width  < 100)
        return; 
      
      if(bufferBackground != null && ! bufferBackground.isDisposed())
        bufferBackground.dispose();
           
      bufferBackground = new Image(drawCanvas.getDisplay(),bounds);
      
      Color colors[] = new Color[4];
      colors[0] = MainWindow.white;
      colors[1] = lightGrey;
      colors[2] = lightGrey2;
      colors[3] = lightGrey;
      GC gcBuffer = new GC(bufferBackground);
      for(int i = 0 ; i < bounds.height - 2 ; i++) {
        gcBuffer.setForeground(colors[i%4]);
        gcBuffer.drawLine(1,i+1,bounds.width-1,i+1);
      }       
      gcBuffer.setForeground(MainWindow.black);
      gcBuffer.drawLine(bounds.width-70,0,bounds.width-70,bounds.height-1);    
      
      gcBuffer.drawRectangle(0,0,bounds.width-1,bounds.height-1);
      gcBuffer.dispose();
    }
  }
  
  public void dispose() {
    if(bufferBackground != null && ! bufferBackground.isDisposed())
      bufferBackground.dispose();
    if(lightGrey != null && ! lightGrey.isDisposed())
      lightGrey.dispose();
    if(lightGrey2 != null && ! lightGrey2.isDisposed())
      lightGrey2.dispose();
  }   
  
}
