package com.aelitis.azureus.ui.swt.shells.main;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
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
import com.aelitis.azureus.ui.swt.skin.SWTSkin;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinUtils;
import com.aelitis.azureus.util.Constants;

public class MainMenu
	implements IMainMenu, IMenuConstants
{
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
		Menu fileMenu = fileItem.getMenu();

		MenuItem openMenuItem = MenuFactory.createOpenMenuItem(fileMenu);
		Menu openSubMenu = openMenuItem.getMenu();
		MenuFactory.addOpenTorrentMenuItem(openSubMenu);
		MenuFactory.addOpenTorrentForTrackingMenuItem(openSubMenu);

		MenuItem shareMenuItem = MenuFactory.createShareMenuItem(fileMenu);
		Menu shareSubMenu = shareMenuItem.getMenu();
		MenuFactory.addShareFileMenuItem(shareSubMenu);
		MenuFactory.addShareFolderMenuItem(shareSubMenu);
		MenuFactory.addShareFolderContentMenuItem(shareSubMenu);
		MenuFactory.addShareFolderContentRecursiveMenuItem(shareSubMenu);

		MenuFactory.addCreateMenuItem(fileMenu);

		MenuFactory.addSeparatorMenuItem(fileMenu);
		MenuFactory.addCloseWindowMenuItem(fileMenu);
		MenuFactory.setEnablementKeys(
				MenuFactory.addCloseDetailsMenuItem(fileMenu), FOR_AZ2 | FOR_AZ3_ADV);
		MenuFactory.addCloseDownloadBarsToMenu(fileMenu);

		MenuFactory.addSeparatorMenuItem(fileMenu);
		MenuFactory.createTransfersMenuItem(fileMenu);

		MenuFactory.addOptionsMenuItem(fileMenu);
		MenuFactory.createPluginsMenuItem(fileMenu, true);

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
			Menu viewMenu = viewItem.getMenu();

			addViewMenuItems(viewMenu);

			MenuFactory.addSeparatorMenuItem(viewMenu);
			MenuItem advancedMenuItem = MenuFactory.createAdvancedMenuItem(viewMenu);
			Menu advancedMenu = advancedMenuItem.getMenu();

			MenuFactory.addMyTorrentsMenuItem(advancedMenu);
			MenuFactory.addMyTrackerMenuItem(advancedMenu);
			MenuFactory.addMySharesMenuItem(advancedMenu);

			MenuFactory.setEnablementKeys(
					MenuFactory.addViewToolbarMenuItem(advancedMenu), FOR_AZ2
							| FOR_AZ3_ADV);

			MenuFactory.addTransferBarToMenu(advancedMenu);
			MenuFactory.addAllPeersMenuItem(advancedMenu);

			if (Constants.isOSX) {
				MenuFactory.addSeparatorMenuItem(viewMenu);
				MenuFactory.addConsoleMenuItem(viewMenu);
				MenuFactory.addStatisticsMenuItem(viewMenu);
			}

			MenuFactory.addSeparatorMenuItem(viewMenu);
			MenuFactory.addLabelMenuItem(viewMenu, MENU_ID_SEARCH_BAR).setEnabled(
					false);

			//									createViewMenuItem(skin, viewMenu, PREFIX_V3 + ".view.searchbar",
			//											"SearchBar.visible", "searchbar");

			MenuFactory.addLabelMenuItem(viewMenu, MENU_ID_TAB_BAR).setEnabled(false);
			//			createViewMenuItem(skin, viewMenu, PREFIX_V3 + ".view.tabbar",
			//					"TabBar.visible", "tabbar");

		} catch (Exception e) {
			Debug.out("Error creating View Menu", e);
		}
	}

	private void addViewMenuItems(Menu viewMenu) {
		MenuFactory.addMenuItem(viewMenu, PREFIX_V3 + ".home", new Listener() {
			public void handleEvent(Event event) {
				skin.setActiveTab(SkinConstants.TABSET_MAIN, "maintabs.home");
			}
		});

		MenuFactory.addMenuItem(viewMenu, PREFIX_V3 + ".browse", new Listener() {
			public void handleEvent(Event event) {
				skin.setActiveTab(SkinConstants.TABSET_MAIN, "maintabs.browse");
			}
		});

		MenuFactory.addMenuItem(viewMenu, PREFIX_V3 + ".library", new Listener() {
			public void handleEvent(Event event) {
				skin.setActiveTab(SkinConstants.TABSET_MAIN, "maintabs.library");
			}
		});

		MenuFactory.addMenuItem(viewMenu, PREFIX_V3 + ".publish", new Listener() {
			public void handleEvent(Event event) {
				skin.setActiveTab(SkinConstants.TABSET_MAIN, "maintabs.publish");
			}
		});

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

		MenuFactory.addFAQMenuItem(helpMenu);
		MenuFactory.addReleaseNotesMenuItem(helpMenu);

		if (false == SystemProperties.isJavaWebStartInstance()) {
			MenuFactory.addSeparatorMenuItem(helpMenu);
			MenuFactory.addCheckUpdateMenuItem(helpMenu);
		}

		MenuFactory.addSeparatorMenuItem(helpMenu);
		MenuFactory.addConfigWizardMenuItem(helpMenu);
		MenuFactory.addNatTestMenuItem(helpMenu);
		MenuFactory.addSpeedTestMenuItem(helpMenu);

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
			final String textID, final String configID, final String viewID) {
		MenuItem item;

		if (!ConfigurationDefaults.getInstance().doesParameterDefaultExist(configID)) {
			COConfigurationManager.setBooleanDefault(configID, true);
		}

		item = MenuFactory.addMenuItem(viewMenu, SWT.CHECK, textID, new Listener() {
			public void handleEvent(Event event) {
				SWTSkinObject skinObject = skin.getSkinObject(viewID);
				if (skinObject != null) {
					SWTSkinUtils.setVisibility(skin, configID, viewID,
							!skinObject.isVisible());
				}
			}
		});
		SWTSkinUtils.setVisibility(skin, configID, viewID,
				COConfigurationManager.getBooleanParameter(configID));

		final MenuItem itemViewSearchBar = item;
		final ParameterListener listener = new ParameterListener() {
			public void parameterChanged(String parameterName) {
				itemViewSearchBar.setSelection(COConfigurationManager.getBooleanParameter(parameterName));
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
