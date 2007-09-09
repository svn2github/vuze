/*
 * Created on Sep 9, 2007
 * Created by Paul Gardner
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */


package com.aelitis.azureus.core.download;

import java.util.Map;

import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;

public class 
EnhancedDownloadManagerFile 
{
	private DiskManagerFileInfo		file;
	private int						header_size;
	
	protected
	EnhancedDownloadManagerFile(
		DiskManagerFileInfo		_file,
		Map						_map )
	{
		file		= _file;
		
		if ( _map != null ){
			
			Long	l_header_size = (Long)_map.get( "header_size" );
			
			if ( l_header_size != null ){
				
				header_size = l_header_size.intValue();
			}
		}
	}
	
	public DiskManagerFileInfo
	getFile()
	{
		return( file );
	}
	
	public int
	getHeaderSize()
	{
		return( header_size );
	}
	
	public String
	getString()
	{
		return( file.getFile(true).getName()+ ",header=" + header_size );
	}
}
