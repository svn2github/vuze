/*
 * File    : PESharedPortSelector.java
 * Created : 24-Nov-2003
 * By      : parg
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.core3.peer.impl.transport.sharedport;

/**
 * @author parg
 *
 */

import java.util.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.logging.*;

public class 
PESharedPortSelector 
{
	public static final long	SOCKET_TIMEOUT	= 30*1000;
	
	protected Selector	selector;
	
	protected Map		outstanding_sockets	= new HashMap();
		
	protected Map		hash_map			= new HashMap();
	
	protected
	PESharedPortSelector()
	
		throws IOException
	{
		selector = Selector.open();
		
		Thread t = new Thread("PESharedPortSelector")
			{
				public void
				run()
				{
					selectLoop();
				}
			};
			
		t.setDaemon(true);
		
		t.start();
	}
	
	protected void
	selectLoop()
	{
		while (true){
			
			try{
			
				selector.select(1000);
				
					// Get set of ready objects
			 		 
				Set readyKeys 		= selector.selectedKeys();
				
				Iterator ready_it 	= readyKeys.iterator();
	
					// System.out.println( "selector keys = " + selector.keys().size());
				
				while (ready_it.hasNext()){
	
					SelectionKey key = (SelectionKey)ready_it.next();
	
						// Remove current entry
						
					ready_it.remove();
	
						// Get channel
					
					SocketChannel socket = (SocketChannel)key.channel();
	
					boolean	remove_this_key = false;
					
					if (key.isReadable()){
	
				  		// Read what's ready in response
				  		
				  		socketData	socket_data = (socketData)outstanding_sockets.get( socket );
				  		
				  		if ( socket_data == null ){
				  			
							LGLogger.log(0, 0, LGLogger.ERROR, getIP(socket) + " : PESharedPortSelector: failed to find socket buffer" );
				  			
				  			remove_this_key = true;
				  			
				  		}else{
				  		
					  		try{
					  			ByteBuffer	buffer = socket_data.getBuffer();
					  			
								int	len = socket.read(buffer);
					  		
					  			if ( len <= 0 ){
					  				
					  				remove_this_key = true;
					  				
					  			}else{
					  									  			
						  			if ( buffer.position() >= 48 ){
						  				
						  				byte[]	contents = new byte[buffer.position()];
									
										buffer.flip();
						  					
						  				buffer.get( contents );
						  				
						  				HashWrapper	hw = new HashWrapper(contents,28,20);
						  				
										PESharedPortServerImpl server = (PESharedPortServerImpl)hash_map.get( hw );
										
										if ( server == null ){
											
											remove_this_key	= true;
											
											LGLogger.log(0, 0, LGLogger.ERROR, getIP(socket) + " : PESharedPortSelector: failed to find server hash" );
										}else{
											
												// hand over this socket 
	
											outstanding_sockets.remove( socket );
											
											key.cancel();
											
											server.connectionReceived( socket, contents );
										}
						  			}
					  			}
					  		}catch( IOException e ){
					  			
					  			remove_this_key	= true;
					  			
								LGLogger.log(0, 0, LGLogger.ERROR, getIP(socket) + " : PESharedPortSelector: error occurred during socket read: " + e.toString());
					  		}
				  		}
					}
					
					if ( remove_this_key ){
						
						outstanding_sockets.remove( socket );
						
						try{
							socket.close();
							
						}catch( IOException e ){
						}
						
						key.cancel();
					}
				}
				
				Iterator	keys_it = selector.keys().iterator();
				
				long	now = System.currentTimeMillis();
				
				while( keys_it.hasNext()){
				
					SelectionKey key = (SelectionKey)keys_it.next();
					
					SocketChannel socket = (SocketChannel)key.channel();
	
					socketData	socket_data = (socketData)outstanding_sockets.get( socket );
					
					if ( socket_data != null ){
						
						if ( now - socket_data.getLastUseTime() > SOCKET_TIMEOUT ){
					
							LGLogger.log(0, 0, LGLogger.INFORMATION, getIP(socket_data.getSocket())+" : PESharedPortSelector: timed out socket connection" );

							outstanding_sockets.remove( socket );
						
							try{
								socket.close();
							
							}catch( IOException e ){
							}
						
							key.cancel();							
						}
					}				
				}
				
			}catch( Throwable e ){
				
				e.printStackTrace();
				
				LGLogger.log(0, 0, "PESharedPortSelector: error occurred during processing: " + e.toString(), e);
				
				try{
						// just in case something has gone mad
						
					Thread.sleep(1000);
					
				}catch( InterruptedException f ){
				}
			}
		}
	}
	
	public void
	addSocket(
		SocketChannel		_socket )
	{		
		try{
			
			socketData	sd = new socketData( _socket );
			
			outstanding_sockets.put( _socket, sd );
			
			_socket.register(selector,SelectionKey.OP_READ);
			
		}catch( ClosedChannelException e ){
			
			try{
			
				_socket.close();
				
			}catch( IOException f ){
			}
		}
	}
	
	public void
	addHash(
		PESharedPortServerImpl		_server,
		byte[]						_hash )
	{		
		hash_map.put( new HashWrapper( _hash ), _server );
	}	
	
	public void
	removeHash(
		PESharedPortServerImpl		_server,
		byte[]						_hash )
	{		
		hash_map.remove( new HashWrapper(_hash));
	}
	
	protected String
	getIP(
		SocketChannel	socket )
	{
		Socket s = socket.socket();
		
		if ( s == null ){
			
			return( "??.??.??.??");
		}
		
		InetAddress addr = s.getInetAddress();
		
		if ( addr == null ){
			
			return( "??.??.??.??");
		}
		
		return( addr.getHostAddress());
	}
	
	protected class
	socketData
	{
		protected SocketChannel	socket;
		protected ByteBuffer	buffer;
		protected long			last_use_time	= System.currentTimeMillis();
		
		protected
		socketData(
			SocketChannel	_socket )
		{
			socket	= _socket;
			
			buffer = ByteBuffer.allocate( 68 );
			
			buffer.position(0);
			
			buffer.limit(68);
		}
		
		protected SocketChannel
		getSocket()
		{
			return( socket );
		}
		
		protected ByteBuffer
		getBuffer()
		{
			last_use_time	= System.currentTimeMillis();
			
			return( buffer );
		}
		
		protected long
		getLastUseTime()
		{
			return( last_use_time );
		}
	}
}
