/*
 * Created on 22 juin 2005
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
package org.gudy.azureus2.ui.swt.views.stats;

import java.util.Iterator;
import java.util.List;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import com.aelitis.azureus.core.dht.vivaldi.maths.VivaldiPosition;
import com.aelitis.azureus.core.dht.vivaldi.maths.impl.HeightCoordinatesImpl;

public class VivaldiPanel {
  
  Display display;
  Composite parent;
  
  Canvas canvas;
  Scale scale;
  
  private class Scale {
    int width;
    int height;
    
    float minX;
    float minY;
    
    float maxX;
    float maxY;    
    float maxH;      
    
    float sin45 = (float) Math.sin(Math.PI/4);
    float cos45 = (float) Math.cos(Math.PI/4);
    
    float vMinX;
    float vMinY;
    float vRatioX;
    float vRatioY;
    float xMinY;
    float yminX;
    
    public void refreshScaleParams() {
      xMinY = cos45 * (maxY - minY);
      vMinX = minX - xMinY;
      vMinY = minY - maxH / cos45;
      vRatioX = (float)width /(maxX - vMinX);
      vRatioY = (float)height / (maxY - vMinY);
    }
    
    public int getX(float x,float y) {
      //return (int) ((x-vMinX) * vRatioX - xMinY * (y-maxY) / (maxY-minY)); 
      return (int) ((x+500)/(1000) * width);
    }
    
    public int getY(float x,float y) {
      return (int) ((y+500)/(1000) * height);
    }
  }
  
  public VivaldiPanel(Composite parent) {
    this.parent = parent;
    this.display = parent.getDisplay();
    this.canvas = new Canvas(parent,SWT.NULL);   
    this.scale = new Scale();
  }
  
  public void setLayoutData(Object data) {
    canvas.setLayoutData(data);
  }
  
  public void refresh(List vivaldiPositions) {
    Rectangle size = canvas.getBounds();
    
    scale.width = size.width;
    scale.height = size.height;
    
    Image img = new Image(display,size);
    GC gc = new GC(img);
    Color white = new Color(display,255,255,255);
    gc.setForeground(white);
    gc.setBackground(white);
    gc.fillRectangle(size);
    
    Color blue = new Color(display,66,87,104);
    gc.setForeground(blue);
    gc.setBackground(blue);
    
    Iterator iter = vivaldiPositions.iterator();
    while(iter.hasNext()) {
      HeightCoordinatesImpl coord = (HeightCoordinatesImpl) ((VivaldiPosition)iter.next()).getCoordinates();
      draw(gc,coord.getX(),coord.getY(),coord.getH());
    }
    
    gc.dispose();
    
    gc = new GC(canvas);
    gc.drawImage(img,0,0);
    gc.dispose();
    img.dispose();
    white.dispose();
    blue.dispose();
  }
  
  private void draw(GC gc,float x,float y,float h) {
    int x0 = scale.getX(x,y);
    int y0 = scale.getY(x,y);   
    gc.fillRectangle(x0-1,y0-1,3,3);    
  }
  
  private void drawBorder(GC gc) {
    
  }
}
