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

package org.gudy.azureus2.core3.upnp.impl.device;

import org.gudy.azureus2.core3.upnp.*;
import org.gudy.azureus2.core3.upnp.impl.UPnPImpl;

/**
 * @author parg
 *
 */

import java.io.InputStream;
import java.net.*;

import org.gudy.azureus2.core3.xml.simpleparser.*;
import org.gudy.azureus2.plugins.utils.resourcedownloader.*;
import org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader.*;
import org.gudy.azureus2.core3.upnp.impl.*;

public class 
UPnPRootDeviceImpl 
	extends 	ResourceDownloaderAdapter
	implements  UPnPRootDevice
{
	protected UPnPImpl		upnp;
	protected URL			location;
	
	protected UPnPDevice	root_device;
	
	public
	UPnPRootDeviceImpl(
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
		
		try{
			InputStream	data = rd.download();
			
			SimpleXMLParserDocument	doc = SimpleXMLParserDocumentFactory.create( data );
			
			root_device = new UPnPDeviceImpl( this, "", doc.getChild( "Device" ));
			
		}catch( Throwable e ){
			
			e.printStackTrace();
			
			upnp.log( e );
		}
	}

	protected UPnPImpl
	getUPnP()
	{
		return( upnp );
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
	reportActivity(
		ResourceDownloader	downloader,
		String				activity )
	{
		upnp.log( activity );
	}
		
	public void
	failed(
		ResourceDownloader			downloader,
		ResourceDownloaderException e )
	{
		upnp.log( e );
	}
}
