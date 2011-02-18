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

import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.UIInputReceiver;
import org.gudy.azureus2.plugins.ui.UIInputReceiverListener;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.menus.MenuManager;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.SimpleTextEntryWindow;

import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.ui.mdi.*;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.browser.BrowserContext;
import com.aelitis.azureus.ui.swt.mdi.MultipleDocumentInterfaceSWT;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.util.ConstantsVuze;
import com.aelitis.azureus.util.ContentNetworkUtils;

/**
 * @author TuxPaper
 * @created Sep 30, 2006
 *
 */
public class Browse
	extends SkinView
	implements MdiCloseListener
{
	private SWTSkinObjectBrowser browserSkinObject;

	public SWTSkinObjectBrowser getBrowserSkinObject() {
		return browserSkinObject;
	}

	private SWTSkinObject soMain;

	private MdiEntryVitalityImage vitalityImage;

	private ContentNetwork contentNetwork;
	
	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectAdapter#skinObjectCreated(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectCreated(SWTSkinObject skinObject, Object params) {
		MultipleDocumentInterfaceSWT mdi = UIFunctionsManagerSWT.getUIFunctionsSWT().getMDISWT();
		if (mdi != null) {
			MdiEntry entry = mdi.getEntryBySkinView(this);
			if (entry != null) {
				entry.addListener(this);
			}
		}

		return super.skinObjectCreated(skinObject, params);
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.swt.views.SkinView#showSupport(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	 */
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		this.soMain = skinObject;
		Object creationParams = skinObject.getData("CreationParams");

		if (creationParams instanceof ContentNetwork) {
			contentNetwork = (ContentNetwork) creationParams;
		} else {
			contentNetwork = ConstantsVuze.getDefaultContentNetwork();
		}
		
		browserSkinObject = SWTSkinUtils.findBrowserSO(soMain);

		final MultipleDocumentInterfaceSWT mdi = UIFunctionsManagerSWT.getUIFunctionsSWT().getMDISWT();
		if (mdi != null) {
			final MdiEntry entry = mdi.getEntryBySkinView(this);
			if (entry != null) {
				vitalityImage = entry.addVitalityImage("image.sidebar.vitality.dots");
				vitalityImage.setVisible(false);

				mdi.addListener(new MdiListener() {
					long lastSelect = 0;

					public void mdiEntrySelected(MdiEntry newEntry,
							MdiEntry oldEntry) {
						if (entry == newEntry) {
							if (entry == oldEntry) {
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

		browserSkinObject.addListener(new SWTSkinObjectListener() {
		
			public Object eventOccured(SWTSkinObject skinObject, int eventType,
					Object params) {
				if (eventType == EVENT_SHOW) {
					browserSkinObject.removeListener(this);

					browserSkinObject.addListener(new BrowserContext.loadingListener() {
						public void browserLoadingChanged(boolean loading, String url) {
							if (vitalityImage != null) {
								vitalityImage.setVisible(loading);
							}
						}
					});

					browserSkinObject.getContext().setContentNetworkID(contentNetwork.getID());

					
					browserSkinObject.setStartURL(ContentNetworkUtils.getUrl(contentNetwork,
							ContentNetwork.SERVICE_BIG_BROWSE));
				}
				return null;
			}
		});

		PluginInterface pi = PluginInitializer.getDefaultInterface();
		UIManager uim = pi.getUIManager();
		MenuManager menuManager = uim.getMenuManager();

		String menuID = "sidebar."
				+ ContentNetworkUtils.getTarget(contentNetwork);
		
		MenuItem item = menuManager.addMenuItem(menuID, "Button.reload");
		item.addListener(new MenuItemListener() {
			public void selected(MenuItem menu, Object target) {
				browserSkinObject.refresh();
			}
		});
		
		
		if (org.gudy.azureus2.core3.util.Constants.isCVSVersion()) {
			MenuItem parent = menuManager.addMenuItem(menuID, "CVS Only");
			parent.setStyle(MenuItem.STYLE_MENU);
			
			
			MenuItem menuItem = menuManager.addMenuItem(parent, "Button.reset");
			menuItem.addListener(new MenuItemListener() {
				public void selected(MenuItem menu, Object target) {
					browserSkinObject.getContext().executeInBrowser("sendMessage('display','reset-url', {});");
					//browserSkinObject.restart();
				}
			});

			menuItem = menuManager.addMenuItem(parent, "Tux RPC Test");
			menuItem.addListener(new MenuItemListener() {
				public void selected(MenuItem menu, Object target) {
					browserSkinObject.setURL("c:\\test\\BrowserMessaging.html");
				}
			});

			menuItem = menuManager.addMenuItem(parent, "URL..");
			menuItem.addListener(new MenuItemListener() {
				public void selected(MenuItem menu, Object target) {
					SimpleTextEntryWindow entryWindow = new SimpleTextEntryWindow("", "!URL!");
					entryWindow.prompt(new UIInputReceiverListener() {
						public void UIInputReceiverClosed(UIInputReceiver entryWindow) {
							if (entryWindow.hasSubmittedInput()) {
								browserSkinObject.setURL(entryWindow.getSubmittedInput());
							}
						}
					});
				}
			});
		}

		return null;
	}

	public void mdiEntryClosed(MdiEntry entry, boolean userClosed) {
		contentNetwork.setPersistentProperty(ContentNetwork.PP_ACTIVE,
				Boolean.FALSE);
	}
}
