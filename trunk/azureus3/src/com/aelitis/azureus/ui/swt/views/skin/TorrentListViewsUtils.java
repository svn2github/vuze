/**
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.ui.swt.views.skin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.download.ForceRecheckListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadImpl;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadManagerImpl;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnEditorWindow;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.download.DownloadManagerEnhancer;
import com.aelitis.azureus.core.download.EnhancedDownloadManager;
import com.aelitis.azureus.core.messenger.config.PlatformDCAdManager;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.common.table.TableSelectionAdapter;
import com.aelitis.azureus.ui.common.table.TableStructureEventDispatcher;
import com.aelitis.azureus.ui.common.table.TableView;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.skin.SWTSkin;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility;
import com.aelitis.azureus.ui.swt.skin.SWTSkinFactory;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinTabSet;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.utils.PublishUtils;
import com.aelitis.azureus.ui.swt.utils.TorrentUIUtilsV3;
import com.aelitis.azureus.ui.swt.views.TorrentListView;
import com.aelitis.azureus.ui.swt.views.TorrentListViewListener;
import com.aelitis.azureus.ui.swt.views.list.ListView;
import com.aelitis.azureus.util.Constants;
import com.aelitis.azureus.util.DCAdManager;
import com.aelitis.azureus.util.win32.Win32Utils;

/**
 * @author TuxPaper
 * @created Oct 12, 2006
 *
 */
public class TorrentListViewsUtils
{

	public static SWTSkinButtonUtility addShareButton(final SWTSkin skin,
			String PREFIX, final ListView view) {
		SWTSkinObject skinObject = skin.getSkinObject(PREFIX + "send-selected");
		if (skinObject == null) {
			return null;
		}

		SWTSkinButtonUtility btn = new SWTSkinButtonUtility(skinObject);

		btn.addSelectionListener(new SWTSkinButtonUtility.ButtonListenerAdapter() {
			public void pressed(SWTSkinButtonUtility buttonUtility) {
				TableRowCore[] selectedRows = view.getSelectedRows();
				if (selectedRows.length > 0) {
					/*
					 * KN: we're only supporting sharing a single content right now
					 */
					DownloadManager dm = getDMFromDS(selectedRows[0].getDataSource(true));
					if (dm != null) {
						VuzeShareUtils.getInstance().shareTorrent(dm);
					}
				}
			}
		});
		return btn;
	}

	public static SWTSkinButtonUtility addStopButton(final SWTSkin skin,
			String PREFIX, final TorrentListView view) {
		SWTSkinObject skinObject = skin.getSkinObject(PREFIX + "stop");
		if (skinObject == null) {
			return null;
		}
		final SWTSkinButtonUtility btn = new SWTSkinButtonUtility(skinObject);

		btn.addSelectionListener(new ButtonListenerAdapter() {
			public void pressed(SWTSkinButtonUtility buttonUtility) {
				TableRowCore[] selectedRows = view.getSelectedRows();
				for (int i = 0; i < selectedRows.length; i++) {
					DownloadManager dm = (DownloadManager) selectedRows[i].getDataSource(true);
					stop(dm);
					StartStopButtonUtil.updateStopButton(view, btn);
				}
			}
		});
		view.addListener(new TorrentListViewListener() {
			public void stateChanged(DownloadManager manager) {
				boolean update = false;
				TableRowCore[] selectedRows = view.getSelectedRows();
				for (int i = 0; i < selectedRows.length; i++) {
					if (selectedRows[i].getDataSource(true) == manager) {
						update = true;
						break;
					}
				}
				if (update) {
					StartStopButtonUtil.updateStopButton(view, btn);
				}
			}
		});
		return btn;
	}

	/**
	 * @param dm
	 *
	 * @since 3.0.1.5
	 */
	public static void stop(DownloadManager dm) {
		int state = dm.getState();
		if (state == DownloadManager.STATE_ERROR) {
			dm.stopIt(DownloadManager.STATE_QUEUED, false, false);
		} else if (state == DownloadManager.STATE_STOPPED) {
			ManagerUtils.queue(dm, null);
		} else {
			ManagerUtils.stop(dm, null);
		}
	}

