/**
 * Created on Jan 28, 2008 
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */

package com.aelitis.azureus.util;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.TableColumnSortObject;

import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderAdapter;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderListener;

import org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader.ResourceDownloaderFactoryImpl;

/**
 * 
 * Comparable implementation sorts on timestamp.<P>
 * equals() implementation compared IDs
 * 
 * @author TuxPaper
 * @created Jan 28, 2008
 *
 */
public class VuzeActivitiesEntry
	implements TableColumnSortObject
{
	public static int TYPE_HEADER = 0;
	
	public static int TYPE_DATA = 1;

	public static int SORT_DATE = 0;

	public static int sortBy = SORT_DATE;

	public String text;

	public String icon;

	public String id;

	public int type;

	private long timestamp;

	public String typeID;

	public String assetHash;

	public String assetImageURL;

	public DownloadManager dm;

	public Object urlHitArea;

	public String url;

	public TableColumnCore tableColumn;

	public byte[] imageBytes;

	public VuzeActivitiesEntry(long timestamp, int type, String text,
			String icon, String id) {
		this.type = 1;
		this.text = text;
		this.icon = icon;
		this.id = id;
		this.type = type;
		this.timestamp = timestamp;
	}

	public VuzeActivitiesEntry(long timestamp, String text, String icon,
			String id, String typeID, String assetHash) {
		this.type = 1;
		this.timestamp = timestamp;
		this.text = text;
		this.icon = icon;
		this.id = id;
		this.typeID = typeID;
		this.assetHash = assetHash;
	}

	/**
	 * 
	 */
	public VuzeActivitiesEntry() {
		// TODO Auto-generated constructor stub
	}

	// @see java.lang.Object#equals(java.lang.Object)
	public boolean equals(Object obj) {
		if ((obj instanceof VuzeActivitiesEntry) && id != null) {
			return id.equals(((VuzeActivitiesEntry) obj).id);
		}
		return super.equals(obj);
	}

	// @see java.lang.Comparable#compareTo(java.lang.Object)
	public int compareTo(Object obj) {
		if (obj instanceof VuzeActivitiesEntry) {
			//System.out.println("EQ" + timestamp + ";" + text.substring(0, 8) + (int) (timestamp - ((VuzeNewsEntry) obj).timestamp));
			long x = (timestamp - ((VuzeActivitiesEntry) obj).timestamp);
			return x == 0 ? 0 : x > 0 ? 1 : -1;
		}
		// we are bigger
		return 1;
	}

	public static VuzeActivitiesEntry readFromMap(Map map) {
		VuzeActivitiesEntry entry = new VuzeActivitiesEntry();
		if (map == null || map.size() == 0) {
			return entry;
		}

		entry.timestamp = MapUtils.getMapLong(map, "timestamp", 0);
		entry.assetHash = MapUtils.getMapString(map, "assetHash", null);
		entry.icon = MapUtils.getMapString(map, "icon", null);
		entry.id = MapUtils.getMapString(map, "id", null);
		entry.text = MapUtils.getMapString(map, "text", null);
		entry.typeID = MapUtils.getMapString(map, "typeID", null);
		entry.type = MapUtils.getMapInt(map, "type", 1);
		entry.setAssetImageURL(MapUtils.getMapString(map, "assetImageURL", null));

		if (entry.assetHash != null) {
			GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
			entry.dm = gm.getDownloadManager(new HashWrapper(
					Base32.decode(entry.assetHash)));
		}

		return entry;
	}

	public void setAssetImageURL(final String url) {
		if (url == null && assetImageURL == null) {
			return;
		}
		if (url == null) {
			assetImageURL = null;
			VuzeActivitiesManager.triggerEntryChanged(VuzeActivitiesEntry.this);
			return;
		} if (url.equals(assetImageURL)) {
			return;
		}

		assetImageURL = url;
		try {
			ResourceDownloader rd = ResourceDownloaderFactoryImpl.getSingleton().create(
					new URL(url));
			rd.addListener(new ResourceDownloaderAdapter() {
				public boolean completed(ResourceDownloader downloader, InputStream is) {
					try {
						if (is != null && is.available() > 0) {
							imageBytes = new byte[is.available()];
							is.read(imageBytes);
						}
						VuzeActivitiesManager.triggerEntryChanged(VuzeActivitiesEntry.this);
						return true;
					} catch (Exception e) {
						Debug.out(e);
					}
					return false;
				}
			});
			rd.asyncDownload();
		} catch (Exception e) {
			Debug.out(e);
		}
	}

	public Map toMap() {
		Map map = new HashMap();
		map.put("timestamp", new Long(timestamp));
		if (assetHash != null) {
			map.put("assetHash", assetHash);
		} else if (dm != null) {
			try {
				map.put("assetHash", dm.getTorrent().getHashWrapper().toBase32String());
			} catch (Exception e) {
			}
		}
		map.put("icon", icon);
		map.put("id", id);
		map.put("text", text);
		map.put("typeID", typeID);
		map.put("type", new Integer(type));
		map.put("assetImageURL", assetImageURL);

		return map;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		if (this.timestamp == timestamp) {
			return;
		}
		this.timestamp = timestamp;
		if (tableColumn != null) {
			tableColumn.setLastSortValueChange(SystemTime.getCurrentTime());
		}
	}
}
