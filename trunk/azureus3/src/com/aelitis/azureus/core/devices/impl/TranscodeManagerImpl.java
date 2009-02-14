/*
 * Created on Feb 4, 2009
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SHA1Simple;
import org.gudy.azureus2.plugins.PluginEvent;
import org.gudy.azureus2.plugins.PluginEventListener;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ipc.IPCInterface;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.content.AzureusContentFile;
import com.aelitis.azureus.core.devices.*;
import com.aelitis.azureus.core.download.DiskManagerFileInfoFile;
import com.aelitis.azureus.core.download.DiskManagerFileInfoStream;
import com.aelitis.azureus.core.util.CopyOnWriteList;

public class 
TranscodeManagerImpl
	implements TranscodeManager
{
	private DeviceManagerImpl		device_manager;
	private AzureusCore				azureus_core;
	
	private TranscodeProviderVuze	vuzexcode_provider;
	
	private CopyOnWriteList<TranscodeManagerListener>	listeners = new CopyOnWriteList<TranscodeManagerListener>();
	
	private TranscodeQueueImpl		queue = new TranscodeQueueImpl( this );
	
	private AESemaphore	init_sem = new AESemaphore( "TM:init" );
	
	protected
	TranscodeManagerImpl(
		DeviceManagerImpl		_dm )
	{
		device_manager	= _dm;
		
		azureus_core = AzureusCoreFactory.getSingleton();
		
		final PluginManager pm = azureus_core.getPluginManager();
		
		final PluginInterface default_pi = pm.getDefaultPluginInterface();
		
		default_pi.addListener(
			new PluginListener()
			{
				public void
				initializationComplete()
				{
					test(default_pi);
					
					default_pi.addEventListener(
						new PluginEventListener()
						{
							public void 
							handleEvent(
								PluginEvent ev )
							{
								int	type = ev.getType();
								
								if ( type == PluginEvent.PEV_PLUGIN_OPERATIONAL ){
									
									pluginAdded((PluginInterface)ev.getValue());
								}
								if ( type == PluginEvent.PEV_PLUGIN_NOT_OPERATIONAL ){
									
									pluginRemoved((PluginInterface)ev.getValue());
								}
							}
						});
					
					PluginInterface[] plugins = pm.getPlugins();
					
					for ( PluginInterface pi: plugins ){
						
						if ( pi.getPluginState().isOperational()){
						
							pluginAdded( pi );
						}
					}
					
					queue.initialise();
					
					init_sem.releaseForever();
				}
				
				public void
				closedownInitiated()
				{	
				}
				
				public void
				closedownComplete()
				{
				}
			});
	}
	
	protected void
	test(
		PluginInterface plugin_interface )
	{
		try{
			PluginInterface av_pi = plugin_interface.getPluginManager().getPluginInterfaceByID( "azupnpav" );
			
			if ( av_pi == null ){
			
				throw( new TranscodeException( "Media Server plugin not found" ));
			}
			
			IPCInterface av_ipc = av_pi.getIPC();

			final File source_file = new File( "c:\\test\\custom1\\Fast_and_Furious_4__Vin_Diesel[TVG00016080].mkv" );
			
			if ( !source_file.exists()){
				
				return;
			}
			
			final DiskManagerFileInfo source = new DiskManagerFileInfoFile( source_file );
			
			final DiskManagerFileInfo stream_file = 
				new DiskManagerFileInfoStream( 
					new DiskManagerFileInfoStream.streamFactory()
					{
						private List<Object>	current_requests = new ArrayList<Object>();
						
						public InputStream 
						getStream(
							Object		request )
						
							throws IOException 
						{
							TranscodeTarget target = null;
							
							for ( Device device: device_manager.getDevices()){
								
								if ( device instanceof TranscodeTarget ){
									
									target = (TranscodeTarget)device;
								}
							}
							
							if ( target == null ){
								
								throw( new IOException( "No transcode target found!!!!" ));
							}
							
							TranscodeProfile profile = null;
							
							for (TranscodeProvider provider: getProviders()){
				
								TranscodeProfile[] profiles = provider.getProfiles();
								
								if ( profiles.length > 0 ){
									
									profile = profiles[0];
									
									for ( TranscodeProfile p: profiles ){
										
										if ( p.getName().toLowerCase().contains( "wii" )){
											
											profile = p;
										}
									}
								}
							}
							
							if ( profile == null ){
								
								throw( new IOException( "No transcode profiles found!!!!" ));
							}
							
							TranscodeJobImpl job = queue.add(
								target,
								profile, 
								source, 
								true );
							
							try{
								synchronized( this ){
								
									current_requests.add( request );
								}
								
								while( true ){
									
									InputStream is = job.getStream( 1000 );
									
									if ( is != null ){
										
										return( is );
									}
									
									int	state = job.getState();
									
									if ( state == TranscodeJobImpl.ST_FAILED ){
										
										throw( new IOException( "Transcode failed: " + job.getError()));
										
									}else if ( state == TranscodeJobImpl.ST_CANCELLED ){
										
										throw( new IOException( "Transcode failed: job cancelled" ));

									}else if ( state == TranscodeJobImpl.ST_COMPLETE ){
										
										throw( new IOException( "Job complete but no stream!" ));
									}
									
									synchronized( this ){
										
										if ( !current_requests.contains( request )){
											
											break;
										}
									}
									
									System.out.println( "waiting for stream" );
									
								}
								
								IOException error = new IOException( "Stream request cancelled" );
								
								job.failed( error );
								
								throw( error );
								
							}finally{
								
								synchronized( this ){
								
									current_requests.remove( request );
								}
							}
						}
						
						public void 
						destroyed(
							Object request ) 
						{
							synchronized( this ){
								
								current_requests.remove( request );
							}
						}
					},
					new File( "c:\\test\\custom1\\Fast_and_Furious_4__Vin_Diesel[TVG00016080].mkv.flv" ));
			
			AzureusContentFile	content = 
				new AzureusContentFile()
				{
					public byte[]
				   	getHash()
					{
						try{
							return( stream_file.getDownloadHash());
							
						}catch( Throwable e ){
							
							e.printStackTrace();
							
							return( null );
						}
					}
				        	
				   	public DiskManagerFileInfo
				   	getFile()
				   	{
				   		return( stream_file );
				   	}
				};
				
			av_ipc.invoke( "addContent", new Object[]{ content });
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	protected void
	pluginAdded(
		PluginInterface		pi )
	{
		if ( pi.getPluginState().isBuiltIn()){
			
			return;
		}
		
		String plugin_id = pi.getPluginID();
		
		if ( plugin_id.equals( "vuzexcode" )){
			
			boolean		added		= false;
			boolean		updated		= false;
			
			TranscodeProviderVuze provider	= null;
			
			synchronized( this ){
				
				if ( vuzexcode_provider == null ){
					
					provider = vuzexcode_provider = new TranscodeProviderVuze( pi );
			
					added = true;
					
				}else if ( pi != vuzexcode_provider ){
					
					provider = vuzexcode_provider;
					
					vuzexcode_provider.update( pi );
					
					updated = true;
				}
			}
			
			if ( added ){
				
				for ( TranscodeManagerListener listener: listeners ){
					
					try{
						listener.providerAdded( provider );
						
					}catch( Throwable e ){
						
						Debug.out( e );
					}
				}
			}else if ( updated ){
				
				for ( TranscodeManagerListener listener: listeners ){
					
					try{
						listener.providerUpdated( provider );
						
					}catch( Throwable e ){
						
						Debug.out( e );
					}
				}
			}
		}
	}
	
	protected void
	pluginRemoved(
		PluginInterface		pi )
	{
		String plugin_id = pi.getPluginID();
		
		if ( plugin_id.equals( "vuzexcode" )){
			
			TranscodeProviderVuze provider	= null;

			synchronized( this ){
				
				if ( vuzexcode_provider != null ){

					provider = vuzexcode_provider;
					
					vuzexcode_provider.destroy();
					
					vuzexcode_provider = null;
				}
			}
			
			if ( provider != null ){
				
				for ( TranscodeManagerListener listener: listeners ){
					
					try{
						listener.providerRemoved( provider );
						
					}catch( Throwable e ){
						
						Debug.out( e );
					}
				}
			}
		}
	}
	
	public TranscodeProvider[]
	getProviders()
	{
		TranscodeProviderVuze	vp = vuzexcode_provider;

		if ( vp == null ){
		
			return( new TranscodeProvider[0] );
		}
		
		return( new TranscodeProvider[]{ vp });
	}
	
	protected TranscodeProfile
	getProfileFromUID(
		String		uid )
	{
		for ( TranscodeProvider provider: getProviders()){
			
			TranscodeProfile profile = provider.getProfile( uid );
			
			if ( profile != null ){
				
				return( profile );
			}
		}
		
		return( null );
	}
	
	public TranscodeQueue 
	getQueue() 
	{
		if ( !init_sem.reserve(10000)){
			
			Debug.out( "Timeout waiting for init" );
		}
		
		return( queue );
	}
	
	protected TranscodeTarget
	lookupTarget(
		String		target_id )
	
		throws TranscodeException
	{
		Device device = device_manager.getDevice( target_id );
		
		if ( device instanceof TranscodeTarget ){
			
			return((TranscodeTarget)device);
		}
		
		throw( new TranscodeException( "Transcode target with id " + target_id + " not found" ));
	}
	
	protected DiskManagerFileInfo
	lookupFile(
		byte[]		hash,
		int			index )
	
		throws TranscodeException
	{
		try{
			Download download = azureus_core.getPluginManager().getDefaultPluginInterface().getDownloadManager().getDownload( hash );
			
			if ( download == null ){
				
				throw( new TranscodeException( "Download with hash " + ByteFormatter.encodeString( hash ) + " not found" ));
			}
		
			return( download.getDiskManagerFileInfo()[index]);
			
		}catch( Throwable e ){
			
			throw( new TranscodeException( "Download with hash " + ByteFormatter.encodeString( hash ) + " not found", e ));

		}
	}
	
	protected void
	close()
	{
		queue.close();
	}
	
	public void
	addListener(
		TranscodeManagerListener		listener )
	{
		listeners.add( listener );
	}
	
	public void
	removeListener(
		TranscodeManagerListener		listener )
	{
		listeners.remove( listener );
	}
	
	protected void
	log(
		String	str )
	{
		device_manager.log( "Trans: " + str );
	}
	
	protected void
	log(
		String		str,
		Throwable	e )
	{
		device_manager.log( "Trans: " + str, e );
	}
}
