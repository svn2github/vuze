/*
 * Created on Jul 5, 2005
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

import java.util.Arrays;
import java.util.Map;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;

import com.aelitis.azureus.core.networkmanager.IncomingMessageQueue;
import com.aelitis.azureus.core.peermanager.connection.AZPeerConnection;
import com.aelitis.azureus.core.peermanager.download.TorrentDownload;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.azureus.*;


public class TorrentSession {
  private static int next_session_id = 0;
  private static final AEMonitor session_mon = new AEMonitor( "TorrentSession" );
  
  private static final int STATE_NEW  = 0;
  private static final int STATE_SYN  = 1;
  private static final int STATE_RUN  = 2;
  private static final int STATE_END  = 3;
  
  private final int local_session_id;
  private int remote_session_id;
  private final TorrentSessionAuthenticator authenticator;
  private final AZPeerConnection connection;
  private final TorrentDownload download;
  private int session_state = STATE_NEW;

  private IncomingMessageQueue.MessageQueueListener incoming_q_listener = null;
  
  
  
  //INCOMING SESSION INIT
  protected TorrentSession( TorrentSessionAuthenticator auth, AZPeerConnection peer, TorrentDownload download, int remote_id ) {
    this.authenticator = auth;
    this.connection = peer;
    this.download = download;
    this.remote_session_id = remote_id;
    this.local_session_id = getLocalSessionID();    
  }
  
  
  //OUTGOING SESSION INIT
  protected TorrentSession( TorrentSessionAuthenticator auth, AZPeerConnection peer, TorrentDownload download ) {
    this.authenticator = auth;
    this.connection = peer;
    this.download = download;
    this.local_session_id = getLocalSessionID();    
  }

  
  private int getLocalSessionID() {
    try{ session_mon.enter();
      int id = next_session_id;
      next_session_id++;
      return id;
    }
    finally{ session_mon.exit();  }
  }
  
  
  
  
  protected void authenticate( Map incoming_syn ) {
    if( incoming_syn == null ) {  //outgoing session
      Map outgoing_syn = authenticator.createSessionSyn();
      
    }
    else {  //incoming session
      
    }
  }
  
  
  
  
  /**
   * Send a session initialization (SYN) request.
   * @param syn_info bencode-able exchange map
   * @param handler for session events
   */
  public void requestSession( Map syn_info, TorrentSessionAuthenticator handler ) {
    if( session_state != STATE_NEW ) {
      Debug.out( "session_state[" +session_state+ "] != STATE_NEW" );
    }    
    
    attachHandler( handler );

    //send out the session request
    AZTorrentSessionSyn syn = new AZTorrentSessionSyn( local_session_id, authenticator.getSessionTypeID(), authenticator.getSessionInfoHash(), syn_info );
    connection.getNetworkConnection().getOutgoingMessageQueue().addMessage( syn, false );
    session_state = STATE_SYN;
    
    //set a timeout timer in case the other peer forgets to send an ACK or END reply
    SimpleTimer.addEvent( SystemTime.getCurrentTime() + 60*1000, new TimerEventPerformer() {
      public void  perform( TimerEvent  event ) {
        if( session_state == STATE_SYN ) {
          endSession( "no session ACK received after 60sec, request timed out" );
        }
      }
    });
  }
  

  /**
   * Acknowledge (ACK) and accept the session.
   * @param ack_info bencode-able exchange map
   * @param handler for session events
   */
  public void ackSession( Map ack_info, TorrentSessionAuthenticator handler ) {
    if( session_state != STATE_NEW ) {
      Debug.out( "session_state[" +session_state+ "] != STATE_NEW" );
    }
    
    attachHandler( handler );
    
    //send out the session acceptance
    AZTorrentSessionAck ack = new AZTorrentSessionAck( local_session_id, authenticator.getSessionTypeID(), authenticator.getSessionInfoHash(), ack_info );
    connection.getNetworkConnection().getOutgoingMessageQueue().addMessage( ack, false );
    
    startSessionProcessing();
  }
  
  
  
  /**
   * End this torrent session for the given reason.
   * @param end_reason of end/error
   */
  public void endSession( String end_reason ){
    if( session_state == STATE_END ) {
      Debug.out( "session_state == STATE_END" );
    }
    
    AZTorrentSessionEnd end = new AZTorrentSessionEnd( authenticator.getSessionTypeID(), authenticator.getSessionInfoHash(), end_reason );
    connection.getNetworkConnection().getOutgoingMessageQueue().addMessage( end, false );
    destroy();
  }

  
  
  private void startSessionProcessing() {
    session_state = STATE_RUN;
    
    //TODO
  }
  
  
  
  private void stopSessionProcessing() {
    //TODO
  }
  
  
  
  
  private void attachHandler( final TorrentSessionAuthenticator handler ) {
    //register for session ACK and END messages
    incoming_q_listener = new IncomingMessageQueue.MessageQueueListener() {
      public boolean messageReceived( Message message ) {
        if( message.getID().equals( AZMessage.ID_AZ_TORRENT_SESSION_ACK ) ) {
          if( session_state != STATE_SYN ) {
            Debug.out( "session_state[" +session_state+ "] != STATE_SYN" );
          }
          
          AZTorrentSessionAck ack = (AZTorrentSessionAck)message;

          if( ack.getSessionType().equals( authenticator.getSessionTypeID() ) && Arrays.equals( ack.getInfoHash(), authenticator.getSessionInfoHash() ) ) {
            remote_session_id = ack.getSessionID();
            //if( handler.sessionAcked( TorrentSession.this, ack.getSessionInfo() ) ) {
            //  startSessionProcessing();
            //}
            ack.destroy();
            return true;
          }
        }
        
        if( message.getID().equals( AZMessage.ID_AZ_TORRENT_SESSION_END ) ) {          
          AZTorrentSessionEnd end = (AZTorrentSessionEnd)message;
          
          if( end.getSessionType().equals( authenticator.getSessionTypeID() ) && Arrays.equals( end.getInfoHash(), authenticator.getSessionInfoHash() ) ) {
            //handler.sessionEnded( TorrentSession.this, end.getEndReason() );
            end.destroy();
            destroy();
            return true;
          } 
        }
        
        return false;
      }

      public void protocolBytesReceived( int byte_count ){}
      public void dataBytesReceived( int byte_count ){}
    };
    
    connection.getNetworkConnection().getIncomingMessageQueue().registerQueueListener( incoming_q_listener );
  }
  
  
  
  private void destroy(){
    if( session_state != STATE_END ) {
      session_state = STATE_END;
      
      stopSessionProcessing();
      
      if( incoming_q_listener != null ) {
        connection.getNetworkConnection().getIncomingMessageQueue().cancelQueueListener( incoming_q_listener );
      }
    }
    else {
      Debug.out( "session_state == STATE_END" );
    }
  }
}
