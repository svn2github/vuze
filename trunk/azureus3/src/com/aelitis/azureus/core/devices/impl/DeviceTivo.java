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
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebContext;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageGenerator;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageRequest;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageResponse;

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
		server_name	= _server_name;

		setAddress( _address );

		if ( !isAlive()){
							
			alive();
		}
	}
	

	protected boolean 
	generate(
		TrackerWebPageRequest 	request,
		TrackerWebPageResponse 	response ) 
	
		throws IOException 
	{
		String	url = request.getURL();
		
		System.out.println( "url: " + url );

		if ( !url.startsWith( "/TiVoConnect?" )){
			
			return( false );
		}
		
		int	pos = url.indexOf( '?' );
		
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
				/*
				"    <Item>" + NL +
				"        <Details>" + NL +
				"            <Title>Admin</Title>" + NL +
				"            <ContentType>text/html</ContentType>" + NL +
				"            <SourceFormat>x-container/folder</SourceFormat>" + NL +
				"        </Details>" + NL +
				"        <Links>" + NL +
				"            <Content>" + NL +
				"                <Url>/TiVoConnect?Command=QueryContainer&amp;Container=Admin</Url>" + NL +
				"                <ContentType>text/html</ContentType>" + NL +
				"            </Content>" + NL +
				"        </Links>" + NL +
				"    </Item>" + NL +
				*/
				"    <Item>" + NL +
				"        <Details>" + NL +
				"            <Title>" + server_name + "</Title>" + NL +
				"            <ContentType>x-container/tivo-videos</ContentType>" + NL +
				"            <SourceFormat>x-container/folder</SourceFormat>" + NL +
				"        </Details>" + NL +
				"        <Links>" + NL +
				"            <Content>" + NL +
				"                <Url>/TiVoConnect?Command=QueryContainer&amp;Container=/Stuff</Url>" + NL +
				"                <ContentType>x-container/tivo-videos</ContentType>" + NL +
				"            </Content>" + NL +
				"        </Links>" + NL +
				"    </Item>" + NL +
				"    <ItemStart>0</ItemStart>" + NL +
				"    <ItemCount>1</ItemCount>" + NL +
				"</TiVoContainer>";
			}else{
				
				reply =
				"<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + NL +
				"<TiVoContainer>" + NL +
				"    <Details>" + NL +
				"        <Title>" + "Stuff" + "</Title>" + NL +
				"        <ContentType>x-container/tivo-server</ContentType>" + NL +
				"        <SourceFormat>x-container/folder</SourceFormat>" + NL +
				"        <TotalItems>0</TotalItems>" + NL +
				"    </Details>" + NL +

				"    <ItemStart>0</ItemStart>" + NL +
				"    <ItemCount>0</ItemCount>" + NL +
				"</TiVoContainer>";
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
}
