/*
 * Created on Feb 10, 2005
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

import java.nio.ByteBuffer;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.plugins.messaging.*;





/**
 *
 */
public class AdapterMessageImpl implements Message, com.aelitis.azureus.core.peermanager.messaging.Message {
  private Message plug_msg = null;
  private com.aelitis.azureus.core.peermanager.messaging.Message core_msg = null;
  
  
  public AdapterMessageImpl( Message plug_msg ) {
    this.plug_msg = plug_msg;
  }
  
  
  public AdapterMessageImpl( com.aelitis.azureus.core.peermanager.messaging.Message core_msg ) {
    this.core_msg = core_msg;
  }
  
  
  
  
  //plugin Message implementation
  public ByteBuffer[] getPayload() {
    if( core_msg == null ) {
      return plug_msg.getPayload();
    }
    
    DirectByteBuffer[] dbbs = core_msg.getData();  
    ByteBuffer[] bbs = new ByteBuffer[ dbbs.length ];  //TODO cache it???
    for( int i=0; i < dbbs.length; i++ ) {
      bbs[i] = dbbs[i].getBuffer( DirectByteBuffer.SS_MSG );
    }
    return bbs;
  }
  
  public Message create( String id, byte version, ByteBuffer data ) throws MessageException  {
    if( core_msg == null ) {
      return plug_msg.create( id, version, data );
    }
    
    try{
      return new AdapterMessageImpl( core_msg.deserialize( id, version, new DirectByteBuffer( data ) ) );
    }
    catch( com.aelitis.azureus.core.peermanager.messaging.MessageException e ) {
      throw new MessageException( e.getMessage() );
    }
  }
  

  
  //shared Message implementation
  public String getID() {
    return core_msg == null ? plug_msg.getID() : core_msg.getID();
  }
  
  public byte getVersion() {
    return core_msg == null ? plug_msg.getVersion() : core_msg.getVersion();
  }
  
  public int getType() {
    return core_msg == null ? plug_msg.getType() : core_msg.getType();
  }
  
  public String getDescription() {
    return core_msg == null ? plug_msg.getDescription() : core_msg.getDescription();
  }
  
  public void destroy() {
    if( core_msg == null ) plug_msg.destroy();
    else core_msg.destroy();
  }
  
  
  
  //core Message implementation 
  public DirectByteBuffer[] getData() {
    if( plug_msg == null ) {
      return core_msg.getData();
    }
    
    ByteBuffer[] bbs = plug_msg.getPayload();
    DirectByteBuffer[] dbbs = new DirectByteBuffer[ bbs.length ];  //TODO cache it???
    for( int i=0; i < bbs.length; i++ ) {
      dbbs[i] = new DirectByteBuffer( bbs[i] );
    }
    return dbbs;
  }
  
  public com.aelitis.azureus.core.peermanager.messaging.Message deserialize( String id, byte version, DirectByteBuffer data ) throws com.aelitis.azureus.core.peermanager.messaging.MessageException {
    if( plug_msg == null ) {
      return core_msg.deserialize( id, version, data );
    }
    
    try{
      return new AdapterMessageImpl( plug_msg.create( id, version, data.getBuffer( DirectByteBuffer.SS_MSG ) ) );
    }
    catch( MessageException e ) {
      throw new com.aelitis.azureus.core.peermanager.messaging.MessageException( e.getMessage() );
    }
    finally {
      data.returnToPool();
    }
  }
  
}
