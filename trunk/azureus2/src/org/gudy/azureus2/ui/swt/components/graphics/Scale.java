/*
 * File    : Scale.java
 * Created : 15 déc. 2003}
 * By      : Olivier
 *
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
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

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;

/**
 * @author Olivier
 *
 */
public class Scale {
  
  private static boolean scaleBinary;
  
  static{
	  COConfigurationManager.addAndFireParameterListener(
			 "ui.scaled.graphics.binary.based",
			 new ParameterListener() {
				
				public void parameterChanged(String name) {
					scaleBinary = COConfigurationManager.getBooleanParameter( name );
				}
			});
  }
  //The target number of pixels per scale level
  private int pixelsPerLevel = 50;
      
  //The max value
  private int max = 1;
  
  //The displayed number of levels
  private int nbLevels;
  
  //The computed (dislayed max)
  private int displayedMax;
  
  
  //The number of pixels
  private int nbPixels = 1;
  
  double 	scaleFactor;
  int 		powFactor;
  
  public void setMax(int max) {
    this.max = max;  
    if(max < 1)
      max = 1;
    computeValues();
  }
  
  public int getMax() {
    return this.max;
  }
  
  public void setNbPixels(int nbPixels) {
    this.nbPixels = nbPixels;
    if(nbPixels < 1)
      nbPixels = 1;
    computeValues();
  }
  
  private void computeValues() {
    int targetNbLevels = nbPixels / pixelsPerLevel;
    if(targetNbLevels < 1)
      targetNbLevels = 1;
    scaleFactor = max / targetNbLevels;
    powFactor = 1;
  
    int scaleThing = scaleBinary?8:10;
    while(scaleFactor >= scaleThing) {
      powFactor = scaleThing * powFactor;
      scaleFactor = scaleFactor / scaleThing;
    }
   
    double scaleMax = scaleBinary?4:5;
    if(scaleFactor >= scaleMax){
      scaleFactor = scaleMax;
    }else if(scaleFactor >= 2){
      scaleFactor = scaleMax/2;
    }else{
      scaleFactor = 1;
    }
    nbLevels = (int)(max / ( scaleFactor * powFactor) + 1);
    displayedMax = (int)( scaleFactor * powFactor * nbLevels );    
  }
  
  
  public int[] getScaleValues() {
    int[] result = new int[nbLevels+1];
    for(int i = 0 ; i < nbLevels + 1 ; i++) {
      result[i] = (int)( i * scaleFactor * powFactor );
    }
    return result;
  }
  
  public int getScaledValue(int value) {
    return(int)( ((long)value * nbPixels) / displayedMax );
  }
  
}
