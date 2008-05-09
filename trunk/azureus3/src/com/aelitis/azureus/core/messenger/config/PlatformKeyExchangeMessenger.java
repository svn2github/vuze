/**
 * Created on Apr 24, 2008
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

import java.util.Map;

import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.buddy.impl.VuzeBuddyManager;
import com.aelitis.azureus.core.crypto.VuzeCryptoException;
import com.aelitis.azureus.core.crypto.VuzeCryptoManager;
import com.aelitis.azureus.core.messenger.PlatformMessage;
import com.aelitis.azureus.core.messenger.PlatformMessenger;
import com.aelitis.azureus.core.messenger.PlatformMessengerListener;
import com.aelitis.azureus.login.NotLoggedInException;
import com.aelitis.azureus.util.Constants;
import com.aelitis.azureus.util.MapUtils;

/**
 * @author TuxPaper
 * @created Apr 24, 2008
 *
 */
public class PlatformKeyExchangeMessenger
{
	public static final String LISTENER_ID = "exchange";

	public static final String PREFIX = "key";

	public static String OP_GETPASSWORD = "getPassword";

	public static String OP_SETPUBLICKEY = "setPublicKey";

	public static void getPassword(
			final platformPasswordListener l)
		throws NotLoggedInException {
		PlatformMessage message = new PlatformMessage(PREFIX, LISTENER_ID,
				OP_GETPASSWORD, new Object[0], 1000);
		message.setRequiresAuthorization(true, false);

		PlatformMessengerListener listener = new PlatformMessengerListener() {

			public void replyReceived(
					PlatformMessage message,
					String replyType,
					Map reply) {
				String pw = MapUtils.getMapString(reply, "password", null);
				if (pw != null && pw.length() > 0) {
					// for session
					VuzeBuddyManager.log("Got PW from webapp");
					VuzeCryptoManager.getSingleton().setPassword(pw);
					if (l != null) {
						l.passwordRetrieved();
					}
				}

			}

			public void messageSent(
					PlatformMessage message) {
			}
		};

		PlatformMessenger.queueMessage(message, listener);
	}

	public static void setPublicKey()
		throws NotLoggedInException {
		final String myPK;
		try {
			myPK = VuzeCryptoManager.getSingleton().getPublicKey(null);
		} catch (VuzeCryptoException e) {
			Debug.out(e);
			return;
		}

		PlatformMessage message = new PlatformMessage(PREFIX, LISTENER_ID,
				OP_SETPUBLICKEY, new Object[] {
					"azid",
					Constants.AZID,
					"publicKey",
					myPK
				}, 1000);
		message.setRequiresAuthorization(true, false);

		PlatformMessengerListener listener = new PlatformMessengerListener() {

			public void replyReceived(
					PlatformMessage message,
					String replyType,
					Map reply) {
			}

			public void messageSent(
					PlatformMessage message) {
				Map parameters = message.getParameters();

				String sMessage = MapUtils.getMapString(parameters, "message", "");
				if (sMessage.toLowerCase().equals("ok")) {
					// do something here?
				}
			}
		};
		PlatformMessenger.queueMessage(message, listener);
	}

	public static interface platformPasswordListener
	{
		public void passwordRetrieved();
	}
}
