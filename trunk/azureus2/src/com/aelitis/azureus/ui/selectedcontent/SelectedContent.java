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
 
package com.aelitis.azureus.ui.selectedcontent;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrentException;

/**
 * Represents a piece of content (torrent) that is selected
 * 
 * @author TuxPaper
 * @created May 6, 2008
 *
 */
public class SelectedContent
{
	public String hash;
	public DownloadManager dm;
	public String displayName;

	/**
	 * @param dm2
	 * @throws Exception 
	 */
	public SelectedContent(DownloadManager dm) throws Exception {
		this.dm = dm;
		this.hash = dm.getTorrent().getHashWrapper().toBase32String();
		displayName = dm.getDisplayName();
	}

	/**
	 * 
	 */
	public SelectedContent(String hash, String displayName) {
		this.hash = hash;
		this.displayName = displayName;
	}
}
