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
import java.util.*;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;


public class 
AZOtherInstanceImpl
	extends AZInstanceImpl
{
	protected static AZOtherInstanceImpl
	decode(
		InetAddress				internal_address,
		String					str )
	{
		StringTokenizer	tok = new StringTokenizer( str, ":" );
		
		tok.nextToken();	// skip 'azureus'
		String	instance_id = tok.nextToken();
		String	int_ip		= unmapAddress(tok.nextToken());
		String	ext_ip		= unmapAddress(tok.nextToken());
		int		tcp			= Integer.parseInt(tok.nextToken());
		int		udp			= Integer.parseInt(tok.nextToken());
		
		List	extra_args = new ArrayList();
		
		while( tok.hasMoreTokens()){
			
			extra_args.add( tok.nextToken());
		}
		
		try{
			if ( !int_ip.equals("0.0.0.0")){
				
				internal_address = InetAddress.getByName( int_ip );
			}

			InetAddress	external_address = InetAddress.getByName( ext_ip );
			
			return( new AZOtherInstanceImpl(instance_id, internal_address, external_address, tcp, udp, extra_args ));
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
		
		return( null );
	}
	
	private String					id;
	private List					internal_addresses	= new ArrayList();
	private InetAddress				external_address;
	private int						tcp_port;
	private int						udp_port;
	private List					extra_args;
	
	private long	alive_time;


	protected
	AZOtherInstanceImpl(
		String					_id,
		InetAddress				_internal_address,
		InetAddress				_external_address,
		int						_tcp_port,
		int						_udp_port,
		List					_extra_args )
	{
		id					= _id;
		
		internal_addresses.add( _internal_address );
		
		external_address	= _external_address;
		tcp_port			= _tcp_port;
		udp_port			= _udp_port;
		extra_args			= _extra_args;
		
		alive_time	= SystemTime.getCurrentTime();
	}
	
	protected boolean
	update(
		AZOtherInstanceImpl	new_inst )
	{		
		alive_time	= SystemTime.getCurrentTime();
		
		InetAddress	new_address = new_inst.getInternalAddress();
		
		boolean	same = true;
		
		if ( !internal_addresses.contains( new_address )){
			
			same	= false;
			
			List	new_addresses = new ArrayList( internal_addresses );
			
			new_addresses.add( 0, new_address );
			
			internal_addresses	= new_addresses;
		}
		
		same	 = 	same && 
					external_address.equals( new_inst.external_address ) &&
					tcp_port == new_inst.tcp_port  &&
					udp_port == new_inst.udp_port;
		
		
		external_address	= new_inst.external_address;
		tcp_port			= new_inst.tcp_port;
		udp_port			= new_inst.udp_port;
		extra_args			= new_inst.extra_args;
	
		return( !same );
	}
	
	public String
	getID()
	{
		return( id );
	}
	
	public InetAddress
	getInternalAddress()
	{
		return((InetAddress)internal_addresses.get(0));
	}
	
	protected List
	getInternalAddresses()
	{
		return( internal_addresses );
	}
	
	public InetAddress
	getExternalAddress()
	{
		return( external_address );
	}
	
	public int
	getTrackerClientPort()
	{
		return( tcp_port );
	}
	
	public int
	getDHTPort()
	{
		return( udp_port );
	}
	
	protected List
	getExtraArgs()
	{
		return( extra_args );
	}
	
	protected long
	getAliveTime()
	{
		long	now = SystemTime.getCurrentTime();
		
		if ( now < alive_time ){
			
			alive_time	= now;
		}
		
		return( alive_time );
	}
}
