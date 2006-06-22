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

package com.aelitis.azureus.core.networkmanager.impl;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.gudy.azureus2.core3.util.AEMonitor;

import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.networkmanager.Transport;

public class 
IncomingConnectionManager 
{
	private static IncomingConnectionManager	singleton = new IncomingConnectionManager();
	
	public static IncomingConnectionManager
	getSingleton()
	{
		return( singleton );
	}
	
	private volatile Map match_buffers_cow = new HashMap();	// copy-on-write
	private final AEMonitor match_buffers_mon = new AEMonitor( "IncomingConnectionManager:match" );
	private int max_match_buffer_size = 0;
	private int max_min_match_buffer_size = 0;

	public boolean
	isEmpty()
	{
		return( match_buffers_cow.isEmpty());
	}
	
	public MatchListener 
	checkForMatch( 
		int	incoming_port, ByteBuffer to_check, boolean min_match ) 
	{ 
	       //remember original values for later restore
	      int orig_position = to_check.position();
	      int orig_limit = to_check.limit();
	      
	      //rewind
	      to_check.position( 0 );

	      MatchListener listener = null;
	           
	      for( Iterator i = match_buffers_cow.entrySet().iterator(); i.hasNext(); ) {
	        Map.Entry entry = (Map.Entry)i.next();
	        NetworkManager.ByteMatcher bm = (NetworkManager.ByteMatcher)entry.getKey();
	        
	        if ( min_match ){
	            if( orig_position < bm.minSize() ) {  //not enough bytes yet to compare
	  	          continue;
	  	        }
	  	                
	  	        if( bm.minMatches( to_check, incoming_port ) ) {  //match found!
	  	          listener = (MatchListener)entry.getValue();
	  	          break;
	  	        }      	
	        }else{
		        if( orig_position < bm.size() ) {  //not enough bytes yet to compare
		          continue;
		        }
		                
		        if( bm.matches( to_check, incoming_port ) ) {  //match found!
		          listener = (MatchListener)entry.getValue();
		          break;
		        }
	        }
	      }

	      //restore original values in case the checks changed them
	      to_check.position( orig_position );
	      to_check.limit( orig_limit );
	      
	      return listener;
	  }
	  
	  /**
	   * Register the given byte sequence matcher to handle matching against new incoming connection
	   * initial data; i.e. the first bytes read from a connection must match in order for the given
	   * listener to be invoked.
	   * @param matcher byte filter sequence
	   * @param listener to call upon match
	   */
	  public void registerMatchBytes( NetworkManager.ByteMatcher matcher, MatchListener listener ) {
	    try {  match_buffers_mon.enter();
	    
	      if( matcher.size() > max_match_buffer_size ) {
	        max_match_buffer_size = matcher.size();
	      }

	      if ( matcher.minSize() > max_min_match_buffer_size ){
	    	  max_min_match_buffer_size = matcher.minSize();
	      }
	      
	      Map	new_match_buffers = new HashMap( match_buffers_cow );
	      
	      new_match_buffers.put( matcher, listener );
	      
	      match_buffers_cow = new_match_buffers;
	    
	      byte[]	secret = matcher.getSharedSecret();
	      
	      if ( secret != null ){
	    	  
		     ProtocolDecoder.addSecret( secret );
	      }
	    } finally {  match_buffers_mon.exit();  }
	    
	  }
	  
	  
	  /**
	   * Remove the given byte sequence match from the registration list.
	   * @param to_remove byte sequence originally used to register
	   */
	  public void deregisterMatchBytes( NetworkManager.ByteMatcher to_remove ) {
	    try {  match_buffers_mon.enter();
	      Map	new_match_buffers = new HashMap( match_buffers_cow );
	    
	      new_match_buffers.remove( to_remove );
	    
	      if( to_remove.size() == max_match_buffer_size ) { //recalc longest buffer if necessary
	        max_match_buffer_size = 0;
	        for( Iterator i = new_match_buffers.keySet().iterator(); i.hasNext(); ) {
	          NetworkManager.ByteMatcher bm = (NetworkManager.ByteMatcher)i.next();
	          if( bm.size() > max_match_buffer_size ) {
	            max_match_buffer_size = bm.size();
	          }
	        }
	      }
	    
	      match_buffers_cow = new_match_buffers;
	      
	      byte[]	secret = to_remove.getSharedSecret();
	      
	      if ( secret != null ){
	    	  
		      ProtocolDecoder.removeSecret( secret );
	      }
	    } finally {  match_buffers_mon.exit();  }  
	  } 
	  
	  public int
	  getMaxMatchBufferSize()
	  {
		  return( max_match_buffer_size );
	  }
	  
	  public int
	  getMaxMinMatchBufferSize()
	  {
		  return( max_min_match_buffer_size );
	  }
	  
	  /**
	   * Listener for byte matches.
	   */
	  public interface MatchListener {
	    
		  /**
		   * Currently if message crypto is on and default fallback for incoming not
		   * enabled then we would bounce incoming messages from non-crypto transports
		   * For example, NAT check
		   * This method allows auto-fallback for such transports
		   * @return
		   */
		public boolean
		autoCryptoFallback();
		
	    /**
	     * The given socket has been accepted as matching the byte filter.
	     * @param channel matching accepted connection
	     * @param read_so_far bytes already read
	     */
	    public void connectionMatched( Transport	transport );
	  }
	    
}
