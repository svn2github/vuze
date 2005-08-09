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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import com.aelitis.azureus.core.dht.control.DHTControlContact;
import com.aelitis.azureus.core.dht.router.DHTRouterContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.vivaldi.maths.Coordinates;
import com.aelitis.azureus.core.dht.vivaldi.maths.VivaldiPosition;
import com.aelitis.azureus.core.dht.vivaldi.maths.impl.HeightCoordinatesImpl;
import com.aelitis.azureus.core.dht.vivaldi.maths.impl.VivaldiPositionImpl;

public class VivaldiPanel {
  
  Display display;
  Composite parent;
  
  Canvas canvas;
  Scale scale;
  
  private boolean mouseLeftDown = false;
  private boolean mouseRightDown = false;
  private int xDown;
  private int yDown;
  
  private boolean antiAliasingAvailable = true;
  
  private class Scale {
    int width;
    int height;
    
    float minX = -1000;
    float maxX = 1000;
    float minY = -1000;
    float maxY = 1000;
    
    float saveMinX;
    float saveMaxX;
    float saveMinY;
    float saveMaxY;    
    
    public int getX(float x,float y) {
      //return (int) ((x-vMinX) * vRatioX - xMinY * (y-maxY) / (maxY-minY)); 
      return (int) ((x-minX)/(maxX - minX) * width);
    }
    
    public int getY(float x,float y) {
      return (int) ((y-minY)/(maxY-minY) * height);
    }
  }
  
  public VivaldiPanel(Composite parent) {
    this.parent = parent;
    this.display = parent.getDisplay();
    this.canvas = new Canvas(parent,SWT.NULL);   
    this.scale = new Scale();
    
    canvas.addMouseListener(new MouseAdapter() {

      public void mouseDown(MouseEvent event) {
        if(event.button == 1) mouseLeftDown = true;
        if(event.button == 3) mouseRightDown = true;
        xDown = event.x;
        yDown = event.y;
        scale.saveMinX = scale.minX;
        scale.saveMaxX = scale.maxX;
        scale.saveMinY = scale.minY;
        scale.saveMaxY = scale.maxY;
      }
      
      public void mouseUp(MouseEvent event) {
        if(event.button == 1) mouseLeftDown = false;
        if(event.button == 3) mouseRightDown = false;
      }                  
    });
    
    canvas.addMouseMoveListener(new MouseMoveListener() {
      public void mouseMove(MouseEvent event) {
        if(mouseLeftDown) {
          int deltaX = event.x - xDown;
          int deltaY = event.y - yDown;
          int width = scale.width;
          int height = scale.height;
          float ratioX = (float) (scale.saveMaxX - scale.saveMinX) / (float) width;
          float ratioY = (float) (scale.saveMaxY - scale.saveMinY) / (float) height;
          float realDeltaX = deltaX * ratioX;
          float realDeltaY  = deltaY * ratioY;
          scale.minX = scale.saveMinX - realDeltaX;
          scale.maxX = scale.saveMaxX - realDeltaX;
          scale.minY = scale.saveMinY - realDeltaY;
          scale.maxY = scale.saveMaxY - realDeltaY;
        }
        if(mouseRightDown) {
          int deltaY = event.y - yDown;
          float scaleFactor = 1 + (float) deltaY / 100;
          if(scaleFactor <= 0) scaleFactor = 0.01f;
          scale.minX = scale.saveMinX * scaleFactor;
          scale.maxX = scale.saveMaxX * scaleFactor;
          scale.minY = scale.saveMinY * scaleFactor;
          scale.maxY = scale.saveMaxY * scaleFactor;
        }
      }
    });
  }
  
  public void setLayoutData(Object data) {
    canvas.setLayoutData(data);
  }
  
  public void refreshContacts(List contacts,DHTTransportContact self) {
    
    if(canvas.isDisposed()) return;
    Rectangle size = canvas.getBounds();
    
    scale.width = size.width;
    scale.height = size.height;
    
    Color white = new Color(display,255,255,255);
    Color blue = new Color(display,66,87,104);
    
    Image img = new Image(display,size);
    
    GC gc = new GC(img);    
    
    gc.setForeground(white);
    gc.setBackground(white);
    
    gc.fillRectangle(size);
    
    if(SWT.getVersion() >= 3138 && antiAliasingAvailable) {
    	try {
    		//gc.setTextAntialias(SWT.ON);
    		//gc.setAntialias(SWT.ON);
      } catch(Exception e) {
        antiAliasingAvailable = false;
      }
    }
    
    
    gc.setForeground(blue);
    gc.setBackground(blue);       
    
    Coordinates ownCoords = self.getVivaldiPosition().getCoordinates();    
    
    Iterator iter = contacts.iterator();
    while(iter.hasNext()) {
      DHTControlContact contact = (DHTControlContact) iter.next();
      VivaldiPosition position = contact.getTransportContact().getVivaldiPosition();
      HeightCoordinatesImpl coord = (HeightCoordinatesImpl) position.getCoordinates();
      if(coord.isValid()) {
        draw(gc,coord.getX(),coord.getY(),coord.getH(),contact,(int)ownCoords.distance(coord),position.getErrorEstimate());
      }
    }
    
    gc.dispose();
    
    gc = new GC(canvas);
    gc.drawImage(img,0,0);
    gc.dispose();
    img.dispose();
    white.dispose();
    blue.dispose();
  }
  
  public void refresh(List vivaldiPositions) {
    if(canvas.isDisposed()) return;
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
      VivaldiPosition position  = (VivaldiPosition)iter.next();
      HeightCoordinatesImpl coord = (HeightCoordinatesImpl) position.getCoordinates();
      
      float error = position.getErrorEstimate() - VivaldiPosition.ERROR_MIN;
      if(error < 0) error = 0;
      if(error > 1) error = 1;
      int blueComponent = (int) (255 - error * 255);
      int redComponent = (int) (255*error);      
      Color drawColor = new Color(display,redComponent,50,blueComponent);      
      gc.setForeground(drawColor);
      draw(gc,coord.getX(),coord.getY(),coord.getH());
      drawColor.dispose();
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
    gc.drawLine(x0,y0,x0,(int)(y0-200*h/(scale.maxY-scale.minY)));
  }
  
  private void draw(GC gc,float x,float y,float h,DHTControlContact contact,int distance,float error) {
    if(x == 0 && y == 0) return;    
    if(error > 1) error = 1;
    int errDisplay = (int) (100 * error);
    int x0 = scale.getX(x,y);
    int y0 = scale.getY(x,y);   
    gc.fillRectangle(x0-1,y0-1,3,3);   
    //int elevation =(int) ( 200*h/(scale.maxY-scale.minY));
    //gc.drawLine(x0,y0,x0,y0-elevation);
    String text = /*contact.getTransportContact().getAddress().getAddress().getHostAddress() + " (" + */distance + " ms \nerr:"+errDisplay+"%";
    int lineReturn = text.indexOf("\n");
    int xOffset = gc.getFontMetrics().getAverageCharWidth() * (lineReturn != -1 ? lineReturn:text.length()) / 2;
    gc.drawText(text,x0-xOffset,y0,true);
  }
  
  private void drawBorder(GC gc) {
    
  }
}
