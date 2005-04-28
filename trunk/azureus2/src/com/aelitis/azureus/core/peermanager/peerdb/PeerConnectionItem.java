/*
 * Created on Apr 27, 2005
 * Created by Alon Rohter
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.core.peermanager.peerdb;


import java.util.*;

import org.gudy.azureus2.core3.util.AEMonitor;

/**
 *
 */
public class PeerConnectionItem {
  private final PeerDatabase parent_db;
  private final PeerItem base_peer;
  private final ArrayList connections_added = new ArrayList();
  private final ArrayList connections_dropped = new ArrayList();
  private final HashMap connected_peers = new HashMap();
  private final AEMonitor peers_mon = new AEMonitor( "PeerConnectionItem" );
  private boolean maintain_peers_state = true;  //assume we do until explicitly disabled
  

  protected PeerConnectionItem( PeerDatabase parent_db, PeerItem peer ) {
    this.parent_db = parent_db;
    this.base_peer = peer;
  }
  

  /**
   * Add peer info obtained via peer exchange.
   * @param peer to add
   */
  public void addConnectedPeer( PeerItem peer ) {
    try{  peers_mon.enter();
      if( !maintain_peers_state )  return;
    
      connected_peers.put( peer, peer );
      
//    TODO: System.out.println( "added exchanged peer: " +new String(peer.getAddress())+":" +peer.getPort() );
      
      if( connected_peers.size() > 1000 ) {
        System.out.println( "connected_peers.size():" + connected_peers.size());
        //TODO we'll need to limit the max size somehow
      }
      
    }
    finally{  peers_mon.exit();  }
  }
  
  
  /**
   * Remove peer info obtained via peer exchange.
   * @param peer to remove
   */
  public void dropConnectedPeer( PeerItem peer ) {
    try{  peers_mon.enter();
    
//  TODO: System.out.println( "dropped exchanged peer: " +new String(peer.getAddress())+":" +peer.getPort() );
      connected_peers.remove( peer );
    }
    finally{  peers_mon.exit();  }
  }
  
 
  
  
  protected void notifyAdded( PeerItem peer_connection ) {
    try{  peers_mon.enter();
      if( !maintain_peers_state )  return;
    
      if( !connections_dropped.contains( peer_connection ) ) {
        if( !connections_added.contains( peer_connection ) ) {
          connections_added.add( peer_connection );  //register new add
        }
      }
      else {  //was dropped and then re-added
        connections_dropped.remove( peer_connection );  //pull drop and ignore add
      }
    }
    finally{  peers_mon.exit();  }
  }
  
  
  protected void notifyDropped( PeerItem peer_connection ) {
    try{  peers_mon.enter();
      if( !maintain_peers_state )  return;
      
      if( !connections_added.contains( peer_connection ) ) {
        if( !connections_dropped.contains( peer_connection ) ) {
          connections_dropped.add( peer_connection );  //register new drop
        }
      }
      else {  //was added and then re-dropped
        connections_added.remove( peer_connection );  //pull add and ignore drop
      }
    }
    finally{  peers_mon.exit();  }
  }
  
  

  /**
   * Get the list of peer connections added since this method was last called.
   * @return new peer connections
   */
  public PeerItem[] getNewlyAddedPeerConnections() {
    try{  peers_mon.enter();
      if( connections_added.isEmpty() )  return null;
      PeerItem[] peers = new PeerItem[ connections_added.size() ];
      connections_added.toArray( peers );
      connections_added.clear();  //reset adds
      return peers;
    }
    finally{  peers_mon.exit();  }
  }
  

  
  /**
   * Get the list of peer connections dropped since this method was last called.
   * @return dropped peer connections
   */
  public PeerItem[] getNewlyDroppedPeerConnections() {
    try{  peers_mon.enter();
      if( connections_dropped.isEmpty() )  return null;
      PeerItem[] peers = new PeerItem[ connections_dropped.size() ];
      connections_dropped.toArray( peers );
      connections_dropped.clear();  //reset drops
      return peers;
    }
    finally{  peers_mon.exit();  }
  }
  

  /**
   * Clears all current peer state records and stops any future state maintenance.
   */
  public void disableStateMaintenance() {
    try{  peers_mon.enter();
      maintain_peers_state = false;
      connections_added.clear();
      connections_dropped.clear();
      connected_peers.clear();
    }
    finally{  peers_mon.exit();  }
  }
  
  
  
  protected boolean isConnectedToPeer( PeerItem peer ) {
    try{  peers_mon.enter();
    
      return connected_peers.containsKey( peer );
    }
    finally{  peers_mon.exit();  }
  }
  
  
  protected PeerItem[] getConnectedPeers() {
    try{  peers_mon.enter();
    
      PeerItem[] peers = new PeerItem[ connected_peers.size() ];
      connected_peers.keySet().toArray( peers );
      return peers;
    }
    finally{  peers_mon.exit();  }
  }
  
  
  
  
  public void destroy() {
    parent_db.deregisterPeerConnection( base_peer );
    
    try{  peers_mon.enter();
      connections_added.clear();
      connections_dropped.clear();
      connected_peers.clear();
    }
    finally{  peers_mon.exit();  }
  }
  
}

