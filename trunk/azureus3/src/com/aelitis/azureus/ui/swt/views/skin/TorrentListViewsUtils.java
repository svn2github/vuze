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
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.download.ForceRecheckListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.util.*;
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
import com.aelitis.azureus.ui.common.table.*;
import com.aelitis.azureus.ui.selectedcontent.ISelectedContent;
import com.aelitis.azureus.ui.selectedcontent.DownloadUrlInfo;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.browser.listener.DownloadUrlInfoSWT;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.utils.TorrentUIUtilsV3;
import com.aelitis.azureus.ui.swt.views.TorrentListView;
import com.aelitis.azureus.ui.swt.views.TorrentListViewListener;
import com.aelitis.azureus.ui.swt.views.list.ListView;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarEntrySWT;
import com.aelitis.azureus.util.*;
import com.aelitis.azureus.util.Constants;
import com.aelitis.azureus.util.win32.Win32Utils;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;

import org.gudy.azureus2.pluginsimpl.local.download.DownloadImpl;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadManagerImpl;

/**
 * @author TuxPaper
 * @created Oct 12, 2006
 *
 */
public class TorrentListViewsUtils
{

	public static final boolean ENABLE_ON_HOVER = false;

	public static SWTSkinButtonUtility addShareButton(final SkinView skinView,
			final String PREFIX, final ListView view) {
		SWTSkinObject skinObject = skinView.getSkinObject(PREFIX + "send-selected");
		if (skinObject == null) {
			return null;
		}

		SWTSkinButtonUtility btn = new SWTSkinButtonUtility(skinObject);

		btn.addSelectionListener(new SWTSkinButtonUtility.ButtonListenerAdapter() {
			public void pressed(SWTSkinButtonUtility buttonUtility) {
				ISelectedContent[] contents = SelectedContentManager.getCurrentlySelectedContent();
				if (contents.length > 0) {
					/*
					 * KN: we're only supporting sharing a single content right now
					 */
					VuzeShareUtils.getInstance().shareTorrent(contents[0], PREFIX + "btn");
				}
			}
		});
		btn.setDisabled(true);
		return btn;
	}

	public static SWTSkinButtonUtility addNewTagButton(final SkinView skinView,
			final String PREFIX, final ListView view) {
		SWTSkinObject skinObject = skinView.getSkinObject(PREFIX + "newtag");
		if (skinObject == null) {
			return null;
		}

		final SWTSkinButtonUtility btn = new SWTSkinButtonUtility(skinObject);

		btn.addSelectionListener(new SWTSkinButtonUtility.ButtonListenerAdapter() {
			public void pressed(SWTSkinButtonUtility buttonUtility) {
				ISelectedContent[] contents = SelectedContentManager.getCurrentlySelectedContent();
				if (contents.length > 0) {
					/*
					 * KN: we're only supporting sharing a single content right now
					 */
					VuzeShareUtils.getInstance().shareTorrent(contents[0],
							PREFIX + "tag-btn");
				}
			}
		});
		return btn;
	}

