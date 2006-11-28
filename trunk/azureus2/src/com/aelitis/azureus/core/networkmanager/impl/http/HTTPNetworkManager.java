/*
 * Created on 2 Oct 2006
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
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.networkmanager.impl.http;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.peer.impl.PEPeerTransport;
import org.gudy.azureus2.core3.util.BEncoder;

import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.networkmanager.impl.TransportHelper;
import com.aelitis.azureus.core.networkmanager.impl.tcp.IncomingSocketChannelManager;
import com.aelitis.azureus.core.peermanager.PeerManager;
import com.aelitis.azureus.core.peermanager.PeerManagerRegistration;
import com.aelitis.azureus.core.peermanager.PeerManagerRoutingListener;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamDecoder;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamEncoder;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamFactory;

public class 
HTTPNetworkManager 
{
	private static final String	NL			= "\r\n";

	private static final LogIDs LOGID = LogIDs.NWMAN;

	private static final HTTPNetworkManager instance = new HTTPNetworkManager();

	public static HTTPNetworkManager getSingleton(){ return( instance ); }

	
	private final IncomingSocketChannelManager http_incoming_manager;

	private 
	HTTPNetworkManager()
	{	
		/*
		try{
			System.out.println( "/webseed?info_hash=" + URLEncoder.encode( new String( ByteFormatter.decodeString("C9C04D96F11FB5C5ECC99D418D3575FBFC2208B0"), "ISO-8859-1"), "ISO-8859-1" ));
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
		*/
		
		http_incoming_manager = new IncomingSocketChannelManager( "HTTP.Data.Listen.Port", "HTTP.Data.Listen.Port.Enable" );
		
		NetworkManager.ByteMatcher matcher =
		   	new NetworkManager.ByteMatcher() 
		    {
				public int matchThisSizeOrBigger(){	return( 4 + 1 + 11 ); } // GET ' ' <url of 1> ' HTTP/1.1<cr><nl>'
				
		    	public int maxSize() { return 256; }	// max GET <url> size - boiler plate plus small url plus hash
		    	public int minSize() { return 3; }		// enough to match GET

		    	public Object
		    	matches( 
		    		TransportHelper		transport,
		    		ByteBuffer 			to_compare, 
		    		int 				port ) 
		    	{ 
		    		InetSocketAddress	address = transport.getAddress();
		    		
		    		int old_limit 		= to_compare.limit();
		    		int old_position 	= to_compare.position();

		    		try{
			    		byte[]	head = new byte[3];
			    		
			    		to_compare.get( head );
			    		
			    			// note duplication of this in min-matches below
			    		
			    		if ( head[0] != 'G' || head[1] != 'E' || head[2] != 'T' ){
			    			
			    			return( null );
			    		}
			    		
			    		byte[]	line_bytes = new byte[to_compare.remaining()];
			    		
			    		to_compare.get( line_bytes );
			    		
			    		try{
			    				// format is GET url HTTP/1.1<NL>
			    			
				    		String	url = new String( line_bytes, "ISO-8859-1" );
				    		
				    		int	space = url.indexOf(' ');
				    		
				    		if ( space == -1 ){
				    			
				    			return( null );
				    		}
				    		
				    			// note that we don't insist on a full URL here, just the start of one
				    		
				    		url = url.substring( space + 1 ).trim();
				    				
				    		if ( url.indexOf( "/index.html") != -1 ){
				    			
					    		return( new Object[]{ transport, getIndexPage() });
					    		
				    		}else if ( url.indexOf( "/ping.html") != -1 ){
					    			
				    				// ping is used for inbound HTTP port checking
				    			
						    	return( new Object[]{ transport, getPingPage( url ) });

				    		}else if ( url.indexOf( "/test503.html" ) != -1 ){
				    			
					    		return( new Object[]{ transport, getTest503()});
				    		}

				    		String	hash_str = null;
				    		
				    		int	hash_pos = url.indexOf( "?info_hash=" );
				    		
				    		if ( hash_pos != -1 ){
				    							    			
				    			int	hash_start = hash_pos + 11;
				    			
				    			int	hash_end = url.indexOf( '&', hash_pos );
				    							    			
				    			if ( hash_end == -1 ){
				    				
				    					// not read the end yet
				    				
				    				return( null );
				    				
				    			}else{
				    				
				    				hash_str = url.substring( hash_start, hash_end );
				    			}
				    		}else{
			    		
				    			hash_pos = url.indexOf( "/files/" );
					    		
					    		if ( hash_pos != -1 ){
					    							    			
					    			int	hash_start = hash_pos + 7;
	
					    			int	hash_end = url.indexOf('/', hash_start );
					    			
					    			if ( hash_end == -1 ){
					    				
					    					// not read the end of the hash yet
					    				
					    				return( null );
					    				
					    			}else{
					    				
					    				hash_str = url.substring( hash_start, hash_end );
					    			}
					    		}
				    		}
				    		
				    		if ( hash_str != null ){
			    				
			    				byte[]	hash = URLDecoder.decode( hash_str, "ISO-8859-1" ).getBytes( "ISO-8859-1" );
			    								    				
			    				PeerManagerRegistration reg_data = PeerManager.getSingleton().manualMatchHash( address, hash );
			    				
			    				if ( reg_data != null ){
			    					
			    						// trim back URL as it currently has header in it too
			    					
			    					int	pos = url.indexOf( ' ' );
			    					
			    					String	trimmed = pos==-1?url:url.substring(0,pos);
			    					
			    					return( new Object[]{ trimmed, reg_data });
			    				}
				    		}
				    		
		   					if (Logger.isEnabled()){
	    						Logger.log(new LogEvent(LOGID, "HTTP decode from " + address + " failed: no match for " + url ));
	    					}
		   					
				    		return( new Object[]{ transport, getNotFound() });
				    		
			    		}catch( Throwable e ){
			    			
		   					if (Logger.isEnabled()){
	    						Logger.log(new LogEvent(LOGID, "HTTP decode from " + address + " failed, " + e.getMessage()));
	    					}
	
		   					return( null );
			    		}
		    		}finally{
		    		
			    			//	restore buffer structure
		    			
			    		to_compare.limit( old_limit );
			    		to_compare.position( old_position );
		    		}
		    	}
		    	
		    	public Object 
		    	minMatches( 
		    		TransportHelper		transport,
		    		ByteBuffer 			to_compare, 
		    		int 				port ) 
		    	{ 
		    		byte[]	head = new byte[3];
		    		
		    		to_compare.get( head );
		    		
		    		if (head[0] != 'G' || head[1] != 'E' || head[2] != 'T' ){
		    			
		    			return( null );
		    		}

		    		return( "" );
		    	}

		    	public byte[][] 
		    	getSharedSecrets()
		    	{
		    		return( null );	
		    	}
		    	
		    	 public int 
		    	 getSpecificPort()
		    	 {
		    		 return( http_incoming_manager.getTCPListeningPortNumber());
		    	 }
		    };
		    
	    // register for incoming connection routing
	    NetworkManager.getSingleton().requestIncomingConnectionRouting(
	        matcher,
	        new NetworkManager.RoutingListener() 
	        {
	        	public void 
	        	connectionRouted( 
	        		final NetworkConnection 	connection, 
	        		Object 						_routing_data ) 
	        	{
	        		Object[]	x = (Object[])_routing_data;
	        		
	        		if ( x[0] instanceof TransportHelper ){
	        			
	        				// routed on failure
	        			
	        			writeReply(connection, (TransportHelper)x[0], (String)x[1]);
	        			
	        			return;
	        		}
	        		
	        		final String					url 			= (String)x[0];
	        		final PeerManagerRegistration	routing_data 	= (PeerManagerRegistration)x[1];
	        		
   					if (Logger.isEnabled()){
						Logger.log(new LogEvent(LOGID, "HTTP connection from " + connection.getEndpoint().getNotionalAddress() + " routed successfully on '" + url + "'" ));
   					}   					
   					   					
	        		PeerManager.getSingleton().manualRoute(
	        				routing_data, 
	        				connection,
	        				new PeerManagerRoutingListener()
	        				{
	        					public boolean
	        					routed(
	        						PEPeerTransport		peer )
	        					{
	        						if ( url.indexOf( "/webseed" ) != -1 ){
	        							
	        							new HTTPNetworkConnectionWebSeed( HTTPNetworkManager.this, connection, peer, url );
	        							
	        							return( true );
	        							
	        						}else if ( url.indexOf( "/files/" ) != -1 ){

	        							new HTTPNetworkConnectionFile( HTTPNetworkManager.this, connection, peer, url );
	        							
	        							return( true );
	        						}
	        						
	        						return( false );
	        					}
	        				});
	        	}
	        	
	        	public boolean
	      	  	autoCryptoFallback()
	        	{
	        		return( false );
	        	}
	        	},
	        new MessageStreamFactory() {
	          public MessageStreamEncoder createEncoder() {  return new HTTPMessageEncoder();  }
	          public MessageStreamDecoder createDecoder() {  return new HTTPMessageDecoder();  }
	        });
	}
	
	public int
	getHTTPListeningPortNumber()
	{
		return( http_incoming_manager.getTCPListeningPortNumber());
	}
	
	public void
	setExplicitBindAddress(
			InetAddress	address )
	{
		http_incoming_manager.setExplicitBindAddress( address );
	}
	  
	protected String
	getIndexPage()
	{
		return( "HTTP/1.1 200 OK" + NL + 
				"Connection: Close" + NL +
				"Content-Length: 0" + NL +
				NL );
	}
	
	protected String
	getPingPage(
		String	url )
	{
		int	pos = url.indexOf( ' ' );

		if ( pos != -1 ){
			
			url = url.substring( 0, pos );
		}
		
		pos = url.indexOf( '?' );
		
		Map	response = new HashMap();
		
		boolean	ok = false;
		
		if ( pos != -1 ){
			
			StringTokenizer tok = new StringTokenizer(url.substring(pos+1), "&");
			
			while( tok.hasMoreTokens()){
				
				String	token = tok.nextToken();
				
				pos	= token.indexOf('=');
				
				if ( pos != -1 ){
					
					String	lhs = token.substring(0,pos);
					String	rhs = token.substring(pos+1);
					
					if ( lhs.equals( "check" )){
						
						response.put( "check", rhs );
						
						ok = true;
					}
				}
			}
		}
		
		if ( ok ){
			
			try{
				byte[]	bytes = BEncoder.encode( response );
			
				byte[]	length = new byte[4];
				
				ByteBuffer.wrap( length ).putInt( bytes.length );
								
				return( "HTTP/1.1 200 OK" + NL + 
						"Connection: Close" + NL +
						"Content-Length: " + ( bytes.length + 4 )+ NL +
						NL + 
						new String( length, "ISO-8859-1" ) + new String( bytes, "ISO-8859-1" ) );
				
			}catch( Throwable e ){
			}
		}
		
		return( getNotFound());

	}

	protected String
	getTest503()
	{
		return( "HTTP/1.1 503 Service Unavailable" + NL + 
				"Connection: Close" + NL +
				"Content-Length: 4" + NL +
				NL + 
				"1234" );
	}
	
	protected String
	getNotFound()
	{
		return( "HTTP/1.1 404 Not Found" + NL +
				"Connection: Close" + NL +
				"Content-Length: 0" + NL +
				NL );
	}
	
	protected String
	getRangeNotSatisfiable()
	{
		return( "HTTP/1.1 416 Not Satisfiable" + NL +
				"Connection: Close" + NL +
				"Content-Length: 0" + NL +
				NL );
	}
	
	
	protected void
	writeReply(
		final NetworkConnection		connection,
		final TransportHelper		transport,
		final String				data )
	{
		byte[]	bytes;
		
		try{
			bytes = data.getBytes( "ISO-8859-1" );
			
		}catch( UnsupportedEncodingException e ){
			
			bytes = data.getBytes();
		}
				
		final ByteBuffer bb = ByteBuffer.wrap( bytes );
		
		try{
			transport.write( bb, false );
			
			if ( bb.remaining() > 0 ){
				
				transport.registerForWriteSelects(
					new TransportHelper.selectListener()
					{
					  	public boolean 
				    	selectSuccess(
				    		TransportHelper	helper, 
				    		Object 			attachment )
					  	{
					  		try{
					  			int written = helper.write( bb, false );
					  			
					  			if ( bb.remaining() > 0 ){
					  			
					  				helper.registerForWriteSelects( this, null );
					  				
					  			}else{
					  				
				  					if (Logger.isEnabled()){
										Logger.log(new LogEvent(LOGID, "HTTP connection from " + connection.getEndpoint().getNotionalAddress() + " closed" ));
				   					}   					

									connection.close();
					  			}
					  			
					  			return( written > 0 );
					  			
					  		}catch( Throwable e ){
					  			
					  			helper.cancelWriteSelects();
					  			
			  					if (Logger.isEnabled()){
									Logger.log(new LogEvent(LOGID, "HTTP connection from " + connection.getEndpoint().getNotionalAddress() + " failed to write error '" + data + "'" ));
			   					}   					

					  			connection.close();
					  			
					  			return( false );
					  		}
					  	}

				        public void 
				        selectFailure(
				        	TransportHelper	helper,
				        	Object 			attachment, 
				        	Throwable 		msg)
				        {
				        	helper.cancelWriteSelects();
				        	
		  					if (Logger.isEnabled()){
								Logger.log(new LogEvent(LOGID, "HTTP connection from " + connection.getEndpoint().getNotionalAddress() + " failed to write error '" + data + "'" ));
		   					}   					

				        	connection.close();
				        }
					},
					null );
			}else{

				if (Logger.isEnabled()){
					Logger.log(new LogEvent(LOGID, "HTTP connection from " + connection.getEndpoint().getNotionalAddress() + " closed" ));
   				}   					

				connection.close();
			}
		}catch( Throwable e ){
			
			if (Logger.isEnabled()){
				Logger.log(new LogEvent(LOGID, "HTTP connection from " + connection.getEndpoint().getNotionalAddress() + " failed to write error '" + data + "'" ));
			}   					

			connection.close();
		}
	}
}
