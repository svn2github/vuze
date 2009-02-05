/*
 * Created on Feb 4, 2009
 * Created by Paul Gardner
 * 
 * Copyright 2009 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.core.devices.impl;

import org.gudy.azureus2.plugins.PluginEvent;
import org.gudy.azureus2.plugins.PluginEventListener;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.devices.*;

public class 
TranscodeManagerImpl
	implements TranscodeManager
{
	private DeviceManagerImpl		device_manager;
	
	protected
	TranscodeManagerImpl(
		DeviceManagerImpl		_dm )
	{
		device_manager	= _dm;
		
		final PluginManager pm = AzureusCoreFactory.getSingleton().getPluginManager();
		
		final PluginInterface default_pi = pm.getDefaultPluginInterface();
		
		default_pi.addListener(
			new PluginListener()
			{
				public void
				initializationComplete()
				{
					default_pi.addEventListener(
						new PluginEventListener()
						{
							public void 
							handleEvent(
								PluginEvent ev )
							{
								int	type = ev.getType();
								
								if ( type == PluginEvent.PEV_PLUGIN_OPERATIONAL ){
									
									pluginAdded((PluginInterface)ev.getValue());
								}
								if ( type == PluginEvent.PEV_PLUGIN_NOT_OPERATIONAL ){
									
									pluginRemoved((PluginInterface)ev.getValue());
								}
							}
						});
					
					PluginInterface[] plugins = pm.getPlugins();
					
					for ( PluginInterface pi: plugins ){
						
						if ( pi.getPluginState().isOperational()){
						
							pluginAdded( pi );
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
	pluginAdded(
		PluginInterface		pi )
	{
		if ( pi.getPluginState().isBuiltIn()){
			
			return;
		}
		
		System.out.println( "plugin added: " + pi.getPlugin());
	}
	
	protected void
	pluginRemoved(
		PluginInterface		pi )
	{
		System.out.println( "plugin removed: " + pi.getPlugin());
	}
	
	public TranscodeProvider[]
	getProviders()
	{
		return( new TranscodeProvider[0] );
	}
	
	protected TranscodeProfile
	getProfileFromUID(
		String		uid )
	{
		return( null );
	}
	
	public TranscodeJob
	queue(
		TranscodeTarget			target,
		TranscodeProfile		profile,
		DiskManagerFileInfo		file )
	{
		return( null );
	}
}
