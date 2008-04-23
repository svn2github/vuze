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

import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.activities.VuzeActivitiesEntryBuddyRequest;
import com.aelitis.azureus.activities.VuzeActivitiesManager;
import com.aelitis.azureus.buddy.QueuedVuzeShare;
import com.aelitis.azureus.buddy.VuzeBuddy;
import com.aelitis.azureus.buddy.VuzeBuddyCreator;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.crypto.VuzeCryptoException;
import com.aelitis.azureus.core.crypto.VuzeCryptoManager;
import com.aelitis.azureus.core.messenger.PlatformMessenger;
import com.aelitis.azureus.core.messenger.config.*;
import com.aelitis.azureus.plugins.net.buddy.*;
import com.aelitis.azureus.util.*;
import com.aelitis.azureus.util.Constants;
import com.aelitis.azureus.util.LoginInfoManager.LoginInfo;

import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginInterface;

/**
 * General Management of Vuze Buddies.
 * <P>
 * requires one init() call before being used
 * 
 * @author TuxPaper
 * @created Apr 14, 2008
 *
 */
public class VuzeBuddyManager
{
	private static final int SEND_P2P_TIMEOUT = 1000 * 60 * 3;

	protected static final boolean ALLOW_ONLY_AZ3 = false;

	private static BuddyPlugin buddyPlugin = null;

	private static List buddyList = new ArrayList();

	private static AEMonitor buddy_mon = new AEMonitor("buddy list/map");

	private static Map mapPKtoVuzeBuddy = new HashMap();

	private static VuzeBuddyCreator vuzeBuddyCreator;

	/**
	 * @param vuzeBuddyCreator
	 *
	 * @since 3.0.5.3
	 */
	public static void init(final VuzeBuddyCreator vuzeBuddyCreator) {
		VuzeBuddyManager.vuzeBuddyCreator = vuzeBuddyCreator;

		try {
			PluginInterface pi;
			pi = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByID(
					"azbuddy");

			if (pi != null) {
				Plugin plugin = pi.getPlugin();
				if (plugin instanceof BuddyPlugin) {
					linkupBuddyPlugin((BuddyPlugin) plugin);
				}
			}
		} catch (Throwable t) {
			Debug.out(t);
		}

		try {
			VuzeRelayListener vuzeRelayListener = new VuzeRelayListener() {
				// @see com.aelitis.azureus.core.messenger.config.VuzeRelayListener#newRelayServerPayLoad(com.aelitis.azureus.buddy.VuzeBuddy, java.lang.String, byte[])
				public void newRelayServerPayLoad(VuzeBuddy sender, String pkSender,
						byte[] payload) {
					try {
						String s = new String(payload, "utf-8");
						Map mapPayload = JSONUtils.decodeJSON(s);

						processPayloadMap(pkSender, mapPayload, sender != null);
					} catch (UnsupportedEncodingException e) {
						Debug.out(e);
					}
				}
			};

			PlatformRelayMessenger.addRelayServerListener(vuzeRelayListener);

			// do one relay check, which will setup a recheck cycle
			PlatformRelayMessenger.relayCheck();
		} catch (Throwable t) {
			Debug.out(t);
		}
	}

