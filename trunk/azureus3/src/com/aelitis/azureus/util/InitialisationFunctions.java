/*
 * Created on 14-Sep-2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.util;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.content.AzureusPlatformContentDirectory;
import com.aelitis.azureus.core.download.DownloadManagerEnhancer;
import com.aelitis.azureus.core.peer.cache.CacheDiscovery;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;

public class InitialisationFunctions
{
	private static final String EXTENSION_PREFIX = "azid";

	public static void earlyInitialisation(AzureusCore core) {
		
		DownloadUtils.initialise( core );
		
		DownloadManagerEnhancer dme = DownloadManagerEnhancer.initialise(core);

		registerTrackerURLExtensions(core);

		AzureusPlatformContentDirectory.register();

		CacheDiscovery.initialise( dme );
	}

	public static void lateInitialisation(AzureusCore core) {
		ExternalStimulusHandler.initialise(core);
	}

	protected static void 
	registerTrackerURLExtensions(
		AzureusCore core ) 
	{
		PluginInterface pi = core.getPluginManager().getDefaultPluginInterface();

		pi.getDownloadManager().addListener(
			new DownloadManagerListener() 
			{
			public void 
			downloadAdded(
				Download download )
			{
					// only add the azid to platform content
				
				if ( !PlatformTorrentUtils.isContent( download.getTorrent())){
					
					return;
				}

				DownloadUtils.addTrackerExtension( download, EXTENSION_PREFIX, Constants.AZID );
			}

			public void downloadRemoved(Download download) {
			}
		});
	}

}
