package com.vuze.tests.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.*;

public class textComboColor
{

	public static void main(String[] args) {
		final Display display = new Display();
		Shell shell = new Shell(display, SWT.SHELL_TRIM);
		shell.setLayout(new FillLayout(SWT.VERTICAL));

		final Combo combo = new Combo(shell, SWT.DROP_DOWN);
		combo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				Color color = combo.getText().length() % 2 == 0 ? null
						: display.getSystemColor(SWT.COLOR_BLUE);
				combo.setBackground(color);
			}
		});
		
		Text btn = new Text(shell, SWT.PUSH);
		btn.setText("text to tab to");

		shell.open();
		combo.setFocus();

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}
}
