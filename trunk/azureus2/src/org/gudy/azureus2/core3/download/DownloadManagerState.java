/*
 * Created on 15-Nov-2004
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.core3.download;

import java.util.Map;

import org.gudy.azureus2.core3.category.Category;
import org.gudy.azureus2.core3.torrent.TOTorrent;

/**
 * @author parg
 */

public interface 
DownloadManagerState 
{
	public static final String AT_CATEGORY		= "category";
	public static final String AT_NETWORKS		= "networks";
	
	public TOTorrent
	getTorrent();
	
	public DownloadManager
	getDownloadManager();
	
	public void
	clearResumeData();
	
	public Map
	getResumeData();
	
	public void
	setResumeData(
		Map	data );
	
	public void
	clearTrackerResponseCache();
	
	public Map
	getTrackerResponseCache();
	
	public void
	setTrackerResponseCache(
		Map		value );
	
	public Category 
	getCategory();
	
	public void 
	setCategory(
		Category cat );
	
	public String[]		// from AENetworkClassifier constants
	getNetworks();
	
	public void
	setNetworks(
		String[]		networks );	// from AENetworkClassifier constants
	
	public void
	setAttribute(
		String		name,		// make sure you use an AT_ value defined above
		String		value );
	
	public String
	getAttribute(
		String		name );		// make sure you use an AT_ value defined above

	public void
	save();
	
		/**
		 * deletes the saved state
		 */
	
	public void
	delete();
	
	public void
	addListener(
		DownloadManagerStateListener	l );
	
	public void
	removeListener(
		DownloadManagerStateListener	l );
}
