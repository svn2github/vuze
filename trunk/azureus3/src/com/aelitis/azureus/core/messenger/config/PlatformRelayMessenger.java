/**
 * Created on Apr 17, 2008
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

import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.buddy.VuzeBuddy;
import com.aelitis.azureus.buddy.impl.VuzeBuddyManager;
import com.aelitis.azureus.core.crypto.VuzeCryptoException;
import com.aelitis.azureus.core.crypto.VuzeCryptoManager;
import com.aelitis.azureus.core.messenger.PlatformMessage;
import com.aelitis.azureus.core.messenger.PlatformMessenger;
import com.aelitis.azureus.core.messenger.PlatformMessengerListener;
import com.aelitis.azureus.plugins.net.buddy.BuddyPlugin;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginBuddy;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginException;
import com.aelitis.azureus.plugins.net.buddy.BuddyPlugin.cryptoResult;
import com.aelitis.azureus.util.MapUtils;

/**
 * @author TuxPaper
 * @created Apr 17, 2008
 *
 */
public class PlatformRelayMessenger
{
	public static final String LISTENER_ID = "relay";

	public static final long DEFAULT_RECHECKIN_SECS = 60 * 60 * 2; // every 2 hours

	public static String OP_FETCH = "fetch";

	public static String OP_PUT = "put";

	public static String OP_ACK = "ack";

	public static List listeners = new ArrayList();

	private static TimerEventPerformer relayCheckPerformer;

	private static TimerEvent timerEvent;

	public static final void put(String[] pks, byte[] payload, long maxDelayMS) {
		String myPK;
		try {
			myPK = VuzeCryptoManager.getSingleton().getPublicKey(null);
		} catch (VuzeCryptoException e) {
			Debug.out(e);
			return;
		}

		BuddyPlugin buddyPlugin = VuzeBuddyManager.getBuddyPlugin();
		if (buddyPlugin == null) {
			return;
		}

		for (int i = 0; i < pks.length; i++) {
			try {
				final String pk = pks[i];

				BuddyPluginBuddy pluginBuddy = buddyPlugin.getBuddyFromPublicKey(pk);

				if (pluginBuddy == null) {
					System.err.println("uhoh, should create a temp one?");
					continue;
				}

				cryptoResult encryptResult = pluginBuddy.encrypt(payload);

				Map mapParameters = new HashMap();
				mapParameters.put("sender_pk", myPK);
				mapParameters.put("recipient_pk", pk);
				mapParameters.put("payload", Base32.encode(encryptResult.getPayload()));
				mapParameters.put("ack_hash",
						Base32.encode(encryptResult.getChallenge()));

				PlatformMessage message = new PlatformMessage("AZMSG", LISTENER_ID,
						OP_PUT, mapParameters, maxDelayMS);

				PlatformMessengerListener listener = new PlatformMessengerListener() {

					public void messageSent(PlatformMessage message) {
					}

					public void replyReceived(PlatformMessage message, String replyType,
							Map reply) {
						String replyMessage = MapUtils.getMapString(reply, "message", null);

						if (replyMessage != null && replyMessage.equals("Ok")) {
							// good
							PlatformMessenger.debug("Relay: Ok to " + pk);
						} else {
							// bad
							PlatformMessenger.debug("Relay: FAILED for " + pk);
						}

					}

				};

				PlatformMessenger.queueMessage(message, listener);
			} catch (BuddyPluginException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static final void fetch(long maxDelayMS) {
		String myPK;
		try {
			myPK = VuzeCryptoManager.getSingleton().getPublicKey(null);
		} catch (VuzeCryptoException e) {
			Debug.out(e);
			return;
		}

		PlatformMessage message = new PlatformMessage("AZMSG", LISTENER_ID,
				OP_FETCH, new Object[] {
					"pk",
					myPK
				}, maxDelayMS);

		final BuddyPlugin buddyPlugin = VuzeBuddyManager.getBuddyPlugin();
		if (buddyPlugin == null) {
			return;
		}

		PlatformMessengerListener listener = new PlatformMessengerListener() {

			public void messageSent(PlatformMessage message) {
			}

			public void replyReceived(PlatformMessage message, String replyType,
					Map reply) {
				List list = (List) MapUtils.getMapObject(reply, "messages",
						Collections.EMPTY_LIST, List.class);
				long recheckIn = MapUtils.getMapLong(reply, "recheck-in-mins",
						DEFAULT_RECHECKIN_SECS) * 1000l * 60;

				if (relayCheckPerformer == null) {
					relayCheckPerformer = new TimerEventPerformer() {
						public void perform(TimerEvent event) {
							PlatformRelayMessenger.fetch(1000);
						}
					};
				}
				if (timerEvent != null) {
					timerEvent.cancel();
				}
				timerEvent = SimpleTimer.addEvent("Relay Server Check", recheckIn,
						relayCheckPerformer);

				for (Iterator iter = list.iterator(); iter.hasNext();) {
					Map map = (Map) iter.next();

					String sender = MapUtils.getMapString(map, "sender", null);

					BuddyPluginBuddy pluginBuddy = buddyPlugin.getBuddyFromPublicKey(sender);
					VuzeBuddy buddy = VuzeBuddyManager.getBuddyByPK(sender);
					if (pluginBuddy == null) {
						PlatformMessenger.debug("Relay: fetch: could not find sender");
					} else {
						long ack_id = MapUtils.getMapLong(map, "id", -1);
						byte[] enc_payload = Base32.decode(MapUtils.getMapString(map,
								"payload", ""));

						cryptoResult decrypt;
						try {
							decrypt = pluginBuddy.decrypt(enc_payload);

							byte[] payload = decrypt.getPayload();

							PlatformMessenger.debug("Relay: got message from " + sender);

							for (Iterator iter2 = listeners.iterator(); iter2.hasNext();) {
								VuzeRelayListener l = (VuzeRelayListener) iter2.next();
								l.newRelayServerPayLoad(buddy, payload);
							}

							ack(ack_id, decrypt.getChallenge());
						} catch (BuddyPluginException e) {
							// TODO send ack_fail here
							PlatformMessenger.debug("Relay: TODO send ack_fail here");
						}
					}
					// "date" also sent, but not needed (?)
				}
			}
		};
		PlatformMessenger.queueMessage(message, listener);
	}

	public static final void addRelayServerListener(VuzeRelayListener l) {
		listeners.add(l);
	}

	private static final void ack(long id, byte[] ack) {
		String myPK;
		try {
			myPK = VuzeCryptoManager.getSingleton().getPublicKey(null);
		} catch (VuzeCryptoException e) {
			Debug.out(e);
			return;
		}

		Map mapACK = new HashMap();
		mapACK.put("id", new Long(id));
		mapACK.put("ack", Base32.encode(ack));

		Map mapParameters = new HashMap();
		mapParameters.put("recipient_pk", myPK);
		mapParameters.put("acks", new Object[] {
			mapACK
		});

		PlatformMessengerListener listener = new PlatformMessengerListener() {

			public void replyReceived(PlatformMessage message, String replyType,
					Map reply) {
				int numDeleted = MapUtils.getMapInt(reply, "deleted", 0);
				PlatformMessenger.debug("Relay: deleted " + numDeleted);
			}

			public void messageSent(PlatformMessage message) {
			}
		};

		PlatformMessage message = new PlatformMessage("AZMSG", LISTENER_ID, OP_ACK,
				mapParameters, 0);

		PlatformMessenger.queueMessage(message, listener);
	}
}
