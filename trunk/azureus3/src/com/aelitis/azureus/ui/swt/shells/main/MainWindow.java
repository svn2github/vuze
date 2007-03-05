/*
 * Created on May 29, 2006 2:07:38 PM
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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
 */
package com.aelitis.azureus.ui.swt.shells.main;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.associations.AssociationChecker;
import org.gudy.azureus2.ui.swt.mainwindow.MainStatusBar;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;
import org.gudy.azureus2.ui.swt.mainwindow.SplashWindow;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTInstanceImpl;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.systray.SystemTraySWT;
import org.json.JSONObject;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.messenger.ClientMessageContext;
import com.aelitis.azureus.core.messenger.PlatformMessenger;
import com.aelitis.azureus.core.messenger.config.PlatformConfigMessenger;
import com.aelitis.azureus.core.messenger.config.PlatformRatingMessenger;
import com.aelitis.azureus.core.messenger.config.PlatformRatingMessenger.GetRatingReplyListener;
import com.aelitis.azureus.core.torrent.GlobalRatingUtils;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.Initializer;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.browser.BrowserContext;
import com.aelitis.azureus.ui.swt.browser.listener.DisplayListener;
import com.aelitis.azureus.ui.swt.browser.listener.TorrentListener;
import com.aelitis.azureus.ui.swt.browser.msg.BrowserMessage;
import com.aelitis.azureus.ui.swt.search.network.NetworkSearch;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.utils.*;
import com.aelitis.azureus.ui.swt.views.skin.*;
import com.aelitis.azureus.util.*;
import com.aelitis.azureus.util.Constants;

import org.gudy.azureus2.plugins.PluginEvent;

/**
 * @author TuxPaper
 * @created May 29, 2006
 *
 */
