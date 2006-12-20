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

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderFactory;
import org.gudy.azureus2.pluginsimpl.local.torrent.TorrentImpl;
import org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader.ResourceDownloaderFactoryImpl;

import com.aelitis.azureus.util.Constants;

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
		
		String	url_str = Constants.URL_PREFIX + Constants.URL_DOWNLOAD + Base32.encode( hash ) + ".torrent";
		
		ResourceDownloaderFactory rdf = ResourceDownloaderFactoryImpl.getSingleton();
		
		try{
			ResourceDownloader rd = rdf.create( new URL( url_str ));
		
			InputStream	is = rd.download();
			
			TOTorrent	torrent = TOTorrentFactory.deserialiseFromBEncodedInputStream( is );
			
			is.close();
			
			return( new AzureusPlatformContent( new TorrentImpl( torrent )));
			
		}catch( Throwable e ){
			
			e.printStackTrace();
			
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
