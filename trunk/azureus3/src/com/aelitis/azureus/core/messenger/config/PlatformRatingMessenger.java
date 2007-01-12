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
import java.util.Map;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import org.json.JSONArray;
import org.json.JSONObject;

import com.aelitis.azureus.core.messenger.PlatformMessage;
import com.aelitis.azureus.core.messenger.PlatformMessenger;
import com.aelitis.azureus.core.messenger.PlatformMessengerListener;

/**
 * @author TuxPaper
 * @created Oct 4, 2006
 *
 */
public class PlatformRatingMessenger
{
	public static final String LISTENER_ID = "rating";

	public static final String OP_GET = "get";

	public static final String OP_SET = "set";

	public static final String RATE_TYPE_CONTENT = "content";

	public static void getUserRating(String[] rateTypes, String[] torrentHashes,
			long maxDelayMS, final GetRatingReplyListener replyListener) {

		PlatformMessage message = new PlatformMessage("AZMSG", "rating",
				"get-user", new Object[] {
					"rating-type",
					rateTypes,
					"torrent-hash",
					torrentHashes,
				}, maxDelayMS);

		PlatformMessengerListener listener = new PlatformMessengerListener() {
			public void messageSent(PlatformMessage message) {
				replyListener.messageSent();
			}

			public void replyReceived(PlatformMessage message, String replyType,
					Object JSONReply) {
				if (JSONReply instanceof JSONObject) {
					Map reply = ((JSONObject) JSONReply).toMap();
					GetRatingReply ratingReply = new GetRatingReply(reply);

					replyListener.replyReceived(replyType, ratingReply);
				} else {
					replyListener.replyReceived(replyType, new GetRatingReply(null));
				}
			}
		};

		PlatformMessenger.queueMessage(message, listener);
	}

	public static void getGlobalRating(String[] rateTypes,
			String[] torrentHashes, long maxDelayMS,
			final GetRatingReplyListener replyListener) {

		PlatformMessage message = new PlatformMessage("AZMSG", "rating",
				"get-global", new Object[] {
					"rating-type",
					rateTypes,
					"torrent-hash",
					torrentHashes,
				}, maxDelayMS);

		PlatformMessengerListener listener = new PlatformMessengerListener() {
			public void messageSent(PlatformMessage message) {
				replyListener.messageSent();
			}

			public void replyReceived(PlatformMessage message, String replyType,
					Object JSONReply) {
				if (JSONReply instanceof JSONObject) {
					Map reply = ((JSONObject) JSONReply).toMap();
					GetRatingReply ratingReply = new GetRatingReply(reply);

					replyListener.replyReceived(replyType, ratingReply);
				} else {
					replyListener.replyReceived(replyType, new GetRatingReply(null));
				}
			}
		};

		PlatformMessenger.queueMessage(message, listener);
	}

	public static void setUserRating(String torrentHash, int rating,
			long maxDelayMS, final PlatformMessengerListener l) {

		JSONArray array = new JSONArray();
		array.put(PlatformMessage.parseParams(new Object[] {
			"rating-type",
			"content",
			"rating-value",
			new Integer(rating)
		}));

		PlatformMessage message = new PlatformMessage("AZMSG", "rating", "set",
				new Object[] {
					"torrent-hash",
					torrentHash,
					"ratings",
					array
				}, maxDelayMS);

		PlatformMessenger.queueMessage(message, new PlatformMessengerListener() {
			// @see com.aelitis.azureus.core.messenger.PlatformMessengerListener#messageSent(com.aelitis.azureus.core.messenger.PlatformMessage)

			public void messageSent(PlatformMessage message) {
				System.out.println(SystemTime.getCurrentTime() + ": messageSent"
						+ message);
				if (l != null) {
					l.messageSent(message);
				}
			}

			// @see com.aelitis.azureus.core.messenger.PlatformMessengerListener#replyReceived(com.aelitis.azureus.core.messenger.PlatformMessage, java.lang.String, java.lang.Object)

			public void replyReceived(PlatformMessage message, String replyType,
					Object jsonReply) {
				System.out.println(SystemTime.getCurrentTime() + ": replyRecieved "
						+ message + ";" + replyType + ";" + jsonReply);
				if (l != null) {
					l.replyReceived(message, replyType, jsonReply);
				}
			}
		});
	}

