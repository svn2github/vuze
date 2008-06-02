/**
 * Created on Apr 22, 2008
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

package com.aelitis.azureus.buddy;

import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.activities.VuzeActivitiesManager;
import com.aelitis.azureus.util.MapUtils;

/**
 * @author TuxPaper
 * @created Apr 22, 2008
 *
 */
public class QueuedVuzeShare
{
	private String code;

	private String pk;

	private String downloadHash;

	private VuzeActivitiesEntry entry;
	
	private long sharedOn;

	/**
	 * @param map
	 */
	public QueuedVuzeShare(Map map) {
		loadFromMap(map);
	}

	/**
	 * 
	 */
	public QueuedVuzeShare() {
		setSharedOn(SystemTime.getCurrentTime());
	}

	/**
	 * @param code the code to set
	 */
	public void setCode(String code) {
		this.code = code;
	}

	/**
	 * @return the code
	 */
	public String getCode() {
		return code;
	}

	/**
	 * @param pk the pk to set
	 */
	public void setPk(String pk) {
		this.pk = pk;
	}

	/**
	 * @return the pk
	 */
	public String getPk() {
		return pk;
	}

	/**
	 * @param downloadHash the downloadHash to set
	 */
	public void setDownloadHash(String downloadHash) {
		this.downloadHash = downloadHash;
	}

	/**
	 * @return the downloadHash
	 */
	public String getDownloadHash() {
		return downloadHash;
	}

	/**
	 * @param entry the entry to set
	 */
	public void setActivityEntry(VuzeActivitiesEntry entry) {
		this.entry = entry;
	}

	/**
	 * @return the entry
	 */
	public VuzeActivitiesEntry getActivityEntry() {
		return entry;
	}

	/**
	 * @return
	 *
	 * @since 3.0.5.3
	 */
	public Map toMap() {
		Map map = new HashMap();
		map.put("code", code);
		if (entry != null) {
			map.put("ActivityEntry", entry.toMap());
		}
		map.put("hash", downloadHash);
		map.put("pk", pk);
		map.put("sharedOn", new Long(sharedOn));

		return map;
	}

	/**
	 * @param map
	 *
	 * @since 3.0.5.3
	 */
	private void loadFromMap(Map map) {
		setCode(MapUtils.getMapString(map, "code", null));
		setSharedOn(MapUtils.getMapLong(map, "sharedOn", 0));
		
		Map entryMap = MapUtils.getMapMap(map, "ActivityEntry", null);
		if (entryMap != null) {
			VuzeActivitiesEntry entry = VuzeActivitiesManager.createEntryFromMap(
					entryMap, true);
			setActivityEntry(entry);
			if (sharedOn == 0) {
				setSharedOn(entry.getTimestamp());
			}
		} else {
			setActivityEntry(null);
		}
		setDownloadHash(MapUtils.getMapString(map, "hash", null));
		setPk(MapUtils.getMapString(map, "pk", null));
		if (sharedOn == 0) {
			sharedOn = SystemTime.getCurrentTime();
		}
	}

	public long getSharedOn() {
		return sharedOn;
	}

	public void setSharedOn(long sharedOn) {
		this.sharedOn = sharedOn;
	}
}
