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

package com.aelitis.azureus.core.torrent;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.*;

import org.bouncycastle.util.encoders.Base64;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.util.*;
import org.json.JSONObject;
import org.json.JSONString;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.messenger.PlatformMessenger;
import com.aelitis.azureus.core.messenger.config.PlatformTorrentMessenger;

import org.gudy.azureus2.plugins.torrent.Torrent;

import org.gudy.azureus2.pluginsimpl.local.torrent.TorrentImpl;

/**
 * @author TuxPaper
 * @created Sep 27, 2006
 *
 */
public class PlatformTorrentUtils
{
	private static final long RETRY_METADATA = 10 * 60 * 1000;

	public static final boolean DEBUG_CACHING = System.getProperty(
			"az3.debug.caching", "0").equals("1");

	private static final String TOR_AZ_PROP_MAP = "Content";

	private static final String TOR_AZ_PROP_HASH = "Content Hash";

	private static final String TOR_AZ_PROP_TITLE = "Title";

	private static final String TOR_AZ_PROP_DESCRIPTION = "Description";

	private static final String TOR_AZ_PROP_AUTHOR = "Author";

	private static final String TOR_AZ_PROP_PUBLISHER = "Publisher";

	private static final String TOR_AZ_PROP_URL = "URL";

	private static final String TOR_AZ_PROP_THUMBNAIL = "Thumbnail";

	private static final String TOR_AZ_PROP_QUALITY = "Quality";

	private static final String TOR_AZ_PROP_USER_RATING = "UserRating";

	private static final String TOR_AZ_PROP_LASTUPDATED = "Revision Date";

	private static final String TOR_AZ_PROP_CREATIONDATE = "Creation Date";

	private static final String TOR_AZ_PROP_METADATA_REFRESHON = "Refresh On";

	private static final String TOR_AZ_PROP_PROGRESSIVE = "Progressive";

	private static final String TOR_AZ_PROP_SPEED = "Speed Bps";

	private static final String TOR_AZ_PROP_MIN_SPEED = "Min Speed Bps";

	private static final String TOR_AZ_PROP_DRM = "DRM";
	
	private static final String TOR_AZ_PROP_QOS_CLASS = "QOS Class";

	private static final ArrayList metaDataListeners = new ArrayList();

	public static Map getContentMap(TOTorrent torrent) {
		if (torrent == null) {
			return new HashMap();
		}

		Map mapAZProps = torrent.getAdditionalMapProperty(TOTorrent.AZUREUS_PROPERTIES);

		if (mapAZProps == null) {
			mapAZProps = new HashMap();
			torrent.setAdditionalMapProperty(TOTorrent.AZUREUS_PROPERTIES, mapAZProps);
		}

		Object objExistingContentMap = mapAZProps.get(TOR_AZ_PROP_MAP);

		Map mapContent;
		if (objExistingContentMap instanceof Map) {
			mapContent = (Map) objExistingContentMap;
		} else {
			mapContent = new HashMap();
			mapAZProps.put(TOR_AZ_PROP_MAP, mapContent);
		}

		return mapContent;
	}

	static Map getTempContentMap(TOTorrent torrent) {
		if (torrent == null) {
			return new HashMap();
		}

		Map mapAZProps = torrent.getAdditionalMapProperty("attributes");

		if (mapAZProps == null) {
			mapAZProps = new HashMap();
			torrent.setAdditionalMapProperty("attributes", mapAZProps);
		}

		Object objExistingContentMap = mapAZProps.get(TOR_AZ_PROP_MAP);

		Map mapContent;
		if (objExistingContentMap instanceof Map) {
			mapContent = (Map) objExistingContentMap;
		} else {
			mapContent = new HashMap();
			mapAZProps.put(TOR_AZ_PROP_MAP, mapContent);
		}

		return mapContent;
	}

