/*
 * Created on Apr 8, 2009
 * Created by Paul Gardner
 * 
 * Copyright 2009 Vuze, Inc.  All rights reserved.
 * 
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.buddy;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;

import com.aelitis.azureus.ui.selectedcontent.DownloadUrlInfo;

public interface 
VuzeShareable 
{
	public String
	getHash();
	
	public String
	getDisplayName();
	
	public String
	getThumbURL();
	
	public boolean
	isPlatformContent();
	
	public String
	getPublisher();
	
	public byte[]
	getImageBytes();
	
	public long
	getSize();
	
	public boolean
	canPlay();
	
		/**
		 * Can return null 
		 * @return
		 */
	
	public TOTorrent
	getTorrent();
	
		/**
		 * Can return null if none associated with this share
		 * @return
		 */
	
	public DownloadManager
	getDownloadManager();
	
		/**
		 * Can return null if none associated with this share
		 * @return
		 */
	
	public DownloadUrlInfo 
	getDownloadInfo();
}
