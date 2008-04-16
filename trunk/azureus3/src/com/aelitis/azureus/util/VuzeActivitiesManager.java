/**
 * Created on Jan 28, 2008 
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

package com.aelitis.azureus.util;

import java.util.*;

import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerListener;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.messenger.config.PlatformRatingMessenger;
import com.aelitis.azureus.core.messenger.config.PlatformVuzeActivitiesMessenger;
import com.aelitis.azureus.core.messenger.config.RatingUpdateListener2;
import com.aelitis.azureus.core.torrent.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkin;
import com.aelitis.azureus.ui.swt.utils.ImageLoader;

/**
 * Manage Vuze News Entries.  Loads, Saves, and expires them
 * 
 * @author TuxPaper
 * @created Jan 28, 2008
 *
 */
public class VuzeActivitiesManager
{
	public static final long MAX_LIFE_MS = 1000L * 60 * 60 * 24 * 30;

	private static final long DEFAULT_PLATFORM_REFRESH = 60 * 60 * 1000L * 24;

	private static final long RATING_REMINDER_DELAY = 1000L * 60 * 60 * 24 * 3;

	private static final long WEEK_MS = 604800000L;

	private static final String SAVE_FILENAME = "VuzeActivities.config";

	protected static final boolean SHOW_DM_REMOVED_ACTIVITY = false;

	private static ArrayList listeners = new ArrayList();

	private static ArrayList allEntries = new ArrayList();

	private static AEMonitor allEntries_mon = new AEMonitor("VuzeActivityMan");

	private static List removedEntries = new ArrayList();

	private static PlatformVuzeActivitiesMessenger.GetEntriesReplyListener replyListener;

	private static AEDiagnosticsLogger diag_logger;

	private static long lastVuzeNewsAt;

	private static boolean skipAutoSave = true;

	private static DownloadManagerListener dmListener;

	private static SWTSkin skin;

	private static ImageLoader imageLoader;

	private static AEMonitor config_mon = new AEMonitor("ConfigMon");

	static {
		if (System.getProperty("debug.vuzenews", "0").equals("1")) {
			diag_logger = AEDiagnostics.getLogger("v3.vuzenews");
			diag_logger.log("\n\nVuze News Logging Starts");
		} else {
			diag_logger = null;
		}
	}

	public static void initialize(final AzureusCore core, final SWTSkin skin) {
		new AEThread2("lazy init", true) {
			public void run() {
				_initialize(core, skin);
			}
		}.start();
	}

