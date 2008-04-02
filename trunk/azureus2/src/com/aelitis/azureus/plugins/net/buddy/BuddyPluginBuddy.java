/*
 * Created on Apr 1, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.plugins.net.buddy;

import java.net.InetAddress;

import org.gudy.azureus2.core3.util.SystemTime;

public class 
BuddyPluginBuddy 
{
	private BuddyPlugin		plugin;
	private String			public_key;
	
	private long			post_time;
	private InetAddress		ip;
	private int				tcp_port;
	private int				udp_port;
	
	private long			last_update;
	
	protected
	BuddyPluginBuddy(
		BuddyPlugin	_plugin,
		String		_pk )
	{
		plugin		= _plugin;
		public_key 	= _pk;
	}
	
	public String
	getPublicKey()
	{
		return( public_key );
	}
	
	protected void
	statusCheckFailed()
	{
		plugin.logMessage( public_key + ": offline" );
	}
	
	protected void
	updateStatus(
		long			_post_time,
		InetAddress		_ip,
		int				_tcp_port,
		int				_udp_port )
	{
		if ( _post_time > post_time ){
			
			last_update = SystemTime.getCurrentTime();
		}
		
		post_time	= _post_time;
		ip			= _ip;
		tcp_port	= _tcp_port;
		udp_port	= _udp_port;
		
		plugin.logMessage( public_key + ": online - ip=" + ip + ",tcp=" + tcp_port + ",udp=" + udp_port + ",age=" + (SystemTime.getCurrentTime() - last_update ));
	}
	
	protected String
	getString()
	{
		return( "pk=" + public_key + ",ip=" + ip + ",tcp=" + tcp_port + ",udp=" + udp_port );
	}
}
