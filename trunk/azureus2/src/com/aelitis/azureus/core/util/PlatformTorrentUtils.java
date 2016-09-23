/**
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
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
 */

package com.aelitis.azureus.core.util;

import java.io.UnsupportedEncodingException;
import java.util.*;

import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;


public class PlatformTorrentUtils
{
		// duplicate of some azureus3 project PlatformTorrentUtils features needed in azureus2 ;(
	
	private static final String TOR_AZ_PROP_MAP = "Content";
	
	private static final String TOR_AZ_PROP_CVERSION = "_Version_";

	private static final String TOR_AZ_PROP_TITLE = "Title";

	private static final String TOR_AZ_PROP_DESCRIPTION = "Description";

	private static final String TOR_AZ_PROP_PRIMARY_FILE = "Primary File Index";

	private static final String TOR_AZ_PROP_THUMBNAIL = "Thumbnail";

	private static final String TOR_AZ_PROP_THUMBNAIL_TYPE = "Thumbnail.type";

	private static final String TOR_AZ_PROP_THUMBNAIL_URL = "Thumbnail.url";


	public static Map getContentMap(TOTorrent torrent) {
		if (torrent == null) {
			return Collections.EMPTY_MAP;
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

	public static String getContentMapString(TOTorrent torrent, String key) {
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

	private static void setContentMapString(TOTorrent torrent, String key,
			String value) {
		if (torrent == null) {
			return;
		}

		Map mapContent = getContentMap(torrent);
		mapContent.put(key, value);
		incVersion(mapContent);
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

	public static Map getContentMapMap(TOTorrent torrent, String key ){
		if ( torrent == null ){
			return( null );
		}
		
		Map mapContent = getContentMap(torrent);
		Object obj = mapContent.get(key);
		
		if ( obj instanceof Map ){
			return((Map)obj);
		}
		
		return( null );
	}
	
	private static void setContentMapLong(TOTorrent torrent, String key,
			long value) {
		if (torrent == null) {
			return;
		}

		Map mapContent = getContentMap(torrent);
		mapContent.put(key, new Long(value));
		incVersion(mapContent);
	}

	public static void setContentMapMap(TOTorrent torrent, String key,
			Map value) {
		if (torrent == null) {
			return;
		}

		Map mapContent = getContentMap(torrent);
		mapContent.put(key, value);
		incVersion(mapContent);
	}
			
	private static void putOrRemove(Map map, String key, Object obj) {
		if (obj == null) {
			map.remove(key);
		} else {
			map.put(key, obj);
		}
	}

	private static void writeTorrentIfExists(TOTorrent torrent) {
		if (!AzureusCoreFactory.isCoreRunning()) {
			return;
		}
		AzureusCore core = AzureusCoreFactory.getSingleton();
		if (core == null || !core.isStarted()) {
			return;
		}

		GlobalManager gm = core.getGlobalManager();
		if (gm == null || gm.getDownloadManager(torrent) == null) {
			return;
		}

		try {
			TorrentUtils.writeToFile(torrent);
		} catch (TOTorrentException e) {
			Debug.out(e);
		}
	}

	private static void
	incVersion(
		Map mapContent )
	{
		Long v = (Long)mapContent.get( TOR_AZ_PROP_CVERSION );
		mapContent.put( TOR_AZ_PROP_CVERSION, v==null?0:v+1 );
	}
	
	public static int getContentVersion(TOTorrent torrent) {
		Map mapContent = getContentMap(torrent);
		Long v = (Long)mapContent.get( TOR_AZ_PROP_CVERSION );
		return(v==null?0:v.intValue());
	}
	
	public static String getContentTitle(TOTorrent torrent) {
		return getContentMapString(torrent, TOR_AZ_PROP_TITLE);
	}
	public static void setContentTitle(TOTorrent torrent, String title) {
		setContentMapString(torrent, TOR_AZ_PROP_TITLE, title);
	}
	
	public static byte[] getContentThumbnail(TOTorrent torrent) {
		Map mapContent = getContentMap(torrent);
		Object obj = mapContent.get(TOR_AZ_PROP_THUMBNAIL);

		if (obj instanceof byte[]) {
			return (byte[]) obj;
		}

		return null;
	}

	public static String getContentDescription(TOTorrent torrent) {
		return getContentMapString(torrent, TOR_AZ_PROP_DESCRIPTION);
	}
	
	public static void setContentDescription(TOTorrent torrent, String desc) {
		setContentMapString(torrent, TOR_AZ_PROP_DESCRIPTION,desc);
		writeTorrentIfExists(torrent);
	}
	
	public static String getContentThumbnailUrl(TOTorrent torrent) {
		return getContentMapString(torrent, TOR_AZ_PROP_THUMBNAIL_URL);
	}

	public static void setContentThumbnailUrl(TOTorrent torrent, String url) {
		setContentMapString(torrent, TOR_AZ_PROP_THUMBNAIL_URL, url);
	}
	
		// thumb
	
	public static void setContentThumbnail(TOTorrent torrent, byte[] thumbnail) {
		Map mapContent = getContentMap(torrent);
		putOrRemove(mapContent, TOR_AZ_PROP_THUMBNAIL, thumbnail);
		incVersion(mapContent);
		writeTorrentIfExists(torrent);
	}

	public static void setContentThumbnail(TOTorrent torrent, byte[] thumbnail, String type ){
		Map mapContent = getContentMap(torrent);
		putOrRemove(mapContent, TOR_AZ_PROP_THUMBNAIL, thumbnail);
		incVersion(mapContent);
		setContentMapString(torrent, TOR_AZ_PROP_THUMBNAIL_TYPE, type);
		writeTorrentIfExists(torrent);
	}
	
	public static String getContentThumbnailType(TOTorrent torrent) {
		return getContentMapString(torrent, TOR_AZ_PROP_THUMBNAIL_TYPE);
	}
	
	public static int getContentPrimaryFileIndex(TOTorrent torrent ){
		return (int)getContentMapLong(torrent, TOR_AZ_PROP_PRIMARY_FILE, -1 );
	}
	
	public static void setContentPrimaryFileIndex(TOTorrent torrent, int index ) {
		setContentMapLong(torrent, TOR_AZ_PROP_PRIMARY_FILE, index );
	}
	
}
