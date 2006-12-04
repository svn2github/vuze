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

package com.aelitis.azureus.core.torrent;

import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.messenger.PlatformMessenger;
import com.aelitis.azureus.core.messenger.config.PlatformRatingMessenger;
import com.aelitis.azureus.core.messenger.config.PlatformRatingMessenger.GetRatingReplyListener;

/**
 * @author TuxPaper
 * @created Oct 24, 2006
 *
 */
public class GlobalRatingUtils
{
	private static final int DEF_EXPIRY_MINS = 48 * 60;

	private static final long RETRY_UPDATERATING = 10 * 60 * 1000;

	private static final String TOR_AZ_PROP_GLOBAL_RATING = "GlobalRating";

	private static final String GLOBAL_RATING_STRING = "String";

	private static final String GLOBAL_RATING_COLOR = "Color";

	private static final String GLOBAL_RATING_COUNT = "Count";

	private static final String GLOBAL_RATING_REFRESH_ON = "Refresh On";

	private static Map getTempGlobalRatingContentMap(TOTorrent torrent) {
		Map mapContent = PlatformTorrentUtils.getTempContentMap(torrent);
		Object o = mapContent.get(TOR_AZ_PROP_GLOBAL_RATING);

		if (o instanceof Map) {
			return (Map) o;
		}

		Map map = new HashMap();
		mapContent.put(TOR_AZ_PROP_GLOBAL_RATING, map);
		return map;
	}

	/**
	 * @param torrent
	 * @return
	 */
	public static String getRatingString(TOTorrent torrent) {
		Map mapContent = getTempGlobalRatingContentMap(torrent);
		Object o = mapContent.get(GLOBAL_RATING_STRING);
		if (o instanceof String) {
			return (String) o;
		} else if (o instanceof byte[]) {
			return new String((byte[]) o);
		}

		if (o != null) {
			Debug.out(GLOBAL_RATING_STRING + " is not String - " + o.getClass() + "("
					+ o + ")");
		}
		return null;
	}

	public static void setRating(final TOTorrent torrent, String rating,
			String color, long count, long refreshOn) {
		Map mapContent = getTempGlobalRatingContentMap(torrent);
		if (rating == null) {
			mapContent.remove(GLOBAL_RATING_STRING);
		} else {
			mapContent.put(GLOBAL_RATING_STRING, rating);
		}
		if (color == null) {
			mapContent.remove(GLOBAL_RATING_COLOR);
		} else {
			mapContent.put(GLOBAL_RATING_COLOR, color);
		}

		mapContent.put(GLOBAL_RATING_REFRESH_ON, new Long(refreshOn));
		mapContent.put(GLOBAL_RATING_COUNT, new Long(count));

		try {
			TorrentUtils.writeToFile(torrent);
		} catch (TOTorrentException e) {
			Debug.out(e);
		}

		if (PlatformTorrentUtils.DEBUG_CACHING) {
			Debug.outNoStack(
					"v3.GR.caching: setRating to "
							+ rating
							+ " for "
							+ torrent
							+ ".  Next refresh in "
							+ (refreshOn
									- SystemTime.getCurrentTime()), false);
		}
		SimpleTimer.addEvent("Update G.Rating", refreshOn,
				new TimerEventPerformer() {
					public void perform(TimerEvent event) {
						if (PlatformTorrentUtils.DEBUG_CACHING) {
							Debug.outNoStack(
									"v3.GR.caching: refresh timer calling updateFromPlatform",
									false);
						}
						updateFromPlatform(torrent, 15000);
					}
				});
	}

	public static String getColor(TOTorrent torrent) {
		Map mapContent = getTempGlobalRatingContentMap(torrent);
		Object o = mapContent.get(GLOBAL_RATING_COLOR);
		if (o instanceof String) {
			return (String) o;
		} else if (o instanceof byte[]) {
			return new String((byte[]) o);
		}
		if (o != null) {
			Debug.out(GLOBAL_RATING_COLOR + " is not String - " + o.getClass() + "("
					+ o + ")");
		}
		return null;
	}

	public static long getRefreshOn(TOTorrent torrent) {
		Map mapContent = getTempGlobalRatingContentMap(torrent);
		Long l = (Long) mapContent.get(GLOBAL_RATING_REFRESH_ON);
		if (l == null) {
			return SystemTime.getCurrentTime();
		}
		return l.longValue();
	}

	public static long getCount(TOTorrent torrent) {
		Map mapContent = getTempGlobalRatingContentMap(torrent);
		Long l = (Long) mapContent.get(GLOBAL_RATING_COUNT);
		if (l == null) {
			return 0;
		}
		return l.longValue();
	}

	public static void updateFromPlatform(final TOTorrent torrent, long maxDelayMS) {
		try {
			final String hash = torrent.getHashWrapper().toBase32String();
			if (PlatformTorrentUtils.DEBUG_CACHING) {
				Debug.outNoStack("v3.GR.caching: updateFromPlatform for " + torrent,
						false);
			}
			PlatformRatingMessenger.getGlobalRating(
					new String[] { PlatformRatingMessenger.RATE_TYPE_CONTENT
					}, new String[] { hash
					}, 5000, new GetRatingReplyListener() {
						public void replyReceived(String replyType,
								PlatformRatingMessenger.GetRatingReply reply) {

							if (PlatformTorrentUtils.DEBUG_CACHING) {
								Debug.outNoStack("v3.GR.caching: reply '" + replyType
										+ "' for " + torrent, false);
							}
							if (replyType.equals(PlatformMessenger.REPLY_RESULT)) {
								String type = PlatformRatingMessenger.RATE_TYPE_CONTENT;
								String rating = reply.getRatingString(hash, type);
								String color = reply.getRatingColor(hash, type);
								long count = reply.getRatingCount(hash, type);
								long expireyMins = reply.getRatingExpireyMins(hash, type);

								if (expireyMins <= 0) {
									expireyMins = DEF_EXPIRY_MINS;
								}

								long refreshOn = SystemTime.getCurrentTime()
										+ (expireyMins * 60 * 1000L);

								setRating(torrent, rating, color, count, refreshOn);
							} else if (replyType.equals(PlatformMessenger.REPLY_EXCEPTION)) {
								// try again in a bit
								SimpleTimer.addEvent("Update MD Retry",
										SystemTime.getCurrentTime() + RETRY_UPDATERATING,
										new TimerEventPerformer() {
											public void perform(TimerEvent event) {
												if (PlatformTorrentUtils.DEBUG_CACHING) {
													Debug.outNoStack("v3.GR.caching: retrying..", false);
												}
												updateFromPlatform(torrent, 15000);
											}
										});

							}

						}

						public void messageSent() {
						}
					});
		} catch (TOTorrentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