	private static void _initialize(AzureusCore core, SWTSkin skin) {
		VuzeActivitiesManager.skin = skin;
		imageLoader = skin.getImageLoader(skin.getSkinProperties());
		if (diag_logger != null) {
			diag_logger.log("Initialize Called");
		}

		loadEvents();

		replyListener = new PlatformVuzeActivitiesMessenger.GetEntriesReplyListener() {
			public void gotVuzeNewsEntries(VuzeActivitiesEntry[] entries,
					long refreshInMS) {
				if (diag_logger != null) {
					diag_logger.log("Received Reply from platform with " + entries.length
							+ " entries.  Refresh in " + refreshInMS);
				}

				addEntries(entries);

				if (refreshInMS <= 0) {
					refreshInMS = DEFAULT_PLATFORM_REFRESH;
				}

				SimpleTimer.addEvent("GetVuzeNews",
						SystemTime.getOffsetTime(refreshInMS), new TimerEventPerformer() {
							public void perform(TimerEvent event) {
								PlatformVuzeActivitiesMessenger.getEntries(Math.min(
										SystemTime.getCurrentTime() - lastVuzeNewsAt, MAX_LIFE_MS),
										5000, replyListener);
								lastVuzeNewsAt = SystemTime.getCurrentTime();
							}
						});
			}
		};

		pullActivitiesNow(5000);

		PlatformRatingMessenger.addListener(new RatingUpdateListener2() {
			// @see com.aelitis.azureus.core.messenger.config.PlatformRatingMessenger.RatingUpdateListener#ratingUpdated(com.aelitis.azureus.core.torrent.RatingInfoList)
			public void ratingUpdated(RatingInfoList rating) {
				if (!(rating instanceof SingleUserRatingInfo)) {
					return;
				}
				Object[] allEntriesArray = allEntries.toArray();
				for (int i = 0; i < allEntriesArray.length; i++) {
					VuzeActivitiesEntry entry = (VuzeActivitiesEntry) allEntriesArray[i];
					String typeID = entry.getTypeID();
					if (VuzeActivitiesEntry.TYPEID_RATING_REMINDER.equals(entry.getTypeID())
							&& entry.dm != null) {
						try {
							String hash = entry.dm.getTorrent().getHashWrapper().toBase32String();
							if (rating.hasHash(hash)
									&& rating.getRatingValue(hash,
											PlatformRatingMessenger.RATE_TYPE_CONTENT) != GlobalRatingUtils.RATING_NONE) {
								removeEntries(new VuzeActivitiesEntry[] {
									entry
								});
							}
						} catch (Exception e) {
						}
					}
				}
			}
		});

		dmListener = new DownloadManagerListener() {

			public void stateChanged(DownloadManager manager, int state) {
				// TODO Auto-generated method stub

			}

			public void positionChanged(DownloadManager download, int oldPosition,
					int newPosition) {
				// TODO Auto-generated method stub

			}

			public void filePriorityChanged(DownloadManager download,
					DiskManagerFileInfo file) {
				// TODO Auto-generated method stub

			}

			public void downloadComplete(DownloadManager dm) {
				VuzeActivitiesEntry entry = createDMCompleteEntry(dm);
				if (entry != null) {
					addEntries(new VuzeActivitiesEntry[] {
						entry
					});
				}
				dm.removeListener(this);
			}

			public void completionChanged(DownloadManager manager, boolean completed) {
				// TODO Auto-generated method stub

			}
		};

		GlobalManagerListener gmListener = new GlobalManagerListener() {

			public void seedingStatusChanged(boolean seeding_only_mode) {
			}

			public void downloadManagerRemoved(DownloadManager dm) {
				try {
					if (PlatformTorrentUtils.getAdId(dm.getTorrent()) != null) {
						return;
					}

					VuzeActivitiesEntry[] entries = getAllEntries();
					for (int i = 0; i < entries.length; i++) {
						VuzeActivitiesEntry oldEntry = entries[i];
						if (oldEntry.dm != null && oldEntry.dm.equals(dm)) {
							removeEntries(new VuzeActivitiesEntry[] {
								oldEntry
							});
						}
					}
					if (SHOW_DM_REMOVED_ACTIVITY) {
						VuzeActivitiesEntry entry = new VuzeActivitiesEntry();

						String hash = dm.getTorrent().getHashWrapper().toBase32String();
						String title;
						if (PlatformTorrentUtils.isContent(dm.getTorrent(), true)) {
							String url = Constants.URL_PREFIX + Constants.URL_DETAILS + hash
									+ ".html?" + Constants.URL_SUFFIX + "&client_ref=activity-"
									+ VuzeActivitiesEntry.TYPEID_DL_REMOVE;
							title = "<A HREF=\"" + url + "\">"
									+ PlatformTorrentUtils.getContentTitle2(dm) + "</A>";
							entry.assetHash = hash;
						} else {
							title = PlatformTorrentUtils.getContentTitle2(dm);
						}

						entry.setTimestamp(SystemTime.getCurrentTime());
						entry.id = hash + ";r" + entry.getTimestamp();
						entry.text = title + " has been removed from your library";
						entry.setTypeID(VuzeActivitiesEntry.TYPEID_DL_REMOVE, true);
						addEntries(new VuzeActivitiesEntry[] {
							entry
						});
					}
				} catch (Throwable t) {
					// ignore
				}

				dm.removeListener(dmListener);
			}

			public void downloadManagerAdded(DownloadManager dm) {
				List entries = registerDM(dm);
				if (entries != null && entries.size() > 0) {
					addEntries((VuzeActivitiesEntry[]) entries.toArray(new VuzeActivitiesEntry[0]));
				}
			}

			public void destroyed() {
			}

			public void destroyInitiated() {
			}
		};

		List newEntries = new ArrayList();
		GlobalManager gm = core.getGlobalManager();
		gm.addListener(gmListener, false);

		List downloadManagers = gm.getDownloadManagers();
		for (Iterator iter = downloadManagers.iterator(); iter.hasNext();) {
			DownloadManager dm = (DownloadManager) iter.next();
			List entries = registerDM(dm);
			if (entries != null && entries.size() > 0) {
				newEntries.addAll(entries);
			}
		}

		if (newEntries.size() > 0) {
			trimReminders(newEntries, false);
			addEntries((VuzeActivitiesEntry[]) newEntries.toArray(new VuzeActivitiesEntry[0]));
		}

		try {
			allEntries_mon.enter();

			trimReminders(allEntries, true);
		} finally {
			allEntries_mon.exit();
		}
	}

