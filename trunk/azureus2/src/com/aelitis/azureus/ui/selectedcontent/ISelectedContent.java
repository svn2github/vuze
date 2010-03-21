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
import org.gudy.azureus2.core3.torrent.TOTorrent;

/**
 * @author TuxPaper
 * @created Jun 9, 2008
 *
 */
public interface ISelectedContent
{

	public abstract String getHash();

	public abstract void setHash(String hash);

	public abstract DownloadManager getDownloadManager();

	public abstract int getFileIndex(); 
		
	public abstract void setDownloadManager(DownloadManager dm);

	public abstract TOTorrent getTorrent();
	
	public void setTorrent( TOTorrent torrent );
	
	public abstract String getDisplayName();

	public abstract void setDisplayName(String displayName);

	/**
	 * @since 3.1.1.1
	 */
	public abstract DownloadUrlInfo getDownloadInfo();

	/**
	 * @since 3.1.1.1
	 */
	public abstract void setDownloadInfo(DownloadUrlInfo downloadInfo);
}