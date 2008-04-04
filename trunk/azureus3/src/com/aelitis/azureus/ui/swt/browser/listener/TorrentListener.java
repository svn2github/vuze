package com.aelitis.azureus.ui.swt.browser.listener;

import java.io.File;
import java.util.Map;

import org.eclipse.swt.widgets.Shell;

import org.bouncycastle.util.encoders.Base64;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManagerFactory;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.messenger.config.PlatformRatingMessenger;
import com.aelitis.azureus.core.torrent.GlobalRatingUtils;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.swt.browser.msg.AbstractMessageListener;
import com.aelitis.azureus.ui.swt.browser.msg.BrowserMessage;
import com.aelitis.azureus.ui.swt.utils.TorrentUIUtilsV3;
import com.aelitis.azureus.util.MapUtils;

public class TorrentListener
	extends AbstractMessageListener
{
	public static final String DEFAULT_LISTENER_ID = "torrent";

	public static final String OP_LOAD_TORRENT_OLD = "loadTorrent";

	public static final String OP_LOAD_TORRENT = "load-torrent";

	public static final String OP_UPDATE_RATING = "update-rating";

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
			boolean playPrepare = MapUtils.getMapBoolean(decodedMap, "play-prepare", false);
			boolean bringToFront = MapUtils.getMapBoolean(decodedMap, "bring-to-front", true);
			if (url != null) {
				TorrentUIUtilsV3.loadTorrent(core, url, message.getReferer(), playNow, playPrepare, bringToFront);
			} else {
				loadTorrentByB64(core, message, MapUtils.getMapString(decodedMap,
						"b64", null));
			}
		} else if (OP_UPDATE_RATING.equals(message.getOperationId())) {
			Map decodedMap = message.getDecodedMap();
			String hash = MapUtils.getMapString(decodedMap, "torrent-hash", null);
			if (hash != null) {
				DownloadManager dm = core.getGlobalManager().getDownloadManager(new HashWrapper(Base32.decode(hash)));
				if (dm != null && dm.getTorrent() != null) {
  				PlatformRatingMessenger.getUserRating(new String[] {
  					PlatformRatingMessenger.RATE_TYPE_CONTENT
  				}, new String[] {
  					hash
  				}, 7000);
  				
  				PlatformRatingMessenger.updateGlobalRating(dm.getTorrent(), 7000);
				}
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
}
