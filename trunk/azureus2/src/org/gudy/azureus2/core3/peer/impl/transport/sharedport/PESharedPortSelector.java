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
import java.nio.*;
import java.nio.channels.*;

import org.gudy.azureus2.core3.util.*;

public class 
PESharedPortSelector 
{
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
				Iterator readyItor 	= readyKeys.iterator();
	
					// System.out.println( "selector keys = " + selector.keys().size());
				
				while (readyItor.hasNext()){
	
					SelectionKey key = (SelectionKey)readyItor.next();
	
						// Remove current entry
						
					readyItor.remove();
	
						// Get channel
					
					SocketChannel socket = (SocketChannel)key.channel();
	
					boolean	remove_this_key = false;
					
					if (key.isReadable()){
	
				  		// Read what's ready in response
				  		
				  		ByteBuffer	buffer = (ByteBuffer)outstanding_sockets.get( socket );
				  		
				  		if ( buffer == null ){
				  			
				  			System.out.println( "eh? buffer not found");
				  			
				  			remove_this_key = true;
				  			
				  		}else{
				  		
					  		try{
					  		
								int	len = socket.read(buffer);
					  		
					  			if ( len <= 0 ){
					  				
					  				remove_this_key = true;
					  				
					  			}else{
					  			
						  			System.out.println( "buffer position = " + buffer.position());
						  			
						  			if ( buffer.position() >= 48 ){
						  				
						  				byte[]	contents = new byte[buffer.position()];
									
										buffer.flip();
						  					
						  				buffer.get( contents );
						  				
						  				HashWrapper	hw = new HashWrapper(contents,28,20);
						  				
										PESharedPortServerImpl server = (PESharedPortServerImpl)hash_map.get( hw );
										
										if ( server == null ){
											
											remove_this_key	= true;
											
											System.out.println( "server dead");
										}else{
											
												// hand over this socket 
												
											key.cancel();
											
											server.connectionReceived( socket, contents );
										}
						  			}
					  			}
					  		}catch( IOException e ){
					  			
					  			remove_this_key	= true;
					  			
					  			e.printStackTrace();
					  		}
				  		}
					}
					
					if ( remove_this_key ){
						
						outstanding_sockets.remove( socket );
						
						try{
							socket.close();
							
						}catch( IOException e ){
							
							e.printStackTrace();
						}
						
						key.cancel();
					}
				}
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
		}
	}
	
	public void
	addSocket(
		SocketChannel		_socket )
	{
		System.out.println( "socket added");
		
		try{
			
			ByteBuffer	buffer = ByteBuffer.allocate( 68 );
			
			buffer.position(0);
			
			buffer.limit(68);
			
			outstanding_sockets.put( _socket, buffer );
			
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
		System.out.println( "hash added");
		
		hash_map.put( new HashWrapper( _hash ), _server );
	}	
	
	public void
	removeHash(
		PESharedPortServerImpl		_server,
		byte[]						_hash )
	{
		System.out.println( "hash removed");
		
		hash_map.remove( new HashWrapper(_hash));
	}
}