	public static SWTSkinButtonUtility addStopButton(final SkinView skinView,
			String PREFIX, final TorrentListView view) {
		SWTSkinObject skinObject = skinView.getSkinObject(PREFIX + "stop");
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

	public static SWTSkinButtonUtility addDetailsButton(final SkinView skinView,
			final String PREFIX, final ListView view) {
		SWTSkinObject skinObject = skinView.getSkinObject(PREFIX + "viewdetails");
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
		viewDetails(DataSourceUtils.getHash(row.getDataSource(true)), ref);
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

	public static SWTSkinButtonUtility addCommentsButton(final SkinView skinView,
			String PREFIX, final ListView view) {
		SWTSkinObject skinObject = skinView.getSkinObject(PREFIX + "comment");
		if (skinObject == null) {
			return null;
		}

		final SWTSkinButtonUtility btn = new SWTSkinButtonUtility(skinObject);

		btn.addSelectionListener(new SWTSkinButtonUtility.ButtonListenerAdapter() {
			public void pressed(SWTSkinButtonUtility buttonUtility) {
				TableRowCore[] selectedRows = view.getSelectedRows();
				if (selectedRows.length > 0) {
					String hash = DataSourceUtils.getHash(selectedRows[0].getDataSource(true));

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

	public static SWTSkinButtonUtility addPlayButton(final SkinView skinView,
			String PREFIX, final ListView view, boolean bOnlyIfMediaServer,
			boolean bPlayOnDoubleClick) {

		debugDCAD("enter - addPlayButton");

		SWTSkinObject skinObject = skinView.getSkinObject(PREFIX + "play");
		if (skinObject == null) {
			return null;
		}

		if (bOnlyIfMediaServer) {
			PluginInterface pi = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByID(
					"azupnpav", true);
			if (pi == null) {
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
			public void mouseEnter(TableRowCore row) {
				if (ENABLE_ON_HOVER) {
					update();
				}
			}

			public void mouseExit(TableRowCore row) {
				if (ENABLE_ON_HOVER) {
					update();
				}
			}

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
				TableRowCore rowWithCursor = ENABLE_ON_HOVER
						? view.getTableRowWithCursor() : null;

				boolean bDisabled = rowWithCursor == null
						? view.getSelectedRowsSize() != 1 : false;
				if (!bDisabled) {
					TableRowCore[] rows = rowWithCursor == null ? view.getSelectedRows()
							: new TableRowCore[] {
								rowWithCursor
							};
					Object ds = rows[0].getDataSource(true);
					bDisabled = !PlayUtils.canPlayDS(ds);

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
		String referal = "unknown";
		if (ds instanceof VuzeActivitiesEntry) {
			referal = "playdashboardactivity";
		} else if (ds instanceof DownloadManager) {
			referal = "playdownloadmanager";
		} else if (ds instanceof ISelectedContent) {
			referal = "selectedcontent";
		} else {
			referal = "unknown";
		}
		playOrStreamDataSource(ds, btn, referal);
	}

	public static void playOrStreamDataSource(Object ds,
			SWTSkinButtonUtility btn, String referal) {

		debugDCAD("enter - playOrStreamDataSource");

		DownloadManager dm = DataSourceUtils.getDM(ds);
		if (dm == null) {
			downloadDataSource(ds, true, referal);
		} else {
			playOrStream(dm, btn);
		}

		debugDCAD("exit - playOrStreamDataSource");

	}

	public static void downloadDataSource(Object ds, boolean playNow,
			String referal) {
		TOTorrent torrent = DataSourceUtils.getTorrent(ds);
		// we want to re-download the torrent if it's ours, since the existing
		// one is likely stale
		if (torrent != null && !DataSourceUtils.isPlatformContent(ds)) {
			TorrentUIUtilsV3.addTorrentToGM(torrent);
		} else {
			AzureusCore core = AzureusCoreFactory.getSingleton();
			DownloadUrlInfo dlInfo = DataSourceUtils.getDownloadInfo(ds);
			if (dlInfo instanceof DownloadUrlInfoSWT) {
				TorrentUIUtilsV3.loadTorrent(core, dlInfo, playNow, false,
						true, true);
				return;
			}

			String hash = DataSourceUtils.getHash(ds);
			if (hash != null) {
				if (ds instanceof VuzeActivitiesEntry) {
					if (((VuzeActivitiesEntry) ds).isDRM()) {
						TorrentListViewsUtils.viewDetails(hash, "drm-play");
						return;
					}
				}

				String url = Constants.URL_PREFIX + Constants.URL_DOWNLOAD + hash
						+ ".torrent?referal=" + referal;
				dlInfo = new DownloadUrlInfo(url);
				TorrentUIUtilsV3.loadTorrent(core, dlInfo, playNow, false, true, true);
			} else if (dlInfo != null) {
				TorrentUIUtilsV3.loadTorrent(core, dlInfo, playNow, false,
						true, true);
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
	public static SWTSkinButtonUtility addColumnSetupButton(SkinView skinView,
			String PREFIX, final ListView view) {
		SWTSkinObject skinObject = skinView.getSkinObject(PREFIX + "columnsetup");
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

	public static SWTSkinButtonUtility addDeleteButton(SkinView skinView,
			String PREFIX, final ListView view) {
		SWTSkinObject skinObject = skinView.getSkinObject(PREFIX + "delete");
		if (skinObject == null) {
			return null;
		}

		SWTSkinButtonUtility btn = new SWTSkinButtonUtility(skinObject);

		btn.addSelectionListener(new SWTSkinButtonUtility.ButtonListenerAdapter() {
			public void pressed(SWTSkinButtonUtility buttonUtility) {
				TableRowCore[] selectedRows = view.getSelectedRows();
				for (int i = 0; i < selectedRows.length; i++) {

					DownloadManager dm = DataSourceUtils.getDM(selectedRows[i].getDataSource(true));
					if (dm != null) {
						boolean delete = ! dm.getDownloadState().getFlag(Download.FLAG_DO_NOT_DELETE_DATA_ON_REMOVE);
						TorrentListViewsUtils.removeDownload(dm, view, true, delete);
					}
				}
			}
		});

		return btn;
	}

	public static boolean playOrStream(final DownloadManager dm,
			final SWTSkinButtonUtility btn) {
		boolean played = _playOrStream(dm, btn);
		if (played) {
			PlatformTorrentUtils.setHasBeenOpened(dm.getTorrent(), true);
		}
		return played;
	}

	private static boolean _playOrStream(final DownloadManager dm,
			final SWTSkinButtonUtility btn) {

		debugDCAD("enter - playOrStream");

		if (dm == null) {
			return false;
		}

		//		if (!canPlay(dm)) {
		//			return false;
		//		}

		TOTorrent torrent = dm.getTorrent();
		if (PlayUtils.canUseEMP(torrent)) {
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
			if (!PlayUtils.canProgressiveOrIsComplete(torrent)) {
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

			//if (untrusted || !trusted) {
			String sPrefix = "v3.mb.openFile.";
			

			UIFunctionsSWT functionsSWT = UIFunctionsManagerSWT.getUIFunctionsSWT();
			if (functionsSWT == null) {
				return false;
			}
			
			Program program = Program.findProgram(ext);
			String sTextID;
			String sFileType;
			if (program == null) {
				sTextID = sPrefix + "text.unknown";
				sFileType = ext;
			} else {
				sTextID = sPrefix + "text.known";
				sFileType = program.getName();
			}
			
			String[] buttons = new String[(program == null ? 2 : 3)];
			buttons[0] = MessageText.getString(sPrefix + "button.guide");
			buttons[buttons.length-1] = MessageText.getString(sPrefix + "button.cancel");
			
			MessageBoxShell mb = null;
			if(program != null) {
				buttons[1] = MessageText.getString(sPrefix + "button.play");
				mb = new MessageBoxShell(functionsSWT.getMainShell(),
						MessageText.getString(sPrefix + "title"), MessageText.getString(
								sTextID, new String[] {
									dm.getDisplayName(),
									sFileType,
									ext
								}), buttons, 0, sPrefix + ".remember_id", MessageText.getString(sPrefix
								+ "remember"), false, 0);
				mb.setRememberOnlyIfButton(1);
				mb.setRelatedObject(dm);
			} else {
				mb = new MessageBoxShell(functionsSWT.getMainShell(),
						MessageText.getString(sPrefix + "title"), MessageText.getString(
								sTextID, new String[] {
									dm.getDisplayName(),
									sFileType,
									ext
								}), buttons, 0);
				mb.setRelatedObject(dm);
			}

			int i = mb.open();
			
			if(i == 0) {
				String url = MessageText.getString(sPrefix + "guideurl");
				if(UrlUtils.isURL(url)) {
					Utils.launch(url);
				}
			}
			
			if (i != 1 || program == null) {
				return false;
			}
			//}

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

						if (PlayUtils.canUseEMP(torrent)) {
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
				"azupnpav", true);

		ArrayList whiteList = new ArrayList();
		String[] goodExts = null;
		if (pi != null) {
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

	/**
	 * XXX DO NOT USE.  Only for EMP <= 2.0.14 support
	 * @param dm
	 * @return
	 */
	public static String getMediaServerContentURL(DownloadManager dm) {
		try {
			return PlayUtils.getMediaServerContentURL(DownloadManagerImpl.getDownloadStatic(dm));
		} catch (DownloadException e) {
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
								runFile(dm.getTorrent(), PlayUtils.getContentUrl(dm), true);
							}
						});
			} else {
				// force to WMP if we aren't using EMP
				runFile(torrent, PlayUtils.getContentUrl(dm), true);
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
			public void mouseEnter(TableRowCore row) {
				if (ENABLE_ON_HOVER) {
					update();
				}
			}

			public void mouseExit(TableRowCore row) {
				if (ENABLE_ON_HOVER) {
					update();
				}
			}

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
				TableRowCore rowWithCursor = ENABLE_ON_HOVER
						? view.getTableRowWithCursor() : null;

				boolean bDisabled;
				int size;
				if (rowWithCursor == null) {
					size = view.getSelectedRowsSize();
					bDisabled = size == 0;
				} else {
					bDisabled = false;
					size = 1;
				}

				for (int i = 0; i < buttonsNeedingRow.length; i++) {
					if (buttonsNeedingRow[i] != null) {
						buttonsNeedingRow[i].setDisabled(bDisabled);
					}
				}

				// now for buttons that require platform torrents
				if (!bDisabled) {
					TableRowCore[] rows = rowWithCursor == null ? view.getSelectedRows()
							: new TableRowCore[] {
								rowWithCursor
							};
					for (int i = 0; i < rows.length; i++) {
						TableRowCore row = rows[i];
						Object ds = row.getDataSource(true);
						boolean ourContent = DataSourceUtils.isPlatformContent(ds);
						if (!ourContent) {
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
					SideBar sideBar = (SideBar) SkinViewManager.getByClass(SideBar.class);
					// 3.2 TODO: properly detect any library
					if (sideBar == null) {
						return;
					}
					SideBarEntrySWT info = sideBar.getCurrentSideBarInfo();
					if (info == null) {
						return;
					}
					if (info.id.equals(SideBar.SIDEBAR_SECTION_LIBRARY)) {
						Display current = Display.getCurrent();
						// checking focusControl for null doesn't really work
						// Preferably, we'd check to see if the app has the OS' focus
						// and not display the popup when it doesn't
						if (current != null && current.getFocusControl() != null
								&& !MessageBoxShell.isOpen()) {
							int ret = MessageBoxShell.open(Utils.findAnyShell(),
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
								sideBar.showItemByID(SideBar.SIDEBAR_SECTION_LIBRARY);
							}
						}
					}
				}
			});
		}
	}

	public static void debugDCAD(String s) {
		PlatformDCAdManager.debug("TorrentListViewsUtils: " + s);
	}//debugDCAD

	public static boolean playOrStream(final DownloadManager dm) {
		return playOrStream(dm, null);
	}
}