	public static SWTSkinButtonUtility addDetailsButton(final SWTSkin skin,
			final String PREFIX, final ListView view) {
		SWTSkinObject skinObject = skin.getSkinObject(PREFIX + "viewdetails");
		if (skinObject == null) {
			return null;
		}

		final SWTSkinButtonUtility btn = new SWTSkinButtonUtility(skinObject);

		btn.addSelectionListener(new SWTSkinButtonUtility.ButtonListenerAdapter() {
			public void pressed(SWTSkinButtonUtility buttonUtility) {
				TableRowCore[] selectedRows = view.getSelectedRows();
				if (selectedRows.length > 0) {
					viewDetails(selectedRows[0], PREFIX.substring(0, PREFIX.length() - 1));
				}
			}
		});

		return btn;
	}

	public static void viewDetails(TableRowCore row, String ref) {
		viewDetails(getAssetHashFromDS(row.getDataSource(true)), ref);
	}

	private static String getAssetHashFromDS(Object ds) {
		try {
			if (ds instanceof DownloadManager) {
				return ((DownloadManager) ds).getTorrent().getHashWrapper().toBase32String();
			} else if (ds instanceof VuzeActivitiesEntry) {
				VuzeActivitiesEntry entry = (VuzeActivitiesEntry) ds;
				return entry.getAssetHash();
			}
		} catch (Exception e) {
			Debug.printStackTrace(e);
		}
		return null;
	}

	private static DownloadManager getDMFromDS(Object ds) {
		try {
			if (ds instanceof DownloadManager) {
				return (DownloadManager) ds;
			} else if (ds instanceof VuzeActivitiesEntry) {
				VuzeActivitiesEntry entry = (VuzeActivitiesEntry) ds;
				return entry.getDownloadManger();
			}
		} catch (Exception e) {
			Debug.printStackTrace(e);
		}
		return null;
	}
	
	private static TOTorrent getTorrentFromDS(Object ds) {
		TOTorrent torrent = null;
		if (ds instanceof DownloadManager) {
			torrent = ((DownloadManager) ds).getTorrent();
		} else if (ds instanceof VuzeActivitiesEntry) {
			torrent = ((VuzeActivitiesEntry) ds).getTorrent();
			if (torrent == null) {
				DownloadManager dm = ((VuzeActivitiesEntry) ds).getDownloadManger();
				if (dm != null) {
					torrent = dm.getTorrent();
				}
			}
		}
		return torrent;
	}


	public static void viewDetails(DownloadManager dm, String ref) {
		if (dm == null) {
			return;
		}
		if (!PlatformTorrentUtils.isContent(dm.getTorrent(), true)) {
			return;
		}

		try {
			viewDetails(dm.getTorrent().getHashWrapper().toBase32String(), ref);
		} catch (TOTorrentException e) {
			Debug.out(e);
		}
	}

	public static void viewDetails(String hash, String ref) {
		if (hash == null) {
			return;
		}

		String url = Constants.URL_PREFIX + Constants.URL_DETAILS + hash + ".html?"
				+ Constants.URL_SUFFIX + "&client_ref=" + ref;

		UIFunctions functions = UIFunctionsManager.getUIFunctions();
		if (functions != null) {
			functions.viewURL(url, SkinConstants.VIEWID_BROWSER_BROWSE, 0, 0, false,
					false);
		}
	}

	public static SWTSkinButtonUtility addCommentsButton(final SWTSkin skin,
			String PREFIX, final ListView view) {
		SWTSkinObject skinObject = skin.getSkinObject(PREFIX + "comment");
		if (skinObject == null) {
			return null;
		}

		final SWTSkinButtonUtility btn = new SWTSkinButtonUtility(skinObject);

		btn.addSelectionListener(new SWTSkinButtonUtility.ButtonListenerAdapter() {
			public void pressed(SWTSkinButtonUtility buttonUtility) {
				TableRowCore[] selectedRows = view.getSelectedRows();
				if (selectedRows.length > 0) {
					String hash = getAssetHashFromDS(selectedRows[0].getDataSource(true));

					String url = Constants.URL_PREFIX + Constants.URL_COMMENTS + hash
							+ ".html?" + Constants.URL_SUFFIX + "&rnd=" + Math.random();

					UIFunctions functions = UIFunctionsManager.getUIFunctions();
					functions.viewURL(url, SkinConstants.VIEWID_BROWSER_BROWSE, 0, 0,
							false, false);
				}
			}
		});

		return btn;
	}

