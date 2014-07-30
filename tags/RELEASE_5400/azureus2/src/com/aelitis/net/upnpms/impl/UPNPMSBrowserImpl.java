/*
 * Created on Dec 19, 2012
 * Created by Paul Gardner
 * 
 * Copyright 2012 Vuze, Inc.  All rights reserved.
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


package com.aelitis.net.upnpms.impl;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.xml.simpleparser.SimpleXMLParserDocumentFactory;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.*;
import org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader.*;

import com.aelitis.net.upnpms.*;

public class 
UPNPMSBrowserImpl 
	implements UPNPMSBrowser
{
	private List<URL>				endpoints;
	private String					client_name;
	private UPNPMSBrowserListener	listener;
	
	private UPNPMSContainerImpl	root;
	
	private URL	preferred_endpoint;
	
	public 
	UPNPMSBrowserImpl(
		String					_client_name,
		List<URL>				_urls,
		UPNPMSBrowserListener	_listener )
	
		throws UPnPMSException
	{
		client_name	= _client_name;
		endpoints 	= _urls;
		listener	= _listener;
	
		client_name = client_name.replaceAll( "\"", "'" );
		client_name = client_name.replaceAll( ";", "," );
		client_name = client_name.replaceAll( "=", "-" );
		
		root = new UPNPMSContainerImpl( this, "0", "" );
	}
	
	public UPNPMSContainer 
	getRoot() 
	
		throws UPnPMSException 
	{
		return( root );
	}
	
	private void
	setPreferredEndpoint(
		URL		url )
	{
		if ( endpoints.size() > 1 ){
			
			if ( url != preferred_endpoint ){
				
				preferred_endpoint = url;
				
				listener.setPreferredURL( preferred_endpoint );
				
				endpoints.remove( preferred_endpoint );
				endpoints.add( 0, preferred_endpoint );
			}
		}
	}
	
	protected List<SimpleXMLParserDocumentNode>
	getContainerContents(
		String		id )
	
		throws UPnPMSException 
	{
		try{
			List<SimpleXMLParserDocumentNode>	results = new ArrayList<SimpleXMLParserDocumentNode>();
			
			int	starting_index = 0;
			
			while( true ){
				
				String NL = "\r\n";
				
				String soap_action = "urn:schemas-upnp-org:service:ContentDirectory:1#Browse";

				String request = 
					"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + NL +
					"<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" + NL +
					"<s:Body>" + NL +
					"<u:Browse xmlns:u=\"urn:schemas-upnp-org:service:ContentDirectory:1\">" + NL +
					"<ObjectID>" + id + "</ObjectID>" + NL +
					"<BrowseFlag>BrowseDirectChildren</BrowseFlag>" + NL +
					"<Filter>dc:date,res@protocolInfo,res@size</Filter>" + NL +
					"<StartingIndex>" + starting_index + "</StartingIndex>" + NL +
					"<RequestedCount>256</RequestedCount>" + NL +
					"<SortCriteria></SortCriteria>" + NL +
					"</u:Browse>" + NL +
					"</s:Body>" + NL +
					"</s:Envelope>";
				
				SimpleXMLParserDocument doc = null;
				
				UPnPMSException last_error = null;
				
				for ( URL endpoint: new ArrayList<URL>( endpoints )){
				
					try{
						doc = getXML( endpoint, soap_action, request );
						
						setPreferredEndpoint( endpoint );
						
						break;
						
					}catch( UPnPMSException e ){
						
						last_error = e;
					}
				}
				
				if ( doc == null ){
					
					throw( last_error );
				}
				
				SimpleXMLParserDocumentNode body = doc.getChild( "Body" );
				
				SimpleXMLParserDocumentNode response = body.getChild( "BrowseResponse" );
				
				SimpleXMLParserDocumentNode didl_result = response.getChild( "Result" );
				
				String 	didl_str = didl_result.getValue();
				
				SimpleXMLParserDocument	didle_doc = SimpleXMLParserDocumentFactory.create( didl_str );
								
				results.add( didle_doc );
				
				int	num_returned 	= Integer.parseInt( response.getChild( "NumberReturned" ).getValue());
				
				if ( num_returned <= 0 ){
					
					break;
				}
				
				starting_index += num_returned;
				
				int	total_matches	= Integer.parseInt( response.getChild( "TotalMatches" ).getValue());
				
				if ( starting_index >= total_matches ){
					
					break;
				}
			}
			
			return( results );
			
		}catch( UPnPMSException e ){
			
			throw( e );
			
		}catch( Throwable e ){
			
			throw( new UPnPMSException( "Failed to read container", e ));
		}
	}
	
	private SimpleXMLParserDocument 
	getXML(
		URL		url,
		String	soap_action,
		String	post_data )
	
		throws UPnPMSException
	{
		ResourceDownloader rd = new ResourceDownloaderFactoryImpl().create( url, post_data );
		
		try{
			rd.setProperty( "URL_Connection", "Keep-Alive" );
			rd.setProperty( "URL_Read_Timeout", 10*60*1000 );
			rd.setProperty( "URL_Connect_Timeout", 5*60*1000 );
			rd.setProperty( "URL_SOAPAction", "\"" + soap_action + "\"");
			rd.setProperty( "URL_X-AV-Client-Info", "av=1.0; cn=\"Azureus Software, Inc.\"; mn=\"" + client_name + "\"; mv=\""+ Constants.AZUREUS_VERSION + "\"" );
			rd.setProperty( "URL_Content-Type", "text/xml; charset=\"utf-8\"" );
					
			SimpleXMLParserDocument  doc = SimpleXMLParserDocumentFactory.create( rd.download());

			// doc.print();
			
			return( doc );
			
		}catch( Throwable e ){
			
			throw( new UPnPMSException( "XML RPC failed", e ));
		}
	}
}
