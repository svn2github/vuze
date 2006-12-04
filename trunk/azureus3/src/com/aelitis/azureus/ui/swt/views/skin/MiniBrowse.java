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

package com.aelitis.azureus.ui.swt.views.skin;

import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.messenger.ClientMessageContext;
import com.aelitis.azureus.core.messenger.config.PlatformConfigMessenger;
import com.aelitis.azureus.ui.swt.browser.BrowserContext;
import com.aelitis.azureus.ui.swt.browser.listener.TorrentListener;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.util.Constants;

/**
 * @author TuxPaper
 * @created Sep 30, 2006
 *
 */
public class MiniBrowse extends SkinView
{
	public static boolean PULL_TABS = false;

	private SWTSkinObjectBrowser browserSkinObject;

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.swt.views.SkinView#showSupport(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	 */
	public Object showSupport(SWTSkinObject skinObject, Object params) {
		browserSkinObject = (SWTSkinObjectBrowser) skinObject;
		if (PULL_TABS) {
			PlatformConfigMessenger.getBrowseSections(
					PlatformConfigMessenger.SECTION_TYPE_MINIBROWSE, 0,
					new PlatformConfigMessenger.GetBrowseSectionsReplyListener() {

						public void replyReceived(final Map[] browseSections) {
							Utils.execSWTThread(new AERunnable() {
								public void runSupport() {
									createBrowseTabs(browserSkinObject, browseSections);
								}
							});
						}

						public void messageSent() {
						}

					});
		} else {
			createBrowseArea(browserSkinObject);
		}
		return null;
	}

	private void createBrowseArea(SWTSkinObjectBrowser browserSkinObject) {
		this.browserSkinObject = browserSkinObject;
		browserSkinObject.setURL(Constants.URL_PREFIX + Constants.URL_MINI_BROWSE
				+ "?" + Constants.URL_SUFFIX);
	}

	/**
	 * @param browseSections
	 */
	protected void createBrowseTabs(SWTSkinObject skinObject,
			final Map[] browseSections) {
		SWTSkin skin = skinObject.getSkin();
		AzureusCore core = AzureusCoreFactory.getSingleton();

		FormData formData;
		Composite cArea = (Composite) skinObject.getControl();

		final Browser browser = new Browser(cArea, SWT.NONE);
		final ClientMessageContext context = new BrowserContext("mini", browser,
				null);
		context.addMessageListener(new TorrentListener(core));

		formData = new FormData();
		formData.top = new FormAttachment(0, 0);
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		formData.bottom = new FormAttachment(100, 0);
		browser.setLayoutData(formData);

		skinObject = skin.getSkinObject("minibrowse-tabs");
		if (skinObject == null) {
			return;
		}

		Control previousControl = null;
		SWTSkinTabSet tabSet = null;
		for (int i = 0; i < browseSections.length; i++) {
			String sTabName = (String) browseSections[i].get("title");
			String sTabID = "internal.minibrowse.tab." + i;
			SWTSkinObject skinTab = skin.createTab(sTabID, "tab", skinObject);

			if (skinTab == null) {
				continue;
			}

			if (tabSet == null) {
				tabSet = skin.getTabSet(skinTab);
			}

			Control currentControl = skinTab.getControl();
			if (previousControl != null) {
				formData = (FormData) skinTab.getControl().getLayoutData();
				if (formData == null) {
					formData = new FormData();
				}
				formData.left = new FormAttachment(previousControl, 1);
				currentControl.setLayoutData(formData);
			}

			previousControl = currentControl;

			SWTSkinObject tabText = skin.getSkinObject("minibrowse-tab-text", skinTab);
			if (tabText instanceof SWTSkinObjectText) {
				((SWTSkinObjectText) tabText).setText(sTabName);
			}

			if (i == 0) {
				tabSet.addListener(new SWTSkinTabSetListener() {
					public void tabChanged(SWTSkinTabSet tabSet, String oldTabID,
							String newTabID) {
						browser.stop();
						browser.execute("document.clear(); document.write('Loading..');");

						char c = newTabID.charAt(newTabID.length() - 1);
						int i = c - '0';
						if (i >= 0 && i < browseSections.length) {
							browser.setUrl((String) browseSections[i].get("url"));
							System.out.println(browser.getUrl());
						}
					}
				});
				tabSet.setActiveTab(sTabID);
			}
		}
		cArea.getParent().layout(true, true);
	}

	public void restart() {
		browserSkinObject.restart();
	}
}
