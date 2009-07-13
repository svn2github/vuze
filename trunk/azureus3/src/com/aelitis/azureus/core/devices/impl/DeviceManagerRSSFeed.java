/*
 * Created on Jul 13, 2009
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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.TimeFormatter;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.core3.xml.util.XUXmlWriter;
import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadScrapeResult;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.tracker.Tracker;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebContext;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageGenerator;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageRequest;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageResponse;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.devices.Device;

public class 
DeviceManagerRSSFeed 
	implements TrackerWebPageGenerator
{
	private DeviceManagerImpl		manager;
	
	private PluginInterface			plugin_interface;
	private TrackerWebContext		context;
	
	protected
	DeviceManagerRSSFeed(
		DeviceManagerImpl	_manager,
		AzureusCore			_core,
		int					_port )
	{
		manager = _manager;
		
		plugin_interface = _core.getPluginManager().getDefaultPluginInterface();
		
		try{
			context = 
				plugin_interface.getTracker().createWebContext(
					"DeviceFeed", 
					_port, 
					Tracker.PR_HTTP, 
					InetAddress.getByName( "127.0.0.1" ));
				
			context.addPageGenerator( this );
			
			manager.log( "RSS feed initialised on port " + _port );
			
		}catch( Throwable e ){
			
			manager.log( "Failed to initialise RSS feed on port " + _port, e );
		}
	}
	
	protected void
	destroy()
	{
		if ( context != null ){
			
			context.destroy();
		}
	}
	
	public boolean
	generate(
		TrackerWebPageRequest		request,
		TrackerWebPageResponse		response )
	
		throws IOException
	{
		URL	url	= request.getAbsoluteURL();
			
		String path = url.getPath();
		
		DeviceImpl[] devices = manager.getDevices();
		
		OutputStream os = response.getOutputStream();

		PrintWriter pw = new PrintWriter(new OutputStreamWriter( os, "UTF-8" ));

		if ( path.length() <= 1 ){
			
			response.setContentType( "text/html; charset=UTF-8" );
			
			pw.println( "<HTML><HEAD><TITLE>Vuze device feeds</TITLE></HEAD><BODY>" );
			
			for ( DeviceImpl d: devices ){
			
				if ( d.getType() != Device.DT_MEDIA_RENDERER || d.isHidden() || !d.isRSSPublishEnabled()){
					
					continue;
				}

				String	name = d.getName();
								
				pw.println( "<UL><A href=\"/" + URLEncoder.encode( name, "UTF-8" ) + "\">" + name + "</A></UL>" );
			}
			
			pw.println( "</BODY></HTML>" );
			
		}else{
			
			String	device_name = URLDecoder.decode( path.substring( 1 ), "UTF-8" );
			
			DeviceImpl	device = null;
			
			for ( DeviceImpl d: devices ){
				
				if ( d.getName().equals( device_name ) && d.isRSSPublishEnabled()){
					
					device = d;
					
					break;
				}
			}
			
			if ( device == null ){
				
				response.setReplyStatus( 404 );
				
				return( true );
			}
			
			response.setContentType( "text/xml; charset=UTF-8" );
				
			pw.println( "<?xml version=\"1.0\" encoding=\"utf-8\"?>" );
			
			pw.println( "<rss version=\"2.0\" xmlns:vuze=\"http://www.vuze.com\" xmlns:itunes=\"http://www.itunes.com/dtds/podcast-1.0.dtd\">" );
			
			pw.println( "<channel>" );
			
			pw.println( "<title>" + escape( device.getName()) + "</title>" );
					
			TranscodeFileImpl[] _files = device.getFiles();
			
			List<TranscodeFileImpl>	files = new ArrayList<TranscodeFileImpl>( _files.length );
			
			files.addAll( Arrays.asList( _files ));
			
			Collections.sort(
				files,
				new Comparator<TranscodeFileImpl>()
				{
					public int  
					compare(
						TranscodeFileImpl f1, 
						TranscodeFileImpl f2) 
					{
						long	added1 = f1.getCreationDateMillis()/1000;
						long	added2 = f2.getCreationDateMillis()/1000;

						return((int)(added2 - added1 ));
					}
				});
										
			String	feed_date_key = "devices.feed_date." + device.getID();
			
			long feed_date = COConfigurationManager.getLongParameter( feed_date_key );

			boolean new_date = false;
			
			for ( TranscodeFileImpl file: files ){
				
				long	file_date = file.getCreationDateMillis();
				
				if ( file_date > feed_date ){
					
					new_date = true;
					
					feed_date = file_date;
				}
			}
			
			if ( new_date ){
				
				COConfigurationManager.setParameter( feed_date_key, feed_date );
			}
			
			pw.println(	"<pubDate>" + TimeFormatter.getHTTPDate( feed_date ) + "</pubDate>" );

			for ( TranscodeFileImpl file: files ){
							
				if ( !file.isComplete()){
					
					continue;
				}
				
				pw.println( "<item>" );
				
				pw.println( "<title>" + escape( file.getName()) + "</title>" );
								
				pw.println(	"<pubDate>" + TimeFormatter.getHTTPDate( file.getCreationDateMillis()) + "</pubDate>" );
				
				String[] categories = file.getCategories();
				
				for ( String category: categories ){
					
					pw.println( "<category>" + category + "</category>" );
				}

				URL stream_url = file.getStreamURL();
				
				if ( stream_url != null ){
					
					pw.println( "<link>" + stream_url.toExternalForm() + "</link>" );
								
					String	mime_type = file.getMimeType();
				
					if ( mime_type != null ){
						
						try{
							pw.println( 
								"<enclosure url=\"" + stream_url.toExternalForm() + 
								"\" length=\"" + file.getTargetFile().getLength() + "\" type=\"" + mime_type + "\"></enclosure>" );
							
						}catch( Throwable e ){
							
						}
					}
				}
				
				pw.println( "<itunes:summary>" + escape( file.getName()) + "</itunes:summary>" );
				pw.println( "<itunes:duration>" + TimeFormatter.formatColon( file.getDurationMillis()/1000 ) + "</itunes:duration>" );
				
				pw.println( "</item>" );
			}
		
			pw.println( "</channel>" );
			
			pw.println( "</rss>" );
		}
		
		pw.flush();
		
		return( true );
	}
	
	protected String
	escape(
		String	str )
	{
		return( XUXmlWriter.escapeXML(str));
	}
}
