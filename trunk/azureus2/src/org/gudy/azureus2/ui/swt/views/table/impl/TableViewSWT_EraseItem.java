package org.gudy.azureus2.ui.swt.views.table.impl;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Utils;
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
	
	private boolean drawExtended;
	
	private boolean first = true;
	
	public TableViewSWT_EraseItem(TableViewSWTImpl<?> _tv, TableOrTreeSWT table) {
		this.table = table;
		this.tv = _tv;
		COConfigurationManager.addAndFireParameterListener("Table.extendedErase",
				new ParameterListener() {
					public void parameterChanged(String parameterName) {
						Utils.execSWTThread(new AERunnable() {
							public void runSupport() {
								drawExtended = COConfigurationManager.getBooleanParameter("Table.extendedErase");
								if (!first) {
									Rectangle bounds = tv.getTableComposite().getBounds();
									tv.getTableComposite().redraw(bounds.x, bounds.y, bounds.width, bounds.height, true);
								}
								first = false;
							}
						});
					}
				});
	}
	
	public void handleEvent(Event event) {
		TableItemOrTreeItem item = TableOrTreeUtils.getEventItem(event.item);
		Rectangle bounds = event.getBounds();

		if ((event.detail & (SWT.HOT | SWT.SELECTED | SWT.FOCUSED)) == 0) {

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
			if (TableViewSWTImpl.DRAW_FULL_ROW && drawExtended
					&& event.index == table.getColumnCount() - 1) {
				tv.swt_calculateClientArea();
				drawBounds = new Rectangle(bounds.x, bounds.y, tv.clientArea.x
						+ tv.clientArea.width - bounds.x, bounds.height);
				event.gc.setClipping(drawBounds);
				//System.out.println(bounds.width);
			}
			event.gc.fillRectangle(drawBounds);
			event.detail &= ~SWT.BACKGROUND;
		}
		
		if ((event.detail & SWT.SELECTED) > 0 && !table.isFocusControl()) {
			event.gc.setBackground(Colors.blues[3]);
			event.gc.fillRectangle(bounds);
			event.detail &= ~SWT.BACKGROUND;
		}

		// Vertical lines between columns
		if (TableViewSWTImpl.DRAW_VERTICAL_LINES && drawExtended) {
			if (item != null
					&& (bounds.width == item.getParent().getColumn(event.index).getWidth())) {
				//System.out.println(bounds.width + ";" + item.getParent().getColumn(event.index).getWidth());
				Color fg = event.gc.getForeground();
				event.gc.setForeground(Colors.black);
				event.gc.setAlpha(10);
				event.gc.setClipping((Rectangle) null);
				event.gc.drawLine(bounds.x + bounds.width - 1, bounds.y - 1, bounds.x
						+ bounds.width - 1, bounds.y + bounds.height);
				event.gc.setForeground(fg);
			}
		}
	}
}