	/**
	 * @param allEntries2
	 *
	 * @since 3.0.4.3
	 */
	private static void trimReminders(List entries, boolean liveRemove) {
		List listReminders = new ArrayList();
		for (Iterator iter = entries.iterator(); iter.hasNext();) {
			VuzeActivitiesEntry entry = (VuzeActivitiesEntry) iter.next();
			if (VuzeActivitiesEntry.TYPEID_RATING_REMINDER.equals(entry.getTypeID())) {
				listReminders.add(entry);
			}
		}
		if (listReminders.size() > 3) {
			Collections.sort(listReminders); // will be sorted by date ascending
			long weekBreak = SystemTime.getCurrentTime() - (WEEK_MS * 4);
			int numInWeek = 0;
			for (Iterator iter = listReminders.iterator(); iter.hasNext();) {
				VuzeActivitiesEntry entry = (VuzeActivitiesEntry) iter.next();

				if (entry.getTimestamp() < weekBreak) {
					numInWeek++;
					if (numInWeek > 3) {
						if (liveRemove) {
							removeEntries(new VuzeActivitiesEntry[] {
								entry
							});
						} else {
							entries.remove(entry);
						}
					}
				} else {
					numInWeek = 1;
					while (entry.getTimestamp() >= weekBreak) {
						weekBreak += WEEK_MS;
					}
				}
			}
		}
	}

	/**
	 * @param dm
	 * @return
	 *
	 * @since 3.0.4.3
	 */
	protected static VuzeActivitiesEntry createDMCompleteEntry(DownloadManager dm) {
		try {
			String hash = dm.getTorrent().getHashWrapper().toBase32String();

			long completedTime = dm.getDownloadState().getLongParameter(
					DownloadManagerState.PARAM_DOWNLOAD_COMPLETED_TIME);

			String id = hash + ";c" + completedTime;

			//System.out.println("DC " + dm.getDisplayName());
			VuzeActivitiesEntry[] entries = getAllEntries();
			for (int i = 0; i < entries.length; i++) {
				VuzeActivitiesEntry oldEntry = entries[i];
				if (oldEntry.dm != null && oldEntry.dm.equals(dm)
						&& VuzeActivitiesEntry.TYPEID_DL_ADDED.equals(oldEntry.getTypeID())) {
					//System.out.println("remove added entry " + oldEntry.id);
					removeEntries(new VuzeActivitiesEntry[] {
						oldEntry
					});
				}
			}

			String title;
			if (PlatformTorrentUtils.isContent(dm.getTorrent(), true)) {
				String url = Constants.URL_PREFIX + Constants.URL_DETAILS + hash
						+ ".html?" + Constants.URL_SUFFIX + "&client_ref=activity-"
						+ VuzeActivitiesEntry.TYPEID_DL_COMPLETE;
				title = "<A HREF=\"" + url + "\">"
						+ PlatformTorrentUtils.getContentTitle2(dm) + "</A>";
			} else {
				title = PlatformTorrentUtils.getContentTitle2(dm);
			}

			VuzeActivitiesEntry entry = new VuzeActivitiesEntry();
			entry.setTimestamp(completedTime);
			entry.id = id;
			entry.text = title + " has completed downloading";
			entry.setTypeID(VuzeActivitiesEntry.TYPEID_DL_COMPLETE, true);
			entry.dm = dm;

			return entry;
		} catch (Throwable t) {
			Debug.out(t);
		}
		return null;
	}

