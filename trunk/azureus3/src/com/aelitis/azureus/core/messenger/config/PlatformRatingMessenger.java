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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.messenger.PlatformMessage;
import com.aelitis.azureus.core.messenger.PlatformMessenger;
import com.aelitis.azureus.core.messenger.PlatformMessengerListener;
import com.aelitis.azureus.core.torrent.*;
import com.aelitis.azureus.util.MapUtils;

/**
 * @author TuxPaper
 * @created Oct 4, 2006
 *
 */
public class PlatformRatingMessenger
{
	private static final int GLOBAL_DEF_EXPIRY_MINS = 48 * 60;

	private static final long GLOBAL_RETRY_UPDATERATING = 10 * 60 * 1000;

	public static final String LISTENER_ID = "rating";

	public static final String OP_GET = "get";

	public static final String OP_SET = "set";

	public static final String RATE_TYPE_CONTENT = "content";

	public static final ArrayList listeners = new ArrayList();

	public static void getUserRating(String[] rateTypes,
			final String[] torrentHashes, long maxDelayMS) {

		PlatformMessage message = new PlatformMessage("AZMSG", "rating",
				"get-user", new Object[] {
					"rating-type",
					rateTypes,
					"torrent-hash",
					torrentHashes,
				}, maxDelayMS);

		PlatformMessengerListener listener = new PlatformMessengerListener() {
			public void messageSent(PlatformMessage message) {
			}

			public void replyReceived(PlatformMessage message, String replyType,
					Map reply) {
				// must create GetRatingReply object even if there's no replyListener
				// as class creation may cause other listener triggers
				RatingInfoList ratingReply = new PlatformRatingInfoList(reply);

				AzureusCore core = AzureusCoreFactory.getSingleton();
				for (int i = 0; i < torrentHashes.length; i++) {
					String hash = torrentHashes[i];
					long value = ratingReply.getRatingValue(hash,
							PlatformRatingMessenger.RATE_TYPE_CONTENT);
					if (value >= -1) {
						DownloadManager dm = core.getGlobalManager().getDownloadManager(
								new HashWrapper(Base32.decode(hash)));
						if (dm != null && dm.getTorrent() != null) {
							if (PlatformRatingMessenger.ratingSucceeded(reply)) {
								PlatformTorrentUtils.setUserRating(dm.getTorrent(), (int) value);
							} else {
								PlatformTorrentUtils.setUserRating(dm.getTorrent(), -1);
							}
						}
					}
				}
			}
		};

		PlatformMessenger.queueMessage(message, listener);
	}

