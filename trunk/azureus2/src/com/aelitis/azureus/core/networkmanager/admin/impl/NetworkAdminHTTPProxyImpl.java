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

import java.util.StringTokenizer;

import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminHTTPProxy;

public class 
NetworkAdminHTTPProxyImpl 
	implements NetworkAdminHTTPProxy
{
	private String	http_host;
	private String	http_port;
	private String	https_host;
	private String	https_port;
	
	private String	user;
	private String	password;
	
	private String[]	non_proxy_hosts;
	
	protected
	NetworkAdminHTTPProxyImpl()
	{
        http_host	= System.getProperty("http.proxyHost", "" ).trim();
        http_port	= System.getProperty("http.proxyPort", "" ).trim();
        https_host	= System.getProperty("https.proxyHost", "" ).trim();
        https_port	= System.getProperty("https.proxyPort", "" ).trim();
        
        http_host	= System.getProperty("http.proxyUser", "" ).trim();
        http_host	= System.getProperty("http.proxyPassword", "" ).trim();
    
        String	nph = System.getProperty("http.nonProxyHosts", "" ).trim();
        
        StringTokenizer	tok = new StringTokenizer( nph, "|" );
        
        non_proxy_hosts = new String[tok.countTokens()];
        
        int	pos = 0;
        
        while( tok.hasMoreTokens()){
        	
        	non_proxy_hosts[pos++] = tok.nextToken();
        }
	}
	
	protected boolean
	isConfigured()
	{
		return( http_host.length() > 0 || https_host.length() > 0 );
	}
	
	public String
	getHTTPHost()
	{
		return( http_host );
	}
	
	public String
	getHTTPPort()
	{
		return( http_port );
	}
	
	public String
	getHTTPSHost()
	{
		return( https_host );
	}
	
	public String
	getHTTPSPort()
	{
		return( https_port );
	}

	public String
	getUser()
	{
		return( user );
	}
	
	public String[]
	getNonProxyHosts()
	{
		return( non_proxy_hosts );
	}
}
