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

package com.aelitis.azureus.ui.swt.views.skin;

import org.eclipse.swt.browser.Browser;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.ui.skin.SkinConstants;
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

	private boolean firstLoginStateSync = true;

	public UserAreaUtils(SWTSkin skin, UIFunctionsSWT uiFunctions) {
		this.skin = skin;
		this.uiFunctions = uiFunctions;

		hookListeners();

	}

	private void hookListeners() {

		/*
		 * Opens LightBoxBrowserWindow pop-up for the Login page
		 */

		SWTSkinObject skinObject = skin.getSkinObject("text-log-in");
		if (null != skinObject) {
			SWTSkinButtonUtility btnGo = new SWTSkinButtonUtility(skinObject);
			btnGo.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {

					String url = Constants.URL_PREFIX + Constants.URL_LOGIN + "?"
							+ Constants.URL_SUFFIX;
					new LightBoxBrowserWindow(url, Constants.URL_PAGE_VERIFIER_VALUE,
							380, 280);

				}
			});
		}

		/*
		 * Opens the On Vuze tab and load the Logout page
		 */
		skinObject = skin.getSkinObject("text-log-out");
		if (skinObject != null) {
			SWTSkinButtonUtility btnGo = new SWTSkinButtonUtility(skinObject);
			btnGo.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {

					/*
					 * We log out by opening the following URL in a browser.  The page
					 * that is loaded will send a 'status:login-update' message which the 
					 * ILoginInfoListener will respond to and update the UI accordingly
					 */
					final String url = Constants.URL_PREFIX + Constants.URL_LOGOUT + "?"
							+ Constants.URL_SUFFIX;

					/*
					 * Loads the page without switching to the On Vuze tab
					 */
					SWTSkinObject skinObject = skin.getSkinObject(SkinConstants.VIEWID_BROWSER_BROWSE);
					if (skinObject instanceof SWTSkinObjectBrowser) {

						/*
						 * KN: Temporary fix for sign-in lead to sign-out when 'browse' tab have not been initialized problem
						 */
						Browser browser = ((SWTSkinObjectBrowser) skinObject).getBrowser();
						if (null != browser) {
							String existingURL = browser.getUrl();
							if (null == existingURL || existingURL.length() < 1) {
								((SWTSkinObjectBrowser) skinObject).setStartURL(Constants.URL_PREFIX
										+ Constants.URL_BIG_BROWSE + "?" + Constants.URL_SUFFIX);
							}
						}

						((SWTSkinObjectBrowser) skinObject).setURL(url);
					}
				}
			});
		}

		/*
		 * Opens LightBoxBrowserWindow pop-up for the Registration page
		 */
		skinObject = skin.getSkinObject("text-get-started");
		if (skinObject != null) {
			SWTSkinButtonUtility btnGo = new SWTSkinButtonUtility(skinObject);
			btnGo.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					if (null != uiFunctions) {
						String url = Constants.URL_PREFIX + Constants.URL_REGISTRATION
								+ "?" + Constants.URL_SUFFIX;
						new LightBoxBrowserWindow(url, Constants.URL_PAGE_VERIFIER_VALUE,
								460, 577);
					}

				}
			});
		}

		/*
		 * Opens the On Vuze tab and load the MyProfile page
		 */
		skinObject = skin.getSkinObject("text-user-name");
		if (skinObject != null) {
			SWTSkinButtonUtility btnGo = new SWTSkinButtonUtility(skinObject);
			btnGo.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					if (null != uiFunctions) {
						String url = Constants.URL_PREFIX + Constants.URL_MY_PROFILE + "?"
								+ Constants.URL_SUFFIX;
						uiFunctions.viewURL(url, SkinConstants.VIEWID_BROWSER_BROWSE, 0, 0,
								true, true);
					}

				}
			});
		}

		/*
		 * Opens the On Vuze tab and load the MyAccount page
		 */
		skinObject = skin.getSkinObject("text-my-account");
		if (skinObject != null) {
			SWTSkinButtonUtility btnGo = new SWTSkinButtonUtility(skinObject);
			btnGo.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					if (null != uiFunctions) {
						String url = Constants.URL_PREFIX + Constants.URL_ACCOUNT + "?"
								+ Constants.URL_SUFFIX;
						uiFunctions.viewURL(url, SkinConstants.VIEWID_BROWSER_BROWSE, 0, 0,
								true, true);
					}

				}
			});
		}

		/*
		 * Launch an external browser and load the FAQ page
		 */
		skinObject = skin.getSkinObject("help-button");
		if (skinObject != null) {
			SWTSkinButtonUtility btnGo = new SWTSkinButtonUtility(skinObject);
			btnGo.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					if (null != uiFunctions) {
						Utils.launch(Constants.URL_FAQ);
					}
				}
			});
		}

		/*
		 * Listens for changes in the login state and update the UI appropriately
		 */
		LoginInfoManager.getInstance().addListener(new ILoginInfoListener() {
			public void loginUpdate(LoginInfo info, boolean isNewLoginID) {
				synchLoginStates(info.userName, info.userID, isNewLoginID);
			}
		});
	}

	/**
	 * Updates the login/logout labels and also resets all embedded browsers
	 * @param userName
	 * @param userID
	 * @param isNewLoginID
	 */
	private void synchLoginStates(String userName, String userID,
			boolean isNewLoginID) {
		updateLoginLabels(userName, userID);
		
		if (firstLoginStateSync) {
			firstLoginStateSync  = false;
			return;
		}
		
		/*
		 * Reset browser tabs if the login state has changed
		 */
		if (true == isNewLoginID) {
			/*
			 * If the user has logged out (user name is null) then reset all pages to their original URL's
			 */
			if (null == userName) {
				resetBrowserPage(SkinConstants.VIEWID_BROWSER_BROWSE);
				resetBrowserPage(SkinConstants.VIEWID_BROWSER_PUBLISH);
				resetBrowserPage(SkinConstants.VIEWID_BROWSER_MINI);
			} else {

				/*
				 * Otherwise just refresh the current URL so the pages can be re-loaded with fresh information
				 */
				refreshBrowserPage(SkinConstants.VIEWID_BROWSER_BROWSE);
				refreshBrowserPage(SkinConstants.VIEWID_BROWSER_PUBLISH);
				refreshBrowserPage(SkinConstants.VIEWID_BROWSER_MINI);
			}
		}
	}

	/**
	 * Updates the login/logout labels to reflect the user's login state
	 * @param userName
	 * @param userID
	 */
	private void updateLoginLabels(String userName, String userID) {

		SWTSkinObject skinObject = null;

		if (null != userName) {
			skinObject = skin.getSkinObject("user-area-logged-out");
			skinObject.setVisible(false);
			skinObject = skin.getSkinObject("user-area-logged-in");
			skinObject.setVisible(true);

			skinObject = skin.getSkinObject("text-user-name");
			if (skinObject instanceof SWTSkinObjectText) {
				((SWTSkinObjectText) skinObject).setText(userName + " ");
			}

		} else {
			skinObject = skin.getSkinObject("user-area-logged-in");
			skinObject.setVisible(false);
			skinObject = skin.getSkinObject("user-area-logged-out");
			skinObject.setVisible(true);
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

	/**
	 * Resets the embedded browser with the given viewID
	 * @param targetViewID
	 */
	private void resetBrowserPage(String targetViewID) {
		SWTSkinObject skinObject = skin.getSkinObject(targetViewID);
		if (skinObject instanceof SWTSkinObjectBrowser) {
			((SWTSkinObjectBrowser) skinObject).restart();
		}
	}

	/**
	 * Refreshes the embedded browser with the given viewID
	 * @param targetViewID
	 */
	private void refreshBrowserPage(String targetViewID) {
		final SWTSkinObject skinObject = skin.getSkinObject(targetViewID);
		if (skinObject instanceof SWTSkinObjectBrowser) {
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					((SWTSkinObjectBrowser) skinObject).getBrowser().refresh();
				}
			});

		}
	}
}
