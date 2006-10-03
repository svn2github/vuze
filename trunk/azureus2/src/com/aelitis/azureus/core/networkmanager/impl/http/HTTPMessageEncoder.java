/*
 * Created on 2 Oct 2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.networkmanager.impl.http;

import java.nio.ByteBuffer;

import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.networkmanager.RawMessage;
import com.aelitis.azureus.core.networkmanager.impl.RawMessageImpl;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamEncoder;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTMessage;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTPiece;

public class 
HTTPMessageEncoder
	implements MessageStreamEncoder
{
	private HTTPNetworkConnection	http_connection;
	
	public void
	setConnection(
		HTTPNetworkConnection	_http_connection )
	{
		http_connection	= _http_connection;
	}
	
	public RawMessage 
	encodeMessage( 
		Message message )
	{
		String	id = message.getID();
		
		System.out.println( "encodeMessage: " + message.getID());
		
		if ( id.equals( BTMessage.ID_BT_CHOKE )){
			
			http_connection.choke();
		
		}else if ( id.equals( BTMessage.ID_BT_UNCHOKE )){
			
			http_connection.unchoke();

		}else if ( id.equals( BTMessage.ID_BT_PIECE )){
						
			return( http_connection.encodePiece( message ));
		}
			
		return( http_connection.getEmptyRawMessage( message ));
	}
}
