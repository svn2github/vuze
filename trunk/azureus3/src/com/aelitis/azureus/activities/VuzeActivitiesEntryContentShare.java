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

import com.aelitis.azureus.buddy.impl.VuzeBuddyManager;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.login.NotLoggedInException;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentV3;
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
	extends VuzeActivitiesEntryBuddy
{
	private String userMessage;

	public VuzeActivitiesEntryContentShare() {
		super();
	}

	public VuzeActivitiesEntryContentShare(SelectedContentV3 content,
			String message) throws NotLoggedInException {
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

		{
			// For older clients, we must build the text for them

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
  
  		String textid = (message == null || message.length() == 0)
  				? "v3.activity.share-content.no-msg" : "v3.activity.share-content";
  
  		String text = MessageText.getString(textid, new String[] {
  			userInfo.getProfileAHREF(VuzeActivitiesConstants.TYPEID_BUDDYSHARE),
  			contentString,
  			userInfo.displayName,
  			message
  		});
  
  		setText(text);
		}
		
		if (dm != null) {
			setTorrentName(PlatformTorrentUtils.getContentTitle2(dm));
		} else {
			setTorrentName(content.getDisplayName());
		}

		setAssetImageURL(content.getThumbURL());
	  
		userMessage = message;

		setAssetHash(content.getHash());
		if (content.getDM() != null) {
			setDownloadManager(content.getDM());
		}
		setShowThumb(true);
		if (content.getImageBytes() == null) {
			setImageBytes(PlatformTorrentUtils.getContentThumbnail(torrent));
		} else {
			setImageBytes(content.getImageBytes());
		}
		setIsPlatformContent(ourContent);
		// The recipient will set the timestamp
		setTimestamp(0);
	}
	
	// @see com.aelitis.azureus.activities.VuzeActivitiesEntryBuddy#toMap()
	public Map toMap() {
		Map map = super.toMap();
		
		map.put("version", new Long(2));
		
		return map;
	}

	// @see com.aelitis.azureus.activities.VuzeActivitiesEntry#loadCommonFromMap(java.util.Map)
	public void loadCommonFromMap(Map map) {
		super.loadCommonFromMap(map);

		boolean legacy = getBuddy() == null;

		// legacy.  Normally will load from super
		String buddyID = MapUtils.getMapString(map, "buddyID", null);
		if ((buddy == null && buddyID != null) || (buddy != null && buddyID != null && !buddy.getLoginID().equals(buddyID))) {
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
		
		long version = MapUtils.getMapLong(map, "version", 1);
		
		if (version >= 2 || getText().indexOf("\\n\\n") == 0) {
			userMessage = MapUtils.getMapString(map, "userMessage", null);
			String textid = (userMessage == null || userMessage.length() == 0)
					? "v3.activity.share-content.no-msg" : "v3.activity.share-content";

  		String contentString;
  	  
  		if (isPlatformContent() || getTorrent() == null) {
  			String url = Constants.URL_PREFIX + Constants.URL_DETAILS
  					+ getAssetHash() + ".html?" + Constants.URL_SUFFIX
  					+ "&client_ref=" + VuzeActivitiesConstants.TYPEID_BUDDYSHARE;
  			contentString = "<A HREF=\"" + url + "\">" + getTorrentName()
  					+ "</A>";
  		} else {
  			contentString = getTorrentName();
  		}
			
			setText(MessageText.getString(textid, new String[] {
				buddy.getProfileAHREF(VuzeActivitiesConstants.TYPEID_BUDDYSHARE),
				contentString,
				buddy.getDisplayName(),
				userMessage
			}));
		}

		setDRM(MapUtils.getMapBoolean(torrentMap, "isDRM", false));
	}
}
