/*
 * Created on Feb 5, 2009
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
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.ipc.IPCInterface;

import com.aelitis.azureus.core.devices.TranscodeProfile;
import com.aelitis.azureus.core.devices.TranscodeProvider;
import com.aelitis.azureus.core.devices.TranscodeProviderAdapter;
import com.aelitis.azureus.core.devices.TranscodeException;
import com.aelitis.azureus.core.devices.TranscodeProviderAnalysis;
import com.aelitis.azureus.core.devices.TranscodeProviderJob;

public class 
TranscodeProviderVuze 
	implements TranscodeProvider
{
	private PluginInterface		plugin_interface;
	
	private volatile TranscodeProfile[]	profiles;
	
	protected
	TranscodeProviderVuze(
		PluginInterface			pi )
	{
		plugin_interface		= pi;
	}
	
	protected void
	update(
		PluginInterface		pi )
	{
		plugin_interface		= pi;
	}
	
	public String
	getName()
	{
		return( plugin_interface.getPluginName() + ": version=" + plugin_interface.getPluginVersion());
	}
	
	public TranscodeProfile[]
	getProfiles()
	{			
		if ( profiles != null ){
				
			return( profiles );
		}
		
		try{
			Map<String, Map<String,String>> profiles_map = (Map<String,Map<String,String>>)plugin_interface.getIPC().invoke( "getProfiles", new Object[]{} );
			
			TranscodeProfile[] res = new TranscodeProfile[profiles_map.size()];
			
			int	index = 0;
			
			for ( Map.Entry<String, Map<String,String>> entry : profiles_map.entrySet()){
				
				res[ index++] = new TranscodeProfileImpl( this, "vuzexcode:" + entry.getKey(), entry.getKey(), entry.getValue());
			}
			
			profiles	= res;
			
			return( res );
			
		}catch( Throwable e ){
			
			e.printStackTrace();
			
			return( new TranscodeProfile[0] );
		}
	}
	
	public TranscodeProfile
	getProfile(
		String		UID )
	{
		TranscodeProfile[] profiles = getProfiles();
		
		for ( TranscodeProfile profile: profiles ){
			
			if ( profile.getUID().equals( UID )){
				
				return( profile );
			}
		}
		
		return( null );
	}
	
	public TranscodeProviderAnalysis
	analyse( 
		final TranscodeProviderAdapter	_adapter,
		DiskManagerFileInfo				input,
		TranscodeProfile				profile )	
	
		throws TranscodeException
	{
		try{
			
			URL 				source_url		= null;
			File				source_file	 	= null;
			
			long	input_length = input.getLength();
			
			if ( input_length > 0 && input_length == input.getDownloaded()){
				
				File file = input.getFile();
				
				if ( file.exists() && file.length() == input_length ){
					
					source_file = file;
				}
			}
			
			TranscodePipe		pipe = null;

			if ( source_file == null ){
							
				PluginInterface av_pi = plugin_interface.getPluginManager().getPluginInterfaceByID( "azupnpav" );
				
				if ( av_pi == null ){
				
					throw( new TranscodeException( "Media Server plugin not found" ));
				}
				
				IPCInterface av_ipc = av_pi.getIPC();
				
				String url_str = (String)av_ipc.invoke( "getContentURL", new Object[]{ input });
		
				if ( url_str != null && url_str.length() > 0 ){
				
					source_url = new URL( url_str );
			
					pipe = new TranscodePipeStreamSource( source_url.getPort());
				
					source_url = UrlUtils.setPort( source_url, pipe.getPort());
				}
			}
			
			if ( source_file == null && source_url == null ){
			
				throw( new TranscodeException( "No UPnPAV URL and file doesn't exist" ));
			}

			final TranscodePipe f_pipe = pipe;
			
			try{	
				final IPCInterface	ipc = plugin_interface.getIPC();

				final Object analysis_context;
				
				if ( source_url != null ){
					
					analysis_context = ipc.invoke(
						"analyseContent",
						new Object[]{ 
							source_url,
							profile.getName() });
				}else{
					
					analysis_context = ipc.invoke(
						"analyseContent",
						new Object[]{ 
							source_file,
							profile.getName() });
				}
				
				final Map<String,Object>	result = new HashMap<String, Object>();
				
				final TranscodeProviderAnalysis analysis = 
					new TranscodeProviderAnalysis()
					{
						public void 
						cancel() 
						{
							try{
								ipc.invoke( "cancelAnalysis", new Object[]{ analysis_context });
								
							}catch( Throwable e ){
								
								Debug.printStackTrace( e );
							}
						}
						
						public boolean
						getBooleanProperty(
							int		property )
						{
							if ( property == PT_TRANSCODE_REQUIRED ){
								
								return( getBooleanProperty( "xcode_required", true ));
								
							}else{
								
								Debug.out( "Unknown property: " + property );
								
								return( false );
							}
						}
						
						public long
						getLongProperty(
							int		property )
						{
							if ( property == PT_DURATION_MILLIS ){
								
								return( getLongProperty( "duration_millis", 0 ));
							
							}else if ( property == PT_VIDEO_WIDTH ){
								
								return( getLongProperty( "video_width", 0 ));
	
							}else if ( property == PT_VIDEO_HEIGHT ){
								
								return( getLongProperty( "video_height", 0 ));
	
							}else{
								
								Debug.out( "Unknown property: " + property );
								
								return( 0 );
							}
						}
						
						protected boolean
						getBooleanProperty(
							String		name,
							boolean		def )
						{
							Boolean b = (Boolean)result.get( name );
							
							if ( b != null ){
								
								return( b );
							}
							
							return( def );
						}
						
						protected long
						getLongProperty(
							String		name,
							long		def )
						{
							Long l = (Long)result.get( name );
							
							if ( l != null ){
								
								return( l );
							}
							
							return( def );
						}
					};
					
				new AEThread2( "analysisStatus", true )
				{
					public void 
					run() 
					{
						try{
							while( true ){
																
								try{
									Map status = (Map)ipc.invoke( "getAnalysisStatus", new Object[]{ analysis_context });
									
									long	state = (Long)status.get( "state" );
									
									if ( state == 0 ){
	
										// running
																				
									}else if ( state == 1 ){
										
										_adapter.failed( new TranscodeException( "Analysis cancelled" ));
										
										return;
										
									}else if ( state == 2 ){
										
										_adapter.failed( new TranscodeException( "Analysis failed", (Throwable)status.get( "error" )));
										
										return;
										
									}else{
										
										result.putAll((Map<String,Object>)status.get( "result" ));
										
										_adapter.complete();
										
										return;
									}
								}catch( Throwable e ){
									
									_adapter.failed( new TranscodeException( "Failed to get status", e ));
								}
							}
						}finally{
							
							if ( f_pipe != null ){
							
								f_pipe.destroy();
							}
						}
					}
				}.start();
				
				return( analysis );

						
			}catch( Throwable e ){
				
				if ( pipe != null ){
				
					pipe.destroy();
				}
				
				throw( e );
			}
		}catch( TranscodeException e ){
			
			throw( e );
				
		}catch( Throwable e ){
			
			throw( new TranscodeException( "analysis failed", e ));
		}
	}
	
	public TranscodeProviderJob
	transcode( 
		final TranscodeProviderAdapter	_adapter,
		DiskManagerFileInfo				input,
		TranscodeProfile				profile,
		URL								output )
	
		throws TranscodeException
	{
		try{
			PluginInterface av_pi = plugin_interface.getPluginManager().getPluginInterfaceByID( "azupnpav" );
			
			if ( av_pi == null ){
			
				throw( new TranscodeException( "Media Server plugin not found" ));
			}
			
			IPCInterface av_ipc = av_pi.getIPC();
			
			String url_str = (String)av_ipc.invoke( "getContentURL", new Object[]{ input });
			
			URL 				source_url;
			TranscodePipe		pipe;
			
			if ( url_str == null || url_str.length() == 0 ){
				
					// see if we can use the file directly
				
				File source_file = input.getFile();
				
				if ( source_file.exists()){
					
					pipe = new TranscodePipeFileSource( source_file );
					
					source_url = new URL( "http://127.0.0.1:" + pipe.getPort() + "/" );
					
				}else{
					
					throw( new TranscodeException( "No UPnPAV URL and file doesn't exist" ));
				}
			}else{
				source_url = new URL( url_str );
			
				pipe = new TranscodePipeStreamSource( source_url.getPort());
				
				source_url = UrlUtils.setPort( source_url, pipe.getPort());
			}
			
			final TranscodePipe f_pipe = pipe;
			
			try{	
				final IPCInterface	ipc = plugin_interface.getIPC();

				final Object context;
				
				final TranscodeProviderAdapter	adapter;
				
				if ( output.getProtocol().equals( "tcp" )){
					
					adapter = _adapter;
					
					context = 
						ipc.invoke(
							"transcodeToTCP",
							new Object[]{ 
								source_url,
								profile.getName(),
								output.getPort() });
				}else{
					
					final File file = new File( output.toURI());
						
					adapter = 
						new TranscodeProviderAdapter()
						{
							public void
							updatePercentDone(
								int								percent )
							{
								_adapter.updatePercentDone( percent );
							}
							
							public void
							failed(
								TranscodeException		error )
							{
								try{
									file.delete();
									
								}finally{
									
									_adapter.failed( error );
								}
							}
							
							public void
							complete()
							{
								_adapter.complete();
							}
						};
						
					context = 
						ipc.invoke(
							"transcodeToFile",
							new Object[]{ 
								source_url,
								profile.getName(),
								file });
				}
	
				new AEThread2( "xcodeStatus", true )
					{
						public void 
						run() 
						{
							try{
								boolean	in_progress = true;
								
								while( in_progress ){
									
									in_progress = false;
									
									try{
										Map status = (Map)ipc.invoke( "getTranscodeStatus", new Object[]{ context });
										
										long	state = (Long)status.get( "state" );
										
										if ( state == 0 ){
											
											int	percent = (Integer)status.get( "percent" );
											
											adapter.updatePercentDone( percent );
											
											if ( percent == 100 ){
												
												adapter.complete();
		
											}else{
												
												in_progress = true;
											
												Thread.sleep(1000);
											}
										}else if ( state == 1 ){
											
											adapter.failed( new TranscodeException( "Transcode cancelled" ));
											
										}else{
											
											adapter.failed( new TranscodeException( "Transcode failed", (Throwable)status.get( "error" )));
										}
									}catch( Throwable e ){
										
										adapter.failed( new TranscodeException( "Failed to get status", e ));
									}
								}
							}finally{
								
								f_pipe.destroy();
							}
						}
					}.start();
					
			
				return( 
					new TranscodeProviderJob()
					{
						public void
						pause()
						{
							f_pipe.pause();
						}
						
						public void
						resume()
						{
							f_pipe.resume();
						}
	
						public void 
						cancel() 
						{
							try{
								ipc.invoke( "cancelTranscode", new Object[]{ context });
								
							}catch( Throwable e ){
								
								Debug.printStackTrace( e );
							}
						}
						
						public void 
						setMaxBytesPerSecond(
							int		 max ) 
						{
							f_pipe.setMaxBytesPerSecond( max );
						}
					});
						
			}catch( Throwable e ){
				
				pipe.destroy();
				
				throw( e );
			}
		}catch( TranscodeException e ){
			
			throw( e );
				
		}catch( Throwable e ){
			
			throw( new TranscodeException( "transcode failed", e ));
		}
	}
	
	protected void
	destroy()
	{
		// TODO
	}
}
