/*
 * Created on Oct 2, 2008
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


package com.aelitis.azureus.core.util.http;

import java.net.*;
import java.util.*;
import java.io.*;

import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;

public class 
HTTPSniffingProxy 
{
	public static final int MAX_PROCESSORS = 32;
	
	private int		port;
	
	private ServerSocket	server_socket;	
	
	private List			processors = new ArrayList();
	
	private volatile boolean		destroyed;
	
	public
	HTTPSniffingProxy(
		String		delegate_to,
		boolean		delegate_is_https )
	
		throws Exception
	{
		server_socket = new ServerSocket();
		
		server_socket.setReuseAddress( true ); 
             
		server_socket.bind( new InetSocketAddress( "127.0.0.1", 0 ));
        
        port = server_socket.getLocalPort();
        
        new AEThread2( 
        	"HTTPSniffingProxy:" + delegate_to + "/" + delegate_is_https + "/" + port, 
        	true )
        	{
        		public void 
        		run() 
        		{
        			try{
        				while( !destroyed ){
    
        					Socket	socket = server_socket.accept();
        					       					
        					synchronized( HTTPSniffingProxy.this ){
        						
        						if ( processors.size() >= MAX_PROCESSORS ){
        							
        							try{
        								Debug.out( "Too many processors" );
        								
        								socket.close();
        								
        							}catch( Throwable e ){
        							}
        						}else{
        							
        							processor proc = new processor( socket );
        							
        							processors.add( proc );
        						}
        					}
        				}
        			}catch( Throwable e ){
        				
        				if ( !destroyed ){
        					
        					Debug.printStackTrace( e );
        				}
        			}
        		}
        	}.start();	
	}
	
	public int
	getPort()
	{
		return( port );
	}
	
	public void
	destroy()
	{
		List	to_destroy;
		
		synchronized( this ){
			
			if ( destroyed ){
				
				return;
			}
			
			destroyed = true;
				
			to_destroy = new ArrayList( processors );
			
			processors.clear();
			
			try{				
				server_socket.close();

			}catch( Throwable e ){	
			}
		}
		
		for (int i=0;i<to_destroy.size();i++){
			
			((processor)to_destroy.get(i)).destroy();
		}
	}
	
	protected class
	processor
	{
		private static final String	NL = "\r\n";
		
		protected Socket		socket;
		
		protected
		processor(
			Socket		_socket )
		{
			socket	= _socket;
			
			new AEThread2( "HTTPSniffingProxy:proc:in", true )
			{
				public void 
				run() 
				{
					try{
						InputStream is = socket.getInputStream();
						
						byte[]	buffer = new byte[1];
						
						String	header = "";
						
						while( true ){
						
							if ( is.read( buffer ) != 1 ){
								
								break;
							}
							
							header += (char)buffer[0];
							
							if ( header.endsWith( NL + NL )){
								
								break;
							}
						}
						
						System.out.println( "Got header " + header );
						
					}catch( Throwable e ){
						
						// todo 
					}
				}
			}.start();
		}
		
		protected void
		destroy()
		{
			try{
				socket.close();
				
			}catch( Throwable e ){
				
			}finally{
			
				synchronized( HTTPSniffingProxy.this ){

					processors.remove( this );
				}
			}
		}
	}
	
	public static void
	main(
		String[]		args )
	{
		try{
			HTTPSniffingProxy proxy = new HTTPSniffingProxy( "www.google.com", false );
			
			System.out.println( "port=" + proxy.getPort());
			
			while( true ){
				
				Thread.sleep(1000);
			}
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
}
