package com.aelitis.azureus.ui.swt.utils;

import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.messenger.ClientMessageContext;
import com.aelitis.azureus.ui.swt.browser.listener.publish.PublishTransaction;
import com.aelitis.azureus.ui.swt.browser.listener.publish.SeedingListener;

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

	private static final String COMPLETE_ATTRIBUTE_KEY 	= "COMPLETE";

	public static void setupContext(ClientMessageContext context) {
		context.registerTransactionType("publish", PublishTransaction.class);
		context.addMessageListener(new SeedingListener());
	}

	public static boolean isPublished(DownloadManager dm) {
		if (dm == null) {
			return false;
		}
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

	public static void setPublished(DownloadManager dm, boolean isPublishedContent) {
		if (isPublishedContent) {
			setPublished(dm);
			return;
		}
		
		try {
			Map mapAttr = dm.getDownloadState().getMapAttribute(CONTENTMAP_KEY);

			if (mapAttr == null) {
				return;
			}
			mapAttr = new HashMap(mapAttr);
			Object remove = mapAttr.remove(PublishTransaction.PUBLISH_ATTRIBUTE_KEY);
			if (remove != null) {
				dm.getDownloadState().setMapAttribute(CONTENTMAP_KEY, mapAttr);
			}
		} catch (Exception e) {
			Debug.out(e);
		}
	}

	public static void setPublished(DownloadManager dm) {
		try {
			Map mapAttr = dm.getDownloadState().getMapAttribute(CONTENTMAP_KEY);

			if (mapAttr == null) {
				mapAttr = new HashMap();
			} else {
				mapAttr = new HashMap(mapAttr);
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

	public static void setPublishComplete(DownloadManager dm) {
		try {
			Map mapAttr = dm.getDownloadState().getMapAttribute(CONTENTMAP_KEY);

			if ( mapAttr == null ){
				
				mapAttr = new HashMap();
			}else{
				mapAttr = new HashMap(mapAttr);
			}
			
			mapAttr.put( COMPLETE_ATTRIBUTE_KEY, new Long(1));
			
			dm.getDownloadState().setMapAttribute(CONTENTMAP_KEY, mapAttr);
			
		} catch (Exception e) {
			
			Debug.out("baH", e);
		}
	}

	public static boolean isPublishComplete( DownloadManager dm ){
		
		Map mapAttr = dm.getDownloadState().getMapAttribute(CONTENTMAP_KEY);

		if ( mapAttr == null ){
			
			return( false );
		}
		
		Long complete = (Long)mapAttr.get( COMPLETE_ATTRIBUTE_KEY );

		return( complete != null );
	}
}