	public static SWTSkinButtonUtility addPlayButton(final SWTSkin skin,
			String PREFIX, final ListView view, boolean bOnlyIfMediaServer,
			boolean bPlayOnDoubleClick) {

		debugDCAD("enter - addPlayButton");

		SWTSkinObject skinObject = skin.getSkinObject(PREFIX + "play");
		if (skinObject == null) {
			return null;
		}

		if (bOnlyIfMediaServer) {
			PluginInterface pi = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByID(
					"azupnpav");
			if (pi == null || !pi.isOperational() || pi.isDisabled()) {
				skinObject.getControl().setVisible(false);
				return null;
			}
		}
		final SWTSkinButtonUtility btn = new SWTSkinButtonUtility(skinObject);

		btn.addSelectionListener(new SWTSkinButtonUtility.ButtonListenerAdapter() {
			public void pressed(SWTSkinButtonUtility buttonUtility) {
				TableRowCore[] selectedRows = view.getSelectedRows();
				if (selectedRows.length <= 0) {
					return;
				}
				playOrStreamDataSource(selectedRows[0].getDataSource(true), btn);
			}
		});

		view.addSelectionListener(new TableSelectionAdapter() {
			public void deselected(TableRowCore[] rows) {
				update();
			}

			public void selected(TableRowCore[] rows) {
				update();
			}

			public void focusChanged(TableRowCore focusedRow) {
				update();
			}

			private void update() {
				boolean bDisabled = view.getSelectedRowsSize() != 1;
				if (!bDisabled) {
					TableRowCore[] rows = view.getSelectedRows();
					Object ds = rows[0].getDataSource(true);
					DownloadManager dm = getDMFromDS(ds);
					if (dm == null) {
						bDisabled = getAssetHashFromDS(ds) == null;
					} else {
						bDisabled = !canPlay(dm);
					}
				}
				btn.setDisabled(bDisabled);
			}

			public void defaultSelected(TableRowCore[] rows) {
				if (rows.length == 1) {
					playOrStreamDataSource(rows[0].getDataSource(true), btn);
				}
			}
		}, true);

		debugDCAD("exit - addPlayButton");

		return btn;
	}

	public static void playOrStreamDataSource(Object ds, SWTSkinButtonUtility btn) {

		debugDCAD("enter - playOrStreamDataSource");

		DownloadManager dm = getDMFromDS(ds);
		if (dm == null) {
			downloadDataSource(ds);
		} else {
			playOrStream(dm, btn);
		}

		debugDCAD("exit - playOrStreamDataSource");

	}
	
	public static void downloadDataSource(Object ds) {
		TOTorrent torrent = getTorrentFromDS(ds);
		if (torrent != null) {
			TorrentUIUtilsV3.addTorrentToGM(torrent);
		} else {
			String hash = getAssetHashFromDS(ds);
			if (hash != null) {
				// Note: the only case where there's no DM, and a hash present is
				//       in a VuzeNewsEntry, which is displayed on the Dashboard's
				//       Activity tab.  For now, we hardcode the referal to that
				//       but we really should pass it in somehow
				String url = Constants.URL_PREFIX + Constants.URL_DOWNLOAD + hash
						+ ".torrent?referal=playdashboardactivity";
				AzureusCore core = AzureusCoreFactory.getSingleton();
				TorrentUIUtilsV3.loadTorrent(core, url, null, true, false, true);
			}
		}
	}

	/**
	 * @param skin
	 * @param prefix
	 * @param view
	 * @return
	 *
	 * @since 3.0.4.3
	 */
	public static SWTSkinButtonUtility addColumnSetupButton(SWTSkin skin,
			String PREFIX, final ListView view) {
		SWTSkinObject skinObject = skin.getSkinObject(PREFIX + "columnsetup");
		if (skinObject == null) {
			return null;
		}

		SWTSkinButtonUtility btn = new SWTSkinButtonUtility(skinObject);
		btn.addSelectionListener(new ButtonListenerAdapter() {
			public void pressed(SWTSkinButtonUtility buttonUtility) {
				String tableID = view.getTableID();
				new TableColumnEditorWindow(view.getComposite().getShell(), tableID,
						view.getAllColumns(), view.getFocusedRow(),
						TableStructureEventDispatcher.getInstance(tableID));
			}
		});

		return btn;
	}

