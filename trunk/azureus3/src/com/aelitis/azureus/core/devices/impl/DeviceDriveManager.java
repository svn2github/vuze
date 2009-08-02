/*
 * Created on Jul 31, 2009
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

import java.io.*;
import java.util.*;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AsyncDispatcher;

import com.aelitis.azureus.core.devices.Device;
import com.aelitis.azureus.core.devices.DeviceTemplate;
import com.aelitis.azureus.core.drivedetector.DriveDetectedInfo;
import com.aelitis.azureus.core.drivedetector.DriveDetectedListener;
import com.aelitis.azureus.core.drivedetector.DriveDetectorFactory;

public class 
DeviceDriveManager 
	implements DriveDetectedListener
{
	private DeviceManagerImpl		manager;
	
	private Map<String,DeviceMediaRendererManual>	device_map = new HashMap<String, DeviceMediaRendererManual>();
	
	private AsyncDispatcher	async_dispatcher = new AsyncDispatcher();
	
	private boolean	listener_added;
	
	protected
	DeviceDriveManager(
		DeviceManagerImpl		_manager )
	{
		manager = _manager;
		
		if ( manager.getAutoSearch()){
			
			listener_added = true;
			
			DriveDetectorFactory.getDeviceDetector().addListener( this );
		}
	}
	
	protected void
	search()
	{
		async_dispatcher.dispatch(
			new AERunnable()
			{
				public void
				runSupport()
				{
					if ( listener_added ){
						
						return;
					}
					
					try{
							// this should synchronously first any discovered drives
						
						DriveDetectorFactory.getDeviceDetector().addListener( DeviceDriveManager.this );

					}finally{
						
						DriveDetectorFactory.getDeviceDetector().removeListener( DeviceDriveManager.this );
					}
				}
			});
	}
	
	public void 
	driveDetected(
		final DriveDetectedInfo info )
	{
		async_dispatcher.dispatch(
			new AERunnable()
			{
				public void
				runSupport()
				{
					File root = info.getLocation();
					
					if ( root.exists()){
						
						File[] folders = root.listFiles();
						
						if ( folders != null ){
							
							Set<String>	names = new HashSet<String>();
							
							for ( File file: folders ){
								
								names.add( file.getName().toLowerCase());
							}
							
							if ( names.contains( "psp" ) && names.contains( "video" )){
								
								DeviceImpl[] devices = manager.getDevices();
								
								String target_name				= "PSP";
								String target_classification 	= "sony.psp";
								
								File target_directory = new File( root,"VIDEO" );

								for ( DeviceImpl device: devices ){
									
									if ( device instanceof DeviceMediaRendererManual ){
									
										DeviceMediaRendererManual renderer = (DeviceMediaRendererManual)device;
										
										String classification = renderer.getClassification();
									
										if ( classification.equalsIgnoreCase( target_classification )){
																						
											mapDevice( renderer, root, target_directory );
											
											return;
										}
									}
								}
								
								DeviceTemplate[] templates = manager.getDeviceTemplates( Device.DT_MEDIA_RENDERER );
								
								for ( DeviceTemplate template: templates ){
									
									if ( template.getClassification().equalsIgnoreCase( target_classification )){
										
										try{
											DeviceMediaRendererManual renderer = (DeviceMediaRendererManual)template.createInstance( target_name );
											
											renderer.setAutoCopyToFolder( true );
											
											mapDevice( renderer, root, target_directory );
											
											return;
											
										}catch( Throwable e ){
											
											log( "Failed to add device", e );
										}
									}
								}
							}
						}
					}
				}
			});
	}
	
	public void 
	driveRemoved(
		final DriveDetectedInfo info )
	{
		async_dispatcher.dispatch(
			new AERunnable()
			{
				public void
				runSupport()
				{
					unMapDevice( info.getLocation());
				}
			});
	}
	
	protected void
	mapDevice(
		DeviceMediaRendererManual		renderer,
		File							root,
		File							copy_to )
	{
		DeviceMediaRendererManual	existing;
		
		synchronized( device_map ){
			
			existing = device_map.put( root.getAbsolutePath(), renderer );
		}
		
		if ( existing != null && existing != renderer ){
			
			log( "Unmapped " + existing.getName() + " from " + root );
			
			existing.setCopyToFolder( null );
		}
		
		log( "Mapped " + renderer.getName() + " to " + root );

		renderer.setCopyToFolder( copy_to );
		
		renderer.setLivenessDetectable( true );
		
		renderer.alive();
	}
	
	protected void
	unMapDevice(
		File							root )
	{
		DeviceMediaRendererManual existing;
		
		synchronized( device_map ){
			
			existing = device_map.remove( root.getAbsolutePath());
		}
		
		if ( existing != null ){
			
			log( "Unmapped " + existing.getName() + " from " + root );

			existing.setCopyToFolder( null );
			
			existing.dead();
		}
	}
	
	protected void
	log(
		String str )
	{
		manager.log( "DriveMan: " + str );
	}
	
	protected void
	log(
		String 		str,
		Throwable 	e )
	{
		manager.log( "DriveMan: " + str, e );
	}
}
