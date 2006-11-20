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

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.aelitis.azureus.core.networkmanager.NetworkAdminASNLookup;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminException;

public class 
NetworkAdminASNLookupImpl 
	implements NetworkAdminASNLookup
{
	private static final String	WHOIS_ADDRESS 	= "whois.cymru.com";
	private static final int	WHOIS_PORT		= 43;
	
	private String		as		= "?";
	private String		asn		= "?";
	
	protected 
	NetworkAdminASNLookupImpl(
		InetAddress		address )
	
		throws NetworkAdminException
	{
		lookupTCP( address );
	}
	
	protected void
	lookupTCP(			
		InetAddress		address )
	
		throws NetworkAdminException
	{
		try{
			Socket	socket = new Socket( WHOIS_ADDRESS, WHOIS_PORT );
		
			try{
				OutputStream	os = socket.getOutputStream();
				
				String	command = "-u " + address.getHostAddress() + "\r\n";
				
				os.write( command.getBytes());
				
				os.flush();
				
				InputStream	is = socket.getInputStream();
				
				byte[]	buffer = new byte[1024];
				
				String	result = "";
				
				while( true ){
					
					int	len = is.read( buffer );
					
					if ( len <= 0 ){
						
						break;
					}
					
					result += new String( buffer, 0, len );
				}
				
				StringTokenizer	lines = new StringTokenizer( result, "\n" );

				int	line_number = 0;
				
				List	keywords = new ArrayList();
				
				Map	map = new HashMap();
				
				while( lines.hasMoreTokens()){
					
					String	line = lines.nextToken().trim();
					
					line_number++;
					
					if ( line_number > 2 ){
						
						break;
					}
					
					StringTokenizer	tok = new StringTokenizer( line, "|" );
				
					int	token_number = 0;
					
					while( tok.hasMoreTokens()){
						
						String	token = tok.nextToken().trim();
						
						if ( line_number == 1 ){
							
							keywords.add( token.toLowerCase());
							
						}else{
							
							if ( token_number >= keywords.size()){
								
								break;
								
							}else{
								
								String	kw = (String)keywords.get( token_number );
	
								map.put( kw, token );
							}
						}
						
						token_number++;
					}
				}
				
				as 	= (String)map.get( "as" );
				asn = (String)map.get( "as name" );
				
			}finally{
				
				socket.close();
			}
		}catch( Throwable e ){
			
			throw( new NetworkAdminException( "whois connection failed", e ));
		}	
	}
	
	public String
	getAS()
	{
		return( as );
	}
	
	public String
	getASName()
	{
		return( asn );
	}
	
	public static void
	main(
		String[]	args )
	{
		try{
			NetworkAdminASNLookupImpl lookup = new NetworkAdminASNLookupImpl( InetAddress.getByName( "www.google.com" ));
			
			System.out.println( "as=" + lookup.getAS() + ",asn=" + lookup.getASName());
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
}
