/*
 * Created on Jan 20, 2005
 * Created by Alon Rohter
 * Copyright (C) 2004-2005 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.core.peermanager;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.peer.PEPeerSource;
import org.gudy.azureus2.core3.peer.impl.*;
import org.gudy.azureus2.core3.peer.util.PeerIdentityManager;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.networkmanager.*;
import com.aelitis.azureus.core.networkmanager.NetworkManager.ByteMatcher;
import com.aelitis.azureus.core.networkmanager.impl.IncomingConnectionManager;
import com.aelitis.azureus.core.peermanager.download.TorrentDownload;
import com.aelitis.azureus.core.peermanager.download.TorrentDownloadFactory;
import com.aelitis.azureus.core.peermanager.messaging.*;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.*;

/**
 *
 */
public class PeerManager {
  private static final LogIDs LOGID = LogIDs.PEER;

  private static final boolean MUTLI_CONTROLLERS	= COConfigurationManager.getBooleanParameter( "peer.multiple.controllers.per.torrent.enable", false );
	
  private static final PeerManager instance = new PeerManager();

 

  /**
   * Get the singleton instance of the peer manager.
   * @return the peer manager
   */
  public static PeerManager getSingleton() {  return instance;  }
  

  
  
  
  private final HashMap active_legacy_managers 		= new HashMap();
  private final HashMap registered_legacy_managers 	= new HashMap();
   
  private final ByteBuffer legacy_handshake_header;
  
  private final AEMonitor	managers_mon = new AEMonitor( "PeerManager:managers" );
  

  private PeerManager() {
    legacy_handshake_header = ByteBuffer.allocate( 20 );
    legacy_handshake_header.put( (byte)BTHandshake.PROTOCOL.length() );
    legacy_handshake_header.put( BTHandshake.PROTOCOL.getBytes() );
    legacy_handshake_header.flip();
    
    MessageManager.getSingleton().initialize();  //ensure it gets initialized
    
    NetworkManager.ByteMatcher matcher =
    	new NetworkManager.ByteMatcher() 
    {
    	public int size() {  return 48;  }
    	public int minSize() { return 20; }

    	public Object
    	matches( 
    		InetSocketAddress	address,
    		ByteBuffer 			to_compare, 
    		int 				port ) 
    	{ 

    		if ( MUTLI_CONTROLLERS ){
    			return( null );
    		}

    		int old_limit = to_compare.limit();
    		int old_position = to_compare.position();

    		to_compare.limit( old_position + 20 );

    		PeerManagerRegistrationImpl	routing_data = null;
    		
    		if( to_compare.equals( legacy_handshake_header ) ) {  //compare header 
    			to_compare.limit( old_position + 48 );
    			to_compare.position( old_position + 28 );

    			byte[]	hash = new byte[to_compare.remaining()];
    			
    			to_compare.get( hash );
    			
    			try{
    				managers_mon.enter();
    				  		
    				routing_data = (PeerManagerRegistrationImpl)registered_legacy_managers.get( new HashWrapper( hash ));
    				
    			}finally{
    				
    				managers_mon.exit();
    			}
    		}

    		//restore buffer structure
    		to_compare.limit( old_limit );
    		to_compare.position( old_position );

    		if ( routing_data != null ){
    			
    			if ( !routing_data.getAdapter().activateRequest( address )){
    				
    				routing_data = null;
    			}
    		}
    		return routing_data;
    	}
    	
    	public Object 
    	minMatches( 
    		InetSocketAddress	address,
    		ByteBuffer 			to_compare, 
    		int 				port ) 
    	{ 
    		if ( MUTLI_CONTROLLERS ){
    		
    			return( null );
    		}

    		boolean matches = false;

    		int old_limit = to_compare.limit();
    		int old_position = to_compare.position();

    		to_compare.limit( old_position + 20 );

    		if( to_compare.equals( legacy_handshake_header ) ) { 
    			matches = true;
    		}

    		//restore buffer structure
    		to_compare.limit( old_limit );
    		to_compare.position( old_position );

    		return matches?"":null;
    	}

    	public byte[] 
    	getSharedSecret()
    	{
    		return( null );	// registered manually above
    	}
    };
    
    // register for incoming connection routing
    NetworkManager.getSingleton().requestIncomingConnectionRouting(
        matcher,
        new NetworkManager.RoutingListener() 
        {
        	public void 
        	connectionRouted( NetworkConnection connection, Object routing_data ) 
        	{
        		PeerManagerRegistrationImpl	registration = (PeerManagerRegistrationImpl)routing_data;
        		
        		System.out.println( "default routing" );
        		
        		connection.close();
        	}
        	public boolean
      	  	autoCryptoFallback()
        	{
        		return( false );
        	}
        	},
        new MessageStreamFactory() {
          public MessageStreamEncoder createEncoder() {  return new BTMessageEncoder();  }
          public MessageStreamDecoder createDecoder() {  return new BTMessageDecoder();  }
        },
        true );
  }
  
     
  public PeerManagerRegistration
  registerLegacyManager(
	HashWrapper						hash,
	PeerManagerRegistrationAdapter  adapter )
  {
	  try{
		  managers_mon.enter();
		  		
		  if ( registered_legacy_managers.get( hash ) != null ){
			  
			  Debug.out( "manager already registered" );
		  }
		  		  
		  IncomingConnectionManager.getSingleton().addSharedSecret( hash.getBytes());
		  
		  PeerManagerRegistration	registration = new PeerManagerRegistrationImpl( hash, adapter );
		  
		  registered_legacy_managers.put( hash, registration );
		  
		  return( registration );
	  }finally{
		  
		  managers_mon.exit();
	  }
  }
  