	/**
	 * Set and listen to buddy plugin.  Process incoming buddy plugin messages
	 * that are for vuze 
	 * 
	 * @param _buddyPlugin
	 *
	 * @since 3.0.5.3
	 */
	private static void linkupBuddyPlugin(final BuddyPlugin _buddyPlugin) {
		if (_buddyPlugin == null) {
			return;
		}
		buddyPlugin = _buddyPlugin;

		BuddyPluginListener listener = new BuddyPluginListener() {
			public void messageLogged(String str) {
			}

			public void initialised(boolean available) {
			}

			public void buddyRemoved(BuddyPluginBuddy buddy) {
				if (!canHandleBuddy(buddy)) {
					return;
				}
				try {
					buddy_mon.enter();

					String pk = buddy.getPublicKey();

					VuzeBuddy vuzeBuddy = (VuzeBuddy) mapPKtoVuzeBuddy.remove(pk);
					if (vuzeBuddy != null) {
						vuzeBuddy.removePublicKey(pk);
						if (vuzeBuddy.getPublicKeys().length == 0) {
							buddyList.remove(buddy);
						}
					}
				} finally {
					buddy_mon.exit();
				}
			}

			public void buddyChanged(BuddyPluginBuddy buddy) {
				if (!canHandleBuddy(buddy)) {
					return;
				}

				try {
					buddy_mon.enter();

					String pk = buddy.getPublicKey();

					VuzeBuddy vuzeBuddy = (VuzeBuddy) mapPKtoVuzeBuddy.get(pk);
					if (vuzeBuddy != null) {
						vuzeBuddy.setDisplayName(buddy.getNickName());
					} else {
						buddyAdded(buddy);
					}
				} finally {
					buddy_mon.exit();
				}
			}

			public void buddyAdded(BuddyPluginBuddy buddy) {
				if (!canHandleBuddy(buddy)) {
					return;
				}

				String pk = buddy.getPublicKey();

				VuzeBuddy vuzeBuddy = (VuzeBuddy) mapPKtoVuzeBuddy.get(pk);
				if (vuzeBuddy != null) {
					// already exists
					return;
				}

				createNewBuddy(buddy, true);
			}
		};

		BuddyPluginBuddyRequestListener requestListener = new BuddyPluginBuddyRequestListener() {
			public Map requestReceived(BuddyPluginBuddy from_buddy, int subsystem,
					Map request) throws BuddyPluginException {
				if (subsystem != BuddyPlugin.SUBSYSTEM_AZ3) {
					return null;
				}

				Map mapResponse = new HashMap();

				try {
					String pk = from_buddy.getPublicKey();

					String reply = processPayloadMap(pk, request,
							from_buddy.isAuthorised());
					mapResponse.put("response", reply);
				} catch (Exception e) {
					mapResponse.put("response", "Exception: " + e.toString());
					Debug.out(e);
				}

				return mapResponse;
			}

			public void pendingMessages(BuddyPluginBuddy[] from_buddies) {
				for (int i = 0; i < from_buddies.length; i++) {
					BuddyPluginBuddy pluginBuddy = from_buddies[i];

					String pk = pluginBuddy.getPublicKey();
					VuzeBuddy vuzeBuddy = getBuddyByPK(pk);
					if (vuzeBuddy != null) {
						PlatformMessenger.debug("Relay: YGM from " + pk);
						PlatformRelayMessenger.fetch(0);
					} else {
						PlatformMessenger.debug("Relay: YGM from non vuzer " + pk);
					}
				}
			}

		};

		// TODO create an addListener that triggers for existing buddies
		buddyPlugin.addListener(listener);
		buddyPlugin.addRequestListener(requestListener);
		List buddies = buddyPlugin.getBuddies();
		for (int i = 0; i < buddies.size(); i++) {
			BuddyPluginBuddy buddy = (BuddyPluginBuddy) buddies.get(i);
			if (canHandleBuddy(buddy)) {
				listener.buddyAdded(buddy);
			}
		}
	}

	/**
	 * Processes a payload map, either from a P2P message, relay server, or
	 * webapp, or elsewhere.
	 * 
	 * @param mapPayload
	 *s
	 * @param authorizedBuddy 
	 * @since 3.0.5.3
	 */
	protected static String processPayloadMap(String pkSender, Map mapPayload,
			boolean authorizedBuddy) {
		String mt = MapUtils.getMapString(mapPayload, "VuzeMessageType", "");

		if (mt.equals("ActivityEntry")) {
			Map mapEntry = (Map) MapUtils.getMapObject(mapPayload, "ActivityEntry",
					new HashMap(), Map.class);
			VuzeActivitiesEntry entry = VuzeActivitiesManager.createEntryFromMap(
					mapEntry, true);

			// NOTE: The timestamps of these entries might be horribly off.  We
			//       should probably handle that somehow.
			if (entry != null) {
				if (authorizedBuddy) {
					VuzeActivitiesManager.addEntries(new VuzeActivitiesEntry[] {
						entry
					});
					return "Ok";
				}

				// not Authorized
				if (VuzeActivitiesEntryBuddyRequest.TYPEID_BUDDYREQUEST.equals(entry.getTypeID())) {
					VuzeActivitiesManager.addEntries(new VuzeActivitiesEntry[] {
						entry
					});
					return "Ok";
				} else {
					return "Not Authorized";
				}
			}
		} else if (authorizedBuddy && mt.equals("BuddySync")) {
			PlatformBuddyMessenger.sync(null);
			return "Ok";
		} else if (mt.equals("BuddyAccept")) {
			String code = MapUtils.getMapString(mapPayload, "BuddyAcceptCode", null);
			VuzeQueuedShares.updateSharePK(code, pkSender);
			// Once sync is done, we will get a buddy add, and send the queued share(s)
			PlatformBuddyMessenger.sync(null);
		}

		return "Unknown Message Type";
	}