	public static SWTSkinButtonUtility addDeleteButton(SWTSkin skin,
			String PREFIX, final ListView view) {
		SWTSkinObject skinObject = skin.getSkinObject(PREFIX + "delete");

		SWTSkinButtonUtility btn = new SWTSkinButtonUtility(skinObject);

		btn.addSelectionListener(new SWTSkinButtonUtility.ButtonListenerAdapter() {
			public void pressed(SWTSkinButtonUtility buttonUtility) {
				TableRowCore[] selectedRows = view.getSelectedRows();
				for (int i = 0; i < selectedRows.length; i++) {

					DownloadManager dm = getDMFromDS(selectedRows[i].getDataSource(true));
					if (dm != null) {
						TorrentListViewsUtils.removeDownload(dm, view, true, true);
					}
				}
			}
		});

		return btn;
	}

	public static boolean canPlay(DownloadManager dm) {
		if (dm == null) {
			return false;
		}
		TOTorrent torrent = dm.getTorrent();
		if (!PlatformTorrentUtils.isContent(torrent, false)) {
			return false;
		}

		return dm.getAssumedComplete() || canUseEMP(torrent);
	}

	public static boolean canUseEMP(TOTorrent torrent) {
		if (!PlatformTorrentUtils.useEMP(torrent)
				|| !PlatformTorrentUtils.embeddedPlayerAvail()) {
			return false;
		}

		return canProgressiveOrIsComplete(torrent);
	}

	private static boolean canProgressiveOrIsComplete(TOTorrent torrent) {
		if (torrent == null) {
			return false;
		}
		try {
			EnhancedDownloadManager edm = DownloadManagerEnhancer.getSingleton().getEnhancedDownload(
					torrent.getHash());

			if (edm == null) {
				return false;
			}

			boolean complete = edm.getDownloadManager().isDownloadComplete(false);
			if (complete) {
				return true;
			}

			// not complete
			if (!edm.supportsProgressiveMode()) {
				return false;
			}
		} catch (TOTorrentException e) {
			return false;
		}

		return true;
	}

	public static boolean prepareForPlay(DownloadManager dm) {
		EnhancedDownloadManager edm = DownloadManagerEnhancer.getSingleton().getEnhancedDownload(
				dm);

		if (edm != null) {

			edm.setProgressiveMode(true);

			return (true);
		}

		return (false);
	}

	public static boolean playOrStream(final DownloadManager dm) {
		return playOrStream(dm, null);
	}

