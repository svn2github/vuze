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

import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.ui.common.util.UserAlerts;
import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.auth.AuthenticatorWindow;
import org.gudy.azureus2.ui.swt.auth.CertificateTrustWindow;
import org.gudy.azureus2.ui.swt.mainwindow.*;
import org.gudy.azureus2.ui.swt.networks.SWTNetworkSelection;
import org.gudy.azureus2.ui.swt.pluginsinstaller.InstallPluginWizard;
import org.gudy.azureus2.ui.swt.progress.ProgressWindow;
import org.gudy.azureus2.ui.swt.update.UpdateMonitor;
import org.gudy.azureus2.ui.swt.updater2.SWTUpdateChecker;

import com.aelitis.azureus.core.*;
import com.aelitis.azureus.ui.IUIIntializer;
import com.aelitis.azureus.ui.swt.shells.main.MainWindow;
import com.aelitis.azureus.ui.swt.utils.UIUpdaterFactory;
import com.aelitis.azureus.util.Constants;
import com.aelitis.azureus.util.InitialisationFunctions;

/**
 * @author TuxPaper
 * @created May 29, 2006
 *
 */
public class Initializer implements IUIIntializer
{
	private static StartServer startServer;

	private final AzureusCore core;

	protected static SplashWindow splash;

	public static void main(final String args[]) {
		// This *has* to be done first as it sets system properties that are read and cached by Java
		COConfigurationManager.preInitialise();

		String mi_str = System.getProperty(Main.PR_MULTI_INSTANCE);
		boolean mi = mi_str != null && mi_str.equalsIgnoreCase("true");

		startServer = new StartServer();

		boolean debugGUI = Boolean.getBoolean("debug");

		if (mi || debugGUI || Main.processParams(args, startServer)) {
			AzureusCore core = AzureusCoreFactory.create();

			core.addLifecycleListener(new AzureusCoreLifecycleAdapter() {
				private GlobalManager gm;

				public void componentCreated(AzureusCore core,
						AzureusCoreComponent component) {
					if (component instanceof GlobalManager) {
						gm = (GlobalManager) component;

						InitialisationFunctions.initialise(core);
					}
				}

				// @see com.aelitis.azureus.core.AzureusCoreLifecycleAdapter#started(com.aelitis.azureus.core.AzureusCore)

				public void started(AzureusCore core) {
					if (gm == null)
						return;

					new UserAlerts(gm);

					new Colors();

					Cursors.init();

					new MainWindow(core, Display.getDefault(), splash);

					SWTUpdateChecker.initialize();

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
					Alerts.stopCompleted();
				}

				public boolean syncInvokeRequired() {
					return (true);
				}

				public boolean stopRequested(AzureusCore _core)
						throws AzureusCoreException {
					return (handleStopRestart(false));
				}

				public boolean restartRequested(final AzureusCore core) {
					return (handleStopRestart(true));
				}

			});

			startServer.pollForConnections(core);

			new Initializer(core, args);
		}
	}

	public static boolean handleStopRestart(final boolean restart) {
		UIFunctionsSWT functionsSWT = UIFunctionsManagerSWT.getUIFunctionsSWT();
		if (functionsSWT != null) {
			return functionsSWT.dispose(restart, true);
		}

		return false;
	}

	/**
	 * Main Initializer
	 * @param core
	 * @param args
	 */
	public Initializer(AzureusCore core, String[] args) {
		this.core = core;

		try {
			SWTThread.createInstance(this);
		} catch (SWTThreadAlreadyInstanciatedException e) {
			Debug.printStackTrace(e);
		}
	}

	public void run() {
		// initialise the SWT locale util
		long startTime = SystemTime.getCurrentTime();

		new LocaleUtilSWT(core);

		final Display display = SWTThread.getInstance().getDisplay();

		UIConfigDefaultsSWT.initialize();

		UIConfigDefaultsSWTv3.initialize();
		
		ImageRepository.loadImagesForSplashWindow(display);

		display.syncExec(new AERunnable() {
			public void runSupport() {
				splash = new SplashWindow(display);
			}
		});

		System.out.println("Locale Initializing took "
				+ (SystemTime.getCurrentTime() - startTime) + "ms");
		startTime = SystemTime.getCurrentTime();

		core.addListener(new AzureusCoreListener() {
			long startTime = SystemTime.getCurrentTime();

			String sLastTask;

			public void reportCurrentTask(AzureusCoreOperation op, String currentTask) {
				if (op.getOperationType() != AzureusCoreOperation.OP_INITIALISATION)
					return;
				if (sLastTask != null && !sLastTask.startsWith("Loading Torrent")) {
					long now = SystemTime.getCurrentTime();
					long diff = now - startTime;
					if (diff > 10) {
						System.out.println("   Core: " + diff + "ms for " + sLastTask);
					}
					startTime = SystemTime.getCurrentTime();
				}
				sLastTask = currentTask;
				//System.out.println(currentTask);
			}

			public void reportPercent(AzureusCoreOperation op, int percent) {
				if (op.getOperationType() != AzureusCoreOperation.OP_INITIALISATION)
					return;
				if (percent == 100) {
					long now = SystemTime.getCurrentTime();
					long diff = now - startTime;
					if (diff > 10) {
						System.out.println("   Core: " + diff + "ms for " + sLastTask);
					}
				}
				// TODO Auto-generated method stub
			}

		});
		core.start();

		splash.reportPercent(25);

		Constants.initialize(core);

		System.out.println("Core Initializing took "
				+ (SystemTime.getCurrentTime() - startTime) + "ms");
		startTime = SystemTime.getCurrentTime();

		Colors.getInstance();

		Alerts.init();

		ProgressWindow.register(core);

		new SWTNetworkSelection();

		new AuthenticatorWindow();

		new CertificateTrustWindow();

		splash.reportPercent(50);

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

			Colors.getInstance().disposeColors();

			//			Cursors.dispose();

			UIUpdaterFactory.getInstance().stopIt();

			SWTThread.getInstance().terminate();

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
}
