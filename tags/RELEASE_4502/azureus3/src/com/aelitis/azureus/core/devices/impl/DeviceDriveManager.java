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
		async_dispatcher.dispatch(new AERunnable() {
			public void runSupport() {
				File root = info.getLocation();

				Object prodID = info.getInfo("ProductID");
				if (prodID == null) {
					prodID = info.getInfo("Product Name");
				}
				String sProdID = (prodID instanceof String) ? ((String) prodID).trim()
						: "";

				Object vendor = info.getInfo("VendorID");
				if (vendor == null) {
					vendor = info.getInfo("Vendor Name");
				}
				String sVendor = (vendor instanceof String) ? ((String) vendor).trim()
						: "";

				if (sProdID.toLowerCase().contains("android")
						|| sVendor.toLowerCase().contains("motorola")
						|| sVendor.toLowerCase().contains("samsung")
						) {
					boolean hidden = false;
					String name = sVendor;
					if (name.length() > 0) {
						name += " ";
					}
					name += sProdID;
					if (sVendor.compareToIgnoreCase("motorola") == 0) {
						if (sProdID.equalsIgnoreCase("a855")) {
							name = "Droid";
						} else if (sProdID.equalsIgnoreCase("a955")) {
							name = "Droid 2";
						} else if (sProdID.equalsIgnoreCase("mb810")) {
							name = "Droid X";
						}
					} else if (sProdID.equalsIgnoreCase("sgh-t959")) {
						name = "Samsung Vibrant"; // non-card
					} else if (sProdID.toLowerCase().contains("sgh-t959")) {
						hidden = true;
					}

					String id = "android.";
					id += sProdID.replaceAll(" ", ".");
					if (sVendor.length() > 0) {
						id += "." + sVendor.replaceAll(" ", ".");
					}
					
					addDevice(name, id.toLowerCase(), root, new File(root, "videos"), hidden);
					return;
				}

				if (root.exists()) {

					File[] folders = root.listFiles();

					if (folders != null) {

						Set<String> names = new HashSet<String>();

						for (File file : folders) {

							names.add(file.getName().toLowerCase());
						}

						if (names.contains("psp") && names.contains("video")) {
							addDevice("PSP", "sony.PSP", root, new File(root, "VIDEO"), false);
						}
					}
				}
			}
		});
	}
	
	protected void addDevice(
			String target_name, 
			String target_classification,
			File root,
			File target_directory,
			boolean hidden)
	{
		
		DeviceImpl[] devices = manager.getDevices();
		
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
		
		DeviceMediaRendererManual	renderer = null;
		
		for ( DeviceTemplate template: templates ){
			
			if ( template.getClassification().equalsIgnoreCase( target_classification )){
				
				try{
					renderer = (DeviceMediaRendererManual)template.createInstance( target_name );

					break;
					
				}catch( Throwable e ){
					
					log( "Failed to add device", e );
				}
			}
		}
		
		if ( renderer == null ){
			
				// damn, the above doesn't work until devices is turned on...
			
			try{
				renderer = (DeviceMediaRendererManual)manager.createDevice( Device.DT_MEDIA_RENDERER, null, target_classification, target_name );
				
			}catch( Throwable e ){
				
				log( "Failed to add device", e );
			}
		}
		
		if ( renderer != null ){
			
			try{
				renderer.setAutoCopyToFolder( true );
				renderer.setHidden(hidden);
				
				mapDevice( renderer, root, target_directory );
				
				return;
				
			}catch( Throwable e ){
				
				log( "Failed to add device", e );
			}
		}
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
