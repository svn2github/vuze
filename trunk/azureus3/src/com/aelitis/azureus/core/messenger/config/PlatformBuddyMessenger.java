/**
 * Created on Apr 18, 2008
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

package com.aelitis.azureus.core.messenger.config;

import java.util.*;

import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.activities.VuzeActivitiesEntryBuddyRequest;
import com.aelitis.azureus.activities.VuzeActivitiesManager;
import com.aelitis.azureus.buddy.VuzeBuddy;
import com.aelitis.azureus.buddy.impl.VuzeBuddyManager;
import com.aelitis.azureus.core.messenger.PlatformMessage;
import com.aelitis.azureus.core.messenger.PlatformMessenger;
import com.aelitis.azureus.core.messenger.PlatformMessengerListener;
import com.aelitis.azureus.login.NotLoggedInException;
import com.aelitis.azureus.util.MapUtils;

/**
 * @author TuxPaper
 * @created Apr 18, 2008
 *
 * TODO: poll invites occasionally
 */
public class PlatformBuddyMessenger
{
	public static final String LISTENER_ID_BUDDY = "buddy";

	public static final String LISTENER_ID_INVITE = "invite";

	public static final String OP_SYNC = "sync";

	public static final String OP_GETINVITES = "fetch";

	public static final String OP_COUNTINVITES = "count";

	public static final String OP_INVITE = "invite";

	public static final String OP_REMOVEBUDDY = "ditch";

	public static void sync(
			final VuzeBuddySyncListener l)
		throws NotLoggedInException {

		PlatformMessage message = new PlatformMessage("AZMSG", LISTENER_ID_BUDDY,
				OP_SYNC, new Object[0], 1000);
		message.setRequiresAuthorization(true, false);

		PlatformMessengerListener listener = new PlatformMessengerListener() {

			public void messageSent(
					PlatformMessage message) {
			}

			public void replyReceived(
					PlatformMessage message,
					String replyType,
					Map reply) {
				if (!replyType.equals(PlatformMessenger.REPLY_RESULT)) {
					return;
				}

				long updateTime = SystemTime.getCurrentTime();

				List buddies = MapUtils.getMapList(reply, "buddies", null);

				if (buddies == null) {
					return;
				}

				for (Iterator iter = buddies.iterator(); iter.hasNext();) {
					Map mapBuddy = (Map) iter.next();

					String loginID = MapUtils.getMapString(mapBuddy, "login-id", null);

					VuzeBuddy buddy = VuzeBuddyManager.getBuddyByLoginID(loginID);
					if (buddy != null) {
						buddy.loadFromMap(mapBuddy);
					} else {
						buddy = VuzeBuddyManager.createNewBuddy(mapBuddy, true);
					}

					if (buddy != null) {
						buddy.setLastUpdated(updateTime);
					}
				}

				VuzeBuddyManager.removeBuddiesOlderThan(updateTime, false);

				if (l != null) {
					l.syncComplete();
				}
			}
		};

		PlatformMessenger.queueMessage(message, listener);
	}

