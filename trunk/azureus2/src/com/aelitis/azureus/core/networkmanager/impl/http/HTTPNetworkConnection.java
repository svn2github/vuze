/*
 * Created on 3 Oct 2006
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
import java.util.*;

import org.gudy.azureus2.core3.peer.impl.PEPeerTransport;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.RawMessage;
import com.aelitis.azureus.core.networkmanager.impl.RawMessageImpl;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTPiece;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTRequest;

public class 
HTTPNetworkConnection 
{
	private static final String	NL			= "\r\n";

	private NetworkConnection	connection;
	private PEPeerTransport		peer;
	
	private HTTPMessageDecoder	decoder;
	private HTTPMessageEncoder	encoder;
	
	private boolean	choked	= true;
	
	private List	pending_requests = new ArrayList();
	
	protected
	HTTPNetworkConnection(
		NetworkConnection		_connection,
		PEPeerTransport			_peer )
	{
		connection	= _connection;
		peer		= _peer;
		
		decoder	= (HTTPMessageDecoder)connection.getIncomingMessageQueue().getDecoder();
		encoder = (HTTPMessageEncoder)connection.getOutgoingMessageQueue().getEncoder();

		decoder.setConnection( this );
		encoder.setConnection( this );
	}
	
	protected PEPeerTransport
	getPeer()
	{
		return( peer );
	}
	
	protected void
	choke()
	{
		synchronized( pending_requests ){
			
			choked	= true;
		}
	}
	
	protected void
	unchoke()
	{
		boolean	wakeup = false;
		
		synchronized( pending_requests ){
			
			choked	= false;
			
			for (int i=0;i<pending_requests.size();i++){
				
				wakeup = true;
				
				decoder.addRequest((BTRequest)pending_requests.get(i));
			}
			
			pending_requests.clear();
		}
		
		if ( wakeup ){
			
			connection.getTransport().setReadyForRead();
		}
	}
	
	protected void
	addRequest(
		BTRequest		request )
	{
		synchronized( pending_requests ){
				
			if ( choked ){
					
				if ( pending_requests.size() > 1024 ){
					
					Debug.out( "pending request limit exceeded" );
					
				}else{
				
					pending_requests.add( request );
				}
			}else{
				
				decoder.addRequest( request );
			}
		}
	}
	
	protected RawMessage
	addPiece(
		Message		message,
		BTPiece		piece )
	{
			// TODO: order?
		
		DirectByteBuffer	data = piece.getPieceData();
		
		
		byte[]	http_header = (
				"HTTP/1.1 200 OK" + NL + 
				"Content-Type: application/octet-stream" + NL +
				"Server: " + Constants.AZUREUS_NAME + " " + Constants.AZUREUS_VERSION + NL +
				"Connection: close" + NL +
				"Content-Length: " + data.remaining( DirectByteBuffer.SS_NET ) + NL +
				NL ).getBytes();
		
		return( 
				new RawMessageImpl( 
						message, 
						new DirectByteBuffer[]{ new DirectByteBuffer( ByteBuffer.wrap( http_header )), data },
						RawMessage.PRIORITY_HIGH, 
						true, 
						new Message[0] ));
	}
}