	public static boolean playOrStream(final DownloadManager dm,
			final SWTSkinButtonUtility btn) {

		debugDCAD("enter - playOrStream");

		if (dm == null) {
			return false;
		}

		//		if (!canPlay(dm)) {
		//			return false;
		//		}

		TOTorrent torrent = dm.getTorrent();
		if (canUseEMP(torrent)) {
			debug("Can use EMP");

			if (openInEMP(dm)) {
				return true;
			} else {
				debug("Open EMP Failed");
			}
			// fallback to normal
		} else {
			debug("Can't use EMP. torrent says "
					+ PlatformTorrentUtils.useEMP(torrent));
		}

		if (btn != null) {
			btn.setDisabled(true);
		}

		boolean reenableButton = false;
		try {
			if (!canProgressiveOrIsComplete(torrent)) {
				return false;
			}

			File file;
			String sFile = null;

			EnhancedDownloadManager edm = DownloadManagerEnhancer.getSingleton().getEnhancedDownload(
					dm);
			if (edm != null) {
				boolean doProgressive = edm.getProgressiveMode();
				if (doProgressive && edm.getProgressivePlayETA() > 0) {
					return false;
				}

				if (!doProgressive && dm.getDiskManagerFileInfo().length > 1
						&& PlatformTorrentUtils.getContentPrimaryFileIndex(torrent) == -1) {
					// multi-file torrent that we aren't progressive playing or useEMPing
					Utils.launch(dm.getSaveLocation().getAbsolutePath());
					return true;
				}

				file = edm.getPrimaryFile().getFile(true);
				sFile = file.getAbsolutePath();
			} else {
				sFile = dm.getDownloadState().getPrimaryFile();
				file = new File(sFile);
			}

			String ext = FileUtil.getExtension(sFile);

			boolean untrusted = isUntrustworthyContent(ext);
			boolean trusted = isTrustedContent(ext);

			if (untrusted || !trusted) {
				String sPrefix = untrusted ? "v3.mb.notTrusted."
						: "v3.mb.UnknownContent.";

				UIFunctionsSWT functionsSWT = UIFunctionsManagerSWT.getUIFunctionsSWT();
				if (functionsSWT == null) {
					return false;
				}
				Program program = Program.findProgram(ext);
				String sTextID;
				String sFileType;
				if (program == null) {
					sTextID = sPrefix + "noapp.text";
					sFileType = ext;
				} else {
					sTextID = sPrefix + "text";
					sFileType = program.getName();
				}

				MessageBoxShell mb = new MessageBoxShell(functionsSWT.getMainShell(),
						MessageText.getString(sPrefix + "title"), MessageText.getString(
								sTextID, new String[] {
									dm.getDisplayName(),
									sFileType,
									ext
								}), new String[] {
							MessageText.getString(sPrefix + "button.run"),
							MessageText.getString(sPrefix + "button.cancel")
						}, 1);
				mb.setRelatedObject(dm);
				int i = mb.open();
				if (i != 0) {
					return false;
				}
			}

			boolean bComplete = dm.isDownloadComplete(false);

			if (bComplete) {
				if (PlatformTorrentUtils.isContentAdEnabled(torrent)) {
					final String sfFile = sFile;
					debug("calling createASX from ...Tor.Utils.playOrStream, in is complete block.");
					DCAdManager.getInstance().createASX(dm,
							new DCAdManager.ASXCreatedListener() {
								public void asxCreated(File asxFile) {
									if (btn != null) {
										btn.setDisabled(false);
									}
									runFile(dm.getTorrent(), asxFile.getAbsolutePath());
								}

								public void asxFailed() {
									if (btn != null) {
										btn.setDisabled(false);
									}
									runFile(dm.getTorrent(), sfFile);
								}
							});
				} else {
					reenableButton = true;
					runFile(dm.getTorrent(), sFile);
				}
			} else {
				reenableButton = true;
				try {
					playViaMediaServer(DownloadManagerImpl.getDownloadStatic(dm));
				} catch (DownloadException e) {
					Debug.out(e);
				}
			}
		} finally {
			if (btn != null && reenableButton) {
				btn.setDisabled(false);
			}
		}

		debugDCAD("enter - playOrStream");

		return true;
	}

	/**
	 * @param string
	 *
	 * @since 3.0.3.3
	 */
	private static void debug(String string) {
		if (org.gudy.azureus2.core3.util.Constants.isCVSVersion()) {
			System.out.println(string);
		}
	}

	private static void runFile(TOTorrent torrent, String runFile) {
		runFile(torrent, runFile, false);
	}

	private static void runFile(final TOTorrent torrent, final String runFile,
			final boolean forceWMP) {

		AEThread2 thread = new AEThread2("runFile", true) {
			public void run() {

				Utils.execSWTThread(new AERunnable() {

					public void runSupport() {
						debugDCAD("enter - runFile - runSupport");

						if (canUseEMP(torrent)) {
							Debug.out("Shouldn't call runFile with EMP torrent.");
						}

						if (PlatformTorrentUtils.isContentDRM(torrent) || forceWMP) {
							if (!runInMediaPlayer(runFile)) {
								Utils.launch(runFile);
							}
						} else {
							Utils.launch(runFile);
						}

						debugDCAD("exit - runFile - runSupport");
					}

				});

			}

		};
		thread.start();
	}

	/**
	 * New version accepts map with ASX parameters. If the params are null then is uses the
	 * old version to start the player. If the
	 *
	 *
	 * @param dm - DownloadManager
	 * @return - boolean
	 * @since 3.0.4.4 -
	 */
	private static boolean openInEMP(DownloadManager dm) {

		debugDCAD("enter - openInEMP");

		Class epwClass = null;
		try {
			PluginInterface pi = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByID(
					"azemp");

			if (pi == null) {

				return (false);
			}

			epwClass = pi.getPlugin().getClass().getClassLoader().loadClass(
					"com.azureus.plugins.azemp.ui.swt.emp.EmbeddedPlayerWindowSWT");

		} catch (ClassNotFoundException e1) {
			return false;
		}

		//Data is passed to the openWindow via download manager.
		try {
			debug("EmbeddedPlayerWindowSWT - openWindow");
			Method method = epwClass.getMethod("openWindow", new Class[] {
				DownloadManager.class
			});

			method.invoke(null, new Object[] {
				dm
			});

			return true;
		} catch (Throwable e) {
			e.printStackTrace();
			if (e.getMessage() == null
					|| !e.getMessage().toLowerCase().endsWith("only")) {
				Debug.out(e);
			}
		}

		return false;
	}//openInEMP

