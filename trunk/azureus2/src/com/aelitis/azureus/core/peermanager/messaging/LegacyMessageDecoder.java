/*
 * Created on Jan 24, 2005
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
import java.nio.ByteBuffer;
import java.util.*;

import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.networkmanager.TCPTransport;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.*;

/**
 *
 */
public class LegacyMessageDecoder implements MessageStreamDecoder {
  
  private static final int HANDSHAKE_FAKE_LENGTH = 323119476;  //(byte)19 + "Bit" readInt() value of header
      
  private final ByteBuffer type_buffer = ByteBuffer.allocate( 1 );
  private ByteBuffer payload_buffer = null;
  private DirectByteBuffer direct_payload_buffer = null;
  private final ByteBuffer length_buffer = ByteBuffer.allocate( 4 );
  
  private final ByteBuffer[] decode_array = new ByteBuffer[] { type_buffer, payload_buffer, length_buffer };
  
  private final ArrayList messages = new ArrayList();
  
  private boolean reading_length_mode = true;
  private boolean reading_handshake_message = false;
  private boolean reading_data_message = false;
  
  private int message_length;
  private int pre_read_start_buffer;
  private int pre_read_start_position;
  
  private final MessageStreamDecoder.DecodeListener decode_listener;
  

  
  
  
  protected LegacyMessageDecoder( MessageStreamDecoder.DecodeListener listener ) {
    this.decode_listener = listener;
  }
                                    
  
  
  public int decodeStream( TCPTransport transport, int max_bytes ) throws IOException {
    int bytes_remaining = max_bytes;
    
    while( bytes_remaining > 0 ) {
      int bytes_possible = preReadProcess( bytes_remaining );
      
      if( bytes_possible < 1 ) {
        System.out.println( "ERROR: bytes_possible < 1" );
        break;
      }
      
      if( reading_length_mode ) {
        transport.read( decode_array, 2, 1 );  //only read into length buffer
      }
      else {
        transport.read( decode_array, 0, 3 );  //read type and payload buffers, and possibly next message length
      }
      
      int bytes_read = postReadProcess();
      
      bytes_remaining -= bytes_read;
      
      if( bytes_read < bytes_possible ) {
        break;
      }
    }
        
    if( !messages.isEmpty() ) {
      for( int i=0; i < messages.size(); i++ ) {
        Message msg = (Message)messages.get( i );
        
        decode_listener.messageDecoded( msg );
      }
      messages.clear();
    }
    
    return max_bytes - bytes_remaining;
  }
  
  
  
  public void destroy() {
    payload_buffer = null;
    
    if( direct_payload_buffer != null ) {
      direct_payload_buffer.returnToPool();
      direct_payload_buffer = null;
    }
 
    messages.clear();
  }
  
  
  
  
  
