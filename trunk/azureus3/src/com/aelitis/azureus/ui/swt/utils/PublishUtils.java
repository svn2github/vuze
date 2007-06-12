package com.aelitis.azureus.ui.swt.utils;

import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.messenger.ClientMessageContext;
import com.aelitis.azureus.ui.swt.browser.listener.publish.DownloadStateAndRemoveListener;
import com.aelitis.azureus.ui.swt.browser.listener.publish.PublishTransaction;
import com.aelitis.azureus.ui.swt.browser.listener.publish.SeedingListener;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.torrent.Torrent;

import org.gudy.azureus2.pluginsimpl.local.download.DownloadImpl;
import org.gudy.azureus2.pluginsimpl.local.torrent.TorrentImpl;

/**
 * Publish functions that are used by both the Publisher plugin and AZ3ui's publish window
 * 
 * @author TuxPaper
 *
 */
public class PublishUtils
{
	private static final String CONTENTMAP_KEY = "Plugin.azdirector.ContentMap";

	private static final String REMOVAL_ATTRIBUTE_KEY = "REMOVAL ALLOWED";

	public static void setupContext(ClientMessageContext context,
			PluginInterface pi, DownloadStateAndRemoveListener downloadListener) {

		context.registerTransactionType("publish", PublishTransaction.class);
		context.addMessageListener(new SeedingListener(pi, downloadListener));
	}

	public static boolean isPublished(DownloadManager dm) {
		try {
			Map mapAttr = dm.getDownloadState().getMapAttribute(CONTENTMAP_KEY);

			return mapAttr != null
					&& mapAttr.containsKey(PublishTransaction.PUBLISH_ATTRIBUTE_KEY);
		} catch (Exception e) {
			Debug.out("baH", e);
		}
		return false;
	}

	/**
	 * @param download
	 * @return
	 *
	 * @since 3.0.1.5
	 */
	public static boolean isPublished(Download download) {
		if (download instanceof DownloadImpl) {
			return isPublished(((DownloadImpl) download).getDownload());
		}
		return false;
	}

	public static void setPublished(Torrent torrent) {
		if (torrent instanceof TorrentImpl) {
			setPublished(((TorrentImpl) torrent).getTorrent());
		}
	}

	/**
	 * @param torrent
	 *
	 * @since 3.0.1.5
	 */
	public static void setPublished(TOTorrent torrent) {
		try {
			if (torrent == null) {
				return;
			}

			Map map = torrent.getAdditionalMapProperty("attributes");

			if (map != null) {
				Map mapAttr = (Map) map.get(CONTENTMAP_KEY);

				mapAttr.put(PublishTransaction.PUBLISH_ATTRIBUTE_KEY, new Long(1));
			}
		} catch (Exception e) {
			Debug.out("baH", e);
		}
	}

	public static void setPublished(DownloadManager dm) {
		try {
			Map mapAttr = dm.getDownloadState().getMapAttribute(CONTENTMAP_KEY);

			if (mapAttr == null) {
				mapAttr = new HashMap();
			}
			mapAttr.put(PublishTransaction.PUBLISH_ATTRIBUTE_KEY, new Long(1));
			dm.getDownloadState().setMapAttribute(CONTENTMAP_KEY, mapAttr);
		} catch (Exception e) {
			Debug.out("baH", e);
		}
	}

	public static void setPublished(Download download) {
		if (download instanceof DownloadImpl) {
			setPublished(((DownloadImpl) download).getDownload());
		}
	}

	public static boolean isRemovalAllowed(Download d) {
		try {
			DownloadManager dm = ((DownloadImpl) d).getDownload();
			Map mapAttr = dm.getDownloadState().getMapAttribute(CONTENTMAP_KEY);

			if (mapAttr != null
					&& mapAttr.containsKey(REMOVAL_ATTRIBUTE_KEY)) {
				return true;
			}

			// Somehow the torrent is in stoppped state and the removal attribute wasn't set
			// Allow removal
			if (d.getState() == Download.ST_STOPPED) {
				return true;
			}
		} catch (Exception e) {
			Debug.out("baH", e);
		}
		return false;
	}

	public static void setRemovalAllowed(Download d) {
		try {
			DownloadManager dm = ((DownloadImpl) d).getDownload();
			Map mapAttr = dm.getDownloadState().getMapAttribute(CONTENTMAP_KEY);

			if (mapAttr == null) {
				mapAttr = new HashMap();
			}
			mapAttr.put(REMOVAL_ATTRIBUTE_KEY, new Long(1));
			dm.getDownloadState().setMapAttribute(CONTENTMAP_KEY, mapAttr);
		} catch (Exception e) {
			Debug.out("baH", e);
		}
	}
}
