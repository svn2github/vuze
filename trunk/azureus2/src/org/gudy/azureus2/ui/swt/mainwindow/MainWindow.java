/*
 * Created on Jun 25, 2003
 * Modified Apr 13, 2004 by Alon Rohter
 * Modified Apr 17, 2004 by Olivier Chalouhi (OSX system menu)
 * Copyright (C) 2003, 2004, 2005, 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 * 
 */
package org.gudy.azureus2.ui.swt.mainwindow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerAdapter;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.associations.AssociationChecker;
import org.gudy.azureus2.ui.swt.components.shell.ShellManager;
import org.gudy.azureus2.ui.swt.config.wizard.ConfigureWizard;
import org.gudy.azureus2.ui.swt.debug.ObfusticateImage;
import org.gudy.azureus2.ui.swt.debug.ObfusticateShell;
import org.gudy.azureus2.ui.swt.debug.ObfusticateTab;
import org.gudy.azureus2.ui.swt.maketorrent.NewTorrentWizard;
import org.gudy.azureus2.ui.swt.minibar.AllTransfersBar;
import org.gudy.azureus2.ui.swt.minibar.MiniBarManager;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTInstanceImpl;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewImpl;
import org.gudy.azureus2.ui.swt.sharing.progress.ProgressWindow;
import org.gudy.azureus2.ui.swt.views.*;
import org.gudy.azureus2.ui.swt.views.stats.StatsView;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewTab;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnCreator;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnManager;
import org.gudy.azureus2.ui.swt.welcome.WelcomeWindow;
import org.gudy.azureus2.ui.systray.SystemTraySWT;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreException;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.UIStatusTextClickListener;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.sharing.ShareException;
import org.gudy.azureus2.plugins.sharing.ShareManager;

/**
 * @author Olivier
 * Runnable : so that GUI initialization is done via asyncExec(this)
 * STProgressListener : To make it visible once initialization is done
 */
