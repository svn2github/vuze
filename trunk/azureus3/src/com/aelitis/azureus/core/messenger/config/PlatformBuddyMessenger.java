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

import com.aelitis.azureus.buddy.VuzeBuddy;
import com.aelitis.azureus.buddy.impl.VuzeBuddyManager;
import com.aelitis.azureus.core.messenger.PlatformMessage;
import com.aelitis.azureus.core.messenger.PlatformMessenger;
import com.aelitis.azureus.core.messenger.PlatformMessengerListener;
import com.aelitis.azureus.util.MapUtils;

/**
 * @author TuxPaper
 * @created Apr 18, 2008
 *
 */
public class PlatformBuddyMessenger
{
	public static final String LISTENER_ID = "buddy";

	public static String OP_SYNC = "sync";

	public static void sync(final VuzeBuddySyncListener l) {
		PlatformMessage message = new PlatformMessage("AZMSG", LISTENER_ID,
				OP_SYNC, new Object[0], 1000);

		PlatformMessengerListener listener = new PlatformMessengerListener() {

			public void messageSent(PlatformMessage message) {
			}

			public void replyReceived(PlatformMessage message, String replyType,
					Map reply) {
				long updateTime = SystemTime.getCurrentTime();

				List mapBuddies = (List) MapUtils.getMapObject(reply, "buddies",
						Collections.EMPTY_LIST, List.class);
				
				if (mapBuddies.size() == 0) {
					return;
				}

				for (Iterator iter = mapBuddies.iterator(); iter.hasNext();) {
					Map mapBuddy = (Map) iter.next();

					String loginID = MapUtils.getMapString(mapBuddy, "login-id",
							null);

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
				
				VuzeBuddyManager.removeBuddiesOlderThan(updateTime);

				if (l != null) {
					l.syncComplete();
				}
			}
		};
		
		message.setRequiresAuthorization(true);

		PlatformMessenger.queueMessage(message, listener);
	}
}
