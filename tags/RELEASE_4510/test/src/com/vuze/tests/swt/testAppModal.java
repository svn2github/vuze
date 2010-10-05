package com.vuze.tests.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.*;

public class testAppModal
{

	public static void main(String[] args) {
		Display display = new Display();
		Shell shellMain = new Shell(display, SWT.SHELL_TRIM);
		shellMain.setLayout(new FillLayout());

		final Label label = new Label(shellMain, SWT.NONE);
		label.setBackground(display.getSystemColor(SWT.COLOR_YELLOW));
		label.setText("Main");

		
		final Shell shellModal = new Shell(shellMain, SWT.SHELL_TRIM | SWT.PRIMARY_MODAL);
		Button btnAbove = new Button(shellModal, SWT.PUSH);
		btnAbove.setText("PUSH");
		shellModal.setLayout(new FillLayout());
		
		shellModal.open();
		
		
		final Shell shell2 = new Shell(shellMain, SWT.SHELL_TRIM);
		shell2.setText("NonModal");
		shell2.open();

		btnAbove.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				shellModal.moveAbove(null);
				shellModal.forceActive();
			}
		});

		shellMain.open();

		while (!shellMain.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}
}
