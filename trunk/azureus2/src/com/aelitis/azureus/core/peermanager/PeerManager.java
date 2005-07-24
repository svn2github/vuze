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
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.peer.PEPeerSource;
import org.gudy.azureus2.core3.peer.impl.*;
import org.gudy.azureus2.core3.peer.util.PeerIdentityManager;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.networkmanager.*;
import com.aelitis.azureus.core.networkmanager.NetworkManager.ByteMatcher;
import com.aelitis.azureus.core.peermanager.download.TorrentDownload;
import com.aelitis.azureus.core.peermanager.download.TorrentDownloadFactory;
import com.aelitis.azureus.core.peermanager.messaging.*;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.*;

/**
 *
 */
public class PeerManager {

  private static final PeerManager instance = new PeerManager();

  private final HashMap legacy_managers = new HashMap();
  
  private final ByteBuffer legacy_handshake_header;
  
  

  private PeerManager() {
    legacy_handshake_header = ByteBuffer.allocate( 20 );
    legacy_handshake_header.put( (byte)BTHandshake.PROTOCOL.length() );
    legacy_handshake_header.put( BTHandshake.PROTOCOL.getBytes() );
    legacy_handshake_header.flip();
    
    MessageManager.getSingleton().initialize();  //ensure it gets initialized
  }
  
  
  
  /**
   * Get the singleton instance of the peer manager.
   * @return the peer manager
   */
  public static PeerManager getSingleton() {  return instance;  }
  
    
  
  
  /**
   * Register legacy peer manager for incoming BT connections.
   * @param manager legacy controller
   */
  public void registerLegacyManager( final PEPeerControl manager ) {
    NetworkManager.ByteMatcher matcher = new NetworkManager.ByteMatcher() {
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
    NetworkManager.getSingleton().requestIncomingConnectionRouting(
        matcher,
        new NetworkManager.RoutingListener() {
          public void connectionRouted( NetworkConnection connection ) {
            
            //make sure not already connected to the same IP address; allow loopback connects for co-located proxy-based connections and testing
            String address = connection.getAddress().getAddress().getHostAddress();
            boolean same_allowed = COConfigurationManager.getBooleanParameter( "Allow Same IP Peers" ) || address.equals( "127.0.0.1" );
            if( !same_allowed && PeerIdentityManager.containsIPAddress( manager.getPeerIdentityDataID(), address ) ){  
              if( LGLogger.isEnabled() )  LGLogger.log( "Incoming TCP connection from [" +connection+ "] dropped as IP address already connected for [" +manager.getDownloadManager().getDisplayName()+ "]" );
              connection.close();
              return;
            }
            
            if( LGLogger.isEnabled() )  LGLogger.log( "Incoming TCP connection from [" +connection+ "] routed to legacy download [" +manager.getDownloadManager().getDisplayName()+ "]" );
            manager.addPeerTransport( PEPeerTransportFactory.createTransport( manager, PEPeerSource.PS_INCOMING, connection ) );
          }
        },
        new MessageStreamFactory() {
          public MessageStreamEncoder createEncoder() {  return new BTMessageEncoder();  }
          public MessageStreamDecoder createDecoder() {  return new BTMessageDecoder();  }
        }
    );
    
    TorrentDownload download = TorrentDownloadFactory.getSingleton().createDownload( manager );  //link legacy with new
    LegacyRegistration leg_reg = new LegacyRegistration( download, matcher );
    
    legacy_managers.put( manager, leg_reg );
  }
  
  
  
  /**
   * Remove legacy peer manager registration.
   * @param manager legacy controller
   */
  public void deregisterLegacyManager( final PEPeerControl manager ) {
    //remove incoming routing registration 
    LegacyRegistration leg_reg = (LegacyRegistration)legacy_managers.remove( manager );
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

}
