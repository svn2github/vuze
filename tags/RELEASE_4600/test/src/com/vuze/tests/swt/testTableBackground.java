package com.vuze.tests.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public class testTableBackground
{

	public static void main(String[] args) {
		Display display = new Display();
		Shell shellMain = new Shell(display, SWT.SHELL_TRIM);
		GridLayout l = new GridLayout();
		l.marginHeight = l.marginWidth = 10;
		shellMain.setLayout(l);

		final Tree tree = new Tree(shellMain, SWT.BORDER | SWT.VIRTUAL
				| SWT.FULL_SELECTION);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		tree.setLayoutData(gridData);

		new TreeColumn(tree, SWT.RIGHT);

		tree.setHeaderVisible(true);
		tree.getColumn(0).setWidth(300);

		int size = 200;
		Image image = new Image(display, size, size);
		GC gc = new GC(image);
		gc.setForeground(display.getSystemColor(SWT.COLOR_GRAY));
		gc.drawOval(0, 0, size, size);
		gc.dispose();
		tree.setBackgroundImage(image);

		Button button = new Button(shellMain, SWT.PUSH);
		button.setText("Add Row");
		button.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				new TreeItem(tree, SWT.NONE).setText("row");
			}
		});

		Button button2 = new Button(shellMain, SWT.PUSH);
		button2.setText("Add Row And Redraw");
		button2.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				new TreeItem(tree, SWT.NONE).setText("row");
				tree.redraw();
			}
		});

		////////////////

		shellMain.setSize(400, 600);
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
