/*
 * Created on 20-Dec-2005
 * Created by Paul Gardner
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.instancemanager.impl;

import java.net.InetAddress;

import com.aelitis.azureus.core.instancemanager.AZInstance;

public class 
AZInstanceImpl
	implements AZInstance
{
	private String				id;
	private InetAddress			internal_address;
	private InetAddress			external_address;
	private int					tcp_port;
	private int					udp_port;
	
	protected
	AZInstanceImpl(
		String			_id,
		InetAddress		_internal_address,
		InetAddress		_external_address,
		int				_tcp_port,
		int				_udp_port )
	{
		id					= _id;
		internal_address	= _internal_address;
		external_address	= _external_address;
		tcp_port			= _tcp_port;
		udp_port			= _udp_port;
	}
	
	public String
	getID()
	{
		return( id );
	}
	
	public InetAddress
	getInternalAddress()
	{
		return( internal_address );
	}
	
	public InetAddress
	getExternalAddress()
	{
		return( external_address );
	}
	
	public int
	getTCPPort()
	{
		return( tcp_port );
	}
	
	public int
	getUDPPort()
	{
		return( udp_port );
	}
	
	public String
	getString()
	{
		return( "id=" + id + ",int=" + internal_address.getHostAddress() + ",ext=" + 
				external_address.getHostAddress() +	",tcp=" + tcp_port + ",udp=" + udp_port );
	}
}
