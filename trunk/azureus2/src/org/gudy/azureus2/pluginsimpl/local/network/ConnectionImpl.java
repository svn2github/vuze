/*
 * Created on Feb 9, 2005
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

package org.gudy.azureus2.pluginsimpl.local.network;

import org.gudy.azureus2.plugins.network.*;

import com.aelitis.azureus.core.networkmanager.LimitedRateGroup;


/**
 *
 */
public class ConnectionImpl implements Connection {

  private final com.aelitis.azureus.core.networkmanager.NetworkConnection core_connection;
  private final OutgoingMessageQueueImpl out_queue;
  private final IncomingMessageQueueImpl in_queue;
  private final TCPTransportImpl tcp_transport;
  
  
  public ConnectionImpl( com.aelitis.azureus.core.networkmanager.NetworkConnection core_connection ) {
    this.core_connection = core_connection;
    this.out_queue = new OutgoingMessageQueueImpl( core_connection.getOutgoingMessageQueue() );
    this.in_queue = new IncomingMessageQueueImpl( core_connection.getIncomingMessageQueue() );
    this.tcp_transport = new TCPTransportImpl( core_connection.getTCPTransport() );
  }
  
  
  public void connect( final ConnectionListener listener ) {
    core_connection.connect( new com.aelitis.azureus.core.networkmanager.NetworkConnection.ConnectionListener() {
      public void connectStarted() { listener.connectStarted();  }
      
      public void connectSuccess() { listener.connectSuccess();  }
      
      public void connectFailure( Throwable failure_msg ) {  listener.connectFailure( failure_msg );  }
      public void exceptionThrown( Throwable error ) {  listener.exceptionThrown( error );  }
    });
  }
  
  
  public void close() {
    core_connection.close();
  }

  
  public OutgoingMessageQueue getOutgoingMessageQueue() {  return out_queue;  }

  public IncomingMessageQueue getIncomingMessageQueue() {  return in_queue;  }

  
  public void startMessageProcessing() {
    core_connection.startMessageProcessing(
        new LimitedRateGroup() {
          public int getRateLimitBytesPerSecond() {  return 0;  }  //no specific write limit for now
        },
        new LimitedRateGroup() {
          public int getRateLimitBytesPerSecond() {  return 0;  }  //no specific read limit for now
        }
    );     
    
    core_connection.enableEnhancedMessageProcessing( true );  //auto-upgrade connection
  }
  
  
  public Transport getTransport() {  return tcp_transport;  }
  
  
  public com.aelitis.azureus.core.networkmanager.NetworkConnection getCoreConnection() {
    return core_connection;
  }
  
}
