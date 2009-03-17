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

import org.gudy.azureus2.core3.util.*;

import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.download.DownloadRemovalVetoException;
import org.gudy.azureus2.plugins.download.DownloadWillBeRemovedListener;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;

import com.aelitis.azureus.core.devices.TranscodeJob;
import com.aelitis.azureus.core.devices.TranscodeProfile;
import com.aelitis.azureus.core.devices.TranscodeException;
import com.aelitis.azureus.core.devices.TranscodeTarget;
import com.aelitis.azureus.core.download.DiskManagerFileInfoFile;
import com.aelitis.azureus.core.messenger.config.PlatformDevicesMessenger;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.util.ImportExportUtils;

public class 
TranscodeJobImpl 
	implements TranscodeJob, DownloadWillBeRemovedListener
{
	private static final int TRANSCODE_OK_DL_PERCENT	= 90;
	
	private TranscodeQueueImpl		queue;
	private TranscodeTarget			target;
	private TranscodeProfile		profile;
	private DiskManagerFileInfo		file;
	private TranscodeFileImpl		transcode_file;
	
	private boolean					is_stream;
	private volatile InputStream	stream;
	private AESemaphore				stream_sem = new AESemaphore( "TJ:s" );
	
	private int						transcode_requirement;
	
	private int						state 				= ST_QUEUED;
	private int						percent_complete	= 0;
	private int						eta					= Integer.MAX_VALUE;
	private String					error;
	private long					started_on;
	private long					paused_on;
	private long					process_time;
	
	private boolean					use_direct_input;
	
	private boolean					auto_retry;
	private int						auto_retry_count;
	
	private Download				download;
	private boolean					download_ok;
	
	protected
	TranscodeJobImpl(
		TranscodeQueueImpl		_queue,
		TranscodeTarget			_target,
		TranscodeProfile		_profile,
		DiskManagerFileInfo		_file,
		int						_transcode_requirement,
		boolean					_is_stream )
	
		throws TranscodeException
	{
		queue					= _queue;
		target					= _target;
		profile					= _profile;
		file					= _file;
		transcode_requirement	= _transcode_requirement;
		is_stream				= _is_stream;
		
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
		
		if ( state == ST_RUNNING ){
			
			state = ST_QUEUED;
		}
		
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
		
		transcode_requirement	= ImportExportUtils.importInt( map, "trans_req", -1 );
		
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
		
			ImportExportUtils.exportInt( map, "trans_req", transcode_requirement );
					
			return( map );
			
		}catch( Throwable e ){
			
			throw( new IOException( "Export failed: " + Debug.getNestedExceptionMessage(e)));
		}
	}
	
	protected void
	init()
	
		throws TranscodeException
	{
		transcode_file = ((DeviceImpl)target.getDevice()).allocateFile( profile, getTranscodeRequirement() == TranscodeTarget.TRANSCODE_NEVER, file, true );
		
		try{
			download = file.getDownload();
			
			if ( download != null ){
				
				download.addDownloadWillBeRemovedListener( this );
				
				updateStatus();
			}
			
		}catch( Throwable e ){
		}
	}
	
	protected void
	updateStatus()
	{
		synchronized( this ){
			
			if ( download_ok ){
				
				return;
			}
			
			long	downloaded 	= file.getDownloaded();
			long	length		= file.getLength();
			
			if ( 	download == null || downloaded == length ){
				
				download_ok = true;
				
			}else{

				if ( PlatformTorrentUtils.isContent( download.getTorrent(), false )){
					
					download_ok = true;
					
				}else{
					
					int	percent_done = (int)( 100*downloaded/length );
					
					if ( percent_done >= TRANSCODE_OK_DL_PERCENT ){
						
						download_ok = true;
					}
				}
			}
		}
		
		if ( download_ok ){
			
			queue.jobChanged( this, true, false );
		}
	}
	
	public long
	getDownloadETA()
	{
		if ( download_ok ){
			
			return( 0 );
		}
		
		if ( file.getDownloaded() == file.getLength()){
			
			return( 0 );
		}

		if ( file.isSkipped() || file.isDeleted()){
				
			return( Long.MAX_VALUE );
		}
			
		try{
			long	eta = PluginCoreUtils.unwrap( download ).getStats().getETA();
			
			if ( eta < 0 ){
				
				return( Long.MAX_VALUE );
			}
			
			long adjusted = eta*100/TRANSCODE_OK_DL_PERCENT;
			
			if ( adjusted == 0 ){
				
				adjusted = 1;
			}
			
			return( adjusted );
			
		}catch( Throwable e ){
			
			Debug.out( e );
			
			return( Long.MAX_VALUE );
		}
	}
	
	protected boolean
	canUseDirectInput()
	{
		long	length = file.getLength();

		return( file.getDownloaded() == length &&
				file.getFile().length() == length );
	}
	
	protected boolean
	useDirectInput()
	{
		synchronized( this ){

			return( use_direct_input );
		}
	}
	
	protected void
	setUseDirectInput()
	{
		synchronized( this ){

			use_direct_input = true;
		}
	}
	
	protected void
	setAutoRetry(
		boolean		_auto_retry )
	{
		synchronized( this ){

			if ( _auto_retry ){
				
				auto_retry 	= true;
				
				auto_retry_count++;
				
			}else{
				
				auto_retry = false;
			}
		}
	}
	
	protected boolean
	isAutoRetry()
	{
		synchronized( this ){
			
			return( auto_retry );
		}
	}
	
	protected int
	getAutoRetryCount()
	{
		synchronized( this ){

			return( auto_retry_count );
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
		if ( state == ST_FAILED ){
			
			throw( new IOException( "Transcode job failed: " + error ));
			
		}else if ( state == ST_CANCELLED ){
			
			throw( new IOException( "Transcode job cancelled" ));

		}else if ( state == ST_REMOVED ){
			
			throw( new IOException( "Transcode job removed" ));
		}
		
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
		if ( download != null ){
		
			if ( download.getDiskManagerFileInfo().length == 1 ){
				
				return( download.getName());
			}
			
			return( download.getName() + ": " + file.getFile().getName());
			
		}else{
			
			return( file.getFile().getName());
		}
	}
	
	protected void
	reset()
	{
		state 				= ST_QUEUED;
		error 				= null;
		percent_complete	= 0;
		eta					= Integer.MAX_VALUE;
	}
	
	protected void
	starts()
	{
		synchronized( this ){
		
			started_on 	= SystemTime.getMonotonousTime();
			paused_on	= 0;

				// this is for an Azureus restart with a paused job - we don't want to change the
				// state as we want it to re-pause...
			
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
		queue.log( "Transcode failed", e );
		
		synchronized( this ){
			
			if ( state != ST_STOPPED ){
			
				state = ST_FAILED;
			
				error = Debug.getNestedExceptionMessage( e );
				
					// process_time filled with negative pause time, so add to it
				
				process_time += SystemTime.getMonotonousTime() - started_on;
				
				started_on = paused_on = 0;
			}
		}
		
		queue.jobChanged( this, false, true );

		// I'd rather do qos from a listener trigger, but for now this ensures
		// I get the event even if listeners haven't had a chance to be added.
		// This also ensures only one failed qos gets sent
		try {
			PlatformDevicesMessenger.qosTranscode(this, TranscodeJob.ST_FAILED);
		} catch (Throwable t) {
			Debug.out(t);
		}
	}
	
	protected void
	complete()
	{
		synchronized( this ){
		
			state = ST_COMPLETE;
			
				// process_time filled with negative pause time, so add to it
			
			process_time += SystemTime.getMonotonousTime() - started_on;
			
			started_on = paused_on = 0;
		}
		
		if ( download != null ){
			
			download.removeDownloadWillBeRemovedListener( this );
		}
		
		transcode_file.setComplete( true );
		
		queue.jobChanged( this, false, false );

		// I'd rather do qos from a listener trigger, but for now this ensures
		// I get the event even if listeners haven't had a chance to be added
		// This also ensures only one completed qos event gets sent
		try {
			PlatformDevicesMessenger.qosTranscode(this, TranscodeJob.ST_COMPLETE);
		} catch (Throwable t) {
			Debug.out(t);
		}
	}
	
	protected void
	updateProgress(
		int		_done,
		int		_eta )
	{
		if ( percent_complete != _done || eta != _eta){
		
			percent_complete	= _done;
			eta					= _eta;
			
			queue.jobChanged( this, false, false );
		}
	}
	
	public TranscodeTarget
	getTarget()
	{
		return( target );
	}
	
	public int
	getTranscodeRequirement()
	{
		if ( transcode_requirement >= 0 ){
			
			return( transcode_requirement );
		}
		
		return( getDevice().getTranscodeRequirement());
	}
	
	protected DeviceImpl
	getDevice()
	{
		return((DeviceImpl)target );
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
	getETA()
	{
		if ( eta < 0 ){
			
			return( null );
			
		}else if ( eta == Integer.MAX_VALUE ){
			
			return( Constants.INFINITY_STRING );
			
		}else{
			
			return( TimeFormatter.format( eta ));
		}
	}
	
	public String
	getError()
	{
		return( error );
	}
	
	public boolean
	canPause()
	{
		synchronized( this ){

			return( !use_direct_input );
		}
	}
	
	public void
	pause()
	{
		synchronized( this ){
			
			if ( use_direct_input ){
				
				return;
			}
			
			if ( state == ST_RUNNING ){
		
				state = ST_PAUSED;
				
				paused_on = SystemTime.getMonotonousTime();
				
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

				if ( paused_on > 0 && started_on > 0 ){
					
					process_time -= SystemTime.getMonotonousTime() - paused_on;
				}
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
								
				reset();
				
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
				
				process_time = 0;

				started_on = 0;
				
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
			
			state = ST_REMOVED;
		}
		
		if ( delete_file && !isStream()){
			
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
	
	public long
	getProcessTime()
	{
		if ( state == ST_COMPLETE ){
		
			return process_time;
		}
		
		if ( started_on == 0 ){
			
			if (  process_time > 0 ){
				
				return process_time;
			}
			
			return 0;
		}
			// process_time filled with pause
		
		return SystemTime.getMonotonousTime() - started_on + process_time;
	}
		
	public void
	generate(
		IndentWriter		writer )
	{
		writer.println( "target=" + target.getID() + ", profile=" + profile.getName() + ", file=" + file );
		writer.println( "tfile=" + transcode_file.getString());
		writer.println( "stream=" + is_stream + ", state=" + state + ", treq=" + transcode_requirement + ", %=" + percent_complete + ", error=" + error );
	}
}
