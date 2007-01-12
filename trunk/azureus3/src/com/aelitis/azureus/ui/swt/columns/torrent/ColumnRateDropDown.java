/*
 * Created on Jul 11, 2006 3:04:03 PM
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
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTGraphicImpl;
import org.gudy.azureus2.ui.swt.views.table.TableCellCore;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.ui.swt.utils.ImageLoaderFactory;
import com.aelitis.azureus.ui.swt.views.RateItListView;
import com.aelitis.azureus.ui.swt.views.list.ListCell;
import com.aelitis.azureus.ui.swt.views.list.ListRow;
import com.aelitis.azureus.ui.swt.views.list.ListSelectionAdapter;

import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author TuxPaper
 * @created Jul 11, 2006
 *
 */
public class ColumnRateDropDown extends CoreTableColumn implements
		TableCellAddedListener
{
	private final static int DROP_DOWN_ARROW_WIDTH = 20;

	final String[] ICON_NAMES = {
		"icon.frogfingers.0",
		"icon.frogfingers.1",
		"icon.frogfingers.2",
		"icon.frogfingers.3",
		"icon.frogfingers.4",
		"icon.frogfingers.5",
		"icon.frogfingers.6",
	};

	private Image imgDD;

	private Rectangle imgDDbounds;

	/**
	 * 
	 */
	public ColumnRateDropDown(String sTableID) {
		super("RateDD", sTableID);
		if (imgDD == null) {
			imgDD = ImageLoaderFactory.getInstance().getImage("image.rateitdd");
			imgDDbounds = imgDD.getBounds();
		}
		initializeAsGraphic(POSITION_LAST, imgDDbounds.width);
		setAlignment(ALIGN_CENTER);
	}

	public void cellAdded(TableCell cell) {
		new Cell(cell);
	}

	private class Cell implements TableCellRefreshListener,
			TableCellMouseListener
	{
		private Composite cDropDownList = null;

		private long lClosedOn = 0;

		private Image imgRating;

		public Cell(TableCell cell) {
			cell.addListeners(this);
			cell.setMarginWidth(0);
			cell.setMarginHeight(0);

			int i = (int) (Math.random() * ICON_NAMES.length);
			imgRating = ImageLoaderFactory.getInstance().getImage(ICON_NAMES[i]);
		}

		public void refresh(TableCell cell) {

			if (cell.isValid()) {
				return;
			}

			Image image = new Image(Display.getCurrent(), imgDDbounds.width,
					imgDDbounds.height);

			GC gc = new GC(image);
			try {
				gc.drawImage(imgDD, 0, 0);
				gc.drawImage(imgRating, 3, 1);
			} finally {
				gc.dispose();
			}

			Graphic graphic = new UISWTGraphicImpl(image);
			cell.setGraphic(graphic);
		}

		// @see org.gudy.azureus2.plugins.ui.tables.TableCellMouseListener#cellMouseTrigger(org.gudy.azureus2.plugins.ui.tables.TableCellMouseEvent)
		public void cellMouseTrigger(final TableCellMouseEvent event) {
			if (event.button != 1
					|| event.eventType != TableCellMouseEvent.EVENT_MOUSEDOWN) {
				return;
			}

			int width = event.cell.getWidth();
			if (event.x < width - DROP_DOWN_ARROW_WIDTH) {
				return;
			}

			event.skipCoreFunctionality = true;

			if (cDropDownList != null) {
				closeDropDownList();
				return;
			}

			if (SystemTime.getCurrentTime() - lClosedOn < 100) {
				// too soon.  Could happen when:
				// 1) Click on drop down arrow
				// 2) Click on drop down arrow again
				// 3) Focus goes to row
				// 4) Focus even fires, closes drop down list
				// 5) cellMouseTrigger fires
				return;
			}

			TableCellCore cellCore = (TableCellCore) event.cell;
			final ListCell listCell = (ListCell) cellCore.getBufferedTableItem();

			// drop down list may be bigger than row, or bigger than parent, so we add
			// the list to shell and position accordingly
			Composite parent = (Composite)listCell.getRow().getView().getControl();

			Rectangle bounds = cellCore.getBounds();
			Point location = parent.toDisplay(bounds.x, bounds.y + bounds.height);

			cDropDownList = new Composite(parent.getShell(), SWT.BORDER);
			cDropDownList.setSize(width, 200);
			location = parent.getShell().toControl(location);
			cDropDownList.setLocation(location.x, location.y);
			cDropDownList.moveAbove(null);
			cDropDownList.setBackground(parent.getDisplay().getSystemColor(
					SWT.COLOR_LIST_BACKGROUND));
			cDropDownList.setForeground(parent.getDisplay().getSystemColor(
					SWT.COLOR_LIST_FOREGROUND));
			cDropDownList.setBackgroundMode(SWT.INHERIT_FORCE);
			cDropDownList.setLayout(new FormLayout());

			RateItListView view = new RateItListView(null, cDropDownList);
			view.setMouseClickIsDefaultSelection(true);
			view.addSelectionListener(new ListSelectionAdapter() {
				public void defaultSelected(ListRow[] rows) {
					closeDropDownList();

					listCell.getRow().getView().getControl().setFocus();

					String id = (String) rows[0].getDataSource(true);
					imgRating = ImageLoaderFactory.getInstance().getImage(id);
					event.cell.invalidate();
				}

				public void selected(ListRow row) {
					closeDropDownList();
				}
			}, false);

			cDropDownList.layout(true, true);
			view.updateUI();
			cDropDownList.setFocus();

			view.getControl().addListener(SWT.FocusOut, new Listener() {
				public void handleEvent(Event event) {
					closeDropDownList();
				}
			});
			view.getControl().addListener(SWT.Deactivate, new Listener() {
				public void handleEvent(Event event) {
					closeDropDownList();
				}
			});
		}

		/**
		 * 
		 */
		protected void closeDropDownList() {
			lClosedOn = SystemTime.getCurrentTime();
			cDropDownList.dispose();
			cDropDownList = null;
		}
	}
}
