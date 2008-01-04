/**
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.ui.swt.views.skin;

import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.TableRowCore;

import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * Legacy support for EMP.  Class moved to
 * {@link org.gudy.azureus2.ui.swt.views.table.impl.FakeTableCell}
 * 
 * @author TuxPaper
 * @created Jan 1, 2008
 *
 */
public class FakeTableCell implements TableCell
{
	org.gudy.azureus2.ui.swt.views.table.impl.FakeTableCell cell;

	public FakeTableCell(TableColumn column) {
		cell = new org.gudy.azureus2.ui.swt.views.table.impl.FakeTableCell(column);
	}

	public FakeTableCell(TableColumnCore column) {
		cell = new org.gudy.azureus2.ui.swt.views.table.impl.FakeTableCell(column);
	}

	public void refresh() {
		cell.refresh();
	}

	public void addDisposeListener(TableCellDisposeListener listener) {
		cell.addDisposeListener(listener);
	}

	public void addListeners(Object listenerObject) {
		cell.addListeners(listenerObject);
	}

	public void addMouseListener(TableCellMouseListener listener) {
		cell.addMouseListener(listener);
	}

	public void addMouseMoveListener(TableCellMouseMoveListener listener) {
		cell.addMouseMoveListener(listener);
	}

	public void addRefreshListener(TableCellRefreshListener listener) {
		cell.addRefreshListener(listener);
	}

	public void addToolTipListener(TableCellToolTipListener listener) {
		cell.addToolTipListener(listener);
	}

	public void addVisibilityListener(TableCellVisibilityListener listener) {
		cell.addVisibilityListener(listener);
	}

	public int compareTo(Object arg0) {
		return cell.compareTo(arg0);
	}

	public void dispose() {
		cell.dispose();
	}

	public void doPaint(GC gc, Rectangle bounds) {
		cell.doPaint(gc, bounds);
	}

	public boolean equals(Object obj) {
		return cell.equals(obj);
	}

	public int[] getBackground() {
		return cell.getBackground();
	}

	public Graphic getBackgroundGraphic() {
		return cell.getBackgroundGraphic();
	}

	public int getCursorID() {
		return cell.getCursorID();
	}

	public Object getDataSource() {
		return cell.getDataSource();
	}

	public int[] getForeground() {
		return cell.getForeground();
	}

	public Graphic getGraphic() {
		return cell.getGraphic();
	}

	public int getHeight() {
		return cell.getHeight();
	}

	public int getMaxLines() {
		return cell.getMaxLines();
	}

	public String getObfusticatedText() {
		return cell.getObfusticatedText();
	}

	public Comparable getSortValue() {
		return cell.getSortValue();
	}

	public TableColumn getTableColumn() {
		return cell.getTableColumn();
	}

	public String getTableID() {
		return cell.getTableID();
	}

	public TableRow getTableRow() {
		return cell.getTableRow();
	}

	public TableRowCore getTableRowCore() {
		return cell.getTableRowCore();
	}

	public String getText() {
		return cell.getText();
	}

	public Object getToolTip() {
		return cell.getToolTip();
	}

	public boolean getVisuallyChangedSinceRefresh() {
		return cell.getVisuallyChangedSinceRefresh();
	}

	public int getWidth() {
		return cell.getWidth();
	}

	public int hashCode() {
		return cell.hashCode();
	}

	public void invalidate() {
		cell.invalidate();
	}

	public void invalidate(boolean mustRefresh) {
		cell.invalidate(mustRefresh);
	}

	public void invokeMouseListeners(TableCellMouseEvent event) {
		cell.invokeMouseListeners(event);
	}

	public void invokeToolTipListeners(int type) {
		cell.invokeToolTipListeners(type);
	}

	public void invokeVisibilityListeners(int visibility,
			boolean invokeColumnListeners) {
		cell.invokeVisibilityListeners(visibility, invokeColumnListeners);
	}

