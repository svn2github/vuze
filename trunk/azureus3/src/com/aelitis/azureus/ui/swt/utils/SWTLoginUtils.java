/**
 * Created on May 6, 2008
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

package com.aelitis.azureus.ui.swt.utils;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.ui.swt.shells.LightBoxBrowserWindow;
import com.aelitis.azureus.util.Constants;
import com.aelitis.azureus.util.ILoginInfoListener;
import com.aelitis.azureus.util.LoginInfoManager;
import com.aelitis.azureus.util.LoginInfoManager.LoginInfo;

/**
 * @author TuxPaper
 * @created May 6, 2008
 *
 */
public class SWTLoginUtils
{
	public static void waitForLogin(final loginWaitListener l) {
		if (l == null) {
			return;
		}

		final AERunnable loginCompleteRunnable = new AERunnable() {
			public void runSupport() {
				l.loginComplete();
			}
		};

		final AERunnable loginCancelRunnable = new AERunnable() {
			public void runSupport() {
				l.loginCanceled();
			}
		};

		final LoginInfoManager loginManager = LoginInfoManager.getInstance();
		if (loginManager.isLoggedIn()) {
			Utils.execSWTThread(loginCompleteRunnable);
			return;
		}

		final ILoginInfoListener loginInfoListener = new ILoginInfoListener() {
			public void loginUpdate(LoginInfo info, boolean isNewLoginID) {
				if (loginManager.isLoggedIn()) {
					loginManager.removeListener(this);
					Utils.execSWTThread(loginCompleteRunnable);
				}
			}
		};

		loginManager.addListener(loginInfoListener);
		LightBoxBrowserWindow loginWindow = openLoginWindow(l.getOptionalMessage());
		loginWindow.setCloseListener(new LightBoxBrowserWindow.closeListener() {
			public void close() {
				SimpleTimer.addEvent("cancel login wait",
						SystemTime.getOffsetTime(l.getCancelDelay()),
						new TimerEventPerformer() {
							public void perform(TimerEvent event) {
								loginManager.removeListener(loginInfoListener);
								if (!loginManager.isLoggedIn()) {
									Utils.execSWTThread(loginCancelRunnable);
								}
							}
						});
			}
		});
	}

	/**
	 * 
	 * @return
	 * @since 3.0.5.3
	 */
	public static LightBoxBrowserWindow openLoginWindow() {
		return openLoginWindow(null);
	}

	/**
	 * 
	 * @param optionalMessage an optional message to display in the Sign In window
	 * @return
	 * @since 3.0.5.3
	 */
	public static LightBoxBrowserWindow openLoginWindow(String optionalMessage) {
		String url = Constants.URL_PREFIX + Constants.URL_LOGIN + "?";
		if (null == optionalMessage || optionalMessage.length() < 1) {
			url += Constants.URL_SUFFIX;
		} else {
			url += "msg=" + optionalMessage;
			url += "&" + Constants.URL_SUFFIX;
		}

		return new LightBoxBrowserWindow(url, Constants.URL_PAGE_VERIFIER_VALUE,
				380, 280);
	}

	public static abstract class loginWaitListener
	{

		private String message = null;

		/**
		 * The milliseconds to wait before firing off a cancel event
		 */
		private long cancelDelay;

		/**
		 * This will be on the SWT thread
		 *
		 * @since 3.0.5.3
		 */
		public abstract void loginComplete();

		public loginWaitListener() {
			init();
		}

		public void init() {
			cancelDelay = 10000;
		}

		public String getOptionalMessage() {
			if (null == message) {
				message = MessageText.getString("login.optional.message");
				message = message.replaceAll(" ", "+");
			}
			return message;
		}

		public void loginCanceled() {
		}

		public long getCancelDelay() {
			return cancelDelay;
		}

		public void setCancelDelay(long cancelDelay) {
			this.cancelDelay = cancelDelay;
		};
	}

}