public class MainWindow
	extends AERunnable
	implements ParameterListener, IconBarEnabler, AEDiagnosticsEvidenceGenerator,
	ObfusticateShell, IMainWindow
{
	private static final LogIDs LOGID = LogIDs.GUI;

	private static MainWindow window;

	private Initializer initializer;

	private AzureusCore azureus_core;

	private GlobalManager globalManager;

	//NICO handle swt on macosx
	public static boolean isAlreadyDead = false;

	public static boolean isDisposeFromListener = false;

	private Display display;

	private Composite parent;

	private Shell shell;

	private IMainMenu mainMenu;

	private IconBar iconBar;

	private Composite folder;

	/** 
	 * Handles initializing and refreshing the status bar (w/help of GUIUpdater)
	 */
	private MainStatusBar mainStatusBar;

	private TrayWindow downloadBasket;

	private SystemTraySWT systemTraySWT;

	private HashMap downloadViews;

	private AEMonitor downloadViews_mon = new AEMonitor("MainWindow:dlviews");

	private Item mytorrents;
	
	private Item detailed_list;

	private Item all_peers;

	private Item my_tracker_tab;

	private Item my_shares_tab;

	private Item stats_tab;

	private Item console;

	private Item multi_options_tab;

	private Item config;

	private ConfigView config_view;

	protected AEMonitor this_mon = new AEMonitor("MainWindow");

	private UISWTInstanceImpl uiSWTInstanceImpl = null;

	private ArrayList events;

	private UIFunctionsSWT uiFunctions;

	private boolean bIconBarEnabled = false;

	private boolean bShowMainWindow;

	private boolean bSettingVisibility = false;

	private Tab mainTabSet;

	public MainWindow(AzureusCore _azureus_core, Initializer _initializer,
			ArrayList events) {
		bShowMainWindow = true;
		try {
			if (Logger.isEnabled())
				Logger.log(new LogEvent(LOGID, "MainWindow start"));

			AEDiagnostics.addEvidenceGenerator(this);

			azureus_core = _azureus_core;

			globalManager = azureus_core.getGlobalManager();

			initializer = _initializer;

			display = SWTThread.getInstance().getDisplay();

			window = this;

			this.events = events;

			display.asyncExec(this);

		} catch (AzureusCoreException e) {

			Debug.printStackTrace(e);
		}
	}

	/**
	 * runSupport() MUST BE CALLED TO FINISH INITIALIZATION
	 * @param _azureus_core
	 * @param _initializer
	 * @param shell
	 * @param parent
	 */
	public MainWindow(AzureusCore _azureus_core, Initializer _initializer,
			Shell shell, Composite parent, UISWTInstanceImpl swtinstance) {
		this.shell = shell;
		this.parent = parent;
		bShowMainWindow = true;

		try {
			if (Logger.isEnabled())
				Logger.log(new LogEvent(LOGID, "MainWindow start"));

			AEDiagnostics.addEvidenceGenerator(this);

			azureus_core = _azureus_core;

			globalManager = azureus_core.getGlobalManager();

			initializer = _initializer;

			display = SWTThread.getInstance().getDisplay();

			window = this;

			uiSWTInstanceImpl = swtinstance;

		} catch (AzureusCoreException e) {

			Debug.printStackTrace(e);
		}
	}
	
	public MainWindow() {
		bShowMainWindow = true;

		try {
			if (Logger.isEnabled())
				Logger.log(new LogEvent(LOGID, "MainWindow start"));

			AEDiagnostics.addEvidenceGenerator(this);

			azureus_core = AzureusCoreFactory.getSingleton();

			globalManager = azureus_core.getGlobalManager();

			display = SWTThread.getInstance().getDisplay();

			window = this;

		} catch (AzureusCoreException e) {

			Debug.printStackTrace(e);
		}
	}
	
	public void init(Composite parent, UISWTInstanceImpl swtInstance) {
		this.parent = parent;
		this.shell = parent.getShell();
		uiSWTInstanceImpl = swtInstance;
	}


	public void setShowMainWindow(boolean b) {
		bShowMainWindow = b;
	}

	// @see org.gudy.azureus2.core3.util.AERunnable#runSupport()
	public void runSupport() {
		FormData formData;

		try {
			uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
			if (uiFunctions == null) {
				uiFunctions = new UIFunctionsImpl(this);
				UIFunctionsManager.setUIFunctions(uiFunctions);
			} else {
				uiFunctions = new UIFunctionsImpl(this);
			}

			globalManager.loadExistingTorrentsNow(true);

			COConfigurationManager.addParameterListener("config.style.useSIUnits",
					this);

			mytorrents = null;
			my_tracker_tab = null;
			console = null;
			config = null;
			config_view = null;
			downloadViews = new HashMap();

			Control attachToTopOf = null;
			Control controlAboveFolder = null;
			Control controlBelowFolder = null;

			//The Main Window
			if (shell == null) {
				shell = new Shell(display, SWT.RESIZE | SWT.BORDER | SWT.CLOSE
						| SWT.MAX | SWT.MIN);
				shell.setData("class", this);
				shell.setText(Constants.APP_NAME); //$NON-NLS-1$
				Utils.setShellIcon(shell);

				if (parent == null) {
					parent = shell;
				}

				// register window
				ShellManager.sharedManager().addWindow(shell);

				mainMenu = new MainMenu(shell);

				FormLayout mainLayout = new FormLayout();
				mainLayout.marginHeight = 0;
				mainLayout.marginWidth = 0;
				try {
					mainLayout.spacing = 0;
				} catch (NoSuchFieldError e) { /* Pre SWT 3.0 */
				}
				shell.setLayout(mainLayout);

				Utils.linkShellMetricsToConfig(shell, "window");

				//NICO catch the dispose event from file/quit on osx
				shell.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent event) {
						if (!isAlreadyDead) {
							isDisposeFromListener = true;
							if (shell != null) {
								shell.removeDisposeListener(this);
								dispose(false, false);
							}
							isAlreadyDead = true;
						}
					}
				});

				shell.addShellListener(new ShellAdapter() {
					public void shellClosed(ShellEvent event) {
						if (bSettingVisibility) {
							return;
						}
						if (systemTraySWT != null
								&& COConfigurationManager.getBooleanParameter("Enable System Tray")
								&& COConfigurationManager.getBooleanParameter("Close To Tray")) {

							minimizeToTray(event);
						} else {
							event.doit = dispose(false, false);
						}
					}

					public void shellIconified(ShellEvent event) {
						if (bSettingVisibility) {
							return;
						}
						if (systemTraySWT != null
								&& COConfigurationManager.getBooleanParameter("Enable System Tray")
								&& COConfigurationManager.getBooleanParameter("Minimize To Tray")) {

							minimizeToTray(event);
						}
					}

					public void shellDeiconified(ShellEvent e) {
						if (Constants.isOSX
								&& COConfigurationManager.getBooleanParameter("Password enabled")) {
							shell.setVisible(false);
							if (PasswordWindow.showPasswordWindow(display)) {
								shell.setVisible(true);
							}
						}
					}
				});

				// Separator between menu and icon bar
				Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
				formData = new FormData();
				formData.top = new FormAttachment(0, 0); // 2 params for Pre SWT 3.0
				formData.left = new FormAttachment(0, 0); // 2 params for Pre SWT 3.0
				formData.right = new FormAttachment(100, 0); // 2 params for Pre SWT 3.0
				separator.setLayoutData(formData);

				attachToTopOf = separator;

				mainStatusBar = new MainStatusBar();
				Composite statusBar = mainStatusBar.initStatusBar(azureus_core,
						globalManager, display, shell);

				controlAboveFolder = attachToTopOf;
				controlBelowFolder = statusBar;

			}

			try {
				Utils.createTorrentDropTarget(parent, true);
			} catch (SWTError e) {
				// "Cannot initialize Drop".. don't spew stack trace
				Logger.log(new LogEvent(LOGID, LogEvent.LT_WARNING,
						"Drag and Drop not available: " + e.getMessage()));
			} catch (Throwable e) {
				Logger.log(new LogEvent(LOGID, "Drag and Drop not available", e));
			}

			mainTabSet = new Tab(this);
			folder = mainTabSet.createFolderWidget(parent);

			formData = new FormData();
			if (controlAboveFolder == null) {
				formData.top = new FormAttachment(0, 0);
			} else {
				formData.top = new FormAttachment(controlAboveFolder);
			}

			if (controlBelowFolder == null) {
				formData.bottom = new FormAttachment(100, 0);
			} else {
				formData.bottom = new FormAttachment(controlBelowFolder);
			}
			formData.left = new FormAttachment(0, 0); // 2 params for Pre SWT 3.0
			formData.right = new FormAttachment(100, 0); // 2 params for Pre SWT 3.0
			folder.setLayoutData(formData);

			display.addFilter(SWT.KeyDown, new Listener() {
				public void handleEvent(Event event) {
					// Another window has control, skip filter
					Control focus_control = display.getFocusControl();
					if (focus_control != null && focus_control.getShell() != shell)
						return;

					int key = event.character;
					if ((event.stateMask & SWT.MOD1) != 0 && event.character <= 26
							&& event.character > 0)
						key += 'a' - 1;

					if (key == 'l' && (event.stateMask & SWT.MOD1) != 0) {
						// Ctrl-L: Open URL
						OpenTorrentWindow.invokeURLPopup(shell, globalManager);
						event.doit = false;
					}
				}
			});

			if (Logger.isEnabled())
				Logger.log(new LogEvent(LOGID, "Initializing GUI complete"));

			globalManager.addListener(new GlobalManagerAdapter() {
				public void downloadManagerAdded(DownloadManager dm) {
					MainWindow.this.downloadManagerAdded(dm);
				}

				public void downloadManagerRemoved(DownloadManager dm) {
					MainWindow.this.downloadManagerRemoved(dm);
				}
			});

			PluginManager plugin_manager = azureus_core.getPluginManager();

			plugin_manager.firePluginEvent(PluginEvent.PEV_CONFIGURATION_WIZARD_STARTS);

			if (!COConfigurationManager.getBooleanParameter("Wizard Completed")) {
				// returns after the wizard is done
				new ConfigureWizard(getAzureusCore(), true);
			}

			plugin_manager.firePluginEvent(PluginEvent.PEV_CONFIGURATION_WIZARD_COMPLETES);

			// attach the UI to plugins
			// Must be done before initializing views, since plugins may register
			// table columns and other objects
			if (uiSWTInstanceImpl == null) {
				TableColumnCreator.initCoreColumns();

				uiSWTInstanceImpl = new UISWTInstanceImpl(azureus_core);
				uiSWTInstanceImpl.init(initializer);

				// check if any plugins shut us down
				if (isAlreadyDead) {
					return;
				}

				postPluginSetup(0, 50);
			}

		} catch (Throwable e) {
			Debug.printStackTrace(e);
		}

		showMainWindow();
	}

	/**
	 * 
	 *
	 * @since 3.0.4.3
	 */
	public void postPluginSetup(int delay, final int delayInc) {
		if (initializer != null) {
			initializer.reportCurrentTask(MessageText.getString("splash.openViews"));
			initializer.nextTask();
		}

		if (!Constants.isSafeMode && azureus_core.getTrackerHost().getTorrents().length > 0) {
			Utils.execSWTThreadLater(delay += delayInc, new Runnable() {
				public void run() {
					showMyTracker();
				}
			});
		}

		PluginManager plugin_manager = azureus_core.getPluginManager();

		// share manager init is async so we need to deal with this

		PluginInterface default_pi = plugin_manager.getDefaultPluginInterface();

		try {
			final ShareManager share_manager = default_pi.getShareManager();

			default_pi.addListener(new PluginListener() {
				public void initializationComplete() {
				}

				public void closedownInitiated() {
					int share_count = share_manager.getShares().length;

					if (share_count != COConfigurationManager.getIntParameter("GUI_SWT_share_count_at_close")) {

						COConfigurationManager.setParameter("GUI_SWT_share_count_at_close",
								share_count);
					}
				}

				public void closedownComplete() {
				}
			});

			if (share_manager.getShares().length > 0
					|| COConfigurationManager.getIntParameter("GUI_SWT_share_count_at_close") > 0) {

				Utils.execSWTThreadLater(delay += delayInc, new Runnable() {
					public void run() {
						showMyShares();
					}
				});
			}
		} catch (ShareException e) {
			Debug.out(e);
		}

		if (!Constants.isSafeMode && COConfigurationManager.getBooleanParameter("Open MyTorrents")) {
			Utils.execSWTThreadLater(delay += delayInc, new Runnable() {
				public void run() {
					showMyTorrents();
				}
			});
		}

		//  share progress window

		new ProgressWindow();

		if (!Constants.isSafeMode && COConfigurationManager.getBooleanParameter("Open Console")) {
			Utils.execSWTThreadLater(delay += delayInc, new Runnable() {
				public void run() {
					showConsole();
				}
			});
		}
		events = null;

		if (Constants.isSafeMode || COConfigurationManager.getBooleanParameter("Open Config")) {
			Utils.execSWTThreadLater(delay += delayInc, new Runnable() {
				public void run() {
					showConfig();
				}
			});
		}

		if (!Constants.isSafeMode && COConfigurationManager.getBooleanParameter("Open Stats On Start")) {
			Utils.execSWTThreadLater(delay += delayInc, new Runnable() {
				public void run() {
					showStats();
				}
			});
		}

		if (!Constants.isSafeMode && COConfigurationManager.getBooleanParameter("Open Transfer Bar On Start")) {
			Utils.execSWTThreadLater(delay += delayInc, new Runnable() {
				public void run() {
					uiFunctions.showGlobalTransferBar();
				}
			});
		}

		COConfigurationManager.addAndFireParameterListener("IconBar.enabled",
				new ParameterListener() {
					public void parameterChanged(String parameterName) {
						setIconBarEnabled(COConfigurationManager.getBooleanParameter(parameterName));
					}
				});

		// init done
		if (initializer != null)
			initializer.abortProgress();
	}

	protected boolean getIconBarEnabled() {
		return bIconBarEnabled;
	}

	protected void setIconBarEnabled(boolean enabled) {
		if (enabled == bIconBarEnabled || shell.isDisposed()) {
			return;
		}
		bIconBarEnabled = enabled;
		COConfigurationManager.setParameter("IconBar.enabled", bIconBarEnabled);
		if (bIconBarEnabled) {
			try {
				iconBar = new IconBar(parent);
				iconBar.setCurrentEnabler(this);
				Composite cIconBar = iconBar.getComposite();

				FormData folderLayoutData = (FormData) folder.getLayoutData();

				FormData formData = new FormData();
				if (folderLayoutData.top != null
						&& folderLayoutData.top.control != null) {
					formData.top = new FormAttachment(folderLayoutData.top.control);
				} else {
					formData.top = new FormAttachment(0, 0);
				}
				folderLayoutData.top = new FormAttachment(cIconBar);

				formData.left = new FormAttachment(0, 0); // 2 params for Pre SWT 3.0
				formData.right = new FormAttachment(100, 0); // 2 params for Pre SWT 3.0
				this.iconBar.setLayoutData(formData);

			} catch (Exception e) {
				Logger.log(new LogEvent(LOGID, "Creating Icon Bar", e));
			}
		} else if (iconBar != null) {
			try {
				FormData folderLayoutData = (FormData) folder.getLayoutData();
				FormData iconBarLayoutData = (FormData) iconBar.getComposite().getLayoutData();

				if (iconBarLayoutData.top != null
						&& iconBarLayoutData.top.control != null) {
					folderLayoutData.top = new FormAttachment(
							iconBarLayoutData.top.control);
				} else {
					folderLayoutData.top = new FormAttachment(0, 0);
				}

				iconBar.delete();
				iconBar = null;
			} catch (Exception e) {
				Logger.log(new LogEvent(LOGID, "Removing Icon Bar", e));
			}
		}
		shell.layout(true, true);
	}

	private void showMainWindow() {
		COConfigurationManager.addAndFireParameterListener("Show Download Basket",this);

		if (!bShowMainWindow) {
			return;
		}

		// No tray access on OSX yet
		boolean bEnableTray = COConfigurationManager.getBooleanParameter("Enable System Tray")
				&& (!Constants.isOSX || SWT.getVersion() > 3300);
		boolean bPassworded = COConfigurationManager.getBooleanParameter("Password enabled");
		boolean bStartMinimize = bEnableTray
				&& (bPassworded || COConfigurationManager.getBooleanParameter("Start Minimized"));

		if (!bStartMinimize) {
			shell.layout();
			shell.open();
			if (!Constants.isOSX) {
				shell.forceActive();
			}
		} else if (Constants.isOSX) {
			shell.setMinimized(true);
			shell.setVisible(true);
		}

		if (bEnableTray) {

			try {
				systemTraySWT = new SystemTraySWT();

			} catch (Throwable e) {

				Logger.log(new LogEvent(LOGID, LogEvent.LT_ERROR,
						"Upgrade to SWT3.0M8 or later for system tray support."));
			}

			if (bStartMinimize) {
				minimizeToTray(null);
			}
			//Only show the password if not started minimized
			//Correct bug #878227
			else {
				if (bPassworded) {
					minimizeToTray(null);
					setVisible(true, true); // invokes password
				}
			}
		}

			// do this before other checks as these are blocking dialogs to force order

		if ( initializer != null ){
			
			initializer.initializationComplete();
		}
		
		checkForWhatsNewWindow();

			// check file associations  
	
		AssociationChecker.checkAssociations();

		azureus_core.triggerLifeCycleComponentCreated(uiFunctions);
	}

	protected void showMyTracker() {
		if (my_tracker_tab == null) {
			my_tracker_tab = mainTabSet.createTabItem(new MyTrackerView(azureus_core), true);
			mainTabSet.getView(my_tracker_tab).getComposite().addDisposeListener(
					new DisposeListener() {
						public void widgetDisposed(DisposeEvent e) {
							my_tracker_tab = null;
						}
					});
		} else {
			mainTabSet.setFocus(my_tracker_tab);
			refreshIconBar();
			refreshTorrentMenu();
		}
	}

	protected void showMyShares() {
		if (my_shares_tab == null) {
			my_shares_tab = mainTabSet.createTabItem(new MySharesView(azureus_core), true);
			mainTabSet.getView(my_shares_tab).getComposite().addDisposeListener(
					new DisposeListener() {
						public void widgetDisposed(DisposeEvent e) {
							my_shares_tab = null;
						}
					});
		} else {
			mainTabSet.setFocus(my_shares_tab);
			refreshIconBar();
			refreshTorrentMenu();
		}
	}

	protected void showMyTorrents() {
		if (mytorrents == null) {
			MyTorrentsSuperView view = new MyTorrentsSuperView(azureus_core);
			mytorrents = mainTabSet.createTabItem(view, true);
			mainTabSet.getView(mytorrents).getComposite().addDisposeListener(
					new DisposeListener() {
						public void widgetDisposed(DisposeEvent e) {
							mytorrents = null;
						}
					});
		} else {
			mainTabSet.setFocus(mytorrents);
		}
		refreshIconBar();
		refreshTorrentMenu();
	}
	
	protected void showDetailedListView() {
		if (detailed_list == null) {
			DetailedListView view = new DetailedListView(azureus_core);
			detailed_list = mainTabSet.createTabItem(view, true);
			mainTabSet.getView(detailed_list).getComposite().addDisposeListener(
					new DisposeListener() {
						public void widgetDisposed(DisposeEvent e) {
							detailed_list = null;
						}
					});
		} else {
			mainTabSet.setFocus(detailed_list);
		}
		refreshIconBar();
		refreshTorrentMenu();
	}

	protected void showAllPeersView() {
		if (all_peers == null) {
			PeerSuperView view = new PeerSuperView(azureus_core.getGlobalManager());
			all_peers = mainTabSet.createTabItem(view, true);
			mainTabSet.getView(all_peers).getComposite().addDisposeListener(
					new DisposeListener() {
						public void widgetDisposed(DisposeEvent e) {
							all_peers = null;
						}
					});
		} else {
			mainTabSet.setFocus(all_peers);
		}
		refreshIconBar();
		refreshTorrentMenu();
	}

	protected void showMultiOptionsView(DownloadManager[] managers) {
		if (multi_options_tab != null) {
			multi_options_tab.dispose();
		}

		TorrentOptionsView view = new TorrentOptionsView(managers);

		multi_options_tab = mainTabSet.createTabItem(view, true);

		view.getComposite().addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				multi_options_tab = null;
			}
		});

		refreshIconBar();
		refreshTorrentMenu();
	}

	private void minimizeToTray(ShellEvent event) {
		//Added this test so that we can call this method with null parameter.
		if (event != null)
			event.doit = false;

		// XXX hack for release.. should not access param outside Utils.linkShellMetrics
		COConfigurationManager.setParameter("window.maximized",
				shell.getMaximized());
		shell.setVisible(false);

		if (downloadBasket != null)
			downloadBasket.setVisible(true);

		MiniBarManager.getManager().setAllVisible(true);
	}

	private void updateComponents() {
		if (mainStatusBar != null)
			mainStatusBar.refreshStatusText();

		if (mainTabSet != null) {
			mainTabSet.update();
		}
	}

	private void downloadManagerAdded(DownloadManager created) {
	}

	protected void openManagerView(DownloadManager downloadManager) {
		try {
			downloadViews_mon.enter();

			if (downloadViews.containsKey(downloadManager)) {
				mainTabSet.setFocus((Item) downloadViews.get(downloadManager));
				refreshIconBar();
				refreshTorrentMenu();
			} else {
				Item tab = openPluginView(null, "DMView", new ManagerView(),
						downloadManager, true, true);
//				Item tab = mainTabSet.createTabItem(new ManagerView(azureus_core,
//						downloadManager), true);
				downloadViews.put(downloadManager, tab);
			}
		} finally {

			downloadViews_mon.exit();
		}
	}

	protected void removeManagerView(DownloadManager downloadManager) {
		try {
			downloadViews_mon.enter();

			downloadViews.remove(downloadManager);
		} finally {

			downloadViews_mon.exit();
		}
	}

	private void downloadManagerRemoved(DownloadManager removed) {
		try {
			downloadViews_mon.enter();

			if (downloadViews.containsKey(removed)) {
				final Item tab = (Item) downloadViews.get(removed);
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						if (display == null || display.isDisposed())
							return;

						mainTabSet.dispose(tab);
					}
				});

			}
		} finally {

			downloadViews_mon.exit();
		}
	}

	protected Display getDisplay() {
		return this.display;
	}

	protected Shell getShell() {
		return shell;
	}

	public void setVisible(final boolean visible, final boolean tryTricks) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				bSettingVisibility = true;
				try {
					boolean currentlyVisible = shell.getVisible()
							&& !shell.getMinimized();
					if (visible && !currentlyVisible) {
						if (COConfigurationManager.getBooleanParameter("Password enabled",
								false)) {
							if (!PasswordWindow.showPasswordWindow(display)) {
								shell.setVisible(false);
								return;
							}
						}
					}

					ArrayList wasVisibleList = null;
					boolean bHideAndShow = false;
					// temp disabled
					//tryTricks && visible && Constants.isWindows && display.getActiveShell() != shell;
					if (bHideAndShow) {
						wasVisibleList = new ArrayList();
						// We don't want the window to just flash and not open, so:
						// -Minimize main shell
						// -Set all shells invisible
						try {
							shell.setMinimized(true);
							Shell[] shells = shell.getDisplay().getShells();
							for (int i = 0; i < shells.length; i++) {
								if (shells[i].isVisible()) {
									wasVisibleList.add(shells[i]);
									shells[i].setVisible(false);
								}
							}
						} catch (Exception e) {
						}
					}

					if (visible) {
						shell.setMinimized(false);
						if (!currentlyVisible
								&& COConfigurationManager.getBooleanParameter("window.maximized")) {
							shell.setMaximized(true);
						}
					} else {
						// XXX hack for release.. should not access param outside Utils.linkShellMetrics
						COConfigurationManager.setParameter("window.maximized",
								shell.getMaximized());
					}

					shell.setVisible(visible);
					if (visible) {
						if (downloadBasket != null) {
							downloadBasket.setVisible(false);
							downloadBasket.setMoving(false);
						}

						/*
						 if (trayIcon != null)
						 trayIcon.showIcon();
						 */
						shell.forceActive();

						if (bHideAndShow) {
							try {
								Shell[] shells = shell.getDisplay().getShells();
								for (int i = 0; i < shells.length; i++) {
									if (shells[i] != shell) {
										if (wasVisibleList.contains(shells[i])) {
											shells[i].setVisible(visible);
										}
										shells[i].setFocus();
									}
								}
							} catch (Exception e) {
							}
						}
					}
				} finally {
					bSettingVisibility = false;
				}

			}
		});
	}

	protected boolean isVisible() {
		return shell.isVisible();
	}

	public boolean dispose(final boolean for_restart,
			final boolean close_already_in_progress) {
		return Utils.execSWTThreadWithBool("MainWindow.dispose",
				new AERunnableBoolean() {
					public boolean runSupport() {
						return _dispose(for_restart, close_already_in_progress);
					}
				});
	}

	private boolean _dispose(boolean for_restart,
			boolean close_already_in_progress) {
		if (isAlreadyDead) {
			return true;
		}

		if (!UIExitUtilsSWT.canClose(globalManager, for_restart)) {
			return false;
		}

		if (systemTraySWT != null) {
			systemTraySWT.dispose();
		}

		/**
		 * Explicitly force the transfer bar location to be saved (if appropriate and open).
		 * 
		 * We can't rely that the normal mechanism for doing this won't fail (which it usually does)
		 * when the GUI is being disposed of.
		 */
		AllTransfersBar transfer_bar = AllTransfersBar.getBarIfOpen(AzureusCoreFactory.getSingleton().getGlobalManager());
		if (transfer_bar != null) {
			transfer_bar.forceSaveLocation();
		}

		// close all tabs
		mainTabSet.closeAllTabs();

		isAlreadyDead = true; //NICO try to never die twice...
		/*
		if (this.trayIcon != null)
		  SysTrayMenu.dispose();
		*/

		if (initializer != null) {
			initializer.stopIt(for_restart, close_already_in_progress);
		}

		//NICO swt disposes the mainWindow all by itself (thanks... ;-( ) on macosx
		if (!shell.isDisposed() && !isDisposeFromListener) {
			shell.dispose();
		}

		COConfigurationManager.removeParameterListener("config.style.useSIUnits",
				this);
		COConfigurationManager.removeParameterListener("Show Download Basket", this);

		UIExitUtilsSWT.uiShutdown();

		return true;
	}

	protected GlobalManager getGlobalManager() {
		return globalManager;
	}

	/**
	 * @return
	 */
	protected static MainWindow getWindow() {
		return window;
	}

	/**
	 * @return
	 */
	protected TrayWindow getTray() {
		return downloadBasket;
	}

	Map pluginTabs = new HashMap();

	protected Item openPluginView(String sParentID, String sViewID,
			UISWTViewEventListener l, Object dataSource, boolean bSetFocus,
			boolean useCoreDS) {

		UISWTViewImpl view = null;
		try {
			view = new UISWTViewImpl(sParentID, sViewID, l);
		} catch (Exception e) {
			Item tab = (Item) pluginTabs.get(sViewID);
			if (tab != null) {
				mainTabSet.setFocus(tab);
			}
			return tab;
		}
		view.setUseCoreDataSource(useCoreDS);
		view.dataSourceChanged(dataSource);

		Item tab = mainTabSet.createTabItem(view, bSetFocus);

		pluginTabs.put(sViewID, tab);
		return tab;
	}

	/**
	 * Close all plugin views with the specified ID
	 * 
	 * @param sViewID
	 */
	protected void closePluginViews(String sViewID) {
		if (mainTabSet != null) {
			mainTabSet.closePluginViews(sViewID);
		}
	}

	/**
	 * Get all open Plugin Views
	 * 
	 * @return open plugin views
	 */
	protected UISWTView[] getPluginViews() {
		IView[] allViews = mainTabSet.getAllViews();

		ArrayList views = new ArrayList();

		for (int i = 0; i < allViews.length; i++) {
			IView view = allViews[i];

			if (view instanceof UISWTViewImpl) {
				views.add(view);
			}
		}

		return (UISWTView[]) views.toArray(new UISWTView[0]);
	}

	protected void openPluginView(final AbstractIView view, final String name) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				Item tab = (Item) pluginTabs.get(name);
				if (tab != null) {
					mainTabSet.setFocus(tab);
				} else {
					tab = mainTabSet.createTabItem(view, true);
					pluginTabs.put(name, tab);
				}
			}
		});
	}

	protected void closePluginView(IView view) {
		Item tab = mainTabSet.getTab(view);

		if (tab != null) {

			mainTabSet.closed(tab);
		}
	}

	public void removeActivePluginView(String view_name) {
		pluginTabs.remove(view_name);
	}

	// @see org.gudy.azureus2.core3.config.ParameterListener#parameterChanged(java.lang.String)
	public void parameterChanged(String parameterName) {
		if (parameterName.equals("Show Download Basket")) {
			if (COConfigurationManager.getBooleanParameter("Show Download Basket")) {
				if (downloadBasket == null) {
					downloadBasket = new TrayWindow(this);
					downloadBasket.setVisible(true);
				}
			} else if (downloadBasket != null) {
				downloadBasket.setVisible(false);
				downloadBasket = null;
			}
		}

		if (parameterName.equals("config.style.useSIUnits")) {
			updateComponents();
		}
	}

	// @see org.gudy.azureus2.ui.swt.IconBarEnabler#isEnabled(java.lang.String)
	public boolean isEnabled(String itemKey) {
		if (itemKey.equals("open"))
			return true;
		if (itemKey.equals("new"))
			return true;
		IView currentView = getCurrentView();
		if (currentView instanceof UISWTViewImpl) {
			UISWTViewEventListener eventListener = ((UISWTViewImpl) currentView).getEventListener();
			if (eventListener instanceof IconBarEnabler) {
				return ((IconBarEnabler)eventListener).isEnabled(itemKey);
			}
		}
		if (currentView != null)
			return currentView.isEnabled(itemKey);
		return false;
	}

	// @see org.gudy.azureus2.ui.swt.IconBarEnabler#isSelected(java.lang.String)
	public boolean isSelected(String itemKey) {
		return false;
	}

	// @see org.gudy.azureus2.ui.swt.IconBarEnabler#itemActivated(java.lang.String)
	public void itemActivated(String itemKey) {
		if (itemKey.equals("open")) {
			TorrentOpener.openTorrentWindow();
			return;
		}
		if (itemKey.equals("new")) {
			new NewTorrentWizard(getAzureusCore(), display);
			return;
		}
		IView currentView = getCurrentView();
		if (currentView instanceof UISWTViewImpl) {
			UISWTViewEventListener eventListener = ((UISWTViewImpl) currentView).getEventListener();
			if (eventListener instanceof IconBarEnabler) {
				((IconBarEnabler)eventListener).itemActivated(itemKey);
				return;
			}
		}
		if (currentView != null)
			currentView.itemActivated(itemKey);
	}

	IView getCurrentView() {
		if (mainTabSet != null) {
			return mainTabSet.getCurrentView();
		}
		return null;
	}

	protected void refreshIconBar() {
		if (iconBar != null) {
			iconBar.setCurrentEnabler(this);
		}
	}

	protected void refreshTorrentMenu() {
		if (this.mainMenu == null) {
			return;
		}
		DownloadManager[] dm;
		boolean detailed_view;
		TableViewSWT tv = null;
		IView currentView = getCurrentView();

		if (currentView instanceof ManagerView) {
			dm = new DownloadManager[] {
				((ManagerView) currentView).getDownload(),
			};
			detailed_view = true;
		} else if (currentView instanceof UISWTView) {
			UISWTView current_swt_view = (UISWTView)currentView;
			Object core_object = PluginCoreUtils.convert(current_swt_view.getDataSource(), true);
			if (core_object instanceof DownloadManager) {
				dm = new DownloadManager[] {(DownloadManager)core_object};
				
				// We should be using a constant somewhere!
				detailed_view = "DMView".equals(current_swt_view.getViewID());
			}
			else {
				dm = null;
				detailed_view = false;
			}
		} else if (currentView instanceof MyTorrentsSuperView) {
			dm = ((MyTorrentsSuperView) this.getCurrentView()).getSelectedDownloads();
			detailed_view = false;
		} else {
			dm = null;
			detailed_view = false;
		}

		if (currentView instanceof TableViewTab) {
			tv = ((TableViewTab) currentView).getTableView();
		}

		/*
		 * KN: Reflectively find the Torrents menu item and update its data
		 */
		final MenuItem torrentItem = MenuFactory.findMenuItem(
				mainMenu.getMenu(IMenuConstants.MENU_ID_MENU_BAR),
				MenuFactory.MENU_ID_TORRENT);

		if (null != torrentItem) {
			final DownloadManager[] dm_final = dm;
			final TableViewSWT tv_final = tv;
			final boolean detailed_view_final = detailed_view;
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					if (null == dm_final) {
						torrentItem.setEnabled(false);
					} else {
						torrentItem.setData("downloads", dm_final);
						torrentItem.setData("TableView", tv_final);
						torrentItem.setData("is_detailed_view",
								Boolean.valueOf(detailed_view_final));
						torrentItem.setEnabled(true);
					}
				}
			}, true); // async
		}

	}

	protected void close() {
		getShell().close();
	}

	protected void closeViewOrWindow() {
		if (getCurrentView() != null)
			mainTabSet.closeCurrent();
		else
			close();
	}

	protected ConfigView showConfig() {
		if (config == null) {
			config_view = new ConfigView(azureus_core);
			config = mainTabSet.createTabItem(config_view, true);
			config_view.getComposite().addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					config = null;
					config_view = null;
				}
			});
		} else {
			mainTabSet.setFocus(config);
			refreshIconBar();
			refreshTorrentMenu();
		}
		return config_view;
	}

	protected boolean showConfig(String id) {
		boolean has_rebuilt = config_view == null;
		showConfig();
		if (config_view == null) {
			return false;
		}
		if (id == null) {
			return true;
		}
		boolean result = config_view.selectSection(id);
		if (!result && !has_rebuilt) {
			config.dispose();
			if (config_view != null) {throw new RuntimeException("something has gone wrong");}
			return showConfig(id);
		}
		return result;	
	}

	protected void showConsole() {
		if (console == null) {
			console = mainTabSet.createTabItem(new LoggerView(events), true);
			mainTabSet.getView(console).getComposite().addDisposeListener(
					new DisposeListener() {
						public void widgetDisposed(DisposeEvent e) {
							console = null;
						}
					});
		} else {
			mainTabSet.setFocus(console);
			refreshIconBar();
			refreshTorrentMenu();
		}
	}

	protected void showStats() {
		if (stats_tab == null) {
			stats_tab = mainTabSet.createTabItem(new StatsView(globalManager, azureus_core), true);
			mainTabSet.getView(stats_tab).getComposite().addDisposeListener(
					new DisposeListener() {
						public void widgetDisposed(DisposeEvent e) {
							stats_tab = null;
						}
					});
		} else {
			mainTabSet.setFocus(stats_tab);
			refreshIconBar();
			refreshTorrentMenu();
		}
	}

	protected void showStatsDHT() {
		showStats();
		if (stats_tab == null) {
			return;
		}
		IView view = mainTabSet.getView(stats_tab);
		if (view instanceof StatsView) {
			((StatsView) view).showDHT();
		}
	}

	protected void showStatsTransfers() {
		showStats();
		if (stats_tab == null) {
			return;
		}
		IView view = mainTabSet.getView(stats_tab);
		if (view instanceof StatsView) {
			((StatsView) view).showTransfers();
		}
	}

	protected void setSelectedLanguageItem() {
		try {
			this_mon.enter();

			Messages.updateLanguageForControl(shell);

			if (systemTraySWT != null) {
				systemTraySWT.updateLanguage();
			}

			if (mainStatusBar != null) {
				mainStatusBar.refreshStatusText();
			}

			if (folder != null) {
				folder.update();
			}

			if (downloadBasket != null) {
				downloadBasket.updateLanguage();
			}

			mainTabSet.updateLanguage();

			if (mainStatusBar != null) {
				mainStatusBar.updateStatusText();
			}

			if (mainMenu != null) {
				MenuFactory.updateMenuText(mainMenu.getMenu(IMenuConstants.MENU_ID_MENU_BAR));
			}
		} finally {

			this_mon.exit();
		}
	}

	/**
	 * @deprecated Use {@link #getMainMenu()} instead
	 * @return
	 */
	public MainMenu getMenu() {
		return (MainMenu) mainMenu;
	}

	/**
	 * @deprecated Use {@link #setMainMenu(IMainMenu)} instead
	 * @param menu
	 */
	public void setMenu(MainMenu menu) {
		mainMenu = menu;
	}

	public IMainMenu getMainMenu() {
		return mainMenu;
	}

	public void setMainMenu(IMainMenu menu) {
		mainMenu = menu;
	}

	protected AzureusCore getAzureusCore() {
		return (azureus_core);
	}

	// @see org.gudy.azureus2.core3.util.AEDiagnosticsEvidenceGenerator#generate(org.gudy.azureus2.core3.util.IndentWriter)
	public void generate(IndentWriter writer) {
		writer.println("SWT UI");

		try {
			writer.indent();
			
			mainTabSet.generateDiagnostics(writer);

			TableColumnManager.getInstance().generateDiagnostics(writer);
		} finally {

			writer.exdent();
		}
	}

	private void checkForWhatsNewWindow() {
		final String CONFIG_LASTSHOWN = "welcome.version.lastshown";

		// Config used to store int, such as 2500.  Now, it stores a string
		// getIntParameter will return default value if parameter is string (user
		// downgraded)
		// getStringParameter will bork if parameter isn't really a string

		try {
			String lastShown = "";
			boolean bIsStringParam = true;
			try {
				lastShown = COConfigurationManager.getStringParameter(CONFIG_LASTSHOWN,
						"");
			} catch (Exception e) {
				bIsStringParam = false;
			}

			if (lastShown.length() == 0) {
				// check if we have an old style version
				int latestDisplayed = COConfigurationManager.getIntParameter(
						CONFIG_LASTSHOWN, 0);
				if (latestDisplayed > 0) {
					bIsStringParam = false;
					String s = "" + latestDisplayed;
					for (int i = 0; i < s.length(); i++) {
						if (i != 0) {
							lastShown += ".";
						}
						lastShown += s.charAt(i);
					}
				}
			}

			if (Constants.compareVersions(lastShown, Constants.getBaseVersion()) < 0) {
				new WelcomeWindow(shell);
				if (!bIsStringParam) {
					// setting parameter to a different value type makes az unhappy
					COConfigurationManager.removeParameter(CONFIG_LASTSHOWN);
				}
				COConfigurationManager.setParameter(CONFIG_LASTSHOWN,
						Constants.getBaseVersion());
				COConfigurationManager.save();
			}
		} catch (Exception e) {
			Debug.out(e);
		}
	}

	protected UISWTInstanceImpl getUISWTInstanceImpl() {
		return uiSWTInstanceImpl;
	}

	/**
	 * @param string
	 */
	protected void setStatusText(String string) {
		// TODO Auto-generated method stub
		if (mainStatusBar != null)
			mainStatusBar.setStatusText(string);
	}

	/**
	 * @param statustype
	 * @param string
	 * @param l
	 */
	protected void setStatusText(int statustype, String string,
			UIStatusTextClickListener l) {
		if (mainStatusBar != null) {
			mainStatusBar.setStatusText(statustype, string, l);
		}
	}

	protected SystemTraySWT getSystemTraySWT() {
		return systemTraySWT;
	}

	protected MainStatusBar getMainStatusBar() {
		return mainStatusBar;
	}

	// @see org.gudy.azureus2.ui.swt.debug.ObfusticateShell#generateObfusticatedImage()
	public Image generateObfusticatedImage() {
		Image image;

		IView[] allViews = mainTabSet.getAllViews();
		for (int i = 0; i < allViews.length; i++) {
			IView view = allViews[i];

			if (view instanceof ObfusticateTab) {
				Item tab = mainTabSet.getTab(view);
				tab.setText(((ObfusticateTab) view).getObfusticatedHeader());
				folder.update();
			}
		}

		Rectangle clientArea = shell.getClientArea();
		image = new Image(display, clientArea.width, clientArea.height);

		GC gc = new GC(shell);
		try {
			gc.copyArea(image, clientArea.x, clientArea.y);
		} finally {
			gc.dispose();
		}

		IView currentView = getCurrentView();

		if (currentView instanceof ObfusticateImage) {
			Point ofs = shell.toDisplay(clientArea.x, clientArea.y);
			try {
				((ObfusticateImage) currentView).obfusticatedImage(image, ofs);
			} catch (Exception e) {
				Debug.out("Obfusticating " + currentView, e);
			}
		}

		for (int i = 0; i < allViews.length; i++) {
			IView view = allViews[i];

			if (view instanceof ObfusticateTab) {
				view.refresh();
			}
		}

		return image;
	}

	private static Point getStoredWindowSize() {
		Point size = null;

		boolean isMaximized = COConfigurationManager.getBooleanParameter(
				"window.maximized", false);
		if (isMaximized) {
			Monitor monitor = SWTThread.getInstance().getPrimaryMonitor();
			if (Utils.isThisThreadSWT()) {
				if (window != null && window.getShell() != null
						&& !window.getShell().isDisposed()) {
					monitor = window.getShell().getMonitor();
				}
			}
			if (monitor != null) {
				Rectangle clientArea = monitor.getClientArea();
				size = new Point(clientArea.width, clientArea.height);
				return size;
			}
		}

		String windowRectangle = COConfigurationManager.getStringParameter(
				"window.rectangle", null);
		if (windowRectangle != null) {
			String[] values = windowRectangle.split(",");
			if (values.length == 4) {
				try {
					size = new Point(Integer.parseInt(values[2]),
							Integer.parseInt(values[3]));
				} catch (Exception e) {
				}
			}
		}
		return size;
	}

	public static void addToVersionCheckMessage(final Map map) {
		try {
				Point size = getStoredWindowSize();
				if (size == null) {
					return;
				}

				map.put("mainwindow.w", new Long(size.x));
				map.put("mainwindow.h", new Long(size.y));
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public UIFunctionsSWT getUIFunctions() {
		return uiFunctions;
	}

	public boolean isVisible(int windowElement) {
		if (windowElement == IMainWindow.WINDOW_ELEMENT_TOOLBAR) {
			return bIconBarEnabled;
		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_STATUSBAR) {
			//TODO:
		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_MENU) {
			//TODO:
		}

		return true;
	}

	public void setVisible(int windowElement, boolean value) {
		if (windowElement == IMainWindow.WINDOW_ELEMENT_TOOLBAR) {
			setIconBarEnabled(value);
		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_STATUSBAR) {
			//TODO:
		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_MENU) {
			//TODO:
		}
	}

	public Rectangle getMetrics(int windowElement) {
		if (windowElement == IMainWindow.WINDOW_ELEMENT_TOOLBAR) {
			if (null != iconBar && null != iconBar.getComposite()) {
				return iconBar.getComposite().getBounds();
			}
		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_STATUSBAR) {

			return mainStatusBar.getBounds();

		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_TOPBAR) {

			//KN: No search bar in classic UI

		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_TABBAR) {

			//KN: No tab bar in classic UI

		} else if (windowElement == IMainWindow.WINDOW_CLIENT_AREA) {

			return shell.getClientArea();

		} else if (windowElement == IMainWindow.WINDOW_CONTENT_DISPLAY_AREA) {

			Rectangle r = shell.getClientArea();

			r.x += iconBar.getComposite().getBounds().x;
			r.height -= iconBar.getComposite().getBounds().height;

			r.height -= mainStatusBar.getBounds().height;

			return r;

		}

		return new Rectangle(0, 0, 0, 0);
	}

	/**
	 * 
	 *
	 * @since 3.1.1.1
	 */
	public void closeAllDetails() {
		if (mainTabSet != null) {
			mainTabSet.closeAllDetails();
		}
	}

	/**
	 * @return
	 *
	 * @since 3.1.1.1
	 */
	public boolean hasDetailViews() {
		if (mainTabSet != null) {
			return( mainTabSet.hasDetails());
		}
		return false;
	}

	public Tab getMainTabSet() {
		return mainTabSet;
	}

}
