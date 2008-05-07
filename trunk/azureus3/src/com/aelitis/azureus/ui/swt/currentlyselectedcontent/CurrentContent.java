/**
 * Created on May 6, 2008
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
 
package com.aelitis.azureus.ui.swt.currentlyselectedcontent;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrentException;

import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;

/**
 * @author TuxPaper
 * @created May 6, 2008
 *
 */
public class CurrentContent
{
	public String hash;
	public DownloadManager dm;
	public String displayName;

	/**
	 * @param dm2
	 */
	public CurrentContent(DownloadManager dm) {
		this.dm = dm;
		try {
			this.hash = dm.getTorrent().getHashWrapper().toBase32String();
		} catch (TOTorrentException e) {
		}
		displayName = PlatformTorrentUtils.getContentTitle2(dm);
	}
}
