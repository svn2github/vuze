/*
 * Created on 15-Jun-2004
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

import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentNode;

/**
 * @author parg
 *
 */

import java.net.InetAddress;
import java.util.*;


import com.aelitis.net.upnp.*;
import com.aelitis.net.upnp.impl.*;

public class 
UPnPDeviceImpl
	implements UPnPDevice
{
	protected UPnPRootDeviceImpl		root_device;
	
	protected String	device_type;
	protected String	friendly_name;
	
	protected List		devices		= new ArrayList();
	protected List		services	= new ArrayList();
	
	protected
	UPnPDeviceImpl(
		UPnPRootDeviceImpl				_root_device,
		String							indent,
		SimpleXMLParserDocumentNode		device_node )
	{
		root_device		= _root_device;
		
		device_type		= device_node.getChild("DeviceType").getValue();
		friendly_name	= device_node.getChild("FriendlyName").getValue();
		
		boolean	interested = device_type.equalsIgnoreCase( "urn:schemas-upnp-org:device:WANConnectionDevice:1" );
		
		root_device.getUPnP().log( indent + friendly_name + (interested?" *":""));
		
		SimpleXMLParserDocumentNode	service_list = device_node.getChild( "ServiceList" );
		
		if ( service_list != null ){
				
			SimpleXMLParserDocumentNode[] service_nodes = service_list.getChildren();
			
			for (int i=0;i<service_nodes.length;i++){
				
				services.add( new UPnPServiceImpl( this, indent + "  ", service_nodes[i]));
			}
		}
		
		SimpleXMLParserDocumentNode	dev_list = device_node.getChild( "DeviceList" );
		
		if ( dev_list != null ){
				
			SimpleXMLParserDocumentNode[] device_nodes = dev_list.getChildren();
			
			for (int i=0;i<device_nodes.length;i++){
				
				devices.add( new UPnPDeviceImpl( root_device, indent + "  ", device_nodes[i]));
			}
		}
	}
	
	protected String
	getAbsoluteURL(
		String	url )
	{
		return( root_device.getAbsoluteURL(url));
	}
	
	public InetAddress
	getLocalAddress()
	{
		return( root_device.getLocalAddress());
	}
	
	protected UPnPImpl
	getUPnP()
	{
		return( (UPnPImpl)root_device.getUPnP());
	}
	
	public UPnPRootDevice
	getRootDevice()
	{
		return( root_device );
	}
	
	public String
	getDeviceType()
	{
		return( device_type );
	}
	
	public String
	getFriendlyName()
	{
		return( friendly_name );
	}
	
	public UPnPDevice[]
	getSubDevices()
	{
		UPnPDevice[]	res = new UPnPDevice[devices.size()];
		
		devices.toArray( res );
		
		return( res );
	}
	
	public UPnPService[]
	getServices()
	{
		UPnPService[]	res = new UPnPService[services.size()];
		
		services.toArray( res );
		
		return( res );
	}
}