	private static List registerDM(DownloadManager dm) {
		TOTorrent torrent = dm.getTorrent();
		if (PlatformTorrentUtils.getAdId(torrent) != null) {
			return null;
		}

		boolean isContent = PlatformTorrentUtils.isContent(torrent, true);

		List entries = new ArrayList();

		if (dm.getAssumedComplete()) {
			VuzeActivitiesEntry entry = createDMCompleteEntry(dm);
			if (entry != null) {
				entries.add(entry);
			}
		} else {
			try {
				long addedOn = (dm == null) ? 0
						: dm.getDownloadState().getLongParameter(
								DownloadManagerState.PARAM_DOWNLOAD_ADDED_TIME);
				if (addedOn < getCutoffTime()) {
					return null;
				}

				VuzeActivitiesEntry entry = new VuzeActivitiesEntry();
				entries.add(entry);
				String hash = torrent.getHashWrapper().toBase32String();

				String title;
				if (isContent) {
					String url = Constants.URL_PREFIX + Constants.URL_DETAILS + hash
							+ ".html?" + Constants.URL_SUFFIX + "&client_ref=activity-"
							+ VuzeActivitiesEntry.TYPEID_DL_ADDED;
					title = "<A HREF=\"" + url + "\">"
							+ PlatformTorrentUtils.getContentTitle2(dm) + "</A>";
					entry.assetHash = hash;
				} else {
					title = PlatformTorrentUtils.getContentTitle2(dm);
				}

				entry.id = hash + ";a" + addedOn;
				entry.text = title + " has been added to your download list";
				entry.setTimestamp(addedOn);
				entry.setTypeID(VuzeActivitiesEntry.TYPEID_DL_ADDED, true);
				entry.dm = dm;
			} catch (Throwable t) {
				// ignore
			}

			dm.addListener(dmListener);
		}

		try {
			if (isContent) {
				long completedOn = dm.getDownloadState().getLongParameter(
						DownloadManagerState.PARAM_DOWNLOAD_COMPLETED_TIME);
				if (completedOn > 0
						&& SystemTime.getCurrentTime() - completedOn > RATING_REMINDER_DELAY) {
					int userRating = PlatformTorrentUtils.getUserRating(torrent);
					if (userRating < 0) {
						VuzeActivitiesEntry entry = new VuzeActivitiesEntry();
						entries.add(entry);

						String hash = torrent.getHashWrapper().toBase32String();
						String title;
						String url = Constants.URL_PREFIX + Constants.URL_DETAILS + hash
								+ ".html?" + Constants.URL_SUFFIX + "&client_ref=activity-"
								+ VuzeActivitiesEntry.TYPEID_RATING_REMINDER;
						title = "<A HREF=\"" + url + "\">"
								+ PlatformTorrentUtils.getContentTitle2(dm) + "</A>";
						entry.assetHash = hash;

						entry.dm = dm;
						entry.showThumb = true;
						entry.id = hash + ";r" + completedOn;
						entry.text = "To improve your recommendations, please rate "
								+ title;
						entry.setTimestamp(SystemTime.getCurrentTime());
						entry.setTypeID(VuzeActivitiesEntry.TYPEID_RATING_REMINDER, true);
					}
				}
			}
		} catch (Throwable t) {
			// ignore
		}

		return entries;
	}

	/**
	 * Pull entries from webapp
	 * 
	 * @param delay max time to wait before running request
	 *
	 * @since 3.0.4.3
	 */
	public static void pullActivitiesNow(long delay) {
		PlatformVuzeActivitiesMessenger.getEntries(Math.min(
				SystemTime.getCurrentTime() - lastVuzeNewsAt, MAX_LIFE_MS), delay,
				replyListener);
		lastVuzeNewsAt = SystemTime.getCurrentTime();
	}

