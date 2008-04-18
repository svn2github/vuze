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

import java.io.UnsupportedEncodingException;
import java.util.*;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.buddy.VuzeBuddy;
import com.aelitis.azureus.core.messenger.config.PlatformRelayMessenger;
import com.aelitis.azureus.plugins.net.buddy.*;
import com.aelitis.azureus.util.JSONUtils;
import com.aelitis.azureus.util.MapUtils;
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

	private List pluginBuddies = new ArrayList();

	private AEMonitor mon_pluginBuddies = new AEMonitor("pluginBuddies");

	public VuzeBuddyImpl(String publicKey) {
		addPublicKey(publicKey);
	}

	public VuzeBuddyImpl() {
	}

	public void loadFromMap(Map mapNewBuddy) {
		List pkList = MapUtils.getMapList(mapNewBuddy, "pks",
				Collections.EMPTY_LIST);
		for (Iterator iter = pkList.iterator(); iter.hasNext();) {
			String pk = (String) iter.next();
			addPublicKey(pk);
		}
		setDisplayName(MapUtils.getMapString(mapNewBuddy, "display-name", ""
				+ mapNewBuddy.hashCode()));
		setLoginID(MapUtils.getMapString(mapNewBuddy, "login-id", ""
				+ mapNewBuddy.hashCode()));
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
		mon_pluginBuddies.enter();
		try {
			for (Iterator iter = pluginBuddies.iterator(); iter.hasNext();) {
				BuddyPluginBuddy pluginBuddy = (BuddyPluginBuddy) iter.next();
				if (pluginBuddy.isOnline()) {
					return true;
				}
			}
		} finally {
			mon_pluginBuddies.exit();
		}
		return false;
	}

	// @see com.aelitis.azureus.buddy.VuzeBuddy#addPublicKey()
	public void addPublicKey(String pk) {
		mon_pluginBuddies.enter();
		try {
			BuddyPlugin buddyPlugin = VuzeBuddyManager.getBuddyPlugin();
			if (buddyPlugin != null) {
				BuddyPluginBuddy pluginBuddy = buddyPlugin.getBuddyFromPublicKey(pk);
				if (pluginBuddy == null) {
					pluginBuddy = buddyPlugin.addBuddy(pk, BuddyPlugin.SUBSYSTEM_AZ3);
				}

				if (pluginBuddy != null && !pluginBuddies.contains(pluginBuddy)) {
					pluginBuddies.add(pluginBuddy);
				}
			}

		} finally {
			mon_pluginBuddies.exit();
		}
	}

	// @see com.aelitis.azureus.buddy.VuzeBuddy#removePublicKey(java.lang.String)
	public void removePublicKey(String pk) {
		mon_pluginBuddies.enter();
		try {
			BuddyPlugin buddyPlugin = VuzeBuddyManager.getBuddyPlugin();
			if (buddyPlugin != null) {
				BuddyPluginBuddy pluginBuddy = buddyPlugin.getBuddyFromPublicKey(pk);
				if (pluginBuddy == null) {
					pluginBuddies.remove(pluginBuddy);
				}
				// buddyPlugin.removeBuddy(pk, BuddyPlugin.SUBSYSTEM_AZ3);
			}
		} finally {
			mon_pluginBuddies.exit();
		}
	}

	public String[] getPublicKeys() {
		mon_pluginBuddies.enter();
		try {
			String[] ret = new String[pluginBuddies.size()];
			int x = 0;

			for (Iterator iter = pluginBuddies.iterator(); iter.hasNext();) {
				BuddyPluginBuddy pluginBuddy = (BuddyPluginBuddy) iter.next();
				if (pluginBuddy != null) {
					ret[x++] = pluginBuddy.getPublicKey();
				}
			}
		} finally {
			mon_pluginBuddies.exit();
		}
		return new String[0];
	}

	// @see com.aelitis.azureus.buddy.VuzeBuddy#sendActivity(com.aelitis.azureus.util.VuzeActivitiesEntry)
	public void sendActivity(VuzeActivitiesEntry entry) {
		try {
			Map map = new HashMap();

			map.put("VuzeMessageType", "ActivityEntry");
			map.put("ActivityEntry", entry.toMap());

			mon_pluginBuddies.enter();
			try {
				for (Iterator iter = pluginBuddies.iterator(); iter.hasNext();) {
					BuddyPluginBuddy pluginBuddy = (BuddyPluginBuddy) iter.next();
					if (pluginBuddy.isOnline() && false) {
						pluginBuddy.sendMessage(BuddyPlugin.SUBSYSTEM_AZ3, map, 10000,
								new BuddyPluginBuddyReplyListener() {

									public void sendFailed(BuddyPluginBuddy to_buddy,
											BuddyPluginException cause) {

										VuzeBuddyManager.log("SEND FAILED "
												+ to_buddy.getPublicKey());
									}

									public void replyReceived(BuddyPluginBuddy from_buddy,
											Map reply) {
										VuzeBuddyManager.log("REPLY REC "
												+ JSONUtils.encodeToJSON(reply));
									}
								});
					} else {
						VuzeBuddyManager.log("NOT ONLINE: " + pluginBuddy.getPublicKey());
						try {
							PlatformRelayMessenger.put(new String[] {
								pluginBuddy.getPublicKey()
							}, JSONUtils.encodeToJSON(map).getBytes("utf-8"), 0);
							pluginBuddy.setMessagePending();
						} catch (UnsupportedEncodingException e) {
							Debug.out(e);
						}
					}
				}
			} finally {
				mon_pluginBuddies.exit();
			}
		} catch (BuddyPluginException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
