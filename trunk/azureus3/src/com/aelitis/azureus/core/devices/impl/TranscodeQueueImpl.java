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

import java.io.File;
import java.util.*;

import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;

import com.aelitis.azureus.core.devices.*;
import com.aelitis.azureus.core.util.CopyOnWriteList;

public class 
TranscodeQueueImpl 	
	implements TranscodeQueue
{
	private List<TranscodeJobImpl>		queue		= new ArrayList<TranscodeJobImpl>();
	private AESemaphore 				queue_sem 	= new AESemaphore( "XcodeQ" );
	private AEThread2					queue_thread;
	
	private CopyOnWriteList<TranscodeQueueListener>	listeners = new CopyOnWriteList<TranscodeQueueListener>();
	
	
	protected
	TranscodeQueueImpl()
	{
		loadConfig();
		
		schedule();
	}
	
	protected boolean
	process(
		final TranscodeJobImpl		job )
	{		
		TranscodeProvider provider = job.getProfile().getProvider();
		
		try{
			job.starts();
			
			final AESemaphore sem = new AESemaphore( "Xcode:proc" );
			
			final TranscodeProviderJob[] provider_job = { null }; 
			
			final Throwable[] error = { null };

			
			TranscodeProviderAdapter adapter = 
				new TranscodeProviderAdapter()
				{
					public void
					updatePercentDone(
						int								percent )
					{
						if ( job.getState() == TranscodeJob.ST_CANCELLED ){
							
							if ( provider_job[0] != null ){
								
								provider_job[0].cancel();
							}
						}else{
							
							job.setPercentDone( percent );
						}
					}
					
					public void
					failed(
						TranscodeProviderException		e )
					{
						error[0] = e;
						
						sem.release();
					}
					
					public void 
					complete() 
					{
						sem.release();
					}
				};
				
			TranscodeProfile profile = job.getProfile();
				
			String ext = profile.getFileExtension();

			DiskManagerFileInfo	file = job.getFile();
			
			String	target_file = file.getFile().getName();
			
			if ( ext != null ){
				
				int	pos = target_file.lastIndexOf( '.' );
				
				if ( pos != -1 ){
					
					target_file = target_file.substring( 0, pos ); 
				}
				
				target_file += ext;
			}
			
			File output_file = job.getTarget().getWorkingDirectory();
			
			output_file = new File( output_file.getAbsoluteFile(), target_file );
					
			provider_job[0] = 
				provider.transcode(
					adapter,
					job.getFile(),
					profile,
					output_file.toURI().toURL());
			
			TranscodeQueueListener listener = 
				new TranscodeQueueListener()
				{
					public void
					jobAdded(
						TranscodeJob		job )
					{					
					}
					
					public void
					jobChanged(
						TranscodeJob		changed_job )
					{
						if ( changed_job == job ){
							
							int	state = job.getState();
							
							if ( state == TranscodeJob.ST_PAUSED ){
								
								provider_job[0].pause();
								
							}else if ( state == TranscodeJob.ST_RUNNING ){
									
								provider_job[0].resume();
							}
						}
					}
					
					public void
					jobRemoved(
						TranscodeJob		removed_job )
					{	
						if ( removed_job == job ){
							
							provider_job[0].cancel();
						}
					}
				};
				
			try{
				addListener( listener );
			
				sem.reserve();
				
			}finally{
				
				removeListener( listener );
			}
			
			if ( error[0] != null ){
				
				throw( error[0] );
			}

			job.complete();
			
			return( true );
			
		}catch( Throwable e ){
			
			job.failed( e );
			
			return( false );
		}
	}
	
	protected void
	schedule()
	{
		synchronized( this ){

			if ( queue.size() > 0 && queue_thread == null ){
				
				queue_thread = new
					AEThread2( "XcodeQ", true )
					{
						public void 
						run() 
						{
							while( true ){
								
								boolean got = queue_sem.reserve( 30*1000 );
									
								TranscodeJobImpl	job = null;
								
								synchronized( TranscodeQueueImpl.this ){
									
									if ( !got ){
										
										if ( queue.size() == 0 ){
											
											queue_thread = null;
											
											return;
										}
										
										continue;
									}
									
									for ( TranscodeJobImpl j: queue ){
										
										if ( j.getState() == TranscodeJob.ST_QUEUED ){
											
											job = j;
											
											break;
										}
									}
								}
								
								if ( job != null ){
								
									if ( process( job )){
									
										remove( job );
									}
								}	
							}
						}
					};
					
				queue_thread.start();
			}
		}
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
			
			queue_sem.release();
			
			saveConfig();
		}
		
		for ( TranscodeQueueListener listener: listeners ){
			
			try{
				listener.jobAdded( job );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}
		
		schedule();
		
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
		
		schedule();
	}
	
	protected void
	jobChanged(
		TranscodeJobImpl		job )
	{

		for ( TranscodeQueueListener listener: listeners ){
			
			try{
				listener.jobChanged( job );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}
	}
	
	protected int
	getIndex(
		TranscodeJobImpl		job )
	{
		return( queue.indexOf(job)+1);
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