  protected void
  unregister(
	PeerManagerRegistrationImpl  registration )
  {
	  try{
		  managers_mon.enter();
		  
		  HashWrapper	hash = registration.getHash();
		  		  
		  IncomingConnectionManager.getSingleton().removeSharedSecret( hash.getBytes());
		  
		  if ( registered_legacy_managers.remove( hash ) == null ){
			  
			  Debug.out( "manager already deregistered" );
		  }
	  }finally{
		  
		  managers_mon.exit();
	  }
  }
  
  /**
   * Register legacy peer manager for incoming BT connections.
   * @param manager legacy controller
   */
  
  protected void 
  activateLegacyManager( 
	final PEPeerControl manager ) 
  {
    NetworkManager.ByteMatcher matcher = new NetworkManager.ByteMatcher() {
      public int size() {  return 48;  }
      public int minSize() { return 20; }
      
      public Object 
      matches( 
    	InetSocketAddress	remote,
    	ByteBuffer 			to_compare, 
    	int 				port ) 
      { 
    	
    	if ( MUTLI_CONTROLLERS ){
    		if ( port != manager.getPort()){
    			return( null);
    		}
    	}
    	  
        boolean matches = false;
        
        int old_limit = to_compare.limit();
        int old_position = to_compare.position();
        
        to_compare.limit( old_position + 20 );
        
        if( to_compare.equals( legacy_handshake_header ) ) {  //compare header 
          to_compare.limit( old_position + 48 );
          to_compare.position( old_position + 28 );
          
          if( to_compare.equals( ByteBuffer.wrap( manager.getHash() ) ) ) {  //compare infohash
            matches = true;
          }
        }
        
        //restore buffer structure
        to_compare.limit( old_limit );
        to_compare.position( old_position );
        
        return matches?"":null;
      }
      
      public Object 
      minMatches( 
    	InetSocketAddress	address,
    	ByteBuffer 			to_compare, 
    	int 				port ) 
      { 
    	  if ( MUTLI_CONTROLLERS ){
        	if ( port != manager.getPort()){
        		return( null);
        	}
    	  }
    	  
          boolean matches = false;
          
          int old_limit = to_compare.limit();
          int old_position = to_compare.position();
          
          to_compare.limit( old_position + 20 );
          
          if( to_compare.equals( legacy_handshake_header ) ) { 
        	  matches = true;
          }
  
          //restore buffer structure
          to_compare.limit( old_limit );
          to_compare.position( old_position );
          
          return matches?"":null;
        }
      
      public byte[] 
      getSharedSecret()
      {
    	  return( null );	// registered manually above
      }
    };
    
    
    // register for incoming connection routing
    NetworkManager.getSingleton().requestIncomingConnectionRouting(
        matcher,
        new NetworkManager.RoutingListener() {
          public void connectionRouted( NetworkConnection connection, Object routing_data ) {
            
            // make sure not already connected to the same IP address; allow
            // loopback connects for co-located proxy-based connections and
            // testing
            String address = connection.getEndpoint().getNotionalAddress().getAddress().getHostAddress();
            boolean same_allowed = COConfigurationManager.getBooleanParameter( "Allow Same IP Peers" ) || address.equals( "127.0.0.1" );
            if( !same_allowed && PeerIdentityManager.containsIPAddress( manager.getPeerIdentityDataID(), address ) ){  
            	if (Logger.isEnabled())
								Logger.log(new LogEvent(LOGID, LogEvent.LT_WARNING,
										"Incoming TCP connection from [" + connection
												+ "] dropped as IP address already "
												+ "connected for ["
												+ manager.getDisplayName() + "]"));
              connection.close();
              return;
            }
            
            if (Logger.isEnabled())
							Logger.log(new LogEvent(LOGID, "Incoming TCP connection from ["
									+ connection + "] routed to legacy download ["
									+ manager.getDisplayName() + "]"));
            manager.addPeerTransport( PEPeerTransportFactory.createTransport( manager, PEPeerSource.PS_INCOMING, connection ) );
          }
          public boolean
      	  autoCryptoFallback()
          {
        	  return( false );
          }
        },
        new MessageStreamFactory() {
          public MessageStreamEncoder createEncoder() {  return new BTMessageEncoder();  }
          public MessageStreamDecoder createDecoder() {  return new BTMessageDecoder();  }
        },
        false );
    
    TorrentDownload download = TorrentDownloadFactory.getSingleton().createDownload( manager );  //link legacy with new
    LegacyRegistration leg_reg = new LegacyRegistration( download, matcher );
    
	  try{
		  managers_mon.enter();

		  active_legacy_managers.put( manager, leg_reg );
		  		  
	  }finally{
		  
		  managers_mon.exit();
	  }
  }
  
  
  
