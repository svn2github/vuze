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

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.activities.*;
import com.aelitis.azureus.buddy.*;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.crypto.VuzeCryptoException;
import com.aelitis.azureus.core.crypto.VuzeCryptoListener;
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

	protected static final boolean ALLOW_ONLY_AZ3 = true;

	private static final String SAVE_FILENAME = "v3.Buddies.config";

	private static BuddyPlugin buddyPlugin = null;

	private static List buddyList = new ArrayList();

	private static AEMonitor buddy_mon = new AEMonitor("buddy list/map");

	private static Map mapPKtoVuzeBuddy = new HashMap();

	private static VuzeBuddyCreator vuzeBuddyCreator;

	private static List listeners = new ArrayList();

	private static boolean skipSave = true;

	private static BuddyPluginBuddyMessageListener buddy_message_handler_listener = new BuddyPluginBuddyMessageListener() {
		private Set pending_messages = new HashSet();

		public void messageQueued(BuddyPluginBuddyMessage message) {
		}

		public void messageDeleted(BuddyPluginBuddyMessage message) {
		}

		public boolean deliverySucceeded(BuddyPluginBuddyMessage message, Map reply) {
			if (message.getSubsystem() != BuddyPlugin.SUBSYSTEM_AZ3) {

				return (true);
			}

			VuzeBuddyManager.log("REPLY REC " + JSONUtils.encodeToJSON(reply));

			String response = MapUtils.getMapString(reply, "response", "");

			if (!response.toLowerCase().equals("ok")) {

				sendViaRelayServer(message);

				// false here will re-attempt this call later (and keep message around)

				return (false);
			}

			return (true);
		}

		public void deliveryFailed(BuddyPluginBuddyMessage message,
				BuddyPluginException cause) {
			if (message.getSubsystem() != BuddyPlugin.SUBSYSTEM_AZ3) {

				return;
			}

			BuddyPluginBuddy buddy = message.getBuddy();

			VuzeBuddyManager.log("SEND FAILED " + buddy.getPublicKey() + "\n" + cause);

			sendViaRelayServer(message);
		}

		protected void sendViaRelayServer(BuddyPluginBuddyMessage message) {
			// we can get in here > once for same message in theory if relay
			// server dispatch slow and async the buddy plugin retries delivery

			synchronized (pending_messages) {

				if (pending_messages.contains(message)) {

					return;
				}

				pending_messages.add(message);
			}

			log("sendViaRelayServer to " + message.getBuddy().getPublicKey());

			PlatformRelayMessenger.put(message, 0,
					new PlatformRelayMessenger.putListener() {
						public void putOK(BuddyPluginBuddyMessage message) {
							try {

								message.delete();

							} finally {

								synchronized (pending_messages) {

									pending_messages.remove(message);
								}
							}

							log("OK: sendViaRelayServer to "
									+ message.getBuddy().getPublicKey());
						}

						public void putFailed(BuddyPluginBuddyMessage message) {
							synchronized (pending_messages) {

								pending_messages.remove(message);
							}
							log("FAIL: sendViaRelayServer to "
									+ message.getBuddy().getPublicKey());
						}
					});
		}
	};

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
			loadVuzeBuddies();

			VuzeCryptoManager.getSingleton().addListener(new VuzeCryptoListener() {
				public void sessionPasswordIncorrect() {
					VuzeBuddyManager.log("Incorrect Password!");
				}

				public char[] getSessionPassword(String reason)
						throws VuzeCryptoException {
					VuzeBuddyManager.log("Someone requested password: " + reason);
					throw new VuzeCryptoException("Not Logged In", null);
				}
			});

			LoginInfoManager.getInstance().addListener(new ILoginInfoListener() {
				public void loginUpdate(final LoginInfo info, boolean isNewLoginID) {
					if (!isNewLoginID) {
						return;
					}
					if (info.userID == null || info.userID.length() == 0) {
						// not logged in
						log("Logging out.. clearing password");
						VuzeCryptoManager.getSingleton().clearPassword();
					} else {
						// logged in

						log("Logging in.. getting pw from webapp");

						// getPassword will set the password viz VuzeCryptoManager

						PlatformKeyExchangeMessenger.getPassword(new PlatformKeyExchangeMessenger.platformPasswordListener() {
							public void passwordRetrieved() {
								// now password is set we can get access to the public key (or
								// create one if first time)

								String myPK = null;
								try {
									myPK = VuzeCryptoManager.getSingleton().getPublicKey(null);
								} catch (VuzeCryptoException e) {
								}
								if (myPK != null && !myPK.equals(info.pk)) {
									log("webapp's PK (" + info.pk
											+ ") doesn't match.  Sending out PK");
									PlatformKeyExchangeMessenger.setPublicKey();
								}

								PlatformRelayMessenger.relayCheck();
								PlatformBuddyMessenger.getInvites();
							}
						});
					}
				}
			});

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

		skipSave = false;
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
							removeBuddy(vuzeBuddy);
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
						//vuzeBuddy.setDisplayName(buddy.getName());
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

				buddy.getMessageHandler().addListener(buddy_message_handler_listener);

				if (false) {
					// XXX To remove.  We don't need to add plugin buddies anymore
					//     because we store the info ourselves in v3.Buddies.config
					String pk = buddy.getPublicKey();

					VuzeBuddy vuzeBuddy = (VuzeBuddy) mapPKtoVuzeBuddy.get(pk);
					if (vuzeBuddy != null) {
						// already exists
						return;
					}

					createNewBuddy(buddy, true);
				}
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
		boolean saveAfterAdd = false;
		buddyPlugin.addRequestListener(requestListener);
		List buddies = buddyPlugin.getBuddies();
		saveAfterAdd = buddies.size() > 0;
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

		log("processing payload " + mt);

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
		} else if (mt.equals("CheckInvites")) {
			PlatformBuddyMessenger.getInvites();
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
				log("Add new buddy '" + buddy.getDisplayName() + "' to Manager; #pk:"
						+ buddy.getPublicKeys().length);
				buddyList.add(buddy);

				if (createActivityEntry) {
					String s = "<A HREF=\"" + buddy.getProfileUrl("new-buddy-inform")
							+ "\">" + buddy.getDisplayName()
							+ "</A> has become your buddy.  Huzzah! :D";

					VuzeActivitiesEntry entry = new VuzeActivitiesEntry();
					entry.setTypeID("buddy-new", true);
					entry.setID("buddy-new-" + buddy.getLoginID());
					entry.setText(s);
					VuzeActivitiesManager.addEntries(new VuzeActivitiesEntry[] {
						entry
					});
				}
				triggerAddListener(buddy);

				saveVuzeBuddies();
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
		if (createActivityEntry) {
			removeInviteActivities(buddy.getLoginID());
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

	public static VuzeBuddy createNewBuddy(Map mapNewBuddy,
			boolean createActivityEntry) {
		VuzeBuddy newBuddy = createNewBuddyNoAdd(mapNewBuddy);

		if (newBuddy != null) {
			addBuddy(newBuddy, createActivityEntry);
		}

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
	public static VuzeBuddy createNewBuddyNoAdd(Map mapNewBuddy) {
		if (buddyPlugin == null) {
			return null;
		}

		VuzeBuddy existingBuddy = getBuddyByLoginID((String) mapNewBuddy.get("login-id"));
		if (existingBuddy != null) {
			return existingBuddy;
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

			if (!buddyList.contains(buddy)) {
				log("Buddy " + buddy.getDisplayName() + ";" + buddy.getLoginID()
						+ " already removed");
				return;
			}

			log("Removing Buddy " + buddy.getDisplayName() + ";" + buddy.getLoginID());

			buddyList.remove(buddy);

			String[] publicKeys = buddy.getPublicKeys();
			for (int i = 0; i < publicKeys.length; i++) {
				String pk = publicKeys[i];

				buddy.removePublicKey(pk);
				mapPKtoVuzeBuddy.remove(pk);
			}

			triggerRemoveListener(buddy);
		} finally {
			buddy_mon.exit();
		}

		if (buddy.getLoginID() != null) {
			PlatformBuddyMessenger.remove(buddy);
		}
	}

	/**
	 * @param updateTime
	 *
	 * @since 3.0.5.3
	 */
	public static void removeBuddiesOlderThan(long updateTime,
			boolean tellPlatform) {
		try {
			buddy_mon.enter();

			for (Iterator iter = buddyList.iterator(); iter.hasNext();) {
				VuzeBuddy buddy = (VuzeBuddy) iter.next();

				if (buddy.getLastUpdated() < updateTime) {
					log("Removing Buddy " + buddy.getDisplayName() + ";"
							+ buddy.getLoginID() + ";updateTime=" + updateTime + ";buddyTime"
							+ buddy.getLastUpdated());

					String[] publicKeys = buddy.getPublicKeys();
					for (int i = 0; i < publicKeys.length; i++) {
						String pk = publicKeys[i];

						buddy.removePublicKey(pk);
						mapPKtoVuzeBuddy.remove(pk);
					}

					iter.remove();
					triggerRemoveListener(buddy);

					if (tellPlatform && buddy.getLoginID() != null) {
						PlatformBuddyMessenger.remove(buddy);
					}
				}
			}

			saveVuzeBuddies();

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

	private static void invitePKs(String[] pks, String code) {
		if (pks != null && pks.length > 0) {
			final BuddyPluginBuddy[] pluginBuddies = new BuddyPluginBuddy[pks.length];
			for (int i = 0; i < pks.length; i++) {
				String pk = pks[i];

				pluginBuddies[i] = buddyPlugin.addBuddy(pk, BuddyPlugin.SUBSYSTEM_AZ3);
			}

			// Webapp will store invite info for User B.  We just need to tell them
			// to sync up if we can

			// Wait 10 seconds after adding a buddy as we might find out they are online
			// within that time (and be able to send the message directly)
			SimpleTimer.addEvent("send invites", SystemTime.getOffsetTime(10000),
					new TimerEventPerformer() {
						public void perform(TimerEvent event) {
							Map map = new HashMap();
							map.put("VuzeMessageType", "CheckInvites");

							sendPayloadMap(map, pluginBuddies);
						}
					});
		}
	}

	/**
	 * 
	 * @param invites This is the map that comes from the webpage after it
	 *                sends outs the invites
	 * @param dm The download you wish to share
	 * @param shareMessage The message the user typed to go with the share
	 * @param buddies The buddies that should be notified
	 *
	 * @since 3.0.5.3
	 */
	public static void inviteWithShare(Map invites, DownloadManager dm,
			String shareMessage, VuzeBuddy[] buddies) {

		if (buddies != null && dm != null) {
			log("share " + dm.toString() + " with " + buddies.length);
			for (int i = 0; i < buddies.length; i++) {
				VuzeBuddy v3Buddy = buddies[i];
				if (v3Buddy != null) {
					v3Buddy.shareDownload(dm, shareMessage);
				}
			}
		}

		String inviteMessage = MapUtils.getMapString(invites, "message", null);
		List sentInvitations = MapUtils.getMapList(invites, "sentInvitations",
				Collections.EMPTY_LIST);

		log("invite " + sentInvitations.size() + " sharing " + dm);

		for (Iterator iter = sentInvitations.iterator(); iter.hasNext();) {
			Map mapInvitation = (Map) iter.next();

			boolean success = MapUtils.getMapBoolean(mapInvitation, "success", false);
			if (success) {
				String code = MapUtils.getMapString(mapInvitation, "code", null);

				if (dm != null) {
					queueShare(dm, shareMessage, code);
				}

				List pkList = MapUtils.getMapList(mapInvitation, "pks",
						Collections.EMPTY_LIST);
				String[] newPKs = (String[]) pkList.toArray(new String[0]);

				VuzeBuddyManager.invitePKs(newPKs, code);
			}
		}
	}

	private static void queueShare(DownloadManager dm, String message, String code) {
		if (dm == null) {
			return;
		}

		VuzeActivitiesEntry entry = new VuzeActivitiesEntryContentShare(dm, message);

		QueuedVuzeShare vuzeShare = VuzeQueuedShares.add(code);
		vuzeShare.setDownloadHash(entry.getAssetHash());
		vuzeShare.setActivityEntry(entry);
	}

	public static void removeInviteActivities(String fromLoginID) {
		if (fromLoginID == null) {
			return;
		}
		List requestEntries = new ArrayList();
		VuzeActivitiesEntry[] allEntries = VuzeActivitiesManager.getAllEntries();
		for (int i = 0; i < allEntries.length; i++) {
			VuzeActivitiesEntry entry = allEntries[i];
			if (entry instanceof VuzeActivitiesEntryBuddyRequest) {
				VuzeActivitiesEntryBuddyRequest inviteEntry = (VuzeActivitiesEntryBuddyRequest) entry;
				if (fromLoginID.equals(inviteEntry.getBuddy().getLoginID())) {
					requestEntries.add(entry);
				}
			}
		}
		if (requestEntries.size() > 0) {
			VuzeActivitiesEntry[] toRemove = (VuzeActivitiesEntry[]) requestEntries.toArray(new VuzeActivitiesEntry[requestEntries.size()]);
			VuzeActivitiesManager.removeEntries(toRemove);
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
		// sync will get new buddy connection
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
			log("sending map to " + buddies.length + " buddies");
			for (int i = 0; i < buddies.length; i++) {
				BuddyPluginBuddy pluginBuddy = buddies[i];

				// outcome reported via buddy's message handler listener
				pluginBuddy.getMessageHandler().queueMessage(BuddyPlugin.SUBSYSTEM_AZ3,
						map, SEND_P2P_TIMEOUT);
			}
		} catch (BuddyPluginException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void addListener(VuzeBuddyListener l, boolean trigger) {
		listeners.add(l);
		if (trigger) {
			Object[] buddies = buddyList.toArray();
			for (int i = 0; i < buddies.length; i++) {
				VuzeBuddy buddy = (VuzeBuddy) buddies[i];
				l.buddyAdded(buddy);
			}
		}
	}

	public static void removeListener(VuzeBuddyListener l) {
		listeners.remove(l);
	}

	/**
	 * @param buddy
	 *
	 * @since 3.0.5.3
	 */
	private static void triggerRemoveListener(VuzeBuddy buddy) {
		Object[] listenersArray = listeners.toArray();
		for (int i = 0; i < listenersArray.length; i++) {
			VuzeBuddyListener l = (VuzeBuddyListener) listenersArray[i];
			l.buddyRemoved(buddy);
		}
	}

	/**
	 * @param buddy
	 *
	 * @since 3.0.5.3
	 */
	private static void triggerAddListener(VuzeBuddy buddy) {
		Object[] listenersArray = listeners.toArray();
		for (int i = 0; i < listenersArray.length; i++) {
			VuzeBuddyListener l = (VuzeBuddyListener) listenersArray[i];
			l.buddyAdded(buddy);
		}
	}

	/**
	 * @param buddy
	 *
	 * @since 3.0.5.3
	 */
	protected static void triggerChangeListener(VuzeBuddy buddy) {
		saveVuzeBuddies();
		Object[] listenersArray = listeners.toArray();
		for (int i = 0; i < listenersArray.length; i++) {
			VuzeBuddyListener l = (VuzeBuddyListener) listenersArray[i];
			l.buddyChanged(buddy);
		}
	}

	private static void saveVuzeBuddies() {
		if (skipSave) {
			return;
		}

		log("save");
		Map mapSave = new HashMap();
		List storedBuddyList = new ArrayList();
		mapSave.put("buddies", storedBuddyList);

		try {
			buddy_mon.enter();

			for (Iterator iter = buddyList.iterator(); iter.hasNext();) {
				VuzeBuddy buddy = (VuzeBuddy) iter.next();

				if (buddy != null) {
					storedBuddyList.add(buddy.toMap());
				}
			}

			FileUtil.writeResilientConfigFile(SAVE_FILENAME, mapSave);
		} finally {
			buddy_mon.exit();
		}
	}

	private static void loadVuzeBuddies() {
		Map map = BDecoder.decodeStrings(FileUtil.readResilientConfigFile(SAVE_FILENAME));

		List storedBuddyList = MapUtils.getMapList(map, "buddies",
				Collections.EMPTY_LIST);

		for (Iterator iter = storedBuddyList.iterator(); iter.hasNext();) {
			Map mapBuddy = (Map) iter.next();

			createNewBuddy(mapBuddy, false);
		}
	}
}
