/*
 * Created on Nov 13, 2004
 * Created by Alon Rohter
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.core.peermanager.utils;

import java.util.*;

import org.gudy.azureus2.core3.util.AEMonitor;

/**
 * Storage class for peer ip address & port connection information.
 */
public class PeerConnectInfoStorage {
  private int capacity;
  private final LinkedList peer_infos = new LinkedList();
  private final AEMonitor peer_infos_mon = new AEMonitor( "PeerConnectInfoStorage:PI" );
  
  /**
   * Create new storage with the given capacity.
   * @param max_capacity max peer infos to store
   */
  public PeerConnectInfoStorage( int max_capacity ) {
    this.capacity = max_capacity;
  }
  
  /**
   * Set the max storage capacity.
   * @param new_max max peer infos to store
   */
  public void setMaxCapacity( int new_max ) {
    capacity = new_max;
  }
  
  /**
   * Add a new peer info to the storage.
   * Info is only added if the address+port combo is unique.
   * If already at capacity, replace oldest.
   * @param peer_info to add
   */
  public void addPeerInfo( PeerInfo peer_info ) {
    try {  peer_infos_mon.enter();
      if( capacity > 0 && !peer_infos.contains( peer_info ) ) {
        if( peer_infos.size() < capacity ) {
          peer_infos.addLast( peer_info );
        }
        else {  //at capacity, so replace oldest
          peer_infos.removeFirst();
          peer_infos.addLast( peer_info );
        }
      }
    }
    finally {  peer_infos_mon.exit();  }
  }
  
  
  /**
   * Get the next peer info from storage.
   * @return peer info if any remaining in storage, null if none remaining
   */
  public PeerInfo getPeerInfo() {
    try {  peer_infos_mon.enter();
      if( !peer_infos.isEmpty() ) {
        return (PeerInfo)peer_infos.removeFirst();
      }
      return null;
    }
    finally {  peer_infos_mon.exit();  }
  }
  
  
  /**
   * Get the number of infos currently stored.
   * @return number in storage
   */
  public int getStoredCount() {
    try {  peer_infos_mon.enter();
      return peer_infos.size();
    }
    finally {  peer_infos_mon.exit();  }
  }
  
  
  /**
   * Holds peer connection info.
   */
  public static class PeerInfo {
    private final String address;
    private final int port;
    private final int hashcode;
    
    /**
     * Create new info with given ip address and port.
     * @param address ip
     * @param port remote port
     */
    public PeerInfo( String address, int port ) {
      this.address = address;
      this.port = port;
      this.hashcode = address.hashCode() + port;
    }
    
    /**
     * Get the peer ip address.
     * @return address
     */
    public String getAddress() {  return address;  }
    
    /**
     * Get the peer port.
     * @return port
     */
    public int getPort() {  return port;  }
    

    public boolean equals( Object obj ) {
      if( this == obj )  return true;
      if( obj != null && obj instanceof PeerInfo ) {
        PeerInfo other = (PeerInfo)obj;
        if( this.port == other.port && this.address.equals( other.address ) ) {
          return true;
        }
      }
      return false;
    }
    
    public int hashCode() {  return hashcode;  }
  }
  
}
