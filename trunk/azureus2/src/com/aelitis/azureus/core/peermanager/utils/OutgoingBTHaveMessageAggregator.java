/*
 * Created on Jul 18, 2004
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

package com.aelitis.azureus.core.peermanager.utils;

import java.util.*;

import org.gudy.azureus2.core3.util.AEMonitor;

import com.aelitis.azureus.core.networkmanager.*;
import com.aelitis.azureus.core.peermanager.messages.ProtocolMessage;
import com.aelitis.azureus.core.peermanager.messages.bittorrent.*;


/**
 * Utility class to enable write aggregation of BT Have messages,
 * in order to save bandwidth by not wasting a whole network packet
 * on a single small 9-byte message, and instead pad them onto other
 * messages.
 */
public class OutgoingBTHaveMessageAggregator {
  
  private final ArrayList 	pending_haves 		= new ArrayList();
  private final AEMonitor	pending_haves_mon	= new AEMonitor( "OutgoingBTHaveMessageAggregator:PH");

  private final OutgoingMessageQueue outgoing_message_q;
    
  private final OutgoingMessageQueue.MessageQueueListener added_message_listener = new OutgoingMessageQueue.MessageQueueListener() {
    public void messageAdded( ProtocolMessage message ) {
      //if another message is going to be sent anyway, add our haves as well
      if( message.getType() != BTProtocolMessage.BT_HAVE ) {
        sendPendingHaves();
      }
    }
    public void messageRemoved( ProtocolMessage message ) {/*nothing*/}
    public void messageSent( ProtocolMessage message ) {/*nothing*/}
    public void bytesSent( int byte_count ) {/*nothing*/}
  };
  
  
  
  
  /**
   * Create a new aggregator, which will send messages out the given queue.
   * @param outgoing_message_q
   */
  public OutgoingBTHaveMessageAggregator( OutgoingMessageQueue outgoing_message_q ) {
    this.outgoing_message_q = outgoing_message_q;
    outgoing_message_q.registerQueueListener( added_message_listener );
  }
  
  
  /**
   * Queue a new have message for aggregated sending.
   * @param piece_number of the have message
   * @param force if true, send this and any other pending haves right away
   */
  public void queueHaveMessage( int piece_number, boolean force ) {
    try{
      pending_haves_mon.enter();
    
      pending_haves.add( new Integer( piece_number ) );
      if( force ) {
        sendPendingHaves();
      }
      else {
        int pending_bytes = pending_haves.size() * 9;
        if( pending_bytes >= NetworkManager.getSingleton().getTcpMssSize() ) {
          System.out.println("enough pending haves for a full packet!");
          //there's enough pending bytes to fill a packet payload
          sendPendingHaves();
        }
      }
    }finally{
    	
    	pending_haves_mon.exit();
    }
  }
  
  
  /**
   * Destroy the aggregator, along with any pending messages.
   */
  public void destroy() {
    try{
      pending_haves_mon.enter();
    
      pending_haves.clear();
    }finally{
    	
      pending_haves_mon.exit();
    }
  }
  
  
  /**
   * Force send of any aggregated/pending have messages.
   */
  public void forceSendOfPending() {   
    sendPendingHaves();
  }
  
  
  private void sendPendingHaves() {    
    try{
      pending_haves_mon.enter();
    
      for( int i=0; i < pending_haves.size(); i++ ) {
        Integer piece_num = (Integer)pending_haves.get( i ); 
        outgoing_message_q.addMessage( new BTHave( piece_num.intValue() ), false );
      }
      pending_haves.clear();
    }finally{
    	
      pending_haves_mon.exit();
    }
  }

}