	public boolean isDisposed() {
		return cell.isDisposed();
	}

	public boolean isMouseOver() {
		return cell.isMouseOver();
	}

	public boolean isShown() {
		return cell.isShown();
	}

	public boolean isUpToDate() {
		return cell.isUpToDate();
	}

	public boolean isValid() {
		return cell.isValid();
	}

	public void locationChanged() {
		cell.locationChanged();
	}

	public void mouseDoubleClick(MouseEvent e) {
		cell.mouseDoubleClick(e);
	}

	public void mouseDown(MouseEvent e) {
		cell.mouseDown(e);
	}

	public void mouseEnter(MouseEvent e) {
		cell.mouseEnter(e);
	}

	public void mouseExit(MouseEvent e) {
		cell.mouseExit(e);
	}

	public void mouseHover(MouseEvent e) {
		cell.mouseHover(e);
	}

	public void mouseMove(MouseEvent e) {
		cell.mouseMove(e);
	}

	public void mouseUp(MouseEvent e) {
		cell.mouseUp(e);
	}

	public boolean needsPainting() {
		return cell.needsPainting();
	}

	public void paintControl(PaintEvent e) {
		cell.paintControl(e);
	}

	public boolean refresh(boolean doGraphics, boolean rowVisible,
			boolean cellVisible) {
		return cell.refresh(doGraphics, rowVisible, cellVisible);
	}

	public boolean refresh(boolean doGraphics, boolean rowVisible) {
		return cell.refresh(doGraphics, rowVisible);
	}

	public boolean refresh(boolean doGraphics) {
		return cell.refresh(doGraphics);
	}

	public void removeDisposeListener(TableCellDisposeListener listener) {
		cell.removeDisposeListener(listener);
	}

	public void removeMouseListener(TableCellMouseListener listener) {
		cell.removeMouseListener(listener);
	}

	public void removeMouseMoveListener(TableCellMouseMoveListener listener) {
		cell.removeMouseMoveListener(listener);
	}

	public void removeRefreshListener(TableCellRefreshListener listener) {
		cell.removeRefreshListener(listener);
	}

	public void removeToolTipListener(TableCellToolTipListener listener) {
		cell.removeToolTipListener(listener);
	}

	public void removeVisibilityListener(TableCellVisibilityListener listener) {
		cell.removeVisibilityListener(listener);
	}

	public void setControl(Composite composite) {
		cell.setControl(composite);
	}

	public void setCursorID(int cursorID) {
		cell.setCursorID(cursorID);
	}

	public void setDataSource(Object datasource) {
		cell.setDataSource(datasource);
	}

	public void setFillCell(boolean fillCell) {
		cell.setFillCell(fillCell);
	}

	public boolean setForeground(int red, int green, int blue) {
		return cell.setForeground(red, green, blue);
	}

	public boolean setForeground(int[] rgb) {
		return cell.setForeground(rgb);
	}

	public boolean setForegroundToErrorColor() {
		return cell.setForegroundToErrorColor();
	}

	public boolean setGraphic(Graphic img) {
		return cell.setGraphic(img);
	}

	public void setMarginHeight(int height) {
		cell.setMarginHeight(height);
	}

	public void setMarginWidth(int width) {
		cell.setMarginWidth(width);
	}

	public void setOrentation(int o) {
		cell.setOrentation(o);
	}

	public boolean setSortValue(Comparable valueToSort) {
		return cell.setSortValue(valueToSort);
	}

	public boolean setSortValue(float valueToSort) {
		return cell.setSortValue(valueToSort);
	}

	public boolean setSortValue(long valueToSort) {
		return cell.setSortValue(valueToSort);
	}

	public boolean setText(String text) {
		return cell.setText(text);
	}

	public void setToolTip(Object tooltip) {
		cell.setToolTip(tooltip);
	}

	public void setUpToDate(boolean upToDate) {
		cell.setUpToDate(upToDate);
	}

	public String toString() {
		return cell.toString();
	}
}
