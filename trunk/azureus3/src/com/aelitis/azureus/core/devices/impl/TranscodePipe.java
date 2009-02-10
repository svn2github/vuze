/*
 * Created on Feb 9, 2009
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


package com.aelitis.azureus.core.devices.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.*;

import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;

public class 
TranscodePipe 
{
	private int				source_port;
	private ServerSocket	server_socket;

	private volatile boolean	destroyed;
	
	private List<Socket>		sockets = new ArrayList<Socket>();
	
	protected
	TranscodePipe(
		int		_source_port )
	
		throws IOException
	{
		source_port	= _source_port;
		
		server_socket = new ServerSocket( 0, 50, InetAddress.getByName( "127.0.0.1" ));
	
		new AEThread2( "TranscodePipe", true )
		{
			public void
			run()
			{
				while( !destroyed ){
					
					try{
						final Socket	socket = server_socket.accept();
						
						new AEThread2( "TranscodePipe", true )
						{
							public void
							run()
							{
								handleStream( socket );
							}
						}.start();
						
					}catch( Throwable e ){
						
						if ( !destroyed ){
												
							destroy();
						}
						
						break;
					}
				}
			}
		}.start();
	}
	
	protected void
	handleStream(
		Socket		socket1 )
	{
		synchronized( this ){
			
			if ( destroyed ){
				
				try{
					socket1.close();
					
				}catch( Throwable e ){				
				}
				
				return;
			}
			
			sockets.add( socket1 );
		}

		try{
			Socket socket2 = new Socket( "127.0.0.1", source_port );
	
			synchronized( this ){

				if ( destroyed ){
					
					try{
						socket1.close();
						
					}catch( Throwable e ){				
					}
					
					try{
						socket2.close();
						
					}catch( Throwable e ){				
					}
					
					sockets.remove( socket1 );
					
					return;
				}
				
				sockets.add( socket2 );
			}
			
			handlePipe( socket1.getInputStream(), socket2.getOutputStream());
			
			handlePipe( socket2.getInputStream(), socket1.getOutputStream());
			
		}catch( Throwable e ){
			
			try{
				socket1.close();
				
			}catch( Throwable f ){
			}
			
			synchronized( this ){

				sockets.remove( socket1 );
			}
		}
	}
	
	protected void
	handlePipe(
		final InputStream		is,
		final OutputStream		os )
	{
		new AEThread2( "TranscodePipe:c", true )
		{
			public void
			run()
			{
				byte[]	buffer = new byte[128*1024];

				while( !destroyed ){
					
					try{						
						int	len =  is.read( buffer );
							
						if ( len <= 0 ){
								
							break;
						}
							
						os.write( buffer, 0, len );
						
					}catch( Throwable e ){
						
						break;
					}
				}
				
				try{
					os.flush();
					
				}catch( Throwable e ){
				}
				
				try{
					is.close();
					
				}catch( Throwable e ){
				}
			}
		}.start();
	}
	
	protected int
	getPort()
	{
		return( server_socket.getLocalPort());
	}
	
	protected void
	destroy()
	{
		synchronized( this ){
			
			if ( destroyed ){
				
				return;
			}
			
			destroyed	= true;
		}

		for (Socket s: sockets ){
			
			try{
				
				s.close();
				
			}catch( Throwable e ){	
			}
		}
		
		try{
			server_socket.close();
			
		}catch( Throwable e ){
		
			Debug.printStackTrace(e);
		}
	}
}
