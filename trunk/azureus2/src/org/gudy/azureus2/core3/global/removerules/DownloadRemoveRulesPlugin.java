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
					
					// System.out.println( "scrape status = " + response.getStatusString());
				}
				
				public void
				announceResult(
					DownloadAnnounceResult			response )
				{
					if ( closing ){
						
						return;
					}
					
					if ( response.getResponseType() == DownloadAnnounceResult.RT_ERROR ){
						
						String	reason = response.getError();
						
						if ( reason != null ){
							
							reason = reason.toLowerCase();
							
							if ( 	reason.indexOf( "not authori" ) != -1 ||
									reason.toLowerCase().indexOf( "unauthori" ) != -1 ){
					
								handleUnauthorised( dm );
							}
						}
					}
				}
			};
			
		dm_listener_map.put( dm, listener );
		
		dm.addTrackerListener( listener );
	}
		
	protected void
	handleUnauthorised(
		final Download	dm )
	{
		if ( !remove_unauthorised.getValue()){
			
			return;
		}
		
		if ( remove_unauthorised_seeding_only.getValue() && dm.getState() != Download.ST_SEEDING ){
			
			return;
		}
		
		log.log( "Download '" + dm.getName() + "' is unauthorised and removal triggered" );
		
		dm.addListener( 
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
							dm.remove();
							
							String msg = 
								plugin_interface.getUtilities().getLocaleUtilities().getLocalisedMessageText(
									"download.removerules.removed.ok",
									new String[]{ dm.getName() });
								
							log.logAlert( 
								LoggerChannel.LT_INFORMATION,
								msg );
						
						}catch( Throwable e ){
							
							log.logAlert( "Automatic removal of download '" + dm.getName() + "' failed", e );
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
			dm.stop();
			
		}catch( DownloadException e ){
			
			log.log( "Removal failed", e );
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
