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
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.download.DownloadRemovalVetoException;
import org.gudy.azureus2.plugins.download.DownloadWillBeRemovedListener;

import com.aelitis.azureus.core.devices.TranscodeFile;
import com.aelitis.azureus.core.devices.TranscodeJob;
import com.aelitis.azureus.core.devices.TranscodeProfile;
import com.aelitis.azureus.core.devices.TranscodeException;
import com.aelitis.azureus.core.devices.TranscodeTarget;
import com.aelitis.azureus.core.download.DiskManagerFileInfoFile;
import com.aelitis.azureus.util.ImportExportUtils;

public class 
TranscodeJobImpl 
	implements TranscodeJob, DownloadWillBeRemovedListener
{
	private TranscodeQueueImpl		queue;
	private TranscodeTarget			target;
	private TranscodeProfile		profile;
	private DiskManagerFileInfo		file;
	private TranscodeFileImpl		transcode_file;
	
	private boolean					is_stream;
	private volatile InputStream	stream;
	private AESemaphore				stream_sem = new AESemaphore( "TJ:s" );
	
	private int						state 				= ST_QUEUED;
	private int						percent_complete	= 0;
	private String					error;
	
	protected
	TranscodeJobImpl(
		TranscodeQueueImpl		_queue,
		TranscodeTarget			_target,
		TranscodeProfile		_profile,
		DiskManagerFileInfo		_file,
		boolean					_is_stream )
	
		throws TranscodeException
	{
		queue		= _queue;
		target		= _target;
		profile		= _profile;
		file		= _file;
		is_stream	= _is_stream;
		
		init();
	}
	
	protected
	TranscodeJobImpl(
		TranscodeQueueImpl		_queue,
		Map<String,Object>		map )
	
		throws IOException, TranscodeException
	{
		queue	= _queue;
		
		state = ImportExportUtils.importInt( map, "state" );
		error = ImportExportUtils.importString( map, "error", null );
		
		String	target_id = ImportExportUtils.importString( map, "target" );
		
		target = queue.lookupTarget( target_id );
		
		String	profile_id = ImportExportUtils.importString( map, "profile" );
		
		profile = queue.lookupProfile( profile_id );
		
		String file_str = ImportExportUtils.importString( map, "file" );
		
		if ( file_str == null ){
			
			byte[] dl_hash = ByteFormatter.decodeString( ImportExportUtils.importString( map, "dl_hash" ));
			
			int file_index = ImportExportUtils.importInt( map, "file_index" );
			
			file = queue.lookupFile( dl_hash, file_index );
		}else{
		
			file = new DiskManagerFileInfoFile( new File( file_str ));
		}
		
		init();
	}

	protected Map<String,Object>
	toMap()
	
		throws IOException
	{
		try{
			Map<String,Object> map = new HashMap<String, Object>();
			
			ImportExportUtils.exportInt( map, "state", state );
			ImportExportUtils.exportString( map, "error", error );
			
			ImportExportUtils.exportString( map, "target", target.getID());
			
			ImportExportUtils.exportString( map, "profile", profile.getUID());
			
			try{
				Download download = file.getDownload();
				
				ImportExportUtils.exportString( map, "dl_hash", ByteFormatter.encodeString( download.getTorrent().getHash()));

				ImportExportUtils.exportInt( map, "file_index", file.getIndex());

			}catch( DownloadException e ){
				
					// external file
				
				ImportExportUtils.exportString( map, "file", file.getFile().getAbsolutePath());
			}
		
			return( map );
			
		}catch( Throwable e ){
			
			throw( new IOException( "Export failed: " + Debug.getNestedExceptionMessage(e)));
		}
	}
	
	protected void
	init()
	
		throws TranscodeException
	{
		transcode_file = (TranscodeFileImpl)target.allocateFile( profile, file );
		
		try{
			file.getDownload().addDownloadWillBeRemovedListener( this );
			
		}catch( Throwable e ){
		}
	}
	
	protected boolean
	isStream()
	{
		return( is_stream );
	}
	
	protected void
	setStream(
		InputStream		_stream )
	{
		stream		= _stream;
		
		stream_sem.releaseForever();
	}
	
	protected InputStream
	getStream(
		int		wait_for_millis )
	
		throws IOException
	{
		stream_sem.reserve( wait_for_millis );
		
		return( stream );
	}
	
	public void 
	downloadWillBeRemoved(
		Download 	download )

		throws DownloadRemovalVetoException
	{
		if ( queue.getIndex( this ) == 0 || state == ST_COMPLETE ){
			
			download.removeDownloadWillBeRemovedListener( this );
			
		}else{
			
			throw( new DownloadRemovalVetoException( "Transcode in progress, removal refused" ));
		}
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
		
			if ( state != ST_PAUSED ){
			
				state = ST_RUNNING;
			}
		}
		
		queue.jobChanged( this, false, true );
	}
	
	protected void
	failed(
		Throwable	e )
	{
		synchronized( this ){
			
			if ( state != ST_STOPPED ){
			
				state = ST_FAILED;
			
				error = Debug.getNestedExceptionMessage( e );
			}
		}
		
		queue.jobChanged( this, false, true );
	}
	
	protected void
	complete()
	{
		synchronized( this ){
		
			state = ST_COMPLETE;
		}
		
		transcode_file.setComplete( true );
		
		queue.jobChanged( this, false, false );
	}
	
	protected void
	setPercentDone(
		int		_done )
	{
		if ( percent_complete != _done ){
		
			percent_complete	= _done;
		
			queue.jobChanged( this, false, false );
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
	
	public TranscodeFileImpl 
	getTranscodeFile() 
	{
		return( transcode_file );
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
		
		queue.jobChanged( this, false, true );
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
		
		queue.jobChanged( this, false, true );
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
		
		queue.jobChanged( this, true, true);
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
		
		queue.jobChanged( this, true, true );
	}
	
	public void
	remove()
	{
		queue.remove( this );
	}
	
	protected void
	destroy()
	{
		boolean	delete_file;
		
		synchronized( this ){
			
			delete_file = state != ST_COMPLETE;
		}
		
		if ( delete_file ){
			
			try{
				transcode_file.delete( true );
				
			}catch( Throwable e ){
				
				queue.log( "Faile to destroy job", e );
			}
		}
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
