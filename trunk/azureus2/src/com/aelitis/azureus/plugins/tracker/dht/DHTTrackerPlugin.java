/*
 * Created on 31-Jan-2005
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

package com.aelitis.azureus.plugins.tracker.dht;

import java.util.*;

import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadListener;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.config.ActionParameter;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.config.StringParameter;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.plugins.utils.UTTimerEvent;
import org.gudy.azureus2.plugins.utils.UTTimerEventPerformer;

import com.aelitis.azureus.plugins.dht.DHTPlugin;

/**
 * @author parg
 *
 */

public class 
DHTTrackerPlugin 
	implements Plugin, DownloadListener
{
	private PluginInterface		plugin_interface;
	
	private DHTPlugin			dht;
	
	private Set					running_downloads 		= new HashSet();
	private Set					registered_downloads 	= new HashSet();
	
	private LoggerChannel		log;
	
	public void
	initialize(
		PluginInterface 	_plugin_interface )
	{
		plugin_interface	= _plugin_interface;
				
		plugin_interface.getPluginProperties().setProperty( "plugin.name", "DHT Tracker" );

		log = plugin_interface.getLogger().getTimeStampedChannel("DHT Tracker");

		UIManager	ui_manager = plugin_interface.getUIManager();

		final BasicPluginViewModel model = 
			ui_manager.createBasicPluginViewModel( "DHT Tracker");
		
		BasicPluginConfigModel	config = 
			ui_manager.createBasicPluginConfigModel( "Plugins", "DHT Tracker" );
			
		model.getActivity().setVisible( false );
		model.getProgress().setVisible( false );
		
		log.addListener(
				new LoggerChannelListener()
				{
					public void
					messageLogged(
						int		type,
						String	message )
					{
						model.getLogArea().appendText( message+"\n");
					}
					
					public void
					messageLogged(
						String		str,
						Throwable	error )
					{
						model.getLogArea().appendText( error.toString()+"\n");
					}
				});

		plugin_interface.addListener(
			new PluginListener()
			{
				public void
				initializationComplete()
				{
					PluginInterface pi = 
						plugin_interface.getPluginManager().getPluginInterfaceByClass(
									DHTPlugin.class );
					
					if ( pi != null ){
						
						dht = (DHTPlugin)pi.getPlugin();
						
						if ( dht != null && dht.isEnabled()){
							
							initialise();
						}
					}
				}
				
				public void
				closedownInitiated()
				{
					
				}
				
				public void
				closedownComplete()
				{
					
				}
			});
	}
	
	protected void
	initialise()
	{
		final TorrentAttribute ta = plugin_interface.getTorrentManager().getAttribute( TorrentAttribute.TA_NETWORKS );
	
		plugin_interface.getDownloadManager().addListener(
				new DownloadManagerListener()
				{
					public void
					downloadAdded(
						Download	download )
					{
						String[]	networks = download.getListAttribute( ta );
						
						for (int i=0;i<networks.length;i++){
							
							if ( networks[i].equalsIgnoreCase( "Public" )){
						
								if ( download.getTorrent() != null ){
								
									registerDownload( download );
								}
							}
						}
					}
					
					public void
					downloadRemoved(
						Download	download )
					{
						unregisterDownload( download );
					}
				});
		
		plugin_interface.getUtilities().createTimer("DHT Tracker").addPeriodicEvent(
			15000,
			new UTTimerEventPerformer()
			{
				public void 
				perform(
					UTTimerEvent event) 
				{
					processRegistrations();
				}
			});
	}
	
	protected void
	processRegistrations()
	{
		Iterator	it = running_downloads.iterator();
		
		while( it.hasNext()){
			
			Download	dl = (Download)it.next();
			
			if ( !registered_downloads.contains( dl )){
				
				
				log.log( "Registering download '" + dl.getName() + "'" );
				
				registered_downloads.add( dl );
				
				dht.put( dl.getTorrent().getHash(), "wibble".getBytes());
			}
		}
		
		it = registered_downloads.iterator();
		
		while( it.hasNext()){
			
			Download	dl = (Download)it.next();
			
			if ( !running_downloads.contains( dl )){
				
				log.log( "Unregistering download '" + dl.getName() + "'" );
				
				it.remove();
				
				dht.remove( dl.getTorrent().getHash());
			}
		}
	}
	
	protected void
	registerDownload(
		Download	download )
	{
		log.log( "Tracking starts for download ' " + download.getName() + "'" );
		
		download.addListener( this );
	}
	
	protected void
	unregisterDownload(
		Download	download )
	{
		log.log( "Tracking stops for download ' " + download.getName() + "'" );

		download.removeListener( this );
		
		running_downloads.remove( download );
	}
	
	public void
	stateChanged(
		Download		download,
		int				old_state,
		int				new_state )
	{
		int	state = download.getState();
		
		if ( 	state == Download.ST_DOWNLOADING ||
				state == Download.ST_SEEDING ||
				state == Download.ST_QUEUED ){
			
			running_downloads.add( download );
			
		}else{
			
			running_downloads.remove( download );
		}
	}
 
	public void
	positionChanged(
		Download		download, 
		int 			oldPosition,
		int 			newPosition )
	{
		
	}
}
