/*
 * Created on Feb 24, 2005
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

package org.gudy.azureus2.pluginsimpl.local.messaging;

import java.util.*;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.*;
import org.gudy.azureus2.plugins.messaging.*;
import org.gudy.azureus2.plugins.peers.*;



/**
 *
 */
public class MessageManagerImpl implements MessageManager {
  
  private static final MessageManagerImpl instance = new MessageManagerImpl();
  
  private final HashMap compat_checks = new HashMap();
  
  private final DownloadManagerListener download_manager_listener = new DownloadManagerListener() {
    public void downloadAdded( Download dwnld ) {
      dwnld.addPeerListener( new DownloadPeerListener() {
        public void peerManagerAdded( final Download download, PeerManager peer_manager ) {
          peer_manager.addListener( new PeerManagerListener() {
            public void peerAdded( PeerManager manager, final Peer peer ) {
              peer.addListener( new PeerListener() {
                public void stateChanged( int new_state ) {
                  
                  if( new_state == Peer.TRANSFERING ) {  //the peer handshake has completed
                    if( peer.supportsMessaging() ) {  //if it supports advanced messaging
                      //see if it supports any registered message types
                      Message[] messages = peer.getSupportedMessages();

                      for( int i=0; i < messages.length; i++ ) {
                        Message msg = messages[i];
                        
                        for( Iterator it = compat_checks.entrySet().iterator(); it.hasNext(); ) {
                          Map.Entry entry = (Map.Entry)it.next();
                          Message message = (Message)entry.getKey();
                          
                          if( msg.getID().equals( message.getID() ) && msg.getVersion() == message.getVersion() ) {  //it does !
                            MessageManagerListener listener = (MessageManagerListener)entry.getValue();
                            
                            listener.compatiblePeerFound( download, peer, message );
                          }
                        }
                      }
                    }
                  }
                }
                
                public void sentBadChunk( int piece_num, int total_bad_chunks ) { /*nothing*/ }
              });
            }

            public void peerRemoved( PeerManager manager, Peer peer ) {
              for( Iterator i = compat_checks.values().iterator(); i.hasNext(); ) {
                MessageManagerListener listener = (MessageManagerListener)i.next();
                
                listener.peerRemoved( download, peer );
              }
            }
          });
        }

        public void peerManagerRemoved( Download download, PeerManager peer_manager ) { /* nothing */ }
      });
    }
    
    public void downloadRemoved( Download download ) { /* nothing */ }
  };
  
  
  
  
  
  
  public static MessageManagerImpl getSingleton() {  return instance;  }
  
  
  private MessageManagerImpl() {
    /*nothing*/
  }
  
  
  public void registerMessageType( Message message ) throws MessageException {
    try {
      com.aelitis.azureus.core.peermanager.messaging.MessageManager.getSingleton().registerMessageType( new MessageAdapter( message ) );
    }
    catch( com.aelitis.azureus.core.peermanager.messaging.MessageException me ) {
      throw new MessageException( me.getMessage() );
    }
  }

  public void deregisterMessageType( Message message ) {
    com.aelitis.azureus.core.peermanager.messaging.MessageManager.getSingleton().deregisterMessageType( new MessageAdapter( message ) );
  }
    
  
  
  public void locateCompatiblePeers( PluginInterface plug_interface, Message message, MessageManagerListener listener ) {
    compat_checks.put( message, listener );  //TODO need to copy-on-write?
    
    if( compat_checks.size() == 1 ) {  //only register global peer locator listener once
      plug_interface.getDownloadManager().addListener( download_manager_listener );
    }
  }
  
  
  public void cancelCompatiblePeersLocation( MessageManagerListener orig_listener ) {
    for( Iterator it = compat_checks.values().iterator(); it.hasNext(); ) {
      MessageManagerListener listener = (MessageManagerListener)it.next();
      
      if( listener == orig_listener ) {
        it.remove();
        break;
      }
    }
  }
}
