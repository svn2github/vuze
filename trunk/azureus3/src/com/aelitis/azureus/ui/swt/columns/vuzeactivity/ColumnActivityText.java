/**
 * Created on Sep 25, 2008
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

package com.aelitis.azureus.ui.swt.columns.vuzeactivity;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter.URLInfo;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.core.messenger.config.PlatformConfigMessenger;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.skin.SWTSkinFactory;
import com.aelitis.azureus.ui.swt.skin.SWTSkinProperties;
import com.aelitis.azureus.util.StringCompareUtils;

import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author TuxPaper
 * @created Sep 25, 2008
 *
 */
public class ColumnActivityText
	extends CoreTableColumn
	implements TableCellSWTPaintListener, TableCellRefreshListener,
	TableCellMouseMoveListener
{
	public static final String COLUMN_ID = "activityText";

	private Color colorLinkNormal;

	private Color colorLinkHover;

	/**
	 * @param name
	 * @param tableID
	 */
	public ColumnActivityText(String tableID) {
		super(COLUMN_ID, 200, tableID);

		initializeAsGraphic(POSITION_LAST, 500);
		SWTSkinProperties skinProperties = SWTSkinFactory.getInstance().getSkinProperties();
		colorLinkNormal = skinProperties.getColor("color.links.normal");
		colorLinkHover = skinProperties.getColor("color.links.hover");
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener#cellPaint(org.eclipse.swt.graphics.GC, org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void cellPaint(GC gc, TableCellSWT cell) {
		VuzeActivitiesEntry entry = (VuzeActivitiesEntry) cell.getDataSource();
		String text = entry.getText();
		Rectangle bounds = getDrawBounds(cell);

		GCStringPrinter sp = new GCStringPrinter(gc, text, bounds, true, true,
				SWT.WRAP);

		sp.calculateMetrics();

		if (sp.hasHitUrl()) {
			URLInfo[] hitUrlInfo = sp.getHitUrlInfo();
			for (int i = 0; i < hitUrlInfo.length; i++) {
				URLInfo info = hitUrlInfo[i];
				info.urlUnderline = cell.getTableRow().isSelected();
				if (info.urlUnderline) {
					info.urlColor = null;
				} else {
					info.urlColor = colorLinkNormal;
				}
			}
			int[] mouseOfs = cell.getMouseOffset();
			if (mouseOfs != null) {
				URLInfo hitUrl = sp.getHitUrl(mouseOfs[0] + bounds.x, mouseOfs[1]
						+ bounds.y);
				if (hitUrl != null) {
					hitUrl.urlColor = colorLinkHover;
				}
			}
		}

		sp.printString();
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener#refresh(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void refresh(TableCell cell) {
		VuzeActivitiesEntry entry = (VuzeActivitiesEntry) cell.getDataSource();

		cell.setSortValue(entry.getText());
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellMouseListener#cellMouseTrigger(org.gudy.azureus2.plugins.ui.tables.TableCellMouseEvent)
	public void cellMouseTrigger(TableCellMouseEvent event) {
		String tooltip = null;
		boolean invalidateAndRefresh = false;

		VuzeActivitiesEntry entry = (VuzeActivitiesEntry) event.cell.getDataSource();
		Rectangle bounds = ((TableCellSWT) event.cell).getBounds();

		String text = entry.getText();

		GCStringPrinter sp = null;
		GC gc = new GC(Display.getDefault());
		try {
			sp = new GCStringPrinter(gc, text, bounds, true, true, SWT.WRAP);
			sp.calculateMetrics();
		} catch (Exception e) {
			Debug.out(e);
		} finally {
			gc.dispose();
		}

		if (sp != null) {
			URLInfo hitUrl = sp.getHitUrl(event.x + bounds.x, event.y + bounds.y);
			int newCursor;
			if (hitUrl != null) {
				if (event.eventType == TableCellMouseEvent.EVENT_MOUSEUP) {
					if (!PlatformConfigMessenger.urlCanRPC(hitUrl.url)) {
						Utils.launch(hitUrl.url);
					} else {
						UIFunctionsSWT uif = UIFunctionsManagerSWT.getUIFunctionsSWT();
						if (uif != null) {
							String target = hitUrl.target == null
									? SkinConstants.VIEWID_BROWSER_BROWSE : hitUrl.target;
							uif.viewURL(hitUrl.url, target, 0, 0, false, false);
							return;
						}
					}
				}

				newCursor = SWT.CURSOR_HAND;
				if (PlatformConfigMessenger.urlCanRPC(hitUrl.url)) {
					tooltip = hitUrl.title;
				} else {
					tooltip = hitUrl.url;
				}
			} else {
				newCursor = SWT.CURSOR_ARROW;
			}

			int oldCursor = ((TableCellSWT) event.cell).getCursorID();
			if (oldCursor != newCursor) {
				invalidateAndRefresh = true;
				((TableCellSWT) event.cell).setCursorID(newCursor);
			}
		}

		Object o = event.cell.getToolTip();
		if ((o == null) | (o instanceof String)) {
			String oldTooltip = (String) o;
			if (!StringCompareUtils.equals(oldTooltip, tooltip)) {
				invalidateAndRefresh = true;
				event.cell.setToolTip(tooltip);
			}
		}

		if (invalidateAndRefresh) {
			event.cell.invalidate();
		}
	}

	private Rectangle getDrawBounds(TableCellSWT cell) {
		Rectangle bounds = cell.getBounds();
		bounds.height -= 12;
		bounds.y += 6;
		bounds.x += 4;
		bounds.width -= 4;

		return bounds;
	}
}
