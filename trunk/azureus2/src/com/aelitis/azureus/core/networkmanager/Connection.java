/*
 * Created on Jul 29, 2004
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

package com.aelitis.azureus.core.networkmanager;

import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.peermanager.messages.ProtocolMessage;

/**
 *
 */
public class Connection {
  private final Transport transport;
  private final ConnectionListener listener;
  private final OutgoingMessageQueue outgoing_message_q;
  private long last_new_write_data_added_time = 0;
  private boolean transport_ready_for_write = true;
    
  
  protected Connection( Transport transport, ConnectionListener listener ) {
    this.transport = transport;
    this.listener = listener;
    outgoing_message_q = new OutgoingMessageQueue( transport );
    outgoing_message_q.registerAddedListener( new OutgoingMessageQueue.AddedMessageListener() {
      public void messageAdded( ProtocolMessage message ) {
        last_new_write_data_added_time = SystemTime.getCurrentTime();
      }
    });
  }
  
  
  protected Transport getTransport() {  return transport;  }
  
  protected void notifyOfException( Throwable error ) {  listener.notifyOfException( error );  }
  
  public OutgoingMessageQueue getOutgoingMessageQueue() {  return outgoing_message_q;  }
  
  
  /**
   * Get the last time a new message (data) was added to the outbound queue.
   * @return time
   */
  protected long getLastNewWriteDataAddedTime() {  return last_new_write_data_added_time;  }
  
  /**
   * Is the underlying transport (socket) ready for a non-blocking write.
   * @return true if ready
   */
  protected boolean isTransportReadyForWrite() {  return transport_ready_for_write;  }
  
  /**
   * Set whether or not the underlying transport is ready for a non-blocking write.
   * @param is_ready
   */
  protected void setTransportReadyForWrite( boolean is_ready ) {  transport_ready_for_write = is_ready;  }
  
  
  public void destroy() {
    outgoing_message_q.destroy();
    transport.destroy();
  }

  
  public String toString() {
    return transport.getDescription();
  }
  
  
  /////////////////////////////////////////////////////////////////////
  
  /**
   * General listener for connection events.
   */
  public interface ConnectionListener {
    /**
     * Handle an exception thrown by this connection.
     * @param error exception
     */
    public void notifyOfException( Throwable error );
  }
  
}
