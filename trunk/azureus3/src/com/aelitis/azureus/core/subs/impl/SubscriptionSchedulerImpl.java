/*
 * Created on Aug 6, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.core.subs.impl;

import java.io.InputStream;
import java.net.URL;
import java.util.*;

import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.utils.StaticUtilities;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderFactory;
import org.gudy.azureus2.pluginsimpl.local.torrent.TorrentImpl;

import com.aelitis.azureus.core.subs.Subscription;
import com.aelitis.azureus.core.subs.SubscriptionException;
import com.aelitis.azureus.core.subs.SubscriptionManagerListener;
import com.aelitis.azureus.core.subs.SubscriptionResult;
import com.aelitis.azureus.core.subs.SubscriptionScheduler;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;

public class 
SubscriptionSchedulerImpl 
	implements SubscriptionScheduler, SubscriptionManagerListener
{
	private SubscriptionManagerImpl		manager;
	
	private Set	active_subscription_downloaders = new HashSet();
	private Set	active_result_downloaders		= new HashSet();
	
	private AsyncDispatcher	result_downloader = new AsyncDispatcher();

	protected
	SubscriptionSchedulerImpl(
		SubscriptionManagerImpl		_manager )
	{
		manager	= _manager;
		
		manager.addListener( this );
		
		calculateSchedule();
	}
	
	public void 
	download(
		Subscription subs )
	
		throws SubscriptionException 
	{
		SubscriptionDownloader downloader;
		
		synchronized( active_subscription_downloaders ){
			
			if ( active_subscription_downloaders.contains( subs )){
				
				return;
			}
	
			downloader = new SubscriptionDownloader(manager, (SubscriptionImpl)subs );
			
			active_subscription_downloaders.add( subs );
		}
		
		try{
			
			downloader.download();
			
		}finally{
			
			synchronized( active_subscription_downloaders ){

				active_subscription_downloaders.remove( subs );
			}
		}
	}
	
	public void
	download(
		final Subscription			subs,
		final SubscriptionResult	result )
	{
		final String dl = (String)result.toJSONMap().get( "dl" );
		
		if ( dl == null ){
			
			return;
		}

		final String	key = subs.getID() + ":" + result.getID();
		
		synchronized( active_result_downloaders ){

			if ( active_result_downloaders.contains( key )){
				
				return;
			}
		
			active_result_downloaders.add( key );
			
			result_downloader.dispatch(
				new AERunnable()
				{
					public void 
					runSupport() 
					{
						try{
							URL url = new URL(dl);
							
							ResourceDownloaderFactory rdf = StaticUtilities.getResourceDownloaderFactory();
							
							ResourceDownloader url_rd = rdf.create( url );
										
							UrlUtils.setBrowserHeaders( url_rd, subs.getReferer());
							
							/*if(cookieParameters!= null && cookieParameters.length > 0) {
								String 	cookieString = "";
								String separator = "";
								for(CookieParameter parameter : cookieParameters) {
									cookieString += separator + parameter.getName() + "=" + parameter.getValue();
									separator = "; ";
								}				
								url_rd.setProperty( "URL_Cookie", cookieString );
							}*/
							
							ResourceDownloader mr_rd = rdf.getMetaRefreshDownloader( url_rd );

							InputStream is = mr_rd.download();

							Torrent torrent = new TorrentImpl( TOTorrentFactory.deserialiseFromBEncodedInputStream( is ));
												
							// PlatformTorrentUtils.setContentTitle(torrent, torr );
					
							Download download = StaticUtilities.getDefaultPluginInterface().getDownloadManager().addDownload( torrent );
							
							subs.addAssociation( torrent.getHash());
							
							result.setRead( true );
							
							manager.log( subs.getName() + ": added download " + download.getName());
							
						}catch( Throwable e ){
							
							manager.log( subs.getName() + ": Failed to download result " + dl, e );
							
						}finally{
							
							active_result_downloaders.remove( key );
						}
					}
				});
		}
	}
	
	protected void
	calculateSchedule()
	{
		
	}
	
	public void
	subscriptionAdded(
		Subscription		subscription )
	{
		calculateSchedule();
	}
	
	public void
	subscriptionChanged(
		Subscription		subscription )
	{
		calculateSchedule();
	}
	
	public void
	subscriptionRemoved(
		Subscription		subscription )
	{
		calculateSchedule();
	}
	
	public void
	associationsChanged(
		byte[]				association_hash )
	{
	}
}
