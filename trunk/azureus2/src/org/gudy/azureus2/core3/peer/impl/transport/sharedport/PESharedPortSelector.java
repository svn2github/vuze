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
	
	protected Selector	  selector;
	
	protected List		register_list		= new ArrayList(16);
			
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
		Map		outstanding_sockets	= new HashMap();
		
		List	sockets_to_handover = new ArrayList();
        
      SelectorGuard selectorGuard = new SelectorGuard(50, 100);
		
		while (true){
			
			try{
				
					// concurrent access to the selector from a second thread causes problems
					// Initially null-pointer exceptions were generated within the Select itself.
					// This was "fixed" by synchronizing the registration and post-select key
					// operations. However, there still remained the problem that on a Linux syste,
					// the thread management was such that the register was being help up by the
					// fact that the registration occurred when the selector was selecting. Hence
					// the move to stick them on a list and pick them up here
				
				synchronized( register_list ){
					
					if ( register_list.size() > 0 ){
						
						for (int i=0;i<register_list.size();i++){
						
							socketData	sd = (socketData)register_list.get(i);
							
							SocketChannel socket = sd.getSocket();
							
							try{
								outstanding_sockets.put( socket, sd );
	
								socket.register(selector,SelectionKey.OP_READ);
								
							}catch( Exception e ){
								
								e.printStackTrace();
								
								outstanding_sockets.remove( socket );
								
								try{
									socket.close();
								}
								catch( IOException f ){ f.printStackTrace(); }
							}								
						}
						
						register_list.clear();
					}
				}
				
				int select_res = selector.select(500);
                
            //make sure the selector is OK
            if (!selectorGuard.isSelectorOK(select_res)) {
            	selector = selectorGuard.repairSelector(selector);
            }
			 			
					// make sure that any socket removed in the previous loop are now handed over
					// do this *after* subsequent select to ensure that the removes have
					// been processed before the socket is used elsewhere. Not sure if this
					// is essential but the code was added when looking into other
					// problems and might as well stay here for the moment :)
				
				for (int i=0;i<sockets_to_handover.size();i++){
			  	
			  		socketData	sd = (socketData) sockets_to_handover.get(i);
			  	                          
					sd.getHandoverServer().connectionReceived( sd.getSocket(), sd.getHandoverData());
				}
			  
			  	sockets_to_handover.clear();
			  
			  	if ( select_res > 0 ){
			  				  	
			    	Iterator ready_it = selector.selectedKeys().iterator();
			    
        	    	while (ready_it.hasNext()){
        	    	
				  		SelectionKey key = (SelectionKey)ready_it.next();
				  
			      		ready_it.remove();
			      
			      		SocketChannel channel = (SocketChannel)key.channel();
			      
			      		boolean remove_this_key = false;
					
						if ( key.isValid() && key.isReadable() ){
						
					  		socketData socket_data = (socketData)outstanding_sockets.get( channel );
					  
					  		if ( socket_data == null ){
					  	
					    		LGLogger.log(0, 0, LGLogger.ERROR, getIP(channel) + " : PESharedPortSelector: failed to find socket buffer" );
					    
					    		remove_this_key = true;
					    
					  		}else{
					  			
					    		try{
					    
					      			ByteBuffer buffer = socket_data.getBuffer();
					    
					      			int len = channel.read(buffer);
              
					      			if ( len < 0 ){  //other end closed the connection
					    
					        			remove_this_key = true;
					      			}else{
					      				
					        			if ( buffer.position() >= 48 ){
					        				
					          				byte[]	contents = new byte[buffer.position()];
					          				
					          				buffer.flip();
					          				
					          				buffer.get( contents );
					          				
                    						HashWrapper	hw = new HashWrapper(contents,28,20);
                    						
                       		  				PESharedPortServerImpl server = (PESharedPortServerImpl)hash_map.get( hw );
                         
		                         			if ( server == null ){
		                         				
		                           				remove_this_key = true;
		                           				
		                           				LGLogger.log(0, 0, LGLogger.ERROR, getIP(channel) + " : PESharedPortSelector: failed to find matching info hash" );
		                           				
										 	}else{
		                           				outstanding_sockets.remove( channel );
		                           				
		                           				key.cancel();
		                           
		    					   				socket_data.setHandoverServer( server );
		    					   
		                           				socket_data.setHandoverData( contents );
		                           
		                           				sockets_to_handover.add( socket_data );
		                          			}
						  	  			}
					  				}
					    		}catch( IOException e ){
					    			
					      			remove_this_key	= true;
					      			
					      			LGLogger.log(0, 0, LGLogger.ERROR, getIP(channel) + " : PESharedPortSelector: error occurred during socket read: " + e.toString());
					  	 		}
              
				  	  		}
						}else{ 
						
							Debug.out("key is invalid or is not readable"); 
						}
					
						if ( remove_this_key ){
						
					  		outstanding_sockets.remove( channel );
					 	
					 			// no need to cancel the key as close does it for us
					 	
		              		try{
						    	channel.close();
						    	
					  		}catch( IOException e ){ 
					  		}
						}
        	    	} 

				
			    	Iterator	keys_it = selector.keys().iterator();
			    
					long	now = System.currentTimeMillis();
        
					while( keys_it.hasNext() ){
					
				  		SelectionKey key = (SelectionKey)keys_it.next();
				  
				  		SocketChannel channel = (SocketChannel)key.channel();
				  
				  		socketData socket_data = (socketData)outstanding_sockets.get( channel );
          
				  		if ( socket_data != null ){
				  	
				    		if ( now - socket_data.getLastUseTime() > SOCKET_TIMEOUT ){
				    	
				      			LGLogger.log(0, 0, LGLogger.INFORMATION, getIP(socket_data.getSocket())+" : PESharedPortSelector: timed out socket connection" );
				      
				      			outstanding_sockets.remove( channel );
				      
				      				// no need to cancel the key as close does it for us
				      								
					  			try{
						  			channel.close();
					  			}catch( IOException e ){
					  			} 
						  	}	
				    	}	
				  	}				
			  	}          
			}catch( Throwable e ){
				
					// we should never get here - possible nio bug producing null pointer
					// exceptions in select statement
					
				if ( e instanceof NullPointerException ){
				
					Debug.out("null pointer exception");      

				}
				
			  	e.printStackTrace();
			  	
				LGLogger.log(0, 0, "PESharedPortSelector: error occurred during processing: " + e.toString(), e);
				
				try{
						// make sure we don't spin here
						
					Thread.sleep(1000);
				}
				catch( InterruptedException f ){ f.printStackTrace(); }
				
					// recreate the selector
										
				try{
					selector.close();
					
				}catch( Throwable f ){
				}
			
				try{
		
					selector = Selector.open();
			
					Iterator	s_it = outstanding_sockets.keySet().iterator();
				
					while( s_it.hasNext()){
					
						SocketChannel channel = (SocketChannel)s_it.next();
		
						channel.register(selector,SelectionKey.OP_READ);
					}
				}catch( Throwable f ){
					
						// worst case, drop the connections
						
					try{
						selector.close();
						
					}catch( Throwable g ){
					}
					
					try{
					
						selector = Selector.open();
						
						Iterator	s_it = outstanding_sockets.keySet().iterator();
				
						while( s_it.hasNext()){
					
							SocketChannel channel = (SocketChannel)s_it.next();
							
							try{
								channel.close();
							}catch( Throwable h ){
							}
						}
						
						outstanding_sockets.clear();
						
					}catch( Throwable g ){
					}
				}
			}
		}
	}
	
	public void
	addSocket(
		SocketChannel		_socket )
	{		
		synchronized( register_list ){
			
			socketData	sd = new socketData( _socket );
     
			register_list.add( sd );
		}
		
		selector.wakeup();
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
		
		protected PESharedPortServerImpl	server;
		protected byte[]					data;
		
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
		
		protected void
		setHandoverData(
			byte[]		d )
		{
			data	= d;
		}
		
		protected byte[]
		getHandoverData()
		{
			return( data );
		}
		
		protected void
		setHandoverServer(
			PESharedPortServerImpl	s )
		{
			server	= s;
		}
		
		protected PESharedPortServerImpl
		getHandoverServer()
		{
			return( server );
		}
	}
}
