package com.aelitis.azureus.ui.swt.browser.listener;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.eclipse.swt.widgets.Shell;

import org.bouncycastle.util.encoders.Base64;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.TorrentUtils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.swt.browser.msg.AbstractMessageListener;
import com.aelitis.azureus.ui.swt.browser.msg.BrowserMessage;
import com.aelitis.azureus.util.MapUtils;

public class TorrentListener
	extends AbstractMessageListener
{
	public static final String DEFAULT_LISTENER_ID = "torrent";

	public static final String OP_LOAD_TORRENT_OLD = "loadTorrent";

	public static final String OP_LOAD_TORRENT = "load-torrent";

	private AzureusCore core;

	private Shell shell;

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
		this.shell = shell;
	}

	public void handleMessage(BrowserMessage message) {
		if (OP_LOAD_TORRENT.equals(message.getOperationId())
				|| OP_LOAD_TORRENT_OLD.equals(message.getOperationId())) {
			Map decodedMap = message.getDecodedMap();
			String url = MapUtils.getMapString(decodedMap, "url", null);
			if (url != null) {
				loadTorrent(url, message.getReferer());
			} else {
				loadTorrentByB64(core, message,
						MapUtils.getMapString(decodedMap, "b64", null));
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
			if (!PlatformTorrentUtils.isPlatformTracker(torrent)) {
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

	private void loadTorrent(String url, String referer) {
		URL urlReferer = null;
		try {
			urlReferer = new URL(referer);
		} catch (MalformedURLException e) {
			Debug.out(e);
		}
		try {
			core.getPluginManager().getDefaultPluginInterface().getDownloadManager().addDownload(
					new URL(url), urlReferer);
		} catch (Exception e) {
			Debug.out(e);
		}
	}
}
