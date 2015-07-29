/*
 * Created on May 29, 2006 2:07:38 PM
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
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
 */
package com.aelitis.azureus.ui.swt.shells.main;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.config.impl.ConfigurationChecker;
import org.gudy.azureus2.core3.config.impl.ConfigurationDefaults;
import org.gudy.azureus2.core3.config.impl.TransferSpeedValidator;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerListener;
import org.gudy.azureus2.core3.global.GlobalManagerStats;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.associations.AssociationChecker;
import org.gudy.azureus2.ui.swt.components.shell.ShellManager;
import org.gudy.azureus2.ui.swt.config.wizard.ConfigureWizard;
import org.gudy.azureus2.ui.swt.debug.ObfusticateImage;
import org.gudy.azureus2.ui.swt.debug.ObfusticateShell;
import org.gudy.azureus2.ui.swt.debug.UIDebugGenerator;
import org.gudy.azureus2.ui.swt.donations.DonationWindow;
import org.gudy.azureus2.ui.swt.mainwindow.*;
import org.gudy.azureus2.ui.swt.minibar.AllTransfersBar;
import org.gudy.azureus2.ui.swt.minibar.MiniBarManager;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTInstanceImpl;
import org.gudy.azureus2.ui.swt.sharing.progress.ProgressWindow;
import org.gudy.azureus2.ui.swt.shells.CoreWaiterSWT;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.speedtest.SpeedTestSelector;
import org.gudy.azureus2.ui.swt.views.utils.LocProvUtils;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;
import org.gudy.azureus2.ui.swt.welcome.WelcomeWindow;
import org.gudy.azureus2.ui.systray.SystemTraySWT;

import com.aelitis.azureus.activities.VuzeActivitiesManager;
import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.messenger.config.PlatformConfigMessenger;
import com.aelitis.azureus.core.messenger.config.PlatformConfigMessenger.PlatformLoginCompleteListener;
import com.aelitis.azureus.core.messenger.config.PlatformDevicesMessenger;
import com.aelitis.azureus.core.metasearch.MetaSearchManagerFactory;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.core.util.FeatureAvailability;
import com.aelitis.azureus.core.versioncheck.VersionCheckClient;
import com.aelitis.azureus.ui.IUIIntializer;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.table.impl.TableColumnManager;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.mdi.*;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.skin.SkinPropertiesImpl;
import com.aelitis.azureus.ui.swt.*;
import com.aelitis.azureus.ui.swt.columns.utils.TableColumnCreatorV3;
import com.aelitis.azureus.ui.swt.extlistener.StimulusRPC;
import com.aelitis.azureus.ui.swt.mdi.BaseMDI;
import com.aelitis.azureus.ui.swt.mdi.TabbedMDI;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.uiupdater.UIUpdaterSWT;
import com.aelitis.azureus.ui.swt.utils.FontUtils;
import com.aelitis.azureus.ui.swt.views.skin.WelcomeView;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.util.MapUtils;
import com.aelitis.azureus.util.NavigationHelper;

/** 
 * @author TuxPaper
 * @created May 29, 2006
 *
 *
 * TODO:
 * - MainStatusBar and sidebar components should update when:
		if (parameterName.equals("config.style.useSIUnits") || parameterName.equals("config.style.forceSIValues")) {
			updateComponents();
		}
 * - IconBarEnabler for "new" and "open"
 */
