/*
 * Created on Feb 12, 2009
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
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;

public abstract class 
TranscodePipe 
{
	protected volatile boolean	paused;
	protected volatile boolean	destroyed;

	protected volatile int		bytes_available;
	protected volatile int		max_bytes_per_sec;

	protected List<Socket>		sockets 		= new ArrayList<Socket>();

	private ServerSocket		server_socket;
	private AEThread2			refiller;

	protected
	TranscodePipe()
	
		throws IOException
	{
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
								handleSocket( socket );
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

	protected abstract void
	handleSocket(
		Socket		socket );
	
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
				final int BUFFER_SIZE = 128*1024;
				
				byte[]	buffer = new byte[ BUFFER_SIZE ];

				while( !destroyed ){
					
					try{	
						int	len;
						
						if ( paused ){
							
							Thread.sleep(250);
							
							len =  is.read( buffer, 0, 1 );
							
						}else{
							
							int	limit;
							
							if ( max_bytes_per_sec > 0 ){
								
								limit = bytes_available;
								
								if ( limit <= 0 ){
									
									Thread.sleep(100);
									
									continue;
								}
								
								limit = Math.min( BUFFER_SIZE, limit );
								
							}else{
								
								limit = BUFFER_SIZE;
							}
							
							len =  is.read( buffer, 0, limit );
						}
							
						if ( len <= 0 ){
								
							break;
						}
							
						if ( max_bytes_per_sec > 0 ){
						
							bytes_available -= len;
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
				
				try{
					os.close();
					
				}catch( Throwable e ){
				}
			}
		}.start();
	}
	
	protected void
	handleRAF(
		final RandomAccessFile	raf,
		final OutputStream		os,
		final long				length )
	{
		new AEThread2( "TranscodePipe:c", true )
		{
			public void
			run()
			{
				final int BUFFER_SIZE = 128*1024;
				
				byte[]	buffer = new byte[ BUFFER_SIZE ];

				long	rem = length;
				
				while( !destroyed && rem > 0){
					
					try{	
						int	len;
						
						if ( paused ){
							
							Thread.sleep(250);
							
							len =  raf.read( buffer, 0, 1 );
							
						}else{
							
							int	limit;
							
							if ( max_bytes_per_sec > 0 ){
								
								limit = bytes_available;
								
								if ( limit <= 0 ){
									
									Thread.sleep(100);
									
									continue;
								}
								
								limit = Math.min( BUFFER_SIZE, limit );
								
							}else{
								
								limit = BUFFER_SIZE;
							}
							
							limit = (int)Math.min( rem, limit );
							
							len =  raf.read( buffer, 0, limit );
						}
							
						if ( len <= 0 ){
								
							break;
						}
							
						rem -= len;
						
						if ( max_bytes_per_sec > 0 ){
						
							bytes_available -= len;
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
					raf.close();
					
				}catch( Throwable e ){
				}
				
				try{
					os.close();
					
				}catch( Throwable e ){
				}
			}
		}.start();
	}
	
	protected void
	pause()
	{
		paused = true;
	}

	protected void
	resume()
	{
		paused = false;
	}
	
	public void 
	setMaxBytesPerSecond(
		int		 max ) 
	{
		if ( max == max_bytes_per_sec ){
			
			return;
		}
		
		max_bytes_per_sec = max;
		
		synchronized( this ){
			
			if ( refiller == null  ){
				
				refiller = 
					new AEThread2( "refiller", true )
					{
						public void 
						run() 
						{
							int	count = 0;
							
							while( !destroyed ){
								
								if ( max_bytes_per_sec == 0 ){
									
									synchronized( this ){
										
										if ( max_bytes_per_sec == 0 ){
										
											refiller = null;
										
											break;
										}
									}
								}
								
								count++;
								
								bytes_available += max_bytes_per_sec/10;
								
								if ( count%10 == 0 ){
									
									bytes_available += max_bytes_per_sec%10;
								}
								
								try{
									Thread.sleep(100);
									
								}catch( Throwable e ){
									
									Debug.printStackTrace(e);
									
									break;
								}
							}
						}
					};
					
				refiller.start();
			}
		}
	}
	
	protected int
	getPort()
	{
		return( server_socket.getLocalPort());
	}
	
	protected boolean
	destroy()
	{
		synchronized( this ){
			
			if ( destroyed ){
				
				return( false );
			}
			
			destroyed	= true;
		}

		for (Socket s: sockets ){
			
			try{			
				s.close();
				
			}catch( Throwable e ){	
			}
		}
		
		sockets.clear();
		
		try{
			server_socket.close();
			
		}catch( Throwable e ){
		
			Debug.printStackTrace(e);
		}
		
		return( true );
	}
}
