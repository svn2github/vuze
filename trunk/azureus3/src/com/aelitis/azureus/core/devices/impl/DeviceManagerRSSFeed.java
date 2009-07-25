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

import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.TimeFormatter;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.core3.xml.util.XUXmlWriter;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.tracker.Tracker;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebContext;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageGenerator;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageRequest;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageResponse;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.devices.Device;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;

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
		int					_port,
		boolean				_local_only )
	{
		manager = _manager;
		
		plugin_interface = _core.getPluginManager().getDefaultPluginInterface();
		
		try{
			if ( _local_only ){
				
				context = 
					plugin_interface.getTracker().createWebContext(
						"DeviceFeed", 
						_port, 
						Tracker.PR_HTTP, 
						InetAddress.getByName( "127.0.0.1" ));
				
			}else{
				
				context = 
					plugin_interface.getTracker().createWebContext(
						"DeviceFeed", 
						_port, 
						Tracker.PR_HTTP );
			}
			
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
			
			if ( device instanceof DeviceMediaRendererImpl ){
				
				((DeviceMediaRendererImpl)device).browseReceived();
			}
			
			response.setContentType( "application/xml" );
			
			pw.println( "<?xml version=\"1.0\" encoding=\"utf-8\"?>" );
			
			pw.println( "<rss version=\"2.0\" xmlns:vuze=\"http://www.vuze.com\" xmlns:itunes=\"http://www.itunes.com/dtds/podcast-1.0.dtd\">" );
			
			pw.println( "<channel>" );
			
			pw.println( "<title>Vuze: " + escape( device.getName()) + "</title>" );
			pw.println( "<link>http://vuze.com</link>" );
			
			pw.println( "<description>Vuze RSS Feed for " + escape( device.getName()) + "</description>" );
			
			pw.println("<itunes:image href=\"http://www.vuze.com/img/vuze_icon_128.png\"/>");
			pw.println("<image><url>http://www.vuze.com/img/vuze_icon_128.png</url></image>");
			
					
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
				try{
							
  				if ( !file.isComplete()){
  					
  					if ( !file.isTemplate()){
  						
  						continue;
  					}
  				}
  				
  				pw.println( "<item>" );
  				
  				pw.println( "<title>" + escape( file.getName()) + "</title>" );
  								
  				pw.println(	"<pubDate>" + TimeFormatter.getHTTPDate( file.getCreationDateMillis()) + "</pubDate>" );
  				
  				String[] categories = file.getCategories();
  				
  				for ( String category: categories ){
  					
  					pw.println( "<category>" + category + "</category>" );
  				}
  				
  
  				String mediaContent = "";
  				URL stream_url = file.getStreamURL();
  				
  				if ( stream_url != null ){
  					
  					String url_ext = stream_url.toExternalForm().replaceAll("127.0.0.1", "192.168.0.149");
  					long fileSize = file.getTargetFile().getLength();
  					
  					pw.println( "<link>" + url_ext + "</link>" );
  					
  					mediaContent = "<media:content medium=\"video\" fileSize=\""
								+ fileSize + "\" url=\"" + url_ext + "\""; 
  					
  					String	mime_type = file.getMimeType();
  					
  					if (mime_type != null) {
  						mediaContent += " type=\"" + mime_type + "\"";
  					}
  				
						pw.println("<enclosure url=\"" + url_ext
								+ "\" length=\"" + fileSize
								+ (mime_type == null ? "" : "\" type=\"" + mime_type)
								+ "\"></enclosure>");
						
  				}
  				
  				try {
  					Torrent torrent = file.getSourceFile().getDownload().getTorrent();
  				
  					TOTorrent toTorrent = PluginCoreUtils.unwrap(torrent);

  					
  					long duration = PlatformTorrentUtils.getContentVideoRunningTime(toTorrent);
  					if (mediaContent.length() > 0) {
  						mediaContent += " duration=\"" + duration + "\"";
  					}
  					mediaContent += ">";
  					
  					String thumbURL = PlatformTorrentUtils.getContentThumbnailUrl(toTorrent);
  					if (thumbURL != null) {
  						pw.println("<itunes:image href=\"" + thumbURL + "\"/>");
  						mediaContent += "<media:thumbnail url=\"" + thumbURL + "\" />";
  					}
  					
  					String author = PlatformTorrentUtils.getContentAuthor(toTorrent);
  					if (author != null) {
  						pw.println("<itunes:author>" + escape(author) + "</itunees:author>");
  					}
  					
  					String desc = PlatformTorrentUtils.getContentDescription(toTorrent);
  					if (desc != null) {
    					mediaContent += "<media:description>" + escapeMultiline(desc) + "</media:description>";
  						pw.println("<description>" + escapeMultiline(desc) + "</description>");
  					}
  					
  				} catch (Exception e) {
  					
  				}

  				if (mediaContent.length() > 0) {
  					if (!mediaContent.contains(">")) {
  						mediaContent += ">";
  					}

  					mediaContent += "<media:title>" + escape( file.getName()) + "</media:title>";

						mediaContent += "</media:content>";
						
						// Unfortunately, writing the media:content tag breaks some rss readers.
						// Comment out until I figure out why
						//pw.println(mediaContent);
  				}

  				pw.println( "<itunes:summary>" + escape( file.getName()) + "</itunes:summary>" );
  				pw.println( "<itunes:duration>" + TimeFormatter.formatColon( file.getDurationMillis()/1000 ) + "</itunes:duration>" );
  				
  				pw.println( "</item>" );
				}catch( Throwable e ){
					Debug.out(e);
				}
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

	protected String
	escapeMultiline(
		String	str )
	{
		return( XUXmlWriter.escapeXML(str.replaceAll("[\r\n]+", "<BR>")));
	}
}
