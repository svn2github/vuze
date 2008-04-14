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

import com.aelitis.azureus.buddy.VuzeBuddy;
import com.aelitis.azureus.plugins.net.buddy.BuddyPlugin;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginBuddy;

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
}