	/**
	 * Pull entries from webapp
	 * 
	 * @param agoMS Pull all events within this timespan (ms)
	 * @param delay max time to wait before running request
	 *
	 * @since 3.0.4.3
	 */
	public static void pullActivitiesNow(long agoMS, long delay) {
		PlatformVuzeActivitiesMessenger.getEntries(agoMS, delay, replyListener);
		lastVuzeNewsAt = SystemTime.getCurrentTime();
	}

	/**
	 * Clear the removed entries list so that an entry that was once deleted will
	 * will be able to be added again
	 * 
	 *
	 * @since 3.0.4.3
	 */
	public static void resetRemovedEntries() {
		removedEntries.clear();
		saveEvents();
	}

	/**
	 * 
	 *
	 * @since 3.0.4.3
	 */
	private static void loadEvents() {
		skipAutoSave = true;

		try {

			Map map = BDecoder.decodeStrings(FileUtil.readResilientConfigFile(SAVE_FILENAME));

			lastVuzeNewsAt = MapUtils.getMapLong(map, "LastCheck", 0);
			long cutoffTime = getCutoffTime();
			if (lastVuzeNewsAt < cutoffTime) {
				lastVuzeNewsAt = cutoffTime;
			}

			Object value;

			List newRemovedEntries = (List) MapUtils.getMapObject(map,
					"removed-entries", null, List.class);
			if (newRemovedEntries != null) {
				for (Iterator iter = newRemovedEntries.iterator(); iter.hasNext();) {
					value = iter.next();
					if (!(value instanceof Map)) {
						continue;
					}
					VuzeActivitiesEntry entry = VuzeActivitiesEntry.readFromMap((Map) value);

					if (entry.getTimestamp() > cutoffTime) {
						removedEntries.add(entry);
					}
				}
			}

			value = map.get("entries");
			if (!(value instanceof List)) {
				return;
			}

			List entries = (List) value;
			for (Iterator iter = entries.iterator(); iter.hasNext();) {
				value = iter.next();
				if (!(value instanceof Map)) {
					continue;
				}

				VuzeActivitiesEntry entry = VuzeActivitiesEntry.readFromMap((Map) value);
				
				if (VuzeActivitiesEntry.TYPEID_RATING_REMINDER.equals(entry.getTypeID())) {
					entry.showThumb = true;
				}

				if (entry.getTimestamp() > cutoffTime) {
					addEntries(new VuzeActivitiesEntry[] {
						entry
					});
				}
			}
		} finally {
			skipAutoSave = false;
		}
	}

	private static void saveEvents() {
		if (skipAutoSave) {
			return;
		}

		try {
			config_mon.enter();

			Map mapSave = new HashMap();
			mapSave.put("LastCheck", new Long(lastVuzeNewsAt));

			List entriesList = new ArrayList();

			VuzeActivitiesEntry[] allEntriesArray = getAllEntries();
			for (int i = 0; i < allEntriesArray.length; i++) {
				VuzeActivitiesEntry entry = allEntriesArray[i];
				boolean isHeader = VuzeActivitiesEntry.TYPEID_HEADER.equals(entry.getTypeID());
				if (!isHeader) {
					entriesList.add(entry.toMap());
				}
			}
			mapSave.put("entries", entriesList);

			List removedEntriesList = new ArrayList();
			for (Iterator iter = removedEntries.iterator(); iter.hasNext();) {
				VuzeActivitiesEntry entry = (VuzeActivitiesEntry) iter.next();
				removedEntriesList.add(entry.toMap());
			}
			mapSave.put("removed-entries", removedEntriesList);

			FileUtil.writeResilientConfigFile(SAVE_FILENAME, mapSave);

		} catch (Throwable t) {
			Debug.out(t);
		} finally {
			config_mon.exit();
		}
	}

	public static long getCutoffTime() {
		return SystemTime.getOffsetTime(-MAX_LIFE_MS);
	}

