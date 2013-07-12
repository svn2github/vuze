/*
 * Created on Jul 9, 2013
 * Created by Paul Gardner
 * 
 * Copyright 2013 Azureus Software, Inc.  All rights reserved.
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


package org.gudy.azureus2.pluginsimpl.local.download;

import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.download.DownloadStub;

import com.aelitis.azureus.util.MapUtils;

public class 
DownloadStubImpl
	implements DownloadStub
{
	private final DownloadManagerImpl		manager;
	private final String					name;
	private final byte[]					hash;
	private final DownloadStubFile[]		files;
	private final Map						gm_map;
	
	protected
	DownloadStubImpl(
		DownloadManagerImpl		_manager,
		String					_name,
		byte[]					_hash,
		DownloadStubFile[]		_files,
		Map						_gm_map )
	{
		manager		= _manager;
		name		= _name;
		hash		= _hash;
		files		= _files;
		gm_map		= _gm_map;
	}
	
	protected
	DownloadStubImpl(
		DownloadManagerImpl		_manager,
		Map						_map )
	{
		manager		= _manager;
		
		hash = (byte[])_map.get( "hash" );
		
		name	= MapUtils.getMapString( _map, "name", null );
		
		files = null;
		
		gm_map = (Map)_map.get( "gm" );
	}
	
	public Map
	exportToMap()
	{
		Map	map = new HashMap();
		
		map.put( "hash", hash );
		
		MapUtils.setMapString(map, "name", name );
		
		map.put( "gm", gm_map );
		
		return( map );
	}
	
	public boolean
	isStub()
	{
		return( true );
	}
	
	public Download
	destubbify()
	
		throws DownloadException
	{
		return( manager.destubbify( this ));
	}
	
	public String
	getName()
	{
		return( name );
	}
	
	public byte[]
	getTorrentHash()
	{
		return( hash );
	}
	
	public DownloadStubFile[]
	getStubFiles()
	{
		return( files );
	}
	
	public Map
	getGMMap()
	{
		return( gm_map );
	}
	
	public void
	remove()
	{
		manager.remove( this );
	}
}
