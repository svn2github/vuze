/*
 * Created on 24 oct. 2004
 * Created by Olivier Chalouhi
 * 
 * Copyright (C) 2004 Aelitis SARL, All rights Reserved
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
 * 
 * AELITIS, SARL au capital de 30,000 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.components.images;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

/**
 * @author Olivier Chalouhi
 *
 */
public class Utils {
  
  public static Image renderTransparency(Display display,Image background,Image foreground) {
    //Checks
    if( display == null || display.isDisposed() ||
        background == null || background.isDisposed() ||        
        foreground == null || foreground.isDisposed()
       ) return null;
    Rectangle r1 = background.getBounds();
    Rectangle r2 = foreground.getBounds();
    if(! r1.equals(r2)) return null;
    
    Image image = new Image(display,r1);
    ImageData backData = background.getImageData();
    ImageData foreData = foreground.getImageData();
    ImageData imgData  = image.getImageData();    
        
    for(int y = 0 ; y < foreData.height ; y++) {
      for(int x = 0 ; x < foreData.width ; x++) {
       int cBack = backData.getPixel(x,y);       
       int cFore = foreData.getPixel(x,y);
       int aFore = foreData.getAlpha(x,y);
       
       int rBack =  cBack         & 0xFF;
       int gBack = (cBack >> 8)  & 0xFF;
       int bBack = (cBack >> 16) & 0xFF;
       
       int rFore =  cFore         & 0xFF;
       int gFore = (cFore >> 8)  & 0xFF;
       int bFore = (cFore >> 16) & 0xFF;
       
       int r = (rBack * aFore + (255 - aFore) * rFore) / 255;
       r = r & 0xFF;
       int g = (gBack * aFore + (255 - aFore) * gFore) / 255;
       g = g & 0xFF;
       int b = (bBack * aFore + (255 - aFore) * bFore) / 255;
       b = b & 0xFF;
       
       int color = r + g << 8 + b << 16;
       imgData.setPixel(x,y,color);
      }
    }
    return image;
  }
}
