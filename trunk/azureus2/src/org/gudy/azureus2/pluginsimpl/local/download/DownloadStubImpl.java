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

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.download.DownloadStub;

public class 
DownloadStubImpl
	implements DownloadStub
{
	private final DownloadManagerImpl		manager;
	private final String					name;
	private final byte[]					hash;
	private final DownloadStubFile[]		files;
	
	protected
	DownloadStubImpl(
		DownloadManagerImpl		_manager,
		String					_name,
		byte[]					_hash,
		DownloadStubFile[]		_files )
	{
		manager		= _manager;
		name		= _name;
		hash		= _hash;
		files		= _files;
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
}