	public static void addListener(VuzeActivitiesListener l) {
		listeners.add(l);
	}

	public static void removeListener(VuzeActivitiesListener l) {
		listeners.remove(l);
	}

	/**
	 * 
	 * @param entries
	 * @return list of entries actually added (no dups)
	 *
	 * @since 3.0.4.3
	 */
	public static VuzeActivitiesEntry[] addEntries(VuzeActivitiesEntry[] entries) {
		long cutoffTime = getCutoffTime();

		ArrayList newEntries = new ArrayList();

		try {
			allEntries_mon.enter();

			for (int i = 0; i < entries.length; i++) {
				VuzeActivitiesEntry entry = entries[i];
				boolean isHeader = VuzeActivitiesEntry.TYPEID_HEADER.equals(entry.getTypeID());
				if ((entry.getTimestamp() >= cutoffTime || isHeader)
						&& !allEntries.contains(entry) && !removedEntries.contains(entry)) {
					newEntries.add(entry);
					allEntries.add(entry);
				}
			}
		} finally {
			allEntries_mon.exit();
		}

		saveEvents();
		//Collections.sort(allEntries);

		VuzeActivitiesEntry[] newEntriesArray = (VuzeActivitiesEntry[]) newEntries.toArray(new VuzeActivitiesEntry[newEntries.size()]);

		Object[] listenersArray = listeners.toArray();
		for (int i = 0; i < listenersArray.length; i++) {
			VuzeActivitiesListener l = (VuzeActivitiesListener) listenersArray[i];
			l.vuzeNewsEntriesAdded(newEntriesArray);
		}

		return newEntriesArray;
	}

	public static void removeEntries(VuzeActivitiesEntry[] entries) {
		long cutoffTime = getCutoffTime();

		try {
			allEntries_mon.enter();

			for (int i = 0; i < entries.length; i++) {
				VuzeActivitiesEntry entry = entries[i];
				if (entry == null) {
					continue;
				}
				//System.out.println("remove " + entry.id);
				allEntries.remove(entry);
				boolean isHeader = VuzeActivitiesEntry.TYPEID_HEADER.equals(entry.getTypeID());
				if (entry.getTimestamp() > cutoffTime && !isHeader) {
					removedEntries.add(entry);
				}
			}
		} finally {
			allEntries_mon.exit();
		}

		Object[] listenersArray = listeners.toArray();
		for (int i = 0; i < listenersArray.length; i++) {
			VuzeActivitiesListener l = (VuzeActivitiesListener) listenersArray[i];
			l.vuzeNewsEntriesRemoved(entries);
		}
		saveEvents();
	}

	public static VuzeActivitiesEntry[] getAllEntries() {
		return (VuzeActivitiesEntry[]) allEntries.toArray(new VuzeActivitiesEntry[allEntries.size()]);
	}

	public static void log(String s) {
		if (diag_logger != null) {
			diag_logger.log(s);
		}
	}

	/**
	 * @param vuzeActivitiesEntry
	 *
	 * @since 3.0.4.3
	 */
	public static void triggerEntryChanged(VuzeActivitiesEntry entry) {
		Object[] listenersArray = listeners.toArray();
		for (int i = 0; i < listenersArray.length; i++) {
			VuzeActivitiesListener l = (VuzeActivitiesListener) listenersArray[i];
			l.vuzeNewsEntryChanged(entry);
		}
		saveEvents();
	}

	/**
	 * @param map
	 * @return
	 *
	 * @since 3.0.5.3
	 */
	public static VuzeActivitiesEntry createEntryFromMap(Map map) {
		VuzeActivitiesEntry entry;
		String typeID = MapUtils.getMapString(map, "type-id", null);
		if (VuzeActivitiesEntryBuddyRequest.TYPEID_BUDDYREQUEST.equals(typeID)) {
			entry = new VuzeActivitiesEntryBuddyRequest(map);
		} else {
			entry = new VuzeActivitiesEntry(map);
		}
		entry.setAssetImageURL(MapUtils.getMapString(map, "related-image-url", null));
		return entry;
	}
}
