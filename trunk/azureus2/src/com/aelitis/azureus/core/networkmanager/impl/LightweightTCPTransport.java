/*
 * Created on Oct 28, 2005
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
package com.aelitis.azureus.core.networkmanager.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import com.aelitis.azureus.core.networkmanager.TCPTransport;


/**
 * This class is essentially a socket channel wrapper to support working with az message encoders/decoders.
 */
public class LightweightTCPTransport implements TCPTransport {
	
	private final TCPTransportHelper helper;	
	
	public LightweightTCPTransport( SocketChannel channel ) {
		this.helper = new TCPTransportHelper( channel );
	}
	

  public long write( ByteBuffer[] buffers, int array_offset, int length ) throws IOException {
  	return helper.write( buffers, array_offset, length );
  }

  
  public long read( ByteBuffer[] buffers, int array_offset, int length ) throws IOException {
  	return helper.read( buffers, array_offset, length );
  }
  

  public SocketChannel getSocketChannel(){  return helper.getSocketChannel();  }
  
  public String getDescription(){  return getSocketChannel().socket().getInetAddress().getHostAddress() + ": " + getSocketChannel().socket().getPort();  }
  
  public void close(){
  	try {
  		getSocketChannel().close();  //close() can block
    }
    catch( Throwable t) { t.printStackTrace(); }
  }
  
  
  public void setAlreadyRead( ByteBuffer bytes_already_read ){ 	throw new RuntimeException( "not implemented" );  }
  public boolean isReadyForWrite(){  throw new RuntimeException( "not implemented" );  }  
  public boolean isReadyForRead(){  throw new RuntimeException( "not implemented" );  }  
  public void establishOutboundConnection( final InetSocketAddress address, final ConnectListener listener ){ throw new RuntimeException( "not implemented" ); }  
  public void setTransportMode( int mode ){ throw new RuntimeException( "not implemented" ); } 
  public int getTransportMode(){ throw new RuntimeException( "not implemented" );  }

}
