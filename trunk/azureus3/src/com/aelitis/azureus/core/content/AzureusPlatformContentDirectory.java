/*
 * Created on Dec 19, 2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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
 * 
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */


package com.aelitis.azureus.core.content;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.utils.StaticUtilities;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderFactory;
import org.gudy.azureus2.pluginsimpl.local.torrent.TorrentImpl;
import org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader.ResourceDownloaderFactoryImpl;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.util.ConstantsVuze;

public class 
AzureusPlatformContentDirectory
	implements AzureusContentDirectory
{
	private static boolean registered = false;
	
	public static synchronized void
	register()
	{
		if ( !registered ){
		
			registered = true;
			
			AzureusContentDirectoryManager.registerDirectory( new AzureusPlatformContentDirectory());
		}
	}
	
	public AzureusContent
	lookupContent(
		Map		attributes )
	{
		byte[]	hash = (byte[])attributes.get( AT_BTIH );
		
		if ( hash == null ){
			
			return( null );
		}
		
		String	url_str = ConstantsVuze.getDefaultContentNetwork().getTorrentDownloadService( Base32.encode( hash ), null );
		
		ResourceDownloaderFactory rdf = ResourceDownloaderFactoryImpl.getSingleton();
		
		try{
			ResourceDownloader rd = rdf.create( new URL( url_str ));
		
			InputStream	is = rd.download();
			
			try{		
				TOTorrent	torrent = TOTorrentFactory.deserialiseFromBEncodedInputStream( is );
			
				return( new AzureusPlatformContent( new TorrentImpl( torrent )));
				
			}finally{
				
				is.close();
			}
			
		}catch( Throwable e ){
			
			e.printStackTrace();
			
			return( null );
		}
	}
	
	public AzureusContentDownload 
	lookupContentDownload(
		Map 		attributes ) 
	{
		byte[]	hash = (byte[])attributes.get( AT_BTIH );
				
		try{
			final Download download = StaticUtilities.getDefaultPluginInterface().getDownloadManager().getDownload(hash);
		
			if ( download == null ){
				
				return( null );
			}
			
			return( 
				new AzureusContentDownload()
				{
					public Download
					getDownload()
					{
						return( download );
					}
					
					public Object
					getProperty(
						String		name )
					{
						return( null );
					}
				});
			
		}catch( Throwable e ){
			
			return( null );
		}
	}
	
	public AzureusContentFile 
	lookupContentFile(
		Map 		attributes) 
	{
		byte[]	hash 	= (byte[])attributes.get( AT_BTIH );
		int		index	= ((Integer)attributes.get( AT_FILE_INDEX )).intValue();
		
		try{

			Download download = StaticUtilities.getDefaultPluginInterface().getDownloadManager().getDownload(hash);
		
			if ( download == null ){
				
				return( null );
			}
			
			Torrent	t_torrent = download.getTorrent();
			
			if ( t_torrent == null ){
				
				return( null );
			}
			
			final TOTorrent torrent = ((TorrentImpl)t_torrent).getTorrent();
			
			final DiskManagerFileInfo	file = download.getDiskManagerFileInfo()[index];

			if ( PlatformTorrentUtils.isContent( torrent, false )){
			
				return(
					new AzureusContentFile()
					{
						public DiskManagerFileInfo
						getFile()
						{
							return( file );
						}
						
						public Object
						getProperty(
							String		name )
						{
							try{
								if ( name.equals( PT_DURATION )){
									
									long duration = PlatformTorrentUtils.getContentVideoRunningTime( torrent );
									
									if ( duration > 0 ){
										
											// secs -> millis
										
										return( new Long( duration*1000 ));
									}
								}else if ( name.equals( PT_VIDEO_WIDTH )){
		
									int[] res = PlatformTorrentUtils.getContentVideoResolution(torrent);
									
									if ( res != null ){
										
										return(new Long( res[0]));
									}								
								}else if ( name.equals( PT_VIDEO_HEIGHT )){
		
									int[] res = PlatformTorrentUtils.getContentVideoResolution(torrent);
									
									if ( res != null ){
										
										return(new Long( res[1] ));
									}
								}else if ( name.equals( PT_DATE )){
		
									return( new Long( file.getDownload().getCreationTime()));
								}
							}catch( Throwable e ){							
							}
							
							return( null );
						}
					});
			}else{
				return(
						new AzureusContentFile()
						{
							public DiskManagerFileInfo
							getFile()
							{
								return( file );
							}
							
							public Object
							getProperty(
								String		name )
							{
								try{
									if ( name.equals( PT_DATE )){
	
										return( new Long( file.getDownload().getCreationTime()));
									}
								}catch( Throwable e ){							
								}
								
								return( null );
							}
						});
			}
		}catch( Throwable e ){
			
			return( null );
		}
	}
	
	protected class
	AzureusPlatformContent
		implements AzureusContent
	{
		private Torrent	torrent;
		
		protected
		AzureusPlatformContent(
			Torrent		_torrent )
		{
			torrent	= _torrent;
		}
		
		public Torrent
		getTorrent()
		{
			return( torrent );
		}
	}
}
