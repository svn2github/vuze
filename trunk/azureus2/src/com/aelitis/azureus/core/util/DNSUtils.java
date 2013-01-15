/*
 * Created on Jun 11, 2009
 * Created by Paul Gardner
 * 
 * Copyright 2009 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.core.util;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;


import org.gudy.azureus2.core3.util.Debug;


public class 
DNSUtils 
{
	private static DNSUtilsIntf impl;
	
	static{
		String cla = System.getProperty( "az.factory.dnsutils.impl", "com.aelitis.azureus.core.util.dns.DNSUtilsImpl" );
		
		try{
			impl = (DNSUtilsIntf)Class.forName( cla ).newInstance();
			
		}catch( Throwable e ){
			
			Debug.out( "Failed to instantiate impl: " + cla, e );
		}
	}
		
	public static DNSUtilsIntf
	getSingleton()
	{
		return( impl );
	}
	
	public interface
	DNSUtilsIntf
	{
		public DNSDirContext
		getInitialDirContext()
		
			throws Exception;
		
		public DNSDirContext
		getDirContextForServer(
			String		dns_server_ip )
		
			throws Exception;
		
		public Inet6Address
		getIPV6ByName(
			String		host )
		
			throws UnknownHostException;
		
		public List<InetAddress>
		getAllByName(
			String		host )
			
			throws UnknownHostException;

		public List<InetAddress>
		getAllByName(
			DNSUtils.DNSDirContext	context,
			String					host )
			
			throws UnknownHostException;
		
		public List<String>
		getTXTRecords(
			String		query );
		
		public String
		getTXTRecord(
			String		query )
		
			throws UnknownHostException;
	}
	
	public interface
	DNSDirContext
	{
		public String
		getString();
	}
}