public class MainWindowImpl
	implements MainWindow, ObfusticateShell, MdiListener,
	AEDiagnosticsEvidenceGenerator, MdiEntryLogIdListener, UIUpdatable
{

	private static final LogIDs LOGID = LogIDs.GUI;

	private Shell shell;

	private Display display;

	private AzureusCore core;

	private IUIIntializer uiInitializer;

	private SWTSkin skin;

	private IMainMenu menu;

	private UISWTInstanceImpl uiSWTInstanceImpl;

	private UIFunctionsImpl uiFunctions;

	private SystemTraySWT systemTraySWT;

	private static Map<String, List> mapTrackUsage = null;

	private final static AEMonitor mapTrackUsage_mon = new AEMonitor(
			"mapTrackUsage");

	private long lCurrentTrackTime = 0;

	private long lCurrentTrackTimeIdle = 0;

	private boolean disposedOrDisposing;

	private DownloadManager[] dms_Startup;

	private boolean isReady = false;

	private MainStatusBar statusBar;

	private String lastShellStatus = null;

	private Color colorSearchTextBG;

	private Color colorSearchTextFG;
	
	private boolean delayedCore;

	private TrayWindow downloadBasket;

	/**
	 * Old Initializer.  AzureusCore is required to be started
	 * 
	 * @param core
	 * @param display
	 * @param uiInitializer
	 */
	protected 
	MainWindowImpl(
		AzureusCore 			core, 
		Display 				display,
		final IUIIntializer		uiInitializer) 
	{
		delayedCore = false;
		this.core = core;
		this.display = display;
		this.uiInitializer = uiInitializer;
		AEDiagnostics.addEvidenceGenerator(this);

		disposedOrDisposing = false;

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				try {
					createWindow(uiInitializer);
				} catch (Throwable e) {
					Logger.log(new LogAlert(false, "Error Initialize MainWindow", e));
				}
				if (uiInitializer != null) {
					uiInitializer.abortProgress();
				}
			}
		});

		// When a download is added, check for new meta data and
		// un-"wait state" the rating
		GlobalManager gm = core.getGlobalManager();
		dms_Startup = (DownloadManager[]) gm.getDownloadManagers().toArray(
				new DownloadManager[0]);
		gm.addListener(new GlobalManagerListener() {

			public void seedingStatusChanged(boolean seeding_only_mode, boolean b) {
			}

			public void downloadManagerRemoved(DownloadManager dm) {
			}

			public void downloadManagerAdded(final DownloadManager dm) {
				downloadAdded(new DownloadManager[] {
					dm
				});
			}

			public void destroyed() {
			}

			public void destroyInitiated() {
			}

		}, false);

		Alerts.addListener(new Alerts.AlertListener() {

			public boolean allowPopup(Object[] relatedObjects, int configID) {
				DownloadManager dm = (DownloadManager) LogRelationUtils.queryForClass(
						relatedObjects, DownloadManager.class);

				if (dm == null) {
					return true;
				}
				if (dm.getDownloadState().getFlag(DownloadManagerState.FLAG_LOW_NOISE)) {
					return false;
				}

				return true;
			}

		});
	}

	/**
	 * New Initializer.  AzureusCore does not need to be started.
	 * Use {@link #init(AzureusCore)} when core is available.
	 * 
	 * Called for STARTUP_UIFIRST
	 * 
	 * 1) Constructor
	 * 2) createWindow
	 * 3) init(core)
	 * 
	 * @param display
	 * @param uiInitializer
	 */
	protected MainWindowImpl(final Display display, final IUIIntializer uiInitializer) {
		//System.out.println("MainWindow: constructor");
		delayedCore = true;
		this.display = display;
		this.uiInitializer = uiInitializer;
		AEDiagnostics.addEvidenceGenerator(this);

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				//System.out.println("createWindow");
				try {
					createWindow(uiInitializer);
				} catch (Throwable e) {
					Logger.log(new LogAlert(false, "Error Initialize MainWindow", e));
				}

				while (!display.isDisposed() && display.readAndDispatch());
			}
		});
	}

	/**
	 * Called only on STARTUP_UIFIRST
	 */
	
	public void init(final AzureusCore core) {
		//System.out.println("MainWindow: _init(core)");

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				//System.out.println("_init");
				_init(core);
				if (uiInitializer != null) {
					uiInitializer.abortProgress();
				}
			}
		});
		UIUpdaterSWT.getInstance().addUpdater(this);
	}

	/**
	 * Called only on STARTUP_UIFIRST
	 */
	private void _init(AzureusCore core) {
		//System.out.println("MainWindow: init(core)");
		this.core = core;

		disposedOrDisposing = false;

		StimulusRPC.hookListeners(core, this);

		if (uiSWTInstanceImpl == null) {
			uiSWTInstanceImpl = new UISWTInstanceImpl();
			uiSWTInstanceImpl.init(uiInitializer);
		}

		postPluginSetup(core);

		// When a download is added, check for new meta data and
		// un-"wait state" the rating
		GlobalManager gm = core.getGlobalManager();
		dms_Startup = (DownloadManager[]) gm.getDownloadManagers().toArray(
				new DownloadManager[0]);
		gm.addListener(new GlobalManagerListener() {

			public void seedingStatusChanged(boolean seeding_only_mode, boolean b) {
			}

			public void downloadManagerRemoved(DownloadManager dm) {
			}

			public void downloadManagerAdded(final DownloadManager dm) {
				downloadAdded(new DownloadManager[] {
					dm
				});
			}

			public void destroyed() {
			}

			public void destroyInitiated() {
			}

		}, false);

		Alerts.addListener(new Alerts.AlertListener() {

			public boolean allowPopup(Object[] relatedObjects, int configID) {
				DownloadManager dm = (DownloadManager) LogRelationUtils.queryForClass(
						relatedObjects, DownloadManager.class);

				if (dm == null) {
					return true;
				}
				if (dm.getDownloadState().getFlag(DownloadManagerState.FLAG_LOW_NOISE)) {
					return false;
				}

				return true;
			}

		});

		core.triggerLifeCycleComponentCreated(uiFunctions);

		processStartupDMS();
	}

	private void postPluginSetup(AzureusCore core) {
		// we pass core in just as reminder that this function needs core
		if (core == null) {
			return;
		}

		if (!Utils.isAZ2UI()) {
			VuzeActivitiesManager.initialize(core);
		}

		LocProvUtils.initialise( core );
		
		if (!Constants.isSafeMode) {
			
			// We used to open up share view here.  Moved to MainMDISetup.. param not used now
			COConfigurationManager.removeParameter("GUI_SWT_share_count_at_close");
			
			MainHelpers.initTransferBar();
			
			COConfigurationManager.addAndFireParameterListener("IconBar.enabled",
					new ParameterListener() {
						public void parameterChanged(String parameterName) {
							setVisible(WINDOW_ELEMENT_TOOLBAR, COConfigurationManager.getBooleanParameter(parameterName));
						}
					});
		}

		//  share progress window
		new ProgressWindow( display );
	}

	private void processStartupDMS() {
		// must be in a new thread because we don't want to block
		// initilization or any other add listeners
		AEThread2 thread = new AEThread2("v3.mw.dmAdded", true) {
			public void run() {
				long startTime = SystemTime.getCurrentTime();
				if (dms_Startup == null || dms_Startup.length == 0) {
					dms_Startup = null;
					return;
				}

				downloadAdded(dms_Startup);

				dms_Startup = null;

				System.out.println("psDMS " + (SystemTime.getCurrentTime() - startTime)
						+ "ms");
			}
		};
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
	}

	private void downloadAdded(final DownloadManager[] dms) {
		boolean oneIsNotPlatformAndPersistent = false;
		for (final DownloadManager dm : dms) {
			if (dm == null) {
				continue;
			}

			DownloadManagerState dmState = dm.getDownloadState();

			final TOTorrent torrent = dm.getTorrent();
			if (torrent == null) {
				continue;
			}

			String title = PlatformTorrentUtils.getContentTitle(torrent);
			if (title != null && title.length() > 0
					&& dmState.getDisplayName() == null) {
				dmState.setDisplayName(title);
			}

			if (ConfigurationChecker.isNewVersion() && dm.getAssumedComplete()) {
				String lastVersion = COConfigurationManager.getStringParameter("Last Version");
				if (org.gudy.azureus2.core3.util.Constants.compareVersions(lastVersion,
						"3.1.1.1") <= 0) {
					long completedTime = dmState.getLongParameter(DownloadManagerState.PARAM_DOWNLOAD_COMPLETED_TIME);
					if (completedTime < SystemTime.getOffsetTime(-(1000 * 60))) {
						PlatformTorrentUtils.setHasBeenOpened(dm, true);
					}
				}
			}

			boolean isContent = PlatformTorrentUtils.isContent(torrent, true)
					|| PlatformTorrentUtils.getContentNetworkID(torrent) == ContentNetwork.CONTENT_NETWORK_VHDNL;

			if (!oneIsNotPlatformAndPersistent && !isContent
					&& !dmState.getFlag(DownloadManagerState.FLAG_LOW_NOISE) && dm.isPersistent()) {
				oneIsNotPlatformAndPersistent = true;
			}

			if (isContent) {
				long now = SystemTime.getCurrentTime();

				long expiresOn = PlatformTorrentUtils.getExpiresOn(torrent);
				if (expiresOn > now) {
					SimpleTimer.addEvent("dm Expirey", expiresOn,
							new TimerEventPerformer() {
								public void perform(TimerEvent event) {
									dm.getDownloadState().setFlag(
											DownloadManagerState.FLAG_LOW_NOISE, true);
									ManagerUtils.asyncStopDelete(dm, DownloadManager.STATE_STOPPED,
											true, true, null);
								}
							});
				}
			} // isContent
		}

		if (oneIsNotPlatformAndPersistent && dms_Startup == null) {
			DonationWindow.checkForDonationPopup();
		}
	}

	/**
	 * @param uiInitializer 
	 * 
	 * called in both delayedCore and !delayedCore
	 */
	private void createWindow(IUIIntializer uiInitializer) {
		//System.out.println("MainWindow: createWindow)");

		long startTime = SystemTime.getCurrentTime();

		UIFunctionsSWT existing_uif = UIFunctionsManagerSWT.getUIFunctionsSWT();
				
		uiFunctions = new UIFunctionsImpl(this);
		
		UIFunctionsManager.setUIFunctions(uiFunctions);

		Utils.disposeComposite(shell);

		increaseProgress(uiInitializer, "splash.initializeGui");

		System.out.println("UIFunctions/ImageLoad took "
				+ (SystemTime.getCurrentTime() - startTime) + "ms");
		startTime = SystemTime.getCurrentTime();

		shell = existing_uif==null?new Shell(display, SWT.SHELL_TRIM):existing_uif.getMainShell();

		if (Constants.isWindows) {
			try {
				Class<?> ehancerClass = Class.forName("org.gudy.azureus2.ui.swt.win32.Win32UIEnhancer");
				Method method = ehancerClass.getMethod("initMainShell",
						new Class[] {
							Shell.class
						});
				method.invoke(null, new Object[] {
					shell
				});
			} catch (Exception e) {
				Debug.outNoStack(Debug.getCompressedStackTrace(e, 0, 30), true);
			}
		}

		try {
			shell.setData("class", this);
			shell.setText( UIFunctions.MAIN_WINDOW_NAME );
			Utils.setShellIcon(shell);
			Utils.linkShellMetricsToConfig(shell, "window");
			//Shell activeShell = display.getActiveShell();
			//shell.setVisible(true);
			//shell.moveBelow(activeShell);

			System.out.println("new shell took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			startTime = SystemTime.getCurrentTime();

			PlatformConfigMessenger.addPlatformLoginCompleteListener(new PlatformLoginCompleteListener() {
				public void platformLoginComplete() {
					Utils.execSWTThread(new AERunnable() {
						public void runSupport() {
							setupUsageTracker();
						}
					});
				}
			});
			
			increaseProgress(uiInitializer, "v3.splash.initSkin");

			skin = SWTSkinFactory.getInstance();
			if (Utils.isAZ2UI()) {
  			SWTSkinProperties skinProperties = skin.getSkinProperties();
  			String skinPath = SkinPropertiesImpl.PATH_SKIN_DEFS + "skin3_classic";
  			ResourceBundle rb = ResourceBundle.getBundle(skinPath);
  			skinProperties.addResourceBundle(rb, skinPath);
			}

			/*
			 * KN: passing the skin to the uifunctions so it can be used by UIFunctionsSWT.createMenu()
			 */
			uiFunctions.setSkin(skin);

			System.out.println("new shell setup took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			startTime = SystemTime.getCurrentTime();

			initSkinListeners();

			increaseProgress(uiInitializer, "v3.splash.initSkin");
			// 0ms
			//System.out.println("skinlisteners init took " + (SystemTime.getCurrentTime() - startTime) + "ms");
			//startTime = SystemTime.getCurrentTime();

			String startID = Utils.isAZ2UI() ? "classic.shell" : "main.shell";
			skin.initialize(shell, startID, uiInitializer);

			increaseProgress(uiInitializer, "v3.splash.initSkin");
			System.out.println("skin init took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			startTime = SystemTime.getCurrentTime();

			if (Utils.isAZ2UI()) {
				menu = new org.gudy.azureus2.ui.swt.mainwindow.MainMenu(shell);
			} else {
				menu = new MainMenu(skin, shell);
			}
			shell.setData("MainMenu", menu);

			System.out.println("MainMenu init took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			startTime = SystemTime.getCurrentTime();

			if (Constants.isOSX) {
				if (Utils.isCarbon) {
  				try {
  
  					Class<?> ehancerClass = Class.forName("org.gudy.azureus2.ui.swt.osx.CarbonUIEnhancer");
  
  					Method method = ehancerClass.getMethod("registerToolbarToggle",
  							new Class[] {
  								Shell.class
  							});
  					method.invoke(null, new Object[] {
  						shell
  					});
  
  				} catch (Exception e) {
  					Debug.printStackTrace(e);
  				}
				} else if (Utils.isCocoa) {
					try {

						Class<?> ehancerClass = Class.forName("org.gudy.azureus2.ui.swt.osx.CocoaUIEnhancer");

						Method mGetInstance = ehancerClass.getMethod("getInstance",
								new Class[0]);
						Object claObj = mGetInstance.invoke(null, new Object[0]);

						Method mregTBToggle = claObj.getClass().getMethod(
								"registerToolbarToggle", new Class[] {
									Shell.class
								});
						if (mregTBToggle != null) {
							mregTBToggle.invoke(claObj, new Object[] {
								shell
							});
						}

					} catch (Throwable e) {
						if (!Constants.isOSX_10_7_OrHigher) {
							Debug.printStackTrace(e);
						}
					}

				}

				Listener toggleListener = new Listener() {
					public void handleEvent(Event event) {
						SWTSkinObject so = skin.getSkinObject(SkinConstants.VIEWID_TOOLBAR);
						if (so != null) {
							so.setVisible(!so.isVisible());
						}
					}
				};
				shell.addListener(SWT.Expand, toggleListener);
				shell.addListener(SWT.Collapse, toggleListener);

				System.out.println("createWindow init took "
						+ (SystemTime.getCurrentTime() - startTime) + "ms");
				startTime = SystemTime.getCurrentTime();
			}


			increaseProgress(uiInitializer, "v3.splash.initSkin");

			skin.layout();

			// 0ms
			//System.out.println("skin layout took " + (SystemTime.getCurrentTime() - startTime) + "ms");
			//startTime = SystemTime.getCurrentTime();

			try {
				Utils.createTorrentDropTarget(shell, false);
			} catch (Throwable e) {
				Logger.log(new LogEvent(LOGID, "Drag and Drop not available", e));
			}

			shell.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					dispose(false, false);
				}
			});

			shell.addShellListener(new ShellAdapter() {
				public void shellClosed(ShellEvent event) {
					if (disposedOrDisposing) {
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

				public void shellActivated(ShellEvent e) {
					Shell shellAppModal = Utils.findFirstShellWithStyle(SWT.APPLICATION_MODAL);
					if (shellAppModal != null) {
						shellAppModal.forceActive();
					} else {
						shell.forceActive();
					}
				}

				public void shellIconified(ShellEvent event) {
					if (disposedOrDisposing) {
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
						if (core == null) {
							return;
						}
						GlobalManager gm = core.getGlobalManager();
						if (gm != null) {
							UIFunctionsManagerSWT.getUIFunctionsSWT().openTorrentWindow();
							event.doit = false;
						}
					} else if (key == 'f'
							&& (event.stateMask & (SWT.MOD1 + SWT.SHIFT)) == SWT.MOD1
									+ SWT.SHIFT) {
						shell.setFullScreen(!shell.getFullScreen());
					}
				}
			});

			increaseProgress(uiInitializer, "v3.splash.initSkin");
			System.out.println("pre skin widgets init took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			startTime = SystemTime.getCurrentTime();

			if (core != null) {
				StimulusRPC.hookListeners(core, this);
			}

			increaseProgress(uiInitializer, "v3.splash.initSkin");
			// 0ms
			//System.out.println("hooks init took " + (SystemTime.getCurrentTime() - startTime) + "ms");
			//startTime = SystemTime.getCurrentTime();

			initMDI();
			System.out.println("skin widgets (1/2) init took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			startTime = SystemTime.getCurrentTime();
			initWidgets2();

			increaseProgress(uiInitializer, "v3.splash.initSkin");
			System.out.println("skin widgets (2/2) init took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			startTime = SystemTime.getCurrentTime();

			System.out.println("pre SWTInstance init took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			increaseProgress(uiInitializer, "v3.splash.hookPluginUI");
			startTime = SystemTime.getCurrentTime();

			TableColumnCreatorV3.initCoreColumns();

			System.out.println("Init Core Columns took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			increaseProgress(uiInitializer, "v3.splash.hookPluginUI");
			startTime = SystemTime.getCurrentTime();

			// attach the UI to plugins
			// Must be done before initializing views, since plugins may register
			// table columns and other objects
			uiSWTInstanceImpl = new UISWTInstanceImpl();
			uiSWTInstanceImpl.init(uiInitializer);

			System.out.println("SWTInstance init took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			increaseProgress(uiInitializer, "splash.initializeGui");
			startTime = SystemTime.getCurrentTime();

		} catch (Throwable t) {
			Debug.out(t);
		} finally {

			String configID = SkinConstants.VIEWID_PLUGINBAR + ".visible";
			if (false == ConfigurationDefaults.getInstance().doesParameterDefaultExist(
					configID)) {
				COConfigurationManager.setBooleanDefault(configID, true);
			}
			setVisible(WINDOW_ELEMENT_TOPBAR,
					COConfigurationManager.getBooleanParameter(configID)
							&& COConfigurationManager.getIntParameter("User Mode") > 1);

			setVisible(WINDOW_ELEMENT_TOOLBAR,
					COConfigurationManager.getBooleanParameter("IconBar.enabled"));

			shell.layout(true, true);

			System.out.println("shell.layout took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			startTime = SystemTime.getCurrentTime();

			showMainWindow();

			//================

			increaseProgress(uiInitializer, "splash.initializeGui");

			System.out.println("shell.open took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			startTime = SystemTime.getCurrentTime();

			processStartupDMS();

			System.out.println("processStartupDMS took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			startTime = SystemTime.getCurrentTime();

			if (core != null) {
				postPluginSetup(core);
			}

			System.out.println("postPluginSetup init took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			startTime = SystemTime.getCurrentTime();

			NavigationHelper.addListener(new NavigationHelper.navigationListener() {
				public void processCommand(final int type, final String[] args) {
					Utils.execSWTThread(new AERunnable() {
						public void runSupport() {

							UIFunctions uif = UIFunctionsManager.getUIFunctions();

							if (type == NavigationHelper.COMMAND_SWITCH_TO_TAB) {
								MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
								if (mdi == null) {
									return;
								}
								mdi.showEntryByID(args[0]);

								if (uif != null) {

									uif.bringToFront();
								}
							} else if (type == NavigationHelper.COMMAND_CONDITION_CHECK) {
							}
						}
					});
				}
			});
			
			if ( !Constants.isOSX ){
				
				COConfigurationManager.addAndFireParameterListener(
					"Show Status In Window Title",
					new ParameterListener()
					{
						private TimerEventPeriodic 	timer;
						private String				old_text;
						private String				my_last_text;
						
						public void 
						parameterChanged(
							final String name ) 
						{
							Utils.execSWTThread(
								new AERunnable() 
								{
									public void 
									runSupport() 
									{
										boolean enable = COConfigurationManager.getBooleanParameter( name );
										
										if ( enable ){
											
											if ( timer == null ){
												
												timer = SimpleTimer.addPeriodicEvent(
													"window.title.updater",
													1000,
													new TimerEventPerformer()
													{
														public void 
														perform(
															TimerEvent event) 
														{
															Utils.execSWTThread(
																	new AERunnable() 
																	{
																		public void 
																		runSupport() 
																		{
																			if ( shell.isDisposed()){
																				
																				return;
																			}
																			
																			String txt = shell.getText();
																			
																			if ( txt != null && !txt.equals( my_last_text )){
																				
																				old_text = txt;
																			}
																			
																			txt = getCurrentTitleText();
													
																			if ( txt != null ){
																			
																				shell.setText( txt );
																			
																				my_last_text = txt;
																			}
																		}
																	});
														}
													});
											}
										}else{
											
											if ( timer != null ){
												
												timer.cancel();
												
												timer = null;
											}
											
											if ( old_text != null && !shell.isDisposed()){
												
												shell.setText( old_text );
											}
										}
									}
								});
						}
					});
			}
		}
	}
	
	private String	last_eta_str = null;
	private long	last_eta;
	private int		eta_tick_count;
	
	private String
	getCurrentTitleText()
	{
		if ( core == null ){
			
			return( null );
		}
		
		GlobalManager gm = core.getGlobalManager();
		
		if ( gm == null ){
			
			return( null );
		}
		
		GlobalManagerStats stats = gm.getStats();
		
		int down 	= stats.getDataReceiveRate() + stats.getProtocolReceiveRate();
		int up		= stats.getDataSendRate() + stats.getProtocolSendRate();
		
		eta_tick_count++;
		
		String eta_str = last_eta_str;
		
		if ( 	eta_str == null ||
				last_eta < 120 ||
				eta_tick_count%10 == 0 ){
			
			long	min_eta = Long.MAX_VALUE;
			int		num_downloading = 0;
			
			List<DownloadManager> dms = gm.getDownloadManagers();
			
			for ( DownloadManager dm: dms ){
				
				if ( dm.getState() == DownloadManager.STATE_DOWNLOADING ){
					
					num_downloading++;
					
					long dm_eta = dm.getStats().getSmoothedETA();
					
					if ( dm_eta < min_eta ){
						
						min_eta = dm_eta;
					}
				}
			}
			
			if ( min_eta == Long.MAX_VALUE ){
				
				min_eta = Constants.CRAPPY_INFINITE_AS_LONG;
			}
			
			last_eta = min_eta;
			
			eta_str = last_eta_str = num_downloading==0?"":DisplayFormatters.formatETA(min_eta);
		}
		
		
		String down_str = formatRateCompact( down );
		String up_str 	= formatRateCompact( up );
		
		StringBuilder result = new StringBuilder( 50 );
		
		result.append( MessageText.getString( "ConfigView.download.abbreviated" ));
		result.append( " " );
		result.append( down_str );
		result.append( " " );
		result.append( MessageText.getString( "ConfigView.upload.abbreviated" ));
		result.append( " " );
		result.append( up_str );
		
		if ( eta_str.length() > 0 ){
			
			result.append( " " );
			result.append( MessageText.getString( "ConfigView.eta.abbreviated" ));
			result.append( " " );
			result.append( eta_str );
		}
		
		return( result.toString());
	}
	
	private String
	formatRateCompact(
		int		rate )
	{
		String str = DisplayFormatters.formatByteCountToKiBEtc( rate, false, true, 2, DisplayFormatters.UNIT_KB );
		
		String[] bits = str.split( " " );
	
		if ( bits.length == 2 ){
	
			String sep = String.valueOf( DisplayFormatters.getDecimalSeparator());
			
			String num 	= bits[0];
			String unit = bits[1];
			
			int	num_len = num.length();
			
			if ( num_len < 4 ){
				
				if ( !num.contains( sep )){
					
					num += sep;
					
					num_len++;
				}
				
				while( num_len < 4 ){
					
					num += "0";
					
					num_len++;
				}
			}else{
				if ( num_len > 4 ){
					
					num = num.substring( 0, 4 );
					
					num_len = 4;
				}
			}
			
			if ( num.endsWith( sep )){

				num = num.substring( 0, num_len - 1 ) + " ";
			}
			
			str = num + " " + unit.charAt(0);
		}
		
		return( str );
	}
	
	/**
	 * @param uiInitializer
	 * @param taskKey TODO
	 *
	 * @since 3.0.4.3
	 */
	private void increaseProgress(IUIIntializer uiInitializer, String taskKey) {
		if (uiInitializer != null) {
			uiInitializer.increaseProgress();
			if (taskKey != null) {
				uiInitializer.reportCurrentTask(MessageText.getString(taskKey));
			}
		}
		// XXX Disabled because plugin update window will pop up and take control
		// 		 of the dispatch loop..
		/*
		if (Utils.isThisThreadSWT()) {
			// clean the dispatch loop so the splash screen gets updated
			int i = 1000;
			while (display.readAndDispatch() && i > 0) {
				i--;
			}
			//if (i < 999) {
			//	System.out.println("dispatched " + (1000 - i));
			//}
		}
		*/
	}

	@SuppressWarnings("deprecation")
	public boolean dispose(final boolean for_restart,
			final boolean close_already_in_progress) {
		if (disposedOrDisposing) {
			return true;
		}
		Boolean b = Utils.execSWTThreadWithBool("v3.MainWindow.dispose",
				new AERunnableBoolean() {
					public boolean runSupport() {
						return _dispose(for_restart, close_already_in_progress);
					}
				});
		return b == null || b;
	}

	private boolean _dispose(final boolean bForRestart, boolean bCloseAlreadyInProgress) {
		if (disposedOrDisposing) {
			return true;
		}

		disposedOrDisposing = true;
		if (core != null
				&& !UIExitUtilsSWT.canClose(core.getGlobalManager(), bForRestart)) {
			disposedOrDisposing = false;
			return false;
		}

		isReady = false;

		UIExitUtilsSWT.uiShutdown();

		if (systemTraySWT != null) {
			systemTraySWT.dispose();
		}

		/**
		 * Explicitly force the transfer bar location to be saved (if appropriate and open).
		 * 
		 * We can't rely that the normal mechanism for doing this won't fail (which it usually does)
		 * when the GUI is being disposed of.
		 */
		try {
			if ( core != null ){
		  		AllTransfersBar transfer_bar = AllTransfersBar.getBarIfOpen(core.getGlobalManager());
		  		if (transfer_bar != null) {
		  			transfer_bar.forceSaveLocation();
		  		}
			}
		} catch (Exception ignore) {
		}

		mapTrackUsage_mon.enter();
		try {
			if (mapTrackUsage != null) {
				String id = getUsageActiveTabID();
				if (id != null) {
					if (lastShellStatus == null) {
						lastShellStatus = id;
					}
					updateMapTrackUsage(lastShellStatus);
				}

				Map<String, Object> map = new HashMap<String, Object>();
				map.put("version",
						org.gudy.azureus2.core3.util.Constants.AZUREUS_VERSION);
				map.put("statsmap", mapTrackUsage);

				FileUtil.writeResilientFile(new File(SystemProperties.getUserPath(),
						"timingstats.dat"), map);
			}
		} finally {
			mapTrackUsage_mon.exit();
		}

		if (!SWTThread.getInstance().isTerminated()) {
			Utils.getOffOfSWTThread(new AERunnable() {
				public void runSupport() {
					if (!SWTThread.getInstance().isTerminated()) {
						SWTThread.getInstance().getInitializer().stopIt(bForRestart, false);
					}
				}
			});
		}

		return true;
	}

	private String getUsageActiveTabID() {
		try {
			MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
			if (mdi != null) {
				MdiEntry curEntry = mdi.getCurrentEntry();
				if (curEntry == null) {
					return "none";
				}
				String id = curEntry.getLogID();
				return id == null ? "null" : id;
			}
		} catch (Exception e) {
			String name = e.getClass().getName();
			int i = name.indexOf('.');
			if (i > 0) {
				return name.substring(i);
			}
			return name;
		}
		return "unknown";
	}

	/**
	 * 
	 */
	private void setupUsageTracker() {
		mapTrackUsage_mon.enter();
		try {
			File f = new File(SystemProperties.getUserPath(), "timingstats.dat");

			if (COConfigurationManager.getBooleanParameter("Send Version Info")
					&& PlatformConfigMessenger.allowSendStats()) {

				mapTrackUsage = new HashMap<String, List>();

				if (f.exists()) {
					Map<?, ?> oldMapTrackUsage = FileUtil.readResilientFile(f);
					String version = MapUtils.getMapString(oldMapTrackUsage, "version",
							null);
					Map<?, ?> map = MapUtils.getMapMap(oldMapTrackUsage, "statsmap", null);
					if (version != null && map != null) {
						PlatformConfigMessenger.sendUsageStats(map, f.lastModified(),
								version, null);
					}
				}

				SimpleTimer.addPeriodicEvent("UsageTracker", 1000,
						new TimerEventPerformer() {
							long lLastMouseMove = SystemTime.getCurrentTime();

							Point ptLastMousePos = new Point(0, 0);

							public void perform(TimerEvent event) {
								Utils.execSWTThread(new AERunnable() {
									public void runSupport() {
										if (shell == null || shell.isDisposed()
												|| shell.getDisplay().getActiveShell() == null) {
											// so when we become active again, we count a few
											// seconds (if the mouse moves)
											if (ptLastMousePos.x > 0) {
												ptLastMousePos.x = 0;
												ptLastMousePos.y = 0;
												lLastMouseMove = 0;
											}
											return;
										}

										Point pt = shell.getDisplay().getCursorLocation();
										if (pt.equals(ptLastMousePos)) {
											return;
										}
										ptLastMousePos = pt;

										long now = SystemTime.getCurrentTime();
										if (lLastMouseMove > 0) {
											long diff = now - lLastMouseMove;
											if (diff < 10000) {
												lCurrentTrackTime += diff;
											} else {
												lCurrentTrackTimeIdle += diff;
											}
										}

										lLastMouseMove = now;
									}
								});
							}
						});

				Listener lActivateDeactivate = new Listener() {
					long start;

					public void handleEvent(Event event) {
						if (event.type == SWT.Activate) {
							lCurrentTrackTimeIdle = 0;
							if (start > 0 && lastShellStatus != null) {
								lCurrentTrackTime = SystemTime.getCurrentTime() - start;
								updateMapTrackUsage(lastShellStatus);
							}
							lastShellStatus = null;
						} else {
							updateMapTrackUsage(getUsageActiveTabID());
							if (shell.getMinimized()) {
								lastShellStatus = "idle-minimized";
							} else if (!shell.isVisible()) {
								lastShellStatus = "idle-invisible";
							} else {
								lastShellStatus = "idle-nofocus";
							}
							start = SystemTime.getCurrentTime();
						}
					}
				};
				shell.addListener(SWT.Activate, lActivateDeactivate);
				shell.addListener(SWT.Deactivate, lActivateDeactivate);

			} else {
				mapTrackUsage = null;
				// No use keeping old usage stats if we are told no one wants them
				try {
					if (f.exists()) {
						f.delete();
					}
				} catch (Exception e) {
				}
			}
		} catch (Exception e) {
			Debug.out(e);
		} finally {
			mapTrackUsage_mon.exit();
		}
	}

	private Set<Shell>	minimized_on_hide = new HashSet<Shell>();
	
	private void showMainWindow() {
		COConfigurationManager.addAndFireParameterListener("Show Download Basket", new ParameterListener() {
			public void parameterChanged(String parameterName) {
				configureDownloadBasket();
			}
		});

		boolean isOSX = org.gudy.azureus2.core3.util.Constants.isOSX;
		boolean bEnableTray = COConfigurationManager.getBooleanParameter("Enable System Tray");
		boolean bPassworded = COConfigurationManager.getBooleanParameter("Password enabled");
		boolean bStartMinimize = bEnableTray
				&& (bPassworded || COConfigurationManager.getBooleanParameter("Start Minimized"));

		SWTSkinObject soMain = skin.getSkinObject("main");
		if (soMain != null) {
			soMain.getControl().setVisible(true);
		}

		shell.addListener(SWT.Show, new Listener() {
			public void handleEvent(Event event) {
				System.out.println("---------SHOWN AT " + SystemTime.getCurrentTime()
						+ ";" + (SystemTime.getCurrentTime() - Initializer.startTime)
						+ "ms");
				
					// attempt to fix occasional missing status bar on show (running async seems to fix issue for me)
				
				if ( statusBar != null ){
					Utils.execSWTThreadLater(
						10,
						new Runnable()
						{
							public void 
							run()
							{
								statusBar.relayout();
							}
						});
				}
				
					// bring back and stand-alone shells 
				
				ShellManager.sharedManager().performForShells(
						new Listener()
						{
							public void 
							handleEvent(
								Event event) 
							{
								Shell this_shell = (Shell)event.widget;
								
								if ( this_shell.getParent() == null && !this_shell.isVisible()){
								
									boolean	minimize;
									
									synchronized( minimized_on_hide ){
										
										minimize = minimized_on_hide.remove( this_shell );
									}
									
									this_shell.setVisible( true );
									
									if ( minimize ){
										
										this_shell.setMinimized( true );

									}else{
									
										this_shell.moveAbove( shell );
									}
								}
							}
						});
			}
		});

		if (!bStartMinimize) {
			shell.open();
			if (!isOSX) {
				shell.forceActive();
			}
		} else if (Utils.isCarbon) {
			shell.setVisible(true);
			shell.setMinimized(true);
		}
		

		if (delayedCore) {
			// max 5 seconds of dispatching.  We don't display.sleep here because
			// we only want to clear the backlog of SWT events, and sleep would
			// add new ones
			try {
  			long endSWTDispatchOn = SystemTime.getOffsetTime(5000);
  			while (SystemTime.getCurrentTime() < endSWTDispatchOn
  					&& !display.isDisposed() && display.readAndDispatch());
			} catch (Exception e) {
				Debug.out(e);
			}

			System.out.println("---------DONE DISPATCH AT "
  				+ SystemTime.getCurrentTime() + ";"
  				+ (SystemTime.getCurrentTime() - Initializer.startTime) + "ms");
  		if (display.isDisposed()) {
  			return;
  		}
		}

		if (bEnableTray) {

			try {
				systemTraySWT = SystemTraySWT.getTray();

			} catch (Throwable e) {

				e.printStackTrace();
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
					setVisible(true); // invokes password
				}
			}
		}

		// do this before other checks as these are blocking dialogs to force order

		if (uiInitializer != null) {

			uiInitializer.initializationComplete();
		}
			
		boolean	run_speed_test = false;

		if (!Utils.isAZ2UI() && !COConfigurationManager.getBooleanParameter("SpeedTest Completed")){
			
			
			if ( ConfigurationChecker.isNewInstall()){
				
				// 4813 - removed auto-speedtest on new install
				//run_speed_test = true;
				
			}else if ( FeatureAvailability.triggerSpeedTestV1()){
				
				long	upload_limit	= COConfigurationManager.getLongParameter("Max Upload Speed KBs" );
				boolean	auto_up			= COConfigurationManager.getBooleanParameter( TransferSpeedValidator.AUTO_UPLOAD_ENABLED_CONFIGKEY );
				
				if ( auto_up ){
					
					if ( upload_limit <= 18 ){
						
						run_speed_test = true;
					}
				}else{
					
					boolean up_seed_limit	= COConfigurationManager.getBooleanParameter("enable.seedingonly.upload.rate" );
				
					if ( upload_limit == 0 && !up_seed_limit ){
						
						run_speed_test = true;
					}
				}
			}
		}
		
		
		if ( run_speed_test ){

			SpeedTestSelector.runMLABTest(
				new AERunnable() 
				{
					public void 
					runSupport() 
					{
						WelcomeView.setWaitLoadingURL(false);
					}
				});
		}else{
			
			WelcomeView.setWaitLoadingURL(false);
		}

		if (Utils.isAZ2UI()) {
  		if (!COConfigurationManager.getBooleanParameter("Wizard Completed")) {
  
  			CoreWaiterSWT.waitForCoreRunning(new AzureusCoreRunningListener() {
  				public void azureusCoreRunning(AzureusCore core) {
  					new ConfigureWizard(false, ConfigureWizard.WIZARD_MODE_FULL);
  				}
  			});
  		}

			checkForWhatsNewWindow();
		}

		AssociationChecker.checkAssociations();

		// Donation stuff
		Map<?, ?> map = VersionCheckClient.getSingleton().getMostRecentVersionCheckData();
		DonationWindow.setInitialAskHours(MapUtils.getMapInt(map,
				"donations.askhrs", DonationWindow.getInitialAskHours()));

		if (core != null) {
			core.triggerLifeCycleComponentCreated(uiFunctions);
		}

		System.out.println("---------READY AT " + SystemTime.getCurrentTime() + ";"
				+ (SystemTime.getCurrentTime() - Initializer.startTime) + "ms");
		isReady = true;
		//SESecurityManagerImpl.getSingleton().exitVM(0);
	}

	private void configureDownloadBasket() {
		if (COConfigurationManager.getBooleanParameter("Show Download Basket")) {
			if (downloadBasket == null) {
				downloadBasket = new TrayWindow();
				downloadBasket.setVisible(true);
			}
		} else if (downloadBasket != null) {
			downloadBasket.setVisible(false);
			downloadBasket = null;
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

	public void 
	setHideAll(
		final boolean hide )
	{
		Utils.execSWTThread(
			new AERunnable() 
			{
				public void
				runSupport()
				{
					if ( hide ){
						
						setVisible( false, true );
					
						if ( systemTraySWT != null ){
						
							systemTraySWT.dispose();
						}
					}else{
						
						setVisible( true, true );
						
						if ( COConfigurationManager.getBooleanParameter("Enable System Tray")) {
			
							systemTraySWT = SystemTraySWT.getTray();
						}
					}
				}
			});
	}
	
	private void setVisible(final boolean visible) {
		setVisible(visible, true);
	}

	public void setVisible(final boolean visible, final boolean tryTricks) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				boolean currentlyVisible = shell.getVisible() && !shell.getMinimized();
				if (visible && !currentlyVisible) {
					if (COConfigurationManager.getBooleanParameter("Password enabled")) {
						if (!PasswordWindow.showPasswordWindow(display)) {
							shell.setVisible(false);
							return;
						}
					}
				}
				
				if (!isReady) {
					return;
				}

				ArrayList<Shell> wasVisibleList = null;
				boolean bHideAndShow = false;
				// temp disabled
				//tryTricks && visible && Constants.isWindows && display.getActiveShell() != shell;
				if (bHideAndShow) {
					wasVisibleList = new ArrayList<Shell>();
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
					if (shell.getMinimized()) {
						shell.setMinimized(false);
					}
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
					shell.forceActive();

					if (bHideAndShow) {
						try {
							Shell[] shells = shell.getDisplay().getShells();
							for (int i = 0; i < shells.length; i++) {
								if (shells[i] != shell) {
									if (wasVisibleList != null
											&& wasVisibleList.contains(shells[i])) {
										shells[i].setVisible(visible);
									}
									shells[i].setFocus();
								}
							}
						} catch (Exception e) {
						}
					}
				}

			}
		});
	}

	private void minimizeToTray(ShellEvent event) {
		//Added this test so that we can call this method with null parameter.
		if (event != null) {
			event.doit = false;
		}

		// XXX hack for release.. should not access param outside Utils.linkShellMetrics
		COConfigurationManager.setParameter("window.maximized",
				shell.getMaximized());
		shell.setVisible(false);
		
		ShellManager.sharedManager().performForShells(
			new Listener()
			{
				public void 
				handleEvent(
					Event event) 
				{
					final Shell shell = (Shell)event.widget;
					
					if ( shell.getParent() == null ){
						
						if ( shell.getMinimized()){
							
							synchronized( minimized_on_hide ){
								
								minimized_on_hide.add( shell );
								
								shell.addDisposeListener(
									new DisposeListener() {
										
										public void 
										widgetDisposed(
											DisposeEvent e) 
										{
											synchronized( minimized_on_hide ){
												
												minimized_on_hide.remove( shell );
											}
										}
									});
							}
						}
						
						shell.setVisible( false );
					}
				}
			});
		
		MiniBarManager.getManager().setAllVisible(true);
	}

	/**
	 * Associates every view ID that we use to a class, and creates the class
	 * on first EVENT_SHOW.
	 */
	private void initSkinListeners() {
		UISkinnableManagerSWT skinnableManagerSWT = UISkinnableManagerSWT.getInstance();
		skinnableManagerSWT.addSkinnableListener(MessageBoxShell.class.toString(),
				new UISkinnableSWTListener() {
					public void skinBeforeComponents(Composite composite,
							Object skinnableObject, Object[] relatedObjects) {

						MessageBoxShell shell = (MessageBoxShell) skinnableObject;

						TOTorrent torrent = null;
						DownloadManager dm = (DownloadManager) LogRelationUtils.queryForClass(
								relatedObjects, DownloadManager.class);
						if (dm != null) {
							torrent = dm.getTorrent();
						} else {
							torrent = (TOTorrent) LogRelationUtils.queryForClass(
									relatedObjects, TOTorrent.class);
						}

						if (torrent != null && shell.getLeftImage() == null) {
							byte[] contentThumbnail = PlatformTorrentUtils.getContentThumbnail(torrent);
							if (contentThumbnail != null) {
								try {
									ByteArrayInputStream bis = new ByteArrayInputStream(
											contentThumbnail);
									final Image img = new Image(Display.getDefault(), bis);

									shell.setLeftImage(img);

									composite.addDisposeListener(new DisposeListener() {
										public void widgetDisposed(DisposeEvent e) {
											if (!img.isDisposed()) {
												img.dispose();
											}
										}
									});
								} catch (Exception e) {

								}
							}
						}
					}

					public void skinAfterComponents(Composite composite,
							Object skinnableObject, Object[] relatedObjects) {
					}
				});
	}

	private void initMDI() {
		Class<?> classMDI = Utils.isAZ2UI() ? TabbedMDI.class : SideBar.class;

		try {
			SWTSkinObject skinObject = skin.getSkinObject(SkinConstants.VIEWID_MDI);
			if (null != skinObject) {
				BaseMDI mdi = (BaseMDI) classMDI.newInstance();
				mdi.setMainSkinObject(skinObject);
				skinObject.addListener(mdi);
				MainMDISetup.setupSideBar(mdi, this);
			}
		} catch (Throwable t) {
			Debug.out(t);
		}
	}

	private void initWidgets2() {
		SWTSkinObject skinObject = skin.getSkinObject("statusbar");
		if (skinObject != null) {
			final Composite cArea = (Composite) skinObject.getControl();

			statusBar = new MainStatusBar();
			Composite composite = statusBar.initStatusBar(cArea);

			composite.setLayoutData(Utils.getFilledFormData());
		}

		skinObject = skin.getSkinObject("search-text");
		if (skinObject != null) {
			attachSearchBox(skinObject);
		}

		skinObject = skin.getSkinObject("add-torrent");
		if (skinObject instanceof SWTSkinObjectButton) {
			SWTSkinObjectButton btn = (SWTSkinObjectButton) skinObject;
			btn.addSelectionListener(new ButtonListenerAdapter() {
				// @see com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter#pressed(com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility, com.aelitis.azureus.ui.swt.skin.SWTSkinObject, int)
				public void pressed(SWTSkinButtonUtility buttonUtility,
						SWTSkinObject skinObject, int stateMask) {
					UIFunctionsManagerSWT.getUIFunctionsSWT().openTorrentWindow();
				}
			});
		}

		skinObject = skin.getSkinObject(SkinConstants.VIEWID_PLUGINBAR);
		if (skinObject != null) {
			Menu topbarMenu = new Menu(shell, SWT.POP_UP);

			if (COConfigurationManager.getIntParameter("User Mode") > 1) {
				MenuItem mi = 
					MainMenu.createViewMenuItem(skin, topbarMenu,
						"v3.MainWindow.menu.view." + SkinConstants.VIEWID_PLUGINBAR,
						SkinConstants.VIEWID_PLUGINBAR + ".visible",
						SkinConstants.VIEWID_PLUGINBAR, true, -1);
				
				if ( Utils.isAZ2UI()){
					
						// remove any accelerator as it doesn't work on this menu and we don't have a View menu entry
					
					String str = mi.getText();
					
					int pos = str.indexOf( "\t" );
					
					if ( pos != -1 ){
						
						str = str.substring(0,pos).trim();
						
						mi.setText( str );
					}
					
					mi.setAccelerator( SWT.NULL );
				}
			}

			new MenuItem(topbarMenu, SWT.SEPARATOR);
			
			final MenuItem itemClipMon = new MenuItem(topbarMenu, SWT.CHECK );
			Messages.setLanguageText(itemClipMon,
					"label.monitor.clipboard");
			itemClipMon.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					COConfigurationManager.setParameter( "Monitor Clipboard For Torrents", itemClipMon.getSelection());
				}
			});
			
			boolean enabled = COConfigurationManager.getBooleanParameter( "Monitor Clipboard For Torrents" );
			itemClipMon.setSelection( enabled );
			
			COConfigurationManager.addAndFireParameterListener(
				"Monitor Clipboard For Torrents",
				new ParameterListener() {
					
					private volatile 	AEThread2 monitor_thread;
					private Clipboard 	clipboard;
					
					private String		last_text;
					
					public void parameterChanged(String parameterName){
						
						boolean enabled = COConfigurationManager.getBooleanParameter( parameterName );
						
						if ( enabled ){
							
							if ( clipboard == null ){
								
								clipboard = new Clipboard(Display.getDefault());
							}
							
							if ( monitor_thread == null ){
								
								final AEThread2 new_thread[] = {null};
								
								monitor_thread = new_thread[0] = new
									AEThread2( "Clipboard Monitor")
									{
										public void 
										run() 
										{
											Runnable checker = 
												new Runnable() 
												{	
													public void 
													run() 
													{
														if ( monitor_thread != new_thread[0] || clipboard == null ){
															
															return;
														}
														
														String text = (String)clipboard.getContents(TextTransfer.getInstance());
																												
														if ( text != null && text.length() <= 2048 ){
															
															if ( last_text == null || !last_text.equals( text )){
																
																last_text = text;
																
																addTorrentsFromClipboard( text );
															}
														}	
													}
												};
												
											while( true ){
												
												try{
													
													Utils.execSWTThread( checker );
														
												}catch( Throwable e ){
													
													Debug.out( e );
													
												}finally{
													
													if ( monitor_thread != new_thread[0] ){
														
														break;
														
													}else{
														
														try{	
															Thread.sleep(500);
															
														}catch( Throwable e ){
															
															Debug.out( e );
															
															break;
														}
													}
												}
											}
										}
									};
									
								monitor_thread.start();
							}
						}else{
							
							monitor_thread 	= null;
							last_text		= null;
							
							if ( clipboard != null ){
								
								clipboard.dispose();
								
								clipboard = null;
							}
						}
					}
				});
			
			new MenuItem(topbarMenu, SWT.SEPARATOR);
			
			final MenuItem itemExport = new MenuItem(topbarMenu, SWT.PUSH);
			Messages.setLanguageText(itemExport,
					"search.export.all");
			itemExport.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					final Shell shell = Utils.findAnyShell();
					
					shell.getDisplay().asyncExec(
						new AERunnable() 
						{
							public void 
							runSupport()
							{
								FileDialog dialog = 
									new FileDialog( shell, SWT.SYSTEM_MODAL | SWT.SAVE );
								
								dialog.setFilterPath( TorrentOpener.getFilterPathData() );
														
								dialog.setText(MessageText.getString("metasearch.export.select.template.file"));
								
								dialog.setFilterExtensions(new String[] {
										"*.vuze",
										"*.vuz",
										org.gudy.azureus2.core3.util.Constants.FILE_WILDCARD
									});
								dialog.setFilterNames(new String[] {
										"*.vuze",
										"*.vuz",
										org.gudy.azureus2.core3.util.Constants.FILE_WILDCARD
									});
								
								String path = TorrentOpener.setFilterPathData( dialog.open());
			
								if ( path != null ){
									
									String lc = path.toLowerCase();
									
									if ( !lc.endsWith( ".vuze" ) && !lc.endsWith( ".vuz" )){
										
										path += ".vuze";
									}
									
									try{
										MetaSearchManagerFactory.getSingleton().getMetaSearch().exportEngines(  new File( path ));
										
									}catch( Throwable e ){
										
										Debug.out( e );
									}
								}
							}
						});	
				}
			});
			
			addMenuAndNonTextChildren((Composite) skinObject.getControl(), topbarMenu);

			skinObject = skin.getSkinObject(SkinConstants.VIEWID_TOOLBAR);
			if (skinObject != null) {
				addMenuAndNonTextChildren((Composite) skinObject.getControl(),
						topbarMenu);
			}
		}
	}

	private void addMenuAndNonTextChildren(Composite parent, Menu menu) {
		parent.setMenu(menu);

		Control[] children = parent.getChildren();
		for (int i = 0; i < children.length; i++) {
			Control control = children[i];
			if (control instanceof Composite) {
				Composite c = (Composite) control;
				addMenuAndNonTextChildren(c, menu);
			} else if (!(control instanceof Text)) {
				control.setMenu(menu);
			}
		}
	}

	private void
	addTorrentsFromClipboard(
		String		text )
	{
		final String[] splitters = {
				"\r\n",
				"\n",
				"\r",
				"\t"
			};

		String[] lines = null;
		
		for (int i = 0; i < splitters.length; i++){
			if (text.indexOf(splitters[i]) >= 0) {
				lines = text.split(splitters[i]);
				break;
			}
		}
		
		if ( lines == null ){
			
			lines = new String[]{ text };
		}
	
		for ( int i=0; i<lines.length; i++ ){
			
			String line = lines[i].trim();
			
			if ( line.startsWith("\"") && line.endsWith("\"")){
				
				if (line.length() < 3){
					
					line = "";
					
				}else{
					
					line = line.substring(1, line.length() - 2);
				}
			}

			if ( UrlUtils.isURL( line )){
				
				Map<String,Object>	options = new HashMap<String, Object>();
				
				options.put( UIFunctions.OTO_HIDE_ERRORS, true );
				
				TorrentOpener.openTorrent( line, options );
			}
		}
	}
	
	
	/**
	 * @param skinObject
	 */
	private void attachSearchBox(SWTSkinObject skinObject) {
		Composite cArea = (Composite) skinObject.getControl();

		final Text text = new Text(cArea, SWT.NONE);
		text.setMessage(MessageText.getString("v3.MainWindow.search.defaultText"));
		FormData filledFormData = Utils.getFilledFormData();
		text.setLayoutData(filledFormData);

		text.setData("ObfusticateImage", new ObfusticateImage() {
			public Image obfusticatedImage(Image image) {
				Point location = Utils.getLocationRelativeToShell(text);
				Point size = text.getSize();
				UIDebugGenerator.obfusticateArea(image, new Rectangle(
						location.x, location.y, size.x, size.y));
				return image;
			}
		});
		
		text.addListener(SWT.Resize, new Listener() {
			Font lastFont = null;
			int	lastHeight = -1;
			
			public void handleEvent(Event event) {
				Text text = (Text) event.widget;

				int h = text.getClientArea().height - 2;
				
				if ( h == lastHeight ){
					return;
				}
				
				lastHeight = h;
				Font font = FontUtils.getFontWithHeight(text.getFont(), null, h);
				if (font != null) {
					text.setFont(font);

					if ( lastFont == null ){
						
						text.addDisposeListener(new DisposeListener() {
							public void widgetDisposed(DisposeEvent e) {
								Text text = (Text) e.widget;
								text.setFont(null);
								Utils.disposeSWTObjects(new Object[] {
									lastFont
								});
							}
						});
						
					}else{
						Utils.disposeSWTObjects(new Object[] {
								lastFont
						});
					}
					
					lastFont = font;
				}
			}
		});
		
		text.setTextLimit(2048);	// URIs can get pretty long...
		
		if (Constants.isWindows) {
  		text.addListener(SWT.MouseDown, new Listener() {
  			public void handleEvent(Event event) {
  				if (event.count == 3) {
  					text.selectAll();
  				}
  			}
  		});
		}

		String tooltip = MessageText.getString( "v3.MainWindow.search.tooltip" );
		
		text.setToolTipText( tooltip );
		
		SWTSkinProperties properties = skinObject.getProperties();
		colorSearchTextBG = properties.getColor("color.search.text.bg");
		colorSearchTextFG = properties.getColor("color.search.text.fg");

		if (colorSearchTextBG != null) {
			text.setBackground(colorSearchTextBG);
		}

		text.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				if (e.stateMask == SWT.MOD1) {

					int key = e.character;
					if (key <= 26 && key > 0) {
						key += 'a' - 1;
					}

					if (key == 'a') {
						text.selectAll();
					}
				}

			}

			public void keyReleased(KeyEvent arg0) {
				// TODO Auto-generated method stub

			}
		});

		text.addListener(SWT.KeyDown, new Listener() {

			public void handleEvent(Event event) {
				Text text = (Text) event.widget;
				if (event.keyCode == SWT.ESC) {
					text.setText("");
					return;
				}
				if (event.character == SWT.CR) {
					uiFunctions.doSearch(text.getText());
				}
			}
		});

		SWTSkinObject searchGo = skin.getSkinObject("search-go");
		if (searchGo != null) {
			SWTSkinButtonUtility btnGo = new SWTSkinButtonUtility(searchGo);
			btnGo.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility,
						SWTSkinObject skinObject, int stateMask) {
					String sSearchText = text.getText().trim();
					uiFunctions.doSearch(sSearchText);
				}
			});
		}

		SWTSkinObject so = skin.getSkinObject("search-dropdown");
		if (so != null) {
			SWTSkinButtonUtility btnSearchDD = new SWTSkinButtonUtility(so);
			btnSearchDD.setTooltipID( "v3.MainWindow.search.tooltip" );
			btnSearchDD.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility,
						SWTSkinObject skinObject, int stateMask) {
					String sSearchText = text.getText().trim();
					uiFunctions.doSearch(sSearchText);
				}
			});
		}
	}

	/**
	 * 
	 */
	private void updateMapTrackUsage(String sTabID) {
		//System.out.println("UPDATE: " + sTabID);
		if (mapTrackUsage != null) {
			mapTrackUsage_mon.enter();
			try {
				if (lCurrentTrackTime > 1000) {
					addUsageStat(sTabID, lCurrentTrackTime);
					//System.out.println("UPDATE: " + sTabID + ";" + newLength);
				}

				if (lCurrentTrackTimeIdle > 1000) {
					String id = "idle-" + sTabID;
					addUsageStat(id, lCurrentTrackTimeIdle);
				}
			} finally {
				mapTrackUsage_mon.exit();
			}
		}

		lCurrentTrackTime = 0;
		lCurrentTrackTimeIdle = 0;
	}

	private static void addUsageStat(String id, long value) {
		if (id == null) {
			return;
		}
		if (id.length() > 150) {
			id = id.substring(0, 150);
		}
		if (mapTrackUsage != null) {
			mapTrackUsage_mon.enter();
			try {
				List currentLength = mapTrackUsage.get(id);
				if (currentLength == null) {
					currentLength = new ArrayList();
					currentLength.add(1);
					currentLength.add(value / 100);
				} else {
					List oldList = currentLength;
					currentLength = new ArrayList();
					currentLength.add(((Number) oldList.get(0)).longValue() + 1);
					currentLength.add(((Number) oldList.get(1)).longValue() + (value / 1000));
				}
				mapTrackUsage.put(id, currentLength);
			} finally {
				mapTrackUsage_mon.exit();
			}
		}
	}

	public Shell
	getShell()
	{
		return( shell );
	}
	
	public UISWTInstanceImpl getUISWTInstanceImpl() {
		return uiSWTInstanceImpl;
	}

	public MainStatusBar getMainStatusBar() {
		return statusBar;
	}

	public boolean isVisible(int windowElement) {
		if (windowElement == IMainWindow.WINDOW_ELEMENT_TOOLBAR) {
			SWTSkinObject skinObject = skin.getSkinObject(SkinConstants.VIEWID_TOOLBAR);
			if (skinObject != null) {
				return skinObject.isVisible();
			}
		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_TOPBAR) {
			SWTSkinObject skinObject = skin.getSkinObject(SkinConstants.VIEWID_PLUGINBAR);
			if (skinObject != null) {
				return skinObject.isVisible();
			}
		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_STATUSBAR) {
			//TODO:
		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_MENU) {
			//TODO:
		}

		return false;
	}

	public void setVisible(int windowElement, boolean value) {
		if (windowElement == IMainWindow.WINDOW_ELEMENT_TOOLBAR) {
			SWTSkinUtils.setVisibility(skin, "IconBar.enabled",
					SkinConstants.VIEWID_TOOLBAR, value, true, true);
		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_TOPBAR) {

			SWTSkinUtils.setVisibility(skin, SkinConstants.VIEWID_PLUGINBAR
					+ ".visible", SkinConstants.VIEWID_PLUGINBAR, value, true, true);

		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_STATUSBAR) {
			//TODO:
		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_MENU) {
			//TODO:
		}

	}

	public Rectangle getMetrics(int windowElement) {
		if (windowElement == IMainWindow.WINDOW_ELEMENT_TOOLBAR) {
		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_TOPBAR) {

			SWTSkinObject skinObject = skin.getSkinObject(SkinConstants.VIEWID_PLUGINBAR);
			if (skinObject != null) {
				return skinObject.getControl().getBounds();
			}

		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_STATUSBAR) {

			return statusBar.getBounds();

		} else if (windowElement == IMainWindow.WINDOW_CLIENT_AREA) {

			return shell.getClientArea();

		} else if (windowElement == IMainWindow.WINDOW_CONTENT_DISPLAY_AREA) {

			Rectangle r = getMetrics(IMainWindow.WINDOW_CLIENT_AREA);
			r.height -= getMetrics(IMainWindow.WINDOW_ELEMENT_TOPBAR).height;
			r.height -= getMetrics(IMainWindow.WINDOW_ELEMENT_TOOLBAR).height;
			r.height -= getMetrics(IMainWindow.WINDOW_ELEMENT_STATUSBAR).height;
			return r;

		}

		return new Rectangle(0, 0, 0, 0);
	}

	private SWTSkin getSkin() {
		return skin;
	}

	public boolean isReady() {
		return isReady;
	}

	public Image generateObfusticatedImage() {
		// 3.2 TODO: Obfusticate! (esp advanced view)

		Rectangle shellBounds = shell.getBounds();
		Rectangle shellClientArea = shell.getClientArea();
		Image fullImage = new Image(display, shellBounds.width, shellBounds.height);
		Image subImage = new Image(display, shellClientArea.width, shellClientArea.height);

		GC gc = new GC(display);
		try {
			gc.copyArea(fullImage, shellBounds.x, shellBounds.y);
		} finally {
			gc.dispose();
		}
		GC gcShell = new GC(shell);
		try {
			gcShell.copyArea(subImage, 0, 0);
		} finally {
			gcShell.dispose();
		}
		GC gcFullImage = new GC(fullImage);
		try {
			Point location = shell.toDisplay(0, 0);
			gcFullImage.drawImage(subImage, location.x - shellBounds.x, location.y
					- shellBounds.y);
		} finally {
			gcFullImage.dispose();
		}
		subImage.dispose();

		Control[] children = shell.getChildren();
		for (int i = 0; i < children.length; i++) {
			Control control = children[i];
			SWTSkinObject so = (SWTSkinObject) control.getData("SkinObject");
			if (so instanceof ObfusticateImage) {
				ObfusticateImage oi = (ObfusticateImage) so;
				oi.obfusticatedImage(fullImage);
			}
		}

		return fullImage;
	}

	// @see com.aelitis.azureus.ui.mdi.MdiListener#mdiEntrySelected(com.aelitis.azureus.ui.mdi.MdiEntry, com.aelitis.azureus.ui.mdi.MdiEntry)
	public void mdiEntrySelected(MdiEntry newEntry,
			MdiEntry oldEntry) {
		if (newEntry == null) {
			return;
		}

		if (mapTrackUsage != null && oldEntry != null) {
			oldEntry.removeListener(this);

			String id2 = null;
			MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
			if (mdi != null) {
				id2 = oldEntry.getLogID();
			}
			if (id2 == null) {
				id2 = oldEntry.getId();
			}

			updateMapTrackUsage(id2);
		}

		if (mapTrackUsage != null) {
			newEntry.addListener(this);
		}
	}

	// @see com.aelitis.azureus.ui.swt.views.skin.sidebar.MdiLogIdListener#sidebarLogIdChanged(com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarEntrySWT, java.lang.String, java.lang.String)
	public void mdiEntryLogIdChanged(MdiEntry sideBarEntrySWT, String oldID,
			String newID) {
		if (oldID == null) {
			oldID = "null";
		}
		updateMapTrackUsage(oldID);
	}

	// @see org.gudy.azureus2.core3.util.AEDiagnosticsEvidenceGenerator#generate(org.gudy.azureus2.core3.util.IndentWriter)
	public void generate(IndentWriter writer) {
		writer.println("SWT UI");

		try {
			writer.indent();

			TableColumnManager.getInstance().generateDiagnostics(writer);
		} finally {

			writer.exdent();
		}
	}

	public void setSelectedLanguageItem() {
		Messages.updateLanguageForControl(shell);

		if (systemTraySWT != null) {
			systemTraySWT.updateLanguage();
		}

		if (statusBar != null) {
			statusBar.refreshStatusText();
		}

		// download basket

		skin.triggerLanguageChange();

		if (statusBar != null) {
			statusBar.updateStatusText();
		}

		if (menu != null) {
			MenuFactory.updateMenuText(menu.getMenu(IMenuConstants.MENU_ID_MENU_BAR));
		}
	}

	public IMainMenu getMainMenu() {
		return menu;
	}

	public void updateUI() {
		//if (shell != null) {
		//	Utils.setShellIcon(shell);
		//}
	}

	public String getUpdateUIName() {
		return "MainWindow";
	}

}