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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;

/**
 * @author Olivier
 *
 */
public class SpeedGraphic extends ScaledGraphic implements ParameterListener {    
  
  private int internalLoop;
  private int graphicsUpdate;
  private Point oldSize;
  
  protected Image bufferImage;
  
  private int average = 0;
  private int nbValues = 0;
  
  private int[] values;
  private int[] targetValues;
  private int currentPosition;
  
  
  
  private SpeedGraphic(Scale scale,ValueFormater formater) {
    super(scale,formater);
    
    currentPosition = 0;
    values = new int[2000];
    targetValues = new int[2000];
    
    COConfigurationManager.addParameterListener("Graphics Update",this);
    parameterChanged("Graphics Update");
  }
  
  public static SpeedGraphic getInstance() {
    return new SpeedGraphic(new Scale(),new ValueFormater() {
      public String format(int value) {
        return DisplayFormatters.formatByteCountToBase10KBEtcPerSec(value);
      }
    });
  }
  
  public void addIntsValue(int value,int targetValue) {  	
    try{
    	this_mon.enter();
    
	    average += value - values[currentPosition];
	    targetValues[currentPosition] = targetValue;
	    values[currentPosition++] = value;
	    	    
	    if(nbValues < 2000)
	      nbValues++;
	    
	    if(currentPosition >= values.length)
	      currentPosition = 0;
	    
    }finally{
    	
    	this_mon.exit();
    }
  }
  
  public void addIntValue(int value) {
    try{
    	this_mon.enter();
    
	    average += value - values[currentPosition];
	    values[currentPosition++] = value;
	    
	    if(nbValues < 2000)
	      nbValues++;
	    
	    if(currentPosition >= values.length)
	      currentPosition = 0;
	    
    }finally{
    	
    	this_mon.exit();
    }
  }
  
  public void refresh() {  
    if(drawCanvas == null || drawCanvas.isDisposed())
      return;
    
    Rectangle bounds = drawCanvas.getClientArea();
    if(bounds.height < 30 || bounds.width  < 100 || bounds.width > 1600 || bounds.height > 1200)
      return;
    
    boolean sizeChanged = (oldSize == null || (oldSize.x != bounds.width && oldSize.y != bounds.height));
    oldSize = new Point(bounds.width,bounds.height);
    
    internalLoop++;
    if(internalLoop > graphicsUpdate)
      internalLoop = 0;
    
    
    if(internalLoop == 0 || sizeChanged) {
	    drawChart(sizeChanged);
    }
    
    GC gc = new GC(drawCanvas);
    gc.drawImage(bufferImage,bounds.x,bounds.y);
    gc.dispose();    
  }
  
  protected void drawChart(boolean sizeChanged) {
   try{
   	  this_mon.enter();
   		
      drawScale(sizeChanged);
      
      Rectangle bounds = drawCanvas.getClientArea();    
        
      //If bufferedImage is not null, dispose it
      if(bufferImage != null && ! bufferImage.isDisposed())
        bufferImage.dispose();
      
      bufferImage = new Image(drawCanvas.getDisplay(),bounds);
      
      GC gcImage = new GC(bufferImage);
      
      gcImage.drawImage(bufferScale,0,0);
      
      int oldAverage = 0;   
      int oldTargetValue = 0;
      Color background = Colors.blues[Colors.BLUES_DARKEST];
      Color foreground = Colors.blues[Colors.BLUES_MIDLIGHT];
      int max = 0;
      for(int x = 0 ; x < bounds.width - 71 ; x++) {
        int position = currentPosition - x -1;
        if(position < 0)
          position+= 2000;
        int value = values[position];
        if(value > max) max = value;
        value = targetValues[position];
        if(value > max) max = value;
      }
      scale.setMax(max);
      int maxHeight = scale.getScaledValue(max);
      for(int x = 0 ; x < bounds.width - 71 ; x++) {
        int position = currentPosition - x -1;
        if(position < 0)
          position+= 2000;
        int value = values[position];
        
        int xDraw = bounds.width - 71 - x;
        int height = scale.getScaledValue(value);        
        gcImage.setForeground(background);
        gcImage.setBackground(foreground); 
        gcImage.setClipping(xDraw,bounds.height - 1 - height,1, height);
        gcImage.fillGradientRectangle(xDraw,bounds.height - 1 - maxHeight,1, maxHeight,true);
        gcImage.setClipping(0,0,bounds.width, bounds.height);
        
        int targetValue = targetValues[position];
        if(x > 1 && targetValue > 0) {
        	int h1 = bounds.height - scale.getScaledValue(targetValue) - 2;
            int h2 = bounds.height - scale.getScaledValue(oldTargetValue) - 2;
            gcImage.setForeground(Colors.blue);
            gcImage.drawLine(xDraw,h1,xDraw+1, h2);
        }
        oldTargetValue = targetValue;
        
        int average = computeAverage(position);
        if(x > 6) {
          int h1 = bounds.height - scale.getScaledValue(average) - 2;
          int h2 = bounds.height - scale.getScaledValue(oldAverage) - 2;
          gcImage.setForeground(Colors.red);
          gcImage.drawLine(xDraw,h1,xDraw+1, h2);
        }
        oldAverage = average;
      }  
      
      if(nbValues > 0) {
        int height = bounds.height - scale.getScaledValue(computeAverage(currentPosition-6)) - 2;
        gcImage.setForeground(Colors.red);
        gcImage.drawText(formater.format(computeAverage(currentPosition-6)),bounds.width - 65,height - 12,true);
      }    
      
      gcImage.dispose();

    }finally{
    	
    	this_mon.exit();
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
  
  public void parameterChanged(String parameter) {
    graphicsUpdate = COConfigurationManager.getIntParameter("Graphics Update");
  }
  
  public void dispose() {
    super.dispose();
    if(bufferImage != null && ! bufferImage.isDisposed()) {
      bufferImage.dispose();
    }
    COConfigurationManager.removeParameterListener("Graphics Update",this);
  }

}
