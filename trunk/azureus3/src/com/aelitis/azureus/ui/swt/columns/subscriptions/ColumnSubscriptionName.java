/**
 * Copyright (C) 2008 Vuze Inc., All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 */

package com.aelitis.azureus.ui.swt.columns.subscriptions;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellMouseEvent;
import org.gudy.azureus2.plugins.ui.tables.TableCellMouseListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.core.subs.Subscription;
import com.aelitis.azureus.ui.swt.subscriptions.SubscriptionManagerUI;
import com.aelitis.azureus.ui.swt.subscriptions.SubscriptionManagerUI.sideBarItem;

/**
 * @author Olivier Chalouhi
 * @created Oct 7, 2008
 *
 */
public class ColumnSubscriptionName
	extends CoreTableColumn
	implements TableCellRefreshListener, TableCellSWTPaintListener, TableCellMouseListener
{
	public static String COLUMN_ID = "name";
	
	static {
		ImageRepository.addPath("com/aelitis/azureus/ui/images/ic_view.png", "ic_view");
	}
	
	
	Image viewImage;
	int imageWidth = -1;
	int imageHeight = -1;

	/** Default Constructor */
	public ColumnSubscriptionName(String sTableID) {
		super(COLUMN_ID, POSITION_LAST, 350, sTableID);
		setMinWidth(300);
		
		viewImage = ImageRepository.getImage("ic_view");
	}

	public void refresh(TableCell cell) {
		String name = null;
		Subscription sub = (Subscription) cell.getDataSource();
		if (sub != null) {
			name = sub.getName();
		}
		if (name == null) {
			name = "";
		}

		if (!cell.setSortValue(name) && cell.isValid()) {
			return;
		}

		if (!cell.isShown()) {
			return;
		}
		
		cell.setText(name);
		return;
	}
	
	public void cellPaint(GC gc, TableCellSWT cell) {
		Rectangle bounds = cell.getBounds();
		if(imageWidth == -1 || imageHeight == -1) {
			imageWidth = viewImage.getBounds().width;
			imageHeight = viewImage.getBounds().height;
		}
		
		bounds.width -= (imageWidth + 5);
		
		GCStringPrinter.printString(gc, cell.getText(), bounds,true,false,SWT.LEFT);
		gc.drawImage(viewImage, bounds.x + bounds.width, bounds.y + bounds.height / 2 - imageHeight / 2);
		
		//gc.drawText(cell.getText(), bounds.x,bounds.y);
	}
	
	public void cellMouseTrigger(TableCellMouseEvent event) {
		
		if (event.eventType == TableCellMouseEvent.EVENT_MOUSEUP
				&& event.button == 1) {
			TableCell cell = event.cell;
			int cellWidth = cell.getWidth();
			if(event.x > cellWidth - imageWidth - 5 && event.x < cellWidth - 5) {
				Subscription sub = (Subscription) cell.getDataSource();
				if(sub != null) {
					sideBarItem item = (sideBarItem) sub.getUserData(SubscriptionManagerUI.SUB_IVIEW_KEY);
					item.activate();
				}
			}
		}
		
	}
	
	
}
