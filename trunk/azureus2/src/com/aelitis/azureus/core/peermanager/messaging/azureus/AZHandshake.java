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

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;




/**
 * AZ handshake message.
 */
public class AZHandshake implements AZMessage {
  private static final byte bss = DirectByteBuffer.SS_MSG;


  private final DirectByteBuffer buffer;
  private final String description;
  private final byte[] identity;
  private final String client;
  private final int client_version;
  private final String[] avail_ids;
  private final byte[] avail_versions;
  
  
  public AZHandshake( byte[] peer_identity, String client, int version, String[] avail_msg_ids, byte[] avail_msg_versions ) {
    this.identity = peer_identity;
    this.client = client;
    this.client_version = version;
    this.avail_ids = avail_msg_ids;
    this.avail_versions = avail_msg_versions;
    
    Map payload_map = new HashMap();
    
    //client info
    payload_map.put( "identity", peer_identity );
    payload_map.put( "client", client );
    payload_map.put( "version", new Long( version ) );
        
    //available message list
    List message_list = new ArrayList();
    String msgs_desc = "";
    for( int i=0; i < avail_msg_ids.length; i++ ) {
      String id = avail_msg_ids[ i ];
      byte ver = avail_msg_versions[ i ];
      
      if( id.equals( getID() ) && ver == getVersion() ) {
        continue;  //skip ourself
      }

      Map msg = new HashMap();
      msg.put( "id", id );
      msg.put( "ver", new byte[]{ ver } );
        
      message_list.add( msg );
      
      msgs_desc += "[" +id+ ":" +ver+ "]";
    }
    payload_map.put( "messages", message_list );

    
    
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
    
    System.out.println( "Generated AZHandshake size = " +raw_payload.length+ " bytes" );

    this.description = getID()+ "from [" +ByteFormatter.nicePrint( peer_identity, true )+ ", " +client+ " " +version+ "] supports " +msgs_desc;
  }

  
  
  public byte[] getIdentity() {  return identity;  }
  
  public String getClient() {  return client;  }
  
  public int getClientVersion() {  return client_version;  }
  
  public String[] getMessageIDs() {  return avail_ids;  }
  
  public byte[] getMessageVersions() {  return avail_versions;  }
  
  
    
  public String getID() {  return AZMessage.ID_AZ_HANDSHAKE;  }
  
  public byte getVersion() {  return AZMessage.AZ_DEFAULT_VERSION;  }
  
  public int getType() {  return Message.TYPE_PROTOCOL_PAYLOAD;  }
    
  public String getDescription() {  return description;  }
  
  public DirectByteBuffer[] getData() {  return new DirectByteBuffer[]{ buffer };  }
  
  public Message deserialize( DirectByteBuffer data ) throws MessageException {   
    if( data == null ) {
      throw new MessageException( "[" +getID() + ":" +getVersion()+ "] decode error: data == null" );
    }
    
    if( data.remaining( bss ) < 42 ) {
      throw new MessageException( "[" +getID() + ":" +getVersion()+ "] decode error: payload.remaining[" +data.remaining( DirectByteBuffer.SS_MSG )+ "] < 42" );
    }

    try {
      byte[] raw = new byte[ data.remaining( bss ) ];
      data.get( bss, raw );
      
      Map root = BDecoder.decode( raw );
      //////////////////////////////////////////////
      byte[] id = (byte[])root.get( "identity" );
      if( id == null ) {
        throw new Exception( "id == null" );
      }
      if( id.length != 20 ) {
        throw new Exception( "id.length != 20" );
      }
      //////////////////////////////////////////////
      byte[] raw_name = (byte[])root.get( "client" );
      if( raw_name == null ) {
        throw new Exception( "raw_name == null" );
      }
      String name = new String( raw_name );
      //////////////////////////////////////////////
      Long raw_ver = (Long)root.get( "version" );
      if( raw_ver == null ) {
        throw new Exception( "raw_ver == null" );
      }
      int version = raw_ver.intValue();
      //////////////////////////////////////////////
      List raw_msgs = (List)root.get( "messages" );
      if( raw_msgs == null ) {
        throw new Exception( "raw_msgs == null" );
      }
      
      String[] ids = new String[ raw_msgs.size() ];
      byte[] vers = new byte[ raw_msgs.size() ];
      
      int pos = 0;
      
      for( Iterator i = raw_msgs.iterator(); i.hasNext(); ) {
        Map msg = (Map)i.next();
        
        byte[] mid = (byte[])msg.get( "id" );
        if( mid == null ) {
          throw new Exception( "mid == null" );
        }
        ids[ pos ] = new String( mid );
        
        byte[] ver = (byte[])msg.get( "ver" );
        if( ver == null ) {
          throw new Exception( "ver == null" );
        }
        if( ver.length != 1 ) {
          throw new Exception( "ver.length != 1" );
        }
        vers[ pos ] = ver[ 0 ];
        
        pos++;
      }
      //////////////////////////////////////////////  
      
      data.returnToPool();
      
      return new AZHandshake( id, name, version, ids, vers );
    }
    catch( Throwable t ) {
      throw new MessageException( "[" +getID() + ":" +getVersion()+ "] payload b-decode error: " +t.getMessage() );
    }  
  }
  
  
  public void destroy() { /*nothing*/ }
}
