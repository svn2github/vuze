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

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.buddy.VuzeBuddy;
import com.aelitis.azureus.buddy.impl.VuzeBuddyManager;
import com.aelitis.azureus.util.MapUtils;

/**
 * @author TuxPaper
 * @created Apr 15, 2008
 *
 */
public class VuzeActivitiesEntryBuddyLinkup
	extends VuzeActivitiesEntry
{
	public static final String TYPEID_BUDDYLINKUP = "buddy-new";

	private VuzeBuddy buddy;

	public VuzeActivitiesEntryBuddyLinkup() {
		super();
	}

	public VuzeActivitiesEntryBuddyLinkup(VuzeBuddy buddy) {
		String url = "<A HREF=\"" + buddy.getProfileUrl("new-buddy-inform") + "\">"
				+ buddy.getDisplayName() + "</A>";

		String text = MessageText.getString("v3.activity.buddy-linkup",
				new String[] {
					url
				});

		setTypeID("buddy-new", true);
		// show multiple link ups
		setID("buddy-new-" + buddy.getLoginID() + "-" + SystemTime.getCurrentTime());
		setText(text);
	}

	public void loadCommonFromMap(Map map) {
		super.loadCommonFromMap(map);

		Map mapNewBuddy = (Map) MapUtils.getMapObject(map, "buddy", new HashMap(),
				Map.class);

		buddy = VuzeBuddyManager.getOrCreatePotentialBuddy(mapNewBuddy);
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
