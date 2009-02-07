/*
 * Created on Jan 28, 2009
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.devices.*;
import com.aelitis.net.upnp.UPnPDevice;

public class 
DeviceMediaRendererImpl
	extends DeviceUPnPImpl
	implements DeviceMediaRenderer
{
	private static final String PP_REND_WORK_DIR		= "rend_work_dir";
	private static final String PP_REND_TRANS_PROF		= "rend_trans_prof";
	private static final String PP_REND_DEF_TRANS_PROF	= "rend_def_trans_prof";
	
	protected
	DeviceMediaRendererImpl(
		DeviceManagerImpl	_manager,
		UPnPDevice			_device )
	{
		super( _manager, _device, Device.DT_MEDIA_RENDERER );
	}
	
	protected
	DeviceMediaRendererImpl(
		DeviceManagerImpl	_manager,
		String				_name )
	{
		super( _manager, Device.DT_MEDIA_RENDERER, _name );
	}
	
	protected
	DeviceMediaRendererImpl(
		DeviceManagerImpl	_manager,
		String				_uuid,
		String				_name,
		boolean				_manual )
	{
		super( _manager, Device.DT_MEDIA_RENDERER, _uuid, _name, _manual );
	}
	
	protected
	DeviceMediaRendererImpl(
		DeviceManagerImpl	_manager,
		Map					_map )
	
		throws IOException
	{
		super(_manager, _map );
	}
	
	protected boolean
	updateFrom(
		DeviceImpl		_other )
	{
		if ( !super.updateFrom( _other )){
			
			return( false );
		}
		
		if ( !( _other instanceof DeviceMediaRendererImpl )){
			
			Debug.out( "Inconsistent" );
			
			return( false );
		}
		
		DeviceMediaRendererImpl other = (DeviceMediaRendererImpl)_other;
		
		return( true );
	}
	
	public Device
	getDevice()
	{
		return( this );
	}
	
	public File
	getWorkingDirectory()
	{
		return( new File( getPersistentStringProperty( PP_REND_WORK_DIR )));
	}
	
	public void
	setWorkingDirectory(
		File		directory )
	{
		setPersistentStringProperty( PP_REND_WORK_DIR, directory.getAbsolutePath());
	}
	
	public TranscodeProfile[]
	getTranscodeProfiles()
	{
		String[] uids = getPersistentStringListProperty( PP_REND_TRANS_PROF );
		
		List<TranscodeProfile>	profiles = new ArrayList<TranscodeProfile>();
		
		DeviceManagerImpl dm = getManager();
		
		TranscodeManagerImpl tm = dm.getTranscodeManager();
		
			// hack for the moment!!!!
		
		TranscodeProvider[] providers = tm.getProviders();
		
		if ( providers.length > 0 ){
			
			return( providers[0].getProfiles());
		}
		
		for ( String uid: uids ){
			
			TranscodeProfile profile = tm.getProfileFromUID( uid );
			
			if ( profile != null ){
				
				profiles.add( profile );
			}
		}
		
		return( profiles.toArray( new TranscodeProfile[profiles.size()] ));
	}
	
	public void
	setTranscodeProfiles(
		TranscodeProfile[]	profiles )
	{
		String[]	uids = new String[profiles.length];
		
		for (int i=0;i<profiles.length;i++){
			
			uids[i] = profiles[i].getUID();
		}
		
		setPersistentStringListProperty( PP_REND_TRANS_PROF, uids );
	}
	
	public TranscodeProfile
	getDefaultTranscodeProfile()
	{
		String uid = getPersistentStringProperty( PP_REND_DEF_TRANS_PROF );
		
		DeviceManagerImpl dm = getManager();
		
		TranscodeManagerImpl tm = dm.getTranscodeManager();

		return( tm.getProfileFromUID( uid ));
	}
	
	public void
	setDefaultTranscodeProfile(
		TranscodeProfile		profile )
	{
		setPersistentStringProperty( PP_REND_DEF_TRANS_PROF, profile.getUID());
	}
	
	protected void
	getDisplayProperties(
		List<String[]>	dp )
	{
		super.getDisplayProperties( dp );

		addDP( dp, "devices.xcode.working_dir", getWorkingDirectory().getAbsolutePath());
		addDP( dp, "devices.xcode.prof_def", getDefaultTranscodeProfile());
		addDP( dp, "devices.xcode.profs", getTranscodeProfiles() );
	}	
	
	protected void
	addDP(
		List<String[]>		dp,
		String				name,
		TranscodeProfile	value )
	{
		addDP( dp, name, value==null?"":value.getName());
	}
	
	protected void
	addDP(
		List<String[]>		dp,
		String				name,
		TranscodeProfile[]	values )
	{
		String[]	names = new String[values.length];
		
		for (int i=0;i<values.length;i++){
			
			names[i] = values[i].getName();
		}
		
		addDP( dp, name, names);
	}
}
