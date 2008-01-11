/**
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.ui.swt.utils;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerAdapter;
import org.gudy.azureus2.core3.global.GlobalManagerListener;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloader;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderCallBackInterface;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.FileDownloadWindow;
import org.gudy.azureus2.ui.swt.TorrentUtil;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.messenger.config.PlatformConfigMessenger;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.views.skin.TorrentListViewsUtils;
import com.aelitis.azureus.util.Constants;

/**
 * @author TuxPaper
 * @created Sep 16, 2007
 *
 */
public class TorrentUIUtilsV3
{
	//catches http://www.vuze.com/download/CHJW43PLS277RC7U3S5XRS2PZ4UUG7RS.torrent
	private static final Pattern hashPattern = Pattern.compile("download/([A-Z0-9]{32})\\.torrent");

	public static void loadTorrent(final AzureusCore core, String url,
			String referer, final boolean playNow) {
		boolean blocked = PlatformConfigMessenger.isURLBlocked(url);
		// Security: Only allow torrents from whitelisted urls
		if (blocked) {
			Debug.out("stopped loading torrent URL because it's not in whitelist");
			return;
		}

		try {
			if (playNow) {
  			Matcher m = hashPattern.matcher(url);
  			if (m.find()) {
  				String hash = m.group(1);
  				System.out.println("HASH? " + hash);
  				GlobalManager gm = core.getGlobalManager();
  				final DownloadManager dm = gm.getDownloadManager(new HashWrapper(Base32.decode(hash)));
  				if (dm != null) {
  					new AEThread("playExisting", true) {
						
							public void runSupport() {
		  					Debug.outNoStack("loadTorrent already exists.. playing", false);
		  					TorrentListViewsUtils.playOrStream(dm);
							}
						
						}.start();
  					return;
  				}
  			}
			}
			
			
			// If it's going to our URLs, add some extra authenication
			if (url.indexOf("azid=") < 0) {
				url += (url.indexOf('?') < 0 ? "?" : "&") + Constants.URL_SUFFIX;
			}
			UIFunctionsSWT uiFunctions = (UIFunctionsSWT) UIFunctionsManager.getUIFunctions();
			if (uiFunctions != null) {
				if (!COConfigurationManager.getBooleanParameter("add_torrents_silently")) {
					uiFunctions.bringToFront();
				}

				Shell shell = uiFunctions.getMainShell();
				if (shell != null) {
					new FileDownloadWindow(core, shell, url, referer,
							new TorrentDownloaderCallBackInterface() {

								public void TorrentDownloaderEvent(int state,
										TorrentDownloader inf) {
									if (state == TorrentDownloader.STATE_FINISHED) {

										File file = inf.getFile();
										file.deleteOnExit();

										// Do a quick check to see if it's a torrent
										if (!TorrentUtil.isFileTorrent(file, Utils.findAnyShell(),
												file.getName())) {
											return;
										}

										TOTorrent torrent;
										try {
											torrent = TorrentUtils.readFromFile(file, false);
										} catch (TOTorrentException e) {
											Debug.out(e);
											return;
										}
										// Security: Only allow torrents from whitelisted trackers
										if (!PlatformTorrentUtils.isPlatformTracker(torrent)) {
											Debug.out("stopped loading torrent because it's not in whitelist");
											return;
										}

										HashWrapper hw;
										try {
											hw = torrent.getHashWrapper();
										} catch (TOTorrentException e1) {
											Debug.out(e1);
											return;
										}

										GlobalManager gm = core.getGlobalManager();

										if (playNow) {
											DownloadManager existingDM = gm.getDownloadManager(hw);
											if (existingDM != null) {
												TorrentListViewsUtils.playOrStream(existingDM);
												return;
											}
										}

										final HashWrapper fhw = hw;

										GlobalManagerListener l = new GlobalManagerAdapter() {
											public void downloadManagerAdded(DownloadManager dm) {

												try {
													core.getGlobalManager().removeListener(this);

													handleDMAdded(dm, playNow, fhw);
												} catch (Exception e) {
													Debug.out(e);
												}
											}

										};
										gm.addListener(l, false);

										if (playNow) {
											PlayNowList.add(hw);
										}

										TorrentOpener.openTorrent(file.getAbsolutePath());
									}
								}
							});
				}
			}
		} catch (Exception e) {
			Debug.out(e);
		}
	}

	private static void handleDMAdded(final DownloadManager dm,
			final boolean playNow, final HashWrapper fhw) {
		new AEThread("playDM", true) {
			public void runSupport() {
				try {
					HashWrapper hw = dm.getTorrent().getHashWrapper();
					if (!hw.equals(fhw)) {
						return;
					}

					boolean showHomeHint = true;
					if (playNow) {
						showHomeHint = !TorrentListViewsUtils.playOrStream(dm);
					}
					if (showHomeHint) {
						TorrentListViewsUtils.showHomeHint(dm);
					}
				} catch (Exception e) {
					Debug.out(e);
				}
			}
		}.start();
	}
}
