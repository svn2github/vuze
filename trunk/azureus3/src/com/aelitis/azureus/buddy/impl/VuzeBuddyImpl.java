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

import java.util.*;

import org.bouncycastle.util.encoders.Base64;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.buddy.VuzeBuddy;
import com.aelitis.azureus.core.messenger.config.PlatformRelayMessenger;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.plugins.net.buddy.*;
import com.aelitis.azureus.util.*;
import com.aelitis.azureus.util.LoginInfoManager.LoginInfo;

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
	private static final int SEND_P2P_TIMEOUT = 1000 * 60 * 3;

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
		byte[] avatarBytes = MapUtils.getMapByteArray(mapNewBuddy, "avatar", null);
		if (avatarBytes == null) {
			String avatarB64 = MapUtils.getMapString(mapNewBuddy, "avatar.B64", null);
			if (avatarB64 != null) {
				avatarBytes = Base64.decode(avatarB64);
			} else {
				String avatarB32 = MapUtils.getMapString(mapNewBuddy, "avatar.B32",
						null);
				avatarBytes = Base32.decode(avatarB32);
			}
		}
		if (avatarBytes != null) {
			setAvatar(avatarBytes);
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
		// We add public keys by adding BuddyPluginBuddy
		mon_pluginBuddies.enter();
		try {
			BuddyPluginBuddy pluginBuddy = VuzeBuddyManager.getBuddyPluginBuddyForVuze(pk);

			if (pluginBuddy != null && !pluginBuddies.contains(pluginBuddy)) {
				pluginBuddies.add(pluginBuddy);
			}

		} finally {
			mon_pluginBuddies.exit();
		}

		VuzeBuddyManager.linkPKtoBuddy(pk, this);
	}

	// @see com.aelitis.azureus.buddy.VuzeBuddy#removePublicKey(java.lang.String)
	public void removePublicKey(String pk) {
		// our public key list is actually a BuddyPluginBuddy list, so find
		// it in our list and remove it
		mon_pluginBuddies.enter();
		try {
			BuddyPlugin buddyPlugin = VuzeBuddyManager.getBuddyPlugin();
			if (buddyPlugin != null) {
				BuddyPluginBuddy pluginBuddy = buddyPlugin.getBuddyFromPublicKey(pk);
				if (pluginBuddy != null) {
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
			final Map map = new HashMap();

			map.put("VuzeMessageType", "ActivityEntry");
			map.put("ActivityEntry", entry.toMap());

			mon_pluginBuddies.enter();
			try {
				for (Iterator iter = pluginBuddies.iterator(); iter.hasNext();) {
					BuddyPluginBuddy pluginBuddy = (BuddyPluginBuddy) iter.next();
					if (pluginBuddy.isOnline()) {
						pluginBuddy.sendMessage(BuddyPlugin.SUBSYSTEM_AZ3, map,
								SEND_P2P_TIMEOUT, new BuddyPluginBuddyReplyListener() {

									public void sendFailed(BuddyPluginBuddy to_buddy,
											BuddyPluginException cause) {
										VuzeBuddyManager.log("SEND FAILED "
												+ to_buddy.getPublicKey() + "\n" + cause);
										sendViaRelayServer(to_buddy, map);
									}

									public void replyReceived(BuddyPluginBuddy from_buddy,
											Map reply) {
										VuzeBuddyManager.log("REPLY REC "
												+ JSONUtils.encodeToJSON(reply));
										String response = MapUtils.getMapString(reply, "response",
												"");
										if (!response.toLowerCase().equals("ok")) {
											sendViaRelayServer(from_buddy, map);
										}
									}
								});
					} else {
						VuzeBuddyManager.log("NOT ONLINE: " + pluginBuddy.getPublicKey());
						sendViaRelayServer(pluginBuddy, map);
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

	/**
	 * @param publicKey
	 * @param map
	 *
	 * @since 3.0.5.3
	 */
	private void sendViaRelayServer(BuddyPluginBuddy pluginBuddy, Map map) {
		try {
			PlatformRelayMessenger.put(new String[] {
				pluginBuddy.getPublicKey()
			}, JSONUtils.encodeToJSON(map).getBytes("utf-8"), 0);
			pluginBuddy.setMessagePending();
		} catch (Exception e) {
			// TODO: Store for later

			Debug.out(e);
		}
	}

	public void shareDownload(DownloadManager dm, String message) {
		if (dm == null) {
			return;
		}
		TOTorrent torrent = dm.getTorrent();
		if (torrent == null) {
			return;
		}

		HashWrapper hashWrapper = null;
		try {
			hashWrapper = torrent.getHashWrapper();
		} catch (TOTorrentException e) {
		}

		if (hashWrapper == null) {
			return;
		}

		LoginInfo userInfo = LoginInfoManager.getInstance().getUserInfo();
		if (userInfo == null || userInfo.userID == null) {
			// TODO: Login!
			VuzeBuddyManager.log("Can't share download: Not logged in");
		}

		VuzeActivitiesEntry entry = new VuzeActivitiesEntry();

		// make all shares unique (so if the user shares the same content twice,
		// their buddy gets two entries)
		entry.setID("Buddy-share-" + SystemTime.getCurrentTime());
		String text = userInfo.userName + " is sharing "
				+ PlatformTorrentUtils.getContentTitle(torrent) + " with you.";
		if (message != null) {
			text += "\n \nMessage from " + userInfo.userName + ":\n" + message;
		}
		entry.setText(text);
		entry.setAssetHash(hashWrapper.toBase32String());
		entry.setDownloadManager(dm);
		entry.setShowThumb(true);
		entry.setTypeID("buddy-share", true);
		entry.setImageBytes(PlatformTorrentUtils.getContentThumbnail(torrent));

		sendActivity(entry);
	}
}
