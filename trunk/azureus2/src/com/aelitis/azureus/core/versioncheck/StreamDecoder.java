/*
 * Created on Dec 10, 2004
 * Created by Alon Rohter
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 *
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

package com.aelitis.azureus.core.versioncheck;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.gudy.azureus2.core3.util.Debug;


/**
 * Reads and decodes a socket channel byte stream into a message.
 * The stream format must look like:
 * 1) one byte for the handshake key length
 * 2) handshake key bytes
 * 3) four bytes (int) for the payload length
 * 4) payload bytes
 */
public class StreamDecoder {
  private String HANDSHAKE;
  private ByteBuffer handshake_buffer;
  private boolean handshaking = true;
  
  private ByteBuffer payload_length_buffer;
  private boolean reading_payload_length = true;
  private ByteBuffer payload_buffer;
  
  
  
  /**
   * Create a new decoder using the given handshake key.
   * @param handshake_key message key
   */
  public StreamDecoder( String handshake_key ) {
    HANDSHAKE = handshake_key;
    handshake_buffer = ByteBuffer.allocate( 1 + HANDSHAKE.getBytes().length );
  }
  
  
  /**
   * Perform a read operation on the given channel,
   * i.e. decode the stream into a message.
   * @param channel connection to read from
   * @return message payload if reading complete, else null if more reading is required for decoding
   * @throws IOException on channel read exception or stream decode error
   */
  public ByteBuffer decode( SocketChannel channel ) throws IOException {
    if( handshaking ) {
      
      int bytes_read = channel.read( handshake_buffer );
      
      if( bytes_read < 0 ) {
        throw new IOException( "end of stream on socket read" );
      }
      
      if( bytes_read == 0 ) {
        Debug.out( "bytes_read == 0" );
      }
      
      if( !handshake_buffer.hasRemaining() ) {  //process handshake
        handshake_buffer.flip();
        if( handshake_buffer.get() != (byte)HANDSHAKE.length() ) {
          throw new IOException( "decode: invalid handshake length" );
        }
        String payload = new String( handshake_buffer.array(), 1, handshake_buffer.limit() - 1 );
        if( !payload.equals( HANDSHAKE ) ) {
          throw new IOException( "decode: invalid handshake key" );
        }
        //handshake successfull
        HANDSHAKE = null;
        handshake_buffer = null;
        payload_length_buffer = ByteBuffer.allocate( 4 );
        handshaking = false;
      }
    }
    
    if( !handshaking ) {
      if( reading_payload_length ) {
        if( channel.read( payload_length_buffer ) < 0 ) {
          throw new IOException( "end of stream" );
        }
        
        if( !payload_length_buffer.hasRemaining() ) {
          payload_length_buffer.flip();
          int size = payload_length_buffer.getInt();
          if( size < 0 || size > 256*1024 ) {  //256KB payload limit
            throw new IOException( "decode: invalid payload size: " +size );
          }
          payload_buffer = ByteBuffer.allocate( size );
          payload_length_buffer = null;
          reading_payload_length = false;
        }
      }
      
      if( !reading_payload_length ) {
        if( channel.read( payload_buffer ) < 0 ) {
          throw new IOException( "end of stream" );
        }
        
        if( !payload_buffer.hasRemaining() ) {
          payload_buffer.flip();
          return payload_buffer;  //read processing complete
        }
      }
    }

    return null;
  }
  
}
