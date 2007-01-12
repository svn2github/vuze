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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.gudy.azureus2.core3.util.Debug;
import org.json.JSONArray;
import org.json.JSONObject;

import com.aelitis.azureus.core.messenger.PlatformMessage;
import com.aelitis.azureus.core.messenger.PlatformMessenger;
import com.aelitis.azureus.core.messenger.PlatformMessengerListener;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.util.Constants;

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
	
	private static String DEFAULT_WHITELIST = "https?://"
		+ Constants.URL_ADDRESS.replaceAll("\\.", "\\\\.") + ":?[0-9]*/"
		+ Constants.URL_NAMESPACE.replaceAll("\\.", "\\\\.") + ".*";
	
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
					Object JSONReply) {
				if (JSONReply instanceof JSONArray) {
					JSONArray array = (JSONArray) JSONReply;
					Map[] reply = new HashMap[array.size()];
					for (int i = 0; i < reply.length; i++) {
						reply[i] = array.getJSONObject(i).toMap();

						String url = (String) reply[i].get("url");
						if (url != null && !url.startsWith("http://")) {
							url = Constants.URL_PREFIX + url;
							if (url.indexOf('?') < 0) {
								url += "?";
							} else {
								url += "&";
							}
							url += Constants.URL_SUFFIX;

							reply[i].put("url", url);
						}
					}
					replyListener.replyReceived(reply);
				} else {
					replyListener.replyReceived(new Map[0]);
				}
			}
		};

		PlatformMessenger.queueMessage(message, listener);
	}
	
	public static void login(long maxDelayMS) {
		Object[] params = new Object[] {
			"version",
			org.gudy.azureus2.core3.util.Constants.AZUREUS_VERSION,
			"locale",
			Locale.getDefault().toString(),
		};
		PlatformMessage message = new PlatformMessage("AZMSG", LISTENER_ID,
				"login", params, maxDelayMS);

		PlatformMessengerListener listener = new PlatformMessengerListener(){
		
			public void replyReceived(PlatformMessage message, String replyType,
					Object jsonReply) {
				if (jsonReply instanceof JSONObject) {
					JSONObject jsonObject = (JSONObject)jsonReply;
					if (jsonObject.has("url-whitelist")) {
						JSONArray array = jsonObject.getJSONArray("url-whitelist");
						String[] sNewWhiteList = new String[array.length() + 1];
						sNewWhiteList[0] = DEFAULT_WHITELIST;

						for (int i = 0; i < array.length(); i++) {
							String string = array.getString(i);
							PlatformTorrentUtils.log("v3.login: got whitelist of " + string);
							sNewWhiteList[i+1] = string;
						}
						sURLWhiteList = sNewWhiteList;
					}
				}
			}
		
			public void messageSent(PlatformMessage message) {
			}
		
		};
		
		PlatformMessenger.queueMessage(message, listener);
	}

	public static void sendUsageStats(Map stats, long timestamp, PlatformMessengerListener l) {
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
}
