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

import java.util.*;

import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.networkmanager.IncomingMessageQueue;
import com.aelitis.azureus.core.peermanager.connection.AZPeerConnection;
import com.aelitis.azureus.core.peermanager.download.TorrentDownload;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.azureus.*;


public class TorrentSession {
  private static int next_session_id = 0;
  private static final AEMonitor session_mon = new AEMonitor( "TorrentSession" );

  private int local_session_id;
  private int remote_session_id;
  private TorrentSessionAuthenticator authenticator;
  private AZPeerConnection connection;
  private TorrentDownload download;
  private TimerEvent syn_timeout_timer;

  
  private final IncomingMessageQueue.MessageQueueListener incoming_q_listener = new IncomingMessageQueue.MessageQueueListener() {
    public boolean messageReceived( Message message ) {
      if( message.getID().equals( AZMessage.ID_AZ_TORRENT_SESSION_ACK ) ) {          
        AZTorrentSessionAck ack = (AZTorrentSessionAck)message;

        if( ack.getSessionType().equals( authenticator.getSessionTypeID() ) && Arrays.equals( ack.getInfoHash(), authenticator.getSessionInfoHash() ) ) {
          remote_session_id = ack.getSessionID();  //capture send-to id
          syn_timeout_timer.cancel();  //abort timeout check
          
          try{
            authenticator.verifySessionAck( ack.getSessionInfo() );
            startSessionProcessing();
          }
          catch( AuthenticatorException ae ) {
            endSession( "AuthenticatorException:: " +ae.getMessage() );
          }

          ack.destroy();
          return true;
        }
      }
      
      if( message.getID().equals( AZMessage.ID_AZ_TORRENT_SESSION_END ) ) {          
        AZTorrentSessionEnd end = (AZTorrentSessionEnd)message;
        
        if( end.getSessionType().equals( authenticator.getSessionTypeID() ) && Arrays.equals( end.getInfoHash(), authenticator.getSessionInfoHash() ) ) {
          System.out.println( "AZ_TORRENT_SESSION_END received: " +end.getEndReason() );
          
          destroy();  //close session
          
          end.destroy();
          return true;
        } 
      }
      
      return false;
    }

    public void protocolBytesReceived( int byte_count ){}
    public void dataBytesReceived( int byte_count ){}
  };
  
  
  
  
  //INCOMING SESSION INIT
  protected TorrentSession( TorrentSessionAuthenticator auth, AZPeerConnection peer, TorrentDownload download, int remote_id ) {
    init( auth, peer, download, remote_id );  
  }
  
  
  //OUTGOING SESSION INIT
  protected TorrentSession( TorrentSessionAuthenticator auth, AZPeerConnection peer, TorrentDownload download ) {
    init( auth, peer, download, -1 );
  }

  
  
  private void init( TorrentSessionAuthenticator a, AZPeerConnection p, TorrentDownload d, int r ) {
    this.authenticator = a;
    this.connection = p;
    this.download = d;
    this.remote_session_id = r;
    
    try{ session_mon.enter();
      local_session_id = next_session_id;
      next_session_id++;
    }
    finally{ session_mon.exit();  }
    
    connection.getNetworkConnection().getIncomingMessageQueue().registerQueueListener( incoming_q_listener );
  }
  
  

  protected void authenticate( Map incoming_syn ) {
    if( incoming_syn == null ) {  //outgoing session
      //send out the session request
      AZTorrentSessionSyn syn = new AZTorrentSessionSyn( local_session_id, authenticator.getSessionTypeID(), authenticator.getSessionInfoHash(), authenticator.createSessionSyn() );
      connection.getNetworkConnection().getOutgoingMessageQueue().addMessage( syn, false );
      
      //set a timeout timer in case the other peer forgets to send an ACK or END reply
      syn_timeout_timer = SimpleTimer.addEvent( SystemTime.getCurrentTime() + 60*1000, new TimerEventPerformer() {
        public void  perform( TimerEvent  event ) {
          endSession( "no session ACK received after 60sec, request timed out" );
        }
      });
    }
    else {  //incoming session
      try{
        Map ack_reply = authenticator.verifySessionSyn( incoming_syn );
        
        //send out the session acceptance
        AZTorrentSessionAck ack = new AZTorrentSessionAck( local_session_id, authenticator.getSessionTypeID(), authenticator.getSessionInfoHash(), ack_reply );
        connection.getNetworkConnection().getOutgoingMessageQueue().addMessage( ack, false );
        
        startSessionProcessing();
      }
      catch( AuthenticatorException ae ) {  //on syn authentication error
        endSession( "AuthenticatorException:: " +ae.getMessage() );
      }      
    }
  }
  
  

  private void endSession( String end_reason ){
    System.out.println( "endSession:: " +end_reason );
    
    //send end notice
    AZTorrentSessionEnd end = new AZTorrentSessionEnd( authenticator.getSessionTypeID(), authenticator.getSessionInfoHash(), end_reason );
    connection.getNetworkConnection().getOutgoingMessageQueue().addMessage( end, false );
    
    destroy();
  }

  
  
  private void destroy(){
    if( syn_timeout_timer != null )  syn_timeout_timer.cancel();  //abort timeout check if running
    connection.getNetworkConnection().getIncomingMessageQueue().cancelQueueListener( incoming_q_listener );
    stopSessionProcessing();
  }
  
  
  
  private void startSessionProcessing() {
    download.registerTorrentSession( this );
    //TODO ?
  }
  
  
  
  private void stopSessionProcessing() {
    download.deregisterTorrentSession( this );
    //TODO ?
  }
  

  
}
