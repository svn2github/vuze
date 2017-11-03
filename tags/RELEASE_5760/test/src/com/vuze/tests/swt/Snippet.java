/**
 * Created on Aug 17, 2010
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */
 
package com.vuze.tests.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.widgets.*;

/**
 * @author TuxPaper
 * @created Aug 17, 2010
 *
 */
public class Snippet
{
	public static void main (String[] args) {
	    Display d = new Display();
	    Shell shell = new Shell(d);
	    shell.setSize(500, 500);
	    shell.addListener(SWT.Paint, new Listener() {
	        public void handleEvent(Event e) {
	            TextLayout layout = new TextLayout(e.display);
	            //U+07C0 to U+07FA 
	            layout.setText("1) Magic Symbol - \u07d0");
	            layout.draw(e.gc, 10, 10);
	            e.gc.drawText("2) Magic Symbol - \u07d0", 160, 10);
	            e.gc.setAlpha(127);
	            layout.draw(e.gc, 10, 50);
	            e.gc.drawText("3) Magic Symbol - \u07d0", 160, 50);
	            layout.dispose();
	        }
	    });
	    shell.open();
	    while (!shell.isDisposed()) {
	        if (!d.readAndDispatch())
	            d.sleep();
	    }
	    d.dispose();
	}
	
}

