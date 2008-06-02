/**
 * Created on Apr 15, 2008
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

package com.aelitis.azureus.activities;

import java.util.Map;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.buddy.VuzeBuddy;
import com.aelitis.azureus.buddy.impl.VuzeBuddyManager;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.login.NotLoggedInException;
import com.aelitis.azureus.ui.selectedcontent.SelectedContent;
import com.aelitis.azureus.util.Constants;
import com.aelitis.azureus.util.LoginInfoManager;
import com.aelitis.azureus.util.MapUtils;
import com.aelitis.azureus.util.LoginInfoManager.LoginInfo;

/**
 * @author TuxPaper
 * @created Apr 15, 2008
 *
 */
public class VuzeActivitiesEntryContentShare
	extends VuzeActivitiesEntry
{
	private VuzeBuddy buddy;

	public VuzeActivitiesEntryContentShare() {
		super();
	}

	public VuzeActivitiesEntryContentShare(SelectedContent content, String message)
			throws NotLoggedInException {
		if (content == null) {
			return;
		}
		if (!LoginInfoManager.getInstance().isLoggedIn()) {
			VuzeBuddyManager.log("Can't share download: Not logged in");
			throw new NotLoggedInException();
		}

		DownloadManager dm = content.getDM();
		TOTorrent torrent = dm == null ? null : dm.getTorrent();

		boolean ourContent = content.isPlatformContent();
		
		setPlayable(content.canPlay());

		LoginInfo userInfo = LoginInfoManager.getInstance().getUserInfo();

		setTypeID(VuzeActivitiesConstants.TYPEID_BUDDYSHARE, true);
		setID(VuzeActivitiesConstants.TYPEID_BUDDYSHARE + "-"
				+ SystemTime.getCurrentTime());
		setTorrent(torrent);

		String contentString;

		if (ourContent || torrent == null) {
			String url = Constants.URL_PREFIX + Constants.URL_DETAILS
					+ content.getHash() + ".html?" + Constants.URL_SUFFIX
					+ "&client_ref=" + VuzeActivitiesConstants.TYPEID_BUDDYSHARE;
			contentString = "<A HREF=\"" + url + "\">" + content.getDisplayName()
					+ "</A>";
		} else {
			contentString = content.getDisplayName();
		}

		if (dm != null) {
			setTorrentName(PlatformTorrentUtils.getContentTitle2(dm));
		}

		setAssetImageURL(content.getThumbURL());
		
		String textid = (message == null || message.length() == 0)
				? "v3.activity.share-content.no-msg" : "v3.activity.share-content";

		String text = MessageText.getString(textid, new String[] {
			userInfo.getProfileAHREF(VuzeActivitiesConstants.TYPEID_BUDDYSHARE),
			contentString,
			userInfo.displayName,
			message
		});

		setText(text);
		setAssetHash(content.getHash());
		if (content.getDM() != null) {
			setDownloadManager(content.getDM());
		}
		setShowThumb(true);
		setImageBytes(PlatformTorrentUtils.getContentThumbnail(torrent));
		setIsPlatformContent(ourContent);
		// The recipient will set the timestamp
		setTimestamp(0);
	}

	// @see com.aelitis.azureus.activities.VuzeActivitiesEntry#loadCommonFromMap(java.util.Map)
	public void loadCommonFromMap(Map map) {
		super.loadCommonFromMap(map);

		String buddyID = MapUtils.getMapString(map, "buddyID", null);
		if (buddyID != null) {
			buddy = VuzeBuddyManager.getBuddyByLoginID(buddyID);
		}

		Map torrentMap = MapUtils.getMapMap(map, "torrent", null);
		if (torrentMap != null) {
			try {
				setTorrent(TOTorrentFactory.deserialiseFromMap(torrentMap));
			} catch (TOTorrentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		setDRM(MapUtils.getMapBoolean(torrentMap, "isDRM", false));
	}

	// @see com.aelitis.azureus.activities.VuzeActivitiesEntry#toMap()
	public Map toMap() {
		Map map = super.toMap();

		if (buddy != null) {
			map.put("buddyID", buddy.getLoginID());
		}
		return map;
	}

	public VuzeBuddy getBuddy() {
		return buddy;
	}

	public void setBuddy(VuzeBuddy buddy) {
		this.buddy = buddy;
	}
}