	/**
	* @param dm
	*
	* @since 3.0.0.7
	*/
	private static void handleNoFileExists(DownloadManager dm) {
		UIFunctionsSWT functionsSWT = UIFunctionsManagerSWT.getUIFunctionsSWT();
		if (functionsSWT == null) {
			return;
		}
		ManagerUtils.start(dm);

		String sPrefix = "v3.mb.PlayFileNotFound.";
		MessageBoxShell mb = new MessageBoxShell(functionsSWT.getMainShell(),
				MessageText.getString(sPrefix + "title"), MessageText.getString(sPrefix
						+ "text", new String[] {
					dm.getDisplayName(),
				}), new String[] {
					MessageText.getString(sPrefix + "button.remove"),
					MessageText.getString(sPrefix + "button.redownload"),
					MessageText.getString("Button.cancel"),
				}, 2);
		mb.setRelatedObject(dm);
		int i = mb.open();

		if (i == 0) {
			ManagerUtils.remove(dm, functionsSWT.getMainShell(), true, false);
		} else if (i == 1) {
			dm.forceRecheck(new ForceRecheckListener() {
				public void forceRecheckComplete(DownloadManager dm) {
					ManagerUtils.start(dm);
				}
			});
		}
	}

	/**
	 * @param string
	 */
	private static boolean runInMediaPlayer(String mediaFile) {

		debugDCAD("enter - runInMediaPlayer");

		if (Constants.isWindows) {
			String wmpEXE = Win32Utils.getWMP();
			if (new File(wmpEXE).exists()) {
				try {
					Runtime.getRuntime().exec(wmpEXE + " \"" + mediaFile + "\"");
					return true;
				} catch (IOException e) {
					Debug.out("error playing " + mediaFile + " via WMP " + mediaFile, e);
				}
			}
		}
		return false;
	}

	private static boolean isTrustedContent(String ext) {
		PluginInterface pi = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByID(
				"azupnpav");

		ArrayList whiteList = new ArrayList();
		String[] goodExts = null;
		if (pi != null && pi.isOperational()) {
			try {
				goodExts = (String[]) pi.getIPC().invoke("getRecognizedExtensions",
						null);
			} catch (Throwable e) {
				//e.printStackTrace();
			}
		}

		if (goodExts == null) {
			// some defaults if media server isn't installed
			goodExts = new String[] {
				"mpg",
				"avi",
				"mov",
				"flv",
				"flc",
				"mp4",
				"mpeg",
				"divx",
				"wmv",
				"asf",
				"mp3",
				"wma",
				"wav",
				"h264",
				"mkv",
			};
		}

		for (int i = 0; i < goodExts.length; i++) {
			Program program = Program.findProgram(goodExts[i]);
			if (program != null) {
				String name = program.getName();
				if (!whiteList.contains(name)) {
					whiteList.add(name);
				}
			}
		}

		Program program = Program.findProgram(ext);
		if (program == null) {
			return false;
		}
		return whiteList.contains(program.getName());
	}

	private static boolean isUntrustworthyContent(String ext) {
		// must be sorted
		final String[] badExts = new String[] {
			"bas",
			"bat",
			"com",
			"cmd",
			"cpl",
			"exe",
			"js",
			"lnk",
			"mdb",
			"msi",
			"osx",
			"pif",
			"reg",
			"scr",
			"vb",
			"vbe",
			"vbs",
			"wsh",
			"wsf",
		};

		if (ext.startsWith(".")) {
			ext = ext.substring(1);
		}
		return Arrays.binarySearch(badExts, ext) >= 0;
	}

	public static String getMediaServerContentURL(DownloadManager dm) {
		try {
			return getMediaServerContentURL(DownloadManagerImpl.getDownloadStatic(dm));
		} catch (DownloadException e) {
		}
		return null;
	}

