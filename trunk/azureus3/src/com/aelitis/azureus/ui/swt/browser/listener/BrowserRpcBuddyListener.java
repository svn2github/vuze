/**
 * Created on Apr 21, 2008
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
 
package com.aelitis.azureus.ui.swt.browser.listener;

import java.util.*;

import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.buddy.impl.VuzeBuddyManager;
import com.aelitis.azureus.core.messenger.browser.BrowserMessage;
import com.aelitis.azureus.core.messenger.browser.listeners.AbstractBrowserMessageListener;
import com.aelitis.azureus.core.messenger.config.PlatformBuddyMessenger;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.util.Constants;
import com.aelitis.azureus.util.MapUtils;

/**
 * @author TuxPaper
 * @created Apr 21, 2008
 *
 */
public class BrowserRpcBuddyListener
extends AbstractBrowserMessageListener
{
	private static final String DEFAULT_LISTENER_ID = "buddy";

	public static final String OP_ACCEPT = "accept";

	public static final String OP_INVITE = "invite";

	public static final String OP_SYNC = "sync";

	public BrowserRpcBuddyListener() {
		super(DEFAULT_LISTENER_ID);
	}

	// @see com.aelitis.azureus.ui.swt.browser.msg.AbstractMessageListener#handleMessage(com.aelitis.azureus.ui.swt.browser.msg.BrowserMessage)
	public void handleMessage(BrowserMessage message) {
		try {
			String opid = message.getOperationId();

			Map decodedMap = message.getDecodedMap();

			if (OP_ACCEPT.equals(opid)) {
				Map mapBuddy = MapUtils.getMapMap(decodedMap, "buddy", Collections.EMPTY_MAP);
				String code = MapUtils.getMapString(decodedMap, "code", null);
				VuzeBuddyManager.log("buddy accept " + mapBuddy.get("login-id") + "/" + code);
				
				if (mapBuddy.size() > 0) {
					List pkList = MapUtils.getMapList(mapBuddy, "pks",
							Collections.EMPTY_LIST);
					String[] pks = (String[]) pkList.toArray(new String[0]);

					VuzeBuddyManager.acceptInvite(code, pks);
				}
			} else if (OP_INVITE.equals(opid)) {
				VuzeBuddyManager.inviteWithShare(decodedMap, null, null, null);
			} else if (OP_SYNC.equals(opid)) {
				long last = MapUtils.getMapLong(decodedMap, "min-time-secs", 0);
				if (SystemTime.getCurrentTime()
						- PlatformBuddyMessenger.getLastSyncCheck() > last * 1000) {
					PlatformBuddyMessenger.sync(null);
				}
			}
		} catch (Throwable t) {
			message.debug("handle Config message", t);
		}
	}
}