  private int preReadProcess( int allowed ) {
    if( allowed < 1 ) {
      System.out.println( "allowed < 1" );
    }
    
    decode_array[ 1 ] = payload_buffer;  //ensure the decode array has the latest payload pointer
    
    int bytes_available = 0;
    boolean shrink_remaining_buffers = false;
    int start_buff = reading_length_mode ? 2 : 0;
    boolean marked = false;    
    
    for( int i = start_buff; i < 3; i++ ) {  //set buffer limits according to bytes allowed
      ByteBuffer bb = decode_array[ i ];
      
      if( bb == null ) {
        System.out.println( "preReadProcess:: bb["+i+"] == null, start_buff=" +start_buff+ ", type=" +decode_array[0].get(0) );
      }
      
      
      if( shrink_remaining_buffers ) {
        bb.limit( 0 );  //ensure no read into this next buffer is possible
      }
      else {
        int remaining = bb.remaining();
        
        if( remaining < 1 )  continue;  //skip full buffer

        if( !marked ) {
          pre_read_start_buffer = i;
          pre_read_start_position = bb.position();
          marked = true;
        }

        if( remaining > allowed ) {  //read only part of this buffer
          bb.limit( bb.position() + allowed );  //limit current buffer
          bytes_available += bb.remaining();
          shrink_remaining_buffers = true;  //shrink any tail buffers
        }
        else {  //full buffer is allowed to be read
          bytes_available += remaining;
          allowed -= remaining;  //count this buffer toward allowed and move on to the next
        }
      }
    }
    
    return bytes_available;
  }
  
  

  
  private int postReadProcess() throws IOException {
    int bytes_read = 0;
    int data_bytes_read = 0;
    int protocol_bytes_read = 0;
    
    if( !reading_length_mode ) {  //reading payload data mode
      //ensure-restore proper buffer limits
      type_buffer.limit( 1 );
      payload_buffer.limit( message_length - 1 );
      length_buffer.limit( 4 );
      
      int read = 0;
      if( pre_read_start_buffer == 0 ) {  //started at type buffer, happens at least once each message        
        if( !type_buffer.hasRemaining() ) {  //check type buffer
          type_buffer.position( 0 );
          byte id = type_buffer.get();
          
          try {
            reading_data_message = reading_handshake_message ? false : MessageFactory.determineLegacyMessageType( id ) == Message.TYPE_DATA_PAYLOAD;
          }
          catch( MessageException me ) {
            Debug.out( me );
            throw new IOException( "message decode failed: " + me.getMessage() );
          }
            
          read = 1 + payload_buffer.position();
        }
      }
      else {  //starting at payload buffer
        read = payload_buffer.position() - pre_read_start_position;
      }
      
      bytes_read += read;
      
      if( reading_data_message ) {
        data_bytes_read += read;
      }
      else {
        protocol_bytes_read += read;
      }

      if( !payload_buffer.hasRemaining() ) {  //full message received!
        type_buffer.position( 0 );  //prepare for use
        payload_buffer.position( 0 );
        
        if( reading_handshake_message ) {  //decode handshake
          reading_handshake_message = false;
          
          ByteBuffer handshake_data = ByteBuffer.allocate( 68 );
          handshake_data.putInt( HANDSHAKE_FAKE_LENGTH );
          handshake_data.put( type_buffer );
          handshake_data.put( payload_buffer );
          handshake_data.flip();
          
          try {
            Message handshake = MessageFactory.createMessage( BTMessage.ID_BT_HANDSHAKE, BTMessage.BT_DEFAULT_VERSION, new DirectByteBuffer( handshake_data ) );
            messages.add( handshake );
          }
          catch( MessageException me ) {
            Debug.out( me );
            throw new IOException( "message decode failed: " + me.getMessage() );
          }
        }
        else {  //decode normal message
          byte legacy_id = type_buffer.get();
          DirectByteBuffer payload = direct_payload_buffer == null ? new DirectByteBuffer( payload_buffer ) : direct_payload_buffer;  
          
          try {
            Message msg = MessageFactory.createLegacyMessage( legacy_id, payload );
            messages.add( msg );
          }
          catch( MessageException me ) {
            Debug.out( me );
            throw new IOException( "message decode failed: " + me.getMessage() );
          }
        }
     
        payload_buffer = null;
        direct_payload_buffer = null;
        reading_length_mode = true;  //see if we've already read the next message's length
      }
    }
    
    
    if( reading_length_mode ) {
      length_buffer.limit( 4 );  //ensure proper buffer limit
      
      int read = (pre_read_start_buffer == 2) ? length_buffer.position() - pre_read_start_position : length_buffer.position();
      bytes_read += read;
      
      if( reading_data_message ) {
        data_bytes_read += read;
      }
      else {
        protocol_bytes_read += read;
      }
      
      if( !length_buffer.hasRemaining() ) {  //done reading the length
        reading_length_mode = false;
        
        length_buffer.position( 0 );
        message_length = length_buffer.getInt();
        
        length_buffer.position( 0 );  //reset it for next length read
        type_buffer.position( 0 );  //reset it
        

        if( message_length == HANDSHAKE_FAKE_LENGTH ) {  //handshake message
          reading_handshake_message = true;
          payload_buffer = ByteBuffer.allocate( 68 - 5 );  //we've already read 4 bytes, plus 1 byte preceding type buffer
          message_length = 68 - 4;  //restore 'real' length
        }
        else if( message_length < 0 || message_length > 16393 ) {  //should never be > 16KB+9B, as we never request chunks > 16KB
          String msg = "Invalid message length given for legacy message decode: " + message_length;
          Debug.out( msg );
          throw new IOException( msg );
        }
        else if( message_length == 0 ) {  //keep-alive message
          reading_length_mode = true;
          try{
            Message keep_alive = MessageFactory.createMessage( BTMessage.ID_BT_KEEP_ALIVE, BTMessage.BT_DEFAULT_VERSION, null );
            messages.add( keep_alive );
          }
          catch( MessageException me ) {
            Debug.out( me );
            throw new IOException( "message decode failed: " + me.getMessage() );
          }
        }
        else {  //normal message
          if( message_length > 1024 ) {
            direct_payload_buffer = DirectByteBufferPool.getBuffer( DirectByteBuffer.SS_NET, message_length - 1 );
            payload_buffer = direct_payload_buffer.getBuffer( DirectByteBuffer.SS_NET );
          }
          else {
            payload_buffer = ByteBuffer.allocate( message_length - 1 );
          }
        }
      }
    }
    
    if( data_bytes_read > 0 )  decode_listener.dataBytesDecoded( data_bytes_read );
    if( protocol_bytes_read > 0 )  decode_listener.protocolBytesDecoded( protocol_bytes_read );
    
    return bytes_read;
  }
  
  
  

}
