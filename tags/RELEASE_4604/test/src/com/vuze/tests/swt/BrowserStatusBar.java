/**
 * Created on Oct 6, 2010
 *
 * Copyright 2010 Vuze, Inc.  All rights reserved.
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
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.StatusTextEvent;
import org.eclipse.swt.browser.StatusTextListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;

/**
 * @author TuxPaper
 * @created Oct 6, 2010
 *
 */
public class BrowserStatusBar
{
	public static void main(String[] args) {
		Display display = new Display();
		final Shell shell = new Shell(display, SWT.SHELL_TRIM);
		shell.setLayout(new FillLayout());

		final Browser b1 = new Browser(shell, SWT.NONE);
		//b1.setUrl(" http://www.vuze.com/client/update.php");
		//b1.setUrl("file://c:/temp/update.html");
		b1.setUrl(" http://192.168.0.88/client/update.php");

		b1.addStatusTextListener(new StatusTextListener() {
			public void changed(StatusTextEvent event) {
				System.out.println("changed " + event.text);
				if ( event.text.contains("accept")){
					Utils.execSWTThreadLater(0, new AERunnable() {	
						public void runSupport(){
							
							try{
								System.out.println("FOOOOOOOOOO");
								
							}catch( Throwable e ){
								
								Debug.out( e );
							}
							
							shell.dispose();
						}
					});
				}
			}
		});
		shell.open();

		while (!shell.isDisposed ()) {
			if (!display.readAndDispatch ())
				display.sleep ();
		}
		display.dispose ();
	}
}
