/*
 * Created on Jul 9, 2013
 * Created by Paul Gardner
 * 
 * Copyright 2013 Azureus Software, Inc.  All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */


package org.gudy.azureus2.pluginsimpl.local.download;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.disk.DiskManagerFactory;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.download.DownloadRemovalVetoException;
import org.gudy.azureus2.plugins.download.DownloadStub;
import org.gudy.azureus2.plugins.download.DownloadStub.DownloadStubEx;
import org.gudy.azureus2.plugins.download.DownloadStub.DownloadStubFile;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;

import com.aelitis.azureus.util.MapUtils;

public class 
DownloadStubImpl
	implements DownloadStubEx
{
	private final DownloadManagerImpl		manager;
	private final String					name;
	private final byte[]					hash;
	private final long						size;
	private final long						date_created;
	private final String					save_path;
	private final DownloadStubFileImpl[]	files;
	private final Map<String,Object>		gm_map;
	
	private DownloadImpl			temp_download;
	private Map<String,Object>		attributes;
	
	protected
	DownloadStubImpl(
		DownloadManagerImpl		_manager,
		DownloadImpl			_download,
		Map<String,Object>		_gm_map )
	{
		manager			= _manager;
		temp_download	= _download;
		
		date_created = SystemTime.getCurrentTime();
				
		name	= temp_download.getName();
		
		Torrent	torrent = temp_download.getTorrent();
		
		hash		= torrent.getHash(); 
		size		= torrent.getSize();
		save_path	= temp_download.getSavePath();
		
		DownloadStubFile[] _files = temp_download.getStubFiles();

		gm_map		= _gm_map;
		
		files		= new DownloadStubFileImpl[_files.length];
		
		for ( int i=0;i<files.length;i++){
			
			files[i] = new DownloadStubFileImpl( _files[i] );
		}
	}
	
	protected
	DownloadStubImpl(
		DownloadManagerImpl		_manager,
		Map<String,Object>		_map )
	{
		manager		= _manager;
		
		date_created	= MapUtils.getMapLong( _map, "dt", 0 );
		hash 			= (byte[])_map.get( "hash" );
		name			= MapUtils.getMapString( _map, "name", null );
		size 			= MapUtils.getMapLong( _map, "s", 0 );
		save_path		= MapUtils.getMapString( _map, "l", null );
		gm_map 			= (Map<String,Object>)_map.get( "gm" );
		
		List<Map<String,Object>>	file_list = (List<Map<String,Object>>)_map.get( "files" );
		
		if ( file_list == null ){
			
			files = new DownloadStubFileImpl[0];
			
		}else{
			
			files = new DownloadStubFileImpl[file_list.size()];
			
			for ( int i=0;i<files.length;i++){
				
				files[i] = new DownloadStubFileImpl((Map)file_list.get(i));
			}
		}
		
		attributes = (Map<String,Object>)_map.get( "attr" );
	}
	
	public Map<String,Object>
	exportToMap()
	{
		Map<String,Object>	map = new HashMap<String,Object>();
		
		map.put( "dt", date_created );
		map.put( "hash", hash );
		map.put( "s", size );
		
		MapUtils.setMapString(map, "name", name );
		MapUtils.setMapString(map, "l", save_path );
		
		map.put( "gm", gm_map );
		
		List<Map<String,Object>>	file_list = new ArrayList<Map<String,Object>>();
		
		map.put( "files", file_list );
		
		for ( DownloadStubFileImpl file: files ){
			
			file_list.add( file.exportToMap());
		}
		
		if ( attributes != null ){
		
			map.put( "attr", attributes );
		}
				
		return( map );
	}
	
	public boolean
	isStub()
	{
		return( true );
	}
	
	protected void
	setStubbified()
	{
		temp_download = null;
	}
	
	public Download
	destubbify()
	
		throws DownloadException
	{
		if ( temp_download != null ){
			
			return( temp_download );
		}
		
		return( manager.destubbify( this ));
	}
	
	public String
	getName()
	{
		return( name );
	}
	
	public byte[]
	getTorrentHash()
	{
		return( hash );
	}
	
	public long
	getTorrentSize()
	{
		return( size );
	}
	
	public long 
	getCreationDate() 
	{
		return( date_created );
	}
	
	public String
	getSavePath()
	{
		return( save_path );
	}
	
	public DownloadStubFile[]
	getStubFiles()
	{
		return( files );
	}
	
	public long 
	getLongAttribute(
		TorrentAttribute 	attribute )
	{
		if ( attributes == null ){
			
			return( 0 );
		}
		
		Long l = (Long)attributes.get( attribute.getName());
		
		if ( l == null ){
			
			return( 0 );
		}
		
		return( l );
	}
	
	  
	public void 
	setLongAttribute(
		TorrentAttribute 	attribute, 
		long 				value)
	{
		if ( attributes == null ){
			
			attributes = new HashMap();
		}
		
		attributes.put( attribute.getName(), value );
		
		if ( temp_download == null ){
			
			manager.updated( this );
		}
	}
	
	public Map
	getGMMap()
	{
		return( gm_map );
	}
	
	public void
	remove()
	{
		manager.remove( this );
	}
	
	public void 
	remove(
		boolean delete_torrent, 
		boolean delete_data )
			
		throws DownloadException, DownloadRemovalVetoException 
	{
		if ( delete_data ){
			
			TOTorrent torrent = manager.getTorrent( this );
			
			if ( torrent != null ){
				
				File save_location = new File(getSavePath());
				
				if ( torrent.isSimpleTorrent()){
					
					if ( save_location.isFile()){
						
						FileUtil.deleteWithRecycle( save_location, false );
					}
				}else{
					
						// stub files are fully resolved (i.e. links followed). Anything outside the
						// save location root doesn't get touched and we want to avoid individual file deletes
						// if we can delete a parent directory instead
					
					if ( save_location.isDirectory()){
						
						DownloadStubFile[] files = getStubFiles();
						
						String	save_path = save_location.getAbsolutePath();
						
						if ( !save_path.endsWith( File.separator )){
							
							save_path += File.separator;
						}
						
						int	found = 0;
						
						for ( DownloadStubFile file: files ){
							
							File f = file.getFile();
							
							String path = f.getAbsolutePath();
							
							if ( path.startsWith( save_path )){
								
								if ( f.exists()){
								
									found++;
								}
							}
						}
						
						int actual = countFiles( save_location );
													
						if ( actual == found ){
							
							FileUtil.deleteWithRecycle( save_location, false );
							
						}else{
							
							for ( DownloadStubFile file: files ){
								
								File f = file.getFile();
								
								String path = f.getAbsolutePath();
								
								if ( path.startsWith( save_path )){
									
									FileUtil.deleteWithRecycle( f, false );
								}
							}
							
							TorrentUtils.recursiveEmptyDirDelete( save_location, false );
						}
					}
				}
			}
		}
	
		if ( delete_torrent ){
			
			byte[]	bytes = (byte[])gm_map.get( "torrent" );
			
			if ( bytes != null ){
				
				try{
					String torrent_file = new String( bytes, "UTF-8" );
					
					File file = new File( torrent_file );
					
					TorrentUtils.delete( file, false );
					
				}catch( Throwable e ){
					
					Debug.out( e );
				}
			}
		}
		
		manager.remove( this );
	}
	
	private int
	countFiles(
		File		dir )
	{
		int	result = 0;
		
		File[] files = dir.listFiles();
		
		if ( files != null ){
			
			for ( File f: files ){
				
				if ( f.isFile()){
					
					result++;
					
				}else{
					
					result += countFiles( f );
				}
			}
		}
		
		return( result );
	}
	
	protected static class
	DownloadStubFileImpl
		implements DownloadStubFile
	{
		private final File		file;
		private final long		length;
		
		protected
		DownloadStubFileImpl(
			DownloadStubFile	stub_file )
		{
			file	= stub_file.getFile();
			length	= stub_file.getLength();
		}
		
		protected
		DownloadStubFileImpl(
			Map		map )
		{
			file 	= new File( MapUtils.getMapString(map, "file", null ));
			
			length 	= (Long)map.get( "len" );
		}
		
		protected Map
		exportToMap()
		{
			Map	map = new HashMap();

			map.put( "file", file.getAbsolutePath());
			map.put( "len", length );
			
			return( map );
		}
		
		public File
		getFile()
		{
			return( file );
		}
		
		public long
		getLength()
		{
			return( length );
		}
	}
}
