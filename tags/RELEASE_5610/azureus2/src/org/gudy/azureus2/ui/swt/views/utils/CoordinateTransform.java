/*
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
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

package org.gudy.azureus2.ui.swt.views.utils;

import org.eclipse.swt.graphics.Rectangle;

public class CoordinateTransform
{
		public CoordinateTransform(Rectangle exteriorBounds)
		{
			extWidth = exteriorBounds.width;
			extHeight = exteriorBounds.height;
		}
	
		final int extWidth;
		final int extHeight;
		
		int offsetX = 0;
		int offsetY = 0;
		double scaleX = 1.0;
		double scaleY = 1.0;
		
		public int x(int x)
		{
			return (int)(Math.round(offsetX+x*scaleX));
		}
		
		public int y(int y)
		{
			return (int)(Math.round(offsetY+y*scaleY));
		}

        private int w(int w)
		{
			return (int)Math.round(w*scaleX);
		}		

        private int h(int h)
		{
			return (int)Math.ceil(h*scaleY);
		}
		
		public void scale(double x, double y)
		{
			scaleX *= x;
			scaleY *= y;
		}
		
		public void shiftExternal(int x, int y)
		{
			offsetX += x;
			offsetY += y;
		}
		
		
		public void shiftInternal(int x, int y)
		{
			offsetX += x*scaleX;
			offsetY += y*scaleY;
		}

        private void calcFromDimensions(int internalWidth, int internalHeight, int marginLeft, int marginRight, int marginTop, int marginBottom, boolean leftToRight, boolean topDown)
		{
			shiftExternal(leftToRight ? 0 : extWidth,topDown ? 0 : extHeight);
			scale(leftToRight ? 1.0 : -1.0, topDown ? 1.0 : -1.0);
			shiftInternal(leftToRight ? marginLeft : marginRight, topDown ? marginTop : marginBottom);
			scale(Math.round((extWidth-marginLeft-marginRight)/(1.0*internalWidth)),Math.round((extHeight-marginTop-marginBottom)/(1.0*internalHeight)));
		}
	}