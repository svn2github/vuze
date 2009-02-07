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

import java.util.*;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;

import com.aelitis.azureus.core.devices.*;
import com.aelitis.azureus.core.util.CopyOnWriteList;

public class 
TranscodeQueueImpl 	
	implements TranscodeQueue
{
	private List<TranscodeJobImpl>		queue = new ArrayList<TranscodeJobImpl>();
	
	private CopyOnWriteList<TranscodeQueueListener>	listeners = new CopyOnWriteList<TranscodeQueueListener>();
	
	
	protected
	TranscodeQueueImpl()
	{
		loadConfig();
	}
	
	public TranscodeJob
	add(
		TranscodeTarget			target,
		TranscodeProfile		profile,
		DiskManagerFileInfo		file )
	{
		TranscodeJobImpl job = new TranscodeJobImpl( this, target, profile, file );
		
		synchronized( this ){
			
			queue.add( job );
			
			saveConfig();
		}
		
		for ( TranscodeQueueListener listener: listeners ){
			
			try{
				listener.jobAdded( job );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}
		
		return( job );
	}
	
	protected void
	remove(
		TranscodeJobImpl		job )
	{
		synchronized( this ){
			
			if ( !queue.remove( job )){
				
				return;
			}
			
			saveConfig();
		}

		for ( TranscodeQueueListener listener: listeners ){
			
			try{
				listener.jobRemoved( job );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}
	}
	
	public TranscodeJob[]
	getJobs()
	{
		synchronized( queue ){

			return( queue.toArray( new TranscodeJob[queue.size()]));
		}
	}
	
	protected void
	loadConfig()
	{
		
	}
	
	protected void
	saveConfig()
	{
		
	}
	
	public void
	addListener(
		TranscodeQueueListener		listener )
	{
		listeners.add( listener );
	}
	
	public void
	removeListener(
		TranscodeQueueListener		listener )
	{
		listeners.remove( listener );	
	}
}
