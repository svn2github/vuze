/*
 * Created on 1 Nov 2006
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
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */


package com.aelitis.azureus.core.download;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManagerListener;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;

import com.aelitis.azureus.core.AzureusCore;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;

public class 
DownloadManagerEnhancer 
{
	public static final int	TICK_PERIOD	= 1000;
	
	private static DownloadManagerEnhancer		singleton;
	
	public static synchronized void
	initialise(
		AzureusCore		core )
	{
		singleton	= new DownloadManagerEnhancer( core );
	}
	
	public static synchronized DownloadManagerEnhancer
	getSingleton()
	{
		return( singleton );
	}
	
	private AzureusCore		core;
	
	private Map				download_map = new HashMap();
	
	private boolean			progressive_enabled;
	
	protected
	DownloadManagerEnhancer(
		AzureusCore	_core )
	{
		core	= _core;
		
		core.getGlobalManager().addListener(
			new GlobalManagerListener()
			{
				public void
				downloadManagerAdded(
					DownloadManager	dm )
				{
					// Don't auto-add to download_map. getEnhancedDownload will
					// take care of it later if we ever need the download
				}
					
				public void
				downloadManagerRemoved(
					DownloadManager	dm )
				{
					synchronized( download_map ){
						
						download_map.remove( dm );
					}
				}
					
				public void
				destroyInitiated()
				{
				}
					
				public void
				destroyed()
				{
				}
			  
			    public void 
			    seedingStatusChanged( 
			    	boolean seeding_only_mode )
			    {
			    }
			});
		
		final PluginInterface	plugin_interface = core.getPluginManager().getDefaultPluginInterface();
		
		plugin_interface.addListener(
			new PluginListener()
			{
				public void
				initializationComplete()
				{
					PluginInterface	ms_pi = plugin_interface.getPluginManager().getPluginInterfaceByID("aeupnpmediaserver");
					
					if ( ms_pi != null ){
						
						progressive_enabled = true;
						
					}else{
						
						System.out.println( "No MediaServer plugin found" );
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
		
		SimpleTimer.addPeriodicEvent(
				"DownloadManagerEnhancer:speedChecker",
				TICK_PERIOD,
				new TimerEventPerformer()
				{
					private int tick_count;
					
					public void 
					perform(
						TimerEvent event ) 
					{
						tick_count++;
						
						List	downloads = core.getGlobalManager().getDownloadManagers();
						
						for ( int i=0;i<downloads.size();i++){
							
							DownloadManager download = (DownloadManager)downloads.get(i);
							
							if ( 	download.getState() == DownloadManager.STATE_DOWNLOADING ||
									download.getState() == DownloadManager.STATE_SEEDING ){
								
								getEnhancedDownload( download ).updateStats( tick_count );
							}
						}
					}
				});
	}
	
	public EnhancedDownloadManager
	getEnhancedDownload(
		DownloadManager	manager )
	{
		synchronized( download_map ){
			
			EnhancedDownloadManager	res = (EnhancedDownloadManager)download_map.get( manager );
			
			if ( res == null ){
				
				res = new EnhancedDownloadManager( DownloadManagerEnhancer.this, manager );
				
				download_map.put( manager, res );
			}
			
			return( res );
		}
	}
	
	protected boolean
	isProgressiveAvailable()
	{
		return( progressive_enabled );
	}
}
