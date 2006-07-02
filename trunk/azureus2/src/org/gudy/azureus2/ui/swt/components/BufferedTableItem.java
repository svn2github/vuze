/*
 * Created on Jun 12, 2006 11:34:42 PM
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.components;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Table;

/**
 * @author TuxPaper
 * @created Jun 12, 2006
 *
 */
public interface BufferedTableItem
{

	public abstract String getText();

	public abstract boolean setText(String text);

	public abstract void setImage(Image img);

	public abstract void setRowForeground(Color color);

	public abstract boolean setForeground(Color color);

	public abstract boolean setForeground(int red, int green, int blue);

	public abstract Color getBackground();

	public abstract Rectangle getBounds();

	public abstract void refresh();

	public abstract void dispose();

	public abstract boolean isShown();

	public abstract boolean needsPainting();

	/** Paint the image only (no update needed)
	 */
	public abstract void doPaint(GC gc);

	/** Column location (not position) changed.  Usually due to a resize of
	 * a column in a position prior to this one.
	 */
	public abstract void locationChanged();

	public abstract int getPosition();

	/**
	 * 
	 */
	public abstract Image getBackgroundImage();

	/**
	 * @return
	 */
	public abstract Color getForeground();

}