  /**
   * Remove legacy peer manager registration.
   * @param manager legacy controller
   */
  protected void 
  deactivateLegacyManager( 
	final PEPeerControl manager ) 
  {
    //remove incoming routing registration 
    LegacyRegistration leg_reg = (LegacyRegistration)active_legacy_managers.remove( manager );
    if( leg_reg != null ) {
      NetworkManager.getSingleton().cancelIncomingConnectionRouting( leg_reg.byte_matcher );
      leg_reg.download.destroy();  //break legacy link
    }
    else {
      Debug.out( "matcher == null" );
    }
  }
  
  
  
  private static class LegacyRegistration {
    private final TorrentDownload download;
    private final ByteMatcher byte_matcher;
    
    private LegacyRegistration( TorrentDownload d, ByteMatcher m ) {
      this.download = d;
      this.byte_matcher = m;
    }  
  }
  
  private class
  PeerManagerRegistrationImpl
  	implements PeerManagerRegistration
  {
	private HashWrapper 					hash;
	private PeerManagerRegistrationAdapter	adapter;
	
	protected
	PeerManagerRegistrationImpl(
		HashWrapper						_hash,
		PeerManagerRegistrationAdapter	_adapter )
	{
		hash	= _hash;
		adapter	= _adapter;
	}
	
	protected HashWrapper
	getHash()
	{
		return( hash );
	}
	
	protected PeerManagerRegistrationAdapter
	getAdapter()
	{
		return( adapter );
	}
	
	public void
	activate(
		PEPeerControl	peer_control )
	{
		PeerManager.this.activateLegacyManager( peer_control );	
	}
	
	public void
	deactivate(
		PEPeerControl	peer_control )
	{
		PeerManager.this.deactivateLegacyManager( peer_control );	
	}
	
	public void
	unregister()
	{
		PeerManager.this.unregister( this );
	}
  }
}
