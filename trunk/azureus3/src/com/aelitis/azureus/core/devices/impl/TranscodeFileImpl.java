/*
 * Created on Feb 18, 2009
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


/**
 * 
 */
package com.aelitis.azureus.core.devices.impl;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.devices.TranscodeException;
import com.aelitis.azureus.core.devices.TranscodeFile;
import com.aelitis.azureus.util.ImportExportUtils;

class
TranscodeFileImpl
	implements TranscodeFile
{
	private DeviceImpl					device;
	private String						key;
	private Map<String,Map<String,?>>	files_map;
	
		// don't store any local state here, store it in the map as this is just a wrapper
		// for the underlying map and there can be multiple such wrappers concurrent
	
	protected 
	TranscodeFileImpl(
		DeviceImpl					_device,
		String						_key,
		Map<String,Map<String,?>>	_files_map,
		File						_file )
	{
		device		= _device;
		key			= _key;
		files_map	= _files_map;

		setString( "file", _file.getAbsolutePath());
	}
	
	protected
	TranscodeFileImpl(
		DeviceImpl					_device,
		String						_key,
		Map<String,Map<String,?>>	_map )
	{
		device			= _device;
		key				= _key;
		files_map		= _map;
	}
	
	
	protected String
	getKey()
	{
		return( key );
	}
	
	public File 
	getFile() 
	{
		return(new File(getString("file")));
	}
		
	protected void
	setComplete(
		boolean b )
	{
		setLong( "comp", b?1:0 );
	}
	
	public boolean
	isComplete()
	{
		return( getLong( "comp" ) == 1 );
	}
	
	public void
	delete(
		boolean	delete_contents )
	
		throws TranscodeException 
	{
		device.deleteFile( this, delete_contents );
	}
	
	private Map<String,?>
	getMap()
	{		
		synchronized( files_map ){
	
			Map<String,?> map = files_map.get( key );
			
			if ( map == null ){
				
				map = new HashMap<String, Object>();
				
				files_map.put( key, map );
			}
			
			return( map );
		}
	}
	
	protected long
	getLong(
		String		key )
	{
		try{
			Map<String,?>	map = getMap();
			
			return(ImportExportUtils.importLong( map, key, 0 ));
			
		}catch( Throwable e ){
			
			Debug.out( e );
			
			return( 0 );
		}
	}
	
	protected void
	setLong(
		String		key,
		long		value )
	{	
		synchronized( files_map ){

			try{
				Map<String,?>	map = getMap();

				ImportExportUtils.exportLong( map, key, value);
				
				device.fileDirty( this );
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
	}
	
	protected String
	getString(
		String		key )
	{
		try{
			Map<String,?>	map = getMap();

			return(ImportExportUtils.importString( map, key ));
			
		}catch( Throwable e ){
			
			Debug.out( e );
			
			return( "" );
		}
	}
	
	protected void
	setString(
		String		key,
		String		value )
	{
		synchronized( files_map ){
			
			Map<String,?>	map = getMap();
			
			try{
				ImportExportUtils.exportString( map, key, value );
				
				device.fileDirty( this );
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
	}
}