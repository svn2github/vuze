package com.aelitis.azureus.ui.swt.utils;

import java.util.Map;

import org.eclipse.swt.browser.Browser;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.messenger.ClientMessageContext;
import com.aelitis.azureus.ui.swt.browser.listener.ConfigListener;
import com.aelitis.azureus.ui.swt.browser.listener.DisplayListener;
import com.aelitis.azureus.ui.swt.browser.listener.TorrentListener;
import com.aelitis.azureus.ui.swt.browser.listener.publish.*;

import org.gudy.azureus2.plugins.PluginInterface;

/**
 * Publish functions that are used by both the Publisher plugin and AZ3ui's publish window
 * 
 * @author TuxPaper
 *
 */
public class PublishUtils
{

	public static void setupContext(ClientMessageContext context,
			Browser browser, PluginInterface pi, LocalHoster hoster,
			DownloadStateAndRemoveListener downloadListener) {

		context.addMessageListener(new PublishListener(browser.getShell(), pi,
				hoster));
		context.registerTransactionType("publish", PublishTransaction.class);
		context.addMessageListener(new DisplayListener(browser));
		context.addMessageListener(new SeedingListener(pi, downloadListener));
		context.addMessageListener(new TorrentListener());
		context.addMessageListener(new ConfigListener(browser));
	}

	public static boolean isPublished(DownloadManager dm) {
		try {
			TOTorrent torrent = dm.getTorrent();
			if (torrent == null) {
				return false;
			}

			Map map = torrent.getAdditionalMapProperty("attributes");

			if (map != null) {
				Map mapAttr = (Map) map.get("Plugin.azdirector.ContentMap");

				return mapAttr != null
						&& mapAttr.containsKey(PublishTransaction.PUBLISH_ATTRIBUTE_KEY);
			}
		} catch (Exception e) {
			Debug.out("baH", e);
		}
		return false;
	}
}
