/*
 * Created on Feb 5, 2009
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

import java.util.*;

import org.gudy.azureus2.plugins.PluginInterface;

import com.aelitis.azureus.core.devices.TranscodeProfile;
import com.aelitis.azureus.core.devices.TranscodeProvider;

public class 
TranscodeProviderVuze 
	implements TranscodeProvider
{
	private PluginInterface		plugin_interface;
	
	private volatile TranscodeProfile[]	profiles;
	
	protected
	TranscodeProviderVuze(
		PluginInterface			pi )
	{
		plugin_interface		= pi;
	}
	
	protected void
	update(
		PluginInterface		pi )
	{
		plugin_interface		= pi;
	}
	
	public String
	getName()
	{
		return( plugin_interface.getPluginName() + ": version=" + plugin_interface.getPluginVersion());
	}
	
	public TranscodeProfile[]
	getProfiles()
	{			
		if ( profiles != null ){
				
			return( profiles );
		}
		
		try{
			Map<String, Map<String,String>> profiles_map = (Map<String,Map<String,String>>)plugin_interface.getIPC().invoke( "getProfiles", new Object[]{} );
			
			TranscodeProfile[] res = new TranscodeProfile[profiles_map.size()];
			
			int	index = 0;
			
			for ( Map.Entry<String, Map<String,String>> entry : profiles_map.entrySet()){
				
				res[ index++] = new TranscodeProfileImpl( this, "vuzexcode:" + entry.getKey(), entry.getKey(), entry.getValue());
			}
			
			profiles	= res;
			
			return( res );
			
		}catch( Throwable e ){
			
			e.printStackTrace();
			
			return( new TranscodeProfile[0] );
		}
	}
	
	public TranscodeProfile
	getProfile(
		String		UID )
	{
		TranscodeProfile[] profiles = getProfiles();
		
		for ( TranscodeProfile profile: profiles ){
			
			if ( profile.getUID().equals( UID )){
				
				return( profile );
			}
		}
		
		return( null );
	}
	
	protected void
	destroy()
	{
	}
}
