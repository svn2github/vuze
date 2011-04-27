package com.aelitis.azureus.ui.swt.shells.main;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.config.impl.ConfigurationDefaults;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarActivationListener;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.*;

import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.util.FeatureAvailability;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.mdi.MultipleDocumentInterface;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.swt.feature.FeatureManagerUI;
import com.aelitis.azureus.ui.swt.shells.RemotePairingWindow;
import com.aelitis.azureus.ui.swt.skin.SWTSkin;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinUtils;
import com.aelitis.azureus.ui.swt.toolbar.ToolBarItemSO;
import com.aelitis.azureus.ui.swt.views.skin.SBC_PlusFTUX;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager;
import com.aelitis.azureus.ui.swt.views.skin.ToolBarView;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.util.ConstantsVuze;
import com.aelitis.azureus.util.ContentNetworkUtils;

public class MainMenu
	implements IMainMenu, IMenuConstants
{
	final String PREFIX_V2 = "MainWindow.menu";

	final String PREFIX_V3 = "v3.MainWindow.menu";

	private Menu menuBar;

	/**
	 * Creates the main menu on the supplied shell
	 * 
	 * @param shell
	 */
	public MainMenu(SWTSkin skin, final Shell shell) {
		if (null == skin) {
			throw new NullPointerException(
					"The parameter [SWTSkin skin] can not be null");
		}

		buildMenu(shell);

	}

	private void buildMenu(Shell parent) {

		//The Main Menu
		menuBar = new Menu(parent, SWT.BAR);
		parent.setMenuBar(menuBar);

		addFileMenu();
		//addViewMenu();
		addSimpleViewMenu();

		addCommunityMenu();

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
			final Menu menuDebug = org.gudy.azureus2.ui.swt.mainwindow.DebugMenuHelper.createDebugMenuItem(menuBar);
			menuDebug.addMenuListener(new MenuListener() {
				
				public void menuShown(MenuEvent e) {
					MenuItem[] items = menuDebug.getItems();
					Utils.disposeSWTObjects(items);
					
					DebugMenuHelper.createDebugMenuItem(menuDebug);
					MenuFactory.addSeparatorMenuItem(menuDebug);
					MenuItem menuItem = new MenuItem(menuDebug, SWT.PUSH);
					menuItem.setText("Log Views");
					menuItem.setEnabled(false);
					PluginsMenuHelper.getInstance().buildPluginLogsMenu(menuDebug);
				}
				
				public void menuHidden(MenuEvent e) {
				}
			});
		}

		addV3HelpMenu();

		/*
		 * Enabled/disable menus based on what ui mode we're in; this method call controls
		 * which menus are enabled when we're in Vuze vs. Vuze Advanced
		 */
		MenuFactory.updateEnabledStates(menuBar);
	}
	
	/**
	 * Creates the File menu and all its children
	 */
	private void addFileMenu() {
		MenuItem fileItem = MenuFactory.createFileMenuItem(menuBar);
		final Menu fileMenu = fileItem.getMenu();
		builFileMenu(fileMenu);

		fileMenu.addListener(SWT.Show, new Listener() {
			public void handleEvent(Event event) {
				MenuItem[] menuItems = fileMenu.getItems();
				for (int i = 0; i < menuItems.length; i++) {
					menuItems[i].dispose();
				}

				builFileMenu(fileMenu);
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

		if ( userMode > 0 ){
			Menu shareSubMenu = MenuFactory.createShareMenuItem(fileMenu).getMenu();
			MenuFactory.addShareFileMenuItem(shareSubMenu);
			MenuFactory.addShareFolderMenuItem(shareSubMenu);
			MenuFactory.addShareFolderContentMenuItem(shareSubMenu);
			MenuFactory.addShareFolderContentRecursiveMenuItem(shareSubMenu);
		}

		MenuFactory.addCreateMenuItem(fileMenu);

		if (FeatureManagerUI.enabled) {
			MenuFactory.addSeparatorMenuItem(fileMenu);
  		MenuFactory.addMenuItem(fileMenu, "menu.plus", new Listener() {
  			public void handleEvent(Event event) {
  				SBC_PlusFTUX.setSourceRef("menu-file");

  				MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
  				mdi.showEntryByID(MultipleDocumentInterface.SIDEBAR_SECTION_PLUS);
  			}
  		});
		}

		MenuFactory.addSeparatorMenuItem(fileMenu);
		MenuFactory.addCloseWindowMenuItem(fileMenu);
		MenuFactory.addCloseDetailsMenuItem(fileMenu);
		MenuFactory.addCloseDownloadBarsToMenu(fileMenu);

		MenuFactory.addSeparatorMenuItem(fileMenu);
		MenuFactory.createTransfersMenuItem(fileMenu);

		/*
		 * No need for restart and exit on OS X since it's already handled on the application menu
		 */
		if (!Utils.isCarbon) {
			MenuFactory.addSeparatorMenuItem(fileMenu);
			MenuFactory.addRestartMenuItem(fileMenu);
		}
		if (!Constants.isOSX) {
			MenuFactory.addExitMenuItem(fileMenu);
		}
	}

	private void addSimpleViewMenu() {
		try {
			MenuItem viewItem = MenuFactory.createViewMenuItem(menuBar);
			final Menu viewMenu = viewItem.getMenu();
			
			viewMenu.addListener(SWT.Show, new Listener() {
				public void handleEvent(Event event) {
					Utils.disposeSWTObjects(viewMenu.getItems());
					buildSimpleViewMenu(viewMenu);
				}
			});
		} catch (Exception e) {
			Debug.out("Error creating View Menu", e);
		}
	}

	/**
	 * @param viewMenu
	 *
	 * @since 4.5.0.3
	 */
	protected void buildSimpleViewMenu(final Menu viewMenu) {
		try {
			
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

			if (Constants.isWindows) {
				MenuFactory.addSeparatorMenuItem(viewMenu);
			}

			boolean needsSep = false;
			boolean enabled = COConfigurationManager.getBooleanParameter("Beta Programme Enabled");
			if (enabled) {
				MenuFactory.addMenuItem(viewMenu, SWT.CHECK, PREFIX_V2 + ".view.beta",
						new Listener() {
							public void handleEvent(Event event) {
								MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
								if (mdi != null) {
									mdi.showEntryByID(MultipleDocumentInterface.SIDEBAR_SECTION_BETAPROGRAM);
								}
							}
				});
				needsSep = true;
			}

			if (Constants.isWindows && FeatureAvailability.isGamesEnabled()) {
  			MenuFactory.addMenuItem(viewMenu, PREFIX_V3 + ".games", new Listener() {
  				public void handleEvent(Event event) {
  					MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
  					mdi.showEntryByID(SideBar.SIDEBAR_SECTION_GAMES);
  				}
  			});
				needsSep = true;
			}

			if (needsSep) {
				MenuFactory.addSeparatorMenuItem(viewMenu);
			}

			MenuFactory.addMenuItem(viewMenu, SWT.RADIO, PREFIX_V3
					+ ".view.asSimpleList", new Listener() {
				public void handleEvent(Event event) {
					ToolBarView tb = (ToolBarView) SkinViewManager.getByClass(ToolBarView.class);
					if (tb != null) {
						UIToolBarItem item = tb.getToolBarItem("modeBig");
						if (item != null) {
							item.triggerToolBarItem(
									UIToolBarActivationListener.ACTIVATIONTYPE_NORMAL,
									SelectedContentManager.convertSelectedContentToObject(null));
						}
					}
				}
			});
			MenuFactory.addMenuItem(viewMenu, SWT.RADIO, PREFIX_V3
					+ ".view.asAdvancedList", new Listener() {
				public void handleEvent(Event event) {
					ToolBarView tb = (ToolBarView) SkinViewManager.getByClass(ToolBarView.class);
					if (tb != null) {
						UIToolBarItem item = tb.getToolBarItem("modeSmall");
						if (item != null) {
							item.triggerToolBarItem(
									UIToolBarActivationListener.ACTIVATIONTYPE_NORMAL,
									SelectedContentManager.convertSelectedContentToObject(null));
						}
					}
				}
			});

			viewMenu.addMenuListener(new MenuListener() {

				public void menuShown(MenuEvent e) {

					MenuItem sidebarMenuItem = MenuFactory.findMenuItem(viewMenu,
							PREFIX_V3 + ".view.sidebar");
					if (sidebarMenuItem != null) {
						MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
						if (mdi != null) {
							sidebarMenuItem.setSelection(mdi.isVisible());
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
							UIToolBarItem item = tb.getToolBarItem("modeBig");
							if (item != null && item.isEnabled()) {
								itemShowAsSimple.setEnabled(true);
								if (item instanceof ToolBarItemSO) {
									itemShowAsSimple.setSelection(((ToolBarItemSO) item).getSkinButton().getSkinObject().getSuffix().contains(
											"-down"));
								}
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
							UIToolBarItem item = tb.getToolBarItem("modeSmall");
							if (item != null && item.isEnabled()) {
								itemShowAsAdv.setEnabled(true);
								if (item instanceof ToolBarItemSO) {
									itemShowAsAdv.setSelection(((ToolBarItemSO) item).getSkinButton().getSkinObject().getSuffix().contains(
											"-down"));
								}
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
		MenuFactory.addClientStatsMenuItem(toolsMenu);
		MenuFactory.addBlockedIPsMenuItem(toolsMenu);

		MenuFactory.addSeparatorMenuItem(toolsMenu);
		MenuFactory.createPluginsMenuItem(toolsMenu, true);

		addPairingMenu(toolsMenu);
		
		MenuFactory.addOptionsMenuItem(toolsMenu);

	}

	private void addPairingMenu(Menu menu) {
		MenuFactory.addMenuItem(menu, MENU_ID_PAIRING, new Listener() {
			public void handleEvent(Event e) {
				RemotePairingWindow.open();
			}
		});
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
						MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
						if (mdi != null) {
							mdi.showEntryByID(SideBar.SIDEBAR_SECTION_WELCOME);
						}
					}
				});

		MenuFactory.addHelpSupportMenuItem(
				helpMenu,
				ContentNetworkUtils.getUrl(
				ConstantsVuze.getDefaultContentNetwork(), ContentNetwork.SERVICE_SUPPORT));
		
		MenuFactory.addHealthMenuItem(helpMenu);

		MenuFactory.addReleaseNotesMenuItem(helpMenu);

		if (false == SystemProperties.isJavaWebStartInstance()) {
			MenuFactory.addSeparatorMenuItem(helpMenu);
			MenuFactory.addCheckUpdateMenuItem(helpMenu);
			MenuFactory.addBetaMenuItem(helpMenu);
		}
		
		if (FeatureManagerUI.enabled) {
  		MenuFactory.addMenuItem(helpMenu, "menu.register", new Listener() {
  			public void handleEvent(Event event) {
  				FeatureManagerUI.openLicenceEntryWindow(false, null);
  			}
  		});
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
		MenuFactory.createTorrentMenuItem(menuBar);
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
