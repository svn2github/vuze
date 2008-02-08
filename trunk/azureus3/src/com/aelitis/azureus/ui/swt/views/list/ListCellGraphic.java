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
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.ui.swt.components.BufferedGraphicTableItem;

/**
 * @author TuxPaper
 * @created Jul 2, 2006
 *
 */
public class ListCellGraphic
	extends ListCell
	implements BufferedGraphicTableItem
{
	// XXX Must remain 1 as plugin docs say so
	private int marginHeight = 1;

	// XXX Must remain 1 as plugin docs say so
	private int marginWidth = 1;

	private int orientation = SWT.CENTER;

	private Image image;

	private Rectangle imageBounds;

	public ListCellGraphic(ListRow row, int alignment, Rectangle bounds) {
		super(row, alignment, bounds);
	}

	public Image getGraphic() {
		return image;
	}

	public Point getSize() {
		Rectangle bounds = getBounds();
		if (bounds == null) {
			return new Point(0, 0);
		}
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

		if (row != null) {
			((ListView) row.getView()).cellRefresh(this, true, true);
		}

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
		if (!isShown()) {
			return;
		}

		Color colorBG = getBackground();
		if (colorBG != null) {
			gc.setBackground(colorBG);
		}
		if (DEBUG_COLORCELL) {
			gc.setBackground(Display.getDefault().getSystemColor(
					(int) (Math.random() * 16)));
		}
		Rectangle bounds = getBounds();
		if (bounds == null) {
			return;
		}
		gc.fillRectangle(bounds);

		// TODO: Orientation: fill
		if (image != null && !image.isDisposed()) {
			// size is without margin
			Point size = getSize();
			//System.out.println(bounds);

			int x;

			int y = marginHeight;
			y += (size.y - imageBounds.height) / 2;

			if (orientation == SWT.CENTER) {
				x = marginWidth;
				x += (size.x - imageBounds.width) / 2;
			} else if (orientation == SWT.RIGHT) {
				x = size.x - imageBounds.width;
			} else {
				x = marginWidth;
			}

			int width = Math.min(size.x - x, imageBounds.width);
			int height = Math.min(size.y - y, imageBounds.height);

			if (width >= 0 && height >= 0) {
				gc.drawImage(image, 0, 0, width, height, bounds.x + x, bounds.y + y,
						width, height);
			}
		}
	}

	public Image getBackgroundImage() {
		Rectangle bounds = getBounds();

		if (bounds == null || bounds.isEmpty()) {
			return null;
		}

		Image image = new Image(Display.getDefault(), bounds.width
				- (marginWidth * 2), bounds.height - (marginHeight * 2));

		GC gc = new GC(image);
		Color colorBG = getBackground();
		if (colorBG != null) {
			gc.setBackground(colorBG);
		}
		gc.fillRectangle(image.getBounds());
		gc.dispose();

		//GC gc = new GC(composite);
		//gc.copyArea(image, bounds.x, bounds.y);
		//gc.dispose();

		return image;
	}
}
