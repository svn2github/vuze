/**
 * Created on Mar 27, 2008
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

package com.aelitis.azureus.core.torrent;

import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;

/**
 * @author TuxPaper
 * @created Mar 27, 2008
 *
 */
public class SingleUserRatingInfo
	implements RatingInfoList
{
	private final TOTorrent torrent;

	public SingleUserRatingInfo(TOTorrent torrent) {
		this.torrent = torrent;
	}

	// @see com.aelitis.azureus.core.torrent.RatingInfoList#getRatingColor(java.lang.String, java.lang.String)
	public String getRatingColor(String hash, String type) {
		// not used for user rating
		return null;
	}

	// @see com.aelitis.azureus.core.torrent.RatingInfoList#getRatingCount(java.lang.String, java.lang.String)
	public long getRatingCount(String hash, String type) {
		return 1;
	}

	// @see com.aelitis.azureus.core.torrent.RatingInfoList#getRatingExpireyMins(java.lang.String, java.lang.String)
	public long getRatingExpireyMins(String hash, String type) {
		// not used for user rating
		return 0;
	}

	// @see com.aelitis.azureus.core.torrent.RatingInfoList#getRatingString(java.lang.String, java.lang.String)
	public String getRatingString(String hash, String type) {
		// not used for user rating
		return "--";
	}

	// @see com.aelitis.azureus.core.torrent.RatingInfoList#getRatingValue(java.lang.String, java.lang.String)
	public long getRatingValue(String hash, String type) {
		if (!hasHash(hash)) {
			return GlobalRatingUtils.RATING_NONE;
		}
		return PlatformTorrentUtils.getUserRating(torrent);
	}

	// @see com.aelitis.azureus.core.torrent.RatingInfoList#hasHash(java.lang.String)
	public boolean hasHash(String hash) {
		try {
			return torrent.getHashWrapper().toBase32String().equals(hash);
		} catch (TOTorrentException e) {
			return false;
		}
	}
}
