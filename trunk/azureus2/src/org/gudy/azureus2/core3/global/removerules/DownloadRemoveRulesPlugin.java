/*
 * Created on 06-Jul-2004
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

package org.gudy.azureus2.core3.global.removerules;

/**
 * @author parg
 *
 */

import java.util.*;

import org.gudy.azureus2.plugins.torrent.*;
import org.gudy.azureus2.plugins.download.*;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.ui.config.*;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.*;

public class 
DownloadRemoveRulesPlugin 
	implements Plugin, DownloadManagerListener
{
	protected PluginInterface		plugin_interface;
	protected boolean				closing;
	
	protected Map		dm_listener_map	= new HashMap(10);
	
	protected LoggerChannel 		log;

	protected BooleanParameter 	remove_unauthorised; 
	protected BooleanParameter 	remove_unauthorised_seeding_only; 
	
	protected BooleanParameter 	remove_update_torrents; 

	
	public void
	initialize(
		PluginInterface 	_plugin_interface )
	{
		plugin_interface	= _plugin_interface;
		
		plugin_interface.getPluginProperties().setProperty( "plugin.name", "Download Remove Rules" );

		log = plugin_interface.getLogger().getChannel("DLRemRules");

		BasicPluginConfigModel	config = plugin_interface.getUIManager().createBasicPluginConfigModel( "torrents", "download.removerules.name" );
			
		config.addLabelParameter2( "download.removerules.unauthorised.info" );
		
		remove_unauthorised = 
			config.addBooleanParameter2( "download.removerules.unauthorised", "download.removerules.unauthorised", false );
		
		remove_unauthorised_seeding_only = 
			config.addBooleanParameter2( "download.removerules.unauthorised.seedingonly", "download.removerules.unauthorised.seedingonly", true );
		
		remove_unauthorised.addEnabledOnSelection( remove_unauthorised_seeding_only );

		remove_update_torrents = 
			config.addBooleanParameter2( "download.removerules.updatetorrents", "download.removerules.updatetorrents", true );

		plugin_interface.getDownloadManager().addListener( this );
	}
	

	public void
	downloadAdded(
		final Download	dm )
	{
		DownloadTrackerListener	listener = 
			new DownloadTrackerListener()
			{
				public void
				scrapeResult(
					DownloadScrapeResult	response )
				{
					if ( closing ){
						
						return;
					}

					handleScrape( dm, response );
				}
				
				public void
				announceResult(
					DownloadAnnounceResult			response )
				{
					if ( closing ){
						
						return;
					}
					
					handleAnnounce( dm, response );
				}
			};
			
		dm_listener_map.put( dm, listener );
		
		dm.addTrackerListener( listener );
	}
		
	protected void
	handleScrape(
		Download				download,
		DownloadScrapeResult	response )
	{
		String	status = response.getStatus();
		
		if ( status == null ){
			
			status = "";
		}
			
		handleAnnounceScrapeStatus( download, status );
	}
	
	protected void
	handleAnnounce(
		Download				download,
		DownloadAnnounceResult	response )
	{
		String	reason = "";
		
		if ( response.getResponseType() == DownloadAnnounceResult.RT_ERROR ){
			
			reason = response.getError();
		}
				
		handleAnnounceScrapeStatus( download, reason );
	}
	
	protected void
	handleAnnounceScrapeStatus(
		Download		download,
		String			status )
	{
		status = status.toLowerCase();
		
		boolean	download_completed = download.isComplete();
		
		if ( 	status.indexOf( "not authori" ) != -1 ||
				status.toLowerCase().indexOf( "unauthori" ) != -1 ){
	
			if ( !remove_unauthorised.getValue()){
				
				return;
			}
			
			if ( remove_unauthorised_seeding_only.getValue() && !download_completed ){
				
				return;
			}
			
			log.log( "Download '" + download.getName() + "' is unauthorised and removal triggered" );
			
			removeDownload( download );
			
		}else{
			
			Torrent	torrent = download.getTorrent();
			
			if ( torrent != null && torrent.getAnnounceURL() != null ){
			
				if ( torrent.getAnnounceURL().toString().toLowerCase().indexOf( "aelitis.com") != -1 ){
		
						// emergency instruction from tracker
					
					if ( 	( download_completed && status.indexOf( "too many seeds" ) != -1 ) ||
							status.indexOf( "too many peers" ) != -1 ){
			
						removeDownloadDelayed( download );
						
					}else if ( download_completed && remove_update_torrents.getValue()){
						
						long	creation_time	= download.getCreationTime();
						
						long	seeds	= download.getLastScrapeResult().getSeedCount();
												
							// try to maintain an upper bound on seeds that isn't going to
							// kill the tracker
						
						long	running_hours = ( System.currentTimeMillis() - creation_time )/(60*60*1000);
						
						if ( seeds > 20000 && running_hours > 0 ){
							
							removeDownloadDelayed( download );							
						}
					}
				}
			}
		}
	}
	
	protected void
	removeDownloadDelayed(
		final Download		download )
	{
			// we need to delay this because other actions may be being performed
			// on the download (e.g. completion may trigger update install)
		
		plugin_interface.getUtilities().createThread( 
			"delayedRemoval",
			new Runnable()
			{
				public void
				run()
				{
					try{
						Thread.sleep(30000);
						
						removeDownload( download );
						
					}catch( Throwable e ){
						
						e.printStackTrace();
					}
				}
			});
	}
	
	protected void
	removeDownload(
		final Download		download )
	{
		if ( download.getState() == Download.ST_STOPPED ){
			
			try{
				download.remove();
				
			}catch( Throwable e ){
				
				log.logAlert( "Automatic removal of download '" + download.getName() + "' failed", e );
			}
		}else{
			
			download.addListener( 
				new DownloadListener()
				{
					public void
					stateChanged(
						Download		download,
						int				old_state,
						int				new_state )
					{
						log.log( "download state changed to '" + new_state +"'" );
						
						if ( new_state == Download.ST_STOPPED ){
							
							try{
								download.remove();
								
								String msg = 
									plugin_interface.getUtilities().getLocaleUtilities().getLocalisedMessageText(
										"download.removerules.removed.ok",
										new String[]{ download.getName() });
									
								log.logAlert( 
									LoggerChannel.LT_INFORMATION,
									msg );
							
							}catch( Throwable e ){
								
								log.logAlert( "Automatic removal of download '" + download.getName() + "' failed", e );
							}
						}
					}
	
					public void
					positionChanged(
						Download	download, 
						int oldPosition,
						int newPosition )
					{
					}
				});
			
			try{
				download.stop();
				
			}catch( DownloadException e ){
				
				log.logAlert( "Automatic removal of download '" + download.getName() + "' failed", e );
			}
		}
	}
	
	public void
	downloadRemoved(
		Download	dm )
	{
		DownloadTrackerListener	listener = (DownloadTrackerListener)dm_listener_map.remove(dm);
		
		if ( listener != null ){
			
			dm.removeTrackerListener( listener );
		}
	}
		
	public void
	destroyInitiated()
	{
		closing	= true;
	}
		
	public void
	destroyed()
	{
	}
}
