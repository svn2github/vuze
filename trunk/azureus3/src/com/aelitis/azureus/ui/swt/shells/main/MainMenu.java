package com.aelitis.azureus.ui.swt.shells.main;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.config.impl.ConfigurationDefaults;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.KeyBindings;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.debug.UIDebugGenerator;
import org.gudy.azureus2.ui.swt.help.AboutWindow;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;
import org.gudy.azureus2.ui.swt.update.UpdateMonitor;
import org.gudy.azureus2.ui.swt.welcome.WelcomeWindow;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.skin.SWTSkin;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.util.Constants;

import org.gudy.azureus2.plugins.update.UpdateCheckInstance;
import org.gudy.azureus2.plugins.update.UpdateCheckInstanceListener;

public class MainMenu
{
	final String PREFIX_V2 = "MainWindow.menu";

	final String PREFIX_V3 = "v3.MainWindow.menu";

	private Menu menuBar;

	private final SWTSkin skin;

	/**
	 * Creates the main menu on the supplied shell
	 * 
	 * @param shell
	 */
	public MainMenu(SWTSkin skin, final Shell shell) {
		this.skin = skin;
		buildMenu(shell);
	}

	private void buildMenu(Shell parent) {
		//The Main Menu
		menuBar = new Menu(parent, SWT.BAR);

		addFileMenu(parent);
		addViewMenu(parent);
		addHelpMenu(parent);

		parent.setMenuBar(menuBar);
	}

	public void linkMenuBar(Shell parent) {
		parent.setMenuBar(menuBar);
	}

