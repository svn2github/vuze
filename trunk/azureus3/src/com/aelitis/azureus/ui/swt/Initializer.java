/*
 * Created on May 29, 2006 2:13:41 PM
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
package com.aelitis.azureus.ui.swt;

import java.util.Iterator;
import java.util.Map;

import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.common.util.UserAlerts;
import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.auth.AuthenticatorWindow;
import org.gudy.azureus2.ui.swt.auth.CertificateTrustWindow;
import org.gudy.azureus2.ui.swt.mainwindow.*;
import org.gudy.azureus2.ui.swt.networks.SWTNetworkSelection;
import org.gudy.azureus2.ui.swt.pluginsinstaller.InstallPluginWizard;
import org.gudy.azureus2.ui.swt.progress.ProgressWindow;
import org.gudy.azureus2.ui.swt.update.UpdateMonitor;
import org.gudy.azureus2.ui.swt.updater2.PreUpdateChecker;
import org.gudy.azureus2.ui.swt.updater2.SWTUpdateChecker;

import com.aelitis.azureus.core.*;
import com.aelitis.azureus.core.messenger.ClientMessageContext;
import com.aelitis.azureus.core.messenger.PlatformMessenger;
import com.aelitis.azureus.core.messenger.config.PlatformConfigMessenger;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.launcher.Launcher;
import com.aelitis.azureus.ui.*;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.browser.listener.DisplayListener;
import com.aelitis.azureus.ui.swt.browser.listener.TorrentListener;
import com.aelitis.azureus.ui.swt.browser.msg.BrowserMessage;
import com.aelitis.azureus.ui.swt.shells.main.MainWindow;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectTab;
import com.aelitis.azureus.ui.swt.skin.SWTSkinTabSet;
import com.aelitis.azureus.ui.swt.utils.TorrentUIUtilsV3;
import com.aelitis.azureus.ui.swt.utils.UIUpdaterFactory;
import com.aelitis.azureus.util.*;
import com.aelitis.azureus.util.Constants;

/**
 * @author TuxPaper
 * @created May 29, 2006
 *
 */
