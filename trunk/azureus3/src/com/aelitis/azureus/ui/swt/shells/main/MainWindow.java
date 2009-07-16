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

import java.io.*;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.ConfigurationChecker;
import org.gudy.azureus2.core3.config.impl.ConfigurationDefaults;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.global.*;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.sidebar.SideBarEntry;
import org.gudy.azureus2.plugins.ui.sidebar.SideBarOpenListener;
import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.associations.AssociationChecker;
import org.gudy.azureus2.ui.swt.debug.ObfusticateShell;
import org.gudy.azureus2.ui.swt.donations.DonationWindow;
import org.gudy.azureus2.ui.swt.mainwindow.*;
import org.gudy.azureus2.ui.swt.minibar.AllTransfersBar;
import org.gudy.azureus2.ui.swt.minibar.MiniBarManager;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTInstanceImpl;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.shells.MessageSlideShell;
import org.gudy.azureus2.ui.swt.views.IView;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnManager;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils.RunDownloadManager;
import org.gudy.azureus2.ui.systray.SystemTraySWT;

import com.aelitis.azureus.activities.VuzeActivitiesManager;
import com.aelitis.azureus.buddy.VuzeBuddy;
import com.aelitis.azureus.buddy.VuzeBuddyCreator;
import com.aelitis.azureus.buddy.impl.VuzeBuddyManager;
import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.cnetwork.ContentNetworkManagerFactory;
import com.aelitis.azureus.core.messenger.ClientMessageContext;
import com.aelitis.azureus.core.messenger.PlatformMessenger;
import com.aelitis.azureus.core.messenger.browser.BrowserMessage;
import com.aelitis.azureus.core.messenger.browser.BrowserMessageDispatcher;
import com.aelitis.azureus.core.messenger.config.*;
import com.aelitis.azureus.core.messenger.config.PlatformConfigMessenger.PlatformLoginCompleteListener;
import com.aelitis.azureus.core.torrent.GlobalRatingUtils;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.core.versioncheck.VersionCheckClient;
import com.aelitis.azureus.launcher.Launcher;
import com.aelitis.azureus.login.NotLoggedInException;
import com.aelitis.azureus.plugins.startstoprules.defaultplugin.StartStopRulesDefaultPlugin;
import com.aelitis.azureus.plugins.startstoprules.defaultplugin.StartStopRulesFPListener;
import com.aelitis.azureus.ui.IUIIntializer;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.*;
import com.aelitis.azureus.ui.swt.Initializer;
import com.aelitis.azureus.ui.swt.buddy.impl.VuzeBuddyFakeSWTImpl;
import com.aelitis.azureus.ui.swt.buddy.impl.VuzeBuddySWTImpl;
import com.aelitis.azureus.ui.swt.columns.utils.TableColumnCreatorV3;
import com.aelitis.azureus.ui.swt.extlistener.StimulusRPC;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.utils.PlayNowList;
import com.aelitis.azureus.ui.swt.views.skin.*;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager.SkinViewManagerListener;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.*;
import com.aelitis.azureus.util.*;

/**
 * @author TuxPaper
 * @created May 29, 2006
 *
 */
