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

package org.gudy.azureus2.core3.upnp.impl;

import org.gudy.azureus2.core3.upnp.*;

/**
 * @author parg
 *
 */

import java.io.InputStream;
import java.net.*;

import org.gudy.azureus2.core3.xml.simpleparser.*;
import org.gudy.azureus2.plugins.utils.resourcedownloader.*;
import org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader.*;

public class 
UPnPRootDevice 
	implements ResourceDownloaderListener
{
	protected UPnPImpl		upnp;
	protected URL			location;
	
	protected
	UPnPRootDevice(
		UPnPImpl	_upnp,
		String		_location,
		String		_usn )
	
		throws UPnPException
	{
		upnp		= _upnp;
		
		try{
			location	= new URL( _location );
			
		}catch( MalformedURLException e ){
			
			throw( new UPnPException( "Root device location '" + _location + "' invalid", e ));
		}
		
		ResourceDownloaderFactory rdf = ResourceDownloaderFactoryImpl.getSingleton();
		
		ResourceDownloader rd = rdf.getRetryDownloader( rdf.create( location ), 3 );
		
		rd.addListener( this );
		
		rd.asyncDownload();
	}
	
	protected void
	decode(
		SimpleXMLParserDocument		doc )
	{		
		decodeDevice( "", doc.getChild( "Device" ));
	}
	
	protected void
	decodeDevice(
		String							indent,
		SimpleXMLParserDocumentNode		device_node )
	{
		String	friendly_name	= device_node.getChild("FriendlyName").getValue();
		
		upnp.log( indent + friendly_name );
		
		SimpleXMLParserDocumentNode	service_list = device_node.getChild( "ServiceList" );
		
		if ( service_list != null ){
				
			SimpleXMLParserDocumentNode[] services = service_list.getChildren();
			
			for (int i=0;i<services.length;i++){
				
				decodeService( indent + "  ", services[i]);
			}
		}
		SimpleXMLParserDocumentNode	dev_list = device_node.getChild( "DeviceList" );
		
		if ( dev_list != null ){
				
			SimpleXMLParserDocumentNode[] devices = dev_list.getChildren();
			
			for (int i=0;i<devices.length;i++){
				
				decodeDevice( indent + "  ", devices[i]);
			}
		}
	}
	
	protected void
	decodeService(
		String							indent,
		SimpleXMLParserDocumentNode		service_node )
	{
		String	desc_url	= service_node.getChild("SCPDURL").getValue();
		String	control_url	= service_node.getChild("controlURL").getValue();
		
		upnp.log( indent + desc_url + ", " + control_url );
	}
	
	public void
	reportPercentComplete(
		ResourceDownloader	downloader,
		int					percentage )
	{
	}
		
	public void
	reportActivity(
		ResourceDownloader	downloader,
		String				activity )
	{
		upnp.log( activity );
	}
		
	public boolean
	completed(
		ResourceDownloader	downloader,
		InputStream			data )
	{
		try{
			SimpleXMLParserDocument	doc = SimpleXMLParserDocumentFactory.create( data );
		
			decode( doc );
			
		}catch( Throwable e ){
			
			e.printStackTrace();
			
			upnp.log( e );
		}
		
		return( true );
	}
		
	public void
	failed(
		ResourceDownloader			downloader,
		ResourceDownloaderException e )
	{
		upnp.log( e );
	}
}
