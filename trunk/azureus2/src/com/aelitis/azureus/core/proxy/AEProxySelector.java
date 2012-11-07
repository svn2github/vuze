/*
 * Created on Nov 1, 2012
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


package com.aelitis.azureus.core.proxy;

import java.net.InetSocketAddress;
import java.net.Proxy;

public interface 
AEProxySelector 
{
	public Proxy
	getActiveProxy();
	
	public void
	startNoProxy();
	
	public void
	endNoProxy();
	
	public Proxy
	getSOCKSProxy(
		InetSocketAddress	proxy_address,
		InetSocketAddress	target );
	
	public Proxy
	getSOCKSProxy(
		String				proxy_host,
		int					proxy_port,
		InetSocketAddress	target );
	
	public void
	connectFailed(
		Proxy				proxy,
		Throwable			error );
	
	public long
	getLastConnectionTime();
	
	public int
	getConnectionCount();
	
	public long
	getLastFailTime();
	
	public int
	getFailCount();
	
	public String
	getInfo();
}
