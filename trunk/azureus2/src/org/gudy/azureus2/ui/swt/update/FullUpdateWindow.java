/**
 * Created on June 29th, 2009
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

package org.gudy.azureus2.ui.swt.update;


import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.*;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;

import com.aelitis.azureus.ui.UIFunctions;


public class FullUpdateWindow
{
	private static Shell shell = null;

	private static Browser browser;

	public static void 
	handleUpdate(
		final String						url,
		final UIFunctions.actionListener	listener )
	{
		try{
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					open( url, listener );
				}
			});
			
		}catch( Throwable e ){
			
			Debug.out( e );
			
			listener.actionComplete( false );
		}
	}

	public static void 
	open(
		final String 						url,
		final UIFunctions.actionListener	listener ) 
	{
		boolean	ok = false;
		
		try{
			if ( shell != null && !shell.isDisposed()){
				
				return;
			}
			
			final Shell parentShell = Utils.findAnyShell();
			
			shell = ShellFactory.createShell(parentShell, SWT.BORDER
					| SWT.APPLICATION_MODAL | SWT.TITLE | SWT.DIALOG_TRIM );
			
			shell.setLayout(new FillLayout());
			
			if (parentShell != null) {
				parentShell.setCursor(shell.getDisplay().getSystemCursor(SWT.CURSOR_WAIT));
			}
			
			shell.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					try{
						if (parentShell != null) {
							parentShell.setCursor(e.display.getSystemCursor(SWT.CURSOR_ARROW));
						}
						shell = null;
						
					}finally{
						
						listener.actionComplete( false );
					}
				}
			});
	
			try {
				browser = new Browser(shell, Utils.getInitialBrowserStyle(SWT.NONE));
			} catch (Throwable t) {
				shell.dispose();
				return;
			}
	
			browser.addTitleListener(new TitleListener() {
				public void changed(TitleEvent event) {
					shell.setText(event.title);
				}
			});
	
			browser.addStatusTextListener(new StatusTextListener() {
				String last = null;
	
				public void changed(StatusTextEvent event) {
					String text = event.text.toLowerCase();
					if (last != null && last.equals(text)) {
						return;
					}
					last = text;
					if ( text.contains("page-loaded")) {
						
						Utils.centreWindow(shell);
						if (parentShell != null) {
							parentShell.setCursor(shell.getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
						}
						shell.open();
					}
				}
			});
	
			browser.addLocationListener(new LocationListener() {
				public void changing(LocationEvent event) {
				}
	
				public void changed(LocationEvent event) {
				}
			});
	
			String final_url = url + ( url.indexOf('?')==-1?"?":"&") + 
						"locale=" + Locale.getDefault().toString() + 
						"&azv=" + Constants.AZUREUS_VERSION; 
	
			SimpleTimer.addEvent(
				"fullupdate.pageload", 
				SystemTime.getOffsetTime(5000),
				new TimerEventPerformer() {
					public void perform(TimerEvent event) {
						Utils.execSWTThread(new AERunnable() {
							public void runSupport() {
								if ( !shell.isDisposed()){
								
									shell.open();
								}
							}
						});
					}
				});
			
			browser.setUrl(final_url);
			
			ok = true;
			
		}finally{
			
			if ( !ok ){
				
				listener.actionComplete( false );
			}
		}
	}

	public static void 
	main(String[] args) 
	{
		try {
			open( 
				"http://www.google.com", 
				new UIFunctions.actionListener()
				{
					public void actionComplete(Object result) {
						System.out.println( "result=" + result );
						
						System.exit(1);
					}
				});
		} catch (Exception e) {
			e.printStackTrace();
		}
		Display d = Display.getDefault();
		while (true) {
			if (!d.readAndDispatch()) {
				d.sleep();
			}
		}
	}

}