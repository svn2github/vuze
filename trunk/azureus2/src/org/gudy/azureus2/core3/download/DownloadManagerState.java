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

import java.io.File;
import java.util.Map;

import org.gudy.azureus2.core3.category.Category;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.plugins.download.Download;

/**
 * @author parg
 */

public interface 
DownloadManagerState 
{
	public static final String AT_CATEGORY					= "category";
	public static final String AT_NETWORKS					= "networks";
	public static final String AT_USER						= "user";
	public static final String AT_PEER_SOURCES				= "peersources";
	public static final String AT_TRACKER_CLIENT_EXTENSIONS	= "trackerclientextensions";
	public static final String AT_FILE_LINKS				= "filelinks";
	public static final String AT_FILE_STORE_TYPES			= "storetypes";
	public static final String AT_FILE_DOWNLOADED			= "filedownloaded";
	public static final String AT_FLAGS						= "flags";
	
	public static final long FLAG_ONLY_EVER_SEEDED	= Download.FLAG_ONLY_EVER_SEEDED;
	
	public TOTorrent
	getTorrent();
	
	public DownloadManager
	getDownloadManager();
	
	public File 
	getStateFile(
		String	name );
	
	public void
	setFlag(
		long		flag,
		boolean		set );
	
	public boolean
	getFlag(
		long		flag );
	
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
	
	public String
	getTrackerClientExtensions();
	
	public void
	setTrackerClientExtensions(
		String		value );
	
	public String[]		// from AENetworkClassifier constants
	getNetworks();
	
	public boolean 
	isNetworkEnabled(
	    String		network); //from AENetworkClassifier constants
	
	public void
	setNetworks(
		String[]	networks );	// from AENetworkClassifier constants
	
	public void
	setNetworkEnabled(
	    String		network,				// from AENetworkClassifier constants
	    boolean		enabled);
	
	public String[]		// from PEPeerSource constants
	getPeerSources();
	
	public boolean
	isPeerSourcePermitted(
		String	peerSource );
	
	public boolean
	isPeerSourceEnabled(
	    String		peerSource); // from PEPeerSource constants
	
	public void
	setPeerSources(
		String[]	sources );	// from PEPeerSource constants

	public void
	setPeerSourceEnabled(
	    String		source,		// from PEPeerSource constants
	    boolean		enabled);
	
		// file links
	
	public void
	setFileLink(
		File	link_source,
		File	link_destination );

	public void
	clearFileLinks();
	
	public File
	getFileLink(
		File	link_source );
	
		/**
		 * returns a File -> File map of the defined links (empty if no links)
		 * @return
		 */
	
	public Map
	getFileLinks();
	
		// general access
	
	public void
	setAttribute(
		String		name,		// make sure you use an AT_ value defined above
		String		value );
	
	public String
	getAttribute(
		String		name );		// make sure you use an AT_ value defined above

	public void
	setMapAttribute(
		String		name,
		Map			value );
	
	public Map
	getMapAttribute(
		String		name );
	
	public void
	setListAttribute(
		String		name,
		String[]	values );
	
	public String[]
	getListAttribute(
		String		name );
	
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