	/**
	 * Determines if this is a plugin buddy we should handle in vuze.
	 * 
	 * @param buddy
	 * @return
	 *
	 * @since 3.0.5.3
	 */
	private static boolean canHandleBuddy(BuddyPluginBuddy buddy) {
		if (buddy == null) {
			return false;
		}
		if (ALLOW_ONLY_AZ3) {
			int subsystem = buddy.getSubsystem();
			return subsystem == BuddyPlugin.SUBSYSTEM_AZ3;
		}

		return true;
	}

	/**
	 * Get direct access tot he buddy plugin.  Usually not a good idea.<br>
	 * Should be never called from the UI.
	 * 
	 * @return
	 *
	 * @since 3.0.5.3
	 */
	public static BuddyPlugin getBuddyPlugin() {
		return buddyPlugin;
	}

	/**
	 * Retrieve a list of all buddies
	 * 
	 * @return List of VuzeBuddy objects.  Adding/Removing from this list
	 *         does not add/remove buddies
	 *
	 * @since 3.0.5.3
	 */
	public static List getAllVuzeBuddies() {
		try {
			buddy_mon.enter();

			return new ArrayList(buddyList);
		} finally {
			buddy_mon.exit();
		}
	}

	/**
	 * Retrieve a VuzeBuddy using their public key
	 * 
	 * @param pk
	 * @return
	 *
	 * @since 3.0.5.3
	 */
	public static VuzeBuddy getBuddyByPK(String pk) {
		try {
			buddy_mon.enter();

			return (VuzeBuddy) mapPKtoVuzeBuddy.get(pk);
		} finally {
			buddy_mon.exit();
		}
	}

	/**
	 * Retrieve a VuzeBuddy using their login id
	 * 
	 * @param loginID
	 * @return
	 *
	 * @since 3.0.5.3
	 */
	public static VuzeBuddy getBuddyByLoginID(String loginID) {
		if (loginID == null) {
			return null;
		}
		loginID = loginID.toLowerCase();

		try {
			buddy_mon.enter();

			// NOTE: Could probably be optimized so we don't search via walk through 
			for (Iterator iter = buddyList.iterator(); iter.hasNext();) {
				VuzeBuddy buddy = (VuzeBuddy) iter.next();

				String loginID2 = buddy.getLoginID();

				if (loginID2 != null && loginID.equals(loginID2.toLowerCase())) {
					return buddy;
				}
			}
		} finally {
			buddy_mon.exit();
		}
		return null;
	}

	public static void log(String s) {
		AEDiagnosticsLogger diag_logger = AEDiagnostics.getLogger("v3.Buddy");
		diag_logger.log(s);
		if (Constants.DIAG_TO_STDOUT) {
			System.out.println(Thread.currentThread().getName() + "|"
					+ System.currentTimeMillis() + "] " + s);
		}
	}

	/**
	 * @param buddy
	 *
	 * @since 3.0.5.3
	 */
	public static void addBuddy(VuzeBuddy buddy, boolean createActivityEntry) {
		try {
			buddy_mon.enter();

			if (!buddyList.contains(buddy)) {
				log("Add new buddy to Manager");
				buddyList.add(buddy);

				if (createActivityEntry) {
					VuzeActivitiesEntry entry = new VuzeActivitiesEntry();
					entry.setTypeID("buddy-new", true);
					entry.setID("buddy-new-" + buddy.getLoginID());
					entry.setText(buddy.getDisplayName()
							+ " has become your buddy.  Huzzah! :D");
					VuzeActivitiesManager.addEntries(new VuzeActivitiesEntry[] {
						entry
					});
				}
			}

		} finally {
			buddy_mon.exit();
		}

		// Send Queued Shares
		String[] publicKeys = buddy.getPublicKeys();
		for (int i = 0; i < publicKeys.length; i++) {
			String pk = publicKeys[i];
			List shares = VuzeQueuedShares.getSharesByPK(pk);
			for (Iterator iter = shares.iterator(); iter.hasNext();) {
				QueuedVuzeShare share = (QueuedVuzeShare) iter.next();
				VuzeActivitiesEntry entry = share.getActivityEntry();
				buddy.sendActivity(entry);

				VuzeQueuedShares.remove(share);
			}
		}
	}

