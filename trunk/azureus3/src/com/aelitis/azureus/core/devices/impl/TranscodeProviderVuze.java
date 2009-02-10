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
import com.aelitis.azureus.core.devices.TranscodeProviderException;
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
	
	public TranscodeProviderJob
	transcode( 
		final TranscodeProviderAdapter	adapter,
		DiskManagerFileInfo				input,
		TranscodeProfile				profile,
		URL								output )
	
		throws TranscodeProviderException
	{
		try{
			PluginInterface av_pi = plugin_interface.getPluginManager().getPluginInterfaceByID( "azupnpav" );
			
			if ( av_pi == null ){
			
				throw( new TranscodeProviderException( "Media Server plugin not found" ));
			}
			
			IPCInterface av_ipc = av_pi.getIPC();
			
			String url_str = (String)av_ipc.invoke( "getContentURL", new Object[]{ input });
			
			URL source_url = new URL( url_str );
			
			final TranscodePipe pipe = new TranscodePipe( source_url.getPort());
			
			try{	
				source_url = UrlUtils.setPort( source_url, pipe.getPort());
				
				File file = new File( output.toURI());
					
				final IPCInterface	ipc = plugin_interface.getIPC();
					
				final Object context = 
					ipc.invoke(
						"transcodeToFile",
						new Object[]{ 
							source_url,
							profile.getName(),
							file });
	
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
											
											adapter.failed( new TranscodeProviderException( "Transcode cancelled" ));
											
										}else{
											
											adapter.failed( new TranscodeProviderException( "Transcode failed", (Throwable)status.get( "error" )));
										}
									}catch( Throwable e ){
										
										adapter.failed( new TranscodeProviderException( "Failed to get status", e ));
									}
								}
							}finally{
								
								pipe.destroy();
							}
						}
					}.start();
					
			
				return( 
					new TranscodeProviderJob()
					{
						public void
						pause()
						{
							pipe.pause();
						}
						
						public void
						resume()
						{
							pipe.resume();
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
					});
						
			}catch( Throwable e ){
				
				pipe.destroy();
				
				throw( e );
			}
		}catch( TranscodeProviderException e ){
			
			throw( e );
				
		}catch( Throwable e ){
			
			throw( new TranscodeProviderException( "transcode failed", e ));
		}
	}
	
	protected void
	destroy()
	{
		// TODO
	}
}