	private static void getGlobalRating(String[] rateTypes,
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
					Map reply) {
				RatingInfoList ratingReply = new PlatformRatingInfoList(reply);
				invokeUpdateListeners(ratingReply);
				replyListener.replyReceived(replyType, ratingReply);
			}
		};

		PlatformMessenger.queueMessage(message, listener);
	}

	public static void setUserRating(final TOTorrent torrent, final int rating,
			final boolean updateGlobalRatingAfter, long maxDelayMS,
			final PlatformMessengerListener l) {

		if (torrent == null) {
			return;
		}

		String torrentHash = null;
		try {
			torrentHash = torrent.getHashWrapper().toBase32String();
		} catch (TOTorrentException e) {
		}
		if (torrentHash == null) {
			return;
		}

		final String fTorrentHash = torrentHash;

		List array = new ArrayList();
		array.add(PlatformMessage.parseParams(new Object[] {
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

		final int oldRating = PlatformTorrentUtils.getUserRating(torrent);
		final RatingInfoList ratingReply = new SingleUserRatingInfo(torrent);

		PlatformTorrentUtils.setUserRating(torrent, -2);

		PlatformMessenger.queueMessage(message, new PlatformMessengerListener() {
			// @see com.aelitis.azureus.core.messenger.PlatformMessengerListener#messageSent(com.aelitis.azureus.core.messenger.PlatformMessage)

			public void messageSent(PlatformMessage message) {
				if (l != null) {
					l.messageSent(message);
				}
			}

			// @see com.aelitis.azureus.core.messenger.PlatformMessengerListener#replyReceived(com.aelitis.azureus.core.messenger.PlatformMessage, java.lang.String, java.lang.Object)
			public void replyReceived(PlatformMessage message, String replyType,
					Map reply) {
				if (ratingSucceeded(reply)) {
					if (PlatformRatingMessenger.ratingSucceeded(reply)) {
						PlatformTorrentUtils.setUserRating(torrent, (int) rating);
						if (updateGlobalRatingAfter) {
							updateGlobalRating(torrent, 2000);
						}
					} else {
						PlatformTorrentUtils.setUserRating(torrent,
								oldRating == GlobalRatingUtils.RATING_WAITING
										? GlobalRatingUtils.RATING_NONE : oldRating);
					}
				}
				if (l != null) {
					l.replyReceived(message, replyType, reply);
				}
			}
		});
	}

	public static boolean ratingSucceeded(Map map) {
		String message = MapUtils.getMapString(map, "message", null);

		if (message != null) {
			return message.equals("Ok");
		}

		return MapUtils.getMapBoolean(map, "success", false);
	}

	private static abstract class GetRatingReplyListener
	{
		public abstract void messageSent();

		public abstract void replyReceived(String replyType, RatingInfoList reply);
	}

	public static void addListener(RatingUpdateListener2 l) {
		if (!listeners.contains(l)) {
			listeners.add(l);
		}
	}

	public static void removeListener(RatingUpdateListener2 l) {
		listeners.remove(l);
	}

	public static void addListener(RatingUpdateListener l) {
		if (!listeners.contains(l)) {
			listeners.add(l);
		}
	}

	public static void removeListener(RatingUpdateListener l) {
		listeners.remove(l);
	}

	public static void invokeUpdateListeners(RatingInfoList rating) {
		Object[] listArray = listeners.toArray();
		for (int i = 0; i < listArray.length; i++) {
			try {
  			if (listArray[i] instanceof RatingUpdateListener) {
  				((RatingUpdateListener)listArray[i]).ratingUpdated(rating);
  			} else if (listArray[i] instanceof RatingUpdateListener2) {
  				((RatingUpdateListener2)listArray[i]).ratingUpdated(rating);
  			}
			} catch (Exception e) {
				Debug.out(e);
			}
		}
	}

	public static void updateGlobalRating(final TOTorrent torrent, long maxDelayMS) {
		try {
			final String hash = torrent.getHashWrapper().toBase32String();
			if (PlatformTorrentUtils.DEBUG_CACHING) {
				PlatformTorrentUtils.log("v3.GR.caching: updateFromPlatform for "
						+ torrent);
			}
			PlatformRatingMessenger.getGlobalRating(new String[] {
				PlatformRatingMessenger.RATE_TYPE_CONTENT
			}, new String[] {
				hash
			}, 5000, new GetRatingReplyListener() {
				public void replyReceived(String replyType, RatingInfoList reply) {
					if (PlatformTorrentUtils.DEBUG_CACHING) {
						PlatformTorrentUtils.log("v3.GR.caching: reply '" + replyType
								+ "' for " + torrent);
					}
					if (replyType.equals(PlatformMessenger.REPLY_RESULT)) {
						String type = PlatformRatingMessenger.RATE_TYPE_CONTENT;
						String rating = reply.getRatingString(hash, type);
						String color = reply.getRatingColor(hash, type);
						long count = reply.getRatingCount(hash, type);
						long expireyMins = reply.getRatingExpireyMins(hash, type);

						if (expireyMins <= 0) {
							expireyMins = GLOBAL_DEF_EXPIRY_MINS;
						}

						long refreshOn = SystemTime.getCurrentTime()
								+ (expireyMins * 60 * 1000L);

						GlobalRatingUtils.setRating(torrent, rating, color, count,
								refreshOn);
					} else if (replyType.equals(PlatformMessenger.REPLY_EXCEPTION)) {
						// try again in a bit
						SimpleTimer.addEvent("Update MD Retry", SystemTime.getCurrentTime()
								+ GLOBAL_RETRY_UPDATERATING, new TimerEventPerformer() {
							public void perform(TimerEvent event) {
								if (PlatformTorrentUtils.DEBUG_CACHING) {
									PlatformTorrentUtils.log("v3.GR.caching: retrying..");
								}
								updateGlobalRating(torrent, 15000);
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

	// Old EMP needs this class
	public static interface RatingUpdateListener
	{
		public void ratingUpdated(GetRatingReply rating);
	}

	// Old EMP needs this class
	public static abstract class GetRatingReply
	{
		public abstract boolean hasHash(String hash);

		public abstract long getRatingValue(String hash, String type);

		public abstract long getRatingCount(String hash, String type);

		public abstract String getRatingString(String hash, String type);

		public abstract String getRatingColor(String hash, String type);

		public abstract long getRatingExpireyMins(String hash, String type);
	}
}