public class MainWindow
	implements IMainWindow, ObfusticateShell, SideBarListener,
	AEDiagnosticsEvidenceGenerator, SideBarLogIdListener
{

	private static final LogIDs LOGID = LogIDs.GUI;

	protected Shell shell;

	private Display display;

	private AzureusCore core;

	private IUIIntializer uiInitializer;

	private SWTSkin skin;

	private org.gudy.azureus2.ui.swt.mainwindow.MainWindow oldMW_tab;

	private org.gudy.azureus2.ui.swt.mainwindow.MainWindow oldMW_SB;

	private org.gudy.azureus2.ui.swt.mainwindow.MainWindow oldMainWindow;

	private MainMenu menu;

	private UISWTInstanceImpl uiSWTInstanceImpl;

	private UIFunctionsImpl uiFunctions;

	private SystemTraySWT systemTraySWT;

	private static Map mapTrackUsage = null;

	private final static AEMonitor mapTrackUsage_mon = new AEMonitor(
			"mapTrackUsage");

	private long lCurrentTrackTime = 0;

	private long lCurrentTrackTimeIdle = 0;

	private boolean disposedOrDisposing;

	private DownloadManager[] dms_Startup;

	protected boolean isReady = false;

	private MainStatusBar statusBar;

	private String lastShellStatus = null;

	private Color colorSearchTextBG;

	private Color colorSearchTextFGdef;

	private Color colorSearchTextFG;
	
	private boolean delayedCore;

	public static void main(String args[]) {
		if (Launcher.checkAndLaunch(MainWindow.class, args))
			return;
		Initializer.main(new String[0]);
		//org.gudy.azureus2.ui.swt.Main.main(args);
	}

	/**
	 * Old Initializer.  AzureusCore is required to be started
	 * 
	 * @param core
	 * @param display
	 * @param uiInitializer
	 */
	public MainWindow(AzureusCore core, Display display,
			final IUIIntializer uiInitializer) {
		delayedCore = false;
		this.core = core;
		this.display = display;
		this.uiInitializer = uiInitializer;
		AEDiagnostics.addEvidenceGenerator(this);

		disposedOrDisposing = false;

		VuzeBuddyManager.init(new VuzeBuddyCreator() {
			public VuzeBuddy createBuddy(String publicKey) {
				VuzeBuddyManager.log("created buddy: " + publicKey);
				return new VuzeBuddySWTImpl(publicKey);
			}

			public VuzeBuddy createBuddy() {
				VuzeBuddyManager.log("created buddy");
				return new VuzeBuddySWTImpl();
			}

			// @see com.aelitis.azureus.buddy.VuzeBuddyCreator#createPotentialBuddy(Map)
			public VuzeBuddy createPotentialBuddy(Map map) {
				return new VuzeBuddyFakeSWTImpl(map);
			}
		});

		// Hack for 3014 -> 3016 upgrades on Vista who become an Administrator
		// user after restart.
		if (Constants.isWindows
				&& System.getProperty("os.name").indexOf("Vista") > 0
				&& !COConfigurationManager.getBooleanParameter("vista.adminquit")) {
			File fileFromInstall = FileUtil.getApplicationFile("license.txt");
			if (fileFromInstall.exists()
					&& fileFromInstall.lastModified() < new GregorianCalendar(2007, 06,
							13).getTimeInMillis()) {
				// install older than 3016
				GlobalManager gm = core.getGlobalManager();
				if (gm != null
						&& gm.getDownloadManagers().size() == 0
						&& gm.getStats().getTotalProtocolBytesReceived() < 1024 * 1024 * 100) {
					File fileTestWrite = FileUtil.getApplicationFile("testwrite.dll");
					fileTestWrite.deleteOnExit();
					try {
						FileOutputStream fos = new FileOutputStream(fileTestWrite);
						fos.write(23);
						fos.close();

						COConfigurationManager.setParameter("vista.adminquit", true);
						MessageBoxShell.open(shell,
								MessageText.getString("mb.azmustclose.title"),
								MessageText.getString("mb.azmustclose.text"), new String[] {
									MessageText.getString("Button.ok")
								}, 0);
						if (uiInitializer != null) {
							uiInitializer.abortProgress();
						}
						dispose(false, false);
						return;
					} catch (Exception e) {
					}
				}
			}
		}

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
		// TODO: smart refreshing of meta data ("Refresh On" attribute)
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

		gm.addDownloadWillBeRemovedListener(new GlobalManagerDownloadWillBeRemovedListener() {
			public void downloadWillBeRemoved(DownloadManager dm,
					boolean remove_torrent, boolean remove_data)

					throws GlobalManagerDownloadRemovalVetoException {
				TOTorrent torrent = dm.getTorrent();
				if (PublishUtils.isPublished(dm)) {
					String title = MessageText.getString("v3.mb.delPublished.title");

					ContentNetwork cn = DataSourceUtils.getContentNetwork(torrent);
					if (cn == null) {
						return;
					}

					String site = ContentNetworkUtils.getUrl(cn,
							ContentNetwork.SERVICE_SITE);

					String site_host = (String) cn.getProperty(ContentNetwork.PROPERTY_SITE_HOST);

					String text = MessageText.getString("v3.mb.delPublished.text",
							new String[] {
								dm.getDisplayName(),
								site,
								site_host,
								ContentNetworkUtils.getUrl(cn,
										ContentNetwork.SERVICE_PUBLISH_ABOUT)
							});

					MessageBoxShell mb = new MessageBoxShell(shell, title, text,
							new String[] {
								MessageText.getString("v3.mb.delPublished.delete"),
								MessageText.getString("v3.mb.delPublished.cancel")
							}, 1);
					mb.setRelatedObject(dm);

					int result = mb.open();
					if (result != 0) {
						throw new GlobalManagerDownloadRemovalVetoException("", true);
					}
				} else if (PlatformTorrentUtils.isContentDRM(torrent) && remove_data) {

					String prefix = "v3.mb.deletePurchased.";
					String title = MessageText.getString(prefix + "title");
					String text = MessageText.getString(prefix + "text", new String[] {
						dm.getDisplayName()
					});

					MessageBoxShell mb = new MessageBoxShell(shell, title, text,
							new String[] {
								MessageText.getString(prefix + "button.delete"),
								MessageText.getString(prefix + "button.cancel")
							}, 1);
					mb.setRelatedObject(dm);

					int result = mb.open();
					if (result != 0) {
						throw new GlobalManagerDownloadRemovalVetoException("", true);
					}
				}
			}
		});

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

				HashWrapper hw;
				try {
					hw = dm.getTorrent().getHashWrapper();
					if (PlayNowList.contains(hw)) {
						return false;
					}
				} catch (TOTorrentException e) {
				}
				return true;
			}

		});
	}

	/**
	 * New Initializer.  AzureusCore does not need to be started.
	 * Use {@link #init(AzureusCore)} when core is available.
	 * 
	 * @param display
	 * @param uiInitializer
	 */
	public MainWindow(final Display display, final IUIIntializer uiInitializer) {
		delayedCore = true;
		this.display = display;
		this.uiInitializer = uiInitializer;

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
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
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				_init(core);
				if (uiInitializer != null) {
					uiInitializer.abortProgress();
				}
			}
		});
	}

	/**
	 * Called only on STARTUP_UIFIRST
	 */
	public void _init(AzureusCore core) {
		this.core = core;
		AEDiagnostics.addEvidenceGenerator(this);

		disposedOrDisposing = false;

		if (!Constants.isSafeMode && COConfigurationManager.getBooleanParameter("Open Transfer Bar On Start")) {
			uiFunctions.showGlobalTransferBar();
		}

		VuzeBuddyManager.init(new VuzeBuddyCreator() {
			public VuzeBuddy createBuddy(String publicKey) {
				VuzeBuddyManager.log("created buddy: " + publicKey);
				return new VuzeBuddySWTImpl(publicKey);
			}

			public VuzeBuddy createBuddy() {
				VuzeBuddyManager.log("created buddy");
				return new VuzeBuddySWTImpl();
			}

			// @see com.aelitis.azureus.buddy.VuzeBuddyCreator#createPotentialBuddy(Map)
			public VuzeBuddy createPotentialBuddy(Map map) {
				return new VuzeBuddyFakeSWTImpl(map);
			}
		});

		// Hack for 3014 -> 3016 upgrades on Vista who become an Administrator
		// user after restart.
		if (Constants.isWindows
				&& System.getProperty("os.name").indexOf("Vista") > 0
				&& !COConfigurationManager.getBooleanParameter("vista.adminquit")) {
			File fileFromInstall = FileUtil.getApplicationFile("license.txt");
			if (fileFromInstall.exists()
					&& fileFromInstall.lastModified() < new GregorianCalendar(2007, 06,
							13).getTimeInMillis()) {
				// install older than 3016
				GlobalManager gm = core.getGlobalManager();
				if (gm != null
						&& gm.getDownloadManagers().size() == 0
						&& gm.getStats().getTotalProtocolBytesReceived() < 1024 * 1024 * 100) {
					File fileTestWrite = FileUtil.getApplicationFile("testwrite.dll");
					fileTestWrite.deleteOnExit();
					try {
						FileOutputStream fos = new FileOutputStream(fileTestWrite);
						fos.write(23);
						fos.close();

						COConfigurationManager.setParameter("vista.adminquit", true);
						MessageBoxShell.open(shell,
								MessageText.getString("mb.azmustclose.title"),
								MessageText.getString("mb.azmustclose.text"), new String[] {
									MessageText.getString("Button.ok")
								}, 0);
						if (uiInitializer != null) {
							uiInitializer.abortProgress();
						}
						dispose(false, false);
						return;
					} catch (Exception e) {
					}
				}
			}
		}

		try {
			DCAdManager.getInstance().initialize(core);
		} catch (Throwable e) {
		}

		StimulusRPC.hookListeners(core, this);

		uiSWTInstanceImpl = new UISWTInstanceImpl(core);
		uiSWTInstanceImpl.init(uiInitializer);

		PluginInterface pi = core.getPluginManager().getPluginInterfaceByID(
				"azbpstartstoprules");
		if (pi != null) {
			// plugin is built in, so instead of using IPC, just cast it
			StartStopRulesDefaultPlugin plugin = (StartStopRulesDefaultPlugin) pi.getPlugin();
			plugin.addListener(new StartStopRulesFPListener() {
				public boolean isFirstPriority(Download dl, int numSeeds, int numPeers,
						StringBuffer debug) {
					// FP while our content doesn't have another seed
					boolean b = dl.getState() == Download.ST_SEEDING && numSeeds == 0
							&& dl.getStats().getAvailability() < 2
							&& PublishUtils.isPublished(dl); // do last as most costly

					return b;
				}
			});
		}

		VuzeActivitiesManager.initialize(core);

		// When a download is added, check for new meta data and
		// un-"wait state" the rating
		// TODO: smart refreshing of meta data ("Refresh On" attribute)
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

		gm.addDownloadWillBeRemovedListener(new GlobalManagerDownloadWillBeRemovedListener() {
			public void downloadWillBeRemoved(DownloadManager dm,
					boolean remove_torrent, boolean remove_data)

					throws GlobalManagerDownloadRemovalVetoException {
				TOTorrent torrent = dm.getTorrent();
				if (PublishUtils.isPublished(dm)) {
					String title = MessageText.getString("v3.mb.delPublished.title");

					ContentNetwork cn = DataSourceUtils.getContentNetwork(torrent);
					if (cn == null) {
						return;
					}

					String site = ContentNetworkUtils.getUrl(cn,
							ContentNetwork.SERVICE_SITE);

					String site_host = (String) cn.getProperty(ContentNetwork.PROPERTY_SITE_HOST);

					String text = MessageText.getString("v3.mb.delPublished.text",
							new String[] {
								dm.getDisplayName(),
								site,
								site_host,
								ContentNetworkUtils.getUrl(cn,
										ContentNetwork.SERVICE_PUBLISH_ABOUT)
							});

					MessageBoxShell mb = new MessageBoxShell(shell, title, text,
							new String[] {
								MessageText.getString("v3.mb.delPublished.delete"),
								MessageText.getString("v3.mb.delPublished.cancel")
							}, 1);
					mb.setRelatedObject(dm);

					int result = mb.open();
					if (result != 0) {
						throw new GlobalManagerDownloadRemovalVetoException("", true);
					}
				} else if (PlatformTorrentUtils.isContentDRM(torrent) && remove_data) {

					String prefix = "v3.mb.deletePurchased.";
					String title = MessageText.getString(prefix + "title");
					String text = MessageText.getString(prefix + "text", new String[] {
						dm.getDisplayName()
					});

					MessageBoxShell mb = new MessageBoxShell(shell, title, text,
							new String[] {
								MessageText.getString(prefix + "button.delete"),
								MessageText.getString(prefix + "button.cancel")
							}, 1);
					mb.setRelatedObject(dm);

					int result = mb.open();
					if (result != 0) {
						throw new GlobalManagerDownloadRemovalVetoException("", true);
					}
				}
			}
		});

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

				HashWrapper hw;
				try {
					hw = dm.getTorrent().getHashWrapper();
					if (PlayNowList.contains(hw)) {
						return false;
					}
				} catch (TOTorrentException e) {
				}
				return true;
			}

		});

		core.triggerLifeCycleComponentCreated(uiFunctions);

		processStartupDMS();
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
		ArrayList<TOTorrent> toUpdateGlobalRating = new ArrayList();
		boolean oneIsNotPlatform = false;
		for (final DownloadManager dm : dms) {
			if (dm == null) {
				continue;
			}

			DownloadManagerState dmState = dm.getDownloadState();

			final TOTorrent torrent = dm.getTorrent();
			if (torrent == null) {
				continue;
			}

			String hash = null;
			try {
				hash = torrent.getHashWrapper().toBase32String();
			} catch (TOTorrentException e) {
				Debug.out(e);
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

			boolean isContent = PlatformTorrentUtils.isContent(torrent, true);

			if (!oneIsNotPlatform && !isContent
					&& !dmState.getFlag(DownloadManagerState.FLAG_LOW_NOISE)) {
				oneIsNotPlatform = true;
			}

			final String fHash = hash;

			if (isContent) {
				if (PlatformTorrentUtils.getUserRating(torrent) == -2) {
					PlatformTorrentUtils.setUserRating(torrent, -1);
					PlatformRatingMessenger.getUserRating(
							PlatformTorrentUtils.getContentNetworkID(torrent), new String[] {
								PlatformRatingMessenger.RATE_TYPE_CONTENT
							}, new String[] {
								hash
							}, 5000);
				}

				long now = SystemTime.getCurrentTime();
				long mdRefreshOn = PlatformTorrentUtils.getMetaDataRefreshOn(torrent);
				if (mdRefreshOn < now) {
					PlatformTorrentUtils.log(torrent, "addDM, update MD NOW");
					PlatformTorrentUtils.updateMetaData(torrent, 5000);
				} else {
					PlatformTorrentUtils.log(torrent, "addDM, update MD on "
							+ new Date(mdRefreshOn));
					SimpleTimer.addEvent("Update MD", mdRefreshOn,
							new TimerEventPerformer() {
								public void perform(TimerEvent event) {
									PlatformTorrentUtils.updateMetaData(torrent, 15000);
								}
							});
				}

				long grRefreshOn = GlobalRatingUtils.getRefreshOn(torrent);
				if (grRefreshOn <= now) {
					toUpdateGlobalRating.add(torrent);
				} else {
					SimpleTimer.addEvent("Update G.Rating", grRefreshOn,
							new TimerEventPerformer() {
								public void perform(TimerEvent event) {
									PlatformRatingMessenger.updateGlobalRating(torrent, 15000);
								}
							});
				}

				long expiresOn = PlatformTorrentUtils.getExpiresOn(torrent);
				if (expiresOn > now) {
					SimpleTimer.addEvent("dm Expirey", expiresOn,
							new TimerEventPerformer() {
								public void perform(TimerEvent event) {
									dm.getDownloadState().setFlag(
											DownloadManagerState.FLAG_LOW_NOISE, true);
									ManagerUtils.remove(dm, null, true, true);
								}
							});
				}

				if (PublishUtils.isPublished(dm)
						&& dm.getStats().getShareRatio() < 1000 && !dm.isForceStart()) {
					dm.setForceStart(true);
				}
			} // isContent
		}

		if (oneIsNotPlatform && dms_Startup == null) {
			DonationWindow.checkForDonationPopup();
		}

		if (toUpdateGlobalRating.size() > 0) {
			TOTorrent[] torrents = toUpdateGlobalRating.toArray(new TOTorrent[0]);
			PlatformRatingMessenger.updateGlobalRating(torrents, 5000);
		}
	}

	/**
	 * @param uiInitializer 
	 * 
	 */
	protected void createWindow(IUIIntializer uiInitializer) {
		long startTime = SystemTime.getCurrentTime();

		uiFunctions = new UIFunctionsImpl(this);
		UIFunctionsManager.setUIFunctions(uiFunctions);

		Utils.disposeComposite(shell);

		increaseProgress(uiInitializer, "splash.initializeGui");

		System.out.println("UIFunctions/ImageLoad took "
				+ (SystemTime.getCurrentTime() - startTime) + "ms");
		startTime = SystemTime.getCurrentTime();

		shell = new Shell(display, SWT.SHELL_TRIM);

		try {
			shell.setData("class", this);
			shell.setText("Vuze");
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

			/*
			 * KN: passing the skin to the uifunctions so it can be used by UIFunctionsSWT.createMenu()
			 */
			uiFunctions.setSkin(skin);

			System.out.println("new shell setup took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			startTime = SystemTime.getCurrentTime();

			initSkinListeners();

			increaseProgress(uiInitializer, "v3.splash.initSkin");
			System.out.println("skinlisteners init took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			startTime = SystemTime.getCurrentTime();

			skin.initialize(shell, "main.shell", uiInitializer);

			increaseProgress(uiInitializer, "v3.splash.initSkin");
			System.out.println("skin init took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			startTime = SystemTime.getCurrentTime();

			menu = new MainMenu(skin, shell);
			shell.setData("MainMenu", menu);

			System.out.println("MainMenu init took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			startTime = SystemTime.getCurrentTime();

			if (org.gudy.azureus2.core3.util.Constants.isOSX
					&& SWT.getPlatform().equals("carbon")) {
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
						ToolBarView tb = (ToolBarView) SkinViewManager.getByClass(ToolBarView.class);
						if (tb != null) {
							tb.flipShowText();
						}
					}
				};
				shell.addListener(SWT.Expand, toggleListener);
				shell.addListener(SWT.Collapse, toggleListener);
			}

			System.out.println("createWindow init took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			startTime = SystemTime.getCurrentTime();

			increaseProgress(uiInitializer, "v3.splash.initSkin");

			skin.layout();

			System.out.println("skin layout took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			startTime = SystemTime.getCurrentTime();

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

			increaseProgress(uiInitializer, "v3.splash.initSkin");
			System.out.println("pre skin widgets init took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			startTime = SystemTime.getCurrentTime();

			if (core != null) {
				try {
					DCAdManager.getInstance().initialize(core);
				} catch (Throwable e) {
				}

				StimulusRPC.hookListeners(core, this);
			}

			increaseProgress(uiInitializer, "v3.splash.initSkin");
			System.out.println("hooks init took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			startTime = SystemTime.getCurrentTime();

			initWidgets();
			System.out.println("skin widgets (1/2) init took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			startTime = SystemTime.getCurrentTime();
			initWidgets2();

			increaseProgress(uiInitializer, "v3.splash.initSkin");
			System.out.println("skin widgets init took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			startTime = SystemTime.getCurrentTime();

			SideBar sidebar = (SideBar) SkinViewManager.getByClass(SideBar.class);
			if (sidebar != null) {
				setupSideBar(sidebar);
			} else {
				SkinViewManager.addListener(new SkinViewManagerListener() {
					public void skinViewAdded(SkinView skinview) {
						if (skinview instanceof SideBar) {
							setupSideBar((SideBar) skinview);
						}
					}
				});
			}

			System.out.println("pre SWTInstance init took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			increaseProgress(uiInitializer, "v3.splash.hookPluginUI");
			startTime = SystemTime.getCurrentTime();

			TableColumnCreatorV3.initCoreColumns();

			System.out.println("Init Core Columns took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			increaseProgress(uiInitializer, "v3.splash.hookPluginUI");
			startTime = SystemTime.getCurrentTime();

			if (core != null) {
				// attach the UI to plugins
				// Must be done before initializing views, since plugins may register
				// table columns and other objects
				uiSWTInstanceImpl = new UISWTInstanceImpl(core);
				uiSWTInstanceImpl.init(uiInitializer);
				//uiSWTInstanceImpl.addView(UISWTInstance.VIEW_MYTORRENTS,
				//		"PieceGraphView", new PieceGraphView());
			}

			System.out.println("SWTInstance init took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			increaseProgress(uiInitializer, "splash.initializeGui");
			startTime = SystemTime.getCurrentTime();

			if (core != null) {
				PluginInterface pi = core.getPluginManager().getPluginInterfaceByID(
						"azbpstartstoprules");
				if (pi != null) {
					// plugin is built in, so instead of using IPC, just cast it
					StartStopRulesDefaultPlugin plugin = (StartStopRulesDefaultPlugin) pi.getPlugin();
					plugin.addListener(new StartStopRulesFPListener() {
						public boolean isFirstPriority(Download dl, int numSeeds,
								int numPeers, StringBuffer debug) {
							// FP while our content doesn't have another seed
							boolean b = dl.getState() == Download.ST_SEEDING && numSeeds == 0
									&& dl.getStats().getAvailability() < 2
									&& PublishUtils.isPublished(dl); // do last as most costly

							return b;
						}
					});
				}
			}

			ManagerUtils.setRunRunnable(new RunDownloadManager() {
				public void run(DownloadManager dm) {
					TOTorrent torrent = dm.getTorrent();
					if (PlatformTorrentUtils.isContent(torrent, true)
							&& PlatformTorrentUtils.isContentAdEnabled(torrent)) {
						TorrentListViewsUtils.playOrStream(dm);
					} else {
						Utils.launch(dm.getSaveLocation().toString());
					}
				}
			});
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

			configID = "Friends.visible";
			if (false == ConfigurationDefaults.getInstance().doesParameterDefaultExist(
					configID)) {
				COConfigurationManager.setBooleanDefault(configID, true);
			}

			setVisible(WINDOW_ELEMENT_TABBAR, true);

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
				VuzeActivitiesManager.initialize(core);
			}

			System.out.println("vuzeactivities init took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			startTime = SystemTime.getCurrentTime();

			NavigationHelper.addListener(new NavigationHelper.navigationListener() {
				public void processCommand(final int type, final String[] args) {
					Utils.execSWTThread(new AERunnable() {
						public void runSupport() {

							UIFunctions uif = UIFunctionsManager.getUIFunctions();

							if (type == NavigationHelper.COMMAND_SWITCH_TO_TAB) {
								SideBar sideBar = (SideBar) SkinViewManager.getByClass(SideBar.class);
								if (sideBar == null) {
									return;
								}
								ContentNetworkUtils.setSourceRef(args[0], "menu", false);
								sideBar.showEntryByTabID(args[0]);

								if (uif != null) {

									uif.bringToFront();
								}
							} else if (type == NavigationHelper.COMMAND_BUDDY_SYNC) {

								try {
									PlatformRelayMessenger.fetch(0);
									PlatformBuddyMessenger.sync(null);
									PlatformBuddyMessenger.getInvites();
								} catch (NotLoggedInException e1) {
								}

							} else if (type == NavigationHelper.COMMAND_CONDITION_CHECK) {

								if (args[0].equals(NavigationHelper.COMMAND_CHECK_BUDDY_MANAGER)) {

									if (args[1].equals(NavigationHelper.COMMAND_CHECK_BUDDY_MANAGER_ENABLED)) {

										if (!VuzeBuddyManager.isEnabled()) {

											VuzeBuddyManager.showDisabledDialog();

											if (uif != null) {

												uif.bringToFront();
											}
										}
									}
								}
							}
						}
					});
				}
			});
		}
	}
	

	/**
	 * @param skinview
	 *
	 * @since 3.1.1.1
	 */
	protected void setupSideBar(final SideBar sidebar) {
		// 3.2 TODO: set default sidebar item

		sidebar.addListener(this);

		Utils.execSWTThreadLater(0, new AERunnable() {
			public void runSupport() {
				String startTab;
				boolean showWelcome = COConfigurationManager.getBooleanParameter("v3.Show Welcome");
				boolean startAdv = COConfigurationManager.getBooleanParameter("v3.Start Advanced");

				ContentNetwork startupCN = ContentNetworkManagerFactory.getSingleton().getStartupContentNetwork();
				if (!startupCN.isServiceSupported(ContentNetwork.SERVICE_WELCOME)) {
					showWelcome = false;
				}

				if (showWelcome && !startAdv) {
					startTab = SideBar.SIDEBAR_SECTION_WELCOME;
				} else {
					if (showWelcome && startAdv) {
						sidebar.showEntryByID(SideBar.SIDEBAR_SECTION_WELCOME);
					}
					if (COConfigurationManager.getBooleanParameter("v3.Start Advanced")) {
						startTab = SideBar.SIDEBAR_SECTION_LIBRARY;
					} else {
						startTab = "ContentNetwork." + startupCN.getID();
						ContentNetworkUtils.setSourceRef(startTab, "startup", false);
					}
				}
				sidebar.showEntryByTabID(startTab);
			}
		});

		//		System.out.println("Activate sidebar " + startTab + " took "
		//				+ (SystemTime.getCurrentTime() - startTime) + "ms");
		//		startTime = SystemTime.getCurrentTime();
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
		if (false && Utils.isThisThreadSWT()) {
			// clean the dispatch loop so the splash screen gets updated
			int i = 1000;
			while (display.readAndDispatch() && i > 0) {
				i--;
			}
			//if (i < 999) {
			//	System.out.println("dispatched " + (1000 - i));
			//}
		}
	}

	public boolean dispose(final boolean for_restart,
			final boolean close_already_in_progress) {
		if (disposedOrDisposing) {
			return true;
		}
		return Utils.execSWTThreadWithBool("v3.MainWindow.dispose",
				new AERunnableBoolean() {
					public boolean runSupport() {
						return _dispose(for_restart, close_already_in_progress);
					}
				});
	}

	public boolean _dispose(boolean bForRestart, boolean bCloseAlreadyInProgress) {
		if (disposedOrDisposing) {
			return true;
		}

		isReady = false;

		disposedOrDisposing = true;
		if (oldMainWindow != null) {
			boolean res = oldMainWindow.dispose(bForRestart, bCloseAlreadyInProgress);
			oldMainWindow = null;

			if (res == false) {
				disposedOrDisposing = false;
				return false;
			}
		} else {
			if (core != null
					&& !UIExitUtilsSWT.canClose(core.getGlobalManager(), bForRestart)) {
				disposedOrDisposing = false;
				return false;
			}

			UIExitUtilsSWT.uiShutdown();
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
		try {
  		AllTransfersBar transfer_bar = AllTransfersBar.getBarIfOpen(core.getGlobalManager());
  		if (transfer_bar != null) {
  			transfer_bar.forceSaveLocation();
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

				Map map = new HashMap();
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
			SWTThread.getInstance().getInitializer().stopIt(bForRestart, false);
		}

		return true;
	}

	private String getUsageActiveTabID() {
		try {
			SideBar sidebar = (SideBar) SkinViewManager.getByClass(SideBar.class);
			if (sidebar != null) {
				SideBarEntrySWT curEntry = sidebar.getCurrentEntry();
				if (curEntry == null) {
					return "none";
				} else {
					String id = curEntry.getLogID();
					return id == null ? "null" : id;
				}
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

				mapTrackUsage = new HashMap();

				if (f.exists()) {
					Map oldMapTrackUsage = FileUtil.readResilientFile(f);
					String version = MapUtils.getMapString(oldMapTrackUsage, "version",
							null);
					Map map = MapUtils.getMapMap(oldMapTrackUsage, "statsmap", null);
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

	private void showMainWindow() {
		if (oldMainWindow != null) {
			//oldMainWindow.postPluginSetup(-1, 0);
		}

		boolean isOSX = org.gudy.azureus2.core3.util.Constants.isOSX;
		// No tray access on OSX yet
		boolean bEnableTray = COConfigurationManager.getBooleanParameter("Enable System Tray")
				&& (!isOSX || SWT.getVersion() > 3300);
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
			}
		});

		if (!bStartMinimize) {
			shell.open();
			if (!isOSX) {
				shell.forceActive();
			}
		} else if (isOSX) {
			shell.setMinimized(true);
			shell.setVisible(true);
		}
		

		if (delayedCore) {
			// TODO: Check if update window takes control and messes things up
  		while (!display.isDisposed() && display.readAndDispatch());
  		System.out.println("---------DONE DISPATCH AT "
  				+ SystemTime.getCurrentTime() + ";"
  				+ (SystemTime.getCurrentTime() - Initializer.startTime) + "ms");
  		if (display.isDisposed()) {
  			return;
  		}
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
					setVisible(true); // invokes password
				}
			}
		}

		// do this before other checks as these are blocking dialogs to force order

		if (uiInitializer != null) {

			uiInitializer.initializationComplete();
		}

		AssociationChecker.checkAssociations();

		// Donation stuff
		Map map = VersionCheckClient.getSingleton().getMostRecentVersionCheckData();
		DonationWindow.setInitialAskHours(MapUtils.getMapInt(map,
				"donations.askhrs", DonationWindow.getInitialAskHours()));

		if (core != null) {
			core.triggerLifeCycleComponentCreated(uiFunctions);
		}

		Utils.execSWTThreadLater(0, new AERunnable() {
			public void runSupport() {
				fixupActionBarSize();
			}
		});

		System.out.println("---------READY AT " + SystemTime.getCurrentTime() + ";"
				+ (SystemTime.getCurrentTime() - Initializer.startTime) + "ms");
		isReady = true;
		//SESecurityManagerImpl.getSingleton().exitVM(0);
	}

	public void setVisible(final boolean visible) {
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
						Color colorBG = skin.getSkinProperties().getColor("color.mainshell");
						Color colorLink = skin.getSkinProperties().getColor(
								"color.links.normal");
						Color colorText = skin.getSkinProperties().getColor("color.text.fg");

						//composite.setBackground(colorBG);
						//composite.setForeground(colorText);

						MessageBoxShell shell = (MessageBoxShell) skinnableObject;
						//shell.setUrlColor(colorLink);

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

		skinnableManagerSWT.addSkinnableListener(
				MessageSlideShell.class.toString(), new UISkinnableSWTListener() {

					public void skinBeforeComponents(Composite composite,
							Object skinnableObject, Object[] relatedObjects) {
						if (skinnableObject instanceof MessageSlideShell) {
							final Image image = new Image(composite.getDisplay(), 250, 300);

							TOTorrent torrent = null;
							DownloadManager dm = (DownloadManager) LogRelationUtils.queryForClass(
									relatedObjects, DownloadManager.class);
							if (dm != null) {
								torrent = dm.getTorrent();
							} else {
								torrent = (TOTorrent) LogRelationUtils.queryForClass(
										relatedObjects, TOTorrent.class);
							}

							MessageSlideShell shell = (MessageSlideShell) skinnableObject;

							byte[] contentThumbnail = PlatformTorrentUtils.getContentThumbnail(torrent);
							GC gc = new GC(image);
							try {
								gc.setBackground(gc.getDevice().getSystemColor(
										SWT.COLOR_WIDGET_BACKGROUND));
								gc.fillRectangle(image.getBounds());

								if (contentThumbnail != null) {

									try {
										ByteArrayInputStream bis = new ByteArrayInputStream(
												contentThumbnail);
										final Image img = new Image(Display.getDefault(), bis);
										Rectangle imgBounds = img.getBounds();
										double pct = 35.0 / imgBounds.height;
										int w = (int) (imgBounds.width * pct);

										try {
											gc.setAdvanced(true);
											gc.setInterpolation(SWT.HIGH);
										} catch (Exception e) {
											// not important if we can't set advanced
										}

										gc.drawImage(img, 0, 0, imgBounds.width, imgBounds.height,
												0, 265, w, 35);
										img.dispose();
									} catch (Exception e) {

									}

								}
							} finally {
								gc.dispose();
							}
							shell.setImgPopup(image);

							composite.addListener(SWT.Dispose, new Listener() {
								public void handleEvent(Event event) {
									if (!image.isDisposed()) {
										image.dispose();
									}
								}
							});
						}
					}

					public void skinAfterComponents(Composite composite,
							Object skinnableObject, Object[] relatedObjects) {
						if (true) {
							return; // temp disable
						}
						Color bg = skin.getSkinProperties().getColor("color.mainshell");
						if (bg != null) {
							composite.setBackground(bg);
						}
						Color fg = skin.getSkinProperties().getColor("color.section.header");
						if (fg != null) {
							setChildrenFG(composite, fg);
						}
						composite.setBackgroundMode(SWT.INHERIT_DEFAULT);
					}
				});
	}

	private void setChildrenFG(Control parent, Color color) {
		parent.setForeground(color);
		if (parent instanceof Composite) {
			Control[] children = ((Composite) parent).getChildren();
			for (int i = 0; i < children.length; i++) {
				Control control = children[i];
				if (!(control instanceof Button)
						|| (((Button) control).getStyle() & SWT.CHECK) > 0) {
					setChildrenFG(control, color);
				}
			}
		}
	}

	/**
	 * 
	 */
	private void initWidgets() {
		SWTSkinObject skinObject;

		/*
		 * Directly loading the buddies viewer since we need to access it
		 * before it's even shown for the first time
		 */
		Class[] forceInits = new Class[] {
			BuddiesViewer.class,
			SideBar.class,
			FriendsToolbar.class
		};
		String[] forceInitsIDs = new String[] {
			SkinConstants.VIEWID_BUDDIES_VIEWER,
			SkinConstants.VIEWID_SIDEBAR,
			SkinConstants.VIEWID_FRIENDS_TOOLBAR
		};

		for (int i = 0; i < forceInits.length; i++) {
			Class cla = forceInits[i];
			String id = forceInitsIDs[i];

			try {
				skinObject = skin.getSkinObject(id);
				if (null != skinObject) {
					SkinView skinView = (SkinView) cla.newInstance();
					skinView.setMainSkinObject(skinObject);
					skinObject.addListener(skinView);
				}
			} catch (Throwable t) {
				Debug.out(t);
			}
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

		skinObject = skin.getSkinObject(SkinConstants.VIEWID_PLUGINBAR);
		if (skinObject != null) {
			Menu topbarMenu = new Menu(shell, SWT.POP_UP);

			if (COConfigurationManager.getIntParameter("User Mode") > 1) {
				MainMenu.createViewMenuItem(skin, topbarMenu,
						"v3.MainWindow.menu.view." + SkinConstants.VIEWID_PLUGINBAR,
						SkinConstants.VIEWID_PLUGINBAR + ".visible",
						SkinConstants.VIEWID_PLUGINBAR, true, -1);
			}

			final MenuItem itemShowText = new MenuItem(topbarMenu, SWT.CHECK);
			Messages.setLanguageText(itemShowText,
					"v3.MainWindow.menu.showActionBarText");
			itemShowText.addSelectionListener(new SelectionAdapter() {
				// @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
				public void widgetSelected(SelectionEvent e) {
					ToolBarView tb = (ToolBarView) SkinViewManager.getByClass(ToolBarView.class);
					if (tb != null) {
						tb.flipShowText();
					}
				}
			});

			topbarMenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent e) {
					ToolBarView tb = (ToolBarView) SkinViewManager.getByClass(ToolBarView.class);
					if (tb != null) {
						itemShowText.setSelection(tb.getShowText());
					}
				}

				public void menuHidden(MenuEvent e) {
				}
			});

			addMenuAndNonTextChildren((Composite) skinObject.getControl(), topbarMenu);

			skinObject = skin.getSkinObject("tabbar");
			if (skinObject != null) {
				addMenuAndNonTextChildren((Composite) skinObject.getControl(),
						topbarMenu);
			}
		}

		/*
		 * Init the user area for login/logout info
		 */
		new UserAreaUtils(skin, uiFunctions);
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

	/**
	 * @param skinObject
	 */
	private void attachSearchBox(SWTSkinObject skinObject) {
		Composite cArea = (Composite) skinObject.getControl();

		shell.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event event) {
				fixupActionBarSize();
			}
		});

		final Text text = new Text(cArea, SWT.NONE);
		FormData filledFormData = Utils.getFilledFormData();
		text.setLayoutData(filledFormData);

		text.addListener(SWT.Resize, new Listener() {
			Font lastFont = null;

			public void handleEvent(Event event) {
				Text text = (Text) event.widget;

				int h = text.getClientArea().height - 2;
				Font font = Utils.getFontWithHeight(text.getFont(), null, h);
				if (font != null) {
					text.setFont(font);

					Utils.disposeSWTObjects(new Object[] {
						lastFont
					});

					text.addDisposeListener(new DisposeListener() {
						public void widgetDisposed(DisposeEvent e) {
							Text text = (Text) e.widget;
							text.setFont(null);
							Utils.disposeSWTObjects(new Object[] {
								lastFont
							});
						}
					});
				}
			}
		});
		text.setTextLimit(254);

		final String sDefault = MessageText.getString("v3.MainWindow.search.defaultText");

		SWTSkinProperties properties = skinObject.getProperties();
		colorSearchTextBG = properties.getColor("color.search.text.bg");
		colorSearchTextFG = properties.getColor("color.search.text.fg");
		colorSearchTextFGdef = properties.getColor("color.search.text.fg.default");

		if (colorSearchTextFGdef != null) {
			text.setForeground(colorSearchTextFGdef);
		}
		if (colorSearchTextBG != null) {
			text.setBackground(colorSearchTextBG);
		}
		text.addMouseListener(new MouseListener() {

			public void mouseUp(MouseEvent e) {
				Text text = (Text) e.widget;
				if (text.getText().equals(sDefault)) {
					if (colorSearchTextFG != null) {
						text.setForeground(colorSearchTextFG);
					}
					text.setText("");
				}
			}

			public void mouseDown(MouseEvent e) {
			}

			public void mouseDoubleClick(MouseEvent e) {
			}
		});

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
				if (text.getText().equals(sDefault)) {
					if (colorSearchTextFG != null) {
						text.setForeground(colorSearchTextFG);
					}
					if (event.character != '\0') {
						text.setText("");
					}
					return;
				}

				Text text = (Text) event.widget;
				if (event.keyCode == SWT.ESC) {
					text.setText("");
					return;
				}
				if (event.character == SWT.CR) {
					doSearch(text.getText());
				}
			}
		});

		// must be done after layout
		text.setText(sDefault);
		//text.selectAll();

		SWTSkinObject searchGo = skin.getSkinObject("search-go");
		if (searchGo != null) {
			SWTSkinButtonUtility btnGo = new SWTSkinButtonUtility(searchGo);
			btnGo.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility,
						SWTSkinObject skinObject, int stateMask) {
					String sSearchText = text.getText().trim();
					doSearch(sSearchText);
				}
			});
		}

		SWTSkinObject so = skin.getSkinObject("sidebar-list");
		if (so != null
				&& so.getProperties().getBooleanValue(
						so.getConfigID() + ".resizeSearch", false)) {
			Listener l = new Listener() {
				public void handleEvent(Event event) {
					SWTSkinObject soSearchArea = skin.getSkinObject("topbar-area-search");
					if (soSearchArea != null) {
						Control c = soSearchArea.getControl();
						Rectangle bounds = ((Control) event.widget).getBounds();
						FormData fd = (FormData) c.getLayoutData();
						int newWidth = bounds.width - 1 - c.getBounds().x;
						if (bounds.width < 125) {
							return;
						}
						fd.width = newWidth;
						Utils.relayout(c);
					}
				}
			};
			so.getControl().addListener(SWT.Resize, l);
		}

		so = skin.getSkinObject("search-dropdown");
		if (so != null) {
			SWTSkinButtonUtility btnSearchDD = new SWTSkinButtonUtility(so);
			btnSearchDD.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility,
						SWTSkinObject skinObject, int stateMask) {
					String sSearchText = text.getText().trim();
					doSearch(sSearchText);
				}
			});
		}
	}

	/**
	 * 
	 *
	 * @since 4.0.0.1
	 */
	protected void fixupActionBarSize() {
		Rectangle clientArea = shell.getClientArea();
		SWTSkinObject soSearch = skin.getSkinObject("topbar-area-search");
		if (soSearch == null) {
			return;
		}
		FormData fd = (FormData) soSearch.getControl().getLayoutData();
		if (fd == null || fd.width <= 0) {
			return;
		}
		if (clientArea.width > 1024 && fd.width == 260) {
			return;
		}
		SWTSkinObject soTabBar = skin.getSkinObject(SkinConstants.VIEWID_TAB_BAR);
		if (soTabBar == null) {
			return;
		}
		Point size = soTabBar.getControl().computeSize(SWT.DEFAULT, SWT.DEFAULT);
		int oldWidth = fd.width;
		fd.width = clientArea.width - (size.x - oldWidth) - 5;
		if (fd.width < 100) {
			fd.width = 100;
		} else if (fd.width > 260) {
			fd.width = 260;
		}

		if (oldWidth != fd.width) {
			((Composite) soTabBar.getControl()).layout(true, true);
		}
	}

	/**
	 * @param searchText
	 */
	//TODO : Tux Move to utils? Could you also add a "mode" or something that would be added to the url
	// eg: &subscribe_mode=true
	public static void doSearch(String sSearchText) {
		doSearch(sSearchText, false);
	}

	public static void doSearch(String sSearchText, boolean toSubscribe) {
		String sDefault = MessageText.getString("v3.MainWindow.search.defaultText");
		if (sSearchText.equals(sDefault) || sSearchText.length() == 0) {
			return;
		}

		SideBar sidebar = (SideBar) SkinViewManager.getByClass(SideBar.class);
		String id = "Search";
		SearchResultsTabArea searchClass = (SearchResultsTabArea) SkinViewManager.getByClass(SearchResultsTabArea.class);
		if (searchClass != null) {
			searchClass.anotherSearch(sSearchText, toSubscribe);
		} else {

			SearchResultsTabArea.SearchQuery sq = new SearchResultsTabArea.SearchQuery();
			sq.term = sSearchText;
			sq.toSubscribe = toSubscribe;

			SideBarEntrySWT entry = sidebar.createEntryFromSkinRef(null, id,
					"main.area.searchresultstab", sSearchText, null, sq, true, -1);
			if (entry != null) {
				entry.setImageLeftID("image.sidebar.search");
			}
		}
		sidebar.showEntryByID(id);
	}

	/**
	 * 
	 */
	private void updateMapTrackUsage(String sTabID) {
		//System.out.println("UPDATE: " + sTabID);
		if (mapTrackUsage != null) {
			mapTrackUsage_mon.enter();
			try {
				if (lCurrentTrackTime > 0) {
					Long currentLength = (Long) mapTrackUsage.get(sTabID);
					long newLength;
					if (currentLength == null) {
						newLength = lCurrentTrackTime;
					} else {
						newLength = currentLength.longValue() + lCurrentTrackTime;
					}
					if (newLength > 1000) {
						mapTrackUsage.put(sTabID, new Long(newLength / 1000));
						//System.out.println("UPDATE: " + sTabID + ";" + newLength);
					}
				}

				if (lCurrentTrackTimeIdle > 0) {
					String id = "idle-" + sTabID;
					Long currentLengthIdle = (Long) mapTrackUsage.get(id);
					long newLengthIdle = currentLengthIdle == null
							? lCurrentTrackTimeIdle : currentLengthIdle.longValue()
									+ lCurrentTrackTimeIdle;
					if (newLengthIdle > 1000) {
						mapTrackUsage.put(id, new Long(newLengthIdle / 1000));
						//System.out.println("UPDATE: " + id + ";" + newLengthIdle);
					}
				}
			} finally {
				mapTrackUsage_mon.exit();
			}
		}

		lCurrentTrackTime = 0;
		lCurrentTrackTimeIdle = 0;
	}

	public static void addUsageStat(String id, long value) {
		if (id == null) {
			return;
		}
		if (id.length() > 150) {
			id = id.substring(0, 150);
		}
		if (mapTrackUsage != null) {
			mapTrackUsage_mon.enter();
			try {
				Long currentLength = (Long) mapTrackUsage.get(id);
				long newLength;
				if (currentLength == null) {
					newLength = value;
				} else {
					newLength = currentLength.longValue() + value;
				}
				if (newLength > 1000) {
					mapTrackUsage.put(id, new Long(newLength / 1000));
				}
			} finally {
				mapTrackUsage_mon.exit();
			}
		}
	}

	/**
	 * 
	 */
	private org.gudy.azureus2.ui.swt.mainwindow.MainWindow createOldMainWindow() {
		if (oldMainWindow != null || disposedOrDisposing) {
			return oldMainWindow;
		}

		SideBar sideBar = (SideBar) SkinViewManager.getByClass(SideBar.class);
		if (sideBar != null) {
			sideBar.showEntryByID(SideBar.SIDEBAR_SECTION_ADVANCED);
		}

		SkinView skinView = SkinViewManager.getByClass(SBC_AdvancedView.class);
		if (skinView instanceof SBC_AdvancedView) {
			oldMainWindow = ((SBC_AdvancedView) skinView).getOldMainWindow();
		}
		return oldMainWindow;
	}

	public org.gudy.azureus2.ui.swt.mainwindow.MainWindow getOldMainWindow(
			boolean bForceCreate) {
		if (oldMW_SB == null && bForceCreate) {
			oldMainWindow = createOldMainWindow();
		}
		oldMainWindow = oldMW_SB;

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

	public UISWTInstance getUISWTInstanceImpl() {
		return uiSWTInstanceImpl;
	}

	/**
	 * @param url
	 * @param target
	 */
	public void showURL(final String url, String target) {

		if (url.startsWith("AZMSG%3B") && false) {
			try {
				BrowserMessage browserMsg;
				browserMsg = new BrowserMessage(URLDecoder.decode(url, "utf-8"));
				ClientMessageContext context = PlatformMessenger.getClientMessageContext();
				BrowserMessageDispatcher dispatcher = context.getDispatcher();
				if (dispatcher != null) {
					dispatcher.dispatch(browserMsg);
					dispatcher.resetSequence();
				} else {
					browserMsg.debug("no dispatcher for showURL action");
				}
			} catch (UnsupportedEncodingException e) {
			}
			return;
		}

		if ("_blank".equalsIgnoreCase(target)) {
			Utils.launch(url);
			return;
		}

		if (target.startsWith("tab-")) {
			target = target.substring(4);
		}

		SideBar sideBar = (SideBar) SkinViewManager.getByClass(SideBar.class);

		// Note; We don't setSourceRef on ContentNetwork here like we do
		// everywhere else because the source ref should already be set
		// by the caller
		String id = sideBar.showEntryByTabID(target);
		if (id == null) {
			Utils.launch(url);
			return;
		}

		SideBarEntrySWT entry = SideBar.getEntry(id);
		entry.addListener(new SideBarOpenListener() {

			public void sideBarEntryOpen(SideBarEntry entry) {
				entry.removeListener(this);

				setVisible(true);

				if (!(entry instanceof SideBarEntrySWT)) {
					return;
				}
				SideBarEntrySWT entrySWT = (SideBarEntrySWT) entry;

				SWTSkinObjectBrowser soBrowser = SWTSkinUtils.findBrowserSO(entrySWT.getSkinObject());

				if (soBrowser != null) {
					//((SWTSkinObjectBrowser) skinObject).getBrowser().setVisible(false);
					if (url == null || url.length() == 0) {
						soBrowser.restart();
					} else {
						String fullURL = url;
						if (UrlFilter.getInstance().urlCanRPC(url)) {
							// 4010 Tux: This shouldn't be.. either determine ContentNetwork from
							//           url or target, or do something..
							fullURL = ConstantsVuze.getDefaultContentNetwork().appendURLSuffix(
									url, false, true);
						}

						soBrowser.setURL(fullURL);
					}
				}
			}
		});
	}

	protected MainStatusBar getMainStatusBar() {
		return statusBar;
	}

	public boolean isVisible(int windowElement) {
		if (windowElement == IMainWindow.WINDOW_ELEMENT_TOOLBAR) {
			/*
			 * Only the (embedded) old main window has a toolbar which is available only in Vuze Advanced
			 */
			if (null != oldMainWindow) {
				return oldMainWindow.isVisible(windowElement);
			}
		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_TOPBAR) {
			SWTSkinObject skinObject = skin.getSkinObject(SkinConstants.VIEWID_PLUGINBAR);
			if (skinObject != null) {
				return skinObject.isVisible();
			}
		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_TABBAR) {
			SWTSkinObject skinObject = skin.getSkinObject("tabbar");
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
			if (null != oldMainWindow) {
				/*
				 * Only the (embedded) old main window has a toolbar which is available only in Vuze Advanced
				 */
				oldMainWindow.setVisible(windowElement, value);
			}
		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_TOPBAR) {

			SWTSkinUtils.setVisibility(skin, SkinConstants.VIEWID_PLUGINBAR
					+ ".visible", SkinConstants.VIEWID_PLUGINBAR, value, true, true);

		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_STATUSBAR) {
			//TODO:
		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_MENU) {
			//TODO:
		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_TABBAR) {
			SWTSkinUtils.setVisibility(skin, "TabBar.visible",
					SkinConstants.VIEWID_TAB_BAR, value, true, true);
		}

	}

	public Rectangle getMetrics(int windowElement) {
		if (windowElement == IMainWindow.WINDOW_ELEMENT_TOOLBAR) {
			if (null != oldMainWindow) {
				/*
				 * Only the (embedded) old main window has a toolbar which is available only in Vuze Advanced
				 */
				return oldMainWindow.getMetrics(windowElement);
			}
		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_TOPBAR) {

			SWTSkinObject skinObject = skin.getSkinObject(SkinConstants.VIEWID_PLUGINBAR);
			if (skinObject != null) {
				return skinObject.getControl().getBounds();
			}

		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_TABBAR) {

			SWTSkinObject skinObject = skin.getSkinObject("tabbar");
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
			r.height -= getMetrics(IMainWindow.WINDOW_ELEMENT_TABBAR).height;
			r.height -= getMetrics(IMainWindow.WINDOW_ELEMENT_STATUSBAR).height;
			return r;

		}

		return new Rectangle(0, 0, 0, 0);
	}

	public SWTSkin getSkin() {
		return skin;
	}

	public boolean isReady() {
		return isReady;
	}

	public Image generateObfusticatedImage() {
		// 3.2 TODO: Obfusticate! (esp advanced view)

		Image image;
		Rectangle clientArea = shell.getClientArea();
		image = new Image(display, clientArea.width, clientArea.height);

		GC gc = new GC(shell);
		try {
			gc.copyArea(image, clientArea.x, clientArea.y);

			Control[] children = shell.getChildren();
			for (int i = 0; i < children.length; i++) {
				Control control = children[i];
				SWTSkinObject so = (SWTSkinObject) control.getData("SkinObject");
				if (so != null) {
					Image obfusticatedImage = so.generateObfusticatedImage();
					if (obfusticatedImage != null) {
						Rectangle bounds = so.getControl().getBounds();
						gc.drawImage(obfusticatedImage, bounds.x, bounds.y);
					}
				}
			}
		} finally {
			gc.dispose();
		}

		return image;
	}

	/**
	 * @param cla
	 * @param data
	 *
	 * @since 3.1.1.1
	 */
	public void openView(final String parentID, final Class cla, String id,
			final Object data, final boolean closeable) {
		final SideBar sideBar = (SideBar) SkinViewManager.getByClass(SideBar.class);

		if (id == null) {
			id = cla.getName();
			int i = id.lastIndexOf('.');
			if (i > 0) {
				id = id.substring(i + 1);
			}
		}

		IView viewFromID = sideBar.getIViewFromID(id);
		if (viewFromID != null) {
			sideBar.showEntryByID(id);
		}

		final String _id = id;
		Utils.execSWTThreadLater(0, new AERunnable() {

			public void runSupport() {
				if (sideBar != null) {
					if (isOnAdvancedView()) {
						try {
							final IView view = (IView) cla.newInstance();

							Tab mainTabSet = oldMainWindow.getMainTabSet();
							mainTabSet.createTabItem(view, true);
						} catch (Exception e) {
							Debug.out(e);
						}
					} else {
						if (sideBar.showEntryByID(_id)) {
							return;
						}
						if (UISWTViewEventListener.class.isAssignableFrom(cla)) {
							try {
								UISWTViewEventListener l = (UISWTViewEventListener) cla.newInstance();
								sideBar.createTreeItemFromEventListener(parentID, null, l, _id,
										closeable, data);
							} catch (Exception e) {
								Debug.out(e);
							}
						} else {
							sideBar.createTreeItemFromIViewClass(parentID, _id, null, cla,
									null, null, data, null, true);
						}
						sideBar.showEntryByID(_id);
					}
				}
			}
		});

	}

	public boolean isOnAdvancedView() {
		SideBar sideBar = (SideBar) SkinViewManager.getByClass(SideBar.class);
		if (sideBar == null) {
			return false;
		}
		SideBarEntrySWT currentSB = sideBar.getCurrentEntry();
		if (currentSB == null) {
			return false;
		}
		if (oldMainWindow != null && currentSB.id != null
				&& currentSB.id.equals(SideBar.SIDEBAR_SECTION_ADVANCED)) {
			return true;
		}
		return false;
	}

	// @see com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarListener#sidebarItemSelected(com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarInfoSWT, com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarInfoSWT)
	public void sidebarItemSelected(SideBarEntrySWT newSideBarEntry,
			SideBarEntrySWT oldSideBarEntry) {
		if (newSideBarEntry == null) {
			return;
		}

		if (mapTrackUsage != null && oldSideBarEntry != null) {
			oldSideBarEntry.removeListener((SideBarLogIdListener) this);

			String id2 = null;
			SideBar sidebar = (SideBar) SkinViewManager.getByClass(SideBar.class);
			if (sidebar != null) {
				id2 = oldSideBarEntry.getLogID();
			}
			if (id2 == null) {
				id2 = oldSideBarEntry.id;
			}

			updateMapTrackUsage(id2);
		}

		if (newSideBarEntry.id.equals(SideBar.SIDEBAR_SECTION_ADVANCED)
				&& oldMW_SB == null) {
			SkinView[] advViews = SkinViewManager.getMultiByClass(SBC_AdvancedView.class);
			if (advViews != null) {
				for (int i = 0; i < advViews.length; i++) {
					SBC_AdvancedView advView = (SBC_AdvancedView) advViews[i];
					if (oldMW_tab == null || advView.getOldMainWindow() != oldMW_tab) {
						oldMW_SB = advView.getOldMainWindow();
					}
				}
			}
			oldMainWindow = oldMW_SB;
		}

		if (mapTrackUsage != null) {
			newSideBarEntry.addListener((SideBarLogIdListener) this);
		}
	}

	// @see com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarLogIdListener#sidebarLogIdChanged(com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarEntrySWT, java.lang.String, java.lang.String)
	public void sidebarLogIdChanged(SideBarEntrySWT sideBarEntrySWT,
			String oldID, String newID) {
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

	protected void setSelectedLanguageItem() {
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

	protected MainMenu getMainMenu() {
		return menu;
	}

}