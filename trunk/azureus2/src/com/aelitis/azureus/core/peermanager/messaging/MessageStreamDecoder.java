/*
 * Created on Jan 25, 2005
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

package com.aelitis.azureus.core.peermanager.messaging;

import java.io.IOException;

import com.aelitis.azureus.core.networkmanager.Transport;

/**
 * Decodes a message stream.
 */
public interface MessageStreamDecoder {
  /**
   * Decode message stream from the given transport.
   * @param transport to decode from
   * @param max_bytes to decode/read from the stream
   * @return number of bytes decoded
   * @throws IOException on decoding error
   */
  public int decodeStream( Transport transport, int max_bytes ) throws IOException;
  
  
  
  
  /**
   * For receiving notification of decode events.
   */
  public interface DecodeListener {
    /**
     * The given message has been read from the transport.
     * @param message received
     */
    public void messageDecoded( Message message );
    
    /**
     * The given number of protocol (overhead) bytes has been read from the transport.
     * @param byte_count number of protocol bytes received
     */
    public void protocolBytesDecoded( int byte_count );
    
    /**
     * The given number of (piece) data bytes has been read from the transport.
     * @param byte_count number of data bytes received
     */
    public void dataBytesDecoded( int byte_count );
  }
  
}
