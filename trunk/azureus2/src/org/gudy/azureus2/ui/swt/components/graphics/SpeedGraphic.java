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

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
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
        return DisplayFormatters.formatByteCountToKBEtcPerSec(value);
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
      for(int x = 0 ; x < bounds.width - 71 ; x++) {
        int position = currentPosition - x;
        if(position < 0)
          position+= 2000;
        int value = values[position];
        int xDraw = bounds.width - 71 - x;
        int height = bounds.height - scale.getScaledValue(value) - 2;
        
        gcImage.setForeground(MainWindow.blues[3]);
        gcImage.drawLine(xDraw,bounds.height - 2,xDraw, height);
        
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