	/**
	 * 
	 *
	 * @throws NotLoggedInException 
	 * @since 3.0.5.3
	 */
	public static void getInvites()
		throws NotLoggedInException {

		PlatformMessage message = new PlatformMessage("AZMSG", LISTENER_ID_INVITE,
				OP_GETINVITES, new Object[0], 1000);
		message.setRequiresAuthorization(true, false);

		PlatformMessengerListener listener = new PlatformMessengerListener() {

			public void replyReceived(
					PlatformMessage message,
					String replyType,
					Map reply) {
				if (!replyType.equals(PlatformMessenger.REPLY_RESULT)) {
					return;
				}

				List invitations = MapUtils.getMapList(reply, "invitations",
						Collections.EMPTY_LIST);
				
				VuzeActivitiesEntry[] allEntries = VuzeActivitiesManager.getAllEntries();
				List existingInvites = new ArrayList();
				for (int i = 0; i < allEntries.length; i++) {
					VuzeActivitiesEntry entry = allEntries[i];
					if (entry instanceof VuzeActivitiesEntryBuddyRequest) {
						VuzeActivitiesEntryBuddyRequest inviteEntry = (VuzeActivitiesEntryBuddyRequest) entry;
						if (inviteEntry.getBuddy() != null) {
							existingInvites.add(entry);
						}
					}
				}

				if (invitations.size() == 0) {
					VuzeActivitiesEntry[] entries = (VuzeActivitiesEntry[]) existingInvites.toArray(new VuzeActivitiesEntry[0]);
					VuzeActivitiesManager.removeEntries(entries);
					return;
				}

				for (Iterator iter = invitations.iterator(); iter.hasNext();) {
					Map mapInvitation = (Map) iter.next();

					Map mapBuddy = MapUtils.getMapMap(mapInvitation, "buddy-info",
							Collections.EMPTY_MAP);

					String inviteCode = MapUtils.getMapString(mapInvitation, "code", null);
					String acceptURL = MapUtils.getMapString(mapInvitation, "accept-url",
							null);

					if (mapBuddy.isEmpty() || inviteCode == null || acceptURL == null) {
						continue;
					}
					
					for (Iterator iter2 = existingInvites.iterator(); iter2.hasNext();) {
						VuzeActivitiesEntryBuddyRequest entry = (VuzeActivitiesEntryBuddyRequest) iter2.next();
						if (inviteCode.equals(entry.getBuddy().getCode())) {
							iter2.remove();
						}
					}

					VuzeBuddy futureBuddy = VuzeBuddyManager.createPotentialBuddy();
					futureBuddy.loadFromMap(mapBuddy);
					futureBuddy.setCode(inviteCode);

					VuzeActivitiesEntryBuddyRequest entry = new VuzeActivitiesEntryBuddyRequest(
							futureBuddy, acceptURL);
					VuzeActivitiesManager.addEntries(new VuzeActivitiesEntry[] {
						entry
					});
				}

				VuzeActivitiesEntry[] entries = (VuzeActivitiesEntry[]) existingInvites.toArray(new VuzeActivitiesEntry[existingInvites.size()]);
				VuzeActivitiesManager.removeEntries(entries);
			}

			public void messageSent(
					PlatformMessage message) {
			}
		};

		PlatformMessenger.queueMessage(message, listener);
	}

	public static void getNumPendingInvites() {
		PlatformMessage message = new PlatformMessage("AZMSG", LISTENER_ID_INVITE,
				OP_COUNTINVITES, new Object[0], 1000);

		PlatformMessengerListener listener = new PlatformMessengerListener() {

			public void replyReceived(
					PlatformMessage message,
					String replyType,
					Map reply) {

				int count = MapUtils.getMapInt(reply, "count", 0);

				// TODO fire off listener
			}

			public void messageSent(
					PlatformMessage message) {
			}
		};

		PlatformMessenger.queueMessage(message, listener);
	}

	public static void invite(
			String loginID,
			String userMessage)
		throws NotLoggedInException {

		Map parameters = new HashMap();
		parameters.put("message", userMessage);

		List invitations = new ArrayList();
		Map invitation = new HashMap();
		parameters.put("invitations", invitations);
		invitation.put("type", "username");
		invitation.put("value", loginID);
		invitations.add(invitation);

		PlatformMessage message = new PlatformMessage("AZMSG", LISTENER_ID_INVITE,
				OP_INVITE, parameters, 1000);
		message.setRequiresAuthorization(true, false);

		PlatformMessengerListener listener = new PlatformMessengerListener() {

			public void replyReceived(
					PlatformMessage message,
					String replyType,
					Map reply) {
			}

			public void messageSent(
					PlatformMessage message) {
			}
		};

		PlatformMessenger.queueMessage(message, listener);
	}

	/**
	 * @param buddy
	 * @param login 
	 * @throws NotLoggedInException 
	 *
	 * @since 3.0.5.3
	 */
	public static void remove(
			final VuzeBuddy buddy,
			boolean login)
		throws NotLoggedInException {
		PlatformMessage message = new PlatformMessage("AZMSG", LISTENER_ID_BUDDY,
				OP_REMOVEBUDDY, new Object[] {
					"username",
					buddy.getLoginID()
				}, 1000);
		message.setRequiresAuthorization(true, login);

		PlatformMessengerListener listener = new PlatformMessengerListener() {

			public void replyReceived(
					PlatformMessage message,
					String replyType,
					Map reply) {
				VuzeBuddyManager.log("removal of " + buddy.getLoginID()
						+ " from webapp: " + replyType);
			}

			public void messageSent(
					PlatformMessage message) {
			}
		};

		PlatformMessenger.queueMessage(message, listener);
	}
}
