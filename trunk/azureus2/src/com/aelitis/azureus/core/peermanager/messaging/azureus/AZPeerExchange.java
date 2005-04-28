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

import java.nio.ByteBuffer;
import java.util.*;

import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.peermanager.messaging.*;
import com.aelitis.azureus.core.peermanager.peerdb.*;




/**
 * AZ handshake message.
 */
public class AZPeerExchange implements AZMessage {
  private static final byte bss = DirectByteBuffer.SS_MSG;

  private final DirectByteBuffer buffer;
  private final String description;
  private final byte[] infohash;
  
  private final PeerItem[] peers_added;
  private final PeerItem[] peers_dropped;
  
  
  
  public AZPeerExchange( byte[] infohash, PeerItem[] peers_added, PeerItem[] peers_dropped ) {
    this.infohash = infohash;
    this.peers_added = peers_added;
    this.peers_dropped = peers_dropped;

    Map payload_map = new HashMap();
    
    payload_map.put( "infohash", infohash );
    insertPeers( "added", payload_map, peers_added );
    insertPeers( "dropped", payload_map, peers_dropped );

    //convert to bytestream
    byte[] raw_payload;
    try {
      raw_payload = BEncoder.encode( payload_map );
    }
    catch( Throwable t ) {
      t.printStackTrace();
      raw_payload = new byte[0];
    }
    
    this.buffer = new DirectByteBuffer( ByteBuffer.wrap( raw_payload ) );
    
    if( raw_payload.length > 1200 )  System.out.println( "Generated AZPeerExchange size = " +raw_payload.length+ " bytes" );

    int add_count = peers_added == null ? 0 : peers_added.length;
    int drop_count = peers_dropped == null ? 0 : peers_dropped.length;
    
    this.description = getID()+ " for infohash " +ByteFormatter.nicePrint( infohash, true )+ " with " +add_count+ " added and " +drop_count+ " dropped peers";
  }



  private void insertPeers( String key_name, Map root_map, PeerItem[] peers ) {
    if( peers != null && peers.length > 0 ) {
      ArrayList raw_peers = new ArrayList();

      for( int i=0; i < peers.length; i++ ) {
        PeerItem peer = peers[i];
        
        //combine address and port bytes into one
        byte[] full_address = new byte[ peer.getAddress().length +2 ];
        System.arraycopy( peer.getAddress(), 0, full_address, 0, peer.getAddress().length );
        full_address[ peer.getAddress().length ] = (byte)(peer.getPort() >> 8);
        full_address[ peer.getAddress().length +1 ] = (byte)(peer.getPort() & 0xff);

        raw_peers.add( full_address );
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
        
        //extract address and port
        byte[] address = new byte[ full_address.length -2 ];
        System.arraycopy( full_address, 0, address, 0, full_address.length -2 );
        
        byte p0 = full_address[ full_address.length -2 ];
        byte p1 = full_address[ full_address.length -1 ];
        
        int port = (p1 & 0xFF) + ((p0 & 0xFF) << 8);
        PeerItem peer = PeerItemFactory.createPeerItem( address, port, PeerItemFactory.PEER_SOURCE_PEER_EXCHANGE );
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
    
  public String getDescription() {  return description;  }
  
  public DirectByteBuffer[] getData() {  return new DirectByteBuffer[]{ buffer };  }
  
  
  public Message deserialize( DirectByteBuffer data ) throws MessageException {   
    if( data == null ) {
      throw new MessageException( "[" +getID() + ":" +getVersion()+ "] decode error: data == null" );
    }
    
    if( data.remaining( bss ) < 10 ) {
      throw new MessageException( "[" +getID() + ":" +getVersion()+ "] decode error: payload.remaining[" +data.remaining( DirectByteBuffer.SS_MSG )+ "] < 10" );
    }

    try {
      byte[] raw = new byte[ data.remaining( bss ) ];
      data.get( bss, raw );
      
      Map root = BDecoder.decode( raw );

      byte[] hash = (byte[])root.get( "infohash" );
      if( hash == null ) {
        throw new MessageException( "hash == null" );
      }
      if( hash.length != 20 ) {
        throw new MessageException( "hash.length != 20: " +hash.length );
      }
      
      PeerItem[] added = extractPeers( "added", root );
      PeerItem[] dropped = extractPeers( "dropped", root );
      
      if( added == null && dropped == null ) {
        throw new MessageException( "[" +getID() + ":" +getVersion()+ "] received exchange message without any adds or drops" );
      }

      data.returnToPool();
      
      return new AZPeerExchange( hash, added, dropped );
    }
    catch( Throwable t ) {
      throw new MessageException( "[" +getID() + ":" +getVersion()+ "] payload b-decode error: " +t.getMessage() );
    }  
  }
  
  
  public void destroy() { /*nothing*/ }
}
