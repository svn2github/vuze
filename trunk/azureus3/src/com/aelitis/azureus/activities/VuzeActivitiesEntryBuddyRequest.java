/**
 * Created on Apr 15, 2008
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

package com.aelitis.azureus.activities;

import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.UrlUtils;

import com.aelitis.azureus.util.Constants;
import com.aelitis.azureus.util.JSONUtils;
import com.aelitis.azureus.util.MapUtils;

/**
 * @author TuxPaper
 * @created Apr 15, 2008
 *
 */
public class VuzeActivitiesEntryBuddyRequest
	extends VuzeActivitiesEntry
{
	public static final String TYPEID_BUDDYREQUEST = "buddy-request";

	String futureBuddyLoginID;

	String futureBuddyDisplayName;

	public VuzeActivitiesEntryBuddyRequest() {
		super();
	}

	public VuzeActivitiesEntryBuddyRequest(String pk, String loginID,
			String displayName) {
		futureBuddyLoginID = loginID;
		futureBuddyDisplayName = displayName;

		String urlUser = Constants.URL_PREFIX + Constants.URL_USER + loginID + "?"
				+ Constants.URL_SUFFIX + "&client_ref=buddy-request";
		String urlAccept = Constants.URL_PREFIX + Constants.URL_BUDDY_ACCEPT
				+ loginID + "?" + Constants.URL_SUFFIX;
		// temporary
		Map map = new HashMap();
		Map mapBuddy = new HashMap();
		mapBuddy.put("pks", new String[] { pk });
		mapBuddy.put("login-id", loginID);
		mapBuddy.put("display-name", displayName);
		map.put("buddy", mapBuddy);
		urlAccept = UrlUtils.encode("AZMSG;0;buddy;accept;" + JSONUtils.encodeToJSON(map));

		setText("<A HREF=\"" + urlUser + "\">" + displayName
				+ "</A> wants to be your buddy\n \n" + "  <A HREF=\"" + urlAccept
				+ "\">OMG, OF COURSE I ACCEPT!</A>");
		setTypeID(TYPEID_BUDDYREQUEST, true);
		setID(TYPEID_BUDDYREQUEST + "-" + Math.random());
	}

	// @see com.aelitis.azureus.activities.VuzeActivitiesEntry#loadFromExternalMap(java.util.Map)
	public void loadFromExternalMap(Map platformEntry) {
		super.loadFromExternalMap(platformEntry);
		loadOtherValuesFromMap(platformEntry);
	}

	// @see com.aelitis.azureus.activities.VuzeActivitiesEntry#loadFromInternalMap(java.util.Map)
	public void loadFromInternalMap(Map map) {
		super.loadFromInternalMap(map);
		loadOtherValuesFromMap(map);
	}

	private void loadOtherValuesFromMap(Map map) {
		Map mapFutureBuddy = (Map) MapUtils.getMapObject(map, "buddy",
				new HashMap(), Map.class);
		futureBuddyLoginID = MapUtils.getMapString(mapFutureBuddy, "login-id",
				"unknown");
		futureBuddyDisplayName = MapUtils.getMapString(mapFutureBuddy,
				"display-name", "Mr Unkown");
	}

	// @see com.aelitis.azureus.activities.VuzeActivitiesEntry#toMap()
	public Map toMap() {
		Map map = super.toMap();

		Map mapFutureBuddy = new HashMap();
		mapFutureBuddy.put("login-id", futureBuddyLoginID);
		mapFutureBuddy.put("display-name", futureBuddyDisplayName);

		map.put("buddy", mapFutureBuddy);
		return map;
	}

	public String getFutureBuddyLoginID() {
		return futureBuddyLoginID;
	}

	public String getFutureBuddyDisplayName() {
		return futureBuddyDisplayName;
	}
}
