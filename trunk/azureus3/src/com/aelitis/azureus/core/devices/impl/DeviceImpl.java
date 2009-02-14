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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DelayedEvent;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.LightHashMap;
import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;

import com.aelitis.azureus.core.devices.Device;
import com.aelitis.azureus.core.devices.DeviceManagerException;
import com.aelitis.azureus.core.devices.TranscodeException;
import com.aelitis.azureus.core.devices.TranscodeFile;
import com.aelitis.azureus.core.devices.TranscodeProfile;
import com.aelitis.azureus.core.devices.TranscodeProvider;
import com.aelitis.azureus.core.devices.TranscodeTargetListener;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.util.ImportExportUtils;

public abstract class 
DeviceImpl
	implements Device
{
	private static final String MY_PACKAGE = "com.aelitis.azureus.core.devices.impl";
	
	protected static DeviceImpl
	importFromBEncodedMapStatic(
		DeviceManagerImpl	manager,
		Map					map )
	
		throws IOException
	{
		String	impl = ImportExportUtils.importString( map, "_impl" );
		
		if ( impl.startsWith( "." )){
			
			impl = MY_PACKAGE + impl;
		}
		
		try{
			Class<DeviceImpl> cla = (Class<DeviceImpl>) Class.forName( impl );
			
			Constructor<DeviceImpl> cons = cla.getDeclaredConstructor( DeviceManagerImpl.class, Map.class );
			
			cons.setAccessible( true );
			
			return( cons.newInstance( manager, map ));
			
		}catch( Throwable e ){

			Debug.out( "Can't construct device for " + impl, e );
			
			throw( new IOException( "Construction failed: " + Debug.getNestedExceptionMessage(e)));
		}
	}
	
	private static final String PP_REND_WORK_DIR		= "tt_work_dir";
	private static final String PP_REND_TRANS_PROF		= "tt_trans_prof";
	private static final String PP_REND_DEF_TRANS_PROF	= "tt_def_trans_prof";

	
	private DeviceManagerImpl	manager;
	private int					type;
	private String				uid;
	private String 				name;
	private boolean				manual;
	
	private boolean			hidden;
	private long			last_seen;
	
	private boolean			online;
	
	private Map<String,Object>	persistent_properties = new LightHashMap<String, Object>(1);

	private Map<Object,Object>	transient_properties = new LightHashMap<Object, Object>(1);
	
	private long						last_load;
	private Map<String,Map<String,?>>	device_files;
	
	private CopyOnWriteList<TranscodeTargetListener>	listeners = new CopyOnWriteList<TranscodeTargetListener>();
	
	protected
	DeviceImpl(
		DeviceManagerImpl	_manager,
		int					_type,
		String				_uid,
		String				_name,
		boolean				_manual )
	{
		manager		= _manager;
		type		= _type;
		uid			= _uid;
		name		= _name;
		manual		= _manual;
	}
	
	protected
	DeviceImpl(
		DeviceManagerImpl	_manager,
		Map					map )
	
		throws IOException
	{
		manager	= _manager;
		
		type	= (int)ImportExportUtils.importLong( map, "_type" );
		uid		= ImportExportUtils.importString( map, "_uid" );
		name	= ImportExportUtils.importString( map, "_name" );
		
		last_seen	= ImportExportUtils.importLong( map, "_ls" );
		hidden		= ImportExportUtils.importBoolean( map, "_hide" );	
		manual		= ImportExportUtils.importBoolean( map, "_man" );

		if ( map.containsKey( "_pprops" )){
			
			persistent_properties = (Map<String,Object>)map.get( "_pprops" );
		}
	}
	
	protected void
	exportToBEncodedMap(
		Map					map )
	
		throws IOException
	{
		String	cla = this.getClass().getName();
		
		if ( cla.startsWith( MY_PACKAGE )){
			
			cla = cla.substring( MY_PACKAGE.length());
		}
		
		ImportExportUtils.exportString( map, "_impl", cla );
		ImportExportUtils.exportLong( map, "_type", new Long( type ));
		ImportExportUtils.exportString( map, "_uid", uid );
		ImportExportUtils.exportString( map, "_name", name );
		
		ImportExportUtils.exportLong( map, "_ls", new Long( last_seen ));
		ImportExportUtils.exportBoolean( map, "_hide", hidden );
		ImportExportUtils.exportBoolean( map, "_man", manual );
		
		map.put( "_pprops", persistent_properties );
	}
	
	public int
	getType()
	{
		return( type );
	}
	
	public String
	getID()
	{
		return( uid );
	}
	
	public Device
	getDevice()
	{
		return( this );
	}

	public String
	getName()
	{
		return( name );
	}
	
	public boolean
	isManual()
	{
		return( manual );
	}
	
	public boolean
	isHidden()
	{
		return( hidden );
	}
	
	public void
	setHidden(
		boolean		h )
	{
		if ( h != hidden ){
			
			hidden	= h;
			
			setDirty();
		}
	}
	
	protected void
	alive()
	{
		if ( !manual ){
			
			last_seen	= SystemTime.getCurrentTime();
			
			online	= true;
			
			setDirty( false );
		}
	}
	
	protected void
	dead()
	{
		if ( !manual ){
			
			online	= false;
			
			setDirty( false );
		}
	}
	
	protected boolean
	updateFrom(
		DeviceImpl		other )
	{
		if ( type != other.type || !uid.equals( other.uid )){
			
			Debug.out( "Inconsistent update operation" );
			
			return( false );
			
		}else{
			
			if ( !name.equals( other.name )){
				
				name	= other.name;
				
				setDirty();
			}
			
			alive();
			
			return( true );
		}
	}
	
	protected void
	setDirty()
	{
		setDirty( true );
	}
	
	protected void
	setDirty(
		boolean		save_changes )
	{
		manager.configDirty( this, save_changes );
	}
	
	protected void
	updateStatus()
	{	
	}
	
	public void 
	requestAttention() 
	{
		manager.requestAttention( this );
	}
	
	public TranscodeFile[]
	getFiles()
	{
		try{
			synchronized( this ){
				
				if ( device_files == null ){
					
					loadDeviceFile();
				}
				
				List<TranscodeFile> result = new ArrayList<TranscodeFile>();
								
				for (Map.Entry<String,Map<String,?>> entry: device_files.entrySet()){
					
					transcodeFile tf = new transcodeFile( entry.getKey(), entry.getValue());
										
					result.add( tf );
				}
				
				return( result.toArray( new TranscodeFile[ result.size() ]));
			}
		}catch( Throwable e ){
			
			Debug.out( e );
			
			return( new TranscodeFile[0] );
		}
	}
	
	public TranscodeFile
	allocateFile(
		TranscodeProfile		profile,
		DiskManagerFileInfo		file )
	
		throws TranscodeException
	{
		transcodeFile	result;
		
		try{
			synchronized( this ){
				
				if ( device_files == null ){
					
					loadDeviceFile();
				}
	
				String	key = ByteFormatter.encodeString( file.getDownloadHash() ) + ":" + file.getIndex();
				
				Map<String,?> existing = device_files.get( key );
				
				if ( existing != null ){
				
					result = new transcodeFile( key, existing );
					
				}else{
							
					String ext = profile.getFileExtension();
					
					String	target_file = file.getFile().getName();
					
					if ( ext != null ){
						
						int	pos = target_file.lastIndexOf( '.' );
						
						if ( pos != -1 ){
							
							target_file = target_file.substring( 0, pos ); 
						}
						
						target_file += ext;
					}
						
					Set<String> name_set = new HashSet<String>();
					
					for (Map<String,?> entry: device_files.values()){
						
						try{
							name_set.add( new File( ImportExportUtils.importString( entry, "file" )).getName());
							
						}catch( Throwable e ){
							
							Debug.out( e );
						}
					}
		
					for (int i=0;i<1024;i++){
						
						String	test_name = i==0?target_file:( i + "_" + target_file);
						
						if ( !name_set.contains( test_name )){
						
							target_file = test_name;
							
							break;
						}				
					}
					
					File output_file = getWorkingDirectory();
	
					output_file = new File( output_file.getAbsoluteFile(), target_file );
	
					result = new transcodeFile( key, output_file );
					
					device_files.put( key, result.toMap());
					
					saveDeviceFile();
				}
			}
		}catch( Throwable e ){
			
			throw( new TranscodeException( "File allocation failed", e ));
		}
		
		for ( TranscodeTargetListener l: listeners ){
			
			try{
				l.fileAdded( result );
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
		
		return( result );
	}
	
	public File
	getWorkingDirectory()
	{				
		String result = getPersistentStringProperty( PP_REND_WORK_DIR );
		
		if ( result.length() == 0 ){
			
			String	def_dir = COConfigurationManager.getStringParameter( "Default save path" );

			File f = new File( def_dir, "transcodes" );
			
			f.mkdirs();
			
			String	name = FileUtil.convertOSSpecificChars( getName(), true );
			
			for (int i=0;i<1024;i++){
				
				String test_name = name + (i==0?"":("_"+i));
				
				File test_file = new File( f, test_name );
				
				if ( !test_file.exists()){
			
					f = test_file;
					
					break;
				}
			}
			
			result = f.getAbsolutePath();
			
			setPersistentStringProperty( PP_REND_WORK_DIR, result );
		}
		
		File f_result = new File( result );
		
		if ( !f_result.exists()){
			
			f_result.mkdirs();
		}
		
		return( f_result );
	}
	
	public void
	setWorkingDirectory(
		File		directory )
	{
		setPersistentStringProperty( PP_REND_WORK_DIR, directory.getAbsolutePath());
	}

	public TranscodeProfile[]
	getTranscodeProfiles()
	{
		String[] uids = getPersistentStringListProperty( PP_REND_TRANS_PROF );
		
		List<TranscodeProfile>	profiles = new ArrayList<TranscodeProfile>();
		
		DeviceManagerImpl dm = getManager();
		
		TranscodeManagerImpl tm = dm.getTranscodeManager();
		
			// hack for the moment!!!!
		
		TranscodeProvider[] providers = tm.getProviders();
		
		if ( providers.length > 0 ){
			
			return( providers[0].getProfiles());
		}
		
		for ( String uid: uids ){
			
			TranscodeProfile profile = tm.getProfileFromUID( uid );
			
			if ( profile != null ){
				
				profiles.add( profile );
			}
		}
		
		return( profiles.toArray( new TranscodeProfile[profiles.size()] ));
	}
	
	public void
	setTranscodeProfiles(
		TranscodeProfile[]	profiles )
	{
		String[]	uids = new String[profiles.length];
		
		for (int i=0;i<profiles.length;i++){
			
			uids[i] = profiles[i].getUID();
		}
		
		setPersistentStringListProperty( PP_REND_TRANS_PROF, uids );
	}
	
	public TranscodeProfile
	getDefaultTranscodeProfile()
	
		throws TranscodeException
	{
		String uid = getPersistentStringProperty( PP_REND_DEF_TRANS_PROF );
		
		DeviceManagerImpl dm = getManager();
		
		TranscodeManagerImpl tm = dm.getTranscodeManager();

		TranscodeProfile profile = tm.getProfileFromUID( uid );
		
		if ( profile != null ){
			
			return( profile );
		}
		
		log( "Default transcode profile for " + getName() + " not found, picking first" );
			
		TranscodeProfile[] profiles = getTranscodeProfiles();
			
		if ( profiles.length == 0 ){
			
			throw( new TranscodeException( "No profiles available" ));			
		}
		
		return( profiles[0] );
	}
	
	public void
	setDefaultTranscodeProfile(
		TranscodeProfile		profile )
	{
		setPersistentStringProperty( PP_REND_DEF_TRANS_PROF, profile.getUID());
	}
	
	public String[][] 
	getDisplayProperties() 
	{
		List<String[]> dp = new ArrayList<String[]>();
		
	    getDisplayProperties( dp );
	    	    
	    String[][] res = new String[2][dp.size()];
	   
	    int	pos = 0;
	    
	    for ( String[] entry: dp ){
	    
	    	res[0][pos] = entry[0];
	    	res[1][pos] = entry[1];
	    	
	    	pos++;
	    }
	    
	    return( res );
	}
	
	protected void
	getDisplayProperties(
		List<String[]>	dp )
	{
		addDP( dp, "TableColumn.header.name", name );
		
		if ( !manual ){
		
			addDP( dp, "azbuddy.ui.table.online",  online );
		
			addDP( dp, "device.lastseen", last_seen==0?"":new SimpleDateFormat().format(new Date( last_seen )));
		}
	}
	
	protected void
	addDP(
		List<String[]>	dp,
		String			name,
		String			value )
	{
		dp.add( new String[]{ name, value });
	}
	
	protected void
	addDP(
		List<String[]>	dp,
		String			name,
		String[]		values )
	{
		String value = "";
		
		for ( String v: values ){
			
			value += (value.length()==0?"":",") + v;
		}
		
		dp.add( new String[]{ name, value });
	}
	
	protected void
	addDP(
		List<String[]>	dp,
		String			name,
		boolean			value )
	{
		dp.add( new String[]{ name, MessageText.getString( value?"GeneralView.yes":"GeneralView.no" ) });
	}
	
	
	protected void
	addDP(
		List<String[]>		dp,
		String				name,
		TranscodeProfile	value )
	{
		addDP( dp, name, value==null?"":value.getName());
	}
	
	protected void
	addDP(
		List<String[]>		dp,
		String				name,
		TranscodeProfile[]	values )
	{
		String[]	names = new String[values.length];
		
		for (int i=0;i<values.length;i++){
			
			names[i] = values[i].getName();
		}
		
		addDP( dp, name, names);
	}
	
	public void
	remove()
	{
		manager.removeDevice( this );
	}
	
	public String
	getPersistentStringProperty(
		String		prop )
	{
		try{
			byte[]	value = (byte[])persistent_properties.get( prop );
	
			if ( value == null ){
				
				return( "" );
			}
			
			return( new String( value, "UTF-8" ));
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
			
			return( "" );
		}
	}
	
	public void
	setPersistentStringProperty(
		String		prop,
		String		value )
	{
		try{
			persistent_properties.put( prop, value.getBytes( "UTF-8" ));
			
			setDirty();
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
	
	public String[]
	getPersistentStringListProperty(
		String		prop )
	{
		try{
			List<byte[]>	values = (List<byte[]>)persistent_properties.get( prop );
	
			if ( values == null ){
				
				return( new String[0] );
			}
			
			String[]	res = new String[values.size()];
			
			int	pos = 0;
			
			for (byte[] value: values ){
			
				res[pos++] = new String( value, "UTF-8" );
			}
			
			return( res );
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
			
			return( new String[0] );
		}
	}
	
	public void
	setPersistentStringListProperty(
		String			prop,
		String[]		values )
	{
		try{
			List<byte[]> values_list = new ArrayList<byte[]>();
			
			for (String value: values ){
				
				values_list.add( value.getBytes( "UTF-8" ));
			}
			
			persistent_properties.put( prop, values_list );
			
			setDirty();
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
	
	public void
	setTransientProperty(
		Object		key,
		Object		value )
	{
		synchronized( transient_properties ){
			
			transient_properties.put( key, value );
		}
	}
	
	public Object
	getTransientProperty(
		Object		key )
	{
		synchronized( transient_properties ){
			
			return( transient_properties.get( key ));
		}
	}
	
	protected void
	loadDeviceFile()
	
		throws IOException
	{
		last_load = SystemTime.getMonotonousTime();
		
		Map	map = FileUtil.readResilientFile( getDeviceFile());

		device_files = (Map<String,Map<String,?>>)map.get( "files" );
		
		if ( device_files == null ){
			
			device_files = new HashMap<String, Map<String,?>>();
		}
		
		System.out.println( "Loaded device file for " + getName() + ": files=" + device_files.size());
		
		final int GC_TIME = 15000;
		
		new DelayedEvent( 
			"Device:gc", 
			GC_TIME,
			new AERunnable()
			{
				public void
				runSupport()
				{
					synchronized( DeviceImpl.this ){
						
						if ( SystemTime.getMonotonousTime() - last_load >= GC_TIME ){
														
							device_files = null;
							
						}else{
							
							new DelayedEvent( "Device:gc2", GC_TIME, this );
						}
					}
				}
			});
	}
	
	protected void
	saveDeviceFile()
	
		throws IOException
	{
		if ( device_files == null || device_files.size()==0 ){
			
			FileUtil.deleteResilientFile( getDeviceFile());
			
		}else{
			Map map = new HashMap();
			
			map.put( "files", device_files );
			
			FileUtil.writeResilientFile( getDeviceFile(), map );
		}
	}
	
	protected File
	getDeviceFile()
	
		throws IOException
	{
 		File dir = getDevicesDir();
 		
 		return( new File( dir, FileUtil.convertOSSpecificChars(getID(),false) + ".dat" ));
	}
	
	protected File
	getDevicesDir()
	
		throws IOException
	{
		File dir = new File(SystemProperties.getUserPath());

		dir = new File( dir, "devices" );
 		
 		if ( !dir.exists()){
 			
 			if ( !dir.mkdirs()){
 				
 				throw( new IOException( "Failed to create '" + dir + "'" ));
 			}
 		}	
 		
 		return( dir );
	}
	
	protected DeviceManagerImpl
	getManager()
	{
		return( manager );
	}

	public void
	addListener(
		TranscodeTargetListener		listener )
	{
		listeners.add( listener );
	}
	
	public void
	removeListener(
		TranscodeTargetListener		listener )
	{
		listeners.remove( listener );
	}
	
	protected void
	log(
		String		str )
	{
		manager.log( str );
	}
	
	protected void
	log(
		String		str,
		Throwable	e )
	{
		manager.log( str, e );
	}
	
	public String
	getString()
	{
		return( "type=" + type + ",uid=" + uid + ",name=" + name );
	}
	
	protected class
	browseLocationImpl
		implements browseLocation
	{
		private String		name;
		private URL			url;
		
		protected
		browseLocationImpl(
			String		_name,
			URL			_url )
		{
			name		= _name;
			url			= _url;
		}
		
		public String
		getName()
		{
			return( name );
		}
		
		public URL
		getURL()
		{
			return( url );
		}
	}
	
	protected class
	transcodeFile
		implements TranscodeFile
	{
		private String				key;
		private Map<String,?>		map;
		
		protected 
		transcodeFile(
			String		_key,
			File		file )
		{
			key	= _key;
			map	= new HashMap<String, Object>();
			
			setString( "file", file.getAbsolutePath());
		}
		
		protected
		transcodeFile(
			String			_key,
			Map<String,?>	_map )
		{
			key		= _key;
			map		= _map;
		}
		
		protected Map<String,?>
		toMap()
		{
			return( map );
		}
		
		public File 
		getFile() 
		{
			return(new File(getString("file")));
		}
		
		protected String
		getString(
			String		key )
		{
			try{
				return(ImportExportUtils.importString( map, key ));
				
			}catch( Throwable e ){
				
				Debug.out( e );
				
				return( "" );
			}
		}
		
		protected void
		setString(
			String		key,
			String		value )
		{
			try{
				ImportExportUtils.exportString(map, "file", value);
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
	}
}