public class MainWindow
	implements SWTSkinTabSetListener
{

	private static final LogIDs LOGID = LogIDs.GUI;

	protected Shell shell;

	private final Display display;

	private final AzureusCore core;

	private SWTSkin skin;

	private org.gudy.azureus2.ui.swt.mainwindow.MainWindow oldMainWindow;

	private org.gudy.azureus2.ui.swt.mainwindow.MainMenu oldMainMenu;

	private MainMenu menu;

	private UISWTInstanceImpl uiSWTInstanceImpl;

	private UIFunctionsImpl uiFunctions;

	private SystemTraySWT systemTraySWT;

	private Map mapTrackUsage;

	private AEMonitor mapTrackUsage_mon = new AEMonitor("mapTrackUsage");

	private long lCurrentTrackTime = 0;

	private boolean disposedOrDisposing;

	private Object[] dms_Startup;

	protected boolean isReady = false;

	public static void main(String args[]) {
		Initializer.main(new String[0]);
		//org.gudy.azureus2.ui.swt.Main.main(args);
	}

	/**
	 * 
	 */
	public MainWindow(AzureusCore core, Display display, final SplashWindow splash) {
		this.core = core;
		this.display = display;
		disposedOrDisposing = false;

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				createWindow(splash);
				if (splash != null) {
					splash.closeSplash();
				}
			}
		});

		// When a download is added, check for new meta data and
		// un-"wait state" the rating
		// TODO: smart refreshing of meta data ("Refresh On" attribute)
		GlobalManager gm = core.getGlobalManager();
		dms_Startup = gm.getDownloadManagers().toArray();
		gm.addListener(new GlobalManagerListener() {

			public void seedingStatusChanged(boolean seeding_only_mode) {
			}

			public void downloadManagerRemoved(DownloadManager dm) {
			}

			public void downloadManagerAdded(final DownloadManager dm) {
				downloadAdded(dm);
			}

			public void destroyed() {
			}

			public void destroyInitiated() {
			}

		}, false);
	}

	private void processStartupDMS() {
		// must be in a new thread because we don't want to block
		// initilization or any other add listeners
		AEThread thread = new AEThread("v3.mw.dmAdded", true) {
			public void runSupport() {
				long startTime = System.currentTimeMillis();
				if (dms_Startup == null || dms_Startup.length == 0) {
					return;
				}

				for (int i = 0; i < dms_Startup.length; i++) {
					DownloadManager dm = (DownloadManager) dms_Startup[i];
					downloadAdded(dm);
				}

				dms_Startup = null;

				System.out.println("psDMS " + (SystemTime.getCurrentTime() - startTime)
						+ "ms");
			}
		};
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
	}

	private void downloadAdded(final DownloadManager dm) {
		final TOTorrent torrent = dm.getTorrent();
		if (torrent == null) {
			return;
		}

		String hash = null;
		try {
			hash = torrent.getHashWrapper().toBase32String();
		} catch (TOTorrentException e) {
			Debug.out(e);
		}

		String title = PlatformTorrentUtils.getContentTitle(torrent);
		if (title != null && title.length() > 0
				&& dm.getDownloadState().getDisplayName() == null) {
			dm.getDownloadState().setDisplayName(title);
		}

		// Show a popup when user adds a download
		// if it wasn't added recently, it's not a new download
		if (skin != null
				&& PlatformTorrentUtils.isContent(torrent)
				&& SystemTime.getCurrentTime()
						- dm.getDownloadState().getLongParameter(
								DownloadManagerState.PARAM_DOWNLOAD_ADDED_TIME) < 10000
				&& !PublishUtils.isPublished(dm)) {
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					SWTSkinTabSet tabSetMain = skin.getTabSet(SkinConstants.TABSET_MAIN);
					if (tabSetMain != null
							&& !tabSetMain.getActiveTab().getSkinObjectID().equals(
									"maintabs.home")) {
						Display current = Display.getCurrent();
						// checking focusControl for null doesn't really work
						// Preferably, we'd check to see if the app has the OS' focus
						// and not display the popup when it doesn't
						if (current != null && current.getFocusControl() != null
								&& !MessageBoxShell.isOpen()) {
							int ret = MessageBoxShell.open(shell,
									MessageText.getString("HomeReminder.title"),
									MessageText.getString("HomeReminder.text", new String[] {
										dm.getDisplayName()
									}), new String[] {
										MessageText.getString("Button.ok"),
										MessageText.getString("HomeReminder.gohome")
									}, 0, "downloadinhome",
									MessageText.getString("MessageBoxWindow.nomoreprompting"),
									false, 15000);

							if (ret == 1) {
								tabSetMain.setActiveTab("maintabs.home");
							}
						}
					}
				}
			});
		}
		final String fHash = hash;

		if (PlatformTorrentUtils.isContent(torrent)) {
			if (PlatformTorrentUtils.getUserRating(torrent) == -2) {
				PlatformTorrentUtils.setUserRating(torrent, -1);
				PlatformRatingMessenger.getUserRating(new String[] {
					PlatformRatingMessenger.RATE_TYPE_CONTENT
				}, new String[] {
					hash
				}, 5000, new GetRatingReplyListener() {
					public void replyReceived(String replyType,
							PlatformRatingMessenger.GetRatingReply reply) {
						if (replyType.equals(PlatformMessenger.REPLY_RESULT)) {
							long rating = reply.getRatingValue(fHash,
									PlatformRatingMessenger.RATE_TYPE_CONTENT);
							if (rating >= -1) {
								PlatformTorrentUtils.setUserRating(torrent, (int) rating);
							}
						}

					}

					public void messageSent() {
					}
				});
			}

			long now = SystemTime.getCurrentTime();
			long mdRefreshOn = PlatformTorrentUtils.getMetaDataRefreshOn(torrent);
			if (mdRefreshOn < now) {
				PlatformTorrentUtils.log("addDM, update MD NOW");
				PlatformTorrentUtils.updateMetaData(torrent, 5000);
			} else {
				PlatformTorrentUtils.log("addDM, update MD on " + new Date(mdRefreshOn));
				SimpleTimer.addEvent("Update MD", mdRefreshOn,
						new TimerEventPerformer() {
							public void perform(TimerEvent event) {
								PlatformTorrentUtils.updateMetaData(torrent, 15000);
							}
						});
			}

			long grRefreshOn = GlobalRatingUtils.getRefreshOn(torrent);
			if (grRefreshOn <= now) {
				GlobalRatingUtils.updateFromPlatform(torrent, 5000);
			} else {
				SimpleTimer.addEvent("Update G.Rating", grRefreshOn,
						new TimerEventPerformer() {
							public void perform(TimerEvent event) {
								GlobalRatingUtils.updateFromPlatform(torrent, 15000);
							}
						});
			}
		}
	}

	/**
	 * @param splash 
	 * 
	 */
	protected void createWindow(SplashWindow splash) {
		long startTime = SystemTime.getCurrentTime();

		uiFunctions = new UIFunctionsImpl(this);
		UIFunctionsManager.setUIFunctions(uiFunctions);

		Utils.disposeComposite(shell);

		splash.reportPercent(60);

		// XXX Temporary.  We'll use our own images
		ImageRepository.loadImagesForSplashWindow(display);
		ImageRepository.loadImages(display);

		shell = new Shell(display, SWT.SHELL_TRIM);
		shell.setData("class", this);
		shell.setText("Azureus");
		Utils.setShellIcon(shell);
		Utils.linkShellMetricsToConfig(shell, "window");

		splash.reportPercent(70);

		skin = SWTSkinFactory.getInstance();

		initSkinListeners();

		// attach the UI to plugins
		// Must be done before initializing views, since plugins may register
		// table columns and other objects
		uiSWTInstanceImpl = new UISWTInstanceImpl(core);
		uiSWTInstanceImpl.init();

		skin.initialize(shell, "main.shell");

		splash.reportPercent(80);

		System.out.println("skin init took "
				+ (SystemTime.getCurrentTime() - startTime) + "ms");
		startTime = SystemTime.getCurrentTime();

		menu = new MainMenu(skin, shell);

		if (org.gudy.azureus2.core3.util.Constants.isOSX) {
			try {

				Class ehancerClass = Class.forName("org.gudy.azureus2.ui.swt.osx.CarbonUIEnhancer");

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

			Listener toggleListener = new Listener() {
				public void handleEvent(Event event) {
					boolean bVisible = (event.type == SWT.Expand);
					menu.setVisibility("SearchBar.visible", "searchbar", bVisible);
				}
			};
			shell.addListener(SWT.Expand, toggleListener);
			shell.addListener(SWT.Collapse, toggleListener);
		}

		System.out.println("createWindow init took "
				+ (SystemTime.getCurrentTime() - startTime) + "ms");
		startTime = SystemTime.getCurrentTime();

		splash.reportPercent(90);

		skin.layout();

		System.out.println("skin layout took "
				+ (SystemTime.getCurrentTime() - startTime) + "ms");
		startTime = SystemTime.getCurrentTime();

		try {
			Utils.createTorrentDropTarget(shell, true);
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
				if (systemTraySWT != null
						&& COConfigurationManager.getBooleanParameter("Enable System Tray")
						&& COConfigurationManager.getBooleanParameter("Close To Tray")) {

					minimizeToTray(event);
				} else {
					event.doit = dispose(false, false);
				}
			}

			public void shellIconified(ShellEvent event) {
				if (systemTraySWT != null
						&& COConfigurationManager.getBooleanParameter("Enable System Tray")
						&& COConfigurationManager.getBooleanParameter("Minimize To Tray")) {

					minimizeToTray(event);
				}
			}

		});

		shell.addListener(SWT.Deiconify, new Listener() {
			public void handleEvent(Event e) {
				if (Constants.isOSX
						&& COConfigurationManager.getBooleanParameter("Password enabled")) {
					e.doit = false;
					shell.setVisible(false);
					PasswordWindow.showPasswordWindow(display);
				}
			}
		});

		ExternalStimulusHandler.addListener(new ExternalStimulusListener() {
			public boolean receive(String name, String value) {
				try {
					if (!name.equals("AZMSG")) {
						return false;
					}
					
					ClientMessageContext context = PlatformMessenger.getClientMessageContext();
					if (context == null) {
						return false;
					}
					BrowserMessage browserMsg = new BrowserMessage(value);
					if (browserMsg.getOperationId().equals(DisplayListener.OP_OPEN_URL)) {
						JSONObject decodedObject = browserMsg.getDecodedObject();
						String url = JSONUtils.getJSONString(decodedObject, "url", null);
						if (decodedObject.has("target")
								&& !PlatformConfigMessenger.isURLBlocked(url)) {

							// implicit bring to front
							final UIFunctions functions = UIFunctionsManager.getUIFunctions();
							if (functions != null) {
								functions.bringToFront();
							}

							// this is actually sync.. so we could add a completion listener
							// and return the boolean result if we wanted/needed
							context.getMessageDispatcher().dispatch(browserMsg);
							context.getMessageDispatcher().resetSequence();
							return true;
						}
					} else if (browserMsg.getOperationId().equals("is-ready")) {
						// The platform needs to know when it can call open-url, and it
						// determines this by the is-ready function
						return isReady;
					}
				} catch (Exception e) {
					Debug.out(e);
				}
				return false;
			}
		});

		initWidgets();

		System.out.println("skin widgets init took "
				+ (SystemTime.getCurrentTime() - startTime) + "ms");
		startTime = SystemTime.getCurrentTime();

		showMainWindow();
		System.out.println("shell.open took "
				+ (SystemTime.getCurrentTime() - startTime) + "ms");
		startTime = SystemTime.getCurrentTime();

		processStartupDMS();

		System.out.println("processStartupDMS took "
				+ (SystemTime.getCurrentTime() - startTime) + "ms");
		startTime = SystemTime.getCurrentTime();
	}

	public boolean dispose(boolean bForRestart, boolean bCloseAlreadyInProgress) {
		if (disposedOrDisposing) {
			return true;
		}

		isReady = false;

		disposedOrDisposing = true;
		if (oldMainWindow != null) {
			boolean res = oldMainWindow.dispose(bForRestart, bCloseAlreadyInProgress);
			oldMainWindow = null;

			if (res == false) {
				return false;
			}
		} else {
			if (!UIExitUtilsSWT.canClose(core.getGlobalManager(), bForRestart)) {
				return false;
			}

			UIExitUtilsSWT.uiShutdown();
		}

		if (systemTraySWT != null) {
			systemTraySWT.dispose();
		}

		if (!SWTThread.getInstance().isTerminated()) {
			SWTThread.getInstance().getInitializer().stopIt(bForRestart, false);
		}

		mapTrackUsage_mon.enter();
		try {
			if (mapTrackUsage != null) {
				SWTSkinTabSet tabSetMain = skin.getTabSet(SkinConstants.TABSET_MAIN);
				if (tabSetMain != null) {
					updateMapTrackUsage(tabSetMain.getActiveTab().getSkinObjectID());
				}

				FileUtil.writeResilientFile(new File(SystemProperties.getUserPath(),
						"timingstats.dat"), mapTrackUsage);
			}
		} finally {
			mapTrackUsage_mon.exit();
		}

		return true;
	}

	private void showMainWindow() {
		boolean isOSX = org.gudy.azureus2.core3.util.Constants.isOSX;
		// No tray access on OSX yet
		boolean bEnableTray = COConfigurationManager.getBooleanParameter("Enable System Tray")
				&& (!isOSX || SWT.getVersion() > 3300);
		boolean bPassworded = COConfigurationManager.getBooleanParameter(
				"Password enabled", false);
		boolean bStartMinimize = bEnableTray
				&& (bPassworded || COConfigurationManager.getBooleanParameter(
						"Start Minimized", false));

		if (!bStartMinimize) {
			shell.layout();
			shell.open();
			if (!isOSX) {
				shell.forceActive();
			}
		} else if (isOSX) {
			shell.setMinimized(true);
			shell.setVisible(true);
		}

		shell.layout(true, true);

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
					setVisible(true); // invokes password
				}
			}
		}

		// check file associations   
		AssociationChecker.checkAssociations();

		core.triggerLifeCycleComponentCreated(uiFunctions);
		core.getPluginManager().firePluginEvent(
				PluginEvent.PEV_INITIALISATION_UI_COMPLETES);

		boolean hasInComplete = false;
		Object[] dms = core.getGlobalManager().getDownloadManagers().toArray();
		for (int i = 0; i < dms.length; i++) {
			DownloadManager dm = (DownloadManager) dms[i];
			if (!dm.getAssumedComplete()) {
				hasInComplete = true;
				break;
			}
		}

		String startTab = hasInComplete ? "maintabs.home" : "maintabs.browse";
		SWTSkinTabSet tabSet = skin.getTabSet(SkinConstants.TABSET_MAIN);
		if (tabSet != null) {
			COConfigurationManager.setBooleanDefault("v3.Start Advanced", false);
			if (COConfigurationManager.getBooleanParameter("v3.Start Advanced")) {
				startTab = "maintabs.advanced";
			}
			tabSet.setActiveTab(startTab);
		}

		isReady = true;
	}

	public void setVisible(final boolean visible) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (visible && !shell.getVisible()) {
					if (COConfigurationManager.getBooleanParameter("Password enabled",
							false)) {
						if (!PasswordWindow.showPasswordWindow(display)) {
							shell.setVisible(false);
							return;
						}
					}
				}

				shell.setVisible(visible);
				if (visible) {
					shell.forceActive();
					shell.setMinimized(false);
				}

			}
		});
	}

	private void minimizeToTray(ShellEvent event) {
		boolean isOSX = org.gudy.azureus2.core3.util.Constants.isOSX;
		//Added this test so that we can call this method with null parameter.
		if (event != null) {
			event.doit = false;
		}
		if (isOSX) {
			shell.setMinimized(true);
		} else {
			shell.setVisible(false);
		}
		MinimizedWindow.setAllVisible(true);
	}

	/**
	 * Associates every view ID that we use to a class, and creates the class
	 * on first EVENT_SHOW.
	 */
	private void initSkinListeners() {
		final Map views = new HashMap();

		// List of all views ids we use
		views.put("minibrowse", MiniBrowse.class);
		views.put("minidownload-list", MiniDownloadList.class);
		views.put("minirecent-list", MiniRecentList.class);

		views.put("browse", Browse.class);

		views.put("manage-dl-list", ManageDlList.class);
		views.put("manage-cd-list", ManageCdList.class);

		views.put("my-media-list", MediaList.class);

		views.put("publish", Publish.class);

		SWTSkinObjectListener l = new SWTSkinObjectListener() {
			public Object eventOccured(SWTSkinObject skinObject, int eventType,
					Object params) {
				if (eventType == EVENT_SHOW) {
					Object key = skinObject.getViewID();
					Class cla = (Class) views.get(key);
					if (cla != null) {
						try {
							SkinView skinView = (SkinView) cla.newInstance();
							SkinViewManager.add(skinView);
							skinObject.addListener(skinView);
							skinView.eventOccured(skinObject, eventType, params);
							views.remove(key);
						} catch (InstantiationException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IllegalAccessException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				return null;
			}
		};

		for (Iterator iterator = views.keySet().iterator(); iterator.hasNext();) {
			String viewID = (String) iterator.next();
			skin.addListener(viewID, l);
		}
	}

	/**
	 * 
	 */
	private void initWidgets() {
		SWTSkinObject skinObject;

		skinObject = skin.getSkinObject("statusbar");
		if (skinObject != null) {
			final Composite cArea = (Composite) skinObject.getControl();

			final MainStatusBar statusBar = new MainStatusBar();
			Composite composite = statusBar.initStatusBar(core,
					core.getGlobalManager(), display, cArea);

			composite.setLayoutData(Utils.getFilledFormData());

			UIUpdater uiUpdater = UIUpdaterFactory.getInstance();
			// XXX Could just make MainStatusBar implement UIUpdatable
			uiUpdater.addUpdater(new UIUpdatable() {
				public String getUpdateUIName() {
					return "StatusBar";
				}

				public void updateUI() {
					statusBar.refreshStatusBar();
				}
			});
		}

		skinObject = skin.getSkinObject("search-text");
		if (skinObject != null) {
			attachSearchBox(skinObject);
		}

		shell.layout(true, true);

		SWTSkinTabSet tabSet = skin.getTabSet(SkinConstants.TABSET_MAIN);
		if (tabSet != null) {
			tabSet.addListener(this);
		}
	}

	/**
	 * @param skinObject
	 */
	private void attachSearchBox(SWTSkinObject skinObject) {
		Composite cArea = (Composite) skinObject.getControl();

		final Text text = new Text(cArea, SWT.NONE);
		text.setLayoutData(Utils.getFilledFormData());
		final String sDefault = MessageText.getString("MainWindow.v3.search.defaultText");
		text.setText(sDefault);
		text.setForeground(ColorCache.getColor(text.getDisplay(), 127, 127, 127));
		text.setBackground(ColorCache.getColor(text.getDisplay(), 255, 255, 255));
		text.selectAll();
		text.addMouseListener(new MouseListener() {

			public void mouseUp(MouseEvent e) {
			}

			public void mouseDown(MouseEvent e) {
				Text text = (Text) e.widget;
				if (text.getText().equals(sDefault)) {
					text.setForeground(ColorCache.getColor(text.getDisplay(), 0, 0, 0));
					text.setText("");
				}
			}

			public void mouseDoubleClick(MouseEvent e) {
			}
		});

		text.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				// TODO Auto-generated method stub

			}

			public void widgetDefaultSelected(SelectionEvent e) {
				// Open a new search result view

				// Search Results are placed in a Search Results tab in the 
				// "Browse Content" tab view. 

				Text text = (Text) e.widget;
				String sSearchText = text.getText();

				doSearch(sSearchText);

			}

		});

		SWTSkinObject searchGo = skin.getSkinObject("search-go");
		if (searchGo != null) {
			SWTSkinButtonUtility btnGo = new SWTSkinButtonUtility(searchGo);
			btnGo.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					String sSearchText = text.getText().trim();
					if (!sSearchText.equals(sDefault) && sSearchText.length() > 0) {
						doSearch(sSearchText);
					}
				}
			});
		}
	}

	/**
	 * @param searchText
	 */
	protected void doSearch(String sSearchText) {
		// Switch to browse tab
		skin.setActiveTab(SkinConstants.TABSET_MAIN, "maintabs.browse");

		String sURL = Constants.URL_PREFIX + Constants.URL_ADD_SEARCH
				+ UrlUtils.encode(sSearchText) + "&" + Constants.URL_SUFFIX;
		//		String sURL = Constants.URL_PREFIX
		//				+ "app?page=content%2FBrowse&service=external&sp=X&sp=X&sp=S"
		//				+ UrlUtils.encode(" " + sSearchText + " ") + "&sp=X&sp=X&"
		//				+ Constants.URL_SUFFIX;
		System.out.println(sURL);

		UIFunctions functions = UIFunctionsManager.getUIFunctions();
		if (functions != null) {
			functions.viewURL(sURL, "browse", 0, 0, false);
			return;
		}

		// below is the old impementation of search, which creates a new tab
		// and browser for each search.

		// Get Search Results tab (which contains a tabset of searched terms),
		// create if needed
		SWTSkinObject skinObject = skin.getSkinObject("browse-tabs");
		if (skinObject == null) {
			System.err.println("no browse-tabs");
			return;
		}

		SWTSkinTabSet tabSetSearch = skin.getTabSet(SWTSkinTabSet.getTabSetID(skin,
				skinObject, "search-tab"));

		SWTSkinObjectTab searchTab = null;
		if (tabSetSearch != null) {
			SWTSkinObjectTab[] tabs = tabSetSearch.getTabs();
			if (tabs.length > 0) {
				searchTab = tabs[tabs.length - 1];
			}
		}

		String sTabID = "internal.tab.searchresults";
		if (searchTab == null || !searchTab.getSkinObjectID().equals(sTabID)) {
			// Create search results tab
			searchTab = skin.createTab(sTabID, "search-tab", skinObject);

			// Attach the new tab to the previous one
			Control currentControl = searchTab.getControl();
			FormData formData = (FormData) currentControl.getLayoutData();
			if (formData == null) {
				formData = new FormData();
			}
			formData.right = new FormAttachment(100, 0);
			currentControl.setLayoutData(formData);

			currentControl.getParent().layout(true);

			tabSetSearch = searchTab.getTabset();
		}

		tabSetSearch.setActiveTab(sTabID);

		SWTSkinObject searchResultsTabsView = skin.getSkinObject("search-results-tabs");
		if (searchResultsTabsView == null) {
			System.err.println("searchResultsTabs null");
			return;
		}

		sTabID = "internal.tab.searchresults."
				+ Integer.toHexString(sSearchText.hashCode());
		SWTSkinObjectTab tabSearchResult = null;

		String sTabSetID = SWTSkinTabSet.getTabSetID(skin, searchResultsTabsView,
				"tab");
		SWTSkinTabSet tabSetSearchResults = skin.getTabSet(sTabSetID);
		SWTSkinObject lastTab = null;
		if (tabSetSearchResults != null) {
			SWTSkinObject[] tabs = tabSetSearchResults.getTabs();
			if (tabs.length > 0) {
				lastTab = tabs[tabs.length - 1];
			}

			tabSearchResult = tabSetSearchResults.getTab(sTabID);
		}

		if (tabSearchResult == null) {
			// Create tab specifically for this search

			tabSearchResult = skin.createTab(sTabID, "tab", searchResultsTabsView);

			if (tabSetSearchResults == null) {
				tabSetSearchResults = skin.getTabSet(sTabSetID);
			}

			Control currentControl = tabSearchResult.getControl();

			// Attach the new tab to the previous one
			if (lastTab != null) {
				FormData formData = (FormData) currentControl.getLayoutData();
				if (formData == null) {
					formData = new FormData();
				}
				formData.left = new FormAttachment(lastTab.getControl(), 2);
				currentControl.setLayoutData(formData);
			}

			// Set the new tab's text
			SWTSkinObject tabText = skin.getSkinObject("search-result-tab-text",
					tabSearchResult);
			if (tabText instanceof SWTSkinObjectText) {
				((SWTSkinObjectText) tabText).setText(sSearchText);
			}

			searchResultsTabsView.getControl().getParent().layout(true, true);

			String[] activeWidgetIDs = tabSearchResult.getActiveWidgetIDs();
			SWTSkinObject searchResultsView = skin.getSkinObject("search-results-view");
			if (activeWidgetIDs.length == 1 && searchResultsView != null) {
				String sContentConfigID = activeWidgetIDs[0];

				String sContentID = "internal.tab.searchresults.content."
						+ Integer.toHexString(sSearchText.hashCode());
				SWTSkinObject searchResultsContent = skin.createSkinObject(sContentID,
						sContentConfigID, searchResultsView);

				tabSearchResult.setActiveWidgets(new SWTSkinObject[] {
					searchResultsContent
				});

				SWTSkinObject searchResultsContentG = skin.getSkinObject(
						"search-results-google", searchResultsContent);
				if (searchResultsContentG != null) {
					Composite cArea = (Composite) searchResultsContentG.getControl();

					final Browser browser = new Browser(cArea, SWT.NONE);
					final ClientMessageContext context = new BrowserContext("search",
							browser, null);
					context.addMessageListener(new TorrentListener(core));
					browser.setLayoutData(Utils.getFilledFormData());
					//					browser.setUrl("http://www.google.com/search?num=5&q="
					//		+ UrlUtils.encode(sSearchText + " torrent"));
					browser.setUrl(sURL);
					cArea.layout(true, true);
				}

				SWTSkinObject searchResultsContentN = skin.getSkinObject(
						"search-results-network", searchResultsContent);
				if (searchResultsContentN != null) {
					Composite cArea = (Composite) searchResultsContentN.getControl();

					final Browser browser = new Browser(cArea, SWT.NONE);
					final ClientMessageContext context = new BrowserContext("search",
							browser, null);

					browser.setLayoutData(Utils.getFilledFormData());

					NetworkSearch.search(core, sSearchText, browser);

					cArea.layout(true, true);
				}
			}
		}

		// activate!
		if (tabSetSearchResults != null) {
			tabSetSearchResults.setActiveTab(sTabID);
		}
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinTabSetListener#tabChanged(com.aelitis.azureus.ui.swt.skin.SWTSkinTabSet, java.lang.String, java.lang.String)
	public void tabChanged(SWTSkinTabSet tabSet, String oldTabID, String newTabID) {
		updateMapTrackUsage(oldTabID);

		if (tabSet.getID().equals(SkinConstants.TABSET_MAIN)) {
			// TODO: Don't use internal skin IDs.  Skin needs to provide an ViewID
			//        we can query (or is passed in)
			if (newTabID.equals("maintabs.advanced")) {
				createOldMainWindow();
				if (oldMainMenu != null) {
					oldMainMenu.linkMenuBar(shell);
				}
			} else if (newTabID.equals("maintabs.home")
					&& oldTabID.equals("maintabs.home")) {
				SkinView view = SkinViewManager.get(MiniBrowse.class);
				if (view instanceof MiniBrowse) {
					((MiniBrowse) view).restart();
				}
			} else if (newTabID.equals("maintabs.browse")
					&& oldTabID.equals("maintabs.browse")) {
				SkinView view = SkinViewManager.get(Browse.class);
				if (view instanceof Browse) {
					((Browse) view).restart();
				}
			} else if (newTabID.equals("maintabs.publish")
					&& oldTabID.equals("maintabs.publish")) {
				SkinView view = SkinViewManager.get(Publish.class);
				if (view instanceof Publish) {
					((Publish) view).restart();
				}
			}

			if (!newTabID.equals("maintabs.advanced") && oldMainMenu != null) {
				menu.linkMenuBar(shell);
			}
		}
	}

	/**
	 * 
	 */
	private void updateMapTrackUsage(String sTabID) {
		if (mapTrackUsage != null) {
			mapTrackUsage_mon.enter();
			try {
				Long currentLength = (Long) mapTrackUsage.get(sTabID);
				if (currentLength == null) {
					currentLength = new Long(lCurrentTrackTime);
				} else {
					currentLength = new Long(currentLength.longValue()
							+ lCurrentTrackTime);
				}
				mapTrackUsage.put(sTabID, currentLength);
			} finally {
				mapTrackUsage_mon.exit();
			}
		}

		lCurrentTrackTime = 0;
	}

	/**
	 * 
	 */
	private void createOldMainWindow() {
		if (oldMainWindow != null || disposedOrDisposing) {
			return;
		}

		if (uiSWTInstanceImpl == null) {
			System.out.println("This will end only in disaster! "
					+ Debug.getCompressedStackTrace());
		}

		SWTSkinObject skinObject = skin.getSkinObject("advanced");
		if (skinObject != null) {
			Composite cArea = (Composite) skinObject.getControl();

			Label lblWait = new Label(cArea, SWT.CENTER);
			FormData formData = new FormData();
			formData.left = new FormAttachment(0, 0);
			formData.right = new FormAttachment(100, 0);
			formData.top = new FormAttachment(0, 0);
			formData.bottom = new FormAttachment(100, 0);
			lblWait.setLayoutData(formData);
			lblWait.setForeground(skinObject.getProperties().getColor("color.row.fg"));
			Messages.setLanguageText(lblWait, "MainWindow.v3.view.wait");
			cArea.layout(true);
			lblWait.update();

			cArea.setBackground(cArea.getShell().getBackground());

			oldMainWindow = new org.gudy.azureus2.ui.swt.mainwindow.MainWindow(core,
					null, cArea.getShell(), cArea, uiSWTInstanceImpl);
			oldMainWindow.setShowMainWindow(false);
			oldMainWindow.runSupport();

			oldMainMenu = new org.gudy.azureus2.ui.swt.mainwindow.MainMenu(shell);
			oldMainMenu.createMenu(core, shell);
			oldMainMenu.setMainWindow(oldMainWindow);
			oldMainWindow.setMenu(oldMainMenu);

			Menu viewMenu = oldMainMenu.getMenu(org.gudy.azureus2.ui.swt.mainwindow.MainMenu.MENU_VIEW);
			if (viewMenu != null) {
				menu.addToOldMenuView(viewMenu);
			}

			uiFunctions.oldMainWindowInitialized(oldMainWindow);

			lblWait.dispose();
			cArea.layout(true);
		}
	}

	public org.gudy.azureus2.ui.swt.mainwindow.MainWindow getOldMainWindow(
			boolean bForceCreate) {
		if (oldMainWindow == null && bForceCreate) {
			createOldMainWindow();
		}
		return oldMainWindow;
	}

	public UIFunctionsSWT getOldUIFunctions(boolean bCreateOld) {
		if (oldMainWindow == null && bCreateOld) {
			createOldMainWindow();
		}
		if (oldMainWindow != null) {
			return oldMainWindow.getUIFunctions();
		}
		return null;
	}

	public void switchToAdvancedTab() {
		SWTSkinTabSet tabSetMain = skin.getTabSet(SkinConstants.TABSET_MAIN);
		if (tabSetMain == null) {
			System.err.println(SkinConstants.TABSET_MAIN);
			return;
		}

		tabSetMain.setActiveTab("maintabs.advanced");
	}

	public UISWTInstance getUISWTInstanceImpl() {
		return uiSWTInstanceImpl;
	}

	/**
	 * @param url
	 * @param target
	 */
	public void showURL(String url, String target) {

		SWTSkinObject skinObject = skin.getSkinObject(target);

		skin.activateTab(skinObject);

		if (skinObject instanceof SWTSkinObjectBrowser) {
			((SWTSkinObjectBrowser) skinObject).getBrowser().setVisible(false);
			if (url == null || url.length() == 0) {
				((SWTSkinObjectBrowser) skinObject).restart();
			} else {
				((SWTSkinObjectBrowser) skinObject).setURL(url);
			}
		}
	}
}