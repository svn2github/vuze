/*
 * Created on 21-Jul-2004
 * Created by Paul Gardner
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

package org.gudy.azureus2.plugins.peers;

import org.gudy.azureus2.core3.peer.PEPeer;

import com.aelitis.azureus.core.peermanager.piecepicker.util.BitFlags;

/**
 * Listener for peer events.
 */
public interface PeerListener {
  
  /**
   * The peer has changed to the given state.
   * @param new_state of peer
   */
  public void stateChanged( int new_state );
  
  /**
   * The peer has sent us a bad piece data chunk.
   * @param piece_num piece that failed hash check
   * @param total_bad_chunks total number of bad chunks sent by this peer so far
   */
  public void sentBadChunk( int piece_num, int total_bad_chunks );
  
  /** The peer asserts that their availability should be added to the torrent-global availability pool.
   * The peer must send this when, and only when, their availability is known (such as after
   * receiving a bitfield message) but not after going to CLOSING state.  After having sent this
   * message, the peer must remember they've done so and later send a corresponding removeAvailability
   * message at an appropriate time.
   * @param peerHavePieces boolean[] of pieces availabile
   */
  public void addAvailability(final boolean[] peerHavePieces);

  /** The peer asserts that their availability must now be taken from the torrent-global availability pool
   * The peer must send this only after having sent a corresponding addAvailability message,
   * and must not send it in a state prior to CLOSING state.  The BitFlags must be complete, with all
   * pieces from any Bitfield message as well as those from any Have messages.
   * @param peerHavePieces boolean[] of pieces no longer available
   */
  public void removeAvailability(final boolean[] peerHavePieces);


}
