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
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.naming.*;
import javax.naming.directory.*;

import org.gudy.azureus2.core3.tracker.protocol.PRHelpers;
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
		//lookupDNS( address );
		
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

				processResult( result );

			}finally{
				
				socket.close();
			}
		}catch( Throwable e ){
			
			throw( new NetworkAdminException( "whois connection failed", e ));
		}	
	}
	
	protected void
	lookupDNS(
		InetAddress		address )
	
		throws NetworkAdminException
	{
		byte[]	bytes = address.getAddress();
		
		String	target	= "origin.asn.cymru.com";
		
		for (int i=0;i<4;i++){
			
			target =  ( bytes[i] & 0xff ) + "." + target;
		}
		
		DirContext context = null;
		
		try{
			Hashtable env = new Hashtable();
			
			env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
			
			context = new InitialDirContext(env);
			
			Attributes attrs = context.getAttributes( target, new String[]{ "TXT" });
			
			NamingEnumeration n_enum = attrs.getAll();

			while( n_enum.hasMoreElements()){
				
				Attribute	attr =  (Attribute)n_enum.next();

				NamingEnumeration n_enum2 = attr.getAll();
				
				while( n_enum2.hasMoreElements()){
				
					String attribute = (String)n_enum2.nextElement();

					if ( attribute != null ){
					
							// "33544 | 64.71.0.0/20 | US | arin | 2006-05-04"
												
						processResult( 
								"AS | BGP Prefix | CC | Reg | Date | AS Name" + "\n" + 
								attribute + " | n/a" );
						
						return;
					}
				}
			}
		}catch( Throwable e ){
			
			throw( new NetworkAdminException( "DNS query failed", e ));
			
		}finally{
			
			if ( context != null ){
				
				try{
					context.close();
					
				}catch( Throwable e ){
				}
			}
		}
	}
		
	protected void
	processResult(
		String		result )
	{
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
	
		try{
			return( getCIDRStartAddress( bgp_prefix ));
			
		}catch( NetworkAdminException e ){
			
			Debug.out(e);
			
			return( null );
		}
	}
	
	protected static InetAddress
	getCIDRStartAddress(
		String	cidr )
	
		throws NetworkAdminException
	{
	
		int	pos = cidr.indexOf('/');
		
		try{
			return( InetAddress.getByName( cidr.substring(0,pos)));
			
		}catch( Throwable e ){
			
			throw( new NetworkAdminException( "Parse failure", e ));
		}
	}
	
	public InetAddress
	getBGPEndAddress()
	{
		if ( bgp_prefix == null ){
			
			return( null );
		}
		
		try{
			return( getCIDREndAddress( bgp_prefix ));
			
		}catch( NetworkAdminException e ){
			
			Debug.out(e);
			
			return( null );
		}
	}
	
	public static InetAddress
	getCIDREndAddress(
		String	cidr )
	
		throws NetworkAdminException
	{

		int	pos = cidr.indexOf('/');
		
		try{
			InetAddress	start = InetAddress.getByName( cidr.substring(0,pos));
			
			int	cidr_mask = Integer.parseInt( cidr.substring( pos+1 ));
			
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
			
			throw( new NetworkAdminException( "Parse failure", e ));
		}		
	}
	
	protected static boolean
	matchesCIDR(
		String		cidr,
		InetAddress	address )
	
		throws NetworkAdminException
	{
		InetAddress	start	= getCIDRStartAddress( cidr );
		InetAddress	end		= getCIDREndAddress( cidr );
		
		long	l_start = PRHelpers.addressToLong( start );
		long	l_end	= PRHelpers.addressToLong( end );
		
		long	test = PRHelpers.addressToLong( address );
		
		return( test >= l_start && test <= l_end );
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
			
			
			/*
			InetAddress	test = InetAddress.getByName( "255.71.15.1" );
			
			System.out.println( test + " -> " + matchesCIDR( "255.71.0.0/20", test ));
			*/
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
}
