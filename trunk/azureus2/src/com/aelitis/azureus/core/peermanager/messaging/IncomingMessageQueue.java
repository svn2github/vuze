/*
 * Created on Oct 17, 2004
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

package com.aelitis.azureus.core.peermanager.messaging;

import java.util.*;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.DirectByteBuffer;



/**
 *
 */
public class IncomingMessageQueue {
  
  private final ArrayList listeners = new ArrayList();
  private final AEMonitor listeners_mon = new AEMonitor( "xxxxxxxx" );
  
  
  
  protected IncomingMessageQueue() {
    //nothing
  }
  
  
  
  
  /**
   * Add a listener to be notified of queue events.
   * @param listener
   */
  public void registerQueueListener( MessageQueueListener listener ) {
    try{
      listeners_mon.enter();
    
      listeners.add( listener );
    }
    finally{
      listeners_mon.exit();
    }
  }
  
  
  /**
   * Cancel queue event notification listener.
   * @param listener
   */
  public void cancelQueueListener( MessageQueueListener listener ) {
    try{
      listeners_mon.enter();
    
      listeners.remove( listener );
    }
    finally{
      listeners_mon.exit();
    }
  }
  
  
  
  public interface MessageQueueListener {
    /**
     * A message has been read from the transport.
     * @param message_data byte contents
     */
    public void messageReceived( DirectByteBuffer message_data );
    
    /**
     * The given number of protocol (overhead) bytes read from the transport.
     * @param byte_count number of protocol bytes
     */
    public void protocolBytesReceived( int byte_count );
    
    
    /**
     * The given number of (piece) data bytes read from the transport.
     * @param byte_count number of data bytes
     */
    public void dataBytesReceived( int byte_count );
    
  }
  
  
}
