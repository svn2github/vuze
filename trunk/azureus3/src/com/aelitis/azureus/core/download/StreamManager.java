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
	private static final int BUFFER_SECS = 30;

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
		StreamManagerDownloadListener	listener )
	{
		SMDImpl	result = new SMDImpl( dm, file_index, url, listener );
				
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
		private StreamManagerDownloadListener	listener;
			
		private AESemaphore				active_sem;
		private TranscodeJob			active_job;
		
		private volatile boolean		cancelled;
		
		private 
		SMDImpl(
			DownloadManager					_dm,
			int								_file_index,
			URL								_url,
			StreamManagerDownloadListener	_listener )
		{
			dm			= _dm;
			file_index	= _file_index;
			url			= _url;
			listener	= _listener;
			
			dispatcher.dispatch( this );
		}
		
		public void
		runSupport()
		{
			boolean	ok				= false;
			boolean	error_reported	= false;
			
			try{
				Download download = PluginCoreUtils.wrap( dm );
				
				DiskManagerFileInfo file = download.getDiskManagerFileInfo( file_index );
				
				Map	map = download.getMapAttribute( mi_ta );
				
				Long	l_duration = null;
				
				if ( map != null ){
					
					Map file_map = (Map)map.get( String.valueOf( file_index ));
					
					if ( file_map != null ){
						
						l_duration = (Long)file_map.get( "duration" );
					}
				}
				
				long duration;
				
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
						
						listener.updateActivity( "analysing media" );
						
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
							
							if ( map == null ){
								
								map = new HashMap();
								
							}else{
								
								map = new HashMap( map );
							}
							
							Map file_map = (Map)map.get( String.valueOf( file_index ));
							
							if ( file_map == null ){
							
								file_map = new HashMap();
								
								map.put( String.valueOf( file_index ), file_map );
							}
							
							file_map.put( "duration", properties[0] );
							file_map.put( "video_width", properties[1] );
							file_map.put( "video_height", properties[2] );
							
							download.setMapAttribute( mi_ta, map );
							
							duration = properties[0];
							
						}catch( Throwable e ){
							
							tj.removeForce();
							
							throw( e );
						}
						
					}catch( Throwable e ){
						
						throw( new Exception( "Media analysis failed", e ));
					}
				}else{
						
					duration = l_duration;
				}
					
				if ( duration == 0 ){
					
					throw( new Exception( "Media analysis failed - duration unknown" ));
				}
				
				PluginInterface emp_pi = checkPlugin( "azemp", "media player" );
				
				final EnhancedDownloadManager edm = DownloadManagerEnhancer.getSingleton().getEnhancedDownload( dm );
				
				long	bytes_per_sec = file.getLength() / (duration/1000);
				
				listener.updateActivity( "media duraton=" + (duration/1000) + " sec, average rate=" + DisplayFormatters.formatByteCountToKiBEtcPerSec( bytes_per_sec ));
												
				edm.setExplicitProgressive( BUFFER_SECS, bytes_per_sec, file_index );
				
				if ( !edm.setProgressiveMode( true )){
					
					throw( new Exception( "Failed to set download as progressive" ));
				}
				
				while( true ){
				
					if ( cancelled ){
						
						throw( new Exception( "Cancelled" ));
					}
					
					long eta = updateETA( edm );
							
					if ( eta <= 0 ){
						
						break;
					}
					
					int dm_state = dm.getState();
					
					if ( dm_state == DownloadManager.STATE_ERROR || dm_state == DownloadManager.STATE_STOPPED ){
						
						throw( new Exception( "Streaming abandoned, download isn't running" ));
					}
										
					Thread.sleep(1000);
				}
				
				Class epwClass = emp_pi.getPlugin().getClass().getClassLoader().loadClass( "com.azureus.plugins.azemp.ui.swt.emp.EmbeddedPlayerWindowSWT" );
				
				Method method = epwClass.getMethod("openWindow", new Class[] { URL.class, String.class });
				
				method.invoke(null, new Object[] { url, file.getFile( true ).getName() });

				new AEThread2( "streamMon" )
				{
					public void
					run()
					{
						try{
							while( edm.getProgressiveMode() && !cancelled ){
																								
								updateETA( edm );
								
								Thread.sleep( 1000 );
							}
						}catch( Throwable e ){
							
							Debug.out( e );
						}
					}
				}.start();
				
				ok = true;
				
			}catch( Throwable e ){
				
				error_reported = true;
				
				listener.failed( e );
				
			}finally{
				
				if ( ok ){
					
					listener.ready();
					
				}else if ( !error_reported ){
					
					listener.failed( new Exception( "Streaming setup failed, reason unknown" ));
				}
			}
		}
		
		private long
		updateETA(
			EnhancedDownloadManager edm )
		{
			long eta = edm.getProgressivePlayETA();
						
			EnhancedDownloadManager.progressiveStats stats = edm.getProgressiveStats();
			
			long provider_pos = stats.getCurrentProviderPosition( false );
			
			long buffer = edm.getContiguousAvailableBytes( edm.getPrimaryFile().getIndex(), provider_pos, 0 );
			
			long bps = stats.getStreamBytesPerSecondMin();
			
			long to_dl 		= stats.getSecondsToDownload();
			long to_watch	= stats.getSecondsToWatch();
			
			System.out.println( "eta=" + eta + ", view=" + provider_pos + ", buffer=" + buffer + "/" + (buffer/bps ) + ", dl=" + to_dl + ", view=" + to_watch );
			
			long actual_buffer_secs = BUFFER_SECS - eta;
			
			if ( actual_buffer_secs < 0 ){
				
				actual_buffer_secs = 0;
			}
			
			if ( actual_buffer_secs > BUFFER_SECS ){
				
				actual_buffer_secs = BUFFER_SECS;
			}
			
			listener.updateStats( (int)actual_buffer_secs, BUFFER_SECS, eta );
			
			return( eta );
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
