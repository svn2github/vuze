/*
 * Created on 29-Jun-2004
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
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
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.core3.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author parg
 *
 */

public class 
HostNameToIPResolver 
{
	static protected Thread			resolver_thread;
	static protected List			request_queue		= new ArrayList();
	static protected Semaphore		request_semaphore	= new Semaphore();
	
	public static void
	addResolverRequest(
		String							host,
		HostNameToIPResolverListener	l )
	{
		byte[]	bytes = textToNumericFormat( host );
		
		if ( bytes != null ){
		
			try{
				l.hostNameResolutionComplete( InetAddress.getByAddress( host, bytes ));
			
				return;
				
			}catch( UnknownHostException e ){
			}
		}
		
		synchronized( request_queue ){
			
			request_queue.add( new request( host, l ));
			
			request_semaphore.release();
			
			if ( resolver_thread == null ){
				
				resolver_thread = 
					new AEThread("HostNameToIPResolver")
					{
						public void
						run()
						{
							while(true){
								
								try{
									request_semaphore.reserve();
									
									request	req;
									
									synchronized( request_queue ){
										
										req	= (request)request_queue.remove(0);
									}
									
									try{
										InetAddress addr = InetAddress.getByName( req.getHost());
										
										req.getListener().hostNameResolutionComplete( addr );
											
									}catch( Throwable e ){
										
										req.getListener().hostNameResolutionComplete( null );
										
									}
								}catch( Throwable e ){
									
									e.printStackTrace();
								}
							}
						}
					};
					
				resolver_thread.setDaemon( true );	
					
				resolver_thread.start();
			}
		}
	}
	
		// this has been copied from Inet4Address - need to change for IPv6
	
	final static int INADDRSZ	= 4;
	
	static byte[] textToNumericFormat(String src)
	    {
		if (src.length() == 0) {
		    return null;
		}
		
		int octets;
		char ch;
		byte[] dst = new byte[INADDRSZ];
	        char[] srcb = src.toCharArray();
		boolean saw_digit = false;

		octets = 0;
		int i = 0;
		int cur = 0;
		while (i < srcb.length) {
		    ch = srcb[i++];
		    if (Character.isDigit(ch)) {
			// note that Java byte is signed, so need to convert to int
			int sum = (dst[cur] & 0xff)*10
			    + (Character.digit(ch, 10) & 0xff);
			
			if (sum > 255)
			    return null;

			dst[cur] = (byte)(sum & 0xff);
			if (! saw_digit) {
			    if (++octets > INADDRSZ)
				return null;
			    saw_digit = true;
			}
		    } else if (ch == '.' && saw_digit) {
			if (octets == INADDRSZ)
			    return null;
			cur++;
			dst[cur] = 0;
			saw_digit = false;
		    } else
			return null;
		}
		if (octets < INADDRSZ)
		    return null;
		return dst;
	}
	
	
	
	protected static class
	request
	{
		protected String						host;
		protected HostNameToIPResolverListener	listener;
		
		protected
		request(
			String							_host,
			HostNameToIPResolverListener	_listener )
		{
			host			= _host;
			listener		= _listener;
		}
		
		protected String
		getHost()
		{
			return( host );
		}
		
		protected HostNameToIPResolverListener
		getListener()
		{
			return( listener );
		}
	}
}
