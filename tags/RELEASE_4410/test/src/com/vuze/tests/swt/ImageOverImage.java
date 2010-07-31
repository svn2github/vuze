package com.vuze.tests.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class ImageOverImage
{
	public static void main(String[] args) {
		FormData formData;

		Display display = new Display();

		Shell shell = new Shell(display, SWT.DIALOG_TRIM);

		FormLayout layout = new FormLayout();
		shell.setLayout(layout);

		Image image1 = new Image(display, 100, 100);

		GC gc = new GC(image1);
		gc.setBackground(display.getSystemColor(SWT.COLOR_BLUE));
		gc.fillRectangle(0, 0, 100, 100);
		gc.dispose();

		final Image image2 = new Image(display, 50, 50);
		gc = new GC(image2);
		gc.setBackground(display.getSystemColor(SWT.COLOR_RED));
		gc.fillRectangle(0, 0, 50, 50);
		gc.dispose();

		final Image image3 = new Image(display, 50, 50);
		gc = new GC(image2);
		gc.setBackground(display.getSystemColor(SWT.COLOR_GREEN));
		gc.fillRectangle(0, 0, 50, 50);
		gc.dispose();

		Composite c1 = new Composite(shell, SWT.BORDER);
		formData = new FormData();
		formData.top = new FormAttachment(0, 0);
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100);
		formData.bottom = new FormAttachment(100);
		c1.setLayoutData(formData);
		c1.setLayout(new FormLayout());

		final Composite c2 = new Composite(c1, SWT.BORDER);
		formData = new FormData();
		formData.top = new FormAttachment(00, 10);
		formData.left = new FormAttachment(00, 10);
		formData.right = new FormAttachment(100, -10);
		formData.bottom = new FormAttachment(100, -10);
		c2.setLayoutData(formData);

		c1.setBackgroundImage(image1);
		c2.setBackgroundImage(image2);

		c2.addMouseTrackListener(new MouseTrackListener() {

			public void mouseHover(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			public void mouseExit(MouseEvent e) {
				c2.setBackgroundImage(image2);
			}

			public void mouseEnter(MouseEvent e) {
				c2.setBackgroundImage(image3);
			}

		});

		shell.setSize(100, 100);
		shell.open();

		while (!display.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}
}
