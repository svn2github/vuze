/*
 * Created on Apr 27, 2005
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

package com.aelitis.azureus.core.peermanager.peerdb;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.Debug;


/**
 *
 */
public class PeerItemFactory {
  public static final int PEER_SOURCE_TRACKER       = 0;
  public static final int PEER_SOURCE_DHT           = 1;
  public static final int PEER_SOURCE_PEER_EXCHANGE = 2;
  public static final int PEER_SOURCE_PLUGIN        = 3;
  public static final int PEER_SOURCE_INCOMING      = 4;
  

  private static final WeakHashMap peer_items = new WeakHashMap();

  private static final AEMonitor item_mon = new AEMonitor( "PeerItemFactory" );
  
  
  /**
   * Create a peer item using the given peer address and port information.
   * @param address of peer
   * @param port of peer
   * @param source this peer info was obtained from
   * @return peer
   */
  public static PeerItem createPeerItem( String address, int port, int source ) {
    return getLightweight( new PeerItem( address, port, source ) );
  }
  
  /**
   * Create a peer item using the given peer raw byte serialization (address and port).
   * @param serialization bytes
   * @param source this peer info was obtained from
   * @return peer
   */
  public static PeerItem createPeerItem( byte[] serialization, int source ) {
    return getLightweight( new PeerItem( serialization, source ) );
  }
  
  
  
  private static PeerItem getLightweight( PeerItem key ) {
    try{  item_mon.enter();
      WeakReference ref = (WeakReference)peer_items.get( key );

      if( ref == null ) {
        peer_items.put( key, new WeakReference( key ) );
        return key;
      }
 
      PeerItem item = (PeerItem)ref.get();
    
      if( item == null ) {
        Debug.out( "OOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOPS: ref.get() == null" );
        peer_items.put( key, new WeakReference( key ) );
        return key;
      }

      return item;
    }
    finally{  item_mon.exit();  }
  }
  
  
  
}