public class Initializer
	implements IUIIntializer
{
	private static StartServer startServer;

	private final AzureusCore core;

	protected static SplashWindow splash;

	private final String[] args;

	private CopyOnWriteList listeners = new CopyOnWriteList();

	private AEMonitor listeners_mon = new AEMonitor("Initializer:l");

	private int curPercent = 0;

	public static void main(final String args[]) {
		if (Launcher.checkAndLaunch(Initializer.class, args))
			return;

		if (System.getProperty("ui.temp") == null) {
			System.setProperty("ui.temp", "az3");
		}

		org.gudy.azureus2.ui.swt.Main.main(args);
	}

	/**
	 * Main Initializer
	 * @param core
	 * @param args
	 */
	public Initializer(AzureusCore core, boolean createSWTThreadAndRun,
			String[] args) {
		this.core = core;
		this.args = args;

		if (createSWTThreadAndRun) {
			try {
				SWTThread.createInstance(this);
			} catch (SWTThreadAlreadyInstanciatedException e) {
				Debug.printStackTrace(e);
			}
		} else {
			Constants.initialize(core);
			PlatformConfigMessenger.login(0);
		}
	}

	public void run() {
		// initialise the SWT locale util
		long startTime = SystemTime.getCurrentTime();

		new LocaleUtilSWT(core);

		final Display display = SWTThread.getInstance().getDisplay();

		UIConfigDefaultsSWT.initialize();

		UIConfigDefaultsSWTv3.initialize(core);

		ImageRepository.loadImagesForSplashWindow(display);

		ImageRepository.addPath("com/aelitis/azureus/ui/images/azureus.jpg",
				"azureus_splash");

		if (COConfigurationManager.getBooleanParameter("Show Splash")) {
			display.syncExec(new AERunnable() {
				public void runSupport() {
					splash = new SplashWindow(display, Initializer.this);
				}
			});
		}

		System.out.println("Locale Initializing took "
				+ (SystemTime.getCurrentTime() - startTime) + "ms");
		startTime = SystemTime.getCurrentTime();

		core.addListener(new AzureusCoreListener() {
			int fakePercent = 80;

			long startTime = SystemTime.getCurrentTime();

			String sLastTask;

			public void reportCurrentTask(AzureusCoreOperation op, String currentTask) {
				if (op.getOperationType() != AzureusCoreOperation.OP_INITIALISATION) {
					return;
				}

				Initializer.this.reportCurrentTask(currentTask);
				if (fakePercent > 0) {
					fakePercent--;
					Initializer.this.reportPercent(curPercent + 1);
				}

				if (sLastTask != null && !sLastTask.startsWith("Loading Torrent")) {
					long now = SystemTime.getCurrentTime();
					long diff = now - startTime;
					if (diff > 10 && diff < 1000 * 60 * 5) {
						System.out.println("   Core: " + diff + "ms for " + sLastTask);
					}
					startTime = SystemTime.getCurrentTime();
				}
				sLastTask = currentTask;
				//System.out.println(currentTask);
			}

			public void reportPercent(AzureusCoreOperation op, int percent) {
				if (op.getOperationType() != AzureusCoreOperation.OP_INITIALISATION) {
					return;
				}
				if (percent == 100) {
					long now = SystemTime.getCurrentTime();
					long diff = now - startTime;
					if (diff > 10 && diff < 1000 * 60 * 5) {
						System.out.println("   Core: " + diff + "ms for " + sLastTask);
					}
				}
				// TODO Auto-generated method stub
			}

		});

		core.addLifecycleListener(new AzureusCoreLifecycleAdapter() {
			private GlobalManager gm;

			public void componentCreated(AzureusCore core,
					AzureusCoreComponent component) {
				Initializer.this.reportPercent(curPercent + 1);
				if (component instanceof GlobalManager) {
					reportCurrentTaskByKey("splash.initializePlugins");

					gm = (GlobalManager) component;

					InitialisationFunctions.earlyInitialisation(core);
				}
			}

			// @see com.aelitis.azureus.core.AzureusCoreLifecycleAdapter#started(com.aelitis.azureus.core.AzureusCore)
			public void started(AzureusCore core) {
				InitialisationFunctions.lateInitialisation(core);
				if (gm == null) {
					return;
				}

				// Ensure colors initialized
				Colors.getInstance();

				Initializer.this.reportPercent(curPercent + 1);
				new UserAlerts(gm);

				reportCurrentTaskByKey("splash.initializeGui");

				Initializer.this.reportPercent(curPercent + 1);
				Cursors.init();

				Initializer.this.reportPercent(curPercent + 1);
				
				MainWindow mainWindow = new MainWindow(core, Display.getDefault(),
						splash);
				
				hookListeners(mainWindow);
				
				reportCurrentTaskByKey("splash.openViews");

				SWTUpdateChecker.initialize();

				PreUpdateChecker.initialize(core,
						COConfigurationManager.getStringParameter("ui"));

				UpdateMonitor.getSingleton(core); // setup the update monitor

				//Tell listeners that all is initialized :
				Alerts.initComplete();

				//Finally, open torrents if any.
				for (int i = 0; i < args.length; i++) {

					try {
						TorrentOpener.openTorrent(args[i]);

					} catch (Throwable e) {

						Debug.printStackTrace(e);
					}
				}
			}

			public void stopping(AzureusCore core) {
				Alerts.stopInitiated();
			}

			public void stopped(AzureusCore core) {
			}

			public boolean syncInvokeRequired() {
				return (true);
			}

			public boolean stopRequested(AzureusCore _core)
					throws AzureusCoreException {
				return org.gudy.azureus2.ui.swt.mainwindow.Initializer.handleStopRestart(false);
			}

			public boolean restartRequested(final AzureusCore core) {
				return org.gudy.azureus2.ui.swt.mainwindow.Initializer.handleStopRestart(true);
			}

		});

		reportCurrentTaskByKey("splash.initializeCore");

		core.start();

		reportPercent(80);

		System.out.println("Core Initializing took "
				+ (SystemTime.getCurrentTime() - startTime) + "ms");
		startTime = SystemTime.getCurrentTime();

		reportCurrentTaskByKey("splash.initializeUIElements");

		// Ensure colors initialized
		Colors.getInstance();

		reportPercent(curPercent + 1);
		Alerts.init();

		reportPercent(curPercent + 1);
		ProgressWindow.register(core);

		reportPercent(curPercent + 1);
		new SWTNetworkSelection();

		reportPercent(curPercent + 1);
		new AuthenticatorWindow();

		reportPercent(curPercent + 1);
		new CertificateTrustWindow();

		reportPercent(90);

		InstallPluginWizard.register(core, display);

		System.out.println("GUI Initializing took "
				+ (SystemTime.getCurrentTime() - startTime) + "ms");
	}

	public void stopIt(boolean isForRestart, boolean isCloseAreadyInProgress)
			throws AzureusCoreException {
		if (core != null && !isCloseAreadyInProgress) {

			if (isForRestart) {

				core.checkRestartSupported();
			}
		}

		try {
			if (startServer != null) {
				startServer.stopIt();
			}

			//			Cursors.dispose();

			UIUpdaterFactory.getInstance().stopIt();

			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					SWTThread.getInstance().terminate();
				}
			});

		} finally {

			if (core != null && !isCloseAreadyInProgress) {

				try {
					if (isForRestart) {

						core.restart();

					} else {

						long lStopStarted = System.currentTimeMillis();
						System.out.println("core.stop");
						core.stop();
						System.out.println("core.stop done in "
								+ (System.currentTimeMillis() - lStopStarted));
					}
				} catch (Throwable e) {

					// don't let any failure here cause the stop operation to fail

					Debug.out(e);
				}
			}
		}
	}

	// @see com.aelitis.azureus.ui.IUIIntializer#addListener(org.gudy.azureus2.ui.swt.mainwindow.InitializerListener)
	public void addListener(InitializerListener listener) {
		try {
			listeners_mon.enter();

			listeners.add(listener);
		} finally {

			listeners_mon.exit();
		}
	}

	// @see com.aelitis.azureus.ui.IUIIntializer#removeListener(org.gudy.azureus2.ui.swt.mainwindow.InitializerListener)
	public void removeListener(InitializerListener listener) {
		try {
			listeners_mon.enter();

			listeners.remove(listener);
		} finally {

			listeners_mon.exit();
		}
	}

	private void reportCurrentTask(String currentTaskString) {
		try {
			listeners_mon.enter();

			Iterator iter = listeners.iterator();
			while (iter.hasNext()) {
				InitializerListener listener = (InitializerListener) iter.next();
				try {
					listener.reportCurrentTask(currentTaskString);
				} catch (Exception e) {
					// ignore
				}
			}
		} finally {

			listeners_mon.exit();
		}
	}

	private void reportCurrentTaskByKey(String key) {
		reportCurrentTask(MessageText.getString(key));
	}

	public void reportPercent(int percent) {
		if (curPercent > percent) {
			return;
		}

		curPercent = percent;
		try {
			listeners_mon.enter();

			Iterator iter = listeners.iterator();
			while (iter.hasNext()) {
				InitializerListener listener = (InitializerListener) iter.next();
				try {
					listener.reportPercent(percent);
				} catch (Exception e) {
					// ignore
				}
			}

			if (percent > 100) {
				listeners.clear();
			}
		} finally {

			listeners_mon.exit();
		}
	}

	/**
	 * Hooks some listeners
	 * @param mainWindow
	 */
	private void hookListeners(final MainWindow mainWindow) {

		
		/*
		 * This code block was moved here from being in-line in MainWindow
		 */
		ExternalStimulusHandler.addListener(new ExternalStimulusListener() {
			public boolean receive(String name, Map values) {
				try {
					if (values == null) {
						return false;
					}

					if (!name.equals("AZMSG")) {
						return false;
					}

					Object valueObj = values.get("value");
					if (!(valueObj instanceof String)) {
						return false;
					}

					String value = (String) valueObj;

					ClientMessageContext context = PlatformMessenger.getClientMessageContext();
					if (context == null) {
						return false;
					}
					BrowserMessage browserMsg = new BrowserMessage(value);
					context.debug("Received External message: " + browserMsg);
					String opId = browserMsg.getOperationId();
					if (opId.equals(DisplayListener.OP_OPEN_URL)) {
						Map decodedMap = browserMsg.getDecodedMap();
						String url = MapUtils.getMapString(decodedMap, "url", null);
						if (decodedMap.containsKey("target")
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
						context.debug("no target or open url");

					} else if (opId.equals(TorrentListener.OP_LOAD_TORRENT)) {
						Map decodedMap = browserMsg.getDecodedMap();
						if (decodedMap.containsKey("b64")) {
							String b64 = MapUtils.getMapString(decodedMap, "b64", null);
							return TorrentListener.loadTorrentByB64(core, b64);
						} else if (decodedMap.containsKey("url")) {
							String url = MapUtils.getMapString(decodedMap, "url", null);
							boolean playNow = MapUtils.getMapBoolean(decodedMap, "play-now",
									false);
							TorrentUIUtilsV3.loadTorrent(core, url, MapUtils.getMapString(
									decodedMap, "referer", null), playNow);

							return true;

						} else {
							return false;
						}
					} else if (opId.equals("is-ready")) {
						// The platform needs to know when it can call open-url, and it
						// determines this by the is-ready function
						return mainWindow.isReady();
					} else if (opId.equals("is-version-ge")) {
						Map decodedMap = browserMsg.getDecodedMap();
						if (decodedMap.containsKey("version")) {
							String id = MapUtils.getMapString(decodedMap, "id", "client");
							String version = MapUtils.getMapString(decodedMap, "version", "");
							if (id.equals("client")) {
								return org.gudy.azureus2.core3.util.Constants.isCVSVersion()
										|| org.gudy.azureus2.core3.util.Constants.compareVersions(
												org.gudy.azureus2.core3.util.Constants.AZUREUS_VERSION,
												version) >= 0;
							}
							return false;

						}
					} else if (opId.equals("is-active-tab")) {
						Map decodedMap = browserMsg.getDecodedMap();
						if (decodedMap.containsKey("tab")) {
							String tabID = MapUtils.getMapString(decodedMap, "tab", "");
							if (tabID.length() > 0) {
								SWTSkinTabSet tabSet = mainWindow.getSkin().getTabSet(
										SkinConstants.TABSET_MAIN);
								if (tabSet != null) {
									SWTSkinObjectTab activeTab = tabSet.getActiveTab();
									if (activeTab != null) {
										return activeTab.getViewID().equals("tab-" + tabID);
									}
								}
							}
						}
					}
				} catch (Exception e) {
					Debug.out(e);
				}
				return false;
			}
			
			public int
			query(
				String		name,
				Map			values )
			{
				return( Integer.MIN_VALUE );
			}
		});
	}
}
