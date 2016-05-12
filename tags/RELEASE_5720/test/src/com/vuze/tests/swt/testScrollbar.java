package com.vuze.tests.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.*;

public class testScrollbar
{

	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display, SWT.SHELL_TRIM);
		shell.setLayout(new FillLayout());

		
		final Table c = new Table(shell, SWT.VIRTUAL);
		
		c.setItemCount(5000);
		
		ScrollBar bar = c.getVerticalBar();
		
		bar.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				System.out.println(event.time + "] Selection! Top now " + c.getTopIndex());
			}
		});
		
		c.addListener(SWT.MouseWheel, new Listener() {
			public void handleEvent(Event event) {
				System.out.println(event.time + "] MouseWheel! Top now " + c.getTopIndex());
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
