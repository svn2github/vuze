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

import org.gudy.azureus2.core3.xml.simpleparser.*;

import com.aelitis.net.upnp.*;
import com.aelitis.net.upnp.impl.UPnPImpl;

public class 
UPnPRootDeviceImpl 
	implements  UPnPRootDevice
{
	protected UPnPImpl		upnp;
	protected InetAddress	local_address;
	
	protected URL			location;
	
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
			
		root_device = new UPnPDeviceImpl( this, "", doc.getChild( "Device" ));
	}

	protected String
	getAbsoluteURL(
		String	url )
	{
		String	lc_url = url.toLowerCase().trim();
		
		if ( lc_url.startsWith( "http://") || lc_url.startsWith( "https://" )){
			
			return( url );
		}
		
		String	prefix = location.toString();
		
		int	p1 = prefix.indexOf( "://" ) + 3;
		
		p1 = prefix.indexOf( "/", p1 );
		
		prefix = prefix.substring( 0, p1 );
		
		return( prefix + (url.startsWith("/")?"":"/") + url );
	}
	
	protected UPnPImpl
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
