/*
 * Created on Jun 16, 2004
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

package com.aelitis.azureus.core.peermanager;

import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.peer.impl.PEPeerTransport;


/**
 * Manages and caches potential new peer connections.
 */
public class NewPeerManager {
  private static final long SLOW_WAIT_TIME = 2000;
  private static final long NORM_WAIT_TIME = 250;
  private static final NewPeerManager instance = new NewPeerManager();
  
  private final LinkedHashMap listeners = new LinkedHashMap( 1, .75F, true );
  
  
  private NewPeerManager() {
    final Thread main_thread = new Thread( "NewPeerManager" ) {
      public void run() {
        mainLoop();
      }
    };
    main_thread.setDaemon( true );
    main_thread.start();
  }
  
  
  private void mainLoop() {
    while( true ) {
      boolean already_added = false;
      synchronized( listeners ) {
        int size = listeners.size();
        while( !already_added && size != 0 ) {
          Listener listener = (Listener)listeners.keySet().iterator().next();
          HashMap peer_list = (HashMap)listeners.get( listener );
          if( peer_list.size() > 0 && listener.isNewPeerNeeded() ) {
            for( Iterator it = peer_list.keySet().iterator(); it.hasNext(); ) {
              PEPeerTransport peer = (PEPeerTransport)it.next();
              if( !listener.isAlreadyConnected( peer ) ) {
                listener.addNewPeer( peer );
                already_added = true;
                it.remove();
                break;
              }
              it.remove();
            }
          }
          size--;
        }
      }
      
      long sleep_time = COConfigurationManager.getBooleanParameter("Slow Connect") ? SLOW_WAIT_TIME : NORM_WAIT_TIME;
      if( !already_added ) sleep_time = SLOW_WAIT_TIME;
      
      try{  Thread.sleep( sleep_time );  } catch( Exception e) { e.printStackTrace(); }
    }
  }
  
 
  
  /**
   * Register a potential new peer connection for future establishment.
   * @param listener new peer listener
   * @param peer new peer
   */
  public static void registerNewPeer( NewPeerManager.Listener listener, PEPeerTransport peer ) {
    synchronized( instance.listeners ) {
      HashMap peer_list = (HashMap)instance.listeners.get( listener );
      if( peer_list == null ) peer_list = new HashMap();
      peer_list.put( peer, null );
      instance.listeners.put( listener, peer_list );
    }
  }
  
  
  /**
   * Remove all pending peer connections for this listener.
   * Used on download termination.
   * @param listener download to remove
   */
  public static void cancelAllNewPeers( NewPeerManager.Listener listener ) {
    synchronized( instance.listeners ) {
      instance.listeners.remove( listener );
    }
  }
  

  /**
   * Listener for new pending peer connections.
   */
  public static interface Listener {
    /**
     * Check if the download needs a new peer connection.
     * @return true if a new peer is allowed, false otherwise
     */
    public boolean isNewPeerNeeded();
    
    /**
     * Check if the given potential peer connection is already connected.
     * @param peer
     * @return true if already connected, otherwise false
     */
    public boolean isAlreadyConnected( PEPeerTransport peer );
    
    /**
     * Add and establish the given new peer connection.
     * @param peer
     */
    public void addNewPeer( PEPeerTransport peer );
  }
  
}
