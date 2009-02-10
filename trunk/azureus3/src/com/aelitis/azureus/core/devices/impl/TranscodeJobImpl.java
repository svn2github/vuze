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

import org.gudy.azureus2.core3.util.Debug;
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
	private String					error;
	
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
	
	protected void
	starts()
	{
		synchronized( this ){
		
			state = ST_RUNNING;
		}
		
		queue.jobChanged( this, false );
	}
	
	protected void
	failed(
		Throwable	e )
	{
		error = Debug.getNestedExceptionMessage( e );
		
		synchronized( this ){
			
			state = ST_FAILED;
		}
		
		queue.jobChanged( this, false );
	}
	
	protected void
	complete()
	{
		synchronized( this ){
		
			state = ST_COMPLETE;
		}
		
		queue.jobChanged( this, false );
	}
	
	protected void
	setPercentDone(
		int		_done )
	{
		if ( percent_complete != _done ){
		
			percent_complete	= _done;
		
			queue.jobChanged( this, false );
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
	getIndex()
	{
		return( queue.getIndex( this ));
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
	
	public String
	getError()
	{
		return( error );
	}
	
	public void
	pause()
	{
		synchronized( this ){
			
			if ( state == ST_RUNNING ){
		
				state = ST_PAUSED;
				
			}else{
				
				return;
			}
		}
		
		queue.jobChanged( this, false );
	}
	
	public void
	resume()
	{
		synchronized( this ){

			if ( state == ST_PAUSED ){
				
				state = ST_RUNNING;
				
			}else{
				
				return;
			}
		}
		
		queue.jobChanged( this, false );
	}
	
	public void
	queue()
	{
		boolean	do_resume;
	
		synchronized( this ){

			do_resume = state == ST_PAUSED;
		}
		
		if ( do_resume ){
			
			resume();
			
			return;
		}

		synchronized( this ){
			
			if ( state != ST_QUEUED ){
		
				if ( 	state == ST_RUNNING ||
						state == ST_PAUSED ){
					
					stop();
				}
				
				state = ST_QUEUED;
				
				error 				= null;
				percent_complete	= 0;
				
			}else{
				
				return;
			}
		}
		
		queue.jobChanged( this, true );
	}
	
	public void
	stop()
	{
		synchronized( this ){
			
			if ( state != ST_STOPPED ){
		
				state = ST_STOPPED;
				
			}else{
				
				return;
			}
		}
		
		queue.jobChanged( this, true );
	}
	
	public void
	remove()
	{
		queue.remove( this );
	}
	
	public void 
	moveUp() 
	{
		queue.moveUp( this );
	}
	
	public void 
	moveDown() 
	{
		queue.moveDown( this );
	}
}
