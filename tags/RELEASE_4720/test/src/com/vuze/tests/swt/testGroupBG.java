package com.vuze.tests.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

public class testGroupBG
{
	public static void main(String[] args) {
		Display display = new Display();
		Color color = new Color(display, 180, 180, 200);

		Shell shell = new Shell(display, SWT.SHELL_TRIM);
		shell.setLayout(new FillLayout());

		Composite c1 = new Composite(shell, SWT.NONE);
		c1.setLayout(new FillLayout());
		c1.setBackground(color);
		c1.setBackgroundMode(SWT.INHERIT_DEFAULT);

		Composite c2 = new Composite(shell, SWT.NONE);
		c2.setLayout(new FillLayout());
		c2.setBackgroundMode(SWT.INHERIT_DEFAULT);

		makeGroup(c1, "Group Test (BG)");
		makeGroup(c2, "Group Test (No BG)");

		shell.open();
		while (!shell.isDisposed())
			if (!display.readAndDispatch())
				display.sleep();
	}

	private static void makeGroup(Composite parent, String title) {
		Group group = new Group(parent, SWT.NONE);
		group.setText(title);
		group.setLayout(new RowLayout());
		
		String s = "This is a line on SWT " + SWT.getPlatform() + "/"
		+ SWT.getVersion();
		for (int i = 0; i < 10; i++) {
			new Label(group, SWT.LEFT).setText(s);
			new Button(group, SWT.CHECK).setText("Checkbox");
		}
	}
}
