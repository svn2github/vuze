/*
 * Created on Jul 2, 2006 1:48:46 PM
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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.gudy.azureus2.ui.swt.components.BufferedTableItem;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.views.table.TableCellCore;
import org.gudy.azureus2.ui.swt.views.table.TableColumnCore;

import org.gudy.azureus2.plugins.ui.tables.TableCellVisibilityListener;

/**
 * @author TuxPaper
 * @created Jul 2, 2006
 *
 */
public class ListCell implements BufferedTableItem
{
	private final int position;

	private String sText;

	protected Color colorFG;

	protected Color colorBG;

	protected Rectangle bounds;

	protected final ListRow row;

	private final int alignment;

	private boolean bLastIsShown = false;

	private TableCellCore cell;

	public ListCell(ListRow row, int position, int alignment, Rectangle bounds) {
		this.row = row;
		this.position = position;
		this.alignment = alignment;
		this.bounds = bounds;
	}

	public void dispose() {
		// TODO Auto-generated method stub

	}

	public void doPaint(GC gc) {
		// TODO: Orientation

		if (!isShowable()) {
			return;
		}

		if (sText == null) {
			return;
		}

		if (colorFG != null) {
			gc.setForeground(colorFG);
		}
		if (colorBG != null) {
			gc.setBackground(colorBG);
		}

		//gc.drawText(sText, bounds.x, bounds.y);
		GCStringPrinter.printString(gc, sText, bounds, true, true, alignment
				| SWT.WRAP);
	}

	/**
	 * Whether the column is set to be showable
	 * @return
	 */
	private boolean isShowable() {
		return position >= 0 && bounds != null && bounds.height > 0;
	}

	public Color getBackground() {
		if (colorBG == null) {
			return row.getBackground();
		}

		return colorBG;
	}

	public Rectangle getBounds() {
		return bounds;
	}

	/**
	 * @param bounds the bounds to set
	 */
	public void setBounds(Rectangle bounds) {
		this.bounds = bounds;
	}

	public int getPosition() {
		return position;
	}

	public String getText() {
		return sText;
	}

	public boolean isShown() {
		boolean bIsShown = isShowable() && row.isVisible()
				&& row.getComposite().getClientArea().intersects(bounds);
		if (bIsShown != bLastIsShown) {
			bLastIsShown = bIsShown;
			if (cell != null) {
				int mode = bIsShown ? TableCellVisibilityListener.VISIBILITY_SHOWN
						: TableCellVisibilityListener.VISIBILITY_HIDDEN;
				((TableColumnCore) cell.getTableColumn()).invokeCellVisibilityListeners(
						cell, mode);
				cell.invokeVisibilityListeners(mode);
			}
		}
		return bIsShown;
	}

	public void locationChanged() {
		// TODO Auto-generated method stub

	}

	public boolean needsPainting() {
		return isShown();
	}

	public void refresh() {
		// TODO Auto-generated method stub

	}

	public void setIcon(Image img) {
		// TODO Auto-generated method stub
	}

	public Image getIcon() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean setForeground(Color color) {
		colorFG = color;
		return true;
	}

	public boolean setForeground(int red, int green, int blue) {
		// TODO Auto-generated method stub
		return false;
	}

	public void setRowForeground(Color color) {
		// TODO Auto-generated method stub

	}

	public boolean setText(String text) {
		if (sText == null && text == null) {
			return false;
		}
		if (sText != null && text != null && (sText == text || sText.equals(text))) {
			return false;
		}

		sText = text;
		redrawCell();
		//System.out.println("TEXT SET " + text);
		return true;
	}

	protected void redrawCell() {
		if (!isShown()) {
			return;
		}
		row.getComposite().redraw(bounds.x, bounds.y, bounds.width, bounds.height,
				false);
	}

	public Image getBackgroundImage() {
		return null;
	}

	public Color getForeground() {
		if (colorFG == null) {
			return row.getForeground();
		}

		return colorFG;
	}

	public ListRow getRow() {
		return row;
	}

	/**
	 * @param cell
	 */
	public void setTableCell(TableCellCore cell) {
		this.cell = cell;
	}

}
