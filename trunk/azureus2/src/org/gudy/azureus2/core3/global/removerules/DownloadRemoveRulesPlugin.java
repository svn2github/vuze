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
import org.gudy.azureus2.plugins.*;

public class 
DownloadRemoveRulesPlugin 
	implements Plugin, DownloadManagerListener
{
	protected boolean	closing;
	
	protected Map		dm_listener_map	= new HashMap(10);
	
	public void
	initialize(
		PluginInterface 	plugin_interface )
	{
		plugin_interface.getPluginProperties().setProperty( "plugin.name", "Download Remove Rules" );

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
						
						if ( reason != null && reason.toLowerCase().indexOf( "unauthori" ) != -1 ){
					
							handleUnauthorised( dm );
						}
					}
				}
			};
			
		dm_listener_map.put( dm, listener );
		
		dm.addTrackerListener( listener );
	}
		
	protected void
	handleUnauthorised(
		Download	dm )
	{
		// System.out.println( "Unauthorised: " + dm.getName());
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
