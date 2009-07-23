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
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.IndentWriter;

import com.aelitis.azureus.core.devices.*;
import com.aelitis.net.upnp.UPnPDevice;

public class 
DeviceMediaRendererImpl
	extends DeviceUPnPImpl
	implements DeviceMediaRenderer
{
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
		String				_classification )
	{
		super( _manager, Device.DT_MEDIA_RENDERER, _classification );
	}
	
	protected
	DeviceMediaRendererImpl(
		DeviceManagerImpl	_manager,
		String				_classification,
		boolean				_manual,
		String				_name )
	{
		super( _manager, Device.DT_MEDIA_RENDERER, _classification, _manual, _name );
	}
	
	protected
	DeviceMediaRendererImpl(
		DeviceManagerImpl	_manager,
		String				_uuid,
		String				_classification,
		boolean				_manual )
	{
		super( _manager, Device.DT_MEDIA_RENDERER, _uuid, _classification, _manual );
	}
	
	protected
	DeviceMediaRendererImpl(
		DeviceManagerImpl	_manager,
		Map					_map )
	
		throws IOException
	{
		super(_manager, _map );
	}
	
	@Override
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
	
	@Override
	protected void
	initialise()
	{
		super.initialise();
	}
	
	@Override
	protected void
	destroy()
	{
		super.destroy();
	}
	
	public boolean
	canCopyToDevice()
	{
		return( false );
	}
	
	public int
	getCopyToDevicePending()
	{
		return( 0 );
	}
	
	public boolean
	canAutoStartDevice()
	{
		return( false );
	}
	
	public boolean
	getAutoStartDevice()
	{
		return( false );
	}
	
	public void
	setAutoStartDevice(
		boolean		auto )
	{
	}
	
	public boolean
	canCopyToFolder()
	{
		return( false );
	}
	
	public File
	getCopyToFolder()
	{
		return( null );
	}
	
	public void
	setCopyToFolder(
		File		file )
	{
	}
	
	public int
	getCopyToFolderPending()
	{
		return( 0 );
	}
	
	public boolean
	getAutoCopyToFolder()
	{
		return( false );
	}
		
	public void
	setAutoCopyToFolder(
		boolean		auto )
	{
	}
	
	public void 
	manualCopy() 
	
		throws DeviceManagerException 
	{
		throw( new DeviceManagerException( "Unsupported" ));
	}
	
	@Override
	protected void
	getDisplayProperties(
		List<String[]>	dp )
	{
		super.getDisplayProperties( dp );

		if ( canCopyToFolder()){
			
			addDP( dp, "devices.copy.folder.auto", getAutoCopyToFolder());
			addDP( dp, "devices.copy.folder.dest", getCopyToFolder());
		}
		
		super.getTTDisplayProperties( dp );
	}	
	
	@Override
	public void
	generate(
		IndentWriter		writer )
	{
		super.generate( writer );
		
		try{
			writer.indent();
	
			generateTT( writer );
			
		}finally{
			
			writer.exdent();
		}
	}
}
