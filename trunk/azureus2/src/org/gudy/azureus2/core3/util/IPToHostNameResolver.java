/*
 * Created on 27-May-2004
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

/**
 * @author parg
 *
 */

import java.net.InetAddress;
import java.util.*;

public class 
IPToHostNameResolver 
{
	static protected Thread			resolver_thread;
	static protected List			request_queue		= new ArrayList();
	static protected AEMonitor		request_mon			= new AEMonitor( "IPToHostNameResolver" );

	static protected AESemaphore	request_semaphore	= new AESemaphore("IPToHostNameResolver");
	
	public static void
	addResolverRequest(
		String							ip,
		IPToHostNameResolverListener	l )
	{
		try{
			request_mon.enter();
			
			request_queue.add( new request( ip, l ));
			
			request_semaphore.release();
			
			if ( resolver_thread == null ){
				
				resolver_thread = 
					new AEThread("IPToHostNameResolver")
					{
						public void
						run()
						{
							while(true){
								
								try{
									request_semaphore.reserve();
									
									request	req;
									
									try{
										request_mon.enter();
										
										req	= (request)request_queue.remove(0);
										
									}finally{
										
										request_mon.exit();
									}
									
									try{
										InetAddress addr = InetAddress.getByName( req.getIP());
											
										req.getListener().IPResolutionComplete( addr.getHostName(), true );
											
									}catch( Throwable e ){
										
										req.getListener().IPResolutionComplete( req.getIP(), false );
										
									}
								}catch( Throwable e ){
									
									Debug.printStackTrace( e );
								}
							}
						}
					};
					
				resolver_thread.setDaemon( true );	
					
				resolver_thread.start();
			}
		}finally{
			
			request_mon.exit();
		}
	}
	
	protected static class
	request
	{
		protected String						ip;
		protected IPToHostNameResolverListener	listener;
		
		protected
		request(
			String							_ip,
			IPToHostNameResolverListener	_listener )
		{
			ip			= _ip;
			listener	= _listener;
		}
		
		protected String
		getIP()
		{
			return( ip );
		}
		
		protected IPToHostNameResolverListener
		getListener()
		{
			return( listener );
		}
	}
}
