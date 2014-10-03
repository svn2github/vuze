/**
 * Created on Oct 3, 2014
 *
 * Copyright Azureus Software, Inc.  All rights reserved.
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
 
package com.aelitis.azureus.ui.swt.columns.tag;

import org.gudy.azureus2.core3.util.SystemTime;

/**
 * @author TuxPaper
 * @created Oct 3, 2014
 *
 */
public class TagDiscovery
{
	private String name;
	private String torrentName;
	private byte[] hash;
	private long timestamp;
	
	public TagDiscovery(String name, String torrentName, byte[] hash) {
		super();
		this.name = name;
		this.torrentName = torrentName;
		this.hash = hash;
		this.timestamp = SystemTime.getCurrentTime();
	}

	public String getName() {
		return name;
	}

	public String getTorrentName() {
		return torrentName;
	}

	public byte[] getHash() {
		return hash;
	}
	
}
