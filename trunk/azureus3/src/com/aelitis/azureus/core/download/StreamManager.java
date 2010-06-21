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
import java.net.URL;


import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;

import com.aelitis.azureus.core.devices.Device;
import com.aelitis.azureus.core.devices.DeviceManager;
import com.aelitis.azureus.core.devices.DeviceManagerFactory;
import com.aelitis.azureus.core.devices.DeviceMediaRenderer;
import com.aelitis.azureus.core.devices.TranscodeAnalysisListener;
import com.aelitis.azureus.core.devices.TranscodeException;
import com.aelitis.azureus.core.devices.TranscodeJob;
import com.aelitis.azureus.core.devices.TranscodeManager;
import com.aelitis.azureus.core.devices.TranscodeProfile;
import com.aelitis.azureus.core.devices.TranscodeProvider;
import com.aelitis.azureus.core.devices.TranscodeProviderAnalysis;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;

public class 
StreamManager 
{
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
		DownloadManager			dm,
		int						file_index,
		URL						url )
	{
		SMDImpl	result = new SMDImpl( dm, file_index, url );
				
		return( result );
	}


	private class
	SMDImpl
		extends AERunnable
		implements StreamManagerDownload
	{
		private DownloadManager			dm;
		private int						file_index;
		private URL						url;
		
		private int						state = 0;
		
		private long					duration;
		
		private 
		SMDImpl(
			DownloadManager			_dm,
			int						_file_index,
			URL						_url )
		{
			dm			= _dm;
			file_index	= _file_index;
			url			= _url;
			
			dispatcher.dispatch( this );
		}
		
		public void
		runSupport()
		{
			if ( state == 0 ){
								
				Download download = PluginCoreUtils.wrap( dm );
				
				Map	map = download.getMapAttribute( mi_ta );
				
				Long	l_duration = null;
				
				if ( map != null ){
					
					l_duration = (Long)map.get( "duration" );
				}
				
				if ( l_duration == null ){
						
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

						TranscodeProfile[] profiles = dmr.getTranscodeProfiles();
						
						TranscodeProfile profile = null;
						
						for (TranscodeProfile p : profiles) {
							
							if ( p.getName().equals( "Generic MP4" )){
								
								profile = p;
	
								break;
							}
						}
						
						final TranscodeJob tj = tm.getQueue().add( dmr, profile, download.getDiskManagerFileInfo( file_index ), true );
					
						try{
							tj.analyseNow(
								new TranscodeAnalysisListener()
								{
									public void
									analysisComplete(
										TranscodeJob					file,
										TranscodeProviderAnalysis		analysis )
									{
										System.out.println( "done" );
										
										analysis.getLongProperty( TranscodeProviderAnalysis.PT_DURATION_MILLIS );
										analysis.getLongProperty( TranscodeProviderAnalysis.PT_VIDEO_WIDTH );
										analysis.getLongProperty( TranscodeProviderAnalysis.PT_VIDEO_HEIGHT );
										
										tj.removeForce();
									}
									
									public void
									analysisFailed(
										TranscodeJob		file,
										TranscodeException	error )
									{
										Debug.out(error);
										
										tj.removeForce();
									}
								});
						}catch( Throwable e ){
							
							tj.removeForce();
							
							Debug.out( e );
						}
						
					}catch( Throwable e ){
						
						Debug.out( e );
					}
				}else{
						
					duration = l_duration;
				}
			}
		}
	}		
}
