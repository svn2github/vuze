/*
 * File    : TRTrackerServerProcessor.java
 * Created : 5 Oct. 2003
 * By      : Parg 
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

package org.gudy.azureus2.core3.tracker.server.impl;


import java.io.*;
import java.net.*;
import java.util.*;

import org.gudy.azureus2.core3.tracker.server.*;
import org.gudy.azureus2.core3.util.*;

public class 
TRTrackerServerProcessor 
{
	protected static final char		CR			= '\015';
	protected static final char		FF			= '\012';
	protected static final String	NL			= "\015\012";

	protected static final int		RT_ANNOUNCE	= 1;
	protected static final int		RT_SCRAPE	= 2;
	
	protected static Map	hash_map = new HashMap();
						
	protected TRTrackerServerImpl		server;
	
	protected
	TRTrackerServerProcessor(
		TRTrackerServerImpl	_server,
		Socket				_socket )
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
	
			System.out.println( "got header:" + header );
			
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
		try{
			Map	root = new HashMap();
				
			try{
				int	request_type;
			
				if ( str.startsWith( "/announce?" )){
					
					request_type	= RT_ANNOUNCE;
					
					str = str.substring(10);
					
				}else if ( str.startsWith( "/scrape?" )){
					
					request_type	= RT_SCRAPE;
					
					str = str.substring(8);
				
				}else{
					
					throw( new Exception( "Unsupported Request Type"));
				}
					
				int	pos = 0;
					
				String		hash_str	= null;
				String		peer_id		= null;
				int			port		= 0;
				String		event		= null;
					
				long		uploaded		= 0;
				long		downloaded		= 0;
				long		left			= 0;
				int			num_peers		= 0;
					
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
							
						throw( new Exception( "format invalid" ));
					}
						
					String	lhs = token.substring( 0, p2 );
					String	rhs = URLDecoder.decode(token.substring( p2+1 ), Constants.BYTE_ENCODING );
						
					// System.out.println( "param:" + lhs + " = " + rhs );
						
					if ( lhs.equals( "info_hash" )){
							
						hash_str	= rhs;
							
					}else if ( lhs.equals( "peer_id" )){
							
						peer_id	= rhs;
								
					}else if ( lhs.equals( "port" )){
							
						port = Integer.parseInt( rhs );
						
					}else if ( lhs.equals( "event" )){
							
						event = rhs;
							
					}else if ( lhs.equals( "uploaded" )){
							
						uploaded = Integer.parseInt( rhs );
						
					}else if ( lhs.equals( "downloaded" )){
							
						downloaded = Integer.parseInt( rhs );
						
					}else if ( lhs.equals( "left" )){
							
						left = Integer.parseInt( rhs );
						
					}else if ( lhs.equals( "num_peers" )){
							
						num_peers = Integer.parseInt( rhs );
					}
						
					if ( p1 == -1 ){
							
						break;
					}
				}

				if ( hash_str == null ){
					
					throw( new Exception( "Hash missing from request "));
				}
										
				System.out.println( "request:" + request_type + ",event:" + event + " - " + client_ip_address + ":" + port );
																		
				TRTrackerServerTorrent	torrent = server.getTorrent( hash_str.getBytes(Constants.BYTE_ENCODING));
					
				if ( torrent == null ){
							
					throw( new Exception( "Torrent unauthorised "));
				}
			
				if ( request_type == RT_ANNOUNCE ){
				
					if ( peer_id == null ){
						
						throw( new Exception( "peer_id missing from request"));
					}
					
					torrent.peerContact( 	event, 
											peer_id, port, client_ip_address,
											uploaded, downloaded, left, num_peers );
					
					torrent.exportPeersToMap( root );
	
					root.put( "interval", new Long( server.getRetryInterval()));
					
				}else{
					
					Map	files = new ByteEncodedKeyHashMap();
					
					Map	hash_entry = new HashMap();
					
					byte[]	torrent_hash = torrent.getHash().getHash();
									
					String	str_hash = new String( torrent_hash,Constants.BYTE_ENCODING );
				
					//System.out.println( "xxx: " + ByteFormatter.nicePrint(torrent_hash) + " -> " + ByteFormatter.nicePrint( str_hash.getBytes( Constants.BYTE_ENCODING )));
	
					files.put( str_hash, hash_entry );
					
					TRTrackerServerPeer[]	peers = torrent.getPeers();
					
					long	seeds 		= 0;
					long	non_seeds	= 0;
					
					for (int i=0;i<peers.length;i++){
						
						if ( peers[i].getAmountLeft() == 0 ){
							
							seeds++;
						}else{
							non_seeds++;
						}
					}
					
					hash_entry.put( "complete", new Long( seeds ));
					hash_entry.put( "incomplete", new Long( non_seeds ));
					
					root.put( "files", files );
				}
				
			}catch( Exception e ){
				
				root.put( "failure reason", e.getMessage());
			}
		
			byte[] data = BEncoder.encode( root );
				
			System.out.println( "sending " + new String(data));
				
			String header = "HTTP/1.1 200 OK" + NL + 
							"Content-Type: text/html" + NL +
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
