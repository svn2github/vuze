/*
 * Created on Feb 6, 2009
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

import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;

import com.aelitis.azureus.core.devices.TranscodeJob;
import com.aelitis.azureus.core.devices.TranscodeProfile;
import com.aelitis.azureus.core.devices.TranscodeTarget;

public class 
TranscodeJobImpl 
	implements TranscodeJob
{
	private TranscodeQueueImpl		queue;
	private TranscodeTarget			target;
	private TranscodeProfile		profile;
	private DiskManagerFileInfo		file;
	
	private int						state 				= ST_QUEUED;
	private int						percent_complete	= 0;
	
	protected
	TranscodeJobImpl(
		TranscodeQueueImpl		_queue,
		TranscodeTarget			_target,
		TranscodeProfile		_profile,
		DiskManagerFileInfo		_file )
	{
		queue		= _queue;
		target		= _target;
		profile		= _profile;
		file		= _file;
	}
	
	public String
	getName()
	{
		try{
			Download download = file.getDownload();
		
			if ( download.getDiskManagerFileInfo().length == 1 ){
				
				return( download.getName());
			}
			
			return( download.getName() + ": " + file.getFile().getName());
			
		}catch( Throwable e ){
			
			return( file.getFile().getName());
		}
	}
	public TranscodeTarget
	getTarget()
	{
		return( target );
	}
	
	public TranscodeProfile
	getProfile()
	{
		return( profile );
	}
	
	public DiskManagerFileInfo
	getFile()
	{
		return( file );
	}
	
	public int
	getState()
	{
		return( state );
	}
	
	public int
	getPercentComplete()
	{
		return( percent_complete );
	}
	
	public void
	pause()
	{
		
	}
	
	public void
	resume()
	{
		
	}
	
	public void
	remove()
	{
		queue.remove( this );
	}
}
