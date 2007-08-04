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
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.ForceRecheckListener;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerDownloadRemovalVetoException;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Alerts;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.download.DownloadManagerEnhancer;
import com.aelitis.azureus.core.download.EnhancedDownloadManager;
import com.aelitis.azureus.core.messenger.config.PlatformConfigMessenger;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.common.table.TableSelectionAdapter;
import com.aelitis.azureus.ui.common.table.TableView;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.shells.BrowserWindow;
import com.aelitis.azureus.ui.swt.skin.SWTSkin;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.utils.PublishUtils;
import com.aelitis.azureus.ui.swt.views.TorrentListView;
import com.aelitis.azureus.ui.swt.views.TorrentListViewListener;
import com.aelitis.azureus.util.AdManager;
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

	public static SWTSkinButtonUtility addShareButton(final SWTSkin skin,
			String PREFIX, final TorrentListView view) {
		SWTSkinObject skinObject = skin.getSkinObject(PREFIX + "send-selected");
		if (skinObject == null) {
			return null;
		}

		SWTSkinButtonUtility btn = new SWTSkinButtonUtility(skinObject);

		btn.addSelectionListener(new SWTSkinButtonUtility.ButtonListenerAdapter() {
			public void pressed(SWTSkinButtonUtility buttonUtility) {
				TableRowCore[] selectedRows = view.getSelectedRows();
				if (selectedRows.length > 0) {
					DownloadManager dm = (DownloadManager) selectedRows[0].getDataSource(true);
					if (dm != null) {
						try {
							String url = Constants.URL_PREFIX + Constants.URL_SHARE
									+ dm.getTorrent().getHashWrapper().toBase32String()
									+ ".html?" + Constants.URL_SUFFIX;
							// temp hackery for alpha
							url = Constants.URL_PREFIX + Constants.URL_DETAILS
									+ dm.getTorrent().getHashWrapper().toBase32String()
									+ ".html#share?" + Constants.URL_SUFFIX;

							UIFunctions functions = UIFunctionsManager.getUIFunctions();
							functions.viewURL(url, "browse", 0, 0, false, false);
						} catch (TOTorrentException e) {
							Debug.out(e);
						}
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
				StartStopButtonUtil.updateStopButton(view, btn);
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
			String PREFIX, final TorrentListView view) {
		SWTSkinObject skinObject = skin.getSkinObject(PREFIX + "viewdetails");
		if (skinObject == null) {
			return null;
		}

		final SWTSkinButtonUtility btn = new SWTSkinButtonUtility(skinObject);

		btn.addSelectionListener(new SWTSkinButtonUtility.ButtonListenerAdapter() {
			public void pressed(SWTSkinButtonUtility buttonUtility) {
				TableRowCore[] selectedRows = view.getSelectedRows();
				if (selectedRows.length > 0) {
					viewDetails(selectedRows[0]);
				}
			}
		});

		return btn;
	}

	public static void viewDetails(TableRowCore row) {
		DownloadManager dm = (DownloadManager) row.getDataSource(true);
		viewDetails(dm);
	}

	public static void viewDetails(DownloadManager dm) {
		if (dm == null) {
			return;
		}
		if (!PlatformTorrentUtils.isContent(dm.getTorrent(), true)) {
			return;
		}

		try {
			String url = Constants.URL_PREFIX + Constants.URL_DETAILS
					+ dm.getTorrent().getHashWrapper().toBase32String() + ".html?"
					+ Constants.URL_SUFFIX;

			UIFunctions functions = UIFunctionsManager.getUIFunctions();
			functions.viewURL(url, "browse", 0, 0, false, false);
		} catch (TOTorrentException e) {
			Debug.out(e);
		}
	}

	public static SWTSkinButtonUtility addCommentsButton(final SWTSkin skin,
			String PREFIX, final TorrentListView view) {
		SWTSkinObject skinObject = skin.getSkinObject(PREFIX + "comment");
		if (skinObject == null) {
			return null;
		}

		final SWTSkinButtonUtility btn = new SWTSkinButtonUtility(skinObject);

		btn.addSelectionListener(new SWTSkinButtonUtility.ButtonListenerAdapter() {
			public void pressed(SWTSkinButtonUtility buttonUtility) {
				TableRowCore[] selectedRows = view.getSelectedRows();
				if (selectedRows.length > 0) {
					DownloadManager dm = (DownloadManager) selectedRows[0].getDataSource(true);
					if (dm != null) {
						try {
							String url = Constants.URL_PREFIX + Constants.URL_COMMENTS
									+ dm.getTorrent().getHashWrapper().toBase32String()
									+ ".html?" + Constants.URL_SUFFIX + "&rnd=" + Math.random();

							UIFunctions functions = UIFunctionsManager.getUIFunctions();
							functions.viewURL(url, "browse", 0, 0, false, false);
						} catch (TOTorrentException e) {
							Debug.out(e);
						}
					}
				}
			}
		});

		return btn;
	}

	public static SWTSkinButtonUtility addPlayButton(final SWTSkin skin,
			String PREFIX, final TorrentListView view, boolean bOnlyIfMediaServer,
			boolean bPlayOnDoubleClick) {
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
				playOrStream((DownloadManager) selectedRows[0].getDataSource(true), btn);
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
					DownloadManager dm = (DownloadManager) rows[0].getDataSource(true);
					if (!dm.isDownloadComplete(false)) {
						DownloadManagerEnhancer dmEnhancer = DownloadManagerEnhancer.getSingleton();
						if (dmEnhancer != null) {
							EnhancedDownloadManager edm = dmEnhancer.getEnhancedDownload(dm);
							if (edm != null
									&& (!edm.supportsProgressiveMode() || edm.getProgressivePlayETA() > 0)) {
								bDisabled = true;
							}
						}
					}
				}
				btn.setDisabled(bDisabled);
			}

			public void defaultSelected(TableRowCore[] rows) {
				if (rows.length == 1) {
					playOrStream((DownloadManager) rows[0].getDataSource(true));
				}
			}
		}, true);

		return btn;
	}

	public static void playOrStream(final DownloadManager dm) {
		playOrStream(dm, null);
	}

	public static void playOrStream(final DownloadManager dm,
			final SWTSkinButtonUtility btn) {
		if (dm == null) {
			return;
		}
		if (btn != null) {
			btn.setDisabled(true);
		}

		boolean reenableButton = false;
		try {
			boolean bComplete = dm.isDownloadComplete(false);

			if (!bComplete) {
				DownloadManagerEnhancer dmEnhancer = DownloadManagerEnhancer.getSingleton();
				if (dmEnhancer != null) {
					EnhancedDownloadManager edm = dmEnhancer.getEnhancedDownload(dm);
					if (edm != null
							&& (!edm.supportsProgressiveMode() || edm.getProgressivePlayETA() > 0)) {
						return;
					}
				}
			}

			File file;
			String sFile = dm.getDownloadState().getPrimaryFile();
			if (sFile == null || sFile.length() == 0 || !new File(sFile).exists()) {
				DiskManagerFileInfo[] diskManagerFileInfo = dm.getDiskManagerFileInfo();
				if (diskManagerFileInfo == null && diskManagerFileInfo.length == 0) {
					return;
				}
				file = diskManagerFileInfo[0].getFile(true);
			} else {
				file = new File(sFile);
			}

			if (!file.exists()) {
				handleNoFileExists(dm);
				return;
			}
			String ext = FileUtil.getExtension(file.getName());

			boolean untrusted = isUntrustworthyContent(ext);
			boolean trusted = isTrustedContent(ext);

			if (untrusted || !trusted) {
				String sPrefix = untrusted ? "v3.mb.notTrusted."
						: "v3.mb.UnknownContent.";

				UIFunctionsSWT functionsSWT = UIFunctionsManagerSWT.getUIFunctionsSWT();
				if (functionsSWT == null) {
					return;
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
					return;
				}
			}

			if (bComplete) {
				TOTorrent torrent = dm.getTorrent();
				if (PlatformTorrentUtils.isContentAdEnabled(torrent)) {
					String url;
					try {
						url = file.toURL().toString();
					} catch (MalformedURLException e) {
						url = sFile;
					}
					AdManager.getInstance().createASX(dm, url,
							new AdManager.ASXCreatedListener() {
								public void asxCreated(File asxFile) {
									if (btn != null) {
										System.out.println("cows2");
										btn.setDisabled(false);
									}
									runFile(dm.getTorrent(), asxFile.getAbsolutePath());
								}

								public void asxFailed() {
									if (btn != null) {
										btn.setDisabled(false);
									}
									runFile(dm.getTorrent(), dm.getSaveLocation().toString());
								}
							});
				} else {
					reenableButton = true;
					runFile(dm.getTorrent(), dm.getSaveLocation().toString());
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
	}

	private static void runFile(TOTorrent torrent, String runFile) {
		if (PlatformTorrentUtils.isContentDRM(torrent)) {
			if (!runInMediaPlayer(runFile)) {
				Utils.launch(runFile);
			}
		} else {
			Utils.launch(runFile);
		}

		if (PlatformTorrentUtils.isContent(torrent, true)) {
			String playAfterURL = PlatformConfigMessenger.getPlayAfterURL();
			if (playAfterURL != null) {
				try {
					if (!playAfterURL.startsWith("http")) {
						playAfterURL = Constants.URL_PREFIX + playAfterURL;
					}
					playAfterURL += playAfterURL.indexOf("?") > 0 ? "&" : "?";
					playAfterURL += "azid=" + Constants.AZID + "&torrentHash="
							+ torrent.getHashWrapper().toBase32String();

					new BrowserWindow(Utils.findAnyShell(), playAfterURL, 0.9, 0.9, true,
							true);
				} catch (Exception e) {
				}
			}
		}
	}

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
		if (Constants.isWindows) {
			String wmpEXE = Win32Utils.getWMP();
			if (new File(wmpEXE).exists()) {
				try {
					Runtime.getRuntime().exec(wmpEXE + " \"" + mediaFile + "\"");
					return true;
				} catch (IOException e) {
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
	 * 
	 */
	public static void playViaMediaServer(Download download) {
		PluginInterface pi = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByID(
				"azupnpav");

		if (pi == null) {

			Logger.log(new LogEvent(LogIDs.UI3, "Media server plugin not found"));

			return;
		}

		if (!pi.isOperational()) {

			Logger.log(new LogEvent(LogIDs.UI3, "Media server plugin not operational"));

			return;
		}

		try {
			Program program = Program.findProgram(".qtl");
			boolean hasQuickTime = program == null ? false
					: (program.getName().toLowerCase().indexOf("quicktime") != -1);

			pi.getIPC().invoke("setQuickTimeAvailable", new Object[] {
				new Boolean(hasQuickTime)
			});
		} catch (Throwable e) {
			Logger.log(new LogEvent(LogIDs.UI3, LogEvent.LT_WARNING,
					"IPC to media server plugin failed", e));
		}

		try {
			final DownloadManager dm = ((DownloadImpl) download).getDownload();

			String url = dm.getSaveLocation().toString();
			try {
				url = new File(url).toURL().toString();
			} catch (MalformedURLException e) {
			}

			Object urlObj = pi.getIPC().invoke("getContentURL", new Object[] {
				download
			});
			if (urlObj instanceof String) {
				url = (String) urlObj;
			}
			final String fURL = url;

			TOTorrent torrent = dm.getTorrent();
			if (PlatformTorrentUtils.isContentAdEnabled(torrent)) {
				AdManager.getInstance().createASX(dm, fURL,
						new AdManager.ASXCreatedListener() {
							public void asxCreated(File asxFile) {
								runFile(dm.getTorrent(), asxFile.getAbsolutePath());
							}

							public void asxFailed() {
								runFile(dm.getTorrent(), fURL);
							}
						});
			} else {
				runFile(dm.getTorrent(), url);
			}
		} catch (Throwable e) {
			try {
				pi.getIPC().invoke("playDownload", new Object[] {
					download
				});
			} catch (Throwable e2) {
				Logger.log(new LogEvent(LogIDs.UI3,
						"IPC to media server plugin failed", e2));
			}
		}
	}

	/**
	 * @param view
	 * @param buttonsNeedingRow
	 * @param buttonsNeedingPlatform
	 * @param buttonsNeedingSingleSelection 
	 * @param btnStop
	 */
	public static void addButtonSelectionDisabler(final TorrentListView view,
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
						DownloadManager dm = (DownloadManager) row.getDataSource(true);
						if (!PlatformTorrentUtils.isContent(dm.getTorrent(), true)) {
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
}
