package com.vuze.tests.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.*;

public class testTableMouseUp
{

	public static void main(String[] args) {
		Display display = new Display();
		Shell shellMain = new Shell(display, SWT.SHELL_TRIM);
		shellMain.setLayout(new FillLayout());

		Table table = new Table(shellMain, SWT.NONE);
		table.setLinesVisible(true);
		table.addMouseListener(new MouseListener() {

			public void mouseUp(MouseEvent e) {
				System.out.println("MouseUp " + e.time);
			}

			public void mouseDown(MouseEvent e) {
				System.out.println("\n\nMouseDown");
			}

			public void mouseDoubleClick(MouseEvent e) {
			}
		});
		
		new TableColumn(table, 0).setWidth(1000);
		new TableColumn(table, 0).setWidth(1000);
		
		table.setSortColumn(table.getColumn(0));
		table.setSortDirection(SWT.UP);
		
		table.addListener(SWT.PaintItem, new Listener() {
			
			public void handleEvent(Event event) {
				// TODO Auto-generated method stub
				event.gc.drawText("Hi", event.x + 100, event.y, true);
			}
		});

		System.out.println(SWT.getPlatform() + ";" + SWT.getVersion());
		TableItem tableItem = new TableItem(table, SWT.NONE);
		tableItem.setText("               click me");
		tableItem = new TableItem(table, SWT.NONE);
		tableItem.setText("               click me");
		tableItem = new TableItem(table, SWT.NONE);
		tableItem.setText("               click me");
		tableItem = new TableItem(table, SWT.NONE);
		tableItem.setText("               click me");
		tableItem.setBackground(display.getSystemColor(SWT.COLOR_GREEN));
		
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.addListener(SWT.PaintItem, new Listener() {
			
			public void handleEvent(Event event) {
				event.gc.drawText("WOW", 0, 0, true);
			}
		});
		table.addListener(SWT.EraseItem, new Listener() {
			
			public void handleEvent(Event event) {
			}
		});

		// XXX Remove this line to get only one MouseUp
		new DragSource(table, DND.DROP_COPY);


		shellMain.open();

		while (!shellMain.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}
}
