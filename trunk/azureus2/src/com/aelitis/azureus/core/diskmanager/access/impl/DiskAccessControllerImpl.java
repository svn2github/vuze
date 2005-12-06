/*
 * Created on 02-Dec-2005
 * Created by Paul Gardner
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 40,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.diskmanager.access.impl;

import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.diskmanager.access.DiskAccessController;
import com.aelitis.azureus.core.diskmanager.access.DiskAccessRequest;
import com.aelitis.azureus.core.diskmanager.access.DiskAccessRequestListener;
import com.aelitis.azureus.core.diskmanager.cache.CacheFile;

public class 
DiskAccessControllerImpl
	implements DiskAccessController
{
	private	DiskAccessControllerInstance	read_dispatcher;
	private	DiskAccessControllerInstance	write_dispatcher;
	
	public
	DiskAccessControllerImpl(
		int		_max_read_threads,
		int		_max_read_mb,
		int 	_max_write_threads,
		int		_max_write_mb )
	{		
		read_dispatcher 	= new DiskAccessControllerInstance( "read", _max_read_threads, _max_read_mb );
		write_dispatcher 	= new DiskAccessControllerInstance( "write", _max_write_threads, _max_write_mb );
	}
	
	public DiskAccessRequest
	queueReadRequest(
		CacheFile					file,
		long						offset,
		DirectByteBuffer			buffer,
		boolean						flush,
		DiskAccessRequestListener	listener )
	{
		DiskAccessRequestImpl	request = 
			new DiskAccessRequestImpl( 
					file, 
					offset, 
					buffer, 
					listener, 
					flush?DiskAccessRequestImpl.OP_READ_AND_FLUSH:DiskAccessRequestImpl.OP_READ );

		read_dispatcher.queueRequest( request );
		
		return( request );
	}
	
	public DiskAccessRequest
	queueWriteRequest(
		CacheFile					file,
		long						offset,
		DirectByteBuffer			buffer,
		boolean						free_buffer,
		DiskAccessRequestListener	listener )
	{
		// System.out.println( "write request: " + offset );
		
		DiskAccessRequestImpl	request = 
			new DiskAccessRequestImpl( 
					file, 
					offset, 
					buffer, 
					listener, 
					free_buffer?DiskAccessRequestImpl.OP_WRITE_AND_FREE:DiskAccessRequestImpl.OP_WRITE );
	
		write_dispatcher.queueRequest( request );
		
		return( request );	
	}
}