	public static boolean ratingSucceeded(Object jsonReply) {
		if (jsonReply instanceof JSONObject) {
			JSONObject jsonObject = (JSONObject) jsonReply;
			if (jsonObject.has("message")) {
				return jsonObject.getString("message").equals("Ok");
			} else if (jsonObject.has("success")) {
				return jsonObject.getBoolean("success");
			}
		}

		return false;
	}

	public static abstract class GetRatingReplyListener
	{
		public abstract void messageSent();

		/**
		 * Map: 
		 * Key = Torrent Hash
		 * Value = Map
		 * 
		 *   Map 2:
		 *   Key = Rating Type
		 *   Value = Map
		 *   
		 *     Map 3:
		 *     Keys = average, count, expires-in-mins
		 * @param reply
		 */
		public abstract void replyReceived(String replyType, GetRatingReply reply);
	}

	public static class GetRatingReply
	{
		private final Map reply;

		/**
		 * 
		 */
		public GetRatingReply(Map message) {
			reply = message == null ? new HashMap() : message;
		}

		public long getRatingValue(String hash, String type) {
			int rating = -1;

			JSONObject mapRating = (JSONObject) reply.get(hash);
			if (mapRating != null) {
				JSONObject mapValues = (JSONObject) mapRating.get(RATE_TYPE_CONTENT);
				if (mapValues != null) {
					if (mapValues.has("value")) {
						Object val = mapValues.get("value");
						if (val instanceof Double) {
							rating = ((Double) val).intValue();
						}
					}
				}
			}

			return rating;
		}

		public long getRatingCount(String hash, String type) {
			long rating = -1;
			try {
				JSONObject mapRating = (JSONObject) reply.get(hash);
				if (mapRating != null) {
					JSONObject mapValues = (JSONObject) mapRating.get(RATE_TYPE_CONTENT);
					if (mapValues != null) {
						if (mapValues.has("count")) {
							Object val = mapValues.get("count");
							if (val instanceof Number) {
								rating = ((Number) val).longValue();
							}
						}
					}
				}
			} catch (Exception e) {
				Debug.out(e);
			}

			return rating;
		}

		public String getRatingString(String hash, String type) {
			String rating = "--";

			try {
				JSONObject mapRating = (JSONObject) reply.get(hash);
				if (mapRating != null) {
					JSONObject mapValues = (JSONObject) mapRating.get(RATE_TYPE_CONTENT);
					if (mapValues != null) {
						if (mapValues.has("value")) {
							Object val = mapValues.get("value");
							if (val instanceof String) {
								rating = (String) val;
							} else if (val instanceof Double) {
								rating = ((Double) val).toString();
							}
						}
					}
				}
			} catch (Exception e) {
				Debug.out(e);
			}

			return rating;
		}

		public String getRatingColor(String hash, String type) {
			String color = null;

			try {
				JSONObject mapRating = (JSONObject) reply.get(hash);
				if (mapRating != null) {
					JSONObject mapValues = (JSONObject) mapRating.get(RATE_TYPE_CONTENT);
					if (mapValues != null) {
						if (mapValues.has("display-settings")) {
							JSONObject map = mapValues.getJSONObject("display-settings");
							if (map.has("color")) {
								color = map.getString("color");
							}
						}
					}
				}
			} catch (Exception e) {
				Debug.out(e);
			}

			return color;
		}
		
		public long getRatingExpireyMins(String hash, String type) {
			long expiryMins = -1;
			try {
				JSONObject mapRating = (JSONObject) reply.get(hash);
				if (mapRating != null) {
					JSONObject mapValues = (JSONObject) mapRating.get(RATE_TYPE_CONTENT);
					if (mapValues != null) {
						if (mapValues.has("expires-in-mins")) {
							Object val = mapValues.get("expires-in-mins");
							if (val instanceof Long) {
								expiryMins = ((Long) val).longValue();
							}
						}
					}
				}
			} catch (Exception e) {
				Debug.out(e);
			}

			return expiryMins;
		}

		/**
		 * @return
		 */
		public Map getMap() {
			return reply;
		}
	}
}
