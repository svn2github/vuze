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


package com.aelitis.azureus.core.subs.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.URL;

import org.gudy.azureus2.core3.config.COConfigurationManager;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.TimeFormatter;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.core3.xml.util.XUXmlWriter;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageRequest;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageResponse;
import org.gudy.azureus2.plugins.utils.search.SearchResult;
import org.gudy.azureus2.plugins.utils.subscriptions.Subscription;
import org.gudy.azureus2.plugins.utils.subscriptions.SubscriptionManager;
import org.gudy.azureus2.plugins.utils.subscriptions.SubscriptionResult;

import com.aelitis.azureus.core.rssgen.RSSGeneratorPlugin;

public class 
SubscriptionRSSFeed 
	implements RSSGeneratorPlugin.Provider
{
	private static final String PROVIDER = "subscriptions";
	
	private SubscriptionManagerImpl		manager;
	private PluginInterface				plugin_interface;
	
	private RSSGeneratorPlugin		generator;
	
	protected
	SubscriptionRSSFeed(
		SubscriptionManagerImpl	_manager,
		PluginInterface			_plugin_interface )
	{
		manager 			= _manager;
		plugin_interface	= _plugin_interface;
				
		generator	= RSSGeneratorPlugin.getSingleton();
		
		generator.registerProvider( PROVIDER, this );
	}
		
	public boolean
	isEnabled()
	{
		return( manager.isRSSPublishEnabled());
	}
	
	public String
	getFeedURL()
	{
		return( generator.getURL() + PROVIDER );
	}
	
	public boolean
	generate(
		TrackerWebPageRequest		request,
		TrackerWebPageResponse		response )
	
		throws IOException
	{
		InetSocketAddress	local_address = request.getLocalAddress();
		
		if ( local_address == null ){
			
			return( false );
		}
		
		URL	url	= request.getAbsoluteURL();
					
		String path = url.getPath();
		
		path = path.substring( PROVIDER.length()+1);
		
		try{
			SubscriptionManager sman = plugin_interface.getUtilities().getSubscriptionManager();
	
			Subscription[] 	subs = sman.getSubscriptions();
			
			OutputStream os = response.getOutputStream();
	
			PrintWriter pw = new PrintWriter(new OutputStreamWriter( os, "UTF-8" ));
	
			if ( path.length() <= 1 ){
				
				response.setContentType( "text/html; charset=UTF-8" );
				
				pw.println( "<HTML><HEAD><TITLE>Vuze Subscription Feeds</TITLE></HEAD><BODY>" );
				
				for ( Subscription s: subs ){
	
					String	name = s.getName();
									
					pw.println( "<LI><A href=\"" + PROVIDER + "/" + s.getID() + "\">" + name + "</A></LI>" );
				}
				
				pw.println( "</BODY></HTML>" );
				
			}else{
				
				String	id = path.substring( 1 );
				
				Subscription	subscription = null;
				
				for ( Subscription s: subs ){
					
					if ( s.getID().equals( id )){
						
						subscription = s;
						
						break;
					}
				}
				
				if ( subscription == null ){
					
					response.setReplyStatus( 404 );
					
					return( true );
				}
				
				URL	feed_url = url;
	
					// absolute url is borked as it doesn't set the host properly. hack 
				
				String	host = (String)request.getHeaders().get( "host" );
				
				if ( host != null ){
					
					int	pos = host.indexOf( ':' );
					
					if ( pos != -1 ){
						
						host = host.substring( 0, pos );
					}
					
					feed_url = UrlUtils.setHost( url, host );
				}
				
				response.setContentType( "application/xml" );
				
				pw.println( "<?xml version=\"1.0\" encoding=\"utf-8\"?>" );
				
				pw.println( 
						"<rss version=\"2.0\" " + 
						"xmlns:vuze=\"http://www.vuze.com\" " +
						"xmlns:media=\"http://search.yahoo.com/mrss/\" " +
						"xmlns:atom=\"http://www.w3.org/2005/Atom\" " +
						"xmlns:itunes=\"http://www.itunes.com/dtds/podcast-1.0.dtd\">" );
				
				pw.println( "<channel>" );
				
				String channel_title = "Vuze Subscription: " + escape( subscription.getName());
						
				pw.println( "<title>" + channel_title + "</title>" );
				pw.println( "<link>http://vuze.com</link>" );
				pw.println( "<atom:link href=\"" + feed_url.toExternalForm() + "\" rel=\"self\" type=\"application/rss+xml\" />" );
				
				pw.println( "<description>Vuze RSS Feed for subscription " + escape( subscription.getName()) + "</description>" );
				
				pw.println("<itunes:image href=\"http://www.vuze.com/img/vuze_icon_128.png\"/>");
				pw.println("<image><url>http://www.vuze.com/img/vuze_icon_128.png</url><title>" + channel_title + "</title><link>http://vuze.com</link></image>");
				
						
	
											
				String	feed_date_key = "subscriptions.feed_date." + subscription.getID();
				
				long feed_date = COConfigurationManager.getLongParameter( feed_date_key );
	
				boolean new_date = false;
				
				// TODO: date
				
				if ( new_date ){
					
					COConfigurationManager.setParameter( feed_date_key, feed_date );
				}
				
				pw.println(	"<pubDate>" + TimeFormatter.getHTTPDate( feed_date ) + "</pubDate>" );
	
				SubscriptionResult[] results = subscription.getResults();
				
				for ( SubscriptionResult result: results ){
											
					try{
		  				pw.println( "<item>" );
		  				
		  				String	name = (String)result.getProperty( SearchResult.PR_NAME );
		  				
		  				pw.println( "<title>" + escape( name ) + "</title>" );
		  					
		  				/*
		  				pw.println(	"<pubDate>" + TimeFormatter.getHTTPDate( file.getCreationDateMillis()) + "</pubDate>" );
		  				
		  				pw.println( "<guid isPermaLink=\"false\">" + escape( file.getKey()) + "</guid>" );
		  				
		  				String[] categories = file.getCategories();
		  				
		  				for ( String category: categories ){
		  					
		  					pw.println( "<category>" + category + "</category>" );
		  				}
		  				
		  				String mediaContent = "";
		  				
		  				URL stream_url = file.getStreamURL( feed_url.getHost() );
		  				
		  				if ( stream_url != null ){
		  					
		  					String url_ext = stream_url.toExternalForm();
		  					
		  					long fileSize = file.getTargetFile().getLength();
		  					
		  					pw.println( "<link>" + url_ext + "</link>" );
		  					
		  					mediaContent = "<media:content medium=\"video\" fileSize=\"" +
												fileSize + "\" url=\"" + url_ext + "\""; 
		  					
		  					String	mime_type = file.getMimeType();
		  					
		  					if ( mime_type != null ){
		  						
		  						mediaContent += " type=\"" + mime_type + "\"";
		  					}
		  				
							pw.println("<enclosure url=\"" + url_ext
									+ "\" length=\"" + fileSize
									+ (mime_type == null ? "" : "\" type=\"" + mime_type)
									+ "\"></enclosure>");		
		  				}
		  				
		   				String	thumb_url		= null;
		  				String	author			= null;
		  				String	description		= null;
		  				
		  				try{
		  					Torrent torrent = file.getSourceFile().getDownload().getTorrent();
		  				
		  					TOTorrent toTorrent = PluginCoreUtils.unwrap(torrent);
						
		  					long duration_secs = PlatformTorrentUtils.getContentVideoRunningTime(toTorrent);
		  					
		  					if ( mediaContent.length() > 0 && duration_secs > 0 ){
		  						
		  						mediaContent += " duration=\"" + duration_secs + "\"";
		  					}
		  					  					
		  					thumb_url = PlatformTorrentUtils.getContentThumbnailUrl(toTorrent);
		  					
		  					author = PlatformTorrentUtils.getContentAuthor(toTorrent);
		  								
		  					description= PlatformTorrentUtils.getContentDescription(toTorrent);
		  					
		  					if ( description != null ){
		  						
		  						description = escapeMultiline( description );
	
		
		  							pw.println( "<description>" + description + "</description>");
		  						//}
		   					}					
		  				}catch( Throwable e ){
		  				}
		
		  					// media elements
		  				
		  				if ( mediaContent.length() > 0 ){
		  					  						
		  					pw.println( mediaContent += "></media:content>" );
		  				}
		
		  				pw.println( "<media:title>" + escape( file.getName()) + "</media:title>" );
		
						if ( description != null ){
								
							pw.println( "<media:description>" + description + "</media:description>" );
						}
						
						if ( thumb_url != null ) {
								
							pw.println("<media:thumbnail url=\"" + thumb_url + "\"/>" );
						}
		 
		 					// iTunes elements
		 					
						if ( thumb_url != null ) {
								
							pw.println("<itunes:image href=\"" + thumb_url + "\"/>");
						}
		
		 				if ( author != null ){
		  					
		  					pw.println("<itunes:author>" + escape(author) + "</itunees:author>");
		  				}
		  				
		  				pw.println( "<itunes:summary>" + escape( file.getName()) + "</itunes:summary>" );
		  				pw.println( "<itunes:duration>" + TimeFormatter.formatColon( file.getDurationMillis()/1000 ) + "</itunes:duration>" );
		  				*/
		  				
		  				pw.println( "</item>" );
		  				
					}catch( Throwable e ){
						
						Debug.out(e);
					}
				}
			
				pw.println( "</channel>" );
				
				pw.println( "</rss>" );
			}
			
			pw.flush();
			
		}catch( Throwable e ){
			
			throw( new IOException( Debug.getNestedExceptionMessage( e )));
		}
		
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
