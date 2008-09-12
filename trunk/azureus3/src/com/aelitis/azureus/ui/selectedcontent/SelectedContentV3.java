/**
 * Created on Jun 9, 2008
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

import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.selectedcontent.ISelectedContent;
import com.aelitis.azureus.ui.selectedcontent.SelectedContent;
import com.aelitis.azureus.util.PlayUtils;

/**
 * @author TuxPaper
 * @created Jun 9, 2008
 *
 */
public class SelectedContentV3
	implements ISelectedContent
{

	private final SelectedContent content;

	private boolean isPlatformContent;

	private boolean canPlay;

	private String thumbURL;
	
	private byte[] imageBytes;
	
	private DownloadUrlInfo downloadInfo;

	public SelectedContentV3(SelectedContent content) {
		this.content = content;
		this.setDM(content.getDM());
	}

	public SelectedContentV3() {
		content = new SelectedContent();
	}

	public SelectedContentV3(String hash, String displayName, boolean isPlatformContent,
			boolean canPlay) {
		this.isPlatformContent = isPlatformContent;
		this.canPlay = canPlay;
		content = new SelectedContent(hash, displayName);
	}

	public SelectedContentV3(DownloadManager dm) throws Exception {
		content = new SelectedContent();
		setDM(dm);
	}

	// @see com.aelitis.azureus.ui.selectedcontent.ISelectedContent#getDisplayName()
	public String getDisplayName() {
		return content.getDisplayName();
	}

	// @see com.aelitis.azureus.ui.selectedcontent.ISelectedContent#getDM()
	public DownloadManager getDM() {
		return content.getDM();
	}

	// @see com.aelitis.azureus.ui.selectedcontent.ISelectedContent#getHash()
	public String getHash() {
		return content.getHash();
	}

	// @see com.aelitis.azureus.ui.selectedcontent.ISelectedContent#setDisplayName(java.lang.String)
	public void setDisplayName(String displayName) {
		content.setDisplayName(displayName);
	}

	// @see com.aelitis.azureus.ui.selectedcontent.ISelectedContent#setDM(org.gudy.azureus2.core3.download.DownloadManager)
	public void setDM(DownloadManager dm) {
		content.setDM(dm);
		if (dm != null) {
			try {
				setHash(dm.getTorrent().getHashWrapper().toBase32String());
			} catch (TOTorrentException e) {
			}
  		setPlatformContent(PlatformTorrentUtils.isContent(dm.getTorrent(), true));
  		setDisplayName(PlatformTorrentUtils.getContentTitle2(dm));
  		setCanPlay(PlayUtils.canUseEMP(dm.getTorrent()));
  		setImageBytes(PlatformTorrentUtils.getContentThumbnail(dm.getTorrent()));
		}
	}

	// @see com.aelitis.azureus.ui.selectedcontent.ISelectedContent#setHash(java.lang.String)
	public void setHash(String hash) {
		content.setHash(hash);
	}

	public void setHash(String hash, boolean isPlatformContent) {
		content.setHash(hash);
		setPlatformContent(isPlatformContent);
	}
	
	public boolean isPlatformContent() {
		return isPlatformContent;
	}

	public void setPlatformContent(boolean isPlatformContent) {
		this.isPlatformContent = isPlatformContent;
	}

	public boolean canPlay() {
		return canPlay;
	}

	public void setCanPlay(boolean canPlay) {
		this.canPlay = canPlay;
	}

	public String getThumbURL() {
		return thumbURL;
	}

	public void setThumbURL(String thumbURL) {
		this.thumbURL = thumbURL;
	}

	/**
	 * @param imageBytes the imageBytes to set
	 */
	public void setImageBytes(byte[] imageBytes) {
		this.imageBytes = imageBytes;
	}

	/**
	 * @return the imageBytes
	 */
	public byte[] getImageBytes() {
		return imageBytes;
	}

	
	// @see com.aelitis.azureus.ui.selectedcontent.ISelectedContent#getDownloadInfo()
	public DownloadUrlInfo getDownloadInfo() {
		return downloadInfo;
	}
	
	// @see com.aelitis.azureus.ui.selectedcontent.ISelectedContent#setDownloadInfo(com.aelitis.azureus.ui.selectedcontent.SelectedContentDownloadInfo)
	public void setDownloadInfo(DownloadUrlInfo info) {
		this.downloadInfo = info;
	}
}
