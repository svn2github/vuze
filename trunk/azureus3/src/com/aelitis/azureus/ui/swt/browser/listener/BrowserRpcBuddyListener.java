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

import com.aelitis.azureus.buddy.impl.VuzeBuddyManager;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.browser.msg.AbstractMessageListener;
import com.aelitis.azureus.ui.swt.browser.msg.BrowserMessage;
import com.aelitis.azureus.util.Constants;
import com.aelitis.azureus.util.MapUtils;

/**
 * @author TuxPaper
 * @created Apr 21, 2008
 *
 */
public class BrowserRpcBuddyListener
extends AbstractMessageListener
{
	private static final String DEFAULT_LISTENER_ID = "buddy";

	public static final String OP_ACCEPT = "accept";

	public static final String OP_INVITE = "invite";

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
				String code = MapUtils.getMapString(mapBuddy, "code", null);
				VuzeBuddyManager.log("buddy accept " + mapBuddy.get("login-id") + "/" + code);
				
				// XXX Cheat..
				UIFunctionsSWT uif = UIFunctionsManagerSWT.getUIFunctionsSWT();
				String url;
				url = Constants.URL_PREFIX + "invitation/" + code;
				uif.viewURL(url, "browse", 0, 0, false, false);
				

				if (mapBuddy.size() > 0) {
					List pkList = MapUtils.getMapList(mapBuddy, "pks",
							Collections.EMPTY_LIST);
					String[] pks = (String[]) pkList.toArray(new String[0]);

					VuzeBuddyManager.acceptInvite(code, pks);
				}
			} else if (OP_INVITE.equals(opid)) {
				VuzeBuddyManager.inviteWithShare(decodedMap, null, null, null);
			}
		} catch (Throwable t) {
			message.debug("handle Config message", t);
		}
	}
}
