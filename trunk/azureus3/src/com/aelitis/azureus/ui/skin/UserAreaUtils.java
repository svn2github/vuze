/**
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.ui.skin;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.shells.LightBoxBrowserWindow;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.util.Constants;
import com.aelitis.azureus.util.ILoginInfoListener;
import com.aelitis.azureus.util.LoginInfoManager;
import com.aelitis.azureus.util.LoginInfoManager.LoginInfo;

public class UserAreaUtils
{
	private SWTSkin skin;

	private UIFunctionsSWT uiFunctions = null;

	public UserAreaUtils(SWTSkin skin, UIFunctionsSWT uiFunctions) {
		this.skin = skin;
		this.uiFunctions = uiFunctions;

		hookListeners();

	}

	private void hookListeners() {

		/*
		 * Listens to log in 
		 */
		SWTSkinObject skinObject = skin.getSkinObject("text-log-in");
		if (null != skinObject) {
			SWTSkinButtonUtility btnGo = new SWTSkinButtonUtility(skinObject);
			btnGo.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {

					String url = Constants.URL_PREFIX + Constants.URL_LOGIN + "?"
							+ Constants.URL_SUFFIX;
					new LightBoxBrowserWindow(url, "Sign In", 0, 0);

				}
			});
		}

		/*
		 * Listens to log out 
		 */
		skinObject = skin.getSkinObject("text-log-out");
		if (skinObject != null) {
			SWTSkinButtonUtility btnGo = new SWTSkinButtonUtility(skinObject);
			btnGo.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					final String url = Constants.URL_PREFIX + Constants.URL_LOGOUT + "?"
							+ Constants.URL_SUFFIX;

					SWTSkinObject skinObject = skin.getSkinObject(SkinConstants.VIEWID_BROWSE_TAB);
					if (skinObject instanceof SWTSkinObjectBrowser) {
						((SWTSkinObjectBrowser) skinObject).setURL(url);
					}
				}
			});
		}

		/*
		 * Listens to "Get Started"
		 */
		skinObject = skin.getSkinObject("text-get-started");
		if (skinObject != null) {
			SWTSkinButtonUtility btnGo = new SWTSkinButtonUtility(skinObject);
			btnGo.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					if (null != uiFunctions) {
						String url = Constants.URL_PREFIX + Constants.URL_REGISTRATION
								+ "?" + Constants.URL_SUFFIX;
						new LightBoxBrowserWindow(url, "Sign Up", 0, 0);
					}

				}
			});
		}

		/*
		 * Listens to user name
		 */
		skinObject = skin.getSkinObject("text-user-name");
		if (skinObject != null) {
			SWTSkinButtonUtility btnGo = new SWTSkinButtonUtility(skinObject);
			btnGo.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					if (null != uiFunctions) {
						String url = Constants.URL_PREFIX + Constants.URL_PROFILE + "?"
								+ Constants.URL_SUFFIX;
						uiFunctions.viewURL(url, SkinConstants.VIEWID_BROWSE_TAB, 0, 0,
								true, true);
					}

				}
			});
		}

		/*
		 * Listens to "MyAccount"
		 */
		skinObject = skin.getSkinObject("text-my-account");
		if (skinObject != null) {
			SWTSkinButtonUtility btnGo = new SWTSkinButtonUtility(skinObject);
			btnGo.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					if (null != uiFunctions) {
						String url = Constants.URL_PREFIX + Constants.URL_ACCOUNT + "?"
								+ Constants.URL_SUFFIX;
						uiFunctions.viewURL(url, SkinConstants.VIEWID_BROWSE_TAB, 0, 0,
								true, true);
					}

				}
			});
		}

		LoginInfoManager.getInstance().addListener(new ILoginInfoListener() {
			public void loginUpdate(LoginInfo info) {
				synchLoginStates(info.userName, info.userID, info.isNewOrUpdated);
			}
		});

		/*
		 * Listens specifically to the browse tab for the completion of the call to logout.start
		 * The logout process will load 2 pages logout.start and then browse.start.
		 * It is only AFTER browse.start has finished loading that we can proceed to
		 * refresh any other embedded pages in the client; prematurely refreshing other pages
		 * will result in a number of problem which may include the pages not refreshing properly, or the
		 * browse.start page loading being interrupted.
		 * 
		 * NOTE: This is quite precarious since we are making a concrete assumption that the logout.start page 
		 * will always load the browse.start page.  If any of this changes we will have to modify this
		 * listener accordingly 
		 */

		skinObject = skin.getSkinObject(SkinConstants.VIEWID_BROWSE_TAB);
		if (skinObject instanceof SWTSkinObjectBrowser) {
			final Browser browser = ((SWTSkinObjectBrowser) skinObject).getBrowser();

			final ProgressListener listener = new ProgressListener() {
				boolean logoutCalled = false;

				public void completed(ProgressEvent event) {

					if (true == logoutCalled) {
						String sURL = browser.getUrl();
						if (null != sURL
								&& sURL.startsWith(Constants.URL_PREFIX
										+ Constants.URL_BIG_BROWSE)) {
							LoginInfoManager.getInstance().setUserInfo(null, null, true);
							logoutCalled = false;
						}
					}
				}

				public void changed(ProgressEvent event) {
					String sURL = browser.getUrl();
					if (false == logoutCalled) {
						if (null != sURL
								&& sURL.startsWith(Constants.URL_PREFIX + Constants.URL_LOGOUT)) {
							logoutCalled = true;
							updateLoginLabels(null, null);
						}
					}
				}
			};

			browser.addProgressListener(listener);
		}
	}

	private void synchLoginStates(String userName, String userID,
			boolean isNewOrUpdated) {
		updateLoginLabels(userName, userID);
		/*
		 * Reset browser tabs if the login state has changed
		 */
		if (true == isNewOrUpdated) {
			resetBrowserPages();
		}
	}

	private void updateLoginLabels(String userName, String userID) {

		SWTSkinObject skinObject = null;

		if (null != userName) {
			skinObject = skin.getSkinObject("user-area-logged-in");
			skinObject.setVisible(true);
			skinObject = skin.getSkinObject("user-area-logged-out");
			skinObject.setVisible(false);

			skinObject = skin.getSkinObject("text-user-name");
			if (skinObject instanceof SWTSkinObjectText) {
				((SWTSkinObjectText) skinObject).setText(userName + " ");
			}

		} else {
			skinObject = skin.getSkinObject("user-area-logged-out");
			skinObject.setVisible(true);
			skinObject = skin.getSkinObject("user-area-logged-in");
			skinObject.setVisible(false);
			skinObject = skin.getSkinObject("text-user-name");
			if (skinObject instanceof SWTSkinObjectText) {
				((SWTSkinObjectText) skinObject).setText("");
			}
		}

		Utils.execSWTThread(new Runnable() {
			public void run() {
				SWTSkinObject skinObject = skin.getSkinObject("user-area");
				if (null != skinObject) {
					Utils.relayout(skinObject.getControl());
				}
			}
		});

	}

	private void resetBrowserPages() {
		SWTSkinObject skinObject = skin.getSkinObject(SkinConstants.VIEWID_PUBLISH_TAB);
		if (skinObject instanceof SWTSkinObjectBrowser) {
			((SWTSkinObjectBrowser) skinObject).restart();
		}
	}
}
