/**
 * Created on Mar 27, 2008
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
 
package com.aelitis.azureus.core.torrent;

import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.messenger.config.PlatformRatingMessenger;
import com.aelitis.azureus.util.MapUtils;

/**
 * @author TuxPaper
 * @created Mar 27, 2008
 *
 */
public class PlatformRatingInfoList
	implements RatingInfoList
{
	/**
	 * Map: 
	 * Key = Torrent Hash
	 * Value = Map
	 * 
	 *   Map 2:
	 *   Key = Rating Type
	 *   Value = Map
	 *   
	 *     Map 3:
	 *     Keys = average, count, expires-in-mins
	 * @param reply
	 */
	private final Map reply;

	/**
	 * 
	 */
	public PlatformRatingInfoList(Map message) {
		reply = message == null ? new HashMap() : message;
	}

	public boolean hasHash(String hash) {
		return reply.get(hash) != null;
	}

	public long getRatingValue(String hash, String type) {
		int rating = -1;

		Map mapRating = (Map) reply.get(hash);
		if (mapRating != null) {
			Map mapValues = (Map) mapRating.get(PlatformRatingMessenger.RATE_TYPE_CONTENT);
			if (mapValues != null) {
				Object val = mapValues.get("value");
				if (val instanceof Number) {
					rating = ((Number) val).intValue();
				}
			}
		}

		return rating;
	}

	public long getRatingCount(String hash, String type) {
		long rating = -1;
		try {
			Map mapRating = (Map) reply.get(hash);
			if (mapRating != null) {
				Map mapValues = (Map) mapRating.get(PlatformRatingMessenger.RATE_TYPE_CONTENT);
				if (mapValues != null) {
					Object val = mapValues.get("count");
					if (val instanceof Number) {
						rating = ((Number) val).longValue();
					}
				}
			}
		} catch (Exception e) {
			Debug.out(e);
		}

		return rating;
	}

	public String getRatingString(String hash, String type) {
		String rating = "--";

		try {
			Map mapRating = (Map) reply.get(hash);
			if (mapRating != null) {
				Map mapValues = (Map) mapRating.get(PlatformRatingMessenger.RATE_TYPE_CONTENT);
				if (mapValues != null) {
					Object val = mapValues.get("value");
					if (val instanceof String) {
						rating = (String) val;
					} else if (val instanceof Double) {
						rating = ((Double) val).toString();
					}
				}
			}
		} catch (Exception e) {
			Debug.out(e);
		}

		return rating;
	}

	public String getRatingColor(String hash, String type) {
		String color = null;

		try {
			Map mapRating = (Map) reply.get(hash);
			if (mapRating != null) {
				Map mapValues = (Map) mapRating.get(PlatformRatingMessenger.RATE_TYPE_CONTENT);
				if (mapValues != null) {
					Map map = (Map) MapUtils.getMapObject(mapRating,
							"display-settings", null, Map.class);
					if (map != null && map.containsKey("color")) {
						color = (String) map.get("color");
					}
				}
			}
		} catch (Exception e) {
			Debug.out(e);
		}

		return color;
	}

	public long getRatingExpireyMins(String hash, String type) {
		long expiryMins = -1;
		try {
			Map mapRating = (Map) reply.get(hash);
			if (mapRating != null) {
				Map mapValues = (Map) mapRating.get(PlatformRatingMessenger.RATE_TYPE_CONTENT);
				if (mapValues != null) {
					Object val = mapValues.get("expires-in-mins");
					if (val instanceof Long) {
						expiryMins = ((Long) val).longValue();
					}
				}
			}
		} catch (Exception e) {
			Debug.out(e);
		}

		return expiryMins;
	}

	/**
	 * @return
	 */
	public Map getMap() {
		return reply;
	}
}
