/*
 * Created on Jul 3, 2005
 * Created by Alon Rohter
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.core.peermanager.download.session;

import java.util.HashMap;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.networkmanager.IncomingMessageQueue;
import com.aelitis.azureus.core.peermanager.connection.*;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.azureus.*;



public class TorrentSessionManager {
  
  private static final TorrentSessionManager instance = new TorrentSessionManager();
  
  private final HashMap registrations = new HashMap();

  protected AEMonitor this_mon = new AEMonitor( "TorrentSessionManager" );
  
 
  //TODO ack wait timer...abort session attempt after say 60s 
  
  
  
  public static TorrentSessionManager getSingleton(){  return instance;  }

  
  private TorrentSessionManager() {
    //register for new peer connection creation notification, so that we can catch torrent session syn messages
    PeerConnectionFactory.getSingleton().registerCreationListener( new PeerConnectionFactory.CreationListener() {
      public void connectionCreated( final AZPeerConnection connection ) {
        connection.getNetworkConnection().getIncomingMessageQueue().registerQueueListener( new IncomingMessageQueue.MessageQueueListener() {
          public boolean messageReceived( Message message ) {
            if( message.getID().equals( AZMessage.ID_AZ_TORRENT_SESSION_SYN ) ) {
              AZTorrentSessionSyn syn = (AZTorrentSessionSyn)message;

              TorrentSession session = TorrentSessionFactory.getSingleton().createSession( syn.getSessionType(), syn.getInfoHash(), connection );
              
              TorrentSessionListener listener = (TorrentSessionListener)registrations.get( syn.getSessionType() );
              
              if( listener != null ) {  
                listener.torrentSessionRequested( session );
              }
              else {  //unknown session type
                session.endSession( "unknown session type id" );  //return error
                Debug.out( "unknown incoming torrent session type: " +syn.getSessionType() );
              }
              
              syn.destroy();
              return true;
            }
            
            return false;
          }

          public void protocolBytesReceived( int byte_count ){}
          public void dataBytesReceived( int byte_count ){}
        });
      }
    });
  }
  
  

  public void registerIncomingSessionListener( String type_id, TorrentSessionListener listener ) {
    try{  this_mon.enter();
      registrations.put( type_id, listener );
    }
    finally{  this_mon.exit();  }
  }
  
  
  public void deregisterIncomingSessionListener( String type_id ){
    try{  this_mon.enter();
      registrations.remove( type_id );
    }
    finally{  this_mon.exit();  }
  }
  

  public String[] getRegisteredSessionTypes() {
    return (String[])registrations.keySet().toArray( new String[0] );
  }
  

  
}
