/*
 * File    : CompletionItem.java
 * Created : 24 nov. 2003
 * By      : Olivier
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

package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;
import org.gudy.azureus2.ui.swt.plugins.UISWTGraphic;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTGraphicImpl;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.tables.*;

import com.aelitis.azureus.ui.swt.utils.ColorCache;

/** Torrent Completion Level Graphic Cell for My Torrents.
 *
 * @author Olivier
 * @author TuxPaper (2004/Apr/17: modified to TableCellAdapter)
 */
public class CompletionItem
	extends CoreTableColumn
	implements TableCellAddedListener, TableCellRefreshListener,
	TableCellDisposeListener, TableCellSWTPaintListener
{
	private static final int borderWidth = 1;

	public static final String COLUMN_ID = "completion";

	private static Font fontText;

	private Map mapCellLastPercentDone = new HashMap();

	private int marginHeight = -1;

	static {
		ImageRepository.addPath("org/gudy/azureus2/ui/icons/dl_bar_end.png", "dl_bar_end");
		ImageRepository.addPath("org/gudy/azureus2/ui/icons/dl_bar_0.png", "dl_bar_0");
		ImageRepository.addPath("org/gudy/azureus2/ui/icons/dl_bar_1.png", "dl_bar_1");
	}
	
	Image imgEnd;
	Image img1;
	Image img0;
	
	Color textColor;
	
	
	/** Default Constructor */
	public CompletionItem(String sTableID) {
		this(sTableID, -1);
	}

	/**
	 * 
	 * @param sTableID
	 * @param marginHeight -- Margin height above and below the progress bar; used in cases where the row is very tall 
	 */
	public CompletionItem(String sTableID, int marginHeight) {
		super(COLUMN_ID, sTableID);
		this.marginHeight = marginHeight;
		initializeAsGraphic(POSITION_INVISIBLE, 150);
		setMinWidth(100);
		
		imgEnd = ImageRepository.getImage("dl_bar_end");
		img0 = ImageRepository.getImage("dl_bar_0");
		img1 = ImageRepository.getImage("dl_bar_1");

	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener#cellAdded(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void cellAdded(TableCell cell) {
		if (marginHeight != -1) {
			cell.setMarginHeight(marginHeight);
		} else {
			cell.setMarginHeight(2);
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellDisposeListener#dispose(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void dispose(TableCell cell) {
		mapCellLastPercentDone.remove(cell);
		Graphic graphic = cell.getGraphic();
		if (graphic instanceof UISWTGraphic) {
			Image img = ((UISWTGraphic) graphic).getImage();
			if (img != null && !img.isDisposed()) {
				img.dispose();
			}
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener#refresh(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void refresh(TableCell cell) {
		int percentDone = getPercentDone(cell);

		Integer intObj = (Integer) mapCellLastPercentDone.get(cell);
		int lastPercentDone = intObj == null ? 0 : intObj.intValue();

		if (!cell.setSortValue(percentDone) && cell.isValid()
				&& lastPercentDone == percentDone) {
			return;
		}
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener#cellPaint(org.eclipse.swt.graphics.GC, org.gudy.azureus2.ui.swt.views.table.TableCellSWT)
	public void cellPaint(GC gcImage, TableCellSWT cell) {
		int percentDone = getPercentDone(cell);

		Rectangle bounds = cell.getBounds();

		int yOfs = (bounds.height - 13) / 2 ;
		int x1 = bounds.width - borderWidth - 2;
		int y1 = bounds.height - 3 - yOfs;

		if (x1 < 10 || y1 < 3) {
			return;
		}
		int textYofs = 0;

		if (y1 >= 28) {
			yOfs = 2;
			y1 = 16;
			//textYofs = yOfs;
		}

		
		
		mapCellLastPercentDone.put(cell, new Integer(percentDone));
		
		
		//draw begining and end
		gcImage.drawImage(imgEnd, bounds.x , bounds.y + yOfs);
		gcImage.drawImage(imgEnd, bounds.x + x1 + 1, bounds.y + yOfs);
		
		
		
		// draw border
//		Color fg = gcImage.getForeground();
//		gcImage.setForeground(Colors.grey);
//		gcImage.drawRectangle(bounds.x, bounds.y + yOfs, x1 + 1, y1 + 1);
//		gcImage.setForeground(fg);

		int limit = (x1 * percentDone) / 1000;
		
		gcImage.drawImage(img1, 0, 0, 1, 13, bounds.x + 1, bounds.y + yOfs, limit, 13);
		if(percentDone < 1000) {
			gcImage.drawImage(img0, 0, 0, 1, 13, bounds.x + limit +1, bounds.y + yOfs, x1 - limit, 13);
		}
		
//		gcImage.setBackground(Colors.blues[Colors.BLUES_DARKEST]);
//		gcImage.fillRectangle(bounds.x + 1, bounds.y + 1 + yOfs, limit, y1);
//		if (limit < x1) {
//			gcImage.setBackground(Colors.blues[Colors.BLUES_LIGHTEST]);
//			gcImage.fillRectangle(bounds.x + limit + 1, bounds.y + 1 + yOfs, x1
//					- limit, y1);
//		}
		
		if(textColor == null) {
			textColor = ColorCache.getColor(gcImage.getDevice(), "#005ACF" );
		}

//		if (textYofs == 0) {
//			if (fontText == null) {
//				fontText = Utils.getFontWithHeight(gcImage.getFont(), gcImage, y1);
//			}
//			gcImage.setFont(fontText);
			gcImage.setForeground(textColor);
//		}
			
		String sPercent = DisplayFormatters.formatPercentFromThousands(percentDone);
		GCStringPrinter.printString(gcImage, sPercent, new Rectangle(bounds.x + 4,
				bounds.y + yOfs, bounds.width - 4,13), true,
				false, SWT.CENTER);
	}

	private int getPercentDone(TableCell cell) {
		DownloadManager dm = (DownloadManager) cell.getDataSource();
		if (dm == null) {
			return 0;
		}
		return dm.getStats().getDownloadCompleted(true);
	}
}
