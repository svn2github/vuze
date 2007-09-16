package com.aelitis.azureus.ui.swt.browser.listener;

import java.io.File;
import java.util.Map;

import org.eclipse.swt.widgets.Shell;

import org.bouncycastle.util.encoders.Base64;
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
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.messenger.config.PlatformConfigMessenger;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.browser.msg.AbstractMessageListener;
import com.aelitis.azureus.ui.swt.browser.msg.BrowserMessage;
import com.aelitis.azureus.ui.swt.utils.PlayNowList;
import com.aelitis.azureus.ui.swt.views.skin.TorrentListViewsUtils;
import com.aelitis.azureus.util.Constants;
import com.aelitis.azureus.util.MapUtils;

public class TorrentListener
	extends AbstractMessageListener
{
	public static final String DEFAULT_LISTENER_ID = "torrent";

	public static final String OP_LOAD_TORRENT_OLD = "loadTorrent";

	public static final String OP_LOAD_TORRENT = "load-torrent";

	private AzureusCore core;

	public TorrentListener(AzureusCore core) {
		this(DEFAULT_LISTENER_ID, core);
	}

	public TorrentListener(String id, AzureusCore core) {
		super(id);
		this.core = core;
	}

	/**
	 * 
	 */
	public TorrentListener() {
		this(AzureusCoreFactory.getSingleton());
	}

	public void setShell(Shell shell) {
	}

	public void handleMessage(BrowserMessage message) {
		if (OP_LOAD_TORRENT.equals(message.getOperationId())
				|| OP_LOAD_TORRENT_OLD.equals(message.getOperationId())) {
			Map decodedMap = message.getDecodedMap();
			String url = MapUtils.getMapString(decodedMap, "url", null);
			boolean playNow = MapUtils.getMapBoolean(decodedMap, "play-now", false);
			if (url != null) {
				loadTorrent(core, url, message.getReferer(), playNow);
			} else {
				loadTorrentByB64(core, message, MapUtils.getMapString(decodedMap,
						"b64", null));
			}
		} else {
			throw new IllegalArgumentException("Unknown operation: "
					+ message.getOperationId());
		}
	}

	public static boolean loadTorrentByB64(AzureusCore core, String b64) {
		return loadTorrentByB64(core, null, b64);
	}

	/**
	 * @param mapString
	 *
	 * @since 3.0.1.7
	 */
	private static boolean loadTorrentByB64(AzureusCore core,
			BrowserMessage message, String b64) {
		if (b64 == null) {
			return false;
		}

		byte[] decodedTorrent = Base64.decode(b64);

		File tempTorrentFile;
		try {
			tempTorrentFile = File.createTempFile("AZU", ".torrent");
			tempTorrentFile.deleteOnExit();
			String filename = tempTorrentFile.getAbsolutePath();
			FileUtil.writeBytesAsFile(filename, decodedTorrent);

			TOTorrent torrent = TorrentUtils.readFromFile(tempTorrentFile, false);
			// Security: Only allow torrents from whitelisted trackers
			if (!PlatformTorrentUtils.isPlatformTracker(torrent)) {
				Debug.out("stopped loading torrent because it's not in whitelist");
				return false;
			}

			String savePath = COConfigurationManager.getStringParameter("Default save path");
			if (savePath == null || savePath.length() == 0) {
				savePath = ".";
			}

			core.getGlobalManager().addDownloadManager(filename, savePath);
		} catch (Throwable t) {
			if (message != null) {
				message.debug("loadUrl error", t);
			} else {
				Debug.out(t);
			}
			return false;
		}
		return true;
	}

	public static void loadTorrent(final AzureusCore core, String url,
			String referer, final boolean playNow) {
		boolean blocked = PlatformConfigMessenger.isURLBlocked(url);
		// Security: Only allow torrents from whitelisted urls
		if (blocked) {
			Debug.out("stopped loading torrent URL because it's not in whitelist");
			return;
		}

		try {
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

										TOTorrent torrent;
										try {
											torrent = TorrentUtils.readFromFile(inf.getFile(), false);
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
										};
										gm.addListener(l, false);

										if (playNow) {
											PlayNowList.add(hw);
										}

										TorrentOpener.openTorrent(inf.getFile().getAbsolutePath());
									}
								}
							});
				}
			}
		} catch (Exception e) {
			Debug.out(e);
		}
	}
}