	private static String getContentMapString(TOTorrent torrent, String key) {
		if (torrent == null) {
			return null;
		}

		Map mapContent = getContentMap(torrent);
		Object obj = mapContent.get(key);

		if (obj instanceof String) {
			return (String) obj;
		} else if (obj instanceof byte[]) {
			try {
				return new String((byte[]) obj, Constants.DEFAULT_ENCODING);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return null;
	}

	private static long getContentMapLong(TOTorrent torrent, String key, long def) {
		if (torrent == null) {
			return def;
		}

		Map mapContent = getContentMap(torrent);
		Object obj = mapContent.get(key);

		try {
			if (obj instanceof Long) {
				return ((Long) obj).longValue();
			} else if (obj instanceof Integer) {
				return ((Integer) obj).longValue();
			} else if (obj instanceof String) {
				return Long.parseLong((String) obj);
			} else if (obj instanceof byte[]) {
				return Long.parseLong(new String((byte[]) obj));
			}
		} catch (Exception e) {
		}

		return def;
	}

	private static void setContentMapLong(TOTorrent torrent, String key, long value) {
		if (torrent == null) {
			return;
		}

		Map mapContent = getContentMap(torrent);
		mapContent.put( key, new Long(value));
	}

	public static String getContentHash(TOTorrent torrent) {
		return getContentMapString(torrent, TOR_AZ_PROP_HASH);
	}

	public static String getContentTitle(TOTorrent torrent) {
		return getContentMapString(torrent, TOR_AZ_PROP_TITLE);
	}

	public static String getContentDescription(TOTorrent torrent) {
		return getContentMapString(torrent, TOR_AZ_PROP_DESCRIPTION);
	}

	public static String getContentAuthor(TOTorrent torrent) {
		return getContentMapString(torrent, TOR_AZ_PROP_AUTHOR);
	}

	public static String getContentPublisher(TOTorrent torrent) {
		return getContentMapString(torrent, TOR_AZ_PROP_PUBLISHER);
	}

	public static String getContentURL(TOTorrent torrent) {
		return getContentMapString(torrent, TOR_AZ_PROP_URL);
	}

	public static String getContentQuality(TOTorrent torrent) {
		return getContentMapString(torrent, TOR_AZ_PROP_QUALITY);
	}

	public static boolean isContentDRM(TOTorrent torrent) {
		return getContentMapLong(torrent, TOR_AZ_PROP_DRM, -1) >= 0;
	}

	public static long getQOSClass(TOTorrent torrent) {
		return getContentMapLong(torrent, TOR_AZ_PROP_QOS_CLASS, 0 );
	}

	public static void setQOSClass(TOTorrent torrent, long cla) {
		setContentMapLong(torrent, TOR_AZ_PROP_QOS_CLASS, cla );
	}

	private static void putOrRemove(Map map, String key, Object obj) {
		if (obj == null || obj.equals(null)) {
			map.remove(key);
		} else {
			map.put(key, obj);
		}
	}

	public static void setContentQuality(TOTorrent torrent, String sQualityID) {
		Map mapContent = getContentMap(torrent);
		putOrRemove(mapContent, TOR_AZ_PROP_QUALITY, sQualityID);

		try {
			TorrentUtils.writeToFile(torrent);
		} catch (TOTorrentException e) {
			Debug.out(e);
		}
	}

	public static byte[] getContentThumbnail(TOTorrent torrent) {
		Map mapContent = getContentMap(torrent);
		Object obj = mapContent.get(TOR_AZ_PROP_THUMBNAIL);

		if (obj instanceof byte[]) {
			return (byte[]) obj;
		}

		return null;
	}

	public static void setContentThumbnail(TOTorrent torrent, byte[] thumbnail) {
		Map mapContent = getContentMap(torrent);
		putOrRemove(mapContent, TOR_AZ_PROP_THUMBNAIL, thumbnail);

		try {
			TorrentUtils.writeToFile(torrent);
		} catch (TOTorrentException e) {
			Debug.out(e);
		}
	}

	public static void setUserRating(TOTorrent torrent, int rating) {
		Map mapContent = getTempContentMap(torrent);
		mapContent.put(TOR_AZ_PROP_USER_RATING, new Long(rating));
		try {
			TorrentUtils.writeToFile(torrent);
		} catch (TOTorrentException e) {
			Debug.out(e);
		}
	}

	public static void removeUserRating(TOTorrent torrent) {
		Map mapContent = getTempContentMap(torrent);
		try {
			if (mapContent.remove(TOR_AZ_PROP_USER_RATING) != null) {
				TorrentUtils.writeToFile(torrent);
			}
		} catch (TOTorrentException e) {
			Debug.out(e);
		}
	}

	/**
	 * 
	 * @param torrent
	 * @return -1: No rating
	 */
	public static int getUserRating(TOTorrent torrent) {
		Map mapContent = getTempContentMap(torrent);
		Long l = (Long) mapContent.get(TOR_AZ_PROP_USER_RATING);
		if (l == null) {
			return -1;
		}
		return l.intValue();
	}

	public static long getMetaDataRefreshOn(TOTorrent torrent) {
		Map mapContent = getTempContentMap(torrent);
		Long l = (Long) mapContent.get(TOR_AZ_PROP_METADATA_REFRESHON);
		if (l == null) {
			return 0;
		}
		return l.longValue();
	}

	public static void setMetaDataRefreshOn(TOTorrent torrent, long refreshOn) {
		Map mapContent = getTempContentMap(torrent);
		mapContent.put(TOR_AZ_PROP_METADATA_REFRESHON, new Long(refreshOn));
		try {
			TorrentUtils.writeToFile(torrent);
		} catch (TOTorrentException e) {
			Debug.out(e);
		}
	}

	public static boolean isContent(TOTorrent torrent) {
		if (torrent == null) {
			return false;
		}
		boolean bContent = PlatformTorrentUtils.getContentHash(torrent) != null;
		if (bContent) {
			return true;
		}

		try {
			URL announceURL = torrent.getAnnounceURL();
			if (announceURL == null) {
				return false;
			}
			String url = announceURL.toString().toLowerCase();
			return url.indexOf("tracker.aelitis.com") >= 0
					|| url.indexOf("azureusplatform.com") >= 0;
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean isContent(Torrent torrent) {
		if (torrent instanceof TorrentImpl) {
			return isContent(((TorrentImpl) torrent).getTorrent());
		}
		return false;
	}

	/**
	 * @param torrent
	 * @param maxDelayMS TODO
	 */
	public static void updateMetaData(final TOTorrent torrent, long maxDelayMS) {
		if (!isContent(torrent)) {
			log("torrent " + new String(torrent.getName()) + " not az content");
			return;
		}

		if (DEBUG_CACHING) {
			log("updateMD");
		}

		PlatformTorrentMessenger.getMetaData(new TOTorrent[] {
			torrent
		}, maxDelayMS, new PlatformTorrentMessenger.GetMetaDataReplyListener() {

			public void messageSent() {
			}

			public void replyReceived(String replyType, Map mapHashes) {
				updateMetaData_handleReply(torrent, null, replyType, mapHashes);
			}
		});
	}

	private static void updateMetaData_handleReply(final TOTorrent torrent,
			String hash, String replyType, Map mapHashes) {
		Map contentMap = PlatformTorrentUtils.getContentMap(torrent);

		if (replyType.equals(PlatformMessenger.REPLY_EXCEPTION)) {
			// try again in a bit
			if (DEBUG_CACHING) {
				log("Exception, retrying later");
			}
			SimpleTimer.addEvent("Update MD Retry", SystemTime.getCurrentTime()
					+ RETRY_METADATA, new TimerEventPerformer() {
				public void perform(TimerEvent event) {
					if (DEBUG_CACHING) {
						log("retry time");
					}
					PlatformTorrentUtils.updateMetaData(torrent, 15000);
				}
			});
		} else {
			if (hash == null) {
				try {
					hash = torrent.getHashWrapper().toBase32String();
				} catch (TOTorrentException e) {
				}
			}
			JSONObject jsonMapMetaData = hash == null ? null
					: (JSONObject) mapHashes.get(hash);
			if (jsonMapMetaData != null) {
				long oldLastUpdated = getContentLastUpdated(torrent);
				long expireyMins = 0;

				for (Iterator iterator = jsonMapMetaData.keys(); iterator.hasNext();) {
					String key = (String) iterator.next();
					Object value = jsonMapMetaData.get(key);

					if (value == null || value.equals(null)) {
						contentMap.remove(key);
					} else if ((key.equals("Thumbnail") || key.endsWith(".B64"))
							&& value instanceof String) {
						contentMap.put(key, Base64.decode((String) value));
					} else if (key.equals("expires-in-mins") && value instanceof Long) {
						expireyMins = ((Long) value).longValue();
					} else if (!(value instanceof JSONString)) {
						final String s = jsonMapMetaData.getString(key);
						contentMap.put(key, s);
					} else {
						System.out.println("BOO! " + key + ";" + value);
					}
					try {
						TorrentUtils.writeToFile(torrent);
					} catch (TOTorrentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					// crappy way of updating the display name
					try {
						GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
						DownloadManager dm = gm.getDownloadManager(torrent);
						String title = PlatformTorrentUtils.getContentTitle(torrent);
						if (title != null && title.length() > 0
								&& dm.getDownloadState().getDisplayName() == null) {
							dm.getDownloadState().setDisplayName(title);
						}
					} catch (Exception e) {

					}
					triggerMetaDataUpdateListeners(torrent);
				}

				long refreshOn;
				if (expireyMins > 0) {
					refreshOn = SystemTime.getCurrentTime() + (expireyMins * 60 * 1000L);
				} else {
					long newLastUpdated = getContentLastUpdated(torrent);

					long diff = newLastUpdated - oldLastUpdated;
					if (diff > 0 && oldLastUpdated != 0) {
						refreshOn = SystemTime.getCurrentTime() + (diff * 2);
					} else {
						refreshOn = SystemTime.getCurrentTime()
								+ (7 * 24 * 60 * 60 * 1000L);
					}
				}

				if (DEBUG_CACHING) {
					log("got MD. Next refresh in "
							+ (refreshOn - SystemTime.getCurrentTime()));
				}
				setMetaDataRefreshOn(torrent, refreshOn);
				SimpleTimer.addEvent("Update MD", refreshOn, new TimerEventPerformer() {
					public void perform(TimerEvent event) {
						PlatformTorrentUtils.updateMetaData(torrent, 15000);
					}
				});
			} else {
				long refreshOn = SystemTime.getCurrentTime()
						+ (30 * 24 * 60 * 60 * 1000L);
				setMetaDataRefreshOn(torrent, refreshOn);
				if (DEBUG_CACHING) {
					log("no hash in reply for " + torrent + ". Next refresh on "
							+ new Date(refreshOn));
				}
			}
		}
	}

	public static void addListener(MetaDataUpdateListener l) {
		if (metaDataListeners.indexOf(l) < 0) {
			metaDataListeners.add(l);
		}
	}

	public static void removeListener(MetaDataUpdateListener l) {
		metaDataListeners.remove(l);
	}

	public static void triggerMetaDataUpdateListeners(TOTorrent torrent) {
		MetaDataUpdateListener[] listeners = (MetaDataUpdateListener[]) metaDataListeners.toArray(new MetaDataUpdateListener[0]);
		for (int i = 0; i < listeners.length; i++) {
			MetaDataUpdateListener listener = listeners[i];
			try {
				listener.metaDataUpdated(torrent);
			} catch (Exception e) {
				Debug.out(e);
			}
		}
	}

	public static long getContentLastUpdated(TOTorrent torrent) {
		return getContentMapLong(torrent, TOR_AZ_PROP_LASTUPDATED, 0);
	}

	public static boolean isContentProgressive(TOTorrent torrent) {
		return getContentMapLong(torrent, TOR_AZ_PROP_PROGRESSIVE, 0) == 1;
	}

	public static long getContentStreamSpeedBps(TOTorrent torrent) {
		return getContentMapLong(torrent, TOR_AZ_PROP_SPEED, 0);
	}

	public static long getContentMinimumSpeedBps(TOTorrent torrent) {
		return getContentMapLong(torrent, TOR_AZ_PROP_MIN_SPEED, 20 * 1024);
	}

	public static void log(String str) {
		if (DEBUG_CACHING) {
			AEDiagnosticsLogger diag_logger = AEDiagnostics.getLogger("v3.MD");
			diag_logger.log(str);
			System.out.println(str);
		}
	}
}
