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
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.peermanager;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.*;

import org.gudy.azureus2.core3.peer.impl.*;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.networkmanager.IncomingConnectionManager;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTHandshake;

/**
 *
 */
public class PeerManager {

  private static final PeerManager instance = new PeerManager();
  
  private final PeerUploadManager upload_manager = new PeerUploadManager();
  
  private final HashMap legacy_managers = new HashMap();
  
  private final ByteBuffer legacy_handshake_header;
  
  
  
  
  private PeerManager() {
    legacy_handshake_header = ByteBuffer.allocate( 20 );
    legacy_handshake_header.put( (byte)BTHandshake.PROTOCOL.length() );
    legacy_handshake_header.put( BTHandshake.PROTOCOL.getBytes() );
    legacy_handshake_header.flip();
  }
  
  
  
  /**
   * Get the singleton instance of the peer manager.
   * @return the peer manager
   */
  public static PeerManager getSingleton() {  return instance;  }
  
  
  /**
   * Get the peer upload manager.
   * @return upload manager
   */
  public PeerUploadManager getUploadManager() {  return upload_manager;  }
  
  
  /**
   * Register legacy peer manager.
   * @param manager legacy controller
   */
  public void registerLegacyPeerManager( final PEPeerControl manager ) {
    IncomingConnectionManager.ByteMatcher matcher = new IncomingConnectionManager.ByteMatcher() {
      public int size() {  return 48;  }

      public boolean matches( ByteBuffer to_compare ) { 
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
        
        return matches;
      }
    };
    
    //register for incoming connection routing   
    NetworkManager.getSingleton().getIncomingConnectionManager().registerMatchBytes( 
        matcher,
        new IncomingConnectionManager.MatchListener() {
          public void connectionMatched( SocketChannel channel, ByteBuffer read_so_far ) {
            System.out.println( "Incoming TCP connection from [" +channel.socket().getInetAddress().getHostAddress()+ ":" +channel.socket().getPort()+ "] successfully routed to [" +manager.getDownloadManager().getDisplayName()+ "]" );
            PEPeerTransport transport = PEPeerTransportFactory.createTransport( manager, channel, read_so_far );
            manager.addPeerTransport( transport );
          }
        }
    );
    
    legacy_managers.put( manager, matcher );
  }
  
  
  /**
   * Remove legacy peer manager registration.
   * @param manager legacy controller
   */
  public void deregisterLegacyPeerManager( final PEPeerControl manager ) {
    //remove incoming routing registration 
    IncomingConnectionManager.ByteMatcher matcher = (IncomingConnectionManager.ByteMatcher)legacy_managers.get( manager );
    if( matcher != null ) {
      NetworkManager.getSingleton().getIncomingConnectionManager().deregisterMatchBytes( matcher );
    }
    else {
      Debug.out( "matcher == null" );
    }
  }
  

}
