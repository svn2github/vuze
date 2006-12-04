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

import org.eclipse.swt.widgets.Composite;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.views.table.TableRowCore;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.download.DownloadManagerEnhancer;
import com.aelitis.azureus.core.download.EnhancedDownloadManager;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.views.TorrentListView;
import com.aelitis.azureus.ui.swt.views.TorrentListViewListener;
import com.aelitis.azureus.ui.swt.views.list.ListRow;
import com.aelitis.azureus.ui.swt.views.list.ListSelectionAdapter;
import com.aelitis.azureus.util.Constants;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;

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
				ListRow[] selectedRows = view.getSelectedRows();
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
							skin.setActiveTab("maintabs", "maintabs.browse");

							SWTSkinObject skinObjectBrowser = skin.getSkinObject("browse");
							if (skinObjectBrowser instanceof SWTSkinObjectBrowser) {
								SWTSkinObjectBrowser sob = (SWTSkinObjectBrowser) skinObjectBrowser;
								sob.setURL(url);
							}
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
				ListRow[] selectedRows = view.getSelectedRows();
				for (int i = 0; i < selectedRows.length; i++) {
					DownloadManager dm = (DownloadManager) selectedRows[i].getDataSource(true);
					int state = dm.getState();
					if (state == DownloadManager.STATE_QUEUED
							|| state == DownloadManager.STATE_STOPPED
							|| state == DownloadManager.STATE_STOPPING
							|| state == DownloadManager.STATE_ERROR) {
						ManagerUtils.start(dm);

						StartStopButtonUtil.updateStopButton(view, btn);
					} else {
						ManagerUtils.stop(dm, (Composite) btn.getSkinObject().getControl());
						StartStopButtonUtil.updateStopButton(view, btn);
					}
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

	public static SWTSkinButtonUtility addDetailsButton(final SWTSkin skin,
			String PREFIX, final TorrentListView view) {
		SWTSkinObject skinObject = skin.getSkinObject(PREFIX + "viewdetails");
		if (skinObject == null) {
			return null;
		}

		final SWTSkinButtonUtility btn = new SWTSkinButtonUtility(skinObject);

		btn.addSelectionListener(new SWTSkinButtonUtility.ButtonListenerAdapter() {
			public void pressed(SWTSkinButtonUtility buttonUtility) {
				ListRow[] selectedRows = view.getSelectedRows();
				if (selectedRows.length > 0) {
					viewDetails(skin, selectedRows[0]);
				}
			}
		});

		return btn;
	}

	public static void viewDetails(SWTSkin skin, ListRow row) {
		DownloadManager dm = (DownloadManager) row.getDataSource(true);
		if (dm != null) {
			if (!PlatformTorrentUtils.isContent(dm.getTorrent())) {
				return;
			}

			try {
				String url = Constants.URL_PREFIX + Constants.URL_DETAILS
						+ dm.getTorrent().getHashWrapper().toBase32String() + ".html?"
						+ Constants.URL_SUFFIX;

				UIFunctions functions = UIFunctionsManager.getUIFunctions();
				functions.viewURL(url, "browse", 0, 0, false);
			} catch (TOTorrentException e) {
				Debug.out(e);
			}
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
				ListRow[] selectedRows = view.getSelectedRows();
				if (selectedRows.length > 0) {
					DownloadManager dm = (DownloadManager) selectedRows[0].getDataSource(true);
					if (dm != null) {
						try {
							String url = Constants.URL_PREFIX + Constants.URL_COMMENTS
									+ dm.getTorrent().getHashWrapper().toBase32String()
									+ ".html?" + Constants.URL_SUFFIX;

							UIFunctions functions = UIFunctionsManager.getUIFunctions();
							functions.viewURL(url, "browse", 0, 0, false);
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
					"aeupnpmediaserver");
			if (pi == null || !pi.isOperational() || pi.isDisabled()) {
				skinObject.getControl().setVisible(false);
				return null;
			}
		}
		final SWTSkinButtonUtility btn = new SWTSkinButtonUtility(skinObject);

		btn.addSelectionListener(new SWTSkinButtonUtility.ButtonListenerAdapter() {
			public void pressed(SWTSkinButtonUtility buttonUtility) {
				ListRow[] selectedRows = view.getSelectedRows();
				if (selectedRows.length <= 0) {
					return;
				}
				Download download = (Download) selectedRows[0].getDataSource(false);
				if (download.isComplete(false)) {
					ManagerUtils.run((DownloadManager) selectedRows[0].getDataSource(true));
				} else {
					playViaMediaServer(download);
				}
			}
		});

		view.addSelectionListener(new ListSelectionAdapter() {
			public void deselected(ListRow row) {
				update();
			}

			public void selected(ListRow row) {
				update();
			}

			public void focusChanged(ListRow focusedRow) {
				update();
			}

			private void update() {
				TableRowCore[] rows = view.getSelectedRows();
				boolean bDisabled = rows.length != 1;
				if (!bDisabled) {
					DownloadManager dm = (DownloadManager) rows[0].getDataSource(true);
					if (!dm.isDownloadComplete(false)) {
						DownloadManagerEnhancer dmEnhancer = DownloadManagerEnhancer.getSingleton();
						if (dmEnhancer != null) {
							EnhancedDownloadManager edm = dmEnhancer.getEnhancedDownload(dm);
							if (edm != null) {
								if (edm.getProgressivePlayETA() > 0) {
									bDisabled = true;
								}
							}
						}
					}
				}
				btn.setDisabled(bDisabled);
			}

			public void defaultSelected(ListRow[] rows) {
				if (rows.length == 1) {
					Download download = (Download) rows[0].getDataSource(false);
					if (download.isComplete(false)) {
						ManagerUtils.run((DownloadManager) rows[0].getDataSource(true));
					} else {
						playViaMediaServer(download);
					}
				}
			}
		}, true);

		return btn;
	}

	/**
	 * 
	 */
	public static void playViaMediaServer(Download download) {
		PluginInterface pi = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByID(
				"aeupnpmediaserver");

		if (pi == null) {

			Logger.log(new LogEvent(LogIDs.UI3, "Media server plugin not found"));

			return;
		}

		if (!pi.isOperational()) {

			Logger.log(new LogEvent(LogIDs.UI3, "Media server plugin not operational"));

			return;
		}

		try {
			pi.getIPC().invoke("playDownload", new Object[] { download
			});

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
	public static void addButtonSelectionDisabler(final TorrentListView view,
			final SWTSkinButtonUtility[] buttonsNeedingRow,
			final SWTSkinButtonUtility[] buttonsNeedingPlatform,
			final SWTSkinButtonUtility[] buttonsNeedingSingleSelection,
			final SWTSkinButtonUtility btnStop) {

		view.addSelectionListener(new ListSelectionAdapter() {
			public void deselected(ListRow row) {
				update();
			}

			public void selected(ListRow row) {
				update();
			}

			public void focusChanged(ListRow focusedRow) {
				update();
			}

			private void update() {
				TableRowCore[] rows = view.getSelectedRows();
				boolean bDisabled = rows.length == 0;

				for (int i = 0; i < buttonsNeedingRow.length; i++) {
					if (buttonsNeedingRow[i] != null) {
						buttonsNeedingRow[i].setDisabled(bDisabled);
					}
				}

				// now for buttons that require platform torrents
				if (!bDisabled) {
					for (int i = 0; i < rows.length; i++) {
						TableRowCore row = rows[i];
						DownloadManager dm = (DownloadManager) row.getDataSource(true);
						if (!PlatformTorrentUtils.isContent(dm.getTorrent())) {
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
				if (rows.length > 1) {
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
}
