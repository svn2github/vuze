/*
 * Created on Feb 25, 2005
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

package org.gudy.azureus2.core3.peer;

/**
 * Listener for peer events.
 */
public interface PEPeerListener {
  
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

}
