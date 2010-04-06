/**
 * Created on Aug 25, 2009
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

package com.aelitis.azureus.ui.swt.content.columns;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter.URLInfo;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;

import com.aelitis.azureus.core.content.RelatedContent;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.swt.shells.main.MainWindow;
import com.aelitis.azureus.ui.swt.skin.SWTSkinFactory;
import com.aelitis.azureus.ui.swt.skin.SWTSkinProperties;

/**
 * @author TuxPaper
 * @created Aug 25, 2009
 *
 */
public class ColumnRC_Actions
	implements TableCellRefreshListener, TableCellMouseMoveListener,
	TableCellSWTPaintListener
{
	public static final String COLUMN_ID = "rc_actions";

	private static Font font = null;

	private Color colorLinkNormal;

	private Color colorLinkHover;


	
	public ColumnRC_Actions(TableColumn column) {
		column.initialize(TableColumn.ALIGN_LEAD, TableColumn.POSITION_LAST, 215);
		column.addListeners(this);
		column.setRefreshInterval(TableColumn.INTERVAL_GRAPHIC);
		column.setType(TableColumn.TYPE_GRAPHIC);

		if (column instanceof TableColumnCore) {
			((TableColumnCore) column).setUseCoreDataSource(true);
			((TableColumnCore) column).addCellOtherListener("SWTPaint", this);
		}

		SWTSkinProperties skinProperties = SWTSkinFactory.getInstance().getSkinProperties();
		colorLinkNormal = skinProperties.getColor("color.links.normal");
		colorLinkHover = skinProperties.getColor("color.links.hover");
	}

	public void cellPaint(GC gc, TableCellSWT cell) {
		String text = (String) cell.getSortValue();
		if (text == null) {
			return;
		}

		if (text != null && text.length() > 0) {
			if (font == null) {
				FontData[] fontData = gc.getFont().getFontData();
				fontData[0].setStyle(SWT.BOLD);
				font = new Font(gc.getDevice(), fontData);
			}
			gc.setFont(font);

			Rectangle bounds = getDrawBounds(cell);

			GCStringPrinter sp = new GCStringPrinter(gc, text, bounds, true, true,
					SWT.WRAP | SWT.CENTER);

			sp.calculateMetrics();

			if (sp.hasHitUrl()) {
				URLInfo[] hitUrlInfo = sp.getHitUrlInfo();
				for (int i = 0; i < hitUrlInfo.length; i++) {
					URLInfo info = hitUrlInfo[i];
					// handle fake row when showing in column editor

					info.urlUnderline = cell.getTableRow() == null
							|| cell.getTableRow().isSelected();
					if (info.urlUnderline) {
						info.urlColor = null;
					} else {
						info.urlColor = colorLinkNormal;
					}
				}
				int[] mouseOfs = cell.getMouseOffset();
				if (mouseOfs != null) {
					Rectangle realBounds = cell.getBounds();
					URLInfo hitUrl = sp.getHitUrl(mouseOfs[0] + realBounds.x, mouseOfs[1]
							+ realBounds.y);
					if (hitUrl != null) {
						hitUrl.urlColor = colorLinkHover;
					}
				}
			}

			sp.printString(GCStringPrinter.FLAG_FULLLINESONLY);
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener#cellAdded(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void cellAdded(TableCell cell) {
		cell.setMarginHeight(0);
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener#refresh(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void refresh(TableCell cell) {
		RelatedContent rc = (RelatedContent) cell.getDataSource();
		if (rc == null) {
			return;
		}
		boolean downloadable = rc.getHash() != null;

		String s;
		s = "<A HREF=\"search\">" + MessageText.getString("Button.search") + "</A>";
		if (downloadable) {
			s += " | <A HREF=\"dl\">"
				+ MessageText.getString("v3.MainWindow.button.download") + "</A>";
		}

		if (!cell.setSortValue(s) && cell.isValid()) {
			return;
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellMouseListener#cellMouseTrigger(org.gudy.azureus2.plugins.ui.tables.TableCellMouseEvent)
	public void cellMouseTrigger(TableCellMouseEvent event) {
		RelatedContent rc = (RelatedContent) event.cell.getDataSource();
		if (rc == null) {
			return;
		}

		boolean invalidateAndRefresh = event.eventType == event.EVENT_MOUSEEXIT;

		Rectangle bounds = ((TableCellSWT) event.cell).getBounds();
		String text = (String) event.cell.getSortValue();
		if (text == null) {
			return;
		}

		GCStringPrinter sp = null;
		GC gc = new GC(Display.getDefault());
		try {
			if (font != null) {
				gc.setFont(font);
			}
			Rectangle drawBounds = getDrawBounds((TableCellSWT) event.cell);
			sp = new GCStringPrinter(gc, text, drawBounds, true, true, SWT.WRAP
					| SWT.CENTER);
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
				if (event.eventType == TableCellMouseEvent.EVENT_MOUSEUP && event.button == 1) {
					if (hitUrl.url.equals("dl")) {
						byte[]  hash = rc.getHash();
						
						if ( hash != null ){
							
							rc.setUnread( false );
							
							TorrentOpener.openTorrent( ByteFormatter.encodeString( hash ));
						}
						
					} else if (hitUrl.url.equals("search")) {
						rc.setUnread( false );
						
						String	title = rc.getTitle();
						
						MainWindow.doSearch( title );
					}
				}

				newCursor = SWT.CURSOR_HAND;
			} else {
				newCursor = SWT.CURSOR_ARROW;
			}

			int oldCursor = ((TableCellSWT) event.cell).getCursorID();
			if (oldCursor != newCursor) {
				invalidateAndRefresh = true;
				((TableCellSWT) event.cell).setCursorID(newCursor);
			}
		}

		if (invalidateAndRefresh) {
			event.cell.invalidate();
			((TableCellSWT)event.cell).redraw();
		}
	}

	private Rectangle getDrawBounds(TableCellSWT cell) {
		Rectangle bounds = cell.getBounds();

		return bounds;
	}
}
