/**
 * Created on Apr 14, 2008
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

package com.aelitis.azureus.buddy.impl;

import java.util.HashMap;
import java.util.Map;

import com.aelitis.azureus.buddy.VuzeBuddy;
import com.aelitis.azureus.plugins.net.buddy.*;
import com.aelitis.azureus.util.JSONUtils;
import com.aelitis.azureus.util.VuzeActivitiesEntry;

/**
 * BuddyPluginBuddy plus some vuze specific stuff
 * 
 * @author TuxPaper
 * @created Apr 14, 2008
 *
 */
public class VuzeBuddyImpl
	implements VuzeBuddy
{
	private String displayName;

	private String loginID;

	private long lastUpdated;

	private byte[] avatar;

	private BuddyPluginBuddy pluginBuddy;

	public VuzeBuddyImpl(String publicKey) {
		BuddyPlugin buddyPlugin = VuzeBuddyManager.getBuddyPlugin();
		if (buddyPlugin != null) {
			pluginBuddy = buddyPlugin.getBuddyFromPublicKey(publicKey);
			if (pluginBuddy == null) {
				buddyPlugin.addBuddy(publicKey);
				pluginBuddy = buddyPlugin.getBuddyFromPublicKey(publicKey);
			}
		}
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getLoginID() {
		return loginID;
	}

	public void setLoginID(String loginID) {
		this.loginID = loginID;
	}

	public long getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(long lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	public byte[] getAvatar() {
		return avatar;
	}

	public void setAvatar(byte[] avatar) {
		this.avatar = avatar;
	}

	public boolean isOnline() {
		if (pluginBuddy != null) {
			return pluginBuddy.isOnline();
		}
		return false;
	}

	public String getPublicKey() {
		if (pluginBuddy != null) {
			return pluginBuddy.getPublicKey();
		}
		return null;
	}

	// @see com.aelitis.azureus.buddy.VuzeBuddy#sendActivity(com.aelitis.azureus.util.VuzeActivitiesEntry)
	public void sendActivity(VuzeActivitiesEntry entry) {
		if (pluginBuddy == null) {
			return;
		}
		BuddyPlugin buddyPlugin = VuzeBuddyManager.getBuddyPlugin();
		if (buddyPlugin == null) {
			return;
		}

		try {
			Map map = new HashMap();
			
			map.put("VuzeMessageType", "ActivityEntry");
			map.put("ActivityEntry", entry.toMap());

			pluginBuddy.sendMessage(BuddyPlugin.SUBSYSTEM_AZ3, map, 10000,
					new BuddyPluginBuddyReplyListener() {

						public void sendFailed(BuddyPluginBuddy to_buddy,
								BuddyPluginException cause) {
							VuzeBuddyManager.log("SEND FAILED");
						}

						public void replyReceived(BuddyPluginBuddy from_buddy, Map reply) {
							VuzeBuddyManager.log("REPLY REC " + JSONUtils.encodeToJSON(reply));
						}
					});
		} catch (BuddyPluginException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
