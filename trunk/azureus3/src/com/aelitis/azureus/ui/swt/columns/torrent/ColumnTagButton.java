/*
 * Created on Jun 16, 2006 5:53:45 PM
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
package com.aelitis.azureus.ui.swt.columns.torrent;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;
import org.gudy.azureus2.ui.swt.plugins.UISWTGraphic;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTGraphicImpl;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.views.table.TableCellCore;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellDisposeListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;

/**
 * @author TuxPaper
 * @created Jun 16, 2006
 *
 */
public class ColumnTagButton extends CoreTableColumn implements
		TableCellAddedListener
{
	private Display display;

	public ColumnTagButton(String sTableID) {
		super("ProgressETA", sTableID);
		initializeAsGraphic(POSITION_LAST, 90);
		setAlignment(ALIGN_CENTER);

		display = SWTThread.getInstance().getDisplay();
	}

	public void cellAdded(TableCell cell) {
		new Cell(cell);
	}

	private class Cell implements TableCellRefreshListener,
			TableCellDisposeListener
	{
		public Cell(TableCell cell) {
			cell.addListeners(this);
			cell.setMarginHeight(2);
			cell.setMarginWidth(0);
			//cell.setFillCell(true);

			int width = 90; // cell.getWidth();
			int height = 26; // cell.getHeight();

			if (width <= 0 || height <= 0) {
				return;
			}

			Image img = new Image(display, width, height);
			GC gc = new GC(img);
			gc.drawRectangle(0, 0, width - 1, height - 1);

			FontData[] fontData = gc.getFont().getFontData();
			int fontHeight = Utils.pixelsToPoint(10, display.getDPI().y);
			fontData[0].setHeight(fontHeight);
			Font font = new Font(display, fontData);
			gc.setFont(font);
			gc.setForeground(display.getSystemColor(SWT.COLOR_DARK_BLUE));

			Rectangle printArea = new Rectangle(3, 1, width - 6, height - 2);
			GCStringPrinter.printString(gc, "Add\nComments/Tags", printArea, true,
					false, SWT.CENTER | SWT.WRAP | SWT.TOP);

			font.dispose();

			gc.dispose();

			Graphic graphic = cell.getGraphic();

			if (graphic == null) {
				graphic = new UISWTGraphicImpl(img);
			} else {
				Image oldImg = ((UISWTGraphic) graphic).getImage();
				((UISWTGraphic) graphic).setImage(img);

				if (oldImg != null && !oldImg.isDisposed()) {
					oldImg.dispose();
				}
			}
			cell.setGraphic(graphic);
		}

		public void refresh(TableCell cell) {
			int width = cell.getWidth();
			int height = cell.getHeight();

			if (width <= 0 || height <= 0) {
				return;
			}
		}

		public void dispose(TableCell cell) {
			Image img = ((TableCellCore) cell).getGraphicSWT();
			if (img != null && !img.isDisposed()) {
				img.dispose();
			}
		}
	}
}
