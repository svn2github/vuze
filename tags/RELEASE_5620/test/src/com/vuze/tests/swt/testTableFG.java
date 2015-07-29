package com.vuze.tests.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.*;

public class testTableFG
{

	public static void main(String[] args) {
		Display display = new Display();
		Shell shellMain = new Shell(display, SWT.SHELL_TRIM);
		shellMain.setLayout(new FillLayout(SWT.VERTICAL));

		Table table = new Table(shellMain, SWT.BORDER);
		new TableColumn(table, SWT.RIGHT);
		new TableItem(table, SWT.NONE).setText("setText");
		table.getColumn(0).setWidth(300);

		table.addListener(SWT.PaintItem, new Listener() {
			public void handleEvent(Event event) {
				Rectangle bounds = ((TableItem) event.item).getBounds(event.index);
				event.gc.fillRectangle(bounds.x + 5, bounds.y, 10, bounds.height);
				event.gc.drawText("drawnText/No EraseItem", bounds.x + 20, bounds.y, true);
			}
		});
		
		/////////////////////

		Table table2 = new Table(shellMain, SWT.BORDER);
		new TableColumn(table2, SWT.RIGHT);
		new TableItem(table2, SWT.NONE).setText("setText");
		table2.getColumn(0).setWidth(300);

		table2.addListener(SWT.PaintItem, new Listener() {
			public void handleEvent(Event event) {
				Rectangle bounds = ((TableItem) event.item).getBounds(event.index);
				event.gc.fillRectangle(bounds.x + 5, bounds.y, 10, bounds.height);
				event.gc.drawText("drawnText/EraseItem", bounds.x + 20, bounds.y, true);
				
			}
		});
		
		table2.addListener(SWT.EraseItem, new Listener() {
			public void handleEvent(Event event) {
			}
		});

		////////////////
		
		shellMain.open();

		while (!shellMain.isDisposed()) {
			try {
				if (!display.readAndDispatch())
					display.sleep();
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
		display.dispose();
	}
}
