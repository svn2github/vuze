/*
 * Created on Jul 17, 2004
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

package com.aelitis.azureus.core.peermanager.messaging.bittorrent;

import com.aelitis.azureus.core.peermanager.messaging.Message;

/**
 * A bittorrent peer protocol message.
 */
public interface BTMessage extends Message {
  
  public static final String ID_BT_HANDSHAKE    = "BT_HANDSHAKE";
  public static final String ID_BT_CHOKE        = "BT_CHOKE";
  public static final String ID_BT_UNCHOKE      = "BT_UNCHOKE";
  public static final String ID_BT_INTERESTED   = "BT_INTERESTED";
  public static final String ID_BT_UNINTERESTED = "BT_UNINTERESTED";
  public static final String ID_BT_HAVE         = "BT_HAVE";
  public static final String ID_BT_BITFIELD     = "BT_BITFIELD";
  public static final String ID_BT_REQUEST      = "BT_REQUEST";
  public static final String ID_BT_PIECE        = "BT_PIECE";
  public static final String ID_BT_CANCEL       = "BT_CANCEL";
  public static final String ID_BT_KEEP_ALIVE   = "BT_KEEP_ALIVE";
  
  public static final byte BT_DEFAULT_VERSION = (byte)1;

}
