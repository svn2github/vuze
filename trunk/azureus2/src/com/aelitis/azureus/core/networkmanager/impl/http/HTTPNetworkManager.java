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

import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.ByteBuffer;

import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.peer.impl.PEPeerTransport;
import org.gudy.azureus2.core3.util.ByteFormatter;

import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
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
		    	public int size() {  return 256;  }
		    	public int minSize() { return 3; }

		    	public Object
		    	matches( 
		    		InetSocketAddress	address,
		    		ByteBuffer 			to_compare, 
		    		int 				port ) 
		    	{ 
		    		int old_limit 		= to_compare.limit();
		    		int old_position 	= to_compare.position();

		    		try{
			    		byte[]	head = new byte[3];
			    		
			    		to_compare.get( head );
			    		
			    			// note duplication of this in min-matches below
			    		
			    		if (head[0] != 'G' || head[1] != 'E' || head[2] != 'T' ){
			    			
			    			return( null );
			    		}
			    		
			    		byte[]	line = new byte[to_compare.remaining()];
			    		
			    		to_compare.get( line );
			    		
			    		try{
				    		String	str = new String( line, "ISO-8859-1" );
				    		
				    		System.out.println( address + " - matches: " + str );
			
				    		int	ws_pos = str.indexOf( "/webseed?info_hash=" );
				    		
				    		if ( ws_pos != -1 ){
				    			
				    			System.out.println( "web seed" );
				    			
				    			int	hash_start = ws_pos + 19;
				    			
				    			int	hash_end = str.indexOf( '&', ws_pos );
				    			
				    			if ( hash_end != -1 ){
				    				
				    				byte[]	hash = URLDecoder.decode( str.substring(hash_start,hash_end), "ISO-8859-1" ).getBytes( "ISO-8859-1" );
				    				
				    				System.out.println( "    hash " + ByteFormatter.encodeString( hash ));
				    				
				    				PeerManagerRegistration reg_data = PeerManager.getSingleton().manualMatchHash( address, hash );
				    				
				    				if ( reg_data != null ){
				    					
				    					return( reg_data );
				    				}
				    			}
				    		}
			    		
				    		return( null );
				    		
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
		    		InetSocketAddress	address,
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

		    	public byte[] 
		    	getSharedSecret()
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
	        		Object 				_routing_data ) 
	        	{
	        		PeerManagerRegistration	routing_data = (PeerManagerRegistration)_routing_data;
	        		
   					if (Logger.isEnabled()){
						Logger.log(new LogEvent(LOGID, "HTTP connection from " + connection.getEndpoint().getNotionalAddress() + " routed successfully" ));
   					}   					
   					   					
	        		PeerManager.getSingleton().manualRoute(
	        				routing_data, 
	        				connection,
	        				new PeerManagerRoutingListener()
	        				{
	        					public void
	        					routed(
	        						PEPeerTransport		peer )
	        					{
	        	  					new HTTPNetworkConnection( connection, peer );
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
}
