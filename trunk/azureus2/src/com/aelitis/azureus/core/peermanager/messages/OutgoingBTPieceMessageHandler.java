/*
 * Created on Jul 19, 2004
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

package com.aelitis.azureus.core.peermanager.messages;

import java.util.*;

import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.diskmanager.ReadRequestListener;
import com.aelitis.azureus.core.networkmanager.OutgoingMessageQueue;
import com.aelitis.azureus.core.peermanager.messages.bittorrent.*;


/**
 * Front-end manager for handling requested outgoing bittorrent Piece messages.
 * Peers often make piece requests in batch, with multiple requests always
 * outstanding, all of which won't necessarily be honored (i.e. we choke them),
 * so we don't want to waste time reading in the piece data from disk ahead
 * of time for all the requests. Thus, we only want to perform read-aheads for a
 * small subset of the requested data at any given time, which is what this handler
 * does, before passing the messages onto the outgoing message queue for transmission.
 */
public class OutgoingBTPieceMessageHandler {
  private static final int MIN_READ_AHEAD = 4;
  private final OutgoingMessageQueue outgoing_message_queue;
  private final DiskManager disk_manager;
  private final LinkedList requests = new LinkedList();
  private final List loading_messages = new ArrayList();
  private final Map queued_messages = new HashMap();
  private int num_messages_loading = 0;
  private int num_messages_in_queue = 0;
  
  private final ReadRequestListener read_req_listener = new ReadRequestListener() {
    public void readCompleted( DiskManagerRequest request, DirectByteBuffer data ) {
      synchronized( loading_messages ) {
        if( !loading_messages.contains( request ) ) { //was canceled
          data.returnToPool();
          return;
        }
        loading_messages.remove( request );
      }
      num_messages_loading--;
      BTPiece msg = new BTPiece( request.getPieceNumber(), request.getOffset(), data );
      synchronized( queued_messages ) {
        queued_messages.put( msg, request );
      }
      outgoing_message_queue.addMessage( msg );
      num_messages_in_queue++;
    }
  };
  
  private final OutgoingMessageQueue.SentMessageListener sent_message_listener = new OutgoingMessageQueue.SentMessageListener() {
    public void messageSent( ProtocolMessage message ) {
      if( message.getType() == BTProtocolMessage.BT_PIECE ) {
        synchronized( queued_messages ) {
          queued_messages.remove( message );
        }
        num_messages_in_queue--;
        doReadAheadLoads();
      }
    }
  };
  
  
  /**
   * Create a new handler for outbound piece messages,
   * reading piece data from the given disk manager
   * and transmitting the messages out the given message queue.
   * @param disk_manager
   * @param outgoing_message_q
   */
  public OutgoingBTPieceMessageHandler( DiskManager disk_manager, OutgoingMessageQueue outgoing_message_q ) {
    this.disk_manager = disk_manager;
    this.outgoing_message_queue = outgoing_message_q;
    outgoing_message_queue.registerSentListener( sent_message_listener );
  }
  
  
  /**
   * Register a new piece data request.
   * @param piece_number
   * @param piece_offset
   * @param length
   */
  public void addPieceRequest( int piece_number, int piece_offset, int length ) {
    DiskManagerRequest dmr = disk_manager.createRequest( piece_number, piece_offset, length );
    synchronized( requests ) {
      requests.addLast( dmr );
    }
    doReadAheadLoads();
  }
  
  
  /**
   * Remove an outstanding piece data request.
   * @param piece_number
   * @param piece_offset
   * @param length
   */
  public void removePieceRequest( int piece_number, int piece_offset, int length ) {
    boolean removed = false;
    DiskManagerRequest dmr = disk_manager.createRequest( piece_number, piece_offset, length );
    synchronized( requests ) {
      if( requests.contains( dmr ) ) {
        requests.remove( dmr );
        removed = true;
      }
    }
    if( removed ) return;
    synchronized( loading_messages ) {
      if( loading_messages.contains( dmr ) ) {
        loading_messages.remove( dmr );
        num_messages_loading--;
        removed = true;
      }
    }
    if( removed ) return;
    
    Object[] entries;
    synchronized( queued_messages ) {
      entries = queued_messages.entrySet().toArray();
    }
    for( int i=0; i < entries.length; i++ ) {
      Map.Entry entry = (Map.Entry)entries[ i ];
      if( entry.getValue().equals( dmr ) ) {
        BTPiece msg = (BTPiece)entry.getKey();
        if( outgoing_message_queue.removeMessage( msg ) ) {
          synchronized( queued_messages ) {
            queued_messages.remove( msg );
          }
          num_messages_in_queue--;
        }
        break;
      }
    }
  }
  
  
  /**
   * Remove all outstanding piece data requests.
   */
  public void removeAllPieceRequests() {
    synchronized( requests ) {
      requests.clear();
    }
    synchronized( loading_messages ) {
      loading_messages.clear();
      num_messages_loading = 0;
    }

    Object[] messages;
    synchronized( queued_messages ) {
      messages = queued_messages.keySet().toArray();
    }
    for( int i=0; i < messages.length; i++ ) {
      BTPiece msg = (BTPiece)messages[ i ];
      if( outgoing_message_queue.removeMessage( msg ) ) { //needs to be outside a synchronized(queued_messages) block due to deadlock
        synchronized( queued_messages ) {
          queued_messages.remove( msg );
        }
        num_messages_in_queue--;
      }
    }
  }
  
  
  private void doReadAheadLoads() {
    synchronized( requests ) {
      while( num_messages_loading + num_messages_in_queue < MIN_READ_AHEAD && !requests.isEmpty() ) {
        DiskManagerRequest dmr = (DiskManagerRequest)requests.removeFirst();
        synchronized( loading_messages ) {
          loading_messages.add( dmr );
        }
        disk_manager.enqueueReadRequest( dmr, read_req_listener );
        num_messages_loading++;
      }
    }
  }

  
}
