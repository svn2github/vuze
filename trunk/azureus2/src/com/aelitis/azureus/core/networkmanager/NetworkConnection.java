/*
 * Created on Feb 21, 2005
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

package com.aelitis.azureus.core.networkmanager;

import java.net.InetSocketAddress;


/**
 * Represents a managed network connection, over which messages can be sent and received. 
 */
public interface NetworkConnection {
  
  
  /**
   * Connect this connection's transport, i.e. establish the network connection.
   * If this connection is already established (from an incoming connection for example),
   * then this provides a mechanism to register the connection listener, in which case
   * connectSuccess() will be called immediately.
   * @param listener notified on connect success or failure
   */
  public void connect( ConnectionListener listener );

  
  /**
   * Close and shutdown this connection.
   */
  public void close();
  
  
  /**
   * Inform connection of a thrown exception.
   * @param error exception
   */
  public void notifyOfException( Throwable error );
  

  
  /**
   * Get the connection's outgoing message queue.
   * @return outbound message queue
   */
  public OutgoingMessageQueue getOutgoingMessageQueue();
  
  
  /**
   * Get the connection's incoming message queue.
   * @return inbound message queue
   */
  public IncomingMessageQueue getIncomingMessageQueue();
  
  
  /**
   * Begin processing incoming and outgoing message queues.
   * @param upload_group upload rate limit group to use
   * @param download_group download rate limit group to use
   */
  public void startMessageProcessing( LimitedRateGroup upload_group, LimitedRateGroup download_group );
  
  
  /**
   * Upgrade the connection to high-speed transfer processing.
   * @param enable true for high-speed processing, false for normal processing
   */
  public void enableEnhancedMessageProcessing( boolean enable );
  
  
  /**
   * Get the connection's data tcp transport interface.
   * @return the transport
   */
  public TCPTransport getTCPTransport();
  

  
  /**
   * Get the connection's internet address.
   * @return remote address
   */
  public InetSocketAddress getAddress();  //TODO hmmm

  

  
  
  
  /**
   * Listener for notification of connection events.
   */
  public interface ConnectionListener {
    /**
     * The connection establishment process has started,
     * i.e. the connection is actively being attempted.
     */
    public void connectStarted();    
    
    /**
     * The connection attempt succeeded.
     * The connection is now established.
     * NOTE: Called only during initial connect attempt.
     */
    public void connectSuccess();
    
    /**
     * The connection attempt failed.
     * NOTE: Called only during initial connect attempt.
     * @param failure_msg failure reason
     */
    public void connectFailure( Throwable failure_msg );
    
    /**
     * Handle exception thrown by this connection.
     * NOTE: Can be called at any time during connection lifetime.
     * @param error exception
     */
    public void exceptionThrown( Throwable error );
  }
}
