package com.vuze.tests.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.*;

public class testTableDrop
{

	public static void main(String[] args) {
		Display display = new Display();
		Shell shellMain = new Shell(display, SWT.SHELL_TRIM);
		shellMain.setLayout(new FillLayout());

		Table table = new Table(shellMain, SWT.BORDER);
		table.setHeaderVisible(true);
		new TableColumn(table, 0);
		table.getColumn(0).setText("Drag a file to the table");
		table.getColumn(0).setWidth(300);

		Transfer[] transferList = new Transfer[] {
			FileTransfer.getInstance(),
		};

		DropTarget dropTarget = new DropTarget(table, DND.DROP_DEFAULT | DND.DROP_MOVE);
		dropTarget.setTransfer(transferList);

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
