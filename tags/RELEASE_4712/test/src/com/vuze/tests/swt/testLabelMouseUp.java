package com.vuze.tests.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.*;

public class testLabelMouseUp
{

	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display, SWT.SHELL_TRIM);
		shell.setLayout(new FillLayout());

		final Button checkbox = new Button(shell, SWT.CHECK);
		checkbox.setSelection(true);
		checkbox.setText("SetImage in MouseDown");
		
		final Label label = new Label(shell, SWT.NONE);
		label.setBackground(display.getSystemColor(SWT.COLOR_YELLOW));
		label.setText("Click Me");

		label.addListener(SWT.MouseDown, new Listener() {
			public void handleEvent(Event event) {
				System.out.println("Mouse Down");

				// This prevents MouseUp from firing
				if (checkbox.getSelection()) {
					label.setImage(null);
				}
			}
		});

		label.addListener(SWT.MouseUp, new Listener() {
			public void handleEvent(Event event) {
				System.out.println("Mouse Up");
			}
		});

		shell.open();

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}
}
