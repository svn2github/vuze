/*
 * Created on 19 nov. 2004
 * Created by Olivier Chalouhi
 * 
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
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
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.components.graphics;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;

import org.gudy.azureus2.ui.swt.mainwindow.Colors;

/**
 * @author Olivier Chalouhi
 *
 */
public class PieUtils {

	public static void drawPie(GC gc,int x, int y,int width,int height,int percent) {
	    Color background = gc.getBackground();
	    gc.setForeground(Colors.blue);
	    int angle = (percent * 360) / 100;
	    if(angle<4)
	    	angle = 0; // workaround fillArc rendering bug
	    gc.setBackground(Colors.white);
	    gc.fillArc(x,y,width,height,0,360);
	    gc.setBackground(background);
	    gc.fillArc(x,y,width,height,90,angle*-1);
	    gc.drawOval(x , y , width-1, height-1);
	}
  
	public static void 
	drawPie(
		GC gc,Image image, int x, int y,int width,int height,int percent, boolean draw_border ) 
	{
		Rectangle image_size = image.getBounds();
		
		int	width_pad 	= ( width - image_size.width  )/2;
		int	height_pad 	= ( height - image_size.height  )/2;
		
	    int angle = (percent * 360) / 100;
	    if(angle<4){
	    	angle = 0; // workaround fillArc rendering bug
	    }
	    
		Region old_clipping = new Region();

		gc.getClipping(old_clipping);
		
		Path path_done = new Path(gc.getDevice());
		
		path_done.addArc(x,y,width,height,90,-angle);
		path_done.lineTo( x+width/2, y+height/2);
		path_done.close();
		
		gc.setClipping( path_done );
		
		gc.drawImage(image, x+width_pad, y+height_pad+1);
				
		Path path_undone = new Path(gc.getDevice());
		
		path_undone.addArc(x,y,width,height,90-angle,angle-360);
		path_undone.lineTo( x+width/2, y+height/2);
		path_undone.close();
		
		gc.setClipping( path_undone );
		
		gc.setAlpha( 75 );
		gc.drawImage(image, x+width_pad, y+height_pad+1);
		gc.setAlpha( 255 );
	
		gc.setClipping( old_clipping );
		
		if ( draw_border ){
			
			gc.setForeground(Colors.blue);
			
			if ( percent == 100 ){
				
				gc.drawOval(x , y , width-1, height-1);
				
			}else{
				
				if ( angle > 0 ){
					
					gc.drawPath( path_done );
				}
			}
		}
		
		path_done.dispose();
		path_undone.dispose();
		old_clipping.dispose();

	}
}
