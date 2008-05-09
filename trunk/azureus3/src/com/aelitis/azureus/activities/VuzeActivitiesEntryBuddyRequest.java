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

import com.aelitis.azureus.buddy.VuzeBuddy;
import com.aelitis.azureus.buddy.impl.VuzeBuddyManager;
import com.aelitis.azureus.util.Constants;
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

	private VuzeBuddy buddy;

	public VuzeActivitiesEntryBuddyRequest() {
		super();
	}

	public VuzeActivitiesEntryBuddyRequest(VuzeBuddy buddy, String acceptURL) {
		this.buddy = buddy;

		String urlAccept = Constants.appendURLSuffix(acceptURL);

		setText("<A HREF=\"" + buddy.getProfileUrl(TYPEID_BUDDYREQUEST) + "\">"
				+ buddy.getDisplayName() + "</A> wants to be your buddy\n \n"
				+ "  <A HREF=\"" + urlAccept + "\">OMG, OF COURSE I ACCEPT!</A>");
		setTypeID(TYPEID_BUDDYREQUEST, true);
		setID(buildID(buddy.getCode()));
	}

	public static String buildID(String code) {
		return TYPEID_BUDDYREQUEST + "-" + code;
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

		buddy = VuzeBuddyManager.createNewBuddyNoAdd(mapFutureBuddy);
	}

	// @see com.aelitis.azureus.activities.VuzeActivitiesEntry#toMap()
	public Map toMap() {
		Map map = super.toMap();

		if (buddy != null) {
			map.put("buddy", buddy.toMap());
		}
		return map;
	}

	public VuzeBuddy getBuddy() {
		return buddy;
	}
}
