/*
 * Created on 22 Jun 2006
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.networkmanager.impl.udp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.networkmanager.impl.TransportHelper;

public class 
UDPTransportHelper 
	implements TransportHelper
{
	private UDPConnectionManager	manager;
	private UDPSelector				selector;
	private InetSocketAddress		address;
	private UDPTransport			transport;
	
	private boolean					incoming;
	
	private UDPConnection			connection;
	
	private selectListener		read_listener;
	private Object				read_attachment;
	private boolean 			read_selects_paused;
	
	private selectListener		write_listener;
	private Object				write_attachment;
	private boolean 			write_selects_paused	= true;	// default is paused

	private boolean				closed;
	private IOException			failed;
	
	private ByteBuffer			pending_partial_write;
	
	protected
	UDPTransportHelper(
		UDPConnectionManager	_manager,
		InetSocketAddress		_address,
		UDPTransport			_transport )
	
		throws IOException
	{
			// outgoing
	
		manager		= _manager;
		address 	= _address;
		transport	= _transport;
		
		incoming	= false;
		
		connection 	= manager.registerOutgoing( this );
		
		selector	= connection.getSelector();

	}
	
	protected
	UDPTransportHelper(
		UDPConnectionManager	_manager,
		InetSocketAddress		_address, 
		UDPConnection			_connection )
	{
			// incoming
			
		manager		= _manager;
		address 	= _address;
		connection = _connection;
	
		incoming	= true;
		
		selector	= connection.getSelector();
	}
	
	protected void
	setTransport(
		UDPTransport	_transport )
	{
		transport	= _transport;
	}

	protected UDPTransport
	getTransport()
	{
		return( transport );
	}
	
	protected int
	getMss()
	{
		if ( transport == null ){
			
			return( UDPNetworkManager.getUdpMssSize());
		}
		
		return( transport.getMssSize());
	}
	
	public boolean
	minimiseOverheads()
	{
		return( true );
	}
	
	public InetSocketAddress
	getAddress()
	{
		return( address );
	}
	
	public boolean
	isIncoming()
	{
		return( incoming );
	}
	
	protected UDPConnection
	getConnection()
	{
		return( connection );
	}
	
	public int 
	write( 
		ByteBuffer 	buffer, 
		boolean		partial_write )
	
		throws IOException
	{
		synchronized( this ){
			
			if ( failed != null ){
				
				throw( failed );
			}
			
			if ( closed ){
				
				throw( new IOException( "Transport closed" ));
			}
		}
		
		if ( partial_write ){
			
			if ( pending_partial_write == null ){
				
				if ( buffer.remaining() < UDPConnectionSet.MIN_WRITE_PAYLOAD ){
				
					ByteBuffer	copy = ByteBuffer.allocate( buffer.remaining());
					
					copy.put( buffer );
					
					copy.position( 0 );
					
					pending_partial_write = copy;
					
					return( copy.remaining());
				}
			}
		}
		
		if ( pending_partial_write != null ){
			
			int	pw_len = pending_partial_write.remaining();
			
			try{
			
				int	written = connection.write( new ByteBuffer[]{ pending_partial_write, buffer }, 0, 2 );
				
				if ( written >= pw_len ){
					
					return( written - pw_len );
					
				}else{
					
					return( 0 );
				}
				
			}finally{
				
				if ( pending_partial_write.remaining() == 0 ){
					
					pending_partial_write = null;
				}
			}
			
		}else{
			
			return( connection.write( new ByteBuffer[]{ buffer }, 0, 1 ));
		}
	}

    public long 
    write( 
    	ByteBuffer[] 	buffers, 
    	int 			array_offset, 
    	int 			length ) 
    
    	throws IOException
    {
		synchronized( this ){
			
			if ( failed != null ){
				
				throw( failed );
			}
			
			if ( closed ){
				
				throw( new IOException( "Transport closed" ));
			}
		}
		
		return( connection.write( buffers, array_offset, length ));
    }

    public int 
    read( 
    	ByteBuffer buffer ) 
    
    	throws IOException
    {
		synchronized( this ){
			
			if ( failed != null ){
				
				throw( failed );
			}
			
			if ( closed ){
				
				throw( new IOException( "Transport closed" ));
			}
		}
		
    	return( connection.read( buffer ));
    }

    public long 
    read( 
    	ByteBuffer[] 	buffers, 
    	int 			array_offset, 
    	int 			length ) 
    
    	throws IOException
    {
		synchronized( this ){
			
			if ( failed != null ){
				
				throw( failed );
			}
			
			if ( closed ){
				
				throw( new IOException( "Transport closed" ));
			}
		}
		
    	long	total = 0;
    	
    	for (int i=array_offset;i<array_offset+length;i++){
    		
    		ByteBuffer	buffer = buffers[i];
    		
    		int	max = buffer.remaining();
    		
    		int	read = connection.read( buffer );
    		
    		total += read;
    		
    		if ( read < max ){
    		
    			break;
    		}
    	}
    	
    	return( total );
    }

    protected void
    canRead()
    {
     	synchronized( this ){
    
    		if ( read_listener != null && !read_selects_paused ){
    			    	   		
    			selector.ready( this, read_listener, read_attachment );
    		}
    	}
    }
    
    protected void
    canWrite()
    {
       	synchronized( this ){
       	    
    		if ( write_listener != null  && !write_selects_paused ){
    			    	   		
    			write_selects_paused	= true;
    			
    			selector.ready( this, write_listener, write_attachment );
    		}
    	}
    }
    
    public synchronized void
    pauseReadSelects()
    {
    	read_selects_paused	= true;
    }
    
    public synchronized void
    pauseWriteSelects()
    {
    	write_selects_paused = true;
    }
 
    public synchronized void
    resumeReadSelects()
    {
    	read_selects_paused = false;
    	
    	if ( connection.canRead()){
    		
    		canRead();
 	
    	}else if ( read_listener != null ){
    		
    		if ( closed ){
    			
    			selector.ready( this, read_listener, read_attachment, new Throwable( "Transport closed" ));
    			
    		}else  if ( failed != null ){
   				
   				selector.ready( this, read_listener, read_attachment, failed );
   			}
    	}
    }
    
    public synchronized void
    resumeWriteSelects()
    {
    	write_selects_paused = false;
    	
    	if ( connection.canWrite()){
    		
    		canWrite();
    		
    	}else if ( write_listener != null ){
    		
    		if ( closed ){
    			
    			selector.ready( this, write_listener, write_attachment, new Throwable( "Transport closed" ));
    			
    		}else  if ( failed != null ){
   				
   				selector.ready( this, write_listener, write_attachment, failed );
   			}
    	}
    }
    
    public void
    registerForReadSelects(
    	selectListener	listener,
    	Object			attachment )
    {
    	synchronized( this ){
    		
	    	read_listener		= listener;
	    	read_attachment		= attachment;
    	}
    	
    	resumeReadSelects();
    }
    
    public void
    registerForWriteSelects(
    	selectListener	listener,
    	Object			attachment )
    {
    	synchronized( this ){
    		
	      	write_listener		= listener;
	    	write_attachment	= attachment;  
    	} 
    	
    	resumeWriteSelects();
    }
    
    public synchronized void
    cancelReadSelects()
    {
    	read_selects_paused	= true;
      	read_listener		= null;
    	read_attachment		= null;
    }
    
    public synchronized void
    cancelWriteSelects()
    {
    	write_selects_paused	= true;
     	write_listener			= null;
    	write_attachment		= null;
    }
    
    protected void
    failed(
    	IOException	error )
    {
    	synchronized( this ){
        		
    		failed			= error;
    	
    		if ( read_listener != null && !read_selects_paused ){
    		
    			selector.ready( this, read_listener, read_attachment, failed );
    		}
    		
      		if ( write_listener != null && !write_selects_paused ){
        		
      			write_selects_paused	= true;
      			
    			selector.ready( this, write_listener, write_attachment, failed );
    		}
    	}
    }
    
    public void
    close(
    	String	reason )
    {
    	synchronized( this ){
    		
    		cancelReadSelects();
    		cancelWriteSelects();
    		
    		closed	= true;
     	}
    	
    	connection.closeSupport( reason );
    }
    
    public void
    failed(
    	Throwable	reason )
    {
    	synchronized( this ){
    		   		
    		if ( reason instanceof IOException ){
    			
    			failed = (IOException)reason;
    			
    		}else{
    			
    			failed	= new IOException( Debug.getNestedExceptionMessageAndStack(reason));
    		}
     	}
    	
    	connection.failedSupport( reason );
    }
    
	protected void
	poll()
	{
	   	synchronized( this ){
	   		
	   		if ( read_listener != null && !read_selects_paused ){
	   			
	   			if ( failed != null  ){
	   				
	   	 			selector.ready( this, read_listener, read_attachment, failed );
	   	 		  
	   			}else if ( connection.canRead()){
	   				
	   	 			selector.ready( this, read_listener, read_attachment );
	   			}
	   		}
	   		
	   		if ( write_listener != null && !write_selects_paused ){
	   			
	   			if ( failed != null  ){
	   				
	   				write_selects_paused	= true;
	   				
	   	 			selector.ready( this, write_listener, write_attachment, failed );
	   	 		  
	   			}else if ( connection.canWrite()){
	   				
	   				write_selects_paused	= true;
	   				
	   	 			selector.ready( this, write_listener, write_attachment );
	   			}
	   		}
	   	}
	}
}
