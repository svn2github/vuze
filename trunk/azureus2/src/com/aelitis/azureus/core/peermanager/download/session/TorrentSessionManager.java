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
import java.util.Iterator;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.networkmanager.IncomingMessageQueue;
import com.aelitis.azureus.core.peermanager.connection.*;
import com.aelitis.azureus.core.peermanager.download.TorrentDownload;
import com.aelitis.azureus.core.peermanager.download.session.standard.StandardSessionManager;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.azureus.*;



public class TorrentSessionManager {
  
  private static final TorrentSessionManager instance = new TorrentSessionManager();
  
  private final HashMap registrations = new HashMap();

  protected AEMonitor this_mon = new AEMonitor( "TorrentSessionManager" );
    
  public static TorrentSessionManager getSingleton(){  return instance;  }

  
  private TorrentSessionManager() {
    /*nothing*/
  }
  
  
  public void init() {
    //init "built-in" session type managers
    StandardSessionManager.getSingleton();
    
    
    //register for new peer connection creation notification, so that we can catch torrent session syn messages
    PeerConnectionFactory.getSingleton().registerCreationListener( new PeerConnectionFactory.CreationListener() {
      public void connectionCreated( final AZPeerConnection connection ) {
        connection.getNetworkConnection().getIncomingMessageQueue().registerQueueListener( new IncomingMessageQueue.MessageQueueListener() {
          public boolean messageReceived( Message message ) {
            if( message.getID().equals( AZMessage.ID_AZ_TORRENT_SESSION_SYN ) ) {
              AZTorrentSessionSyn syn = (AZTorrentSessionSyn)message;

              TorrentSession session = TorrentSessionFactory.getSingleton().createSession( syn.getSessionType(), syn.getInfoHash(), connection );
              session.remote_session_id = syn.getSessionID();
              
              TorrentSessionListener listener = null;
              try{  this_mon.enter();
                listener = (TorrentSessionListener)registrations.get( syn.getSessionType() );
              }
              finally{  this_mon.exit();  }
              
              if( listener != null ) {  
                listener.torrentSessionRequested( session, syn.getSessionInfo() );
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
  
  
  
  /**
   * Register for incoming session requests of the given type.
   * @param type_id of the listener
   * @param listener to handle requests
   */
  public void registerIncomingSessionListener( String type_id, TorrentSessionListener listener ) {
    try{  this_mon.enter();
      registrations.put( type_id, listener );
    }
    finally{  this_mon.exit();  }
  }
  
  
  /**
   * Remove registration for incoming session requests of the given type.
   * @param type_id to remove
   */
  public void deregisterIncomingSessionListener( String type_id ){
    try{  this_mon.enter();
      registrations.remove( type_id );
    }
    finally{  this_mon.exit();  }
  }
  
  
  /*
  public String[] getRegisteredSessionTypes() {
    return (String[])registrations.keySet().toArray( new String[0] );
  }
  */

  
  
  /**
   * Register the given download for torrent session management.
   * @param download to add
   */
  public void registerForSessionManagement( TorrentDownload download ) {
    //add infohash to incoming listeners
    try{  this_mon.enter();
      for( Iterator it = registrations.values().iterator(); it.hasNext(); ) {
        TorrentSessionListener listener = (TorrentSessionListener)it.next();
        listener.registerSessionInfoHash( download.getInfoHash() );
      }
    }
    finally{  this_mon.exit();  }
    
    //TODO
  }
  
  
  /**
   * Deregister the given download from torrent session management.
   * @param download to remove
   */
  public void deregisterForSessionManagement( TorrentDownload download ) {
    //remove infohash from incoming listeners
    try{  this_mon.enter();
      for( Iterator it = registrations.values().iterator(); it.hasNext(); ) {
        TorrentSessionListener listener = (TorrentSessionListener)it.next();
        listener.deregisterSessionInfoHash( download.getInfoHash() );
      }
    }
    finally{  this_mon.exit();  }
    
    //TODO
  }
  
  
  /**
   * Initiate a standard torrent session for the given download with the given peer connection.
   * @param download for session
   * @param connection to send request to
   */
  public void requestStandardTorrentSession( TorrentDownload download, AZPeerConnection connection ) {
    TorrentSession session = TorrentSessionFactory.getSingleton().createSession( StandardSessionManager.SESSION_TYPE_ID, download.getInfoHash(), connection );
    session.requestSession( null, StandardSessionManager.getSingleton() );
  }
  
}
