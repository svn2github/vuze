/*
 * Created on Jul 10, 2005
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

package com.aelitis.azureus.core.peermanager.download.session;

import java.util.Map;


public interface TorrentSessionAuthenticator {
  
  public static final String AUTH_TYPE_STANDARD = "STANDARD";
  public static final String AUTH_TYPE_SECURE   = "SECURE";
  
  
  /**
   * Get the type id that this authenticator handles.
   * @return type id
   */
  public String getSessionTypeID();
  
  /**
   * Get the torrent infohash associated with this session.
   * @return infohash
   */
  public byte[] getSessionInfoHash();
  
  /**
   * Create bencode-able map info for outgoing session syn.
   * @return syn info
   */
  public Map createSessionSyn();
  
  /**
   * Decode and verify the given (bencoded) map of incoming session SYN information,
   * and create the session ACK reply.
   * @param syn_info incoming session syn info
   * @return bencode-able map info for session ack reply
   * @throws AuthenticatorException on error / failure
   */
  public Map verifySessionSyn( Map syn_info ) throws AuthenticatorException;

  /**
   * Decode and verify the given (bencoded) map of outgoing session ACK information.
   * @param ack_info incoming session ack info
   * @throws AuthenticatorException on error / failure
   */
  public void verifySessionAck( Map ack_info ) throws AuthenticatorException;
  
}
