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
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.views.tableitems.pieces.ReservedByItem;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.messenger.ClientMessageContext;
import com.aelitis.azureus.core.messenger.config.PlatformConfigMessenger;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.browser.BrowserContext;
import com.aelitis.azureus.ui.swt.browser.listener.TorrentListener;
import com.aelitis.azureus.ui.swt.browser.listener.VuzeListener;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarEntrySWT;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarListener;
import com.aelitis.azureus.util.ConstantsV3;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.menus.MenuManager;
import org.gudy.azureus2.plugins.ui.sidebar.SideBarVitalityImage;

/**
 * @author TuxPaper
 * @created Sep 30, 2006
 *
 */
public class Browse
	extends SkinView
{
	public static boolean PULL_TABS = false;

	private SWTSkinObjectBrowser browserSkinObject;

	public SWTSkinObjectBrowser getBrowserSkinObject() {
		return browserSkinObject;
	}

	private SWTSkin skin;

	private SWTSkinObject soMain;

	private SideBarVitalityImage vitalityImage;

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.swt.views.SkinView#showSupport(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	 */
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		this.soMain = skinObject;
		skin = skinObject.getSkin();
		browserSkinObject = (SWTSkinObjectBrowser) skin.getSkinObject(
				SkinConstants.VIEWID_BROWSER_BROWSE, soMain);

		SideBar sidebar = (SideBar) SkinViewManager.getByClass(SideBar.class);
		if (sidebar != null) {
			final SideBarEntrySWT entry = sidebar.getSideBarEntry(this);
			if (entry != null) {
				vitalityImage = entry.addVitalityImage("image.sidebar.vitality.dots");
				vitalityImage.setVisible(false);
				
				sidebar.addListener(new SideBarListener() {
					long lastSelect = 0;
					public void sidebarItemSelected(SideBarEntrySWT newSideBarEntry,
							SideBarEntrySWT oldSideBarEntry) {
						if (entry == newSideBarEntry) {
							if (entry == oldSideBarEntry) {
								if (lastSelect < SystemTime.getOffsetTime(-1000)) {
									if (browserSkinObject != null) {
										browserSkinObject.restart();
									}
								}
							} else {
								lastSelect = SystemTime.getCurrentTime();
							}
						}
					}
				});
			}
		}

		browserSkinObject.addListener(new BrowserContext.loadingListener() {
			public void browserLoadingChanged(boolean loading, String url) {
				if (vitalityImage != null) {
					vitalityImage.setVisible(loading);
				}
			}
		});

		if (PULL_TABS) {
			PlatformConfigMessenger.getBrowseSections(
					PlatformConfigMessenger.SECTION_TYPE_BIGBROWSE, 0,
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
		
		if (org.gudy.azureus2.core3.util.Constants.isCVSVersion()) {
  		PluginManager pm = AzureusCoreFactory.getSingleton().getPluginManager();
  		PluginInterface pi = pm.getDefaultPluginInterface();
  		UIManager uim = pi.getUIManager();
  		MenuManager menuManager = uim.getMenuManager();
  		MenuItem menuItem = menuManager.addMenuItem("sidebar."
  				+ SideBar.SIDEBAR_SECTION_BROWSE, "Button.reset");
  		menuItem.addListener(new MenuItemListener() {
  			public void selected(MenuItem menu, Object target) {
  				browserSkinObject.restart();
  			}
  		});
  		
  		menuItem = menuManager.addMenuItem("sidebar."
  				+ SideBar.SIDEBAR_SECTION_BROWSE, "Tux RPC Test");
  		menuItem.addListener(new MenuItemListener() {
  			public void selected(MenuItem menu, Object target) {
  	  		browserSkinObject.setURL("c:\\test\\BrowserMessaging.html");
  			}
  		});
  		
		}

		return null;
	}
	
	/**
	 * 
	 */
	private void createBrowseArea(SWTSkinObjectBrowser browserSkinObject) {
		this.browserSkinObject = browserSkinObject;

		browserSkinObject.setURL( ConstantsV3.DEFAULT_CONTENT_NETWORK.getServiceURL( ContentNetwork.SERVICE_BIG_BROWSE ));
	}

	/**
	 * @param browseSections
	 */
	protected void createBrowseTabs(SWTSkinObject skinObject,
			final Map[] browseSections) {
		AzureusCore core = AzureusCoreFactory.getSingleton();

		FormData formData;
		Composite cArea = (Composite) skinObject.getControl();

		final Browser browser = new Browser(cArea,
				Utils.getInitialBrowserStyle(SWT.NONE));
		final ClientMessageContext context = new BrowserContext("big", browser,
				null, true);
		context.addMessageListener(new TorrentListener(core));
		context.addMessageListener(new VuzeListener());
		
		formData = new FormData();
		formData.top = new FormAttachment(0, 0);
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		formData.bottom = new FormAttachment(100, 0);
		browser.setLayoutData(formData);

		skinObject = skin.getSkinObject("browse-tabs");
		if (skinObject == null) {
			return;
		}

		Control previousControl = null;
		SWTSkinTabSet tabSet = null;
		for (int i = 0; i < browseSections.length; i++) {
			String sTabName = (String) browseSections[i].get("title");
			String sTabID = "internal.browse.tab." + i;
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

			SWTSkinObject tabText = skin.getSkinObject("browse-tab-text", skinTab);
			if (tabText instanceof SWTSkinObjectText) {
				((SWTSkinObjectText) tabText).setText(sTabName);
			}

			if (i == 0) {
				tabSet.addListener(new SWTSkinTabSetListener() {
					public void tabChanged(SWTSkinTabSet tabSet, String oldTabID,
							String newTabID) {
						System.out.println(newTabID);
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
				tabSet.setActiveTabByID(sTabID);
			}
		}
		cArea.getParent().layout(true);
	}
}
