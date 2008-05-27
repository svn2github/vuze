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

/**
 * Represents a piece of content (torrent) that is selected
 * 
 * @author TuxPaper
 * @created May 6, 2008
 *
 */
public class SelectedContent
{
	private String hash;

	private DownloadManager dm;

	private String displayName;
	
	private String thumbURL;
	
	private boolean isPlatformContent;

	/**
	 * @param dm2
	 * @throws Exception 
	 */
	public SelectedContent(DownloadManager dm)
			throws Exception {
		this.dm = dm;
		this.hash = dm.getTorrent().getHashWrapper().toBase32String();
		displayName = dm.getDisplayName();
	}

	/**
	 * 
	 */
	public SelectedContent(String hash, String displayName, boolean isPlatformContent) {
		this.hash = hash;
		this.displayName = displayName;
		this.isPlatformContent = isPlatformContent;
	}

	public SelectedContent() {
	}

	public String getThumbURL() {
		return thumbURL;
	}

	public void setThumbURL(String thumbURL) {
		this.thumbURL = thumbURL;
	}

	public String getHash() {
		return hash;
	}

	public void setHash(String hash, boolean isPlatformContent) {
		this.hash = hash;
		this.isPlatformContent = isPlatformContent;
	}

	public DownloadManager getDM() {
		return dm;
	}

	public void setDM(DownloadManager dm, boolean isPlatformContent) {
		this.dm = dm;
		this.isPlatformContent = isPlatformContent;
		if (this.dm != null) {
			try {
				hash = this.dm.getTorrent().getHashWrapper().toBase32String();
			} catch (Exception e) {
				hash = null;
			}
		}
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public boolean isPlatformContent() {
		return isPlatformContent;
	}

	public void setPlatformContent(boolean isPlatformContent) {
		this.isPlatformContent = isPlatformContent;
	}
}