	/**
	 * @param dl
	 *
	 * @since 3.0.2.3
	 */
	private static String getMediaServerContentURL(Download dl) {

		debugDCAD("enter - getMediaServerContentURL");

		PluginManager pm = AzureusCoreFactory.getSingleton().getPluginManager();
		PluginInterface pi = pm.getPluginInterfaceByID("azupnpav");

		if (pi == null) {
			Logger.log(new LogEvent(LogIDs.UI3, "Media server plugin not found"));
			return null;
		}

		if (!pi.isOperational()) {
			Logger.log(new LogEvent(LogIDs.UI3, "Media server plugin not operational"));
			return null;
		}

		try {
			Program program = Program.findProgram(".qtl");
			boolean hasQuickTime = program == null ? false
					: (program.getName().toLowerCase().indexOf("quicktime") != -1);

			pi.getIPC().invoke("setQuickTimeAvailable", new Object[] {
				new Boolean(hasQuickTime)
			});

			Object url = pi.getIPC().invoke("getContentURL", new Object[] {
				dl
			});
			if (url instanceof String) {
				return (String) url;
			}
		} catch (Throwable e) {
			Logger.log(new LogEvent(LogIDs.UI3, LogEvent.LT_WARNING,
					"IPC to media server plugin failed", e));
		}

		return null;
	}

	/**
	 * 
	 */
	public static void playViaMediaServer(Download download) {

		debugDCAD("enter - playViaMediaServer");

		try {
			final DownloadManager dm = ((DownloadImpl) download).getDownload();

			TOTorrent torrent = dm.getTorrent();
			if (PlatformTorrentUtils.isContentAdEnabled(torrent)) {
				debug("calling createASX from ...Tor.Utils.playViaMediaServer, in is complete block. dm="
						+ dm);
				DCAdManager.getInstance().createASX(dm,
						new DCAdManager.ASXCreatedListener() {
							public void asxCreated(File asxFile) {
								runFile(dm.getTorrent(), asxFile.getAbsolutePath(), true);
							}

							public void asxFailed() {
								runFile(dm.getTorrent(), getContentUrl(dm), true);
							}
						});
			} else {
				// force to WMP if we aren't using EMP
				runFile(torrent, getContentUrl(dm), true);
			}
		} catch (Throwable e) {
			Logger.log(new LogEvent(LogIDs.UI3, "IPC to media server plugin failed",
					e));
		}
	}

	/**
	 * @param view
	 * @param buttonsNeedingRow
	 * @param buttonsNeedingPlatform
	 * @param buttonsNeedingSingleSelection 
	 * @param btnStop
	 */
	public static void addButtonSelectionDisabler(final ListView view,
			final SWTSkinButtonUtility[] buttonsNeedingRow,
			final SWTSkinButtonUtility[] buttonsNeedingPlatform,
			final SWTSkinButtonUtility[] buttonsNeedingSingleSelection,
			final SWTSkinButtonUtility btnStop) {

		view.addSelectionListener(new TableSelectionAdapter() {
			public void deselected(TableRowCore[] rows) {
				update();
			}

			public void selected(TableRowCore[] rows) {
				update();
			}

			public void focusChanged(TableRowCore focusedRow) {
				update();
			}

			private void update() {
				int size = view.getSelectedRowsSize();
				boolean bDisabled = size == 0;

				for (int i = 0; i < buttonsNeedingRow.length; i++) {
					if (buttonsNeedingRow[i] != null) {
						buttonsNeedingRow[i].setDisabled(bDisabled);
					}
				}

				// now for buttons that require platform torrents
				if (!bDisabled) {
					TableRowCore[] rows = view.getSelectedRows();
					for (int i = 0; i < rows.length; i++) {
						TableRowCore row = rows[i];
						Object ds = row.getDataSource(true);
						DownloadManager dm = getDMFromDS(ds);
						if (dm == null && (ds instanceof VuzeActivitiesEntry)) {
							if (((VuzeActivitiesEntry) ds).getAssetHash() == null) {
								bDisabled = true;
								break;
							}
						} else if (!PlatformTorrentUtils.isContent(dm.getTorrent(), true)) {
							bDisabled = true;
							break;
						}
					}
				}
				for (int i = 0; i < buttonsNeedingPlatform.length; i++) {
					if (buttonsNeedingPlatform[i] != null) {
						buttonsNeedingPlatform[i].setDisabled(bDisabled);
					}
				}

				// buttons needing single selection
				if (size > 1) {
					for (int i = 0; i < buttonsNeedingSingleSelection.length; i++) {
						if (buttonsNeedingSingleSelection[i] != null) {
							buttonsNeedingSingleSelection[i].setDisabled(true);
						}
					}
				}

				if (btnStop != null) {
					StartStopButtonUtil.updateStopButton(view, btnStop);
				}
			}
		}, true);
	}