	/**
	 * Creates and adds a new VuzeBuddy via their public key
	 * 
	 * @param pk
	 * @return
	 *
	 * @since 3.0.5.3
	 */
	public static VuzeBuddy createNewBuddy(BuddyPluginBuddy buddy,
			boolean createActivityEntry) {
		String pk = buddy.getPublicKey();

		VuzeBuddy newBuddy;
		if (vuzeBuddyCreator == null) {
			newBuddy = new VuzeBuddyImpl(pk);
		} else {
			newBuddy = vuzeBuddyCreator.createBuddy(pk);
		}

		if (newBuddy == null) {
			return null;
		}

		if (newBuddy != null) {
			newBuddy.setDisplayName(buddy.getName());
		}

		getBuddyPluginBuddyForVuze(pk);

		addBuddy(newBuddy, createActivityEntry);

		return newBuddy;
	}

	/**
	 * Creates, Adds, and sets properties of a VuzeBuddy using a predefined
	 * map representation of a VuzeBuddy
	 * 
	 * @param mapNewBuddy
	 * @return 
	 *
	 * @since 3.0.5.3
	 */
	public static VuzeBuddy createNewBuddy(Map mapNewBuddy,
			boolean createActivityEntry) {
		if (buddyPlugin == null) {
			return null;
		}
		VuzeBuddy newBuddy;
		if (vuzeBuddyCreator == null) {
			newBuddy = new VuzeBuddyImpl();
		} else {
			newBuddy = vuzeBuddyCreator.createBuddy();
		}

		if (newBuddy == null) {
			return null;
		}

		newBuddy.loadFromMap(mapNewBuddy);

		addBuddy(newBuddy, createActivityEntry);

		return newBuddy;
	}

	/**
	 * @param loginID
	 *
	 * @since 3.0.5.3
	 */
	public static void removeBuddy(VuzeBuddy buddy) {
		if (buddy == null) {
			return;
		}

		try {
			buddy_mon.enter();

			buddyList.remove(buddy);

			// TODO: Remove all public keys too (pluginBuddy)

		} finally {
			buddy_mon.exit();
		}
	}

	/**
	 * @param updateTime
	 *
	 * @since 3.0.5.3
	 */
	public static void removeBuddiesOlderThan(long updateTime) {
		try {
			buddy_mon.enter();

			for (Iterator iter = buddyList.iterator(); iter.hasNext();) {
				VuzeBuddy buddy = (VuzeBuddy) iter.next();

				if (buddy.getLastUpdated() < updateTime) {
					String[] publicKeys = buddy.getPublicKeys();
					for (int i = 0; i < publicKeys.length; i++) {
						String pk = publicKeys[i];
						mapPKtoVuzeBuddy.remove(pk);
					}

					iter.remove();
				}
			}

		} finally {
			buddy_mon.exit();
		}
	}

	/**
	 * @param pk
	 * @param vuzeBuddyImpl
	 *
	 * @since 3.0.5.3
	 */
	protected static void linkPKtoBuddy(String pk, VuzeBuddy buddy) {
		try {
			buddy_mon.enter();

			log("add PK " + pk);
			mapPKtoVuzeBuddy.put(pk, buddy);

		} finally {
			buddy_mon.exit();
		}
	}

	/**
	 * Gets a BuddyPluginBuddy using a public key.  Creates the BuddyPluginBuddy
	 * if it doesn't exist yet.  Ensures it's of Vuze type.
	 * 
	 * @param pk
	 * @return
	 *
	 * @since 3.0.5.3
	 */
	protected static BuddyPluginBuddy getBuddyPluginBuddyForVuze(String pk) {
		if (buddyPlugin == null) {
			return null;
		}

		BuddyPluginBuddy pluginBuddy = buddyPlugin.getBuddyFromPublicKey(pk);
		if (pluginBuddy == null) {
			pluginBuddy = buddyPlugin.addBuddy(pk, BuddyPlugin.SUBSYSTEM_AZ3);
		} else {
			pluginBuddy.setSubsystem(BuddyPlugin.SUBSYSTEM_AZ3);
		}
		return pluginBuddy;
	}

