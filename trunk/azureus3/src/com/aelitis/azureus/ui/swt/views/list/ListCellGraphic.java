/*
 * Created on Jul 2, 2006 2:07:56 PM
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
package com.aelitis.azureus.ui.swt.views.list;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.ui.swt.components.BufferedGraphicTableItem;

/**
 * @author TuxPaper
 * @created Jul 2, 2006
 *
 */
public class ListCellGraphic extends ListCell implements
		BufferedGraphicTableItem
{
	private int marginHeight = 1;

	private int marginWidth = 1;

	private int orientation = SWT.CENTER;

	private Image image;

	private Rectangle imageBounds;

	public ListCellGraphic(ListRow row, int position, int alignment,
			Rectangle bounds) {
		super(row, position, alignment, bounds);
	}

	public Image getGraphic() {
		return image;
	}

	public Point getSize() {
		Rectangle bounds = getBounds();
		return new Point(bounds.width - (marginWidth * 2), bounds.height
				- (marginHeight * 2));
	}

	public boolean setGraphic(Image img) {
		//System.out.println(img.getBounds());
		if (img != null && img.isDisposed()) {
			return false;
		}
		
		if (image == img) {
			return false;
		}

		image = img;
		if (image != null) {
			imageBounds = image.getBounds();
		}
		
		((ListView)row.getView()).cellRefresh(this, true, true);
		
		return true;
	}

	public int getMarginHeight() {
		return marginHeight;
	}

	public int getMarginWidth() {
		return marginWidth;
	}

	public void setMargin(int width, int height) {
		if (width >= 0) {
			marginWidth = width;
		}

		if (height >= 0) {
			marginHeight = height;
		}
	}

	public int getOrientation() {
		return orientation;
	}

	public void setOrientation(int orientation) {
		this.orientation = orientation;
	}

	public void doPaint(GC gc) {
		if (getPosition() < 0) {
			return;
		}

		gc.setBackground(getBackground());
		if (DEBUG_COLORCELL) {
			gc.setBackground(Display.getDefault().getSystemColor((int)(Math.random() * 16)));
		}
		gc.fillRectangle(getBounds());

		// TODO: Orientation: fill
		if (image != null && !image.isDisposed()) {
			Point size = getSize();
			//System.out.println(bounds);

			int x;

			int y = marginHeight;
			y += (size.y - imageBounds.height) / 2;

			if (orientation == SWT.CENTER) {
				x = marginWidth;
				x += (size.x - (marginWidth * 2) - imageBounds.width) / 2;
			} else if (orientation == SWT.RIGHT) {
				x = bounds.height - marginWidth - imageBounds.width;
			} else {
				x = marginWidth;
			}

			int width = Math.min(bounds.width - x - marginWidth, imageBounds.width);
			int height = Math.min(bounds.height - y - marginHeight,
					imageBounds.height);

			if (width >= 0 && height >= 0) {
				gc.drawImage(image, 0, 0, width, height, bounds.x + x, bounds.y + y,
						width, height);
			}
		}
	}
}
