/**
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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

import java.io.File;
import java.util.*;

import org.bouncycastle.util.encoders.Base64;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.messenger.PlatformMessage;
import com.aelitis.azureus.core.messenger.PlatformMessenger;
import com.aelitis.azureus.core.messenger.PlatformMessengerListener;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.util.AdManager;
import com.aelitis.azureus.util.Constants;
import com.aelitis.azureus.util.MapUtils;

/**
 * @author TuxPaper
 * @created May 31, 2007
 *
 */
public class PlatformAdManager
{
	public static String LISTENER_ID = "ads";

	public static String OP_GETADS = "get-ads";

	public static String OP_GETPLAYLIST = "get-playlist";

	public static String OP_STOREIMPRESSIONS = "store-impressions";

	public static List unsentImpressions = new ArrayList();

	public static AEMonitor mon_unsentImpressions = new AEMonitor(
			"unsentImpressions");

	public static boolean DEBUG_ADS = false;

	private static int RESEND_DELAY = 1000 * 60 * 10; // 10 min

	private static TimerEvent resendEvent;

	public static void getAds(DownloadManager[] adEnabledDownloads,
			long maxDelayMS, final GetAdsDataReplyListener replyListener) {

		String[] hashes = new String[adEnabledDownloads.length];
		for (int i = 0; i < adEnabledDownloads.length; i++) {
			DownloadManager dm = adEnabledDownloads[i];
			try {
				hashes[i] = dm.getTorrent().getHashWrapper().toBase32String();
			} catch (TOTorrentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		DownloadManager[] existingAds = AdManager.getInstance().getAds(true);
		List ads = new ArrayList();
		for (int i = 0; i < existingAds.length; i++) {
			DownloadManager dm = existingAds[i];

			try {
				TOTorrent torrent = dm.getTorrent();
				String hash = torrent.getHashWrapper().toBase32String();
				String adid = PlatformTorrentUtils.getAdId(torrent);

				Map mapAd = new HashMap();
				mapAd.put("hash", hash);
				mapAd.put("id", adid);

				ads.add(mapAd);
			} catch (TOTorrentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		Map parameters = new HashMap();
		parameters.put("hashes", hashes);
		parameters.put("ads", ads);

		PlatformMessage message = new PlatformMessage("AZMSG", LISTENER_ID,
				OP_GETADS, parameters, maxDelayMS);

		PlatformMessenger.queueMessage(message, new PlatformMessengerListener() {
			public void replyReceived(PlatformMessage message, String replyType,
					Map reply) {
				if (!replyType.equals(PlatformMessenger.REPLY_RESULT)) {
					if (replyListener != null) {
						replyListener.replyReceived(replyType, reply);
					}
					return;
				}

				List adTorrents = new ArrayList();
				List torrentsList = (List) reply.get("torrents");
				if (torrentsList != null) {
					for (int i = 0; i < torrentsList.size(); i++) {
						byte[] torrentBEncoded = Base64.decode((String) torrentsList.get(i));
						try {
							TOTorrent torrent = TOTorrentFactory.deserialiseFromBEncodedByteArray(torrentBEncoded);
							adTorrents.add(torrent);
						} catch (TOTorrentException e) {
							Debug.out(e);
						}
					}
				}
				if (replyListener != null) {
					replyListener.adsReceived(adTorrents);
				}
			}

			public void messageSent(PlatformMessage message) {
				if (replyListener != null) {
					replyListener.messageSent();
				}
			}

		});
	}

	public static interface GetAdsDataReplyListener
	{
		public void messageSent();

		public void adsReceived(List torrents);

		public void replyReceived(String replyType, Map mapHashes);
	}

	public static void getPlayList(DownloadManager dmToPlay, String trackingURL,
			long maxDelayMS, final GetPlaylistReplyListener replyListener) {

		try {
			Map parameters = new HashMap();

			String sFile = dmToPlay.getDownloadState().getPrimaryFile();

			parameters.put("hash",
					dmToPlay.getTorrent().getHashWrapper().toBase32String());
			parameters.put("content-url", new File(sFile).toURL().toString());
			parameters.put("tracking-urls", new String[] {
				trackingURL
			});

			DownloadManager[] existingAds = AdManager.getInstance().getAds(false);
			List ads = new ArrayList();
			for (int i = 0; i < existingAds.length; i++) {
				DownloadManager dm = existingAds[i];

				try {
					TOTorrent torrent = dm.getTorrent();
					String hash = torrent.getHashWrapper().toBase32String();
					String adid = PlatformTorrentUtils.getAdId(torrent);

					Map mapAd = new HashMap();
					mapAd.put("hash", hash);
					mapAd.put("id", adid);
					String sAdFile = dm.getDownloadState().getPrimaryFile();
					mapAd.put("local-url", new File(sAdFile).toURL().toString());

					ads.add(mapAd);
				} catch (TOTorrentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			parameters.put("ads", ads);

			PlatformMessage message = new PlatformMessage("AZMSG", LISTENER_ID,
					OP_GETPLAYLIST, parameters, maxDelayMS);

			PlatformMessenger.queueMessage(message, new PlatformMessengerListener() {
				public void replyReceived(PlatformMessage message, String replyType,
						Map reply) {
					if (!replyType.equals(PlatformMessenger.REPLY_RESULT)) {
						replyListener.replyReceived(replyType, null);
						return;
					}

					String playlist = MapUtils.getMapString(reply, "playlist", null);
					if (replyListener != null) {
						replyListener.replyReceived(replyType, playlist);
					}
				}

				public void messageSent(PlatformMessage message) {
					if (replyListener != null) {
						replyListener.messageSent();
					}
				}

			});
		} catch (Exception e) {
			Debug.out(e);
		}
	}

	public static interface GetPlaylistReplyListener
	{
		public void messageSent();

		public void replyReceived(String replyType, String playlist);
	}

	public static void storeImpresssion(String trackingID, long viewedOn,
			String contentHash, long maxDelayMS) {
		// pass in contentHash instead of DownloadManager in case the user removed
		// the DM (and we are retrying)
		try {
			Map ad = new HashMap();

			ad.put("tracking-id", trackingID);
			ad.put("viewed-on", new Long(viewedOn));
			ad.put("content-hash", contentHash);

			try {
				mon_unsentImpressions.enter();
				unsentImpressions.add(ad);
			} finally {
				mon_unsentImpressions.exit();
			}
			saveUnsentImpressions();
			sendUnsentImpressions(maxDelayMS);
		} catch (Exception e) {
			Debug.out(e);
		}
	}

	public static void sendUnsentImpressions(long maxDelayMS) {
		// clear unsentImpressions.  If storing fails, we'll add them back in
		List sendingImpressions;
		try {
			mon_unsentImpressions.enter();

			sendingImpressions = unsentImpressions;
			unsentImpressions = new ArrayList();
		} finally {
			mon_unsentImpressions.exit();
		}

		final List fSendingImpressions = sendingImpressions;

		Map ads = new HashMap();
		ads.put("ads", fSendingImpressions);

		try {
			debug("sending " + fSendingImpressions.size() + " impressions");
			PlatformMessage message = new PlatformMessage("AZMSG", LISTENER_ID,
					OP_STOREIMPRESSIONS, ads, maxDelayMS);

			PlatformMessenger.queueMessage(message, new PlatformMessengerListener() {
				public void replyReceived(PlatformMessage message, String replyType,
						Map reply) {
					if (!replyType.equals(PlatformMessenger.REPLY_RESULT)) {
						debug("sending " + fSendingImpressions + " impressions failed");
						try {
							mon_unsentImpressions.enter();

							unsentImpressions.addAll(fSendingImpressions);

							setupResendTimer();
						} finally {
							mon_unsentImpressions.exit();
						}
						saveUnsentImpressions();
						return;
					}
					// TODO: check result to see which ones succeeded
					debug("sending " + fSendingImpressions.size()
							+ " impressions completed");
				}

				public void messageSent(PlatformMessage message) {
				}

			});
		} catch (Exception e) {
			Debug.out(e);
		}
	}

	/**
	 * 
	 *
	 * @since 3.0.1.5
	 */
	protected static void setupResendTimer() {
		if (resendEvent != null) {
			resendEvent.cancel();
			resendEvent = null;
		}
		resendEvent = SimpleTimer.addEvent("resender",
				SystemTime.getOffsetTime(RESEND_DELAY), new TimerEventPerformer() {
					public void perform(TimerEvent event) {
						debug("resend impressions triggered");
						sendUnsentImpressions(5000);
					}
				});
	}

	/**
	 * 
	 *
	 * @since 3.0.1.5
	 */
	private static void saveUnsentImpressions() {
		try {
			mon_unsentImpressions.enter();

			Map map = new HashMap();
			map.put("unsent", unsentImpressions);
			FileUtil.writeResilientConfigFile("unsentdata.config", map);
		} finally {
			mon_unsentImpressions.exit();
		}
	}

	public static void loadUnsentImpressions() {
		try {
			mon_unsentImpressions.enter();

			Map map = FileUtil.readResilientConfigFile("unsentdata.config");
			Object value = map.get("unsent");
			if (value instanceof List) {
				unsentImpressions = (List) value;
			} else {
				unsentImpressions.clear();
			}
		} finally {
			mon_unsentImpressions.exit();
		}
	}

	public static void debug(String string) {
		AEDiagnosticsLogger diag_logger = AEDiagnostics.getLogger("v3.ads");
		diag_logger.log(string);
		if (Constants.DIAG_TO_STDOUT || DEBUG_ADS) {
			System.out.println(Thread.currentThread().getName() + "|ADS|"
					+ System.currentTimeMillis() + "] " + string);
		}
	}

}
