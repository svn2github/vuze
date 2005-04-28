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

import java.net.InetAddress;

/**
 *
 */
public class PeerItemFactory {
  public static final int PEER_SOURCE_TRACKER       = 0;
  public static final int PEER_SOURCE_DHT           = 1;
  public static final int PEER_SOURCE_PEER_EXCHANGE = 2;
  public static final int PEER_SOURCE_PLUGIN        = 3;
  public static final int PEER_SOURCE_INCOMING      = 4;
  
  
  /**
   * Create a peer item using the given peer ip and port and source information.
   * NOTE: For creating IPv4 and IPv6 peers.
   * @param address ip of peer
   * @param port of peer
   * @param source this peer info was obtained from
   * @return peer
   */
  public static PeerItem createPeerItem( InetAddress address, int port, int source ) {
    return createPeerItem( address.getAddress(), port, source );
  }

  
  
  /**
   * Create a peer item using the given peer raw byte address and port and source information.
   * NOTE: For creating non-IPv4/6 peers, i.e. I2P/Tor/non-dns-resolvable addresses.
   * @param raw_address of peer
   * @param port of peer
   * @param source this peer info was obtained from
   * @return peer
   */
  public static PeerItem createPeerItem( byte[] raw_address, int port, int source ) {
    //TODO use this to create "lightweight" re-used peer items, since many exchanged peer connections will be duplicates, using weak hashmap?
    return new PeerItem( raw_address, port, source );
  }
  
}
