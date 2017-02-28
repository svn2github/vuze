/*
 * Created on Oct 14, 2016
 * Created by Paul Gardner
 * 
 * Copyright 2016 Azureus Software, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or 
 * (at your option) any later version.
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


package com.aelitis.azureus.activities;

import java.util.*;

import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.ui.swt.UserAlerts;

import com.aelitis.azureus.core.util.AZ3Functions;

public class 
LocalActivityManager 
{
	private static List<VuzeActivitiesEntry>	pending = new ArrayList<VuzeActivitiesEntry>(1);
	
	static{
		VuzeActivitiesManager.addListener(
			new VuzeActivitiesLoadedListener() 
			{
				public void 
				vuzeActivitiesLoaded() 
				{					
					synchronized( LocalActivityManager.class ){
					
						for ( VuzeActivitiesEntry entry: pending ){
							
							VuzeActivitiesManager.addEntries(new VuzeActivitiesEntry[] {
									entry
								});
						}
						
						pending = null;
					}
				}
			});
		
		VuzeActivitiesManager.addListener(
			new VuzeActivitiesListener() {
				
				@Override
				public void vuzeNewsEntryChanged(VuzeActivitiesEntry entry) {
				}
				
				@Override
				public void vuzeNewsEntriesRemoved(VuzeActivitiesEntry[] entries) {
				}
				
				@Override
				public void vuzeNewsEntriesAdded(VuzeActivitiesEntry[] entries) {
					boolean	local_added = false;
					for ( VuzeActivitiesEntry entry: entries ){
						if ( entry.getTypeID().equals( VuzeActivitiesConstants.TYPEID_LOCALNEWS )){
							local_added = true;
						}
					}
					if ( local_added ){
						UserAlerts ua = UserAlerts.getSingleton();
						
						if ( ua != null ){
							ua.notificationAdded();
						}
					}
				}
			});
	}
	
	public static void
	addLocalActivity(
		String															uid,
		String															icon_id,
		String															name,
		String[]														actions,
		Class<? extends AZ3Functions.provider.LocalActivityCallback>	callback,
		Map<String,String>												callback_data )
	{
		VuzeActivitiesEntry entry = 
			new VuzeActivitiesEntry(
				SystemTime.getCurrentTime(), 
				name,
				VuzeActivitiesConstants.TYPEID_LOCALNEWS ) ;
		
		entry.setID( uid );
		
		entry.setIconIDRaw( icon_id );
		
		entry.setActions( actions );
		
		entry.setCallback( callback,  callback_data );
		
		synchronized( LocalActivityManager.class ){
			
			if ( pending != null ){
				
				pending.add( entry );
				
			}else{
		
				VuzeActivitiesManager.addEntries(new VuzeActivitiesEntry[] {
						entry
				});
			}
		}
	}
	
	public interface
	LocalActivityCallback
		extends AZ3Functions.provider.LocalActivityCallback
	{
	}
}