	public static void invitePKs(String[] pks) {
		String myPK;
		try {
			myPK = VuzeCryptoManager.getSingleton().getPublicKey(null);
		} catch (VuzeCryptoException e) {
			Debug.out(e);
			return;
		}
		LoginInfo userInfo = LoginInfoManager.getInstance().getUserInfo();

		for (int i = 0; i < pks.length; i++) {
			String pk = pks[i];

			BuddyPluginBuddy pluginBuddy = buddyPlugin.addBuddy(pk,
					BuddyPlugin.SUBSYSTEM_AZ3);

			VuzeActivitiesEntryBuddyRequest entry = new VuzeActivitiesEntryBuddyRequest(
					myPK, userInfo.userID, userInfo.userName);
			// P2P will probably fail (since we aren't buddies yet), but we try
			// just in case we already are.  On fail, it writes YGM and to the relay
			// server which is really what we want to do.
			sendActivity(entry, new BuddyPluginBuddy[] {
				pluginBuddy
			});
		}
	}
	
	public static void inviteNonVuzers(String[] codes) {
		for (int i = 0; i < codes.length; i++) {
			String code = codes[i];
			VuzeQueuedShares.add(code);
		}
	}

	/**
	 * You've accepted an invite
	 * 
	 * @param code Invite code or somesuch id that the webapp gave you
	 * @param pks Public Keys of the user you accepted the invite from
	 *
	 * @since 3.0.5.3
	 */
	public static void acceptInvite(final String code, final String pks[]) {
		PlatformBuddyMessenger.sync(new VuzeBuddySyncListener() {
			public void syncComplete() {
				Map map = new HashMap();
				map.put("VuzeMessageType", "BuddyAccept");
				map.put("BuddyAcceptCode", code);

				for (int i = 0; i < pks.length; i++) {
					String pk = pks[i];

					VuzeBuddy buddy = getBuddyByPK(pk);

					if (buddy != null) {
						buddy.sendPayloadMap(map);
						// send will send to all public keys of buddy, so there's no need
						// to go through the rest of the pks
						break;
					}
				}
			}
		});
	}

	/**
	 * @param publicKey
	 * @param map
	 *
	 * @since 3.0.5.3
	 */
	protected static void sendViaRelayServer(BuddyPluginBuddy pluginBuddy, Map map) {
		try {
			PlatformRelayMessenger.put(new String[] {
				pluginBuddy.getPublicKey()
			}, JSONUtils.encodeToJSON(map).getBytes("utf-8"), 0);

			pluginBuddy.setMessagePending();
		} catch (BuddyPluginException be) {
			// set message pending failed.. probably because plugin isn't fully
			// initialized.
			// We could try send YGM later..
		} catch (Exception e) {
			// TODO: Store for later
			Debug.out(e);
		}
	}

	protected static void sendActivity(VuzeActivitiesEntry entry,
			BuddyPluginBuddy[] buddies) {
		final Map map = new HashMap();

		map.put("VuzeMessageType", "ActivityEntry");
		map.put("ActivityEntry", entry.toMap());

		sendPayloadMap(map, buddies);
	}

	/**
	 * @param map
	 * @param buddies
	 *
	 * @since 3.0.5.3
	 */
	public static void sendPayloadMap(final Map map, BuddyPluginBuddy[] buddies) {
		try {
			for (int i = 0; i < buddies.length; i++) {
				BuddyPluginBuddy pluginBuddy = buddies[i];
				if (pluginBuddy.isOnline()) {
					pluginBuddy.sendMessage(BuddyPlugin.SUBSYSTEM_AZ3, map,
							SEND_P2P_TIMEOUT, new BuddyPluginBuddyReplyListener() {

								public void sendFailed(BuddyPluginBuddy to_buddy,
										BuddyPluginException cause) {
									VuzeBuddyManager.log("SEND FAILED " + to_buddy.getPublicKey()
											+ "\n" + cause);
									sendViaRelayServer(to_buddy, map);
								}

								public void replyReceived(BuddyPluginBuddy from_buddy, Map reply) {
									VuzeBuddyManager.log("REPLY REC "
											+ JSONUtils.encodeToJSON(reply));
									String response = MapUtils.getMapString(reply, "response", "");
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
		} catch (BuddyPluginException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
