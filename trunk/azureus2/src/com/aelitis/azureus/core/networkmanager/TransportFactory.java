/*
 * Created on Nov 1, 2005
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
package com.aelitis.azureus.core.networkmanager;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import com.aelitis.azureus.core.networkmanager.impl.LightweightTCPTransport;
import com.aelitis.azureus.core.networkmanager.impl.TCPTransportImpl;

/**
 * 
 */
public class TransportFactory {
	
	/**
	 * Create a disconnected TCP transport (the core runs the select ops automatically).
	 * @return outgoing transport
	 */
	public static TCPTransport createTCPTransport() {
		return new TCPTransportImpl();
	}

	/**
	 * Create an already-connected TCP transport (the core runs the select ops automatically).
	 * @param channel of incoming connection
	 * @param already_read bytes from the channel
	 * @return incoming transport
	 */
	public static TCPTransport createTCPTransport( SocketChannel channel, ByteBuffer already_read ) {
		return new TCPTransportImpl( channel, already_read );
	}
	
  
	/**
	 * Create a lightweight TCP transport (only read/write/close functionality, no auto-selects).
	 * @param channel of connection
	 * @return lightweight transport
	 */
  public static TCPTransport createLightweightTCPTransport( SocketChannel channel ) {
  	return new LightweightTCPTransport( channel );
  }
                                                                                     
                                                                                  
}
