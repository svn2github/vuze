/*
 * Created on 03-Oct-2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.gudy.azureus2.core3.tracker.server.impl;

/**
 * @author gardnerpar
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

import java.util.*;
import java.net.*;
import java.io.*;

import org.gudy.azureus2.core3.tracker.server.*;
import org.gudy.azureus2.core3.util.*;

public class 
TRTrackerServerProcessor 
{
	protected static final char		CR			= '\015';
	protected static final char		FF			= '\012';
	protected static final String	NL			= "\015\012";

	protected static Map	hash_map = new HashMap();
						
	protected TRTrackerServer		server;
	
	protected
	TRTrackerServerProcessor(
		TRTrackerServer	_server,
		Socket			_socket )
	{
		server	= _server;
		
		try{
			InputStream	is = _socket.getInputStream();
			
			String	header = "";
			
			byte[]	parp = new byte[1];
			
			while( header.length()< 4096 ){
					
				if ( is.read(parp) < 0 ){
					
					break;
				}
								
				header += (char)parp[0];
								
				if ( header.endsWith( NL )){
					
					break;
				}
			}
	
			// System.out.println( "got header:" + header );
			
			if ( !header.startsWith( "GET " )){
				
				throw( new TRTrackerServerException( "header doesn't start with GET" ));
			}
			
			header = header.substring(4).trim();
			
			int	pos = header.indexOf( " " );
			
			if ( pos == -1 ){
				
				throw( new TRTrackerServerException( "header doesn't have space in right place" ));
			}
			
			header = header.substring(0,pos);
			
			processRequest( header, 
							_socket.getInetAddress().getHostAddress(),
							_socket.getOutputStream());
			
			is.close();
			
		}catch( Throwable e ){
			
			e.printStackTrace();
			
		}finally{
		
			try{
				_socket.close();
				
			}catch( Throwable e ){
				
				e.printStackTrace();
			}	
		}
	}
	
	protected synchronized void
	processRequest(
		String			str,
		String			client_ip_address,
		OutputStream	os )
		
		throws IOException
	{
		long	now = System.currentTimeMillis();
		
		try{
			Map	root = new HashMap();

			root.put( "interval", new Long( server.getRetryInterval()));
			
			if ( str.startsWith( "/announce?" )){
			
				str = str.substring(10);
				
				int	pos = 0;
				
				String		hash	= null;
				String		peer_id	= null;
				int			port	= 0;
				String		event	= null;
				
				while(true){
					
					int	p1 = str.indexOf( '&', pos );
					
					String	token;
					
					if ( p1 == -1 ){
						
						token = str.substring( pos );
						
					}else{
						
						token = str.substring( pos, p1 );
						
						pos = p1+1;
					}
				
					int	p2 = token.indexOf('=');
					
					if ( p2 == -1 ){
						
						throw( new IOException( "format invalid" ));
					}
					
					String	lhs = token.substring( 0, p2 );
					String	rhs = URLDecoder.decode(token.substring( p2+1 ), Constants.BYTE_ENCODING );
					
					// System.out.println( "param:" + lhs + " = " + rhs );
					
					if ( lhs.equals( "info_hash" )){
						
						hash	= rhs;
						
					}else if ( lhs.equals( "peer_id" )){
						
						peer_id	= rhs;
							
					}else if ( lhs.equals( "port" )){
						
						port = Integer.parseInt( rhs );
					
					}else if ( lhs.equals( "event" )){
						
						event = rhs;
					}
					
					if ( p1 == -1 ){
						
						break;
					}
				}
			
				if ( hash != null && peer_id != null ){
					
					System.out.println( "event:" + event + " - " + client_ip_address + ":" + port );
										
					if ( event != null && event.equalsIgnoreCase( "stopped" )){
				
						Map	peer_map = (Map)hash_map.get( hash );
					
						if ( peer_map != null ){
						
							TRTrackerServerPeer	peer = (TRTrackerServerPeer)peer_map.get( peer_id );
							
							if ( peer != null ){
								
								peer_map.remove( peer_id );
							
								System.out.println( "removing stopped client '" + peer.getString());
							}
						}
					}else{
								
						Map	peer_map = (Map)hash_map.get( hash );
					
						if ( peer_map == null ){
							
							peer_map = new HashMap();
							
							hash_map.put( hash, peer_map );
						}
			
						TRTrackerServerPeer	this_peer = (TRTrackerServerPeer)peer_map.get( peer_id );
					
						if ( this_peer == null ){
							
							Iterator	it = peer_map.values().iterator();
							
							while (it.hasNext()){
							
								TRTrackerServerPeer peer = (TRTrackerServerPeer)it.next();
															
								if (	peer.getPort() == port &&
										new String(peer.getIP()).equals( client_ip_address )){
									
									System.out.println( "removing dead client '" + peer.getString());
									
									it.remove();
								}
							}
							
							this_peer = new TRTrackerServerPeer( peer_id.getBytes(), client_ip_address.getBytes(), port );
							
							peer_map.put( peer_id, this_peer );
							
						}
					
						this_peer.setLastContactTime( now );
					
						List	rep_peers = new ArrayList();
			
						root.put( "peers", rep_peers );

						Iterator	it = peer_map.values().iterator();
					
						while(it.hasNext()){

							TRTrackerServerPeer	peer = (TRTrackerServerPeer)it.next();
							
							if ( (now - peer.getLastContactTime()) > server.getRetryInterval()*1000*2 ){
							
								System.out.println( "removing timedout client '" + peer.getString());
								
								it.remove();
								
							}else{
								
								Map rep_peer = new HashMap();

								rep_peers.add( rep_peer );
														 
								rep_peer.put( "peer id", peer.getPeerId() );
								rep_peer.put( "ip", peer.getIP() );
								rep_peer.put( "port", new Long( peer.getPort()));
							}
						}
					}
				}
			}else{
			
			}

			byte[] data = BEncoder.encode( root );
			
			System.out.println( "sending " + new String(data));
			
			String header = "HTTP/1.0 200 OK" + NL + 
							"Server: PG/1.0.1" + NL +
							"Mime-version: 1.0" + NL +
							"Content-type: text/html" + 
							"Content-Length: " + data.length + 
							NL + NL;

			os.write( header.getBytes());
			
			os.write( data );
						
			os.flush();
			
		}finally{
			
			os.close();
		}
	}
}