	public static void removeDownload(final DownloadManager dm,
			final TableView tableView, final boolean bDeleteTorrent,
			final boolean bDeleteData) {

		debug("removeDownload");

		tableView.removeDataSource(dm, true);

		AERunnable failure = new AERunnable() {
			public void runSupport() {
				tableView.addDataSource(dm, true);
			}
		};

		if (PublishUtils.isPublished(dm)) {
			ManagerUtils.remove(dm, null, false, false, failure);
		} else {
			ManagerUtils.remove(dm, null, bDeleteTorrent, bDeleteData, failure);
		}
	}

	public static void main(String[] args) {
		AzureusCoreFactory.create();
		System.out.println(isTrustedContent(FileUtil.getExtension("moo.exep")));
		System.out.println(isUntrustworthyContent(FileUtil.getExtension("moo.exe")));
	}

	/**
	 * @param dm
	 *
	 * @since 3.0.2.3
	 */
	public static void showHomeHint(final DownloadManager dm) {
		// Show a popup when user adds a download
		// if it wasn't added recently, it's not a new download
		if (SystemTime.getCurrentTime()
				- dm.getDownloadState().getLongParameter(
						DownloadManagerState.PARAM_DOWNLOAD_ADDED_TIME) < 10000
				&& !PublishUtils.isPublished(dm)
				&& !dm.getDownloadState().getFlag(DownloadManagerState.FLAG_LOW_NOISE)) {
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					SWTSkin skin = SWTSkinFactory.getInstance();
					SWTSkinTabSet tabSetMain = skin.getTabSet(SkinConstants.TABSET_MAIN);
					if (tabSetMain != null
							&& !tabSetMain.getActiveTab().getSkinObjectID().equals(
									"maintabs.home")) {
						Shell shell = tabSetMain.getActiveTab().getControl().getShell();
						Display current = Display.getCurrent();
						// checking focusControl for null doesn't really work
						// Preferably, we'd check to see if the app has the OS' focus
						// and not display the popup when it doesn't
						if (current != null && current.getFocusControl() != null
								&& !MessageBoxShell.isOpen()) {
							int ret = MessageBoxShell.open(shell,
									MessageText.getString("v3.HomeReminder.title"),
									MessageText.getString("v3.HomeReminder.text", new String[] {
										dm.getDisplayName()
									}), new String[] {
										MessageText.getString("Button.ok"),
										MessageText.getString("v3.HomeReminder.gohome")
									}, 0, "downloadinhome",
									MessageText.getString("MessageBoxWindow.nomoreprompting"),
									false, 15000);

							if (ret == 1) {
								tabSetMain.setActiveTab(SkinConstants.VIEWID_HOME_TAB);
							}
						}
					}
				}
			});
		}
	}

	private static void debugDCAD(String s) {
		PlatformDCAdManager.debug("TorrentListViewsUtils: " + s);
	}//debugDCAD

	/**
	 * @param dmContent
	 * @return
	 *
	 * @since 3.0.4.3
	 */
	public static String getContentUrl(DownloadManager dmContent) {
		String contentPath;
		if (dmContent.isDownloadComplete(false)) {
			//use the file path if download is complete.
			EnhancedDownloadManager edm = DownloadManagerEnhancer.getSingleton().getEnhancedDownload(
					dmContent);
			File file;
			if (edm != null) {
				file = edm.getPrimaryFile().getFile(true);
			} else {
				file = new File(dmContent.getDownloadState().getPrimaryFile());
			}
			try {
				contentPath = file.toURL().toString();
			} catch (MalformedURLException e) {
				contentPath = file.getAbsolutePath();
			}
		} else {
			//use the stream path if download is not complete.
			contentPath = TorrentListViewsUtils.getMediaServerContentURL(dmContent);
		}
		return contentPath;
	}
}
