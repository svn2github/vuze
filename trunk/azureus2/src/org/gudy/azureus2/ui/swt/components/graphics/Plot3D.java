/*
 * Created on Jun 26, 2007
 * Created by Paul Gardner
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */


package org.gudy.azureus2.ui.swt.components.graphics;

import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.HSLColor;

public class 
Plot3D
	implements Graphic, ParameterListener
{
	private Canvas		canvas;
	
	private String[]		labels;
	private ValueFormater[]	formatters;
	
	private int internalLoop;
	private int graphicsUpdate;
	private Point oldSize;

	protected Image bufferImage;

	protected AEMonitor	this_mon	= new AEMonitor( "Plot3D" );

	private int[][]		values = new int[0][];
	
	private Color[]		colours;
	
	public
	Plot3D(
		String[]		_labels,
		ValueFormater[]	_formatters )
	{
		labels		= _labels;
		formatters	= _formatters;

		COConfigurationManager.addAndFireParameterListener( "Graphics Update", this );
		
		parameterChanged("Graphics Update");
	}
	
	public void 
	initialize(
		Canvas 		_canvas )
	{
		canvas 	= _canvas;
		
		Device device = canvas.getDisplay();
		
		colours = new Color[16];
		
		HSLColor hsl = new HSLColor();
		
		hsl.initHSLbyRGB( 130,240,240 );
		
		int	step = 128 / colours.length;
		
		int	hue = colours.length * step;
		
		for (int i=0;i<colours.length;i++){
			
			hsl.setHue( hue );
			
			hue -= step;
			
			colours[i] = new Color( device, hsl.getRed(), hsl.getGreen(), hsl.getBlue());
		}
	}

	public void 
	refresh()
	{
		if ( canvas == null || canvas.isDisposed()){
			
			return;
		}
		
		Rectangle bounds = canvas.getClientArea();
		
		if(bounds.height < 30 || bounds.width  < 100 || bounds.width > 2000 || bounds.height > 2000)
			return;

		boolean sizeChanged = (oldSize == null || oldSize.x != bounds.width || oldSize.y != bounds.height);

		oldSize = new Point(bounds.width,bounds.height);

		internalLoop++;

		if(internalLoop > graphicsUpdate){

			internalLoop = 0;
		}

		if (internalLoop == 0 || sizeChanged ){
			
			drawPlot();
		}

		GC gc = new GC(canvas);
		
		gc.drawImage(bufferImage,bounds.x,bounds.y);
		
		gc.dispose();   
	}
	
	
	protected void 
	drawPlot()
	{	
		final int	PAD_TOP		= 10;
		final int	PAD_BOTTOM	= 10;
		final int	PAD_RIGHT	= 10;
		final int	PAD_LEFT	= 10;
		
		final double ANGLE_RADS = 0.7;
		
		final double ANGLE_TAN = Math.tan( ANGLE_RADS );
		
		try{
			this_mon.enter();

			Rectangle bounds = canvas.getClientArea();    

			if ( bufferImage != null && ! bufferImage.isDisposed()){
				
				bufferImage.dispose();
			}
			
			bufferImage = new Image( canvas.getDisplay(), bounds );

			GC image = new GC( bufferImage );

			int	max_x = 0;
			int	max_y = 0;
			int	max_z = 0;
			
			for (int i=0;i<values.length;i++){
				
				int[]	entry = (int[])values[i];
				
				if ( entry[0] > max_x ){
					
					max_x = entry[0];
				}
				if ( entry[1] > max_y ){
					
					max_y = entry[1];
				}
				if ( entry[2] > max_z ){
				
					max_z = entry[2];
				}
			}
			
			
			int usable_width 	= bounds.width - PAD_LEFT - PAD_RIGHT;
			int usable_height	= bounds.height - PAD_TOP - PAD_BOTTOM;
			
			image.setAntialias( SWT.ON );
			
			double	x_ratio = ((float)usable_width-((usable_height/2)/ANGLE_TAN)) / max_x;
			double	y_ratio = ((float)usable_height/2) / max_y;
			double	z_ratio = ((float)usable_height/2) / max_z;
			
			for (int i=0;i<values.length;i++){
				
				int[]	entry = (int[])values[i];
				
				int	draw_x = (int)( x_ratio * entry[0] );
				int	draw_y = (int)( y_ratio * entry[1] );
				int	draw_z = (int)( z_ratio * entry[2] );
				
				draw_x += draw_y / ANGLE_TAN;
				
				image.setForeground( colours[(int)(((float)entry[2]/max_z)*(colours.length-1))]);
			
				image.drawLine( 
						PAD_LEFT + draw_x, 
						PAD_TOP + usable_height - draw_y, 
						PAD_LEFT + draw_x, 
						PAD_TOP + usable_height - ( draw_y + draw_z ));
			}
				
			image.setForeground( Colors.black );
			
			image.drawRectangle( bounds.x, bounds.y, bounds.width-1, bounds.height-1 );
			
			int	font_height = image.getFontMetrics().getHeight();
			int char_width	= image.getFontMetrics().getAverageCharWidth();
			
				// x axis
			
			int arrow_x = PAD_LEFT + usable_width;
			int arrow_y	= usable_height + PAD_TOP;
			
			image.drawLine( PAD_LEFT, usable_height + PAD_TOP, arrow_x, arrow_y );
			image.drawLine( usable_width, usable_height + PAD_TOP - 4, arrow_x, arrow_y );
			image.drawLine( usable_width, usable_height + PAD_TOP + 4, arrow_x, arrow_y );

			String x_text = labels[0] + " - " + formatters[0].format( max_x );
			
			image.drawText( x_text, arrow_x - 20 - x_text.length()*char_width, arrow_y - font_height - 2 );
			
				// z axis
			
			String z_text = labels[2] + " - " + formatters[2].format( max_z );
			
			image.drawText( z_text, PAD_LEFT + 4, PAD_TOP + 10 );
			
			image.drawLine( PAD_LEFT, usable_height + PAD_TOP, PAD_LEFT, PAD_TOP );
			image.drawLine( PAD_LEFT-4, PAD_TOP + PAD_BOTTOM, PAD_LEFT, PAD_TOP );
			image.drawLine( PAD_LEFT+4, PAD_TOP + PAD_BOTTOM, PAD_LEFT, PAD_TOP );

				// y axis
			
			arrow_x = PAD_LEFT + (int)((usable_height/2) / ANGLE_TAN );
			arrow_y = usable_height / 2;
			
			image.drawLine( PAD_LEFT, usable_height + PAD_TOP, 	arrow_x, arrow_y );
			
			image.drawLine( arrow_x-6, arrow_y,	arrow_x, arrow_y );
			
			image.drawLine( arrow_x, arrow_y + 6, arrow_x, arrow_y );

			String	y_text = labels[1] + " - " + formatters[1].format( max_y );
			
			image.drawText( y_text, arrow_x - (y_text.length() * char_width), arrow_y - font_height - 2);

			image.dispose();

		}finally{

			this_mon.exit();
		}
	}
	 
	public void 
	parameterChanged(
		String parameter) 
	{
		graphicsUpdate = COConfigurationManager.getIntParameter("Graphics Update");
	}

	public void 
	dispose() 
	{		
		if ( bufferImage != null && ! bufferImage.isDisposed()){
			
			bufferImage.dispose();
		}
		
		if ( colours != null ){
			
			for (int i=0;i<colours.length;i++){
				
				colours[i].dispose();
			}
		}
		
		COConfigurationManager.removeParameterListener("Graphics Update",this);
	}
	
	public void
	update(
		int[][]	_values )
	{
		try{
			this_mon.enter();
		
			values = _values;
			
		}finally{

			this_mon.exit();
		}
	}
}
