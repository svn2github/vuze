/*
 * Created on Jul 24, 2009
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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;


import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.xml.util.XUXmlWriter;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageRequest;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageResponse;

import com.aelitis.azureus.core.devices.TranscodeFile;

public class 
DeviceTivo
	extends DeviceMediaRendererImpl
{
	private static final String		NL				= "\r\n";

	private String		server_name;
	
	
	protected
	DeviceTivo(
		DeviceManagerImpl	_manager,
		String				_uid,
		String				_classification )
	{
		super( _manager, _uid, _classification, false );
		
		setName( "TiVo" );
	}

	protected
	DeviceTivo(
		DeviceManagerImpl	_manager,
		Map					_map )
	
		throws IOException
	{
		super( _manager, _map );
	}
	
	protected boolean
	updateFrom(
		DeviceImpl		_other )
	{
		if ( !super.updateFrom( _other )){
			
			return( false );
		}
		
		if ( !( _other instanceof DeviceTivo )){
			
			Debug.out( "Inconsistent" );
			
			return( false );
		}
		
		DeviceTivo other = (DeviceTivo)_other;
		
		return( true );
	}
	
	protected void
	initialise()
	{
		super.initialise();
	}
	
	public boolean 
	canAssociate()
	{
		return( true );
	}
	
	protected void
	found(
		InetAddress			_address,
		String				_server_name )
	{
		boolean	first_time = false;
		
		synchronized( this ){
		
			if ( server_name == null ){
			
				server_name	= _server_name;
				
				first_time = true;
			}
		}
		
		setAddress( _address );
							
		alive();
		
		if ( first_time ){
			
			browseReceived();
		}
	}
	

	protected boolean 
	generate(
		TrackerWebPageRequest 	request,
		TrackerWebPageResponse 	response ) 
	
		throws IOException 
	{
		InetSocketAddress	local_address = request.getLocalAddress();
		
		if ( local_address == null ){
			
			return( false );
		}
		
		String	host = local_address.getAddress().getHostAddress();
		
		String	url = request.getURL();
		
		System.out.println( "url: " + url );

		if ( !url.startsWith( "/TiVoConnect?" )){
			
			return( false );
		}
		
		int pos = url.indexOf( '?' );
		
		if ( pos == -1 ){
			
			return(false );
		}
		
		String[]	bits = url.substring( pos+1 ).split( "&" );
		
		Map<String,String>	args = new HashMap<String, String>();
		
		for ( String bit: bits ){
			
			String[] x = bit.split( "=" );
			
			args.put( x[0], URLDecoder.decode( x[1], "UTF-8" ));
		}
		
		System.out.println( "args: " + args );
		
			// root folder /TiVoConnect?Command=QueryContainer&Container=%2F
		
		String	command = args.get( "Command" );
		
		if ( command == null ){
			
			return( false );
		}
		
		String reply = null;

		if ( command.equals( "QueryContainer" )){
			
			String	container = args.get( "Container" );
			
			if ( container == null ){
				
				return( false );
			}
						
			if ( container.equals( "/" )){
			
				reply =
				"<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + NL +
				"<TiVoContainer>" + NL +
				"    <Details>" + NL +
				"        <Title>" + server_name + "</Title>" + NL +
				"        <ContentType>x-container/tivo-server</ContentType>" + NL +
				"        <SourceFormat>x-container/folder</SourceFormat>" + NL +
				"        <TotalItems>1</TotalItems>" + NL +
				"    </Details>" + NL +
				"    <Item>" + NL +
				"        <Details>" + NL +
				"            <Title>" + server_name + "</Title>" + NL +
				"            <ContentType>x-container/tivo-videos</ContentType>" + NL +
				"            <SourceFormat>x-container/folder</SourceFormat>" + NL +
				"        </Details>" + NL +
				"        <Links>" + NL +
				"            <Content>" + NL +
				"                <Url>/TiVoConnect?Command=QueryContainer&amp;Container=" + urlencode( "/Content" ) + "</Url>" + NL +
				"                <ContentType>x-container/tivo-videos</ContentType>" + NL +
				"            </Content>" + NL +
				"        </Links>" + NL +
				"    </Item>" + NL +
				"    <ItemStart>0</ItemStart>" + NL +
				"    <ItemCount>1</ItemCount>" + NL +
				"</TiVoContainer>";
				
			}else if ( container.startsWith( "/Content" )){
				
				
				List<TranscodeFile> files = new ArrayList<TranscodeFile>(Arrays.asList( getFiles()));
				
				Iterator<TranscodeFile> it = files.iterator();
				
				while( it.hasNext()){
					
					TranscodeFile file = it.next();
					
					if ( !file.isComplete()){
						
						it.remove();
					}
				
					URL stream_url = file.getStreamURL( host );
					
					if ( stream_url == null ){
						
						it.remove();
					}
				}
				
					// todo sorting
				
				String item_count_str = args.get( "ItemCount" );
				
				if ( item_count_str == null ){
					
					return( false );
				}
				
				int	item_count = Integer.parseInt( item_count_str );
				
				String	anchor = args.get( "AnchorItem" );
				
				int	item_start;
				
				if ( anchor == null ){
					
					item_start = 0;
					
				}else{
					
					// find index of anchor and then add offset if found
					
					item_start = 0;
				}
				
				int	num_to_return = Math.min( item_count, files.size() - item_start );
				
				int	container_id = 1;
				
				String	header = 
				"<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + NL +
				//"<?xml-stylesheet type=\"text/xsl\" href=\"/TiVoConnect?Command=XSL&amp;Container=Parg%27s%20pyTivo\"?>" + NL +
				"<TiVoContainer>" + NL +
				"    <Tivos>" + NL +
				"                <Tivo>VuzeTivoHDDVR</Tivo>" + NL +	// TODO
				"    </Tivos>" + NL +
				"    <ItemStart>" + item_start + "</ItemStart>" + NL +
				"    <ItemCount>" + num_to_return + "</ItemCount>" + NL +
				"    <Details>" + NL +
				"        <Title>" + escape( container ) + "</Title>" + NL +
				"        <ContentType>x-container/tivo-videos</ContentType>" + NL +
				"        <SourceFormat>x-container/folder</SourceFormat>" + NL +
				"        <TotalItems>" + files.size() + "</TotalItems>" + NL +
				"        <UniqueId>" + container_id + "</UniqueId>" + NL +
				"    </Details>" + NL;
				
				reply = header;
				
				for (int i=item_start;i<item_start+num_to_return;i++){
					
					TranscodeFile	file = files.get(i);
					
					long	source_size = 0;
					
					try{
						source_size = file.getSourceFile().getLength();
						
					}catch( Throwable e ){	
					}
					
					String	capture_date = Long.toString( file.getCreationDateMillis()/1000, 16);
					
					reply +=
				
				"    <Item>" + NL +
				"        <Details>" + NL +
				"            <Title>" + escape( file.getName()) + "</Title>" + NL +
				"            <ContentType>video/x-tivo-mpeg</ContentType>" + NL +
				"            <SourceFormat>video/x-ms-wmv</SourceFormat>" + NL +
				"            <SourceSize>" + source_size + "</SourceSize>" + NL +
				"            <Duration>" + file.getDurationMillis() + "</Duration>" + NL +
				"            <Description></Description>" + NL +
				"            <SourceChannel>0</SourceChannel>" + NL +
				"            <SourceStation></SourceStation>" + NL +
				"            <SeriesId></SeriesId>" + NL +
				"            <CaptureDate>0x" + capture_date + "</CaptureDate>" + NL + 
				"        </Details>" + NL +
				"        <Links>" + NL +
				"            <Content>" + NL +
				"                <ContentType>video/x-tivo-mpeg</ContentType>" + NL +
				"                    <AcceptsParams>No</AcceptsParams>" + NL +
				"                    <Url>" + file.getStreamURL( host ).toExternalForm() + "</Url>" + NL +
				"                </Content>" + NL +
				"                <CustomIcon>" + NL +
				"                    <ContentType>video/*</ContentType>" + NL +
				"                    <AcceptsParams>No</AcceptsParams>" + NL +
				"                    <Url>urn:tivo:image:save-until-i-delete-recording</Url>" + NL +
				"                </CustomIcon>" + NL +
				"            <TiVoVideoDetails>" + NL +
				"                <ContentType>text/xml</ContentType>" + NL +
				"                <AcceptsParams>No</AcceptsParams>" + NL +
				"                <Url>/TiVoConnect?Command=TVBusQuery&amp;Container=Parg%27s%20pyTivo&amp;File=/Big_Buck_Bunny%5BBLEN00000001%5D.mkv</Url>" + NL +
				"            </TiVoVideoDetails>" + NL +
				//"            <Push>" + NL +
				//"                <Container>" + escape( container ) + "</Container>" + NL +
				//"                <File>\\" + escape( file.getName()) + "</File>" + NL +
				//"            </Push>" + NL +
				"        </Links>" + NL +
				"    </Item>" + NL;
				}
				
				String footer =
				"</TiVoContainer>";
				
				reply += footer;
			}
			
	
		}else if ( command.equals( "QueryFormats")){
		
			String source_format = args.get( "SourceFormat" );
			
			if ( source_format != null && source_format.startsWith( "video" )){
				
					// /TiVoConnect?Command=QueryFormats&SourceFormat=video%2Fx-tivo-mpeg
				
				reply = 
				"<?xml version=\"1.0\" encoding=\"utf-8\"?>" + NL +
				"<TiVoFormats><Format>" + NL +
				"<ContentType>video/x-tivo-mpeg</ContentType><Description/>" + NL +
				"</Format></TiVoFormats>";
			}
		}
		
		if ( reply == null ){
			
			return( false );
		}
		
		System.out.println( "->" + reply );
		
		response.setContentType( "text/xml" );
		
		response.getOutputStream().write( reply.getBytes( "UTF-8" ));
		
		return( true );
	}
	
	protected String
	urlencode(
		String	str )
	
		throws IOException
	{
		return( URLEncoder.encode( str, "UTF-8" ));
	}
	
	protected String
	escape(
		String	str )
	{
		return( XUXmlWriter.escapeXML( str ));
	}
}
