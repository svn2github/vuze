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
  
  private static final int	ENTRIES	= 2000;
  
  public static Color	color_average		= Colors.red;
  public static Color	color_value0		= Colors.blues[Colors.BLUES_MIDDARK];	// approx
  public static Color	color_value1		= Colors.blue;
  public static Color	color_value2plus	= Colors.grey;
  public static Color	color_trimmed		= Colors.light_grey;
  
  private int internalLoop;
  private int graphicsUpdate;
  private Point oldSize;
  
  protected Image bufferImage;
  
  private int average = 0;
  private int nbValues = 0;
  
  private int[][] all_values	= new int[1][ENTRIES];
  private int currentPosition;
  
   
  
  
  private SpeedGraphic(Scale scale,ValueFormater formater) {
    super(scale,formater);
    
    currentPosition = 0;
    
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
  
  public void addIntsValue(int[] new_values) {  	
    try{
    	this_mon.enter();
    
    	if ( all_values.length < new_values.length ){
    		
    		int[][]	new_all_values = new int[new_values.length][];
    		
    		for (int i=0;i<all_values.length;i++){
    		
    			new_all_values[i] = all_values[i];
    		}
    		
    		for (int i=all_values.length;i<new_all_values.length; i++ ){
    			
    			new_all_values[i] = new int[ENTRIES];
    		}
    		
    		all_values = new_all_values;
    	}
    	
	    average += new_values[0] - all_values[0][currentPosition];
	    
	    for (int i=0;i<new_values.length;i++){
    		
	        all_values[i][currentPosition] = new_values[i];
    	}
	  	
	    currentPosition++;
	    
	    if(nbValues < ENTRIES){
	    	
	      nbValues++;
	    }
	    
	    if(currentPosition >= ENTRIES){
	    	
	      currentPosition = 0;
	    }
	    
    }finally{
    	
    	this_mon.exit();
    }
  }
  
  public void addIntValue(int value) {
	  addIntsValue( new int[]{ value });
  }
  
  public void refresh() {  
    if(drawCanvas == null || drawCanvas.isDisposed())
      return;
    
    Rectangle bounds = drawCanvas.getClientArea();
    if(bounds.height < 30 || bounds.width  < 100 || bounds.width > 2000 || bounds.height > 2000)
      return;
    
    boolean sizeChanged = (oldSize == null || oldSize.x != bounds.width || oldSize.y != bounds.height);
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
      int[] oldTargetValues = new int[all_values.length];
      Color background = Colors.blues[Colors.BLUES_DARKEST];
      Color foreground = Colors.blues[Colors.BLUES_MIDLIGHT];
      int[] maxs = new int[all_values.length];
      for(int x = 0 ; x < bounds.width - 71 ; x++) {
        int position = currentPosition - x -1;
        if(position < 0)
          position+= 2000;
        for (int z=0;z<all_values.length;z++){
        	int value = all_values[z][position];
        	if(value > maxs[z]){
        		maxs[z] = value;
        	}
        }
      }
      int	max = maxs[0];
      int	max_primary = max;
      for (int i=1;i<maxs.length;i++){
    	  int	m = maxs[i];
    	  if ( i == 1 ){
    		  if  ( max < m ){
    			  max 			= m;
    			  max_primary	= max;
    		  }
    	  }else{
    		  	// trim secondary indicators so we don't loose the more important info 
    		  
    		  if ( max < m ){
    			  
    			  if ( m <= 4*max_primary ){
    		  
    				  max = m;
    			  }else{
    				  
    				  max = 4*max_primary;
    				  
    				  break;
    			  }
    		  }
    	  }
      }
      
      scale.setMax(max);
      int maxHeight = scale.getScaledValue(max);
      for(int x = 0 ; x < bounds.width - 71 ; x++) {
        int position = currentPosition - x -1;
        if(position < 0)
          position+= 2000;
        
        int value = all_values[0][position];
        
        int xDraw = bounds.width - 71 - x;
        int height = scale.getScaledValue(value); 
        
        gcImage.setForeground(background);
        gcImage.setBackground(foreground); 
        gcImage.setClipping(xDraw,bounds.height - 1 - height,1, height);
        gcImage.fillGradientRectangle(xDraw,bounds.height - 1 - maxHeight,1, maxHeight,true);
        gcImage.setClipping(0,0,bounds.width, bounds.height);
        
        for (int z=1;z<all_values.length;z++){
	        int targetValue 	= all_values[z][position];
	        int oldTargetValue 	= oldTargetValues[z];
	        
	        if ( x > 1 && 
	        		( z == 1 && ( targetValue > 0 && oldTargetValue > 0 ) ||
	        		( z > 1  && ( targetValue > 0 || oldTargetValue > 0 )))){
	        	
	        	int	trimmed = 0;
	        	if ( targetValue > max ){
	        		targetValue = max;
	        		trimmed++;
	        	}
	        	if ( oldTargetValue > max ){
	        		oldTargetValue = max;
	        		trimmed++;
	        	}
	        	if ( 	trimmed < 2 ||
	        			trimmed == 2 && position % 3 == 0 ){
	        		
		        	int h1 = bounds.height - scale.getScaledValue(targetValue) - 2;
	            	int h2 = bounds.height - scale.getScaledValue(oldTargetValue) - 2;
	            	gcImage.setForeground(z==1?color_value1:(trimmed>0?color_trimmed:color_value2plus));
	            	gcImage.drawLine(xDraw,h1,xDraw+1, h2);
	        	}
	        }
	        
	        oldTargetValues[z] = all_values[z][position];
        }
        
        int average = computeAverage(position);
        if(x > 6) {
          int h1 = bounds.height - scale.getScaledValue(average) - 2;
          int h2 = bounds.height - scale.getScaledValue(oldAverage) - 2;
          gcImage.setForeground(color_average);
          gcImage.drawLine(xDraw,h1,xDraw+1, h2);
        }
        oldAverage = average;
      }  
      
      if(nbValues > 0) {
        int height = bounds.height - scale.getScaledValue(computeAverage(currentPosition-6)) - 2;
        gcImage.setForeground(color_average);
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
      sum += all_values[0][pos];
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
