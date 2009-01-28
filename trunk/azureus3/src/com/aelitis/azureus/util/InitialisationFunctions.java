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

import java.net.URL;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.cnetwork.ContentNetworkManagerFactory;
import com.aelitis.azureus.core.content.AzureusPlatformContentDirectory;
import com.aelitis.azureus.core.devices.DeviceManagerFactory;
import com.aelitis.azureus.core.download.DownloadManagerEnhancer;
import com.aelitis.azureus.core.metasearch.MetaSearchManagerFactory;
import com.aelitis.azureus.core.peer.cache.CacheDiscovery;
import com.aelitis.azureus.core.subs.Subscription;
import com.aelitis.azureus.core.subs.SubscriptionManagerFactory;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.core.util.AZ3Functions;
import com.aelitis.azureus.ui.swt.views.skin.TorrentListViewsUtils;

import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.download.DownloadManagerStateAttributeListener;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.download.DownloadWillBeAddedListener;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;

public class InitialisationFunctions
{
	private static final String EXTENSION_PREFIX = "azid";

	public static void earlyInitialisation(AzureusCore core) {
		
		DownloadUtils.initialise( core );
		
		DownloadManagerEnhancer dme = DownloadManagerEnhancer.initialise(core);

		registerTrackerURLExtensions(core);

		AzureusPlatformContentDirectory.register();

		CacheDiscovery.initialise( dme );
		
		ContentNetworkManagerFactory.preInitialise();
		
		MetaSearchManagerFactory.preInitialise();
		
		SubscriptionManagerFactory.preInitialise();
		
		DeviceManagerFactory.preInitialise();
		
		NavigationHelper.initialise();
		
		AZ3Functions.setProvider(
			new AZ3Functions.provider()
			{
				public void 
				subscribeToRSS(
					String		name,
					URL 		url,
					int			interval,
					boolean		is_public,
					String		creator_ref )
				
					throws Exception
				{
					Subscription subs =
						SubscriptionManagerFactory.getSingleton().createSingletonRSS(
						name, url, interval );
					
					if ( subs.isPublic() != is_public ){
						
						subs.setPublic( is_public );
					}
					
					if ( creator_ref != null ){
						
						subs.setCreatorRef( creator_ref );
					}
				}
				
				public boolean 
				canShowCDP(
					org.gudy.azureus2.core3.download.DownloadManager dm ) 
				{
					return( TorrentListViewsUtils.canViewDetails( dm ));
				}
				
				public void 
				showCDP(
					org.gudy.azureus2.core3.download.DownloadManager	dm,
					String 												ref )
				{
					TorrentListViewsUtils.viewDetails( dm, ref );
				}
				
				public String 
				getCDPURL(
					org.gudy.azureus2.core3.download.DownloadManager 	dm ) 
				{
					return( TorrentListViewsUtils.getDetailsURL( dm ));
				}
			});
	}

	public static void 
	lateInitialisation(
		AzureusCore core ) 
	{
		ExternalStimulusHandler.initialise(core);
		
		core.getPluginManager().getDefaultPluginInterface().getUtilities().createDelayedTask(
			new Runnable()
			{
				public void 
				run() 
				{
					MetaSearchManagerFactory.getSingleton();
					
					SubscriptionManagerFactory.getSingleton();
				}
			}).queue();
	}

	protected static void 
	registerTrackerURLExtensions(
		AzureusCore core ) 
	{
		PluginInterface pi = core.getPluginManager().getDefaultPluginInterface();

		DownloadManager	dm = pi.getDownloadManager();
		
			// need to get in early to ensure property present on initial announce
		
		dm.addDownloadWillBeAddedListener(
			new DownloadWillBeAddedListener()
			{
				public void
				initialised(
					Download 	download )
				{
					register( download );
				}
			});
		
		dm.addListener(
			new DownloadManagerListener() 
			{
				public void 
				downloadAdded(
					Download download )
				{
					register( download );
				}

				public void downloadRemoved(Download download) {
				}
			});
	}

	protected static void
	register(
		final Download	download )
	{
			// only add the azid to platform content

		DownloadManagerStateAttributeListener dmsal = new DownloadManagerStateAttributeListener() {
			public void attributeEventOccurred(org.gudy.azureus2.core3.download.DownloadManager dm, String attribute_name, int event_type) {
				try{							
					Torrent t = download.getTorrent();
					if (t == null) {return;}
					if (!PlatformTorrentUtils.isContent(t, true)) {return;}
					DownloadUtils.addTrackerExtension(download, EXTENSION_PREFIX, ConstantsV3.AZID);	
					
					// allow the tracker to manipulate peer sources for dead/unauthorised torrents
					download.setFlag(Download.FLAG_ALLOW_PERMITTED_PEER_SOURCE_CHANGES, true);
				}
				finally {
					dm.getDownloadState().removeListener(this, DownloadManagerState.AT_TRACKER_CLIENT_EXTENSIONS, DownloadManagerStateAttributeListener.WILL_BE_READ);
				}
			}
		};
		
		PluginCoreUtils.unwrap( download ).getDownloadState().addListener(dmsal, DownloadManagerState.AT_TRACKER_CLIENT_EXTENSIONS, DownloadManagerStateAttributeListener.WILL_BE_READ);				

	}
}
