/*
 * Created on Apr 30, 2004
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

package org.gudy.azureus2.core3.peer.impl.transport.protocol;

import org.gudy.azureus2.core3.util.DirectByteBuffer;

/**
 * A BitTorrent protocol message.
 */
public interface BTMessage {
  
  public static final int BT_HANDSHAKE    = -1;
  public static final int BT_CHOKE        = 0;
  public static final int BT_UNCHOKE      = 1;
  public static final int BT_INTERESTED   = 2;
  public static final int BT_UNINTERESTED = 3;
  public static final int BT_HAVE         = 4;
  public static final int BT_BITFIELD     = 5;
  public static final int BT_REQUEST      = 6;
  public static final int BT_PIECE        = 7;
  public static final int BT_CANCEL       = 8;
  public static final int BT_KEEP_ALIVE   = 9;
  
  /**
   * Get the message's type.
   * @return type
   */
  public int getType();
  
  /**
   * Get the message's data payload.
   * @return data payload
   */
  public DirectByteBuffer getPayload();
  
  /**
   * Get the message's textual description.
   * @return description
   */
  public String getDescription();
  
}
