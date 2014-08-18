/*
 * Created on 15-Nov-2004
 * Created by Paul Gardner
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
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
 */

package org.gudy.azureus2.core3.download;

import java.io.File;

import org.gudy.azureus2.core3.download.impl.*;

import org.gudy.azureus2.core3.torrent.*;

/**
 * @author parg
 *
 */

public class 
DownloadManagerStateFactory 
{
	public static DownloadManagerState
	getDownloadState(
		TOTorrent		torrent )
	
		throws TOTorrentException
	{
		return( DownloadManagerStateImpl.getDownloadState( torrent ));
	}
	
	
	public static void
	loadGlobalStateCache()
	{
		DownloadManagerStateImpl.loadGlobalStateCache();
	}
	
	public static void
	saveGlobalStateCache()
	{
		DownloadManagerStateImpl.saveGlobalStateCache();
	}
	
	public static void
	discardGlobalStateCache()
	{
		DownloadManagerStateImpl.discardGlobalStateCache();
	}
	
	public static void
	importDownloadState(
		File		source_dir,
		byte[]		download_hash )
	
		throws DownloadManagerException
	{
		DownloadManagerStateImpl.importDownloadState( source_dir, download_hash );
	}
	
	public static void
	deleteDownloadState(
		byte[]		download_hash )
	
		throws DownloadManagerException
	{
		DownloadManagerStateImpl.deleteDownloadState( download_hash );
	}
	
	public static void
	deleteDownloadState(
		File		source_dir,
		byte[]		download_hash )
	
		throws DownloadManagerException
	{
		DownloadManagerStateImpl.deleteDownloadState( source_dir, download_hash );
	}
}
