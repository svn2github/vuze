package com.aelitis.azureus.ui.swt.browser.listener;

import java.io.File;
import java.util.Map;

import org.eclipse.swt.widgets.Shell;

import org.bouncycastle.util.encoders.Base64;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.cnetwork.ContentNetworkManagerFactory;
import com.aelitis.azureus.core.messenger.ClientMessageContext;
import com.aelitis.azureus.core.messenger.ClientMessageContext.torrentURLHandler;
import com.aelitis.azureus.core.messenger.browser.BrowserMessage;
import com.aelitis.azureus.core.messenger.browser.listeners.AbstractBrowserMessageListener;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.selectedcontent.DownloadUrlInfo;
import com.aelitis.azureus.ui.selectedcontent.DownloadUrlInfoContentNetwork;
import com.aelitis.azureus.ui.swt.utils.TorrentUIUtilsV3;
import com.aelitis.azureus.util.MapUtils;

public class TorrentListener
	extends AbstractBrowserMessageListener
{
	public static final String DEFAULT_LISTENER_ID = "torrent";

	public static final String OP_LOAD_TORRENT_OLD = "loadTorrent";

	public static final String OP_LOAD_TORRENT = "load-torrent";

	private ClientMessageContext.torrentURLHandler		torrentURLHandler;
	
	public TorrentListener(String id) {
		super(id);
	}

	public TorrentListener() {
		this(DEFAULT_LISTENER_ID);
	}

	public void 
	setTorrentURLHandler(
		torrentURLHandler handler) 
	{
		torrentURLHandler = handler;
	}
	
	public void setShell(Shell shell) {
	}

	public void handleMessage(final BrowserMessage message) {
		String opid = message.getOperationId();
		if (OP_LOAD_TORRENT.equals(opid) || OP_LOAD_TORRENT_OLD.equals(opid)) {
			final Map decodedMap = message.getDecodedMap();
			String url = MapUtils.getMapString(decodedMap, "url", null);
			final boolean playNow = MapUtils.getMapBoolean(decodedMap, "play-now", false);
			final boolean playPrepare = MapUtils.getMapBoolean(decodedMap, "play-prepare",
					false);
			final boolean bringToFront = MapUtils.getMapBoolean(decodedMap,
					"bring-to-front", true);
			if (url != null) {
				if ( torrentURLHandler != null ){
					
					try{
						torrentURLHandler.handleTorrentURL( url );
						
					}catch( Throwable e ){
						
						Debug.printStackTrace(e);
					}
				}
				final DownloadUrlInfo dlInfo = new DownloadUrlInfoContentNetwork(url,
						ContentNetworkManagerFactory.getSingleton().getContentNetwork(
								context.getContentNetworkID()));
				dlInfo.setReferer(message.getReferer());

				AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
					public void azureusCoreRunning(AzureusCore core) {
						TorrentUIUtilsV3.loadTorrent(dlInfo, playNow, playPrepare,
								bringToFront);
					}
				});
			} else {
				AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
					public void azureusCoreRunning(AzureusCore core) {
						loadTorrentByB64(core, message, MapUtils.getMapString(decodedMap,
								"b64", null));
					}
				});
			}
		} else {
			throw new IllegalArgumentException("Unknown operation: " + opid);
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
