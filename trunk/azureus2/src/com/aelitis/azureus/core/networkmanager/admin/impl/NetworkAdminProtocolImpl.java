/*
 * Created on 1 Nov 2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */


package com.aelitis.azureus.core.networkmanager.admin.impl;


import java.net.InetAddress;

import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminNetworkInterfaceAddress;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminProtocol;

public class 
NetworkAdminProtocolImpl 
	implements NetworkAdminProtocol
{
	private int				type;
	private int				port;
	
	protected 
	NetworkAdminProtocolImpl(
		int			_type,
		int			_port )
	{
		type		= _type;
		port		= _port;
	}
	
	public int
	getType()
	{
		return( type );
	}
	
	public int 
	getPort()
	{
		return( port );
	}
	
	public boolean
	test(
		NetworkAdminNetworkInterfaceAddress	address )
	{
		InetAddress a = address==null?null:address.getAddress();
		
		NetworkAdminProtocolTester	tester;
		
		if ( type == PT_HTTP ){
			
			tester = new NetworkAdminHTTPTester();
			
		}else if ( type == PT_TCP ){
			
			tester = new NetworkAdminTCPTester();

		}else{
			
			tester = new NetworkAdminUDPTester();
		}
		
		try{
			InetAddress	res;
			
			if ( port <= 0 ){
				
				res = tester.testOutbound( a, 0 );
				
			}else{
				
				res = tester.testInbound( a, 0 );
			}
			
			return( true );
			
		}catch( Throwable e){
			
			e.printStackTrace();
			
			return( false );
		}
	}
}
