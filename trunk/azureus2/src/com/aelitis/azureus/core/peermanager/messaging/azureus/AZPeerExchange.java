/*
 * Created on Apr 30, 2004
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

package com.aelitis.azureus.core.peermanager.messaging.azureus;

import java.util.*;

import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.peermanager.messaging.*;
import com.aelitis.azureus.core.peermanager.peerdb.*;




/**
 * AZ peer exchange message.
 */
public class AZPeerExchange implements AZMessage {
  private static final byte bss = DirectByteBuffer.SS_MSG;

  private DirectByteBuffer buffer = null;
  private String description = null;
  
  private final byte[] infohash;
  private final PeerItem[] peers_added;
  private final PeerItem[] peers_dropped;
  
  
  
  public AZPeerExchange( byte[] infohash, PeerItem[] peers_added, PeerItem[] peers_dropped ) {
    this.infohash = infohash;
    this.peers_added = peers_added;
    this.peers_dropped = peers_dropped;
  }
  

  
  private void insertPeers( String key_name, Map root_map, PeerItem[] peers ) {
    if( peers != null && peers.length > 0 ) {
      ArrayList raw_peers = new ArrayList();

      for( int i=0; i < peers.length; i++ ) {
        raw_peers.add( peers[i].getSerialization() );
      }

      root_map.put( key_name, raw_peers );
    }
  }
  
  
  
  private PeerItem[] extractPeers( String key_name, Map root_map ) {
    PeerItem[] return_peers = null;
    ArrayList peers = new ArrayList();

    List raw_peers = (List)root_map.get( key_name );
    if( raw_peers != null ) {
      for( Iterator it = raw_peers.iterator(); it.hasNext(); ) {
        byte[] full_address = (byte[])it.next();
        PeerItem peer = PeerItemFactory.createPeerItem( full_address, PeerItemFactory.PEER_SOURCE_PEER_EXCHANGE );
        peers.add( peer );
      }
    }
    
    if( !peers.isEmpty() ) {
      return_peers = new PeerItem[ peers.size() ];
      peers.toArray( return_peers );
    }
    
    return return_peers;
  }
  
  
  
  
  
  public byte[] getInfoHash() {  return infohash;  }
  
  public PeerItem[] getAddedPeers() {  return peers_added;  }
  
  public PeerItem[] getDroppedPeers() {  return peers_dropped;  }
  
  
    
  public String getID() {  return AZMessage.ID_AZ_PEER_EXCHANGE;  }
  
  public byte getVersion() {  return AZMessage.AZ_DEFAULT_VERSION;  }
  
  public int getType() {  return Message.TYPE_PROTOCOL_PAYLOAD;  }
    
  
  public String getDescription() {
    if( description == null ) {
      int add_count = peers_added == null ? 0 : peers_added.length;
      int drop_count = peers_dropped == null ? 0 : peers_dropped.length;
      
      description = getID()+ " for infohash " +ByteFormatter.nicePrint( infohash, true )+ " with " +add_count+ " added and " +drop_count+ " dropped peers";
    }
    
    return description;
  }
  
  
  public DirectByteBuffer[] getData() {
    if( buffer == null ) {
      Map payload_map = new HashMap();
      
      payload_map.put( "infohash", infohash );
      insertPeers( "added", payload_map, peers_added );
      insertPeers( "dropped", payload_map, peers_dropped );

      buffer = MessagingUtil.convertPayloadToBencodedByteStream( payload_map, DirectByteBuffer.AL_MSG_AZ_PEX );

      if( buffer.remaining( bss ) > 1000 )  System.out.println( "Generated AZPeerExchange size = " +buffer.remaining( bss )+ " bytes" );
    }
    
    return new DirectByteBuffer[]{ buffer };
  }
  
  
  public Message deserialize( DirectByteBuffer data ) throws MessageException {
    if( data.remaining( bss ) > 1000 )  System.out.println( "Received PEX msg byte size = " +data.remaining( bss ) );
    
    Map root = MessagingUtil.convertBencodedByteStreamToPayload( data, 10, getID(), getVersion() );

    byte[] hash = (byte[])root.get( "infohash" );
    if( hash == null )  throw new MessageException( "hash == null" );
    if( hash.length != 20 )  throw new MessageException( "hash.length != 20: " +hash.length );

    PeerItem[] added = extractPeers( "added", root );
    PeerItem[] dropped = extractPeers( "dropped", root );
      
    if( added == null && dropped == null )  throw new MessageException( "[" +getID() + ":" +getVersion()+ "] received exchange message without any adds or drops" );

    return new AZPeerExchange( hash, added, dropped );
  }
  
  
  public void destroy() {
    if( buffer != null )  buffer.returnToPool();
  }
  
}
