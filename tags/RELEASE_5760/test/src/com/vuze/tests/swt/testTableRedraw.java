/**
 * Created on Jan 18, 2011
 *
 * Copyright 2010 Vuze, Inc.  All rights reserved.
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
 
package com.vuze.tests.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

/**
 * @author TuxPaper
 * @created Jan 18, 2011
 *
 */
public class testTableRedraw
{

	public static void main(String[] args) {
		final Display display = new Display();
		Shell shellMain = new Shell(display, SWT.SHELL_TRIM);
		GridLayout l = new GridLayout();
		l.marginHeight = l.marginWidth = 10;
		shellMain.setLayout(l);

		
		final Tree table = new Tree(shellMain, SWT.DOUBLE_BUFFERED | SWT.FULL_SELECTION);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table.setHeaderVisible(true);

		for (int i = 0; i < 6; i++) {
			TreeColumn tableColumn = new TreeColumn(table, SWT.NONE);
			tableColumn.setWidth(99);
			tableColumn.setText("" + i);
		}
		
		for (int i = 0; i < 20; i++) {
			TreeItem tableItem = new TreeItem(table, SWT.None);
			tableItem.setText(new String[] { "Some", "Text", "For", "Testing", "Tree", "Speed" });
		}
		
		Runnable runRedrawCell = new Runnable() {
			public void run() {
				//System.out.println(System.currentTimeMillis());
				if (table.isDisposed()) {
					return;
				}
				display.timerExec(500, this);

				for (int i = 0; i < 10; i++) {
					int row = (int) (Math.random() * table.getItemCount());
					int col = (int) (Math.random() * table.getColumnCount());
  				TreeItem item = table.getItem(row);
  				Rectangle bounds = item.getBounds(col);
  				
  				//System.out.print(row + "x" + col + ",");
  				
  				GC gc = new GC(table);
  				gc.drawRectangle(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
  				gc.dispose();
  				
  				table.redraw(bounds.x, bounds.y, bounds.width, bounds.height, true);
				}
				//System.out.println();
			}
		};
		
		display.timerExec(500, runRedrawCell);

		final Listener listenerPaintItem = new Listener() {
			public void handleEvent(Event event) {
				//System.out.println("Paint");
			}
		};
		final Listener listenerEraseItem = new Listener() {
			public void handleEvent(Event event) {
				event.gc.setBackground(event.display.getSystemColor((int)(Math.random() * 16)));
				event.gc.fillRectangle(event.getBounds());
			}
		};
		
		Button btnToggleListeners = new Button(shellMain, SWT.TOGGLE);
		btnToggleListeners.setText("PaintListeners");
		btnToggleListeners.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				if (((Button) event.widget).getSelection()) {
					table.addListener(SWT.PaintItem, listenerPaintItem);
					table.addListener(SWT.EraseItem, listenerEraseItem);
				} else {
					table.removeListener(SWT.PaintItem, listenerPaintItem);
					table.removeListener(SWT.EraseItem, listenerEraseItem);
				}
			}
		});
		
		
		////////////////

		shellMain.setSize(800, 600);
		shellMain.open();

		while (!shellMain.isDisposed()) {
			try {
				long last = System.currentTimeMillis();

				while (display.readAndDispatch());

				long now = System.currentTimeMillis();
				long diff = now - last;
				last = now;

				if (diff > 0) {
					System.out.println(diff);
				}

				display.sleep();
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
		display.dispose();
	}
}
