/**
 * Created on Jun 17, 2008
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
import com.aelitis.azureus.buddy.VuzeBuddyListener;
import com.aelitis.azureus.buddy.impl.VuzeBuddyManager;
import com.aelitis.azureus.util.MapUtils;

/**
 * @author TuxPaper
 * @created Jun 17, 2008
 *
 */
public class VuzeActivitiesEntryBuddy
	extends VuzeActivitiesEntry
	implements VuzeBuddyListener
{
	protected VuzeBuddy buddy;


	public void loadCommonFromMap(Map map) {
		super.loadCommonFromMap(map);

		Map mapNewBuddy = (Map) MapUtils.getMapObject(map, "buddy", new HashMap(),
				Map.class);
		if (mapNewBuddy == null) {
			String buddyID = MapUtils.getMapString(map, "buddyID", null);
			if (buddyID != null) {
				buddy = VuzeBuddyManager.getBuddyByLoginID(buddyID);
			}
		} else {
			buddy = VuzeBuddyManager.getOrCreatePotentialBuddy(mapNewBuddy);
		}
		
		if (buddy != null) {
			buddy.addListener(this);
		}
	}

	// @see com.aelitis.azureus.activities.VuzeActivitiesEntry#toMap()
	public Map toMap() {
		Map map = super.toMap();

		if (buddy != null) {
			map.put("buddy", buddy.toMap());
			map.put("buddyID", buddy.getLoginID());
		}
		return map;
	}

	public VuzeBuddy getBuddy() {
		return buddy;
	}

	public void setBuddy(VuzeBuddy buddy) {
		if (buddy != this.buddy) {
			if (this.buddy != null) {
				buddy.removeListener(this);
			}
		}
		this.buddy = buddy;

		if (buddy != null) {
			buddy.addListener(this);
		}
	}

	// @see com.aelitis.azureus.buddy.VuzeBuddyListener#buddyAdded(com.aelitis.azureus.buddy.VuzeBuddy, int)
	public void buddyAdded(VuzeBuddy buddy, int position) {
	}

	// @see com.aelitis.azureus.buddy.VuzeBuddyListener#buddyChanged(com.aelitis.azureus.buddy.VuzeBuddy)
	public void buddyChanged(VuzeBuddy buddy) {
		VuzeActivitiesManager.triggerEntryChanged(this);
	}

	// @see com.aelitis.azureus.buddy.VuzeBuddyListener#buddyOrderChanged()
	public void buddyOrderChanged() {
	}

	// @see com.aelitis.azureus.buddy.VuzeBuddyListener#buddyRemoved(com.aelitis.azureus.buddy.VuzeBuddy)
	public void buddyRemoved(VuzeBuddy buddy) {
	}
}
