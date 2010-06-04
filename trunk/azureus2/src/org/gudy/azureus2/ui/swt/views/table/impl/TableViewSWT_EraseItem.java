package org.gudy.azureus2.ui.swt.views.table.impl;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.views.table.TableItemOrTreeItem;
import org.gudy.azureus2.ui.swt.views.table.TableOrTreeSWT;

public class TableViewSWT_EraseItem
	implements Listener
{
	final Color[] alternatingColors = new Color[] {
		null,
		Colors.colorAltRow
	};

	private final TableOrTreeSWT table;
	
	private TableViewSWTImpl<?> tv;
	
	public TableViewSWT_EraseItem(TableViewSWTImpl<?> tv, TableOrTreeSWT table) {
		this.table = table;
		this.tv = tv;
	}
	
	public void handleEvent(Event event) {
		TableItemOrTreeItem item = TableOrTreeUtils.getEventItem(event.item);
		Rectangle bounds = event.getBounds();

		if (TableViewSWTImpl.DRAW_FULL_ROW
				&& (event.detail & (SWT.HOT | SWT.SELECTED | SWT.FOCUSED)) == 0) {

			int pos;
			TableItemOrTreeItem parentItem = item.getParentItem();
			if (parentItem != null) {
				pos = parentItem.indexOf(item) + ((table.indexOf(parentItem) + 1) % 2);
			} else {
				pos = table.indexOf(item);
			}
			Color color = alternatingColors[pos % 2];
			if (color != null) {
				event.gc.setBackground(color);
			}
			Rectangle drawBounds = bounds;
			if (event.index == table.getColumnCount() - 1) {
				tv.swt_calculateClientArea();
				drawBounds = new Rectangle(bounds.x, bounds.y, tv.clientArea.x
						+ tv.clientArea.width - bounds.x, bounds.height);
				event.gc.setClipping(drawBounds);
				//System.out.println(bounds.width);
			}
			event.gc.fillRectangle(drawBounds);
			event.detail &= ~SWT.BACKGROUND;
		}

		// Vertical lines between columns
		if (TableViewSWTImpl.DRAW_VERTICAL_LINES) {
			if (item != null
					&& (bounds.width == item.getParent().getColumn(event.index).getWidth())) {
				//System.out.println(bounds.width + ";" + item.getParent().getColumn(event.index).getWidth());
				Color fg = event.gc.getForeground();
				event.gc.setForeground(Colors.blues[Colors.BLUES_LIGHTEST + 1]);
				//event.gc.setAlpha(40);
				event.gc.setClipping((Rectangle) null);
				event.gc.drawLine(bounds.x + bounds.width - 1, bounds.y - 1, bounds.x
						+ bounds.width - 1, bounds.y + bounds.height);
				event.gc.setForeground(fg);
			}
		}
	}
}
