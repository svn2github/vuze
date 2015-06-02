/**
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.vuze.tests.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.ui.swt.Utils;

/**
 * Eclipse Bug 164512:
 * seizure inducing flicker on resize in Browser w/parent having paint listener
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=164512
 * 
 * @author TuxPaper
 * @created Nov 13, 2006
 *
 */
public class BrowserFlicker
{
	final static int INDENT = 10;

	public static void main(String[] args) {
		final Display display = new Display();
		final Shell shell = new Shell(display, SWT.SHELL_TRIM);

		FormData fd;

		shell.setLayout(new FormLayout());

		final Composite right = new Composite(shell, SWT.NONE);
		right.setLayout(new FormLayout());

		final Browser b = new Browser(right, Utils.getInitialBrowserStyle(SWT.NONE));
		fd = new FormData();
		fd.top = new FormAttachment(0, INDENT);
		fd.left = new FormAttachment(0, INDENT);
		fd.right = new FormAttachment(100, -INDENT);
		fd.bottom = new FormAttachment(100, -INDENT);
		b.setLayoutData(fd);
		// black so we can see the flicker better
		b.setText("<html><body BGCOLOR=black></body></html>");

		fd = new FormData();
		fd.top = new FormAttachment(0);
		fd.left = new FormAttachment(50);
		fd.bottom = new FormAttachment(100);
		fd.right = new FormAttachment(100);
		right.setLayoutData(fd);

		shell.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event event) {
				// code here to resulting in a need to re-layout

				right.getParent().layout();
			}
		});

		Listener l = new Listener() {
			public void handleEvent(Event event) {
				Point size = ((Control) event.widget).getSize();
				event.gc.setBackground(display.getSystemColor(SWT.COLOR_BLUE));
				event.gc.fillOval(0, 0, INDENT, INDENT);
				event.gc.fillOval(size.x - INDENT, 0, INDENT, INDENT);
				event.gc.fillOval(size.x - INDENT, size.y - INDENT, INDENT, INDENT);
				event.gc.fillOval(0, size.y - INDENT, INDENT, INDENT);

				// mimic other work
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		};

		right.addListener(SWT.Paint, l);
		shell.setSize(200, 200);
		shell.open();

		while (!shell.isDisposed()) {
			if (display.readAndDispatch()) {
				display.sleep();
			}
		}
	}
}
