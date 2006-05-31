/*
 * Created on May 28, 2006 4:31:42 PM
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.debug;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;

/**
 * @author TuxPaper
 * @created May 28, 2006
 *
 */
public class UIDebugGenerator
{
	public static void generate() {
		Display display = Display.getCurrent();
		if (display == null) {
			return;
		}

		// make sure display is up to date
		if (!display.readAndDispatch())
			display.sleep();

		Shell[] shells = display.getShells();
		if (shells == null || shells.length == 0) {
			return;
		}

		for (int i = 0; i < shells.length; i++) {
			Shell shell = shells[i];
			Image image;

			if (shell.getData("class") instanceof ObfusticateShell) {
				ObfusticateShell shellClass = (ObfusticateShell) shell.getData("class");

				image = shellClass.generateObfusticatedImage();
			} else {

				Rectangle clientArea = shell.getClientArea();
				image = new Image(display, clientArea.width, clientArea.height);

				GC gc = new GC(shell);
				try {
					gc.copyArea(image, clientArea.x, clientArea.y);
				} finally {
					gc.dispose();
				}
			}

			if (image != null) {
				String sFileName = "C:\\temp\\image-" + i + ".jpg";
				ImageLoader imageLoader = new ImageLoader();
				imageLoader.data = new ImageData[] { image.getImageData() };
				imageLoader.save(sFileName, SWT.IMAGE_JPEG);

				Program.launch(sFileName);
			}
		}
	}

	/**
	 * @param image
	 * @param bounds
	 */
	public static void obfusticateArea(Image image, Rectangle bounds) {
		GC gc = new GC(image);
		try {
			gc.setBackground(image.getDevice().getSystemColor(SWT.COLOR_WHITE));
			gc.setForeground(image.getDevice().getSystemColor(SWT.COLOR_RED));
			gc.fillRectangle(bounds);
			gc.drawRectangle(bounds);
			int x2 = bounds.x + bounds.width;
			int y2 = bounds.y + bounds.height;
			gc.drawLine(bounds.x, bounds.y, x2, y2);
			gc.drawLine(x2, bounds.y, bounds.x, y2);
		} finally {
			gc.dispose();
		}
	}

	/**
	 * @param image
	 * @param bounds
	 * @param text
	 */
	public static void obfusticateArea(Image image, Rectangle bounds, String text)
	{

		if (bounds.isEmpty())
			return;

		if (text == "") {
			obfusticateArea(image, bounds);
			return;
		}

		GC gc = new GC(image);
		try {
			gc.setBackground(image.getDevice().getSystemColor(SWT.COLOR_WHITE));
			gc.setForeground(image.getDevice().getSystemColor(SWT.COLOR_RED));
			gc.fillRectangle(bounds);
			gc.drawRectangle(bounds);
			gc.setClipping(bounds);
			gc.drawText(text, bounds.x + 2, bounds.y + 1);
		} finally {
			gc.dispose();
		}
	}

	/**
	 * @param image
	 * @param control
	 * @param shellOffset 
	 * @param text 
	 */
	public static void obfusticateArea(Image image, Control control,
			Point shellOffset, String text)
	{
		Rectangle bounds = control.getBounds();
		Point offset = control.getParent().toDisplay(bounds.x, bounds.y);
		bounds.x = offset.x - shellOffset.x;
		bounds.y = offset.y - shellOffset.y;

		obfusticateArea(image, bounds, text);
	}
}
