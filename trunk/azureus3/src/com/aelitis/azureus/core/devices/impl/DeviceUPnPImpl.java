/*
 * Created on Jan 28, 2009
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

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.ipc.IPCInterface;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.utils.StaticUtilities;

import com.aelitis.azureus.core.content.AzureusContentDownload;
import com.aelitis.azureus.core.content.AzureusContentFile;
import com.aelitis.azureus.core.devices.TranscodeException;
import com.aelitis.azureus.core.devices.TranscodeFile;
import com.aelitis.azureus.core.devices.TranscodeProfile;
import com.aelitis.azureus.core.devices.TranscodeTarget;
import com.aelitis.azureus.core.devices.TranscodeTargetListener;
import com.aelitis.azureus.core.download.DiskManagerFileInfoFile;
import com.aelitis.azureus.core.download.DiskManagerFileInfoStream;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.core.util.UUIDGenerator;
import com.aelitis.net.upnp.UPnPDevice;
import com.aelitis.net.upnp.UPnPRootDevice;

public abstract class 
DeviceUPnPImpl
	extends DeviceImpl
	implements TranscodeTargetListener, DownloadManagerListener
{
	private static final Object UPNPAV_FILE_KEY = new Object();
	
	private static final String MY_ACF_KEY = "DeviceUPnPImpl:device";
	
	protected static String
	getDisplayName(
		UPnPDevice		device )
	{
		UPnPDevice	root = device.getRootDevice().getDevice();
		
		String fn = root.getFriendlyName();
		
		if ( fn == null || fn.length() == 0 ){
			
			fn = device.getFriendlyName();
		}
		
		String	dn = root.getModelName();
		
		if ( dn == null || dn.length() == 0 ){
		
			dn = device.getModelName();
		}
		
		if ( dn != null && dn.length() > 0 ){
			
			if ( !fn.contains( dn ) && ( !dn.contains( "Azureus" ) || dn.contains( "Vuze" ))){
			
				fn += " (" + dn + ")";
			}
		}
		
		return( fn );
	}
	
	
	
	private final DeviceManagerUPnPImpl	upnp_manager;
	private volatile UPnPDevice		device_may_be_null;
	
	private IPCInterface		upnpav_ipc;
	private TranscodeProfile	dynamic_transcode_profile;
	private Map<String,AzureusContentFile>	dynamic_xcode_map;
	
	
	protected
	DeviceUPnPImpl(
		DeviceManagerImpl		_manager,
		UPnPDevice				_device,
		int						_type )
	{
		super( _manager, _type, _type + "/" + _device.getRootDevice().getUSN(), getDisplayName( _device ), false );
		
		upnp_manager		= _manager.getUPnPManager();
		device_may_be_null 	= _device;
	}	
	
	protected
	DeviceUPnPImpl(
		DeviceManagerImpl	_manager,
		int					_type,
		String				_name )

	{
		super( _manager, _type, UUIDGenerator.generateUUIDString(), _name, true );
		
		upnp_manager		= _manager.getUPnPManager();
	}
	
	protected
	DeviceUPnPImpl(
		DeviceManagerImpl	_manager,
		int					_type,
		String				_uuid,
		String				_name,
		boolean				_manual )

	{
		super( _manager, _type, _uuid, _name, _manual );
		
		upnp_manager		= _manager.getUPnPManager();
	}
	
	protected
	DeviceUPnPImpl(
		DeviceManagerImpl	_manager,
		Map					_map )
	
		throws IOException
	{
		super(_manager, _map );
		
		upnp_manager		= _manager.getUPnPManager();
	}
	
	protected void
	initialise()
	{
		super.initialise();
	}
	
	protected void
	destroy()
	{
		super.destroy();
	}
	
	protected boolean
	updateFrom(
		DeviceImpl		_other )
	{
		if ( !super.updateFrom( _other )){
			
			return( false );
		}
		
		if ( !( _other instanceof DeviceUPnPImpl )){
			
			Debug.out( "Inconsistent" );
			
			return( false );
		}
		
		DeviceUPnPImpl other = (DeviceUPnPImpl)_other;
		
		device_may_be_null	= other.device_may_be_null;
		
		return( true );
	}
	
	protected UPnPDevice
	getUPnPDevice()
	{
		return( device_may_be_null );
	}
	
	public boolean
	isBrowsable()
	{
		return( true );
	}
	
	public browseLocation[]
	getBrowseLocations()
	{
		List<browseLocation>	locs = new ArrayList<browseLocation>();
	
		UPnPDevice device = device_may_be_null;
		
		if ( device != null ){
			
			URL		presentation = getPresentationURL( device );

			if ( presentation != null ){
					
				locs.add( new browseLocationImpl( "device.upnp.present_url", presentation ));
			}
			
			locs.add( new browseLocationImpl( "device.upnp.desc_url", device.getRootDevice().getLocation()));
		}
		
		return( locs.toArray( new browseLocation[ locs.size() ]));
	}
	
	public boolean
	canFilterFilesView()
	{
		return( true );
	}
	
	public void
	setFilterFilesView(
		boolean	filter )
	{
		setPersistentBooleanProperty( PP_FILTER_FILES, filter );
	}
	
	public boolean
	getFilterFilesView()
	{
		return( getPersistentBooleanProperty( PP_FILTER_FILES, true ));
	}
	
	protected URL
	getLocation()
	{
		UPnPDevice device = device_may_be_null;
		
		if ( device != null ){
			
			UPnPRootDevice root = device.getRootDevice();
			
			return( root.getLocation());
		}
		
		return( null );
	}
	
	protected InetAddress
	getAddress()
	{
		try{

			UPnPDevice device = device_may_be_null;
	
			if ( device != null ){
				
				UPnPRootDevice root = device.getRootDevice();
				
				URL location = root.getLocation();
				
				return( InetAddress.getByName( location.getHost() ));
				
			}else{
				
				InetAddress address = (InetAddress)getTransientProperty( TP_IP_ADDRESS );
				
				if ( address != null ){
					
					return( address );
				}
				
				String last = getPersistentStringProperty( PP_IP_ADDRESS );
				
				if ( last != null && last.length() > 0 ){
					
					return( InetAddress.getByName( last ));
				}
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		
		}
		
		return( null );
	}
	
	protected void
	setAddress(
		InetAddress	address )
	{
		setTransientProperty( TP_IP_ADDRESS, address );
		
		setPersistentStringProperty( PP_IP_ADDRESS, address.getHostAddress());
	}
	
	protected void
	browseReceived(
		TranscodeProfile	_dynamic_transcode_profile )
	{
		IPCInterface ipc = upnp_manager.getUPnPAVIPC();
		
		if ( ipc == null ){
			
			return;
		}
		
		synchronized( this ){
			
			if ( upnpav_ipc != null ){
				
				return;
			}
			
			upnpav_ipc = ipc;
			
			dynamic_transcode_profile	= _dynamic_transcode_profile;
		}
		
		if ( dynamic_transcode_profile != null && this instanceof TranscodeTarget ){
			
			DownloadManager dm = StaticUtilities.getDefaultPluginInterface().getDownloadManager();
			
			dm.addListener( this, true );
		}
				
		addListener( this );

		TranscodeFile[]	transcode_files = getFiles();
		
		for ( TranscodeFile file: transcode_files ){
			
			fileAdded( file );
		}
	}
	
	protected void
	resetUPNPAV()
	{		
		synchronized( this ){
			
			if ( upnpav_ipc == null ){
				
				return;
			}
			
			upnpav_ipc = null;
			
			dynamic_transcode_profile = null;
			
			dynamic_xcode_map = null;
		 
			DownloadManager dm = StaticUtilities.getDefaultPluginInterface().getDownloadManager();

			dm.removeListener( this );
			
			removeListener( this );
			
			TranscodeFile[]	transcode_files = getFiles();
			
			for ( TranscodeFile file: transcode_files ){

				file.setTransientProperty( UPNPAV_FILE_KEY, null );
			}
		}
	}
	
	public void
	downloadAdded(
		Download	download )
	{
		Torrent torrent = download.getTorrent();
		
		if ( torrent != null && PlatformTorrentUtils.isContent( torrent, false )){
								
			addDynamicXCode( download.getDiskManagerFileInfo()[0]);
		}
	}
	
	public void
	downloadRemoved(
		Download	download )
	{
		Torrent torrent = download.getTorrent();
		
		if ( torrent != null && PlatformTorrentUtils.isContent( torrent, false )){
								
			removeDynamicXCode( download.getDiskManagerFileInfo()[0]);
		}
	}
		
	protected void
	addDynamicXCode(
		final DiskManagerFileInfo		source )
	{
		final TranscodeProfile profile = dynamic_transcode_profile;
		
		IPCInterface			ipc	= upnpav_ipc;
		
		if ( profile == null || ipc == null ){
			
			return;
		}
		
		try{
			TranscodeFileImpl transcode_file = allocateFile( profile, source );
			
			AzureusContentFile acf = (AzureusContentFile)transcode_file.getTransientProperty( UPNPAV_FILE_KEY );

			if ( acf != null ){
				
				return;
			}
			
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
							try{
								TranscodeJobImpl job = getManager().getTranscodeManager().getQueue().add(
										(TranscodeTarget)DeviceUPnPImpl.this,
										profile, 
										source, 
										true );
									
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
								
							}catch( IOException e ){
								
								throw( e );
								
							}catch( Throwable e ){
								
								throw( new IOException( "Failed to add transcode job: " + Debug.getNestedExceptionMessage(e)));
								
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
					transcode_file.getCacheFile());
			
			final Map<String,Object> properties =  new HashMap<String, Object>();
				
			acf =	new AzureusContentFile()
					{	
					   	public DiskManagerFileInfo
					   	getFile()
					   	{
					   		return( stream_file );
					   	}
					   	
						public Object
						getProperty(
							String		name )
						{
								// TODO: duration etc

							if ( name.equals( MY_ACF_KEY )){
								
								return( DeviceUPnPImpl.this );
							}
							
							return( null );
						}
					};
			
			synchronized( this ){
				
				if ( dynamic_xcode_map == null ){
					
					dynamic_xcode_map = new HashMap<String,AzureusContentFile>();
				}
				
				dynamic_xcode_map.put( transcode_file.getKey(), acf );
			}
			
			ipc.invoke( "addContent", new Object[]{ acf });
			
			transcode_file.setTransientProperty( UPNPAV_FILE_KEY, acf );

		}catch( Throwable e ){
			
			Debug.out( e );
		}
	}
	
	protected void
	removeDynamicXCode(
		final DiskManagerFileInfo		source )
	{
		final TranscodeProfile profile = dynamic_transcode_profile;
		
		IPCInterface			ipc	= upnpav_ipc;
		
		if ( profile == null || ipc == null ){
			
			return;
		}
		
		try{
			TranscodeFileImpl transcode_file = allocateFile( profile, source );

			if ( !transcode_file.isComplete()){
				
				AzureusContentFile acf = null;
				
				synchronized( this ){
	
					if ( dynamic_xcode_map != null ){
					
						acf = dynamic_xcode_map.get( transcode_file.getKey());
					}
				}
				
				transcode_file.delete( true );
				
				if ( acf != null ){
				
					ipc.invoke( "removeContent", new Object[]{ acf });
				}
			}
		}catch( Throwable e ){
			
			Debug.out( e );
		}
	}
	
	protected boolean
	isVisible(
		AzureusContentDownload		file )
	{
		return( !getFilterFilesView());
	}
	
	protected boolean
	isVisible(
		AzureusContentFile		file )
	{	
		boolean	result;
		
		if ( getFilterFilesView()){
		
			result = file.getProperty( MY_ACF_KEY ) == this;
			
		}else{
			
			result = true;
		}
		
		System.out.println( file.getFile().getFile().getName() + " -> " + result );
		
		return( result );
	}
	
	public void
	fileAdded(
		TranscodeFile		_transcode_file )
	{
		TranscodeFileImpl	transcode_file = (TranscodeFileImpl)_transcode_file;
		
		IPCInterface ipc = upnpav_ipc;
		
		synchronized( this ){
			
			if ( ipc == null ){

				return;
			}
			
			if ( !transcode_file.isComplete()){
				
				return;
			}
			
			AzureusContentFile acf = (AzureusContentFile)transcode_file.getTransientProperty( UPNPAV_FILE_KEY );
			
			if ( acf != null ){
				
				return;
			}

			try{
				final DiskManagerFileInfo 	f 		= transcode_file.getTargetFile();
				final String				tf_key	= transcode_file.getKey();
							
				acf = 
					new AzureusContentFile()
					{
						public DiskManagerFileInfo
					    getFile()
						{
							return( f );
						}
						
						public Object
						getProperty(
							String		name )
						{						
							if(  name.equals( MY_ACF_KEY )){
								
								return( DeviceUPnPImpl.this );
								
							}else{
								
								TranscodeFileImpl	tf = getTranscodeFile( tf_key );
								
								if ( tf != null ){
									
									long	res = 0;
									
									if ( name.equals( PT_DURATION )){
										
										res = tf.getDurationMillis();
										
									}else if ( name.equals( PT_VIDEO_WIDTH )){
										
										res = tf.getVideoWidth();
										
									}else if ( name.equals( PT_VIDEO_HEIGHT )){
										
										res = tf.getVideoHeight();
										
									}else if ( name.equals( PT_DATE )){

										res = tf.getCreationDateMillis();
									}
									
									if ( res > 0 ){
										
										return( new Long( res ));
									}
								}
							}
							
							return( null );
						}
					};
					
				try{
					ipc.invoke( "addContent", new Object[]{ acf });
				
					transcode_file.setTransientProperty( UPNPAV_FILE_KEY, acf );
					
				}catch( Throwable e ){
					
					Debug.out( e );
				}		
			}catch( TranscodeException e ){
				// file deleted
			}
		}
	}
	
	public void
	fileChanged(
		TranscodeFile		file,
		int					type,
		Object				data )
	{
		if ( file.isComplete()){
			
			fileAdded( file );
		}
	}
	
	public void
	fileRemoved(
		TranscodeFile		file )
	{
		IPCInterface ipc = upnp_manager.getUPnPAVIPC();
		
		if ( ipc == null ){
			
			return;
		}

		synchronized( this ){

			AzureusContentFile acf = (AzureusContentFile)file.getTransientProperty( UPNPAV_FILE_KEY );

			if ( acf == null ){
		
				return;
			}
			
			file.setTransientProperty( UPNPAV_FILE_KEY, null );

			try{
				ipc.invoke( "removeContent", new Object[]{ acf });
			
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
	}
	
	protected URL
	getPresentationURL(
		UPnPDevice		device )
	{
		String	presentation = device.getRootDevice().getDevice().getPresentation();
		
		if ( presentation != null ){
			
			try{
				URL url = new URL( presentation );
				
				return( url );

			}catch( Throwable e ){				
			}
		}
		
		return( null );
	}
	
	protected void
	getDisplayProperties(
		List<String[]>	dp )
	{
		super.getDisplayProperties( dp );
		
		UPnPDevice device = device_may_be_null;
		
		if ( device != null ){
			
			UPnPRootDevice root = device.getRootDevice();
			
			URL location = root.getLocation();
			
			addDP( dp, "dht.reseed.ip", location.getHost() + ":" + location.getPort()); 
	
			String	model_details 	= device.getModelName();
			String	model_url		= device.getModelURL();
			
			if ( model_url != null && model_url.length() > 0 ){
				model_details += " (" + model_url + ")";
			}
			
			String	manu_details 	= device.getManufacturer();
			String	manu_url		= device.getManufacturerURL();
			
			if ( manu_url != null && manu_url.length() > 0 ){
				manu_details += " (" + manu_url + ")";
			}
			
			addDP( dp, "device.model.desc", device.getModelDescription());
			addDP( dp, "device.model.name", model_details );
			addDP( dp, "device.model.num", device.getModelNumber());
			addDP( dp, "device.manu.desc", manu_details );
		}else{
			
			InetAddress ia = getAddress();
			
			if ( ia != null ){
				
				addDP( dp, "dht.reseed.ip", ia.getHostAddress()); 
			}
		}
	}
}
