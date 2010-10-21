/*
 * Created on Jun 21, 2010
 * Created by Paul Gardner
 * 
 * Copyright 2010 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.core.download;

import java.util.*;
import java.lang.reflect.Method;
import java.net.URL;


import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimeFormatter;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.devices.Device;
import com.aelitis.azureus.core.devices.DeviceManager;
import com.aelitis.azureus.core.devices.DeviceManagerFactory;
import com.aelitis.azureus.core.devices.DeviceMediaRenderer;
import com.aelitis.azureus.core.devices.TranscodeAnalysisListener;
import com.aelitis.azureus.core.devices.TranscodeException;
import com.aelitis.azureus.core.devices.TranscodeJob;
import com.aelitis.azureus.core.devices.TranscodeManager;
import com.aelitis.azureus.core.devices.TranscodeProfile;
import com.aelitis.azureus.core.devices.TranscodeProviderAnalysis;
import com.aelitis.azureus.core.devices.TranscodeQueue;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.swt.plugininstall.SimplePluginInstaller;

public class 
StreamManager 
{
	private static final int BUFFER_SECS 		= 30;
	private static final int BUFFER_MIN_SECS	= 3;
	

	private static StreamManager		singleton = new StreamManager();
	
	public static StreamManager
	getSingleton()
	{
		return( singleton );
	}
	
	private TorrentAttribute	mi_ta;
	
	private AsyncDispatcher	dispatcher = new AsyncDispatcher();
	
	private
	StreamManager()
	{
		PluginInterface default_pi = PluginInitializer.getDefaultInterface();

		mi_ta = default_pi.getTorrentManager().getPluginAttribute( "sm_metainfo" );
	}
	
	public StreamManagerDownload
	stream(
		DownloadManager					dm,
		int								file_index,
		URL								url,
		boolean							preview_mode,
		StreamManagerDownloadListener	listener )
	{
		SMDImpl	result = new SMDImpl( dm, file_index, url, preview_mode, listener );
				
		return( result );
	}


	private class
	SMDImpl
		extends AERunnable
		implements StreamManagerDownload
	{
		private DownloadManager						dm;
		private int									file_index;
		private URL									url;
		private StreamManagerDownloadListener		listener;
			
		private boolean					preview_mode;
		private long					preview_mode_last_change = 0;
		
		private AESemaphore				active_sem;
		private TranscodeJob			active_job;
		
		private volatile boolean		cancelled;
		
		private 
		SMDImpl(
			DownloadManager					_dm,
			int								_file_index,
			URL								_url,
			boolean							_preview_mode,
			StreamManagerDownloadListener	_listener )
		{
			dm				= _dm;
			file_index		= _file_index;
			url				= _url;
			preview_mode	= _preview_mode;
			listener		= _listener;
			
			dispatcher.dispatch( this );
		}
		
		public DownloadManager
		getDownload()
		{
			return( dm );
		}
		
		public int
		getFileIndex()
		{
			return( file_index );
		}
		
		public URL
		getURL()
		{
			return( url );
		}
		
		public boolean
		getPreviewMode()
		{
			return( preview_mode );
		}
		
		public void
		setPreviewMode(
			boolean	_preview_mode )
		{
			long	now = SystemTime.getMonotonousTime();
			
			if ( 	preview_mode_last_change == 0 || 
					now - preview_mode_last_change > 500 ){
				
				preview_mode_last_change = now;
				
				preview_mode = _preview_mode;
				
				listener.updateActivity( "Preview mode changed to " + preview_mode );
			}
		}
		
		public void
		runSupport()
		{
			try{
				Download download = PluginCoreUtils.wrap( dm );
				
				final DiskManagerFileInfo file = download.getDiskManagerFileInfo( file_index );
				
				PluginInterface emp_pi = checkPlugin( "azemp", "media player" );

				Class<?> epwClass = emp_pi.getPlugin().getClass().getClassLoader().loadClass( "com.azureus.plugins.azemp.ui.swt.emp.EmbeddedPlayerWindowSWT" );
				
				Method method = epwClass.getMethod( "prepareWindow", new Class[] { String.class });
				
				final Object player = method.invoke(null, new Object[] { file.getFile( true ).getName() });
			
				final Method buffering_method	= player.getClass().getMethod( "bufferingPlayback", new Class[] { Map.class });

				final StreamManagerDownloadListener original_listener = listener;
				
				listener = 
					new StreamManagerDownloadListener()
					{
						public void
						updateActivity(
							String		str )
						{
							original_listener.updateActivity(str);
						}
						
						public void
						updateStats(
							int			secs_until_playable,
							int			buffer_secs,
							long		buffer_bytes,
							int			target_buffer_secs )
						{
							original_listener.updateStats(secs_until_playable, buffer_secs, buffer_bytes, target_buffer_secs);
						}
						
						public void
						ready()
						{
							original_listener.ready();
						}
						
						public void
						failed(
							Throwable 	error )
						{
							original_listener.failed(error);
							
							Map<String,Object> b_map = new HashMap<String,Object>();
							
							b_map.put( "state", new Integer( 3 ));
							b_map.put( "msg", Debug.getNestedExceptionMessage( error ));
							
							try{
								buffering_method.invoke(player, new Object[] { b_map });

							}catch( Throwable e ){
								
								Debug.out( e );
							}
						}
					};
				
				Map<String,Map<String,Object>>	map = (Map<String,Map<String,Object>>)download.getMapAttribute( mi_ta );
				
				Long	l_duration 		= null;
				Long	l_video_width 	= null;
				Long	l_video_height 	= null;
				
				if ( map != null ){
					
					Map<String,Object> file_map = map.get( String.valueOf( file_index ));
					
					if ( file_map != null ){
						
						l_duration 		= (Long)file_map.get( "duration" );
						l_video_width 	= (Long)file_map.get( "video_width" );
						l_video_height 	= (Long)file_map.get( "video_height" );
					}
				}
				
				long duration;
				long video_width;
				long video_height;
				
				if ( l_duration == null ){
						
					checkPlugin( "vuzexcode", "media analyser" );
					
					try{
						DeviceManager dm = DeviceManagerFactory.getSingleton();
						
						TranscodeManager tm = dm.getTranscodeManager();
						
						DeviceMediaRenderer dmr = 
							(DeviceMediaRenderer)dm.addVirtualDevice(
								Device.DT_MEDIA_RENDERER,
								"18a0b53a-a466-6795-1d0f-cf38c830ca0e", 
								"generic",
								"Media Analyser" );
	
						dmr.setHidden(true);
	
						TranscodeQueue queue = tm.getQueue();
						
						TranscodeJob[] jobs = queue.getJobs();
						
						for ( TranscodeJob job: jobs ){
							
							if ( job.getTarget() == dmr ){
								
								job.removeForce();
							}
						}
						
						TranscodeProfile[] profiles = dmr.getTranscodeProfiles();
						
						TranscodeProfile profile = null;
						
						for (TranscodeProfile p : profiles) {
							
							if ( p.getName().equals( "Generic MP4" )){
								
								profile = p;
	
								break;
							}
						}
						
						if ( profile == null ){
							
							throw( new Exception( "Analyser transcode profile not found" ));
						}
						
						listener.updateActivity( "Analysing media" );
						
						Map<String,Object> b_map = new HashMap<String,Object>();
						
						b_map.put( "state", new Integer( 1 ));
						b_map.put( "msg", "Analysing Media" );
						
						buffering_method.invoke(player, new Object[] { b_map });

						final TranscodeJob tj = queue.add( dmr, profile, file, true );
											
						try{
							final AESemaphore sem = new AESemaphore( "analyserWait" );

							synchronized( StreamManager.this ){
								
								if ( cancelled ){
									
									throw( new Exception( "Cancelled" ));
								}
								
								active_sem	= sem;
								active_job 	= tj;
							}
								
							final long[] properties = new long[3];
							
							final Throwable[] error = { null };
							
							tj.analyseNow(
								new TranscodeAnalysisListener()
								{
									public void
									analysisComplete(
										TranscodeJob					file,
										TranscodeProviderAnalysis		analysis )
									{
										try{											
											properties[0] = analysis.getLongProperty( TranscodeProviderAnalysis.PT_DURATION_MILLIS );
											properties[1] = analysis.getLongProperty( TranscodeProviderAnalysis.PT_VIDEO_WIDTH );
											properties[2] = analysis.getLongProperty( TranscodeProviderAnalysis.PT_VIDEO_HEIGHT );
											
											tj.removeForce();
											
										}finally{
											
											sem.release();
										}
									}
									
									public void
									analysisFailed(
										TranscodeJob		file,
										TranscodeException	e )
									{
										try{
											error[0] = e;
										
											tj.removeForce();
											
										}finally{
											
											sem.release();
										}
									}
								});
							
							sem.reserve();
							
							synchronized( StreamManager.this ){
								
								if ( cancelled ){
										
									throw( new Exception( "Cancelled" ));
								}
								
								active_job 	= null;
								active_sem	= null;
							}
							
							if ( error[0] != null ){
								
								throw( error[0] );
							}
							
							duration 		= properties[0];
							video_width		= properties[1];
							video_height	= properties[2];
							
							if ( duration > 0 ){
								
								if ( map == null ){
									
									map = new HashMap<String, Map<String,Object>>();
									
								}else{
									
									map = new HashMap<String, Map<String,Object>>( map );
								}
								
								Map<String,Object> file_map = map.get( String.valueOf( file_index ));
								
								if ( file_map == null ){
								
									file_map = new HashMap<String, Object>();
									
									map.put( String.valueOf( file_index ), file_map );
								}
								
								file_map.put( "duration", duration );
								file_map.put( "video_width", video_width );
								file_map.put( "video_height", video_height );
								
								download.setMapAttribute( mi_ta, map );
							}
							
						}catch( Throwable e ){
							
							tj.removeForce();
							
							throw( e );
						}
						
					}catch( Throwable e ){
						
						throw( new Exception( "Media analysis failed", e ));
					}
				}else{
						
					duration 		= l_duration;
					video_width		= l_video_width==null?0:l_video_width;
					video_height	= l_video_height==null?0:l_video_height;
				}
					
				if ( video_width == 0 || video_height == 0){
					
					throw( new Exception( "Media analysis failed - video stream not found" ));
				}
				
				if ( duration == 0 ){
					
					throw( new Exception( "Media analysis failed - duration unknown" ));
				}
				
				listener.updateActivity( "MetaData read: duration=" + TimeFormatter.formatColon( duration/1000) + ", width=" + video_width + ", height=" + video_height );
				
				Method smd_method = player.getClass().getMethod( "setMetaData", new Class[] { Map.class });
				
				Map<String,Object>	md_map = new HashMap<String,Object>();
				
				md_map.put( "duration", duration );
				md_map.put( "width", video_width );
				md_map.put( "height", video_height );
				
				smd_method.invoke( player, new Object[] { md_map });

				final EnhancedDownloadManager edm = DownloadManagerEnhancer.getSingleton().getEnhancedDownload( dm );
				
				long	bytes_per_sec = file.getLength() / (duration/1000);
				
				listener.updateActivity( "Average rate=" + DisplayFormatters.formatByteCountToKiBEtcPerSec( bytes_per_sec ));
												
				edm.setExplicitProgressive( BUFFER_SECS, bytes_per_sec, file_index );
				
				if ( !edm.setProgressiveMode( true )){
					
					throw( new Exception( "Failed to set download as progressive" ));
				}

				new AEThread2( "streamMon" )
				{
					private boolean playback_started 	= false;
					private boolean	playback_paused		= false;
					
					public void
					run()
					{	
						boolean	error_reported = false;
						
						try{
							Method start_method 	= player.getClass().getMethod( "startPlayback", new Class[] { URL.class });
							Method pause_method 	= player.getClass().getMethod( "pausePlayback", new Class[] {});
							Method resume_method 	= player.getClass().getMethod( "resumePlayback", new Class[] {});
							Method buffering_method	= player.getClass().getMethod( "bufferingPlayback", new Class[] { Map.class });

							while( !cancelled ){
									
								if ( file.getLength() != file.getDownloaded()){

									int dm_state = dm.getState();
									
									if ( dm_state == DownloadManager.STATE_ERROR || dm_state == DownloadManager.STATE_STOPPED ){
										
										throw( new Exception( "Streaming abandoned, download isn't running" ));
									}
	
									if ( !edm.getProgressiveMode()){
									
										throw( new Exception( "Streaming mode abandoned for download" ));
									}
								}
							
								long[] details = updateETA( edm );
								
								int		eta 		= (int)details[0];
								int		buffer_secs	= (int)details[1];
								long	buffer		= details[2];
								
								listener.updateStats( eta, buffer_secs, buffer, BUFFER_SECS );

								boolean playable =  buffer_secs > BUFFER_SECS && ( eta <= 0 || preview_mode );
								
								if ( !playback_started ){
									
									if ( playable ){
										
										listener.ready();
											
										start_method.invoke(player, new Object[] { url });
										
										playback_started = true;
									}
								}else if ( playback_started ){
									
									if ( buffer_secs < BUFFER_MIN_SECS ){
										
										if ( !playback_paused ){
											
											listener.updateActivity( "Pausing playback to prevent stall" );
																						
											pause_method.invoke(player, new Object[] {});

											playback_paused = true;
										}
									}else if ( playable ){
										
										if ( playback_paused ){
										
											listener.updateActivity( "Resuming playback" );
											
											resume_method.invoke(player, new Object[] {});

											playback_paused = false;
										}
									}
								}
							
								if ( !playable ){
									
									Map<String,Object> map = new HashMap<String,Object>();
									
									map.put( "state", new Integer( 2 ));
									map.put( "eta", new Integer( eta ));
									map.put( "buffer_secs", new Integer( buffer_secs ));
									map.put( "buffer_bytes", new Long( buffer ));
									
									buffering_method.invoke(player, new Object[] { map });
								}
								
								Thread.sleep( 250 );
							}
						}catch( Throwable e ){
							
							error_reported = true;
							
							listener.failed( e );
							
						}finally{
							
							if ( !( error_reported || cancelled )){
								
								if ( !playback_started ){
								
									listener.failed( new Exception( "Streaming failed, reason unknown" ));
								}
							}
						}
					}
				}.start();
								
			}catch( Throwable e ){
								
				listener.failed( e );
			}
		}
		
		private long[]
		updateETA(
			EnhancedDownloadManager 	edm )
		{
			long _eta = edm.getProgressivePlayETA();
					
			int	eta = _eta>=Integer.MAX_VALUE?Integer.MAX_VALUE:(int)_eta;
			
			EnhancedDownloadManager.progressiveStats stats = edm.getProgressiveStats();
			
			long provider_pos = stats.getCurrentProviderPosition( false );
			
			long buffer = edm.getContiguousAvailableBytes( edm.getPrimaryFile().getIndex(), provider_pos, 0 );
			
			long bps = stats.getStreamBytesPerSecondMin();
						
			int	buffer_secs = bps<=0?Integer.MAX_VALUE:(int)(buffer/bps);
											
			return( new long[]{ eta, buffer_secs, buffer });
		}
		
		public void
		cancel()
		{
			TranscodeJob	job;
			
			synchronized( StreamManager.this ){
				
				cancelled = true;
			
				job = active_job;
				
				if ( active_sem != null ){
					
					active_sem.release();
				}
			}
			
			if ( job != null ){
				
				job.removeForce();
			}
		}
		
		private PluginInterface
		checkPlugin(
			String		id,
			String		name )
		
			throws Throwable
		{
			PluginManager plug_man = AzureusCoreFactory.getSingleton().getPluginManager();
			
			PluginInterface pi = plug_man.getPluginInterfaceByID( id, false );
			
			if ( pi == null ){
				
				listener.updateActivity( "Installing " + name );
				
				final AESemaphore sem = new AESemaphore( "analyserWait" );

				synchronized( StreamManager.this ){
					
					if ( cancelled ){
						
						throw( new Exception( "Cancelled" ));
					}
					
					active_sem	= sem;
				}
				
				final Throwable[] error = { null };
				
				new SimplePluginInstaller(
						id,
	    				"dlg.install." + id,
	    				new UIFunctions.actionListener()
						{
							public void
							actionComplete(
								Object		result )
							{
								try{
									if ( result instanceof Boolean ){
										
									}else{
										
										error[0] = (Throwable)result;
									}
								}finally{
									
									sem.release();
								}
							}
						});
				
				sem.reserve();
				
				synchronized( StreamManager.this ){
					
					if ( cancelled ){
							
						throw( new Exception( "Cancelled" ));
					}
					
					active_sem	= null;
				}

				if( error[0] != null ){
					
					throw( error[0] );
				}
				
				long start = SystemTime.getMonotonousTime();
				
				listener.updateActivity( "Waiting for plugin initialisation" );
				
				while( true ){
					
					if ( cancelled ){
						
						throw( new Exception( "Cancelled" ));
					}
					
					if ( SystemTime.getMonotonousTime() - start >= 30*1000 ){
						
						throw( new Exception( "Timeout waiting for " + name + " to initialise" ));
					}
					
					pi = plug_man.getPluginInterfaceByID( id, false );

					if ( pi != null && pi.getPluginState().isOperational()){
						
						return( pi );
					}
					
					Thread.sleep(250);
				}
			}else if ( !pi.getPluginState().isOperational()){
				
				throw( new Exception( name + " not operational" ));
				
			}else{
				
				return( pi );
			}
		}
	}		
}
