/**
 * Created on Apr 22, 2008
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

package com.aelitis.azureus.buddy;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;

/**
 * @author TuxPaper
 * @created Apr 22, 2008
 *
 */
public class QueuedVuzeShare
{
	private String code;

	private String pk;

	private String downloadHash;
	
	private VuzeActivitiesEntry entry;

	/**
	 * @param code the code to set
	 */
	public void setCode(String code) {
		this.code = code;
	}

	/**
	 * @return the code
	 */
	public String getCode() {
		return code;
	}

	/**
	 * @param pk the pk to set
	 */
	public void setPk(String pk) {
		this.pk = pk;
	}

	/**
	 * @return the pk
	 */
	public String getPk() {
		return pk;
	}

	/**
	 * @param downloadHash the downloadHash to set
	 */
	public void setDownloadHash(String downloadHash) {
		this.downloadHash = downloadHash;
	}

	/**
	 * @return the downloadHash
	 */
	public String getDownloadHash() {
		return downloadHash;
	}

	/**
	 * @param entry the entry to set
	 */
	public void setActivityEntry(VuzeActivitiesEntry entry) {
		this.entry = entry;
	}

	/**
	 * @return the entry
	 */
	public VuzeActivitiesEntry getActivityEntry() {
		return entry;
	}
}
