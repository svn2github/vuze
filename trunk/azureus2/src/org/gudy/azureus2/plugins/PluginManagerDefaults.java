/*
 * Created on 13-Jul-2004
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

package org.gudy.azureus2.plugins;

/**
 * @author parg
 *
 */

public interface 
PluginManagerDefaults 
{
			// if default plugins get removed then set their id to -1!
	
	public static final String	PID_START_STOP_RULES		= "Start/Stop Rules";
	public static final String	PID_REMOVE_RULES			= "Torrent Removal Rules";
	public static final String	PID_SHARE_HOSTER			= "Share Hoster";
	public static final String	PID_DEFAULT_TRACKER_WEB		= "Default Tracker Web";
	//public static final String	PID_UPDATE_LANGUAGE			= "Update Language";
	public static final String	PID_PLUGIN_UPDATE_CHECKER	= "Plugin Update Checker";
	public static final String	PID_CORE_UPDATE_CHECKER		= "Core Update Checker";
	public static final String	PID_CORE_PATCH_CHECKER		= "Core Patch Checker";
	public static final String	PID_PLATFORM_CHECKER		= "Platform Checker";
	public static final String	PID_UPNP					= "UPnP";
		
	public static final String[] PLUGIN_IDS = {
			
			PID_START_STOP_RULES,
			PID_REMOVE_RULES,
			PID_SHARE_HOSTER,
			PID_DEFAULT_TRACKER_WEB,
			//PID_UPDATE_LANGUAGE,
			PID_CORE_UPDATE_CHECKER,
			PID_CORE_PATCH_CHECKER,
			PID_PLATFORM_CHECKER,
			PID_UPNP,
		};
	
	public String[]
	getDefaultPlugins();
	
		/**
		 * by default all default plugins are enabled. This methods allows them to be disabled
		 * @param plugin_id
		 */
	
	public void
	setDefaultPluginEnabled(
		String		plugin_id,
		boolean		enabled );
		
	public boolean
	isDefaultPluginEnabled(
		String		plugin_id );
}
