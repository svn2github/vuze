package com.aelitis.azureus.ui.swt.shells.main;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.config.impl.ConfigurationDefaults;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.IMainMenu;
import org.gudy.azureus2.ui.swt.mainwindow.IMenuConstants;
import org.gudy.azureus2.ui.swt.mainwindow.MenuFactory;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.cnetwork.ContentNetworkManager;
import com.aelitis.azureus.core.cnetwork.ContentNetworkManagerFactory;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.toolbar.ToolBarItem;
import com.aelitis.azureus.ui.swt.utils.ContentNetworkUIManagerWindow;
import com.aelitis.azureus.ui.swt.views.skin.FriendsToolbar;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager;
import com.aelitis.azureus.ui.swt.views.skin.ToolBarView;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarEntrySWT;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar.UISWTViewEventListenerSkinObject;
import com.aelitis.azureus.util.ConstantsVuze;
import com.aelitis.azureus.util.ContentNetworkUtils;

public class MainMenu
	implements IMainMenu, IMenuConstants
{
	private static final boolean ALLOW_ACTIONBAR_HIDING = false;

	private static final boolean ALLOW_SIDEBAR_HIDING = true;

	final String PREFIX_V2 = "MainWindow.menu";

	final String PREFIX_V3 = "v3.MainWindow.menu";

	private Menu menuBar;

	private final SWTSkin skin;

	private AzureusCore core;

	/**
	 * Creates the main menu on the supplied shell
	 * 
	 * @param shell
	 */
	public MainMenu(SWTSkin skin, final Shell shell) {
		this.skin = skin;

		if (null == skin) {
			throw new NullPointerException(
					"The parameter [SWTSkin skin] can not be null");
		}

		buildMenu(shell);

	}

	private void buildMenu(Shell parent) {

		if (core == null) {
			core = AzureusCoreFactory.getSingleton();
		}

		//The Main Menu
		menuBar = new Menu(parent, SWT.BAR);
		parent.setMenuBar(menuBar);

		addFileMenu();
		//addViewMenu();
		addSimpleViewMenu();

		addContentNetworksMenu();

		addCommunityMenu();

		addPublishMenu();

		addToolsMenu();

		/*
		 * The Torrents menu is a user-configured option
		 */
		if (true == COConfigurationManager.getBooleanParameter("show_torrents_menu")) {
			addTorrentMenu();
		}

		addWindowMenu();

		// ===== Debug menu (development only)====
		if (org.gudy.azureus2.core3.util.Constants.isCVSVersion()) {
			Menu menuDebug = org.gudy.azureus2.ui.swt.mainwindow.DebugMenuHelper.createDebugMenuItem(menuBar);
			DebugMenuHelper.createDebugMenuItem(menuDebug);
		}

		addV3HelpMenu();

		/*
		 * Enabled/disable menus based on what ui mode we're in; this method call controls
		 * which menus are enabled when we're in Vuze vs. Vuze Advanced
		 */
		MenuFactory.updateEnabledStates(menuBar);
	}

	/**
	 * 
	 *
	 * @since 4.0.0.3
	 */
	private void addContentNetworksMenu() {
		try {
			MenuItem contentNetworkItem = MenuFactory.createTopLevelMenuItem(menuBar,
					MENU_ID_CONTENT_NETWORKS);
			final Menu contentNetworkMenu = contentNetworkItem.getMenu();

			contentNetworkMenu.addListener(SWT.Show, new Listener() {
				public void handleEvent(Event event) {
					try {
						MenuItem[] menuItems = contentNetworkMenu.getItems();
						Utils.disposeSWTObjects(menuItems);

						buildContentNetworkMenu(contentNetworkMenu);

					} catch (Exception e) {
						Debug.out("Error creating Menu", e);
					}

				}
			});
		} catch (Exception e) {
			Debug.out("Error creating Menu", e);
		}
	}

	/**
	 * @param contentNetworkMenu
	 *
	 * @since 4.0.0.3
	 */
	protected void buildContentNetworkMenu(final Menu contentNetworkMenu) {
		ContentNetworkManager cnManager = ContentNetworkManagerFactory.getSingleton();
		if (cnManager == null) {
			return;
		}
		ContentNetwork[] contentNetworks = cnManager.getContentNetworks();
		Arrays.sort(contentNetworks, new Comparator() {
			public int compare(Object o1, Object o2) {
				String p1 = ""
						+ ((ContentNetwork) o1).getProperty(ContentNetwork.PROPERTY_ORDER);
				String p2 = ""
						+ ((ContentNetwork) o2).getProperty(ContentNetwork.PROPERTY_ORDER);

				return p1.compareTo(p2);
			}
		});
		for (int i = 0; i < contentNetworks.length; i++) {
			ContentNetwork contentNetwork = contentNetworks[i];
			addContentNetworkItem(contentNetworkMenu, contentNetwork);
		}
		
		MenuFactory.addSeparatorMenuItem(contentNetworkMenu);

		MenuFactory.addMenuItem(contentNetworkMenu, MENU_ID_CONTENT_NETWORKS
				+ ".manage", new Listener() {
			public void handleEvent(Event event) {
				new ContentNetworkUIManagerWindow();
			}			
		});

		MenuFactory.addSeparatorMenuItem(contentNetworkMenu);

		MenuFactory.addMenuItem(contentNetworkMenu, MENU_ID_CONTENT_NETWORKS
				+ ".about", new Listener() {
			public void handleEvent(Event event) {
				String url = "AboutHDNetworks.start";
				UIFunctionsManagerSWT.getUIFunctionsSWT().viewURL(url, "_blank", 0,
						0, true, false);
			}			
		});
	}

	
	/**
	 * @param contentNetworkMenu
	 * @param cn
	 *
	 * @since 4.0.0.3
	 */
	private void addContentNetworkItem(Menu contentNetworkMenu,
			ContentNetwork cn) {
		if (cn == null) {
			return;
		}

		Object prop = cn.getProperty(ContentNetwork.PROPERTY_REMOVEABLE);
		boolean removable = (prop instanceof Boolean) ? ((Boolean) prop).booleanValue()
				: false;
		
		if (removable) {
  		prop = cn.getPersistentProperty(ContentNetwork.PP_SHOW_IN_MENU);
  		boolean show = (prop instanceof Boolean) ? ((Boolean) prop).booleanValue()
  				: true;
  		
  		if (!show) {
  			return;
  		}
		}


		final MenuItem item = MenuFactory.addMenuItem(contentNetworkMenu,
				SWT.CHECK,
				null,
				new Listener() {
					public void handleEvent(Event event) {
						MenuItem item = (MenuItem) event.widget;
						ContentNetwork contentNetwork = (ContentNetwork) item.getData("ContentNetwork");
						if (contentNetwork == null) {
							return;
						}

						SideBar sideBar = (SideBar) SkinViewManager.getByClass(SideBar.class);
						if (sideBar == null) {
							return;
						}
						String sidebarID = ContentNetworkUtils.getTarget(contentNetwork);

						ContentNetworkUtils.setSourceRef(contentNetwork, "menu", false);
						sideBar.showEntryByTabID(sidebarID);
					}
				});

		item.setText(cn.getName());

		item.setData("ContentNetwork", cn);
	}

	/**
	 * 
	 *
	 * @since 3.1.1.1
	 */
	private void addPublishMenu() {
		try {
			MenuItem publishItem = MenuFactory.createPublishMenuItem(menuBar);
			final Menu publishMenu = publishItem.getMenu();

			addPublishMenuItems(publishMenu);

		} catch (Exception e) {
			Debug.out("Error creating View Menu", e);
		}
	}

	/**
	 * Creates the File menu and all its children
	 */
	private void addFileMenu() {
		MenuItem fileItem = MenuFactory.createFileMenuItem(menuBar);
		final Menu fileMenu = fileItem.getMenu();
		builFileMenu(fileMenu);

		fileMenu.addListener(SWT.Show, new Listener() {
			private boolean isAZ3_ADV = MenuFactory.isAZ3_ADV;

			public void handleEvent(Event event) {
				if (isAZ3_ADV != MenuFactory.isAZ3_ADV) {

					MenuItem[] menuItems = fileMenu.getItems();
					for (int i = 0; i < menuItems.length; i++) {
						menuItems[i].dispose();
					}

					builFileMenu(fileMenu);

					isAZ3_ADV = MenuFactory.isAZ3_ADV;
				}
			}
		});
	}

	/**
	 * Builds the File menu dynamically
	 * @param fileMenu
	 */
	private void builFileMenu(Menu fileMenu) {

		MenuItem openMenuItem = MenuFactory.createOpenMenuItem(fileMenu);
		Menu openSubMenu = openMenuItem.getMenu();
		MenuFactory.addOpenTorrentMenuItem(openSubMenu);
		MenuFactory.addOpenTorrentForTrackingMenuItem(openSubMenu);
		MenuFactory.addOpenVuzeFileMenuItem(openSubMenu);

		int userMode = COConfigurationManager.getIntParameter("User Mode");

		if ( MenuFactory.isAZ3_ADV || userMode > 0 ){
			Menu shareSubMenu = MenuFactory.createShareMenuItem(fileMenu).getMenu();
			MenuFactory.addShareFileMenuItem(shareSubMenu);
			MenuFactory.addShareFolderMenuItem(shareSubMenu);
			MenuFactory.addShareFolderContentMenuItem(shareSubMenu);
			MenuFactory.addShareFolderContentRecursiveMenuItem(shareSubMenu);
		}

		MenuFactory.addCreateMenuItem(fileMenu);

		MenuFactory.addSeparatorMenuItem(fileMenu);
		MenuFactory.addCloseWindowMenuItem(fileMenu);
		MenuFactory.addCloseDetailsMenuItem(fileMenu);
		MenuFactory.addCloseDownloadBarsToMenu(fileMenu);

		MenuFactory.addSeparatorMenuItem(fileMenu);
		MenuFactory.createTransfersMenuItem(fileMenu);

		/*
		 * No need for restart and exit on OS X since it's already handled on the application menu
		 */
		if (false == Constants.isOSX) {
			MenuFactory.addSeparatorMenuItem(fileMenu);
			MenuFactory.addRestartMenuItem(fileMenu);
			MenuFactory.addExitMenuItem(fileMenu);
		}
	}

	private void addSimpleViewMenu() {
		try {
			MenuItem viewItem = MenuFactory.createViewMenuItem(menuBar);
			final Menu viewMenu = viewItem.getMenu();

			MenuFactory.addMenuItem(viewMenu, SWT.CHECK, PREFIX_V3 + ".view.sidebar",
					new Listener() {
						public void handleEvent(Event event) {
							SideBar sidebar = (SideBar) SkinViewManager.getByClass(SideBar.class);
							if (sidebar != null) {
								sidebar.flipSideBarVisibility();
							}
						}
					});

			MenuFactory.addMenuItem(viewMenu, SWT.CHECK, PREFIX_V3
					+ ".view.toolbartext", new Listener() {
				public void handleEvent(Event event) {
					ToolBarView tb = (ToolBarView) SkinViewManager.getByClass(ToolBarView.class);
					if (tb != null) {
						tb.flipShowText();
					}
				}
			});

			if (Constants.isCVSVersion()) {
				MenuItem itemStatusBar = MenuFactory.createTopLevelMenuItem(viewMenu,
						"v3.MainWindow.menu.view.statusbar");
				itemStatusBar.setText(itemStatusBar.getText() + " (CVS Only)");
				Menu menuStatusBar = itemStatusBar.getMenu();

				final String[] statusAreaLangs = {
					"ConfigView.section.style.status.show_sr",
					"ConfigView.section.style.status.show_nat",
					"ConfigView.section.style.status.show_ddb",
					"ConfigView.section.style.status.show_ipf",
				};
				final String[] statusAreaConfig = {
					"Status Area Show SR",
					"Status Area Show NAT",
					"Status Area Show DDB",
					"Status Area Show IPF",
				};

				for (int i = 0; i < statusAreaConfig.length; i++) {
					final String configID = statusAreaConfig[i];
					String langID = statusAreaLangs[i];

					final MenuItem item = new MenuItem(menuStatusBar, SWT.CHECK);
					Messages.setLanguageText(item, langID);
					item.addListener(SWT.Selection, new Listener() {
						public void handleEvent(Event event) {
							COConfigurationManager.setParameter(configID,
									!COConfigurationManager.getBooleanParameter(configID));
						}
					});
					menuStatusBar.addListener(SWT.Show, new Listener() {
						public void handleEvent(Event event) {
							item.setSelection(COConfigurationManager.getBooleanParameter(configID));
						}
					});
				}
			}

			MenuFactory.addSeparatorMenuItem(viewMenu);

			MenuFactory.addMenuItem(viewMenu, SWT.RADIO, PREFIX_V3
					+ ".view.asSimpleList", new Listener() {
				public void handleEvent(Event event) {
					ToolBarView tb = (ToolBarView) SkinViewManager.getByClass(ToolBarView.class);
					if (tb != null) {
						ToolBarItem item = tb.getToolBarItem("modeBig");
						if (item != null) {
							item.triggerToolBarItem();
						}
					}
				}
			});
			MenuFactory.addMenuItem(viewMenu, SWT.RADIO, PREFIX_V3
					+ ".view.asAdvancedList", new Listener() {
				public void handleEvent(Event event) {
					ToolBarView tb = (ToolBarView) SkinViewManager.getByClass(ToolBarView.class);
					if (tb != null) {
						ToolBarItem item = tb.getToolBarItem("modeSmall");
						if (item != null) {
							item.triggerToolBarItem();
						}
					}
				}
			});

			viewMenu.addMenuListener(new MenuListener() {

				public void menuShown(MenuEvent e) {

					MenuItem sidebarMenuItem = MenuFactory.findMenuItem(viewMenu,
							PREFIX_V3 + ".view.sidebar");
					if (sidebarMenuItem != null) {
						SideBar sidebar = (SideBar) SkinViewManager.getByClass(SideBar.class);
						if (sidebar != null) {
							sidebarMenuItem.setSelection(sidebar.isVisible());
						}
					}

					MenuItem itemShowText = MenuFactory.findMenuItem(viewMenu, PREFIX_V3
							+ ".view.toolbartext");
					if (itemShowText != null) {
						ToolBarView tb = (ToolBarView) SkinViewManager.getByClass(ToolBarView.class);
						if (tb != null) {
							itemShowText.setSelection(tb.getShowText());
						}
					}

					MenuItem itemShowAsSimple = MenuFactory.findMenuItem(viewMenu,
							PREFIX_V3 + ".view.asSimpleList");
					if (itemShowAsSimple != null) {
						ToolBarView tb = (ToolBarView) SkinViewManager.getByClass(ToolBarView.class);
						if (tb != null) {
							ToolBarItem item = tb.getToolBarItem("modeBig");
							if (item != null && item.isEnabled()) {
								itemShowAsSimple.setEnabled(true);
								itemShowAsSimple.setSelection(!item.getSkinButton().getSkinObject().getSuffix().equals(
										""));
							} else {
								itemShowAsSimple.setEnabled(false);
								itemShowAsSimple.setSelection(false);
							}
						}
					}
					MenuItem itemShowAsAdv = MenuFactory.findMenuItem(viewMenu, PREFIX_V3
							+ ".view.asAdvancedList");
					if (itemShowAsAdv != null) {
						ToolBarView tb = (ToolBarView) SkinViewManager.getByClass(ToolBarView.class);
						if (tb != null) {
							ToolBarItem item = tb.getToolBarItem("modeSmall");
							if (item != null && item.isEnabled()) {
								itemShowAsAdv.setEnabled(true);
								itemShowAsAdv.setSelection(!item.getSkinButton().getSkinObject().getSuffix().equals(
										""));
							} else {
								itemShowAsAdv.setSelection(false);
								itemShowAsAdv.setEnabled(false);
							}
						}
					}
				}

				public void menuHidden(MenuEvent e) {
				}
			});

		} catch (Exception e) {
			Debug.out("Error creating View Menu", e);
		}
	}

	/**
	 * Creates the View menu and all its children
	 */
	private void addViewMenu() {
		try {
			MenuItem viewItem = MenuFactory.createViewMenuItem(menuBar);
			final Menu viewMenu = viewItem.getMenu();

			addViewToolBarsMenu(viewMenu);

			//addViewMenuItems(viewMenu);

		} catch (Exception e) {
			Debug.out("Error creating View Menu", e);
		}
	}

	/**
	 * 
	 *
	 * @since 3.1.1.1
	 */
	private void addViewToolBarsMenu(Menu parent) {
		try {

			MenuItem viewToolBarsItem = MenuFactory.createTopLevelMenuItem(parent,
					PREFIX_V3 + ".view.toolbars");
			final Menu viewToolBarsMenu = viewToolBarsItem.getMenu();

			if (ALLOW_SIDEBAR_HIDING) {
				MenuFactory.addMenuItem(viewToolBarsMenu, SWT.CHECK, PREFIX_V3
						+ ".view.sidebar", new Listener() {
					public void handleEvent(Event event) {
						SideBar sidebar = (SideBar) SkinViewManager.getByClass(SideBar.class);
						if (sidebar != null) {
							sidebar.flipSideBarVisibility();
						}
					}
				});
			}

			if (ALLOW_ACTIONBAR_HIDING) {
				MenuFactory.addMenuItem(viewToolBarsMenu, SWT.CHECK, PREFIX_V3
						+ ".view.actionbar", new Listener() {
					public void handleEvent(Event event) {
						if (skin != null) {
							SWTSkinObject so = skin.getSkinObject(SkinConstants.VIEWID_TAB_BAR);
							if (so != null) {
								so.setVisible(!so.isVisible());
							}
						}
					}
				});
			}

			/*
			 * NOTE: The following menu items must be created on-demand because
			 * their creation code relies on the main window being in proper size already.
			 * Adding these menus before the window is fully opened will result in improper
			 * layout of the PluginBar and TabBar
			 */
			viewToolBarsMenu.addMenuListener(new MenuListener() {

				public void menuShown(MenuEvent e) {

					MenuItem sidebarMenuItem = MenuFactory.findMenuItem(viewToolBarsMenu,
							PREFIX_V3 + ".view.sidebar");
					if (sidebarMenuItem != null) {
						SideBar sidebar = (SideBar) SkinViewManager.getByClass(SideBar.class);
						if (sidebar != null) {
							sidebarMenuItem.setSelection(sidebar.isVisible());
						}
					}

					MenuItem actionbarMenuItem = MenuFactory.findMenuItem(
							viewToolBarsMenu, PREFIX_V3 + ".view.actionbar");
					if (actionbarMenuItem != null) {
						if (skin != null) {
							SWTSkinObject so = skin.getSkinObject(SkinConstants.VIEWID_TAB_BAR);
							if (so != null) {
								actionbarMenuItem.setSelection(so.isVisible());
							}
						}
					}

					if (null == MenuFactory.findMenuItem(viewToolBarsMenu, PREFIX_V3
							+ ".view." + SkinConstants.VIEWID_PLUGINBAR)) {
						createViewMenuItem(skin, viewToolBarsMenu, PREFIX_V3 + ".view."
								+ SkinConstants.VIEWID_PLUGINBAR,
								SkinConstants.VIEWID_PLUGINBAR + ".visible",
								SkinConstants.VIEWID_PLUGINBAR, true, 0);
					}

					if (null == MenuFactory.findMenuItem(viewToolBarsMenu, PREFIX_V3
							+ ".view." + SkinConstants.VIEWID_BUDDIES_VIEWER)) {
						createViewMenuItem(skin, viewToolBarsMenu, PREFIX_V3 + ".view."
								+ SkinConstants.VIEWID_BUDDIES_VIEWER, "Friends.visible",
								SkinConstants.VIEWID_BUDDIES_VIEWER, true, -1);
					}

				}

				public void menuHidden(MenuEvent e) {
					// Do nothing
				}

			});
		} catch (Exception e) {
			Debug.out("Error creating View Menu", e);
		}
	}

	private void addViewMenuItems(Menu viewMenu) {

		MenuFactory.addMenuItem(viewMenu, PREFIX_V3 + ".browse", new Listener() {
			public void handleEvent(Event event) {
				SideBar sidebar = (SideBar) SkinViewManager.getByClass(SideBar.class);
				sidebar.showEntryByID(SideBar.SIDEBAR_SECTION_BROWSE);
			}
		});

		MenuFactory.addMenuItem(viewMenu, PREFIX_V3 + ".library", new Listener() {
			public void handleEvent(Event event) {
				SideBar sidebar = (SideBar) SkinViewManager.getByClass(SideBar.class);
				sidebar.showEntryByID(SideBar.SIDEBAR_SECTION_LIBRARY);
			}
		});

		MenuFactory.addMenuItem(viewMenu, PREFIX_V3 + ".publish", new Listener() {
			public void handleEvent(Event event) {
				SideBar sidebar = (SideBar) SkinViewManager.getByClass(SideBar.class);
				sidebar.showEntryByID(SideBar.SIDEBAR_SECTION_PUBLISH);
			}
		});

	}

	private void addPublishMenuItems(Menu publishMenu) {
		MenuFactory.addMenuItem(publishMenu, PREFIX_V3 + ".publish.new",
				new Listener() {
					public void handleEvent(Event event) {
						String sURL = ContentNetworkUtils.getUrl(
								ConstantsVuze.getDefaultContentNetwork(),
								ContentNetwork.SERVICE_PUBLISH_NEW);

						SideBar sidebar = (SideBar) SkinViewManager.getByClass(SideBar.class);
						SideBarEntrySWT entry = SideBar.getEntry(SideBar.SIDEBAR_SECTION_PUBLISH);
						if (entry.getIView() == null) {
							entry = sidebar.createEntryFromSkinRef(
									SideBar.SIDEBAR_SECTION_BROWSE,
									SideBar.SIDEBAR_SECTION_PUBLISH, "publishtab.area",
									"Publish", null, sURL, true, -1);
						} else {
							UISWTViewEventListener eventListener = entry.getEventListener();
							if (eventListener instanceof UISWTViewEventListenerSkinObject) {
								SWTSkinObject so = ((UISWTViewEventListenerSkinObject) eventListener).getSkinObject();
								if (so instanceof SWTSkinObjectBrowser) {
									((SWTSkinObjectBrowser) so).setURL(sURL);
								}
							}
						}
						sidebar.showEntryByID(SideBar.SIDEBAR_SECTION_PUBLISH);
					}
				});

		MenuFactory.addMenuItem(publishMenu, PREFIX_V3 + ".publish.mine",
				new Listener() {
					public void handleEvent(Event event) {
						String sURL = ContentNetworkUtils.getUrl(
								ConstantsVuze.getDefaultContentNetwork(),
								ContentNetwork.SERVICE_PUBLISH);

						SideBar sidebar = (SideBar) SkinViewManager.getByClass(SideBar.class);
						SideBarEntrySWT entry = SideBar.getEntry(SideBar.SIDEBAR_SECTION_PUBLISH);
						if (entry.getIView() == null) {
							entry = sidebar.createEntryFromSkinRef(
									SideBar.SIDEBAR_SECTION_BROWSE,
									SideBar.SIDEBAR_SECTION_PUBLISH, "publishtab.area",
									"Publish", null, sURL, true, -1);
						} else {
							UISWTViewEventListener eventListener = entry.getEventListener();
							if (eventListener instanceof UISWTViewEventListenerSkinObject) {
								SWTSkinObject so = ((UISWTViewEventListenerSkinObject) eventListener).getSkinObject();
								if (so instanceof SWTSkinObjectBrowser) {
									((SWTSkinObjectBrowser) so).setURL(sURL);
								}
							}
						}
						sidebar.showEntryByID(SideBar.SIDEBAR_SECTION_PUBLISH);
					}
				});

		MenuFactory.addSeparatorMenuItem(publishMenu);

		MenuFactory.addMenuItem(publishMenu, PREFIX_V3 + ".publish.about",
				new Listener() {
					public void handleEvent(Event event) {
						String sURL = ContentNetworkUtils.getUrl(
								ConstantsVuze.getDefaultContentNetwork(),
								ContentNetwork.SERVICE_PUBLISH_ABOUT);
						Utils.launch(sURL);
					}
				});

	}

	/**
	 * Creates the Tools menu and all its children
	 */
	private void addToolsMenu() {
		MenuItem toolsItem = MenuFactory.createToolsMenuItem(menuBar);
		Menu toolsMenu = toolsItem.getMenu();

		MenuFactory.addMyTrackerMenuItem(toolsMenu);
		MenuFactory.addMySharesMenuItem(toolsMenu);
		MenuFactory.addConsoleMenuItem(toolsMenu);
		MenuFactory.addStatisticsMenuItem(toolsMenu);

		MenuFactory.addTransferBarToMenu(toolsMenu);
		MenuFactory.addAllPeersMenuItem(toolsMenu);
		MenuFactory.addBlockedIPsMenuItem(toolsMenu);

		MenuFactory.addSeparatorMenuItem(toolsMenu);
		MenuFactory.createPluginsMenuItem(toolsMenu, true);

		if (false == Constants.isOSX) {
			/*
			 * Options is on the application menu on OSX
			 */
			MenuFactory.addOptionsMenuItem(toolsMenu);
		}

	}

	/**
	 * Creates the Help menu and all its children
	 */
	private void addV3HelpMenu() {
		MenuItem helpItem = MenuFactory.createHelpMenuItem(menuBar);
		Menu helpMenu = helpItem.getMenu();

		if (false == Constants.isOSX) {
			/*
			 * The 'About' menu is on the application menu on OSX
			 */
			MenuFactory.addAboutMenuItem(helpMenu);
			MenuFactory.addSeparatorMenuItem(helpMenu);
		}

		MenuFactory.addMenuItem(helpMenu, PREFIX_V3 + ".getting_started",
				new Listener() {
					public void handleEvent(Event event) {
						SideBar sidebar = (SideBar) SkinViewManager.getByClass(SideBar.class);
						sidebar.showEntryByID(SideBar.SIDEBAR_SECTION_WELCOME);
					}
				});

		MenuFactory.addHelpSupportMenuItem(
				helpMenu,
				ContentNetworkUtils.getUrl(
				ConstantsVuze.getDefaultContentNetwork(), ContentNetwork.SERVICE_SUPPORT));

		MenuFactory.addReleaseNotesMenuItem(helpMenu);

		if (false == SystemProperties.isJavaWebStartInstance()) {
			MenuFactory.addSeparatorMenuItem(helpMenu);
			MenuFactory.addCheckUpdateMenuItem(helpMenu);
		}
		
		MenuFactory.addDonationMenuItem(helpMenu);

		MenuFactory.addSeparatorMenuItem(helpMenu);
		MenuFactory.addConfigWizardMenuItem(helpMenu);
		MenuFactory.addNatTestMenuItem(helpMenu);
		MenuFactory.addSpeedTestMenuItem(helpMenu);

		MenuFactory.addSeparatorMenuItem(helpMenu);
		MenuFactory.addDebugHelpMenuItem(helpMenu);

	}

	/**
	 * Creates the Window menu and all its children
	 */
	private void addWindowMenu() {
		MenuItem menu_window = MenuFactory.createWindowMenuItem(menuBar);
		Menu windowMenu = menu_window.getMenu();

		MenuFactory.addMinimizeWindowMenuItem(windowMenu);
		MenuFactory.addZoomWindowMenuItem(windowMenu);
		MenuFactory.addSeparatorMenuItem(windowMenu);
		MenuFactory.addBringAllToFrontMenuItem(windowMenu);

		MenuFactory.addSeparatorMenuItem(windowMenu);
		MenuFactory.appendWindowMenuItems(windowMenu);
	}

	/**
	 * Creates the Torrent menu and all its children
	 */
	private void addTorrentMenu() {
		MenuFactory.setEnablementKeys(MenuFactory.createTorrentMenuItem(menuBar),
				FOR_AZ2 | FOR_AZ3_ADV);
	}

	public Menu getMenu(String id) {
		if (true == MENU_ID_MENU_BAR.equals(id)) {
			return menuBar;
		}
		return MenuFactory.findMenu(menuBar, id);
	}

	private void addCommunityMenu() {
		MenuItem item = MenuFactory.createTopLevelMenuItem(menuBar,
				MENU_ID_COMMUNITY);
		Menu communityMenu = item.getMenu();

		MenuFactory.addMenuItem(communityMenu, MENU_ID_COMMUNITY_FORUMS,
				new Listener() {
					public void handleEvent(Event e) {
						Utils.launch(ContentNetworkUtils.getUrl(
								ConstantsVuze.getDefaultContentNetwork(),
								ContentNetwork.SERVICE_FORUMS));
					}
				});

		MenuFactory.addMenuItem(communityMenu, MENU_ID_COMMUNITY_WIKI,
				new Listener() {
					public void handleEvent(Event e) {
						Utils.launch(ContentNetworkUtils.getUrl(
								ConstantsVuze.getDefaultContentNetwork(),
								ContentNetwork.SERVICE_WIKI));
					}
				});

		MenuFactory.addMenuItem(communityMenu, MENU_ID_COMMUNITY_BLOG,
				new Listener() {
					public void handleEvent(Event e) {
						Utils.launch(ContentNetworkUtils.getUrl(
								ConstantsVuze.getDefaultContentNetwork(),
								ContentNetwork.SERVICE_BLOG));
					}
				});

		MenuFactory.addMenuItem(communityMenu, MENU_ID_FAQ, new Listener() {
			public void handleEvent(Event e) {
				Utils.launch(ContentNetworkUtils.getUrl(
						ConstantsVuze.getDefaultContentNetwork(), ContentNetwork.SERVICE_FAQ));
			}
		});

		MenuFactory.addSeparatorMenuItem(communityMenu);

		MenuFactory.addMenuItem(communityMenu, MENU_ID_COMMUNITY_ADD_FRIENDS,
				new Listener() {
					public void handleEvent(Event e) {
						FriendsToolbar friendsToolbar = (FriendsToolbar) SkinViewManager.getByClass(FriendsToolbar.class);
						if (friendsToolbar != null) {
							friendsToolbar.addBuddy();
						}
					}
				});
	}

	//====================================

	/**
	 * @deprecated This method has been replaced with {@link #getMenu(String)};
	 * use {@link #getMenu(IMenuConstants.MENU_ID_MENU_BAR)} instead
	 * @return the menuBar
	 */
	public Menu getMenuBar() {
		return menuBar;
	}

	/**
	 * @param viewMenu
	 * @param string
	 * @param string2
	 */
	public static MenuItem createViewMenuItem(final SWTSkin skin, Menu viewMenu,
			final String textID, final String configID, final String viewID,
			final boolean fast, int menuIndex) {
		MenuItem item;

		if (!ConfigurationDefaults.getInstance().doesParameterDefaultExist(configID)) {
			COConfigurationManager.setBooleanDefault(configID, true);
		}

		item = MenuFactory.addMenuItem(viewMenu, SWT.CHECK, menuIndex, textID,
				new Listener() {
					public void handleEvent(Event event) {
						SWTSkinObject skinObject = skin.getSkinObject(viewID);
						if (skinObject != null) {
							boolean newVisibility = !skinObject.isVisible();

							SWTSkinUtils.setVisibility(skin, configID, viewID, newVisibility,
									true, fast);
						}
					}
				});
		SWTSkinUtils.setVisibility(skin, configID, viewID,
				COConfigurationManager.getBooleanParameter(configID), false, true);

		final MenuItem itemViewPluginBar = item;
		final ParameterListener listener = new ParameterListener() {
			public void parameterChanged(String parameterName) {
				itemViewPluginBar.setSelection(COConfigurationManager.getBooleanParameter(parameterName));
			}
		};

		COConfigurationManager.addAndFireParameterListener(configID, listener);
		item.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				COConfigurationManager.removeParameterListener(configID, listener);
			}
		});

		return item;
	}

	// backward compat..
	public static void setVisibility(SWTSkin skin, String configID,
			String viewID, boolean visible) {
		SWTSkinUtils.setVisibility(skin, configID, viewID, visible, true, false);
	}

	// backward compat..
	public static void setVisibility(SWTSkin skin, String configID,
			String viewID, boolean visible, boolean save) {
		SWTSkinUtils.setVisibility(skin, configID, viewID, visible, save, false);
	}

}
