/**
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.messenger.config;

import java.util.*;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.platform.PlatformManager;
import org.gudy.azureus2.platform.PlatformManagerFactory;

import com.aelitis.azureus.core.messenger.PlatformMessage;
import com.aelitis.azureus.core.messenger.PlatformMessenger;
import com.aelitis.azureus.core.messenger.PlatformMessengerListener;
import com.aelitis.azureus.util.Constants;
import com.aelitis.azureus.util.MapUtils;

import org.gudy.azureus2.plugins.platform.PlatformManagerException;

/**
 * @author TuxPaper
 * @created Sep 26, 2006
 *
 */
public class PlatformConfigMessenger
{
	public static final String LISTENER_ID = "config";

	public static final String SECTION_TYPE_BIGBROWSE = "browse";

	public static final String SECTION_TYPE_MINIBROWSE = "minibrowse";

	private static int iRPCVersion = 0;

	private static String DEFAULT_WHITELIST = "https?://"
			+ Constants.URL_ADDRESS.replaceAll("\\.", "\\\\.") + ":?[0-9]*/" + ".*";

	private static String[] sURLWhiteList = new String[] {
		DEFAULT_WHITELIST
	};

	public static void getBrowseSections(String sectionType, long maxDelayMS,
			final GetBrowseSectionsReplyListener replyListener) {

		PlatformMessage message = new PlatformMessage("AZMSG", LISTENER_ID,
				"get-browse-sections", new Object[] {
					"section-type",
					sectionType,
					"locale",
					Locale.getDefault().toString()
				}, maxDelayMS);

		PlatformMessengerListener listener = new PlatformMessengerListener() {
			public void messageSent(PlatformMessage message) {
				replyListener.messageSent();
			}

			public void replyReceived(PlatformMessage message, String replyType,
					Map reply) {
				if (reply != null) {
					List array = (List) reply.get("value");
					Map[] newReply = new HashMap[array.size()];
					for (int i = 0; i < newReply.length; i++) {
						newReply[i] = (Map) array.get(i);

						String url = (String) newReply[i].get("url");
						if (url != null && !url.startsWith("http://")) {
							url = Constants.URL_PREFIX + url;
							if (url.indexOf('?') < 0) {
								url += "?";
							} else {
								url += "&";
							}
							url += Constants.URL_SUFFIX;

							newReply[i].put("url", url);
						}
					}
					replyListener.replyReceived(newReply);
				} else {
					replyListener.replyReceived(new Map[0]);
				}
			}
		};

		PlatformMessenger.queueMessage(message, listener);
	}

	public static void login(long maxDelayMS) {
		PlatformManager pm = PlatformManagerFactory.getPlatformManager();
		String azComputerID = "";
		try {
			azComputerID = pm.getAzComputerID();
		} catch (PlatformManagerException e) {
		}

		Object[] params = new Object[] {
			"version",
			org.gudy.azureus2.core3.util.Constants.AZUREUS_VERSION,
			"locale",
			Locale.getDefault().toString(),
			"azCID",
			azComputerID
		};
		PlatformMessage message = new PlatformMessage("AZMSG", LISTENER_ID,
				"login", params, maxDelayMS);

		PlatformMessengerListener listener = new PlatformMessengerListener() {

			public void replyReceived(PlatformMessage message, String replyType,
					Map reply) {
				if (reply == null) {
					return;
				}

				try {
					List listURLs = (List) MapUtils.getMapObject(reply, "url-whitelist",
							null, List.class);
					if (listURLs != null) {
						String[] sNewWhiteList = new String[listURLs.size() + 1];
						sNewWhiteList[0] = DEFAULT_WHITELIST;

						for (int i = 0; i < listURLs.size(); i++) {
							String string = (String) listURLs.get(i);
							PlatformMessenger.debug("v3.login: got whitelist of " + string);
							sNewWhiteList[i + 1] = string;
						}
						sURLWhiteList = sNewWhiteList;
					}
				} catch (Exception e) {
					Debug.out(e);
				}

				iRPCVersion = MapUtils.getMapInt(reply, "rpc-version", 0);
			}

			public void messageSent(PlatformMessage message) {
			}

		};

		PlatformMessenger.queueMessage(message, listener);
	}

	public static void sendUsageStats(Map stats, long timestamp,
			PlatformMessengerListener l) {
		try {
			PlatformMessage message = new PlatformMessage("AZMSG", LISTENER_ID,
					"send-usage-stats", new Object[] {
						"stats",
						stats,
						"version",
						org.gudy.azureus2.core3.util.Constants.AZUREUS_VERSION,
						"timestamp",
						new Long(timestamp)
					}, 5000);

			PlatformMessenger.queueMessage(message, l);
		} catch (Exception e) {
			Debug.out(e);
		}
	}

	public static interface GetBrowseSectionsReplyListener
	{
		public void messageSent();

		public void replyReceived(Map[] browseSections);
	}

	public static String[] getURLWhitelist() {
		return sURLWhiteList;
	}

	public static boolean isURLBlocked(String url) {
		if (url == null) {
			Debug.out("URL null and should be blocked");
			return true;
		}

		String[] whitelist = PlatformConfigMessenger.getURLWhitelist();
		for (int i = 0; i < whitelist.length; i++) {
			if (url.matches(whitelist[i])) {
				return false;
			}
		}
		Debug.out("URL '" + url + "' " + " does not match one of the "
				+ whitelist.length + " whitelist entries");
		return true;
	}

	/**
	 * @return the iRPCVersion
	 */
	public static int getRPCVersion() {
		return iRPCVersion;
	}
}
