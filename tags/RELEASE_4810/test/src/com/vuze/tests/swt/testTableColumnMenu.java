package com.vuze.tests.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.*;

public class testTableColumnMenu
{

	public static void main(String[] args) {
		Display display = new Display();
		Shell shellMain = new Shell(display, SWT.SHELL_TRIM);
		shellMain.setLayout(new FillLayout(SWT.VERTICAL));

		Table table = new Table(shellMain, SWT.BORDER);
		table.setHeaderVisible(true);
		TableColumn column = new TableColumn(table, SWT.RIGHT);
		column.setText("Column");
		table.setSortColumn(column);
		table.setSortDirection(SWT.UP);
		new TableItem(table, SWT.NONE).setText("No EraseItem");
		table.getColumn(0).setWidth(300);

		table.select(0);
		
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
