package com.vuze.tests.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.*;

public class testTableFullRow
{

	public static void main(String[] args) {
		Display display = new Display();
		Shell shellMain = new Shell(display, SWT.SHELL_TRIM);
		shellMain.setLayout(new FillLayout(SWT.VERTICAL));

		Table table = new Table(shellMain, SWT.BORDER);
		new TableColumn(table, SWT.RIGHT);
		new TableItem(table, SWT.NONE).setText("No EraseItem");
		table.getColumn(0).setWidth(300);

		table.select(0);
		
		/////////////////////

		Table table2 = new Table(shellMain, SWT.BORDER);
		new TableColumn(table2, SWT.RIGHT);
		new TableItem(table2, SWT.NONE).setText("Has EraseItem");
		table2.getColumn(0).setWidth(300);

		table2.addListener(SWT.EraseItem, new Listener() {
			public void handleEvent(Event event) {
			}
		});
		
		table2.select(0);

		////////////////
		
		shellMain.setSize(400, 200);
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