	private void addHelpMenu(final Shell parent) {
		final Display display = parent.getDisplay();

		//The Help Menu
		MenuItem helpItem = new MenuItem(menuBar, SWT.CASCADE);
		Messages.setLanguageText(helpItem, "MainWindow.menu.help");
		final Menu helpMenu = new Menu(parent, SWT.DROP_DOWN);
		helpItem.setMenu(helpMenu);

		if (!Constants.isOSX) {
			MenuItem help_about = new MenuItem(helpMenu, SWT.NULL);
			Messages.setLanguageText(help_about, "MainWindow.menu.help.about");
			help_about.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					AboutWindow.show(display);
				}
			});
			new MenuItem(helpMenu, SWT.SEPARATOR);
		}

		MenuItem help_whatsnew = new MenuItem(helpMenu, SWT.NULL);
		Messages.setLanguageText(help_whatsnew, "MainWindow.menu.help.releasenotes");
		help_whatsnew.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				new WelcomeWindow(parent);
			}
		});

		MenuItem help_faq = new MenuItem(helpMenu, SWT.NULL);
		Messages.setLanguageText(help_faq, "MainWindow.menu.help.faq");
		help_faq.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				String faqString = com.aelitis.azureus.util.Constants.URL_FAQ;
				Utils.launch(faqString);
			}
		});

		new MenuItem(helpMenu, SWT.SEPARATOR);

		if (!SystemProperties.isJavaWebStartInstance()) {
			MenuItem help_checkupdate = new MenuItem(helpMenu, SWT.NULL);
			KeyBindings.setAccelerator(help_checkupdate,
					"MainWindow.menu.help.checkupdate");
			Messages.setLanguageText(help_checkupdate,
					"MainWindow.menu.help.checkupdate");
			help_checkupdate.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
					if (uiFunctions != null) {
						uiFunctions.bringToFront();
					}
					AzureusCore core = AzureusCoreFactory.getSingleton();
					UpdateMonitor.getSingleton(core).performCheck(true, false,
							new UpdateCheckInstanceListener() {
								public void cancelled(UpdateCheckInstance instance) {
								}

								public void complete(UpdateCheckInstance instance) {
									if (instance.getUpdates().length == 0) {
										Utils.execSWTThread(new AERunnable() {
											public void runSupport() {
												Utils.openMessageBox(parent, SWT.ICON_INFORMATION
														| SWT.OK, "window.update.noupdates",
														(String[]) null);
											}
										});
									}
								}
							});
				}
			});
		}

    new MenuItem(helpMenu,SWT.SEPARATOR);
    MenuItem help_debug = new MenuItem(helpMenu, SWT.NULL);
    Messages.setLanguageText(help_debug, "MainWindow.menu.help.debug");
    help_debug.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
      	UIDebugGenerator.generate();
      }
    });
	}

	private void addViewMenu(Shell parent) {
		MenuItem viewItem = new MenuItem(menuBar, SWT.CASCADE);
		Messages.setLanguageText(viewItem, PREFIX_V2 + ".view");
		Menu viewMenu = new Menu(parent, SWT.DROP_DOWN);
		viewItem.setMenu(viewMenu);
		addViewMenuItems(viewMenu);
	}

	private void addViewMenuItems(Menu viewMenu) {
		createMenuItem(viewMenu, PREFIX_V3 + ".home", new Listener() {
			public void handleEvent(Event event) {
				skin.setActiveTab(SkinConstants.TABSET_MAIN, "maintabs.home");
			}
		});

		createMenuItem(viewMenu, PREFIX_V3 + ".browse", new Listener() {
			public void handleEvent(Event event) {
				skin.setActiveTab(SkinConstants.TABSET_MAIN, "maintabs.browse");
			}
		});

		createMenuItem(viewMenu, PREFIX_V3 + ".library", new Listener() {
			public void handleEvent(Event event) {
				skin.setActiveTab(SkinConstants.TABSET_MAIN, "maintabs.library");
			}
		});

		createMenuItem(viewMenu, PREFIX_V3 + ".publish", new Listener() {
			public void handleEvent(Event event) {
				skin.setActiveTab(SkinConstants.TABSET_MAIN, "maintabs.publish");
			}
		});

		new MenuItem(viewMenu, SWT.SEPARATOR);

		createMenuItem(viewMenu, PREFIX_V3 + ".advanced", new Listener() {
			public void handleEvent(Event event) {
				skin.setActiveTab(SkinConstants.TABSET_MAIN, "maintabs.advanced");
			}
		});
	}

	private void addFileMenu(Shell parent) {
		//The File Menu
		MenuItem fileItem = new MenuItem(menuBar, SWT.CASCADE);
		Messages.setLanguageText(fileItem, PREFIX_V2 + ".file");
		Menu fileMenu = new Menu(parent, SWT.DROP_DOWN);
		fileItem.setMenu(fileMenu);

		createMenuItem(fileMenu, PREFIX_V2 + ".file.open", new Listener() {
			public void handleEvent(Event event) {
				TorrentOpener.openTorrentSimple();
			}
		});

		//No need for restart and exit on OS X
		if (!Constants.isOSX) {
			new MenuItem(fileMenu, SWT.SEPARATOR);

			MenuItem file_restart = new MenuItem(fileMenu, SWT.NULL);
			Messages.setLanguageText(file_restart, PREFIX_V2 + ".file.restart");

			file_restart.addListener(SWT.Selection, new Listener() {

				public void handleEvent(Event event) {
					UIFunctionsManagerSWT.getUIFunctionsSWT().dispose(true, false);
				}
			});

			final MenuItem file_exit = new MenuItem(fileMenu, SWT.NULL);
			if (!COConfigurationManager.getBooleanParameter("Enable System Tray")
					|| !COConfigurationManager.getBooleanParameter("Close To Tray")) {
				KeyBindings.setAccelerator(file_exit, PREFIX_V2 + ".file.exit");
			}
			Messages.setLanguageText(file_exit, PREFIX_V2 + ".file.exit");

			file_exit.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					UIFunctionsManagerSWT.getUIFunctionsSWT().dispose(false, false);
				}
			});

			// let platform decide
			ParameterListener paramListener = new ParameterListener() {
				public void parameterChanged(String parameterName) {
					if (COConfigurationManager.getBooleanParameter("Enable System Tray")
							&& COConfigurationManager.getBooleanParameter("Close To Tray")) {
						KeyBindings.removeAccelerator(file_exit, PREFIX_V2 + ".file.exit");
					} else {
						KeyBindings.setAccelerator(file_exit, PREFIX_V2 + ".file.exit");
					}
				}
			};
			COConfigurationManager.addParameterListener("Enable System Tray",
					paramListener);
			COConfigurationManager.addParameterListener("Close To Tray",
					paramListener);
		}
	}

	private MenuItem createMenuItem(Menu parent, String key,
			Listener selectionListener) {
		return createMenuItem(parent, SWT.PUSH, key, selectionListener);
	}

	private static MenuItem createMenuItem(Menu parent, int style, String key,
			Listener selectionListener) {
		MenuItem item = new MenuItem(parent, style);
		Messages.setLanguageText(item, key);
		KeyBindings.setAccelerator(item, key);
		item.addListener(SWT.Selection, selectionListener);
		return item;
	}

	/**
	 * @param viewMenu
	 */
	public void addToOldMenuView(Menu viewMenu) {
		new MenuItem(viewMenu, SWT.SEPARATOR);

		addViewMenuItems(viewMenu);

		new MenuItem(viewMenu, SWT.SEPARATOR);

		createViewMenuItem(skin, viewMenu, PREFIX_V3 + ".view.searchbar",
				"SearchBar.visible", "searchbar");
		createViewMenuItem(skin, viewMenu, PREFIX_V3 + ".view.tabbar",
				"TabBar.visible", "tabbar");
	}

	/**
	 * @param viewMenu
	 * @param string
	 * @param string2
	 */
	public static MenuItem createViewMenuItem(final SWTSkin skin, Menu viewMenu,
			final String textID,
			final String configID,
			final String viewID) {
		MenuItem item;

		if (!ConfigurationDefaults.getInstance().doesParameterDefaultExist(configID)) {
			COConfigurationManager.setBooleanDefault(configID, true);
		}

		item = createMenuItem(viewMenu, SWT.CHECK, textID,
				new Listener() {
					public void handleEvent(Event event) {
						SWTSkinObject skinObject = skin.getSkinObject(viewID);
						if (skinObject != null) {
							setVisibility(skin, configID, viewID, !skinObject.isVisible());
						}
					}
				});
		setVisibility(skin, configID, viewID,
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

	public static void setVisibility(SWTSkin skin, String configID,
			String viewID, boolean visible) {
		SWTSkinObject skinObject = skin.getSkinObject(viewID);
		// XXX Following wont work at startup because main window is invisible..
		//		if (skinObject != null && skinObject.isVisible() != visible) {
		if (skinObject != null) {
			final Control control = skinObject.getControl();
			if (control != null && !control.isDisposed()) {
				if (control.getData("Sliding") != null) {
					return;
				}
				if (visible) {
					final FormData fd = (FormData) control.getLayoutData();
					Point size = (Point) control.getData("v3.oldHeight");
					//System.out.println("oldHeight = " + size + ";v=" + control.getVisible() + ";s=" + control.getSize());
					if (size == null && control.getSize().y < 2) {
						size = control.computeSize(SWT.DEFAULT, SWT.DEFAULT);
						if (fd.height > 0) {
							size.y = fd.height;
						}
					}

					if (size != null) {
						if (fd != null && (fd.width != size.x || fd.height != size.y)) {
							slide(control, fd, size);
						}
					}
					control.setData("v3.oldHeight", null);
				} else {
					final FormData fd = (FormData) control.getLayoutData();
					if (fd != null) {
						Point oldSize = new Point(fd.width, fd.height);
						if (oldSize.y <= 0) {
							oldSize = null;
						}
						control.setData("v3.oldHeight", oldSize);
						final Point size = new Point(0, 0);

						slide(control, fd, size);
					}
				}
				skinObject.setVisible(visible);
				Utils.relayout(control);
			}

			if (COConfigurationManager.getBooleanParameter(configID) != visible) {
				COConfigurationManager.setParameter(configID, visible);
			}
		}
	}

	private static void slide(final Control control, final FormData fd, final Point size) {
		//System.out.println("slid to " + size);
		AERunnable runnable = new AERunnable() {
			boolean firstTime = true;

			public void runSupport() {
				if (control.isDisposed()) {
					return;
				}
				if (true && false) {
					// sliding disabled until we prevent slide in being triggered while
					// sliding out (or visa-versa)
					fd.width = size.x;
					fd.height = size.y;
					control.setLayoutData(fd);
					Utils.relayout(control);
					return;
				}
				if (firstTime) {
					firstTime = false;
					if (control.getData("Sliding") != null) {
						return;
					}
					control.setData("Sliding", "1");
				}

				int newWidth = (int) (fd.width + (size.x - fd.width) * 0.4);
				int h = fd.height >= 0 ? fd.height : control.getSize().y;
				int newHeight = (int) (h + (size.y - h) * 0.4);
				//System.out.println(control + "] newh=" + newHeight + " to " + size.y);
				
				if (newWidth == fd.width && newHeight == h) {
					fd.width = size.x;
					fd.height = size.y;
					//System.out.println(control + "] side to " + size.y + " done");
					control.setLayoutData(fd);
					Utils.relayout(control);
					control.setData("Sliding", null);
				} else {
					fd.width = newWidth;
					fd.height = newHeight;
					control.setLayoutData(fd);
					Utils.relayout(control);

					final AERunnable r = this;
					SimpleTimer.addEvent("slide", SystemTime.getCurrentTime() + 10,
							new TimerEventPerformer() {
								public void perform(TimerEvent event) {
									control.getDisplay().asyncExec(r);
								}
							});
				}
			}
		};
		control.getDisplay().asyncExec(runnable);
	}

	/**
	 * @return the menuBar
	 */
	public Menu getMenuBar() {
		return menuBar;
	}
}
