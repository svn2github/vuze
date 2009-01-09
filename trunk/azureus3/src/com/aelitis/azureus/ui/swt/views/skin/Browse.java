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

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.messenger.config.PlatformConfigMessenger;
import com.aelitis.azureus.ui.common.RememberedDecisionsManager;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.browser.BrowserContext;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.views.skin.SkinnedDialog.SkinnedDialogClosedListener;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.*;
import com.aelitis.azureus.util.ConstantsV3;
import com.aelitis.azureus.util.ContentNetworkUtils;

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
	implements SideBarCloseListener
{
	protected static final String CFG_SHOWCLOSE = "contentnetwork.close.reminder";

	private SWTSkinObjectBrowser browserSkinObject;

	public SWTSkinObjectBrowser getBrowserSkinObject() {
		return browserSkinObject;
	}

	private SWTSkin skin;

	private SWTSkinObject soMain;

	private SideBarVitalityImage vitalityImage;

	private ContentNetwork contentNetwork;

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectAdapter#skinObjectCreated(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectCreated(SWTSkinObject skinObject, Object params) {
		final SideBar sidebar = (SideBar) SkinViewManager.getByClass(SideBar.class);
		if (sidebar != null) {
			final SideBarEntrySWT entry = sidebar.getEntryBySkinView(this);
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
		skin = skinObject.getSkin();
		Object creationParams = skinObject.getData("CreationParams");

		if (creationParams instanceof ContentNetwork) {
			contentNetwork = (ContentNetwork) creationParams;
		} else {
			contentNetwork = ConstantsV3.DEFAULT_CONTENT_NETWORK;
		}

		// Vuze network login happens in Initializer.  The rest can be initialized
		// when browser area is created (here)
		if (contentNetwork.getID() != ContentNetwork.CONTENT_NETWORK_VUZE) {
			PlatformConfigMessenger.login(contentNetwork.getID(), 0);
		}

		browserSkinObject = SWTSkinUtils.findBrowserSO(soMain);

		final SideBar sidebar = (SideBar) SkinViewManager.getByClass(SideBar.class);
		if (sidebar != null) {
			final SideBarEntrySWT entry = sidebar.getEntryBySkinView(this);
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

		browserSkinObject.getContext().setContentNetwork(contentNetwork);

		createBrowseArea(browserSkinObject);

		if (org.gudy.azureus2.core3.util.Constants.isCVSVersion()) {
			PluginManager pm = AzureusCoreFactory.getSingleton().getPluginManager();
			PluginInterface pi = pm.getDefaultPluginInterface();
			UIManager uim = pi.getUIManager();
			MenuManager menuManager = uim.getMenuManager();

			String menuID = "sidebar."
					+ ContentNetworkUtils.getTarget(contentNetwork);

			MenuItem menuItem = menuManager.addMenuItem(menuID, "Button.reset");
			menuItem.addListener(new MenuItemListener() {
				public void selected(MenuItem menu, Object target) {
					browserSkinObject.restart();
				}
			});

			menuItem = menuManager.addMenuItem(menuID, "Tux RPC Test");
			menuItem.addListener(new MenuItemListener() {
				public void selected(MenuItem menu, Object target) {
					browserSkinObject.setURL("c:\\test\\BrowserMessaging.html");
				}
			});

			if (contentNetwork != ConstantsV3.DEFAULT_CONTENT_NETWORK) {
				menuItem = menuManager.addMenuItem(menuID, "Remove HD Network");
				menuItem.addListener(new MenuItemListener() {
					public void selected(MenuItem menu, Object target) {
						if (sidebar != null) {
							final SideBarEntrySWT entry = sidebar.getEntryBySkinView(Browse.this);
							if (entry != null) {
								entry.removeListener(Browse.this);
							}
							sidebar.closeEntry(ContentNetworkUtils.getTarget(contentNetwork));
						}
						contentNetwork.remove();
					}
				});

				menuItem = menuManager.addMenuItem(menuID, "Reset IP Flag && Close");
				menuItem.addListener(new MenuItemListener() {
					public void selected(MenuItem menu, Object target) {
						contentNetwork.setPersistentProperty(
								ContentNetwork.PP_AUTH_PAGE_SHOWN, Boolean.FALSE);
						if (sidebar != null) {
							final SideBarEntrySWT entry = sidebar.getEntryBySkinView(Browse.this);
							if (entry != null) {
								entry.removeListener(Browse.this);
							}
							sidebar.closeEntry(ContentNetworkUtils.getTarget(contentNetwork));
						}
					}
				});
			}

		}

		return null;
	}

	private void createBrowseArea(SWTSkinObjectBrowser browserSkinObject) {
		this.browserSkinObject = browserSkinObject;

		browserSkinObject.setURL(ContentNetworkUtils.getUrl(contentNetwork,
				ContentNetwork.SERVICE_BIG_BROWSE));
	}

	public void sidebarClosed(SideBarEntrySWT entry) {
		boolean wasActive = false;
		Object prop = contentNetwork.getPersistentProperty(ContentNetwork.PP_ACTIVE);
		if (prop instanceof Boolean) {
			prop = ((Boolean) prop).booleanValue();
		}
		
		contentNetwork.setPersistentProperty(ContentNetwork.PP_ACTIVE,
				Boolean.FALSE);
		
		// send sidebar close event to webapp
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				Shell shell = null;
				
				String url = ContentNetworkUtils.getUrl(contentNetwork,
						ContentNetwork.SERVICE_SIDEBAR_CLOSE);
				if (url == null) {
					return;
				}
				
				UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
				if (uiFunctions != null) {
					shell = uiFunctions.getMainShell();
				}
				
				if (shell == null) {
					return;
				}
				final Browser browser = new Browser(shell, SWT.NONE);
				browser.setVisible(false);

				browser.addProgressListener(new ProgressListener() {
					public void completed(ProgressEvent event) {
						Utils.execSWTThreadLater(1000, new AERunnable() {
							public void runSupport() {
								browser.setUrl("about:blank");
								browser.dispose();
							}
						});
					}

					public void changed(ProgressEvent event) {
					}
				});
				
				browser.setUrl(url);
			}
		});

		if (!wasActive) {
			return;
		}

		int decision = RememberedDecisionsManager.getRememberedDecision(CFG_SHOWCLOSE);
		if (decision != 1) {
			final SkinnedDialog closeDialog = new SkinnedDialog(
					"skin3_close_notification", "close-notification.body");
			SWTSkin skin = closeDialog.getSkin();
			SWTSkinObjectButton soButton = (SWTSkinObjectButton) skin.getSkinObject("close");

			if (soButton != null) {
				soButton.addSelectionListener(new ButtonListenerAdapter() {
					public void pressed(SWTSkinButtonUtility buttonUtility,
							SWTSkinObject skinObject, int stateMask) {
						closeDialog.close();
					}
				});
			}

			closeDialog.addCloseListener(new SkinnedDialogClosedListener() {
				public void skinDialogClosed(SkinnedDialog dialog) {
					SWTSkin skin = closeDialog.getSkin();
					SWTSkinObjectCheckbox soCheck = (SWTSkinObjectCheckbox) skin.getSkinObject("noshowagain");
					if (soCheck != null && soCheck.isChecked()) {
						RememberedDecisionsManager.setRemembered(CFG_SHOWCLOSE, 1);
					}
				}
			});

			closeDialog.open();
		}
	}
}
