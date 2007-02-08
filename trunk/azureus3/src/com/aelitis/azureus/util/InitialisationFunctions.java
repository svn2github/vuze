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

import org.gudy.azureus2.core3.util.Base32;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.content.AzureusPlatformContentDirectory;
import com.aelitis.azureus.core.download.DownloadManagerEnhancer;
import com.aelitis.azureus.core.peer.cache.CacheDiscovery;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.torrent.TorrentManager;

public class InitialisationFunctions
{
	private static final String EXTENSION_PREFIX = "&azid=";

	public static void
	earlyInitialisation(
		AzureusCore		core )
	{
		DownloadManagerEnhancer.initialise( core );
		
		registerTrackerURLExtensions( core );
		
		AzureusPlatformContentDirectory.register();
		
		CacheDiscovery.initialise();
	}
	
	public static void
	lateInitialisation(
		AzureusCore		core )
	{	
		ExternalStimulusHandler.initialise( core );
	}
	
	protected static void registerTrackerURLExtensions(AzureusCore core) {
		byte[] secure_id = core.getCryptoManager().getSecureID();

		final String extension = EXTENSION_PREFIX + Base32.encode(secure_id);

		// initial hack to set the azid on tracker communications

		PluginInterface pi = core.getPluginManager().getDefaultPluginInterface();

		TorrentManager tm = pi.getTorrentManager();

		final TorrentAttribute ta = tm.getAttribute(TorrentAttribute.TA_TRACKER_CLIENT_EXTENSIONS);

		pi.getDownloadManager().addListener(new DownloadManagerListener() {
			public void downloadAdded(Download download) {
				// TODO: only add the attribtute for platform content

				String value = download.getAttribute(ta);

				if (value != null) {

					if (value.indexOf(extension) != -1) {

						return;
					}

					if ( value.indexOf(EXTENSION_PREFIX) != -1) {

						String[] bits = value.split("&");

						value = "";

						for (int i = 0; i < bits.length; i++) {

							String bit = bits[i].trim();

							if ( bit.length() == 0 ){
								
								continue;
							}
							
							if (!bit.startsWith(EXTENSION_PREFIX.substring(1))) {

								value += "&" + bit;
							}
						}
					}

					value += extension;

				} else {

					value = extension;
				}

				download.setAttribute(ta, value);
			}

			public void downloadRemoved(Download download) {
			}
		});
	}

}
