/*
 * Created on Sep 9, 2004
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

/**
 * Handles peer client identification and banning.
 */
public class PeerClassifier {
  
  /**
   * Get a client description (name and version) from the given peerID byte array. 
   * @param peer_id peerID sent in handshake
   * @return description
   */
  public static String getClientDescription( byte[] peer_id ) {
    return PeerIDByteDecoder.decode( peer_id );
  }
  
  
  /**
   * Get a printable representation of the given raw peerID byte array,
   * i.e. filter out the first 32 non-printing ascii chars.
   * @param peer_id peerID sent in handshake
   * @return printable peerID
   */
  public static String getPrintablePeerID( byte[] peer_id ) {
    return PeerIDByteDecoder.getPrintablePeerID( peer_id, 0 );
  }
  

  /**
   * Check if the client type is allowed to connect.
   * @param client_description given by getClientDescription
   * @return true if allowed, false if banned
   */
  public static boolean isClientTypeAllowed( String client_description ) {
    if( client_description.startsWith( "BitComet" ) ) return false;
    return true;
  }
  

}
