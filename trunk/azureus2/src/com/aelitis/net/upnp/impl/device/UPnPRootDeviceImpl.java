/*
 * Created on 14-Jun-2004
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
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
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.net.upnp.impl.device;


/**
 * @author parg
 *
 */

import java.util.*;
import java.net.*;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocument;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentNode;

import com.aelitis.net.upnp.*;
import com.aelitis.net.upnp.impl.UPnPImpl;

public class 
UPnPRootDeviceImpl 
	implements  UPnPRootDevice
{
	protected UPnPImpl		upnp;
	protected InetAddress	local_address;
	
	protected URL			location;
	protected URL			url_base_for_relative_urls;
	
	protected UPnPDevice	root_device;
	
	protected List			listeners	= new ArrayList();
	
	public
	UPnPRootDeviceImpl(
		UPnPImpl	_upnp,
		InetAddress	_local_address,
		URL			_location )
	
		throws UPnPException
	{
		upnp			= _upnp;
		local_address	= _local_address;
		location		= _location;
		
		SimpleXMLParserDocument	doc = upnp.downloadXML( location );
			
		SimpleXMLParserDocumentNode url_base_node = doc.getChild("URLBase");
		
		try{
			if ( url_base_node != null ){
				
				String	url_str = url_base_node.getValue().trim();
			
					// url_str is sometimes blank
				
				if ( url_str.length() > 0 ){
					
					url_base_for_relative_urls = new URL(url_str);
				}
			}
			
			upnp.log( "Relative URL base is " + (url_base_for_relative_urls==null?"unspecified":url_base_for_relative_urls.toString()));
			
		}catch(MalformedURLException e ){
			
			upnp.log( "Invalid URLBase - " + url_base_node.getValue());
			
			upnp.log( e );
			
			Debug.printStackTrace( e );
		}
		
		root_device = new UPnPDeviceImpl( this, "", doc.getChild( "Device" ));
	}

	protected String
	getAbsoluteURL(
		String	url )
	{
		String	lc_url = url.toLowerCase().trim();
		
		if ( lc_url.startsWith( "http://") || lc_url.startsWith( "https://" )){
			
				// already absolute
			
			return( url );
		}
		
			// relative URL
		
		if ( url_base_for_relative_urls != null ){
			
			String	abs_url = url_base_for_relative_urls.toString();
			
			if ( !abs_url.endsWith("/")){
				
				abs_url += "/";
			}
			
			if ( url.startsWith("/")){
				
				abs_url += url.substring(1);
				
			}else{
				
				abs_url += url;
			}
			
			return( abs_url );
			
		}else{
		
				// base on the root document location
			
			String	abs_url = location.toString();
		
			int	p1 = abs_url.indexOf( "://" ) + 3;
			
			p1 = abs_url.indexOf( "/", p1 );
			
			abs_url = abs_url.substring( 0, p1 );
			
			return( abs_url + (url.startsWith("/")?"":"/") + url );
		}
	}
	
	public UPnP
	getUPnP()
	{
		return( upnp );
	}
	
	public InetAddress
	getLocalAddress()
	{
		return( local_address );
	}
	
	public URL
	getLocation()
	{
		return( location );
	}
	
	public UPnPDevice
	getDevice()
	{
		return( root_device );
	}
	
	public void
	destroy(
		boolean		replaced )
	{
		for (int i=0;i<listeners.size();i++){
			
			((UPnPRootDeviceListener)listeners.get(i)).lost( this, replaced);
		}
	}
	
	public void
	addListener(
		UPnPRootDeviceListener	l )
	{
		listeners.add( l );
	}
	
	public void
	removeListener(
		UPnPRootDeviceListener	l )
	{
		listeners.remove( l );
	}
}
