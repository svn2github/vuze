/**
 * Created on May 15, 2008
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

package com.aelitis.azureus.ui.swt.browser;

import java.net.URL;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.core.messenger.PlatformAuthorizedSender;
import com.aelitis.azureus.core.messenger.PlatformMessenger;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.utils.SWTLoginUtils;
import com.aelitis.azureus.util.ConstantsV3;

/**
 * @author TuxPaper
 * @created May 15, 2008
 *
 */
public class PlatformAuthorizedSenderImpl
	implements PlatformAuthorizedSender
{
	String s = null;

	private Shell shell = null;

	/**
	 * 
	 */
	public PlatformAuthorizedSenderImpl() {
	}

	// @see com.aelitis.azureus.core.messenger.PlatformAuthorizedSender#startDownload(java.net.URL, java.lang.String, org.gudy.azureus2.core3.util.AESemaphore, boolean)
	public void startDownload(final URL url, final String data,
			final AESemaphore sem_waitDL, final boolean loginAndRetry) {
		Utils.execSWTThread(new AERunnable() {
			boolean[] isRetry = { false };

			public void runSupport() {
				try {
					boolean ourShell = false;
					Shell shell = PlatformAuthorizedSenderImpl.this.shell;
					if (shell == null) {
						UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
						if (uiFunctions != null) {
							shell = uiFunctions.getMainShell();
						}
						if (shell != null) {
							PlatformAuthorizedSenderImpl.this.shell = shell;
						} else {
							shell = new Shell();
							shell.setVisible(false);
							ourShell= true;
						}
					}
					final Browser browser = new Browser(shell,
							Utils.getInitialBrowserStyle(SWT.NONE));
					browser.setVisible(false);
					if (ourShell) {
						final Shell shellFinal = shell; 
						browser.addDisposeListener(new DisposeListener() {
							public void widgetDisposed(DisposeEvent e) {
								// better safe than sorry, don't dispose shell while
								// the browser which is in the shell is being disposed
								Utils.execSWTThreadLater(0, new AERunnable() {
									public void runSupport() {
										shellFinal.dispose();
									}
								});
							}
						});
					}

					// Safari doesn't return getText() when results aren't text/html
					// IE removes /n when in text/html mode
					String responseType = ConstantsV3.isOSX ? "text/html" : "text/plain";
					
					final String fullUrl = url + "?" + data
							+ "&responseType=" + responseType;
					PlatformMessenger.debug("Open Auth URL: "
							+ url + " in " + responseType + "\n" + fullUrl);

					browser.addProgressListener(new ProgressListener() {
						public void completed(ProgressEvent event) {
							parseAuthorizedListenerResult(browser, sem_waitDL, isRetry,
									loginAndRetry);
						}

						public void changed(ProgressEvent event) {
						}
					});
					
					browser.setUrl(fullUrl);

				} catch (Throwable e) {

					Debug.printStackTrace(e);

					sem_waitDL.release();
				}
			}
		});
	}

	private void parseAuthorizedListenerResult(final Browser browser,
			final AESemaphore sem_waitDL, boolean[] isRetry, boolean loginAndRetry) {
		if (browser.isDisposed()) {
			sem_waitDL.release();
			return;
		}
		
		boolean	went_async = false;
		
		try {
			s = browser.getText();

			// authFail message is "authentication required"
			// catch a little bit more, just in case 
			boolean authFail = s.indexOf(";exception;") > 0
					&& s.indexOf("authenticat") > 0 && s.indexOf("required") > 0;

			int i = s.indexOf("0;");

			if (i < 0) {
				String partial = s.length() == 0 ? "" : s.substring(0, Math.min(200,
						s.length()));
				PlatformMessenger.debug("Got BAD Auth Reply ( " + s.length() + "): "
						+ partial);
			}

			if ( authFail && loginAndRetry && !isRetry[0]){
				
				s = null;

				isRetry[0] = true;

				went_async = true;
				
				SWTLoginUtils.waitForLogin(new SWTLoginUtils.loginWaitListener() {
					public void loginComplete() {
						// once the page is reloaded ProgressListener.complete will be
						// triggered again
						
						browser.refresh();
					}
					
					public void loginCanceled() {
						browser.dispose();
						sem_waitDL.release();
					}
				});
			} else {
				if (i > 0) {
					s = s.substring(i);
				}
				// On PPC, we get a JVM crash on disposal, so maybe this delay will
				// fix it.
				if (ConstantsV3.isOSX) {
					Utils.execSWTThreadLater(0, new AERunnable() {
						public void runSupport() {
							browser.dispose();
						}
					});
				} else {
					browser.dispose();
				}
			}
		} finally {
			
			if ( !went_async ){
			
				sem_waitDL.release();
			}
		}
	}

	public String getResults() {
		return s;
	}
	
	public void clearResults() {
		s = null;
	}
}
