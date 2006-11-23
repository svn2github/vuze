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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminASNLookup;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminException;

public class 
NetworkAdminASNLookupImpl 
	implements NetworkAdminASNLookup
{
	private static final String	WHOIS_ADDRESS 	= "whois.cymru.com";
	private static final int	WHOIS_PORT		= 43;
	
	private static final int	TIMEOUT			= 30000;
	
	private String		as;
	private String		asn;
	private String		bgp_prefix;
	
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
			Socket	socket = new Socket();
			
			int	timeout = TIMEOUT;
				
			long	start = SystemTime.getCurrentTime();
			
			socket.connect( new InetSocketAddress( WHOIS_ADDRESS, WHOIS_PORT ), timeout );
		
			long	end = SystemTime.getCurrentTime();
			
			timeout -= (end - start );
			
			if ( timeout <= 0 ){
				
				throw( new NetworkAdminException( "Timeout on connect" ));
				
			}else if ( timeout > TIMEOUT ){
				
				timeout = TIMEOUT;
			}
			
			socket.setSoTimeout( timeout );
			
			try{
				OutputStream	os = socket.getOutputStream();
				
				String	command = "-u -p " + address.getHostAddress() + "\r\n";
				
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
				
				as 			= (String)map.get( "as" );
				asn 		= (String)map.get( "as name" );
				bgp_prefix	= (String)map.get( "bgp prefix" );
				
				if ( bgp_prefix != null ){
					
					int	pos = bgp_prefix.indexOf(' ');
					
					if ( pos != -1 ){
						
						bgp_prefix = bgp_prefix.substring(pos+1).trim();
					}
					
					if ( bgp_prefix.indexOf('/') == -1 ){
						
						bgp_prefix = null;
					}
				}
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
		return( as==null?"":as );
	}
	
	public String
	getASName()
	{
		return( asn==null?"":asn );
	}
	
	public String
	getBGPPrefix()
	{
		return( bgp_prefix==null?"":bgp_prefix );
	}
	
	public InetAddress
	getBGPStartAddress()
	{
		if ( bgp_prefix == null ){
			
			return( null );
		}
		
		int	pos = bgp_prefix.indexOf('/');
		
		try{
			return( InetAddress.getByName( bgp_prefix.substring(0,pos)));
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
			
			return( null );
		}
	}
	
	public InetAddress
	getBGPEndAddress()
	{
		if ( bgp_prefix == null ){
			
			return( null );
		}
		
		int	pos = bgp_prefix.indexOf('/');
		
		try{
			InetAddress	start = InetAddress.getByName( bgp_prefix.substring(0,pos));
			
			int	cidr_mask = Integer.parseInt( bgp_prefix.substring( pos+1 ));
			
			int	rev_mask = 0;
			
			for (int i=0;i<32-cidr_mask;i++){
				
			
				rev_mask = ( rev_mask << 1 ) | 1;
			}
			
			byte[]	bytes = start.getAddress();
			
			bytes[0] |= (rev_mask>>24)&0xff;
			bytes[1] |= (rev_mask>>16)&0xff;
			bytes[2] |= (rev_mask>>8)&0xff;
			bytes[3] |= (rev_mask)&0xff;
			
			return( InetAddress.getByAddress( bytes ));
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
			
			return( null );
		}		
	}
	
	
	public String
	getString()
	{
		return( "as=" + getAS() + ",asn=" + getASName() + ", bgp_prefx=" + getBGPPrefix() + "[" +getBGPStartAddress() + "-" + getBGPEndAddress() + "]" );
	}
	
	public static void
	main(
		String[]	args )
	{
		try{
			NetworkAdminASNLookupImpl lookup = new NetworkAdminASNLookupImpl( InetAddress.getByName( "64.71.8.82" ));
			
			System.out.println( lookup.getString());
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
}
