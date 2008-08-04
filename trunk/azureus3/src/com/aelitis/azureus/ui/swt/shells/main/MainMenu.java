package com.aelitis.azureus.ui.swt.shells.main;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.config.impl.ConfigurationDefaults;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.ui.swt.mainwindow.*;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.util.Constants;

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

		addFileMenu(parent);
		addViewMenu(parent);

		/*
		 * There is no Tools menu for OSX
		 */
		if (false == Constants.isOSX) {
			addToolsMenu(parent);
		}

		/*
		 * The Torrents menu is a user-configured option
		 */
		if (true == COConfigurationManager.getBooleanParameter("show_torrents_menu")) {
			addTorrentMenu(parent);
		}

		addWindowMenu(parent);

		// ===== Debug menu (development only)====
		if (org.gudy.azureus2.core3.util.Constants.isCVSVersion()) {
			DebugMenuHelper.createDebugMenuItem(menuBar);
		}

		addHelpMenu(parent);

		/*
		 * Enabled/disable menus based on what ui mode we're in; this method call controls
		 * which menus are enabled when we're in Vuze vs. Vuze Advanced
		 */
		MenuFactory.updateEnabledStates(menuBar);
	}

	/**
	 * Creates the File menu and all its children
	 * @param parent
	 * @param notMainWindow
	 * @param isModal
	 */
	private void addFileMenu(final Shell parent) {
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

		if (true == MenuFactory.isAZ3_ADV) {
			Menu shareSubMenu = MenuFactory.createShareMenuItem(fileMenu).getMenu();
			MenuFactory.addShareFileMenuItem(shareSubMenu);
			MenuFactory.addShareFolderMenuItem(shareSubMenu);
			MenuFactory.addShareFolderContentMenuItem(shareSubMenu);
			MenuFactory.addShareFolderContentRecursiveMenuItem(shareSubMenu);
		}

		MenuFactory.addCreateMenuItem(fileMenu);

		MenuFactory.addSeparatorMenuItem(fileMenu);
		MenuFactory.addCloseWindowMenuItem(fileMenu);
		MenuFactory.setEnablementKeys(
				MenuFactory.addCloseDetailsMenuItem(fileMenu), FOR_AZ2 | FOR_AZ3_ADV);
		MenuFactory.addCloseDownloadBarsToMenu(fileMenu);

		MenuFactory.addSeparatorMenuItem(fileMenu);
		MenuFactory.createTransfersMenuItem(fileMenu);

		if (true == Constants.isOSX) {
			MenuFactory.createPluginsMenuItem(fileMenu, true);
		}

		/*
		 * No need for restart and exit on OS X
		 */
		if (false == Constants.isOSX) {
			MenuFactory.addSeparatorMenuItem(fileMenu);
			MenuFactory.addRestartMenuItem(fileMenu);
			MenuFactory.addExitMenuItem(fileMenu);
		}
	}

	/**
	 * Creates the View menu and all its children
	 * @param parent
	 * @param notMainWindow
	 */
	private void addViewMenu(final Shell parent) {
		try {
			MenuItem viewItem = MenuFactory.createViewMenuItem(menuBar);
			final Menu viewMenu = viewItem.getMenu();

			addViewToolBarsMenu(viewMenu);
			
			addViewMenuItems(viewMenu);

			MenuFactory.addSeparatorMenuItem(viewMenu);
			MenuItem advancedMenuItem = MenuFactory.createAdvancedMenuItem(viewMenu);
			Menu advancedMenu = advancedMenuItem.getMenu();

			MenuFactory.addMyTorrentsMenuItem(advancedMenu);
			MenuFactory.addMyTrackerMenuItem(advancedMenu);
			MenuFactory.addMySharesMenuItem(advancedMenu);
			MenuFactory.addConsoleMenuItem(advancedMenu);
			MenuFactory.addStatisticsMenuItem(advancedMenu);

			MenuFactory.setEnablementKeys(
					MenuFactory.addViewToolbarMenuItem(advancedMenu), FOR_AZ2
							| FOR_AZ3_ADV);

			MenuFactory.addTransferBarToMenu(advancedMenu);
			MenuFactory.addAllPeersMenuItem(advancedMenu);
			MenuFactory.addBlockedIPsMenuItem(advancedMenu);

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
  			MenuFactory.addMenuItem(viewToolBarsMenu, SWT.CHECK,
  					PREFIX_V3 + ".view.sidebar", new Listener() {
  						public void handleEvent(Event event) {
  							SideBar sidebar = (SideBar) SkinViewManager.getByClass(SideBar.class);
  							if (sidebar != null) {
  								sidebar.flipSideBarVisibility();
  							}
  						}
  					});
			}

			if (ALLOW_ACTIONBAR_HIDING) {
  			MenuFactory.addMenuItem(viewToolBarsMenu, SWT.CHECK,
  					PREFIX_V3 + ".view.actionbar", new Listener() {
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

					MenuItem actionbarMenuItem = MenuFactory.findMenuItem(viewToolBarsMenu,
							PREFIX_V3 + ".view.actionbar");
					if (actionbarMenuItem != null) {
						if (skin != null) {
							SWTSkinObject so = skin.getSkinObject(SkinConstants.VIEWID_TAB_BAR);
							if (so != null) {
								actionbarMenuItem.setSelection(so.isVisible());
							}
						}
					}
					
					if (null == MenuFactory.findMenuItem(viewToolBarsMenu, PREFIX_V3 + ".view."
							+ SkinConstants.VIEWID_PLUGINBAR)) {
						createViewMenuItem(skin, viewToolBarsMenu, PREFIX_V3 + ".view."
								+ SkinConstants.VIEWID_PLUGINBAR,
								SkinConstants.VIEWID_PLUGINBAR + ".visible",
								SkinConstants.VIEWID_PLUGINBAR, true);
					}

//					if (null == MenuFactory.findMenuItem(viewMenu, PREFIX_V3
//							+ ".view.tabbar")) {
//						createViewMenuItem(skin, viewMenu, PREFIX_V3 + ".view.tabbar",
//								"TabBar.visible", SkinConstants.VIEWID_TAB_BAR, true);
//					}

					if (null == MenuFactory.findMenuItem(viewToolBarsMenu, PREFIX_V3 + ".view."
							+ SkinConstants.VIEWID_FOOTER)) {
						createViewMenuItem(skin, viewToolBarsMenu, PREFIX_V3 + ".view."
								+ SkinConstants.VIEWID_FOOTER, "Footer.visible",
								SkinConstants.VIEWID_FOOTER, true);
					}

//					if (null == MenuFactory.findMenuItem(viewMenu, PREFIX_V3 + ".view."
//							+ SkinConstants.VIEWID_BUTTON_BAR)) {
//						createViewMenuItem(skin, viewMenu, PREFIX_V3 + ".view."
//								+ SkinConstants.VIEWID_BUTTON_BAR, "Buttonbar.visible",
//								SkinConstants.VIEWID_BUTTON_BAR, true);
//					}

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
		MenuFactory.addMenuItem(viewMenu, PREFIX_V3 + ".home", new Listener() {
			public void handleEvent(Event event) {
				SideBar sidebar = (SideBar)SkinViewManager.getByClass(SideBar.class);
				sidebar.showItemByID(SideBar.SIDEBAR_SECTION_WELCOME);
			}
		});

		MenuFactory.addMenuItem(viewMenu, PREFIX_V3 + ".browse", new Listener() {
			public void handleEvent(Event event) {
				SideBar sidebar = (SideBar)SkinViewManager.getByClass(SideBar.class);
				sidebar.showItemByID(SideBar.SIDEBAR_SECTION_BROWSE);
			}
		});

		MenuFactory.addMenuItem(viewMenu, PREFIX_V3 + ".library", new Listener() {
			public void handleEvent(Event event) {
				SideBar sidebar = (SideBar)SkinViewManager.getByClass(SideBar.class);
				sidebar.showItemByID(SideBar.SIDEBAR_SECTION_LIBRARY);
			}
		});

		MenuFactory.addMenuItem(viewMenu, PREFIX_V3 + ".publish", new Listener() {
			public void handleEvent(Event event) {
				SideBar sidebar = (SideBar)SkinViewManager.getByClass(SideBar.class);
				sidebar.showItemByID(SideBar.SIDEBAR_SECTION_PUBLISH);
			}
		});

	}

	/**
	 * Creates the Tools menu and all its children
	 * @param parent
	 * @param isModal
	 */
	private void addToolsMenu(final Shell parent) {
		MenuItem toolsItem = MenuFactory.createToolsMenuItem(menuBar);
		Menu toolsMenu = toolsItem.getMenu();

		MenuFactory.addConfigWizardMenuItem(toolsMenu);
		MenuFactory.addNatTestMenuItem(toolsMenu);
		MenuFactory.addSpeedTestMenuItem(toolsMenu);

		MenuFactory.addSeparatorMenuItem(toolsMenu);
		if (false == Constants.isOSX) {
			MenuFactory.createPluginsMenuItem(toolsMenu, true);
			MenuFactory.addOptionsMenuItem(toolsMenu);
		}

	}

	/**
	 * Creates the Help menu and all its children
	 * @param parent
	 * @param isModal
	 */
	private void addHelpMenu(final Shell parent) {
		MenuItem helpItem = MenuFactory.createHelpMenuItem(menuBar);
		Menu helpMenu = helpItem.getMenu();

		if (false == Constants.isOSX) {
			MenuFactory.addAboutMenuItem(helpMenu);
		}

		MenuFactory.addFAQMenuItem(helpMenu, Constants.URL_FAQ);
		MenuFactory.addReleaseNotesMenuItem(helpMenu);

		if (false == SystemProperties.isJavaWebStartInstance()) {
			MenuFactory.addSeparatorMenuItem(helpMenu);
			MenuFactory.addCheckUpdateMenuItem(helpMenu);
		}

		if (true == Constants.isOSX) {
			MenuFactory.addSeparatorMenuItem(helpMenu);
			MenuFactory.addConfigWizardMenuItem(helpMenu);
			MenuFactory.addNatTestMenuItem(helpMenu);
			MenuFactory.addSpeedTestMenuItem(helpMenu);
		}
		MenuFactory.addSeparatorMenuItem(helpMenu);
		MenuFactory.addDebugHelpMenuItem(helpMenu);

	}

	/**
	 * Creates the Window menu and all its children
	 * @param parent
	 */
	private void addWindowMenu(Shell parent) {
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
	 * @param parent
	 */
	private void addTorrentMenu(final Shell parent) {
		MenuFactory.setEnablementKeys(MenuFactory.createTorrentMenuItem(menuBar),
				FOR_AZ2 | FOR_AZ3_ADV);
	}

	public Menu getMenu(String id) {
		if (true == MENU_ID_MENU_BAR.equals(id)) {
			return menuBar;
		}
		return MenuFactory.findMenu(menuBar, id);
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
			final boolean fast) {
		MenuItem item;

		if (!ConfigurationDefaults.getInstance().doesParameterDefaultExist(configID)) {
			COConfigurationManager.setBooleanDefault(configID, true);
		}

		item = MenuFactory.addMenuItem(viewMenu, SWT.CHECK, textID, new Listener() {
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
