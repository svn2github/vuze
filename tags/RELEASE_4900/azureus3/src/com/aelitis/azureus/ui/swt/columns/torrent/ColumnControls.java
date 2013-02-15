/**
 * Created on April 28, 2007 
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */

package com.aelitis.azureus.ui.swt.columns.torrent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;
import org.gudy.azureus2.ui.swt.plugins.UISWTGraphic;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTGraphicImpl;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.views.table.CoreTableColumnSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;

import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.swt.columns.utils.ColumnImageClickArea;
import com.aelitis.azureus.ui.swt.utils.ColorCache;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author TuxPaper
 * @created Apr 28, 2007
 *
 */
public class ColumnControls
	extends CoreTableColumnSWT
	implements TableCellAddedListener
{
	public static final Class DATASOURCE_TYPE = Download.class;

	public static String COLUMN_ID = "Controls";

	private static final int COLUMN_WIDTH = 32;

	private static final boolean DEBUG = false;

	private Display display;

	public static Font fontText;

	List listClickAreas = new ArrayList();

	public ColumnControls(String sTableID) {
		super(DATASOURCE_TYPE, COLUMN_ID, ALIGN_LEAD, COLUMN_WIDTH, sTableID);
		initializeAsGraphic(COLUMN_WIDTH);
		setMinWidth(COLUMN_WIDTH);
		setMaxWidth(COLUMN_WIDTH);

		display = SWTThread.getInstance().getDisplay();

		ColumnImageClickArea clickArea;

		clickArea = new ColumnImageClickArea(COLUMN_ID, "up", "image.torrent.up");
		clickArea.setPosition(0, 0);
		listClickAreas.add(clickArea);

		clickArea = new ColumnImageClickArea(COLUMN_ID, "down", "image.torrent.down");
		clickArea.setPosition(16, 0);
		listClickAreas.add(clickArea);
}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener#cellAdded(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void cellAdded(TableCell cell) {
		new Cell(cell);
	}

	private class Cell
		implements TableCellRefreshListener, TableCellDisposeListener,
		TableCellMouseListener, TableCellVisibilityListener
	{
		public Cell(TableCell cell) {
			cell.addListeners(this);
			cell.setMarginHeight(1);
			cell.setMarginWidth(0);
			cell.setFillCell(true);

			for (Iterator iter = listClickAreas.iterator(); iter.hasNext();) {
				ColumnImageClickArea clickArea = (ColumnImageClickArea) iter.next();
				clickArea.addCell(cell);
			}
		}

		public void dispose(TableCell cell) {
			disposeExisting(cell);
		}

		public void refresh(TableCell cell) {
			refresh(cell, false);
		}

		private void refresh(TableCell cell, boolean bForce) {
			DownloadManager dm = (DownloadManager) cell.getDataSource();
			if (dm == null) {
				disposeExisting(cell);
				return;
			}
			int position = dm.getPosition();

			int cellWidth = cell.getWidth();
			int cellHeight = cell.getHeight();

			Image image = null;
			Graphic graphic = cell.getGraphic();
			if (graphic instanceof UISWTGraphic) {
				image = ((UISWTGraphic)graphic).getImage();
			}
			if (image != null) {
				Rectangle bounds = image.getBounds();
				if (!cell.setSortValue(position) && cell.isValid()
						&& bounds.width == cellWidth && bounds.height == cellHeight) {
					return;
				}
			} else {
				cell.setSortValue(position);
			}

			disposeExisting(cell);
			image = new Image(display, cellWidth, cellHeight);

			GC gcImage = new GC(image);
			try {
				Color background = ColorCache.getColor(display, cell.getBackground());
				if (background != null) {
					gcImage.setBackground(background);
					gcImage.fillRectangle(0, 0, cellWidth, cellHeight);
				}
				

//				if (fontText == null) {
//					fontText = Utils.getFontWithHeight(gcImage.getFont(), gcImage, 15);
//				}
//				gcImage.setFont(fontText);
				int[] fg = cell.getForeground();
				gcImage.setForeground(ColorCache.getColor(display, fg[0], fg[1], fg[2]));

				Rectangle bounds = image.getBounds();
				GCStringPrinter.printString(gcImage, "" + position + (dm.getAssumedComplete() ? "^" : "v"), bounds, true,
						false, SWT.BOTTOM | SWT.CENTER);
				gcImage.setFont(null);

				for (Iterator iter = listClickAreas.iterator(); iter.hasNext();) {
					ColumnImageClickArea clickArea = (ColumnImageClickArea) iter.next();
					clickArea.drawImage(cell, gcImage);
				}
			} finally {
				gcImage.dispose();
			}

			disposeExisting(cell);

			if (cell instanceof TableCellSWT) {
				((TableCellSWT) cell).setGraphic(image);
			} else {
				cell.setGraphic(new UISWTGraphicImpl(image));
			}
		}

		// @see org.gudy.azureus2.plugins.ui.tables.TableCellMouseListener#cellMouseTrigger(org.gudy.azureus2.plugins.ui.tables.TableCellMouseEvent)
		public void cellMouseTrigger(TableCellMouseEvent event) {
			if (event.data instanceof ColumnImageClickArea) {
				DownloadManager dm = (DownloadManager) event.cell.getDataSource();
				if (dm == null) {
					return;
				}

				ColumnImageClickArea clickArea = (ColumnImageClickArea) event.data;
				log(event.cell, "CLICK ON " + clickArea.getId());
				if (clickArea.getId().equals("up")) {
					dm.getGlobalManager().moveUp(dm);
				} else if (clickArea.getId().equals("down")) {
					dm.getGlobalManager().moveDown(dm);
				}
				event.cell.getTableColumn().invalidateCells();
			}
		}

		public void cellVisibilityChanged(TableCell cell, int visibility) {
			if (visibility == TableCellVisibilityListener.VISIBILITY_HIDDEN) {
				//log(cell, "whoo, save");
				disposeExisting(cell);
			} else if (visibility == TableCellVisibilityListener.VISIBILITY_SHOWN) {
				//log(cell, "whoo, draw");
				refresh(cell, true);
			}
		}

		private void disposeExisting(TableCell cell) {
			Graphic oldGraphic = cell.getGraphic();
			//log(cell, oldGraphic);
			if (oldGraphic instanceof UISWTGraphic) {
				Image oldImage = ((UISWTGraphic) oldGraphic).getImage();
				if (oldImage != null && !oldImage.isDisposed()) {
					//log(cell, "dispose");
					cell.setGraphic(null);
					oldImage.dispose();
				}
			}
		}

		private void log(TableCell cell, String s) {
			if (!DEBUG) {
				return;
			}
			System.out.println(((TableRowCore) cell.getTableRow()).getIndex() + ":"
					+ System.currentTimeMillis() + ": " + s);
		}
	}
}
