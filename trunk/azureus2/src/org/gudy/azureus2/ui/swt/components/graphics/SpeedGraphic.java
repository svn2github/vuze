/*
 * File    : SpeedGraphic.java
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
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.MainWindow;

/**
 * @author Olivier
 *
 */
public class SpeedGraphic extends ScaledGraphic {
  
  private int max = 1;
  private int average = 0;
  private int nbValues = 0;
  
  private int[] values;
  private int currentPosition;
  
  
  private SpeedGraphic(Scale scale,ValueFormater formater) {
    super(scale,formater);
    
    currentPosition = 0;
    values = new int[2000];
  }
  
  public static SpeedGraphic getInstance() {
    return new SpeedGraphic(new Scale(),new ValueFormater() {
      public String format(int value) {
        return DisplayFormatters.formatByteCountToKiBEtcPerSec(value);
      }
    });
  }
  
  public void addIntValue(int value) {
    synchronized(this) {
	    if(value > max) {
	      max = value;
	      scale.setMax(max);
	    }
	    average += value - values[currentPosition];
	    values[currentPosition++] = value;
	    
	    if(nbValues < 2000)
	      nbValues++;
	    
	    if(currentPosition >= values.length)
	      currentPosition = 0;
	    
    }
  }
  
  public void refresh() {
    super.drawScale();    
    if(drawCanvas == null || drawCanvas.isDisposed())
      return;
    
    drawBackGround();
    drawScale();
    drawShart();
    
    
    Rectangle bounds = drawCanvas.getClientArea();
    if(bounds.height < 30 || bounds.width  < 100)
      return;
    
    GC gc = new GC(drawCanvas);
    gc.drawImage(bufferImage,bounds.x,bounds.y);
    gc.dispose();    
  }
  
  protected void drawShart() {
    synchronized(this) {
      if(drawCanvas == null || drawCanvas.isDisposed())
        return;
      Rectangle bounds = drawCanvas.getClientArea();
      if(bounds.height < 30 || bounds.width  < 100)
        return;
      GC gcImage = new GC(super.bufferImage);        
      
      
      int oldAverage = 0;
      Display display = drawCanvas.getDisplay();
      for(int x = 0 ; x < bounds.width - 71 ; x++) {
        int position = currentPosition - x -1;
        if(position < 0)
          position+= 2000;
        int value = values[position];
        int xDraw = bounds.width - 71 - x;
        int height = scale.getScaledValue(value);
        int percent = value * 255 / max;
        Color background = MainWindow.blues[4];
        Color foreground = MainWindow.blues[1];
        int r1 = background.getRed();
        int g1 = background.getGreen();
        int b1 = background.getBlue();
        int r2 = foreground.getRed();
        int g2 = foreground.getGreen();
        int b2 = foreground.getBlue();
        int r = (percent * r1) / 255 + ((255-percent) * r2) / 255;
        int g = (percent * g1) / 255 + ((255-percent) * g2) / 255;
        int b = (percent * b1) / 255 + ((255-percent) * b2) / 255;
        Color tempcolor = new Color(display,r,g,b);
        gcImage.setForeground(tempcolor);
        gcImage.setBackground(foreground);        
        gcImage.fillGradientRectangle(xDraw,bounds.height - 1 - height,1, height,true);
        tempcolor.dispose();
        
        int average = computeAverage(position);
        if(x > 6) {
          int h1 = bounds.height - scale.getScaledValue(average) - 2;
          int h2 = bounds.height - scale.getScaledValue(oldAverage) - 2;
          gcImage.setForeground(MainWindow.red);
          gcImage.drawLine(xDraw,h1,xDraw+1, h2);
        }
        oldAverage = average;
      }  
      
      if(nbValues > 0) {
        int height = bounds.height - scale.getScaledValue(computeAverage(currentPosition-6)) - 2;
        gcImage.setForeground(MainWindow.red);
        gcImage.drawText(formater.format(computeAverage(currentPosition-6)),bounds.width - 65,height - 12,true);
      }
      
      
      gcImage.dispose();
    }
  }
  
  protected int computeAverage(int position) {
    int sum = 0;
    for(int i = -5 ; i < 6 ; i++) {
      int pos = position + i;
      if (pos < 0)
        pos += 2000;
      if(pos >= 2000)
        pos -= 2000;
      sum += values[pos];
    }
    return (sum / 11);
    
  }
  
  

}
